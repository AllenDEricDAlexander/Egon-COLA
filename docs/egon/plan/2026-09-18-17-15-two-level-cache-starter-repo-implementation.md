# 二级缓存 Starter 与 Repository 增强：文件级实现计划

| Field              | Value                                                                                                                                                                                                                                                                                                                                                                                                     |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document           | `docs/egon/plan/2026-09-18-17-15-two-level-cache-starter-repo-implementation.md`                                                                                                                                                                                                                                                                                                                          |
| Template Version   | `4`                                                                                                                                                                                                                                                                                                                                                                                                       |
| Status             | `Review`                                                                                                                                                                                                                                                                                                                                                                                                  |
| Created            | `2026-09-18 17:15 CST`                                                                                                                                                                                                                                                                                                                                                                                    |
| Updated            | `2026-09-18 17:15 CST`                                                                                                                                                                                                                                                                                                                                                                                    |
| Owner              | `mario`                                                                                                                                                                                                                                                                                                                                                                                                   |
| Repository         | `Egon-COLA`                                                                                                                                                                                                                                                                                                                                                                                               |
| Scope              | `egon-cola-components（父 pom 依赖治理、common-core 端口、新 cache starter、mp-ext repo 增强、BOM 导出）+ egon-cola-archetypes/source-projects 七根适配与 definitions 再生成`                                                                                                                                                                                                                                                       |
| Source Requirement | `用户原话："在 egon-cola-components/egon-cola-component-common 里给我加一个 基于 spring cache +redission + guava cache 设计二级缓存 的 starter，spring cache 的 update 和 失效机制，需要基于 redis 的 event 来实现，通用 event ，而不是每个类型的 cache 都写一个 listener ，并增强给 egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter 的 repo 层。依赖管理要在父pom中管理，不要局部引入,mp-ext增强后，archetypes中的也要进行更新适配。 先给我方案。"；随后用户明示"spec审核通过，开始写plan吧"` |
| Baseline Revision  | `dd7389c1e (main；撰写计划时工作区仅含未跟踪文件 docs/egon/spec/2026-09-18-15-37-two-level-cache-redis-event-starter-repo-enhancement.md，执行阶段不得触碰或提交该 spec 文件本身)`                                                                                                                                                                                                                                                         |
| Implements Spec    | `[二级缓存 Starter 与 Repository 层增强设计](../spec/2026-09-18-15-37-two-level-cache-redis-event-starter-repo-enhancement.md)`                                                                                                                                                                                                                                                                                     |
| Spec Status        | `Accepted`                                                                                                                                                                                                                                                                                                                                                                                                |
| Spec Revision      | `Template Version 7，Updated 2026-09-18 17:15 CST，Status Accepted（用户 2026-09-18 会话内明示"spec审核通过"）`                                                                                                                                                                                                                                                                                                          |
| Effective Specs    | `[二级缓存 Starter 与 Repository 层增强设计](../spec/2026-09-18-15-37-two-level-cache-redis-event-starter-repo-enhancement.md)`（唯一有效设计文档；其 Amends/Depends On 所指 09-14、09-12、08-25-19-09、08-23-16-43、09-07 各 Spec 的冻结条款在本计划中以主 Spec 转述为准，不另行展开其需求集）                                                                                                                                                                  |
| Depends On Plans   | `None`                                                                                                                                                                                                                                                                                                                                                                                                    |
| Supersedes         | `None`                                                                                                                                                                                                                                                                                                                                                                                                    |
| Superseded By      | `None`                                                                                                                                                                                                                                                                                                                                                                                                    |
| Related Plans      | `None`                                                                                                                                                                                                                                                                                                                                                                                                    |

## 1. Summary

本计划将已获批准的 Spec（`docs/egon/spec/2026-09-18-15-37-two-level-cache-redis-event-starter-repo-enhancement.md`，
`Accepted`）翻译为 11 个按依赖序排列、每步一个语义提交的实现路径。主线：Step 1 收敛 `egon-cola-components/pom.xml` 依赖治理（补管
plain `org.redisson:redisson`、删 access-guard 与 dtp 两处局部版本）；Step 2 在 common-core 落纯 JDK 端口
`EgonColaCachePort`；Step 3-6 自底向上构建 `egon-cola-component-common-cache-spring-boot-starter`（模块骨架与配置面 →
事件契约与编解码 → Guava L1 + `RMapCache` L2 两级内核 → 通用监听器 + 端口实现 + 自动配置装配）；Step 7 增强
`EgonColaRepository`（15 个 final 写方法提交后失效登记、`getByCache`/`listByCache`、两个 protected 挂点）并原子修订架构围栏测试；Step
8 BOM 导出；Step 9-11 按家族适配 7 个 archetype 源工程根（pom 引用与既有 mp-ext 版本违例清理、28 个 `application*.yml`
键块、代表 Repository 注入示例、README、definitions 再生成），最后以根全量回归与三个校验脚本收口。全部有效需求 `REQ-001`~
`REQ-018` 及外部不变量 `REQ-025` 均可追溯到 Step 与验证门；测试先行、Testcontainers Redis 双节点夹具与机器围栏（`TEST-017`/
`TEST-018`/`TEST-021`/`TEST-022`）保证交付物可证。执行边界：默认 `enabled=false` 零影响，不改任何关系 schema、interceptor 链或
`EgonColaIRepository` 契约面。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- 路径：`docs/egon/spec/2026-09-18-15-37-two-level-cache-redis-event-starter-repo-enhancement.md`（Template Version `7`
  ，Status `Accepted`，Updated `2026-09-18 17:15 CST`，Baseline `dd7389c1e`）。
- 批准证据：用户 2026-09-18 会话内明示"spec审核通过，开始写plan吧"；Spec §20.6 verdict `PASS — Ready for user review`。
- 源需求：§1 头部 `Source Requirement` 原话与四项已拍板决策（自建 TwoLevelCacheManager；写路径透明失效 + 声明式读；archetype
  依赖+配置骨架；三项防护全含），Spec `EVD-012`。
- 有效需求集：Spec §4 表 `REQ-001`~`REQ-018`；`REQ-025` 为 Depends On 前置 Spec `2026-08-25-19-09` 的事务提交后副作用不变量（Spec
  §19 编号说明行），本计划将其作为 Step 7 的强约束来源，不新增语义。

### 2.2 Relationships resolved

| Document                                                                                                               | Status                                    | Treatment in this Plan                                                         |
|------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|--------------------------------------------------------------------------------|
| 主 Spec（上表链接）                                                                                                           | `Accepted`                                | 唯一有效设计；§8.2 目标树 = 文件清单权威；§9 八契约 + §10 模型 + §14 24 测试用例全部编译进 Steps              |
| `2026-09-14-22-05-mp-sharding-jdbc-starter-unification.md`                                                             | `Accepted`（被主 Spec Amends §15/§3.3 两行）    | 缓存/Redis 边界行被主 Spec 取代；其 Repository 骨架与 @Order 链不变量在本计划 Step 7 验证门中保持          |
| `2026-09-12-21-11-mybatis-repository-cqrs-postgresql-evolution.md`                                                     | `Accepted`（被主 Spec Amends §3.2 carve-out） | 缓存失效信号不入业务事件总线；Step 6 的失效通道为独立 `RTopic`                                        |
| `2026-08-25-19-09-archetype-mybatis-plus-unification.md`                                                               | `Review`                                  | 其 `REQ-025`（after-commit 不变量）被主 Spec `REQ-005` 引用并落实于 Step 7；不引入该 Spec 其余未冻结需求 |
| `2026-08-23-16-43-open-source-archetype-family.md` / `2026-09-07-16-52-archetype-native-open-dependency-governance.md` | `Review` / `Accepted`                     | 依赖所有权与 BOM 导出机制在 Step 8-11 以脚本实测结果为准（见 PC-002/PC-003）                          |

无未治理冲突：主 Spec `Accepted` 且晚于全部关联文档，Amends 均为其显式命名条款。

### 2.3 Repository drift detected during planning

基线核验发现一处 Spec 写作时不可见的既有红门：`python3 scripts/check-archetype-dependency-ownership.py` 在 `dd7389c1e`
上即失败，输出 7 条
`egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter must use its Components BOM version`（7
根各一处，mp-ext 引用带 `<version>${egon-cola.version}</version>`，实测于本会话）。该违例恰好落在主 Spec `REQ-014`（"依赖管理要在父
pom 中管理，不要局部引入"）与 `REQ-015` 验收行（"ownership 脚本对 7 根通过"）的治理面上，修复为删除版本元素（BOM 已导出 mp-ext
且 archetypes 父 pom 唯一 import 该 BOM，解析版本同值不变）。处理方式记入 PC-002，落点在 Step 9-11 与 cache starter
引用同一提交内。除此之外，`EgonColaRepository.java` 行号、components 父 pom 版本属性、BOM 导出先例、28 yml/7 根/代表 repo
清单与主 Spec `EVD-*` 逐一对得上，无其他漂移。

## 3. Effective Requirements and Acceptance

有效需求集为主 Spec `## 4. Requirements and Acceptance Criteria` 全部出现的需求编号：`REQ-001`~`REQ-018` 与 §4
正文内引用的外部不变量 `REQ-025`（来自 Depends On 前置 Spec `2026-08-25-19-09-archetype-mybatis-plus-unification.md`，主
Spec §4 的 `REQ-005` 行与 §15 编号说明均确认其为"保持型"约束而非新增需求）。

| Source ID | Requirement statement（摘要，全文见主 Spec §4）                                                                                                                                            | Acceptance（主 Spec §4 验收列）                                                       | Implementation impact                                        |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|--------------------------------------------------------------|
| `REQ-001` | common 聚合下新增 `egon-cola-component-common-cache-spring-boot-starter`（包 `top.egon.cola.component.common.cache`，前缀 `egon.cola.component.cache`，`enabled` 默认 false），可被根 reactor 构建    | 根 `./mvnw -B -ntp clean test` 通过；缺省配置下 `ApplicationContextRunner` 断言缓存 bean 不存在 | Step 3（模块骨架/配置面）、Step 6（装配开关矩阵）、Step 11（全仓回归）                |
| `REQ-002` | L1 Guava（容量 + 逐条抖动 TTL）、L2 Redisson `RMapCache`（逐条 TTL）；自实现 `EgonColaTwoLevelCache`/`EgonColaTwoLevelCacheManager` 并暴露 Spring Cache SPI                                           | L2 命中回填 L1；`@Cacheable` 可用同一 `CacheManager`；集成测试断言两级命中                          | Step 5（内核与管理器）                                               |
| `REQ-003` | 更新/失效经单条 `RTopic` 通用事件驱动；每节点恰一个订阅与一个监听器，按 `cacheName` 分发；禁止按类型写 listener                                                                                                          | 节点 A 失效后节点 B 的 L1 清除；全模块仅一处 `addListener`（集成测试 + 静态扫描）                          | Step 4（信封）、Step 5（发布点）、Step 6（单监听器）                          |
| `REQ-004` | 唯一事件 record `EgonColaCacheChangedEvent(schemaVersion,eventId,originNodeId,occurredAt,cacheName,operation,keys)`，操作集 `{PUT,EVICT,PREFIX_EVICT}`；自回声跳过                              | 编解码往返通过；自回声不重复应用失效                                                              | Step 4                                                       |
| `REQ-005` | 全部受控写方法成功后经端口登记"事务提交后失效"：提交后双级失效 + 发布 + 延迟二次失效；回滚零副作用（保持 `REQ-025`）                                                                                                               | 提交用例一次失效 + 一次发布 + 一次延迟；回滚用例零交互                                                  | Step 6（端口缓冲）、Step 7（15 个写方法登记）                               |
| `REQ-006` | 新增 `public final getByCache(Serializable)` 与 `listByCache(Collection<? extends Serializable>)`，复用 `selectActiveById`/`selectActiveByIds` 与 `validateLoaded*` 语义，业务 Repository 零注解 | miss 回源结果与 `getById`/`listByIds` 一致；hit 时数据库零访问                                 | Step 7                                                       |
| `REQ-007` | 空值缓存：loader 返回 null 写哨兵 `EgonColaCacheNullValueBO`，命中哨兵直接 null 不回源；`ttl.null-expire` 可配                                                                                           | 连续两次 miss 查询 loader 恰一次                                                         | Step 4（哨兵）、Step 5（缓存实现）                                      |
| `REQ-008` | TTL 抖动：L1/L2 逐条过期时间在基准上叠加随机偏移，ratio ∈ [0, 0.5]（TEST-008 固定为 `[expire, expire+jitter]` 单侧非负区间，见 PC-004）                                                                            | 100 个 key 的 TTL 离散度落在期望区间                                                       | Step 5                                                       |
| `REQ-009` | 互斥回源：双级 miss 以 `RLock.tryLock(wait-time)` 串行化，获锁先 recheck；超时降级直读且本次不写缓存                                                                                                           | 并发 8 线程 loader 恰一次；超时线程仍成功返回                                                    | Step 5                                                       |
| `REQ-010` | `EgonColaCachePort`（get/getAll/registerEvictionAfterCommit，纯 JDK 签名）定义在 common-core；mp-ext 主源码零 import `top.egon.cola.component.common.cache`                                     | 静态扫描围栏通过；最小 classpath 负例测试正常读写                                                  | Step 2（端口）、Step 7（围栏修订）                                      |
| `REQ-011` | 零影响开关：端口 bean 缺席时 `getByCache`≡`getById`、`listByCache`≡`listByIds`，写方法零 Redis 交互                                                                                                  | 无端口装配行为与现状一致；Redis 交互计数为 0                                                      | Step 7（null 端口直通）                                            |
| `REQ-012` | 租户隔离：键形状 `tenantId:id`；exact key 与 glob 必须以当前 `requireTenantId()` 开头；合法 glob 仅 `tenantId:*`；违规抛 `CACHE_KEY_TENANT_MISMATCH`/`CACHE_GLOB_PATTERN_FORBIDDEN`                        | 跨租户、裸 `*`、其它形状 glob 均抛对应码                                                       | Step 4（KeyGuard）、Step 6（端口守卫）、Step 7（键构造）                    |
| `REQ-013` | 一致性收敛：事件 at-most-once；双写者复活竞态由提交后失效 + 延迟二次失效 + 逐条 TTL 上界收敛，不一致窗口 ≤ min(TTL, 二次延迟)                                                                                                 | 模拟丢事件节点在 TTL 前经二次失效收敛                                                           | Step 5（TTL 上界）、Step 6（延迟第二拍）                                 |
| `REQ-014` | 组件父 pom dependencyManagement 增 plain `org.redisson:redisson`；access-guard 与 dtp 两处局部 `<version>` 移除；BOM 导出新 starter 与 redisson；组件树零新增局部版本                                         | 两 pom 修改后构建通过；`${redisson.version}` 局部声明清零；BOM 可解析                              | Step 1（父 pom 收敛）、Step 3/8（starter 与 BOM 引用）                  |
| `REQ-015` | 7 个 source-project 根按其既有 top.egon 管理方式引入 cache starter（引用处无内联版本），过 ownership 脚本                                                                                                   | 脚本对 7 根通过；生成工程依赖树含 cache starter                                                | Step 9（light 家族）、Step 10（service 家族 + agent）、Step 11（web 家族） |
| `REQ-016` | 28 个 `application*.yml` 追加结构一致的 `egon.cola.component.cache` 键块；模板 Repository 注入示例与 README 同步                                                                                      | 键集合比对通过；模板 repo 编译装配通过；README 命令可执行                                             | Step 9~11                                                    |
| `REQ-017` | 机器围栏保持或显式修订后绿色：架构测试（接口 57 钉数不变、实现公有方法 = 上游 ∪ 缓存读白名单）、链序围栏、`./scripts/generate_archetypes.sh check`、全仓 `clean test`                                                                | 全部命令通过且围栏修订仅白名单增量                                                               | Step 7（围栏）、Step 9~11（生成与校验）                                  |
| `REQ-018` | 降级可观测：L2/Redis 读写或反序列化失败不抛错；记录稳定错误码（`CACHE_REDISSON_CLIENT_MISSING`、`CACHE_EVENT_DESERIALIZE_FAILED`、`CACHE_L2_OPERATION_FAILED`）并回源                                              | 断 Redis 用例读回源成功、写提交成功、日志含码                                                      | Step 4（码常量）、Step 5（吞并记码）、Step 6（fail-fast 与监听器容错）            |
| `REQ-025` | 外部不变量（Depends On 08-25-19-09）：事务提交后才允许产生提交后副作用，回滚零副作用                                                                                                                             | 主 Spec §4 `REQ-005` 行验收即其体现                                                     | Step 6（afterCommit 登记）、Step 7（写路径仅登记不立即执行）                   |

## 4. Implementation Strategy and Dependency Order

### 4.1 Strategy overview

按主 Spec §16 发布顺序构建单一依赖路径：先收敛组件树版本治理（Step 1，纯 pom，解锁新 starter 的依赖位点），再发布稳定端口契约（Step
2，common-core 纯 JDK 接口），然后自底向上建设 cache starter（Step 3 模块与配置面 → Step 4 事件契约与编解码 → Step 5
两级内核 → Step 6 监听器/端口实现/自动装配），starter 全绿后才增强 mp-ext（Step 7，实现与围栏同一提交原子收口），经 BOM
导出（Step 8）后按家族分批适配 7 个 archetype 根（Step 9 light 家族 → Step 10 service 家族 + agent → Step 11 web
家族与全仓回归）。每一步都以可独立编译、可独立验证的语义提交收口；任何一步失败回滚到该步起始点，不影响已合入步骤。

### 4.2 Why this sequence works

- 依赖方向不可逆：cache starter 编译依赖 common-core（端口）与父 pom 治理后的 redisson 版本位点；mp-ext 运行期依赖 starter
  提供的端口 bean 但编译期只认 common-core；archetype 根同时消费三者。故顺序只能是 治理 → 契约 → starter → mp-ext → BOM →
  archetype。
- starter 内部：Properties 先于一切装配；事件信封/编解码/哨兵是纯数据结构，先于依赖它们的两级内核；内核（Manager/Cache）先于监听器与端口实现（二者调用内核）；
  `AutoConfiguration` 与 `AutoConfiguration.imports` 最后接入，使测试矩阵（TEST-020）一次成型。
- mp-ext 的实现方法与架构围栏必须同一提交：`EgonColaRepositoryArchitectureTest` 的"实现公有方法集精确相等"会立即拒绝
  `getByCache`/`listByCache`，白名单增量与实现分离会造成红门中间态。
- archetype 分三步按家族执行：每家族一个提交，`./scripts/generate_archetypes.sh generate` 与 definitions
  再生产物同提交，避免生成物与源漂移跨提交残留。

### 4.3 Test strategy

__SKIP__ test-first：Step 2（契约测试先 RED 于端口类缺失）、Step 3（Properties 绑定测试）、Step 4（信封守卫/往返测试）、Step
5（管理器与两级缓存集成测试，Testcontainers 夹具复制 access-guard `RedissonStoreIntegrationTest` 模式）、Step
6（监听器、端口、装配矩阵测试）、Step 7（`EgonColaRepositoryCacheEnhancementTest` 以 mock 端口 + `TestBusinessRepository`
夹具模式断言登记/直通/键形状）。Step 1、Step 8 为纯 pom 治理，Step 9~11 为配置/pom/文档类改动，test-first 不适用（各 Step
内有证据化说明），验证门是仓库脚本、`dependency:tree` 与全仓构建。Redis 相关测试统一用 Testcontainers `GenericContainer`
（redis:7.4-alpine，access-guard 测试件实测先例，系统属性门控），双节点收敛用同 JVM 两个 `EgonColaTwoLevelCacheManager`
实例共享同一 Testcontainers Redis（TEST-011/012）。

### 4.4 Parallelism and commit boundaries

Step 1 与 Step 2 无相互依赖可并行准备，但 Step 3 需两者都完成。Step 9/10/11 写入路径按根目录天然隔离，若用户后续批准可并行执行也不冲突；本
Plan 默认按序执行，除此之外所有 Step 严格串行。每个 Step 恰好一个语义提交，提交路径严格限定在该 Step 的文件清单内；worktree
中已存在的未跟踪主 Spec 文件不纳入任何 Step 提交路径。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element                                              | Spec necessity verdict/section                                                      | Current repository evidence                                                                          |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| 自建 `EgonColaTwoLevelCache`/`EgonColaTwoLevelCacheManager` | Must — DEC-001（用户选项①；§17 论证开源 `RLocalCachedCache` 跨节点失效为 PRO 能力）                    | 仓内零缓存库（`EVD-001`）；Spring Cache SPI 可由宿主直接使用                                                          |
| 单 `RTopic` 通用事件 + 单监听器                                    | Must — 用户原话"通用 event，而不是每个类型的 cache 都写一个 listener"（`REQ-003`）；DEC-006 拒绝并入业务事件总线    | 组件已有 Redisson topic 基建使用先例（access-guard 测试依赖同型客户端）                                                   |
| `EgonColaCachePort` 放 common-core                         | Must — DEC-003/EVD-013：final 方法引用 starter 类型必致 NoClassDefFoundError，optional 依赖无法豁免 | common-core 现为纯 JDK + jakarta 注解层，被 mp-ext 与各 starter 共同依赖                                           |
| 空值哨兵 + TTL 抖动 + 互斥回源                                      | Must — 用户决策④全含（`REQ-007`~`REQ-009`）                                                 | Guava `Expiry`、Redisson `RLock` 均在已治理依赖面内                                                            |
| `getByCache`/`listByCache` 声明式读                           | Must — 用户决策②（`REQ-006`）；DEC-008 拒绝列表整体键                                             | `EgonColaRepository` 既有 final `getById`/`listByIds` 与 `selectActiveById`/`selectActiveByIds` 路径可直接复用 |
| 写路径 15 个方法尾部登记                                            | Must — `REQ-005` 覆盖"全部受控写方法"                                                        | `EgonColaRepository.java` 实测 15 个 final 写方法（:68~:227）                                                |
| 延迟二次失效（`second-evict-delay`）                              | Must — `REQ-005`/`REQ-013` 双失效协议                                                    | `TransactionSynchronizationManager` + JDK 单线程 daemon 调度器足够，无新依赖                                      |
| 受限 PTV 的 `JsonJacksonCodec`                               | Must — `REQ-004` 安全约束 + TEST-023                                                    | Jackson 位点已在组件父 pom 管理                                                                               |
| MDC 租户读取（`tenant-mdc-key`）                                | Plan 推断（PC-005）— starter 不能依赖 mp-ext 的 `requireTenantId`                            | access-guard 直接读 MDC 的仓内先例                                                                           |
| 区域级豁免/独立 TTL 配置                                           | Not planned — DEC-009（YAGNI，无害 no-op）                                               | 无对应代码位点                                                                                              |
| 值广播/跨节点扩散                                                 | Not planned — DEC-007（PUT 仅逐出对端 L1）                                                 | 监听器分发表无该操作                                                                                           |
| 版本 CAS 防复活                                                | Not planned — DEC-010（需改表，超范围）                                                      | 无迁移需求                                                                                                |
| 新 MyBatis 插件                                              | Not planned — DEC-005（链序围栏存在）                                                       | `EgonColaPluginOrderTest` 与 mp-ext `AutoConfiguration.imports` 均不动                                   |
| Kryo/Protostuff 编解码                                       | Not planned — 用户规则 6 绑定 Jackson                                                     | 组件树无其他序列化库位点                                                                                         |

### 4.6 Change-unit Dependency Matrix

| Change unit                       | Requirements                                                  | Proof/RED point                                            |
|-----------------------------------|---------------------------------------------------------------|------------------------------------------------------------|
| CU-1 组件版本治理（父 pom + 两处局部版本 + BOM） | REQ-014                                                       | Step 1 `dependency:tree` 收敛至单一 redisson 版本位点；Step 8 BOM 解析 |
| CU-2 端口契约（common-core）            | REQ-010                                                       | Step 2 `EgonColaCachePortContractTest` 编译失败（类不存在）转绿        |
| CU-3 模块与配置面                       | REQ-001                                                       | Step 3 `EgonColaCachePropertiesTest` 未绑定/非法值 RED           |
| CU-4 事件契约与编解码                     | REQ-004, REQ-012, REQ-018                                     | Step 4 守卫与往返测试 RED（TEST-001/002/023）                       |
| CU-5 两级内核                         | REQ-002, REQ-007, REQ-008, REQ-009                            | Step 5 集成测试 RED（TEST-004/006/007/008）                      |
| CU-6 监听器 + 端口实现 + 装配              | REQ-003, REQ-005, REQ-011, REQ-013, REQ-018, REQ-025          | Step 6 监听器/端口/装配矩阵/双节点测试 RED（TEST-003/009~014/016/020/024） |
| CU-7 mp-ext 增强与围栏                 | REQ-005, REQ-006, REQ-010, REQ-011, REQ-012, REQ-017, REQ-025 | Step 7 增强测试 RED + 围栏白名单同提交转绿（TEST-005/015/017/018/019）     |
| CU-8 archetype light 家族           | REQ-015, REQ-016, REQ-017                                     | Step 9 ownership 脚本 + 键集比对 + generate check                |
| CU-9 archetype service 家族与 agent  | REQ-015, REQ-016, REQ-017                                     | Step 10 同上                                                 |
| CU-10 archetype web 家族与全仓回归       | REQ-001, REQ-015, REQ-016, REQ-017                            | Step 11 根 clean test + 三脚本                                 |

依赖链：CU-3 ← CU-1/CU-2；CU-4/CU-5/CU-6 ← CU-3；CU-7 ← CU-2（编译）与 CU-6（运行期）；CU-8/CU-9/CU-10 ← CU-1 至 CU-7 全绿。

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

架构剖面：cache starter 与 common-core 修改沿用组件仓库既有 starter 剖面（`autoconfigure` 包 + `Properties` +
`AutoConfiguration.imports`，同 access-guard/dtp/transaction-outbox 模块）；mp-ext 修改在既有 `extension`/`contract`
包内；archetype 根沿用各自 COLA 分层（infrastructure 层 repo 注入示例）。不新造分层，不新增模块剖面。

| Concern         | Current repository evidence                                                                                            | Effective Spec decision                                                                                                                              |
|-----------------|------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| Bean 命名与注入      | 组件 starter 全部显式 bean 名与构造注入（access-guard/dtp 先例）                                                                       | spec §13：bean 名 `egonColaTwoLevelCacheManager`/`egonColaCacheChangedListener`/`egonColaCachePort`；`@ConditionalOnMissingBean(CacheManager.class)` 让位 |
| 配置键             | 前缀 house style `egon.cola.component.<name>`；Duration 用 ISO-8601（archetype agent yml `max-clock-backward: PT0.005S` 先例） | spec §16：28 yml 同构键块，`PT5S`/`PT30M`/`PT60S` 等                                                                                                        |
| JSON 与错误码       | 组件树统一 Jackson（父 pom 管理）；SCREAMING_SNAKE 错误码日志 house style                                                              | spec §10/§13.3：受限 PTV `JsonJacksonCodec` + `JavaTimeModule`；错误码全集冻结                                                                                  |
| 时间类型            | 仓内 `java.time` 全面使用                                                                                                    | spec §10.2 信封 `Instant occurredAt`；配置 `Duration`                                                                                                     |
| Lombok 与 record | 组件内 record 紧凑构造器先例；`@Slf4j`/`@RequiredArgsConstructor` house style                                                     | spec §13.3：信封/哨兵用 record，管理器/监听器用 Lombok 构造注入                                                                                                        |
| 测试              | JUnit5 + AssertJ + Mockito；Testcontainers 测试件（access-guard/outbox 现用，版本提升至父治理见 PC-006）                                 | spec §14.3 TEST-001~024 矩阵                                                                                                                           |
| 对象转换            | 本特性无跨层 PO/BO 转换需求，`EgonColaRepository` 既有转换路径不动                                                                        | spec §10.4：cache starter 不引入 MapStruct                                                                                                               |

复用台账：

| Need         | Candidates inspected                                                                         | Exact evidence                                                            | Fit/gap                    | Decision                                                                                                |
|--------------|----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|----------------------------|---------------------------------------------------------------------------------------------------------|
| 缓存 SPI       | spring-context `org.springframework.cache.Cache`/`CacheManager`/`AbstractValueAdaptingCache` | 宿主 spring-boot-starter-cache 依赖位点（provided 引入）                            | 命中：自实现仅补两级协议               | 用 Spring SPI，零新缓存库                                                                                      |
| Redis 客户端    | 宿主 `RedissonClient`（redisson-spring-boot-starter 已在宿主）                                       | access-guard 以 provider 解析同型先例                                            | 命中                         | 解析不创建（DEC-004）                                                                                          |
| 序列化          | Jackson（父 pom 管理）+ redisson `JsonJacksonCodec`                                               | 组件现有 jackson 位点                                                           | 命中                         | 受限 PTV 包装，不引 Kryo                                                                                       |
| 互斥锁          | Redisson `RLock`                                                                             | redisson 依赖内                                                              | 命中                         | tryLock 带 wait/lease                                                                                    |
| 本地缓存         | Guava `CacheBuilder`/`Expiry`                                                                | common 组件已依赖 guava（父 pom 管理）                                              | 命中                         | L1 用 Guava，不引 Caffeine                                                                                  |
| after-commit | Spring `TransactionSynchronizationManager`                                                   | mp-ext 已依赖 spring-tx；08-25 Spec 不变量先例                                     | 命中                         | 端口实现内注册同步器                                                                                              |
| 延迟调度         | JDK `ScheduledThreadPoolExecutor` 单线程 daemon                                                 | JDK only                                                                  | 命中                         | 不引调度框架                                                                                                  |
| 测试 Redis     | `org.testcontainers:testcontainers` `GenericContainer`（redis:7.4-alpine）                     | access-guard `RedissonStoreIntegrationTest` 实测 :33；outbox pom 本地属性 1.21.4 | 命中：单容器 + 同 JVM 双管理器即可覆盖双节点 | 两 testcontainers 坐标提升至父 dmu（PC-006），cache 模块 pom 零版本；父 pom 既有 `it.ozimov:embedded-redis` 管理位仓内零使用，不入本计划 |
| ID/租户校验      | mp-ext `requireTenantId`（:600）与 `requireSerializableId`（:628）                                | `EgonColaRepository.java` 实读                                              | 命中                         | 键构造复用既有校验，不新写                                                                                           |
| 新三方依赖        | 上述之外全部候选                                                                                     | 组件 pom 树扫描                                                                | 无 gap                      | 组件树零新增三方件，仅治理位点重排（REQ-014）                                                                              |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source                | Repository evidence                                                                         | Exact files and order                                                                                                                 | Pseudocode obligations                                                                          | Validation gate                                             | Steps                             | Status/blocker |
|--------------|----------------------------|---------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|-------------------------------------------------------------|-----------------------------------|----------------|
| Rule 1       | 主 Spec §13（POJO 命名与 BO 后缀） | 全仓类名后缀一致基线                                                                                  | Step 4 `.../cache/model/EgonColaCacheNullValueBO.java`；信封/枚举为技术契约 record 亦以类型后缀语义命名                                                   | 所有新类型 PascalCase + 语义后缀；测试夹具类显式命名                                                               | Step 4 编译 + 文件块命名核对                                         | 4                                 | 无阻塞            |
| Rule 2       | 主 Spec §13.2 配置校验          | `@Validated` + jakarta 约束先例（transaction-outbox `TransactionalOutboxProperties`）             | Step 3 `.../cache/autoconfigure/EgonColaCacheProperties.java`                                                                         | 属性绑定测试含违规值被拒路径；jitter-ratio 用原生 `@DecimalMin`/`@DecimalMax`，不自写校验                               | Step 3 `EgonColaCachePropertiesTest`                        | 3                                 | 无阻塞            |
| Rule 3       | 主 Spec §10.2/§13.3         | 组件 record 紧凑构造器先例；本特性无跨层转换（§10.4）                                                           | Step 4 `EgonColaCacheChangedEvent.java`、`EgonColaCacheChangedOperation.java`；Step 5 管理器/缓存用 `@RequiredArgsConstructor`                | record 紧凑构造器做版本/非空/键守卫；不可变 `List.copyOf`；单例哨兵 record                                            | Step 4 往返与守卫测试（TEST-001/002）                                | 4, 5                              | 无阻塞            |
| Rule 4       | 主 Spec §13.3 bean 名        | 组件 bean 全显式命名 house style；archetype 根 lombok.config 第 2 行 copyableAnnotations 含 `Qualifier` | Step 5/6 管理器/监听器/端口类；Step 6 `EgonColaCacheAutoConfiguration.java`；Step 9~11 模板 repo 注入位点                                              | 业务类 `@Slf4j`；被管理 bean 显式名；注入处 `@Qualifier` 全列出并依赖 copyableAnnotations 传导                        | Step 6 装配矩阵测试（TEST-020）+ Step 9~11 模板编译                     | 5, 6, 9, 10, 11                   | 无阻塞            |
| Rule 5       | 主 Spec §17 依赖面结论           | 组件树已用 Guava/Jackson/Apache Commons；无新三方                                                     | Step 1~3 pom（仅 redisson 治理位点）、Step 4~6（Guava、JDK、Jackson、Redisson API）                                                                | 工具仅 jdk/Apache/Guava 白名单；不引 Caffeine/Kryo/Protostuff                                            | Step 1 `dependency:tree` + Step 11 根构建                      | 1, 3, 4, 5, 6, 8                  | 无阻塞            |
| Rule 6       | 主 Spec §13.3 codec         | 组件树 Jackson 统一位点（父 pom 管理）                                                                  | Step 4 `.../cache/codec/EgonColaCacheCodecs.java`；Step 6 监听器手工 JSON 解码                                                                | 事件与缓存值均走 Jackson（`JsonJacksonCodec` + `JavaTimeModule` + 受限 PTV 白名单）；topic 载荷 StringCodec 手工序列化 | TEST-023 + Step 4 往返测试                                      | 4, 6                              | 无阻塞            |
| Rule 7       | 主 Spec §16 配置面             | 28 yml 现有多 profile 键集一致基线（light application.yml 组件块 :135-160 先例）                            | Step 9~11 各根 4 份 yml（同块字节一致，值按 profile 可不同）                                                                                           | cache 键块在 7 根 × 4 文件结构一致                                                                        | 键集比对（Step 9 内联脚本）+ `./scripts/generate_archetypes.sh check` | 9, 10, 11                         | 无阻塞            |
| Rule 9       | 主 Spec §13.1 模式点名          | 组件既有模板方法/事件分发先例                                                                             | Step 5 `EgonColaTwoLevelCache.java`（继承 `AbstractValueAdaptingCache` 模板方法）、Step 6 `EgonColaCacheChangedListener.java`（按枚举穷尽 switch 分发） | 禁止按缓存类型 if-else 硬编码分发；操作集合以枚举封闭                                                                 | Step 6 监听器测试 + 静态扫描单 addListener                            | 5, 6                              | 无阻塞            |
| Rule 10      | 主 Spec §10.2 时间字段          | 仓内 `java.time` 全面使用                                                                         | Step 4 `EgonColaCacheChangedEvent.java`（`Instant`）；Step 3/5（`Duration` TTL）                                                           | 时间仅 `java.time` 类型，禁 `java.util.Date`/`Calendar`                                                | Step 4/5 测试 + 编译期类型约束                                       | 3, 4, 5                           | 无阻塞            |
| Rule 11      | 主 Spec §8 架构与文件树           | 组件 starter 剖面与 archetype COLA 分层实测（§4.7 证据）                                                 | 全部 Step 的文件均落在 §5 树内既有剖面                                                                                                              | 每 Step 文件顺序与包结构不得偏离组件/archetype 剖面                                                              | 各 Step 编译 + Step 11 全仓回归与边界脚本                               | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 | 无阻塞            |

