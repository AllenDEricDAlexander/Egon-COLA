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
facade          RPC 对外契约层
application     应用编排层
domain          领域核心层
infrastructure  基础设施层
common          通用基础层
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

adapter 层只保留：

```text
rpc             RPC 入站
mq              MQ 入站
facade.impl     Facade 实现，只能放在 adapter
convertor       入站对象转换
dto             入站 DTO / Message DTO
handler         RPC / MQ 异常处理器，不处理 Web
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
    - examing 领域
```

说明：

```text
1. student-management-organization 是一个独立工程，不是根聚合工程下的普通模块。
2. student-management-evaluation 是另一个独立工程，不是根聚合工程下的普通模块。
3. 每个 Project 内部再拆 starter / common / facade / application / domain / infrastructure / adapter 等 Maven 子模块。
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
student-management-organization-adapter import student-management-organization-facade

student-management-organization-application import student-management-organization-domain

student-management-organization-domain import student-management-organization-common

student-management-organization-infrastructure import student-management-organization-application
```

`facade` 不依赖 `common`。

`facade` 内部有自己的：

```text
utils
enums
exceptions
dto
```

也就是说：

```text
facade 是自包含契约模块。
facade 不依赖 common。
facade 不依赖 application。
facade 不依赖 domain。
facade 不依赖 infrastructure。
```

## 2.2 依赖关系图

```text
                        starter
                           |
            -------------------------------
            |                             |
         adapter                    infrastructure
            |                             |
     ----------------                  application
     |              |                       |
 application     facade                  domain
     |                                      |
   domain                                  common
     |
   common
```

说明：

```text
1. starter 负责启动和装配 adapter、infrastructure。
2. adapter 负责 RPC / MQ 入站，依赖 application 和 facade。
3. facade 是 RPC 契约模块，不依赖 common。
4. application 负责编排业务流程，依赖 domain。
5. domain 负责核心规则，依赖 common。
6. infrastructure 负责技术实现，依赖 application。
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
    student-management-organization-facade/
    student-management-organization-application/
    student-management-organization-domain/
    student-management-organization-infrastructure/
    student-management-organization-adapter/

student-management-evaluation/
    pom.xml
    student-management-evaluation-starter/
    student-management-evaluation-common/
    student-management-evaluation-facade/
    student-management-evaluation-application/
    student-management-evaluation-domain/
    student-management-evaluation-infrastructure/
    student-management-evaluation-adapter/
```

如果两个 Project 之间需要调用，不能直接依赖对方的 application、domain、infrastructure。

推荐方式：

```text
1. 通过对方 facade 进行 RPC 调用。
2. 通过 MQ 进行事件通知。
3. 通过 infrastructure.client.impl 封装外部调用细节。
```

例如：

```text
student-management-evaluation-infrastructure
    -> OrganizationUserFacade
    -> student-management-organization-adapter/facade.impl
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

`adapter` 是入站适配层。

纯 Service 项目中，adapter 只负责：

```text
1. RPC 入站。
2. MQ 入站。
3. Facade 实现。
4. 入站 DTO 转换。
5. RPC / MQ 异常处理。
```

### 3.2.2 推荐结构

```text
adapter
    - rpc
    - mq
    - facade.impl
    - convertor
    - dto
    - handler
    - package-info.java
```

明确不允许出现：

```text
adapter/controller
adapter/web
adapter/filter
adapter/graphql
adapter/vo
```

### 3.2.3 能做什么

```text
1. 暴露 Dubbo Provider。
2. 暴露 gRPC Provider。
3. 实现 facade 接口。
4. 消费 Kafka / RocketMQ / RabbitMQ 等入站消息。
5. 将 MQ Message DTO 转换为 application 入参。
6. 将 RPC DTO 转换为 application 入参。
7. 将 application 返回值转换为 facade DTO。
8. 处理 RPC 层异常映射。
9. 处理 MQ 消费异常和重试入口。
```

### 3.2.4 不能做什么

```text
1. 不提供 HTTP Controller。
2. 不提供 Web Filter。
3. 不提供 Web Interceptor。
4. 不提供 GraphQL Resolver。
5. 不定义 VO。
6. 不直接调用 Mapper。
7. 不直接调用 JPA Repository。
8. 不直接调用 RedisTemplate。
9. 不直接发送 MQ。
10. 不写核心业务规则。
11. 不绕过 application 直接调用 domain。
```

### 3.2.5 调用链路

```text
RPC Provider -> Application
FacadeImpl   -> Application
MQ Consumer  -> Application
```

---

## 3.3 facade 模块

### 3.3.1 职责

`facade` 是 RPC 对外契约层。

它只定义：

```text
1. Facade 接口。
2. RPC 请求 DTO。
3. RPC 响应 DTO。
4. 对外枚举。
5. 对外异常。
6. Facade 内部工具类。
```

### 3.3.2 推荐结构

```text
facade
    - api
    - dto
    - enums
    - exceptions
    - utils
    - package-info.java
