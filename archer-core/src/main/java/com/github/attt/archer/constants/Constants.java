package com.github.attt.archer.constants;

/**
 * Constants
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class Constants {

    /**
     * 默认的region
     */
    public static final String DEFAULT_REGION = "r0";

    /**
     * 默认存放同一region所有缓存key集合的缓存key后缀
     */
    public static final String DEFAULT_KEYS_SUFFIX = "~keys";

    /**
     * 默认分隔符
     */
    public static final String DEFAULT_DELIMITER = ":";

    public static final String OBJECT_LOAD_METHOD_ARGS_CACHE_PREFIX = "$args$" + DEFAULT_DELIMITER;

    public static final String CACHE_IS_LOADED_PREFIX = "$is_loaded$" + DEFAULT_DELIMITER;

    /**
     * 表示缓存中的值已加载但实际为null的情况，用于区分缓存未命中，防止发生缓存穿透
     */
    public static final byte[] NONE_VALUE = new byte[]{1};

    /**
     * 表示列表缓存为空列表
     */
    public static final byte[] BLANK_VALUE = new byte[]{2};

}
