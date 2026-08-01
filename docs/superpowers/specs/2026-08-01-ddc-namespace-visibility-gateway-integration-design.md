# DDC Namespace 可见性、运行时身份与 Gateway 联调设计

## 1. 背景

当前 DDC 数据模型实际是 `biz -> app -> namespace`，运行时服务注册和配置身份均包含
namespace。这个模型无法表达以下已确认的业务语义：

- namespace 是集团管控与授权可见性分组，不是应用部署归属；
- 一个应用及其配置、服务实例只存在一份；
- 同一个应用可以在不同 namespace、不同环境组合下可见；
- 用户后续按 namespace 授权后，只看到该 namespace 在指定环境绑定的应用；
- 管理页面按 `biz -> namespace -> env -> app` 浏览；
- Gateway Admin 与 Gateway Engine 分别使用 `infra/ga`、`infra/ge`；
- 服务目录和配置列表允许任意筛选条件，缺少条件不能返回内部错误；
- 两个相同的无状态服务副本属于同一个逻辑服务，但必须具有不同实例身份。

当前 Gateway Admin Web 的 Refresh Token 输入框还会误导本地联调。本地 HMAC JWT
模式没有 OAuth Token Endpoint，不能生成或刷新 Refresh Token。

## 2. 目标与非目标

### 2.1 目标

1. 将 namespace 改成 biz 下的管控实体。
2. 使用环境级关联实体表达 `namespace + env + app` 多对多可见关系。
3. 将物理服务注册身份和配置身份中的 namespace 移除，保证应用只注册、部署、配置一次。
4. 明确逻辑服务、服务实例和租约会话三层身份。
5. 修复 DDC 服务目录和配置列表的可选筛选行为。
6. 让 DDC Web 按 `biz -> namespace -> env -> app` 浏览，并能查看应用的真实实例。
7. 让 Gateway Admin、Gateway Engine 正确注册到 DDC，并完成本机完整联调。
8. 简化 Gateway Admin Web 本地登录，只要求 Access Token。

### 2.2 非目标

- 本次不实现用户、角色、账号和完整 RBAC；只提供 namespace 授权所需的数据边界和查询接口。
- 不通过复制注册、复制配置或重复部署实现 namespace 可见性。
- 不使用 MAC、IP 或 PID Hash 作为默认实例 ID。
- 不引入 Redis Key 全量 `SCAN` 作为服务目录常规查询方案。
- 不修改现有 V1-V6 Flyway 文件。
- 不使用 Docker 或 Testcontainers 代替本机 PostgreSQL、Redis 联调证据。

## 3. 核心领域模型

### 3.1 管控层级

```text
biz
├─ namespace-a
│  ├─ dev
│  │  ├─ app-1
│  │  └─ app-2
│  └─ prod
│     └─ app-1
└─ namespace-b
   └─ prod
      └─ app-1
```

`namespace-a/prod/app-1` 与 `namespace-b/prod/app-1` 指向同一个物理应用。两条关联只控制
可见性，不产生两份配置、两份注册或两组实例。

### 3.2 物理身份

```text
应用身份：     biz + app
部署身份：     biz + env + app
配置身份：     biz + env + app + configKey
逻辑服务身份： biz + env + app + serviceKind + protocol
               + serviceName + group + version
可见性身份：   namespace + env + app
```

namespace 不参与配置、服务注册、心跳、路由发现和实例租约的物理身份。

### 3.3 唯一约束

| 实体 | 唯一键 |
|---|---|
| biz | `biz_code` |
| env | `env_code` |
| app | `(biz_code, app_code)` |
| namespace | `(biz_code, namespace_code)` |
| namespace-env-app binding | `(namespace_id, env_code, app_id)` |
| config item | `(biz_code, env, app_code, config_key)` |

## 4. 服务与实例身份

### 4.1 身份分层

