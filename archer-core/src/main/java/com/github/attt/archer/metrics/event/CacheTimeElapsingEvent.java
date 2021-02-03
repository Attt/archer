package com.github.attt.archer.metrics.event;

import com.github.attt.archer.metrics.api.CacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache time elapsing event
 * <p>
 * Produced to count cache accessing time elapsing
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheTimeElapsingEvent implements CacheEvent {

    private static final Logger log = LoggerFactory.getLogger(CacheTimeElapsingEvent.class);

    private final long startNanoTime;

    private long endNanoTime;

    public CacheTimeElapsingEvent() {
        this.startNanoTime = System.nanoTime();
    }

    public void done() {
        this.endNanoTime = System.nanoTime();
    }

    public long elapsing() {
        if (this.endNanoTime <= 0) {
            log.warn("failed to count cache time elapsing.");
            return 0L;
        }
        return this.endNanoTime - this.startNanoTime;
    }
}