```

### 3.3.3 能做什么

```text
1. 定义 RPC 接口。
2. 定义 RPC DTO。
3. 定义 RPC 对外枚举。
4. 定义 RPC 对外异常。
5. 定义 facade 自己需要的轻量工具。
6. 被其他工程作为 RPC 契约依赖。
```

### 3.3.4 不能做什么

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
10. 不写 MQ 消费逻辑。
```

---

## 3.4 application 模块

### 3.4.1 职责

`application` 是应用编排层，负责组织一次完整业务用例。

它不关心请求来自 RPC 还是 MQ。

### 3.4.2 推荐结构

```text
application
    - manage
    - convertor
    - validators
    - assemblers
    - client
    - package-info.java
```

其中 `manage` 按领域拆分：

```text
application/manage/user
application/manage/user/impl
application/manage/teaching
application/manage/teaching/impl
```

注意方向是：

```text
manage.user.impl
manage.teaching.impl
```

不是：

```text
manage.impl.user
manage.impl.teaching
```

### 3.4.3 能做什么

```text
1. 编排业务流程。
2. 控制事务边界。
3. 调用 domain service。
4. 调用 domain repository 接口。
5. 调用 application client 接口。
6. 做应用级校验。
7. 做权限、幂等、流程前置校验。
8. 做 RPC / MQ 入参到领域模型的转换。
9. 聚合多个领域完成一个业务用例。
```

### 3.4.4 不能做什么

```text
1. 不直接调用 Mapper。
2. 不直接调用 JPA Repository。
3. 不直接调用 RedisTemplate。
4. 不直接调用 KafkaTemplate / RabbitTemplate。
5. 不直接调用外部 RPC 实现。
6. 不写 RPC Provider。
7. 不写 MQ Consumer。
8. 不写 Web 相关逻辑。
```

---

## 3.5 domain 模块

### 3.5.1 职责

`domain` 是领域核心层，负责实体、聚合、值对象、领域服务、仓储接口、领域规则。

### 3.5.2 推荐结构

```text
domain
    - entities
    - aggregates
    - vos
    - service
    - repos
    - validators
    - enums
    - package-info.java
```

领域服务必须使用：

```text
service
service.impl
```

不使用：

```text
domainservices
domainservicesimpl
```

### 3.5.3 能做什么

```text
1. 定义领域实体。
2. 定义聚合。
3. 定义值对象。
4. 定义领域服务接口。
5. 定义领域服务实现。
6. 定义领域仓储接口。
7. 定义领域校验器。
8. 定义领域枚举。
9. 表达核心业务规则。
```

### 3.5.4 不能做什么

```text
1. 不依赖 application。
2. 不依赖 infrastructure。
3. 不依赖 adapter。
4. 不依赖 facade。
5. 不依赖 MyBatis-Plus。
6. 不依赖 JPA。
7. 不依赖 Redis。
8. 不依赖 MQ。
9. 不依赖 Dubbo / gRPC 技术实现。
```

---

## 3.6 infrastructure 模块

### 3.6.1 职责

`infrastructure` 是基础设施层，负责数据库、缓存、MQ 出站、外部 RPC 调用、第三方 SDK 等技术实现。

### 3.6.2 推荐结构

```text
infrastructure
    - repo
    - client
    - mq
    - cache
    - config
    - aop
    - validators
    - package-info.java
```

其中 `repo` 必须按领域拆分：

```text
repo.user.impl
repo.user.po
repo.user.mp
repo.user.jpa
repo.user.converter

repo.teaching.impl
repo.teaching.po
repo.teaching.mp
repo.teaching.jpa
repo.teaching.converter
```

不能写成：

```text
repo.impl.user
repo.po.user
repo.mp.user
repo.jpa.user
```

### 3.6.3 能做什么

```text
1. 实现 domain repository 接口。
2. 调用 MyBatis-Plus Service。
3. 调用 MyBatis Mapper。
4. 调用 JPA Repository。
5. 实现 application client 接口。
6. 调用外部 Facade。
7. 调用外部 gRPC / Dubbo / HTTP Client。
8. 发送出站 MQ 消息。
9. 封装 Redis、Caffeine 等缓存。
10. 定义基础设施配置。
```

### 3.6.4 不能做什么

