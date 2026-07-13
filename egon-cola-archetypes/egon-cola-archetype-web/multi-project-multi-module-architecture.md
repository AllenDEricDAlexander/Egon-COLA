# 学生管理系统多工程多模块轻量领域分层架构说明文档

## 1. 综述

本文档描述学生管理系统在大型单体场景下的多工程、多模块、轻量领域分层架构。

这里的“多工程”不是指一个根工程下面聚合所有业务模块，而是指按照业务边界拆成两个独立 Project：

```text
student-management-organization  // 组织管理工程：user + teaching
student-management-evaluation    // 评价管理工程：course + exam
```

每个 Project 内部再按照统一分层拆成多个 Maven 子模块：

```text
starter          // 启动类和业务无关配置
adapter          // 入站适配层
facade           // 对外契约层
application      // 应用编排层
domain           // 领域核心层
infrastructure   // 基础设施层
common           // 工程内部通用基础层
```

两个 Project 的领域划分如下：

```text
student-management-organization
    - user        // 用户、角色、权限
    - teaching    // 班级、年级、教学组织

student-management-evaluation
    - course      // 课程、课程安排、课程资源
    - exam     // 考试、成绩、评价
```

该架构的目标不是做完整重型 DDD，而是在大型单体或准单体工程内建立清晰边界：

```text
1. Project 按业务边界拆分。
2. Project 内部按分层模块拆分。
3. 各层内部按领域包继续隔离。
4. adapter 只做入站适配。
5. application 只做应用编排。
6. domain 只沉淀核心业务规则。
7. infrastructure 只处理技术实现。
8. facade 独立承载对外契约。
```

一句话：

```text
两个独立工程，各自完整分层；每个工程内部再按领域包拆分。
```

---

## 2. 依赖关系

## 2.1 Project 级关系

本架构中不存在如下根聚合工程：

```text
student-management
├── student-management-organization
└── student-management-evaluation
```

正确结构是两个独立 Project：

```text
student-management-organization
student-management-evaluation
```

两个 Project 可以在同一个 Git 仓库中，也可以在不同 Git 仓库中，但工程语义上它们是两个独立 Project，不是同一个 Maven
聚合根下的两个普通子模块。

## 2.2 单个 Project 内部依赖关系

每个 Project 内部统一使用以下依赖关系。

```text
starter import adapter
starter import infrastructure

adapter import application
adapter import facade

application import domain

domain import common

infrastructure import domain
```

注意：

```text
1. facade 不依赖 common。
2. facade 有自己的 utils、enums、exceptions。
3. adapter/facade.impl 是 Facade 实现唯一位置。
4. application 不放 facade.impl。
5. infrastructure 只声明 infrastructure import domain，直接实现 domain 定义的仓储和出站端口。
```

## 2.3 依赖关系图

```text
                    starter
                       |
            -----------------------
            |                     |
         adapter             infrastructure
            |                     |
      -------------               |
      |           |               |
application   facade              |
      |                           |
    domain <-----------------------
      |
    common
```

## 2.4 禁止依赖

```text
1. domain 不依赖 application。
2. domain 不依赖 infrastructure。
3. domain 不依赖 adapter。
4. domain 不依赖 facade。
5. application 不依赖 infrastructure。
6. application 不依赖 adapter。
7. application 不依赖 facade。
8. facade 不依赖 application。
9. facade 不依赖 domain。
10. facade 不依赖 common。
11. infrastructure 不依赖 adapter。
12. starter 不写业务，只负责装配 adapter 和 infrastructure。
```

---

## 3. 模块说明

## 3.1 starter 模块

### 职责

`starter` 是当前 Project 的启动装配模块，只放启动类和业务无关配置。

### 结构

```text
starter
    - src/main/java
    - src/main/resources
    - src/test/java
    - src/test/resources
```

### 能做

```text
1. 放 Spring Boot 启动类。
2. 放 application.yml、bootstrap.yml、logback-spring.xml。
3. 放 Swagger、Actuator、Jackson、线程池等业务无关配置。
4. 装配 adapter 和 infrastructure。
```

### 不能做

```text
1. 不写 Controller。
2. 不写 MQ Consumer。
3. 不写 FacadeImpl。
4. 不写 Manage。
5. 不写 Domain Service。
6. 不写 Repository。
7. 不写 Mapper。
8. 不写业务逻辑。
```

---

## 3.2 adapter 模块

### 职责

`adapter` 是入站适配层，负责处理 HTTP、MQ 入站、RPC 入站、GraphQL 入站和 Facade 实现。

### 结构

```text
adapter
    - <business-domain>
        - controller  // Web Project 可用
        - mq          // 仅入站
        - rpc
        - converter
        - dto
        - vo          // Web Project 可用
        - graphql     // Web Project 可用
        - facade.impl // Facade 实现唯一位置
        - validators
    - handler         // 跨领域共享
    - filter          // Web Project 跨领域共享
```

### 能做

```text
1. 接收 HTTP 请求。
2. 消费入站 MQ 消息。
3. 暴露 RPC Provider。
4. 暴露 GraphQL Resolver。
5. 实现 facade 接口。
6. 做入站 DTO、VO、Facade DTO 转换。
7. 调用 application。
8. 处理全局异常、过滤器、请求上下文。
```

### 不能做

```text
1. 不直接调用 Mapper。
2. 不直接调用 MP Service。
3. 不直接调用 JPA Repository。
4. 不直接操作 RedisTemplate。
5. 不直接发送 MQ。
6. 不直接调用 RepositoryImpl。
7. 不直接写核心业务规则。
8. 不绕过 application 调 domain。
```

---

## 3.3 facade 模块

### 职责

`facade` 是对外契约层，只定义对外接口、DTO、枚举、异常、工具类。

### 结构

```text
facade
    - <business-domain>
        - Facade定义
        - dto
    - dto             // 仅保留跨领域响应包装
    - enums           // 跨领域共享
    - exceptions      // 跨领域共享
    - utils           // 跨领域共享
```

### 能做

```text
1. 定义 Facade 接口。
2. 定义对外 DTO。
3. 定义对外枚举。
4. 定义 Facade 专属异常。
5. 定义 Facade 专属工具类。
```

### 不能做

```text
1. 不写 Facade 实现。
2. 不依赖 common。
3. 不依赖 application。
4. 不依赖 domain。
5. 不依赖 infrastructure。
6. 不依赖 adapter。
7. 不写业务流程。
8. 不写数据库、缓存、MQ 逻辑。
```

---

## 3.4 application 模块

### 职责

`application` 是应用编排层，负责组织一个完整业务用例。

### 结构

```text
application
    - <business-domain>
        - command
        - query
        - result
        - converter
        - manage
            - impl
        - validators
        - assemblers
    - config          // 跨领域共享
    - context         // 跨领域共享
    - exceptions      // 跨领域共享
    - support         // 跨领域共享
```

### 能做

```text
1. 编排业务流程。
2. 控制事务边界。
3. 调用 domain service。
4. 调用 domain repository 接口。
5. 调用 domain client 接口。
6. 做应用级校验。
7. 做对象装配和转换。
8. 协调当前 Project 内多个领域。
```

### 不能做

