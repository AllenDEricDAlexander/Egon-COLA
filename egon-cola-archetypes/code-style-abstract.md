# 大型单体轻量领域分层架构 Code Style

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
6. 核心业务规则沉淀到 domain。
7. 公共基础能力沉淀到 common。
```

---

## 2. 依赖关系

## 2.1 模块依赖

推荐依赖关系如下：

```text
starter        -> adapter, infrastructure

adapter        -> application, facade

application    -> domain

domain         -> common

infrastructure -> application

facade         -> none

common         -> none
```

说明：

```text
1. starter 负责启动装配，只依赖 adapter 和 infrastructure。
2. adapter 负责入站适配，可以依赖 application 和 facade。
3. application 负责业务编排，只依赖 domain。
4. domain 负责核心业务规则，只依赖 common。
5. infrastructure 负责技术实现，依赖 application 中定义的 client / port 等接口。
6. facade 是对外契约包，自身维护 dto、enums、exceptions、utils，不依赖 common。
7. common 是最底层公共基础包，不依赖其他业务模块。
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
application client / port interface
```

仓储实现方向：

```text
application
        |
        v
domain repository interface
        |
        v
infrastructure repository implementation
```

---

## 3. 标准包结构

## 3.1 总体结构

```text
src
├── main
│   ├── java
│   │   └── com.xxx.project
│   │       ├── starter
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
│       └── mapper
└── test
    ├── java
    │   └── com.xxx.project
    │       ├── adapter
    │       ├── application
    │       ├── infrastructure
    │       └── domain
    └── resources
        ├── application-test.yml
        ├── logback-test.xml
        └── sql
```

---

## 3.2 starter

### 职责

`starter` 只负责启动类和业务无关配置。

### 结构

```text
starter
├── ProjectApplication.java              // Spring Boot 启动类
├── config
│   ├── JacksonConfig.java               // JSON 序列化配置
│   ├── OpenApiConfig.java               // OpenAPI / Swagger 配置
│   ├── ActuatorConfig.java              // 监控配置
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
├── controller
│   ├── UserController.java              // HTTP 入站入口
│   ├── TeachingController.java          // HTTP 入站入口
│   └── package-info.java
├── mq
│   ├── UserCreatedConsumer.java         // MQ 入站消费者
│   ├── TeachingChangedConsumer.java     // MQ 入站消费者
│   └── package-info.java
├── rpc
│   ├── UserRpcProvider.java             // RPC 入站 Provider
│   ├── TeachingRpcProvider.java         // RPC 入站 Provider
│   └── package-info.java
├── graphql
│   ├── UserResolver.java                // GraphQL 入站 Resolver
│   ├── TeachingResolver.java            // GraphQL 入站 Resolver
│   └── package-info.java
├── facade
│   └── impl
│       ├── UserFacadeImpl.java          // Facade 实现，只能放在 adapter
│       ├── TeachingFacadeImpl.java      // Facade 实现，只能放在 adapter
│       └── package-info.java
├── dto
│   ├── CreateUserRequest.java           // adapter 入站请求 DTO
│   ├── UpdateUserRequest.java           // adapter 入站请求 DTO
│   ├── CreateTeachingRequest.java       // adapter 入站请求 DTO
│   └── package-info.java
├── vo
│   ├── UserDetailVO.java                // HTTP 响应 VO
│   ├── TeachingDetailVO.java            // HTTP 响应 VO
│   └── package-info.java
├── convertor
│   ├── UserAdapterConvertor.java        // adapter 对象转换
│   ├── TeachingAdapterConvertor.java    // adapter 对象转换
│   └── package-info.java
├── handler
│   ├── GlobalExceptionHandler.java      // Web 全局异常处理
│   ├── ResponseWrapperHandler.java      // 响应包装处理
│   └── package-info.java
├── filter
│   ├── TraceIdFilter.java               // Web TraceId 过滤器
│   ├── RequestContextFilter.java        // 请求上下文过滤器
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
1. 不直接调用 mapper。
2. 不直接调用 mp service。
3. 不直接调用 jpa repository。
4. 不直接操作 RedisTemplate。
5. 不直接发送 MQ。
6. 不直接调用 infrastructure。
7. 不直接调用 repository impl。
8. 不写核心业务规则。
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

### 结构