```text
1. 不写核心业务规则。
2. 不消费入站 MQ 消息。
3. 不暴露 RPC Provider。
4. 不实现 Facade 接口。
5. 不处理 Web 请求。
6. 不让 application 直接感知 mapper / jpa / redis / mq。
7. 不让 domain 感知基础设施实现。
```

---

## 3.7 common 模块

### 3.7.1 职责

`common` 是当前 Project 内部通用基础层。

注意：`common` 不给 `facade` 使用。`facade` 有自己的 utils、enums、exceptions。

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
├── student-management-organization-facade
├── student-management-organization-domain
├── student-management-organization-application
├── student-management-organization-infrastructure
└── student-management-organization-adapter

student-management-evaluation/
├── pom.xml
├── student-management-evaluation-starter
├── student-management-evaluation-common
├── student-management-evaluation-facade
├── student-management-evaluation-domain
├── student-management-evaluation-application
├── student-management-evaluation-infrastructure
└── student-management-evaluation-adapter
```

---

## 4.2 student-management-organization 结构示例

`student-management-organization` 包含两个领域：

```text
user      用户、角色、权限
teaching  班级、年级、教学组织
```

### 4.2.1 starter

```text
student-management-organization-starter
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/student/organization/starter
│   │   │       ├── OrganizationServiceApplication.java          // 纯 Service 启动类
│   │   │       ├── config
│   │   │       │   ├── RpcProviderConfig.java                   // RPC Provider 配置
│   │   │       │   ├── MqConsumerConfig.java                    // MQ Consumer 配置
│   │   │       │   ├── ServiceThreadPoolConfig.java             // 服务线程池配置
│   │   │       │   └── package-info.java
│   │   │       └── package-info.java
│   │   └── resources
│   │       ├── application.yml                                  // 应用配置
│   │       ├── application-dev.yml                              // 开发环境配置
│   │       ├── application-prod.yml                             // 生产环境配置
│   │       ├── bootstrap.yml                                    // 启动引导配置
│   │       ├── logback-spring.xml                               // 日志配置
│   │       └── META-INF
│   │           └── spring
│   │               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── test
│       ├── java
│       │   └── com/example/student/organization/starter
│       │       ├── OrganizationServiceApplicationTest.java      // 启动测试
│       │       └── package-info.java
│       └── resources
│           ├── application-test.yml                             // 测试环境配置
│           └── logback-test.xml                                 // 测试日志配置
```

### 4.2.2 common

```text
student-management-organization-common
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/common
│   │           ├── constants
│   │           │   ├── OrganizationConstants.java               // 组织服务通用常量
│   │           │   └── package-info.java
│   │           ├── utils
│   │           │   ├── OrganizationIdUtils.java                 // 组织服务 ID 工具
│   │           │   ├── OrganizationDateUtils.java               // 组织服务日期工具
│   │           │   └── package-info.java
│   │           ├── enums
│   │           │   ├── YesNoEnum.java                           // 通用是否枚举
│   │           │   └── package-info.java
│   │           ├── exceptions
│   │           │   ├── OrganizationBizException.java            // 组织服务基础业务异常
│   │           │   ├── OrganizationErrorCode.java               // 组织服务基础错误码
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/organization/common
│       │       ├── OrganizationIdUtilsTest.java                 // 工具类测试
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

### 4.2.3 facade

