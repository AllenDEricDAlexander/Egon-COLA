# 大型单体轻量领域分层架构 Code Style

> **规范效力说明**
>
> 本文描述的是 `egon-cola-archetype-light`、`egon-cola-archetype-web`、`egon-cola-archetype-service`
> 三个骨架**实际落地并强制执行**的架构，不是一份可选的建议清单。
>
> 其中的分层依赖约束由 `egon-cola-component-bytecode-architecture-maven-plugin` 自动校验：
> 该插件绑定在 Maven 的 `verify` 阶段，并以 `unknownLayerPolicy=FAIL` 运行，任何越界依赖和未登记的包都会直接导致构建失败。
>
> 当本文与骨架代码不一致时，**以骨架为准**，并回头修正本文。

## 1. 文档目的

本文用于规范大型单体项目中的分层依赖、包结构、命名风格和代码放置边界。

该架构不是完整重型 DDD，也不是传统三层架构，而是一种适合企业级大型单体项目的轻量领域分层架构。

核心目标：

```text
1. 分层清晰，避免业务逻辑散落。
2. 依赖单向，避免循环依赖。
3. 技术细节隔离在 infrastructure。
4. 入站入口统一收敛到 adapter。
5. 用例编排统一收敛到 application。
6. 核心业务规则和全部端口接口沉淀到 domain。
7. 公共基础能力沉淀到 common。
```

---

## 2. 依赖关系

## 2.1 模块依赖

骨架强制的依赖关系如下（这是**唯一**允许的内部依赖边集合）：

```text
starter        -> adapter, infrastructure

adapter        -> application, facade

application    -> domain

domain         -> common

infrastructure -> domain

facade         -> none

common         -> none
```

说明：

```text
1. starter 负责启动装配，只依赖 adapter 和 infrastructure。
2. adapter 负责入站适配，可以依赖 application 和 facade。
3. application 负责业务编排，只依赖 domain。
4. domain 负责核心业务规则，并持有全部端口接口，只依赖 common。
5. infrastructure 负责技术实现，只依赖 domain，实现 domain 中定义的仓储 / 领域服务 / client 端口接口。
6. facade 是对外契约包，自身维护 dto、enums、exceptions、utils，不依赖 common。
7. common 是最底层公共基础包，不依赖其他业务模块。
```

命名提示：

```text
1. web / service 骨架是多模块工程，启动装配模块的包名是 starter。
2. light 骨架是单模块工程，启动装配包名是 start，其余包名与本文完全一致。
3. 架构插件的 packageMappings 中，start 与 starter 都映射到 STARTER 层。
```

## 2.2 禁止依赖

```text
domain         -x-> application
domain         -x-> infrastructure
domain         -x-> adapter
domain         -x-> facade

application    -x-> infrastructure
application    -x-> adapter
application    -x-> facade.impl

infrastructure -x-> application
infrastructure -x-> adapter
infrastructure -x-> facade

facade         -x-> application
facade         -x-> domain
facade         -x-> infrastructure
facade         -x-> adapter
facade         -x-> common

adapter        -x-> infrastructure

starter        -x-> domain
starter        -x-> application
```

## 2.3 调用方向

```text
HTTP / RPC / MQ / GraphQL
        |
        v
adapter
        |
        v
application
        |
        v
domain
        |
        v
common
```

基础设施调用方向：

```text
infrastructure
        |
        v
domain 端口接口
（domain.<business>.repos / domain.<business>.service / domain.client.<external>）
```

仓储实现方向：

```text
application
        |
        v
domain.<business>.repos.XxxRepository          // 仓储端口，只有接口
        |
        v
infrastructure.<business>.repo.impl.XxxRepositoryImpl   // 唯一实现位置，基于 JPA
```

外部依赖实现方向：

```text
application / domain
        |
        v
domain.client.<external>.XxxPort               // 出站端口，只有接口
        |
        v
infrastructure.client.<external>.XxxClient     // 唯一实现位置
```

---

## 3. 标准包结构

## 3.1 总体结构

```text
src
├── main
│   ├── java
│   │   └── com.xxx.project
│   │       ├── starter                  // light 骨架中命名为 start
│   │       ├── adapter
│   │       ├── facade
│   │       ├── application
│   │       ├── infrastructure
│   │       ├── domain
│   │       └── common
│   └── resources
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-test.yml
│       ├── application-prod.yml
│       ├── bootstrap.yml
│       ├── logback-spring.xml
│       ├── graphql                      // GraphQL schema
│       └── db/migration                 // Flyway 迁移脚本
└── test
    ├── java
    │   └── com.xxx.project
    │       ├── adapter
    │       ├── application
    │       ├── infrastructure
    │       └── domain
    └── resources
        ├── application-test.yml
        └── logback-test.xml
```

说明：

```text
1. light 骨架把上述七个包放在同一个 Maven 模块中。
2. web / service 骨架把每一层拆成独立 Maven 模块，模块内的包名与上表一致。
3. 不存在 resources/mapper、resources/mybatis 目录，持久化实现只使用 JPA。
```

