package de.bluewhale.atprotofeed.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Displays framework startup banner and version information.
 */
@Component
@Slf4j
public class FrameworkBanner {
    
    private static final String VERSION = "0.1-SNAPSHOT";
    private static final String BANNER = """
        
        ╔═══════════════════════════════════════════════════════════════╗
        ║                                                               ║
        ║              ATProto Feed Framework                           ║
        ║              Version: %-40s║
        ║                                                               ║
        ║              Build feed logic, not feed infrastructure.       ║
        ║                                                               ║
        ╚═══════════════════════════════════════════════════════════════╝
        """;
    
    private final FrameworkProperties properties;
    private final Environment environment;
    
    public FrameworkBanner(FrameworkProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(String.format(BANNER, VERSION));
        log.info("Framework Configuration:");
        log.info("  Feed ID: {}", properties.getFeedId());
        log.info("  Jetstream URL: {}", properties.getJetstreamUrl());
        log.info("  Retry Strategy: {} attempts with backoff {}", 
            properties.getRetry().getMaxAttempts(),
            properties.getRetry().getBackoffIntervals());
        log.info("  Cursor TTL: {} hour(s)", properties.getCursor().getTtlHours());
        
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            log.info("  Active Profiles: {}", String.join(", ", activeProfiles));
        }
        
        log.info("Framework started successfully");
    }
}
