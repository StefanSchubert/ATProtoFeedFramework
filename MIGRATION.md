# Migration Guide

## v0.2.0-SNAPSHOT - Multi-Module Architecture

### Overview

Version 0.2.0 introduces a **breaking architectural change** by refactoring the project into a multi-module Maven structure. This separates the reusable framework from application code, making the framework consumable as a Maven dependency.

### What Changed

#### Project Structure

**Before (0.1.0-SNAPSHOT):**
```
ATProtoFeedFramework/
├── src/main/java/...
├── src/main/resources/...
└── pom.xml
```

**After (0.2.0-SNAPSHOT):**
```
ATProtoFeedFramework/
├── atproto-feed-api/          # Pure Java contracts
├── atproto-feed-framework/    # Spring Boot implementation
├── sample-feed-server/         # Reference application
└── pom.xml                     # Parent POM
```

#### Package Structure

All packages have been reorganized:

| Old Package | New Package |
|------------|-------------|
| `de.bluewhale.atprotofeed.framework.*` | `de.bluewhale.atprotofeed.api.*` (API module) |
| `de.bluewhale.atprotofeed.framework.config.*` | `de.bluewhale.atprotofeed.framework.config.*` (Framework module) |
| `de.bluewhale.atprotofeed.framework.health.*` | `de.bluewhale.atprotofeed.framework.health.*` (Framework module) |

#### Maven Coordinates

The project now publishes three artifacts:

```xml
<!-- API only (pure Java contracts) -->
<dependency>
    <groupId>de.bluewhale</groupId>
    <artifactId>atproto-feed-api</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>

<!-- Framework (includes API transitively) -->
<dependency>
    <groupId>de.bluewhale</groupId>
    <artifactId>atproto-feed-framework</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

### Breaking Changes

#### 1. PostReference API Changes

**Removed methods:**
- `PostReference.toEntity()` - Use `PostReferenceMapper.toEntity()` instead
- `PostReference.fromEntity()` - Use `PostReferenceMapper.fromEntity()` instead

**Rationale:** API layer must not contain persistence logic.

**Migration:**

Before:
```java
PostReferenceEntity entity = postReference.toEntity();
PostReference post = PostReference.fromEntity(entity);
```

After:
```java
@Autowired
private PostReferenceMapper mapper;

PostReferenceEntity entity = mapper.toEntity(postReference);
PostReference post = mapper.fromEntity(entity);
```

#### 2. Annotation Changes

**Changed:** `org.springframework.lang.@Nullable` → `jakarta.annotation.@Nullable`

**Rationale:** API module has no Spring dependencies.

**Migration:**

Before:
```java
import org.springframework.lang.Nullable;

public record PostReference(@Nullable Map<String, Object> metadata) {}
```

After:
```java
import jakarta.annotation.Nullable;

public record PostReference(@Nullable Map<String, Object> metadata) {}
```

#### 3. Import Path Changes

All imports from the old `framework.*` package must be updated:

Before:
```java
import de.bluewhale.atprotofeed.framework.FeedProvider;
import de.bluewhale.atprotofeed.framework.FeedIndex;
```

After:
```java
import de.bluewhale.atprotofeed.api.feed.FeedProvider;
import de.bluewhale.atprotofeed.api.index.FeedIndex;
```

### Migration Steps

#### For Framework Contributors

1. **Update your local repository:**
   ```bash
   git pull origin main
   mvn clean install
   ```

2. **Build order:**
   ```bash
   # Install parent POM
   mvn install -N
   
   # Build all modules
   mvn clean verify
   ```

#### For Application Developers

If you were building on top of the old monolithic structure:

1. **Update your `pom.xml`:**

   Before:
   ```xml
   <dependency>
       <groupId>de.bluewhale</groupId>
       <artifactId>ATProtoFeedFramework</artifactId>
       <version>0.1-SNAPSHOT</version>
   </dependency>
   ```

   After:
   ```xml
   <dependency>
       <groupId>de.bluewhale</groupId>
       <artifactId>atproto-feed-framework</artifactId>
       <version>0.2.0-SNAPSHOT</version>
   </dependency>
   ```

2. **Update package imports:**
   
   Run a global search-and-replace in your IDE:
   - `de.bluewhale.atprotofeed.framework.FeedProvider` → `de.bluewhale.atprotofeed.api.feed.FeedProvider`
   - `de.bluewhale.atprotofeed.framework.FeedIndex` → `de.bluewhale.atprotofeed.api.index.FeedIndex`
   - `de.bluewhale.atprotofeed.framework.PostReference` → `de.bluewhale.atprotofeed.api.dto.PostReference`
   - etc.

3. **Update `@Nullable` annotations:**
   
   Replace:
   ```java
   import org.springframework.lang.Nullable;
   ```
   
   With:
   ```java
   import jakarta.annotation.Nullable;
   ```

4. **Refactor entity conversion code:**

   If your code was calling `PostReference.toEntity()` or `PostReference.fromEntity()`, inject `PostReferenceMapper` and use its methods instead.

### Testing Your Migration

1. **Verify compilation:**
   ```bash
   mvn clean compile
   ```

2. **Run tests:**
   ```bash
   mvn test
   ```

3. **Start application:**
   ```bash
   mvn spring-boot:run
   ```

### Reference Implementation

See the [`sample-feed-server`](sample-feed-server) module for a complete working example demonstrating the new structure.

### Benefits of This Migration

✅ **Clean separation of concerns** - API, framework, and application are distinct  
✅ **Framework as a library** - No longer a monolithic application  
✅ **Reusable across projects** - Add as Maven dependency  
✅ **Stable API contracts** - API module has minimal dependencies  
✅ **Spring Boot conventions** - Follows Spring Boot starter patterns  

### Getting Help

- Review the [sample-feed-server README](sample-feed-server/README.md)
- Check the [project documentation](docs/)
- Open an issue if you encounter migration problems

### Version Compatibility

| Version | Structure | Status |
|---------|-----------|--------|
| 0.1-SNAPSHOT | Monolithic | Deprecated |
| 0.2.0-SNAPSHOT | Multi-module | Current |

**Note:** Version 0.1-SNAPSHOT will not receive further updates. Please migrate to 0.2.0-SNAPSHOT.
