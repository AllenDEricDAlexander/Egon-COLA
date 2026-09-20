# Egon COLA Spring Cache Starter

Spring Cache 原生注解 + L1 Guava + L2 Redisson `RMapCache`，通过一个 Redis Pub/Sub 通道失效各节点 L1。
适用于 Java 21 / Spring Boot 3.5.x。mp-sd-ext 只负责持久化，缓存策略由具体 Repository 的业务方法声明。
沿用 Spring Cache SPI 和现有两级缓存实现，不提供自定义缓存注解、SpEL 解析器或业务切面。

## 1. 启用

引入本 starter，并由宿主提供 `RedissonClient` Bean；组件不创建客户端。启用配置：

```yaml
egon:
  cola:
    component:
      cache:
        enabled: true
        ttl:
          expire: PT30M
          null-expire: PT60S
          jitter-ratio: 0.1
        regions:
          UserPO:
            expire: PT10M
            null-expire: PT20S
            jitter-ratio: 0.2
          UserSearch:
            expire: PT2M
```

在宿主的配置类显式启用 Spring AOP 缓存。将启用注解与组件开关放在一起，可以保持默认关闭时直通数据库：

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "egon.cola.component.cache", name = "enabled", havingValue = "true")
@EnableCaching(proxyTargetClass = true)
public class CacheConfiguration {
}
```

自动配置先于 Boot 的 `CacheAutoConfiguration` 注册 `egonColaTwoLevelCacheManager`。
宿主已经提供任意 `CacheManager` 时，整组自动配置让位；如有多个 manager，通过
`@CacheConfig(cacheManager = "egonColaTwoLevelCacheManager")` 或每个注解上的 `cacheManager` 明确选择。
`@EnableCaching` 与 `enabled=true` 是两个独立开关，只有注册 manager 并不会启用注解拦截。
`spring-boot-starter-cache` 随本 starter 传递，Redisson 客户端仍由宿主提供。

## 2. mp-sd-ext 集成

`EgonColaRepository` 不再提供 `getByCache`、`listByCache`、`getCachePortProvider`、`cacheRegionName`，
也不再在 CRUD 写方法内隐式失效缓存。原调用者应迁移到具体 Repository 的注解方法；这是明确的 API 变更。
业务 Service 通过注入的 Repository Bean 调用这些 public、非 final 方法：

```java
@Repository
@CacheConfig(cacheNames = "UserPO")
public class UserRepository extends EgonColaRepository<UserDAO, UserPO> {
    // 构造注入 mapper、校验器、租户提供者及配置，沿用项目已有实现。

    @Cacheable(key = "T(org.slf4j.MDC).get('tenantId') + ':' + #p0", sync = true)
    public UserPO findCachedById(@NotNull @Positive Long id) {
        return getById(id);
    }

    @CacheEvict(key = "T(org.slf4j.MDC).get('tenantId') + ':' + #p0.id", condition = "#result")
    public boolean updateCachedById(@NotNull UserPO entity) {
        return updateById(entity);
    }

    @CacheEvict(allEntries = true, condition = "#result")
    public boolean updateCachedBatch(Collection<UserPO> entities) {
        return updateBatchById(entities);
    }
}
```

缓存注解放在具体业务入口上；基类 final CRUD 方法保留原有数据校验、租户和事务守卫。
禁止给 final/private 方法加注解并期望 CGLIB 拦截。批量写依旧由上层事务包围。
所有影响缓存数据的新增、修改、删除及自定义 SQL 路径都必须声明相应失效；直接调用普通 CRUD 不再自动失效。
批量读不要把一个 `List<PO>` 写到单实体区域的某个 ID Key；如缓存整个批次，应使用独立区域，规范化 ID 顺序和重复值，并在写入时失效该区域。

## 3. 注解、SpEL 和组合操作

Spring 负责 `@Cacheable`、`@CachePut`、`@CacheEvict`、`@Caching`、`@CacheConfig` 的标准解析。
推荐 `#p0` / `#a0` 避免依赖参数名；也支持 `#id`、`#root.methodName`、`#root.target`、静态方法和 `@beanName`。

