package com.dyh.club.platform.like;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LikeRedisOutOfOrderTest {
    @Test
    void versionSevenWinsWhenVersionSixReachesRedisLater() throws Exception {
        JdbcTemplate jdbc = database();
        OutOfOrderRedis redis = new OutOfOrderRedis();
        StaticListableBeanFactory redisBeans = new StaticListableBeanFactory();
        redisBeans.addBean("redis", redis);
        LikeService service = new LikeService(redisBeans.getBeanProvider(StringRedisTemplate.class),
                new StaticListableBeanFactory().getBeanProvider(RocketMqLikeBridge.class),
                new LikePersistenceService(jdbc), jdbc);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Map<String, Object>> versionSix = executor.submit(() -> service.set(7, 9, true));
            assertThat(redis.versionSixWaiting.await(3, TimeUnit.SECONDS)).isTrue();
            Future<Map<String, Object>> versionSeven = executor.submit(() -> service.set(7, 9, false));

            Map<String, Object> sevenResult = versionSeven.get(3, TimeUnit.SECONDS);
            Map<String, Object> sixResult = versionSix.get(3, TimeUnit.SECONDS);

            assertThat(sevenResult).containsEntry("version", 7L).containsEntry("liked", false);
            assertThat(sixResult).containsEntry("version", 7L).containsEntry("liked", false);
            assertThat(redis.accepted.get()).isEqualTo(1);
            assertThat(redis.stale.get()).isEqualTo(1);
            assertThat(redis.pendingWrites.get()).isEqualTo(1);
            assertThat(redis.pendingClears.get()).isEqualTo(1);
            assertThat(redis.version).isEqualTo(7L);
            assertThat(redis.liked).isFalse();
            assertThat(jdbc.queryForObject("SELECT state_version FROM club_question_like WHERE question_id=7 AND user_id=9", Long.class)).isEqualTo(7L);
            assertThat(jdbc.queryForObject("SELECT liked FROM club_question_like WHERE question_id=7 AND user_id=9", Boolean.class)).isFalse();
            assertThat(LikeService.TOGGLE_SCRIPT)
                    .contains("incoming<=current", "return {0,current", "ARGV[3]..'|'..incoming");
        } finally {
            executor.shutdownNow();
        }
    }

    private JdbcTemplate database() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:like-order;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE club_question(id BIGINT PRIMARY KEY,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE club_question_like(id BIGINT AUTO_INCREMENT PRIMARY KEY,question_id BIGINT,user_id BIGINT,liked BOOLEAN,state_version BIGINT,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(question_id,user_id))");
        jdbc.execute("CREATE TABLE club_like_version(question_id BIGINT,user_id BIGINT,current_version BIGINT,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(question_id,user_id))");
        jdbc.update("INSERT INTO club_question VALUES(7,'PUBLISHED')");
        jdbc.update("INSERT INTO club_question_like(question_id,user_id,liked,state_version) VALUES(7,9,FALSE,5)");
        return jdbc;
    }

    private static class OutOfOrderRedis extends StringRedisTemplate {
        private final CountDownLatch versionSixWaiting = new CountDownLatch(1);
        private final CountDownLatch versionSevenApplied = new CountDownLatch(1);
        private final AtomicInteger accepted = new AtomicInteger();
        private final AtomicInteger stale = new AtomicInteger();
        private final AtomicInteger pendingWrites = new AtomicInteger();
        private final AtomicInteger pendingClears = new AtomicInteger();
        private volatile long version = 5;
        private volatile boolean liked;

        @SuppressWarnings("unchecked")
        @Override
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            if (args.length == 2) {
                pendingClears.incrementAndGet();
                return (T) Long.valueOf(1);
            }
            long incoming = Long.parseLong(String.valueOf(args[7]));
            if (incoming == 6) {
                versionSixWaiting.countDown();
                try {
                    if (!versionSevenApplied.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("版本 7 未及时到达 Redis");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }
            synchronized (this) {
                if (incoming <= version) {
                    stale.incrementAndGet();
                    return (T) Arrays.asList(0L, version, liked ? 1L : 0L, liked ? 1L : 0L);
                }
                version = incoming;
                liked = "1".equals(String.valueOf(args[2]));
                accepted.incrementAndGet();
                pendingWrites.incrementAndGet();
                if (incoming == 7) {
                    versionSevenApplied.countDown();
                }
                return (T) Arrays.asList(1L, version, liked ? 1L : 0L, liked ? 1L : 0L);
            }
        }
    }
}
