
# ![logo](./doc/img/archer_150.png) Archer ![version](https://img.shields.io/badge/version-1.0-rc2-ff69b4) 

- [介绍](#介绍)
- [入门](#入门)
    - [依赖](#依赖)
    - [注解](#注解)
        - [对象缓存](#对象缓存)
        - [多对象缓存](#多对象缓存)
        - [列表缓存](#列表缓存)
        - [缓存淘汰](#缓存淘汰)
        - [全局属性](#全局属性)
    - [开始使用](#开始使用)
    - [使用redis或caffeine缓存实现](#使用redis或caffeine缓存实现)
    - [Spring](#Spring配置)
    - [SpringBoot](#SpringBoot配置)

# 介绍
archer是一个完全基于方法注解的缓存框架，解除缓存与业务代码的耦合。额外支持多对象缓存和列表缓存，从而提高缓存命中率：

- 开箱即用，支持最简化的配置
- 类上全局属性声明，减少方法注解的冗余配置
- 简明清晰的注解属性
- 基于map（默认）、redis、caffeine的缓存实现
- 基于Java（默认）、FastJson、Kryo、Hessian序列化方式
- 支持缓存穿透、击穿保护
- 组件高度可定制化
- JetBrains IDEA® 插件[archer-plugin](./plugin/archer-plugin.zip)，支持注解属性提示
- Spring及Spring Boot支持

依赖：

![jdk.version](https://img.shields.io/badge/Jdk-1.8%2B-ff69b4) 

可选：

![spring.version](https://img.shields.io/badge/Spring-4.3.18.RELEASE%2B-green)

![spring.boot.version](https://img.shields.io/badge/Spring.boot-1.5.13.RELEASE%2B-blue)

![jedis.version](https://img.shields.io/badge/Jedis-2.9.0%2B-red)

![caffeine.version](https://img.shields.io/badge/Caffeine-2.8.4%2B-blueviolet)

# 入门
## 依赖
```xml
<dependency>
    <groupId>com.github.attt</groupId>
    <artifactId>archer-core</artifactId>
    <version>${archer.version}</version>
</dependency>
```
## 注解
### 对象缓存
```java
interface UserService {

    @Cache(key = "'user:' + #userId", 
                expiration = 7, expirationTimeUnit = TimeUnit.DAYS, 
                breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
                valueSerializer = "customValueSerializer",
                keyGenerator = "customKeyGenerator",
                condition = "1 == 1",
                overwrite = false
        )
    User getUserById(long userId);
}
```
``key`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html))  缓存key

``expiration``、``expirationTimeUnit`` 缓存存活时间(ttl)

``breakdownProtect`` 是否开启缓存击穿保护

``breakdownProtectTimeout``、``breakdownProtectTimeUnit`` 缓存击穿保护的超时时间，仅在开启缓存击穿保护时有效

``valueSerializer`` 自定义值序列化工具

``keyGenerator`` 自定义缓存key生成器，优先级高于``key``

``condition`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html)) 缓存条件，仅当表达式值为true时进行缓存

``overwrite`` 是否覆盖缓存，开启时无论缓存是否存在都会被新值覆盖

⚠️注意：

所有的SpringEL表达式中的参数名称类似于``userId``需要在编译时通过jdk1.8的``-parameters``参数或者支持保留参数名的spring版本来支持，否则请使用参数序号如``arg0``或``param0``来表示


### 多对象缓存
```java
interface UserService {

    @CacheMulti(elementKey = "'user:' + #userIds$each",
                expiration = 7, expirationTimeUnit = TimeUnit.DAYS,
                breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
                valueSerializer = "customValueSerializer",
                keyGenerator = "customKeyGenerator",
                condition = "1 == 1",
                overwrite = false,
                orderBy = "#result$each.age"
    )
    List<User> getUsersByIdList(@MapTo("#result$each.id") List<Long> userIds);
    
    
    @CacheMulti(elementKey = "'user:' + #userIds$each",
                expiration = 7, expirationTimeUnit = TimeUnit.DAYS,
                breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
                valueSerializer = "customValueSerializer",
                keyGenerator = "customKeyGenerator",
                condition = "1 == 1",
                overwrite = false,
                orderBy = "#result$each.age"
    )
    @HashKey("#result$each$value.id")
    Map<Long,User> getUsersByIdList(@MapTo("#result$each.id") List<Long> userIds);

}
```

``elementKey`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html)) 结果每一个元素的缓存key

``orderBy`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html)) 自定义结果的排序顺序

⚠️注意：

``#result``表示返回结果，``#result$each``表示返回结果数组或者集合的每一个元素


### 列表缓存
```java
interface UserService {

    @CacheList(key = "'user:page:' + #pageId", elementKey = "'user:' + #result$each.id",
                expiration = 7, expirationTimeUnit = TimeUnit.DAYS,
                breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
                elementValueSerializer = "customValueSerializer",
                keyGenerator = "customKeyGenerator",
                elementKeyGenerator = "customElementKeyGenerator",
                condition = "#pageId == 1",
                overwrite = false
    )
    List<User> getPagingUsers(int pageId, int pageSize);
}
```

``elementValueSerializer`` 自定义元素值序列化工具

``elementKeyGenerator`` 自定义元素缓存key生成器，优先级高于``elementKey``

### 缓存淘汰
```java
interface UserService {

    @Evict(key = "#user.id", 
            keyGenerator = "customKeyGenerator", 
            afterInvocation = true, condition = "1==1")
    void deleteUser(User user);

    @EvictMulti(elementKey = "#userIds$each", 
            keyGenerator = "customKeyGenerator", 
            afterInvocation = true, condition = "1==1")
    void deleteUsers(List<Long> userIds);

}
```

``afterInvocation`` 是否在方法执行后执行

### 全局属性
```java
@Cacheable(prefix = "archer:example:user",
        valueSerializer = "customValueSerializer", 
        keyGenerator = "customKeyGenerator")
interface UserService {
}
```

``prefix`` 声明当前方法中的所有key的前缀


## 开始使用

### 使用默认配置
```java
    Archer.create("com.github.attt.example").init().start(UserService.class);
```

### 自定义组件
```java
    Archer.enableMetrics();
    Archer.serialization(Serialization.FAST_JSON);
    Archer.create("com.github.attt.example")
          .addKeyGenerator("customKeyGenerator", new CustomKeyGenerator())
          .addValueSerializer("customValueSerializer", new CustomValueSerializer())
          .addStatsListener(new AllCacheEventListener())
    .init().start(UserService.class);
```

## 使用redis或caffeine缓存实现
默认缓存实现是HashMap，要使用redis或者caffeine，需要：
### 依赖
#### redis
```xml
<dependency>
    <groupId>com.github.attt</groupId>
    <artifactId>archer-redis</artifactId>
    <version>${archer.version}</version>
</dependency>
```
#### caffeine
```xml
<dependency>
    <groupId>com.github.attt</groupId>
    <artifactId>archer-caffeine</artifactId>
    <version>${archer.version}</version>
</dependency>
```

## Spring配置
### 依赖
```xml
<dependency>
    <groupId>com.github.attt</groupId>
    <artifactId>archer-spring</artifactId>
    <version>${archer.version}</version>
</dependency>
```
### xml配置
引入命名空间
```xml
<beans 
       xmlns:archer="http://attt.github.com/schema/archer"
       xsi:schemaLocation="
       http://attt.github.com/schema/archer
       http://attt.github.com/schema/archer.xsd"
>
</beans>
```
增加配置
```xml
<archer:enable base-package="com.github.attt.example" serialization="HESSIAN" enable-metrics="true">
    <archer:shard-list>
        <bean class="com.github.attt.archer.cache.redis.RedisShard">
            <property name="database" value="6"/>
        </bean>
        <bean class="com.github.attt.archer.cache.redis.RedisShard">
            <property name="database" value="7"/>
        </bean>
        <bean class="com.github.attt.archer.cache.redis.RedisShard">
            <property name="database" value="8"/>
        </bean>
        <bean class="com.github.attt.archer.cache.redis.RedisShard">
            <property name="database" value="9"/>
        </bean>
    </archer:shard-list>
</archer:enable>
```
#### 配置和含义
``base-package`` 指定需要扫描的包路径

``serialization`` 指定默认的序列化方式，默认是java序列化

``enable-metrics`` 指定是否打开统计开关，默认打开，会发送各种统计事件

``shard-list`` 缓存配置

## SpringBoot配置
### 依赖
```xml
<dependency>
    <groupId>com.github.attt</groupId>
    <artifactId>archer-spring-boot-starter</artifactId>
    <version>${archer.version}</version>
</dependency>
```
### 启动类或者配置类
```java
@EnableWebMvc
@EnableArcher(basePackages = BASE_PACKAGE,
        enableMetrics = false, serialization = Serialization.FAST_JSON)
@SpringBootApplication(scanBasePackages = BASE_PACKAGE)
public class Application {

    public static final String BASE_PACKAGE = "com.github.attt.example";

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.run(args);
    }
}
```
### 缓存配置
如果使用[redis](#redis)或者[caffeine](#caffeine)，支持properties配置
```yaml
archer:
  redis:
    shards:
    - database: 0
      host: localhost
      port: 6379
    - database: 2
      host: localhost
      port: 6379
    connect-timeout: 400
```

也可以作为bean注入到spring
```java
@Configuration
class CacheConfig{

    @Bean
    RedisShard shard(){
        RedisShard redisShard = new RedisShar();
        redisShard.setHost("redis.host");
        redisShard.setPort(6505);
        return redisShard;
    }   
}
```
