# Data Model: Core Framework Implementation

**Date**: 2026-07-18  
**Status**: Phase 1 Design  
**Dependencies**: [research.md](./research.md)

## Overview

This document defines the persistence entities, domain objects, and data structures for the ATProtoFeedFramework. The design follows JPA/Hibernate conventions for database entities and uses Java records for immutable domain objects.

---

## 1. Database Entities (JPA)

### 1.1 PostReferenceEntity

Persistent representation of an indexed ATProto post.

**Table**: `post_references`

```java
@Entity
@Table(name = "post_references", indexes = {
    @Index(name = "idx_feed_timestamp", columnList = "feed_id, indexed_at DESC"),
    @Index(name = "idx_uri", columnList = "post_uri", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReferenceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ATProto post URI (e.g., at://did:plc:abc123/app.bsky.feed.post/3jui7kd)
     */
    @Column(name = "post_uri", nullable = false, unique = true, length = 500)
    private String postUri;
    
    /**
     * Feed identifier this post belongs to
     */
    @Column(name = "feed_id", nullable = false, length = 500)
    private String feedId;
    
    /**
     * Author DID (e.g., did:plc:abc123)
     */
    @Column(name = "author_did", nullable = false, length = 255)
    private String authorDid;
    
    /**
     * Post creation timestamp (from ATProto event)
     */
    @Column(name = "post_created_at", nullable = false)
    private Instant postCreatedAt;
    
    /**
     * Timestamp when indexed by framework
     */
    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;
    
    /**
     * Optional metadata for ranking/filtering (JSON column)
     * Examples: {"engagement": 42, "topics": ["tech"], "geo": "US-CA"}
     */
    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;
    
    /**
     * Optimistic locking
     */
    @Version
    private Long version;
}
```

**Index Strategy**:
- `idx_feed_timestamp`: Primary query pattern (feed queries sorted by `indexed_at DESC`)
- `idx_uri`: Deduplication constraint (prevent duplicate posts)

**SQL Schema** (Flyway: `V1__initial_schema.sql`):
```sql
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
```

---

### 1.2 PaginationCursorEntity

Persistent cursor state for pagination (1-hour TTL).

**Table**: `pagination_cursors`

```java
@Entity
@Table(name = "pagination_cursors", indexes = {
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationCursorEntity {
    
    @Id
    @Column(name = "cursor_token", length = 36)
    private String cursorToken;  // UUID v4
    
    @Column(name = "feed_id", nullable = false, length = 500)
    private String feedId;
    
    /**
     * Last post ID returned in previous page (for offset continuation)
     */
    @Column(name = "last_post_id", nullable = false)
    private Long lastPostId;
    
    /**
     * Last post timestamp (for tie-breaking)
     */
    @Column(name = "last_timestamp", nullable = false)
    private Instant lastTimestamp;
    
    @Column(name = "request_limit", nullable = false)
    private Integer requestLimit;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
```

**SQL Schema**:
```sql
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
```

---

### 1.3 DeadLetterEventEntity

Dead-letter queue for persistently failed events.

**Table**: `dead_letter_events`

```java
@Entity
@Table(name = "dead_letter_events", indexes = {
    @Index(name = "idx_status_failed", columnList = "status, failed_at DESC"),
    @Index(name = "idx_failure_reason", columnList = "failure_reason")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;
    
    @Column(name = "event_source", nullable = false, length = 100)
    private String eventSource;  // "jetstream"
    
    @Column(name = "feed_id", length = 500)
    private String feedId;
    
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;
    
    @Column(name = "payload_type", nullable = false, length = 100)
    private String payloadType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", nullable = false, length = 100)
    private FailureReason failureReason;
    
    @Column(name = "exception_class", length = 500)
    private String exceptionClass;
    
    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;
    
    @Column(name = "stack_trace", columnDefinition = "MEDIUMTEXT")
    private String stackTrace;
    
    @Column(name = "retry_attempts", nullable = false)
    private Integer retryAttempts;
    
    @Column(name = "last_retry_at")
    private Instant lastRetryAt;
    
    @Column(name = "retry_intervals_ms", length = 100)
    private String retryIntervalsMs;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DlqStatus status;
    
    @Column(name = "reprocess_attempts", nullable = false)
    private Integer reprocessAttempts;
    
    @Column(name = "last_reprocess_at")
    private Instant lastReprocessAt;
    
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    
    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;
    
    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

**Enums**:
```java
public enum FailureReason {
    PROCESSING_ERROR,
    VALIDATION_ERROR,
    DB_ERROR,
    TIMEOUT,
    UNKNOWN
}

public enum DlqStatus {
    PENDING_REVIEW,
    IN_RETRY,
    RESOLVED,
    DISCARDED
}
```

---

## 2. Domain Objects (Records)

### 2.1 RepositoryEvent

Immutable representation of an ATProto event from Jetstream.

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryEvent(
    String did,
    @JsonProperty("time_us") long timeUs,
    String kind,
    @JsonProperty("commit") CommitDetails commit,
    @JsonIgnore String rawPayload  // Original JSON for DLQ storage
) {
    
    public Instant timestamp() {
        return Instant.ofEpochMilli(timeUs / 1000);
    }
    
    public String postUri() {
        if (commit == null) return null;
        return String.format("at://%s/%s/%s", did, commit.collection(), commit.rkey());
    }
    
    public boolean isPostCreation() {
        return "commit".equals(kind) 
            && commit != null 
            && "create".equals(commit.operation())
            && "app.bsky.feed.post".equals(commit.collection());
    }
    
    public String eventId() {
        return commit != null ? commit.cid() : String.format("%s-%d", did, timeUs);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public record CommitDetails(
    String operation,
    String collection,
    String rkey,
    String cid,
    @JsonProperty("record") JsonNode record
) {}
```