| 字段 | 语义 | 生成方 | 生命周期 |
|---|---|---|---|
| `serviceKey` | 结构化逻辑服务身份 | Starter/Provider Runtime | 服务契约不变时稳定 |
| `serviceId` | `serviceKey` 的 SHA-256 派生摘要 | DDC | 与 `serviceKey` 同生命周期 |
| `instanceId` | 一个实际运行副本的身份 | Starter | 进程或 Pod 生命周期 |
| `leaseId` | 本次注册会话的 fencing token | DDC Admin | 单次成功注册会话 |
| `host:port` | 实际访问地址 | 服务实例 | 可随实例变化 |

两个相同订单服务副本共享同一个 `serviceKey/serviceId`，但具有不同 `instanceId` 和
`leaseId`。DDC 先按 `serviceKey` 聚合逻辑服务，再在该服务下返回多个实例。

### 4.2 serviceId

`serviceId` 不由客户端随机生成，也不使用主机信息。DDC 根据规范化后的物理
`serviceKey.canonicalValue()` 计算完整 SHA-256：

```text
serviceId = SHA-256(serviceKey.canonicalValue())

serviceKey.canonicalValue():
  ddc-service-key-v3\n
  bizCode\n
  env\n
  appCode\n
  serviceKind\n
  protocol\n
  serviceName\n
  group\n
  version
```

canonicalValue 使用固定字段顺序和换行分隔；构造时继续拒绝字段中的 CR/LF，因此不存在字段
边界歧义。首行版本标识防止后续协议演进复用旧摘要口径。

- 相同逻辑服务的所有副本得到相同 `serviceId`；
- 不同环境、应用、版本、协议、group 或 serviceName 得到不同 `serviceId`；
- `serviceId` 是只读派生字段，不替代结构化 `serviceKey`，不增加数据库状态；
- API 返回完整摘要，Web 默认显示前 12 位并支持复制完整值；
- Redis V3 Key 复用同一摘要算法，避免生成两个口径。

### 4.3 instanceId 生成策略

新增共享 `DdcInstanceIdProvider` Strategy，DDC Config Client、HTTP Provider、RPC Provider、
Gateway Admin 和 Gateway Engine 统一使用。优先级固定为：

1. 显式配置 `egon.cola.component.ddc.instance.id`；
2. 业务提供的自定义 `DdcInstanceIdProvider` Bean；
3. Starter 默认生成完整 UUIDv7。

默认 ID 不包含 MAC、IP、PID，不截断 UUID。显式配置适用于 Kubernetes Pod UID、ECS
Task ID 等编排器已经保证唯一的身份。无状态实例重启默认生成新 `instanceId`，旧实例由
lease TTL 清理。

MAC Hash 不作为默认策略，因为同机多进程会冲突，容器虚拟 MAC 会变化，多网卡选择不确定，
VM 克隆可能重复，快速重启还会与旧租约冲突。业务若确有裸机场景，可以通过自定义
`DdcInstanceIdProvider` 实现，但必须自行保证唯一性和滚动发布安全。

## 5. 数据库模型与 V7 迁移

### 5.1 表结构

`ddc_namespace` 改成 biz 下的独立实体：

```text
id, biz_code, namespace_code, namespace_name,
description, enabled, created_at, updated_at
```

新增关联实体 `ddc_namespace_env_app`：

```text
id, namespace_id, env_code, app_id,
enabled, created_at, updated_at
```

关联实体而不是普通多对多集合，因为关系自身携带 env、enabled 和审计时间。该设计采用
Association Object 模式，直接对应用户后续以 namespace 为授权单元的需求。

配置当前态表补充 `biz_code`，唯一键改为：

```text
(biz_code, env, app_code, config_key)
```

配置版本、发布任务、ACK、配置客户端实例和操作日志补充 biz 维度。既有 namespace 字段在
历史表中作为 legacy snapshot 保留并允许为空，新写入不再把它当作运行时身份。

### 5.2 Flyway 规则

