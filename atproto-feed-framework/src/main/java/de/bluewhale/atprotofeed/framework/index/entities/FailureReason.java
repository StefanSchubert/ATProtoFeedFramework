package de.bluewhale.atprotofeed.framework.index.entities;

/**
 * Categorizes why an event failed processing.
 */
public enum FailureReason {
    /** Error during event processing logic */
    PROCESSING_ERROR,
    
    /** Event data failed validation */
    VALIDATION_ERROR,
    
    /** Database operation failed */
    DB_ERROR,
    
    /** Operation timed out */
    TIMEOUT,
    
    /** Unknown or uncategorized failure */
    UNKNOWN
}
