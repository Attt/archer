package com.github.attt.archer.annotation.config;

import com.github.attt.archer.loader.MultipleLoader;
import com.github.attt.archer.loader.SingleLoader;
import com.github.attt.archer.annotation.metadata.CacheMetadata;

/**
 * Cache acceptation operation
 *
 * @param <M> metadata type
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see CacheMetadata
 * @since 1.0
 */
public abstract class AbstractCacheConfig<M extends CacheMetadata, V> extends AbstractConfig<M> {

    protected SingleLoader<V> loader;

    protected MultipleLoader<V> multipleLoader;

    public SingleLoader<V> getLoader() {
        return loader;
    }

    public void setLoader(SingleLoader<V> loader) {
        this.loader = loader;
    }

    public MultipleLoader<V> getMultipleLoader() {
        return multipleLoader;
    }

    public void setMultipleLoader(MultipleLoader<V> multipleLoader) {
        this.multipleLoader = multipleLoader;
    }

}
