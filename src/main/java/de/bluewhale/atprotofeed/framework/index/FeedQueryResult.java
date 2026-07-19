package de.bluewhale.atprotofeed.framework.index;

import de.bluewhale.atprotofeed.framework.feed.PostReference;
import org.springframework.lang.Nullable;

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
    
    public static FeedQueryResult empty() {
        return new FeedQueryResult(List.of(), null, false);
    }
    
    public static FeedQueryResult of(List<PostReference> posts, String cursor) {
        return new FeedQueryResult(posts, cursor, cursor != null);
    }
}
