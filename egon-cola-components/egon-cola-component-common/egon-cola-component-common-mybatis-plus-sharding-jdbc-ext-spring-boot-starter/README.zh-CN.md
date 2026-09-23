# MyBatis-Plus + ShardingSphere-JDBC Starter

消费本 Starter 必须同时使用 PostgreSQL、MyBatis-Plus 与 ShardingSphere-JDBC。排除 `shardingsphere-jdbc` 不受支持，编译或启动必须失败。逻辑 `@Primary DataSource` 由一份 YAML（前缀 `egon.cola.component.mybatis-plus.sharding`）描述。`config-style: STRATEGY` 与 `config-style: NATIVE` 互斥。

推荐 STRATEGY：订单/明细使用 `COMPLEX_TENANT_THEN_BUSINESS`（先 `tenant_id` 再 `order_id`）。默认事务 LOCAL。`transaction-default-type` 只在 YAML 未写 `defaultType` 时填入 ShardingSphere 事务规则，校验只接受 `LOCAL` 或 `XA`，其他取值以 `LOCAL_TRANSACTION_REQUIRED` 失败。选择 `XA` 只是转发该取值：本 starter 不依赖任何 XA 事务 provider，宿主必须自行引入 provider 及其恢复管理器。

```yaml
egon:
  cola:
    component:
      mybatis-plus:
        sharding:
          enabled: true
          mode: SHARDING
          config-style: STRATEGY
          transaction-default-type: LOCAL
          data-sources:
            - name: master_data
              logical-name: master_data
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/master_data
              username: postgres
              password: postgres
            - name: shard_0
              logical-name: shard_0
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_0
              username: postgres
              password: postgres
            - name: shard_1
              logical-name: shard_1
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_1
              username: postgres
              password: postgres
          tables:
            users:
              type: SINGLE
              data-source: master_data
            dict_region:
              type: BROADCAST
            orders:
              type: COMPLEX_TENANT_THEN_BUSINESS
              sharding-column: tenant_id
              table-columns: [tenant_id, order_id]
              root-key-name: order_id
            order_items:
              type: COMPLEX_TENANT_THEN_BUSINESS
              sharding-column: tenant_id
              table-columns: [tenant_id, order_id]
              root-key-name: order_id
```

NATIVE 逃生舱：`native-rules-resource` 指向一份 ShardingSphere rules 文件。同一 YAML 不得再声明 STRATEGY `tables`。

```yaml
egon:
  cola:
    component:
      mybatis-plus:
        sharding:
          enabled: true
          mode: SHARDING
          config-style: NATIVE
          transaction-default-type: LOCAL
          native-rules-resource: classpath:egon-ss-native.yml
          data-sources:
            - name: shard_0
              logical-name: shard_0
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_0
              username: postgres
              password: postgres
            - name: shard_1
              logical-name: shard_1
              role: PRIMARY
              driver-class-name: org.postgresql.Driver
              jdbc-url: jdbc:postgresql://localhost:5432/shard_1
              username: postgres
              password: postgres
```

```yaml
# classpath:egon-ss-native.yml
databaseName: egon
rules:
  - !SHARDING
    tables:
      orders:
        actualDataNodes: shard_0.orders,shard_1.orders
        databaseStrategy:
          standard:
            shardingColumn: tenant_id
            shardingAlgorithmName: tenant_db
    shardingAlgorithms:
      tenant_db:
        type: CLASS_BASED
        props:
          strategy: STANDARD
          algorithmClassName: top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaLongTenantShardingAlgorithm
  - !SINGLE
    tables:
      - master_data.users
```

本组件使用 MyBatis-Plus 3.5.16，为 PostgreSQL 提供模型约束、Repository 命令、显式 SQL 查询、MybatisBatch、租户/版本保护、分片拓扑及受管 DDL 运行器。业务层保持 `Controller → Service → Repository → Mapper`；在现有 COLA 项目中，Application/Domain Service 保留业务职责，Repository 位于 infrastructure。

## 模型与主键

业务 PO 继承 `EgonModel<PO>`，配置 `@TableName`；不要重复声明技术字段。