```java
@CacheConfig(cacheNames = "UserByCode", cacheManager = "egonColaTwoLevelCacheManager")
public class UserLookupRepository {
    // keyFactory 是宿主注册的 Bean，返回带当前租户前缀的稳定字符串。
    @Cacheable(key = "@keyFactory.byCode(#p0)",
               condition = "#p0 != null && !#p0.isBlank()", unless = "#result == null")
    public UserPO findByCode(String code) { /* 查询数据库，返回 PO */ }

    @Caching(
        put = @CachePut(cacheNames = "UserPO",
                       key = "T(org.slf4j.MDC).get('tenantId') + ':' + #result.id",
                       unless = "#result == null"),
        evict = {
            @CacheEvict(cacheNames = "UserByCode", allEntries = true),
            @CacheEvict(cacheNames = "UserSearch", allEntries = true)
        })
    public UserPO updateAndReload(UserPO entity) { /* 更新成功后重新查询并返回完整 PO */ }
}
```

上述片段展示注解位置，省略业务实现。`@CachePut` 缓存的是**方法返回值**，不能直接放在返回 `boolean` 的
`updateById` 上，否则会把布尔值写进 PO 缓存。部分字段更新后不要把不完整入参当成完整 PO 缓存；优先失效，或重新查询后返回。

- `condition` 决定是否参与缓存；`@Cacheable` 在调用前求值，不能访问 `#result`。
- `unless` 在执行后否决缓存写入，可访问 `#result`；`@CacheEvict` 没有 `unless`，成功写后失效可用 `condition = "#result"`。
- `@CacheEvict` 默认成功返回后执行；`beforeInvocation=true` 即使业务失败或事务回滚也立即失效，不能使用 `#result`。
- `@Caching` 用于多个更新/失效操作。本项目约定其中不放 `@Cacheable`，避免“命中时跳过执行”与写操作混合。
- `sync=true` 只能指定一个 cache，不允许 `unless`，也不能与其他缓存操作组合。不要用 `@Caching` 规避这些限制。

参考：[Spring 缓存注解](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)、
[
`Cacheable` API](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/cache/annotation/Cacheable.html)。

## 4. Key 和租户边界

所有 Key 必须是 `tenantId:businessKey` 字符串，例如 `41:7`、`41:code:alice`、`41:query:page:1`。
租户前缀为数值，业务段非空，不允许 `*` 和控制字符；区域名为 `[A-Za-z0-9_.-]{1,100}`。
默认 Spring `SimpleKey` / Long Key 不符合此约定，必须显式使用 SpEL 或返回上述字符串的 `KeyGenerator`。
组合参数须采用无歧义编码，包含影响结果的排序、分页、权限范围等维度；不要直接拼接不受控分隔符。

每次 Cache SPI 访问（包括缓存命中）校验 MDC 中配置的租户，不允许跨租户读写或缺失上下文。
`@CacheEvict(allEntries=true)`、`Cache.clear()` 和 `Cache.invalidate()` **只清当前租户的该区域**，不会清其他租户。
该语义是本组件为多租户区域提供的约束，与全局物理区域清空不同。跨租户任务应逐租户设置上下文后操作。
精确 Key 和 `tenantId:*` 失效事件继续共享一个通道。启用业务字符串 Key 前应完成所有节点升级，旧节点仍会拒绝非数字业务段。

## 5. 空值、雪崩与击穿

- **穿透**：默认将 null 作为内部哨兵缓存，使用较短的 `null-expire`，可与 `sync=true` 一起使用。
  `unless = "#result == null"` 只表示“不缓存空值”，重复查询不存在的数据仍会回源，不能单独作为穿透防护。
  必须不缓存空值时，可在业务入口增加布隆过滤器；业务负责初始化、增量更新和误判后的数据库查询，starter 不推断数据全集。
- **雪崩**：每次写入按 `[TTL, TTL × (1 + jitter-ratio)]` 采样一次，L1/L2 共用样本。
  `regions.<cacheName>` 按字段覆盖全局 TTL；未指定的空值 TTL 和抖动比例继承全局配置。
  TTL 至少为 1ms，抖动比例限制为 `[0, 0.5]`。
- **击穿**：`@Cacheable(sync=true)` 调用 `Cache.get(key, Callable)`。本机合并同 Key 的重叠加载，
  Redis 正常时再使用分布式锁与二次查询。锁等待超时、Redis 故障时回源但不回填，本机重叠请求仍共享本次结果。
  分布式锁采用有限租期，超过租期或等待时间、Redis 故障时，不保证全群集只执行一次。
  按数据库最坏耗时配置 `lock.wait-time` / `lock.lease-time`，不要把 `sync=true` 当成业务互斥锁。

