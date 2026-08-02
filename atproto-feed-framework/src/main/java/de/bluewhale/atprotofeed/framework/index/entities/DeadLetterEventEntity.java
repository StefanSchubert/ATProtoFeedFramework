package de.bluewhale.atprotofeed.framework.index.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Dead-letter queue for persistently failed events.
 * 
 * Events that fail after all retry attempts are stored for manual review
 * and potential reprocessing.
 */
@Entity
@Table(name = "dead_letter_events", indexes = {
    @Index(name = "idx_status_failed", columnList = "status, failed_at DESC"),
    @Index(name = "idx_failure_reason", columnList = "failure_reason")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;
    
    @Column(name = "event_source", nullable = false, length = 100)
    private String eventSource;  // "jetstream"
    
    @Column(name = "feed_id", length = 500)
    private String feedId;
    
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;
    
    @Column(name = "payload_type", nullable = false, length = 100)
    private String payloadType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", nullable = false, length = 100)
    private FailureReason failureReason;
    
    @Column(name = "exception_class", length = 500)
    private String exceptionClass;
    
    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;
    
    @Column(name = "stack_trace", columnDefinition = "MEDIUMTEXT")
    private String stackTrace;
    
    @Column(name = "retry_attempts", nullable = false)
    private Integer retryAttempts;
    
    @Column(name = "last_retry_at")
    private Instant lastRetryAt;
    
    @Column(name = "retry_intervals_ms", length = 100)
    private String retryIntervalsMs;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DlqStatus status;
    
    @Column(name = "reprocess_attempts", nullable = false)
    private Integer reprocessAttempts;
    
    @Column(name = "last_reprocess_at")
    private Instant lastReprocessAt;
    
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    
    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;
    
    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
