# Yuheng 三资源身份种子实施计划

| Field | Value |
| --- | --- |
| Document | `2026-09-05-16-00-yuheng-oauth-resource-bootstrap-implementation.md` |
| Template Version | `4` |
| Status | `Ready` |
| Created | `2026-09-05 16:00 CST` |
| Updated | `2026-09-05 16:20 CST` |
| Owner | 用户 / Codex |
| Repository | `Egon-COLA` |
| Scope | local Tianquan-Shoubing bootstrap 静态种子与测试；父任务的前置修正 |
| Source Requirement | 用户批准两个 Engine + Admin 三个独立 Resource Server 并继续修复 |
| Baseline Revision | `main@4237b6bdbc95de37c58296e1bdf01890040f74b2` |
| Implements Spec | [Yuheng 三资源身份种子](../spec/2026-09-05-16-00-yuheng-oauth-resource-bootstrap.md) |
| Spec Status | `Accepted` |
| Spec Revision | `2026-09-05 16:20 CST` |
| Effective Specs | [身份种子](../spec/2026-09-05-16-00-yuheng-oauth-resource-bootstrap.md) |
| Depends On Plans | [主计划](2026-09-02-21-03-yuheng-dual-engine-separation-implementation.md) 已提交 Step 6/10 |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | [主计划](2026-09-02-21-03-yuheng-dual-engine-separation-implementation.md) Step 11 的身份先决 |

## 1. Summary

以一个顺序、可单独提交的 Step 实施身份种子 Spec 的四项要求。只修改两个现有 Tianquan-Shoubing 文件和模块 Lombok 配置及 POM 构建依赖；先测试 RED，再声明式修改，再测试 GREEN。继续父任务所需的 Yuheng fan-out、版本核对与运行时验收不由这个先决提交冒充完成。

## 2. Target Spec and Effective Design

### 2.1 Primary target

[身份种子 Spec](../spec/2026-09-05-16-00-yuheng-oauth-resource-bootstrap.md)，Accepted，Updated 2026-09-05 16:20 CST。批准证据为用户本轮确认三组独立身份并要求继续修复；本计划只是该明确选择的最小实现。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [身份种子](../spec/2026-09-05-16-00-yuheng-oauth-resource-bootstrap.md) | Accepted / 2026-09-05 16:20 CST | 全文 | 当前前置交付 |
| Context-only predecessor | [双 Engine 分离](../spec/2026-09-02-19-52-yuheng-dual-engine-separation.md) | Accepted / 2026-09-05 07:01 CST | §7.1、§8 已实现固定角色与 Admin/Engine 边界，由 Primary §6.1 保留 | 该主计划的其余要求不属于此窄范围前置计划，仍在父任务执行 |

### 2.3 Superseded or excluded content

Primary Spec 明确修改父 Spec 的 local Tianquan-Shoubing 范围限制。主计划原同 app/同版本分发结论不作为本计划验收，也不可用此先决完成关闭父任务；下一分发修订继续处理。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | Primary §4/7 | 三组独立 Resource/Client | 精确断言 Admin/API 保留、MCP 新增 | bootstrap 列表 |
| REQ-002 | Primary §4/7.3 | MCP 最小 Tianshu/RBAC/Task grant | PLATFORM vs tenant context 正确；无 Admin scope | 成员和 Client 常量 |
| REQ-003 | Primary §4/16 | 幂等与已有数据保护 | 不重复 save/rotate，不复用旧 Task ID，冲突拒绝 | 新 ID 字面前缀、测试 |
| REQ-004 | Primary §4/9–15 | 鉴权/API/schema/profile 边界不动 | 四文件 diff 与 focused regression | 测试/范围审查 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

测试先行。生产调整种子并按 Rule 4 规范化本类 DI，不增加业务类型/Bean 数量/DB版本；因此测试与种子值在同一个可编译 Step。

### 4.2 Test-first strategy

三组 Resource 精确映射、MCP grant owner/范围和新 ID 的断言会在旧实现 RED。复用既有 mock + @TempDir，再补正确资源/active secret 重入和错误 owner/app 拒绝案例。不把编译失败当 RED。生产改动只使这些行为 GREEN。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | 主计划 Step 6/10 已提交 | None | 任何四个目标文件的并发修改 | 单个先决任务 |

