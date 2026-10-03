package de.bluewhale.atprotofeed.api.feed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.Instant;

/**
 * Immutable representation of an ATProto event from Jetstream.
 * 
 * Models repository events for post creation, update, and deletion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record RepositoryEvent(
    String did,
    @JsonProperty("time_us") long timeUs,
    String kind,
    @JsonProperty("commit") CommitDetails commit,
    @JsonIgnore String rawPayload  // Original JSON for DLQ storage
) {
    
    /**
     * Get the event timestamp.
     * 
     * @return instant representation of timeUs
     */
    public Instant timestamp() {
        return Instant.ofEpochMilli(timeUs / 1000);
    }
    
    /**
     * Get the AT Protocol URI for this post.
     * 
     * @return post URI (at://{did}/{collection}/{rkey})
     */
    public String postUri() {
        if (commit == null) return null;
        return String.format("at://%s/%s/%s", did, commit.collection(), commit.rkey());
    }
    
    /**
     * Check if this is a post creation event.
     * 
     * @return true if this is a create operation for app.bsky.feed.post
     */
    public boolean isPostCreation() {
        return "commit".equals(kind) 
            && commit != null 
            && "create".equals(commit.operation())
            && "app.bsky.feed.post".equals(commit.collection());
    }
    
    /**
     * Get a unique identifier for this event.
     * 
     * @return event identifier
     */
    public String eventId() {
        return commit != null ? commit.cid() : String.format("%s-%d", did, timeUs);
    }
}
