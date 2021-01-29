package com.github.attt.archer.annotation.metadata;

import com.github.attt.archer.annotation.metadata.AbstractCacheMetadata;

/**
 * Cache metadata
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheMetadata extends AbstractCacheMetadata {

    protected Long expirationInMillis;

    protected boolean breakdownProtect;

    protected Long breakdownProtectTimeoutInMillis;

    protected boolean invokeAnyway;

    public Long getExpirationInMillis() {
        return expirationInMillis;
    }

    public void setExpirationInMillis(Long expirationInMillis) {
        this.expirationInMillis = expirationInMillis;
    }

    public boolean getBreakdownProtect() {
        return breakdownProtect;
    }

    public void setBreakdownProtect(boolean breakdownProtect) {
        this.breakdownProtect = breakdownProtect;
    }

    public Long getBreakdownProtectTimeoutInMillis() {
        return breakdownProtectTimeoutInMillis;
    }

    public void setBreakdownProtectTimeoutInMillis(Long breakdownProtectTimeoutInMillis) {
        this.breakdownProtectTimeoutInMillis = breakdownProtectTimeoutInMillis;
    }

    public boolean getInvokeAnyway() {
        return invokeAnyway;
    }

    public void setInvokeAnyway(boolean invokeAnyway) {
        this.invokeAnyway = invokeAnyway;
    }

}