## 6. 事务、集群一致性和手动操作

存在实际 Spring 事务和事务同步器时，`put`、`evict`、`clear` 在提交后执行，回滚丢弃；无事务时立即执行。
事务内读绕过缓存，加载结果只在提交后发布，避免其他事务看见未提交值。事务内同步加载不跨事务合并。
需要缓存写入处于同一事务时，应由外层 Service 开启事务，或让事务 advisor 的顺序先于缓存 advisor。
`beforeInvocation=true` 使用立即失效方法，保留 Spring 原生行为。

写节点修改 L2 并发布事件；其他节点只失效 L1，避免重复/迟到事件删除 L2 的新值。
L2 回填 L1 使用剩余 TTL，并扣除读取耗时。Pub/Sub 丢消息、并发回源与写入竞态仍可能造成 TTL 范围内的旧值，
该缓存不承诺强一致性。Redis 查询、写入、锁获取/释放失败按原有 `CACHE_L2_OPERATION_FAILED` 日志降级。
组件关闭时销毁内部调度器，不关闭宿主 RedissonClient。

同类 `this.findCachedById()` 不经过 Spring 代理。优先让外部 Bean 调用注解入口；复杂流程可通过相同 manager 手动操作：

```java
Cache cache = cacheManager.getCache("UserPO");
UserPO value = cache.get(tenantId + ":" + id, () -> repository.getById(id));
cache.evict(tenantId + ":" + id);
```

该方式保留租户校验、两级同步和提交后处理。宿主可以用 `RedisTemplate` 操作**独立命名空间**的专用缓存，
但不能直接修改本组件的 `RMapCache` Redis Key：其内部结构、序列化、TTL 元数据与 L1 事件协议必须一起维护。
直接 `RedisTemplate.delete` 不会自动清理各节点 L1，也不会自动具备本组件的事务语义。

为兼容已有手动调用，`egonColaCachePort` Bean 和 common-core 的 `EgonColaCachePort` 保留；
mp-sd-ext 及脚手架已不再注入它。`second-evict-delay` 仅作用于旧端口的提交后延迟失效，注解路径使用上面的 Cache SPI 语义。

## 7. 配置

所有键均位于 `egon.cola.component.cache` 下：

| 键                                                | 默认值                     | 用途             |
|--------------------------------------------------|-------------------------|----------------|
| `enabled`                                        | `false`                 | 自动配置开关         |
| `node-id`                                        | 随机 JVM 标识               | 忽略本机事件回声       |
| `key-prefix`                                     | `egon:cola:cache`       | L2 和锁命名空间      |
| `redis.topic`                                    | `egon:cola:cache:event` | 单一事件通道         |
| `tenant-mdc-key`                                 | `tenantId`              | 当前租户上下文        |
| `l1.max-size`                                    | `10000`                 | 每个区域本机容量       |
| `ttl.expire`                                     | `PT30M`                 | 普通值基础 TTL      |
| `ttl.null-expire`                                | `PT60S`                 | 空值基础 TTL       |
| `ttl.jitter-ratio`                               | `0.1`                   | TTL 增量随机比例     |
| `regions.<name>.expire/null-expire/jitter-ratio` | 继承全局                    | 区域级覆盖          |
| `batch.max-keys`                                 | `1000`                  | 旧端口批读上限及分批删除大小 |
| `lock.wait-time`                                 | `PT0.5S`                | 分布式锁最大等待       |
| `lock.lease-time`                                | `PT10S`                 | 分布式锁租期         |
| `second-evict-delay`                             | `PT5S`                  | 旧端口延迟二次失效      |

L2 codec 沿用受限类型白名单：`top.egon.cola.`、`java.util.`、`java.time.`、`java.lang.`。
不在白名单中的宿主对象不能直接作为 L2 值；不要仅根据 L1 命中判断 Redis 序列化已通过。

## 8. 验证

`EgonColaCacheAnnotationTest` 使用真实 Spring 缓存代理和 mock Redis，覆盖注解、SpEL、条件、组合、租户、TTL、并发和事务。
mp-sd-ext 的 `EgonColaRepositoryCacheEnhancementTest` 验证具体 Repository 与基类 CRUD 的代理集成。
真实 Redis/多节点测试仍通过 `-Degon.cola.cache.redis.it=true` 显式启用，需要调用者先准备并授权 Docker 环境。
