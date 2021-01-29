package com.github.attt.archer.stats.event;

import com.github.attt.archer.cache.Cache;
import com.github.attt.archer.stats.api.CacheEvent;
import com.github.attt.archer.stats.api.CacheEventCollector;

import java.util.Collection;

/**
 * Cache actively evict event
 * <p>
 * Produced when {@link Cache#remove(String, String, CacheEventCollector)}
 * or {@link Cache#removeAll(String, CacheEventCollector)}
 * or {@link Cache#removeAll(String, Collection, CacheEventCollector)} is invoked
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheActivelyEvictEvent implements CacheEvent {
}