## 5. Change File Tree

约定：`CACHE-MOD` =
`egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter`；`SRC` =
`CACHE-MOD/src/main/java/top/egon/cola/component/common/cache`；`TST` =
`CACHE-MOD/src/test/java/top/egon/cola/component/common/cache`；`CORE` =
`egon-cola-components/egon-cola-component-common/egon-cola-component-common-core`；`MPEXT` =
`egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`；
`SP` = `egon-cola-archetypes/source-projects`。cache 键块 yml 每根恰 4 个文件（`application.yml`、`application-dev.yml`、
`application-test.yml`、`application-prod.yml`）；light/light-open 在 `src/main/resources/`
下，service/service-open/web/web-open/agent 在各自 `egon-cola-source-<family>-starter/src/main/resources/` 下。

| Op        | Path                                                                                                                                                                                                                                                             | Symbols/elements                                                                                                                                                                                      | Responsibility                                  | Requirements                                         | Step |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|------------------------------------------------------|------|
| MODIFY    | `egon-cola-components/pom.xml`                                                                                                                                                                                                                                   | 属性 `testcontainers.version` + dependencyManagement：`org.redisson:redisson`（`${redisson.version}`）、`org.testcontainers:testcontainers`、`org.testcontainers:junit-jupiter`（`${testcontainers.version}`） | 父 pom 统一治理 plain redisson 与 testcontainers 版本位点 | REQ-014                                              | 1    |
| MODIFY    | `egon-cola-components/egon-cola-component-access-guard-starter/pom.xml`                                                                                                                                                                                          | 删除 redisson 局部版本元素                                                                                                                                                                                    | 局部版本清零                                          | REQ-014                                              | 1    |
| MODIFY    | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`                                                                                                                                           | 删除 redisson 局部版本元素                                                                                                                                                                                    | 局部版本清零                                          | REQ-014                                              | 1    |
| CREATE    | `CORE/src/main/java/top/egon/cola/component/common/core/cache/EgonColaCachePort.java`                                                                                                                                                                            | `EgonColaCachePort`（get/getAll/registerEvictionAfterCommit）                                                                                                                                           | 稳定端口 SPI，纯 JDK 签名                               | REQ-010                                              | 2    |
| CREATE    | `CORE/src/test/java/top/egon/cola/component/common/core/cache/EgonColaCachePortContractTest.java`                                                                                                                                                                | 契约测试                                                                                                                                                                                                  | 钉死 3 方法签名与 JDK-only                             | REQ-010                                              | 2    |
| CREATE    | `CACHE-MOD/pom.xml`                                                                                                                                                                                                                                              | 模块依赖面                                                                                                                                                                                                 | starter 组件依赖位点（版本全走父管理）                         | REQ-001, REQ-014                                     | 3    |
| MODIFY    | `.../egon-cola-component-common/pom.xml`                                                                                                                                                                                                                         | modules 第 9 项                                                                                                                                                                                         | 聚合纳入新模块                                         | REQ-001                                              | 3    |
| CREATE    | `SRC/autoconfigure/EgonColaCacheProperties.java`                                                                                                                                                                                                                 | `EgonColaCacheProperties`（13 键）                                                                                                                                                                       | 配置绑定与 jakarta 校验                                | REQ-001                                              | 3    |
| CREATE    | `TST/autoconfigure/EgonColaCachePropertiesTest.java`                                                                                                                                                                                                             | 绑定/违规值测试                                                                                                                                                                                              | 配置面验收                                           | REQ-001                                              | 3    |
| CREATE    | `SRC/event/EgonColaCacheChangedOperation.java`                                                                                                                                                                                                                   | 枚举 `PUT/EVICT/PREFIX_EVICT`                                                                                                                                                                           | 操作封闭集                                           | REQ-004                                              | 4    |
| CREATE    | `SRC/model/EgonColaCacheNullValueBO.java`                                                                                                                                                                                                                        | 无字段 record + 单例                                                                                                                                                                                       | 空值哨兵                                            | REQ-007                                              | 4    |
| CREATE    | `SRC/event/EgonColaCacheChangedEvent.java`                                                                                                                                                                                                                       | record + 嵌套 `KeyGuard`                                                                                                                                                                                | 通用信封与键守卫                                        | REQ-004, REQ-012                                     | 4    |
| CREATE    | `SRC/codec/EgonColaCacheCodecs.java`                                                                                                                                                                                                                             | `EgonColaCacheCodecs`                                                                                                                                                                                 | 受限 PTV Jackson 编解码                              | REQ-004, REQ-018                                     | 4    |
| CREATE    | `TST/event/EgonColaCacheChangedEventTest.java`                                                                                                                                                                                                                   | 守卫/往返测试                                                                                                                                                                                               | TEST-001/002                                    | REQ-004, REQ-012                                     | 4    |
| CREATE    | `TST/codec/EgonColaCacheCodecsTest.java`                                                                                                                                                                                                                         | 白名单/多态测试                                                                                                                                                                                              | TEST-023                                        | REQ-004                                              | 4    |
| CREATE    | `SRC/core/EgonColaTwoLevelCache.java`                                                                                                                                                                                                                            | 继承 `AbstractValueAdaptingCache` + 嵌套 `Jitter`                                                                                                                                                         | 两级读写/回填/哨兵/互斥/抖动/发布                             | REQ-002, REQ-007, REQ-008, REQ-009, REQ-013, REQ-018 | 5    |
| CREATE    | `SRC/core/EgonColaTwoLevelCacheManager.java`                                                                                                                                                                                                                     | `CacheManager` 实现                                                                                                                                                                                     | 区域映射、进程 node-id/调度器、topic 发布原语                  | REQ-002, REQ-003                                     | 5    |
| CREATE    | `TST/support/CacheRedisTestSupport.java`                                                                                                                                                                                                                         | Testcontainers Redis 夹具（门控 `egon.cola.cache.redis.it`）                                                                                                                                                | Step 5/6 集成测试共享基建                               | REQ-002                                              | 5    |
| CREATE    | `TST/core/EgonColaTwoLevelCacheManagerTest.java`                                                                                                                                                                                                                 | 管理器测试                                                                                                                                                                                                 | TEST-004 前置装配                                   | REQ-002                                              | 5    |
| CREATE    | `TST/core/EgonColaTwoLevelCacheTest.java`                                                                                                                                                                                                                        | 两级集成测试                                                                                                                                                                                                | TEST-005~008 命中/哨兵/抖动/互斥                        | REQ-002, REQ-007~009                                 | 5    |
| CREATE    | `SRC/event/EgonColaCacheChangedListener.java`                                                                                                                                                                                                                    | `SmartLifecycle` + 嵌套 `EventBus`                                                                                                                                                                      | 全模块唯一订阅与分发                                      | REQ-003, REQ-004, REQ-018                            | 6    |
| CREATE    | `SRC/port/EgonColaTwoLevelCachePort.java`                                                                                                                                                                                                                        | `EgonColaCachePort` 实现 + 嵌套 `EvictionBuffer`                                                                                                                                                          | 守卫/after-commit/双失效/批量组装                        | REQ-005, REQ-010~REQ-013, REQ-025                    | 6    |
| CREATE    | `SRC/autoconfigure/EgonColaCacheAutoConfiguration.java`                                                                                                                                                                                                          | 自动装配                                                                                                                                                                                                  | bean 装配、RedissonClient 解析、fail-fast             | REQ-001, REQ-011, REQ-018                            | 6    |
| CREATE    | `CACHE-MOD/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`                                                                                                                                                  | 1 行                                                                                                                                                                                                   | 装配入口                                            | REQ-001                                              | 6    |
| CREATE    | `TST/event/EgonColaCacheChangedListenerTest.java`                                                                                                                                                                                                                | 监听器测试                                                                                                                                                                                                 | TEST-003 自回声/坏 JSON/未知操作/多区域                    | REQ-003, REQ-004                                     | 6    |
| CREATE    | `TST/port/EgonColaTwoLevelCachePortEvictionIntegrationTest.java`                                                                                                                                                                                                 | 端口失效测试                                                                                                                                                                                                | TEST-009/010/013/014                            | REQ-005, REQ-012, REQ-025                            | 6    |
| CREATE    | `TST/port/EgonColaCacheClusterConvergenceTest.java`                                                                                                                                                                                                              | 收敛/getAll 测试                                                                                                                                                                                          | TEST-011/012                                    | REQ-013                                              | 6    |
| CREATE    | `TST/autoconfigure/EgonColaCacheAutoConfigurationTest.java`                                                                                                                                                                                                      | 装配矩阵测试                                                                                                                                                                                                | TEST-020                                        | REQ-001, REQ-011, REQ-018                            | 6    |
| CREATE    | `CACHE-MOD/README.md`                                                                                                                                                                                                                                            | 使用文档                                                                                                                                                                                                  | 开关、键规则、cluster 部署说明                             | REQ-001                                              | 6    |
| CREATE    | `MPEXT/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaRepositoryCacheEnhancementTest.java`                                                                                                                                               | 增强测试 + 内联夹具                                                                                                                                                                                           | TEST-005/015/017(部分)/019                        | REQ-005, REQ-006, REQ-011, REQ-012                   | 7    |
| MODIFY    | `MPEXT/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaRepository.java`                                                                                                                                                                   | seam 2 个 + 15 写方法登记 + `getByCache`/`listByCache` + 私有 helper                                                                                                                                          | repo 层缓存增强                                      | REQ-005, REQ-006, REQ-010, REQ-011, REQ-012, REQ-025 | 7    |
| MODIFY    | `MPEXT/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaRepositoryArchitectureTest.java`                                                                                                                                                    | 白名单等式 + 禁 import 扫描                                                                                                                                                                                   | TEST-017/018 围栏                                 | REQ-010, REQ-017                                     | 7    |
| MODIFY    | `MPEXT/README.md`、`MPEXT/README.zh-CN.md`                                                                                                                                                                                                                        | 缓存增强一节（双语镜像）                                                                                                                                                                                          | 文档同步                                            | REQ-006                                              | 7    |
| MODIFY    | `egon-cola-components/egon-cola-components-bom/pom.xml`                                                                                                                                                                                                          | 导出 cache starter + plain redisson                                                                                                                                                                     | 发布依赖面                                           | REQ-014, REQ-015                                     | 8    |
| MODIFY    | `SP/egon-cola-source-light/pom.xml`、`SP/egon-cola-source-light-open/pom.xml`                                                                                                                                                                                     | 增无版本 cache starter 引用 + 删 mp-ext 引用内联版本                                                                                                                                                               | 依赖适配（REQ-015）；同文件为 light/light-open 各自根 pom     | REQ-014, REQ-015                                     | 9    |
| MODIFY    | light/light-open 两根 `src/main/resources/` 下 `application.yml`、`application-dev.yml`、`application-test.yml`、`application-prod.yml`（共 8 文件）                                                                                                                        | `egon.cola.component.cache` 键块                                                                                                                                                                        | 配置骨架（REQ-016，规则 7）                              | REQ-016                                              | 9    |
| MODIFY    | `SP/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/user/repo/UserRepository.java`、`SP/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/user/repo/UserRepository.java` | 端口注入示例                                                                                                                                                                                                | 模板注入（REQ-016）                                   | REQ-016                                              | 9    |
| MODIFY    | `SP/egon-cola-source-light/README.md`、`README.zh-CN.md` 与 `SP/egon-cola-source-light-open/README.md`、`README.zh-CN.md`（双语镜像）                                                                                                                                     | 缓存骨架一节                                                                                                                                                                                                | 文档（REQ-016）                                     | REQ-016                                              | 9    |
| GENERATED | `egon-cola-archetypes/definitions/egon-cola-archetype-light/**`、`egon-cola-archetypes/definitions/egon-cola-archetype-light-open/**`                                                                                                                             | 再生成产物                                                                                                                                                                                                 | 与 source-projects 同步（REQ-017）                   | REQ-017                                              | 9    |
| MODIFY    | `SP/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml`、`SP/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml`                                                                                            | 删 mp-ext 内联版本（:20）+ 增无版本 cache 引用（根 pom 不动，PC-003）                                                                                                                                                    | service 家族依赖适配                                  | REQ-014, REQ-015                                     | 10   |
| MODIFY    | service/service-open/agent 三根 starter `src/main/resources/` 下 4 份 yml（共 12 文件）                                                                                                                                                                                   | cache 键块                                                                                                                                                                                              | 配置骨架                                            | REQ-016                                              | 10   |
| MODIFY    | `SP/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/course/repo/CourseRepository.java` 与 `.../serviceopen/.../course/repo/CourseRepository.java`                            | 端口注入示例                                                                                                                                                                                                | 模板注入                                            | REQ-016                                              | 10   |
| MODIFY    | service/service-open/agent 三根 `README.md` 与 `README.zh-CN.md`（共 6 文件）                                                                                                                                                                                            | 缓存骨架一节                                                                                                                                                                                                | 文档                                              | REQ-016                                              | 10   |
| MODIFY    | `SP/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml`                                                                                                                                                                                                | 删 mp-ext 内联版本（:19）+ 增无版本 cache 引用（agent 根 pom 不动、无 repo 注入示例）                                                                                                                                         | agent 依赖适配                                      | REQ-014, REQ-015                                     | 10   |
| GENERATED | `egon-cola-archetypes/definitions/egon-cola-archetype-service/**`、`egon-cola-archetype-service-open/**`、`egon-cola-archetype-agent/**`                                                                                                                           | 再生成产物                                                                                                                                                                                                 | 同步                                              | REQ-017                                              | 10   |
| MODIFY    | web/web-open 两 `-infrastructure/pom.xml`（删 mp-ext 内联版本 :21 + 无版本 cache 引用；根 pom 不动）、两 starter 下 4 份 yml（共 8 文件）、两 `.../user/repo/UserRepository.java`（web/webopen 包）、`README.md`+`README.zh-CN.md` 共 4 文件                                                        | 与 light/service 家族同构                                                                                                                                                                                  | web 家族适配                                        | REQ-014, REQ-015, REQ-016                            | 11   |
| GENERATED | `egon-cola-archetypes/definitions/egon-cola-archetype-web/**`、`egon-cola-archetype-web-open/**`                                                                                                                                                                  | 再生成产物                                                                                                                                                                                                 | 同步                                              | REQ-017                                              | 11   |

不删除、不重命名任何既有文件；不触碰 `egon-cola-archetypes/source-projects/**/target/`、模板 DDL 目录、mp-ext
`AutoConfiguration.imports`、`EgonColaIRepository` 与任何 Flyway 迁移。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Commands and environment

- 构建/测试统一命令面：组件模块 `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :<artifactId> -am clean test`
  （在仓库根目录执行）；全仓 `./mvnw -B -ntp clean test`；archetype 校验 `./scripts/generate_archetypes.sh check` 与
  `python3 scripts/check-archetype-dependency-ownership.py`、`python3 scripts/check-native-archetype-boundaries.py`、
  `python3 scripts/check-archetype-family-boundaries.py`。
- 环境：JDK 17+（仓内 `java.time`/record 全面使用，`mvnw` 以根 pom 编译目标为准）、Maven 由 wrapper 提供、Testcontainers 需本地
  Docker（门控属性先例实测 access-guard `-Degon.access.guard.redis.it=true`，本计划门控
  `-Degon.cola.cache.redis.it=true`）。

### 6.2 Immutable files and contracts

- 主 Spec 已冻结：错误码全集、bean 名、配置键树（13 键）、事件信封字段序、键形状正则 `^\d+:\d+$`/glob `^\d+:\*$`、区域名
  `[A-Za-z0-9_.-]{1,100}`、topic 默认 `egon:cola:cache:event`、key-prefix `egon:cola:cache`。Plan 与实现不得偏移。
- 既有围栏不可稀释：接口 57 钉数、链序 `EgonColaPluginOrderTest`、`EgonColaMybatisPlusContractValidator`、租户别名文本扫描。仅允许
  §5 所列白名单式修订。
- `EgonColaRepository` 全部既有 public final 签名逐字节不变；新增仅为 protected seam、私有 helper 与 2 个 public final
  读方法。
- 根 reactor（根 pom modules=components/xingyuan/archetypes）与 `egon-cola-archetypes/definitions/` 的提交纪律：definitions
  是已提交生成产物，任何 source-projects 变更必须与 `generate` 再生产物同提交。

### 6.3 Dirty worktree precautions

基线 `dd7389c1e` 上仅有 1 个未跟踪文件（主 Spec 本身）。所有 Step 提交路径严禁包含 `docs/egon/spec/2026-09-18-15-37-*.md`；本
Plan 文件与其 Related Plans 回填由用户决定提交时机，不属于任何实现 Step。

### 6.4 Plan Clarifications

| ID     | Clarification                                                                                                                                                                                                                       | Evidence                                                                                                                                                                                                                                                | Impact if wrong                                                       |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| PC-001 | 主 Spec §10.1/§13.1/§14.1 点名的 `KeyGuard`/`Jitter`/`EventBus`/`EvictionBuffer` 4 个协作对象实现为宿主类嵌套静态类（分别嵌套于 `EgonColaCacheChangedEvent`、`EgonColaTwoLevelCache`、`EgonColaCacheChangedListener`、`EgonColaTwoLevelCachePort`），符号名可达、不新增顶层文件 | 主 Spec §8.2 文件树与 §13.3"10 生产类"为文件数权威；4 者均无跨类复用需求（逐处实读 spec 点名上下文）                                                                                                                                                                                       | 若用户要求顶层类：仅移动文件位置，测试与公共契约零变化，可机械重构                                     |
| PC-002 | 基线红门：`python3 scripts/check-archetype-dependency-ownership.py` 在 Step 1 前实测 exit=1，报 7 根 mp-ext 引用内联版本违例（非本特性引入）。处理：在各家族 Step 引入 cache starter 的同文件提交中删除该内联版本元素，使 REQ-015 验收达成时脚本对 7 根转绿                                            | 本会话实测输出（7 条 mp-ext 违例逐一列出 pom 行号）；spec REQ-015 验收"ownership 脚本对 7 根通过"                                                                                                                                                                                  | 若用户要求先修复再实施：需另立前置 Spec/Plan 变更；不影响本 Plan Step 内部一致性                   |
| PC-003 | archetype 根仅"无版本引用"，不在根 dependencyManagement 新增 cache starter 管理条目                                                                                                                                                                  | 主 Spec §8.2 web 根注释与脚本行为冲突：ownership 脚本对任意带版本的 top.egon BOM 件（含 dmu 条目）报错；根 dmu 现仅管 grpc/protobuf/facade 实测                                                                                                                                             | 若脚本判定不同：仅调整声明位置（根 dmu 或 infrastructure 引用），依赖语义不变                     |
| PC-004 | TTL 抖动取 TEST-008 边界：有效 TTL ∈ `[expire, expire+jitter]`（单侧非负偏移），同次写入抽样共享给 L1/L2                                                                                                                                                      | 主 Spec REQ-008 文案"±jitter"与 TEST-008 断言区间不一致；TEST 为验收权威；ASM-002 要求 L1 ≤ L2                                                                                                                                                                              | 若需双侧偏移：改 `Jitter` 一个方法 + 测试边界常量，公共契约不变                                |
| PC-005 | starter 感知当前租户经配置 `tenant-mdc-key`（默认 `tenantId`）直读 `org.slf4j.MDC`；上下文缺失按 fail-closed 抛 `CACHE_KEY_TENANT_MISMATCH`                                                                                                                | REQ-012 要求键守卫二次校验但 starter 禁止依赖 mp-ext；access-guard 直读 MDC 先例；TEST-010 使用"MDC 租户桩"                                                                                                                                                                      | 若需注入式租户 SPI：新增一个 common-core 小接口由宿主实现，端口内替换读取源，键规则不变                  |
| PC-006 | 测试 Redis 夹具采用 Testcontainers（`org.testcontainers:testcontainers` + `junit-jupiter`，版本 1.21.4 提升至 `egon-cola-components/pom.xml` dependencyManagement，属性 `testcontainers.version`）；cache 模块测试 pom 零版本引用                              | 主 Spec §14.1 建议 `it.ozimov:embedded-redis` 并称 access-guard 先例——实测证伪：该管理位（父 pom :264-268）仓内零使用，`RedissonStoreIntegrationTest` 实际用 Testcontainers `GenericContainer(redis:7.4-alpine)` + 系统属性门控；Testcontainers 是当前仓库唯一活的 Redis 集成测试模式，且符合用户"父 pom 统一治理"约束 | 若用户坚持 embedded-redis：仅替换 support 夹具一个文件，测试逻辑与门控属性不变；父 pom 管理位已存在，无需新增 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 组件父 pom 统一 plain redisson 与 testcontainers 版本治理

- Requirements: REQ-014
- Dependencies: 无（基线 `dd7389c1e` 起步；可与 Step 2 并行准备，但 Step 3 需两步齐备）
- Baseline state: `egon-cola-components/pom.xml:78` 已有属性 `redisson.version=3.26.0`，dmu 仅管理
  `redisson-spring-boot-starter`（:204-214）；`egon-cola-component-access-guard-starter/pom.xml:52-56` 与
  `egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml:64-68` 以局部
  `${redisson.version}` 引入 plain `org.redisson:redisson`；`egon-cola-component-dynamic-thread-pool-admin/pom.xml:55-58`
  另有一处同类局部版本，属主 Spec 未点名的既有存量，本 Step 按 REQ-014 字面范围保留不动（差异记录于 §11
  风险行，不构成阻塞）。另实测：testcontainers 无父级管理（outbox pom :20 本地属性 1.21.4、access-guard pom :87-102
  内联版本，均先例即违例形态）；父 dmu :264-268 的 `it.ozimov:embedded-redis` 管理位仓内零使用。本 Step 一并把
  `org.testcontainers:testcontainers` 与 `org.testcontainers:junit-jupiter` 提升进父 dmu（PC-006），供 Step 3 cache
  模块零版本引用。
- Observable outcome: plain redisson 与 testcontainers 版本在组件树治理面各自恰好声明一次；两个被点名 starter 的 redisson
  依赖不再有版本元素；`dependency:tree` 对两模块解析出 3.26.0；新增属性 `testcontainers.version=1.21.4` 与两条
  testcontainers 管理条目使 Step 3 的 test 依赖可零版本解析。
- End state: Step 3 新建 starter pom 可直接声明无版本的 `org.redisson:redisson` 并继承父管理。
- Test-first gate: Not applicable — 纯构建治理，无行为面可测；REQ-014 验收列本身以 pom 构建与依赖树为证据。
- Manual Checks: MC-DEP-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 5, Rule 11
- Ordered files: 见下 File 1~3，顺序为"先建管理位、再撤局部版本"，任何颠倒都会造成中间不可构建态。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter,:egon-cola-component-dynamic-thread-pool-starter -am dependency:tree -Dincludes=org.redisson`
- Expected result: 两模块依赖树各含 `org.redisson:redisson:jar:3.26.0`（access-guard 侧标 optional），无
  `dependencies.dependency.version is missing` 错误，BUILD SUCCESS。
- Failure returns to: File 1（管理位坐标或插入位置错误）。
- Completion criteria: 上述命令输出含两行 3.26.0；且 `grep -c 'redisson' 两 starter pom` 复核其 redisson 依赖块内无
  `version` 元素；组件 reactor 相关模块构建绿。
- Rollback:
  `git checkout -- egon-cola-components/pom.xml egon-cola-components/egon-cola-component-access-guard-starter/pom.xml egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`
- Commit paths: `egon-cola-components/pom.xml`, `egon-cola-components/egon-cola-component-access-guard-starter/pom.xml`,
  `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`
- Commit: `build(components): manage plain redisson and testcontainers versions in parent dependencyManagement`

#### File 1 — `MODIFY egon-cola-components/pom.xml`

- Purpose: 在组件父 pom dependencyManagement 增加 plain `org.redisson:redisson` 与两条 `org.testcontainers:*`
  管理条目（含新属性）。
- Symbols: dependencyManagement 新条目 `org.redisson:redisson` → `${redisson.version}`；新属性
  `testcontainers.version=1.21.4`；新条目 `org.testcontainers:testcontainers`、`org.testcontainers:junit-jupiter` →
  `${testcontainers.version}`（testcontainers-bom 不引入，仅两坐标，避免 BOM 面扩大）。
- Repository evidence: 属性 `redisson.version=3.26.0` 已在 :78；同类兄弟条目 `redisson-spring-boot-starter` 在 :204-214（含
  actuator exclusion），新条目紧随其后同风格书写。
- Dependencies and consumers: 下游消费方为 File 2/File 3 两 starter 与 Step 3 新建 cache starter pom；父 pom 自身 import
  了 `top.egon:egon-cola-components-bom`（:125-131），新增条目与之无坐标冲突（BOM 内无 redisson 坐标，实测 grep 零命中）。
- Why now: 管理位必须先于局部版本撤除，否则两 starter 立即失去版本来源而不可构建。
- Contract/signature changes: 仅构建元数据；不改任何属性值与既有条目。
- Input/output and state mapping: 无运行时映射；pom 解析态从"plain redisson 无管理"变为"单点管理"。
- Error and edge behavior: 若坐标或版本属性拼错，File 2/File 3 构建即报 `version is missing`，由本 Step 验证命令兜住。
- Standards impact: MC-DEP-001 通过——三方件版本收敛至父 dmu 单点；不新增依赖坐标。
- Implementation pseudocode:

```xml
<!-- properties 段（embedded.redis.version 同区）新增 -->
<testcontainers.version>1.21.4</testcontainers.version>

<!-- 紧随 redisson-spring-boot-starter 条目之后插入 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>${redisson.version}</version>
</dependency>
<!-- 紧随其后再插两条（test 用途，无 scope 声明于 dmu） -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>${testcontainers.version}</version>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>${testcontainers.version}</version>
</dependency>
```

- Verification contribution: 为 Verification command 的依赖树解析提供版本源；并为 Step 5 `CacheRedisTestSupport`
  （testcontainers 零版本引用）提供管理位。
- Literal rule enforcement: Rule 5——依赖仍限既有白名单三方件（Redisson 已在用），仅治理位点上移；Rule 11——文件位置与既有条目风格不偏离组件父
  pom 剖面。
- After this file: 父 pom 可构建，两 starter 局部版本仍在（幂等覆盖态）。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-access-guard-starter/pom.xml`

- Purpose: 撤除 plain redisson 的局部版本元素，保留 optional 语义。
- Symbols: `org.redisson:redisson` 依赖声明（现 :52-56）。
- Repository evidence: 实测块内容——groupId/artifactId 后跟 `<version>${redisson.version}</version>`（:55）再跟
  `optional=true`；:45-51 有解释"不用 redisson-spring-boot-starter 以免自动建连打爆无 Redis 启动"的注释，保留。
- Dependencies and consumers: access-guard 主源码 `RedissonStore` 编译依赖该坐标；同模块既有集成测试（Testcontainers）运行期依赖解析结果
  3.26.0。
- Why now: 管理位就绪后第一处撤除点。
- Contract/signature changes: 仅删除版本元素一行；optional 与注释不动。
- Input/output and state mapping: 解析版本从局部字面引用变为父 dmu 继承，值不变（3.26.0）。
- Error and edge behavior: 若误删 optional 元素，access-guard 会把 redisson 传染给宿主——验证命令 + 模块既有测试共同拦截。
- Standards impact: MC-DEP-001——组件树局部版本声明数 -1；MC-SCOPE-001——仅动一个依赖元素。
- Implementation pseudocode:

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <!-- 删除 version 元素一行；继承 File 1 管理位 -->
    <optional>true</optional>
</dependency>
```

- Verification contribution: Verification command 中 access-guard 树行仍须解析 3.26.0。
- Literal rule enforcement: Rule 5——不新增任何工具/依赖，仅治理既有 Redisson 位点；Rule 11——保持 starter pom 既有依赖块顺序。
- After this file: access-guard 无局部版本；reactor 可构建。

#### File 3 —
`MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`

- Purpose: 同 File 2，撤除 dtp-starter plain redisson 局部版本。
- Symbols: `org.redisson:redisson` 依赖声明（现 :64-68，版本元素在 :67）。
- Repository evidence: 实测该块为非 optional 的 plain redisson 直依赖（dtp 自建行内注释 :59-63 说明其自建
  RedissonClient），仅版本元素一行待删。
