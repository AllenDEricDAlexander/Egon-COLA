# 大型单体轻量领域分层架构 Mermaid 图集

> 本文用于补充《大型单体轻量领域分层架构 Code Style》中的架构图。  
> 所有图均使用 Mermaid 编写，可直接复制到支持 Mermaid 的 Markdown 编辑器、GitLab、GitHub、语雀、Typora、Obsidian 或文档平台中渲染。

> **规范效力说明**
>
> 本文所有图描述的是 `egon-cola-archetype-light`、`egon-cola-archetype-web`、`egon-cola-archetype-service`
> 三个骨架**实际落地并强制执行**的架构。
>
> 其中的分层依赖约束由 `egon-cola-component-bytecode-architecture-maven-plugin` 自动校验：
> 该插件绑定在 Maven 的 `verify` 阶段，并以 `unknownLayerPolicy=FAIL` 运行，任何越界依赖和未登记的包都会直接导致构建失败。
>
> 当本文与骨架代码不一致时，**以骨架为准**，并回头修正本文。
>
> 两条最容易记错的边：`infrastructure` 只依赖 `domain`（不依赖 `application`），全部端口接口都定义在 `domain`。

---

## 1. 总体分层依赖图

```mermaid
flowchart TD
    STARTER["starter<br/>启动装配层<br/>light 骨架中包名为 start"]
    ADAPTER["adapter<br/>入站适配层"]
    APPLICATION["application<br/>应用编排层"]
    DOMAIN["domain<br/>领域核心层<br/>持有全部端口接口"]
    INFRA["infrastructure<br/>基础设施层"]
    FACADE["facade<br/>对外契约层<br/>自包含 dto / enums / exceptions / utils"]
    COMMON["common<br/>通用基础层"]
    STARTER --> ADAPTER
    STARTER --> INFRA
    ADAPTER --> APPLICATION
    ADAPTER --> FACADE
    APPLICATION --> DOMAIN
    DOMAIN --> COMMON
    INFRA --> DOMAIN
    FACADE -. " 不依赖 common " .- FACADE
    COMMON -. " 不依赖其他模块 " .- COMMON
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
    INFRA -. " 禁止 " .-> APPLICATION
    INFRA -. " 禁止 " .-> ADAPTER
    INFRA -. " 禁止 " .-> FACADE
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
        subgraph ADAPTER_BIZ["adapter.{business}.* 领域优先分包"]
            CONTROLLER["controller<br/>HTTP 入站"]
            MQC["mq<br/>仅入站 Consumer"]
            RPC["rpc<br/>RPC Provider"]
            GRAPHQL["graphql<br/>Resolver"]
            FACADE_IMPL["facade.impl<br/>Facade 实现唯一位置"]
            DTO["dto<br/>入站请求对象"]
            VO["vo<br/>HTTP 响应对象"]
            CONVERTOR["convertor<br/>入站对象转换"]
        end
        HANDLER["handler<br/>异常处理 / 响应包装<br/>跨领域，留在分层根目录"]
        FILTER["filter<br/>Web Filter / TraceId / 上下文<br/>跨领域，留在分层根目录"]
    end

    APP["application.{business}.manage.*"]
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
        subgraph FACADE_BIZ["facade.{business} 领域优先分包，无 api 子包"]
            API["facade.user.UserFacade<br/>facade.teaching.CourseFacade"]
            DTO["dto<br/>CreateUserDTO.java / UserDetailDTO.java"]
            ENUMS["enums<br/>UserFacadeStatus.java"]
            EXCEPTIONS["exceptions<br/>UserFacadeException.java"]
            UTILS["utils<br/>UserFacadeAssert.java"]
        end
    end

    OUTER["外部系统 / 其他模块"] --> API
    IMPL["adapter.{business}.facade.impl<br/>实现唯一位置"] -.-> API
    API --> DTO
    API --> ENUMS
    API --> EXCEPTIONS
    API --> UTILS
    FACADE -. " 不写实现 " .- FACADE
    FACADE -. " 不依赖 common " .- FACADE
```

---

## 6. Application 业务编排图

