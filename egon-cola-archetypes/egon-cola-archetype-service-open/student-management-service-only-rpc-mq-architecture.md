# 学生管理系统纯 Service 多 Project 架构说明文档（RPC / MQ 版）

## 1. 综述

本文档定义一种适用于大型业务系统的纯 Service 形态架构。

这里的纯 Service 指：

```text
1. 不对外提供 HTTP 接口。
2. 不提供 Controller。
3. 不提供 Web Filter。
4. 不提供 GraphQL。
5. 对外只通过 RPC 或 MQ 暴露服务能力。
6. adapter 层只保留 RPC 入站、MQ 入站、Facade 实现、DTO 转换、入站异常处理等能力。
```

该架构适合以下场景：

```text
1. 内部后端服务。
2. 中台服务。
3. 任务处理服务。
4. 事件消费服务。
5. Dubbo / gRPC Provider 服务。
6. RocketMQ / Kafka / RabbitMQ Consumer 服务。
7. 不希望该服务直接暴露 Web API 的系统。
```

整体架构仍然保持轻量领域分层：

```text
starter         启动装配层
adapter         入站适配层，仅 RPC / MQ
application     应用编排层
domain          领域核心层
infrastructure  基础设施层
common          通用基础层
```

RPC 对外契约不再由生成项目内的本地模块持有，而是依赖两个独立发布的规范契约：

```text
top.egon:egon-cola-organization-facade
top.egon:egon-cola-evaluation-facade
```

纯 Service 架构下，adapter 层不再包含：

```text
controller
web
filter
graphql
web handler
vo
```

adapter 层只保留以下职责，并统一放在业务领域之下：

```text
<domain>/rpc          RPC 入站
<domain>/mq           MQ 入站
<domain>/facade/impl  Facade 实现，只能放在 adapter
<domain>/converter    入站对象转换
<domain>/dto          入站 DTO / Message DTO
handler               跨领域 RPC / MQ 异常处理器，不处理 Web
```

本示例按两个独立 Project 展开：

```text
student-management-organization
student-management-evaluation
```

其中：

```text
student-management-organization
    - user 领域
    - teaching 领域

student-management-evaluation
    - course 领域
    - exam 领域
```

说明：

```text
1. student-management-organization 是一个独立工程，不是根聚合工程下的普通模块。
2. student-management-evaluation 是另一个独立工程，不是根聚合工程下的普通模块。
3. 每个 Project 内部再拆 starter / common / application / domain / infrastructure / adapter 等 Maven 子模块。
4. 每个 Project 内部可以包含多个相近领域。
```

---

## 2. 依赖关系

## 2.1 单个 Project 内部依赖关系

以 `student-management-organization` 为例，依赖关系如下：

```text
student-management-organization-starter import student-management-organization-adapter
student-management-organization-starter import student-management-organization-infrastructure

student-management-organization-adapter import student-management-organization-application
student-management-organization-adapter import top.egon:egon-cola-organization-facade

student-management-organization-application import student-management-organization-domain

student-management-organization-domain import student-management-organization-common

student-management-organization-infrastructure import student-management-organization-domain
```

规范 Facade artifact 是生成项目之外的自包含契约，内部有自己的：

```text
utils
enums
exceptions
dto
```

也就是说：

```text
canonical facade 是自包含契约 artifact。
canonical facade 不依赖生成项目的 common。
canonical facade 不依赖 application。
canonical facade 不依赖 domain。
canonical facade 不依赖 infrastructure。
生成项目只依赖 canonical facade；canonical facade 不反向依赖生成项目。
```

## 2.2 依赖关系图

```text
Canonical Provider Facade -> adapter -> application -> domain -> common
                                      starter
                                         |
                             adapter ----+---- infrastructure
                                                |
                                              domain
Canonical Consumer Facade -> infrastructure
```

说明：

```text
1. starter 负责启动和装配 adapter、infrastructure。
2. adapter 负责 RPC / MQ 入站，依赖 application 和对应的 canonical facade。
3. canonical facade 是独立发布的 RPC 契约，不依赖任何生成项目模块。
4. application 负责编排业务流程，依赖 domain。
5. domain 负责核心规则，依赖 common。
6. infrastructure 负责技术实现，依赖 domain。
```

