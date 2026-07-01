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
start import adapter infrastructure

adapter import application facade

application import domain

domain import common

infrastructure import application
```

注意：

```text
1. facade 不依赖 common。
2. facade 有自己的 dto / enums / exceptions / utils。
3. application 不依赖 infrastructure。
4. domain 不依赖 application。
5. domain 不依赖 infrastructure。
6. infrastructure 依赖 application。
7. start 只负责装配 adapter 和 infrastructure。
```

### 2.2 依赖关系图

```text
                    start
                      |
          -------------------------
          |                       |
       adapter              infrastructure
          |                       |
    -------------                 |
    |           |                 |
application   facade         application
    |
  domain
    |
  common
```

### 2.3 主调用方向

系统主调用方向为：

```text
adapter -> application -> domain -> common
```

对外契约方向为：

```text
adapter -> facade
```

基础设施方向为：

```text
infrastructure -> application
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

domain.user import common
domain.teaching import common

adapter import application
adapter import facade

infrastructure import application

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
        -> domain.teaching.repos.CourseRepository
```

### 2.5 依赖控制方式

因为当前是单体工程，不拆 Maven 子模块，所以不能只依赖 Maven 来限制模块边界。推荐通过以下方式控制依赖：

```text
1. 代码评审约束包依赖方向。
2. 使用 ArchUnit 做包依赖检查。
3. 禁止 Controller 直接调用 Mapper / RepositoryImpl。
4. 禁止 Application 直接调用 Infrastructure 技术实现。
5. 禁止 Domain 依赖 Spring MVC / MyBatis-Plus / JPA / Redis / MQ。
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
    - rpc
    - convertor
    - dto
    - vo
    - graphql
    - facade.impl
    - handler
    - filter
```

#### 3.2.3 能做什么

```text
1. 接收 HTTP 请求。
2. 消费入站 MQ 消息。
3. 暴露 RPC Provider。
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
2. 不直接调用 JPA Repository。
3. 不直接调用 MP Service。
4. 不直接操作 RedisTemplate。
5. 不直接发送 MQ。
6. 不直接写核心业务规则。
7. 不直接操作 domain repository。
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
RPC Provider -> Application
GraphQL Resolver -> Application
FacadeImpl -> Application
```

---

### 3.3 facade

#### 3.3.1 职责

`facade` 是对外契约层，只定义对外接口、DTO、枚举、异常、工具。

它适合被 RPC、内部 SDK、其他系统调用方依赖。

#### 3.3.2 推荐结构

```text
facade
    - facade 定义
    - dto
    - enums
    - exceptions
    - utils
```

#### 3.3.3 能做什么

```text
1. 定义 Facade 接口。
2. 定义对外请求 DTO。
3. 定义对外响应 DTO。
4. 定义对外枚举。
5. 定义对外异常。
6. 定义 Facade 内部轻量工具。
```

#### 3.3.4 不能做什么

```text
1. 不写 Facade 实现类。
2. 不依赖 common。
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
    - convertor
    - manage
    - validators
    - assemblers
    - client
```

其中 `manage` 必须按领域分包，接口和实现按以下方式组织：

```text
application/manage/user/UserManage.java
application/manage/user/impl/UserManageImpl.java

application/manage/teaching/CourseManage.java
application/manage/teaching/impl/CourseManageImpl.java
```

不允许写成：

```text
application/manage/impl/user/UserManageImpl.java
```

#### 3.4.3 能做什么

```text
1. 编排业务流程。
2. 控制事务边界。
3. 调用 domain service。
4. 调用 domain repos 接口。
5. 调用 application client 接口。
6. 做应用级参数校验。
7. 做权限、幂等、流程前置校验。
8. 做 DTO、Command、Domain Model 的转换。
9. 聚合多个领域完成一个业务用例。
```

#### 3.4.4 不能做什么

```text
1. 不直接调用 mapper。
2. 不直接调用 JPA Repository。
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
MyBatis-Plus
JPA
AOP
基础设施配置
```

#### 3.5.2 推荐结构

```text
infrastructure
    - repo
        - user
            - impl
            - po
            - mp
            - jpa
            - converter
        - teaching
            - impl
            - po
            - mp
            - jpa
            - converter
    - validators
    - client
        - user
            - impl
        - teaching
            - impl
    - aop
    - mq
    - cache
    - config
```

注意：`repo` 必须按照领域分包，例如：

```text
repo.user.*
repo.teaching.*
```

不建议写成：

```text
repo.impl.user.*
repo.po.user.*
repo.mp.user.*
```

#### 3.5.3 能做什么

```text
1. 实现 domain repos 接口。
2. 调用 MyBatis-Plus Service。
3. 调用 MyBatis Mapper。
4. 调用 JPA Repository。
5. 实现 application client 接口。
6. 调用外部 Facade。
7. 调用外部 HTTP / RPC / gRPC。
8. 发送出站 MQ 消息。
9. 封装 Redis、Caffeine 等缓存。
10. 定义基础设施相关配置。
11. 做数据库对象 PO 和领域对象之间的转换。
```

#### 3.5.4 不能做什么

```text
1. 不写核心业务规则。
2. 不处理入站 HTTP 请求。
3. 不消费入站 MQ 消息。
4. 不暴露 Controller。
5. 不暴露 Facade 实现。
6. 不让 application 直接感知 mapper / jpa / redis / mq。
7. 不让 domain 感知任何基础设施实现。
```

#### 3.5.5 repo 规范

推荐调用链路：

```text
Application
    -> Domain Repos Interface
        -> Infrastructure Repo Impl
            -> MP Service / JPA Repository
                -> Database
