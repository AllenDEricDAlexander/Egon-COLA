# DDC 模块边界与 Starter 运行闭环调整设计

状态：已确认（2026-07-27）

编写日期：2026-07-27

代码基线：`main@37250665`

涉及模块：

- `egon-cola-component-dynamic-config-center`
- `egon-cola-component-gateway`
- `egon-cola-component-rpc`
- `egon-cola-components-bom`

---

## 1. 结论

本次调整将 DDC 对业务应用暴露的依赖统一为
`egon-cola-component-dynamic-config-center-starter`，删除独立的
`egon-cola-component-dynamic-config-center-management-client` Maven 模块。

原 management-client 中的管理 OpenAPI Client、DTO、HMAC 签名和共享实例元数据契约
并不迁入可执行 Admin 服务，而是合并到 Starter。这样同时满足：

1. 业务系统、Gateway 和 RPC 只依赖一个 DDC SDK 制品；
2. Gateway Admin 不会依赖 DDC Admin 的 JPA、Flyway、数据库、安全和 Actuator 运行时；
3. DDC Admin 继续通过 Starter 复用协议契约，但不反向暴露服务端实现；
4. 删除 BOM 中容易被业务方误认为需要单独引入的 management-client 条目。

DDC test 模块恢复为纯 Starter 消费端样例和黑盒验证模块。所有直接使用 DDC Admin
Repository、Service、Entity 或数据库脚本的测试迁回 Admin 模块。

---

## 2. 当前问题与证据

### 2.1 management-client 的边界错位

management-client 不是未使用模块。当前直接或间接消费者包括：

- DDC Starter：HTTP 传输安全、HMAC 签名、实例元数据契约；
- DDC Admin：管理 OpenAPI DTO、错误码和签名验证；
- Gateway Admin：配置写入、发布、任务查询和服务目录查询；
- Gateway Starter：HMAC 请求签名；
- RPC Starter：实例元数据编码。

因此不能把整个模块直接放入 DDC Admin 后再让 Gateway 依赖 Admin。这样会把
`spring-data-jpa`、Flyway、PostgreSQL、SQLite、Spring Security、Actuator 和 Admin
应用类传递到 Gateway Admin，形成控制面服务之间的错误编译依赖。

真正的问题是：这些跨模块协议已经成为 Starter SDK 的公共能力，却仍以第二个业务依赖
坐标单独发布和展示。

### 2.2 test 模块同时扮演了三种角色

当前 test 模块同时包含：

1. 只使用 Starter 的注解注入、刷新和租约生命周期测试；
2. 直接使用 Admin 发布状态机与 Repository 的服务端测试；
3. 使用 Testcontainers 验证 Admin Redis Repository 的 Sentinel/Cluster 测试。

因此 test POM 必须以 test scope 引入 Admin。这不符合“业务应用只引 Starter”的样例定位，
也让测试依赖无法证明真实的 Starter 边界。

### 2.3 当前 test 应用没有连接配置中心

`egon-cola-component-dynamic-config-center-test/src/main/resources/application.yml` 没有显式
配置 `admin.endpoint`，实际依赖 `DdcProperties.Admin` 中的
`http://localhost:18080` 默认值。

同时该文件配置了 `redis.enabled=false`。`DdcRuntimeCoordinator` 只有在
`DdcRedisChangeSubscription` 存在时才会装配，所以当前样例运行时不会执行：

```text
注册 CONFIG_CLIENT
  -> 上报 @DdcValue 默认值
  -> 从 Admin 拉取完整快照
  -> 订阅 Redis 发布消息
  -> 心跳续租
```

现有 `DdcSampleInjectionTest` 只证明 Admin 不可用且 `fail-fast=false` 时字段保留注解默认值，
不能作为服务注册或配置读取闭环证据。

---

## 3. 目标与非目标

### 3.1 目标

1. DDC 对普通消费者只暴露 Starter 一个 Maven 依赖；
2. 删除 management-client 模块及其 BOM、聚合 POM、README 条目；
3. 保持现有 Java 包名和公共类型签名，降低 Gateway/RPC 源码迁移量；
4. test 模块的 compile/test 依赖中不再出现 DDC Admin；
5. test 示例显式说明 Admin 地址、HMAC 凭据和 Redis 地址；
6. 启动测试能够证明 Starter 按顺序注册、上报默认值、拉取配置并应用；
7. Admin 内部与 Redis 拓扑测试继续保留，不降低现有覆盖率；
8. 明确区分配置客户端注册与 RPC/Gateway 服务注册。

### 3.2 非目标

- 不修改 Admin REST 路径、JSON 字段、HMAC 算法或错误码；
- 不修改 Redis Key、租约协议、同步发布和 ACK 语义；
- 不新增服务发现框架、客户端负载均衡或 Admin 节点选举；
- 不引入新的第三方库；management-client 已有依赖合并到 Starter，Testcontainers 只从
  test 模块迁移到 Admin test scope；
