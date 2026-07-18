# FeedProvider Interface Contract

**Package**: `de.bluewhale.atprotofeed.framework.feed`  
**Type**: Interface (Developer-facing API)  
**Audience**: Feed Application Developers

## Purpose

The `FeedProvider` interface is the **primary extension point** for developers building custom ATProto feeds. Developers implement this interface to define:
- **Selection logic**: Which posts belong in the feed?
- **Ranking logic**: In what order should posts appear?
- **Filtering**: Based on content, author, metadata, context

This interface was designed based on architecture review feedback to support future extensions: ranking algorithms, scoring, context-aware filtering, geodata, topic classification.

---

## Interface Definition

```java
package de.bluewhale.atprotofeed.framework.feed;

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
 * <h2>Example: Geographic Feed</h2>
 * <pre>{@code
 * @Component
 * public class BerlinFeedProvider implements FeedProvider {
 *     
 *     @Override
 *     public String getFeedId() {
 *         return "at://did:plc:example/app.bsky.feed.generator/berlin";
 *     }
 *     
 *     @Override
 *     public boolean shouldIndex(PostReference post) {
 *         // Index posts from Berlin users
 *         return post.metadata() != null 
 *             && "DE-BE".equals(post.metadata().get("geo"));
 *     }
 *     
 *     @Override
 *     public List<PostReference> selectPosts(FeedContext context) {
 *         // Return posts in reverse chronological order (default)
 *         // Could add custom ranking here (e.g., by engagement)
 *         return context.candidatePosts().stream()
 *             .sorted(Comparator.comparing(PostReference::indexedAt).reversed())
 *             .limit(context.limit())
 *             .toList();
 *     }
 * }
 * }</pre>
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
```

---

## FeedContext (Support Class)

```java
/**
 * Contextual information provided to FeedProvider during feed queries.
 * 
 * @param feedId feed being queried
 * @param request original ATProto request
 * @param candidatePosts all indexed posts for this feed (pre-filtered by framework)
 * @param userPreferences optional user-specific context (future: personalization)
 */
@Builder
public record FeedContext(
    String feedId,
    FeedRequest request,
    List<PostReference> candidatePosts,
    @Nullable Map<String, Object> userPreferences
) {
    
    /**
     * Requested page size (1-100).
     */
    public int limit() {
        return request.limit();
    }
    
    /**
     * Optional pagination cursor from previous page.
     */
    public Optional<String> cursor() {
        return Optional.ofNullable(request.cursor());
    }
}
```

---

## Implementation Examples

### Example 1: Simple Chronological Feed

```java
@Component
public class RecentPostsFeedProvider implements FeedProvider {
    
    @Override
    public String getFeedId() {
        return "at://did:plc:example/app.bsky.feed.generator/recent";
    }
    
    @Override
    public boolean shouldIndex(PostReference post) {
        // Index all posts
        return true;
    }
    
    @Override
    public List<PostReference> selectPosts(FeedContext context) {
        // Return newest first (reverse chronological)
        return context.candidatePosts().stream()
            .sorted(Comparator.comparing(PostReference::indexedAt).reversed())
            .limit(context.limit())
            .toList();
    }
}
```

**Lines of code**: 15 ✅ (meets SC-001: <100 LOC)

---

### Example 2: Author-Filtered Feed

```java
@Component
public class AuthorFeedProvider implements FeedProvider {
    
    @Value("${feed.author.did}")
    private String authorDid;
    
    @Override
    public String getFeedId() {
        return "at://did:plc:example/app.bsky.feed.generator/author-feed";
    }
    
    @Override
    public boolean shouldIndex(PostReference post) {
        return post.authorDid().equals(authorDid);
    }
    
    @Override
    public List<PostReference> selectPosts(FeedContext context) {
        return context.candidatePosts().stream()
            .sorted(Comparator.comparing(PostReference::postCreatedAt).reversed())
            .limit(context.limit())
            .toList();
    }
}
```

---

### Example 3: Topic-Based Feed with Scoring