```

如果使用 MyBatis-Plus：

```text
repo.user.impl.UserRepositoryImpl
    -> repo.user.mp.service.UserMpService
        -> repo.user.mp.mapper.UserMapper
```

业务代码不允许直接调用 Mapper。

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
    - exceptions
```

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
2. 不放具体业务异常。
3. 不放业务规则工具类。
4. 不放数据库表名常量。
5. 不放 Redis 业务 Key。
6. 不放领域模型。
7. 不被 facade 依赖。
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
        - service.impl
        - repos
        - validators
        - enums
    - teaching
        - entities
        - aggregates
        - vos
        - service
        - service.impl
        - repos
        - validators
        - enums
```

注意：领域服务包必须是：

```text
service
service.impl
```

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
4. 定义领域服务。
5. 定义领域仓储接口。
6. 定义领域校验器。
7. 定义领域枚举。
8. 表达核心业务规则。
9. 维护领域对象内部状态一致性。
```

#### 3.7.4 不能做什么

```text
1. 不依赖 Spring MVC。
2. 不依赖 MyBatis-Plus。
3. 不依赖 JPA。
4. 不依赖 Redis。
5. 不依赖 MQ。
6. 不依赖 HTTP Client。
7. 不依赖 Dubbo / gRPC。
8. 不依赖 infrastructure。
9. 不依赖 adapter。
10. 不依赖 application。
11. 不依赖 facade。
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
├── pom.xml                                                   // 单体工程 Maven 配置，不拆子模块
├── README.md                                                 // 项目说明
├── .gitignore                                                // Git 忽略配置
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── demo
│   │   │           └── student
│   │   │               ├── package-info.java                // 学生管理系统根包说明
│   │   │               │
│   │   │               ├── start
│   │   │               │   ├── package-info.java            // 启动装配层包说明
│   │   │               │   ├── StudentManagementApplication.java // Spring Boot 启动类
│   │   │               │   └── config
│   │   │               │       ├── package-info.java        // 启动层业务无关配置包说明
│   │   │               │       ├── JacksonConfig.java       // JSON 序列化配置
│   │   │               │       ├── OpenApiConfig.java       // OpenAPI / Swagger 配置
│   │   │               │       └── ActuatorConfig.java      // 监控端点配置
│   │   │               │
│   │   │               ├── adapter
│   │   │               │   ├── package-info.java            // 入站适配层包说明
│   │   │               │   ├── controller
│   │   │               │   │   ├── package-info.java        // HTTP Controller 包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户权限 HTTP 入口包说明
│   │   │               │   │   │   ├── UserController.java  // 用户接口
│   │   │               │   │   │   ├── RoleController.java  // 角色接口
│   │   │               │   │   │   └── PermissionController.java // 权限接口
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学管理 HTTP 入口包说明
│   │   │               │   │       ├── SchoolClassController.java // 班级接口，避免使用 ClassController
│   │   │               │   │       └── CourseController.java // 课程接口
│   │   │               │   ├── mq
│   │   │               │   │   ├── package-info.java        // MQ 入站消费包说明，仅入站
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户权限消息消费包说明
│   │   │               │   │   │   └── UserImportedConsumer.java // 用户导入完成消息消费者
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学消息消费包说明
│   │   │               │   │       └── CourseImportedConsumer.java // 课程导入完成消息消费者
│   │   │               │   ├── rpc
│   │   │               │   │   ├── package-info.java        // RPC 入站适配包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户 RPC Provider 包说明
│   │   │               │   │   │   └── UserRpcProvider.java // 用户 RPC 入站服务
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学 RPC Provider 包说明
│   │   │               │   │       └── CourseRpcProvider.java // 课程 RPC 入站服务
│   │   │               │   ├── graphql
│   │   │               │   │   ├── package-info.java        // GraphQL Resolver 包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户 GraphQL 包说明
│   │   │               │   │   │   └── UserResolver.java    // 用户 GraphQL 查询入口
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学 GraphQL 包说明
│   │   │               │   │       └── CourseResolver.java  // 课程 GraphQL 查询入口
│   │   │               │   ├── facade
│   │   │               │   │   ├── package-info.java        // Facade 入站适配包说明
│   │   │               │   │   └── impl
│   │   │               │   │       ├── package-info.java    // Facade 实现包说明，只能放 adapter/facade/impl
│   │   │               │   │       ├── user
│   │   │               │   │       │   ├── package-info.java // 用户 Facade 实现包说明
│   │   │               │   │       │   ├── UserFacadeImpl.java // 用户 Facade 实现
│   │   │               │   │       │   └── PermissionFacadeImpl.java // 权限 Facade 实现
│   │   │               │   │       └── teaching
│   │   │               │   │           ├── package-info.java // 教学 Facade 实现包说明
│   │   │               │   │           ├── SchoolClassFacadeImpl.java // 班级 Facade 实现
│   │   │               │   │           └── CourseFacadeImpl.java // 课程 Facade 实现
│   │   │               │   ├── dto
│   │   │               │   │   ├── package-info.java        // Adapter 入参 DTO 包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户请求 DTO 包说明
│   │   │               │   │   │   ├── CreateUserRequest.java // 创建用户请求
│   │   │               │   │   │   ├── AssignRoleRequest.java // 分配角色请求
│   │   │               │   │   │   └── GrantPermissionRequest.java // 授权权限请求
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学请求 DTO 包说明
│   │   │               │   │       ├── CreateSchoolClassRequest.java // 创建班级请求
│   │   │               │   │       └── CreateCourseRequest.java // 创建课程请求
│   │   │               │   ├── vo
│   │   │               │   │   ├── package-info.java        // Adapter 出参 VO 包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户响应 VO 包说明
│   │   │               │   │   │   ├── UserDetailVO.java    // 用户详情响应
│   │   │               │   │   │   └── PermissionTreeVO.java // 权限树响应
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学响应 VO 包说明
│   │   │               │   │       ├── SchoolClassDetailVO.java // 班级详情响应
│   │   │               │   │       └── CourseDetailVO.java  // 课程详情响应
│   │   │               │   ├── convertor
│   │   │               │   │   ├── package-info.java        // Adapter 转换器包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户 Adapter 转换器包说明
│   │   │               │   │   │   └── UserAdapterConvertor.java // 用户请求/响应转换器
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学 Adapter 转换器包说明
│   │   │               │   │       └── TeachingAdapterConvertor.java // 教学请求/响应转换器
│   │   │               │   ├── handler
│   │   │               │   │   ├── package-info.java        // 入站异常与响应处理包说明
│   │   │               │   │   ├── GlobalExceptionHandler.java // 全局异常处理器
│   │   │               │   │   └── ResponseWrapperHandler.java // 响应包装处理器
│   │   │               │   └── filter
│   │   │               │       ├── package-info.java        // Web Filter 包说明
│   │   │               │       ├── TraceIdFilter.java       // TraceId 过滤器
│   │   │               │       └── RequestContextFilter.java // 请求上下文过滤器
│   │   │               │
│   │   │               ├── facade
│   │   │               │   ├── package-info.java            // 对外契约层包说明，不依赖 common
│   │   │               │   ├── user
│   │   │               │   │   ├── package-info.java        // 用户权限 Facade 定义包说明
│   │   │               │   │   ├── UserFacade.java          // 用户 Facade 契约
│   │   │               │   │   └── PermissionFacade.java    // 权限 Facade 契约
│   │   │               │   ├── teaching
│   │   │               │   │   ├── package-info.java        // 教学 Facade 定义包说明
│   │   │               │   │   ├── SchoolClassFacade.java   // 班级 Facade 契约
│   │   │               │   │   └── CourseFacade.java        // 课程 Facade 契约
│   │   │               │   ├── dto
│   │   │               │   │   ├── package-info.java        // Facade DTO 包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户 Facade DTO 包说明
│   │   │               │   │   │   ├── CreateUserDTO.java   // 创建用户 DTO
│   │   │               │   │   │   ├── UserDetailDTO.java   // 用户详情 DTO
│   │   │               │   │   │   └── PermissionDTO.java   // 权限 DTO
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学 Facade DTO 包说明
│   │   │               │   │       ├── CreateSchoolClassDTO.java // 创建班级 DTO
│   │   │               │   │       ├── SchoolClassDetailDTO.java // 班级详情 DTO
│   │   │               │   │       └── CourseDTO.java       // 课程 DTO
│   │   │               │   ├── enums
│   │   │               │   │   ├── package-info.java        // Facade 对外枚举包说明
│   │   │               │   │   ├── UserFacadeStatus.java    // 用户契约状态枚举
│   │   │               │   │   └── CourseFacadeStatus.java  // 课程契约状态枚举
│   │   │               │   ├── exceptions
│   │   │               │   │   ├── package-info.java        // Facade 异常包说明
│   │   │               │   │   └── FacadeException.java     // Facade 契约异常
│   │   │               │   └── utils
│   │   │               │       ├── package-info.java        // Facade 工具包说明
│   │   │               │       └── FacadeAssertUtils.java   // Facade 断言工具
│   │   │               │
│   │   │               ├── application
│   │   │               │   ├── package-info.java            // 应用编排层包说明
│   │   │               │   ├── manage
│   │   │               │   │   ├── package-info.java        // 应用用例接口包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户权限应用用例包说明
│   │   │               │   │   │   ├── UserManage.java      // 用户应用服务接口
│   │   │               │   │   │   ├── RoleManage.java      // 角色应用服务接口
│   │   │               │   │   │   ├── PermissionManage.java // 权限应用服务接口
│   │   │               │   │   │   └── impl
│   │   │               │   │   │       ├── package-info.java // 用户权限应用用例实现包说明
│   │   │               │   │   │       ├── UserManageImpl.java // 用户应用服务实现
│   │   │               │   │   │       ├── RoleManageImpl.java // 角色应用服务实现
│   │   │               │   │   │       └── PermissionManageImpl.java // 权限应用服务实现
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学应用用例包说明
│   │   │               │   │       ├── SchoolClassManage.java // 班级应用服务接口
│   │   │               │   │       ├── CourseManage.java    // 课程应用服务接口
│   │   │               │   │       └── impl
│   │   │               │   │           ├── package-info.java // 教学应用用例实现包说明
│   │   │               │   │           ├── SchoolClassManageImpl.java // 班级应用服务实现
│   │   │               │   │           └── CourseManageImpl.java // 课程应用服务实现
│   │   │               │   ├── convertor
│   │   │               │   │   ├── package-info.java        // Application 转换器包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户应用转换器包说明
│   │   │               │   │   │   └── UserApplicationConvertor.java // 用户应用层转换器
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学应用转换器包说明
│   │   │               │   │       └── TeachingApplicationConvertor.java // 教学应用层转换器
│   │   │               │   ├── validators
│   │   │               │   │   ├── package-info.java        // Application Validator 包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户应用校验器包说明
│   │   │               │   │   │   └── UserApplicationValidator.java // 用户用例前置校验器
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学应用校验器包说明
│   │   │               │   │       └── TeachingApplicationValidator.java // 教学用例前置校验器
│   │   │               │   ├── assemblers
│   │   │               │   │   ├── package-info.java        // Application 组装器包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户应用组装器包说明
│   │   │               │   │   │   └── UserAssembler.java   // 用户领域对象组装器
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学应用组装器包说明
│   │   │               │   │       └── TeachingAssembler.java // 教学领域对象组装器
│   │   │               │   └── client
│   │   │               │       ├── package-info.java        // Application 出站 Client 接口包说明
│   │   │               │       ├── user
│   │   │               │       │   ├── package-info.java    // 用户外部能力接口包说明
│   │   │               │       │   └── UserQueryClient.java // 用户查询出站接口
│   │   │               │       └── teaching
│   │   │               │           ├── package-info.java    // 教学外部能力接口包说明
│   │   │               │           └── TeachingQueryClient.java // 教学查询出站接口
│   │   │               │
│   │   │               ├── infrastructure
│   │   │               │   ├── package-info.java            // 基础设施层包说明
│   │   │               │   ├── repo
│   │   │               │   │   ├── package-info.java        // Repository 基础包说明，按领域分包
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户权限 Repository 领域包说明
│   │   │               │   │   │   ├── impl
│   │   │               │   │   │   │   ├── package-info.java // 用户权限仓储实现包说明
│   │   │               │   │   │   │   ├── UserRepositoryImpl.java // 用户仓储实现
│   │   │               │   │   │   │   ├── RoleRepositoryImpl.java // 角色仓储实现
│   │   │               │   │   │   │   └── PermissionRepositoryImpl.java // 权限仓储实现
│   │   │               │   │   │   ├── po
│   │   │               │   │   │   │   ├── package-info.java // 用户权限 PO 包说明
│   │   │               │   │   │   │   ├── UserPO.java      // 用户持久化对象
│   │   │               │   │   │   │   ├── RolePO.java      // 角色持久化对象
│   │   │               │   │   │   │   ├── PermissionPO.java // 权限持久化对象
│   │   │               │   │   │   │   ├── UserRolePO.java  // 用户角色关联 PO
│   │   │               │   │   │   │   └── RolePermissionPO.java // 角色权限关联 PO
│   │   │               │   │   │   ├── mp
│   │   │               │   │   │   │   ├── package-info.java // 用户权限 MyBatis-Plus 包说明
│   │   │               │   │   │   │   ├── mapper
│   │   │               │   │   │   │   │   ├── package-info.java // 用户权限 Mapper 包说明
│   │   │               │   │   │   │   │   ├── UserMapper.java // 用户 Mapper
│   │   │               │   │   │   │   │   ├── RoleMapper.java // 角色 Mapper
│   │   │               │   │   │   │   │   └── PermissionMapper.java // 权限 Mapper
│   │   │               │   │   │   │   └── service
│   │   │               │   │   │   │       ├── package-info.java // 用户权限 MP Service 包说明
│   │   │               │   │   │   │       ├── UserMpService.java // 用户 MP Service
│   │   │               │   │   │   │       ├── RoleMpService.java // 角色 MP Service
│   │   │               │   │   │   │       ├── PermissionMpService.java // 权限 MP Service
│   │   │               │   │   │   │       └── impl
│   │   │               │   │   │   │           ├── package-info.java // 用户权限 MP Service 实现包说明
│   │   │               │   │   │   │           ├── UserMpServiceImpl.java // 用户 MP Service 实现
│   │   │               │   │   │   │           ├── RoleMpServiceImpl.java // 角色 MP Service 实现
│   │   │               │   │   │   │           └── PermissionMpServiceImpl.java // 权限 MP Service 实现
│   │   │               │   │   │   ├── jpa
│   │   │               │   │   │   │   ├── package-info.java // 用户权限 JPA 包说明
│   │   │               │   │   │   │   ├── UserJpaRepository.java // 用户 JPA Repository
│   │   │               │   │   │   │   ├── RoleJpaRepository.java // 角色 JPA Repository
│   │   │               │   │   │   │   └── PermissionJpaRepository.java // 权限 JPA Repository
│   │   │               │   │   │   └── converter
│   │   │               │   │   │       ├── package-info.java // 用户权限 PO 转换器包说明
│   │   │               │   │   │       ├── UserPOConverter.java // 用户 PO 转换器
│   │   │               │   │   │       ├── RolePOConverter.java // 角色 PO 转换器
│   │   │               │   │   │       └── PermissionPOConverter.java // 权限 PO 转换器
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学 Repository 领域包说明
│   │   │               │   │       ├── impl
│   │   │               │   │       │   ├── package-info.java // 教学仓储实现包说明
│   │   │               │   │       │   ├── SchoolClassRepositoryImpl.java // 班级仓储实现
│   │   │               │   │       │   └── CourseRepositoryImpl.java // 课程仓储实现
│   │   │               │   │       ├── po
│   │   │               │   │       │   ├── package-info.java // 教学 PO 包说明
│   │   │               │   │       │   ├── SchoolClassPO.java // 班级持久化对象
│   │   │               │   │       │   └── CoursePO.java    // 课程持久化对象
│   │   │               │   │       ├── mp
│   │   │               │   │       │   ├── package-info.java // 教学 MyBatis-Plus 包说明
│   │   │               │   │       │   ├── mapper
│   │   │               │   │       │   │   ├── package-info.java // 教学 Mapper 包说明
│   │   │               │   │       │   │   ├── SchoolClassMapper.java // 班级 Mapper
│   │   │               │   │       │   │   └── CourseMapper.java // 课程 Mapper
│   │   │               │   │       │   └── service
│   │   │               │   │       │       ├── package-info.java // 教学 MP Service 包说明
│   │   │               │   │       │       ├── SchoolClassMpService.java // 班级 MP Service
│   │   │               │   │       │       ├── CourseMpService.java // 课程 MP Service
│   │   │               │   │       │       └── impl
│   │   │               │   │       │           ├── package-info.java // 教学 MP Service 实现包说明
│   │   │               │   │       │           ├── SchoolClassMpServiceImpl.java // 班级 MP Service 实现
│   │   │               │   │       │           └── CourseMpServiceImpl.java // 课程 MP Service 实现
│   │   │               │   │       ├── jpa
│   │   │               │   │       │   ├── package-info.java // 教学 JPA 包说明
│   │   │               │   │       │   ├── SchoolClassJpaRepository.java // 班级 JPA Repository
│   │   │               │   │       │   └── CourseJpaRepository.java // 课程 JPA Repository
│   │   │               │   │       └── converter
│   │   │               │   │           ├── package-info.java // 教学 PO 转换器包说明
│   │   │               │   │           ├── SchoolClassPOConverter.java // 班级 PO 转换器
│   │   │               │   │           └── CoursePOConverter.java // 课程 PO 转换器
│   │   │               │   ├── validators
│   │   │               │   │   ├── package-info.java        // 基础设施校验器包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户基础设施校验包说明
│   │   │               │   │   │   └── UserInfraValidator.java // 用户外部数据校验器
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学基础设施校验包说明
│   │   │               │   │       └── TeachingInfraValidator.java // 教学外部数据校验器
│   │   │               │   ├── client
│   │   │               │   │   ├── package-info.java        // 外部 Client 实现根包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户外部 Client 实现包说明
│   │   │               │   │   │   └── impl
│   │   │               │   │   │       ├── package-info.java // 用户外部 Client 实现包说明
│   │   │               │   │   │       └── UserQueryClientImpl.java // 用户查询 Client 实现
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学外部 Client 实现包说明
│   │   │               │   │       └── impl
│   │   │               │   │           ├── package-info.java // 教学外部 Client 实现包说明
│   │   │               │   │           └── TeachingQueryClientImpl.java // 教学查询 Client 实现
│   │   │               │   ├── aop
│   │   │               │   │   ├── package-info.java        // 基础设施 AOP 包说明
│   │   │               │   │   ├── RepositoryMonitorAspect.java // 仓储监控切面
│   │   │               │   │   └── InfraLogAspect.java      // 基础设施日志切面
│   │   │               │   ├── mq
│   │   │               │   │   ├── package-info.java        // MQ 出站发送包说明，仅出站
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户消息发送包说明
│   │   │               │   │   │   └── UserChangedProducer.java // 用户变更消息发送器
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学消息发送包说明
│   │   │               │   │       └── CourseChangedProducer.java // 课程变更消息发送器
│   │   │               │   ├── cache
│   │   │               │   │   ├── package-info.java        // 缓存包说明
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── package-info.java    // 用户缓存包说明
│   │   │               │   │   │   └── UserCache.java       // 用户缓存封装
│   │   │               │   │   └── teaching
│   │   │               │   │       ├── package-info.java    // 教学缓存包说明
│   │   │               │   │       └── CourseCache.java     // 课程缓存封装
│   │   │               │   └── config
│   │   │               │       ├── package-info.java        // 基础设施配置包说明
│   │   │               │       ├── MyBatisPlusConfig.java   // MyBatis-Plus 配置
│   │   │               │       ├── JpaConfig.java           // JPA 配置
│   │   │               │       ├── RedisConfig.java         // Redis 配置
│   │   │               │       └── MqConfig.java            // MQ 配置
│   │   │               │
│   │   │               ├── common
│   │   │               │   ├── package-info.java            // 通用基础层包说明
│   │   │               │   ├── constants
│   │   │               │   │   ├── package-info.java        // 通用常量包说明
│   │   │               │   │   └── CommonConstants.java     // 通用常量
│   │   │               │   ├── utils
│   │   │               │   │   ├── package-info.java        // 通用工具包说明
│   │   │               │   │   ├── IdUtils.java             // ID 工具
│   │   │               │   │   └── DateTimeUtils.java       // 时间工具
│   │   │               │   ├── enums
│   │   │               │   │   ├── package-info.java        // 通用枚举包说明
│   │   │               │   │   └── DeletedStatus.java       // 删除状态枚举
│   │   │               │   └── exceptions
│   │   │               │       ├── package-info.java        // 通用异常包说明
│   │   │               │       ├── BizException.java        // 基础业务异常
│   │   │               │       └── ErrorCode.java           // 基础错误码
│   │   │               │
│   │   │               └── domain
│   │   │                   ├── package-info.java            // 领域核心层包说明
│   │   │                   ├── user
│   │   │                   │   ├── package-info.java        // 用户权限领域包说明
│   │   │                   │   ├── entities
│   │   │                   │   │   ├── package-info.java    // 用户权限实体包说明
│   │   │                   │   │   ├── User.java            // 用户实体
│   │   │                   │   │   ├── Role.java            // 角色实体
│   │   │                   │   │   └── Permission.java      // 权限实体
│   │   │                   │   ├── aggregates
│   │   │                   │   │   ├── package-info.java    // 用户权限聚合包说明
│   │   │                   │   │   ├── UserAggregate.java   // 用户聚合
│   │   │                   │   │   └── RolePermissionAggregate.java // 角色权限聚合
│   │   │                   │   ├── vos
│   │   │                   │   │   ├── package-info.java    // 用户权限值对象包说明
│   │   │                   │   │   ├── UserId.java          // 用户 ID 值对象
│   │   │                   │   │   ├── RoleCode.java        // 角色编码值对象
│   │   │                   │   │   └── PermissionCode.java  // 权限编码值对象
│   │   │                   │   ├── service
│   │   │                   │   │   ├── package-info.java    // 用户权限领域服务接口包说明
│   │   │                   │   │   ├── UserDomainService.java // 用户领域服务
│   │   │                   │   │   ├── RoleDomainService.java // 角色领域服务
│   │   │                   │   │   ├── PermissionDomainService.java // 权限领域服务
│   │   │                   │   │   └── impl
│   │   │                   │   │       ├── package-info.java // 用户权限领域服务实现包说明
│   │   │                   │   │       ├── UserDomainServiceImpl.java // 用户领域服务实现
│   │   │                   │   │       ├── RoleDomainServiceImpl.java // 角色领域服务实现
│   │   │                   │   │       └── PermissionDomainServiceImpl.java // 权限领域服务实现
│   │   │                   │   ├── repos
│   │   │                   │   │   ├── package-info.java    // 用户权限仓储接口包说明，只定义接口
│   │   │                   │   │   ├── UserRepository.java  // 用户仓储接口
│   │   │                   │   │   ├── RoleRepository.java  // 角色仓储接口
│   │   │                   │   │   └── PermissionRepository.java // 权限仓储接口
│   │   │                   │   ├── validators
│   │   │                   │   │   ├── package-info.java    // 用户权限领域校验包说明
│   │   │                   │   │   ├── UserDomainValidator.java // 用户领域校验器
│   │   │                   │   │   ├── RoleDomainValidator.java // 角色领域校验器
│   │   │                   │   │   └── PermissionDomainValidator.java // 权限领域校验器
│   │   │                   │   └── enums
│   │   │                   │       ├── package-info.java    // 用户权限领域枚举包说明
│   │   │                   │       ├── UserStatus.java      // 用户状态枚举
│   │   │                   │       ├── RoleStatus.java      // 角色状态枚举
│   │   │                   │       └── PermissionType.java  // 权限类型枚举
│   │   │                   └── teaching
│   │   │                       ├── package-info.java        // 教学领域包说明
│   │   │                       ├── entities
│   │   │                       │   ├── package-info.java    // 教学实体包说明
│   │   │                       │   ├── SchoolClass.java     // 班级实体，避免使用 Java 关键字 Class
│   │   │                       │   └── Course.java          // 课程实体
│   │   │                       ├── aggregates
│   │   │                       │   ├── package-info.java    // 教学聚合包说明
│   │   │                       │   ├── SchoolClassAggregate.java // 班级聚合
│   │   │                       │   └── CourseAggregate.java // 课程聚合
│   │   │                       ├── vos
│   │   │                       │   ├── package-info.java    // 教学值对象包说明
│   │   │                       │   ├── SchoolClassId.java   // 班级 ID 值对象
│   │   │                       │   ├── CourseCode.java      // 课程编码值对象
│   │   │                       │   └── Semester.java        // 学期值对象
│   │   │                       ├── service
│   │   │                       │   ├── package-info.java    // 教学领域服务接口包说明
│   │   │                       │   ├── SchoolClassDomainService.java // 班级领域服务
│   │   │                       │   ├── CourseDomainService.java // 课程领域服务
│   │   │                       │   └── impl
│   │   │                       │       ├── package-info.java // 教学领域服务实现包说明
│   │   │                       │       ├── SchoolClassDomainServiceImpl.java // 班级领域服务实现
│   │   │                       │       └── CourseDomainServiceImpl.java // 课程领域服务实现
│   │   │                       ├── repos
│   │   │                       │   ├── package-info.java    // 教学仓储接口包说明，只定义接口
│   │   │                       │   ├── SchoolClassRepository.java // 班级仓储接口
│   │   │                       │   └── CourseRepository.java // 课程仓储接口
│   │   │                       ├── validators
│   │   │                       │   ├── package-info.java    // 教学领域校验包说明
│   │   │                       │   ├── SchoolClassDomainValidator.java // 班级领域校验器
│   │   │                       │   └── CourseDomainValidator.java // 课程领域校验器
│   │   │                       └── enums
│   │   │                           ├── package-info.java    // 教学领域枚举包说明
│   │   │                           ├── SchoolClassStatus.java // 班级状态枚举
│   │   │                           └── CourseStatus.java    // 课程状态枚举
│   │   │
│   │   └── resources
│   │       ├── application.yml                              // 默认配置
│   │       ├── application-dev.yml                          // 开发环境配置
│   │       ├── application-test.yml                         // 测试环境配置
│   │       ├── application-prod.yml                         // 生产环境配置
│   │       ├── bootstrap.yml                                // 启动阶段配置，可选
│   │       ├── logback-spring.xml                           // 日志配置
│   │       ├── mapper
│   │       │   ├── user
│   │       │   │   ├── UserMapper.xml                       // 用户 MyBatis XML
│   │       │   │   ├── RoleMapper.xml                       // 角色 MyBatis XML
│   │       │   │   └── PermissionMapper.xml                 // 权限 MyBatis XML
│   │       │   └── teaching
│   │       │       ├── SchoolClassMapper.xml                // 班级 MyBatis XML
│   │       │       └── CourseMapper.xml                     // 课程 MyBatis XML
│   │       ├── db
│   │       │   └── migration
│   │       │       ├── V1__init_user.sql                    // 用户权限表初始化脚本
│   │       │       └── V2__init_teaching.sql                // 教学管理表初始化脚本
│   │       ├── graphql
│   │       │   ├── user.graphqls                            // 用户 GraphQL Schema
│   │       │   └── teaching.graphqls                        // 教学 GraphQL Schema
│   │       └── META-INF
│   │           └── spring
│   │               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports // 自动配置声明，可选
│   │
│   └── test
│       ├── java
│       │   └── com
│       │       └── demo
│       │           └── student
│       │               ├── package-info.java                // 测试根包说明
│       │               ├── start
│       │               │   ├── package-info.java            // 启动测试包说明
│       │               │   └── StudentManagementApplicationTests.java // 应用启动测试
│       │               ├── adapter
│       │               │   ├── package-info.java            // Adapter 测试包说明
│       │               │   └── controller
│       │               │       ├── package-info.java        // Controller 测试包说明
│       │               │       ├── user
│       │               │       │   ├── package-info.java    // 用户 Controller 测试包说明
│       │               │       │   └── UserControllerTest.java // 用户接口测试
│       │               │       └── teaching
│       │               │           ├── package-info.java    // 教学 Controller 测试包说明
│       │               │           └── CourseControllerTest.java // 课程接口测试
│       │               ├── application
│       │               │   ├── package-info.java            // Application 测试包说明
│       │               │   └── manage
│       │               │       ├── package-info.java        // 应用服务测试包说明
│       │               │       ├── user
│       │               │       │   ├── package-info.java    // 用户应用服务测试包说明
│       │               │       │   └── UserManageTest.java  // 用户应用服务测试
│       │               │       └── teaching
│       │               │           ├── package-info.java    // 教学应用服务测试包说明
│       │               │           └── CourseManageTest.java // 课程应用服务测试
│       │               ├── domain
│       │               │   ├── package-info.java            // Domain 测试包说明
│       │               │   ├── user
│       │               │   │   ├── package-info.java        // 用户领域测试包说明
│       │               │   │   └── service
│       │               │   │       ├── package-info.java    // 用户领域服务测试包说明
│       │               │   │       └── UserDomainServiceTest.java // 用户领域服务测试
│       │               │   └── teaching
│       │               │       ├── package-info.java        // 教学领域测试包说明
│       │               │       └── service
│       │               │           ├── package-info.java    // 教学领域服务测试包说明
│       │               │           └── CourseDomainServiceTest.java // 课程领域服务测试
│       │               ├── infrastructure
│       │               │   ├── package-info.java            // Infrastructure 测试包说明
│       │               │   └── repo
│       │               │       ├── package-info.java        // 仓储测试包说明
│       │               │       ├── user
│       │               │       │   ├── package-info.java    // 用户仓储测试包说明
│       │               │       │   └── impl
│       │               │       │       ├── package-info.java // 用户仓储实现测试包说明
│       │               │       │       └── UserRepositoryImplTest.java // 用户仓储实现测试
│       │               │       └── teaching
│       │               │           ├── package-info.java    // 教学仓储测试包说明
│       │               │           └── impl
│       │               │               ├── package-info.java // 教学仓储实现测试包说明
│       │               │               └── CourseRepositoryImplTest.java // 课程仓储实现测试
│       │               └── common
│       │                   ├── package-info.java            // Common 测试包说明
│       │                   └── utils
│       │                       ├── package-info.java        // 通用工具测试包说明
│       │                       └── IdUtilsTest.java         // ID 工具测试
│       │
│       └── resources
│           ├── application-test.yml                         // 单元测试 / 集成测试配置
│           ├── logback-test.xml                             // 测试日志配置
│           ├── db
│           │   ├── schema-test.sql                          // 测试库表结构
│           │   └── data-test.sql                            // 测试基础数据
│           ├── mapper
│           │   ├── user
│           │   │   └── UserMapperTest.xml                   // 用户 Mapper 测试 XML，可选
│           │   └── teaching
│           │       └── CourseMapperTest.xml                 // 课程 Mapper 测试 XML，可选
│           └── testdata
│               ├── user
│               │   ├── create-user-request.json             // 创建用户测试请求
│               │   └── assign-role-request.json             // 分配角色测试请求
│               └── teaching
│                   ├── create-school-class-request.json     // 创建班级测试请求
│                   └── create-course-request.json           // 创建课程测试请求
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
5. adapter 不允许直接调用 repository impl。
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
RPC Provider -> Application
GraphQL Resolver -> Application
FacadeImpl -> Application
```

