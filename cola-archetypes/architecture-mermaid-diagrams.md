# 大型单体轻量领域分层架构 Mermaid 图集

> 本文用于补充《大型单体轻量领域分层架构 Code Style》中的架构图。  
> 所有图均使用 Mermaid 编写，可直接复制到支持 Mermaid 的 Markdown 编辑器、GitLab、GitHub、语雀、Typora、Obsidian 或文档平台中渲染。

---

## 1. 总体分层依赖图

```mermaid
flowchart TD
    STARTER["starter<br/>启动装配层"]
    ADAPTER["adapter<br/>入站适配层"]
    APPLICATION["application<br/>应用编排层"]
    DOMAIN["domain<br/>领域核心层"]
    INFRA["infrastructure<br/>基础设施层"]
    FACADE["facade<br/>对外契约层<br/>自包含 dto / enums / exceptions / utils"]
    COMMON["common<br/>通用基础层"]
    STARTER --> ADAPTER
    STARTER --> INFRA
    ADAPTER --> APPLICATION
    ADAPTER --> FACADE
    APPLICATION --> DOMAIN
    DOMAIN --> COMMON
    INFRA --> APPLICATION
    FACADE -. " 不依赖 common " . - FACADE 
 COMMON - . " 不依赖其他模块 " . - COMMON
```

---

## 2. 禁止依赖关系图

```mermaid
flowchart TD
    DOMAIN["domain"]
    APPLICATION["application"]
    INFRA["infrastructure"]
    ADAPTER["adapter"]
    FACADE["facade"]
    STARTER["starter"]
    COMMON["common"]
    DOMAIN -. " 禁止 " .-> APPLICATION
    DOMAIN -. " 禁止 " .-> INFRA
    DOMAIN -. " 禁止 " .-> ADAPTER
    DOMAIN -. " 禁止 " .-> FACADE
    APPLICATION -. " 禁止 " .-> INFRA
    APPLICATION -. " 禁止 " .-> ADAPTER
    APPLICATION -. " 禁止实现 " .-> FACADE
    FACADE -. " 禁止 " .-> APPLICATION
    FACADE -. " 禁止 " .-> DOMAIN
    FACADE -. " 禁止 " .-> INFRA
    FACADE -. " 禁止 " .-> ADAPTER
    FACADE -. " 禁止 " .-> COMMON
    ADAPTER -. " 禁止 " .-> INFRA
    STARTER -. " 禁止 " .-> DOMAIN
    STARTER -. " 禁止 " .-> APPLICATION
    COMMON -. " 禁止依赖业务模块 " .-> DOMAIN
```

---

## 3. 标准调用方向图

```mermaid
flowchart TD
    REQ["外部请求<br/>HTTP / RPC / MQ / GraphQL"]
    ADAPTER["adapter<br/>协议适配 / DTO 转换 / 入站校验"]
    APP["application<br/>业务用例编排 / 事务 / 权限 / 幂等"]
    DOMAIN["domain<br/>实体 / 聚合 / 领域服务 / 领域规则"]
    COMMON["common<br/>通用基础能力"]
    REQ --> ADAPTER
    ADAPTER --> APP
    APP --> DOMAIN
    DOMAIN --> COMMON
```

---

## 4. Adapter 入站适配图

```mermaid
flowchart LR
    subgraph ADAPTER["adapter 入站适配层"]
        CONTROLLER["controller<br/>HTTP 入站"]
        MQC["mq<br/>仅入站 Consumer"]
        RPC["rpc<br/>RPC Provider"]
        GRAPHQL["graphql<br/>Resolver"]
        FACADE_IMPL["facade.impl<br/>Facade 实现唯一位置"]
        DTO["dto<br/>入站请求对象"]
        VO["vo<br/>HTTP 响应对象"]
        CONVERTOR["convertor<br/>入站对象转换"]
        HANDLER["handler<br/>异常处理 / 响应包装"]
        FILTER["filter<br/>Web Filter / TraceId / 上下文"]
    end

    APP["application.manage.*"]
    CONTROLLER --> CONVERTOR
    MQC --> CONVERTOR
    RPC --> CONVERTOR
    GRAPHQL --> CONVERTOR
    FACADE_IMPL --> CONVERTOR
    DTO --> CONVERTOR
    CONVERTOR --> APP
    APP --> CONVERTOR
    CONVERTOR --> VO
    FILTER --> CONTROLLER
    HANDLER --> CONTROLLER
```

