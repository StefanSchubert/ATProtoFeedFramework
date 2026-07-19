package de.bluewhale.atprotofeed.framework.eventsource;

/**
 * Connection status for event sources.
 */
public enum ConnectionStatus {
    /**
     * WebSocket connected and receiving events.
     */
    CONNECTED,
    
    /**
     * Connection closed (initial state or after disconnect()).
     */
    DISCONNECTED,
    
    /**
     * Attempting to establish connection (initial connect or reconnect).
     */
    CONNECTING,
    
    /**
     * Connection failed and retry backoff in progress.
     */
    ERROR
}
