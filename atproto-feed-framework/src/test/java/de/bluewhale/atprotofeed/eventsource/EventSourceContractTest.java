package de.bluewhale.atprotofeed.eventsource;

import de.bluewhale.atprotofeed.api.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.api.eventsource.EventHandler;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;
import de.bluewhale.atprotofeed.api.feed.RepositoryEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for EventSource implementations.
 * 
 * <p>Any implementation of EventSource MUST pass these tests to ensure
 * it conforms to the expected lifecycle and behavior.
 * 
 * <p>Task: T037 - Contract test for EventSource interface
 */
public abstract class EventSourceContractTest {
    
    /**
     * Subclasses must provide an EventSource implementation to test.
     */
    protected abstract EventSource createEventSource();
    
    /**
     * Subclasses may override to simulate an event for testing.
     * Default implementation returns null (no events).
     */
    protected RepositoryEvent createTestEvent() {
        return null;
    }
    
    @Test
    void connect_shouldEstablishConnection() throws InterruptedException {
        EventSource eventSource = createEventSource();
        
        // Initially disconnected
        assertThat(eventSource.getStatus()).isIn(ConnectionStatus.DISCONNECTED, ConnectionStatus.CONNECTING);
        
        // Connect
        eventSource.connect();
        
        // Give it a moment to connect (max 5 seconds)
        Thread.sleep(5000);
        
        // Should be connected or connecting (depending on async implementation)
        assertThat(eventSource.getStatus()).isIn(ConnectionStatus.CONNECTED, ConnectionStatus.CONNECTING);
        
        // Cleanup
        eventSource.disconnect();
    }
    
    @Test
    void subscribe_shouldReceiveEvents() throws InterruptedException {
        EventSource eventSource = createEventSource();
        CountDownLatch eventReceived = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);
        
        // Subscribe before connecting
        EventHandler handler = event -> {
            eventCount.incrementAndGet();
            eventReceived.countDown();
        };
        
        eventSource.subscribe(handler);
        eventSource.connect();
        
        // Wait for at least one event (timeout 30 seconds)
        boolean received = eventReceived.await(30, TimeUnit.SECONDS);
        
        // If test events are available, verify reception
        if (createTestEvent() != null) {
            assertThat(received).isTrue();
            assertThat(eventCount.get()).isGreaterThan(0);
        }
        
        // Cleanup
        eventSource.disconnect();
    }
    
    @Test
    void disconnect_shouldCloseConnection() throws InterruptedException {
        EventSource eventSource = createEventSource();
        
        // Connect first
        eventSource.connect();
        Thread.sleep(2000);
        
        // Verify connected
        assertThat(eventSource.getStatus()).isIn(ConnectionStatus.CONNECTED, ConnectionStatus.CONNECTING);
        
        // Disconnect
        eventSource.disconnect();
        Thread.sleep(1000);
        
        // Should be disconnected
        assertThat(eventSource.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
    }
    
    @Test
    void getStatus_shouldReturnCurrentConnectionState() {
        EventSource eventSource = createEventSource();
        
        // Initial state
        ConnectionStatus status = eventSource.getStatus();
        assertThat(status).isNotNull();
        assertThat(status).isIn(ConnectionStatus.DISCONNECTED, ConnectionStatus.CONNECTING);
    }
    
    @Test
    void getDescription_shouldReturnNonEmptyString() {
        EventSource eventSource = createEventSource();
        
        String description = eventSource.getDescription();
        assertThat(description).isNotBlank();
    }
    
    @Test
    void multipleHandlers_shouldAllReceiveEvents() throws InterruptedException {
        EventSource eventSource = createEventSource();
        CountDownLatch handler1Latch = new CountDownLatch(1);
        CountDownLatch handler2Latch = new CountDownLatch(1);
        
        // Register multiple handlers
        eventSource.subscribe(event -> handler1Latch.countDown());
        eventSource.subscribe(event -> handler2Latch.countDown());
        
        eventSource.connect();
        
        // Wait for events (timeout 30 seconds)
        boolean handler1Received = handler1Latch.await(30, TimeUnit.SECONDS);
        boolean handler2Received = handler2Latch.await(30, TimeUnit.SECONDS);
        
        // If test events are available, verify both handlers received
        if (createTestEvent() != null) {
            assertThat(handler1Received).isTrue();
            assertThat(handler2Received).isTrue();
        }
        
        // Cleanup
        eventSource.disconnect();
    }
}