```text
student-management-organization-facade
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/facade
│   │           ├── api
│   │           │   ├── UserFacade.java                          // 用户 RPC 契约
│   │           │   ├── RoleFacade.java                          // 角色 RPC 契约
│   │           │   ├── PermissionFacade.java                    // 权限 RPC 契约
│   │           │   ├── TeachingFacade.java                      // 教学组织 RPC 契约
│   │           │   └── package-info.java
│   │           ├── dto
│   │           │   ├── user
│   │           │   │   ├── CreateUserRpcRequest.java             // 创建用户 RPC 请求
│   │           │   │   ├── UserRpcResponse.java                  // 用户 RPC 响应
│   │           │   │   ├── AssignRoleRpcRequest.java             // 分配角色 RPC 请求
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── CreateSchoolClassRpcRequest.java      // 创建班级 RPC 请求
│   │           │   │   ├── SchoolClassRpcResponse.java           // 班级 RPC 响应
│   │           │   │   ├── GradeRpcResponse.java                 // 年级 RPC 响应
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── enums
│   │           │   ├── OrganizationFacadeStatus.java             // Facade 对外状态枚举
│   │           │   ├── OrganizationFacadeErrorCode.java          // Facade 对外错误码
│   │           │   └── package-info.java
│   │           ├── exceptions
│   │           │   ├── OrganizationFacadeException.java          // Facade 对外异常
│   │           │   └── package-info.java
│   │           ├── utils
│   │           │   ├── OrganizationFacadeAssert.java             // Facade 断言工具
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/organization/facade
│       │       ├── OrganizationFacadeDtoTest.java                // Facade DTO 测试
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

### 4.2.4 domain

```text
student-management-organization-domain
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/domain
│   │           ├── entities
│   │           │   ├── user
│   │           │   │   ├── User.java                             // 用户实体
│   │           │   │   ├── Role.java                             // 角色实体
│   │           │   │   ├── Permission.java                       // 权限实体
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClass.java                      // 班级实体
│   │           │   │   ├── Grade.java                            // 年级实体
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── aggregates
│   │           │   ├── user
│   │           │   │   ├── UserAggregate.java                    // 用户聚合
│   │           │   │   ├── RolePermissionAggregate.java          // 角色权限聚合
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassAggregate.java             // 班级聚合
│   │           │   │   ├── GradeTeachingAggregate.java           // 年级教学聚合
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── vos
│   │           │   ├── user
│   │           │   │   ├── UserId.java                           // 用户 ID 值对象
│   │           │   │   ├── RoleCode.java                         // 角色编码值对象
│   │           │   │   ├── PermissionCode.java                   // 权限编码值对象
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassId.java                    // 班级 ID 值对象
│   │           │   │   ├── GradeCode.java                        // 年级编码值对象
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── service
│   │           │   ├── user
│   │           │   │   ├── UserDomainService.java                // 用户领域服务接口
│   │           │   │   ├── RoleDomainService.java                // 角色领域服务接口
│   │           │   │   ├── PermissionDomainService.java          // 权限领域服务接口
│   │           │   │   ├── impl
│   │           │   │   │   ├── UserDomainServiceImpl.java        // 用户领域服务实现
│   │           │   │   │   ├── RoleDomainServiceImpl.java        // 角色领域服务实现
│   │           │   │   │   ├── PermissionDomainServiceImpl.java  // 权限领域服务实现
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassDomainService.java         // 班级领域服务接口
│   │           │   │   ├── GradeDomainService.java               // 年级领域服务接口
│   │           │   │   ├── impl
│   │           │   │   │   ├── SchoolClassDomainServiceImpl.java // 班级领域服务实现
│   │           │   │   │   ├── GradeDomainServiceImpl.java       // 年级领域服务实现
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── repos
│   │           │   ├── user
│   │           │   │   ├── UserRepository.java                   // 用户仓储接口
│   │           │   │   ├── RoleRepository.java                   // 角色仓储接口
│   │           │   │   ├── PermissionRepository.java             // 权限仓储接口
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassRepository.java            // 班级仓储接口
│   │           │   │   ├── GradeRepository.java                  // 年级仓储接口
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── validators
│   │           │   ├── user
│   │           │   │   ├── UserDomainValidator.java              // 用户领域校验器
│   │           │   │   ├── RoleDomainValidator.java              // 角色领域校验器
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassDomainValidator.java       // 班级领域校验器
│   │           │   │   ├── GradeDomainValidator.java             // 年级领域校验器
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── enums
│   │           │   ├── user
│   │           │   │   ├── UserStatus.java                       // 用户状态枚举
│   │           │   │   ├── RoleStatus.java                       // 角色状态枚举
│   │           │   │   ├── PermissionType.java                   // 权限类型枚举
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassStatus.java                // 班级状态枚举
│   │           │   │   ├── GradeStatus.java                      // 年级状态枚举
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/organization/domain
│       │       ├── service
│       │       │   ├── user
│       │       │   │   ├── UserDomainServiceTest.java            // 用户领域服务测试
│       │       │   │   └── package-info.java
│       │       │   ├── teaching
│       │       │   │   ├── SchoolClassDomainServiceTest.java     // 班级领域服务测试
│       │       │   │   └── package-info.java
│       │       │   └── package-info.java
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

### 4.2.5 application

