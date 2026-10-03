package de.bluewhale.atprotofeed.api.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

/**
 * Details of an ATProto commit operation within a repository event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record CommitDetails(
    String operation,   // "create", "update", "delete"
    String collection,  // e.g., "app.bsky.feed.post"
    String rkey,        // Record key
    String cid,         // Content identifier
    @JsonProperty("record") JsonNode record  // Post content
) {}
