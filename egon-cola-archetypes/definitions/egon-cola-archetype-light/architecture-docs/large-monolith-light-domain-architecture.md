# 大型单体轻量领域分层架构说明文档

## 1. 综述

本架构适用于大型单体项目。这里的 `starter`、`adapter`、`facade`、`application`、`infrastructure`、`common`、`domain` 指的是*
*单体工程内部的逻辑包结构**，不是 Maven 子模块，也不是微服务拆分。

也就是说，工程只有一个主应用模块，例如：

```text
student-management
```

在这个单体工程内部，通过包结构划分职责：

```text
com.demo.student.start
com.demo.student.adapter
com.demo.student.facade
com.demo.student.application
com.demo.student.infrastructure
com.demo.student.common
com.demo.student.domain
```

该架构不是完整重型 DDD，也不是传统三层架构，而是一种更适合企业级大型单体长期演进的轻量领域分层架构。

传统三层架构通常是：

```text
controller -> service -> dao
```

在大型单体项目中，这种结构容易出现以下问题：

```text
1. service 层持续膨胀，最终变成上帝类。
2. mapper / dao 被多个入口直接调用，数据访问边界失控。
3. 业务规则和技术实现混在一起。
4. 多个业务领域之间互相穿透，依赖关系复杂。
5. 后续重构、拆模块、拆服务成本较高。
```

本架构在单体工程内部建立以下逻辑层：

```text
start           启动装配层
adapter         入站适配层
facade          对外契约层
application     应用编排层
infrastructure  基础设施层
common          通用基础层
domain          领域核心层
```

核心职责如下：

```text
start           负责启动类和业务无关配置
adapter         负责 Controller / MQ 入站 / RPC 入站 / GraphQL / Facade 实现
facade          负责对外接口契约、DTO、枚举、异常和工具
application     负责业务用例编排、事务、应用级校验、跨领域协调
infrastructure  负责数据库、缓存、外部调用、MQ 出站、基础设施配置
common          负责项目内部通用基础能力
domain          负责核心领域模型、领域服务、仓储接口、领域规则
```

对于大型单体项目，多领域不建议全部塞到同一个 `domain` 根包下面一层，而是按照领域继续分包，例如：

```text
com.demo.student.domain.user       用户、角色、权限
com.demo.student.domain.teaching   班级、课程
```

这样项目仍然是一个单体工程、一个应用、一个部署单元，但代码结构具备清晰的领域边界。后续如果某个领域需要拆成独立模块或独立服务，也可以从领域包开始逐步演进。

---

## 2. 依赖关系

### 2.1 包级依赖关系

当前架构的包级依赖关系如下：

```text
start import adapter infrastructure common.exception

adapter import application facade common.exception

application import domain common.exception

domain import common.exception

infrastructure import domain facade common.exception

common import none
```

注意：

```text
1. facade 只允许依赖 common 的异常根类型，其余 common 内容不得进入契约层。
2. facade 有自己的 dto / enums / utils / validation，异常统一放 common.exception。
3. application 不依赖 infrastructure。
4. domain 不依赖 application。
5. domain 不依赖 infrastructure。
6. infrastructure 依赖 domain，并只把 facade DTO 作为对外消息载荷。
7. start 只负责装配 adapter 和 infrastructure。
8. 任何层都不反向依赖 outer 层，common 不依赖任何业务层。
```

### 2.2 依赖关系图

```text
start -> adapter -> application -> domain -> common.exception
start -> infrastructure -> domain -> common.exception

adapter -> facade -> common.exception
infrastructure -> facade -> common.exception

common -> (无内部依赖)
```

### 2.3 主调用方向

系统主调用方向为：

```text
adapter -> application -> domain -> common.exception
```

对外契约方向为：

```text
adapter -> facade
infrastructure -> facade          // 仅把 facade DTO 作为对外消息载荷
facade -> common.exception        // 契约层只允许这一条 common 依赖
```

基础设施方向为：

```text
infrastructure -> domain
infrastructure -> facade      // 只作为对外消息载荷，不承载业务判断
```

启动装配方向为：

```text
start -> adapter
start -> infrastructure
```

### 2.4 单体内多领域关系

以学生管理系统为例：

```text
application import domain.user
application import domain.teaching

domain.user import common.exception
domain.teaching import common.exception

adapter import application
adapter import facade

facade import common.exception

infrastructure import domain
infrastructure import facade

start import adapter
start import infrastructure
```

领域之间不直接互相依赖。

不推荐：

```text
domain.user import domain.teaching
domain.teaching import domain.user
```

推荐由 application 层做跨领域编排：

```text
application
    -> domain.user
    -> domain.teaching
```

例如：

```text
给学生分配课程：

adapter.controller.teaching.CourseController
    -> application.manage.teaching.CourseManage
        -> domain.user.service.UserDomainService
        -> domain.teaching.service.CourseDomainService
        -> domain.teaching.service.CourseDomainService
```

### 2.5 依赖控制方式

因为当前是单体工程，不拆 Maven 子模块，所以不能只依赖 Maven 来限制模块边界。推荐通过以下方式控制依赖：

```text
1. 代码评审约束包依赖方向。
2. 使用 ArchUnit 做包依赖检查。
3. 禁止 Controller 直接调用 DAO / ServiceImpl。
4. 禁止 Application 直接调用 Infrastructure 技术实现。
5. 禁止 Domain 依赖 Spring MVC / MyBatis-Plus 实现 / JPA / Redis / MQ；只允许依赖 Common MP contract。
```

---

## 3. 模块说明

本章中的“模块”指单体工程内部的逻辑包，不是 Maven 子模块。

---

### 3.1 start

#### 3.1.1 职责

`start` 是启动装配层，只负责应用启动和业务无关配置。

#### 3.1.2 推荐结构

```text
start
    - StudentManagementApplication.java
    - config
```

#### 3.1.3 能做什么

```text
1. 放 Spring Boot 启动类。
2. 放启动装配配置。
3. 放业务无关的全局配置。
4. 放 JSON、OpenAPI、Actuator、线程池等启动级配置。
5. 负责扫描 adapter、application、infrastructure 等包。
```

#### 3.1.4 不能做什么

```text
1. 不写 Controller。
2. 不写 MQ Consumer。
3. 不写 RPC Provider。
4. 不写 Application Manage。
5. 不写 Domain Service。
6. 不写 Repository。
7. 不写具体业务逻辑。
```

---

### 3.2 adapter

#### 3.2.1 职责

`adapter` 是入站适配层，负责处理所有外部进入系统的请求。

包括：