```text
facade
├── api
│   ├── UserFacade.java                  // 用户对外 Facade
│   ├── TeachingFacade.java              // 教学对外 Facade
│   └── package-info.java
├── dto
│   ├── UserDTO.java                     // 对外用户 DTO
│   ├── UserQueryDTO.java                // 对外用户查询 DTO
│   ├── TeachingDTO.java                 // 对外教学 DTO
│   ├── TeachingQueryDTO.java            // 对外教学查询 DTO
│   └── package-info.java
├── enums
│   ├── FacadeResultCode.java            // Facade 返回码
│   ├── FacadeStatus.java                // Facade 状态枚举
│   └── package-info.java
├── exceptions
│   ├── FacadeException.java             // Facade 契约异常
│   └── package-info.java
├── utils
│   ├── FacadeAssert.java                // Facade 断言工具
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
├── manage
│   ├── user
│   │   ├── UserManage.java              // 用户用例接口
│   │   ├── RoleManage.java              // 角色用例接口
│   │   ├── PermissionManage.java        // 权限用例接口
│   │   ├── impl
│   │   │   ├── UserManageImpl.java      // 用户用例实现
│   │   │   ├── RoleManageImpl.java      // 角色用例实现
│   │   │   ├── PermissionManageImpl.java// 权限用例实现
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── teaching
│   │   ├── TeachingManage.java          // 教学用例接口
│   │   ├── SchoolClassManage.java       // 班级用例接口
│   │   ├── CourseManage.java            // 课程用例接口
│   │   ├── impl
│   │   │   ├── TeachingManageImpl.java  // 教学用例实现
│   │   │   ├── SchoolClassManageImpl.java// 班级用例实现
│   │   │   ├── CourseManageImpl.java    // 课程用例实现
│   │   │   └── package-info.java
│   │   └── package-info.java
│   └── package-info.java
├── client
│   ├── UserClient.java                  // 外部用户能力接口
│   ├── TeachingClient.java              // 外部教学能力接口
│   └── package-info.java
├── assemblers
│   ├── UserAssembler.java               // 应用层对象装配
│   ├── TeachingAssembler.java           // 应用层对象装配
│   └── package-info.java
├── convertor
│   ├── UserApplicationConvertor.java    // 应用层对象转换
│   ├── TeachingApplicationConvertor.java// 应用层对象转换
│   └── package-info.java
├── validators
│   ├── UserApplicationValidator.java    // 应用层校验
│   ├── TeachingApplicationValidator.java// 应用层校验
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 编排业务流程。
2. 控制事务边界。
3. 调用 domain service。
4. 调用 domain repository 接口。
5. 调用 application client 接口。
6. 做应用级参数校验。
7. 做权限、幂等、流程前置校验。
8. 聚合多个领域完成业务用例。
```

### 不能做

```text
1. 不直接调用 mapper。
2. 不直接调用 mp service。
3. 不直接调用 jpa repository。
4. 不直接操作 RedisTemplate。
5. 不直接使用 KafkaTemplate / RabbitTemplate。
6. 不直接调用外部 HTTP / RPC 实现。
7. 不写 Web 层逻辑。
8. 不实现 facade 接口。
```

---

## 3.6 infrastructure

### 职责

`infrastructure` 是基础设施层，负责数据库、缓存、MQ 出站、外部调用、第三方 SDK 等技术实现。

### 结构

