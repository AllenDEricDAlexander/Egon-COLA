# Egon-COLA Archetype 正常源码与 Central 发布两阶段实施 Plan

| Field              | Value                                                                                                                                                                                                                                                                        |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document           | `2026-09-01-11-23-archetype-two-stage-source-generation-implementation.md`                                                                                                                                                                                                   |
| Template Version   | `4`                                                                                                                                                                                                                                                                          |
| Status             | `Review`                                                                                                                                                                                                                                                                     |
| Created            | `2026-09-01 11:23 CST`                                                                                                                                                                                                                                                       |
| Updated            | `2026-09-01 11:45 CST`                                                                                                                                                                                                                                                       |
| Owner              | `Mario / Egon-COLA maintainers`                                                                                                                                                                                                                                              |
| Repository         | `Egon-COLA`                                                                                                                                                                                                                                                                  |
| Scope              | `egon-cola-archetypes 六套 normal source project、manifest 驱动生成器、六个稳定 maven-archetype 发布模块、版本脚本、CI 与 Maven Central 发布 Gate`                                                                                                                                                     |
| Source Requirement | `2026-08-27 用户要求正常源码 -> 生成 Archetype -> 推送 Maven Central 两阶段改造；2026-09-01 用户确认主 Spec 并明确要求开始写 Plan`                                                                                                                                                                          |
| Baseline Revision  | `main@28b9e596d；2026-09-01 11:23 CST dirty-worktree snapshot：保留 egon-cola-platforms/egon-cola-platform-admin-web-shared/tsconfig.app.tsbuildinfo 的并发修改与未提交主 Spec，不覆盖、不回退`                                                                                                    |
| Implements Spec    | [Archetype 正常源码与发布制品两阶段生成设计](../spec/2026-08-27-20-31-archetype-two-stage-source-generation.md)                                                                                                                                                                              |
| Spec Status        | `Review`                                                                                                                                                                                                                                                                     |
| Spec Revision      | `Updated 2026-08-27 20:31 CST；当前工作树中的未提交 Review 文档`                                                                                                                                                                                                                          |
| Effective Specs    | [两阶段主 Spec](../spec/2026-08-27-20-31-archetype-two-stage-source-generation.md)；[Archetype MyBatis-Plus unification](../spec/2026-08-25-19-09-archetype-mybatis-plus-unification.md)；[Open-source archetype family](../spec/2026-08-23-16-43-open-source-archetype-family.md) |
| Depends On Plans   | `None`                                                                                                                                                                                                                                                                       |
| Supersedes         | `None`                                                                                                                                                                                                                                                                       |
| Superseded By      | `None`                                                                                                                                                                                                                                                                       |
| Related Plans      | [Archetype MyBatis-Plus Implementation Plan](2026-08-25-20-02-archetype-mybatis-plus-implementation.md)；[Open-Source Archetype Family Implementation Plan](2026-08-23-19-14-open-source-archetype-implementation.md)                                                         |

## 1. Summary

本 Plan 实施两阶段主 Spec：把六套模板中的 Java、POM、配置、测试与项目文档迁入六个可直接
`clean verify` 的正常 Maven 工程，再由固定的
`maven-archetype-plugin:3.4.1:create-from-project` 单向生成 ignored `.generated` 工作区，最后由六个现有
`maven-archetype` 模块组合 curated metadata、post-generate、IT、sources/javadoc/GPG 附件并继续通过根 Reactor
一次发布到 Central。

实施分 9 个顺序 Step：先建立可失败注入的生成器合同；按 Light、Service、Web 三组建立 normal source；接入六个
manifest；按同样三组切换发布模块；最后更新版本、CI 和发布链路。完成证据是六个 source reactor build、生成器
determinism/atomic/round-trip Gate、六个 Archetype IT、无签名 release-shape、真实受保护 workflow 的签名/发布前置条件，
以及 old Flyway 路径与 SHA-256 全量不变。本 Plan 不执行真实 deploy、不启动服务/浏览器/Docker/外部基础设施。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Archetype 正常源码与发布制品两阶段生成设计](../spec/2026-08-27-20-31-archetype-two-stage-source-generation.md)
- Status: `Review`
- Revision: `Updated 2026-08-27 20:31 CST；设计 baseline main@907e0a26e756d94390e3ae579ebd4306cccc9381`
- Approval evidence: 用户于 2026-09-01 明确回复“确认，开始写 plan”，授权针对 Review 主 Spec 规划；未批准本 Plan 实施，故本
  Plan 保持 `Review`。

### 2.2 Effective Spec set

| Role                 | Spec/link                                                                                            | Status/revision                 | Effective sections                                  | Why included                                |
|----------------------|------------------------------------------------------------------------------------------------------|---------------------------------|-----------------------------------------------------|---------------------------------------------|
| Primary              | [两阶段主 Spec](../spec/2026-08-27-20-31-archetype-two-stage-source-generation.md)                       | `Review / 2026-08-27 20:31 CST` | `§1-§20`                                            | 决定源码所有权、生成 CLI、package/release、Flyway 与验证边界 |
| Amending dependency  | [Archetype MyBatis-Plus unification](../spec/2026-08-25-19-09-archetype-mybatis-plus-unification.md) | `Review / 2026-08-25 22:28 CST` | `§1、§3、§5-§18`，但 `§8、§14、§16` 的路径/生成/发布内容由主 Spec 修订 | 提供六族当前有效 Java、模块、配置、schema、测试与 verifier 合同  |
| Normative dependency | [Open-source archetype family](../spec/2026-08-23-16-43-open-source-archetype-family.md)             | `Review / 2026-08-23 19:42 CST` | `§1、§3、§5-§18`，但 `§8、§14、§16` 的路径/生成/发布内容由主 Spec 修订 | 提供三套 `-open` 的七模块/手工 SQL/Proto/OpenAPI 边界   |

### 2.3 Superseded or excluded content

- 两个 predecessor Spec 中把 `src/main/resources/archetype-resources` 当作日常源码事实源的 `§8` 内容被主 Spec 修订。
- 两个 predecessor Spec 中 template-first 验证与 rollout 顺序的 `§14/§16` 内容被主 Spec 的 source -> generate ->
  package -> release 顺序修订。
- 业务 Java、API/RPC/GraphQL/MQ、模型、schema、配置值、生成模块拓扑、GAV 和 verifier 断言均未被替换。
- 已完成的旧 Implementation Plan 只作为当前内容来源和路径证据，不成为本 Plan 的执行依赖。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section                                                            | Effective statement                                                       | Observable acceptance                                               | Implementation impact                      |
|-------------|--------------------------------------------------------------------------------|---------------------------------------------------------------------------|---------------------------------------------------------------------|--------------------------------------------|
| `REQ-001`   | [主 Spec](../spec/2026-08-27-20-31-archetype-two-stage-source-generation.md) §4 | 六个产品各有正常 Maven 源码工程                                                       | 六工程无 Archetype 占位符并 `clean verify`                                  | Steps 2-4 建 normal source 与 source reactor |
| `REQ-002`   | 主 Spec §4                                                                      | normal source 是唯一可编辑业务事实源                                                 | package 模块除不可变 Flyway archive 外不再跟踪业务模板；`.generated` 无 tracked file | Steps 1、6-8                                |
| `REQ-003`   | 主 Spec §4/§9                                                                   | 固定使用 Plugin 3.4.1 `create-from-project`                                   | 六 manifest 均出现固定 goal 构建日志和预期 resources                             | Steps 1、5                                  |
| `REQ-004`   | 主 Spec §4/§9                                                                   | 动态发现 manifest，不硬编码六 GAV                                                   | manifest/package 数相等；临时新增/删除/重复 fixture 有稳定结果                       | Steps 1、5、9                                |
| `REQ-005`   | 主 Spec §4/§8                                                                   | `.generated` ignored、read-only、fresh checkout 可重建                         | `.gitignore` 精确命中；`git ls-files` 为零；缺目录 package fail closed         | Steps 1、5-8                                |
| `REQ-006`   | 主 Spec §4/§7/§9                                                                | 六套输出原子替换并互斥                                                               | 中途失败/信号/并发不产生混合代际，旧完整集合保留                                           | Steps 1、5                                  |
| `REQ-007`   | 主 Spec §4/§8                                                                   | curated metadata/post/IT 不被自动产物覆盖                                         | 六套 descriptor 基线 hash/结构不变且 package 只 overlay generated resources   | Steps 5-8                                  |
| `REQ-008`   | 主 Spec §4/§14                                                                  | source -> template -> same-sentinel consumer round-trip 等价                | 规范化路径/文本/执行位/POM diff 为零，双次 hash 一致                                 | Steps 1、5-8                                |
| `REQ-009`   | 主 Spec §4/§14                                                                  | 六 GAV、输入参数与 1/6/7 模块拓扑兼容                                                  | 六 IT 与外部 consumer generation/verify 通过，无 sentinel 泄漏                | Steps 2-8、9                                |
| `REQ-010`   | 主 Spec §4/§16                                                                  | source projects 不进入根 Central Reactor                                      | root modules 不增加 source；staging 无 `egon-cola-source-*` artifact     | Steps 4、9                                  |
| `REQ-011`   | 主 Spec §4/§16                                                                  | root `all` 仍为单 bundle，generate 是 deploy 前置                                | workflow 只在 source/generate/check/IT/shape 全绿后执行一次 root deploy      | Step 9                                     |
| `REQ-012`   | 主 Spec §4/§14                                                                  | 六 Archetype 都有 main/sources/javadoc，真实发布均有 `.asc`                         | local shape 检查前三类；protected publish 检查 POM/三类 JAR 签名                | Steps 6-9                                  |
| `REQ-013`   | 主 Spec §4                                                                      | packaging extension/plugin 统一到 3.4.1                                      | effective POM/日志无 3.2.1                                             | Step 6                                     |
| `REQ-014`   | 主 Spec §4/§16                                                                  | bump 只更新 Reactor、source `egon-cola.version` 与公开文档                         | fixture 中 source sentinel version不变且 `.generated` 未被编辑              | Step 9                                     |
| `REQ-015`   | 主 Spec §4/§11/§16                                                              | 已存在 Flyway 文件路径与 bytes 不可变                                                | 12 个当前 migration 路径/SHA 保留；source copy逐个同 hash                      | Steps 2-8                                  |
| `REQ-016`   | 主 Spec §4/§7/§9/§16                                                            | 任一 Gate 失败禁止发布；Central UNKNOWN 不盲重放                                       | failure tests非零且 publish step不可达；runbook要求查 deployment              | Steps 1、5-9                                |
| `REQ-017`   | 主 Spec §4/§14                                                                  | 验证不得依赖业务服务或外部基础设施                                                         | 仅 Maven/JUnit/Groovy/shell/static；无 run/browser/Docker/live DB/MQ   | Every Step                                 |
| `REQ-018`   | predecessor Specs §4/§14/§16                                                   | Open family继续可发现/发布；master_data保持当前单节点tenant边界                            | docs/CI/catalog/GAV保持，generated routing tests继续通过                   | Steps 6-9保持既有发布与routing合同                  |
| `REQ-019`   | predecessor Specs §4/§11/§14                                                   | tenant-scoped唯一性保持；generated tests不连接live Nacos/Redis/PostgreSQL/RabbitMQ | current SQL/DAO/test profile tests在normal source和consumer均通过        | Steps 2-9作为unchanged regression            |
| `REQ-020`   | predecessor Specs §4/§11/§16                                                   | 已实施的新Flyway migrations和全部历史文件保持；不启动项目/执行SQL                               | exact path/hash，全部验证只build/test                                     | Steps 2-9 immutable/runtime boundary       |
| `REQ-021`   | predecessor Specs §4/§11/§14                                                   | Open manual SQL和Long/Snowflake ID合同保持                                     | no UUID、manual SQL顺序、Long/BIGINT/RPC/HTTP round-trip tests通过        | Steps 2-8 source/generated parity          |
| `REQ-022`   | MyBatis predecessor §4/§11                                                     | 未知历史数据迁移仍fail closed，不猜测UUID/tenant映射                                     | current migration precondition fixtures原样通过                         | Steps 2-8 preserve tests/scripts           |
| `REQ-023`   | MyBatis predecessor §4/§6/§10                                                  | common-core复用与domain-specific模型选择不变                                       | dependency/source/reuse scans与current tests通过                       | Steps 2-8 no Java/dependency change        |
| `REQ-024`   | MyBatis predecessor §4/§8/§14                                                  | 六metadata/verifier/README/living docs与实际生成树同步                             | six IT/generated verify/docs ownership audit                        | Steps 5-9                                  |
| `REQ-025`   | MyBatis predecessor §4/§7/§14                                                  | 业务路由、状态、缓存、事务和错误包装保持                                                      | current focused/contract tests在source和consumer都通过                   | Steps 2-8 round-trip regression            |
| `REQ-026`   | MyBatis predecessor §4/§14-§16                                                 | 不启动项目/外部基础设施                                                              | command audit只有Maven/JUnit/Groovy/shell/static                      | Every Step                                 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先提交生成器的隔离 fixture 合同，使 path containment、manifest parser、lock、trap、staging/atomic swap、deterministic hash
在没有真实 source 时也可 RED/GREEN。随后使用当前六个已通过 IT 的 Archetype 以固定 sentinel GAV/package生成正常项目，
按 Light、Service、Web 三组提交，禁止手工反向替换模板；第三组同时建立非发布 source reactor。所有 source 可构建后再提交
六 manifest并生成完整 ignored workspace。

package cutover同样按三组推进：每组先让既有 shell package-wiring Gate 对 template-first 状态失败，再把 POM资源指向
`.generated`、添加说明性 javadoc classifier、增强 consumer verifier并删除旧业务模板。legacy 三族只留下原位置旧 Flyway
archive，实际打包通过 generated source copy读取。最后才改版本脚本与三条 workflow，使 root deploy 的唯一入口具有完整前置
Gate。这个顺序保证每个 commit 可验证、source/public GAV不混淆、Central 从不看到内部 source artifact。

### 4.2 Test-first strategy

| Behavior/change unit      | RED point                                                                    | Minimum GREEN                                                          | Refactor/wiring permitted                          |
|---------------------------|------------------------------------------------------------------------------|------------------------------------------------------------------------|----------------------------------------------------|
| Generator safety/control  | `scripts/test-generate-archetypes.sh unit` 在脚本缺失或未拒绝逃逸/重复/并发时失败              | POSIX shell 直接 pipeline、mktemp、mkdir lock、trap、exact swap              | 仅抽取脚本函数；不建 Java plugin/framework                   |
| Normal source relocation  | N/A：是从当前成功 consumer 的机械 materialization，无新业务行为                               | 当前 Archetype生成 sentinel项目后原样入库并直接 verify                               | 只允许 normal POM sentinel/模块名机械修正                    |
| Real manifests/generation | Step 1 后真实 `generate` 因零 manifest稳定非零                                        | 六 manifest + 六 source产生完整 `.generated`                                 | 只做 Spec允许的 rootArtifactId/gitignore/排除目录 normalize |
| Package ownership cutover | `scripts/test-generate-archetypes.sh package <family>` 对旧 POM/旧业务模板失败        | external generated resource + curated META-INF + fail-closed preflight | 可在 parent pluginManagement去重版本/附件配置                |
| Version/release flow      | 新 `scripts/test-bump-cola-version.sh` 先证明旧脚本仍扫描 template POM；workflow静态审计先失败 | source discovery、generate/check/IT/shape/deploy顺序                      | 不增加发布目标或第二 bundle                                  |

### 4.3 Sequential and parallel boundaries