### 包内排布顺序：领域优先

除少数跨领域的运行时公共关注点外，包结构一律遵循**领域段在前、技术职责段在后**：

```text
<layer>/<business>/<technical-responsibility>
```

正确：

```text
domain/user/entities
domain/user/repos
application/teaching/manage
infrastructure/user/repo/impl
adapter/user/facade/impl
```

错误（技术优先，禁止）：

```text
domain/entities/user
application/manage/teaching
infrastructure/repo/user
adapter/facade/impl/user
```

以下跨领域的运行时公共关注点保留在各自分层根目录，不下沉到领域包：

```text
adapter/handler            // 全局异常处理 / 响应包装
adapter/filter             // TraceId / 请求上下文
infrastructure/aop         // 基础设施切面
infrastructure/config      // 数据源 / Redis / MQ 等技术配置
infrastructure/client      // 出站外部依赖，按外部系统而不是本地领域分包
domain/client              // 出站端口，按外部系统分包
```

---

## 3.2 starter

### 职责

`starter` 只负责启动类和业务无关配置。light 骨架中该包命名为 `start`。

### 结构

```text
starter                                  // light 骨架为 start
├── ProjectApplication.java              // Spring Boot 启动类
├── config
│   ├── JacksonConfig.java               // JSON 序列化配置
│   ├── OpenApiConfig.java               // OpenAPI / Swagger 配置
│   ├── ActuatorConfig.java              // 监控配置
│   ├── async
│   │   ├── AsyncConfiguration.java      // 线程池 / 异步执行配置
│   │   └── package-info.java
│   ├── encryption
│   │   ├── ConfigDecryptor.java         // 配置解密扩展
│   │   └── package-info.java
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 放启动类。
2. 放业务无关配置。
3. 放全局扫描配置。
4. 放通用序列化、监控、文档配置。
```

### 不能做

```text
1. 不写 Controller。
2. 不写 Application Service。
3. 不写 Domain Service。
4. 不写 Repository。
5. 不写 MQ Consumer / Producer。
6. 不写业务规则。
```

---

## 3.3 adapter

### 职责

`adapter` 是入站适配层，负责接收外部请求并转发给 application。

### 结构

```text
adapter
├── user                                     // 领域段在前
│   ├── controller
│   │   ├── UserController.java              // HTTP 入站入口
│   │   ├── RoleController.java              // HTTP 入站入口
│   │   ├── PermissionController.java        // HTTP 入站入口
│   │   └── package-info.java
│   ├── mq
│   │   ├── UserImportedConsumer.java        // MQ 入站消费者
│   │   └── package-info.java
│   ├── rpc
│   │   ├── UserRpcProvider.java             // RPC 入站 Provider
│   │   ├── PermissionRpcProvider.java       // RPC 入站 Provider
│   │   └── package-info.java
│   ├── graphql
│   │   ├── UserResolver.java                // GraphQL 入站 Resolver
│   │   └── package-info.java
│   ├── facade
│   │   ├── impl
│   │   │   ├── UserFacadeImpl.java          // Facade 实现，只能放在 adapter
│   │   │   ├── PermissionFacadeImpl.java    // Facade 实现，只能放在 adapter
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── dto
│   │   ├── CreateUserRequest.java           // adapter 入站请求 DTO
│   │   ├── AssignRoleRequest.java           // adapter 入站请求 DTO
│   │   ├── GrantPermissionRequest.java      // adapter 入站请求 DTO
│   │   └── package-info.java
│   ├── vo
│   │   ├── UserDetailVO.java                // HTTP 响应 VO
│   │   ├── PermissionTreeVO.java            // HTTP 响应 VO
│   │   └── package-info.java
│   ├── convertor
│   │   ├── UserAdapterConvertor.java        // adapter 对象转换
│   │   └── package-info.java
│   ├── validators
│   │   ├── UserRequestValidator.java        // 入站请求格式校验
│   │   └── package-info.java
│   └── package-info.java
├── teaching                                 // 另一个领域，结构同上
│   ├── controller
│   │   ├── SchoolClassController.java       // HTTP 入站入口
│   │   ├── CourseController.java            // HTTP 入站入口
│   │   └── package-info.java
│   ├── mq
│   │   ├── CourseImportedConsumer.java      // MQ 入站消费者
│   │   └── package-info.java
│   ├── rpc
│   │   ├── SchoolClassRpcProvider.java      // RPC 入站 Provider
│   │   ├── CourseRpcProvider.java           // RPC 入站 Provider
│   │   └── package-info.java
│   ├── graphql
│   │   ├── CourseResolver.java              // GraphQL 入站 Resolver
│   │   └── package-info.java
│   ├── facade
│   │   ├── impl
│   │   │   ├── SchoolClassFacadeImpl.java   // Facade 实现，只能放在 adapter
│   │   │   ├── CourseFacadeImpl.java        // Facade 实现，只能放在 adapter
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── dto
│   │   ├── CreateSchoolClassRequest.java    // adapter 入站请求 DTO
│   │   ├── CreateCourseRequest.java         // adapter 入站请求 DTO
│   │   ├── ScheduleCourseRequest.java       // adapter 入站请求 DTO
│   │   └── package-info.java
│   ├── vo
│   │   ├── SchoolClassDetailVO.java         // HTTP 响应 VO
│   │   ├── CourseDetailVO.java              // HTTP 响应 VO
│   │   └── package-info.java
│   ├── convertor
│   │   ├── TeachingAdapterConvertor.java    // adapter 对象转换
│   │   └── package-info.java
│   ├── validators
│   │   ├── TeachingRequestValidator.java    // 入站请求格式校验
│   │   └── package-info.java
│   └── package-info.java
├── handler                                  // 跨领域运行时关注点，留在分层根目录
│   ├── ApiResponse.java                     // 统一响应结构
│   ├── GlobalExceptionHandler.java          // Web 全局异常处理
│   ├── GraphQlExceptionResolver.java        // GraphQL 异常处理
│   ├── RabbitConsumerErrorHandler.java      // MQ 消费异常处理
│   ├── ResponseWrapperHandler.java          // 响应包装处理
│   └── package-info.java
├── filter                                   // 跨领域运行时关注点，留在分层根目录
│   ├── TraceIdFilter.java                   // Web TraceId 过滤器
│   ├── RequestContextFilter.java            // 请求上下文过滤器
│   ├── RequestContextHolder.java            // 请求上下文持有者
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 接收 HTTP 请求。
2. 消费入站 MQ。
3. 暴露 RPC Provider。
4. 暴露 GraphQL Resolver。
5. 实现 facade 接口。
6. 做请求 DTO 与 application 入参转换。
7. 做 application 出参与 VO / facade DTO 转换。
8. 做入站参数格式校验。
9. 做 Web 异常处理和请求过滤。
```