```text
1. HTTP Controller。
2. MQ 入站消息消费。
3. RPC 入站请求。
4. GraphQL 查询入口。
5. Facade 接口实现。
6. 请求 DTO 与响应 VO 转换。
7. 入站异常处理和过滤器。
```

#### 3.2.2 推荐结构

```text
adapter
    - controller
    - mq
    - pojo.dto
    - pojo.vo
    - pojo.convertor
    - validators
    - graphql
    - facade.impl
    - handler
    - filter
```

原生 RPC 不再有独立的 `adapter/rpc` 包：`@EgonRpcProvider` 直接落在 `adapter/*/facade/impl` 的 Facade 实现类上，入站载体统一放 `pojo`。

#### 3.2.3 能做什么

```text
1. 接收 HTTP 请求。
2. 消费入站 MQ 消息。
3. 以 Facade 实现暴露原生 RPC Provider。
4. 暴露 GraphQL Resolver。
5. 实现 facade 接口。
6. 将外部请求 DTO 转换为 application 入参。
7. 将 application 返回值转换为 VO 或 Facade DTO。
8. 处理全局异常。
9. 处理 TraceId、登录态、租户上下文等入站上下文。
```

#### 3.2.4 不能做什么

```text
1. 不直接调用 mapper。
2. 不直接调用 DAO。
3. 不直接调用 MP Service。
4. 不直接操作 RedisTemplate。
5. 不直接发送 MQ。
6. 不直接写核心业务规则。
7. 不直接调用 domain service 实现。
8. 不绕过 application 调用 domain service。
9. 不在 application 里放 facade.impl。
```

#### 3.2.5 关键规则

```text
adapter/facade/impl 只能放 Facade 实现。
application 不能放 facade.impl。
```

调用链路：

```text
Controller -> Application
MQ Consumer -> Application
GraphQL Resolver -> Application
FacadeImpl -> Application                     // 原生 RPC Provider 注解在 FacadeImpl 上
```

---

### 3.3 facade

#### 3.3.1 职责

`facade` 是对外契约层，只定义对外接口、DTO、枚举、校验分组、契约工具。

它适合被 RPC、内部 SDK、其他系统调用方依赖。

#### 3.3.2 推荐结构

```text
facade
    - facade 定义
    - dto
    - enums
    - utils
    - validation            // 对外分组校验标记，供 DTO 与 adapter 共用
```

对外异常不放在 `facade`，统一收敛到 `common/exception`，例如 `UserFacadeException`、`TeachingFacadeException`。

#### 3.3.3 能做什么

```text
1. 定义 Facade 接口。
2. 定义对外请求 DTO。
3. 定义对外响应 DTO。
4. 定义对外枚举。
5. 定义对外分组校验标记。
6. 定义 Facade 内部轻量工具。
```

#### 3.3.4 不能做什么

```text
1. 不写 Facade 实现类。
2. 除 common.exception 的异常根类型外，不依赖 common 的其他内容。
3. 不依赖 application。
4. 不依赖 domain。
5. 不依赖 infrastructure。
6. 不依赖 adapter。
7. 不写业务逻辑。
8. 不写数据库逻辑。
9. 不写缓存逻辑。
```

---

### 3.4 application

#### 3.4.1 职责

`application` 是应用编排层，负责组织一次完整业务用例。

它不处理具体协议，不处理数据库细节，不处理缓存细节，不处理 MQ 发送细节，而是负责编排领域能力完成业务流程。

#### 3.4.2 推荐结构

```text
application
    - manage
        - impl
    - pojo.command
    - pojo.query
    - pojo.result
    - pojo.convertor
    - validators
```

用例载体统一放 `pojo`，`Command`、`Query`、`Result` 不再各自散落在 `application/<domain>` 顶层；快照投影由 `pojo.convertor` 的 `BaseForwardConverter` 承担，不设 `assemblers` 包。

其中每一层都必须按领域分包，`manage` 接口和实现按以下方式组织：

```text
application/user/manage/UserManage.java
application/user/manage/impl/UserManageImpl.java

application/teaching/manage/CourseManage.java
application/teaching/manage/impl/CourseManageImpl.java
```

不允许写成：

```text
application/manage/user/impl/UserManageImpl.java
application/manage/impl/user/UserManageImpl.java
```

#### 3.4.3 能做什么

```text
1. 编排业务流程。
2. 控制事务边界。
3. 调用 domain service。
4. 只通过 domain service 取得查询、事件发布和幂等能力。
5. 不感知 infrastructure DAO/PO。
6. 做应用级参数校验。
7. 做权限、幂等、流程前置校验。
8. 做 DTO、Command、Domain Model 的转换。
9. 聚合多个领域完成一个业务用例。
```

#### 3.4.4 不能做什么

```text
1. 不直接调用 mapper。
2. 不直接调用 DAO。
3. 不直接调用 MP Service。
4. 不直接操作 RedisTemplate。
5. 不直接使用 KafkaTemplate / RabbitTemplate。
6. 不直接调用外部 HTTP / RPC 实现。
7. 不写 Web 层逻辑。
8. 不关心请求来自 Controller、MQ、RPC 还是 GraphQL。
9. 不实现 Facade 接口。
10. 不包含 facade.impl 包。
```

---

### 3.5 infrastructure

#### 3.5.1 职责

`infrastructure` 是基础设施层，负责所有技术实现细节。

包括：

```text
数据库
缓存
MQ 出站
外部 RPC
外部 HTTP
第三方 SDK
Common MyBatis-Plus runtime
AOP
基础设施配置
```

#### 3.5.2 推荐结构

```text
infrastructure
    - user
        - dao
        - po
        - converter
        - repo
        - service
            - impl
        - client
            - impl
        - validators
    - teaching
        - dao
        - po
        - converter
        - repo
        - service
            - impl
        - client
            - impl
        - validators
    - aop
    - mq
        - impl
    - config
```

注意：领域目录在前，技术目录平级展开，例如：

```text
infrastructure/user/dao.*
infrastructure/user/po.*
infrastructure/user/repo.*
```

不允许写成：

```text
    repo.dao.user.*
    repo.po.user.*
    service.impl.user.*
    infrastructure/cache/*
```

缓存不构成独立包：读放大治理由 Repository 与 Common 缓存合同承接，`infrastructure` 下不设 `cache` 目录，也不定义 `*CachePort`。MQ 出站只保留 `mq` 与 `mq/impl`，不额外按领域复制一套 `mq` 目录。

#### 3.5.3 能做什么