```text
student-management-organization-application
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/application
│   │           ├── manage
│   │           │   ├── user
│   │           │   │   ├── UserManage.java                       // 用户应用服务接口
│   │           │   │   ├── RoleManage.java                       // 角色应用服务接口
│   │           │   │   ├── PermissionManage.java                 // 权限应用服务接口
│   │           │   │   ├── impl
│   │           │   │   │   ├── UserManageImpl.java               // 用户应用服务实现
│   │           │   │   │   ├── RoleManageImpl.java               // 角色应用服务实现
│   │           │   │   │   ├── PermissionManageImpl.java         // 权限应用服务实现
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassManage.java                // 班级应用服务接口
│   │           │   │   ├── GradeManage.java                      // 年级应用服务接口
│   │           │   │   ├── impl
│   │           │   │   │   ├── SchoolClassManageImpl.java        // 班级应用服务实现
│   │           │   │   │   ├── GradeManageImpl.java              // 年级应用服务实现
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── convertor
│   │           │   ├── user
│   │           │   │   ├── UserApplicationConvertor.java         // 用户应用层转换器
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassApplicationConvertor.java  // 班级应用层转换器
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── validators
│   │           │   ├── user
│   │           │   │   ├── UserApplicationValidator.java         // 用户应用校验器
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── TeachingApplicationValidator.java     // 教学应用校验器
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── assemblers
│   │           │   ├── user
│   │           │   │   ├── UserAssembler.java                    // 用户对象装配器
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassAssembler.java             // 班级对象装配器
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── client
│   │           │   ├── evaluation
│   │           │   │   ├── CourseClient.java                     // 课程服务调用接口
│   │           │   │   ├── ExamClient.java                       // 考试服务调用接口
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/organization/application
│       │       ├── manage
│       │       │   ├── user
│       │       │   │   ├── UserManageImplTest.java               // 用户应用服务测试
│       │       │   │   └── package-info.java
│       │       │   ├── teaching
│       │       │   │   ├── SchoolClassManageImplTest.java        // 班级应用服务测试
│       │       │   │   └── package-info.java
│       │       │   └── package-info.java
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

### 4.2.6 infrastructure

```text
student-management-organization-infrastructure
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/infrastructure
│   │           ├── repo
│   │           │   ├── user
│   │           │   │   ├── impl
│   │           │   │   │   ├── UserRepositoryImpl.java           // 用户仓储实现
│   │           │   │   │   ├── RoleRepositoryImpl.java           // 角色仓储实现
│   │           │   │   │   ├── PermissionRepositoryImpl.java     // 权限仓储实现
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── po
│   │           │   │   │   ├── UserPO.java                       // 用户持久化对象
│   │           │   │   │   ├── RolePO.java                       // 角色持久化对象
│   │           │   │   │   ├── PermissionPO.java                 // 权限持久化对象
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── mp
│   │           │   │   │   ├── mapper
│   │           │   │   │   │   ├── UserMapper.java               // 用户 Mapper
│   │           │   │   │   │   ├── RoleMapper.java               // 角色 Mapper
│   │           │   │   │   │   └── package-info.java
│   │           │   │   │   ├── service
│   │           │   │   │   │   ├── UserMpService.java            // 用户 MP Service
│   │           │   │   │   │   ├── RoleMpService.java            // 角色 MP Service
│   │           │   │   │   │   ├── impl
│   │           │   │   │   │   │   ├── UserMpServiceImpl.java    // 用户 MP Service 实现
│   │           │   │   │   │   │   ├── RoleMpServiceImpl.java    // 角色 MP Service 实现
│   │           │   │   │   │   │   └── package-info.java
│   │           │   │   │   │   └── package-info.java
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── jpa
│   │           │   │   │   ├── UserJpaRepository.java            // 用户 JPA Repository
│   │           │   │   │   ├── RoleJpaRepository.java            // 角色 JPA Repository
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── converter
│   │           │   │   │   ├── UserPOConverter.java              // 用户 PO 转换器
│   │           │   │   │   ├── RolePOConverter.java              // 角色 PO 转换器
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── impl
│   │           │   │   │   ├── SchoolClassRepositoryImpl.java    // 班级仓储实现
│   │           │   │   │   ├── GradeRepositoryImpl.java          // 年级仓储实现
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── po
│   │           │   │   │   ├── SchoolClassPO.java                // 班级持久化对象
│   │           │   │   │   ├── GradePO.java                      // 年级持久化对象
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── mp
│   │           │   │   │   ├── mapper
│   │           │   │   │   │   ├── SchoolClassMapper.java        // 班级 Mapper
│   │           │   │   │   │   ├── GradeMapper.java              // 年级 Mapper
│   │           │   │   │   │   └── package-info.java
│   │           │   │   │   ├── service
│   │           │   │   │   │   ├── SchoolClassMpService.java     // 班级 MP Service
│   │           │   │   │   │   ├── GradeMpService.java           // 年级 MP Service
│   │           │   │   │   │   ├── impl
│   │           │   │   │   │   │   ├── SchoolClassMpServiceImpl.java // 班级 MP Service 实现
│   │           │   │   │   │   │   ├── GradeMpServiceImpl.java   // 年级 MP Service 实现
│   │           │   │   │   │   │   └── package-info.java
│   │           │   │   │   │   └── package-info.java
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── jpa
│   │           │   │   │   ├── SchoolClassJpaRepository.java     // 班级 JPA Repository
│   │           │   │   │   ├── GradeJpaRepository.java           // 年级 JPA Repository
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── converter
│   │           │   │   │   ├── SchoolClassPOConverter.java       // 班级 PO 转换器
│   │           │   │   │   ├── GradePOConverter.java             // 年级 PO 转换器
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── client
│   │           │   ├── evaluation
│   │           │   │   ├── impl
│   │           │   │   │   ├── CourseClientImpl.java             // 课程服务调用实现
│   │           │   │   │   ├── ExamClientImpl.java               // 考试服务调用实现
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── mq
│   │           │   ├── user
│   │           │   │   ├── UserChangedProducer.java              // 用户变更出站消息发送器
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassChangedProducer.java       // 班级变更出站消息发送器
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── cache
│   │           │   ├── user
│   │           │   │   ├── UserCache.java                        // 用户缓存
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassCache.java                 // 班级缓存
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── config
│   │           │   ├── MybatisPlusConfig.java                    // MyBatis-Plus 配置
│   │           │   ├── JpaConfig.java                            // JPA 配置
│   │           │   ├── RedisConfig.java                          // Redis 配置
│   │           │   ├── MqProducerConfig.java                     // MQ Producer 配置
│   │           │   └── package-info.java
│   │           ├── aop
│   │           │   ├── InfrastructureTraceAspect.java            // 基础设施追踪切面
│   │           │   └── package-info.java
│   │           ├── validators
│   │           │   ├── ExternalResponseValidator.java            // 外部响应校验器
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/organization/infrastructure
│       │       ├── repo
│       │       │   ├── user
│       │       │   │   ├── UserRepositoryImplTest.java           // 用户仓储实现测试
│       │       │   │   └── package-info.java
│       │       │   ├── teaching
│       │       │   │   ├── SchoolClassRepositoryImplTest.java    // 班级仓储实现测试
│       │       │   │   └── package-info.java
│       │       │   └── package-info.java
│       │       └── package-info.java
│       └── resources
│           ├── application-test.yml
│           └── mapper
│               ├── user
│               └── teaching
```

### 4.2.7 adapter

```text
student-management-organization-adapter
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/adapter
│   │           ├── rpc
│   │           │   ├── user
│   │           │   │   ├── UserRpcProvider.java                  // 用户 RPC Provider
│   │           │   │   ├── RoleRpcProvider.java                  // 角色 RPC Provider
│   │           │   │   ├── PermissionRpcProvider.java            // 权限 RPC Provider
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── TeachingRpcProvider.java              // 教学组织 RPC Provider
│   │           │   │   ├── SchoolClassRpcProvider.java           // 班级 RPC Provider
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── mq
│   │           │   ├── user
│   │           │   │   ├── UserImportConsumer.java               // 用户导入入站消息消费者
│   │           │   │   ├── RoleSyncConsumer.java                 // 角色同步入站消息消费者
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── SchoolClassImportConsumer.java        // 班级导入入站消息消费者
│   │           │   │   ├── GradeSyncConsumer.java                // 年级同步入站消息消费者
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── facade
│   │           │   ├── impl
│   │           │   │   ├── user
│   │           │   │   │   ├── UserFacadeImpl.java               // 用户 Facade 实现，只能放 adapter
│   │           │   │   │   ├── RoleFacadeImpl.java               // 角色 Facade 实现，只能放 adapter
│   │           │   │   │   ├── PermissionFacadeImpl.java         // 权限 Facade 实现，只能放 adapter
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── teaching
│   │           │   │   │   ├── TeachingFacadeImpl.java           // 教学组织 Facade 实现，只能放 adapter
│   │           │   │   │   ├── SchoolClassFacadeImpl.java        // 班级 Facade 实现，只能放 adapter
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── convertor
│   │           │   ├── user
│   │           │   │   ├── UserRpcConvertor.java                 // 用户 RPC 转换器
│   │           │   │   ├── UserMqConvertor.java                  // 用户 MQ 转换器
│   │           │   │   └── package-info.java
│   │           │   ├── teaching
│   │           │   │   ├── TeachingRpcConvertor.java             // 教学 RPC 转换器
│   │           │   │   ├── TeachingMqConvertor.java              // 教学 MQ 转换器
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── dto
│   │           │   ├── mq
│   │           │   │   ├── user
│   │           │   │   │   ├── UserImportMessage.java            // 用户导入消息 DTO
│   │           │   │   │   ├── RoleSyncMessage.java              // 角色同步消息 DTO
│   │           │   │   │   └── package-info.java
│   │           │   │   ├── teaching
│   │           │   │   │   ├── SchoolClassImportMessage.java     // 班级导入消息 DTO
│   │           │   │   │   ├── GradeSyncMessage.java             // 年级同步消息 DTO
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── handler
│   │           │   ├── RpcExceptionHandler.java                  // RPC 异常处理器
│   │           │   ├── MqConsumeExceptionHandler.java            // MQ 消费异常处理器
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/organization/adapter
│       │       ├── rpc
│       │       │   ├── user
│       │       │   │   ├── UserRpcProviderTest.java              // 用户 RPC Provider 测试
│       │       │   │   └── package-info.java
│       │       │   └── package-info.java
│       │       ├── mq
│       │       │   ├── user
│       │       │   │   ├── UserImportConsumerTest.java           // 用户 MQ Consumer 测试
│       │       │   │   └── package-info.java
│       │       │   └── package-info.java
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