| Step   | Depends on | May run in parallel with | Must not overlap with                         | Reason                             |
|--------|------------|--------------------------|-----------------------------------------------|------------------------------------|
| Step 1 | None       | None                     | `scripts/generate_archetypes.sh`、`.gitignore` | 先锁生成 CLI与测试 seam                   |
| Step 2 | Step 1     | None                     | Light/Light Open source dirs                  | source baseline来自当前 artifact，需隔离提交 |
| Step 3 | Step 2     | None                     | Service/Service Open source dirs              | 保持 source family commit可审查         |
| Step 4 | Step 3     | None                     | Web/Web Open source dirs、source reactor POM   | aggregator只有六目录全部存在后才可绿色           |
| Step 5 | Step 4     | None                     | six manifests、`.generated`                    | 第一次建立完整原子集合                        |
| Step 6 | Step 5     | None                     | archetypes parent、Light package pair          | parent治理与首组 cutover必须一起验证          |
| Step 7 | Step 6     | None                     | Service package pair                          | 使用 Step 6 parent合同                 |
| Step 8 | Step 7     | None                     | Web package pair                              | 完成六族单事实源前不可进入 release wiring       |
| Step 9 | Step 8     | None                     | version/deploy scripts、docs、workflows         | 只对最终六族结构接入发布 Gate                  |

### 4.4 Commit boundaries

每个 Step 产生一个 semantic、path-limited commit。normal source family虽然包含大量文件，但每组是由一个当前公共 Archetype
生成的不可拆分 Maven project集合；拆成 Java/POM/config commits会造成不可构建中间态。`.generated` 永不提交。执行时只
stage该 Step列出的路径，绝不包含当前并发 `tsconfig.app.tsbuildinfo` 或 Plan/Spec之外的 dirty path。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element                        | Spec necessity verdict/section | Current repository evidence                                    | Direct/reuse alternative                     | Interaction/implementation cost  | Plan decision       |
|-------------------------------------|--------------------------------|----------------------------------------------------------------|----------------------------------------------|----------------------------------|---------------------|
| six normal source projects          | Add / 主 Spec §7.0              | 六个 template tree 不能作为 Maven source root                        | IDE mark-source不能解析模板 POM/Velocity           | 六 tracked trees替代旧编辑所有权          | Implement Steps 2-4 |
| source reactor                      | Add / §7.0                     | same-version components/platforms/facades需 fresh local reactor | 六次手工 install易漏依赖顺序                           | 一个 internal POM，无 Central module | Implement Step 4    |
| manifest-driven generator           | Add / §7.0/§13                 | 当前无 generate precondition                                      | 六个硬编码命令会漂移；自研 plugin过重                       | 一个 shell CLI、六 properties        | Implement Steps 1/5 |
| atomic `.generated`                 | Add / §7.0                     | package当前直接读 tracked template                                  | 逐目录覆盖可留下半套                                   | ignored state、lock、staging、swap  | Implement Steps 1/5 |
| existing package modules            | Keep/Modify / §7.0             | 六 GAV/metadata/IT/root release已存在                              | 直接 deploy plugin生成 POM会丢 Central metadata/IT | 六轻量POM+curated contract          | Implement Steps 6-8 |
| javadoc README JAR                  | Add / §5 DEC-006               | javadoc plugin对 maven-archetype跳过                              | 伪造 Java javadoc不真实                           | 每 GAV一个小说明 JAR                   | Implement Steps 6-8 |
| fetch-then-forward API              | Not present                    | 本范围无 HTTP/RPC新增                                                | N/A                                          | 无新增网络交互/状态                       | No action           |
| Java Strategy/Factory/plugin module | Remove / §7.0/§13              | Plugin 3.4.1和POSIX能力足够                                         | manifest + direct pipeline                   | 避免新模块/API/依赖                     | Do not implement    |
| source Central artifact             | Remove / §7.0                  | root Reactor目前只发布现有模块                                          | internal build即可                             | 避免新不可变GAV                        | Do not implement    |

审计未发现 caller fetch-then-forward、可由可信上下文派生却要求外传的值、重复模型/mapper、cache/job/page 或未获批准依赖。

### 4.6 Change-unit Dependency Matrix

| Change unit                | Requirements                                        | Proof/RED point                         | Compile/runtime prerequisites   | Produces                                | Consumers/unblocks          | Owning Step |
|----------------------------|-----------------------------------------------------|-----------------------------------------|---------------------------------|-----------------------------------------|-----------------------------|-------------|
| generator safety core      | `REQ-003`-`006`,`016`                               | shell fixture RED/GREEN                 | Bash/POSIX、root wrapper         | safe `generate/check` CLI               | manifests/package/workflows | Step 1      |
| Light normal sources       | `REQ-001`,`002`,`009`,`015`                         | current artifact -> direct verify       | parent/components local install | two normal projects                     | generation manifest         | Step 2      |
| Service normal sources     | `REQ-001`,`002`,`009`,`015`,`017`,`019`-`026`       | current artifact -> direct verify       | facades/components              | two 6/7-module projects                 | generation manifest         | Step 3      |
| Web normal sources/reactor | `REQ-001`,`002`,`009`,`010`,`015`,`017`,`019`-`026` | source reactor clean install            | all prior source dirs           | two 6/7-module projects + aggregator    | complete generation         | Step 4      |
| manifests/generated set    | `REQ-003`-`009`,`013`,`016`                         | zero-manifest RED; generate/check GREEN | Steps 1-4                       | ignored six-product set                 | package cutover             | Step 5      |
| Light package pair         | `REQ-002`,`005`,`007`-`009`,`012`,`013`,`015`       | package light RED/GREEN                 | generated set                   | two source-first public artifacts       | next pair                   | Step 6      |
| Service package pair       | `REQ-002`,`005`,`007`-`009`,`012`,`015`-`026`       | package service RED/GREEN               | Step 6 parent                   | two source-first public artifacts       | next pair                   | Step 7      |
| Web package pair           | `REQ-002`,`005`,`007`-`009`,`012`,`015`-`026`       | package web RED/GREEN                   | Step 7                          | final two source-first public artifacts | release wiring              | Step 8      |
| version/CI/release         | `REQ-004`,`010`-`012`,`014`,`016`,`017`             | bump/workflow RED/GREEN                 | all six cut over                | enforced source->Central chain          | protected release           | Step 9      |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

本 Plan 创建 normal Java trees，但不新增或改变业务 Java类型/逻辑；所有 Java内容从当前六个有效 Archetype生成并以
round-trip守护。结构选择是六个已存在的精确 Egon Archetype profile，不是传统 `biz.*` 三层，也不产生第三种 hybrid。

| Concern                            | Current repository evidence                                                         | Effective Spec decision                                        | Planned implementation consequence                   | Owning Steps/checks                         |
|------------------------------------|-------------------------------------------------------------------------------------|----------------------------------------------------------------|------------------------------------------------------|---------------------------------------------|
| Architecture profile               | six `archetype-resources` + metadata + ArchUnit/verify.groovy                       | 主 Spec §6.1/§10：逐族 exact Light/Service/Web/Open                | normal source与consumer保持1/6/7模块、package direction和测试 | Steps 2-8；`MC-ARCH-001`                     |
| Reuse/capability                   | parent Plugin/Jar/Source/GPG/Enforcer、Maven wrapper、Groovy verifier、现有 shell trap惯例 | 主 Spec §6.1/§7.0                                               | 复用固定插件和JDK/POSIX，不加 runtime/library                  | Steps 1、5-9；`MC-REUSE-001`,`MC-DEP-001`     |
| Naming/model/validation/conversion | current six Java inventories与 predecessor Specs                                     | 主 Spec §6.2/§10全部保持                                            | 文件迁移不改类型名、Validation、Lombok、MapStruct/BaseConverter  | Steps 2-4；N/A rows由inventory diff证明         |
| Bean/logging/util/JSON/time/config | current generated source、`lombok.config`、application profiles                       | 主 Spec §6.2/§15                                                | Bean/日志/JSON/time bytes保持；所有 profile成组迁移和key parity  | Steps 2-8；`MC-UTIL-001`,`MC-CONFIG-001`     |
| Business variation/pattern         | 无业务逻辑变更；生成流程有六产品变化与原子性                                                              | 主 Spec §13选择 Manifest-driven Pipeline + Staging/Atomic Publish | shell数据驱动，不引入Java Strategy/Factory；业务Rule 9不触发       | Steps 1/5；`MC-PATTERN-001` N/A for business |

#### Capability reuse ledger

| Need                       | Candidates inspected                                           | Exact evidence                                                     | Fit/gap                             | Decision                               | Added dependency/custom code | Owning Step/check        |
|----------------------------|----------------------------------------------------------------|--------------------------------------------------------------------|-------------------------------------|----------------------------------------|------------------------------|--------------------------|
| source -> archetype        | Apache Plugin / custom Maven plugin                            | `egon-cola-archetypes/pom.xml`已管理 3.4.1；本地 goal help确认参数           | Apache goal满足转换；缺少Egon overlay/原子集合 | reuse Plugin + shell orchestration     | 无依赖；一个 repository script     | Steps 1/5；`MC-REUSE-001` |
| source same-version build  | root reactor / six manual builds                               | components/platforms/facades当前module证据                             | internal aggregator能保持依赖顺序          | reuse modules，source POM不入root         | 无新artifact依赖                 | Step 4                   |
| lock/staging/hash          | JDK/POSIX / third-party CLI                                    | `bump_cola_version.sh`的mktemp/trap/backup；系统`shasum`/`sha256sum`差异 | POSIX工具足够，需跨macOS/Linux选择           | direct functions with capability probe | 无                            | Step 1；`MC-UTIL-001`     |
| descriptor/consumer checks | create-from-project metadata / current curated metadata+Groovy | six `META-INF`与`projects/basic`                                    | 自动metadata不能表达当前合同                  | keep curated + extend verifier         | 无                            | Steps 5-8                |
| Central attachments        | existing Source/Javadoc/GPG/Jar plugins                        | parent pluginManagement；当前 javadoc跳过证据                             | Jar plugin可在GPG前附说明 classifier      | reuse maven-jar-plugin                 | 无                            | Steps 6-9；`MC-DEP-001`   |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source          | Repository evidence                                      | Exact files and order                                  | Pseudocode obligations                                      | Validation gate                                            | Steps       | Status/blocker                            |
|--------------|----------------------|----------------------------------------------------------|--------------------------------------------------------|-------------------------------------------------------------|------------------------------------------------------------|-------------|-------------------------------------------|
| Rule 1       | 主 Spec §6.2/§10      | 不新增/改名业务类型；只materialize current inventories              | six `source-projects/egon-cola-source-*/**/*.java`原样生成 | 不改PO/BO/VO/DTO/Query/Command/Event/behavior类名               | source/generated Java path+content inventory               | Steps 2-4   | N/A — no naming change                    |
| Rule 2       | 主 Spec §6.2/§10      | current Validation/groups/ValidationUtils由 predecessor有效 | same six Java trees before任何POM wiring                 | 保持所有handoff annotation/group/normalization/error bytes      | normal source tests + round-trip + existing contract tests | Steps 2-8   | N/A — no handoff change                   |
| Rule 3       | 主 Spec §6.2/§10      | current record/Lombok/MapStruct/BaseConverter inventory  | same six Java trees                                    | 禁止机械迁移时重写class/constructor/converter                        | compile + inventory normalized diff                        | Steps 2-8   | N/A — no model/converter change           |
| Rule 4       | 主 Spec §6.2/§10      | current beans、`lombok.config`、Qualifier                  | source trees先于generation/package                       | 保持`@Slf4j`、Bean name、constructor、Qualifier与config           | context/compile + byte/semantic round-trip                 | Steps 2-8   | N/A — no business Bean change             |
| Rule 5       | 主 Spec §6.1/§6.2/§13 | generator只用JDK/POSIX和existing Maven                      | scripts before manifests                               | 不引入allowlist外utility/dependency                             | POM dependency diff + shell command audit                  | Steps 1/5/9 | PASS                                      |
| Rule 6       | 主 Spec §6.2/§10      | external JSON DTO不变                                      | source/consumer Java tree                              | 保持Jackson annotations/defaults                              | existing serialization/consumer tests                      | Steps 2-8   | N/A — no JSON change                      |
| Rule 7       | 主 Spec §6.2/§8/§14   | 六族均有 application base/dev/test/prod 配置                   | family source trees -> generated -> consumer           | 所有profile同批迁移，key集合一致、值不改                                   | profile key parity + round-trip                            | Steps 2-8   | PASS                                      |
| Rule 9       | 主 Spec §6.2/§13      | 无复杂业务变化；build flow已选manifest pipeline/atomic publish     | scripts/manifests only                                 | 不对业务Java增加模式；不硬编码六产品控制流                                     | fixture新增/删除manifest + atomic failure tests                | Steps 1/5   | N/A for business; build pattern evidenced |
| Rule 10      | 主 Spec §6.2/§10      | 无日期字段/API改变                                              | source/consumer Java tree                              | 保持`java.time`，不引入`java.util.Date/Calendar/SimpleDateFormat` | forbidden import diff/search                               | Steps 2-8   | N/A — no time change                      |
| Rule 11      | 主 Spec §6.1/§8       | current exact Light/Service/Web/Open tree + verifier     | every source/package/workflow file in Steps 1-9        | 只允许逐族exact Archetype profile，source ownership不改变层次          | source/consumer architecture tests + metadata topology     | Every Step  | PASS                                      |

## 5. Change File Tree

```text
.gitignore                                                        MODIFY Step 1
scripts/
├── generate_archetypes.sh                                        CREATE Step 1
├── test-generate-archetypes.sh                                   CREATE Step 1
├── test-bump-cola-version.sh                                     CREATE Step 9
├── bump_cola_version.sh                                          MODIFY Step 9
├── maven-deploy.sh                                               MODIFY Step 9
└── maven-deploy.md                                               MODIFY Step 9
egon-cola-archetypes/
├── pom.xml                                                       MODIFY Step 6
├── source-projects/
│   ├── pom.xml                                                   CREATE Step 4
│   ├── egon-cola-source-light/**                                 CREATE Step 2
│   ├── egon-cola-source-light-open/**                            CREATE Step 2
│   ├── egon-cola-source-service/**                               CREATE Step 3
│   ├── egon-cola-source-service-open/**                          CREATE Step 3
│   ├── egon-cola-source-web/**                                   CREATE Step 4
│   └── egon-cola-source-web-open/**                              CREATE Step 4
├── .generated/egon-cola-archetype-*/**                           GENERATED Step 5, ignored
├── egon-cola-archetype-{light,light-open}/
│   ├── pom.xml                                                   MODIFY Step 6
│   ├── src/main/archetype/archetype.properties                  CREATE Step 5
│   ├── src/main/javadoc/README.md                               CREATE Step 6
│   ├── src/main/resources/archetype-resources/**                DELETE Step 6 except immutable legacy Flyway archive
│   └── src/test/resources/projects/basic/verify.groovy          MODIFY Step 6
├── egon-cola-archetype-{service,service-open}/...                same operations Steps 5/7
├── egon-cola-archetype-{web,web-open}/...                        same operations Steps 5/8
└── open-source-archetype-code-style.md                           MODIFY Step 9
.github/workflows/
├── ci.yaml                                                       MODIFY Step 9
├── ci_java_compatibility.yaml                                    MODIFY Step 9
└── publish-maven-central.yml                                     MODIFY Step 9
```