```text
1. 实现 domain service 接口并组合具体 Repository；技术基类继承属于 Repository。
2. 使用 `EgonColaMapper` DAO 访问 MyBatis-Plus。
3. 实现 Domain 定义的查询、事件与幂等服务接口。
4. 调用外部 Facade。
5. 调用外部 HTTP / RPC / gRPC。
6. 发送出站 MQ 消息。
7. 通过 Repository 与 Common 缓存合同收敛读放大。
8. 定义基础设施相关配置。
9. 做数据库对象 PO 和领域对象之间的转换。
```

#### 3.5.4 不能做什么

```text
1. 不写核心业务规则。
2. 不处理入站 HTTP 请求。
3. 不消费入站 MQ 消息。
4. 不暴露 Controller。
5. 不暴露 Facade 实现。
6. 不让 application 直接感知 DAO / MyBatis-Plus / redis / mq。
7. 不让 domain 感知任何基础设施实现。
```

#### 3.5.5 repo 规范

推荐调用链路：

```text
Application
    -> Domain Service Interface
        -> Infrastructure ServiceImpl
            -> Repository (EgonColaRepository)
                -> DAO (EgonColaMapper)
                    -> Database
```

MyBatis-Plus 统一使用 Common starter：

```text
domain.user.service.UserDomainService
    -> infrastructure.user.service.impl.UserDomainServiceImpl
        -> infrastructure.user.repo.UserRepository
            -> infrastructure.user.dao.UserDAO (EgonColaMapper)
                -> infrastructure.user.po.UserPO (EgonModel)
```

`UserRepository` 必须 `extends EgonColaRepository<...>`，由它承接 Common 的租户、逻辑删除、乐观锁与缓存合同；`ServiceImpl` 不直接注入 DAO 以外的技术细节。

Application 只能依赖 Domain service 接口，不能直接调用 DAO。

---

### 3.6 common

#### 3.6.1 职责

`common` 是项目内部通用基础层，只放与具体业务无关、稳定复用的基础能力。

#### 3.6.2 推荐结构

```text
common
    - constants
    - utils
    - enums
    - exception
```

`exception` 是全部层次共享的异常根类型所在目录：`*UseCaseException`、`*DomainException`、`*FacadeException` 与 `ConfigDecryptException` 都定义在这里，并统一继承 Common 的 `BusinessException` / `CommonException`。

#### 3.6.3 能做什么

```text
1. 放通用常量。
2. 放通用工具类。
3. 放基础异常。
4. 放基础错误码。
5. 放通用枚举。
6. 放 Result、PageRequest、PageResult。
7. 放 TraceId、Date、String 等基础工具。
```

#### 3.6.4 不能做什么

```text
1. 不放具体业务枚举。
2. 不放携带业务判断的异常实现，`exception` 只提供各层共享的异常根类型。
3. 不放业务规则工具类。
4. 不放数据库表名常量。
5. 不放 Redis 业务 Key。
6. 不放领域模型。
7. 除 `exception` 目录外，不被 facade 依赖。
```

---

### 3.7 domain

#### 3.7.1 职责

`domain` 是领域核心层，负责表达业务模型、业务规则和业务不变量。

#### 3.7.2 推荐结构

```text
domain
    - user
        - entities
        - aggregates
        - vos
        - service
        - validators
        - enums
    - teaching
        - entities
        - aggregates
        - vos
        - service
        - validators
        - enums
```

注意：领域服务接口必须位于 `service`，实现必须位于 infrastructure 的 `service.impl`：

```text
service
infrastructure/.../service.impl
```

查询、事件发布、幂等都以领域服务接口表达（`*QueryService`、`*EventService`、`*IdempotencyService`），不设 `client`、`gateway`、`event`、`exceptions`、`pojo` 包；外部 HTTP 客户端属于 infrastructure 自己的 `client` 目录，不作为领域端口下沉到 domain。异常根类型统一在 `common/exception`。

不使用：

```text
domainservices
domainservicesimpl
```

#### 3.7.3 能做什么

```text
1. 定义领域实体。
2. 定义聚合。
3. 定义值对象。
4. 定义仅包含领域语义的服务接口；Repository 承接 Common 技术合同。
5. 以领域服务接口表达查询、事件发布与幂等能力，实现落在 infrastructure。
6. 定义领域校验器。
7. 定义领域枚举。
8. 表达核心业务规则。
9. 维护领域对象内部状态一致性。
```

#### 3.7.4 不能做什么

```text
1. 不依赖 Spring MVC。
2. 不依赖 MyBatis-Plus 实现、DAO 或 Mapper。
3. 不依赖 JPA。
4. 不依赖 Redis。
5. 不依赖 MQ。
6. 不依赖 HTTP Client。
7. 不依赖 Egon RPC / gRPC。
8. 不依赖 infrastructure。
9. 不依赖 adapter。
10. 不依赖 application。
11. 不依赖 facade。
12. 不把 PO、DAO 或 `EgonColaRepository` 放入 domain。
```

---

## 4. 结构示例 + 命名示例

示例系统：学生管理系统。

业务领域：

```text
1. 用户权限领域：user + role + permission
2. 教学管理领域：class + course
```

说明：`class` 是 Java 关键字，代码中不建议直接使用 `Class` 作为业务类名。本文统一使用 `SchoolClass`。

本示例是**单体单模块结构**，不是 Maven 多子模块结构。