- Dependencies and consumers: dtp-starter 主源码与 `EgonColaDtpExecutorRegistry` 等运行期使用 Redisson API；dtp-admin
  模块的同类既有局部版本（:55-58）按主 Spec 点名范围不列入本 Step。
- Why now: REQ-014 点名两处中的最后一处；放在 light 无关位置以免中间态。
- Contract/signature changes: 删除版本元素一行。
- Input/output and state mapping: 同 File 2。
- Error and edge behavior: 误删相邻依赖元素会使 dtp 编译失败，模块级 clean test（本 Step 完成判据的一部分）拦截。
- Standards impact: MC-DEP-001——第二处局部版本清零；MC-TEST-001——由既有 dtp 测试回归保护，无新测试需求（纯构建变更）。
- Implementation pseudocode:

```xml
<!-- :64-68 依赖块整块保留，仅删 :67 版本元素一行（:59-63 自建客户端说明注释不动）： -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <!-- 原 version 元素行删除，版本改由父 dependencyManagement 解析为 3.26.0 -->
</dependency>
<!-- 编辑后本文件 diff 恰为 -1 行；optional、exclusions 等相邻元素误动即编译/传递面失败 -->
```

- Verification contribution: Verification command 中 dtp-starter 树行解析 3.26.0。
- Literal rule enforcement: Rule 5、Rule 11 同 File 2。
- After this file: Step 1 三文件齐备，执行验证命令与
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter,:egon-cola-component-dynamic-thread-pool-starter -am clean test`
  双绿后提交。

### Step 2 — common-core 缓存端口契约发布

- Requirements: REQ-010
- Dependencies: 无编译依赖（与 Step 1 平行可行）；Step 3/6/7 消费本契约。
- Baseline state: common-core 为纯 JDK + jakarta-validation 层（`CoreBoundaryTest.java` 实测扫描禁 `org.springframework.`
  import、jakarta 仅放行 validation）；模块 pom 无 junit 显式依赖但父 pom 全局注入 `junit-jupiter`(test)（父 pom :112-118）；
  `src/main/java/top/egon/cola/component/common/core/` 下已有 code/exception 等包。
- Observable outcome: `top.egon.cola.component.common.core.cache.EgonColaCachePort` 以 3 个纯 JDK 签名方法发布，契约测试钉死签名与
  JDK-only。
- End state: cache starter（Step 6）可提供实现、mp-ext（Step 7）可零缓存依赖地引用该类型。
- Test-first gate: Required — RED：契约测试编译失败，符号 `EgonColaCachePort` 不存在。
- Manual Checks: MC-ARCH-001, MC-NAME-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 11
- Ordered files: 先契约测试（File 1）后端口接口（File 2）。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-core -am clean test`
- Expected result: `EgonColaCachePortContractTest` 3+ 用例全绿；`CoreBoundaryTest`/`CoreConsolidationTest` 保持全绿；BUILD
  SUCCESS。
- Failure returns to: File 2（签名偏离契约）。
- Completion criteria: 新测试类全部通过且 `CoreBoundaryTest` 不因新文件报错（端口零 import 非 JDK 框架）。
- Rollback: 删除新增两文件（`git rm` 两路径或恢复未跟踪态）。
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/cache/EgonColaCachePortContractTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/cache/EgonColaCachePort.java`
- Commit: `feat(core): publish EgonColaCachePort SPI for two-level cache integration`

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/cache/EgonColaCachePortContractTest.java`

- Purpose: 钉死端口三方法签名、JDK-only 类型面与桩实现语义。
- Symbols: `EgonColaCachePortContractTest`、内联私有桩类 `RecordingPort implements EgonColaCachePort`。
- Repository evidence: common-core 测试目录既有 `code`/`exception` 子包按主源码包镜像放置（实测
  `src/test/java/top/egon/cola/component/common/core/` 结构）；JUnit5 断言风格同 `CoreBoundaryTest`（
  `org.junit.jupiter.api.Assertions`）。
- Dependencies and consumers: 本文件仅消费 File 2 的接口；后续 Step 6/7 的测试不依赖本类。
- Why now: test-first RED 锚点。
- Contract/signature changes: 新增测试类；无生产契约变化。
- Input/output and state mapping: 桩记录调用参数供断言；`getAll` 返回与入参 keys 等长列表。
- Error and edge behavior: 反射签名漂移（如误改 `Supplier` 为 `Callable`）即编译失败或断言失败。
- Standards impact: MC-TEST-001——契约即测试；MC-NAME-001——测试类以 `Test` 结尾。
- Implementation pseudocode:

```java
class EgonColaCachePortContractTest {
    @Test void exposesThreeJdkOnlyMethods() throws Exception {
        assertEquals(Object.class, EgonColaCachePort.class
            .getMethod("get", String.class, String.class, Supplier.class).getReturnType());
        assertEquals(List.class, EgonColaCachePort.class
            .getMethod("getAll", String.class, List.class, Function.class).getReturnType());
        void.class.getMethod... // registerEvictionAfterCommit(String, Collection, Collection)
    }
    @Test void portSourceImportsJdkOnly() throws Exception {
        // 读主源码文件逐行断言：无 import 前缀超出 java.util / java.util.function
    }
    @Test void recordingStubRoundTripsArguments() {
        var port = new RecordingPort();
        port.get("UserBO", "41:7", () -> "v");
        // 断言 cacheName/key/loader 调用恰一次，桩返回 loader.get()
    }
}
```

- Verification contribution: 步骤验证命令的主目标测试类。
- Literal rule enforcement: Rule 11——测试包结构镜像主源码包，遵循组件模块剖面。
- After this file: 模块编译失败（接口缺失），即预期 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/cache/EgonColaCachePort.java`

- Purpose: 发布稳定缓存能力端口 SPI。
- Symbols: `public interface EgonColaCachePort`，方法
  `Object get(String cacheName, String key, Supplier<Object> loader)`、
  `List<Object> getAll(String cacheName, List<String> keys, Function<String, Object> loader)`、
  `void registerEvictionAfterCommit(String cacheName, Collection<String> exactKeys, Collection<String> globPatterns)`。
- Repository evidence: 主 Spec §10.1 端口定义与 §8.2 树；common-core 包根 `top.egon.cola.component.common.core` 下新增
  `cache` 子包，与既有 `code`/`exception` 同层。
- Dependencies and consumers: 仅 `java.util`/`java.util.function` import（满足 `CoreBoundaryTest` 扫描）；消费者为 Step 6
  实现与 Step 7 mp-ext。
- Why now: 契约先于实现与消费者发布，保证依赖方向下层稳定。
- Contract/signature changes: 新增接口；Javadoc 必须写明键形状 `tenantId:id`、glob 仅 `tenantId:*`、getAll 返回与 keys
  等长（null 表示该 id 无值）、registerEvictionAfterCommit 的事务语义（激活事务 afterCommit 执行；回滚丢弃）。
- Input/output and state mapping: 纯签名，无状态；语义由实现方与 spec §10.4 承载。
- Error and edge behavior: 接口层不抛受检异常、不做校验（校验在实现方守卫）。
- Standards impact: MC-ARCH-001——端口置于 common-core，mp-ext/starter 依赖方向不变；MC-NAME-001——类型名以 `Port` 语义后缀且
  PascalCase。
- Implementation pseudocode:

```java
package top.egon.cola.component.common.core.cache;

public interface EgonColaCachePort {
    Object get(String cacheName, String key, Supplier<Object> loader);
    List<Object> getAll(String cacheName, List<String> keys, Function<String, Object> loader);
    void registerEvictionAfterCommit(String cacheName, Collection<String> exactKeys,
                                     Collection<String> globPatterns);
}
```

- Verification contribution: 使 File 1 与模块全量测试转绿。
- Literal rule enforcement: Rule 1——接口按 POJO 规范 PascalCase + 领域后缀 `Port`；Rule 11——落在 common-core 既有包剖面。
- After this file: 契约发布完成；执行验证命令绿后提交本 Step。

### Step 3 — cache starter 模块骨架与配置面

- Requirements: REQ-001
- Dependencies: Step 1（redisson 治理位点就绪）、Step 2（common-core 端口可依赖）。
- Baseline state: common 聚合 pom 现 8 个模块（实测清单含
  core/crypto/data-desensitize/id/mp-ext/test/trace/trace-starter）；组件父 pom 全局注入 lombok(provided) 与
  junit-jupiter(test)；配置类 house style 为 JavaBean + `@ConfigurationProperties` + jakarta 校验（transaction-outbox 模块
  `TransactionalOutboxProperties` 先例，实测该模块存在）；Duration 配置在仓内以 ISO-8601 字符串书写（archetype agent yml
  `max-clock-backward: PT0.005S` 先例）。
- Observable outcome: 新模块 `egon-cola-component-common-cache-spring-boot-starter` 进入 reactor 并可构建；
  `EgonColaCacheProperties` 完成 13 键绑定、默认值与校验（此时尚无任何 bean 装配）。
- End state: Step 4~6 在模块内按包新增类型；Step 6 的自动装配直接消费本 Properties。
- Test-first gate: Required — RED：`EgonColaCachePropertiesTest` 编译失败，符号 `EgonColaCacheProperties` 不存在。
- Manual Checks: MC-CONFIG-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 2, Rule 5, Rule 7, Rule 10, Rule 11
- Ordered files: 模块 pom → 聚合登记 → 绑定测试 → Properties 类。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test`
- Expected result: 新模块编译，`EgonColaCachePropertiesTest` 全绿，BUILD SUCCESS。
- Failure returns to: File 1（依赖面缺位导致编译/绑定失败）。
- Completion criteria: 上述命令绿 + `git status` 仅含本 Step 四文件（聚合 pom 一行增量）。
- Rollback: 删除 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter`
  整目录并还原聚合 pom 一行。
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/pom.xml`,
  `egon-cola-components/egon-cola-component-common/pom.xml`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/EgonColaCachePropertiesTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/autoconfigure/EgonColaCacheProperties.java`
- Commit: `feat(cache): scaffold two-level cache starter with validated properties`

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/pom.xml`

- Purpose: 新 starter 的组件依赖面，版本全部走父管理。
- Symbols: artifactId `egon-cola-component-common-cache-spring-boot-starter`，parent `egon-cola-component-common`。
- Repository evidence: 同目录兄弟模块 access-guard pom 结构实测（parent 坐标 :8-11、依赖无版本书写 :21-56）；plain redisson
  管理位由 Step 1 File 1 建立；guava 父 dmu :229-231；testcontainers 两条管理位由 Step 1 File 1 建立（PC-006）。
- Dependencies and consumers: 编译期消费 common-core（Step 2 端口）；Step 4~6 全部类编译于此模块。
- Why now: 模块不存在则其余三个文件无处安放。
- Contract/signature changes: 依赖清单：`top.egon:egon-cola-component-common-core`、
  `org.springframework.boot:spring-boot-autoconfigure`、`org.springframework.boot:spring-boot-starter-cache`
  （provided：宿主未引缓存 starters 时不强制）、`org.springframework:spring-tx`、`org.redisson:redisson`（provided：宿主已带
  redisson-spring-boot-starter）、`com.google.guava:guava`、`com.fasterxml.jackson.core:jackson-databind`、
  `org.springframework.boot:spring-boot-configuration-processor`（optional）、测试件
  `org.springframework.boot:spring-boot-starter-test` 与 `org.testcontainers:testcontainers`、
  `org.testcontainers:junit-jupiter`（均 test scope）。全部零版本元素。
- Input/output and state mapping: 无运行时映射。
- Error and edge behavior: 任一依赖漏版本管理会在构建期立即报 missing version；scope 写错（如 redisson 非
  provided）会使宿主被动升级客户端版本——由 File 2 后 reactor 构建与 Step 8 BOM 校验兜住。
- Standards impact: MC-DEP-001——零局部版本；MC-CONFIG-001——模块坐标与前缀 `egon.cola.component.cache` 的 starter 命名剖面一致。
- Implementation pseudocode:

```xml
<!-- parent: top.egon / egon-cola-component-common / 相对目录 ../pom.xml -->
<!-- 依赖逐条书写（结构同 access-guard pom），示例其一： -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <scope>provided</scope>
</dependency>
<!-- 其余：common-core、spring-boot-autoconfigure、spring-boot-starter-cache(provided)、
     spring-tx、guava、jackson-databind、configuration-processor(optional)、
     spring-boot-starter-test(test)、testcontainers 两坐标(test)；均不带版本元素 -->
```

- Verification contribution: 使 reactor 接受 `-pl :egon-cola-component-common-cache-spring-boot-starter`。
- Literal rule enforcement: Rule 5——依赖面仅 JDK 白名单生态件（Spring/Guava/Jackson/Redisson），零新三方件；Rule 11——pom
  剖面与兄弟 starter 模块逐字段一致。
- After this file: 模块目录存在但聚合未登记，单独构建仍经 `-am` 可达。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/pom.xml`

- Purpose: 把新模块登记进 common 聚合。
- Symbols: 模块清单追加一行 `egon-cola-component-common-cache-spring-boot-starter`。
- Repository evidence: 实测聚合 pom 现含 8 个子模块目录名列表（modules 清单元素与 8 个目录一一对应，如
  `egon-cola-component-common-core`）。
- Dependencies and consumers: 根 reactor 经 components→common 聚合发现该模块；CI 的根 `clean test` 因此覆盖之。
- Why now: 紧随 File 1 使聚合与目录原子一致。
- Contract/signature changes: 仅追加一行清单条目，位置随既有字母序尾位追加。
- Input/output and state mapping: 无。
- Error and edge behavior: 拼错目录名即 reactor 构建失败。
- Standards impact: MC-SCOPE-001——一行变更。
- Implementation pseudocode:

```text
在聚合 pom 的模块清单内仿照既有 8 个目录条目追加一行（书写与相邻行同缩进，按目录名序追加到末位）：
egon-cola-component-common-cache-spring-boot-starter
# 先例：common-core 为首条目的裸目录名行；编辑后 diff 恰为 +1 行
# 目录名拼错时 reactor 立即报 Child module does not exist，由本 Step 验证命令拦截
```

- Verification contribution: 验证命令的 `-pl :` 选择器与 `-am` 链路成立。
- Literal rule enforcement: Rule 11——沿用聚合模块剖面。
- After this file: reactor 含新模块（空源码集，可构建）。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/EgonColaCachePropertiesTest.java`

- Purpose: 钉死 13 键绑定、默认值与 jakarta 校验边界。
- Symbols: `EgonColaCachePropertiesTest`。
- Repository evidence: 组件测试普遍使用 `Binder`/`MapConfigurationPropertySource` 或 `ApplicationContextRunner` 风格；本类用
  `Binder` 直绑，避免依赖 Step 6 的自动装配。
- Dependencies and consumers: 消费 File 4 类；Step 6 装配矩阵测试另行覆盖开关语义。
- Why now: test-first RED 锚点。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: 空 map → `enabled=false`、`nodeId=""`、`keyPrefix="egon:cola:cache"`、
  `redis.topic="egon:cola:cache:event"`、`tenantMdcKey="tenantId"`、`secondEvictDelay=Duration.ofSeconds(5)`、
  `l1.maxSize=10000`、`ttl.expire=Duration.ofMinutes(30)`、`ttl.nullExpire=Duration.ofSeconds(60)`、`ttl.jitterRatio=0.1`、
  `batch.maxKeys=1000`、`lock.waitTime=Duration.ofMillis(500)`、`lock.leaseTime=Duration.ofSeconds(10)`；ISO 字符串 `PT5S`/
  `PT30M` 等可绑定为 `Duration`。
- Error and edge behavior: `ttl.jitterRatio=0.6`/`-0.1` → `jakarta.validation.ConstraintViolationException`（经
  `ValidationBindHandler`）；`l1.maxSize=0` 拒；`ttl.expire` 缺失取默认。
- Standards impact: MC-VALID-001——校验全部原生 jakarta 注解，无自写 Validator；MC-CONFIG-001——键树与主 Spec §16
  逐键一致；MC-TIME-001——时长一律 `java.time.Duration`。
- Implementation pseudocode:

```java
private EgonColaCacheProperties bind(Map<String, Object> props) {
    Binder binder = new Binder(new MapConfigurationPropertySource(props));
    return binder.bind("egon.cola.component.cache", Bindable.of(EgonColaCacheProperties.class)).get();
}
@Test void defaultsMatchSpecKeyTree() { /* 空 map → 13 键默认值逐项断言 */ }
@Test void rejectsJitterRatioAboveHalf() {
    assertThatThrownBy(() -> bind(Map.of("ttl.jitter-ratio", "0.6")))
        .isInstanceOf(ConstraintViolationException.class);
}
```

- Verification contribution: 步骤验证命令主目标。
- Literal rule enforcement: Rule 2——校验经由绑定时的分组化原生注解；Rule 7——本测试即 28 yml 键结构的代码化锚点（Step 9~11
  比对脚本二次锁形）；Rule 10——时长类型仅 `java.time.Duration`。
- After this file: 模块测试编译失败（类不存在），预期 RED。

#### File 4 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/autoconfigure/EgonColaCacheProperties.java`

- Purpose: 绑定并校验 `egon.cola.component.cache` 配置面。
- Symbols: `EgonColaCacheProperties` + 嵌套静态类 `Redis`、`L1`、`Ttl`、`Batch`、`Lock`。
- Repository evidence: `TransactionalOutboxProperties` 的 JavaBean + getter/setter 形态为组件 house style 实测先例；嵌套组合同其结构。
- Dependencies and consumers: File 3 测试；Step 5/6 的 Manager/Cache/Port/AutoConfiguration 读取。
- Why now: Properties 是一切装配的输入。
- Contract/signature changes: `@ConfigurationProperties(prefix = "egon.cola.component.cache")` + `@Validated`；字段：
  `boolean enabled = false`、`String nodeId = ""`、`String keyPrefix = "egon:cola:cache"`、嵌套
  `Redis{String topic = "egon:cola:cache:event"}`、`String tenantMdcKey = "tenantId"`、
  `Duration secondEvictDelay = Duration.ofSeconds(5)`、嵌套 `L1{@Positive int maxSize = 10000}`、嵌套
  `Ttl{@NotNull Duration expire = Duration.ofMinutes(30); @NotNull Duration nullExpire = Duration.ofSeconds(60); @DecimalMin("0.0") @DecimalMax("0.5") double jitterRatio = 0.1}`
  、嵌套 `Batch{@Positive int maxKeys = 1000}`、嵌套
  `Lock{@NotNull Duration waitTime = Duration.ofMillis(500); @NotNull Duration leaseTime = Duration.ofSeconds(10)}`；嵌套类均
  `@Valid` 级联；JavaBean getter/setter 齐备。
- Input/output and state mapping: relaxed-binding 键（`jitter-ratio` 等）↔ camelCase 字段。
- Error and edge behavior: 校验失败在绑定阶段抛 `ConstraintViolationException`，不进入运行期。
- Standards impact: MC-NAME-001——`Properties` 后缀；MC-MODEL-001——可变绑定载体用 JavaBean 而非 record（Spring 绑定语义要求）。
- Implementation pseudocode:

```java
@Data @Validated
@ConfigurationProperties(prefix = "egon.cola.component.cache")
public class EgonColaCacheProperties {
    private boolean enabled = false;
    private String nodeId = "";
    private String keyPrefix = "egon:cola:cache";
    @Valid private Redis redis = new Redis();
    private String tenantMdcKey = "tenantId";
    @NotNull private Duration secondEvictDelay = Duration.ofSeconds(5);
    @Valid private L1 l1 = new L1(); @Valid private Ttl ttl = new Ttl();
    @Valid private Batch batch = new Batch(); @Valid private Lock lock = new Lock();
    @Data public static class Ttl {
        @NotNull private Duration expire = Duration.ofMinutes(30);
        @NotNull private Duration nullExpire = Duration.ofSeconds(60);
        @DecimalMin("0.0") @DecimalMax("0.5") private double jitterRatio = 0.1;
    }
    // Redis/L1/Batch/Lock 同型
}
```

- Verification contribution: File 3 转绿。
- Literal rule enforcement: Rule 2——只用原生 jakarta 注解；Rule 10——时长仅 `java.time.Duration`；Rule 3——配置载体为可变复杂对象，用
  Lombok `@Data` 组合而非 record。
- After this file: 执行验证命令绿后提交本 Step。

### Step 4 — 事件信封、操作枚举、哨兵与受限编解码

- Requirements: REQ-004, REQ-012, REQ-018
- Dependencies: Step 3（模块与依赖面就绪）。
- Baseline state: 模块内尚无主源码类；Jackson 与 redisson codec API 在编译面（File 1 of Step 3 依赖清单）；主 Spec §10.2
  冻结信封字段序与错误码全集；§13.3 冻结编解码白名单（`top.egon.cola.`、`java.util`、`java.time`、`java.lang`）。
- Observable outcome: 通用信封（含嵌套 `KeyGuard`）、操作枚举、空值哨兵与 `EgonColaCacheCodecs`
  就绪且被守卫/往返/白名单测试锁定（TEST-001、TEST-002、TEST-023）。
- End state: Step 5 内核发布事件、Step 6 监听器消费事件均直接复用本 Step 类型；`KeyGuard` 成为端口实现（Step 6）与内核共用的键规则单点。
- Test-first gate: Required — RED：两个新测试类因符号缺失编译失败。
- Manual Checks: MC-MODEL-001, MC-JSON-001, MC-TIME-001, MC-NAME-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001,
  MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 3, Rule 6, Rule 10, Rule 11
- Ordered files: 两测试先行（File 1~2），再按依赖序 Operation → 哨兵 → 信封 → codec（File 3~6）。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test`
- Expected result: `EgonColaCacheChangedEventTest`、`EgonColaCacheCodecsTest` 与 Step 3 测试全绿，BUILD SUCCESS。
- Failure returns to: File 5/File 6（守卫规则或 codec 配置偏离 spec）。
- Completion criteria: 三新测试类通过；grep 确认模块内仅一处类型定义信封 record（通用单信封约束）。
- Rollback: 删除本 Step 六文件。
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedEventTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/codec/EgonColaCacheCodecsTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedOperation.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/model/EgonColaCacheNullValueBO.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedEvent.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/codec/EgonColaCacheCodecs.java`
- Commit: `feat(cache): define generic change event envelope, null sentinel and restricted codecs`

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedEventTest.java`

- Purpose: 锁定信封守卫与不可变性（TEST-001）。
- Symbols: `EgonColaCacheChangedEventTest`。
- Repository evidence: 组件测试断言风格 JUnit5 + `assertThatThrownBy`（Step 3 File 3 同）；错误码字面串 house style
  SCREAMING_SNAKE（主 Spec §13.2 冻结清单）。
- Dependencies and consumers: 消费 File 3/5；无下游。
- Why now: RED 锚点（守卫语义先于实现固化）。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: 合法样本 `cacheName="UserBO"`、`keys=["41:7","41:8"]`（EVICT）与 `["41:*"]`
  （PREFIX_EVICT）；违规样本覆盖：`schemaVersion=2`、空白 `eventId`/`originNodeId`、null `occurredAt`、null `operation`、空
  `keys`、跨租户混装 `["41:7","42:8"]` 中裸键 `"7"`、非法 glob `"*"`/`":*" `"41:7:*"`、非法区域名（空串、含 `:`、超 100 字符）。
- Error and edge behavior: 每类违规断言异常类型（`IllegalArgumentException`/`IllegalStateException`）且 message 含对应错误码子串：
  `CACHE_EVENT_UNSUPPORTED_SCHEMA`、`CACHE_KEY_TENANT_MISMATCH`、`CACHE_GLOB_PATTERN_FORBIDDEN`、`CACHE_NAME_INVALID`；断言
  `keys()` 返回不可变列表（`List.of` 语义）。
- Standards impact: MC-TEST-001——每条守卫分支独立用例；MC-MODEL-001——record 紧凑构造器为规范化唯一入口。
- Implementation pseudocode:

```java
@Test void rejectsForeignTenantExactKey() {
    assertThatThrownBy(() -> event(1, "41:7", "42:8"))   // EVICT 键段混租户
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
}
@Test void rejectsIllegalGlobShapes() { /* "*", "41:7:*", "*:8" → CACHE_GLOB_PATTERN_FORBIDDEN */ }
@Test void unsupportedSchemaVersionRejected() { /* schemaVersion=2 → CACHE_EVENT_UNSUPPORTED_SCHEMA */ }
@Test void keysListIsImmutableCopy() { /* 源 list 构造后 mutate 不影响事件 */ }
```

- Verification contribution: 步骤验证命令目标类。
- Literal rule enforcement: Rule 3——规范化全部经 record 紧凑构造器；Rule 10——时间字段仅 `Instant`。
- After this file: 模块测试编译失败（File 3/5 未建），预期 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/codec/EgonColaCacheCodecsTest.java`

- Purpose: 锁定受限 PTV 白名单与往返（TEST-002/TEST-023）。
- Symbols: `EgonColaCacheCodecsTest`。
- Repository evidence: redisson `JsonJacksonCodec` 与 `PolymorphicTypeValidator` API 在 provided 依赖面内；Jackson
  `JavaTimeModule` 组件树已用（access-guard jackson 先例）。
- Dependencies and consumers: 消费 File 6 与 File 4/5。
- Why now: codec 安全边界先以测试固化。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: 往返样本：信封 record（含 `Instant`）、`EgonColaCacheNullValueBO.INSTANCE`、
  `java.util.LinkedHashMap`；拒绝样本：`@class` 指向 `javax.script.ScriptEngineManager` 与 `java.lang.Runtime` 子类的恶意
  JSON。
- Error and edge behavior: 恶意 `@class` → `ObjectMapper` 抛类型解析类异常（`InvalidTypeIdException`/`IOException`
  族），断言异常且不实例化目标类；`Instant` 序列化为 ISO 文本（`JavaTimeModule` 生效）。
- Standards impact: MC-JSON-001——仅 Jackson，多态白名单受限；MC-TEST-001——正反用例齐备。
- Implementation pseudocode:

```java
@Test void valueCodecRoundTripsEnvelopeWithInstant() { /* encode→decode 字段逐一相等 */ }
@Test void valueCodecRejectsEvilTypeId() {
    String evil = "{\"@class\":\"javax.script.ScriptEngineManager\"}";
    assertThatThrownBy(() -> decode(evil)).isInstanceOf(IOException.class);
}
@Test void nullSentinelRoundTrips() { /* 哨兵 encode/decode 恒等 */ }
```

- Verification contribution: 步骤验证命令目标类。
- Literal rule enforcement: Rule 6——对外/跨进程载荷一律 Jackson 注解与模块装配。
- After this file: 仍 RED（File 6 未建）。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedOperation.java`

- Purpose: 操作封闭集枚举。
- Symbols: `public enum EgonColaCacheChangedOperation { PUT, EVICT, PREFIX_EVICT }`。
- Repository evidence: 主 Spec §10.2 冻结；组件内枚举先例为纯常量 + Javadoc。
- Dependencies and consumers: File 5 信封字段、Step 5 发布点、Step 6 监听器 switch。
- Why now: 信封编译前置。
- Contract/signature changes: 新增枚举，无方法体。
- Input/output and state mapping: PUT→写路径回源广播（对端仅逐出 L1，DEC-007）；EVICT→双级精确失效；PREFIX_EVICT→租户段 glob
  失效。
- Error and edge behavior: 未知枚举名反序列化失败由 Step 6 监听器按 `CACHE_EVENT_DESERIALIZE_FAILED` 记码丢弃。
- Standards impact: MC-NAME-001——类型与常量命名 PascalCase/SCREAMING_SNAKE。
- Implementation pseudocode:

```java
public enum EgonColaCacheChangedOperation {
    /** 回源写入后通告：对端仅逐出本地 L1。 */
    PUT,
    /** 精确键失效（双级）。 */
    EVICT,
    /** 租户段 glob 失效（双级），键形状仅 tenantId:*。 */
    PREFIX_EVICT
}
```

- Verification contribution: File 1/2 编译与用例矩阵的输入。
- Literal rule enforcement: Rule 9——操作集以枚举封闭，杜绝字符串硬编码分支。
- After this file: 仍缺信封主体，测试仍 RED。

#### File 4 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/model/EgonColaCacheNullValueBO.java`

- Purpose: 空值缓存哨兵（防穿透）。
- Symbols: `public record EgonColaCacheNullValueBO()` + `public static final EgonColaCacheNullValueBO INSTANCE`。
- Repository evidence: 主 Spec §10.3 定义"无字段 record 单例、PTV 白名单内（`top.egon.cola.` 前缀覆盖）"。
- Dependencies and consumers: Step 5 缓存读写分支、Step 7 `getByCache` null 语义。
- Why now: 内核与 codec 测试都引用它。
- Contract/signature changes: 新增类型。
- Input/output and state mapping: 写入 L1/L2 的 value 即 `INSTANCE`；读取命中哨兵 → 对外语义为 null（Step 5 的 `fromCache`
  翻译）。
- Error and edge behavior: 永不与真实值混淆：宿主值类型不允许为该 record（PTV 白名单外的业务类不受影响，`INSTANCE`
  仅由组件内部写入）。
- Standards impact: MC-MODEL-001——简单载体用无字段 record；MC-NAME-001——`BO` 后缀。
- Implementation pseudocode:

```java
public record EgonColaCacheNullValueBO() {
    public static final EgonColaCacheNullValueBO INSTANCE = new EgonColaCacheNullValueBO();
}
```

- Verification contribution: File 2 哨兵往返用例。
- Literal rule enforcement: Rule 1——POJO 命名规范 `BO` 后缀；Rule 3——不可变简单对象 record。
- After this file: 仍缺信封。

#### File 5 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedEvent.java`

- Purpose: 全模块唯一缓存事件信封 + 共享键规则 `KeyGuard`（PC-001 嵌套裁决）。
- Symbols:
  `record EgonColaCacheChangedEvent(int schemaVersion, String eventId, String originNodeId, Instant occurredAt, String cacheName, EgonColaCacheChangedOperation operation, List<String> keys)`
  ；嵌套 `static final class KeyGuard`：`static void requireValidName(String)`、`static void requireExactKey(String)`、
  `static void requireGlob(String)`、`static String tenantOf(String)`。
- Repository evidence: 主 Spec §10.2 字段序逐字冻结；键正则 `^\d+:\d+$`、glob `^\d+:\*$`、区域名 `[A-Za-z0-9_.-]{1,100}`
  （§13.2）；错误码常量集合 §13.2 冻结。
- Dependencies and consumers: File 1/2 测试；Step 5 发布；Step 6 监听器解码与端口守卫复用 `KeyGuard`。
- Why now: 信封是本 Step 测试的核心被测物。
- Contract/signature changes: 新增 record；紧凑构造器顺序校验：`schemaVersion == SCHEMA_VERSION(1)` 否则
  `IllegalStateException("CACHE_EVENT_UNSUPPORTED_SCHEMA")`；`eventId`/`originNodeId`/`cacheName` 非空白；`occurredAt`/
  `operation` 非 null；`keys` 非空且逐键按 operation 校验（PREFIX_EVICT 走 `requireGlob`，其余 `requireExactKey`）；
  `cacheName` 过 `requireValidName`；末了 `keys = List.copyOf(keys)`。
- Input/output and state mapping: 与 Redis 载荷的 JSON 字段名即 record 组件名（Jackson record 支持）；`SCHEMA_VERSION = 1`
  公开常量供发布方复用。
- Error and edge behavior: 守卫异常消息即错误码字面串（`IllegalArgumentException` 携 `CACHE_KEY_TENANT_MISMATCH`/
  `CACHE_GLOB_PATTERN_FORBIDDEN`/`CACHE_NAME_INVALID`）；`KeyGuard.requireSameTenant(cacheName, keys)` 供 Step 6 二次校验当前
  MDC 租户。
- Standards impact: MC-MODEL-001——record 紧凑构造器完成全部规范化；MC-NAME-001——嵌套守卫类 PascalCase。
- Implementation pseudocode:

