package de.bluewhale.atprotofeed.integration;

import de.bluewhale.atprotofeed.AbstractIntegrationTest;
import de.bluewhale.atprotofeed.framework.config.FrameworkAutoConfiguration;
import de.bluewhale.atprotofeed.framework.config.FrameworkProperties;
import de.bluewhale.atprotofeed.framework.eventsource.EventSource;
import de.bluewhale.atprotofeed.framework.health.FrameworkHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance Test T029: Successful Initialization
 * 
 * Verifies that the framework starts up successfully with minimal configuration.
 * 
 * Success Criteria:
 * - Spring Boot application context loads without errors
 * - Framework auto-configuration is triggered
 * - All required beans are created
 * - Configuration validation passes
 * - Framework is ready to accept FeedProvider implementations
 */
@DisplayName("US1 Acceptance Test: Framework Startup - Successful Initialization")
public class FrameworkStartupTest extends AbstractIntegrationTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Autowired
    private FrameworkProperties properties;
    
    @Autowired
    private EventSource eventSource;
    
    @Autowired
    private FrameworkHealthIndicator healthIndicator;
    
    @Test
    @DisplayName("Spring Boot application context loads successfully with framework enabled")
    void applicationContextLoadsSuccessfully() {
        // GIVEN: Minimal framework configuration in application-test.yml
        
        // WHEN: Spring Boot application starts
        
        // THEN: Application context should be created
        assertThat(context).isNotNull();
        assertThat(context.getDisplayName()).isNotBlank();
    }
    
    @Test
    @DisplayName("Framework auto-configuration is triggered and active")
    void autoConfigurationTriggered() {
        // GIVEN: Framework on classpath with META-INF/spring.factories
        
        // WHEN: Spring Boot scans for auto-configurations
        
        // THEN: FrameworkAutoConfiguration should be loaded
        assertThat(context.getBeansOfType(FrameworkAutoConfiguration.class))
            .as("Auto-configuration bean should exist")
            .hasSize(1);
        
        // AND: Configuration should be validated (no exceptions thrown)
        assertThat(properties.getJetstreamUrl()).isNotBlank();
        assertThat(properties.getFeedId()).isNotBlank();
    }
    
    @Test
    @DisplayName("All required framework beans are created")
    void requiredBeansCreated() {
        // GIVEN: Framework auto-configuration active
        
        // WHEN: Querying for framework beans
        
        // THEN: EventSource should be registered
        assertThat(eventSource)
            .as("EventSource bean should be injected")
            .isNotNull();
        
        // THEN: FrameworkProperties should be registered and validated
        assertThat(properties)
            .as("FrameworkProperties bean should be injected")
            .isNotNull();
        
        // THEN: Health indicator should be registered
        assertThat(healthIndicator)
            .as("FrameworkHealthIndicator bean should be injected")
            .isNotNull();
    }
    
    @Test
    @DisplayName("Configuration validation passes for valid config")
    void configurationValidationPasses() {
        // GIVEN: Valid configuration in application-test.yml
        
        // WHEN: Framework validates configuration on startup
        
        // THEN: Jetstream URL should be validated
        assertThat(properties.getJetstreamUrl())
            .startsWith("wss://")
            .contains("test.example.com");
        
        // THEN: Feed ID should be validated
        assertThat(properties.getFeedId())
            .startsWith("at://")
            .contains("app.bsky.feed.generator");
        
        // THEN: Retry config should be within valid ranges
        assertThat(properties.getRetry().getMaxAttempts())
            .isBetween(1, 10);
        
        assertThat(properties.getRetry().getBackoffIntervals())
            .isNotEmpty()
            .hasSize(2);
    }
    
    @Test
    @DisplayName("Framework logs startup information")
    void startupLoggingWorks() {
        // GIVEN: Framework configured and started
        
        // WHEN: Framework banner component is active
        
        // THEN: FrameworkBanner bean should exist
        assertThat(context.containsBean("frameworkBanner"))
            .as("Banner should be registered as a bean")
            .isTrue();
    }
    
    @Test
    @DisplayName("Framework is ready to accept FeedProvider implementations")
    void readyForFeedProviders() {
        // GIVEN: Framework fully initialized
        
        // WHEN: Application is ready
        
        // THEN: Framework should be in operational state
        // (EventSource created, even if not connected yet)
        assertThat(eventSource.getDescription())
            .as("EventSource should have a description")
            .isNotBlank();
        
        // THEN: Health indicator should report status
        assertThat(healthIndicator.health())
            .as("Health indicator should return status map")
            .isNotNull()
            .containsKey("status");
    }
    
    @Test
    @DisplayName("Database schema is created via Flyway migrations")
    void databaseSchemaCreated() {
        // GIVEN: Flyway enabled with V1__initial_schema.sql
        
        // WHEN: Application starts with test database
        
        // THEN: Flyway bean should be present
        assertThat(context.containsBean("flyway"))
            .as("Flyway should be auto-configured")
            .isTrue();
        
        // THEN: EntityManagerFactory should be created (validates schema)
        assertThat(context.containsBean("entityManagerFactory"))
            .as("JPA EntityManagerFactory should exist")
            .isTrue();
    }
}