### 不能做

```text
1. 不直接调用 jpa repository。
2. 不直接操作 RedisTemplate。
3. 不直接发送 MQ。
4. 不直接调用 infrastructure。
5. 不直接调用 repository impl。
6. 不写核心业务规则。
```

### 调用规范

```text
Controller      -> Application Manage
MQ Consumer     -> Application Manage
RPC Provider    -> Application Manage
GraphQL Resolver -> Application Manage
FacadeImpl      -> Application Manage
```

---

## 3.4 facade

### 职责

`facade` 是对外契约包，只定义对外接口、DTO、枚举、异常和少量契约工具。

`facade` 不依赖 common，避免外部系统引入多余依赖。

`facade` 同样是领域优先排布：Facade 接口直接放在 `facade.<business>` 下，**不存在 `facade.api` 包**。

### 结构

```text
facade
├── user                                 // 领域段在前
│   ├── UserFacade.java                  // 用户对外 Facade 接口
│   ├── PermissionFacade.java            // 权限对外 Facade 接口
│   ├── dto
│   │   ├── CreateUserDTO.java           // 对外入参 DTO
│   │   ├── AssignRoleDTO.java           // 对外入参 DTO
│   │   ├── GrantPermissionDTO.java      // 对外入参 DTO
│   │   ├── UserDetailDTO.java           // 对外出参 DTO
│   │   ├── PermissionDTO.java           // 对外出参 DTO
│   │   ├── PermissionDetailDTO.java     // 对外出参 DTO
│   │   └── package-info.java
│   ├── enums
│   │   ├── UserFacadeStatus.java        // Facade 状态枚举
│   │   └── package-info.java
│   ├── exceptions
│   │   ├── UserFacadeException.java     // Facade 契约异常
│   │   └── package-info.java
│   ├── utils
│   │   ├── UserFacadeAssert.java        // Facade 断言工具
│   │   └── package-info.java
│   └── package-info.java
├── teaching                             // 另一个领域，结构同上
│   ├── SchoolClassFacade.java           // 班级对外 Facade 接口
│   ├── CourseFacade.java                // 课程对外 Facade 接口
│   ├── dto
│   │   ├── CreateSchoolClassDTO.java    // 对外入参 DTO
│   │   ├── CreateCourseDTO.java         // 对外入参 DTO
│   │   ├── ScheduleCourseDTO.java       // 对外入参 DTO
│   │   ├── SchoolClassDetailDTO.java    // 对外出参 DTO
│   │   ├── CourseDTO.java               // 对外出参 DTO
│   │   └── package-info.java
│   ├── enums
│   │   ├── CourseFacadeStatus.java      // Facade 状态枚举
│   │   └── package-info.java
│   ├── exceptions
│   │   ├── TeachingFacadeException.java // Facade 契约异常
│   │   └── package-info.java
│   ├── utils
│   │   ├── TeachingFacadeAssert.java    // Facade 断言工具
│   │   └── package-info.java
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 定义对外接口。
2. 定义对外 DTO。
3. 定义对外枚举。
4. 定义对外异常。
5. 定义轻量契约工具。
```

