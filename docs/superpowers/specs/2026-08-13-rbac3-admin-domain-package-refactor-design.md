# RBAC3 Admin 领域分包与 Java 类型独立化改造规格

> 状态：待用户书面审查
> 编写日期：2026-08-13
> 代码基线：`main@456edb67`
> 适用仓库：`/Users/mario/SelfProject/Egon-COLA`

主要涉及模块：

- `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin`

相关契约验证涉及但不主动重构的模块：

- `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter`

本文固化已确认的“方案 C”：RBAC3 Admin 改为领域优先的垂直分包，每个领域内部按 `controller / domain / repository / service` 展开；`domain` 再按实际内容细分为 `dto / vo / po / enums / exception`；生产源码中现存的全部 386 个嵌套 `record`、`class`、`enum` 和 `interface` 必须清零，不只处理 Controller 和 Service 中的公开类型。

本文是设计规格，不是实施计划。本规格获书面批准后，下一阶段才编写包含逐文件迁移清单、386 个嵌套类型去向清单、依赖顺序和逐提交验证命令的 implementation plan；本阶段不修改生产代码。

---

## 1. 决策摘要

| 编号 | 已确认决策 |
|---|---|
| RA-01 | 包结构采用领域优先：`admin.<领域>.controller/domain/repository/service` |
| RA-02 | `domain` 下按实际需要建立 `dto / vo / po / enums / exception`，不创建空包 |
| RA-03 | 原有全局技术根包 `application / interfaces / infrastructure / integration / security / worker / snapshot` 在迁移完成后必须消失 |
| RA-04 | `Rbac3AdminApplication` 保留在 `top.egon.cola.platform.rbac3.admin` 根包，维持 Spring Boot 默认组件扫描边界 |
| RA-05 | `bootstrap`、`config` 和 `shared` 是明确例外；它们只承担启动装配、配置和真正跨领域的通用契约，不成为业务类型垃圾桶 |
| RA-06 | `resource` 领域同时容纳 Application、Resource、Permission 和 Manifest 相关模型，不新增含义含混且容易与技术层混淆的顶层 `application` 业务包 |
| RA-07 | 原 `snapshot`、`worker` 以及运行态 DDC、Gateway、Outbox 集成统一归入 `runtime` 领域 |
| RA-08 | 所有生产 Java 源码实行一个顶层类型一个文件，现有 386 个嵌套类型全部独立，最终不允许任何嵌套 `record / class / enum / interface` |
| RA-09 | Request、Command、Query、Mutation 等输入类型归入 `domain/dto` 并使用 `DTO` 后缀 |
| RA-10 | Response、View、Result、Page、Projection 等输出类型归入 `domain/vo` 并使用 `VO` 后缀 |
| RA-11 | JPA Entity、Embeddable 和持久化记录归入 `domain/po` 并使用 `PO` 后缀；枚举归入 `domain/enums` 并使用 `Enum` 后缀 |
| RA-12 | Repository 契约是独立顶层类型，位于所属领域 `repository`；实现按 `jpa / jdbc / redis / http / ddc / outbox / internal` 等真实技术细分 |
| RA-13 | Controller、Service、Facade、Processor、Coordinator、Revalidator、Factory 等功能性类不得继续充当实体或协议类型容器，调用点不得出现 `功能类.实体类` |
| RA-14 | Controller 只依赖 DTO、VO 和 Service；Service 不依赖 Controller 或 Repository 实现；Domain 不依赖 Controller、Service、Repository |
| RA-15 | HTTP 路由、方法、状态码、JSON 字段、校验、Jackson 行为、权限语义、数据库结构、Redis Key 和 Outbox 业务字段全部冻结 |
| RA-16 | JPA 类改名为 `*PO` 时保留原显式 entity name，以避免本次同时改写 JPQL entity name；表、列、索引和 Flyway 不变 |
| RA-17 | Gateway Operation 的外部名称和可见性冻结；Java Schema definition key 因 Java FQCN 变化而改变，属于受控且必须验证的预期差异 |
| RA-18 | 不引入新依赖、ArchUnit、代码生成器、新模块或为目录整齐而创建的空抽象 |
| RA-19 | 保留有业务含义的现有 Facade 和 Ports/Adapters 边界，不为本次搬包额外引入 Strategy、Factory、Template Method 等模式 |
| RA-20 | 改造分八个可编译、可测试、可独立提交的波次完成；任何提交都不得处于源码无法编译的中间状态 |

