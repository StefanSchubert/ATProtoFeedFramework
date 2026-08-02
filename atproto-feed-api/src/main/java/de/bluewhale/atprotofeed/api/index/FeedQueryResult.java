package de.bluewhale.atprotofeed.api.index;

import de.bluewhale.atprotofeed.api.feed.PostReference;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Result of a feed query with pagination support.
 * 
 * @param posts the posts in this page
 * @param cursor optional cursor for next page (null if no more pages)
 * @param hasMore whether more pages are available
 */
public record FeedQueryResult(
    List<PostReference> posts,
    @Nullable String cursor,
    boolean hasMore
) {
    
    /**
     * Create an empty result.
     * 
     * @return empty feed query result
     */
    public static FeedQueryResult empty() {
        return new FeedQueryResult(List.of(), null, false);
    }
    
    /**
     * Create result with posts and cursor.
     * 
     * @param posts the posts
     * @param cursor next page cursor
     * @return new feed query result
     */
    public static FeedQueryResult of(List<PostReference> posts, String cursor) {
        return new FeedQueryResult(posts, cursor, cursor != null);
    }
}
