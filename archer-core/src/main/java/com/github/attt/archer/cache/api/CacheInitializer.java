package com.github.attt.archer.cache.api;

import com.github.attt.archer.roots.Component;

/**
 * Cache operation initialization processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheInitializer extends Component {

    /**
     * 根据缓存（分片）配置 {@link CacheShard}初始化缓存 {@link Cache}
     *
     * @param shard 缓存（分片）配置
     * @return 初始化完成后的缓存实例
     * @throws Throwable 异常
     */
    Cache initial(CacheShard shard) throws Throwable;

    /**
     * 是否启用本初始化工具
     *
     * @return 是否启用
     */
    default boolean enabled() {
        return true;
    }

    /**
     * 初始化优先级，只执行优先级最高的初始化工具
     *
     * @return 小值优先级（范围：{@link Integer#MIN_VALUE} 到 {@link Integer#MAX_VALUE} ）
     */
    default int order() {
        return -1;
    }
}