### 不能做

```text
1. 不写接口实现类。
2. 不依赖 application。
3. 不依赖 domain。
4. 不依赖 infrastructure。
5. 不依赖 adapter。
6. 不依赖 common。
7. 不写业务逻辑。
8. 不写数据库、缓存、MQ 逻辑。
```

---

## 3.5 application

### 职责

`application` 是应用编排层，负责组织完整业务用例。

### 结构

```text
application
├── user                                     // 领域段在前
│   ├── manage
│   │   ├── UserManage.java                  // 用户用例接口
│   │   ├── RoleManage.java                  // 角色用例接口
│   │   ├── PermissionManage.java            // 权限用例接口
│   │   ├── UserUseCaseException.java        // 用例异常
│   │   ├── impl
│   │   │   ├── UserManageImpl.java          // 用户用例实现
│   │   │   ├── RoleManageImpl.java          // 角色用例实现
│   │   │   ├── PermissionManageImpl.java    // 权限用例实现
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── command
│   │   ├── CreateUserCommand.java           // 写用例入参
│   │   ├── AssignRoleCommand.java           // 写用例入参
│   │   ├── GrantPermissionCommand.java      // 写用例入参
│   │   └── package-info.java
│   ├── query
│   │   ├── GetUserQuery.java                // 读用例入参
│   │   ├── GetUserPermissionsQuery.java     // 读用例入参
│   │   └── package-info.java
│   ├── result
│   │   ├── UserResult.java                  // 用例出参
│   │   ├── PermissionResult.java            // 用例出参
│   │   ├── PermissionDetailResult.java      // 用例出参
│   │   └── package-info.java
│   ├── assemblers
│   │   ├── UserAssembler.java               // 应用层对象装配
│   │   └── package-info.java
│   ├── convertor
│   │   ├── UserApplicationConvertor.java    // 应用层对象转换
│   │   └── package-info.java
│   ├── validators
│   │   ├── UserApplicationValidator.java    // 应用层校验
│   │   └── package-info.java
│   └── package-info.java
├── teaching                                 // 另一个领域，结构同上
│   ├── manage
│   │   ├── SchoolClassManage.java           // 班级用例接口
│   │   ├── CourseManage.java                // 课程用例接口
│   │   ├── TeachingUseCaseException.java    // 用例异常
│   │   ├── impl
│   │   │   ├── SchoolClassManageImpl.java   // 班级用例实现
│   │   │   ├── CourseManageImpl.java        // 课程用例实现
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── command
│   │   ├── CreateSchoolClassCommand.java    // 写用例入参
│   │   ├── CreateCourseCommand.java         // 写用例入参
│   │   ├── ScheduleCourseCommand.java       // 写用例入参
│   │   └── package-info.java
│   ├── query
│   │   ├── GetSchoolClassQuery.java         // 读用例入参
│   │   ├── GetCourseQuery.java              // 读用例入参
│   │   └── package-info.java
│   ├── result
│   │   ├── SchoolClassResult.java           // 用例出参
│   │   ├── CourseResult.java                // 用例出参
│   │   └── package-info.java
│   ├── assemblers
│   │   ├── TeachingAssembler.java           // 应用层对象装配
│   │   └── package-info.java
│   ├── convertor
│   │   ├── TeachingApplicationConvertor.java// 应用层对象转换
│   │   └── package-info.java
│   ├── validators
│   │   ├── TeachingApplicationValidator.java// 应用层校验
│   │   └── package-info.java
│   └── package-info.java
└── package-info.java
```

注意：`application` **不持有任何端口接口**。外部能力接口一律定义在 `domain.client.<external>`，
仓储接口定义在 `domain.<business>.repos`，不存在 `application.client` 包。

### 能做

```text
1. 编排业务流程。
2. 控制事务边界。
3. 调用 domain service 接口。
4. 调用 domain repository 接口。
5. 调用 domain.client 出站端口接口。
6. 做应用级参数校验。
7. 做权限、幂等、流程前置校验。
8. 聚合多个领域完成业务用例。
```

### 不能做

```text
1. 不定义端口接口，端口一律定义在 domain。
2. 不直接调用 jpa repository。
3. 不直接操作 RedisTemplate。
4. 不直接使用 KafkaTemplate / RabbitTemplate。
5. 不直接调用外部 HTTP / RPC 实现。
6. 不写 Web 层逻辑。
7. 不实现 facade 接口。
8. 不依赖 infrastructure 与 adapter。
```

---

## 3.6 infrastructure

### 职责

`infrastructure` 是基础设施层，负责数据库、缓存、MQ 出站、外部调用、第三方 SDK 等技术实现。

`infrastructure` **只依赖 `domain`**，其存在的全部意义就是实现 `domain` 中定义的端口接口。

持久化实现**只使用 JPA**。骨架集成测试断言 `pom.xml` 中不包含 `mybatis-plus` / `mybatis-spring`，
并断言 `OrganizationMybatisPlusConfig.java`、`src/main/resources/mapper`、`src/main/resources/mybatis` 均不存在。

