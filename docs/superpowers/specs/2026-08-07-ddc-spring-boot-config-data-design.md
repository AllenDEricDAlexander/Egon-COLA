# DDC Spring Boot ConfigData 接入与分级刷新设计

状态：已确认，进入实施

编写日期：2026-08-07

代码基线：`main@3a735492`

主要涉及模块：

- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test`
- `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin`
- `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine`

本文已按 2026-08-07 的确认结论关闭决策项，后续实现必须以本设计和对应实施计划为准。

---

## 1. 需求结论

本次改造把 DDC 的业务配置接入 Spring Boot 3.5.16 原生 ConfigData 生命周期：

1. 使用 `ConfigDataLocationResolver` 和 `ConfigDataLoader` 在 ApplicationContext 创建前拉取远端配置；
2. 使用 Spring Boot 官方 `YamlPropertySourceLoader` 解析完整 YAML 配置资源；
3. 不把本地配置和远端配置复制到一个 Map 中做手工 merge；
4. 由 Spring Boot ConfigData 的 PropertySource 顺序处理覆盖、删除回退、占位符和类型绑定；
5. DDC 远端配置在 ConfigData 范围内优先级最高；
6. DDC 客户端自身配置只从本地 bootstrap 链路、系统属性、环境变量等本地来源读取，远端禁止覆盖；
7. 远端只承载业务配置，不承载 DDC 连接、认证、作用域或 ConfigData 控制配置；
8. 初始 YAML 在 Bean 创建前生效，普通 `@Value`、`Environment` 和 `@ConfigurationProperties` 首次绑定自然使用远端值；
9. 运行期只刷新明确支持动态更新的对象；不刷新 ApplicationContext，不重建整个容器，也不假装所有 Spring Boot 基础设施都能热更新；
10. 保留 `@DdcValue` 和现有 `DdcConfigApplier` 扩展点，避免破坏 Gateway、IdP 等既有消费者。

`bootstrap.yml` 在 Spring Boot 原生 ConfigData 中不是默认文件名。必须明确选择本文第 6 节的加载方式，不能只把文件放进 classpath 后假设 Boot 会读取。

---

## 2. 当前实现与问题

### 2.1 当前配置进入容器太晚

`DdcRuntimeCoordinator` 是 `SmartLifecycle`。当前顺序是：

```text
ApplicationContext 创建并完成 Bean 初始化
  -> Redis 订阅可用
  -> 注册 CONFIG_CLIENT
  -> 上报 @DdcValue 默认值
  -> HTTP pull
  -> DdcRefreshService 逐 configKey 写字段或调用自定义 Applier