```text
student-management
├── pom.xml                                               // 单体工程 Maven 配置，不拆子模块
├── README.md                                             // 项目说明
├── .gitignore                                            // Git 忽略配置
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── demo
│   │   │           └── student                           // 学生管理系统根包说明
│   │   │               ├── package-info.java             // 根包说明
│   │   │               │
│   │   │               ├── start                         // 启动装配层包说明
│   │   │               │   ├── package-info.java         // 启动装配层包说明
│   │   │               │   ├── StudentManagementApplication.java // Spring Boot 启动类
│   │   │               │   └── config                    // 启动层业务无关配置包说明
│   │   │               │       ├── package-info.java     // 启动层配置包说明
│   │   │               │       ├── JacksonConfig.java    // JSON 序列化配置
│   │   │               │       ├── OpenApiConfig.java    // OpenAPI / Swagger 配置
│   │   │               │       ├── ActuatorConfig.java   // 监控端点配置
│   │   │               │       ├── NativeRpcConfiguration.java // 原生 RPC 装配
│   │   │               │       ├── NativeHttpSecurityConfiguration.java // 原生 HTTP 与 RPC 边界装配
│   │   │               │       ├── async                 // 异步执行配置包说明
│   │   │               │       │   └── AsyncConfiguration.java // 线程池与异步配置
│   │   │               │       └── encryption            // 配置密文解密包说明
│   │   │               │           ├── ConfigDecryptor.java // 密文解密契约
│   │   │               │           ├── AesGcmConfigDecryptor.java // AES-GCM 解密实现
│   │   │               │           ├── ConfigDecryptEnvironmentPostProcessor.java // 启动期密文回填
│   │   │               │           └── ConfigCipherCli.java // 密文生成命令行工具
│   │   │               │
│   │   │               ├── adapter                       // 入站适配层包说明
│   │   │               │   ├── package-info.java         // 入站适配层包说明
│   │   │               │   ├── user                      // 用户权限入站包说明
│   │   │               │   │   ├── package-info.java     // 用户权限入站包说明
│   │   │               │   │   ├── controller            // 用户权限 HTTP 入口包说明
│   │   │               │   │   │   ├── UserController.java // 用户接口
│   │   │               │   │   │   ├── RoleController.java // 角色接口
│   │   │               │   │   │   └── PermissionController.java // 权限接口
│   │   │               │   │   ├── mq                    // 用户权限 MQ 入站消费包说明，仅入站
│   │   │               │   │   │   └── UserImportedConsumer.java // 用户导入完成消息消费者
│   │   │               │   │   ├── graphql               // 用户 GraphQL Resolver 包说明
│   │   │               │   │   │   └── UserResolver.java // 用户 GraphQL 查询入口
│   │   │               │   │   ├── facade                // 用户 Facade 入站实现包说明
│   │   │               │   │   │   └── impl              // Facade 实现包说明，原生 @EgonRpcProvider 落在这里
│   │   │               │   │   │       ├── UserFacadeImpl.java // 用户 Facade 实现，同时是原生 RPC Provider
│   │   │               │   │   │       └── PermissionFacadeImpl.java // 权限 Facade 实现，同时是原生 RPC Provider
│   │   │               │   │   ├── pojo                  // 用户入站载体包说明
│   │   │               │   │   │   ├── dto               // 用户入参 DTO 包说明
│   │   │               │   │   │   │   ├── CreateUserRequest.java // 创建用户请求
│   │   │               │   │   │   │   ├── AssignRoleRequest.java // 分配角色请求
│   │   │               │   │   │   │   └── GrantPermissionRequest.java // 授权权限请求
│   │   │               │   │   │   ├── vo                // 用户出参 VO 包说明
│   │   │               │   │   │   │   ├── UserDetailVO.java // 用户详情响应
│   │   │               │   │   │   │   └── PermissionTreeVO.java // 权限树响应
│   │   │               │   │   │   └── convertor         // 用户 Adapter 转换器包说明
│   │   │               │   │   │       └── UserAdapterConvertor.java // 用户请求/响应单向转换器
│   │   │               │   │   └── validators            // 用户入站校验器包说明
│   │   │               │   │       └── UserRequestValidator.java // 用户 HTTP/RPC 入参校验器
│   │   │               │   ├── teaching                  // 教学管理入站包说明
│   │   │               │   │   ├── controller            // 教学 HTTP 入口包说明
│   │   │               │   │   │   ├── SchoolClassController.java // 班级接口，避免使用 ClassController
│   │   │               │   │   │   └── CourseController.java // 课程接口
│   │   │               │   │   ├── mq
│   │   │               │   │   │   └── CourseImportedConsumer.java // 课程导入完成消息消费者
│   │   │               │   │   ├── graphql
│   │   │               │   │   │   └── CourseResolver.java // 课程 GraphQL 查询入口
│   │   │               │   │   ├── facade
│   │   │               │   │   │   └── impl              // 原生 RPC Provider 注解在 FacadeImpl 上
│   │   │               │   │   │       ├── SchoolClassFacadeImpl.java // 班级 Facade 实现
│   │   │               │   │   │       └── CourseFacadeImpl.java // 课程 Facade 实现
│   │   │               │   │   ├── pojo
│   │   │               │   │   │   ├── dto
│   │   │               │   │   │   │   ├── CreateSchoolClassRequest.java // 创建班级请求
│   │   │               │   │   │   │   └── CreateCourseRequest.java // 创建课程请求
│   │   │               │   │   │   ├── vo
│   │   │               │   │   │   │   ├── SchoolClassDetailVO.java // 班级详情响应
│   │   │               │   │   │   │   └── CourseDetailVO.java // 课程详情响应
│   │   │               │   │   │   └── convertor
│   │   │               │   │   │       └── TeachingAdapterConvertor.java // 教学请求/响应单向转换器
│   │   │               │   │   └── validators
│   │   │               │   │       └── TeachingRequestValidator.java // 教学 HTTP/RPC 入参校验器
│   │   │               │   ├── pojo                      // 跨领域入站载体包说明
│   │   │               │   │   ├── dto
│   │   │               │   │   │   └── RpcIdQuery.java   // 原生 RPC 通用 ID 入参
│   │   │               │   │   └── convertor
│   │   │               │   │       └── LightFacadeConverter.java // Facade DTO 与各层载体的共享单向转换
│   │   │               │   ├── handler                   // 入站异常与响应处理包说明
│   │   │               │   │   ├── GlobalExceptionHandler.java // 全局异常处理器，按 getStatus 输出稳定错误码
│   │   │               │   │   ├── GraphQlExceptionResolver.java // GraphQL 异常解析器
│   │   │               │   │   ├── RabbitConsumerErrorHandler.java // MQ 消费异常处理器
│   │   │               │   │   ├── ApiResponse.java      // 统一响应载体
│   │   │               │   │   └── ResponseWrapperHandler.java // 响应包装处理器
│   │   │               │   └── filter                    // Web Filter 包说明
│   │   │               │       ├── TraceIdFilter.java    // TraceId 过滤器
│   │   │               │       ├── RequestContextFilter.java // 请求上下文过滤器
│   │   │               │       ├── RequestContext.java   // 请求上下文载体
│   │   │               │       └── RequestContextHolder.java // 请求上下文持有器
│   │   │               │
│   │   │               ├── facade                        // 对外契约层包说明，除 common.exception 外不依赖其他层
│   │   │               │   ├── package-info.java         // 对外契约层包说明
│   │   │               │   ├── user                      // 用户权限契约包说明
│   │   │               │   │   ├── UserFacade.java       // 用户 Facade 契约
│   │   │               │   │   ├── PermissionFacade.java // 权限 Facade 契约
│   │   │               │   │   ├── dto                   // 用户 Facade DTO 包说明
│   │   │               │   │   │   ├── CreateUserDTO.java // 创建用户 DTO
│   │   │               │   │   │   ├── AssignRoleDTO.java // 分配角色 DTO
│   │   │               │   │   │   └── UserDetailDTO.java // 用户详情 DTO
│   │   │               │   │   ├── enums                 // 用户契约枚举包说明
│   │   │               │   │   │   └── UserFacadeStatus.java // 用户契约状态枚举
│   │   │               │   │   └── utils                 // 用户契约工具包说明
│   │   │               │   │       └── UserFacadeAssert.java // 用户契约断言工具
│   │   │               │   ├── teaching                  // 教学契约包说明
│   │   │               │   │   ├── SchoolClassFacade.java // 班级 Facade 契约
│   │   │               │   │   ├── CourseFacade.java     // 课程 Facade 契约
│   │   │               │   │   ├── dto
│   │   │               │   │   │   ├── CreateSchoolClassDTO.java // 创建班级 DTO
│   │   │               │   │   │   ├── CreateCourseDTO.java // 创建课程 DTO
│   │   │               │   │   │   └── CourseDTO.java    // 课程 DTO
│   │   │               │   │   ├── enums
│   │   │               │   │   │   └── CourseFacadeStatus.java // 课程契约状态枚举
│   │   │               │   │   └── utils
│   │   │               │   │       └── TeachingFacadeAssert.java // 教学契约断言工具
│   │   │               │   └── validation                // 跨传输契约校验组包说明
│   │   │               │       └── NativeRpcValidationGroup.java // 原生 RPC 分组校验标记，默认 HTTP 校验不受影响
│   │   │               │
│   │   │               ├── application                   // 应用编排层包说明
│   │   │               │   ├── package-info.java         // 应用编排层包说明
│   │   │               │   ├── user                      // 用户权限用例包说明
│   │   │               │   │   ├── manage                // 用户应用用例接口包说明
│   │   │               │   │   │   ├── UserManage.java   // 用户应用服务接口
│   │   │               │   │   │   ├── RoleManage.java   // 角色应用服务接口
│   │   │               │   │   │   ├── PermissionManage.java // 权限应用服务接口
│   │   │               │   │   │   └── impl              // 用户应用用例实现包说明
│   │   │               │   │   │       ├── UserManageImpl.java // 用户应用服务实现
│   │   │               │   │   │       ├── RoleManageImpl.java // 角色应用服务实现
│   │   │               │   │   │       └── PermissionManageImpl.java // 权限应用服务实现
│   │   │               │   │   ├── pojo                  // 用户用例载体包说明
│   │   │               │   │   │   ├── command
│   │   │               │   │   │   │   ├── CreateUserCommand.java // 创建用户命令
│   │   │               │   │   │   │   └── AssignRoleCommand.java // 分配角色命令
│   │   │               │   │   │   ├── query
│   │   │               │   │   │   │   └── GetUserQuery.java // 用户查询对象
│   │   │               │   │   │   ├── result
│   │   │               │   │   │   │   └── UserResult.java // 用户用例结果
│   │   │               │   │   │   └── convertor         // 用户应用转换器包说明
│   │   │               │   │   │       └── UserApplicationConvertor.java // 用户 Command/Domain 单向转换，快照投影同样由它承担
│   │   │               │   │   └── validators
│   │   │               │   │       └── UserApplicationValidator.java // 用户用例前置校验器
│   │   │               │   └── teaching                  // 教学用例包说明
│   │   │               │       ├── manage
│   │   │               │       │   ├── SchoolClassManage.java // 班级应用服务接口
│   │   │               │       │   ├── CourseManage.java // 课程应用服务接口
│   │   │               │       │   └── impl
│   │   │               │       │       ├── SchoolClassManageImpl.java // 班级应用服务实现
│   │   │               │       │       └── CourseManageImpl.java // 课程应用服务实现
│   │   │               │       ├── pojo
│   │   │               │       │   ├── command
│   │   │               │       │   │   └── CreateCourseCommand.java // 创建课程命令
│   │   │               │       │   ├── query
│   │   │               │       │   │   └── GetCourseQuery.java // 课程查询对象
│   │   │               │       │   ├── result
│   │   │               │       │   │   └── CourseResult.java // 课程用例结果
│   │   │               │       │   └── convertor
│   │   │               │       │       └── TeachingApplicationConvertor.java // 教学 Command/Domain 单向转换
│   │   │               │       └── validators
│   │   │               │           └── TeachingApplicationValidator.java // 教学用例前置校验器
│   │   │               │
│   │   │               ├── domain                        // 领域核心层包说明
│   │   │               │   ├── package-info.java         // 领域核心层包说明
│   │   │               │   ├── user                      // 用户权限领域包说明
│   │   │               │   │   ├── entities
│   │   │               │   │   │   ├── User.java         // 用户实体
│   │   │               │   │   │   ├── Role.java         // 角色实体
│   │   │               │   │   │   └── Permission.java   // 权限实体
│   │   │               │   │   ├── aggregates
│   │   │               │   │   │   ├── UserAggregate.java // 用户聚合
│   │   │               │   │   │   └── RolePermissionAggregate.java // 角色权限聚合
│   │   │               │   │   ├── vos
│   │   │               │   │   │   ├── UserId.java       // 用户 ID 值对象
│   │   │               │   │   │   ├── UserSnapshot.java // 用户读快照值对象
│   │   │               │   │   │   └── UserEvent.java    // 用户事件值对象
│   │   │               │   │   ├── service               // 用户领域服务接口包说明，实现落在 infrastructure
│   │   │               │   │   │   ├── UserDomainService.java // 用户领域服务
│   │   │               │   │   │   ├── UserQueryService.java // 用户查询领域服务
│   │   │               │   │   │   ├── UserEventService.java // 用户事件发布领域服务
│   │   │               │   │   │   └── UserIdempotencyService.java // 用户幂等领域服务
│   │   │               │   │   ├── validators
│   │   │               │   │   │   └── UserDomainValidator.java // 用户领域校验器
│   │   │               │   │   └── enums
│   │   │               │   │       └── UserStatus.java   // 用户状态枚举
│   │   │               │   └── teaching                  // 教学领域包说明
│   │   │               │       ├── entities
│   │   │               │       │   ├── SchoolClass.java  // 班级实体，避免使用 Java 关键字 Class
│   │   │               │       │   └── Course.java       // 课程实体
│   │   │               │       ├── aggregates
│   │   │               │       │   └── SchoolClassAggregate.java // 班级聚合
│   │   │               │       ├── vos
│   │   │               │       │   └── Semester.java     // 学期值对象
│   │   │               │       ├── service
│   │   │               │       │   ├── SchoolClassDomainService.java // 班级领域服务
│   │   │               │       │   ├── TeachingQueryService.java // 教学查询领域服务
│   │   │               │       │   ├── TeachingEventService.java // 教学事件发布领域服务
│   │   │               │       │   └── CourseIdempotencyService.java // 课程幂等领域服务
│   │   │               │       ├── validators
│   │   │               │       │   └── TeachingDomainValidator.java // 教学领域校验器
│   │   │               │       └── enums
│   │   │               │           └── CourseStatus.java // 课程状态枚举
│   │   │               │
│   │   │               ├── infrastructure                // 基础设施层包说明
│   │   │               │   ├── package-info.java         // 基础设施层包说明
│   │   │               │   ├── user                      // 用户权限基础设施包说明，领域在前、技术目录平级
│   │   │               │   │   ├── dao
│   │   │               │   │   │   ├── UserDAO.java      // Common MP DAO
│   │   │               │   │   │   ├── RoleDAO.java
│   │   │               │   │   │   └── PermissionDAO.java
│   │   │               │   │   ├── po
│   │   │               │   │   │   ├── UserPO.java       // EgonModel + MP table mapping
│   │   │               │   │   │   └── RolePO.java
│   │   │               │   │   ├── converter
│   │   │               │   │   │   └── UserPOConverter.java // PO 与领域对象双向转换
│   │   │               │   │   ├── repo
│   │   │               │   │   │   └── UserRepository.java // extends EgonColaRepository，承接租户/逻辑删除/乐观锁/缓存合同
│   │   │               │   │   ├── service
│   │   │               │   │   │   └── impl
│   │   │               │   │   │       ├── UserDomainServiceImpl.java // 用户领域服务实现
│   │   │               │   │   │       ├── UserQueryServiceImpl.java // 用户查询领域服务实现
│   │   │               │   │   │       └── UserEventServiceImpl.java // 用户事件领域服务实现
│   │   │               │   │   ├── client
│   │   │               │   │   │   ├── UserQueryClient.java // 外部用户查询能力接口
│   │   │               │   │   │   └── impl
│   │   │               │   │   │       ├── RestUserQueryClientImpl.java // 外部 HTTP 实现
│   │   │               │   │   │       └── LocalUserQueryClientImpl.java // 单体内置回退实现
│   │   │               │   │   └── validators
│   │   │               │   │       └── UserInfrastructureValidator.java // 外部数据校验器
│   │   │               │   ├── teaching                  // 教学基础设施包说明
│   │   │               │   │   ├── dao
│   │   │               │   │   │   ├── SchoolClassDAO.java
│   │   │               │   │   │   └── CourseDAO.java
│   │   │               │   │   ├── po
│   │   │               │   │   │   └── SchoolClassPO.java
│   │   │               │   │   ├── converter
│   │   │               │   │   │   └── SchoolClassPOConverter.java
│   │   │               │   │   ├── repo
│   │   │               │   │   │   └── SchoolClassRepository.java
│   │   │               │   │   ├── service
│   │   │               │   │   │   └── impl
│   │   │               │   │   │       └── SchoolClassDomainServiceImpl.java
│   │   │               │   │   ├── client
│   │   │               │   │   │   ├── TeachingQueryClient.java
│   │   │               │   │   │   └── impl
│   │   │               │   │   │       └── RestTeachingQueryClientImpl.java
│   │   │               │   │   └── validators
│   │   │               │   │       └── TeachingInfrastructureValidator.java
│   │   │               │   ├── aop                       // 基础设施 AOP 包说明
│   │   │               │   │   ├── DaoMonitorAspect.java // DAO 耗时与异常监控切面
│   │   │               │   │   └── InfrastructureLogAspect.java // 基础设施日志切面
│   │   │               │   ├── mq                        // MQ 出站包说明，仅出站
│   │   │               │   │   ├── MqMessageService.java // 出站消息发送接口
│   │   │               │   │   ├── MqRouteEnum.java      // 路由与事件类型枚举
│   │   │               │   │   └── impl
│   │   │               │   │       └── RabbitMqMessageServiceImpl.java // RabbitMQ 出站实现
│   │   │               │   └── config                    // 基础设施配置包说明
│   │   │               │       ├── RedisConfig.java      // Redis / 二级缓存配置
│   │   │               │       ├── RabbitMqConfig.java   // MQ 出站配置
│   │   │               │       ├── ExternalClientConfig.java // 外部客户端配置
│   │   │               │       └── TransactionCompletionExecutor.java // 事务完成后执行器
│   │   │               │
│   │   │               └── common                        // 通用基础层包说明
│   │   │                   ├── package-info.java         // 通用基础层包说明
│   │   │                   ├── constants
│   │   │                   │   └── TraceConstants.java   // TraceId 等通用常量
│   │   │                   ├── enums
│   │   │                   │   └── DeletedStatus.java    // 逻辑删除状态枚举
│   │   │                   ├── exception                 // 各层共享的异常根类型包说明
│   │   │                   │   ├── BaseBusinessException.java // 项目业务异常基类
│   │   │                   │   ├── UserUseCaseException.java // 用户用例异常
│   │   │                   │   ├── TeachingUseCaseException.java // 教学用例异常
│   │   │                   │   ├── UserDomainException.java // 用户领域异常
│   │   │                   │   ├── UserFacadeException.java // 用户契约异常
│   │   │                   │   └── ConfigDecryptException.java // 配置解密异常
│   │   │                   └── utils
│   │   │                       └── package-info.java     // 通用工具包说明，按需新增
│   │   ├── proto
│   │   │   └── teaching_user_facade.proto                // 原生 RPC 契约定义，编译产物只在 RPC 入口使用
│   │   ├── resources
│   │   │   ├── application.yml                           // 默认配置
│   │   │   ├── application-dev.yml                       // 开发环境配置
│   │   │   ├── application-test.yml                      // 测试环境配置
│   │   │   ├── application-prod.yml                      // 生产环境配置
│   │   │   ├── logback-spring.xml                        // 日志配置
│   │   │   ├── egon-mybatis-plus-sharding.yml            // 分片数据源配置
│   │   │   ├── mybatis/mapper
│   │   │   │   ├── user
│   │   │   │   │   └── UserDAO.xml                       // 用户 DAO XML，含 selectActiveById 等合同语句
│   │   │   │   └── teaching
│   │   │   │       └── SchoolClassDAO.xml                // 班级 DAO XML
│   │   │   ├── graphql
│   │   │   │   ├── user.graphqls                         // 用户 GraphQL Schema
│   │   │   │   └── teaching.graphqls                     // 教学 GraphQL Schema
│   │   │   ├── db/migration/sharding
│   │   │   │   ├── master-data
│   │   │   │   │   ├── B20260825_001__baseline_light_master_data_schema.sql // 基线
│   │   │   │   │   └── V20260825_001__migrate_light_master_data_to_egon_model.sql // 主数据租户化迁移，已发布脚本只追加不改
│   │   │   │   └── shard
│   │   │   │       ├── B20260825_002__baseline_light_sharded_schema.sql      // 基线
│   │   │   │       └── V20260825_002__migrate_light_sharded_to_tenant_model.sql // 分片表租户化迁移
│   │   │   └── db/egon-mp
│   │   │       ├── repository-manifest.json              // 持久化清单，含脚本 SHA-256
│   │   │       └── V20260913_001__initialize_repository_schema.sql // 组件持久化建表脚本
│   │
│   └── test
│       ├── java/com/demo/student
│       │   ├── start/config
│       │   │   ├── NativeRpcConfigurationTest.java       // 原生 RPC 装配测试
│       │   │   └── NativeHttpCompatibilityTest.java      // HTTP 与 RPC 兼容性测试
│       │   ├── adapter
│       │   │   ├── NativeRpcProviderTest.java            // FacadeImpl 作为 RPC Provider 的行为测试
│       │   │   └── user/controller
│       │   │       └── UserControllerTest.java           // 用户接口测试
│       │   ├── facade
│       │   │   ├── NativeRpcContractTest.java            // 契约与载体一致性测试
│       │   │   └── NativeRpcMappingTest.java             // RPC 入站到操作的映射测试
│       │   ├── application/user/manage
│       │   │   └── UserManageTest.java                   // 用户应用服务测试
│       │   ├── domain/user/aggregates
│       │   │   └── UserAggregateTest.java                // 用户聚合测试
│       │   ├── infrastructure
│       │   │   ├── config
│       │   │   │   └── RabbitMqConfigTest.java           // MQ 配置测试
│       │   │   └── user/client
│       │   │       └── RestUserQueryClientImplTest.java  // 外部查询实现测试
│       │   └── architecture
│       │       ├── ArchetypeContractConvergenceTest.java // 分层与公共合同守门测试
│       │       └── LightPersistenceArchitectureTest.java // 持久化访问路径守门测试
│       └── resources
│           ├── application-test.yml                      // 单元测试 / 集成测试配置
│           └── db/schema-test.sql                        // 测试库表结构
│
└── deploy
    ├── container                                         // 镜像构建说明
    └── compose                                           // compose 编排（local / dev / prod）
```

