# 传统三层 Java 分包设计

> 本文件是 `three-layer-architecture.md` 的全中文审核镜像。当前 skill 只规范这一种 Java 分包形式，DDD 和 COLA 分包暂缓。

## 适用门禁

只有受影响模块已经采用传统三层结构，或用户明确选择该结构时才使用。不得静默把已有 DDD、COLA、六边形或自定义架构迁移成三层结构。如果当前仓库不是三层架构且任务需要改变分包，应将其视为重大决策并询问用户。

## 目标分包树

```text
<base-package>/biz
├── controller
├── service
│   └── impl
├── dao
├── config
├── utils
└── domain
    └── <符合仓库惯例的 POJO 文件或确有必要的职责子包>
```

`service.impl` 必须位于 `service` 下，绝不能把 `impl` 与 `service` 平级。`domain` 可以保持扁平，也可以使用仓库已有的 `po`、`dto`、`vo`、`query` 等职责子包。只能创建当前改动确有必要的包与类，绝不能默认按每个 POJO 术语生成一个包。

## 包职责

| 包 | 职责 | 禁止承担的职责 |
| --- | --- | --- |
| `biz.controller` | HTTP/API 入口、请求绑定、传输校验、认证上下文、响应与错误映射 | 数据库访问或业务流程 |
| `biz.service` | Controller 和其他调用者依赖的稳定业务用例接口 | Controller 或 DAO 的框架实现细节 |
| `biz.service.impl` | Service 实现、业务规则、流程编排、事务边界，以及 DAO/协作者组合 | 仅为复用工具方法建立继承树 |
| `biz.dao` | 数据库访问组件、Mapper/Repository 查询和持久化操作 | 业务决策或传输响应构造 |
| `biz.config` | 模块配置、Bean 组装、属性、序列化和技术接线 | 业务流程 |
| `biz.utils` | 无状态、与业务流程无关且无法复用现有公共工具的小型工具 | 有状态编排或业务规则垃圾桶 |
| `biz.domain` | 按职责分类的 POJO 数据载体 | 擅自引入 DDD 聚合、领域服务或值对象 |

## 依赖方向

```text
controller -> service
service.impl -> service, dao, domain
dao -> domain
controller -> domain
config -> 模块装配
utils -> 不反向依赖 controller/service/dao 流程
```

- Controller 依赖 Service 接口，不直接依赖 `service.impl` 或 DAO。
- Controller 只能引用接口契约需要的传输、查询和展示对象，不能暴露 PO/ORM Entity。
- `service.impl` 中的类实现对应 Service 接口，并承担通常的事务边界。
- DAO 不得调用 Controller 或 Service，也不得决定业务策略。
- Config 可以组装实现，但不能成为业务代码使用的 Service Locator。
- Utils 保持无状态和内聚；创建新工具前优先复用现有项目工具。

## 对象放置

在 `biz.domain` 下增加对象前，必须执行 `pojo-modeling.zh-CN.md`。

- 持久化对象使用 `po`、`entity` 或仓库已有术语；没有真实边界时不能创建同义持久化模型。
- 只有独立语义确实需要时，才使用 DTO、VO、BO、Query、Command、Request、Response、Form、Param、PageQuery 或 PageResult。
- 当前结构允许 Request/Response 放在 `biz.domain`；如果仓库一贯放在 `controller` 下，则沿用仓库惯例。
- DAO 是访问组件，绝不能列入 POJO 对象清单。
- 当前结构不得引入 Aggregate、Domain Service、Repository Port 或 DDD Value Object。

## Spec 必需证据

Spec 必须展示现有树和目标树、精确文件/符号、包职责、依赖方向、Service 接口与实现的对应关系、Controller 消费者、DAO 访问路径、事务归属、POJO 职责和测试。任何偏离本树的设计都必须引用现有仓库证据或用户明确决定。