- 不重构 Gateway、RPC 或 DDC 的业务逻辑；
- 不启动 DDC Admin、Redis、PostgreSQL、Gateway 或 RPC 进程。

---

## 4. 目标模块边界

### 4.1 Starter

Starter 是唯一面向业务应用的 DDC SDK，包含：

- `@DdcValue`、字段绑定和本地配置应用；
- 配置客户端注册、心跳、下线、默认值上报、快照拉取和 ACK；
- Redis 配置发布订阅与服务目录订阅；
- RPC Provider、HTTP Provider、Internal Gateway 使用的注册契约；
- 原 management-client 的管理 OpenAPI Client 和 DTO；
- HMAC 规范请求、签名器、TLS HTTP Client 工厂；
- `ServiceInstanceMeta` 等 Gateway/RPC 共享元数据契约。

原 management-client 的 Java 包名保持不变，例如：

```text
top.egon.cola.component.ddc.management.*
top.egon.cola.component.ddc.security.*
```

只移动源码归属，不做无收益的包名重构。外部调用方只需要把 Maven 坐标替换为 Starter，
Java import 不变。

### 4.2 Admin

Admin 继续是独立可执行控制面，负责：

- 配置 CRUD、版本、回滚、发布与审计；
- PostgreSQL/JPA/Flyway 持久化；
- Redis 配置缓存、通知、配置客户端租约和服务注册租约；
- 管理 OpenAPI 与业务 OpenAPI 服务端实现；
- 同步发布状态机、精确目标 ACK 和失败恢复。

Admin 依赖 Starter 取得协议契约，不再依赖 management-client。Admin 不向 Gateway/RPC
暴露 Repository、Service、Entity 或可执行应用依赖。

### 4.3 Test

Test 只依赖：

- DDC Starter；
- Spring Boot Web；
- Spring Boot Test（test scope）。

Test 不再依赖 DDC Admin，也不再持有 Testcontainers 依赖或 Admin Redis 拓扑 Profile。

### 4.4 依赖关系

```text
业务应用 ───────────────> DDC Starter
RPC Starter ─────────────> DDC Starter
Gateway Starter ─────────> DDC Starter
Gateway Admin ───────────> DDC Starter
DDC Admin ───────────────> DDC Starter
DDC Test ────────────────> DDC Starter

不存在任何模块 ─────────> DDC Admin（作为库依赖）
```

---

## 5. Maven 与源码迁移

### 5.1 删除 management-client 模块

实施时执行以下结构变更：

1. 将 management-client 的 `src/main` 源码移动到 DDC Starter；
2. 将 management-client 的 `src/test` 测试移动到 DDC Starter；
3. 删除 management-client 子模块目录；
4. 从 DDC 聚合 POM 的 `modules` 和 `dependencyManagement` 删除该坐标；
5. 从 DDC Starter、DDC Admin POM 删除该依赖；
6. Gateway Admin、Gateway Starter 改为依赖 DDC Starter；
7. 从组件 BOM POM 和中英文 BOM README 删除 management-client 条目；
8. 更新 DDC 中英文 README 的模块列表、依赖示例和构建命令；
9. 更新仍引用旧模块名的当前有效设计/运行文档。历史 Spec 保留历史事实，不批量改写。

### 5.2 Starter 自动装配必须显式启用

management Client 合并进 Starter 后，Gateway Admin 等调用方可能只需要管理 Client，
不需要启动配置客户端运行时。为避免“仅使用 DTO/Client 却自动连接 localhost Redis/Admin”，
DDC 自动装配改为显式启用：

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: true
```

具体约束：

- `DdcProperties.enabled` 默认改为 `false`；
- `DdcAutoConfig` 的 `matchIfMissing` 改为 `false`；
- Gateway Engine 等真实配置客户端 Runtime 应用显式补上 `enabled: true`；
- DDC Admin、Gateway Admin 和仅使用管理 Client 的应用不启动 DDC Runtime；
- 现有已显式配置 `enabled` 的测试和样例保持原语义。

`DdcRegistryAutoConfig` 仍由 `egon.cola.component.ddc.registry.enabled` 独立控制。
RPC/Gateway 只使用服务注册能力时不必同时启动配置客户端 Runtime，但必须显式配置
Admin Endpoint、HMAC 凭据和 Redis 拓扑。

这是本次唯一有意的自动装配行为变化，目的是让“依赖 Starter”与“启动 DDC Runtime”解耦。

---

## 6. 配置与运行闭环

### 6.1 Admin 地址必须显式提供

删除 `DdcProperties.Admin.endpoint` 的 `http://localhost:18080` 默认值。配置客户端 Runtime
或服务注册 Client 任一启用时，`admin.endpoint` 必须是合法的 HTTP/HTTPS 根地址：

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: order-service
        env: dev
        namespace: default
        admin:
          endpoint: ${DDC_ADMIN_ENDPOINT}
          signature-enabled: true
          access-key: ${DDC_ACCESS_KEY}
          secret-key: ${DDC_SECRET_KEY}