### 4.4 Commit boundaries

一个非空 path-limited commit：fix(tianquan-shoubing): seed separate yuheng resource identities。无自动 push/PR；父任务后续修复独立提交，不 amend 历史。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| MCP Client/Resource | Primary §7 Add | 现有 MACHINE_CLIENTS/RESOURCES 少第三组 | 同 record/reconcile | 一组已定义实体；无新接口 | Implement |
| MCP 服务 Grant | Primary §7 Keep/扩充成员 | TIANQUAN_JIANSHEN_SERVICE_CLIENTS/TIANSHU_REGISTRATION_CLIENTS/MCP_TASK_SERVICE_CLIENT | 原数据驱动协调 | 同类 grant；新 ID 避免旧 PK | Implement |
| 新 API/层/Mapper/策略 | Primary §17 Remove | 现有列表机制足够 | 原样复用 | 无新 fetch-then-forward | 不创建 |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 三身份种子+最小授权 | REQ-001/002/003/004 | IdpDevelopmentClientBootstrapTest | 既有 Tianquan-Shoubing admin test 依赖 | 可重复引导的 MCP 身份 | 后续脚本/Tianshu 目标修复 | Step 1 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | support/bootstrap → oauth/service、resource/repo | 保留 feature-local traditional layered | 无类/包/模块移动 | Step 1 / MC-ARCH-001 |
| Reuse/capability | Entity 工厂和已有列表 reconcile | 复用全部行为 | 不新增 Mapper/Validator | Step 1 / MC-REUSE-001 |
| Naming/model/validation/conversion | 既有 record、requireMatchingResource | 类型/映射/手动构造器不在此次变更面 | 仅必要 DI 注解与实例/前缀；不改 POJO/映射 | Step 1 / MC-SCOPE-001 |
| Bean/logging/util/JSON/time/config | 原 Bean/DI/Instant/profile 条件 | 必要规范化 bootstrap DI；其余结构保留 | 用 diff 阻止无关改造；JDK 工具 | Step 1 / MC-UTIL-001 |
| Business variation/pattern | 既有数据驱动流程 | Simple 常量修正 | 不为新增成员制造策略类 | Step 1 / MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 身份创建 | Spring local bootstrap + OAuthClientService | IdpDevelopmentClientBootstrap#create/reconcile | 完整支持 confidential Client/secret | 复用 | None | Step 1 / MC-REUSE-001 |
| Resource/Grant | Tianquan-Shoubing Entity 工厂与 Repository | createResource/reconcileResourceAndGrant/reconcileMcpTaskServiceGrants | 只缺种子成员与正确 ID 前缀 | 复用 | None | Step 1 / MC-DEP-001 |
| 校验/映射 | existing requireMatchingResource；common ValidationUtils/BaseConverter | 未改层间签名和实例映射 | 没有新 DTO/转换缺口 | 不新增基础设施 | None | Step 1 / MC-VALID-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 4 种子/DI | 不新增类型或修改既有 record 声明；仅种子实例 | focused 测试 + diff 审查 | Step 1 | N/A |
| Rule 2 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | 保留 requireMatchingResource、机密 Client 类型校验与既有 Service/Repo 契约；增加不匹配拒绝测试；无新层间输入对象或复用组 | focused 测试 + diff 审查 | Step 1 | PASS |
| Rule 3 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | 既有 record、Entity 与映射代码不变；无 Converter | focused 测试 + diff 审查 | Step 1 | N/A |
| Rule 4 | Primary §6.2/7.2 | 本类现有注入；Yuheng lombok.config | 测试 → 模块 lombok.config → bootstrap | bootstrap 添加 @Slf4j、显式 Bean 名、@RequiredArgsConstructor；五个 final 依赖标 @Qualifier，保留两项 @Value；模块 lombok.config 复制注解，删除手写注入构造器 | 构造器注解/编译/diff | Step 1 | PASS |
| Rule 5 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | 仅既有 JDK 工具；测试 JUnit/Mockito；不引入新库，仅声明已管理 Lombok 构建依赖 | focused 测试 + diff 审查 | Step 1 | PASS |
| Rule 6 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | 不改外部 JSON 字段或 Jackson 行为 | focused 测试 + diff 审查 | Step 1 | N/A |
| Rule 7 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | 不改任何 profile 的配置键 | focused 测试 + diff 审查 | Step 1 | N/A |
| Rule 9 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | Simple 声明式种子修正，复用 reconcile；未引入复杂业务规则 | focused 测试 + diff 审查 | Step 1 | PASS |
| Rule 10 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | 时间仍用 Instant；测试固定 Instant.EPOCH | focused 测试 + diff 审查 | Step 1 | PASS |
| Rule 11 | Primary §6.2 | bootstrap 的既有 record/reconcile 和 §8 范围 | File 1 测试 → File 2 Lombok 配置 → File 3 种子/DI | 保留父 Spec 批准的 feature-local traditional layered 配置支持边界，无包迁移 | focused 测试 + diff 审查 | Step 1 | PASS |

