package de.bluewhale.atprotofeed.framework.feed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Immutable representation of an ATProto event from Jetstream.
 * 
 * Models repository events for post creation, update, and deletion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryEvent(
    String did,
    @JsonProperty("time_us") long timeUs,
    String kind,
    @JsonProperty("commit") CommitDetails commit,
    @JsonIgnore String rawPayload  // Original JSON for DLQ storage
) {
    
    public Instant timestamp() {
        return Instant.ofEpochMilli(timeUs / 1000);
    }
    
    public String postUri() {
        if (commit == null) return null;
        return String.format("at://%s/%s/%s", did, commit.collection(), commit.rkey());
    }
    
    public boolean isPostCreation() {
        return "commit".equals(kind) 
            && commit != null 
            && "create".equals(commit.operation())
            && "app.bsky.feed.post".equals(commit.collection());
    }
    
    public String eventId() {
        return commit != null ? commit.cid() : String.format("%s-%d", did, timeUs);
    }
}