不允许：

```text
Controller -> Mapper
Controller -> RedisTemplate
Controller -> RepositoryImpl
Controller -> Domain Repository
Controller -> Domain Service
```

### 5.3 facade 约束

```text
1. facade 只定义接口契约。
2. facade 放 DTO、接口、对外枚举、对外异常、契约工具。
3. facade 不写实现类。
4. facade 不依赖 common。
5. facade 不依赖 application。
6. facade 不依赖 domain。
7. facade 不依赖 infrastructure。
8. facade 不依赖 adapter。
```

### 5.4 application 约束

```text
1. application 负责业务用例编排。
2. application 可以调用 domain service。
3. application 可以调用 domain repos 接口。
4. application 可以调用 application client 接口。
5. application 负责事务控制。
6. application 不依赖 infrastructure。
7. application 不直接调用 mapper。
8. application 不直接调用 JPA Repository。
9. application 不直接调用 RedisTemplate。
10. application 不直接调用 MQ Template。
11. application 不直接调用外部 RPC / HTTP 实现。
12. application 不实现 Facade 接口。
13. application 不包含 facade.impl 包。
14. manage 包必须按 manage.user.impl、manage.teaching.impl 这种方式组织。
```

允许：

```text
Application -> Domain Service
Application -> Domain Repository Interface
Application -> Application Client Interface
Application -> Application Validator
Application -> Assembler
```