## 5. Change File Tree

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java  MODIFY
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java  MODIFY
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/lombok.config  CREATE
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml  MODIFY
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java` | bootstrap 三个既有测试与 fixtures | 增补三身份/授权/幂等/冲突测试 | 锁定可观察行为 | Step 1 | REQ-001/002/003/004 | Codex focused test |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml` | 缺 Lombok 编译依赖；父 POM 已有处理器 | provided/optional Lombok，${lombok.version} | Rule 4 编译支撑 | Step 1 | REQ-004 | focused compile |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/lombok.config` | 模块尚无 lombok.config；Yuheng 已有同形配置 | 复制 Qualifier/Value | DI 元数据传播 | Step 1 | REQ-004 | constructor metadata test |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java` | MACHINE_CLIENTS/RESOURCES/两个授权列表/Task 常量/ID 前缀 | §7 Spec 精确值 | 数据声明修复 | Step 1 | REQ-001/002/003/004 | Codex diff+test |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

cwd=/Users/mario/SelfProject/Egon-COLA；main@4237b6bdb。保持用户 AGENTS 的最小安全改动、每任务一次提交；本次无 subagent。

不触碰 12 个已暂存 Archetype migration 删除、Archetype Spec、shared Web tsbuildinfo、其他新 Spec/Plan。只用 git commit --only 明确四文件，文档先单独提交。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Maven/JDK | ./mvnw，root pom.xml | 当前 Java21，已有依赖 | 编译/模块 |
| Focused | ./mvnw -pl :egon-cola-tianquan-shoubing-admin -am -Dtest=IdpDevelopmentClientBootstrapTest -Dsurefire.failIfNoSpecifiedTests=false test | @TempDir/Mockito | 不启动服务/DB |
| Scope | git diff --check -- 四个 §5 路径；git diff --name-only | 无外部并发覆盖 | 静态 |

### 6.3 Immutable constraints and approved decisions

历史 Flyway 与 Tianquan-Shoubing/Tianshu 核心鉴权完全不改；Admin/API 身份旧值保留；MCP 不拿 Admin scope；旧 grant 不删除。

2026-09-05 16:20 Build correction：首次 GREEN 缺 Lombok compile classpath（identity-step-01-missing-lombok.log）；停止代码推进，先修正 Spec 和本计划，新增本模块 POM 到当前 Step。这是父 POM 已配置处理器与实际依赖的不一致，不改身份设计，不跳过 Rule 4；用户已要求自行决定并继续修复。

### 6.4 Plan Clarifications

