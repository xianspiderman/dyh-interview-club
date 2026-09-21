package com.dyh.club.lock;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisLockExecutorTest {
    @SuppressWarnings("unchecked")
    @Test
    void lockUsesAtomicSetNxWithLease() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq("club:lock:question:1"), eq("owner"), eq(Duration.ofSeconds(5))))
                .thenReturn(true);

        RedisLockExecutor executor = new RedisLockExecutor(redis);
        assertTrue(executor.tryLock("club:lock:question:1", "owner", 0, 5000));
        verify(values).setIfAbsent("club:lock:question:1", "owner", Duration.ofSeconds(5));
    }

    @Test
    void unlockExecutesCompareAndDeleteScript() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), eq(java.util.Collections.singletonList("k")), eq("owner"))).thenReturn(1L);
        assertTrue(new RedisLockExecutor(redis).unlock("k", "owner"));
    }
}
