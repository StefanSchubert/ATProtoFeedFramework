# FeedIndex Interface Contract

**Package**: `de.bluewhale.atprotofeed.framework.index`  
**Type**: Interface (SPI - Service Provider Interface)  
**Implementations**: `MariaDBFeedIndex`

## Purpose

The `FeedIndex` interface abstracts the persistence layer for indexed ATProto posts. It provides:
- **Indexing**: Store posts selected by FeedProviders
- **Querying**: Retrieve posts for feed generation
- **Pagination**: Cursor-based pagination for large feeds
- **Metrics**: Track index size and performance

Developers typically do not implement this interface; the framework provides `MariaDBFeedIndex` as the default implementation.

---

## Interface Definition

```java
package de.bluewhale.atprotofeed.framework.index;

/**
 * Persistence layer for ATProto feed posts.
 * 
 * <p>This interface abstracts the storage backend (MariaDB by default) and provides
 * CRUD operations for indexed posts.
 * 
 * <p>Thread safety: All methods are thread-safe and can be called concurrently.
 * 
 * <p>Performance targets:
 * <ul>
 *   <li>{@link #index(PostReference, String)} - &lt;10ms (indexing throughput: 10k/minute)
 *   <li>{@link #queryFeed(String, String, int)} - &lt;500ms for 100k posts
 *   <li>{@link #countPosts(String)} - &lt;50ms (indexed query)
 * </ul>
 * 
 * @see MariaDBFeedIndex
 */
public interface FeedIndex {
    
    /**
     * Index a post for a specific feed.
     * 
     * <p>Called by the framework after {@link FeedProvider#shouldIndex(PostReference)}
     * returns true. Posts are persisted with:
     * <ul>
     *   <li>Unique constraint: (feed_id, post_uri) - prevents duplicates
     *   <li>Index: feed_id + indexed_at DESC - for efficient chronological queries
     *   <li>Metadata: JSON column for custom ranking data
     * </ul>
     * 
     * <p>Duplicate posts (same feed_id + post_uri) are silently skipped (idempotent).
     * 
     * <p><b>Exception handling</b>: Transient failures (DB connection loss) trigger
     * retry (3 attempts) then dead-letter queue. Constraint violations are logged and skipped.
     * 
     * @param post the post to index (must not be null)
     * @param feedId the feed identifier (must not be null)
     * @throws IndexException if indexing fails after retries
     */
    void index(PostReference post, String feedId);
    
    /**
     * Query posts for a specific feed with pagination.
     * 
     * <p>Returns posts in the order defined by the FeedProvider's {@code selectPosts()}
     * logic. The framework handles:
     * <ul>
     *   <li>Cursor decoding (Base64 → timestamp)
     *   <li>Limit validation (1-100)
     *   <li>Empty result handling
     * </ul>
     * 
     * <p>Cursor format: {@code base64(indexed_at_timestamp)} (opaque to clients)
     * 
     * @param feedId the feed identifier
     * @param cursor optional pagination cursor (null for first page)
     * @param limit max posts to return (1-100)
     * @return paginated result with posts and next cursor
     */
    FeedQueryResult queryFeed(String feedId, @Nullable String cursor, int limit);
    
    /**
     * Count total indexed posts for a specific feed.
     * 
     * <p>Used for metrics and monitoring (Prometheus gauge: {@code atproto_feed_posts_total{feed_id}}).
     * 
     * @param feedId the feed identifier
     * @return total post count (0 if feed has no posts)
     */
    long countPosts(String feedId);
    
    /**
     * Remove posts older than specified timestamp (cleanup).
     * 
     * <p>Typically called by scheduled task (e.g., daily) to enforce retention policy.
     * 
     * <p>Example retention: Remove posts >30 days old.
     * 
     * @param feedId the feed identifier (null = all feeds)
     * @param olderThan cutoff timestamp (posts with indexed_at < olderThan are deleted)
     * @return number of posts deleted
     */
    int deleteOldPosts(@Nullable String feedId, Instant olderThan);
    
    /**
     * Get all feed IDs currently in the index.
     * 
     * <p>Used for:
     * <ul>
     *   <li>Discovery: List all registered feeds
     *   <li>Monitoring: Track feed growth
     *   <li>Testing: Verify FeedProvider registration
     * </ul>
     * 
     * @return set of feed IDs (empty if no posts indexed)
     */
    Set<String> getAllFeedIds();
    
    /**
     * Check if a specific post is already indexed for a feed.
     * 
     * <p>Used to avoid redundant indexing (though idempotent behavior handles this).
     * 
     * @param postUri the post URI (at://... format)
     * @param feedId the feed identifier
     * @return true if post exists in index
     */
    boolean exists(String postUri, String feedId);
    
    /**
     * Get index statistics (for health checks).
     * 
     * @return statistics (total posts, index size, oldest post timestamp)
     */
    IndexStatistics getStatistics();
}
```