```java
@Component
public class TechFeedProvider implements FeedProvider {
    
    private final TopicClassifier topicClassifier;
    
    @Override
    public String getFeedId() {
        return "at://did:plc:example/app.bsky.feed.generator/tech";
    }
    
    @Override
    public boolean shouldIndex(PostReference post) {
        // Index all posts (topic classification in enrichMetadata)
        return true;
    }
    
    @Override
    public PostReference enrichMetadata(PostReference post) {
        // Extract topics from post content (NLP)
        List<String> topics = topicClassifier.classify(post);
        
        if (!topics.contains("technology")) {
            return post; // No tech topic
        }
        
        // Add metadata for ranking
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("topics", topics);
        metadata.put("tech_score", calculateTechRelevance(post, topics));
        
        return post.withMetadata(metadata);
    }
    
    @Override
    public List<PostReference> selectPosts(FeedContext context) {
        return context.candidatePosts().stream()
            .filter(p -> hasTechTopic(p))  // Only tech posts
            .sorted(Comparator.comparing(this::getTechScore).reversed())  // Highest score first
            .limit(context.limit())
            .toList();
    }
    
    private boolean hasTechTopic(PostReference post) {
        if (post.metadata() == null) return false;
        List<String> topics = (List<String>) post.metadata().get("topics");
        return topics != null && topics.contains("technology");
    }
    
    private double getTechScore(PostReference post) {
        if (post.metadata() == null) return 0.0;
        return (double) post.metadata().getOrDefault("tech_score", 0.0);
    }
}
```

---

## Testing Guidelines

### Unit Test (Mock FeedProvider)

```java
@Test
void feedProviderFiltersPostsByAuthor() {
    var provider = new AuthorFeedProvider();
    provider.setAuthorDid("did:plc:author123");
    
    var post1 = PostReference.builder()
        .authorDid("did:plc:author123")
        .build();
    var post2 = PostReference.builder()
        .authorDid("did:plc:other456")
        .build();
    
    assertThat(provider.shouldIndex(post1)).isTrue();
    assertThat(provider.shouldIndex(post2)).isFalse();
}
```

### Integration Test (Full Pipeline)

```java
@SpringBootTest
class FeedProviderIntegrationTest {
    
    @Autowired
    private FeedIndex feedIndex;
    
    @Autowired
    private FeedProvider feedProvider;
    
    @Test
    void feedProviderIntegrationFlow() {
        // 1. Index posts
        var post = createTestPost();
        if (feedProvider.shouldIndex(post)) {
            feedIndex.index(post, feedProvider.getFeedId());
        }
        
        // 2. Query feed
        var context = FeedContext.builder()
            .feedId(feedProvider.getFeedId())
            .candidatePosts(feedIndex.findAll(feedProvider.getFeedId()))
            .request(FeedRequest.of(feedProvider.getFeedId(), null, 50))
            .build();
        
        var selected = feedProvider.selectPosts(context);
        
        assertThat(selected).isNotEmpty();
    }
}
```

---

## Performance Considerations

### shouldIndex() Performance

| Scenario | Target | Reasoning |
|----------|--------|-----------|
| Simple filter (DID check) | <0.1ms | ~10k calls/minute under load |
| Metadata extraction | <1ms | JSON parsing is fast |
| External API call | ❌ DON'T | Blocks event stream |

**Best Practice**: Precompute expensive logic in `enrichMetadata()`, store in `metadata_json`, use in `selectPosts()`.

### selectPosts() Performance

| Scenario | Target | Reasoning |
|----------|--------|-----------|
| Reverse chronological | <50ms | Sorting 100k posts |
| Score-based ranking | <200ms | Custom comparators |
| Complex filtering | <500ms | Spec requirement (SC-003) |

**Optimization**: If candidatePosts is large (>100k), consider:
1. Database-side filtering (custom FeedIndex queries)
2. Pre-computed indexes
3. Pagination cursor hints

---

## Extensibility (Future)

The FeedProvider interface is designed for extension without breaking changes:

**Future Enhancements**:
1. **Personalization**: `FeedContext.userPreferences()` → user-specific ranking
2. **Geodata Filtering**: `PostReference.metadata.geo` → location-based feeds
3. **Multi-Feed Aggregation**: `FeedProvider.getRelatedFeeds()` → feed discovery
4. **Real-Time Scoring**: `PostReference.metadata.engagement` → trending algorithms

**Backward Compatibility**: All future additions use `default` methods or optional fields.

---

## Related Contracts

- [FeedIndex Interface](./feedindex-interface.md) - Persistence layer for indexed posts
- [ATProto Feed API](./atproto-feed-api.md) - HTTP endpoint contract
- [EventSource Interface](./eventsource-interface.md) - Event ingestion