None。名称与新 ID 前缀已由 Primary Spec 明确定义，非实施时重设计。

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 补齐三个独立 Yuheng Resource 身份及 MCP 最小授权

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004
- Dependencies: 主计划 Step 6 `0ac2b91c7`、Step 10 `4237b6bdb`
- Baseline state: Admin/API 已有 Resource；MCP Task 仍共用 API Client；三项原 bootstrap 测试
- Observable outcome: MCP 拥有独立 owner/app/Resource 和准确 grant；正确已有值保持
- End state: 身份先决可编译且测试通过；fan-out 和 runtime cutover 仍未交付
- Test-first gate: Required — 缺 MCP Client/Resource、Task 指向 API、旧 grant ID 前缀导致新断言失败
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java`

- Purpose: 先锁定三身份与数据保护行为。
- Symbols: createsOnlyMissingPublicClients、reconcilesOneExactRbac3ServiceGrantForEachConfiguredTenant；新增 preservesExistingGatewayIdentitiesAndSecrets、rejectsMismatchedMcpResource、doesNotReuseLegacyMcpTaskGrantId、reusesExistingMcpGrants
- Repository evidence: 同类使用 JUnit5/Mockito、@TempDir、stubClientCreation 与 machineClient 工厂；不使用真实 DB
- Dependencies and consumers: IdpDevelopmentClientBootstrap 与现有 Entity/VO/Repository
- Why now: 为缺失种子提供行为 RED，不依赖尚不存在的新类型
- Contract/signature changes: 不新增生产签名；测试精确字段/side effects
- Input/output and state mapping: Mock empty/existing resources/clients/grants → afterSingletonsInstantiated → captures；三身份表逐字段校验；每 tenant 断言 RBAC/Task grant；Tianshu tenant=null
- Error and edge behavior: 错误 app/owner 抛现有 IllegalStateException；现有 active Client 加临时 secret 后不 rotate；正确既有 Resource/grant 不 save；新 Task ID 不等旧 tenant ID
- Standards impact: MC-SCOPE-001/MC-TEST-001/MC-VALID-001/MC-TIME-001；保留 POJO 声明；测试验证新的 Bean/DI 元数据，fixtures 时间为 Instant.EPOCH
- Literal rule enforcement: Rule 1/3/6/7 的未触及范围由测试证明；Rule 4 测试显式 Bean 名与构造器 Qualifier/Value；Rule 2 验证错误绑定 fail closed；Rule 5 使用既有 JUnit/Mockito 与 JDK；Rule 9 Simple fixture；Rule 10 Instant；Rule 11 保留现有 test 包
- Implementation pseudocode:

```java
bootstrap.afterSingletonsInstantiated();
verify(clients).create(argThat(c -> c.clientId().equals("yuheng-mcp-gateway-service")
        && c.clientType() == CONFIDENTIAL && c.redirectUris().isEmpty()));
verify(resources).save(argThat(r -> matchesSpecMcpIdentity(r)));
verify(resources).save(argThat(r -> preservesSpecApiIdentity(r)));
verify(resources).save(argThat(r -> preservesSpecAdminIdentity(r)));
// MCP Tianshu grant: PLATFORM/null tenant/exact registration scope;
// RBAC and Task: each configured tenant, exact scope sets.
// Existing resource/client/active secret: no save/rotate.
// Existing correct grants: no save; incorrect resource app/owner: exception.
// New Task grant id starts with dev-mcp-engine-task-grant-, never legacy id.
```

- Verification contribution: ./mvnw -pl :egon-cola-tianquan-shoubing-admin -am -Dtest=IdpDevelopmentClientBootstrapTest -Dsurefire.failIfNoSpecifiedTests=false test；RED 必须是 Mockito/assertion 缺行为，不是编译/环境错误
- After this file: 测试可编译并对原代码出现预期失败

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/lombok.config`

- Purpose: 在当前模块保证 Lombok 复制注入注解。
- Symbols: config.stopBubbling、lombok.copyableAnnotations
- Repository evidence: Yuheng/lombok.config 已使用相同配置；Tianquan-Shoubing admin 无模块配置
- Dependencies and consumers: File 4 的 @RequiredArgsConstructor；File 1 构造器注解测试
- Why now: 生产切换 Lombok 前先准备注解传播
- Contract/signature changes: 无运行 API 变化；只编译期构造注解
- Input/output and state mapping: final 字段 Qualifier/Value → 生成构造器参数
- Error and edge behavior: 缺失/错误注解由 metadata test 拒绝
- Standards impact: MC-BEAN-001/MC-SCOPE-001/MC-TEST-001；仅当前 Tianquan-Shoubing admin 生效
- Literal rule enforcement: Rule 4 注解复制，Rule 7 配置键和值不变，Rule 11 模块根配置
- Implementation pseudocode:

```text
config.stopBubbling = true
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value
```

