package de.bluewhale.atprotofeed.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Feed Server - Example application demonstrating ATProto Feed Framework usage.
 * 
 * <p>This application shows how to:
 * <ul>
 *   <li>Add the framework as a Maven dependency
 *   <li>Implement a custom {@link de.bluewhale.atprotofeed.api.feed.FeedProvider}
 *   <li>Configure feed settings via application.yml
 *   <li>Deploy as a standalone Spring Boot application
 * </ul>
 */
@SpringBootApplication
public class SampleFeedApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SampleFeedApplication.class, args);
    }
}
