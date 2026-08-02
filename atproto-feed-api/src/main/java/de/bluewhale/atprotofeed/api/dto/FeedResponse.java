package de.bluewhale.atprotofeed.api.dto;

import de.bluewhale.atprotofeed.api.feed.PostReference;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * ATProto feed query response.
 * 
 * Contains post references and optional cursor for pagination.
 */
public record FeedResponse(
    List<FeedPost> feed,
    @Nullable String cursor
) {
    
    /**
     * Create feed response from post references.
     * 
     * @param posts the posts to include
     * @param nextCursor pagination cursor for next page
     * @return new feed response
     */
    public static FeedResponse of(List<PostReference> posts, String nextCursor) {
        return new FeedResponse(
            posts.stream()
                .map(FeedPost::from)
                .toList(),
            nextCursor
        );
    }
}
