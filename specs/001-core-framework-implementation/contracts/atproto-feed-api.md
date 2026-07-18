# ATProto Feed Generator API Contract

**Endpoint**: `/xrpc/app.bsky.feed.getFeedSkeleton`  
**Protocol**: HTTP/1.1 or HTTP/2  
**Authentication**: None (public feeds)  
**Specification**: [Bluesky Feed Generator Specification](https://github.com/bluesky-social/feed-generator)

## Purpose

This contract defines the HTTP API exposed by the framework to serve custom ATProto feeds. This endpoint is queried by Bluesky clients (web, mobile apps) to render feed timelines.

The framework provides:
- **Endpoint implementation**: Spring Boot `@RestController`
- **Request validation**: Parameter validation, cursor decoding
- **Response serialization**: JSON-LD serialization per ATProto spec
- **Error handling**: Standard ATProto error responses

Developers **do not** implement this endpoint; they only provide `FeedProvider` implementations.

---

## Request Specification

### HTTP Method

```
GET /xrpc/app.bsky.feed.getFeedSkeleton
```

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `feed` | string | ✅ Yes | Feed identifier (ATProto URI) |
| `cursor` | string | No | Opaque pagination cursor |
| `limit` | integer | No | Max posts to return (default: 50, range: 1-100) |

### Example Request

```http
GET /xrpc/app.bsky.feed.getFeedSkeleton?feed=at://did:plc:abc123/app.bsky.feed.generator/tech&limit=30 HTTP/1.1
Host: feed.example.com
Accept: application/json
```

### Parameter Validation

| Rule | Error Response |
|------|----------------|
| `feed` missing | 400 Bad Request: "Missing required parameter: feed" |
| `feed` not found | 404 Not Found: "Feed not found" |
| `limit < 1` or `limit > 100` | 400 Bad Request: "Limit must be between 1 and 100" |
| `cursor` malformed | 400 Bad Request: "Invalid cursor format" |

---

## Response Specification

### Success Response (200 OK)

```json
{
  "feed": [
    {
      "post": "at://did:plc:xyz789/app.bsky.feed.post/3k2j5h3k2j5h"
    },
    {
      "post": "at://did:plc:abc123/app.bsky.feed.post/9m8n7b6v5c4x"
    }
  ],
  "cursor": "eyJpbmRleGVkX2F0IjoiMjAyNC0wMS0xNVQxMDozMDowMFoiLCJpZCI6MTIzNDU2fQ=="
}
```

**Fields**:
- `feed` (array, required): List of post references (max `limit` items)
  - `post` (string, required): ATProto post URI
- `cursor` (string, optional): Pagination cursor for next page (omit if last page)

### Response Headers

```
Content-Type: application/json; charset=utf-8
X-RateLimit-Limit: 5000
X-RateLimit-Remaining: 4999
X-RateLimit-Reset: 1705334400
```

### Empty Feed Response

```json
{
  "feed": []
}
```

**Note**: `cursor` field is omitted when feed is empty or last page reached.

---

## Error Responses

### 400 Bad Request (Invalid Parameters)

```json
{
  "error": "InvalidRequest",
  "message": "Limit must be between 1 and 100"
}
```

### 404 Not Found (Feed Not Found)

```json
{
  "error": "NotFound",
  "message": "Feed not found: at://did:plc:abc123/app.bsky.feed.generator/unknown"
}
```

### 500 Internal Server Error (Query Failure)

```json
{
  "error": "InternalServerError",
  "message": "Failed to query feed index"
}
```

### 503 Service Unavailable (Unhealthy)

```json
{
  "error": "ServiceUnavailable",
  "message": "Feed service is starting up"
}
```

---

## Pagination Details

### First Page Request

```http
GET /xrpc/app.bsky.feed.getFeedSkeleton?feed=at://did:plc:abc123/app.bsky.feed.generator/tech&limit=50
```

**Response**:
```json
{
  "feed": [ /* 50 posts */ ],
  "cursor": "eyJpbmRleGVkX2F0IjoiMjAyNC0wMS0xNVQxMDozMDowMFoiLCJpZCI6MTIzNDU2fQ=="
}
```

### Subsequent Page Request

```http
GET /xrpc/app.bsky.feed.getFeedSkeleton?feed=at://did:plc:abc123/app.bsky.feed.generator/tech&limit=50&cursor=eyJpbmRleGVkX2F0IjoiMjAyNC0wMS0xNVQxMDozMDowMFoiLCJpZCI6MTIzNDU2fQ==
```

**Response** (last page):
```json
{
  "feed": [ /* 20 posts */ ]
}
```

**Note**: `cursor` field is omitted → client knows this is the last page.

---

## Performance Requirements

| Metric | Target | Reasoning |
|--------|--------|-----------|
| Response time (p99) | <500ms | Spec SC-003 (100k posts in index) |
| Response time (p50) | <100ms | User experience (perceived latency) |
| Throughput | 100 req/s | Typical feed load |
| Concurrent requests | 500 | Burst traffic (new posts viral) |

**Optimization**:
- Database connection pooling (HikariCP)
- Index optimization: `(feed_id, indexed_at DESC)`
- Cursor-based pagination (avoids OFFSET)

---

## Implementation (Framework-Provided)

### Spring Boot Controller

```java
@RestController
@RequestMapping("/xrpc/app.bsky.feed")
public class FeedController {
    
    private final FeedService feedService;
    
    @GetMapping("/getFeedSkeleton")
    public ResponseEntity<FeedSkeletonResponse> getFeedSkeleton(
        @RequestParam String feed,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "50") int limit
    ) {
        // Validate
        if (limit < 1 || limit > 100) {
            throw new InvalidRequestException("Limit must be between 1 and 100");
        }
        
        // Query
        try {
            var result = feedService.queryFeed(feed, cursor, limit);
            return ResponseEntity.ok(result);
        } catch (FeedNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidCursorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("InvalidRequest", e.getMessage()));
    }
}
```

### FeedService (Orchestration Layer)

```java
@Service
public class FeedService {
    
    private final FeedIndex feedIndex;
    private final Map<String, FeedProvider> feedProviders;
    
    public FeedSkeletonResponse queryFeed(String feedId, String cursor, int limit) {
        // 1. Find FeedProvider
        var provider = feedProviders.get(feedId);
        if (provider == null) {
            throw new FeedNotFoundException("Feed not found: " + feedId);
        }
        
        // 2. Query index
        var queryResult = feedIndex.queryFeed(feedId, cursor, limit);
        
        // 3. Build FeedContext
        var context = FeedContext.builder()
            .feedId(feedId)
            .candidatePosts(queryResult.posts())
            .request(FeedRequest.of(feedId, cursor, limit))
            .build();
        
        // 4. Let FeedProvider select/rank posts
        var selectedPosts = provider.selectPosts(context);
        
        // 5. Convert to ATProto response
        var feedItems = selectedPosts.stream()
            .map(post -> new FeedPost(post.postUri()))
            .toList();
        
        return FeedSkeletonResponse.builder()
            .feed(feedItems)
            .cursor(queryResult.nextCursor())
            .build();
    }
}
```

---

## Security Considerations

### Rate Limiting

**Configuration**:
```yaml
atproto:
  framework:
    rate-limit:
      enabled: true
      max-requests: 5000
      window-seconds: 3600  # 5000 req/hour per IP
```

**Implementation**: Spring Boot Filter with Redis (distributed) or ConcurrentHashMap (single-instance).

**Response Headers**:
```
X-RateLimit-Limit: 5000
X-RateLimit-Remaining: 4999
X-RateLimit-Reset: 1705334400
```

**Error (429 Too Many Requests)**:
```json
{
  "error": "RateLimitExceeded",
  "message": "Rate limit exceeded, retry after 3600 seconds"
}
```

### Input Validation

| Attack Vector | Mitigation |
|---------------|------------|
| SQL Injection | Parameterized queries (JPA) |
| Cursor Tampering | HMAC signature validation |
| Feed ID Forgery | Whitelist validation (registered FeedProviders only) |
| Limit Overflow | Max limit: 100 (enforced) |

### CORS Configuration

```yaml
atproto:
  framework:
    cors:
      allowed-origins: "*"  # Public API
      allowed-methods: [GET, OPTIONS]
      max-age: 3600
```

---

## Monitoring & Observability

### Metrics (Prometheus)

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `atproto.feed.requests.total` | Counter | `feed_id`, `status` | Total requests per feed |
| `atproto.feed.request.duration` | Histogram | `feed_id` | Request latency distribution |
| `atproto.feed.errors.total` | Counter | `feed_id`, `error_type` | Error count by type |
| `atproto.feed.posts.returned.total` | Counter | `feed_id` | Total posts returned |

### Logging (SLF4J)

**Info Level** (default):
```
2024-01-15 10:30:00 INFO  FeedController - Feed query: feed=at://did:plc:abc123/app.bsky.feed.generator/tech, cursor=null, limit=50, duration=45ms, posts=50
```

**Error Level**:
```
2024-01-15 10:30:00 ERROR FeedController - Feed query failed: feed=at://did:plc:abc123/app.bsky.feed.generator/tech, error=DatabaseConnectionException
```

### Health Check

**Endpoint**: `/actuator/health`

**Response**:
```json
{
  "status": "UP",
  "components": {
    "feedService": {
      "status": "UP",
      "details": {
        "registeredFeeds": 5,
        "totalPosts": 123456
      }
    },
    "database": {
      "status": "UP"
    },
    "jetstream": {
      "status": "UP",
      "details": {
        "connectionStatus": "CONNECTED",
        "uptime": "3600s"
      }
    }
  }
}
```

---

## Testing Strategies

### Unit Test (Controller)

```java
@WebMvcTest(FeedController.class)
class FeedControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private FeedService feedService;
    
    @Test
    void getFeedSkeletonReturnsOk() throws Exception {
        var response = FeedSkeletonResponse.builder()
            .feed(List.of(new FeedPost("at://did:plc:123/app.bsky.feed.post/abc")))
            .build();
        
        when(feedService.queryFeed(any(), any(), anyInt())).thenReturn(response);
        
        mockMvc.perform(get("/xrpc/app.bsky.feed.getFeedSkeleton")
                .param("feed", "at://did:plc:abc123/app.bsky.feed.generator/test")
                .param("limit", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.feed").isArray())
            .andExpect(jsonPath("$.feed[0].post").value("at://did:plc:123/app.bsky.feed.post/abc"));
    }
    
    @Test
    void invalidLimitReturns400() throws Exception {
        mockMvc.perform(get("/xrpc/app.bsky.feed.getFeedSkeleton")
                .param("feed", "at://did:plc:abc123/app.bsky.feed.generator/test")
                .param("limit", "200"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("InvalidRequest"));
    }
}
```

### Integration Test (Full Stack)

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class FeedApiIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.0");
    
    @Test
    void fullPipelineTest() {
        // 1. Register FeedProvider (Spring Boot auto-wiring)
        // 2. Index test posts
        indexTestPosts();
        
        // 3. Query feed via HTTP
        var response = RestAssured.given()
            .port(port)
            .queryParam("feed", "at://did:plc:test/app.bsky.feed.generator/test")
            .queryParam("limit", 50)
            .when()
            .get("/xrpc/app.bsky.feed.getFeedSkeleton")
            .then()
            .statusCode(200)
            .extract()
            .as(FeedSkeletonResponse.class);
        
        assertThat(response.feed()).hasSize(50);
    }
}
```

### Load Test (Gatling)

```scala
class FeedApiLoadTest extends Simulation {
  
