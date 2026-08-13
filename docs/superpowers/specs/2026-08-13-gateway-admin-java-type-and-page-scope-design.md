# Gateway Admin 领域分包、Java 类型独立化与页面级 Scope 改造规格

> 状态：已确认，进入实施计划阶段
> 编写日期：2026-08-13
> 代码基线：`main@e61ec1e7`
> 适用仓库：`/Users/mario/SelfProject/Egon-COLA`

主要涉及模块：

- `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin`
- `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web`

本文固化已经确认的三项改造：

1. Gateway Admin Java 包改为领域优先的垂直结构，每个领域内部按 `controller / domain / repository / service` 展开；
2. Gateway Admin 生产源码不再包含任何嵌套 `record`、`class`、`enum` 或 `interface`，现有 165 个嵌套类型全部独立为顶层 Java 文件；
3. Gateway Admin Web 不再把 `bizCode / namespace / env / appCode` 作为贯穿全站的全局查询上下文，改为各页面独立筛选和独立查询。

本次不删除或修改认证接口，不让前端解析 JWT，也不修改任何既有 HTTP 接口契约。

---

## 1. 决策摘要

| 编号 | 已确认决策 |
|---|---|
| GA-01 | `GatewayAuthBootstrapController` 和 `GatewayAdminSessionController` 保留，路径、入参、响应和鉴权行为不变 |
| GA-02 | Gateway Admin Web 继续通过现有 Session API 获取身份和 capabilities，不解析 JWT |
| GA-03 | Spring Security、IdP、RBAC3、`AdminActor` 和后端权限校验不在本次改造范围 |
| GA-04 | 外部 HTTP 契约冻结；Java 内部类型名称、包位置和方法签名允许因类型独立化而调整 |
| GA-05 | Gateway Admin 全部生产 Java 类中不得继续声明嵌套 `record`、`class`、`enum` 或 `interface` |
| GA-06 | 独立数据载体允许继续使用顶层 `record`；本次不把不可变数据载体机械改写为可变 JavaBean |
| GA-07 | 不建立通用 `pojo`、巨型 `dto` 或跨功能 `model` 垃圾包；类型放回其所属接口或业务功能包 |
| GA-08 | Gateway Web 删除全局选中 Scope、Header Scope 选择器、Scope LocalStorage 和切换 Scope 时的全局缓存清理/跳转 |
| GA-09 | DDC Scope Binding 继续作为页面筛选选项和创建表单的权威数据源，但不再承担全站当前上下文 |
| GA-10 | 能用现有接口查询全量数据的页面默认展示跨 Scope 数据；仍要求 Scope 参数的接口由对应页面独立选择后查询 |
| GA-11 | 页面级筛选写入本页面 URL Query，不跨页面共享，不写入 LocalStorage |
| GA-12 | 本次不以浏览器遍历全部 Scope、并发请求后再合并的方式伪造全局聚合或全局分页 |
| GA-13 | Java 包采用领域优先结构：`admin.<领域>.controller/domain/repository/service` |
| GA-14 | `domain` 下按实际需要建立 `dto / vo / po / enums / exception`，不创建空包 |
| GA-15 | 不再保留承载全部业务的 `interfaces / infrastructure / persistence` 技术根包；`admin.application` 仅表示 Gateway Application 领域，不再作为全局 Application Layer |
| GA-16 | DTO、VO、PO 和 Enum 使用明确后缀；真实领域对象可直接位于本领域 `domain` 包 |
| GA-17 | Repository 实现按 `jdbc / jpa / filesystem` 继续细分，仓储接口和实现不得散落在 Service 包 |
| GA-18 | `src/main/java` 中 165 个嵌套类型全部清零；私有辅助类型也必须成为消费者同包下的包级顶层类型 |
| GA-19 | MCP 保持为一个完整领域，本次不为目录整齐而强拆 `McpControlPlaneService` |

---

## 2. 背景与现状问题

### 2.1 Java 类型与宿主职责混杂

当前 `gateway-admin` 中，Controller、Service、Store、Repository 实现、配置、消息消费者、规则工具和异常处理器除了自己的主要职责，还在文件尾部声明 Request、Response、Command、Query、View、Result、PO、枚举、校验结果和内部计算状态。

基线扫描结果为：

- 59 个生产 Java 文件存在嵌套类型；
- 共 165 个嵌套类型；
- 其中 149 个为 `record`，10 个为 `class`，6 个为 `enum`；
- 原 Controller、Service、ExceptionHandler 范围是其中 87 个；
- 其余 78 个分布在 Store、JDBC 实现、配置、领域对象、规则、消息消费和定时任务中。

这产生以下问题：

1. API DTO 被迫依附 Controller 名称，其他消费者必须引用 `Controller.NestedType`；
2. Application Command/View 被迫依附 Service 名称，调用方与服务实现文件形成不必要的编译耦合；
3. 单个 Service 文件同时承担流程、协议和大量数据结构定义，类文件过长且变更噪声大；
4. 同名的 `CreateRequest`、`MutationRequest`、`MutationControl` 只能依赖嵌套作用域区分，难以表达真实语义；
5. 类型不能被单独检索、评审和编写有意义的 JavaDoc；
6. 后续继续向任意生产类塞嵌套数据类型时缺少自动化阻断。

本次解决的是类型归属和源码组织问题，不借机改变业务模型。

### 2.2 当前包结构以技术层为中心

当前源码同时存在：

```text
application
domain
infrastructure
interfaces
mcp/application
mcp/interfaces
mcp/persistence
rule
security
```

同一业务领域被拆散在多棵技术目录中。例如 Gateway Catalog 的 Controller 位于 `interfaces.management`，Service/Store 位于 `application.catalog`，JDBC 实现位于 `infrastructure.persistence`；查看一次完整调用链必须跨三个根包。

新结构以业务领域作为第一层边界，技术职责作为领域内部第二层。`bootstrap` 和 `config` 是模块装配例外，不承担业务对象归属。

### 2.3 全局 Scope 把查询条件错误提升为应用上下文

当前 Gateway Web 的数据流为：

```text
ScopeProvider
  -> 加载全部 DDC Scope Binding
  -> 从 LocalStorage / 环境变量 / connected binding 选择一个完整 Scope
  -> Admin Header 展示 Biz / Namespace / Env / App 四级选择器
  -> 所有页面通过 useScope() 读取同一个 Scope
  -> 切换后清除绝大多数 React Query 缓存并跳转 Dashboard
```

因此一个页面的查询条件会同时控制 Dashboard、Application、Catalog、Provider、Trace、Audit 和 MCP。用户无法在不同页面保留不同的查询意图，也容易误以为系统只存在当前 Scope 下的数据。

更具体地：

- `ApplicationsPage` 即使后端支持空查询返回全部 Application，前端仍强制附加完整 Scope；
- `CatalogPage` 只能从全局 Scope 下的 Application 中选择；
- MCP 页面通过全局 Scope 缩小 Group 或 Operation 选项；
- Header 切换任一字段会清空其他页面缓存并跳回 `/dashboard`；
- DDC Binding 加载失败或没有 Binding 时，`ScopeProvider` 会阻断整个已登录应用；
- 默认 Scope 环境变量和 LocalStorage 把一次查询偏好变成了全站状态。

### 2.4 现有接口对“跨 Scope”的支持能力不同

本次冻结后端 HTTP 接口后，必须忠实现有能力：

| 页面/数据 | 当前接口能力 | 本期可实现的行为 |
|---|---|---|
| Gateway Group | `GET /gateway-groups` 已返回全部 Group，额外 Scope 参数实际未参与后端过滤 | 默认展示全部，可在页面内按 Env/Namespace 过滤 |
| Application | `GET /applications` 的四个 Scope 参数均可选，空查询返回全部 | 默认展示全部，页面可选过滤 |
| Catalog | 由 Application ID 查询目录 | 先从全部/页面筛选后的 Application 选择，再查目录 |
| MCP | 主要以 Gateway Group ID 或 Server ID 查询 | 先从全部/页面筛选后的 Group 选择，不依赖全局 Scope |
| Dashboard | 必须提供 Biz/App/Env/Namespace | 页面独立选择完整 Scope 后查询，不能一次返回全局聚合 |
| Provider | 必须提供 Biz/App/Env/Namespace | 页面独立选择完整 Scope 后查询，不能一次返回全部 Scope |
| Trace | 必须提供 Env/Namespace | 页面独立选择 Env/Namespace 后查询 |
| Audit | 必须提供 Env/Namespace | 页面独立选择 Env/Namespace 后查询 |