| Operation | Path                                                                                                                                                   | Current evidence/symbol                           | Final symbols/state                                                        | Responsibility                              | Step               | Requirements                                        | Validation owner       |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|----------------------------------------------------------------------------|---------------------------------------------|--------------------|-----------------------------------------------------|------------------------|
| MODIFY    | `.gitignore`                                                                                                                                           | no `.generated` rule                              | exact `egon-cola-archetypes/.generated/` ignore                            | generated ownership                         | 1                  | `REQ-002`,`005`                                     | shell hygiene gate     |
| CREATE    | `scripts/test-generate-archetypes.sh`                                                                                                                  | absent；nearby shell tests use isolated temp state | `unit/generation/package/release` contract modes                           | RED/failure/round-trip/static checks        | 1                  | `REQ-003`-`009`,`016`                               | script itself          |
| CREATE    | `scripts/generate_archetypes.sh`                                                                                                                       | absent；`bump_cola_version.sh` style               | `generate                                                                  | check`、manifest parser、lock/stage/hash/swap | internal build CLI | 1                                                   | `REQ-003`-`008`,`016`  | shell test |
| CREATE    | `egon-cola-archetypes/source-projects/egon-cola-source-{light,light-open}/**`                                                                          | current public templates                          | two concrete normal projects                                               | Light source truth                          | 2                  | `REQ-001`,`002`,`009`,`015`,`017`                   | direct verify          |
| CREATE    | `egon-cola-archetypes/source-projects/egon-cola-source-{service,service-open}/**`                                                                      | current 6/7-module templates                      | two normal multi-module projects                                           | Service source truth                        | 3                  | `REQ-001`,`002`,`009`,`015`,`017`,`019`-`026`       | direct verify          |
| CREATE    | `egon-cola-archetypes/source-projects/egon-cola-source-{web,web-open}/**`                                                                              | current 6/7-module templates                      | two normal multi-module projects                                           | Web source truth                            | 4                  | `REQ-001`,`002`,`009`,`010`,`015`,`017`,`019`-`026` | source reactor         |
| CREATE    | `egon-cola-archetypes/source-projects/pom.xml`                                                                                                         | absent                                            | components -> platforms -> facades -> six sources module order             | non-Central verification reactor            | 4                  | `REQ-001`,`010`                                     | clean install          |
| CREATE    | six `egon-cola-archetype-*/src/main/archetype/archetype.properties`                                                                                    | absent                                            | seven-field allowlisted manifest                                           | dynamic mapping/topology                    | 5                  | `REQ-003`,`004`,`007`                               | generation test        |
| GENERATED | `egon-cola-archetypes/.generated/egon-cola-archetype-*/**`                                                                                             | absent/ignored                                    | six atomic resources + SHA manifests                                       | read-only package input                     | 5                  | `REQ-005`,`006`,`008`                               | generate/check         |
| MODIFY    | `egon-cola-archetypes/pom.xml`                                                                                                                         | extension 3.2.1/plugin 3.4.1                      | one `maven.archetype.version=3.4.1` + shared plugin config                 | version/release governance                  | 6                  | `REQ-012`,`013`                                     | effective POM/shape    |
| MODIFY    | `egon-cola-archetypes/egon-cola-archetype-{light,light-open}/pom.xml`                                                                                  | no external resources/attachment/preflight        | consume own `.generated`, overlay META-INF, attach README javadoc          | Light package contract                      | 6                  | `REQ-005`,`007`,`012`,`013`                         | targeted IT/shape      |
| CREATE    | `egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/main/javadoc/README.md`                                                               | absent                                            | explanatory classifier content                                             | Central javadoc artifact                    | 6                  | `REQ-012`                                           | jar content scan       |
| MODIFY    | `egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/test/resources/projects/basic/verify.groovy`                                          | current consumer contract                         | add source sentinel/derived-boundary assertions                            | Light transformation proof                  | 6                  | `REQ-008`,`009`,`015`                               | Archetype IT           |
| DELETE    | `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/**` except `src/main/resources/db/migration/**`                 | 429 files incl. four immutable SQL                | only old migration paths remain tracked                                    | remove duplicate business truth             | 6                  | `REQ-002`,`015`                                     | inventory/hash gate    |
| DELETE    | `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/**`                                                        | 427 files, no Flyway archive exception            | directory removed                                                          | remove duplicate business truth             | 6                  | `REQ-002`                                           | inventory gate         |
| MODIFY    | `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/pom.xml`                                                                              | no generated resource/preflight                   | generated + curated resources, shared executions                           | Service package wiring                      | 7                  | `REQ-005`,`007`,`012`                               | targeted IT/shape      |
| CREATE    | `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/main/javadoc/README.md`                                                           | absent                                            | explanatory classifier content                                             | Service Central docs                        | 7                  | `REQ-012`                                           | jar content scan       |
| MODIFY    | `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/test/resources/projects/basic/verify.groovy`                                      | current consumer checks                           | sentinel/root/topology/SQL/Proto checks                                    | Service output proof                        | 7                  | `REQ-008`,`009`,`024`                               | Archetype IT           |
| DELETE    | `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/main/resources/archetype-resources/**` except legacy Service `**/db/migration/**` | 337/345 files                                     | only legacy four-file archive remains                                      | Service ownership cutover                   | 7                  | `REQ-002`,`015`,`020`                               | hash/inventory         |
| MODIFY    | `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/pom.xml`                                                                                      | no generated resource/preflight                   | generated + curated resources, shared executions                           | Web package wiring                          | 8                  | `REQ-005`,`007`,`012`                               | targeted IT/shape      |
| CREATE    | `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/main/javadoc/README.md`                                                                   | absent                                            | explanatory classifier content                                             | Web Central docs                            | 8                  | `REQ-012`                                           | jar content scan       |
| MODIFY    | `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/test/resources/projects/basic/verify.groovy`                                              | current consumer checks                           | sentinel/root/topology/OpenAPI/Gateway checks                              | Web output proof                            | 8                  | `REQ-008`,`009`,`024`                               | Archetype IT           |
| DELETE    | `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/main/resources/archetype-resources/**` except legacy Web `**/db/migration/**`             | 427/436 files                                     | only legacy four-file archive remains                                      | Web ownership cutover                       | 8                  | `REQ-002`,`015`,`020`                               | hash/inventory         |
| CREATE    | `scripts/test-bump-cola-version.sh`                                                                                                                    | absent                                            | isolated source-version/sentinel/rollback assertions                       | version RED/GREEN                           | 9                  | `REQ-014`,`016`                                     | shell test             |
| MODIFY    | `scripts/bump_cola_version.sh`                                                                                                                         | scans `archetype-resources/**/pom.xml`            | scans source POMs with `egon-cola.version`, excludes sentinel/`.generated` | version owner                               | 9                  | `REQ-014`                                           | fixture test           |
| MODIFY    | `scripts/maven-deploy.sh`                                                                                                                              | directly `clean verify/deploy`                    | run source install + generate/check + IT + shape first                     | local guarded release wrapper               | 9                  | `REQ-011`,`012`,`016`                               | shell/static/dry run   |
| MODIFY    | `scripts/maven-deploy.md`                                                                                                                              | template-first release commands                   | two-stage commands, classifier/signature/UNKNOWN runbook                   | operator contract                           | 9                  | `REQ-011`,`012`,`016`                               | doc command audit      |
| MODIFY    | `egon-cola-archetypes/open-source-archetype-code-style.md`                                                                                             | tells contributors to edit templates              | points all six contributors to normal source only                          | ownership documentation                     | 9                  | `REQ-002`                                           | forbidden wording scan |
| MODIFY    | `.github/workflows/{ci.yaml,ci_java_compatibility.yaml}`                                                                                               | builds/templates and hardcoded six array          | source/generate/check/manifest discovery/IT/consumer/shape                 | CI gates                                    | 9                  | `REQ-004`,`011`,`012`,`016`,`017`                   | YAML/static + CI       |
| MODIFY    | `.github/workflows/publish-maven-central.yml`                                                                                                          | root verify/deploy without source generate        | full preflight then single root deploy                                     | protected Central Gate                      | 9                  | `REQ-010`-`012`,`016`                               | workflow audit         |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Applicable repository instructions：本仓库 AGENTS.md；每 Step独立commit；不自动启动项目；不修改existing Flyway；不触碰无关路径。
- Branch/commit：`main@28b9e596d`。实施前必须重新读取 branch/HEAD/status；若相关路径漂移，先停止并修订 Plan。
- 当前无关 dirty path：`egon-cola-platforms/egon-cola-platform-admin-web-shared/tsconfig.app.tsbuildinfo`
  ；不得stage/restore。
- 主 Spec与本 Plan在规划时均未提交；执行必须由用户批准后按各 Step path-limited stage/commit。
- `.generated`、所有 Maven `target`、临时 canonical consumer都不可提交；临时目录使用 `mktemp -d` 并由 trap清理明确路径。

### 6.2 Build, test, and environment prerequisites

