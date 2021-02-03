package com.github.attt.archer.test.components;

import com.github.attt.archer.metrics.api.CacheEvent;
import com.github.attt.archer.metrics.api.listener.CacheMetricsListener;
import com.github.attt.archer.metrics.event.CacheTimeElapsingEvent;

import java.util.function.Predicate;

/**
 * @author: atpex
 * @since 1.0
 */
public class AllCacheEventListener implements CacheMetricsListener<CacheEvent> {

    @Override
    public void onEvent(String name, CacheEvent event) {
        if (event instanceof CacheTimeElapsingEvent) {
            System.out.println(name + " : " + event.getClass().getSimpleName() + " : " + ((CacheTimeElapsingEvent) event).elapsing() + "ns");
        } else {
            System.out.println(name + " : " + event.getClass().getSimpleName());
        }
    }

    @Override
    public Predicate<CacheEvent> filter() {
        return (e) -> true;
    }

    @Override
    public int compareTo(CacheMetricsListener<CacheEvent> o) {
        return 0;
    }
}
