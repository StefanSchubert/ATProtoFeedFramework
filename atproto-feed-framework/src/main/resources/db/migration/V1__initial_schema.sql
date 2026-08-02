-- ATProtoFeedFramework Initial Schema
-- Version: 1
-- Description: Creates core tables for post indexing, pagination cursors, and dead-letter queue

-- ============================================================================
-- POST REFERENCES TABLE
-- Stores indexed ATProto posts for feed generation
-- ============================================================================
CREATE TABLE post_references (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    post_uri        VARCHAR(500)    NOT NULL,
    feed_id         VARCHAR(500)    NOT NULL,
    author_did      VARCHAR(255)    NOT NULL,
    post_created_at DATETIME(3)     NOT NULL,
    indexed_at      DATETIME(3)     NOT NULL,
    metadata_json   JSON            NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    
    PRIMARY KEY (id),
    UNIQUE KEY uq_post_uri (post_uri),
    INDEX idx_feed_timestamp (feed_id, indexed_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- PAGINATION CURSORS TABLE
-- Stores temporary cursor state for feed pagination (1-hour TTL)
-- ============================================================================
CREATE TABLE pagination_cursors (
    cursor_token    VARCHAR(36)     NOT NULL PRIMARY KEY,
    feed_id         VARCHAR(500)    NOT NULL,
    last_post_id    BIGINT          NOT NULL,
    last_timestamp  DATETIME(3)     NOT NULL,
    request_limit   INT             NOT NULL,
    created_at      DATETIME(3)     NOT NULL,
    expires_at      DATETIME(3)     NOT NULL,
    
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- DEAD LETTER EVENTS TABLE
-- Stores failed events for manual review and reprocessing
-- ============================================================================
CREATE TABLE dead_letter_events (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    event_id            VARCHAR(255)    NOT NULL,
    event_source        VARCHAR(100)    NOT NULL,
    feed_id             VARCHAR(500)    NULL,
    payload_json        LONGTEXT        NOT NULL,
    payload_type        VARCHAR(100)    NOT NULL,
    failure_reason      VARCHAR(100)    NOT NULL,
    exception_class     VARCHAR(500)    NULL,
    exception_message   TEXT            NULL,
    stack_trace         MEDIUMTEXT      NULL,
    retry_attempts      INT             NOT NULL DEFAULT 0,
    last_retry_at       DATETIME(3)     NULL,
    retry_intervals_ms  VARCHAR(100)    NULL,
    status              VARCHAR(50)     NOT NULL DEFAULT 'PENDING_REVIEW',
    reprocess_attempts  INT             NOT NULL DEFAULT 0,
    last_reprocess_at   DATETIME(3)     NULL,
    resolved_at         DATETIME(3)     NULL,
    resolved_by         VARCHAR(255)    NULL,
    failed_at           DATETIME(3)     NOT NULL,
    created_at          DATETIME(3)     NOT NULL,
    updated_at          DATETIME(3)     NOT NULL,
    
    PRIMARY KEY (id),
    UNIQUE KEY uq_event_id (event_id),
    INDEX idx_status_failed (status, failed_at DESC),
    INDEX idx_failure_reason (failure_reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
