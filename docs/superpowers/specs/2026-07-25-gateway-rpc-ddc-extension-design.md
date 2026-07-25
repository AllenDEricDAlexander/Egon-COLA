# GWS-02 Gateway 所需 RPC/DDC 扩展 Spec

状态：草案，等待审核

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01

涉及现有模块：

- `egon-cola-component-dynamic-config-center-starter`
- `egon-cola-component-dynamic-config-center-management-client`（新增）
- `egon-cola-component-dynamic-config-center-admin`
- `egon-cola-component-dynamic-config-center-test`
- `egon-cola-component-rpc-starter`
- `egon-cola-component-rpc-test`

## 1. 目标

本 Spec 只补齐 Gateway 依赖 RPC/DDC 时缺少的公共契约。路由、限流、负载均衡、
生产 Gateway Handler 和 Admin 业务模型仍属于 Gateway，不下沉到 RPC/DDC。

当前事实：

- DDC 只有 `RPC_PROVIDER`、`INTERNAL_GATEWAY`；
- DDC 配置运行时固定使用字段绑定 Apply；
- DDC 配置只在启动/租约恢复时全量拉取，运行期依赖 full-value Pub/Sub；
- DDC HMAC OpenAPI 没有稳定的配置管理机器接口；
- RPC Provider 已校验 Contract，但没有公开只读 Contract Catalog；
- RPC Provider 注册 Metadata 只有框架字段；
- RPC Test 中的 Mock Gateway 不是生产 API。

## 2. 范围

### 2.1 DDC

1. 增加 HTTP Provider 服务类型；
2. 支持按 Config Key 组合自定义 Apply；
3. 增加配置周期版本校准；
4. 增加稳定、HMAC 保护的管理端机器 API 与 Client；
5. 增加配置值容量保护；
6. 保持服务 Metadata 的安全限制和可扩展性。

### 2.2 RPC

1. 暴露已校验的只读 Contract Catalog；
2. 导出可序列化 Protobuf Contract Snapshot；
3. 提供 Provider Metadata Contributor；
4. 固化 Gateway 可依赖的 Metadata/Error 契约；
5. 不提供生产 Gateway 实现。

## 3. 明确不做

- 不实现 Nacos、Dubbo 或兼容迁移；
- 不把 HTTP Provider 生命周期放入 Gateway Starter；
- 不把 Provider Directory、负载均衡或限流放入 DDC；
- 不把动态 gRPC Handler、Channel Cache 或 Forwarder 放入 RPC Starter；
- 不改变 RPC Consumer“只发现唯一 `INTERNAL_GATEWAY`”的规则；
- 不实现 DDC 多 Admin、Redis Sentinel/Cluster、选主或分布式锁；
- 不增加 RPC Streaming；
- 不直接让 Gateway Admin 调用 DDC Repository/JPA Entity。

## 4. DDC 服务类型扩展

### 4.1 枚举

增加：

```java
public enum DdcServiceKind {
    RPC_PROVIDER,
    HTTP_PROVIDER,
    INTERNAL_GATEWAY
}

public enum DdcLeaseRole {
    CONFIG_CLIENT,
    RPC_PROVIDER,
    HTTP_PROVIDER,
    INTERNAL_GATEWAY
}
```

`DdcServiceKind.leaseRole()` 保持同名映射。

### 4.2 HTTP Provider Service Key

```text
env
+ namespace
+ HTTP_PROVIDER
+ serviceName
+ group
+ version
+ protocol(http|https)
```

约束：

- `serviceName` 是稳定应用/服务代码，不使用 Host；
- `group` 默认 `default`，不能为空字符串；
- `version` 是可路由服务版本；
- `protocol` 只能为 `http` 或 `https`；
- `secure` 与 protocol 必须一致；
- Host、Port 属于 Instance，不进入 Service Key。

### 4.3 Redis 与数据库

- 继续复用 DDC 当前服务注册 Redis Key 结构；
- Service Registry 事实仍只存在 Redis；
- 不为 `HTTP_PROVIDER` 新增数据库表；
- Redis 重启后 Provider Runtime 使用新租约重新注册；
- 旧 `leaseId` 不能续约新实例。

## 5. DDC 可组合配置应用器

### 5.1 问题