---

## 2. 当前基线与问题

### 2.1 基线统计

以本文代码基线扫描 `rbac3-admin/src/main/java`：

- 生产 Java 文件共 242 个；
- 其中 121 个文件包含嵌套类型；
- 实际嵌套类型共 386 个；
- 按类型分为 232 个 `record`、73 个 `enum`、69 个 `interface`、12 个 `class`；
- 其中 350 个是 `public` 嵌套类型；
- Controller 中存在 46 个 `public` 嵌套类型；
- Service、Facade、Processor、Coordinator、Revalidator、Factory 等功能性宿主中存在 194 个 `public` 嵌套类型；
- 仅按用户明确禁止的“功能性类中的 public record/class”口径也有 174 个，但方案 C 要求处理全部 386 个；
- 持久化映射类型共 44 个，其中 43 个使用 `@Entity`；
- 模块测试 Java 文件共 86 个，迁移时必须同步更新引用和结构约束。

统计排除了一个名为 `record` 的局部变量误命中。实施计划生成前必须基于当时 HEAD 重新扫描并冻结逐类型清单；如基线发生变化，以重新扫描结果为准，但“生产嵌套类型归零”的验收标准不变。

### 2.2 当前包结构同时使用领域根和技术根

当前一部分代码按领域组织：

```text
activation/application|domain|infrastructure
assignment/application|domain|infrastructure
audit/application|domain|infrastructure
...
```

另一部分又散落在全局技术根：

```text
application/port
interfaces/http
infrastructure/persistence
integration/ddc|flyway|gateway|outbox|runtime
security
snapshot/application|infrastructure
worker
```

因此一次业务调用链可能从 `interfaces.http` 跨到某领域 `application`，再跨到该领域 `infrastructure` 或全局 `integration`。同一职责还存在两套命名：领域内部是 `application/infrastructure`，目标规范却要求 `service/repository`。

### 2.3 功能性类兼任类型容器

Controller、Service 和 Facade 文件中大量声明 Request、Response、Command、View、Result、Page、Repository Contract、Entity、枚举和内部状态，产生以下问题：

1. 调用方引用 `RoleFacade.RoleView`、`RuntimeQueryService.MutationPage` 等功能类限定名；
2. 协议类型无法独立检索、审查和编写清晰 JavaDoc；
3. 功能类的 public API 被其内部数据结构无意放大；
4. Repository 契约和实现容易与 Service、Facade 混在同一文件；
5. 单文件过长，移动任何协议类型都会制造与业务流程无关的 diff；
6. 私有嵌套类型没有统一归属，无法通过结构测试阻止继续增长。

本次解决的是包、类型和依赖归属，不借机重写业务规则。

### 2.4 跨领域巨型适配器扩大耦合

以下文件已经超出单一领域边界，需要在搬包时做有边界的职责拆分：

- `TenantUserDirectoryController` 同时承担 Tenant、User Directory 和 Directory HTTP 入口；
- `AuditSimulationController` 同时承担 Audit 和 Authorization Simulation 入口；
- `Rbac3IdentitySessionQueryStore` 同时实现 Auth、Refresh、Bootstrap、Session、Assignment、Step-up、Directory 等九组查询/存储契约；
- `IdentityRepositories` 同时承载 Identity Mapping、Password Credential 和 Tenant 查询。

这些拆分只按既有契约和方法群分离，不改变执行逻辑、查询语义或事务边界。

---

## 3. 目标与非目标

### 3.1 目标

1. 让每个业务领域在一个根包下即可看到入口、模型、仓储和服务；
2. 让 Controller 和 Service 返回、接收的 DTO/VO 都是独立顶层类型；
3. 让 PO、Enum、Exception 和 Repository Contract 有稳定、明确且可检索的位置；
4. 清除生产源码全部 386 个嵌套类型，不因可见性或宿主类别留下例外；
5. 保持每个原有不可变载体的不可变性、component 顺序和构造校验；
6. 保持原有业务行为、外部协议、持久化语义、权限语义和运行时装配；
7. 拆分四个已经跨越多个领域的巨型入口或适配器；
8. 为所有新建 package 和迁移后的顶层类型补齐与项目既有风格一致的中英双语 JavaDoc；
9. 建立无需新依赖的结构回归测试，阻止嵌套类型、旧技术根包和反向层依赖回流；
10. 以八个原子提交完成迁移，每个提交都可编译和回归。