```mermaid
flowchart TD
    ADAPTER["adapter"] --> MANAGE

    subgraph APPLICATION["application 应用编排层，领域优先分包"]
        MANAGE["user.manage.UserManage<br/>teaching.manage.CourseManage"]
        MANAGE_IMPL["user.manage.impl.UserManageImpl<br/>teaching.manage.impl.CourseManageImpl"]
        COMMAND["command / query<br/>用例入参"]
        RESULT["result<br/>用例出参"]
        VALIDATOR["validators<br/>应用级校验"]
        ASSEMBLER["assemblers<br/>对象装配"]
        CONVERTOR["convertor<br/>对象转换"]
    end

    MANAGE --> MANAGE_IMPL
    COMMAND --> MANAGE_IMPL
    MANAGE_IMPL --> VALIDATOR
    MANAGE_IMPL --> ASSEMBLER
    MANAGE_IMPL --> CONVERTOR
    MANAGE_IMPL --> RESULT
    MANAGE_IMPL --> DOMAIN["domain<br/>领域服务端口 / 仓储端口 / client 端口"]
    APPLICATION -. " 不定义任何端口接口 " .- APPLICATION
```

---

## 7. Domain 领域核心图

```mermaid
flowchart TD
    subgraph DOMAIN["domain 领域核心层，持有全部端口接口"]
        subgraph DOMAIN_BIZ["domain.{business}.* 领域优先分包"]
            ENTITIES["entities<br/>User.java / Course.java"]
            AGGREGATES["aggregates<br/>UserAggregate.java / CourseAggregate.java"]
            VOS["vos<br/>UserId.java / CourseCode.java"]
            SERVICE["service<br/>XxxDomainService.java<br/>XxxCacheService.java / XxxEventPublisher.java<br/>领域服务与技术端口"]
            SERVICE_IMPL["service.impl<br/>XxxDomainServiceImpl.java<br/>仅纯业务规则实现"]
            REPOS["repos<br/>XxxRepository.java<br/>仓储端口，只定义接口"]
            VALIDATORS["validators<br/>领域不变量校验"]
            ENUMS["enums<br/>领域状态 / 类型"]
            EXCEPTIONS["exceptions<br/>领域异常"]
        end
        CLIENT["domain.client.{external}<br/>EvaluationQueryPort.java<br/>出站端口，按外部系统分包"]
    end

    SERVICE --> SERVICE_IMPL
    SERVICE_IMPL --> ENTITIES
    SERVICE_IMPL --> AGGREGATES
    SERVICE_IMPL --> VOS
    SERVICE_IMPL --> VALIDATORS
    SERVICE_IMPL --> REPOS
    SERVICE_IMPL --> CLIENT
    ENTITIES --> ENUMS
    AGGREGATES --> ENUMS
    VALIDATORS --> EXCEPTIONS
    INFRA["infrastructure<br/>实现全部端口"] -.-> SERVICE
    INFRA -.-> REPOS
    INFRA -.-> CLIENT
```

---

## 8. Infrastructure 基础设施图

```mermaid
flowchart TD
    subgraph INFRA["infrastructure 基础设施层，只依赖 domain"]
        subgraph INFRA_BIZ["infrastructure.{business}.* 领域优先分包"]
            REPO["repo<br/>impl / jpa / po / converter"]
            MQ["mq<br/>仅出站 Publisher"]
            CACHE["cache<br/>Redis / 本地缓存实现"]
            SERVICE_IMPL["service.impl<br/>领域服务端口实现"]
            VALIDATORS["validators<br/>外部响应 / 技术适配校验"]
        end
        CLIENT_IMPL["client.{external}<br/>出站端口实现<br/>按外部系统分包"]
        AOP["aop<br/>基础设施切面<br/>跨领域，留在分层根目录"]
        CONFIG["config<br/>datasource / JPA / Redis / MQ 配置<br/>跨领域，留在分层根目录"]
    end

    DOMAIN_CLIENT["domain.client.{external}.*"] --> CLIENT_IMPL
    DOMAIN_REPO["domain.{business}.repos.*"] --> REPO
    DOMAIN_SERVICE["domain.{business}.service.*"] --> SERVICE_IMPL
    DOMAIN_SERVICE --> CACHE
    DOMAIN_SERVICE --> MQ
    REPO --> DB[(Database)]
    CLIENT_IMPL --> OUTER["外部 Facade / HTTP / RPC / SDK"]
    MQ --> BROKER["Kafka / RabbitMQ / RocketMQ"]
    CACHE --> REDIS["Redis / Local Cache"]
    CONFIG --> REPO
    AOP --> CLIENT_IMPL
    VALIDATORS --> CLIENT_IMPL
    INFRA -. " 禁止依赖 application / adapter / facade " .- INFRA
```

---

## 9. Repository 调用链路图

