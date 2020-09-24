package com.github.attt.archer.stats.api;

import com.github.attt.archer.stats.api.listener.CacheStatsListener;


/**
 * Cache event collector
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheEventCollector {

    /**
     * Register {@link CacheStatsListener} listeners to this collector
     *
     * @param listener
     */
    void register(CacheStatsListener<CacheEvent> listener);

    /**
     * Collect {@link CacheEvent}
     *
     * @param cacheEvent
     */
    void collect(CacheEvent cacheEvent);
}