### 3.2 非目标

- 不修改 RBAC 权限计算、授权决策、角色闭包、约束计算、会话刷新或审计业务规则；
- 不新增、删除、重命名 HTTP Route，不改变 HTTP Method、Status、JSON 字段或错误语义；
- 不改变 Gateway Operation 名称、权限映射或外部可见性；
- 不修改数据库表、列、索引、约束和数据，不新增 Flyway migration；
- 不修改 Redis Key、缓存 TTL 和序列化业务字段；
- 不改变 Outbox 事件的业务字段、投递语义和幂等语义；
- 不新增公共兼容代理类型来永久保留旧 FQCN；
- 不为了消除所有跨领域调用而创建新的 Facade、Event Bus、Strategy 或中间模块；
- 不把所有 DTO、VO、PO 集中到模块级公共包；
- 不把 `record` 机械改写为 JavaBean，不引入 Lombok；
- 不启动项目，不执行浏览器测试或运行态联调；
- 不在本规格阶段修改生产源码或编写 implementation plan。

---

## 4. 目标 package tree

### 4.1 最终树

以下是迁移完成后允许存在的一级领域和技术职责。`dto / vo / po / enums / exception` 以及 Repository 技术子包仅在存在真实类型时创建；最终仓库不得保留空目录或只为占位创建的 `package-info.java`。

```text
top.egon.cola.platform.rbac3.admin
├── Rbac3AdminApplication.java
├── package-info.java
├── bootstrap
│   ├── controller
│   │   └── cli
│   ├── domain
│   │   ├── dto
│   │   └── vo
│   ├── repository
│   │   └── jpa
│   └── service
├── config
│   ├── ddc
│   ├── flyway
│   ├── persistence
│   ├── properties
│   ├── redis
│   ├── runtime
│   └── security
├── shared
│   ├── controller
│   ├── domain
│   │   ├── dto
│   │   ├── vo
│   │   ├── po
│   │   └── exception
│   ├── repository
│   │   └── jpa
│   └── service
├── tenant
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── identity
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── directory
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── auth
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,redis,internal}
│   └── service/{internal}
├── session
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,redis,internal}
│   └── service/{internal}
├── resource
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── role
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,jdbc,internal}
│   └── service/{internal}
├── assignment
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── activation
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── constraint
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── management
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── authorization
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── participation
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── audit
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
├── simulation
│   ├── controller
│   ├── domain/{dto,vo,po,enums,exception}
│   ├── repository/{jpa,internal}
│   └── service/{internal}
└── runtime
    ├── controller
    │   ├── message
    │   └── scheduled
    ├── domain/{dto,vo,po,enums,exception}
    ├── repository/{ddc,http,jpa,outbox,redis,internal}
    └── service/{internal}
```

大括号是规格中的紧凑表示，不是实际 Java 包名。例如 `tenant.domain.{dto,vo}` 表示两个独立包 `tenant.domain.dto` 和 `tenant.domain.vo`。implementation plan 必须依据逐类型清单裁剪未使用分支。

### 4.2 一级包边界

| 目标一级包 | 归属内容 |
|---|---|
| `bootstrap` | 初始化、默认管理员/租户引导、CLI 入站、引导状态持久化 |
| `config` | Spring 配置、Properties、Security、Flyway、JPA、Redis、DDC 和运行时装配 |
| `shared` | 经逐消费者证明同时服务多个领域且没有合理业务归属的基础契约；不得放置“暂时不知道放哪里”的类型 |
| `tenant` | 租户生命周期、租户查询和租户级配置 |
| `identity` | 身份映射、凭据主体、身份状态和身份管理 |
| `directory` | 用户目录、组织目录和目录查询 |
| `auth` | 登录认证、Step-up、刷新凭据和认证策略 |
| `session` | 会话创建、查询、刷新、吊销和会话强度 |
| `resource` | Application、Resource、Permission、Manifest 及其管理接口 |
| `role` | 角色、角色层级、角色闭包和角色视图 |
| `assignment` | 用户/主体与角色的分配关系 |
| `activation` | 角色激活、激活上下文和激活状态 |
| `constraint` | 约束定义、编译、存储和校验 |
| `management` | 管理策略及管理面授权规则 |
| `authorization` | 授权查询、决策、Mutation 和运行时投影 |
| `participation` | 参与关系和参与者授权上下文 |
| `audit` | 审计记录、审计查询和审计输出 |
| `simulation` | 授权模拟输入、计算和模拟结果 |
| `runtime` | Runtime Snapshot、Worker、DDC/Gateway/Outbox 投递、运行态同步和状态查询 |

