package com.github.attt.archer.processor.api;

/**
 * Provide manual cache process
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface ManualProcessor {

    /**
     * Remove cache with certain area and key
     *
     * @param area
     * @param key
     */
    void delete(String area, String key);

    /**
     * Remove all caches with certain area
     *
     * @param area
     */
    void deleteAll(String area);

    /**
     * If cache with specified key exists in specified area
     *
     * @param area
     * @param key
     * @return true if exist
     */
    boolean exist(String area, String key);
}