| Concern               | Exact command/source                                                                     | Required state                                                | Validation boundary                   |
|-----------------------|------------------------------------------------------------------------------------------|---------------------------------------------------------------|---------------------------------------|
| JDK/Maven             | `java -version`; `./mvnw -version`                                                       | JDK 21+、repo wrapper executable                               | toolchain only                        |
| parent bootstrap      | `./mvnw -B -ntp -N install`; `./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml install` | current parent POM in local repo                              | local Maven repo；no deploy            |
| normal source reactor | `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install`           | six sources + current components/platforms/facades            | Maven/JUnit only                      |
| generator             | `./scripts/generate_archetypes.sh generate`; `./scripts/generate_archetypes.sh check`    | no active lock；repo-contained paths                           | filesystem/Maven only                 |
| package reactor       | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test`                  | complete `.generated`                                         | Archetype IT/generated JUnit          |
| release shape         | `./mvnw -B -ntp -Prelease -Dgpg.skip=true clean verify`                                  | prior Gates green                                             | no Central write, no signature proof  |
| real signing/publish  | protected `publish-maven-central.yml`                                                    | non-SNAPSHOT main/master、explicit confirm、Central/GPG secrets | external user-controlled release only |

### 6.3 Immutable constraints and approved decisions

- 六个 public GAV、root module list、consumer parameters和Light 1 / legacy 6 / open 7 module topology不变。
- 12个当前 legacy Flyway文件不得modify/rename/delete；source copy必须同 bytes；future migration只在normal source创建新版本。
- source project GAV不公开、不加入root module/BOM/distributionManagement/Central bundle。
- `create-from-project`自动metadata不得进入最终JAR；curated `META-INF`、post script和IT由package module继续拥有。
- `.generated`作为一个六产品集合原子替换；禁止逐产品commit/局部fallback/并发抢锁。
- release-shape附件先于verify阶段GPG；真实Central UNKNOWN先查deployment，不自动重放同版本。
- 不新增runtime dependency、业务Java/API/schema/config语义；不启动服务、浏览器、Docker或live基础设施。

### 6.4 Plan Clarifications

| ID              | Small implementation inference                                                                                                               | Repository evidence                                              | Why semantics are unchanged                              | Impact if wrong                             |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|----------------------------------------------------------|---------------------------------------------|
| `PLAN-CLAR-001` | six source统一内部groupId=`top.egon.internal.archetype.source`、version=`0.1.0-SNAPSHOT`，artifact/package按主 Spec §8.2表                            | 当前public generation允许任意consumer GAV；source GAV无public contract   | 只作为create-from-project唯一sentinel，public consumer仍替换为调用者值 | collision/leakage Gate失败，改sentinel并重建source |
| `PLAN-CLAR-002` | manifest `expectedTopology`使用逗号分隔的module suffix集合；single为`root`，6模块为`common,domain,application,infrastructure,adapter,starter`，7模块增加`facade` | current six descriptors精确提供这些module集合                            | 只是§9已批准字段的本地可解析格式，不增加产品或模块                               | topology mismatch会在Step 5阻断，回到manifest/Spec |
| `PLAN-CLAR-003` | shell test把完整脚本复制到`mktemp` fake repo，通过fake `mvnw`注入第N产品失败，不增加production test-only env/argv                                                  | current scripts从自身目录解析repo root；Spec禁止扩展CLI字段                    | 测试隔离实现细节，不改变`generate                                    | check` public contract                      | 无法注入则改fixture复制方式，不加hidden production flag |
| `PLAN-CLAR-004` | `scripts/maven-deploy.sh`也加入与workflow相同preflight                                                                                             | 当前脚本是本地release入口，主 Spec Scope包含release scripts且要求任一入口fail closed | 只落实既定generate-before-release语义，不新增target/GAV             | 若遗漏，本地入口可绕过Gate，Step 9静态测试失败                |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 建立 manifest 驱动生成器的安全与原子性合同

- Requirements: `REQ-002`,`REQ-003`,`REQ-004`,`REQ-005`,`REQ-006`,`REQ-007`,`REQ-008`,`REQ-016`,`REQ-017`,`REQ-026`
- Dependencies: `None`
- Baseline state: 仓库只有template-first resources；没有generator、manifest、`.generated`规则。
- Observable outcome: isolated shell fixture证明usage/field/path/discovery/lock/failure/signal/hash/atomic
  swap；真实repo因零manifest明确fail closed。
- End state: `generate|check` CLI可用，未来generation/package/release test modes已定义；没有真实`.generated`或source变更。
- Test-first gate: `Required` — 先创建test script；由于production script缺失，unit mode以“generator not
  found”预期RED；实现后fixture GREEN，真实generate仍以“no manifests”预期非零。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-UTIL-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `CREATE scripts/test-generate-archetypes.sh`

- Purpose: 在temp fake repo中先定义CLI、parser、path containment、atomic/deterministic/lock和后续real-family Gate。
- Symbols: `test_usage`, `test_manifest_inventory`, `test_path_escape`, `test_atomic_failure`, `test_lock`,
  `test_determinism`, `test_generation`, `test_package_family`, `test_release_wiring`。
- Repository evidence: `scripts/unified-platform/test-direct-run-contract.sh`等使用Bash断言；主 Spec `TEST-005`-`013`,
  `021`要求shell fixture。
- Dependencies and consumers: 被本地/CI调用；复制待测script到`mktemp` fake repo；fake `mvnw`只生成受控resource tree。
- Why now: 先固定缺失行为和错误语义，避免生成器实现决定测试。
- Contract/signature changes: `scripts/test-generate-archetypes.sh unit|generation|package <light|service|web>|release`
  ；production CLI仍只有`generate|check`。
- Input/output and state mapping: fake manifests/source -> fake Maven output -> staging/current set；assert
  stdout/exit/hash和old set；real modes只读/生成ignored state。
- Error and edge behavior: unknown test mode=2；零/重复/未知字段/绝对/`..`/missing source、fourth
  failure、signal、concurrent lock全部assert non-zero且old set不变。
- Standards impact: `MC-UTIL-001`仅JDK/POSIX；`MC-PATTERN-001`验证manifest pipeline/atomic publish；不触碰Java。
- Literal rule enforcement: `Rule 5`无allowlist外工具；`Rule 9`以数据manifest消除产品switch；`Rule 11`fixture不改变业务profile。
- Implementation pseudocode:

```bash
main(mode, family?) {
  fixture_root="$(mktemp -d ...)"; trap cleanup exact fixture_root
  copy generator under test into fixture_root/scripts
  create fake package manifests and concrete source POM/package trees
  fake mvnw: for each source emit src/main/resources/archetype-resources; fail on configured ordinal
  case unit:
    assert invalid modes/fields/paths fail before writes
    assert fourth failure preserves seeded old complete set
    assert two invocations produce same sorted hashes
    assert concurrent second process fails without deleting owner lock
  case generation:
    run real generate/check; assert six targets, no tracked generated files, no sentinel leakage
  case package:
    assert selected family POM references exact generated dir, curated META-INF, preflight, javadoc
    assert old business template inventory is zero except exact immutable Flyway allowlist
  case release:
    assert source/generate/check/IT/shape precede deploy in scripts/workflows
}
```

- Verification contribution: RED/GREEN for `TEST-005`-`010`,`011`,`013`,`021`和Step 6-9静态合同。
- After this file: test contract存在并因production generator缺失按预期RED。

#### File 2 — `CREATE scripts/generate_archetypes.sh`

- Purpose: 实现主 Spec `CLI-001 generate|check`的最小安全pipeline。
- Symbols: `usage`, `die`, `discover_manifests`, `parse_manifest`, `validate_repo_path`, `acquire_lock`, `generate_one`,
  `normalize_one`, `hash_tree`, `publish_atomic`, `run_generate`, `run_check`, `cleanup_on_exit`。
- Repository evidence: `bump_cola_version.sh`使用`set -Eeuo pipefail`、readonly root、mktemp/trap/rollback；本地Plugin
  help确认3.4.1的`outputDirectory/packageName/propertyFile`。
- Dependencies and consumers: root `mvnw`、six manifests/source、package/workflows；只写repo exact `.generated`和temp。
- Why now: shell tests已固定行为；不等待业务source即可验证安全core。
- Contract/signature changes: argv只接受`generate|check`；manifest字段allowlist严格为主 Spec七字段。
- Input/output and state mapping: sorted manifests ->每source `create-from-project` temp output -> copy only
  `src/main/resources/archetype-resources` -> normalize `.gitignore`为`__gitignore__`
  、sentinel/rootArtifactId/topology -> stable SHA file -> all-set swap。
- Error and edge behavior: parser绝不`source/eval`；absolute/escape/broad target拒绝；lock-held
  fail-fast；Maven/normalize/hash/signal清temp/lock且旧set不动；check不修改current set。
- Standards impact: `MC-REUSE-001`复用Plugin；`MC-DEP-001`无dependency；`MC-UTIL-001`能力探测`shasum -a 256`或`sha256sum`
  ；bounded logs不打印文件内容/secret。
- Literal rule enforcement: `Rule 5`闭合JDK/POSIX工具；`Rule 9`实现主 Spec选择的Manifest-driven Pipeline与Staging+Atomic
  Publish；`Rule 11`只转换不重构Java profile。
- Implementation pseudocode:

```bash
main(mode) {
  require exact repo/mvnw and mode
  manifests = find package modules -path '*/src/main/archetype/archetype.properties' | sort
  require manifests nonempty and count == packaging=maven-archetype module count
  for manifest in manifests:
    parse key=value without eval; require exactly seven allowed keys
    canonicalize sourceProject and target; reject outside approved roots/duplicates
  acquire mkdir lock or fail
  stage = mktemp alongside generated parent
  snapshot git HEAD + tracked/worktree source hashes
  for manifest:
    write temp plugin properties with derived rootArtifactId only for multi topology
    mvnw -f source/pom.xml archetype:3.4.1:create-from-project
      -Dinteractive=false -DpackageName=sourcePackage -DoutputDirectory=temp/product
    copy only generated archetype-resources
    copy source .gitignore as __gitignore__; exclude .git/.idea/target/source-only files
    normalize concrete source GAV/package/root module names to Velocity tokens
    require exact topology and no sentinel leakage; write sorted generation-manifest.sha256
  require source snapshot unchanged
  if generate: rename current to exact backup, rename stage to .generated, delete exact backup after success
  if check: generate second temp set, compare both and current, never write tracked state
}
```

- Verification contribution: `scripts/test-generate-archetypes.sh unit`覆盖`TEST-005`-`010`；后续real modes覆盖
  `TEST-007`,`009`,`013`。
- After this file: fixture GREEN；real repo执行会在零manifest处清晰失败，不会创建`.generated`。

#### File 3 — `MODIFY .gitignore`

- Purpose: 声明唯一派生工作区不进入Git。
- Symbols: exact rule `egon-cola-archetypes/.generated/`。
- Repository evidence: 当前仅忽略通用`target`，无`.generated`。
- Dependencies and consumers: Git、generator、CI hygiene；不影响任何target之外路径。
- Why now: generator落真实输出前先建立ownership boundary。
- Contract/signature changes: new ignored path only。
- Input/output and state mapping: generated files -> Git ignored；tracked source/package仍可见。
- Error and edge behavior: 不使用宽泛`.generated/`或`egon-cola-archetypes/**`；`git check-ignore`必须只命中exact path。
- Standards impact: `MC-SCOPE-001`防止掩盖其他派生目录；Java N/A。
- Literal rule enforcement: `Rule 11`不改变任何业务结构，只隔离build state。
- Implementation pseudocode:

```gitignore
# Append one repository-root-relative rule after the existing build-state block.
# Do not use a generic .generated rule because other modules may own that name.
egon-cola-archetypes/.generated/
# Verify: git check-ignore matches a probe below this path, while git ls-files returns zero.
```

- Verification contribution: `TEST-021`和`REQ-005`。
- After this file: future `.generated`不会出现在tracked/staged inventory。

- Validation working directory: repository root `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `bash -n scripts/generate_archetypes.sh scripts/test-generate-archetypes.sh && ./scripts/test-generate-archetypes.sh unit && test "$(./scripts/generate_archetypes.sh generate >/tmp/egon-generate-red.log 2>&1; printf '%s' "$?")" -ne 0 && grep -Fq 'no archetype manifests' /tmp/egon-generate-red.log && git check-ignore -q egon-cola-archetypes/.generated/probe && test -z "$(git ls-files egon-cola-archetypes/.generated)"`
- Expected result: shell syntax/unit tests exit 0；真实generate只有预期零manifest非零；ignore命中且tracked count=0。
- Failure returns to: File 1若断言/fixture错误；File 2若错误语义/安全行为错误；File 3若ignore scope错误。
- Completion criteria: 主 Spec `TEST-005`,`006`,`008`-`010`在fixture绿色，真实repo无任何generated写入。
- Rollback: path-limited revert三文件；不存在外部或数据库状态。
- Commit paths: `.gitignore`, `scripts/generate_archetypes.sh`, `scripts/test-generate-archetypes.sh`
- Commit: `build(archetypes): add atomic source generation pipeline`

### Step 2 — 生成并固化 Light 与 Light Open 正常源码工程

- Requirements: `REQ-001`,`REQ-002`,`REQ-009`,`REQ-015`,`REQ-017`,`REQ-019`,`REQ-020`,`REQ-021`,`REQ-022`,`REQ-023`,
  `REQ-025`,`REQ-026`
- Dependencies: `Step 1`
- Baseline state: 两套Light业务内容仍只在public template；当前artifact/IT是canonical输入。
- Observable outcome: 两个concrete normal Maven project可独立导入和`clean verify`，无Velocity占位符。
- End state: Light pair normal source成为即将切换的source truth；package仍template-first，避免本Step混合发布改造。
- Test-first gate: `Not applicable` — 结构materialization不改变业务行为；canonical输入必须先由当前artifact成功生成，随后以现有tests直接验证。
- Manual Checks:
  `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 7, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-{light,light-open}/**`

- Purpose: 用当前两个public Archetype生成可开发单模块normal projects，而非手工反模板化。
- Symbols: concrete GAV `top.egon.internal.archetype.source:egon-cola-source-{light,light-open}:0.1.0-SNAPSHOT`；packages
  `top.egon.cola.archetype.source.light{,open}`；现有全部Java/tests/config/docs。
- Repository evidence: current template counts 429/427；fixture GAV inputs和single-module metadata；主 Spec
  §8.2给出目录/package。
- Dependencies and consumers: current installed Light artifacts生成；normal Maven消费current components/BOM；Step 5
  manifest读取。
- Why now: 先用最小single-module pair校验source-first开发体验，且不触碰package contract。
- Contract/signature changes: internal source GAV only；所有public type/API/config/schema签名保持current consumer结果。
- Input/output and state mapping: current archetype + sentinel inputs -> temp canonical project -> exact source
  directory；移除temp target，保留wrapper executable；old Flyway四文件复制同hash。
- Error and edge behavior: generation/verify任一失败不复制；禁止对Java/SQL做“顺手修复”；检测`${package}`/`${groupId}`/
  `${rootArtifactId}`/`__rootArtifactId__`为零；profile key集合一致。
- Standards impact: Java类型、Validation、Lombok/Converter、Bean/Jackson/time均原样；`MC-CONFIG-001`要求application
  base/dev/test/prod同批进入source。
- Literal rule enforcement: `Rule 1/2/3/4/6/10`由current consumer inventory原样保持；`Rule 7`全profile迁移；`Rule 11`
  single-module Light/Light Open profile不变。
- Implementation pseudocode:

```bash
install current root/archetypes parents and current Light pair artifacts
for product in light light-open:
  temp = mktemp -d
  mvnw archetype:generate
    -DarchetypeGroupId=top.egon -DarchetypeArtifactId=egon-cola-archetype-${product}
    -DarchetypeVersion=${currentRootVersion}
    -DgroupId=top.egon.internal.archetype.source
    -DartifactId=egon-cola-source-${product} -Dversion=0.1.0-SNAPSHOT
    -Dpackage=top.egon.cola.archetype.source.${lightOrLightopen} -Dgitignore=.gitignore
    -DinteractiveMode=false
  assert generated project root name == egon-cola-source-product
  remove only generated target/build state
  compare generated topology/type/config/SQL inventory to current basic consumer contract
  copy project into source-projects exact destination preserving mvnw executable bit
  assert no Velocity/path placeholders
  for legacy light migrations: assert sha256(old archive) == sha256(source copy)
```

- Verification contribution: `TEST-001`,`004`,`019`和后续round-trip source side。
- After this file: 两个normal projects完整可构建；发布模块尚未消费它们。

- Validation working directory: repository root
- Verification command:
  `./mvnw -B -ntp -N install && ./mvnw -B -ntp -f egon-cola-components/pom.xml clean install && ./mvnw -B -ntp -f egon-cola-platforms/pom.xml clean install && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml clean verify && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml clean verify && ! rg -n '\$\{(package|groupId|rootArtifactId)\}|__rootArtifactId__' egon-cola-archetypes/source-projects/egon-cola-source-{light,light-open} && git diff --check -- egon-cola-archetypes/source-projects/egon-cola-source-{light,light-open}`
- Expected result: two builds exit 0；placeholder scan零命中；legacy Light四SQL path/hash parity。
- Failure returns to: canonical generation command/当前artifact若生成失败；本File机械materialization若compile/diff/hash失败；业务差异回主
  Spec而非现场修复。
- Completion criteria: 两个普通工程可直接IDE/Maven开发，source-only GAV不出现在root modules。
- Rollback: 删除本Step两个新目录；current package仍可独立构建。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-{light,light-open}/**`（即两个新source project目录）
- Commit: `refactor(archetypes): materialize light source projects`

### Step 3 — 生成并固化 Service 与 Service Open 正常源码工程

- Requirements: `REQ-001`,`REQ-002`,`REQ-009`,`REQ-015`,`REQ-017`,`REQ-019`,`REQ-020`,`REQ-021`,`REQ-022`,`REQ-023`,
  `REQ-025`,`REQ-026`
- Dependencies: `Step 2`
- Baseline state: Light source存在；Service两族仍由337/345个template文件维护。
- Observable outcome: legacy 6-module和open 7-module normal Service projects直接`clean verify`。
- End state: 两个Service source tree具备concrete module/GAV/package；package仍未切换。
- Test-first gate: `Not applicable` — canonical materialization，无新业务行为；current Archetype generation和existing
  tests是输入/输出Gate。
- Manual Checks:
  `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 7, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-{service,service-open}/**`

- Purpose: materialize current Service consumer为normal 6/7-module Maven projects。
- Symbols: root internal artifacts `egon-cola-source-service{,-open}`（normal source POM root不加public `-parent`
  后缀）；module artifact prefixes同root；packages `top.egon.cola.archetype.source.service{,open}`。
- Repository evidence: current descriptor module suffixes与canonical evaluation facade；legacy四Flyway、open manual
  SQL/Proto/ArchUnit contracts。
- Dependencies and consumers: components/platforms、`egon-cola-evaluation-facade`/organization fixture、Step 5 generator。
- Why now: Light证明单模块路径后，建立RPC/MQ multi-module source，单独提交便于审查module parent/dependency映射。
- Contract/signature changes: 仅internal sentinel POM中root parent artifact机械收敛为sourceArtifactId；public generated
  POM仍由normalize恢复`${rootArtifactId}-parent`。
- Input/output and state mapping: current Service GAV + `artifactId=rootArtifactId=egon-cola-source-*` -> temp 6/7
  modules -> remove targets -> source；legacy migration bytes保持。
- Error and edge behavior: module suffix缺失/额外、parent relativePath/GAV不一致、Proto checksum、manual SQL、profile
  key、placeholder或SQL hash任一失败均停止。
- Standards impact: `MC-ARCH-001`,`MC-CONFIG-001`；exact Service/Open COLA module direction保持，所有business Java
  rule合同不改，configuration成组迁移。
- Literal rule enforcement: `Rule 1-4/6/10`原样；`Rule 7`所有starter profile同步；`Rule 11`6/7-module Service
  profile和facade边界不混合。
- Implementation pseudocode:

```bash
for product in service service-open:
  mvnw archetype:generate with public top.egon:egon-cola-archetype-${product}:${currentRootVersion}
    and concrete group=top.egon.internal.archetype.source,
    artifactId=rootArtifactId=egon-cola-source-${product}, version=0.1.0-SNAPSHOT,
    package=top.egon.cola.archetype.source.${serviceOrServiceopen}, gitignore=.gitignore
  assert expected modules = common,domain,application,infrastructure,adapter,starter [+ facade]
  normalize only internal source root parent artifact from sentinel-parent to sentinel
  update child parent references mechanically; do not change dependency direction/content
  remove targets and copy into exact source destination
  assert POM reactor/effective module graph and Java/package inventories
  assert legacy migration and open manual SQL/Proto bytes/checksums
```

- Verification contribution: `TEST-002`,`004`,`019`和public topology source baseline。
- After this file: Service pair可作为normal reactors开发；无package/CI变更。

- Validation working directory: repository root
- Verification command:
  `./mvnw -B -ntp -N install && ./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml install && ./mvnw -B -ntp -f egon-cola-components/pom.xml clean install && ./mvnw -B -ntp -f egon-cola-platforms/pom.xml clean install && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-organization-facade,egon-cola-evaluation-facade -am install && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml clean verify && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml clean verify && ! rg -n '\$\{(package|groupId|rootArtifactId)\}|__rootArtifactId__' egon-cola-archetypes/source-projects/egon-cola-source-{service,service-open}`
- Expected result: 6/7-module builds exit 0；no placeholder；legacy四SQL source/original hash相同。
- Failure returns to: current archetype/facade bootstrap、mechanical POM sentinel change或source inventory；不在本Step重设业务合同。
- Completion criteria: 两个multi-module normal sources绿色且module count精确。
- Rollback: 删除两个新source目录；无外部状态。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-{service,service-open}/**`（即两个新source
  project目录）
- Commit: `refactor(archetypes): materialize service source projects`

### Step 4 — 生成 Web 正常源码并建立非发布 source reactor

- Requirements: `REQ-001`,`REQ-002`,`REQ-009`,`REQ-010`,`REQ-015`,`REQ-017`,`REQ-019`,`REQ-020`,`REQ-021`,`REQ-022`,
  `REQ-023`,`REQ-025`,`REQ-026`
- Dependencies: `Step 3`
- Baseline state: four source projects存在；Web两族仍template-first；无统一source build入口。
- Observable outcome: six source projects在一个internal reactor按components/platforms/facades/source顺序`clean install`。
- End state: 完整normal-code stage存在且root `pom.xml` module list未改变。
- Test-first gate: `Not applicable` — materialization/aggregator是build structure，existing project
  tests提供直接proof；任何业务测试变化禁止。
- Manual Checks:
  `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 7, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-{web,web-open}/**`

- Purpose: materialize current Web 6/7-moduleconsumer为normal projects。
- Symbols: `egon-cola-source-web{,-open}` roots/modules；packages `top.egon.cola.archetype.source.web{,open}`
  ；现有HTTP/GraphQL/OpenAPI/local Proto内容。
- Repository evidence: 427/436 current files、organization facade、current metadata/verifier、legacy Web四Flyway。
- Dependencies and consumers: components/platforms/facades、source aggregator、Step 5 generator。
- Why now: 最后引入Web pair后aggregator可一次声明最终全部modules，避免无效中间POM。
- Contract/signature changes: internal sentinel only；HTTP/GraphQL/OpenAPI/Gateway-negative business contracts不变。
- Input/output and state mapping: current Web GAV -> sentinel temp projects -> exact source dirs，6/7 module topology
  preserved。
- Error and edge behavior: module suffix/parent GAV、placeholder、profile key、OpenAPI/Gateway negative scan或old migration
  hash任一不符都停止，不现场改业务合同。
- Standards impact: `MC-ARCH-001`,`MC-CONFIG-001`；exact Web/Open COLA profile，all profile configs成组，Java/business
  contracts原样。
- Literal rule enforcement: `Rule 1-4/6/10`无内容变更；`Rule 7`全profiles；`Rule 11`6/7 Web结构。
- Implementation pseudocode:

```bash
generate web and web-open from current public top.egon GAVs with
  group=top.egon.internal.archetype.source,
  artifactId=rootArtifactId=egon-cola-source-${product}, version=0.1.0-SNAPSHOT,
  package=top.egon.cola.archetype.source.${webOrWebopen}, gitignore=.gitignore
assert module suffixes and local facade only for open
normalize internal root parent artifact only
copy preserving wrapper mode; remove targets
assert no template placeholder, no generated Gateway artifact/property, current OpenAPI tests remain
assert legacy Web migration source copies equal immutable archive hashes
```

- Verification contribution: `TEST-003`,`004`,`019`。
- After this file: all six source dirs存在，但统一aggregator尚未声明。

#### File 2 — `CREATE egon-cola-archetypes/source-projects/pom.xml`

- Purpose: 为fresh/unreleased同版本提供一个不进入Central的build reactor。
- Symbols: internal aggregator `top.egon.internal:egon-cola-archetype-source-projects:0.1.0-SNAPSHOT`; ordered Maven
  module list。
- Repository evidence: root modules顺序components -> platforms -> archetypes；主 Spec ASM-004要求复用facades。
- Dependencies and consumers: references `../../egon-cola-components`, `../../egon-cola-platforms`,
  `../egon-cola-organization-facade`, `../egon-cola-evaluation-facade`, then six sources；CI/local build only。
- Why now: every declared directory已存在，可保持commit Maven-valid。
- Contract/signature changes: no distributionManagement/release/Central plugin；not added to root modules。
- Input/output and state mapping: repository source modules -> reactor local install；不产生public deployment inventory。
- Error and edge behavior: module missing或duplicate让Maven model失败；source artifact不得出现在root/bom/public docs。
- Standards impact: `MC-ARCH-001`只聚合不改变module内层次；`MC-DEP-001`无新dependency。
- Literal rule enforcement: `Rule 11`aggregator不引入第三业务层，逐族project保持exact profile。
- Implementation pseudocode:

```xml
<project>
  <groupId>top.egon.internal</groupId>
  <artifactId>egon-cola-archetype-source-projects</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  modules in order:
    components; platforms; organization-facade; evaluation-facade;
    light; service; web; light-open; service-open; web-open
</project>
```

- Verification contribution: authoritative `TEST-001`-`003` source build和`REQ-010` negative release boundary。
- After this file: one command builds all internal prerequisites and six normal sources。

- Validation working directory: repository root
- Verification command:
  `./mvnw -B -ntp -N install && ./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml install && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install && ! rg -n '\$\{(package|groupId|rootArtifactId)\}|__rootArtifactId__' egon-cola-archetypes/source-projects/egon-cola-source-* && ! rg -n 'source-projects|egon-cola-source-' pom.xml --glob 'pom.xml'`
- Expected result: source reactor全部success；placeholder zero；root module scan zero；legacy migration paths/hashes全12通过。
- Failure returns to: File 1 source materialization，File 2 module order/path；业务测试差异返回primary/predecessor Spec。
- Completion criteria: normal-code stage完整、可直接开发、无Central/public module影响。
- Rollback: 删除Web pair和source aggregator；prior four sources仍独立存在。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-{web,web-open}/**`,
  `egon-cola-archetypes/source-projects/pom.xml`
- Commit: `refactor(archetypes): complete normal source reactor`

### Step 5 — 声明六个manifest并生成完整派生工作区

- Requirements: `REQ-003`,`REQ-004`,`REQ-005`,`REQ-006`,`REQ-007`,`REQ-008`,`REQ-009`,`REQ-013`,`REQ-015`,`REQ-016`,
  `REQ-017`,`REQ-024`,`REQ-026`
- Dependencies: `Step 4`
- Baseline state: six sources和generator存在；real generate因zero manifests预期失败。
- Observable outcome: dynamic discovery生成/检查六个deterministic target，并完成same-sentinel round-trip和migration
  parity。
- End state: ignored `.generated`完整可重建；package模块仍读旧template，package-family test保持预期RED直到Steps 6-8。
- Test-first gate: `Required` — Step 1已建立real generation assertions且zero-manifest RED；本Step只增加声明数据使其GREEN。
- Manual Checks:
  `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-UTIL-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 7, Rule 9, Rule 11`
- Ordered files:

#### File 1 —
`CREATE egon-cola-archetypes/egon-cola-archetype-{light,service,web,light-open,service-open,web-open}/src/main/archetype/archetype.properties`

- Purpose: 每个public package module声明自己的source sentinel、target和topology，供所有脚本/workflow动态发现。
- Symbols: keys exactly
  `sourceProject,sourceGroupId,sourceArtifactId,sourceVersion,sourcePackage,targetArtifactId,expectedTopology`。
- Repository evidence: six package POM artifactIds；source paths/GAV/packages from Steps 2-4；descriptor module suffixes。
- Dependencies and consumers: generator parser、version/CI/release discovery；不被打进最终Archetype资源。
- Why now: six source全部可build，manifest不会引用不存在路径。
- Contract/signature changes: new internal properties contract only；targetArtifactId必须等于owning package POM。
- Input/output and state mapping: one manifest -> one source -> one `.generated/<targetArtifactId>`；topology
  value为root或suffix列表。
- Error and edge behavior: no comments interpreted as
  fields；unknown/missing/duplicate/escape失败；group/version/package需exact sentinel。
- Standards impact: `MC-CONFIG-001` manifest不是Spring profile；`MC-PATTERN-001`数据驱动替代硬编码products。
- Literal rule enforcement: `Rule 5`plain properties parser；`Rule 7`不改变Spring config；`Rule 9`variation明确放在manifest；
  `Rule 11`topology逐族exact。
- Implementation pseudocode:

```properties
sourceProject=../source-projects/egon-cola-source-service
sourceGroupId=top.egon.internal.archetype.source
sourceArtifactId=egon-cola-source-service
sourceVersion=0.1.0-SNAPSHOT
sourcePackage=top.egon.cola.archetype.source.service
targetArtifactId=egon-cola-archetype-service
expectedTopology=common,domain,application,infrastructure,adapter,starter
```

- Verification contribution: `TEST-005`,`007`,`012` dynamic inventory/topology。
- After this file: `generate`有exact six inputs；尚未产生tracked output。

#### File 2 —
`GENERATED egon-cola-archetypes/.generated/egon-cola-archetype-{light,service,web,light-open,service-open,web-open}/**`

- Purpose: 由CLI生成全六族read-only package inputs和provenance hash。
- Symbols: per target `archetype-resources/**`, `generation-manifest.sha256`。
- Repository evidence: target tree from main Spec §8.2；Plugin diagnostic实际输出
  `src/main/resources/archetype-resources`。
- Dependencies and consumers: generated by File 1 manifests + source trees；Steps 6-8 package POM读取；Git不跟踪。
- Why now: manifests完整后第一次建立原子set，供cutover逐组消费。
- Contract/signature changes: no public contract；derived content必须规范化到current curated descriptor预期。
- Input/output and state mapping: concrete GAV/package/root modules ->
  `${groupId}/${version}/${package}/${rootArtifactId}`；source `.gitignore` -> `__gitignore__`；root/module POM保持current
  consumer semantics。
- Error and edge behavior: two-generation hash diff、source mutation、sentinel leakage、missing topology、old migration hash
  mismatch、curated metadata uncovered path均fail且不swap。
- Standards impact: `MC-ARCH-001`,`MC-CONFIG-001`,`MC-SCOPE-001`；no manual edit，config/Java/content parity由round-trip证明。
- Literal rule enforcement: `Rule 7`profile key/content parity；`Rule 11`generated module/profile保持。
- Implementation pseudocode:

```text
run generate -> stage all six -> validate -> atomic swap
run check -> generate twice in temp -> compare both -> compare current generated
for each target generate a same-sentinel consumer with curated descriptor
normalize target/.git/.idea and source-only manifest exclusions
assert consumer tree == source tree, including executable mvnw policy and SQL hashes
```

- Verification contribution: `TEST-007`,`009`,`013`,`019`,`021`。
- After this file: complete local generated set存在但`git status`不显示；old package templates仍是authoritative packaging
  input until cutover。

- Validation working directory: repository root
- Verification command:
  `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install && ./scripts/generate_archetypes.sh generate && ./scripts/generate_archetypes.sh check && ./scripts/test-generate-archetypes.sh generation && test "$(find egon-cola-archetypes -path '*/src/main/archetype/archetype.properties' | wc -l | tr -d ' ')" = 6 && test -z "$(git ls-files egon-cola-archetypes/.generated)"`