---

## FeedQueryResult (Support Class)

```java
/**
 * Paginated query result from FeedIndex.
 * 
 * @param posts list of posts (max {@code limit} items)
 * @param nextCursor opaque pagination cursor (null if last page)
 */
public record FeedQueryResult(
    List<PostReference> posts,
    @Nullable String nextCursor
) {
    
    /**
     * Check if there are more pages.
     */
    public boolean hasMore() {
        return nextCursor != null;
    }
}
```

---

## IndexStatistics (Support Class)

```java
/**
 * Aggregate statistics about the feed index.
 * 
 * @param totalPosts total posts across all feeds
 * @param feedCount number of distinct feeds
 * @param oldestPostTimestamp timestamp of oldest indexed post (null if empty)
 * @param indexSizeBytes approximate storage size in bytes
 */
public record IndexStatistics(
    long totalPosts,
    int feedCount,
    @Nullable Instant oldestPostTimestamp,
    long indexSizeBytes
) {}
```

---

## Implementation: MariaDBFeedIndex

### Database Schema

```sql
CREATE TABLE post_references (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feed_id VARCHAR(255) NOT NULL,
    post_uri VARCHAR(512) NOT NULL,
    author_did VARCHAR(255) NOT NULL,
    post_created_at TIMESTAMP NOT NULL,
    indexed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata_json JSON,
    
    UNIQUE KEY uk_feed_post (feed_id, post_uri),
    INDEX idx_feed_indexed (feed_id, indexed_at DESC)
) ENGINE=InnoDB;
```

**Key Indexes**:
1. **uk_feed_post**: Ensures no duplicate posts per feed (idempotent inserts)
2. **idx_feed_indexed**: Optimizes reverse chronological queries (meets SC-003: <500ms for 100k posts)

### Configuration

```yaml
atproto:
  framework:
    index:
      retention-days: 30  # Auto-delete posts older than 30 days
      cleanup-cron: "0 2 * * *"  # Daily at 2 AM
```

### Transaction Behavior

- **index()**: Single-row INSERT with `ON DUPLICATE KEY UPDATE` (idempotent)
- **queryFeed()**: Read-only, no transaction needed
- **deleteOldPosts()**: Batched deletes (1000 rows per transaction)

---

## Usage Examples

### Index a Post (Framework-Invoked)

```java
@Service
public class EventProcessor implements EventHandler {
    
    private final FeedIndex feedIndex;
    private final List<FeedProvider> feedProviders;
    
    @Override
    public void handle(RepositoryEvent event) {
        var post = toPostReference(event);
        
        for (FeedProvider provider : feedProviders) {
            if (provider.shouldIndex(post)) {
                // Enrich metadata
                var enriched = provider.enrichMetadata(post);
                
                // Index
                feedIndex.index(enriched, provider.getFeedId());
            }
        }
    }
}
```

### Query Feed (Controller)

```java
@RestController
public class FeedController {
    
    private final FeedIndex feedIndex;
    
    @GetMapping("/xrpc/app.bsky.feed.getFeedSkeleton")
    public FeedSkeletonResponse getFeed(
        @RequestParam String feed,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "50") int limit
    ) {
        // Query index
        var result = feedIndex.queryFeed(feed, cursor, limit);
        
        // Convert to ATProto response
        return FeedSkeletonResponse.builder()
            .feed(result.posts().stream()
                .map(p -> new FeedPost(p.postUri()))
                .toList())
            .cursor(result.nextCursor())
            .build();
    }
}
```

### Cleanup Old Posts (Scheduled Task)

```java
@Component
public class IndexCleanupTask {
    
    private final FeedIndex feedIndex;
    
    @Value("${atproto.framework.index.retention-days}")
    private int retentionDays;
    
    @Scheduled(cron = "${atproto.framework.index.cleanup-cron}")
    public void cleanupOldPosts() {
        var cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = feedIndex.deleteOldPosts(null, cutoff);
        
        log.info("Deleted {} posts older than {} days", deleted, retentionDays);
    }
}
```

---

## Pagination Details

### Cursor Format

**Encoding**: Base64(JSON(`{"indexed_at": "<timestamp>", "id": <row_id>}`))

**Example**:
```json
{"indexed_at": "2024-01-15T10:30:00Z", "id": 123456}
```
→ Base64: `eyJpbmRleGVkX2F0IjoiMjAyNC0wMS0xNVQxMDozMDowMFoiLCJpZCI6MTIzNDU2fQ==`

**SQL Query**:
```sql
SELECT * FROM post_references
WHERE feed_id = ?
  AND (indexed_at, id) < (?, ?)  -- Cursor position
ORDER BY indexed_at DESC, id DESC
LIMIT ?
```

**Why `id` in cursor?** Handles tie-breaking when multiple posts have identical `indexed_at` timestamps.