| Java 字段 | SQL 列 | 类型与语义 |
| --- | --- | --- |
| id | id | Long / BIGINT，继承强制 `@TableId(type=ASSIGN_ID)` |
| tenantId | tenant_id | Long / BIGINT NOT NULL |
| createUserId / updateUserId | create_user_id / update_user_id | String，来自当前用户上下文 |
| createTime / updateTime | create_time / update_time | Instant，UTC、微秒精度 |
| deletedAt | deleted_at | LocalDateTime / timestamp(6)，未删除 NULL |
| version | version | Long / BIGINT NOT NULL，插入 0，更新与软删递增 |

逻辑删除使用 `@TableLogic` 与 `(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')`。主键适配器 `EgonColaIdentifierGenerator` 调用进程级静态 `SnowflakeIdGenerator`，该引擎由 `IdGeneratorAutoConfiguration` 在校验 ID 配置后一次性初始化，不重新实现分布式算法。实例必须配置唯一 `EGON_COLA_COMPONENT_ID_MACHINE_ID`；计数器只用于隔离测试。`@KeySequence` 与该 ASSIGN_ID 合同冲突，启动校验拒绝组合。

Common 允许任意非空 Long tenantId；ShardingSphere 宿主要求正 Long 分片键。技术字段统一由 MetaObjectHandler 填充，MyBatis-Plus 启用即装配该 handler，没有开关；扩展钩子只允许处理业务字段。SQL Injector 不承担元数据填充职责。

租户身份不再以注入 Provider 的方式传递。`EgonColaTenantIdProvider` 是基于 MDC 的静态不可实例化入口，MDC 键名由 `tenant-id.mdc-key` 配置（默认 `tenantId`），每次调用重新读取 SLF4J；上下文缺失或非数值时以 `TENANT_CONTEXT_MISSING` / `TENANT_CONTEXT_MALFORMED` 失败。模型校验共享同一静态绑定入口，若已有存活的冲突 owner 则以 `MODEL_VALIDATION_BINDING_CONFLICT` 拒绝重复绑定。

## Repository 与 CQRS

技术接口为 `EgonColaIRepository<T>`（扩展官方 `IRepository`），实现基类为 `EgonColaRepository<M,T>`。业务 Domain Service 不继承技术 CRUD 接口，也不携带 PO 泛型。具体 Repository 使用具名 Bean、Lombok 构造注入，只提供 mapper 与 properties 两个 getter，基类守卫从上述静态入口读取租户与校验上下文；参考脚手架中的具体实现。

- 命令使用 save、带版本的 updateById/removeById、受保护的批量 API；调用方检查影响行数。
- 业务 Query 使用命名 Mapper XML。每个 Mapper 都需提供 `selectActiveById`、`selectActiveByIds`、`deleteVersionedById`。
- ActiveRecord 不可用。QueryChain、lambdaQuery 等链式查询及通用 Query Wrapper 入口快速拒绝；不要使用 `.last()` 拼接 SQL。
- 自定义 UPDATE 中 MP 乐观锁插件会先增加实体版本，WHERE 必须绑定 `#{MP_OPTLOCK_VERSION_ORIGINAL}`。同时保留 `deleted_at IS NULL`、租户以及业务期望状态。
- 更新前从调用方或同一事务加载的行保留 id、tenant、create metadata、version。零行冲突不得当作成功。
- MybatisBatch 必须在相同 DataSource 的真实 Spring 事务内运行；空集合不发 SQL，重复/非法 ID 提前拒绝。默认分块 1000，总集合上限 10000，失败标记 rollback-only。

只在明确需要批量/特殊 SQL 的场景使用 Mapper 扩展。没有注册 MyBatis-Plus SQL Injector：启动校验要求上述三个语句必须来自 Mapper XML。字段 TypeHandler 用于 JSONB、数组等真实类型差异；Agent 的 JSONB 使用字段专用 handler，不覆盖全局 String handler。持久化枚举需唯一 `@EnumValue`，对外枚举值需匹配 `@JsonValue`/Jackson 合同，启动时校验。

## 注解缓存

