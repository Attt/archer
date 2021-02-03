package com.github.attt.archer.invoker;


import java.util.List;
import java.util.Map;

/**
 * Cache processor
 *
 * @param <CONTEXT>     processing context
 * @param <PROPERTIES>   cache properties
 * @param <E_PROPERTIES> eviction properties
 * @param <V>           value type
 * @author atpexgo
 * @since 1.0
 */
public interface Invoker<CONTEXT, PROPERTIES, E_PROPERTIES, V> {

    /**
     * Get {@link V} using {@link PROPERTIES} with {@link CONTEXT}
     *
     * @param context
     * @param properties
     * @return
     */
    V get(CONTEXT context, PROPERTIES properties);

    /**
     * Get {@link V} mapping to {@link CONTEXT} using {@link PROPERTIES} with {@link CONTEXT} list
     *
     * @param contextList
     * @param properties
     * @return
     */
    Map<CONTEXT, V> getAll(List<CONTEXT> contextList, PROPERTIES properties);

    /**
     * Put {@link V} using {@link PROPERTIES} with {@link CONTEXT}
     *
     * @param context
     * @param value
     * @param properties
     */
    void put(CONTEXT context, V value, PROPERTIES properties);

    /**
     * Put {@link V} mapping to {@link CONTEXT} using {@link PROPERTIES}
     *
     * @param contextValueMap
     * @param properties
     */
    void putAll(Map<CONTEXT, V> contextValueMap, PROPERTIES properties);

    /**
     * Delete {@link V} using {@link E_PROPERTIES} with {@link CONTEXT}
     *
     * @param context
     * @param evictionProperties
     */
    void delete(CONTEXT context, E_PROPERTIES evictionProperties);

    /**
     * Delete all {@link V} using {@link E_PROPERTIES} with {@link CONTEXT} list
     *
     * @param contextList
     * @param evictionProperties
     */
    void deleteAll(List<CONTEXT> contextList, E_PROPERTIES evictionProperties);

    /**
     * Delete all using {@link PROPERTIES}
     *
     * @param evictionProperties
     */
    void deleteAll(E_PROPERTIES evictionProperties);
}
