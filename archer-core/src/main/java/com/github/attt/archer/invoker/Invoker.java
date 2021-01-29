package com.github.attt.archer.invoker;


import java.util.List;
import java.util.Map;

/**
 * Cache processor
 *
 * @param <CONTEXT>     processing context
 * @param <OPERATION>   cache operation
 * @param <E_OPERATION> evict operation
 * @param <V>           value type
 * @author atpexgo
 * @since 1.0
 */
public interface Invoker<CONTEXT, OPERATION, E_OPERATION, V> {

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
     * Delete {@link V} using {@link E_OPERATION} with {@link CONTEXT}
     *
     * @param context
     * @param evictCacheOperation
     */
    void delete(CONTEXT context, E_OPERATION evictCacheOperation);

    /**
     * Delete all {@link V} using {@link E_OPERATION} with {@link CONTEXT} list
     *
     * @param contextList
     * @param evictCacheOperation
     */
    void deleteAll(List<CONTEXT> contextList, E_OPERATION evictCacheOperation);

    /**
     * Delete all using {@link OPERATION}
     *
     * @param evictCacheOperation
     */
    void deleteAll(E_OPERATION evictCacheOperation);
}
