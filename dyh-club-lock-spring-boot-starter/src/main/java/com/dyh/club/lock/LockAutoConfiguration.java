package com.dyh.club.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnClass({StringRedisTemplate.class, DistributedLock.class})
@ConditionalOnProperty(prefix = "club.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LockProperties.class)
public class LockAutoConfiguration {
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(LockExecutor.class)
    public LockExecutor lockExecutor(StringRedisTemplate redis) {
        return new RedisLockExecutor(redis);
    }

    @Bean
    @ConditionalOnBean(LockExecutor.class)
    @ConditionalOnMissingBean(DistributedLockAspect.class)
    public DistributedLockAspect distributedLockAspect(LockExecutor executor, LockProperties properties) {
        return new DistributedLockAspect(executor, properties);
    }
}