```

因此远端配置不能参与以下启动行为：

- `@ConfigurationProperties` 首次绑定；
- 数据源、Web Server、线程池等自动配置条件判断；
- 普通 `@Value` 注入；
- Bean 创建阶段读取的 `Environment`；
- profile 激活后的配置文件选择。

### 2.2 当前模型是逐 Key 写入，不是 ConfigData

`DdcAdminClient.pull()` 返回 `List<DdcConfigValue>`。`DdcRefreshService` 按
`configKey + version + checksum` 逐项处理，默认回退到 `DdcFieldBindingService`，只更新
`@DdcValue` 字段。

这条链路有自己的优先级、版本和字段写入语义，但没有成为 Spring `Environment` 的
PropertySource。即使把 YAML 手工展开成若干 Key 再写入，也仍然不是 Spring Boot ConfigData
处理，且会重新实现一套本地/远端 merge 规则，违背本次目标。

### 2.3 当前已有公共动态扩展点，不能直接删除

以下类型已被 DDC 外部模块使用：

- `DdcConfigApplier`；
- `DdcConfigApplierRegistry`；
- `@DdcValue`；
- `DdcRefreshService` 的运行时发布链路。

Gateway 规则和 IdP 策略已经注册精确 Key 或前缀 Applier。本次新增 YAML ConfigData 主路径时，
这些面向非 Spring 配置对象的原子切换能力继续保留，不能强制迁移成通用 Bean 重绑。

### 2.4 当前 archetype 的 bootstrap 来自 Spring Cloud

三个 archetype 当前都依赖 `spring-cloud-starter-bootstrap`，由 Spring Cloud 兼容机制加载
`bootstrap.yml`。DDC Starter 自身没有该依赖。

本次目标是使用 Spring Boot 原生 ConfigData SPI，因此 DDC 不能在内部依赖
`PropertySourceLocator`、`BootstrapConfiguration` 或 Spring Cloud Context 的
`ConfigurationPropertiesRebinder`。本轮已确认使用第 6.1 节的纯 Spring Boot ConfigData 方式；
现有 archetype 的 Spring Cloud bootstrap 不作为 DDC Starter 的运行前提。

---

## 3. 目标与非目标

### 3.1 目标

1. 远端 YAML 在 ApplicationContext 创建前成为 ConfigData PropertySource；
2. 使用 Boot 官方 YAML 解析、Origin、profile 和 PropertySource 优先级能力；
3. DDC 业务配置高于所有本地 ConfigData，且不改变其他本地来源之间的官方顺序；
4. 禁止远端覆盖 DDC 自身配置和 ConfigData 控制键；
5. 启动拉取与运行期 Redis 刷新使用同一份 YAML 校验规则；
6. 动态 PropertySource 以不可变快照原子替换，不向本地配置写回；
7. 仅重绑显式声明可刷新的、可变的 `@ConfigurationProperties` Bean；
8. 不适合热更新的配置只在下次启动完整生效，并通过变更事件明确标记；
9. 保持现有发布版本、checksum、目标租约、ACK 和 reconcile 语义；
10. 删除现有 scalar/JSON/TXT 配置与默认值上报链路；`@DdcValue` 和 Gateway/IdP 自定义
    Applier 改为消费 YAML 展平后的叶子属性。

### 3.2 非目标

- 不引入 Spring Cloud Context 或 `@RefreshScope`；
- 不调用 `/actuator/refresh`；
- 不执行 `ApplicationContext` close/restart；
- 不承诺 `server.port`、DataSource、EntityManagerFactory、线程池、日志系统等任意基础设施自动热重建；
- 不从远端修改 active/default/include profile；
- 不允许远端递归导入其他 ConfigData；
- 不把本地 YAML、远端 YAML 和系统属性合并成 DDC 自有配置对象；
- 不修改现有 Flyway 文件；本轮推荐方案不需要数据库迁移；
- 不重构服务注册、Redis 拓扑或 Admin 发布状态机；
- 不自动启动 DDC Admin、Redis、PostgreSQL 或任何业务应用。

---

## 4. 配置资源契约

### 4.1 唯一远端资源模型

复用现有 DDC 配置项，不新增数据库字段：

```text
configKey   = application.yml
configValue = 完整 Spring Boot YAML 文本
valueType   = YAML
version     = DDC 发布版本
```

V1 每个 `bizCode + env + appCode` 只允许一个完整 YAML 资源 `application.yml`。Admin 不再接受
调用方提供 `configKey`、`valueType` 或 `defaultValue`，也不再创建或发布 scalar/JSON/TXT 配置项；
底层现有列可继续存储常量以避免无意义的数据库迁移。

这样满足“不 merge”：

- DDC 不把多条配置项组装成一棵 Map；
- DDC 不把本地 YAML 与远端 YAML 合并；
- `YamlPropertySourceLoader` 只解析远端完整文档；
- 同名属性最终取值完全由 Spring Environment 的 PropertySource 顺序决定。

### 4.2 位置语法

推荐显式位置：

```yaml
spring:
  config:
    import: ddc:application.yml
