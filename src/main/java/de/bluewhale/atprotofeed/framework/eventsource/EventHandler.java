package de.bluewhale.atprotofeed.framework.eventsource;

import de.bluewhale.atprotofeed.framework.exception.EventProcessingException;
import de.bluewhale.atprotofeed.framework.feed.RepositoryEvent;

/**
 * Handler for ATProto repository events.
 * 
 * <p>Implementations process incoming events (filter, index, etc.). The framework
 * provides {@link EventProcessor} as the primary handler implementation.
 */
@FunctionalInterface
public interface EventHandler {
    
    /**
     * Handle an ATProto repository event.
     * 
     * <p>Exceptions thrown by this method trigger the retry strategy:
     * <ol>
     *   <li>1st retry after 1 second
     *   <li>2nd retry after 2 seconds
     *   <li>3rd retry after 4 seconds
     *   <li>Dead-letter queue after 3 failures
     * </ol>
     * 
     * @param event the repository event
     * @throws EventProcessingException if event processing fails (triggers retry)
     */
    void handle(RepositoryEvent event) throws EventProcessingException;
}
