package de.bluewhale.atprotofeed.framework.feed;

import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Developer-implemented interface defining custom feed selection and ranking logic.
 * 
 * <p>The framework provides:
 * <ul>
 *   <li>Event ingestion (Jetstream WebSocket connection)
 *   <li>Post indexing (MariaDB persistence)
 *   <li>Feed API (ATProto Feed Generator endpoint)
 * </ul>
 * 
 * <p>Developers provide:
 * <ul>
 *   <li>Selection criteria (which posts belong in this feed?)
 *   <li>Ranking algorithm (in what order?)
 *   <li>Optional metadata enrichment
 * </ul>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Framework receives ATProto event
 *   <li>Event converted to {@link PostReference}
 *   <li>Framework calls {@link #shouldIndex(PostReference)} for each registered FeedProvider
 *   <li>If true, post indexed with {@code feed_id} from {@link #getFeedId()}
 *   <li>On feed query, framework calls {@link #selectPosts(FeedContext)}
 *   <li>FeedProvider returns selected/ranked posts
 *   <li>Framework serializes to ATProto response
 * </ol>
 * 
 * @see FeedContext
 * @see PostReference
 */
public interface FeedProvider {
    
    /**
     * Get the unique feed identifier (ATProto URI).
     * 
     * <p>Format: {@code at://<did>/app.bsky.feed.generator/<feedName>}
     * 
     * <p>Example: {@code at://did:plc:abc123/app.bsky.feed.generator/my-feed}
     * 
     * @return feed URI (must be stable, used as database key)
     */
    String getFeedId();
    
    /**
     * Determine if a post should be indexed for this feed.
     * 
     * <p>Called for every incoming ATProto post event. Return {@code true} to persist
     * the post to the feed index, {@code false} to skip.
     * 
     * <p><b>Performance note</b>: This method is called ~10,000 times/minute under load.
     * Keep logic simple and fast (&lt;1ms typical). Avoid external API calls.
     * 
     * <p><b>Exception handling</b>: Thrown exceptions trigger retry (3 attempts) then
     * dead-letter queue. The event stream continues.
     * 
     * @param post the post to evaluate
     * @return true if post should be indexed for this feed
     */
    boolean shouldIndex(PostReference post);
    
    /**
     * Select and rank posts for a feed query.
     * 
     * <p>The framework provides:
     * <ul>
     *   <li>{@code context.candidatePosts()} - All indexed posts for this feed (pre-filtered by feed_id)
     *   <li>{@code context.limit()} - Requested page size (1-100, from ATProto client)
     *   <li>{@code context.cursor()} - Optional pagination cursor
     *   <li>{@code context.userPreferences()} - Optional user context (future extension)
     * </ul>
     * 
     * <p>Implementations MUST:
     * <ul>
     *   <li>Return posts in desired order (e.g., reverse chronological, by score)
     *   <li>Respect {@code context.limit()} (return at most this many posts)
     *   <li>Handle empty {@code candidatePosts} gracefully (return empty list)
     * </ul>
     * 
     * <p>Implementations MAY:
     * <ul>
     *   <li>Apply additional filtering (e.g., remove blocked authors)
     *   <li>Boost posts based on scoring algorithms
     *   <li>Interleave different post types
     *   <li>Use {@code metadata} for ranking (engagement, topics, etc.)
     * </ul>
     * 
     * <p><b>Performance target</b>: &lt;500ms for 100k indexed posts (see spec SC-003)
     * 
     * @param context query context (request params, candidate posts)
     * @return selected and ranked posts (max {@code context.limit()} items)
     */
    List<PostReference> selectPosts(FeedContext context);
    
    /**
     * Enrich post metadata during indexing (optional hook).
     * 
     * <p>Called after {@link #shouldIndex(PostReference)} returns true, before persistence.
     * Use to extract/compute metadata for ranking:
     * <ul>
     *   <li>Engagement score (from ATProto record)
     *   <li>Topic tags (NLP classification)
     *   <li>Geographic location
     *   <li>Language detection
     * </ul>
     * 
     * <p>Metadata is stored as JSON in {@code post_references.metadata_json}.
     * 
     * <p>Default implementation: no enrichment (return original post).
     * 
     * @param post the post being indexed
     * @return enriched post with metadata
     */
    default PostReference enrichMetadata(PostReference post) {
        return post;
    }
}
