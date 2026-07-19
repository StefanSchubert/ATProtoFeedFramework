package de.bluewhale.atprotofeed.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Framework configuration properties bound from application.yml.
 * 
 * Prefix: atproto.feed
 */
@Component
@ConfigurationProperties(prefix = "atproto.feed")
@Data
public class FrameworkProperties {
    
    /**
     * Jetstream WebSocket URL (e.g., wss://jetstream2.us-west.bsky.network/subscribe)
     */
    private String jetstreamUrl;
    
    /**
     * Feed identifier (e.g., at://did:plc:xyz/app.bsky.feed.generator/my-feed)
     */
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
        private int initialDelaySeconds = 1;
        
        /**
         * Maximum delay in seconds between reconnect attempts
         */
        private int maxDelaySeconds = 30;
        
        /**
         * Maximum reconnection attempts (9999 = effectively unlimited)
         */
        private int maxAttempts = 9999;
    }
    
    @Data
    public static class RetryConfig {
        /**
         * Maximum retry attempts for failed events
         */
        private int maxAttempts = 3;
        
        /**
         * Backoff intervals in ISO-8601 duration format (e.g., ["1s", "2s", "4s"])
         */
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
        private int ttlHours = 1;
        
        /**
         * Cleanup interval in minutes for expired cursors
         */
        private int cleanupIntervalMinutes = 15;
    }
}