## 2.3 两个独立 Project 的依赖边界

两个 Project 分别独立构建、独立启动、独立部署。

```text
student-management-organization
student-management-evaluation
```

它们不是下面这种结构：

```text
student-management
    - student-management-organization
    - student-management-evaluation
```

而是两个独立工程：

```text
student-management-organization/
    pom.xml
    student-management-organization-starter/
    student-management-organization-common/
    student-management-organization-application/
    student-management-organization-domain/
    student-management-organization-infrastructure/
    student-management-organization-adapter/

student-management-evaluation/
    pom.xml
    student-management-evaluation-starter/
    student-management-evaluation-common/
    student-management-evaluation-application/
    student-management-evaluation-domain/
    student-management-evaluation-infrastructure/
    student-management-evaluation-adapter/
```

如果两个 Project 之间需要调用，不能直接依赖对方的 application、domain、infrastructure。

推荐方式：

```text
1. 通过独立发布的 canonical facade 进行 RPC 调用。
2. 通过 MQ 进行事件通知。
3. 通过 infrastructure.client.impl 封装外部调用细节。
```

例如：

```text
student-management-evaluation-infrastructure
    -> top.egon:egon-cola-organization-facade
    -> OrganizationUserFacade
    -> student-management-organization-adapter/user/facade/impl
    -> student-management-organization-application
```

---

## 3. 模块说明

## 3.1 starter 模块

### 3.1.1 职责

`starter` 是启动装配层，只负责应用启动和业务无关配置。

纯 Service 项目中，`starter` 不启动 Web 服务能力，不配置 MVC，不配置 WebFlux Controller。

### 3.1.2 推荐结构

```text
starter
    - 启动类
    - config
    - resources
    - package-info.java
```

### 3.1.3 能做什么

```text
1. 放 Spring Boot 启动类。
2. 放 application.yml。
3. 放 bootstrap.yml。
4. 放 logback-spring.xml。
5. 放 RPC Provider 配置。
6. 放 MQ Consumer 配置。
7. 放基础设施装配配置。
8. 放应用启动参数配置。
9. 扫描 adapter、application、infrastructure。
```

### 3.1.4 不能做什么

```text
1. 不写 Controller。
2. 不写 Web Filter。
3. 不写 GraphQL Resolver。
4. 不写业务逻辑。
5. 不写 Repository。
6. 不写 MQ Consumer 业务处理逻辑。
7. 不写 RPC Provider 业务处理逻辑。
```

---

## 3.2 adapter 模块

### 3.2.1 职责

`adapter` 是 RPC / MQ 入站适配层。所有业务代码先按领域归属，再在领域内按技术职责拆分。

### 3.2.2 推荐结构

```text
adapter
    - <domain>
        - facade
            - impl
        - rpc
        - mq
        - converter
        - dto
        - validators
    - handler
    - package-info.java
```

没有实际代码的职责目录不创建。例如某领域没有入站 Message DTO，就不创建空的 `dto` 或 `mq` 包。

明确不允许出现：

```text
adapter/controller
adapter/web
adapter/filter
adapter/graphql
adapter/vo
以技术职责作为一级目录、再在其下放领域目录
```

### 3.2.3 能做什么

```text
1. 暴露 Dubbo Triple 或 gRPC Provider。
2. 实现 Facade 接口。
3. 消费 Kafka / RocketMQ / RabbitMQ 等入站消息。
4. 将 RPC DTO 或 Message DTO 转换为 Application 入参。
5. 将 Application 返回值转换为 Facade DTO。
6. 完成入站校验、异常映射和消费重试入口。
```

### 3.2.4 不能做什么

```text
1. 不提供 HTTP Controller、Web Filter、Web Interceptor 或 GraphQL Resolver。
2. 不定义 VO。
3. 不直接调用 Mapper、JPA Repository、RedisTemplate 或 MQ Producer。
4. 不写核心业务规则。
5. 不绕过 Application 直接调用 Domain。
```

