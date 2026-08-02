package de.bluewhale.atprotofeed.sample.provider;

import de.bluewhale.atprotofeed.api.feed.FeedContext;
import de.bluewhale.atprotofeed.api.feed.FeedProvider;
import de.bluewhale.atprotofeed.api.feed.PostReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Example FeedProvider implementation demonstrating feed customization.
 * 
 * <p>This feed indexes posts that:
 * <ul>
 *   <li>Are in German language (de)
 *   <li>Contain technology-related keywords
 * </ul>
 * 
 * <p>Posts are ranked in reverse chronological order.
 */
@Component
@Slf4j
public class GermanTechFeedProvider implements FeedProvider {
    
    private static final String[] TECH_KEYWORDS = {
        "java", "spring", "kotlin", "software", "programmierung",
        "entwicklung", "code", "framework", "api", "database",
        "cloud", "docker", "kubernetes", "tech", "technologie"
    };
    
    @Override
    public String getFeedId() {
        // TODO: Replace with your actual DID after feed registration
        return "at://did:plc:example/app.bsky.feed.generator/german-tech";
    }
    
    @Override
    public boolean shouldIndex(PostReference post) {
        Map<String, Object> metadata = post.metadata();
        if (metadata == null) {
            return false;
        }
        
        // Check language
        String language = (String) metadata.get("language");
        if (!"de".equals(language) && !"de-DE".equals(language)) {
            return false;
        }
        
        // Check for tech keywords
        String text = (String) metadata.get("text");
        if (text == null || text.isBlank()) {
            return false;
        }
        
        boolean hasTechKeyword = containsTechKeywords(text);
        if (hasTechKeyword) {
            log.debug("Indexing German tech post: {}", post.postUri());
        }
        
        return hasTechKeyword;
    }
    
    @Override
    public List<PostReference> selectPosts(FeedContext context) {
        // Simple reverse chronological ordering
        // For production, consider more sophisticated ranking algorithms
        return context.candidatePosts().stream()
            .sorted(Comparator.comparing(PostReference::postCreatedAt).reversed())
            .limit(context.limit())
            .toList();
    }
    
    @Override
    public PostReference enrichMetadata(PostReference post) {
        // Example: Extract and store additional metadata
        Map<String, Object> metadata = post.metadata();
        if (metadata == null) {
            return post;
        }
        
        // Calculate a simple relevance score based on keyword matches
        String text = (String) metadata.get("text");
        if (text != null) {
            int score = calculateRelevanceScore(text);
            metadata.put("relevance_score", score);
        }
        
        return post.withMetadata(metadata);
    }
    
    /**
     * Check if text contains any technology keywords.
     */
    private boolean containsTechKeywords(String text) {
        String lowerText = text.toLowerCase();
        for (String keyword : TECH_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calculate relevance score based on keyword frequency.
     */
    private int calculateRelevanceScore(String text) {
        String lowerText = text.toLowerCase();
        int score = 0;
        for (String keyword : TECH_KEYWORDS) {
            // Count occurrences of each keyword
            int index = 0;
            while ((index = lowerText.indexOf(keyword, index)) != -1) {
                score++;
                index += keyword.length();
            }
        }
        return score;
    }
}