```java
public record EgonColaCacheChangedEvent(int schemaVersion, String eventId, String originNodeId,
        Instant occurredAt, String cacheName, EgonColaCacheChangedOperation operation,
        List<String> keys) {
    public static final int SCHEMA_VERSION = 1;
    public EgonColaCacheChangedEvent {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalStateException("CACHE_EVENT_UNSUPPORTED_SCHEMA");
        // requireText(eventId)/requireText(originNodeId)/requireText(cacheName)+KeyGuard.requireValidName
        // occurredAt/operation/keys 非空；逐键: operation==PREFIX_EVICT ? KeyGuard.requireGlob : KeyGuard.requireExactKey
        keys = List.copyOf(keys);
    }
    public static final class KeyGuard {
        static final Pattern EXACT = Pattern.compile("^\\d+:\\d+$");
        static final Pattern GLOB = Pattern.compile("^\\d+:\\*$");
        static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_.-]{1,100}$");
        // requireExactKey/requireGlob/requireValidName 抛 IllegalArgumentException(错误码字面串)
        // requireTenant(String key, long currentTenantId): 前缀段不等 → CACHE_KEY_TENANT_MISMATCH
    }
}
```

- Verification contribution: File 1 全守卫用例。
- Literal rule enforcement: Rule 3——紧凑构造器数据规范化；Rule 10——`Instant`；Rule 9——键规则集中于单一守卫而非散落 if。
- After this file: 仅剩 codec 缺失。

#### File 6 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/codec/EgonColaCacheCodecs.java`

- Purpose: 构造受限多态的编解码器（RMapCache 值 codec + 事件 JSON mapper）。
- Symbols: `final class EgonColaCacheCodecs`：`static JsonJacksonCodec mapValueCodec()`、
  `static ObjectMapper eventMapper()`。
- Repository evidence: 主 Spec §13.3 白名单裁决（`top.egon.cola.`、`java.util`、`java.time`、`java.lang`）；redisson
  `JsonJacksonCodec` 在 provided 依赖内。
- Dependencies and consumers: Step 5 管理器构建 RMapCache 时传入 codec；Step 6 监听器编解码 topic 载荷（StringCodec +
  `eventMapper`）。
- Why now: File 2 测试的被测主体。
- Contract/signature changes: 新增工具类（类名非 BO 后缀——codec 属技术设施，遵循 spec §13.3 冻结命名）。
- Input/output and state mapping: `mapValueCodec`：`ObjectMapper` 注册 `JavaTimeModule`、
  `disable(WRITE_DATES_AS_TIMESTAMPS, FAIL_ON_UNKNOWN_PROPERTIES)`、`activateDefaultTyping(baseValidator, NON_FINAL)`；
  `eventMapper`：无默认多态，仅信封编解码。
- Error and edge behavior: 白名单外 `@class` 在解析阶段抛 `IOException` 族（不实例化）；两 mapper 均为线程安全共享实例（静态持有或每管理器一实例，由
  Step 5 决定实例化点）。
- Standards impact: MC-JSON-001——Jackson 单一 JSON 栈 + PTV 白名单；MC-UTIL-001——final 工具类私有构造。
- Implementation pseudocode:

```java
public final class EgonColaCacheCodecs {
    private EgonColaCacheCodecs() {}
    public static JsonJacksonCodec mapValueCodec() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        PolymorphicTypeValidator ptv = PolymorphicTypeValidator.builder()
            .allowIfSubType("top.egon.cola.").allowIfSubType("java.util.")
            .allowIfSubType("java.time.").allowIfSubType("java.lang.").build();
        mapper.activateDefaultTyping(ptv, DefaultTyping.NON_FINAL);
        return new JsonJacksonCodec(mapper);
    }
    public static ObjectMapper eventMapper() { /* 无默认类型；JavaTimeModule；禁未知属性 */ }
}
```

- Verification contribution: File 2 白名单/往返用例转绿。
- Literal rule enforcement: Rule 6——跨进程 JSON 一律 Jackson 且实体按需注解；Rule 5——仅 Jackson 白名单生态。
- After this file: 执行验证命令全绿后提交本 Step。

### Step 5 — 两级缓存内核与管理器

- Requirements: REQ-002, REQ-007, REQ-008, REQ-009, REQ-013
- Dependencies: Step 3（模块/Properties）、Step 4（信封/枚举/哨兵/codec）。
- Baseline state: 仓内 Redis 集成测试唯一模式实测为
  `egon-cola-components/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/store/redisson/RedissonStoreIntegrationTest.java`
  ——Testcontainers `GenericContainer(redis:7.4-alpine)` +
  `@EnabledIfSystemProperty(named="egon.access.guard.redis.it", matches="true")` 门控 + `Assumptions` Docker 可用性 + 双
  `RedissonClient`（天然满足本 Step 双节点需求）；父 pom 已管理 guava（:229-231）；`AbstractValueAdaptingCache` 在
  spring-boot-starter-cache(provided) 编译面。cache starter 对 testcontainers 的依赖经 Step 1 新增管理位以零版本声明（PC-006）。
- Observable outcome: `EgonColaTwoLevelCacheManager` 与 `EgonColaTwoLevelCache` 就绪；TEST-004（L1/仅L2/双miss
  三态与回填）、TEST-006（哨兵二次读零回源）、TEST-007（同 key 8 线程 loader 恰一次 + 超时降级）、TEST-008（固定种子
  TTL ∈ [expire, expire+jitter]，jitter=0 恒等）全绿；管理器句柄复用与区域名拒绝为纯单测（无 Docker 亦执行）。
- End state: Step 6 的监听器与端口实现直接调用 Manager 的 publish/applyLocalEviction/applyRemotePut/scheduler 原语。
- Test-first gate: Required — RED：三测试类因 Manager/Cache 符号缺失编译失败。
- Manual Checks: MC-MODEL-001, MC-PATTERN-001, MC-LOG-001, MC-TIME-001, MC-UTIL-001, MC-SCOPE-001, MC-TEST-001,
  MC-BLOCKER-001
- Literal Rules: Rule 3, Rule 4, Rule 5, Rule 9, Rule 10, Rule 11
- Ordered files: 夹具 → Manager 测试 → Cache 测试（含 Jitter 单测段）→ Manager 实现 → Cache 实现；实现两个类互相引用（同包），先写被测试锁定的装配方
  Manager，再写覆写模板的 Cache，一并编译。
- Validation working directory: 仓库根目录（Redis 集成用例需 Docker；无 Docker 环境由系统属性门控自动跳过，非门控用例仍须全绿）
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test -Degon.cola.cache.redis.it=true`
- Expected result: `EgonColaTwoLevelCacheManagerTest`、`EgonColaTwoLevelCacheTest` 与既有 Step 3/4 测试全绿，BUILD
  SUCCESS。
- Failure returns to: File 5（内核协议分支）。
- Completion criteria: 上列测试类全绿；grep 确认 L1 容量与 TTL 仅从 Properties 读取、降级分支错误码仅
  `CACHE_L2_OPERATION_FAILED` 一类。
- Rollback: 删除本 Step 五文件。
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/support/CacheRedisTestSupport.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCacheManagerTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCacheTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCacheManager.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCache.java`
- Commit: `feat(cache): implement two-level L1-Guava L2-RMapCache kernel with jitter and mutex load`

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/support/CacheRedisTestSupport.java`

- Purpose: Testcontainers Redis 共享夹具（单容器 + 每节点独立 `RedissonClient`/Manager 构造原语）。
- Symbols: `abstract class CacheRedisTestSupport`：静态 `GenericContainer` 容器字段、`static RedissonClient newClient()`、
  `static EgonColaCacheProperties props(Map<String,Object> overrides)`（默认键树 + `keyPrefix="egon:cola:cache:it"` 短
  TTL 覆盖）。
- Repository evidence: 逐要素镜像实测 `RedissonStoreIntegrationTest`（`@EnabledIfSystemProperty` :28、`GenericContainer` :
  31-32、`Assumptions.assumeTrue(DockerClientFactory...)` :40、双 client :34-35、`flushdb` 于 @AfterEach :49-52）。
- Dependencies and consumers: Step 5 File 2/3 与 Step 6 全部集成测试继承/引用；testcontainers(test) 依赖经 PC-006 管理位声明。
- Why now: 后续集成测试类的公共前置。
- Contract/signature changes: 新增测试支撑类。
- Input/output and state mapping: 门控属性名固定 `egon.cola.cache.redis.it`；容器镜像 `redis:7.4-alpine` 与既有先例同版。
- Error and edge behavior: Docker 缺失 → assume 跳过（套件仍绿）；每测试后 `flushdb` 隔离。
- Standards impact: MC-TEST-001——夹具集中防复制；MC-SCOPE-001——仅测试域。
- Implementation pseudocode:

```text
类级 @EnabledIfSystemProperty(named="egon.cola.cache.redis.it", matches="true")；
静态字段持有 GenericContainer（redis:7.4-alpine，暴露 6379）；
@BeforeAll：Assumptions.assumeTrue(Docker 可用) 后启动容器；
newClient()：Config.useSingleServer() 指向容器 host 与首个映射端口，Redisson.create 返回；
props(overrides)：new EgonColaCacheProperties() 后按 overrides 覆写 keyPrefix 与 ttl（Duration 短值）。
```

- Verification contribution: 全部 Redis 门控用例的运行环境。
- Literal rule enforcement: Rule 10——超时/TTL 全部 `Duration`。
- After this file: 夹具可编译（依赖 Step 4 类型），行为测试仍 RED。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCacheManagerTest.java`

- Purpose: 纯单测锁定管理器契约：句柄复用、区域名守卫、`getCacheNames`、originNodeId 派生。
- Symbols: `EgonColaTwoLevelCacheManagerTest`。
- Repository evidence: 无 Redis 依赖的组件单测先例充分（access-guard contract 测试同法直构造）。
- Dependencies and consumers: 消费 File 4。
- Why now: 先钉管理器 API 面（Step 6 依赖其签名）。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: `getCache("UserBO")` 两次同一实例；`getCache("bad name")`（含空格）→
  `IllegalArgumentException` 含 `CACHE_NAME_INVALID`；`nodeId` 配置非空时 `originNodeId()` 恒等、缺省时以 `node-` 前缀
  UUID 且进程内稳定；未触碰区域时 `getCacheNames()` 为空集。
- Error and edge behavior: 管理器构造以 Mockito mock `RedissonClient`（构造路径不触碰网络方法即不失败）。
- Standards impact: MC-TEST-001——断言逐分支；MC-BEAN-001——本类不经 Spring 直构造验证可测性。
- Implementation pseudocode:

```text
@Test returnsSameHandleForSameRegion：manager.getCache("UserBO") 两次 isSameAs；
@Test rejectsInvalidRegionName：assertThatThrownBy getCache("bad name") 含 CACHE_NAME_INVALID；
@Test configuredNodeIdWinsElseStableUuid：两种 props 下 originNodeId() 断言恒等/前缀 node- 且两次调用相等。
```

- Verification contribution: 步骤验证命令目标类。
- Literal rule enforcement: Rule 11——测试包镜像主源码 `core` 包。
- After this file: 编译失败（Manager 未建），预期 RED。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCacheTest.java`

- Purpose: TEST-004/006/007（门控集成）+ TEST-008（种子单测段）。
- Symbols: `EgonColaTwoLevelCacheTest extends CacheRedisTestSupport`。
- Repository evidence: 断言风格 AssertJ（先例文件实测 import）；并发夹具用
  `ExecutorService + CountDownLatch + AtomicInteger` JDK 原语（Rule 5 白名单）。
- Dependencies and consumers: 消费 File 4/5 与 Step 4 类型。
- Why now: 内核行为先固化。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: 三态矩阵——(a) put 后 get 双级在场（L1 断言 + L2 `fastGet` 断言）；(b) 清 L1 后 get 触发
  L2 命中并回填（loader 计 0）；(c) 双清后 get 走 loader=1、写双级；哨兵：loader 返回 null 两次 → 第二次 loader 计 0 且 L2
  值为哨兵序列化形态；互斥：8 线程同 key `CountDownLatch` 齐发，loader 内置 sleep(50ms) + `AtomicInteger`，断言恰
  1；降级：锁被外部持有超 `lock.wait-time` 时本线程仍返回权威值且缓存槽位空；抖动：`ttl.jitter-ratio=0.2`、种子 `Random(42)`
  ，100 次采样全落 `[expire, expire*1.2]` 且离散 > 0，ratio=0 时恒等。
- Error and edge behavior: 断言消息含场景前缀便于定位；Docker 缺失时仅抖动段与构造段执行。
- Standards impact: MC-TEST-001——矩阵化用例；MC-TIME-001——TTL 断言以 `Duration` 毫秒值比较。
- Implementation pseudocode:

```text
@Test l2HitBackfillsL1WithoutLoader：put("41:7") → L1 侧清空 → get(key, failLoader) 返回原值
    且 L1 快照含该键（TEST-004）；
@Test nullSentinelShortCircuitsSecondLoad：loader 计数 1→两次 get 均 null→L2 fastGet 为哨兵（TEST-006）；
@Test eightThreadsSameKeyLoadExactlyOnce：齐发后 counter==1 或超时线程降级值正确（TEST-007）；
@Test jitterWithinBoundedRangeWithSeededRandom：Jitter.jitteredMillis(base,0.2,Random(42)) 100 次
    全部 ∈ [base, base*1.2] 且离散>0；ratio=0 恒等（TEST-008）。
```

- Verification contribution: 步骤验证命令主目标。
- Literal rule enforcement: Rule 9——被测对象即模板方法/互斥协议实现，测试按协议态穷举；Rule 10——时间断言仅 `java.time`。
- After this file: 仍 RED（实现未建）。

#### File 4 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCacheManager.java`

- Purpose: `CacheManager` 实现；持有区域句柄映射、进程级 originNodeId、发布原语、延迟调度器与本地失效应用原语。
- Symbols: `class EgonColaTwoLevelCacheManager implements CacheManager`；方法 `getCache(String)`、`getCacheNames()`、
  `originNodeId()`、`properties()`、`l2(String cacheName)`（`RMapCache<String,Object>`）、
  `publish(String cacheName, EgonColaCacheChangedOperation operation, List<String> keys)`、
  `applyLocalEviction(String, List<String>)`、`applyLocalPrefixEviction(String, String globKey)`、
  `applyRemotePut(String, List<String>)`、`scheduler()`（`ScheduledExecutorService`）、`lock(String cacheName, String key)`（
  `RLock`）。
- Repository evidence: `RTopic`/`RMapCache`/`RLock` API 在 redisson(provided)；house style 构造注入 + `@Slf4j`；句柄
  `ConcurrentHashMap` 生命周期=应用上下文（spec §12 状态机行）。
- Dependencies and consumers: 被 Step 6 装配、监听器与端口注入；File 5 Cache 由本类创建并回调。
- Why now: Cache 的协作者与全部共享设施在管理器收口，签名先被 File 2 锁定。
- Contract/signature changes: 新增类。字段：`properties`、`redissonClient`、`handles`（region→Cache）、`l2Handles`
  （region→RMapCache 懒建，名 = `properties.keyPrefix + ":" + region`，codec=`EgonColaCacheCodecs.mapValueCodec()`）、
  `originNodeId = properties.nodeId 空白时 "node-" + UUID.randomUUID() 否则配置值`、`scheduler`（单线程 daemon，线程名
  `egon-cache-second-evict`）、topic 句柄 = `redissonClient.getTopic(properties.redis.topic, StringCodec.INSTANCE)`。
- Input/output and state mapping: `publish` 组信封（schemaVersion=1、eventId=`UUID.randomUUID().toString()`、occurredAt=
  `Instant.now()`、originNodeId 本机）→ `topic.publish(eventMapper JSON)`；锁键 =
  `keyPrefix + ":lock:" + cacheName + ":" + key`（spec §7.3 表行 5 的 `lock:region:tenantId:id` 形态）。
- Error and edge behavior: `getCache(非法名)` 经 `KeyGuard.requireValidName` 抛 `CACHE_NAME_INVALID`；publish 整段
  try/catch：失败记 WARN `CACHE_L2_OPERATION_FAILED`（region + 键计数 + originNodeId 前 8 位 + eventId 前缀，不落键明细——spec
  §11 日志裁决）；scheduler 关闭态拒收抛 `RejectedExecutionException` 由调用方吞并记 WARN（TEST-024 关联：停机丢弃）。
- Standards impact: MC-BEAN-001——本类经 Step 6 `@Bean("egonColaTwoLevelCacheManager")` 显式命名；MC-LOG-001——`@Slf4j`
  ；MC-PATTERN-001——发布/应用原语集中单类，杜绝散落 topic 操作。
- Implementation pseudocode:

```text
@Slf4j + @RequiredArgsConstructor（properties, redissonClient）；
getCache(name)：KeyGuard.requireValidName(name) → handles.computeIfAbsent(name, n -> new EgonColaTwoLevelCache(n, this))；
publish(name, op, keys)：try { event = new EgonColaCacheChangedEvent(SCHEMA_VERSION, uuid, originNodeId,
    Instant.now(), name, op, keys); topic.publish(EVENT_MAPPER.writeValueAsString(event)); }
  catch (Exception ex) { log.warn("CACHE_L2_OPERATION_FAILED region={} op={} keyCount={}", ...); }
applyLocalEviction(name, keys)：handle(name).evictLocal(keys)；
applyLocalPrefixEviction(name, glob)：L1 keySet removeIf 匹配 + L2 readAllKeySet 过滤后 fastRemove（ASM-003）；
applyRemotePut(name, keys)：仅 L1 invalidate（DEC-007）；
lock(name, key)：redissonClient.getLock(keyPrefix + ":lock:" + name + ":" + key)。
```

- Verification contribution: File 2 全部断言 + File 3 的发布观测面。
- Literal rule enforcement: Rule 4——`@Slf4j` + `@RequiredArgsConstructor` 构造注入两协作者；Rule 10——`Instant`/`Duration`
  ；Rule 3——结构类非载体不套 `@Data`。
- After this file: Cache 未建，模块仍不可编译（预期中间态，与 File 5 一并转绿）。

#### File 5 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/core/EgonColaTwoLevelCache.java`

- Purpose: 两级读写内核：模板方法缓存实现 + 嵌套 `Jitter`（PC-001）+ 互斥回源 + 哨兵 + 降级。
- Symbols: `class EgonColaTwoLevelCache extends AbstractValueAdaptingCache`；覆写
  `getName/getNativeCache/lookup/put/evict/clear` 与 `get(Object, ValueLoader)`；嵌套
  `static final class Jitter { static long jitteredMillis(long baseMillis, double ratio, Random random) }`；包内方法
  `evictLocal(List<String>)`、`applyPrefixLocally(String globKey)`。
- Repository evidence: Guava `CacheBuilder`/`Expiry`（父 dmu guava :229-231）；`AbstractValueAdaptingCache` 由
  spring-context 提供（starter-cache provided）；L2 逐条 TTL 用 `RMapCache#fastPut(key, value, ttl, TimeUnit)`（redisson
  3.26 API）。
- Dependencies and consumers: 仅经 Manager 取 L2/topic/scheduler/lock/properties（构造参数=cacheName+manager）；Step 6 装配与
  `getByCache` 路径最终命中本类。
- Why now: 行为测试（File 3）已锁协议，实现按分支落位。
- Contract/signature changes: 新增类；键一律字符串并要求 `KeyGuard.requireExactKey` 形状（非 `tenantId:id`
  形状直接拒绝读写——缓存协议键与租户规则同源；`allowNullValues=true`）。
- Input/output and state mapping: `lookup(k)`：L1 `getIfPresent` 命中即返（含哨兵）；miss → L2 `fastGet(k)`（异常记
  `CACHE_L2_OPERATION_FAILED` WARN 按 miss 处理）→ 命中回填 L1；`toStoreValue(null)` →
  `EgonColaCacheNullValueBO.INSTANCE`；`fromStoreValue(哨兵)` → null；TTL 抽样：单次
  `Jitter.jitteredMillis(base, ratio, randomSeed)` 产出 ∈ [base, base*(1+ratio)]，同值共享给 L1（Guava 逐条 deadline）与 L2
  `fastPut`（PC-004，L1≤L2 不变量之等值特例）。
- Error and edge behavior: `get(key, ValueLoader)` 双 miss → `manager.lock(name,k).tryLock(waitTime, leaseTime, MILLIS)`
  ：获锁 → recheck L1/L2 → 仍 miss 才 `valueLoader.call()` → 双级写入 + `publish(PUT)`，finally 仅持锁者 unlock；未获锁（等待超时）→
  直接 `valueLoader.call()` 返回且不写缓存（降级防排队）；loader 异常原样传播（业务语义同 `getById`）；`put` 写双级 +
  `publish(PUT)`；`evict` 本地双删 + `publish(EVICT)`；`clear()` 本节点 L1 全清 + L2 `readAllKeySet`→`fastRemove`
  批删、不发全局事件（spec §10.5 宿主逃生口）；批删单键失败逐项 catch 计数（spec §7.2 行 7）。
- Standards impact: MC-PATTERN-001——Template Method（继承 `AbstractValueAdaptingCache` 骨架）+ DCL 互斥回源，为 spec §13.1
  点名模式落地；MC-UTIL-001——随机仅 JDK `Random`（种子可注入供 TEST-008）；MC-LOG-001——`@Slf4j` 降级日志带稳定码。
- Implementation pseudocode:

```text
@Slf4j；字段 name、manager、l1 = CacheBuilder.maximumSize(properties.l1.maxSize)
    .expireAfter(逐条 deadline：ConcurrentHashMap 侧录 expireAtNanos，Expiry 读取) 构建；
lookup(k)：v = l1.getIfPresent(k)；命中返回；否则 try v = manager.l2(name).fastGet(k)
    catch → WARN CACHE_L2_OPERATION_FAILED 按 miss；v 非空 → l1.put(k, v)（回填）；返回 v；
get(k, ValueLoader v)：形状守卫 requireExactKey；命中路径同上；双 miss →
    lk = manager.lock(name, k)；lk.tryLock(waitTime, leaseTime) 成功 →
        try { recheck；仍 miss → value = v.call()；ttlMillis = Jitter.jitteredMillis(...)；
              l1 写（带 deadline）；l2.fastPut(k, toStoreValue(value), ttlMillis, MILLIS)；
              manager.publish(name, PUT, List.of(k))；return value } finally 持锁者 unlock；
    获锁失败 → return v.call()（不写缓存，REQ-009 降级）；
put(k, value)：双级写入（jitter 同值）+ publish(PUT)；
evict(k)：l1.invalidate + l2.fastRemove + publish(EVICT)；
clear()：l1.invalidateAll + l2.readAllKeySet→fastRemove，不发事件；
Jitter.jitteredMillis(base, ratio, rnd)：base + (long)(base * ratio * rnd.nextDouble())。
```

- Verification contribution: File 3 三态/哨兵/互斥/抖动全部分支。
- Literal rule enforcement: Rule 3——内核为结构类用普通类 + 构造注入；哨兵/信封复用 record；Rule 9——两级协同与互斥按模式实现而非
  if-per-cache 硬编码；Rule 10——TTL 全链路 `Duration`，毫秒换算集中于 `Jitter`。
- After this file: 执行验证命令，File 2/3 转绿后提交本 Step。

### Step 6 — 通用事件监听器、缓存端口实现与自动装配

- Requirements: REQ-003, REQ-005, REQ-011, REQ-012, REQ-013, REQ-018, REQ-025
- Dependencies: Step 2（端口接口）、Step 4（信封/KeyGuard/codec）、Step 5（Manager/Cache 原语）。
- Baseline state: Step 5 夹具与 Manager 原语就绪；`SmartLifecycle`/`TransactionSynchronizationManager` 在
  spring-context/spring-tx 编译面；`@AutoConfiguration` 与 `AutoConfiguration.imports` 机制同 access-guard 模块实测剖面；MDC
  读取零新依赖（slf4j 随 spring-boot-starter 传递）。
- Observable outcome: 全模块唯一订阅点（静态扫描 + 装配测试断言 `addListener` 恰一处）；TEST-003（自回声/坏
  JSON/未知操作/多区域混合）、TEST-009（提交后恰一次双级失效 + 恰 1 条 EVICT + 延迟第二拍 1 次且不再发布）、TEST-010（MDC
  租户桩下跨租户键/glob 登记期拒绝）、TEST-011/012（双节点 PUT 仅逐出对端 L1、EVICT 双级皆除、glob
  只清本租户）、TEST-013（回滚零交互 + 事件重复乱序应用幂等）、TEST-014（停容器后业务零异常、仅 `CACHE_L2_OPERATION_FAILED`
  日志）、TEST-016（`get`/`getAll` 断连降级返回权威值）、TEST-020（enabled 缺省/false/true × 有/无唯一客户端矩阵；true 无客户端启动抛
  `CACHE_REDISSON_CLIENT_MISSING`；宿主自定义 `CacheManager` 时让位）全绿。
- End state: starter 完整可用；`egonColaCachePort` bean 成为 Step 7 repo 的唯一接合点。
- Test-first gate: Required — RED：四测试类因 Listener/Port/AutoConfiguration 符号缺失编译失败。
- Manual Checks: MC-BEAN-001, MC-ARCH-001, MC-PATTERN-001, MC-LOG-001, MC-JSON-001, MC-VALID-001, MC-SCOPE-001,
  MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 2, Rule 4, Rule 6, Rule 9, Rule 11
- Ordered files: 四测试（File 1~4）→ 监听器（File 5）→ 端口实现（File 6）→ 自动装配（File 7）→ imports（File 8）→ README（File 9）。
- Validation working directory: 仓库根目录（需 Docker 的门控用例同 Step 5 开关）
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test -Degon.cola.cache.redis.it=true`
- Expected result: 模块全部测试类（含 Step 3~5 既有）绿，BUILD SUCCESS。
- Failure returns to: File 5/File 6（分发与登记协议分支）。
- Completion criteria: 上列矩阵全绿 + `grep -c "addListener" 模块主源码` 合计为 1 + imports 文件恰 1 行。
- Rollback: 删除本 Step 九文件。
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedListenerTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/port/EgonColaTwoLevelCachePortEvictionIntegrationTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/port/EgonColaCacheClusterConvergenceTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/autoconfigure/EgonColaCacheAutoConfigurationTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedListener.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/port/EgonColaTwoLevelCachePort.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/autoconfigure/EgonColaCacheAutoConfiguration.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/README.md`
- Commit: `feat(cache): add generic change-event listener, eviction port and autoconfiguration`

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedListenerTest.java`

- Purpose: TEST-003——监听器分发、容错、自回声与"单一 listener"约束。
- Symbols: `EgonColaCacheChangedListenerTest`（Mockito 直构造 Manager 桩 + 捕获 topic 注册回调）。
- Repository evidence: access-guard contract 单测直构造先例；信封 JSON 样本经 Step 4 `eventMapper()` 手工生成（含篡改载荷）。
- Dependencies and consumers: 消费 File 5；Step 7 无涉。
- Why now: 分发协议先固化。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: 样本集——EVICT(`41:7`) 触发 manager.applyLocalEviction 恰一次；PREFIX_EVICT(`41:*`) 触发
  applyLocalPrefixEviction；PUT 触发 applyRemotePut（非双级）；originNodeId=本机 → 三原语零调用；坏 JSON/未知 operation
  名/白名单外 `@class` → 零调用 + ERROR 日志含 `CACHE_EVENT_DESERIALIZE_FAILED`（ListAppender 捕获）；多区域混合两条消息按
  cacheName 各归各。
- Error and edge behavior: 监听回调抛任何异常都不得逃逸到 Redisson 线程（断言 handle 不抛）。
- Standards impact: MC-TEST-001——分支穷举；MC-SCOPE-001——仅测试域。
- Implementation pseudocode:

```text
@Test evictAppliesDualLevelLocally：deliver(jsonOf(EVICT, "UserBO", ["41:7"])) →
    verify(manager).applyLocalEviction("UserBO", List.of("41:7"))；
@Test putOnlyDropsRemoteL1：verify(manager).applyRemotePut(...) 且无 applyLocalEviction；
@Test selfEchoSkipped：originNodeId 同本机 → verifyNoInteractions；
@Test badPayloadLoggedAndDropped：deliver("not-json") → 无交互 + 日志含 CACHE_EVENT_DESERIALIZE_FAILED；
@Test singleSubscriptionOnly：反射/参数捕获断言 topic.addListener 恰被调用一次。
```

- Verification contribution: 步骤验证命令目标类。
- Literal rule enforcement: Rule 9——分发按枚举 switch 穷尽，测试锁三种操作各一条通路。
- After this file: RED（Listener 未建）。

#### File 2 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/port/EgonColaTwoLevelCachePortEvictionIntegrationTest.java`

- Purpose: TEST-009/010/013/024——提交失效全链路、租户守卫、回滚零副作用、批上限。
- Symbols: `EgonColaTwoLevelCachePortEvictionIntegrationTest extends CacheRedisTestSupport`。
- Repository evidence: `TransactionTemplate` 驱动真实事务同步器；事件计数经独立测试订阅者（与产品监听器隔离的原始 `RTopic`
  addListener，仅测试文件内）；MDC 桩用 `MDC.putCloseable("tenantId", "41")`。
- Dependencies and consumers: 消费 File 6/5/7（经手工装配 Manager+Port+Listener，不启 Spring 上下文以精确计数）。
- Why now: 端口登记协议是 REQ-005/REQ-025 的核心证明。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: (a) 事务内 `registerEvictionAfterCommit("UserBO", ["41:7"], [])`：提交前对 Redis
  零交互计数；提交后 L1/L2 删除恰 1 次、EVICT 事件恰 1 条、`second-evict-delay=PT0.1S` 下第二拍再删 1 次且不再发事件；(b)
  回滚 → 四项计数全 0；(c) 非事务上下文登记 → 立即执行且 WARN 一条；(d) MDC=41 下键 `42:7` → 登记期抛
  `CACHE_KEY_TENANT_MISMATCH`、glob `*`/`41:7:*` → `CACHE_GLOB_PATTERN_FORBIDDEN`、MDC 缺失 → 同 mismatch
  码（fail-closed，PC-005）；(e) `getAll` keys 超 `batch.max-keys` → `CACHE_KEY_SIZE_EXCEEDED`；scheduler 关停后延迟第二拍 →
  `RejectedExecutionException` 吞并记 WARN 且主失效已完成。
- Error and edge behavior: 停 Redis（`REDIS.stop()`）后登记+提交 → 业务无异常、仅 `CACHE_L2_OPERATION_FAILED`（TEST-014
  关联行）。
- Standards impact: MC-TEST-001——计数式精确断言；MC-VALID-001——守卫在登记期即抛。
- Implementation pseudocode:

```text
@Test commitTriggersExactlyOneEvictOnePublishOneDelayedReplay()：
    txTemplate.execute { repoSave; port.registerEvictionAfterCommit("UserBO", List.of("41:7"), List.of()) }；
    assert evictCount(l1+l2)==1 && evictEventCount==1 && after(150ms) secondShotCount==1 && eventTotal==1；
@Test rollbackProducesZeroInteractions()：executeAndRollback → 全部计数 0；
@Test rejectsCrossTenantRegistration()：MDC=41 + key "42:7" → assertThatThrownBy 含 CACHE_KEY_TENANT_MISMATCH；
@Test rejectsIllegalGlobsAndOversizedBatch()：... CACHE_GLOB_PATTERN_FORBIDDEN / CACHE_KEY_SIZE_EXCEEDED。
```

- Verification contribution: 步骤验证命令主目标。
- Literal rule enforcement: Rule 2——守卫即原生校验语义（错误码消息），不自定义注解。
- After this file: 仍 RED（Port 未建）。

#### File 3 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/port/EgonColaCacheClusterConvergenceTest.java`

- Purpose: TEST-011/012——双节点事件收敛与前缀失效的租户边界。
- Symbols: `EgonColaCacheClusterConvergenceTest extends CacheRedisTestSupport`（两个独立 Manager+Listener 实例共享一个容器，各用独立
  `RedissonClient`）。
