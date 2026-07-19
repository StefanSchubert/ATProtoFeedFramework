package de.bluewhale.atprotofeed.framework.resilience;

import java.time.Duration;

/**
 * Exponential backoff calculator for reconnection attempts.
 * 
 * Calculates delays as: initialDelay * (2 ^ attempt), capped at maxDelay.
 */
public class ExponentialBackoff {
    
    private final Duration initialDelay;
    private final Duration maxDelay;
    
    public ExponentialBackoff(Duration initialDelay, Duration maxDelay) {
        if (initialDelay.isNegative() || initialDelay.isZero()) {
            throw new IllegalArgumentException("initialDelay must be positive");
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be >= initialDelay");
        }
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }
    
    /**
     * Calculate backoff delay for given attempt number.
     * 
     * @param attemptNumber Attempt number (0-based)
     * @return Backoff duration
     */
    public Duration calculate(int attemptNumber) {
        if (attemptNumber < 0) {
            throw new IllegalArgumentException("attemptNumber must be >= 0");
        }
        
        if (attemptNumber == 0) {
            return initialDelay;
        }
        
        // Calculate: initialDelay * (2 ^ attemptNumber)
        long multiplier = 1L << Math.min(attemptNumber, 30); // Cap at 2^30 to prevent overflow
        long delayMillis = initialDelay.toMillis() * multiplier;
        
        // Cap at maxDelay
        if (delayMillis > maxDelay.toMillis()) {
            return maxDelay;
        }
        
        return Duration.ofMillis(delayMillis);
    }
    
    /**
     * Convenience factory for creating from seconds.
     */
    public static ExponentialBackoff ofSeconds(int initialSeconds, int maxSeconds) {
        return new ExponentialBackoff(
            Duration.ofSeconds(initialSeconds),
            Duration.ofSeconds(maxSeconds)
        );
    }
}