---

### 2.2 PostReference

Immutable domain representation of an indexed post.

```java
@Builder
public record PostReference(
    @Nullable Long id,           // Null for new posts, populated after indexing
    String postUri,
    String authorDid,
    Instant postCreatedAt,
    Instant indexedAt,
    @Nullable Map<String, Object> metadata
) {
    
    public static PostReference fromEntity(PostReferenceEntity entity) {
        return PostReference.builder()
            .id(entity.getId())
            .postUri(entity.getPostUri())
            .authorDid(entity.getAuthorDid())
            .postCreatedAt(entity.getPostCreatedAt())
            .indexedAt(entity.getIndexedAt())
            .metadata(parseMetadata(entity.getMetadataJson()))
            .build();
    }
    
    public PostReferenceEntity toEntity(String feedId) {
        return PostReferenceEntity.builder()
            .postUri(postUri)
            .feedId(feedId)
            .authorDid(authorDid)
            .postCreatedAt(postCreatedAt)
            .indexedAt(indexedAt != null ? indexedAt : Instant.now())
            .metadataJson(serializeMetadata(metadata))
            .build();
    }
    
    private static Map<String, Object> parseMetadata(String json) {
        if (json == null) return Map.of();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }
    
    private static String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        return objectMapper.writeValueAsString(metadata);
    }
}
```

---

### 2.3 FeedContext

Contextual information provided to FeedProvider during selection.

```java
@Builder
public record FeedContext(
    String feedId,
    FeedRequest request,
    List<PostReference> candidatePosts,
    @Nullable Map<String, Object> userPreferences
) {
    
    public static FeedContext from(FeedRequest request, List<PostReference> candidates) {
        return FeedContext.builder()
            .feedId(request.feed())
            .request(request)
            .candidatePosts(candidates)
            .build();
    }
    
    public int limit() {
        return request.limit();
    }
    
    public Optional<String> cursor() {
        return Optional.ofNullable(request.cursor());
    }
}
```

---

### 2.4 FeedRequest

ATProto feed query request.

```java
public record FeedRequest(
    String feed,      // Feed URI (e.g., at://did:plc:xyz/app.bsky.feed.generator/my-feed)
    @Nullable String cursor,
    int limit         // Default: 50, max: 100
) {
    
    public FeedRequest {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }
    
    public static FeedRequest of(String feed, String cursor, Integer limit) {
        return new FeedRequest(feed, cursor, limit != null ? limit : 50);
    }
}
```

---

### 2.5 FeedResponse

ATProto feed query response.

```java
public record FeedResponse(
    List<FeedPost> feed,
    @Nullable String cursor
) {
    
    public static FeedResponse of(List<PostReference> posts, String nextCursor) {
        return new FeedResponse(
            posts.stream()
                .map(FeedPost::from)
                .toList(),
            nextCursor
        );
    }
}

public record FeedPost(String post) {
    public static FeedPost from(PostReference ref) {
        return new FeedPost(ref.postUri());
    }
}
```

---

## 3. Data Flow

```
ATProto Jetstream (JSON)
    ↓
RepositoryEvent (record)
    ↓
EventProcessor.process()
    ↓
PostReference (domain object)
    ↓
FeedProvider.selectPosts(FeedContext)
    ↓
PostReferenceEntity (JPA)
    ↓
MariaDB persistence
    ↓
FeedQuery → PostReference → FeedResponse
```

---

## 4. Entity Relationships

```
┌─────────────────────┐
│ PostReferenceEntity │ 1:1 with PostReference (domain)
│  - Indexed posts    │
└─────────────────────┘
          ↑
          │ feed_id
          │
┌──────────────────────┐
│ PaginationCursor     │ References last PostReference.id
│  - Pagination state  │
└──────────────────────┘
          
┌──────────────────────┐
│ DeadLetterEvent      │ Contains failed RepositoryEvent payload
│  - Failed events     │
└──────────────────────┘
```

---

## 5. Validation Rules

- **PostReferenceEntity.postUri**: Must match ATProto URI format (`at://did:plc:[a-z0-9]+/app.bsky.feed.post/[a-z0-9]+`)
- **PaginationCursorEntity.expiresAt**: Must be `createdAt + 1 hour`
- **DeadLetterEventEntity.retryAttempts**: Must be `3` (per spec FR-014)
- **FeedRequest.limit**: Must be `1 ≤ limit ≤ 100`
- **FeedResponse.cursor**: Required if more pages available, `null` on last page

---

## Next Steps

With data model defined, [contracts/](./contracts/) will document the interfaces (EventSource, FeedProvider, FeedIndex) that operate on these entities.
