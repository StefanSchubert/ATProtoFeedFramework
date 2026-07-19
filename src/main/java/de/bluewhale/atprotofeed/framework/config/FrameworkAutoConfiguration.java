package de.bluewhale.atprotofeed.framework.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
public class FrameworkAutoConfiguration {
    // Auto-configuration is declarative through annotations
    // Beans will be created via component scanning
}