---

## 4.3 student-management-evaluation 结构示例

`student-management-evaluation` 包含两个领域：

```text
course   课程领域
examing  考试、成绩领域
```

说明：`examing` 按你的命名保留。如果后续想更自然，也可以改成 `exam` 或 `examination`。

### 4.3.1 starter

```text
student-management-evaluation-starter
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/student/evaluation/starter
│   │   │       ├── EvaluationServiceApplication.java            // 纯 Service 启动类
│   │   │       ├── config
│   │   │       │   ├── RpcProviderConfig.java                   // RPC Provider 配置
│   │   │       │   ├── MqConsumerConfig.java                    // MQ Consumer 配置
│   │   │       │   ├── ServiceThreadPoolConfig.java             // 服务线程池配置
│   │   │       │   └── package-info.java
│   │   │       └── package-info.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── bootstrap.yml
│   │       └── logback-spring.xml
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/starter
│       │       ├── EvaluationServiceApplicationTest.java        // 启动测试
│       │       └── package-info.java
│       └── resources
│           ├── application-test.yml
│           └── logback-test.xml
```

### 4.3.2 common

```text
student-management-evaluation-common
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/common
│   │           ├── constants
│   │           │   ├── EvaluationConstants.java                  // 评价服务通用常量
│   │           │   └── package-info.java
│   │           ├── utils
│   │           │   ├── EvaluationIdUtils.java                    // 评价服务 ID 工具
│   │           │   ├── ScoreUtils.java                           // 成绩工具
│   │           │   └── package-info.java
│   │           ├── enums
│   │           │   ├── YesNoEnum.java                            // 通用是否枚举
│   │           │   └── package-info.java
│   │           ├── exceptions
│   │           │   ├── EvaluationBizException.java               // 评价服务基础异常
│   │           │   ├── EvaluationErrorCode.java                  // 评价服务基础错误码
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/common
│       │       ├── ScoreUtilsTest.java                           // 成绩工具测试
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

### 4.3.3 facade

```text
student-management-evaluation-facade
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/facade
│   │           ├── api
│   │           │   ├── CourseFacade.java                         // 课程 RPC 契约
│   │           │   ├── ExamFacade.java                           // 考试 RPC 契约
│   │           │   ├── ScoreFacade.java                          // 成绩 RPC 契约
│   │           │   └── package-info.java
│   │           ├── dto
│   │           │   ├── course
│   │           │   │   ├── CreateCourseRpcRequest.java            // 创建课程 RPC 请求
│   │           │   │   ├── CourseRpcResponse.java                 // 课程 RPC 响应
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── CreateExamRpcRequest.java              // 创建考试 RPC 请求
│   │           │   │   ├── ExamRpcResponse.java                   // 考试 RPC 响应
│   │           │   │   ├── ScoreRpcResponse.java                  // 成绩 RPC 响应
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── enums
│   │           │   ├── EvaluationFacadeStatus.java                // Facade 对外状态枚举
│   │           │   ├── EvaluationFacadeErrorCode.java             // Facade 对外错误码
│   │           │   └── package-info.java
│   │           ├── exceptions
│   │           │   ├── EvaluationFacadeException.java             // Facade 对外异常
│   │           │   └── package-info.java
│   │           ├── utils
│   │           │   ├── EvaluationFacadeAssert.java                // Facade 断言工具
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/facade
│       │       ├── EvaluationFacadeDtoTest.java                   // Facade DTO 测试
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

