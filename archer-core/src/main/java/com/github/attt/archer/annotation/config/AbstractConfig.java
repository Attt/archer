package com.github.attt.archer.annotation.config;

import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;
import com.github.attt.archer.roots.Component;
import com.github.attt.archer.stats.api.CacheEventCollector;

import static com.github.attt.archer.constants.Constants.DEFAULT_DELIMITER;

/**
 * Abstract cache operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public abstract class AbstractConfig<M extends AbstractCacheMetadata> implements Component {

    private String cacheType;

    protected M metadata;

    protected CacheEventCollector cacheEventCollector;

    public M getMetadata() {
        return metadata;
    }

    public void setMetadata(M metadata) {
        this.metadata = metadata;
    }

    public CacheEventCollector getCacheEventCollector() {
        return cacheEventCollector;
    }

    public void setCacheEventCollector(CacheEventCollector cacheEventCollector) {
        this.cacheEventCollector = cacheEventCollector;
    }

    public String getCacheType() {
        if (cacheType == null) {
            String methodSignature = getMetadata().getMethodSignature();
            String condition = getMetadata().getCondition();
            cacheType = methodSignature + " " + DEFAULT_DELIMITER
                    + condition + " ";
        }
        return cacheType;
    }
}
