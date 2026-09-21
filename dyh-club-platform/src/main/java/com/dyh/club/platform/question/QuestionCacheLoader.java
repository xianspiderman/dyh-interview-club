package com.dyh.club.platform.question;

import com.dyh.club.lock.DistributedLock;
import com.dyh.club.platform.common.BizException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QuestionCacheLoader {
    private final JdbcTemplate jdbc;
    private final QuestionTypeHandlerFactory handlers;

    public QuestionCacheLoader(JdbcTemplate jdbc, QuestionTypeHandlerFactory handlers) {
        this.jdbc = jdbc;
        this.handlers = handlers;
    }

    @DistributedLock(prefix = "question:detail:rebuild", key = "#questionId", leaseMillis = 5000)
    public Map<String, Object> load(Long questionId) {
        try {
            Map<String, Object> question = jdbc.queryForObject(
                    "SELECT q.id,q.name,q.analysis_text,q.difficulty,q.score,q.question_type,q.status,q.data_version," +
                            "q.created_by,u.nickname AS author_name,u.avatar AS author_avatar,q.created_at,q.updated_at " +
                            "FROM club_question q JOIN club_user u ON u.id=q.created_by WHERE q.id=?",
                    (rs, n) -> {
                        Map<String, Object> value = new LinkedHashMap<>();
                        value.put("id", rs.getLong("id"));
                        value.put("name", rs.getString("name"));
                        value.put("analysis", rs.getString("analysis_text"));
                        value.put("difficulty", rs.getInt("difficulty"));
                        value.put("score", rs.getInt("score"));
                        value.put("type", rs.getString("question_type"));
                        value.put("status", rs.getString("status"));
                        value.put("dataVersion", rs.getLong("data_version"));
                        value.put("createdBy", rs.getLong("created_by"));
                        value.put("authorName", rs.getString("author_name"));
                        value.put("authorAvatar", rs.getString("author_avatar"));
                        value.put("createdAt", rs.getTimestamp("created_at"));
                        value.put("updatedAt", rs.getTimestamp("updated_at"));
                        return value;
                    }, questionId);
            String type = String.valueOf(question.get("type"));
            question.putAll(handlers.get(type).load(questionId));
            question.put("categories", jdbc.query(
                    "SELECT c.id,c.name FROM club_question_category qc JOIN club_category c ON c.id=qc.category_id WHERE qc.question_id=? ORDER BY c.sort_no,c.id",
                    (rs, n) -> named(rs.getLong(1), rs.getString(2)), questionId));
            question.put("labels", jdbc.query(
                    "SELECT l.id,l.name FROM club_question_label ql JOIN club_label l ON l.id=ql.label_id WHERE ql.question_id=? ORDER BY l.id",
                    (rs, n) -> named(rs.getLong(1), rs.getString(2)), questionId));
            return question;
        } catch (EmptyResultDataAccessException missing) {
            throw BizException.notFound("题目不存在");
        }
    }

    private Map<String, Object> named(long id, String name) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", id);
        value.put("name", name);
        return value;
    }
}
