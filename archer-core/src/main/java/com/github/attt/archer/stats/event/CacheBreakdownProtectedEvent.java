package com.github.attt.archer.stats.event;

import com.github.attt.archer.annotation.Protection;
import com.github.attt.archer.annotation.metadata.CacheMetadata;
import com.github.attt.archer.invoker.AbstractInvoker;
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
 * @see AbstractInvoker#doSynchronizedLoadAndPut(String, long, Supplier, Consumer, Supplier, CacheEventCollector)
 * @since 1.0
 */
public class CacheBreakdownProtectedEvent implements CacheEvent {
}
