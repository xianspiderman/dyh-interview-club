package com.dyh.club.platform.ops;

import com.dyh.club.platform.like.LikeService;
import com.dyh.club.platform.question.QuestionCache;
import com.dyh.club.platform.search.SearchService;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class OpsDashboardService {
    private final JdbcTemplate jdbc;private final DataSource dataSource;private final RedisConnectionFactory redis;
    private final QuestionCache cache;private final ThreadPoolTaskExecutor labelExecutor;private final LikeService likes;private final SearchService search;private final MeterRegistry metrics;
    public OpsDashboardService(JdbcTemplate jdbc,DataSource dataSource,ObjectProvider<RedisConnectionFactory> redis,QuestionCache cache,
        @Qualifier("labelExecutor")ThreadPoolTaskExecutor labelExecutor,LikeService likes,SearchService search,MeterRegistry metrics){
        this.jdbc=jdbc;this.dataSource=dataSource;this.redis=redis.getIfAvailable();this.cache=cache;this.labelExecutor=labelExecutor;this.likes=likes;this.search=search;this.metrics=metrics;}
    public Map<String,Object> dashboard(){Map<String,Object> root=new LinkedHashMap<>();root.put("generatedAt",new Date());root.put("service",service());root.put("requests",requests());root.put("jvm",jvm());root.put("threadPools",threadPools());root.put("database",database());root.put("redis",redis());root.put("cache",cache());root.put("message",message());root.put("search",search.status());root.put("jobs",jobs());return root;}
    private Map<String,Object> service(){return map("status","UP","questionCount",number("SELECT COUNT(*) FROM club_question"),"userCount",number("SELECT COUNT(*) FROM club_user"),"practiceCount",number("SELECT COUNT(*) FROM club_practice"),"postCount",number("SELECT COUNT(*) FROM club_post"));}
    private Map<String,Object> requests(){long count=0,errors=0;double totalNanos=0,p95=0;for(Timer t:metrics.find("club.http.requests").timers()){count+=t.count();totalNanos+=t.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS);String status=t.getId().getTag("status");if(status!=null&&status.startsWith("5"))errors+=t.count();for(io.micrometer.core.instrument.distribution.ValueAtPercentile v:t.takeSnapshot().percentileValues())if(Math.abs(v.percentile()-.95)<.001)p95=Math.max(p95,v.value(java.util.concurrent.TimeUnit.MILLISECONDS));}return map("count",count,"averageMillis",count==0?0:Math.round(totalNanos/count/1_000_000d*100)/100d,"p95Millis",Math.round(p95*100)/100d,"errorRate",count==0?0:Math.round(errors*10000d/count)/100d);}
    private Map<String,Object> jvm(){MemoryUsage heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();long gcCount=0,gcMs=0;for(GarbageCollectorMXBean gc:ManagementFactory.getGarbageCollectorMXBeans()){gcCount+=Math.max(0,gc.getCollectionCount());gcMs+=Math.max(0,gc.getCollectionTime());}return map("heapUsedBytes",heap.getUsed(),"heapMaxBytes",heap.getMax(),"threadCount",ManagementFactory.getThreadMXBean().getThreadCount(),"gcCount",gcCount,"gcMillis",gcMs);}
    private Map<String,Object> threadPools(){ThreadPoolExecutor p=labelExecutor.getThreadPoolExecutor();return map("web",map("configuredMax",200),"aggregate",map("active",p.getActiveCount(),"poolSize",p.getPoolSize(),"max",p.getMaximumPoolSize(),"queueSize",p.getQueue().size(),"queueRemaining",p.getQueue().remainingCapacity()));}
    private Map<String,Object> database(){try{HikariDataSource h=dataSource.unwrap(HikariDataSource.class);return map("status","UP","active",h.getHikariPoolMXBean().getActiveConnections(),"idle",h.getHikariPoolMXBean().getIdleConnections(),"waiting",h.getHikariPoolMXBean().getThreadsAwaitingConnection(),"max",h.getMaximumPoolSize());}catch(Exception e){return map("status","DOWN");}}
    private Map<String,Object> redis(){if(redis==null)return map("status","DISABLED");RedisConnection connection=null;try{connection=redis.getConnection();String pong=connection.ping();return map("status","PONG".equalsIgnoreCase(pong)?"UP":"UNKNOWN");}catch(Exception e){return map("status","DOWN");}finally{if(connection!=null)connection.close();}}
    private Map<String,Object> cache(){CacheStats s=cache.stats();return map("requestCount",s.requestCount(),"hitCount",s.hitCount(),"hitRate",Math.round(s.hitRate()*10000)/100d,"evictionCount",s.evictionCount(),"pendingInvalidations",number("SELECT COUNT(*) FROM club_cache_invalidation_task WHERE status<>'DONE'"));}
    private Map<String,Object> message(){long pending,age;try{pending=likes.pendingCount();age=likes.pendingOldestAgeSeconds();}catch(Exception e){pending=-1;age=-1;}return map("likePending",pending,"oldestAgeSeconds",age,"backlogAlert",pending>5000||age>120,"countThreshold",5000,"ageThresholdSeconds",120);}
    private List<Map<String,Object>> jobs(){return jdbc.queryForList("SELECT job_name,status,processed_count,detail_text,started_at,finished_at FROM club_job_run ORDER BY id DESC LIMIT 10");}
    private long number(String sql){Number n=jdbc.queryForObject(sql,Number.class);return n==null?0:n.longValue();}
    private static Map<String,Object> map(Object...v){Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)m.put(String.valueOf(v[i]),v[i+1]);return m;}
}
