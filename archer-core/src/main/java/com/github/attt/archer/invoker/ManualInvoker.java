package com.github.attt.archer.invoker;

/**
 * Provide manual cache process
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface ManualInvoker {

    /**
     * Remove cache with certain region and key
     *
     * @param region
     * @param key
     */
    void delete(String region, String key);

    /**
     * Remove all caches with certain region
     *
     * @param region
     */
    void deleteAll(String region);

    /**
     * If cache with specified key exists in specified region
     *
     * @param region
     * @param key
     * @return true if exist
     */
    boolean exist(String region, String key);
}
