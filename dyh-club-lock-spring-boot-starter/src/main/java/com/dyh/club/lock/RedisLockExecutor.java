package com.dyh.club.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;

public class RedisLockExecutor implements LockExecutor {
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisLockExecutor(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryLock(String key, String owner, long waitMillis, long leaseMillis) {
        long deadline = System.nanoTime() + Math.max(0L, waitMillis) * 1_000_000L;
        do {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, owner, Duration.ofMillis(leaseMillis));
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }
            if (waitMillis <= 0L) {
                break;
            }
            try {
                Thread.sleep(Math.min(25L, waitMillis));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.nanoTime() < deadline);
        return false;
    }

    @Override
    public boolean unlock(String key, String owner) {
        Long deleted = redis.execute(UNLOCK_SCRIPT, Collections.singletonList(key), owner);
        return Long.valueOf(1L).equals(deleted);
    }
}
