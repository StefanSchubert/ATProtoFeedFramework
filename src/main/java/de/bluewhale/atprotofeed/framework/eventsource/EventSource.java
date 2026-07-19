package de.bluewhale.atprotofeed.framework.eventsource;

/**
 * Abstraction for ATProto event streams.
 * 
 * <p>Implementations connect to an ATProto event source (e.g., Jetstream WebSocket),
 * receive repository events, deserialize them, and forward to registered handlers.
 * 
 * <p>Lifecycle:
 * <ul>
 *   <li>{@link #connect()} - Establish connection (called on Spring Boot startup)
 *   <li>{@link #subscribe(EventHandler)} - Register event handler
 *   <li>{@link #disconnect()} - Close connection gracefully (called on shutdown)
 * </ul>
 * 
 * <p>Connection failures trigger automatic reconnection with exponential backoff.
 * 
 * @see JetstreamEventSource
 */
public interface EventSource {
    
    /**
     * Establish connection to the event source.
     * 
     * <p>This method is non-blocking and returns immediately. Connection lifecycle
     * (retries, errors) are managed internally.
     * 
     * <p>Implementations MUST:
     * <ul>
     *   <li>Retry connection failures with exponential backoff (1s → 30s)
     *   <li>Update health status (UP when connected, DOWN when connection lost >30s)
     *   <li>Log connection events at INFO level
     * </ul>
     * 
     * @throws EventSourceException if initial connection setup fails
     */
    void connect();
    
    /**
     * Register an event handler to receive ATProto events.
     * 
     * <p>Multiple handlers can be registered; events are delivered to all handlers
     * in registration order.
     * 
     * <p>Handler errors MUST NOT stop event processing. Failed events trigger the
     * retry strategy (3 attempts) then move to dead-letter queue.
     * 
     * @param handler the event handler
     */
    void subscribe(EventHandler handler);
    
    /**
     * Close the connection gracefully.
     * 
     * <p>Called automatically during Spring Boot shutdown. Implementations should:
     * <ul>
     *   <li>Close WebSocket/HTTP connections
     *   <li>Flush any buffered events
     *   <li>Update health status to DOWN
     * </ul>
     * 
     * <p>Must complete within 30 seconds (Spring shutdown timeout).
     */
    void disconnect();
    
    /**
     * Get the current connection status.
     * 
     * @return connection status (CONNECTED, DISCONNECTED, CONNECTING, ERROR)
     */
    ConnectionStatus getStatus();
    
    /**
     * Get a human-readable description of this event source.
     * 
     * @return source description (e.g., "Jetstream: wss://jetstream2.us-east.bsky.network")
     */
    String getDescription();
}
