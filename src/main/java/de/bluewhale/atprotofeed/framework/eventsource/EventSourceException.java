package de.bluewhale.atprotofeed.framework.eventsource;

/**
 * Exception thrown when event source operations fail.
 */
public class EventSourceException extends RuntimeException {
    
    public EventSourceException(String message) {
        super(message);
    }
    
    public EventSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
