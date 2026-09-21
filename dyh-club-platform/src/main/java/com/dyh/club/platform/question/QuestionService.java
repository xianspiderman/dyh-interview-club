package com.dyh.club.platform.question;

import com.dyh.club.platform.common.BizException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class QuestionService {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final QuestionTypeHandlerFactory handlers;
    private final QuestionCache cache;
    private final CacheInvalidationService invalidation;
    private final ThreadPoolTaskExecutor labelExecutor;
    private final long branchTimeout;
    private final long totalTimeout;

    public QuestionService(JdbcTemplate jdbc, QuestionTypeHandlerFactory handlers, QuestionCache cache,
                           CacheInvalidationService invalidation,
                           @Qualifier("labelExecutor") ThreadPoolTaskExecutor labelExecutor,
                           @Value("${club.aggregate.branch-timeout-millis:300}") long branchTimeout,
                           @Value("${club.aggregate.total-timeout-millis:500}") long totalTimeout) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
        this.handlers = handlers;
        this.cache = cache;
        this.invalidation = invalidation;
        this.labelExecutor = labelExecutor;
        this.branchTimeout = branchTimeout;
        this.totalTimeout = totalTimeout;
    }

    @Transactional
    public long create(QuestionModels.QuestionRequest request, long userId) {
        String type = normalizedType(request.type);
        handlers.get(type);
        validateReferences(request.categoryIds, request.labelIds);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator statement = connection -> {
            java.sql.PreparedStatement prepared = connection.prepareStatement(
                    "INSERT INTO club_question(name,analysis_text,difficulty,score,question_type,status,data_version,created_by) VALUES(?,?,?,?,?,?,1,?)",
                    new String[]{"id"});
            prepared.setString(1, request.name.trim());
            prepared.setString(2, request.analysis);
            prepared.setInt(3, request.difficulty);
            prepared.setInt(4, request.score);
            prepared.setString(5, type);
            prepared.setString(6, normalizeStatus(request.status));
            prepared.setLong(7, userId);
            return prepared;
        };
        jdbc.update(statement, keyHolder);
        Number generated = keyHolder.getKey();
        if (generated == null) throw new IllegalStateException("题目主键生成失败");
        long id = generated.longValue();
        handlers.get(type).save(id, request);
        saveMappings(id, request.categoryIds, request.labelIds);
        invalidation.recordQuestion(id);
        return id;
    }

    @Transactional
    public void update(long id, QuestionModels.QuestionRequest request) {
        Map<String, Object> current = cache.get(id);
        String oldType = String.valueOf(current.get("type"));
        String type = normalizedType(request.type);
        handlers.get(type);
        validateReferences(request.categoryIds, request.labelIds);
        int changed = jdbc.update("UPDATE club_question SET name=?,analysis_text=?,difficulty=?,score=?,question_type=?,status=?,data_version=data_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                request.name.trim(), request.analysis, request.difficulty, request.score, type, normalizeStatus(request.status), id);
        if (changed == 0) throw BizException.notFound("题目不存在");
        handlers.get(oldType).delete(id);
        if (!"BRIEF".equals(oldType)) jdbc.update("DELETE FROM club_question_brief WHERE question_id=?", id);
        if (!"BRIEF".equals(type)) jdbc.update("DELETE FROM club_question_option WHERE question_id=?", id);
        handlers.get(type).save(id, request);
        jdbc.update("DELETE FROM club_question_category WHERE question_id=?", id);
        jdbc.update("DELETE FROM club_question_label WHERE question_id=?", id);
        saveMappings(id, request.categoryIds, request.labelIds);
        invalidation.recordQuestion(id);
    }

    @Transactional
    public void changeStatus(long id, String status) {
        String value = normalizeStatus(status);
        if (jdbc.update("UPDATE club_question SET status=?,data_version=data_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?", value, id) == 0) {
            throw BizException.notFound("题目不存在");
        }
        invalidation.recordQuestion(id);
    }

    public Map<String, Object> detail(long id, boolean includeAnswer) {
        Map<String, Object> result = copyDetail(cache.get(id));
        if (!includeAnswer) {
            result.remove("referenceAnswer");
            Object options = result.get("options");
            if (options instanceof List) {
                for (Object option : (List<?>) options) if (option instanceof Map) ((Map<?, ?>) option).remove("correct");
            }
        }
        return result;
    }

    private Map<String, Object> copyDetail(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        Object options = source.get("options");
        if (options instanceof List) {
            List<Object> optionCopies = new ArrayList<>();
            for (Object option : (List<?>) options) {
                optionCopies.add(option instanceof Map ? new LinkedHashMap<>((Map<?, ?>) option) : option);
            }
            copy.put("options", optionCopies);
        }
        Object categories = source.get("categories");
        if (categories instanceof List) copy.put("categories", new ArrayList<>((List<?>) categories));
        Object labels = source.get("labels");
        if (labels instanceof List) copy.put("labels", new ArrayList<>((List<?>) labels));
        return copy;
    }

    public Map<String, Object> page(QuestionModels.QuestionQuery query) {
        StringBuilder from = new StringBuilder(" FROM club_question q WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (query.status != null && !query.status.trim().isEmpty()) { from.append(" AND q.status=:status"); params.addValue("status", query.status); }
        if (query.type != null && !query.type.trim().isEmpty()) { from.append(" AND q.question_type=:type"); params.addValue("type", normalizedType(query.type)); }
        if (query.keyword != null && !query.keyword.trim().isEmpty()) { from.append(" AND LOWER(q.name) LIKE :keyword"); params.addValue("keyword", "%" + query.keyword.trim().toLowerCase(Locale.ROOT) + "%"); }
        if (query.categoryId != null) { from.append(" AND EXISTS(SELECT 1 FROM club_question_category qc WHERE qc.question_id=q.id AND qc.category_id=:categoryId)"); params.addValue("categoryId", query.categoryId); }
        if (query.labelId != null) { from.append(" AND EXISTS(SELECT 1 FROM club_question_label ql WHERE ql.question_id=q.id AND ql.label_id=:labelId)"); params.addValue("labelId", query.labelId); }
        Long total = namedJdbc.queryForObject("SELECT COUNT(*)" + from, params, Long.class);
        params.addValue("limit", query.size).addValue("offset", (query.page - 1) * query.size);
        List<Map<String, Object>> rows = namedJdbc.query("SELECT q.id,q.name,q.difficulty,q.score,q.question_type,q.status,q.updated_at" + from + " ORDER BY q.id DESC LIMIT :limit OFFSET :offset",
                params, (rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id")); row.put("name", rs.getString("name"));
                    row.put("difficulty", rs.getInt("difficulty")); row.put("score", rs.getInt("score"));
                    row.put("type", rs.getString("question_type")); row.put("status", rs.getString("status"));
                    row.put("updatedAt", rs.getTimestamp("updated_at"));
                    return row;
                });
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows); result.put("total", total == null ? 0 : total); result.put("page", query.page); result.put("size", query.size);
        return result;
    }

    public List<Map<String, Object>> categories(Long parentId) {
        if (parentId == null) return jdbc.query("SELECT id,parent_id,name,sort_no FROM club_category WHERE parent_id IS NULL AND status='ENABLED' ORDER BY sort_no,id", this::categoryRow);
        return jdbc.query("SELECT id,parent_id,name,sort_no FROM club_category WHERE parent_id=? AND status='ENABLED' ORDER BY sort_no,id", this::categoryRow, parentId);
    }

    public List<QuestionModels.AggregateResponse> aggregate(long parentId) {
        List<Map<String, Object>> categories = categories(parentId);
        List<CompletableFuture<QuestionModels.AggregateResponse>> futures = new ArrayList<>();
        for (Map<String, Object> category : categories) {
            futures.add(CompletableFuture.supplyAsync(() -> labelsFor(category), labelExecutor));
        }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(totalTimeout);
        List<QuestionModels.AggregateResponse> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            long remaining = Math.min(branchTimeout, Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
            try {
                results.add(futures.get(i).get(remaining, TimeUnit.MILLISECONDS));
            } catch (Exception failure) {
                QuestionModels.AggregateResponse degraded = new QuestionModels.AggregateResponse();
                degraded.categoryId = ((Number) categories.get(i).get("id")).longValue();
                degraded.categoryName = String.valueOf(categories.get(i).get("name"));
                degraded.degraded = true;
                futures.get(i).cancel(true);
                results.add(degraded);
            }
        }
        return results;
    }

    public boolean isCorrect(long questionId, String answer) {
        String type = jdbc.queryForObject("SELECT question_type FROM club_question WHERE id=?", String.class, questionId);
        return handlers.get(type).isCorrect(questionId, answer);
    }

    private QuestionModels.AggregateResponse labelsFor(Map<String, Object> category) {
        QuestionModels.AggregateResponse value = new QuestionModels.AggregateResponse();
        value.categoryId = ((Number) category.get("id")).longValue();
        value.categoryName = String.valueOf(category.get("name"));
        value.labels = jdbc.query("SELECT id,name FROM club_label WHERE category_id=? AND status='ENABLED' ORDER BY id",
                (rs, n) -> { Map<String, Object> item = new LinkedHashMap<>(); item.put("id", rs.getLong(1)); item.put("name", rs.getString(2)); return item; }, value.categoryId);
        return value;
    }

    private Map<String, Object> categoryRow(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>(); row.put("id", rs.getLong("id"));
        long parent = rs.getLong("parent_id"); row.put("parentId", rs.wasNull() ? null : parent);
        row.put("name", rs.getString("name")); row.put("sortNo", rs.getInt("sort_no")); return row;
    }

    private void saveMappings(long id, List<Long> categories, List<Long> labels) {
        for (Long value : new LinkedHashSet<>(categories)) jdbc.update("INSERT INTO club_question_category(question_id,category_id) VALUES(?,?)", id, value);
        for (Long value : new LinkedHashSet<>(labels == null ? Collections.emptyList() : labels)) jdbc.update("INSERT INTO club_question_label(question_id,label_id) VALUES(?,?)", id, value);
    }

    private void validateReferences(List<Long> categories, List<Long> labels) {
        if (categories == null || categories.isEmpty()) throw BizException.badRequest("至少选择一个分类");
        for (Long id : categories) if (count("club_category", id) == 0) throw BizException.badRequest("分类不存在: " + id);
        if (labels != null) for (Long id : labels) if (count("club_label", id) == 0) throw BizException.badRequest("标签不存在: " + id);
    }

    private int count(String table, Long id) { Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id=?", Integer.class, id); return value == null ? 0 : value; }
    private String normalizedType(String type) { return type == null ? "" : type.trim().toUpperCase(Locale.ROOT); }
    private String normalizeStatus(String status) {
        String value = status == null ? "DRAFT" : status.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("DRAFT", "PUBLISHED", "OFFLINE").contains(value)) throw BizException.badRequest("题目状态不合法");
        return value;
    }
}
