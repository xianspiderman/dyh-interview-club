package com.dyh.club.platform.like;

import com.dyh.club.lock.DistributedLock;
import com.dyh.club.platform.common.BizException;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LikeService {
    private static final Logger log=LoggerFactory.getLogger(LikeService.class);
    private static final String PENDING="club:like:pending",PENDING_AGE="club:like:pending:age",VERSIONS="club:like:versions";
    private static final DefaultRedisScript<List> TOGGLE=new DefaultRedisScript<>(
            "local old=redis.call('hget',KEYS[1],ARGV[1]); local ver=redis.call('hget',KEYS[3],ARGV[2]); local cnt=redis.call('get',KEYS[2]); if not cnt then cnt=ARGV[7]; redis.call('set',KEYS[2],cnt) end; if not old then old=ARGV[5]; redis.call('hset',KEYS[1],ARGV[1],old) end; if not ver or tonumber(ver)<tonumber(ARGV[6]) then if ver and old~=ARGV[5] then if ARGV[5]=='1' then cnt=redis.call('incr',KEYS[2]) else if tonumber(cnt)>0 then cnt=redis.call('decr',KEYS[2]) end end end; old=ARGV[5]; ver=ARGV[6]; redis.call('hset',KEYS[1],ARGV[1],old); redis.call('hset',KEYS[3],ARGV[2],ver) end; if old==ARGV[3] then return {0,tonumber(ver),tonumber(cnt)} end; ver=tonumber(ARGV[8]); redis.call('hset',KEYS[3],ARGV[2],ver); redis.call('hset',KEYS[1],ARGV[1],ARGV[3]); if ARGV[3]=='1' then cnt=redis.call('incr',KEYS[2]) else if tonumber(cnt)>0 then cnt=redis.call('decr',KEYS[2]) end end; redis.call('hset',KEYS[4],ARGV[2],ARGV[3]..'|'..ver); redis.call('zadd',KEYS[5],ARGV[4],ARGV[2]); return {1,tonumber(ver),tonumber(cnt)}",List.class);
    private static final DefaultRedisScript<Long> CLEAR=new DefaultRedisScript<>(
            "local value=redis.call('hget',KEYS[1],ARGV[1]); if value==ARGV[2] then redis.call('hdel',KEYS[1],ARGV[1]); redis.call('zrem',KEYS[2],ARGV[1]); return 1 else return 0 end",Long.class);
    private final StringRedisTemplate redis;private final LikePersistenceService persistence;private final ObjectProvider<RocketMqLikeBridge> mq;private final JdbcTemplate jdbc;
    public LikeService(ObjectProvider<StringRedisTemplate> redis,ObjectProvider<RocketMqLikeBridge> mq,LikePersistenceService persistence,JdbcTemplate jdbc){this.redis=redis.getIfAvailable();this.mq=mq;this.persistence=persistence;this.jdbc=jdbc;}

    public Map<String,Object> set(long questionId,long userId,boolean liked){
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM club_question WHERE id=? AND status='PUBLISHED'",Integer.class,questionId);if(exists==null||exists==0)throw BizException.notFound("题目不存在或未发布");
        long databaseVersion=mysqlVersion(questionId,userId), allocatedVersion=persistence.nextVersion(questionId,userId);boolean databaseLiked=mysqlLiked(questionId,userId);long databaseCount=mysqlCount(questionId);
        LikeEvent event;long count;
        if(redis!=null){try{
            String state="club:like:state:"+questionId,countKey="club:like:count:"+questionId,key=questionId+":"+userId;
            List result=redis.execute(TOGGLE,Arrays.asList(state,countKey,VERSIONS,PENDING,PENDING_AGE),String.valueOf(userId),key,liked?"1":"0",String.valueOf(System.currentTimeMillis()),databaseLiked?"1":"0",String.valueOf(databaseVersion),String.valueOf(databaseCount),String.valueOf(allocatedVersion));
            if(result==null||result.size()<3)throw new IllegalStateException("点赞 Lua 返回值异常");long changed=((Number)result.get(0)).longValue();long version=((Number)result.get(1)).longValue();count=((Number)result.get(2)).longValue();event=new LikeEvent(questionId,userId,liked,version);if(changed==1)publishOrPersist(event);
        }catch(Exception error){log.warn("Redis 点赞链路失败，降级为数据库条件写入",error);event=new LikeEvent(questionId,userId,liked,allocatedVersion);requireApplied(event);count=mysqlCount(questionId);}}
        else{event=new LikeEvent(questionId,userId,liked,allocatedVersion);requireApplied(event);count=mysqlCount(questionId);}
        Map<String,Object> response=new LinkedHashMap<>();response.put("questionId",questionId);response.put("liked",liked);response.put("count",count);response.put("version",event.version);return response;
    }

    private void publishOrPersist(LikeEvent event){RocketMqLikeBridge bridge=mq.getIfAvailable();if(bridge!=null){try{bridge.send(event);return;}catch(Exception error){log.error("点赞消息发送失败，保留待处理记录，key={}",event.businessKey(),error);return;}}requireApplied(event);clearPending(event);}
    public void consume(LikeEvent event){LikePersistenceService.ApplyResult result=persistence.apply(event);if(result!=LikePersistenceService.ApplyResult.STALE)clearPending(event);else log.info("忽略低版本点赞事件且保留 pending，key={}, version={}",event.businessKey(),event.version);}
    private void requireApplied(LikeEvent event){if(persistence.apply(event)==LikePersistenceService.ApplyResult.STALE)throw BizException.conflict("点赞状态已被其他请求更新，请重试");}
    private void clearPending(LikeEvent event){if(redis==null)return;try{redis.execute(CLEAR,Arrays.asList(PENDING,PENDING_AGE),event.businessKey(),(event.liked?"1":"0")+"|"+event.version);}catch(Exception error){log.warn("点赞待处理记录清理失败，等待补偿再次处理",error);}}

    public Map<String,Object> status(long questionId,Long userId){boolean liked=false;long count;
        if(redis!=null){try{String value=userId==null?null:(String)redis.opsForHash().get("club:like:state:"+questionId,String.valueOf(userId));liked=value==null?(userId!=null&&mysqlLiked(questionId,userId)):"1".equals(value);String raw=redis.opsForValue().get("club:like:count:"+questionId);count=raw==null?mysqlCount(questionId):Long.parseLong(raw);}
        catch(Exception e){liked=userId!=null&&mysqlLiked(questionId,userId);count=mysqlCount(questionId);}}
        else{liked=userId!=null&&mysqlLiked(questionId,userId);count=mysqlCount(questionId);}Map<String,Object>out=new LinkedHashMap<>();out.put("liked",liked);out.put("count",count);return out;}

    @Scheduled(fixedDelay=60000)
    @XxlJob("subjectLikeCompensation")
    @DistributedLock(prefix="job:like-compensation",key="'all'",leaseMillis=55000)
    public void compensate(){processPending(500,"like-compensation");}
    public int recover(int requestedLimit){return processPending(Math.max(1,Math.min(requestedLimit,100)),"like-controlled-recovery");}
    private int processPending(int limit,String jobName){if(redis==null)return 0;int processed=0;try(Cursor<Map.Entry<Object,Object>> cursor=redis.opsForHash().scan(PENDING,ScanOptions.scanOptions().count(limit).build())){while(cursor.hasNext()&&processed<limit){Map.Entry<Object,Object>entry=cursor.next();String[]key=String.valueOf(entry.getKey()).split(":");String[]value=String.valueOf(entry.getValue()).split("\\|");if(key.length==2&&value.length==2){LikeEvent event=new LikeEvent(Long.parseLong(key[0]),Long.parseLong(key[1]),"1".equals(value[0]),Long.parseLong(value[1]));consume(event);processed++;}}recordRun(jobName,"SUCCESS",processed,"待处理记录补偿");return processed;}
        catch(Exception error){recordRun(jobName,"FAILED",processed,shortMessage(error));log.error("点赞补偿失败",error);return processed;}}
    public long pendingCount(){if(redis==null)return 0;try{return redis.opsForHash().size(PENDING);}catch(Exception e){return -1;}}
    public long pendingOldestAgeSeconds(){if(redis==null)return 0;try{Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> rows=redis.opsForZSet().rangeWithScores(PENDING_AGE,0,0);if(rows==null||rows.isEmpty())return 0;Double score=rows.iterator().next().getScore();return score==null?0:Math.max(0,(System.currentTimeMillis()-score.longValue())/1000);}catch(Exception e){return -1;}}
    private boolean mysqlLiked(long q,long u){Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM club_question_like WHERE question_id=? AND user_id=? AND liked=TRUE",Integer.class,q,u);return count!=null&&count>0;}
    private long mysqlCount(long q){Long count=jdbc.queryForObject("SELECT COUNT(*) FROM club_question_like WHERE question_id=? AND liked=TRUE",Long.class,q);return count==null?0:count;}
    private long mysqlVersion(long q,long u){Long version=jdbc.queryForObject("SELECT COALESCE(MAX(state_version),0) FROM club_question_like WHERE question_id=? AND user_id=?",Long.class,q,u);return version==null?0:version;}
    private void recordRun(String name,String status,int count,String detail){jdbc.update("INSERT INTO club_job_run(job_name,status,processed_count,detail_text,finished_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)",name,status,count,detail);}
    private String shortMessage(Exception e){String m=e.getMessage();return m==null?e.getClass().getSimpleName():m.substring(0,Math.min(480,m.length()));}
}