```text
infrastructure
├── repo
│   ├── user
│   │   ├── impl
│   │   │   ├── UserRepositoryImpl.java          // 用户仓储实现
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
│   │   ├── mp
│   │   │   ├── mapper
│   │   │   │   ├── UserMapper.java              // MyBatis-Plus Mapper
│   │   │   │   ├── RoleMapper.java              // MyBatis-Plus Mapper
│   │   │   │   ├── PermissionMapper.java        // MyBatis-Plus Mapper
│   │   │   │   └── package-info.java
│   │   │   ├── service
│   │   │   │   ├── UserMpService.java           // MP Service 接口
│   │   │   │   ├── RoleMpService.java           // MP Service 接口
│   │   │   │   ├── PermissionMpService.java     // MP Service 接口
│   │   │   │   ├── impl
│   │   │   │   │   ├── UserMpServiceImpl.java   // MP Service 实现
│   │   │   │   │   ├── RoleMpServiceImpl.java   // MP Service 实现
│   │   │   │   │   ├── PermissionMpServiceImpl.java// MP Service 实现
│   │   │   │   │   └── package-info.java
│   │   │   │   └── package-info.java
│   │   │   └── package-info.java
│   │   ├── jpa
│   │   │   ├── UserJpaRepository.java           // JPA Repository
│   │   │   ├── RoleJpaRepository.java           // JPA Repository
│   │   │   ├── PermissionJpaRepository.java     // JPA Repository
│   │   │   └── package-info.java
│   │   ├── converter
│   │   │   ├── UserPOConverter.java             // PO 与 Domain 转换
│   │   │   ├── RolePOConverter.java             // PO 与 Domain 转换
│   │   │   ├── PermissionPOConverter.java       // PO 与 Domain 转换
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── teaching
│   │   ├── impl
│   │   │   ├── SchoolClassRepositoryImpl.java   // 班级仓储实现
│   │   │   ├── CourseRepositoryImpl.java        // 课程仓储实现
│   │   │   └── package-info.java
│   │   ├── po
│   │   │   ├── SchoolClassPO.java               // 班级持久化对象
│   │   │   ├── CoursePO.java                    // 课程持久化对象
│   │   │   └── package-info.java
│   │   ├── mp
│   │   │   ├── mapper
│   │   │   │   ├── SchoolClassMapper.java       // MyBatis-Plus Mapper
│   │   │   │   ├── CourseMapper.java            // MyBatis-Plus Mapper
│   │   │   │   └── package-info.java
│   │   │   ├── service
│   │   │   │   ├── SchoolClassMpService.java    // MP Service 接口
│   │   │   │   ├── CourseMpService.java         // MP Service 接口
│   │   │   │   ├── impl
│   │   │   │   │   ├── SchoolClassMpServiceImpl.java// MP Service 实现
│   │   │   │   │   ├── CourseMpServiceImpl.java // MP Service 实现
│   │   │   │   │   └── package-info.java
│   │   │   │   └── package-info.java
│   │   │   └── package-info.java
│   │   ├── jpa
│   │   │   ├── SchoolClassJpaRepository.java    // JPA Repository
│   │   │   ├── CourseJpaRepository.java         // JPA Repository
│   │   │   └── package-info.java
│   │   ├── converter
│   │   │   ├── SchoolClassPOConverter.java      // PO 与 Domain 转换
│   │   │   ├── CoursePOConverter.java           // PO 与 Domain 转换
│   │   │   └── package-info.java
│   │   └── package-info.java
│   └── package-info.java
├── client
│   └── impl
│       ├── UserClientImpl.java                  // application client 实现
│       ├── TeachingClientImpl.java              // application client 实现
│       └── package-info.java
├── mq
│   ├── UserChangedProducer.java                 // MQ 出站发送
│   ├── TeachingChangedProducer.java             // MQ 出站发送
│   └── package-info.java
├── cache
│   ├── UserCache.java                           // 缓存封装
│   ├── TeachingCache.java                       // 缓存封装
│   └── package-info.java
├── validators
│   ├── ExternalUserResponseValidator.java       // 外部响应校验
│   ├── ExternalTeachingResponseValidator.java   // 外部响应校验
│   └── package-info.java
├── aop
│   ├── InfrastructureLogAspect.java             // 基础设施日志切面
│   └── package-info.java
├── config
│   ├── MyBatisPlusConfig.java                   // MP 配置
│   ├── JpaConfig.java                           // JPA 配置
│   ├── RedisConfig.java                         // Redis 配置
│   ├── MqConfig.java                            // MQ 配置
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 实现 domain repository 接口。
2. 实现 application client 接口。
3. 调用 MyBatis-Plus Service。
4. 调用 MyBatis Mapper。
5. 调用 JPA Repository。
6. 调用 RedisTemplate。
7. 发送出站 MQ。
8. 调用外部 HTTP / RPC / SDK。
9. 做 PO 与 Domain 之间的转换。
10. 做基础设施配置。
```

### 不能做

```text
1. 不处理入站 HTTP 请求。
2. 不消费入站 MQ。
3. 不暴露 FacadeImpl。
4. 不暴露 Controller。
5. 不写核心业务规则。
6. 不让 application 感知 mapper / jpa / redis / mq。
7. 不让 domain 感知基础设施实现。
```

---

## 3.7 domain

### 职责

`domain` 是领域核心层，负责实体、聚合、值对象、领域服务、仓储接口、领域校验和领域枚举。

### 结构