当前 `DdcAutoConfig` 把 `DdcRefreshService` 固定绑定到
`DdcFieldBindingService::apply`。Gateway Rule Snapshot 只有完成 Schema 校验、
路由编译、资源准备和本地原子切换后才能 ACK，不能先刷新一个 String 字段再异步生效。

### 5.2 目标契约

保留现有函数接口：

```java
@FunctionalInterface
public interface DdcConfigApplier {
    void apply(String configKey, String value, long version);
}
```

新增注册契约：

```java
public interface DdcConfigApplierRegistry {
    void registerExact(String configKey, DdcConfigApplier applier);
    void registerPrefix(String configKeyPrefix, DdcConfigApplier applier);
    DdcConfigApplier resolve(String configKey);
}
```

行为：

1. 精确 Config Key 自定义 Applier 优先；
2. 没有精确匹配时，使用最长前缀匹配；
3. 没有自定义 Applier 时回退现有字段绑定；
4. 重复注册同一精确 Key 或同一前缀时应用启动失败；允许前缀嵌套，并始终选择最长
   前缀；
5. 前缀必须以业务命名空间和分隔符结尾，例如 `gateway.rules.chunk.`，不能注册空前缀
   或直接接管全部 Config Key；
6. 自定义 Applier 抛出异常时：
   - 本地版本和 checksum 不更新；
   - ACK 为 `FAILED`；
   - `currentVersion` 保持旧值；
   - 错误信息经过长度限制和脱敏；
7. Applier 返回即表示本地配置已经真正生效，不允许返回后再异步切换。

`DdcRefreshService` 继续负责 Target、Version、Checksum、锁和 ACK；Gateway 只实现
Rule Activation 精确 Key 与 Rule Chunk 前缀对应的 Applier。

注册发生在 Spring Bean 初始化阶段；`DdcRuntimeCoordinator` 启动前冻结 Registry。
运行期不允许动态替换 Applier，防止同一 Config Version 在不同实现之间漂移。

### 5.3 并发

- 同一 Config Key 使用现有本地锁串行 Apply；
- 不同 Config Key 可以独立；
- 低版本消息返回 `IGNORED`；
- 相同版本且 checksum 相同返回幂等 `SUCCESS`；
- 相同版本但 checksum 不同视为协议错误并返回 `FAILED`，不能覆盖。

## 6. DDC 配置周期校准

### 6.1 需求

运行期即使租约和心跳正常，也必须补偿单次 Redis Pub/Sub 丢失。

新增配置：

```yaml
egon:
  cola:
    component:
      ddc:
        consistency:
          config-reconcile-enabled: true
          config-reconcile-interval-seconds: 30
```

约束：

- 间隔必须大于 0；
- Gateway Engine 生产环境默认开启；
- 测试可以缩短间隔；
- 校准线程不能阻塞 DDC 心跳线程。

### 6.2 算法

1. 调用现有 Config Pull API 获取当前 scope 全量配置；
2. 按 `configKey` 与本地 `version + checksum` 比较；
3. 远端版本更高时调用同一 `DdcRefreshService.applySnapshot`；
4. 版本相同 checksum 不同视为数据损坏，记录错误并保持本地值；
5. 远端版本更低不回退；
6. 拉取失败进入下一周期重试，不清空本地配置；
7. 校准 Apply 不属于某个活跃 `changeId`，不伪造发布 ACK。

周期校准只保证最终收敛，不替代同步发布的精确 Target ACK。

## 7. DDC 管理端机器接口

### 7.1 边界

Gateway Admin 是机器调用方，需要配置 Upsert、发布、任务查询和实例投影。当前
`/api/v1/ddc/configs` 与 `/api/v1/ddc/publish-tasks` 是管理端接口，部分响应暴露
JPA Entity，不适合作为 Component 间稳定契约。

新增 HMAC 保护前缀：

```text
/api/v1/ddc/openapi/management
```

沿用现有 Access Key、时间戳、Nonce、Body SHA-256 和 HMAC 签名规则。

### 7.2 API