### 结构

```text
infrastructure
├── user                                         // 领域段在前
│   ├── repo
│   │   ├── impl
│   │   │   ├── UserRepositoryImpl.java          // 用户仓储实现，唯一实现位置
│   │   │   ├── RoleRepositoryImpl.java          // 角色仓储实现
│   │   │   ├── PermissionRepositoryImpl.java    // 权限仓储实现
│   │   │   └── package-info.java
│   │   ├── po
│   │   │   ├── UserPO.java                      // 用户持久化对象
│   │   │   ├── RolePO.java                      // 角色持久化对象
│   │   │   ├── PermissionPO.java                // 权限持久化对象
│   │   │   ├── UserRolePO.java                  // 用户角色关系持久化对象
│   │   │   ├── RolePermissionPO.java            // 角色权限关系持久化对象
│   │   │   └── package-info.java
│   │   ├── jpa
│   │   │   ├── UserJpaRepository.java           // JPA Repository
│   │   │   ├── RoleJpaRepository.java           // JPA Repository
│   │   │   ├── PermissionJpaRepository.java     // JPA Repository
│   │   │   ├── UserRoleJpaRepository.java       // JPA Repository
│   │   │   ├── RolePermissionJpaRepository.java // JPA Repository
│   │   │   └── package-info.java
│   │   ├── converter
│   │   │   ├── UserPOConverter.java             // PO 与 Domain 转换
│   │   │   ├── RolePOConverter.java             // PO 与 Domain 转换
│   │   │   ├── PermissionPOConverter.java       // PO 与 Domain 转换
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── service
│   │   ├── impl
│   │   │   ├── UserDomainServiceImpl.java       // domain 领域服务端口实现
│   │   │   ├── RoleDomainServiceImpl.java       // domain 领域服务端口实现
│   │   │   ├── PermissionDomainServiceImpl.java // domain 领域服务端口实现
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── cache
│   │   ├── RedisUserCacheService.java           // domain 缓存端口实现
│   │   ├── InMemoryUserCacheService.java        // 本地回退实现
│   │   └── package-info.java
│   ├── mq
│   │   ├── RabbitUserEventPublisher.java        // domain 事件发布端口实现，仅出站
│   │   ├── LocalUserEventPublisher.java         // 本地回退实现
│   │   └── package-info.java
│   ├── validators
│   │   ├── UserInfrastructureValidator.java     // 技术适配校验
│   │   └── package-info.java
│   └── package-info.java
├── teaching                                     // 另一个领域，结构同上
│   ├── repo
│   │   ├── impl
│   │   │   ├── SchoolClassRepositoryImpl.java   // 班级仓储实现
│   │   │   ├── CourseRepositoryImpl.java        // 课程仓储实现
│   │   │   └── package-info.java
│   │   ├── po
│   │   │   ├── SchoolClassPO.java               // 班级持久化对象
│   │   │   ├── CoursePO.java                    // 课程持久化对象
│   │   │   ├── ClassCourseSchedulePO.java       // 排课关系持久化对象
│   │   │   └── package-info.java
│   │   ├── jpa
│   │   │   ├── SchoolClassJpaRepository.java    // JPA Repository
│   │   │   ├── CourseJpaRepository.java         // JPA Repository
│   │   │   ├── ClassCourseScheduleJpaRepository.java // JPA Repository
│   │   │   └── package-info.java
│   │   ├── converter
│   │   │   ├── SchoolClassPOConverter.java      // PO 与 Domain 转换
│   │   │   ├── CoursePOConverter.java           // PO 与 Domain 转换
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── service
│   │   └── impl
│   │       ├── SchoolClassDomainServiceImpl.java// domain 领域服务端口实现
│   │       ├── CourseDomainServiceImpl.java     // domain 领域服务端口实现
│   │       └── package-info.java
│   ├── cache
│   │   ├── RedisCourseCacheService.java         // domain 缓存端口实现
│   │   ├── InMemoryCourseCacheService.java      // 本地回退实现
│   │   └── package-info.java
│   ├── mq
│   │   ├── RabbitTeachingEventPublisher.java    // domain 事件发布端口实现，仅出站
│   │   ├── LocalTeachingEventPublisher.java     // 本地回退实现
│   │   └── package-info.java
│   ├── validators
│   │   ├── TeachingInfrastructureValidator.java // 技术适配校验
│   │   └── package-info.java
│   └── package-info.java
├── client                                       // 出站外部依赖，按外部系统分包
│   ├── evaluation
│   │   ├── DubboEvaluationQueryClient.java      // domain.client.evaluation 端口实现
│   │   ├── LocalEvaluationQueryStub.java        // 本地回退实现
│   │   ├── EvaluationClientFailureMapper.java   // 外部失败语义映射
│   │   └── package-info.java
│   └── package-info.java
├── aop                                          // 跨领域运行时关注点，留在分层根目录
│   ├── InfrastructureLogAspect.java             // 基础设施日志切面
│   ├── RepositoryMonitorAspect.java             // 仓储监控切面
│   └── package-info.java
├── config                                       // 跨领域技术配置，留在分层根目录
│   ├── RedisConfig.java                         // Redis 配置
│   ├── RabbitMqConfig.java                      // MQ 配置
│   ├── ExternalClientConfig.java                // 外部客户端配置
│   ├── datasource
│   │   ├── PhysicalDataSourceFactory.java       // 数据源装配
│   │   ├── ShardingSphereDataSourceConfiguration.java // 分片数据源配置
│   │   └── package-info.java
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 实现 domain.<business>.repos 中的仓储接口。
2. 实现 domain.<business>.service 中的领域服务端口。
3. 实现 domain.client.<external> 中的出站端口。
4. 调用 JPA Repository。
5. 调用 RedisTemplate。
6. 发送出站 MQ。
7. 调用外部 HTTP / RPC / SDK。
8. 做 PO 与 Domain 之间的转换。
9. 做基础设施配置。
```

