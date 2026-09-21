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
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final JdbcTemplate jdbc;
    private final RestTemplate http;
    private final boolean enabled;
    private final List<String> nodes;
    private final AtomicInteger nodeCursor=new AtomicInteger();
    private final ReentrantReadWriteLock rebuildCutoverLock = new ReentrantReadWriteLock(true);
    private final String alias;
    private final ObjectMapper mapper;

    public SearchService(JdbcTemplate jdbc,ObjectMapper mapper,
                         @Value("${club.search.elasticsearch-enabled:false}") boolean enabled,
                         @Value("${club.search.nodes:http://localhost:9200}") String nodes,
                         @Value("${club.search.alias:club-subject-search}") String alias) {
        this.jdbc = jdbc; this.mapper=mapper; this.enabled = enabled;
        List<String> configured=new ArrayList<>();for(String node:nodes.split(","))if(!node.trim().isEmpty())configured.add(node.trim().replaceAll("/$", ""));
        if(configured.isEmpty())throw new IllegalArgumentException("至少配置一个 Elasticsearch 节点");this.nodes=Collections.unmodifiableList(configured);this.alias = alias;
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
        else bool.put("must", Collections.singletonMap("multi_match", map("query", keyword, "fields", Arrays.asList("name^3", "answer^2", "analysis"))));
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("from", (page - 1) * size); body.put("size", size); body.put("query", Collections.singletonMap("bool", bool));
        body.put("highlight", map("pre_tags", Collections.singletonList("<mark>"), "post_tags", Collections.singletonList("</mark>"),
                "fields", map("name", Collections.emptyMap(), "answer",Collections.emptyMap(),"analysis", Collections.emptyMap())));
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
        if(enabled)bulkIndex(alias,Collections.singleton(id));
    }
    public void indexBatch(Set<Long> ids){if(!enabled)throw new IllegalStateException("Canal 已启用但 Elasticsearch 未启用");bulkIndex(alias,ids);}

    /**
     * Canal uses the read side of the cutover lock so a rebuilding index cannot
     * miss or resurrect a document in the final replay/alias-switch window.
     */
    @DistributedLock(prefix="search:cutover", key="'all'", waitMillis=1000, leaseMillis=1800000)
    public void indexCanalBatch(Set<Long> ids) {
        if (!enabled) {
            throw new IllegalStateException("Canal 已启用但 Elasticsearch 未启用");
        }
        rebuildCutoverLock.readLock().lock();
        try {
            bulkIndex(alias, ids);
            for (String rebuilding : jdbc.queryForList(
                    "SELECT target_index FROM club_search_rebuild WHERE status='BUILDING'", String.class)) {
                bulkIndex(rebuilding, ids);
            }
        } finally {
            rebuildCutoverLock.readLock().unlock();
        }
    }

    public void deleteDocument(long id) { if (enabled) try { exchange("DELETE", "/" + alias + "/_doc/" + id, null); } catch (Exception ignored) { } }

    @DistributedLock(prefix="search:verify", key="'all'", leaseMillis=300000)
    @Scheduled(fixedDelayString="${club.search.verify-delay-millis:600000}")
    public void verifyAndRepair() {
        if (!enabled) return;
        int repaired = 0;
        try {
            java.sql.Timestamp last=jdbc.queryForObject("SELECT last_verified_at FROM club_search_checkpoint WHERE id=1",java.sql.Timestamp.class);
            java.sql.Timestamp since=new java.sql.Timestamp((last==null?System.currentTimeMillis()-600000:last.getTime()-60000));
            for (Map<String,Object> row : jdbc.queryForList("SELECT id,data_version FROM club_question WHERE updated_at>=? ORDER BY id",since)) {
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

    @DistributedLock(prefix="search:cutover", key="'all'", leaseMillis=1800000)
    public String rebuild() {
        if (!enabled) throw new BizException(503, "当前未启用 Elasticsearch");
        String index = alias + "-" + Instant.now().toEpochMilli();
        String startPosition=jdbc.queryForObject("SELECT binlog_position FROM club_search_checkpoint WHERE id=1",String.class);
        Long startChange=jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM club_search_change_log",Long.class);
        jdbc.update("INSERT INTO club_search_rebuild(target_index,start_binlog_position,start_change_id,status) VALUES(?,?,?,'BUILDING')",index,startPosition,startChange==null?0:startChange);
        try {
        createIndex(index);long cursor=0;
        while(true){List<Long>page=jdbc.queryForList("SELECT id FROM club_question WHERE id>? ORDER BY id LIMIT 500",Long.class,cursor);if(page.isEmpty())break;bulkIndex(index,new LinkedHashSet<>(page));cursor=page.get(page.size()-1);}
        rebuildCutoverLock.writeLock().lock();
        try {
            Long replayTo=jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM club_search_change_log",Long.class);
            List<Long>changed=jdbc.queryForList("SELECT DISTINCT question_id FROM club_search_change_log WHERE id>? AND id<=? ORDER BY question_id",Long.class,startChange==null?0:startChange,replayTo==null?0:replayTo);
            for(int from=0;from<changed.size();from+=500)bulkIndex(index,new LinkedHashSet<>(changed.subList(from,Math.min(from+500,changed.size()))));
            Long expected=jdbc.queryForObject("SELECT COUNT(*) FROM club_question WHERE status='PUBLISHED'",Long.class);long actual=count(index);if(expected==null||actual!=expected)throw new IllegalStateException("重建校验失败 expected="+expected+", actual="+actual);
            Map<String,Object> aliases = exchange("GET", "/_alias/" + alias, null);
            List<Object> actions = new ArrayList<>();
            for (String old : aliases.keySet()) actions.add(Collections.singletonMap("remove", map("index", old, "alias", alias)));
            actions.add(Collections.singletonMap("add", map("index", index, "alias", alias)));
            exchange("POST", "/_aliases", Collections.singletonMap("actions", actions));
            jdbc.update("UPDATE club_search_rebuild SET status='COMPLETED',replayed_change_id=?,document_count=?,finished_at=CURRENT_TIMESTAMP WHERE target_index=?",replayTo==null?0:replayTo,actual,index);
            return index;
        } finally {
            rebuildCutoverLock.writeLock().unlock();
        }
        } catch(RuntimeException failure) {
            jdbc.update("UPDATE club_search_rebuild SET status='FAILED',detail_text=?,finished_at=CURRENT_TIMESTAMP WHERE target_index=?",abbreviate(failure.getMessage()),index);
            throw failure;
        }
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
    void bulkIndex(String target,Set<Long> requestedIds){
        if(requestedIds.isEmpty())return;if(requestedIds.size()>500)throw new IllegalArgumentException("单批题目 ID 不能超过 500 个");
        List<Long> ids=new ArrayList<>(requestedIds);String marks=String.join(",",Collections.nCopies(ids.size(),"?"));
        Map<Long,Map<String,Object>> questions=new LinkedHashMap<>();
        for(Map<String,Object> q:jdbc.queryForList("SELECT id,name,analysis_text,difficulty,question_type,status,data_version,updated_at FROM club_question WHERE id IN ("+marks+")",ids.toArray()))questions.put(((Number)q.get("id")).longValue(),q);
        Map<Long,List<Long>> categories=longGroups("SELECT question_id,category_id FROM club_question_category WHERE question_id IN ("+marks+")",ids);
        Map<Long,List<Long>> labels=longGroups("SELECT question_id,label_id FROM club_question_label WHERE question_id IN ("+marks+")",ids);
        Map<Long,List<String>> answers=new LinkedHashMap<>();
        for(Map<String,Object> row:jdbc.queryForList("SELECT question_id,option_content AS answer_text FROM club_question_option WHERE question_id IN ("+marks+") UNION ALL SELECT question_id,reference_answer AS answer_text FROM club_question_brief WHERE question_id IN ("+marks+")",concat(ids,ids).toArray())){
            long id=((Number)row.get("question_id")).longValue();answers.computeIfAbsent(id,k->new ArrayList<>()).add(String.valueOf(row.get("answer_text")));
        }
        Set<Long> remaining=new LinkedHashSet<>(ids);Map<Long,Map<String,Object>> docs=new LinkedHashMap<>();
        for(Long id:ids){Map<String,Object>q=questions.get(id);if(q!=null&&"PUBLISHED".equals(q.get("status"))){docs.put(id,map("id",id,"name",q.get("name"),"answer",String.join(" ",answers.getOrDefault(id,Collections.emptyList())),"analysis",q.get("analysis_text"),"difficulty",q.get("difficulty"),"type",q.get("question_type"),"status",q.get("status"),"version",q.get("data_version"),"updatedAt",timestamp(q.get("updated_at")),"categoryIds",categories.getOrDefault(id,Collections.emptyList()),"labelIds",labels.getOrDefault(id,Collections.emptyList())));}}
        for(int attempt=0;attempt<3&&!remaining.isEmpty();attempt++)remaining=bulkOnce(target,remaining,docs);
        if(!remaining.isEmpty())throw new IllegalStateException("Elasticsearch Bulk 仍有失败文档: "+remaining);
    }
    private Set<Long> bulkOnce(String target,Set<Long> ids,Map<Long,Map<String,Object>> docs){try{
        StringBuilder ndjson=new StringBuilder();for(Long id:ids){if(docs.containsKey(id)){ndjson.append(mapper.writeValueAsString(Collections.singletonMap("index",map("_index",target,"_id",id)))).append('\n');ndjson.append(mapper.writeValueAsString(docs.get(id))).append('\n');}else ndjson.append(mapper.writeValueAsString(Collections.singletonMap("delete",map("_index",target,"_id",id)))).append('\n');}
        Map<String,Object> response=bulkExchange(ndjson.toString());return failedIds(ids,response);
    }catch(Exception e){throw new IllegalStateException("Elasticsearch Bulk 请求失败",e);}}
    @SuppressWarnings("unchecked") private Map<String,Object> bulkExchange(String body){RuntimeException last=null;int start=Math.floorMod(nodeCursor.getAndIncrement(),nodes.size());for(int i=0;i<nodes.size();i++){try{HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.parseMediaType("application/x-ndjson"));ResponseEntity<Map>r=http.exchange(URI.create(nodes.get((start+i)%nodes.size())+"/_bulk?refresh=false"),HttpMethod.POST,new HttpEntity<>(body,h),Map.class);return r.getBody()==null?Collections.emptyMap():r.getBody();}catch(RuntimeException e){last=e;}}throw last==null?new IllegalStateException("Elasticsearch 节点不可用"):last;}
    private Map<Long,List<Long>> longGroups(String sql,List<Long>ids){Map<Long,List<Long>>out=new LinkedHashMap<>();for(Map<String,Object>r:jdbc.queryForList(sql,ids.toArray())){long id=((Number)r.get("question_id")).longValue();Object raw=r.containsKey("category_id")?r.get("category_id"):r.get("label_id");long value=((Number)raw).longValue();out.computeIfAbsent(id,k->new ArrayList<>()).add(value);}return out;}
    private List<Long> concat(List<Long>a,List<Long>b){List<Long>all=new ArrayList<>(a);all.addAll(b);return all;}
    @SuppressWarnings("unchecked") static Set<Long> failedIds(Set<Long>ids,Map<String,Object>response){List<Map<String,Object>>items=(List<Map<String,Object>>)response.get("items");if(items==null||items.size()!=ids.size())throw new IllegalStateException("Bulk 返回条目数不匹配");Set<Long>failed=new LinkedHashSet<>();int i=0;for(Long id:ids){Map<String,Object>action=(Map<String,Object>)items.get(i++).values().iterator().next();int status=((Number)action.get("status")).intValue();if(status>=300&&status!=404)failed.add(id);}return failed;}
    int configuredNodeCount(){return nodes.size();}
    static Map<String,Object> indexDefinition(){return map("settings",map("number_of_shards",1,"number_of_replicas",1),"mappings",map("properties",map(
            "id",map("type","long"),"name",map("type","text","analyzer","ik_max_word","search_analyzer","ik_smart"),"answer",map("type","text","analyzer","ik_max_word","search_analyzer","ik_smart"),"analysis",map("type","text","analyzer","ik_max_word","search_analyzer","ik_smart"),
            "difficulty",map("type","integer"),"type",map("type","keyword"),"status",map("type","keyword"),"version",map("type","long"),"categoryIds",map("type","long"),"labelIds",map("type","long"),"updatedAt",map("type","date","format","strict_date_optional_time"))));}
    private void createIndex(String index) { exchange("PUT", "/" + index,indexDefinition()); }
    @SuppressWarnings("unchecked") private long count(){return count(alias);} @SuppressWarnings("unchecked") private long count(String target){Map<String,Object> r=exchange("GET","/"+target+"/_count",null);return ((Number)r.get("count")).longValue();}
    @SuppressWarnings("unchecked") private Map<String,Object> exchange(String method,String path,Object body){RuntimeException last=null;int start=Math.floorMod(nodeCursor.getAndIncrement(),nodes.size());for(int i=0;i<nodes.size();i++){String node=nodes.get((start+i)%nodes.size());try{HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);ResponseEntity<Map> r=http.exchange(URI.create(node+path),HttpMethod.resolve(method),new HttpEntity<>(body,h),Map.class);return r.getBody()==null?Collections.emptyMap():r.getBody();}catch(RuntimeException e){last=e;}}throw last==null?new IllegalStateException("Elasticsearch 节点不可用"):last;}
    private Map<String,Object> page(List<Map<String,Object>> records,long total,int page,int size,String source,boolean degraded){return map("records",records,"total",total,"page",page,"size",size,"source",source,"degraded",degraded);}
    private String highlight(String name,String keyword){if(keyword.isEmpty())return name;return name.replaceAll("(?i)"+java.util.regex.Pattern.quote(keyword),"<mark>$0</mark>");}
    private String timestamp(Object value){return value instanceof java.sql.Timestamp?((java.sql.Timestamp)value).toInstant().toString():String.valueOf(value);}
    private void recordJob(String name,String status,int count,String detail){jdbc.update("INSERT INTO club_job_run(job_name,status,processed_count,detail_text,finished_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)",name,status,count,detail);}
    private String abbreviate(String s){return s==null?"未知错误":s.substring(0,Math.min(480,s.length()));}
    private static Map<String,Object> map(Object... values){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put(String.valueOf(values[i]),values[i+1]);return m;}
}
