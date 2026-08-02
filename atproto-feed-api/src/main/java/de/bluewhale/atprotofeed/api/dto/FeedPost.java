package de.bluewhale.atprotofeed.api.dto;

import de.bluewhale.atprotofeed.api.feed.PostReference;

/**
 * Single post entry in feed response.
 */
public record FeedPost(String post) {
    
    /**
     * Create feed post from post reference.
     * 
     * @param ref the post reference
     * @return feed post with URI
     */
    public static FeedPost from(PostReference ref) {
        return new FeedPost(ref.postUri());
    }
}
