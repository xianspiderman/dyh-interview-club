package com.dyh.club.platform.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class QuestionCache {
    private static final Logger log = LoggerFactory.getLogger(QuestionCache.class);
    private static final String NULL_VALUE = "__NULL__";
    private final Cache<Long, Map<String, Object>> local;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final QuestionCacheLoader loader;
    private final int redisTtl;
    private final int jitter;
    private final int nullTtl;

    public QuestionCache(ObjectProvider<StringRedisTemplate> redisProvider, ObjectMapper mapper, QuestionCacheLoader loader,
                         @Value("${club.cache.local-ttl-seconds:30}") int localTtl,
                         @Value("${club.cache.redis-ttl-seconds:600}") int redisTtl,
                         @Value("${club.cache.redis-jitter-seconds:120}") int jitter,
                         @Value("${club.cache.null-ttl-seconds:120}") int nullTtl) {
        this.redis = redisProvider.getIfAvailable();
        this.mapper = mapper;
        this.loader = loader;
        this.redisTtl = redisTtl;
        this.jitter = jitter;
        this.nullTtl = nullTtl;
        this.local = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofSeconds(localTtl)).recordStats().build();
    }

    public Map<String, Object> get(long id) {
        Map<String, Object> cached = local.getIfPresent(id);
        if (cached != null) return copy(cached);
        String key = key(id);
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(key);
                if (NULL_VALUE.equals(json)) throw com.dyh.club.platform.common.BizException.notFound("题目不存在");
                if (json != null) {
                    cached = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                    local.put(id, cached);
                    return copy(cached);
                }
            } catch (com.dyh.club.platform.common.BizException expected) {
                throw expected;
            } catch (Exception error) {
                log.warn("Redis 题目缓存读取失败，降级回源，key={}", key, error);
            }
        }
        try {
            Map<String, Object> loaded = loader.load(id);
            put(id, loaded);
            return copy(loaded);
        } catch (com.dyh.club.platform.common.BizException missing) {
            if (redis != null && missing.getCode() == 404) {
                try { redis.opsForValue().set(key, NULL_VALUE, Duration.ofSeconds(nullTtl)); } catch (Exception ignored) { }
            }
            throw missing;
        }
    }

    public void evict(long id) {
        local.invalidate(id);
        if (redis != null) {
            try {
                redis.delete(key(id));
                redis.convertAndSend("club:cache:invalidate", key(id));
            } catch (Exception error) {
                throw new IllegalStateException("缓存失效发布失败", error);
            }
        }
    }

    public void evictLocal(String cacheKey) {
        if (cacheKey != null && cacheKey.startsWith("club:question:detail:")) {
            try { local.invalidate(Long.parseLong(cacheKey.substring(cacheKey.lastIndexOf(':') + 1))); } catch (NumberFormatException ignored) { }
        }
    }

    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() { return local.stats(); }

    private void put(long id, Map<String, Object> value) {
        local.put(id, value);
        if (redis != null) {
            try {
                int extra = jitter <= 0 ? 0 : ThreadLocalRandom.current().nextInt(jitter + 1);
                redis.opsForValue().set(key(id), mapper.writeValueAsString(value), Duration.ofSeconds(redisTtl + extra));
            } catch (Exception error) {
                log.warn("Redis 题目缓存回填失败，key={}", key(id), error);
            }
        }
    }

    private String key(long id) { return "club:question:detail:" + id; }
    private Map<String, Object> copy(Map<String, Object> value) { return new LinkedHashMap<>(value); }
}
