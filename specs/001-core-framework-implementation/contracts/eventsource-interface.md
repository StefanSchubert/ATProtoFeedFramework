# EventSource Interface Contract

**Package**: `de.bluewhale.atprotofeed.framework.eventsource`  
**Type**: Interface (SPI - Service Provider Interface)  
**Implementations**: `JetstreamEventSource`

## Purpose

The `EventSource` interface abstracts ATProto event stream connections. Framework users do not typically implement this interface; it's primarily extended by the framework itself to support multiple event sources (Jetstream, Firehose, test mocks).

---

## Interface Definition

```java
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
```

---

## EventHandler Interface

```java
/**
 * Handler for ATProto repository events.
 * 
 * <p>Implementations process incoming events (filter, index, etc.). The framework
 * provides {@link EventProcessor} as the primary handler implementation.
 */
@FunctionalInterface
public interface EventHandler {
    
    /**
     * Handle an ATProto repository event.
     * 
     * <p>Exceptions thrown by this method trigger the retry strategy:
     * <ol>
     *   <li>1st retry after 1 second
     *   <li>2nd retry after 2 seconds
     *   <li>3rd retry after 4 seconds
     *   <li>Dead-letter queue after 3 failures
     * </ol>
     * 
     * @param event the repository event
     * @throws EventProcessingException if event processing fails (triggers retry)
     */
    void handle(RepositoryEvent event) throws EventProcessingException;
}
```

---

## ConnectionStatus Enum

```java
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
```

---

## Implementation Requirements

### JetstreamEventSource

**Configuration**:
```yaml
atproto:
  framework:
    jetstream-url: wss://jetstream2.us-east.bsky.network/subscribe
```

**Behavior**:
1. **Connection**: Spring WebFlux WebSocket client, virtual threads for scalability
2. **Reconnection**: Exponential backoff (1s → 2s → 4s → 8s → 16s → 30s max)
3. **Deserialization**: Jackson ObjectMapper, lenient parsing (`@JsonIgnoreProperties`)
4. **Filtering**: Only forward `commit` events with `operation=create` and `collection=app.bsky.feed.post`
5. **Health**: `HealthIndicator` reports `DOWN` if disconnected >30s

**Thread Safety**: All methods are thread-safe. Multiple subscribers can register concurrently.

---

## Usage Example

### Framework Setup (Auto-Configuration)

```java
@Configuration
public class FrameworkAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public EventSource eventSource(FrameworkProperties props) {
        return new JetstreamEventSource(props.getJetstreamUrl());
    }
    
    @Bean
    public EventHandler eventProcessor(
        List<FeedProvider> feedProviders,
        FeedIndex feedIndex,
        RetryStrategy retryStrategy
    ) {
        return new EventProcessor(feedProviders, feedIndex, retryStrategy);
    }
    
    @PostConstruct
    public void startEventStream() {
        EventSource source = eventSource(props);
        source.subscribe(eventProcessor(...));
        source.connect();
    }
}
```

### Testing (Custom EventSource for Tests)

```java
public class TestEventSource implements EventSource {
    
    private final List<EventHandler> handlers = new CopyOnWriteArrayList<>();
    
    @Override
    public void connect() {
        // No-op for tests
    }
    
    @Override
    public void subscribe(EventHandler handler) {
        handlers.add(handler);
    }
    
    /**
     * Test helper: Inject event into pipeline.
     */
    public void emitEvent(RepositoryEvent event) {
        handlers.forEach(h -> h.handle(event));
    }
}
```

---

## Error Handling

### Connection Errors

**Scenario**: Jetstream WebSocket closes unexpectedly.

**Behavior**:
1. Log error at WARN level: `"Jetstream connection lost, reconnecting in {delay}s"`
2. Increment Prometheus counter: `atproto_eventsource_reconnect_total`
3. Sleep exponentially (1s → 2s → 4s → ...)
4. Retry connection
5. Update health status to `DOWN` if disconnected >30s

### Deserialization Errors

**Scenario**: Malformed JSON from Jetstream.

**Behavior**:
1. Log warning: `"Failed to deserialize event, skipping: {json}"`
2. Increment counter: `atproto_eventsource_malformed_total`
3. Skip event (do NOT invoke handlers)
4. Continue processing next event

---

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `atproto.eventsource.connect.total` | Counter | Total connection attempts |
| `atproto.eventsource.reconnect.total` | Counter | Reconnection attempts after failures |
| `atproto.eventsource.events.received.total` | Counter | Total events received |
| `atproto.eventsource.events.malformed.total` | Counter | Deserialization failures |
| `atproto.eventsource.connection.duration` | Gauge | Current connection uptime in seconds |

---

## Thread Safety

- **connect()**: Idempotent. Multiple calls have no effect if already connected.
- **subscribe()**: Thread-safe. Handlers can be registered concurrently.
- **disconnect()**: Thread-safe. Can be called from shutdown hooks.
- **Event Delivery**: Events delivered sequentially to each handler (no parallel dispatch per event).

---

## Testing Guidelines

### Unit Test (Mocking)

```java
@Test
void eventSourceDeliversEventsToHandlers() {
    var source = new TestEventSource();
    var handler = mock(EventHandler.class);
    
    source.subscribe(handler);
    source.emitEvent(createTestEvent());
    
    verify(handler).handle(any(RepositoryEvent.class));
}
```

### Integration Test (Testcontainers)

```java
@SpringBootTest
@Testcontainers
class JetstreamIntegrationTest {
    
    @Container
    static GenericContainer<?> jetstream = new GenericContainer<>("bluesky/jetstream:latest")
        .withExposedPorts(6008);
    
    @Test
    void connectsToRealJetstream() {
        String url = "ws://" + jetstream.getHost() + ":" + jetstream.getMappedPort(6008);
        var source = new JetstreamEventSource(url);
        
        source.connect();
        await().atMost(10, SECONDS).until(() -> source.getStatus() == CONNECTED);
    }
}
```

---

## Related Contracts

- [FeedProvider Interface](./feedprovider-interface.md) - Consumes events via EventProcessor
- [EventProcessor](./eventprocessor-contract.md) - Primary EventHandler implementation