- Expected result: source install和three generator checks exit 0；six manifests/targets；deterministic/round-trip/hash
  clean；package mode尚可针对旧POM预期RED。
- Failure returns to: manifest File 1若字段/topology；Step 1 generator若transform/atomic；Steps 2-4 source若round-trip业务差异。
- Completion criteria: one complete ignored set可重复生成，无sentinel/descriptor/Flyway drift。
- Rollback: revert six manifests并删除exact ignored `.generated`; source projects不受影响。
- Commit paths:
  `egon-cola-archetypes/egon-cola-archetype-{light,service,web,light-open,service-open,web-open}/src/main/archetype/archetype.properties`;
  `egon-cola-archetypes/.generated/egon-cola-archetype-{light,service,web,light-open,service-open,web-open}/**`
  仅作为ignored验证输入，明确不得stage。
- Commit: `build(archetypes): declare six source generation manifests`

### Step 6 — 切换 Light 发布模块并统一 Archetype release治理

- Requirements: `REQ-002`,`REQ-005`,`REQ-007`,`REQ-008`,`REQ-009`,`REQ-012`,`REQ-013`,`REQ-015`,`REQ-016`,`REQ-017`,
  `REQ-018`,`REQ-019`,`REQ-020`,`REQ-021`,`REQ-022`,`REQ-023`,`REQ-024`,`REQ-025`,`REQ-026`
- Dependencies: `Step 5`
- Baseline state: package-light test因POM未读`.generated`且旧业务模板仍tracked而RED。
- Observable outcome: Light pair只从generated加载业务内容，curated contract与consumer行为保持，release
  shape有main/sources/javadoc。
- End state: Light legacy只保留四个不可变Flyway archive；Light Open旧template目录删除；other four package仍template-first。
- Test-first gate: `Required` — 先运行`package light`确认旧ownership RED；更新verifier/POM/delete后GREEN。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 7, Rule 11`
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/test/resources/projects/basic/verify.groovy`

- Purpose: 在public consumer层增加source sentinel泄漏、derived ownership、topology和migration断言。
- Symbols: existing Groovy verification script assertions。
- Repository evidence: current scripts已验证完整consumer；主 Spec保留该framework。
- Dependencies and consumers: Maven Archetype IT生成的basic project；不读取repo外部基础设施。
- Why now: 先固定cutover后的observable output，再改POM资源。
- Contract/signature changes: no public API；assert no `top.egon.internal.archetype.source`, `egon-cola-source-*`,
  `0.1.0-SNAPSHOT`, `.generated` path。
- Input/output and state mapping: generated basic project tree -> current contract assertions + no-sentinel + Light
  migration checks。
- Error and edge behavior: missing/extra file、wrong executable contract、sentinel或SQL mismatch throw
  AssertionError使IT非零。
- Standards impact: existing Java/config contracts only observed；`MC-CONFIG-001`profile keys remain。
- Literal rule enforcement: `Rule 7`assert profile parity；`Rule 11`assert single-module Light profiles。
- Implementation pseudocode:

```groovy
assertGeneratedProject(project) {
  existingAllAssertions()
  assert forbiddenSentinelTokens(project).isEmpty()
  assert !anyPathContains('.generated')
  assert exactLightTopology(project)
  if legacy: assert migrationRelativePathsAndHashes(project, immutableBaseline)
}
```

- Verification contribution: `TEST-013`,`014`,`017`,`019`。
- After this file: verifier可在旧/新输出上运行；package ownership仍由shell RED指出。

#### File 2 — `MODIFY egon-cola-archetypes/pom.xml`

- Purpose: 统一Archetype lifecycle/goal版本并集中外部resources、preflight、sources/javadoc/GPG顺序所需plugin治理。
- Symbols: property `maven.archetype.version=3.4.1`; extension/pluginManagement versions；Enforcer/Jar execution
  templates。
- Repository evidence: current extension 3.2.1、plugin 3.4.1、Jar/Source/Javadoc/GPG/Enforcer均已管理。
- Dependencies and consumers: six package children；facade modules不激活Archetype-specific executions。
- Why now: first package cutover前建立共享版本/附件合同，后续children只声明plugin/resource。
- Contract/signature changes: extension upgrade 3.2.1 -> property 3.4.1；release GAV/version unchanged。
- Input/output and state mapping: child plugin declaration -> validate generated `archetype-resources/pom.xml`; package
  attach README javadoc before verify GPG。
- Error and edge behavior: missing generated root fail validate withgenerate-first message；javadoc dir missing/empty
  fail shape；facades不附占位classifier。
- Standards impact: `MC-DEP-001`只对齐existing plugin，无new dependency；no Java。
- Literal rule enforcement: `Rule 5`existing Maven plugins only；`Rule 11`parent不改module list/profile。
- Implementation pseudocode:

```xml
<maven.archetype.version>3.4.1</maven.archetype.version>
archetype-packaging.version = ${maven.archetype.version}
maven-archetype-plugin.version = ${maven.archetype.version}
pluginManagement:
  enforcer execution require-generated-archetype-resources at validate
  jar execution attach-archetype-javadocs from src/main/javadoc at package classifier=javadoc
release profile keeps source at verify, gpg after attached artifacts, Central root behavior unchanged
```

