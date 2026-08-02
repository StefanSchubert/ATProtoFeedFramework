package de.bluewhale.atprotofeed.framework.resilience;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Retry strategy with configurable backoff for failed operations.
 * 
 * Executes an operation up to maxAttempts times, waiting between attempts
 * according to the backoff intervals.
 */
@Slf4j
public class RetryStrategy {
    
    private final int maxAttempts;
    private final List<Duration> backoffIntervals;
    
    public RetryStrategy(int maxAttempts, List<Duration> backoffIntervals) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (backoffIntervals == null || backoffIntervals.isEmpty()) {
            throw new IllegalArgumentException("backoffIntervals cannot be empty");
        }
        this.maxAttempts = maxAttempts;
        this.backoffIntervals = backoffIntervals;
    }
    
    /**
     * Execute operation with retry logic.
     * 
     * @param operation Operation to execute
     * @param operationName Name for logging
     * @param <T> Return type
     * @return Result of successful operation
     * @throws Exception Last exception if all retries exhausted
     */
    public <T> T execute(Supplier<T> operation, String operationName) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Executing {} (attempt {}/{})", operationName, attempt, maxAttempts);
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for {}: {}", 
                    attempt, maxAttempts, operationName, e.getMessage());
                
                if (attempt < maxAttempts) {
                    Duration delay = getBackoffDelay(attempt - 1);
                    log.debug("Waiting {}ms before retry", delay.toMillis());
                    Thread.sleep(delay.toMillis());
                }
            }
        }
        
        log.error("All {} attempts exhausted for {}", maxAttempts, operationName);
        throw lastException;
    }
    
    /**
     * Execute void operation with retry logic.
     */
    public void executeVoid(Runnable operation, String operationName) throws Exception {
        execute(() -> {
            operation.run();
            return null;
        }, operationName);
    }
    
    private Duration getBackoffDelay(int attemptIndex) {
        if (attemptIndex < backoffIntervals.size()) {
            return backoffIntervals.get(attemptIndex);
        }
        // Use last interval for attempts beyond configured list
        return backoffIntervals.get(backoffIntervals.size() - 1);
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