### 4.3.4 domain

```text
student-management-evaluation-domain
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/domain
│   │           ├── entities
│   │           │   ├── course
│   │           │   │   ├── Course.java                           // 课程实体
│   │           │   │   ├── CourseSchedule.java                   // 课程安排实体
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── Exam.java                             // 考试实体
│   │           │   │   ├── ExamPaper.java                        // 试卷实体
│   │           │   │   ├── Score.java                            // 成绩实体
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── aggregates
│   │           │   ├── course
│   │           │   │   ├── CourseAggregate.java                  // 课程聚合
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── ExamAggregate.java                    // 考试聚合
│   │           │   │   ├── ScoreAggregate.java                   // 成绩聚合
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── vos
│   │           │   ├── course
│   │           │   │   ├── CourseId.java                         // 课程 ID 值对象
│   │           │   │   ├── CourseCode.java                       // 课程编码值对象
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── ExamId.java                           // 考试 ID 值对象
│   │           │   │   ├── ScoreValue.java                       // 分数值对象
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── service
│   │           │   ├── course
│   │           │   │   ├── CourseDomainService.java              // 课程领域服务接口
│   │           │   │   ├── impl
│   │           │   │   │   ├── CourseDomainServiceImpl.java      // 课程领域服务实现
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── ExamDomainService.java                // 考试领域服务接口
│   │           │   │   ├── ScoreDomainService.java               // 成绩领域服务接口
│   │           │   │   ├── impl
│   │           │   │   │   ├── ExamDomainServiceImpl.java        // 考试领域服务实现
│   │           │   │   │   ├── ScoreDomainServiceImpl.java       // 成绩领域服务实现
│   │           │   │   │   └── package-info.java
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── repos
│   │           │   ├── course
│   │           │   │   ├── CourseRepository.java                 // 课程仓储接口
│   │           │   │   ├── CourseScheduleRepository.java         // 课程安排仓储接口
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── ExamRepository.java                   // 考试仓储接口
│   │           │   │   ├── ScoreRepository.java                  // 成绩仓储接口
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── validators
│   │           │   ├── course
│   │           │   │   ├── CourseDomainValidator.java            // 课程领域校验器
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── ExamDomainValidator.java              // 考试领域校验器
│   │           │   │   ├── ScoreDomainValidator.java             // 成绩领域校验器
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           ├── enums
│   │           │   ├── course
│   │           │   │   ├── CourseStatus.java                     // 课程状态枚举
│   │           │   │   └── package-info.java
│   │           │   ├── examing
│   │           │   │   ├── ExamStatus.java                       // 考试状态枚举
│   │           │   │   ├── ScoreStatus.java                      // 成绩状态枚举
│   │           │   │   └── package-info.java
│   │           │   └── package-info.java
│   │           └── package-info.java
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/domain
│       │       ├── service
│       │       │   ├── course
│       │       │   │   ├── CourseDomainServiceTest.java          // 课程领域服务测试
│       │       │   │   └── package-info.java
│       │       │   ├── examing
│       │       │   │   ├── ExamDomainServiceTest.java            // 考试领域服务测试
│       │       │   │   ├── ScoreDomainServiceTest.java           // 成绩领域服务测试
│       │       │   │   └── package-info.java
│       │       │   └── package-info.java
│       │       └── package-info.java
│       └── resources
│           └── application-test.yml
```