```mermaid
sequenceDiagram
    participant Adapter as adapter
    participant App as application.{business}.manage
    participant DomainRepo as domain.{business}.repos.XxxRepository
    participant RepoImpl as infrastructure.{business}.repo.impl.XxxRepositoryImpl
    participant Converter as infrastructure.{business}.repo.converter.XxxPOConverter
    participant JpaRepo as infrastructure.{business}.repo.jpa.XxxJpaRepository
    participant DB as Database
    Adapter ->> App: 调用业务用例
    App ->> DomainRepo: 调用仓储端口
    DomainRepo ->> RepoImpl: Spring 注入唯一实现
    RepoImpl ->> Converter: 领域对象 -> XxxPO
    RepoImpl ->> JpaRepo: 调用 JPA Repository
    JpaRepo ->> DB: 执行 ORM 查询
    DB -->> JpaRepo: 返回 XxxPO
    JpaRepo -->> RepoImpl: 返回 XxxPO
    RepoImpl ->> Converter: XxxPO -> 领域对象
    RepoImpl -->> DomainRepo: 返回领域对象
    DomainRepo -->> App: 返回领域对象
    App -->> Adapter: 返回应用结果
```

> 持久化只有 JPA 一条链路，不存在 MyBatis-Plus 分支。

---

## 10. JPA 仓储实现链路图

```mermaid
flowchart TD
    APP["application"] --> DOMAIN_REPO["domain.user.repos.UserRepository"]
    DOMAIN_REPO --> REPO_IMPL["infrastructure.user.repo.impl.UserRepositoryImpl"]
    REPO_IMPL --> CONVERTER["infrastructure.user.repo.converter.UserPOConverter"]
    REPO_IMPL --> JPA_REPO["infrastructure.user.repo.jpa.UserJpaRepository"]
    CONVERTER --> PO["infrastructure.user.repo.po.UserPO"]
    JPA_REPO --> PO
    JPA_REPO --> DB[(Database)]
    APP -. " 禁止直调 " .-> JPA_REPO
    APP -. " 禁止感知 PO " .-> PO
```

> 这是**唯一**的仓储实现链路：`application` -> `domain.{business}.repos` -> `infrastructure.{business}.repo.impl`
> -> `converter` + `jpa` -> `po`。

---

## 11. 外部 Client 防腐层图

```mermaid
flowchart LR
    APP["application.{business}.manage.*"] --> CLIENT["domain.client.evaluation.EvaluationQueryPort"]
    CLIENT --> CLIENT_IMPL["infrastructure.client.evaluation.DubboEvaluationQueryClient"]
    CLIENT_IMPL --> CONVERTOR["infrastructure.client.evaluation<br/>失败映射 / 校验"]
    CONVERTOR --> OUTER_FACADE["外部 Facade"]
    CONVERTOR --> HTTP["外部 HTTP API"]
    CONVERTOR --> RPC["外部 RPC / gRPC"]
    CONVERTOR --> SDK["第三方 SDK"]
    APP -. " 不感知外部协议 " .- APP
    CLIENT_IMPL -. " 隐藏外部系统细节 " .- CLIENT_IMPL
```

> 出站端口定义在 `domain.client.{external}`，实现唯一放在 `infrastructure.client.{external}`。
> 不存在 `application.client` 包，也不使用 `infrastructure.client.impl` 这种技术优先分包。

---

## 12. MQ 入站与出站隔离图

```mermaid
flowchart TD
    BROKER_IN["MQ Broker<br/>入站消息"] --> CONSUMER["adapter.{business}.mq.XxxConsumer<br/>仅入站消费"]
    CONSUMER --> APP["application.{business}.manage.*"]
    APP --> DOMAIN["domain"]
    APP --> EVENT_PORT["domain.{business}.service.XxxEventPublisher<br/>出站事件端口"]
    EVENT_PORT --> PRODUCER["infrastructure.{business}.mq.RabbitXxxEventPublisher<br/>仅出站发送"]
    PRODUCER --> BROKER_OUT["MQ Broker<br/>出站消息"]
    CONSUMER -. " 不发送 MQ " .- CONSUMER
    PRODUCER -. " 不消费 MQ " .- PRODUCER
```

---

## 13. Validator 分层职责图

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

## 14. 单体内两个领域示例图