`EgonColaRepository` 仅负责持久化，已移除 `getByCache`、`listByCache`、`getCachePortProvider`、`cacheRegionName` 与 CRUD 自动失效。
具体 Repository 在 public、非 final 业务方法上使用 Spring `@CacheConfig`、`@Cacheable`、`@CachePut`、`@CacheEvict`、`@Caching`，由外部 Bean 经代理调用，方法内部沿用基类 CRUD；基类的数据校验、租户与批量事务守卫保持不变。
单键读/写/失效统一使用具名 `keyGenerator = "egonColaRepositoryKeyGenerator"`，由它产出可信的 `tenant:id`；多参数与集合批量签名不支持该策略，需使用独立区域或显式失效。
所有影响缓存数据的写路径必须声明失效，普通 CRUD 不再自动失效；`@CachePut` 缓存方法返回值，不能挂在返回 boolean 的更新方法上。

两级缓存是必需组件（`egon.cola.component.cache.enabled` 缺省 `true`），本 starter 依赖受管理的 common-cache starter 并在启动时校验 CacheManager：组件启用而宿主仅存在不兼容的 `CacheManager` 时以 `CACHE_MANAGER_INCOMPATIBLE` 失败，不静默退回单级缓存；显式关闭组件时跳过该校验，键生成器仍可用。
Key、TTL 采样、事务提交/回滚、`sync` 与 `unless` 的限制、组合注解和手动操作以 [缓存 starter 文档](../egon-cola-component-common-cache-spring-boot-starter/README.zh-CN.md) 为单一事实源。

## SQL 与事务保护

原始 SQL Guard 在执行前验证正 ID 范围；TenantLine 后再次验证最终 SQL 的租户、active、版本和审计条件。全表更新/删除拦截、乐观锁、PG 分页与 LOCAL 写目标保护统一装配。动态表名默认关闭，只接受显式白名单映射。

LOCAL Guard 与声明的事务类型无关，跨 SqlSessionFactory 检查事务目标。一个事务可写同一物理组的多表，跨组写入以 `LOCAL_WRITE_TARGET_MISMATCH` 拒绝，无真实事务的多目标写入以 `LOCAL_TRANSACTION_REQUIRED` 拒绝。`BASE` 在拓扑校验阶段即被拒绝，本组件既不提供也不验证 XA 恢复管理器。精确 root-key 批量语句必须通过 `local-write-guard.allowed-root-statements` 注册完整 statementId 和列名；不接受普通业务以任意 Wrapper 绕过 ID/版本保护。

分页上限 500。IllegalSQL 仅 `dev` 允许。同时启用 dev/prod 会被拒绝。

## 受管 DDL 与 ShardingSphere

`EgonColaPostgreDdlRunner` 接受显式物理 PRIMARY、schema、role 和 SHA-256 manifest。先执行 schema advisory lock，再校验空/受管历史与脚本前缀；SQL 和 ddl_history 在同一连接、同一事务提交。未知提交结果用新连接核实，不直接重放。非空未受管库、校验和漂移、路由指纹变化需要人工处理；没有自动 DROP/repair/历史导入。

不要把 `EgonColaDdlTargetBO` 注册为默认 MP IDdl Bean，也不要混用 MP 默认 DdlApplicationRunner。本 Starter 负责物理池、拓扑校验、TableInfo 维护、脚本 DDL 与逻辑数据源；应用不得再复制 `ShardingDataSourceBootstrapper`。

七个生成脚手架都依赖本 starter：其中六个由该运行器接管并显式启用，旧 B/V/manual SQL 原样归档；Agent 关闭该运行器并继续使用 Flyway。

## 配置与验证

核心配置位于 `egon.cola.component.mybatis-plus`；源脚手架四个 profile 给出完整配置。Common 的 `ddl.enabled` 默认 false，六个非 Agent 脚手架显式启用，Agent 显式关闭。主键配置位于 `egon.cola.component.id`。

CPU/Mock/H2 用例参考官方 MyBatis-Plus 测试的真实 Mapper/插件调用方式。它们不证明 PG DDL、复制、分片实际落点或性能。真实 PG/SS 测试默认跳过，由环境变量开启：`EGON_MP_PG_MODEL_TEST=true` 启用逻辑删除与乐观锁用例，`EGON_MP_PG_DDL_TEST=true` 启用受管 DDL 用例，两者都从 `EGON_MP_PG_URL` / `EGON_MP_PG_USER` / `EGON_MP_PG_PASSWORD` 读取专用测试库连接。这些用例不在 CI 中执行，必须手动运行；SQL 性能需在真实数据分布上以 EXPLAIN 验收。