---

## 5. Facade 契约层图

```mermaid
flowchart TD
    subgraph FACADE["facade 对外契约层"]
        API["api<br/>XxxFacade.java"]
        DTO["dto<br/>XxxDTO.java / XxxQueryDTO.java"]
        ENUMS["enums<br/>FacadeResultCode.java"]
        EXCEPTIONS["exceptions<br/>FacadeException.java"]
        UTILS["utils<br/>FacadeAssert.java"]
    end

    OUTER["外部系统 / 其他模块"] --> API
    API --> DTO
    API --> ENUMS
    API --> EXCEPTIONS
    API --> UTILS
    FACADE -. " 不写实现 " .-> FACADE
    FACADE -. " 不依赖 common " .-> FACADE
```

---

## 6. Application 业务编排图

```mermaid
flowchart TD
    ADAPTER["adapter"] --> MANAGE["application.manage.{domain}.*"]

    subgraph APPLICATION["application 应用编排层"]
        MANAGE["manage.user.UserManage<br/>manage.teaching.TeachingManage"]
        MANAGE_IMPL["manage.user.impl.UserManageImpl<br/>manage.teaching.impl.TeachingManageImpl"]
        VALIDATOR["validators<br/>应用级校验"]
        ASSEMBLER["assemblers<br/>对象装配"]
        CONVERTOR["convertor<br/>对象转换"]
        CLIENT["client<br/>外部能力接口"]
    end

    MANAGE --> MANAGE_IMPL
    MANAGE_IMPL --> VALIDATOR
    MANAGE_IMPL --> ASSEMBLER
    MANAGE_IMPL --> CONVERTOR
    MANAGE_IMPL --> CLIENT
    MANAGE_IMPL --> DOMAIN["domain<br/>领域服务 / 仓储接口"]
```

---

## 7. Domain 领域核心图

```mermaid
flowchart TD
    subgraph DOMAIN["domain 领域核心层"]
        ENTITIES["entities<br/>User.java / Role.java / SchoolClass.java"]
        AGGREGATES["aggregates<br/>UserAggregate.java / CourseAggregate.java"]
        VOS["vos<br/>UserId.java / CourseCode.java"]
        SERVICE["service<br/>XxxDomainService.java"]
        SERVICE_IMPL["service.impl<br/>XxxDomainServiceImpl.java"]
        REPOS["repos<br/>XxxRepository.java<br/>只定义接口"]
        VALIDATORS["validators<br/>领域不变量校验"]
        ENUMS["enums<br/>领域状态 / 类型"]
    end

    SERVICE --> SERVICE_IMPL
    SERVICE_IMPL --> ENTITIES
    SERVICE_IMPL --> AGGREGATES
    SERVICE_IMPL --> VOS
    SERVICE_IMPL --> VALIDATORS
    SERVICE_IMPL --> REPOS
    ENTITIES --> ENUMS
    AGGREGATES --> ENUMS
```

---

## 8. Infrastructure 基础设施图

```mermaid
flowchart TD
    subgraph INFRA["infrastructure 基础设施层"]
        REPO["repo<br/>按领域分包"]
        CLIENT_IMPL["client.impl<br/>application client 实现"]
        MQ["mq<br/>仅出站 Producer"]
        CACHE["cache<br/>Redis / Caffeine 封装"]
        AOP["aop<br/>基础设施切面"]
        CONFIG["config<br/>MP / JPA / Redis / MQ 配置"]
        VALIDATORS["validators<br/>外部响应 / 技术适配校验"]
    end

    APP_CLIENT["application.client.*"] --> CLIENT_IMPL
    DOMAIN_REPO["domain.repos.*"] --> REPO
    CLIENT_IMPL --> OUTER["外部 Facade / HTTP / RPC / SDK"]
    MQ --> BROKER["Kafka / RabbitMQ / RocketMQ"]
    CACHE --> REDIS["Redis / Local Cache"]
    CONFIG --> REPO
    AOP --> CLIENT_IMPL
    VALIDATORS --> CLIENT_IMPL
```

