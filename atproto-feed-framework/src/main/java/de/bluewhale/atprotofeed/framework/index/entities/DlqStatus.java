package de.bluewhale.atprotofeed.framework.index.entities;

/**
 * Lifecycle status of a dead-letter event.
 */
public enum DlqStatus {
    /** Awaiting manual review */
    PENDING_REVIEW,
    
    /** Currently being retried */
    IN_RETRY,
    
    /** Successfully reprocessed or manually resolved */
    RESOLVED,
    
    /** Permanently discarded (not fixable) */
    DISCARDED
}
