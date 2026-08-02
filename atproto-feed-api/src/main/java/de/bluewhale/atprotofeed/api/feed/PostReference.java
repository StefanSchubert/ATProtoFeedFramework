package de.bluewhale.atprotofeed.api.feed;

import jakarta.annotation.Nullable;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable domain representation of an indexed post.
 * 
 * <p>This is a pure domain model with no persistence concerns.
 * The framework provides mappers to convert between this model and JPA entities.
 */
@Builder
public record PostReference(
    @Nullable Long id,           // Null for new posts, populated after indexing
    String postUri,
    String authorDid,
    Instant postCreatedAt,
    Instant indexedAt,
    @Nullable Map<String, Object> metadata
) {
    
    /**
     * Create a new post reference with updated metadata.
     * 
     * @param newMetadata the new metadata
     * @return a new PostReference with updated metadata
     */
    public PostReference withMetadata(Map<String, Object> newMetadata) {
        return new PostReference(id, postUri, authorDid, postCreatedAt, indexedAt, newMetadata);
    }
    
    /**
     * Create a new post reference for indexing (sets indexedAt to now).
     * 
     * @return a new PostReference with current timestamp
     */
    public PostReference forIndexing() {
        return new PostReference(id, postUri, authorDid, postCreatedAt, Instant.now(), metadata);
    }
}