- Verification contribution: `TEST-011`,`015`,`016` configuration和`REQ-013`。
- After this file: shared governance可由children opt in；service/web尚未改变。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-archetype-{light,light-open}/pom.xml`

- Purpose: package generated template + curated META-INF并激活shared preflight/javadoc execution。
- Symbols: Maven build resources、`maven-enforcer-plugin`和`maven-jar-plugin` declarations。
- Repository evidence: current minimalPOM/GAV metadata；generated target path；javadoc plugin skip evidence。
- Dependencies and consumers: `.generated/${project.artifactId}/archetype-resources`; committed
  `src/main/resources/META-INF`; release profile。
- Why now: shared parent合同可继承；verifier已固定consumer output。
- Contract/signature changes: none to GAV；main JAR resource origin changes；new javadoc classifier。
- Input/output and state mapping: external generated target copied as root `archetype-resources/**`; curated META-INF
  copied last；source plugin sees configured resources；File 4 README becomes javadoc JAR input。
- Error and edge behavior: missing generated POM/empty dir fail validate；curated descriptor duplicate
  forbidden；classifier input missing makes shape Gate fail。
- Standards impact: `MC-REUSE-001`,`MC-DEP-001`；config only，复用existing plugins。
- Literal rule enforcement: `Rule 11`single-module consumer output不变。
- Implementation pseudocode:

```xml
resources:
  - directory ../.generated/${project.artifactId}, filtering=false
    include archetype-resources/**
  - directory src/main/resources, filtering=false
    include META-INF/**
plugins:
  - maven-enforcer-plugin (inherit require generated POM)
  - maven-jar-plugin (inherit attach README javadoc)
```

- Verification contribution: `TEST-011`,`012`,`015`。
- After this file: Light pair package读取generated并保持curated descriptor ownership。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/main/javadoc/README.md`

- Purpose: 为两个非classpath Archetype制品提供真实、说明性的javadoc classifier内容。
- Symbols: bilingual-friendly artifact purpose、consumer generation command、指向repository/public docs的说明文本。
- Repository evidence: current `maven-javadoc-plugin`对`maven-archetype`跳过；主 Spec DEC-006选择README JAR。
- Dependencies and consumers: Step 6 parent Jar execution读取；Central/repository consumer只把它作为文档附件。
- Why now: POM已声明attachment input，先创建内容再执行package/verify。
- Contract/signature changes: 新增`${artifactId}-${version}-javadoc.jar`内容，不声明模板Java API javadocs。
- Input/output and state mapping: committed README -> jar root `README.md` -> GPG signature duringreal release。
- Error and edge behavior: README不得为空、不得含source sentinel/secret/local absolute path；artifactId说明必须与owning
  module一致。
- Standards impact: `MC-DEP-001`,`MC-SCOPE-001`；documentation-only，无Java类型或dependency。
- Literal rule enforcement: `Rule 11`明确文档对应Light/Light Open exact public Archetype，不创造新structure。
- Implementation pseudocode:

```markdown
# Egon-COLA Light Archetype documentation
State that this classifier documents a Maven Archetype distribution, not a Java API.
Name the owning public GAV and the standard archetype:generate consumer command.
Point maintainers to source-projects as edit owner and to generated/package verification docs.
Do not include internal sentinel coordinates or local filesystem paths.
```

- Verification contribution: `TEST-015`检查classifier存在且JAR内`README.md`非空。
- After this file: Light pair具备package阶段可附加的说明文档输入。

#### File 5 —
`DELETE egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/main/resources/archetype-resources/** (legacy Light db/migration/** excepted)`

- Purpose: 完成Light single source of truth，不违反Flyway immutable rule。
- Symbols: delete所有Java/POM/config/test/docs/manual SQL等old resources；legacy exact four migration paths不操作。
- Repository evidence: 429/427 tracked counts；current legacy four migration list。
- Dependencies and consumers: source copies + generated resource已由File 3提供；package resource includes不读取archive；File
  4只负责附件。
- Why now: 只有new package GREEN path已配置后才删除duplicate source。
- Contract/signature changes: Git ownership only；consumer bytes来自source-generated copy。
- Input/output and state mapping: old business files removed；legacy archive retained；source working copy继续生成classpath
  SQL。
- Error and edge behavior: any attempted Flyway delete/modify/rename/hash drift立即停止；Open pair不留空占位文件。
- Standards impact: no Java rewrite；`MC-SCOPE-001`严格allowlist。
- Literal rule enforcement: `Rule 7`config已迁source且consumer parity；`Rule 11`no module change。
- Implementation pseudocode:

```text
compute immutable Light migration path+sha inventory before deletion
delete old Light archetype-resources minus exact migration allowlist
delete all Light Open old archetype-resources
assert immutable paths and sha unchanged
assert package main/sources jars obtain corresponding SQL/business files from .generated
```

- Verification contribution: `TEST-002`,`015`,`019`,`021`。
- After this file: Light pair完成source-first，no duplicate editable business tree。

- Validation working directory: repository root
- Verification command:
  `./scripts/test-generate-archetypes.sh package light && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-light,egon-cola-archetype-light-open -am clean integration-test && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-light,egon-cola-archetype-light-open -am -Prelease -Dgpg.skip=true clean verify`
- Expected result: package contract GREEN；two IT pass；each two Archetype target has
  main/sources/javadoc，sources含POM/Java，javadoc含README；extension log=3.4.1。
- Failure returns to: File 1 consumer assertion；File 2 shared lifecycle；File 3 resource wiring；File 4 javadoc
  content；File 5 inventory/hash。
- Completion criteria: Light pair public behavior等价，source ownership唯一，old Flyway immutable。
- Rollback: revert Step 6 paths并regenerate；不要改Flyway；Central未写。
- Commit paths:
  `egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/test/resources/projects/basic/verify.groovy`;
  `egon-cola-archetypes/pom.xml`; `egon-cola-archetypes/egon-cola-archetype-{light,light-open}/pom.xml`;
  `egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/main/javadoc/README.md`;
  `egon-cola-archetypes/egon-cola-archetype-{light,light-open}/src/main/resources/archetype-resources/** (legacy Light db/migration/** excepted)`。
- Commit: `refactor(archetypes): switch light packages to generated sources`

### Step 7 — 切换 Service 发布模块到正常源码派生物

- Requirements: `REQ-002`,`REQ-005`,`REQ-007`,`REQ-008`,`REQ-009`,`REQ-012`,`REQ-015`,`REQ-016`,`REQ-017`,`REQ-018`,
  `REQ-019`,`REQ-020`,`REQ-021`,`REQ-022`,`REQ-023`,`REQ-024`,`REQ-025`,`REQ-026`
- Dependencies: `Step 6`
- Baseline state: Light pair source-first；Service package test因旧resource ownership RED。
- Observable outcome: legacy/open Service 6/7-moduleconsumer由normal sources派生且release shape完整。
- End state: legacy Service旧template只留四Flyway archive；Service Open旧业务tree删除。
- Test-first gate: `Required` — `package service`在修改前因old ownership/POM RED，完成Files后GREEN。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 7, Rule 11`
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/test/resources/projects/basic/verify.groovy`

- Purpose: 加入6/7-module、rootArtifactId、sentinel、Proto/manual/Flyway边界断言。
- Symbols: existing verify scripts。
- Repository evidence: current Service verifiers和metadata module definitions。
- Dependencies and consumers: generated basic multi-moduleprojects；canonical/local facade contracts。
- Why now: resource cutover前固定observable contract。
- Contract/signature changes: no public contract；negative sentinel/derived assertions。
- Input/output and state mapping: basic project -> exact root parent/modules/dependency artifactIds；legacy
  migration/open manual SQL/Proto保持。
- Error and edge behavior: `${artifactId}`误替代rootArtifactId、concrete sentinel残留、module dir错误、hash/checksum差异均fail。
- Standards impact: `MC-ARCH-001`,`MC-CONFIG-001`；existing Java/config/profile contracts observed。
- Literal rule enforcement: `Rule 7`starter profiles；`Rule 11`6/7 Service exact profile。
- Implementation pseudocode:

```groovy
existingAssertions()
assertRootPomArtifact("${rootArtifactId}-parent")
assertModules(expectedSixOrSeven)
assertNoSentinelOrGeneratedPath()
assertLegacyFlywayOrOpenManualProtoContracts()
```

- Verification contribution: `TEST-012`-`014`,`017`,`019`。
- After this file: consumer contract ready forcutover。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-{service,service-open}/pom.xml`

- Purpose: opt in Step 6 external resource/preflight/javadoc governance。
- Symbols: ordered resources/plugins and multi-module generated preflight。
- Repository evidence: current service POM adds facade test dependencies但无build resources；shared parent available。
- Dependencies and consumers: complete `.generated` and curated META-INF/IT。
- Why now: verifier fixed and parent contract stable。
- Contract/signature changes: GAV/dependencies unchanged；resource origin only。
- Input/output and state mapping: generated multi-module tree + curated descriptor -> current Service JAR/consumer；File
  3 README -> javadoc classifier。
- Error and edge behavior: missing module/generated root/duplicate META-INF/classifier fail before deploy。
- Standards impact: `MC-REUSE-001`,`MC-DEP-001`；no dependency/Java change。
- Literal rule enforcement: `Rule 5`existing plugins；`Rule 11`exact modules。
- Implementation pseudocode:

```xml
resources = [
  ../.generated/${project.artifactId} including archetype-resources/**,
  src/main/resources including META-INF/**
]
inherit require-generated-archetype-resources at validate
require each manifest topology module directory and pom.xml before package
attach src/main/javadoc/README.md as classifier javadoc at package
```

- Verification contribution: `TEST-011`,`012`,`015`。
- After this file: Service pair build input已切到generated。

#### File 3 — `CREATE egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/main/javadoc/README.md`

- Purpose: 为Service两个Archetype附加说明性javadoc文档，不伪造模板Java API。
- Symbols: owning GAV、6/7 module用途、consumer generation与source ownership说明。
- Repository evidence: Step 6 parent Jar execution和main Spec DEC-006；当前目录不存在。
- Dependencies and consumers: owning POM attachment execution、release-shape/GPG/Central。
- Why now: package wiring已声明input，删除old resources前创建独立committed attachment source。
- Contract/signature changes: new javadoc classifier content only；no GAV/API/module change。
- Input/output and state mapping: each README -> own target javadoc JAR root -> verify-phase signature。
- Error and edge behavior: no internal sentinel/local path/secret；Service和Service Open module count文字必须对应manifest。
- Standards impact: `MC-DEP-001`,`MC-SCOPE-001`；documentation-only，无Java/dependency。
- Literal rule enforcement: `Rule 11`文档分别陈述exact 6/7-module Service profiles。
- Implementation pseudocode:

```markdown
# Egon-COLA Service Archetype documentation
Describe the owning public Maven Archetype GAV and whether it generates six or seven modules.
Explain that the classifier documents a project generator rather than runtime Java APIs.
Give the public archetype:generate entry and point maintainers to the normal source project.
Exclude internal sentinel coordinates, local paths and release credentials.
```

- Verification contribution: `TEST-015` classifier entry/content和Step 9 signature scan。
- After this file: Service pair attachment input完整。

#### File 4 —
`DELETE egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/main/resources/archetype-resources/** (legacy Service db/migration/** excepted)`

- Purpose: 移除Service duplicate truth并保留Flyway history。
- Symbols: delete337/345树中的非legacy archive业务内容；legacy four exact paths unchanged。
- Repository evidence: main Spec EVD-015和current migration inventory。
- Dependencies and consumers: source/generated contents replace deleted files。
- Why now: POM已切换且preflight确保generated完整。
- Contract/signature changes: ownership only。
- Input/output and state mapping: old Service business files移除，legacy四SQL archive原地保留；normal source经generator映射到
  `__rootArtifactId__-*`并进入main/sources JAR。
- Error and edge behavior: immutable file operation/hash mismatch blocks；source/consumer module mappingdiff blocks。
- Standards impact: `MC-CONFIG-001`,`MC-SCOPE-001`；no Java rewrite，config parity保留。
- Literal rule enforcement: `Rule 7`,`Rule 11`通过round-trip/IT。
- Implementation pseudocode:

```text
record exact legacy Service migration hashes
delete non-allowlisted legacy resources and all Service Open resources
assert old migration inventory unchanged
assert package/source jars contain generated POM/Java/config/SQL for all modules
```

- Verification contribution: `TEST-002`,`014`,`015`,`019`,`021`。
- After this file: Service pair source-first；only Web pair remainsold ownership。

- Validation working directory: repository root
- Verification command:
  `./scripts/test-generate-archetypes.sh package service && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-service,egon-cola-archetype-service-open -am clean integration-test && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-service,egon-cola-archetype-service-open -am -Prelease -Dgpg.skip=true clean verify`
- Expected result: package/IT/release shape全绿；6/7 modules；four old SQL hash unchanged。
- Failure returns to: File 1 normalize/consumer；File 2 POM/resources；File 3 javadoc content；File 4 deletion/hash。
- Completion criteria: Service pair只有normal source可编辑，public GAV/consumer行为保持。
- Rollback: revert Step 7 exact paths；regenerate；no Central/db state。
- Commit paths:
  `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/test/resources/projects/basic/verify.groovy`;
  `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/pom.xml`;
  `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/main/javadoc/README.md`;
  `egon-cola-archetypes/egon-cola-archetype-{service,service-open}/src/main/resources/archetype-resources/** (legacy Service db/migration/** excepted)`。
- Commit: `refactor(archetypes): switch service packages to generated sources`

### Step 8 — 切换 Web 发布模块并完成六族单事实源

- Requirements: `REQ-002`,`REQ-005`,`REQ-007`,`REQ-008`,`REQ-009`,`REQ-012`,`REQ-015`,`REQ-016`,`REQ-017`,`REQ-018`,
  `REQ-019`,`REQ-020`,`REQ-021`,`REQ-022`,`REQ-023`,`REQ-024`,`REQ-025`,`REQ-026`
- Dependencies: `Step 7`
- Baseline state: four package modules source-first；Web package test RED。
- Observable outcome: all six package modules只消费normal-source派生物且full Archetype reactor通过。
- End state: legacy Web只留四Flyway archive；Web Open旧tree删除；六族ownership迁移完成。
- Test-first gate: `Required` — `package web`先RED，Files后GREEN，再跑全六族IT/shape。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 7, Rule 11`
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/test/resources/projects/basic/verify.groovy`

- Purpose: 固定6/7-module Web、OpenAPI、Gateway-negative、rootArtifactId和sentinel contract。
- Symbols: existing Web Groovy verifiers。
- Repository evidence: current verifier已有HTTP/GraphQL/config/architecture assertions。
- Dependencies and consumers: generated basic projects/local facade/OpenAPI tests。
- Why now: 先测试consumer再cutover。
- Contract/signature changes: none；only stronger transformation invariants。
- Input/output and state mapping: generated project -> exact modules/POM/HTTP docs/config/SQL and no sentinel。
- Error and edge behavior: Gateway artifact/property出现、OpenAPI缺失、module/root POM错误、sentinel/hash mismatch全部fail。
- Standards impact: `MC-ARCH-001`,`MC-CONFIG-001`；config/profile and architecture only。
- Literal rule enforcement: `Rule 7`profile parity；`Rule 11`Web/Open exact profile。
- Implementation pseudocode:

```groovy
existingAssertions()
assert exactWebModules(openVariant ? sevenModules : sixModules)
assert forbiddenSentinelTokens(project).isEmpty()
assert openApiContractIsPresent()
assert gatewayDependenciesAndPropertiesAreAbsent()
assert legacyFlywayOrOpenManualSqlContracts()
```

- Verification contribution: `TEST-012`-`014`,`017`,`019`。
- After this file: Web observable contract locked。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-{web,web-open}/pom.xml`

- Purpose: opt in generated resources/preflight/javadoc attachment。
- Symbols: ordered build resources、shared Enforcer/Jar executions、multi-module preflight。
- Repository evidence: current Web POM/facade test dependencies；Step 6 parent治理。
- Dependencies and consumers: `.generated` Web pair、curated META-INF、IT/release。
- Why now: test contract已锁。
- Contract/signature changes: no public GAV/dependency change。
- Input/output and state mapping: generated Web resources + curated descriptor -> public artifacts；expected six/seven
  module directories preflight；File 3 README提供classifier内容。
- Error and edge behavior: missing generated/module/README/classifier fail closed。
- Standards impact: `MC-REUSE-001`,`MC-DEP-001`；no new dependency/Java。
- Literal rule enforcement: `Rule 5`,`Rule 11`。
- Implementation pseudocode:

```xml
read ../.generated/${project.artifactId}/archetype-resources as first resource
overlay only committed src/main/resources/META-INF/** as second resource
validate every six/seven-module path declared by owning manifest
attach src/main/javadoc/README.md at package before verify-phase GPG
preserve current GAV, facade test dependencies and curated integration tests
```

- Verification contribution: `TEST-011`,`012`,`015`。
- After this file: final pairpackage readsderived input。

#### File 3 — `CREATE egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/main/javadoc/README.md`

- Purpose: 为Web两个Archetype创建说明性Central javadoc附件输入。
- Symbols: owning GAV、6/7 modules、HTTP/GraphQL/OpenAPI用途、source ownership与consumer command。
- Repository evidence: current dirs absent；Step 6 parent execution和main Spec DEC-006。
- Dependencies and consumers: owning Web POMs、release-shape/GPG/Central。
- Why now: generated resource wiring完成后，删除old business tree前补齐independent committed docs。
- Contract/signature changes: new javadoc classifier content only；no HTTP/GraphQL/OpenAPI contract change。
- Input/output and state mapping: README -> javadoc JAR root -> real release signature。
- Error and edge behavior: no local/sentinel/secret；Web Open说明external Gateway且不声称generated Gateway module。
- Standards impact: `MC-DEP-001`,`MC-SCOPE-001`；documentation-only，无Java/dependency。
- Literal rule enforcement: `Rule 11`分别说明exact Web 6-module和Web Open 7-module profile。
- Implementation pseudocode:

```markdown
# Egon-COLA Web Archetype documentation
Name the owning public GAV and exact six/seven-module project purpose.
State that HTTP, GraphQL and OpenAPI live in the generated project while Gateway remains external.
Document the public archetype:generate entry and the normal source maintenance path.
Do not expose internal sentinel coordinates, local paths, credentials or unsupported runtime claims.
```

- Verification contribution: `TEST-015` README classifier content和Step 9 signature scan。
- After this file: Web pairattachment source完整。

#### File 4 —
`DELETE egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/main/resources/archetype-resources/** (legacy Web db/migration/** excepted)`

- Purpose: 完成all-six single-source ownership。
- Symbols: deletelegacy非migration和Open全部old resources；keep four exact legacy migration files。
- Repository evidence: current 427/436 counts和migration inventory。
- Dependencies and consumers: source/generated replacements alreadyready。
- Why now: last cutover completes Spec rollout atomic end state。
- Contract/signature changes: repository ownership only。
- Input/output and state mapping: normal source -> generated -> package/consumer；archive not packaged by
  committed-resource include。
- Error and edge behavior: any old businessfile remaining or Flyway path/hash change fails full audit。
- Standards impact: `MC-ARCH-001`,`MC-CONFIG-001`,`MC-SCOPE-001`；no business content rewrite。
- Literal rule enforcement: `Rule 7`,`Rule 11`throughfull source/consumer verification。
- Implementation pseudocode:

```text
capture the four exact legacy Web migration paths and SHA-256 values
delete every other legacy Web archetype-resources file and all Web Open old resources
assert all four archive paths and hashes are unchanged
assert no old business Java, POM, config, test or docs remain under package resources
assert generated package/source jars still contain the corresponding consumer files
```

- Verification contribution: `TEST-003`,`014`,`015`,`019`,`021`。
- After this file: six package modules source-first；old editable template business content为零。

- Validation working directory: repository root
- Verification command:
  `./scripts/test-generate-archetypes.sh package web && ./scripts/generate_archetypes.sh check && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test && ./mvnw -B -ntp -Prelease -Dgpg.skip=true clean verify`
- Expected result: all six package modes/IT pass；root release-shape main/sources/javadoc齐全；no old business tree、no
  trackedgenerated、12 Flyway hashes unchanged。
- Failure returns to: File 1 consumer/normalize；File 2 package wiring；File 3 javadoc；File 4 ownership/hash；跨族失败回owning
  Step 6/7。
- Completion criteria: full six source->generated->package->consumer chain绿色，尚未有Central或GPGlive proof。
- Rollback: revert Step 8 exactpaths；all earlierpairs不受影响；no DB/Central state。
- Commit paths:
  `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/test/resources/projects/basic/verify.groovy`;
  `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/pom.xml`;
  `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/main/javadoc/README.md`;
  `egon-cola-archetypes/egon-cola-archetype-{web,web-open}/src/main/resources/archetype-resources/** (legacy Web db/migration/** excepted)`。
- Commit: `refactor(archetypes): switch web packages to generated sources`

### Step 9 — 将版本、CI 与 Central 发布入口接入两阶段 Gate

- Requirements: `REQ-004`,`REQ-010`,`REQ-011`,`REQ-012`,`REQ-014`,`REQ-016`,`REQ-017`,`REQ-018`,`REQ-019`,`REQ-020`,
  `REQ-021`,`REQ-024`,`REQ-026`
- Dependencies: `Step 8`
- Baseline state: local full chain可运行；version脚本仍扫描old template；workflow/local deploy可绕过generate。
- Observable outcome: 所有维护/CI/release入口按source install -> generate/check -> IT -> release shape -> optional one
  root deploy顺序执行。
- End state: dynamic manifest inventory驱动consumer matrix；source sentinel不被bump；Central source
  artifact缺席；真实publish仍需user protected confirmation。
- Test-first gate: `Required` — 创建version fixture先对旧discovery RED；`test-generate ... release`对旧workflow order
  RED；修改后GREEN。
- Manual Checks:
  `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001`
- Literal Rules: `Rule 5, Rule 7, Rule 11`
- Ordered files:

#### File 1 — `CREATE scripts/test-bump-cola-version.sh`

- Purpose: 在isolated fixture验证version owner迁到source且rollback/sentinel/derived boundary正确。
- Symbols: tests `updates_source_egon_version`, `preserves_source_sentinel_version`, `never_edits_generated`,
  `rolls_back_on_failure`。
- Repository evidence: current bump script已有backup/trap但discover template POM；无测试。
- Dependencies and consumers: temp mini-repo + copiedscript/fake mvnw；CI static/unit。
- Why now: 先让old function name/path行为失败。
- Contract/signature changes: internal test CLI only。
- Input/output and state mapping: current/new Egon version -> root/reactor/source `<egon-cola.version>`/README；source
  project own 0.1.0 stays；`.generated` seeded hash stays。
- Error and edge behavior: invalid version、mismatch、fake Maven failureassert rollback exact；不触碰real worktree。
- Standards impact: `MC-UTIL-001`,`MC-TEST-001`；JDK/POSIX only。
- Literal rule enforcement: `Rule 5`allowlist；`Rule 11`only buildmetadata。
- Implementation pseudocode:

```bash
create temp fixture with root POM, source POM egon-cola.version, sentinel project.version, README, seeded .generated
copy bump script and fake mvnw
run new version
assert root/source egon version and README updated
assert sentinel project.version and generated tree hash unchanged
inject failure; assert all managed files restored byte-for-byte
```

- Verification contribution: `TEST-020`,`016`。
- After this file: old bump script按预期RED。

#### File 2 — `MODIFY scripts/bump_cola_version.sh`

- Purpose: 把template POM discovery替换为normal source POM discovery并保持事务rollback。
- Symbols: rename `find_archetype_template_poms` -> `find_archetype_source_poms`; corresponding verify/update
  functions/logs。
- Repository evidence: lines 45-69/132-156当前扫描`src/main/resources/archetype-resources`。
- Dependencies and consumers: source projects/README/root versions；不调用generator，不编辑`.generated`。
- Why now: File 1锁定version mapping。
- Contract/signature changes: user CLI仍`<new-version>`；managed path改变。
- Input/output and state mapping: only source POMs containing`<egon-cola.version>` update；source
  `<version>0.1.0-SNAPSHOT`excluded。
- Error and edge behavior: no source POM/mismatch/failure rollback；prune `.git/.worktrees/target/.generated`。
- Standards impact: `MC-UTIL-001`,`MC-SCOPE-001`；no new utility/dependency。
- Literal rule enforcement: `Rule 5`,`Rule 11`。
- Implementation pseudocode:

```bash
find source-projects/egon-cola-source-* -type f -name pom.xml excluding target/.generated
for each egon-cola.version tag: require current root version then replace with new
never replace project.version 0.1.0-SNAPSHOT
reuse existing backup/trap/restore and final root validate
```

- Verification contribution: `TEST-020`。
- After this file: version fixture GREEN。

#### File 3 —
`MODIFY {scripts/maven-deploy.sh,scripts/maven-deploy.md,egon-cola-archetypes/open-source-archetype-code-style.md}`

- Purpose: 统一local release command、operator runbook和contributor edit owner。
- Symbols: deploy `run_archetype_preflight`; docs two-stage sequence/classifier/signature/UNKNOWN；code-style source
  paths。
- Repository evidence: deploy目前直接clean verify/deploy；doc声称javadoc但无generate；code-style称edit templates。
- Dependencies and consumers: source reactor/generator/package/root Maven；maintainer/operator。
- Why now: all six final结构已经存在，文档不描述过渡态。
- Contract/signature changes: deploy targets/options不变；任何target在release lifecycle前都执行完整all-six preflight；
  `--skip-tests`仅可用于已完成preflight后的重复deploy，不能跳generate/attachments/GPG。
- Input/output and state mapping: source candidate -> local targets/artifacts；publish mode仍single selected
  root/archetype semantics，但Central production instruction只允许`all`。
- Error and edge behavior: any preflight nonzero stops；non-SNAPSHOT/confirm保持；Central UNKNOWN no blind retry；no
  service/Docker/DB。
- Standards impact: `MC-SCOPE-001`,`MC-TEST-001`；no Java。
- Literal rule enforcement: `Rule 5`wrapper只调用existing scripts/Maven；`Rule 11`docs指向normal exact profiles。
- Implementation pseudocode:

```bash
run_preflight() {
  mvnw -N install
  mvnw -N -f archetypes/pom.xml install
  mvnw -f source-projects/pom.xml clean install
  generate_archetypes.sh generate
  generate_archetypes.sh check
  mvnw -f archetypes/pom.xml clean integration-test
  mvnw -Prelease -Dgpg.skip=true clean verify
}
run_preflight || exit
if publish: require non-SNAPSHOT then root -Prelease clean deploy
```

- Verification contribution: `TEST-015`,`016`,`018`,`021`和operational contract。
- After this file: local/docs无法合法绕过source generation。

#### File 4 — `MODIFY .github/workflows/{ci.yaml,ci_java_compatibility.yaml}`

- Purpose: fresh checkout执行normal source/generation/package/consumer/release-shape，并用manifest动态发现六产品。
- Symbols: new ordered steps；replace hardcoded `ARCHETYPE_CONFIGS` array with sorted manifest extraction。
- Repository evidence: compatibility workflow lines 158-269当前硬编码六artifact；CI已有release shape基础。
- Dependencies and consumers: setup-java/cache/current container policy、scripts/manifests/Maven。
- Why now: local contracts和docs已定，可映射相同命令。
- Contract/signature changes: CI job ordering only；Java matrix保持。
- Input/output and state mapping: checkout -> source install -> generated set -> root/package -> each manifest external
  consumer -> release files。
- Error and edge behavior: manifest count mismatch、generated dirty、IT/consumer/shape任何失败job非零；不启动containers/services。
- Standards impact: `MC-TEST-001`future CI proof；config key不改。
- Literal rule enforcement: `Rule 7`existing generated profile tests；`Rule 11`six topology matrix动态但exact。
- Implementation pseudocode:

```yaml
steps:
  - bootstrap parent POMs
  - source-projects clean install
  - generate; check; assert git ls-files .generated empty
  - archetypes clean integration-test
  - for manifest sorted: parse targetArtifactId; generate external sentinel consumer; clean verify
  - root -Prelease -Dgpg.skip=true clean verify
  - assert six main/sources/javadoc contents and no source artifact
```

- Verification contribution: `TEST-001`-`004`,`014`,`015`,`017`,`018`,`021`future CI evidence。
- After this file: ordinary CI covers full no-external-runtime pipeline。

#### File 5 — `MODIFY .github/workflows/publish-maven-central.yml`

- Purpose: 把source/generate/IT/shape/signature completeness放到唯一root deploy之前。
- Symbols: preflight steps、artifact/signature scan、existing target guard/deploy。
- Repository evidence: workflow当前只允许`all`，main/master/confirm/version/secret guards和root `clean verify/deploy`。
- Dependencies and consumers: protected secrets、root Central plugin；source projects不属于root modules。
- Why now: 最后接external write boundary。
- Contract/signature changes: input/target仍all；增加mandatory preflight；真实deploy仍一次。
- Input/output and state mapping: verified source snapshot -> six package attachments + root bundle -> Central；scan
  staging excludes source GAV。
- Error and edge behavior: preflight/shape/signature任一missing stops before deploy；GPG/Portal UNKNOWN logs deployment
  identity and requiresoperator inspect；no automatic retry。
- Standards impact: `MC-DEP-001`,`MC-SCOPE-001`,`MC-TEST-001`；security secrets保持masked，no new action/dependency。
- Literal rule enforcement: `Rule 5`existing actions/Maven only；`Rule 11`root/public module list不变。
- Implementation pseudocode:

```yaml
guard branch/confirm/version/secrets
run exact source bootstrap + source clean install + generate + check + archetypes IT
run root release verify with real GPG enabled
for every six manifest target:
  assert POM/main/sources/javadoc and corresponding asc files
assert no egon-cola-source-* artifact and expected root reactor set
only then: ./mvnw -f pom.xml -Prelease ... clean deploy
on timeout/unknown: fail with operator status-inspection instruction; never loop deploy
```

- Verification contribution: `TEST-016`,`018`和`REQ-011/016`不可达publish证明。
- After this file: protected workflow是完整两阶段Central入口；未实际运行/发布。

- Validation working directory: repository root
- Verification command:
  `bash -n scripts/{test-bump-cola-version.sh,bump_cola_version.sh,maven-deploy.sh,generate_archetypes.sh,test-generate-archetypes.sh} && ./scripts/test-bump-cola-version.sh && ./scripts/test-generate-archetypes.sh release && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install && ./scripts/generate_archetypes.sh generate && ./scripts/generate_archetypes.sh check && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test && ./mvnw -B -ntp -Prelease -Dgpg.skip=true clean verify && git diff --check`
- Expected result: scripts/tests/full source/generator/IT/release shape全exit 0；workflow static order正确；no Central
  write、no GPG validityclaim。
- Failure returns to: File 1/2 version mapping；File 3 local/docs；File 4 CI order/discovery；File 5 publish
  boundary；package regression回Steps 6-8。
- Completion criteria: every explicit requirement有future executable Gate；full local no-write
  proof绿色；真实GPG/Central保留为user-controlled release。
- Rollback: revert Step 9 scripts/docs/workflows；不重放/撤销Central；若已发布只能new patch forward-fix。
- Commit paths: `scripts/test-bump-cola-version.sh`; `scripts/bump_cola_version.sh`;
  `{scripts/maven-deploy.sh,scripts/maven-deploy.md,egon-cola-archetypes/open-source-archetype-code-style.md}`;
  `.github/workflows/{ci.yaml,ci_java_compatibility.yaml}`; `.github/workflows/publish-maven-central.yml`。
- Commit: `ci(archetypes): gate source generation and central release`

## 8. Test, Validation, and Quality Gates

| Gate/order          | Working directory        | Command or method                                                                 | Scope                                 | Expected result                                   | Failure returns to      | Requirements/runtime boundary                         |
|---------------------|--------------------------|-----------------------------------------------------------------------------------|---------------------------------------|---------------------------------------------------|-------------------------|-------------------------------------------------------|
| RED Step 1          | repo root                | `./scripts/test-generate-archetypes.sh unit` before generator                     | shell contract                        | generator-missing expected failure                | Step 1 File 1           | `REQ-003`-`006`; static/temp                          |
| GREEN Step 1        | repo root                | `bash -n ... && ./scripts/test-generate-archetypes.sh unit`                       | parser/safety/atomic                  | exit 0                                            | Step 1 File 2           | same; temp only                                       |
| Source family       | repo root                | Step 2/3 exact direct `clean verify`                                              | 1/6/7-module source projects          | exit 0, current tests pass                        | owning source Step      | `REQ-001`,`009`; Maven/JUnit                          |
| Full source         | repo root                | `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install`    | prerequisites + six sources           | all modules success                               | Steps 2-4               | `REQ-001`,`010`; local repo                           |
| Generator real      | repo root                | `generate`, `check`, `test ... generation`                                        | six manifests/resources/round-trip    | exit 0, double hash/diff clean                    | Steps 1/5/source owner  | `REQ-003`-`009`,`015`; filesystem/Maven               |
| Package family      | repo root                | `test ... package family` + targeted `clean integration-test`                     | Light/Service/Web pair                | wiring/ownership/IT green                         | Steps 6/7/8             | `REQ-002`,`007`-`009`; Archetype IT                   |
| Full package        | repo root                | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test`           | all six + facades                     | reactor success                                   | owning package Step     | `REQ-007`-`009`,`016`; no live infra                  |
| Release shape       | repo root                | `./mvnw -B -ntp -Prelease -Dgpg.skip=true clean verify` + classifier content scan | root full candidate                   | main/sources/javadoc exist; no signatures claimed | Steps 6-9               | `REQ-012`,`013`; local no publish                     |
| Flyway immutability | repo root                | sorted `sha256` baseline/source/generated comparison                              | 12 old archives + source copies       | paths/hash exact; no modify/delete/rename         | source/package owner    | `REQ-015`; static only                                |
| Versioning          | temp fixture             | `./scripts/test-bump-cola-version.sh`                                             | source Egon version/sentinel/rollback | exit 0                                            | Step 9 Files 1/2        | `REQ-014`,`016`; temp only                            |
| Workflow static     | repo root                | `./scripts/test-generate-archetypes.sh release`                                   | CI/publish command order/reachability | deploy after all gates only                       | Step 9 Files 3-5        | `REQ-010`-`012`,`016`; static                         |
| Hygiene             | repo root                | `git diff --check`; `git status --short`; `git ls-files .generated`               | full scoped diff                      | no whitespace/generated/unrelated stage           | owning Step             | `REQ-002`,`005`,`016`; Git                            |
| Protected release   | GitHub Actions / Central | user-confirmed `publish-maven-central.yml`                                        | real GPG and one root bundle          | all `.asc` valid, deployment published            | Step 9/release operator | `REQ-011`,`012`,`016`; external, not run by this Plan |

Focused gate在每Step commit前执行；Step 8后执行full source/generator/package/release-shape；Step 9后重复全部no-write Gate。
真实GPG/Central只在用户操作的protected workflow执行，不能用local `-Dgpg.skip=true`结果替代。

## 9. Migration, Compatibility, Rollout, and Rollback

这是repository source ownership/build migration，不是database schema/data migration：不新增Flyway文件、不执行SQL、无backfill、
dual-read/write、feature flag或服务deploy。existing migration保持原路径/bytes；normal source保存working
copy；package通过generated
copy交付相同classpath path。

Rollout顺序严格为Step 1基础设施、Steps 2-4 normal source、Step 5 all-six generated、Steps 6-8 package cutover、Step 9
release wiring。中间commit可短暂同时存在source与old template，但只有source开始后允许业务编辑；最终merge Gate要求six package
全部不再拥有业务template。public GAV、consumer command、module/API/schema兼容不变。

Central前rollback为path-limited revert owning Step并删除/rebuild `.generated`。任何rollback都不得编辑old
Flyway。若真实Central已
published，不覆盖同版本：恢复仓库后发布更高patch；timeout/UNKNOWN先查询deployment，确认unpublished前不重试。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section         | Steps      | Files                                        | Tests/gates                                        | Completion evidence                                         |
|-------------|--------------------------------|------------|----------------------------------------------|----------------------------------------------------|-------------------------------------------------------------|
| `REQ-001`   | 主 Spec §4                      | 2-4,9      | six source dirs/reactor/workflows            | `TEST-001`-`004`; source clean install             | six normal builds                                           |
| `REQ-002`   | §4                             | 1,2-8,9    | source dirs, ignore, package deletions, docs | package/hygiene scans                              | source-only editable truth                                  |
| `REQ-003`   | §4/§9                          | 1,5        | generator/manifests                          | `TEST-007`/plugin log                              | six 3.4.1 outputs                                           |
| `REQ-004`   | §4/§9                          | 1,5,9      | parser/manifests/CI                          | `TEST-005`, dynamic matrix                         | exact dynamic inventory                                     |
| `REQ-005`   | §4/§8                          | 1,5-8      | ignore/generated/POMs                        | `TEST-011`,`021`                                   | untracked + fail missing                                    |
| `REQ-006`   | §4/§7/§9                       | 1,5        | generator/test/generated                     | `TEST-008`-`010`                                   | no mixed set                                                |
| `REQ-007`   | §4/§8                          | 5-8        | manifests/POMs/curated overlay/verifiers     | `TEST-012`,`014`                                   | metadata/IT stable                                          |
| `REQ-008`   | §4/§14                         | 1,5-8      | generator/verifiers                          | `TEST-009`,`013`,`017`                             | normalized parity                                           |
| `REQ-009`   | §4/§14                         | 2-9        | all source/package/CI                        | `TEST-012`-`014`,`017`                             | six public consumers verify                                 |
| `REQ-010`   | §4/§16                         | 4,9        | source reactor/publish workflow              | `TEST-018`                                         | no source artifact/root module                              |
| `REQ-011`   | §4/§16                         | 9          | deploy scripts/workflow                      | static reachability/`TEST-018`                     | one root deploy after gates                                 |
| `REQ-012`   | §4/§14                         | 6-9        | parent/child POM/javadoc/workflows           | `TEST-015`,`016`,`018`                             | classifiers/signatures                                      |
| `REQ-013`   | §4                             | 6          | archetypes parent                            | effective POM/log                                  | all 3.4.1                                                   |
| `REQ-014`   | §4/§16                         | 9          | bump test/script/docs                        | `TEST-020`                                         | source Egon version only                                    |
| `REQ-015`   | §4/§11/§16                     | 2-8        | source SQL copies/package archives           | `TEST-019`                                         | path/SHA parity                                             |
| `REQ-016`   | §4/§7/§9/§16                   | 1,5-9      | all failures/workflows/runbook               | failure injection/reachability                     | publish impossible on failure                               |
| `REQ-017`   | §4/§14                         | Every Step | commands/workflows                           | command audit                                      | no services/browser/Docker/live infra                       |
| `REQ-018`   | predecessor §4/§14/§16         | 6-9        | package GAV/docs/CI/workflow                 | current routing tests + discovery/release scan     | Open artifacts stay discoverable; master topology unchanged |
| `REQ-019`   | predecessor §4/§11/§14         | 2-9        | source SQL/config/tests and consumers        | tenant uniqueness + no-live-infra generated verify | current data/test boundary preserved                        |
| `REQ-020`   | predecessor §4/§11/§16         | 2-9        | immutable Flyway/source copies/commands      | path/SHA + command audit                           | migrations unchanged; no app/SQL execution                  |
| `REQ-021`   | predecessor §4/§11/§14         | 2-8        | Open manual SQL/Long ID source and consumers | no-UUID/manual-order/ID round-trip tests           | existing Open ID/schema contract preserved                  |
| `REQ-022`   | MyBatis predecessor §4/§11     | 2-8        | migration precondition fixtures              | current migration failure tests                    | unknown history still fails closed                          |
| `REQ-023`   | MyBatis predecessor §4/§6/§10  | 2-8        | dependency/model/source inventories          | dependency/source/reuse scans                      | common-core reuse unchanged                                 |
| `REQ-024`   | MyBatis predecessor §4/§8/§14  | 5-9        | metadata/verifiers/docs/generated trees      | six IT/generated verify/doc audit                  | delivery contracts synchronized                             |
| `REQ-025`   | MyBatis predecessor §4/§7/§14  | 2-8        | all source/consumer business tests           | focused/contract/round-trip regression             | business outcome unchanged                                  |
| `REQ-026`   | MyBatis predecessor §4/§14-§16 | Every Step | commands/workflows                           | command/static audit                               | no project/external infrastructure start                    |

## 11. Risks, Blockers, and User Decisions

| ID             | Risk or decision                                                                             | Impacted Steps/files            | Evidence                                                               | Owner            | Status/action                                                                                          |
|----------------|----------------------------------------------------------------------------------------------|---------------------------------|------------------------------------------------------------------------|------------------|--------------------------------------------------------------------------------------------------------|
| `RISK-001`     | create-from-project对multi-module `${artifactId}`/`${rootArtifactId}`处理与current curated POM不同 | Steps 1/5, multi-source/package | local diagnostic已见自动metadata/module POM差异                              | Maintainer       | Closed by manifest-derived custom property + explicit POM/path normalize + round-trip; mismatch blocks |
| `RISK-002`     | `.gitignore`被Plugin默认排除                                                                      | Steps 1/5                       | local diagnostic output缺`.gitignore`，current template使用`__gitignore__` | Maintainer       | Closed：explicit source copy -> `__gitignore__` + consumer check                                        |
| `RISK-003`     | source同版本Egon依赖在fresh repo不可解析                                                               | Steps 2-4/9                     | current BOM/components/platforms topology                              | Build owner      | Closed：two parent `-N install` + internal source reactor                                               |
| `RISK-004`     | source+full IT增加CI时间                                                                         | Step 9 workflows                | six full products/current heavy tests                                  | CI owner         | Mitigated：先保留完整Gate并记录duration；超时后仅拆job，不skip correctness                                              |
| `RISK-005`     | old Flyway archive与source working copy双份漂移                                                   | Steps 2-8                       | AGENTS immutable rule + 12 current files                               | Maintainer       | Closed by exact path/hash Gate；future migration source-only                                            |
| `RISK-006`     | maven-source-plugin未包含external generated resources                                           | Steps 6-8                       | current plugin behavior未在final wiring验证                                | Build owner      | Closed by release-shape content test；若失败仅调整existing Source/Jar config，不改Spec/依赖                        |
| `RISK-007`     | Central/GPG真实环境不可由local Plan证明                                                               | Step 9 workflow                 | secrets/Portal external                                                | Release operator | Accepted external proof boundary；protected workflow required                                           |
| `DECISION-001` | Plan尚未获用户批准执行                                                                                | all Steps                       | primary Spec获Plan授权，但Plan status Review                                | User             | Plan内容无阻塞；交付后等待用户审查，批准前不使用executing-plan                                                               |

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

正常代码结构由Steps 2-4建立；生成Archetype由Steps 1/5完成；生成物通过stable package modules与root Central workflow发布由
Steps 6-9完成。Plan没有把source project发布为新GAV，也没有退回resources日常开发。

### 12.2 Spec consistency

Plan保持六GAV、module/API/model/schema/config与root single-bundle设计；existing Flyway没有任何modify/rename/delete操作。
四个Plan Clarification都是internal sentinel/topology/test fixture/local wrapper推导，不增加public交互、业务行为或architecture。
Simplicity审计未发现fetch-then-forward、caller-supplied derivable context、重复model/mapper或speculative cache/layer。

### 12.3 Repository executability

所有current paths/counts/scripts/POM versions/workflow entry均在`main@28b9e596d`核对；每Step具有baseline/end state、file
order、
pseudocode、exact working directory/command/failure return/rollback/commit paths。大量Java以family
tree为一个机械materialization
file group，与旧Plan同样使用path patterns，实施者不需选择业务architecture。

### 12.4 Test and release completeness

generator/版本/ownership行为test-first；纯source materialization以current public artifact+existing tests作为evidence-backed
N/A。
focused -> source -> generation -> family package -> full IT -> release shape -> protected signing/Central顺序闭合。local
proof不冒充live
GPG/Central；无database/runtime/browser/Docker动作。

### 12.5 Blocking Manual Check

| Check ID         | Applicability    | Status | Evidence                                                        | Finding                                                                          | Required action/exception          |
|------------------|------------------|--------|-----------------------------------------------------------------|----------------------------------------------------------------------------------|------------------------------------|
| `MC-ARCH-001`    | `Applicable`     | `PASS` | §4.7、six current metadata/verifier、Steps 2-8                    | exact Light/Service/Web/Open profiles preserved                                  | None                               |
| `MC-REUSE-001`   | `Applicable`     | `PASS` | §4.7 capability ledger；existing Plugin/Jar/Source/GPG/Groovy    | all existing capabilities reused before shell gap                                | None                               |
| `MC-DEP-001`     | `Applicable`     | `PASS` | parent POM/current pluginManagement；§4.7                        | no new dependency；only extension alignment                                       | None                               |
| `MC-NAME-001`    | `Not applicable` | `N/A`  | Rule 1 matrix；source is current consumer Java inventory         | no type create/rename/content change                                             | None                               |
| `MC-VALID-001`   | `Not applicable` | `N/A`  | Rule 2 matrix；no handoff change                                 | Validation contracts copied unchanged                                            | None                               |
| `MC-MODEL-001`   | `Not applicable` | `N/A`  | Rule 3 matrix；no object change                                  | record/Lombok construction unchanged                                             | None                               |
| `MC-CONVERT-001` | `Not applicable` | `N/A`  | Rule 3 matrix；no converter change                               | MapStruct/BaseConverter unchanged                                                | None                               |
| `MC-LOG-001`     | `Not applicable` | `N/A`  | Rule 4 matrix；no business class change                          | existing logging unchanged                                                       | None                               |
| `MC-BEAN-001`    | `Not applicable` | `N/A`  | Rule 4 matrix；no Spring Bean change                             | Bean names/Qualifier/constructors unchanged                                      | None                               |
| `MC-UTIL-001`    | `Applicable`     | `PASS` | Steps 1/9 pseudocode；capability ledger                          | JDK/POSIX + existing Maven only                                                  | None                               |
| `MC-JSON-001`    | `Not applicable` | `N/A`  | Rule 6 matrix；no external object change                         | Jackson contracts unchanged                                                      | None                               |
| `MC-TIME-001`    | `Not applicable` | `N/A`  | Rule 10 matrix；no time symbol change                            | java.time inventory preserved                                                    | None                               |
| `MC-CONFIG-001`  | `Applicable`     | `PASS` | Rule 7 matrix；Steps 2-8 profile parity                          | all environment files migrate/generate together                                  | None                               |
| `MC-PATTERN-001` | `Not applicable` | `N/A`  | no business logic change；main Spec §13                          | build flow uses selected manifest/atomic pattern；Rule 9 business scope untouched | None                               |
| `MC-SCOPE-001`   | `Applicable`     | `PASS` | §5 tree、§6.1 dirty boundaries、per-Step commit paths             | no unrelated platform path or business redesign                                  | None                               |
| `MC-TEST-001`    | `Applicable`     | `PASS` | §7 exact commands、§8 gates、§10 traceability                     | all standards/requirements have executable future proof                          | None                               |
| `MC-BLOCKER-001` | `Applicable`     | `PASS` | §11 only Plan approval Gate remains；all technical rows PASS/N/A | no unresolved Spec/technical decision blocks review                              | User must approve before execution |

### 12.6 Final verdict

`PASS — Ready for user review`

本 verdict只表示Plan内部完整并可供审查；主 Spec和Plan仍为`Review`，不得据此开始实现、commit或Central发布。
