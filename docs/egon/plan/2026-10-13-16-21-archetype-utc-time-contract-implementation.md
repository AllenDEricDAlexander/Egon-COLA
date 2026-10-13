# Archetype UTC 时间契约实施 Plan

| Field | Value |
| --- | --- |
| Document | `2026-10-13-16-21-archetype-utc-time-contract-implementation.md` |
| Template Version | `5` |
| Status | `Completed` |
| Created | `2026-10-13 16:21 Asia/Shanghai` |
| Updated | `2026-10-13 20:50 Asia/Shanghai` |
| Owner | 用户（设计/实施批准）；Codex（计划编写） |
| Repository | `Egon-COLA` |
| Scope | 七套 source-projects、Code Generator、Common MP-SDJ及必要生成校验；实施与验收记录 |
| Source Requirement | 2026-10-13 用户先审核Spec和Plan，随后批准逐Step实施；最终一个聚焦commit；SQL例外原位、deletedAt兼容、唯一键差距排除、保留并行修改、不启动项目 |
| Baseline Revision | `main @ 2f01b7937f3e2d68fe66d62ec7bed5b913a07b6d`；编写时 52 项既有工作区变更；逐文件当前 SHA 见 §5 |
| Implements Spec | [UTC 时间契约修订](../spec/2026-10-13-14-07-archetype-utc-time-contract-amendment.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-10-13 16:21 Asia/Shanghai`（只记录用户接受与 Related Plans；设计正文未改） |
| Effective Specs | [主 UTC 修订 Spec](../spec/2026-10-13-14-07-archetype-utc-time-contract-amendment.md)；完整有效集合及限定继承范围见 §2.2：`2026-08-27-20-31-archetype-two-stage-source-generation.md`、`2026-09-12-21-11-mybatis-repository-cqrs-postgresql-evolution.md` |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

按已接受的 UTC 修订 Spec，分 7 个依次验收的 Step 实施；批准时源目标清单和必要依赖补充见§5/PLAN-CLAR-004～011。最终源码差分为150路径（两BO rename各含旧/新路径），5个计划测试目标实证无需修改；临时检查脚本已删除。先锁定生成器与 MP 类型矩阵，再完成两 Light 垂直链、两 Service 精度/SQL，最后配置、派生与回归。此文件保留批准时的实施路径，并在末尾记录逐Step执行、必要事实修正及最终验收结果。

用户 AGENTS 的“一项逻辑任务至多一个 commit”优先：每 Step 保留验收记录，全部通过后才统一提交 UTC 时间任务的精确路径。用户随后明确批准实施；最终源码实施完成与全Spec验收分开记录，未运行的真实PG/端口测试不计为通过。

## 2. Target Spec and Effective Design

### 2.1 Primary target

主目标：[UTC 时间契约修订](../spec/2026-10-13-14-07-archetype-utc-time-contract-amendment.md)，Status Accepted，原设计 Updated 2026-10-13 15:08 Asia/Shanghai；本轮只记录接受/关系时间。用户原文：“Spec 审核通过，按这份 UTC 时间契约修订 Spec 写一份 Plan，先不要实施。SQL 按已确认的 archetype 例外原位修改，不新增版本；deletedAt 保持兼容，既有排课唯一键差距不纳入本次范围。”此确认关闭设计决策；随后用户明确批准执行本Plan，逐Step验证后统一一个commit。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [UTC 修订](../spec/2026-10-13-14-07-archetype-utc-time-contract-amendment.md) | Accepted；设计正文 2026-10-13 15:08 | 全部12项当前 REQ、TIME/API/TEST/约束 | 唯一新增实施范围；§4 REQ-012 是前轮 Spec 文档交付，不转成新业务工作 |
| Dependency | [两阶段生成](../spec/2026-08-27-20-31-archetype-two-stage-source-generation.md) | Review；Updated 2026-08-27 20:31；所需源所有权由已接受主 Spec 纳入 | source-projects → definitions → .generated 的来源与生成/check 不变量 | 仅继承所有权/验源；不把旧整套发布需求重做 |
| Dependency | [MP 演进](../spec/2026-09-12-21-11-mybatis-repository-cqrs-postgresql-evolution.md) | Accepted；Updated 2026-09-13 09:13 | §5 DEC-001，§7/§10/§11 的租户、ID、Repository、LOCAL、deletedAt、version、Clock 覆盖不变量；时间/迁移部分由主 Spec 修订 | 保留兼容，未继承旧完整实施任务或新增迁移要求 |


Effective Specs 头部的主文档承担自动 REQ 覆盖；上述限定继承范围在本节逐项人工复核。验证器会对头部每个链接全文提取旧 REQ，不能表达按章节继承，因此不以重复实施旧全部 REQ 来伪造覆盖。完整集合的链接与范围仍在此明确保留。

### 2.3 Superseded or excluded content

主 Spec 明确替代旧 MP 统一稿对排课 LocalDateTime/naive timestamp 的规则，以及仅针对四个未部署模板的迁移不可原位修改规则。四源 SQL 不新增版本，不 repair，不连接实际数据库；其他历史迁移仍不可变。既有 Light 排课业务唯一键差距由用户排除，不作为本计划阻塞或修复项。旧 SQL 版本、角色、FK、索引、tenant、deleted_at、version 全保留。Native 安全并行工作与 frontend/GraphQL 新字段、全局 Instant.now 改造不属于本任务。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 主 Spec §4 | 四套排课/考试窗口统一 Instant，Light/Open 不再保存 LocalDateTime 窗口 | 所有受影响请求/Command/DTO/值对象/PO 类型一致；同一 UTC 点比较/重叠一致 | Step 3, Step 4 |
| REQ-002 | 主 Spec §4 | 发生时刻文本输入必须含 Z 或数值偏移，输出 UTC Z；纯日期/本地时间保持语义 | naive/空白/缺失偏移拒绝；+08:00 与 Z 等值；PB 字段号及标准 Timestamp 载体保留 | Step 3, Step 4 |
| REQ-003 | 主 Spec §4 | 窗口持久化使用 timestamptz(6)，入参归一到微秒后才比较/写入 | 四套所有物理排课/考试窗口列带时区；任意 JVM/PG 时区往返规范 Instant 相等；不足一微秒窗口归一后拒绝 | Step 2, Step 3, Step 4, Step 5, Step 7 |
| REQ-004 | 主 Spec §4 | deletedAt 保留 Legacy UTC，其他 EgonModel 技术字段保持 ABI | LocalDateTime/NULL/UTC SQL/版本/单调删除及恢复传参不变；不透出 naive deletedAt 为新发生时刻契约 | Step 2 |
| REQ-005 | 主 Spec §4 | 生成器分别识别 SQL instant/date/local time/local timestamp | timestamp with time zone/timestamptz→Instant；date→LocalDate；time without→LocalTime；naive timestamp→LocalDateTime；两入口一致 | Step 1 |
| REQ-006 | 主 Spec §4 | Common MP SQL 提案区分 Java 时间类型 | Instant/OffsetDateTime→TIMESTAMPTZ(6)，LocalDateTime→TIMESTAMP(6)，LocalDate→DATE，LocalTime→TIME(6)；unsupported temporal 类型失败；无 DataSource 调用 | Step 2 |
| REQ-007 | 主 Spec §4 | 模板传输与启动默认 UTC，显式保留业务/展示时区边界 | 七套全部 profile 时间键一致；六套容器 TZ=UTC、启动默认 UTC；无 naive fallback、无全局按请求切换时区 | Step 6, Step 7 |
| REQ-008 | 主 Spec §4 | 允许列明未部署 archetype SQL 原位修改，不新增版本 | 只改四个初始化脚本和相应 manifest checksum；版本 token 不变；旧真实库/ddl_history 不 repair、不回填、不连接 | Step 3, Step 4, Step 5, Step 7 |
| REQ-009 | 主 Spec §4 | source-projects 是真源，派生 archetype 按既有脚本重建 | 不手改 .generated；generate/check 与每个相关生成样例验证；初始 CREATE 生成只在隔离 native 测试目标使用 | Step 7 |
| REQ-010 | 主 Spec §4 | 不新增依赖、运行时、时间框架或命名 Clock 行为改变 | 保持 Maven 坐标/版本、Common MP host unique/Primary Clock 覆盖与 UTC 回退；不新增业务查询/时区表 | Step 7 |
| REQ-011 | 主 Spec §4 | 异常、校验、文档与消费者准确表达新时间契约 | 受影响 REST/RPC 输入错误一致无写入；springdoc date-time、示例、operationId 与测试一致；输入纳秒明确微秒截断 | 文档交付，Step 7 门禁 |
| REQ-012 | 主 Spec §4 | 前轮 Spec 文档已交付且本轮只编写 Plan、更新接受/关系元数据；不实施 | 文档 strict 检查、147路径清单、原工作区保留；Plan 为 Review | Step 7 |
| REQ-001 | 主 Spec §4 | UC-001 | 身份、GraphQL、原业务状态不改 | Step 3, Step 4 |
| REQ-002 | 主 Spec §4 | UC-001 | PB字段号、标准Timestamp及其余RPC不改 | Step 3, Step 4 |
| REQ-003 | 主 Spec §4 | UC-001 | 原路由、LOCAL、guard、索引/状态 | Step 2, Step 3, Step 4, Step 5, Step 7 |
| REQ-004 | 主 Spec §4 | UC-004 | EgonModel/Clock/deletion/restore现有ABI不改 | Step 2 |
| REQ-005 | 主 Spec §4 | UC-002 | profile/catalog/非时间类型、ownership不改 | Step 1 |
| REQ-006 | 主 Spec §4 | UC-004 | 无新增数据库执行/自动schema维护 | Step 2 |
| REQ-007 | 主 Spec §4 | UC-001/002 | Clock主机覆盖、DB role/session、其他配置不改 | Step 6, Step 7 |
| REQ-008 | 主 Spec §4 | UC-003 | 真实库/history/Agent迁移所有权不改 | Step 3, Step 4, Step 5, Step 7 |
| REQ-009 | 主 Spec §4 | UC-002/003 | 不手改派生树/用户自定义文件 | Step 7 |
| REQ-010 | 主 Spec §4 | UC-002/004 | 其余架构、dep版本、Clock选择、事件/存储平台不改 | Step 7 |
| REQ-011 | 主 Spec §4 | UC-001 | 原安全、成功摘要/error fields/presence保持 | 文档交付，Step 7 门禁 |
| REQ-012 | 主 Spec §4 | 前轮 Spec 文档已交付且本轮只编写 Plan、更新接受/关系元数据；不实施 | 文档 strict 检查、147路径清单、原工作区保留；Plan 为 Review | Step 7 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

Step 1 固定未来生成的四种时间类型；Step 2 提案输出对应 SQL 并证明 deletedAt/Clock 不变。Step 3/4 分别完成 native/open Light 的入口 → Command → 值对象 → PO → 源 SQL/manifest；每套作为一个可编译结果验收，重命名和所有消费者在该 Step 内完成，不提交中间编译红态。Step 5 对已使用 Instant 的 Service/Open 仅加 Command 微秒归一化、原协议回归和源列修正。Step 6 才改默认配置/双语文档，避免仅改 TZ 掩盖错误类型；Step 7 重建/check 派生和全范围回归。

### 4.2 Test-first strategy

每个行为 Step 先处理表内测试文件，执行对应 §8 精确命令并记录预期 RED。类型变化测试优先经 JSON/反射/源码合同断言现有公开 API，避免仅因拟改构造器类型不能编译而无法观测；不可分割的 CourseScheduleBO 重命名测试允许 javac 缺少新类型作为声明合同 RED，随后同 Step 完成所有导入。缺依赖、测试配置/基线编译故障不算业务 RED，必须先记录/隔离，不能替换 POM、升级依赖或顺修安全代码。

生成器 RED 为 date/time/aware 全返回 LocalDateTime 或拒绝 timestamptz 别名；MP RED 为 LocalDate/LocalTime 被 TEXT、LocalDateTime 带时区、unsupported temporal 未失败；Light RED 为 offset 时间不能贯通/数值未被严格拒绝/缺 BO 合同；Service RED 为 nanos 未显式截断或折叠窗口仍接受；配置 RED 为旧 Asia/Shanghai/default date-format。SQL RED 先运行现有源合同测试，再用 Step 7 临时源审计对基线的24个窗口列断言，确认 naive 列确实失败；在 Step 3 首次 SQL 修改前提前创建并运行同一脚本，Step 7 汇总后删除。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | 并行 native 安全修改及其他 dirty 文件的相同行 | 同步生成器 SQL → Java 时间矩阵 |
| Step 2 | Step 1 | None | 并行 native 安全修改及其他 dirty 文件的相同行 | 修正 Common MP 时间提案并锁定兼容边界 |
| Step 3 | Step 2 | None | 并行 native 安全修改及其他 dirty 文件的相同行 | 贯通 native Light 排课 UTC 契约 |
| Step 4 | Step 3 | None | 并行 native 安全修改及其他 dirty 文件的相同行 | 贯通 Light-open 排课 UTC 契约 |
| Step 5 | Step 4 | None | 并行 native 安全修改及其他 dirty 文件的相同行 | 统一 Service 与 Service-open 窗口微秒和 SQL |
| Step 6 | Step 5 | None | 并行 native 安全修改及其他 dirty 文件的相同行 | 统一七套传输/启动默认值与双语说明 |
| Step 7 | Step 6 | None | 并行 native 安全修改及其他 dirty 文件的相同行 | 完成源、派生产物和可选 PG 验收门禁 |

无需子代理；按本任务明确顺序实施，遇到并行修改只重读并保留对应 guard。

### 4.4 Commit boundaries

每 Step 是验收/回退边界，不是独立 commit。用户 AGENTS 强制一项逻辑任务至多一个 commit，覆盖 skill 默认逐 Step 提交。本轮不提交。获批实施后只在 7 Step 验收完毕时创建一个 `fix(time): align archetype UTC instant contracts`；根据 §5 清单逐路径/逐 hunk 暂存，只含 UTC 改动。临时 harness、缓存、target、环境变量/凭据和既有安全/skill 工作不提交；派生成品按既有 gitignore/仓库发布所有权处理，不 force-add。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| strict Request decoder | 主 Spec §7.0/§9/§10，必要 | Jackson InstantDeserializer 默认允许 number/stringified timestamp | 在现有两 Request 中嵌套 StdScalarDeserializer；不新增全局时间模块 | 各一嵌套类、一个字段级入口；无新API | Implement |
| 两 BO rename/Command 微秒 | 主 Spec §10 | CourseSchedule.record / ScheduleCourseCommand 原窗口比较 | 复用不可变record与JDK截断；不建 WindowFactory/Strategy | 同Step改导入；无新层 | Implement |
| SQL与MP映射 | 主 Spec §10/§11 | 两javaType及postgresType已有分支 | 直接明确类型分支，保留原诊断/提案接口 | 三个原方法；四SQL+四checksum | Implement |
| Adapter/Facade/Domain协作 | 主 Spec §7/§13 | 原Converter/Manage/Domain/Repository | 原Adapter与Facade处理协议变化；Command/Domain保留验证 | 无registry/cache/newQuery；无fetch-then-forward | Already exists |
| Clock/删除恢复/幂等渠道 | 主 Spec §10/§15 | EgonModel/MetaObjectHandler及原MQ | 只增加回归；不改Bean选择/事务/发送渠道 | 无新增生产类/依赖 | Already exists |
| 业务时区/展示页 | 主 Spec §3/§6.3/§12 | 窗口为确定instant、GraphQL只有scheduleCount | 无新日历特性；TIME表明确N/A | 无时区表/前端 | Already excluded |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 同步生成器 SQL → Java 时间矩阵 | REQ-005 | 见§4.2/§8 TEST映射 | 原依赖/JDK；None | 该Step完整GREEN合同 | 下一Step；Step7汇总 | Step 1 |
| 修正 Common MP 时间提案并锁定兼容边界 | REQ-003, REQ-004, REQ-006 | 见§4.2/§8 TEST映射 | 原依赖/JDK；Step 1 | 该Step完整GREEN合同 | 下一Step；Step7汇总 | Step 2 |
| 贯通 native Light 排课 UTC 契约 | REQ-001, REQ-002, REQ-003, REQ-008 | 见§4.2/§8 TEST映射 | 原依赖/JDK；Step 2 | 该Step完整GREEN合同 | 下一Step；Step7汇总 | Step 3 |
| 贯通 Light-open 排课 UTC 契约 | REQ-001, REQ-002, REQ-003, REQ-008 | 见§4.2/§8 TEST映射 | 原依赖/JDK；Step 3 | 该Step完整GREEN合同 | 下一Step；Step7汇总 | Step 4 |
| 统一 Service 与 Service-open 窗口微秒和 SQL | REQ-003, REQ-008 | 见§4.2/§8 TEST映射 | 原依赖/JDK；Step 4 | 该Step完整GREEN合同 | 下一Step；Step7汇总 | Step 5 |
| 统一七套传输/启动默认值与双语说明 | REQ-007 | 见§4.2/§8 TEST映射 | 原依赖/JDK；Step 5 | 该Step完整GREEN合同 | 下一Step；Step7汇总 | Step 6 |
| 完成源、派生产物和可选 PG 验收门禁 | REQ-003, REQ-007, REQ-008, REQ-009, REQ-010, REQ-012 | 见§4.2/§8 TEST映射 | 原依赖/JDK；Step 6 | 该Step完整GREEN合同 | 下一Step；Step7汇总 | Step 7 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | 两light src/main/java adapter/application/domain/infrastructure；service/web/agent各自模块 | 主Spec §6.1 精确七profile；Common/codegen技术组件为既有扩展 | 分别保持 light/light-open/service/service-open/web/web-open/agent 的现有COLA profile，不合并成第三种分层 | Every Step; MC-ARCH-001 |
| Reuse/capability | MP starter/POM；MapStruct BaseConverter；Validation/Jackson | 主Spec reuse ledger | 不新增依赖或JDBC时间handler，生成入口保留CREATE-only | Step 1–7; MC-REUSE-001/MC-DEP-001 |
| Naming/model/validation/conversion | CourseSchedule.record、EgonModel @SuperBuilder、TeachingApplicationConvertor | BO后缀；不可变record例外；可变PO标准Lombok | 两同位CourseScheduleBO；PO @SuperBuilder/@EqualsAndHashCode(callSuper=true)；原MapStruct BaseConverter/BaseForwardConverter，逐handoff验证原分组 | Step 3–5; MC-NAME-001/MC-MODEL-001/MC-VALID-001/MC-CONVERT-001 |
| Bean/logging/util/JSON/time/config | 现有具名Bean、字段Qualifier与lombok.config；四profiles；JDK时间 | 主Spec §6.2/§10/§15 | 触达业务Bean @Slf4j/具名/@RequiredArgsConstructor/字段@Qualifier，lombok传播；只用现有工具/Jackson/java.time；输出UTC Z | Step 1–6; MC-BEAN-001/MC-LOG-001/MC-UTIL-001/MC-JSON-001/MC-TIME-001/MC-CONFIG-001 |
| Business variation/pattern | LightFacadeConverter/原Facade与Domain Service/Command/Event | 主Spec §7.0/§13 | Complex 跨边界流程复用 Adapter/Facade/Command/Domain Event；单个类型分支/窗口比较为Simple直接实现，不新增Strategy | Step 1–5; MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 时间语义/精度 | JDK java.time | Instant/OffsetDateTime/LocalDate/LocalTime/ChronoUnit.MICROS | 足够；默认zone不能解释输入 | Reuse | None | Step 1–6; MC-REUSE-001 |
| strict JSON input | Spring Boot Jackson/jsr310 | 主Spec EVD-015；现有Request/Jackson | 默认number解码不满足strict字符串 | 现有Request嵌套decoder+匹配字段异常 | 两嵌套类；无新依赖/全局替代 | Step 3–4; MC-JSON-001 |
| 字段转换 | MapStruct/Common Core | LightFacadeConverter、TeachingApplicationConvertor extends BaseConverter/Forward | 满足映射/不可逆投影 | 修改原mapper表达式/类型 | None | Step 3–4; MC-CONVERT-001 |
| layer validation | Jakarta/Boot Validation/ValidationUtils | 三Teaching Validator、Command ModelValidation组 | 约束在边界与原自定义validator；null保留待约束 | Reuse原分组和Annotation；Utils仅手工补充 | None；电话能力N/A | Step 3–5; MC-VALID-001 |
| DDL/PO/Clock | Egon COLA MP-SDJ | EgonModel/MetaObjectHandler/TableInfoMaintainer/AutoConfiguration | 生产机制足够；PG证据需独立gate | 只改提案类型；复用Clock与生命周期 | None | Step 2/7; MC-DEP-001 |
| CREATE生成/SQL检查 | 原parser/FreeMarker/generation scripts | PostgreDdlAdapter/GenerationScopeValidator/TemplateContextService | 映射需修正；现有manifest-family测试不验四源checksum | 改两映射；scratch验实际SQL与bytes | 临时harness删除；无新持久工具 | Step 1/7; MC-REUSE-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

有效源规则原文（编号/拼写保持；Rule 11 的既有排课 unique-key 差距仅按用户本轮明确范围排除）：

```text
1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用对象使用分组校验；基于原生和自定义约束注解、@Valid、@Validated 及 ConstraintValidator 扩展实现。ValidationUtils 只承担通用手工校验，不承载全部业务校验；电话号码复用 libphonenumber。
3 Java POJO 默认使用 class，注解为 @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)，按构造目标选择 @Builder 或 @SuperBuilder；父类状态参与相等性时使用 @EqualsAndHashCode(callSuper = true)。只有不可变 value object 可以使用 record，并豁免 class 的 Lombok 规范；普通实体不能因简单而使用 record。使用 MapStruct、MapStructPlus 和 egon-cola-component-common-core 的 BaseConverter（双向）或 BaseForwardConverter（不可逆投影）进行转换。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 JSON 使用 Spring Boot Jackson；持久化枚举值使用 @EnumValue，向前端输出的枚举值使用 @JsonValue，禁止 ordinal 作为业务编码。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 只允许现有三层结构或当前 egon-cola-archetypes 的 DDD 结构，三层结构保持现状。必须尽量复用 egon-cola-components；持久化模块必须使用 Egon COLA MP Starter，DDL 统一由 egon-mp-sdj-ext-starter 分布式管理。业务唯一键必须组合业务列与 deletedAt（数据库 deleted_at），并验证 NULL、租户及重复软删除语义。采用 CQE（Command Query Event）；Event 必须经 egon-cola-component-transactional-outbox-starter 或 MQ 中间件投递。
```

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主Spec §6.2/§7/§10 | CourseScheduleBO、现有DTO/PO/Command后缀 | §7按文件顺序；Step 3/4 先rename后导入；其他模块不机械更名 | Step 3/4 先rename后导入；其他模块不机械更名 | 语义类型命名与编译 | Step 1–6 | PASS |
| Rule 2 | 主Spec §6.2/§7/§10 | 三层handoff沿用Valid/Validated/原validator与组 | §7按文件顺序；Request→Command→Manage→Domain→Repo；null由约束拒绝，微秒先归一 | Request→Command→Manage→Domain→Repo；null由约束拒绝，微秒先归一 | TEST-001/002/005/006，无旁路 | Step 1–6 | PASS |
| Rule 3 | 主Spec §6.2/§7/§10 | 不可变record；PO class标准Lombok；MapStruct+BaseConverter | §7按文件顺序；两PO匹配EgonModel SuperBuilder；不手写双向转换 | 两PO匹配EgonModel SuperBuilder；不手写双向转换 | TEST-003/007/013、MapStruct编译 | Step 1–6 | PASS |
| Rule 4 | 主Spec §6.2/§7/§10 | 触达业务类@Slf4j/具名Bean/RequiredArgsConstructor/Qualifier | §7按文件顺序；保留Bean名与lombok.config参数传播；不命名强绑hostClock | 保留Bean名与lombok.config参数传播；不命名强绑hostClock | 编译/原wiring测试、脱敏日志review | Step 1–6 | PASS |
| Rule 5 | 主Spec §6.2/§7/§10 | JDK/已批准Commons/Guava allowlist | §7按文件顺序；parse/regex/truncate/SHA用JDK；不新TimeUtils/Tika | parse/regex/truncate/SHA用JDK；不新TimeUtils/Tika | POM无新增、import审查 | Step 1–6 | PASS |
| Rule 6 | 主Spec §6.2/§7/§10 | Boot Jackson，原EnumValue/JsonValue合同 | §7按文件顺序；只Request组件decoder与time配置；不改枚举/ResultRecord timestamp | 只Request组件decoder与time配置；不改枚举/ResultRecord timestamp | TEST-001/003/014 | Step 1–6 | PASS |
| Rule 7 | 主Spec §6.2/§7/§10 | 七套base/dev/test/prod同时间键 | §7按文件顺序；各profile UTC与write-dates-as-timestamps=false；六套删已有date-format | 各profile UTC与write-dates-as-timestamps=false；六套删已有date-format | TEST-014 | Step 1–6 | PASS |
| Rule 9 | 主Spec §6.2/§7/§10 | Complex使用原Adapter/Facade/Command/Domain Event | §7按文件顺序；简单类型矩阵直接分支；不造registry/factory/strategy | 简单类型矩阵直接分支；不造registry/factory/strategy | TEST-002/003/006/007/010 | Step 1–6 | PASS |
| Rule 10 | 主Spec §6.2/§7/§10 | java.time且instant/date/time/LegacyUTC各自语义 | §7按文件顺序；Instant归一微秒；严格偏移；OffsetDateTime->Instant；Clock选择不变 | Instant归一微秒；严格偏移；OffsetDateTime->Instant；Clock选择不变 | TIME-001～006/TEST-001～011 | Step 1–6 | PASS |
| Rule 11 | 主Spec §6.2/§7/§10 | 既有COLA/组件MP与原CQE；排课unique差距排除 | §7按文件顺序；不改结构/唯一键/MQ/Outbox；deletedAt NULL/tenant重复软删除守卫回归 | 不改结构/唯一键/MQ/Outbox；deletedAt NULL/tenant重复软删除守卫回归 | TEST-011/012/013；明确用户例外 | Every Step | PASS；Rule 11 unique-key差距仅用户确认排除，保留现状 |

### 4.9 Time semantics and conversion contract

Time contract: Required

| Time ID | Fields/symbols | Semantics | Java/runtime type | Storage policy | Wire policy | Business zone source | Display zone source | Boundary / DST rules | Precision / Clock | Compatibility | Verification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TIME-001 | ScheduleCourseRequest/DTO/Command、CourseScheduleBO、ClassCourseSchedulePO.startsAt/endsAt，两 Light 树 | Occurred instant | Instant | UTC instant | UTC Z | Not applicable: 窗口已经是确定时刻，无日历日计算 | Not applicable: 源模板没有时间展示页，消费端按显式展示 zone | Z/offset输入；不接受无偏移/地区标签猜测；微秒后 start<end；DST 已由输入偏移消除歧义 | 输入0–9位小数，Command截断微秒；DB(6)、文本输出规范值；无当前时刻生成 | 旧无偏移调用不兼容，未部署模板与调用fixture一起更新 | TEST-001/003/004/005  Step 3/4；§8命令 |
| TIME-002 | Service/Open ScheduleCourseCommand/CreateExamCommand、CourseSchedulePO/ExamPO.startsAt/endsAt | Occurred instant | Instant | UTC instant | UTC Z | Not applicable: 一次性排课/考试窗口只比较 instant | Not applicable: 只维护服务模板，无前端 | 保留 offset/标准 PB Timestamp 输入；归一化为微秒再业务验证；不得固定加减8小时 | wire可解析纳秒，Command显式truncatedTo(MICROS)；PG(6)；结果取规范领域值/已存值 | Java/PB类型与字段号保留，§16限定微秒精度修订；标准 Timestamp按协议保持seconds/nanos | TEST-003/004/005/006  Step 5；§8命令 |
| TIME-003 | EgonModel.createTime/updateTime 及其 PG 列 | Occurred instant | Instant | UTC instant | Internal | Not applicable: 技术审计记录时刻，无日历规则 | Not applicable: 不新增审计外部输出 | 保留现有填充、禁止外部写入；读取同一Instant | 已有注入Clock/host选择；truncatedTo(MICROS)，PG(6) | 既有技术字段ABI和Clock覆盖行为保持，非新增字段 | TEST-004/011  Step 2/7；§8命令 |
| TIME-004 | EgonModel.deletedAt、所有 inherited deleted_at 与恢复token | Occurred instant | LocalDateTime | Legacy UTC | Internal | Not applicable: 强制UTC解释，不是业务当地时间 | Not applicable: 不新增 deletedAt 外部传输 | 原CURRENT_TIMESTAMP AT TIME ZONE UTC、NULL active；仅需要外部投影时在Converter以ZoneOffset.UTC转Instant，禁止直接输出naive值 | 保持timestamp(6)、单调删除微秒策略、数据库Clock | 既有UTC解释和UTC conversion保留；恢复持有LocalDateTime token；不改列/守卫/唯一键 | TEST-011  Step 2/7；§8命令 |
| TIME-005 | codegen date 列/MP LocalDate 提案 | Pure date | LocalDate | Pure date | Internal | Not applicable: 纯日期没有发生时刻或地区偏移 | Not applicable: 类型生成测试，不新增页面 | date ↔ LocalDate；不得转午夜Instant | 日期精度一天；Not applicable: 不生成当前日期 | 没有对应业务字段迁移，只纠正未来类型生成 | TEST-007/010  Step 1/2；§8命令 |
| TIME-006 | codegen time without zone 列/MP LocalTime 提案 | Local time | LocalTime | Local time | Internal | Not applicable: 提案/生成类型，不解析排课发生时刻 | Not applicable: 不新增页面 | time ↔ LocalTime；不认为已得到全球唯一时刻 | 使用明确列精度，提案TIME(6)；Not applicable: 无Clock生成 | naive timestamp仍LocalDateTime、date/time不混用 | TEST-007/010  Step 1/2；§8命令 |

## 5. Change File Tree

以下完整精确路径也构成变更树；147项来自主Spec §8，顺序经依赖调整。额外 scratch 路径为临时验证工具，最终删除且不提交；源文件 current SHA 只作为漂移证据，不授权覆盖他人新变更。

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/TimeTypeMappingTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/TimeTypeMappingTest.java`；TimeTypeMappingTest | 两映射入口、别名、精度、纯日期/time/unsupported 参数化测试 | Step 1 / File 1 | REQ-005 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java` | 已存在 SHA256 c4e6f98e0b8fee4a；BackendCrudTemplateTest | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java`；BackendCrudTemplateTest | 按时间矩阵生成/校验/编译隔离产物，其他CREATE-only保护不变 | Step 1 / File 2 | REQ-005 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java` | 已存在 SHA256 78e9dca95c9c6152；PersistenceTemplateTest | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java`；PersistenceTemplateTest | 按时间矩阵生成/校验/编译隔离产物，其他CREATE-only保护不变 | Step 1 / File 3 | REQ-005 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java` | 已存在 SHA256 682731f24711474b；PostgreDdlAdapterTest | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java`；PostgreDdlAdapterTest | 按时间矩阵生成/校验/编译隔离产物，其他CREATE-only保护不变 | Step 1 / File 4 | REQ-005 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java` | 已存在 SHA256 49fc23d6151416bf；validate/renderModel/javaType/requireBase | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java`；validate/renderModel/javaType/requireBase | SQL时间类型矩阵；timestamptz base别名；unknown类型保持诊断；不改非时间分支 | Step 1 / File 5 | REQ-005 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java` | 已存在 SHA256 8b9185184db23729；context/javaType | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java`；context/javaType | SQL时间类型矩阵；timestamptz base别名；unknown类型保持诊断；不改非时间分支 | Step 1 / File 6 | REQ-005 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/support/time/TimeTypesPO.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/support/time/TimeTypesPO.java`；TimeTypesPO | 仅测试夹具，继承EgonModel，Instant/OffsetDateTime/LocalDateTime/LocalDate/LocalTime | Step 2 / File 1 | REQ-006 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaUtcTimePostgreSqlTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaUtcTimePostgreSqlTest.java`；EgonColaUtcTimePostgreSqlTest | 显式环境门禁，连接已授权测试库；UTC/Shanghai/NewYork跨连接/zone往返；不自动启动库 | Step 2 / File 2 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java` | 已存在 SHA256 aef6e6ddb6bd5a14；EgonModelTest | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java`；EgonModelTest | 审计micro、Clock行为、LocalDateTime删除ABI保持 | Step 2 / File 3 | REQ-004 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainerTest.java` | 已存在 SHA256 2a5df870c3f4eb09；EgonColaTableInfoSchemaMaintainerTest | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainerTest.java`；EgonColaTableInfoSchemaMaintainerTest | 各Class输出、Legacy UTC列、unsupported temporal以及DataSource零调用 | Step 2 / File 4 | REQ-006 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainer.java` | 已存在 SHA256 26abd88d6ad1d5d6；postgresType/maintain | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainer.java`；postgresType/maintain | 正确Java→PG时间类型；unknown temporal/temporal数组拒绝；只读提案 | Step 2 / File 5 | REQ-006 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/ScheduleUtcConverterTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/ScheduleUtcConverterTest.java`；ScheduleUtcConverterTest | 隔离MockMvc或converter验证新UTC合同与原成功/错误shape | Step 3 / File 1 | REQ-002 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/ScheduleUtcContractTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/ScheduleUtcContractTest.java`；ScheduleUtcContractTest | 隔离MockMvc或converter验证新UTC合同与原成功/错误shape | Step 3 / File 2 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/manage/SchoolClassManageTest.java` | 已存在 SHA256 4dee34fceca9d835；SchoolClassManageTest | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/manage/SchoolClassManageTest.java`；SchoolClassManageTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 3 / File 3 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidatorTest.java` | 已存在 SHA256 3fde9ef5ff4a410c；TeachingApplicationValidatorTest | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidatorTest.java`；TeachingApplicationValidatorTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 3 / File 4 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregateTest.java` | 已存在 SHA256 b0298badb43bbe54；SchoolClassAggregateTest | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregateTest.java`；SchoolClassAggregateTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 3 / File 5 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java` | 已存在 SHA256 f241fed380cf4dfd；NativeRpcMappingTest | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java`；NativeRpcMappingTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 3 / File 6 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeSecurityContractTest.java` | 已存在 SHA256 1d4fe02c3b2786fd（编写期间并行变更，已刷新）；NativeSecurityContractTest | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeSecurityContractTest.java`；NativeSecurityContractTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 3 / File 7 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/resources/mybatis/h2-schema.sql` | 已存在 SHA256 70c7d7f72d1749bd；h2-schema | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/resources/mybatis/h2-schema.sql`；h2-schema | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 3 / File 8 | REQ-001 | §8对应TEST与Step命令 |
| RENAME | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/vos/CourseSchedule.java` | 已存在 SHA256 c5bd65d4e745b67c；CourseSchedule → CourseScheduleBO | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/vos/CourseScheduleBO.java`；CourseSchedule → CourseScheduleBO | 迁移到同目录CourseScheduleBO.java，两个Instant时间字段，保留不可变窗口不变量 | Step 3 / File 9 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/command/ScheduleCourseCommand.java` | 已存在 SHA256 c1068fc4b365763b；ScheduleCourseCommand | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/command/ScheduleCourseCommand.java`；ScheduleCourseCommand | 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解 | Step 3 / File 10 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/pojo/dto/ScheduleCourseRequest.java` | 已存在 SHA256 d8e15e66dff5b3aa；ScheduleCourseRequest / nested UtcInstantInputDeserializer | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`；ScheduleCourseRequest / nested UtcInstantInputDeserializer | 时间字段Instant；组件级严格字符串@JsonDeserialize守卫；Command规范化；保留既有对象角色/Validation/PO注解 | Step 3 / File 11 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java` | 已存在 SHA256 311ce0c559ef1701；ScheduleCourseDTO | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java`；ScheduleCourseDTO | 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解 | Step 3 / File 12 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/po/ClassCourseSchedulePO.java` | 已存在 SHA256 b3d39604518f4454；ClassCourseSchedulePO | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/po/ClassCourseSchedulePO.java`；ClassCourseSchedulePO | 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解 | Step 3 / File 13 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/convertor/TeachingApplicationConvertor.java` | 已存在 SHA256 1e8a800329ce6a8e；TeachingApplicationConvertor | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`；TeachingApplicationConvertor | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 3 / File 14 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/LightFacadeConverter.java` | 已存在 SHA256 c5b11fd3bd2f09d5；toProtoTime/fromProtoTime | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/LightFacadeConverter.java`；toProtoTime/fromProtoTime | Instant parse/toString；PB 3/4 string字段不变 | Step 3 / File 15 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregate.java` | 已存在 SHA256 ca92a9a5c1594a55；SchoolClassAggregate | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregate.java`；SchoolClassAggregate | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 3 / File 16 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/validators/TeachingDomainValidator.java` | 已存在 SHA256 e9d27b5e05ce10be；TeachingDomainValidator | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/validators/TeachingDomainValidator.java`；TeachingDomainValidator | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 3 / File 17 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/service/SchoolClassDomainService.java` | 已存在 SHA256 1b84839300553ea9；SchoolClassDomainService | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/service/SchoolClassDomainService.java`；SchoolClassDomainService | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 3 / File 18 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java` | 已存在 SHA256 bbcdc00365436462；SchoolClassDomainServiceImpl | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`；SchoolClassDomainServiceImpl | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 3 / File 19 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidator.java` | 已存在 SHA256 706785e7736371b8；TeachingApplicationValidator | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidator.java`；TeachingApplicationValidator | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 3 / File 20 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/validators/TeachingRequestValidator.java` | 已存在 SHA256 8e07502d51937441；TeachingRequestValidator | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/validators/TeachingRequestValidator.java`；TeachingRequestValidator | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 3 / File 21 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/manage/impl/SchoolClassManageImpl.java` | 已存在 SHA256 9a93a2adfbc3d3ac；SchoolClassManageImpl | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/manage/impl/SchoolClassManageImpl.java`；SchoolClassManageImpl | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 3 / File 22 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/handler/GlobalExceptionHandler.java` | 已存在 SHA256 f2ad36e0bf90aa45；GlobalExceptionHandler | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/handler/GlobalExceptionHandler.java`；GlobalExceptionHandler | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 3 / File 23 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassController.java` | 已存在 SHA256 d5deef6f3cd03818（编写期间并行变更，已刷新）；SchoolClassController | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassController.java`；SchoolClassController | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 3 / File 24 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto` | 已存在 SHA256 9cca36945eb79465；ScheduleCourseRpcRequest | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto`；ScheduleCourseRpcRequest | 只改3/4字段的时间语义注释为ISO_INSTANT UTC，保留field number/wire type | Step 3 / File 25 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql` | 已存在 SHA256 e4936cb2a7943d50；class_course_schedules_0/1.starts_at/ends_at | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`；class_course_schedules_0/1.starts_at/ends_at | 原位改TIMESTAMP(6) WITH TIME ZONE；其他schema/index/lifecycle不变 | Step 3 / File 26 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/repository-manifest.json` | 已存在 SHA256 78b8d724f62f0050；scripts[0].sha256 | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/repository-manifest.json`；scripts[0].sha256 | 按新字节重算SHA-256；version/path/family不变 | Step 3 / File 27 | REQ-008 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/pojo/convertor/ScheduleUtcConverterTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/pojo/convertor/ScheduleUtcConverterTest.java`；ScheduleUtcConverterTest | 隔离MockMvc或converter验证新UTC合同与原成功/错误shape | Step 4 / File 1 | REQ-002 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/ScheduleUtcContractTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/ScheduleUtcContractTest.java`；ScheduleUtcContractTest | 隔离MockMvc或converter验证新UTC合同与原成功/错误shape | Step 4 / File 2 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/SchoolClassManageTest.java` | 已存在 SHA256 59e08d51559160ee；SchoolClassManageTest | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/SchoolClassManageTest.java`；SchoolClassManageTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 4 / File 3 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidatorTest.java` | 已存在 SHA256 b386a79150266d34；TeachingApplicationValidatorTest | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidatorTest.java`；TeachingApplicationValidatorTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 4 / File 4 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregateTest.java` | 已存在 SHA256 25fa743f747dfd3e；SchoolClassAggregateTest | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregateTest.java`；SchoolClassAggregateTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 4 / File 5 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java` | 已存在 SHA256 fff8aa63c30a0da8；SchoolClassRepositoryImplTest | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java`；SchoolClassRepositoryImplTest | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 4 / File 6 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/resources/mybatis/h2-schema.sql` | 已存在 SHA256 23c5fb1737ec0cea；h2-schema | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/resources/mybatis/h2-schema.sql`；h2-schema | 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言 | Step 4 / File 7 | REQ-001 | §8对应TEST与Step命令 |
| RENAME | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/vos/CourseSchedule.java` | 已存在 SHA256 a132a6167a626d3c；CourseSchedule → CourseScheduleBO | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/vos/CourseScheduleBO.java`；CourseSchedule → CourseScheduleBO | 迁移到同目录CourseScheduleBO.java，两个Instant时间字段，保留不可变窗口不变量 | Step 4 / File 8 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/command/ScheduleCourseCommand.java` | 已存在 SHA256 34dc1bd61cbee1b7；ScheduleCourseCommand | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/command/ScheduleCourseCommand.java`；ScheduleCourseCommand | 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解 | Step 4 / File 9 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/pojo/dto/ScheduleCourseRequest.java` | 已存在 SHA256 c16ca3cab647f896；ScheduleCourseRequest / nested UtcInstantInputDeserializer | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`；ScheduleCourseRequest / nested UtcInstantInputDeserializer | 时间字段Instant；组件级严格字符串@JsonDeserialize守卫；Command规范化；保留既有对象角色/Validation/PO注解 | Step 4 / File 10 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/facade/teaching/dto/ScheduleCourseDTO.java` | 已存在 SHA256 7f5f8c2f881986c5；ScheduleCourseDTO | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/facade/teaching/dto/ScheduleCourseDTO.java`；ScheduleCourseDTO | 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解 | Step 4 / File 11 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/po/ClassCourseSchedulePO.java` | 已存在 SHA256 eceb8876e8f9cb17；ClassCourseSchedulePO | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/po/ClassCourseSchedulePO.java`；ClassCourseSchedulePO | 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解 | Step 4 / File 12 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/convertor/TeachingApplicationConvertor.java` | 已存在 SHA256 b24216d33eb9962f；TeachingApplicationConvertor | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`；TeachingApplicationConvertor | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 4 / File 13 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregate.java` | 已存在 SHA256 3a92d294cc854195；SchoolClassAggregate | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregate.java`；SchoolClassAggregate | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 4 / File 14 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/validators/TeachingDomainValidator.java` | 已存在 SHA256 a9e8ae6be1e9ab08；TeachingDomainValidator | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/validators/TeachingDomainValidator.java`；TeachingDomainValidator | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 4 / File 15 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/service/SchoolClassDomainService.java` | 已存在 SHA256 a6b79745f02c932f；SchoolClassDomainService | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/service/SchoolClassDomainService.java`；SchoolClassDomainService | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 4 / File 16 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java` | 已存在 SHA256 2a33fff2b247ac65；SchoolClassDomainServiceImpl | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`；SchoolClassDomainServiceImpl | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 4 / File 17 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidator.java` | 已存在 SHA256 d2751cea90d51bba；TeachingApplicationValidator | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidator.java`；TeachingApplicationValidator | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 4 / File 18 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/validators/TeachingRequestValidator.java` | 已存在 SHA256 e0c24953c7a4ac63；TeachingRequestValidator | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/validators/TeachingRequestValidator.java`；TeachingRequestValidator | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 4 / File 19 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/impl/SchoolClassManageImpl.java` | 已存在 SHA256 d3a653f966dfab85；SchoolClassManageImpl | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/impl/SchoolClassManageImpl.java`；SchoolClassManageImpl | 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为 | Step 4 / File 20 | REQ-001 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/handler/GlobalExceptionHandler.java` | 已存在 SHA256 7ad15478e79020f6；GlobalExceptionHandler | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/handler/GlobalExceptionHandler.java`；GlobalExceptionHandler | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 4 / File 21 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassController.java` | 已存在 SHA256 d385ee56e6c599e3；SchoolClassController | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassController.java`；SchoolClassController | 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型 | Step 4 / File 22 | REQ-002 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql` | 已存在 SHA256 3e10f8c487e692c3；light_class_course_schedules_0/1.starts_at/ends_at | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`；light_class_course_schedules_0/1.starts_at/ends_at | 原位改TIMESTAMP(6) WITH TIME ZONE；其他schema/index/lifecycle不变 | Step 4 / File 23 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/repository-manifest.json` | 已存在 SHA256 d808bd0c3dd1ec54；scripts[0].sha256 | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/repository-manifest.json`；scripts[0].sha256 | 按新字节重算SHA-256；version/path/family不变 | Step 4 / File 24 | REQ-008 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/time/WindowUtcCommandTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/time/WindowUtcCommandTest.java`；WindowUtcCommandTest | 微秒归一化及零窗口拒绝入口回归 | Step 5 / File 1 | REQ-003 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/WindowUtcCommandTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/WindowUtcCommandTest.java`；WindowUtcCommandTest | 微秒归一化及零窗口拒绝入口回归 | Step 5 / File 2 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/src/test/java/top/egon/cola/archetype/source/serviceopen/adapter/exam/facade/impl/ExamFacadeImplTest.java` | 已存在 SHA256 fc97cb80b943f71c；ExamFacadeImplTest | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/src/test/java/top/egon/cola/archetype/source/serviceopen/adapter/exam/facade/impl/ExamFacadeImplTest.java`；ExamFacadeImplTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 3 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/course/CourseManageTest.java` | 已存在 SHA256 a56cb6c68525f0e5；CourseManageTest | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/course/CourseManageTest.java`；CourseManageTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 4 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/exam/ExamManageTest.java` | 已存在 SHA256 d046f55722561827；ExamManageTest | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/exam/ExamManageTest.java`；ExamManageTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 5 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/transaction/LocalTransactionBoundaryTest.java` | 已存在 SHA256 3dad30d23ded2006；LocalTransactionBoundaryTest | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/transaction/LocalTransactionBoundaryTest.java`；LocalTransactionBoundaryTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 6 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/src/test/java/top/egon/cola/archetype/source/serviceopen/facade/contract/ProtoDescriptorContractTest.java` | 已存在 SHA256 e4a2872c23dbbb1c；ProtoDescriptorContractTest | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/src/test/java/top/egon/cola/archetype/source/serviceopen/facade/contract/ProtoDescriptorContractTest.java`；ProtoDescriptorContractTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 7 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/architecture/ArchetypeContractConvergenceTest.java` | 已存在 SHA256 c80c453e8a8c5eb8；ArchetypeContractConvergenceTest | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/architecture/ArchetypeContractConvergenceTest.java`；ArchetypeContractConvergenceTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 8 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/resources/mybatis/h2-schema.sql` | 已存在 SHA256 0f412f5834e97853；h2-schema | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/resources/mybatis/h2-schema.sql`；h2-schema | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 9 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImplTest.java` | 已存在 SHA256 74eef6c0d450139a；ExamFacadeImplTest | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImplTest.java`；ExamFacadeImplTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 10 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/course/CourseManageTest.java` | 已存在 SHA256 156492312aad3794；CourseManageTest | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/course/CourseManageTest.java`；CourseManageTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 11 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/exam/ExamManageTest.java` | 已存在 SHA256 5e1e30e15e81dec0；ExamManageTest | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/exam/ExamManageTest.java`；ExamManageTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 12 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/transaction/LocalTransactionBoundaryTest.java` | 已存在 SHA256 f2adb7f8ebbc2c2b；LocalTransactionBoundaryTest | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/transaction/LocalTransactionBoundaryTest.java`；LocalTransactionBoundaryTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 13 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/architecture/ArchetypeContractConvergenceTest.java` | 已存在 SHA256 a53e257a4887b955；ArchetypeContractConvergenceTest | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/architecture/ArchetypeContractConvergenceTest.java`；ArchetypeContractConvergenceTest | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 14 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/resources/mybatis/h2-schema.sql` | 已存在 SHA256 b46cb730ace3df26；h2-schema | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/resources/mybatis/h2-schema.sql`；h2-schema | 时间fixture/精度与既有RPC合同回归 | Step 5 / File 15 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/course/pojo/command/ScheduleCourseCommand.java` | 已存在 SHA256 6571bf53a0a1a340；ScheduleCourseCommand | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/course/pojo/command/ScheduleCourseCommand.java`；ScheduleCourseCommand | 保留Instant/原字段；compact constructor截断微秒、保留null给Validation | Step 5 / File 16 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/exam/pojo/command/CreateExamCommand.java` | 已存在 SHA256 2cb9bcc40159c859；CreateExamCommand | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/exam/pojo/command/CreateExamCommand.java`；CreateExamCommand | 保留Instant/原字段；compact constructor截断微秒、保留null给Validation | Step 5 / File 17 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/course/pojo/command/ScheduleCourseCommand.java` | 已存在 SHA256 f53146b9f36821cc；ScheduleCourseCommand | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/course/pojo/command/ScheduleCourseCommand.java`；ScheduleCourseCommand | 保留Instant/原字段；compact constructor截断微秒、保留null给Validation | Step 5 / File 18 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/exam/pojo/command/CreateExamCommand.java` | 已存在 SHA256 37a02ce199c50fd9；CreateExamCommand | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/exam/pojo/command/CreateExamCommand.java`；CreateExamCommand | 保留Instant/原字段；compact constructor截断微秒、保留null给Validation | Step 5 / File 19 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql` | 已存在 SHA256 fc9c2a35e556a736；evaluation_course_schedule_0/1、evaluation_exam_0/1 | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`；evaluation_course_schedule_0/1、evaluation_exam_0/1 | 只改starts_at/ends_at到aware(6)，其余索引、约束和表保留 | Step 5 / File 20 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql` | 已存在 SHA256 384296c05cfc5e1f；evaluation_course_schedule_0/1、evaluation_exam_0/1 | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`；evaluation_course_schedule_0/1、evaluation_exam_0/1 | 只改starts_at/ends_at到aware(6)，其余索引、约束和表保留 | Step 5 / File 21 | REQ-003 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json` | 已存在 SHA256 02ba47d7dcc30076；scripts[0].sha256 | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`；scripts[0].sha256 | 只更新受修改script的SHA，version不增 | Step 5 / File 22 | REQ-008 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json` | 已存在 SHA256 3d65fcdd39213c47；scripts[0].sha256 | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`；scripts[0].sha256 | 只更新受修改script的SHA，version不增 | Step 5 / File 23 | REQ-008 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/ArchetypeUtcConfigurationTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/ArchetypeUtcConfigurationTest.java`；ArchetypeUtcConfigurationTest | 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试 | Step 6 / File 1 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-dev.yml` | 已存在 SHA256 97ac6c6adf772c63；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-dev.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 2 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-prod.yml` | 已存在 SHA256 f16b06146bc918a4；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-prod.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 3 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-test.yml` | 已存在 SHA256 48fe060240f55af7；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-test.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 4 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application.yml` | 已存在 SHA256 47da3a0cf7a115a4；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 5 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/launch/launch.args` | 已存在 SHA256 bdda3cf830c6fd34；JVM -Duser.timezone | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/launch/launch.args`；JVM -Duser.timezone | 默认UTC，保留其余占位/flags | Step 6 / File 6 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.md` | 已存在 SHA256 466ffa9668f5c6c8；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 7 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.zh-CN.md` | 已存在 SHA256 5c08bfd54d7830ea；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.zh-CN.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 8 | REQ-007 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ArchetypeUtcConfigurationTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ArchetypeUtcConfigurationTest.java`；ArchetypeUtcConfigurationTest | 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试 | Step 6 / File 9 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml` | 已存在 SHA256 c940de755750321c；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 10 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml` | 已存在 SHA256 7ac2b567b00726e9；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 11 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml` | 已存在 SHA256 605dd7110f3f1c66；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 12 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml` | 已存在 SHA256 d9cbd68c3e796106；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 13 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/launch/launch.args` | 已存在 SHA256 bdda3cf830c6fd34；JVM -Duser.timezone | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/launch/launch.args`；JVM -Duser.timezone | 默认UTC，保留其余占位/flags | Step 6 / File 14 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/Dockerfile` | 已存在 SHA256 a0f34312e413249d；TZ / default JAVA_OPTS | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/Dockerfile`；TZ / default JAVA_OPTS | UTC默认，保持engine/安全/内存/镜像过程 | Step 6 / File 15 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/README.md` | 已存在 SHA256 cba5725f64283403；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-light/README.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 16 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md` | 已存在 SHA256 cbc75d2b6c918f7b；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 17 | REQ-007 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ArchetypeUtcConfigurationTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ArchetypeUtcConfigurationTest.java`；ArchetypeUtcConfigurationTest | 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试 | Step 6 / File 18 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-dev.yml` | 已存在 SHA256 3041fe9fc0b60318；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-dev.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 19 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-prod.yml` | 已存在 SHA256 9ed87c7159644b03；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-prod.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 20 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-test.yml` | 已存在 SHA256 a365fe49fba5ec03；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-test.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 21 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application.yml` | 已存在 SHA256 0f25e41505a42bd6；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 22 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/launch/launch.args` | 已存在 SHA256 bdda3cf830c6fd34；JVM -Duser.timezone | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/launch/launch.args`；JVM -Duser.timezone | 默认UTC，保留其余占位/flags | Step 6 / File 23 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/deploy/container/Dockerfile` | 已存在 SHA256 a0f34312e413249d；TZ / default JAVA_OPTS | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/deploy/container/Dockerfile`；TZ / default JAVA_OPTS | UTC默认，保持engine/安全/内存/镜像过程 | Step 6 / File 24 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.md` | 已存在 SHA256 6e6676b91c33d83b；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 25 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.zh-CN.md` | 已存在 SHA256 bfbca0a3a49d5bdb；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.zh-CN.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 26 | REQ-007 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ArchetypeUtcConfigurationTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ArchetypeUtcConfigurationTest.java`；ArchetypeUtcConfigurationTest | 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试 | Step 6 / File 27 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml` | 已存在 SHA256 84057084b1afc1cb；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 28 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml` | 已存在 SHA256 9b5b228c8e320e75；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 29 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml` | 已存在 SHA256 7f52006127a310e2；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 30 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml` | 已存在 SHA256 b7224159691b02b0；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 31 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/launch/launch.args` | 已存在 SHA256 bdda3cf830c6fd34；JVM -Duser.timezone | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/launch/launch.args`；JVM -Duser.timezone | 默认UTC，保留其余占位/flags | Step 6 / File 32 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/Dockerfile` | 已存在 SHA256 1afb9b2a22fb2111；TZ / default JAVA_OPTS | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/Dockerfile`；TZ / default JAVA_OPTS | UTC默认，保持engine/安全/内存/镜像过程 | Step 6 / File 33 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/README.md` | 已存在 SHA256 a5d0c5d515f2f5d8；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-service/README.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 34 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md` | 已存在 SHA256 568245798e09b6b8；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 35 | REQ-007 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ArchetypeUtcConfigurationTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ArchetypeUtcConfigurationTest.java`；ArchetypeUtcConfigurationTest | 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试 | Step 6 / File 36 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-dev.yml` | 已存在 SHA256 ae9ad4e4106bab38；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-dev.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 37 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-prod.yml` | 已存在 SHA256 a9148364ff5bfc9c；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-prod.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 38 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-test.yml` | 已存在 SHA256 5a5c589e7c5dbb34；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-test.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 39 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application.yml` | 已存在 SHA256 ade342f098d001ab；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 40 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/launch/launch.args` | 已存在 SHA256 bdda3cf830c6fd34；JVM -Duser.timezone | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/launch/launch.args`；JVM -Duser.timezone | 默认UTC，保留其余占位/flags | Step 6 / File 41 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/deploy/container/Dockerfile` | 已存在 SHA256 afe71c416fe79bae；TZ / default JAVA_OPTS | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/deploy/container/Dockerfile`；TZ / default JAVA_OPTS | UTC默认，保持engine/安全/内存/镜像过程 | Step 6 / File 42 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.md` | 已存在 SHA256 07cd41f7a82d9f37；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 43 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.zh-CN.md` | 已存在 SHA256 738ffd1facd78301；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.zh-CN.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 44 | REQ-007 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ArchetypeUtcConfigurationTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ArchetypeUtcConfigurationTest.java`；ArchetypeUtcConfigurationTest | 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试 | Step 6 / File 45 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml` | 已存在 SHA256 44a124b67305568b；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 46 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml` | 已存在 SHA256 55c670ba245d1cb3；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 47 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml` | 已存在 SHA256 899846fb55171b4b；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 48 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml` | 已存在 SHA256 2dffe316f3ae11fd；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 49 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/launch/launch.args` | 已存在 SHA256 bdda3cf830c6fd34；JVM -Duser.timezone | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/launch/launch.args`；JVM -Duser.timezone | 默认UTC，保留其余占位/flags | Step 6 / File 50 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/Dockerfile` | 已存在 SHA256 750549513aa9378c；TZ / default JAVA_OPTS | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/Dockerfile`；TZ / default JAVA_OPTS | UTC默认，保持engine/安全/内存/镜像过程 | Step 6 / File 51 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/README.md` | 已存在 SHA256 9d2156fda4b9db78；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-web/README.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 52 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md` | 已存在 SHA256 94e3966f500185b9；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 53 | REQ-007 | §8对应TEST与Step命令 |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ArchetypeUtcConfigurationTest.java` | CREATE 路径当前不存在；同包既有测试风格 | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ArchetypeUtcConfigurationTest.java`；ArchetypeUtcConfigurationTest | 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试 | Step 6 / File 54 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-dev.yml` | 已存在 SHA256 8fbb9a1e63d6ffa8；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-dev.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 55 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-prod.yml` | 已存在 SHA256 3a8d30735cd44ea5；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-prod.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 56 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-test.yml` | 已存在 SHA256 134229dc0f534297；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-test.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 57 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application.yml` | 已存在 SHA256 99fa420cf965b8a6；spring.jackson time keys | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application.yml`；spring.jackson time keys | 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构 | Step 6 / File 58 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/launch/launch.args` | 已存在 SHA256 bdda3cf830c6fd34；JVM -Duser.timezone | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/launch/launch.args`；JVM -Duser.timezone | 默认UTC，保留其余占位/flags | Step 6 / File 59 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/deploy/container/Dockerfile` | 已存在 SHA256 63d4c6220e2c84a5；TZ / default JAVA_OPTS | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/deploy/container/Dockerfile`；TZ / default JAVA_OPTS | UTC默认，保持engine/安全/内存/镜像过程 | Step 6 / File 60 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.md` | 已存在 SHA256 6946ccd7400a2fef；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 61 | REQ-007 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.zh-CN.md` | 已存在 SHA256 222df4762b73b8f0；时间/输入/部署段 | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.zh-CN.md`；时间/输入/部署段 | 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外 | Step 6 / File 62 | REQ-007 | §8对应TEST与Step命令 |
| CREATE | `scripts/work/2026-10-13-16-21-utc-source-audit.sh` | CREATE 路径当前不存在；同包既有测试风格 | `scripts/work/2026-10-13-16-21-utc-source-audit.sh`；source SQL/manifest/time contract scratch audit | 临时只读验源SQL、checksum、profile、改动范围与派生关系；验证后删除 | Step 7 / File 1 | REQ-003, REQ-007, REQ-008, REQ-009, REQ-010 | §8对应TEST与Step命令 |
| MODIFY | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java` | column/getArrayData；真实时间数组RED | 同原ColumnBO.sqlType保留[] | 必要parser补充 | Step 1 / File 7 | REQ-005 | TimeTypeMappingTest |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassControllerTest.java` | 旧MockitoBean无name | schoolClassManageImpl mock名称 | 必要caller wiring | Step 4 / File 25 | REQ-011 | SchoolClassControllerTest |

| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/package-info.java` | 当前不存在；AgentSourceContractTest完整回归发现缺失 | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/package-info.java`；仅测试包文档 | 遵守Agent既有逐包文档守卫，不改变业务行为 | Step 6 / File 63 | REQ-007/010 | AgentSourceContractTest+ArchetypeUtcConfigurationTest |
| MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy` | 原vos/CourseSchedule断言；真实IT因rename失败 | `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`；一个路径常量 | 必要rename消费者同步，无守卫放宽 | Step 7 / File 2 | REQ-009/011 | generated-archetypes真实IT |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/package-info.java` | 当前新增测试包；既有generic package-info校验 | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/package-info.java`；测试包说明 | 必要测试包文档，无运行时变更 | Step 5 / File 24 | REQ-007/009 | source配置/完整回归及generated verifier |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/package-info.java` | 当前新增测试包；既有generic package-info校验 | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/package-info.java`；测试包说明 | 必要测试包文档，无运行时变更 | Step 6 / File 64 | REQ-007/009 | source配置/完整回归及generated verifier |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/package-info.java` | 当前新增测试包；既有generic package-info校验 | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/package-info.java`；测试包说明 | 必要测试包文档，无运行时变更 | Step 6 / File 65 | REQ-007/009 | source配置/完整回归及generated verifier |
| CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/package-info.java` | 当前新增测试包；既有generic package-info校验 | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/package-info.java`；测试包说明 | 必要测试包文档，无运行时变更 | Step 6 / File 66 | REQ-007/009 | source配置/完整回归及generated verifier |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

用户本轮 AGENTS 是适用上位规则：仅文档、不得自动启动、不得GUI、最多单commit、旧Flyway默认不可改；本次四 archetype 初始化SQL仅按用户明确例外处理。当前 main HEAD 未变，既有dirty涵盖 .agents skill修改和native安全实现；本计划逐文件SHA/声明来自真实读取。执行时重新读重叠文件并仅改时间hunk，不自动git restore、clean、reset、全树add。manifest checksum必须读取修改后的实际bytes，不复用这里的基线摘要。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Maven/JDK | 根与七source均有 ./mvnw；pom.xml已声明版本 | JDK及声明坐标在缓存；优先 -o targeted test，不能自行安装/换版本 | 缺依赖不算RED；本轮不运行构建 |
| PB/MapStruct | 相关 source ./mvnw test / generate-test-sources lifecycle | 现有插件依赖、各自产物source生成 | 用真实PB descriptor/mapper；不能伪实现 |
| PG explicit opt-in | 原 EgonColaLogicDeletePostgreSqlTest 的 EGON_MP_PG_MODEL_TEST/URL/USER/PASSWORD | 用户已启动并明确授权隔离PG；测试仅建唯一scratch schema并finally清理自己的schema | 不自动Docker/PG/应用；Skipped不算pass；默认纯单测 |
| 生成/check | scripts/generate_archetypes.sh generate / check；scripts/regression/test-generate-archetypes.sh | 现有已声明依赖与生成脚本资源；临时隔离生成位置 | 不对真实自定义样例egon-codegen apply；不手改.generated |
| 文档validator | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py <Plan> --strict | skill预检已通过；源码scope和规范人工补充 | 仅证明计划结构/trace，不证明运行时 |

### 6.3 Immutable constraints and approved decisions

四个 V20260913_001__initialize_repository_schema.sql 原路径/版本原位改 starts_at/ends_at→TIMESTAMP(6) WITH TIME ZONE，只重算各 repository-manifest.json 指定entry。12个表实例/8个不同表名/24列；native Light class_course_schedules_0/1，Open light_class_course_schedules_0/1；Service/Open各 evaluation_course_schedule_0/1、evaluation_exam_0/1。EgonModel/create/update/deletedAt ABI、Clock unique/@Primary选择/UTC fallback、tenant/LOCAL/版本/SQL guard/幂等保持。HTTP既有route/operationId/ResultRecord，native PB string field3/4，OpenService标准google.protobuf.Timestamp及seconds/nanos保持；无新OpenLight RPC注册。输入字符串破坏性修订仅未部署模板及fixtures；所有四窗口在微秒比较后写入。

### 6.4 Plan Clarifications

PLAN-CLAR-011（同一新增测试包文档约束）：generated Web-open要求新增time包具有package-info；全量审查发现native Web及native Service的app/time、starter/time也受既有generic包文档校验约束。仅为这4个本任务新包补同风格两行说明，保留全部检查。

PLAN-CLAR-010（必要rename消费端）：Step7真实archetype IT的native Light verify.groovy仍断言vos/CourseSchedule.java，直接阻断已批准BO rename的验收。仅把该一个路径替换为vos/CourseScheduleBO.java；Open verifier没有旧BO断言，不改它。其余校验、历史SQL守卫均保留。

PLAN-CLAR-009（必要包文档）：完整Agent源回归的AgentSourceContractTest要求每个有Java源码的包具有package-info.java。Step6新增time测试包必须补同风格的一行包文档；只增加该package-info，不放宽既有检查。

PLAN-CLAR-007（旧OAS事实）：Light-open原Controller没有@Operation，实际springdoc的既有operationId为schedule。保留schedule并明确注解，不改成原文误记的新名字；新增OAS描述/时间schema不变更原SDK操作名。

PLAN-CLAR-008（必要已有测试caller）：OpenController按已批准Rule4明确原Bean名/Qualifier后，既有SchoolClassControllerTest的MockitoBean须显式使用原service Bean名schoolClassManageImpl。增加该测试文件的一行名称修正；不是新业务/安全工作。

PLAN-CLAR-006（原响应事实校正）：两Light现有ResponseWrapperHandler将SchoolClassDetailVO置于ResultRecord.data；原Spec把投影写成完整直出与源码及既有NativeHttpCompatibilityTest矛盾。按用户“保留既有行为”保留包装器，校正REST响应说明与本Step OAS/tests，保留五字段payload及UnixMillis timestamp；没有新wrapper、字段或接口。

PLAN-CLAR-005（测试夹具定位）：TimeTypesPO 移至 schema/support/time 子包；原 schema/support 同包存在 PoMissingTenant，maintain 在route查找前检查所有扫描模型tenant，必须隔离以免合法时间测试被无tenant夹具污染。原缺tenant行为保持，新PO及static unsupported PO均继承EgonModel；只是必要测试路径，不改生产扫描/校验顺序。

PLAN-CLAR-004（实施必要补充）：真实AST丢数组维度，按用户AGENTS“必要正确性文件可修改”在Step1增加PostgreDdlAdapter.java的temporal-only维度保留；落实现有REQ-005，不引入数组支持、依赖或新API。RED证据为28测试中唯一TIMESTAMP[]失败。

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | 按单任务一次commit收敛Step提交 | 用户AGENTS优先于skill默认每Step commit | 只改变提交组织，不改变行为或每Step验收 | 停止暂存并按人类确认调整；不拆/重写历史 |
| PLAN-CLAR-002 | 创建 `scripts/work/2026-10-13-16-21-utc-source-audit.sh` 临时只读源门禁，Step3 SQL前RED、Step7最终删除 | Spec TEST-009引用的EgonColaDdlManifestFamilyTest只校验family枚举，不验这四SQL bytes；skill Rule22要求无现成gate用scripts/work | 补上已批准SQL/hash/范围验收，无生产或持久脚本新增 | 如真实已有覆盖用该命令，删临时harness；不得因此扩展DDL |
| PLAN-CLAR-003 | configuration测试从当前模块向上定位对应source根，Agent不要求Docker | 七source布局，service等starter不同工作目录 | 只读profile/launch/Docker合同；避免路径依赖或编造Agent镜像 | 调整相对定位测试夹具，合同与依赖不变 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 同步生成器 SQL → Java 时间矩阵

- Requirements: REQ-005
- Dependencies: None
- Baseline state: 两个javaType混合temporal→LocalDateTime，基础字段检查未接受全部aware alias。
- Observable outcome: 同步生成器 SQL → Java 时间矩阵，对应§8合同可观察。
- End state: 本Step目标完整编译/GREEN；下一Step未完成项保持清晰，不把该Step验证宣称全任务运行通过。
- Test-first gate: Required — §4.2对应行为RED；先下列测试，后生产/接线；Step7复用已先建临时门禁的RED，不新改业务。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 本Step按§12.5逐项评审适用面；测试/配置/SQL文件的N/A理由在各块明确。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 非触达能力按§4.8边界证据保留，原unique-key例外不能扩大。
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/TimeTypeMappingTest.java`

- Purpose: 两映射入口、别名、精度、纯日期/time/unsupported 参数化测试
- Symbols: TimeTypeMappingTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 同 Step 的上游/下游原调用链；§5精确文件清单与主Spec §7/§9/§10。
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 新增矩阵测试；通过两个公开context/renderModel入口，不暴露private方法
- Input/output and state mapping: 同一CREATE table/列 → 四种FQN/simpleName/import；unsupported → diagnostics且无输出
- Error and edge behavior: 保持原异常/权限/tenant/版本守卫；不新增重试、数据库调用或其他时间语义。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void temporalSqlTypesAgreeAcrossRenderEntrypoints() {
    // Use existing DdlSchemaService/PostgreDdlAdapter CREATE fixture with inherited base audit fields.
    // Matrix: TIMESTAMPTZ(6), TIMESTAMP(6) WITH TIME ZONE -> java.time.Instant;
    // TIMESTAMP(6) [WITHOUT TIME ZONE] -> LocalDateTime; DATE -> LocalDate; TIME(6) [WITHOUT TIME ZONE] -> LocalTime.
    // Feed identical table to TemplateContextService.context(schema, table, profile, artifact, policies)
    // and GenerationScopeValidator.renderModel(config, table, artifact); assert field javaType AND sorted imports.
    // Validate timestamptz create/update aliases, nullable naive deleted_at; reject aware deleted_at and naive audits.
    // timetz, time with time zone, interval, temporal arrays: scope UNKNOWN_SQL_TYPE; context throws before render/write.
    // Repeat case/spacing/precision through real parser; do not call private javaType by reflection.
}
```

- Verification contribution: Step 1 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java`

