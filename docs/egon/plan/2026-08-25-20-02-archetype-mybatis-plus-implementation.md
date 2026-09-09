# Egon-COLA Archetype MyBatis-Plus 公共基线统一实施 Plan

| Field | Value |
| --- | --- |
| Document | `2026-08-25-20-02-archetype-mybatis-plus-implementation.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-08-25 20:02 CST` |
| Updated | `2026-08-25 22:28 CST` |
| Owner | `Mario / Egon-COLA maintainers` |
| Repository | `Egon-COLA` |
| Scope | `Common MyBatis-Plus Starter；organization/evaluation canonical facade；light/service/web 与三个 -open Maven Archetype 模板、SQL、配置、测试、metadata、verifier 和文档` |
| Source Requirement | `用户批准 2026-08-25 Archetype MyBatis-Plus 统一 Spec，并要求按 egon-coding-writing-plan 生成可执行文件级 Plan` |
| Baseline Revision | `main@3be897e5cb4781890bfbac3104512e6e73bb943a；2026-08-25 20:02 CST dirty-worktree snapshot` |
| Implements Spec | [Egon-COLA Archetype MyBatis-Plus 与公共基线统一重构设计](../spec/2026-08-25-19-09-archetype-mybatis-plus-unification.md) |
| Spec Status | `Review` |
| Spec Revision | `Updated 2026-08-25 22:28 CST；用户于 2026-08-25 要求检查并修复阻断项；此前执行确认不自动延伸为修订后 Plan 的继续执行批准` |
| Effective Specs | [Egon-COLA Archetype MyBatis-Plus 与公共基线统一重构设计](../spec/2026-08-25-19-09-archetype-mybatis-plus-unification.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | [Open-Source Archetype Family Implementation Plan](2026-08-23-19-14-open-source-archetype-implementation.md)（历史已实现基线，只作现状证据，不执行其步骤） |

## 1. Summary

本 Plan 实施唯一目标 Spec，将 Common Starter、两个 canonical facade 和六个 Maven Archetype 模板统一到 MyBatis-Plus/Common MP Starter、Long/Snowflake、`EgonModel/EgonColaMapper/EgonColaIService/EgonColaServiceImpl`、tenant 路由与统一校验/转换合同。依赖顺序为 Common 扩展点先行，随后发布 Java facade Long 合同，再按 Light、Service、Web 的 legacy/open 六个生成项目逐一完成可独立验证的垂直迁移，最后关闭跨族文档与发布门禁。

共 9 个实现 Step，每个 Step 对应一个 path-limited semantic commit。最终证据是 Common focused tests、两个 facade contract tests、六个 archetype integration-test、六个 generated project `clean verify`、禁止词/配置/迁移 checksum 静态门禁和根 archetype reactor `clean integration-test`。Plan 本身不修改实现、执行 SQL 或启动服务。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path：[Egon-COLA Archetype MyBatis-Plus 与公共基线统一重构设计](../spec/2026-08-25-19-09-archetype-mybatis-plus-unification.md)
- Status：文件 metadata 仍为 `Review`。
- Revision：`Updated 2026-08-25 22:28 CST`，baseline `main@3be897e5cb4781890bfbac3104512e6e73bb943a`。
- Approval evidence：用户此前确认从 Spec 进入 Plan并批准执行；Step 1 编译发现 `ModelBuilder` 阻断后，本轮用户显式调用 Spec/Plan skills要求修复阻断。修订后 Plan 保持 `Review`，不在本轮继续实现。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary and consolidated effective contract | [MyBatis-Plus unification](../spec/2026-08-25-19-09-archetype-mybatis-plus-unification.md) | `Review` / 2026-08-25 22:28；本轮修复阻断 | 全文 `REQ-001`-`REQ-026`、`DEC-001`-`DEC-007`、21 表、46 tests、Manual Checks | 唯一执行需求命名空间；已把下列历史设计的适用部分、修订和冲突收敛为一份当前合同 |
| Incorporated accepted dependency | [Common MyBatis-Plus Starter](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md) | `Accepted` / 2026-08-21 14:50 | 现有 Starter API、57-method parity、TenantLine、MetaFill、Validation；primary 仅修订 ServiceImpl constructor seam，并明确 EgonModel 不增加 Lombok builder | 证明复用能力；不重新执行已完成的 Starter 创建工作，也不复活已漂移的 `AbstractModel` 文本 |
| Incorporated amended predecessor | [Open-source archetype family](../spec/2026-08-23-16-43-open-source-archetype-family.md) | `Review` / 2026-08-23 19:42；实现已存在 | Open 模块、Protobuf/Triple、manual SQL、外部 Yuheng、生成验证边界；持久依赖/Service/PO/DAO/tenant 合同由 primary 覆盖 | 保留 Open 的非持久化产品边界，不执行旧 Plan |
| Living architecture context | `egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md`；`egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md`；`egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md` | repository living docs | Light 单模块；Service/Web 七模块；domain-first bounded context、入口边界 | 决定文件归属和依赖方向；相关章节由 primary 明确修订 |
| Code-style dependency | `egon-cola-archetypes/open-source-archetype-code-style.md` | repository guide | 除 domain/MP 依赖这一行外的 package/module ownership、mapper safety、manual SQL review | 约束 Open 代码；旧的 domain 禁 MP 条目被用户 `DEC-001/REQ-003` 明确覆盖 |

Header 的 `Effective Specs` 只链接 primary，因为它是当前唯一无歧义的 `REQ-*` 命名空间并已经合并上述适用合同；把三个带重复 `REQ-001` 编号的历史文档再次作为独立执行 Spec 会制造错误的需求碰撞。上表仍完整记录所有被读取、继承或覆盖的规范来源。

### 2.3 Superseded or excluded content

- Open predecessor 中“原三个 archetype 不变”、直连 MyBatis-Plus `3.5.17`、domain 不依赖 MP、local Mapper/RepositoryImpl 持久链，被 primary `REQ-001`-`REQ-006` 与 `DEC-001` 覆盖。
- Open code-style 中 `domain | Must not depend on MyBatis-Plus` 这一格被用户明确批准的 domain Common MP Starter 依赖覆盖；其他模块归属/依赖方向继续有效。
- Accepted Common Spec 的 Starter 创建、57 方法和自动装配已完成，不形成重复实现 Step；当前源码真实继承外部 MP `Model`。Step 1 编译证明该父类没有 `ModelBuilder`，因此 primary `EVD-017/REQ-008` 要求 EgonModel 不增加 Builder/SuperBuilder 并保持现有 AR parity。
- 历史 Open Plan 已实施，不是本 Plan 的前置执行步骤；本 Plan 从当前 `main@3be897e5` 模板状态继续。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | [Spec](../spec/2026-08-25-19-09-archetype-mybatis-plus-unification.md) §4 | 六模板清除 JPA/Hibernate persistence 合同 | source/dependency scan 无生产 JPA/Hibernate | 六模板 POM、Java、tests、docs、verifier |
| `REQ-002` | §4、§6 | 仅通过 Common MP Starter 使用 MP；Lombok 对齐 `1.18.46` | 无 direct MP/version `3.5.17`；六模板无 `1.18.38` | root/domain/infra POM、lombok.config |
| `REQ-003` | §4、§8.4 | domain `service` 只保留泛型 Egon business Service；非持久 port 移语义包 | 12 逻辑 Service、24 sibling interfaces；ArchUnit/verify 通过 | domain 服务和 port package |
| `REQ-004` | §4、§8.4 | infrastructure ServiceImpl 绑定 DAO/PO | 24 sibling impl 继承/实现精确泛型且 Bean 可注入 | infra service.impl |
| `REQ-005` | §4、§10.3.4 | DAO 位于 infra `repo.dao` 并继承 `EgonColaMapper` | 42 sibling DAO sources；XML namespace 精确 | DAO、Mapper XML、MapperScan |
| `REQ-006` | §4、§10-§11 | 21 逻辑 PO、42 sibling sources 继承 `EgonModel` | `*PO` 命名、泛型、表映射和反射检查通过 | PO、package-info、schema |
| `REQ-007` | §4、§10.3.3-§10.3.4 | PO 使用用户确认的五个 Lombok 注解、普通 Builder 与 MP annotations；无 RequiredArgs/SuperBuilder；builder 仅含业务字段 | delombok/compile/builder/annotation tests | 42 PO sources、六 lombok configs |
| `REQ-008` | §4、§10.3 | EgonModel 不增加 Lombok builder/constructor 注解且七字段/JavaBean/AR不变 | no-ModelBuilder compile fixture、source scan 与 AR parity 通过 | Common EgonModel/tests |
| `REQ-009` | §4、§7、§9 | EgonColaServiceImpl 删除手写 protected constructor，子类 Lombok 注入 | 无 handwritten ctor；57 方法行为相同 | Common base/test subclasses、24 impl |
| `REQ-010` | §4、§6.2 | 具体业务 Bean 有 Slf4j、stable name、final Qualified fields、RequiredArgs | context/compile/log scan 通过 | ServiceImpl 与受影响业务 Bean |
| `REQ-011` | §4、§10.4 | MapStruct/MapStructPlus converter 实现 `BaseConverter` | 无业务 Service 手写 copy；双向/list/null tests | PO/application/adapter converters |
| `REQ-012` | §4、§9-§10 | 所有受影响层 handoff 使用 Validation/groups | invalid 输入在 owning boundary/JDBC 前失败 | DTO/Command/Query/Service/DAO/tests |
| `REQ-013` | §4、§9.3、§10.3.2 | Java/domain/RPC/PO/DB business ID 全为 positive Long/Snowflake | 无 UUID/String business ID；facade API Long | facade、六模板、DDL |
| `REQ-014` | §4、§9.3 | HTTP/GraphQL ID 是 canonical decimal string | >2^53 无精度丢失；非法 lexical form 拒绝 | Jackson/GraphQL/controller contracts |
| `REQ-015` | §4、§7.3 | tenant/user 来自 trusted context，异步显式传播，缺失 fail-closed | 伪造/缺失/线程清理测试通过 | filters/context/events/Provider integration |
| `REQ-016` | §4、§10.3.1、§11 | 所有表具有 EgonModel 七字段 | 29 physical table shapes parity；NOT NULL/default exact | PO、SQL、schema tests |
| `REQ-017` | §4、§11 | 已分片表以 positive tenant_id 做 database/table route | 六套 YAML 同 key；父子同 node | Sharding YAML/algorithm/tests |
| `REQ-018` | §4、§11 | master_data 保持 singleton 且 SQL tenant-scoped | none strategy/single node 与 TenantLine 共存 | Sharding YAML、SQL/tests |
| `REQ-019` | §4、§11 | business/link uniqueness tenant-scoped 并兼容 logic delete | partial unique/link index 与 DAO SQL 一致 | SQL/index/tenant tests |
| `REQ-020` | §4、§11、§16 | legacy 旧 Flyway immutable，每个独立 location 一个新 migration | 6 new migrations；旧 checksum 不变 | light/service/web legacy SQL |
| `REQ-021` | §4、§11、§16 | Open 每 location 一个新 manual sequence，无 Flyway | 6 new `003/004` scripts + README | three Open SQL/manual tests |
| `REQ-022` | §4、§11、§16 | unknown UUID/tenant history 不猜测 | preflight fixture aborts；offline mapping boundary documented | migration guards/tests/runbooks |
| `REQ-023` | §4、§6.1、§10 | common-core 能力逐项复用，语义不符不机械替换 | reuse/source ledger；无 duplicate helper | POM/converters/validation/page/result |
| `REQ-024` | §4、§8、§14 | metadata/verifier/docs 与真实模板同步 | 六 archetype IT + generated verify | metadata、verify、README/living docs |
| `REQ-025` | §4、§7、§14 | 除明确 ID/tenant/persistence delta 外保持业务行为 | state/cache/event/transaction regression 通过 | all six vertical slices |
| `REQ-026` | §4、§14-§16 | 不自动启动项目或外部基础设施 | execution command audit 无 start/Docker/live DB | 所有 Step validation boundary |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先保持 Common `EgonModel` 源码不变，修改 `EgonColaServiceImpl` 和 compile fixtures，使所有后续 PO 使用业务字段 `@Builder`、ServiceImpl 使用同一个 Lombok constructor seam；Common test 在任何模板 POM 切换前 GREEN。
2. 再修改 organization/evaluation canonical facade 的 Long contract，使 legacy Service/Web adapter/application 能针对唯一 Java API 编译；HTTP/GraphQL wire 仍由模板保持 decimal string。
3. 六个 archetype 分别作为垂直 RED/GREEN slice：测试/生成 contract 先定义缺失行为，然后一次性修改该模板的 POM、domain/boundary、PO/DAO/ServiceImpl/XML、schema/config 和 metadata/docs，避免提交不能编译的 JPA/MP hybrid。
4. legacy 每族新增 master/shard 两个独立 Flyway history 文件；Open 每族新增 manual `003/004`，绝不改旧脚本。
5. 最后只同步跨族 code-style/release proof，执行完整 reactor 与六个 generated project gate；不启动服务或连接真实基础设施。

### 4.2 Test-first strategy

| Behavior | RED first | Expected RED reason | Minimum GREEN |
| --- | --- | --- | --- |
| Common Builder/Lombok extension | Common builder/compile/source/parity tests | 已记录的错误分支因 MP `Model` 无 `ModelBuilder` 而失败；修正分支的 TestBusinessService 仍需 handwritten super ctor | PO普通Builder只含业务字段；EgonModel无builder改造；base abstract collaborator getters + Lombok fixture |
| Canonical Long API | two facade contract tests | method/record reflection still reports String IDs/NotBlank | change only identity components/parameters to Long + Positive/NotNull |
| Each archetype persistence slice | existing repository/schema/route/architecture tests plus verifier assertions | legacy finds JPA/UUID；Open finds direct MP/old PO/mapper/service contract | one family-specific Common MP vertical migration |
| Migration safety | schema/manual support test before new file | expected new version absent or seven-column/tenant parity fails | one new file per independent location |
| Cross-family generation | verifier mutation/static contract | required/forbidden inventory mismatches before family GREEN | synchronized metadata/verifier/docs and full reactor |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | Common Starter main/test/docs | publishes compile base for every template |
| Step 2 | Step 1 | None | canonical facade sources/tests | Service/Web consumers compile against one Long contract |
| Step 3 | Steps 1-2 | Step 4 only after Step 3 contract comparison, but execution default sequential | Light legacy tree | same business family comparison; no shared write paths |
| Step 4 | Steps 1-3 | None | Light Open tree | preserves canonical/open parity and consumes settled Light decisions |
| Step 5 | Steps 1-2 | Step 7 in isolated workers only | Service legacy tree | evaluation facade prerequisite; no Web files |
| Step 6 | Steps 1-2, Step 5 parity result | None | Service Open tree | local Proto stays wire-compatible with legacy Long semantics |
| Step 7 | Steps 1-2 | Step 5 in isolated workers only | Web legacy tree | organization facade prerequisite; no Service files |
| Step 8 | Steps 1-2, Step 7 parity result | None | Web Open tree | local Proto/HTTP/GraphQL parity closes after legacy reference |
| Step 9 | Steps 1-8 | None | cross-family docs/gates only | final assertions require all six templates GREEN |

Implementation execution is sequential by default because AGENTS.md requires one commit per task and the final executor must review each commit. Only Steps 5 and 7 may be delegated in parallel with non-overlapping trees; their agents must not touch shared facades, Common, primary Spec, Plan or cross-family code-style.

### 4.4 Commit boundaries

Nine Steps produce nine semantic commits. Each archetype Step is intentionally a vertical commit: separating its POM from PO/DAO/Service/schema would leave an unbuildable mixed stack. `git add` must enumerate only the Step paths; current unrelated dirty docs never enter staging. Generated `target/**` is validation output and never committed.

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| Common MP Starter | Keep, §7.0 | existing Starter with 57 methods/TenantLine/MetaFill | direct MP/JPA | one dependency/version; no new runtime call | Reuse existing |
| EgonModel SuperBuilder | Reject, §7.0/§10.3 | `EVD-017`证明外部 MP `Model` 无 `ModelBuilder` | plain PO Builder刻意排除framework-owned技术字段 | no new ABI；compile/source/parity tests | Keep EgonModel unchanged in Step 1 |
| Service base abstract accessors | Add, §7.0/§9.2 | handwritten protected three-arg ctor blocks Lombok subclasses | Lombok on current final base fields cannot generate subclass super call | three protected seams; no network/state | Implement Step 1 |
| Generic domain Service | Add, user DEC-001/§8.4 | concrete PO cannot be imported into domain without module cycle | repository port keeps domain pure but contradicts user | 12 logical interfaces, inherited CRUD surface | Implement Steps 3-8 |
| DAO/PO/ServiceImpl | Merge/replace, §7.0 | JPA repos or local BaseMapper + repository forwarding | keep forwarding layer | fewer hops; XML/DI migration | Implement Steps 3-8 |
| Link-table DomainService | Reject, §8.4 | link PO has no independent business use case | ceremonial Service | extra API/Bean/test only | Do not create |
| Trusted tenant request field/API | Reject, §7.0 | provider/context already authoritative | fetch/body forward | added spoofable state/failure | Do not create |
| Master-data full sharding | Reject, §7.0/ASM-003 | current topology separates singleton master | shard everything | cross-DB move/ops | Preserve singleton |
| Tenant database/table Strategy | Keep/adapt, §13 | existing `SnowflakeLongShardingAlgorithm/ShardingNodeMap` | hard-coded modulo | config/test only | Reuse with tenant key |
| New pattern classes for CRUD | Reject, §13.2 | MP Template Method and Sharding Strategy cover variations | Factory/Chain | class explosion | Direct CRUD under selected patterns |
| New utilities/converter hierarchy | Reject, §6.1/§10.4 | ValidationUtils/BaseConverter/MapStruct exist | local helpers/manual copy | drift/security cost | Reuse existing |
| New tables | Reject, §11 | all 21 logical tables exist | create parallel tables | migration/data duplication | Migrate existing only |
| Generic update-time index | Reject except two evidenced pages, §11.0 | only Course/Score have matching page SQL | index every table | write amplification | Add only exact two indexes |
| Frontend pages | N/A, §12 | no frontend source in six templates | invent UI | new project/state/API | No files |

No fetch-then-forward API、caller-supplied tenant、speculative cache/layer/table/pattern or duplicate converter remains。`EVD-017` 已触发并完成一次 return-to-Spec 修订：SuperBuilder方案被拒绝，未新增自定义builder Adapter/父类。Audit verdict：修订后所有planned elements均为present needs或显式migration，无剩余return-to-Spec blocker。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Common builder/service seam | `REQ-007`-`010` | business-only builder + forbidden-SuperBuilder + Lombok subclass compile tests | existing Starter | corrected PO construction and Service subclass pattern | six templates | Step 1 |
| Canonical Long facade | `REQ-012`-`014`,`025` | facade reflection/serialization tests | Step 1 only for shared build reactor | Long Java API | legacy Service/Web adapters | Step 2 |
| Light legacy vertical slice | `REQ-001`-`020`,`022`-`026` | Light repo/schema/route/verifier | Steps 1-2 | JPA-free Light template | Light Open parity | Step 3 |
| Light Open vertical slice | `REQ-001`-`019`,`021`-`026` | OpenArchitecture/manual schema/verifier | Steps 1-3 | Common MP Light Open | final family gate | Step 4 |
| Service legacy vertical slice | `REQ-001`-`020`,`022`-`026` | repo/page/schema/route/verifier | Steps 1-2 | JPA-free evaluation template | Service Open parity | Step 5 |
| Service Open vertical slice | `REQ-001`-`019`,`021`-`026` | identity/proto/manual/schema/verifier | Steps 1-2,5 | Common MP Service Open | final family gate | Step 6 |
| Web legacy vertical slice | `REQ-001`-`020`,`022`-`026` | HTTP/GraphQL/repo/schema/verifier | Steps 1-2 | JPA-free organization template | Web Open parity | Step 7 |
| Web Open vertical slice | `REQ-001`-`019`,`021`-`026` | HTTP/GraphQL/proto/manual/verifier | Steps 1-2,7 | Common MP Web Open | final family gate | Step 8 |
| Cross-family release contract | `REQ-001`,`002`,`020`-`026` | full reactor/generated verify/static scans | Steps 1-8 | reviewable atomic release evidence | implementation handoff | Step 9 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | Light single-project package tree；Service/Web exact seven-module trees；six metadata/verifiers | exact Egon-COLA Archetype with user-approved domain Starter dependency | no new module/layer；impl/DAO/PO infra，interfaces domain | Steps 3-9；`MC-ARCH-001` |
| Reuse/capability | Common MP Starter、common-core BaseConverter/ValidationUtils/PageQuery、open NodeMap | reuse all; direct JPA/MP removed | no local base service/mapper/interceptor/utils | Steps 1,3-9；`MC-REUSE-001`,`MC-DEP-001` |
| Naming/model/validation/conversion | existing PO/Mapper/JPA/Converter inventories and six validation suites | `*PO/*DAO/*DomainServiceImpl`；layer Validation；MapStruct BaseConverter | rename `*Po/*Mapper`; exact groups/annotations/mapping | Steps 2-8；`MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001` |
| Bean/logging/util/JSON/time/config | six lombok.config copy Qualifier；Boot Jackson；Instant/LocalDateTime；4 profiles each | stable names/RequiredArgs/Qualifier/Slf4j；Jackson decimal IDs；java.time；key parity | concrete Bean annotations and profile/static gates | Steps 1-9；`MC-LOG-001`,`MC-BEAN-001`,`MC-UTIL-001`,`MC-JSON-001`,`MC-TIME-001`,`MC-CONFIG-001` |
| Business variation/pattern | Common Service Template Method；NodeMap Strategy；current domain state methods | keep those two approved patterns；simple CRUD direct | no factory/chain; route and 57-method tests prove variation | Steps 1,3-8；`MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| MP CRUD/tenant/audit | Spring JDBC、official MP、Common MP Starter | `egon-cola-component-common-mybatis-plus-spring-boot-starter` | Common fully fits | Reuse Common | None | Steps 1,3-8；`MC-REUSE-001` |
| Validation | native Jakarta、Boot Validation、common ValidationUtils/model interceptor | common-core + Starter sources | fits handoff/manual/automatic paths | Reuse | None | Steps 2-8；`MC-VALID-001` |
| Conversion | BeanUtils/manual、MapStruct、BaseConverter | common-core `BaseConverter<S,T>` and existing converters | MapStruct + Base fits | Reuse/rename | None | Steps 3-8；`MC-CONVERT-001` |
| ID | UUID、DB sequence、Common ID/MP ASSIGN_ID | Common ID starter + EgonModel TableId | fits positive Long | Reuse | None | Steps 2-8；`MC-DEP-001` |
| Pagination | local page pairs、MP Page、common PageQuery | existing Course/Score page paths | PageQuery fits input；domain result stays local where wire differs | Selective reuse | None | Steps 5-6；`MC-REUSE-001` |
| Routing | UUID algorithm、new modulo、open SnowflakeLong algorithm/NodeMap | six sharding config/algorithm tests | existing Strategy fits after key=tenantId | Reuse Open implementation in legacy | No new library | Steps 3-8；`MC-PATTERN-001` |
| Architecture enforcement | verify.groovy、Open ArchUnit | six verifiers; Open ArchUnit 1.4.2 | legacy needs same test-only proof or equivalent generated verifier | Reuse repository test dependency where needed | ArchUnit test dependency only where legacy generated project lacks it; version copied from Open | Steps 3,5,7；`MC-DEP-001` |
| JSON/time | Boot Jackson、Gson/Fastjson；java.time vs util Date | current POM/DTO/time fields | Boot Jackson/java.time fit | Reuse | None | Steps 2-8；`MC-JSON-001`,`MC-TIME-001` |
| Utilities | JDK/Commons/Guava/current helpers | POMs/import scans | no capability gap | JDK/current approved only | None | All；`MC-UTIL-001` |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Spec §6.2/§8/§10 | 42 PO/DAO sources、12 Service pairs、DTO/VO/Command/Query/Event | tests -> `*PO/*DAO/*DomainService*` -> consumers -> verifier | exact semantic suffix and rename consumers | type inventory/compile/forbidden names | Steps 2-9 | PASS |
| Rule 2 | Spec §6.2/§7/§9/§10 | controllers/facades/application validators/domain services/model interceptor | boundary tests -> record constraints/groups -> Service/DAO guards | `@Valid/@Validated/@NotNull/@Positive`、groups、ValidationUtils；no phone field | positive/negative/group/JDBC-zero tests | Steps 2-8 | PASS |
| Rule 3 | Spec §6.2/§10；latest user PO override + `EVD-017` | records、MapStruct converters、BaseConverter；PO inheritance | builder/mapper tests -> unchanged EgonModel -> PO -> converters | records compact only for normalization；PO exact user-approved Data/NoArgs/AllArgs/Builder/Accessors and no RequiredArgs/SuperBuilder；builder仅业务字段；BaseConverter mandatory | delombok/compile/mapping tests | Steps 1-8 | PASS — compiler evidence closes SuperBuilder option |
| Rule 4 | Spec §6.2/§7/§13 | six lombok.config already copy Qualifier；business Bean inventory | wiring tests -> lombok.config -> ServiceImpl/handlers | Slf4j、explicit name、final Qualified fields、RequiredArgs；PO excluded | context/constructor/log scan | Steps 1,3-8 | PASS |
| Rule 5 | Spec §6.2/§15 | current JDK/Commons/Guava dependencies | POM first -> Java imports -> dependency scan | no new Utils/non-allowlist library | dependency/import search | Steps 1-9 | PASS |
| Rule 6 | Spec §6.2/§9/§10 | HTTP/GraphQL/MQ facade DTOs use Boot Jackson | contract tests -> records/VO/Event -> adapters | Long IDs decimal strings where JSON consumers；no JSON conversion | MockMvc/GraphQL/Jackson tests | Steps 2-8 | PASS |
| Rule 7 | Spec §6.2/§15 | base/dev/test/prod plus two sharding YAML per family | parity test -> all four profiles -> sharding configs | identical core keys，values may differ | YAML key-set comparison/config binding | Steps 3-8 | PASS |
| Rule 9 | Spec §6.2/§13 | Service base Template Method、Sharding Strategy | pattern tests -> Common base/algorithm -> ServiceImpl/config | no hard-coded route/switch；simple CRUD no ceremonial pattern | 57 parity + route same-node tests | Steps 1,3-8 | PASS |
| Rule 10 | Spec §6.2/§10/§11 | EgonModel Instant、business schedule Instant/LocalDateTime、TIMESTAMPTZ | schema/time tests -> PO/converter/DTO/SQL | UTC Instant audit；local timetable semantics unchanged；no util Date | time round-trip + forbidden import scan | Steps 1-8 | PASS |
| Rule 11 | Spec §6.1/§6.2/§8 | exact Light/Service/Web/Open trees、metadata/verifier | every Step remains inside selected module/package tree | no third/hybrid layer；only approved domain MP dependency exception | ArchUnit/verify/generated tree | Every Step | PASS |

## 5. Change File Tree

有限花括号只压缩完全枚举的 sibling 路径，例如 `{light,light-open}` 恰好表示两个列名值，不是 glob；每个 Step 的 File blocks 再给出 symbols、操作和消费者。执行时必须把花括号展开为 path-limited staging 清单。

```text
egon-cola-components/egon-cola-component-common/
└── egon-cola-component-common-mybatis-plus-spring-boot-starter/
    ├── src/main/java/.../model/EgonModel.java                              VERIFY UNCHANGED Step 1
    ├── src/main/java/.../extension/EgonColaServiceImpl.java                MODIFY Step 1
    ├── src/test/java/.../{model,extension,support,contract}/**             MODIFY Step 1
    └── README{,.zh-CN}.md                                                  MODIFY Step 1

egon-cola-archetypes/
├── egon-cola-{organization,evaluation}-facade/src/{main,test}/java/**      MODIFY Step 2
├── egon-cola-archetype-{light,light-open}/**                               MODIFY/CREATE/DELETE Step 3/4
├── egon-cola-archetype-{service,service-open}/**                           MODIFY/CREATE/DELETE/RENAME Step 5/6
├── egon-cola-archetype-{web,web-open}/**                                   MODIFY/CREATE/DELETE Step 7/8
└── open-source-archetype-code-style.md                                     MODIFY Step 9
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| VERIFY UNCHANGED | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java` | seven fields + AR, no Lombok builder | same bytes as baseline；no Lombok type annotations；fields/AR unchanged | framework-owned technical state | 1 | `REQ-007`,`008` | source diff + Common compile/parity tests |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java` | final collaborators + handwritten ctor | abstract no-arg Template Method accessors | Lombok subclass seam | 1 | `REQ-009`,`010` | Service compile/57 tests |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/{model/EgonModelTest.java,contract/EgonModelActiveRecordParityTest.java,extension/EgonColaServiceImplTest.java,support/TestBusinessModel.java,support/TestBusinessService.java}` | current manual fixtures | RED/GREEN builder/constructor/parity proof | focused Common proof | 1 | `REQ-007`-`010` | targeted Maven test |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README{,.zh-CN}.md` | old handwritten subclass constructor examples | Lombok-only extension、business-only Builder usage、SuperBuilder prohibition | public migration docs | 1 | `REQ-007`-`009`,`023` | doc/source scan |
| MODIFY | `egon-cola-archetypes/egon-cola-{organization,evaluation}-facade/src/main/java/**` | String business identity | positive Long identity; code/status/text Strings unchanged | canonical Java contract | 2 | `REQ-012`-`014`,`025` | facade contract tests |
| MODIFY | `egon-cola-archetypes/egon-cola-{organization,evaluation}-facade/src/test/java/**ContractTest.java` | String signatures asserted | Long/validation/time/serialization reflection | RED/GREEN facade gate | 2 | `REQ-012`-`014` | targeted facade tests |
| MODIFY/CREATE/DELETE | `egon-cola-archetypes/egon-cola-archetype-light/src/**` | JPA/UUID/old service ports/schema | Common MP Light template + new V20260825 files | Light legacy vertical slice | 3 | `REQ-001`-`020`,`022`-`026` | Light IT/generated verify |
| MODIFY/CREATE/DELETE | `egon-cola-archetypes/egon-cola-archetype-light-open/src/**` | direct MP 3.5.17/local Mapper/repository chain | Common MP Light Open + new manual 003/004 | Light Open vertical slice | 4 | `REQ-001`-`019`,`021`-`026` | Light Open IT/generated verify |
| MODIFY/CREATE/DELETE/RENAME | `egon-cola-archetypes/egon-cola-archetype-service/src/**` | JPA/String/impl in domain | Common MP evaluation + Long + new migrations | Service legacy vertical slice | 5 | `REQ-001`-`020`,`022`-`026` | Service IT/generated verify |
| MODIFY/CREATE/DELETE/RENAME | `egon-cola-archetypes/egon-cola-archetype-service-open/src/**` | direct MP/`*Po`/impl in domain | Common MP evaluation Open + manual 003/004 | Service Open vertical slice | 6 | `REQ-001`-`019`,`021`-`026` | Service Open IT/generated verify |
| MODIFY/CREATE/DELETE | `egon-cola-archetypes/egon-cola-archetype-web/src/**` | JPA/String/impl in domain | Common MP organization + Long + new migrations | Web legacy vertical slice | 7 | `REQ-001`-`020`,`022`-`026` | Web IT/generated verify |
| MODIFY/CREATE/DELETE | `egon-cola-archetypes/egon-cola-archetype-web-open/src/**` | direct MP/local Mapper/repository chain | Common MP organization Open + manual 003/004 | Web Open vertical slice | 8 | `REQ-001`-`019`,`021`-`026` | Web Open IT/generated verify |
| MODIFY | `egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md` | old JPA/domain persistence wording | Light Common MP/domain generic/infra impl/tenant contract | Light living architecture | 3 | `REQ-023`-`025` | Light verifier/doc scan |
| MODIFY | `egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md` | old JPA/domain impl wording | Service Common MP/domain generic/infra impl/tenant contract | Service living architecture | 5 | `REQ-023`-`025` | Service verifier/doc scan |
| MODIFY | `egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md` | old JPA/domain impl wording | Web Common MP/domain generic/infra impl/tenant contract | Web living architecture | 7 | `REQ-023`-`025` | Web verifier/doc scan |
| MODIFY | `egon-cola-archetypes/open-source-archetype-code-style.md` | old direct MP/domain purity wording | unified Open Common MP/DAO/PO/manual rules | cross-family code style | 9 | `REQ-023`-`025` | full doc/source gate |

`target/**` 只允许作为 GENERATED validation output，不能手工编辑或进入 commit。所有具体模板 Java/XML/YAML/SQL/metadata/verifier paths 在对应 Step 的有限 path set 内各出现一次；旧 migration paths只读取/checksum，operation 始终为 untouched。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Applicable instructions：本线程用户提供的 root `AGENTS.md`；仓库内 `rg --files -g AGENTS.md` 未发现更深层文件。
- Branch/commit：`main@3be897e5cb4781890bfbac3104512e6e73bb943a`。
- Interrupted Step 1 state：六个 Common main/test 文件存在未提交实验改动；其中 `EgonModel`/`TestBusinessModel` 的 SuperBuilder 分支已由 `ModelBuilder` 编译错误判定无效。恢复执行时只能在 Step 1 内纠正这些现有改动，不得把失败分支提交，也不得用 broad reset/checkout 覆盖其他用户改动。
- 必须保留且不得 stage/commit：现有 Tianquan-Shoubing Spec/Plan 修改、Access Guard/Open/Tianquan-Jianshen/Yuheng Specs/Plans，包括并不限于 `docs/egon/spec/2026-08-25-19-01-yuheng-openapi31-source-refactor.md`、`docs/egon/spec/2026-08-25-19-43-yuheng-openapi-group-aggregation-amendment.md`。
- 每个 Step 使用 `git status --short`、显式 `git add -- <Step paths>`、`git diff --cached --name-only`；不使用 broad add/reset/checkout。
- 不修改 generated `target/**`；archetype source of truth 是 `src/main/resources/archetype-resources`、metadata 和 `verify.groovy`。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | `java -version`、`./mvnw -version` | Java 21；repository Maven Wrapper | local build only |
| Common focused | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-mybatis-plus-spring-boot-starter -am test` | exit 0 | embedded H2/module |
| Facade focused | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-organization-facade,egon-cola-evaluation-facade -am test` | exit 0 | module/contract |
| Archetype per module | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-NAME -am clean integration-test` | named generated project + verify.groovy pass | generated static/H2 |
| Full archetype | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test` | all eight reactor modules success | generated static/H2 |
| PostgreSQL | user-controlled disposable PostgreSQL/manual review after implementation | not required for Plan or default static gate | runtime/DBA external |
| External systems | Redis/RabbitMQ/Nacos/Dubbo/Yuheng/browser/Docker | not started automatically | user runtime external |

### 6.3 Immutable constraints and approved decisions

- 历史 six legacy `V20260726_001/_002` 与 Open `001/002` scripts checksum 不变。
- legacy master/shard 是独立 Flyway histories，每 location 正好一个新 file；Open 每 location 正好一个新 manual sequence。
- business Java identity breaking change为 Long；HTTP/GraphQL wire保持 decimal string；Proto field number/type不变。
- tenant/user 不新增 body/path/query input；从 trusted Provider/context派生，缺失失败。
- master_data不改为物理分片；现有 sharded tables才以 tenant database+table dual route。
- PO不用 `@RequiredArgsConstructor`/`@SuperBuilder`；具体业务 Bean必须用`@RequiredArgsConstructor`。PO固定普通`@Builder`且只包含自身业务字段，继承技术字段由framework mapping/fill维护。
- 不自动启动项目，不修改外部已生成系统，不在线猜测 UUID/tenant 数据。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | finite brace path set 用于成对 sibling 文件，执行 staging 时展开为列出的精确名字 | 六模板 canonical/open 同构目录；§5/Step blocks枚举实体/上下文 | 仅文档压缩，不扩大 scope 或产生运行 glob | executor必须先展开并核对存在性 |
| `PLAN-CLAR-002` | Legacy architecture test复用 Open 已锁定 `archunit-junit5:1.4.2`；若 verifier静态规则能完全证明同一合同，可不新增该 test dependency | Open root/starter POM 与 `OpenArchitectureTest` | 仅选择已有 test proof mechanism，不改变生产 dependency | 若 legacy build管理冲突，回到该 Step用 verifier等价证明，不能降级规则 |
| `PLAN-CLAR-003` | Common Starter不单独新增 Lombok POM dependency，因为 components parent已提供 `provided` Lombok 1.18.46和annotationProcessor | `egon-cola-components/pom.xml` dependencies/pluginManagement | 使用现有managed build capability | 若module effective-POM未继承，则Step 1补显式provided dependency并记录dependency-tree证据 |
| `PLAN-CLAR-004` | 现有 custom XML `created_at/updated_at`排序迁移为 `create_time/update_time`；Course/Score page index按primary §11.0精确更名 | current CourseMapper/ScoreMapper XML + Spec §11.0 | 字段只是已批准七字段映射，不改变page sort direction | 漏改会在 BoundSql/schema parity test失败 |

## 7. Ordered File-by-file Implementation Steps

> 每个 Step 在提交前必须先执行本 Step focused gate，再执行最小 module/archetype regression；验证输出 `target/**` 不进入提交。

### Step 1 — 发布 Common 业务字段 Builder 与 Lombok-only Service 扩展合同

- Requirements: `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-016`, `REQ-023`, `REQ-025`, `REQ-026`
- Dependencies: None
- Baseline state: baseline Common Starter 57-method/AR/H2 tests通过；`EgonModel`无Lombok builder；`EgonColaServiceImpl`持有三个final collaborator并有handwritten protected ctor；`TestBusinessService`显式调用`super(...)`。中断执行的六个未提交文件包含失败的`@SuperBuilder`实验，必须在本Step内纠正。
- Observable outcome: Common消费者以普通`@Builder`构造PO自身业务字段，继承的七个技术字段仍由JavaBean mapping/ASSIGN_ID/trusted Provider/MetaFill维护；具体Service以纯Lombok子类实现三个collaborator seam，不写constructor且57方法/AR行为不变。
- End state: `EgonModel`相对baseline无源码差异；Common ServiceImpl main/test/docs发布新extension API；模板尚未消费，等待Step 3-8。
- Test-first gate: Required — 已记录的SuperBuilder分支因缺失`ModelBuilder`而RED并被否决；纠正fixture/EgonModel后，新的有效RED必须只来自base尚无abstract getter且仍要求super constructor，不能再次出现`ModelBuilder`。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/{TestBusinessModel.java,TestBusinessService.java}`

- Purpose: 建立真实PO业务字段builder和Lombok-only Service subclass的compile fixture，并移除失败的SuperBuilder实验。
- Symbols: `TestBusinessModel.builder()`；`TestBusinessService`三final collaborator fields、protected Lombok getters、generated required constructor。
- Repository evidence: 两文件是所有Common unit/integration tests共用fixture；当前Model手写accessor、Service手写ctor和`setMapperForTest`已存在。
- Dependencies and consumers: JUnit tests、H2 integration、`EgonModel`、`EgonColaServiceImpl`；不被生产artifact消费者使用。
- Why now: compile fixture先锁定纠正后的用户要求；PO本身在baseline即可编译，Service fixture的预期RED才是base constructor seam。
- Contract/signature changes: `TestBusinessModel extends EgonModel<TestBusinessModel>`增加`@Data @NoArgsConstructor @AllArgsConstructor @Builder @Accessors(chain=true)`及显式TableField，不含SuperBuilder/RequiredArgs；`TestBusinessService`改`@RequiredArgsConstructor`且不写ctor。
- Input/output and state mapping: builder只设置`title/payload/version`；`id/tenantId/createUserId/createTime/updateUserId/updateTime/isDeleted`经继承setter或MetaFill fixture设置。Service constructor参数精确为validationUtils、tenantProvider、properties。
- Error and edge behavior: null collaborator由base getter access时`Objects.requireNonNull`稳定失败；test mapper仍仅通过test setter设置；不改变H2事务或tenant语义。
- Standards impact: `MC-MODEL-001`,`MC-BEAN-001`,`MC-PATTERN-001`,`MC-TEST-001` — test PO验证业务字段builder与framework技术字段边界，test Service验证Lombok/Template Method而非手写constructor。
- Literal rule enforcement: `Rule 1`,`Rule 3`,`Rule 4`,`Rule 9`,`Rule 11` — PO/Service语义名不变；PO用primary批准注解特化；Service使用RequiredArgs；Template Method seam显式。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder @Accessors(chain = true)
@TableName("test_business_record")
class TestBusinessModel extends EgonModel<TestBusinessModel> { @TableField("title") String title; ... }

@RequiredArgsConstructor
class TestBusinessService extends EgonColaServiceImpl<TestBusinessMapper,TestBusinessModel> {
  @Getter(PROTECTED) final EgonColaModelValidationUtils modelValidationUtils;
  @Getter(PROTECTED) final EgonColaTenantIdProvider tenantIdProvider;
  @Getter(PROTECTED) final EgonColaMybatisPlusProperties properties;
}
```

- Verification contribution: Common `testCompile` RED/GREEN和Step 1全部unit/integration fixture。
- After this file: TestBusinessModel不再引用`EgonModelBuilder/ModelBuilder`；在旧production Service base上仍预期因无可用super no-arg/abstract seam而compile RED，不得是PO builder或Lombok配置缺失。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/{model/EgonModelTest.java,contract/EgonModelActiveRecordParityTest.java,extension/EgonColaServiceImplTest.java}`

- Purpose: 固定业务字段builder、技术字段归属、构造器消失、protected seam和57/AR parity的可观察合同。
- Symbols: `buildsInheritedTechnicalAndBusinessFields`、`publishesProtectedLombokExtensionSeam`、existing parity assertions。
- Repository evidence: 三个测试已分别拥有MetaFill、AR reflection、IService行为fixture。
- Dependencies and consumers: File 1 fixtures与两个production types；只运行embedded/local test。
- Why now: 在production修改前先定义RED assertions。
- Contract/signature changes: reflection断言Service base为abstract、无三参数declared ctor、恰有三个protected abstract getter；source/delombok断言EgonModel无Builder/SuperBuilder、PO builder无七个继承技术字段，七字段annotation/AR不变。
- Input/output and state mapping: fixture builder的business values逐字段读取；technical values通过继承setter/MetaFill fixture读取；save/list/batch仍以tenant 9L、原return shapes和transaction annotation运行。
- Error and edge behavior: null getter结果在mapper前失败；missing tenant、invalid model、empty wrapper、oversize batch原断言全部保留。
- Standards impact: `MC-MODEL-001`,`MC-TIME-001`,`MC-PATTERN-001`,`MC-TEST-001` — builder不包含audit Instant等技术字段，parity防止PO/Lombok重构改变技术行为。
- Literal rule enforcement: `Rule 3`,`Rule 9`,`Rule 10`,`Rule 11` — exact construction/pattern/time contracts成为自动门禁。
- Implementation pseudocode:

```java
model = TestBusinessModel.builder().title("saved").payload("p").version(0L).build();
assertBusinessFields(model); assertBuilderHasOnly(title, payload, version);
model.setId(101L); model.setTenantId(9L); applyAuditFixture(model, NOW);
assertTechnicalFields(model); assertEgonModelSourceHasNoLombokBuilder();
assertTableAnnotationsUnchanged(EgonModel.class);
assertThat(declaredConstructors(EgonColaServiceImpl.class)).noneMatch(threeCollaborators);
assertProtectedAbstractGetters(modelValidationUtils, tenantIdProvider, properties);
runExisting57MethodAndActiveRecordParityAssertions();
```

- Verification contribution: focused RED与最终Common regression。
- After this file: test source完整表达新合同；production仍未GREEN。

#### Resume correction（not a final diff）— `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java`

- Purpose: 移除中断执行遗留的`@NoArgsConstructor/@SuperBuilder`实验，恢复并锁定baseline EgonModel合同。
- Symbols: `EgonModel<M>` class annotations；现有seven fields、AR overrides/hooks/getters/setters。
- Repository evidence: 该文件是七字段和AR唯一source of truth；实际编译错误精确指向外部父类`ModelBuilder`缺失。
- Dependencies and consumers: all future template POs、Common handler/interceptor/AR tests；继承MP `Model<M>`保持。
- Why now: 必须先清掉无效实验，才能得到只针对Service constructor seam的有效RED。
- Contract/signature changes: 最终无production contract change；删除未提交的Lombok imports/type annotations，不改field/table annotation/constructors/AR method visibility。
- Input/output and state mapping: 技术字段仍由现有setter、MP result mapping、ASSIGN_ID、MetaFill与TableLogic处理；无builder入口。
- Error and edge behavior: pre-insert null technical fields仍允许，Persisted group仍拒null；不存在builder绕过handler的问题。
- Standards impact: `MC-REUSE-001`,`MC-MODEL-001`,`MC-TIME-001`,`MC-SCOPE-001` — 避免为builder复制外部父类或改变公共ABI，Instant与AR行为不变。
- Literal rule enforcement: `Rule 3`,`Rule 5`,`Rule 10`,`Rule 11` — 普通PO Builder满足业务字段构造；不引Adapter、自定义builder父类或新stack。
- Implementation pseudocode:

```java
public abstract class EgonModel<M extends EgonModel<M>> extends Model<M> {
  keep @TableId id and six @TableField/@TableLogic fields exactly;
  keep final insert/update/delete templates, pkVal, hooks and accessors exactly;
}
assert git diff 3be897e5cb4781890bfbac3104512e6e73bb943a -- exact EgonModel.java path is empty before Step commit;
```

- Verification contribution: no-ModelBuilder compile、source diff、field/constructor/AR parity。
- After this correction: focused command不得再出现`ModelBuilder`；应只因Service fixture仍等待File 3而RED。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`

- Purpose: 删除handwritten protected ctor，用abstract collaborator accessors保持57方法invariant。
- Symbols: class改abstract；`getModelValidationUtils/getTenantIdProvider/getProperties`；现有57 methods/private helpers。
- Repository evidence: current lines 41-52 ctor与三个fields；全部增强方法只读取这三个collaborators。
- Dependencies and consumers: concrete template ServiceImpl、test fixture、IService parity；MyBatis `baseMapper`仍framework注入。
- Why now: EgonModel错误分支已清除，现关闭唯一剩余的Lombok subclass compile RED。
- Contract/signature changes: 删除三个base fields和ctor；增加protected abstract getters；所有field reads改为non-null checked getter result；无public method signature变化。
- Input/output and state mapping: tenant snapshot、model validation、pagination/batch properties取值顺序不变；每次业务调用只从subclass field读取同一Bean。
- Error and edge behavior: getter返回null稳定`Objects.requireNonNull`并在SQL前失败；batch/transaction/wrapper/result semantics保持。
- Standards impact: `MC-BEAN-001`,`MC-PATTERN-001`,`MC-LOG-001`,`MC-TEST-001` — Template Method扩展点，具体Bean负责Lombok DI；base无业务log新增需求。
- Literal rule enforcement: `Rule 4`,`Rule 9`,`Rule 11` — 删除手写注入constructor，Complex invariant继续Template Method。
- Implementation pseudocode:

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class EgonColaServiceImpl<M extends EgonColaMapper<T>,T extends EgonModel<T>> ... {
  protected abstract EgonColaModelValidationUtils getModelValidationUtils();
  protected abstract EgonColaTenantIdProvider getTenantIdProvider();
  protected abstract EgonColaMybatisPlusProperties getProperties();
  private Long requireTenantId() { return requireNonNull(getTenantIdProvider()).currentTenantId(); }
  keep all official method signatures, transactions, validation order and result shapes;
}
```

- Verification contribution: Service compile fixture、57 method tests、H2 tenant/batch tests。
- After this file: Common tests全部应GREEN；模板尚会因旧subclass ctor在后续Step迁移。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README{,.zh-CN}.md`

- Purpose: 发布新的consumer migration、PO业务字段builder示例和SuperBuilder禁止说明。
- Symbols: `EgonModel`示例、`EgonColaServiceImpl` subclass示例、compatibility note。
- Repository evidence: 双语README当前是Starter公共使用合同并含Model/Service snippets。
- Dependencies and consumers: component consumers/release reviewers；必须与Files 3-4 API一致。
- Why now: production/test已GREEN后记录可复制用法，避免示例再次写constructor。
- Contract/signature changes: 示例PO使用普通Builder且只声明业务字段；明确EgonModel/PO禁止SuperBuilder；示例Service使用RequiredArgs、final Qualifier fields/protected Lombok getters；列出breaking source migration。
- Input/output and state mapping: 文档明确id/tenant/audit/isDeleted不来自builder/request，读取时由MyBatis映射，insert时由ASSIGN_ID/Provider/MetaFill覆盖。
- Error and edge behavior: null collaborator、missing tenant、old subclass super constructor compile failure均给出迁移说明；不承诺binary compatibility。
- Standards impact: `MC-MODEL-001`,`MC-BEAN-001`,`MC-SCOPE-001` — 文档与真实annotation/DI合同一致。
- Literal rule enforcement: `Rule 3`,`Rule 4`,`Rule 11` — PO和Bean例子分别执行最新精确规则。
- Implementation pseudocode:

```text
document PO extends EgonModel<PO> with @Builder for declared business fields only;
document why MP Model lacks ModelBuilder and forbid @SuperBuilder/custom bridge;
document ServiceImpl with @Service("userDomainService"), @RequiredArgsConstructor,
final @Qualifier collaborators and protected Lombok getters; show no handwritten ctor;
state technical fields are framework-owned and old subclasses must migrate in the same release.
```

- Verification contribution: doc token/source API parity scan。
- After this file: Step 1可独立提交并供后续template compile。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-mybatis-plus-spring-boot-starter -am test`
- Expected result: exit 0；Common named unit/integration/parity tests全过；source scan无handwritten three-collaborator constructor。
- Failure returns to: File 1/2 forfixture/assertion RED mismatch，Resume correction for任何ModelBuilder或EgonModel diff，File 3 for57/tenant/batch行为。
- Completion criteria: business-only builder和Lombok subclass compile/behavior均GREEN；EgonModel相对baseline无diff；README双语同步；无其他Common module变更。
- Rollback: 在任何模板消费前path-limited revert Step 1；消费后必须与Steps 3-8同release forward-fix。
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/{TestBusinessModel.java,TestBusinessService.java}`; `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/{model/EgonModelTest.java,contract/EgonModelActiveRecordParityTest.java,extension/EgonColaServiceImplTest.java}`; `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`; `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README{,.zh-CN}.md`。`EgonModel.java`必须恢复为baseline且不得进入提交。
- Commit: `feat(common-mp): support business builders and lombok service inheritance`

### Step 2 — 发布 canonical facade 的 positive Long Java合同

- Requirements: `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-023`, `REQ-025`, `REQ-026`
- Dependencies: Step 1
- Baseline state: organization/evaluation facade contract tests通过String IDs；business code/status/message fields也为String且必须保留。
- Observable outcome: 两个canonical facade的所有business identity参数/record components为positive Long，非identity String语义、method names、return wrappers和time fields不变。
- End state: legacy Service/Web后续可针对统一Long API编译；模板consumer尚未修改。
- Test-first gate: Required — reflection/record component tests先期望Long和`@NotNull @Positive`，当前String/`@NotBlank`使其RED。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-{organization,evaluation}-facade/src/test/java/top/egon/cola/{organization,evaluation}/facade/{OrganizationFacadeContractTest.java,EvaluationFacadeContractTest.java}`

- Purpose: 在production API前定义Long signature、validation annotation和serialization保持。
- Symbols: existing facade method inventory assertions；new `allBusinessIdsArePositiveLong`、large Snowflake fixture。
- Repository evidence: 两测试是canonical public contract唯一集中reflection gate。
- Dependencies and consumers: facade main records/interfaces；archetypes reactor JUnit。
- Why now: RED锁定只迁移identity而非所有String。
- Contract/signature changes: 断言ID components/parameters为`Long`，nullable request IDs有`@NotNull @Positive`；code/name/status/error/traceId仍String；Instant仍Instant。
- Input/output and state mapping: `9007199254740993L`在Java serialization round trip保持；list of user IDs变`List<Long>`。
- Error and edge behavior: null/0/negative IDs validation失败；UUID lexical不再是Java API输入；非ID空白规则保持。
- Standards impact: `MC-VALID-001`,`MC-MODEL-001`,`MC-JSON-001`,`MC-TIME-001`,`MC-TEST-001` — record保持simple carrier，validation/time/serialization逐字段断言。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 6`,`Rule 10`,`Rule 11` — DTO/Request/Response suffix保持且所有boundary ID规则可执行。
- Implementation pseudocode:

```java
for each facade method and record component classified as business identity:
  assert type == Long.class and annotations include NotNull + Positive when input is required;
assert code/name/status/message/traceId remain String and Instant components remain Instant;
validate(requestWith(0L or -1L)) has violations;
serialize/deserialize responseWith(9007199254740993L) and assert exact Long value;
```

- Verification contribution: Step 2 RED/GREEN唯一contract gate。
- After this file: tests compile againstold API where possible并因type/annotation断言RED。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/{user/{UserFacade.java,PermissionFacade.java,dto/{AssignRoleDTO.java,PermissionTreeDTO.java,UserDetailDTO.java}},teaching/{GradeFacade.java,SchoolClassFacade.java,dto/{AssignUserToClassDTO.java,GradeDetailDTO.java,SchoolClassDetailDTO.java}}}`

- Purpose: 将organization canonical identity surface精确迁移Long。
- Symbols: `getUser(Long)`、`getPermissionTree(Long)`、`getGrade(Long)`、`getSchoolClass(Long,Long)`；records的user/grade/class IDs。
- Repository evidence: current files含String IDs；Role code、permission code、grade code等business code仍String。
- Dependencies and consumers: Web legacy adapter/application、Service cross-client、contract test；不影响Open local Proto field numbers。
- Why now: tests已固定精确identity inventory。
- Contract/signature changes: ID String/NotBlank -> Long/NotNull/Positive；`SchoolClassDetailDTO.userIds` -> `List<Long>`；不改method names/cardinality。
- Input/output and state mapping: request Long直接进入consumer command/domain ID；response Long不做UUID/text parsing；business codes原样。
- Error and edge behavior: invalid ID由Jakarta validation拒绝；missing entity/exception types/error code保持。
- Standards impact: `MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-JSON-001`,`MC-TIME-001` — simple records保持，只有identity component变化。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 6`,`Rule 10`,`Rule 11` — semantic DTO和validation精确，不引mutable carrier。
- Implementation pseudocode:

```java
interface UserFacade { UserDetailDTO getUser(@NotNull @Positive Long userId); }
record AssignRoleDTO(@NotNull @Positive Long userId, @NotBlank String roleCode) {}
record SchoolClassDetailDTO(@NotNull @Positive Long id, String name, String gradeCode,
                            String gradeName, String status, List<@Positive Long> userIds) {}
preserve create DTO business code/name/email fields and all exception/result contracts;
```

- Verification contribution: Organization facade contract test与后续Web compilation。
- After this file: organization module应GREEN一半；evaluation仍RED。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/course/{CourseFacade.java,dto/{CourseResponse.java,CourseScheduleResponse.java,GetCourseRequest.java,ScheduleCourseRequest.java}}`

- Purpose: 迁移course/schedule identity，同时保留PageQuery、code/name/status/time语义。
- Symbols: courseId、classId、scheduleId record components和facade signatures。
- Repository evidence: current DTOs为Serializable records，startsAt/endsAt是Instant，PageCourseRequest只含page fields。
- Dependencies and consumers: Service legacy adapter/application、Web evaluation client、contract test。
- Why now: organization slice已固定，继续同一public release中的evaluation course部分。
- Contract/signature changes: identity String -> Long + constraints；CreateCourseRequest code/name不变；PageCourseRequest不新增ID。
- Input/output and state mapping: Long传domain/PO；Instant保持UTC wire；response wrappers不变。
- Error and edge behavior: nonpositive IDs失败；schedule starts>=ends业务校验仍由consumer owning layer处理。
- Standards impact: `MC-VALID-001`,`MC-MODEL-001`,`MC-TIME-001`,`MC-SCOPE-001` — record/Instant/validation保持。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 10`,`Rule 11` — Request/Response命名和java.time不变。
- Implementation pseudocode:

```java
record GetCourseRequest(@NotNull @Positive Long courseId) implements Serializable {}
record ScheduleCourseRequest(@NotNull @Positive Long courseId,
  @NotNull @Positive Long classId, @NotNull Instant startsAt, @NotNull Instant endsAt) ...
record CourseResponse(Long id, String code, String name, int credit, String status) ...
keep CourseFacade method names, Response/PageResponse wrappers and page normalization unchanged;
```

- Verification contribution: Evaluation course contract/reflection与Service compile prerequisite。
- After this file: course identity GREEN；exam/score仍RED。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/exam/{ExamFacade.java,ScoreFacade.java,dto/{AttachExamPaperRequest.java,CreateExamRequest.java,ExamPaperResponse.java,ExamResponse.java,GetExamRequest.java,GetScoreRequest.java,PageScoreRequest.java,PublishExamRequest.java,RecordScoreRequest.java,ScoreResponse.java}}`

- Purpose: 迁移exam/paper/score/student/course identity并保持RPC业务contract。
- Symbols: examId、paperId、scoreId、courseId、studentId fields/signatures。
- Repository evidence: current records显式Serializable，points/totalPoints数值和status String不属于identity。
- Dependencies and consumers: Service legacy MQ/RPC/application、organization evaluation client、contract test。
- Why now: 完成evaluation public surface后才能提交同release contract。
- Contract/signature changes: business identity全为Long；PageScoreRequest examId有Positive；所有Proto/Open wire字段不在本模块改变。
- Input/output and state mapping: Long一对一；points仍int；Instant starts/ends不变；event/message ID不在canonical DTO中引入。
- Error and edge behavior: null/nonpositive ID由validation拒绝；invalid points/time/state仍由现有domain flow处理。
- Standards impact: `MC-VALID-001`,`MC-MODEL-001`,`MC-JSON-001`,`MC-TIME-001`,`MC-SCOPE-001` — 不把status/code误改Long。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 6`,`Rule 10`,`Rule 11` — exact record components/constraints/time。
- Implementation pseudocode:

```java
record GetScoreRequest(@NotNull @Positive Long examId,
                       @NotNull @Positive Long scoreId) implements Serializable {}
record RecordScoreRequest(@NotNull @Positive Long examId,
  @NotNull @Positive Long studentId, @PositiveOrZero int points) ...
record ScoreResponse(Long id, Long examId, Long courseId, Long studentId,
                     int points, String status) ...
keep ExamFacade/ScoreFacade method names and existing response/error wrappers;
```

- Verification contribution: Evaluation contract test和Steps 5-8 consumer compilation。
- After this file: 两个canonical modules全部应GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-organization-facade,egon-cola-evaluation-facade -am test`
- Expected result: exit 0；two contract tests pass；reflection无String business identity/NotBlank ID，non-ID Strings/Instant保持。
- Failure returns to: File 1 forinventory误分类，File 2 fororganization signature，Files 3-4 for evaluation mapping/validation。
- Completion criteria: facade Java breaking contract完整且只有identity变化；后续template尚未提交混合consumer。
- Rollback: 在Steps 5/7消费前可path-limited revert；消费后需facade+templates同release forward-fix。
- Commit paths: `egon-cola-archetypes/egon-cola-{organization,evaluation}-facade/src/test/java/top/egon/cola/{organization,evaluation}/facade/{OrganizationFacadeContractTest.java,EvaluationFacadeContractTest.java}`; `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/{user/{UserFacade.java,PermissionFacade.java,dto/{AssignRoleDTO.java,PermissionTreeDTO.java,UserDetailDTO.java}},teaching/{GradeFacade.java,SchoolClassFacade.java,dto/{AssignUserToClassDTO.java,GradeDetailDTO.java,SchoolClassDetailDTO.java}}}`; `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/course/{CourseFacade.java,dto/{CourseResponse.java,CourseScheduleResponse.java,GetCourseRequest.java,ScheduleCourseRequest.java}}`; `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/exam/{ExamFacade.java,ScoreFacade.java,dto/{AttachExamPaperRequest.java,CreateExamRequest.java,ExamPaperResponse.java,ExamResponse.java,GetExamRequest.java,GetScoreRequest.java,PageScoreRequest.java,PublishExamRequest.java,RecordScoreRequest.java,ScoreResponse.java}}`
- Commit: `feat(archetype-facade): migrate business identities to long`

### Step 3 — 将 Light legacy 模板从 JPA/UUID 垂直迁移到 Common MP

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`
- Dependencies: Steps 1-2
- Baseline state: Light单模块使用JPA/UUID、repo.jpa/RepositoryImpl、旧Flyway和UUID分片；HTTP/GraphQL/RPC/MQ业务行为测试存在。
- Observable outcome: `egon-cola-archetype-light`生成的单模块项目只使用Common MP，5个domain Service绑定8个PO/DAO，Long/wire/tenant/schema/route合同均GREEN。
- End state: Light legacy archetype IT和generated verify独立通过；Light Open仍待Step 4。
- Test-first gate: Required — architecture/repository/schema/route/verifier assertions先发现JPA、missing DAO/EgonModel、old UUID route和missing V20260825 migrations。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 定义Light精确package、继承、禁止词、非持久port移包和Bean annotation合同。
- Symbols: ArchUnit/JUnit rules for `domain..service`、`infrastructure..repo.dao/po/service.impl`、JPA/UUID forbidden。
- Repository evidence: legacy无architecture test；Light Open已有`OpenArchitectureTest`可复用1.4.2 pattern；verifier已有生成树断言。
- Dependencies and consumers: generated test runtime、POM test dependency、所有后续production Java。
- Why now: 作为本vertical slice的RED contract。
- Contract/signature changes: 新test-only contract，不改runtime API；精确识别5 Services、8 POs、8 DAOs和non-persistence port packages。
- Input/output and state mapping: imported classes -> package/generic/annotation assertions；不执行SQL。
- Error and edge behavior: 任一JPA import、BaseMapper direct、PO RequiredArgs、domain impl或infra import立即失败。
- Standards impact: `MC-ARCH-001`,`MC-NAME-001`,`MC-MODEL-001`,`MC-BEAN-001`,`MC-TEST-001` — executable architecture/naming/annotation gate。
- Literal rule enforcement: `Rule 1`,`Rule 3`,`Rule 4`,`Rule 11` — suffix、PO特化、Bean和exact Light结构全部自动检查。
- Implementation pseudocode:

```java
import generated package classes and assert only User/Role/Permission/Course/SchoolClassDomainService
  reside in domain..service and extend EgonColaIService with EgonModel bound;
assert eight *PO extend EgonModel and eight *DAO extend EgonColaMapper;
assert *DomainServiceImpl reside infrastructure..service.impl with Slf4j/Service/RequiredArgs;
assert no source depends on jakarta.persistence, JpaRepository, EntityManager or UUID generators;
```

- Verification contribution: expected RED until Files 3-6；final generated architecture test。
- After this file: test可编译需File 2 POM；执行时对current JPA tree RED。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/{pom.xml,lombok.config,src/main/java/start/StudentManagementApplication.java}`

- Purpose: 建立Common MP/MapStruct/validation/ArchUnit编译与精确DAO scan。
- Symbols: `lombok.version=1.18.46`、Common BOM Starter dependency、`@MapperScan` user/teaching DAO packages。
- Repository evidence: root template当前JPA dependency和1.18.38；application无DAO scan；lombok.config已copy Qualifier。
- Dependencies and consumers: all generated modules in single POM、Files 1/3-7。
- Why now: RED test需要依赖，production types需要Common API；与同Step原子提交避免hybrid release。
- Contract/signature changes: 删除JPA/Hibernate/direct persistence依赖；加入Common MP Starter和test ArchUnit；保留Boot/Web/RPC/MQ dependencies。
- Input/output and state mapping: Maven resolution only；MapperScan精确`${package}.infrastructure.{user,teaching}.repo.dao`。
- Error and edge behavior: duplicate mapper scan/MP version/JPA transitive由dependency scan失败；Qualifier copy行保持。
- Standards impact: `MC-DEP-001`,`MC-BEAN-001`,`MC-ARCH-001`,`MC-SCOPE-001` — existing managed capabilities only。
- Literal rule enforcement: `Rule 4`,`Rule 5`,`Rule 11` — DI propagation和exact package root。
- Implementation pseudocode:

```xml
set lombok.version=1.18.46; import Egon components BOM already used by template;
depend on egon-cola-component-common-mybatis-plus-spring-boot-starter once;
remove spring-boot-starter-data-jpa and any official direct MyBatis-Plus coordinate;
add archunit-junit5 1.4.2 in test scope only; keep compiler Lombok/MapStruct processors aligned;
```

- Verification contribution: dependency tree、architecture testCompile、Mapper context。
- After this file: compile仍RED，因old JPA sources and missing DAO；dependency基础已正确。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/{user,teaching}/{service,client,yuheng,event}/**`

- Purpose: 固定5个generic persistence Service并将cache/query/publisher ports移出service package。
- Symbols: `User/Role/Permission/Course/SchoolClassDomainService<P extends EgonModel<P>>`；`User/CourseCachePort`、`User/TeachingQueryGateway`、event publishers及消费者imports。
- Repository evidence: current domain service目录含12 interfaces；infrastructure已有对应cache/client/event adapters和5 service impl。
- Dependencies and consumers: application manage、infra service.impl/cache/client/event adapter、adapter entrypoints。
- Why now: Common API/POM存在后，先建立infra实现要满足的domain compile contracts。
- Contract/signature changes: business methods IDs -> Long/domain ID Long；每个business Service extends `EgonColaIService<P>`；non-persistence method signatures保持业务语义只改suffix/package。
- Input/output and state mapping: commands/domain entities/events Long一对一；tenant不加参数；cache key/event ref ID变Long。
- Error and edge behavior: inherited CRUD禁止application跨边界滥用由architecture/verifier scan；missing/nonpositive IDs在owning validation boundary失败。
- Standards impact: `MC-NAME-001`,`MC-VALID-001`,`MC-PATTERN-001`,`MC-ARCH-001` — 语义suffix和domain contract精确，无新层。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 9`,`Rule 11` — Service only含Egon Service，其他ports使用Port/Yuheng/Event角色。
- Implementation pseudocode:

```java
interface UserDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
  User create(@Valid CreateUserCommand command); Optional<User> get(@Positive Long userId); ...
}
move UserCacheService -> domain.user.client.UserCachePort and UserQueryService -> UserQueryGateway;
move TeachingEventPublisher/UserEventPublisher into domain.*.event without persistence inheritance;
update all application/infrastructure imports; never import concrete PO/DAO into domain;
```

- Verification contribution: architecture generic/port rules和application compile。
- After this file: domain contracts符合目标；infra old repositories尚不compile。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/{adapter,facade,application,domain}/**/*{DTO,VO,Request,Command,Query,Event,Entity,Controller,Resolver,Facade,Manage,Validator}.java`

- Purpose: 将Light入口到domain的business IDs统一Long并补齐每层Validation/Jackson wire。
- Symbols: User/Course/SchoolClass controllers/facades/resolvers、commands/queries、domain IDs/entities/events、application validators/manages。
- Repository evidence: current files含String/UUID IDs、HTTP/GraphQL routes和现有validator classes；route/field names由Spec保持。
- Dependencies and consumers: Step 2 canonical facade、File 3 domain Service、Files 5-6 persistence、HTTP/GraphQL/RPC/MQ tests。
- Why now: domain signatures已确定，可使persistence mapping使用唯一Long model。
- Contract/signature changes: internal Java IDs Long；HTTP/GraphQL decimal String codec/annotations；tenant/user不加payload；time保持Instant/LocalDateTime语义。
- Input/output and state mapping: decimal text -> validated positive Long -> Command/domain -> Long response -> decimal text；event business IDs decimal-safe，eventId保持String。
- Error and edge behavior: blank/sign/exponent/UUID/overflow/0/negative拒绝；validation error wrapper/status/routes保持；>2^53 round trip精确。
- Standards impact: `MC-VALID-001`,`MC-CONVERT-001`,`MC-JSON-001`,`MC-TIME-001`,`MC-LOG-001` — 每个handoff groups/MapStruct BaseConverter/Jackson/java.time明确。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 6`,`Rule 10`,`Rule 11` — exact carriers/records/converters/Bean annotations。
- Implementation pseudocode:

```java
controller accepts canonical decimal id text and maps via Spring/Jackson to @Positive Long command field;
application @Validated calls ValidationUtils for internal re-entry, then UserDomainService<UserPO> only through interface;
MapStruct converter extends BaseConverter<Request,Command> and maps Long exactly, no BeanUtils/manual copy;
response/event serializer writes business Long as decimal string while code/status/eventId remain String;
assert invalid lexical forms stop before DomainService/DAO and preserve existing error wrapper;
```

- Verification contribution: MockMvc/GraphQL/facade/event/validation regression。
- After this file: boundary/domain Long compile path建立；persistence still missing。

#### File 5 — `DELETE egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/{domain/{user,teaching}/repos,infrastructure/{user,teaching}/repo/{jpa,impl}}/**`

- Purpose: 删除已被Egon Service/DAO直接覆盖的Repository ports/adapters与JPA access。
- Symbols: `*Repository`、`*RepositoryImpl`、`*JpaRepository`和package-info。
- Repository evidence: current chain是domain Repository -> impl -> JPA；Spec minimum design判定pure forwarding remove/merge。
- Dependencies and consumers: File 3 business Service/Files 6 ServiceImpl接管；application不再import repositories。
- Why now: consumers已在Files 3-4切换，可安全删除旧链。
- Contract/signature changes: internal persistence interfaces消失；无external route/facade deletion。
- Input/output and state mapping: same domain/PO data由ServiceImpl/DAO处理；transaction/cache/event时机在File 6保留。
- Error and edge behavior: `rg`必须证明零consumer/metadata reference；若发现复杂non-CRUD port，停止删除并按primary只迁其persistence部分，不凭Plan删业务合同。
- Standards impact: `MC-ARCH-001`,`MC-REUSE-001`,`MC-SCOPE-001`,`MC-TEST-001` — 去重复层而不扩大业务重构。
- Literal rule enforcement: `Rule 1`,`Rule 11` — DAO成为唯一access suffix；保持Light结构。
- Implementation pseudocode:

```text
before delete: search every Repository/JpaRepository/RepositoryImpl symbol and move callers to named DomainService/DAO;
delete domain repos and infrastructure repo.jpa/repo.impl Java/package-info only after zero consumers;
remove metadata/verifier expectations for deleted paths and add DAO/service.impl expectations later in this Step;
assert no jakarta.persistence/JpaRepository/EntityManager import remains outside negative tests.
```

- Verification contribution: forbidden source scan和architecture absence rules。
- After this file: old persistence chain absent；new DAO/PO/impl由File 6补全使GREEN。

#### File 6 — `CREATE egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/{java/infrastructure/{user,teaching}/{repo/dao,service/impl},resources/mybatis/mapper/{user,teaching}}/**`

- Purpose: 创建8个DAO/XML并把5个concrete ServiceImpl绑定8个PO和existing BaseConverters。
- Symbols: `User/Role/Permission/UserRole/RolePermission/Course/SchoolClass/ClassCourseScheduleDAO`；5 `*DomainServiceImpl`；Mapper XML namespaces。
- Repository evidence: current JPA PO/converters/service impl及Open sibling SQL提供业务方法/transaction/query证据；Common Mapper/Service contract已Step 1 GREEN。
- Dependencies and consumers: File 3 Service interfaces、File 4 application、existing PO/converters（同File修改）、Common Starter、DB schema File 7。
- Why now: old chain已删除，minimum MP implementation关闭compile/behavior RED。
- Contract/signature changes: DAO `@Mapper extends EgonColaMapper<ExactPO>`；PO改`extends EgonModel<PO>`与exact Lombok/MP annotations；ServiceImpl `extends EgonColaServiceImpl<DAO,PO>`并implements domain Service<PO>。
- Input/output and state mapping: business fields显式TableField；technical七字段只继承；link DAO批量/查询保留tenant/is_deleted；Converters extends BaseConverter并ignore framework-owned fields。
- Error and edge behavior: tenant/user缺失、cross-tenant relation、duplicate active key、invalid model在SQL前/constraint处失败并rollback；no SELECT *或physical suffix。
- Standards impact: `MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001`,`MC-LOG-001`,`MC-BEAN-001`,`MC-PATTERN-001` — exact PO/DAO/Service/DI/mapping合同。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 9`,`Rule 10`,`Rule 11` — PO用户特化、Service RequiredArgs/Qualifier、BaseConverter、Template Method。
- Implementation pseudocode:

```java
@TableName("light_users") @Data @NoArgsConstructor @AllArgsConstructor
@Builder @Accessors(chain=true)
class UserPO extends EgonModel<UserPO> { @TableField("external_id") String externalId; ... }
@Mapper interface UserDAO extends EgonColaMapper<UserPO> { Optional<UserPO> findByExternalId(...); }
@Slf4j @Service("userDomainService") @RequiredArgsConstructor
class UserDomainServiceImpl extends EgonColaServiceImpl<UserDAO,UserPO> implements UserDomainService<UserPO> {
  final @Qualifier("userDAO") UserDAO userDAO; final @Qualifier("userPOConverter") UserPOConverter converter; ...
  @Transactional create/assign methods validate, persist aggregate+links, publish/cache only after approved state point;
}
xml namespace=fullyQualified UserDAO; explicit columns include seven inherited columns; predicates/order match current behavior;
```

- Verification contribution: repository/service/mapper/H2 tests、context wiring、converter tests。
- After this file: Java/MyBatis call path可compile；schema/config仍待File 7。

#### File 7 — `CREATE egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/{master-data/V20260825_001__migrate_light_master_data_to_egon_model.sql,shard/V20260825_002__migrate_light_sharded_to_tenant_model.sql}`

- Purpose: 在两个独立Flyway histories前向迁移8逻辑表到Long/EgonModel/tenant schema。
- Symbols: `light_users/roles/permissions/user_roles/role_permissions/courses/school_classes_0/_1/class_course_schedules_0/_1` columns/constraints/indexes。
- Repository evidence: immutable V20260726 files与Spec §11 tables/indexes；master/shard locations分离。
- Dependencies and consumers: File 6 PO/XML、Flyway fixtures、File 8 metadata。
- Why now: Java mapping已固定，DDL可逐字段实现并被tests验证。
- Contract/signature changes: existing IDs/FKs deterministic seed mapping toBIGINT；add seven columns；tenant-scoped partial unique/link indexes；unknown rows preflight abort。
- Input/output and state mapping: known template seed IDs mappositive Long/tenant；create/update audit use deterministic migration actor/time；active rows is_deleted=false。
- Error and edge behavior: no unknown UUID/tenant guessing；precondition raises exception before destructive cast；DDL transactional where PostgreSQL supports；old files untouched。
- Standards impact: `MC-TIME-001`,`MC-CONFIG-001`,`MC-SCOPE-001`,`MC-TEST-001` — TIMESTAMPTZ/tenant/index exact。
- Literal rule enforcement: `Rule 7`,`Rule 10`,`Rule 11` — schema与profiles/PO time contract一致。
- Implementation pseudocode:

```sql
preflight assert only known template seed UUIDs/rows and deterministic tenant mapping exist;
ALTER/rename/rebuild PK/FK columns to BIGINT without editing V20260726 predecessors;
ADD tenant_id BIGINT, create_user_id/update_user_id VARCHAR(128),
    create_time/update_time TIMESTAMPTZ, is_deleted BOOLEAN DEFAULT FALSE;
enforce NOT NULL after deterministic backfill; add tenant-scoped active uniques/link indexes;
verify no null/duplicate/orphan rows and both suffix tables are isomorphic; abort otherwise.
```

- Verification contribution: migration fixture/checksum/schema parity/unknown-row failure。
- After this file: database target可由fresh Flyway chain生成；route/config尚待File 8。

#### File 8 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light/{src/main/resources/archetype-resources/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md,large-monolith-light-domain-architecture.md}`

- Purpose: 以tenant_id双层路由、profile parity、生成清单和文档关闭Light vertical slice。
- Symbols: `SnowflakeLongShardingAlgorithm/ShardingNodeMap`、two YAML table rules、four app profiles、metadata fileSets、verify forbidden/required assertions。
- Repository evidence: current UUID algorithm/tests、two YAML、four profiles、metadata/verifier/README/living doc均存在。
- Dependencies and consumers: Files 1-7全体，Maven Archetype generation。
- Why now: Java/schema完成后才能准确注册资源和断言最终树。
- Contract/signature changes: sharded `school_classes/class_course_schedules` database/table `shardingColumn: tenant_id`；master none；profile MP key hierarchy一致；metadata加入DAO/XML/new migrations并删JPA paths。
- Input/output and state mapping: positive tenant -> existing NodeMap database+suffix；same tenant parent/link same target；app config不从raw header建立tenant。
- Error and edge behavior: zero/negative/range/unavailable target fail；profile missing key、stale JPA/direct MP/String ID或old path使verifier non-zero。
- Standards impact: `MC-CONFIG-001`,`MC-PATTERN-001`,`MC-ARCH-001`,`MC-TEST-001` — Strategy、key parity、generated tree强制。
- Literal rule enforcement: `Rule 5`,`Rule 7`,`Rule 9`,`Rule 11` — existing Strategy复用且所有profiles/结构同步。
- Implementation pseudocode:

```yaml
for school_classes and class_course_schedules in both sharding YAML:
  actualDataNodes = shard_${0..1}.logical_${0..1}
  databaseStrategy.standard.shardingColumn = tenant_id
  tableStrategy.standard.shardingColumn = tenant_id
master tables stay master_data with none strategy; TenantLine still injects tenant_id;
copy identical egon.cola.component.mybatis-plus core keys into base/dev/test/prod;
metadata/verifier require DAO/PO/ServiceImpl/XML/new migrations and forbid JPA/UUID/direct MP/old dirs;
```

- Verification contribution: route/unit/config/source/metadata/verifier/archetype IT和generated verify。
- After this file: Light legacy目标完整GREEN，Step可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-light -am clean integration-test`
- Expected result: exit 0；Light generated project tests/verify通过；old migrations checksum不变；production/source dependency scan无JPA/UUID/direct MP。
- Failure returns to: File 1 architecture，Files 3-4 contracts/validation，Files 5-6 persistence，File 7 schema，File 8 route/config/generation。
- Completion criteria: one generated Light project独立可`clean verify`，8 PO/DAO、5 Service pair、Long/wire/tenant/schema全部有proof。
- Rollback: application/source可revert本Step；已执行新migration后不得回旧UUID/JPA writer，只能forward-fix/restore新app。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/architecture/LightPersistenceArchitectureTest.java`; `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/{pom.xml,lombok.config,src/main/java/start/StudentManagementApplication.java}`; `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/{user,teaching}/{service,client,yuheng,event}/**`; `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/{adapter,facade,application,domain}/**/*{DTO,VO,Request,Command,Query,Event,Entity,Controller,Resolver,Facade,Manage,Validator}.java`; `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/{domain/{user,teaching}/repos,infrastructure/{user,teaching}/repo/{jpa,impl}}/**`; `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/{java/infrastructure/{user,teaching}/{repo/dao,service/impl},resources/mybatis/mapper/{user,teaching}}/**`; `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/{master-data/V20260825_001__migrate_light_master_data_to_egon_model.sql,shard/V20260825_002__migrate_light_sharded_to_tenant_model.sql}`; `egon-cola-archetypes/egon-cola-archetype-light/{src/main/resources/archetype-resources/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md,large-monolith-light-domain-architecture.md}`
- Commit: `refactor(archetype-light): migrate persistence to common mybatis plus`

### Step 4 — 将 Light Open 收敛到同一 Common MP合同

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-021`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`
- Dependencies: Steps 1-3
- Baseline state: Light Open已是Long/direct MP 3.5.17/manual SQL，但PO不继承EgonModel、Mapper/RepositoryImpl与domain service ports不符合新合同。
- Observable outcome: Light Open与Step 3业务/PO/DAO/Service/tenant/schema一致，同时保留no-Flyway/manual SQL和Open运行边界。
- End state: Light canonical/open两个archetype均可独立生成验证；不修改legacy Step 3 paths。
- Test-first gate: Required — existing `OpenArchitectureTest`、mapper/schema tests和verifier先增加Common inheritance/annotation/manual 003/004 assertions，对current direct MP tree RED。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/{architecture/OpenArchitectureTest.java,infrastructure/{migration/{ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java},user/repo/{MapperSqlIntegrationTest.java,UserRepositoryImplTest.java},teaching/repo/SchoolClassRepositoryImplTest.java,config/datasource/{LightDataSourceModeTest.java,ShardingNodeMapTest.java,ShardingTopologyValidatorTest.java}}}`

- Purpose: 定义Open Common MP继承、SQL、manual sequence、tenant route与行为parity的RED合同。
- Symbols: eight PO/DAO/five Service assertions；`003/004` order；same-tenant same-node；Mapper SQL explicit columns。
- Repository evidence: named tests当前覆盖direct MP、manual 001/002、repository aggregate和route。
- Dependencies and consumers: generated Light Open test runtime；Files 2-5。
- Why now: 先精确固定与Step 3 parity及Open-only no-Flyway边界。
- Contract/signature changes: tests only；添加EgonModel annotations、no JPA/Flyway/direct MP、technical field/meta fill/logic delete assertions。
- Input/output and state mapping: positive tenant/user context -> insert/load/delete；manual 001..004 -> target schema；Long IDs保持。
- Error and edge behavior: missing tenant、cross tenant、invalid PO、unknown migration rows、negative route均零写/fail；application不得执行manual SQL。
- Standards impact: `MC-ARCH-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONFIG-001`,`MC-PATTERN-001`,`MC-TEST-001` — full Open RED/GREEN proof。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 7`,`Rule 9`,`Rule 10`,`Rule 11` — suffix/model/config/pattern/time均测试化。
- Implementation pseudocode:

```java
assert OpenArchitectureTest sees only five domain persistence Services and infra DAO/PO/impl;
apply manual SQL in exact 001,002,003,004 order to disposable fixture and compare all seven columns;
insert tenant 1 and tenant 2 rows; assert TenantLine isolation and stable database/table target;
parse every mapper XML and assert exact DAO namespace, explicit columns, tenant/is_deleted behavior;
assert no Flyway/JPA/direct official MP runtime dependency or application SQL runner exists;
```

- Verification contribution: Step 4 focused RED/GREEN和generated regression。
- After this file: current Open tree RED only forapproved missing changes。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/{pom.xml,lombok.config,src/main/java/start/StudentManagementApplication.java,src/main/java/domain/{user,teaching}/**}`

- Purpose: 切换Common MP依赖并实现generic domain Service/port package合同。
- Symbols: remove `mybatis-plus.version=3.5.17`；5 generic Services；CachePort/QueryGateway/Event packages；exact MapperScan DAO roots。
- Repository evidence: current root POM direct MP；domain service目录与Light legacy同构；application scan指向repo.mapper。
- Dependencies and consumers: Step 1 Common API、File 1 tests、application/infra adapters。
- Why now: 先建立compile dependencies和domain contracts，再替换infra。
- Contract/signature changes: root唯一Common MP Starter/Lombok 1.18.46；Service extends EgonColaIService；Long业务签名不变；port只改role suffix/package。
- Input/output and state mapping: current Long domain IDs保持；tenant仍不进业务payload；existing event/cache external keys按Long。
- Error and edge behavior: domain不能import具体PO/DAO；ArchUnit拒绝domain impl/non-Egon service；missing context由Starter拒绝。
- Standards impact: `MC-DEP-001`,`MC-NAME-001`,`MC-VALID-001`,`MC-BEAN-001`,`MC-ARCH-001` — no direct MP/new layer。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 4`,`Rule 5`,`Rule 11` — semantic ports、validation、Qualifier propagation和exact Light Open结构。
- Implementation pseudocode:

```xml
remove mybatis-plus.version and mybatis-plus-spring-boot3-starter;
add only egon-cola-component-common-mybatis-plus-spring-boot-starter through Egon BOM;
set lombok.version=1.18.46; keep Common ID/DTP and all approved Open dependencies;
```
```java
interface CourseDomainService<P extends EgonModel<P>> extends EgonColaIService<P> { existing Long methods; }
move cache/query/event interfaces out of service; MapperScan exactly repo.dao packages;
```

- Verification contribution: dependency/architecture/testCompile。
- After this file: domain/POM correct；old mapper/repository infra still RED。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/{java/infrastructure/{user,teaching}/{repo/{mapper,po,converter,impl},service/impl},resources/mybatis/mapper/{user,teaching}}/**`

- Purpose: 将8 Mapper重命名DAO、8 PO继承EgonModel、5 ServiceImpl绑定并删除pure forwarding impl。
- Symbols: exact eight `*DAO/*PO`、five `*DomainServiceImpl`、existing BaseConverters和XML IDs。
- Repository evidence: current Open mapper/XML/repository tests提供全部query/transaction语义；Step 3是目标parity reference。
- Dependencies and consumers: File 2 domain contracts、application、manual schema File 4。
- Why now: dependency/domain已就位，可一次性关闭direct Mapper/Repository compile gap。
- Contract/signature changes: Mapper -> DAO + `@Mapper extends EgonColaMapper`；PO exact annotations/TableName `light_*`；RepositoryImpl merge/delete；ServiceImpl Lombok fields/getters。
- Input/output and state mapping: current business Long字段原样；add inheritedseven technical fields；MapStruct BaseConverter ignore technical spoofing；XML改light-prefixed logical tables。
- Error and edge behavior: no duplicate active key/cross-tenant access；batch links same transaction；Mapper custom lists保留order且显式is_deleted语义。
- Standards impact: `MC-NAME-001`,`MC-MODEL-001`,`MC-CONVERT-001`,`MC-LOG-001`,`MC-BEAN-001`,`MC-PATTERN-001` — precise Egon persistence contracts。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 9`,`Rule 10`,`Rule 11` — same exact code shape asStep 3。
- Implementation pseudocode:

```java
rename UserMapper -> UserDAO and every XML namespace/reference; extends EgonColaMapper<UserPO>;
annotate each PO with TableName(light_*), exact five Lombok annotations and explicit business TableFields;
create/modify five @Slf4j @Service(named) @RequiredArgsConstructor impls with final @Qualifier dependencies;
merge existing RepositoryImpl orchestration into owning ServiceImpl, retain transactions/cache/events, then delete zero-consumer impls;
make all MapStruct converters implement BaseConverter and ignore tenant/audit/isDeleted on domain-to-PO;
```

- Verification contribution: architecture、mapper SQL、repository/service/context tests。
- After this file: Java/XML target complete；schema/config/metadata待Files 4-5。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/db/manual/postgresql/{master-data/003__migrate_light_master_data_to_egon_model.sql,shard/004__migrate_light_sharded_to_tenant_model.sql}`

- Purpose: 以前向manual sequence交付Light Open目标schema并迁移tenant route。
- Symbols: same 8 logical/10 physical table shapes asStep 3；manual README execution contract。
- Repository evidence: immutable 001/002、ManualSchema support、PostgreSQL BIGINT/ON CONFLICT style。
- Dependencies and consumers: File 3 mappings、File 1 schema tests、File 5 metadata/docs。
- Why now: PO/XML字段确定后编写精确SQL。
- Contract/signature changes: addseven fields/tenant indexes/light prefixes；known data deterministic，unknown abort；不引Flyway。
- Input/output and state mapping: operator按master/shard physical DB逐库执行；audit/time/is_deleted backfill与legacy target一致。
- Error and edge behavior: preflight/transaction/duplicate/orphan fail；application startup不执行；old 001/002 byte unchanged。
- Standards impact: `MC-TIME-001`,`MC-CONFIG-001`,`MC-SCOPE-001`,`MC-TEST-001` — PostgreSQL/java.time/schema parity。
- Literal rule enforcement: `Rule 7`,`Rule 10`,`Rule 11` — manual DB contract与profiles/PO一致。
- Implementation pseudocode:

```sql
003 master: preflight known rows; rename/rebuild logical tables to light_*; add/backfill seven fields;
004 shard: apply same columns/constraints to every _0/_1 table and tenant-scoped parent/link keys;
create only Spec-approved indexes; validate null/duplicate/orphan counts equal zero;
RAISE EXCEPTION for unmapped historical identity/tenant; never default unknown production rows to tenant 1;
do not include Flyway markers or any application-execution hook.
```

- Verification contribution: manual parser/PostgreSQL fixture/static no-Flyway gate。
- After this file: target schema deliverable exists；runtime route/packaging待File 5。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/{src/main/resources/archetype-resources/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml,db/manual/postgresql/README.md}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`

- Purpose: 同步tenant route、all profiles、manual runbook、generation inventory和Open README。
- Symbols: existing SnowflakeLong algorithm改input tenant；metadata 003/004；verifier Common-only/no JPA/Flyway/direct MP。
- Repository evidence: current Open files已覆盖manual schema和runtime profiles但路由key为business ID。
- Dependencies and consumers: Files 1-4；archetype plugin/generated user/DBA。
- Why now: 以最终tree/schema更新所有consumer-facing contracts。
- Contract/signature changes: sharded tables dual tenant_id；master none；four profiles same MP keys；README说明Starter/Service/PO/DAO/no-auto-DDL。
- Input/output and state mapping: same tenant parent/link同node；manual operator order/backup/pre-post counts写清；Long wire保持。
- Error and edge behavior: missing/unpositive tenant、profile drift、stale mapper/direct MP/old scripts使tests/verifier失败。
- Standards impact: `MC-CONFIG-001`,`MC-PATTERN-001`,`MC-ARCH-001`,`MC-TEST-001` — final Open Strategy/config/generation contract。
- Literal rule enforcement: `Rule 5`,`Rule 7`,`Rule 9`,`Rule 11` — no new utility，all profile parity，exact Open structure。
- Implementation pseudocode:

```yaml
set both database/table shardingColumn to tenant_id for school_classes and class_course_schedules;
retain master none strategy; use existing SnowflakeLongShardingAlgorithm/NodeMap positive guard;
mirror egon.cola.component.mybatis-plus key set in application/dev/test/prod;
```
```text
metadata/verify require repo.dao, EgonModel PO, ServiceImpl, XML, 003/004 and README;
forbid JPA, Flyway, direct MP version/artifact, repo.mapper/repo.impl and PO RequiredArgs;
```

- Verification contribution: Light Open archetype IT/generated verify/config/manual/route gates。
- After this file: Step 4 complete并与Light legacy parity。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-light-open -am clean integration-test`
- Expected result: exit 0；OpenArchitecture/manual/schema/mapper/route/verifier全部通过；no Flyway/JPA/direct MP。
- Failure returns to: File 1 test contract，File 2 dependency/domain，File 3 persistence，File 4 manual SQL，File 5 config/generation。
- Completion criteria: Light Open generated project可`clean verify`且与Step 3同business persistence contract，Open-onlyno-Flyway保持。
- Rollback: source/manual files可在operator执行前revert；执行003/004后只forward-fix/restore Common MP app。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/{architecture/OpenArchitectureTest.java,infrastructure/{migration/{ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java},user/repo/{MapperSqlIntegrationTest.java,UserRepositoryImplTest.java},teaching/repo/SchoolClassRepositoryImplTest.java,config/datasource/{LightDataSourceModeTest.java,ShardingNodeMapTest.java,ShardingTopologyValidatorTest.java}}}`; `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/{pom.xml,lombok.config,src/main/java/start/StudentManagementApplication.java,src/main/java/domain/{user,teaching}/**}`; `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/{java/infrastructure/{user,teaching}/{repo/{mapper,po,converter,impl},service/impl},resources/mybatis/mapper/{user,teaching}}/**`; `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/db/manual/postgresql/{master-data/003__migrate_light_master_data_to_egon_model.sql,shard/004__migrate_light_sharded_to_tenant_model.sql}`; `egon-cola-archetypes/egon-cola-archetype-light-open/{src/main/resources/archetype-resources/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml,db/manual/postgresql/README.md}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`
- Commit: `refactor(archetype-light-open): adopt common mybatis plus contract`

### Step 5 — 将 Service legacy evaluation模板迁移到Common MP

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`
- Dependencies: Steps 1-2
- Baseline state: seven-module Service template使用JPA/String IDs，3 impl位于domain，5 tables分master/shard，Course/Score有page SQL语义。
- Observable outcome: evaluation legacy生成项目以3 generic Service、5 PO/DAO、Long canonical facade、tenant route和new Flyway运行，保持RPC/MQ-only。
- End state: Service legacy archetype独立GREEN；Service Open待Step 6。
- Test-first gate: Required — repository/page/rollback/datasource/verifier和new architecture test先对JPA/domain impl/String/old route RED。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/EvaluationPersistenceArchitectureTest.java`

- Purpose: 定义七模块方向、3 Service/5 PO/DAO、impl移infra及no-web/no-JPA合同。
- Symbols: module/package ArchUnit rules、generic inheritance、Bean annotations、forbidden dependencies。
- Repository evidence: current domain has service/impl；Open sibling有`OpenArchitectureTest`；Service living doc定义RPC/MQ-only。
- Dependencies and consumers: generated starter test runtime、Files 2-5。
- Why now: vertical RED contract必须先于POM/source迁移。
- Contract/signature changes: test-only；明确domain可依赖Common MP上界但不能infra，adapter不能HTTP/GraphQL。
- Input/output and state mapping: class imports/generics/annotations -> assertions；无runtime调用。
- Error and edge behavior: domain impl、repo.jpa/mapper、JPA/direct MP、Controller/Web依赖或non-Egon service使test失败。
- Standards impact: `MC-ARCH-001`,`MC-NAME-001`,`MC-MODEL-001`,`MC-BEAN-001`,`MC-TEST-001` — exact Service Archetype gate。
- Literal rule enforcement: `Rule 1`,`Rule 3`,`Rule 4`,`Rule 11` — naming/model/Bean/architecture自动检查。
- Implementation pseudocode:

```java
assert domain course/exam service interfaces extend EgonColaIService and no service.impl package remains;
assert infrastructure owns Course/Exam/ScoreDomainServiceImpl and five DAO/PO pairs;
assert adapter exposes only RPC/MQ/facade, never controller/graphql/web filter;
assert forbidden JPA/direct MP/String business ID imports/tokens are absent from production tree;
```

- Verification contribution: Step 5 architecture RED/GREEN。
- After this file: POM dependency未加入前testCompile RED；expected prerequisite failure清楚。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/EvaluationServiceApplication.java}}`

- Purpose: 建立Common dependency module direction、Lombok/ArchUnit和DAO scan。
- Symbols: domain Common MP Starter；infra无JPA；starter exact `course/exam.repo.dao` scan；1.18.46。
- Repository evidence: current infra POM JPA，domain无MP，starter现有runtime/test deps；Open sibling管理ArchUnit 1.4.2。
- Dependencies and consumers: all seven generated modules、File 1、Files 3-5。
- Why now: compile prerequisites；同Step保持atomic vertical release。
- Contract/signature changes: remove JPA/Hibernate；no direct official MP；domain consumes Egon Starter；root processors/version align。
- Input/output and state mapping: Maven graph仍adapter->application->domain、infra->domain、starter->adapter+infra；only approved domain technical upper bound exception。
- Error and edge behavior: dependency cycle、duplicate MP version/mapper scan、Web leakage使dependency/ArchUnit gate失败。
- Standards impact: `MC-DEP-001`,`MC-ARCH-001`,`MC-BEAN-001`,`MC-SCOPE-001` — exact seven-module graph。
- Literal rule enforcement: `Rule 4`,`Rule 5`,`Rule 11` — existing dependencies only，Qualifier propagation和Service architecture。
- Implementation pseudocode:

```xml
root: set Lombok 1.18.46 and remove mybatis-plus.version/direct starter properties;
domain: add Common MP Starter from Egon BOM so generic Service compiles;
infrastructure: delete spring-data-jpa/Hibernate, retain domain/facade/MapStruct/database test dependencies;
starter: add/reuse ArchUnit test scope and scan ${package}.infrastructure.{course,exam}.repo.dao;
preserve no spring-boot-starter-web dependency and all RPC/MQ runtime contracts;
```

- Verification contribution: effective dependency tree/testCompile/context scan。
- After this file: POM graph正确；old Java JPA/domain impl仍RED。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`

- Purpose: 一次迁移Long boundary/domain、3 generic Services、5 `*PO/*DAO`、infra impl和BaseConverters/XML。
- Symbols: Course/Exam/Score Service；CourseSchedule/ExamPaper companion DAO；`CoursePo`等rename `CoursePO`；application commands/queries/results；facade converters/providers/MQ。
- Repository evidence: current JPA repository/tests and Open Mapper/XML provide method/transaction/page/order semantics；Step 2 canonical facade已Long。
- Dependencies and consumers: File 2 graph、canonical facade、schema File 4、RPC/MQ tests。
- Why now: compile/API/persistence不可分离；避免提交domain impl move或POM单独breaking。
- Contract/signature changes: String IDs -> Long；impl move domain->infra；DAO extends EgonColaMapper；PO exact annotations/EgonModel；Repository/Impl/JPA merge/delete；converters BaseConverter。
- Input/output and state mapping: Course/Exam/Score fields按Spec §11；Course/Score page sort用create_time desc,id asc；tenant/audit inherited；Proto/open不在本Step。
- Error and edge behavior: validation at facade->application->domain->DAO；schedule overlap、exam state、score uniqueness/rollback保持；tenant mismatch在SQL前失败。
- Standards impact: `MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001`,`MC-LOG-001`,`MC-BEAN-001`,`MC-PATTERN-001`,`MC-TIME-001` — full evaluation vertical Java contract。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 6`,`Rule 9`,`Rule 10`,`Rule 11` — exact suffix/validation/PO/DI/MapStruct/time/no Web。
- Implementation pseudocode:

```java
move Course/Exam/ScoreDomainServiceImpl to infrastructure.*.service.impl;
make each interface <P extends EgonModel<P>> extends EgonColaIService<P>;
rename five *Po -> *PO and *JpaRepository/*Mapper -> *DAO extends EgonColaMapper<PO>;
annotate PO with @TableName("evaluation_*") and explicit TableField business columns; inherit seven technical fields once;
ServiceImpl uses @Slf4j @Service(exactName) @RequiredArgsConstructor and final @Qualifier DAO/converter/client/event fields;
MapStruct converters extend BaseConverter, Long-map IDs, ignore technical spoofing; XML preserves predicates/order/page/affected rows;
delete old domain service.impl and pure Repository/JPA paths only after all callers compile.
```

- Verification contribution: repository/page/transaction/RPC/MQ/validation tests和File 1。
- After this file: Java/XML compile target complete；schema/config/generation待Files 4-5。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/{master-data/V20260825_001__migrate_evaluation_master_data_to_egon_model.sql,shard/V20260825_002__migrate_evaluation_sharded_to_tenant_model.sql}`

- Purpose: 迁移evaluation 5逻辑/9 physical shapes到Long/EgonModel/tenant schema。
- Symbols: evaluation_course、course_schedule_0/_1、exam_0/_1、exam_paper_0/_1、score_0/_1，exact indexes。
- Repository evidence: immutable legacy V001/V002、current JPA schema、Spec 5 table sections和page indexes。
- Dependencies and consumers: File 3 PO/XML、migration/repository tests、File 5 metadata。
- Why now: mapping/query已定，schema可exact实现。
- Contract/signature changes: new six? 此family恰好two independent files；old files untouched；Long IDs/FKs、seven fields、tenant partial uniques/indexes。
- Input/output and state mapping: known seed data deterministic；Course master singleton但tenant scoped；four sharded logic tablestenant route；audit UTC。
- Error and edge behavior: unknown UUID/tenant/preexisting duplicates abort；cross-location FK只application enforce；same-shard composite relation constraints按Spec。
- Standards impact: `MC-TIME-001`,`MC-CONFIG-001`,`MC-SCOPE-001`,`MC-TEST-001` — exact PostgreSQL/route/index contract。
- Literal rule enforcement: `Rule 7`,`Rule 10`,`Rule 11` — schema/time与Service profile一致。
- Implementation pseudocode:

```sql
master V20260825_001: preflight/backfill evaluation_course Long and seven fields; add active tenant/code/page index;
shard V20260825_002: migrate every course_schedule/exam/exam_paper/score suffix identically;
add tenant_id to every route/index/key; add score tenant+exam+active+create_time+id page index;
preserve schedule order and one-paper/one-score business constraints scoped by tenant/is_deleted;
verify suffix parity, no nulls, no duplicates/orphans; raise and abort on unknown data.
```

- Verification contribution: Flyway checksum/fresh/unknown fixture、schema/PO/XML parity。
- After this file: target DB chain存在；route/profiles/metadata待File 5。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md,student-management-service-only-rpc-mq-architecture.md}`

- Purpose: 关闭evaluation tenant route/profile/generation/docs合同。
- Symbols: SnowflakeLong route、course master none、four sharded logical tables tenant key、metadata/verifier/new migration paths。
- Repository evidence: current UUID sharding configs/tests、four profiles、Service verifier/README/living doc。
- Dependencies and consumers: Files 1-4、archetype plugin/generated Service users。
- Why now: final tree/schema可准确声明并验证。
- Contract/signature changes: all shard database/table columns tenant_id；MP key parity；delete JPA/domain impl resources；require DAO/XML/Long/PO/service impl/new migrations。
- Input/output and state mapping: same tenant course schedule/exam/paper/score同node；master course tenantline；RPC/MQ IDs Long。
- Error and edge behavior: range route unsupported；no HTTP stack；stale String/JPA/direct MP/profile/migration paths使verifier nonzero。
- Standards impact: `MC-CONFIG-001`,`MC-PATTERN-001`,`MC-ARCH-001`,`MC-TEST-001` — exact RPC-only generated contract。
- Literal rule enforcement: `Rule 5`,`Rule 7`,`Rule 9`,`Rule 11` — no Web/hybrid，all profiles/Strategy/doc sync。
- Implementation pseudocode:

```yaml
course: master_data.evaluation_course with none strategies;
course_schedule/exam/exam_paper/score: database and table standard strategy on tenant_id;
algorithm class = reused SnowflakeLongShardingAlgorithm, target database/table, positive NodeMap;
all application profiles expose identical Common MP + app.sharding core keys;
```
```text
metadata/verify/living docs require infra service.impl/repo.dao/repo.po/XML/V20260825 files;
forbid controllers/GraphQL, JPA/Hibernate/UUID/String business IDs/direct MP/domain service.impl;
```

- Verification contribution: evaluation route/config/verifier/archetype IT/generated verify。
- After this file: Service legacy completeGREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-service -am clean integration-test`
- Expected result: exit 0；evaluation facade/provider/MQ/repository/page/migration/route/architecture/verifier全部通过且无Web/JPA。
- Failure returns to: File 1 architecture，File 2 POM/wiring，File 3 Java/XML，File 4 migration，File 5 route/config/generation。
- Completion criteria: generated `student-management-evaluation`能`clean verify`，3 Services/5 PO/DAO/Long/tenant合同完整。
- Rollback: migration执行前source revert；执行后禁止JPA/String writer rollback，使用forward-fix。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/EvaluationPersistenceArchitectureTest.java`; `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/EvaluationServiceApplication.java}}`; `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`; `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/{master-data/V20260825_001__migrate_evaluation_master_data_to_egon_model.sql,shard/V20260825_002__migrate_evaluation_sharded_to_tenant_model.sql}`; `egon-cola-archetypes/egon-cola-archetype-service/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md,student-management-service-only-rpc-mq-architecture.md}`
- Commit: `refactor(archetype-service): migrate evaluation persistence to common mp`

### Step 6 — 将 Service Open evaluation模板收敛Common MP并保持Proto wire

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-021`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`
- Dependencies: Steps 1-2 and Step 5 parity result
- Baseline state: Service Open已有Long/int64、direct MP/manual SQL/local Proto，但`*Po/*Mapper/RepositoryImpl`和domain impl未统一。
- Observable outcome: Service Open使用与Step 5相同3 Service/5 PO/DAO/tenant schema，Proto field number/int64和RPC/MQ-only保持。
- End state: canonical/open evaluation两个generated projects独立GREEN。
- Test-first gate: Required — Long identity/Proto descriptor保持GREEN基线，新Common inheritance/manual 003/004/architecture assertions对current tree RED。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{__rootArtifactId__-domain/src/test/java/domain/identity/ServiceLongIdentityContractTest.java,__rootArtifactId__-facade/src/test/java/facade/contract/ProtoDescriptorContractTest.java,__rootArtifactId__-starter/src/test/java/architecture/OpenArchitectureTest.java,__rootArtifactId__-infrastructure/src/test/java/infrastructure/{migration/{ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java},course/repo/{CourseRepositoryTest.java,CourseScheduleRepositoryTest.java},exam/repo/{ExamRepositoryTest.java,ExamPaperRepositoryTest.java,ScoreRepositoryTest.java}}}`

- Purpose: 同时锁定Common MP迁移和Proto/int64 compatibility。
- Symbols: existing ID/proto tests；3 Service/5 PO/DAO；manual 003/004；page/order/rollback/tenant tests。
- Repository evidence: named tests已覆盖Open Long/Proto/mapper/manual；只需反转到Common contract。
- Dependencies and consumers: Files 2-5；generated protoc/Dubbo Triple compilation。
- Why now: RED必须先保证不会把持久重构误改wire。
- Contract/signature changes: tests assert descriptor field numbers/types unchanged；Java implementation/PO common inheritance changed。
- Input/output and state mapping: Protobuf int64 <-> Java long/Long <-> domain/PO Long exact；manual schema seven fields/tenant route。
- Error and edge behavior: descriptor diff、String/UUID、direct MP、missing context、unknown row、cross-tenant access均失败。
- Standards impact: `MC-ARCH-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-JSON-001`,`MC-CONFIG-001`,`MC-TEST-001` — wire/persistence separate proof。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 6`,`Rule 7`,`Rule 11` — Proto contract和semantic types不漂移。
- Implementation pseudocode:

```java
assert all existing proto services/methods/field numbers and int64 wire types equal baseline descriptors;
assert generated Java adapters map int64 to positive Long without string conversion;
assert three domain Services/five DAO/PO/infra impl exact Common inheritance and annotations;
apply manual 001..004 and run page/order/tenant/rollback repository cases;
assert no HTTP controller/GraphQL/JPA/Flyway/direct MP dependency appears.
```

- Verification contribution: Step 6 RED/GREEN及wire compatibility gate。
- After this file: current direct MP tree RED但Proto baseline remainsGREEN。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/EvaluationServiceApplication.java}}`

- Purpose: 收敛依赖/annotation processing/module wiring到Common Starter。
- Symbols: remove MP 3.5.17；domain Common MP；infra DAO；starter MapperScan；Lombok 1.18.46。
- Repository evidence: root/infra direct MP与Open ArchUnit均存在；Common/DTP/Proto依赖需保留。
- Dependencies and consumers: Step 1 API、File 1 tests、Files 3-5。
- Why now: compile prerequisite afterRED contract。
- Contract/signature changes: only persistence dependency/scan；Proto/grpc/dubbo/nacos/rabbit/DTP versions/ownership不改。
- Input/output and state mapping: Maven graph与local facade生成不变；domain upper-bound exception明确。
- Error and edge behavior: any direct MP/Flyway/JPA or missing generated Proto plugin makesdependency/verifier fail。
- Standards impact: `MC-DEP-001`,`MC-BEAN-001`,`MC-ARCH-001`,`MC-SCOPE-001` — approved dependencies only。
- Literal rule enforcement: `Rule 4`,`Rule 5`,`Rule 11` — Qualifier/Lombok和exact Open module graph。
- Implementation pseudocode:

```xml
remove mybatis-plus.version 3.5.17 and direct official starter from root/infra;
add Common MP Starter in domain through Egon BOM; inherit transitives in infra/starter;
align Lombok 1.18.46 and preserve protobuf/grpc/dubbo code generation plugins unchanged;
MapperScan only ${package}.infrastructure.{course,exam}.repo.dao;
retain ArchUnit test dependency and no Web runtime.
```

- Verification contribution: dependency tree/testCompile/protoc build。
- After this file: dependencies correct；source still old names/impl paths。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`

- Purpose: 实现与Step 5一致的generic Services、`*PO/*DAO`、infra impl和BaseConverters，保持local Proto adapters。
- Symbols: rename `CoursePo/CourseSchedulePo/ExamPo/ExamPaperPo/ScorePo` to `*PO`；Mapper->DAO；3 impl move；all consumers/imports/XML。
- Repository evidence: current direct MP source已是Long且测试完整；Step 5提供target name/table/transaction reference。
- Dependencies and consumers: File 2 graph、generated Proto types、MQ/adapter、manual SQL File 4。
- Why now: dependency/contract fixed后进行minimum vertical Java migration。
- Contract/signature changes: persistence only plus package/semantic suffix；Proto APIs/field numbers/methods不变。
- Input/output and state mapping: Proto long -> application record Long -> domain -> PO Long；technical fieldsMetaFill；page query/order preserved。
- Error and edge behavior: invalid/nonpositive/missing tenant在adapter/application/Service/DAO逐层失败；state/transaction/MQ behavior保持。
- Standards impact: `MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001`,`MC-LOG-001`,`MC-BEAN-001`,`MC-PATTERN-001`,`MC-TIME-001` — exact target parity。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 6`,`Rule 9`,`Rule 10`,`Rule 11` — no handwritten copy/ctor/wire drift。
- Implementation pseudocode:

```java
rename PO/DAO and update every XML namespace/resultMap/import/generated adapter mapping;
each PO extends EgonModel with @TableName("evaluation_*") and exact Lombok/MP annotations;
each Service interface is generic Egon IService; each infra impl is named/Slf4j/RequiredArgs/Qualified;
converters implement BaseConverter and map Long/Instant/enums explicitly while ignoring framework fields;
keep Protobuf generated sources GENERATED-only and never hand-edit; descriptor tests must remain byte-contract compatible.
```

- Verification contribution: Long/proto/repository/architecture/context tests。
- After this file: Java/XML target complete；manual/config/generation待Files 4-5。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/{master-data/003__migrate_evaluation_master_data_to_egon_model.sql,shard/004__migrate_evaluation_sharded_to_tenant_model.sql}`

- Purpose: 交付evaluation Open与Step 5同target schema的manual sequence。
- Symbols: 5 logical/9 physical table shapes、tenant/page/unique indexes、README order。
- Repository evidence: current immutable 001/002和manual tests；Spec exact filenames。
- Dependencies and consumers: File 3 PO/XML、File 1 schema tests、File 5 metadata/runbook。
- Why now: mapping确定后编写operator-forward schema。
- Contract/signature changes: addseven fields/table prefixes/tenant route；no app runner/Flyway。
- Input/output and state mapping: known rows deterministic；course page和score exam page indexes exact；UTC audit。
- Error and edge behavior: unknown rows/duplicates/orphans abort；manual per physical DB；old scripts unchanged。
- Standards impact: `MC-TIME-001`,`MC-CONFIG-001`,`MC-SCOPE-001`,`MC-TEST-001` — database/time/route safety。
- Literal rule enforcement: `Rule 7`,`Rule 10`,`Rule 11` — same target across Open profiles/PO。
- Implementation pseudocode:

```sql
003 migrates course -> evaluation_course with Long/seven fields and tenant active page/business indexes;
004 migrates every schedule/exam/paper/score suffix with tenant_id route and exact relation/page keys;
guard known seed identity/tenant mapping before DDL/data casts; raise on unknown state;
postcheck suffix parity, no nulls, active duplicates or invalid relationships;
deliver operator SQL only; never register Flyway or auto-execution.
```

- Verification contribution: manual SQL ordered fixture/schema parity。
- After this file: schema deliverable complete；Files 5 closes route/docs。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/README.md,src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`

- Purpose: 关闭Service Open tenant route/profile/manual/generation contract。
- Symbols: 4sharded tables tenant dual route；manual 003/004；Common-only verifier；RPC/MQ noWeb。
- Repository evidence: current Open files已验证Proto/manual/direct MP baseline。
- Dependencies and consumers: Files 1-4、archetype plugin/DBA/generated users。
- Why now: final tree可同步所有资源和文档。
- Contract/signature changes: route key tenant_id；master none；all profile keys equal；metadata rename PO/DAO/impl/manual paths。
- Input/output and state mapping: tenant/node/time/Long same Step 5；Proto field mapping documented unchanged。
- Error and edge behavior: stale `*Po/*Mapper/repo.impl`、direct MP/Flyway/JPA/HTTP、profile drift或descriptor diff阻断。
- Standards impact: `MC-CONFIG-001`,`MC-PATTERN-001`,`MC-ARCH-001`,`MC-TEST-001` — final Open Service gate。
- Literal rule enforcement: `Rule 5`,`Rule 7`,`Rule 9`,`Rule 11` — exact noWeb/Open architecture和Strategy/config。
- Implementation pseudocode:

```yaml
course remains singleton master; schedule/exam/paper/score database/table strategy uses tenant_id;
mirror Common MP settings in application/dev/test/prod and retain existing Nacos/Rabbit/Dubbo values per profile;
validate the identical core key set and positive tenant NodeMap targets before generated verification;
```
```text
manual README orders backups -> per-location preflight -> 001/002 -> 003/004 -> verification;
metadata/verifier require renamed PO/DAO/infra impl/XML/manual files and unchanged Proto descriptors;
forbid JPA/Flyway/direct MP/domain impl/HTTP artifacts.
```

- Verification contribution: Service Open archetype IT/generated verify/proto/manual/config/route。
- After this file: evaluation canonical/open parity complete。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-service-open -am clean integration-test`
- Expected result: exit 0；protoc/Dubbo Triple descriptor、Long identity、Common MP、manual schema、route、noWeb/noFlyway verifier全部通过。
- Failure returns to: File 1 contract，File 2 dependency/build generation，File 3 mapping，File 4 schema，File 5 route/metadata/docs。
- Completion criteria: generated Service Open `clean verify`，wire descriptor不变，3 Services/5 PO/DAO/tenant target与Step 5一致。
- Rollback: operator执行manual前source revert；执行后forward-fix，Proto contract始终不得回退字段号/type。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{__rootArtifactId__-domain/src/test/java/domain/identity/ServiceLongIdentityContractTest.java,__rootArtifactId__-facade/src/test/java/facade/contract/ProtoDescriptorContractTest.java,__rootArtifactId__-starter/src/test/java/architecture/OpenArchitectureTest.java,__rootArtifactId__-infrastructure/src/test/java/infrastructure/{migration/{ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java},course/repo/{CourseRepositoryTest.java,CourseScheduleRepositoryTest.java},exam/repo/{ExamRepositoryTest.java,ExamPaperRepositoryTest.java,ScoreRepositoryTest.java}}}`; `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/EvaluationServiceApplication.java}}`; `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`; `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/{master-data/003__migrate_evaluation_master_data_to_egon_model.sql,shard/004__migrate_evaluation_sharded_to_tenant_model.sql}`; `egon-cola-archetypes/egon-cola-archetype-service-open/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/README.md,src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`
- Commit: `refactor(archetype-service-open): adopt common mp without wire drift`

### Step 7 — 将 Web legacy organization模板迁移到Common MP

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`
- Dependencies: Steps 1-2
- Baseline state: seven-module Web模板使用JPA/String、4 impl在domain，8 tables、HTTP/GraphQL/RPC/MQ和Idempotency行为已存在。
- Observable outcome: organization legacy以4 generic Services、8 PO/DAO、Long canonical facade、tenant dual route和new Flyway运行，HTTP/GraphQL decimal wire及业务行为保持。
- End state: Web legacy独立GREEN；Web Open待Step 8。
- Test-first gate: Required — architecture、HTTP/GraphQL、Rabbit、repository、rollback、schema、route和verifier先期望Common/Long/tenant，对current JPA tree RED。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/OrganizationPersistenceArchitectureTest.java`

- Purpose: 定义Web七模块、4 Service/8 PO/DAO、infra impl和HTTP/GraphQL persistence隔离合同。
- Symbols: ArchUnit generic/package/annotation/dependency rules；Role只作companion DAO无ceremonial Service。
- Repository evidence: current domain impl/JPA；Web Open有`OpenArchitectureTest`；living doc定义organization-only Web形态。
- Dependencies and consumers: generated starter tests、Files 2-5。
- Why now: vertical RED contract先于生产/POM变更。
- Contract/signature changes: test-only；允许approved domain Common MP上界，不允许domain->infra、adapter->SQL。
- Input/output and state mapping: class/module graph -> assertions；无业务state。
- Error and edge behavior: JPA/direct MP/domain impl/non-Egon service/DAO wrong package/missing Bean annotations立即失败。
- Standards impact: `MC-ARCH-001`,`MC-NAME-001`,`MC-MODEL-001`,`MC-BEAN-001`,`MC-TEST-001` — exact Web architecture proof。
- Literal rule enforcement: `Rule 1`,`Rule 3`,`Rule 4`,`Rule 11` — names/PO/Bean/module direction自动门禁。
- Implementation pseudocode:

```java
assert only User/Permission/Grade/SchoolClassDomainService remain in domain..service and extend Egon IService;
assert Role/UserRole/RolePermission/SchoolClassUser are companion DAO/PO without fake DomainService;
assert four implementations reside infrastructure..service.impl and eight DAO/PO pairs use Common bases;
assert adapter owns HTTP/GraphQL/RPC, infrastructure owns SQL, and no JPA/direct MP/UUID token exists;
```

- Verification contribution: Step 7 architecture RED/GREEN。
- After this file: current JPA/domain impl tree RED。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/OrganizationApplication.java}}`

- Purpose: 建立Common MP依赖、Lombok/ArchUnit与精确organization DAO scan。
- Symbols: root 1.18.46；domain Starter；infra no JPA；starter user/teaching repo.dao scan。
- Repository evidence: current infra JPA、domain无MP、starter既有Web/GraphQL/test wiring。
- Dependencies and consumers: Step 1/2 API、File 1、Files 3-5。
- Why now: test/source compile prerequisites；本Step原子避免mixed release。
- Contract/signature changes: persistence dependencies only；Web/RPC/MQ/idempotency/runtime modules保持。
- Input/output and state mapping: module graph不变；canonical organization/evaluation facades仍same dependencies但IDsLong。
- Error and edge behavior: dependency cycle、JPA/direct MP、broad MapperScan或profile build drift在gates失败。
- Standards impact: `MC-DEP-001`,`MC-ARCH-001`,`MC-BEAN-001`,`MC-SCOPE-001` — existing capabilities only。
- Literal rule enforcement: `Rule 4`,`Rule 5`,`Rule 11` — Qualifier propagation、allowlist与exact Web modules。
- Implementation pseudocode:

```xml
align Lombok 1.18.46; add Common MP Starter to domain through Egon BOM;
remove spring-data-jpa/Hibernate/direct official MP from infrastructure/root;
preserve Boot Web/GraphQL/Rabbit/Dubbo/facade dependencies and plugin configuration;
add/reuse ArchUnit test scope; MapperScan only ${package}.infrastructure.{user,teaching}.repo.dao;
```

- Verification contribution: effective POM/testCompile/context wiring。
- After this file: dependencies正确；old Java persistence仍RED。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`

- Purpose: 垂直迁移Long boundary、4generic Services、8PO/DAO、infra impl、BaseConverters和Mapper XML。
- Symbols: User/Permission/Grade/SchoolClass Services；Role/UserRole/RolePermission/SchoolClassUser companions；all commands/queries/events/DTO/VO/controllers/resolvers/facade impl。
- Repository evidence: current JPA repository/flow/rollback/HTTP/GraphQL tests与Web Open Mapper/XML定义现有业务行为。
- Dependencies and consumers: File 2 graph、Step 2 facades、File 4 schema、all Web entries/events/caches/clients。
- Why now: public Long/persistence/impl move形成一个compile/behavior unit。
- Contract/signature changes: internal IDsLong；wiredecimal strings；impl move；DAO/EgonModel/PO annotations；Repository/JPA merge/delete；role remains companion。
- Input/output and state mapping: HTTP/GraphQL decimal -> Long -> application/domain -> PO；events Long decimal-safe；technical fieldsframework-owned；all existing business codes/status/idempotency keysString。
- Error and edge behavior: invalid lexical/nonpositive、cross-tenant、duplicate active key、class membership mismatch、downstream failure/rollback保持；trusted tenant不来自request。
- Standards impact: `MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001`,`MC-LOG-001`,`MC-BEAN-001`,`MC-JSON-001`,`MC-TIME-001`,`MC-PATTERN-001` — full Web Java/wire/DI/mapping contract。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 6`,`Rule 9`,`Rule 10`,`Rule 11` — semantic carriers、every handoff、PO user override、BaseConverter、Jackson/java.time。
- Implementation pseudocode:

```java
convert every business identity in Controller/GraphQL/facade/application/domain/event to positive Long;
serialize external HTTP/GraphQL/event Long as canonical decimal string; retain field names/status/error/idempotency key;
move four DomainServiceImpl classes to infrastructure, generic-bind primary DAO/PO, and compose companion DAOs;
rename/create eight DAO extends EgonColaMapper; make eight PO extends EgonModel with @TableName("organization_*");
use @Slf4j @Service(exact) @RequiredArgsConstructor and final @Qualifier fields; converters extend BaseConverter;
preserve cache/event/idempotency timing and @Transactional rollback; delete zero-consumer JPA/Repository forwarding chain;
```

- Verification contribution: HTTP/GraphQL/RPC/MQ/repository/rollback/validation/context tests。
- After this file: Java/XML target complete；schema/config/generation待Files 4-5。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/{master-data/V20260825_001__migrate_organization_master_data_to_egon_model.sql,shard/V20260825_002__migrate_organization_sharded_to_tenant_model.sql}`

- Purpose: 迁移organization 8logical/10physical shapes到Long/EgonModel/tenant schema。
- Symbols: organization_users/roles/permissions/user_roles/role_permissions/grades、school_classes_0/_1、school_class_users_0/_1。
- Repository evidence: immutable V001/V002、current JPA schema、Spec 8 table sections/ER/indexes。
- Dependencies and consumers: File 3 PO/XML、flow/rollback/schema tests、File 5 metadata。
- Why now: Java/SQL mapping确定后实现new forward migrations。
- Contract/signature changes: Long IDs/FKs、seven fields、tenant active unique/link indexes；unknown data guard；old migrations untouched。
- Input/output and state mapping: known seed deterministic；master tables tenantline singleton；sharded class/membership tenant route and same-node。
- Error and edge behavior: invalid/unknown UUID/tenant、duplicates/orphans abort；cross-datasource FK application-enforced；no destructive guessing。
- Standards impact: `MC-TIME-001`,`MC-CONFIG-001`,`MC-SCOPE-001`,`MC-TEST-001` — exact schema/time/tenant proof。
- Literal rule enforcement: `Rule 7`,`Rule 10`,`Rule 11` — migration与PO/profile architecture一致。
- Implementation pseudocode:

```sql
master V20260825_001: preflight and migrate six organization_* master tables to Long/seven fields;
add tenant-scoped active email/code/relation indexes and deterministic audit backfill;
shard V20260825_002: migrate both school_classes and school_class_users suffixes identically;
add tenant_id to route/unique/link keys and verify same-tenant membership relationships;
abort transaction on unknown identity/tenant, nulls, active duplicates or orphan mappings.
```

- Verification contribution: fresh/unknown Flyway、schema/PO/route parity、old checksum。
- After this file: target schema chain exists；File 5 closes runtime/generation。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md,multi-project-multi-module-architecture.md}`

- Purpose: 关闭organization route/profile/metadata/verifier/README/living architecture。
- Symbols: master six none；school_classes/school_class_users tenant dual route；all profiles；generated file sets。
- Repository evidence: current UUID route、four profiles、verifier和living doc均存在。
- Dependencies and consumers: Files 1-4、archetype plugin/generated Web users。
- Why now: final tree/schema/wire可同步描述与生成。
- Contract/signature changes: Long/Common MP paths、new migrations；routes/status/HTTP/GraphQL field names不改；metadata删除JPA/domain impl。
- Input/output and state mapping: trusted positive tenant -> same class/membership node；decimal IDs preserved；master tenant filtering。
- Error and edge behavior: missing profile key、JPA/direct MP/String ID/old paths/route mismatch/unsafe header trust使tests/verifier失败。
- Standards impact: `MC-CONFIG-001`,`MC-PATTERN-001`,`MC-ARCH-001`,`MC-JSON-001`,`MC-TEST-001` — final generated Web contract。
- Literal rule enforcement: `Rule 5`,`Rule 6`,`Rule 7`,`Rule 9`,`Rule 11` — no duplicate utility/JSON stack，profile/Strategy/structure exact。
- Implementation pseudocode:

```yaml
six master tables remain master_data none strategy with TenantLine tenant_id predicates;
school_classes and school_class_users use tenant_id for database and table standard strategies;
all application profiles contain identical Common MP and app.sharding core keys;
```
```text
metadata/verify require infra service.impl/repo.dao/PO/XML/new migrations and Long wire tests;
forbid JPA/Hibernate/UUID/direct MP/domain service.impl/old repository paths;
README/living doc state four Services, eight PO/DAO, trusted tenant and decimal external IDs.
```

- Verification contribution: Web route/config/HTTP/GraphQL/verifier/archetype IT/generated verify。
- After this file: Web legacy completeGREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-web -am clean integration-test`
- Expected result: exit 0；organization HTTP/GraphQL/RPC/MQ/repository/rollback/migration/route/architecture/verifier全部通过，decimal IDs精确。
- Failure returns to: File 1 architecture，File 2 dependency，File 3 Java/XML/wire，File 4 migration，File 5 route/config/generation。
- Completion criteria: generated `student-management-organization`可`clean verify`，4 Services/8PO/DAO和Long/tenant contracts完整。
- Rollback: migration执行前source revert；执行后只forward-fix，不恢复String/JPA writer。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/OrganizationPersistenceArchitectureTest.java`; `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/OrganizationApplication.java}}`; `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`; `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/{master-data/V20260825_001__migrate_organization_master_data_to_egon_model.sql,shard/V20260825_002__migrate_organization_sharded_to_tenant_model.sql}`; `egon-cola-archetypes/egon-cola-archetype-web/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md,multi-project-multi-module-architecture.md}`
- Commit: `refactor(archetype-web): migrate organization persistence to common mp`

### Step 8 — 将 Web Open organization模板收敛Common MP并保持HTTP/GraphQL/Proto wire

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-021`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`
- Dependencies: Steps 1-2 and Step 7 parity result
- Baseline state: Web Open已有Long/int64、HTTP/GraphQL、direct MP、manual SQL/local Proto，8 mapper/PO和repository forwarding未统一。
- Observable outcome: Web Open与Step 7同4 Service/8PO/DAO/tenant schema，HTTP/GraphQL decimal IDs和Proto descriptors保持。
- End state: six archetype coding slices全部GREEN，等待Step 9 cross-family release gate。
- Test-first gate: Required — existing HTTP/GraphQL/Proto/OpenArchitecture/manual/repository tests先增加Common inheritance/manual 003/004/tenant assertions，current tree RED而wire baseline保持GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-adapter/src/test/java/adapter/{OrganizationGraphQlContractTest.java,OrganizationHttpErrorContractTest.java},__rootArtifactId__-facade/src/test/java/facade/ProtoContractTest.java,__rootArtifactId__-starter/src/test/java/{architecture/OpenArchitectureTest.java,starter/{OrganizationDataSourceModeTest.java,OrganizationManualSchemaTestSupport.java,OrganizationFlowTest.java,OrganizationRollbackTest.java}},__rootArtifactId__-infrastructure/src/test/java/infrastructure/{OrganizationRabbitMqContractTest.java,teaching/repo/{GradeRepositoryImplTest.java,SchoolClassRepositoryImplTest.java},user/repo/{RolePermissionRepositoryImplTest.java,UserRepositoryImplTest.java}}}`

- Purpose: 固定wire compatibility、Common persistence、manual schema、tenant isolation和业务regression。
- Symbols: >2^53 HTTP/GraphQL IDs、Proto descriptors、4Service/8DAO/PO、flow/rollback/Rabbit、manual 001..004。
- Repository evidence: named Open tests已覆盖这些边界；只需增强Common/tenant/schema断言。
- Dependencies and consumers: Files 2-5、generated Web Open runtime。
- Why now: 在source修改前同时锁定wire unchanged和persistence changed。
- Contract/signature changes: tests only；descriptor/route/status/field names baseline不改。
- Input/output and state mapping: decimal HTTP/GraphQL和int64 Proto -> Long -> PO；tenant/user context -> technical fields；event IDs decimal-safe。
- Error and edge behavior: invalid lexical/nonpositive、missing/forged context、cross-tenant、manual unknown rows、transaction failure均assert zero unsafe effects。
- Standards impact: `MC-VALID-001`,`MC-JSON-001`,`MC-MODEL-001`,`MC-CONFIG-001`,`MC-PATTERN-001`,`MC-TEST-001` — complete Web Open RED/GREEN。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 6`,`Rule 7`,`Rule 9`,`Rule 10`,`Rule 11` — carriers/wire/model/config/time/pattern被测试。
- Implementation pseudocode:

```java
assert HTTP and GraphQL accept/return canonical "9007199254740993" without precision loss;
assert Proto service/method/field numbers and int64 types match baseline descriptors;
assert four Services/eight DAO/PO/infra impl use exact Common contracts and annotations;
apply manual 001..004, then execute tenant isolation, flow, Rabbit and rollback cases;
assert invalid context/input causes no DAO write/event/cache mutation and no JPA/Flyway/direct MP exists.
```

- Verification contribution: Step 8 contract RED/GREEN和final E2E-like generated component tests。
- After this file: current direct MP/manual 001/002 tree RED only forapproved changes。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/OrganizationApplication.java}}`

- Purpose: 收敛Common dependency/Lombok/DAO scan且保留Open Web stack。
- Symbols: remove MP 3.5.17；domain Common Starter；infra no direct MP；user/teaching DAO scan；1.18.46。
- Repository evidence: current POMs含direct MP和Open Web/Proto/Nacos/DTP dependencies。
- Dependencies and consumers: Step 1/2 APIs、File 1、Files 3-5。
- Why now: source/tests compile prerequisite。
- Contract/signature changes: persistence dependency only；Springdoc/HTTP/GraphQL/Dubbo Triple/Proto/Rabbit/DTP不改。
- Input/output and state mapping: module graph不变；domain approved technical upper bound；facade generated contract不变。
- Error and edge behavior: direct MP/JPA/Flyway、mapper broad scan、plugin drift在dependency/proto/verifier gate失败。
- Standards impact: `MC-DEP-001`,`MC-BEAN-001`,`MC-ARCH-001`,`MC-SCOPE-001` — no new runtime dependency。
- Literal rule enforcement: `Rule 4`,`Rule 5`,`Rule 11` — Qualifier propagation和exact Open Web structure。
- Implementation pseudocode:

```xml
remove mybatis-plus.version/direct starter and align Lombok to 1.18.46;
add Common MP Starter only through Egon BOM in domain; keep Common ID/DTP and Open framework deps;
preserve protobuf/grpc/Dubbo/Springdoc build plugins and generated-source ownership;
MapperScan exact ${package}.infrastructure.{user,teaching}.repo.dao; retain ArchUnit test scope;
```

- Verification contribution: dependency/testCompile/protoc/OpenAPI context。
- After this file: POM/wiring correct；old persistence source remainsRED。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`

- Purpose: 实现与Step 7一致的4 generic Services、8PO/DAO、infra impl、BaseConverters并保持all Open adapters。
- Symbols: mapper->DAO、PO exact annotations/table names organization_*、domain impl move、RepositoryImpl merge/delete、all Long/wire/event/client mappings。
- Repository evidence: current Open source已Long且有full adapter/repo tests；Step 7 target是parity reference。
- Dependencies and consumers: File 2 graph、generated Proto、HTTP/GraphQL/Rabbit、manual SQL File 4。
- Why now: minimum vertical Java/XML migration after contracts/POM。
- Contract/signature changes: persistence/package only；route/method/field/descriptors unchanged；Role仍companion DAO。
- Input/output and state mapping: decimal/int64 -> Long -> domain/PO；technical fieldsframework fill；caches/events/idempotency timing保持。
- Error and edge behavior: cross-tenant/duplicate/member mismatch/downstream rollback/missing context保持并由File 1验证。
- Standards impact: `MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001`,`MC-LOG-001`,`MC-BEAN-001`,`MC-JSON-001`,`MC-TIME-001`,`MC-PATTERN-001` — exact target code shape。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 6`,`Rule 9`,`Rule 10`,`Rule 11` — no manual copy/ctor/wire drift。
- Implementation pseudocode:

```java
make User/Permission/Grade/SchoolClass interfaces generic Egon Services and move impls to infrastructure;
rename eight Mapper interfaces to DAO and bind exact *PO extends EgonModel; update all XML namespaces/resultMaps;
annotate PO with exact user-approved Lombok set, organization_* TableName and explicit business TableFields;
wire @Slf4j @Service(named) @RequiredArgsConstructor impls with final @Qualifier primary/companion DAO, BaseConverters and ports;
preserve HTTP/GraphQL/Proto/Rabbit mappings, Long precision annotations, transactions/cache/event/idempotency ordering;
delete pure RepositoryImpl and old mapper directories only after consumer scan is zero.
```

- Verification contribution: all Web Open behavior/wire/persistence tests。
- After this file: Java/XML target complete；manual/config/generation待Files 4-5。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/{master-data/003__migrate_organization_master_data_to_egon_model.sql,shard/004__migrate_organization_sharded_to_tenant_model.sql}`

- Purpose: 交付organization Open与Step 7同target schema的manual sequence。
- Symbols: 8logical/10physical table shapes、tenant active/link indexes、seven fields。
- Repository evidence: immutable 001/002、PostgreSQL/ON CONFLICT style、ManualSchema support。
- Dependencies and consumers: File 3 PO/XML、File 1 tests、File 5 runbook/metadata。
- Why now: mapping确定后写精确forward SQL。
- Contract/signature changes: target organization_* names/Long/technical fields/tenant route；no Flyway/app execution。
- Input/output and state mapping: known seed deterministic；master singleton tenant-scoped；classes/members same tenant node；UTC audit。
- Error and edge behavior: unknown identity/tenant/duplicates/orphans abort；old scripts unchanged；per physical DB manual execution。
- Standards impact: `MC-TIME-001`,`MC-CONFIG-001`,`MC-SCOPE-001`,`MC-TEST-001` — schema/time/route proof。
- Literal rule enforcement: `Rule 7`,`Rule 10`,`Rule 11` — Open manual target与PO/profiles一致。
- Implementation pseudocode:

```sql
003 preflights and migrates users/roles/permissions/user_roles/role_permissions/grades to organization_*;
backfill seven fields and add tenant-scoped active email/code/link constraints;
004 migrates both school_classes/school_class_users suffixes with tenant_id route and relation indexes;
validate no null/duplicate/orphan and exact suffix parity; raise on unknown historical mapping;
do not edit 001/002, add Flyway dependency, or execute from application startup.
```

- Verification contribution: manual ordered schema fixture/parity/no-Flyway。
- After this file: schema deliverable complete；File 5 closes config/docs。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/README.md,src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`

- Purpose: 关闭Web Open tenant route/profile/manual/metadata/verifier/docs并保护wire。
- Symbols: master none、two shard tables tenant dual route、all profiles、003/004、Common-only/no-wire-drift assertions。
- Repository evidence: current Open config/README/metadata/verifier覆盖direct MP/manual/Proto/HTTP baseline。
- Dependencies and consumers: Files 1-4、archetype plugin/DBA/generated users。
- Why now: final source/schema可同步所有consumer contracts。
- Contract/signature changes: persistence/route/resource paths only；HTTP/GraphQL/Proto/OpenAPI routes/field numbers保持。
- Input/output and state mapping: trusted tenant same-node；wire decimal/int64 exact；manual execution/runbook explicit。
- Error and edge behavior: profile/route/descriptor/file drift、JPA/Flyway/direct MP/old Mapper/PO suffix阻断build。
- Standards impact: `MC-CONFIG-001`,`MC-PATTERN-001`,`MC-ARCH-001`,`MC-JSON-001`,`MC-TEST-001` — final Open Web gate。
- Literal rule enforcement: `Rule 5`,`Rule 6`,`Rule 7`,`Rule 9`,`Rule 11` — Jackson-only、all profiles、Strategy、exact Open modules。
- Implementation pseudocode:

```yaml
master organization_* tables stay none strategy; school_classes/school_class_users dual-route by tenant_id;
copy exact Common MP/app.sharding key set into application/dev/test/prod while preserving environment values;
validate same-tenant parent/link co-location and reject missing or nonpositive tenant contexts;
```
```text
manual README documents backup/preflight/001-004/per-node verification/forward-fix;
metadata/verifier require DAO/PO/infra impl/XML/manual files and unchanged HTTP/GraphQL/Proto tests;
forbid JPA/Flyway/direct MP/repo.mapper/repo.impl/domain impl/String business ID.
```

- Verification contribution: Web Open archetype IT/generated verify/wire/manual/route/config。
- After this file: all six archetypes code slicescompleteGREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-web-open -am clean integration-test`
- Expected result: exit 0；HTTP/GraphQL>2^53、Proto descriptors、Rabbit/flow/rollback、Common MP/manual/route/verifier全过。
- Failure returns to: File 1 contract，File 2 dependency/build，File 3 mapping/wire/business，File 4 schema，File 5 route/metadata/docs。
- Completion criteria: generated Web Open `clean verify`，wire unchanged，4Services/8PO/DAO/tenant target与Step 7一致。
- Rollback: manual执行前source revert；执行后forward-fix，wire descriptor/route不可临时回退。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-adapter/src/test/java/adapter/{OrganizationGraphQlContractTest.java,OrganizationHttpErrorContractTest.java},__rootArtifactId__-facade/src/test/java/facade/ProtoContractTest.java,__rootArtifactId__-starter/src/test/java/{architecture/OpenArchitectureTest.java,starter/{OrganizationDataSourceModeTest.java,OrganizationManualSchemaTestSupport.java,OrganizationFlowTest.java,OrganizationRollbackTest.java}},__rootArtifactId__-infrastructure/src/test/java/infrastructure/{OrganizationRabbitMqContractTest.java,teaching/repo/{GradeRepositoryImplTest.java,SchoolClassRepositoryImplTest.java},user/repo/{RolePermissionRepositoryImplTest.java,UserRepositoryImplTest.java}}}`; `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,lombok.config,__rootArtifactId__-domain/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/{pom.xml,src/main/java/starter/OrganizationApplication.java}}`; `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,infrastructure,adapter}/src/main/java/{domain,application,infrastructure,adapter}/**`; `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/{master-data/003__migrate_organization_master_data_to_egon_model.sql,shard/004__migrate_organization_sharded_to_tenant_model.sql}`; `egon-cola-archetypes/egon-cola-archetype-web-open/{src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/{java/infrastructure/config/datasource/**,resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml,sharding/shardingsphere-sharding.yml,sharding/shardingsphere-sharding-readwrite.yml}},src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/README.md,src/main/resources/META-INF/maven/archetype-metadata.xml,src/test/resources/projects/basic/verify.groovy,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`
- Commit: `refactor(archetype-web-open): adopt common mp and preserve wire contracts`

### Step 9 — 关闭跨族code-style与atomic release门禁

- Requirements: `REQ-001`, `REQ-002`, `REQ-020`, `REQ-021`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`
- Dependencies: Steps 1-8
- Baseline state: six archetype Steps各自GREEN；Open code-style仍写domain不得依赖MP并描述Mapper ownership，和用户批准target不一致。
- Observable outcome: repository cross-family guide准确描述Common MP/domain generic/infra DAO/PO/impl/manual SQL，完整reactor和six generated verification形成release evidence。
- End state: 9 commits可按顺序review/merge；Plan执行目标全部完成，仍不自动start runtime。
- Test-first gate: Not applicable — 唯一production change是living code-style文档；代码行为已由Steps 1-8 RED/GREEN覆盖，本Step先做doc token/link static review再跑full regression。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/open-source-archetype-code-style.md`

- Purpose: 修订Open contributor guide的domain dependency、persistence/mapper safety和manual SQL review规则。
- Symbols: Package/module ownership table；Persistence and mapper safety；Manual SQL/schema review；validation/conversion/Bean examples。
- Repository evidence: primary明确Depends On这些sections；current guide的domain no-MP/Mapper wording与DEC-001/DAO target冲突。
- Dependencies and consumers: all six final templates/verifiers/readmes；maintainers/reviewers。
- Why now: 只有六个actual trees稳定后才能用真实path/symbol写guide并跑cross-family scan。
- Contract/signature changes: 文档声明domain只可依赖Common MP Starter的EgonModel/IService上界，不能具体infra；infra owns DAO/PO/impl；Open no Flyway；exact PO/Bean annotations。
- Input/output and state mapping: guide列出Long/tenant/audit/isDeleted、manual 001..004 operator flow；不新增runtime input/state。
- Error and edge behavior: 明确unknown migration rows fail、tenant context fail-closed、wire compatibility、no auto-DDL；禁止direct MP/JPA/Flyway。
- Standards impact: `MC-ARCH-001`,`MC-REUSE-001`,`MC-DEP-001`,`MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001`,`MC-BEAN-001`,`MC-CONFIG-001`,`MC-PATTERN-001`,`MC-SCOPE-001` — guide与实现/Manual Checks逐项一致。
- Literal rule enforcement: `Rule 1`,`Rule 2`,`Rule 3`,`Rule 4`,`Rule 5`,`Rule 6`,`Rule 7`,`Rule 9`,`Rule 10`,`Rule 11` — exact suffix、validation、PO override、DI、Jackson、profiles、patterns、time和architecture全部写明。
- Implementation pseudocode:

```text
update ownership table: domain may import only Common MP EgonModel/EgonColaIService generic contracts,
never concrete PO/DAO/ServiceImpl; infrastructure owns repo.dao/repo.po/service.impl and XML;
document PO exact Data/NoArgs/AllArgs/Builder/Accessors + MP annotations and no RequiredArgs/SuperBuilder;
document business Beans Slf4j/named/RequiredArgs/final Qualifier and MapStruct BaseConverter handoffs;
document positive Long, trusted tenant, java.time/Jackson/profile parity and manual 001..004 fail-fast flow;
forbid JPA/Flyway/direct official MP/local base classes/auto schema execution.
```

- Verification contribution: cross-family doc/source/forbidden/link review和full reactor interpretation。
- After this file: docs与all final source contracts一致，ready for full gate/commit。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test`
- Expected result: exit 0；facades + six archetype generation/verifiers全部成功；随后six generated project directories各执行`./mvnw -B -ntp clean verify` exit 0；`git diff --check`通过。
- Failure returns to: owning Step 1-8 based on first failing module/test；File 1 only fordoc link/token mismatch；不得在Step 9绕过family test。
- Completion criteria: full reactor + six generated verify + forbidden/config/migration checksum gates成功，cross-family guide同步，无runtime启动。
- Rollback: doc可单独revert但release不得保留与source冲突的guide；代码rollback按owning Step/migration forward-fix边界。
- Commit paths: `egon-cola-archetypes/open-source-archetype-code-style.md`
- Commit: `docs(archetypes): document unified common mp contract`

## 8. Test, Validation, and Quality Gates

所有命令都是未来执行指令，不是本Plan已运行的代码证明。RED gate只接受“目标行为尚缺失”导致的失败；dependency resolution、fixture syntax、外部服务不可用不是有效RED。

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| 1 RED Common | repository root | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonModelTest,EgonModelActiveRecordParityTest,EgonColaServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` after Step 1 Files 1-2 and Resume correction only | business-only builder/constructor/parity | fails only for Service base missing abstract seam/super no-arg；must not report ModelBuilder | Step 1 Files 1-2/Resume correction if fixture/baseline issue，File 3 if expected seam | `REQ-007`-`010`; reactor module |
| 2 GREEN Common | repository root | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-mybatis-plus-spring-boot-starter -am test`；随后`git diff --exit-code 3be897e5cb4781890bfbac3104512e6e73bb943a -- egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java` | Common full unit/H2 + unchanged EgonModel | Maven exit 0；all named tests pass；EgonModel diff exit 0 | Step 1 owning file；若diff失败回Resume correction | `REQ-007`-`010`,`016`; embedded H2/source |
| 3 RED facades | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-organization-facade,egon-cola-evaluation-facade -Dtest=OrganizationFacadeContractTest,EvaluationFacadeContractTest test` after Step 2 File 1 | Java Long/validation contract | fails on current String identity, not setup | Step 2 File 1/2-4 | `REQ-012`-`014`; module |
| 4 GREEN facades | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-organization-facade,egon-cola-evaluation-facade -am test` | both canonical facades | exit 0 | Step 2 | `REQ-012`-`014`,`025`; module |
| 5 Light legacy | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-light -am clean integration-test` | Step 3 archetype/generated tests | exit 0; verify.groovy passes | Step 3 first failing file | Light requirement subset; generated/H2 |
| 6 Light Open | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-light-open -am clean integration-test` | Step 4 Open/manual | exit 0; no JPA/Flyway/direct MP | Step 4 | Open Light subset; generated/H2/static SQL |
| 7 Service legacy | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-service -am clean integration-test` | Step 5 RPC/MQ/evaluation | exit 0; no Web/JPA | Step 5 | Service subset; generated/H2 |
| 8 Service Open | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-service-open -am clean integration-test` | Step 6 Proto/manual | exit 0; descriptor compatible | Step 6 | Service Open subset; generated/protoc/H2 |
| 9 Web legacy | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-web -am clean integration-test` | Step 7 HTTP/GraphQL/organization | exit 0; decimal Long contracts pass | Step 7 | Web subset; generated/H2 |
| 10 Web Open | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl egon-cola-archetype-web-open -am clean integration-test` | Step 8 HTTP/GraphQL/Proto/manual | exit 0; wire descriptors/routes pass | Step 8 | Web Open subset; generated/protoc/H2 |
| 11 Full reactor | repository root | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test` | two facades + six archetypes | exit 0; all modules and verifiers success | first failing Step 2-8 | `REQ-001`-`026`; generated/H2 |
| 12 Generated Light | `egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic` | `./mvnw -B -ntp clean verify` | generated Light | exit 0 | Step 3 | `REQ-024`; generated project |
| 13 Generated Light Open | `egon-cola-archetypes/egon-cola-archetype-light-open/target/test-classes/projects/basic/project/basic` | `./mvnw -B -ntp clean verify` | generated Light Open | exit 0 | Step 4 | `REQ-024`; generated project |
| 14 Generated Service | `egon-cola-archetypes/egon-cola-archetype-service/target/test-classes/projects/basic/project/student-management-evaluation` | `./mvnw -B -ntp clean verify` | generated Service | exit 0 | Step 5 | `REQ-024`; generated project |
| 15 Generated Service Open | `egon-cola-archetypes/egon-cola-archetype-service-open/target/test-classes/projects/basic/project/student-management-evaluation` | `./mvnw -B -ntp clean verify` | generated Service Open | exit 0 | Step 6 | `REQ-024`; generated project |
| 16 Generated Web | `egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization` | `./mvnw -B -ntp clean verify` | generated Web | exit 0 | Step 7 | `REQ-024`; generated project |
| 17 Generated Web Open | `egon-cola-archetypes/egon-cola-archetype-web-open/target/test-classes/projects/basic/project/student-management-organization` | `./mvnw -B -ntp clean verify` | generated Web Open | exit 0 | Step 8 | `REQ-024`; generated project |
| 18 Forbidden source/deps | repository root | `rg -n 'spring-boot-starter-data-jpa|jakarta\.persistence|JpaRepository|EntityManager|mybatis-plus\.version|mybatis-plus-spring-boot3-starter|lombok\.experimental\.SuperBuilder|UuidV7BucketShardingAlgorithm|java\.util\.(Date|Calendar)|SimpleDateFormat' egon-cola-archetypes/egon-cola-archetype-{light,service,web,light-open,service-open,web-open}/src` with negative-test/doc allowlist review | six source trees | no production/runtime forbidden hits；no PO SuperBuilder；negative assertions documented | owning Step | `REQ-001`,`002`,`007`,`013`,`023`; static |
| 19 Exact contract inventory | repository root | source/reflection verifier checks 42 PO,42 DAO,24 interface,24 impl,21 logical/29 physical shapes,12 new scripts and six profile key sets | counts/names/annotations | every expected item exactly once | Steps 3-8 | `REQ-003`-`007`,`016`-`021`; static |
| 20 Immutable migration | repository root | `git diff --exit-code 3be897e5cb4781890bfbac3104512e6e73bb943a -- 'egon-cola-archetypes/**/V20260726_*.sql' 'egon-cola-archetypes/**/001__create_*_schema.sql' 'egon-cola-archetypes/**/002__create_*_schema.sql'` | historical SQL bytes | exit 0 | owning schema Step | `REQ-020`-`022`; source |
| 21 Worktree/format | repository root | `git diff --check && git status --short && git diff --cached --name-only` | whitespace/scope/staging | no diff errors; staged paths exactly current Step | owning Step | `REQ-024`-`026`; source |
| 22 User-controlled PostgreSQL | disposable PostgreSQL 17 / each physical location | follow each generated manual/Flyway runbook: backup, preflight, apply, null/duplicate/orphan/suffix/index/route verification | dialect/DDL/runtime | all counts expected and queries route correctly | schema/config owning Step | `REQ-016`-`022`; external runtime, not automatic |

配置key parity必须解析YAML结构而不是比较文本值：同一family的base/dev/test/prod核心keys集合相等，credentials/URLs/profile values可不同。H2或parser成功不等于真实PostgreSQL/ShardingSphere/Nacos/Redis/Rabbit/Dubbo/browser proof。

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Migration file order

1. 历史 legacy `V20260726_001/_002` 和Open `001/002`只读/checksum。
2. Light legacy master `V20260825_001`与shard `V20260825_002`分别进入两个独立Flyway histories；Light Open operator在已有001/002后分别执行master `003`、shard `004`。
3. Evaluation按同一独立location顺序执行legacy V20260825 master/shard或Open manual 003/004。
4. Organization按同一独立location顺序执行legacy V20260825 master/shard或Open manual 003/004。
5. 每个location先backup/profile/preflight，后apply，再校验null/duplicate/orphan/index/suffix parity；任一失败停止该项目rollout，不继续application发布。

master/shard文件不属于同一个`flyway_schema_history`，所以每location一个新文件符合“一个数据库变更一个migration”。Open没有Flyway，manual编号只是operator顺序；应用永不加载这些文件。

### 9.2 Compatibility matrix

| Consumer/writer | Old schema/app | New schema/Common MP app | Decision |
| --- | --- | --- | --- |
| String/UUID Java facade consumer | compatible old only | source incompatible | canonical facade与legacy templates必须同release升级；无dual signature |
| HTTP/GraphQL decimal ID consumer | prior String field | same field/route，lexical收窄positive decimal | wire field names preserved；UUID/noncanonical client必须升级 |
| Open Proto consumer | current int64/field numbers | same int64/field numbers | descriptor compatible；implementation only changes |
| JPA/old direct MP writer | old schema | unsafe after Long/tenant migration | migration前停止，禁止并行old writer |
| Common MP writer | unavailable old contract | required new schema | schema/preflight/route先完成再部署 |
| External generated real database | unknown UUID/tenant data | not automatically migratable | separate offline mapping/re-shard Spec/runbook required |

### 9.3 Rollout

1. 按Step 1发布/安装同版本Common component artifacts；验证57/AR/builder/constructor contracts。
2. 发布Step 2 canonical facades；Service/Web template artifacts在同一Egon release中消费，不支持String/Long mixed reactor。
3. 按Light、Service、Web分别生成/验证legacy/open projects；不能以一个family成功替代另一个。
4. 对目标fresh或明确mapped数据库先执行schema流程；验证每个physical node后部署Common MP application。
5. 验证trusted tenant/user Provider integration、same-tenant route、cross-tenant denial、audit/logic delete和business regression。
6. 运行Step 9 full reactor、six generated verify和static gates；只在全部exit 0时发布archetype release。
7. 用户自行决定何时启动生成项目/外部系统；执行Plan的agent不自动start。

### 9.4 Rollback and forward fix

- SQL执行前：path-limited source revert对应Step即可。
- SQL执行后但application未写：优先保留target schema并修复/重部署new app；不要编辑Flyway history或回旧JPA writer。
- new Long/tenant rows已写：application source rollback到UUID/String/JPA不安全；restore verified backup或发布新forward migration/app fix。
- Open manual SQL出现问题：按runbook backup restore；若已继续写入则新建后续manual sequence，不改003/004。
- Proto/HTTP field names/field numbers不回滚改变；wire regression直接回到Step 2/6/8 source fix。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | Spec §4 | 3-9 | six POM/source/verifier sets | gates 5-21 | no JPA/Hibernate production contract |
| `REQ-002` | Spec §4/§6 | 3-9 | six root/domain/infra POMs,lombok configs | dependency/forbidden/full reactor | Common-only MP,1.18.46 |
| `REQ-003` | Spec §4/§8.4 | 3-8 | 24 domain Service interfaces/port moves | architecture/count gates | service package only Egon Services |
| `REQ-004` | Spec §4/§8.4 | 3-8 | 24 infra implementations | context/architecture/generated tests | exact generic binding/Beans |
| `REQ-005` | Spec §4/§10.3.4 | 3-8 | 42 DAO/XML sources | mapper/reflection/XML gates | EgonColaMapper + namespaces |
| `REQ-006` | Spec §4/§10-§11 | 3-8 | 42 PO sources | annotation/schema/count gates | EgonModel inheritance |
| `REQ-007` | Spec §4/§10.3 | 1,3-8 | Common fixtures + all PO/lombok configs | builder/delombok/architecture | exact PO annotations；business-only Builder；no RequiredArgs/SuperBuilder |
| `REQ-008` | Spec §4/§10.3 | 1 | unchanged Common EgonModel + tests/docs | Common gate + source diff | no ModelBuilder；field/constructor/AR parity |
| `REQ-009` | Spec §4/§7/§9 | 1,3-8 | Common base + 24 impl | compile/reflection/context | no handwritten super ctor |
| `REQ-010` | Spec §4/§6.2 | 1,3-8 | concrete ServiceImpl/Beans/lombok configs | context/log/source scan | named Slf4j RequiredArgs Qualified Beans |
| `REQ-011` | Spec §4/§10.4 | 2-8 | MapStruct converters | mapping/list/null tests | BaseConverter,no manual copy |
| `REQ-012` | Spec §4/§9-§10 | 2-8 | DTO/Command/Query/Service/DAO/tests | validation positive/negative/JDBC-zero | every handoff validated |
| `REQ-013` | Spec §4/§9.3 | 2-8 | facade/templates/PO/SQL | facade/identity/forbidden gates | positive Long/Snowflake |
| `REQ-014` | Spec §4/§9.3 | 2-4,7-8 | HTTP/GraphQL/Jackson contracts | >2^53/invalid lexical tests | decimal exact wire |
| `REQ-015` | Spec §4/§7.3 | 3-8 | context/filter/event/service/route files | missing/forged/concurrent tenant tests | trusted fail-closed context |
| `REQ-016` | Spec §4/§11 | 1,3-8 | EgonModel/PO/12 scripts | 29-shape schema parity | seven fields exact |
| `REQ-017` | Spec §4/§11 | 3-8 | twelve sharding YAML/algorithms | same-node/positive route tests | tenant database+table key |
| `REQ-018` | Spec §4/§11 | 3-8 | master rules/XML/SQL | none strategy + TenantLine tests | singleton tenant-scoped master |
| `REQ-019` | Spec §4/§11 | 3-8 | indexes/DAO XML/Services | duplicate/link/delete tests | tenant active uniqueness |
| `REQ-020` | Spec §4/§11/§16 | 3,5,7,9 | six new legacy migrations | checksum/Flyway/full gates | old immutable,new per location |
| `REQ-021` | Spec §4/§11/§16 | 4,6,8,9 | six new manual scripts/runbooks | ordered parser/PostgreSQL/static | no Flyway/manual 003/004 |
| `REQ-022` | Spec §4/§11/§16 | 3-8 | migration preflight/tests/runbooks | unknown-row abort | no guessed production mapping |
| `REQ-023` | Spec §4/§6/§10 | 1-9 | POM/converters/validation/docs | reuse/dependency/source gates | no duplicate common capability |
| `REQ-024` | Spec §4/§8/§14 | 3-9 | six metadata/verifiers/READMEs/living docs | reactor + six generated verify | generation contract synchronized |
| `REQ-025` | Spec §4/§7/§14 | 1-9 | all behavior slices | state/cache/event/transaction/wire regression | preserved business outcome |
| `REQ-026` | Spec §4/§14-§16 | 1-9 | validation commands/runbooks | process/command audit | no automatic runtime start |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `DEC-001` | domain Common MP dependency/generic Service、infra impl/DAO/PO | Steps 3-8 | user confirmation + Spec DEC-001 | User | Closed — implement exact profile amendment |
| `DEC-002` | PO no RequiredArgs/SuperBuilder；普通Builder只含业务字段 | Steps 1,3-8 | user correction + compile `ModelBuilder` evidence + Spec REQ-007/DEC-007 | User/Maintainer | Closed — exact annotation/delombok/compile tests |
| `DEC-003` | all business identity Long/Snowflake | Steps 2-8 | user confirmation + Spec DEC-002 | User | Closed — breaking coordinated release |
| `DEC-004` | sharded tables route by tenant_id；master singleton | Steps 3-8 | user confirmation + ASM-003 | User | Closed — exact YAML/schema tests |
| `RISK-001` | SuperBuilder被重新加入EgonModel/PO并再次触发缺失ModelBuilder | Steps 1,3-8 | actual Step 1 compiler failure | Maintainer | Mitigated — forbidden scan + compile fixture + unchanged EgonModel diff gate |
| `RISK-002` | inherited CRUD exposed through domain Service | Steps 3-8 | generic IService contract | Maintainer | Mitigated — architecture/verifier forbids use outside approved paths |
| `RISK-003` | tenant distribution skew/hot slot | Steps 3-8/runtime | NodeMap strategy; no live distribution | Operator | Accepted operational risk — measure after deployment; topology expansion separate Spec |
| `RISK-004` | unknown external UUID/tenant data cannot migrate automatically | schema Steps | source-only template evidence | DBA/User | Mitigated — preflight abort/offline mapping/backup |
| `RISK-005` | ShardingSphere parser/TenantLine custom XML interaction | Steps 3-8 | current XML + future BoundSql tests | Maintainer | Mitigated for release by BoundSql/H2; real topology user-controlled |
| `RISK-006` | large vertical archetype commits | Steps 3-8 | POM/source/schema compile inseparability | Maintainer | Mitigated — per-family isolation, focused files/order/tests, one semantic result |
| `RISK-007` | historical source documents have duplicate REQ IDs/conflicting domain purity | §2/Step9 | Common/Open/code-style metadata | Maintainer | Closed — primary consolidated Spec is sole executable namespace; exclusions explicit |

No unresolved design blocker remains。失败的SuperBuilder实现仍作为六个未提交文件中的中断现场保留，本轮不改代码；恢复执行时按Step 1 Resume correction与File 1-3纠正并重新RED/GREEN，File 4同步文档。Plan status保持`Review`，等待用户审核并重新确认修订后的执行合同。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

本Plan覆盖用户要求的六模板Spring Data JPA全迁移、Open direct MP收敛、Common MP Starter/common-core复用、domain Service/infra impl/DAO/PO、EgonColaServiceImpl Lombok重构、PO exact annotations/业务字段Builder/MP annotations及SuperBuilder禁止规则、Long和tenant ShardingSphere；非持久ports、其余layers、schema/config/metadata/verifier/docs按Manual Checks完整列入。

### 12.2 Spec consistency

Plan不新增public route/Proto field/table/module/frontend/cache/job/tenant request。Simplicity audit确认无fetch-then-forward或caller-supplied tenant。四个`PLAN-CLAR`只处理finite path notation、existing ArchUnit proof、managed Lombok dependency和approved column rename，均不改变业务/架构。SuperBuilder阻断已由primary Spec `EVD-017/DEC-007`关闭；Open旧domain purity文本由更晚的user DEC-001显式覆盖。

### 12.3 Repository executability

所有主要paths/symbols/POM/profile/migration/metadata/verifier/generated output directories均在`main@3be897e5`核对；每Step给出baseline/end state、RED/GREEN、ordered files、mapping/errors、cwd、command、failure return、exact commit paths和rollback。unrelated dirty docs明确隔离，target output不提交。

### 12.4 Test and release completeness

Common/facade先行，六个family vertical slices分别验证，最终root reactor加six generated verify；migration checksum/manual sequence、Long wire、tenant/route、validation、conversion、Bean/config/time/pattern/behavior回归均有owning gate。PostgreSQL/live topology/外部系统/browser仍明确属于用户runtime边界。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable | PASS | §4.7、Steps 3-8 architecture tests、six metadata/verifiers | exact Light single-module and Service/Web seven-module profiles preserved | None |
| `MC-REUSE-001` | Applicable | PASS | §4.7 capability ledger、Common Starter/BaseConverter/ValidationUtils/NodeMap paths | existing Spring/Egon/module capabilities reused first | None |
| `MC-DEP-001` | Applicable | PASS | Steps 1-8 POM blocks、dependency gates | no new runtime dependency；legacy ArchUnit is existing test proof only | None |
| `MC-NAME-001` | Applicable | PASS | §3/§5、Steps 3-8 42 PO/DAO and 24 Service pairs | semantic suffix inventory complete；`*Po/*Mapper/JPA` removed | None |
| `MC-VALID-001` | Applicable | PASS | Steps 2-8 handoff pseudocode/tests | external/application/domain/DAO/event validation and groups planned；no phone field | None |
| `MC-MODEL-001` | Applicable | PASS | Step 1 builder fixtures、Steps 3-8 PO blocks、Spec `EVD-017/DEC-007` | records stay simple；PO exact Data/NoArgs/AllArgs/Builder/Accessors + MP annotations；builder仅业务字段；无RequiredArgs/SuperBuilder | None |
| `MC-CONVERT-001` | Applicable | PASS | capability ledger、Steps 3-8 converter pseudocode/gates | every affected conversion uses MapStruct/MapStructPlus + BaseConverter | None |
| `MC-LOG-001` | Applicable | PASS | Steps 1,3-8 ServiceImpl/business Bean blocks | concrete business classes use Slf4j and safe logs；no PO logging | None |
| `MC-BEAN-001` | Applicable | PASS | six lombok.config、Step 1 seam、Steps 3-8 context tests | stable Bean names、RequiredArgs、final Qualifier fields and propagation planned | None |
| `MC-UTIL-001` | Applicable | PASS | capability ledger、gate 18 | no new helper/non-allowlist dependency or BeanUtils conversion | None |
| `MC-JSON-001` | Applicable | PASS | Steps 2,3,4,7,8 wire pseudocode；gates >2^53/Proto | Boot Jackson only；decimal ID/descriptor compatibility explicit | None |
| `MC-TIME-001` | Applicable | PASS | EgonModel Instant、PO/SQL/time pseudocode、gate18 | java.time/TIMESTAMPTZ/ordering exact；no util Date additions | None |
| `MC-CONFIG-001` | Applicable | PASS | Steps 3-8 four profiles/two sharding YAML；gate key-set method | same key structure per family with value differences allowed | None |
| `MC-PATTERN-001` | Applicable | PASS | Step 1 Template Method、Steps 3-8 NodeMap Strategy；§4.5 rejects ceremonial patterns | all Complex technical variation has approved pattern；simple CRUD direct | None |
| `MC-SCOPE-001` | Applicable | PASS | §5 tree、§6 dirty exclusions、nine path-limited commits | only Common/facades/six templates/docs necessary cone touched | None |
| `MC-TEST-001` | Applicable | PASS | §8 22 ordered gates、each Step RED/GREEN/failure return | focused/module/archetype/generated/static/manual boundaries complete | None |
| `MC-BLOCKER-001` | Applicable | PASS | Spec `EVD-017/DEC-007`、§4.5、Step 1、§8 gates、§11 risks | ModelBuilder阻断已从设计/Plan移除；无未知major decision；中断代码只待重新批准后按Plan纠正 | None |

### 12.6 Final verdict

PASS — Ready for user review

该PASS只表示Plan内部可执行且Manual Checks闭合；Plan状态仍为`Review`。用户明确批准本Plan并调用执行Skill前，不得开始修改production/test/config/SQL，也不得执行migration或启动项目。
