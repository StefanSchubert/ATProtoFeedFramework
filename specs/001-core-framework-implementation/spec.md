# Feature Specification: Core Framework Implementation

**Feature Branch**: `001-core-framework-implementation`

**Created**: 2026-07-18

**Status**: Draft

**Input**: User description: "Implement the core ATProtoFeedFramework components to enable developers to build custom ATProto feed applications without implementing protocol infrastructure themselves."

## Clarifications

### Session 2026-07-18

- Q: **Feed Post Ranking Strategy**: Wie sollen Posts innerhalb eines Feeds sortiert werden? → A: Reverse chronological (newest first)
- Q: **Event Processing Retry Strategy**: Was soll bei transienten Fehlern während der Event-Verarbeitung passieren? → A: Exponential backoff with 3 retries (1s, 2s, 4s), then dead-letter queue for failed events
- Q: **Pagination Cursor Lifetime**: Wie lange sollen Paginierungs-Cursor gültig bleiben? → A: 1 hour (short-lived, frequent refreshes)
- Q: **Default Log Level**: Welches Log-Level soll die Framework-Standardkonfiguration verwenden? → A: INFO (production standard)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Framework Setup and Configuration (Priority: P1)

As a Java developer, I want to add the ATProtoFeedFramework dependency to my project and configure it to connect to an ATProto event source, so that I can start building my custom feed without dealing with protocol details.

**Why this priority**: This is the foundational capability - without being able to set up and configure the framework, no other functionality is possible. This represents the absolute minimum viable product.

**Independent Test**: Can be fully tested by adding the framework dependency to a new Maven project, providing minimal configuration (event source URL, feed identifier), and verifying the application starts successfully without errors. Delivers value by proving the framework can be integrated and initialized.

**Acceptance Scenarios**:

1. **Given** a new Java Maven project, **When** I add the ATProtoFeedFramework dependency to pom.xml, **Then** the dependency resolves successfully and the project compiles
2. **Given** the framework is added to my project, **When** I provide minimal configuration (event source connection details, feed ID), **Then** the application starts successfully and logs indicate successful initialization
3. **Given** the application is running, **When** I check the application health endpoint, **Then** the framework reports healthy status

---

### User Story 2 - Connect to ATProto Event Stream (Priority: P1)

As a feed developer, I want the framework to automatically connect to an ATProto event source (Jetstream) and receive repository events, so that I can process posts without implementing WebSocket connection management.

**Why this priority**: Event ingestion is the second critical piece - without receiving events, the framework cannot populate any feeds. This completes the input side of the framework.

**Independent Test**: Can be tested by configuring the framework to connect to a Jetstream instance, starting the application, and verifying that the framework logs show successful connection and event reception. Delivers value by proving the framework can consume the ATProto event stream.

**Acceptance Scenarios**:

1. **Given** I have configured a Jetstream URL, **When** the application starts, **Then** the framework establishes a WebSocket connection to Jetstream
2. **Given** the connection is established, **When** new posts are created in the ATProto network, **Then** the framework receives corresponding repository events
3. **Given** the connection is active, **When** a network interruption occurs, **Then** the framework automatically reconnects with exponential backoff
4. **Given** events are being received, **When** I stop the application, **Then** the connection is closed gracefully

---

### User Story 3 - Implement Custom Feed Logic (Priority: P1)

As a feed developer, I want to implement a simple FeedProvider interface that defines which posts belong in my feed, so that I can focus on my selection rules without worrying about indexing or API implementation.

**Why this priority**: This is where the developer's custom logic lives - the core value proposition of the framework. Without this, developers cannot create their unique feeds.

**Independent Test**: Can be tested by implementing a simple FeedProvider (e.g., "accept all posts from user X"), registering it with the framework, and verifying that matching posts are selected. Delivers value by proving developers can implement custom feed logic.

**Acceptance Scenarios**:

1. **Given** I implement a FeedProvider with a simple selection rule, **When** I register it with the framework, **Then** the framework accepts and initializes my provider
2. **Given** my FeedProvider is registered, **When** relevant events arrive, **Then** my selection logic is invoked for each event
3. **Given** my selection logic returns true for a post, **When** the framework processes the post, **Then** the post reference is indexed for later retrieval
4. **Given** my FeedProvider throws an exception, **When** processing an event, **Then** the framework logs the error and continues processing other events

---

### User Story 4 - Query Feed via ATProto API (Priority: P2)

As a Bluesky user, I want to query the custom feed through the standard ATProto Feed Generator API, so that I can see the posts selected by the feed logic in my Bluesky client.

