package com.dyh.club.platform.practice;

import com.dyh.club.platform.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Service
public class PracticeService {
    private final JdbcTemplate jdbc;
    private final PracticeQuestionGateway questions;

    public PracticeService(JdbcTemplate jdbc, PracticeQuestionGateway questions) { this.jdbc = jdbc; this.questions = questions; }

    @Transactional
    public long generate(long userId, GenerateRequest request) {
        int amount = Math.max(1, Math.min(request.count <= 0 ? 10 : request.count, 50));
        StringBuilder sql = new StringBuilder("SELECT q.id FROM club_question q WHERE q.status='PUBLISHED' AND q.question_type IN ('RADIO','MULTIPLE','JUDGE')");
        List<Object> args = new ArrayList<>();
        if (request.categoryId != null) { sql.append(" AND EXISTS(SELECT 1 FROM club_question_category qc WHERE qc.question_id=q.id AND qc.category_id=?)"); args.add(request.categoryId); }
        if (request.labelId != null) { sql.append(" AND EXISTS(SELECT 1 FROM club_question_label ql WHERE ql.question_id=q.id AND ql.label_id=?)"); args.add(request.labelId); }
        sql.append(" ORDER BY q.id LIMIT ?"); args.add(amount);
        List<Long> ids = jdbc.query(sql.toString(), (rs, n) -> rs.getLong(1), args.toArray());
        if (ids.isEmpty()) throw BizException.badRequest("当前筛选条件下没有可用于自动判分的题目");
        String title = request.title == null || request.title.trim().isEmpty() ? "专项练习" : request.title.trim();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO club_practice(user_id,title,status,total_count) VALUES(?,?,'IN_PROGRESS',?)", new String[]{"id"});
            statement.setLong(1, userId); statement.setString(2, title); statement.setInt(3, ids.size()); return statement;
        }, key);
        long practiceId = Objects.requireNonNull(key.getKey()).longValue();
        for (Long questionId : ids) jdbc.update("INSERT INTO club_practice_question(practice_id,question_id,answer_status) VALUES(?,?,'UNANSWERED')", practiceId, questionId);
        return practiceId;
    }

    @Transactional
    public void saveAnswer(long userId, long practiceId, long questionId, String answer, int elapsedSeconds) {
        assertOwner(userId, practiceId);
        String status = jdbc.queryForObject("SELECT status FROM club_practice WHERE id=?", String.class, practiceId);
        if (!"IN_PROGRESS".equals(status)) throw BizException.conflict("练习已经交卷，不能继续修改答案");
        int changed = jdbc.update("UPDATE club_practice_question SET answer_content=?,answer_status='ANSWERED',updated_at=CURRENT_TIMESTAMP WHERE practice_id=? AND question_id=?",
                normalizeAnswer(answer), practiceId, questionId);
        if (changed == 0) throw BizException.badRequest("该题不属于当前练习");
        jdbc.update("UPDATE club_practice SET elapsed_seconds=? WHERE id=? AND status='IN_PROGRESS'", Math.max(0, elapsedSeconds), practiceId);
    }

    public List<Map<String, Object>> unfinished(long userId) {
        return jdbc.query("SELECT id,title,started_at,elapsed_seconds,total_count FROM club_practice WHERE user_id=? AND status='IN_PROGRESS' ORDER BY started_at DESC",
                (rs, n) -> { Map<String,Object> row=new LinkedHashMap<>(); row.put("id",rs.getLong("id")); row.put("title",rs.getString("title")); row.put("startedAt",rs.getTimestamp("started_at")); row.put("elapsedSeconds",rs.getInt("elapsed_seconds")); row.put("totalCount",rs.getInt("total_count")); return row; }, userId);
    }

    public Map<String, Object> resume(long userId, long practiceId) {
        assertOwner(userId, practiceId);
        Map<String, Object> practice = new LinkedHashMap<>(jdbc.queryForMap("SELECT id,title,status,started_at,submitted_at,elapsed_seconds,correct_count,total_count FROM club_practice WHERE id=?", practiceId));
        List<Map<String, Object>> items = jdbc.query("SELECT question_id,answer_content,answer_status,correct_flag FROM club_practice_question WHERE practice_id=? ORDER BY id",
                (rs, n) -> { Map<String,Object> row=new LinkedHashMap<>(); long questionId=rs.getLong("question_id"); row.put("questionId",questionId); row.put("answer",rs.getString("answer_content")); row.put("answerStatus",rs.getString("answer_status")); Object correct=rs.getObject("correct_flag"); row.put("correct",correct==null?null:rs.getBoolean("correct_flag")); row.put("question",questions.detail(questionId)); return row; }, practiceId);
        practice.put("questions", items);
        return practice;
    }

    @Transactional
    public Map<String, Object> submit(long userId, long practiceId, int elapsedSeconds) {
        assertOwner(userId, practiceId);
        int first = jdbc.update("UPDATE club_practice SET status='SUBMITTED',submitted_at=CURRENT_TIMESTAMP,elapsed_seconds=?,version_no=version_no+1 WHERE id=? AND status='IN_PROGRESS'",
                Math.max(0, elapsedSeconds), practiceId);
        if (first == 0) return report(userId, practiceId);
        List<Map<String, Object>> answers = jdbc.queryForList("SELECT question_id,answer_content,answer_status FROM club_practice_question WHERE practice_id=? ORDER BY id", practiceId);
        int correct = 0;
        for (Map<String, Object> item : answers) {
            long questionId = ((Number) item.get("question_id")).longValue();
            String answerStatus = String.valueOf(item.get("answer_status"));
            boolean ok = "ANSWERED".equals(answerStatus) && questions.isCorrect(questionId, (String) item.get("answer_content"));
            if (ok) correct++;
            jdbc.update("UPDATE club_practice_question SET correct_flag=?,answer_status=? WHERE practice_id=? AND question_id=?",
                    ok, "ANSWERED".equals(answerStatus) ? "ANSWERED" : "UNANSWERED", practiceId, questionId);
        }
        jdbc.update("UPDATE club_practice SET correct_count=? WHERE id=?", correct, practiceId);
        return report(userId, practiceId);
    }

    public Map<String, Object> report(long userId, long practiceId) {
        assertOwner(userId, practiceId);
        Map<String, Object> report = new LinkedHashMap<>(jdbc.queryForMap("SELECT id,title,status,started_at,submitted_at,elapsed_seconds,correct_count,total_count FROM club_practice WHERE id=?", practiceId));
        int correct = ((Number) report.get("correct_count")).intValue();
        int total = ((Number) report.get("total_count")).intValue();
        report.put("correctRate", total == 0 ? 0D : Math.round(correct * 10000D / total) / 100D);
        report.put("labelStats", jdbc.query(
                "SELECT l.id,l.name,COUNT(*) total_count,SUM(CASE WHEN pq.correct_flag=TRUE THEN 1 ELSE 0 END) correct_count " +
                        "FROM club_practice_question pq JOIN club_question_label ql ON ql.question_id=pq.question_id JOIN club_label l ON l.id=ql.label_id " +
                        "WHERE pq.practice_id=? GROUP BY l.id,l.name ORDER BY correct_count ASC,total_count DESC",
                (rs,n)->{Map<String,Object> row=new LinkedHashMap<>();row.put("labelId",rs.getLong(1));row.put("labelName",rs.getString(2));row.put("total",rs.getInt(3));row.put("correct",rs.getInt(4));return row;}, practiceId));
        report.put("details", jdbc.queryForList("SELECT question_id,answer_content,answer_status,correct_flag FROM club_practice_question WHERE practice_id=? ORDER BY id", practiceId));
        return report;
    }

    private void assertOwner(long userId, long practiceId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM club_practice WHERE id=? AND user_id=?", Integer.class, practiceId, userId);
        if (count == null || count == 0) throw BizException.notFound("练习不存在或无权访问");
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) return "";
        String[] parts = answer.trim().toUpperCase(Locale.ROOT).split("[,\\s]+");
        TreeSet<String> sorted = new TreeSet<>(Arrays.asList(parts));
        sorted.remove("");
        return String.join(",", sorted);
    }

    public static class GenerateRequest { public String title; public Long categoryId; public Long labelId; public int count = 10; }
    public static class AnswerRequest { public long questionId; public String answer; public int elapsedSeconds; }
    public static class SubmitRequest { public int elapsedSeconds; }
}