| Method | Path | 用途 |
|---|---|---|
| PUT | `/configs/{appCode}/{env}/{namespace}/{configKey}` | 幂等创建或更新 Config Draft |
| DELETE | `/configs/{appCode}/{env}/{namespace}/{configKey}` | 删除不再引用的 Config |
| POST | `/configs/{appCode}/{env}/{namespace}/{configKey}/publish` | `SYNC_ALL_ACK` 发布 |
| GET | `/publish-tasks/{changeId}` | 查询任务和 Target ACK |
| POST | `/publish-tasks/{changeId}/retry` | 按原 Target 重试 |
| GET | `/instances` | 按 appCode/env/namespace 查询 Config Client 投影 |
| GET | `/registry/services` | 查询服务目录 |
| GET | `/registry/instances` | 查询服务实例 |

Service Registry 已有 OpenAPI 可以复用实现，但机器 Client 对外提供统一 Facade。

### 7.3 稳定 DTO

```text
DdcManagementConfigUpsertRequest
DdcManagementPublishRequest
DdcManagementPublishResult
DdcManagementPublishTask
DdcManagementPublishTarget
DdcManagementConfigClientInstance
DdcManagementServiceCatalog
DdcManagementServiceSnapshot
```

禁止返回：

- `DdcConfigItemEntity`；
- `DdcPublishTaskEntity`；
- `DdcPublishAckEntity`；
- 任意 Repository Page；
- Redis 原始 Key 或 JSON。

`DdcManagementConfigClientInstance` 至少包含：

```text
appCode/env/namespace
instanceId/leaseId
host/port
leaseRole
registeredAt/lastHeartbeatAt/expireAt
```

Host/Port 是 Config Client 自报的管理地址，只能作为 Gateway Admin 受控查询节点状态
的候选地址；调用方仍需执行可信网段、租约身份和超时校验。

发布结果必须包含：

- `changeId`；
- `status`：`SUCCESS|FAILED|TIMEOUT|UNKNOWN`；
- `targetVersion`；
- `contentChecksum`；
- 固化 Target 数量与各 Target 结果；
- 失败原因；
- 创建、分发、完成时间。

### 7.4 Management Client 模块

新增独立：

```text
egon-cola-component-dynamic-config-center-management-client
```

它只包含稳定 DTO、HMAC Signer、Spring `RestClient` Adapter、超时与错误转换，不依赖
Redisson、DDC Runtime AutoConfiguration、JPA 或 Admin 实现。Gateway Admin 不应为了
机器管理调用引入完整 DDC Starter。

该模块提供：

```java
public interface DdcManagementClient {
    DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request);
    void delete(DdcManagementConfigDeleteRequest request);
    DdcManagementPublishResult publish(DdcManagementPublishRequest request);
    DdcManagementPublishTask getPublishTask(String changeId);
    DdcManagementPublishResult retry(String changeId);
    List<DdcManagementConfigClientInstance> getConfigClients(DdcInstanceQuery query);
    DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query);
    DdcServiceSnapshot getInstances(DdcServiceKey key);
}
```

Gateway Admin 只能依赖该 Client 或等价公共契约。

删除约束：

- 只有 Gateway 已证明 Config 不再被当前 Active 或仍允许 Retry 的 Release 引用时才能
  删除；
- 删除必须幂等，不存在的 Config 返回成功；
- 请求携带 `expectedVersion`，版本已变化时拒绝；
- 删除操作进入 DDC 审计日志并执行与 Upsert 相同的 HMAC、作用域和权限校验；
- 删除 Draft 不能隐式发布新版本，Gateway 必须先完成新 Activation 发布和保留期检查；
- 删除不向普通 Config Client 下发“空值”或清空字段；Gateway Engine 由自身 Staging
  Retention 清理已无引用 Chunk；
- 被删除的不可变 Chunk Key 永不复用；
- 禁止通过前缀或通配符批量删除，禁止删除 Gateway 当前 Active Key。

## 8. DDC 配置容量保护

DDC Pub/Sub 当前携带完整 Config Value。新增：

```yaml
egon.cola.component.ddc.admin.config.max-value-bytes: 1048576
```

规则：

1. 按 UTF-8 字节数检查，不按 Java 字符数；
2. Create、Update、Publish 都执行相同限制；
3. 超限在写 DB/Redis 前失败；
4. 默认 1 MiB 是安全上限，不代表 Gateway Rule Snapshot 目标容量；
5. Gateway 子 Spec 可以设置更低发布上限；
6. DDC 不在本轮实现透明压缩或分片。

