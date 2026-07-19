package de.bluewhale.atprotofeed.framework.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.atprotofeed.framework.index.entities.PostReferenceEntity;
import lombok.Builder;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable domain representation of an indexed post.
 * 
 * Maps between domain logic and persistence (PostReferenceEntity).
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
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static PostReference fromEntity(PostReferenceEntity entity) {
        return PostReference.builder()
            .id(entity.getId())
            .postUri(entity.getPostUri())
            .authorDid(entity.getAuthorDid())
            .postCreatedAt(entity.getPostCreatedAt())
            .indexedAt(entity.getIndexedAt())
            .metadata(parseMetadata(entity.getMetadataJson()))
            .build();
    }
    
    public PostReferenceEntity toEntity(String feedId) {
        return PostReferenceEntity.builder()
            .postUri(postUri)
            .feedId(feedId)
            .authorDid(authorDid)
            .postCreatedAt(postCreatedAt)
            .indexedAt(indexedAt != null ? indexedAt : Instant.now())
            .metadataJson(serializeMetadata(metadata))
            .build();
    }
    
    private static Map<String, Object> parseMetadata(String json) {
        if (json == null) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
    
    private static String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