本期“解决全局问题”的准确含义是：Scope 不再是跨页面共享的应用上下文，每个页面自行决定是否需要以及如何使用筛选条件。它不等同于在接口冻结时为所有页面新增后端全局查询能力。

---

## 3. 目标与非目标

### 3.1 目标

1. 将 Gateway Admin Java 源码改为领域优先、领域内部技术分层的包结构；
2. 清除 Gateway Admin `src/main/java` 中全部 165 个嵌套类型声明；
3. 为每个 Request、Response、Command、Query、View、Result、PO、枚举和内部计算对象建立独立 Java 文件；
4. 保持字段、校验注解、不可变性、构造校验、JSON 名称和现有业务行为；
5. 让 Controller 只负责协议适配、校验、鉴权声明和 Service 调用；
6. 让 Service 只负责业务流程编排，不在文件尾部兼任类型容器；
7. 让 Repository 契约、持久化实现和 PO 位于所属领域的明确目录；
8. 增加结构回归测试，阻止嵌套类型和旧技术根包回流；
9. 删除 Gateway Web 全局 Scope Context 和 Header Scope 选择器；
10. 让 Gateway Group、Application、Catalog 和 MCP 默认能看到跨 Scope 候选数据；
11. 让 Dashboard、Provider、Trace 和 Audit 在各自页面独立选择现有接口所要求的 Scope；
12. 让查询键、筛选状态、缓存失效和页面跳转都局限在所属页面；
13. 让前端 DDC Binding 查询失败不再阻断应用壳和与该查询无关的页面；
14. 更新单元测试、前端说明和相关 E2E 测试夹具源码，使其与新页面结构一致。

### 3.2 非目标

- 不删除、重命名或修改任何 Gateway Admin HTTP 接口；
- 不改变请求字段、响应 JSON、HTTP 状态码、错误码和权限注解；
- 不删除或停用 `GatewayAuthBootstrapController`；
- 不删除或停用 `GatewayAdminSessionController`；
- 不修改 Gateway Web 的登录、刷新、Session API 或 capability 行为；
- 不让 Gateway Web 或 Admin Web Shared 新增 JWT 身份解析职责；
- 不修改 IdP、RBAC3、DDC 的认证授权模型；
- 不修改数据库，不增加 Flyway migration；
- 不改变 Gateway Application 的物理身份或 DDC Binding 校验语义；
- 不为 Dashboard、Provider、Trace、Audit 新增跨 Scope 后端查询；
- 不在前端并发遍历多个 Scope 后合并分页、排序或聚合数据；
- 不引入 ArchUnit、ClassGraph、新状态管理框架或新 UI 框架；
- 不进行与本次分包、类型归位和 DTO/VO/PO/Enum 规范无关的 Service 拆分、领域重构或命名清洗；
- 不为了满足目录形式而新增空接口、Facade、Factory 或无业务价值的抽象；
- 不在本期拆分 `McpControlPlaneService`、改变跨领域工作流或新增模块；
- 不启动项目，不执行浏览器或运行态联调。

---

## 4. 领域分包与 Java 类型独立化规范

### 4.1 强制规则

实施完成后，Gateway Admin 的整个 `src/main/java` 必须不存在嵌套类型。所有生产类都必须满足 `getDeclaredClasses().length == 0`，编译产物目录中不得出现业务源码生成的 `$` 内部类文件。

禁止的嵌套声明包括：

```text
record / class / enum / interface
```

无论其可见性是 `public`、包级、`protected` 或 `private`，只要源码声明了上述类型，都必须独立。包括：

- Request、Response、Command、Query、Mutation、View、Result；
- JPA Entity、数据库 Record、持久化快照和状态枚举；
- 配置属性、消息消费状态、规则编译状态和定时任务上下文；
- JDBC Row、Filter、Mutable Assembler 等实现辅助类型。

私有实现辅助类型迁移后保持包级可见性，不因为独立文件而扩大公共 API。

### 4.2 类型形式

- 现有不可变 `record` 迁移为顶层 `record`，不改变 component 顺序和类型；
- 现有 `enum` 迁移为顶层 `enum`；
- 现有嵌套 `class` 迁移为独立顶层 `class`；配置属性和 JPA PO 保留框架所需的可变性、构造器和访问器；
- 原私有辅助类型迁移为消费者所在 `service`、`repository.jdbc` 或 Controller 子包中的包级类型，只有真实消费者需要跨包时才提升为 `public`；
- 不为满足“POJO”字面形式而添加 setter、无参构造器或 Lombok；
- 构造器中的 `List.copyOf`、`Set.copyOf`、必填校验和归一化逻辑原样保留；
- Jakarta Validation 注解继续放在 HTTP Request record component 上；
- Jackson 可见字段名不得因 Java 类型重命名而变化。

### 4.3 领域优先包模板

业务领域作为第一层，技术职责作为领域内部第二层：

```text
admin.<领域>
├── controller
├── domain
│   ├── dto
│   ├── vo
│   ├── po
│   ├── enums
│   └── exception
├── repository
│   ├── jdbc
│   ├── jpa
│   └── filesystem
└── service
```

规则：

- 只创建实际有类型的目录，不建立空的 `dto / vo / po / enums / exception`；
- 不新建模块级通用 `pojo / dto / vo / po / model` 总包；
- `bootstrap` 和 `config` 是模块装配例外，不承载业务数据模型；
- 一个顶层类型对应一个 `.java` 文件；
- 第一层不再保留 `interfaces / infrastructure / persistence` 技术分层目录；`application` 只作为 Gateway Application 业务领域存在。

### 4.4 类型分类与命名

| 目录 | 放置内容 | 命名要求 |
|---|---|---|
| `domain/dto` | HTTP 请求、Command、Query、Mutation、Control、跨层输入 | `DTO` 后缀 |
| `domain/vo` | HTTP 响应、View、Projection、Result、Report、Page | `VO` 后缀 |
| `domain/po` | JPA Entity、数据库记录、持久化快照 | `PO` 后缀 |
| `domain/enums` | 业务、协议和持久化状态枚举 | `Enum` 后缀 |
| `domain/exception` | 当前领域异常 | `Exception` 后缀 |
| `domain` | 有行为的领域对象、状态机、Revision、业务 Key | 使用真实业务名称，不强加 DTO/VO 后缀 |
| `repository/jdbc` | JDBC 实现及其 Row、SQL Filter、Mutable Assembler | 辅助类型使用完整语义名并保持包级可见 |
| `service` | Service 及只服务于该包编排/算法的计算状态 | 辅助类型使用完整语义名并保持包级可见 |

示例：

```text
GatewayApplicationController.CreateRequest
  -> application.domain.dto.GatewayApplicationCreateRequestDTO

GatewayApplicationService.GatewayApplicationView
  -> application.domain.vo.GatewayApplicationVO

GatewayApplicationEntity
  -> application.domain.po.GatewayApplicationPO

GatewayCatalogService.Protocol
  -> catalog.domain.enums.GatewayCatalogProtocolEnum

JdbcGatewayCatalogStore.MutableBusiness
  -> catalog.repository.jdbc.GatewayCatalogMutableBusiness
```

本次只改变 Java 类型名和 FQCN，不改变 JSON 字段、业务动作、表名或列名。

### 4.5 分层依赖规则

- `controller` 可以依赖本领域 `service` 和 `domain/dto|vo`，不得直接依赖 Repository 实现；
- `service` 可以依赖本领域 `domain`、Repository 契约和现有跨领域公开契约；
- `repository` 可以依赖本领域 `domain/po`、持久化框架和基础设施客户端；
- `domain` 不得依赖本领域的 `controller / service / repository`；
- `repository/jdbc|jpa|filesystem` 是实现目录，不反向成为 Service 的类型容器；辅助类型与消费者同包，避免为跨子包访问扩大可见性；
- 本次以行为兼容为先，不为消除既有跨领域查询而新增 Facade、Factory 或空接口。

