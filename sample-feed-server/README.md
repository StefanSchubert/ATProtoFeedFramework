# Sample Feed Server

This is a reference implementation demonstrating how to build a custom ATProto feed using the **ATProtoFeedFramework**.

## What This Example Does

The `GermanTechFeedProvider` creates a feed that indexes:
- Posts in German language (`de` or `de-DE`)
- Posts containing technology-related keywords (Java, Spring, Kotlin, programming, etc.)
- Posts ranked in reverse chronological order

## Quick Start

### Prerequisites
- Java 25+
- MariaDB or MySQL database
- Maven 3.9+

### 1. Set Up Database

```bash
# Create database
mysql -u root -p <<EOF
CREATE DATABASE atproto_feed CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'feed_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON atproto_feed.* TO 'feed_user'@'localhost';
FLUSH PRIVILEGES;
EOF
```

### 2. Configure Application

Edit `src/main/resources/application.yml` or set environment variables:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/atproto_feed
    username: feed_user
    password: your_secure_password

atproto:
  feed:
    feed-id: at://did:plc:YOUR_DID/app.bsky.feed.generator/german-tech
```

Or use environment variables:

```bash
export DATABASE_URL=jdbc:mariadb://localhost:3306/atproto_feed
export DATABASE_USER=feed_user
export DATABASE_PASSWORD=your_secure_password
export FEED_ID=at://did:plc:YOUR_DID/app.bsky.feed.generator/german-tech
```

### 3. Build and Run

```bash
# Build
mvn clean package

# Run
java -jar target/sample-feed-server.jar

# Or using Maven
mvn spring-boot:run
```

### 4. Verify Health

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "atproto-framework": {
      "status": "UP",
      "details": {
        "eventSource": "CONNECTED",
        "feedProviders": 1
      }
    }
  }
}
```

## Creating Your Own Feed

### Step 1: Add Framework Dependency

```xml
<dependency>
    <groupId>de.bluewhale</groupId>
    <artifactId>atproto-feed-framework</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Implement FeedProvider

```java
@Component
public class MyCustomFeed implements FeedProvider {
    
    @Override
    public String getFeedId() {
        return "at://did:plc:YOUR_DID/app.bsky.feed.generator/my-feed";
    }
    
    @Override
    public boolean shouldIndex(PostReference post) {
        // Your filtering logic
        return true;
    }
    
    @Override
    public List<PostReference> selectPosts(FeedContext context) {
        // Your ranking logic
        return context.candidatePosts().stream()
            .limit(context.limit())
            .toList();
    }
}
```

### Step 3: Configure and Deploy

See configuration options in `application.yml`.

## Feed Registration

To make your feed discoverable on Bluesky:

1. Deploy your server with a public URL
2. Register your feed at: https://feed-generator-docs.vercel.app/
3. Update your `feed-id` configuration with the registered DID

## Monitoring

### Metrics (Prometheus)
```bash
curl http://localhost:8080/actuator/prometheus
```

Key metrics:
- `atproto_feed_posts_total{feed_id}` - Total indexed posts
- `atproto_feed_indexing_duration_seconds` - Indexing performance
- `atproto_feed_events_received_total` - Event throughput

### Logs
```bash
tail -f logs/application.log
```

## Architecture

```
SampleFeedApplication (Spring Boot)
    ↓
GermanTechFeedProvider (Your Implementation)
    ↓
ATProtoFeedFramework (Auto-configured)
    ├── Jetstream Connection
    ├── Event Processing
    ├── Post Indexing (MariaDB)
    └── Feed API Endpoints
```

## Customization Examples

### 1. Add Scoring Algorithm

```java
@Override
public List<PostReference> selectPosts(FeedContext context) {
    return context.candidatePosts().stream()
        .sorted(Comparator.comparing(this::calculateScore).reversed())
        .limit(context.limit())
        .toList();
}

private int calculateScore(PostReference post) {
    Map<String, Object> metadata = post.metadata();
    Integer relevance = (Integer) metadata.get("relevance_score");
    return relevance != null ? relevance : 0;
}
```

### 2. Filter by Author

```java
@Override
public boolean shouldIndex(PostReference post) {
    return TRUSTED_AUTHORS.contains(post.authorDid());
}
```

### 3. Time-based Filtering

```java
@Override
public List<PostReference> selectPosts(FeedContext context) {
    Instant cutoff = Instant.now().minus(Duration.ofDays(7));
    return context.candidatePosts().stream()
        .filter(p -> p.postCreatedAt().isAfter(cutoff))
        .sorted(Comparator.comparing(PostReference::postCreatedAt).reversed())
        .limit(context.limit())
        .toList();
}
```

## Deployment

### Docker

```dockerfile
FROM eclipse-temurin:25-jre
COPY target/sample-feed-server.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t my-feed-server .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:mariadb://db:3306/atproto_feed \
  -e FEED_ID=at://did:plc:YOUR_DID/app.bsky.feed.generator/my-feed \
  my-feed-server
```

### Kubernetes

See `kubernetes/` directory for deployment manifests.

## Troubleshooting

### Database Connection Issues
- Verify MariaDB is running: `systemctl status mariadb`
- Check credentials in `application.yml`
- Ensure database exists and user has permissions

### Jetstream Connection Issues
- Check network connectivity to `jetstream2.us-west.bsky.network`
- Verify no firewall blocking WebSocket connections
- Check logs for reconnection attempts

### No Posts Being Indexed
- Verify `shouldIndex()` logic
- Check logs for filtering decisions (`logging.level.de.bluewhale.atprotofeed: DEBUG`)
- Confirm Jetstream is sending events (`atproto_feed_events_received_total`)

## Support

- Framework Documentation: See main README.md
- Issues: https://github.com/StefanSchubert/ATProtoFeedFramework/issues
- AT Protocol Docs: https://atproto.com/

## License

See main project LICENSE file.
