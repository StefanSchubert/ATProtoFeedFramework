# Tasks: Core Framework Implementation

**Feature Branch**: `001-core-framework-implementation`  
**Input**: Design documents from `/specs/001-core-framework-implementation/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic Maven structure

- [X] T001 Create Maven module structure with framework and sample submodules in pom.xml
- [X] T002 [P] Configure Java 25 compiler target in pom.xml
- [X] T003 [P] Add Spring Boot 4.1.0 parent dependency to pom.xml
- [X] T004 [P] Add core dependencies (Spring Data JPA, MariaDB, WebFlux) to pom.xml
- [X] T005 [P] Add operational dependencies (Actuator, Micrometer, Log4j2) to pom.xml
- [X] T006 [P] Configure JaCoCo plugin with 80% coverage threshold in pom.xml
- [X] T007 [P] Configure OWASP dependency check plugin in pom.xml
- [X] T008 Create package structure src/main/java/de/bluewhale/atprotofeed/framework/
- [X] T009 [P] Create application.yml template with framework defaults in src/main/resources/
- [X] T010 [P] Configure Log4j2.xml with structured logging patterns in src/main/resources/

**Checkpoint**: Project compiles, all dependencies resolve, basic structure ready

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T011 Create Flyway migration script V1__initial_schema.sql in src/main/resources/db/migration/
- [X] T012 [P] Define PostReferenceEntity JPA entity in src/main/java/de/bluewhale/atprotofeed/framework/index/entities/
- [X] T013 [P] Define PaginationCursorEntity JPA entity in src/main/java/de/bluewhale/atprotofeed/framework/index/entities/
- [X] T014 [P] Define DeadLetterEventEntity JPA entity in src/main/java/de/bluewhale/atprotofeed/framework/index/entities/
- [X] T015 [P] Create RepositoryEvent domain record in src/main/java/de/bluewhale/atprotofeed/framework/eventsource/
- [X] T016 [P] Create PostReference domain record in src/main/java/de/bluewhale/atprotofeed/framework/feed/
- [X] T017 [P] Create FeedContext domain record in src/main/java/de/bluewhale/atprotofeed/framework/feed/
- [X] T018 [P] Create FeedRequest DTO record in src/main/java/de/bluewhale/atprotofeed/framework/api/dto/
- [X] T019 [P] Create FeedResponse DTO record in src/main/java/de/bluewhale/atprotofeed/framework/api/dto/
- [X] T020 [P] Create FrameworkProperties configuration class in src/main/java/de/bluewhale/atprotofeed/framework/config/
- [X] T021 [P] Create RetryStrategy utility in src/main/java/de/bluewhale/atprotofeed/framework/resilience/
- [X] T022 [P] Create ExponentialBackoff utility in src/main/java/de/bluewhale/atprotofeed/framework/resilience/
- [X] T023 [P] Create custom exception classes (EventProcessingException, IndexingException) in src/main/java/de/bluewhale/atprotofeed/framework/exception/
- [X] T024 [P] Configure Spring Boot Actuator health endpoint in src/main/resources/application.yml
- [X] T025 [P] Configure Prometheus metrics endpoint in src/main/resources/application.yml
- [X] T026 Create FrameworkAutoConfiguration with component scanning in src/main/java/de/bluewhale/atprotofeed/framework/config/
- [X] T027 Create META-INF/spring.factories for auto-configuration discovery in src/main/resources/

**Checkpoint**: Foundation ready - database schema defined, domain objects created, configuration infrastructure in place

---

## Phase 3: User Story 1 - Framework Setup and Configuration (P1) 🎯 MVP

**Goal**: Enable developers to add framework dependency and configure it to connect to ATProto event source

**Independent Test**: Add framework to new Maven project, provide minimal config, verify successful startup

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T028 [P] [US1] Write acceptance test for dependency resolution in tests/integration/test_framework_setup.java
- [X] T029 [P] [US1] Write acceptance test for successful initialization in tests/integration/test_framework_startup.java
- [X] T030 [P] [US1] Write acceptance test for health endpoint availability in tests/integration/test_health_check.java

### Implementation for User Story 1

- [X] T031 [US1] Implement FrameworkProperties with Jetstream URL and feed ID fields in src/main/java/de/bluewhale/atprotofeed/framework/config/
- [X] T032 [US1] Add @ConfigurationProperties validation annotations to FrameworkProperties
- [X] T033 [US1] Create framework health indicator in src/main/java/de/bluewhale/atprotofeed/framework/health/FrameworkHealthIndicator.java
- [X] T034 [US1] Register health indicator with Spring Boot Actuator
- [X] T035 [US1] Add startup banner with framework version logging in src/main/java/de/bluewhale/atprotofeed/framework/config/
- [X] T036 [US1] Update FrameworkAutoConfiguration to validate required properties on startup

**Checkpoint**: Framework can be added to Maven project, configured, and starts successfully with health endpoint

---

## Phase 4: User Story 2 - Connect to ATProto Event Stream (P1)

**Goal**: Automatically connect to Jetstream and receive repository events

**Independent Test**: Configure Jetstream URL, start application, verify connection and event reception in logs

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T037 [P] [US2] Write contract test for EventSource interface in tests/unit/test_eventsource_contract.java
- [ ] T038 [P] [US2] Write contract test for EventHandler interface in tests/unit/test_eventhandler_contract.java
- [ ] T039 [P] [US2] Write integration test for WebSocket connection in tests/integration/test_jetstream_connection.java
- [ ] T040 [P] [US2] Write integration test for reconnection behavior in tests/integration/test_reconnection.java
- [ ] T041 [P] [US2] Write integration test for graceful shutdown in tests/integration/test_shutdown.java

### Implementation for User Story 2

- [ ] T042 [P] [US2] Define EventSource interface in src/main/java/de/bluewhale/atprotofeed/framework/eventsource/
- [ ] T043 [P] [US2] Define EventHandler functional interface in src/main/java/de/bluewhale/atprotofeed/framework/eventsource/
- [ ] T044 [US2] Implement JetstreamEventSource with ReactorNettyWebSocketClient in src/main/java/de/bluewhale/atprotofeed/framework/eventsource/
- [ ] T045 [US2] Add @PostConstruct connect() method to JetstreamEventSource
- [ ] T046 [US2] Implement exponential backoff retry logic using RetryBackoffSpec in JetstreamEventSource
- [ ] T047 [US2] Add @PreDestroy disconnect() method for graceful shutdown in JetstreamEventSource
- [ ] T048 [US2] Create Jackson ObjectMapper configuration for RepositoryEvent deserialization in src/main/java/de/bluewhale/atprotofeed/framework/config/
- [ ] T049 [US2] Add structured logging for connection events (connected, disconnected, reconnecting) in JetstreamEventSource
- [ ] T050 [US2] Update FrameworkHealthIndicator to monitor EventSource connection status
- [ ] T051 [US2] Create ConnectionStatusMetrics for Micrometer in src/main/java/de/bluewhale/atprotofeed/framework/metrics/

**Checkpoint**: Framework connects to Jetstream, receives events, handles reconnection, reports health status

---

## Phase 5: User Story 3 - Implement Custom Feed Logic (P1)

**Goal**: Provide FeedProvider interface for developers to implement custom selection and ranking logic

**Independent Test**: Implement sample FeedProvider, register it, verify feed query returns correctly filtered/ranked posts

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T052 [P] [US3] Write contract test for FeedProvider interface in tests/unit/test_feedprovider_contract.java
- [ ] T053 [P] [US3] Write unit test for EventProcessor with mock FeedProvider in tests/unit/test_event_processor.java
- [ ] T054 [P] [US3] Write integration test for retry strategy in tests/integration/test_retry_mechanism.java
- [ ] T055 [P] [US3] Write integration test for dead-letter queue in tests/integration/test_dead_letter_queue.java
- [ ] T056 [P] [US3] Write integration test for SimpleFeedProvider example in tests/integration/test_sample_provider.java

### Implementation for User Story 3

- [ ] T057 [P] [US3] Define FeedProvider interface with getFeedId(), shouldIndex(), selectPosts(), enrichMetadata() in src/main/java/de/bluewhale/atprotofeed/framework/feed/
- [ ] T058 [US3] Create EventProcessor that invokes FeedProvider.shouldIndex() on events in src/main/java/de/bluewhale/atprotofeed/framework/eventsource/
- [ ] T059 [US3] Wire EventProcessor to JetstreamEventSource via EventHandler in FrameworkAutoConfiguration
- [ ] T060 [US3] Implement retry logic with 3 attempts (1s, 2s, 4s backoff) in EventProcessor
- [ ] T061 [US3] Create DeadLetterQueue service for persistent event failures in src/main/java/de/bluewhale/atprotofeed/framework/resilience/
- [ ] T062 [US3] Create DeadLetterRepository JPA repository in src/main/java/de/bluewhale/atprotofeed/framework/index/repository/
- [ ] T063 [US3] Add error handling and logging for FeedProvider exceptions in EventProcessor
- [ ] T064 [US3] Create EventProcessingMetrics for Micrometer (events received, processed, failed) in src/main/java/de/bluewhale/atprotofeed/framework/metrics/
- [ ] T065 [US3] Implement SimpleFeedProvider example (accepts all posts) in src/main/java/de/bluewhale/atprotofeed/sample/provider/
- [ ] T066 [US3] Implement TechFeedProvider example (filters by keywords) in src/main/java/de/bluewhale/atprotofeed/sample/provider/
- [ ] T067 [US3] Create sample application entry point SampleFeedApplication.java in src/main/java/de/bluewhale/atprotofeed/sample/

**Checkpoint**: Developers can implement FeedProvider, framework processes events with retry/DLQ, sample providers work

---

## Phase 6: User Story 4 - Query Feed via ATProto API (P2)

**Goal**: Expose ATProto Feed Generator API endpoint for querying feeds

**Independent Test**: Populate index with test data, make GET request to /xrpc/app.bsky.feed.getFeedSkeleton, verify response

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T068 [P] [US4] Write contract test for ATProto API response format in tests/unit/test_atproto_response.java
- [ ] T069 [P] [US4] Write integration test for feed query endpoint in tests/integration/test_feed_api.java
- [ ] T070 [P] [US4] Write integration test for pagination with cursor in tests/integration/test_pagination.java
- [ ] T071 [P] [US4] Write integration test for empty feed response in tests/integration/test_empty_feed.java
- [ ] T072 [P] [US4] Write integration test for expired cursor handling in tests/integration/test_expired_cursor.java

### Implementation for User Story 4

- [ ] T073 [P] [US4] Create FeedController REST controller in src/main/java/de/bluewhale/atprotofeed/framework/api/
- [ ] T074 [P] [US4] Create FeedService orchestration layer in src/main/java/de/bluewhale/atprotofeed/framework/api/
- [ ] T075 [US4] Implement GET /xrpc/app.bsky.feed.getFeedSkeleton endpoint in FeedController
- [ ] T076 [US4] Add request validation (feed URI, limit, cursor) in FeedController
- [ ] T077 [US4] Implement cursor-based pagination logic in FeedService
- [ ] T078 [US4] Create CursorService for cursor generation and validation in src/main/java/de/bluewhale/atprotofeed/framework/api/
- [ ] T079 [US4] Create PaginationCursorRepository JPA repository in src/main/java/de/bluewhale/atprotofeed/framework/index/repository/
- [ ] T080 [US4] Implement cursor expiry check (1 hour TTL) in CursorService
- [ ] T081 [US4] Add ATProto response serialization with Jackson in FeedController
- [ ] T082 [US4] Create ATProtoValidator for response validation in src/main/java/de/bluewhale/atprotofeed/framework/api/validation/
- [ ] T083 [US4] Add error handling for invalid/expired cursors with proper ATProto error responses in FeedController
- [ ] T084 [US4] Create FeedApiMetrics for Micrometer (requests, latency, errors) in src/main/java/de/bluewhale/atprotofeed/framework/metrics/
- [ ] T085 [US4] Add structured logging for API requests in FeedController

**Checkpoint**: Feed API endpoint works, pagination functions correctly, ATProto response format validated

---

## Phase 7: User Story 5 - Persist Feed Index (P2)

**Goal**: Persist feed index to MariaDB for restart-safe operation

**Independent Test**: Configure database, run application, index posts, restart, verify posts still available

### Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T086 [P] [US5] Write contract test for FeedIndex interface in tests/unit/test_feedindex_contract.java
- [ ] T087 [P] [US5] Write integration test for post indexing with Testcontainers MariaDB in tests/integration/test_indexing.java
- [ ] T088 [P] [US5] Write integration test for query performance (100k posts) in tests/integration/test_query_performance.java
- [ ] T089 [P] [US5] Write integration test for deduplication in tests/integration/test_deduplication.java
- [ ] T090 [P] [US5] Write integration test for database connection failure in tests/integration/test_db_failure.java

### Implementation for User Story 5

- [ ] T091 [P] [US5] Define FeedIndex interface with index(), queryFeed(), countPosts(), deleteOldPosts(), exists() in src/main/java/de/bluewhale/atprotofeed/framework/index/
- [ ] T092 [US5] Create PostReferenceRepository JPA repository in src/main/java/de/bluewhale/atprotofeed/framework/index/repository/
- [ ] T093 [US5] Implement MariaDBFeedIndex with PostReferenceRepository in src/main/java/de/bluewhale/atprotofeed/framework/index/
- [ ] T094 [US5] Implement index() method with deduplication check (unique post URI constraint) in MariaDBFeedIndex
- [ ] T095 [US5] Implement queryFeed() with reverse chronological sorting and pagination in MariaDBFeedIndex
- [ ] T096 [US5] Add FeedProvider.selectPosts() invocation to FeedService after querying index
- [ ] T097 [US5] Implement exists() method for deduplication in MariaDBFeedIndex
- [ ] T098 [US5] Wire MariaDBFeedIndex to EventProcessor for indexing accepted posts
- [ ] T099 [US5] Add database connection health check in FrameworkHealthIndicator
- [ ] T100 [US5] Create IndexMetrics for Micrometer (posts indexed, query latency, index size) in src/main/java/de/bluewhale/atprotofeed/framework/metrics/
- [ ] T101 [US5] Implement Flyway schema validation on startup in FrameworkAutoConfiguration
- [ ] T102 [US5] Add structured logging for indexing operations in MariaDBFeedIndex

**Checkpoint**: Posts persist to database, survive restarts, queries work efficiently, deduplication prevents duplicates

---

## Phase 8: User Story 6 - Monitor Framework Health (P3)

**Goal**: Expose health checks and metrics for production monitoring

**Independent Test**: Start application, query health endpoints, verify Prometheus metrics available

### Tests for User Story 6

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T103 [P] [US6] Write integration test for health endpoint in tests/integration/test_health_endpoint.java
- [ ] T104 [P] [US6] Write integration test for readiness probe in tests/integration/test_readiness.java
- [ ] T105 [P] [US6] Write integration test for Prometheus metrics endpoint in tests/integration/test_metrics_endpoint.java
- [ ] T106 [P] [US6] Write integration test for degraded health status in tests/integration/test_degraded_health.java

### Implementation for User Story 6

- [ ] T107 [P] [US6] Create EventSourceHealthIndicator for connection status in src/main/java/de/bluewhale/atprotofeed/framework/health/
- [ ] T108 [P] [US6] Create DatabaseHealthIndicator for MariaDB status in src/main/java/de/bluewhale/atprotofeed/framework/health/
- [ ] T109 [P] [US6] Create FeedProviderHealthIndicator for custom provider validation in src/main/java/de/bluewhale/atprotofeed/framework/health/
- [ ] T110 [US6] Configure composite health indicator aggregation in FrameworkAutoConfiguration
- [ ] T111 [US6] Register custom MeterBinder for all framework metrics in src/main/java/de/bluewhale/atprotofeed/framework/metrics/FrameworkMetrics.java
- [ ] T112 [US6] Add JVM memory metrics (heap, non-heap usage) via Micrometer
- [ ] T113 [US6] Add thread pool metrics for event processing via Micrometer
- [ ] T114 [US6] Configure Prometheus scraping endpoint in application.yml
- [ ] T115 [US6] Add readiness probe endpoint configuration in application.yml
- [ ] T116 [US6] Create structured log entries for health state changes

**Checkpoint**: Health endpoints work, metrics exposed to Prometheus, readiness probes function correctly

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and final validation

- [ ] T117 [P] Create comprehensive Javadoc for all public APIs (FeedProvider, EventSource, FeedIndex)
- [ ] T118 [P] Add package-info.java documentation for each framework package
- [ ] T119 [P] Write README.md for sample application in src/main/java/de/bluewhale/atprotofeed/sample/
- [ ] T120 [P] Create TROUBLESHOOTING.md guide with common issues in docs/
- [ ] T121 [P] Update arc42 Building Block View (05) with implemented components in docs/arc42/
- [ ] T122 [P] Write ADR for WebSocket library choice in docs/arc42/adr/ADR-001-websocket-library.md
- [ ] T123 [P] Write ADR for cursor storage strategy in docs/arc42/adr/ADR-002-cursor-storage.md
- [ ] T124 [P] Write ADR for retry and DLQ mechanism in docs/arc42/adr/ADR-003-retry-dlq.md
- [ ] T125 Validate quickstart.md examples against actual sample application code
- [ ] T126 Run all integration tests with Testcontainers (Jetstream + MariaDB)
- [ ] T127 Run JaCoCo coverage report and verify >80% coverage
- [ ] T128 Run OWASP dependency check and resolve high-severity issues
- [ ] T129 Perform load test (10k events/minute) and validate performance targets
- [ ] T130 Validate feed query performance (<500ms for 100k posts)
- [ ] T131 Validate memory usage (<512MB steady-state with 50k posts)
- [ ] T132 Validate startup time (<10 seconds)
- [ ] T133 Run sample application end-to-end test per quickstart.md
- [ ] T134 Code cleanup and refactoring based on test coverage insights
- [ ] T135 Security hardening review (input validation, error messages)

**Checkpoint**: All quality gates pass, documentation complete, sample application validated

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-8)**: All depend on Foundational phase completion
  - **US1, US2, US3 (Phase 3-5)**: Priority P1 - Core MVP functionality
  - **US4, US5 (Phase 6-7)**: Priority P2 - Complete feature set
  - **US6 (Phase 8)**: Priority P3 - Production monitoring
- **Polish (Phase 9)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P1)**: Depends on US2 completion (needs EventSource) - Must process events before indexing
- **User Story 4 (P2)**: Depends on US3 and US5 (needs indexed posts to query)
- **User Story 5 (P2)**: Can start after Foundational (Phase 2) - Parallel with US1-3, needed by US4
- **User Story 6 (P3)**: Can start after Foundational (Phase 2) - Parallel with all other stories

### Critical Path

```text
Setup (Phase 1)
    ↓