- 只新增一个版本号 `V7`；
- 按当前项目惯例提供 PostgreSQL 和 SQLite 方言文件；
- 不编辑、重命名或格式化 V1-V6；
- PostgreSQL 是本机和生产验证目标，SQLite 保持既有仓库测试能力。

### 5.3 旧数据迁移

1. 从旧 `ddc_namespace.app_code` 找到 app 及其 biz。
2. 同一 biz 中 namespaceCode 相同的旧记录合并为一个 namespace 实体。
3. 对旧 app-namespace 关系和当前所有 env 实体生成 `namespace + env + app` 绑定，保持旧模型
   中 namespace 与 env 正交的可见语义。
4. 配置表通过 app 回填 bizCode。
5. 升级前检查是否存在多个旧 namespace 下相同 `(biz, env, app, configKey)` 的配置行。
6. 只要存在重复物理配置键，V7 就中止并报告冲突，不自动覆盖、挑选或删除数据。

迁移失败必须保持事务回滚，用户先解决冲突后重新执行。当前本机数据在实际迁移前也要运行
同一条预检查查询。

## 6. DDC Starter 与运行时协议

### 6.1 配置协议

配置读取、订阅、发布和 ACK 的作用域统一为：

```text
bizCode + env + appCode
```

Redis 使用新的 V3 Key：

```text
ddc:v3:{scopeDigest}:config:{configKey}
ddc:v3:{scopeDigest}:version:{configKey}
ddc:v3:{scopeDigest}:topic
ddc:v3:{scopeDigest}:lease:instance:{instanceId}
```

`egon.cola.component.ddc.namespace` 保留一个版本作为 deprecated 配置，启动时记录迁移提示，
但不再参与配置拉取或注册。所有仓库内消费者在同一次发布中完成升级并重启。

### 6.2 服务注册协议

V3 `DdcServiceKey` 字段顺序固定为：

```text
bizCode, env, appCode, serviceKind,
serviceName, group, version, protocol
```

注册、心跳、下线和实例查询不再要求 namespace。`DdcServiceRegistration` 继续携带
`instanceId + serviceKey + host + port + metadata + lease settings`。

旧 V2 Redis 租约按 TTL 自然过期；V3 目录不读取 V2 Key。部署时必须先升级 DDC Admin，
再统一重启 Starter、Gateway、RPC 和 Provider 进程。

### 6.3 运行时门控

注册和配置拉取只校验 biz、env、app 是否存在且启用。namespace binding 只控制管理面查询和
授权，不影响已经运行的应用。删除或禁用一条 namespace binding 不会终止服务或使配置拉取失败。

## 7. 服务目录与可选筛选

### 7.1 全局目录

新增持久化 Redis V3 全局服务目录 Set 和 revision：

```text
ddc:v3:{registry-catalog}:services
ddc:v3:{registry-catalog}:revision
```

注册和成功心跳都会补写全局目录，因此 DDC Admin 重启后能由存活客户端自愈。下线和过期清理
在最后一个实例消失后删除目录项。全局目录使用独立 Hash Tag，不把它塞入现有跨 scope Lua
脚本，避免 Redis Cluster `CROSSSLOT`。

完整物理条件查询继续使用 scope 精确 catalog；缺少任意条件时读取全局目录并通过
`DdcServiceQuery.matches` 过滤。禁止使用常规 Redis Key `SCAN`。

### 7.2 查询接口

`GET /api/v1/ddc/registry/services` 的以下参数全部可选：

```text
bizCode, namespaceCode, env, appCode,
serviceKind, protocol, serviceName, group, version
```

- 不传参数返回所有存活服务；
- 传什么筛什么；
- namespaceCode 通过 SQL binding 与物理目录求交集；
- 未知筛选值返回空 services；
- serviceId 在响应中派生返回；
- 过期且无实例的服务不会返回。