### 3.2.5 调用链路

```text
RPC Provider -> Application
FacadeImpl   -> Application
MQ Consumer  -> Application
```

---

## 3.3 canonical facade 契约 artifact

### 3.3.1 职责

canonical facade 是独立发布的 RPC 对外契约，不属于任何生成项目的本地 Maven 模块。Facade 接口直接放在业务领域下，DTO 再放在该领域的 `dto` 子包中。

### 3.3.2 推荐结构

```text
top.egon.cola.<bounded-context>.facade
    - <domain>
        - XxxFacade.java
        - dto
    - dto
    - enums
    - exceptions
    - utils
    - package-info.java
```

共享目录只保存确实跨领域的契约；Facade 接口和领域 DTO 不再按技术职责优先排列。

### 3.3.3 能做什么

```text
1. 定义 RPC 接口、请求 DTO、响应 DTO。
2. 定义对外枚举、异常和轻量工具。
3. 以 `top.egon:egon-cola-organization-facade` 或 `top.egon:egon-cola-evaluation-facade` 供 Provider 和 Consumer 共同依赖。
```

### 3.3.4 不能做什么

```text
1. 不写 Facade 实现类。
2. 不依赖任何生成项目的 Common、Application、Domain、Infrastructure 或 Adapter。
3. 不写业务、数据库、缓存或 MQ 消费逻辑。
```

---

## 3.4 application 模块

### 3.4.1 职责

`application` 是应用编排层，负责组织完整业务用例，不关心请求来自 RPC 还是 MQ。

### 3.4.2 推荐结构

```text
application
    - <domain>
        - command
        - converter
        - manage
            - impl
        - query
        - result
        - validators
    - config
    - exceptions
    - result
    - package-info.java
```

正确方向是 `application/<domain>/manage/impl`，不是 `application/manage/<domain>` 或 `application/manage/impl/<domain>`。

### 3.4.3 能做什么

```text
1. 编排业务流程和事务边界。
2. 调用 Domain Service 与 Domain Repository 接口。
3. 做应用级校验、权限、幂等和流程前置校验。
4. 完成入参、领域模型和结果模型之间的转换。
5. 聚合多个领域完成一个业务用例。
```

### 3.4.4 不能做什么

```text
1. 不直接调用 Mapper、JPA Repository、RedisTemplate 或 MQ Template。
2. 不直接调用外部 RPC 技术实现。
3. 不写 RPC Provider、MQ Consumer 或 Web 相关逻辑。
```

---

## 3.5 domain 模块

### 3.5.1 职责

`domain` 是领域核心层，负责实体、聚合、值对象、领域事件、领域服务、仓储端口和领域规则。

### 3.5.2 推荐结构

```text
domain
    - <domain>
        - aggregates
        - entities
        - enums
        - event
        - repos
        - service
        - validators
        - vos
    - common
    - client
        - <external-system>
    - package-info.java
```

业务领域必须位于技术职责之前，例如 `domain/exam/entities`。外部系统端口是明确例外，保留在 `domain/client/<external-system>`，用于表达消费方拥有的 Anti-Corruption Layer 边界。

### 3.5.3 能做什么

```text
1. 定义领域实体、聚合、值对象和事件。
2. 定义并实现领域服务。
3. 定义领域仓储和外部能力端口。
4. 定义领域校验器、枚举和核心业务规则。
```

### 3.5.4 不能做什么

```text
1. 不依赖 Application、Infrastructure、Adapter 或 Facade。
2. 不依赖 JPA、MyBatis、Redis、MQ、Dubbo 或 gRPC 技术实现。
```

---

## 3.6 infrastructure 模块

### 3.6.1 职责

`infrastructure` 是基础设施层，负责数据库、缓存、MQ 出站、外部 RPC 调用和第三方 SDK 的技术实现。

### 3.6.2 推荐结构

```text
infrastructure
    - <domain>
        - repo
            - impl
            - po
            - jpa
            - converter
        - mq
            - message
    - client
        - <external-system>
    - config
    - aop
    - validators
    - package-info.java
```

