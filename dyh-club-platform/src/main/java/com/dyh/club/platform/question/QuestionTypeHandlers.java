package com.dyh.club.platform.question;

import com.dyh.club.platform.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

abstract class OptionQuestionHandler implements QuestionTypeHandler {
    protected final JdbcTemplate jdbc;
    OptionQuestionHandler(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(long questionId, QuestionModels.QuestionRequest request) {
        validate(request);
        for (QuestionModels.OptionRequest option : request.options) {
            jdbc.update("INSERT INTO club_question_option(question_id,option_code,option_content,correct_flag) VALUES(?,?,?,?)",
                    questionId, option.code.trim().toUpperCase(Locale.ROOT), option.content.trim(), option.correct);
        }
    }

    protected void validate(QuestionModels.QuestionRequest request) {
        if (request.options == null || request.options.size() < 2) throw BizException.badRequest("客观题至少需要两个选项");
    }

    @Override public void delete(long questionId) { jdbc.update("DELETE FROM club_question_option WHERE question_id=?", questionId); }

    @Override
    public Map<String, Object> load(long questionId) {
        List<Map<String, Object>> options = jdbc.query("SELECT option_code,option_content,correct_flag FROM club_question_option WHERE question_id=? ORDER BY option_code",
                (rs, n) -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("code", rs.getString("option_code"));
                    value.put("content", rs.getString("option_content"));
                    value.put("correct", rs.getBoolean("correct_flag"));
                    return value;
                }, questionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("options", options);
        return result;
    }

    protected Set<String> correctCodes(long questionId) {
        return new TreeSet<>(jdbc.query("SELECT option_code FROM club_question_option WHERE question_id=? AND correct_flag=TRUE",
                (rs, n) -> rs.getString(1).toUpperCase(Locale.ROOT), questionId));
    }

    protected Set<String> normalize(String answer) {
        if (answer == null || answer.trim().isEmpty()) return Collections.emptySet();
        return Arrays.stream(answer.toUpperCase(Locale.ROOT).split("[,\\s]+"))
                .filter(v -> !v.isEmpty()).collect(Collectors.toCollection(TreeSet::new));
    }
}

@Component
class RadioQuestionHandler extends OptionQuestionHandler {
    RadioQuestionHandler(JdbcTemplate jdbc) { super(jdbc); }
    @Override public String type() { return "RADIO"; }
    @Override protected void validate(QuestionModels.QuestionRequest request) {
        super.validate(request);
        if (request.options.stream().filter(o -> o.correct).count() != 1) throw BizException.badRequest("单选题必须且只能有一个正确选项");
    }
    @Override public boolean isCorrect(long id, String answer) { return correctCodes(id).equals(normalize(answer)); }
}

@Component
class MultipleQuestionHandler extends OptionQuestionHandler {
    MultipleQuestionHandler(JdbcTemplate jdbc) { super(jdbc); }
    @Override public String type() { return "MULTIPLE"; }
    @Override protected void validate(QuestionModels.QuestionRequest request) {
        super.validate(request);
        if (request.options.stream().filter(o -> o.correct).count() < 2) throw BizException.badRequest("多选题至少需要两个正确选项");
    }
    @Override public boolean isCorrect(long id, String answer) { return correctCodes(id).equals(normalize(answer)); }
}

@Component
class JudgeQuestionHandler extends OptionQuestionHandler {
    JudgeQuestionHandler(JdbcTemplate jdbc) { super(jdbc); }
    @Override public String type() { return "JUDGE"; }
    @Override protected void validate(QuestionModels.QuestionRequest request) {
        super.validate(request);
        if (request.options.size() != 2 || request.options.stream().filter(o -> o.correct).count() != 1) {
            throw BizException.badRequest("判断题需要两个选项且只能有一个正确答案");
        }
    }
    @Override public boolean isCorrect(long id, String answer) { return correctCodes(id).equals(normalize(answer)); }
}

@Component
class BriefQuestionHandler implements QuestionTypeHandler {
    private final JdbcTemplate jdbc;
    BriefQuestionHandler(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public String type() { return "BRIEF"; }
    @Override public void save(long id, QuestionModels.QuestionRequest request) {
        if (request.referenceAnswer == null || request.referenceAnswer.trim().isEmpty()) throw BizException.badRequest("简答题参考答案不能为空");
        jdbc.update("INSERT INTO club_question_brief(question_id,reference_answer) VALUES(?,?)", id, request.referenceAnswer.trim());
    }
    @Override public void delete(long id) { jdbc.update("DELETE FROM club_question_brief WHERE question_id=?", id); }
    @Override public Map<String, Object> load(long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("referenceAnswer", jdbc.queryForObject("SELECT reference_answer FROM club_question_brief WHERE question_id=?", String.class, id));
        return result;
    }
    @Override public boolean isCorrect(long id, String answer) { return false; }
}