- Repository evidence: 先例 `RedissonStoreIntegrationTest` 的 firstClient/secondClient 双客户端结构（:34-35 实测）。
- Dependencies and consumers: 消费 File 5/6/7 装配（手工 new）。
- Why now: 集群协议验证依赖监听器与端口双全。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: 节点 A `port.registerEvictionAfterCommit`（经事务）后节点 B：EVICT 键双级皆无；PUT 键 B 的
  L1 无而 L2 保留（DEC-007）；PREFIX_EVICT：B 上租户 42 全部键清除、租户 43 对照键分毫未动（TEST-012）；丢弃事件模拟（B
  侧监听器暂停窗口内发布）→ B 旧值至迟在短 TTL（夹具 `ttl.expire=PT1S`）后消失（TEST-013 收敛面）。
- Error and edge behavior: Awaitility 式轮询禁用（零新依赖），用 `CountDownLatch` 于监听测试钩子或轮询 + `Thread.sleep` JDK
  原语，超时上限 2s。
- Standards impact: MC-TEST-001——跨节点断言显式建模两节点；MC-UTIL-001——等待逻辑仅 JDK。
- Implementation pseudocode:

```text
@Test remoteEvictClearsBothLevelsOnPeer：A 提交失效 "42:7" → 轮询断言 B.l1 无 && B.l2.fastGet 无；
@Test remotePutOnlyDropsPeerL1：A put → B L1 被逐出、L2 值仍在；
@Test prefixEvictRespectsTenantBoundary：B 预填 42:* 与 43:* 各 3 键 → A 发 PREFIX_EVICT "42:*"
    → 42 全清、43 原样。
```

- Verification contribution: 步骤验证命令目标类。
- Literal rule enforcement: Rule 9——收敛协议经 Observer 模式落地并由测试穷举。
- After this file: 仍 RED（装配未建）。

#### File 4 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/test/java/top/egon/cola/component/common/cache/autoconfigure/EgonColaCacheAutoConfigurationTest.java`

- Purpose: TEST-020 装配矩阵 + fail-fast + 让位 + 零影响。
- Symbols: `EgonColaCacheAutoConfigurationTest`（`ApplicationContextRunner`）。
- Repository evidence: access-guard `AccessGuardRedissonAutoConfigurationTest`（实测存在于 autoconfigure 测试包）同类矩阵方法。
- Dependencies and consumers: 消费 File 7/8（imports 经 `withConfiguration(AutoConfigurations.of(...))` 或直接类引用）。
- Why now: 装配面最后一组测试先行。
- Contract/signature changes: 新增测试类。
- Input/output and state mapping: 缺省（无 enabled 键）与 `enabled=false` → 三 bean 全不存在；`enabled=true` + 上下文含唯一
  `RedissonClient` mock → `egonColaTwoLevelCacheManager`/`egonColaCacheChangedListener`/`egonColaCachePort` 齐备且 bean
  名精确；`enabled=true` 无客户端 → 启动失败且异常消息含 `CACHE_REDISSON_CLIENT_MISSING`；`enabled=true` + 宿主自定义
  `CacheManager` bean → 管理器让位（isNotDisplayed 或同一实例为宿主）；同名 bean `redissonClient` 存在但另有第二客户端
  bean → by-name 命中不抛非唯一。
- Error and edge behavior: 上下文关闭无泄漏异常（SmartLifecycle stop 幂等）。
- Standards impact: MC-BEAN-001——bean 名逐一断言；MC-TEST-001——矩阵参数化。
- Implementation pseudocode:

```text
runner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(EgonColaCacheAutoConfiguration.class));
@Test disabledByDefaultShowsNoBeans()；@Test enabledWithoutClientFailsFast()
    → assertThatExceptionOfType(IllegalStateException.class) 含 CACHE_REDISSON_CLIENT_MISSING；
@Test enabledWithUniqueClientRegistersThreeNamedBeans()；
@Test hostCacheManagerWins()：.withBean("custom", CacheManager.class,...) → 无 egonColaTwoLevelCacheManager；
@Test namedRedissonClientBeatsNonUnique()。
```

- Verification contribution: 步骤验证命令目标类。
- Literal rule enforcement: Rule 4——全部断言针对显式 bean 名。
- After this file: 仍 RED（AutoConfiguration 未建）。

#### File 5 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/event/EgonColaCacheChangedListener.java`

- Purpose: 全模块唯一 RTopic 订阅与通用分发（嵌套 `EventBus`，PC-001）。
- Symbols: `class EgonColaCacheChangedListener implements SmartLifecycle`；嵌套
  `static final class EventBus { static void apply(EgonColaCacheChangedEvent event, EgonColaTwoLevelCacheManager manager) }`
  ；字段 `listenerId`、`running`（AtomicBoolean）。
- Repository evidence: `SmartLifecycle` 保证订阅发生在上下文就绪后；`StringCodec` topic 由 Manager 持有（Step 5 File
  4），本类只 `addListener(String.class, ...)` 一次。
- Dependencies and consumers: 消费者为 Redisson 事件线程；调用 Manager 三原语；File 4 装配之。
- Why now: 分发器在测试固化后被实现。
- Contract/signature changes: 新增类。构造参数（`@RequiredArgsConstructor` + `@Qualifier` 传导）：manager、properties。
- Input/output and state mapping: 收到字符串载荷 → `EVENT_MAPPER.readValue(json, EgonColaCacheChangedEvent.class)`（未知
  operation/非 1 版本在 mapper 或紧凑构造器处抛）→ try/catch 全包：解析失败 ERROR `CACHE_EVENT_DESERIALIZE_FAILED`（载荷前
  200 字符入日志，不落整包键明细，spec §11）；`originNodeId == manager.originNodeId()` → return（自回声）；`EventBus.apply` 按
  operation 枚举 switch 穷尽分发（default 分支抛 `IllegalStateException` 被外层捕获记同一码）。
- Error and edge behavior: `stop()` 反注册 `topic.removeListener(listenerId)` 且置 running=false；重复 start 幂等；回调线程绝不外抛。
- Standards impact: MC-BEAN-001——bean 名 `egonColaCacheChangedListener`（File 7 声明）；MC-PATTERN-001——Observer + 枚举分发，杜绝
  per-cache listener（用户原话约束的落地单点）；MC-LOG-001——`@Slf4j`。
- Implementation pseudocode:

```text
start()：if running.compareAndSet(false,true) then listenerId =
    manager.topic().addListener(String.class, (channel, body) -> onMessage((String) body))；
onMessage(String body)：try { var e = EVENT_MAPPER.readValue(body, EgonColaCacheChangedEvent.class);
    if (manager.originNodeId().equals(e.originNodeId())) return; EventBus.apply(e, manager); }
  catch (Exception ex) { log.error("CACHE_EVENT_DESERIALIZE_FAILED payloadPrefix={}", prefix(body,200), ex); }
EventBus.apply(e, manager)：switch (e.operation()) {
    case EVICT -> manager.applyLocalEviction(e.cacheName(), e.keys());
    case PREFIX_EVICT -> e.keys().forEach(g -> manager.applyLocalPrefixEviction(e.cacheName(), g));
    case PUT -> manager.applyRemotePut(e.cacheName(), e.keys()); }
stop()：manager.topic().removeListener(listenerId); running.set(false);
```

- Verification contribution: File 1 全部分支 + File 3 对端应用。
- Literal rule enforcement: Rule 9——通用分发单点模式化；Rule 4——`@Slf4j` + 显式 bean 名 + 构造注入。
- After this file: 监听器就绪；端口与装配仍缺。

#### File 6 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/port/EgonColaTwoLevelCachePort.java`

- Purpose: common-core 端口的两级缓存实现：守卫、after-commit 登记、双失效重放、批量组装（嵌套 `EvictionBuffer`，PC-001）。
- Symbols: `class EgonColaTwoLevelCachePort implements EgonColaCachePort`；嵌套
  `static final class EvictionBuffer { void offer(String cacheName, Collection<String> exactKeys, Collection<String> globs); List<Pending> drain() }`
  （Pending 为其嵌套 record）；私有 `long currentTenantOrThrow()`（读 `MDC.get(properties.getTenantMdcKey())`）。
- Repository evidence: 08-25 Spec 的 after-commit 缓冲先例 `OutboxAfterCommitBuffer`（spec §13.1 镜像声明）；
  `TransactionSynchronizationManager.isSynchronizationActive()` + `registerSynchronization` 为 spring-tx 标准面（依赖已在
  Step 3 pom）。
- Dependencies and consumers: 注入 manager + properties；被 Step 7 模板 repo 经 `ObjectProvider` 取用；File 2/3 测试消费。
- Why now: 复用 Listener 同域的 Manager 原语，装配前最后一片行为逻辑。
- Contract/signature changes: 新增类，实现 Step 2 三方法签名逐字不变。
- Input/output and state mapping: `get(name,key,loader)`：`KeyGuard.requireValidName` + `requireExactKey` + 租户段等于
  `currentTenantOrThrow()`（缺失/不等 → `CACHE_KEY_TENANT_MISMATCH`，PC-005）→
  `((EgonColaTwoLevelCache) manager.getCache(name)).get(key, loader::get)` 语义（走 Step 5 互斥回源）；
  `getAll(name,keys,loader)`：size > `batch.maxKeys` → `CACHE_KEY_SIZE_EXCEEDED`；整批租户守卫一次；结果 ArrayList 与 keys
  等长同序，逐 key 调 `get` 复用互斥；`registerEvictionAfterCommit`：登记期守卫全部键/glob（跨租户
  `CACHE_KEY_TENANT_MISMATCH`、非法 glob `CACHE_GLOB_PATTERN_FORBIDDEN`）→ `EvictionBuffer.offer`；事务激活 →
  `registerSynchronization(afterCommit → flush(buffer))`；非事务 → 立即 flush + WARN "no-transaction immediate eviction"
  ；flush：逐项 本地双级失效（L1+L2）+ `publish(EVICT 或 PREFIX_EVICT)`，随后
  `scheduler().schedule(同键位再本地双级失效（不再发布）, secondEvictDelay)`，`RejectedExecutionException` 吞并记
  WARN；afterCommit 内任何异常吞并记码不反抛（REQ-025：事务已提交不得污染提交线程）。
- Error and edge behavior: 同事务重复登记同键 → 缓冲合并去重（幂等删除语义）；drain 后缓冲空。
- Standards impact: MC-ARCH-001——端口实现位于 starter，common-core 保持零实现；MC-VALID-001——登记期守卫
  fail-fast；MC-BEAN-001——bean 名 `egonColaCachePort`。
- Implementation pseudocode:

```text
registerEvictionAfterCommit(name, exactKeys, globs)：
    tenant = currentTenantOrThrow(); KeyGuard.requireValidName(name);
    exactKeys.forEach(k -> { KeyGuard.requireExactKey(k); requireTenantSegment(k, tenant); });
    globs.forEach(g -> { KeyGuard.requireGlob(g); requireTenantSegment(g, tenant); });
    buffer.offer(name, exactKeys, globs);
    if (TransactionSynchronizationManager.isSynchronizationActive())
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { flush(buffer.drain()); } });
    else { log.warn("no transaction context; immediate eviction applied"); flush(buffer.drain()); }
flush(items)：for item：try { manager.applyLocalEviction/applyLocalPrefixEviction(item)；
        manager.publish(item.name, item.op, item.keys)；
        try { manager.scheduler().schedule(() -> localOnlyEvict(item), secondEvictDelay) }
        catch (RejectedExecutionException rex) { log.warn("CACHE_L2_OPERATION_FAILED scheduler closed") } }
    catch (Exception ex) { log.warn("CACHE_L2_OPERATION_FAILED region={}", item.name, ex); }
```

- Verification contribution: File 2 全部计数断言 + File 3 A 侧触发。
- Literal rule enforcement: Rule 2——守卫用既有 KeyGuard/原生异常语义；Rule 9——缓冲/重放以 Buffer+Scheduler 模式结构化而非散写；Rule
  4——`@Slf4j` + 构造注入 `@Qualifier`。
- After this file: 端口就绪，待装配。

#### File 7 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/java/top/egon/cola/component/common/cache/autoconfigure/EgonColaCacheAutoConfiguration.java`

- Purpose: 条件装配三 bean 并解析宿主 RedissonClient。
- Symbols: `EgonColaCacheAutoConfiguration`：`@AutoConfiguration`、
  `@EnableConfigurationProperties(EgonColaCacheProperties.class)`、
  `@ConditionalOnProperty(prefix="egon.cola.component.cache", name="enabled", havingValue="true")`（无
  matchIfMissing——缺省即关，REQ-001）；bean 方法 `egonColaTwoLevelCacheManager`（
  `@ConditionalOnMissingBean(CacheManager.class)`）、`egonColaCacheChangedListener`、`egonColaCachePort`。
- Repository evidence: access-guard 自动配置类的 `@ConditionalOnProperty` + 显式 bean 名剖面（实测其 autoconfigure 包与
  imports 文件各 1 处注册）。
- Dependencies and consumers: 消费 Step 3~6 全部类型；File 4 测试矩阵与宿主应用消费。
- Why now: 全部组件类就绪后一次接线。
- Contract/signature changes: 新增类。客户端解析私有方法
  `resolveRedissonClient(ObjectProvider<RedissonClient> provider)`：`provider.getIfAvailable()` 前先按名取
  `provider.stream().filter(p -> "redissonClient".equals(p.getBeanName()))` 或
  `ConfigurableListableBeanFactory.getBeanProvider` 语义的 by-name 优先（实现取 `provider.stream()` 过滤名匹配，无则
  `getIfUnique()`）；两者皆空 → 抛 `IllegalStateException("CACHE_REDISSON_CLIENT_MISSING")`（fail-fast，DEC-004，绝不创建客户端）。
- Input/output and state mapping: manager 构造 `(properties, redissonClient)`；listener 构造 `(manager, properties)`；port
  构造 `(manager, properties)`；bean 间以方法参数注入。
- Error and edge behavior: `enabled=false`/缺省 → 条件短路零 bean；宿主已有任意 `CacheManager` → 管理器让位但监听器与端口仍装配？——否：三
  bean 同属本配置，管理器让位时端口经 `ObjectProvider<CacheManager>` 取宿主 bean 会破坏两级协议，故
  `@ConditionalOnMissingBean(CacheManager.class)` 置于**配置类级**（连同端口与监听器整体让位），与 TEST-020"让位"断言一致。
- Standards impact: MC-BEAN-001——三 bean 显式命名；MC-ARCH-001——自动配置零业务逻辑，仅接线。
- Implementation pseudocode:

```text
@AutoConfiguration @ConditionalOnProperty(prefix = "egon.cola.component.cache", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean(CacheManager.class) —— 类级让位
@EnableConfigurationProperties(EgonColaCacheProperties.class)
class EgonColaCacheAutoConfiguration {
  @Bean("egonColaTwoLevelCacheManager") EgonColaTwoLevelCacheManager manager(
      EgonColaCacheProperties p, ObjectProvider<RedissonClient> clients) {
      return new EgonColaTwoLevelCacheManager(p, resolveRedissonClient(clients)); }
  @Bean("egonColaCacheChangedListener") ... new EgonColaCacheChangedListener(manager, p);
  @Bean("egonColaCachePort") ... new EgonColaTwoLevelCachePort(manager, p);
}
```

- Verification contribution: File 4 矩阵全部行。
- Literal rule enforcement: Rule 4——bean 名逐一显式声明；Rule 11——自动配置剖面与组件 starter 家族一致。
- After this file: 装配就绪，imports 未注册。

#### File 8 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: 发布自动配置入口。
- Symbols: 单行 FQCN `top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheAutoConfiguration`。
- Repository evidence: access-guard 模块同名路径文件实测存在且每配置一行（其 2 行先例）。
- Dependencies and consumers: Spring Boot 自动配置加载器；宿主 classpath 即生效。
- Why now: 类就绪后注册才可加载。
- Contract/signature changes: 新文件恰 1 行。
- Input/output and state mapping: 文件缺失 → bean 不装配（REQ-011 零影响语义的另一半保险）。
- Error and edge behavior: 拼错 FQCN → 装配静默缺席，由 File 4 runner 断言拦截（其经类引用不受影响），故完成判据含"
  生成工程启动冒烟"于 Step 9~11。
- Standards impact: MC-SCOPE-001——单行资源文件。
- Implementation pseudocode:

```text
# 文件恰 1 行：自动配置类 FQCN，无空行、无注释行、无尾随空白（access-guard 同名文件每配置一行的先例）：
top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheAutoConfiguration
# Properties 不登记于此（经 AutoConfiguration 的 EnableConfigurationProperties 挂载）；
# FQCN 拼错或文件缺失 → 装配静默缺席，File 4 runner 断言与 Steps 9~11 生成工程冒烟双重拦截
```

- Verification contribution: 步骤"imports 恰 1 行"判据。
- Literal rule enforcement: Rule 11——组件 starter 资源剖面既有约定。
- After this file: starter 功能完整，文档待补。

#### File 9 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/README.md`

- Purpose: 模块使用文档（开关语义、键规则、集群要求、错误码表）。
- Symbols: 无代码符号；章节：Quick start / Configuration keys（13 键表）/ Key & tenancy rules / Cluster requirements（共享
  Redis + 每节点自动订阅）/ Degradation & error codes / Zero-impact guarantee。
- Repository evidence: 组件模块 README 先例（`egon-cola-components/README.md` 与 common 聚合 README 实测存在，模块级
  README 书写风格随 access-guard）。
- Dependencies and consumers: 宿主开发者与 Step 9~11 archetype README 引用入口。
- Why now: 功能冻结后一次性成文防漂移。
- Contract/signature changes: 新文档。
- Input/output and state mapping: 文档命令与键表逐字复制 spec §16 键树与 §13.2 错误码表。
- Error and edge behavior: 文档含"未启用时 repo 行为与基线逐字节一致"声明（REQ-011 对外承诺）。
- Standards impact: MC-CONFIG-001——键表与 Properties 逐键一致。
- Implementation pseudocode:

```text
# 章节顺序与内容要点：
Quick start：enabled=true + 宿主唯一 RedissonClient 前置的最小接入段落；
Configuration keys：13 键表（键/默认值/含义），与 EgonColaCacheProperties 逐键一致；
Key & tenancy rules：tenantId:id 形状正则、glob 白名单与 CACHE_* 错误码全表；
Cluster requirements：共享 Redis + 每节点恰一订阅（通用事件协议时序文字版）；
Degradation & error codes / Zero-impact guarantee：enabled=false 或未引 starter 时行为与基线逐字节一致（REQ-011 对外承诺）。
```

- Verification contribution: README 命令可复制执行由 Step 9~11 模板装配测试间接背书。
- Literal rule enforcement: Rule 7——文档中键示例与 yml 键块结构一致。
- After this file: 执行验证命令九文件转绿后提交本 Step。

### Step 7 — mp-ext 仓库层缓存增强与围栏原子修订

- Requirements: REQ-005, REQ-006, REQ-010, REQ-011, REQ-012, REQ-017, REQ-025
- Dependencies: Step 2（`EgonColaCachePort` 接口，mp-ext pom :62 已依赖 common-core 零新增）；Step 6 仅在运行期集成场景需要端口
  bean，本 Step 模块内测试以 mock 端口自足。
- Baseline state: `EgonColaRepository.java` 实测 15 个受控写方法（`save:68`、`saveBatch:75`、`saveOrUpdateBatch:87`、
  `removeById:118/124/130`、`removeByMap:138`、`remove:143`、`removeByIds:148/187`、`updateById:193`、`update:201/206`、
  `updateBatchById:215`、`saveOrUpdate:227`），其余批式/链式重载经实测全部委托这 15 个（如 :449 `saveBatch(Collection)` → :
  75）；`getById:234`/`listByIds:245` 为读复用路径；`requireTenantId():600`（private，返回 Long）、
  `requireSerializableId():628`（private static，`ID_MUST_BE_POSITIVE_LONG`）、`getEntityClass():408`（public final）；类声明
  `EgonColaRepository<M extends EgonColaMapper<T>, T extends EgonModel<T>>`。围栏 `EgonColaRepositoryArchitectureTest`
  实测双等式：接口钉数 `assertEquals(57, expected.size())`（:31）与"实现公有方法集 == IService 公有方法集"（:40-50，
  `MethodKeyBO` 为该测试嵌套 record :89）。
- Observable outcome: TEST-005（`getByCache` 与 `getById` 对照、命中态 Mapper 计数 0）、TEST-015（`listByCache` 部分命中组装顺序与
  null 剔除）、TEST-017（围栏修订后实现公有集 = IService 57 ∪ {getByCache, listByCache} 精确相等、接口 57
  钉数不动）、TEST-018（mp-ext 与 common-core 主源码零 import cache 实现包、端口文件仅 JDK import）、TEST-019（端口缺位时
  `getByCache`≡`getById`、`listByCache`≡`listByIds`、写路径端口零交互）全部转绿；写方法登记行为以 mock 端口
  verify（exact/glob 形状、去重保序、恰一次）。
- End state: repo 层缓存增强对宿主透明可用——模板子类覆写两个 protected seam 即接线（Step 9~11 示例），未覆写或无 bean
  时行为与基线逐字节一致；围栏与实现同提交原子生效。
- Test-first gate: Required — RED：File 1 因 `getByCache`/`listByCache`/seam 符号不存在而编译失败；File 3 修订先行亦
  RED（等式拒绝尚不存在的两方法）。
- Manual Checks: MC-NAME-001, MC-BEAN-001, MC-ARCH-001, MC-DEP-001, MC-VALID-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001,
  MC-BLOCKER-001
- Literal Rules: Rule 2, Rule 4, Rule 5, Rule 11
- Ordered files: File 1（增强测试）→ File 2（实现）→ File 3（围栏修订，与实现同提交转绿）→ File 4（双语 README）。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am clean test`
- Expected result: 模块全部测试（含既有 `EgonColaRepositoryTest`、修订后围栏）BUILD SUCCESS 全绿；`getByCache`/`listByCache`
  相关 5 项验收用例通过。
- Failure returns to: File 2（登记与直通分支）；若仅围栏失败则回 File 2 对齐方法集，禁止放宽 File 3 等式。
- Completion criteria: 上列测试全绿 + 实现与围栏处于同一提交 + `git show --stat HEAD` 路径恰为本 Step 五文件。
- Rollback: `git revert` 本 Step 提交（实现与围栏原子镜像，spec §16 回滚义务）；不得单独 revert File 3。
- Commit paths:
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaRepositoryCacheEnhancementTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaRepository.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaRepositoryArchitectureTest.java`,
  `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/README.md, egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/README.zh-CN.md`
- Commit: `feat(mp-ext): add transparent cache eviction and declarative cached reads to repository`

#### File 1 —
`CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaRepositoryCacheEnhancementTest.java`

- Purpose: TEST-005/015/019 与写路径登记 verify 的行为锚点（TEST-017/018 归 File 3）。
- Symbols: `EgonColaRepositoryCacheEnhancementTest` + 嵌套夹具 `TestBusinessCachedRepository`（继承既有
  `TestBusinessRepository` 剖面）、`RecordingCachePort`（实现 `EgonColaCachePort` 的记录桩）。
- Repository evidence: 同包 `EgonColaRepositoryTest.java` 实测夹具模式——`TestBusinessRepository`/`TestBusinessMapper`/
  `TestBusinessPO`/`TestTenantIdProvider`（固定 41L）均位于
  `src/test/java/top/egon/cola/component/common/mybatis/support/`；测试直接 new repo 并注入 mock mapper。
- Dependencies and consumers: 消费 File 2 的 seam 与新读方法；`RecordingCachePort` 实现 Step 2 端口三签名。
- Why now: test-first RED 锚点；夹具内联于测试文件避免污染 support 包公共面。
- Contract/signature changes: 新增测试类，无生产契约。
- Input/output and state mapping: (a) 无端口夹具（seam 不覆写）：`getByCache(7L)` 返回值/异常与 `getById(7L)` 逐字节一致且
  mapper `selectActiveById` 计数==调用次数（TEST-019）；(b) 有端口：`getByCache` 首次 miss → loader 经 `getById` 回源，端口收到
  key==`"41:7"`、region==`TestBusinessPO.class.getSimpleName()`；桩返命中值时 mapper 计数 0（TEST-005）；(c)
  `listByCache([7,8])` 桩返回 `[PO7, null]` → 结果 == [PO7]（null 剔除、顺序保序，TEST-015）；(d) 写登记：`save(entity)` → 端口收
  exact `["41:<id>"]` + glob `[]` 恰一次；`update(entity, wrapper)`/`remove(wrapper)`/`removeByMap(map)` → exact `[]` +
  glob `["41:*"]`；`removeByIds([7,7,8])` → exact `["41:7","41:8"]`（去重保序）；`saveBatch/list` 委托路径不重复登记（外层方法各
  1 次）。
- Error and edge behavior: `getByCache(null)`/`getByCache(-1L)` 仍抛 `ID_MUST_BE_POSITIVE_LONG`（直通既有校验）；端口
  `registerEvictionAfterCommit` 抛守卫码时原样上抛（登记期先于提交失败，spec §9.2 交互 5）；`listByCache(空集)` →
  `List.of()` 且端口零调用。
- Standards impact: MC-TEST-001——断言键形状/region/计数三要素齐备；MC-NAME-001——`*Test` 后缀与被测类镜像包。
- Implementation pseudocode:

```java
class TestBusinessCachedRepository extends TestBusinessRepository {
    RecordingCachePort port; // null 模拟端口缺位
    @Override protected ObjectProvider<EgonColaCachePort> getCachePortProvider() {
        return port == null ? null : new SingletonProvider<>(port);
    }
}
@Test void getByCacheHitZeroSql() {
    repo.port.cachedValue = cachedPo;
    assertThat(repo.getByCache(7L)).isSameAs(cachedPo);
    verify(mapper, never()).selectActiveById(any());
}
@Test void listByCachePartialHitOrder() { /* port.getAllValue=[po7,null] → 结果 [po7]，keys 入参 [41:7,41:8] */ }
@Test void writeRegistersExactEviction() {
    repo.save(entity);
    assertThat(repo.port.registrations).single()
        .extracting(r -> r.region(), r -> r.exactKeys(), r -> r.globs())
        .contains(...region, List.of("41:" + entity.getId()), List.of());
}
```

- Verification contribution: 步骤验证命令的主目标测试类；TEST-005/015/019 唯一执行面。
- Literal rule enforcement: Rule 2——夹具经端口交接键，异常语义直通既有校验不吞不改写；Rule 11——文件位于既有测试镜像包，夹具复用
  support 件。
- After this file: 测试编译失败（`getCachePortProvider` 等符号不存在），预期 RED。

#### File 2 —
`MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaRepository.java`

- Purpose: 增加两个 protected 挂点、15 个受控写方法的提交后失效登记、`getByCache`/`listByCache` 声明式读。
- Symbols: 新增 `getCachePortProvider()`、`cacheRegionName()`、私有 `cachePortOrNull()`、私有
  `registerCacheEviction(Collection<? extends Serializable>, boolean)`、public final `getByCache(Serializable)`、public
  final `listByCache(Collection<? extends Serializable>)`；修改 15 个既有 final 写方法体尾部各一行登记调用。
- Repository evidence: 15 个写方法与读路径行号实测见本 Step Baseline；类内已 import
  `org.springframework.beans.factory.ObjectProvider`？——实测未 import，本文件新增该单一 import 与
  `top.egon.cola.component.common.core.cache.EgonColaCachePort` import（common-core 依赖 pom :62 已在，零新增依赖）。
- Dependencies and consumers: 被 File 1 消费；运行期消费方为 Step 6 `egonColaCachePort` bean（宿主模板子类经 seam 注入）；
  `EgonColaIRepository` 接口面不动（REQ-010）。
- Why now: 端口接口（Step 2）就绪后第一个消费点；先于围栏修订（File 3 需方法存在才能钉等式）。
- Contract/signature changes: 既有 15 方法签名逐字节不变，仅体内追加登记；新增 2 个 public final 读方法与 2 个 protected
  seam；`EgonColaIRepository` 零改动。
- Input/output and state mapping: `getByCache(id)`：`requireTenantId()` + `requireSerializableId(id)` → 端口缺位时直接
  `return getById(id)`；有端口时 `port.get(cacheRegionName(), tenantId + ":" + id, () -> getById(id))` 强转 T 返回——回源异常类型与
  getById 完全一致。`listByCache(ids)`：`requireCollection` + 空集短路 `List.of()`（端口零调用）→ 缺位直接 `listByIds(ids)`
  ；否则 `ids.stream().map(requireSerializableId).distinct()` → 键表 `tenantId+":"+id` →
  `port.getAll(region, keys, k -> getById(Long.valueOf(k.substring(k.indexOf(':') + 1))))` → 剔除 null 后按入参序返回
  List<T>。写登记映射：`save:68`/`saveOrUpdate:227`/`updateById:193` → exact=[entity.getId()]；`saveBatch:75`/
  `saveOrUpdateBatch:87`/`updateBatchById:215` → exact=实体集 id 去重保序；`removeById:118/124` → exact=[id]；
  `removeById(T):130` → exact=[entity.getId()]；`removeByIds:148/187` → exact=入参去重保序；`removeByMap:138`/
  `remove:143`/`update:201`/`update:206` → exact=[] + glob=[tenantId+":*"]（影响面不可枚举）。
- Error and edge behavior: 端口缺位（provider null 或 bean 不存在）→ 全部直通基线路径、零 Redis 交互（REQ-011/REQ-025）；
  `registerEvictionAfterCommit` 守卫抛码原样上抛（登记失败先于提交，写事务由调用方回滚语义接管）；entity id 为 null 的写（如未生成
  id 的 saveOrUpdate 分支）走既有 `requireEntityId` 校验先行，登记点在写成功之后不引入新 NPE；回滚时缓冲区丢弃由端口负责（Step
  6），repo 侧零状态。
- Standards impact: MC-ARCH-001——仅经端口接口交接、mp-ext 对 cache 实现包零 import；MC-BEAN-001——seam 返回
  `ObjectProvider`，bean 名约束由模板子类 `@Qualifier("egonColaCachePort")` 承担（Step 9 示例）；MC-VALID-001——参数校验复用
  `requireSerializableId`/`requireCollection` 既有原生路径；MC-LOG-001——类内既有 `@Slf4j` 继承面，新增逻辑不写日志（日志归端口，避免双记）。
- Implementation pseudocode:

```java
protected ObjectProvider<EgonColaCachePort> getCachePortProvider() { return null; }
protected String cacheRegionName() { return getEntityClass().getSimpleName(); }

private EgonColaCachePort cachePortOrNull() {
    ObjectProvider<EgonColaCachePort> provider = getCachePortProvider();
    return provider == null ? null : provider.getIfAvailable();
}
private void registerCacheEviction(Collection<? extends Serializable> ids, boolean prefixEvict) {
    EgonColaCachePort port = cachePortOrNull();
    if (port == null) { return; }
    Long tenantId = requireTenantId();
    List<String> exact = new ArrayList<>();
    if (ids != null) {
        for (Serializable id : new LinkedHashSet<>(ids)) {
            exact.add(tenantId + ":" + requireSerializableId(id));
        }
    }
    List<String> globs = prefixEvict ? List.of(tenantId + ":*") : List.of();
    port.registerEvictionAfterCommit(cacheRegionName(), exact, globs);
}
// 15 个写方法尾部：boolean ok = ...原逻辑...; if (ok) { registerCacheEviction(...); } return ok;
// 谓词四写：registerCacheEviction(null, true)

public final T getByCache(Serializable id) {
    requireTenantId();
    requireSerializableId(id);
    EgonColaCachePort port = cachePortOrNull();
    if (port == null) { return getById(id); }
    String key = requireTenantId() + ":" + id;
    return (T) port.get(cacheRegionName(), key, () -> getById(id));
}
public final List<T> listByCache(Collection<? extends Serializable> idList) {
    requireTenantId();
    requireCollection(idList);
    if (idList.isEmpty()) { return List.of(); }
    EgonColaCachePort port = cachePortOrNull();
    if (port == null) { return listByIds(idList); }
    Long tenantId = requireTenantId();
    List<String> keys = idList.stream().map(EgonColaRepository::requireSerializableId)
        .distinct().map(id -> tenantId + ":" + id).toList();
    return port.getAll(cacheRegionName(), keys,
        k -> getById(Long.valueOf(k.substring(k.indexOf(':') + 1))))
        .stream().filter(Objects::nonNull).map(v -> (T) v).toList();
}
```