```

未配置地址时在创建配置或注册 HTTP Client 阶段快速失败，错误信息必须包含完整属性名
`egon.cola.component.ddc.admin.endpoint`，不能以 NPE 或连接 localhost 的形式失败。

本轮继续使用单个 Endpoint。多 Admin 部署通过外部 VIP、DNS 或负载均衡地址访问，
不在 Starter 内增加 Admin 节点发现或客户端负载均衡。

### 6.2 HMAC 与 TLS

- `signature-enabled=true` 时，Access Key 和 Secret Key 必须非空；
- HTTPS 必须提供现有 mTLS 配置；
- 开发明文 HTTP 仍需显式 `development-plaintext=true`；
- 异常和 `toString()` 不得输出 Secret Key；
- test 示例使用环境变量，不提交真实凭据。

### 6.3 Redis 与配置客户端生命周期

完整远端模式需要共享 Redis：

```yaml
egon:
  cola:
    component:
      ddc:
        redis:
          enabled: true
          mode: SINGLE
          host: ${DDC_REDIS_HOST}
          port: ${DDC_REDIS_PORT:6379}
```

满足以下条件后，Starter 自动执行：

```text
创建并激活 Redis 订阅
  -> 向 admin.endpoint 注册 CONFIG_CLIENT 并取得 leaseId
  -> 上报 @DdcValue 默认值
  -> 按 appCode + env + namespace 拉取完整快照
  -> 应用配置并进入 READY
  -> 周期心跳与快照校准
  -> 收到 Redis 发布消息后应用并 ACK
  -> 关闭时主动下线
```

`redis.enabled=false` 只允许用于显式的本地默认值/单元测试场景。该模式不注册、不拉取、
不订阅，也不能作为配置中心联调证据；启动日志必须明确提示当前没有远端 DDC 生命周期。

### 6.4 配置客户端注册与服务注册的区别

- `DdcRuntimeCoordinator` 注册的是 `CONFIG_CLIENT`，用于配置发布目标、心跳和 ACK；
- `DdcServiceRegistryClient` 注册的是 `RPC_PROVIDER`、`HTTP_PROVIDER` 或
  `INTERNAL_GATEWAY`，由 RPC/Gateway 对应 Runtime 发起；
- `registry.enabled=true` 只装配服务注册 Client，不会猜测普通业务 Bean 并自动注册服务；
- DDC test 的消费端样例验证 `CONFIG_CLIENT` 闭环；RPC/Gateway 服务注册继续由各自集成测试验证。

---

## 7. 测试归位

### 7.1 保留在 DDC Test

以下测试只依赖 Starter，继续保留：

- `DdcLeaseLifecycleTest`；
- `DdcSampleInjectionTest`，但重命名或补充断言，明确它是离线默认值模式；
- `DdcSampleRefreshFlowTest`；
- `DdcRegistryLifecycleTest` 中纯内存注册、订阅、心跳、注销部分。

增加一个 Starter 启动闭环测试，使用 test scope 的 Recording/Fake Client 与激活的 Redis
Subscription Test Double，验证：

1. Spring Boot 应用只依赖 Starter 即可装配；
2. 启动顺序严格为 register、defaults、pull；
3. 拉取值真实写入 `SampleConfigService`；
4. 当前租约进入 READY，关闭时执行 offline；
5. 不加载任何 `top.egon.cola.component.ddc.admin` 类型。

该测试验证 Starter 编排，不伪装成真实外部 Redis/Admin 集成证明。

### 7.2 迁入 DDC Admin Test

以下测试直接验证 Admin 实现，迁入 Admin：

- `DdcSyncPublishFlowTest`；
- `DdcRedisSentinelIT`；
- `DdcRedisClusterIT`；
- `DdcRegistryLifecycleTest` 中 JPA 注解和数据库脚本边界断言。

Testcontainers 版本、依赖以及 `ddc-redis-sentinel`、`ddc-redis-cluster` Maven Profile
随这些测试移动到 Admin POM，现有 Profile 名称和执行语义保持不变。

### 7.3 Runnable Sample 配置

DDC test 主资源中的示例配置改为环境变量驱动，并显式出现所有关键地址：

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: ${DDC_ENABLED:false}
        app-code: ${DDC_APP_CODE:demo-app}
        env: ${DDC_ENV:dev}
        namespace: ${DDC_NAMESPACE:default}
        admin:
          endpoint: ${DDC_ADMIN_ENDPOINT:}
          signature-enabled: ${DDC_SIGNATURE_ENABLED:true}
          access-key: ${DDC_ACCESS_KEY:}
          secret-key: ${DDC_SECRET_KEY:}
          tls:
            enabled: ${DDC_TLS_ENABLED:false}
            development-plaintext: ${DDC_DEVELOPMENT_PLAINTEXT:false}
        redis:
          enabled: ${DDC_REDIS_ENABLED:true}
          host: ${DDC_REDIS_HOST:127.0.0.1}
          port: ${DDC_REDIS_PORT:6379}
```

