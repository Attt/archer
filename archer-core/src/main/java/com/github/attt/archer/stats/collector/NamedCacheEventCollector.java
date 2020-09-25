package com.github.attt.archer.stats.collector;

import com.github.attt.archer.stats.api.CacheEvent;
import com.github.attt.archer.stats.api.CacheEventCollector;
import com.github.attt.archer.stats.api.listener.CacheStatsListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Named cache event collector
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class NamedCacheEventCollector implements CacheEventCollector {

    private final String name;

    private final List<CacheStatsListener<CacheEvent>> listeners = new ArrayList<>();

    public NamedCacheEventCollector(String name) {
        this.name = name;
    }


    @Override
    public void register(CacheStatsListener<CacheEvent> listener) {
        listeners.add(listener);
    }

    @Override
    public void collect(CacheEvent cacheEvent) {
        for (CacheStatsListener<CacheEvent> listener : listeners) {
            if (listener.filter().test(cacheEvent)) {
                listener.onEvent(name, cacheEvent);
            }
        }
    }
}
