package de.bluewhale.atprotofeed.framework.exception;

/**
 * Exception thrown during post indexing operations.
 * 
 * Indicates failures in storing, retrieving, or managing indexed posts.
 */
public class IndexingException extends RuntimeException {
    
    public IndexingException(String message) {
        super(message);
    }
    
    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
