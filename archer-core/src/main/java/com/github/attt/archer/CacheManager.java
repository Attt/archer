package com.github.attt.archer;

import com.github.attt.archer.cache.ShardingCache;
import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.api.Serializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.expression.CacheExpressionUtilObject;
import com.github.attt.archer.annotation.metadata.CacheMetadata;
import com.github.attt.archer.annotation.config.AbstractCacheConfig;
import com.github.attt.archer.annotation.config.EvictionConfig;
import com.github.attt.archer.invoker.ListCacheInvoker;
import com.github.attt.archer.invoker.CacheInvoker;
import com.github.attt.archer.invoker.AbstractInvoker;
import com.github.attt.archer.roots.Component;
import com.github.attt.archer.roots.ListComponent;
import com.github.attt.archer.roots.ObjectComponent;
import com.github.attt.archer.stats.api.listener.CacheStatsListener;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.SpringElUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache manager
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings("all")
public class CacheManager implements Component {

    public static final String INTERNAL_CACHE_MANAGER_BEAN_NAME = "archar.cache.internalCacheManager";

    /**
     * cache config properties
     */
    public static class Config {
        public static boolean metricsEnabled = false;
        public static Serialization valueSerialization = Serialization.JAVA;
    }

    /**
     * Sharding cache operation
     */
    private ShardingCache shardingCache;

    /**
     * Manage all key generators
     */
    private Map<String, KeyGenerator> keyGeneratorMap = new ConcurrentHashMap<>();

    /**
     * Manage all serializers
     */
    private Map<String, Serializer> serializerMap = new ConcurrentHashMap<>();

    /**
     * Manage all cache acceptation operation sources
     */
    private Map<String, AbstractCacheConfig> cacheOperationMap = new ConcurrentHashMap<>();


    /**
     * Manage all cache eviction operation sources
     */
    private Map<String, EvictionConfig> evictionOperationMap = new ConcurrentHashMap<>();

    /**
     * Manage all method mapping to operation source name
     */
    private Map<String, List<String>> methodSignatureToOperationSourceName = new ConcurrentHashMap<>();

    /**
     * Manage all processors
     */
    private Map<String, AbstractInvoker> processors = new ConcurrentHashMap<>();

    /**
     * Manage all cache stats listeners
     */
    private Map<String, CacheStatsListener> statsListenerMap = new ConcurrentHashMap<>();


    public ShardingCache getShardingCache() {
        return shardingCache;
    }

    public void setShardingCache(ShardingCache shardingCache) {
        this.shardingCache = shardingCache;
    }

    public Map<String, AbstractCacheConfig> getCacheOperationMap() {
        return cacheOperationMap;
    }

    public void setCacheOperationMap(Map<String, AbstractCacheConfig> cacheOperationMap) {
        this.cacheOperationMap = cacheOperationMap;
    }

    public Map<String, EvictionConfig> getEvictionOperationMap() {
        return evictionOperationMap;
    }

    public void setEvictionOperationMap(Map<String, EvictionConfig> evictionOperationMap) {
        this.evictionOperationMap = evictionOperationMap;
    }

    public Map<String, List<String>> getMethodSignatureToOperationSourceName() {
        return methodSignatureToOperationSourceName;
    }

    public void setMethodSignatureToOperationSourceName(Map<String, List<String>> methodSignatureToOperationSourceName) {
        this.methodSignatureToOperationSourceName = methodSignatureToOperationSourceName;
    }


    public Map<String, KeyGenerator> getKeyGeneratorMap() {
        return keyGeneratorMap;
    }

    public void setKeyGeneratorMap(Map<String, KeyGenerator> keyGeneratorMap) {
        this.keyGeneratorMap = keyGeneratorMap;
    }

    public Map<String, Serializer> getSerializerMap() {
        return serializerMap;
    }

    public void setSerializerMap(Map<String, Serializer> serializerMap) {
        this.serializerMap = serializerMap;
    }

