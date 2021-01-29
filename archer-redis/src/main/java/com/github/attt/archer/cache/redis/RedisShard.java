package com.github.attt.archer.cache.redis;

import com.github.attt.archer.cache.CacheConfig;

/**
 * Redis shard
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class RedisShard implements CacheConfig {

    private boolean ssl = false;

    private int database = 0;

    private String host = "localhost";

    private int port = 6379;

    private String password = "";

    private boolean lifo = Constant.DEFAULT_LIFO;

    private boolean fairness = Constant.DEFAULT_FAIRNESS;

    private long readTimeout = Constant.DEFAULT_TIMEOUT;

    private long connectTimeout = Constant.DEFAULT_TIMEOUT;

    private int maxTotal = Constant.DEFAULT_MAX_TOTAL;

    private int maxIdle = Constant.DEFAULT_MAX_IDLE;

    private int minIdle = Constant.DEFAULT_MIN_IDLE;

    private long maxWaitMillis = Constant.DEFAULT_MAX_WAIT_MILLIS;

    private long minEvictableIdleTimeMillis = Constant.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    private long softMinEvictableIdleTimeMillis = Constant.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    private int numTestsPerEvictionRun = Constant.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

    private boolean testOnCreate = Constant.DEFAULT_TEST_ON_CREATE;

    private boolean testOnBorrow = Constant.DEFAULT_TEST_ON_BORROW;

    private boolean testOnReturn = Constant.DEFAULT_TEST_ON_RETURN;

    private boolean testWhileIdle = Constant.DEFAULT_TEST_WHILE_IDLE;

    private long timeBetweenEvictionRunsMillis = Constant.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

    private boolean blockWhenExhausted = Constant.DEFAULT_BLOCK_WHEN_EXHAUSTED;

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public boolean isLifo() {
        return lifo;
    }

    public void setLifo(boolean lifo) {
        this.lifo = lifo;
    }

    public boolean isFairness() {
        return fairness;
    }

    public void setFairness(boolean fairness) {
        this.fairness = fairness;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public long getSoftMinEvictableIdleTimeMillis() {
        return softMinEvictableIdleTimeMillis;
    }

    public void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        this.softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
    }

    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    public boolean isTestOnCreate() {
        return testOnCreate;
    }

    public void setTestOnCreate(boolean testOnCreate) {
        this.testOnCreate = testOnCreate;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public boolean isBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    public void setBlockWhenExhausted(boolean blockWhenExhausted) {
        this.blockWhenExhausted = blockWhenExhausted;
    }

    @Override
    public String toString() {
        return "RedisShard{" +
                "ssl=" + ssl +
                ", database=" + database +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", password='" + password + '\'' +
                ", lifo=" + lifo +
                ", fairness=" + fairness +
                ", readTimeout=" + readTimeout +
                ", connectTimeout=" + connectTimeout +
                ", maxTotal=" + maxTotal +
                ", maxIdle=" + maxIdle +
                ", minIdle=" + minIdle +
                ", maxWaitMillis=" + maxWaitMillis +
                ", minEvictableIdleTimeMillis=" + minEvictableIdleTimeMillis +
                ", softMinEvictableIdleTimeMillis=" + softMinEvictableIdleTimeMillis +
                ", numTestsPerEvictionRun=" + numTestsPerEvictionRun +
                ", testOnCreate=" + testOnCreate +
                ", testOnBorrow=" + testOnBorrow +
                ", testOnReturn=" + testOnReturn +
                ", testWhileIdle=" + testWhileIdle +
                ", timeBetweenEvictionRunsMillis=" + timeBetweenEvictionRunsMillis +
                ", blockWhenExhausted=" + blockWhenExhausted +
                '}';
    }
}