- Verification contribution: 编译 + IdpDevelopmentClientBootstrapTest 构造参数注解断言
- After this file: 生成构造器可保留五个 Qualifier 和两个 Value

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml`

- Purpose: 修正首次 GREEN 暴露的缺失构建依赖，不省略 Rule 4。
- Symbols: dependencies/org.projectlombok:lombok
- Repository evidence: root lombok.version=1.18.46；xingyuan parent 已有 Lombok annotationProcessorPaths；Tianquan-Shoubing admin 未声明编译依赖
- Dependencies and consumers: File 4 注解；现有 Maven compiler
- Why now: 记录编译失败后修订 Spec/Plan，先补必要 classpath 再重跑 GREEN
- Contract/signature changes: 仅构建 classpath；无 REST/RPC/DB 变化
- Input/output and state mapping: root 受管理版本 → 本模块 provided、optional 依赖
- Error and edge behavior: 版本不复制硬编码；不向下游传递依赖
- Standards impact: MC-DEP-001/MC-BEAN-001/MC-TEST-001；复用现有库和处理器
- Literal rule enforcement: Rule 4 Lombok 编译支持；Rule 5 无新工具库；Rule 11 模块 POM
- Implementation pseudocode:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <scope>provided</scope>
    <optional>true</optional>
</dependency>
```

- Verification contribution: 同 focused 命令重新编译成功；构造器注解测试通过
- After this file: 所有既有处理器与当前注解所需类型均可解析

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java`

- Purpose: 用现有协调机制执行准确种子映射。
- Symbols: MACHINE_CLIENTS、RESOURCES、TIANQUAN_JIANSHEN_SERVICE_CLIENTS、TIANSHU_REGISTRATION_CLIENTS、MCP_TASK_SERVICE_CLIENT、mcpTaskServiceGrantId
- Repository evidence: MACHINE_CLIENTS 中已有 yuheng-biz-gateway-service；RESOURCES 已有 Admin/API；mcpTaskServiceGrantId 仅 tenant hash
- Dependencies and consumers: 原 Entity 工厂/Repositories/投影；local 启动调用；不改方法签名、模块/配置
- Why now: File 1 已固定精确输入、输出和兼容边界
- Contract/signature changes: 不改业务签名；删除手写构造器，由 Lombok 生成同次序七参数构造器；测试改用七参数，移除六参数包内便利构造器
- Input/output and state mapping: MCP Client=yuheng-mcp-gateway-service；Resource=identity-yuheng-mcp-gateway-default-local；URI=https://api.egon.internal/local/identity/yuheng-mcp-gateway-default；biz=identity/app=yuheng-mcp-gateway-default/env=local；RBAC/mock:read沿用 Spec §7；Admin/API 全部保留
- Error and edge behavior: 原 requireMatchingResource、confidential Client 检查、secret 原子写入、精确 grant lookup 保留；旧 API grant 不迁移、不删除，新 ID 不冲突
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-UTIL-001/MC-SCOPE-001/MC-TEST-001；种子与必要 Bean/构造规范化；record/映射/JSON/profile/schema 不变
- Literal rule enforcement: Rule 1/3/6/7 无类型/外部字段/profile 改动；Rule 4 实现 @Slf4j、显式 Bean 名、五个 qualified final 依赖及两项 Value 字段，@RequiredArgsConstructor；Rule 2 原边界与失败契约保留；Rule 5 既有 JDK；Rule 9 Simple 常量变更；Rule 10 java.time 不变；Rule 11 原包不动
- Implementation pseudocode:

```java
MACHINE_CLIENTS = List.of(existingEntries,
    new MachineClientSpec("yuheng-mcp-gateway-service", "Yuheng MCP Engine Local Service"));
RESOURCES = List.of(existingEntries,
    new ResourceSpec("identity-yuheng-mcp-gateway-default-local",
        "https://api.egon.internal/local/identity/yuheng-mcp-gateway-default",
        "identity", "yuheng-mcp-gateway-default", "Yuheng MCP Engine Local",
        "yuheng-mcp-gateway-service", "mock-backend", "mock:read", null));