采用的是领域垂直分包和单一职责原则，不引入 Strategy、Factory、Template Method 等 GoF 模式。目录重组和类型归位已经能够解决当前问题，增加模式会扩大变更面。

### 4.6 目标 package tree

```text
top.egon.cola.component.gateway.admin
├── bootstrap
│   ├── GatewayAdminApplication
│   └── GatewayAdminConfiguration
├── config
│   ├── GatewayAdminProperties
│   ├── GatewayAdminSecurityConfiguration
│   ├── GatewayAdminTransportSecurityValidator
│   └── properties
├── shared
│   ├── controller
│   ├── domain
│   │   ├── vo
│   │   ├── po
│   │   ├── enums
│   │   └── exception
│   └── repository
│       └── jdbc
├── auth
│   ├── controller
│   ├── domain
│   │   └── vo
│   └── service
├── application
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   ├── po
│   │   └── exception
│   ├── repository
│   └── service
├── group
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   └── po
│   ├── repository
│   └── service
├── catalog
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   ├── po
│   │   └── enums
│   ├── repository
│   │   └── jdbc
│   └── service
├── credential
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   └── po
│   ├── repository
│   │   └── jdbc
│   └── service
├── scope
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   └── vo
│   └── service
├── routing
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   └── po
│   ├── repository
│   │   └── jdbc
│   └── service
├── release
│   ├── controller
│   │   └── scheduled
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   ├── po
│   │   └── enums
│   ├── repository
│   │   └── jdbc
│   └── service
├── rule
│   ├── domain
│   │   ├── dto
│   │   └── vo
│   └── service
├── runtime
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   └── vo
│   └── service
├── observability
│   ├── controller
│   │   ├── message
│   │   └── scheduled
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   ├── po
│   │   └── enums
│   ├── repository
│   │   └── jdbc
│   └── service
├── reporting
│   ├── controller
│   │   ├── openapi
│   │   └── scheduled
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   └── po
│   ├── repository
│   │   └── jdbc
│   └── service
└── mcp
    ├── controller
    ├── domain
    │   ├── dto
    │   ├── vo
    │   ├── po
    │   ├── enums
    │   └── exception
    ├── repository
    │   ├── jdbc
    │   └── filesystem
    └── service
```

`runtime` 取代原 `application.projection`，因为 Provider、Engine Node 和 Runtime Consistency 是业务职责，Projection 只是实现形式。MCP 暂时作为一个完整领域，避免在没有业务边界的情况下拆散现有控制面编排。

### 4.7 JavaDoc 和注释

- 被迁移的顶层类型保留并修正现有中英双语 JavaDoc；
- JavaDoc 必须说明类型职责、使用边界和每个 component 的真实业务语义；
- 不保留“保存某字段对应的状态、依赖或配置值”这类无信息量模板描述；
- 所有宿主类上因嵌套类型产生的旧链接改为新顶层类型链接；
- 不借本次任务批量改写未触及类的注释。

---

## 5. 165 个嵌套类型迁移清单

下表中的目标类型均为独立顶层 Java 文件，目标包使用从 `admin` 根包开始的相对路径。5.1 至 5.4 是原 Controller、Service、ExceptionHandler 范围内的 87 个类型；5.5 是扩展扫描发现并已经确认一并清理的 78 个类型。

### 5.1 原 Management HTTP 范围

| 当前宿主 | 当前嵌套类型 | 目标顶层类型 |
|---|---|---|
| `GatewayAdminSessionController` | `SessionView` | `auth.domain.vo.GatewayAdminSessionVO` |
| `GatewayAdminExceptionHandler` | `ErrorResponse` | `shared.domain.vo.GatewayAdminErrorVO` |
| `GatewayAdminExceptionHandler` | `FieldError` | `shared.domain.vo.GatewayAdminFieldErrorVO` |
| `GatewayApplicationController` | `CreateRequest` | `application.domain.dto.GatewayApplicationCreateRequestDTO` |
| `GatewayApplicationController` | `UpdateRequest` | `application.domain.dto.GatewayApplicationUpdateRequestDTO` |
| `GatewayGroupController` | `CreateRequest` | `group.domain.dto.GatewayGroupCreateRequestDTO` |
| `GatewayGroupController` | `UpdateRequest` | `group.domain.dto.GatewayGroupUpdateRequestDTO` |
| `GatewayCredentialController` | `RotateRequest` | `credential.domain.dto.GatewayCredentialRotateRequestDTO` |
| `GatewayReleaseController` | `CreateRequest` | `release.domain.dto.GatewayReleaseCreateRequestDTO` |
| `GatewayReleaseController` | `RollbackRequest` | `release.domain.dto.GatewayReleaseRollbackRequestDTO` |
| `GatewayDraftController` | `RouteRequest` | `routing.domain.dto.GatewayDraftRouteRequestDTO` |
| `GatewayDraftController` | `PolicyRequest` | `routing.domain.dto.GatewayDraftPolicyRequestDTO` |
| `GatewayDraftController` | `MutationRequest` | `routing.domain.dto.GatewayDraftMutationRequestDTO` |
| `GatewayCatalogController` | `ResourceCreated` | `catalog.domain.vo.GatewayCatalogResourceCreatedVO` |
| `GatewayCatalogController` | `ManualInterfaceGroupRequest` | `catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO` |
| `GatewayCatalogController` | `ManualOperationRequest` | `catalog.domain.dto.GatewayManualOperationRequestDTO` |
| `GatewayCatalogController` | `ManualDefinitionRequest` | `catalog.domain.dto.GatewayManualDefinitionRequestDTO` |
| `GatewayCatalogController` | `ManualMetadataRequest` | `catalog.domain.dto.GatewayManualMetadataRequestDTO` |

### 5.2 原 Gateway Application Service 范围

| 当前宿主 | 当前嵌套类型 | 目标顶层类型 |
|---|---|---|
| `GatewayApplicationService` | `CreateGatewayApplication` | `application.domain.dto.GatewayApplicationCreateCommandDTO` |
| `GatewayApplicationService` | `UpdateGatewayApplication` | `application.domain.dto.GatewayApplicationUpdateCommandDTO` |
| `GatewayApplicationService` | `GatewayApplicationView` | `application.domain.vo.GatewayApplicationVO` |
| `GatewayGroupService` | `CreateGatewayGroup` | `group.domain.dto.GatewayGroupCreateCommandDTO` |
| `GatewayGroupService` | `UpdateGatewayGroup` | `group.domain.dto.GatewayGroupUpdateCommandDTO` |
| `GatewayGroupService` | `GatewayGroupView` | `group.domain.vo.GatewayGroupVO` |
| `GatewayCatalogService` | `Protocol` | `catalog.domain.enums.GatewayCatalogProtocolEnum` |
| `GatewayCatalogService` | `ManualOperation` | `catalog.domain.dto.GatewayManualOperationDTO` |
| `GatewayCatalogService` | `ManualDefinition` | `catalog.domain.dto.GatewayManualDefinitionDTO` |
| `GatewayCatalogService` | `ManualMetadata` | `catalog.domain.dto.GatewayManualMetadataDTO` |
| `GatewayCatalogService` | `OperationDetail` | `catalog.domain.vo.GatewayOperationDetailVO` |
| `GatewayCredentialService` | `IssuedCredential` | `credential.domain.vo.IssuedGatewayCredentialVO` |
| `GatewayCredentialService` | `CredentialView` | `credential.domain.vo.GatewayCredentialVO` |
| `GatewayProjectionService` | `ProjectionEnvelope<T>` | `runtime.domain.vo.GatewayProjectionEnvelopeVO<T>` |
| `GatewayProjectionService` | `ProviderQuery` | `runtime.domain.dto.GatewayProviderQueryDTO` |
| `GatewayProjectionService` | `ProviderInstanceProjection` | `runtime.domain.vo.GatewayProviderInstanceVO` |
| `GatewayProjectionService` | `RuntimeConsistency` | `runtime.domain.vo.GatewayRuntimeConsistencyVO` |
| `GatewayProjectionService` | `EngineNodeConsistency` | `runtime.domain.vo.GatewayEngineNodeConsistencyVO` |
| `GatewayProjectionService` | `RuleExpectation` | `runtime.service.GatewayRuleExpectation`（包级） |
| `GatewayProjectionService` | `ProjectionCounts` | `runtime.service.GatewayProjectionCounts`（包级） |
| `GatewayReleaseService` | `PreparedRelease` | `release.service.PreparedGatewayRelease`（包级） |
| `GatewayReleaseService` | `CreateRelease` | `release.domain.dto.GatewayReleaseCreateCommandDTO` |
| `GatewayReleaseService` | `RollbackRelease` | `release.domain.dto.GatewayReleaseRollbackCommandDTO` |
| `GatewayReleaseService` | `ReleaseView` | `release.domain.vo.GatewayReleaseVO` |
| `GatewayDefinitionReportService` | `DefinitionCounts` | `reporting.service.GatewayDefinitionCounts`（包级） |
| `GatewayDraftService` | `DraftView` | `routing.domain.vo.GatewayDraftVO` |
| `GatewayDraftService` | `MutationControl` | `routing.domain.dto.GatewayDraftMutationControlDTO` |
| `GatewayDraftService` | `RouteMutation` | `routing.domain.dto.GatewayRouteMutationDTO` |
| `GatewayDraftService` | `PolicyMutation` | `routing.domain.dto.GatewayPolicyMutationDTO` |
| `GatewayDraftService` | `MutationResult` | `routing.domain.vo.GatewayDraftMutationResultVO` |
| `GatewayDraftService` | `ValidationIssue` | `routing.domain.vo.GatewayDraftValidationIssueVO` |
| `GatewayDraftService` | `ValidationReport` | `routing.domain.vo.GatewayDraftValidationReportVO` |
| `GatewayDraftService` | `DraftDiff` | `routing.domain.vo.GatewayDraftDiffVO` |
| `GatewayScopeService` | `ScopeQuery` | `scope.domain.dto.GatewayScopeQueryDTO` |
| `GatewayScopeService` | `PhysicalApplicationKey` | `scope.domain.GatewayPhysicalApplicationKey` |
| `GatewayScopeService` | `ScopeView` | `scope.domain.vo.GatewayScopeVO` |

