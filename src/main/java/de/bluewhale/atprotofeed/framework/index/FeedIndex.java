package de.bluewhale.atprotofeed.framework.index;

import de.bluewhale.atprotofeed.framework.exception.IndexingException;
import de.bluewhale.atprotofeed.framework.feed.PostReference;
import org.springframework.lang.Nullable;

import java.time.Instant;

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
     * <p>Called by the framework after {@link de.bluewhale.atprotofeed.framework.feed.FeedProvider#shouldIndex(PostReference)}
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
     * @throws IndexingException if indexing fails after retries
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
     * @param feedId the feed identifier (null = all feeds)
     * @param olderThan cutoff timestamp
     * @return number of posts removed
     */
    int removePostsOlderThan(@Nullable String feedId, Instant olderThan);
}