TIANQUAN_JIANSHEN_SERVICE_CLIENTS = List.of(existingEntries, "yuheng-mcp-gateway-service");
TIANSHU_REGISTRATION_CLIENTS = List.of(existingEntries, "yuheng-mcp-gateway-service");
MCP_TASK_SERVICE_CLIENT = "yuheng-mcp-gateway-service";
// Bean: @Slf4j @RequiredArgsConstructor @Component("idpDevelopmentClientBootstrap")
// final dependencies: @Qualifier("oauthClientServiceImpl"), @Qualifier("identityResourceServerRepository"),
// @Qualifier("identityClientResourceGrantRepository"), @Qualifier("identityClientRepository"),
// @Qualifier("resourceServerProjectionService").
// final String secretDirectory/rbac3ServiceTenantIds retain their original @Value expressions.
// afterSingletonsInstantiated first validates tenantIds(rbac3ServiceTenantIds).
// writeSecret/secretFile normalize Path.of(secretDirectory); each tenant method obtains the
// unchanged Set from tenantIds(this.rbac3ServiceTenantIds). No new algorithm/helper.
// mcpTaskServiceGrantId retains the existing tenant hash calculation:
return "dev-mcp-engine-task-grant-" + suffix;
```

- Verification contribution: 完整 focused GREEN；确认 production diff 限于种子、必要 DI 及局部配置解析，历史 Admin/API 值不变
- After this file: 三身份先决行为 GREEN，其他全平台工作明确继续

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: `./mvnw -pl :egon-cola-tianquan-shoubing-admin -am -Dtest=IdpDevelopmentClientBootstrapTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: Exit 0；原测试与新增三身份/授权/幂等/冲突用例全通过，无被跳过测试当通过
- Failure returns to: File 1（fixture/断言问题）或 File 3（种子字段错误）；触及鉴权/DDL则返回 Spec 修正
- Completion criteria: 四项要求、十条 literal rule、17项 Manual Check 分别有证据；diff无噪声
- Rollback: 记录本 Step hash，必要时人工做 path-limited forward correction；不删数据库资源/secret
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/lombok.config`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan-shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java`
- Commit: `fix(tianquan-shoubing): seed separate yuheng resource identities`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED | /Users/mario/SelfProject/Egon-COLA | ./mvnw -pl :egon-cola-tianquan-shoubing-admin -am -Dtest=IdpDevelopmentClientBootstrapTest -Dsurefire.failIfNoSpecifiedTests=false test | bootstrap test | 缺 MCP 行/owner/ID 失败 | File 1 | REQ-001/002/003；模块 |
| GREEN | 同上 | 同一命令 | bootstrap test | Exit 0，无失败/跳过 | File 3 | REQ-001/002/003/004；模块 |
| Diff | 同上 | git diff --check -- §5 四个精确路径；逐行 diff | 范围 | 不改其他生产符号 | Step 1 | REQ-004；静态 |
| 父任务 runtime | 本地 xingyuan | 后续父任务修订的启动与测试脚本 | 三真实身份、DDC发布、Web | 此计划不宣称已经执行 | 主计划 Step 11 | 非本前置提交完成门槛；总体目标仍须验证 |

## 9. Migration, Compatibility, Rollout, and Rollback

Primary §11/16：没有 Flyway/数据库迁移；已有 local-enabled bootstrap 插入缺失种子。旧 Admin/API 与旧 Task grant 不删。新 MCP ID 避免与旧 Task ID 冲突。先代码/测试，后 Yuheng fan-out/脚本，最后实际 cutover。回退代码不逆向删除数据。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | Primary §4/7 | Step 1 | §5 四文件 | TEST-001 | focused log + commit |
| REQ-002 | Primary §4/7.3 | Step 1 | 同上 | TEST-002 | scoped grants assertions |
| REQ-003 | Primary §4/16 | Step 1 | 同上 | TEST-003 | repeat/mismatch/legacy ID tests |
| REQ-004 | Primary §9–15 | Step 1 | 同上 | TEST-004/diff | scope review |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-001 | 三独立资源 | Step 1 | 用户本轮确认 | User | Closed / 按 Spec 三行实现 |
| RISK-001 | 种子不等于 fan-out 运行成功 | 父任务 | 当前 Coordinator 仍单目标 | Codex | 后续分发修订和 runtime 验收必须继续；不计此 Step 完成 |

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