```text
1. 不写 facade.impl。
2. 不直接调用 Mapper。
3. 不直接调用 MP Service。
4. 不直接调用 JPA Repository。
5. 不直接操作 RedisTemplate。
6. 不直接发送 MQ。
7. 不直接调用外部 RPC / HTTP 实现。
8. 不写入站协议逻辑。
```

---

## 3.5 domain 模块

### 职责

`domain` 是领域核心层，负责实体、聚合、值对象、领域服务、仓储接口、出站客户端接口、领域校验器和领域枚举。

### 结构

```text
domain
    - <business-domain>
        - entities
        - aggregates
        - vos
        - service
            - impl
        - repos
        - client
        - validators
        - enums
        - events
    - client
        - <external-project> // 外部 Facade ACL 的技术优先例外
    - exceptions             // 跨领域共享
```

### 能做

```text
1. 定义领域实体。
2. 定义聚合。
3. 定义值对象。
4. 定义领域服务接口和实现。
5. 定义仓储接口。
6. 定义出站客户端接口。
7. 定义领域校验器。
8. 定义领域枚举。
9. 表达核心业务规则。
```

### 不能做

```text
1. 不依赖 application。
2. 不依赖 adapter。
3. 不依赖 facade。
4. 不依赖 infrastructure。
5. 不依赖 MyBatis-Plus。
6. 不依赖 JPA。
7. 不依赖 Redis。
8. 不依赖 MQ。
9. 不依赖 HTTP / RPC 技术实现。
```

---

## 3.6 infrastructure 模块

### 职责

`infrastructure` 是基础设施层，负责数据库、缓存、MQ 出站、外部调用、技术配置、AOP 等实现。

### 结构

```text
infrastructure
    - <business-domain>
        - repo
            - impl
            - po
            - jpa
            - converter
        - cache
        - mq          // 仅出站，按需
    - client
        - <external-project> // 外部 Facade ACL 的技术优先例外
    - validators      // 跨领域共享
    - aop             // 跨领域共享
    - mq              // 跨领域共享出站支持
    - cache           // 跨领域共享缓存支持
    - config          // 跨领域共享
```

### 能做

```text
1. 实现 domain repository 接口。
2. 调用 MP Service。
3. 调用 Mapper。
4. 调用 JPA Repository。
5. 实现 domain client 接口。
6. 调用外部 Facade / HTTP / RPC / SDK。
7. 发送出站 MQ。
8. 封装缓存。
9. 提供基础设施配置。
```

### 不能做

```text
1. 不处理入站 HTTP。
2. 不消费入站 MQ。
3. 不暴露 FacadeImpl。
4. 不写核心业务规则。
5. 不让 application 直接感知 Mapper / Redis / MQ / JPA。
```

---

## 3.7 common 模块

### 职责

`common` 是当前 Project 内部通用基础层，提供稳定、通用、非业务强绑定的基础能力。

### 结构

```text
common
    - constants
    - utils
    - enums
    - exceptions
```

### 能做

```text
1. 放工程内部通用常量。
2. 放工程内部通用工具。
3. 放工程内部通用枚举。
4. 放工程内部基础异常。
5. 放 Result、PageRequest、PageResult 等通用对象。
```

### 不能做

```text
1. 不放具体领域状态枚举。
2. 不放具体业务规则。
3. 不放 Redis 业务 Key。
4. 不放数据库表名常量。
5. 不被 facade 依赖。
```

---

## 4. 结构示例 + 命名示例

## 4.1 工程结构

错误结构如下，不采用：

```text
student-management
├── student-management-organization
└── student-management-evaluation
```

正确结构是两个独立 Project：

```text
student-management-organization
student-management-evaluation
```

如果放在同一个 Git 仓库中，也只是物理目录并列，不代表 Maven 聚合父工程：

```text
workspace
├── student-management-organization              // 独立 Project，组织管理工程
│   ├── pom.xml
│   ├── student-management-organization-starter
│   ├── student-management-organization-common
│   ├── student-management-organization-facade
│   ├── student-management-organization-application
│   ├── student-management-organization-domain
│   ├── student-management-organization-infrastructure
│   └── student-management-organization-adapter
│
└── student-management-evaluation                // 独立 Project，评价管理工程
    ├── pom.xml
    ├── student-management-evaluation-starter
    ├── student-management-evaluation-common
    ├── student-management-evaluation-facade
    ├── student-management-evaluation-application
    ├── student-management-evaluation-domain
    ├── student-management-evaluation-infrastructure
    └── student-management-evaluation-adapter
```

---

## 4.2 student-management-organization 工程结构

`student-management-organization` 是独立 Project，包含两个领域：

```text
user        // 用户、角色、权限
teaching    // 班级、年级、教学组织
```

业务代码统一采用领域优先路径，下面各模块的类型清单均以此映射为准：

```text
facade/{user,teaching}/dto
domain/{user,teaching}/{aggregates,client,entities,enums,events,repos,service,validators,vos}
application/{user,teaching}/{assemblers,command,converter,manage,query,result,validators}
infrastructure/{user,teaching}/{repo,cache}
adapter/{user,teaching}/{controller,converter,dto,facade/impl,graphql,mq,rpc,vo}
```

`domain/client/evaluation` 与 `infrastructure/client/evaluation` 是 Organization 访问外部 Evaluation Facade 的 ACL，也是本工程唯一保留的技术优先外部客户端路径。跨领域的异常、配置、过滤器、消息支持等仍保留在各层共享根。

### 4.2.1 organization-starter

```text
student-management-organization-starter
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/student/organization/starter
│   │   │       ├── package-info.java
│   │   │       ├── OrganizationApplication.java                  // 组织管理工程启动类
│   │   │       └── config
│   │   │           ├── package-info.java
│   │   │           ├── OrganizationSwaggerConfig.java             // Swagger 配置
│   │   │           ├── OrganizationJacksonConfig.java             // Jackson 配置
│   │   │           └── OrganizationActuatorConfig.java            // Actuator 配置
│   │   └── resources
│   │       ├── application.yml                                    // 主配置
│   │       ├── application-dev.yml                                // 开发环境配置
│   │       ├── application-test.yml                               // 测试环境配置
│   │       ├── application-prod.yml                               // 生产环境配置
│   │       ├── bootstrap.yml                                      // 启动引导配置
│   │       └── logback-spring.xml                                 // 日志配置
│   └── test
│       ├── java
│       │   └── com/example/student/organization/starter
│       │       ├── package-info.java
│       │       └── OrganizationApplicationTest.java               // 启动测试
│       └── resources
│           ├── application-test.yml                               // 测试配置
│           └── logback-test.xml                                   // 测试日志配置
```

### 4.2.2 organization-common

```text
student-management-organization-common
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/common
│   │           ├── package-info.java
│   │           ├── constants
│   │           │   ├── package-info.java
│   │           │   └── OrganizationCommonConstants.java           // 组织工程通用常量
│   │           ├── utils
│   │           │   ├── package-info.java
│   │           │   ├── OrganizationDateUtils.java                 // 日期工具
│   │           │   └── OrganizationTraceUtils.java                // Trace 工具
│   │           ├── enums
│   │           │   ├── package-info.java
│   │           │   └── OrganizationOperationStatus.java           // 业务无关操作状态
│   │           └── exceptions
│   │               ├── package-info.java
│   │               ├── OrganizationBaseException.java             // 基础异常
│   │               └── OrganizationErrorCode.java                 // 基础错误码
│   └── test
│       ├── java
│       │   └── com/example/student/organization/common
│       │       ├── package-info.java
│       │       └── OrganizationTraceUtilsTest.java                // 工具测试
│       └── resources
│           └── application-test.yml                               // 测试配置
```

