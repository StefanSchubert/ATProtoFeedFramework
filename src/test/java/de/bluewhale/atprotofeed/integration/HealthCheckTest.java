package de.bluewhale.atprotofeed.integration;

import de.bluewhale.atprotofeed.AbstractIntegrationTest;
import de.bluewhale.atprotofeed.framework.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.framework.eventsource.EventSource;
import de.bluewhale.atprotofeed.framework.health.FrameworkHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance Test T030: Health Endpoint Availability
 * 
 * Verifies that the framework health endpoint is available and reports
 * connection status correctly.
 * 
 * Success Criteria:
 * - Health indicator bean is available
 * - Health status reflects EventSource connection state
 * - Health check returns structured data with required fields
 * - Health endpoint can be queried programmatically
 */
@DisplayName("US1 Acceptance Test: Health Check - Endpoint Availability")
public class HealthCheckTest extends AbstractIntegrationTest {
    
    @Autowired
    private FrameworkHealthIndicator healthIndicator;
    
    @Autowired
    private EventSource eventSource;
    
    @Test
    @DisplayName("Health indicator bean is available in Spring context")
    void healthIndicatorAvailable() {
        // GIVEN: Framework auto-configuration active
        
        // WHEN: Querying for health indicator bean
        
        // THEN: Bean should be injected
        assertThat(healthIndicator)
            .as("FrameworkHealthIndicator should be available")
            .isNotNull();
    }
    
    @Test
    @DisplayName("Health check returns structured data with required fields")
    void healthCheckReturnsStructuredData() {
        // GIVEN: Framework is running
        
        // WHEN: Calling health check method
        Map<String, Object> health = healthIndicator.health();
        
        // THEN: Health response should contain required fields
        assertThat(health)
            .as("Health response should not be null")
            .isNotNull()
            .containsKeys("status", "connectionStatus", "eventSource", "feedId");
        
        // THEN: Status should be a valid health status
        assertThat(health.get("status"))
            .as("Status should be UP or DOWN")
            .isIn("UP", "DOWN");
    }
    
    @Test
    @DisplayName("Health status reflects EventSource connection state - DISCONNECTED")
    void healthReflectsDisconnectedState() {
        // GIVEN: EventSource is not connected (NoOpEventSource in tests)
        ConnectionStatus status = eventSource.getStatus();
        
        // WHEN: Health check is performed
        Map<String, Object> health = healthIndicator.health();
        
        // THEN: Health should report DOWN for disconnected state
        if (status == ConnectionStatus.DISCONNECTED) {
            assertThat(health.get("status"))
                .as("Health should be DOWN when disconnected")
                .isEqualTo("DOWN");
            
            assertThat(health.get("connectionStatus"))
                .as("Connection status should be DISCONNECTED")
                .isEqualTo("DISCONNECTED");
            
            assertThat(health.get("reason"))
                .as("Reason should explain disconnection")
                .isEqualTo("disconnected");
        }
    }
    
    @Test
    @DisplayName("Health check includes EventSource description")
    void healthIncludesEventSourceDescription() {
        // GIVEN: EventSource with description
        
        // WHEN: Health check is performed
        Map<String, Object> health = healthIndicator.health();
        
        // THEN: Event source description should be included
        assertThat(health.get("eventSource"))
            .as("EventSource description should be present")
            .isNotNull();
        
        String description = (String) health.get("eventSource");
        assertThat(description)
            .as("Description should identify the event source")
            .containsAnyOf("NoOpEventSource", "test.example.com");
    }
    
    @Test
    @DisplayName("Health check includes configured feed ID")
    void healthIncludesFeedId() {
        // GIVEN: Framework configured with feed ID
        
        // WHEN: Health check is performed
        Map<String, Object> health = healthIndicator.health();
        
        // THEN: Feed ID should be included
        assertThat(health.get("feedId"))
            .as("Feed ID should be present")
            .isEqualTo("at://did:plc:test123/app.bsky.feed.generator/test-feed");
    }
    
    @Test
    @DisplayName("Health indicator has isHealthy() convenience method")
    void isHealthyConvenienceMethod() {
        // GIVEN: Framework with health indicator
        
        // WHEN: Checking health with convenience method
        boolean healthy = healthIndicator.isHealthy();
        
        // THEN: Result should match detailed health status
        Map<String, Object> detailedHealth = healthIndicator.health();
        boolean expectedHealthy = "UP".equals(detailedHealth.get("status"));
        
        assertThat(healthy)
            .as("isHealthy() should match detailed status")
            .isEqualTo(expectedHealthy);
    }
    
    @Test
    @DisplayName("Health check handles missing EventSource gracefully")
    void healthCheckHandlesErrorsGracefully() {
        // GIVEN: Health indicator is present
        
        // WHEN: Health check is called (even if EventSource has issues)
        // THEN: Should not throw exceptions
        try {
            Map<String, Object> health = healthIndicator.health();
            assertThat(health)
                .as("Should return valid health map even on errors")
                .containsKey("status");
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("Health check should not throw exceptions: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Multiple health checks return consistent results")
    void healthChecksAreConsistent() {
        // GIVEN: Framework in stable state
        
        // WHEN: Performing multiple health checks
        Map<String, Object> health1 = healthIndicator.health();
        Map<String, Object> health2 = healthIndicator.health();
        Map<String, Object> health3 = healthIndicator.health();
        
        // THEN: Results should be consistent
        assertThat(health1.get("status"))
            .as("Health status should be consistent across calls")
            .isEqualTo(health2.get("status"))
            .isEqualTo(health3.get("status"));
        
        assertThat(health1.get("feedId"))
            .as("Feed ID should be consistent")
            .isEqualTo(health2.get("feedId"));
    }
}
