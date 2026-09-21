package com.dyh.club.platform.like;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class LikeRedisRecoveryBoundaryTest {
    @Test void emptyRedisIsSeededFromHighDatabaseVersionBeforeToggle() {
        JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:like-recovery;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE club_question(id BIGINT PRIMARY KEY,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE club_question_like(id BIGINT AUTO_INCREMENT PRIMARY KEY,question_id BIGINT,user_id BIGINT,liked BOOLEAN,state_version BIGINT,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(question_id,user_id))");
        jdbc.execute("CREATE TABLE club_like_version(question_id BIGINT,user_id BIGINT,current_version BIGINT,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(question_id,user_id))");
        jdbc.update("INSERT INTO club_question VALUES(7,'PUBLISHED')");jdbc.update("INSERT INTO club_question_like(question_id,user_id,liked,state_version) VALUES(7,9,TRUE,41)");
        CapturingRedis redis=new CapturingRedis();StaticListableBeanFactory beans=new StaticListableBeanFactory();beans.addBean("redis",redis);
        LikePersistenceService persistence=new LikePersistenceService(jdbc);
        LikeService service=new LikeService(beans.getBeanProvider(StringRedisTemplate.class),new StaticListableBeanFactory().getBeanProvider(RocketMqLikeBridge.class),persistence,jdbc);

        Map<String,Object> result=service.set(7,9,false);

        assertThat(redis.arguments).hasSize(8);assertThat(redis.arguments[4]).isEqualTo("1");assertThat(redis.arguments[5]).isEqualTo("41");assertThat(redis.arguments[7]).isEqualTo("42");
        assertThat(result).containsEntry("liked",false).containsEntry("version",42L);
        assertThat(jdbc.queryForObject("SELECT state_version FROM club_question_like WHERE question_id=7 AND user_id=9",Long.class)).isEqualTo(42L);
        assertThat(jdbc.queryForObject("SELECT liked FROM club_question_like WHERE question_id=7 AND user_id=9",Boolean.class)).isFalse();
    }
    private static class CapturingRedis extends StringRedisTemplate {
        Object[] arguments;
        @SuppressWarnings("unchecked") @Override public <T>T execute(RedisScript<T> script,List<String>keys,Object...args){if(args.length==8){arguments=args;long version=Long.parseLong(String.valueOf(args[7]));long count=Math.max(0,Long.parseLong(String.valueOf(args[6]))-1);return(T)Arrays.asList(1L,version,count,0L);}return(T)Long.valueOf(1);}
    }
}