### 5.3 原 MCP HTTP 范围

| 当前宿主 | 当前嵌套类型 | 目标顶层类型 |
|---|---|---|
| `McpAppAdminController` | `ArtifactRequest` | `mcp.domain.dto.McpArtifactRequestDTO` |
| `McpApprovalController` | `ApprovalRequest` | `mcp.domain.dto.McpApprovalRequestDTO` |
| `McpApprovalController` | `ApprovalResponse` | `mcp.domain.vo.McpApprovalVO` |
| `McpApprovalController` | `ApprovalOwner` | `mcp.domain.vo.McpApprovalOwnerVO`（包级） |
| `McpCapabilityController` | `CapabilityRequest` | `mcp.domain.dto.McpCapabilityRequestDTO` |
| `McpProtocolInspectorController` | `InspectRequest` | `mcp.domain.dto.McpProtocolInspectRequestDTO` |
| `McpProtocolInspectorController` | `Inspection` | `mcp.domain.vo.McpProtocolInspectionVO` |
| `McpRemoteProviderController` | `ProviderRequest` | `mcp.domain.dto.McpRemoteProviderRequestDTO` |
| `McpRemoteProviderController` | `MountRequest` | `mcp.domain.dto.McpRemoteMountRequestDTO` |
| `McpServerController` | `ServerRequest` | `mcp.domain.dto.McpServerRequestDTO` |
| `McpServerController` | `MutationRequest` | `mcp.domain.dto.McpServerMutationRequestDTO` |
| `McpTaskAdminController` | `CancelRequest` | `mcp.domain.dto.McpTaskCancelRequestDTO` |
| `McpTaskAdminController` | `CancelResult` | `mcp.domain.vo.McpTaskCancelResultVO` |
| `McpToolAdminController` | `ManagedToolOverrideRequest` | `mcp.domain.dto.McpManagedToolOverrideRequestDTO` |
| `McpToolAdminController` | `RemoteToolRequest` | `mcp.domain.dto.McpRemoteToolRequestDTO` |
| `McpToolAdminController` | `MutationRequest` | `mcp.domain.dto.McpToolMutationRequestDTO` |

### 5.4 原 MCP Application Service 范围

| 当前宿主 | 当前嵌套类型 | 目标顶层类型 |
|---|---|---|
| `McpControlPlaneService` | `ServerMutation` | `mcp.domain.dto.McpServerMutationDTO` |
| `McpControlPlaneService` | `CapabilityMutation` | `mcp.domain.dto.McpCapabilityMutationDTO` |
| `McpControlPlaneService` | `RemoteProviderMutation` | `mcp.domain.dto.McpRemoteProviderMutationDTO` |
| `McpControlPlaneService` | `RemoteMountMutation` | `mcp.domain.dto.McpRemoteMountMutationDTO` |
| `McpControlPlaneService` | `ArtifactMutation` | `mcp.domain.dto.McpArtifactMutationDTO` |
| `McpControlPlaneService` | `ArtifactUpload` | `mcp.domain.dto.McpArtifactUploadDTO` |
| `McpControlPlaneService` | `MutationControl` | `mcp.domain.dto.McpMutationControlDTO` |
| `McpControlPlaneService` | `ServerView` | `mcp.domain.vo.McpServerVO` |
| `McpControlPlaneService` | `MutationResult` | `mcp.domain.vo.McpMutationResultVO` |
| `McpControlPlaneService` | `Preview` | `mcp.domain.vo.McpCapabilityPreviewVO` |
| `McpToolAdminService` | `ManagedToolOverrideMutation` | `mcp.domain.dto.McpManagedToolOverrideMutationDTO` |
| `McpToolAdminService` | `RemoteToolMutation` | `mcp.domain.dto.McpRemoteToolMutationDTO` |
| `McpToolAdminService` | `MutationControl` | `mcp.domain.dto.McpToolMutationControlDTO` |
| `McpToolAdminService` | `ManagedToolView` | `mcp.domain.vo.McpManagedToolVO` |
| `McpToolAdminService` | `RemoteToolView` | `mcp.domain.vo.McpRemoteToolVO` |
| `McpValidationService` | `ValidationReport` | `mcp.domain.vo.McpValidationReportVO` |
| `McpValidationService` | `ValidationFinding` | `mcp.domain.vo.McpValidationFindingVO` |

### 5.5 其他生产代码嵌套类型