默认 `DDC_ENABLED=false`，避免构建或误运行时连接外部服务。进行本地联调时必须显式启用并
提供 Admin Endpoint 和凭据。

---

## 8. 错误处理与兼容性

### 8.1 快速失败

配置客户端 Runtime 或服务注册 Client 显式启用后，下列配置错误在网络请求前失败：

- Admin Endpoint 缺失或不是合法 HTTP/HTTPS 根 URI；
- HMAC 已启用但 Access Key/Secret Key 为空；
- TLS 模式与 Endpoint scheme 不匹配；
- 心跳间隔不小于租约时间；
- Redis 拓扑参数不完整。

### 8.2 兼容性

- Java 包名、REST 路径、JSON 契约和 HMAC 规范保持不变；
- 直接依赖旧 management-client 坐标的调用方必须改为 Starter；
- 配置客户端 Runtime 由默认启用改为显式启用，现有真实运行应用必须补充
  `egon.cola.component.ddc.enabled=true`；
- 单独启用服务注册的应用必须显式配置 Admin Endpoint、HMAC 凭据和 Redis 拓扑；
- 隐式 localhost Admin Endpoint 被移除，现有运行配置必须显式填写；
- Redis Key、数据库表和 Flyway migration 不变，本次不新增或修改 migration；
- 不保留空壳 management-client 兼容模块，避免继续暴露错误依赖入口。

---

## 9. 设计模式判断

保留现有 Adapter：

- `DdcAdminClient` / `DdcManagementClient` 是端口；
- `HttpDdcAdminClient` / `HttpDdcManagementClient` 是 HTTP Adapter；
- 测试使用 Recording/Fake Adapter 验证编排。

本次问题是 Maven 制品边界、配置显式性和测试归属问题，不存在多算法切换、复杂构造或状态
扩展需求，因此不新增 Strategy、Factory、Facade 或新的接口层。继续使用现有 Adapter
已经足够，额外抽象只会增加迁移范围。

---

## 10. 验收标准

### 10.1 静态边界

1. 仓库中不存在 management-client 模块目录和 Maven 坐标；
2. BOM 只暴露 DDC Starter；
3. DDC test 的依赖树不包含 DDC Admin；
4. Gateway Admin、Gateway Starter、RPC Starter 编译通过；
5. `rg` 不再发现当前 POM/README/运行文档引用旧模块；
6. 历史 Spec 可保留旧名称，但必须被视为历史实现记录。

### 10.2 行为验证

1. Starter 单元测试通过；
2. management Client 原有序列化、HMAC、TLS 和 HTTP 测试迁移后全部通过；
3. test 模块证明 register、defaults、pull、apply、offline 编排；
4. Admin 单元/集成测试全部通过；
5. Sentinel/Cluster Profile 仍能找到并执行迁移后的测试；
6. Gateway Admin、Gateway Starter、Gateway Engine 和 RPC Starter 相关测试通过；
7. 显式启用配置客户端或服务注册但未配置 Endpoint 时，有确定、可读的启动失败；
8. 仅把 Starter 作为管理 Client 契约依赖时，不自动启动 DDC Runtime。

### 10.3 建议验证命令

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am clean test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am dependency:tree '-Dincludes=top.egon:*'

git diff --check
```

Sentinel/Cluster Profile 需要 Docker/Testcontainers 环境，作为单独的外部拓扑验证执行；普通
Maven 测试成功不能证明真实生产 Redis、PostgreSQL、DNS/VIP 或多 Admin 部署。

---

## 11. 实施顺序

1. 先补充 Starter 显式启用、Endpoint 校验与失败测试；
2. 迁移 management-client 源码和测试到 Starter；
3. 调整 DDC/Gateway/BOM POM，删除旧模块；
4. 将 Admin 专属测试和 Testcontainers Profile 迁入 Admin；
5. 精简 test POM，补齐 Starter 启动闭环与显式示例配置；
6. 更新 README 和当前运行文档；
7. 执行 DDC、Gateway、RPC 分层验证和依赖边界检查；
8. 每个实施任务独立提交，最终不启动任何应用进程。
