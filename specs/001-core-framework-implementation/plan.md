# Implementation Plan: Core Framework Implementation

**Branch**: `001-core-framework-implementation` | **Date**: 2026-07-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-core-framework-implementation/spec.md`

**Note**: This plan translates the functional specification into technical design decisions, research needs, and implementation artifacts.

## Summary

Build the core ATProtoFeedFramework to enable Java developers to create custom ATProto feed applications with minimal boilerplate. The framework abstracts event ingestion (Jetstream WebSocket), indexing (MariaDB persistence), and feed exposure (ATProto Feed Generator API), allowing developers to focus solely on implementing FeedProvider selection and ranking logic. Target: Developers can deploy their first feed with <100 lines of custom code in <4 hours.

## Technical Context

**Language/Version**: Java 25 (leveraging latest JVM performance improvements, virtual threads for scalable WebSocket handling)

**Primary Dependencies**: 
- Spring Boot 4.1.0 (dependency injection, configuration, operational features)
- Spring WebSocket (Jetstream connection)
- Spring Data JPA (database abstraction)
- MariaDB Connector/J (JDBC driver)
- SLF4J + Log4j2 (structured logging)
- Micrometer + Prometheus (metrics)
- Jackson (JSON deserialization for ATProto events)
- Lombok (boilerplate reduction)

**Storage**: MariaDB for persistent feed index (post references, timestamps, metadata)

**Testing**: 
- JUnit 5 (unit testing framework)
- Spring Boot Test (integration testing with TestContainers for MariaDB)
- Testcontainers (ephemeral Jetstream + MariaDB instances for integration tests)
- AssertJ (fluent assertions)

**Target Platform**: Containerized deployments (Docker/Kubernetes) with JVM 25 runtime

**Project Type**: Framework library (JAR artifact) + Sample application demonstrating usage

**Performance Goals**: 
- Process 10,000 events/minute without degradation
- Feed queries <500ms for 100k indexed posts
- Startup <10 seconds
- Memory <512MB under steady-state (50k posts)
- 99.9% event processing <100ms

**Constraints**: 
- Cursor lifetime: 1 hour
- Retry strategy: 3 attempts with exponential backoff (1s, 2s, 4s)
- Log level: INFO default, runtime configurable per component
- Test coverage: >80% for framework core
- Javadoc coverage: >90% for public APIs

**Scale/Scope**: 
- Single framework module with clear interface-first architecture
- Sample application demonstrating framework usage
- Support for 100k+ indexed posts per feed
- Designed for feeds with <10k events/minute throughput

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Test-Driven Development (NON-NEGOTIABLE)

**Status**: ✅ **PASS**

**Approach**:
- All framework interfaces (EventSource, FeedProvider, FeedIndex) will be designed test-first
- Acceptance scenarios from spec translate directly to acceptance tests
- Red-Green-Refactor cycle for all production code
- JUnit 5 + AssertJ for unit tests
- Spring Boot Test + Testcontainers for integration tests
- Coverage gate: >80% for framework core (enforced in CI)

### II. ISO 25010 Quality Standards

**Status**: ✅ **PASS**

**Alignment**:
- **Functional Suitability**: All 20 FRs have acceptance criteria mapped to tests
- **Performance Efficiency**: Performance goals explicit (<500ms queries, <512MB memory)
- **Compatibility**: ATProto Feed Generator API compliance (FR-008, FR-018)
- **Usability**: FeedProvider API designed for developer simplicity (<100 LOC target)
- **Reliability**: Exponential backoff retry (FR-013, FR-014), graceful degradation
- **Security**: OWASP dependency scanning in build pipeline (pom.xml already configured)
- **Maintainability**: Interface-first architecture, Javadoc >90%, SLF4J logging
- **Portability**: Containerized deployment target, Spring Boot conventions

### III. Framework-First Architecture

**Status**: ✅ **PASS**

**Design**:
- Clear framework/application boundary: Framework provides EventSource, FeedIndex, API handling; Application provides FeedProvider implementation
- Extension points well-defined: FeedProvider interface with FeedContext for extensibility
- No unnecessary constraints: FeedProvider can return any ranking algorithm
- Interface-first: EventSource → JetstreamEventSource, FeedIndex → MariaDBFeedIndex
- Breaking API changes: Semantic versioning with MAJOR increment for breaking changes

### IV. Arc42 Documentation Standard

**Status**: ✅ **PASS**

**Plan**:
- Architecture decisions will be recorded as ADRs in `docs/arc42/adr/`
- Building block view already exists (05-building-block-view.md) - will be updated with concrete component design
- Glossary already defined (12_glossary.md) - terms used consistently
- Implementation will update: Solution Strategy (04), Deployment View (07), Cross-Cutting Concepts (08)
- All significant decisions (WebSocket library choice, retry mechanism, cursor storage) documented as ADRs

### V. Production Readiness

**Status**: ✅ **PASS**

**Implementation**:
- **Logging**: SLF4J + Log4j2 (already in pom.xml), INFO default (FR-017)
- **Metrics**: Micrometer + Prometheus for event rate, query latency, memory usage (FR-016)
- **Health Checks**: Spring Boot Actuator `/actuator/health` for K8s probes (FR-015)
- **Configuration**: Externalized via `application.yml` (Spring Boot conventions, FR-002)
- **Security Scanning**: OWASP Dependency Check plugin (already in pom.xml)
- **Error Handling**: Retry logic (FR-014), dead-letter queue, clear error messages
- **Graceful Degradation**: Event stream errors don't stop processing, connection failures trigger reconnect

**Overall Gate**: ✅ **PASS** - All constitution principles satisfied, no violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/001-core-framework-implementation/
├── plan.md              # This file
├── research.md          # Phase 0 output (WebSocket libraries, cursor strategies, retry patterns)
├── data-model.md        # Phase 1 output (PostReference, FeedContext, RepositoryEvent schemas)
├── quickstart.md        # Phase 1 output (Sample application walkthrough)
├── contracts/           # Phase 1 output (FeedProvider API, EventSource API, ATProto responses)
│   ├── feedprovider-interface.md
│   ├── eventsource-interface.md
│   └── atproto-feed-api.md
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Java Framework with Maven structure (interface-first architecture)

src/
├── main/
│   ├── java/
│   │   └── de/bluewhale/atprotofeed/
│   │       ├── framework/                    # Framework core (interfaces-first)
│   │       │   ├── eventsource/             # Event ingestion abstraction
│   │       │   │   ├── EventSource.java     # Interface
│   │       │   │   ├── JetstreamEventSource.java  # Jetstream implementation
│   │       │   │   ├── RepositoryEvent.java
│   │       │   │   └── EventProcessor.java
│   │       │   ├── feed/                    # Feed provider abstraction
│   │       │   │   ├── FeedProvider.java    # Interface (developer implements)
│   │       │   │   ├── FeedContext.java
│   │       │   │   └── PostReference.java
│   │       │   ├── index/                   # Persistence layer
│   │       │   │   ├── FeedIndex.java       # Interface
│   │       │   │   ├── MariaDBFeedIndex.java
│   │       │   │   └── entities/            # JPA entities
│   │       │   │       ├── PostReferenceEntity.java
│   │       │   │       └── CursorEntity.java
│   │       │   ├── api/                     # ATProto Feed API
│   │       │   │   ├── FeedController.java
│   │       │   │   ├── dto/
│   │       │   │   │   ├── FeedRequest.java
│   │       │   │   │   └── FeedResponse.java
│   │       │   │   └── validation/
│   │       │   │       └── ATProtoValidator.java
│   │       │   ├── resilience/              # Retry & error handling
│   │       │   │   ├── RetryStrategy.java
│   │       │   │   ├── DeadLetterQueue.java
│   │       │   │   └── ExponentialBackoff.java
│   │       │   └── config/                  # Framework configuration
│   │       │       ├── FrameworkAutoConfiguration.java
│   │       │       └── FrameworkProperties.java
│   │       └── sample/                       # Sample application
│   │           ├── SampleFeedApplication.java
│   │           └── provider/
│   │               └── SimpleFeedProvider.java
│   └── resources/
│       ├── application.yml                   # Default framework configuration
│       ├── db/migration/                     # Flyway migrations
│       │   └── V1__initial_schema.sql
│       └── META-INF/
│           └── spring.factories              # Spring Boot auto-configuration

tests/
├── unit/                                     # Fast unit tests (interfaces, logic)
│   └── java/de/bluewhale/atprotofeed/
│       ├── eventsource/
│       │   ├── JetstreamEventSourceTest.java
│       │   └── EventProcessorTest.java
│       ├── feed/
│       │   └── FeedProviderContractTest.java
│       ├── index/
│       │   └── MariaDBFeedIndexTest.java
│       ├── api/
│       │   └── FeedControllerTest.java
│       └── resilience/
│           ├── RetryStrategyTest.java
│           └── ExponentialBackoffTest.java
├── integration/                              # Integration tests (Testcontainers)
│   └── java/de/bluewhale/atprotofeed/
│       ├── FrameworkIntegrationTest.java     # Full framework lifecycle
│       ├── JetstreamIntegrationTest.java     # Real WebSocket connection
│       └── DatabaseIntegrationTest.java      # Real MariaDB persistence
└── contract/                                 # ATProto API contract tests
    └── java/de/bluewhale/atprotofeed/
        └── ATProtoFeedAPIContractTest.java   # Validates spec compliance

pom.xml                                       # Maven build configuration (already exists)
```

