package de.bluewhale.atprotofeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test application for framework integration tests.
 * 
 * This minimal Spring Boot application is used to verify that the framework
 * auto-configuration works correctly when added as a dependency.
 */
@SpringBootApplication
public class TestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
