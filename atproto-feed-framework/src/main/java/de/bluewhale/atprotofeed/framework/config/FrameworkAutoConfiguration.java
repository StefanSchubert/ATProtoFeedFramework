package de.bluewhale.atprotofeed.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;
import de.bluewhale.atprotofeed.framework.eventsource.JetstreamEventSource;
import de.bluewhale.atprotofeed.framework.eventsource.NoOpEventSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for ATProtoFeedFramework.
 * 
 * Enables the framework when atproto.feed.enabled is true (default).
 * Scans framework packages for components and enables property binding.
 * 
 * <p>Task: T049 - Register JetstreamEventSource as @Bean
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "atproto.feed", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FrameworkProperties.class)
@Import(JacksonConfiguration.class)
@ComponentScan(basePackages = {
    "de.bluewhale.atprotofeed.framework.eventsource",
    "de.bluewhale.atprotofeed.framework.feed",
    "de.bluewhale.atprotofeed.framework.index",
    "de.bluewhale.atprotofeed.framework.api",
    "de.bluewhale.atprotofeed.framework.resilience",
    "de.bluewhale.atprotofeed.framework.health",
    "de.bluewhale.atprotofeed.framework.metrics"
})
@Slf4j
public class FrameworkAutoConfiguration {
    
    private final FrameworkProperties properties;
    
    public FrameworkAutoConfiguration(FrameworkProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void validateConfiguration() {
        log.info("Validating ATProtoFeedFramework configuration...");
        
        // Validation annotations are checked by Spring Boot automatically
        // Additional business logic validation can go here
        
        if (properties.getJetstreamUrl() == null || properties.getJetstreamUrl().isBlank()) {
            throw new IllegalStateException(
                "Framework configuration incomplete: atproto.feed.jetstream-url must be configured"
            );
        }
        
        if (properties.getFeedId() == null || properties.getFeedId().isBlank()) {
            throw new IllegalStateException(
                "Framework configuration incomplete: atproto.feed.feed-id must be configured"
            );
        }
        
        log.info("Framework configuration validated successfully");
    }
    
    /**
     * EventSource bean - uses JetstreamEventSource in production, can be overridden for testing.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "atproto.feed", name = "event-source", havingValue = "jetstream", matchIfMissing = true)
    public EventSource jetstreamEventSource(ObjectMapper objectMapper) {
        log.info("Configuring Jetstream EventSource: {}", properties.getJetstreamUrl());
        return new JetstreamEventSource(properties, objectMapper);
    }
    
    /**
     * NoOp EventSource for testing without real Jetstream connection.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "atproto.feed", name = "event-source", havingValue = "noop")
    public EventSource noOpEventSource() {
        log.warn("Using NoOpEventSource (test mode) - no real events will be received");
        return new NoOpEventSource();
    }
}

