package de.bluewhale.atprotofeed.framework.config;

import de.bluewhale.atprotofeed.api.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.api.eventsource.EventHandler;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;

/**
 * No-op EventSource implementation for testing framework setup.
 * 
 * This is a temporary implementation used until JetstreamEventSource is ready.
 * It always reports DISCONNECTED status for health checks.
 */
class NoOpEventSource implements EventSource {
    
    private final String url;
    
    NoOpEventSource(String url) {
        this.url = url;
    }
    
    @Override
    public void connect() {
        // No-op: Will be implemented in Phase 4
    }
    
    @Override
    public void subscribe(EventHandler handler) {
        // No-op: Will be implemented in Phase 4
    }
    
    @Override
    public void disconnect() {
        // No-op: Will be implemented in Phase 4
    }
    
    @Override
    public ConnectionStatus getStatus() {
        return ConnectionStatus.DISCONNECTED;
    }
    
    @Override
    public String getDescription() {
        return "NoOpEventSource (placeholder): " + url;
    }
}
