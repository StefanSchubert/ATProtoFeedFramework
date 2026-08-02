package de.bluewhale.atprotofeed.api.feed;

import de.bluewhale.atprotofeed.api.dto.FeedRequest;
import jakarta.annotation.Nullable;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contextual information provided to FeedProvider during selection.
 * 
 * Enables sophisticated ranking algorithms by providing candidate posts,
 * request context, and optional user preferences.
 */
@Builder
public record FeedContext(
    String feedId,
    FeedRequest request,
    List<PostReference> candidatePosts,
    @Nullable Map<String, Object> userPreferences
) {
    
    /**
     * Create feed context from request and candidates.
     * 
     * @param request the feed request
     * @param candidates the candidate posts
     * @return new feed context
     */
    public static FeedContext from(FeedRequest request, List<PostReference> candidates) {
        return FeedContext.builder()
            .feedId(request.feed())
            .request(request)
            .candidatePosts(candidates)
            .build();
    }
    
    /**
     * Get the requested page size limit.
     * 
     * @return page limit (1-100)
     */
    public int limit() {
        return request.limit();
    }
    
    /**
     * Get the pagination cursor if present.
     * 
     * @return optional cursor
     */
    public Optional<String> cursor() {
        return Optional.ofNullable(request.cursor());
    }
}
