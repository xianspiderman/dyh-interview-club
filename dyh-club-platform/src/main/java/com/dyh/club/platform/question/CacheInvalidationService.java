package com.dyh.club.platform.question;

import com.dyh.club.lock.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Service
public class CacheInvalidationService {
    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);
    private final JdbcTemplate jdbc;
    private final QuestionCache cache;

    public CacheInvalidationService(JdbcTemplate jdbc, QuestionCache cache) { this.jdbc = jdbc; this.cache = cache; }

    public void recordQuestion(long questionId) {
        jdbc.update("INSERT INTO club_cache_invalidation_task(cache_key,status) VALUES(?, 'PENDING')", "club:question:detail:" + questionId);
        Runnable afterCommit = this::retry;
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { afterCommit.run(); }
            });
        } else afterCommit.run();
    }

    @Scheduled(fixedDelay = 5000)
    @DistributedLock(prefix="cache:invalidation",key="'retry'",leaseMillis=4500)
    public void retry() {
        jdbc.update("UPDATE club_cache_invalidation_task SET status='RETRY',updated_at=CURRENT_TIMESTAMP WHERE status='PROCESSING' AND claimed_at<?",
                new java.sql.Timestamp(System.currentTimeMillis()-60000));
        List<Map<String, Object>> tasks = jdbc.queryForList(
                "SELECT id,cache_key FROM club_cache_invalidation_task WHERE status IN ('PENDING','RETRY') ORDER BY id LIMIT 100");
        for (Map<String, Object> task : tasks) {
            long id = ((Number) task.get("id")).longValue();
            if (jdbc.update("UPDATE club_cache_invalidation_task SET status='PROCESSING',claimed_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('PENDING','RETRY')", id) == 0) continue;
            String key = String.valueOf(task.get("cache_key"));
            try {
                cache.evict(Long.parseLong(key.substring(key.lastIndexOf(':') + 1)));
                jdbc.update("UPDATE club_cache_invalidation_task SET status='DONE',updated_at=CURRENT_TIMESTAMP,last_error=NULL WHERE id=?", id);
            } catch (Exception error) {
                log.warn("缓存失效任务失败，id={}, key={}", id, key, error);
                jdbc.update("UPDATE club_cache_invalidation_task SET status='RETRY',retry_count=retry_count+1,last_error=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                        abbreviate(error.getMessage()), id);
            }
        }
    }

    private String abbreviate(String value) { return value == null ? "未知错误" : value.substring(0, Math.min(value.length(), 480)); }
}
