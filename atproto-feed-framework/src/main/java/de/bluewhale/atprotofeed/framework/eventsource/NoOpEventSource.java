package de.bluewhale.atprotofeed.framework.eventsource;

import de.bluewhale.atprotofeed.api.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.api.eventsource.EventHandler;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * No-op event source for testing without real Jetstream connection.
 * 
 * <p>Task: T050 - NoOp event source stub implementation
 */
@Slf4j
public class NoOpEventSource implements EventSource {
    
    private final List<EventHandler> handlers = new ArrayList<>();
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    
    @Override
    public void connect() {
        log.info("NoOpEventSource: connect() called (test mode)");
        status = ConnectionStatus.CONNECTED;
    }
    
    @Override
    public void subscribe(EventHandler handler) {
        handlers.add(handler);
        log.debug("NoOpEventSource: registered handler (total: {})", handlers.size());
    }
    
    @Override
    public void disconnect() {
        log.info("NoOpEventSource: disconnect() called");
        status = ConnectionStatus.DISCONNECTED;
    }
    
    @Override
    public ConnectionStatus getStatus() {
        return status;
    }
    
    @Override
    public String getDescription() {
        return "NoOpEventSource (Test Mode)";
    }
}