### 4.3 旧根包到目标边界

| 当前包或职责 | 目标位置 |
|---|---|
| `interfaces.http` | 按 Route 所属领域迁入 `<domain>.controller` |
| `application.port` | 按契约所属领域迁入 `<domain>.repository` 或 `<domain>.service`；不能整体搬到 `shared` |
| `infrastructure.persistence` | 按 PO/Repository 所属领域迁入 `<domain>.domain.po`、`<domain>.repository.jpa` |
| `<domain>.application` | 业务编排迁入 `<domain>.service`，输入/输出类型分别迁入 DTO/VO |
| `<domain>.infrastructure` | Repository 实现迁入 `<domain>.repository.<technology>` |
| `integration.ddc` | 装配类迁入 `config.ddc`，领域运行态访问迁入 `runtime.repository.ddc` |
| `integration.flyway` | `config.flyway` |
| `integration.gateway` | 运行态 HTTP 客户端归入 `runtime.repository.http`，纯装配归入 `config.runtime` |
| `integration.outbox` | `runtime.repository.outbox` 或 `runtime.service` |
| `integration.runtime` | 按职责拆入相关领域 Repository/Service；运行态聚合归入 `runtime` |
| `security` | 安全装配归入 `config.security`；认证业务归入 `auth`，会话业务归入 `session` |
| `snapshot` | `runtime.domain`、`runtime.repository`、`runtime.service` |
| `worker` | 入站调度归入 `runtime.controller.scheduled`，处理流程归入 `runtime.service` |

---

## 5. Java 类型独立化规范

### 5.1 强制清零范围

实施完成后，`rbac3-admin/src/main/java` 中每个生产类必须满足：

```text
getDeclaredClasses().length == 0
```

禁止的嵌套声明覆盖任意可见性：

```text
record / class / enum / interface
```

这包括 Controller/Service 中的 public Request/Response，也包括 Repository 实现中的 private Row、Service 中的 private Calculation Context、配置类中的内部属性类型和领域对象中的嵌套枚举。匿名类、Lambda 生成的编译器实现不作为源码声明类型处理；验收测试针对源码对应顶层类的 `getDeclaredClasses()`，不以所有带 `$` 的 JVM 合成类名做误判。

### 5.2 一个类型一个文件

- 每个 `.java` 文件只声明一个顶层 `class`、`record`、`enum` 或 `interface`；
- 原 `public` 嵌套类型迁移为必要可见性的顶层类型；
- 原 `private` 或包级实现辅助类型迁移为消费者所在 `service.internal`、`repository.internal` 或具体技术实现包中的包级顶层类型；
- 不因独立文件而机械扩大为 `public`；
- 同一辅助类型被多个包真实消费时，先判断是否属于领域 DTO/VO/PO 或 Repository 契约，不直接丢入 `shared`；
- 顶层 `record` 保留原 component 顺序、紧凑构造器、不可变拷贝和校验；
- 顶层 class 保留框架要求的构造器、访问器和可变性；
- 不引入 Lombok 或代码生成。

### 5.3 类型分类和命名

| 目标目录 | 放置内容 | 命名规范 |
|---|---|---|
| `domain/dto` | HTTP Request、Command、Query、Mutation、Control、跨层输入 | 明确业务语义 + `DTO` |
| `domain/vo` | Response、View、Result、Page、Projection、Report | 明确业务语义 + `VO` |
| `domain/po` | JPA Entity、Embeddable、数据库持久化 Record | 明确业务语义 + `PO` |
| `domain/enums` | 业务、协议、持久化状态枚举 | 明确业务语义 + `Enum` |
| `domain/exception` | 本领域异常 | 明确业务语义 + `Exception` |
| `domain` | 有行为的 Aggregate、Value Object、Policy、Key、Revision | 使用真实领域名称，不强加数据载体后缀 |
| `repository` | Repository/Store/Port 契约 | 优先使用 `Repository` 后缀；保留具备明确端口含义的既有命名 |
| `repository.<technology>` | JPA/JDBC/Redis/HTTP/DDC/Outbox 实现 | 技术前缀 + 领域语义 + `Repository`/`Client`/`Publisher` |
| `service` | Service、Facade、业务编排 | 保留真实职责后缀，不承载 DTO/VO/PO 定义 |

