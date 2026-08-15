# 三层架构 POJO 职责与对象模型设计

> 本文件是 `pojo-modeling.md` 的全中文审核镜像。编写 Spec 第 10 章和第 13 章的 Service 结构时使用，适用于 `three-layer-architecture.zh-CN.md` 定义的传统三层架构。以下名称表示语义职责，不是要求每一行都创建一个类的清单。

## 标准职责词汇

| 缩写/名称 | 全称 | 设计含义 | 主要用途 |
| --- | --- | --- | --- |
| POJO | Plain Old Java Object | 普通 Java 对象的总称，不代表分层，也不是必须使用的后缀 | 普通对象类别 |
| PO | Persistent Object | 与表、行或持久化记录对应的持久化表达 | DAO/Mapper 持久化边界 |
| DO | Data Object / Domain Object | 含义依团队而异，存在歧义 | 只有仓库已经明确其含义时才使用 |
| DTO | Data Transfer Object | 在层、模块、进程或服务之间传输的数据 | 不拥有持久化职责的边界传输 |
| VO | View Object | 为前端或展示层组织的数据 | API/页面展示输出 |
| BO | Business Object | Service 内部计算或编排使用的业务对象 | 只有中间业务计算具有独立语义时使用 |
| Entity | Entity | 当前三层规范中具有稳定身份的 ORM 持久化对象 | 承担持久化状态和生命周期；必须说明准确 ORM 含义 |
| DAO | Data Access Object | 数据库访问组件，不是数据载体 | 持久化访问行为 |
| Query / QO | Query Object | 查询条件对象 | 搜索和过滤输入 |
| Command / CO | Command Object | 修改意图对象 | 新增、更新、删除等改变状态的用例 |
| Request | Request Object | Controller 或 API 输入对象 | 传输校验和请求兼容边界 |
| Response | Response Object | Controller 或 API 输出对象 | 稳定的传输响应边界 |
| Form | Form Object | 表单提交对象 | 与 API Request 不同的 UI 表单绑定 |
| Param | Parameter Object | 方法或 API 参数组合 | 以一个内聚概念避免过长参数列表 |
| PageQuery | Page Query Object | 分页信息和查询条件 | 分页查询输入 |
| PageResult | Page Result Object | 分页数据和分页元信息 | 分页查询输出 |

当前规范不定义 Aggregate、Domain Service、Repository Port 或 DDD Value Object。不得为了让普通三层设计看起来像领域驱动设计而引入这些职责。

## 仓库优先的分类与放置规则

1. 提议名称前，检查现有后缀、`biz.domain` 子包、框架注解、序列化器、Mapper、持久化类型和公开契约。
2. 除非违反用户明确决定或造成已记录的正确性问题，否则沿用仓库已经一致使用的定义。
3. 仓库中的 `DO`、`VO` 或 `Entity` 有歧义时，必须在 Spec 中写明选定含义。实质性不兼容的命名变化属于重大设计决策。
4. 根据所有权和边界语义分类，不能因为两个类碰巧字段相同就认为职责相同。
5. 确有必要的数据载体直接放在 `biz.domain`，或放在其下符合仓库惯例的子包中；不得创建所有可能的子包。
6. DAO、Mapper 是带行为的访问组件，应放在 `biz.dao`，不能混入 POJO 清单。
7. 具体 Service 实现放在 `biz.service.impl`；数据对象分类不能制造另一层 Service。

## 类必要性检验

不得默认同时创建 `FooPO`、`FooDO`、`FooEntity`、`FooBO`、`FooDTO`、`FooVO`、`FooRequest` 和 `FooResponse`。

只有至少存在一个具体差异时，才能创建独立类型：

- 所有权或依赖方向不同；
- 公开兼容性或序列化形态不同；
- 校验、鉴权、隐私或字段暴露策略不同；
- 可变性、生命周期、身份、校验规则或状态转换不同；
- 持久化映射、懒加载、生成字段或数据库空值语义不同；
- 某个边界需要其他模型不应拥有的聚合、投影、反规范化、本地化或分页结构；
- 独立版本或变化节奏使复用不安全。

如果语义、生命周期、校验、暴露策略和依赖方向确实相同，应复用现有类型，并记录复用安全的理由。

不能为了减少类数量而把 PO 或 ORM Entity 直接作为公开 Request、Response、DTO 或 View Object。持久化注解、内部字段、懒加载关系和 Schema 演进不能泄漏到外部边界。

确需两个独立类型时，必须定义准确的转换负责人和字段映射。禁止没有语义边界的空转 Mapper 链和中间对象。

## Spec 必需证据

每个拟议对象必须记录：

- 准确类名、包/路径和选定职责；
- 所有者和生命周期；
- 消费者与跨越的边界；
- 字段、校验、空值/默认值语义、敏感数据处理和状态规则；
- 适用的持久化或协议映射；
- 为什么必须独立建类，或为什么可安全复用已有类型；
- 需要映射时的转换负责人；
- 对应需求编号。

数据跨越三个及以上对象职责时，Spec 必须提供对象流图或映射表。

## PO 与 ORM Entity 继承

PO 或 ORM Entity 允许继承，但不强制继承。只有仓库惯例和语义支持真正可替换的持久化子类型，或支持稳定的通用持久化基类时才采用。

通用持久化基类可以集中持久化对象已经共享的身份、审计时间、租户归属、乐观锁版本或其他生命周期机制。Spec 必须说明：

- `is-a` 或共同生命周期依据；
- 继承字段和校验/状态规则；
- ORM 映射、代理、懒加载、鉴别字段/表策略和迁移影响；
- 身份及 `equals`/`hashCode` 行为；
- 序列化和外部契约暴露；
- 测试影响与兼容性。

如果继承只是为了少写一些无关字段或工具方法，应拒绝继承。不存在可替换的持久化关系时，优先使用组合。

## 业务 Service 组合原则

`biz.service.impl` 中的具体类默认采用组合与委托，显式注入或组装 DAO、Policy、Strategy、Validator、Calculator、Gateway 和其他内聚协作者。

不得仅为复用代码创建业务 `BaseService`、多层 Service 继承树或以子类表达功能变化；这会耦合生命周期、隐藏状态、protected 扩展点和无关行为。

只有现有框架要求稳定扩展契约，或仓库已经存在合理的 Template Method、不变算法及狭窄变化钩子时，才允许 Service 继承。Spec 必须说明为什么组合不能更清晰地表达需求，以及如何保证可替换性、可测试性和生命周期安全。
