package com.dyh.club.platform.search;

import com.dyh.club.lock.DistributedLock;
import com.dyh.club.platform.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Instant;
import java.util.*;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final JdbcTemplate jdbc;
    private final RestTemplate http;
    private final boolean enabled;
    private final String node;
    private final String alias;

    public SearchService(JdbcTemplate jdbc,
                         @Value("${club.search.elasticsearch-enabled:false}") boolean enabled,
                         @Value("${club.search.nodes:http://localhost:9200}") String nodes,
                         @Value("${club.search.alias:club-subject-search}") String alias) {
        this.jdbc = jdbc; this.enabled = enabled;
        this.node = nodes.split(",")[0].replaceAll("/$", ""); this.alias = alias;
        SimpleClientHttpRequestFactory requestFactory=new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(500);requestFactory.setReadTimeout(1000);
        this.http=new RestTemplate(requestFactory);
    }

    @PostConstruct
    public void initialize() {
        jdbc.update("INSERT INTO club_search_checkpoint(id,last_question_id) SELECT 1,0 WHERE NOT EXISTS(SELECT 1 FROM club_search_checkpoint WHERE id=1)");
        if (enabled) {
            try { ensureIndex(); } catch (Exception e) { log.warn("Elasticsearch 初始化失败，搜索将自动降级", e); }
        }
    }

    public Map<String,Object> search(String keyword, Long categoryId, Long labelId, String type, String sort, int page, int size) {
        String clean = keyword == null ? "" : keyword.trim();
        int safePage = Math.max(1, page), safeSize = Math.max(1, Math.min(size, 20));
        if (enabled) {
            try { return elasticsearch(clean, categoryId, labelId, type, sort, safePage, safeSize); }
            catch (Exception e) { log.warn("Elasticsearch 查询失败，执行受限 MySQL 名称查询", e); }
        }
        if (safePage > 5) throw new BizException(503, "搜索服务暂不可用，降级查询仅支持前 5 页");
        return mysqlFallback(clean, categoryId, labelId, type, safePage, safeSize);
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> elasticsearch(String keyword, Long categoryId, Long labelId, String type, String sort, int page, int size) {
        List<Object> filters = new ArrayList<>();
        filters.add(Collections.singletonMap("term", Collections.singletonMap("status", "PUBLISHED")));
        if (categoryId != null) filters.add(Collections.singletonMap("term", Collections.singletonMap("categoryIds", categoryId)));
        if (labelId != null) filters.add(Collections.singletonMap("term", Collections.singletonMap("labelIds", labelId)));
        if (type != null && !type.trim().isEmpty()) filters.add(Collections.singletonMap("term", Collections.singletonMap("type", type.toUpperCase(Locale.ROOT))));
        Map<String,Object> bool = new LinkedHashMap<>(); bool.put("filter", filters);
        if (keyword.isEmpty()) bool.put("must", Collections.singletonMap("match_all", Collections.emptyMap()));
        else bool.put("must", Collections.singletonMap("multi_match", map("query", keyword, "fields", Arrays.asList("name^3", "analysis"))));
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("from", (page - 1) * size); body.put("size", size); body.put("query", Collections.singletonMap("bool", bool));
        body.put("highlight", map("pre_tags", Collections.singletonList("<mark>"), "post_tags", Collections.singletonList("</mark>"),
                "fields", map("name", Collections.emptyMap(), "analysis", Collections.emptyMap())));
        if ("latest".equalsIgnoreCase(sort)) body.put("sort", Arrays.asList(map("updatedAt", "desc"), map("_score", "desc")));
        Map<String,Object> response = exchange("POST", "/" + alias + "/_search", body);
        Map<String,Object> hits = (Map<String,Object>) response.get("hits");
        Map<String,Object> totalNode = (Map<String,Object>) hits.get("total");
        List<Map<String,Object>> records = new ArrayList<>();
        for (Map<String,Object> hit : (List<Map<String,Object>>) hits.get("hits")) {
            Map<String,Object> row = new LinkedHashMap<>((Map<String,Object>) hit.get("_source"));
            Map<String,List<String>> highlight = (Map<String,List<String>>) hit.get("highlight");
            if (highlight != null) {
                if (highlight.containsKey("name")) row.put("highlightName", highlight.get("name").get(0));
                if (highlight.containsKey("analysis")) row.put("highlightAnalysis", highlight.get("analysis").get(0));
            }
            records.add(row);
        }
        return page(records, ((Number) totalNode.get("value")).longValue(), page, size, "ELASTICSEARCH", false);
    }

    private Map<String,Object> mysqlFallback(String keyword, Long categoryId, Long labelId, String type, int page, int size) {
        StringBuilder where = new StringBuilder(" FROM club_question q WHERE q.status='PUBLISHED'");
        List<Object> args = new ArrayList<>();
        if (!keyword.isEmpty()) { where.append(" AND LOWER(q.name) LIKE ?"); args.add("%" + keyword.toLowerCase(Locale.ROOT) + "%"); }
        if (categoryId != null) { where.append(" AND EXISTS(SELECT 1 FROM club_question_category qc WHERE qc.question_id=q.id AND qc.category_id=?)"); args.add(categoryId); }
        if (labelId != null) { where.append(" AND EXISTS(SELECT 1 FROM club_question_label ql WHERE ql.question_id=q.id AND ql.label_id=?)"); args.add(labelId); }
        if (type != null && !type.trim().isEmpty()) { where.append(" AND q.question_type=?"); args.add(type.toUpperCase(Locale.ROOT)); }
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, args.toArray());
        args.add(size); args.add((page - 1) * size);
        List<Map<String,Object>> records = jdbc.query("SELECT q.id,q.name,q.difficulty,q.question_type AS type,q.updated_at AS updatedAt" + where + " ORDER BY q.id DESC LIMIT ? OFFSET ?",
                (rs, n) -> map("id", rs.getLong("id"), "name", rs.getString("name"), "highlightName", highlight(rs.getString("name"), keyword),
                        "difficulty", rs.getInt("difficulty"), "type", rs.getString("type"), "updatedAt", rs.getTimestamp("updatedAt")), args.toArray());
        return page(records, total == null ? 0 : total, page, size, "MYSQL_LIMITED", true);
    }

    public void indexQuestion(long id) {
        if (!enabled) return;
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT id,name,analysis_text,difficulty,question_type,status,data_version,updated_at FROM club_question WHERE id=?", id);
        if (rows.isEmpty()) { deleteDocument(id); return; }
        Map<String,Object> q = rows.get(0), doc = new LinkedHashMap<>();
        doc.put("id", id); doc.put("name", q.get("name")); doc.put("analysis", q.get("analysis_text"));
        doc.put("difficulty", q.get("difficulty")); doc.put("type", q.get("question_type")); doc.put("status", q.get("status"));
        doc.put("version", q.get("data_version")); doc.put("updatedAt", timestamp(q.get("updated_at")));
        doc.put("categoryIds", jdbc.queryForList("SELECT category_id FROM club_question_category WHERE question_id=?", Long.class, id));
        doc.put("labelIds", jdbc.queryForList("SELECT label_id FROM club_question_label WHERE question_id=?", Long.class, id));
        exchange("PUT", "/" + alias + "/_doc/" + id + "?refresh=false", doc);
    }

    public void deleteDocument(long id) { if (enabled) try { exchange("DELETE", "/" + alias + "/_doc/" + id, null); } catch (Exception ignored) { } }

    @DistributedLock(prefix="search:verify", key="'all'", leaseMillis=300000)
    @Scheduled(fixedDelayString="${club.search.verify-delay-millis:600000}")
    public void verifyAndRepair() {
        if (!enabled) return;
        int repaired = 0;
        try {
            for (Map<String,Object> row : jdbc.queryForList("SELECT id,data_version FROM club_question WHERE status='PUBLISHED' ORDER BY id")) {
                long id = ((Number) row.get("id")).longValue();
                try { Map<String,Object> response = exchange("GET", "/" + alias + "/_doc/" + id, null); @SuppressWarnings("unchecked") Map<String,Object> source=(Map<String,Object>)response.get("_source");long indexed=source==null||source.get("version")==null?-1:((Number)source.get("version")).longValue();if (!Boolean.TRUE.equals(response.get("found"))||indexed!=((Number)row.get("data_version")).longValue()) { indexQuestion(id); repaired++; } }
                catch (Exception missing) { indexQuestion(id); repaired++; }
            }
            jdbc.update("UPDATE club_search_checkpoint SET last_verified_at=CURRENT_TIMESTAMP,last_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=1");
            recordJob("search-verify", "SUCCESS", repaired, "漏同步校验完成");
        } catch (Exception e) {
            jdbc.update("UPDATE club_search_checkpoint SET last_error=?,updated_at=CURRENT_TIMESTAMP WHERE id=1", abbreviate(e.getMessage()));
            recordJob("search-verify", "FAILED", repaired, abbreviate(e.getMessage()));
        }
    }

    @DistributedLock(prefix="search:rebuild", key="'all'", leaseMillis=300000)
    public String rebuild() {
        if (!enabled) throw new BizException(503, "当前未启用 Elasticsearch");
        java.sql.Timestamp rebuildStarted=new java.sql.Timestamp(System.currentTimeMillis());
        String index = alias + "-" + Instant.now().toEpochMilli();
        createIndex(index);
        for (Map<String,Object> row : jdbc.queryForList("SELECT id FROM club_question ORDER BY id")) indexTo(index, ((Number) row.get("id")).longValue());
        for (Long changed : jdbc.queryForList("SELECT id FROM club_question WHERE updated_at>=? ORDER BY id",Long.class,rebuildStarted)) indexTo(index,changed);
        Map<String,Object> aliases = exchange("GET", "/_alias/" + alias, null);
        List<Object> actions = new ArrayList<>();
        for (String old : aliases.keySet()) actions.add(Collections.singletonMap("remove", map("index", old, "alias", alias)));
        actions.add(Collections.singletonMap("add", map("index", index, "alias", alias)));
        exchange("POST", "/_aliases", Collections.singletonMap("actions", actions));
        return index;
    }

    public Map<String,Object> status() {
        Map<String,Object> result = new LinkedHashMap<>(); result.put("enabled", enabled);
        if (!enabled) { result.put("status", "DISABLED"); return result; }
        try { Map<String,Object> health = exchange("GET", "/_cluster/health/" + alias, null); result.put("status", health.get("status")); result.put("documents", count()); }
        catch (Exception e) { result.put("status", "DOWN"); result.put("error", "搜索组件不可用，已启用受限降级"); }
        return result;
    }

    private void ensureIndex() {
        try { exchange("GET", "/_alias/" + alias, null); }
        catch (Exception notFound) { String index = alias + "-v1"; createIndex(index); exchange("POST", "/_aliases", Collections.singletonMap("actions", Collections.singletonList(Collections.singletonMap("add", map("index", index, "alias", alias))))); }
    }
    private void createIndex(String index) { exchange("PUT", "/" + index, map("settings", map("number_of_shards", 1, "number_of_replicas", 1), "mappings", map("properties", map(
            "id", map("type", "long"), "name", map("type", "text", "analyzer", "standard"), "analysis", map("type", "text", "analyzer", "standard"),
            "difficulty", map("type", "integer"), "type", map("type", "keyword"), "status", map("type", "keyword"), "version", map("type", "long"),
            "categoryIds", map("type", "long"), "labelIds", map("type", "long"), "updatedAt", map("type", "date", "format", "strict_date_optional_time"))))); }
    private void indexTo(String index,long id) { Map<String,Object> q=jdbc.queryForMap("SELECT id,name,analysis_text,difficulty,question_type,status,data_version,updated_at FROM club_question WHERE id=?",id); Map<String,Object> doc=map("id",id,"name",q.get("name"),"analysis",q.get("analysis_text"),"difficulty",q.get("difficulty"),"type",q.get("question_type"),"status",q.get("status"),"version",q.get("data_version"),"updatedAt",timestamp(q.get("updated_at")),"categoryIds",jdbc.queryForList("SELECT category_id FROM club_question_category WHERE question_id=?",Long.class,id),"labelIds",jdbc.queryForList("SELECT label_id FROM club_question_label WHERE question_id=?",Long.class,id)); exchange("PUT","/"+index+"/_doc/"+id,doc); }
    @SuppressWarnings("unchecked") private long count(){Map<String,Object> r=exchange("GET","/"+alias+"/_count",null);return ((Number)r.get("count")).longValue();}
    @SuppressWarnings("unchecked") private Map<String,Object> exchange(String method,String path,Object body){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);ResponseEntity<Map> r=http.exchange(URI.create(node+path),HttpMethod.resolve(method),new HttpEntity<>(body,h),Map.class);return r.getBody()==null?Collections.emptyMap():r.getBody();}
    private Map<String,Object> page(List<Map<String,Object>> records,long total,int page,int size,String source,boolean degraded){return map("records",records,"total",total,"page",page,"size",size,"source",source,"degraded",degraded);}
    private String highlight(String name,String keyword){if(keyword.isEmpty())return name;return name.replaceAll("(?i)"+java.util.regex.Pattern.quote(keyword),"<mark>$0</mark>");}
    private String timestamp(Object value){return value instanceof java.sql.Timestamp?((java.sql.Timestamp)value).toInstant().toString():String.valueOf(value);}
    private void recordJob(String name,String status,int count,String detail){jdbc.update("INSERT INTO club_job_run(job_name,status,processed_count,detail_text,finished_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)",name,status,count,detail);}
    private String abbreviate(String s){return s==null?"未知错误":s.substring(0,Math.min(480,s.length()));}
    private static Map<String,Object> map(Object... values){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put(String.valueOf(values[i]),values[i+1]);return m;}
}