---

## 9. Repository 调用链路图

```mermaid
sequenceDiagram
    participant Adapter as adapter
    participant App as application.manage
    participant DomainRepo as domain.repos.XxxRepository
    participant RepoImpl as infrastructure.repo.{domain}.impl.XxxRepositoryImpl
    participant MpService as infrastructure.repo.{domain}.mp.service.XxxMpService
    participant Mapper as infrastructure.repo.{domain}.mp.mapper.XxxMapper
    participant JpaRepo as infrastructure.repo.{domain}.jpa.XxxJpaRepository
    participant DB as Database
    Adapter ->> App: 调用业务用例
    App ->> DomainRepo: 调用仓储接口
    DomainRepo ->> RepoImpl: Spring 注入具体实现

    alt 使用 MyBatis-Plus
        RepoImpl ->> MpService: 调用 MP Service
        MpService ->> Mapper: 调用 Mapper
        Mapper ->> DB: 执行 SQL
        DB -->> Mapper: 返回 PO 数据
        Mapper -->> MpService: 返回 PO
        MpService -->> RepoImpl: 返回 PO
    else 使用 JPA
        RepoImpl ->> JpaRepo: 调用 JPA Repository
        JpaRepo ->> DB: 执行 ORM 查询
        DB -->> JpaRepo: 返回 Entity / PO
        JpaRepo -->> RepoImpl: 返回数据
    end

    RepoImpl -->> DomainRepo: 转换为领域对象
    DomainRepo -->> App: 返回领域对象
    App -->> Adapter: 返回应用结果
```

---

## 10. MyBatis-Plus 仓储实现链路图

```mermaid
flowchart TD
    APP["application"] --> DOMAIN_REPO["domain.repos.UserRepository"]
    DOMAIN_REPO --> REPO_IMPL["infrastructure.repo.user.impl.UserRepositoryImpl"]
    REPO_IMPL --> CONVERTER["infrastructure.repo.user.converter.UserPOConverter"]
    REPO_IMPL --> MP_SERVICE["infrastructure.repo.user.mp.service.UserMpService"]
    MP_SERVICE --> MP_SERVICE_IMPL["infrastructure.repo.user.mp.service.impl.UserMpServiceImpl"]
    MP_SERVICE_IMPL --> MAPPER["infrastructure.repo.user.mp.mapper.UserMapper"]
    MAPPER --> DB[(Database)]
    APP -. " 禁止直调 " .-> MAPPER
    APP -. " 禁止直调 " .-> MP_SERVICE
```

---

## 11. JPA 仓储实现链路图

```mermaid
flowchart TD
    APP["application"] --> DOMAIN_REPO["domain.repos.SchoolClassRepository"]
    DOMAIN_REPO --> REPO_IMPL["infrastructure.repo.teaching.impl.SchoolClassRepositoryImpl"]
    REPO_IMPL --> CONVERTER["infrastructure.repo.teaching.converter.SchoolClassPOConverter"]
    REPO_IMPL --> JPA_REPO["infrastructure.repo.teaching.jpa.SchoolClassJpaRepository"]
    JPA_REPO --> DB[(Database)]
    APP -. " 禁止直调 " .-> JPA_REPO
```

---

## 12. 外部 Client 防腐层图

```mermaid
flowchart LR
    APP["application.manage.*"] --> CLIENT["application.client.UserClient"]
    CLIENT --> CLIENT_IMPL["infrastructure.client.impl.UserClientImpl"]
    CLIENT_IMPL --> CONVERTOR["infrastructure client converter / validator"]
    CONVERTOR --> OUTER_FACADE["外部 Facade"]
    CONVERTOR --> HTTP["外部 HTTP API"]
    CONVERTOR --> RPC["外部 RPC / gRPC"]
    CONVERTOR --> SDK["第三方 SDK"]
    APP -. " 不感知外部协议 " .-> APP
    CLIENT_IMPL -. " 隐藏外部系统细节 " .-> CLIENT_IMPL
```

---

## 13. MQ 入站与出站隔离图