### 4.2.3 organization-facade

```text
student-management-organization-facade
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/facade
│   │           ├── package-info.java
│   │           ├── user
│   │           │   ├── package-info.java
│   │           │   ├── UserFacade.java                            // 用户 Facade
│   │           │   ├── RoleFacade.java                            // 角色 Facade
│   │           │   ├── PermissionFacade.java                      // 权限 Facade
│   │           │   └── dto
│   │           │       ├── CreateUserDTO.java                     // 创建用户 DTO
│   │           │       ├── UserDetailDTO.java                     // 用户详情 DTO
│   │           │       ├── AssignRoleDTO.java                     // 分配角色 DTO
│   │           │       └── PermissionTreeDTO.java                 // 权限树 DTO
│   │           ├── teaching
│   │           │   ├── package-info.java
│   │           │   ├── SchoolClassFacade.java                     // 班级 Facade
│   │           │   ├── GradeFacade.java                           // 年级 Facade
│   │           │   └── dto
│   │           │       ├── CreateSchoolClassDTO.java              // 创建班级 DTO
│   │           │       ├── SchoolClassDetailDTO.java              // 班级详情 DTO
│   │           │       ├── CreateGradeDTO.java                    // 创建年级 DTO
│   │           │       └── GradeDetailDTO.java                    // 年级详情 DTO
│   │           ├── enums
│   │           │   ├── package-info.java
│   │           │   ├── OrganizationFacadeStatus.java              // Facade 状态枚举
│   │           │   ├── UserFacadeType.java                        // 用户 Facade 类型
│   │           │   └── TeachingFacadeType.java                    // 教学组织 Facade 类型
│   │           ├── exceptions
│   │           │   ├── package-info.java
│   │           │   └── OrganizationFacadeException.java           // Facade 异常
│   │           └── utils
│   │               ├── package-info.java
│   │               └── OrganizationFacadeUtils.java               // Facade 工具
│   └── test
│       ├── java
│       │   └── com/example/student/organization/facade
│       │       ├── package-info.java
│       │       └── OrganizationFacadeContractTest.java            // Facade 契约测试
│       └── resources
│           └── application-test.yml
```

### 4.2.4 organization-application

```text
student-management-organization-application
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/application
│   │           ├── package-info.java
│   │           ├── manage
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── UserManage.java                       // 用户应用服务接口
│   │           │   │   ├── RoleManage.java                       // 角色应用服务接口
│   │           │   │   ├── PermissionManage.java                 // 权限应用服务接口
│   │           │   │   └── impl
│   │           │   │       ├── package-info.java
│   │           │   │       ├── UserManageImpl.java               // 用户应用服务实现
│   │           │   │       ├── RoleManageImpl.java               // 角色应用服务实现
│   │           │   │       └── PermissionManageImpl.java         // 权限应用服务实现
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       ├── SchoolClassManage.java                // 班级应用服务接口
│   │           │       ├── GradeManage.java                      // 年级应用服务接口
│   │           │       └── impl
│   │           │           ├── package-info.java
│   │           │           ├── SchoolClassManageImpl.java        // 班级应用服务实现
│   │           │           └── GradeManageImpl.java              // 年级应用服务实现
│   │           ├── converter
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── UserApplicationConverter.java         // 用户应用转换器
│   │           │   │   └── RoleApplicationConverter.java         // 角色应用转换器
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       ├── SchoolClassApplicationConverter.java  // 班级应用转换器
│   │           │       └── GradeApplicationConverter.java        // 年级应用转换器
│   │           ├── validators
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── UserApplicationValidator.java         // 用户用例校验器
│   │           │   │   └── PermissionApplicationValidator.java   // 权限用例校验器
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       └── SchoolClassApplicationValidator.java  // 班级用例校验器
│   │           ├── assemblers
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   └── UserAssembler.java                    // 用户装配器
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       └── SchoolClassAssembler.java             // 班级装配器
│   │           ├── command
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── CreateUserCommand.java                // 创建用户命令
│   │           │   │   └── AssignRoleCommand.java                // 分配角色命令
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       └── CreateSchoolClassCommand.java         // 创建班级命令
│   │           ├── query
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   └── UserDetailQuery.java                  // 用户详情查询
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       └── SchoolClassDetailQuery.java           // 班级详情查询
│   │           └── result
│   │               ├── package-info.java
│   │               ├── user
│   │               │   ├── package-info.java
│   │               │   └── UserDetailResult.java                 // 用户详情结果
│   │               └── teaching
│   │                   ├── package-info.java
│   │                   └── SchoolClassDetailResult.java          // 班级详情结果
│   └── test
│       ├── java
│       │   └── com/example/student/organization/application
│       │       ├── package-info.java
│       │       ├── user
│       │       │   ├── package-info.java
│       │       │   └── UserManageImplTest.java                  // 用户应用服务测试
│       │       └── teaching
│       │           ├── package-info.java
│       │           └── SchoolClassManageImplTest.java           // 班级应用服务测试
│       └── resources
│           └── application-test.yml
```

### 4.2.5 organization-domain

```text
student-management-organization-domain
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/domain
│   │           ├── package-info.java
│   │           ├── entities
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── User.java                            // 用户实体
│   │           │   │   ├── Role.java                            // 角色实体
│   │           │   │   └── Permission.java                      // 权限实体
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       ├── SchoolClass.java                     // 班级实体
│   │           │       └── Grade.java                           // 年级实体
│   │           ├── aggregates
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── UserAggregate.java                   // 用户聚合
│   │           │   │   └── RolePermissionAggregate.java         // 角色权限聚合
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       └── SchoolClassAggregate.java            // 班级聚合
│   │           ├── vos
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── UserId.java                          // 用户 ID 值对象
│   │           │   │   ├── RoleCode.java                        // 角色编码值对象
│   │           │   │   └── PermissionCode.java                  // 权限编码值对象
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       ├── SchoolClassId.java                   // 班级 ID 值对象
│   │           │       └── GradeCode.java                       // 年级编码值对象
│   │           ├── service
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── UserDomainService.java               // 用户领域服务接口
│   │           │   │   ├── PermissionDomainService.java         // 权限领域服务接口
│   │           │   │   └── impl
│   │           │   │       ├── package-info.java
│   │           │   │       ├── UserDomainServiceImpl.java       // 用户领域服务实现
│   │           │   │       └── PermissionDomainServiceImpl.java // 权限领域服务实现
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       ├── SchoolClassDomainService.java        // 班级领域服务接口
│   │           │       └── impl
│   │           │           ├── package-info.java
│   │           │           └── SchoolClassDomainServiceImpl.java // 班级领域服务实现
│   │           ├── repos
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   ├── UserRepository.java                  // 用户仓储接口
│   │           │   │   ├── RoleRepository.java                  // 角色仓储接口
│   │           │   │   └── PermissionRepository.java            // 权限仓储接口
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       ├── SchoolClassRepository.java           // 班级仓储接口
│   │           │       └── GradeRepository.java                 // 年级仓储接口
│   │           ├── client
│   │           │   ├── package-info.java
│   │           │   └── evaluation
│   │           │       ├── package-info.java
│   │           │       └── EvaluationQueryPort.java             // Evaluation Facade ACL 出站端口
│   │           ├── validators
│   │           │   ├── package-info.java
│   │           │   ├── user
│   │           │   │   ├── package-info.java
│   │           │   │   └── UserDomainValidator.java             // 用户领域校验器
│   │           │   └── teaching
│   │           │       ├── package-info.java
│   │           │       └── SchoolClassDomainValidator.java      // 班级领域校验器
│   │           └── enums
│   │               ├── package-info.java
│   │               ├── user
│   │               │   ├── package-info.java
│   │               │   ├── UserStatus.java                      // 用户状态
│   │               │   ├── RoleStatus.java                      // 角色状态
│   │               │   └── PermissionType.java                  // 权限类型
│   │               └── teaching
│   │                   ├── package-info.java
│   │                   ├── SchoolClassStatus.java               // 班级状态
│   │                   └── GradeStatus.java                     // 年级状态
│   └── test
│       ├── java
│       │   └── com/example/student/organization/domain
│       │       ├── package-info.java
│       │       ├── user
│       │       │   ├── package-info.java
│       │       │   └── UserDomainServiceTest.java               // 用户领域测试
│       │       └── teaching
│       │           ├── package-info.java
│       │           └── SchoolClassDomainServiceTest.java        // 班级领域测试
│       └── resources
│           └── application-test.yml
```