---

## 5. 开发约束

### 5.1 start 约束

```text
1. start 只负责启动和装配。
2. start 不写业务逻辑。
3. start 不写 Controller。
4. start 不写 Repository。
5. start 不写 MQ Consumer / Producer。
6. start 不写 Domain Service。
```

### 5.2 adapter 约束

```text
1. adapter 只处理入站请求。
2. adapter 可以调用 application。
3. adapter 可以依赖 facade。
4. adapter 不允许直接调用 mapper。
5. adapter 不允许直接调用 ServiceImpl。
6. adapter 不允许直接操作 Redis。
7. adapter 不允许直接发送 MQ。
8. adapter 不允许写核心业务规则。
9. adapter.mq 只负责入站消息消费。
10. adapter.facade.impl 只负责实现 facade 接口并转发到 application。
11. facade.impl 只能放在 adapter，不能放在 application。
```

允许：

```text
Controller -> Application
MQ Consumer -> Application
GraphQL Resolver -> Application
FacadeImpl -> Application                     // 原生 RPC Provider 注解在 FacadeImpl 上
```

不允许：

```text
Controller -> Mapper
Controller -> RedisTemplate
Controller -> ServiceImpl/DAO
Controller -> Domain Repository
Controller -> Domain Service
```

### 5.3 facade 约束