### 不能做

```text
1. 不依赖 application、adapter、facade。
2. 不处理入站 HTTP 请求。
3. 不消费入站 MQ。
4. 不暴露 FacadeImpl。
5. 不暴露 Controller。
6. 不写核心业务规则。
7. 不引入 MyBatis-Plus / MyBatis，持久化只允许 JPA。
8. 不让 application 感知 jpa / redis / mq。
9. 不让 domain 感知基础设施实现。
```

---

## 3.7 domain

### 职责

`domain` 是领域核心层，负责实体、聚合、值对象、领域服务、领域校验、领域枚举，
并**持有全部端口接口**：仓储端口、领域服务端口、出站 client 端口。

### 结构

```text
domain
├── user                                      // 领域段在前
│   ├── entities
│   │   ├── User.java                         // 用户实体
│   │   ├── Role.java                         // 角色实体
│   │   ├── Permission.java                   // 权限实体
│   │   └── package-info.java
│   ├── aggregates
│   │   ├── UserAggregate.java                // 用户聚合
│   │   ├── RolePermissionAggregate.java      // 角色权限聚合
│   │   └── package-info.java
│   ├── vos
│   │   ├── UserId.java                       // 用户 ID 值对象
│   │   ├── RoleCode.java                     // 角色编码值对象
│   │   ├── PermissionCode.java               // 权限编码值对象
│   │   ├── UserSnapshot.java                 // 用户快照值对象
│   │   └── package-info.java
│   ├── service
│   │   ├── UserDomainService.java            // 用户领域服务端口
│   │   ├── RoleDomainService.java            // 角色领域服务端口
│   │   ├── PermissionDomainService.java      // 权限领域服务端口
│   │   ├── UserCacheService.java             // 用户缓存端口
│   │   ├── UserEventPublisher.java           // 用户事件发布端口
│   │   ├── UserQueryService.java             // 用户查询端口
│   │   └── package-info.java
│   ├── repos
│   │   ├── UserRepository.java               // 用户仓储端口，只定义接口
│   │   ├── RoleRepository.java               // 角色仓储端口，只定义接口
│   │   ├── PermissionRepository.java         // 权限仓储端口，只定义接口
│   │   └── package-info.java
│   ├── validators
│   │   ├── UserDomainValidator.java          // 用户领域校验
│   │   └── package-info.java
│   ├── enums
│   │   ├── UserStatus.java                   // 用户状态
│   │   ├── RoleStatus.java                   // 角色状态
│   │   ├── PermissionStatus.java             // 权限状态
│   │   └── package-info.java
│   ├── exceptions
│   │   ├── UserDomainException.java          // 用户领域异常
│   │   └── package-info.java
│   └── package-info.java
├── teaching                                  // 另一个领域，结构同上
│   ├── entities
│   │   ├── SchoolClass.java                  // 班级实体
│   │   ├── Course.java                       // 课程实体
│   │   └── package-info.java
│   ├── aggregates
│   │   ├── SchoolClassAggregate.java         // 班级聚合
│   │   ├── CourseAggregate.java              // 课程聚合
│   │   └── package-info.java
│   ├── vos
│   │   ├── SchoolClassId.java                // 班级 ID 值对象
│   │   ├── CourseCode.java                   // 课程编码值对象
│   │   ├── CourseSchedule.java               // 排课值对象
│   │   ├── Semester.java                     // 学期值对象
│   │   └── package-info.java
│   ├── service
│   │   ├── SchoolClassDomainService.java     // 班级领域服务端口
│   │   ├── CourseDomainService.java          // 课程领域服务端口
│   │   ├── CourseCacheService.java           // 课程缓存端口
│   │   ├── TeachingEventPublisher.java       // 教学事件发布端口
│   │   ├── TeachingQueryService.java         // 教学查询端口
│   │   └── package-info.java
│   ├── repos
│   │   ├── SchoolClassRepository.java        // 班级仓储端口，只定义接口
│   │   ├── CourseRepository.java             // 课程仓储端口，只定义接口
│   │   └── package-info.java
│   ├── validators
│   │   ├── TeachingDomainValidator.java      // 教学领域校验
│   │   └── package-info.java
│   ├── enums
│   │   ├── SchoolClassStatus.java            // 班级状态
│   │   ├── CourseStatus.java                 // 课程状态
│   │   └── package-info.java
│   ├── exceptions
│   │   ├── TeachingDomainException.java      // 教学领域异常
│   │   └── package-info.java
│   └── package-info.java
├── client                                    // 出站端口，按外部系统分包
│   ├── evaluation
│   │   ├── EvaluationQueryPort.java          // 外部评测系统查询端口
│   │   ├── EvaluationCourse.java             // 外部系统契约值对象
│   │   ├── EvaluationScore.java              // 外部系统契约值对象
│   │   └── package-info.java
│   ├── ExternalDependencyException.java      // 外部依赖失败的领域语义异常
│   ├── ExternalDependencyFailure.java        // 外部依赖失败分类
│   └── package-info.java
└── package-info.java
```