```mermaid
flowchart TD
    BROKER_IN["MQ Broker<br/>入站消息"] --> CONSUMER["adapter.mq.XxxConsumer<br/>仅入站消费"]
    CONSUMER --> APP["application.manage.*"]
    APP --> DOMAIN["domain"]
    APP --> EVENT_PORT["application client / event publisher interface"]
    EVENT_PORT --> PRODUCER["infrastructure.mq.XxxProducer<br/>仅出站发送"]
    PRODUCER --> BROKER_OUT["MQ Broker<br/>出站消息"]
    CONSUMER -. " 不发送 MQ " .-> CONSUMER
    PRODUCER -. " 不消费 MQ " .-> PRODUCER
```

---

## 14. Validator 分层职责图

```mermaid
flowchart TD
    REQ["外部请求"] --> ADAPTER_VALIDATOR["adapter validator<br/>请求格式校验"]
    ADAPTER_VALIDATOR --> APP_VALIDATOR["application validator<br/>用例前置校验"]
    APP_VALIDATOR --> DOMAIN_VALIDATOR["domain validator<br/>领域不变量校验"]
    APP_VALIDATOR --> INFRA_VALIDATOR["infrastructure validator<br/>技术适配校验"]
    ADAPTER_VALIDATOR --> A1["参数为空 / 长度 / 日期格式 / 枚举值"]
    APP_VALIDATOR --> A2["权限 / 幂等 / 流程前置 / 操作上下文"]
    DOMAIN_VALIDATOR --> A3["状态流转 / 聚合一致性 / 核心业务规则"]
    INFRA_VALIDATOR --> A4["外部响应 / 缓存结构 / DB 冲突 / MQ 结果"]
```

---

## 15. 单体内两个领域示例图

```mermaid
flowchart TD
    subgraph PROJECT["student-management 单体工程"]
        subgraph USER["user 领域"]
            USER_ENTITY["User / Role / Permission"]
            USER_SERVICE["UserDomainService"]
            USER_REPO["UserRepository"]
        end

        subgraph TEACHING["teaching 领域"]
            TEACHING_ENTITY["SchoolClass / Course"]
            TEACHING_SERVICE["SchoolClassDomainService / CourseDomainService"]
            TEACHING_REPO["SchoolClassRepository / CourseRepository"]
        end

        APP["application<br/>跨领域业务编排"]
    end

    APP --> USER_SERVICE
    APP --> TEACHING_SERVICE
    USER_SERVICE --> USER_ENTITY
    TEACHING_SERVICE --> TEACHING_ENTITY
    USER_SERVICE --> USER_REPO
    TEACHING_SERVICE --> TEACHING_REPO
    USER -. " 领域之间不直接依赖 " . - TEACHING 
```

---

## 16. 典型 HTTP 请求时序图

```mermaid
sequenceDiagram
    participant Client as Client
    participant Filter as adapter.filter.TraceIdFilter
    participant Controller as adapter.controller.UserController
    participant Convertor as adapter.convertor.UserAdapterConvertor
    participant Manage as application.manage.user.UserManage
    participant DomainService as domain.service.UserDomainService
    participant Repository as domain.repos.UserRepository
    participant RepoImpl as infrastructure.repo.user.impl.UserRepositoryImpl
    participant DB as Database
    Client ->> Filter: HTTP Request
    Filter ->> Controller: 传递 TraceId / RequestContext
    Controller ->> Convertor: Request -> Command
    Convertor ->> Manage: 调用应用用例
    Manage ->> DomainService: 调用领域服务
    DomainService ->> Repository: 调用仓储接口
    Repository ->> RepoImpl: 注入仓储实现
    RepoImpl ->> DB: 查询 / 保存数据
    DB -->> RepoImpl: 返回数据
    RepoImpl -->> Repository: 返回领域对象
    Repository -->> DomainService: 返回领域对象
    DomainService -->> Manage: 返回领域结果
    Manage -->> Convertor: 返回应用结果
    Convertor -->> Controller: 转换为 VO
    Controller -->> Client: HTTP Response
```

---

## 17. 典型 RPC 请求时序图