禁止继续使用 `CreateRequest`、`Result`、`Page` 这类只能依靠宿主类区分的模糊顶层名。类型名必须带上业务语义，例如 `RoleAssignmentCreateDTO`、`AuthorizationMutationPageVO`。

### 5.4 代表性映射

| 当前类型 | 目标类型 |
|---|---|
| `RoleFacade.RoleView` | `role.domain.vo.RoleVO` |
| `RuntimeQueryService.MutationPage` | `runtime.domain.vo.AuthorizationMutationPageVO` |
| `AssignmentFacade.AssignRequest` | `assignment.domain.dto.RoleAssignmentDTO` |
| `SessionEntity` | `session.domain.po.SessionPO` |
| `SessionEntity.AuthenticationStrength` | `session.domain.enums.AuthenticationStrengthEnum` |
| `ConstraintFacade.ConstraintStore` | `constraint.repository.ConstraintRepository` |
| 当前 `ConstraintRepository` 实现 | `constraint.repository.jpa.JpaConstraintRepository` |
| `PostgresqlRoleClosureStore` | `role.repository.jdbc.PostgresqlRoleClosureRepository` |
| `GatewayAdminControlPlaneStatusClient` | `runtime.repository.http.GatewayAdminControlPlaneStatusClient` |
| `Rbac3RuntimeProjectionDeliveryHandler` | `runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler` |

代表性映射只说明规则，不替代实施前的 386 行逐类型清单。清单至少包含：原 FQCN、宿主、类型种类、可见性、消费者、目标 FQCN、目标类型形式、兼容性注意项和所属提交波次。

---

## 6. 分层依赖与调用规则

### 6.1 允许依赖

```text
controller
  -> 本领域 domain.dto / domain.vo
  -> 本领域 service

service
  -> 本领域 domain
  -> 本领域 repository 契约
  -> 已存在且确有必要的跨领域公开 service/repository 契约

repository.<technology>
  -> 本领域 repository 契约
  -> 本领域 domain.po / domain
  -> 框架、数据库和外部客户端

config
  -> 允许装配上述实现，是依赖规则的显式组合根例外
```

### 6.2 禁止依赖

- `domain` 不得依赖 `controller / service / repository`；
- Controller 不得直接注入或调用 `Jpa*Repository`、`Jdbc*Repository`、Redis/HTTP/DDC 实现；
- Service 不得依赖 Controller 类型或具体 Repository 实现；
- Repository 实现不得返回 `Controller.Type` 或 `Service.Type`；
- DTO/VO/PO 不得通过功能类限定名访问；
- 跨领域调用不得为了消除编译错误而任意提升可见性或搬入 `shared`；
- `config` 不得承载业务流程，只负责 Bean、Properties、安全和基础设施装配。

### 6.3 数据流

```text
HTTP / CLI / Message / Scheduled 入站
  -> controller
  -> DTO 校验与协议适配
  -> service / facade
  -> domain + repository contract
  -> repository technology implementation
  -> PO / 外部系统
  -> VO
  -> controller 输出
```

Message、Scheduled、Filter 和 CLI 都被视为入站适配器，按所属领域放在 `controller.message`、`controller.scheduled`、`controller.filter`、`controller.cli`。JPA、JDBC、Redis、HTTP、DDC 和 Outbox 是出站适配器，按所属领域放在 `repository.<technology>`。

---

## 7. 必须拆分的跨领域宿主

### 7.1 `TenantUserDirectoryController`

按现有 Route 和调用服务拆分为：

- `tenant.controller.TenantController`
- `identity.controller.UserDirectoryController`
- `directory.controller.DirectoryController`

拆分必须原样保留每个方法的 Route、HTTP Method、参数绑定、Validation、权限注解、返回状态和 JSON 结构。类级 Route 若当前共享，需转换为各新类中等价的类级/方法级组合，不能改变最终完整路径。

### 7.2 `AuditSimulationController`

拆分为：

- `audit.controller.AuditController`
- `simulation.controller.AuthorizationSimulationController`

