package de.bluewhale.atprotofeed.framework.feed;

import de.bluewhale.atprotofeed.framework.api.dto.FeedRequest;
import lombok.Builder;
import org.springframework.lang.Nullable;

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
    
    public static FeedContext from(FeedRequest request, List<PostReference> candidates) {
        return FeedContext.builder()
            .feedId(request.feed())
            .request(request)
            .candidatePosts(candidates)
            .build();
    }
    
    public int limit() {
        return request.limit();
    }
    
    public Optional<String> cursor() {
        return Optional.ofNullable(request.cursor());
    }
}