```

支持：

- `ddc:application.yml`：资源不存在或拉取失败时启动失败；
- `optional:ddc:application.yml`：只忽略资源不存在，不忽略认证失败、非法 YAML、重复资源、非法版本或保留键越权；
- `.yml` 与 `.yaml` 扩展名；
- 同一次启动同一逻辑资源只加载一次。

不支持：

- 绝对 URL、文件路径或任意协议透传；
- 在 location 中携带 endpoint、Access Key、Secret Key；
- 通配符扫描远端资源；
- 由远端 YAML 再声明新的 `spring.config.import`。

### 4.3 多文档和 profile 资源

V1 只允许单 YAML 文档。DDC 已由 `env` 隔离运行环境，无需再把
`application-{profile}.yml` 和 `---` 多文档激活规则叠加到远端。

限制单文档的原因不是 YAML 解析能力不足，而是运行期刷新必须保持 ConfigData 初始建立的
PropertySource 数量、激活条件和相对位置。动态增加/删除文档或修改
`spring.config.activate.on-profile` 需要重跑整个 ConfigData 处理器，不能通过单个动态
PropertySource 安全模拟。

V1 不实现 `resolveProfileSpecific(...)`，也不接受远端 profile 文件或多文档结构。

---

## 5. ConfigData 启动链路

### 5.1 SPI 注册

新增 `META-INF/spring.factories`：

```properties
org.springframework.boot.context.config.ConfigDataLocationResolver=\
top.egon.cola.component.ddc.bootstrap.DdcConfigDataLocationResolver
org.springframework.boot.context.config.ConfigDataLoader=\
top.egon.cola.component.ddc.bootstrap.DdcConfigDataLoader
```

ConfigData SPI 不能放进 `AutoConfiguration.imports`。后者在 ConfigData 已完成后才参与 Bean
定义装配，时机不满足启动前拉取。

### 5.2 `DdcConfigDataLocationResolver`

职责：

1. 只识别 `ddc:` 前缀；
2. 解析 `optional` 与远端资源名；
3. 使用 `ConfigDataLocationResolverContext.getBinder()` 从当前已加载的本地配置绑定
   `DdcProperties`；
4. 校验 `enabled`、`bizCode`、`env`、`appCode`、Admin Endpoint、HMAC 和 TLS；
5. 在 `ConfigurableBootstrapContext` 中登记或复用 `DdcBootstrapClient`；
6. 返回不包含凭据和远端正文的 `DdcConfigDataResource`；
7. 不在 `isResolvable(...)` 中发起网络请求。

`enabled=false` 时显式 `ddc:` import 返回空 ConfigData，不创建远端客户端；显式启用后，scope 和
连接配置必须完整，不能继续使用 `default-app` 等占位默认值连接远端。

Resource 的 `equals/hashCode` 只包含：

```text
optional + bizCode + env + namespace + appCode + resourceName
```

日志和 `toString()` 不包含 Access Key、Secret Key、证书内容或 URL user-info。

### 5.3 `DdcConfigDataLoader`

职责：

1. 从 BootstrapContext 获取 `DdcBootstrapClient`；
2. 通过现有 HTTP/HMAC/TLS 契约拉取当前 scope 的已发布快照；
3. 按 `resourceName` 精确选择 YAML 配置项，不把其他 configKey 拼入该资源；
4. 验证版本和资源唯一性，按正文计算 checksum，并检查最大内容长度；
5. 委托 `DdcYamlPropertySourceLoader` 解析；
6. 执行保留键校验；
7. 返回包含 `DdcDynamicPropertySource` 的 `ConfigData`；
8. 使用 `ConfigData.Option.IGNORE_IMPORTS` 和 `IGNORE_PROFILES`，禁止远端改变 ConfigData 导入和 active/include/default profile；
9. 将初始资源版本和 checksum 放入动态 PropertySource 快照，供运行期幂等比较使用。

### 5.4 `DdcBootstrapClient`

这是 ConfigData 阶段的轻量客户端，不是 Spring Bean：

- 复用现有 `HttpDdcAdminClient` 的 Endpoint、HMAC、TLS 和错误契约；
- 不依赖 AutoConfiguration、Redisson、ApplicationContext 或业务 Bean；
- 同一次启动的 Resolver/Loader 复用一个客户端和一次 scope pull 结果；
- 只读取已发布配置，不注册 CONFIG_CLIENT、不启动心跳、不订阅 Redis、不发送 ACK；
- Context 完成后仍由 `DdcRuntimeCoordinator` 注册、订阅并再次 reconcile，关闭“启动拉取到订阅建立”之间的发布竞态。

---

## 6. 本地 bootstrap 与导入方式

### 6.1 纯 Spring Boot ConfigData 方案（推荐）

Spring Boot 需要在搜索 ConfigData 之前知道 `spring.config.additional-location`。因此通过 OS 环境
变量、System Property 或命令行把 `bootstrap.yml` 声明为最后一个附加位置：

```bash
export SPRING_CONFIG_ADDITIONAL_LOCATION=optional:classpath:/bootstrap.yml
```

也可使用等价的 `-Dspring.config.additional-location=...` 或
`--spring.config.additional-location=...`。不能把该属性写进已经开始加载的 `application.yml` 后再
期待它改变搜索路径。

`bootstrap.yml` 作为最后的附加 ConfigData 位置，保存 DDC 客户端配置并导入远端：

```yaml
spring:
  config:
    import: ddc:application.yml