业务持久化与出站消息按 `infrastructure/<domain>/<responsibility>` 排列。外部系统适配器是明确例外，保留在 `infrastructure/client/<external-system>`，与 Domain 端口共同组成 Anti-Corruption Layer。

### 3.6.3 能做什么

```text
1. 实现 Domain Repository 与外部能力端口。
2. 调用 JPA Repository、Mapper、外部 Facade、Dubbo、gRPC 或 HTTP Client。
3. 发送出站 MQ 消息。
4. 封装缓存和基础设施配置。
```

### 3.6.4 不能做什么

```text
1. 不写核心业务规则。
2. 不消费入站 MQ 消息或暴露 RPC Provider。
3. 不实现 Facade 接口。
4. 不处理 Web 请求。
5. 不让 Application 或 Domain 感知基础设施实现。
```

---

## 3.7 common 模块

### 3.7.1 职责

`common` 是当前 Project 内部通用基础层。

注意：`common` 只供当前生成项目内部使用。独立发布的 canonical facade 有自己的 utils、enums、exceptions，不依赖该模块。

### 3.7.2 推荐结构

```text
common
    - constants
    - utils
    - enums
    - exceptions
    - package-info.java
```

### 3.7.3 能做什么

```text
1. 放当前 Project 内部通用常量。
2. 放当前 Project 内部通用工具类。
3. 放当前 Project 内部基础异常。
4. 放当前 Project 内部基础枚举。
5. 给 domain、application、infrastructure 等内部模块复用。
```

### 3.7.4 不能做什么

```text
1. 不放 facade 契约对象。
2. 不放 facade 对外异常。
3. 不放 facade 对外枚举。
4. 不放具体业务状态枚举。
5. 不放业务规则工具类。
6. 不放 Redis 业务 Key。
7. 不放数据库表名常量。
```

---

## 4. 结构示例 + 命名示例

## 4.1 Project 结构

本架构下是两个独立 Project：

```text
student-management-organization/
student-management-evaluation/
```

不是一个总根工程下面挂两个子模块。

错误示例：

```text
student-management/
├── student-management-organization
└── student-management-evaluation
```

正确示例：

```text
student-management-organization/
├── pom.xml
├── student-management-organization-starter
├── student-management-organization-common
├── student-management-organization-domain
├── student-management-organization-application
├── student-management-organization-infrastructure
└── student-management-organization-adapter

student-management-evaluation/
├── pom.xml
├── student-management-evaluation-starter
├── student-management-evaluation-common
├── student-management-evaluation-domain
├── student-management-evaluation-application
├── student-management-evaluation-infrastructure
└── student-management-evaluation-adapter
```

---

## 4.2 student-management-organization 结构示例

`student-management-organization` 包含 `user` 与 `teaching` 两个领域。以下示例强调同一条规则：领域名先于技术职责。