若 Gateway 需要分片，由 Gateway Rule Activation 定义，DDC 仍只管理普通版本化
配置。

## 9. Provider Metadata

DDC 保持当前安全限制：

- 最多 32 个键；
- Key 最长 64；
- Value 最长 512；
- 禁止 Secret/Token/Private Key/Certificate；
- `ddc.*` 与 `egon.internal.*` 保留；
- `egon.rpc.*` 只有框架白名单。

Gateway Provider 公共 Metadata 使用：

```text
gateway.zone
gateway.region
gateway.weight
gateway.tags
gateway.protocol-version
gateway.management-path
gateway.definition-set-id
gateway.artifact-version
gateway.build-id
```

其中：

- `weight` 为 1～10000 的十进制整数；
- `tags` 使用规范化、长度受限的 `key=value` 列表；
- `definition-set-id` 是 GWS-10 的定义集合身份，用于 Admin 判断滚动版本，不参与
  Provider Service Key；
- `artifact-version`、`build-id` 只用于定义/实例关联和诊断，不参与负载均衡；
- Metadata 是 Provider 声明事实，Admin Rule 可以覆盖权重或筛选条件；
- Metadata 不携带鉴权凭据。

`INTERNAL_GATEWAY` 额外允许：

```text
gateway.engine-version
gateway.group-code
```

这些字段是稳定实例能力，不保存每次变化的 Rule Version；动态运行状态通过 GWS-06
Runtime Status 查询。

## 10. RPC Contract Catalog

### 10.1 只读 Catalog

RPC Starter 新增：

```java
public interface RpcContractCatalog {
    List<RpcContractDescriptor> contracts();
    Optional<RpcContractDescriptor> find(RpcServiceIdentity serviceIdentity);
    List<RpcContractSnapshot> snapshots();
    Optional<RpcContractSnapshot> findSnapshot(RpcServiceIdentity serviceIdentity);
}
```

行为：

- 由 `RpcProviderBeanScanner` 完成全部校验后一次性构建；
- 作为不可变 Bean 暴露；
- Catalog 中只包含当前应用实际暴露的 Provider Contract；
- Descriptor 与可序列化 Snapshot 来自同一份校验结果；
- 重复 Service Identity 启动失败；
- Gateway Starter 只能读取，不能修改 Provider Method Registry。

RPC Provider Lifecycle、Server Definition 和 Gateway Starter 使用同一份校验结果，
避免重复扫描产生漂移。

### 10.2 可序列化 Snapshot

新增与 Java 反射对象解耦的模型：

```text
RpcContractSnapshot
├── serviceName
├── group
├── version
├── protoPackage
├── protoServiceName
├── fileDescriptorSet
├── descriptorSha256
└── methods[]
    ├── methodName
    ├── fullMethodName
    ├── requestType
    ├── responseType
    └── rpcType = UNARY
```

`fileDescriptorSet` 是标准 Protobuf `FileDescriptorSet` 的字节内容；JSON 传输时使用
Base64。导出时必须包含重建 Method Descriptor 所需的传递依赖，并按文件名排序后
计算 SHA-256。

Snapshot 不包含：

- Java `Class`；
- Java `Method`；
- Bean Name；
- Provider Host/Port；
- gRPC Channel；
- Secret。

## 11. RPC Provider Metadata Contributor

新增：

```java
@FunctionalInterface
public interface RpcProviderMetadataContributor {
    Map<String, String> contribute(RpcServiceIdentity serviceIdentity);
}
```

规则：

1. 所有 Contributor 按 Spring Order 合并；
2. Key 冲突且 Value 不同则启动失败；
3. Contributor 不能覆盖 `egon.rpc.transport`、
   `egon.rpc.serialization`、`egon.rpc.runtime-version`；
4. 合并结果进入 DDC 原有 Metadata 校验；
5. Gateway 可提供独立 Contributor 写入 `gateway.*`，RPC Component 不依赖
   Gateway；
6. Contributor 抛异常时 Provider 不启动、不注册半成品租约。

## 12. Gateway 可依赖的 RPC 协议

以下保持公共稳定：

