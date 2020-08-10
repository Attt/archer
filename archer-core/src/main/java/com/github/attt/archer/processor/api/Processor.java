package com.github.attt.archer.processor.api;


import java.util.List;
import java.util.Map;

/**
 * Cache processor
 *
 * @param <V> value type
 * @author atpexgo
 * @since 1.0
 */
public interface Processor<CONTEXT, OPERATION, V> {

    V get(CONTEXT context, OPERATION cacheOperation);

    Map<CONTEXT, V> getAll(List<CONTEXT> contextList, OPERATION cacheOperation);

    void put(CONTEXT context, V value, OPERATION cacheOperation);

    void putAll(Map<CONTEXT, V> contextValueMap, OPERATION cacheOperation);

    void delete(CONTEXT context, OPERATION cacheOperation);

    void deleteAll(List<CONTEXT> contextList, OPERATION cacheOperation);

    void deleteAll(OPERATION cacheOperation);
}