**Why this priority**: This completes the output side - feeds must be queryable to be useful. However, it depends on having posts already indexed (P1 stories), so it's naturally P2.

**Independent Test**: Can be tested by populating the feed index with test data, making an HTTP GET request to the feed endpoint with proper ATProto parameters, and verifying the response contains the expected post references in correct format. Delivers value by proving the feed is consumable by ATProto clients.

**Acceptance Scenarios**:

1. **Given** my feed has indexed posts, **When** I make a GET request to `/xrpc/app.bsky.feed.getFeedSkeleton` with my feed URI, **Then** I receive a valid ATProto feed response
2. **Given** the feed contains 100 posts, **When** I request the feed with limit=50, **Then** I receive the first 50 posts and a cursor for pagination
3. **Given** I have a pagination cursor, **When** I request the feed with that cursor, **Then** I receive the next set of posts
4. **Given** the feed is empty, **When** I query the feed, **Then** I receive an empty feed response without errors

---

### User Story 5 - Persist Feed Index (Priority: P2)

As an operator, I want the framework to persist the feed index to a database, so that my feed survives application restarts and does not need to reprocess all historical events.

**Why this priority**: Persistence is critical for production deployments but not required for initial development/testing. Developers can experiment with in-memory indexing before adding persistence.

**Independent Test**: Can be tested by configuring database connection details, running the application to index posts, restarting the application, and verifying previously indexed posts are still available. Delivers value by proving the framework supports stateful production deployments.

**Acceptance Scenarios**:

1. **Given** I configure database connection details (MariaDB), **When** the application starts, **Then** the framework initializes the database schema
2. **Given** the application is running, **When** posts are indexed, **Then** post references are persisted to the database
3. **Given** posts are persisted, **When** I restart the application, **Then** previously indexed posts are immediately available in feed queries
4. **Given** the database is unavailable, **When** the application starts, **Then** the framework logs a clear error message and fails to start

---

### User Story 6 - Monitor Framework Health (Priority: P3)

As an operator, I want the framework to expose health checks and metrics, so that I can monitor the application status and troubleshoot issues in production.

**Why this priority**: Monitoring is important for production but not required for initial development. Developers can build and test feeds before implementing comprehensive monitoring.

**Independent Test**: Can be tested by starting the application, making requests to health/readiness endpoints, and verifying Prometheus metrics are exposed. Delivers value by proving the framework supports production monitoring.

**Acceptance Scenarios**:

1. **Given** the application is running, **When** I query the `/actuator/health` endpoint, **Then** I receive the overall health status
2. **Given** the event source is connected, **When** I query readiness probe, **Then** the application reports ready
3. **Given** the application is processing events, **When** I query the `/actuator/prometheus` endpoint, **Then** I see metrics for events received, posts indexed, and API requests served
4. **Given** the event source connection fails, **When** I query health endpoint, **Then** the status shows degraded with details about the failed component

---

### Edge Cases

- What happens when the event source sends malformed events? (Framework must log error, skip event, continue processing)
- How does the system handle duplicate events? (Framework must deduplicate based on post URI to prevent duplicate indexing)
- What happens when feed selection logic is very slow? (Framework must implement timeout and log performance warnings)
- How does the system handle database connection loss during indexing? (Framework must apply exponential backoff retry strategy with dead-letter queue for persistent failures)
- What happens when pagination cursor is invalid or expired (>1 hour old)? (Framework must return appropriate error response per ATProto specification)
- How does the framework handle very large feeds (millions of posts)? (Framework must support efficient database queries with proper indexing)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Framework MUST provide a Maven dependency that can be added to any Java project
- **FR-002**: Framework MUST support configuration via external properties files (Spring Boot conventions)
- **FR-003**: Framework MUST connect to ATProto Jetstream WebSocket event source automatically on startup
- **FR-004**: Framework MUST receive and deserialize ATProto repository events (commit events for posts)
- **FR-005**: Framework MUST provide a FeedProvider interface for developers to implement custom feed selection logic
- **FR-006**: Framework MUST invoke FeedProvider selection logic for each incoming post event
- **FR-007**: Framework MUST index post references (ATProto URIs) for posts accepted by FeedProvider logic
- **FR-008**: Framework MUST expose ATProto Feed Generator API endpoint `/xrpc/app.bsky.feed.getFeedSkeleton`
- **FR-009**: Framework MUST support pagination in feed queries using cursor-based navigation with posts sorted in reverse chronological order (newest first), where cursors expire after 1 hour
- **FR-010**: Framework MUST persist feed index to a relational database (MariaDB as initial target)
- **FR-011**: Framework MUST initialize database schema automatically on first startup
- **FR-012**: Framework MUST recover indexed posts from database on application restart
- **FR-013**: Framework MUST handle event source connection failures with automatic reconnection (exponential backoff)
- **FR-014**: Framework MUST handle event processing errors with exponential backoff retry strategy (3 attempts: 1s, 2s, 4s intervals) and move persistently failing events to a dead-letter queue without stopping the event stream
- **FR-015**: Framework MUST expose health check endpoint compatible with container orchestration systems
- **FR-016**: Framework MUST expose Prometheus-compatible metrics endpoint
- **FR-017**: Framework MUST use structured logging (SLF4J) with INFO as default log level and support runtime configuration of log levels per component
- **FR-018**: Framework MUST validate ATProto feed responses against the specification
- **FR-019**: Framework MUST deduplicate events to prevent indexing the same post multiple times
- **FR-020**: Framework MUST support graceful shutdown (close connections, flush buffers)