`GET /api/v1/ddc/registry/instances` 是精确操作，要求完整物理 serviceKey，不要求 namespace。
缺少精确身份返回 `DDC_INVALID_REQUEST`，不能包装成 `DDC_INTERNAL_FAILURE`。

## 8. 配置列表与管理 API

### 8.1 级联读取

管理面统一使用：

```text
GET /api/v1/ddc/namespaces?bizCode=infra
GET /api/v1/ddc/envs?bizCode=infra&namespaceCode=default
GET /api/v1/ddc/apps?bizCode=infra&namespaceCode=default&env=local
```

- namespace 按 biz 查询；
- env 查询返回该 namespace 已存在 binding 的环境；
- app 查询返回指定 namespace/env 绑定的应用；
- namespace binding 编辑器加载全部全局 env 和指定 biz 的 app 作为候选项。

### 8.2 Binding 管理

新增 `/api/v1/ddc/namespace-env-app-bindings`：

- GET：按 bizCode、namespaceCode、env、appCode 任意过滤；
- POST：创建一条环境级绑定；
- DELETE：删除一条绑定；
- PUT enabled：启用或禁用绑定。

重复绑定返回稳定的 `DDC_NAMESPACE_BINDING_EXISTS`；不存在返回
`DDC_NAMESPACE_BINDING_NOT_FOUND`。删除 binding 不删除 app、配置、实例或注册信息。

### 8.3 配置列表

`GET /api/v1/ddc/configs` 支持以下可选条件：

```text
bizCode, namespaceCode, env, appCode, configKey, includeDeleted
```

- 完全不传返回所有配置；
- namespaceCode 通过 binding `EXISTS` 子查询限制可见 app；
- 同一配置绑定多个 namespace 时，无 namespace 筛选只返回一行；
- 响应附带 `visibleNamespaces`，用于 Web 展示标签；
- 创建和更新的物理身份只包含 biz/env/app/configKey；
- 管理请求可携带 namespaceCode 作为访问上下文，但不写入配置唯一键；
- 将来接入 RBAC 时，访问上下文与当前用户可访问 namespace 求交集。

## 9. DDC Admin Web

### 9.1 统一作用域组件

`ScopeSelects` 顺序改成：

```text
BizSelect -> NamespaceSelect -> EnvSelect -> AppSelect
```

上级改变时清空下级。列表筛选允许任意层级为空；新建 binding 和配置时才校验需要的完整上下文。

### 9.2 Namespace 管理页

- namespace 创建时选择 biz，不选择 app；
- 增加 binding 管理区；
- 按 env 分组，通过 app 多选维护 `namespace + env + app`；
- 删除 namespace 前只检查 binding；有 binding 时返回 `DDC_NAMESPACE_IN_USE`；
- 删除 binding 不影响物理数据。

### 9.3 服务注册页

- 初次加载不注入默认 biz/app/env/namespace；
- 支持 `biz -> namespace -> env -> app` 逐级筛选；
- 主列表按 binding path 显示，因此同一物理 app 可出现在多个 namespace 视图；
- 概览中的物理应用数按 appId 去重；
- 点击任意 namespace 下的 app，实例 Drawer 使用服务响应自身的完整物理 serviceKey；
- 同一 app 在两个 namespace 下打开时，serviceId、instanceId 和实例集合完全相同；
- Drawer 展示 serviceId、service kind/name/group/version、实例状态、instanceId、host:port、
  最近心跳和过期时间。

### 9.4 配置页

- 任意空筛选可查询；
- 同一物理配置只显示一行；
- visibleNamespaces 使用标签展示；
- 从任一绑定 namespace 打开配置时读取相同 configId、版本和值。

## 10. Gateway Admin Web 登录

本地 HMAC JWT 模式没有 Refresh Token。登录页根据以下条件决定是否展示 Refresh Token：

```text
VITE_GATEWAY_ADMIN_TOKEN_URL 与 VITE_GATEWAY_ADMIN_CLIENT_ID 均存在
```

未配置时：