| 当前宿主 | 当前嵌套类型 | 目标顶层类型 |
|---|---|---|
| `AdminActor` | `ActorType` | `shared.domain.enums.AdminActorTypeEnum` |
| `GatewayAdminProperties` | `Ddc` | `config.properties.GatewayAdminDdcProperties` |
| `GatewayAdminProperties` | `RuleChunk` | `config.properties.GatewayRuleChunkProperties` |
| `GatewayCallEventConsumerHandler` | `Result` | `observability.domain.enums.GatewayCallEventConsumeResultEnum` |
| `GatewayCatalogStore` | `ManualHierarchy` | `catalog.domain.dto.GatewayManualHierarchyDTO` |
| `GatewayCatalogStore` | `InterfaceGroupScope` | `catalog.domain.vo.GatewayInterfaceGroupScopeVO` |
| `GatewayCatalogStore` | `OperationRecord` | `catalog.domain.po.GatewayOperationPO` |
| `GatewayCatalogStore` | `OperationDefinition` | `catalog.domain.po.GatewayOperationDefinitionPO` |
| `GatewayCatalogStore` | `CurrentOperationDefinition` | `catalog.domain.vo.GatewayCurrentOperationDefinitionVO` |
| `GatewayCatalogStore` | `CatalogTree` | `catalog.domain.vo.GatewayCatalogTreeVO` |
| `GatewayCatalogStore` | `BusinessNode` | `catalog.domain.vo.GatewayBusinessNodeVO` |
| `GatewayCatalogStore` | `EntityNode` | `catalog.domain.vo.GatewayEntityNodeVO` |
| `GatewayCatalogStore` | `InterfaceGroupNode` | `catalog.domain.vo.GatewayInterfaceGroupNodeVO` |
| `GatewayCatalogStore` | `OperationNode` | `catalog.domain.vo.GatewayOperationNodeVO` |
| `GatewayCredentialStore` | `CredentialRecord` | `credential.domain.po.GatewayCredentialPO` |
| `GatewayDdcYamlDocument` | `Removal` | `rule.service.GatewayYamlRemoval`（包级） |
| `GatewayDdcYamlDocument` | `LeafLocation` | `rule.service.GatewayYamlLeafLocation`（包级） |
| `GatewayDdcYamlDocument` | `ParentLink` | `rule.service.GatewayYamlParentLink`（包级） |
| `GatewayDdcYamlDocument` | `PrefixMatch` | `rule.service.GatewayYamlPrefixMatch`（包级） |
| `GatewayDefinitionLifecycleReconciler` | `Scope` | `reporting.domain.dto.GatewayDefinitionLifecycleScopeDTO` |
| `GatewayDefinitionLifecycleStore` | `ReconcileResult` | `reporting.domain.vo.GatewayReconcileResultVO` |
| `GatewayDefinitionReportStore` | `StoredReport` | `reporting.domain.po.GatewayStoredReportPO` |
| `GatewayDraftStore` | `RouteDraft` | `routing.domain.po.GatewayRouteDraftPO` |
| `GatewayDraftStore` | `PolicyDraft` | `routing.domain.po.GatewayPolicyDraftPO` |
| `GatewayKafkaCallEventConsumer` | `Settings` | `observability.domain.dto.GatewayKafkaConsumerSettingsDTO` |
| `GatewayKafkaCallEventConsumer` | `RecordKey` | `observability.controller.message.GatewayKafkaRecordKey`（包级） |
| `GatewayKafkaCallEventConsumer` | `RebalanceListener` | `observability.controller.message.GatewayKafkaRebalanceListener`（包级） |
| `GatewayObservabilityStore` | `ConsumeFailure` | `observability.domain.po.GatewayConsumeFailurePO` |
| `GatewayObservabilityStore` | `TraceQuery` | `observability.domain.dto.GatewayTraceQueryDTO` |
| `GatewayObservabilityStore` | `AuditQuery` | `observability.domain.dto.GatewayAuditQueryDTO` |
| `GatewayObservabilityStore` | `TraceSummary` | `observability.domain.vo.GatewayTraceVO` |
| `GatewayObservabilityStore` | `AuditSummary` | `observability.domain.vo.GatewayAuditVO` |
| `GatewayObservabilityStore` | `RequestPoint` | `observability.domain.dto.GatewayRequestPointDTO` |
| `GatewayObservabilityStore` | `ProtocolCall` | `observability.domain.dto.GatewayProtocolCallDTO` |
| `GatewayObservabilityStore` | `DashboardSummary` | `observability.domain.vo.GatewayDashboardVO` |
| `GatewayObservabilityStore` | `Page<T>` | `observability.domain.vo.GatewayPageVO<T>` |
| `GatewayOperationSchemaValidator` | `State` | `reporting.service.GatewaySchemaValidationState`（包级） |
| `GatewayReleasePublicationCoordinator` | `PublicationOutcome` | `release.domain.vo.GatewayPublicationOutcomeVO` |
| `GatewayReleasePublicationCoordinator` | `Scope` | `release.domain.dto.GatewayPublicationScopeDTO` |
| `GatewayReleasePublicationCoordinator` | `Artifact` | `release.domain.vo.GatewayReleaseArtifactVO` |
| `GatewayReleasePublicationStore` | `PublicationRecord` | `release.domain.po.GatewayReleasePublicationPO` |
| `GatewayReleasePublicationStore` | `PhaseType` | `release.domain.enums.GatewayPublicationPhaseEnum` |
| `GatewayReleasePublicationStore` | `PublicationStatus` | `release.domain.enums.GatewayPublicationStatusEnum` |
| `GatewayReleasePublicationStore` | `ChunkCleanupCandidate` | `release.domain.po.GatewayChunkCleanupCandidatePO` |
| `GatewayReleaseStore` | `ReleaseRecord` | `release.domain.po.GatewayReleasePO` |
| `GatewayReleaseStore` | `TargetRecord` | `release.domain.po.GatewayReleaseTargetPO` |
| `GatewayReleaseStore` | `AttemptRecord` | `release.domain.po.GatewayReleaseAttemptPO` |
| `GatewayReleaseStore` | `RecoverableAttempt` | `release.domain.po.GatewayRecoverableReleaseAttemptPO` |
| `GatewayReportHmacFilter` | `AuthenticationFailure` | `reporting.controller.openapi.GatewayReportAuthenticationFailure`（包级） |
| `GatewayReportHmacFilter` | `CachedBodyRequest` | `reporting.controller.openapi.GatewayCachedBodyRequest`（包级） |
| `GatewayRouteTransportPolicyValidator` | `Range` | `routing.service.GatewayTransportRange`（包级） |
| `GatewayRouteTransportPolicyValidator` | `ValidationIssue` | `routing.service.GatewayTransportValidationIssue`（包级） |
| `GatewaySecretProtector` | `ProtectedSecret` | `credential.domain.vo.GatewayProtectedSecretVO` |
| `IdempotencyStore` | `Record` | `shared.domain.po.IdempotencyPO` |
| `JdbcGatewayCatalogStore` | `MutableBusiness` | `catalog.repository.jdbc.GatewayCatalogMutableBusiness`（包级） |
| `JdbcGatewayCatalogStore` | `MutableEntity` | `catalog.repository.jdbc.GatewayCatalogMutableEntity`（包级） |
| `JdbcGatewayCatalogStore` | `MutableGroup` | `catalog.repository.jdbc.GatewayCatalogMutableGroup`（包级） |
| `JdbcGatewayDefinitionReportStore` | `GroupRow` | `reporting.repository.jdbc.GatewayDefinitionGroupRow`（包级） |
| `JdbcGatewayDefinitionReportStore` | `OperationRow` | `reporting.repository.jdbc.GatewayDefinitionOperationRow`（包级） |
| `JdbcGatewayDefinitionReportStore` | `MutableStored` | `reporting.repository.jdbc.GatewayMutableStoredReport`（包级） |
| `JdbcGatewayObservabilityStore` | `SqlFilter` | `observability.repository.jdbc.GatewayObservabilitySqlFilter`（包级） |
| `JdbcMcpApprovalStore` | `Approval` | `mcp.domain.po.McpApprovalPO` |
| `JdbcMcpArtifactMetadataStore` | `ArtifactMetadata` | `mcp.domain.po.McpArtifactMetadataPO` |
| `JdbcMcpCapabilityDraftStore` | `CapabilityKind` | `mcp.domain.enums.McpCapabilityKindEnum` |
| `JdbcMcpCapabilityDraftStore` | `CapabilityDraft` | `mcp.domain.po.McpCapabilityRecordPO` |
| `JdbcMcpCapabilityDraftStore` | `McpCapabilityDraft` | `mcp.domain.po.McpCapabilityDraftPO` |
| `JdbcMcpCapabilityDraftStore` | `DraftMutation` | `mcp.domain.dto.McpCapabilityDraftMutationDTO` |
| `JdbcMcpCapabilityDraftStore` | `Binding` | `mcp.repository.jdbc.McpCapabilityBinding`（包级） |
| `JdbcMcpManagedToolOverrideStore` | `ManagedToolOverride` | `mcp.domain.po.McpManagedToolOverridePO` |
| `JdbcMcpManagedToolOverrideStore` | `DraftMutation` | `mcp.domain.dto.McpManagedToolDraftMutationDTO` |
| `JdbcMcpRemoteProviderStore` | `RemoteProviderDraft` | `mcp.domain.po.McpRemoteProviderDraftPO` |
| `JdbcMcpRemoteProviderStore` | `RemoteCapability` | `mcp.domain.po.McpRemoteCapabilityPO` |
| `JdbcMcpRemoteProviderStore` | `RemoteMountDraft` | `mcp.domain.po.McpRemoteMountDraftPO` |
| `JdbcMcpRemoteProviderStore` | `Mutation` | `mcp.domain.dto.McpRemoteProviderDraftMutationDTO` |
| `JdbcMcpRemoteToolDraftStore` | `RemoteToolDraft` | `mcp.domain.po.McpRemoteToolDraftPO` |
| `JdbcMcpRemoteToolDraftStore` | `DraftMutation` | `mcp.domain.dto.McpRemoteToolDraftMutationDTO` |
| `JdbcMcpTaskStore` | `TaskRecord` | `mcp.domain.po.McpTaskPO` |
| `McpReleaseContentFactory` | `ManagedToolProjection` | `mcp.domain.vo.McpManagedToolProjectionVO` |

