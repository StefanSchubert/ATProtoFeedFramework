package de.bluewhale.atprotofeed.framework.api.dto;

import de.bluewhale.atprotofeed.framework.feed.PostReference;
import org.springframework.lang.Nullable;

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
    
    public static FeedResponse of(List<PostReference> posts, String nextCursor) {
        return new FeedResponse(
            posts.stream()
                .map(FeedPost::from)
                .toList(),
            nextCursor
        );
    }
}