```mermaid
sequenceDiagram
    participant Caller as 外部调用方
    participant Facade as facade.api.UserFacade
    participant Impl as adapter.facade.impl.UserFacadeImpl
    participant Convertor as adapter.convertor.UserAdapterConvertor
    participant Manage as application.manage.user.UserManage
    participant Domain as domain.service.UserDomainService
    participant Repository as domain.repos.UserRepository
    Caller ->> Facade: RPC 调用
    Facade ->> Impl: 路由到 FacadeImpl
    Impl ->> Convertor: Facade DTO -> Application 入参
    Convertor ->> Manage: 调用应用用例
    Manage ->> Domain: 执行业务规则
    Domain ->> Repository: 读写领域仓储接口
    Repository -->> Domain: 返回领域对象
    Domain -->> Manage: 返回领域结果
    Manage -->> Convertor: 返回应用结果
    Convertor -->> Impl: 转换为 Facade DTO
    Impl -->> Caller: RPC Response
```

---

## 18. 典型 MQ 入站时序图

```mermaid
sequenceDiagram
    participant Broker as MQ Broker
    participant Consumer as adapter.mq.UserCreatedConsumer
    participant Convertor as adapter.convertor.UserAdapterConvertor
    participant Manage as application.manage.user.UserManage
    participant Domain as domain.service.UserDomainService
    participant Repo as domain.repos.UserRepository
    Broker ->> Consumer: 投递消息
    Consumer ->> Convertor: Message -> Command
    Convertor ->> Manage: 调用应用用例
    Manage ->> Domain: 执行业务规则
    Domain ->> Repo: 读写仓储接口
    Repo -->> Domain: 返回领域对象
    Domain -->> Manage: 返回领域结果
    Manage -->> Consumer: 处理完成
    Consumer -->> Broker: ACK / NACK
```

---

## 19. 包结构关系图

```mermaid
flowchart TD
    ROOT["com.xxx.project"]
    ROOT --> STARTER["starter"]
    ROOT --> ADAPTER["adapter"]
    ROOT --> FACADE["facade"]
    ROOT --> APPLICATION["application"]
    ROOT --> DOMAIN["domain"]
    ROOT --> INFRA["infrastructure"]
    ROOT --> COMMON["common"]
    ADAPTER --> ADAPTER_CONTROLLER["controller"]
    ADAPTER --> ADAPTER_MQ["mq 仅入站"]
    ADAPTER --> ADAPTER_RPC["rpc"]
    ADAPTER --> ADAPTER_FACADE_IMPL["facade.impl"]
    APPLICATION --> APP_MANAGE_USER["manage.user.impl"]
    APPLICATION --> APP_MANAGE_TEACHING["manage.teaching.impl"]
    APPLICATION --> APP_CLIENT["client"]
    DOMAIN --> DOMAIN_SERVICE["service.impl"]
    DOMAIN --> DOMAIN_REPOS["repos"]
    INFRA --> INFRA_REPO_USER["repo.user.impl"]
    INFRA --> INFRA_REPO_TEACHING["repo.teaching.impl"]
    INFRA --> INFRA_CLIENT_IMPL["client.impl"]
    INFRA --> INFRA_MQ["mq 仅出站"]
```

---

## 20. 架构边界总览图

```mermaid
flowchart LR
    subgraph INBOUND["入站边界"]
        HTTP["HTTP"]
        RPC["RPC"]
        MQ_IN["MQ Consumer"]
        GRAPHQL["GraphQL"]
    end

    subgraph CORE["业务核心"]
        APP["application<br/>业务流程"]
        DOMAIN["domain<br/>业务规则"]
    end

    subgraph OUTBOUND["出站边界"]
        DB["Database"]
        CACHE["Redis / Cache"]
        MQ_OUT["MQ Producer"]
        EXT["External System"]
    end

    HTTP --> ADAPTER["adapter"]
    RPC --> ADAPTER
    MQ_IN --> ADAPTER
    GRAPHQL --> ADAPTER
    ADAPTER --> APP
    APP --> DOMAIN
    DOMAIN --> REPO_IF["domain.repos 接口"]
    REPO_IF --> INFRA["infrastructure"]
    INFRA --> DB
    INFRA --> CACHE
    INFRA --> MQ_OUT
    INFRA --> EXT
```
