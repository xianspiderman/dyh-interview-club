package com.dyh.club.platform.like;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikePersistenceService {
    private final JdbcTemplate jdbc;
    public LikePersistenceService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Transactional
    public boolean apply(LikeEvent event){
        int updated=jdbc.update("UPDATE club_question_like SET liked=?,state_version=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND user_id=? AND state_version<?",
                event.liked,event.version,event.questionId,event.userId,event.version);
        if(updated==1)return true;
        Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM club_question_like WHERE question_id=? AND user_id=?",Integer.class,event.questionId,event.userId);
        if(existing!=null&&existing>0)return false;
        try{jdbc.update("INSERT INTO club_question_like(question_id,user_id,liked,state_version) VALUES(?,?,?,?)",event.questionId,event.userId,event.liked,event.version);return true;}
        catch(DuplicateKeyException race){return jdbc.update("UPDATE club_question_like SET liked=?,state_version=?,updated_at=CURRENT_TIMESTAMP WHERE question_id=? AND user_id=? AND state_version<?",event.liked,event.version,event.questionId,event.userId,event.version)==1;}
    }
}