### 4.3.5 application / infrastructure / adapter 命名规则

`student-management-evaluation` 后续层级与 organization 保持一致，只是领域名替换为：

```text
course
examing
```

必须使用以下方向：

```text
application/manage/course/impl/CourseManageImpl.java
application/manage/examing/impl/ExamManageImpl.java

infrastructure/repo/course/impl/CourseRepositoryImpl.java
infrastructure/repo/examing/impl/ExamRepositoryImpl.java

adapter/facade/impl/course/CourseFacadeImpl.java
adapter/facade/impl/examing/ExamFacadeImpl.java
```

不能使用：

```text
application/manage/impl/course/CourseManageImpl.java
infrastructure/repo/impl/course/CourseRepositoryImpl.java
adapter/facade/course/impl/CourseFacadeImpl.java
```

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
2. RPC 实现只能放在 adapter/facade/impl。
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
7. facade 只定义 RPC 契约。
8. facade.impl 只能放 adapter。
9. MQ 入站放 adapter.mq。
10. MQ 出站放 infrastructure.mq。
```

两个 Project 的边界是：

```text
student-management-organization
    - user
    - teaching

student-management-evaluation
    - course
    - examing
```

每个 Project 内部保持完整分层：

```text
starter
common
facade
domain
application
infrastructure
adapter
```

最终调用链路：

```text
RPC -> Adapter -> Application -> Domain -> Repository Interface -> Infrastructure
MQ  -> Adapter -> Application -> Domain -> Repository Interface -> Infrastructure
```

最终依赖方向：

```text
starter -> adapter / infrastructure
adapter -> application / facade
application -> domain
domain -> common
infrastructure -> application
```

一句话总结：

```text
这是一个没有 Web 入口的后端 Service 架构，入口只走 RPC / MQ，业务只进 Application，规则只沉 Domain，技术实现只放 Infrastructure。
```
