package com.github.attt.archer.metrics.api;

import com.github.attt.archer.metrics.api.listener.CacheMetricsListener;


/**
 * Cache event collector
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheEventCollector {

    /**
     * Register {@link CacheMetricsListener} listeners to this collector
     *
     * @param listener
     */
    void register(CacheMetricsListener<CacheEvent> listener);

    /**
     * Collect {@link CacheEvent}
     *
     * @param cacheEvent
     */
    void collect(CacheEvent cacheEvent);
}