```text
domain
├── entities
│   ├── User.java                         // 用户实体
│   ├── Role.java                         // 角色实体
│   ├── Permission.java                   // 权限实体
│   ├── SchoolClass.java                  // 班级实体
│   ├── Course.java                       // 课程实体
│   └── package-info.java
├── aggregates
│   ├── UserAggregate.java                // 用户聚合
│   ├── RolePermissionAggregate.java      // 角色权限聚合
│   ├── SchoolClassAggregate.java         // 班级聚合
│   ├── CourseAggregate.java              // 课程聚合
│   └── package-info.java
├── vos
│   ├── UserId.java                       // 用户 ID 值对象
│   ├── RoleCode.java                     // 角色编码值对象
│   ├── PermissionCode.java               // 权限编码值对象
│   ├── SchoolClassId.java                // 班级 ID 值对象
│   ├── CourseCode.java                   // 课程编码值对象
│   └── package-info.java
├── service
│   ├── UserDomainService.java            // 用户领域服务
│   ├── RoleDomainService.java            // 角色领域服务
│   ├── PermissionDomainService.java      // 权限领域服务
│   ├── SchoolClassDomainService.java     // 班级领域服务
│   ├── CourseDomainService.java          // 课程领域服务
│   ├── impl
│   │   ├── UserDomainServiceImpl.java    // 用户领域服务实现
│   │   ├── RoleDomainServiceImpl.java    // 角色领域服务实现
│   │   ├── PermissionDomainServiceImpl.java// 权限领域服务实现
│   │   ├── SchoolClassDomainServiceImpl.java// 班级领域服务实现
│   │   ├── CourseDomainServiceImpl.java  // 课程领域服务实现
│   │   └── package-info.java
│   └── package-info.java
├── repos
│   ├── UserRepository.java               // 用户仓储接口，只定义接口
│   ├── RoleRepository.java               // 角色仓储接口，只定义接口
│   ├── PermissionRepository.java         // 权限仓储接口，只定义接口
│   ├── SchoolClassRepository.java        // 班级仓储接口，只定义接口
│   ├── CourseRepository.java             // 课程仓储接口，只定义接口
│   └── package-info.java
├── validators
│   ├── UserDomainValidator.java          // 用户领域校验
│   ├── RoleDomainValidator.java          // 角色领域校验
│   ├── PermissionDomainValidator.java    // 权限领域校验
│   ├── SchoolClassDomainValidator.java   // 班级领域校验
│   ├── CourseDomainValidator.java        // 课程领域校验
│   └── package-info.java
├── enums
│   ├── UserStatus.java                   // 用户状态
│   ├── RoleStatus.java                   // 角色状态
│   ├── PermissionType.java               // 权限类型
│   ├── SchoolClassStatus.java            // 班级状态
│   ├── CourseStatus.java                 // 课程状态
│   └── package-info.java
└── package-info.java
```

### 能做

```text
1. 定义领域实体。
2. 定义聚合。
3. 定义值对象。
4. 定义领域服务。
5. 定义领域仓储接口。
6. 定义领域校验器。
7. 定义领域枚举。
8. 表达核心业务规则。
```

### 不能做

```text
1. 不依赖 Spring MVC。
2. 不依赖 MyBatis-Plus。
3. 不依赖 JPA。
4. 不依赖 Redis。
5. 不依赖 MQ。
6. 不依赖 HTTP / RPC 技术实现。
7. 不依赖 application。
8. 不依赖 infrastructure。
9. 不依赖 adapter。
10. 不依赖 facade。
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
3. impl 固定放在接口所在领域包的下一级。
4. infrastructure.repo 必须按领域分包。
5. application.manage 必须按领域分包。
6. adapter.facade.impl 是 facade 实现唯一位置。
```

推荐：

```text
application.manage.user.impl
application.manage.teaching.impl

infrastructure.repo.user.impl
infrastructure.repo.teaching.impl

domain.service.impl

adapter.facade.impl
```

不推荐：

```text
application.manage.impl.user
application.facade.impl
domain.domainservices
domain.domainservicesimpl
infrastructure.repo.impl.user
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

Persistent Object -> XxxPO
MyBatis Mapper    -> XxxMapper
MP Service        -> XxxMpService
MP Service Impl   -> XxxMpServiceImpl
JPA Repository    -> XxxJpaRepository

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
4. adapter 不允许直接调用 mapper / jpa repository / redis / mq producer。
5. adapter.facade.impl 是 facade 实现唯一位置。
```

## 5.2 application 约束

```text
1. application 负责编排业务流程。
2. application 不依赖 infrastructure。
3. application 不实现 facade。
4. application 不直接接触数据库、缓存、MQ、外部 HTTP / RPC 实现。
```

## 5.3 domain 约束

```text
1. domain 只表达业务规则。
2. domain 不依赖 application、adapter、infrastructure、facade。
3. domain 中的 repos 只定义接口，不写实现。
4. domain service 使用 service / service.impl 包结构。
```

## 5.4 infrastructure 约束

```text
1. infrastructure 负责技术实现。
2. infrastructure.repo 必须按领域分包。
3. repo.impl 调用 mp service 或 jpa repository。
4. 业务代码不允许直调 mapper。
5. infrastructure.mq 只负责出站消息。
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

推荐最终依赖方向：

```text
starter        -> adapter, infrastructure
adapter        -> application, facade
application    -> domain
domain         -> common
infrastructure -> application
facade         -> none
common         -> none
```

推荐最终结构方向：

```text
adapter.facade.impl             // Facade 实现唯一位置
application.manage.user.impl    // 应用用例按领域分包
domain.service.impl             // 领域服务实现位置
infrastructure.repo.user.impl   // 仓储实现按领域分包
facade                          // 独立契约包，不依赖 common
```

一句话总结：

```text
入口在 adapter，流程在 application，规则在 domain，技术在 infrastructure，契约在 facade，通用能力在 common，启动装配在 starter。
```