```text
student-management-organization
├── student-management-organization-starter
│   └── src/main/java/com/example/student/organization/starter
│       ├── OrganizationServiceApplication.java
│       └── config
├── student-management-organization-common
│   └── src/main/java/com/example/student/organization/common
│       ├── constants
│       ├── enums
│       ├── exceptions
│       └── utils
├── student-management-organization-domain
│   └── src/main/java/com/example/student/organization/domain
│       ├── user
│       │   ├── aggregates
│       │   ├── entities
│       │   ├── enums
│       │   ├── events
│       │   ├── repos
│       │   ├── service
│       │   ├── validators
│       │   └── vos
│       ├── teaching
│       │   ├── aggregates
│       │   ├── entities
│       │   ├── enums
│       │   ├── events
│       │   ├── repos
│       │   ├── service
│       │   ├── validators
│       │   └── vos
│       └── client
│           └── evaluation
├── student-management-organization-application
│   └── src/main/java/com/example/student/organization/application
│       ├── user
│       │   ├── command
│       │   ├── converter
│       │   ├── manage
│       │   │   └── impl
│       │   ├── query
│       │   ├── result
│       │   └── validators
│       ├── teaching
│       │   ├── command
│       │   ├── converter
│       │   ├── manage
│       │   │   └── impl
│       │   ├── query
│       │   ├── result
│       │   └── validators
│       ├── config
│       ├── exceptions
│       └── result
├── student-management-organization-infrastructure
│   └── src/main/java/com/example/student/organization/infrastructure
│       ├── user
│       │   ├── repo
│       │   │   ├── impl
│       │   │   ├── po
│       │   │   ├── jpa
│       │   │   └── converter
│       │   └── mq
│       ├── teaching
│       │   ├── repo
│       │   │   ├── impl
│       │   │   ├── po
│       │   │   ├── jpa
│       │   │   └── converter
│       │   └── mq
│       └── client
│           └── evaluation
└── student-management-organization-adapter
    └── src/main/java/com/example/student/organization/adapter
        ├── user
        │   ├── facade
        │   │   └── impl
        │   ├── rpc
        │   ├── mq
        │   ├── converter
        │   ├── dto
        │   └── validators
        ├── teaching
        │   ├── facade
        │   │   └── impl
        │   ├── rpc
        │   ├── mq
        │   ├── converter
        │   ├── dto
        │   └── validators
        └── handler
```

`domain/client/evaluation` 与 `infrastructure/client/evaluation` 是跨 Project Anti-Corruption Layer，不按本地业务领域重排。所有测试包镜像生产领域路径，并为实际含 Java 类的目录提供 `package-info.java`。

---

## 4.3 student-management-evaluation 结构示例

`student-management-evaluation` 使用 `course` 与 `exam` 两个领域。下面是当前 Service archetype 的规范布局：

```text
student-management-evaluation
├── student-management-evaluation-starter
│   └── src/main/java/com/example/student/evaluation/starter
│       ├── EvaluationServiceApplication.java
│       └── config
├── student-management-evaluation-common
│   └── src/main/java/com/example/student/evaluation/common
│       ├── constants
│       ├── enums
│       ├── exceptions
│       └── utils
├── student-management-evaluation-domain
│   └── src/main/java/com/example/student/evaluation/domain
│       ├── course
│       │   ├── aggregates
│       │   ├── entities
│       │   ├── enums
│       │   ├── event
│       │   ├── repos
│       │   ├── service
│       │   ├── validators
│       │   └── vos
│       ├── exam
│       │   ├── aggregates
│       │   ├── entities
│       │   ├── enums
│       │   ├── event
│       │   ├── repos
│       │   ├── service
│       │   ├── validators
│       │   └── vos
│       ├── common
│       └── client
│           └── organization
├── student-management-evaluation-application
│   └── src/main/java/com/example/student/evaluation/application
│       ├── course
│       │   ├── command
│       │   ├── converter
│       │   ├── manage
│       │   │   └── impl
│       │   ├── query
│       │   ├── result
│       │   └── validators
│       ├── exam
│       │   ├── command
│       │   ├── converter
│       │   ├── manage
│       │   │   └── impl
│       │   ├── query
│       │   ├── result
│       │   └── validators
│       ├── config
│       ├── exceptions
│       └── result
├── student-management-evaluation-infrastructure
│   └── src/main/java/com/example/student/evaluation/infrastructure
│       ├── course
│       │   ├── repo
│       │   │   ├── impl
│       │   │   ├── po
│       │   │   ├── jpa
│       │   │   └── converter
│       │   └── mq
│       │       └── message
│       ├── exam
│       │   ├── repo
│       │   │   ├── impl
│       │   │   ├── po
│       │   │   ├── jpa
│       │   │   └── converter
│       │   └── mq
│       │       └── message
│       ├── client
│       │   └── organization
│       ├── config
│       ├── aop
│       └── validators
└── student-management-evaluation-adapter
    └── src/main/java/com/example/student/evaluation/adapter
        ├── course
        │   ├── facade
        │   │   └── impl
        │   ├── converter
        │   └── validators
        ├── exam
        │   ├── facade
        │   │   └── impl
        │   ├── converter
        │   ├── dto
        │   ├── mq
        │   └── validators
        └── handler
```

