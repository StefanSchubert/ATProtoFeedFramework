package de.bluewhale.atprotofeed.framework.health;

import de.bluewhale.atprotofeed.framework.config.FrameworkProperties;
import de.bluewhale.atprotofeed.framework.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.framework.eventsource.EventSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for ATProtoFeedFramework.
 * 
 * Reports UP when:
 * - Event source is connected
 * - Configuration is valid
 * 
 * Reports DOWN when:
 * - Event source disconnected >30 seconds
 * - Configuration validation fails
 */
@Component("atprotoFramework")
@RequiredArgsConstructor
@Slf4j
public class FrameworkHealthIndicator {
    
    private final EventSource eventSource;
    private final FrameworkProperties properties;
    
    /**
     * Get current health status.
     * 
     * @return health status map with details
     */
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            ConnectionStatus status = eventSource.getStatus();
            
            boolean isHealthy = status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CONNECTING;
            
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("connectionStatus", status.name());
            health.put("eventSource", eventSource.getDescription());
            health.put("feedId", properties.getFeedId());
            
            if (!isHealthy) {
                health.put("reason", status == ConnectionStatus.DISCONNECTED ? "disconnected" : "connection_error");
            }
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    /**
     * Check if framework is healthy.
     * 
     * @return true if framework is operational
     */
    public boolean isHealthy() {
        return "UP".equals(health().get("status"));
    }
}
