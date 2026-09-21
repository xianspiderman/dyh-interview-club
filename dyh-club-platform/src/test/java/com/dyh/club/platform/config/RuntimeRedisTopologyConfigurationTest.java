package com.dyh.club.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeRedisTopologyConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

    @Test
    void fourRuntimeServicesCanUseThreeNodeSentinelFromEnvironmentProperties() {
        contextRunner.withPropertyValues(
                "spring.redis.sentinel.master=clubmaster",
                "spring.redis.sentinel.nodes[0]=sentinel-1:26379",
                "spring.redis.sentinel.nodes[1]=sentinel-2:26379",
                "spring.redis.sentinel.nodes[2]=sentinel-3:26379")
                .run(context -> {
                    LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.isRedisSentinelAware()).isTrue();
                    assertThat(factory.getSentinelConfiguration().getMaster().getName()).isEqualTo("clubmaster");
                    Set<String> nodes = factory.getSentinelConfiguration().getSentinels().stream()
                            .map(RedisNode::asString).collect(Collectors.toSet());
                    assertThat(nodes).containsExactlyInAnyOrder(
                            "sentinel-1:26379", "sentinel-2:26379", "sentinel-3:26379");
                });
    }

    @Test
    void localModeStillUsesStandaloneHostAndPort() {
        contextRunner.withPropertyValues("spring.redis.host=redis", "spring.redis.port=6379")
                .run(context -> {
                    LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
                    assertThat(factory.isRedisSentinelAware()).isFalse();
                    assertThat(factory.getStandaloneConfiguration().getHostName()).isEqualTo("redis");
                    assertThat(factory.getStandaloneConfiguration().getPort()).isEqualTo(6379);
                });
    }
}