- 隐藏 Refresh Token 输入框；
- 显示“本地模式仅需 Access Token”；
- 登录只提交 accessToken；
- 重启验收环境时生成新的 `target/local-biz-app-run/admin.jwt`；
- 不在日志、终端输出或文档中打印 Token 明文。

配置 OAuth Token Endpoint 时保留现有可选 Refresh Token 和自动刷新逻辑。

## 11. Gateway Admin 与 Engine 注册

本机联调初始化以下数据：

```text
biz: infra
namespace: default
env: local
apps:
  - ga / Gateway Admin
  - ge / Gateway Engine
bindings:
  - infra/default/local/ga
  - infra/default/local/ge
```

Gateway Admin：

- 物理部署身份为 `infra/local/ga`；
- 复用现有 HTTP Provider lease runtime，不重复实现心跳恢复；
- serviceName 为 `egon-cola-gateway-admin`；
- serviceKind 为 `HTTP_PROVIDER`；
- metadata 包含 `gateway.component=admin`；
- HTTP 监听端口就绪后注册。

Gateway Engine：

- 物理部署身份为 `infra/local/ge`；
- 保持 `INTERNAL_GATEWAY`；
- serviceName 为 `egon-gateway-rpc`；
- Engine Ready 后注册；
- namespace 不再进入 `RpcGatewaySlotRuntime` 的 serviceKey。

把 ga/ge 绑定到第二个 namespace 只新增 binding，不需要重启或重新注册。

## 12. 本机测试服务

除 Gateway Admin 和 Engine 外，启动两个相同的无状态订单 HTTP Provider：

```text
biz: retail
namespace: default
env: local
app: order
serviceName: order-service
ports: 18084, 18085
```

两个进程共享 serviceKey/serviceId，具有不同 UUIDv7 instanceId、leaseId 和端口。Gateway 路由
通过同一逻辑服务发现两个实例，并可重复请求观察负载选择。再把 order 绑定到第二个 namespace，
验证不重启 Provider 也能从新 namespace 看见相同实例集合。

## 13. 错误处理

| 场景 | 结果 |
|---|---|
| 服务目录缺少筛选 | 正常返回匹配服务或全部服务 |
| 配置列表缺少筛选 | 正常返回匹配配置或全部配置 |
| 未知筛选值 | 成功响应，data 为空 |
| 精确实例查询缺少 serviceKey 字段 | `DDC_INVALID_REQUEST` |
| serviceKind 非法 | `DDC_INVALID_REQUEST` |
| binding 重复 | `DDC_NAMESPACE_BINDING_EXISTS` |
| binding 不存在 | `DDC_NAMESPACE_BINDING_NOT_FOUND` |
| V7 发现旧配置物理键冲突 | Flyway 中止并保留旧数据 |
| instanceId 与存活旧租约冲突 | `DDC_INSTANCE_ID_CONFLICT` |
| 未预期异常 | 记录 traceId，返回 `DDC_INTERNAL_FAILURE` |

Spring MVC 参数绑定和枚举转换错误必须映射到 `DDC_INVALID_REQUEST`，不能继续落入通用 56999。

## 14. 兼容性与发布顺序

这是一次明确的运行时作用域契约升级：namespace 从物理身份改为管理可见性。发布顺序固定为：

1. 停止当前本机 DDC/Gateway/Provider 业务进程，保留 PostgreSQL、Redis 数据服务；
2. 执行 V7 配置冲突预检查；
3. 构建并启动新 DDC Admin，完成 V7；
4. 创建或确认 infra/default/local/ga、ge bindings；
5. 重启 Gateway Admin、Gateway Engine、两个订单 Provider；
6. 启动 DDC Web 和 Gateway Web；
7. 等待 V2 租约 TTL 收敛，只读取 V3 目录；
8. 执行 API、Web 和 Gateway 路由验收；
9. 服务保持运行，交由用户继续测试。

