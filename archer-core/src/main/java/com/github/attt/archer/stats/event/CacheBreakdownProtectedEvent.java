package com.github.attt.archer.stats.event;

import com.github.attt.archer.cache.annotation.Protection;
import com.github.attt.archer.metadata.CacheMetadata;
import com.github.attt.archer.processor.api.AbstractProcessor;
import com.github.attt.archer.stats.api.CacheEvent;
import com.github.attt.archer.stats.api.CacheEventCollector;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Cache breakdown protected event
 * <p>
 * Produced when any request that could causing cache breakdown,</br>
 * if breakdown protect option (such as {@link Protection#breakdownProtect()}) is set to true
 *
 * @author atpexgo.wu
 * @see CacheMetadata#breakdownProtect
 * @see AbstractProcessor#doSynchronizedLoadAndPut(String, long, Supplier, Consumer, Supplier, CacheEventCollector)
 * @since 1.0
 */
public class CacheBreakdownProtectedEvent implements CacheEvent {
}
