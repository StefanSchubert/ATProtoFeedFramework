package de.bluewhale.atprotofeed.framework.config;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Framework configuration properties bound from application.yml.
 * 
 * Prefix: atproto.feed
 */
@Component
@ConfigurationProperties(prefix = "atproto.feed")
@Validated
@Data
public class FrameworkProperties {
    
    /**
     * Jetstream WebSocket URL (e.g., wss://jetstream2.us-west.bsky.network/subscribe)
     */
    @NotBlank(message = "Jetstream URL must be configured (atproto.feed.jetstream-url)")
    @Pattern(regexp = "wss?://.*", message = "Jetstream URL must start with ws:// or wss://")
    private String jetstreamUrl;
    
    /**
     * Feed identifier (e.g., at://did:plc:xyz/app.bsky.feed.generator/my-feed)
     */
    @NotBlank(message = "Feed ID must be configured (atproto.feed.feed-id)")
    @Pattern(regexp = "at://.*", message = "Feed ID must be an ATProto URI (at://...)")
    private String feedId;
    
    /**
     * Reconnection configuration
     */
    private ReconnectConfig reconnect = new ReconnectConfig();
    
    /**
     * Retry configuration for event processing
     */
    private RetryConfig retry = new RetryConfig();
    
    /**
     * Cursor management configuration
     */
    private CursorConfig cursor = new CursorConfig();
    
    @Data
    public static class ReconnectConfig {
        /**
         * Initial delay in seconds before first reconnect attempt
         */
        @Min(value = 1, message = "Initial delay must be at least 1 second")
        private int initialDelaySeconds = 1;
        
        /**
         * Maximum delay in seconds between reconnect attempts
         */
        @Min(value = 1, message = "Max delay must be at least 1 second")
        @Max(value = 300, message = "Max delay cannot exceed 5 minutes")
        private int maxDelaySeconds = 30;
        
        /**
         * Maximum reconnection attempts (9999 = effectively unlimited)
         */
        @Min(value = 1, message = "Max attempts must be at least 1")
        private int maxAttempts = 9999;
    }
    
    @Data
    public static class RetryConfig {
        /**
         * Maximum retry attempts for failed events
         */
        @Min(value = 1, message = "Max retry attempts must be at least 1")
        @Max(value = 10, message = "Max retry attempts cannot exceed 10")
        private int maxAttempts = 3;
        
        /**
         * Backoff intervals in ISO-8601 duration format (e.g., ["1s", "2s", "4s"])
         */
        @NotNull(message = "Backoff intervals must be configured")
        @Size(min = 1, message = "At least one backoff interval is required")
        private List<Duration> backoffIntervals = List.of(
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            Duration.ofSeconds(4)
        );
    }
    
    @Data
    public static class CursorConfig {
        /**
         * Cursor time-to-live in hours
         */
        @Min(value = 1, message = "Cursor TTL must be at least 1 hour")
        @Max(value = 24, message = "Cursor TTL cannot exceed 24 hours")
        private int ttlHours = 1;
        
        /**
         * Cleanup interval in minutes for expired cursors
         */
        @Min(value = 1, message = "Cleanup interval must be at least 1 minute")
        @Max(value = 1440, message = "Cleanup interval cannot exceed 24 hours")
        private int cleanupIntervalMinutes = 15;
    }
}