**Structure Decision**: 

This structure follows **interface-first architecture** principles as clarified in the specification review. Framework components are organized by domain (eventsource, feed, index, api, resilience) with interfaces defined before implementations. The sample application demonstrates usage without polluting the framework core.

Key architectural decisions reflected in structure:
1. **Interfaces First**: Each domain has interface definitions (EventSource, FeedProvider, FeedIndex) before concrete implementations
2. **Framework/Application Boundary**: Clear separation between `framework/` (reusable infrastructure) and `sample/` (developer-implemented logic)
3. **Layer Independence**: Event source, feed logic, indexing, and API layers are independently testable
4. **Test Strategy**: Three test tiers (unit for interfaces, integration for infrastructure, contract for API compliance)
5. **Spring Boot Conventions**: Auto-configuration in `META-INF/spring.factories` for zero-config framework usage

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. All constitution principles pass without requiring complexity justification.

---

## Phase 0: Research Artifacts ✅

**Completed**: All technical unknowns resolved. See [research.md](./research.md) for detailed findings.

**Key Decisions**:
1. **WebSocket Library**: Spring WebFlux WebSocket Client (already in Spring Boot 4.1.0, virtual threads compatible)
2. **Cursor Storage**: MariaDB table with 1-hour TTL (restart-safe, multi-instance ready)
3. **Dead-Letter Queue**: MariaDB table with status lifecycle (PENDING_REVIEW → IN_RETRY → RESOLVED/DISCARDED)
4. **ATProto Event Schema**: Jackson POJOs with lenient parsing (`@JsonIgnoreProperties(ignoreUnknown = true)`)
5. **Spring Boot Auto-Configuration**: Single `@Configuration` class with `META-INF/spring.factories` registration

