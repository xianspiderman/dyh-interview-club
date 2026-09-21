package com.dyh.club.platform.search;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix="club.canal",name="enabled",havingValue="true")
public class CanalQuestionSync {
    private static final Logger log = LoggerFactory.getLogger(CanalQuestionSync.class);
    private final SearchService search;
    private final JdbcTemplate jdbc;
    private final CanalConnector connector;

    public CanalQuestionSync(SearchService search, JdbcTemplate jdbc,
                             @Value("${club.canal.host:localhost}") String host,
                             @Value("${club.canal.port:11111}") int port,
                             @Value("${club.canal.destination:example}") String destination) {
        this.search = search;
        this.jdbc = jdbc;
        this.connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(host, port), destination, "", "");
    }

    @PostConstruct
    public void start() {
        connector.connect();
        connector.subscribe(".*\\.club_question.*");
        connector.rollback();
    }

    @PreDestroy
    public void stop() {
        connector.disconnect();
    }

    @Scheduled(fixedDelay=1000)
    public void poll() {
        Message message = connector.getWithoutAck(500);
        long batchId = message.getId();
        if (batchId < 0 || message.getEntries().isEmpty()) {
            return;
        }
        try {
            Set<Long> ids = new LinkedHashSet<>();
            Map<Long, String> changes = new LinkedHashMap<>();
            for (CanalEntry.Entry entry : message.getEntries()) {
                if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                    continue;
                }
                CanalEntry.RowChange change = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                for (CanalEntry.RowData row : change.getRowDatasList()) {
                    List<CanalEntry.Column> columns = change.getEventType() == CanalEntry.EventType.DELETE
                            ? row.getBeforeColumnsList() : row.getAfterColumnsList();
                    String target = entry.getHeader().getTableName().equals("club_question")
                            ? "id" : "question_id";
                    for (CanalEntry.Column column : columns) {
                        if (target.equals(column.getName()) && !column.getValue().isEmpty()) {
                            long id = Long.parseLong(column.getValue());
                            ids.add(id);
                            changes.put(id, change.getEventType() == CanalEntry.EventType.DELETE
                                    ? "DELETE" : "UPSERT");
                        }
                    }
                }
            }

            for (Map.Entry<Long, String> change : changes.entrySet()) {
                jdbc.update("INSERT INTO club_search_change_log(question_id,change_type,binlog_position) VALUES(?,?,?)",
                        change.getKey(), change.getValue(), String.valueOf(batchId));
            }
            List<Long> all = new ArrayList<>(ids);
            for (int from = 0; from < all.size(); from += 500) {
                Set<Long> chunk = new LinkedHashSet<>(all.subList(from, Math.min(from + 500, all.size())));
                search.indexCanalBatch(chunk);
            }
            Long last = ids.stream().max(Long::compareTo).orElse(null);
            jdbc.update("UPDATE club_search_checkpoint SET binlog_position=?," +
                            "last_question_id=COALESCE(?,last_question_id),last_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=1",
                    String.valueOf(batchId), last);
            connector.ack(batchId);
        } catch (Exception e) {
            connector.rollback(batchId);
            String messageText = String.valueOf(e.getMessage());
            jdbc.update("UPDATE club_search_checkpoint SET last_error=?,updated_at=CURRENT_TIMESTAMP WHERE id=1",
                    messageText.substring(0, Math.min(480, messageText.length())));
            log.error("Canal 批次同步失败，已回滚 batchId={}", batchId, e);
        }
    }
}
