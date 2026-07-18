# Research Report: Core Framework Implementation

**Date**: 2026-07-18  
**Status**: Complete  
**Phase**: Phase 0 - Technical Research

## Executive Summary

This research phase resolves technical unknowns identified during planning. All decisions align with the project's constraints: Java 25, Spring Boot 4.1.0, MariaDB persistence, zero additional infrastructure, and "keep operational complexity low" quality goal.

---

## 1. WebSocket Library for Jetstream Connection

### Decision: **Spring WebFlux WebSocket Client**

**Rationale**:
- **Already in dependency tree**: Spring Boot 4.1.0 includes WebFlux WebSocket support
- **Virtual Threads Compatible**: Java 25's virtual threads work seamlessly with reactive WebFlux
- **Native Spring Integration**: Auto-configuration, dependency injection, health indicators built-in
- **Production-Grade**: Used by thousands of Spring Boot applications in production

**Alternatives Considered**:
- **Java-WebSocket (org.java-websocket)**: Simpler API but requires manual lifecycle management, no Spring integration
- **Tyrus (JSR 356)**: Standard but verbose, not optimized for virtual threads
- **OkHttp WebSocket**: Excellent library but adds another HTTP client (Spring already uses RestClient/WebClient)

**Implementation Approach**:
```java
// JetstreamEventSource.java
@Component
public class JetstreamEventSource implements EventSource {
    
    private final WebSocketClient client = new ReactorNettyWebSocketClient();
    private final ObjectMapper objectMapper;
    
    @PostConstruct
    public void connect() {
        var uri = URI.create(properties.getJetstreamUrl());
        
        client.execute(uri, session -> 
            session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(json -> objectMapper.readValue(json, RepositoryEvent.class))
                .doOnNext(eventHandler::handle)
                .doOnError(this::handleError)
                .then()
        ).retry(retrySpec()).subscribe();
    }
    
    private RetryBackoffSpec retrySpec() {
        return Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
            .maxBackoff(Duration.ofSeconds(30));
    }
}
```

**Connection Lifecycle**:
1. Auto-connect on Spring Boot startup (`@PostConstruct`)
2. Exponential backoff retry on connection failures (1s → 30s max)
3. Health indicator: `DOWN` if connection lost for >30s
4. Graceful shutdown: `@PreDestroy` closes WebSocket cleanly

---

## 2. Pagination Cursor Storage Strategy

### Decision: **Database Table with 1-Hour TTL**

**Rationale**:
- **No New Dependencies**: Uses existing MariaDB, no Redis/Caffeine required
- **Restart-Safe**: Cursors survive application restarts (important for rolling deployments)
- **Simple Implementation**: Single table with `expires_at` timestamp, cleanup via scheduled task
- **Multi-Instance Ready**: When scaling horizontally, all instances share cursor state

**Alternatives Considered**:
- **In-Memory Cache (Caffeine)**: Fast but cursors lost on restart. Acceptable for MVP but DB approach is more robust.
- **Redis**: Excellent for distributed deployments but adds operational complexity (violates "keep it simple" goal)
- **Encrypted State in Cursor**: No storage needed, but requires complex encryption/decryption and doesn't prevent replay attacks

**Schema Design**:
```sql
CREATE TABLE pagination_cursors (
    cursor_token    VARCHAR(36)     NOT NULL PRIMARY KEY,  -- UUID
    feed_id         VARCHAR(500)    NOT NULL,
    last_post_id    BIGINT          NOT NULL,              -- PostReference.id for offset
    last_timestamp  DATETIME(3)     NOT NULL,              -- PostReference.timestamp for tie-breaking
    request_limit   INT             NOT NULL,
    created_at      DATETIME(3)     NOT NULL,
    expires_at      DATETIME(3)     NOT NULL,              -- created_at + 1 hour
    
    INDEX idx_expires_at (expires_at)                      -- Cleanup queries
) ENGINE=InnoDB;
```

**Cursor Format**: Opaque UUID v4 token (e.g., `a3f5c8d2-1e4b-4a9c-8d7f-2b3c4d5e6f7a`). Client treats as black box.

**Cleanup Strategy**:
```java
@Scheduled(fixedRate = 300_000) // Every 5 minutes
public void cleanupExpiredCursors() {
    int deleted = cursorRepository.deleteByExpiresAtBefore(Instant.now());
    if (deleted > 0) {
        log.info("Cleaned up {} expired pagination cursors", deleted);
    }
}
```