本计划覆盖 Primary 四要求，保留父任务余项。

### 12.2 Spec consistency

精确三个身份与 grant context/tenant/scope/ID 前缀符合 Spec；无新 API、表、模式、依赖；无 Plan Clarification。

### 12.3 Repository executability

四个文件均已读取且无用户重叠修改；测试能通过已有生产签名定义 RED；Maven wrapper 存在；提交限定范围。

### 12.4 Test and release completeness

新增测试涵盖三身份、最小授权、重入、冲突与旧 ID。无真实数据库启动，后续不能以本 focused pass 代替整体测试。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 现有 Tianquan-Shoubing admin 的 support/bootstrap、oauth/service、resource/repo 分层；仅四个目标文件 | 不移动或引入架构 | None |
| MC-REUSE-001 | Applicable | PASS | IdpDevelopmentClientBootstrap 的 MACHINE_CLIENTS、RESOURCES、reconcileResourceAndGrant | 复用已存在的本地种子协调机制 | None |
| MC-DEP-001 | Applicable | PASS | Tianquan-Shoubing admin POM 缺 Lombok；xingyuan parent 已配置其处理器，root lombok.version=1.18.46 | 只补 provided/optional 的仓库已管理构建依赖 | None |
| MC-NAME-001 | Not applicable | N/A | 复用 MachineClientSpec/ResourceSpec；不新增或修改类型声明 | 无新 POJO 或行为类型 | None |
| MC-VALID-001 | Applicable | PASS | requireMatchingResource 和 confidential Client 检查保留；新增冲突测试 | 现有身份不匹配时拒绝，不覆盖 | None |
| MC-MODEL-001 | Not applicable | N/A | 既有私有 record 的组件、构造器和实体声明均不变 | 只新增种子实例 | None |
| MC-CONVERT-001 | Not applicable | N/A | createResource/reconcileResourceAndGrant 的既有参数映射不变 | 无新 Converter 或跨层映射 | None |
| MC-LOG-001 | Applicable | PASS | IdpDevelopmentClientBootstrap 使用 @Slf4j，成功日志仅记录完成，不记录 secret | Rule 4 的必要 touched-class 规范化 | None |
| MC-BEAN-001 | Applicable | PASS | 显式 idpDevelopmentClientBootstrap、@RequiredArgsConstructor、五个 @Qualifier、两项 @Value；新增 Tianquan-Shoubing admin lombok.config | 生成构造器注解与默认值需测试验证 | None |
| MC-UTIL-001 | Applicable | PASS | 仍为 JDK List/Set/UUID/Files；JUnit/Mockito 属于测试框架 | 不增加工具类 | None |
| MC-JSON-001 | Not applicable | N/A | 既有 allowedScopes 字符串构建、实体字段、序列化契约不变 | 无外部 JSON 契约修改 | None |
| MC-TIME-001 | Applicable | PASS | 既有 Instant 与测试 Instant.EPOCH | 不引入 java.util 日期 | None |
| MC-CONFIG-001 | Not applicable | N/A | @Profile(local) 和 development-bootstrap.enabled 原样保留；无 YAML 键变更 | 本次只种子声明 | None |
| MC-PATTERN-001 | Applicable | PASS | 现有数据驱动 reconcile 流程；只加入一个同形资源和 Client | Simple 常量修正，不引入 Strategy/Factory | None |
| MC-SCOPE-001 | Applicable | PASS | bootstrap/测试、模块 lombok.config、Tianquan-Shoubing admin pom.xml 四文件；不改其他业务类 | 范围锁定 | None |
| MC-TEST-001 | Applicable | PASS | IdpDevelopmentClientBootstrapTest RED/GREEN；身份/授权/重复启动/旧 ID 冲突/错误绑定 | 测试隔离使用 @TempDir 与 Mock | None |
| MC-BLOCKER-001 | Applicable | PASS | 本 Spec 仅身份种子先决修复；发布 fan-out 与真实验收在父任务继续 | 不将前置完成等同总体完成 | None |

### 12.6 Final verdict

PASS — Ready for user review

已获用户本轮执行授权。PASS 是本前置计划可执行性审查，非父任务完成。
