package de.bluewhale.atprotofeed.framework.index.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persistent representation of an indexed ATProto post.
 * 
 * Posts are stored with their URI, author, timestamps, and optional metadata
 * for feed generation and ranking.
 */
@Entity
@Table(name = "post_references", indexes = {
    @Index(name = "idx_feed_timestamp", columnList = "feed_id, indexed_at DESC"),
    @Index(name = "idx_uri", columnList = "post_uri", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReferenceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ATProto post URI (e.g., at://did:plc:abc123/app.bsky.feed.post/3jui7kd)
     */
    @Column(name = "post_uri", nullable = false, unique = true, length = 500)
    private String postUri;
    
    /**
     * Feed identifier this post belongs to
     */
    @Column(name = "feed_id", nullable = false, length = 500)
    private String feedId;
    
    /**
     * Author DID (e.g., did:plc:abc123)
     */
    @Column(name = "author_did", nullable = false, length = 255)
    private String authorDid;
    
    /**
     * Post creation timestamp (from ATProto event)
     */
    @Column(name = "post_created_at", nullable = false)
    private Instant postCreatedAt;
    
    /**
     * Timestamp when indexed by framework
     */
    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;
    
    /**
     * Optional metadata for ranking/filtering (JSON column)
     * Examples: {"engagement": 42, "topics": ["tech"], "geo": "US-CA"}
     */
    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;
    
    /**
     * Optimistic locking
     */
    @Version
    private Long version;
}