Adapter 实现 `top.egon.cola.evaluation.facade.course` 与 `top.egon.cola.evaluation.facade.exam` 中的契约。外部 Organization 边界继续保留在 `domain/client/organization` 与 `infrastructure/client/organization`，不混入本地 `course` 或 `exam` 领域。

该工程保持纯 Service：不创建业务 Controller、Web、Filter、GraphQL 或 VO 包；业务流量只通过 Dubbo Triple 或 RabbitMQ 进入。

---

## 5. 开发约束

## 5.1 纯 Service 入口约束

```text
1. 不提供 HTTP Controller。
2. 不提供 Web Filter。
3. 不提供 Web Interceptor。
4. 不提供 GraphQL Resolver。
5. 对外只提供 RPC Provider 或 MQ Consumer。
6. 所有入站请求必须经过 adapter。
7. adapter 只能调用 application。
```

## 5.2 RPC 约束

```text
1. RPC 契约定义在 facade。
2. RPC 实现只能放在 `adapter/<domain>/facade/impl`。
3. RPC Provider 只能调用 application。
4. RPC DTO 只能用于 facade 和 adapter 边界。
5. application 不允许直接依赖 RPC 技术实现。
```

允许：

```text
FacadeImpl -> Application Manage
RpcProvider -> Application Manage
```

不允许：

```text
RpcProvider -> Domain Service
RpcProvider -> RepositoryImpl
RpcProvider -> Mapper
RpcProvider -> RedisTemplate
```

## 5.3 MQ 入站约束

```text
1. adapter.mq 只负责入站消息消费。
2. MQ Consumer 只能调用 application。
3. MQ Consumer 不写核心业务规则。
4. MQ Consumer 不直接写数据库。
5. MQ Consumer 不直接操作 Redis。
6. MQ Consumer 不直接调用 RepositoryImpl。
```

允许：

```text
MQ Consumer -> Application Manage
```

不允许：

```text
MQ Consumer -> Mapper
MQ Consumer -> RepositoryImpl
MQ Consumer -> RedisTemplate
MQ Consumer -> Domain Repository
```

## 5.4 MQ 出站约束

```text
1. infrastructure.mq 只负责出站消息发送。
2. application 需要发送消息时，调用 application client / publisher 接口。
3. infrastructure 实现具体 MQ 发送。
4. application 不直接调用 KafkaTemplate / RabbitTemplate / RocketMQTemplate。
```

推荐：

```text
Application -> Application EventPublisher Interface -> Infrastructure MQ Producer
```

## 5.5 Application 约束

```text
1. application 负责业务用例编排。
2. application 可以调用 domain service。
3. application 可以调用 domain repository 接口。
4. application 可以调用 application client 接口。
5. application 负责事务控制。
6. application 不依赖 infrastructure。
7. application 不直接调用 mapper。
8. application 不直接调用 jpa repository。
9. application 不直接操作 RedisTemplate。
10. application 不直接调用 MQ Template。
```

## 5.6 Domain 约束

```text
1. domain 只表达核心业务规则。
2. domain 不依赖 adapter。
3. domain 不依赖 facade。
4. domain 不依赖 application。
5. domain 不依赖 infrastructure。
6. domain 不依赖 MyBatis-Plus。
7. domain 不依赖 JPA。
8. domain 不依赖 Redis。
9. domain 不依赖 MQ。
10. domain 不依赖 Dubbo / gRPC。
```

## 5.7 Infrastructure 约束

```text
1. infrastructure 负责技术实现。
2. infrastructure 实现 domain repository 接口。
3. infrastructure 实现 application client 接口。
4. infrastructure 可以调用 mapper。
5. infrastructure 可以调用 jpa repository。
6. infrastructure 可以调用 RedisTemplate。
7. infrastructure 可以发送出站 MQ。
8. infrastructure 可以调用外部 RPC / HTTP / SDK。
9. infrastructure 不写核心业务规则。
10. infrastructure 不消费入站 MQ。
11. infrastructure 不暴露 RPC Provider。
```

