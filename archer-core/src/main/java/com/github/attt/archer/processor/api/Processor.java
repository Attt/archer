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

    /**
     * Get {@link V} using {@link OPERATION} with {@link CONTEXT}
     *
     * @param context
     * @param cacheOperation
     * @return
     */
    V get(CONTEXT context, OPERATION cacheOperation);

    /**
     * Get {@link V} mapping to {@link CONTEXT} using {@link OPERATION} with {@link CONTEXT} list
     *
     * @param contextList
     * @param cacheOperation
     * @return
     */
    Map<CONTEXT, V> getAll(List<CONTEXT> contextList, OPERATION cacheOperation);

    /**
     * Put {@link V} using {@link OPERATION} with {@link CONTEXT}
     *
     * @param context
     * @param value
     * @param cacheOperation
     */
    void put(CONTEXT context, V value, OPERATION cacheOperation);

    /**
     * Put {@link V} mapping to {@link CONTEXT} using {@link OPERATION}
     *
     * @param contextValueMap
     * @param cacheOperation
     */
    void putAll(Map<CONTEXT, V> contextValueMap, OPERATION cacheOperation);

    /**
     * Delete {@link V} using {@link OPERATION} with {@link CONTEXT}
     *
     * @param context
     * @param cacheOperation
     */
    void delete(CONTEXT context, OPERATION cacheOperation);

    /**
     * Delete all {@link V} using {@link OPERATION} with {@link CONTEXT} list
     *
     * @param contextList
     * @param cacheOperation
     */
    void deleteAll(List<CONTEXT> contextList, OPERATION cacheOperation);

    /**
     * Delete all using {@link OPERATION}
     *
     * @param cacheOperation
     */
    void deleteAll(OPERATION cacheOperation);
}
