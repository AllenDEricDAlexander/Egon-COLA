# Archetype 原生与 Open 依赖治理迁移实施计划

| Field | Value |
| --- | --- |
| Document | `2026-09-08-03-30-archetype-native-open-dependency-migration.md` |
| Template Version | `4` |
| Status | `Ready` |
| Created | `2026-09-08 03:30 CST` |
| Updated | `2026-09-08 15:08 CST` |
| Owner | `用户 / Egon-COLA 维护者` |
| Repository | `Egon-COLA` |
| Scope | 根 parent、archetypes parent/source roots、native RPC/DDC/API Doc、open/agent 边界、definitions、generator/generated reactor |
| Source Requirement | 用户确认的 parent/BOM 归属、native/open 技术栈边界、Components BOM、ShardingSphere、commons-lang3 和 native Protobuf RPC 迁移 |
| Baseline Revision | `main @ dfd24ce3f57f77e2edc8628dac56fe8aba14b87e`；工作区保留用户未提交 POM/Agent 变更 |
| Implements Spec | [Archetype 原生与 Open 技术栈依赖治理迁移](../spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md) |
| Spec Status | `Accepted` |
| Spec Revision | `2026-09-08 15:08 CST`；用户确认补齐并继续执行 |
| Effective Specs | [native/open dependency governance](../spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md)、[generated reactor design](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md)、[open archetype family](../spec/2026-08-23-16-43-open-source-archetype-family.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本 Plan 按“根 aggregation parent -> `egon-cola-archetypes-parent` -> source/generated roots -> profile-specific dependencies”实施。七个 Step 依次处理 owner/parent、native Protobuf contract、native light、native service/web、open/agent guards、generator normalization、definitions/generated release gate。完成证据为 effective-POM/dependency-tree、native/open/agent 静态扫描、`RpcContractValidator` contract tests、source/generated Maven reactor 和 archetype integration-test；不启动应用或外部基础设施。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [`docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md`](../spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md)
- Status: `Accepted`
- Revision: `2026-09-08 15:08 CST`
- Approval evidence: 用户明确确认“补齐spec后，逐步实现、验证、提交”；本次合约/范围/验证修订完成后直接逐 Step 执行。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [native/open dependency governance](../spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md) | Accepted / 2026-09-08 15:08 CST | §3–§20 | 本次依赖、parent、RPC、profile 和 generator 设计 |
| Normative dependency | [generated reactor design](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md) | existing | §7、§8、§14、§16、§19、§20 | `.generated`、原子生成和 IT 约束 |
| Normative dependency | [open archetype family](../spec/2026-08-23-16-43-open-source-archetype-family.md) | existing | §3、§7、§9、§16 | `-open` 外部技术栈兼容边界 |

### 2.3 Superseded or excluded content

旧的“source root 直接继承 aggregation parent”描述由 primary Spec `DEC-001` 替换为 source/generated root 继承已发布 `egon-cola-archetypes-parent`。`-open` 外部 transport/IDL 保持不变；不改数据库、公共 HTTP 和前端。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | §4 | native light/service/web 使用 Egon DDC/RPC/API Doc | native 无 Dubbo/Nacos/Cloud/Alibaba/Springdoc 直连 | POM、Java、配置、测试、verifier |
| `REQ-002` | §4 | `-open` 保留外部 Spring 体系 | Cloud/Alibaba/Nacos/Dubbo/Triple/gRPC/Protobuf/Springdoc/ShardingSphere 可解析 | open POM/测试/配置 |
| `REQ-003` | §4 | Agent 不引入无用基础设施 | Agent 无 DDC/RPC/Nacos/Dubbo/ShardingSphere | Agent POM/verifier |
| `REQ-004` | §4 | Components BOM 负责组件版本 | 七个 roots 从 BOM 取得 Common/ID/MyBatis/DTP/RPC 版本 | archetype/source POM |
| `REQ-005` | §4 | ShardingSphere owner 在 archetype 层 | source roots 无本地 `${shardingsphere.version}` | parent/source POM |
| `REQ-006` | §4 | commons-lang3 不由 archetype 管版本 | 删除本地 version，保留 Common Core 传递路径 | POM/dependency scan |
| `REQ-007` | §4 | parent 链可独立解析 | root/source/generated 用发布坐标，generated `<relativePath/>` | root/source/generator |
| `REQ-008` | §4 | native 迁移同步源码/配置/测试/Compose/verifier | native 无外部 RPC/registry 残留 | native tree |
| `REQ-009` | §4 | open 保留实际需要的 COLA 基础组件 | `EgonModel`、ID、MyBatis、DTP、BaseConverter 编译 | open POM/source |
| `REQ-010` | §4 | generator topology/原子性/确定性保持 | generate/check/generated IT 通过 | scripts/definitions |
| `REQ-011` | §4、§9 | native facade 使用可验证 Protobuf unary Egon RPC | proto、generated gRPC、Egon annotations、validator/provider/consumer tests 通过 | native contract/adapters/tests |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先让 Maven 模型具备正确 parent、Components BOM 和 version owner（保留旧 native 依赖直至对应完整切换），再在 Light/两个既有 shared facade 发布完整 31-operation native Protobuf contract，随后按真实模块依赖接 light、service/web provider/consumer/DDC/API Doc。open/agent 只改边界 guard，最后改 generator/definitions 并重新生成 `.generated`。

### 4.2 Test-first strategy

POM 以 effective-POM/dependency-tree 失败为 RED；native RPC 以 `RpcContractValidator` 缺少 descriptor/annotation 为 RED；provider/consumer 以 native bean 缺失为 RED；open/agent 以 allowlist 反例为 RED；generator 以 source-local parent/sentinel 泄漏为 RED。每步只做最小 GREEN。

### 4.3 Sequential and parallel boundaries

严格 Step 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7；每个 Step 验证并提交后才开始下一步，不并行修改多个 Step。Step 2 同时建立 Light 与两个 shared facade 的编译前置，Step 3/4 分别接线。Step 6 只运行临时生成 fixture，正式 `.generated` 重建放在 Step 7。

### 4.4 Commit boundaries

Spec/Plan 补齐先作一个 docs commit；七个实现 Step 各一个 path-limited semantic commit，最终执行审计报告单独 docs commit。用户原有8个相关POM改动按授权纳入Step1；Agent五个无关已暂存文件保持原状。`.generated`只在Step7由脚本生成且绝不提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| Archetype parent/BOM owner | §7.0 Add/Move | `egon-cola-archetypes/pom.xml`、七个 source POM | root-local versions | drift and publish risk | Implement |
| Native Protobuf facade contracts | §9 RPC-001..031/REQ-011 | native facades and `RpcContractValidator` | retain Dubbo/Nacos | violates native boundary | Implement |
| New business DTO/DAO layer | §10 reuse | existing DTO/Result/Converter/DAO tree | duplicate models | unnecessary mapping/coupling | Exclude |
| Generator normalization | §7.0 Add | `normalize_generated_product` | copy source POM | local parent leaks | Implement |
| Agent DDC/RPC classes | §3/§7 | Agent only AI/ADK/Agent Flow | universal BOM | unused footprint | Exclude |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Parent/BOM ownership | `REQ-004..007` | effective-POM duplicate owner failure | published parents/BOM | managed versions | all roots | 1 |
| Native unary IDL | `REQ-011` | validator missing descriptor | protobuf/gRPC generation | generated classes | native adapters | 2 |
| Native light transport | `REQ-001,008,011` | Dubbo marker scan | Step 2 contract | provider/reference/DDC wiring | light tests | 3 |
| Native service/web transport | `REQ-001,008,011` | Dubbo client/provider scan | Step 2 contract | module adapters | service/web tests | 4 |
| Open/agent guards | `REQ-002,003,009` | allowlist fixture | Step 1 BOM | family proof | definitions | 5 |
| Generator normalization | `REQ-007,010` | sentinel/relativePath fixture | final source | deterministic products | generated reactor | 6 |
| Generated release gate | `REQ-007,010` | verifier RED | Step 6 output | validated set | release pipeline | 7 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | exact Light/Service/Web/Open/Agent archetype trees and verifiers | preserve exact Egon-COLA profiles | no `biz.*` hybrid or new layer | all; `MC-ARCH-001` |
| Reuse/capability | Components BOM, RPC starter/DDC adapter, platform DDC/OpenAPI, BaseConverter | reuse existing capabilities first | no duplicate registry/converter/doc stack | 1–4; `MC-REUSE-001` |
| Naming/model/validation/conversion | existing DTO/Request/Response/DAO and MapStruct/BaseConverter | no new business carrier | generated messages map to existing DTOs | 2–4; `MC-NAME/VALID/MODEL/CONVERT-001` |
| Bean/logging/injection | existing `@RequiredArgsConstructor`, `@Qualifier`, Spring beans | stable bean names and constructor injection | native adapters/providers follow conventions | 3–4; `MC-LOG/BEAN-001` |
| Utility/JSON/time/config | Jackson/profile files; Common Core transitive commons-lang3 | closed utilities, unchanged JSON/time, profile parity | no new utility or JSON trick | 1,3–5; `MC-UTIL/JSON/TIME/CONFIG-001` |
| Business variation/pattern | DTO/proto Adapter and existing proxy factoriesing; profile/generator variation | use existing profile boundary and centralized normalization | no ceremonial Strategy/Factory | 5–7; `MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| Component versions | Boot management; Components BOM | `egon-cola-components/egon-cola-components-bom/pom.xml` | sufficient | import BOM at archetype owner | None | 1; `MC-REUSE-001` |
| Native RPC | Spring context; RPC starter | annotations and `RpcContractValidator` | source adapters are gap | reuse starter, add proto/adapters | approved native proto only | 2–4; `MC-DEP-001` |
| Native DDC/API Doc | platform/RPC modules | platform POM and module trees | sufficient | reuse starters | None beyond config | 3–4 |
| Open transport | native alternative; Dubbo/Triple/gRPC | open providers/clients/IDL | external is required | keep open stack | no native fallback | 5 |
| Agent workflow | native infra; AI/ADK/Flow | Agent source POM/modules | AI stack sufficient | keep AI only | no DDC/RPC | 5 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | §6.2/§10 | semantic DTO/Request/Response/DAO types | proto, adapters, config carriers | semantic suffixes; generated names compiler-owned | type inventory/compile | 2–4 | PASS |
| Rule 2 | §6.2/§9 | existing validators and Boot validation | provider/consumer tests | preserve `@Valid`/`@Validated` groups | focused tests | 2–4 | PASS |
| Rule 3 | §6.2/§10 | MapStruct/BaseConverter | adapter mapping tests | reuse converters; no BeanUtils/JSON | compile/converter tests | 2–4 | PASS |
| Rule 4 | §6.2/§7 | Spring beans and RPC annotations | native providers/clients/config | `@Slf4j`, names, `@RequiredArgsConstructor`, `@Qualifier` | context scan | 3–4 | PASS |
| Rule 5 | §6.2/§15 | Common Core transitive commons-lang3 | POM/import scans | closed utility allowlist | dependency tree | 1,5 | PASS |
| Rule 6 | §6.2/§9 | public Jackson contracts unchanged | DTO/OpenAPI tests | preserve JSON shape | regression tests | 3–4 | PASS |
| Rule 7 | §6.2/§15 | base/dev/test/prod files | every changed profile file | identical key set | parity check | 3–5 | PASS |
| Rule 9 | §6.2/§13 | DTO/proto Adapter and existing proxy factories | profile/generator files | centralized profile/normalization boundary | static review | 5–7 | PASS |
| Rule 10 | §6.2/§10 | LocalDateTime and Instant DTO fields | transport time conversions | preserve ISO local/instant nanos and absence; forbid Date imports | static scan | 2–5 | PASS |
| Rule 11 | §6.1/§8 | exact archetype trees/verifiers | every Step target | no hybrid architecture | architecture verifier | every Step | PASS |

## 5. Change File Tree

所有实际提交路径必须来自下面逐 Step 展开的文件清单。`MODIFY` 表示该 Step 获授权的相同责任区域；没有语义差异的文件保持字节不变，不制造空改动。多个 Step 使用同一 POM 时，Step1只改owner，Step2只加codegen，Step3/4切换runtime；不是并行编辑。

| Step | Operation | Exact path |
| --- | --- | --- |
| 1 | CREATE | `scripts/check-archetype-dependency-ownership.py` |
| 1 | MODIFY | `scripts/test-bump-cola-version.sh` |
| 1 | MODIFY | `pom.xml` |
| 1 | MODIFY | `egon-cola-components/egon-cola-components-bom/pom.xml` |
| 1 | MODIFY | `egon-cola-components/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-common/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-domain/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-application/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-common/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-domain/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-common/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-common/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-domain/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-adapter/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-application/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-common/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-domain/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml` |
| 1 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/pom.xml` |
| 1 | MODIFY | `scripts/bump_cola_version.sh` |
| 2 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml` |
| 2 | MODIFY | `egon-cola-archetypes/egon-cola-organization-facade/pom.xml` |
| 2 | MODIFY | `egon-cola-archetypes/egon-cola-evaluation-facade/pom.xml` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcContractTest.java` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcContractTest.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcMappingTest.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcContractTest.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcMappingTest.java` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/proto/organization_facade.proto` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/proto/evaluation_facade.proto` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/CourseRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/SchoolClassRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/UserRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/PermissionRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/UserRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RoleRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/PermissionRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/GradeRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/SchoolClassRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/CourseRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ExamRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ScoreRpcService.java` |
| 2 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/LightRpcConverter.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/OrganizationRpcConverter.java` |
| 2 | CREATE | `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/EvaluationRpcConverter.java` |
| 3 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/NativeRpcProviderTest.java` |
| 3 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfigurationTest.java` |
| 3 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeHttpCompatibilityTest.java` |
| 3 | CREATE | `scripts/check-native-archetype-boundaries.py` |
| 3 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/NativeRpcValidationGroup.java` |
| 3 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/RpcIdQuery.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CourseDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateCourseDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateSchoolClassDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/SchoolClassDetailDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/package-info.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/AssignRoleDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/CreateUserDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/GrantPermissionDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/PermissionDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/PermissionDetailDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/UserDetailDTO.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/package-info.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProvider.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProvider.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProvider.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProvider.java` |
| 3 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfiguration.java` |
| 3 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeHttpSecurityConfiguration.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/StudentManagementApplication.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/OpenApiConfig.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/package-info.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/package-info.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml` |
| 3 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-dev.yml` |
| 3 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-prod.yml` |
| 3 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-test.yml` |
| 3 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap.yml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProviderTest.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProviderTest.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProviderTest.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProviderTest.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/StudentManagementApplicationTest.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/RuntimeConfigurationTest.java` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.prod.yaml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.yaml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.prod.yaml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.yaml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.prod.yaml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.yaml` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.example` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.prod.example` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/README.md` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md` |
| 3 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/README.md` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/NativeServiceRpcProviderTest.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcProviderTest.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcContextTest.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClientTest.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClientTest.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/NativeServiceConfigurationTest.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeWebConfigurationTest.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeHttpCompatibilityTest.java` |
| 4 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcIdQuery.java` |
| 4 | CREATE | `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcSchoolClassQuery.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/rpc/CourseRpcProvider.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ExamRpcProvider.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ScoreRpcProvider.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/facade/impl/CourseFacadeImpl.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImpl.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ScoreFacadeImpl.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/teaching/rpc/SchoolClassRpcProvider.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/user/rpc/UserRpcProvider.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/config/NativeRpcConfiguration.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/config/NativeRpcConfiguration.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcConfiguration.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcProperties.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationDirectoryConverter.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcConfiguration.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcProperties.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationQueryConverter.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClient.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClient.java` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClient.java` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClient.java` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClientTest.java` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClientTest.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationClientFailureMapper.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationClientFailureMapper.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextDTO.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextInterceptor.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationFacadeSupport.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/NativeHttpSecurityConfiguration.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/NativeHttpSecurityConfiguration.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-common/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-domain/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-application/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-common/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-domain/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/pom.xml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/EvaluationServiceApplication.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/OrganizationApplication.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/OrganizationSwaggerConfig.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-dev.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-prod.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-test.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap.yml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-dev.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-prod.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-test.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap.yml` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/EvaluationDubboTripleIntegrationTest.java` |
| 4 | DELETE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/OrganizationDubboProviderConfigurationTest.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationDataSourceModeTest.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationExternalFreeContextTest.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationApplicationTest.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationDataSourceModeTest.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/package-info.java` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.prod.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.prod.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.prod.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.example` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.prod.example` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/README.md` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/README.md` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.prod.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.prod.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.prod.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.yaml` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.example` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.prod.example` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/README.md` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md` |
| 4 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/README.md` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/config/package-info.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/rpc/package-info.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/package-info.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/package-info.java` |
| 4 | CREATE | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/config/package-info.java` |
| 5 | CREATE | `scripts/check-archetype-family-boundaries.py` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-common/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-common/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-domain/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-adapter/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-application/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-common/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-domain/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/pom.xml` |
| 5 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy` |
| 5 | CREATE | `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/open-dependency-boundary.groovy` |
| 6 | CREATE | `scripts/test-generated-parent-normalization.sh` |
| 6 | MODIFY | `scripts/generate_archetypes.sh` |
| 6 | MODIFY | `scripts/test-generate-archetypes.sh` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/verify.groovy` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/test/resources/projects/basic/verify.groovy` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/test/resources/projects/basic/verify.groovy` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/main/resources/META-INF/maven/archetype-metadata.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/main/resources/META-INF/maven/archetype-metadata.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/main/resources/META-INF/maven/archetype-metadata.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/main/resources/META-INF/maven/archetype-metadata.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light/architecture-docs/large-monolith-light-domain-architecture.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/javadoc/README.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service/architecture-docs/student-management-service-only-rpc-mq-architecture.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service/src/main/javadoc/README.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web/architecture-docs/multi-project-multi-module-architecture.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web/src/main/javadoc/README.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-agent/architecture-docs/agent-multi-module-architecture.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/main/javadoc/README.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/architecture-docs/large-monolith-light-domain-architecture.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/main/javadoc/README.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/architecture-docs/student-management-service-only-rpc-mq-architecture.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/main/javadoc/README.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/architecture-docs/multi-project-multi-module-architecture.md` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/main/javadoc/README.md` |
| 7 | GENERATED | `egon-cola-archetypes/.generated/**` |
| 7 | MODIFY | `egon-cola-archetypes/pom.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light/packaging-pom.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service/packaging-pom.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web/packaging-pom.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-agent/packaging-pom.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/packaging-pom.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/packaging-pom.xml` |
| 7 | MODIFY | `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/packaging-pom.xml` |

经用户确认补齐的配置解密文件（保持现有目录结构）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`

Light MVC 切片装配修复（同属 Step 3 HTTP 兼容验证，用户已允许修复）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/CourseControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/PermissionControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/RoleControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/UserControllerTest.java`

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

`main @ dfd24ce3f57f77e2edc8628dac56fe8aba14b87e`。执行前记录并比较 staged patch、dirty POM 与所有历史 SQL 哈希。用户已有8个相关 POM 改动获授权在 Step1 中继续；Agent staged .dockerignore/.gitattributes/.gitignore/mvnw/mvnw.cmd 不纳入任何实现提交。不能 broad-stage、reset、clean、amend 或手改 generated。

### 6.2 Build, test, and environment prerequisites

使用仓库 Maven Wrapper/Java21、现有本地 Maven repository。只运行源码、编译、focused/module/in-process/H2 测试和本地生成；不启动应用、浏览器、Docker、外部数据库/Redis/DDC，也不推送、PR或发布。Bootstrap POM/artifact install 与真正运行 test 分开记录。

### 6.3 Immutable constraints and approved decisions

Spec §7.4 是已授权补齐：具体parent版本、Components Commons owner、31个native合约、共享facade归属、Adapter/MapStruct、构造注入proxy、metadata保留、完整native config/Compose迁移和实际generatedprofile。Open外部IDL/SQL与业务接口、所有历史Flyway文件不变。

### 6.4 Plan Clarifications

| ID | Small implementation inference / approved correction | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | source parent写具体5.4.0与emptyrelativePath | Maven parent先于child properties解析，现有发布坐标5.4.0 | 只是使既定parent决策可解析 | parent/effectiveModel gate阻断 |
| PLAN-CLAR-002 | 三个native均选platform OpenAPI MVC | 三个starter当前都有spring-boot-starter-web | 业务HTTP栈不变，明确兼容securitychain | HTTP/OAS regression阻断 |
| PLAN-CLAR-003 | 单个helper/Mapper的builder presence语法可按实际MapStruct生成调整 | protoc/MapStruct已有API，Spec冻结字段和语义 | 仅语法/框架适配，不修改contract | 必须有null/list/time往返测试 |
| PLAN-CLAR-004 | 测试缺共享artifact时先按release顺序本地install | 当前source/reactor与Maven Wrapper | build前置，不是runtime或publish | 不能将skipTests记为GREEN |
| PLAN-CLAR-005 | 无关历史全仓失败依effective generated Spec TEST-020保留并隔离 | 2026-09-03 generated Spec §14明确允许fresh baseline isolation | 不修改或掩盖无关业务；所有受影响gate仍须通过 | 最终不得误报全仓通过 |

| PLAN-CLAR-006 | root Boot `commons-lang3.version` 属性桥接 Components BOM 的3.20.0，静态断言两值一致 | 最小Bootparent+ComponentsBOM effective POM实测仍为3.17.0 | 已接受版本仍为Core3.20.0，archetype/source无本地版本；只修正Maven优先级实现推断 | effective POM不为3.20.0或桥接值漂移即阻断 |

| PLAN-CLAR-007 | 用户确认将三个 native 配置解密器及对应测试共六文件补入 Step 3/4 | 移除 Cloud 后三处源码与三处测试仍依赖 BootstrapConfigFileApplicationListener | 改为 Boot Config Data 之后执行，保留算法、密钥规则、配置优先级；bootstrap 用例改为显式 import | 六文件和既有解密回归必须通过后才提交相应 Step |

| PLAN-CLAR-008 | 在用户“确认，允许修复”的既有授权下补齐 Light 五个 MVC 切片的安全链 import | 两个原切片已实际返回403；显式 ContextConfiguration 未加载新的应用兼容链 | 仅导入当前 NativeHttpSecurityConfiguration，保留全部请求/状态/body/业务验证断言 | 现有五个切片和完整 NativeHttpCompatibilityTest 均必须通过 |

| PLAN-CLAR-009 | 对 LightRpcConverterImpl 这个 MapStruct 生成类增加精确 facadeImplementationPackages 项，同时保留 ..adapter.. | ARCH-010 仅按 Facade 层及 Impl 后缀识别，生成的纯 DTO/Protobuf converter 触发误报 | 不变更已接受的 converter 合同/位置，不改组件规则，不允许其他 Facade 实现迁出 Adapter | 归属 Step 2 的独立修正提交；真实 verify 必须0违规 |

| PLAN-CLAR-010 | root 资源插件启用默认 Maven 分隔符，archetype parent 显式保持 false | Step 4 完整回归中 RpcRuntimeVersionTest 发现 Boot parent 的 @-only 默认值令库版本资源保留占位符 | 恢复库原有 ${...} 过滤，同时保持 source/generated 的 Spring/Velocity 保护和 launch.args 配置 | 归属 Step 1 独立修正；RPC/DDC版本资源为5.4.0，archetype运行配置字节不变 |

| PLAN-CLAR-011 | 既有允许修复授权下补齐 Step 4 新增五个包的 package-info.java | Service/Web generated verifier 逐 Java 包检查，五处新增包缺说明 | 仅沿用相邻包注释格式，不改 Java 行为、模块或验证强度 | 归属 Step 4 的独立文档修正；生成 IT 原包文档断言必须通过 |

| PLAN-CLAR-012 | root 清除 Boot parent 的默认 Shade configuration/执行配置并取消隐式 default phase；子模块显式执行保留 | Step 7 完整 install 因 AppendingTransformer.resource 合入 Bytecode Agent ManifestResourceTransformer 而失败 | 恢复 parent 迁移前库/Agent/JMH 各自的 Shade 配置，不修改组件源码或自定义执行 | Step 1 独立修正；Bytecode Agent/benchmark 包与 manifest 校验、完整 install 必须通过 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — Establish parent and dependency ownership

- Requirements: `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`
- Dependencies: `None`
- Baseline state: root Boot parent 与七个 source parent 删除是用户已有未提交工作；版本 owner 尚重复。
- Observable outcome: 七个 source root 继承具体发布 parent，Components BOM 导出 Commons 3.20.0，ShardingSphere/open versions 上移；保留 native 旧依赖直到其迁移 Step。
- End state: 七个 source root 继承具体发布 parent，Components BOM 导出 Commons 3.20.0，ShardingSphere/open versions 上移；保留 native 旧依赖直到其迁移 Step。 后续 Step 内容保持 Pending。
- Test-first gate: `Required` — 首先运行本Step声明的missing behavior/owner/annotation/normalization测试，记录真实RED后最小GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 5`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE scripts/check-archetype-dependency-ownership.py`

- Purpose: 建立 XML/version owner RED gate。
- Symbols: main; check_parent; check_component_versions; check_commons_owner
- Repository evidence: 七个 source root 当前无 parent，六个 roots 有 commons-lang3.version；已有脚本使用本地 fixture。
- Dependencies and consumers: 七个 source root 当前无 parent，六个 roots 有 commons-lang3.version；已有脚本使用本地 fixture。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: main; check_parent; check_component_versions; check_commons_owner；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 读取当前 root/BOM/source POM，不解析或输出 Maven credentials。
- Error and edge behavior: 缺 parent、root-local version、重复 owner 或内部 source version 被改均非零退出。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 建立 XML/version owner RED gate。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```python
parse Maven XML namespaces
assert source.parent == top.egon:egon-cola-archetypes-parent:root_version and empty relativePath
assert Common component versions come from BOM
assert commons-lang3 == 3.20.0 in Components BOM and absent as local owner
assert ShardingSphere owner is archetypes parent
```

- Verification contribution: 本Step下列命令验证 main; check_parent; check_component_versions; check_commons_owner；需要非零目标测试数或明确静态断言。
- After this file: 建立 XML/version owner RED gate。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 2 — `MODIFY scripts/test-bump-cola-version.sh`

- Purpose: 先证明版本脚本遗漏新增 source parent。
- Symbols: write_fixture; source parent version assertion; rollback assertions
- Repository evidence: 当前 fixture 只检查 aggregator parent 与 egon-cola.version。
- Dependencies and consumers: 当前 fixture 只检查 aggregator parent 与 egon-cola.version。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: write_fixture; source parent version assertion; rollback assertions；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: fixture 新增真实 parent，升级需更新 parent 而保持 project 0.1.0-SNAPSHOT。
- Error and edge behavior: RED 必须为 source parent 未更新；注入失败后所有 POM 恢复。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 先证明版本脚本遗漏新增 source parent。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
fixture source.parent.version = oldVersion
run bump script
assert source.parent.version == newVersion
assert project.version == 0.1.0-SNAPSHOT
assert injected validate failure restores every versioned file
```

- Verification contribution: 本Step下列命令验证 write_fixture; source parent version assertion; rollback assertions；需要非零目标测试数或明确静态断言。
- After this file: 先证明版本脚本遗漏新增 source parent。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 3 — `MODIFY pom.xml`

- Purpose: 将已授权的用户 Boot parent 变更纳入本 Step。
- Symbols: root parent and global defaults
- Repository evidence: baseline dirty diff 仅新增 spring-boot-starter-parent:3.5.16。
- Dependencies and consumers: baseline dirty diff 仅新增 spring-boot-starter-parent:3.5.16。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: root parent and global defaults；增加与Components BOM相等的Boot Commons版本桥接，详见PLAN-CLAR-006。
- Input/output and state mapping: 保留已有共享 release/Flyway/Springdoc defaults，不新增 Cloud/Alibaba/Dubbo/Nacos root owner。
- Error and edge behavior: parent 发布解析错误 fail fast。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 将已授权的用户 Boot parent 变更纳入本 Step。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
retain Spring Boot parent 3.5.16 with empty relativePath
retain current root release/global management
set Boot commons-lang3.version=3.20.0; assert it equals the Components BOM export
// Preserved mapping: 保留已有共享 release/Flyway/Springdoc defaults，不新增 Cloud/Alibaba/Dubbo/Nacos root owner。
// Required failure assertion: parent 发布解析错误 fail fast。
```

- Verification contribution: 本Step下列命令验证 root parent and global defaults；需要非零目标测试数或明确静态断言。
- After this file: 将已授权的用户 Boot parent 变更纳入本 Step。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 4 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: 在组件 BOM 导出 Core 的实际 Commons 版本。
- Symbols: commons.lang3.version; dependencyManagement commons-lang3
- Repository evidence: components parent 当前为 3.20.0，Boot BOM 为 3.17.0。
- Dependencies and consumers: components parent 当前为 3.20.0，Boot BOM 为 3.17.0。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: commons.lang3.version; dependencyManagement commons-lang3；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 将既有 3.20.0 owner 移到 BOM，组件版本条目原样保持。
- Error and edge behavior: 禁止让 Boot 管理回退到 3.17.0。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 在组件 BOM 导出 Core 的实际 Commons 版本。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
declare commons.lang3.version=3.20.0
manage org.apache.commons:commons-lang3 from this property
retain every existing component managed GAV
```

- Verification contribution: 本Step下列命令验证 commons.lang3.version; dependencyManagement commons-lang3；需要非零目标测试数或明确静态断言。
- After this file: 在组件 BOM 导出 Core 的实际 Commons 版本。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 5 — `MODIFY egon-cola-components/pom.xml`

- Purpose: 组件 parent 复用 BOM，消除重复 Commons owner。
- Symbols: Components BOM import; remove duplicated Commons property/entry
- Repository evidence: 当前 Components BOM 不继承任何 parent，可以安全被 parent 导入。
- Dependencies and consumers: 当前 Components BOM 不继承任何 parent，可以安全被 parent 导入。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Components BOM import; remove duplicated Commons property/entry；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 只移动 Commons owner，其他已管理版本不变。
- Error and edge behavior: 不得制造 parent/import 环或更改组件 runtime 行为。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 组件 parent 复用 BOM，消除重复 Commons owner。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
import top.egon:egon-cola-components-bom:${project.version}
remove local commons.lang3.version and duplicate commons-lang3 entry
// Preserved mapping: 只移动 Commons owner，其他已管理版本不变。
// Required failure assertion: 不得制造 parent/import 环或更改组件 runtime 行为。
```

- Verification contribution: 本Step下列命令验证 Components BOM import; remove duplicated Commons property/entry；需要非零目标测试数或明确静态断言。
- After this file: 组件 parent 复用 BOM，消除重复 Commons owner。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 6 — `MODIFY egon-cola-archetypes/pom.xml`

- Purpose: 集中 archetype 依赖和双 gRPC/Protobuf 版本来源。
- Symbols: Components BOM; ShardingSphere management; native/open codegen properties
- Repository evidence: archetypes parent 已发布且继承 aggregation；Open grpc 1.73.0/protobuf 3.25.8，native component 1.75.0/4.32.0。
- Dependencies and consumers: archetypes parent 已发布且继承 aggregation；Open grpc 1.73.0/protobuf 3.25.8，native component 1.75.0/4.32.0。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Components BOM; ShardingSphere management; native/open codegen properties；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: BOM/管理项不会添加 runtime 依赖；Groovy test 依赖、module/profile 结构保持。
- Error and edge behavior: native/open 版本不能交叉覆盖。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 集中 archetype 依赖和双 gRPC/Protobuf 版本来源。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
import Components BOM and existing Springdoc/Cloud/Alibaba management
manage existing ShardingSphere artifact set at 5.5.3
move open version properties to parent
declare separate native.grpc.version=1.75.0/native.protobuf.version=4.32.0 and native codegen plugin management
do not add infrastructure to parent dependencies
```

- Verification contribution: 本Step下列命令验证 Components BOM; ShardingSphere management; native/open codegen properties；需要非零目标测试数或明确静态断言。
- After this file: 集中 archetype 依赖和双 gRPC/Protobuf 版本来源。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 7 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/pom.xml`

- Purpose: 规范七个 roots 和实际受影响的 component dependency declarations。
- Symbols: parent; BOM imports; local version owners; explicit component versions
- Repository evidence: roots 与 child POM 已全部按真实目录展开；child 继续继承自身 source root。
- Dependencies and consumers: roots 与 child POM 已全部按真实目录展开；child 继续继承自身 source root。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: parent; BOM imports; local version owners; explicit component versions；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: root parent 使用 5.4.0 + empty relativePath；内建 source/module version 和模块顺序保持。
- Error and edge behavior: 此时不删 native Dubbo/Nacos 依赖；没有需要改变的 child POM 保持字节不变，提交清单只包含实际 diff。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 规范七个 roots 和实际受影响的 component dependency declarations。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
add concrete archetypes parent to seven roots
remove duplicate Components/Cloud/Alibaba/Springdoc imports where inherited
remove local commons-lang3/shardingsphere/external version properties now owned upstream
remove explicit managed component versions and duplicate ShardingSphere management
retain Open grpc/protobuf selection and existing external artifacts
retain source module topology/version and unrelated plugin configuration
```

- Verification contribution: 本Step下列命令验证 parent; BOM imports; local version owners; explicit component versions；需要非零目标测试数或明确静态断言。
- After this file: 规范七个 roots 和实际受影响的 component dependency declarations。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 8 — `MODIFY scripts/bump_cola_version.sh`

- Purpose: 同步所有新增 source root parent，保持升级和回滚。
- Symbols: verify_archetype_source_pom_versions; source parent version updates
- Repository evidence: 现有脚本备份所有 POM，但只改 source aggregator parent。
- Dependencies and consumers: 现有脚本备份所有 POM，但只改 source aggregator parent。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: verify_archetype_source_pom_versions; source parent version updates；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 只匹配 top.egon:egon-cola-archetypes-parent，禁止改 internal child parent version。
- Error and edge behavior: 任何不匹配版本 fail fast；使用既有 backup/restore。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 同步所有新增 source root parent，保持升级和回滚。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
for each source POM whose parent artifact is egon-cola-archetypes-parent:
  verify old parent version; replace only parent version
update egon-cola.version using existing path
validate; rollback on failure
```

- Verification contribution: 本Step下列命令验证 verify_archetype_source_pom_versions; source parent version updates；需要非零目标测试数或明确静态断言。
- After this file: 同步所有新增 source root parent，保持升级和回滚。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 9 — `MODIFY docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md`

同一责任文档：`MODIFY docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md`。

- Purpose: 将Maven实测优先级与必要全局桥接记录到已授权的Spec补齐和Plan中。
- Symbols: Spec §7.4.1; PLAN-CLAR-006; root Commons property/effective model gate。
- Repository evidence: baseline最小Bootparent+ComponentsBOM与Light有效模型均为3.17.0，不能声称BOM覆盖成功。
- Dependencies and consumers: Step1 root/BOM/source模型及最终审计。
- Why now: 在本Step提交前修正本轮新增的实现推断，后续Step顺序不变。
- Contract/signature changes: 已接受的3.20.0与archetype无本地版本目标不变，只补足Root全局Boot桥接。
- Input/output and state mapping: Components BOM版本与Root同名Boot属性必须相等，实际七模型必须为3.20.0。
- Error and edge behavior: 不更新版本目标、不隐瞒第一次模型失败；任何漂移阻断提交。
- Standards impact: `MC-SCOPE-001`, `MC-DEP-001`, `MC-TEST-001` — 证据修正与当前Step源代码一起审查。
- Literal rule enforcement: `Rule 5`, `Rule 11` — 无新工具或架构，明确构建模型覆盖的全局责任。
- Implementation pseudocode:

```text
record actual Maven inherited-management precedence and first 3.17.0 result
specify root Boot Commons property bridge to the accepted Core 3.20.0
require exact root/BOM equality plus seven resolved effective POM assertions
retain original failure evidence and unchanged Step ordering
```

- Verification contribution: Spec/Plan strict validators、root/BOM gate及Maven实际模型。
- After this file: 文档准确描述实际实现，不把第一次模型成功解析误报为目标版本通过。

- Validation working directory: repository root（逐root help:effective-pom/dependency:tree在相应POM目录）。
- Verification command:

```bash
python3 scripts/check-archetype-dependency-ownership.py
bash scripts/test-bump-cola-version.sh
./mvnw -B -ntp -N install -DskipTests
./mvnw -B -ntp -N -f egon-cola-components/egon-cola-components-bom/pom.xml install
./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml install -DskipTests
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml validate
# 对七个 source root 逐一执行 help:effective-pom 和 dependency:tree（源码 POM 所在目录）。
# 多模块 tree 所需 internal artifacts 先以 source reactor -DskipTests install 构建；此命令不作为测试通过证据。
```

- Expected result: 本Step所有目标测试/静态断言成功，目标测试实际执行数非零，`git diff --check`通过；七个 source root 继承具体发布 parent，Components BOM 导出 Commons 3.20.0，ShardingSphere/open versions 上移；保留 native 旧依赖直到其迁移 Step。
- Failure returns to: 本Step出现失败的精确文件；前序缺陷使用归属原Step的独立corrective commit，不重写历史。
- Completion criteria: Requirements对应证据、全部适用Manual Check和十条Literal Rule分别记录PASS或有证据N/A；无FAIL/BLOCKED/UNKNOWN。
- Rollback: 仅回退本Step源代码commit；保留用户worktree和不可变migration，generated通过脚本重建。
- Commit paths: `egon-cola-archetypes/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml`, `egon-cola-components/egon-cola-components-bom/pom.xml`, `egon-cola-components/pom.xml`, `pom.xml`, `scripts/bump_cola_version.sh`, `scripts/check-archetype-dependency-ownership.py`, `scripts/test-bump-cola-version.sh`, `docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md`, `docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md`

- Commit: `refactor(archetypes): centralize parent and dependency ownership`

### Step 2 — Add native unary Protobuf contracts

- Requirements: `REQ-001`, `REQ-008`, `REQ-011`
- Dependencies: `Step 1`
- Baseline state: Step 1 owner commit 已存在；业务 facade 与旧 providers 可继续编译。
- Observable outcome: 三个既有 contract owner 提供 31 个 unary methods、12 个 annotated Java interfaces 及 MapStruct 双向转换，业务 facade 签名不变。
- End state: 三个既有 contract owner 提供 31 个 unary methods、12 个 annotated Java interfaces 及 MapStruct 双向转换，业务 facade 签名不变。 后续 Step 内容保持 Pending。
- Test-first gate: `Required` — 首先运行本Step声明的missing behavior/owner/annotation/normalization测试，记录真实RED后最小GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/egon-cola-organization-facade/pom.xml`
- `MODIFY egon-cola-archetypes/egon-cola-evaluation-facade/pom.xml`

- Purpose: 建立 codegen/test 编译前置，不先实现合约行为。
- Symbols: protobuf-maven-plugin; native gRPC BOM; optional RPC/common/MapStruct; processors
- Repository evidence: RPC component 已用 protoc/grpc-java + os-maven-plugin；共享 facade artifact 已在 source reactor。
- Dependencies and consumers: RPC component 已用 protoc/grpc-java + os-maven-plugin；共享 facade artifact 已在 source reactor。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: protobuf-maven-plugin; native gRPC BOM; optional RPC/common/MapStruct; processors；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: codegen 1.75.0/4.32.0；shared facade 的 RPC/common/MapStruct 依赖按需求 optional，避免无谓传递。
- Error and edge behavior: 不能让 parent `${egon-cola.version}` unresolved；本文件完成后合约仍缺失。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 建立 codegen/test 编译前置，不先实现合约行为。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
bind compile/compile-custom protobuf goals
use native version properties and existing plugin convention
add required protobuf/grpc dependencies and annotation processor
keep old facade methods and existing tests buildable
```

- Verification contribution: 本Step下列命令验证 protobuf-maven-plugin; native gRPC BOM; optional RPC/common/MapStruct; processors；需要非零目标测试数或明确静态断言。
- After this file: 建立 codegen/test 编译前置，不先实现合约行为。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 2 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcContractTest.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java`
- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcContractTest.java`
- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcMappingTest.java`
- `CREATE egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcContractTest.java`
- `CREATE egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcMappingTest.java`

- Purpose: 定义 31-operation contract 与字段往返 RED。
- Symbols: validatesEveryNativeOperation; rejectsMissingAnnotation; rejectsMismatchedDescriptor; roundTripsNullTimeAndError
- Repository evidence: Spec §9 操作清单来自当前 10/10/11 个业务方法；前置依赖已可解析。
- Dependencies and consumers: Spec §9 操作清单来自当前 10/10/11 个业务方法；前置依赖已可解析。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: validatesEveryNativeOperation; rejectsMissingAnnotation; rejectsMismatchedDescriptor; roundTripsNullTimeAndError；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 先用 class-name/metadata 断言缺失生产合约，确保测试可编译；codegen 后补齐强类型 mapper assertions。
- Error and edge behavior: RED 是缺少指定生产 contract/mapper；不是语法或 Maven 环境错误。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 定义 31-operation contract 与字段往返 RED。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
assert named production RPC interfaces exist
validator.validate each interface; compare all method names and exact request/response descriptors
assert unary only and negative descriptor/annotation fixtures reject
round-trip optional null, zero, lists, all page fields, failure code/message/trace, nanos
```

- Verification contribution: 本Step下列命令验证 validatesEveryNativeOperation; rejectsMissingAnnotation; rejectsMismatchedDescriptor; roundTripsNullTimeAndError；需要非零目标测试数或明确静态断言。
- After this file: 定义 31-operation contract 与字段往返 RED。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 3 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/proto/organization_facade.proto`
- `CREATE egon-cola-archetypes/egon-cola-evaluation-facade/src/main/proto/evaluation_facade.proto`

- Purpose: 定义 native v1 wire，覆盖全清单而非原稿子集。
- Symbols: Course/User/Permission/SchoolClass/Grade/Role/Exam/Score services; RPC envelopes
- Repository evidence: Spec §7.4.2/§9/§10 提供 exact owner、操作及字段来源。
- Dependencies and consumers: Spec §7.4.2/§9/§10 提供 exact owner、操作及字段来源。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Course/User/Permission/SchoolClass/Grade/Role/Exam/Score services; RPC envelopes；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: optional int64/string 保留 null；日期 ISO；成功/失败/分页 envelope 完整。
- Error and edge behavior: 错误 data 不补默认成功；field tag 固定；不改任何 Open proto。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 定义 native v1 wire，覆盖全清单而非原稿子集。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```proto
syntax = "proto3"; java_multiple_files = true;
java_package = owner.facade.rpc.proto;
service methods = exact 31-operation Spec inventory;
responses = success/code/message/data/trace_id with fixed field numbers;
repeated fields preserve order; optional fields preserve absence
```

- Verification contribution: 本Step下列命令验证 Course/User/Permission/SchoolClass/Grade/Role/Exam/Score services; RPC envelopes；需要非零目标测试数或明确静态断言。
- After this file: 定义 native v1 wire，覆盖全清单而非原稿子集。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 4 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/CourseRpcService.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/SchoolClassRpcService.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/UserRpcService.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/PermissionRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/UserRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RoleRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/PermissionRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/GradeRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/SchoolClassRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/CourseRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ExamRpcService.java`
- `CREATE egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ScoreRpcService.java`

- Purpose: 补齐真实 RpcContractValidator 所需 Java interface。
- Symbols: @EgonRpcService(grpcClass); @EgonRpcMethod; @NotNull input
- Repository evidence: validator 要求 interface、完整 method annotations 和精确 Protobuf Message 类型。
- Dependencies and consumers: validator 要求 interface、完整 method annotations 和精确 Protobuf Message 类型。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: @EgonRpcService(grpcClass); @EgonRpcMethod; @NotNull input；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 既有 group/version，read idempotent=true，commands=false，retries=0。
- Error and edge behavior: 不改原 Java DTO facade；不添加 overloaded/streaming methods。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 补齐真实 RpcContractValidator 所需 Java interface。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
@EgonRpcService(grpcClass=CourseServiceGrpc.class, group=existingGroup, version="1.0.0", retries=0)
interface CourseRpcService {
 @EgonRpcMethod(name="GetCourse", idempotent=true)
 CourseRpcResponse getCourse(@NotNull GetCourseRpcRequest request);
}
expand every method exactly from Spec §9
```

- Verification contribution: 本Step下列命令验证 @EgonRpcService(grpcClass); @EgonRpcMethod; @NotNull input；需要非零目标测试数或明确静态断言。
- After this file: 补齐真实 RpcContractValidator 所需 Java interface。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 5 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/LightRpcConverter.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/OrganizationRpcConverter.java`
- `CREATE egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/EvaluationRpcConverter.java`

- Purpose: 共享双向 DTO/proto 转换并保留失败和时间精度。
- Symbols: BaseConverter generics; toTarget/toSource; per-operation wrapper mappings; presence/time/list mappings
- Repository evidence: BaseConverter<S,T> 提供双向合同；当前 business DTO 是 records / SingleResponse。
- Dependencies and consumers: BaseConverter<S,T> 提供双向合同；当前 business DTO 是 records / SingleResponse。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: BaseConverter generics; toTarget/toSource; per-operation wrapper mappings; presence/time/list mappings；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 所有字段依 Spec §10；MapStruct 使用 protobuf builders、presence/null checks、必要的集合/时间映射。
- Error and edge behavior: null、空集合、失败、page metadata、Instant/LocalDateTime 纳秒不得丢失；无 JSON/reflection/BeanUtils。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 共享双向 DTO/proto 转换并保留失败和时间精度。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
@Mapper(collectionMappingStrategy=ADDER_PREFERRED, nullValueCheckStrategy=ALWAYS)
interface LightRpcConverter extends BaseConverter<CreateCourseDTO, CreateCourseRpcRequest> {
 // explicit additional methods for every request/payload/envelope
 // ISO_LOCAL_DATE_TIME and ISO_INSTANT conversion preserve nanos/null
}
use MapStruct generation; inspect produced builders and round-trip tests
```

- Verification contribution: 本Step下列命令验证 BaseConverter generics; toTarget/toSource; per-operation wrapper mappings; presence/time/list mappings；需要非零目标测试数或明确静态断言。
- After this file: 共享双向 DTO/proto 转换并保留失败和时间精度。 已完成；只有全部组和门禁完成后可提交本Step。

- Validation working directory: repository root（逐root help:effective-pom/dependency:tree在相应POM目录）。
- Verification command:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -pl :egon-cola-source-light,:egon-cola-organization-facade,:egon-cola-evaluation-facade -am -Dtest='NativeRpcContractTest,NativeRpcMappingTest,NativeOrganizationRpcContractTest,NativeOrganizationRpcMappingTest,NativeEvaluationRpcContractTest,NativeEvaluationRpcMappingTest' -Dsurefire.failIfNoSpecifiedTests=false test
# 每个目标测试类必须有非零 tests-run；依赖模块无同名测试可跳过匹配，不可跳过目标测试。
```

- Expected result: 本Step所有目标测试/静态断言成功，目标测试实际执行数非零，`git diff --check`通过；三个既有 contract owner 提供 31 个 unary methods、12 个 annotated Java interfaces 及 MapStruct 双向转换，业务 facade 签名不变。
- Failure returns to: 本Step出现失败的精确文件；前序缺陷使用归属原Step的独立corrective commit，不重写历史。
- Completion criteria: Requirements对应证据、全部适用Manual Check和十条Literal Rule分别记录PASS或有证据N/A；无FAIL/BLOCKED/UNKNOWN。
- Rollback: 仅回退本Step源代码commit；保留用户worktree和不可变migration，generated通过脚本重建。
- Commit paths: `egon-cola-archetypes/egon-cola-evaluation-facade/pom.xml`, `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/CourseRpcService.java`, `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/EvaluationRpcConverter.java`, `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ExamRpcService.java`, `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ScoreRpcService.java`, `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/proto/evaluation_facade.proto`, `egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcContractTest.java`, `egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcMappingTest.java`, `egon-cola-archetypes/egon-cola-organization-facade/pom.xml`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/GradeRpcService.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/OrganizationRpcConverter.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/PermissionRpcService.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RoleRpcService.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/SchoolClassRpcService.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/UserRpcService.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/proto/organization_facade.proto`, `egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcContractTest.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcMappingTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/CourseRpcService.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/LightRpcConverter.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/PermissionRpcService.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/SchoolClassRpcService.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/UserRpcService.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcContractTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java`

- Commit: `feat(archetypes): define native protobuf rpc contracts`

### Step 3 — Migrate native light provider and DDC/API Doc wiring

- Requirements: `REQ-001`, `REQ-008`, `REQ-011`
- Dependencies: `Step 2`
- Baseline state: Light native contract/mapper 已提交；Dubbo providers 和 bootstrap 仍在原位。
- Observable outcome: Light 10 个操作经 native adapters；无 Dubbo/Nacos/直接 Springdoc，四 profile 与 Compose/README 一致，业务 HTTP 保持兼容。
- End state: Light 10 个操作经 native adapters；无 Dubbo/Nacos/直接 Springdoc，四 profile 与 Compose/README 一致，业务 HTTP 保持兼容。 后续 Step 内容保持 Pending。
- Test-first gate: `Required` — 首先运行本Step声明的missing behavior/owner/annotation/normalization测试，记录真实RED后最小GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/NativeRpcProviderTest.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfigurationTest.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeHttpCompatibilityTest.java`
- `CREATE scripts/check-native-archetype-boundaries.py`

- Purpose: 先建立 provider/config/HTTP 与 native 残留 RED。
- Symbols: exportsAllContracts; validatesRequests; preservesHttpRoutes; compareProfileKeys; native file/dependency scan
- Repository evidence: 当前 providers 是 Dubbo，runtime flags 使用 dubbo.*，native bootstrap 仍有 Nacos。
- Dependencies and consumers: 当前 providers 是 Dubbo，runtime flags 使用 dubbo.*，native bootstrap 仍有 Nacos。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: exportsAllContracts; validatesRequests; preservesHttpRoutes; compareProfileKeys; native file/dependency scan；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 测试不连接 DDC/Redis/HTTP，现有 fake facade + MockMvc/ApplicationContextRunner。
- Error and edge behavior: 必须证明 target tests 实际执行；无服务监听、无 runtime 启动。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 先建立 provider/config/HTTP 与 native 残留 RED。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
assert EgonRpcProvider and named bean metadata
invoke all 10 operations against facade mocks; reject invalid inputs before delegate
assert mapped failure and null response behavior
assert HTTP status/body and OAS paths unchanged
compare changed key set across base/dev/test/prod
scan native source/config/compose excluding target only
```

- Verification contribution: 本Step下列命令验证 exportsAllContracts; validatesRequests; preservesHttpRoutes; compareProfileKeys; native file/dependency scan；需要非零目标测试数或明确静态断言。
- After this file: 先建立 provider/config/HTTP 与 native 残留 RED。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 2 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/NativeRpcValidationGroup.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/RpcIdQuery.java`

- Purpose: 提供 Protobuf 无法直接承载的 Jakarta 校验边界。
- Symbols: NativeRpcValidationGroup; record RpcIdQuery(@NotNull @Positive Long id)
- Repository evidence: Light facade DTO 当前无 annotations；原查询/command 已要求合法 ID 与上下文。
- Dependencies and consumers: Light facade DTO 当前无 annotations；原查询/command 已要求合法 ID 与上下文。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: NativeRpcValidationGroup; record RpcIdQuery(@NotNull @Positive Long id)；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 仅 native group，不改变 HTTP 默认 validation group；简单 carrier 用 record。
- Error and edge behavior: 不存在的新业务规则不得引入；禁止自写 ConstraintValidator。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 提供 Protobuf 无法直接承载的 Jakarta 校验边界。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
interface NativeRpcValidationGroup {}
record RpcIdQuery(@NotNull @Positive Long id) {}
// Preserved mapping: 仅 native group，不改变 HTTP 默认 validation group；简单 carrier 用 record。
// Required failure assertion: 不存在的新业务规则不得引入；禁止自写 ConstraintValidator。
```

- Verification contribution: 本Step下列命令验证 NativeRpcValidationGroup; record RpcIdQuery(@NotNull @Positive Long id)；需要非零目标测试数或明确静态断言。
- After this file: 提供 Protobuf 无法直接承载的 Jakarta 校验边界。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 3 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CourseDTO.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateCourseDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateSchoolClassDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/SchoolClassDetailDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/package-info.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/AssignRoleDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/CreateUserDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/GrantPermissionDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/PermissionDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/PermissionDetailDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/UserDetailDTO.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/package-info.java`

- Purpose: 补齐 RPC-only DTO 约束并保留字段和默认 HTTP 语义。
- Symbols: @NotBlank/@NotNull/@Positive groups; existing record components
- Repository evidence: Light application validators 的 code/name/operator/requestId 与 ID 条件已核对。
- Dependencies and consumers: Light application validators 的 code/name/operator/requestId 与 ID 条件已核对。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: @NotBlank/@NotNull/@Positive groups; existing record components；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 只为实际输入字段添加 NativeRpcValidationGroup；输出 DTO 无需改动时保持。
- Error and edge behavior: 不增加业务字段，不改变构造器语义或默认 group。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 补齐 RPC-only DTO 约束并保留字段和默认 HTTP 语义。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
apply existing semantic constraints with groups=NativeRpcValidationGroup.class
retain record fields, ordering, compact constructors and JSON shape
validate DTO at RPC boundary using this group
```

- Verification contribution: 本Step下列命令验证 @NotBlank/@NotNull/@Positive groups; existing record components；需要非零目标测试数或明确静态断言。
- After this file: 补齐 RPC-only DTO 约束并保留字段和默认 HTTP 语义。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 4 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProvider.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProvider.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProvider.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProvider.java`

- Purpose: 将四个 provider 变为完整 native Adapter。
- Symbols: CourseRpcProvider; SchoolClassRpcProvider; UserRpcProvider; PermissionRpcProvider
- Repository evidence: 已有 facade delegate 与稳定 bean names；Step 2 mapper/contract 可复用。
- Dependencies and consumers: 已有 facade delegate 与稳定 bean names；Step 2 mapper/contract 可复用。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: CourseRpcProvider; SchoolClassRpcProvider; UserRpcProvider; PermissionRpcProvider；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: proto -> DTO validation -> original facade -> complete proto response；业务异常保留 code/message。
- Error and edge behavior: 只 catch 既有 business/validation failures；未知错误不转成功。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 将四个 provider 变为完整 native Adapter。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
@EgonRpcProvider @Component("courseRpcProvider") @Slf4j @RequiredArgsConstructor
class CourseRpcProvider implements CourseRpcService {
 @Qualifier("courseFacadeImpl") private final CourseFacade delegate;
 @Qualifier("lightRpcConverter") private final LightRpcConverter converter;
 @Qualifier("nativeRpcValidation") private final ValidationUtils validation;
 getCourse(request): validate RpcIdQuery -> delegate.getCourse -> converter result
 createCourse(request): map/validate native group -> delegate -> converter result
}
repeat exact existing 10 operations and failure envelopes
```

- Verification contribution: 本Step下列命令验证 CourseRpcProvider; SchoolClassRpcProvider; UserRpcProvider; PermissionRpcProvider；需要非零目标测试数或明确静态断言。
- After this file: 将四个 provider 变为完整 native Adapter。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 5 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfiguration.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: 显式接线 Converter/Validation 和兼容 HTTP chain。
- Symbols: lightRpcConverter; nativeRpcValidation; native validation exception mapper; applicationSecurityFilterChain
- Repository evidence: Core ValidationUtils/MapStruct Mappers 与 RpcProviderExceptionMapper SPI；platform webmvc 引入 security。
- Dependencies and consumers: Core ValidationUtils/MapStruct Mappers 与 RpcProviderExceptionMapper SPI；platform webmvc 引入 security。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: lightRpcConverter; nativeRpcValidation; native validation exception mapper; applicationSecurityFilterChain；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: named beans，final qualified injections；原 HTTP 不变；OpenAPI governance 由显式配置启用。
- Error and edge behavior: 避免全站意外 401、无授权真实 JWT 调用、参数异常映射 INTERNAL。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 显式接线 Converter/Validation 和兼容 HTTP chain。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
@Bean("lightRpcConverter") -> Mappers.getMapper(LightRpcConverter.class)
@Bean("nativeRpcValidation") -> ValidationUtils(defaultValidator)
@Bean named RpcProviderExceptionMapper -> ConstraintViolationException => INVALID_ARGUMENT
@Bean applicationSecurityFilterChain -> preserve current business HTTP access and CSRF behavior
```

- Verification contribution: 本Step下列命令验证 lightRpcConverter; nativeRpcValidation; native validation exception mapper; applicationSecurityFilterChain；需要非零目标测试数或明确静态断言。
- After this file: 显式接线 Converter/Validation 和兼容 HTTP chain。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 6 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/StudentManagementApplication.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/OpenApiConfig.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/package-info.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/package-info.java`

- Purpose: 完整切换 runtime 依赖和应用 bootstrap。
- Symbols: native RPC/DDC/OpenAPI dependencies; remove EnableDubbo; retain OpenAPI Info
- Repository evidence: 当前 POM 直接 Dubbo/Cloud/Nacos/Springdoc；app 使用 EnableDubbo。
- Dependencies and consumers: 当前 POM 直接 Dubbo/Cloud/Nacos/Springdoc；app 使用 EnableDubbo。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native RPC/DDC/OpenAPI dependencies; remove EnableDubbo; retain OpenAPI Info；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 添加已有 native starter，移除旧依赖和 annotations；public scans/HTTP metadata 不变。
- Error and edge behavior: 没有 Nacos/Dubbo fallback；不把 RPC 4.32 runtime 用 Open 3.25 版本覆盖。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 完整切换 runtime 依赖和应用 bootstrap。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
remove Dubbo/Cloud/Nacos/direct Springdoc dependencies
add BOM-managed RPC starter + DDC adapter + platform OpenAPI MVC
remove @EnableDubbo; preserve component scanning
name OpenAPI configuration/bean explicitly; retain Info and routes
```

- Verification contribution: 本Step下列命令验证 native RPC/DDC/OpenAPI dependencies; remove EnableDubbo; retain OpenAPI Info；需要非零目标测试数或明确静态断言。
- After this file: 完整切换 runtime 依赖和应用 bootstrap。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 7 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml`

- Purpose: 迁移身份并对齐四 profile 的真实 starter keys。
- Symbols: spring.application.name; egon.cola.component.rpc/ddc/gateway.openapi
- Repository evidence: 旧 app name 位于 bootstrap；真实 prefix 来自已读 properties。
- Dependencies and consumers: 旧 app name 位于 bootstrap；真实 prefix 来自已读 properties。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: spring.application.name; egon.cola.component.rpc/ddc/gateway.openapi；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: base/dev/test/prod 同 key，值可不同；test 关闭 server/registry/remote；不改变 datasource/Flyway location。
- Error and edge behavior: bootstrap 移除后不能丢 name；缺 target/credentials 依原 native starter fail-fast。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 迁移身份并对齐四 profile 的真实 starter keys。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```yaml
move non-Cloud bootstrap identity into application profiles
replace dubbo/logging namespaces with native RPC
bind actual DDC/RPC/OpenAPI properties in all profiles
test: no external/port lifecycle; dev/prod: explicit env settings
```

- Verification contribution: 本Step下列命令验证 spring.application.name; egon.cola.component.rpc/ddc/gateway.openapi；需要非零目标测试数或明确静态断言。
- After this file: 迁移身份并对齐四 profile 的真实 starter keys。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 8 — `DELETE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-dev.yml`

同一责任的逐文件顺序（全部显式归属本组）：

- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-prod.yml`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-test.yml`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap.yml`

- Purpose: 删除不再被加载的 Cloud/Nacos bootstrap。
- Symbols: bootstrap.yml and profile overrides
- Repository evidence: 有效 application identity 已由前组迁移。
- Dependencies and consumers: 有效 application identity 已由前组迁移。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: bootstrap.yml and profile overrides；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 不删除 datasource profile 或历史 SQL。
- Error and edge behavior: 扫描无 Nacos/Cloud 内容；不要通过删除测试掩盖配置遗漏。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 删除不再被加载的 Cloud/Nacos bootstrap。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
assert remaining useful keys were moved
delete native bootstrap files
run profile and application-name regressions
# Preserved mapping: 不删除 datasource profile 或历史 SQL。
# Required failure assertion: 扫描无 Nacos/Cloud 内容；不要通过删除测试掩盖配置遗漏。
```

- Verification contribution: 本Step下列命令验证 bootstrap.yml and profile overrides；需要非零目标测试数或明确静态断言。
- After this file: 删除不再被加载的 Cloud/Nacos bootstrap。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 9 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProviderTest.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProviderTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProviderTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProviderTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/StudentManagementApplicationTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/RuntimeConfigurationTest.java`

- Purpose: 更新现有测试到 native 协议，保留业务断言。
- Symbols: native request/response assertions; disabled test lifecycle
- Repository evidence: 原 provider tests 调用 DTO 方法；原 app/runtime tests 断言 Dubbo。
- Dependencies and consumers: 原 provider tests 调用 DTO 方法；原 app/runtime tests 断言 Dubbo。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native request/response assertions; disabled test lifecycle；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 语义断言保持，transport fixture 改为 generated requests。
- Error and edge behavior: 不删测试覆盖，不把服务监听带入 test profile。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 更新现有测试到 native 协议，保留业务断言。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
replace old transport fixtures/assertions with native descriptors and same business expectations
assert test profile disables all network runtime hooks
// Preserved mapping: 语义断言保持，transport fixture 改为 generated requests。
// Required failure assertion: 不删测试覆盖，不把服务监听带入 test profile。
```

- Verification contribution: 本Step下列命令验证 native request/response assertions; disabled test lifecycle；需要非零目标测试数或明确静态断言。
- After this file: 更新现有测试到 native 协议，保留业务断言。 已完成；只有全部组和门禁完成后可提交本Step。

本组补充 MVC 切片文件（仅在 `@ContextConfiguration` 中加入同一应用 `NativeHttpSecurityConfiguration`，不删除 filter 或放宽业务断言）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/CourseControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/PermissionControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/RoleControllerTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/UserControllerTest.java`

#### File 10 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.prod.yaml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.prod.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.prod.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.example`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.prod.example`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/README.md`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/README.md`

- Purpose: 同步 deployment/env/README 的 native 边界。
- Symbols: native RPC port; external DDC env; remove Nacos service/volumes
- Repository evidence: 6 Compose 和2 env 共用当前运行参数，数据库/Redis/MQ 仍被业务使用。
- Dependencies and consumers: 6 Compose 和2 env 共用当前运行参数，数据库/Redis/MQ 仍被业务使用。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native RPC port; external DDC env; remove Nacos service/volumes；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 仅删除 Nacos 专用服务、volume/dependency/env，保留其他数据卷，DDC endpoint 外部提供。
- Error and edge behavior: 不运行容器、不增加未经发布的 DDC 镜像；源码中的地址仅为配置。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 同步 deployment/env/README 的 native 边界。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```yaml
replace native transport env/port examples
remove Nacos-only service/depends_on/volumes
retain database/Redis/MQ identities and volumes
write operator-configured DDC/TLS/credential prerequisites
```

- Verification contribution: 本Step下列命令验证 native RPC port; external DDC env; remove Nacos service/volumes；需要非零目标测试数或明确静态断言。
- After this file: 同步 deployment/env/README 的 native 边界。 已完成；只有全部组和门禁完成后可提交本Step。

#### 配置解密补充文件组 — 用户确认的 `PLAN-CLAR-007`

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`

- Purpose: 移除已退役 Cloud bootstrap 的编译和测试依赖，保持配置解密行为。
- Symbols: ConfigDecryptEnvironmentPostProcessor.getOrder; ConfigDecryptEnvironmentPostProcessorTest
- Repository evidence: 原实现和测试引用 BootstrapConfigFileApplicationListener.DEFAULT_ORDER，native 移除 Cloud 后不能编译。
- Dependencies and consumers: 本 Step POM/配置迁移；Spring factories 的既有 EnvironmentPostProcessor。
- Why now: 只在所属 Step 移除 Cloud 后改为 Boot Config Data 加载顺序。
- Contract/signature changes: 无公共签名变更；内部顺序常量迁移。
- Input/output and state mapping: 保留算法、密钥来源/优先级、占位符覆盖、property source 优先级和密钥清零。
- Error and edge behavior: 保留缺失密钥失败、configtree、显式导入配置和明文覆盖测试。
- Standards impact: MC-SCOPE-001 / MC-CONFIG-001 / MC-TEST-001；框架实例化构造器保持原状，不新增业务载体。
- Literal rule enforcement: Rules 1/2/3/4/5/6/7/9/10/11 逐项复核；Boot 回调保持原有 infrastructure 分类。
- Implementation pseudocode:

```java
// ConfigDecryptEnvironmentPostProcessor.getOrder:
return ConfigDataEnvironmentPostProcessor.ORDER + 1;
// Test: replace automatic bootstrap loading with explicit spring.config.import.
// Retain Config Data, configtree, key precedence, missing-key and placeholder assertions.
```

- Verification contribution: 本 Step 额外执行 ConfigDecryptEnvironmentPostProcessorTest 及配置加解密相关测试，使用临时文件和 WebApplicationType.NONE。
- After this file: 本 Step 所有门禁通过后按既有单独 commit 边界提交。

- Validation working directory: repository root（逐root help:effective-pom/dependency:tree在相应POM目录）。
- Verification command:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -pl :egon-cola-source-light -am -Dtest='NativeRpc*Test,*RpcProviderTest,RuntimeConfigurationTest,StudentManagementApplicationTest' -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml test
python3 scripts/check-native-archetype-boundaries.py light
# 还需核对 dependency:tree 和 OpenAPI/HTTP compatibility assertions。
```

- Expected result: 本Step所有目标测试/静态断言成功，目标测试实际执行数非零，`git diff --check`通过；Light 10 个操作经 native adapters；无 Dubbo/Nacos/直接 Springdoc，四 profile 与 Compose/README 一致，业务 HTTP 保持兼容。
- Failure returns to: 本Step出现失败的精确文件；前序缺陷使用归属原Step的独立corrective commit，不重写历史。
- Completion criteria: Requirements对应证据、全部适用Manual Check和十条Literal Rule分别记录PASS或有证据N/A；无FAIL/BLOCKED/UNKNOWN。
- Rollback: 仅回退本Step源代码commit；保留用户worktree和不可变migration，generated通过脚本重建。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/CourseControllerTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassControllerTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/PermissionControllerTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/RoleControllerTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/UserControllerTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.example`, `egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.prod.example`, `egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/NativeRpcValidationGroup.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/RpcIdQuery.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CourseDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateCourseDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateSchoolClassDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/SchoolClassDetailDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/AssignRoleDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/CreateUserDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/GrantPermissionDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/PermissionDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/PermissionDetailDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/UserDetailDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/StudentManagementApplication.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeHttpSecurityConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/OpenApiConfig.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/NativeRpcProviderTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProviderTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProviderTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProviderTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProviderTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/StudentManagementApplicationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeHttpCompatibilityTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/RuntimeConfigurationTest.java`, `scripts/check-native-archetype-boundaries.py`

- Commit: `refactor(archetype-light): migrate native rpc and ddc wiring`

### Step 4 — Migrate native service/web RPC, DDC and API Doc

- Requirements: `REQ-001`, `REQ-008`, `REQ-011`
- Dependencies: `Step 3`
- Baseline state: Step 2 shared contracts 与 Step 3 native 接线模式已提交；service/web 旧 transport 尚保留。
- Observable outcome: Service 11 个 provider 操作、Web 10 个 provider 操作及双向 domain clients 均为 native，metadata/错误/业务 HTTP 保持。
- End state: Service 11 个 provider 操作、Web 10 个 provider 操作及双向 domain clients 均为 native，metadata/错误/业务 HTTP 保持。 后续 Step 内容保持 Pending。
- Test-first gate: `Required` — 首先运行本Step声明的missing behavior/owner/annotation/normalization测试，记录真实RED后最小GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/NativeServiceRpcProviderTest.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcProviderTest.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcContextTest.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClientTest.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClientTest.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/NativeServiceConfigurationTest.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeWebConfigurationTest.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeHttpCompatibilityTest.java`

- Purpose: 为 shared provider/client/context/config 建立完整 RED。
- Symbols: native contracts exported; direct clients; metadata lifecycle; failure categories; key parity
- Repository evidence: 现有 Dubbo tests/port results、OrganizationFacadeSupport 和 profile 配置。
- Dependencies and consumers: 现有 Dubbo tests/port results、OrganizationFacadeSupport 和 profile 配置。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native contracts exported; direct clients; metadata lifecycle; failure categories; key parity；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 使用 mocks、in-process RPC、fake DDC directories 和 MockMvc；覆盖完整31清单中对应21项。
- Error and edge behavior: 空响应/业务失败不得伪成功；metadata 不泄漏或丢失；没有 external runtime。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 为 shared provider/client/context/config 建立完整 RED。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
assert native export metadata before production replacement
map all shared unary inputs/results against existing facades
assert timeout/unavailable/validation/contract failures map existing domain categories
assert actor/roles/trace/idempotency and context cleanup
assert profile and HTTP invariants
```

- Verification contribution: 本Step下列命令验证 native contracts exported; direct clients; metadata lifecycle; failure categories; key parity；需要非零目标测试数或明确静态断言。
- After this file: 为 shared provider/client/context/config 建立完整 RED。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 2 — `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcIdQuery.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcSchoolClassQuery.java`

- Purpose: 为 organization primitive facade 参数复用 Jakarta constraints。
- Symbols: record RpcIdQuery; record RpcSchoolClassQuery
- Repository evidence: 共享 facade getUser/getGrade/getSchoolClass 参数具有 NotNull/Positive。
- Dependencies and consumers: 共享 facade getUser/getGrade/getSchoolClass 参数具有 NotNull/Positive。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: record RpcIdQuery; record RpcSchoolClassQuery；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: transport criteria records；不引入 persistence 或业务层。
- Error and edge behavior: 缺失/非正 ID 在 delegate 前拒绝。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 为 organization primitive facade 参数复用 Jakarta constraints。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
record RpcIdQuery(@NotNull @Positive Long id) {}
record RpcSchoolClassQuery(@NotNull @Positive Long gradeId, @NotNull @Positive Long schoolClassId) {}
// Preserved mapping: transport criteria records；不引入 persistence 或业务层。
// Required failure assertion: 缺失/非正 ID 在 delegate 前拒绝。
```

- Verification contribution: 本Step下列命令验证 record RpcIdQuery; record RpcSchoolClassQuery；需要非零目标测试数或明确静态断言。
- After this file: 为 organization primitive facade 参数复用 Jakarta constraints。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 3 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/rpc/CourseRpcProvider.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ExamRpcProvider.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ScoreRpcProvider.java`

- Purpose: Service native provider Adapter 保留既有 facade 业务实现。
- Symbols: Course/Exam/ScoreRpcProvider; typed DTO and envelope maps
- Repository evidence: 共享 evaluation contracts Step 2；既有 FacadeImpl/Manage/GlobalFacadeExceptionHandler 不重写。
- Dependencies and consumers: 共享 evaluation contracts Step 2；既有 FacadeImpl/Manage/GlobalFacadeExceptionHandler 不重写。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Course/Exam/ScoreRpcProvider; typed DTO and envelope maps；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: proto -> validated original Request -> original Facade -> SingleResponse -> proto，11 operations。
- Error and edge behavior: validation / business result / unknown transport failure 按 Spec 区分。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — Service native provider Adapter 保留既有 facade 业务实现。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
@EgonRpcProvider @Component(explicitName) @Slf4j @RequiredArgsConstructor
qualified final facade/converter/ValidationUtils
map validated request; delegate exact existing method; map complete SingleResponse
no duplicated Manage orchestration
```

- Verification contribution: 本Step下列命令验证 Course/Exam/ScoreRpcProvider; typed DTO and envelope maps；需要非零目标测试数或明确静态断言。
- After this file: Service native provider Adapter 保留既有 facade 业务实现。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 4 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/facade/impl/CourseFacadeImpl.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImpl.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ScoreFacadeImpl.java`

- Purpose: 保留 Service 原业务实现，移除 Dubbo export 并补齐受影响 bean 声明。
- Symbols: Component names; Slf4j; dependency Qualifiers
- Repository evidence: 现有 class 直接带 DubboService 且部分依赖无 Qualifier。
- Dependencies and consumers: 现有 class 直接带 DubboService 且部分依赖无 Qualifier。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Component names; Slf4j; dependency Qualifiers；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 原方法体/业务结果/validation handler 保持；provider 在前组单独负责 transport。
- Error and edge behavior: 不复制业务逻辑、不改下层 manager/DAO。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 保留 Service 原业务实现，移除 Dubbo export 并补齐受影响 bean 声明。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
replace @DubboService with named @Component
add @Slf4j and missing @Qualifier to final injected fields
retain RequiredArgsConstructor and original facade signatures/behavior
```

- Verification contribution: 本Step下列命令验证 Component names; Slf4j; dependency Qualifiers；需要非零目标测试数或明确静态断言。
- After this file: 保留 Service 原业务实现，移除 Dubbo export 并补齐受影响 bean 声明。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 5 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/teaching/rpc/SchoolClassRpcProvider.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/user/rpc/UserRpcProvider.java`

- Purpose: 用既有两个 Web provider 文件导出五个 native contracts。
- Symbols: UserRpcProvider implements User/Role/PermissionRpcService; SchoolClassRpcProvider implements Grade/SchoolClassRpcService
- Repository evidence: 当前两个 configuration 通过 ServiceBean 暴露五个 facade。
- Dependencies and consumers: 当前两个 configuration 通过 ServiceBean 暴露五个 facade。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: UserRpcProvider implements User/Role/PermissionRpcService; SchoolClassRpcProvider implements Grade/SchoolClassRpcService；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 全部10操作继续委托既有 named facade beans，converter共享。
- Error and edge behavior: 失去任何原 export 均失败；不保留 ServiceBean fallback。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 用既有两个 Web provider 文件导出五个 native contracts。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
replace ServiceBean configuration with named EgonRpcProvider adapters
implement all five annotated interfaces and their 10 operations
validate DTO/scalar IDs; preserve OrganizationFacadeException code/message/traceId
```

- Verification contribution: 本Step下列命令验证 UserRpcProvider implements User/Role/PermissionRpcService; SchoolClassRpcProvider implements Grade/SchoolClassRpcService；需要非零目标测试数或明确静态断言。
- After this file: 用既有两个 Web provider 文件导出五个 native contracts。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 6 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/config/NativeRpcConfiguration.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/config/NativeRpcConfiguration.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcConfiguration.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcProperties.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationDirectoryConverter.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcConfiguration.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcProperties.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationQueryConverter.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClient.java`
- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClient.java`

- Purpose: 复用现有 proxy/strategy APIs 实现构造注入的 DIRECT clients。
- Symbols: named proxy beans; typed target records; NativeOrganizationDirectoryClient; NativeEvaluationQueryClient; BaseConverter domain maps
- Repository evidence: RpcReferenceDefinition/StrategyFactory/ConsumerProxyFactory 已存在；EgonRpcReference 仅字段后注入不适合 final constructor。
- Dependencies and consumers: RpcReferenceDefinition/StrategyFactory/ConsumerProxyFactory 已存在；EgonRpcReference 仅字段后注入不适合 final constructor。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: named proxy beans; typed target records; NativeOrganizationDirectoryClient; NativeEvaluationQueryClient; BaseConverter domain maps；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: target biz/app/current-env + group/version；timeout<=consumer ceiling，retries=0，FAIL_CLOSED；DTO->既有 domain record。
- Error and edge behavior: 禁止写 final field、禁止 generic factory 再造、禁止 native/open fallback；strategies 由现有 factory 关闭。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 复用现有 proxy/strategy APIs 实现构造注入的 DIRECT clients。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
validate target properties
descriptor = RpcContractValidator.validate(contract)
definition = DIRECT identity/query + fixed per-method policy (bounded timeout, retries=0, FAIL_CLOSED)
proxy = RpcConsumerProxyFactory.create(descriptor, definition, RpcReferenceStrategyFactory.create(definition))
@Bean explicit proxy/converter names
@RequiredArgsConstructor clients with final @Qualifier fields
map proto result to facade DTO then MapStruct/BaseConverter to domain record; retain failure categories
```

- Verification contribution: 本Step下列命令验证 named proxy beans; typed target records; NativeOrganizationDirectoryClient; NativeEvaluationQueryClient; BaseConverter domain maps；需要非零目标测试数或明确静态断言。
- After this file: 复用现有 proxy/strategy APIs 实现构造注入的 DIRECT clients。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 7 — `DELETE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClient.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClient.java`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClientTest.java`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClientTest.java`

- Purpose: 移除被同职责 native client/test 替换的旧文件。
- Symbols: old Dubbo clients and tests
- Repository evidence: 所有消费者依赖 domain port，native replacement 已由前组提供。
- Dependencies and consumers: 所有消费者依赖 domain port，native replacement 已由前组提供。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: old Dubbo clients and tests；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 等价重命名和 transport 切换；domain port 不改。
- Error and edge behavior: 先保留/迁移原测试语义再删旧文件，扫描无旧 class reference。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 移除被同职责 native client/test 替换的旧文件。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
verify Native*Client covers old public port methods and failure assertions
delete replaced Dubbo files
assert no old client/test reference remains
```

- Verification contribution: 本Step下列命令验证 old Dubbo clients and tests；需要非零目标测试数或明确静态断言。
- After this file: 移除被同职责 native client/test 替换的旧文件。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 8 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationClientFailureMapper.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationClientFailureMapper.java`

- Purpose: 保留 business 分类，替换 transport exception 分类。
- Symbols: EgonRpcException codes; existing domain failure enums
- Repository evidence: 原 map 使用 RpcException.isTimeout，否则 UNAVAILABLE。
- Dependencies and consumers: 原 map 使用 RpcException.isTimeout，否则 UNAVAILABLE。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: EgonRpcException codes; existing domain failure enums；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: RPC_DEADLINE_EXCEEDED -> TIMEOUT；unavailable/service lookup -> UNAVAILABLE；descriptor/method invalid -> CONTRACT_INCOMPATIBLE；业务 code 原样。
- Error and edge behavior: 不把错误分类成成功，不扩大 retry。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 保留 business 分类，替换 transport exception 分类。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
replace Dubbo exception branch with EgonRpcErrorCode classification
retain business code and existing failure wrappers
log safe operation/category only at native client boundary
```

- Verification contribution: 本Step下列命令验证 EgonRpcException codes; existing domain failure enums；需要非零目标测试数或明确静态断言。
- After this file: 保留 business 分类，替换 transport exception 分类。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 9 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextDTO.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextInterceptor.java`

- Purpose: 通过现有 ServerInterceptor 扩展承载原 organization metadata。
- Symbols: Context.Key<OrganizationRpcContextDTO>; interceptCall
- Repository evidence: OrganizationFacadeSupport 原读取 RpcContext 四个 attachment；现有 RPC factory 收集 ServerInterceptor beans。
- Dependencies and consumers: OrganizationFacadeSupport 原读取 RpcContext 四个 attachment；现有 RPC factory 收集 ServerInterceptor beans。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Context.Key<OrganizationRpcContextDTO>; interceptCall；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 同名 actor/roles/trace/idempotency header -> immutable context record；gRPC Context 管理生命周期。
- Error and edge behavior: 不新增权限规则、默认 SYSTEM 语义不扩大；并发/异常不泄漏 context。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 通过现有 ServerInterceptor 扩展承载原 organization metadata。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
record OrganizationRpcContextDTO(actorId, actorRoles, traceId, idempotencyKey)
@Component("organizationRpcContextInterceptor") @Slf4j @RequiredArgsConstructor
interceptCall: read existing metadata keys; Contexts.interceptCall with scoped record
no custom thread-local lifecycle
```

- Verification contribution: 本Step下列命令验证 Context.Key<OrganizationRpcContextDTO>; interceptCall；需要非零目标测试数或明确静态断言。
- After this file: 通过现有 ServerInterceptor 扩展承载原 organization metadata。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 10 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationFacadeSupport.java`

- Purpose: 迁移 attachment 来源并保留上下文优先与清理。
- Symbols: attachment; requestId; context; invoke
- Repository evidence: 既有 context holder / finally clear / OrganizationFacadeException。
- Dependencies and consumers: 既有 context holder / finally clear / OrganizationFacadeException。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: attachment; requestId; context; invoke；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 原 HTTP 已设置的上下文优先；RPC 使用 scoped context 与原默认值，trace 可回退 RpcInvocationMetadata。
- Error and edge behavior: 不丢失 idempotency-key，不改变事务或 UUID technical request id fallback。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 迁移 attachment 来源并保留上下文优先与清理。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
replace RpcContext attachment read with OrganizationRpcContextDTO scoped values
retain current holder precedence, default values, business exception and finally clear
// Preserved mapping: 原 HTTP 已设置的上下文优先；RPC 使用 scoped context 与原默认值，trace 可回退 RpcInvocationMetadata。
// Required failure assertion: 不丢失 idempotency-key，不改变事务或 UUID technical request id fallback。
```

- Verification contribution: 本Step下列命令验证 attachment; requestId; context; invoke；需要非零目标测试数或明确静态断言。
- After this file: 迁移 attachment 来源并保留上下文优先与清理。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 11 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/NativeHttpSecurityConfiguration.java`

- Purpose: 保持引入 platform OpenAPI 后的原业务 HTTP 行为。
- Symbols: applicationSecurityFilterChain
- Repository evidence: platform MVC 提供 security；旧业务路径无新增认证要求。
- Dependencies and consumers: platform MVC 提供 security；旧业务路径无新增认证要求。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: applicationSecurityFilterChain；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 与 Light 同一配置语义，无真实 JWT 请求。
- Error and edge behavior: 意外401/CSRF拒绝是回归。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 保持引入 platform OpenAPI 后的原业务 HTTP 行为。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
named application SecurityFilterChain preserves original business access
leave explicitly enabled document governance to platform chain
// Preserved mapping: 与 Light 同一配置语义，无真实 JWT 请求。
// Required failure assertion: 意外401/CSRF拒绝是回归。
```

- Verification contribution: 本Step下列命令验证 applicationSecurityFilterChain；需要非零目标测试数或明确静态断言。
- After this file: 保持引入 platform OpenAPI 后的原业务 HTTP 行为。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 12 — `CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/NativeHttpSecurityConfiguration.java`

- Purpose: 保持引入 platform OpenAPI 后的原业务 HTTP 行为。
- Symbols: applicationSecurityFilterChain
- Repository evidence: platform MVC 提供 security；旧业务路径无新增认证要求。
- Dependencies and consumers: platform MVC 提供 security；旧业务路径无新增认证要求。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: applicationSecurityFilterChain；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 与 Light 同一配置语义，无真实 JWT 请求。
- Error and edge behavior: 意外401/CSRF拒绝是回归。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 保持引入 platform OpenAPI 后的原业务 HTTP 行为。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
named application SecurityFilterChain preserves original business access
leave explicitly enabled document governance to platform chain
// Preserved mapping: 与 Light 同一配置语义，无真实 JWT 请求。
// Required failure assertion: 意外401/CSRF拒绝是回归。
```

- Verification contribution: 本Step下列命令验证 applicationSecurityFilterChain；需要非零目标测试数或明确静态断言。
- After this file: 保持引入 platform OpenAPI 后的原业务 HTTP 行为。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 13 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/EvaluationServiceApplication.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/OrganizationApplication.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/OrganizationSwaggerConfig.java`

- Purpose: 在相关 adapter/infrastructure/starter 同步替换依赖与 bootstrap。
- Symbols: native starter deps; remove EnableDubbo; shared facade native version selection
- Repository evidence: Dubbo 与直接 Springdoc 当前在 child POM；Nacos 在 starter POM。
- Dependencies and consumers: Dubbo 与直接 Springdoc 当前在 child POM；Nacos 在 starter POM。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native starter deps; remove EnableDubbo; shared facade native version selection；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 组件版本来自 BOM；业务模块和共享 facade artifact 不改；保留 OpenAPI Info。
- Error and edge behavior: 无 undeclared production changes；未需要的 child POM 不产生 diff。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 在相关 adapter/infrastructure/starter 同步替换依赖与 bootstrap。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
remove native Dubbo/Cloud/Nacos/direct Springdoc in actual consumer modules
add RPC/DDC/platform OpenAPI MVC and required native BOM selection
remove EnableDubbo; retain application/component scan
name touched configuration beans
```

- Verification contribution: 本Step下列命令验证 native starter deps; remove EnableDubbo; shared facade native version selection；需要非零目标测试数或明确静态断言。
- After this file: 在相关 adapter/infrastructure/starter 同步替换依赖与 bootstrap。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 14 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml`

- Purpose: 同步四 profiles 的真实 native settings。
- Symbols: application identity; native RPC/DDC/OpenAPI; typed direct targets
- Repository evidence: bootstrap identity 和旧 app integration group/version 已核对。
- Dependencies and consumers: bootstrap identity 和旧 app integration group/version 已核对。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: application identity; native RPC/DDC/OpenAPI; typed direct targets；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 保留 datasource/Flyway/MQ/Redis键与值，新增基础设施 key 四环境对齐。
- Error and edge behavior: test 不创建 server/client/registry；dev/prod fail-fast 配置边界明确。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 同步四 profiles 的真实 native settings。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```yaml
move application name from bootstrap
bind exact native prefixes and target properties
replace Dubbo logger/settings; compare profile core-key sets
```

- Verification contribution: 本Step下列命令验证 application identity; native RPC/DDC/OpenAPI; typed direct targets；需要非零目标测试数或明确静态断言。
- After this file: 同步四 profiles 的真实 native settings。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 15 — `DELETE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-dev.yml`

同一责任的逐文件顺序（全部显式归属本组）：

- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-prod.yml`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-test.yml`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap.yml`

- Purpose: 删除迁移完成的 native Cloud bootstrap。
- Symbols: bootstrap variants
- Repository evidence: 上组已迁移 application identity。
- Dependencies and consumers: 上组已迁移 application identity。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: bootstrap variants；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 不动其他 resources。
- Error and edge behavior: 不能遗漏仍有效键。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 删除迁移完成的 native Cloud bootstrap。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
verify remaining useful keys moved; delete Cloud bootstrap files
# Preserved mapping: 不动其他 resources。
# Required failure assertion: 不能遗漏仍有效键。
```

- Verification contribution: 本Step下列命令验证 bootstrap variants；需要非零目标测试数或明确静态断言。
- After this file: 删除迁移完成的 native Cloud bootstrap。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 16 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml`

- Purpose: 同步四 profiles 的真实 native settings。
- Symbols: application identity; native RPC/DDC/OpenAPI; typed direct targets
- Repository evidence: bootstrap identity 和旧 app integration group/version 已核对。
- Dependencies and consumers: bootstrap identity 和旧 app integration group/version 已核对。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: application identity; native RPC/DDC/OpenAPI; typed direct targets；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 保留 datasource/Flyway/MQ/Redis键与值，新增基础设施 key 四环境对齐。
- Error and edge behavior: test 不创建 server/client/registry；dev/prod fail-fast 配置边界明确。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 同步四 profiles 的真实 native settings。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```yaml
move application name from bootstrap
bind exact native prefixes and target properties
replace Dubbo logger/settings; compare profile core-key sets
```

- Verification contribution: 本Step下列命令验证 application identity; native RPC/DDC/OpenAPI; typed direct targets；需要非零目标测试数或明确静态断言。
- After this file: 同步四 profiles 的真实 native settings。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 17 — `DELETE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-dev.yml`

同一责任的逐文件顺序（全部显式归属本组）：

- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-prod.yml`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-test.yml`
- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap.yml`

- Purpose: 删除迁移完成的 native Cloud bootstrap。
- Symbols: bootstrap variants
- Repository evidence: 上组已迁移 application identity。
- Dependencies and consumers: 上组已迁移 application identity。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: bootstrap variants；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 不动其他 resources。
- Error and edge behavior: 不能遗漏仍有效键。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 删除迁移完成的 native Cloud bootstrap。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
verify remaining useful keys moved; delete Cloud bootstrap files
# Preserved mapping: 不动其他 resources。
# Required failure assertion: 不能遗漏仍有效键。
```

- Verification contribution: 本Step下列命令验证 bootstrap variants；需要非零目标测试数或明确静态断言。
- After this file: 删除迁移完成的 native Cloud bootstrap。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 18 — `DELETE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/EvaluationDubboTripleIntegrationTest.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `DELETE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/OrganizationDubboProviderConfigurationTest.java`

- Purpose: 以新 native provider/in-process tests 接替旧 Dubbo-only fixture。
- Symbols: replaced native transport fixtures
- Repository evidence: 本 Step 第一组 tests 保留所有业务和失败语义，并覆盖完整21操作。
- Dependencies and consumers: 本 Step 第一组 tests 保留所有业务和失败语义，并覆盖完整21操作。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: replaced native transport fixtures；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: Open 的 Triple tests 原样保留。
- Error and edge behavior: 不通过删除行为覆盖来让测试变绿。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 以新 native provider/in-process tests 接替旧 Dubbo-only fixture。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
compare old business assertions against new native tests
remove replaced native-only Dubbo test files
# Preserved mapping: Open 的 Triple tests 原样保留。
# Required failure assertion: 不通过删除行为覆盖来让测试变绿。
```

- Verification contribution: 本Step下列命令验证 replaced native transport fixtures；需要非零目标测试数或明确静态断言。
- After this file: 以新 native provider/in-process tests 接替旧 Dubbo-only fixture。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 19 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationDataSourceModeTest.java`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationExternalFreeContextTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationApplicationTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationDataSourceModeTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/package-info.java`

- Purpose: 将原 context/DB-mode 测试的 transport 断言迁移。
- Symbols: test profile disabled hooks; no old client beans
- Repository evidence: 原测试包含 dubbo.protocol/provider properties。
- Dependencies and consumers: 原测试包含 dubbo.protocol/provider properties。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: test profile disabled hooks; no old client beans；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 原 datasource和业务 assertions 不改。
- Error and edge behavior: 任何DB schema/migration修改越界。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 将原 context/DB-mode 测试的 transport 断言迁移。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```java
replace only old transport properties/names
retain datasource and business assertions
run all affected module tests
// Preserved mapping: 原 datasource和业务 assertions 不改。
// Required failure assertion: 任何DB schema/migration修改越界。
```

- Verification contribution: 本Step下列命令验证 test profile disabled hooks; no old client beans；需要非零目标测试数或明确静态断言。
- After this file: 将原 context/DB-mode 测试的 transport 断言迁移。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 20 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.prod.yaml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.prod.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.prod.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.example`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.prod.example`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/README.md`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/README.md`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.prod.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.prod.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.prod.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.yaml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.example`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.prod.example`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/README.md`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/README.md`

- Purpose: 同步两个 native 产品的部署与使用文档。
- Symbols: native port/env; external DDC; profile boundary
- Repository evidence: 各6Compose/2env当前含Nacos。
- Dependencies and consumers: 各6Compose/2env当前含Nacos。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native port/env; external DDC; profile boundary；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 只移除旧基础设施专属项，保留原数据卷和数据库/MQ/Redis。
- Error and edge behavior: 不启动/连接基础设施，不引入未发布镜像。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 同步两个 native 产品的部署与使用文档。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```yaml
replace Dubbo port/env and remove Nacos-only services/volumes
wire operator-provided DDC target/TLS/credentials
document original HTTP and new native wire boundary
```

- Verification contribution: 本Step下列命令验证 native port/env; external DDC; profile boundary；需要非零目标测试数或明确静态断言。
- After this file: 同步两个 native 产品的部署与使用文档。 已完成；只有全部组和门禁完成后可提交本Step。

#### 配置解密补充文件组 — 用户确认的 `PLAN-CLAR-007`

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`

- Purpose: 移除已退役 Cloud bootstrap 的编译和测试依赖，保持配置解密行为。
- Symbols: ConfigDecryptEnvironmentPostProcessor.getOrder; ConfigDecryptEnvironmentPostProcessorTest
- Repository evidence: 原实现和测试引用 BootstrapConfigFileApplicationListener.DEFAULT_ORDER，native 移除 Cloud 后不能编译。
- Dependencies and consumers: 本 Step POM/配置迁移；Spring factories 的既有 EnvironmentPostProcessor。
- Why now: 只在所属 Step 移除 Cloud 后改为 Boot Config Data 加载顺序。
- Contract/signature changes: 无公共签名变更；内部顺序常量迁移。
- Input/output and state mapping: 保留算法、密钥来源/优先级、占位符覆盖、property source 优先级和密钥清零。
- Error and edge behavior: 保留缺失密钥失败、configtree、显式导入配置和明文覆盖测试。
- Standards impact: MC-SCOPE-001 / MC-CONFIG-001 / MC-TEST-001；框架实例化构造器保持原状，不新增业务载体。
- Literal rule enforcement: Rules 1/2/3/4/5/6/7/9/10/11 逐项复核；Boot 回调保持原有 infrastructure 分类。
- Implementation pseudocode:

```java
// ConfigDecryptEnvironmentPostProcessor.getOrder:
return ConfigDataEnvironmentPostProcessor.ORDER + 1;
// Test: replace automatic bootstrap loading with explicit spring.config.import.
// Retain Config Data, configtree, key precedence, missing-key and placeholder assertions.
```

- Verification contribution: 本 Step 额外执行 ConfigDecryptEnvironmentPostProcessorTest 及配置加解密相关测试，使用临时文件和 WebApplicationType.NONE。
- After this file: 本 Step 所有门禁通过后按既有单独 commit 边界提交。

- Validation working directory: repository root（逐root help:effective-pom/dependency:tree在相应POM目录）。
- Verification command:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -pl :egon-cola-source-service-starter,:egon-cola-source-web-starter -am -Dtest='*Native*Rpc*Test,*Native*ConfigurationTest,*Native*CompatibilityTest,EvaluationExternalFreeContextTest,OrganizationApplicationTest' -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -pl :egon-cola-source-service-starter,:egon-cola-source-web-starter -am test
python3 scripts/check-native-archetype-boundaries.py service web
```

- Expected result: 本Step所有目标测试/静态断言成功，目标测试实际执行数非零，`git diff --check`通过；Service 11 个 provider 操作、Web 10 个 provider 操作及双向 domain clients 均为 native，metadata/错误/业务 HTTP 保持。
- Failure returns to: 本Step出现失败的精确文件；前序缺陷使用归属原Step的独立corrective commit，不重写历史。
- Completion criteria: Requirements对应证据、全部适用Manual Check和十条Literal Rule分别记录PASS或有证据N/A；无FAIL/BLOCKED/UNKNOWN。
- Rollback: 仅回退本Step源代码commit；保留用户worktree和不可变migration，generated通过脚本重建。
- Commit paths: `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/config/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/rpc/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/config/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcIdQuery.java`, `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcSchoolClassQuery.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.example`, `egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.prod.example`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/config/NativeRpcConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/facade/impl/CourseFacadeImpl.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/rpc/CourseRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImpl.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ScoreFacadeImpl.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ExamRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ScoreRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/NativeServiceRpcProviderTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/EvaluationDubboTripleIntegrationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/package-info.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClient.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClient.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcProperties.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationClientFailureMapper.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationDirectoryConverter.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClientTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClientTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/EvaluationServiceApplication.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/NativeHttpSecurityConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationDataSourceModeTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationExternalFreeContextTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/NativeServiceConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.prod.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.yaml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/README.md`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.example`, `egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.prod.example`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/config/NativeRpcConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationFacadeSupport.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextDTO.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextInterceptor.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/teaching/rpc/SchoolClassRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/user/rpc/UserRpcProvider.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcContextTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcProviderTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/OrganizationDubboProviderConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClient.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationClientFailureMapper.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationQueryConverter.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClient.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcProperties.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClientTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClientTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/OrganizationApplication.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/NativeHttpSecurityConfiguration.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/OrganizationSwaggerConfig.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-dev.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-prod.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-test.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap.yml`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeHttpCompatibilityTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeWebConfigurationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationApplicationTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationDataSourceModeTest.java`, `egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml`

- Commit: `refactor(archetypes): migrate native service and web infrastructure`

### Step 5 — Enforce open compatibility and Agent minimal dependencies

- Requirements: `REQ-002`, `REQ-003`, `REQ-005`, `REQ-006`, `REQ-009`
- Dependencies: `Step 4`
- Baseline state: Step1已规范owner；Open wire/业务代码未改，Agent仍为AI应用。
- Observable outcome: Open runtime与COLA基础组件保持，Agent不含DDC/EgonRPC/Nacos/Dubbo/ShardingSphere；guard真实区分BOM和runtime。
- End state: Open runtime与COLA基础组件保持，Agent不含DDC/EgonRPC/Nacos/Dubbo/ShardingSphere；guard真实区分BOM和runtime。 后续 Step 内容保持 Pending。
- Test-first gate: `Required` — 首先运行本Step声明的missing behavior/owner/annotation/normalization测试，记录真实RED后最小GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 5`, `Rule 7`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE scripts/check-archetype-family-boundaries.py`

- Purpose: 增加 family 检查与失败 fixture。
- Symbols: Open common/transport requirements; Agent forbidden coordinates
- Repository evidence: Light Open 无Dubbo业务；service/webOpen使用自己的facadeIDL；Agent第三方grpc不等于EgonRPC。
- Dependencies and consumers: Light Open 无Dubbo业务；service/webOpen使用自己的facadeIDL；Agent第三方grpc不等于EgonRPC。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Open common/transport requirements; Agent forbidden coordinates；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: XML direct declarations/effective/runtime tree分别检查，不能要求BOM出现在普通tree。
- Error and edge behavior: fixture注入缺失Common/错误native starter时必须失败。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 增加 family 检查与失败 fixture。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 7`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```python
assert Light Open retains Cloud/Nacos/Springdoc/Common/MyBatis/DTP as consumed
assert Service/Web Open retain Dubbo/Triple/gRPC/proto and local facade
assert Agent excludes exact forbidden Egon/DDC/Nacos/Dubbo/ShardingSphere coordinates
verify negative fixtures without editing source
```

- Verification contribution: 本Step下列命令验证 Open common/transport requirements; Agent forbidden coordinates；需要非零目标测试数或明确静态断言。
- After this file: 增加 family 检查与失败 fixture。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 2 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-adapter/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-application/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-common/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-domain/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml`
- `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/pom.xml`

- Purpose: 仅修正family guard发现的必要依赖声明，保留wire/AI版本。
- Symbols: Open managed declarations; Agent minimal dependencies
- Repository evidence: 所有root/child路径已展开；Step1 owner转换后仅本Step负责family必要性。
- Dependencies and consumers: 所有root/child路径已展开；Step1 owner转换后仅本Step负责family必要性。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: Open managed declarations; Agent minimal dependencies；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 版本来自owner；Opengrpc1.73/proto3.25.8；AgentSpringAI1.1.8/ADK/Flow。
- Error and edge behavior: 不改OpenJava/proto/SQL或Agent业务代码；无变化文件不提交。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 仅修正family guard发现的必要依赖声明，保留wire/AI版本。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 7`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
retain source-used external and Common/ID/MP/DTP artifacts
remove only evidenced forbidden or duplicate declarations
leave Open protobuf files byte-identical and Agent runtime flow unchanged
```

- Verification contribution: 本Step下列命令验证 Open managed declarations; Agent minimal dependencies；需要非零目标测试数或明确静态断言。
- After this file: 仅修正family guard发现的必要依赖声明，保留wire/AI版本。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 3 — `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`

- Purpose: 对生成Agent加入真实运行时边界断言。
- Symbols: required AI/ADK/Flow; forbidden native infrastructure
- Repository evidence: 现有verifier支持generated POM/fileassertions。
- Dependencies and consumers: 现有verifier支持generated POM/fileassertions。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: required AI/ADK/Flow; forbidden native infrastructure；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 管理条目不等于传递运行依赖；不要误伤ADK的grpc。
- Error and edge behavior: 禁用/丢失必须AI依赖均失败。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 对生成Agent加入真实运行时边界断言。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 7`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```groovy
inspect actual dependency declarations/tree for forbidden coordinates
assert required Agent Flow/SpringAI/ADK and topology
// Preserved mapping: 管理条目不等于传递运行依赖；不要误伤ADK的grpc。
// Required failure assertion: 禁用/丢失必须AI依赖均失败。
```

- Verification contribution: 本Step下列命令验证 required AI/ADK/Flow; forbidden native infrastructure；需要非零目标测试数或明确静态断言。
- After this file: 对生成Agent加入真实运行时边界断言。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 4 — `CREATE egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/open-dependency-boundary.groovy`

- Purpose: 提供可被verify调用的Open边界断言。
- Symbols: verifyOpenDependencyBoundary
- Repository evidence: 现有Groovy verifier与外部技术栈源码。
- Dependencies and consumers: 现有Groovy verifier与外部技术栈源码。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: verifyOpenDependencyBoundary；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: Light Open不得被强加Dubbo；Common版本来自parent管理。
- Error and edge behavior: 本文件在Step7由verify与generator实际接线，不声称未接线脚本已执行。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 提供可被verify调用的Open边界断言。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 7`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```groovy
assert parsed POM inherits expected parent
assert Light Open existing external/Common stack
assert no native RPC/DDC dependencies
use resolved tree only for runtime assertions
```

- Verification contribution: 本Step下列命令验证 verifyOpenDependencyBoundary；需要非零目标测试数或明确静态断言。
- After this file: 提供可被verify调用的Open边界断言。 已完成；只有全部组和门禁完成后可提交本Step。

- Validation working directory: repository root（逐root help:effective-pom/dependency:tree在相应POM目录）。
- Verification command:

```bash
python3 scripts/check-archetype-dependency-ownership.py
python3 scripts/check-archetype-family-boundaries.py
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -pl :egon-cola-source-light-open,:egon-cola-source-service-open-starter,:egon-cola-source-web-open-starter,:egon-cola-source-agent-starter -am test
```

- Expected result: 本Step所有目标测试/静态断言成功，目标测试实际执行数非零，`git diff --check`通过；Open runtime与COLA基础组件保持，Agent不含DDC/EgonRPC/Nacos/Dubbo/ShardingSphere；guard真实区分BOM和runtime。
- Failure returns to: 本Step出现失败的精确文件；前序缺陷使用归属原Step的独立corrective commit，不重写历史。
- Completion criteria: Requirements对应证据、全部适用Manual Check和十条Literal Rule分别记录PASS或有证据N/A；无FAIL/BLOCKED/UNKNOWN。
- Rollback: 仅回退本Step源代码commit；保留用户worktree和不可变migration，generated通过脚本重建。
- Commit paths: `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/open-dependency-boundary.groovy`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-adapter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-application/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-common/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-domain/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/pom.xml`, `egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml`, `scripts/check-archetype-family-boundaries.py`

- Commit: `test(archetypes): enforce open and agent dependency boundaries`

### Step 6 — Normalize generated parent and sentinel handling

- Requirements: `REQ-007`, `REQ-010`
- Dependencies: `Step 5`
- Baseline state: source family切换已提交；仓库.generated仍为旧集合。
- Observable outcome: 临时fixture证明生成parent为具体发布GAV、emptyrelativePath，并保持lock/staging/hash；正式生成留Step7。
- End state: 临时fixture证明生成parent为具体发布GAV、emptyrelativePath，并保持lock/staging/hash；正式生成留Step7。 后续 Step 内容保持 Pending。
- Test-first gate: `Required` — 首先运行本Step声明的missing behavior/owner/annotation/normalization测试，记录真实RED后最小GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 5`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE scripts/test-generated-parent-normalization.sh`

- Purpose: 为parent/sentinel提供独立RED fixture。
- Symbols: source-local-parent fixture; release GAV assertions
- Repository evidence: generator集中normalize_generated_product，existingfixture已有fakeMaven。
- Dependencies and consumers: generator集中normalize_generated_product，existingfixture已有fakeMaven。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: source-local-parent fixture; release GAV assertions；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 临时目录输入含source parent、相对路径、内部版本sentinel。
- Error and edge behavior: 旧generator会泄漏parent；RED必须针对该差异。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 为parent/sentinel提供独立RED fixture。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 9`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
create isolated source/definition fixture
run generator through fixture fake Maven
assert generated root uses concrete archetypes parent and empty relativePath
assert source/project/module versions normalize independently
assert no source sentinel; unchanged-input check passes
```

- Verification contribution: 本Step下列命令验证 source-local-parent fixture; release GAV assertions；需要非零目标测试数或明确静态断言。
- After this file: 为parent/sentinel提供独立RED fixture。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 2 — `MODIFY scripts/generate_archetypes.sh`

- Purpose: 在现有pipeline规范root parent并复制新增verification资源。
- Symbols: normalize_generated_parent; normalize_generated_product; copy_curated_assets
- Repository evidence: 已有sourcePOMoverlay、Velocityescaping、hash/lock/atomic实现。
- Dependencies and consumers: 已有sourcePOMoverlay、Velocityescaping、hash/lock/atomic实现。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: normalize_generated_parent; normalize_generated_product; copy_curated_assets；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 只改生成用户项目rootparent，不改generatedpackaging parent相对路径；增加Openboundarygroovy被实际拷贝。
- Error and edge behavior: 任何失败在swap前停止；不能手改.generated或破坏源字节。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 在现有pipeline规范root parent并复制新增verification资源。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 9`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
after source POM overlay and token normalization:
  rewrite only generated consumer root parent to release coordinate/empty relativePath
  reject unresolved rootVersion/source path
  preserve child rootArtifactId/version and Velocity escaping
copy declared auxiliary Groovy verification resource
retain lock/staging/atomic/hash pipeline
```

- Verification contribution: 本Step下列命令验证 normalize_generated_parent; normalize_generated_product; copy_curated_assets；需要非零目标测试数或明确静态断言。
- After this file: 在现有pipeline规范root parent并复制新增verification资源。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 3 — `MODIFY scripts/test-generate-archetypes.sh`

- Purpose: 更新fixture并补齐determinism/原子失败/curated资源断言。
- Symbols: parentfixture; auxiliaryverifiercopy; source drift and lock tests
- Repository evidence: 现有test已有两代hash和失败注入。
- Dependencies and consumers: 现有test已有两代hash和失败注入。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: parentfixture; auxiliaryverifiercopy; source drift and lock tests；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 保留所有旧测试语义并加入新parent情形。
- Error and edge behavior: 不删除atomic/locktests，不触碰真实用户生成目录。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 更新fixture并补齐determinism/原子失败/curated资源断言。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 5`, `Rule 9`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
include concrete/source-local parent in fake Maven output
assert normalized root parent and auxiliary verifier copied
retain all determinism/failure/lock/source-sentinel checks
```

- Verification contribution: 本Step下列命令验证 parentfixture; auxiliaryverifiercopy; source drift and lock tests；需要非零目标测试数或明确静态断言。
- After this file: 更新fixture并补齐determinism/原子失败/curated资源断言。 已完成；只有全部组和门禁完成后可提交本Step。

- Validation working directory: repository root（逐root help:effective-pom/dependency:tree在相应POM目录）。
- Verification command:

```bash
bash scripts/test-generated-parent-normalization.sh
bash scripts/test-generate-archetypes.sh
bash -n scripts/generate_archetypes.sh
# fixture 内运行 generate/check；本 Step 不对正式旧 .generated 要求 check 成功。
```

- Expected result: 本Step所有目标测试/静态断言成功，目标测试实际执行数非零，`git diff --check`通过；临时fixture证明生成parent为具体发布GAV、emptyrelativePath，并保持lock/staging/hash；正式生成留Step7。
- Failure returns to: 本Step出现失败的精确文件；前序缺陷使用归属原Step的独立corrective commit，不重写历史。
- Completion criteria: Requirements对应证据、全部适用Manual Check和十条Literal Rule分别记录PASS或有证据N/A；无FAIL/BLOCKED/UNKNOWN。
- Rollback: 仅回退本Step源代码commit；保留用户worktree和不可变migration，generated通过脚本重建。
- Commit paths: `scripts/generate_archetypes.sh`, `scripts/test-generate-archetypes.sh`, `scripts/test-generated-parent-normalization.sh`

- Commit: `fix(archetypes): normalize generated parent coordinates`

### Step 7 — Update definitions, regenerate and close release gates

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-021`
- Dependencies: `Step 6`
- Baseline state: 所有source/contract/generatorsteps已提交；definitions仍可能要求旧parent和native标记。
- Observable outcome: 七个generated产品通过更新后的verifier与MavenIT，.generated保持ignored，最终Spec与逐规则审计有完整记录。
- End state: 七个generated产品通过更新后的verifier与MavenIT，.generated保持ignored，最终Spec与逐规则审计有完整记录。 后续 Step 内容保持 Pending。
- Test-first gate: `Not applicable` — 最终派生/发布形状门禁；此前Step已提供RED/GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/verify.groovy`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/test/resources/projects/basic/verify.groovy`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/test/resources/projects/basic/verify.groovy`

- Purpose: 修正七个产品的parent/runtime/transport/配置门禁。
- Symbols: native/open/Agent boundaries; published parent; contract operation count
- Repository evidence: 现有nativeverifier还要求Bootparent/Dubbo/Nacos；Openverifier保留21operation。
- Dependencies and consumers: 现有nativeverifier还要求Bootparent/Dubbo/Nacos；Openverifier保留21operation。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native/open/Agent boundaries; published parent; contract operation count；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 按实际family检查source/managed/runtime；LightOpen调用auxiliaryGroovygate。
- Error and edge behavior: 不能删业务/architectureassertions；任何缺operation/旧依赖都阻断。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 修正七个产品的parent/runtime/transport/配置门禁。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```groovy
assert generated root parent is concrete archetypes parent with empty relativePath
assert native exact RPC/interface/config contracts and no old stack
assert Open external and Common boundaries, Agent minimalism
retain topology/SQL/HTTP/architecture/launch checks
execute auxiliary Open script explicitly
```

- Verification contribution: 本Step下列命令验证 native/open/Agent boundaries; published parent; contract operation count；需要非零目标测试数或明确静态断言。
- After this file: 修正七个产品的parent/runtime/transport/配置门禁。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 2 — `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/main/resources/META-INF/maven/archetype-metadata.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/main/resources/META-INF/maven/archetype-metadata.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/main/resources/META-INF/maven/archetype-metadata.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/main/resources/META-INF/maven/archetype-metadata.xml`

- Purpose: 将新增nativeproto包含在生成合同中，保留全部旧fileSets。
- Symbols: light src/main/proto fileSet; native newJava/test sources
- Repository evidence: Lightmetadata目前没有protofileset，Java/testrecursivefileSets已存在。
- Dependencies and consumers: Lightmetadata目前没有protofileset，Java/testrecursivefileSets已存在。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: light src/main/proto fileSet; native newJava/test sources；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 只新增缺失proto覆盖；sharedfacade通过发布artifact消费不复制入service/web。
- Error and edge behavior: 不使数据库/部署fileSets丢失；未需要改变的metadata保持字节。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 将新增nativeproto包含在生成合同中，保留全部旧fileSets。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
add filtered native Light src/main/proto **/*.proto fileSet
retain all existing requiredProperties/fileSets
assert shared contract dependency resolution in generated service/web
```

- Verification contribution: 本Step下列命令验证 light src/main/proto fileSet; native newJava/test sources；需要非零目标测试数或明确静态断言。
- After this file: 将新增nativeproto包含在生成合同中，保留全部旧fileSets。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 3 — `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light/architecture-docs/large-monolith-light-domain-architecture.md`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/javadoc/README.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service/architecture-docs/student-management-service-only-rpc-mq-architecture.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service/src/main/javadoc/README.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web/architecture-docs/multi-project-multi-module-architecture.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web/src/main/javadoc/README.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-agent/architecture-docs/agent-multi-module-architecture.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/main/javadoc/README.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light-open/architecture-docs/large-monolith-light-domain-architecture.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/main/javadoc/README.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service-open/architecture-docs/student-management-service-only-rpc-mq-architecture.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/main/javadoc/README.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web-open/architecture-docs/multi-project-multi-module-architecture.md`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/main/javadoc/README.md`

- Purpose: 同步交付文档中的parent/native/Open/Agent说明。
- Symbols: native RPC/DDC/OpenAPI; source-to-generated ownership
- Repository evidence: definitions架构文档是curated交付内容；原native部分仍写Dubbo/Nacos。
- Dependencies and consumers: definitions架构文档是curated交付内容；原native部分仍写Dubbo/Nacos。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: native RPC/DDC/OpenAPI; source-to-generated ownership；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 只更新本次变动能力和契约位置，Open/Agent原业务技术栈说明保持。
- Error and edge behavior: 不重写无关业务章节，不引入runtime已验收断言。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 同步交付文档中的parent/native/Open/Agent说明。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```markdown
replace stale native transport and parent statements
record shared native facade artifact and native/Open version separation
preserve unrelated architecture and immutable SQL instructions
```

- Verification contribution: 本Step下列命令验证 native RPC/DDC/OpenAPI; source-to-generated ownership；需要非零目标测试数或明确静态断言。
- After this file: 同步交付文档中的parent/native/Open/Agent说明。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 4 — `GENERATED egon-cola-archetypes/.generated/**`

- Purpose: 经canonical脚本重建七个产品并校验。
- Symbols: generated consumer POM/proto/tests/verifiers/manifests
- Repository evidence: Steps1–6 +当前definition最终输入。
- Dependencies and consumers: Steps1–6 +当前definition最终输入。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: generated consumer POM/proto/tests/verifiers/manifests；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: generate->check->-Pgenerated-archetypes IT；源为唯一事实。
- Error and edge behavior: 任何失败不发布；.generated永不stage/commit。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 经canonical脚本重建七个产品并校验。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```bash
bash scripts/generate_archetypes.sh generate
bash scripts/generate_archetypes.sh check
./mvnw -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean integration-test
assert no tracked .generated files
```

- Verification contribution: 本Step下列命令验证 generated consumer POM/proto/tests/verifiers/manifests；需要非零目标测试数或明确静态断言。
- After this file: 经canonical脚本重建七个产品并校验。 已完成；只有全部组和门禁完成后可提交本Step。

#### File 5 — `MODIFY egon-cola-archetypes/pom.xml`

同一责任的逐文件顺序（全部显式归属本组）：

- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light/packaging-pom.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service/packaging-pom.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web/packaging-pom.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-agent/packaging-pom.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-light-open/packaging-pom.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-service-open/packaging-pom.xml`
- `MODIFY egon-cola-archetypes/definitions/egon-cola-archetype-web-open/packaging-pom.xml`

- Purpose: 只处理最终IT要求的test/package接线。
- Symbols: generated-archetypes profile; verifier dependencies
- Repository evidence: 现有defaultreactor只有2facade；实际7archetypes在generatedprofile。
- Dependencies and consumers: 现有defaultreactor只有2facade；实际7archetypes在generatedprofile。；后续本Step接线及source/generated consumer。
- Why now: 本组在前组编译/测试或转换前置之后完成，产生下一组依赖的状态；首组负责本Step最早可执行的证明点。
- Contract/signature changes: generated-archetypes profile; verifier dependencies；以Spec §7.4/§9/§10为准。
- Input/output and state mapping: 不增加runtime dependency或改publicGAV；没有所需差异时不编辑/提交。
- Error and edge behavior: 禁止无实际IT证据的test规避。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001` — 只处理最终IT要求的test/package接线。 Java类遵守语义后缀、Validation、MapStruct/BaseConverter、named Bean/qualified constructor与java.time；本组非Java部分不凭空增加业务对象。
- Literal rule enforcement: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11` — 必须逐项核对本组实际diff；手写简单carrier用record，复杂carrier若出现必须完整Lombok；不允许未闭合例外。Rule11保持所选archetype树。
- Implementation pseudocode:

```xml
run actual generated profile IT
correct only required test/package wiring revealed by this gate
retain public coordinates and source/packaging distinction
```

- Verification contribution: 本Step下列命令验证 generated-archetypes profile; verifier dependencies；需要非零目标测试数或明确静态断言。
- After this file: 只处理最终IT要求的test/package接线。 已完成；只有全部组和门禁完成后可提交本Step。

- Validation working directory: repository root（逐root help:effective-pom/dependency:tree在相应POM目录）。
- Verification command:

```bash
bash scripts/test-generate-archetypes.sh
bash scripts/test-archetype-release.sh
python3 scripts/check-archetype-dependency-ownership.py
python3 scripts/check-native-archetype-boundaries.py light service web
python3 scripts/check-archetype-family-boundaries.py
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml test
# 安装本次parent/BOM/shared facade与source前置artifact供generatedconsumer解析；install不等于publish。
./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -DskipTests install
bash scripts/generate_archetypes.sh generate
bash scripts/generate_archetypes.sh check
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean integration-test
test -z "$(git ls-files egon-cola-archetypes/.generated)"
# 临时Maven repository安装本次parent/BOM/artifacts，再验证empty-relativePath消费者；不使用远程publish。
# 最后核对非目标文件哈希、不可变SQL、所有Stepcommit与Manual/LiteralRules。
```

- Expected result: 本Step所有目标测试/静态断言成功，目标测试实际执行数非零，`git diff --check`通过；七个generated产品通过更新后的verifier与MavenIT，.generated保持ignored，最终Spec与逐规则审计有完整记录。
- Failure returns to: 本Step出现失败的精确文件；前序缺陷使用归属原Step的独立corrective commit，不重写历史。
- Completion criteria: Requirements对应证据、全部适用Manual Check和十条Literal Rule分别记录PASS或有证据N/A；无FAIL/BLOCKED/UNKNOWN。
- Rollback: 仅回退本Step源代码commit；保留用户worktree和不可变migration，generated通过脚本重建。
- Commit paths: `egon-cola-archetypes/definitions/egon-cola-archetype-agent/architecture-docs/agent-multi-module-architecture.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-agent/packaging-pom.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/main/javadoc/README.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/main/resources/META-INF/maven/archetype-metadata.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/architecture-docs/large-monolith-light-domain-architecture.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/packaging-pom.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/main/javadoc/README.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/main/resources/META-INF/maven/archetype-metadata.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/definitions/egon-cola-archetype-light/architecture-docs/large-monolith-light-domain-architecture.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-light/packaging-pom.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/javadoc/README.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/architecture-docs/student-management-service-only-rpc-mq-architecture.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/packaging-pom.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/main/javadoc/README.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/main/resources/META-INF/maven/archetype-metadata.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/definitions/egon-cola-archetype-service/architecture-docs/student-management-service-only-rpc-mq-architecture.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-service/packaging-pom.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-service/src/main/javadoc/README.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/architecture-docs/multi-project-multi-module-architecture.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/packaging-pom.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/main/javadoc/README.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/main/resources/META-INF/maven/archetype-metadata.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/definitions/egon-cola-archetype-web/architecture-docs/multi-project-multi-module-architecture.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-web/packaging-pom.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-web/src/main/javadoc/README.md`, `egon-cola-archetypes/definitions/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`, `egon-cola-archetypes/definitions/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`, `egon-cola-archetypes/pom.xml`; GENERATED evidence only, never stage or commit: `egon-cola-archetypes/.generated/**`

- Commit: `test(archetypes): close native open agent generated release gates`

Step 2 修正记录：Step 3 的完整 `verify` 发现唯一 `ARCH-010` 来自编译器生成的 `LightRpcConverterImpl`。依 `PLAN-CLAR-009` 仅调整 Light POM 的该生成类配置，先做独立修正提交，再恢复 Step 3。原 Step 2 提交不重写，所有 mapper 字段、映射与单元测试保持。

## 8. Test, Validation, and Quality Gates

1. `python3 .agents/skills/egon-coding-writing-spec/scripts/validate_spec.py docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md --strict`。
2. `python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md --strict`。
3. 按每Step命令RED/GREEN、diff review、逐Manual/LiteralRules再路径提交，不跨Step编辑。
4. Step7运行全部source/generated/family/fixture gates。记录所有失败、跳过和runtime未验证；不把bootstrap install或部分log当测试成功。
5. 最终审计报告为 `docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.execution.md`，单独docs提交；包含每个Step commit、逐requirement、17项MC、10条Rule与全量输出索引。

## 9. Migration, Compatibility, Rollout, and Rollback

按七Step严格串行；native wire一次性迁到新v1，共享业务facade DTO/HTTP/DB不变，不承诺旧Dubbo wire互通。Open IDL和外部栈保持。Source/generated roots发布parent具体版本；本地install只为验证，不向远程发布。原有用户相关POM变更包含在Step1，不覆盖Agent五个无关已暂存文件。不得修改、删除、重命名任何历史Flyway migration。正式.generated保持ignored且只由generator生成。真实DDC/TLS/认证/外部系统运行时验收留用户。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Source and effective meaning | Steps | Primary proof |
| --- | --- | --- | --- |
| `REQ-001` | primary §4：native RPC/DDC/API Doc | 2,3,4,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-002` | primary §4：Open外部体系 | 5,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-003` | primary §4：Agent最小依赖 | 5,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-004` | primary §4：Components BOM owner | 1,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-005` | primary §4：ShardingSphere owner | 1,5,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-006` | primary §4：Commons owner及不降级 | 1,5,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-007` | primary §4：具体parent和独立解析 | 1,6,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-008` | primary §4：native源码/配置/测试/Compose/verifier联动 | 2,3,4,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-009` | primary §4：Open所需COLA基础组件 | 5,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-010` | primary §4：生成确定性/原子性 | 6,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-011` | primary §4：31-operation native unary contracts | 2,3,4,7 | Step命令与Spec TEST对应，最终source/generated审计 |
| `REQ-012` | open family相应REQ（非primary新需求）：Common whitelist按primary REQ-009修订，既有EgonModel/MP/DTP消费者保留；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-013` | open family相应REQ（非primary新需求）：DTP executor/config原样；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-014` | open family相应REQ（非primary新需求）：21个Open Triple/gRPC operation与codegen版本保持；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-015` | open family相应REQ（非primary新需求）：Open Gateway外置/Light-Web Springdoc；native改用平台owner；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-016` | open family相应REQ（非primary新需求）：最小有消费者依赖；generation/migration boundaries；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-017` | open family相应REQ（非primary新需求）：业务HTTP/GraphQL/MQ兼容；历史SQL哈希不变；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-018` | open family相应REQ（非primary新需求）：公开archetype GAV/discovery/topology保持；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-019` | open family相应REQ（非primary新需求）：Open测试无外部依赖，manual SQL不改；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-020` | open family相应REQ（非primary新需求）：无runtime/publish；所有gate失败可见；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |
| `REQ-021` | open family相应REQ（非primary新需求）：Open Common Snowflake ID与Long/BIGINT保持；generated Spec同号条款仅在其指定章节范围生效 | 5,7 | 原Open测试、verifier、source/SQL哈希与generated IT；parent/owner被primary显式覆盖 |

## 11. Risks, Blockers, and User Decisions

旧稿的缺JavaRPCinterface/共享合约归属、漏Webprovider/childPOM/metadata/Compose、skipTests假GREEN、Step6提前check与未启用generatedprofile已在Spec §7.4和本Plan逐项补齐。用户已授权本次修订后继续实施，不因原文Review状态再次等待。

剩余风险均为执行验证责任：MapStruct protobuf builder/presence/集合、跨服务业务错误/context、原生starter配置及依赖版本必须在对应Step闭合；无法在授权范围内闭合的真实问题需报告，不静默略过。真实运行时验收与远程发布未执行。历史全仓失败只能按effective TEST-020用fresh证据隔离并明确披露。

## 12. Review and Acceptance

### 12.1 Original-request fidelity

保留用户全部迁移要求与七Step串行提交，补齐31个方法而不增加业务operation；HTTP/DB/Open/Agent边界不扩张。

### 12.2 Repository and technical fidelity

依据当前dfd24ce3f +用户dirty POM，逐文件展开真实路径；实际native prefix、RPCinterface要求、只支持字段后注入的EgonRpcReference、BOM优先级和generatedprofile来自源码。只将内部语法适配留给实施。

### 12.3 Cross-section consistency

Spec §7.4/§9/§10、文件清单、Step命令、提交边界、回滚和最终报告一致。每Step Manual与LiteralRules是未来实证门禁，文档PASS不等于实现完成。

### 12.4 Relationship and effective-design review

Primary Spec Accepted，Plan Ready，依据当前会话明确授权；parent/Common allowlist的前驱差异由primary Amends明确覆盖，其余effective章节保持。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable | PASS | §5 exact既有archetype/facade树；无新增module | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-REUSE-001` | Applicable | PASS | Spec §7.4 core/平台/proxy/validator/mapper ledger | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-DEP-001` | Applicable | PASS | Step1双native/Open版本与Commons owner；Step3/4实际starter消费者 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-NAME-001` | Applicable | PASS | Step2 RpcService/Converter；Step3/4 Query/Properties/ContextDTO records | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-VALID-001` | Applicable | PASS | Step3/4 DTO group、scalar query与原business boundary验证 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-MODEL-001` | Applicable | PASS | Spec §10 records；既有Response不改，generated由protoc负责 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-CONVERT-001` | Applicable | PASS | Step2三个BaseConverter/MapStruct；Step4两个domain mapper | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-LOG-001` | Applicable | PASS | Step3/4 provider/client @Slf4j且不输出payload/credentials | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-BEAN-001` | Applicable | PASS | namedBean + Lombok finalQualifier；programmaticproxy修正field-only冲突 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-UTIL-001` | Applicable | PASS | JDK/既有Commons/Guava；无额外utility库 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-JSON-001` | Applicable | PASS | HTTPDTO字段不改，Nativewire独立；Jackson/OAS回归 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-TIME-001` | Applicable | PASS | Spec §10 LocalDateTime/Instant ISO、null、纳秒往返 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-CONFIG-001` | Applicable | PASS | 三native四profile完整清单、真实prefix、HTTP兼容 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-PATTERN-001` | Applicable | PASS | Adapter分离原facade和proto；复用既有factory，保留generatorPipeline | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-SCOPE-001` | Applicable | PASS | 逐文件归属、step-pathlimitedcommit、保留Agentstaged | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-TEST-001` | Applicable | PASS | 31operation/mapper/context/native/family/generator/真实generatedprofile gate | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |
| `MC-BLOCKER-001` | Applicable | PASS | 本轮用户已授权修订，旧稿阻塞已明确改正；执行仍逐Step实证 | 设计与执行门禁明确，尚非运行证据 | None；在所属Step重新验证 |

### 12.6 Final verdict

`PASS — Ready for user review`

用户已确认补齐后执行，Plan状态为Ready；上述verdict仅表示文档结构和设计审查通过，后续实现必须提供独立证据。全部Step完成后仍需最终Spec/MC/LiteralRule审计。