审计查询和模拟执行不再共享宿主类，但保持原有 API 契约。

### 7.3 `Rbac3IdentitySessionQueryStore`

按其九组既有接口拆成领域适配器，分别进入 `bootstrap`、`tenant`、`identity`、`directory`、`auth`、`session`、`assignment` 的 `repository.jpa`。每个新类只实现所属领域契约；公共 SQL/查询辅助若确实复用，放在最小可见范围的 `repository.internal`，不重建一个跨领域巨型 Store。

### 7.4 `IdentityRepositories`

至少拆为：

- `identity.repository.jpa.JpaIdentityMappingRepository`
- `identity.repository.jpa.JpaPasswordCredentialRepository`
- 租户查询部分迁入 `tenant.repository.jpa` 的明确实现

拆分不改变现有事务传播、JPA Query、锁语义和返回排序。

### 7.5 不做机械拆分的类

`ConstraintFacade`、`RoleFacade`、`ManagementPolicyFacade` 在抽离嵌套类型后继续保留。Facade 是当前代码中有意义的编排边界；仅因文件原先较长而拆成更多 Service 会扩大风险且不解决本次核心问题。

---

## 8. 持久化与数据库兼容

### 8.1 JPA 类型重命名

JPA 类型从 `*Entity` 重命名为 `*PO` 时必须保留原 entity name。例如：

```java
@Entity(name = "SessionEntity")
@Table(name = "rbac3_session")
public class SessionPO {
}
```

这样现有使用 `SessionEntity` 的 JPQL entity name 可以保持不变，本次不同时承担 JPQL 全量改名。若某实体当前没有显式 `@Entity(name = ...)`，迁移时需补上与原简单类名一致的 name，并通过启动前的 JPA/查询测试验证。

### 8.2 冻结项

- `@Table`、`@Column`、`@JoinColumn`、`@Index` 和约束不变；
- ID 生成、版本字段、Converter、Enum 持久化方式不变；
- JPQL、Native SQL 的语义不变；
- Redis Key、Hash Field 和序列化业务字段不变；
- Outbox 表、事件业务字段、aggregate key 和幂等 key 不变；
- 不修改任何已有 Flyway 文件；
- 本次不创建新的 Flyway migration。

若实施时发现某个 PO 改名无法在不改变数据库或业务语义的情况下完成，必须暂停该波次并回到规格审查，不能顺带修改 schema。

---

## 9. HTTP、Gateway 与 Java API 兼容

### 9.1 HTTP 契约冻结

所有 Controller 搬迁和拆分都必须保持：

- 完整 URL Route；
- HTTP Method、Status 和 Header；
- Request/Response JSON 字段名、嵌套结构和空值行为；
- Jakarta Validation 注解和错误行为；
- Jackson 注解、日期/枚举序列化和 property order；
- Spring Security、权限表达式和 Gateway Operation 元数据；
- 分页、排序、过滤和默认值语义。

DTO/VO 的 Java 简单类名和 FQCN 允许改变，但其对外 JSON 契约不得改变。

### 9.2 Admin 模块外 Java 调用影响

基线源码扫描未发现其他 RBAC3 生产模块直接 import `rbac3.admin` 的 Java 类型，现有直接编译影响主要集中在 Admin 模块自身和测试。implementation plan 仍需在当时 HEAD 上重新执行全仓 import/FQCN 扫描，防止并发新增消费者。

本次不创建旧 FQCN 的永久兼容代理。若扫描发现真实外部 Java 消费者，应将该消费者的同步迁移列入对应波次；如果消费者无法同步修改，则必须回到用户审批，不得擅自扩大兼容层。

### 9.3 Gateway Schema 的受控预期差异

`GatewayJavaSchemaMapper.definitionKey` 使用 Java 简单类名以及 canonical FQCN 的哈希生成 schema definition key。DTO/VO 搬包或改名后：

- `$ref` definition key 会改变；
- definition SHA 可能改变；
- JSON 字段、required、type、format、enum、数组和嵌套语义必须保持等价；
- Gateway Operation 的名称、Route 和外部可见性必须保持不变。

因此不能用 schema 文本逐字节相等作为唯一验收。需同时执行：

1. Operation 集合、Route、权限和外部可见性对比；
2. 去除/归一化 definition key 与 `$ref` 名称后的 schema 语义对比；
3. 对 definition key 变化建立明确快照更新，禁止把字段丢失误判为正常 FQCN 变化。