egon:
  cola:
    component:
      ddc:
        enabled: true
        biz-code: order
        env: dev
        app-code: order-service
        admin:
          endpoint: ${DDC_ADMIN_ENDPOINT}
          signature-enabled: true
          access-key: ${DDC_ACCESS_KEY}
          secret-key: ${DDC_SECRET_KEY}
          tls:
            enabled: ${DDC_TLS_ENABLED:false}
            development-plaintext: ${DDC_DEVELOPMENT_PLAINTEXT:false}
        redis:
          enabled: true
          mode: ${DDC_REDIS_MODE:SINGLE}
          host: ${DDC_REDIS_HOST:127.0.0.1}
          port: ${DDC_REDIS_PORT:6379}
```

DDC 不自动扫描 `bootstrap.yml`，也不新增 EnvironmentPostProcessor 偷改
`spring.config.location`。bootstrap 必须是最后的 additional location，且其中的 `ddc:` import
必须是最后一个同级 import，才能由 Boot 的官方顺序保证远端高于其他本地 ConfigData。

可以在 `application.yml` 使用 `spring.config.import: classpath:bootstrap.yml` 作为简化方式，但只适合
没有更高优先级外部 ConfigData 的应用。它不是“DDC 始终为 ConfigData 层最高”的通用证明，不能作为
本次优先级验收的唯一配置。

### 6.2 Spring Cloud bootstrap 边界

DDC Starter 不新增 Spring Cloud 依赖，也不对 Spring Cloud bootstrap 提供第二套加载实现。应用可
继续包含 Spring Cloud 依赖，但 DDC 的验收链路只认第 6.1 节的 Boot ConfigData additional-location。

---

## 7. 优先级定义

### 7.1 精确定义

“DDC 远端优先级最高”定义为：DDC 是所有 Spring Boot ConfigData PropertySource 中的最高
优先级来源。命令行参数、`SPRING_APPLICATION_JSON`、Java System Properties、OS 环境变量、
测试属性等仍按 Spring Boot 官方外部化配置顺序生效。

期望分层由高到低：

```text
Spring Boot 标准的非 ConfigData 高阶来源（保持各自官方顺序）
  > DDC 远端 ConfigData
  > 本地 bootstrap ConfigData
  > 本地 application-{profile}.yml
  > 本地 application.yml
  > 默认属性