### 4.2.6 organization-infrastructure

```text
student-management-organization-infrastructure
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/student/organization/infrastructure
│   │   │       ├── package-info.java
│   │   │       ├── repo
│   │   │       │   ├── package-info.java
│   │   │       │   ├── user
│   │   │       │   │   ├── package-info.java
│   │   │       │   │   ├── impl
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── UserRepositoryImpl.java          // 用户仓储实现
│   │   │       │   │   │   ├── RoleRepositoryImpl.java          // 角色仓储实现
│   │   │       │   │   │   └── PermissionRepositoryImpl.java    // 权限仓储实现
│   │   │       │   │   ├── po
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── UserPO.java                      // 用户持久化对象
│   │   │       │   │   │   ├── RolePO.java                      // 角色持久化对象
│   │   │       │   │   │   └── PermissionPO.java                // 权限持久化对象
│   │   │       │   │   ├── mp
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── mapper
│   │   │       │   │   │   │   ├── package-info.java
│   │   │       │   │   │   │   ├── UserMapper.java              // 用户 Mapper
│   │   │       │   │   │   │   ├── RoleMapper.java              // 角色 Mapper
│   │   │       │   │   │   │   └── PermissionMapper.java        // 权限 Mapper
│   │   │       │   │   │   ├── service
│   │   │       │   │   │   │   ├── package-info.java
│   │   │       │   │   │   │   ├── UserMpService.java           // 用户 MP Service
│   │   │       │   │   │   │   ├── RoleMpService.java           // 角色 MP Service
│   │   │       │   │   │   │   └── PermissionMpService.java     // 权限 MP Service
│   │   │       │   │   │   └── service.impl
│   │   │       │   │   │       ├── package-info.java
│   │   │       │   │   │       ├── UserMpServiceImpl.java       // 用户 MP Service 实现
│   │   │       │   │   │       ├── RoleMpServiceImpl.java       // 角色 MP Service 实现
│   │   │       │   │   │       └── PermissionMpServiceImpl.java // 权限 MP Service 实现
│   │   │       │   │   ├── jpa
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── UserJpaRepository.java           // 用户 JPA Repository
│   │   │       │   │   │   ├── RoleJpaRepository.java           // 角色 JPA Repository
│   │   │       │   │   │   └── PermissionJpaRepository.java     // 权限 JPA Repository
│   │   │       │   │   └── converter
│   │   │       │   │       ├── package-info.java
│   │   │       │   │       ├── UserPOConverter.java             // 用户 PO 转换器
│   │   │       │   │       ├── RolePOConverter.java             // 角色 PO 转换器
│   │   │       │   │       └── PermissionPOConverter.java       // 权限 PO 转换器
│   │   │       │   └── teaching
│   │   │       │       ├── package-info.java
│   │   │       │       ├── impl
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── SchoolClassRepositoryImpl.java   // 班级仓储实现
│   │   │       │       │   └── GradeRepositoryImpl.java         // 年级仓储实现
│   │   │       │       ├── po
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── SchoolClassPO.java               // 班级持久化对象
│   │   │       │       │   └── GradePO.java                     // 年级持久化对象
│   │   │       │       ├── mp
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── mapper
│   │   │       │       │   │   ├── package-info.java
│   │   │       │       │   │   ├── SchoolClassMapper.java       // 班级 Mapper
│   │   │       │       │   │   └── GradeMapper.java             // 年级 Mapper
│   │   │       │       │   ├── service
│   │   │       │       │   │   ├── package-info.java
│   │   │       │       │   │   ├── SchoolClassMpService.java    // 班级 MP Service
│   │   │       │       │   │   └── GradeMpService.java          // 年级 MP Service
│   │   │       │       │   └── service.impl
│   │   │       │       │       ├── package-info.java
│   │   │       │       │       ├── SchoolClassMpServiceImpl.java // 班级 MP Service 实现
│   │   │       │       │       └── GradeMpServiceImpl.java      // 年级 MP Service 实现
│   │   │       │       ├── jpa
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── SchoolClassJpaRepository.java    // 班级 JPA Repository
│   │   │       │       │   └── GradeJpaRepository.java          // 年级 JPA Repository
│   │   │       │       └── converter
│   │   │       │           ├── package-info.java
│   │   │       │           ├── SchoolClassPOConverter.java      // 班级 PO 转换器
│   │   │       │           └── GradePOConverter.java            // 年级 PO 转换器
│   │   │       ├── client/evaluation
│   │   │       │   ├── package-info.java
│   │   │       │   └── DubboEvaluationQueryClient.java          // Evaluation Facade ACL
│   │   │       ├── validators
│   │   │       │   ├── package-info.java
│   │   │       │   └── OrganizationInfraValidator.java          // 基础设施校验器
│   │   │       ├── aop
│   │   │       │   ├── package-info.java
│   │   │       │   └── OrganizationInfraLogAspect.java          // 基础设施日志切面
│   │   │       ├── mq
│   │   │       │   ├── package-info.java
│   │   │       │   └── OrganizationEventProducer.java           // 组织工程出站消息发送
│   │   │       ├── cache
│   │   │       │   ├── package-info.java
│   │   │       │   ├── UserCache.java                           // 用户缓存
│   │   │       │   └── SchoolClassCache.java                    // 班级缓存
│   │   │       └── config
│   │   │           ├── package-info.java
│   │   │           ├── OrganizationMybatisPlusConfig.java        // MP 配置
│   │   │           ├── OrganizationJpaConfig.java                // JPA 配置
│   │   │           ├── OrganizationRedisConfig.java              // Redis 配置
│   │   │           └── OrganizationMqConfig.java                 // MQ 配置
│   │   └── resources
│   │       ├── mapper
│   │       │   ├── user
│   │       │   │   ├── UserMapper.xml                            // 用户 Mapper XML
│   │       │   │   ├── RoleMapper.xml                            // 角色 Mapper XML
│   │       │   │   └── PermissionMapper.xml                      // 权限 Mapper XML
│   │       │   └── teaching
│   │       │       ├── SchoolClassMapper.xml                     // 班级 Mapper XML
│   │       │       └── GradeMapper.xml                           // 年级 Mapper XML
│   │       └── application-infrastructure.yml                    // 基础设施配置
│   └── test
│       ├── java
│       │   └── com/example/student/organization/infrastructure
│       │       ├── package-info.java
│       │       ├── user
│       │       │   ├── package-info.java
│       │       │   └── UserRepositoryImplTest.java               // 用户仓储测试
│       │       └── teaching
│       │           ├── package-info.java
│       │           └── SchoolClassRepositoryImplTest.java        // 班级仓储测试
│       └── resources
│           ├── application-test.yml
│           └── db/migration/V1__organization_test_schema.sql     // 测试库脚本
```

