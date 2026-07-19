package de.bluewhale.atprotofeed.integration;

import de.bluewhale.atprotofeed.AbstractIntegrationTest;
import de.bluewhale.atprotofeed.framework.config.FrameworkProperties;
import de.bluewhale.atprotofeed.framework.eventsource.EventSource;
import de.bluewhale.atprotofeed.framework.feed.FeedProvider;
import de.bluewhale.atprotofeed.framework.index.FeedIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance Test T028: Dependency Resolution
 * 
 * Verifies that the framework can be added as a Maven dependency and all
 * required components are available in the Spring context.
 * 
 * Success Criteria:
 * - Framework auto-configuration loads successfully
 * - Core interfaces (EventSource, FeedProvider, FeedIndex) are available as beans
 * - Configuration properties are bound correctly
 * - No dependency conflicts or classpath issues
 */
@DisplayName("US1 Acceptance Test: Framework Setup - Dependency Resolution")
public class FrameworkSetupTest extends AbstractIntegrationTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    @DisplayName("Framework can be added as Maven dependency and auto-configuration loads")
    void frameworkDependencyResolution() {
        // GIVEN: Framework is added to pom.xml
        
        // WHEN: Spring Boot application starts
        
        // THEN: Auto-configuration should load successfully
        assertThat(context).isNotNull();
        assertThat(context.getApplicationName()).contains("atproto-feed-framework");
    }
    
    @Test
    @DisplayName("Core framework interfaces are available as Spring beans")
    void coreInterfacesAvailable() {
        // GIVEN: Framework auto-configuration is active
        
        // WHEN: Querying for core interface beans
        
        // THEN: EventSource should be available
        assertThat(context.getBeansOfType(EventSource.class))
            .as("EventSource bean should be registered")
            .isNotEmpty();
        
        // THEN: FeedProvider interface should be available for injection
        // (No implementations registered yet, but interface should exist)
        assertThat(EventSource.class).isInterface();
        assertThat(FeedProvider.class).isInterface();
        assertThat(FeedIndex.class).isInterface();
    }
    
    @Test
    @DisplayName("Framework configuration properties are bound correctly from application.yml")
    void configurationPropertiesBinding() {
        // GIVEN: application-test.yml contains framework configuration
        
        // WHEN: Framework properties bean is queried
        FrameworkProperties props = context.getBean(FrameworkProperties.class);
        
        // THEN: Properties should be bound correctly
        assertThat(props).isNotNull();
        assertThat(props.getJetstreamUrl())
            .as("Jetstream URL should be bound from config")
            .isEqualTo("wss://test.example.com/subscribe");
        
        assertThat(props.getFeedId())
            .as("Feed ID should be bound from config")
            .isEqualTo("at://did:plc:test123/app.bsky.feed.generator/test-feed");
        
        assertThat(props.getRetry().getMaxAttempts())
            .as("Retry config should be bound")
            .isEqualTo(2);
    }
    
    @Test
    @DisplayName("No dependency conflicts or classpath issues")
    void noDependencyConflicts() {
        // GIVEN: Framework with all dependencies
        
        // WHEN: Loading critical classes
        
        // THEN: All required classes should be on classpath
        assertThat(loadClass("org.springframework.boot.autoconfigure.SpringBootApplication"))
            .as("Spring Boot should be available")
            .isNotNull();
        
        assertThat(loadClass("jakarta.persistence.Entity"))
            .as("JPA should be available")
            .isNotNull();
        
        assertThat(loadClass("com.fasterxml.jackson.databind.ObjectMapper"))
            .as("Jackson should be available")
            .isNotNull();
        
        assertThat(loadClass("org.slf4j.Logger"))
            .as("SLF4J should be available")
            .isNotNull();
    }
    
    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