- `RpcMetadataKeys`；
- `RpcInvocationMetadata` 的可传输语义；
- `RpcServiceIdentity`；
- `EgonRpcErrorCode`；
- `RpcStatusExceptionMapper` 的状态映射；
- Deadline、Cancellation、Traceparent/Tracestate 语义；
- `serviceName + group + version + fullMethodName` 的路由身份。

RPC Component 不暴露：

- Mock Gateway Handler；
- Mock Provider Directory；
- Mock Channel Cache；
- Mock Round Robin；
- Mock Unary Forwarder。

Gateway Engine 自己实现上述生产能力，并用 RPC Test Contract 做兼容验证。

## 13. 安全

- Management OpenAPI 必须强制 HMAC，生产环境不能关闭；
- Nonce 防重放存储与现有 OpenAPI 保持一致；
- Config Value 和错误信息不得写入普通 Access Log；
- Service Metadata 不允许 Secret；
- Contract Snapshot 只包含 Schema，不包含业务数据；
- Gateway Admin 对 DDC 的 Access Key/Secret 只来自安全配置。

## 14. 兼容与迁移

- 新增 `HTTP_PROVIDER` 不改变现有两个 Service Kind 的 Key；
- 现有 `@DdcValue` 在没有自定义 Applier 时行为不变；
- 周期校准默认开启前必须通过现有 Starter 回归测试；
- 旧管理 UI/API 可以保留，但 Gateway 只使用新 Management OpenAPI；
- RPC Provider 未配置 Contributor 时注册 Metadata 与当前版本一致；
- Contract Catalog 是新增只读能力，不改变 RPC 调用协议；
- 不修改现有 Flyway Migration；需要数据库字段时只能新增一个后续 Migration。

## 15. 测试设计

### 15.1 DDC

1. `HTTP_PROVIDER` 注册、心跳、查询、订阅、过期、注销；
2. 相同 Service Key 多实例；
3. 自定义 Applier 成功后 ACK；
4. 自定义 Applier 失败时版本/checksum 不前进；
5. 精确匹配优先于最长前缀，歧义注册启动失败；
6. 未匹配自定义 Applier 时仍执行原字段绑定；
7. 重复版本幂等与同版本不同 checksum 拒绝；
8. 丢失 Pub/Sub 后周期校准应用新版本；
9. 校准失败保留本地版本；
10. Management API HMAC 成功、签名错误、过期、Nonce 重放；
11. Config 删除幂等、作用域隔离、审计和非法批量删除拒绝；
12. Management DTO 不暴露 Entity 字段；
13. Config Value 超限在 DB/Redis 写入前失败。

### 15.2 RPC

1. Catalog 只包含已校验 Provider；
2. 重复 Service Identity 启动失败；
3. Snapshot 可重建所有 Unary Method Descriptor；
4. Snapshot 文件顺序稳定且 SHA-256 可复现；
5. Contributor 合并顺序、冲突和保留 Key 保护；
6. 无 Contributor 时现有 Metadata 不变；
7. RPC 原有 Provider/Consumer/独立进程测试全部回归。

## 16. 验收标准

1. Gateway 可以注册和发现 `HTTP_PROVIDER`；
2. Gateway Rule Applier 真正生效后才产生成功 ACK；
3. Pub/Sub 丢失可在租约不变的情况下周期收敛；
4. Gateway Admin 不依赖 DDC Entity/Repository；
5. DDC Management 调用全程 HMAC；
6. RPC Starter 提供不可变 Contract Catalog；
7. Contract Snapshot 不含反射对象且可重建 Descriptor；
8. RPC Provider 可以安全贡献 `gateway.*` Metadata；
9. RPC/DDC 均不包含生产 Gateway 路由、负载均衡或治理实现；
10. DDC 单 Admin/单 Redis 和 RPC 单 Gateway 约束保持不变。

## 17. 本轮审核项

1. 认可 `HTTP_PROVIDER` 作为 DDC 第三个 Service Kind；
2. 认可 Config Key 精确/最长前缀 Applier + 字段绑定回退；
3. 认可 30 秒默认配置校准和 full-value 容量保护；
4. 认可新增 HMAC Management OpenAPI/Client；
5. 认可 RPC 暴露只读 Catalog 和标准 FileDescriptorSet Snapshot；
6. 认可通过 Contributor 扩展 RPC Provider Metadata；
7. 认可所有 Gateway 生产数据面能力仍留在 Gateway Component。
