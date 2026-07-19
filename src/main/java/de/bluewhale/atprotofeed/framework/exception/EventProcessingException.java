package de.bluewhale.atprotofeed.framework.exception;

/**
 * Exception thrown during event processing operations.
 * 
 * Indicates failures in parsing, validation, or processing repository events.
 */
public class EventProcessingException extends RuntimeException {
    
    public EventProcessingException(String message) {
        super(message);
    }
    
    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