---

## 6. Validator 规范

## 6.1 Adapter Validator

纯 Service 项目中，Adapter Validator 负责 RPC / MQ 入站参数格式校验。

适合校验：

```text
1. RPC 请求字段是否为空。
2. RPC DTO 长度是否合法。
3. MQ 消息字段是否完整。
4. MQ 消息版本是否支持。
5. 枚举值是否合法。
6. 消息 traceId 是否存在。
```

不适合校验：

```text
1. 用户是否存在。
2. 课程是否允许发布。
3. 考试是否允许录入成绩。
4. 数据库唯一性是否冲突。
```

## 6.2 Application Validator

Application Validator 负责用例级校验。

适合校验：

```text
1. 当前操作是否允许执行。
2. 当前流程是否满足前置条件。
3. 当前请求是否重复提交。
4. 当前业务用例是否需要幂等控制。
5. 跨领域协作前置条件是否满足。
```

示例：

```text
1. 分配角色前校验角色是否可用。
2. 导入用户前校验导入任务是否重复。
3. 创建考试前校验课程是否存在。
4. 录入成绩前校验考试是否已发布。
```

## 6.3 Domain Validator

Domain Validator 负责领域不变量校验。

适合校验：

```text
1. 已禁用用户不能登录。
2. 已归档角色不能继续分配。
3. 已停用权限不能绑定角色。
4. 同一个班级不能重复绑定同一个年级规则。
5. 已结课课程不能继续安排考试。
6. 已发布成绩不能随意覆盖。
```

这类规则不应该只放在 RPC 或 MQ 入口，因为换一个入口后仍然必须成立。

## 6.4 Infrastructure Validator

Infrastructure Validator 负责技术适配校验。

适合校验：

```text
1. 外部 RPC 返回值是否合法。
2. 外部服务返回错误码转换。
3. 数据库唯一键冲突转换。
4. Redis 缓存结构是否合法。
5. MQ 发送结果是否合法。
6. 第三方系统字段是否缺失。
```

## 6.5 Validator 放置原则

```text
1. RPC / MQ 入站格式校验放 adapter。
2. 用例前置校验放 application。
3. 核心业务规则校验放 domain。
4. 外部系统、数据库、缓存、MQ 技术校验放 infrastructure。
```

判断标准：

```text
如果换成另一个入口后仍然必须校验，通常不要只放 adapter。
如果规则属于业务永恒规则，放 domain。
如果规则属于当前业务流程，放 application。
如果规则属于外部系统或技术实现，放 infrastructure。
```

---

## 7. 总结

纯 Service 架构的核心是：

```text
1. 不提供 HTTP。
2. 不提供 Controller。
3. 不提供 Web Filter。
4. 不提供 GraphQL。
5. 对外只提供 RPC 或 MQ。
6. adapter 只处理 RPC / MQ 入站。
7. canonical facade artifact 只定义 RPC 契约。
8. Facade 实现只能放在 `adapter.<domain>.facade.impl`。
9. MQ 入站放在 `adapter.<domain>.mq`。
10. MQ 出站放在 `infrastructure.<domain>.mq`。
```

两个 Project 的边界是：

```text
student-management-organization
    - user
    - teaching

student-management-evaluation
    - course
    - exam
```

每个 Project 内部保持完整分层：

```text
starter
common
domain
application
infrastructure
adapter
```

两个 canonical facade artifact 位于生成项目之外，由 Provider 与 Consumer 共同依赖。

最终调用链路：

```text
RPC -> Adapter -> Application -> Domain -> Repository Interface -> Infrastructure
MQ  -> Adapter -> Application -> Domain -> Repository Interface -> Infrastructure
```

最终依赖方向：

```text
starter -> adapter / infrastructure
adapter -> application / canonical provider facade
application -> domain
domain -> common
infrastructure -> domain / canonical consumer facade
```

一句话总结：

```text
这是一个没有 Web 入口的后端 Service 架构，入口只走 RPC / MQ，业务只进 Application，规则只沉 Domain，技术实现只放 Infrastructure。
```