---

## Phase 1: Design Artifacts ✅

### Data Model

**File**: [data-model.md](./data-model.md)

**Contents**:
- **JPA Entities** (3): PostReferenceEntity, PaginationCursorEntity, DeadLetterEventEntity
- **Domain Records** (5): RepositoryEvent, PostReference, FeedContext, FeedRequest, FeedResponse
- **SQL Schemas**: CREATE TABLE statements with indexes and constraints
- **Validation Rules**: Entity-level constraints (e.g., `post_uri` length limits, timestamp defaults)
- **Data Flow**: Diagram showing event → entity → domain object flow

**Key Design Decisions**:
- Composite unique key: `(feed_id, post_uri)` prevents duplicate indexing
- Index: `(feed_id, indexed_at DESC)` optimizes reverse chronological queries (meets SC-003: <500ms for 100k posts)
- JSON metadata column: Stores custom ranking data without schema changes

---

### Interface Contracts

**Directory**: [contracts/](./contracts/)

#### 1. EventSource Interface

**File**: [contracts/eventsource-interface.md](./contracts/eventsource-interface.md)

**Methods**:
- `connect()` - Establish WebSocket connection to Jetstream
- `subscribe(EventHandler)` - Register event handler for incoming posts
- `disconnect()` - Graceful shutdown
- `getStatus()` - Connection health (CONNECTED, DISCONNECTED, CONNECTING, ERROR)