### 5.6 现有顶层类型的领域归属

嵌套类型独立化之外，现有顶层类型也必须移动到目标领域。以下映射是强制边界：

| 当前包 | 目标领域/目录 |
|---|---|
| `admin` 根包 | 应用入口进入 `bootstrap`，装配类进入 `bootstrap` 或 `config` |
| `application` | Application、Group 分别进入 `application`、`group`；审计上下文和幂等能力进入 `shared` |
| `application.catalog` | `catalog.service` 或 `catalog.repository` |
| `application.credential` | `credential.service` 或 `credential.repository` |
| `application.observability` | `observability.service` 或 `observability.repository` |
| `application.projection` | `runtime.service` |
| `application.release` | `release.service` 或 `release.repository` |
| `application.reporting` | `reporting.service` 或 `reporting.repository` |
| `application.routing` | `routing.service` 或 `routing.repository` |
| `application.scope` | `scope.service` |
| `domain` | 按职责进入 `shared.domain`、`routing.domain`、`release.domain` |
| `infrastructure.messaging` | `observability.controller.message` |
| `infrastructure.persistence` | 按表/仓储所属领域进入 `<领域>.repository`、`<领域>.repository.jdbc` 或 `<领域>.domain.po` |
| `infrastructure.security` | JWT 转换进入 `auth.service`，安全装配进入 `config`，密钥保护进入 `credential.service` |
| `interfaces.management` | 按资源进入对应领域 `controller`；公共 Resolver/Advice 进入 `shared.controller`；认证接口进入 `auth.controller` |
| `interfaces.openapi` | `reporting.controller.openapi` |
| `interfaces.scheduled` | 按任务进入 `reporting.controller.scheduled`、`release.controller.scheduled` 或 `observability.controller.scheduled` |
| `mcp.application` | `mcp.service`、`mcp.domain.vo` 或 `mcp.domain.exception` |
| `mcp.artifact` | `mcp.repository.filesystem` |
| `mcp.interfaces` | `mcp.controller` |
| `mcp.persistence` | `mcp.repository`、`mcp.repository.jdbc` 或 `mcp.domain.po` |
| `rule` | 编译/发布能力进入 `rule.service`，传输对象进入 `rule.domain.dto/vo`；Route 专属 Mapper/Validator 进入 `routing.service` |
| `security` | `config` |

顶层类型命名同步规范化：

- `GatewayApplicationEntity`、`GatewayGroupEntity`、`GatewayDraftEntity`、`GatewayAuditLogEntity`、`McpServerEntity` 分别改为所属领域的 `...PO`；
- 仓储契约的 `Store` 后缀改为 `Repository`，例如 `GatewayCatalogStore -> GatewayCatalogRepository`；
- JDBC 实现的 `Jdbc...Store` 改为 `Jdbc...Repository`；
- `FileSystemMcpAppArtifactStore` 改为 `FileSystemMcpAppArtifactRepository`；
- 已经准确使用 `Repository` 后缀的 Spring Data 接口保留类名，只移动包；
- 类名/FQCN 变化只属于 Java 内部兼容面，不能改变数据库表、列、SQL、HTTP 或消息契约。

迁移总数必须保持 165。实施时若基线新增嵌套类型，必须按同一规则一并独立，不能只机械处理本表后忽略新增项。

---

## 6. HTTP 与认证兼容性边界

### 6.1 完全保留的认证行为

以下接口和调用链不变：

```text
GET /api/v1/auth/bootstrap
GET /api/v1/gateway/admin/session
```

同时保留：

- `GatewayAuthBootstrapController` 对 `AuthorizationBootstrapService` 的调用；
- `GatewayAdminSessionController` 从后端 Authentication/RBAC3 Authority 形成 Session View；
- Gateway Web `AuthContext` 请求 Session API；
- `CapabilityProvider`、`RequireCapability` 和 `useCapability`；
- Spring Security JWT 验签和后端权限校验；
- `GatewayAdminActorArgumentResolver` 与 `AdminActor` 审计身份链路。

`GatewayAdminSessionController.SessionView` 只迁移并重命名为 `auth.domain.vo.GatewayAdminSessionVO`，不删除接口或改由前端解析 JWT。

### 6.2 HTTP 契约冻结规则

独立化前后必须保持：

- Controller 的 `@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping` 路径；
- `@ResponseStatus`；
- `@PreAuthorize`、`@RequiresPermission`；
- Request component 的字段名、顺序、Java 类型和 Validation 注解；
- Response 的字段名、Java 类型和 Jackson 序列化结构；
- 现有异常到 HTTP 状态/错误码的映射；
- null、空集合、时间类型和枚举序列化行为。

Java FQCN 不是外部 HTTP 契约，可以改变；任何 JSON 差异都视为回归。

---

## 7. Gateway Web 页面级 Scope 架构

### 7.1 删除全局选择状态

删除以下职责：

- `ScopeProvider` 和 `useScope()`；
- `egon.gateway.admin.scope.v1` LocalStorage；
- `resolveInitialScope`、`configuredInitialScope` 和全局 `changeScope`；
- `VITE_GATEWAY_ADMIN_DEFAULT_BIZ_CODE`；
- `VITE_GATEWAY_ADMIN_DEFAULT_NAMESPACE`；
- `VITE_GATEWAY_ADMIN_DEFAULT_ENV`；
- `VITE_GATEWAY_ADMIN_DEFAULT_APP_CODE`；
- `AdminLayout` Header 中 Biz/Namespace/Env/App 四个选择器；
- Scope 切换时的 `queryClient.removeQueries(...)`；
- Scope 切换时强制跳转 `/dashboard`。

`App.tsx` 中的 Provider 层级调整为：

```text
AuthProvider
  -> CapabilityProvider
    -> RouterProvider
```

### 7.2 保留共享的 Scope Binding 数据，不共享选中值

新增 Gateway Web 内部查询 Hook：

```text
useGatewayScopeBindings()
```

职责仅限：

- 调用现有 `GET /api/v1/gateway/admin/scopes`；
- 使用固定 React Query Key `['gateway-scopes']` 缓存 Binding 列表；
- 暴露 loading、error、data 和 refetch；
- 不选择 Scope；
- 不写 LocalStorage；
- 不阻断应用根节点。

多个页面可以共享同一份 Binding 查询缓存，但每个页面的选中值独立保存在自身 URL Query 中。

### 7.3 页面级受控筛选组件

新增 Gateway Web 内部组件：

```text
GatewayScopeFilter
```

组件采用受控输入：

```ts
type GatewayScopeFilterProps = {
  fields: Array<'bizCode' | 'namespace' | 'env' | 'appCode'>
  value: Partial<Scope>
  required?: boolean
  onChange: (value: Partial<Scope>) => void
}
```

约束：

- 组件只负责依据 Binding 生成级联选项和提交筛选值；
- 页面决定需要哪些字段、是否完整后才能查询；
- 上游字段变化时，只清除已不再合法的下游值；
- 可选筛选支持清空；
- 组件不操作 Router 跳转以外的全局状态；
- Binding 加载失败时在组件局部展示错误和重试，不替换整个应用页面。

这是组件组合，不是新的全局 Context。

### 7.4 URL Query 作为页面筛选状态

使用 React Router `useSearchParams` 管理页面筛选：

```text
/applications?bizCode=retail&env=prod
/providers?bizCode=retail&namespace=default&env=prod&appCode=order
/observability/traces?env=prod&namespace=default&protocol=HTTP
```

规则：

- 缺失参数表示本页面未选择该筛选；
- 空字符串不发送给后端；
- 页面加载时从 URL 恢复；
- 页面间导航不会自动复制 Scope 参数；
- 返回/前进可以恢复本页面历史筛选；
- Query Key 必须包含规范化后的页面筛选；
- 页面 Mutation 只失效当前资源相关 Query Key，不清除其他页面缓存。

---

## 8. 页面行为规格

### 8.1 Admin Layout

- Header 保留平台名称、Admin API 状态、用户菜单和退出登录；
- 移除四个 Scope Select；
- 不再导入 `useScope`、`optionsFor` 或 `ScopeField`；
- 退出登录仍清理认证相关状态和 Query Client，并跳转登录页；
- capability 导航过滤保持不变。

