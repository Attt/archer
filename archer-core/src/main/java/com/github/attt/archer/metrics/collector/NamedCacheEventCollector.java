package com.github.attt.archer.metrics.collector;

import com.github.attt.archer.metrics.api.CacheEvent;
import com.github.attt.archer.metrics.api.CacheEventCollector;
import com.github.attt.archer.metrics.api.listener.CacheMetricsListener;

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

    private final List<CacheMetricsListener<CacheEvent>> listeners = new ArrayList<>();

    public NamedCacheEventCollector(String name) {
        this.name = name;
    }


    @Override
    public void register(CacheMetricsListener<CacheEvent> listener) {
        listeners.add(listener);
    }

    @Override
    public void collect(CacheEvent cacheEvent) {
        for (CacheMetricsListener<CacheEvent> listener : listeners) {
            if (listener.filter().test(cacheEvent)) {
                listener.onEvent(name, cacheEvent);
            }
        }
    }
}
