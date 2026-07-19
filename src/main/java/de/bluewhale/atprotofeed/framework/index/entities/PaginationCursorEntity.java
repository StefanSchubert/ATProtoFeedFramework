package de.bluewhale.atprotofeed.framework.index.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persistent cursor state for pagination (1-hour TTL).
 * 
 * Cursors enable stateless pagination across feed queries by storing
 * the position in the result set.
 */
@Entity
@Table(name = "pagination_cursors", indexes = {
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationCursorEntity {
    
    @Id
    @Column(name = "cursor_token", length = 36)
    private String cursorToken;  // UUID v4
    
    @Column(name = "feed_id", nullable = false, length = 500)
    private String feedId;
    
    /**
     * Last post ID returned in previous page (for offset continuation)
     */
    @Column(name = "last_post_id", nullable = false)
    private Long lastPostId;
    
    /**
     * Last post timestamp (for tie-breaking)
     */
    @Column(name = "last_timestamp", nullable = false)
    private Instant lastTimestamp;
    
    @Column(name = "request_limit", nullable = false)
    private Integer requestLimit;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
