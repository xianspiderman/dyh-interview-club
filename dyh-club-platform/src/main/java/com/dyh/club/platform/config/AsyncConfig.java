package com.dyh.club.platform.config;

import org.slf4j.MDC;
import com.dyh.club.platform.common.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {
    @Bean("labelExecutor")
    public ThreadPoolTaskExecutor labelExecutor(
            @Value("${club.aggregate.core-pool-size:16}") int core,
            @Value("${club.aggregate.max-pool-size:32}") int max,
            @Value("${club.aggregate.queue-capacity:200}") int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix("club-label-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(task -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            Long userId = UserContext.get();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                Long previousUser = UserContext.get();
                try {
                    if (context != null) MDC.setContextMap(context); else MDC.clear();
                    UserContext.set(userId);
                    task.run();
                } finally {
                    if (previous != null) MDC.setContextMap(previous); else MDC.clear();
                    UserContext.set(previousUser);
                }
            };
        });
        executor.initialize();
        return executor;
    }
}