```mermaid
flowchart TD
    subgraph PROJECT["student-management 单体工程"]
        subgraph USER["domain.user 领域"]
            USER_ENTITY["entities<br/>User / Role / Permission"]
            USER_SERVICE["service<br/>UserDomainService"]
            USER_REPO["repos<br/>UserRepository"]
        end

        subgraph TEACHING["domain.teaching 领域"]
            TEACHING_ENTITY["entities<br/>SchoolClass / Course"]
            TEACHING_SERVICE["service<br/>SchoolClassDomainService / CourseDomainService"]
            TEACHING_REPO["repos<br/>SchoolClassRepository / CourseRepository"]
        end

        APP["application<br/>跨领域业务编排"]
    end

    APP --> USER_SERVICE
    APP --> TEACHING_SERVICE
    USER_SERVICE --> USER_ENTITY
    TEACHING_SERVICE --> TEACHING_ENTITY
    USER_SERVICE --> USER_REPO
    TEACHING_SERVICE --> TEACHING_REPO
    USER -. " 领域之间不直接依赖 " .- TEACHING
```

---

## 15. 典型 HTTP 请求时序图

```mermaid
sequenceDiagram
    participant Client as Client
    participant Filter as adapter.filter.TraceIdFilter
    participant Controller as adapter.user.controller.UserController
    participant Convertor as adapter.user.convertor.UserAdapterConvertor
    participant Manage as application.user.manage.UserManage
    participant DomainService as domain.user.service.UserDomainService
    participant Repository as domain.user.repos.UserRepository
    participant RepoImpl as infrastructure.user.repo.impl.UserRepositoryImpl
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

## 16. 典型 RPC 请求时序图

```mermaid
sequenceDiagram
    participant Caller as 外部调用方
    participant Facade as facade.user.UserFacade
    participant Impl as adapter.user.facade.impl.UserFacadeImpl
    participant Convertor as adapter.user.convertor.UserAdapterConvertor
    participant Manage as application.user.manage.UserManage
    participant Domain as domain.user.service.UserDomainService
    participant Repository as domain.user.repos.UserRepository
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

## 17. 典型 MQ 入站时序图

```mermaid
sequenceDiagram
    participant Broker as MQ Broker
    participant Consumer as adapter.user.mq.UserImportedConsumer
    participant Convertor as adapter.user.convertor.UserAdapterConvertor
    participant Manage as application.user.manage.UserManage
    participant Domain as domain.user.service.UserDomainService
    participant Repo as domain.user.repos.UserRepository
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

## 18. 包结构关系图

```mermaid
flowchart TD
    ROOT["com.xxx.project"]
    ROOT --> STARTER["starter<br/>light 骨架为 start"]
    ROOT --> ADAPTER["adapter"]
    ROOT --> FACADE["facade"]
    ROOT --> APPLICATION["application"]
    ROOT --> DOMAIN["domain"]
    ROOT --> INFRA["infrastructure"]
    ROOT --> COMMON["common"]
    ADAPTER --> ADAPTER_USER["user.controller / user.rpc<br/>user.mq 仅入站 / user.facade.impl"]
    ADAPTER --> ADAPTER_TEACHING["teaching.controller / teaching.rpc<br/>teaching.mq 仅入站 / teaching.facade.impl"]
    ADAPTER --> ADAPTER_SHARED["handler / filter<br/>跨领域，留在分层根目录"]
    FACADE --> FACADE_USER["user<br/>UserFacade + dto / enums / exceptions / utils"]
    FACADE --> FACADE_TEACHING["teaching<br/>CourseFacade + dto / enums / exceptions / utils"]
    APPLICATION --> APP_USER["user.manage.impl<br/>user.command / query / result"]
    APPLICATION --> APP_TEACHING["teaching.manage.impl<br/>teaching.command / query / result"]
    DOMAIN --> DOMAIN_USER["user.entities / user.service<br/>user.repos"]
    DOMAIN --> DOMAIN_TEACHING["teaching.entities / teaching.service<br/>teaching.repos"]
    DOMAIN --> DOMAIN_CLIENT["client.evaluation<br/>出站端口，按外部系统分包"]
    INFRA --> INFRA_USER["user.repo.impl / user.repo.jpa<br/>user.service.impl / user.cache / user.mq 仅出站"]
    INFRA --> INFRA_TEACHING["teaching.repo.impl / teaching.repo.jpa<br/>teaching.service.impl / teaching.cache / teaching.mq 仅出站"]
    INFRA --> INFRA_SHARED["client.evaluation / aop / config<br/>跨领域，留在分层根目录"]
    COMMON --> COMMON_PKG["constants / utils / enums / exceptions"]
```

---

## 19. 架构边界总览图

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
    DOMAIN --> PORTS["domain 端口<br/>{business}.repos / {business}.service / client.{external}"]
    PORTS --> INFRA["infrastructure<br/>只依赖 domain"]
    INFRA --> DB
    INFRA --> CACHE
    INFRA --> MQ_OUT
    INFRA --> EXT
```
