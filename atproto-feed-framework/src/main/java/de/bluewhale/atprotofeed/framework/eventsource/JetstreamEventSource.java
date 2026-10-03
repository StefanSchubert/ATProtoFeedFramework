package de.bluewhale.atprotofeed.framework.eventsource;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.atprotofeed.api.eventsource.ConnectionStatus;
import de.bluewhale.atprotofeed.api.eventsource.EventHandler;
import de.bluewhale.atprotofeed.api.eventsource.EventSource;
import de.bluewhale.atprotofeed.api.exception.EventSourceException;
import de.bluewhale.atprotofeed.api.feed.RepositoryEvent;
import de.bluewhale.atprotofeed.framework.config.FrameworkProperties;
import de.bluewhale.atprotofeed.framework.resilience.ExponentialBackoff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Jetstream WebSocket event source implementation.
 * 
 * <p>Tasks: T044-T047 - Jetstream implementation with reconnection, graceful shutdown, and logging
 */
@Slf4j
public class JetstreamEventSource implements EventSource {
    
    private final FrameworkProperties properties;
    private final ObjectMapper objectMapper;
    private final WebSocketClient webSocketClient;
    private final List<EventHandler> handlers;
    private final AtomicReference<ConnectionStatus> status;
    private final ExponentialBackoff backoff;
    
    public JetstreamEventSource(FrameworkProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webSocketClient = new ReactorNettyWebSocketClient();
        this.handlers = new ArrayList<>();
        this.status = new AtomicReference<>(ConnectionStatus.DISCONNECTED);
        this.backoff = ExponentialBackoff.ofSeconds(
            properties.getReconnect().getInitialDelaySeconds(),
            properties.getReconnect().getMaxDelaySeconds()
        );
    }
    
    @PostConstruct
    @Override
    public void connect() {
        if (status.get() == ConnectionStatus.CONNECTED || status.get() == ConnectionStatus.CONNECTING) {
            log.debug("Already connected or connecting, skipping duplicate connect()");
            return;
        }
        
        status.set(ConnectionStatus.CONNECTING);
        log.info("Connecting to Jetstream: {}", properties.getJetstreamUrl());
        
        URI uri = URI.create(properties.getJetstreamUrl());
        
        webSocketClient.execute(uri, session -> {
            log.info("Jetstream WebSocket connected successfully");
            status.set(ConnectionStatus.CONNECTED);
            
            return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(this::processMessage)
                .doOnError(error -> {
                    log.error("Error receiving message from Jetstream", error);
                    status.set(ConnectionStatus.ERROR);
                })
                .then();
        })
        .retryWhen(Retry.backoff(
            properties.getReconnect().getMaxAttempts(),
            Duration.ofSeconds(properties.getReconnect().getInitialDelaySeconds())
        ).maxBackoff(Duration.ofSeconds(properties.getReconnect().getMaxDelaySeconds()))
        .doBeforeRetry(signal -> {
            log.warn("Jetstream connection failed (attempt {}), retrying...", 
                signal.totalRetries() + 1);
            status.set(ConnectionStatus.CONNECTING);
        }))
        .doOnError(error -> {
            log.error("Jetstream connection failed permanently", error);
            status.set(ConnectionStatus.ERROR);
        })
        .subscribe();
    }
    
    private void processMessage(String message) {
        try {
            RepositoryEvent event = objectMapper.readValue(message, RepositoryEvent.class);
            
            // Deliver to all handlers
            for (EventHandler handler : handlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    log.error("Handler failed to process event {}", event.eventId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to deserialize message: {}", message, e);
        }
    }
    
    @Override
    public void subscribe(EventHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        handlers.add(handler);
        log.debug("Registered event handler (total: {})", handlers.size());
    }
    
    @PreDestroy
    @Override
    public void disconnect() {
        log.info("Disconnecting from Jetstream");
        status.set(ConnectionStatus.DISCONNECTED);
        // WebSocket client will be closed by reactor shutdown
    }
    
    @Override
    public ConnectionStatus getStatus() {
        return status.get();
    }
    
    @Override
    public String getDescription() {
        return String.format("Jetstream: %s", properties.getJetstreamUrl());
    }
}