### Key Entities

- **EventSource**: Represents the connection to an ATProto event stream (e.g., Jetstream). Manages WebSocket lifecycle, receives repository events, deserializes JSON payloads.
- **RepositoryEvent**: Internal representation of an ATProto commit event. Contains post URI, author DID, content reference, timestamp.
- **FeedProvider**: Interface implemented by developers to define feed selection logic. Single method: `boolean shouldInclude(RepositoryEvent event)`.
- **PostReference**: Indexed representation of a post. Contains post URI, indexed timestamp, optional metadata for ranking.
- **FeedIndex**: Persistent storage for PostReferences. Supports efficient queries for feed generation (time-ordered reverse chronological, paginated).
- **FeedRequest**: Incoming query from ATProto client. Contains feed URI, optional limit, optional pagination cursor.
- **FeedResponse**: ATProto-compliant response. Contains list of post URIs and optional cursor for next page.
- **Configuration**: External configuration values. Includes event source URL, database connection details, feed identifier, default log level (INFO), per-component log level overrides.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can create a working custom feed application with fewer than 100 lines of custom code
- **SC-002**: The framework handles event streams with 1000+ events per minute without memory leaks or degradation
- **SC-003**: Feed queries return results in under 500 milliseconds for feeds with up to 100,000 indexed posts
- **SC-004**: The framework automatically recovers from event source disconnections within 30 seconds
- **SC-005**: Application startup completes in under 10 seconds when database is available
- **SC-006**: 95% of developers successfully deploy their first custom feed within 4 hours of starting (measured through sample application completion time)
- **SC-007**: Zero data loss occurs during normal operation (all accepted posts are successfully indexed)
- **SC-008**: Framework consumes less than 512MB heap memory under steady-state operation with 50,000 indexed posts
- **SC-009**: The framework processes 99.9% of events within 100 milliseconds of receipt
- **SC-010**: All public APIs are documented with Javadoc coverage above 90%

## Assumptions

- **Event Source**: Initial implementation targets Jetstream as the primary event source. Firehose support is deferred to future releases.
- **Database**: MariaDB is the reference persistence implementation. Support for other databases (PostgreSQL, MySQL) is achievable through standard JDBC but not explicitly tested initially.
- **Deployment Environment**: The framework targets containerized deployments (Docker/Kubernetes) with health probes and metrics endpoints.
- **Java Version**: Framework requires Java 17 or later to align with modern Spring Boot practices.
- **Spring Boot**: Framework uses Spring Boot for dependency injection, configuration, and operational features (actuator endpoints).
- **Event Volume**: Framework is designed for feeds processing up to 10,000 events per minute. Higher volumes may require additional optimization.
- **Feed Complexity**: Initial FeedProvider interface assumes stateless selection logic. Stateful feeds (e.g., personalized recommendations) may require additional framework support.
- **Security**: Initial release assumes feed endpoints are publicly accessible. Authentication/authorization for feed queries is deferred to future releases.
- **Testing**: Framework includes unit tests for core components. Integration tests require local Jetstream and database instances.
- **Documentation**: Sample application demonstrates the complete developer journey from setup to published feed.
- **Performance**: Database queries assume proper indexing on post timestamp (descending order for reverse chronological retrieval) and feed ID columns for efficient pagination.
- **Monitoring**: Operators are expected to configure external monitoring systems (Prometheus, Grafana) to consume exposed metrics.