```

如果要求 DDC 高于命令行或系统属性，就必须在 ConfigData 完成后手工重排 Environment，
这与“别的按照 Spring Boot 现有优先级”冲突，本方案不采用。

### 7.2 不手工 merge

实现禁止：

- 遍历所有 PropertySource 后写出一个最终 Map；
- 把本地 YAML 和远端 YAML 深度 merge；
- 为集合、Map 或对象发明 DDC 自有覆盖规则；
- 在 Bean 绑定后再用反射模拟启动时的属性优先级。

当远端删除一个属性时，`DdcDynamicPropertySource` 不再返回该属性，Spring Environment 自然
回退到下一个本地 PropertySource。这是 PropertySource 查找，不是 DDC merge。

运行时只接受 `application.yml`。任何其他远端 `configKey` 都视为服务端契约错误并拒绝，Starter
不会尝试把它拼入 YAML，也不会继续维护第二套配置优先级体系。

---

## 8. DDC 保留配置与安全边界

### 8.1 `DdcReservedConfigurationKeys`

启动和刷新共用同一套 canonical key 校验。推荐禁止以下远端键及其子键：

```text
egon.cola.component.ddc
spring.config.import
spring.config.location
spring.config.additional-location
spring.config.name
spring.profiles.active
spring.profiles.default
spring.profiles.include
spring.profiles.group
```

第一组保证 DDC 的 Endpoint、凭据、TLS、Redis、scope、租约、fail-fast 等配置不能被 DDC
自身覆盖。后两组保证远端不能改变配置来源和 profile 选择。

校验使用 Spring Boot canonical `ConfigurationPropertyName` 语义，不能只做大小写敏感的原始字符串
比较，避免 relaxed binding 变体绕过。

### 8.2 失败策略

发现任一保留键时拒绝整个远端 YAML，不做部分删除后继续：

- 启动阶段：抛出包含资源名、违规 canonical key 和 YAML Origin 的异常，不输出值；
- 运行期：保留旧动态快照，ACK `FAILED`，错误信息脱敏并限制长度；
- reconcile：记录告警并保留旧版本/checksum；
- 不允许“忽略违规键但应用其他键”，避免 Admin 显示发布成功而客户端只应用部分内容。

Starter 是最终强制边界。Admin 在创建、更新、回滚和发布阶段使用相同规则提前拒绝，以便在配置
进入发布状态机前给出明确反馈。

---

## 9. 运行期动态 PropertySource

### 9.1 `DdcDynamicPropertySource`

该类型是 ConfigData 初始返回的 PropertySource，也是运行期更新的唯一 Environment 写入点：

- 继承 `EnumerablePropertySource`；
- 内部使用 `AtomicReference<Snapshot>`；
- Snapshot 包含不可变属性、Origin、DDC version 和 checksum；
- `getProperty` 和 `getPropertyNames` 始终读取同一个快照；
- candidate YAML 完成解析和校验后一次 CAS/`set` 切换；
- 失败时旧快照不变；
- PropertySource 在 Environment 中的位置不变，不执行 remove/addFirst 重排；
- 不修改本地 PropertySource，也不向本地文件写回。

### 9.2 `DdcYamlPropertySourceLoader`

该类只做 DDC 边界适配：

1. 把远端 UTF-8 文本包装为带稳定文件名的 Resource；
2. 委托 `org.springframework.boot.env.YamlPropertySourceLoader`；
3. 保留 `OriginTrackedMapPropertySource` 的 Origin；
4. 规范化为 `DdcDynamicPropertySource.Snapshot`；
5. 调用 `DdcReservedConfigurationKeys`；
6. 不自己实现 YAML parser、扁平化、集合 merge 或 profile 表达式解析。

---

## 10. 分级刷新模型

### 10.1 刷新分类

| 配置消费者 | 启动时 | 运行期 | 规则 |
|---|---|---|---|
| `Environment#getProperty` | 使用远端 | 立即看到新动态快照 | PropertySource 原生能力 |
| 普通 `@Value` | 使用远端 | 不重注入 | 下次启动生效 |
| 普通 `@ConfigurationProperties` | 使用远端 | 不重绑 | 下次启动生效 |
| `@DdcRefreshable` + 可变 `@ConfigurationProperties` | 使用远端 | 受控重绑 | 校验成功才确认刷新 |
| `@DdcValue(refreshable = true)` | 读取 YAML 叶子 | 继续刷新 | 兼容注解，不再对应独立配置项 |
| `@DdcValue(refreshable = false)` | 读取 YAML 叶子 | 不刷新 | 下次启动生效 |
| 已注册 `DdcConfigApplier` | 按现状 | 继续调用 | 适合 Gateway 规则等领域原子切换 |
| Web Server/DataSource/JPA/线程池等基础设施 | 启动时使用远端 | 默认不重建 | 除非组件提供专用 Applier |

