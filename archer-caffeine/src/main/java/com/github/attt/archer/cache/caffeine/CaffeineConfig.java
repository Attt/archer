package com.github.attt.archer.cache.caffeine;

import com.github.attt.archer.cache.CacheConfig;

/**
 * Caffeine config
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class CaffeineConfig implements CacheConfig {

    private int initialCapacity = 1;

    private int maximumSize = Integer.MAX_VALUE;

    private int expireAfterWriteInSecond = 10;

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public void setInitialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    public int getExpireAfterWriteInSecond() {
        return expireAfterWriteInSecond;
    }

    public void setExpireAfterWriteInSecond(int expireAfterWriteInSecond) {
        this.expireAfterWriteInSecond = expireAfterWriteInSecond;
    }

    @Override
    public String toString() {
        return "CaffeineConfig{" +
                "initialCapacity=" + initialCapacity +
                ", maximumSize=" + maximumSize +
                ", expireAfterWriteInSecond=" + expireAfterWriteInSecond +
                '}';
    }
}