**Implementation**: `JetstreamEventSource` with exponential backoff reconnection (1s → 30s max)

**Error Handling**: Connection failures trigger auto-reconnect; deserialization errors are logged and skipped

---

#### 2. FeedProvider Interface

**File**: [contracts/feedprovider-interface.md](./contracts/feedprovider-interface.md)

**Methods** (Enhanced based on ChatGPT feedback):
- `getFeedId()` - Return ATProto feed URI
- `shouldIndex(PostReference)` - Filter criteria for incoming posts
- `selectPosts(FeedContext)` - **Core method**: Select and rank posts for feed queries
- `enrichMetadata(PostReference)` - Optional: Add custom ranking metadata

**Example Implementations**:
1. Simple chronological feed: 15 LOC ✅
2. Author-specific feed: ~20 LOC ✅
3. Topic-based feed with scoring: ~40 LOC ✅

**Future Extensions**: Supports geodata, engagement scoring, topic classification without API breaks

---

#### 3. FeedIndex Interface

**File**: [contracts/feedindex-interface.md](./contracts/feedindex-interface.md)

**Methods**:
- `index(PostReference, String feedId)` - Persist post to feed index
- `queryFeed(String feedId, String cursor, int limit)` - Retrieve paginated posts
- `countPosts(String feedId)` - Metrics support
- `deleteOldPosts(String feedId, Instant olderThan)` - Cleanup task
- `exists(String postUri, String feedId)` - Duplicate check

**Implementation**: `MariaDBFeedIndex` with HikariCP connection pooling

**Performance**: Cursor-based pagination (no OFFSET) meets SC-003 (<500ms for 100k posts)

---

#### 4. ATProto Feed API

**File**: [contracts/atproto-feed-api.md](./contracts/atproto-feed-api.md)

**Endpoint**: `GET /xrpc/app.bsky.feed.getFeedSkeleton`

**Parameters**:
- `feed` (required): ATProto feed URI
- `cursor` (optional): Pagination cursor (Base64-encoded timestamp)
- `limit` (optional): Page size (1-100, default: 50)

**Response**:
```json
{
  "feed": [{"post": "at://..."}],
  "cursor": "eyJ..."
}
```

**Error Responses**: 400 (invalid params), 404 (feed not found), 500 (query failure), 503 (unhealthy)

**Security**: Rate limiting (5000 req/hour), CORS enabled, input validation, cursor HMAC signatures

---

### Quickstart Guide

**File**: [quickstart.md](./quickstart.md)

**Contents**:
- **Prerequisites**: Java 25, Maven, MariaDB setup
- **Project Setup**: Maven dependency configuration (2 minutes)
- **Database Configuration**: `application.yml` + Docker MariaDB (1 minute)
- **FeedProvider Implementation**: 3 complete examples (5 minutes)
  1. Simple "Recent Posts" feed (15 LOC)
  2. Author-specific feed (20 LOC)
  3. Keyword-based feed with metadata enrichment (40 LOC)