**Edge Case Handling**:
- **Expired cursor**: Return `400 Bad Request` with ATProto error: `{"error":"InvalidCursor","message":"Cursor has expired"}`
- **Invalid cursor**: Same response (don't leak whether cursor ever existed)
- **Cursor for different feed**: Validate `feed_id` matches request, return error if mismatch

---

## 3. Dead-Letter Queue Implementation

### Decision: **MariaDB Table with Status Lifecycle**

**Rationale**: See detailed research from `dead-letter-queue-impl` agent (completed successfully).

**Key Points**:
- Database table: `dead_letter_events`
- Full event payload stored as JSON for replay capability
- Status lifecycle: `PENDING_REVIEW` → `IN_RETRY` → `RESOLVED`/`DISCARDED`
- Retry history captured: attempts, intervals, timestamps
- Integration: Spring Retry exhaustion → DLQ enqueue

**Monitoring**:
- Micrometer counter: `atproto.dlq.enqueue.total` (alert if >10/hour)
- Prometheus query: `rate(atproto_dlq_enqueue_total[5m]) > 0.1`
- Admin endpoint: `GET /actuator/dlq/pending` (Spring Boot Actuator extension)

**Reprocessing**:
```java
@PostMapping("/admin/dlq/{id}/reprocess")
public ReprocessResult reprocessEvent(@PathVariable long id) {
    return deadLetterQueue.reprocess(id);
}
```

---

## 4. ATProto Jetstream Event Schema

### Decision: **Custom Jackson POJOs with Lenient Parsing**

**Research Findings**:
- **Jetstream Documentation**: https://docs.bsky.app/docs/advanced-guides/jetstream (official Bluesky docs)
- **WebSocket Endpoint**: `wss://jetstream2.us-east.bsky.network/subscribe`
- **Event Types**: `commit`, `identity`, `account` (we only process `commit` for post events)

**Commit Event Schema** (relevant fields):
```json
{
  "did": "did:plc:user123",
  "time_us": 1705334400000000,
  "kind": "commit",
  "commit": {
    "rev": "3jui7kd27a52s",
    "operation": "create",
    "collection": "app.bsky.feed.post",
    "rkey": "3jui7kd27a52s",
    "record": {
      "$type": "app.bsky.feed.post",
      "text": "Hello ATProto!",
      "createdAt": "2024-01-15T12:00:00.000Z"
    },
    "cid": "bafyreihzvp3..."
  }
}
```

**Java POJOs**:
```java
// RepositoryEvent.java
@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryEvent(
    String did,
    @JsonProperty("time_us") long timeUs,
    String kind,
    @JsonProperty("commit") CommitDetails commit
) {
    public Instant timestamp() {
        return Instant.ofEpochMilli(timeUs / 1000);
    }
    
    public String postUri() {
        if (commit == null) return null;
        return String.format("at://%s/%s/%s", 
            did, commit.collection(), commit.rkey());
    }
    
    public boolean isPostCreation() {
        return "commit".equals(kind) 
            && commit != null 
            && "create".equals(commit.operation())
            && "app.bsky.feed.post".equals(commit.collection());
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public record CommitDetails(
    String operation,
    String collection,
    String rkey,
    @JsonProperty("record") JsonNode record  // JsonNode for flexible parsing
) {}
```

**Filtering Strategy**:
```java
// EventProcessor.java
public void process(RepositoryEvent event) {
    if (!event.isPostCreation()) {
        return; // Skip identity/account events, deletes, etc.
    }
    
    // Convert to internal PostReference
    var postRef = PostReference.builder()
        .uri(event.postUri())
        .authorDid(event.did())
        .timestamp(event.timestamp())
        .build();
    
    // Invoke FeedProvider logic
    feedProviders.forEach(provider -> 
        provider.process(postRef, FeedContext.from(event))
    );
}
```

**Error Handling**:
- **Malformed JSON**: Log warning, skip event, increment `atproto.events.malformed.total` counter
- **Missing fields**: `@JsonIgnoreProperties` prevents exceptions, `isPostCreation()` validates required fields
- **Schema evolution**: Forward-compatible via `ignoreUnknown = true`

---

## 5. Spring Boot Auto-Configuration Best Practices

### Decision: **Single Auto-Configuration Class with Conditional Beans**

**Rationale**:
- **Simplicity**: One `@Configuration` class easier to maintain than split modules
- **IDE Support**: `spring-configuration-metadata.json` enables autocomplete in application.yml
- **Zero-Config Defaults**: All beans have `@ConditionalOnMissingBean` for overrides

**Implementation**:
```java
// FrameworkAutoConfiguration.java
@Configuration
@EnableConfigurationProperties(FrameworkProperties.class)
@ConditionalOnProperty(name = "atproto.framework.enabled", matchIfMissing = true)
public class FrameworkAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public EventSource jetstreamEventSource(FrameworkProperties props) {
        return new JetstreamEventSource(props.getJetstreamUrl());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public FeedIndex feedIndex(DataSource dataSource) {
        return new MariaDBFeedIndex(dataSource);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterQueue deadLetterQueue(DeadLetterEventRepository repo) {
        return new MariaDBDeadLetterQueue(repo);
    }
    
    // ... other beans
}
```

**Configuration Properties**:
```java
// FrameworkProperties.java
@ConfigurationProperties(prefix = "atproto.framework")
@Validated
public class FrameworkProperties {
    
    /**
     * Jetstream WebSocket URL.
     */
    @NotBlank
    private String jetstreamUrl = "wss://jetstream2.us-east.bsky.network/subscribe";
    
    /**
     * Feed identifier (e.g., at://did:plc:xyz/app.bsky.feed.generator/my-feed)
     */
    @NotBlank
    private String feedUri;
    
    /**
     * Pagination cursor TTL in seconds (default: 3600 = 1 hour)
     */
    private int cursorTtlSeconds = 3600;
    
    /**
     * Retry configuration
     */
    private RetryConfig retry = new RetryConfig();
    
    // Getters/setters
    
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long initialIntervalMs = 1000;
        private double multiplier = 2.0;
        private long maxIntervalMs = 4000;
        // Getters/setters
    }
}
```

**Developer Experience** (application.yml):
```yaml
atproto:
  framework:
    jetstream-url: wss://jetstream2.us-east.bsky.network/subscribe
    feed-uri: at://did:plc:example123/app.bsky.feed.generator/my-feed
    cursor-ttl-seconds: 3600
    retry:
      max-attempts: 3
      initial-interval-ms: 1000
```

**Spring Factories Registration** (`META-INF/spring.factories`):
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
de.bluewhale.atprotofeed.framework.config.FrameworkAutoConfiguration
```

**Testing Auto-Configuration**:
```java
@Test
void autoConfigurationLoadsWithDefaults() {
    new ApplicationContextRunner()
        .withUserConfiguration(FrameworkAutoConfiguration.class)
        .withPropertyValues("atproto.framework.feed-uri=at://did:plc:test/app.bsky.feed.generator/test")
        .run(context -> {
            assertThat(context).hasSingleBean(EventSource.class);
            assertThat(context).hasSingleBean(FeedIndex.class);
            assertThat(context.getBean(FrameworkProperties.class).getJetstreamUrl())
                .isEqualTo("wss://jetstream2.us-east.bsky.network/subscribe");
        });
}

@Test
void userCanOverrideBeans() {
    new ApplicationContextRunner()
        .withUserConfiguration(
            FrameworkAutoConfiguration.class,
            CustomEventSourceConfig.class
        )
        .run(context -> 
            assertThat(context.getBean(EventSource.class))
                .isInstanceOf(CustomEventSource.class)
        );
}
```

---

## Research Decisions Summary

| Research Area | Decision | Rationale |
|---------------|----------|-----------|
| **WebSocket Library** | Spring WebFlux WebSocket | Already in dependencies, virtual threads compatible, production-grade |
| **Cursor Storage** | MariaDB table with 1h TTL | Zero new dependencies, restart-safe, multi-instance ready |
| **Dead-Letter Queue** | MariaDB table with status lifecycle | Aligns with "keep operational complexity low", full queryability |
| **Event Schema** | Jackson POJOs with lenient parsing | Forward-compatible, ATProto spec-compliant, simple deserialization |
| **Auto-Configuration** | Single @Configuration class | Developer-friendly, zero-config defaults, easy to override |

All decisions are **implementation-ready** and require no further clarification.

---

## Next Steps (Phase 1)

With research complete, Phase 1 (Design) can now proceed to generate:
1. **data-model.md**: Entity schemas (PostReference, CursorEntity, DeadLetterEvent)
2. **contracts/**: Interface definitions (EventSource, FeedProvider, FeedIndex APIs)
3. **quickstart.md**: Sample application walkthrough demonstrating <100 LOC implementation