### 10.2 `@DdcRefreshable`

推荐定义：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DdcRefreshable {
}
```

只允许标注同时满足以下条件的 Bean：

- 是 Spring Boot `@ConfigurationProperties` Bean；
- JavaBean/setter 可变绑定；
- 不是 record、constructor binding 或不可变值对象；
- Bean 的更新不要求重建持有它的其他 Bean；
- Bean 自身允许重复绑定，且校验失败可回到旧配置。

误标不可刷新 Bean 时启动失败并指出 beanName 和 prefix，不能静默降级成“看起来支持刷新”。

### 10.3 `DdcConfigurationPropertiesRebinder`

职责：

1. 通过 `ConfigurationPropertiesBean.getAll(applicationContext)` 获取正式 Boot 绑定元数据；
2. 只选择带 `@DdcRefreshable` 的 Bean；
3. 对选中的 Bean 复用 `ConfigurationPropertiesBindingPostProcessor` 的正式绑定入口，只重新执行
   binding post-processor，不调用 `initializeBean`、destroy callback 或完整 Bean 生命周期；
4. 只处理本次 changed keys 命中的 prefix；
5. 绑定和 Validation 失败时报告失败，由上层恢复旧 PropertySource 后重绑旧值；
6. 不销毁单例、不替换 Bean 引用、不重复执行任意初始化/销毁回调；
7. 不处理普通 `@Value`。

删除属性对可变 Bean 的重绑需要单独验证。若 Binder 无法把删除恢复为下层 PropertySource 或类型
默认值，该 Bean 本轮标记 restart-required，不宣称热刷新成功。

### 10.4 `DdcYamlConfigApplier`

运行期 YAML 发布的处理顺序：

```text
校验 scope / target / checksum / version
  -> 用官方 YamlPropertySourceLoader 解析 candidate
  -> 校验保留键
  -> 计算 old/new resolved property names
  -> 原子切换 DdcDynamicPropertySource
  -> 重绑命中的 @DdcRefreshable Bean
  -> 将 changed YAML 叶子分发给 @DdcValue / 自定义 Applier
  -> 发布 DdcConfigurationChangedEvent
  -> 更新本地 version/checksum
  -> ACK
```

若解析、保留键校验或可刷新 Bean 重绑失败：

```text
恢复旧动态快照
  -> 尽力用旧 Environment 重绑已更新 Bean
  -> 不更新本地 version/checksum
  -> ACK FAILED
```

### 10.5 `DdcConfigurationChangedEvent`

事件在 PropertySource 切换和所有受控重绑完成后发布，至少包含：

```text
resourceName
version
checksum
changedKeys
addedKeys
updatedKeys
removedKeys
refreshedKeys
restartRequiredKeys
changeId（Redis 发布触发时）
```

事件不包含完整配置值、Secret 或证书正文。监听器抛错不回滚已成功的核心刷新；需要参与事务性应用的
组件应注册 `DdcConfigApplier`，不能依赖普通事件监听器。

---

## 11. 现有发布、ACK 与 reconcile

### 11.1 初始加载与运行时注册

ConfigData Loader 首次拉取发生在 CONFIG_CLIENT 注册前。ApplicationContext 就绪后仍执行：

```text
建立 Redis 订阅
  -> 注册 CONFIG_CLIENT
  -> 再次 pull/reconcile
  -> READY
