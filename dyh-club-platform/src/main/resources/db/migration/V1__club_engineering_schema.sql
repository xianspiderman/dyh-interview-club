CREATE TABLE IF NOT EXISTS club_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar VARCHAR(255),
    role_code VARCHAR(32) NOT NULL DEFAULT 'USER',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS club_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(64) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_category_parent_name UNIQUE (parent_id, name)
);

CREATE TABLE IF NOT EXISTS club_label (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_label_category_name UNIQUE (category_id, name)
);

CREATE TABLE IF NOT EXISTS club_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    analysis_text TEXT,
    difficulty INT NOT NULL DEFAULT 2,
    score INT NOT NULL DEFAULT 1,
    question_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    data_version BIGINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_question_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_code VARCHAR(8) NOT NULL,
    option_content VARCHAR(1000) NOT NULL,
    correct_flag BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_club_question_option UNIQUE (question_id, option_code)
);

CREATE TABLE IF NOT EXISTS club_question_brief (
    question_id BIGINT PRIMARY KEY,
    reference_answer TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS club_question_category (
    question_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (question_id, category_id)
);

CREATE TABLE IF NOT EXISTS club_question_label (
    question_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    PRIMARY KEY (question_id, label_id)
);

CREATE INDEX idx_club_question_filter ON club_question(status, question_type, id);
CREATE INDEX idx_club_question_category_filter ON club_question_category(category_id, question_id);
CREATE INDEX idx_club_question_label_filter ON club_question_label(label_id, question_id);

CREATE TABLE IF NOT EXISTS club_practice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    elapsed_seconds INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    total_count INT NOT NULL DEFAULT 0,
    version_no BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS club_practice_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_content VARCHAR(2000),
    answer_status VARCHAR(16) NOT NULL DEFAULT 'UNANSWERED',
    correct_flag BOOLEAN,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_practice_question UNIQUE (practice_id, question_id)
);

CREATE INDEX idx_club_practice_unfinished ON club_practice(user_id, status, started_at);

CREATE TABLE IF NOT EXISTS club_circle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(64) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED'
);

CREATE TABLE IF NOT EXISTS club_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    circle_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'VISIBLE',
    comment_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_club_post_circle ON club_post(circle_id, status, id);

CREATE TABLE IF NOT EXISTS club_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type VARCHAR(16) NOT NULL,
    biz_id BIGINT NOT NULL,
    parent_id BIGINT,
    root_id BIGINT,
    author_id BIGINT NOT NULL,
    to_user_id BIGINT,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'VISIBLE',
    audit_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_club_comment_roots ON club_comment(biz_type, biz_id, parent_id, status, created_at, id);
CREATE INDEX idx_club_comment_replies ON club_comment(root_id, status, created_at, id);

CREATE TABLE IF NOT EXISTS club_question_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    liked BOOLEAN NOT NULL DEFAULT FALSE,
    state_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_question_like UNIQUE (question_id, user_id)
);

CREATE TABLE IF NOT EXISTS club_sensitive_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_text VARCHAR(128) NOT NULL,
    list_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    dictionary_version BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_sensitive_word UNIQUE (word_text, list_type)
);

CREATE TABLE IF NOT EXISTS club_cache_invalidation_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    claimed_at TIMESTAMP,
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cache_task_status ON club_cache_invalidation_task(status, id);

CREATE TABLE IF NOT EXISTS club_content_recheck_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dictionary_version BIGINT NOT NULL,
    post_cursor BIGINT NOT NULL DEFAULT 0,
    comment_cursor BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_job_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    processed_count INT NOT NULL DEFAULT 0,
    detail_text VARCHAR(500),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE INDEX idx_club_job_name ON club_job_run(job_name, id);

CREATE TABLE IF NOT EXISTS club_search_checkpoint (
    id BIGINT PRIMARY KEY,
    binlog_position VARCHAR(255),
    last_question_id BIGINT,
    last_verified_at TIMESTAMP,
    last_error VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