```text
1. facade 只定义接口契约。
2. facade 放 DTO、接口、对外枚举、契约工具和契约校验分组。
3. facade 不写实现类。
4. facade 只允许依赖 common 的 exception 目录，其余内容一律不依赖。
5. facade 不依赖 application。
6. facade 不依赖 domain。
7. facade 不依赖 infrastructure。
8. facade 不依赖 adapter。
```

### 5.4 application 约束

```text
1. application 负责业务用例编排。
2. application 可以调用 domain service。
3. application 只能通过 domain service 取得查询、事件发布与幂等能力。
4. application 不能调用 infrastructure DAO/PO。
5. application 负责事务控制。
6. application 不依赖 infrastructure。
7. application 不直接调用 mapper。
8. application 不直接调用 DAO。
9. application 不直接调用 RedisTemplate。
10. application 不直接调用 MQ Template。
11. application 不直接调用外部 RPC / HTTP 实现。
12. application 不实现 Facade 接口。
13. application 不包含 facade.impl 包。
14. manage 包必须按 user.manage.impl、teaching.manage.impl 这种"领域在前"的方式组织。
```

允许：

```text
Application -> Domain Service
Application -> Domain Query/Event/Idempotency Service
Application -> Application Validator
Application -> pojo.convertor（BaseForwardConverter 单向转换与快照投影）
```

