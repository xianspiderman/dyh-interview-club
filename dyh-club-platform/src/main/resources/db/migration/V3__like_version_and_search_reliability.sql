CREATE TABLE IF NOT EXISTS club_like_version (
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    current_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id, user_id)
);

CREATE TABLE IF NOT EXISTS club_search_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    change_type VARCHAR(16) NOT NULL,
    binlog_position VARCHAR(128),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_search_change_time ON club_search_change_log(changed_at, id);

CREATE TABLE IF NOT EXISTS club_search_rebuild (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_index VARCHAR(160) NOT NULL,
    start_binlog_position VARCHAR(128),
    start_change_id BIGINT NOT NULL DEFAULT 0,
    replayed_change_id BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL,
    document_count BIGINT NOT NULL DEFAULT 0,
    detail_text VARCHAR(500),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL
);