  val httpProtocol = http.baseUrl("http://localhost:8080")
  
  val scn = scenario("Feed Query Load Test")
    .exec(http("Get Feed")
      .get("/xrpc/app.bsky.feed.getFeedSkeleton")
      .queryParam("feed", "at://did:plc:test/app.bsky.feed.generator/load-test")
      .queryParam("limit", 50)
      .check(status.is(200))
      .check(jsonPath("$.feed[*].post").count.gte(1)))
  
  setUp(
    scn.inject(
      rampUsersPerSec(10) to 100 during (60 seconds),
      constantUsersPerSec(100) during (180 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile(99).lt(500),  // SC-003
     global.successfulRequests.percent.gt(99.9)
   )
}
```

---

## ATProto Compliance Checklist

- ✅ **Endpoint path**: `/xrpc/app.bsky.feed.getFeedSkeleton`
- ✅ **HTTP methods**: GET, OPTIONS (CORS)
- ✅ **Query parameters**: `feed`, `cursor`, `limit`
- ✅ **Response format**: JSON with `feed` array and optional `cursor`
- ✅ **Post URI format**: `at://{did}/{collection}/{rkey}`
- ✅ **Pagination**: Cursor-based (opaque strings)
- ✅ **Error responses**: Standard ATProto error objects
- ✅ **Rate limiting**: Headers (`X-RateLimit-*`)
- ✅ **CORS**: Enabled for public access

---

## Client Integration Example

### JavaScript (Bluesky Web Client)

```javascript
async function fetchFeed(feedUri, cursor = null, limit = 50) {
  const params = new URLSearchParams({
    feed: feedUri,
    limit: limit.toString()
  });
  
  if (cursor) {
    params.append('cursor', cursor);
  }
  
  const response = await fetch(
    `https://feed.example.com/xrpc/app.bsky.feed.getFeedSkeleton?${params}`,
    { headers: { 'Accept': 'application/json' } }
  );
  
  if (!response.ok) {
    throw new Error(`Feed query failed: ${response.status}`);
  }
  
  return response.json();
}

// Usage
const result = await fetchFeed('at://did:plc:abc123/app.bsky.feed.generator/tech');
console.log(`Fetched ${result.feed.length} posts`);

// Pagination
if (result.cursor) {
  const nextPage = await fetchFeed('at://...', result.cursor);
}
```

---

## Related Contracts

- [FeedProvider Interface](./feedprovider-interface.md) - Implements feed selection logic
- [FeedIndex Interface](./feedindex-interface.md) - Provides post persistence
- [EventSource Interface](./eventsource-interface.md) - Ingests ATProto events