```

再次 pull 必须用动态 PropertySource 中的初始 version/checksum 做幂等判断，不能重复触发相同版本的
Bean 重绑或变更事件。

### 11.2 YAML 叶子路由

`DdcRefreshService` 继续负责版本、checksum、锁、目标校验和 ACK，但只接收
`application.yml`。`DdcYamlConfigApplier` 在原子切换动态 PropertySource 后计算 changed leaf keys，
按现有精确 Key、最长前缀和 fallback 顺序逐叶调用 `DdcConfigApplierRegistry`。默认 fallback 只负责
刷新匹配的 `@DdcValue` 字段，不再处理独立远端配置项。

Gateway Admin 发布规则时，发布日志仍记录 `gateway.rules.chunk.*` 或
`gateway.rules.active` 叶子 Key；实际 DDC 配置资源始终是 `application.yml`。每个发布阶段读取当前
YAML、更新一个规则叶子、校验完整文档，并以当前文档版本发布。Gateway Engine 继续收到同名叶子
Key，因此现有规则原子激活 Applier 不需要改变领域契约。

### 11.3 非动态配置的 ACK

确认语义：只要新 YAML 已通过校验并原子更新动态 PropertySource，ACK 可为 `SUCCESS`。未重绑的
Bean Key 进入 `restartRequiredKeys`，下次启动生效。

这表示 ACK 确认“客户端接受了目标配置版本”，不等价于“所有基础设施对象已热重建”。

---

## 12. 包结构与兼容边界

用户建议的分包作为本次配置链路目标结构：

```text
top.egon.cola.component.ddc
├── bootstrap
│   ├── DdcConfigDataLocationResolver
│   ├── DdcConfigDataResource
│   ├── DdcConfigDataLoader
│   └── DdcBootstrapClient
├── environment
│   ├── DdcDynamicPropertySource
│   ├── DdcYamlPropertySourceLoader
│   └── DdcReservedConfigurationKeys
├── refresh
│   ├── DdcRefreshService
│   ├── DdcYamlConfigApplier
│   ├── DdcConfigurationPropertiesRebinder
│   └── DdcConfigurationChangedEvent
├── listener
│   ├── DdcRedisChangeListener
│   └── DdcRedisChangeSubscription
├── annotation
│   ├── DdcValue
│   └── DdcRefreshable
├── service
│   ├── DdcConfigApplier
│   └── DdcConfigApplierRegistry
└── legacy
    └── DdcFieldBindingService
