package de.bluewhale.atprotofeed.framework.api.dto;

import org.springframework.lang.Nullable;

/**
 * ATProto feed query request.
 * 
 * Validates limit constraints per ATProto spec.
 */
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
