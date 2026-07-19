package de.bluewhale.atprotofeed.framework.api.dto;

import de.bluewhale.atprotofeed.framework.feed.PostReference;

/**
 * Single post-entry in feed response.
 */
public record FeedPost(String post) {
    
    public static FeedPost from(PostReference ref) {
        return new FeedPost(ref.postUri());
    }
}
