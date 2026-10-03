package de.bluewhale.atprotofeed.framework.metrics;

import de.bluewhale.atprotofeed.api.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Metrics for EventSource connection status.
 * 
 * <p>Task: T051 - Connection status metrics for Micrometer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionStatusMetrics {
    
    private final MeterRegistry meterRegistry;
    private final EventSource eventSource;
    
    @PostConstruct
    public void registerMetrics() {
        // Connection status gauge (1=CONNECTED, 0=DISCONNECTED, -1=ERROR)
        Gauge.builder("atproto.eventsource.connection.status", eventSource, this::getStatusValue)
            .description("EventSource connection status (1=CONNECTED, 0=DISCONNECTED, -1=ERROR)")
            .register(meterRegistry);
        
        log.debug("Registered connection status metrics");
    }
    
    private double getStatusValue(EventSource source) {
        ConnectionStatus status = source.getStatus();
        return switch (status) {
            case CONNECTED -> 1.0;
            case DISCONNECTED, CONNECTING -> 0.0;
            case ERROR -> -1.0;
        };
    }
}