    public Map<String, CacheStatsListener> getStatsListenerMap() {
        return statsListenerMap;
    }

    public void setStatsListenerMap(Map<String, CacheStatsListener> statsListenerMap) {
        this.statsListenerMap = statsListenerMap;
    }

    /**
     * Get acceptation operations by method signature and source type
     *
     * @param methodSignature
     * @param sourceType
     * @param <T>
     * @return
     */
    public <T extends AbstractCacheConfig> List<T> getCacheOperations(String methodSignature, Class<T> sourceType) {
        List<String> configNames = methodSignatureToOperationSourceName.getOrDefault(methodSignature, null);

        List<T> sources = new ArrayList<>();
        if (!CommonUtils.isEmpty(configNames)) {
            for (String configName : configNames) {
                AbstractCacheConfig<CacheMetadata, Object> cacheOperation = cacheOperationMap.getOrDefault(configName, null);
                if (cacheOperation == null || !sourceType.isAssignableFrom(cacheOperation.getClass())) {
                    continue;
                }
                sources.add((T) cacheOperation);
            }

        }
        return sources;
    }

    /**
     * Get eviction operation sources by method signature
     *
     * @param methodSignature
     * @return
     */
    public List<EvictionConfig> getEvictionOperations(String methodSignature) {
        List<String> configNames = methodSignatureToOperationSourceName.getOrDefault(methodSignature, null);

        List<EvictionConfig> evictionConfigs = new ArrayList<>();
        if (!CommonUtils.isEmpty(configNames)) {
            for (String configName : configNames) {
                EvictionConfig evictionOperation = evictionOperationMap.getOrDefault(configName, null);
                if (evictionOperation == null) {
                    continue;
                }
                evictionConfigs.add(evictionOperation);
            }

        }
        return evictionConfigs;
    }

    /**
     * Get all processors
     *
     * @return
     */
    public Collection<AbstractInvoker> getProcessors(){
        return processors.values();
    }

    /**
     * Get cache processor which matches operation source
     *
     * @param cacheOperation
     * @return
     */
    public AbstractInvoker getProcessor(AbstractCacheConfig cacheOperation) {
        List<String> componentInterfaces = new ArrayList<>();
        Class<?>[] interfaces = cacheOperation.getClass().getInterfaces();
        for (Class<?> ifc : interfaces) {
            if (Component.class.isAssignableFrom(ifc)) {
                componentInterfaces.add(ifc.toString());
            }
        }
        for (String componentInterface : componentInterfaces) {
            if (processors.containsKey(componentInterface)) {
                return processors.get(componentInterface);
            }
        }


        throw new CacheOperationException("Illegal cache operation type " + cacheOperation.getClass().getName());
    }

    /**
     * Get cache processor which matches component type
     *
     * @param type
     * @param <C>
     * @return
     */
    public <C extends Component> AbstractInvoker getProcessor(Class<C> type) {
        return processors.getOrDefault(type.toString(), null);
    }

    @Override
    public void initialized() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("util", CacheExpressionUtilObject.class);
        SpringElUtil.init(vars);

        // note: add complex cache processor before object cache processor
        // because complex cache may has the same key name with object cache
        // but need deal with more data than object cache when evicting

        ListCacheInvoker<?> listServiceCacheProcessor = new ListCacheInvoker<>();
        listServiceCacheProcessor.afterInitialized(this);
        processors.put(ListComponent.class.toString(), listServiceCacheProcessor);

        CacheInvoker<?> objectServiceCacheProcessor = new CacheInvoker<>();
        objectServiceCacheProcessor.afterInitialized(this);
        processors.put(ObjectComponent.class.toString(), objectServiceCacheProcessor);
    }

    @Override
    public String initializedInfo() {
        return "\r\n" + "cache operations : " + cacheOperationMap.size() + "\r\n"
                + "eviction operations : " + evictionOperationMap.size() + "\r\n"
                + "processors : \r\n" + processors + "\r\n";
    }
}