```

这只是配置链路分包，不移动 `management`、`registry`、`security`、`model` 等无关包。

兼容要求：

- `DdcConfigApplier` 和 `DdcConfigApplierRegistry` 的包名、方法签名保持不变；
- `@DdcValue` 的包名和属性保持不变；
- `DdcFieldBindingService` 可留在现有包中，避免仅为目录美观制造破坏；
- `DdcRefreshService` 当前被测试和运行协调器直接使用。移动包前需保留兼容入口或同步修复所有当前
  消费者，不能仅为目录美观制造无关破坏；
- 保留 Admin 现有配置 CRUD/publish 路径、HMAC 规范请求、Redis Topic 和发布消息字段；配置请求字段
  收敛为 YAML 文档契约，并删除 `/defaults/report`。

---

## 13. 设计模式判断

### 13.1 采用的模式

采用 Adapter：

- `DdcConfigDataLocationResolver` / `DdcConfigDataLoader` 把现有 DDC HTTP 拉取契约适配成 Spring Boot ConfigData SPI；
- `DdcYamlPropertySourceLoader` 把远端文本资源适配给官方 `YamlPropertySourceLoader`，不重写 YAML 解析。

采用 Strategy + Registry：

- 保留现有 `DdcConfigApplier` / `DdcConfigApplierRegistry`；
- YAML PropertySource、Gateway 规则、IdP 策略和兼容字段是确实不同的动态应用策略；
- 精确 Key、最长前缀和 fallback 已是当前项目的稳定扩展方式。

### 13.2 不采用的模式

- 不新增通用 Factory 层：Resolver/Loader 的 SPI 和现有 AutoConfiguration 已负责对象创建；
- 不使用 Template Method：启动加载和运行刷新共享解析/校验组件即可，不需要继承层级；
- 不使用 Chain of Responsibility 扫描所有 Bean：刷新对象必须显式标记或注册，避免不可控副作用；
- 不引入 Decorator 包装所有 PropertySource：只有 DDC 远端来源需要动态快照。

采用 Observer：

- `DdcConfigurationChangedEvent` 只用于通知已经完成的刷新结果；
- 需要参与成功/失败判定的逻辑仍走同步 Applier 或 Rebinder，不把事务语义交给事件监听器。

---

## 14. 测试与验收

### 14.1 ConfigData SPI

1. `ddc:` Resolver 能被 `spring.factories` 发现；
2. Resolver 只识别 DDC location，且 `isResolvable` 不发网络请求；
3. Loader 在 Bean 创建前拉取，`@ConfigurationProperties`、普通 `@Value` 和条件装配读取远端值；
4. 远端 YAML Origin 保留资源名、文档和行列；
5. 非 optional 缺失失败，optional 只忽略不存在；
6. 认证、TLS、非法 YAML、重复资源、超限和保留键始终失败；
7. 同一启动只 pull 一次 scope 快照。

### 14.2 优先级矩阵

必须用真实 `SpringApplication`/ConfigData 启动测试证明：

| 来源组合 | 期望 |
|---|---|
| local application vs local profile | Boot 原有 profile 规则 |
| local application/profile vs bootstrap | bootstrap 按确认的导入位置生效 |
| 任意 local ConfigData vs DDC | DDC 胜出 |
| DDC vs OS env/System Property/CLI | Boot 官方高阶来源胜出 |
| DDC 删除 Key且本地有值 | Environment 回退本地值 |
| DDC 含自身配置 Key | 整份资源拒绝 |

测试还必须输出 PropertySource 名称和实际顺序，不能只断言一个最终值。

### 14.3 动态刷新

1. 同版本同 checksum 不重复刷新；
2. 同版本不同 checksum 失败；
3. 新版本合法 YAML 原子替换，读线程看不到部分状态；
4. 解析/保留键/重绑失败恢复旧快照与 metadata；
5. 普通 `@Value` 和普通 `@ConfigurationProperties` 不重绑；
6. `@DdcRefreshable` 可变 Bean 重绑并执行 Validation；
7. record/constructor-bound/不可变 Bean 被明确拒绝；
8. 删除 Key 的回退与 restart-required 规则有专项测试；
9. `@DdcValue`、Gateway/IdP 自定义 Applier 回归通过；
10. 事件 changed/refreshed/restartRequired 分类准确且不携带值。

### 14.4 Admin 与 Gateway

1. Admin 创建、更新、回滚、发布只接受单文档 YAML；
2. 空文档、多文档、非法 YAML、非 Map 根节点、保留键整份拒绝；
3. 每个 scope 只能存在 `application.yml`，返回模型固定显示 `application.yml/YAML`；
4. 默认值上报接口和客户端调用删除；
5. Gateway 的 chunk/activation 发布实际更新完整 YAML，且 Engine 仍收到原叶子 Key；
6. Admin Web 只展示 YAML 编辑器，不再显示 Key、类型和默认值控件，并展示服务端校验错误。

### 14.5 回归范围

至少执行：

```bash
./mvnw -B -ntp -pl \
egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter,\
egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test \
-am test
```

由于 Gateway 和 IdP 使用公共 Applier API，还需执行其受影响的 focused tests/compile。最终运行：

```bash
git diff --check
```

本任务不自动启动项目。外部 DDC Admin、Redis 与多进程验证由用户后续发起。

---

## 15. 已确认决策

1. V1 每个 `bizCode + env + appCode` 只有一个 `application.yml`，只支持单 YAML 文档；
2. 使用纯 Spring Boot ConfigData 链，由本地 `bootstrap.yml` 保存 DDC 客户端配置并导入 `ddc:`；
3. DDC 只在 ConfigData 范围内最高，其他来源继续遵循 Boot 官方优先级；
4. 远端禁止 `egon.cola.component.ddc.*`、`spring.config.*` 和 profile 选择键，违规时整份拒绝；
5. Starter 和 Admin 都执行 YAML/保留键校验，Admin Web 同步改为 YAML-only；
6. 未热刷新的 Key 可 ACK `SUCCESS`，通过 `restartRequiredKeys` 明确表达；
7. `@DdcRefreshable` 只支持 setter 可变的 `@ConfigurationProperties` Bean；
8. `@DdcValue` 仅作为 YAML 叶子属性兼容注解，不再上报默认值或创建独立配置；
9. 现有数据为空，不提供 scalar 数据迁移或双读兼容期。