### 8.2 Dashboard

现有 `/dashboard` 接口要求完整 `bizCode / appCode / env / namespace`，因此：

- 页面顶部提供四字段 `GatewayScopeFilter`；
- URL 未提供完整合法 Scope 时不发送 Dashboard 请求；
- 未选择完成时展示明确引导，不使用第一条 Binding 静默替用户选择；
- 选择完成后只查询当前页面 Scope；
- 切换条件只影响 Dashboard Query；
- 本期不把多个 Scope 的统计结果在浏览器相加。

### 8.3 Gateway Group

- `GET /gateway-groups` 不再由前端附加全局 Scope；
- 默认展示全部 Gateway Group；
- 页面提供可选 Env、Namespace 筛选；
- 由于现有接口没有 Group 查询参数，筛选在已返回的 Group 列表上执行；
- 表格保留 Env、Namespace 列；
- 新建 Group 表单新增 Env、Namespace 选择，选项来自 DDC Binding 去重后的组合；
- 编辑已有 Group 不从页面筛选覆盖资源自身 Scope；
- 创建/更新成功只失效 Gateway Group 相关 Query。

### 8.4 Application / Credential

- 默认调用 `GET /applications`，不发送任何 Scope 参数，展示全部 Application；
- 页面提供可选 Biz、Namespace、Env、App 筛选；
- 已选择的非空字段作为现有可选 Request Param 发送；
- 表格必须显示 Biz、Application Code、Env、Namespace，避免跨 Scope 行不可辨认；
- 新建 Application 时必须从 DDC Binding 选择一个精确四字段 Scope；
- 已连接 Binding 继续出现在筛选候选中，但创建表单必须禁用 `connected=true` 的 Binding，避免重复创建；
- 创建请求字段和后端 `requireEnabled` 校验保持不变；
- 编辑 Application 和 Credential 操作始终以选中资源 ID 为准；
- 页面筛选不得覆盖已有 Application 的物理身份。

### 8.5 Interface Catalog

- Application 候选默认来自空条件 `/applications`；
- 页面可选 Biz、Namespace、Env、App 筛选 Application 候选；
- 每个选项标签包含 Biz/App/Env/Namespace 和显示名；
- 当前 Application 被新筛选排除时清空选中值，不继续展示不匹配目录；
- Catalog 仍通过 Application ID 请求，不修改接口；
- `/applications/:applicationId/catalog` 路由优先使用路径中的 Application ID，并展示其自身 Scope，不依赖其他页面状态。

### 8.6 Provider

- 页面顶部提供完整四字段 Scope Filter；
- 未选择完整 Scope 时不请求 `/providers/instances`；
- 选择完成后按现有接口查询；
- Scope 值保存在 Provider 页 URL；
- 10 秒刷新只刷新当前 Provider 页面当前 Scope；
- 本期不遍历所有 Binding 并发获取 Provider。

### 8.7 Trace

- 页面只要求现有接口真正支持的 Env、Namespace；
- Env、Namespace 与 Trace ID、Protocol、Status 共同保存在当前页面 URL；
- Env、Namespace 未完整选择时不请求；
- 提交新筛选时页码重置为 1；
- 自动刷新只使用当前页面参数；
- 不再读取 Biz/App 全局状态。

### 8.8 Audit

- 页面只要求现有接口真正支持的 Env、Namespace；
- Env、Namespace 与 Actor、Resource ID、Trace ID、Successful 共同保存在当前页面 URL；
- Env、Namespace 未完整选择时不请求；
- 提交新筛选时页码重置为 1；
- Drawer 详情和脱敏逻辑不变；
- capability 检查保持不变。

### 8.9 MCP

- `McpServersPage` 和 `McpRemoteProvidersPage` 默认加载全部 Gateway Group；
- 页面可选 Env、Namespace 缩小 Group 候选，但筛选只属于当前 MCP 页面；
- Server/Remote Provider 的后续请求继续使用选中的 Gateway Group ID；
- `McpResourcesPanel`、`McpPromptsPanel` 各自提供页面内 Application 选择器，候选默认来自全部 Application；
- 选定 Application 后只加载该 Application 的 Catalog，再构建 Operation 候选，不并发拉取全部 Application Catalog；
- Application 选项标签包含 Biz/App/Env/Namespace 和显示名，可在 Panel 内按 Scope 缩小候选；
- 不再从全局 Scope 隐式排除其他 Application 的 Operation；
- MCP Mutation、Validation、Preview 和审批协议不变。

### 8.10 详情页

以下详情页继续按路径资源 ID 查询，不要求全局或页面 Scope：

- Gateway Group Overview；
- Draft Routes/Policies；
- Release List/Detail；
- Operation Detail；
- MCP Server Workbench。

详情页展示 Scope 时读取资源自身字段，不读取其他页面 URL。

---

## 9. Gateway Web API Client 调整

这些是前端内部函数签名调整，不改变后端 HTTP 接口。

```ts
gatewayApi.groups(signal?)

gatewayApi.applications(filters?: Partial<Scope>, signal?)

gatewayApi.dashboard(scope: Scope, signal?)

gatewayApi.providers(scope: Scope, signal?)

gatewayApi.traces(
  scope: Pick<Scope, 'env' | 'namespace'>,
  filters: URLSearchParams,
  signal?: AbortSignal,
)

gatewayApi.audits(
  scope: Pick<Scope, 'env' | 'namespace'>,
  filters: URLSearchParams,
  signal?: AbortSignal,
)

gatewayApi.mcpOperationOptions(
  applicationId: string,
  signal?: AbortSignal,
)
```

前端 `GatewayGroup` 类型不再继承完整 `Scope`。它必须只显式声明后端实际返回的 Group 字段和 `env / namespace`，避免在类型层伪造不存在的 `bizCode / appCode`。

Query String 构建规则：

- 使用一个只附加非空字段的工具函数；
- 不再要求所有 API 调用都传完整 `Scope`；
- `groups` 不发送后端未声明的 Scope 参数；
- 不产生尾部 `?` 或重复 `&`；
- URL 参数必须使用 `URLSearchParams` 编码。

---

## 10. 错误、空状态与安全行为

### 10.1 Scope Binding 错误

- 应用壳和与 Scope Binding 查询无关的页面仍可进入；
- 受影响页面继续呈现其实际后端查询结果或错误，不把 Binding Hook 的错误提升为全局错误；
- 使用 Scope Filter 的局部区域显示“DDC Scope 加载失败”和重试；
- 依赖精确 Binding 的创建按钮可以禁用，并说明原因；
- 不再用全屏 `Result` 替换整个已登录应用。

### 10.2 未完成必填筛选

Dashboard、Provider、Trace、Audit 在筛选不完整时：

- 不发送无效请求；
- 不把后端 400 当作页面初始化方式；
- 展示“请选择查询范围”的局部空状态；
- 不自动选择第一个 Scope。

### 10.3 无匹配数据

- 查询成功且列表为空时使用页面现有空状态/Table empty；
- Scope Binding 为空不代表 Gateway Group/Application 等本地数据为空；
- 只有依赖 Binding 的筛选/创建区域显示无可用 Binding。

### 10.4 认证授权

- 401 仍进入现有 Refresh/Fatal Auth 流程；
- 403 仍由现有页面错误处理和 capability 边界处理；
- 前端页面级 Scope 只表示查询条件，不是授权边界；
- 后端仍必须执行全部认证、权限和 DDC Binding 校验。

---

## 11. 设计模式取舍

本次不引入 Strategy、Factory、Template Method 或新的全局状态容器。

原因：

- Java 部分是明确的职责归位和类型独立化，直接顶层类型比额外接口/工厂更清晰；
- 页面 Scope 的变化点是页面所需字段和查询触发条件，受控组件加小型 Hook 已能表达；
- 引入 Scope Strategy 或 Store 会再次把简单查询条件提升为跨页面抽象，违背本次目标；
- 现有 React Query 已负责服务端状态缓存，无需再增加 Redux/Zustand。

采用的原则是组合与单一职责：共享 Binding 数据查询，页面独立拥有选择状态。

---

## 12. 测试与验收

### 12.1 Java 结构测试