### 4.2.7 organization-adapter

```text
student-management-organization-adapter
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/organization/adapter
│   │           ├── package-info.java
│   │           ├── user/controller
│   │           │   ├── package-info.java
│   │           │   ├── UserController.java                       // 用户 HTTP 控制器
│   │           │   ├── RoleController.java                       // 角色 HTTP 控制器
│   │           │   └── PermissionController.java                 // 权限 HTTP 控制器
│   │           ├── teaching/controller
│   │           │   ├── package-info.java
│   │           │   ├── SchoolClassController.java                // 班级 HTTP 控制器
│   │           │   └── GradeController.java                      // 年级 HTTP 控制器
│   │           ├── user/mq
│   │           │   └── UserCreatedConsumer.java                  // 用户创建入站消息消费者
│   │           ├── teaching/mq
│   │           │   └── SchoolClassChangedConsumer.java           // 班级变更入站消息消费者
│   │           ├── user/rpc
│   │           │   └── UserRpcProvider.java                      // 用户 RPC Provider
│   │           ├── teaching/rpc
│   │           │   └── SchoolClassRpcProvider.java               // 班级 RPC Provider
│   │           ├── user/graphql
│   │           │   └── UserResolver.java                         // 用户 GraphQL Resolver
│   │           ├── teaching/graphql
│   │           │   └── SchoolClassResolver.java                  // 班级 GraphQL Resolver
│   │           ├── graphql
│   │           │   └── OrganizationGraphQlContextInterceptor.java // 跨领域共享
│   │           ├── user/facade/impl
│   │           │   ├── UserFacadeImpl.java                       // 用户 Facade 实现
│   │           │   ├── RoleFacadeImpl.java                       // 角色 Facade 实现
│   │           │   └── PermissionFacadeImpl.java                 // 权限 Facade 实现
│   │           ├── teaching/facade/impl
│   │           │   ├── SchoolClassFacadeImpl.java                // 班级 Facade 实现
│   │           │   └── GradeFacadeImpl.java                      // 年级 Facade 实现
│   │           ├── facade/impl
│   │           │   └── OrganizationFacadeSupport.java            // 跨领域共享
│   │           ├── user/dto
│   │           │   ├── CreateUserRequest.java                    // 创建用户请求
│   │           │   └── AssignRoleRequest.java                    // 分配角色请求
│   │           ├── teaching/dto
│   │           │   └── CreateSchoolClassRequest.java             // 创建班级请求
│   │           ├── user/vo
│   │           │   └── UserDetailVO.java                         // 用户详情 VO
│   │           ├── teaching/vo
│   │           │   └── SchoolClassDetailVO.java                  // 班级详情 VO
│   │           ├── user/converter
│   │           │   └── UserAdapterConverter.java                 // 用户入站转换器
│   │           ├── teaching/converter
│   │           │   └── SchoolClassAdapterConverter.java          // 班级入站转换器
│   │           ├── user/validators
│   │           │   └── UserRequestValidator.java                 // 用户请求格式校验器
│   │           ├── teaching/validators
│   │           │   └── SchoolClassRequestValidator.java          // 班级请求格式校验器
│   │           ├── handler
│   │           │   ├── package-info.java
│   │           │   └── OrganizationGlobalExceptionHandler.java   // 全局异常处理器
│   │           └── filter
│   │               ├── package-info.java
│   │               ├── OrganizationTraceFilter.java              // Trace 过滤器
│   │               └── OrganizationAuthContextFilter.java        // 鉴权上下文过滤器
│   └── test
│       ├── java
│       │   └── com/example/student/organization/adapter
│       │       ├── package-info.java
│       │       ├── user/controller
│       │       │   ├── UserControllerTest.java                   // 用户控制器测试
│       │       │   └── RolePermissionControllerTest.java         // 角色权限控制器测试
│       │       └── teaching/controller
│       │           └── TeachingControllerTest.java               // 教学控制器测试
│       └── resources
│           └── application-test.yml
```

---

## 4.3 student-management-evaluation 工程结构

`student-management-evaluation` 是独立 Project，包含两个领域：

```text
course      // 课程、课程安排、课程资源
exam        // 考试、成绩、评价
```

业务代码统一采用领域优先路径，Service Project 不创建 Controller、Web Filter、GraphQL 或 VO 包：

```text
facade/course/{CourseFacade,dto}
facade/exam/{ExamFacade,ScoreFacade,dto}
domain/{course,exam}/{aggregates,entities,enums,event,repos,service,validators,vos}
application/{course,exam}/{command,converter,manage,query,result,validators}
infrastructure/{course,exam}/{repo,mq}
adapter/{course,exam}/{converter,facade/impl,validators}
adapter/exam/{dto,mq}
```

`domain/client/organization` 与 `infrastructure/client/organization` 是 Evaluation 访问外部 Organization Facade 的 ACL，也是本工程唯一保留的技术优先外部客户端路径。共享的响应包装、异常、配置与基础设施支持继续保留在各层根包。

### 4.3.1 evaluation-starter

```text
student-management-evaluation-starter
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/student/evaluation/starter
│   │   │       ├── package-info.java
│   │   │       ├── EvaluationApplication.java                    // 评价管理工程启动类
│   │   │       └── config
│   │   │           ├── package-info.java
│   │   │           ├── EvaluationSwaggerConfig.java               // Swagger 配置
│   │   │           └── EvaluationActuatorConfig.java              // Actuator 配置
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       ├── bootstrap.yml
│   │       └── logback-spring.xml
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/starter
│       │       ├── package-info.java
│       │       └── EvaluationApplicationTest.java                 // 启动测试
│       └── resources
│           ├── application-test.yml
│           └── logback-test.xml
```

### 4.3.2 evaluation-common