不允许：

```text
Application -> Mapper
Application -> JpaRepository
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
2. infrastructure 实现 domain repos 接口。
3. infrastructure 实现 application client 接口。
4. infrastructure 可以调用 mapper。
5. infrastructure 可以调用 JPA Repository。
6. infrastructure 可以调用 RedisTemplate。
7. infrastructure 可以调用 MQ Template。
8. infrastructure 可以调用外部 HTTP / RPC / SDK。
9. infrastructure 不写核心业务规则。
10. infrastructure.mq 只负责出站消息发送。
11. infrastructure.repo 必须按照 repo.user.*、repo.teaching.* 这种方式分领域组织。
```

允许：

```text
RepositoryImpl -> MpService -> Mapper
RepositoryImpl -> JpaRepository
ClientImpl -> ExternalFacade
ClientImpl -> HTTP Client
MQ Producer -> KafkaTemplate / RabbitTemplate
Cache -> RedisTemplate
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
4. common 不放业务异常。
5. common 不放业务 Redis Key。
6. common 不放业务表名常量。
7. common 不被 facade 依赖。
```

### 5.7 domain 约束

```text
1. domain 只表达业务规则。
2. domain 定义实体、聚合、值对象、领域服务、仓储接口。
3. domain 不依赖 application。
4. domain 不依赖 infrastructure。
5. domain 不依赖 adapter。
6. domain 不依赖 facade。
7. domain 不依赖 MyBatis-Plus。
8. domain 不依赖 JPA。
9. domain 不依赖 Redis。
10. domain 不依赖 MQ。
11. domain 不依赖 HTTP / RPC 技术实现。
12. domain service 包必须按 service / service.impl 组织。
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
