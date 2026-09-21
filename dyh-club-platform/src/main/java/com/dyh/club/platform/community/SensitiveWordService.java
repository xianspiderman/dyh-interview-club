package com.dyh.club.platform.community;

import com.dyh.club.lock.DistributedLock;
import com.dyh.club.platform.common.BizException;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SensitiveWordService {
    private static final Logger log = LoggerFactory.getLogger(SensitiveWordService.class);
    private final JdbcTemplate jdbc;
    private final AtomicReference<Dictionary> dictionary = new AtomicReference<>(new Dictionary(0, new Node()));

    public SensitiveWordService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct public void initialize() { refresh(); }

    @Scheduled(fixedDelay = 60000)
    public void refresh() {
        Long version = jdbc.queryForObject("SELECT COALESCE(MAX(dictionary_version),0) FROM club_sensitive_word WHERE status='ENABLED'", Long.class);
        long current = version == null ? 0 : version;
        if (dictionary.get().version == current) return;
        try {
            Node root = new Node();
            jdbc.query("SELECT word_text,list_type FROM club_sensitive_word WHERE status='ENABLED' ORDER BY LENGTH(word_text) DESC",
                    (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                            add(root, normalize(rs.getString(1)), "WHITE".equals(rs.getString(2))));
            dictionary.set(new Dictionary(current, root));
        } catch (Exception error) {
            log.error("敏感词词典重建失败，继续使用旧版本 {}", dictionary.get().version, error);
        }
    }

    public void assertAllowed(String content) {
        Match match = firstBlackMatch(content);
        if (match != null) throw BizException.badRequest("内容包含不合规词语，请修改后再发布");
    }

    public Match firstBlackMatch(String content) {
        if (content == null || content.isEmpty()) return null;
        String text = normalize(content);
        Dictionary snapshot = dictionary.get();
        List<Match> whites = matches(text, snapshot.root, true);
        for (Match black : matches(text, snapshot.root, false)) {
            boolean covered = false;
            for (Match white : whites) if (white.start <= black.start && white.end >= black.end) { covered = true; break; }
            if (!covered) return black;
        }
        return null;
    }

    public long version() { return dictionary.get().version; }

    @Transactional
    public void saveWord(String word, String type) {
        String clean = word == null ? "" : word.trim();
        String listType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (clean.isEmpty() || clean.length() > 128) throw BizException.badRequest("词语不能为空且不能超过 128 个字符");
        if (!Arrays.asList("BLACK", "WHITE").contains(listType)) throw BizException.badRequest("名单类型只能是 BLACK 或 WHITE");
        Long current = jdbc.queryForObject("SELECT COALESCE(MAX(dictionary_version),0) FROM club_sensitive_word", Long.class);
        long next = (current == null ? 0 : current) + 1;
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM club_sensitive_word WHERE word_text=? AND list_type=?", Integer.class, clean, listType);
        if (count != null && count > 0) jdbc.update("UPDATE club_sensitive_word SET status='ENABLED',dictionary_version=?,updated_at=CURRENT_TIMESTAMP WHERE word_text=? AND list_type=?", next, clean, listType);
        else jdbc.update("INSERT INTO club_sensitive_word(word_text,list_type,status,dictionary_version) VALUES(?,?,'ENABLED',?)", clean, listType, next);
        jdbc.update("INSERT INTO club_content_recheck_task(dictionary_version,status) VALUES(?, 'PENDING')", next);
        refresh();
    }

    @Transactional
    public void disableWord(long id) {
        Long current = jdbc.queryForObject("SELECT COALESCE(MAX(dictionary_version),0) FROM club_sensitive_word", Long.class);
        long next = (current == null ? 0 : current) + 1;
        if (jdbc.update("UPDATE club_sensitive_word SET status='DISABLED',dictionary_version=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='ENABLED'", next, id) == 0) throw BizException.notFound("敏感词不存在");
        jdbc.update("INSERT INTO club_content_recheck_task(dictionary_version,status) VALUES(?, 'PENDING')", next);
        refresh();
    }

    public List<Map<String, Object>> listWords() { return jdbc.queryForList("SELECT id,word_text,list_type,status,dictionary_version,updated_at FROM club_sensitive_word ORDER BY id DESC"); }

    @Scheduled(fixedDelay = 60000)
    @XxlJob("sensitiveContentRecheck")
    @DistributedLock(prefix="job:sensitive-recheck",key="'all'",leaseMillis=55000)
    public void recheckPendingContent() {
        List<Map<String, Object>> tasks = jdbc.queryForList("SELECT id,post_cursor,comment_cursor FROM club_content_recheck_task WHERE status IN ('PENDING','RUNNING') ORDER BY id LIMIT 1");
        if (tasks.isEmpty()) return;
        Map<String, Object> task = tasks.get(0);
        long taskId = ((Number) task.get("id")).longValue();
        jdbc.update("UPDATE club_content_recheck_task SET status='RUNNING',updated_at=CURRENT_TIMESTAMP WHERE id=?", taskId);
        try {
            long postCursor = ((Number) task.get("post_cursor")).longValue();
            List<Map<String, Object>> posts = jdbc.queryForList("SELECT id,title,content,status FROM club_post WHERE id>? ORDER BY id LIMIT 500", postCursor);
            for (Map<String, Object> post : posts) {
                long id = ((Number) post.get("id")).longValue();
                Match match = firstBlackMatch(String.valueOf(post.get("title")) + " " + String.valueOf(post.get("content")));
                if (match != null) jdbc.update("UPDATE club_post SET status='HIDDEN_SENSITIVE',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status<>'HIDDEN_SENSITIVE'", id);
                else jdbc.update("UPDATE club_post SET status='VISIBLE',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='HIDDEN_SENSITIVE'", id);
                postCursor = id;
            }
            long commentCursor = ((Number) task.get("comment_cursor")).longValue();
            List<Map<String, Object>> comments = jdbc.queryForList("SELECT id,biz_type,biz_id,content,status FROM club_comment WHERE id>? ORDER BY id LIMIT 500", commentCursor);
            for (Map<String, Object> comment : comments) {
                long id = ((Number) comment.get("id")).longValue();
                Match match = firstBlackMatch(String.valueOf(comment.get("content")));
                String old = String.valueOf(comment.get("status"));
                if (match != null && "VISIBLE".equals(old)) {
                    if (jdbc.update("UPDATE club_comment SET status='HIDDEN_SENSITIVE',audit_reason='命中新版敏感词' WHERE id=? AND status='VISIBLE'", id) == 1 && "POST".equals(comment.get("biz_type"))) {
                        jdbc.update("UPDATE club_post SET comment_count=CASE WHEN comment_count>0 THEN comment_count-1 ELSE 0 END WHERE id=?", comment.get("biz_id"));
                    }
                } else if (match == null && "HIDDEN_SENSITIVE".equals(old)) {
                    if (jdbc.update("UPDATE club_comment SET status='VISIBLE',audit_reason=NULL WHERE id=? AND status='HIDDEN_SENSITIVE'", id) == 1 && "POST".equals(comment.get("biz_type"))) {
                        jdbc.update("UPDATE club_post SET comment_count=comment_count+1 WHERE id=?", comment.get("biz_id"));
                    }
                }
                commentCursor = id;
            }
            boolean done = posts.size() < 500 && comments.size() < 500;
            jdbc.update("UPDATE club_content_recheck_task SET post_cursor=?,comment_cursor=?,status=?,last_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    postCursor, commentCursor, done ? "DONE" : "RUNNING", taskId);
            recordRun("sensitive-content-recheck", "SUCCESS", posts.size() + comments.size(), done ? "复检完成" : "等待下一批");
        } catch (Exception error) {
            jdbc.update("UPDATE club_content_recheck_task SET status='PENDING',last_error=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", abbreviate(error.getMessage()), taskId);
            recordRun("sensitive-content-recheck", "FAILED", 0, abbreviate(error.getMessage()));
            log.error("存量内容复检失败，taskId={}", taskId, error);
        }
    }

    private void recordRun(String name, String status, int count, String detail) {
        jdbc.update("INSERT INTO club_job_run(job_name,status,processed_count,detail_text,finished_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", name, status, count, detail);
    }

    private String normalize(String input) {
        String value = Normalizer.normalize(input, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(value.length());
        for (int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if (Character.isWhitespace(c) || c=='*' || c=='-' || c=='_' || c=='.' || c=='·' || c=='—') continue;
            out.append(c);
        }
        return out.toString();
    }

    private void add(Node root, String word, boolean white) {
        if (word.isEmpty()) return;
        Node node=root;
        for (int i=0;i<word.length();i++) node=node.children.computeIfAbsent(word.charAt(i), ignored -> new Node());
        if (white) node.white=true; else node.black=true;
    }

    private List<Match> matches(String text, Node root, boolean white) {
        List<Match> found=new ArrayList<>();
        for(int start=0;start<text.length();start++) {
            Node node=root;
            for(int i=start;i<text.length();i++) {
                node=node.children.get(text.charAt(i)); if(node==null) break;
                if ((white && node.white) || (!white && node.black)) found.add(new Match(start,i+1,text.substring(start,i+1)));
            }
        }
        found.sort((a,b)->Integer.compare((b.end-b.start),(a.end-a.start)));
        return found;
    }

    private String abbreviate(String value) { return value == null ? "未知错误" : value.substring(0,Math.min(480,value.length())); }
    private static final class Dictionary { final long version; final Node root; Dictionary(long version,Node root){this.version=version;this.root=root;} }
    private static final class Node { final Map<Character,Node> children=new HashMap<>(); boolean black; boolean white; }
    public static final class Match { public final int start,end; public final String word; Match(int start,int end,String word){this.start=start;this.end=end;this.word=word;} }
}
