package de.bluewhale.atprotofeed.integration;

import de.bluewhale.atprotofeed.AbstractIntegrationTest;
import de.bluewhale.atprotofeed.api.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for reconnection logic.
 * 
 * <p>Task: T040 - Integration test for reconnection logic
 */
@TestPropertySource(properties = {
    "atproto.feed.reconnect.initial-delay-seconds=1",
    "atproto.feed.reconnect.max-delay-seconds=5"
})
public class ReconnectionTest extends AbstractIntegrationTest {
    
    @Autowired(required = false)
    private EventSource eventSource;
    
    @Test
    void eventSource_shouldReconnectAfterDisconnect() throws InterruptedException {
        // Connect
        eventSource.connect();
        Thread.sleep(3000);
        
        // Verify connected
        assertThat(eventSource.getStatus()).isIn(
            ConnectionStatus.CONNECTED, 
            ConnectionStatus.CONNECTING
        );
        
        // Disconnect
        eventSource.disconnect();
        Thread.sleep(1000);
        
        // Verify disconnected
        assertThat(eventSource.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
        
        // Reconnect
        eventSource.connect();
        Thread.sleep(3000);
        
        // Verify reconnected
        assertThat(eventSource.getStatus()).isIn(
            ConnectionStatus.CONNECTED, 
            ConnectionStatus.CONNECTING
        );
    }
    
    @Test
    void eventSource_shouldHandleMultipleConnectCalls() throws InterruptedException {
        // Multiple connect calls should be idempotent
        eventSource.connect();
        Thread.sleep(1000);
        
        eventSource.connect();  // Second call
        Thread.sleep(1000);
        
        eventSource.connect();  // Third call
        Thread.sleep(1000);
        
        // Should still be in valid state
        assertThat(eventSource.getStatus()).isIn(
            ConnectionStatus.CONNECTED, 
            ConnectionStatus.CONNECTING,
            ConnectionStatus.ERROR
        );
    }
}