```text
student-management-evaluation-common
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/common
│   │           ├── package-info.java
│   │           ├── constants
│   │           │   ├── package-info.java
│   │           │   └── EvaluationCommonConstants.java             // 评价工程通用常量
│   │           ├── utils
│   │           │   ├── package-info.java
│   │           │   └── EvaluationDateUtils.java                   // 日期通用工具
│   │           ├── enums
│   │           │   ├── package-info.java
│   │           │   └── EvaluationOperationStatus.java             // 业务无关操作状态
│   │           └── exceptions
│   │               ├── package-info.java
│   │               ├── EvaluationBaseException.java               // 基础异常
│   │               └── EvaluationErrorCode.java                   // 基础错误码
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/common
│       │       ├── package-info.java
│       │       └── EvaluationDateUtilsTest.java                   // 日期工具测试
│       └── resources
│           └── application-test.yml
```

### 4.3.3 evaluation-facade

```text
student-management-evaluation-facade
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/facade
│   │           ├── package-info.java
│   │           ├── course
│   │           │   ├── package-info.java
│   │           │   ├── CourseFacade.java                         // 课程 Facade
│   │           │   └── CourseScheduleFacade.java                 // 课程安排 Facade
│   │           ├── exam
│   │           │   ├── package-info.java
│   │           │   ├── ExamFacade.java                           // 考试 Facade
│   │           │   └── ScoreFacade.java                          // 成绩 Facade
│   │           ├── dto
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   ├── CreateCourseDTO.java                  // 创建课程 DTO
│   │           │   │   ├── CourseDetailDTO.java                  // 课程详情 DTO
│   │           │   │   └── CourseScheduleDTO.java                // 课程安排 DTO
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       ├── CreateExamDTO.java                    // 创建考试 DTO
│   │           │       ├── ExamDetailDTO.java                    // 考试详情 DTO
│   │           │       ├── SubmitScoreDTO.java                   // 提交成绩 DTO
│   │           │       └── ScoreDetailDTO.java                   // 成绩详情 DTO
│   │           ├── enums
│   │           │   ├── package-info.java
│   │           │   ├── EvaluationFacadeStatus.java               // Facade 状态枚举
│   │           │   ├── CourseFacadeType.java                     // 课程 Facade 类型
│   │           │   └── ExamFacadeType.java                       // 考试 Facade 类型
│   │           ├── exceptions
│   │           │   ├── package-info.java
│   │           │   └── EvaluationFacadeException.java            // Facade 异常
│   │           └── utils
│   │               ├── package-info.java
│   │               └── EvaluationFacadeUtils.java                // Facade 工具
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/facade
│       │       ├── package-info.java
│       │       └── EvaluationFacadeContractTest.java             // Facade 契约测试
│       └── resources
│           └── application-test.yml
```

### 4.3.4 evaluation-application

```text
student-management-evaluation-application
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/application
│   │           ├── package-info.java
│   │           ├── manage
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   ├── CourseManage.java                    // 课程应用服务接口
│   │           │   │   ├── CourseScheduleManage.java            // 课程安排应用服务接口
│   │           │   │   └── impl
│   │           │   │       ├── package-info.java
│   │           │   │       ├── CourseManageImpl.java            // 课程应用服务实现
│   │           │   │       └── CourseScheduleManageImpl.java    // 课程安排应用服务实现
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       ├── ExamManage.java                      // 考试应用服务接口
│   │           │       ├── ScoreManage.java                     // 成绩应用服务接口
│   │           │       └── impl
│   │           │           ├── package-info.java
│   │           │           ├── ExamManageImpl.java              // 考试应用服务实现
│   │           │           └── ScoreManageImpl.java             // 成绩应用服务实现
│   │           ├── converter
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   └── CourseApplicationConverter.java      // 课程应用转换器
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       └── ExamApplicationConverter.java        // 考试应用转换器
│   │           ├── validators
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   └── CourseApplicationValidator.java      // 课程用例校验器
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       └── ExamApplicationValidator.java        // 考试用例校验器
│   │           ├── assemblers
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   └── CourseAssembler.java                 // 课程装配器
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       └── ExamAssembler.java                   // 考试装配器
│   │           ├── command
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   └── CreateCourseCommand.java             // 创建课程命令
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       └── SubmitScoreCommand.java              // 提交成绩命令
│   │           ├── query
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   └── CourseDetailQuery.java               // 课程详情查询
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       └── ExamDetailQuery.java                 // 考试详情查询
│   │           └── result
│   │               ├── package-info.java
│   │               ├── course
│   │               │   ├── package-info.java
│   │               │   └── CourseDetailResult.java              // 课程详情结果
│   │               └── exam
│   │                   ├── package-info.java
│   │                   └── ExamDetailResult.java                // 考试详情结果
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/application
│       │       ├── package-info.java
│       │       ├── course
│       │       │   ├── package-info.java
│       │       │   └── CourseManageImplTest.java                // 课程应用服务测试
│       │       └── exam
│       │           ├── package-info.java
│       │           └── ExamManageImplTest.java                  // 考试应用服务测试
│       └── resources
│           └── application-test.yml
```

### 4.3.5 evaluation-domain

```text
student-management-evaluation-domain
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/domain
│   │           ├── package-info.java
│   │           ├── entities
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   ├── Course.java                          // 课程实体
│   │           │   │   └── CourseSchedule.java                  // 课程安排实体
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       ├── Exam.java                            // 考试实体
│   │           │       └── Score.java                           // 成绩实体
│   │           ├── aggregates
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   └── CourseAggregate.java                 // 课程聚合
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       ├── ExamAggregate.java                   // 考试聚合
│   │           │       └── ScoreAggregate.java                  // 成绩聚合
│   │           ├── vos
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   ├── CourseId.java                        // 课程 ID 值对象
│   │           │   │   └── CourseCode.java                      // 课程编码值对象
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       ├── ExamId.java                          // 考试 ID 值对象
│   │           │       └── ScoreValue.java                      // 成绩值对象
│   │           ├── service
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   ├── CourseDomainService.java             // 课程领域服务接口
│   │           │   │   └── impl
│   │           │   │       ├── package-info.java
│   │           │   │       └── CourseDomainServiceImpl.java     // 课程领域服务实现
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       ├── ExamDomainService.java               // 考试领域服务接口
│   │           │       ├── ScoreDomainService.java              // 成绩领域服务接口
│   │           │       └── impl
│   │           │           ├── package-info.java
│   │           │           ├── ExamDomainServiceImpl.java       // 考试领域服务实现
│   │           │           └── ScoreDomainServiceImpl.java      // 成绩领域服务实现
│   │           ├── repos
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   ├── CourseRepository.java                // 课程仓储接口
│   │           │   │   └── CourseScheduleRepository.java        // 课程安排仓储接口
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       ├── ExamRepository.java                  // 考试仓储接口
│   │           │       └── ScoreRepository.java                 // 成绩仓储接口
│   │           ├── client
│   │           │   ├── package-info.java
│   │           │   └── organization
│   │           │       ├── package-info.java
│   │           │       └── OrganizationDirectoryPort.java       // Organization Facade ACL 出站端口
│   │           ├── validators
│   │           │   ├── package-info.java
│   │           │   ├── course
│   │           │   │   ├── package-info.java
│   │           │   │   └── CourseDomainValidator.java           // 课程领域校验器
│   │           │   └── exam
│   │           │       ├── package-info.java
│   │           │       └── ExamDomainValidator.java             // 考试领域校验器
│   │           └── enums
│   │               ├── package-info.java
│   │               ├── course
│   │               │   ├── package-info.java
│   │               │   ├── CourseStatus.java                    // 课程状态
│   │               │   └── CourseType.java                      // 课程类型
│   │               └── exam
│   │                   ├── package-info.java
│   │                   ├── ExamStatus.java                      // 考试状态
│   │                   └── ScoreStatus.java                     // 成绩状态
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/domain
│       │       ├── package-info.java
│       │       ├── course
│       │       │   ├── package-info.java
│       │       │   └── CourseDomainServiceTest.java             // 课程领域测试
│       │       └── exam
│       │           ├── package-info.java
│       │           └── ExamDomainServiceTest.java               // 考试领域测试
│       └── resources
│           └── application-test.yml
```

