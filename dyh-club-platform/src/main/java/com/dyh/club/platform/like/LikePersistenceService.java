package com.dyh.club.platform.like;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikePersistenceService {
    private final JdbcTemplate jdbc;
    public LikePersistenceService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public enum ApplyResult { APPLIED, ALREADY_APPLIED, STALE }

    @Transactional
    public long nextVersion(long questionId,long userId){
        Long allocated;
        try {
            allocated=jdbc.queryForObject("SELECT current_version FROM club_like_version WHERE question_id=? AND user_id=? FOR UPDATE",Long.class,questionId,userId);
        } catch (EmptyResultDataAccessException missing) {
            Long persisted=jdbc.queryForObject("SELECT COALESCE(MAX(state_version),0) FROM club_question_like WHERE question_id=? AND user_id=?",Long.class,questionId,userId);
            long first=(persisted==null?0:persisted)+1;
            try { jdbc.update("INSERT INTO club_like_version(question_id,user_id,current_version) VALUES(?,?,?)",questionId,userId,first); return first; }
            catch (DuplicateKeyException race) { allocated=jdbc.queryForObject("SELECT current_version FROM club_like_version WHERE question_id=? AND user_id=? FOR UPDATE",Long.class,questionId,userId); }
        }
        Long persisted=jdbc.queryForObject("SELECT COALESCE(MAX(state_version),0) FROM club_question_like WHERE question_id=? AND user_id=?",Long.class,questionId,userId);
        long next=Math.max(allocated==null?0:allocated,persisted==null?0:persisted)+1;
        if(jdbc.update("UPDATE club_like_version SET current_version=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND user_id=?",next,questionId,userId)!=1)
            throw new IllegalStateException("点赞状态版本分配失败");
        return next;
    }

    @Transactional
    public ApplyResult apply(LikeEvent event){
        int updated=jdbc.update("UPDATE club_question_like SET liked=?,state_version=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND user_id=? AND state_version<?",
                event.liked,event.version,event.questionId,event.userId,event.version);
        if(updated==1)return ApplyResult.APPLIED;
        try {
            java.util.Map<String,Object> existing=jdbc.queryForMap("SELECT liked,state_version FROM club_question_like WHERE question_id=? AND user_id=?",event.questionId,event.userId);
            long version=((Number)existing.get("state_version")).longValue();
            boolean liked=Boolean.TRUE.equals(existing.get("liked")) || (existing.get("liked") instanceof Number && ((Number)existing.get("liked")).intValue()!=0);
            return version==event.version&&liked==event.liked?ApplyResult.ALREADY_APPLIED:ApplyResult.STALE;
        } catch (EmptyResultDataAccessException missing) {
            try{jdbc.update("INSERT INTO club_question_like(question_id,user_id,liked,state_version) VALUES(?,?,?,?)",event.questionId,event.userId,event.liked,event.version);return ApplyResult.APPLIED;}
            catch(DuplicateKeyException race){
                int won=jdbc.update("UPDATE club_question_like SET liked=?,state_version=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND user_id=? AND state_version<?",event.liked,event.version,event.questionId,event.userId,event.version);
                return won==1?ApplyResult.APPLIED:ApplyResult.STALE;
            }
        }
    }
}