Foundational (Phase 2) ← CRITICAL BLOCKER
    ↓
US1: Framework Setup (P1)
    ↓
US2: Connect to Jetstream (P1)
    ↓
US3: Implement FeedProvider (P1) ← MVP COMPLETE HERE
    ↓
US5: Persist Index (P2) [parallel to US4]
    ↓
US4: Query Feed API (P2) ← FULL FEATURE SET
    ↓
US6: Monitor Health (P3)
    ↓
Polish (Phase 9)
```

### Parallel Opportunities

**Within Setup (Phase 1)**:
- T002-T007: All dependency additions can be done in parallel
- T009-T010: Configuration files can be created in parallel

**Within Foundational (Phase 2)**:
- T012-T014: All JPA entities in parallel (different files)
- T015-T019: All domain records in parallel (different files)
- T020-T023: All configuration/utility classes in parallel
- T024-T025: Actuator configuration in parallel

**Within User Story 1 (Phase 3)**:
- T028-T030: All tests in parallel
- T031-T032: Properties implementation (sequential)
- T033-T036: Health and startup logic can overlap

**Within User Story 2 (Phase 4)**:
- T037-T041: All tests in parallel
- T042-T043: Interface definitions in parallel
- T050-T051: Metrics and health updates in parallel

**Within User Story 3 (Phase 5)**:
- T052-T056: All tests in parallel
- T057, T061-T062: Interface and repository definitions in parallel
- T065-T067: Sample provider implementations in parallel

**Within User Story 4 (Phase 6)**:
- T068-T072: All tests in parallel
- T073-T074: Controller and service in parallel
- T078-T079: Cursor service and repository in parallel

**Within User Story 5 (Phase 7)**:
- T086-T090: All tests in parallel
- T091-T092: Interface and repository in parallel

**Within User Story 6 (Phase 8)**:
- T103-T106: All tests in parallel
- T107-T109: All health indicators in parallel
- T112-T113: Metrics additions in parallel

**Within Polish (Phase 9)**:
- T117-T124: All documentation tasks in parallel
- T129-T132: All performance validation tests in parallel

---

## Parallel Example: User Story 2 (Event Ingestion)

```bash
# Launch all tests for User Story 2 together:
Task: T037 "Write contract test for EventSource interface"
Task: T038 "Write contract test for EventHandler interface"
Task: T039 "Write integration test for WebSocket connection"
Task: T040 "Write integration test for reconnection behavior"
Task: T041 "Write integration test for graceful shutdown"