- Verification contribution: File 1 全用例 + File 3 等式转绿的实现面。
- Literal rule enforcement: Rule 2——层间交接（repo→端口）键与集合在进入端口前完成原生校验复用；Rule 4——本类非 @Service
  构件，注入经 protected seam 延迟到模板子类，子类以 `@RequiredArgsConstructor` + `@Qualifier` 书写（Step 9 示例承接）；Rule
  5——零新三方件（`LinkedHashSet`/`List.of` 皆 JDK）；Rule 11——文件位置与类结构维持 Archetype COLA infrastructure 剖面。
- After this file: File 1 转绿；File 3 旧等式此刻 RED（实现多出两公有方法），必须随本 Step 同提交修订。

#### File 3 —
`MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaRepositoryArchitectureTest.java`

- Purpose: 围栏显式修订——实现公有方法集白名单 +2 精确等式；新增 mp-ext 与 common-core 禁 import cache
  实现包扫描（TEST-017/018）。
- Symbols: 既有两测试方法之一（实现集等式 :40-50）修改 + 新增 `forbidCacheImplementationImports()`；嵌套 `MethodKeyBO`（:89
  record）不动。
- Repository evidence: 实测 `MethodKeyBO.of(Method)`（:91）与 `canonicalType` 处理 IService 擦除差异（:97 注释）；接口钉数
  `assertEquals(57, ...)`（:31）为不动面；common-core 相对路径 `../egon-cola-component-common-core/src/main/java` 实测存在。
- Dependencies and consumers: 消费 File 2 新方法签名；CI 根回归消费本围栏。
- Why now: 与实现同提交是 spec §16 步骤④ 原子性要求；置于实现之后书写。
- Contract/signature changes: 测试内部等式右端
  `expected.addAll(Set.of(MethodKeyBO.of(EgonColaRepository.class.getMethod("getByCache", Serializable.class)), MethodKeyBO.of(EgonColaRepository.class.getMethod("listByCache", Collection.class))))`
  ；接口 57 钉数行零改动。
- Input/output and state mapping: 实现公有方法扫描口径维持现状（仅 public、不含 protected seam——两个新 seam 与私有 helper
  不入白名单，天然被"public 过滤"排除）。
- Error and edge behavior: 未来任何人给 repo 加公有方法 → 等式拒绝，必须回到 spec 层面修订白名单；mp-ext/common-core 出现
  `import top.egon.cola.component.common.cache.` 前缀 → 断言失败并列出违例文件；common-core 端口文件出现非 `java.`/
  `java.util` 系 import → 失败（纯 JDK 签名不变量，与 common-core `CoreBoundaryTest` 的 spring 禁入扫描互补不重复）。
- Standards impact: MC-ARCH-001——依赖方向（mp-ext ↛ cache 实现）机器化钉死；MC-TEST-001——等式为精确集合相等，禁止
  containsAll 弱化。
- Implementation pseudocode:

```java
@Test void repositoryPublicApiMatchesFence() {
    // 原 expected = IService.getMethods() → Set<MethodKeyBO>（57 钉数测试保持不动）
    expected.addAll(Set.of(
        MethodKeyBO.of(EgonColaRepository.class.getMethod("getByCache", Serializable.class)),
        MethodKeyBO.of(EgonColaRepository.class.getMethod("listByCache", Collection.class))));
    assertEquals(expected, actualPublicMethods); // 精确相等，不放宽
}
@Test void forbidCacheImplementationImports() throws IOException {
    var roots = List.of(mainSrcDir, testSrcDir,                           // mp-ext 自身
        Path.of("../egon-cola-component-common-core/src/main/java"));     // 存在才扫描
    // 逐 .java 行扫描：命中 "import top.egon.cola.component.common.cache." → fail(违例文件相对路径)
    // 端口文件 EgonColaCachePort.java 额外断言：所有 import 以 "java." 开头
}
```

- Verification contribution: TEST-017/018 唯一执行面；本 Step 验证命令内转绿。
- Literal rule enforcement: Rule 11——分层结构约束以机器围栏钉死（Archetype COLA 允许面内新增依赖边禁止）；Rule 5——扫描逻辑仅
  JDK nio。
- After this file: 实现+围栏同态，模块测试全绿，可执行本 Step 提交。

#### File 4 —
`MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/README.md, egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/README.zh-CN.md`

- Purpose: 双语文档同步缓存增强用法（模块 README 双语并存实测于此目录）。
- Symbols: 无代码符号；新增节 "Cached reads / 声明式缓存读"、"Transparent eviction / 写路径透明失效"、"Zero-impact without
  port bean / 端口缺位零影响"。
- Repository evidence: 该模块 README.md/README.zh-CN.md 实测并存，双语章节镜像为既有风格。
- Dependencies and consumers: 宿主开发者；Step 9~11 模板 README 引用入口。
- Why now: 实现冻结后成文防漂移，并入本 Step 提交保持文档与围栏同步点一致。
- Contract/signature changes: 文档新增三节；既有章节零改动。
- Input/output and state mapping: 示例代码块与 File 2 seam 签名、Step 9 模板注入写法逐字一致；键形状与错误码表指向 cache
  starter README 单一事实源。
- Error and edge behavior: 文档明确"未装配 `egonColaCachePort` 时本模块行为与旧版本逐字节一致"（REQ-011 对外承诺）。
- Standards impact: MC-CONFIG-001——文档不含配置键复制面，仅引用，避免双源漂移。
- Implementation pseudocode:

```text
## Cached reads / 声明式缓存读
模板子类覆写 getCachePortProvider()/cacheRegionName() 两挂点；
getByCache(id) 与 getById(id) 语义一致（缓存前置），listByCache(ids) 与 listByIds(ids) 一致（逐键组装、去重保序）。
## Transparent eviction / 写路径透明失效
15 个受控写方法成功且事务提交后：精确键 EVICT；wrapper 谓词写 → 本租户 `tenantId:*` PREFIX_EVICT。
## Zero-impact without port bean / 端口缺位零影响
无 ObjectProvider 覆写或无 bean → 直通基线，零 Redis 交互。
```

- Verification contribution: 无测试贡献；由 Step 9~11 模板装配测试间接背书示例正确性。
- Literal rule enforcement: Rule 7——文档中键与开关表述与 starter 配置面一致；Rule 11——文档面维持模块 README 剖面。
- After this file: 执行验证命令五文件转绿后提交本 Step。

### Step 8 — Components BOM 导出新 starter 与 redisson 坐标

- Requirements: REQ-014, REQ-015
- Dependencies: Step 3（cache starter 坐标存在且进入 reactor）；Step 1（redisson 版本权威值 3.26.0 已定）。
- Baseline state: `egon-cola-components/egon-cola-components-bom/pom.xml` 实测无 parent、自持版本；三方件导出以自有
  property 书写（`commons.lang3.version=3.20.0` :53，条目 :63-67 先例）；组件导出条目 house style 为 `top.egon` +
  `${project.version}`（common-core :69-71、mp-ext :74-76 等）；当前 BOM 无任何 redisson 坐标（实测 grep 零命中）。
- Observable outcome: BOM 可解析安装；archetype 根（Step 9~11）与宿主经 `import` 该 BOM 即可对 cache starter 与两个
  redisson 坐标零版本声明。
- End state: 发布依赖面完整——REQ-014 "BOM 导出新 starter 与 redisson 坐标"验收达成，Step 9~11 的无版本引用有据。
- Test-first gate: Not applicable — 纯构建元数据；REQ-014 验收列以"BOM 可解析 + 下游解析结果"为证据（Step 9~11 的
  `dependency:tree` 与 ownership 脚本为二次锁形门）。
- Manual Checks: MC-DEP-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 5, Rule 11
- Ordered files: 见下 File 1（单文件）。
- Validation working directory: 仓库根目录
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-components-bom clean install`
- Expected result: BUILD SUCCESS；`~/.m2` 中当日版本 BOM 的 effective pom 含三条新条目（可用
  `mvn help:effective-pom -pl :egon-cola-components-bom` 复核）。
- Failure returns to: File 1（坐标/属性拼写或插入位置）。
- Completion criteria: 安装成功 + BOM diff 恰为 1 属性 + 3 条目，无既有条目变动。
- Rollback: `git checkout -- egon-cola-components/egon-cola-components-bom/pom.xml` 后重装。
- Commit paths: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Commit: `build(bom): export cache starter and redisson coordinates`

#### File 1 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: BOM 导出 cache starter 与 redisson 坐标，补齐宿主侧无版本引用面。
- Symbols: 新 property `redisson.version=3.26.0`；dependencyManagement 新条目
  `top.egon:egon-cola-component-common-cache-spring-boot-starter`（`${project.version}`）、`org.redisson:redisson`、
  `org.redisson:redisson-spring-boot-starter`（均 `${redisson.version}`）。
- Repository evidence: commons-lang3 自有 property 先例 :53/:63-67；组件 `${project.version}` 条目群 :69-96；版本值与
  `egon-cola-components/pom.xml:78` 的 `redisson.version=3.26.0` 同值（两处 BOM/父 pom 各自持属性是 BOM 无 parent
  的既有形态，非新违例）。
- Dependencies and consumers: 被 7 个 archetype 根唯一 import（`EVD-008` 家族机制）；Step 9~11 的 cache starter 无版本引用与宿主
  redisson starter 引用消费本文件。
- Why now: 必须在 archetype 适配（Step 9~11）之前发布导出面，否则根的无版本引用不可解析。
- Contract/signature changes: 仅新增；`<properties>` 段与组件条目区各插入一处。
- Input/output and state mapping: 无运行态；解析态从"cache/redisson 不在 BOM"变为"单点导出"。
- Error and edge behavior: 若 `redisson.version` 与父 pom 漂移（≠3.26.0），组件树与宿主树解析出不同 redisson 版本——由 Step
  9 `dependency:tree` 复核钉死。
- Standards impact: MC-DEP-001——外部消费面版本单点；MC-SCOPE-001——三条目一属性，不触既有条目。
- Implementation pseudocode:

```xml
<!-- properties 段，commons.lang3.version 之后 -->
<redisson.version>3.26.0</redisson.version>
<!-- dependencyManagement：commons-lang3 条目之后，双 redisson 坐标同版本 -->
<dependency><groupId>org.redisson</groupId><artifactId>redisson</artifactId>
    <version>${redisson.version}</version></dependency>
<dependency><groupId>org.redisson</groupId><artifactId>redisson-spring-boot-starter</artifactId>
    <version>${redisson.version}</version></dependency>
<!-- top.egon 组件条目区（data-desensitize :94-96 之后同风格） -->
<dependency><groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
    <version>${project.version}</version></dependency>
```

- Verification contribution: REQ-014 "BOM 可解析"；为 TEST-022 生成工程依赖树含 cache starter 提供解析源。
- Literal rule enforcement: Rule 5——导出件均为既有白名单三方件与自产件；Rule 11——BOM 剖面（无 parent、自有 property）零偏离。
- After this file: 本地仓库可见新 BOM；执行验证命令通过后提交本 Step。

### Step 9 — light 家族 archetype 适配（依赖 + 配置骨架 + 模板注入 + 再生成）

- Requirements: REQ-014, REQ-015, REQ-016, REQ-017
- Dependencies: Step 8（BOM 导出后根内无版本引用才可解析）；Step 7（模板 repo 注入示例消费 seam）。
- Baseline state: `SP/egon-cola-source-light/pom.xml:88-92` 与 `SP/egon-cola-source-light-open/pom.xml:46-50` 实测
  mp-ext 引用带 `<version>${egon-cola.version}</version>`（PC-002 七违例之二），同区 common-core/id-starter 为无版本引用样式；两根各
  4 份 yml 实测位于 `src/main/resources/`，`application.yml:135-141` 已有 `egon: cola: component: mybatis-plus:` 键树（cache
  须作 `component:` 同级新子键并入，禁止出现第二个顶层 `egon:` 键）；两 `UserRepository`（`...source.light...` 与
  `...source.lightopen...`）实测 `@Slf4j @Validated @Repository("userRepository") @RequiredArgsConstructor` +
  `@Getter(AccessLevel.PROTECTED) @Qualifier(...)` final 字段注入剖面，模块 lombok.config copyableAnnotations 含
  `@Qualifier`；definitions 实测目录名 `egon-cola-archetype-light`、`egon-cola-archetype-light-open`。
- Observable outcome: 两家族工程独立构建绿且依赖树含
  `top.egon:egon-cola-component-common-cache-spring-boot-starter:5.4.0`（reactor/BOM 解析、零内联版本）；两根 8 份 yml
  出现逐字节一致的 13 键 `cache` 块（内联比对脚本 light 集通过）；`generate` 后 definitions 与源同步、`check` 无
  diff；ownership 脚本违例从 7 条降为 5 条（剩余恰为 service/service-open/agent/web/web-open，本 Step 预期中间态）。
- End state: light 家族宿主生成即得"依赖 + 配置骨架 + 注入示例"三位一体，默认 `enabled=false` 零影响。
- Test-first gate: Not applicable — pom/配置/文档/生成物类改动；验证手段为构建、依赖树、键集比对脚本与再生成 check（§4.3
  既定口径，REQ-015/016 验收行即脚本证据）。
- Manual Checks: MC-CONFIG-001, MC-DEP-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 4, Rule 5, Rule 7, Rule 11
- Ordered files: 两 pom → 两组 yml → 两个 UserRepository → README 组 → 再生成（顺序不可换：pom 先于构建验证，生成物最后固化）。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml clean test && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml clean test && ./scripts/generate_archetypes.sh generate && ./scripts/generate_archetypes.sh check && python3 scripts/check-archetype-dependency-ownership.py`
- Expected result: 两工程 BUILD SUCCESS；check 无 diff；ownership 脚本违例列表只剩 5 条非 light 条目（无 `light` 路径违例）。
- Failure returns to: File 1/File 2（引用坐标或版本元素残留）；yml 比对失败回 File 3/File 4 对齐键块。
- Completion criteria: 上列三项齐备 + `grep -c "<version>" 两 pom 的 cache 引用块` 为 0 + light 8 yml 键集比对通过。
- Rollback: `git checkout --` 本 Step 路径集（含 definitions 再生成回退）。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-prod.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/user/repo/UserRepository.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/user/repo/UserRepository.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-light/README.md, egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.md, egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.zh-CN.md`,
  `egon-cola-archetypes/definitions/egon-cola-archetype-light/, egon-cola-archetypes/definitions/egon-cola-archetype-light-open/`
- Commit: `feat(archetypes-light): wire cache starter, config skeleton and template injection example`

> **执行修订 AM-002（Step 10 执行期追加，2026-09-19，属 AM-001 用户批准的纠正面）**：①service 与 service-open 的
`PersistenceTestSupport.java` 按 AM-001 同型追加 `egonColaRoutingProfiles`/`egonColaShardingRouteFingerprint`
`@TestBean` 覆盖（两工程 `clean test` 实测绿）。②agent 家族暴露 AM-001 同族基线既有破损：`KnowledgeRepositoryTest` 的
`ApplicationContextRunner` 自组件强制拓扑契约引入以来缺 `egonColaWriteTargetResolver`/`egonColaRoutingProfiles`/
`egonColaShardingRouteFingerprint` 三 bean（`NoSuchBeanDefinitionException`，HEAD 即红、与本 Step 源改动无关），以测试配置补三个同型
> bean 修复。③agent 4 份 profile yml 的 `logging: level:` 空桩为 `0d3c030f`（9/14）引入的既有缺陷（`logging.level` 绑定
> fail、agent starter 全部 context 测试 HEAD 即红），删除该空桩键。④REQ-016 要求的 13 键块含小写 `redis:` 配置键，命中
`AgentSourceContractTest` 技术栈词汇围栏（禁 `fastjson|redis|graphql|dubbo`）；该围栏意图是禁运行时栈引用而非禁用中的配置骨架键，
`allSourceText()` 精准豁免独立成行的 `redis:` 键（其余禁项不变），与 Step 7"围栏原子修订"同一治理先例。⑤`9c1cd5930` 排序修复使
> agent 的 `@SpringBootTest` context 首次真正吃到强制 `dataSource`（此前 boot Hikari 抢先、9/14 的 ③ 缺陷又掩盖了启动路径）：新建
`egon-cola-source-agent-starter/src/test/java/.../starter/PersistenceTestSupport.java`（与 light/service 同型五 bean
`@TestBean` 覆盖、裸 H2 无 schema populator），四个 context 测试类改为继承之；`Knowledge*RepositoryImpl` 主源仍零改动，符合
> REQ-016/本 Step agent 特例裁决。


> **执行修订 AM-001（用户批准，2026-09-19）**：Step 9 验证暴露基线既有破损——组件提交 `699bae32d` 的强制 ShardingSphere
> DataSource 与 Spring `DataSourceAutoConfiguration` 处理次序冲突（`BeanDefinitionOverrideException: dataSource`），脚手架
> context 测试自 9/15 起即红（9/17 `94a636159` 已从 CI 移除 source-projects 作业，故未被发现）。处置：①先行专用纠正提交
`9c1cd5930`（mp-ext 自动配置 `before = DataSourceAutoConfiguration.class` + 组件回归围栏，RED/GREEN 均已实测）；②本 Step
> 文件面追加两文件：light 与 light-open 的 `src/test/java/.../support/PersistenceTestSupport.java`——以 `@TestBean` 覆盖
`egonColaRoutingProfiles`/`egonColaShardingRouteFingerprint`，使隔离 H2 测试面与强制拓扑契约重新对齐（service/agent
> 家族同型适配归 Step 10，web 家族归 Step 11）；③File 8 口径澄清：`definitions/` 仅承载 archetype 元数据（本次零变更属实），模板档案由
`generate` 自源工程实时打包发布至 `.generated`，`check` 以“generated set is deterministic and matches current
> workspace”通过为准。

#### File 1 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`

- Purpose: light 根引入 cache starter（无版本）并清除 mp-ext 内联版本违例。
- Symbols: `egon-cola-component-common-cache-spring-boot-starter` 依赖条目；mp-ext 引用删除
  `<version>${egon-cola.version}</version>` 一行（:91）。
- Repository evidence: 实测 :84-97 依赖区——common-core 无版本、mp-ext 带版本（违例）、id-starter 无版本；根 dmu 唯一 import
  components BOM（Step 8 已导出 cache 坐标）；同文件同风格可复制。
- Dependencies and consumers: 构建解析消费方为 light 全工程与生成后宿主；ownership 脚本扫描本文件。
- Why now: 一切后续构建验证的前提。
- Contract/signature changes: 依赖 +1（无版本）、版本元素 -1；`egon-cola.version` 属性本身保留（facade 等仍在用）。
- Input/output and state mapping: 无运行态；解析态 cache starter 版本由 BOM 单点提供。
- Error and edge behavior: 若误加版本元素，ownership 脚本立即红（本 Step 验证命令兜住）；若 BOM 未含该坐标，构建报 version
  missing——依赖 Step 8 前置。
- Standards impact: MC-DEP-001——引用处零内联版本；MC-SCOPE-001——两处编辑同文件。
- Implementation pseudocode:

```xml
<!-- mp-ext 引用块内删除 <version>${egon-cola.version}</version> -->
<!-- 紧随 mp-ext 引用块之后新增（与 common-core 同风格、无版本）： -->
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
</dependency>
```

- Verification contribution: REQ-015 "引用处无内联版本" + ownership 违例 -1。
- Literal rule enforcement: Rule 5——仅引用既有治理坐标，无新三方件；Rule 11——声明位置与书写风格零偏离根 pom 剖面。
- After this file: light 根可解析 cache starter（BOM 版本），待 yml 与模板接线。

#### File 2 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml`

- Purpose: light-open 根与 light 镜像同构改造。
- Symbols: 同 File 1（mp-ext 引用实测 :46-50）。
- Repository evidence: 实测该 pom 的 mp-ext 带版本引用与无版本 core 引用并存，剖面与 light 一致。
- Dependencies and consumers: light-open 工程构建与 ownership 脚本。
- Why now: 家族内两根必须同提交对称，防半家族漂移。
- Contract/signature changes: 同 File 1。
- Input/output and state mapping: 同 File 1。
- Error and edge behavior: 同 File 1；若两根引用形态不一致，Step 11 全量比对与家族边界脚本可暴露。
- Standards impact: MC-DEP-001、MC-SCOPE-001 同 File 1。
- Implementation pseudocode:

```xml
<!-- 两处编辑与 File 1 逐字符一致（mp-ext 引用实测 :46-50）： -->
<!-- ① mp-ext 引用块内删除版本元素；② 紧随其后新增无版本 cache 引用： -->
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
</dependency>
```

- Verification contribution: REQ-015；ownership 违例 -1。
- Literal rule enforcement: Rule 5、Rule 11——同 File 1 镜像。
- After this file: 两 pom 治理就绪，本家族依赖面冻结。

#### File 3 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`

- Purpose: 四 profile yml 注入结构一致的 13 键 cache 块。
- Symbols: `egon.cola.component.cache`
  键树（enabled/node-id/key-prefix/tenant-mdc-key/second-evict-delay/redis.topic/l1.max-size/ttl.expire/ttl.null-expire/ttl.jitter-ratio/batch.max-keys/lock.wait-time/lock.lease-time）。
- Repository evidence: `application.yml:135-141` 实测已有 `egon.cola.component.mybatis-plus` 树——cache 作为 `component:`
  下与 `mybatis-plus:` 同级子键并入；dev/test/prod 同构位置实测存在（各文件均含 egon 根键）。
- Dependencies and consumers: `EgonColaCacheProperties`（Step 3）relaxed-binding 消费；TEST-021 键集比对脚本消费。
- Why now: 依赖就绪后配置骨架先行，模板代码示例引用其键名。
- Contract/signature changes: 每文件 +13 叶键一个块；既有键零改动。
- Input/output and state mapping: 所有 profile `enabled: false`（决策③零影响）；值即 Properties 默认值字面（时长 ISO-8601：
  `PT5S`/`PT30M`/`PT60S`/`PT0.5S`/`PT10S`）。
- Error and edge behavior: 出现第二个顶层 `egon:` 键 → Spring yaml 加载 duplicate key 失败，本地构建即暴露；键名拼错 →
  绑定静默忽略但 TEST-021 键集比对红。
- Standards impact: MC-CONFIG-001——多环境键集恒等、值允许分歧（本特性值亦全同）。
- Implementation pseudocode:

```yaml
# 在 egon.cola.component 下与 mybatis-plus 同级追加（四文件逐字节一致）：
      cache:
        enabled: false
        node-id: ""
        key-prefix: egon:cola:cache
        tenant-mdc-key: tenantId
        second-evict-delay: PT5S
        redis:
          topic: "egon:cola:cache:event"
        l1:
          max-size: 10000
        ttl:
          expire: PT30M
          null-expire: PT60S
          jitter-ratio: 0.1
        batch:
          max-keys: 1000
        lock:
          wait-time: PT0.5S
          lease-time: PT10S
```

- Verification contribution: REQ-016 键块；light 集键比对（Step 11 全 28 比对的前件）。
- Literal rule enforcement: Rule 7——四 profile 键结构完全一致；Rule 10——时长一律 ISO-8601 字符串绑定
  `java.time.Duration`。
- After this file: light 配置骨架就位（默认关，零影响）。

#### File 4 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-prod.yml`

- Purpose: light-open 四 yml 镜像注入。
- Symbols: 同 File 3 键树。
- Repository evidence: light-open `src/main/resources/` 实测含同 4 份 application yml（另有 bootstrap*.yml，不属于本键块面，不动）。
- Dependencies and consumers: 同 File 3。
- Why now: 家族对称性，与 File 3 同提交。
- Contract/signature changes: 同 File 3。
- Input/output and state mapping: 同 File 3（与 File 3 全部 8 文件逐字节同块）。
- Error and edge behavior: 同 File 3；light/light-open 间键集差异由比对脚本捕获。
- Standards impact: MC-CONFIG-001 同 File 3。
- Implementation pseudocode:

```text
将 File 3 的 13 键 cache 块逐字节注入 4 份文件（既有 egon.cola.component 树下、与 mybatis-plus 同级）；
bootstrap 系列文件不属于本键块面，保持零触碰（§5 约定行）；
完成后 light+light-open 共 8 文件键集全等，家族内任一键差异由比对脚本与家族边界脚本暴露。
```

- Verification contribution: REQ-016；light 家族 8 文件键集比对对象。
- Literal rule enforcement: Rule 7、Rule 10——同 File 3。
- After this file: 两 yml 组就位，比对脚本应报 8 文件同集。

#### File 5 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/user/repo/UserRepository.java`

- Purpose: 模板 Repository 的端口注入示例（REQ-016 唯一代码示例面）。
- Symbols: 新增 final 字段 `cachePortProvider`（`ObjectProvider<EgonColaCachePort>`）+ 两个 import；lombok
  `@Getter(AccessLevel.PROTECTED)` 生成的 `getCachePortProvider()` 即 seam 覆写。
- Repository evidence: 实测类既有
  `@Getter(AccessLevel.PROTECTED) @Qualifier("egonColaModelValidationUtils") private final ...` 三连字段剖面；
  `@RequiredArgsConstructor` 在类；模块 `lombok.config` 实测
  `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier`——构造器注入时 @Qualifier 传递成立。
- Dependencies and consumers: 消费 Step 7 seam 与 Step 2 端口；运行期消费 `egonColaCachePort` bean（enabled=true 时）；缺位时
  `getIfAvailable()` 语义由基类直通处理。
- Why now: 依赖与配置就位后示例才可编译。
- Contract/signature changes: 类字段 +1、构造器参数 +1（lombok 生成面）；其余方法零改动；不新增对 `getByCache`
  的调用示例方法（保持模板最小面，用法见 README）。
- Input/output and state mapping: 字段名 `cachePortProvider` → lombok getter 名与基类 seam 完全一致 → 基类经其取端口；bean
  缺席时 `getIfAvailable()` 返回 null → 基类直通。
- Error and edge behavior: 若字段名漂移（如 `cachePort`），getter 不覆写 seam → 基类永远走缺位直通——README 显式警告；@Qualifier
  丢失 → enabled=true 且宿主多 CachePort 实现时装配歧义（copyableAnnotations 保障）。
- Standards impact: MC-BEAN-001——bean 名经 `@Qualifier("egonColaCachePort")` 钉定；MC-NAME-001——字段与 seam 语义对齐。
- Implementation pseudocode:

```java
// imports 追加：
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
// 字段区追加（与 modelValidationUtils 同剖面）：
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;
```

- Verification contribution: light 工程编译 + 生成宿主 `dependency:tree` 含 cache starter（REQ-015 验收）；示例正确性由
  README 命令与后续 Step 11 全仓构建背书。
- Literal rule enforcement: Rule 4——注入一律 `@RequiredArgsConstructor` 构造器面 + `@Qualifier` 钉 bean
  名，字段声明零手写构造器；Rule 11——infrastructure/repo 分层位置不动。
- After this file: light 家族模板接线完成。

#### File 6 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/user/repo/UserRepository.java`

- Purpose: light-open 镜像注入示例。
- Symbols: 同 File 5。
- Repository evidence: 实测该类与 light 版逐结构镜像（包名 `lightopen`），同一 lombok 剖面。
- Dependencies and consumers: 同 File 5。
- Why now: 家族对称同提交。
- Contract/signature changes: 同 File 5。
- Input/output and state mapping: 同 File 5。
- Error and edge behavior: 同 File 5。
- Standards impact: MC-BEAN-001——`@Qualifier("egonColaCachePort")` 显式命名传导；MC-NAME-001——字段名 `cachePortProvider`
  语义钉死；其余同 File 5。
- Implementation pseudocode:

```java
// 与 File 5 逐字符相同（仅所在包名 lightopen），imports + 字段追加：
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;
// 字段名 cachePortProvider 钉死：mp-ext seam 依赖 lombok 生成的 getter 名（Step 7 File 2）
```

- Verification contribution: light-open 工程编译与装配面。
- Literal rule enforcement: Rule 4、Rule 11——同 File 5。
- After this file: 两模板 repo 完成对称注入。

#### File 7 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/README.md, egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.md, egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.zh-CN.md`

- Purpose: 双语 README 增加"二级缓存骨架"一节。
- Symbols: 无代码符号；新章节四要素：默认关说明 / 开启步骤（RedissonClient 装配 + enabled=true）/ repo 示例（`getByCache`/
  `listByCache` 与写透自动失效一句话）/ 指向组件 cache starter README 的相对链接。
- Repository evidence: 各根 README.md/README.zh-CN.md 双语镜像为实测剖面。
- Dependencies and consumers: 生成宿主使用者；Step 10/11 家族 README 复制本节。
- Why now: 代码与配置冻结后成文。
- Contract/signature changes: 文档追加；既有章节不动。
- Input/output and state mapping: 键名与默认值抄录与 File 3/File 4 块逐字一致。
- Error and edge behavior: 文档键名漂移由 Step 11 全量比对与人工评审兜住。
- Standards impact: MC-CONFIG-001——文档键示例单一事实源指回组件 README。
- Implementation pseudocode:

```text
## 二级缓存骨架（默认关闭）
生成工程已含 egon-cola-component-common-cache-spring-boot-starter 依赖与
egon.cola.component.cache 键块（enabled: false）。启用：①装配 RedissonClient；
②置 enabled: true；UserRepository 已示范端口注入，直接调用 getByCache/listByCache，
受控写方法提交后自动跨节点失效。详见组件文档相对链接。
```

- Verification contribution: 无测试；命令可复制性人工核验 + REQ-016 文档面。
- Literal rule enforcement: Rule 7——双语与多根文档内容一致。
- After this file: 文档就绪，触发再生成。

#### File 8 —
`GENERATED egon-cola-archetypes/definitions/egon-cola-archetype-light/, egon-cola-archetypes/definitions/egon-cola-archetype-light-open/`

- Purpose: 固化再生成产物，使 archetype 发布面与源工程零漂移。
- Symbols: 无手写符号；整目录由脚本再生成。
- Repository evidence: `scripts/generate_archetypes.sh` 实测 `SOURCE_ROOT=source-projects`、
  `DEFINITIONS_ROOT=definitions`，`generate|check` 双模式；definitions 现目录名 `egon-cola-archetype-light`/
  `egon-cola-archetype-light-open` 实测。
- Dependencies and consumers: File 1~7 全部源改动；`check` 模式与 TEST-022 消费产物。
- Why now: 生成物必须与源同提交（§4.4 提交边界规则）。
- Contract/signature changes: 目录内容整体刷新，无手工编辑。
- Input/output and state mapping: source-projects 改动 → 模板 archive 内容映射由脚本完成。
- Error and edge behavior: 手工编辑 definitions → `check` 红；脚本失败回查 File 1~7 源合法性。
- Standards impact: MC-ARCH-001——发布面与源同步机器保障。
- Implementation pseudocode:

```text
./scripts/generate_archetypes.sh generate
git status --porcelain egon-cola-archetypes/definitions/   # 仅 light 两目录变更
./scripts/generate_archetypes.sh check                      # 无 diff
```

- Verification contribution: TEST-022 前置；REQ-017 再生成一致性。
- Literal rule enforcement: Rule 11——生成机制零改动，仅产物刷新。
- After this file: 执行本 Step 验证命令五段全过（比对脚本 light 集通过、ownership 违例 5 条）后提交本 Step。

### Step 10 — service 家族与 agent 工程适配（含 agent 无示例特例）