领域服务实现位置：

```text
1. 纯业务规则实现，可以放在 domain.<business>.service.impl（web / service 骨架采用）。
2. 需要技术能力才能实现的端口，一律放在 infrastructure（light 骨架全部走这条路径）：
   domain.<business>.service.XxxDomainService  -> infrastructure.<business>.service.impl.XxxDomainServiceImpl
   domain.<business>.service.XxxCacheService   -> infrastructure.<business>.cache.RedisXxxCacheService
   domain.<business>.service.XxxEventPublisher -> infrastructure.<business>.mq.RabbitXxxEventPublisher
   domain.client.<external>.XxxPort            -> infrastructure.client.<external>.XxxClient
```

### 能做

```text
1. 定义领域实体。
2. 定义聚合。
3. 定义值对象。
4. 定义领域服务端口。
5. 定义领域仓储端口。
6. 定义出站 client 端口。
7. 定义领域校验器。
8. 定义领域枚举与领域异常。
9. 表达核心业务规则。
```

### 不能做

```text
1. 不依赖 Spring MVC。
2. 不依赖 JPA / MyBatis 等持久化框架。
3. 不依赖 Redis。
4. 不依赖 MQ。
5. 不依赖 HTTP / RPC 技术实现。
6. 不依赖 application。
7. 不依赖 infrastructure。
8. 不依赖 adapter。
9. 不依赖 facade。
10. 端口只写接口，不写技术实现。
```

---

## 3.8 common

### 职责

`common` 是通用基础层，只放与具体业务无关、稳定复用的基础能力。

### 结构

```text
common
├── constants
│   ├── CommonConstants.java              // 通用常量
│   └── package-info.java
├── utils
│   ├── DateUtils.java                    // 日期工具
│   ├── StringUtils.java                  // 字符串工具
│   ├── TraceIdUtils.java                 // TraceId 工具
│   └── package-info.java
├── enums
│   ├── DeletedEnum.java                  // 通用删除状态
│   ├── EnabledEnum.java                  // 通用启停状态
│   └── package-info.java
├── exceptions
│   ├── BizException.java                 // 业务异常基类
│   ├── ErrorCode.java                    // 错误码接口
│   ├── CommonErrorCode.java              // 通用错误码
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 放通用常量。
2. 放通用工具。
3. 放基础异常。
4. 放基础错误码。
5. 放跨领域通用枚举。
```

### 不能做

```text
1. 不放具体业务枚举。
2. 不放具体业务异常。
3. 不放业务规则工具类。
4. 不放业务 Redis Key。
5. 不放数据库表名常量。
```

---

## 4. Code Style 规范

## 4.1 包命名规范

```text
1. 包名全部小写。
2. 包名使用单数或领域名称，不使用缩写。
3. 领域段在前，技术职责段在后：<layer>.<business>.<responsibility>。
4. impl 固定放在接口所在领域包的下一级。
5. 每一层的业务代码都必须按领域分包。
6. adapter.<business>.facade.impl 是 facade 实现唯一位置。
7. 跨领域运行时关注点留在分层根目录：adapter.handler、adapter.filter、infrastructure.aop、infrastructure.config。
8. 出站相关的包按外部系统而不是本地领域分包：domain.client.<external>、infrastructure.client.<external>。
```

推荐：

```text
domain.user.entities
domain.user.repos
domain.user.service
domain.client.evaluation

application.user.manage.impl
application.teaching.manage.impl

infrastructure.user.repo.impl
infrastructure.teaching.repo.impl
infrastructure.client.evaluation

adapter.user.facade.impl
facade.user
```

不推荐：

```text
application.manage.user.impl
application.manage.impl.user
application.client
application.facade.impl
domain.entities.user
domain.domainservices
domain.domainservicesimpl
infrastructure.repo.user.impl
infrastructure.repo.impl.user
infrastructure.client.impl
facade.api
```

