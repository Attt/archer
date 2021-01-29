package com.github.attt.archer.cache;

import com.github.attt.archer.roots.Component;

/**
 * Cache operation initialization processor
 *
 * 通过实现这个接口来定义初始化缓存的行为
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheFactory extends Component {

    /**
     * 根据缓存（分片）配置 {@link CacheConfig}初始化缓存 {@link Cache}
     *
     * @param config 缓存（分片）配置
     * @return 初始化完成后的缓存实例
     * @throws Throwable 异常
     */
    Cache initial(CacheConfig config) throws Throwable;

    /**
     * 是否启用本初始化工具
     *
     * @return 是否启用
     */
    default boolean enabled() {
        return true;
    }
}