不允许：

```text
Application -> Mapper
Application -> EgonColaMapper/DAO
Application -> RedisTemplate
Application -> KafkaTemplate
Application -> RabbitTemplate
Application -> ExternalFacade
Application -> FeignClient
Application -> FacadeImpl
```

### 5.5 infrastructure 约束

```text
1. infrastructure 负责技术实现。
2. infrastructure 实现 domain service 接口，并承载外部 HTTP / RPC 客户端实现。
3. infrastructure ServiceImpl 只依赖 Repository，Repository 继承 EgonColaRepository 后才接触 DAO、PO、Converter。
4. infrastructure 可以调用 EgonColaMapper DAO。
5. infrastructure 可以调用 RedisTemplate。
6. infrastructure 可以调用 MQ Template。
7. infrastructure 可以调用外部 HTTP / RPC / SDK。
8. infrastructure 不写核心业务规则。
9. infrastructure.mq 只负责出站消息发送。
10. infrastructure 必须按 user.*、teaching.* 分领域组织，领域目录内 dao / po / converter / repo / service / client / validators 平级。
11. infrastructure 不设 cache 目录，缓存合同由 Repository 与 Common 组件承接。
```

允许：

```text
ServiceImpl -> Repository (EgonColaRepository) -> DAO (EgonColaMapper) -> Database
ClientImpl -> ExternalFacade
ClientImpl -> HTTP Client
MQ Producer -> KafkaTemplate / RabbitTemplate
Repository -> 二级缓存
```

