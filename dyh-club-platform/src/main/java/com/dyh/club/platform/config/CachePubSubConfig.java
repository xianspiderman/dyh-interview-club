package com.dyh.club.platform.config;

import com.dyh.club.platform.question.QuestionCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class CachePubSubConfig {
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisMessageListenerContainer cacheInvalidationListener(RedisConnectionFactory factory, QuestionCache cache) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((message, pattern) -> cache.evictLocal(message.toString()), new ChannelTopic("club:cache:invalidate"));
        return container;
    }
}
