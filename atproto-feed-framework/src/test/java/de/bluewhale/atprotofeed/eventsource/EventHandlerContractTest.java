package de.bluewhale.atprotofeed.eventsource;

import de.bluewhale.atprotofeed.api.eventsource.EventHandler;
import de.bluewhale.atprotofeed.api.exception.EventProcessingException;
import de.bluewhale.atprotofeed.api.feed.RepositoryEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for EventHandler implementations.
 * 
 * <p>Task: T038 - Contract test for EventHandler interface
 */
public abstract class EventHandlerContractTest {
    
    /**
     * Subclasses must provide an EventHandler implementation to test.
     */
    protected abstract EventHandler createEventHandler();
    
    /**
     * Subclasses must provide a test event.
     */
    protected abstract RepositoryEvent createTestEvent();
    
    @Test
    void handle_shouldProcessEvent() throws EventProcessingException {
        EventHandler handler = createEventHandler();
        RepositoryEvent event = createTestEvent();
        
        // Should not throw exception for valid event
        handler.handle(event);
    }
    
    @Test
    void handle_shouldBeInvokedForEachEvent() throws EventProcessingException {
        AtomicBoolean invoked = new AtomicBoolean(false);
        
        EventHandler handler = event -> {
            invoked.set(true);
        };
        
        RepositoryEvent event = createTestEvent();
        handler.handle(event);
        
        assertThat(invoked.get()).isTrue();
    }
    
    @Test
    void handle_shouldReceiveCorrectEvent() throws EventProcessingException {
        AtomicReference<RepositoryEvent> receivedEvent = new AtomicReference<>();
        
        EventHandler handler = event -> {
            receivedEvent.set(event);
        };
        
        RepositoryEvent testEvent = createTestEvent();
        handler.handle(testEvent);
        
        assertThat(receivedEvent.get()).isNotNull();
        assertThat(receivedEvent.get()).isEqualTo(testEvent);
    }
    
    @Test
    void handle_shouldThrowEventProcessingExceptionOnFailure() {
        EventHandler failingHandler = event -> {
            throw new EventProcessingException("Intentional failure for testing");
        };
        
        RepositoryEvent event = createTestEvent();
        
        assertThatThrownBy(() -> failingHandler.handle(event))
            .isInstanceOf(EventProcessingException.class)
            .hasMessageContaining("Intentional failure");
    }
    
    @Test
    void functionalInterface_shouldAllowLambdaExpression() throws EventProcessingException {
        // Verify @FunctionalInterface works
        EventHandler lambdaHandler = event -> {
            // No-op for test
        };
        
        lambdaHandler.handle(createTestEvent());
    }
}