仓库外使用旧 Starter 的应用必须同步升级。旧客户端缺少 V3 物理身份，不能与新目录混用。

## 15. 设计模式取舍

- 使用 Association Object 表达 namespace-env-app，因为关系携带 env、enabled 和审计属性；
- 使用 Strategy 表达 `DdcInstanceIdProvider`，因为 Kubernetes、ECS、裸机和默认 UUIDv7 是真实变化点；
- Gateway Admin 复用现有 HTTP Provider lease runtime，沿用既有 Adapter/lease recovery 边界；
- 不增加 MAC Factory、重复注册 Facade 或“主 namespace”别名层；这些抽象会制造错误身份或重复状态；
- 可选筛选使用直接、可测试的 repository 查询和 binding `EXISTS`，不为简单条件树新增处理器链。

## 16. 测试与验证

### 16.1 后端

- V7 PostgreSQL/SQLite 迁移与冲突拒绝；
- namespace 归 biz、binding 唯一约束、启禁用和保护性删除；
- 配置身份不含 namespace，同一绑定应用只保存一份配置；
- serviceKey V3 canonical、serviceId SHA-256 往返与稳定性；
- UUIDv7 默认 instanceId、显式覆盖、自定义 Provider 优先级；
- 两个相同 serviceKey 的实例能同时注册、独立心跳和下线；
- leaseId fencing 和 instanceId 冲突保持严格；
- 服务目录全空、部分条件、完整条件和 namespace binding 求交；
- 配置列表全空、部分条件、namespace 过滤和去重；
- Spring 参数错误映射为 `DDC_INVALID_REQUEST`；
- Gateway Admin HTTP lease、Engine INTERNAL_GATEWAY lease 与故障恢复。

### 16.2 前端

- ScopeSelects 的 biz -> namespace -> env -> app 级联；
- namespace binding 管理；
- Registry 首次全量、部分筛选、binding path 和实例 Drawer；
- 同一 app 多 namespace 时实例集合一致；
- Configs 空筛选、可见 namespace 标签和配置去重；
- Gateway 本地登录隐藏 Refresh Token；
- OAuth 配置存在时 Refresh Token 行为回归。

### 16.3 验证命令范围

- DDC Starter/Admin/Test reactor Maven 测试；
- RPC 和 Gateway 受影响模块 Maven 测试；
- DDC/Gateway Web Vitest、typecheck、lint、build；
- 根 reactor 受影响模块集成验证，并检查 reactor summary 确认实际子模块和测试被执行；
- 本机 PostgreSQL、Redis 多进程联调，不使用容器。

## 17. 验收标准

1. `GET /registry/services` 无参数、任意部分参数均成功，不返回 56999。
2. `GET /configs` 无参数、任意部分参数均成功，结果符合所传条件。
3. DDC Web 能按 `biz -> namespace -> env -> app` 浏览。
4. 同一 app 可绑定多个 namespace/env，但只有一份配置和一组物理实例。
5. 两个相同订单服务副本共享 serviceId，instanceId、leaseId、host:port 不同且均 ONLINE。
6. `infra/default/local` 下能看到 ga、ge；ga 注册为 Gateway Admin HTTP 服务，ge 注册为
   INTERNAL_GATEWAY。
7. 给 ga、ge 或 order 增加第二个 namespace binding 后，不重启服务即可看到相同实例。
8. 两个 namespace 打开同一配置时，configId、版本和值一致。
9. Gateway Web 本地登录只要求 Access Token，Refresh Token 不显示。
10. 新 Access Token 写入受控运行目录且不输出明文。
11. HTTP Provider 经 Gateway 路由可用，两个订单实例均可被发现。
12. 所有目标 Maven、Vitest、typecheck、lint、build 验证通过；任何既有非本次错误单独说明。
13. DDC、Gateway Admin、Gateway Engine、两个订单 Provider、DDC Web、Gateway Web 重启后保持运行，
    供用户继续测试。