- Purpose: 按时间矩阵生成/校验/编译隔离产物，其他CREATE-only保护不变
- Symbols: BackendCrudTemplateTest
- Repository evidence: 34: class BackendCrudTemplateTest {；92: public final class OrdersErrorMapper {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 四语义业务列生成PO/Command/Result/import与真实converter编译；技术列继承与约束组；没有OPEN/AGENT apply或覆盖真实项目
- Input/output and state mapping: 原CREATE schema字符串/临时native LIGHT WEB SERVICE → 四语义业务列生成PO/Command/Result/import与真实converter编译；技术列继承与约束组；没有OPEN/AGENT apply或覆盖真实项目
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void crudArtifactsRetainDistinctTimeSemantics() {
    // Arrange: 原CREATE schema字符串/临时native LIGHT WEB SERVICE.
    // Inputs / expected assertions: 四语义业务列生成PO/Command/Result/import与真实converter编译；技术列继承与约束组；没有OPEN/AGENT apply或覆盖真实项目.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=BackendCrudTemplateTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java`

- Purpose: 按时间矩阵生成/校验/编译隔离产物，其他CREATE-only保护不变
- Symbols: PersistenceTemplateTest
- Repository evidence: 35: class PersistenceTemplateTest {；74: assertTrue(repo.contains("@Repository(\"ordersRepository\")"));
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 时间列PO/import正确且可编译；MapperXML tenant/deleted_at/version/bound params保持；native LIGHT WEB SERVICE profile不越权
- Input/output and state mapping: 原DdlSchemaService/JavaCompiler/TempDir → 时间列PO/import正确且可编译；MapperXML tenant/deleted_at/version/bound params保持；native LIGHT WEB SERVICE profile不越权
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void renderedPersistenceUsesFourTimeTypesAndCompiles() {
    // Arrange: 原DdlSchemaService/JavaCompiler/TempDir.
    // Inputs / expected assertions: 时间列PO/import正确且可编译；MapperXML tenant/deleted_at/version/bound params保持；native LIGHT WEB SERVICE profile不越权.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=PersistenceTemplateTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java`

- Purpose: 按时间矩阵生成/校验/编译隔离产物，其他CREATE-only保护不变
- Symbols: PostgreDdlAdapterTest
- Repository evidence: 24: class PostgreDdlAdapterTest {；101: String hash = sha256(Files.readAllBytes(script));
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: case/whitespace/p；aware审计alias+nullable naive删除；unsupported temporal/array/illegalp UNKNOWN_SQL_TYPE或原解析诊断；ALTER继续拒绝；不能强行String成功
- Input/output and state mapping: 原parser/CREATE AST及scope公开入口 → case/whitespace/p；aware审计alias+nullable naive删除；unsupported temporal/array/illegalp UNKNOWN_SQL_TYPE或原解析诊断；ALTER继续拒绝；不能强行String成功
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void temporalDdlAliasesAndUnsupportedShapesStayGuarded() {
    // Arrange: 原parser/CREATE AST及scope公开入口.
    // Inputs / expected assertions: case/whitespace/p；aware审计alias+nullable naive删除；unsupported temporal/array/illegalp UNKNOWN_SQL_TYPE或原解析诊断；ALTER继续拒绝；不能强行String成功.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=PostgreDdlAdapterTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java`

- Purpose: SQL时间类型矩阵；timestamptz base别名；unknown类型保持诊断；不改非时间分支
- Symbols: validate/renderModel/javaType/requireBase
- Repository evidence: 20: public class GenerationScopeValidator {；180: String javaType = javaType(column);
- Dependencies and consumers: GenerationScopeValidator.validate/renderModel、TemplateContextService.context、FreeMarker/CREATE plan/check/apply；两个入口矩阵必须相同
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: SQL时间类型矩阵；timestamptz base别名；unknown类型保持诊断；不改非时间分支
- Input/output and state mapping: ColumnBO.sqlType → 原模型fields.javaType/imports；create_time/update_time aware，deleted_at nullable naive
- Error and edge behavior: scope未知时间/数组 UNKNOWN_SQL_TYPE，base错误 BASE_FIELD_TYPE；context直接调用非法类型抛IllegalArgumentException；无写入
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
String javaType(CodegenSchemaBO.ColumnBO column) {
    String sqlType = column.getSqlType() == null ? "" : column.getSqlType().trim().toLowerCase(Locale.ROOT);
    // Normalize whitespace/precision for matching; retain []/unsupported qualifiers to reject rather than strip.
    // temporal arrays / timetz / time with time zone / interval: validator returns null for UNKNOWN_SQL_TYPE;
    // context throws IllegalArgumentException with column/type context, never silently returns String.
    if (awareTimestampShape(sqlType)) return "java.time.Instant"; // timestamp[(p)] with time zone / timestamptz[(p)]
    if (naiveTimestampShape(sqlType)) return "java.time.LocalDateTime"; // timestamp[(p)] [without time zone]
    if ("date".equals(sqlType)) return "java.time.LocalDate";
    if (naiveTimeShape(sqlType)) return "java.time.LocalTime"; // time[(p)] [without time zone]
    // Shape predicates represent short inline JDK regex branches, not proposed classes/interfaces/registries.
    // timestamp[(p)] [without time zone] -> "java.time.LocalDateTime";
    // date -> "java.time.LocalDate"; time[(p)] [without time zone] -> "java.time.LocalTime";
    // Preserve bigint/int/bool/numeric/text branches and sorted imports/simple names/hidden policies.
}
// GenerationScopeValidator requireBase / audit-zone checks accept aware timestamptz aliases only.
// Invalid precision and unsupported shapes retain existing parser/scope diagnostics; validate precedes file writes.
```

- Verification contribution: Step 1 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java`

- Purpose: SQL时间类型矩阵；timestamptz base别名；unknown类型保持诊断；不改非时间分支
- Symbols: context/javaType
- Repository evidence: 16: * Turns schema and field policy into a read-only render model. It does not expose the parser or a class loader.；19: public class TemplateContextService {
- Dependencies and consumers: GenerationScopeValidator.validate/renderModel、TemplateContextService.context、FreeMarker/CREATE plan/check/apply；两个入口矩阵必须相同
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: SQL时间类型矩阵；timestamptz base别名；unknown类型保持诊断；不改非时间分支
- Input/output and state mapping: ColumnBO.sqlType → 原模型fields.javaType/imports；create_time/update_time aware，deleted_at nullable naive
- Error and edge behavior: scope未知时间/数组 UNKNOWN_SQL_TYPE，base错误 BASE_FIELD_TYPE；context直接调用非法类型抛IllegalArgumentException；无写入
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
String javaType(CodegenSchemaBO.ColumnBO column) {
    String sqlType = column.getSqlType() == null ? "" : column.getSqlType().trim().toLowerCase(Locale.ROOT);
    // Normalize whitespace/precision for matching; retain []/unsupported qualifiers to reject rather than strip.
    // temporal arrays / timetz / time with time zone / interval: validator returns null for UNKNOWN_SQL_TYPE;
    // context throws IllegalArgumentException with column/type context, never silently returns String.
    if (awareTimestampShape(sqlType)) return "java.time.Instant"; // timestamp[(p)] with time zone / timestamptz[(p)]
    if (naiveTimestampShape(sqlType)) return "java.time.LocalDateTime"; // timestamp[(p)] [without time zone]
    if ("date".equals(sqlType)) return "java.time.LocalDate";
    if (naiveTimeShape(sqlType)) return "java.time.LocalTime"; // time[(p)] [without time zone]
    // Shape predicates represent short inline JDK regex branches, not proposed classes/interfaces/registries.
    // timestamp[(p)] [without time zone] -> "java.time.LocalDateTime";
    // date -> "java.time.LocalDate"; time[(p)] [without time zone] -> "java.time.LocalTime";
    // Preserve bigint/int/bool/numeric/text branches and sorted imports/simple names/hidden policies.
}
// GenerationScopeValidator requireBase / audit-zone checks accept aware timestamptz aliases only.
// Invalid precision and unsupported shapes retain existing parser/scope diagnostics; validate precedes file writes.
```

- Verification contribution: Step 1 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java`

- Purpose: 保留真实AST中的时间数组维度，落实已批准unsupported temporal array失败规则。
- Symbols: PostgreDdlAdapter.column(ColumnDefinition, int) / ColDataType.getArrayData()
- Repository evidence: 真实CREATE TIMESTAMP[] 的TimeTypeMappingTest失败；当前column只读getDataType而丢失getArrayData；无生产协议改动。
- Dependencies and consumers: DdlSchemaService → GenerationScopeValidator与TemplateContextService的两个已有类型入口。
- Why now: 矩阵测试从真实CREATE入口已RED；仅改类型模型时无法观测被parser丢弃的维度。
- Contract/signature changes: 原公开方法签名与非时间列不变；时间数组ColumnBO.sqlType保留[]而被原诊断拒绝。
- Input/output and state mapping: temporal ColDataType+arrayData → 原sqlType加[] → UNKNOWN_SQL_TYPE/直接context错误；无生成写入。
- Error and edge behavior: 任意维度时间数组不降成标量，不支持数组类型，不改变非时间数组既有行为。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；JDK/已有parser，无新Bean/模型/依赖。
- Literal rule enforcement: Rule 5/10/11：JDK与原技术包；Rule 2 保持诊断边界；其它Java模型/注解/JSON/config规则N/A。
- Implementation pseudocode:

```java
// Existing column(ColumnDefinition definition, int ordinal): after current precision/nullability extraction.
if (typeName != null && dataType.getArrayData() != null && !dataType.getArrayData().isEmpty()
        && typeName.toLowerCase(Locale.ROOT).matches("^(?:timestamp|timestamptz|date|time|timetz|interval).*")) {
    typeName += "[]".repeat(dataType.getArrayData().size());
}
// Preserve all existing non-temporal paths and return fields. Scope then rejects array sqlType before rendering.
```

- Verification contribution: TimeTypeMappingTest.unsupportedSqlTemporalShapesRemainVisibleToTheScope；真实CREATE与两mapping入口。
- After this file: 已批准时间数组错误路径可观测；运行本Step完整GREEN命令再验收。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command:

```bash
./mvnw -o -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result: 对应TEST全部GREEN；Step3/4所有BO/import/mapper编译成功；Step5四源列与摘要GREEN；Step6七套配置测试；Step7generate/check与回归无新增发现。可选PG若未授权运行记Not run/Skipped，不能算PASS。
- Failure returns to: 当前Step对应File（行为/映射/fixture）；声明坐标缺失或既有编译失败记录baseline；真实JDBC契约或行为设计冲突返回Spec，不新增依赖/handler或调整guard。
- Completion criteria: 该Step测试成功输出、相关静态门禁/完整diff与原guard保持证据齐全；没有误改其它路径。
- Rollback: 只撤销本人本Step的时间hunk或前向修正；保留之前GREEN与用户dirty；SQL为未部署源模板，无DBrollback/repair。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/TimeTypeMappingTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java`
- Commit: 不单独提交（用户单任务一次commit）；Step7汇总 `fix(time): align archetype UTC instant contracts`。

### Step 2 — 修正 Common MP 时间提案并锁定兼容边界

- Requirements: REQ-003, REQ-004, REQ-006
- Dependencies: Step 1验收完成；源码改动保留未提交
- Baseline state: postgresType Instant/LocalDateTime→TIMESTAMPTZ，date/time→TEXT；模型Clock/删除已有兼容合同。
- Observable outcome: 修正 Common MP 时间提案并锁定兼容边界，对应§8合同可观察。
- End state: 本Step目标完整编译/GREEN；下一Step未完成项保持清晰，不把该Step验证宣称全任务运行通过。
- Test-first gate: Required — §4.2对应行为RED；先下列测试，后生产/接线；Step7复用已先建临时门禁的RED，不新改业务。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 本Step按§12.5逐项评审适用面；测试/配置/SQL文件的N/A理由在各块明确。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 非触达能力按§4.8边界证据保留，原unique-key例外不能扩大。
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/support/time/TimeTypesPO.java`

- Purpose: 仅测试夹具，继承EgonModel，Instant/OffsetDateTime/LocalDateTime/LocalDate/LocalTime
- Symbols: TimeTypesPO
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: EgonColaTableInfoSchemaMaintainerTest；原TableInfo scanner
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 仅测试PO夹具，无生产持久字段新增
- Input/output and state mapping: PO五业务时间字段+继承deletedAt→TableInfo对应SQL类型
- Error and edge behavior: unsupported夹具隔离，不能污染成功scan
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)
@SuperBuilder @EqualsAndHashCode(callSuper = true)
@TableName("time_types")
class TimeTypesPO extends EgonModel<TimeTypesPO> {
    private Instant occurredAt; private OffsetDateTime offsetAt;
    private LocalDateTime localAt; private LocalDate pureDate; private LocalTime localTime;
    // Inherit id/tenantId/createTime/updateTime/deletedAt/version; do not redeclare technical fields.
}
// Keep unsupported ZonedDateTime/OffsetTime/Duration nested @TableName PO fixtures in a distinct scan package
// or scan only that nested class name, so supported scan never accidentally finds unsupported fixtures.
```

- Verification contribution: Step 2 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaUtcTimePostgreSqlTest.java`

- Purpose: 显式环境门禁，连接已授权测试库；UTC/Shanghai/NewYork跨连接/zone往返；不自动启动库
- Symbols: EgonColaUtcTimePostgreSqlTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 用户已启动/授权隔离PG；参考原EgonColaLogicDeletePostgreSqlTest fixture
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 显式环境门禁，连接已授权测试库；UTC/Shanghai/NewYork跨连接/zone往返；不自动启动库
- Input/output and state mapping: 真实MP Instant写读/两session与JVM zone→同一micro Instant；临时schema自身清理
- Error and edge behavior: env未开启显式Skipped，不算PG通过；驱动失败返回Spec，不新增handler
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@EnabledIfEnvironmentVariable(named = "EGON_MP_PG_MODEL_TEST", matches = "true")
class EgonColaUtcTimePostgreSqlTest {
    @Test void instantRoundTripsAcrossJvmAndSessionZones() {
        // Read EGON_MP_PG_URL/USER/PASSWORD exactly as existing integration fixture; never print secrets.
        // Create one UUID-named test schema only; own cleanup in finally; no touching existing history/data.
        Instant expected = Instant.parse("2026-10-13T06:07:08.123456Z");
        // Use real MP PO/Mapper/TableInfo and existing MyBatis Instant binding, not JDBC-only surrogate assertions.
        // Write/read via different connections with session zone UTC and Asia/Shanghai;
        // repeat with JVM default zone UTC/Asia/Shanghai, restore TimeZone in finally, no parallel mutation.
        // assert returned Instant == expected; assert database column type timestamptz and precision 6.
        // Assert audits micro Instant, deleted_at naive NULL-active/UTC token and original lifecycle guards.
        // OffsetDateTime proposal coverage is separate; no custom JDBC TypeHandler if this gate fails.
    }
}
```

- Verification contribution: Step 2 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java`

- Purpose: 审计micro、Clock行为、LocalDateTime删除ABI保持
- Symbols: EgonModelTest
- Repository evidence: 18: import java.time.Instant;；27: class EgonModelTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: create/update microInstant；deletedAt仍LocalDateTime/NULL-active；继承version/tenant不变；Clock unique/Primary覆盖与UTC fallback通过现有AutoConfigurationTest一并验证
- Input/output and state mapping: 原模型/MetaObjectHandler固定Clock夹具 → create/update microInstant；deletedAt仍LocalDateTime/NULL-active；继承version/tenant不变；Clock unique/Primary覆盖与UTC fallback通过现有AutoConfigurationTest一并验证
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void legacyAuditAndDeletionContractsRemainStable() {
    // Arrange: 原模型/MetaObjectHandler固定Clock夹具.
    // Inputs / expected assertions: create/update microInstant；deletedAt仍LocalDateTime/NULL-active；继承version/tenant不变；Clock unique/Primary覆盖与UTC fallback通过现有AutoConfigurationTest一并验证.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=EgonModelTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainerTest.java`

- Purpose: 各Class输出、Legacy UTC列、unsupported temporal以及DataSource零调用
- Symbols: EgonColaTableInfoSchemaMaintainerTest
- Repository evidence: 16: import java.time.Instant;；25: class EgonColaTableInfoSchemaMaintainerTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 五种时间精确SQL；继承deleted_at naive；unsupported ZonedDateTime/OffsetTime/Duration失败TIME_TYPE_UNSUPPORTED；drop=true原错误；所有mockDataSource零交互
- Input/output and state mapping: 现有maintain query/route/mock fixture、TimeTypesPO → 五种时间精确SQL；继承deleted_at naive；unsupported ZonedDateTime/OffsetTime/Duration失败TIME_TYPE_UNSUPPORTED；drop=true原错误；所有mockDataSource零交互
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void proposalUsesSemanticTimeTypesWithoutExecutingSql() {
    // Arrange: 现有maintain query/route/mock fixture、TimeTypesPO.
    // Inputs / expected assertions: 五种时间精确SQL；继承deleted_at naive；unsupported ZonedDateTime/OffsetTime/Duration失败TIME_TYPE_UNSUPPORTED；drop=true原错误；所有mockDataSource零交互.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=EgonColaTableInfoSchemaMaintainerTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainer.java`

- Purpose: 正确Java→PG时间类型；unknown temporal/temporal数组拒绝；只读提案
- Symbols: postgresType/maintain
- Repository evidence: 22: import java.time.Instant;；34: @Component("egonColaTableInfoSchemaMaintainer")
- Dependencies and consumers: maintain(EgonColaSchemaMaintainQuery)→createTableSql→postgresType；管理SQL提案消费者
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 正确Java→PG时间类型；unknown temporal/temporal数组拒绝；只读提案
- Input/output and state mapping: TableInfo property Class→CREATE SQL；NO DataSource execution
- Error and edge behavior: unsupported temporal TIME_TYPE_UNSUPPORTED；drop/tenant原失败不变
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
String postgresType(TableFieldInfo field) {
    Class<?> type = field.getPropertyType();
    // Preserve current Long/long -> BIGINT, Integer/int -> INTEGER and non-temporal TEXT fallback.
    if (type == Instant.class || type == OffsetDateTime.class) return "TIMESTAMPTZ(6)";
    if (type == LocalDateTime.class) return "TIMESTAMP(6) WITHOUT TIME ZONE"; // inherited deleted_at too
    if (type == LocalDate.class) return "DATE";
    if (type == LocalTime.class) return "TIME(6) WITHOUT TIME ZONE";
    if (TemporalAccessor.class.isAssignableFrom(type) || TemporalAmount.class.isAssignableFrom(type)) {
        throw new EgonColaMybatisPlusConfigurationException("TIME_TYPE_UNSUPPORTED");
    }
    // Unsupported guard follows all supported temporal branches. maintain(query) returns SQL only;
    // keep Clock/appliedAt, DROP_FORBIDDEN, TENANT_ID_REQUIRED, schema/table routing unchanged.
}
```

- Verification contribution: Step 2 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command:

```bash
./mvnw -o -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result: 对应TEST全部GREEN；Step3/4所有BO/import/mapper编译成功；Step5四源列与摘要GREEN；Step6七套配置测试；Step7generate/check与回归无新增发现。可选PG若未授权运行记Not run/Skipped，不能算PASS。
- Failure returns to: 当前Step对应File（行为/映射/fixture）；声明坐标缺失或既有编译失败记录baseline；真实JDBC契约或行为设计冲突返回Spec，不新增依赖/handler或调整guard。
- Completion criteria: 该Step测试成功输出、相关静态门禁/完整diff与原guard保持证据齐全；没有误改其它路径。
- Rollback: 只撤销本人本Step的时间hunk或前向修正；保留之前GREEN与用户dirty；SQL为未部署源模板，无DBrollback/repair。
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/support/time/TimeTypesPO.java`; `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaUtcTimePostgreSqlTest.java`; `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java`; `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainerTest.java`; `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainer.java`
- Commit: 不单独提交（用户单任务一次commit）；Step7汇总 `fix(time): align archetype UTC instant contracts`。

### Step 3 — 贯通 native Light 排课 UTC 契约

- Requirements: REQ-001, REQ-002, REQ-003, REQ-008, REQ-011
- Dependencies: Step 2验收完成；源码改动保留未提交
- Baseline state: native Light窗口LocalDateTime和naive SQL，PB3/4 string；安全工作dirty。
- Observable outcome: 贯通 native Light 排课 UTC 契约，对应§8合同可观察。
- End state: 本Step目标完整编译/GREEN；下一Step未完成项保持清晰，不把该Step验证宣称全任务运行通过。
- Test-first gate: Required — §4.2对应行为RED；先下列测试，后生产/接线；Step7复用已先建临时门禁的RED，不新改业务。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 本Step按§12.5逐项评审适用面；测试/配置/SQL文件的N/A理由在各块明确。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 非触达能力按§4.8边界证据保留，原unique-key例外不能扩大。
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/ScheduleUtcConverterTest.java`

- Purpose: 隔离MockMvc或converter验证新UTC合同与原成功/错误shape
- Symbols: ScheduleUtcConverterTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: raw offset→同Instant；Command微秒→BO/PO/DTO；native PB3/4 string与输出Z、无naive fallback；OpenJavaDTO无虚构RPC；null原约束失败
- Input/output and state mapping: Mappers.getMappernative原LightFacadeConverter与两TeachingApplicationConvertor → raw offset→同Instant；Command微秒→BO/PO/DTO；native PB3/4 string与输出Z、无naive fallback；OpenJavaDTO无虚构RPC；null原约束失败
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void realMapperPreservesInstantAndUtcWire() {
    // Arrange: Mappers.getMappernative原LightFacadeConverter与两TeachingApplicationConvertor.
    // Inputs / expected assertions: raw offset→同Instant；Command微秒→BO/PO/DTO；native PB3/4 string与输出Z、无naive fallback；OpenJavaDTO无虚构RPC；null原约束失败.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ScheduleUtcConverterTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 2 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/ScheduleUtcContractTest.java`

- Purpose: 隔离MockMvc或converter验证新UTC合同与原成功/错误shape
- Symbols: ScheduleUtcContractTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 合法Z/+08:00、0–9小数；数字/浮点/数值字符串/boolean/array/object/空白/naive/null/缺失/无秒/offset秒/leap-second/坏JSON；invalid400与Manage零调用；实际OAS string/date-time/required/Z例子/原operationId/response
- Input/output and state mapping: actual Boot Jackson/MockMvc、原校验通过的native principal或Opencontext → 合法Z/+08:00、0–9小数；数字/浮点/数值字符串/boolean/array/object/空白/naive/null/缺失/无秒/offset秒/leap-second/坏JSON；invalid400与Manage零调用；实际OAS string/date-time/required/Z例子/原operationId/response
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void httpScheduleRequiresOffsetStringsAndDocumentsUtc() {
    // Arrange: actual Boot Jackson/MockMvc、原校验通过的native principal或Opencontext.
    // Inputs / expected assertions: 合法Z/+08:00、0–9小数；数字/浮点/数值字符串/boolean/array/object/空白/naive/null/缺失/无秒/offset秒/leap-second/坏JSON；invalid400与Manage零调用；实际OAS string/date-time/required/Z例子/原operationId/response.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ScheduleUtcContractTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 3 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/manage/SchoolClassManageTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: SchoolClassManageTest
- Repository evidence: 33: import java.time.LocalDateTime;；44: class SchoolClassManageTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: fixture用ZInstant；微秒值传Domain与Repository；null/非法窗口/duplicate/关联/tenant拒绝无save与新增event；事务/幂等原顺序
- Input/output and state mapping: 原Manage mocks/validator/repository → fixture用ZInstant；微秒值传Domain与Repository；null/非法窗口/duplicate/关联/tenant拒绝无save与新增event；事务/幂等原顺序
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void scheduleFlowValidatesCanonicalWindow() {
    // Arrange: 原Manage mocks/validator/repository.
    // Inputs / expected assertions: fixture用ZInstant；微秒值传Domain与Repository；null/非法窗口/duplicate/关联/tenant拒绝无save与新增event；事务/幂等原顺序.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=SchoolClassManageTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 4 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidatorTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: TeachingApplicationValidatorTest
- Repository evidence: 13: import java.time.LocalDateTime;；20: class TeachingApplicationValidatorTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 先micro归一后strictstart<end；null在@NotNull原组拒绝；IDs/operator/key原失败保持；无ValidationUtils承载全业务规则
- Input/output and state mapping: 原Default/native/model validation组 → 先micro归一后strictstart<end；null在@NotNull原组拒绝；IDs/operator/key原失败保持；无ValidationUtils承载全业务规则
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void scheduleValidationRejectsCanonicalInvalidWindow() {
    // Arrange: 原Default/native/model validation组.
    // Inputs / expected assertions: 先micro归一后strictstart<end；null在@NotNull原组拒绝；IDs/operator/key原失败保持；无ValidationUtils承载全业务规则.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=TeachingApplicationValidatorTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 5 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregateTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: SchoolClassAggregateTest
- Repository evidence: 14: import java.time.LocalDate;；19: class SchoolClassAggregateTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 用固定Instant；相邻不overlap、真实overlap仍拒绝、倒置/归一后零长拒绝；direct BO构造微秒；CourseSchedule所有import只rename
- Input/output and state mapping: CourseScheduleBO与原Aggregate → 用固定Instant；相邻不overlap、真实overlap仍拒绝、倒置/归一后零长拒绝；direct BO构造微秒；CourseSchedule所有import只rename
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void microUtcWindowsKeepHalfOpenOverlap() {
    // Arrange: CourseScheduleBO与原Aggregate.
    // Inputs / expected assertions: 用固定Instant；相邻不overlap、真实overlap仍拒绝、倒置/归一后零长拒绝；direct BO构造微秒；CourseSchedule所有import只rename.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=SchoolClassAggregateTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 6 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: NativeRpcMappingTest
- Repository evidence: 12: import java.time.Instant;；13: import java.time.LocalDateTime;
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: PBstarts_at/ends_at3/4为string；+08/Z→同Instant、输出Z；bad/naive/null INVALID_ARGUMENT或原业务false边界；metadata/权限保持
- Input/output and state mapping: 真实生成nativePB/mapper，原授权context → PBstarts_at/ends_at3/4为string；+08/Z→同Instant、输出Z；bad/naive/null INVALID_ARGUMENT或原业务false边界；metadata/权限保持
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void nativeScheduleWireUsesUtcInstant() {
    // Arrange: 真实生成nativePB/mapper，原授权context.
    // Inputs / expected assertions: PBstarts_at/ends_at3/4为string；+08/Z→同Instant、输出Z；bad/naive/null INVALID_ARGUMENT或原业务false边界；metadata/权限保持.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=NativeRpcMappingTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 7 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeSecurityContractTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: NativeSecurityContractTest
- Repository evidence: NativeSecurityContractTest现有排课fixture已使用2026-10-13T01:00:00Z/02:00:00Z；原scope/role/字段写守卫拒绝断言；当前SHA256 1d4fe02c3b2786fdf6f4cebf6f2fcdbdf4d67a062239926d5d3094dceb69a35a；并行安全工作已刷新证据，实施不得覆盖。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 只把被调用排课时间字符串换成Z；原SERVICE/USER auth/role/field-write拒绝断言保留；不调整授权期望来让测试通过
- Input/output and state mapping: 当前并行安全测试原fixtures → 只把被调用排课时间字符串换成Z；原SERVICE/USER auth/role/field-write拒绝断言保留；不调整授权期望来让测试通过
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void scheduleTimeFixturesRespectExistingSecurity() {
    // Arrange: 当前并行安全测试原fixtures.
    // Inputs / expected assertions: 只把被调用排课时间字符串换成Z；原SERVICE/USER auth/role/field-write拒绝断言保留；不调整授权期望来让测试通过.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=NativeSecurityContractTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 8 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/resources/mybatis/h2-schema.sql`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: h2-schema
- Repository evidence: 126: starts_at TIMESTAMP NOT NULL,；127: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: class_course_schedules_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for class_course_schedules_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 9 — `RENAME egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/vos/CourseSchedule.java`

- Purpose: 迁移到同目录CourseScheduleBO.java，两个Instant时间字段，保留不可变窗口不变量
- Symbols: CourseSchedule → CourseScheduleBO
- Repository evidence: 5: import java.time.LocalDateTime;；8: public record CourseSchedule(CourseCode courseCode, LocalDateTime startsAt, LocalDateTime endsAt) {
- Dependencies and consumers: Aggregate/DomainService/infraServiceImpl/TeachingApplicationConvertor及同Step测试
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原同包immutable record改名CourseScheduleBO；startsAt/endsAt Instant；overlaps参数新BO
- Input/output and state mapping: rawInstant→microInstant→strict窗口；对齐窗口半开overlap
- Error and edge behavior: 原null防御与INVALID_SCHEDULE保持，归一后零长/倒置拒绝
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// RENAME same package path: CourseSchedule.java -> CourseScheduleBO.java; immutable value object record.
public record CourseScheduleBO(CourseCode courseCode, Instant startsAt, Instant endsAt) {
    public CourseScheduleBO {
        Objects.requireNonNull(courseCode); Objects.requireNonNull(startsAt); Objects.requireNonNull(endsAt);
        startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
        if (!startsAt.isBefore(endsAt)) throw new TeachingDomainException("INVALID_SCHEDULE", "startsAt must be before endsAt");
    }
    public boolean overlaps(CourseScheduleBO other) {
        return startsAt.isBefore(other.endsAt()) && other.startsAt().isBefore(endsAt);
    }
}
// Update all exact consumers listed in this Step; preserve CourseCode and package vos ownership.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 10 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/command/ScheduleCourseCommand.java`

- Purpose: 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ScheduleCourseCommand
- Repository evidence: 3: import java.time.LocalDateTime;；5: public record ScheduleCourseCommand(
- Dependencies and consumers: 既有Adapter/FacadeConvertor→Manage/Validator→Domain/PO及响应规范值
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 所有非时间字段/现有约束/构造检查保持；Light两Command字段LocalDateTime→Instant，Service四Command原Instant不变
- Input/output and state mapping: 非nullInstant→truncatedTo(MICROS)；null留给原Jakarta约束；其他字段逐原mapper
- Error and edge behavior: micro后start≥end由既有业务/validator拒绝，不用DBround制造成功
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record ScheduleCourseCommand(/* retain exact existing components; */ Instant startsAt, Instant endsAt /* and other existing fields */) {
    public ScheduleCourseCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
        // Preserve current constructor ID/association guards and carrier annotations; no new broad business rule here.
        // Original Default/native/model annotations and validators reject null, start >= end AFTER canonicalization.
        // Light changes LocalDateTime -> Instant; Service/Open already Instant, only canonicalize.
    }
}
// Every HTTP/RPC/direct construction reaches the same canonical carrier; no server-zone or +8h conversion.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 11 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`

- Purpose: 时间字段Instant；组件级严格字符串@JsonDeserialize守卫；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ScheduleCourseRequest / nested UtcInstantInputDeserializer
- Repository evidence: 5: import java.time.LocalDateTime;；7: public record ScheduleCourseRequest(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt) {
- Dependencies and consumers: HTTP SchoolClassController与TeachingRequestValidator/原Mapper/Springdoc
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: Request两component Instant＋@NotNull＋字段级@JsonDeserialize，内嵌public static decoder
- Input/output and state mapping: offset/Z严格文本→Instant raw nanos→原validator/Converter→microCommand
- Error and edge behavior: 数字与坏shape/格式拒绝；null原@NotNull；不跨request改变其他日期解码
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record ScheduleCourseRequest(
        @NotNull @JsonDeserialize(using = UtcInstantInputDeserializer.class) Instant startsAt,
        @NotNull @JsonDeserialize(using = UtcInstantInputDeserializer.class) Instant endsAt) {
    public static final class UtcInstantInputDeserializer extends StdScalarDeserializer<Instant> {
        // Jackson-compatible no-arg constructor, stateless, no Bean/global ObjectMapper registration.
        @Override public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            // Require JsonToken.VALUE_STRING. No number/stringified-number coercion, array/object/boolean.
            // Require full string: 4-digit-year-MM-dd [Tt] HH:mm:ss [fraction 1..9] ([Zz] or +/-HH:mm).
            // Seconds 00..59; no whitespace, missing seconds, offset seconds, zone names, leap-second.
            // Use JDK strict OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }
            String text = parser.getText();
            // Full-string pattern: [0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-5][0-9]
            // followed by optional .fraction(1..9) and required [Zz] or [+-]HH:mm.
            if (!STRICT_OFFSET_INSTANT.matcher(text).matches()) {
                return (Instant) context.handleWeirdStringValue(Instant.class, text, "offset or Z datetime required");
            }
            try {
                return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
            } catch (DateTimeParseException failure) {
                return (Instant) context.handleWeirdStringValue(Instant.class, text, "invalid offset datetime");
            }
            // Static compiled JDK Pattern inside existing nested class; no default-zone fallback or new utility.
        }
    }
}
// Jackson null/absent remains null for @NotNull; preserve raw parsed nanos, Command later truncates MICROS.
// Output uses existing JavaTimeModule ISO Instant UTC Z; no global numeric timestamp change elsewhere.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 12 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java`

- Purpose: 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ScheduleCourseDTO
- Repository evidence: 9: import java.time.LocalDateTime;；11: public record ScheduleCourseDTO(
- Dependencies and consumers: nativeFacade/原convertor/Manage；Open仅原JavaFacade
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: facadeDTO两窗口Instant；原字段/annotation group/接口方法形状保持
- Input/output and state mapping: PB解析值或Open JavaInstant→原分组校验→Command micro
- Error and edge behavior: null缺失/invalid保持原约束路径
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep current record/class disposition and every existing component/annotation/group.
// Change startsAt and endsAt from LocalDateTime to java.time.Instant.
// Retain @NotNull in current Default/native RPC groups and positive ID/operator/key validation.
// Offset/Z parse happens in current native PB converter; Open carrier is Java Instant, no new transport.
// Current TeachingApplicationConvertor maps to ScheduleCourseCommand and micro canonicalization;
// no direct DTO -> PO bypass, no coercion through server default zone.
// JSON projections use existing Jackson Instant UTC Z; preserve other facade method signatures.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 13 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/po/ClassCourseSchedulePO.java`

- Purpose: 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ClassCourseSchedulePO
- Repository evidence: 14: import java.time.LocalDateTime;；22: @TableName("class_course_schedules")
- Dependencies and consumers: TeachingApplicationConvertor/infraService/RepoDAO/原Mapper XML
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 两PO仅startsAt/endsAt Instant；既有builder/继承注解满足EgonModel @SuperBuilder
- Input/output and state mapping: BO/Command microInstant→PO→现有MyBatis binding→timestamptz(6)
- Error and edge behavior: null/tenant/version/软删除验证不旁路，无新typehandler
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)
@SuperBuilder @EqualsAndHashCode(callSuper = true)
@TableName(/* preserve current exact table mapping */)
public class ClassCourseSchedulePO extends EgonModel<ClassCourseSchedulePO> {
    // Keep all current business fields/column annotations, replace only these field types.
    private Instant startsAt;
    private Instant endsAt;
    // Inherit audit Instant and deletedAt LocalDateTime/version/tenant from EgonModel; do not redeclare.
    // SuperBuilder matches superclass; generated mapper and existing builders/constructors compile.
}
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 14 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: TeachingApplicationConvertor
- Repository evidence: 18: @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)；20: public interface TeachingApplicationConvertor extends BaseForwardConverter<Course, CourseResult> {
- Dependencies and consumers: SchoolClassManageImpl、Domain/infra Service/所有mapper测试
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原TeachingApplicationConvertor.toSchedule(ScheduleCourseCommand, Course)返回CourseScheduleBO；其它投影方法不变
- Input/output and state mapping: Controller/Facade按原构造器造Command；TeachingApplicationConvertor.toSchedule仅Command+Course→BO；infraService原builder造PO
- Error and edge behavior: null按原mapper策略与下一边界约束处理
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep existing MapStruct declaration, generated implementation and Egon BaseConverter/Forward contract.
// Controller/Facade retain direct ScheduleCourseCommand construction: Request/DTO Instant -> constructor -> micro Instant.
// CourseSchedule.java -> CourseScheduleBO imports and mapping return/parameter types in same package vos.
// Existing toSchedule(command, course): command micro startsAt/endsAt and course.code -> CourseScheduleBO.
// ClassCourseSchedulePO construction/readback stays in infrastructure SchoolClassDomainServiceImpl, not in this mapper.
// Preserve schoolClassId/courseId/operatorId/idempotencyKey mapping and null policy/technical field exclusions.
// Add no reflective BeanUtils, JSON roundtrip or extra forwarding mapper. Compile annotation processor output.
// Real mapper tests construct micro Command then assert toSchedule yields the same micro BO;
// test existing infra builder/readback separately. No invented mapper methods or time fields on scheduleCount responses.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 15 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/LightFacadeConverter.java`

- Purpose: Instant parse/toString；PB 3/4 string字段不变
- Symbols: toProtoTime/fromProtoTime
- Repository evidence: 13: import java.time.LocalDateTime;；19: @Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
- Dependencies and consumers: 真实PB/nativeFacade mapping；NativeRpcConfiguration既有错误mapper
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原native文本parse/format变offset-awareInstant/Z，string3/4不变
- Input/output and state mapping: PBstring→InstantDTO→microCommand/BO；microInstant→PB UTC Z
- Error and edge behavior: naive/offset非法原DateTimeParseException→INVALID_ARGUMENT；权限不变
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Preserve original MapStruct mapper and Egon BaseConverter/BaseForwardConverter participants.
// Replace existing fromProtoTime(String) -> Instant and toProtoTime(Instant) -> String helper signatures:
// validate text has Z/numeric offset; OffsetDateTime.parse(text, ISO_OFFSET_DATE_TIME).toInstant().
// No empty/naive server-zone fallback. Existing DateTimeParseException/ConstraintViolationException
// reach NativeRpcConfiguration INVALID_ARGUMENT; keep provider business exception -> original result.
// schedule Proto string fields starts_at/ends_at map to DTO Instant; existing exposed time-carrier output -> Instant.toString().
// Do not add startsAt/endsAt to SchoolClassDetailVO or GraphQL; their existing scheduleCount remains unchanged.
// Keep IDs, request metadata, unknown fields, SERVICE/USER authorization and non-time conversions.
// Update only affected CourseScheduleBO/import/reference mappings, no hand-written parallel converter.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 16 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregate.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassAggregate
- Repository evidence: 12: public final class SchoolClassAggregate {
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Replace CourseSchedule with same-package CourseScheduleBO in imports, collections and schedule API.
// For each existing schedule call, BO constructor guarantees micro Instant and strict start < end.
// Original overlap condition remains half-open: new.startsAt < old.endsAt && old.startsAt < new.endsAt.
// Keep aggregate identity/state/membership/CourseCode and original exception/event behavior.
// No new validation registry/pattern; direct immutable value object enforces the same domain invariant.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 17 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/validators/TeachingDomainValidator.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: TeachingDomainValidator
- Repository evidence: 16: public class TeachingDomainValidator extends BaseValidator {；25: throw new TeachingDomainException("CLASS_NOT_ACTIVE", "School class must be active");
- Dependencies and consumers: 对应Request/Application/Domain handoff调用方；TEST-001/002/005
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原validation方法类型跟随Instant/BO；校验分组/错误码保持
- Input/output and state mapping: 原carrier约束→micro窗口strictstart<end→Domain，仅合法继续
- Error and edge behavior: null、折叠、倒置拒绝；原ID/关联/group rules全保留
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep TeachingDomainValidator extends current BaseValidator / existing bean, @Slf4j, @RequiredArgsConstructor,
// explicit current @Component name and @Qualifier("egonColaValidationUtils") propagation.
// validateSchedule / current teaching method first validates existing Bean/groups and identifiers.
// Use startsAt/endsAt Instant (or micro locals for raw Request/DTO), no LocalDateTime/defaultZone conversion.
// Compare canonical truncatedTo(MICROS) endpoints with strict isBefore; reject collapsed/reversed windows
// using the current boundary IllegalArgumentException or TeachingDomainException INVALID_SCHEDULE.
// Request null rejects through @NotNull; application/domain handoff maintains original validation groups.
// Update CourseScheduleBO import/signature where consumed, and retain all other teaching rules unchanged.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 18 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/service/SchoolClassDomainService.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassDomainService
- Repository evidence: 14: /** Persistence-owning school-class domain service contract. */；15: public interface SchoolClassDomainService {
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Update CourseSchedule references/signatures/imports to CourseScheduleBO and window parameter types to Instant.
// Retain exact existing method names, repository ports, associated ID/value types and dependency direction.
// Calls carrying commands/BO are validated through original domain service/application handoff.
// No new Spring/MP/OpenAPI dependency in Domain and no new interface solely for forwarding time conversion.
// All implementations and caller compile together in owning Step; no published intermediate incompatible signature.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 19 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassDomainServiceImpl
- Repository evidence: 27: /** Business rules and orchestration for the school-class domain service. */；30: @Service("schoolClassDomainService")
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Preserve existing named Bean, @Slf4j, @RequiredArgsConstructor/Qualifier and domain-service port implementation.
// saveAggregate uses the existing ClassCourseSchedulePO.builder().startsAt(schedule.startsAt()).endsAt(schedule.endsAt());
// findAggregateById constructs new CourseScheduleBO(course.code(), po.getStartsAt(), po.getEndsAt()).
// Read PO through original Repository/DAO; canonical BO constructor handles precision, no Timestamp string/default zone.
// Existing guarded save/update, tenant/version/deleted_at/LOCAL boundaries and association failures remain.
// No new DDL execution or DataSource use; SQL schema change is the separate authorized source-template edit.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 20 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidator.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: TeachingApplicationValidator
- Repository evidence: 16: @Component("teachingApplicationValidator")；20: public class TeachingApplicationValidator extends BaseValidator {
- Dependencies and consumers: 对应Request/Application/Domain handoff调用方；TEST-001/002/005
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原validation方法类型跟随Instant/BO；校验分组/错误码保持
- Input/output and state mapping: 原carrier约束→micro窗口strictstart<end→Domain，仅合法继续
- Error and edge behavior: null、折叠、倒置拒绝；原ID/关联/group rules全保留
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep TeachingApplicationValidator extends current BaseValidator / existing bean, @Slf4j, @RequiredArgsConstructor,
// explicit current @Component name and @Qualifier("egonColaValidationUtils") propagation.
// validateSchedule / current teaching method first validates existing Bean/groups and identifiers.
// Use startsAt/endsAt Instant (or micro locals for raw Request/DTO), no LocalDateTime/defaultZone conversion.
// Compare canonical truncatedTo(MICROS) endpoints with strict isBefore; reject collapsed/reversed windows
// using the current boundary IllegalArgumentException or TeachingDomainException INVALID_SCHEDULE.
// Request null rejects through @NotNull; application/domain handoff maintains original validation groups.
// Update CourseScheduleBO import/signature where consumed, and retain all other teaching rules unchanged.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 21 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/validators/TeachingRequestValidator.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: TeachingRequestValidator
- Repository evidence: 14: @Component("teachingRequestValidator")；17: public class TeachingRequestValidator extends BaseValidator {
- Dependencies and consumers: 对应Request/Application/Domain handoff调用方；TEST-001/002/005
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原validation方法类型跟随Instant/BO；校验分组/错误码保持
- Input/output and state mapping: 原carrier约束→micro窗口strictstart<end→Domain，仅合法继续
- Error and edge behavior: null、折叠、倒置拒绝；原ID/关联/group rules全保留
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep TeachingRequestValidator extends current BaseValidator / existing bean, @Slf4j, @RequiredArgsConstructor,
// explicit current @Component name and @Qualifier("egonColaValidationUtils") propagation.
// validateSchedule / current teaching method first validates existing Bean/groups and identifiers.
// Use startsAt/endsAt Instant (or micro locals for raw Request/DTO), no LocalDateTime/defaultZone conversion.
// Compare canonical truncatedTo(MICROS) endpoints with strict isBefore; reject collapsed/reversed windows
// using the current boundary IllegalArgumentException or TeachingDomainException INVALID_SCHEDULE.
// Request null rejects through @NotNull; application/domain handoff maintains original validation groups.
// Update CourseScheduleBO import/signature where consumed, and retain all other teaching rules unchanged.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 22 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/manage/impl/SchoolClassManageImpl.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassManageImpl
- Repository evidence: 30: @Service("schoolClassManageImpl")；34: public class SchoolClassManageImpl implements SchoolClassManage {
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep @Service current name, @Slf4j, @RequiredArgsConstructor, existing @Qualifier fields/transaction.
// Original validate Command -> claim/check associations/load state -> domain schedule -> guarded repo/save/event sequence.
// Consume already micro Instant; construct renamed CourseScheduleBO through original converter/domain boundary.
// Preserve original duplicate, overlap, tenant/permission, operator and idempotency behavior and rollback.
// Response/view derives canonical domain/saved value; do not echo unpersisted raw nanos or invent retries.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 23 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/handler/GlobalExceptionHandler.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: GlobalExceptionHandler
- Repository evidence: 15: @RestControllerAdvice；17: public class GlobalExceptionHandler {
- Dependencies and consumers: 两ScheduleUtcContractTest、既有Global advice/API wrapper
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 仅排课Request startsAt/endsAt decode失败映射已有400 VALIDATION_ERROR
- Input/output and state mapping: 精确Request/field mapping cause→ResultRecord；无业务调用
- Error and edge behavior: 非匹配异常保持Spring原处理，不吞异常/全局改JSON格式失败
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Extend existing GlobalExceptionHandler narrowly for HttpMessageNotReadableException.
// Inspect Jackson cause / JsonMappingException Reference path, actual ScheduleCourseRequest type and
// startsAt or endsAt component. Do not reclassify unreadable errors from unrelated DTOs/endpoints.
// For matched schedule-time token/format failure return HTTP 400 ResultRecord.failure(
//     ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getStatus(), safe field message).
// Preserve current MethodArgumentNotValidException, ValidationException, IllegalArgumentException,
// TeachingUseCaseException/UserUseCaseException handlers and unrelated default Spring error handling.
// Existing @Slf4j; use a stable named advice if touched under Rule 4, no credential/raw body logging.
// For nonmatching HttpMessageNotReadableException delegate/rethrow to existing Spring handling;
// do not install a broad catch(Exception) or change public wrapper timestamp/payload/status semantics.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 24 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassController.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: SchoolClassController
- Repository evidence: SchoolClassController.schedule @PreAuthorize/RBACAPIResource 与 fieldWrite.requireWritableFields(Set.of("startsAt", "endsAt"))；原direct Command构造；当前SHA256 d5deef6f3cd03818e92f1ea5929178beac6defaacb89158a0bc31fbc68d7f8ee；并行安全工作已刷新证据，实施不得覆盖。
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep route, @Valid/@Validated, authenticated principal-derived IDs, operationId/security and response wrapper.
// @Schema for startsAt/endsAt is string/date-time with Z examples; @Operation description requires offset/Z
// and explicitly states canonical micros. Do not add preflight endpoint or request business-zone parameter.
// Keep field-write guard and TeachingRequestValidator, then direct new ScheduleCourseCommand(...) -> Manage.
// Existing adapter converter only projects SchoolClassResult -> SchoolClassDetailVO; no new mapper forwarding layer.
// Negative decode/validation never calls Manage; response retains scheduleCount without new time fields.
// Use actual OAS output tests rather than only annotation text; ResultRecord timestamp remains UnixMillis.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 25 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto`

- Purpose: 只改3/4字段的时间语义注释为ISO_INSTANT UTC，保留field number/wire type
- Symbols: ScheduleCourseRpcRequest
- Repository evidence: 50: optional string starts_at = 3;；52: optional string ends_at = 4;
- Dependencies and consumers: 原native gRPC/PB caller及NativeRpcMappingTest
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 仅comments，PB wire字段号/载体完全不变
- Input/output and state mapping: PBstring3/4⇄Instant，protocolcomments新契约
- Error and edge behavior: 坏string仍由converter/既有RPC mapper拒绝
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```protobuf
// Keep package/service/message names, string starts_at = 3 and string ends_at = 4 unchanged.
// Replace only the affected schedule time comments: input ISO offset/Z string, output UTC ISO_INSTANT Z.
// Explain input accepts fractional seconds up to nanos, business Command truncates to micros before validation.
// Do not switch to google.protobuf.Timestamp in native Light, renumber fields, alter presence or metadata.
// Regenerate PB through current Maven plugin as part of owning source test lifecycle.
// NativeRpcMappingTest asserts real descriptor field numbers/types and offset input -> canonical wire Z.
```

- Verification contribution: Step 3 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 26 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`

- Purpose: 原位改TIMESTAMP(6) WITH TIME ZONE；其他schema/index/lifecycle不变
- Symbols: class_course_schedules_0/1.starts_at/ends_at
- Repository evidence: 165: starts_at TIMESTAMP NOT NULL,；166: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: class_course_schedules_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for class_course_schedules_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 27 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 按新字节重算SHA-256；version/path/family不变
- Symbols: scripts[0].sha256
- Repository evidence: 7: "sha256": "e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31"
- Dependencies and consumers: 原MP-SDJ manifest reader、新模板生成、scratch审计
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 只更新该源SQL entry checksum，实际key名称按当前manifest
- Input/output and state mapping: 刚修改SQL文件原始bytes→完整64hexSHA→现有matchingentry
- Error and edge behavior: path/version不匹配或摘要不同直接gate失败，不修复真实DB
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```json
{
  "existing manifest": "keep original structure, family, version, role, path, target declarations",
  "matching V20260913_001 entry": {
    "sha256": "MessageDigest/SHA-256 of the just-edited owning SQL raw bytes, all 64 lowercase hex digits",
    "other keys": "unchanged; no added version, no cross-project copied checksum"
  }
}
// Re-read this exact manifest JSON and owning source SQL; assert the matching path/version and digest.
// Preserve all unrelated entries and current formatting; no automatic repair/rebuild of applied ddl_history.
```

- Verification contribution: TEST-009四source entry raw-byte摘要精确一致
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-light
- Verification command:

```bash
./mvnw -o -Dtest=ScheduleUtcContractTest,ScheduleUtcConverterTest,TeachingApplicationValidatorTest,SchoolClassAggregateTest,SchoolClassManageTest,NativeRpcMappingTest,NativeSecurityContractTest,LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result: 对应TEST全部GREEN；Step3/4所有BO/import/mapper编译成功；Step5四源列与摘要GREEN；Step6七套配置测试；Step7generate/check与回归无新增发现。可选PG若未授权运行记Not run/Skipped，不能算PASS。
- Failure returns to: 当前Step对应File（行为/映射/fixture）；声明坐标缺失或既有编译失败记录baseline；真实JDBC契约或行为设计冲突返回Spec，不新增依赖/handler或调整guard。
- Completion criteria: 该Step测试成功输出、相关静态门禁/完整diff与原guard保持证据齐全；没有误改其它路径。
- Rollback: 只撤销本人本Step的时间hunk或前向修正；保留之前GREEN与用户dirty；SQL为未部署源模板，无DBrollback/repair。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/ScheduleUtcConverterTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/ScheduleUtcContractTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/manage/SchoolClassManageTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidatorTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregateTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeSecurityContractTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/resources/mybatis/h2-schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/vos/CourseSchedule.java` → `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/vos/CourseScheduleBO.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/command/ScheduleCourseCommand.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/po/ClassCourseSchedulePO.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/LightFacadeConverter.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregate.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/validators/TeachingDomainValidator.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/service/SchoolClassDomainService.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidator.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/validators/TeachingRequestValidator.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/manage/impl/SchoolClassManageImpl.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/handler/GlobalExceptionHandler.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassController.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/repository-manifest.json`
- Commit: 不单独提交（用户单任务一次commit）；Step7汇总 `fix(time): align archetype UTC instant contracts`。

### Step 4 — 贯通 Light-open 排课 UTC 契约

- Requirements: REQ-001, REQ-002, REQ-003, REQ-008, REQ-011
- Dependencies: Step 3验收完成；源码改动保留未提交
- Baseline state: Light-open同样LocalDateTime窗口，原Java facade无transport注册。
- Observable outcome: 贯通 Light-open 排课 UTC 契约，对应§8合同可观察。
- End state: 本Step目标完整编译/GREEN；下一Step未完成项保持清晰，不把该Step验证宣称全任务运行通过。
- Test-first gate: Required — §4.2对应行为RED；先下列测试，后生产/接线；Step7复用已先建临时门禁的RED，不新改业务。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 本Step按§12.5逐项评审适用面；测试/配置/SQL文件的N/A理由在各块明确。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 非触达能力按§4.8边界证据保留，原unique-key例外不能扩大。
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/pojo/convertor/ScheduleUtcConverterTest.java`

- Purpose: 隔离MockMvc或converter验证新UTC合同与原成功/错误shape
- Symbols: ScheduleUtcConverterTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: raw offset→同Instant；Command微秒→BO/PO/DTO；native PB3/4 string与输出Z、无naive fallback；OpenJavaDTO无虚构RPC；null原约束失败
- Input/output and state mapping: Mappers.getMappernative原LightFacadeConverter与两TeachingApplicationConvertor → raw offset→同Instant；Command微秒→BO/PO/DTO；native PB3/4 string与输出Z、无naive fallback；OpenJavaDTO无虚构RPC；null原约束失败
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void realMapperPreservesInstantAndUtcWire() {
    // Arrange: Mappers.getMappernative原LightFacadeConverter与两TeachingApplicationConvertor.
    // Inputs / expected assertions: raw offset→同Instant；Command微秒→BO/PO/DTO；native PB3/4 string与输出Z、无naive fallback；OpenJavaDTO无虚构RPC；null原约束失败.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ScheduleUtcConverterTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 2 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/ScheduleUtcContractTest.java`

- Purpose: 隔离MockMvc或converter验证新UTC合同与原成功/错误shape
- Symbols: ScheduleUtcContractTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 合法Z/+08:00、0–9小数；数字/浮点/数值字符串/boolean/array/object/空白/naive/null/缺失/无秒/offset秒/leap-second/坏JSON；invalid400与Manage零调用；实际OAS string/date-time/required/Z例子/原operationId/response
- Input/output and state mapping: actual Boot Jackson/MockMvc、原校验通过的native principal或Opencontext → 合法Z/+08:00、0–9小数；数字/浮点/数值字符串/boolean/array/object/空白/naive/null/缺失/无秒/offset秒/leap-second/坏JSON；invalid400与Manage零调用；实际OAS string/date-time/required/Z例子/原operationId/response
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void httpScheduleRequiresOffsetStringsAndDocumentsUtc() {
    // Arrange: actual Boot Jackson/MockMvc、原校验通过的native principal或Opencontext.
    // Inputs / expected assertions: 合法Z/+08:00、0–9小数；数字/浮点/数值字符串/boolean/array/object/空白/naive/null/缺失/无秒/offset秒/leap-second/坏JSON；invalid400与Manage零调用；实际OAS string/date-time/required/Z例子/原operationId/response.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ScheduleUtcContractTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 3 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/SchoolClassManageTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: SchoolClassManageTest
- Repository evidence: 33: import java.time.LocalDateTime;；44: class SchoolClassManageTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: fixture用ZInstant；微秒值传Domain与Repository；null/非法窗口/duplicate/关联/tenant拒绝无save与新增event；事务/幂等原顺序
- Input/output and state mapping: 原Manage mocks/validator/repository → fixture用ZInstant；微秒值传Domain与Repository；null/非法窗口/duplicate/关联/tenant拒绝无save与新增event；事务/幂等原顺序
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void scheduleFlowValidatesCanonicalWindow() {
    // Arrange: 原Manage mocks/validator/repository.
    // Inputs / expected assertions: fixture用ZInstant；微秒值传Domain与Repository；null/非法窗口/duplicate/关联/tenant拒绝无save与新增event；事务/幂等原顺序.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=SchoolClassManageTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 4 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidatorTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: TeachingApplicationValidatorTest
- Repository evidence: 13: import java.time.LocalDateTime;；20: class TeachingApplicationValidatorTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 先micro归一后strictstart<end；null在@NotNull原组拒绝；IDs/operator/key原失败保持；无ValidationUtils承载全业务规则
- Input/output and state mapping: 原Default/native/model validation组 → 先micro归一后strictstart<end；null在@NotNull原组拒绝；IDs/operator/key原失败保持；无ValidationUtils承载全业务规则
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void scheduleValidationRejectsCanonicalInvalidWindow() {
    // Arrange: 原Default/native/model validation组.
    // Inputs / expected assertions: 先micro归一后strictstart<end；null在@NotNull原组拒绝；IDs/operator/key原失败保持；无ValidationUtils承载全业务规则.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=TeachingApplicationValidatorTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 5 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregateTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: SchoolClassAggregateTest
- Repository evidence: 14: import java.time.LocalDate;；19: class SchoolClassAggregateTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 用固定Instant；相邻不overlap、真实overlap仍拒绝、倒置/归一后零长拒绝；direct BO构造微秒；CourseSchedule所有import只rename
- Input/output and state mapping: CourseScheduleBO与原Aggregate → 用固定Instant；相邻不overlap、真实overlap仍拒绝、倒置/归一后零长拒绝；direct BO构造微秒；CourseSchedule所有import只rename
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void microUtcWindowsKeepHalfOpenOverlap() {
    // Arrange: CourseScheduleBO与原Aggregate.
    // Inputs / expected assertions: 用固定Instant；相邻不overlap、真实overlap仍拒绝、倒置/归一后零长拒绝；direct BO构造微秒；CourseSchedule所有import只rename.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=SchoolClassAggregateTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 6 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: SchoolClassRepositoryImplTest
- Repository evidence: 15: class SchoolClassRepositoryImplTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: Instant读写与PO继承元数据；tenant/version/deleted_at/软删除守卫保持；H2不证明PG时区/driver正确
- Input/output and state mapping: 原H2/MP guarded Repository夹具 → Instant读写与PO继承元数据；tenant/version/deleted_at/软删除守卫保持；H2不证明PG时区/driver正确
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void utcTimeMappingsKeepGuardedPersistence() {
    // Arrange: 原H2/MP guarded Repository夹具.
    // Inputs / expected assertions: Instant读写与PO继承元数据；tenant/version/deleted_at/软删除守卫保持；H2不证明PG时区/driver正确.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=SchoolClassRepositoryImplTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 7 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/resources/mybatis/h2-schema.sql`

- Purpose: 原案例时间改UTC Instant/BO、SQL fixture列带时区；新增等值/naive/精度/边界断言
- Symbols: h2-schema
- Repository evidence: 126: starts_at TIMESTAMP NOT NULL,；127: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: light_class_course_schedules_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for light_class_course_schedules_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 8 — `RENAME egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/vos/CourseSchedule.java`

- Purpose: 迁移到同目录CourseScheduleBO.java，两个Instant时间字段，保留不可变窗口不变量
- Symbols: CourseSchedule → CourseScheduleBO
- Repository evidence: 5: import java.time.LocalDateTime;；8: public record CourseSchedule(CourseCode courseCode, LocalDateTime startsAt, LocalDateTime endsAt) {
- Dependencies and consumers: Aggregate/DomainService/infraServiceImpl/TeachingApplicationConvertor及同Step测试
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原同包immutable record改名CourseScheduleBO；startsAt/endsAt Instant；overlaps参数新BO
- Input/output and state mapping: rawInstant→microInstant→strict窗口；对齐窗口半开overlap
- Error and edge behavior: 原null防御与INVALID_SCHEDULE保持，归一后零长/倒置拒绝
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// RENAME same package path: CourseSchedule.java -> CourseScheduleBO.java; immutable value object record.
public record CourseScheduleBO(CourseCode courseCode, Instant startsAt, Instant endsAt) {
    public CourseScheduleBO {
        Objects.requireNonNull(courseCode); Objects.requireNonNull(startsAt); Objects.requireNonNull(endsAt);
        startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
        if (!startsAt.isBefore(endsAt)) throw new TeachingDomainException("INVALID_SCHEDULE", "startsAt must be before endsAt");
    }
    public boolean overlaps(CourseScheduleBO other) {
        return startsAt.isBefore(other.endsAt()) && other.startsAt().isBefore(endsAt);
    }
}
// Update all exact consumers listed in this Step; preserve CourseCode and package vos ownership.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 9 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/command/ScheduleCourseCommand.java`

- Purpose: 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ScheduleCourseCommand
- Repository evidence: 3: import java.time.LocalDateTime;；5: public record ScheduleCourseCommand(
- Dependencies and consumers: 既有Adapter/FacadeConvertor→Manage/Validator→Domain/PO及响应规范值
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 所有非时间字段/现有约束/构造检查保持；Light两Command字段LocalDateTime→Instant，Service四Command原Instant不变
- Input/output and state mapping: 非nullInstant→truncatedTo(MICROS)；null留给原Jakarta约束；其他字段逐原mapper
- Error and edge behavior: micro后start≥end由既有业务/validator拒绝，不用DBround制造成功
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record ScheduleCourseCommand(/* retain exact existing components; */ Instant startsAt, Instant endsAt /* and other existing fields */) {
    public ScheduleCourseCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
        // Preserve current constructor ID/association guards and carrier annotations; no new broad business rule here.
        // Original Default/native/model annotations and validators reject null, start >= end AFTER canonicalization.
        // Light changes LocalDateTime -> Instant; Service/Open already Instant, only canonicalize.
    }
}
// Every HTTP/RPC/direct construction reaches the same canonical carrier; no server-zone or +8h conversion.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 10 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`

- Purpose: 时间字段Instant；组件级严格字符串@JsonDeserialize守卫；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ScheduleCourseRequest / nested UtcInstantInputDeserializer
- Repository evidence: 5: import java.time.LocalDateTime;；7: public record ScheduleCourseRequest(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt) {
- Dependencies and consumers: HTTP SchoolClassController与TeachingRequestValidator/原Mapper/Springdoc
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: Request两component Instant＋@NotNull＋字段级@JsonDeserialize，内嵌public static decoder
- Input/output and state mapping: offset/Z严格文本→Instant raw nanos→原validator/Converter→microCommand
- Error and edge behavior: 数字与坏shape/格式拒绝；null原@NotNull；不跨request改变其他日期解码
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record ScheduleCourseRequest(
        @NotNull @JsonDeserialize(using = UtcInstantInputDeserializer.class) Instant startsAt,
        @NotNull @JsonDeserialize(using = UtcInstantInputDeserializer.class) Instant endsAt) {
    public static final class UtcInstantInputDeserializer extends StdScalarDeserializer<Instant> {
        // Jackson-compatible no-arg constructor, stateless, no Bean/global ObjectMapper registration.
        @Override public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            // Require JsonToken.VALUE_STRING. No number/stringified-number coercion, array/object/boolean.
            // Require full string: 4-digit-year-MM-dd [Tt] HH:mm:ss [fraction 1..9] ([Zz] or +/-HH:mm).
            // Seconds 00..59; no whitespace, missing seconds, offset seconds, zone names, leap-second.
            // Use JDK strict OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }
            String text = parser.getText();
            // Full-string pattern: [0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-5][0-9]
            // followed by optional .fraction(1..9) and required [Zz] or [+-]HH:mm.
            if (!STRICT_OFFSET_INSTANT.matcher(text).matches()) {
                return (Instant) context.handleWeirdStringValue(Instant.class, text, "offset or Z datetime required");
            }
            try {
                return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
            } catch (DateTimeParseException failure) {
                return (Instant) context.handleWeirdStringValue(Instant.class, text, "invalid offset datetime");
            }
            // Static compiled JDK Pattern inside existing nested class; no default-zone fallback or new utility.
        }
    }
}
// Jackson null/absent remains null for @NotNull; preserve raw parsed nanos, Command later truncates MICROS.
// Output uses existing JavaTimeModule ISO Instant UTC Z; no global numeric timestamp change elsewhere.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 11 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/facade/teaching/dto/ScheduleCourseDTO.java`

- Purpose: 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ScheduleCourseDTO
- Repository evidence: 8: import java.time.LocalDateTime;；10: public record ScheduleCourseDTO(
- Dependencies and consumers: nativeFacade/原convertor/Manage；Open仅原JavaFacade
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: facadeDTO两窗口Instant；原字段/annotation group/接口方法形状保持
- Input/output and state mapping: PB解析值或Open JavaInstant→原分组校验→Command micro
- Error and edge behavior: null缺失/invalid保持原约束路径
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep current record/class disposition and every existing component/annotation/group.
// Change startsAt and endsAt from LocalDateTime to java.time.Instant.
// Retain @NotNull in current Default/native RPC groups and positive ID/operator/key validation.
// Offset/Z parse happens in current native PB converter; Open carrier is Java Instant, no new transport.
// Current TeachingApplicationConvertor maps to ScheduleCourseCommand and micro canonicalization;
// no direct DTO -> PO bypass, no coercion through server default zone.
// JSON projections use existing Jackson Instant UTC Z; preserve other facade method signatures.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 12 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/po/ClassCourseSchedulePO.java`

- Purpose: 时间字段Instant；Command规范化；保留既有对象角色/Validation/PO注解
- Symbols: ClassCourseSchedulePO
- Repository evidence: 14: import java.time.LocalDateTime;；22: @TableName("light_class_course_schedules")
- Dependencies and consumers: TeachingApplicationConvertor/infraService/RepoDAO/原Mapper XML
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 两PO仅startsAt/endsAt Instant；既有builder/继承注解满足EgonModel @SuperBuilder
- Input/output and state mapping: BO/Command microInstant→PO→现有MyBatis binding→timestamptz(6)
- Error and edge behavior: null/tenant/version/软删除验证不旁路，无新typehandler
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)
@SuperBuilder @EqualsAndHashCode(callSuper = true)
@TableName(/* preserve current exact table mapping */)
public class ClassCourseSchedulePO extends EgonModel<ClassCourseSchedulePO> {
    // Keep all current business fields/column annotations, replace only these field types.
    private Instant startsAt;
    private Instant endsAt;
    // Inherit audit Instant and deletedAt LocalDateTime/version/tenant from EgonModel; do not redeclare.
    // SuperBuilder matches superclass; generated mapper and existing builders/constructors compile.
}
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 13 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: TeachingApplicationConvertor
- Repository evidence: 18: @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)；20: public interface TeachingApplicationConvertor extends BaseForwardConverter<Course, CourseResult> {
- Dependencies and consumers: SchoolClassManageImpl、Domain/infra Service/所有mapper测试
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原TeachingApplicationConvertor.toSchedule(ScheduleCourseCommand, Course)返回CourseScheduleBO；其它投影方法不变
- Input/output and state mapping: Controller/Facade按原构造器造Command；TeachingApplicationConvertor.toSchedule仅Command+Course→BO；infraService原builder造PO
- Error and edge behavior: null按原mapper策略与下一边界约束处理
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep existing MapStruct declaration, generated implementation and Egon BaseConverter/Forward contract.
// Controller/Facade retain direct ScheduleCourseCommand construction: Request/DTO Instant -> constructor -> micro Instant.
// CourseSchedule.java -> CourseScheduleBO imports and mapping return/parameter types in same package vos.
// Existing toSchedule(command, course): command micro startsAt/endsAt and course.code -> CourseScheduleBO.
// ClassCourseSchedulePO construction/readback stays in infrastructure SchoolClassDomainServiceImpl, not in this mapper.
// Preserve schoolClassId/courseId/operatorId/idempotencyKey mapping and null policy/technical field exclusions.
// Add no reflective BeanUtils, JSON roundtrip or extra forwarding mapper. Compile annotation processor output.
// Real mapper tests construct micro Command then assert toSchedule yields the same micro BO;
// test existing infra builder/readback separately. No invented mapper methods or time fields on scheduleCount responses.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 14 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregate.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassAggregate
- Repository evidence: 12: public final class SchoolClassAggregate {
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Replace CourseSchedule with same-package CourseScheduleBO in imports, collections and schedule API.
// For each existing schedule call, BO constructor guarantees micro Instant and strict start < end.
// Original overlap condition remains half-open: new.startsAt < old.endsAt && old.startsAt < new.endsAt.
// Keep aggregate identity/state/membership/CourseCode and original exception/event behavior.
// No new validation registry/pattern; direct immutable value object enforces the same domain invariant.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 15 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/validators/TeachingDomainValidator.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: TeachingDomainValidator
- Repository evidence: 16: public class TeachingDomainValidator extends BaseValidator {；25: throw new TeachingDomainException("CLASS_NOT_ACTIVE", "School class must be active");
- Dependencies and consumers: 对应Request/Application/Domain handoff调用方；TEST-001/002/005
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原validation方法类型跟随Instant/BO；校验分组/错误码保持
- Input/output and state mapping: 原carrier约束→micro窗口strictstart<end→Domain，仅合法继续
- Error and edge behavior: null、折叠、倒置拒绝；原ID/关联/group rules全保留
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep TeachingDomainValidator extends current BaseValidator / existing bean, @Slf4j, @RequiredArgsConstructor,
// explicit current @Component name and @Qualifier("egonColaValidationUtils") propagation.
// validateSchedule / current teaching method first validates existing Bean/groups and identifiers.
// Use startsAt/endsAt Instant (or micro locals for raw Request/DTO), no LocalDateTime/defaultZone conversion.
// Compare canonical truncatedTo(MICROS) endpoints with strict isBefore; reject collapsed/reversed windows
// using the current boundary IllegalArgumentException or TeachingDomainException INVALID_SCHEDULE.
// Request null rejects through @NotNull; application/domain handoff maintains original validation groups.
// Update CourseScheduleBO import/signature where consumed, and retain all other teaching rules unchanged.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 16 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/service/SchoolClassDomainService.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassDomainService
- Repository evidence: 14: /** Persistence-owning school-class domain service contract. */；15: public interface SchoolClassDomainService {
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Update CourseSchedule references/signatures/imports to CourseScheduleBO and window parameter types to Instant.
// Retain exact existing method names, repository ports, associated ID/value types and dependency direction.
// Calls carrying commands/BO are validated through original domain service/application handoff.
// No new Spring/MP/OpenAPI dependency in Domain and no new interface solely for forwarding time conversion.
// All implementations and caller compile together in owning Step; no published intermediate incompatible signature.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 17 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassDomainServiceImpl
- Repository evidence: 27: /** Business rules and orchestration for the school-class domain service. */；30: @Service("schoolClassDomainService")
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Preserve existing named Bean, @Slf4j, @RequiredArgsConstructor/Qualifier and domain-service port implementation.
// saveAggregate uses the existing ClassCourseSchedulePO.builder().startsAt(schedule.startsAt()).endsAt(schedule.endsAt());
// findAggregateById constructs new CourseScheduleBO(course.code(), po.getStartsAt(), po.getEndsAt()).
// Read PO through original Repository/DAO; canonical BO constructor handles precision, no Timestamp string/default zone.
// Existing guarded save/update, tenant/version/deleted_at/LOCAL boundaries and association failures remain.
// No new DDL execution or DataSource use; SQL schema change is the separate authorized source-template edit.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 18 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidator.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: TeachingApplicationValidator
- Repository evidence: 16: @Component("teachingApplicationValidator")；20: public class TeachingApplicationValidator extends BaseValidator {
- Dependencies and consumers: 对应Request/Application/Domain handoff调用方；TEST-001/002/005
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原validation方法类型跟随Instant/BO；校验分组/错误码保持
- Input/output and state mapping: 原carrier约束→micro窗口strictstart<end→Domain，仅合法继续
- Error and edge behavior: null、折叠、倒置拒绝；原ID/关联/group rules全保留
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep TeachingApplicationValidator extends current BaseValidator / existing bean, @Slf4j, @RequiredArgsConstructor,
// explicit current @Component name and @Qualifier("egonColaValidationUtils") propagation.
// validateSchedule / current teaching method first validates existing Bean/groups and identifiers.
// Use startsAt/endsAt Instant (or micro locals for raw Request/DTO), no LocalDateTime/defaultZone conversion.
// Compare canonical truncatedTo(MICROS) endpoints with strict isBefore; reject collapsed/reversed windows
// using the current boundary IllegalArgumentException or TeachingDomainException INVALID_SCHEDULE.
// Request null rejects through @NotNull; application/domain handoff maintains original validation groups.
// Update CourseScheduleBO import/signature where consumed, and retain all other teaching rules unchanged.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 19 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/validators/TeachingRequestValidator.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: TeachingRequestValidator
- Repository evidence: 13: @Component("teachingRequestValidator")；16: public class TeachingRequestValidator extends BaseValidator {
- Dependencies and consumers: 对应Request/Application/Domain handoff调用方；TEST-001/002/005
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原validation方法类型跟随Instant/BO；校验分组/错误码保持
- Input/output and state mapping: 原carrier约束→micro窗口strictstart<end→Domain，仅合法继续
- Error and edge behavior: null、折叠、倒置拒绝；原ID/关联/group rules全保留
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep TeachingRequestValidator extends current BaseValidator / existing bean, @Slf4j, @RequiredArgsConstructor,
// explicit current @Component name and @Qualifier("egonColaValidationUtils") propagation.
// validateSchedule / current teaching method first validates existing Bean/groups and identifiers.
// Use startsAt/endsAt Instant (or micro locals for raw Request/DTO), no LocalDateTime/defaultZone conversion.
// Compare canonical truncatedTo(MICROS) endpoints with strict isBefore; reject collapsed/reversed windows
// using the current boundary IllegalArgumentException or TeachingDomainException INVALID_SCHEDULE.
// Request null rejects through @NotNull; application/domain handoff maintains original validation groups.
// Update CourseScheduleBO import/signature where consumed, and retain all other teaching rules unchanged.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 20 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/impl/SchoolClassManageImpl.java`

- Purpose: 替换准确CourseScheduleBO引用/MapStruct输出，保持其余方法行为
- Symbols: SchoolClassManageImpl
- Repository evidence: 30: @Service("schoolClassManageImpl")；34: public class SchoolClassManageImpl implements SchoolClassManage {
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep @Service current name, @Slf4j, @RequiredArgsConstructor, existing @Qualifier fields/transaction.
// Original validate Command -> claim/check associations/load state -> domain schedule -> guarded repo/save/event sequence.
// Consume already micro Instant; construct renamed CourseScheduleBO through original converter/domain boundary.
// Preserve original duplicate, overlap, tenant/permission, operator and idempotency behavior and rollback.
// Response/view derives canonical domain/saved value; do not echo unpersisted raw nanos or invent retries.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 21 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/handler/GlobalExceptionHandler.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: GlobalExceptionHandler
- Repository evidence: 15: @RestControllerAdvice；17: public class GlobalExceptionHandler {
- Dependencies and consumers: 两ScheduleUtcContractTest、既有Global advice/API wrapper
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 仅排课Request startsAt/endsAt decode失败映射已有400 VALIDATION_ERROR
- Input/output and state mapping: 精确Request/field mapping cause→ResultRecord；无业务调用
- Error and edge behavior: 非匹配异常保持Spring原处理，不吞异常/全局改JSON格式失败
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Extend existing GlobalExceptionHandler narrowly for HttpMessageNotReadableException.
// Inspect Jackson cause / JsonMappingException Reference path, actual ScheduleCourseRequest type and
// startsAt or endsAt component. Do not reclassify unreadable errors from unrelated DTOs/endpoints.
// For matched schedule-time token/format failure return HTTP 400 ResultRecord.failure(
//     ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getStatus(), safe field message).
// Preserve current MethodArgumentNotValidException, ValidationException, IllegalArgumentException,
// TeachingUseCaseException/UserUseCaseException handlers and unrelated default Spring error handling.
// Existing @Slf4j; use a stable named advice if touched under Rule 4, no credential/raw body logging.
// For nonmatching HttpMessageNotReadableException delegate/rethrow to existing Spring handling;
// do not install a broad catch(Exception) or change public wrapper timestamp/payload/status semantics.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 22 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassController.java`

- Purpose: 规范时间验证、原安全/错误模型及受影响operation文档；格式异常仅限排课类型
- Symbols: SchoolClassController
- Repository evidence: 24: @RestController；28: public class SchoolClassController {
- Dependencies and consumers: 原Controller→Manage→DomainService→infraService→Repo/DAO链（各自责任不跨层）
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 原业务入口/状态与注解保持，时间签名/BO消费者跟随同Step契约；Controller同步精确OAS
- Input/output and state mapping: 现有入口/Command micro→BO/PO guarded保存→规范domain/原响应
- Error and edge behavior: 原错误/租户/事务/重叠/幂等序列保持；无time任务新重试
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
// Keep route, @Valid/@Validated, authenticated principal-derived IDs, operationId/security and response wrapper.
// @Schema for startsAt/endsAt is string/date-time with Z examples; @Operation description requires offset/Z
// and explicitly states canonical micros. Do not add preflight endpoint or request business-zone parameter.
// Keep field-write guard and TeachingRequestValidator, then direct new ScheduleCourseCommand(...) -> Manage.
// Existing adapter converter only projects SchoolClassResult -> SchoolClassDetailVO; no new mapper forwarding layer.
// Negative decode/validation never calls Manage; response retains scheduleCount without new time fields.
// Use actual OAS output tests rather than only annotation text; ResultRecord timestamp remains UnixMillis.
```

- Verification contribution: Step 4 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 23 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`

- Purpose: 原位改TIMESTAMP(6) WITH TIME ZONE；其他schema/index/lifecycle不变
- Symbols: light_class_course_schedules_0/1.starts_at/ends_at
- Repository evidence: 176: starts_at TIMESTAMP NOT NULL,；177: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: light_class_course_schedules_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for light_class_course_schedules_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 24 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 按新字节重算SHA-256；version/path/family不变
- Symbols: scripts[0].sha256
- Repository evidence: 7: "sha256": "3e10f8c487e692c38e81ac652d1f8cee7f5ca65f0552cea944a858bbb158b777"
- Dependencies and consumers: 原MP-SDJ manifest reader、新模板生成、scratch审计
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 只更新该源SQL entry checksum，实际key名称按当前manifest
- Input/output and state mapping: 刚修改SQL文件原始bytes→完整64hexSHA→现有matchingentry
- Error and edge behavior: path/version不匹配或摘要不同直接gate失败，不修复真实DB
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```json
{
  "existing manifest": "keep original structure, family, version, role, path, target declarations",
  "matching V20260913_001 entry": {
    "sha256": "MessageDigest/SHA-256 of the just-edited owning SQL raw bytes, all 64 lowercase hex digits",
    "other keys": "unchanged; no added version, no cross-project copied checksum"
  }
}
// Re-read this exact manifest JSON and owning source SQL; assert the matching path/version and digest.
// Preserve all unrelated entries and current formatting; no automatic repair/rebuild of applied ddl_history.
```

- Verification contribution: TEST-009四source entry raw-byte摘要精确一致
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 25 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassControllerTest.java`

- Purpose: 原MockitoBean caller与批准的显式Bean/Qualifier名称一致。
- Symbols: SchoolClassControllerTest.schoolClassManage / @MockitoBean
- Repository evidence: 旧web slice mock按默认field名，而真实@Service已有schoolClassManageImpl名称。
- Dependencies and consumers: 该Step SchoolClassController的Qualifier；已有WebMvcTest。
- Why now: Controller原行为可编译后，完整consumer测试不能因mock名遗漏失败。
- Contract/signature changes: 只明确test mock Bean名，生产接口与测试期望不改。
- Input/output and state mapping: mock field → schoolClassManageImpl → 原Controller构造注入。
- Error and edge behavior: 原response/context断言保留，不放宽校验或增加native安全。
- Standards impact: MC-BEAN-001, MC-SCOPE-001, MC-TEST-001；沿用既有MockitoBean。
- Literal rule enforcement: Rule 4/11原Bean名与原profile保持；无POJO/时间/JSON/依赖变更。
- Implementation pseudocode:

```java
@MockitoBean(name = "schoolClassManageImpl")
private SchoolClassManage schoolClassManage;
// Preserve existing @WebMvcTest/@ContextConfiguration and all create/get assertions.
// Named dependency satisfies approved @Qualifier without a fake production adapter/extra Bean.
```

- Verification contribution: SchoolClassControllerTest与本Step完整suite。
- After this file: 原web slice消费准确Bean名；重跑完整本Step GREEN。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-light-open
- Verification command:

```bash
./mvnw -o -Dtest=ScheduleUtcContractTest,ScheduleUtcConverterTest,TeachingApplicationValidatorTest,SchoolClassAggregateTest,SchoolClassManageTest,SchoolClassRepositoryImplTest,OpenArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result: 对应TEST全部GREEN；Step3/4所有BO/import/mapper编译成功；Step5四源列与摘要GREEN；Step6七套配置测试；Step7generate/check与回归无新增发现。可选PG若未授权运行记Not run/Skipped，不能算PASS。
- Failure returns to: 当前Step对应File（行为/映射/fixture）；声明坐标缺失或既有编译失败记录baseline；真实JDBC契约或行为设计冲突返回Spec，不新增依赖/handler或调整guard。
- Completion criteria: 该Step测试成功输出、相关静态门禁/完整diff与原guard保持证据齐全；没有误改其它路径。
- Rollback: 只撤销本人本Step的时间hunk或前向修正；保留之前GREEN与用户dirty；SQL为未部署源模板，无DBrollback/repair。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/pojo/convertor/ScheduleUtcConverterTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/ScheduleUtcContractTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/SchoolClassManageTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidatorTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregateTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/resources/mybatis/h2-schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/vos/CourseSchedule.java` → `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/vos/CourseScheduleBO.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/command/ScheduleCourseCommand.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/facade/teaching/dto/ScheduleCourseDTO.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/po/ClassCourseSchedulePO.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregate.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/validators/TeachingDomainValidator.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/service/SchoolClassDomainService.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidator.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/validators/TeachingRequestValidator.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/impl/SchoolClassManageImpl.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/handler/GlobalExceptionHandler.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassController.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/repository-manifest.json`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassControllerTest.java`
- Commit: 不单独提交（用户单任务一次commit）；Step7汇总 `fix(time): align archetype UTC instant contracts`。

### Step 5 — 统一 Service 与 Service-open 窗口微秒和 SQL

- Requirements: REQ-003, REQ-008, REQ-011
- Dependencies: Step 4验收完成；源码改动保留未提交
- Baseline state: Service/Open窗口原Instant但Command未统一micro、源窗口列naive。
- Observable outcome: 统一 Service 与 Service-open 窗口微秒和 SQL，对应§8合同可观察。
- End state: 本Step目标完整编译/GREEN；下一Step未完成项保持清晰，不把该Step验证宣称全任务运行通过。
- Test-first gate: Required — §4.2对应行为RED；先下列测试，后生产/接线；Step7复用已先建临时门禁的RED，不新改业务。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 本Step按§12.5逐项评审适用面；测试/配置/SQL文件的N/A理由在各块明确。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 非触达能力按§4.8边界证据保留，原unique-key例外不能扩大。
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/time/WindowUtcCommandTest.java`

- Purpose: 微秒归一化及零窗口拒绝入口回归
- Symbols: WindowUtcCommandTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: ScheduleCourseCommand/CreateExamCommand .123456789→.123456；submicro窗口折叠拒绝；null保持以原约束拒绝；+08/Z、DST两offset表示的同Instant一致
- Input/output and state mapping: 六字段Command原构造与真实Jakarta validator → ScheduleCourseCommand/CreateExamCommand .123456789→.123456；submicro窗口折叠拒绝；null保持以原约束拒绝；+08/Z、DST两offset表示的同Instant一致
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void windowCommandsTruncateBeforeValidation() {
    // Arrange: 六字段Command原构造与真实Jakarta validator.
    // Inputs / expected assertions: ScheduleCourseCommand/CreateExamCommand .123456789→.123456；submicro窗口折叠拒绝；null保持以原约束拒绝；+08/Z、DST两offset表示的同Instant一致.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=WindowUtcCommandTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 2 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/WindowUtcCommandTest.java`

- Purpose: 微秒归一化及零窗口拒绝入口回归
- Symbols: WindowUtcCommandTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: ScheduleCourseCommand/CreateExamCommand .123456789→.123456；submicro窗口折叠拒绝；null保持以原约束拒绝；+08/Z、DST两offset表示的同Instant一致
- Input/output and state mapping: 六字段Command原构造与真实Jakarta validator → ScheduleCourseCommand/CreateExamCommand .123456789→.123456；submicro窗口折叠拒绝；null保持以原约束拒绝；+08/Z、DST两offset表示的同Instant一致
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void windowCommandsTruncateBeforeValidation() {
    // Arrange: 六字段Command原构造与真实Jakarta validator.
    // Inputs / expected assertions: ScheduleCourseCommand/CreateExamCommand .123456789→.123456；submicro窗口折叠拒绝；null保持以原约束拒绝；+08/Z、DST两offset表示的同Instant一致.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=WindowUtcCommandTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 3 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/src/test/java/top/egon/cola/archetype/source/serviceopen/adapter/exam/facade/impl/ExamFacadeImplTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: ExamFacadeImplTest
- Repository evidence: 8: import java.time.Instant;；32: class ExamFacadeImplTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: offset/标准Timestamp仍原协议；createCommand微秒，响应来自规范domain/持久值；业务错误/metadata/关联/权限原合同保持
- Input/output and state mapping: 真实原PB mapper与Manage mock → offset/标准Timestamp仍原协议；createCommand微秒，响应来自规范domain/持久值；业务错误/metadata/关联/权限原合同保持
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void examFacadeDoesNotEchoUnstoredNanos() {
    // Arrange: 真实原PB mapper与Manage mock.
    // Inputs / expected assertions: offset/标准Timestamp仍原协议；createCommand微秒，响应来自规范domain/持久值；业务错误/metadata/关联/权限原合同保持.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ExamFacadeImplTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 4 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/course/CourseManageTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: CourseManageTest
- Repository evidence: 19: import java.time.Instant;；29: class CourseManageTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: ScheduleCourseCommand .123456789→.123456；collapse/overlap/ID/tenant业务错误保持；无额外save/MQ
- Input/output and state mapping: 原Manage/领域/Repository夹具 → ScheduleCourseCommand .123456789→.123456；collapse/overlap/ID/tenant业务错误保持；无额外save/MQ
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void courseManagePersistsCanonicalInstant() {
    // Arrange: 原Manage/领域/Repository夹具.
    // Inputs / expected assertions: ScheduleCourseCommand .123456789→.123456；collapse/overlap/ID/tenant业务错误保持；无额外save/MQ.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=CourseManageTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 5 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/exam/ExamManageTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: ExamManageTest
- Repository evidence: 22: import java.time.Instant;；31: class ExamManageTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: CreateExamCommand微秒；collapse拒绝、null拒绝；原Exam状态/关联/租户/查询类型保持
- Input/output and state mapping: 原Manage/Repository/validation夹具 → CreateExamCommand微秒；collapse拒绝、null拒绝；原Exam状态/关联/租户/查询类型保持
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void examManageValidatesCanonicalWindow() {
    // Arrange: 原Manage/Repository/validation夹具.
    // Inputs / expected assertions: CreateExamCommand微秒；collapse拒绝、null拒绝；原Exam状态/关联/租户/查询类型保持.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ExamManageTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 6 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/transaction/LocalTransactionBoundaryTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: LocalTransactionBoundaryTest
- Repository evidence: 9: class LocalTransactionBoundaryTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 仅更新时间断言/fixture；originalrollback/partial failure/单target约束保持；不把mock测试说成真实MQ原子性
- Input/output and state mapping: 原LOCAL事务边界测试夹具 → 仅更新时间断言/fixture；originalrollback/partial failure/单target约束保持；不把mock测试说成真实MQ原子性
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void canonicalTimeDoesNotAlterLocalRollback() {
    // Arrange: 原LOCAL事务边界测试夹具.
    // Inputs / expected assertions: 仅更新时间断言/fixture；originalrollback/partial failure/单target约束保持；不把mock测试说成真实MQ原子性.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=LocalTransactionBoundaryTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 7 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/src/test/java/top/egon/cola/archetype/source/serviceopen/facade/contract/ProtoDescriptorContractTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: ProtoDescriptorContractTest
- Repository evidence: 16: class ProtoDescriptorContractTest {；38: assertMessageField(CourseSchedule.getDescriptor(), "starts_at", "google.protobuf.Timestamp");
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: OpenService google.protobuf.Timestamp原消息/field IDs/presence/type与seconds/nanos保持；不改成string；Command后微秒与响应规范值分开断言
- Input/output and state mapping: 真实生成PB descriptor → OpenService google.protobuf.Timestamp原消息/field IDs/presence/type与seconds/nanos保持；不改成string；Command后微秒与响应规范值分开断言
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void timestampDescriptorsKeepNumbersAndNanos() {
    // Arrange: 真实生成PB descriptor.
    // Inputs / expected assertions: OpenService google.protobuf.Timestamp原消息/field IDs/presence/type与seconds/nanos保持；不改成string；Command后微秒与响应规范值分开断言.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ProtoDescriptorContractTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 8 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/architecture/ArchetypeContractConvergenceTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: ArchetypeContractConvergenceTest
- Repository evidence: 23: class ArchetypeContractConvergenceTest {；98: assertTrue(remote.contains("@Component(\"organizationDirectoryClient\")"),
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 新TIMESTAMP(6) WITH TIME ZONE窗口期望；其他tenant/version/deleted_at/事务/架构断言不削弱；原SQL版本保留
- Input/output and state mapping: 原源文本/architecture检查 → 新TIMESTAMP(6) WITH TIME ZONE窗口期望；其他tenant/version/deleted_at/事务/架构断言不削弱；原SQL版本保留
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void utcSourceSqlRetainsArchitectureContract() {
    // Arrange: 原源文本/architecture检查.
    // Inputs / expected assertions: 新TIMESTAMP(6) WITH TIME ZONE窗口期望；其他tenant/version/deleted_at/事务/架构断言不削弱；原SQL版本保留.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeContractConvergenceTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 9 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/resources/mybatis/h2-schema.sql`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: h2-schema
- Repository evidence: 31: starts_at TIMESTAMP NOT NULL,；32: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: evaluation_course_schedule_0/1 and evaluation_exam_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for evaluation_course_schedule_0/1 and evaluation_exam_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 10 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImplTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: ExamFacadeImplTest
- Repository evidence: 14: import java.time.Instant;；21: class ExamFacadeImplTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: offset/标准Timestamp仍原协议；createCommand微秒，响应来自规范domain/持久值；业务错误/metadata/关联/权限原合同保持
- Input/output and state mapping: 真实原PB mapper与Manage mock → offset/标准Timestamp仍原协议；createCommand微秒，响应来自规范domain/持久值；业务错误/metadata/关联/权限原合同保持
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void examFacadeDoesNotEchoUnstoredNanos() {
    // Arrange: 真实原PB mapper与Manage mock.
    // Inputs / expected assertions: offset/标准Timestamp仍原协议；createCommand微秒，响应来自规范domain/持久值；业务错误/metadata/关联/权限原合同保持.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ExamFacadeImplTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 11 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/course/CourseManageTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: CourseManageTest
- Repository evidence: 19: import java.time.Instant;；29: class CourseManageTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: ScheduleCourseCommand .123456789→.123456；collapse/overlap/ID/tenant业务错误保持；无额外save/MQ
- Input/output and state mapping: 原Manage/领域/Repository夹具 → ScheduleCourseCommand .123456789→.123456；collapse/overlap/ID/tenant业务错误保持；无额外save/MQ
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void courseManagePersistsCanonicalInstant() {
    // Arrange: 原Manage/领域/Repository夹具.
    // Inputs / expected assertions: ScheduleCourseCommand .123456789→.123456；collapse/overlap/ID/tenant业务错误保持；无额外save/MQ.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=CourseManageTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 12 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/exam/ExamManageTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: ExamManageTest
- Repository evidence: 22: import java.time.Instant;；31: class ExamManageTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: CreateExamCommand微秒；collapse拒绝、null拒绝；原Exam状态/关联/租户/查询类型保持
- Input/output and state mapping: 原Manage/Repository/validation夹具 → CreateExamCommand微秒；collapse拒绝、null拒绝；原Exam状态/关联/租户/查询类型保持
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void examManageValidatesCanonicalWindow() {
    // Arrange: 原Manage/Repository/validation夹具.
    // Inputs / expected assertions: CreateExamCommand微秒；collapse拒绝、null拒绝；原Exam状态/关联/租户/查询类型保持.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ExamManageTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 13 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/transaction/LocalTransactionBoundaryTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: LocalTransactionBoundaryTest
- Repository evidence: 9: class LocalTransactionBoundaryTest {
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 仅更新时间断言/fixture；originalrollback/partial failure/单target约束保持；不把mock测试说成真实MQ原子性
- Input/output and state mapping: 原LOCAL事务边界测试夹具 → 仅更新时间断言/fixture；originalrollback/partial failure/单target约束保持；不把mock测试说成真实MQ原子性
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void canonicalTimeDoesNotAlterLocalRollback() {
    // Arrange: 原LOCAL事务边界测试夹具.
    // Inputs / expected assertions: 仅更新时间断言/fixture；originalrollback/partial failure/单target约束保持；不把mock测试说成真实MQ原子性.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=LocalTransactionBoundaryTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 14 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/architecture/ArchetypeContractConvergenceTest.java`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: ArchetypeContractConvergenceTest
- Repository evidence: 23: class ArchetypeContractConvergenceTest {；345: assertTrue(message.contains("Instant startsAt"), "time values stay on java.time");
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 新TIMESTAMP(6) WITH TIME ZONE窗口期望；其他tenant/version/deleted_at/事务/架构断言不削弱；原SQL版本保留
- Input/output and state mapping: 原源文本/architecture检查 → 新TIMESTAMP(6) WITH TIME ZONE窗口期望；其他tenant/version/deleted_at/事务/架构断言不削弱；原SQL版本保留
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void utcSourceSqlRetainsArchitectureContract() {
    // Arrange: 原源文本/architecture检查.
    // Inputs / expected assertions: 新TIMESTAMP(6) WITH TIME ZONE窗口期望；其他tenant/version/deleted_at/事务/架构断言不削弱；原SQL版本保留.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeContractConvergenceTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 15 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/resources/mybatis/h2-schema.sql`

- Purpose: 时间fixture/精度与既有RPC合同回归
- Symbols: h2-schema
- Repository evidence: 20: starts_at TIMESTAMP NOT NULL,；21: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 先固定RED预期；测试夹具准备优先于调用方。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: evaluation_course_schedule_0/1 and evaluation_exam_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for evaluation_course_schedule_0/1 and evaluation_exam_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 16 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/course/pojo/command/ScheduleCourseCommand.java`

- Purpose: 保留Instant/原字段；compact constructor截断微秒、保留null给Validation
- Symbols: ScheduleCourseCommand
- Repository evidence: 5: import java.time.Instant;；7: public record ScheduleCourseCommand(
- Dependencies and consumers: 既有Adapter/FacadeConvertor→Manage/Validator→Domain/PO及响应规范值
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 所有非时间字段/现有约束/构造检查保持；Light两Command字段LocalDateTime→Instant，Service四Command原Instant不变
- Input/output and state mapping: 非nullInstant→truncatedTo(MICROS)；null留给原Jakarta约束；其他字段逐原mapper
- Error and edge behavior: micro后start≥end由既有业务/validator拒绝，不用DBround制造成功
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record ScheduleCourseCommand(/* retain exact existing components; */ Instant startsAt, Instant endsAt /* and other existing fields */) {
    public ScheduleCourseCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
        // Preserve current constructor ID/association guards and carrier annotations; no new broad business rule here.
        // Original Default/native/model annotations and validators reject null, start >= end AFTER canonicalization.
        // Light changes LocalDateTime -> Instant; Service/Open already Instant, only canonicalize.
    }
}
// Every HTTP/RPC/direct construction reaches the same canonical carrier; no server-zone or +8h conversion.
```

- Verification contribution: Step 5 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 17 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/exam/pojo/command/CreateExamCommand.java`

- Purpose: 保留Instant/原字段；compact constructor截断微秒、保留null给Validation
- Symbols: CreateExamCommand
- Repository evidence: 6: import java.time.Instant;；8: public record CreateExamCommand(
- Dependencies and consumers: 既有Adapter/FacadeConvertor→Manage/Validator→Domain/PO及响应规范值
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 所有非时间字段/现有约束/构造检查保持；Light两Command字段LocalDateTime→Instant，Service四Command原Instant不变
- Input/output and state mapping: 非nullInstant→truncatedTo(MICROS)；null留给原Jakarta约束；其他字段逐原mapper
- Error and edge behavior: micro后start≥end由既有业务/validator拒绝，不用DBround制造成功
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record CreateExamCommand(/* retain exact existing components; */ Instant startsAt, Instant endsAt /* and other existing fields */) {
    public CreateExamCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
        // Preserve current constructor ID/association guards and carrier annotations; no new broad business rule here.
        // Original Default/native/model annotations and validators reject null, start >= end AFTER canonicalization.
        // Light changes LocalDateTime -> Instant; Service/Open already Instant, only canonicalize.
    }
}
// Every HTTP/RPC/direct construction reaches the same canonical carrier; no server-zone or +8h conversion.
```

- Verification contribution: Step 5 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 18 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/course/pojo/command/ScheduleCourseCommand.java`

- Purpose: 保留Instant/原字段；compact constructor截断微秒、保留null给Validation
- Symbols: ScheduleCourseCommand
- Repository evidence: 5: import java.time.Instant;；7: public record ScheduleCourseCommand(
- Dependencies and consumers: 既有Adapter/FacadeConvertor→Manage/Validator→Domain/PO及响应规范值
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 所有非时间字段/现有约束/构造检查保持；Light两Command字段LocalDateTime→Instant，Service四Command原Instant不变
- Input/output and state mapping: 非nullInstant→truncatedTo(MICROS)；null留给原Jakarta约束；其他字段逐原mapper
- Error and edge behavior: micro后start≥end由既有业务/validator拒绝，不用DBround制造成功
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record ScheduleCourseCommand(/* retain exact existing components; */ Instant startsAt, Instant endsAt /* and other existing fields */) {
    public ScheduleCourseCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
        // Preserve current constructor ID/association guards and carrier annotations; no new broad business rule here.
        // Original Default/native/model annotations and validators reject null, start >= end AFTER canonicalization.
        // Light changes LocalDateTime -> Instant; Service/Open already Instant, only canonicalize.
    }
}
// Every HTTP/RPC/direct construction reaches the same canonical carrier; no server-zone or +8h conversion.
```

- Verification contribution: Step 5 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 19 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/exam/pojo/command/CreateExamCommand.java`

- Purpose: 保留Instant/原字段；compact constructor截断微秒、保留null给Validation
- Symbols: CreateExamCommand
- Repository evidence: 6: import java.time.Instant;；8: public record CreateExamCommand(
- Dependencies and consumers: 既有Adapter/FacadeConvertor→Manage/Validator→Domain/PO及响应规范值
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 所有非时间字段/现有约束/构造检查保持；Light两Command字段LocalDateTime→Instant，Service四Command原Instant不变
- Input/output and state mapping: 非nullInstant→truncatedTo(MICROS)；null留给原Jakarta约束；其他字段逐原mapper
- Error and edge behavior: micro后start≥end由既有业务/validator拒绝，不用DBround制造成功
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
public record CreateExamCommand(/* retain exact existing components; */ Instant startsAt, Instant endsAt /* and other existing fields */) {
    public CreateExamCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
        // Preserve current constructor ID/association guards and carrier annotations; no new broad business rule here.
        // Original Default/native/model annotations and validators reject null, start >= end AFTER canonicalization.
        // Light changes LocalDateTime -> Instant; Service/Open already Instant, only canonicalize.
    }
}
// Every HTTP/RPC/direct construction reaches the same canonical carrier; no server-zone or +8h conversion.
```

- Verification contribution: Step 5 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 20 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`

- Purpose: 只改starts_at/ends_at到aware(6)，其余索引、约束和表保留
- Symbols: evaluation_course_schedule_0/1、evaluation_exam_0/1
- Repository evidence: 39: starts_at TIMESTAMP NOT NULL,；40: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: evaluation_course_schedule_0/1 and evaluation_exam_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for evaluation_course_schedule_0/1 and evaluation_exam_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 21 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`

- Purpose: 只改starts_at/ends_at到aware(6)，其余索引、约束和表保留
- Symbols: evaluation_course_schedule_0/1、evaluation_exam_0/1
- Repository evidence: 29: starts_at TIMESTAMP NOT NULL,；30: ends_at TIMESTAMP NOT NULL,
- Dependencies and consumers: 现有MP-SDJ manifest/新生成项目；H2为对应测试Repo夹具
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 源初始化脚本仅窗口类型；H2 fixture与PO匹配，无PG证据替代
- Input/output and state mapping: evaluation_course_schedule_0/1 and evaluation_exam_0/1 starts_at/ends_at→aware(6)；所有其他列/index保持
- Error and edge behavior: 仅四源SQL获明确例外；不碰已有真实history；H2不证明PGdriver
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```sql
-- In the existing CREATE TABLE bodies for evaluation_course_schedule_0/1 and evaluation_exam_0/1:
-- replace only starts_at / ends_at column declarations with TIMESTAMP(6) WITH TIME ZONE; keep NULL/default clauses unchanged.
-- No ALTER journal/new version/backfill: user-authorized undeployed archetype source exception.
-- Keep create_time/update_time aware, deleted_at TIMESTAMP(6) WITHOUT TIME ZONE nullable, version defaults.
-- Preserve every table name, role/schema, tenant/id/business columns, unique/index/FK definition.
-- For H2 fixture use supported TIMESTAMP(6) WITH TIME ZONE syntax only; H2 is a semantic unit fixture.
-- After production SQL edits compute SHA-256 from actual bytes and update only owning manifest entry.
-- Never execute this source SQL against real data/history or modify other db scripts.
```

- Verification contribution: TEST-009 scratch AST/bytes/24columns gate；对应H2 tests；PG单独TEST-004
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 22 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 只更新受修改script的SHA，version不增
- Symbols: scripts[0].sha256
- Repository evidence: 7: "sha256": "fc9c2a35e556a73649bfafc336d5953b7142b43f57203c81c42602738c797d4b"
- Dependencies and consumers: 原MP-SDJ manifest reader、新模板生成、scratch审计
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 只更新该源SQL entry checksum，实际key名称按当前manifest
- Input/output and state mapping: 刚修改SQL文件原始bytes→完整64hexSHA→现有matchingentry
- Error and edge behavior: path/version不匹配或摘要不同直接gate失败，不修复真实DB
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```json
{
  "existing manifest": "keep original structure, family, version, role, path, target declarations",
  "matching V20260913_001 entry": {
    "sha256": "MessageDigest/SHA-256 of the just-edited owning SQL raw bytes, all 64 lowercase hex digits",
    "other keys": "unchanged; no added version, no cross-project copied checksum"
  }
}
// Re-read this exact manifest JSON and owning source SQL; assert the matching path/version and digest.
// Preserve all unrelated entries and current formatting; no automatic repair/rebuild of applied ddl_history.
```

- Verification contribution: TEST-009四source entry raw-byte摘要精确一致
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 23 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 只更新受修改script的SHA，version不增
- Symbols: scripts[0].sha256
- Repository evidence: 7: "sha256": "384296c05cfc5e1f590e60107916c4dd2233796bdb277831586e93c4c61f191b"
- Dependencies and consumers: 原MP-SDJ manifest reader、新模板生成、scratch审计
- Why now: 前置测试已规定行为；此顺序按类型→消费者→SQL→manifest推进。
- Contract/signature changes: 只更新该源SQL entry checksum，实际key名称按当前manifest
- Input/output and state mapping: 刚修改SQL文件原始bytes→完整64hexSHA→现有matchingentry
- Error and edge behavior: path/version不匹配或摘要不同直接gate失败，不修复真实DB
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```json
{
  "existing manifest": "keep original structure, family, version, role, path, target declarations",
  "matching V20260913_001 entry": {
    "sha256": "MessageDigest/SHA-256 of the just-edited owning SQL raw bytes, all 64 lowercase hex digits",
    "other keys": "unchanged; no added version, no cross-project copied checksum"
  }
}
// Re-read this exact manifest JSON and owning source SQL; assert the matching path/version and digest.
// Preserve all unrelated entries and current formatting; no automatic repair/rebuild of applied ddl_history.
```

- Verification contribution: TEST-009四source entry raw-byte摘要精确一致
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 24 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/package-info.java`

- Purpose: 补本任务新增测试包的既有package-info文档。
- Symbols: package declaration and purpose comment
- Repository evidence: native Service/Web及Web-open verifier通用assertPackageDocs扫描所有有Java代码的测试包。
- Dependencies and consumers: source编译与生成样例文件检查，无运行时消费者。
- Why now: 真实archetype IT发现缺口，同类新增包必须一并满足现有规则。
- Contract/signature changes: 无API或行为变更，仅声明已有测试包用途。
- Input/output and state mapping: 新UTC测试包→同风格package-info。
- Error and edge behavior: 不跳过包或弱化原断言，不增加无关包。
- Standards impact: MC-ARCH-001、MC-SCOPE-001、MC-TEST-001；carrier/Bean/映射业务规则N/A。
- Literal rule enforcement: Rule 11: 保持原测试包树；Rule 1: 用途明确；其他规则无新增生产面。
- Implementation pseudocode:

```java
/** Offline UTC configuration or window command contract tests. */
// Use the exact package of this file's sibling UTC test; keep the nearby one-line comment style.
// No helper, Bean or runtime hook is introduced by a package-info declaration.
```

- Verification contribution: 重新source test compile，真实generated包文档检查和源码静态存在检查。
- After this file: 本任务新包具备既有强制文档。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-service；再 /Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-service-open
- Verification command:

```bash
# Native service root
./mvnw -o -Dtest=WindowUtcCommandTest,CourseManageTest,ExamManageTest,LocalTransactionBoundaryTest,ExamFacadeImplTest,OwnedFacadeContractTest,EvaluationPersistenceArchitectureTest,ArchetypeContractConvergenceTest -Dsurefire.failIfNoSpecifiedTests=false test
# Then run at source-service-open root
./mvnw -o -Dtest=WindowUtcCommandTest,CourseManageTest,ExamManageTest,LocalTransactionBoundaryTest,ExamFacadeImplTest,ProtoDescriptorContractTest,OpenArchitectureTest,ArchetypeContractConvergenceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result: 对应TEST全部GREEN；Step3/4所有BO/import/mapper编译成功；Step5四源列与摘要GREEN；Step6七套配置测试；Step7generate/check与回归无新增发现。可选PG若未授权运行记Not run/Skipped，不能算PASS。
- Failure returns to: 当前Step对应File（行为/映射/fixture）；声明坐标缺失或既有编译失败记录baseline；真实JDBC契约或行为设计冲突返回Spec，不新增依赖/handler或调整guard。
- Completion criteria: 该Step测试成功输出、相关静态门禁/完整diff与原guard保持证据齐全；没有误改其它路径。
- Rollback: 只撤销本人本Step的时间hunk或前向修正；保留之前GREEN与用户dirty；SQL为未部署源模板，无DBrollback/repair。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/package-info.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/time/WindowUtcCommandTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/WindowUtcCommandTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/src/test/java/top/egon/cola/archetype/source/serviceopen/adapter/exam/facade/impl/ExamFacadeImplTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/course/CourseManageTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/exam/ExamManageTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/transaction/LocalTransactionBoundaryTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/src/test/java/top/egon/cola/archetype/source/serviceopen/facade/contract/ProtoDescriptorContractTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/architecture/ArchetypeContractConvergenceTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/resources/mybatis/h2-schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImplTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/course/CourseManageTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/exam/ExamManageTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/transaction/LocalTransactionBoundaryTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/architecture/ArchetypeContractConvergenceTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/resources/mybatis/h2-schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/course/pojo/command/ScheduleCourseCommand.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/exam/pojo/command/CreateExamCommand.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/course/pojo/command/ScheduleCourseCommand.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/exam/pojo/command/CreateExamCommand.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`
- Commit: 不单独提交（用户单任务一次commit）；Step7汇总 `fix(time): align archetype UTC instant contracts`。

### Step 6 — 统一七套传输/启动默认值与双语说明

- Requirements: REQ-007
- Dependencies: Step 5验收完成；源码改动保留未提交
- Baseline state: 六family Jackson/容器Asia/Shanghai；Agent缺统一传输key；七launch未统一UTC。
- Observable outcome: 统一七套传输/启动默认值与双语说明，对应§8合同可观察。
- End state: 本Step目标完整编译/GREEN；下一Step未完成项保持清晰，不把该Step验证宣称全任务运行通过。
- Test-first gate: Required — §4.2对应行为RED；先下列测试，后生产/接线；Step7复用已先建临时门禁的RED，不新改业务。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 本Step按§12.5逐项评审适用面；测试/配置/SQL文件的N/A理由在各块明确。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 非触达能力按§4.8边界证据保留，原unique-key例外不能扩大。
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/ArchetypeUtcConfigurationTest.java`

- Purpose: 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试
- Symbols: ArchetypeUtcConfigurationTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Input/output and state mapping: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker → 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void allProfilesAndLaunchDefaultsUseUtc() {
    // Arrange: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker.
    // Inputs / expected assertions: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeUtcConfigurationTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 2 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-dev.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 21: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 3 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-prod.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 21: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 4 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-test.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 24: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 5 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 36: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 6 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/launch/launch.args`

- Purpose: 默认UTC，保留其余占位/flags
- Symbols: JVM -Duser.timezone
- Repository evidence: 当前文件存在，SHA256 bdda3cf830c6fd34a0c9035fb21ec2c9d49cb2d5f81516a490b7aabb457f3de0；实际声明 JVM -Duser.timezone
- Dependencies and consumers: 当前jar launcher；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 既有JVM args默认UTC；不改其他flags/占位
- Input/output and state mapping: launcher→JVM user.timezone=UTC
- Error and edge behavior: 可显式部署覆盖按原launcher机制；不启动runtime
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In existing JVM argument section before the main JAR, add/replace exactly once:
-Duser.timezone=UTC
# Preserve existing environment placeholders, quoting, module flags, classpath and launcher format.
# No app argument emulating timezone after -jar; no APP_TIME_ZONE fallback and no service startup.
# Keep host custom Clock override and explicit business zones independent from this default.
# Existing source launch tests/ArchetypeUtcConfigurationTest assert one UTC property and no duplicate conflicting zone.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 7 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/README.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 48: command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not；93: / `PENDING` / claimed by a delivery / `PROCESSING` / a pending record exists /
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: English; edit only existing time/input/deploy sections and matching examples.
Explain: 已有发生时刻继续Instant，纯日期/本地时间保持语义; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 8 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/README.zh-CN.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 148: 知识库查询统一为绑定参数的 Mapper XML。更新同时限制租户、活动状态、期望版本与入库状态；`deleted_at` 使用可空 UTC LocalDateTime，`version` 从 0 递增。JSONB 保留字段专用 handler。
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: 中文; edit only existing time/input/deploy sections and matching examples.
Explain: 已有发生时刻继续Instant，纯日期/本地时间保持语义; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 9 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ArchetypeUtcConfigurationTest.java`

- Purpose: 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试
- Symbols: ArchetypeUtcConfigurationTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Input/output and state mapping: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker → 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void allProfilesAndLaunchDefaultsUseUtc() {
    // Arrange: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker.
    // Inputs / expected assertions: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeUtcConfigurationTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 10 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 164: max-clock-backward: PT0.005S；244: clock-skew: PT30S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 11 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 169: max-clock-backward: PT0.005S；249: clock-skew: PT30S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 12 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 171: max-clock-backward: PT0.005S；251: clock-skew: PT30S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 13 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 71: date-format: yyyy-MM-dd HH:mm:ss；72: time-zone: ${APP_TIME_ZONE:Asia/Shanghai}
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 14 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/launch/launch.args`

- Purpose: 默认UTC，保留其余占位/flags
- Symbols: JVM -Duser.timezone
- Repository evidence: 当前文件存在，SHA256 bdda3cf830c6fd34a0c9035fb21ec2c9d49cb2d5f81516a490b7aabb457f3de0；实际声明 JVM -Duser.timezone
- Dependencies and consumers: 当前jar launcher；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 既有JVM args默认UTC；不改其他flags/占位
- Input/output and state mapping: launcher→JVM user.timezone=UTC
- Error and edge behavior: 可显式部署覆盖按原launcher机制；不启动runtime
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In existing JVM argument section before the main JAR, add/replace exactly once:
-Duser.timezone=UTC
# Preserve existing environment placeholders, quoting, module flags, classpath and launcher format.
# No app argument emulating timezone after -jar; no APP_TIME_ZONE fallback and no service startup.
# Keep host custom Clock override and explicit business zones independent from this default.
# Existing source launch tests/ArchetypeUtcConfigurationTest assert one UTC property and no duplicate conflicting zone.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 15 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/Dockerfile`

- Purpose: UTC默认，保持engine/安全/内存/镜像过程
- Symbols: TZ / default JAVA_OPTS
- Repository evidence: 26: ENV TZ=Asia/Shanghai \
- Dependencies and consumers: 现有deployment image users；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 六套现有TZ及default JVM time UTC；无新增镜像
- Input/output and state mapping: ENV/defaultflags→进程默认UTC；其他镜像行为不变
- Error and edge behavior: 不运行容器，preserve显式覆盖机制
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In the current image/runtime stage and existing default JAVA_OPTS/JVM argument mechanism:
ENV TZ=UTC
# Retain all current JVM options and append a single -Duser.timezone=UTC to the default options.
# Preserve base image, user/permissions, ports, build stages, JAR path and entrypoint interpolation.
# Six existing Dockerfiles only; Agent has no Dockerfile and receives no invented image.
# Do not run docker build/up here; static configuration test observes both TZ and effective default flags.
# Runtime zone defaults do not replace Instant/timestamptz mapping or alter deletedAt UTC legacy contract.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 16 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/README.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 39: command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not；96: `egon-cola-component-bytecode-architecture-maven-plugin` enforces the graph. It is bound to the `verify` phase, maps every `top.egon.cola.archetype.source.light.<layer>` package to its layer, and runs with `unknownLayerPolicy=FAIL`, so a class that lands outside the mapped layers breaks the build instead of being skipped with a warning. Technology-first roots such as `adapter.controller` or `infrastructure.repo` are rejected because they resolve to no layer. The check runs at `verify`; `./mvnw test` alone never reaches it.
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: English; edit only existing time/input/deploy sections and matching examples.
Explain: 带Z或±HH:MM输入，HTTP严格字符串/拒绝naive/number、0–9位输入、Command微秒截断后比较与保存; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 17 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 99: - 转换：DTO/Command/Domain/PO 映射由 MapStruct（`@Mapper`）生成并继承 `BaseConverter` 或 `BaseForwardConverter`；只做快照投影的转换器不补反向方法。；205: 本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: 中文; edit only existing time/input/deploy sections and matching examples.
Explain: 带Z或±HH:MM输入，HTTP严格字符串/拒绝naive/number、0–9位输入、Command微秒截断后比较与保存; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 18 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ArchetypeUtcConfigurationTest.java`

- Purpose: 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试
- Symbols: ArchetypeUtcConfigurationTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Input/output and state mapping: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker → 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void allProfilesAndLaunchDefaultsUseUtc() {
    // Arrange: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker.
    // Inputs / expected assertions: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeUtcConfigurationTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 19 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-dev.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 31: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 20 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-prod.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 36: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 21 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-test.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 122: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 22 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 38: date-format: yyyy-MM-dd HH:mm:ss；39: time-zone: ${APP_TIME_ZONE:Asia/Shanghai}
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 23 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/launch/launch.args`

- Purpose: 默认UTC，保留其余占位/flags
- Symbols: JVM -Duser.timezone
- Repository evidence: 当前文件存在，SHA256 bdda3cf830c6fd34a0c9035fb21ec2c9d49cb2d5f81516a490b7aabb457f3de0；实际声明 JVM -Duser.timezone
- Dependencies and consumers: 当前jar launcher；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 既有JVM args默认UTC；不改其他flags/占位
- Input/output and state mapping: launcher→JVM user.timezone=UTC
- Error and edge behavior: 可显式部署覆盖按原launcher机制；不启动runtime
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In existing JVM argument section before the main JAR, add/replace exactly once:
-Duser.timezone=UTC
# Preserve existing environment placeholders, quoting, module flags, classpath and launcher format.
# No app argument emulating timezone after -jar; no APP_TIME_ZONE fallback and no service startup.
# Keep host custom Clock override and explicit business zones independent from this default.
# Existing source launch tests/ArchetypeUtcConfigurationTest assert one UTC property and no duplicate conflicting zone.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 24 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/deploy/container/Dockerfile`

- Purpose: UTC默认，保持engine/安全/内存/镜像过程
- Symbols: TZ / default JAVA_OPTS
- Repository evidence: 26: ENV TZ=Asia/Shanghai \
- Dependencies and consumers: 现有deployment image users；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 六套现有TZ及default JVM time UTC；无新增镜像
- Input/output and state mapping: ENV/defaultflags→进程默认UTC；其他镜像行为不变
- Error and edge behavior: 不运行容器，preserve显式覆盖机制
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In the current image/runtime stage and existing default JAVA_OPTS/JVM argument mechanism:
ENV TZ=UTC
# Retain all current JVM options and append a single -Duser.timezone=UTC to the default options.
# Preserve base image, user/permissions, ports, build stages, JAR path and entrypoint interpolation.
# Six existing Dockerfiles only; Agent has no Dockerfile and receives no invented image.
# Do not run docker build/up here; static configuration test observes both TZ and effective default flags.
# Runtime zone defaults do not replace Instant/timestamptz mapping or alter deletedAt UTC legacy contract.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 25 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 39: command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not；109: - Conversion: DTO/Command/Domain/PO mappings are MapStruct (`@Mapper`) generated and extend `BaseConverter` or `BaseForwardConverter`; a forward-only snapshot projection never gets a reverse method.
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: English; edit only existing time/input/deploy sections and matching examples.
Explain: 带Z或±HH:MM输入，HTTP严格字符串/拒绝naive/number、0–9位输入、Command微秒截断后比较与保存; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 26 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.zh-CN.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 99: - 转换：DTO/Command/Domain/PO 映射由 MapStruct（`@Mapper`）生成并继承 `BaseConverter` 或 `BaseForwardConverter`；只做快照投影的转换器不补反向方法。；144: 本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: 中文; edit only existing time/input/deploy sections and matching examples.
Explain: 带Z或±HH:MM输入，HTTP严格字符串/拒绝naive/number、0–9位输入、Command微秒截断后比较与保存; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 27 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ArchetypeUtcConfigurationTest.java`

- Purpose: 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试
- Symbols: ArchetypeUtcConfigurationTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Input/output and state mapping: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker → 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void allProfilesAndLaunchDefaultsUseUtc() {
    // Arrange: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker.
    // Inputs / expected assertions: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeUtcConfigurationTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 28 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 29: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 29 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 34: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 30 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 29: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 31 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 39: date-format: yyyy-MM-dd HH:mm:ss；40: time-zone: ${APP_TIME_ZONE:Asia/Shanghai}
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 32 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/launch/launch.args`

- Purpose: 默认UTC，保留其余占位/flags
- Symbols: JVM -Duser.timezone
- Repository evidence: 当前文件存在，SHA256 bdda3cf830c6fd34a0c9035fb21ec2c9d49cb2d5f81516a490b7aabb457f3de0；实际声明 JVM -Duser.timezone
- Dependencies and consumers: 当前jar launcher；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 既有JVM args默认UTC；不改其他flags/占位
- Input/output and state mapping: launcher→JVM user.timezone=UTC
- Error and edge behavior: 可显式部署覆盖按原launcher机制；不启动runtime
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In existing JVM argument section before the main JAR, add/replace exactly once:
-Duser.timezone=UTC
# Preserve existing environment placeholders, quoting, module flags, classpath and launcher format.
# No app argument emulating timezone after -jar; no APP_TIME_ZONE fallback and no service startup.
# Keep host custom Clock override and explicit business zones independent from this default.
# Existing source launch tests/ArchetypeUtcConfigurationTest assert one UTC property and no duplicate conflicting zone.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 33 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/Dockerfile`

- Purpose: UTC默认，保持engine/安全/内存/镜像过程
- Symbols: TZ / default JAVA_OPTS
- Repository evidence: 26: ENV TZ=Asia/Shanghai \
- Dependencies and consumers: 现有deployment image users；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 六套现有TZ及default JVM time UTC；无新增镜像
- Input/output and state mapping: ENV/defaultflags→进程默认UTC；其他镜像行为不变
- Error and edge behavior: 不运行容器，preserve显式覆盖机制
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In the current image/runtime stage and existing default JAVA_OPTS/JVM argument mechanism:
ENV TZ=UTC
# Retain all current JVM options and append a single -Duser.timezone=UTC to the default options.
# Preserve base image, user/permissions, ports, build stages, JAR path and entrypoint interpolation.
# Six existing Dockerfiles only; Agent has no Dockerfile and receives no invented image.
# Do not run docker build/up here; static configuration test observes both TZ and effective default flags.
# Runtime zone defaults do not replace Instant/timestamptz mapping or alter deletedAt UTC legacy contract.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 34 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/README.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 39: command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not；78: - Course RPC creates a course with a unique normalized code, reads it, pages it, and schedules a class without overlapping time ranges.
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: English; edit only existing time/input/deploy sections and matching examples.
Explain: Service原Instant/标准Timestamp或native string载体保持，Command统一micro；PBseconds/nanos协议保留; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 35 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 163: 本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: 中文; edit only existing time/input/deploy sections and matching examples.
Explain: Service原Instant/标准Timestamp或native string载体保持，Command统一micro；PBseconds/nanos协议保留; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 36 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ArchetypeUtcConfigurationTest.java`

- Purpose: 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试
- Symbols: ArchetypeUtcConfigurationTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Input/output and state mapping: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker → 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void allProfilesAndLaunchDefaultsUseUtc() {
    // Arrange: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker.
    // Inputs / expected assertions: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeUtcConfigurationTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 37 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-dev.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 26: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 38 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-prod.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 31: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 39 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-test.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 113: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 40 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 38: date-format: yyyy-MM-dd HH:mm:ss；39: time-zone: ${APP_TIME_ZONE:Asia/Shanghai}
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 41 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/launch/launch.args`

- Purpose: 默认UTC，保留其余占位/flags
- Symbols: JVM -Duser.timezone
- Repository evidence: 当前文件存在，SHA256 bdda3cf830c6fd34a0c9035fb21ec2c9d49cb2d5f81516a490b7aabb457f3de0；实际声明 JVM -Duser.timezone
- Dependencies and consumers: 当前jar launcher；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 既有JVM args默认UTC；不改其他flags/占位
- Input/output and state mapping: launcher→JVM user.timezone=UTC
- Error and edge behavior: 可显式部署覆盖按原launcher机制；不启动runtime
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In existing JVM argument section before the main JAR, add/replace exactly once:
-Duser.timezone=UTC
# Preserve existing environment placeholders, quoting, module flags, classpath and launcher format.
# No app argument emulating timezone after -jar; no APP_TIME_ZONE fallback and no service startup.
# Keep host custom Clock override and explicit business zones independent from this default.
# Existing source launch tests/ArchetypeUtcConfigurationTest assert one UTC property and no duplicate conflicting zone.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 42 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/deploy/container/Dockerfile`

- Purpose: UTC默认，保持engine/安全/内存/镜像过程
- Symbols: TZ / default JAVA_OPTS
- Repository evidence: 26: ENV TZ=Asia/Shanghai \
- Dependencies and consumers: 现有deployment image users；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 六套现有TZ及default JVM time UTC；无新增镜像
- Input/output and state mapping: ENV/defaultflags→进程默认UTC；其他镜像行为不变
- Error and edge behavior: 不运行容器，preserve显式覆盖机制
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In the current image/runtime stage and existing default JAVA_OPTS/JVM argument mechanism:
ENV TZ=UTC
# Retain all current JVM options and append a single -Duser.timezone=UTC to the default options.
# Preserve base image, user/permissions, ports, build stages, JAR path and entrypoint interpolation.
# Six existing Dockerfiles only; Agent has no Dockerfile and receives no invented image.
# Do not run docker build/up here; static configuration test observes both TZ and effective default flags.
# Runtime zone defaults do not replace Instant/timestamptz mapping or alter deletedAt UTC legacy contract.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 43 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 97: command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not；106: This archetype uses MyBatis-Plus 3.5.16. Business service ports retain domain semantics; concrete repositories extend `EgonColaRepository` and mappers extend `EgonColaMapper`. Queries use explicit XML. `EgonModel` owns id, tenantId, creation/update actors and times, `LocalDateTime deletedAt` and `Long version`: NULL is active, deletion writes a UTC timestamp and increments the version. AR/QueryChain are disabled; technical filling is mandatory.
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: English; edit only existing time/input/deploy sections and matching examples.
Explain: Service原Instant/标准Timestamp或native string载体保持，Command统一micro；PBseconds/nanos协议保留; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 44 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.zh-CN.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 100: 本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: 中文; edit only existing time/input/deploy sections and matching examples.
Explain: Service原Instant/标准Timestamp或native string载体保持，Command统一micro；PBseconds/nanos协议保留; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 45 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ArchetypeUtcConfigurationTest.java`

- Purpose: 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试
- Symbols: ArchetypeUtcConfigurationTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Input/output and state mapping: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker → 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void allProfilesAndLaunchDefaultsUseUtc() {
    // Arrange: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker.
    // Inputs / expected assertions: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeUtcConfigurationTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 46 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 49: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 47 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 54: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 48 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 49: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 49 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 39: date-format: yyyy-MM-dd HH:mm:ss；40: time-zone: ${APP_TIME_ZONE:Asia/Shanghai}
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 50 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/launch/launch.args`

- Purpose: 默认UTC，保留其余占位/flags
- Symbols: JVM -Duser.timezone
- Repository evidence: 当前文件存在，SHA256 bdda3cf830c6fd34a0c9035fb21ec2c9d49cb2d5f81516a490b7aabb457f3de0；实际声明 JVM -Duser.timezone
- Dependencies and consumers: 当前jar launcher；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 既有JVM args默认UTC；不改其他flags/占位
- Input/output and state mapping: launcher→JVM user.timezone=UTC
- Error and edge behavior: 可显式部署覆盖按原launcher机制；不启动runtime
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In existing JVM argument section before the main JAR, add/replace exactly once:
-Duser.timezone=UTC
# Preserve existing environment placeholders, quoting, module flags, classpath and launcher format.
# No app argument emulating timezone after -jar; no APP_TIME_ZONE fallback and no service startup.
# Keep host custom Clock override and explicit business zones independent from this default.
# Existing source launch tests/ArchetypeUtcConfigurationTest assert one UTC property and no duplicate conflicting zone.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 51 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/Dockerfile`

- Purpose: UTC默认，保持engine/安全/内存/镜像过程
- Symbols: TZ / default JAVA_OPTS
- Repository evidence: 26: ENV TZ=Asia/Shanghai \
- Dependencies and consumers: 现有deployment image users；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 六套现有TZ及default JVM time UTC；无新增镜像
- Input/output and state mapping: ENV/defaultflags→进程默认UTC；其他镜像行为不变
- Error and edge behavior: 不运行容器，preserve显式覆盖机制
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In the current image/runtime stage and existing default JAVA_OPTS/JVM argument mechanism:
ENV TZ=UTC
# Retain all current JVM options and append a single -Duser.timezone=UTC to the default options.
# Preserve base image, user/permissions, ports, build stages, JAR path and entrypoint interpolation.
# Six existing Dockerfiles only; Agent has no Dockerfile and receives no invented image.
# Do not run docker build/up here; static configuration test observes both TZ and effective default flags.
# Runtime zone defaults do not replace Instant/timestamptz mapping or alter deletedAt UTC legacy contract.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 52 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/README.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 39: command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not；93: The complete `teaching` vertical creates and queries grades and school classes, caches reads, and publishes committed changes. User-to-class assignment is one Application transaction using both Domain repositories; `school_class_users` is the authoritative membership relationship.
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: English; edit only existing time/input/deploy sections and matching examples.
Explain: 已有发生时刻继续Instant，纯日期/本地时间保持语义; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 53 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 186: 本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: 中文; edit only existing time/input/deploy sections and matching examples.
Explain: 已有发生时刻继续Instant，纯日期/本地时间保持语义; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 54 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ArchetypeUtcConfigurationTest.java`

- Purpose: 读取四profile/JVM/Docker时间键及现有Jackson序列化，隔离配置测试
- Symbols: ArchetypeUtcConfigurationTest
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: 所属测试模块→实际生产入口与依赖；既有JUnit/Mockito/MockMvc/原fixture，无新依赖
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Input/output and state mapping: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker → 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改
- Error and edge behavior: 合法与负向断言同时覆盖；invalid无Manage/Repository写入；默认测试无基础设施启动
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001：按§4.7/§4.8及本文件精确变更执行，纯测试不套生产Bean注解。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 1/2/3/4/5/6/9：语义后缀、注解validation/原groups、不可变record或标准PO Lombok、原具名业务Bean/Qualifier/Slf4j、JDK工具、Boot Jackson与原Adapter/Facade；本文件不涉及项N/A，不能为了规则扩大修改。
- Implementation pseudocode:

```java
@Test void allProfilesAndLaunchDefaultsUseUtc() {
    // Arrange: 只读当前source的base/dev/test/prod YAML和launch.args；六套现有Dockerfile，Agent无Docker.
    // Inputs / expected assertions: 四profile均spring.jackson.time-zone=UTC、serialization.write-dates-as-timestamps=false；date-format/APP_TIME_ZONE fallback不存在；launch -Duser.timezone=UTC、六Docker TZ=UTC且default flags；实际Boot mapper Instant输出Z、LocalDate/LocalTime不变；原Clock Bean/其他profile键不改.
    // Invoke the actual public entry point / generated mapper described above, no fake serializer or source-only substitute.
    // Before production edit record behavior-specific RED; after the owning Step run the same selector for GREEN.
    // Re-run original permission/tenant/lifecycle assertions; a tooling/baseline failure is not expected RED.
}
```

- Verification contribution: `-Dtest=ArchetypeUtcConfigurationTest`（所属source reactor或组件，§8）；这些断言实现对应TEST合同
- After this file: 合同测试具备预期RED；先记录缺失行为，再继续该Step最小实现。

#### File 55 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-dev.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 48: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 56 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-prod.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 53: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 57 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-test.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 136: max-clock-backward: PT0.005S
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 58 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application.yml`

- Purpose: 固定UTC，date-format去除仅对六套已有key；write-dates-as-timestamps=false；各profile保持同结构
- Symbols: spring.jackson time keys
- Repository evidence: 38: date-format: yyyy-MM-dd HH:mm:ss；39: time-zone: ${APP_TIME_ZONE:Asia/Shanghai}
- Dependencies and consumers: 原Boot Jackson auto-config及七ArchetypeUtcConfigurationTest
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 四profile同time-key结构；UTC字符串化；六套已有date-format去除、Agent不编造旧key
- Input/output and state mapping: 配置keys→原Boot Jackson Instant Z；其他envkeys不变
- Error and edge behavior: 不引入全局按请求切zone或naive fallback
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```yaml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
# Apply this exact time-key structure to base/dev/test/prod; preserve every unrelated existing key/value.
# Remove existing date-format (six families) and APP_TIME_ZONE:Asia/Shanghai interpolation only for this contract.
# Agent currently lacks these keys: add under existing spring node, never duplicate YAML mappings.
# UTC transmission default does not choose a business/display zone or change PO/schema/Clock behavior.
```

- Verification contribution: TEST-014实际BootMapper+four-profilekey parity
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 59 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/launch/launch.args`

- Purpose: 默认UTC，保留其余占位/flags
- Symbols: JVM -Duser.timezone
- Repository evidence: 当前文件存在，SHA256 bdda3cf830c6fd34a0c9035fb21ec2c9d49cb2d5f81516a490b7aabb457f3de0；实际声明 JVM -Duser.timezone
- Dependencies and consumers: 当前jar launcher；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 既有JVM args默认UTC；不改其他flags/占位
- Input/output and state mapping: launcher→JVM user.timezone=UTC
- Error and edge behavior: 可显式部署覆盖按原launcher机制；不启动runtime
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In existing JVM argument section before the main JAR, add/replace exactly once:
-Duser.timezone=UTC
# Preserve existing environment placeholders, quoting, module flags, classpath and launcher format.
# No app argument emulating timezone after -jar; no APP_TIME_ZONE fallback and no service startup.
# Keep host custom Clock override and explicit business zones independent from this default.
# Existing source launch tests/ArchetypeUtcConfigurationTest assert one UTC property and no duplicate conflicting zone.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 60 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/deploy/container/Dockerfile`

- Purpose: UTC默认，保持engine/安全/内存/镜像过程
- Symbols: TZ / default JAVA_OPTS
- Repository evidence: 26: ENV TZ=Asia/Shanghai \
- Dependencies and consumers: 现有deployment image users；TEST-014
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 六套现有TZ及default JVM time UTC；无新增镜像
- Input/output and state mapping: ENV/defaultflags→进程默认UTC；其他镜像行为不变
- Error and edge behavior: 不运行容器，preserve显式覆盖机制
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
# In the current image/runtime stage and existing default JAVA_OPTS/JVM argument mechanism:
ENV TZ=UTC
# Retain all current JVM options and append a single -Duser.timezone=UTC to the default options.
# Preserve base image, user/permissions, ports, build stages, JAR path and entrypoint interpolation.
# Six existing Dockerfiles only; Agent has no Dockerfile and receives no invented image.
# Do not run docker build/up here; static configuration test observes both TZ and effective default flags.
# Runtime zone defaults do not replace Instant/timestamptz mapping or alter deletedAt UTC legacy contract.
```

- Verification contribution: Step 6 §8 TEST 对应断言；以真实调用/生成mapper检验，不以源码搜索替代行为。
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 61 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 157: command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not；166: This archetype uses MyBatis-Plus 3.5.16. Business service ports retain domain semantics; concrete repositories extend `EgonColaRepository` and mappers extend `EgonColaMapper`. Queries use explicit XML. `EgonModel` owns id, tenantId, creation/update actors and times, `LocalDateTime deletedAt` and `Long version`: NULL is active, deletion writes a UTC timestamp and increments the version. AR/QueryChain are disabled; technical filling is mandatory.
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: English; edit only existing time/input/deploy sections and matching examples.
Explain: 已有发生时刻继续Instant，纯日期/本地时间保持语义; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 62 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.zh-CN.md`

- Purpose: 同步中英文Instant/UTC、微秒、deletedAt兼容、未部署SQL例外
- Symbols: 时间/输入/部署段
- Repository evidence: 147: 本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。
- Dependencies and consumers: 模板维护者/新生成项目调用方；TEST-014+人工双语例子核对
- Why now: 该family先RED测试，再四profile、launch/容器，最后双语说明；不先用TZ掩盖类型错误。
- Contract/signature changes: 双语只同步时间/启动/适用协议说明，other docs保持
- Input/output and state mapping: 代码/API/配置/SQL已批准合同→对应语言说明/固定例子
- Error and edge behavior: Web/Agent没有窗口API则不编造此API；旧真实库明确另行方案
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```text
Document language: 中文; edit only existing time/input/deploy sections and matching examples.
Explain: 已有发生时刻继续Instant，纯日期/本地时间保持语义; all occurred-instant text outputs use UTC Z.
Document four profile Jackson UTC and launcher defaults; six existing containers UTC, Agent no container addition.
Explain audit Instant/Clock, deletedAt nullable LocalDateTime as Legacy UTC and original delete/restore semantics.
Only the four listed undeployed source initialization SQL files use the user-approved same-version exception;
no instruction to repair/backfill/reuse edited checksums against an already generated/deployed real database.
Keep both README languages semantically aligned; examples use e.g. 2026-10-13T06:07:08.123456Z / equivalent +08:00.
Do not imply live PG/MQ/provider/browser or full application boot was verified by offline checks.
```

- Verification contribution: TEST-001/003/006/014与双语文档合同review
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 63 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/package-info.java`

- Purpose: 为新增UTC配置测试包补充Agent现有package-info文档。
- Symbols: package top.egon.cola.archetype.source.agent.time
- Repository evidence: AgentSourceContractTest.documents_every_populated_java_package_and_keeps_profile_key_sets_equal检查src/test/java的每个Java包；邻包starter/package-info.java使用单行JavaDoc。
- Dependencies and consumers: 仅测试源码扫描与archetype资源生成，不新增运行时消费者。
- Why now: 完整源回归发现直接相关缺口；恢复既有包文档合同，不放宽测试。
- Contract/signature changes: 无API、模型、Bean或时间行为变更。
- Input/output and state mapping: 新time测试包→同风格package-info.java。
- Error and edge behavior: 保留扫描断言，不跳过该包。
- Standards impact: MC-ARCH-001/MC-SCOPE-001/MC-TEST-001；对象/业务注解和模式N/A，非业务包声明。
- Literal rule enforcement: Rule 11: 原测试包结构；其他Java carrier/Bean/validation/JSON规则N/A，未新增能力。
- Implementation pseudocode:

```java
/** Offline UTC profile, Jackson and launch contract tests. */
package top.egon.cola.archetype.source.agent.time;
// AgentSourceContractTest scans this populated test directory and requires this declaration.
// Keep this comment style identical to the existing starter/package-info.java.
```

- Verification contribution: AgentSourceContractTest与ArchetypeUtcConfigurationTest均成功，再跑Agent全source回归。
- After this file: 完成该Step新增测试包的既有文档守卫。

#### File 64 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/package-info.java`

- Purpose: 补本任务新增测试包的既有package-info文档。
- Symbols: package declaration and purpose comment
- Repository evidence: native Service/Web及Web-open verifier通用assertPackageDocs扫描所有有Java代码的测试包。
- Dependencies and consumers: source编译与生成样例文件检查，无运行时消费者。
- Why now: 真实archetype IT发现缺口，同类新增包必须一并满足现有规则。
- Contract/signature changes: 无API或行为变更，仅声明已有测试包用途。
- Input/output and state mapping: 新UTC测试包→同风格package-info。
- Error and edge behavior: 不跳过包或弱化原断言，不增加无关包。
- Standards impact: MC-ARCH-001、MC-SCOPE-001、MC-TEST-001；carrier/Bean/映射业务规则N/A。
- Literal rule enforcement: Rule 11: 保持原测试包树；Rule 1: 用途明确；其他规则无新增生产面。
- Implementation pseudocode:

```java
/** Offline UTC configuration or window command contract tests. */
// Use the exact package of this file's sibling UTC test; keep the nearby one-line comment style.
// No helper, Bean or runtime hook is introduced by a package-info declaration.
```

- Verification contribution: 重新source test compile，真实generated包文档检查和源码静态存在检查。
- After this file: 本任务新包具备既有强制文档。

#### File 65 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/package-info.java`

- Purpose: 补本任务新增测试包的既有package-info文档。
- Symbols: package declaration and purpose comment
- Repository evidence: native Service/Web及Web-open verifier通用assertPackageDocs扫描所有有Java代码的测试包。
- Dependencies and consumers: source编译与生成样例文件检查，无运行时消费者。
- Why now: 真实archetype IT发现缺口，同类新增包必须一并满足现有规则。
- Contract/signature changes: 无API或行为变更，仅声明已有测试包用途。
- Input/output and state mapping: 新UTC测试包→同风格package-info。
- Error and edge behavior: 不跳过包或弱化原断言，不增加无关包。
- Standards impact: MC-ARCH-001、MC-SCOPE-001、MC-TEST-001；carrier/Bean/映射业务规则N/A。
- Literal rule enforcement: Rule 11: 保持原测试包树；Rule 1: 用途明确；其他规则无新增生产面。
- Implementation pseudocode:

```java
/** Offline UTC configuration or window command contract tests. */
// Use the exact package of this file's sibling UTC test; keep the nearby one-line comment style.
// No helper, Bean or runtime hook is introduced by a package-info declaration.
```

- Verification contribution: 重新source test compile，真实generated包文档检查和源码静态存在检查。
- After this file: 本任务新包具备既有强制文档。

#### File 66 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/package-info.java`

- Purpose: 补本任务新增测试包的既有package-info文档。
- Symbols: package declaration and purpose comment
- Repository evidence: native Service/Web及Web-open verifier通用assertPackageDocs扫描所有有Java代码的测试包。
- Dependencies and consumers: source编译与生成样例文件检查，无运行时消费者。
- Why now: 真实archetype IT发现缺口，同类新增包必须一并满足现有规则。
- Contract/signature changes: 无API或行为变更，仅声明已有测试包用途。
- Input/output and state mapping: 新UTC测试包→同风格package-info。
- Error and edge behavior: 不跳过包或弱化原断言，不增加无关包。
- Standards impact: MC-ARCH-001、MC-SCOPE-001、MC-TEST-001；carrier/Bean/映射业务规则N/A。
- Literal rule enforcement: Rule 11: 保持原测试包树；Rule 1: 用途明确；其他规则无新增生产面。
- Implementation pseudocode:

```java
/** Offline UTC configuration or window command contract tests. */
// Use the exact package of this file's sibling UTC test; keep the nearby one-line comment style.
// No helper, Bean or runtime hook is introduced by a package-info declaration.
```

- Verification contribution: 重新source test compile，真实generated包文档检查和源码静态存在检查。
- After this file: 本任务新包具备既有强制文档。

- Validation working directory: §8列出的七source根，逐个执行
- Verification command:

```bash
./mvnw -o -Dtest=ArchetypeUtcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- Expected result: 对应TEST全部GREEN；Step3/4所有BO/import/mapper编译成功；Step5四源列与摘要GREEN；Step6七套配置测试；Step7generate/check与回归无新增发现。可选PG若未授权运行记Not run/Skipped，不能算PASS。
- Failure returns to: 当前Step对应File（行为/映射/fixture）；声明坐标缺失或既有编译失败记录baseline；真实JDBC契约或行为设计冲突返回Spec，不新增依赖/handler或调整guard。
- Completion criteria: 该Step测试成功输出、相关静态门禁/完整diff与原guard保持证据齐全；没有误改其它路径。
- Rollback: 只撤销本人本Step的时间hunk或前向修正；保留之前GREEN与用户dirty；SQL为未部署源模板，无DBrollback/repair。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/package-info.java`; `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/package-info.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/package-info.java`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/package-info.java`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/ArchetypeUtcConfigurationTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-dev.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-prod.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-test.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/launch/launch.args`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.md`; `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.zh-CN.md`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ArchetypeUtcConfigurationTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/launch/launch.args`; `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/Dockerfile`; `egon-cola-archetypes/source-projects/egon-cola-source-light/README.md`; `egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ArchetypeUtcConfigurationTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-dev.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-prod.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-test.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/launch/launch.args`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/deploy/container/Dockerfile`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.md`; `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.zh-CN.md`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ArchetypeUtcConfigurationTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/launch/launch.args`; `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/Dockerfile`; `egon-cola-archetypes/source-projects/egon-cola-source-service/README.md`; `egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ArchetypeUtcConfigurationTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-dev.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-prod.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-test.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/launch/launch.args`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/deploy/container/Dockerfile`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.md`; `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.zh-CN.md`; `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ArchetypeUtcConfigurationTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/launch/launch.args`; `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/Dockerfile`; `egon-cola-archetypes/source-projects/egon-cola-source-web/README.md`; `egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ArchetypeUtcConfigurationTest.java`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-dev.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-prod.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-test.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application.yml`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/launch/launch.args`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/deploy/container/Dockerfile`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.md`; `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.zh-CN.md`
- Commit: 不单独提交（用户单任务一次commit）；Step7汇总 `fix(time): align archetype UTC instant contracts`。

### Step 7 — 完成源、派生产物和可选 PG 验收门禁

- Requirements: REQ-003, REQ-007, REQ-008, REQ-009, REQ-010, REQ-012
- Dependencies: Step 6验收完成；源码改动保留未提交
- Baseline state: 源变更已逐Step GREEN，PG默认skip；派生产物还需generate/check、临时审计需删除。
- Observable outcome: 完成源、派生产物和可选 PG 验收门禁，对应§8合同可观察。
- End state: 本Step目标完整编译/GREEN；下一Step未完成项保持清晰，不把该Step验证宣称全任务运行通过。
- Test-first gate: Required — §4.2对应行为RED；先下列测试，后生产/接线；Step7复用已先建临时门禁的RED，不新改业务。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 本Step按§12.5逐项评审适用面；测试/配置/SQL文件的N/A理由在各块明确。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 非触达能力按§4.8边界证据保留，原unique-key例外不能扩大。
- Ordered files:

#### File 1 — `CREATE scripts/work/2026-10-13-16-21-utc-source-audit.sh`

- Purpose: 临时只读验源SQL、checksum、profile、改动范围与派生关系；验证后删除
- Symbols: source SQL/manifest/time contract scratch audit
- Repository evidence: 当前路径不存在；复用本模块已有JUnit测试/annotation/fixture；精确职责见主Spec §8/§14。
- Dependencies and consumers: Step3/4/5/6/7执行者；§8 TEST-009/013/014
- Why now: Step3 SQL前预建临时脚本记录RED；本Step汇总所有源与派生证据，最后删除。
- Contract/signature changes: 临时只读检查，无生产API；删除不提交
- Input/output and state mapping: 四SQL+JSON源bytes和profile/launch/Docker→确定性报告/exit1异常
- Error and edge behavior: 不执行DDL、不读取凭据、不放宽错SHA/错version；delete only own file
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-TIME-001；MC-CONFIG-001/MC-JSON-001（配置）或MC-MODEL-001（SQL）；非Java对象规范在此N/A，已有数据/架构守卫保留。
- Literal rule enforcement: Rule 11：原架构/MP/CQE/tenant/deletedAt守卫保持，用户排除既有unique-key差距；Rule 10：java.time/UTC/micro或Legacy UTC精确语义；Rule 7：四profile时间键一致（配置文件）；Rule 5：摘要/脚本只用现有JDK/标准库；其他Java注解/转换/业务模式在非Java文件N/A。
- Implementation pseudocode:

```bash
#!/usr/bin/env bash
set -euo pipefail
# Scratch, read-only against repository sources; never print environment/secrets or execute SQL.
# Python stdlib embedded: load four exact SQL/manifest paths from Plan §5, verify version token and raw-byte SHA256.
# Parse named CREATE TABLE bodies (or reuse existing parser), assert exactly 12 window table instances /24 aware(6) columns.
# Compare captured pre-change source bodies: only starts_at/ends_at type changes; indices/FKs/roles/deleted_at remain.
# Validate seven profile time key tuples, seven launch args, six Docker defaults; Agent Docker assertion N/A.
python3 - "$SOURCE_ROOT" <<'AUDIT_PSEUDOCODE'
# Pseudocode: SOURCE_ROOT comes from this script path; source_paths are exactly the four SQL paths in §5.
for sql_path in source_paths:
    sql_bytes = sql_path.read_bytes()
    manifest = json.loads((sql_path.parent / "repository-manifest.json").read_text())
    entry = next(item for item in manifest["scripts"] if item["version"] == "20260913_001")
    assert entry["path"] == "db/egon-mp/V20260913_001__initialize_repository_schema.sql"
    assert entry["sha256"] == hashlib.sha256(sql_bytes).hexdigest()
    # Parse only the §6.3 exact window tables; assert both columns TIMESTAMP(6) WITH TIME ZONE.
    # Compare each full body to baseline after normalizing only those 2 column type tokens.
assert total_window_tables == 12 and total_window_columns == 24 and distinct_table_names == 8
AUDIT_PSEUDOCODE
# Before Step3 SQL edit run expecting RED naive-column failure; after Step5/6 run GREEN.
# Step7 run generate/check and existing regression externally in documented order; audit resultant source/derived shape.
# Always delete this harness after final verification; not a durable tool or commit path; preserve failure report only.
```

- Verification contribution: TEST-009实际四源hash/SQL；TEST-013/014限定静态补充，不替代runtime
- After this file: 本文件目标明确；后续同Step消费者接线/编译尚需完成，按§8验收全Step。

#### File 2 — `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- Purpose: 同步native Light BO rename的现有生成样例校验路径。
- Symbols: teaching domain value-object expected path list
- Repository evidence: verify.groovy原列表包含vos/CourseSchedule；真实archetype IT报告Expected file src/main/java/it/pkg/domain/teaching/vos/CourseSchedule.java。
- Dependencies and consumers: generate_archetypes.sh从definitions复制curated verify.groovy到派生产品，再由Maven archetype IT执行。
- Why now: 真实生成消费端发现本次rename直接遗漏，必须修复后再次generate/check/IT。
- Contract/signature changes: 仅旧类文件路径→已批准CourseScheduleBO路径；不新增API/类型。
- Input/output and state mapping: source CourseScheduleBO.java→原强制存在断言，不保留废弃类以绕过校验。
- Error and edge behavior: 其他时间、架构、安全、历史SQL校验均原样保留。
- Standards impact: MC-ARCH-001、MC-NAME-001、MC-SCOPE-001、MC-TEST-001；非Java生产文件的Bean/转换注解N/A。
- Literal rule enforcement: Rule 1: 已批准BO语义名称；Rule 11: 原包/模块/校验器保留；其他规则无新增业务面。
- Implementation pseudocode:

```groovy
// Keep the existing teaching-domain inventory and expected-file helper unchanged.
// Replace exactly the single retired type literal in that list.
"vos/CourseScheduleBO",
// All other value objects, entity/service inventories and SQL archive assertions remain.
```

- Verification contribution: 先IT rename路径RED，再重建派生、check和真实IT；禁止用跳过该断言代替修复。
- After this file: 不再因本次rename而产生旧文件路径失败。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command:

```bash
bash scripts/work/2026-10-13-16-21-utc-source-audit.sh
./scripts/generate_archetypes.sh generate
./scripts/generate_archetypes.sh check
./scripts/regression/test-generate-archetypes.sh
```

- Expected result: 对应TEST全部GREEN；Step3/4所有BO/import/mapper编译成功；Step5四源列与摘要GREEN；Step6七套配置测试；Step7generate/check与回归无新增发现。可选PG若未授权运行记Not run/Skipped，不能算PASS。
- Failure returns to: 当前Step对应File（行为/映射/fixture）；声明坐标缺失或既有编译失败记录baseline；真实JDBC契约或行为设计冲突返回Spec，不新增依赖/handler或调整guard。
- Completion criteria: 该Step测试成功输出、相关静态门禁/完整diff与原guard保持证据齐全；没有误改其它路径。
- Rollback: 只撤销本人本Step的时间hunk或前向修正；保留之前GREEN与用户dirty；SQL为未部署源模板，无DBrollback/repair。
- Commit paths: `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`; `scripts/work/2026-10-13-16-21-utc-source-audit.sh` 是excluded scratch：必须删除，不stage/commit；本Step另含File2必要rename校验器修正，最终commit为前6Step精确源路径集合（RENAME含old+new）及 `docs/egon/plan/2026-10-13-16-21-archetype-utc-time-contract-implementation.md`、`docs/egon/spec/2026-10-13-14-07-archetype-utc-time-contract-amendment.md`；仅任务时间/文档hunk，排除既有dirty。
- Commit: 不单独提交（用户单任务一次commit）；Step7汇总 `fix(time): align archetype UTC instant contracts`。

## 8. Test, Validation, and Quality Gates

以下均为获批实施后的命令。本轮只执行文档/路径检查。`-o` 在声明依赖已缓存时使用；依赖缺失明确报告，不换版本。每 Step 第一批测试加入后先跑同一命令并记录预期 RED，之后再跑 GREEN；新方法重命名造成的短暂不可编译只作为明确类型合同RED，不发布/提交该中间状态。

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Step 1 RED → GREEN | §7该Step Validation working directory | §7该Step精确Verification command | 同步生成器 SQL → Java 时间矩阵 | 基线行为RED；完成后exit0且原guard断言保持 | Step 1首测试/对应生产File | REQ-005；默认无PG/应用/MQ/容器启动 |
| Step 2 RED → GREEN | §7该Step Validation working directory | §7该Step精确Verification command | 修正 Common MP 时间提案并锁定兼容边界 | 基线行为RED；完成后exit0且原guard断言保持 | Step 2首测试/对应生产File | REQ-003, REQ-004, REQ-006；默认无PG/应用/MQ/容器启动 |
| Step 3 RED → GREEN | §7该Step Validation working directory | §7该Step精确Verification command | 贯通 native Light 排课 UTC 契约 | 基线行为RED；完成后exit0且原guard断言保持 | Step 3首测试/对应生产File | REQ-001, REQ-002, REQ-003, REQ-008；默认无PG/应用/MQ/容器启动 |
| Step 4 RED → GREEN | §7该Step Validation working directory | §7该Step精确Verification command | 贯通 Light-open 排课 UTC 契约 | 基线行为RED；完成后exit0且原guard断言保持 | Step 4首测试/对应生产File | REQ-001, REQ-002, REQ-003, REQ-008；默认无PG/应用/MQ/容器启动 |
| Step 5 RED → GREEN | §7该Step Validation working directory | §7该Step精确Verification command | 统一 Service 与 Service-open 窗口微秒和 SQL | 基线行为RED；完成后exit0且原guard断言保持 | Step 5首测试/对应生产File | REQ-003, REQ-008；默认无PG/应用/MQ/容器启动 |
| Step 6 RED → GREEN | §7该Step Validation working directory | §7该Step精确Verification command | 统一七套传输/启动默认值与双语说明 | 基线行为RED；完成后exit0且原guard断言保持 | Step 6首测试/对应生产File | REQ-007；默认无PG/应用/MQ/容器启动 |

### 8.1 Source reactor 与 generated 合同

Step 6 对以下七根逐个运行 `./mvnw -o -Dtest=ArchetypeUtcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`，验证每套真正运行的测试数量大于零，不能以未命中测试的exit0冒充成功：

- `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-light`
- `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-light-open`
- `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-service`
- `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-service-open`
- `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-web`
- `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-web-open`
- `/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-agent`

配置测试使用已有 Spring Boot `YamlPropertySourceLoader` 读取四YAML的property keys和值，使用已有Boot测试Jackson实例验证Instant/LocalDate/LocalTime输出；不引入YAML解析依赖，不拉起完整应用上下文或外部服务。Service两reactor都运行Step5命令；native Service若无ProtoDescriptorContractTest，须由已有 OwnedFacadeContractTest/真实nativePB映射测试覆盖其原string描述，不能以 Surefire“无指定测试”视为PB覆盖。OpenService有ProtoDescriptorContractTest，其标准Timestamp保持。

Step 7 先对七 source 根逐个运行 `./mvnw -o test`（完整受影响源回归，不启动服务），再按下面根目录命令验派生与真实archetype实例；若source baseline编译/guard已有问题，记录确切路径/原因与新增发现为零的证据，不能宣称全绿。

```bash
# Working directory: /Users/mario/SelfProject/Egon-COLA
bash scripts/generate_archetypes.sh generate
bash scripts/generate_archetypes.sh check
bash scripts/regression/test-generate-archetypes.sh unit
bash scripts/regression/test-generate-archetypes.sh generation
./mvnw -o -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean integration-test
```

generate重建既有`.generated`的原子输出；check不得通过手改派生绕过。definitions/verify.groovy或旧legacy-SQL归档若为既有门禁失败，记录baseline，不自行新增归档/削弱verifier。本Plan没有发布/install到远程、外部consumer部署或真实RPC/DDC/IdP验收。Step1已用native LIGHT/WEB/SERVICE临时CREATE fixture生成与编译未来PO/CRUD，不对源定制样例apply，不给OPEN/AGENT编造生成能力。

### 8.2 SQL/checksum 与 scratch 生命周期

临时门禁完整路径 `scripts/work/2026-10-13-16-21-utc-source-audit.sh`（主Spec TEST-009必要验收）。先尝试原源architecture/ArchetypeContractConvergenceTest命令，再在Step3生产SQL前创建该脚本，保留四SQL/四manifest精确原byte副本在本次临时目录；不提交这些副本。原EgonColaDdlManifestFamilyTest不覆盖四源hash，因此必须补验证，不能只运行它。

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| SQL baseline RED | /Users/mario/SelfProject/Egon-COLA | bash scripts/work/2026-10-13-16-21-utc-source-audit.sh | 四SQL/manifest | naive窗口列断言失败；与checksum基线错误分开 | Step3 SQL前检查脚本 | REQ-003/008；只读、无DDL执行 |
| SQL GREEN | /Users/mario/SelfProject/Egon-COLA | bash scripts/work/2026-10-13-16-21-utc-source-audit.sh | 12表实例/24窗口列/8distinct names；四manifest | 全部窗口aware(6)、原role/path/version/deletedAt/index/FK保持；SHA=新bytes | Step3/4/5源SQL/manifest | TEST-009；不碰实际history |
| Final disposition | /Users/mario/SelfProject/Egon-COLA | rm -- scripts/work/2026-10-13-16-21-utc-source-audit.sh | 本任务自建临时脚本 | 最终不存在；报告保留，不stage/force-add临时脚本 | Step7 | REQ-009/010；明确delete，非持久工具 |

脚本配置只能内置§5四源精确路径与限定表名，不能递归修改db目录。还应验证七套UTC默认值与未预期的时间变更/依赖变更；真实行为以各单测为主。

### 8.3 PostgreSQL 显式集成边界

只有用户启动并授权隔离 PostgreSQL 后，复用已有 `EGON_MP_PG_MODEL_TEST=true`、`EGON_MP_PG_URL/USER/PASSWORD`；不得读取/打印真实配置凭据或把默认单测连向已部署库。新integration fixture仅创建UUID隔离schema，finally恢复本测试改过的JVM TimeZone并清理自己的schema。默认env未开启必须明确报告Skipped/Not run。

```bash
# Working directory: /Users/mario/SelfProject/Egon-COLA
# The user has already supplied/authorized the isolated PG fixture variables; do not echo them.
EGON_MP_PG_MODEL_TEST=true ./mvnw -o \
  -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am \
  -Dtest=EgonColaUtcTimePostgreSqlTest,EgonColaLogicDeletePostgreSqlTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

TEST-004/011须分别核验真实MP Instant绑定/读取、UTC与Asia/Shanghai JVM/session组合及微秒值相等；deletedAt LocalDateTime UTC token、NULL活动/tenant/version/重复软删除守卫不变。禁止只比较 JDBC Timestamp字符串。若实际驱动失败，回到Spec确认gap/方案，不能自行加TypeHandler或新依赖。无需应用启动、前端或真实MQ验证。

### 8.4 Test ID / owning Step map

| Test ID | Owning Steps | Exact test scope / assertions | Evidence boundary |
| --- | --- | --- | --- |
| TEST-001 | Step 3/4 | 两ScheduleUtcContractTest；Z、+08:00、naive、number/float/numeric string/array/object/boolean、空白、null/absent/无seconds/offset秒/leap-second、JSON格式错误；实际生成OAS；同一UTC值；invalid 400且Manage未调用；OAS time string/date-time/required/Z例子/operationId/原response shape | 原MockMvc/Jackson，fake已验证principal/原Opencontext |
| TEST-002 | Step 3/4 | CourseScheduleBO、Request/Application/Domain validator；相邻/重叠、倒置、null、direct domain构造、不可变字段；micro后strictstart<end；半开窗口不误判相邻；原错误码；每handoff验证 | 固定Instant与现有Validator/Default/native/model组 |
| TEST-003 | Step 3/4 | 两Light ScheduleUtcConverterTest及NativeFacade/provider mapper；PB fields3/4合法offset、缺失、naive、DTOnull；真实Mapper；Instant相同、输出Z；field number/type/presence不变；native INVALID_ARGUMENT / 原business false正确；OpenJava无编造transport | 生成PB descriptor、实际MapStruct、固定值 |
| TEST-004 | Step 2/7 | EgonColaUtcTimePostgreSqlTest；同一micro instant在不同JVM/session zone、不同连接写读；timestamptz读回同一Instant；不是Timestamp字符串相等；SQLNULL/column类型符合 | 用户已启动/授权隔离PG；MP真实handler |
| TEST-005 | Step 3/4/5 | 六个受影响Command与CourseScheduleBO；输入.123456789及结束在同一micro内；DST offset不同但utc相等；truncate到.123456，不round；micro后折叠窗口拒绝；DST无server-default解释 | 纯JDK Instant/OffsetDateTime；不连接基础设施 |
| TEST-006 | Step 5/7 | Service/Open既有PB/Facade/Manage测试；原ScheduleCourse/CreateExam请求与读取；native strings/标准Timestamp nanos；输出关联/状态；字段号/消息/原metadata/错误保持；Command生成规范Instant、返回值不echo高精度未存值；原读取字段类型不变 | 真实PB和Converter，mock Repository必要边界 |
| TEST-007 | Step 1 | TimeTypeMappingTest + PersistenceTemplateTest/BackendCrudTemplateTest；同schema timestamp with/without、timestamptz alias、date、time、精度/大小写/空格；两入口一致FQN/import；不同semantics四种类型；生成PO/Command/Result/converter可编译，不引入Date | 当前CREATE AST与FreeMarker、临时fresh native项目fixture |
| TEST-008 | Step 1 | PostgreDdlAdapterTest + base/type scope；aware create/update、naive nullable deleted；timestamptz别名；timetz/interval/非法p/ALTER；base校验保持zone；unknown temporal诊断；CREATE-only/无越权profile/无强行String | 真实parser/scope，无数据库 |
| TEST-009 | Step 3/4/5/7 | 四SQL、manifest与八表；仅starts/ends类型改变；SHA按文件bytes；原version/role/index/FK/deletedAt；8个不同表名/12个模板表实例aware(6)；各SHA与源字节匹配；version不新增；light_前缀与原删除列/其余索引保持 | 读取source SQL/JSON与AST，禁止执行 |
| TEST-010 | Step 2 | EgonColaTableInfoSchemaMaintainerTest/TimeTypesPO；Instant、OffsetDateTime、LocalDateTime、LocalDate、LocalTime及unsupported；drop=true；精确native types；Legacy删除naive；unknown时间不TEXT；所有mock DataSource零调用 | 真实TableInfo，固定Clock，unsupported nested PO夹具隔离 |
| TEST-011 | Step 2/7 | EgonModelTest、原LogicDelete/Lifecycle tests；fixed Clock多候选/Primary/唯一自定义；NULL active、反复删除/恢复/冲突、UTC token；audit既有micro；主机Clock覆盖与fallback不变；deletedAt仍LocalDateTime/UTC；version/tenant guard不变 | 默认纯fixture；PG部分另外env-gated用户目标 |
| TEST-012 | Step 3/4/5/7 | 原Manage/Repository/caller tests；权限/tenant拒绝、duplicate、重叠、关联缺失、异常回滚/响应未知；UTC变更不绕过guard、不引新自动retry/伪持久幂等/MQ原子性 | 原mock/fixture；不声称真实MQ运行 |
| TEST-013 | Step 7 | generate/check、源verify/ArchUnit、选中native隔离生成；新BO/import/MapStruct/PO SuperBuilder、PB namespace、其他层/初始CREATE ownership；各profile编译/架构与schema一致；不手改派生树；native旧静态基线问题独立记录；不覆盖用户编辑 | 既有离线依赖，临时隔离生成，不安装/升级未知依赖 |
| TEST-014 | Step 6 | 七ArchetypeUtcConfigurationTest；base/dev/test/prod key一致、UTC、launch/Docker默认；Instant/date/time序列化；无APP_TIME_ZONE传输fallback；时间键每profile相同、Z输出、纯date稳定；不能用改TZ掩盖错误类型/迁移deleteAt；保留已有Clock名称行为 | 只读配置，Spring Boot测试ObjectMapper/纯JDK，不拉起应用 |

## 9. Migration, Compatibility, Rollout, and Rollback

本任务没有新Flyway/MP版本、数据backfill或实际数据库迁移。唯一SQL例外为§5四个未部署source初始化版本 `V20260913_001` 原位修改与各自SHA；不得改其他已有迁移，也不得repair已应用的ddl_history。修改顺序：单套类型/mapper与验证→初始化列→本套manifest bytes→静态合同；全部源完成后generate/check，再在新生成的隔离空目标检验。对已经生成/部署的项目，不能用这一源checksum覆盖旧history；用户另立新迁移任务，本Plan不提供操作真实库的捷径。

Light旧无偏移请求破坏性停止支持，README/OAS/fixtures同时明确Z/offset与微秒；四套已有字段名/协议号与nativePBstring、OpenServiceTimestamp载体保留。新高精度输入先截断到微秒，不round；返回规范领域/已存值，不能echo旧nanos。deletedAt及restore token、Clock覆盖、技术审计、租户/事务/MQ/APIwrapper保持。无双读写/兼容naive解析/featureflag/新business-zone参数。

发布是源码/新模板产物交付，本Plan不自动部署、启动或推送。回退仅撤销本人时间hunk与重新生成旧源派生，不碰用户其他dirty、不跨实际DB边界。任一源manifest/schema检查失败则停止该套输出与最终commit，修正准确源文件；PG异常返回Spec设计复核。现有分布式DDL失败/逐target重试由原MP-SDJ机制持有，未增加重试器或history操作。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | 主Spec §4/§7～§16 | Step 3, Step 4 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/manage/SchoolClassManageTest.java` 等 | TEST-001/002/003/005/006/012/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-002 | 主Spec §4/§7～§16 | Step 3, Step 4 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/ScheduleUtcConverterTest.java` 等 | TEST-001/003/005/006 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-003 | 主Spec §4/§7～§16 | Step 2, Step 3, Step 4, Step 5, Step 7 | §5完整路径；`egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaUtcTimePostgreSqlTest.java` 等 | TEST-004/005/009 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-004 | 主Spec §4/§7～§16 | Step 2 | §5完整路径；`egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java` 等 | TEST-010/011 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-005 | 主Spec §4/§7～§16 | Step 1 | §5完整路径；`egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/TimeTypeMappingTest.java` 等 | TEST-007/008 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-006 | 主Spec §4/§7～§16 | Step 2 | §5完整路径；`egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/support/time/TimeTypesPO.java` 等 | TEST-010 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-007 | 主Spec §4/§7～§16 | Step 6, Step 7 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/ArchetypeUtcConfigurationTest.java` 等 | TEST-014 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-008 | 主Spec §4/§7～§16 | Step 3, Step 4, Step 5, Step 7 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/repository-manifest.json` 等 | TEST-009 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-009 | 主Spec §4/§7～§16 | Step 7 | §5完整路径；`scripts/work/2026-10-13-16-21-utc-source-audit.sh` 等 | TEST-007/008/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-010 | 主Spec §4/§7～§16 | Step 7 | §5完整路径；`scripts/work/2026-10-13-16-21-utc-source-audit.sh` 等 | TEST-008/011/012/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-011 | 主Spec §4/§7～§16 | Step 3, Step 4, Step 5 | §5完整路径；§5两Light Request/Handler/Controller/Converters与四ServiceCommands及对应tests | TEST-001/002/003/005/006/012/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-012 | 主Spec §4/§7～§16 | Step 7 | §5完整路径；本Plan与Spec接受/Related Plans元数据；Step7文档gate | 文档strict、路径/元数据/dirty审计 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-001 | 主Spec §4/§7～§16 | Step 3, Step 4 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/manage/SchoolClassManageTest.java` 等 | TEST-001/002/003/005/006/012/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-002 | 主Spec §4/§7～§16 | Step 3, Step 4 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/ScheduleUtcConverterTest.java` 等 | TEST-001/003/005/006 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-003 | 主Spec §4/§7～§16 | Step 2, Step 3, Step 4, Step 5, Step 7 | §5完整路径；`egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaUtcTimePostgreSqlTest.java` 等 | TEST-004/005/009 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-004 | 主Spec §4/§7～§16 | Step 2 | §5完整路径；`egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java` 等 | TEST-010/011 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-005 | 主Spec §4/§7～§16 | Step 1 | §5完整路径；`egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/TimeTypeMappingTest.java` 等 | TEST-007/008 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-006 | 主Spec §4/§7～§16 | Step 2 | §5完整路径；`egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/support/time/TimeTypesPO.java` 等 | TEST-010 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-007 | 主Spec §4/§7～§16 | Step 6, Step 7 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/ArchetypeUtcConfigurationTest.java` 等 | TEST-014 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-008 | 主Spec §4/§7～§16 | Step 3, Step 4, Step 5, Step 7 | §5完整路径；`egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/repository-manifest.json` 等 | TEST-009 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-009 | 主Spec §4/§7～§16 | Step 7 | §5完整路径；`scripts/work/2026-10-13-16-21-utc-source-audit.sh` 等 | TEST-007/008/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-010 | 主Spec §4/§7～§16 | Step 7 | §5完整路径；`scripts/work/2026-10-13-16-21-utc-source-audit.sh` 等 | TEST-008/011/012/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-011 | 主Spec §4/§7～§16 | Step 3, Step 4, Step 5 | §5完整路径；§5两Light Request/Handler/Controller/Converters与四ServiceCommands及对应tests | TEST-001/002/003/005/006/012/013 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |
| REQ-012 | 主Spec §4/§7～§16 | Step 7 | §5完整路径；本Plan与Spec接受/Related Plans元数据；Step7文档gate | 文档strict、路径/元数据/dirty审计 | 获批实施后记录各GREEN/输出；本轮只有文档/路径证据 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-PLAN-001 | Spec已接受，Plan未批准实施 | All | 本轮用户原文与Review状态 | 用户 | Closed design；Plan等待审核，不运行实施 |
| DEC-PLAN-002 | 四未部署模板SQL原位不增版 | Step3/4/5 | 主Spec DEC-003及最新用户确认 | 用户/实施者 | Closed；严格限定四SQL+checksum |
| DEC-PLAN-003 | deletedAt保留，排课unique-key缺口排除 | Step2–7 | 最新用户明确scope | 实施者 | Closed；不为缺口新增索引或扩大任务 |
| RISK-PLAN-001 | native安全并行dirty、少量基线编译拼写漂移 | Step3/7 | 当前source/diff；主Spec EVD-014 | 实施者 | 实施前重新检查；相同hunk不覆盖他人，baseline缺陷单独记录 |
| RISK-PLAN-002 | 缓存不含声明依赖/JDK版本不匹配 | §8所有Maven命令 | wrapper/POM；offline本轮未运行 | 实施者 | 如发生记录exact失败；不擅改版本/新增依赖 |
| RISK-PLAN-003 | 真实PG绑定未证明 | Step2/7 integration | 新gate尚未执行 | 用户/实施者 | 无环境不是计划设计阻塞；交付时明确Not run，不伪造PASS |
| RISK-PLAN-004 | 旧generated/legacy归档gate可能baseline失败 | Step7 | 生成/check/verify脚本现有边界 | 实施者 | 先测基线与新增发现，不得削弱gate；未成功不能声明全范围运行通过 |

本Plan没有未解决的时间设计或需要用户再选的重大方案；上述运行前提是实施证据条件，不把已批准SQL/兼容决策重新变成确认请求。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

PASS：保留原12个REQ，REQ-012文档交付按其原轮次记录；所有147目标有Step/File，SQL四版本例外、deletedAt兼容、既有唯一键排除原样落实。未实施、启动或提交。

### 12.2 Spec consistency

PASS：只消费已接受设计。§6.4三项clarification仅提交纪律、scratch证据路径与测试定位，未新增API、DB字段/版本、pattern层、依赖、页面、transport。§4.5必要性审计PASS，复用原Adapter/Facade/Command/Domain Event和短分支，没有fetch-then-forward或参数预检接口。

### 12.3 Repository executability

PASS（计划审查）：131原文件已检查、16拟新增路径不存在；基线SHA/首声明、精确调用者与测试/命令可定位。每Step先RED后生产、临时compile红态在同Step修复；每源完整GREEN是验收边界；最多一次commit且dirtyhunk隔离。此结论不表示未来Maven/PG/generate已跑。

### 12.4 Test and release completeness

PASS（计划覆盖）：TEST-001～014有所属Step、负向/precision/group/真实Mapper/OAS/PB/源SQL+SHA门禁；默认无基础设施，PG显式opt-in且跳过不算通过；scratch最终删除，source/derivedcheck与rollback边界准确。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.7/§5/§7、原source七profile与组件现有包 | 各树保持原COLA；组件未改造为业务层 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-REUSE-001 | Applicable | PASS | §4.7 capability ledger、原Jackson/MapStruct/MP/DDL parser | 缺口仅两局部decoder和既有映射修改 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-DEP-001 | Applicable | PASS | §4.7/§6.2，全部既有POM只读 | 新增依赖/版本/框架为零 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-NAME-001 | Applicable | PASS | Step3/4 CourseScheduleBO同位rename及所有消费者 | 后缀规范局部落实；无宽泛重命名 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-VALID-001 | Applicable | PASS | Step3/4三validator/Controller/Command，Step5Commands；TEST-001/002/005/006 | 原分组、null、micro先后顺序、失败无写入覆盖 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-MODEL-001 | Applicable | PASS | Step3/4 PO与BO/Commands；Step2 TimeTypesPO/EgonModelTest | 可变PO标准Lombok及SuperBuilder；immutable record保持；删除兼容 | Rule 11 排课unique-key差距按用户明确排除；本轮不整改 |
| MC-CONVERT-001 | Applicable | PASS | Step3/4native原LightFacadeConverter与两TeachingApplicationConvertor；TEST-003 | 实际MapStruct与Egon BaseConverter/Forward保留，无JSON/BeanUtils旁路 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-LOG-001 | Applicable | PASS | Step3/4业务/validator/handler与§4.7 | @Slf4j及低噪声原日志；不记录凭据/raw body | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-BEAN-001 | Applicable | PASS | 原字段Qualifier/lombok.config与Step业务块；Step2 read-only AutoConfigurationTest | 具名/RequiredArgsConstructor/Qualifier约束保留；hostClock unique/Primary不改 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-UTIL-001 | Applicable | PASS | Step1/2/3 JDK时间/regex/MessageDigest；无POM改动 | allowlist满足，无自建时间框架 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-JSON-001 | Applicable | PASS | 两Request嵌套decoder/Handler/Controller OAS；TEST-001/003/014 | strict字符串+错误shape负向；输出Z；PB与wrapper保持 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-TIME-001 | Applicable | PASS | §4.9 TIME001～006、所有时间块/§8 TEST004/005/011 | instant/date/time/LegacyUTC分开；PG与Clock证据限制清楚 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-CONFIG-001 | Applicable | PASS | Step6四profile+7launch+6Docker/7test/14README | 时间键parity与默认UTC；Agent无Docker | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-PATTERN-001 | Applicable | PASS | §4.5/§4.7与Step3–5原Facade/Adapter/Domain协作 | 复用Complex流程模式；Simple类型映射无需新Strategy | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-SCOPE-001 | Applicable | PASS | §2.3/§5/§6.3/§9，最新用户唯一键排除 | 147源路径+临时删除harness；真实DB/安全/前端排除 | Rule 11 排课unique-key差距按用户明确排除；本轮不整改 |
| MC-TEST-001 | Applicable | PASS | §4.2/§7/§8 TEST001～014/RED→GREEN/脚本disposition | 门禁可执行且未执行/Skipped/基线错误不伪称通过 | None；此PASS为计划内容审查，非实施/运行结果 |
| MC-BLOCKER-001 | Applicable | PASS | §11用户决策Closed、以上各行与clarifications | 无范围内未决设计；等待Plan审核不是未解决Spec阻塞 | None；此PASS为计划内容审查，非实施/运行结果 |

### 12.6 Final verdict

PASS — Ready for user review

此PASS是批准前Plan内容审查的历史结果。用户随后已批准执行；实际实施与独立验收结论在文末，全Spec结论为PARTIAL。

## Java / CQE contract review

POJO/value object/enum：只两个CourseScheduleBO record和六时间Command及两可变PO触达，无enum修改；测试PO同规范。组件MP：既有EgonModel/MP-SDJ不替代。annotationvalidation：原@NotNull/@Valid/@Validated/分组与自定义validators保留。业务唯一键+deleted_at/NULL：维护已存在守卫并回归，用户明确排除既有排课unique gap，不虚称已修复。CQE：Command/Query/原Domain Event与既有MQ投递保持，没有采用Outbox/改变技术交付，无需Outbox/Statemachine新方案。DDD：准确profile/Domain无Spring/MP依赖，业务层结构保持。对应MC逐项证据见§12.5。

## Backend generation and dependency evidence

Code Generator现有 `scripts/egon-codegen.sh` 只在Step1测试的临时native LIGHT/WEB/SERVICE新CREATE工程验证未来四类型输出，复用现有model/layout/FreeMarker/JavaCompiler；不对已有定制source-projects重新apply，不支持Open/Agent也不编造命令。四初始化SQL和8个逻辑名称/12实例在§6.3列明，原版本仅获准同位类型修订；产物catalog/java/mapper无需重生覆盖用户源码。此任务没有generator apply或授权的实际业务DDL生成动作，因此不新增classpath SQL journal；若后续另行执行skill apply须遵循其journal协议，不能把此Plan当作授权。archetype派生产物由既有generate/check完全生成，不手写。新增依赖为零，当前声明版本缺失的build问题单独报告。


## Execution authorization and evidence

2026-10-13 用户已批准本Plan实施；严格逐Step验证，所有Step完成后只提交一个聚焦commit。用户确认四SQL原位例外、不改deletedAt、不修既有排课unique差距、保留并行修改、不启动项目。执行基线为 `ee414cb645a67725899d69f2fdddc69865a8f0e8`，替代编写时旧HEAD；已有并行dirty按快照保留。

逐Step记录将在此追加；生产代码实施前已记录工作区baseline与staged状态。

### Execution Step 1 — Verified（按用户指示，最终统一commit）

- Baseline HEAD: `ee414cb645a67725899d69f2fdddc69865a8f0e8`；更早Step保留GREEN，不提前提交。
- Scope: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/TimeTypeMappingTest.java`, `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java`, `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java`, `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java`, `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java`, `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java`, `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java`
- RED: 原baseline成功；新测试27项中5项预期类型断言失败（date/aware/unsupported）；实际CREATE数组28项中TIMESTAMP[]唯一失败。
- GREEN: 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。
- Diff: git diff --check通过；两个mapping、time policy FQN、base zone guard与temporal-only parser数组标记；无POM改动。

| Check ID | Applicability | Status | Evidence | Finding | Action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原codegen template/validation/ddl技术包，未新增层 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-REUSE-001 | Applicable | PASS | JDK regex/Locale、原JSqlParser/DdlSchemaService/FreeMarker；未引工具 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-DEP-001 | Applicable | PASS | POM无改动；声明缓存baseline构建成功 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-NAME-001 | Applicable | PASS | TimeTypeMappingTest、原Service/Validator/Adapter名；无新carrier | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-VALID-001 | Applicable | PASS | unsupported UNKNOWN_SQL_TYPE/base zone BASE_FIELD_TYPE；实际CREATE负向覆盖 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-MODEL-001 | Applicable | PASS | 生成PO原SuperBuilder/EgonModel，三native真实编译 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-CONVERT-001 | Applicable | PASS | 原MapStruct+BaseConverter/Forward模板，真实CRUD生成编译 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-LOG-001 | Applicable | PASS | 原生产@Slf4j保持；无敏感日志或新增高频日志 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-BEAN-001 | Not applicable | N/A | 该Step无Spring managed Bean/外部JSON/环境配置修改，现有基础行为保持 | 范围不涉及此能力 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-UTIL-001 | Applicable | PASS | 只有JDK工具与已有parser，无新Utils | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-JSON-001 | Not applicable | N/A | 该Step无Spring managed Bean/外部JSON/环境配置修改，现有基础行为保持 | 范围不涉及此能力 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-TIME-001 | Applicable | PASS | Instant/LocalDT/LocalDate/LocalTime正确FQN/import，p0..6与unsupported数组关闭 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-CONFIG-001 | Not applicable | N/A | 该Step无Spring managed Bean/外部JSON/环境配置修改，现有基础行为保持 | 范围不涉及此能力 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-PATTERN-001 | Applicable | PASS | Simple短类型分支；原生成Manage/Domain模式未重写 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-SCOPE-001 | Applicable | PASS | 7精确file（含必要parser）+Plan metadata；无SQL/runtime改动 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-TEST-001 | Applicable | PASS | 28 targeted GREEN，原基线通过；RED与夹具修正分开记录 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| MC-BLOCKER-001 | Applicable | PASS | 全部适用行已PASS，无未解决实现失败 | 本Step代码/测试与该要求一致 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |

| Literal rule | Applicability | Status | Diff/path evidence | Test/static evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | TimeTypeMappingTest、原Service/Validator/Adapter名；无新carrier | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 2 | Applicable | PASS | unsupported UNKNOWN_SQL_TYPE/base zone BASE_FIELD_TYPE；实际CREATE负向覆盖 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 3 | Applicable | PASS | 生成PO原SuperBuilder/EgonModel，三native真实编译；原MapStruct+BaseConverter/Forward模板，真实CRUD生成编译 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 4 | Applicable | PASS | 原生产@Slf4j保持；无敏感日志或新增高频日志；技术类非SpringBean无注入 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 5 | Applicable | PASS | 只有JDK工具与已有parser，无新Utils | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 6 | Not applicable | N/A | 本Step无外部Jackson合同、枚举或environment profile更改 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | N/A范围理由见证据 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 7 | Not applicable | N/A | 本Step无外部Jackson合同、枚举或environment profile更改 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | N/A范围理由见证据 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 9 | Applicable | PASS | Simple短类型分支；原生成Manage/Domain模式未重写 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 10 | Applicable | PASS | Instant/LocalDT/LocalDate/LocalTime正确FQN/import，p0..6与unsupported数组关闭 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |
| Rule 11 | Applicable | PASS | 原codegen template/validation/ddl技术包，未新增层；原tenant/deleted_at/version模板守卫 | 根目录Plan Step1 exact ./mvnw -o -pl ...code-generator -am -Dtest=TimeTypeMappingTest,PersistenceTemplateTest,BackendCrudTemplateTest,PostgreDdlAdapterTest,CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:23）。生成native LIGHT/WEB/SERVICE PO/CRUD真实Lombok/MapStruct编译成功。 | 已逐条人工复核 | PLAN-CLAR-004 parser必要正确性文件；用户允许必要文件且unsupported数组本属REQ-005。 |

- Runtime boundary: 纯源码/离线单测；未启动项目、DB、容器或GUI。

### Execution Step 2 — Verified（按用户指示，最终统一commit）

- Baseline HEAD: `b9a515391fc2e623c8c57373d1973d80fa2af968`；更早Step保留GREEN，不提前提交。
- Scope: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/support/time/TimeTypesPO.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaUtcTimePostgreSqlTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainerTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/schema/EgonColaTableInfoSchemaMaintainer.java`
- RED: 41 targeted tests中2预期失败：五时间类型/Legacy deleted_at SQL不符，以及unsupported temporal未抛TIME_TYPE_UNSUPPORTED；原Clock/审计回归通过。
- GREEN: 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。
- Diff: git diff --check通过；仅postgresType时间分支+5个原计划source/test夹具，测试PO按PLAN-CLAR-005隔离scan；EgonModel/MetaObjectHandler/AutoConfiguration生产无改动。

| Check ID | Applicability | Status | Evidence | Finding | Action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原common/mybatis/schema+test包，MP基类/原接口保持 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-REUSE-001 | Applicable | PASS | 原TableInfo/ValidationUtils/EgonModel/Clock/ObjectProvider与MyBatis binding复用 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-DEP-001 | Applicable | PASS | 无POM/新依赖修改，离线声明构建成功 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-NAME-001 | Applicable | PASS | TimeTypesPO及nested *PO、原Maintainer/Test语义后缀 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-VALID-001 | Applicable | PASS | maintain原query校验/drop/tenant顺序保持；unsupported错误及零DataSource交互测试 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-MODEL-001 | Applicable | PASS | TimeTypesPO及nested POs完整Lombok/SuperBuilder/EgonModel；deletedAt声明/NULL/UTC SQL保持 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-CONVERT-001 | Not applicable | N/A | 本Step没有新Converter/外部JSON/环境配置修改 | 范围不涉及此能力 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-LOG-001 | Applicable | PASS | 原Maintainer @Slf4j保留，无新日志/凭据回显 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-BEAN-001 | Applicable | PASS | 原@Component名称/RequiredArgsConstructor/Qualifier保持；实际Clock Bean factory选择none/unique/@Primary/ambiguous四case通过 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-UTIL-001 | Applicable | PASS | JDK java.time/Class/array判断，只有既有测试与MP/Spring APIs | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-JSON-001 | Not applicable | N/A | 本Step没有新Converter/外部JSON/环境配置修改 | 范围不涉及此能力 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-TIME-001 | Applicable | PASS | 五类SQL精确(6)，unsupported temporal/多维array失败；auditmicro、Clock、LocalDateTime deletedAt ABI测试；PG未验证 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-CONFIG-001 | Not applicable | N/A | 本Step没有新Converter/外部JSON/环境配置修改 | 范围不涉及此能力 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-PATTERN-001 | Applicable | PASS | Simple原postgresType局部分支；无新策略/runner/框架 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-SCOPE-001 | Applicable | PASS | 5声明file＋必要fixture子包定位/Plan/Spec；不执行DDL、不改真实库 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-TEST-001 | Applicable | PASS | 41 targeted GREEN及两RED；PG编译证据不算运行证明 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| MC-BLOCKER-001 | Applicable | PASS | 所有适用源码/单测行通过；本Step显式PG gate依Plan暂不运行 | 本Step代码/测试与该要求一致 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |

| Literal rule | Applicability | Status | Diff/path evidence | Test/static evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | TimeTypesPO及nested *PO、原Maintainer/Test语义后缀 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 2 | Applicable | PASS | maintain原query校验/drop/tenant顺序保持；unsupported错误及零DataSource交互测试 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 3 | Applicable | PASS | TimeTypesPO及nested POs完整Lombok/SuperBuilder/EgonModel；deletedAt声明/NULL/UTC SQL保持 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 4 | Applicable | PASS | 原Maintainer @Slf4j保留，无新日志/凭据回显；原@Component名称/RequiredArgsConstructor/Qualifier保持；实际Clock Bean factory选择none/unique/@Primary/ambiguous四case通过 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 5 | Applicable | PASS | JDK java.time/Class/array判断，只有既有测试与MP/Spring APIs | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 6 | Not applicable | N/A | 无外部Jackson/枚举/环境profile触达 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | N/A范围理由见证据 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 7 | Not applicable | N/A | 无外部Jackson/枚举/环境profile触达 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | N/A范围理由见证据 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 9 | Applicable | PASS | Simple原postgresType局部分支；无新策略/runner/框架 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 10 | Applicable | PASS | 五类SQL精确(6)，unsupported temporal/多维array失败；auditmicro、Clock、LocalDateTime deletedAt ABI测试；PG未验证；java.util.TimeZone仅在gated测试模拟JVM配置，不是模型类型 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |
| Rule 11 | Applicable | PASS | 原common/mybatis/schema+test包，MP基类/原接口保持；原SQL提案无执行，NULL/tenant/version兼容保持 | 根目录Plan Step2 exact ./mvnw -o -pl ...common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -Dtest=EgonColaTableInfoSchemaMaintainerTest,EgonModelTest,EgonColaMybatisPlusAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test：41 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 17:31）。 | 已逐条人工复核 | PLAN-CLAR-005只改变测试fixture子包；unique-key差距按用户排除；PG只编译未连接。 |

- Runtime boundary: EgonColaUtcTimePostgreSqlTest已编译，env-gated且本Step未运行；真实PG跨JVM/session往返待用户环境，未启动任何DB/应用。

### Execution Step 3 — Verified（按用户指示，最终统一commit）

- Baseline HEAD: `b9a515391fc2e623c8c57373d1973d80fa2af968`；更早Step保留GREEN，不提前提交。
- Scope: `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/ScheduleUtcConverterTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/ScheduleUtcContractTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/manage/SchoolClassManageTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidatorTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregateTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeSecurityContractTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/resources/mybatis/h2-schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/vos/CourseSchedule.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/command/ScheduleCourseCommand.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/po/ClassCourseSchedulePO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/pojo/convertor/LightFacadeConverter.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/aggregates/SchoolClassAggregate.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/validators/TeachingDomainValidator.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/domain/teaching/service/SchoolClassDomainService.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/validators/TeachingApplicationValidator.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/validators/TeachingRequestValidator.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/application/teaching/manage/impl/SchoolClassManageImpl.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/handler/GlobalExceptionHandler.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassController.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/repository-manifest.json`
- RED: 已声明native安全JAR缓存陈旧导致baseline缺类；离线构建当前已有两starter+依赖并install本地制品后，原63 tests成功。新HTTP/converter 7 tests全部预期断言RED，0 errors；源SQL初始naive列审计RED。
- GREEN: native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。
- Diff: git diff --check通过；原CourseSchedule同位rename为BO；全chain Instant/micro，strict HTTP只匹配Request字段错误；既有NativeSecurityContractTest已用Z，从并行提交继承且未编辑；原ResponseWrapperHandler不改。

| Check ID | Applicability | Status | Evidence | Finding | Action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原Light单模块COLA包与domain无Spring/MP引入；architecture通过 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-REUSE-001 | Applicable | PASS | 原MapStruct/BaseConverter/Forward、Jakarta/ValidationUtils、BootJackson、java.time/MP接口 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-DEP-001 | Applicable | PASS | 无POM/版本/新依赖；只离线刷新既有本地5.4.1内部starter制品以验证当前baseline | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-NAME-001 | Applicable | PASS | CourseScheduleBO同位rename全消费者；原Request/DTO/Command/PO保持角色 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-VALID-001 | Applicable | PASS | Request@NotNull/严格decoder；Command@NotNull/Positive+micro；原App业务INVALID_SCHEDULE；Domain@Valid/NotNull和BO构造；PO原Model Insert/Persisted组 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-MODEL-001 | Applicable | PASS | immutable record+非nullmicro不变量；PO完整Lombok/SuperBuilder/EqualsSuper，无基类字段复制 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-CONVERT-001 | Applicable | PASS | 真实TeachingApplicationConvertor.toSchedule与LightFacadeConverter/nativePB roundtrip通过；原infra builder保留 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-LOG-001 | Applicable | PASS | 触达DomainValidator补@Slf4j；其他业务Bean/handler原注解与低噪声保持，无raw body/凭据log | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-BEAN-001 | Applicable | PASS | Global advice name正确；其余显式Bean名/RequiredArgsConstructor/Qualifier/lombok复制保持并编译 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-UTIL-001 | Applicable | PASS | JDK时间与regex、原框架；无新的Utils/registry | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-JSON-001 | Applicable | PASS | 实际Boot ObjectMapper严格字符串17类非法shape/格式/缺失、原400code/status；ResultRecord/data投影与numeric timestamp/OAS200/400安全headers通过 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-TIME-001 | Applicable | PASS | 两carrier/Command/BO/PO Instant；micro先后、相邻/overlap、DST offset等价；native字符串3/4保持；PG未验证 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-CONFIG-001 | Not applicable | N/A | 环境配置由Step6修改，本Step不碰profile/启动默认 | 范围不涉及此能力 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-PATTERN-001 | Applicable | PASS | 原Adapter/Facade/Command/Domain Service/Event模式保持；micro和解码为简单技术逻辑 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-SCOPE-001 | Applicable | PASS | 27文件声明中NativeSecurity测试已满足因此不制造diff；时间变化+准确OAS/原四SQL例外，无guard/unique-key重写 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-TEST-001 | Applicable | PASS | 73 exact source tests GREEN；7行为RED和SQLRED已记录；原安全11case保留 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| MC-BLOCKER-001 | Applicable | PASS | 所有本Step源码/单测/SQL门禁通过；未以缓存缺类或别处root失败伪称RED | 本Step代码/测试与该要求一致 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |

| Literal rule | Applicability | Status | Diff/path evidence | Test/static evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | CourseScheduleBO同位rename全消费者；原Request/DTO/Command/PO保持角色 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 2 | Applicable | PASS | Request@NotNull/严格decoder；Command@NotNull/Positive+micro；原App业务INVALID_SCHEDULE；Domain@Valid/NotNull和BO构造；PO原Model Insert/Persisted组 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 3 | Applicable | PASS | immutable record+非nullmicro不变量；PO完整Lombok/SuperBuilder/EqualsSuper，无基类字段复制；真实TeachingApplicationConvertor.toSchedule与LightFacadeConverter/nativePB roundtrip通过；原infra builder保留 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 4 | Applicable | PASS | 触达DomainValidator补@Slf4j；其他业务Bean/handler原注解与低噪声保持，无raw body/凭据log；Global advice name正确；其余显式Bean名/RequiredArgsConstructor/Qualifier/lombok复制保持并编译 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 5 | Applicable | PASS | JDK时间与regex、原框架；无新的Utils/registry | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 6 | Applicable | PASS | 实际Boot ObjectMapper严格字符串17类非法shape/格式/缺失、原400code/status；ResultRecord/data投影与numeric timestamp/OAS200/400安全headers通过 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 7 | Not applicable | N/A | profile统一归属Step6 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | N/A范围理由见证据 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 9 | Applicable | PASS | 原Adapter/Facade/Command/Domain Service/Event模式保持；micro和解码为简单技术逻辑 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 10 | Applicable | PASS | 两carrier/Command/BO/PO Instant；micro先后、相邻/overlap、DST offset等价；native字符串3/4保持；PG未验证 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |
| Rule 11 | Applicable | PASS | 原Light单模块COLA包与domain无Spring/MP引入；architecture通过；原tenant/ID/SQL guard/LOCAL/MQ与deletedAt保持，unique gap排除 | native Light Plan Step3 exact selector（-f source POM等价于source cwd）：73 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:01）；包括实际Boot Jackson/MockMvc生成OAS、真实PB/MapStruct、NativeSecurityContractTest与architecture。source SQL/manifest light gate PASS：2表/4窗口列，只type tokens/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006校正既有响应包事实，无新wrapper；旧排课unique-gap按用户明确排除；source SQL同版例外。 |

- Runtime boundary: 只有限MockMvc/annotation Spring上下文测试，无应用main/服务器/PG/容器/GUI启动。额外误从根cwd运行的全reactor命令未命中本Step测试且被既有RPC-test-contract旧构造器测试编译阻塞，不算本StepRED/GREEN；精确source验证已成功。

### Execution Step 4 — Verified（按用户指示，最终统一commit）

- Baseline HEAD: `b9a515391fc2e623c8c57373d1973d80fa2af968`；更早Step保留GREEN，不提前提交。
- Scope: `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/pojo/convertor/ScheduleUtcConverterTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/ScheduleUtcContractTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/SchoolClassManageTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidatorTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregateTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/resources/mybatis/h2-schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/vos/CourseSchedule.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/command/ScheduleCourseCommand.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/pojo/dto/ScheduleCourseRequest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/facade/teaching/dto/ScheduleCourseDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/po/ClassCourseSchedulePO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/pojo/convertor/TeachingApplicationConvertor.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/aggregates/SchoolClassAggregate.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/validators/TeachingDomainValidator.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/domain/teaching/service/SchoolClassDomainService.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/infrastructure/teaching/service/impl/SchoolClassDomainServiceImpl.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/validators/TeachingApplicationValidator.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/validators/TeachingRequestValidator.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/application/teaching/manage/impl/SchoolClassManageImpl.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/handler/GlobalExceptionHandler.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassController.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/db/egon-mp/repository-manifest.json`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/adapter/teaching/controller/SchoolClassControllerTest.java`
- RED: 原Open baseline 17 tests成功；新6项时间测试暴露类型/offset/参数错误包/微秒窗口未拒绝。OAS还证明原operationId=schedule，已校正误记而保留旧操作名。
- GREEN: Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。
- Diff: git diff --check通过；24原目标＋必要已有Controller mock名称caller；全域BO/Instant/micro、严格字段decoder；明确原Bean名与Qualifier；无native RPC/security新增。

| Check ID | Applicability | Status | Evidence | Finding | Action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原Light-open单模块COLA包与domain无Spring/MP引入；architecture通过 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-REUSE-001 | Applicable | PASS | 原MapStruct/BaseConverter/Forward、Jakarta/ValidationUtils、BootJackson、java.time/MP接口 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-DEP-001 | Applicable | PASS | 无POM/版本/新依赖；已声明缓存依赖离线构建 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-NAME-001 | Applicable | PASS | CourseScheduleBO同位rename全消费者；原Request/DTO/Command/PO保持角色；operationId保持原schedule，明确Controller Bean名原schoolClassController。 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-VALID-001 | Applicable | PASS | Request@NotNull/严格decoder；Command@NotNull/Positive+micro；原App业务INVALID_SCHEDULE；Domain@Valid/NotNull和BO构造；PO原Model Insert/Persisted组 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-MODEL-001 | Applicable | PASS | immutable record+非nullmicro不变量；PO完整Lombok/SuperBuilder/EqualsSuper，无基类字段复制 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-CONVERT-001 | Applicable | PASS | 实际TeachingApplicationConvertor.toSchedule与Open DTO/Command/PO builder链；无PB/native mapper或新时间字段。 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-LOG-001 | Applicable | PASS | 触达DomainValidator补@Slf4j；其他业务Bean/handler原注解与低噪声保持，无raw body/凭据log | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-BEAN-001 | Applicable | PASS | Global advice name正确；其余显式Bean名/RequiredArgsConstructor/Qualifier/lombok复制保持并编译 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-UTIL-001 | Applicable | PASS | JDK时间与regex、原框架；无新的Utils/registry | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-JSON-001 | Applicable | PASS | 实际Boot ObjectMapper严格字符串17类非法shape/格式/缺失、原400code/status；ResultRecord/data投影与numeric timestamp/OAS200/400Open无native安全schema新增 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-TIME-001 | Applicable | PASS | 两carrier/Command/BO/PO Instant；micro先后、相邻/overlap、DST offset等价；Open无PB/transport新增；PG未验证 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-CONFIG-001 | Not applicable | N/A | 环境配置由Step6修改，本Step不碰profile/启动默认 | 范围不涉及此能力 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-PATTERN-001 | Applicable | PASS | 原Adapter/Facade/Command/Domain Service/Event模式保持；micro和解码为简单技术逻辑 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-SCOPE-001 | Applicable | PASS | 24原目标＋必要1 mock名称caller；时间变化+准确OAS/原四SQL例外，无guard/unique-key重写 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-TEST-001 | Applicable | PASS | 28 source tests GREEN与原17case基线；OAS/JSON/mapper/Bean caller通过 | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| MC-BLOCKER-001 | Applicable | PASS | 所有本Step源码/单测/SQL门禁通过；未以缓存缺类或别处root失败伪称RED | 本Step代码/测试与该要求一致 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |

| Literal rule | Applicability | Status | Diff/path evidence | Test/static evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | CourseScheduleBO同位rename全消费者；原Request/DTO/Command/PO保持角色 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 2 | Applicable | PASS | Request@NotNull/严格decoder；Command@NotNull/Positive+micro；原App业务INVALID_SCHEDULE；Domain@Valid/NotNull和BO构造；PO原Model Insert/Persisted组 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 3 | Applicable | PASS | immutable record+非nullmicro不变量；PO完整Lombok/SuperBuilder/EqualsSuper，无基类字段复制；实际TeachingApplicationConvertor.toSchedule与Open DTO/Command/PO builder链；无PB/native mapper或新时间字段。 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 4 | Applicable | PASS | 触达DomainValidator补@Slf4j；其他业务Bean/handler原注解与低噪声保持，无raw body/凭据log；Global advice name正确；其余显式Bean名/RequiredArgsConstructor/Qualifier/lombok复制保持并编译 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 5 | Applicable | PASS | JDK时间与regex、原框架；无新的Utils/registry | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 6 | Applicable | PASS | 实际Boot ObjectMapper严格字符串17类非法shape/格式/缺失、原400code/status；ResultRecord/data投影与numeric timestamp/OAS200/400Open无native安全schema新增 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 7 | Not applicable | N/A | profile统一归属Step6 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | N/A范围理由见证据 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 9 | Applicable | PASS | 原Adapter/Facade/Command/Domain Service/Event模式保持；micro和解码为简单技术逻辑 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 10 | Applicable | PASS | 两carrier/Command/BO/PO Instant；micro先后、相邻/overlap、DST offset等价；Open无PB/transport新增；PG未验证 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |
| Rule 11 | Applicable | PASS | 原Light-open单模块COLA包与domain无Spring/MP引入；architecture通过；原tenant/ID/SQL guard/LOCAL/MQ与deletedAt保持，unique gap排除 | Light-open source Plan Step4 selector＋必要SchoolClassControllerTest：28 tests, 0 failures/errors/skips，BUILD SUCCESS（2026-10-13 18:28）；实际MockMvc/BootJackson/OAS与MapStruct。source SQL/manifest light-open审计PASS：2表/4窗口列/原light_前缀，只有types/SHA变。 | 已逐条人工复核 | PLAN-CLAR-006既有响应事实、007旧schedule operationId保持、008必要mock名称；排课unique gap排除，SQL例外限定。 |

- Runtime boundary: 只有限MockMvc/web slice上下文，无应用main、服务器监听、PG、容器或GUI。Open原Header context保持，不能把它当作native生产授权证明。

### Execution Step 5 — Verified（按用户指示，最终统一commit）

- Baseline HEAD: `b9a515391fc2e623c8c57373d1973d80fa2af968`；更早Step保留GREEN，不提前提交。
- Scope: `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/time/WindowUtcCommandTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/time/WindowUtcCommandTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/src/test/java/top/egon/cola/archetype/source/serviceopen/adapter/exam/facade/impl/ExamFacadeImplTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/course/CourseManageTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/exam/ExamManageTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/test/java/top/egon/cola/archetype/source/serviceopen/application/transaction/LocalTransactionBoundaryTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/src/test/java/top/egon/cola/archetype/source/serviceopen/facade/contract/ProtoDescriptorContractTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/architecture/ArchetypeContractConvergenceTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/resources/mybatis/h2-schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImplTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/course/CourseManageTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/exam/ExamManageTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/test/java/top/egon/cola/archetype/source/service/application/transaction/LocalTransactionBoundaryTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/architecture/ArchetypeContractConvergenceTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/resources/mybatis/h2-schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/course/pojo/command/ScheduleCourseCommand.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/src/main/java/top/egon/cola/archetype/source/serviceopen/application/exam/pojo/command/CreateExamCommand.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/course/pojo/command/ScheduleCourseCommand.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/src/main/java/top/egon/cola/archetype/source/service/application/exam/pojo/command/CreateExamCommand.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/repository-manifest.json`
- RED: native/Open各Window/Manage 8 tests中4 failures、0 errors，真实nano传参及折叠窗口未拒绝；日志 /tmp/egon-utc-step5-{native,open}-red.log。
- GREEN: native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。
- Diff: 仅四Command归一、时间相关新/现有测试、两源SQL/H2/manifest。既有LocalTransactionBoundaryTest/ArchetypeContractConvergenceTest无时间误断言，无需编辑，原样运行通过。

| Check ID | Applicability | Status | Evidence | Finding | Action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原两套COLA多模块、Domain无新增框架；原architecture suites通过 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-REUSE-001 | Applicable | PASS | Command compact constructor和原DomainValidator、MapStruct、MP/SQLmanifest复用 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-DEP-001 | Applicable | PASS | 无POM或依赖版本改动，全部离线缓存构建 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-NAME-001 | Applicable | PASS | 四原Command record名称/包/非时间字段保持；新WindowUtcCommandTest明确测试职责 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-VALID-001 | Applicable | PASS | 原@NotNull/Positive/NotBlank保留；null约束、micro折叠与倒置经真实DomainValidator拒绝 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-MODEL-001 | Applicable | PASS | 四immutable record仅compact constructor归一；PO/基类/Domain实体未改 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-CONVERT-001 | Applicable | PASS | Course/Exam Manage真实MapStruct输出；RPC真实PB mapper捕获micro Command与响应；无手写复制 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-LOG-001 | Applicable | PASS | 本Step仅carrier/SQL/测试，不新增生产业务日志或Bean | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-BEAN-001 | Applicable | PASS | 本Step不新增或改变业务Bean；原Manage/Facade构造、Qualifier保持且编译通过 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-UTIL-001 | Applicable | PASS | java.time Instant.truncatedTo(MICROS)；无新工具类/库 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-JSON-001 | Applicable | PASS | 不新增HTTP JSON接口；原native string和Open Timestamp载体保持 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-TIME-001 | Applicable | PASS | 四Command非null先micro，null约束保持；offset、DST、零长、倒置测试；12表/24列限定SQL审计PASS | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-CONFIG-001 | Not applicable | N/A | profile/launch默认统一在Step6，本Step只SQLmanifest SHA变 | 范围不涉及此能力 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-PATTERN-001 | Applicable | PASS | 复用Command/Adapter/Facade/Domain Service；微秒技术规则用直接表达式，无新模式层 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-SCOPE-001 | Applicable | PASS | SQL按明确四源例外原位；H2按逻辑单表每源4列；manifest SHA；deletedAt/index/guard保持 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-TEST-001 | Applicable | PASS | 两source selectors与所有SQL范围审计PASS，精度RED每源4fail/0error可辨 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| MC-BLOCKER-001 | Applicable | PASS | 本Step所有授权源码/单测门禁完成；PG证据待用户环境，不伪称已验证 | 本Step代码/测试与该要求一致 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |

| Literal rule | Applicability | Status | Diff/path evidence | Test/static evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 四原Command record名称/包/非时间字段保持；新WindowUtcCommandTest明确测试职责 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 2 | Applicable | PASS | 原@NotNull/Positive/NotBlank保留；null约束、micro折叠与倒置经真实DomainValidator拒绝 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 3 | Applicable | PASS | 四immutable record仅compact constructor归一；PO/基类/Domain实体未改 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 4 | Applicable | PASS | 本Step不新增或改变业务Bean；原Manage/Facade构造、Qualifier保持且编译通过 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 5 | Applicable | PASS | java.time Instant.truncatedTo(MICROS)；无新工具类/库 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 6 | Applicable | PASS | 不新增HTTP JSON接口；原native string和Open Timestamp载体保持 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 7 | Not applicable | N/A | profile/launch默认统一在Step6，本Step只SQLmanifest SHA变 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | N/A范围理由见证据 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 9 | Applicable | PASS | 复用Command/Adapter/Facade/Domain Service；微秒技术规则用直接表达式，无新模式层 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 10 | Applicable | PASS | 四Command非null先micro，null约束保持；offset、DST、零长、倒置测试；12表/24列限定SQL审计PASS | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |
| Rule 11 | Applicable | PASS | 原两套COLA多模块、Domain无新增框架；原architecture suites通过 | native与Open完整Step5 selector BUILD SUCCESS，RPC/Manage/域validator、原事务和architecture；SQL审计PASS 12table/24columns/8names。日志 /tmp/egon-utc-step5-{native,open}-green.log。 | 已逐条人工复核 | SQL同版本四源限定例外；唯一键差距排除；按用户最终一次commit。 |

- Runtime boundary: 纯源码/离线单测；未启动项目、DB、容器或GUI。

### Execution Step 6 — Verified（按用户指示，最终统一commit）

- Baseline HEAD: `b9a515391fc2e623c8c57373d1973d80fa2af968`；更早Step保留GREEN，不提前提交。
- Scope: `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/time/ArchetypeUtcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/src/main/launch/launch.args`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ArchetypeUtcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/launch/launch.args`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/Dockerfile`, `egon-cola-archetypes/source-projects/egon-cola-source-light/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ArchetypeUtcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/launch/launch.args`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/deploy/container/Dockerfile`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ArchetypeUtcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/launch/launch.args`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/Dockerfile`, `egon-cola-archetypes/source-projects/egon-cola-source-service/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ArchetypeUtcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/launch/launch.args`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/deploy/container/Dockerfile`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ArchetypeUtcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/launch/launch.args`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/Dockerfile`, `egon-cola-archetypes/source-projects/egon-cola-source-web/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ArchetypeUtcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/launch/launch.args`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/deploy/container/Dockerfile`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/README.zh-CN.md`
- RED: 七套各2tests/2fail/0error；分别显示旧Jackson default及缺失launch UTC。日志 /tmp/egon-utc-step6-{family}-red.log（nativeLight初次 /tmp/egon-utc-step6-red.log）。
- GREEN: 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。
- Diff: 28 YAML只时间键、7 src/main/launch/launch.args保留@run.jvm-args@并追加UTC、6Docker只TZ/JVM、14双语ReadMe。七测试真实Bootmapper和四profile源码；source placeholders和target已过滤参数区分校正。

| Check ID | Applicability | Status | Evidence | Finding | Action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 仅七source profile/launch/ReadMe和六既有Docker；新test留原starter或Light测试包，无新增层 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-REUSE-001 | Applicable | PASS | YamlPropertySourceLoader与Boot JacksonAutoConfiguration实际mapper；原Maven launch资源过滤保留 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-DEP-001 | Applicable | PASS | 零POM/依赖/版本变更；解析YAML复用Boot现有库 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-NAME-001 | Applicable | PASS | 七个同职责ArchetypeUtcConfigurationTest；本Step无生产carrier新增 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-VALID-001 | Not applicable | N/A | 本Step无新的业务输入或层间传递；原@Request/Command/Domain/DAO约束不变 | 范围不涉及此能力 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-MODEL-001 | Not applicable | N/A | 本Step无生产model/PO/Enum新增或修改 | 范围不涉及此能力 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-CONVERT-001 | Not applicable | N/A | 本Step无生产Converter/手写映射；mapper由Boot真实管理 | 范围不涉及此能力 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-LOG-001 | Not applicable | N/A | 无生产业务类或日志变更；测试不输出配置凭据 | 范围不涉及此能力 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-BEAN-001 | Not applicable | N/A | 无生产Bean/injection变更；ApplicationContextRunner只导入Jackson配置，有限无应用main | 范围不涉及此能力 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-UTIL-001 | Applicable | PASS | 现有JDK文件/java.time、Boot YAML/Jackson；无新Utils | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-JSON-001 | Applicable | PASS | 四profile声明UTC/字符串日期；每套实际mapper在UTC/Shanghai/NewYork序列化Instant/LocalDate/LocalTime一致 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-TIME-001 | Applicable | PASS | 28profile UTC+ISO；7launch UTC；6Docker TZ/JVM UTC；deletedAt LegacyUTC和展示边界说明 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-CONFIG-001 | Applicable | PASS | 每一套base/dev/test/prod的三个时间键一致：UTC/false/no date-format；APP_TIME_ZONE删除；原其他键保持 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-PATTERN-001 | Not applicable | N/A | 仅静态默认值/文档，简单配置，不引入策略框架；原业务模式保持 | 范围不涉及此能力 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-SCOPE-001 | Applicable | PASS | 62 Plan路径，Web并行安全hunk capture前后独立保留；不新增AgentDocker或迁移真实历史 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-TEST-001 | Applicable | PASS | 七套各2tests共14 tests, 0fail/error/skip；先14个行为RED，后GREEN；git diff --check通过 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| MC-BLOCKER-001 | Applicable | PASS | 本Step所有七source配置门禁通过；运行真实JVM/容器由用户控制，未启动 | 本Step代码/测试与该要求一致 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |

| Literal rule | Applicability | Status | Diff/path evidence | Test/static evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 七个同职责ArchetypeUtcConfigurationTest；本Step无生产carrier新增 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | 已逐条人工复核 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 2 | Not applicable | N/A | 本Step无新的业务输入或层间传递；原@Request/Command/Domain/DAO约束不变 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | N/A范围理由见证据 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 3 | Not applicable | N/A | 本Step无生产model/PO/Enum新增或修改 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | N/A范围理由见证据 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 4 | Not applicable | N/A | 无生产Bean/injection变更；ApplicationContextRunner只导入Jackson配置，有限无应用main | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | N/A范围理由见证据 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 5 | Applicable | PASS | 现有JDK文件/java.time、Boot YAML/Jackson；无新Utils | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | 已逐条人工复核 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 6 | Applicable | PASS | 四profile声明UTC/字符串日期；每套实际mapper在UTC/Shanghai/NewYork序列化Instant/LocalDate/LocalTime一致 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | 已逐条人工复核 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 7 | Applicable | PASS | 每一套base/dev/test/prod的三个时间键一致：UTC/false/no date-format；APP_TIME_ZONE删除；原其他键保持 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | 已逐条人工复核 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 9 | Not applicable | N/A | 仅静态默认值/文档，简单配置，不引入策略框架；原业务模式保持 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | N/A范围理由见证据 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 10 | Applicable | PASS | 28profile UTC+ISO；7launch UTC；6Docker TZ/JVM UTC；deletedAt LegacyUTC和展示边界说明 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | 已逐条人工复核 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |
| Rule 11 | Applicable | PASS | 仅七source profile/launch/ReadMe和六既有Docker；新test留原starter或Light测试包，无新增层 | 七source reactor各ArchetypeUtcConfigurationTest命中2项，全14项0fail/error/skip，BUILD SUCCESS。日志 /tmp/egon-utc-step6-{family}-green.log。 | 已逐条人工复核 | 按用户最终统一commit；无项目/容器启动；测试TimeZone仅模拟JVM并finally恢复。 |

- Runtime boundary: 纯源码/离线单测；未启动项目、DB、容器或GUI。

- Step6 correction validation: PLAN-CLAR-009新增Agent time/package-info.java，完整Agent source回归BUILD SUCCESS（/tmp/egon-utc-step7-source-agent-corrected.log）；无放宽包文档守卫。

### Step7 source regression and environment findings

- 原缓存安全starter不匹配当前源码：Native Light/Service原完整回归出现3/1个缺JwtDecoder错误；去除本次UTC改动的隔离副本也分别复现相同3/1错误。离线重建当前已声明OpenAPI starter及其已有依赖后，两套原工作区完整source回归通过，无时间或安全业务补丁。
- Web并行任务持续修改测试、POM和target：第一次完整源出现旧缓存JwtDecoder/Audit错误；随后原目录曾因并行新增GraphQL测试依赖和共享target内class文件缺失失败。只读冻结当前完整Web源码/definitions后，隔离目录完整回归通过。提交范围的HEAD+本人UTC hunk副本亦单独通过2项配置测试。Web POM、安全代码和后续README修改不属于本次提交。
- Agent完整源发现本次新time测试包缺少既有强制包文档，已按PLAN-CLAR-009补两行package-info，未放宽检查，完整源通过。
- 第一轮generated archetype IT：Agent实例及Native Light实例的源码测试均成功；Light post-build verifier仍要求旧CourseSchedule.java，因此该整条命令失败，后续家庭未运行。该直接相关遗漏已按PLAN-CLAR-010仅修正一个路径常量，重新生成并再次运行真实IT。
- 用户禁止启动项目：Service-open source回归仅排除会启动监听端口的EvaluationDubboTripleIntegrationTest，其他测试完整执行。该排除不是Skipped计数，也不作为transport成功证据。Generated IT通过继承MAVEN_OPTS同一test排除运行，不修改或弱化源验证器。

| Source family | Finite JUnit run | Failures | Errors | Skipped | Evidence and boundary |
| --- | --- | --- | --- | --- | --- |
| light | 233 | 0 | 0 | 0 | `/tmp/egon-utc-step7-source-light-refreshed.log`；原source reactor完整回归 |
| light-open | 155 | 0 | 0 | 0 | `/tmp/egon-utc-step7-source-light-open.log`；原source reactor完整回归 |
| service | 121 | 0 | 0 | 0 | `/tmp/egon-utc-step7-source-service-refreshed.log`；原source reactor完整回归 |
| service-open | 102 | 0 | 0 | 0 | `/tmp/egon-utc-step7-source-service-open.log`；原source reactor完整回归，仅排除监听端口的Dubbo测试 |
| web | 133 | 0 | 0 | 0 | `/tmp/egon-utc-step7-source-web-isolated.log`；冻结当前源码的隔离完整回归 |
| web-open | 97 | 0 | 0 | 0 | `/tmp/egon-utc-step7-source-web-open.log`；原source reactor完整回归 |
| agent | 153 | 0 | 0 | 0 | `/tmp/egon-utc-step7-source-agent-corrected.log`；原source reactor完整回归 |

合计994项JUnit测试通过。上述数字不包括已明确排除的监听端口测试。MP、codegen定向命令另外通过：MP 41项；codegen原Step1 selector 28项。真实PostgreSQL两项env gate显示Skipped（未授权隔离实例）；该2项不得计为通过。

### Generated IT second-run findings and dispositions

`-fae`全7家庭命令已完成，source实例内测试通过不能等同于post-build verifier通过。结果如下：

| Family / case | Result | Cause / task ownership | Disposition |
| --- | --- | --- | --- |
| Agent basic | PASS | 新time包文档和UTC测试均存在 | 保留命令成功证据 |
| Light basic | FAIL | BO旧路径遗漏已消除；当前失败为既有assertMissing(adapter/graphql)，但HEAD已包含native安全GraphQL包 | baseline矛盾，非UTC；不删除安全代码、不弱化守卫 |
| Light-open basic | PASS | 生成样例源码/校验完整通过 | 保留命令成功证据 |
| Service no-peer | FAIL before basic | post-generate要求NativeServiceConfigurationTest旧peer断言1段；HEAD实际已0段。UTC任务未改此测试或post-generate脚本 | baseline矛盾；不恢复旧业务断言来绕过正则。basic独立执行又遇既有NativeSecurityContractTest冻结proto SHA随生成包名改变而失配；source proto/test与HEAD字节相等 |
| Service-open basic | Source tests PASS / verifier FAIL | 被用户no-listener限制排除的EvaluationDubboTripleIntegrationTest无report；旧verifier强制要求该report | Runtime unverified；不伪造report，不弱化verifier |
| Web no-peer | FAIL before basic | 同native Service，NativeWebConfigurationTest旧peer段在HEAD已不存在 | baseline矛盾。basic独立执行的源码测试通过，既有GraphQL测试包缺package-info而被原generic verifier阻断；不属于UTC新增time包 |
| Web-open basic | 首次FAIL；修正后完整PASS | 本任务新time包原缺package-info.java，已按PLAN-CLAR-011补齐 | 冻结提交差分由官方脚本生成；原默认basic完整BUILD SUCCESS，日志`/tmp/egon-utc-step7-frozen-web-open-default-it.log` |

Baseline证据：HEAD Native Light有3个adapter/graphql安全类且HEAD verify.groovy仍assertMissing同目录；HEAD native Service/Web配置测试没有旧properties(...version).endsWith(:1.0})断言，HEAD post-generate仍replaceExactly expected=1。未以当前UTC修改解释这些既有冲突。

新增包文档的源compile/config命令：native Service WindowUtcCommandTest+ArchetypeUtcConfigurationTest通过5项；Web-open配置通过2项；Web隔离冻结副本重新复制新包文档后配置2项通过，日志`/tmp/egon-utc-step7-package-docs-web.log`。仅包文档，无方法或行为变更；原994项fullsource回归结论保持限定。

### Execution Step 7 — 已执行：源码及隔离生成验证完成，整体验收PARTIAL

- Baseline HEAD: `3c7723f1a24e09a2612aad66daf263123910314a`；更早Step保留GREEN，不提前提交。
- Scope: `scripts/work/2026-10-13-16-21-utc-source-audit.sh`, `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- RED: 初始source SQL审计显示四套naive列；真实generated发现旧BO路径和新包文档遗漏，均按CLAR010/011修复；后续失败严格区分baseline/fixture路径/用户未授权runtime。
- GREEN: 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。
- Diff: private index仅UTC source差分；scope/禁用新增旧日期API/SQL byte guard PASS；删除本任务scratch；2 BO rename、必要5包文档与单一BO verifier literal；无POM/skills/手改.generated。

| Check ID | Applicability | Status | Evidence | Finding | Action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原7 source结构、包/模块/方向保持；源architecture suites通过；必要仅BO rename verifier常量；不增加层。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-REUSE-001 | Applicable | PASS | 两原javaType入口、原maintain/Clock/MetaObjectHandler、原Command/Domain Service/MapStruct/BaseConverters/BootJackson/MP；局部field codec解决实证数字强制转换缺口。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-DEP-001 | Applicable | PASS | private delivery index无POM；SDK/OpenAPI缓存刷新使用已声明坐标及现有源码，未改依赖。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-NAME-001 | Applicable | PASS | 两CourseScheduleBO语义rename全部消费者与一个必要native verifier同步；原carrier后缀保持；新增tests/fixture/time包有明确职责与Agent包文档。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-VALID-001 | Applicable | PASS | 真实Request StrictJackson+NotNull、Command原注解与micro、App/Domain原validators及组、PO原Insert/Persisted；负向HTTP零Manage、原事务/LOCAL/tenant守卫复核。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-MODEL-001 | Applicable | PASS | 只immutable record carrier与BO；PO完整Data/构造/Accessors/SuperBuilder/EqualsSuper；共享EgonModel技术字段不复制；真实Lombok/MapStruct编译。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-CONVERT-001 | Applicable | PASS | Teaching/Facade/BaseConverter或BaseForward原路径；实际protobuf/JavaDTO/Command/BO/PO mapper测试；局部ScalarDeserializer是字段技术codec而非对象映射替代。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-LOG-001 | Applicable | PASS | 触达DomainValidator补Slf4j，Advice/业务类原Slf4j保留；不增加raw body/token日志；其余非业务测试/静态配置不套日志要求。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-BEAN-001 | Applicable | PASS | Advice name而非basePackages误用；Open Controller明确原Bean名/Qualifier且MockitoBean caller一致；其余Bean名/Lombok constructor/Qualifier copy和编译保持。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-UTIL-001 | Applicable | PASS | JDK java.time/regex/标准IO与现有Spring/Boot/MyBatis/Jackson；无新Utils/registry/库；TimeZone仅测试模拟环境并finally恢复。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-JSON-001 | Applicable | PASS | 实际Boot Jackson；严格输入17类型/格式负向、success ResultRecord七字段与data原投影、numeric timestamp保留；OAS真实解析，不以文字注解代替。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-TIME-001 | Applicable | BLOCKED | 模型、Command/BO micro、UTC/offset/DST/纯日期/LegacyABI及H2/mapper证据通过；真实PostgreSQL driver/时区/软删除往返未授权运行，不能将H2替代PG。该runtime证据门禁不影响已授权的source-only单commit，但禁止宣称全Spec PASS。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-CONFIG-001 | Applicable | PASS | 独立复核28profiles时间三键同值、7launch、6Docker；实际YAML loader/mapper 14tests；其他值与并行安全hunk保留。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-PATTERN-001 | Applicable | PASS | 复杂用例沿用现有Adapter/Facade、Command、Domain Service/Event/Repository；固定类型矩阵与codec/micro为Simple技术规则，不增模式层。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-SCOPE-001 | Applicable | PASS | 差分preimage→owned-after三方合到HEAD；3Web profile冲突只提取UTC块；foreign POM/security/skills及后续README不stage；四SQL例外、排课唯一键差距排除。 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-TEST-001 | Applicable | BLOCKED | 994 source+codegen28+MP41、SQL、14配置、strict文档和冻结官方generate/check PASS；7-family完整IT仍有已证明的非UTC baseline冲突、未授权监听测试证据缺失 | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| MC-BLOCKER-001 | Applicable | BLOCKED | 源代码缺口已修复；真实PG两项Skipped和生成baseline/transport证明未完成；全Spec PARTIAL，不标记PASS | 本Step代码/测试与该要求一致 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |

| Literal rule | Applicability | Status | Diff/path evidence | Test/static evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 两CourseScheduleBO语义rename全部消费者与一个必要native verifier同步；原carrier后缀保持；新增tests/fixture/time包有明确职责与Agent包文档。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 2 | Applicable | PASS | 真实Request StrictJackson+NotNull、Command原注解与micro、App/Domain原validators及组、PO原Insert/Persisted；负向HTTP零Manage、原事务/LOCAL/tenant守卫复核。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 3 | Applicable | PASS | 只immutable record carrier与BO；PO完整Data/构造/Accessors/SuperBuilder/EqualsSuper；共享EgonModel技术字段不复制；真实Lombok/MapStruct编译。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 4 | Applicable | PASS | Advice name而非basePackages误用；Open Controller明确原Bean名/Qualifier且MockitoBean caller一致；其余Bean名/Lombok constructor/Qualifier copy和编译保持。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 5 | Applicable | PASS | JDK java.time/regex/标准IO与现有Spring/Boot/MyBatis/Jackson；无新Utils/registry/库；TimeZone仅测试模拟环境并finally恢复。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 6 | Applicable | PASS | 实际Boot Jackson；严格输入17类型/格式负向、success ResultRecord七字段与data原投影、numeric timestamp保留；OAS真实解析，不以文字注解代替。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 7 | Applicable | PASS | 独立复核28profiles时间三键同值、7launch、6Docker；实际YAML loader/mapper 14tests；其他值与并行安全hunk保留。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 9 | Applicable | PASS | 复杂用例沿用现有Adapter/Facade、Command、Domain Service/Event/Repository；固定类型矩阵与codec/micro为Simple技术规则，不增模式层。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 10 | Applicable | BLOCKED | 模型、Command/BO micro、UTC/offset/DST/纯日期/LegacyABI及H2/mapper证据通过；真实PostgreSQL driver/时区/软删除往返未授权运行，不能将H2替代PG。该runtime证据门禁不影响已授权的source-only单commit，但禁止宣称全Spec PASS。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |
| Rule 11 | Applicable | PASS | 原7 source结构、包/模块/方向保持；源architecture suites通过；必要仅BO rename verifier常量；不增加层。 | 7套source 994项、codegen28/MP41、SQL12实例24列、14配置与strict文档通过；官方generation wrapper在冻结树exit0；Web-open默认完整generated IT通过；原全7 IT已执行，非UTC baseline和监听证据缺口不计PASS。 | 已逐条人工复核 | 按用户已批准Plan和单commit/no-start边界，源实现及授权验证完成后统一提交；真实PG/监听测试由用户启动，既有生成baseline另行处理；全Spec验收明确PARTIAL。 |

- Runtime boundary: 纯源码/离线单测；未启动项目、DB、容器或GUI。

## Independent final Spec conformance audit

结论：**PARTIAL — Spec requirements are unmet or unverified**。源码实施与已授权有限验证完成；实际PostgreSQL及监听端口未验证，生成工程中的非UTC基线冲突仍存在。主Spec保持Accepted，不能标记Implemented或全验收PASS。

- Effective Specs：主UTC修订全文；两阶段source ownership与MP演进的限定继承范围沿用§2.2，后续用户实施授权取代前轮doc-only限制。
- Delivery：按用户要求最终只创建一个UTC聚焦commit；提交前HEAD `3c7723f1a24e09a2612aad66daf263123910314a`。源码审计与官方生成回归的冻结树 `4e1c7e2a07672526144d6c188f663b5f5e7e2637`；具体提交号在交付消息和git历史中记录。
- Isolation：逐文件capture→owned-after的UTC差分三方合到HEAD；private index不包含并行POM、安全/Agent业务、skills或.generated。两处Web README冲突以去掉UTC段等于HEAD的字节条件校正。原真实index与工作树由提交前后检查保留。
- Scratch disposition：`scripts/work/2026-10-13-16-21-utc-source-audit.sh`最后一次通过12实例/24窗口列/8名称与精确SQL+manifest差分审计后删除。实际scratch负责SQL/manifest字节边界，profile由7套durable JUnit负责，派生由官方脚本负责，范围由private-index审计负责；不保留或提交临时工具。

### Final command outcomes

| Gate | Result | Evidence / limit |
| --- | --- | --- |
| Seven source reactors | PASS: 994 JUnit | 上方7-family表列出命令日志、233/155/121/102/133/97/153；Web冻结当前源码隔离，Service-open排除监听类 |
| Codegen exact Step1 selectors | PASS: 28 | `/tmp/egon-utc-step7-codegen.log`；真实LIGHT/WEB/SERVICE PO/CRUD编译 |
| MP targeted suites | PASS: 41; PG 2 Skipped | `/tmp/egon-utc-step7-components.log`；Skipped不能计PASS |
| Source SQL/manifest | PASS | `/tmp/egon-utc-step7-sql-final.log`；只窗口类型和4 SHA、旧version/deletedAt/index/FK不变 |
| Generation unit safety suite | PASS | `/tmp/egon-utc-step7-generation-unit.log`；原子失败、锁/信号、determinism等既有测试 |
| Official generate/check wrapper | PASS: exit 0 | 冻结提交差分目录原样执行 `bash scripts/regression/test-generate-archetypes.sh generation`；`/tmp/egon-utc-step7-frozen-generation-regression.log` |
| Mutable workspace generation | FAIL: source drift | `/tmp/egon-utc-step7-generation-last-captured.log`；原子发布前正确阻断并行写入，不修改锁或手改产物 |
| Seven-family generated IT (-fae) | PARTIAL | `/tmp/egon-utc-step7-generated-it-corrected.log`；上表区分源码测试与verifier；监听类未运行 |
| Frozen original service basic fixture | FAIL: existing frozen proto SHA | `/tmp/egon-utc-step7-frozen-service-basic-it.log`；WindowUtcCommandTest 3/配置2 PASS；NativeSecurityContractTest:130固定源proto SHA随生成包名变化失配，源proto/test与冻结HEAD字节相等 |
| Frozen original web basic fixture | Source PASS; verifier FAIL: existing GraphQL package docs | `/tmp/egon-utc-step7-frozen-web-basic-it.log`；配置2 PASS；HEAD NativeGraphQlSecurityContractTest所在包已缺package-info，不属UTC time包 |
| Frozen original web-open default basic fixture | PASS: BUILD SUCCESS | `/tmp/egon-utc-step7-frozen-web-open-default-it.log`；原默认fixture clean integration-test完整通过；第一次外置fixture文档寻找NPE为验证路径错误，已改原默认fixture |
| Spec/Plan strict + diff whitespace | PASS | 最终strict文档检查与private-index diff --check；结构PASS不等于行为全Spec PASS |

### Requirement evidence matrix

| Requirement ID | Effective Spec source | Expected behavior | Commit evidence | Code/symbol evidence | Test/validation evidence | Status | Gap/impact/next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 窗口统一Instant | 单次UTC提交；上述冻结差分树及§7路径 | 两Light Request/DTO/Command/BO/PO均Instant；Service四Command既有Instant保持；源码/真实mapper编译与994项源回归。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-002 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 显式Z/offset和原PB载体 | 单次UTC提交；上述冻结差分树及§7路径 | ScheduleUtcContractTest实际BootJackson/MockMvc拒绝numeric/naive/空白/无秒等17类；offset/Z等值；native PB3/4 string、Open Timestamp descriptor和真实mapper保持；标准日期/本地时间测试。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-003 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 先micro规范再比较/写入；真实PG往返 | 单次UTC提交；上述冻结差分树及§7路径 | Command及BO在比较前truncatedTo(MICROS)；24列aware(6)、SQL/H2/DTO/Command测试通过；EgonColaUtcTimePostgreSqlTest编译并因env未开启Skipped，实际PG三时区往返未执行。 | 上方精确Gate及逐Step记录 | Runtime unverified | 用户提供隔离fixture后按Plan env gate执行，未自动启动DB |
| REQ-004 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | deletedAt LegacyUTC及原ABI/restore | 单次UTC提交；上述冻结差分树及§7路径 | EgonModel.deletedAt LocalDateTime、NULL-active、UTC SQL、version/tenant和restore ABI未改；EgonModelTest/原source持久化守卫通过；真实PG logic-delete/restore env gate Skipped。 | 上方精确Gate及逐Step记录 | Runtime unverified | 用户提供隔离fixture后按Plan env gate执行，未自动启动DB |
| REQ-005 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 两生成入口精确SQL/java.time矩阵 | 单次UTC提交；上述冻结差分树及§7路径 | TemplateContextService.javaType与GenerationScopeValidator.javaType同矩阵；PostgreDdlAdapter保留temporal []使失败可观测；原5selector 28 tests通过，实际LIGHT/WEB/SERVICE PO/CRUD生成编译。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-006 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | MP只读SQL提案精确映射、unsupported关闭 | 单次UTC提交；上述冻结差分树及§7路径 | EgonColaTableInfoSchemaMaintainer.postgresType五种类型精确SQL；TemporalAccessor/Amount及数组fail closed TIME_TYPE_UNSUPPORTED；test verifyNoInteractions(DataSource)。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-007 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 7套4profile/launch及6Docker默认UTC | 单次UTC提交；上述冻结差分树及§7路径 | 7源*4profiles UTC/false/no date-format；7launch保留Maven占位符追加UTC；6既有Docker UTC；真实Bootmapper在3JVM zones保持Instant/LocalDate/LocalTime输出，14配置测试通过；14双语README。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-008 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 仅4未部署初始化SQL例外+SHA | 单次UTC提交；上述冻结差分树及§7路径 | 只4列明V20260913_001源脚本时间类型+4manifest SHA；版本/path/role/index/FK/deletedAt全部byte审计保持；未连接实际DB，无新SQL版本或repair。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-009 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 真源重建派生并校验全部样例 | 单次UTC提交；上述冻结差分树及§7路径 | 官方generation/check冻结差分树成功；Agent/Light-open/Web-open完整样例PASS；Native Light GraphQL目录守卫、Native Service/Web no-peer旧断言及basic的proto SHA/GraphQL包文档为既有baseline失败；Open Service缺未运行监听测试report；未手改.generated；官方generation wrapper在冻结差分树执行，完整post-build gate缺口单独记录。 | 上方精确Gate及逐Step记录 | Partial | 按上方生成baseline/runtime清单另行完成，未在UTC任务更改安全或旧hook |
| REQ-010 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 零新依赖/时间框架/Clock选择改变 | 单次UTC提交；上述冻结差分树及§7路径 | 提交范围无POM/依赖版本/新框架；Java.time/Jackson/原MapStruct与Common MP复用；none/unique/Primary/ambiguous Clock四种factory合同测试，生产Clock选择未改。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-011 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 校验、错误、OAS与消费者准确 | 单次UTC提交；上述冻结差分树及§7路径 | 保留原HTTP/RPC状态/envelope/operationId/metadata/权限；实际OAS required string/date-time/offset示例/200/400通过；微秒折叠无写入；两Light strict field codec仅目标Request错误映射。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |
| REQ-012 | 主Spec §4/对应TIME、API、TEST；§2.2限定继承 | 文档和工作区保留；后续授权实施 | 单次UTC提交；上述冻结差分树及§7路径 | 原Spec-only/Review要求已由后续用户Plan审批和实施授权更新；Accepted Spec与完整执行Plan保留，最新strict validators均通过；并行hunk不stage。 | 上方精确Gate及逐Step记录 | Satisfied | 无未完成源码项；范围约束保持 |

### Independent final Manual Checks

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原7 source结构、包/模块/方向保持；源architecture suites通过；必要仅BO rename verifier常量；不增加层。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-REUSE-001 | Applicable | PASS | 两原javaType入口、原maintain/Clock/MetaObjectHandler、原Command/Domain Service/MapStruct/BaseConverters/BootJackson/MP；局部field codec解决实证数字强制转换缺口。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-DEP-001 | Applicable | PASS | private delivery index无POM；SDK/OpenAPI缓存刷新使用已声明坐标及现有源码，未改依赖。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-NAME-001 | Applicable | PASS | 两CourseScheduleBO语义rename全部消费者与一个必要native verifier同步；原carrier后缀保持；新增tests/fixture/time包有明确职责与Agent包文档。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-VALID-001 | Applicable | PASS | 真实Request StrictJackson+NotNull、Command原注解与micro、App/Domain原validators及组、PO原Insert/Persisted；负向HTTP零Manage、原事务/LOCAL/tenant守卫复核。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-MODEL-001 | Applicable | PASS | 只immutable record carrier与BO；PO完整Data/构造/Accessors/SuperBuilder/EqualsSuper；共享EgonModel技术字段不复制；真实Lombok/MapStruct编译。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-CONVERT-001 | Applicable | PASS | Teaching/Facade/BaseConverter或BaseForward原路径；实际protobuf/JavaDTO/Command/BO/PO mapper测试；局部ScalarDeserializer是字段技术codec而非对象映射替代。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-LOG-001 | Applicable | PASS | 触达DomainValidator补Slf4j，Advice/业务类原Slf4j保留；不增加raw body/token日志；其余非业务测试/静态配置不套日志要求。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-BEAN-001 | Applicable | PASS | Advice name而非basePackages误用；Open Controller明确原Bean名/Qualifier且MockitoBean caller一致；其余Bean名/Lombok constructor/Qualifier copy和编译保持。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-UTIL-001 | Applicable | PASS | JDK java.time/regex/标准IO与现有Spring/Boot/MyBatis/Jackson；无新Utils/registry/库；TimeZone仅测试模拟环境并finally恢复。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-JSON-001 | Applicable | PASS | 实际Boot Jackson；严格输入17类型/格式负向、success ResultRecord七字段与data原投影、numeric timestamp保留；OAS真实解析，不以文字注解代替。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-TIME-001 | Applicable | BLOCKED | 模型、Command/BO micro、UTC/offset/DST/纯日期/LegacyABI及H2/mapper证据通过；真实PostgreSQL driver/时区/软删除往返未授权运行，不能将H2替代PG。该runtime证据门禁不影响已授权的source-only单commit，但禁止宣称全Spec PASS。 | 缺少全部规定证据，不能宣称全Spec PASS | 用户已批准逐Step源码实施、最终单commit及不启动项目；批准Plan的PG为opt-in。源实施提交不代表全Spec验收；PG/监听证明由用户另行运行，baseline生成校验冲突按原流程另行处理。 |
| MC-CONFIG-001 | Applicable | PASS | 独立复核28profiles时间三键同值、7launch、6Docker；实际YAML loader/mapper 14tests；其他值与并行安全hunk保留。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-PATTERN-001 | Applicable | PASS | 复杂用例沿用现有Adapter/Facade、Command、Domain Service/Event/Repository；固定类型矩阵与codec/micro为Simple技术规则，不增模式层。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-SCOPE-001 | Applicable | PASS | 差分preimage→owned-after三方合到HEAD；3Web profile冲突只提取UTC块；foreign POM/security/skills及后续README不stage；四SQL例外、排课唯一键差距排除。 | 最终差分与限定范围一致 | None；用户明确排除既有排课unique-key差距 |
| MC-TEST-001 | Applicable | BLOCKED | 994 source tests、codegen28、MP41、SQL字节审计、14配置/OAS/RPC/真实mapper及strict文档有成功证据；官方generation/check冻结差分树成功；Agent/Light-open/Web-open完整样例PASS；Native Light GraphQL目录守卫、Native Service/Web no-peer旧断言及basic的proto SHA/GraphQL包文档为既有baseline失败；Open Service缺未运行监听测试report；不能将已知baseline失败或排除runtime计为通过。 | 缺少全部规定证据，不能宣称全Spec PASS | 用户已批准逐Step源码实施、最终单commit及不启动项目；批准Plan的PG为opt-in。源实施提交不代表全Spec验收；PG/监听证明由用户另行运行，baseline生成校验冲突按原流程另行处理。 |
| MC-BLOCKER-001 | Applicable | BLOCKED | 真实PG三时区/logic delete恢复证明未运行；官方generation/check冻结差分树成功；Agent/Light-open/Web-open完整样例PASS；Native Light GraphQL目录守卫、Native Service/Web no-peer旧断言及basic的proto SHA/GraphQL包文档为既有baseline失败；Open Service缺未运行监听测试report；source代码相关缺口已修复，全Spec禁止PASS。 | 缺少全部规定证据，不能宣称全Spec PASS | 用户已批准逐Step源码实施、最终单commit及不启动项目；批准Plan的PG为opt-in。源实施提交不代表全Spec验收；PG/监听证明由用户另行运行，baseline生成校验冲突按原流程另行处理。 |

### Independent final Literal Rules

| Literal rule | Applicability | Status | Final diff/path/symbol evidence | Final test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 两CourseScheduleBO语义rename全部消费者与一个必要native verifier同步；原carrier后缀保持；新增tests/fixture/time包有明确职责与Agent包文档。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 2 | Applicable | PASS | 真实Request StrictJackson+NotNull、Command原注解与micro、App/Domain原validators及组、PO原Insert/Persisted；负向HTTP零Manage、原事务/LOCAL/tenant守卫复核。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 3 | Applicable | PASS | 只immutable record carrier与BO；PO完整Data/构造/Accessors/SuperBuilder/EqualsSuper；共享EgonModel技术字段不复制；真实Lombok/MapStruct编译。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 4 | Applicable | PASS | Advice name而非basePackages误用；Open Controller明确原Bean名/Qualifier且MockitoBean caller一致；其余Bean名/Lombok constructor/Qualifier copy和编译保持。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 5 | Applicable | PASS | JDK java.time/regex/标准IO与现有Spring/Boot/MyBatis/Jackson；无新Utils/registry/库；TimeZone仅测试模拟环境并finally恢复。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 6 | Applicable | PASS | 实际Boot Jackson；严格输入17类型/格式负向、success ResultRecord七字段与data原投影、numeric timestamp保留；OAS真实解析，不以文字注解代替。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 7 | Applicable | PASS | 独立复核28profiles时间三键同值、7launch、6Docker；实际YAML loader/mapper 14tests；其他值与并行安全hunk保留。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 9 | Applicable | PASS | 复杂用例沿用现有Adapter/Facade、Command、Domain Service/Event/Repository；固定类型矩阵与codec/micro为Simple技术规则，不增模式层。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |
| Rule 10 | Applicable | BLOCKED | 模型、Command/BO micro、UTC/offset/DST/纯日期/LegacyABI及H2/mapper证据通过；真实PostgreSQL driver/时区/软删除往返未授权运行，不能将H2替代PG。该runtime证据门禁不影响已授权的source-only单commit，但禁止宣称全Spec PASS。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 完整时间runtime证据未获执行，保留BLOCKED | 用户已批准逐Step源码实施、最终单commit及不启动项目；批准Plan的PG为opt-in。源实施提交不代表全Spec验收；PG/监听证明由用户另行运行，baseline生成校验冲突按原流程另行处理。 |
| Rule 11 | Applicable | PASS | 原7 source结构、包/模块/方向保持；源architecture suites通过；必要仅BO rename verifier常量；不增加层。 | 源命令/实际mapper、独立import与scope检查及上述完整Gate记录 | 逐条最终复核，无新增模式层或依赖 | None |

### Delivery boundary and remaining verification

用户已批准逐Step源码实施、最终单commit及不启动项目；批准Plan的PG为opt-in。源实施提交不代表全Spec验收；PG/监听证明由用户另行运行，baseline生成校验冲突按原流程另行处理。

本次没有启动main/server、监听端口测试、数据库、容器或GUI。通过的Spring测试均为有限Mock/annotation/application-context测试。实际PG JDBC、跨session zone及logic delete/restore的未执行证明和Open Triple wire证据不由H2、静态SQL或mock替代。源码聚焦提交后保留并行工作，后续runtime测试由用户启动。

- Commit hook boundary：现有hook只有Qoder post-commit跟踪worker，pre-commit/commit-msg验证hook不存在。本次以命令级core.hooksPath=/dev/null避免额外应用worker；不修改仓库或全局Git配置。

- 派生目录限制：当前共享工作区官方generate因并行source drift在发布前失败，原.generated未被手动覆盖；最终成功的generate/check位于冻结提交差分目录。工作区.generated需在并行源码稳定后由原脚本重建，不把隔离成功等同于共享目录已同步。