## 4.2 类命名规范

```text
Controller      -> XxxController
MQ Consumer     -> XxxConsumer
RPC Provider    -> XxxRpcProvider
GraphQL Resolver -> XxxResolver
Facade          -> XxxFacade
Facade Impl     -> XxxFacadeImpl

Application Use Case Interface -> XxxManage
Application Use Case Impl      -> XxxManageImpl

Domain Service Interface -> XxxDomainService
Domain Service Impl      -> XxxDomainServiceImpl

Repository Interface -> XxxRepository
Repository Impl      -> XxxRepositoryImpl

Outbound Port      -> XxxPort / XxxQueryService / XxxCacheService / XxxEventPublisher
Outbound Port Impl -> XxxClient / RedisXxx / RabbitXxx / LocalXxx

Persistent Object -> XxxPO
JPA Repository    -> XxxJpaRepository
PO Converter      -> XxxPOConverter

Converter -> XxxConverter / XxxConvertor
Assembler -> XxxAssembler
Validator -> XxxValidator
```

说明：

```text
如果项目历史上已经统一使用 Convertor，则全项目继续统一使用 Convertor。
如果新项目没有历史包袱，推荐统一使用 Converter。
不要在同一项目中混用 Convertor 和 Converter。
```

## 4.3 注释规范

目录树中的注释推荐使用：

```text
// 模块职责
// 类职责
// 技术边界
```

示例：

```text
UserRepositoryImpl.java        // 用户仓储实现
UserPO.java                    // 用户持久化对象
UserDomainService.java         // 用户领域服务
UserManage.java                // 用户应用用例接口
UserFacadeImpl.java            // 用户 Facade 实现，只能放在 adapter
```

---

## 5. 开发约束

## 5.1 adapter 约束

```text
1. adapter 只能调用 application。
2. adapter 可以依赖 facade。
3. adapter 不允许调用 infrastructure。
4. adapter 不允许直接调用 jpa repository / redis / mq producer。
5. adapter.<business>.facade.impl 是 facade 实现唯一位置。
```

## 5.2 application 约束

```text
1. application 负责编排业务流程。
2. application 不依赖 infrastructure，也不依赖 adapter。
3. application 不定义端口接口，端口一律定义在 domain。
4. application 不实现 facade。
5. application 不直接接触数据库、缓存、MQ、外部 HTTP / RPC 实现。
```

## 5.3 domain 约束

```text
1. domain 表达业务规则，并持有全部端口接口。
2. domain 不依赖 application、adapter、infrastructure、facade。
3. domain.<business>.repos 只定义接口，不写实现。
4. domain.<business>.service 定义领域服务端口，纯业务实现可放 service.impl。
5. domain.client.<external> 定义出站端口，不写实现。
```

## 5.4 infrastructure 约束

```text
1. infrastructure 负责技术实现，只依赖 domain。
2. infrastructure 的业务代码必须按领域分包：infrastructure.<business>.repo 等。
3. repo.impl 只调用同领域的 jpa repository 与 converter。
4. 持久化只允许 JPA，禁止引入 MyBatis-Plus / MyBatis。
5. infrastructure.<business>.mq 只负责出站消息。
6. infrastructure.client.<external> 是 domain.client.<external> 端口的唯一实现位置。
```

## 5.5 facade 约束

```text
1. facade 只定义契约。
2. facade 不依赖 common。
3. facade 不写实现。
4. facade 不写业务逻辑。
5. facade 自带 dto、enums、exceptions、utils。
```

---

## 6. Validator 规范

```text
adapter validator        -> 请求格式校验
application validator    -> 用例前置校验
domain validator         -> 领域不变量校验
infrastructure validator -> 技术适配校验
```

判断规则：

```text
1. 只和 HTTP / MQ / RPC 入参格式有关，放 adapter。
2. 和当前业务用例流程有关，放 application。
3. 无论入口是什么都必须成立的业务规则，放 domain。
4. 和外部系统、数据库、缓存、MQ 适配有关，放 infrastructure。
```

---

## 7. 总结

最终依赖方向（骨架强制）：

```text
starter        -> adapter, infrastructure
adapter        -> application, facade
application    -> domain
domain         -> common
infrastructure -> domain
facade         -> none
common         -> none
```

最终结构方向：

```text
adapter.user.facade.impl              // Facade 实现唯一位置
application.user.manage.impl          // 应用用例按领域分包，不含端口
domain.user.repos                     // 仓储端口
domain.user.service                   // 领域服务端口
domain.client.evaluation              // 出站端口，按外部系统分包
infrastructure.user.repo.impl         // JPA 仓储实现，按领域分包
infrastructure.client.evaluation      // 出站端口实现
facade.user                           // 独立契约包，不依赖 common，无 api 子包
```

一句话总结：

```text
入口在 adapter，流程在 application，规则和端口在 domain，技术实现在 infrastructure，契约在 facade，通用能力在 common，启动装配在 starter。
```