不允许：

```text
Infrastructure -> Controller
Infrastructure -> FacadeImpl
Infrastructure -> Adapter MQ Consumer
Infrastructure 中堆核心业务流程
```

### 5.6 common 约束

```text
1. common 只放通用基础能力。
2. common 不放具体业务逻辑。
3. common 不放业务状态枚举。
4. common.exception 只提供各层共享的异常根类型，不放业务判断。
5. common 不放业务 Redis Key。
6. common 不放业务表名常量。
7. 除 exception 目录外，common 不被 facade 依赖。
```

### 5.7 domain 约束

```text
1. domain 只表达业务规则。
2. domain 定义实体、聚合、值对象、领域校验器，以及查询 / 事件 / 幂等等领域服务接口。
3. domain 不依赖 application。
4. domain 不依赖 infrastructure。
5. domain 不依赖 adapter。
6. domain 不依赖 facade。
7. domain 不依赖 MyBatis-Plus 实现、DAO 或 Mapper。
8. domain 不依赖 JPA/ORM。
9. domain 不依赖 Redis。
10. domain 不依赖 MQ。
11. domain 不依赖 HTTP / RPC 技术实现。
12. domain service 接口位于 service；实现位于 infrastructure/service.impl。
```

---

## 6. Validator 规范

Validator 按层分为四类：

```text
adapter validator
application validator
domain validator
infrastructure validator
```

不同层的 Validator 负责不同类型的校验，不能混用。

---

### 6.1 Adapter Validator

#### 职责

负责入站请求格式校验。

#### 适合校验

```text
1. 参数是否为空。
2. 字段长度是否合法。
3. 日期格式是否合法。
4. 枚举值是否合法。
5. 分页参数是否合法。
6. 请求体结构是否合法。
```

#### 示例

```text
CreateUserRequest.username 不能为空
CreateUserRequest.mobile 格式必须正确
CreateCourseRequest.courseCode 不能为空
PageRequest.pageNo 必须大于 0
```

#### 不适合校验

```text
1. 用户是否存在。
2. 角色是否有权限。
3. 课程是否可以排课。
4. 班级是否允许删除。
5. 数据库唯一性是否冲突。
```

---

### 6.2 Application Validator

#### 职责

负责应用用例级校验。

#### 适合校验

```text
1. 当前用户是否有权限执行该操作。
2. 当前流程是否允许继续。
3. 当前操作是否满足前置条件。
4. 当前请求是否重复提交。
5. 多领域协作时的前置校验。
6. 操作上下文是否合法。
```

#### 示例

```text
创建用户前校验当前操作人是否有用户管理权限
分配角色前校验角色是否可用
创建课程前校验当前学院是否允许开课
安排课程前校验班级和课程是否都有效
```

#### 不适合校验

```text
1. 实体内部状态流转规则。
2. 聚合内部一致性规则。
3. Redis 数据结构是否合法。
4. 外部接口返回字段是否合法。
```

---

### 6.3 Domain Validator

#### 职责

负责领域不变量校验。

领域不变量是无论入口来自 HTTP、MQ、RPC 还是定时任务，都必须始终成立的业务规则。

#### 适合校验

```text
1. 用户状态是否合法。
2. 角色权限关系是否合法。
3. 权限编码是否符合领域规则。
4. 班级状态是否允许变更。
5. 课程状态是否允许排课。
6. 课程时间是否冲突。
7. 聚合内部数据是否一致。
```

#### 示例

```text
已禁用用户不能登录
已归档角色不能继续分配给用户
已停用权限不能绑定到角色
已结课课程不能继续排课
同一个班级同一时间不能安排两门课程
```

#### 不适合校验

```text
1. HTTP 参数格式。
2. 当前登录人身份。
3. Redis Key 是否存在。
4. 外部系统返回值是否为空。
5. 数据库连接是否正常。
```

---

### 6.4 Infrastructure Validator

#### 职责

负责基础设施适配过程中的技术校验和数据兼容校验。

#### 适合校验

```text
1. 外部接口返回值是否合法。
2. 第三方系统字段是否缺失。
3. 数据库唯一键冲突转换。
4. 缓存数据结构是否合法。
5. MQ 消息发送结果是否合法。
6. 外部 Facade 返回错误码转换。
```

#### 示例

```text
外部用户中心返回 userId 为空
外部课程系统返回 courseCode 缺失
Redis 中缓存的权限树结构无法解析
数据库唯一索引冲突转换为业务异常
MQ 发送失败转换为基础设施异常
```

#### 不适合校验

```text
1. 核心业务状态流转。
2. 用户是否可以分配角色。
3. 班级是否可以安排课程。
4. 课程是否可以结课。
```

---

### 6.5 Validator 放置原则

```text
1. 请求格式校验放 adapter。
2. 用例前置校验放 application。
3. 核心业务规则校验放 domain。
4. 技术适配校验放 infrastructure。
```

判断规则：

```text
如果换成 MQ 入口后仍然需要校验，通常不应该只放 adapter。

如果这条规则属于业务永恒规则，应该放 domain。

如果这条规则只和当前操作流程有关，应该放 application。

如果这条规则只和外部系统、数据库、缓存、MQ 有关，应该放 infrastructure。
```

---

## 7. 总结

本架构是一种适合大型单体项目的轻量领域分层架构。

它的核心不是把项目拆成很多 Maven 子模块，而是在一个单体工程内部通过包结构建立清晰边界：

```text
start           负责启动装配
adapter         负责入站适配
facade          负责对外契约
application     负责业务编排
domain          负责核心规则
infrastructure  负责技术实现
common          负责通用基础能力
```

最终包级依赖方向为：

```text
start -> adapter / infrastructure

adapter -> application / facade

application -> domain

domain -> common

infrastructure -> application
```

在多领域大型单体中，推荐按领域拆分 domain 子包：

```text
domain.user
    - user
    - role
    - permission

domain.teaching
    - school class
    - course
```

本架构的最终目标是：

```text
1. 单体部署，不拆子模块。
2. 包边界清晰。
3. 没有循环依赖。
4. 入口统一收敛到 application。
5. 核心业务规则沉淀在 domain。
6. 技术实现隔离在 infrastructure。
7. Facade 实现只能放 adapter/facade/impl。
8. resources、test、test resources 结构完整。
9. 后续具备平滑拆模块或拆服务的可能。
```

一句话总结：

```text
它不是把单体拆散，而是给单体立规矩，别让它长成一锅 Java 粥。
```



## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。