### Pagination Edge Cases

| Scenario | Behavior |
|----------|----------|
| `cursor=null` | Return first page |
| `cursor` expired (>1h old) | Restart from beginning (return first page) |
| `cursor` malformed | Return 400 Bad Request |
| No more posts | Return empty list, `nextCursor=null` |
| Exactly `limit` posts | Include `nextCursor` (may be empty next page) |

---

## Error Handling

### Indexing Failures

**Scenario**: Database connection lost during `index()`.

**Behavior**:
1. Log error: `"Failed to index post {uri} for feed {feedId}, retrying..."`
2. Retry 3 times (1s, 2s, 4s backoff)
3. If still failing → dead-letter queue
4. Increment metric: `atproto_index_errors_total{reason="db_connection"}`

### Query Failures

**Scenario**: Slow query timeout (>5s).

**Behavior**:
1. Log warning: `"Query timeout for feed {feedId}, returning partial results"`
2. Return empty result: `FeedQueryResult(emptyList(), null)`
3. Increment metric: `atproto_index_query_timeout_total`

---

## Performance Benchmarks

### Target Performance (from Spec SC-003, SC-004)

| Operation | Target | Measurement |
|-----------|--------|-------------|
| `index()` | <10ms | 99th percentile |
| `queryFeed()` (100k posts) | <500ms | 99th percentile |
| `countPosts()` | <50ms | Median |
| `deleteOldPosts()` (10k rows) | <2s | Mean |

### Optimization Strategies

**For index():**
- Batch inserts (100 posts per transaction) → 10× throughput
- Async indexing with queue → non-blocking event stream

**For queryFeed():**
- Covering index: `(feed_id, indexed_at DESC, post_uri)` → avoid table lookups
- Limit result set: Always use `LIMIT` clause

**For countPosts():**
- Materialized counts: Update counter on insert/delete
- Cache TTL: 1 minute (acceptable staleness)

---

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `atproto.index.posts.total` | Gauge | Total indexed posts (per feed_id) |
| `atproto.index.operation.duration` | Histogram | Operation latency (index, query, delete) |
| `atproto.index.errors.total` | Counter | Indexing failures (per reason) |
| `atproto.index.query.timeout.total` | Counter | Query timeouts |
| `atproto.index.size.bytes` | Gauge | Approximate storage size |

---

## Testing Guidelines

### Unit Test (H2 In-Memory Database)

```java
@DataJpaTest
class FeedIndexTest {
    
    @Autowired
    private FeedIndex feedIndex;
    
    @Test
    void indexPostSuccessfully() {
        var post = PostReference.builder()
            .postUri("at://did:plc:123/app.bsky.feed.post/abc")
            .authorDid("did:plc:123")
            .build();
        
        feedIndex.index(post, "test-feed");
        
        assertThat(feedIndex.exists(post.postUri(), "test-feed")).isTrue();
        assertThat(feedIndex.countPosts("test-feed")).isEqualTo(1);
    }
    
    @Test
    void paginationReturnsCorrectPages() {
        // Index 150 posts
        for (int i = 0; i < 150; i++) {
            feedIndex.index(createTestPost(i), "test-feed");
        }
        
        // Page 1
        var page1 = feedIndex.queryFeed("test-feed", null, 50);
        assertThat(page1.posts()).hasSize(50);
        assertThat(page1.hasMore()).isTrue();
        
        // Page 2
        var page2 = feedIndex.queryFeed("test-feed", page1.nextCursor(), 50);
        assertThat(page2.posts()).hasSize(50);
        
        // Page 3
        var page3 = feedIndex.queryFeed("test-feed", page2.nextCursor(), 50);
        assertThat(page3.posts()).hasSize(50);
        assertThat(page3.hasMore()).isFalse();
    }
}
```

### Integration Test (Testcontainers MariaDB)

```java
@SpringBootTest
@Testcontainers
class MariaDBFeedIndexIntegrationTest {
    
    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.0")
        .withDatabaseName("testdb");
    
    @Test
    void performanceUnder100kPosts() {
        // Index 100k posts
        for (int i = 0; i < 100_000; i++) {
            feedIndex.index(createTestPost(i), "load-test-feed");
        }
        
        // Query with timer
        var start = System.currentTimeMillis();
        var result = feedIndex.queryFeed("load-test-feed", null, 50);
        var duration = System.currentTimeMillis() - start;
        
        assertThat(duration).isLessThan(500);  // SC-003 requirement
        assertThat(result.posts()).hasSize(50);
    }
}
```

---

## Related Contracts

- [FeedProvider Interface](./feedprovider-interface.md) - Uses FeedIndex for querying candidate posts
- [ATProto Feed API](./atproto-feed-api.md) - REST endpoint that queries FeedIndex
- [Data Model](../data-model.md) - Database schema details