- **Validation Scenarios**: 5 end-to-end test scenarios
  1. Event ingestion verification
  2. Feed filtering correctness
  3. Custom ranking validation
  4. Pagination correctness (no duplicates)
  5. Performance under load (100k posts)
- **Troubleshooting**: Common issues and solutions
- **Deployment**: Docker Compose + Cloud deployment options

**Success Criteria Validation**: All SC-001 through SC-010 validated in quickstart scenarios

---

## Post-Design Constitution Check ✅

Re-evaluating all 5 principles against Phase 1 design artifacts:

### 1. Test-Driven Development (TDD) ✅

**Status**: PASS

**Evidence**:
- **User Story → Acceptance Scenario**: FR-001 (event ingestion) maps to "Scenario 1: Event Ingestion" in quickstart
- **Test Fixtures**: `quickstart.md` includes `@DataJpaTest` examples with H2 in-memory database
- **Coverage Gate**: `pom.xml` already contains `jacoco-maven-plugin` configured with 80% threshold (line 143-158)
- **Test Structure**: Three-tier testing (unit/integration/contract) defined in Project Structure

**Design Validation**: data-model.md includes "Testing Guidelines" for each entity; contracts include "Testing Strategies" sections

---

### 2. ISO 25010 Quality Standards ✅

**Status**: PASS

**Evidence**: All 8 characteristics addressed in design:

| Characteristic | Design Coverage |
|----------------|-----------------|
| **Functional Suitability** | 20 FRs mapped to contracts (FeedProvider, EventSource, FeedIndex) |
| **Performance Efficiency** | SC-003 (feed queries <500ms) validated via index design `(feed_id, indexed_at DESC)` |
| **Compatibility** | ATProto API contract compliance (eventsource-interface.md, atproto-feed-api.md) |
| **Usability** | Quickstart guide demonstrates <100 LOC implementation (SC-001) |
| **Reliability** | Retry strategy (research.md), DLQ (data-model.md: DeadLetterEventEntity) |
| **Security** | Rate limiting, CORS, input validation (atproto-feed-api.md: Security Considerations) |
| **Maintainability** | Interface-first architecture (all contracts define clear extension points) |
| **Portability** | Docker Compose deployment (quickstart.md: "Deploy to Production") |

---

### 3. Framework-First Architecture ✅

**Status**: PASS

**Evidence**:
- **Clear Boundary**: `framework/` (reusable) vs. `sample/` (developer-implemented) in Project Structure
- **Minimal API Surface**: Single developer-facing interface (`FeedProvider`) with 4 methods
- **Zero Configuration**: Spring Boot auto-configuration (`FrameworkAutoConfiguration`) enables instant usage
- **Quickstart Validation**: 3 working examples (15-40 LOC) demonstrate minimal integration effort

**Design Validation**: feedprovider-interface.md documents framework responsibilities (event ingestion, indexing, API) vs. developer responsibilities (selection logic, ranking)

---

### 4. Arc42 Documentation Standard ✅

**Status**: PASS

**Evidence**:
- **Building Block View**: Project Structure section maps to arc42 Section 5 (components identified)
- **Runtime View**: data-model.md includes "Data Flow" diagram showing event → processing → persistence flow
- **Deployment View**: quickstart.md includes Docker Compose deployment architecture
- **Concepts**: contracts/ documents key patterns (cursor-based pagination, exponential backoff reconnection)

**Future Action**: After implementation, update `docs/arc42/08-concepts.md` with ADRs for:
- WebSocket library choice (Spring WebFlux vs. alternatives)
- DLQ implementation (MariaDB vs. messaging middleware)
- Cursor format (Base64 JSON vs. opaque tokens)

---

### 5. Production Readiness ✅

**Status**: PASS

**Evidence**:

| Requirement | Design Coverage |
|-------------|-----------------|
| **Health Checks** | eventsource-interface.md: `getStatus()` method + Spring Boot Actuator `/actuator/health` |
| **Metrics** | All contracts define Prometheus metrics (e.g., `atproto.index.posts.total`, `atproto.feed.request.duration`) |
| **Logging** | research.md: SLF4J + Log4j2, INFO default, structured logging examples in all contracts |
| **Error Handling** | Retry strategy (FR-014), dead-letter queue (DeadLetterEventEntity in data-model.md) |
| **Security** | atproto-feed-api.md: Rate limiting, CORS, input validation, SQL injection prevention (JPA parameterized queries) |
| **Deployment** | quickstart.md: Docker Compose + Cloud deployment guides |
| **Monitoring** | All contracts include metrics section (Counter, Histogram, Gauge) |
| **OWASP Scanning** | `pom.xml` already includes `dependency-check-maven` plugin (line 120-141) |

---

## Validation: Design → Spec Traceability

Confirming all functional requirements (FR-001 to FR-020) have design coverage:

| Functional Requirement | Design Artifact | Section |
|------------------------|-----------------|---------|
| FR-001: Jetstream connection | eventsource-interface.md | `connect()` method |
| FR-002: Event deserialization | research.md | "ATProto Jetstream Schema" → Jackson POJOs |
| FR-003: Filter to post events | eventsource-interface.md | "Filtering" section |
| FR-004: shouldIndex() invocation | feedprovider-interface.md | Lifecycle section |
| FR-005: Post indexing | feedindex-interface.md | `index()` method |
| FR-006: Feed queries | atproto-feed-api.md | Full endpoint specification |
| FR-007: selectPosts() invocation | feedprovider-interface.md | `selectPosts(FeedContext)` |
| FR-008: Result serialization | atproto-feed-api.md | Response format |
| FR-009: Cursor pagination | feedindex-interface.md | "Pagination Details" + data-model.md: PaginationCursorEntity |
| FR-010: Limit validation | atproto-feed-api.md | "Parameter Validation" (1-100 range) |
| FR-011: Empty result handling | atproto-feed-api.md | "Empty Feed Response" |
| FR-012: Multi-feed support | feedindex-interface.md | `feedId` parameter in all methods |
| FR-013: Connection retry | eventsource-interface.md | "Reconnection" behavior (exponential backoff) |
| FR-014: Event retry + DLQ | research.md: "Dead-Letter Queue Implementation" + data-model.md: DeadLetterEventEntity |
| FR-015: Health endpoint | eventsource-interface.md: `getStatus()` + atproto-feed-api.md: "Health Check" |
| FR-016: Metrics exposure | All contracts: "Metrics" sections |
| FR-017: Structured logging | research.md: SLF4J + Log4j2 decision + all contracts: "Logging" examples |
| FR-018: Database migrations | Project Structure: `db/migration/V1__initial_schema.sql` |
| FR-019: Zero-config setup | research.md: "Spring Boot Auto-Configuration" |
| FR-020: Sample application | Project Structure: `sample/` directory + quickstart.md: 3 examples |

✅ **All 20 functional requirements have design coverage**

---

## Summary

Phase 1 design complete with **no constitution violations**. All artifacts traceable to specification requirements.

**Generated Artifacts**:
1. ✅ research.md (5 technical decisions)
2. ✅ data-model.md (3 entities + 5 domain objects)
3. ✅ contracts/eventsource-interface.md (EventSource + EventHandler)
4. ✅ contracts/feedprovider-interface.md (FeedProvider + FeedContext)
5. ✅ contracts/feedindex-interface.md (FeedIndex + FeedQueryResult)
6. ✅ contracts/atproto-feed-api.md (REST endpoint specification)
7. ✅ quickstart.md (Developer guide with 3 examples + 5 validation scenarios)

**Ready for**: Task generation (next: `/speckit.tasks` or `/speckit.implement`)