- Requirements: REQ-014, REQ-015, REQ-016, REQ-017
- Dependencies: Step 8（BOM）、Step 9（README 本节文案源、yml 块字面模板）。
- Baseline state: `SP/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml:18-22` 与
  `...-service-open-infrastructure/pom.xml:18-22`、
  `SP/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml:16-20` 实测 mp-ext 引用带
  `<version>${egon-cola.version}</version>`（PC-002 剩余违例中的三条，web 家族两条留 Step 11）；三根 yml 各 4 份实测位于各自
  `-starter/src/main/resources/`；`CourseRepository` 实测于 `...source.service.infrastructure.course.repo`（open 侧包段
  `serviceopen`）且 lombok 剖面同 light；agent 根无 `repo` 包注入示例面（其 infra `Knowledge*RepositoryImpl` 保持零改动，spec
  REQ-016"代表性模板 Repository"以 service/light/web 三线 exemplar 满足）。
- Observable outcome: 三工程构建绿且依赖树含 cache starter；12 份 yml 出现与 light 家族逐字节一致的 13 键块；两
  `CourseRepository` 注入示例编译通过；`check` 无 diff；ownership 违例降为 2 条（仅 web/web-open）。
- End state: 7 根中 5 根完成适配；剩余 web 家族一步收口。
- Test-first gate: Not applicable — 同 Step 9 口径（脚本 + 构建为验收证据）。
- Manual Checks: MC-CONFIG-001, MC-DEP-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 4, Rule 5, Rule 7, Rule 11
- Ordered files: 三 pom → 三组 yml → 两 CourseRepository → README 组 → 再生成。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml clean test && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml clean test && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean test && ./scripts/generate_archetypes.sh generate && ./scripts/generate_archetypes.sh check && python3 scripts/check-archetype-dependency-ownership.py`
- Expected result: 三工程 BUILD SUCCESS；check 无 diff；ownership 违例列表仅剩 2 条 web 家族条目。
- Failure returns to: pom 违例残留回 File 1/File 2/File 3；agent 构建红多因误动其 repo 类（该类不在本 Step 文件集，必须回退）。
- Completion criteria: 三构建绿 + 12 yml 键集与 light 家族比对同集 + ownership 违例计数==2 且均为 web。
- Rollback: `git checkout --` 本 Step 路径集。
- Commit paths:
  `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-prod.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-prod.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/course/repo/CourseRepository.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/java/top/egon/cola/archetype/source/serviceopen/infrastructure/course/repo/CourseRepository.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-service/README.md, egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.md, egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-agent/README.md, egon-cola-archetypes/source-projects/egon-cola-source-agent/README.zh-CN.md`,
  `egon-cola-archetypes/definitions/egon-cola-archetype-service/, egon-cola-archetypes/definitions/egon-cola-archetype-service-open/, egon-cola-archetypes/definitions/egon-cola-archetype-agent/`
- Commit: `feat(archetypes-service,agent): wire cache starter, config skeleton and template injection examples`

#### File 1 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml`

- Purpose: service 基础设施模块引入无版本 cache 引用并删 mp-ext 内联版本（根 pom 不动，PC-003）。
- Symbols: :18-22 mp-ext 引用块删版本行 + 新 cache 依赖条目。
- Repository evidence: 实测引用块与 light 根同构（top.egon + version 元素），同模块 dmu 经家族根 import BOM 传递解析。
- Dependencies and consumers: CourseRepository 编译消费；ownership 脚本扫描。
- Why now: 家族改造依赖先行。
- Contract/signature changes: 依赖 +1、版本元素 -1。
- Input/output and state mapping: 无运行态。
- Error and edge behavior: 同 Step 9 File 1。
- Standards impact: MC-DEP-001、MC-SCOPE-001。
- Implementation pseudocode:

```xml
<!-- infra pom 实测 :20-21 mp-ext 引用块：删除版本元素一行；紧随其后追加无版本 cache 引用（字面同 Step 9 File 1）： -->
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
</dependency>
<!-- 编辑后该 pom 的 top.egon 引用全部无版本，ownership 脚本本根转绿 -->
```

- Verification contribution: REQ-014/015；ownership 违例 -1。
- Literal rule enforcement: Rule 5、Rule 11——引用无版本、位置镜像既有块。
- After this file: service infra 依赖就绪。

#### File 2 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml`

- Purpose: service-open 镜像改造。
- Symbols: :18-22 同构块。
- Repository evidence: 实测与 File 1 逐结构相同（artifactId 前缀 `-service-open-`）。
- Dependencies and consumers: 同 File 1。
- Why now: 家族对称。
- Contract/signature changes: 同 File 1。
- Input/output and state mapping: 同 File 1。
- Error and edge behavior: 同 File 1。
- Standards impact: MC-DEP-001——无版本引用且清零本根内联违例；MC-SCOPE-001——单 pom 编辑块；其余同 File 1。
- Implementation pseudocode:

```xml
<!-- 与 File 1 相同两处编辑（mp-ext 引用实测 :20，无版本 cache 引用紧随追加）： -->
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
</dependency>
```

- Verification contribution: ownership 违例 -1。
- Literal rule enforcement: Rule 5、Rule 11。
- After this file: 两 infra pom 就绪。

#### File 3 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml`

- Purpose: agent 域模块引入无版本 cache 引用并删 mp-ext 内联版本；agent 无 repo 注入示例（Baseline 已裁决）。
- Symbols: :16-20 mp-ext 引用块删 :19 版本行 + 新 cache 条目。
- Repository evidence: 实测 agent 的 mp-ext 引用位于 domain 模块（7 违例之一）；agent 根 pom 仅属性 `egon-cola.version`（:
  23）不属违例面，不动。
- Dependencies and consumers: agent 全工程构建；ownership 脚本。
- Why now: 与 service 家族同 Step 清违例，保持脚本违例数单调下降可核对。
- Contract/signature changes: 同 File 1。
- Input/output and state mapping: 无运行态；agent 工程默认仍 `enabled=false`。
- Error and edge behavior: 若在 agent repo 实现类误加注入示例，超出 spec 文件树授权——禁止。
- Standards impact: MC-DEP-001——domain 模块引用无版本、根属性零触碰；MC-SCOPE-001——单 pom 编辑块；其余同 File 1。
- Implementation pseudocode:

```xml
<!-- agent domain pom 实测 :19 mp-ext 引用：删除版本元素；引用块后追加无版本 cache 引用（字面同 File 1） -->
<!-- 根 pom :23 的 egon-cola.version 属性行零触碰（仅 domain 内联违例在列） -->
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
</dependency>
```

- Verification contribution: ownership 违例 -1；REQ-015 agent 面。
- Literal rule enforcement: Rule 5、Rule 11。
- After this file: 三 pom 就绪，进入配置面。

#### File 4 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`

- Purpose: service starter 模块 4 profile yml 注入 13 键块。
- Symbols: `egon.cola.component.cache` 键树（字面模板 = Step 9 File 3 块）。
- Repository evidence: 实测 4 份 yml 位于 `-starter/src/main/resources/`（多模块家族的配置汇聚点）。
- Dependencies and consumers: Properties 绑定与键集比对。
- Why now: 依赖就绪后统一注入。
- Contract/signature changes: 每文件 +1 块。
- Input/output and state mapping: 与 light 家族 8 文件逐字节同块。
- Error and edge behavior: 同 Step 9 File 3。
- Standards impact: MC-CONFIG-001。
- Implementation pseudocode:

```text
将 Step 9 File 3 的 13 键 yaml 块逐字节注入 4 份文件（egon.cola.component 树下与 mybatis-plus 同级；
service 家族 yml 位于 egon-cola-source-service-starter/src/main/resources/，§5 约定）；
enabled 在 4 份文件一律 false；完成后本根 4 文件键集全等。
```

- Verification contribution: REQ-016；比对脚本 12 文件集成员。
- Literal rule enforcement: Rule 7、Rule 10。
- After this file: service 配置就位。

#### File 5 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-prod.yml`

- Purpose: service-open 4 yml 镜像注入。
- Symbols: 同 File 4。
- Repository evidence: 实测路径存在。
- Dependencies and consumers: 同 File 4。
- Why now: 家族对称。
- Contract/signature changes: 同 File 4。
- Input/output and state mapping: 同 File 4。
- Error and edge behavior: 同 File 4。
- Standards impact: MC-CONFIG-001——13 键块与全根同集；MC-TIME-001——时长值一律 ISO-8601 `PT` 字面量；其余同 File 4。
- Implementation pseudocode:

```text
同 Step 9 File 3 的 13 键 yaml 块逐字节注入 4 份文件（service-open starter resources 目录，§5 约定）；
enabled 在 application.yml 与 dev/test/prod 三份中一律 false（REQ-011 零影响承诺）；
键集与 service/agent 根完全一致，值仅按 profile 允许不同（Rule 7）；
TEST-021 收口比对覆盖全部 28 文件，本根任一缺键即红。
```

- Verification contribution: REQ-016。
- Literal rule enforcement: Rule 7、Rule 10。
- After this file: service-open 配置就位。

#### File 6 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-prod.yml`

- Purpose: agent 4 yml 注入（配置键块全 7 根无例外，REQ-016 字面）。
- Symbols: 同 File 4。
- Repository evidence: 实测 4 份位于 agent-starter resources。
- Dependencies and consumers: 同 File 4。
- Why now: agent 无代码示例但配置面必须齐备（键一致承诺）。
- Contract/signature changes: 同 File 4。
- Input/output and state mapping: 同 File 4。
- Error and edge behavior: 同 File 4。
- Standards impact: MC-CONFIG-001——13 键块与全根同集；MC-TIME-001——时长值一律 ISO-8601 `PT` 字面量；其余同 File 4。
- Implementation pseudocode:

```text
同 Step 9 File 3 的 13 键 yaml 块逐字节注入 agent starter resources 4 份文件（§5 目录约定）；
enabled 全 profile false；ttl 各值维持 ISO-8601 `PT` 字面量与模板默认一致；
本 File 不含 repo 注入面（agent 无示例特例见 §5 约定与本 Step Repository evidence，Knowledge 域零触碰）。
```

- Verification contribution: REQ-016 agent 面。
- Literal rule enforcement: Rule 7、Rule 10。
- After this file: 12 份 yml 三组齐备，进入代码示例。

#### File 7 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/course/repo/CourseRepository.java`

- Purpose: service 家族代表性 repo 注入示例。
- Symbols: 同 Step 9 File 5 的字段 + imports 追加。
- Repository evidence: 实测该类 lombok 剖面与 light `UserRepository` 一致（`@Getter(AccessLevel.PROTECTED)` +
  `@Qualifier` 字段群）。
- Dependencies and consumers: Step 7 seam；`egonColaCachePort` bean（可选）。
- Why now: 编译依赖 File 1。
- Contract/signature changes: 字段 +1。
- Input/output and state mapping: 同 Step 9 File 5。
- Error and edge behavior: 同 Step 9 File 5。
- Standards impact: MC-BEAN-001、MC-NAME-001。
- Implementation pseudocode:

```java
// 与 Step 9 File 5 逐字符相同（course.repo 包名之外字面一致）：
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;
// 构造器仍走既有 @RequiredArgsConstructor 面，不手写构造器（Rule 4）
```

- Verification contribution: service 工程编译绿。
- Literal rule enforcement: Rule 4、Rule 11。
- After this file: service 示例完成。

#### File 8 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/java/top/egon/cola/archetype/source/serviceopen/infrastructure/course/repo/CourseRepository.java`

- Purpose: service-open 镜像示例。
- Symbols: 同 File 7。
- Repository evidence: 实测包段 `serviceopen`，剖面一致。
- Dependencies and consumers: 同 File 7。
- Why now: 家族对称。
- Contract/signature changes: 同 File 7。
- Input/output and state mapping: 同 File 7。
- Error and edge behavior: 同 File 7。
- Standards impact: MC-BEAN-001——`@Qualifier("egonColaCachePort")` 显式命名传导；MC-NAME-001——字段名 `cachePortProvider`
  语义钉死；其余同 File 7。
- Implementation pseudocode:

```java
// 与 File 7 逐字符一致（包名 source.serviceopen...course.repo）：
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;
```

- Verification contribution: service-open 编译绿。
- Literal rule enforcement: Rule 4、Rule 11。
- After this file: 两示例完成。

#### File 9 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/README.md, egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.md, egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-agent/README.md, egon-cola-archetypes/source-projects/egon-cola-source-agent/README.zh-CN.md`

- Purpose: 三工程双语 README 缓存骨架节。
- Symbols: 无代码符号；文案 = Step 9 File 7 模板，service 侧示例名换 `CourseRepository`，agent 侧声明"本模板含依赖与配置骨架，业务
  repo 自行覆写 seam 接线"。
- Repository evidence: 六文件实测并存且双语镜像。
- Dependencies and consumers: 宿主使用者。
- Why now: 源改动冻结后成文。
- Contract/signature changes: 文档追加。
- Input/output and state mapping: 键名抄录与 File 4/5/6 块一致。
- Error and edge behavior: agent README 不得虚构 repo 示例（与 File 3 特例裁决一致）。
- Standards impact: MC-CONFIG-001。
- Implementation pseudocode:

```text
6 份 README 各追加"二级缓存骨架（默认关闭）"节，文案 = Step 9 File 7 模板（英文/中文 README 各自对应语言版本）：
13 键表、启用步骤（dev/test 单模块先行翻 true）、零影响承诺；
agent 两份为变体：删除"Repository 注入示例"句（agent 本 Step 无 repo 示例，Knowledge 域零触碰）。
```

- Verification contribution: 无测试；REQ-016 文档面。
- Literal rule enforcement: Rule 7——跨根文档一致。
- After this file: 文档就绪，触发再生成。

#### File 10 —
`GENERATED egon-cola-archetypes/definitions/egon-cola-archetype-service/, egon-cola-archetypes/definitions/egon-cola-archetype-service-open/, egon-cola-archetypes/definitions/egon-cola-archetype-agent/`

- Purpose: 三模板再生成固化。
- Symbols: 无手写符号。
- Repository evidence: 目录名实测；机制同 Step 9 File 8。
- Dependencies and consumers: File 1~9 源改动。
- Why now: 产物与源同提交。
- Contract/signature changes: 目录刷新。
- Input/output and state mapping: 脚本映射。
- Error and edge behavior: 手改 definitions → check 红。
- Standards impact: MC-ARCH-001。
- Implementation pseudocode:

```text
./scripts/generate_archetypes.sh generate
# git status 仅 service/service-open/agent 三目录变化
./scripts/generate_archetypes.sh check
```

- Verification contribution: TEST-022 前件；REQ-017。
- Literal rule enforcement: Rule 11。
- After this file: 执行本 Step 验证命令（三构建 + check + ownership 违例 2 条）全过即提交本 Step。

### Step 11 — web 家族适配与全仓收口回归

- Requirements: REQ-001, REQ-014, REQ-015, REQ-016, REQ-017
- Dependencies: Step 1~10 全部（本 Step 同时是发布前最终回归门）。
- Baseline state: web 两 `-infrastructure/pom.xml` 的 mp-ext 引用实测 :19-23（带版本，PC-002 最后两条违例）；web/web-open 各
  4 份 yml 实测位于 `-starter/src/main/resources/`；两 `UserRepository` 实测于 `...source.web.infrastructure.user.repo` 与
  `...source.webopen.infrastructure.user.repo`；根 `./mvnw -B -ntp clean test` 覆盖 components + xingyuan + archetypes
  三大聚合（根 pom :58-60 modules 实测）。
- Observable outcome: web 家族适配完成 + 全仓收口五门全绿——①根 reactor `clean test` BUILD SUCCESS（REQ-001 验收字面）；②28
  份模板 yml 的 `egon.cola.component.cache.*` 键集全等（TEST-021）；③`generate` 后 `check` 无 diff（TEST-022）；④ownership 脚本
  exit 0 违例 0（基线 7 条清零，PC-002 闭合）；⑤`check-native-archetype-boundaries.py` 与
  `check-archetype-family-boundaries.py` exit 0（无未授权依赖边/家族越界）。
- End state: 全部 11 Step 交付完成，特性处于"默认关、可灰度开启"的可发布态；§8 质量门全部闭合。
- Test-first gate: Not applicable — web 面同族适配 + 收口验证为脚本/全量构建门。
- Manual Checks: MC-CONFIG-001, MC-DEP-001, MC-BEAN-001, MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 4, Rule 5, Rule 7, Rule 11
- Ordered files: 两 infra pom → 两组 yml → 两 UserRepository → README 组 → 再生成 → （Step 级）全仓回归。
- Validation working directory: 仓库根目录
- Verification command:
  `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml clean test && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml clean test && ./scripts/generate_archetypes.sh generate && ./mvnw -B -ntp clean test && ./scripts/generate_archetypes.sh check && python3 scripts/check-archetype-dependency-ownership.py && python3 scripts/check-native-archetype-boundaries.py && python3 scripts/check-archetype-family-boundaries.py`
- Expected result: 两工程与根 reactor 全量 BUILD SUCCESS；check 无 diff；ownership 脚本违例 0 条 exit 0；两边界脚本 exit 0。
- Failure returns to: 根回归红按首个失败模块回对应 Step 文件（Step 5/6 门控用例需 `-Degon.cola.cache.redis.it=true`
  环境，Docker 缺位按 Assumptions 跳过为合法绿）。
- Completion criteria: 上列五门齐备 + 28 文件键集比对（内联 python 逐文件取 `egon:cola:component:cache` 子树键路径集合断言全等）通过。
- Rollback: 本 Step 路径 `git checkout --`；全仓收口失败禁止部分提交残留，整体回退重跑。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-prod.yml`,
  `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/user/repo/UserRepository.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/src/main/java/top/egon/cola/archetype/source/webopen/infrastructure/user/repo/UserRepository.java`,
  `egon-cola-archetypes/source-projects/egon-cola-source-web/README.md, egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.md, egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.zh-CN.md`,
  `egon-cola-archetypes/definitions/egon-cola-archetype-web/, egon-cola-archetypes/definitions/egon-cola-archetype-web-open/`
- Commit: `feat(archetypes-web): complete cache wiring across families and pass full-repo regression`

#### File 1 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml`

- Purpose: web infra 无版本 cache 引用 + mp-ext 内联版本清零（第 6 条违例闭合）。
- Symbols: :19-23 mp-ext 块删版本行 + 新 cache 条目。
- Repository evidence: 实测结构与 service infra 逐同（web 家族为 4+1 模块 COLA 剖面）。
- Dependencies and consumers: web UserRepository 编译；ownership 脚本。
- Why now: 收口 Step 依赖先行。
- Contract/signature changes: 依赖 +1、版本元素 -1。
- Input/output and state mapping: 无运行态。
- Error and edge behavior: 同 Step 9 File 1。
- Standards impact: MC-DEP-001、MC-SCOPE-001。
- Implementation pseudocode:

```xml
<!-- web infra pom 实测 :19-23 mp-ext 引用块：删除版本元素（PC-002 最后两条违例之一）；引用块后追加无版本 cache 引用（字面同 Step 9 File 1）： -->
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
</dependency>
```

- Verification contribution: ownership 违例 -1。
- Literal rule enforcement: Rule 5、Rule 11。
- After this file: web infra 依赖就绪。

#### File 2 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml`

- Purpose: web-open 镜像（第 7 条违例闭合）。
- Symbols: 同 File 1。
- Repository evidence: 实测 :19-23 同构。
- Dependencies and consumers: 同 File 1。
- Why now: 家族对称。
- Contract/signature changes: 同 File 1。
- Input/output and state mapping: 同 File 1。
- Error and edge behavior: 同 File 1。
- Standards impact: MC-DEP-001——无版本引用且清零本根内联违例；MC-SCOPE-001——单 pom 编辑块；其余同 File 1。
- Implementation pseudocode:

```xml
<!-- web-open infra pom 实测 :19-23：与 File 1 相同两处编辑（删版本元素 + 无版本 cache 引用） -->
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-cache-spring-boot-starter</artifactId>
</dependency>
```

- Verification contribution: ownership 违例清零点（此后脚本应 exit 0）。
- Literal rule enforcement: Rule 5、Rule 11。
- After this file: 7 违例全部闭合。

#### File 3 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`

- Purpose: web 4 yml 注入 13 键块。
- Symbols: 同 Step 9 File 3 键树。
- Repository evidence: 实测 4 文件位于 web-starter resources；`application-prod.yml:48-55` 实测含 `egon.cola.component`
  树并入点。
- Dependencies and consumers: Properties 绑定；28 文件全量比对。
- Why now: 与依赖同 Step。
- Contract/signature changes: 每文件 +1 块。
- Input/output and state mapping: 与既有 20 文件逐字节同块。
- Error and edge behavior: 同 Step 9 File 3。
- Standards impact: MC-CONFIG-001。
- Implementation pseudocode:

```text
同 Step 9 File 3 的 13 键 yaml 块逐字节注入 web starter resources 4 份文件（§5 目录约定）；
enabled 全 profile false；插入位置仍为既有 egon.cola.component 树下与 mybatis-plus 同级；
完成后本根键集与其余 6 根全等，进入本 Step 收口段的 TEST-021 全 28 文件比对。
```

- Verification contribution: REQ-016；全量比对成员。
- Literal rule enforcement: Rule 7、Rule 10。
- After this file: web 配置就位。

#### File 4 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application.yml, egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-dev.yml, egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-test.yml, egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-prod.yml`

- Purpose: web-open 4 yml 注入（28 文件集补全）。
- Symbols: 同 File 3。
- Repository evidence: 实测路径存在。
- Dependencies and consumers: 同 File 3。
- Why now: 家族对称。
- Contract/signature changes: 同 File 3。
- Input/output and state mapping: 同 File 3。
- Error and edge behavior: 同 File 3。
- Standards impact: MC-CONFIG-001——13 键块与全根同集；MC-TIME-001——时长值一律 ISO-8601 `PT` 字面量；其余同 File 3。
- Implementation pseudocode:

```text
同 Step 9 File 3 的 13 键 yaml 块逐字节注入 web-open starter resources 4 份文件（§5 目录约定）；
enabled 全 profile false；13 键一个不多一个不少（batch/lock/redis 子树层级与 File 3 逐字符一致）；
与 File 3 合计 8 文件字面一致，家族内比对随本 Step 收口门 TEST-021 执行。
```

- Verification contribution: REQ-016；TEST-021 全 28 集达成。
- Literal rule enforcement: Rule 7、Rule 10。
- After this file: 28/28 yml 键块齐备。

#### File 5 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/user/repo/UserRepository.java`

- Purpose: web 代表 repo 注入示例（EVD-014 点名的模板剖面原文件）。
- Symbols: 同 Step 9 File 5 的 imports + `cachePortProvider` 字段。
- Repository evidence: EVD-014 实测该类即 house style 出处（`@Repository("userRepository")` + Qualifier 字段群）。
- Dependencies and consumers: Step 7 seam；web 工程编译。
- Why now: File 1 依赖就绪后。
- Contract/signature changes: 字段 +1。
- Input/output and state mapping: 同 Step 9 File 5。
- Error and edge behavior: 同 Step 9 File 5。
- Standards impact: MC-BEAN-001、MC-NAME-001。
- Implementation pseudocode:

```java
// 与 Step 9 File 5 逐字符相同（user.repo 包名之外字面一致）：
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;
```

- Verification contribution: web 编译绿 + 装配示例面。
- Literal rule enforcement: Rule 4、Rule 11。
- After this file: web 示例完成。

#### File 6 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/src/main/java/top/egon/cola/archetype/source/webopen/infrastructure/user/repo/UserRepository.java`

- Purpose: web-open 镜像示例。
- Symbols: 同 File 5。
- Repository evidence: 实测包段 `webopen` 同构。
- Dependencies and consumers: 同 File 5。
- Why now: 家族对称。
- Contract/signature changes: 同 File 5。
- Input/output and state mapping: 同 File 5。
- Error and edge behavior: 同 File 5。
- Standards impact: MC-BEAN-001——`@Qualifier("egonColaCachePort")` 显式命名传导；MC-NAME-001——字段名 `cachePortProvider`
  语义钉死；其余同 File 5。
- Implementation pseudocode:

```java
// 与 File 5 逐字符一致（包名 source.webopen.infrastructure.user.repo）：
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;
```

- Verification contribution: web-open 编译绿。
- Literal rule enforcement: Rule 4、Rule 11。
- After this file: 两示例完成。

#### File 7 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/README.md, egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md, egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.md, egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.zh-CN.md`

- Purpose: web 家族双语 README 缓存骨架节。
- Symbols: 无代码符号；文案 = Step 9 File 7 模板。
- Repository evidence: 四文件实测双语镜像剖面。
- Dependencies and consumers: 宿主使用者。
- Why now: 源冻结后成文。
- Contract/signature changes: 文档追加。
- Input/output and state mapping: 键名抄录一致。
- Error and edge behavior: 同 Step 9 File 7。
- Standards impact: MC-CONFIG-001。
- Implementation pseudocode:

```text
4 份 README 各追加"二级缓存骨架（默认关闭）"节，文案 = Step 9 File 7 模板（英文/中文各自对应语言版本）：
13 键表、启用步骤、零影响承诺；示例命令路径替换为 web/web-open 工程实际路径；
键表内容与 starter README、yml 键块三方逐字一致（Rule 7 文档面）。
```

- Verification contribution: REQ-016 文档面。
- Literal rule enforcement: Rule 7。
- After this file: 文档就绪，触发再生成与全仓收口。

#### File 8 —
`GENERATED egon-cola-archetypes/definitions/egon-cola-archetype-web/, egon-cola-archetypes/definitions/egon-cola-archetype-web-open/`

- Purpose: web 双模板再生成，7 definitions 全部与源同步。
- Symbols: 无手写符号。
- Repository evidence: 目录名实测；机制同 Step 9 File 8。
- Dependencies and consumers: File 1~7。
- Why now: 产物同提交固化后，才能执行收口三脚本。
- Contract/signature changes: 目录刷新。
- Input/output and state mapping: 脚本映射。
- Error and edge behavior: 任一 definitions 与源不一致 → check 红。
- Standards impact: MC-ARCH-001。
- Implementation pseudocode:

```text
./scripts/generate_archetypes.sh generate    # web 两家族 definitions 全量再生成
./scripts/generate_archetypes.sh check       # 源与生成物零 diff
# 生成内容 = File 1~7 的源改动投影（pom 引用、13 键块、repo 注入、README 节）；
# 禁止手改 definitions 下任何文件——check 有 diff 时回对应源文件修正后重新 generate。
```

- Verification contribution: TEST-022 与 §8 收口门全量执行点。
- Literal rule enforcement: Rule 11。
- After this file: 执行本 Step 验证命令全段五门通过后提交本 Step——全部 11 Step 完成。

> **执行修订 AM-003（用户批准 Option A，2026-09-19，Step 11 根回归门）**：根 `./mvnw -B -ntp clean test` 唯一失败点为
`egon-cola-component-bytecode-test` 的 `BytecodeReleaseShapeTest.publishesOnePremainOnlyShadedAgent:37`（"main Agent JAR
> is missing"）——基线既有缺陷：shade 绑定于 agent 模块 `package` 相位（agent pom :42），`clean test` 生命周期永不产
> jar，且该测试无门控（同文件姊妹测试 :91 有 `assumeTrue(egon.release.shape)`）；证据链：
`git diff dd7389c1e..HEAD -- egon-cola-component-bytecode/` 为空（本特性零触碰），基线 dd7389c1e 干净 worktree 实测同测试同消息复现，根
> reactor `-fn` 全量运行仅此一枚 FAILURE、其余全绿。处置：专用纠正提交
`test(bytecode): gate the agent jar release-shape assertion behind the release property`（仅 1 个测试文件，在 Step 11
> 提交之前先行落地）；实测：模块 `clean test` SUCCESS（2 skipped），`clean package -DskipTests` 后 `-Degon.release.shape=true`
> 定向运行该测试 PASSED（门控非死化）；同性质开关下姊妹 `releaseProfileAttachesSourcesAndJavadocs...` 在未加 `-Prelease`
> 时的失败为既有越界行为，不属本纠正。

## 8. Test, Validation, and Quality Gates

下表汇编各 Step 已声明的验证门（命令、工作目录、预期结果与失败返回点与 Step 正文逐字一致），供执行者按 Step 内点位运行；本
Plan 只定义未来验证，不声称任何门已在本阶段运行通过。