新增不依赖第三方扫描库的 JUnit 测试，从当前测试运行时的 `target/classes/top/egon/cola/component/gateway/admin` 动态加载全部顶层生产类，并断言：

```java
assertThat(type.getDeclaredClasses()).isEmpty();
```

测试必须：

- 覆盖整个 Gateway Admin `src/main/java`，不能只枚举 Controller/Service；
- 证明基线 59 个宿主中的 165 个嵌套类型已经全部清零；
- 对迁移后新增的顶层类型同样执行检查；
- 输出仍存在嵌套声明的宿主 FQCN，便于定位；
- 不引入 ArchUnit、ClassGraph 或反射扫描依赖。

同时增加源码/构建脚本检查，保证生产 Java 文件中不再出现缩进后的 `record / class / enum / interface` 声明。

新增包结构测试，验证：

- 不再存在 `admin.interfaces`、`admin.infrastructure` 和旧 `admin.mcp.application/interfaces/persistence/artifact`；
- 不再存在旧 `admin.application.catalog/credential/observability/projection/release/reporting/routing/scope`；
- Controller 不直接依赖 `repository.jdbc/jpa/filesystem`；
- `domain` 不依赖本领域 `controller/service/repository`；
- DTO/VO/PO/Enum 的目录与后缀一致；
- 每个非空新包包含符合现有中英双语规范的 `package-info.java`。

### 12.2 Java 行为兼容测试

- 现有 Controller MVC/Security 测试继续通过；
- 现有 Service 单元测试继续通过；
- 现有 MCP Controller/Service 测试继续通过；
- Request Validation 行为保持；
- Session View 和 Error Response JSON 字段保持；
- 代表性 Command/View 构造校验和集合不可变性保持；
- 编译期更新全部 `OwningType.NestedType` 和旧技术包引用，仓库中不遗留旧 FQCN；
- JPA Entity 移动/重命名后表名、列名、Version、查询方法和持久化行为保持；
- Store/Repository 重命名后方法签名、事务边界和 JDBC SQL 保持。

建议针对迁移前易回归的外部响应增加 JSON Characterization Test，至少覆盖：

- Gateway Admin Session；
- Gateway Admin Error；
- Application/Group View；
- Projection Envelope/Runtime Consistency；
- Draft Validation/Mutation Result；
- MCP Mutation/Validation Result。

### 12.3 前端单元测试

至少覆盖：

1. `AdminLayout` 不再渲染全局 Scope 选择器；
2. App 根节点不再依赖 `ScopeProvider`；
3. Scope Binding Hook 在多个页面调用时共享 React Query 缓存；
4. 页面筛选只修改当前 URL Query；
5. Dashboard/Provider 缺少完整四字段时不请求；
6. Trace/Audit 缺少 Env/Namespace 时不请求；
7. Gateway Group 默认展示不同 Env/Namespace 的数据；
8. Application 默认请求无 Scope 参数并展示跨 Scope 行；
9. Application 创建提交所选 Binding 的精确四字段；
10. Catalog 候选不再被其他页面 Scope 限制；
11. MCP Group/Application/Operation 候选不再依赖全局 Scope，且只加载已选 Application 的 Catalog；
12. 页面切换不会清除无关 React Query 缓存；
13. Binding Hook 加载失败不会用全屏错误替换应用壳，未依赖该 Hook 的页面仍可进入；
14. 原认证、Session 和 capability 测试继续通过。

### 12.4 静态验收命令

后端：

```bash
./mvnw -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am test
```

前端，在 `egon-cola-platform-gateway-admin-web` 下：

```bash
npm test
npm run typecheck
npm run lint
npm run build
```

仓库检查：

```bash
java_root=egon-cola-platforms/egon-cola-platform-gateway/\
egon-cola-platform-gateway-admin/src/main/java

rg -n --glob '*.java' \
  '^[[:space:]]+(public[[:space:]]+|protected[[:space:]]+|private[[:space:]]+|static[[:space:]]+|final[[:space:]]+)*(record|class|enum|interface)[[:space:]]+[A-Z]' \
  "$java_root"

rg -n '^package .*gateway\.admin\.(interfaces|infrastructure)(\.|;)' \
  "$java_root"

rg -n '^package .*gateway\.admin\.mcp\.(application|interfaces|persistence|artifact)(\.|;)' \
  "$java_root"

rg -n '^package .*gateway\.admin\.application\.(catalog|credential|observability|projection|release|reporting|routing|scope)(\.|;)' \
  "$java_root"

rg --files "$java_root" | rg '/[^/]*(Entity|Store)\.java$'

rg "ScopeProvider|useScope\\(|VITE_GATEWAY_ADMIN_DEFAULT_" \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web

git diff --check
```

预期：上述嵌套声明、旧技术根包、旧 `Entity/Store` 文件名、全局 Scope Provider 和默认 Scope 环境变量均无活动源码命中；历史设计文档不作为失败依据。

### 12.5 不执行的运行态验证

遵循项目协作约束，本任务完成后不自动启动 Gateway Admin、Gateway Web、DDC、IdP 或 RBAC3，也不打开浏览器。运行态联调由用户启动环境后执行。

---

## 13. 实施任务与提交边界

实施阶段采用以下独立任务和独立提交，避免 Java 大规模包迁移与前端行为改造混在一起：

1. 建立 package 结构守卫，并迁移 `bootstrap/config/shared/auth`；
2. 迁移 `application/group/scope` 的 Controller、Domain、Repository、Service 和相关测试；
3. 迁移 `catalog/credential`，独立 Store/JDBC 内部类型并完成 Repository/PO 命名规范化；
4. 迁移 `routing/release/rule`，保持发布、规则编译和 DDC 交互行为；
5. 迁移 `runtime/observability/reporting`，同步消息、OpenAPI、定时任务和持久化测试；
6. 迁移完整 `mcp` 领域，清理 Controller/Service/JDBC/FileSystem 中全部嵌套类型；
7. 执行全模块旧包/FQCN/嵌套类型收口和 Java 契约回归；
8. Gateway Web 移除全局 Scope 基础设施并建立页面级 Binding/Filter 能力；
9. Gateway Group/Application/Catalog/MCP 迁移到默认全量与页面筛选；
10. Dashboard/Provider/Trace/Audit 迁移到页面级必填 Scope；
11. 前端测试、E2E 夹具源码和 README 收口。

每个任务只提交自身路径，提交前检查工作树，不能夹带其他 agent 或用户的并行修改。

---

## 14. 完成定义

只有同时满足以下条件，才可声明本规格实施完成：

- 165 个基线嵌套类型均已迁移，整个生产源码新增嵌套类型为 0；
- 领域优先 package tree 已落地，不再存在旧技术根包和旧 MCP 技术子包；
- DTO、VO、PO、Enum、Exception 和内部辅助类型均位于规定目录并使用规定命名；
- 全生产类嵌套类型守卫和 package 结构测试通过；
- Controller 不直接依赖 Repository 实现，Domain 不反向依赖 Controller/Service/Repository；
- 所有既有 Gateway Admin HTTP 路径与 JSON 契约保持；
- 两个认证接口、Gateway Web Session API 和 capability 行为未改变；
- Gateway Web Header 不再存在全局 Scope 选择器；
- 应用根节点不再由 Scope Binding 成败决定是否可用；
- Gateway Group、Application、Catalog、MCP 默认候选不受单一全局 Scope 限制；
- Dashboard、Provider、Trace、Audit 各自持有并恢复自己的 URL 查询范围；
- 页面筛选不会清空其他页面缓存或强制跳转；
- DDC Binding 仍用于合法筛选和创建输入；
- 后端目标测试、前端 test/typecheck/lint/build 和 `git diff --check` 全部通过；
- 没有数据库 migration、认证模型或无关模块变更；
- 未自动启动项目，运行态边界已明确报告。

---

## 15. 后续事项

若未来要求 Dashboard、Provider、Trace、Audit 在一次查询中真正返回跨 Biz/App/Namespace/Env 的全局数据，应单独设计后端接口，包括：

- 可选 Scope 参数；
- 全局分页与稳定排序；
- Dashboard 指标的服务端聚合口径；
- DDC Namespace 可见性和授权边界；
- 大范围查询的索引、限流和超时策略。

该后续能力不能通过浏览器循环请求并简单相加代替，也不属于本规格实施范围。
