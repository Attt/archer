package com.github.attt.archer.metrics.api.listener;

import com.github.attt.archer.metrics.api.CacheEvent;

import java.util.EventListener;
import java.util.function.Predicate;

/**
 * Cache metrics listener
 *
 * @param <E> cache event
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheMetricsListener<E extends CacheEvent> extends Comparable<CacheMetricsListener<E>>, EventListener {

    /**
     * Resolve {@link CacheEvent}
     *
     * @param name
     * @param event
     */
    void onEvent(String name, E event);

    /**
     * Filter {@link CacheEvent}
     *
     * @return true if event is acceptable
     */
    Predicate<E> filter();
}