`Rbac3GatewayDocumentCatalogContractTest` 当前硬编码 `interfaces.http` 扫描包，实施时必须改为扫描新的领域 Controller 包，且不能漏掉拆分后的 Controller。

---

## 10. JavaDoc 与 package-info 规范

- 每个实际创建的 package 必须有 `package-info.java`；
- package JavaDoc 使用中英双语说明职责、允许类型和依赖边界；
- 每个迁移或新建的顶层 Java 类型必须保留并完善中英双语类级 JavaDoc；
- DTO/VO/PO 的字段或 record component 说明要覆盖业务语义、是否可空、单位/格式和约束；
- public 方法继续使用项目既有中英双语格式说明用途、参数、返回和异常；
- 不为显而易见的 getter/setter 编写重复注释；
- 仅迁移位置时，不顺带改写与本次无关的业务说明。

`package-info.java` 只为真实 package 创建，不允许用文档文件人为维持空目录。

---

## 11. 结构回归测试

不新增 ArchUnit 或其他依赖，使用现有 JUnit、反射和源码/类路径扫描能力扩展 `AdminLayerBoundaryTest` 或增加同职责测试。

必须覆盖：

1. 所有生产顶层类 `getDeclaredClasses().length == 0`；
2. 每个生产 Java 文件只包含一个顶层类型声明；
3. 旧根包 `application / interfaces / infrastructure / integration / security / worker / snapshot` 不再存在生产类；
4. `domain` 不依赖 Controller、Service 或 Repository；
5. Controller 不依赖 Repository 实现；
6. Service 不依赖 Controller 或 Repository 实现；
7. 不再出现 `Controller.NestedType`、`Service.NestedType`、`Facade.NestedType` 等功能类限定类型引用；
8. `Rbac3AdminApplication` 仍位于根包并能覆盖所有新包的组件扫描；
9. 所有 Controller 都能被 Gateway Document Catalog 发现；
10. package tree 中没有只有占位文件而无真实职责的空包。

结构测试首先随第一波建立为限定范围的迁移护栏，随每个波次扩大覆盖；最终波次启用全模块“嵌套类型为零”和“旧根包为零”的强制断言。不能在迁移过程中用永久忽略列表掩盖未完成项。

---

## 12. 迁移波次与提交边界

每个波次对应一个独立提交。implementation plan 会把逐文件和逐类型清单分配到以下波次：

1. `shared / config / bootstrap` 基础结构、package JavaDoc 和限定范围结构守卫；
2. `tenant / identity / directory`，拆分 `TenantUserDirectoryController`、`IdentityRepositories` 和对应的 `Rbac3IdentitySessionQueryStore` 职责；
3. `auth / session`，迁移认证、Step-up、Refresh、Session 及持久化类型；
4. `resource / role`，迁移 Application、Resource、Permission、Manifest、Role 和 Closure；
5. `assignment / activation / constraint / management`；
6. `authorization / participation / audit / simulation`，拆分 `AuditSimulationController`；
7. `runtime` 吸收原 `snapshot / worker / integration.ddc|gateway|outbox|runtime`，完成消息和定时入口归位；
8. 删除全部旧技术根包，更新全量测试和文档，启用全局结构守卫并完成契约对比。

每个波次必须满足：

- 同一个类型的定义、消费者和测试在同一提交同步更新；
- 不提交仅移动一半、依赖下一提交才能编译的状态；
- 只移动本波次所属领域，不借机重构未进入波次的业务；
- 先迁移被依赖的顶层契约和数据类型，再迁移消费者；
- 每个提交完成 targeted compile/test 和 `git diff --check`；
- 最终提交完成模块全量编译、测试和结构/契约验收。

---

## 13. 验证方案与验收标准

### 13.1 每波验证

至少执行：

```bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -DskipTests compile
```

并运行该波次受影响测试及：

```bash
git diff --check
```

### 13.2 最终验证

```bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am test
```

最终还必须提供以下证据：