# After tests written, launch interface definitions:
Task: T042 "Define EventSource interface"
Task: T043 "Define EventHandler functional interface"

# After implementation complete, launch observability tasks:
Task: T050 "Update FrameworkHealthIndicator to monitor EventSource"
Task: T051 "Create ConnectionStatusMetrics for Micrometer"
```

---

## Implementation Strategy

### MVP First (User Stories 1-3 Only)

1. Complete Phase 1: Setup → Project structure ready
2. Complete Phase 2: Foundational → Database schema, domain objects, configuration ready
3. Complete Phase 3: User Story 1 → Framework dependency working
4. Complete Phase 4: User Story 2 → Event ingestion working
5. Complete Phase 5: User Story 3 → Custom feed logic working
6. **STOP and VALIDATE**: Test end-to-end with SimpleFeedProvider
7. Deploy/demo if ready

**MVP Value**: Developers can create custom feeds by implementing FeedProvider and receive events

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (config works!)
3. Add User Story 2 → Test independently → Deploy/Demo (events flowing!)
4. Add User Story 3 → Test independently → Deploy/Demo (MVP!)
5. Add User Story 5 → Test independently → Deploy/Demo (persistence!)
6. Add User Story 4 → Test independently → Deploy/Demo (API complete!)
7. Add User Story 6 → Test independently → Deploy/Demo (monitoring!)
8. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together → Foundation ready
2. Once Foundational is done:
   - Developer A: User Story 1 + 2
   - Developer B: User Story 5 (parallel to A)
   - Developer C: User Story 6 (parallel to A + B)
3. After US1, US2, US5 complete:
   - Developer A: User Story 3
4. After US3 complete:
   - Developer B: User Story 4
5. All developers: Polish phase

---

## Notes

- [P] tasks = different files, no dependencies within phase
- [Story] label maps task to specific user story for traceability
- Each user story should be independently testable after completion
- Verify tests fail (RED) before implementing (GREEN), then refactor
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Constitution compliance: TDD mandatory, >80% coverage gate, ADRs for significant decisions
- Performance targets validated in Phase 9: <500ms queries, <512MB memory, <10s startup
