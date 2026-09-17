# 传统三层 Java 分包设计

> 本文件是 `references/three-layer-architecture.md` 的全中文审核镜像，只规范传统分层形态。本 skill 也允许仓库准确选中的 `egon-cola-archetypes` COLA 结构，使用 `references/java-spring-egon-coding-standards.zh-CN.md` 选择形态。绝不能把本 `biz.*` Tree 混入 Archetype COLA 项目。

## 适用门禁

只有受影响模块已经采用传统三层结构，或用户明确选择该结构时才使用。不得静默把已有 DDD、COLA、六边形或自定义架构迁移成三层结构。如果当前仓库不是三层架构且任务需要改变分包，应将其视为重大决策并询问用户。

## 目标分包树

```text
<base-package>/biz
├── controller
├── service
│   └── impl
├── repository
├── dao
├── config
├── utils
└── domain
    └── <符合仓库惯例的 POJO 文件或确有必要的职责子包>
```

`service.impl` 必须位于 `service` 下，绝不能把 `impl` 与 `service` 平级。`repository` 是 Service 与持久化之间的防腐层。
`dao` 只放 Mapper 接口和 XML。`domain` 可以保持扁平，也可以使用仓库已有的 `po`、`dto`、`vo`、`query`
等职责子包。只能创建当前改动确有必要的包与类，绝不能默认按每个 POJO 术语生成一个包。

## 包职责

| 包                  | 职责                                                                                         | 禁止承担的职责                                   |
|--------------------|--------------------------------------------------------------------------------------------|-------------------------------------------|
| `biz.controller`   | HTTP/API 入口、参数绑定、传输校验、认证上下文、结果与错误映射                                                        | 数据库访问、Mapper 调用或业务流程                      |
| `biz.service`      | Controller 和其他调用者依赖的稳定业务用例接口；只暴露领域类型，不含 PO 泛型                                              | Controller、Repository 或 Mapper 的框架实现细节    |
| `biz.service.impl` | Service 实现、业务规则、流程编排、事务边界，以及 Repository/协作者组合                                              | 直接使用 Mapper/DAO、泄漏 PO，或仅为复用工具方法建立继承树      |
| `biz.repository`   | 防腐层：把 Service 的 Query/Command/BO/DTO/`*Result` 转成 `EgonModel` PO，承担受守卫的持久化 Command，并暴露具名查询 | 业务流程、HTTP 映射，或把 Mapper/XML/PO 泄漏给 Service |
| `biz.dao`          | MyBatis `*Mapper` 接口与 XML；只写 SQL                                                           | 业务决策、防腐转换或传输结果构造                          |
| `biz.config`       | 模块配置、Bean 组装、属性、序列化和技术接线                                                                   | 业务流程                                      |
| `biz.utils`        | 无状态、与业务流程无关且无法复用现有公共工具的小型工具                                                                | 有状态编排或业务规则垃圾桶                             |
| `biz.domain`       | 按职责分类的 POJO 数据载体                                                                           | 擅自引入 DDD 聚合、领域服务或值对象                      |

## 依赖方向

```text
controller -> service
service.impl -> service, repository, domain
repository -> dao, domain
dao -> domain
controller -> domain
config -> 模块装配
utils -> 不反向依赖 controller/service/repository/dao 流程
```

- Controller 依赖 Service 接口，不直接依赖 `service.impl`、Repository 或 DAO/Mapper。
- Controller 只能引用接口契约需要的 Query、Command、展示对象和 `*Result`，不能暴露 PO/ORM Entity。
- `service.impl` 中的类实现对应 Service 接口，承担通常的事务边界，并依赖不含 PO 泛型的 Repository 类型。
- Repository 不得调用 Controller 或 Service，也不得决定业务策略。
- DAO/Mapper 不得调用 Controller、Service 或 Repository，也不得决定业务策略。
- Config 可以组装实现，但不能成为业务代码使用的 Service Locator。
- Utils 保持无状态和内聚；创建新工具前优先复用现有项目工具。

## Repository 防腐层与 egon-mp-ext

使用当前 `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 的 Repository 契约。不得自研
CRUD Helper、Query Wrapper 门面或 `*Gateway` 持久化适配器。

- Service 面对的是 `biz.repository` 中的具名 `*Repository` 接口（仓库已拆 api/impl 时按现有拆分）。方法入参/出参是领域
  Query/Command/BO/DTO/`*Result`，不是 `EgonModel` PO。
- 实现类继承 `EgonColaRepository<M extends EgonColaMapper<T>, T extends EgonModel<T>>`，使用 Qualifier 的 Lombok 构造注入，并提供
  mapper / `modelValidationUtils` / `tenantIdProvider` / properties getter。
- Command 使用 `save`、带版本的 `updateById` / `removeById` 和受守卫批量；调用方检查影响行数。影响 0 行不是成功。
- Query 使用具名 Mapper XML。每个 Mapper 提供 `selectActiveById`、`selectActiveByIds`、`deleteVersionedById`。禁止
  ActiveRecord、QueryChain、`lambdaQuery`、通用 Query Wrapper 和 `.last()` SQL。
- 自定义 UPDATE 在 WHERE 中绑定 `#{MP_OPTLOCK_VERSION_ORIGINAL}`，并带上租户、活跃行和预期业务状态。id、租户、创建元数据和
  version 必须来自调用方或同一事务中加载的行。
- PO 留在 Repository 之后。PO ↔ 领域转换由 Repository 实现内部的 MapStruct/`BaseConverter` 负责。

## 对象放置

在 `biz.domain` 下增加对象前，必须执行 `references/pojo-modeling.zh-CN.md`。

- 持久化对象使用 `po`、`entity` 或仓库已有术语；没有真实边界时不能创建同义持久化模型。
- 只有独立语义确实需要时，才使用 DTO、VO、BO、Query、Command、Event、Result、PageQuery 或 PageResult。禁止新增含糊的 `Data`、
  `Info`、`Param` 或 `Bean` 载体。
- 入参没有额外的 `*Request` 后缀。出参协议对象使用 `*Result`（展示形态使用 `*VO`），不使用 `*Response`。
- DAO/Mapper 和 `*Repository` 是访问/防腐组件，绝不能列入 POJO 对象清单。
- 当前结构使用 MP-ext 的 `*Repository` 防腐类，不得引入 Aggregate、Domain Service、DDD Repository Port 或 DDD Value Object。

## Spec 必需证据

Spec 必须展示现有树和目标树、精确文件/符号、包职责、依赖方向、Service 接口与实现的对应关系、Controller 消费者、Repository
防腐方法、Mapper/DAO SQL 路径、事务归属、POJO 职责和测试。任何偏离本树的设计都必须引用现有仓库证据或用户明确决定。
