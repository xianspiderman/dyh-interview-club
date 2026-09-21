package com.dyh.club.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRedisSentinelConfigurationTest {
    @Test
    void gatewayUsesConfiguredSentinelMasterAndAllThreeNodes() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues(
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
}
