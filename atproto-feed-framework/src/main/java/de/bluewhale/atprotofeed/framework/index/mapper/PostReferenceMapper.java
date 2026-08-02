package de.bluewhale.atprotofeed.framework.index.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.atprotofeed.api.feed.PostReference;
import de.bluewhale.atprotofeed.framework.index.entities.PostReferenceEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Mapper between API PostReference domain model and JPA PostReferenceEntity.
 * 
 * <p>This class bridges the gap between the framework-agnostic API layer
 * and the persistence layer, handling JSON serialization/deserialization
 * of metadata.
 */
@Component
public class PostReferenceMapper {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert JPA entity to API domain model.
     * 
     * @param entity the JPA entity
     * @return API post reference
     */
    public PostReference fromEntity(PostReferenceEntity entity) {
        return PostReference.builder()
            .id(entity.getId())
            .postUri(entity.getPostUri())
            .authorDid(entity.getAuthorDid())
            .postCreatedAt(entity.getPostCreatedAt())
            .indexedAt(entity.getIndexedAt())
            .metadata(parseMetadata(entity.getMetadataJson()))
            .build();
    }
    
    /**
     * Convert API domain model to JPA entity.
     * 
     * @param post the API post reference
     * @param feedId the feed identifier
     * @return JPA entity
     */
    public PostReferenceEntity toEntity(PostReference post, String feedId) {
        return PostReferenceEntity.builder()
            .postUri(post.postUri())
            .feedId(feedId)
            .authorDid(post.authorDid())
            .postCreatedAt(post.postCreatedAt())
            .indexedAt(post.indexedAt() != null ? post.indexedAt() : Instant.now())
            .metadataJson(serializeMetadata(post.metadata()))
            .build();
    }
    
    /**
     * Parse JSON metadata string to Map.
     * 
     * @param json JSON string
     * @return metadata map (empty if null or invalid)
     */
    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
    
    /**
     * Serialize metadata map to JSON string.
     * 
     * @param metadata metadata map
     * @return JSON string (null if empty)
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