### 4.3.6 evaluation-infrastructure

```text
student-management-evaluation-infrastructure
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/student/evaluation/infrastructure
│   │   │       ├── package-info.java
│   │   │       ├── repo
│   │   │       │   ├── package-info.java
│   │   │       │   ├── course
│   │   │       │   │   ├── package-info.java
│   │   │       │   │   ├── impl
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── CourseRepositoryImpl.java        // 课程仓储实现
│   │   │       │   │   │   └── CourseScheduleRepositoryImpl.java // 课程安排仓储实现
│   │   │       │   │   ├── po
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── CoursePO.java                    // 课程持久化对象
│   │   │       │   │   │   └── CourseSchedulePO.java            // 课程安排持久化对象
│   │   │       │   │   ├── mp
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── mapper
│   │   │       │   │   │   │   ├── package-info.java
│   │   │       │   │   │   │   ├── CourseMapper.java            // 课程 Mapper
│   │   │       │   │   │   │   └── CourseScheduleMapper.java    // 课程安排 Mapper
│   │   │       │   │   │   ├── service
│   │   │       │   │   │   │   ├── package-info.java
│   │   │       │   │   │   │   ├── CourseMpService.java         // 课程 MP Service
│   │   │       │   │   │   │   └── CourseScheduleMpService.java // 课程安排 MP Service
│   │   │       │   │   │   └── service.impl
│   │   │       │   │   │       ├── package-info.java
│   │   │       │   │   │       ├── CourseMpServiceImpl.java     // 课程 MP Service 实现
│   │   │       │   │   │       └── CourseScheduleMpServiceImpl.java // 课程安排 MP Service 实现
│   │   │       │   │   ├── jpa
│   │   │       │   │   │   ├── package-info.java
│   │   │       │   │   │   ├── CourseJpaRepository.java         // 课程 JPA Repository
│   │   │       │   │   │   └── CourseScheduleJpaRepository.java // 课程安排 JPA Repository
│   │   │       │   │   └── converter
│   │   │       │   │       ├── package-info.java
│   │   │       │   │       ├── CoursePOConverter.java           // 课程 PO 转换器
│   │   │       │   │       └── CourseSchedulePOConverter.java   // 课程安排 PO 转换器
│   │   │       │   └── exam
│   │   │       │       ├── package-info.java
│   │   │       │       ├── impl
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── ExamRepositoryImpl.java          // 考试仓储实现
│   │   │       │       │   └── ScoreRepositoryImpl.java         // 成绩仓储实现
│   │   │       │       ├── po
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── ExamPO.java                      // 考试持久化对象
│   │   │       │       │   └── ScorePO.java                     // 成绩持久化对象
│   │   │       │       ├── mp
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── mapper
│   │   │       │       │   │   ├── package-info.java
│   │   │       │       │   │   ├── ExamMapper.java              // 考试 Mapper
│   │   │       │       │   │   └── ScoreMapper.java             // 成绩 Mapper
│   │   │       │       │   ├── service
│   │   │       │       │   │   ├── package-info.java
│   │   │       │       │   │   ├── ExamMpService.java           // 考试 MP Service
│   │   │       │       │   │   └── ScoreMpService.java          // 成绩 MP Service
│   │   │       │       │   └── service.impl
│   │   │       │       │       ├── package-info.java
│   │   │       │       │       ├── ExamMpServiceImpl.java       // 考试 MP Service 实现
│   │   │       │       │       └── ScoreMpServiceImpl.java      // 成绩 MP Service 实现
│   │   │       │       ├── jpa
│   │   │       │       │   ├── package-info.java
│   │   │       │       │   ├── ExamJpaRepository.java           // 考试 JPA Repository
│   │   │       │       │   └── ScoreJpaRepository.java          // 成绩 JPA Repository
│   │   │       │       └── converter
│   │   │       │           ├── package-info.java
│   │   │       │           ├── ExamPOConverter.java             // 考试 PO 转换器
│   │   │       │           └── ScorePOConverter.java            // 成绩 PO 转换器
│   │   │       ├── client/organization
│   │   │       │   ├── package-info.java
│   │   │       │   └── DubboOrganizationDirectoryClient.java    // Organization Facade ACL
│   │   │       ├── validators
│   │   │       │   ├── package-info.java
│   │   │       │   └── EvaluationInfraValidator.java            // 基础设施校验器
│   │   │       ├── aop
│   │   │       │   ├── package-info.java
│   │   │       │   └── EvaluationInfraLogAspect.java            // 基础设施日志切面
│   │   │       ├── mq
│   │   │       │   ├── package-info.java
│   │   │       │   └── EvaluationEventProducer.java             // 评价工程出站消息发送
│   │   │       ├── cache
│   │   │       │   ├── package-info.java
│   │   │       │   ├── CourseCache.java                         // 课程缓存
│   │   │       │   └── ExamCache.java                           // 考试缓存
│   │   │       └── config
│   │   │           ├── package-info.java
│   │   │           ├── EvaluationMybatisPlusConfig.java          // MP 配置
│   │   │           ├── EvaluationJpaConfig.java                  // JPA 配置
│   │   │           ├── EvaluationRedisConfig.java                // Redis 配置
│   │   │           └── EvaluationMqConfig.java                   // MQ 配置
│   │   └── resources
│   │       ├── mapper
│   │       │   ├── course
│   │       │   │   ├── CourseMapper.xml                          // 课程 Mapper XML
│   │       │   │   └── CourseScheduleMapper.xml                  // 课程安排 Mapper XML
│   │       │   └── exam
│   │       │       ├── ExamMapper.xml                            // 考试 Mapper XML
│   │       │       └── ScoreMapper.xml                           // 成绩 Mapper XML
│   │       └── application-infrastructure.yml                    // 基础设施配置
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/infrastructure
│       │       ├── package-info.java
│       │       ├── course
│       │       │   ├── package-info.java
│       │       │   └── CourseRepositoryImplTest.java             // 课程仓储测试
│       │       └── exam
│       │           ├── package-info.java
│       │           └── ExamRepositoryImplTest.java               // 考试仓储测试
│       └── resources
│           ├── application-test.yml
│           └── db/migration/V1__evaluation_test_schema.sql       // 测试库脚本
```

### 4.3.7 evaluation-adapter

