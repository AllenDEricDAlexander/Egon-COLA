# 时间语义、UTC 与转换边界

当受影响字段、查询、事件、计算、调度、持久化映射或前端依赖时间时读取本规范。只在已授权变更范围内应用；读取规范不授权修改数据库、历史数据、配置或业务代码。

## 先区分语义

| Semantics | Java 类型 | PostgreSQL 表示 | 传输规则 |
| --- | --- | --- | --- |
| Occurred instant：创建、支付、完成、过期、事件发生时刻 | `Instant`；边界的 `OffsetDateTime` 转为发生时刻 | `timestamptz(6)` | ISO 8601 UTC，结尾 `Z`，如 `2026-10-13T02:00:00Z` |
| Pure date：生日、业务日期、账期日期 | `LocalDate` | `date` | `YYYY-MM-DD`，不做时区偏移 |
| Local time：营业时间、本地钟表时间 | `LocalTime` | `time` | 本地时间；解析实际发生时刻时明确所属业务时区 |
| Zoned schedule：本地预约或周期执行规则 | `LocalDateTime` / `LocalTime` 加 IANA `ZoneId`；计算时用 `ZonedDateTime` | 本地日期/时间加地区时区 ID，按需保存周期规则 | 本地值加明确的地区时区 ID |
| Duration：经过时长、超时、TTL | `Duration` | 明确单位的数值或既有兼容表示 | 明确单位或约定 ISO duration，不做时区转换 |

预约解析后的发生时刻另按 Occurred instant 处理。未来或周期行为需要遵循地区规则时保留本地意图和地区时区。日期时间加 `+08:00` 偏移可定位时刻，但不能表达地区未来的夏令时规则。业务时区使用 `Asia/Shanghai`、`America/New_York` 等 IANA 名称。非 Java 任务记录等价运行时类型，不凭空增加 Java 文件。

## 存储与传输

- 新增发生时刻按 UTC instant 语义持久化；PostgreSQL `timestamptz` 归一到 UTC，但不保留原始偏移或 IANA 时区。只有真实需求需要时才另存地区时区。改变 DB 会话显示时区不会改变已存时刻。
- 新增/变更 REST、GraphQL、Event、RPC 中的发生时刻输出 ISO 8601 UTC `Z`。输入带明确偏移或 `Z` 后归一化；需要本地输入流程时，必须取得明确业务时区并在边界解析。禁止从服务器默认时区猜测，禁止将无偏移的本地日期时间作为发生时刻输出。纯日期、本地调度、时长保留自己的格式。
- 检查真实 MyBatis/JDBC/JSON 映射。pgJDBC 原生 `timestamptz` 对应 `OffsetDateTime`；业务模型使用 `Instant` 时须验证既有映射或获准的边界转换。不得假定 `setObject(Instant)` 可用，也不得在现有能力足够时另建 Handler。
- 持久化精度与真实列/Handler 一致，PostgreSQL 通常为微秒；明确传输精度和舍入/截断，往返保持同一时刻。禁止前端 `Date` 丢失亚毫秒精度后仍作为准确值或版本回传。

## 业务计算与前端展示

- 每条依赖时间的业务规则明确时区来源：门店、租户、应用显式配置或契约允许请求指定的时区，并定义本规则的优先级、回退及缺失/非法时区行为。禁止隐式使用 OS/JVM/DB 默认时区或浏览者时区作为业务权威。需求明确且已配置时允许固定全局业务时区。
- 部署授权范围允许时，JVM 和 DB 会话统一 UTC；日历计算仍显式使用 `ZoneId`。复用注入的 `Clock` 取得当前时刻，通常为 `Clock.systemUTC()`；区分经过时长 `Duration` 和日历运算。禁止按请求修改全局默认时区。
- 按日筛选、日/周/月报表先在业务时区计算本地起点和下一本地边界，再转为 instant，查询 `[start, end)`。下一日不能固定增加 24 小时，夏令时可能改变一天长度。SQL 分组使用同一显式业务时区。不额外持久化展示时间副本。
- 本地预约/调度输入必须明确夏令时不存在时间及重复时间的策略：拒绝或采用有依据的业务规则，不静默采用 Java 默认调整。相关场景还要明确周期执行行为和地区规则变化后未来时刻是否重算。
- 前端将 UTC 时刻按显式用户偏好或有说明的浏览器回退转换一次；报表/账务视图按权威业务时区展示并标注。格式化不能改变存储值或后端业务日期。纯日期提交日期字符串，不转午夜时间戳；本地调度提交携带/解析业务时区。禁止解析带偏移值后再手工加减固定小时数。

## 既有契约与范围

先检查当前类型、物理列、序列化和数据来源，再决定变更。既有约定 UTC 的 `LocalDateTime` / `timestamp without time zone`（如逻辑删除字段）归类为 `Legacy UTC`，不能作为新增字段默认方案。记录强制 UTC 解释、读写转换、来源时区证据和兼容策略。不能仅凭本规范宣称当前模板或模型全部符合要求。

未受影响的历史契约在本任务保持原样。已授权变更确需纠正列或历史数据时，按仓库机制新增下一迁移版本；不得编辑既有迁移、猜测未知历史时区、自动整体平移数据，也不得把 skill 更新当作迁移授权。来源不明或与有效 Spec 冲突时阻断并取得明确决策。

## 契约表与阻断校验

新 Spec Version 9 保留 §6.3，新 Plan Version 5 保留 §4.9。受影响时间语义/边界写 `Time contract: Required`，否则写 `Time contract: Not applicable` 并给出准确范围证据。Plan 消费有效 Spec 的时间决策；重大决策缺失时退回 Spec，不能自行补造。

语义及边界相同的字段可合为一条 `TIME-NNN`，列明准确字段/符号；策略不同必须拆行。中英文产物均保留以下英文机读值：

- Semantics：`Occurred instant`、`Pure date`、`Local time`、`Zoned schedule`、`Duration`。
- Storage policy：`UTC instant`、`Pure date`、`Local time`、`Local + zone`、`Duration`、`Legacy UTC`、`Not persisted`。
- Wire policy：`UTC Z`、`Pure date`、`Local`、`Local + zone`、`Duration`、`Internal`（无受影响传输边界）。
- 不相关的业务/展示时区、精度/Clock 或兼容项可写 `Not applicable: <原因>`；单独 `N/A` 不足。地区调度必须写地区时区来源。`Legacy UTC` 在 Compatibility 明确既有契约及转换。

`MC-TIME-001` 必须提供受影响边界的语义分类、真实持久化/驱动/JSON 往返、显式业务时区计算、前端展示/提交、精度/Clock、历史兼容证据。Spec Verification 在 §14 定义具体 `TEST-*`；Plan Verification 写所属 `Step N`、§8 中测试/命令及逐文件义务。Execute 记录实际结果和必需运行验证缺口；不得自动启动应用、数据库或浏览器。

仅选择适用检查：UTC/上海/纽约环境同一时刻、`Z` 输出与带偏移输入、缺少时区拒绝、纯日期稳定、业务午夜/月界、夏令时跳跃/重复与日历日区间、精度往返、历史 UTC 映射。Mock/H2 不证明 PostgreSQL 映射。结构校验器只检查表格和策略冲突，不证明文字、迁移或运行行为正确；人工证据仍是阻断门禁。

来源：[PostgreSQL 日期时间语义](https://www.postgresql.org/docs/current/datatype-datetime.html)、[pgJDBC Java 时间映射](https://jdbc.postgresql.org/documentation/query/#using-java-8-date-and-time-classes)。
