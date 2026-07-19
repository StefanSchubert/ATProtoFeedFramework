package de.bluewhale.atprotofeed.framework.config;

import de.bluewhale.atprotofeed.framework.eventsource.EventSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for ATProtoFeedFramework.
 * 
 * Enables the framework when atproto.feed.enabled is true (default).
 * Scans framework packages for components and enables property binding.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "atproto.feed", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FrameworkProperties.class)
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
     * Placeholder bean for EventSource until JetstreamEventSource is implemented.
     * This will be replaced by the actual implementation in Phase 4.
     */
    @Bean
    @ConditionalOnMissingBean
    public EventSource eventSource() {
        return new NoOpEventSource(properties.getJetstreamUrl());
    }
}