- 生产 Java 文件数、顶层类型数和嵌套类型数复扫，嵌套类型为 `0`；
- 旧一级技术根包生产类型数为 `0`；
- 全仓旧 FQCN/import 扫描无遗漏；
- 44 个持久化映射基线逐项核对，Entity/Table/Column/JPQL 语义未丢失；
- HTTP Route、Method、Status、JSON、Validation、权限契约测试通过；
- Gateway Operation 集合不变，归一化 Java Schema 语义对比通过；
- Redis 和 Outbox 相关测试通过；
- 中英双语 JavaDoc 和实际 package 的 `package-info.java` 覆盖检查通过；
- `git diff --check` 通过，工作区中不包含无关文件。

### 13.3 Definition of Done

只有同时满足以下条件才算完成：

1. 最终 package tree 符合第 4 节，且没有空包；
2. 所有 386 个基线嵌套类型均在逐类型清单中有唯一去向，最终复扫为零；
3. 不存在 `功能类.实体类` 形式的生产引用；
4. Controller、Domain、Repository、Service 依赖规则全部通过；
5. 四个指定跨领域宿主按第 7 节拆分；
6. HTTP、数据库、Redis、Outbox、权限和 Gateway Operation 契约保持兼容；
7. Gateway Schema 仅存在已解释的 definition key/FQCN 差异，归一化语义无变化；
8. 所有实际 package 和迁移类型具备符合项目规范的中英双语 JavaDoc；
9. 八个提交均可独立编译，不存在中间破损提交；
10. 模块编译、测试、结构守卫和契约验证全部通过。

---

## 14. 风险与控制

| 风险 | 控制措施 |
|---|---|
| 大量 FQCN 变化造成漏改 | implementation plan 固化 386 行清单；每波执行全仓旧 FQCN/import 搜索 |
| JPA 简单类名变化破坏 JPQL | `@Entity(name = 原简单类名)`；逐查询测试和持久化映射核对 |
| Controller 拆分造成 Route 拼接变化 | 输出完整 Route 清单，拆分前后逐项对比 |
| Jackson/Validation 注解在 record 拆出时丢失 | 逐 component 迁移并运行 HTTP 契约测试 |
| Gateway Schema 快照大面积变化掩盖真实回归 | definition key 归一化后比较字段语义，单独审查预期 key 差异 |
| 跨领域巨型 Store 拆分改变事务或查询 | 按原接口和方法群机械迁移，保留查询、锁和事务注解 |
| 为解决包可见性而扩大 public API | 私有辅助类型优先变为同包 package-private 顶层类型 |
| `shared` 演变为垃圾包 | 进入 `shared` 前必须记录至少两个真实跨领域消费者和无合理领域归属的依据 |
| 八波迁移期间新代码继续写入旧包 | 第一波引入渐进结构守卫，最终禁止全部旧根包 |
| 并发工作改变基线 | 每波开始前复查 HEAD/status；只提交本波路径，不覆盖无关改动 |

发生失败时以波次提交为回滚单元。禁止通过修改数据库、降低结构测试、保留永久旧包代理或删除契约断言来“修复”迁移失败。

---

## 15. 设计模式取舍

本次保留并利用现有 Facade 和 Ports/Adapters 结构：

- Facade 继续承担跨多个内部服务的业务编排；
- Repository Contract 与 JPA/JDBC/Redis/HTTP/DDC/Outbox Adapter 分离；
- `config` 作为组合根完成实现装配。

未新增 Strategy、Factory、Template Method、Command Bus 或 Domain Event 模式。当前问题是类型嵌套、包归属和少数宿主职责过宽，领域垂直分包、顶层类型独立化和按既有接口拆适配器已经足够；新增模式会增加类型数量、迁移面和验证成本。现有确有变化点的 `IdentityAuthenticatorStrategy` 保留，不因本次改造重命名或重构。

---

## 16. 书面审查后的下一步

本规格获用户批准后，使用 implementation planning 流程产出实施计划。计划必须包含：

1. 基于批准时 HEAD 重新生成的生产文件、嵌套类型、持久化类型和测试基线；
2. 全部嵌套类型逐行迁移清单，不得只列代表性类型；
3. 每个现有生产类的目标文件和 package；
4. 四个跨领域宿主的逐方法拆分表；
5. 八个提交的精确文件范围、依赖顺序和提交消息；
6. 每个提交对应的测试、编译、结构扫描和契约验证命令；
7. Gateway Schema definition key 的预期变化清单和语义归一化对比方法；
8. 风险点、停止条件和回滚边界。

在书面规格再次获批之前，不进入生产代码迁移。