```text
student-management-evaluation-adapter
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com/example/student/evaluation/adapter
│   │           ├── package-info.java
│   │           ├── course
│   │           │   ├── converter
│   │           │   │   └── CourseFacadeDTOConverter.java         // 课程 Facade DTO 转换
│   │           │   ├── facade/impl
│   │           │   │   └── CourseFacadeImpl.java                 // 课程 Facade 实现
│   │           │   └── validators
│   │           │       └── CourseFacadeValidator.java            // 课程 Facade 校验
│   │           ├── exam
│   │           │   ├── converter
│   │           │   │   ├── ExamFacadeDTOConverter.java           // 考试 Facade DTO 转换
│   │           │   │   └── ScoreFacadeDTOConverter.java          // 成绩 Facade DTO 转换
│   │           │   ├── dto
│   │           │   │   └── ScoreImportMessage.java               // 考试领域内部消息 DTO
│   │           │   ├── facade/impl
│   │           │   │   ├── ExamFacadeImpl.java                   // 考试 Facade 实现
│   │           │   │   └── ScoreFacadeImpl.java                  // 成绩 Facade 实现
│   │           │   ├── mq
│   │           │   │   └── ScoreImportConsumer.java              // 成绩导入消费者
│   │           │   └── validators
│   │           │       ├── ExamFacadeValidator.java              // 考试 Facade 校验
│   │           │       └── ScoreFacadeValidator.java             // 成绩 Facade 校验
│   │           ├── handler
│   │           │   ├── package-info.java
│   │           │   └── EvaluationGlobalExceptionHandler.java     // 全局异常处理器
│   └── test
│       ├── java
│       │   └── com/example/student/evaluation/adapter
│       │       ├── package-info.java
│       │       ├── course
│       │       │   └── CourseFacadeImplTest.java                 // 课程 Facade 测试
│       │       ├── exam
│       │       │   └── ExamFacadeImplTest.java                   // 考试 Facade 测试
│       │       └── rpc
│       │           └── EvaluationDubboProviderConfigurationTest.java
│       └── resources
│           └── application-test.yml
```

---

## 5. 开发约束

## 5.1 工程边界约束

```text
1. student-management-organization 和 student-management-evaluation 是两个独立 Project。
2. 不允许再额外创建 student-management 根聚合工程统一管理两个 Project。
3. 跨 Project 调用只能通过 facade、RPC、HTTP、MQ 或 domain.client 出站端口完成。
4. 一个 Project 内部可以有多个领域包。
5. 一个 Project 内部领域之间由 application 编排，不建议 domain 之间互相依赖。
```

## 5.2 starter 约束

```text
1. starter 只负责启动和装配。
2. starter 只依赖 adapter 和 infrastructure。
3. starter 不写任何业务逻辑。
```

## 5.3 adapter 约束

```text
1. adapter 只处理入站请求。
2. adapter 可以依赖 application 和 facade。
3. adapter/facade.impl 是 Facade 实现唯一位置。
4. adapter.mq 只负责入站消息消费。
5. adapter 不直接访问数据库、缓存和 MQ 出站能力。
```

## 5.4 facade 约束

```text
1. facade 不依赖 common。
2. facade 拥有自己的 utils、enums、exceptions。
3. facade 只定义接口契约，不写实现。
4. facade 不依赖 application、domain、infrastructure、adapter。
```

## 5.5 application 约束

```text
1. application 不放 facade.impl。
2. application 的 manage 实现按 manage.user.impl、manage.teaching.impl、manage.course.impl、manage.exam.impl 分包。
3. application 负责事务和用例编排。
4. application 不直接调用 Mapper、RedisTemplate、KafkaTemplate、RabbitTemplate、JpaRepository。
```

## 5.6 domain 约束

```text
1. domain 的领域服务目录必须是 service 和 service.impl。
2. domain 不使用 domainservices、domainservicesimpl 这种命名。
3. domain 只定义领域对象、领域服务、仓储接口、出站客户端接口、领域校验器和领域枚举。
4. domain 不感知任何基础设施实现。
```

## 5.7 infrastructure 约束

```text
1. infrastructure.repo 必须按领域分包。
2. organization 使用 repo.user.*、repo.teaching.*。
3. evaluation 使用 repo.course.*、repo.exam.*。
4. repo.impl 调用 mp.service 或 jpa.repository。
5. 业务代码不允许直调 mapper。
6. infrastructure.mq 只负责出站消息发送。
7. infrastructure 只依赖 domain，不依赖 application。
8. infrastructure.client.<external-project> 只实现对应 domain.client.<external-project> 出站端口。
```

---

## 6. Validator 规范

## 6.1 Adapter Validator

负责入站请求格式校验。

```text
1. 参数不能为空。
2. 字段长度是否合法。
3. 日期格式是否合法。
4. 枚举值是否合法。
5. 分页参数是否合法。
```

示例：

```text
CreateUserRequest.username 不能为空
CreateCourseRequest.courseCode 不能为空
SubmitScoreRequest.score 不能为空
```

## 6.2 Application Validator

负责用例级校验。

```text
1. 当前用户是否有权限。
2. 当前流程是否允许继续。
3. 当前请求是否重复提交。
4. 跨领域操作前置条件是否满足。
```

示例：

```text
分配角色前校验角色是否可用
创建班级前校验年级是否存在
安排考试前校验课程是否有效
录入成绩前校验考试是否已开始评分
```

## 6.3 Domain Validator

负责领域不变量校验。

```text
1. 用户状态是否合法。
2. 角色权限关系是否合法。
3. 班级状态是否允许变更。
4. 课程状态是否允许排课。
5. 考试状态是否允许录分。
6. 成绩范围是否合法。
```

示例：

```text
禁用用户不能登录
已归档角色不能继续授权
已结课课程不能继续排课
已发布成绩不能随意修改
```

## 6.4 Infrastructure Validator

负责技术适配校验。

```text
1. 外部接口返回字段是否合法。
2. 数据库唯一键冲突转换。
3. 缓存数据结构是否合法。
4. MQ 发送结果是否合法。
5. 外部 Facade 返回码转换。
```

---

## 7. 总结

本架构最终结构是：

```text
student-management-organization  // 独立 Project
    - user
    - teaching

student-management-evaluation    // 独立 Project
    - course
    - exam
```

每个 Project 内部按统一分层拆模块：

```text
starter
common
facade
application
domain
infrastructure
adapter
```

每个分层模块内部再按领域分包：

```text
organization:
    user
    teaching

evaluation:
    course
    exam
```

最终依赖方向保持为：

```text
starter -> adapter / infrastructure
adapter -> application / facade
application -> domain
domain -> common
infrastructure -> domain
```

关键规范：

```text
1. 两个工程不是一个根工程下的两个模块。
2. adapter/facade.impl 是 Facade 实现唯一位置。
3. application 不放 facade.impl。
4. facade 不依赖 common，facade 有自己的 utils、enums、exceptions。
5. infrastructure.repo 必须按领域分包。
6. domain service 必须使用 service / service.impl。
7. application manage 必须使用 manage.user.impl 这种方向。
8. 每个包都保留 package-info.java。
9. 每个模块都补齐 src/main/resources、src/test/java、src/test/resources。
```

这套结构的目标不是“为了拆而拆”，而是在大型业务系统中保持边界清晰，避免代码最后变成一锅学生管理麻辣烫。