| Gate/order                                               | Working directory | Command or method                                                                                                                                                                                                                                                                                                                                             | Scope                                                                             | Expected result                                                                                                  | Failure returns to                                 | Requirements/runtime boundary                                                            |
|----------------------------------------------------------|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|------------------------------------------------------------------------------------------|
| 依赖治理门 for Step 1（test-first Not applicable，纯构建治理）        | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter,:egon-cola-component-dynamic-thread-pool-starter -am dependency:tree -Dincludes=org.redisson`，随后 `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-access-guard-starter,:egon-cola-component-dynamic-thread-pool-starter -am clean test` | components 父 pom 与两 starter pom                                                   | 两模块依赖树各含 `org.redisson:redisson:jar:3.26.0`（access-guard 侧标 optional），无 version-missing 错误，两模块既有测试 BUILD SUCCESS | File 1（管理位坐标或插入位置）                                 | `REQ-014`；components 模块                                                                  |
| RED for Step 2                                           | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-core -am clean test`（Step 2 File 1 后运行）                                                                                                                                                                                                                                       | `EgonColaCachePortContractTest`                                                   | 编译失败：符号 `EgonColaCachePort` 不存在（预期 RED）                                                                          | Step 2 File 1                                      | `REQ-010`；common-core                                                                    |
| GREEN for Step 2                                         | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-core -am clean test`（File 2 后运行）                                                                                                                                                                                                                                              | common-core 全模块                                                                   | 契约测试 3+ 用例全绿；`CoreBoundaryTest`/`CoreConsolidationTest` 保持全绿；BUILD SUCCESS                                       | Step 2 File 2（签名偏离契约）                              | `REQ-010`；common-core                                                                    |
| RED for Step 3                                           | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test`（Step 3 File 3 后运行）                                                                                                                                                                                                                  | `EgonColaCachePropertiesTest`                                                     | 编译失败：符号 `EgonColaCacheProperties` 不存在（预期 RED）                                                                    | Step 3 File 3                                      | `REQ-001`；cache starter                                                                  |
| GREEN for Step 3                                         | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test`（File 4 后运行）                                                                                                                                                                                                                         | cache starter 全模块                                                                 | 新模块编译，`EgonColaCachePropertiesTest` 全绿，BUILD SUCCESS                                                             | Step 3 File 1（依赖面缺位）                               | `REQ-001`；cache starter                                                                  |
| RED for Step 4                                           | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test`（Step 4 File 1~2 后运行）                                                                                                                                                                                                                | `EgonColaCacheChangedEventTest`、`EgonColaCacheCodecsTest`                         | 两测试类因信封/枚举/哨兵/编解码符号缺失编译失败（预期 RED）                                                                                | Step 4 File 1/File 2                               | `REQ-004`, `REQ-012`, `REQ-018`；cache starter                                            |
| GREEN for Step 4                                         | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test`（File 3~6 后运行）                                                                                                                                                                                                                       | cache starter 全模块                                                                 | `EgonColaCacheChangedEventTest`、`EgonColaCacheCodecsTest` 与 Step 3 测试全绿，BUILD SUCCESS                            | File 5/File 6（守卫规则或 codec 配置偏离 spec）               | `REQ-004`, `REQ-012`, `REQ-018`；cache starter                                            |
| RED for Step 5                                           | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test -Degon.cola.cache.redis.it=true`（Step 5 File 2~3 后运行）                                                                                                                                                                                | `EgonColaTwoLevelCacheManagerTest`、`EgonColaTwoLevelCacheTest`                    | 三测试类因 Manager/Cache 符号缺失编译失败（预期 RED；夹具 File 1 先行）                                                                | Step 5 File 2/File 3                               | `REQ-002`, `REQ-007`~`REQ-009`, `REQ-013`；cache starter + Testcontainers Redis（需 Docker） |
| GREEN for Step 5                                         | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test -Degon.cola.cache.redis.it=true`（File 4~5 后运行）                                                                                                                                                                                       | cache starter 全模块                                                                 | `EgonColaTwoLevelCacheManagerTest`、`EgonColaTwoLevelCacheTest` 与既有 Step 3/4 测试全绿，BUILD SUCCESS                   | File 5（内核协议分支）                                     | `REQ-002`, `REQ-007`~`REQ-009`, `REQ-013`；cache starter                                  |
| RED for Step 6                                           | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test -Degon.cola.cache.redis.it=true`（Step 6 File 1~4 后运行）                                                                                                                                                                                | 监听器/端口/收敛/装配四测试类                                                                  | 四测试类因 Listener/Port/AutoConfiguration 符号缺失编译失败（预期 RED）                                                           | Step 6 File 1~File 4                               | `REQ-003`, `REQ-005`, `REQ-011`~`REQ-013`, `REQ-018`, `REQ-025`；cache starter            |
| GREEN for Step 6                                         | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter -am clean test -Degon.cola.cache.redis.it=true`（File 5~9 后运行）                                                                                                                                                                                       | cache starter 全模块                                                                 | 模块全部测试类（含 Step 3~5 既有）绿，BUILD SUCCESS                                                                            | File 5/File 6（分发与登记协议分支）                           | `REQ-003`, `REQ-005`, `REQ-011`~`REQ-013`, `REQ-018`, `REQ-025`；cache starter            |
| RED for Step 7                                           | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am clean test`（Step 7 File 1 与 File 3 围栏修订后运行）                                                                                                                                                                            | `EgonColaRepositoryCacheEnhancementTest`、修订后 `EgonColaRepositoryArchitectureTest` | File 1 因 `getByCache`/`listByCache`/seam 符号缺失编译失败；File 3 等式拒绝尚不存在的两方法（双 RED，预期）                                  | Step 7 File 1/File 3                               | `REQ-005`, `REQ-006`, `REQ-010`~`REQ-012`, `REQ-017`, `REQ-025`；mp-ext                   |
| GREEN for Step 7                                         | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am clean test`（File 2 后运行）                                                                                                                                                                                                | mp-ext 全模块                                                                        | 既有 `EgonColaRepositoryTest` 与修订后围栏全绿；缓存读 5 项验收用例通过；BUILD SUCCESS；若仅围栏失败回 File 2 对齐方法集，禁止放宽 File 3 等式             | Step 7 File 2（登记与直通分支）                             | `REQ-005`, `REQ-006`, `REQ-010`~`REQ-012`, `REQ-017`, `REQ-025`；mp-ext                   |
| BOM 安装门 for Step 8（test-first Not applicable，纯构建元数据）     | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-components-bom clean install`（Step 8 File 1 后运行）                                                                                                                                                                                                                                               | components BOM                                                                    | BUILD SUCCESS；本地仓库当日 BOM 的 effective pom 含三条新条目（`mvn help:effective-pom -pl :egon-cola-components-bom` 复核）       | File 1（坐标/属性拼写或插入位置）                               | `REQ-014`, `REQ-015`；本地仓库                                                                |
| light 家族门 for Step 9（test-first Not applicable，脚本+构建为验收） | 仓库根目录             | `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml clean test && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml clean test && ./scripts/generate_archetypes.sh generate && ./scripts/generate_archetypes.sh check && python3 scripts/check-archetype-dependency-ownership.py` | light 两源工程 + definitions 再生成                                                      | 两工程 BUILD SUCCESS；check 无 diff；ownership 违例列表只剩 5 条非 light 条目（无 light 路径违例）                                      | File 1/File 2（引用坐标或版本元素残留）；键集比对失败回 File 3/File 4   | `REQ-014`~`REQ-017`；archetypes light 家族                                                  |
| service 家族与 agent 门 for Step 10                          | 仓库根目录             | Step 10 验证命令五段（service、service-open、agent 三工程 `clean test` + `./scripts/generate_archetypes.sh generate` 后 `check` + ownership 脚本）                                                                                                                                                                                                                            | service/service-open/agent 三源工程 + definitions                                     | 三工程 BUILD SUCCESS；check 无 diff；ownership 违例列表仅剩 2 条 web 家族条目；agent 构建红多因误动其 repo 类（不在本 Step 文件集，必须回退）            | File 1~File 3（pom）；repo 注入示例回 File 7/File 8        | `REQ-014`~`REQ-017`；archetypes service 家族 + agent                                        |
| web 家族与收口门 for Step 11                                   | 仓库根目录             | Step 11 验证命令八段：两 web 工程 `clean test` → `./scripts/generate_archetypes.sh generate` → 根 `./mvnw -B -ntp clean test` → `./scripts/generate_archetypes.sh check` → `python3 scripts/check-archetype-dependency-ownership.py` → `python3 scripts/check-native-archetype-boundaries.py` → `python3 scripts/check-archetype-family-boundaries.py`                   | 全仓（components + xingyuan + archetypes）                                            | 两工程与根 reactor 全量 BUILD SUCCESS；check 无 diff；ownership 违例 0 条 exit 0；两边界脚本 exit 0                                 | 根回归红按首个失败模块回对应 Step 文件（门控用例缺 Docker 按 §6 假设跳过为合法绿） | `REQ-001`, `REQ-014`~`REQ-017`；全仓                                                        |
| 机器围栏（静态，随 Step 7 与 Step 9~11 各自提交前运行）                    | 仓库根目录             | `EgonColaRepositoryArchitectureTest`（TEST-017）+ TEST-018 依赖扫描 + 四脚本（generate check / ownership / 两边界脚本）                                                                                                                                                                                                                                                       | 契约面与依赖面                                                                           | 接口 57 钉数不变；实现公有方法集合精确相等；mp-ext 与 common-core 零 cache 包 import；脚本各 exit 0                                         | Step 7 File 2/File 3；各家族 pom/yml 文件                | `REQ-010`, `REQ-015`~`REQ-017`；静态                                                        |
| 发布验证（全仓回归 + 完整集成一次）                                      | 仓库根目录             | `./mvnw -B -ntp clean test`（无 Docker 也可过的收口面）与 `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-cache-spring-boot-starter,:egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am clean test -Degon.cola.cache.redis.it=true`（有 Docker，至少一次全绿）                                                      | 根聚合 + 两核心模块门控面                                                                    | 两命令 BUILD SUCCESS；TEST-009~TEST-016 门控用例在有 Docker 运行中全部执行并通过                                                     | 对应 Step（Step 5/6/7）                                | 全部 REQ；用户可控运行时                                                                           |

运行时机口径：focused=每 Step 的 Step 验证命令（RED 在测试文件落盘后、GREEN 在生产文件完成后各运行一次）；module=同一命令的
`-am clean test` 全模块面；cross-module=Step 8 BOM install 与 Step 9~11 各家族源工程构建；full=Step 11 收口段根
`./mvnw -B -ntp clean test`（覆盖 components、xingyuan、archetypes 三大聚合，根 pom :58-60 modules 实测）；migration 与
frontend 门=Not applicable——主 Spec §11 零关系 schema、§12 零前端，本 Plan §5 树无任何迁移或前端文件；manual/runtime 门=Not
applicable——纯组件库无独立部署面，宿主可见验证即发布验证行的两命令。

## 9. Migration, Compatibility, Rollout, and Rollback

- 数据库迁移：**N/A**——主 Spec §11 明示零关系 schema 改动（缓存仅存活于 Redis L2 与 Guava L1，无持久化表），本 Plan §5 文件树无任何
  Flyway 文件；无历史迁移触碰，无 backfill，无 dual-read/write。
- 契约与事件兼容性：`EgonColaIRepository` 契约面零改动（57 方法钉数不变，Step 7 File 3 围栏锁定）；`getByCache`/`listByCache`
  仅增于实现类 `EgonColaRepository`，对既有子类与调用方二进制/源码兼容。缓存事件走新增独立 `RTopic`（`redis.topic` 键，默认值即
  Step 3 Properties 常量），与业务事件总线零交集（09-12 Spec carve-out）；未装配缓存的旧节点不订阅、不收发，天然向后兼容。
  `CacheManager` bean 以 `@ConditionalOnMissingBean(CacheManager.class)` 让位宿主自定义，接入零抢占。
- 配置与 feature flag：唯一总开关 `egon.cola.component.cache.enabled` 默认 false，且 Step 9~11 写入 28 份 yml 的全部
  profile 一律 false（REQ-011/REQ-016）——archetype 使用者拉取更新后零行为变化；13 个键的键集在 7 根 × 4 文件间结构一致（Rule
  7，TEST-021 比对）。
- Rollout 顺序：①Steps 1~7 组件侧合并进 main 并跑全仓回归；②Step 8 BOM install 使下游可解析；③Steps 9~11 archetype
  家族逐个提交（light→service/agent→web），每家族以 generate+check 同提交消除源/生成物漂移窗口；④宿主启用序：单模块（单缓存区域）在
  dev/test profile 置 `enabled=true` → 观察 TEST-020 装配矩阵等价行为与错误码日志 → 多节点启用（要求共享
  Redis，时钟偏差不影响收敛，收敛上界=min(TTL, 二次延迟)，REQ-013）。
- 发布后检查：装配矩阵（缺省/false 零 bean；true 且无 `RedissonClient` 时启动 fail-fast `CACHE_REDISSON_CLIENT_MISSING`
  ）、双节点收敛用例（TEST-011/012 同型场景在真实 Redis 复跑一次）、错误码日志抽样。
- Rollback：每 Step 单语义提交，逆序 `git revert` 即可；Step 7 实现与围栏修订同提交原子回退（无"围栏放宽"中间态）；组件 jar
  层回退=移除 BOM 解析出的 starter 依赖；运行层 kill-switch=`enabled=false`（仅改配置回退，无需回码重部署）；缓存数据无需清理——L2
  逐条 TTL 自然过期，L1 随进程消亡。
- Forward-fix 与防回退：TEST-017/TEST-018 围栏与 ownership/边界脚本常驻 CI，后续误改（内联版本、契约面扩张、cache 包 import
  渗透）即时红。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section                                                                                         | Steps              | Files                                                                                                                                      | Tests/gates                                  | Completion evidence                                                                      |
|-------------|----------------------------------------------------------------------------------------------------------------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|------------------------------------------------------------------------------------------|
| `REQ-001`   | [主 Spec](../spec/2026-09-18-15-37-two-level-cache-redis-event-starter-repo-enhancement.md) §4 REQ-001 行；§8 文件树 | Steps 3, 6, 11     | Step 3 File 1~4（starter pom、common 聚合 `pom.xml`、`EgonColaCacheProperties` 与测试）、Step 6 File 7~8（`EgonColaCacheAutoConfiguration` + imports） | TEST-020；§8 Step 3 RED/GREEN 与 Step 11 收口门   | 新模块入根 reactor 构建通过；缺省配置 `ApplicationContextRunner` 断言零缓存 bean                            |
| `REQ-002`   | 主 Spec §4；§7 架构设计；§9 接口定义                                                                                      | Steps 5, 6         | Step 5 File 4~5（`EgonColaTwoLevelCacheManager`/`EgonColaTwoLevelCache`）、Step 6 File 7（bean 暴露）                                             | TEST-004、TEST-020                            | L2 命中回填 L1 三态矩阵绿；`@Cacheable` 经同一 CacheManager 可用                                        |
| `REQ-003`   | 主 Spec §4；§7；§9                                                                                                | Steps 4, 5, 6      | Step 4 File 5（信封）、Step 5 File 5（发布原语）、Step 6 File 5/File 7（单监听器与订阅）                                                                        | TEST-003、TEST-011                            | 节点 A 失效→节点 B L1 清除；全模块恰一处 `addListener`（静态扫描）                                            |
| `REQ-004`   | 主 Spec §4；§9.1 事件契约；§10.2 信封模型                                                                                 | Steps 4, 6         | Step 4 File 3（操作枚举）、File 5（record 信封 + 嵌套 `KeyGuard`）                                                                                      | TEST-002、TEST-011                            | 编解码往返通过；自回声跳过断言绿                                                                         |
| `REQ-005`   | 主 Spec §4；§7；§9                                                                                                | Steps 6, 7         | Step 6 File 6（`EgonColaTwoLevelCachePort` afterCommit 缓冲）、Step 7 File 2（15 写方法登记）                                                          | TEST-009、TEST-012、TEST-013                   | 提交后一次失效+一次发布+一次延迟第二拍；回滚零交互                                                               |
| `REQ-006`   | 主 Spec §4；§9                                                                                                   | Step 7             | Step 7 File 2（`getByCache`/`listByCache` + 两个 protected 挂点）                                                                                | TEST-005、TEST-015                            | 与 `getById`/`listByIds` 逐字段等义；命中槽 0 SQL                                                  |
| `REQ-007`   | 主 Spec §4；§10 模型（哨兵）                                                                                           | Steps 4, 5         | Step 4 File 4（`EgonColaCacheNullValueBO`）、Step 5 File 5（哨兵读写）                                                                              | TEST-006                                     | 连续两次 miss loader 恰一次；L2 值为哨兵序列化形态                                                        |
| `REQ-008`   | 主 Spec §4；§7（抖动协议）                                                                                             | Step 5             | Step 5 File 5（嵌套 `Jitter`，种子可注入）                                                                                                           | TEST-008（区间口径 PC-004）                        | 100 key TTL 离散度全落 `[expire, expire+jitter]`                                              |
| `REQ-009`   | 主 Spec §4；§7（互斥回源协议）                                                                                           | Step 5             | Step 5 File 5（`RLock.tryLock` + recheck + 超时降级直读不写缓存）                                                                                      | TEST-007、TEST-016                            | 8 线程并发 loader 恰一次；超时线程仍成功返回                                                              |
| `REQ-010`   | 主 Spec §4；§8 文件树（端口在 common-core）；§9                                                                           | Steps 2, 7         | Step 2 File 2（`EgonColaCachePort` 纯 JDK 签名）、Step 7 File 3（围栏依赖扫描项）                                                                         | TEST-018、TEST-019                            | mp-ext 与 common-core 零 `top.egon.cola.component.common.cache` import；最小 classpath 负例读写正常 |
| `REQ-011`   | 主 Spec §4；§7（开关语义）                                                                                             | Step 7             | Step 7 File 2（seam 返回 null 时直通 `getById`/`listByIds`，写路径 `registerCacheEviction` 早退）                                                       | TEST-005、TEST-019、TEST-024                   | 无端口装配行为与基线快照逐项相等；Redis 交互计数 0                                                            |
| `REQ-012`   | 主 Spec §4；§9（守卫契约）；§10                                                                                         | Steps 4, 6, 7      | Step 4 File 5（`KeyGuard` 键形状/glob 规则）、Step 6 File 6（登记期守卫 fail-fast）、Step 7 File 2（`tenantId:id` 键构造）                                      | TEST-001、TEST-010、TEST-012                   | 跨租户/裸 `*`/非法 glob 抛码矩阵绿；租户 43 对照键分毫未动                                                    |
| `REQ-013`   | 主 Spec §4；§7（收敛协议）；§15                                                                                         | Steps 5, 6         | Step 5 File 5（逐条 TTL 上界）、Step 6 File 6（延迟二次失效）                                                                                             | TEST-013                                     | 事件重复/乱序重放幂等；模拟丢事件节点在 TTL 前经二次失效收敛                                                        |
| `REQ-014`   | 主 Spec §4；§16 依赖治理；§17                                                                                         | Steps 1, 3, 8      | Step 1 File 1~3（父 dmu + 两 starter 去局部版本）、Step 3 File 1（新模块零版本引用）、Step 8 File 1（BOM 导出）                                                     | TEST-018（局部版本面）；§8 依赖治理门与 BOM 安装门            | `${redisson.version}` 等局部声明清零；BOM effective pom 三条新条目可解析                                 |
| `REQ-015`   | 主 Spec §4；§16                                                                                                  | Steps 9, 10, 11    | Step 9 File 1~2、Step 10 File 1~3、Step 11 File 1~2（7 根 pom 无版本引用 + PC-002 违例清理）                                                             | TEST-021、TEST-022                            | ownership 脚本违例 7→5（Step 9）→2（Step 10）→0（Step 11）exit 0                                   |
| `REQ-016`   | 主 Spec §4；§16 配置面                                                                                              | Steps 9, 10, 11    | Step 9 File 3~4/5~7、Step 10 File 4~6/7~9、Step 11 File 3~4/5~7（28 yml 键块、代表 Repository 注入示例、README 双语）                                      | TEST-021、TEST-022                            | 28 文件键集比对通过；模板 repo 编译装配通过；README 命令可执行                                                  |
| `REQ-017`   | 主 Spec §4；§14（TEST-017~022 围栏面）                                                                                | Steps 7, 9, 10, 11 | Step 7 File 3（围栏白名单增量）、Step 9 File 8、Step 10 File 10、Step 11 File 8（definitions 再生成）                                                       | TEST-017、TEST-022                            | 57 钉数不变；实现集合 = 上游 ∪ 缓存读白名单精确相等；check 无 diff；全仓绿                                          |
| `REQ-018`   | 主 Spec §4；§13.3（codec 与错误码）；§15                                                                                | Steps 4, 5, 6      | Step 4 File 2/File 6（`EgonColaCacheCodecs` PTV 受限编解码）、Step 5 File 5（吞并记码回源）、Step 6 File 5（监听器坏载荷容错）                                        | TEST-002、TEST-003、TEST-014、TEST-016、TEST-023 | 断 Redis 用例读回源成功、写提交成功、日志含稳定错误码；白名单外类型反序列化被拒                                              |
| `REQ-025`   | 主 Spec §4 REQ-005 行；§19 追溯（Depends On 08-25-19-09）                                                             | Steps 6, 7         | Step 6 File 6（`TransactionSynchronizationManager` afterCommit 登记）、Step 7 File 2（写路径仅登记不立即执行）                                               | TEST-009、TEST-013                            | 提交后副作用只在 afterCommit 发生；回滚路径零 Redis/事件交互计数                                               |

## 11. Risks, Blockers, and User Decisions

| ID        | Risk or decision                                                                                               | Impacted Steps/files                         | Evidence                                                                          | Owner | Status/action                                                                          |
|-----------|----------------------------------------------------------------------------------------------------------------|----------------------------------------------|-----------------------------------------------------------------------------------|-------|----------------------------------------------------------------------------------------|
| `RSK-001` | PC-002 基线红门：ownership 脚本在 `dd7389c1e` 即 exit=1（7 根 mp-ext 内联版本违例，非本特性引入），若不处置 REQ-015 验收不可达                    | Steps 9, 10, 11 各家族 pom 文件                   | 本会话实测输出 7 条违例逐一含行号；§2.3 漂移记录                                                      | mario | Closed——各家族引入 cache starter 的同文件提交内删除内联版本元素，违例 7→5→2→0 于 Step 9~11 逐级收敛                |
| `RSK-002` | PC-006 夹具偏离主 Spec §14.1 文案：Spec 建议 `it.ozimov:embedded-redis`，实测该管理位（父 pom :264-268）仓内零使用，现行模式为 Testcontainers | Steps 1, 5, 6（`CacheRedisTestSupport` 与模块测试） | `RedissonStoreIntegrationTest` :33 实读（GenericContainer + 系统属性门控先例）；PC-006 行       | mario | Closed——采用 Testcontainers 并按 REQ-014 提升至父 dmu；若用户改回 embedded-redis 仅替换夹具一个文件，契约与门控属性不变 |
| `RSK-003` | PC-004 抖动断言口径：REQ-008 文案"±jitter"与 TEST-008 区间断言不一致，取 TEST 为权威                                                 | Step 5 File 5（`Jitter`）与其测试                  | 主 Spec §4 REQ-008 行与 §14.3 TEST-008 行逐字对比；ASM-002（L1 ≤ L2）                        | mario | Closed——固定单侧区间 `[expire, expire+jitter]`；若需双侧仅改 `Jitter` 一个方法与测试边界常量                   |
| `RSK-004` | PC-001 嵌套协作者：`KeyGuard`/`Jitter`/`EventBus`/`EvictionBuffer` 不新增顶层文件，与 Spec 逐节点名方式存在读法差异                       | Steps 4, 5, 6                                | 主 Spec §8.2 文件树与 §13.3"10 生产类"为文件数权威                                              | mario | Closed——嵌套静态类实现；若用户要求顶层类为可机械重构，公共契约零变化                                                 |
| `RSK-005` | PC-003 archetype 引用形态：根 dmu 若加 cache 管理条目会被 ownership 脚本判违例                                                    | Steps 9, 10, 11 各 pom                        | `check-archetype-dependency-ownership.py` 对带版本 top.egon 条目（含 dmu）报错的脚本行为实测        | mario | Closed——根仅无版本引用；若脚本判定不同只调整声明位置，依赖语义不变                                                  |
| `RSK-006` | dtp-admin 子模块既有局部版本残留（其 pom 内 redisson 相关声明）不在 REQ-014 点名范围（Spec 仅列 access-guard 与 dtp-starter 两处）             | Step 1 File 3                                | 主 Spec §16 两处点名与 components 树实测                                                   | mario | Closed——本 Plan 不触碰 dtp-admin（§5 树外即越权）；留待后续依赖治理 Spec                                   |
| `RSK-007` | 执行环境无 Docker 时 TEST-004~TEST-016 门控用例跳过，Step 5/6 GREEN 覆盖面降低                                                   | Steps 5, 6, 11（发布验证行）                        | 门控属性 `-Degon.cola.cache.redis.it=true` 设计（PC-006 先例同款）；§8 发布验证行要求有 Docker 时至少一次全绿 | mario | Closed——§6 假设与 §8 口径已声明：无 Docker 跳过为合法绿，最终发布验证必须补一次门控全绿运行                              |
| `RSK-008` | Step 7 围栏原子性：实现与围栏修订若拆成两提交会出现红门中间态                                                                             | Step 7 File 2 + File 3                       | 围栏等式拒绝尚不存在的两方法（本 Step RED 声明）                                                     | mario | Closed——单提交原子推进（Commit paths 五文件同 Step），回滚点即该提交整体 revert                               |

无未闭合阻塞项：上表全部 Closed；四项用户已拍板决策（EVD-012 ①自建管理器 ②透明失效+声明式读 ③依赖+配置骨架默认关
④三项防护全含）已逐条落实到 Steps 3~7 与 9~11，无需新决策。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

用户原话四项主张——"spring cache + redisson + guava 二级缓存 starter"（Steps 3~6）、"update/失效基于 redis event、通用 event
而非逐类型 listener"（Step 4 信封 + Step 6 单监听器 + TEST-003 静态扫描锁单点）、"增强 mp-ext repo 层"（Step 7）、"依赖管理在父
pom、不要局部引入"与"archetypes 同步适配"（Steps 1/8 与 9~11）——均有对应 Step、文件与验证门；"先给我方案"已由 Accepted 主
Spec 满足。EVD-012 四项拍板决策全部按选定方案编译进计划，未夹带 Spec 外行为。

### 12.2 Spec consistency

本 Plan 未重设计架构、契约、字段、状态、schema、UI、测试、兼容或 rollout：§5 树与主 Spec §8.2 逐文件对应；§9 八契约与 §10 模型编译进
Steps 2~7 且签名逐字保留；§14.3 全部 24 个 TEST 落位到 Step 文件集。澄清仅 §6.4 PC-001~PC-006 六条，均为局部、可逆、有实测证据的实现细节，其中
PC-006 记录了对 Spec 文案的仓库实测纠偏（Testcontainers 取代 embedded-redis 叙述）。§4.5 简洁性审计结论：无
fetch-then-forward 接口、无多余 API/层/表/依赖——端口三方法均被 Step 7 写读两路径实际消费，`registerEvictionAfterCommit`
的缓冲协议为 REQ-025 不变量所迫。

### 12.3 Repository executability

全部路径/符号/命令对基线 `dd7389c1e` 实核（§2.3 记录唯一漂移及处置）：mp-ext 实际包路径
`top/egon/cola/component/common/mybatis/...`、fence 钉数 57 位于 :31、`getById:234`/`listByIds:245` 语义、7 根 pom 违例行号、28
yml 分布、`./mvnw` 与四脚本命令面均为本会话实测。每 Step 有唯一 baseline/end 状态、严格文件顺序、独立验证与单提交边界（§4.4），写
scope 不重叠；Step 间仅 Step 2→3/5/6/7、Step 8→9~11 存在硬前置，Step 9/10 家族内与 Step 10/11 之间可并行但提交不相交。

### 12.4 Test and release completeness

RED/GREEN 序覆盖全部行为面（Steps 2~7 七处 RED 声明均给出预期失败原因；Steps 1/8~11 为构建/配置/生成物面，test-first Not
applicable 并给出脚本+构建替代证明）；§10 矩阵 19 个 REQ 全覆盖且每 REQ 至少一处测试/门；迁移与前端 N/A 有据（§9）；rollout 与
rollback 以 `enabled=false` kill-switch、单 Step revert、围栏常驻 CI 三层闭合；发布验证边界=根全量回归 + 一次 Docker
门控全绿，为本 Plan 定义的未来执行口径而非既有运行时证明。

### 12.5 Blocking Manual Check

| Check ID         | Applicability    | Status | Evidence                                                                                                                                                                                                                                         | Finding                                                   | Required action/exception |
|------------------|------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|---------------------------|
| `MC-ARCH-001`    | `Applicable`     | `PASS` | §4.7 剖面声明与 §5 树（组件 starter 剖面有 access-guard/dtp/outbox 实测先例；archetype 各根沿用自身 COLA 分层）                                                                                                                                                            | 仅沿用既有剖面，不新造分层或模块剖面                                        | None（已闭合）                 |
| `MC-REUSE-001`   | `Applicable`     | `PASS` | §4.7 复用台账 10 行（Spring Cache SPI、宿主 `RedissonClient`、Jackson、`RLock`、Guava、`TransactionSynchronizationManager`、JDK 延迟调度、Testcontainers、mp-ext 既有 require 校验、零新增面）                                                                                 | 全部能力命中既有栈，组件树零新增三方件                                       | None（已闭合）                 |
| `MC-DEP-001`     | `Applicable`     | `PASS` | Step 1（redisson 与 testcontainers 提升父 dmu）、Step 3 File 1 与 Steps 9~11 各 pom（引用处无内联版本）、Step 8 File 1（BOM 导出）                                                                                                                                       | 坐标变化仅治理位点重排与新 starter/BOM 导出；ownership 脚本 Step 11 收敛 0 违例 | None（已闭合）                 |
| `MC-NAME-001`    | `Applicable`     | `PASS` | §5 树类型后缀全集（`…Event`/`…BO`/`…Properties`/`…Manager`/`…Port`/`…Listener`/`…Codecs`/枚举与 record 契约）与 §4.8 Rule 1 行                                                                                                                                   | 命名符合 POJO 语义后缀规范，无泛化名                                     | None（已闭合）                 |
| `MC-VALID-001`   | `Applicable`     | `PASS` | Step 3 File 4（`@Validated` + 原生 jakarta 约束，jitter-ratio 用原生 `@DecimalMin`/`@DecimalMax`）、Step 4 File 5（record 紧凑构造器 + `KeyGuard`）、Step 6 File 6（登记期 fail-fast 守卫）、Step 7 File 2（复用 `requireTenantId`/`requireSerializableId`）                    | 层间移交校验全走原生注解与既有 require 路径，未自写 Validator                  | None（已闭合）                 |
| `MC-MODEL-001`   | `Applicable`     | `PASS` | §4.8 Rule 3 行；Step 4 File 3~5（record 紧凑构造器、`List.copyOf` 深不可变、单例哨兵）；Step 5 File 4~5（管理/缓存类完整 Lombok 构造注入集）                                                                                                                                       | 简单对象 record、复杂对象 Lombok 全集、不可变面构造期规范化，符合 Rule 3           | None（已闭合）                 |
| `MC-CONVERT-001` | `Not applicable` | `N/A`  | §4.7 对象转换行与主 Spec §10.4（本特性无跨层 PO/BO 转换，cache starter 不引入 MapStruct；`EgonColaRepository` 既有转换路径不动）                                                                                                                                               | 无转换面可审；无绕过 `BaseConverter` 的可能                            | None                      |
| `MC-LOG-001`     | `Applicable`     | `PASS` | Step 5 File 4/5、Step 6 File 5~7 与 Step 7 端口的 Standards impact 行（业务类一律 `@Slf4j`；Step 7 实现类继承既有 `@Slf4j` 面且不双记日志）                                                                                                                                  | 日志注入符合规范且降级日志带稳定错误码                                       | None（已闭合）                 |
| `MC-BEAN-001`    | `Applicable`     | `PASS` | §4.7 Bean 命名行、Step 6 File 7（`egonColaTwoLevelCacheManager`/`egonColaCacheChangedListener`/`egonColaCachePort` 显式名 + `@RequiredArgsConstructor`）、Step 9 File 5（`@Qualifier("egonColaCachePort")` 经各根 lombok.config copyableAnnotations 传导，实测根 :2） | 单例 bean 全显式命名、注入点全列 Qualifier                             | None（已闭合）                 |
| `MC-UTIL-001`    | `Applicable`     | `PASS` | §4.7 复用台账（Guava 限 L1、JDK `Random` 种子可注入、JDK 单线程 daemon 调度、Apache/Jackson 既有位点）                                                                                                                                                                   | 工具面限定 jdk/Apache/Guava 白名单，未引 Caffeine、Kryo、Protostuff    | None（已闭合）                 |
| `MC-JSON-001`    | `Applicable`     | `PASS` | Step 4 File 6（`JsonJacksonCodec` + `JavaTimeModule` + PTV 受限白名单）、Step 6 File 5（topic 载荷 StringCodec 手工 Jackson 往返）                                                                                                                               | JSON 统一 Spring Boot Jackson，对外载荷实体经注解与白名单修饰               | None（已闭合）                 |
| `MC-TIME-001`    | `Applicable`     | `PASS` | Step 3 File 4（时长键全 `java.time.Duration`）、Step 4 File 5（信封 `Instant occurredAt`）、Step 4 File 6（ISO-8601 断言）                                                                                                                                       | 时间类型全 `java.time`，零 `java.util.Date`/`Calendar`           | None（已闭合）                 |
| `MC-CONFIG-001`  | `Applicable`     | `PASS` | Step 9 File 3 提交的 13 键 yml 块字面量（各根复用）+ TEST-021 键集比对（Steps 9~11 验证命令内）                                                                                                                                                                           | 7 根 × 4 文件全 profile 键集一致、值按 profile 可不同（Rule 7）           | None（已闭合）                 |
| `MC-PATTERN-001` | `Applicable`     | `PASS` | §4.8 Rule 9 行；Step 5 File 5（Template Method 继承 `AbstractValueAdaptingCache` + DCL 互斥回源）、Step 6 File 5（Observer + 枚举穷尽 switch 分发）                                                                                                                 | 复杂流程全部使用 Spec 点名模式，无按类型 if-else 硬编码分发                     | None（已闭合）                 |
| `MC-SCOPE-001`   | `Applicable`     | `PASS` | §5 变更树 + §7 各 Step 文件集（xingyuan 业务、interceptor 链、`EgonColaIRepository` 契约面、`light-open` bootstrap 系列均零触碰，dtp-admin 见 RSK-006）                                                                                                                    | 计划文件全部落在 REQ-001~018/REQ-025 范围内，无顺手重构                    | None（已闭合）                 |
| `MC-TEST-001`    | `Applicable`     | `PASS` | §8 门矩阵（Steps 2~7 各 RED/GREEN 配对 + 围栏/收口行）与主 Spec §14.3 全部 24 TEST 在 Steps 文件集中的落位（TEST-019~024 装配/比对/生成/白名单/批上限面）                                                                                                                                | 每 Step 验证命令同时证明命名 MC 与对应 REQ 验收                           | None（已闭合）                 |
| `MC-BLOCKER-001` | `Applicable`     | `PASS` | §11 八行全部 Closed + 上列 16 行全部 PASS 或 N/A                                                                                                                                                                                                           | 无未闭合阻塞、无未批例外                                              | None                      |

### 12.6 Final verdict

`PASS — Ready for user review`
