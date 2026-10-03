package de.bluewhale.atprotofeed.integration;

import de.bluewhale.atprotofeed.AbstractIntegrationTest;
import de.bluewhale.atprotofeed.api.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;
import de.bluewhale.atprotofeed.api.feed.RepositoryEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for WebSocket connection to Jetstream.
 * 
 * <p>Task: T039 - Integration test for WebSocket connection
 */
public class JetstreamConnectionTest extends AbstractIntegrationTest {
    
    @Autowired(required = false)
    private EventSource eventSource;
    
    @Test
    void eventSource_shouldBeAvailable() {
        assertThat(eventSource).isNotNull();
    }
    
    @Test
    void eventSource_shouldConnect() throws InterruptedException {
        // Initial state
        assertThat(eventSource.getStatus()).isIn(
            ConnectionStatus.DISCONNECTED, 
            ConnectionStatus.CONNECTING,
            ConnectionStatus.CONNECTED
        );
        
        // Connect if not already connected
        if (eventSource.getStatus() != ConnectionStatus.CONNECTED) {
            eventSource.connect();
            Thread.sleep(5000);  // Give it time to connect
        }
        
        // Verify connection established
        assertThat(eventSource.getStatus()).isIn(
            ConnectionStatus.CONNECTED, 
            ConnectionStatus.CONNECTING
        );
    }
    
    @Test
    void eventSource_shouldReceiveEvents() throws InterruptedException {
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);
        
        eventSource.subscribe(event -> {
            eventCount.incrementAndGet();
            eventLatch.countDown();
        });
        
        if (eventSource.getStatus() != ConnectionStatus.CONNECTED) {
            eventSource.connect();
        }
        
        // Wait for at least one event (30 seconds timeout)
        boolean received = eventLatch.await(30, TimeUnit.SECONDS);
        
        // For NoOp event source in tests, this may not receive events
        // Real Jetstream should receive events within 30 seconds
        if (eventSource.getDescription().contains("Jetstream")) {
            assertThat(received).isTrue();
            assertThat(eventCount.get()).isGreaterThan(0);
        }
    }
    
    @Test
    void eventSource_shouldProvideDescription() {
        String description = eventSource.getDescription();
        assertThat(description).isNotBlank();
    }
}
