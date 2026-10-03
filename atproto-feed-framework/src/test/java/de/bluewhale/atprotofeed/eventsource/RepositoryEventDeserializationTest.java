package de.bluewhale.atprotofeed.eventsource;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.atprotofeed.api.feed.CommitDetails;
import de.bluewhale.atprotofeed.api.feed.RepositoryEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for RepositoryEvent deserialization.
 * 
 * <p>Task: T041 - Unit test for RepositoryEvent deserialization
 */
public class RepositoryEventDeserializationTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void deserialize_shouldParseCommitEvent() throws Exception {
        String json = """
            {
              "did": "did:plc:test123",
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
            """;
        
        RepositoryEvent event = objectMapper.readValue(json, RepositoryEvent.class);
        
        assertThat(event).isNotNull();
        assertThat(event.did()).isEqualTo("did:plc:test123");
        assertThat(event.timeUs()).isEqualTo(1705334400000000L);
        assertThat(event.kind()).isEqualTo("commit");
        assertThat(event.commit()).isNotNull();
        assertThat(event.commit().operation()).isEqualTo("create");
        assertThat(event.commit().collection()).isEqualTo("app.bsky.feed.post");
    }
    
    @Test
    void deserialize_shouldHandleIdentityEvent() throws Exception {
        String json = """
            {
              "did": "did:plc:test123",
              "time_us": 1705334400000000,
              "kind": "identity"
            }
            """;
        
        RepositoryEvent event = objectMapper.readValue(json, RepositoryEvent.class);
        
        assertThat(event).isNotNull();
        assertThat(event.kind()).isEqualTo("identity");
        assertThat(event.commit()).isNull();
    }
    
    @Test
    void deserialize_shouldHandleUnknownFields() throws Exception {
        String json = """
            {
              "did": "did:plc:test123",
              "time_us": 1705334400000000,
              "kind": "commit",
              "unknown_field": "should be ignored",
              "commit": {
                "operation": "create",
                "collection": "app.bsky.feed.post",
                "rkey": "abc123",
                "cid": "bafyreihzvp3..."
              }
            }
            """;
        
        RepositoryEvent event = objectMapper.readValue(json, RepositoryEvent.class);
        
        assertThat(event).isNotNull();
        assertThat(event.did()).isEqualTo("did:plc:test123");
    }
    
    @Test
    void isPostCreation_shouldReturnTrueForCreatePost() {
        RepositoryEvent event = RepositoryEvent.builder()
            .did("did:plc:test")
            .timeUs(1705334400000000L)
            .kind("commit")
            .commit(CommitDetails.builder()
                .operation("create")
                .collection("app.bsky.feed.post")
                .rkey("abc123")
                .cid("bafyreihzvp3...")
                .build())
            .build();
        
        assertThat(event.isPostCreation()).isTrue();
    }
    
    @Test
    void isPostCreation_shouldReturnFalseForNonPost() {
        RepositoryEvent event = RepositoryEvent.builder()
            .did("did:plc:test")
            .timeUs(1705334400000000L)
            .kind("commit")
            .commit(CommitDetails.builder()
                .operation("create")
                .collection("app.bsky.actor.profile")  // Not a post
                .rkey("abc123")
                .cid("bafyreihzvp3...")
                .build())
            .build();
        
        assertThat(event.isPostCreation()).isFalse();
    }
    
    @Test
    void isPostCreation_shouldReturnFalseForDeleteOperation() {
        RepositoryEvent event = RepositoryEvent.builder()
            .did("did:plc:test")
            .timeUs(1705334400000000L)
            .kind("commit")
            .commit(CommitDetails.builder()
                .operation("delete")  // Not create
                .collection("app.bsky.feed.post")
                .rkey("abc123")
                .cid("bafyreihzvp3...")
                .build())
            .build();
        
        assertThat(event.isPostCreation()).isFalse();
    }
    
    @Test
    void postUri_shouldConstructCorrectUri() {
        RepositoryEvent event = RepositoryEvent.builder()
            .did("did:plc:test123")
            .timeUs(1705334400000000L)
            .kind("commit")
            .commit(CommitDetails.builder()
                .operation("create")
                .collection("app.bsky.feed.post")
                .rkey("3jui7kd27a52s")
                .cid("bafyreihzvp3...")
                .build())
            .build();
        
        String uri = event.postUri();
        
        assertThat(uri).isEqualTo("at://did:plc:test123/app.bsky.feed.post/3jui7kd27a52s");
    }
    
    @Test
    void timestamp_shouldConvertMicrosecondsToInstant() {
        RepositoryEvent event = RepositoryEvent.builder()
            .did("did:plc:test")
            .timeUs(1705334400000000L)  // 2024-01-15T12:00:00Z
            .kind("commit")
            .build();
        
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.timestamp().toEpochMilli()).isEqualTo(1705334400000L);
    }
}
