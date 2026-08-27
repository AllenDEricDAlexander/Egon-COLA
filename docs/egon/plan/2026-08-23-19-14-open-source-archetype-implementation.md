# Egon COLA Open-Source Archetype Family Implementation Plan

| Field | Value |
| --- | --- |
| Document | `2026-08-23-19-14-open-source-archetype-implementation.md` |
| Template Version | `2` |
| Status | `Review` |
| Created | `2026-08-23 19:14 CST` |
| Updated | `2026-08-23 19:42 CST` |
| Owner | `Mario / Egon-COLA maintainers` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-archetypes parent；新增 light-open/service-open/web-open；generated templates、manual SQL、Proto、Mapper、DTP、验证器、CI、README 与发布文档` |
| Source Requirement | `2026-08-23 用户在完成 Review Spec 后显式调用 egon-coding-writing-plan；要求只规划三个开源 -open archetype，不实施内部版，不写代码或启动项目` |
| Baseline Revision | `main@772df2b28b4abf29e7fffae6ea1b10fd616397b4；2026-08-23 19:14 CST dirty-worktree snapshot，§6.1 列出的并发路径不得进入本 Plan 的提交` |
| Implements Spec | [Open-Source Archetype Family Spec](../spec/2026-08-23-16-43-open-source-archetype-family.md) |
| Spec Status | `Review` |
| Spec Revision | `Updated 2026-08-23 19:42 CST（仅 Related Plans relationship metadata；设计内容修订于 17:29 CST）` |
| Effective Specs | [Open-Source Archetype Family Spec](../spec/2026-08-23-16-43-open-source-archetype-family.md)；[Light living architecture](../../../egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md)；[Service living architecture](../../../egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md)；[Web living architecture](../../../egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md)；[Domain-first package design](../../superpowers/specs/2026-07-13-web-service-archetype-domain-first-package-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本 Plan 将一个 `Review` 状态的主 Spec 转换成 14 个顺序、可验证、每 Step 一次 path-limited commit 的实现单元。
依赖路径是：先复制并隔离三个 sibling archetype；再依次完成 Light、Service、Web 的 Long ID、MyBatis/manual
SQL、Proto/DTP/配置切面；最后收口 verifier、全 reactor、CI、双语发现与发布文档。Service/Web 的 Proto
contract 先于 provider/consumer 编译，Long domain/PO 先于 Mapper/SQL，focused RED 先于每个 GREEN 转换。

最终证据是三个新坐标分别通过 Maven Invoker generated-project `clean verify`，六个 archetype 一起通过
`egon-cola-archetypes` reactor `clean integration-test`，open 生成树不存在 JPA/Flyway/UUID/Gateway/unapproved
`top.egon` artifact，21 个 RPC 与 18 个表族合同被验证，原三个 archetype path-limited diff 为零。本文件只
规划未来命令；当前没有复制模板、执行 SQL、启动 Nacos/Redis/PostgreSQL/RabbitMQ/Gateway 或运行应用。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Open-Source Archetype Family Spec](../spec/2026-08-23-16-43-open-source-archetype-family.md)
- Status: `Review`
- Revision: Header `Updated 2026-08-23 19:42 CST`；design baseline `main@772df2b...`。
- Approval evidence: 用户在收到 `PASS — Ready for user review` 后显式调用本 Plan Skill。这授权编写
  `Review` Plan，但不等价于接受 Spec、批准 Plan 或授权 implementation。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [Open family](../spec/2026-08-23-16-43-open-source-archetype-family.md) | `Review` / 2026-08-23 19:42 | All §1-§20 | 唯一 governs `-open` product、Long/Proto/SQL/dependency/runtime/test/release contracts |
| Normative dependency | [Light living architecture](../../../egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md) | living repository document at baseline | §1-§5 | 保留单模块 layer/package/call-chain ownership |
| Normative dependency | [Service living architecture](../../../egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md) | living repository document at baseline | §1-§5 | 保留 service-only、RPC/MQ、module/layer restrictions |
| Normative dependency | [Web living architecture](../../../egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md) | living repository document at baseline | §1-§5 | 保留 Web HTTP/GraphQL/RPC/module ownership |
| Normative dependency | [Domain-first design](../../superpowers/specs/2026-07-13-web-service-archetype-domain-first-package-design.md) | `Approved for design documentation` / 2026-07-13 | §Package Organization Invariant through §Generated Contract Verification | 固定 `user/teaching/course/exam` domain-first package and external-client exception |

Primary Spec is later and scope-specific：它只对新 `-open` copies 覆盖旧 dependency、canonical external facade、
JPA/Flyway/UUID/schema 和 six-to-seven module statements；原产品与其 living documents 不被改写。Dependency
documents继续约束 layer direction、domain-first、service-only 和 existing business behavior。

### 2.3 Superseded or excluded content

- `Amends=None`、`Supersedes=None`；不存在 accepted amendment 或 replacement。
- 原 ShardingSphere/Flyway/UUID Specs 与 Plans只解释 baseline，不能把 Flyway/UUID 规则带入 open implementation。
- Common MP Starter Spec 只是冲突证据；`DEC-001` 明确选择 official MP + Common ID，不消费该 Starter。
- 平台 Gateway、DTP Admin、future components/platforms archetype product line、Spring gRPC/native second server 全部排除。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | [Spec §4](../spec/2026-08-23-16-43-open-source-archetype-family.md) | 新增 light-open | coordinate generates one module and verifies；original light unchanged | Step 1-4、13-14 |
| `REQ-002` | Spec §4 | 新增 service-open | coordinate generates seven modules and verifies | Step 1、5-8、13-14 |
| `REQ-003` | Spec §4 | 新增 web-open | coordinate generates seven modules and verifies | Step 1、9-14 |
| `REQ-004` | Spec §3/§4 | internal product/originals/components/platforms source out of scope | path-limited original/component/platform diff remains zero | Step 1、13-14 |
| `REQ-005` | Spec §4/§7/§13 | 保持 current layer/module/domain-first direction | generated ArchUnit and verifier reject crossings | Step 4、8、12-14 |
| `REQ-006` | Spec §4/§6 | Java 21、Boot 3.5.16、wrapper 3.9.14 | effective POM/wrapper exact | Step 1、4、8、12-14 |
| `REQ-007` | Spec §4/§6/§15 | Cloud 2025.0.3、SCA 2025.0.0.0、Nacos 3.0.3 | BOM/config/image aligned；tests open no Nacos socket | Step 4、8、12-14 |
| `REQ-008` | Spec §4/§10 | official MP 3.5.17；no Spring Data JPA | 21 Mapper replacements；zero JPA/Hibernate dependency/source | Step 3、6、10、13 |
| `REQ-009` | Spec §4/§7/§11 | ShardingSphere-JDBC 5.5.3 + exact Long stable-slot route | positive keys route once；invalid/range reject；topology parity | Step 2-3、5-6、9-10、13 |
| `REQ-010` | Spec §4/§11 | no Flyway/Liquibase/runtime migration | zero dependency/class/config/history/token；no implicit init | Step 3、6、10、13 |
| `REQ-011` | Spec §4/§11 | six manual PostgreSQL SQL + three runbooks | exact order/targets/BIGINT/parity；test-only explicit execution | Step 3、6、10、13-14 |
| `REQ-012` | Spec §4/§6.2 | exact Common/DTP `top.egon` allowlist | effective tree contains only approved direct/transitive artifacts | Step 3-4、6-8、10-13 |
| `REQ-013` | Spec §4/§7/§15 | each runnable app uses one bounded DTP-governed executor | `applicationTaskExecutor` discovered；context/metric/config test；test DTP disabled | Step 4、8、12-13 |
| `REQ-014` | Spec §4/§9 | 8 Proto services / 21 RPC；Triple only；standard gRPC interop | descriptor/parity/provider/client/status/deadline tests | Step 7、11、13 |
| `REQ-015` | Spec §4/§6/§9 | Gateway external；Light/Web Springdoc；Service no Springdoc | no Gateway artifact/config/route；OpenAPI valid | Step 4、8、12-14 |
| `REQ-016` | Spec §4/§7.0 | no unused dependency/framework/pattern | direct-consumer audit and forbidden tree pass | Step 3-4、6-8、10-14 |
| `REQ-017` | Spec §4/§9/§10/§16 | preserve HTTP/GraphQL/MQ/domain use cases；approved ID/wire representation only | copied behavior fixtures pass；HTTP IDs remain decimal String | Step 2-12、13 |
| `REQ-018` | Spec §4/§8/§16 | open artifacts independently publishable/discoverable | reactor、catalog、README、CI、deploy guide list all three | Step 1、13-14 |
| `REQ-019` | Spec §4/§14/§15 | generated verify needs no live infra | test profile disables/stubs Nacos/DTP/DB/MQ/RPC external calls | Step 3-4、6-8、10-14 |
| `REQ-020` | Spec §4/§16/§20 | no app start or DB SQL execution | implementation uses compile/test/package/static only | All Steps；final gate Step 14 |
| `REQ-021` | Spec §4/§7/§10/§11 | remove generated UUID；Common Snowflake Long + explicit machine-id | no UUID source/DDL；Long/BIGINT/int64/decimal mapping exact | Step 2-3、5-7、9-11、13 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. Step 1 establishes additive coordinates with byte-for-byte copy provenance before semantic transformation。
2. Each product then uses two or four compile-safe slices：Long identity/sharding first；MyBatis/manual SQL second；
   Service/Web Proto third；runtime/DTP/docs fourth。JPA temporarily maps Long only inside unshipped open copies，then is removed。
3. Proto source precedes generated classes，provider and consumer；generated output stays under `target/` and is never edited。
4. Repository ports/use cases remain stable；Adapter converts HTTP decimal String/Proto int64 to Domain Long；Infrastructure
   maps Domain Long to PO/BIGINT and explicit Mapper SQL。
5. Final verifier and CI gates prove negative contracts and original-family immutability after all three open products are GREEN。

### 4.2 Test-first strategy

| Behavior | RED first | Expected RED reason | Minimum GREEN |
| --- | --- | --- | --- |
| Long ID/sharding | copied domain/application/repository + `SnowflakeLongShardingAlgorithmTest` | String/UUID signatures or missing renamed algorithm | Long signatures、Common generator、stable hash route |
| MyBatis/manual schema | copied repository tests + new manual SQL contract tests | JPA repositories/Flyway paths present，Mapper absent | 21 Mapper/XML + PO/RepositoryImpl + manual SQL/no migrator |
| Proto RPC | descriptor and adapter/client contract tests | local facade module/generated services absent | five Proto files/module + provider/client mapping/status/deadline |
| DTP/runtime | Async/config/context tests | no DTP dependency/decorator/managed executor；test may try Redis | bounded Boot executor + decorator + profile config |
| Architecture/API/forbidden | ArchUnit/OpenAPI/verifier assertions | internal plugin/Gateway/forbidden tokens or old version | ArchUnit、Springdoc scope、negative scans |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | all new open directories | establishes copy baseline/coordinates |
| Steps 2-4 | Step 1 | none during execution | light-open paths | same POM/config/test files evolve in ordered slices |
| Steps 5-8 | Step 1 | Steps 9-12 only with separate agents/worktrees | service-open paths | Service local contracts precede Service consumers |
| Steps 9-12 | Step 1 | Steps 5-8 only with separate agents/worktrees | web-open paths | Web local contracts precede Web consumers |
| Step 13 | Steps 2-12 | None | all three open verifier/metadata paths | cross-product parity requires both Proto copies |
| Step 14 | Step 13 | None | shared CI/README/release docs | public discovery only after artifacts prove GREEN |

This Plan is authored for sequential execution。Parallel Service/Web implementation is permissible only with non-overlapping
open directories；Step 11 consumes the wire contract established in Step 7，so descriptor field-number decisions remain
single-source and Step 13 still performs final byte/wire parity。

### 4.4 Commit boundaries

Each Step ends in one semantic commit and stages only its declared paths。A copied open POM/config/test file may reappear in
later Steps only for explicitly named dependency sections：persistence dependency removal，then Proto module/plugin，then
DTP/ArchUnit/runtime config。No Step stages the dirty IdP/Gateway/DDC/RBAC/scripts/other Spec/Plan paths in §6.1。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| three sibling archetypes | Add / Spec §7.0 | parent has only original three | profile flag couples lifecycle | 3 artifacts + duplicate templates | Implement Step 1 |
| 21 Mapper/XML | Add / Spec §7/§10 | 21 real `*JpaRepository` at baseline | JPA compatibility adapter violates literal ban | 21 interfaces + explicit query SQL | Implement；count clarified in §6.4 |
| manual SQL/runbook | Add / Spec §11 | six original Flyway SQL/migrator chain | schema.sql/Liquibase/Flyway violate ownership | DBA step + missing-schema failure | Implement Steps 3/6/10 |
| Long ID/routing | Add / Spec §7/§10 | deprecated UUID bean + UUID hash route | keeping UUID contradicts user | type/schema/API conversion + machine policy | Implement |
| local facade + Proto | Add / Spec §7/§9 | external canonical facade provider/clients | decorative Proto or dual stack creates drift | 2 modules × 5 sources，21 operations | Implement Steps 7/11 |
| new Proto DTO mapper layer | no approved necessity | current adapter converters/facade impls already own mapping | change existing converters/impls directly | extra files/calls without value | Do not add |
| DTP executor config class | bounded executor required / Spec §7 | three existing `AsyncConfiguration` return `null`；Boot provides `applicationTaskExecutor` | configure and decorate existing executor | no new layer，one existing bean governed | Reuse in Steps 4/8/12 |
| DTP Admin | Remove / Spec §7.0 | separate component module/README | external deployment | second service/UI/ops | Do not add |
| Gateway module/dependency | Remove / DEC-003 | no current generated Gateway source | external platform + Springdoc | no generated call/state | Forbid and verify |
| Dubbo in Light | RPC consumer absent / REQ-016 | Light POM declares starter but no provider/client symbol | remove from Light open | reduces unused runtime/startup | Remove Step 4 |
| Springdoc in Service | N/A / REQ-015 | no business Controller | no API docs dependency | avoids unused surface | Keep absent |
| native gRPC server | Remove / DEC-004 | Triple already owns port/registry | standard test client only | avoids second server/port | Do not add |
| Common MP Starter | Remove / DEC-001 | Starter adds tenant/audit/delete | official MP + direct PO | avoids global semantic change | Do not add |
| frontend application | N/A / Spec §12 | no JS/TS/UI tree | framework Swagger UI only | no page state | Do not add |

No fetch-then-forward interaction is introduced。In particular，ScheduleCourse and RecordScore keep `class_id/student_id`
as caller-owned positive cross-service references，matching current use cases；the Plan does not add Organization lookups。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| open skeletons | `REQ-001`-`004`,`006`,`018` | Maven module/package checks | original templates | three copy baselines | all later Steps | Step 1 |
| Light Long identity | `REQ-009`,`017`,`021` | Long/sharding/domain tests | Step 1 | Long Light call chain | Light Mapper/schema | Step 2 |
| Light persistence/manual SQL | `REQ-008`-`012`,`017`,`019`,`021` | repo/manual SQL tests | Step 2 | 8 Mapper + two scripts | Light runtime | Step 3 |
| Light runtime governance | `REQ-005`-`007`,`013`,`015`-`019` | Async/OpenAPI/ArchUnit/config tests | Step 3 | runnable open Light contract | cross-family verifier | Step 4 |
| Service Long identity | `REQ-009`,`017`,`021` | Long/sharding/use-case tests | Step 1 | Long Evaluation call chain | Service Mapper/Proto | Step 5 |
| Service persistence/manual SQL | `REQ-008`-`012`,`017`,`019`,`021` | repo/manual SQL tests | Step 5 | 5 Mapper + two scripts | Service Proto runtime | Step 6 |
| Service local Proto/RPC | `REQ-002`,`005`,`012`,`014`,`017`-`019`,`021` | descriptor/provider/client/interop tests | Step 6 | seven modules + 21-contract copy | Web parity/verifier | Step 7 |
| Service runtime governance | `REQ-005`-`007`,`013`,`015`-`019` | Async/ArchUnit/config tests | Step 7 | external-free runnable Service | cross-family verifier | Step 8 |
| Web Long identity | `REQ-009`,`017`,`021` | HTTP/domain/sharding tests | Step 1 | Long Organization call chain | Web Mapper/Proto | Step 9 |
| Web persistence/manual SQL | `REQ-008`-`012`,`017`,`019`,`021` | repo/manual SQL tests | Step 9 | 8 Mapper + two scripts | Web Proto/runtime | Step 10 |
| Web local Proto/RPC | `REQ-003`,`005`,`012`,`014`,`017`-`019`,`021` | descriptor/provider/client/interop tests | Steps 7、10 | seven modules + provider/client | runtime/verifier | Step 11 |
| Web runtime governance | `REQ-005`-`007`,`013`,`015`-`019` | Async/OpenAPI/ArchUnit/config tests | Step 11 | runnable Web，no Gateway | verifier/docs | Step 12 |
| final open contracts | `REQ-001`-`019`,`021` | three verify.groovy + full reactor | Steps 2-12 | negative/parity/generated proof | CI/public docs | Step 13 |
| CI/release/documentation | `REQ-004`,`006`,`015`,`018`-`020` | CI static/full commands | Step 13 | discoverable/releasable family | user implementation review | Step 14 |

## 5. Change File Tree

Mechanical-copy groups are exact target directory sets derived once from baseline `git ls-files`；all transformed families
below name their target paths/symbols。`target/**` is GENERATED and never committed。

```text
egon-cola-archetypes/pom.xml                                      # MODIFY Step 1
egon-cola-archetypes/egon-cola-archetype-light-open/             # CREATE mirror Step 1; MODIFY Steps 2-4,13
egon-cola-archetypes/egon-cola-archetype-service-open/           # CREATE mirror Step 1; MODIFY Steps 5-8,13
egon-cola-archetypes/egon-cola-archetype-web-open/               # CREATE mirror Step 1; MODIFY Steps 9-13
├── src/main/resources/META-INF/maven/archetype-metadata.xml      # Service/Web facade module Steps 7/11
├── src/main/resources/archetype-resources/
│   ├── pom.xml                                                   # version/dependency/module slices
│   ├── __rootArtifactId__-facade/                               # CREATE Steps 7/11
│   │   ├── pom.xml
│   │   └── src/main/proto/{evaluation,organization}/v1/*.proto
│   ├── **/domain/**, **/application/**, **/adapter/**            # Long/RPC mapping slices
│   ├── **/infrastructure/**/repo/{mapper,po,impl}/**             # 21 Mapper migration
│   ├── **/resources/mybatis/mapper/**/*.xml                      # CREATE Steps 3/6/10
│   ├── **/resources/db/manual/postgresql/**                     # CREATE 6 SQL + 3 README
│   ├── **/config/datasource/SnowflakeLongShardingAlgorithm.java # RENAME/modify Steps 2/5/9
│   ├── **/config/async/AsyncConfiguration.java                  # MODIFY Steps 4/8/12
│   ├── **/test/**                                                # focused RED/GREEN tests
│   ├── README.md, README.zh-CN.md, deploy/**                    # runtime/manual docs
│   └── target/generated-sources/protobuf/java/**                # GENERATED, never commit
└── src/test/resources/projects/basic/verify.groovy              # MODIFY Steps 13
.github/workflows/ci_java_compatibility.yaml                      # MODIFY Step 14
README.md, README.zh-CN.md                                        # MODIFY Step 14
scripts/maven-deploy.md                                           # MODIFY Step 14
egon-cola-archetypes/architecture-mermaid-diagrams-open.md        # CREATE Step 14
egon-cola-archetypes/code-style-abstract-open.md                   # CREATE Step 14
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-archetypes/pom.xml` | five modules | adds three `-open` modules only | reactor aggregation | 1 | `REQ-001`-`004` | parent Maven |
| CREATE/MODIFY | `egon-cola-archetypes/egon-cola-archetype-light-open/**` | target absent；source light has 450 tracked files | isolated single-module open product | Light template | 1-4、13 | mapped Light REQs | Light Invoker |
| CREATE/MODIFY | `egon-cola-archetypes/egon-cola-archetype-service-open/**` | target absent；source service has 351 tracked files | seven-module Service open product | Service template | 1、5-8、13 | mapped Service REQs | Service Invoker |
| CREATE/MODIFY | `egon-cola-archetypes/egon-cola-archetype-web-open/**` | target absent；source web has 440 tracked files | seven-module Web open product | Web template | 1、9-13 | mapped Web REQs | Web Invoker |
| CREATE | `**-open/**/repo/mapper/*Mapper.java` | 21 JPA repositories | 21 `BaseMapper<PO>` + exact custom methods | persistence access | 3、6、10 | `REQ-008`,`017` | repository tests |
| CREATE | `**-open/**/resources/mybatis/mapper/**/*.xml` | derived queries in JPA names | explicit logical-table SQL/order/page/composite predicates | SQL mapping | 3、6、10 | `REQ-008`,`009`,`017` | H2/route tests |
| DELETE | `**-open/**/repo/jpa/**` | 21 `JpaRepository` + package-info | absent | forbidden source removal | 3、6、10 | `REQ-008` | rg/verifier |
| DELETE | `**-open/**/PhysicalDataSourceFlywayMigrator*` and `**/migration/**` | migrator/tests/db/migration | absent；manual resources replace | schema ownership | 3、6、10 | `REQ-010`,`011` | manual SQL tests |
| CREATE | `**-open/**/db/manual/postgresql/**` | no manual directories | six SQL + three runbooks | DBA schema contract | 3、6、10 | `REQ-011`,`021` | SQL tests |
| RENAME | `**-open/**/UuidV7BucketShardingAlgorithm*.java` -> `SnowflakeLongShardingAlgorithm*.java` | String UUID strategy/tests | `StandardShardingAlgorithm<Long>` exact stable hash | route contract | 2、5、9 | `REQ-009`,`021` | route tests |
| CREATE | `service-open/**/__rootArtifactId__-facade/**` | generated local module absent | five Proto sources/codegen/test | local contract | 7 | `REQ-014`,`018` | descriptor/compile |
| CREATE | `web-open/**/__rootArtifactId__-facade/**` | generated local module absent | same wire Proto sources/codegen/test | local contract | 11 | `REQ-014`,`018` | parity/compile |
| MODIFY | three open `AsyncConfiguration.java` + runtime YAML/POM | returns null；no DTP | bounded decorated Boot executor and disabled test profile | executor governance | 4、8、12 | `REQ-013`,`019` | context tests |
| CREATE | three open `OpenArchitectureTest.java` | internal plugin currently proves boundaries | ArchUnit 1.4.2 rules | architecture proof | 4、8、12 | `REQ-005`,`012` | JUnit/verify |
| MODIFY | three open `verify.groovy` | copied assertions require JPA/Flyway/UUID and forbid MP/DTP | final positive/negative/generated contract | product verifier | 13 | all runtime REQs | Maven Invoker |
| MODIFY | `.github/workflows/ci_java_compatibility.yaml` | loop lists original three | loop lists six and compares Proto sources | CI proof | 14 | `REQ-004`,`018`,`019` | workflow/static |
| MODIFY/CREATE | root/public docs listed above | only original products documented | bilingual open discovery/architecture/release | public contract | 14 | `REQ-015`,`018`,`020` | docs/rg |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Apply prompt-provided AGENTS rules：smallest safe change，existing JavaDoc/style，one tested commit per Step，no project start。
- No applicable repository-local `AGENTS.md` exists under `/Users/mario/SelfProject/Egon-COLA`。
- Branch/revision：`main@772df2b28b4abf29e7fffae6ea1b10fd616397b4`。
- Existing dirty/untracked work includes IdP/DDC/Gateway/RBAC/platform scripts、two identity docs、Access Guard Spec/Plan
  and `V6__backfill_confidential_client_app_ids.sql`。None overlaps the three new open directories、archetypes parent、root
  README、CI or deploy guide；execution must use explicit `git add <Step commit paths>` and inspect `git diff --cached --name-only`。
- Never stage `target/**`、generated Proto Java、Invoker `project/basic` output or local `.m2` artifacts。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Maven/Java | `./mvnw -version` from repo root | wrapper 3.9.14，JDK 21 | local toolchain only |
| Approved Egon artifacts | `./mvnw -B -ntp -pl egon-cola-components/egon-cola-components-bom,egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am install -DskipTests` | 5.3.3 BOM/core/id/trace/DTP locally resolvable | local Maven repo，not Central proof |
| Focused archetype | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-<kind>-open -am integration-test` | target module exists after Step 1 | Maven Invoker/generated compile/tests |
| Full archetype | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test` | all Step 13 contracts complete | six archetypes + existing facades；no live infra |
| Static | `rg` forbidden scans、`git diff --check`、`git diff --cached --name-only` | no forbidden/unrelated/generated path | source/build model only |
| Manual PostgreSQL | DBA follows generated `db/manual/postgresql/README.md` | not executed by agent/CI | user-controlled release gate |

### 6.3 Immutable constraints and approved decisions

- Original three archetypes、their six existing Flyway SQL、components/platforms production source are immutable for this work。
- Open copies contain no Flyway migration；new manual SQL is initial schema，not an edit/new version in original `classpath:db`。
- Domain/application behavior、HTTP/GraphQL/MQ semantics and service-only constraint remain；only approved ID/persistence/RPC representation changes。
- `top.egon` direct allowlist is Components BOM、common-core、common-id-starter、DTP starter；common-trace only transitive。
- Gateway remains external；no dependency/property/route/module。Service has no Springdoc；Light/Web use 2.8.17。
- `EGON_ID_MACHINE_ID` is required in runtime and unique per JVM；tests use deterministic fake/explicit `0`。
- No automatic mutation retry for Proto commands；read 3s、command 5s；error code/trace travel in trailing metadata。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | Replace 21，not 19，JPA repositories：Light 8、Service 5、Web 8 | baseline `find ... -name '*JpaRepository.java'` returns 21；Spec already has 21 PO | completes the same literal no-JPA requirement；no contract/schema addition | omitting two leaves forbidden source/build failure |
| `PLAN-CLAR-002` | Reuse existing `AsyncConfiguration` and Boot `applicationTaskExecutor`，configure core=8、max=32、queue=1000、keepAlive=60s through env-overridable `spring.task.execution`，add `DtpTaskDecorator` bean | all three copied classes return null；DTP README uses 8/32/1000/60 and auto-discovers `ThreadPoolTaskExecutor` beans | same one bounded governed executor required by Spec；no new class/layer or call | different capacity affects performance only and remains runtime-configurable |
| `PLAN-CLAR-003` | Copy completeness is computed from baseline `git ls-files` into temporary `target/open-copy-manifest/*.txt`；manifest is verification-only and uncommitted | source counts are 450/351/440；Spec requires one-to-one copy | deterministic proof without adding product artifact | target omission/extra file fails Step 1 before commit |
| `PLAN-CLAR-004` | Proto Java output is generated by `dubbo-maven-plugin:3.3.6` into `target/generated-sources/protobuf/java`；providers implement generated `CourseService/ExamService/...` interfaces directly | official 3.3 plugin and current `@DubboService`/ServiceBean patterns | changes only local class/import syntax under approved Proto authority | plugin-generated symbol drift returns to Step 7 contract compile，not hand editing generated code |

## 7. Ordered File-by-file Implementation Steps

> 每个 Step 从声明的 baseline 开始，先建立 RED/contract gate，再形成一个 GREEN semantic commit。所有命令均是
> 将来执行指令，不是本 Plan 已运行的实现证据。

### Step 1 — 建立三个隔离的 Open Archetype 复制基线

- Requirements: `REQ-001`,`REQ-002`,`REQ-003`,`REQ-004`,`REQ-006`,`REQ-018`
- Dependencies: None；执行前 Spec 与本 Plan 必须由用户明确批准。
- Baseline state: parent 聚合两个 facade + 三个原 archetype；三个 `-open` target 不存在；原 reactor 当前可构建。
- Observable outcome: parent 可以识别三个 additive `-open` Maven archetype，目标树与 baseline tracked files 一一对应。
- End state: open copies仍保持原 JPA/Flyway/UUID behavior，作为后续 transformation baseline；原目录无改动。
- Test-first gate: `Not applicable — deterministic directory duplication/coordinate registration has no behavior implementation seam；file-count/hash and Maven package are the proof gates.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/pom.xml`

- Purpose: 聚合三个新 artifact，同时保持原五个 module 的相对顺序和内容。
- Symbols: Maven reactor module entries `egon-cola-archetype-light-open/service-open/web-open`。
- Repository evidence: baseline lines 54-60 list facade、light、service、web；all archetypes inherit this parent。
- Dependencies and consumers: Maven reactor consumes child POMs；CI/full integration later selects parent。
- Why now: child coordinates不能在 parent 未声明时用 `-pl :artifactId` 验证。
- Contract/signature changes: append exactly three module entries；no version/plugin/dependency change。
- Input/output and state mapping: repository directory -> reactor module；no runtime/data state。
- Error and edge behavior: duplicate/missing module fails Maven model；original module removal is forbidden。
- Implementation pseudocode:

```xml
keep organization/evaluation facade and original light/service/web module entries unchanged
append light-open, service-open, web-open exactly once under the Maven reactor modules element
assert no dependencyManagement, profile, plugin, or release setting changes in this file
```

- Verification contribution: parent `validate/package` resolves all eight modules。
- After this file: Maven expects three child POMs；repository is intentionally incomplete until Files 2-4 exist。

#### File 2 — `CREATE egon-cola-archetypes/egon-cola-archetype-light-open/`

- Purpose: 建立 Light Open 的 deterministic copy baseline。
- Symbols: archetype artifactId/name/description suffix、metadata、post-generate、450-file source manifest、basic Invoker project。
- Repository evidence: `egon-cola-archetype-light` has 450 tracked files and single-module generated shape。
- Dependencies and consumers: File 1 reactor；future Steps 2-4；Invoker consumes metadata/resources/verifier。
- Why now: semantic migration must happen only in isolated target。
- Contract/signature changes: outer artifact/name/description become `egon-cola-archetype-light-open`；generated caller properties unchanged。
- Input/output and state mapping: every tracked source-relative path copied to same relative target；only outer product identity normalized。
- Error and edge behavior: source missing/target extra/hash drift fails temporary manifest comparison；never copy `target/`。
- Implementation pseudocode:

```text
source_set = git ls-files egon-cola-archetype-light excluding target
create target paths with identical bytes; change only outer POM artifact/name/description to *-light-open
compare sorted relative-path manifests and assert copied baseline generated project still reaches verify
```

- Verification contribution: Light Open outer package and basic Invoker baseline。
- After this file: Light Open is a publishable but not yet final open-stack copy。

#### File 3 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/`

- Purpose: 建立 Service Open 的 deterministic copy baseline。
- Symbols: service-open artifact identity、351-file manifest、six-module generated baseline、Invoker fixture。
- Repository evidence: `egon-cola-archetype-service` has 351 tracked files and current facade test dependencies。
- Dependencies and consumers: parent reactor；Steps 5-8/13 transform only this tree。
- Why now: preserve service-only architecture while separating product lifecycle。
- Contract/signature changes: outer coordinate suffix only；generated `rootArtifactId/package` inputs unchanged。
- Input/output and state mapping: exact relative copy；no module/facade/schema redesign in this Step。
- Error and edge behavior: manifest mismatch or unresolved copied facade fixture blocks commit；no fallback to original target edits。
- Implementation pseudocode:

```text
derive 351-file tracked manifest from service source and reproduce it under service-open
normalize outer POM artifactId/name/description while retaining Invoker properties and current six-module output
run baseline generation; reject missing executable bits, wrapper, metadata, or verification fixture
```

- Verification contribution: Service Open coordinate/package can be selected independently。
- After this file: Service Open still uses copied external facades/JPA/Flyway/UUID until Steps 5-8。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/`

- Purpose: 建立 Web Open 的 deterministic copy baseline。
- Symbols: web-open artifact identity、440-file manifest、six-module generated baseline、Invoker fixture。
- Repository evidence: `egon-cola-archetype-web` has 440 tracked files and HTTP/GraphQL/Springdoc contracts。
- Dependencies and consumers: parent reactor；Steps 9-12/13 transform only this tree。
- Why now: protect original Web while preserving complete business fixtures for migration。
- Contract/signature changes: outer coordinate suffix only；generated caller properties unchanged。
- Input/output and state mapping: all tracked bytes/permissions copied；no browser/runtime process。
- Error and edge behavior: missing docs/deploy/resource/test file fails manifest；copied `target` content is forbidden。
- Implementation pseudocode:

```text
derive 440-file tracked manifest from web source and reproduce it under web-open
change only outer POM artifactId/name/description; retain current generated artifact/root/package contract
assert baseline Maven Invoker produces the copied six-module Web project before semantic migration
```

- Verification contribution: Web Open coordinate and copy completeness。
- After this file: all three isolated baselines exist and later commits need not touch originals。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light-open,:egon-cola-archetype-service-open,:egon-cola-archetype-web-open -am package -DskipTests`
- Expected result: exit 0；all three maven-archetype artifacts package；temporary relative-path counts are 450/351/440；no `target/**` staged。
- Failure returns to: Files 2-4 for copy/identity mismatch；File 1 for reactor resolution。
- Completion criteria: three additive artifacts exist，original path diff is zero，copied baselines package。
- Rollback: path-limited revert of parent module entries and deletion/revert of only the three new directories。
- Commit paths: `egon-cola-archetypes/pom.xml` `egon-cola-archetypes/egon-cola-archetype-light-open/` `egon-cola-archetypes/egon-cola-archetype-service-open/` `egon-cola-archetypes/egon-cola-archetype-web-open/`
- Commit: `feat(archetype): add open edition copy baselines`

### Step 2 — 将 Light Open 主键与分片链迁为 Snowflake Long

- Requirements: `REQ-001`,`REQ-005`,`REQ-009`,`REQ-012`,`REQ-017`,`REQ-021`
- Dependencies: Step 1 committed。
- Baseline state: Light Open compiles with String/UUID domain、PO、adapter and UUID sharding algorithm。
- Observable outcome: every generated Light technical ID is positive Long internally，HTTP remains decimal String，routing uses the approved Long hash。
- End state: copied JPA still temporarily persists Long PO；Flyway copy still exists solely until Step 3；no UUID generator/call site remains。
- Test-first gate: `Required — copied Long/domain/sharding tests fail to compile or assert UUID/String behavior before production signatures are changed.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/{domain,application,adapter,infrastructure}/**/*.java`

- Purpose: 先冻结 Long value、decimal boundary、one-ID-per-command 和 stable-slot behavior。
- Symbols: `UserIdTest`/`SchoolClassIdTest`，manage/use-case tests，filter/controller tests，`ShardingNodeMapTest`，repository fixtures。
- Repository evidence: copied tests already assert UUID strings and current business results without live services。
- Dependencies and consumers: tests call existing public domain/application/adapter/repository symbols；fake `LongIdGenerator` replaces UUID fixture supplier。
- Why now: fixes RED contract before changing signatures/fields。
- Contract/signature changes: test IDs become `long`/decimal String；invalid zero/negative/overflow cases added；one generator invocation asserted。
- Input/output and state mapping: fake `42L` -> Domain/PO `42L` -> HTTP `"42"`；trace/request fallback -> `nextId()` decimal String。
- Error and edge behavior: `0/-1/non-digit/overflow` rejected；no UUID fallback；transaction failure does not persist generated row。
- Implementation pseudocode:

```java
arrange LongIdGenerator.nextLongId() returns 42L and existing command fixture
assert domain/repository receives 42L while HTTP/GraphQL response exposes "42"
assert routeSlot(42L) follows Long.hashCode/spread/mask and invalid keys fail before SQL
```

- Verification contribution: expected RED compile/assertion set for Long migration。
- After this file: tests describe final ID behavior and fail for old String/UUID production signatures。

#### File 2 — `RENAME egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java -> egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithm.java`

- Purpose: 替换 UUID-specific ShardingSphere SPI，同时保留 database/table target selection。
- Symbols: `SnowflakeLongShardingAlgorithm implements StandardShardingAlgorithm<Long>`，`doSharding` precise/range，`ShardingNodeMap.routeSlot(long)`。
- Repository evidence: copied class delegates database/table selection to one node map；current available-target validation is reusable。
- Dependencies and consumers: YAML class name、ShardingNodeMap、route tests、ShardingSphere SPI。
- Why now: Long domain/PO writes require matching generic route type before integration tests compile。
- Contract/signature changes: String -> Long；exact formula from Spec §7.3；range remains unsupported。
- Input/output and state mapping: positive Long -> hash/spread/slot -> existing `PhysicalNode` -> actual target name。
- Error and edge behavior: null/non-positive/range/absent target/malformed properties fail closed；node-count change remains offline-only。
- Implementation pseudocode:

```java
routeSlot(long key): require key > 0; hash=Long.hashCode(key); spread=hash^(hash>>>16); return spread&(nodeCount-1)
doSharding(available, PreciseShardingValue<Long>): node=nodeMap.route(value); select exact database or _suffix target
doSharding(available, RangeShardingValue<Long>): throw UnsupportedOperationException without broadcast
```

- Verification contribution: renamed focused algorithm/node-map tests become GREEN。
- After this file: routing core accepts Long only；YAML references are still RED until File 5。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java -> egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithmTest.java`

- Purpose: align the focused test identity with the production SPI and retain exact target/range assertions。
- Symbols: `routesLongToSameDatabaseAndTableSlot`、`rejectsNonPositiveKey`、`rejectsRangeRoute`、`rejectsMissingTarget`。
- Repository evidence: copied test already covers database/table target branch and range rejection for UUID。
- Dependencies and consumers: File 2 production class；no Spring context/live DB。
- Why now: rename after production symbol exists，then drive exact GREEN behavior。
- Contract/signature changes: all fixture keys `Long`；UUID version/canonical-string cases removed。
- Input/output and state mapping: known keys -> expected `shard_N`/`table_suffix`；available target collection is authoritative。
- Error and edge behavior: duplicate/missing/mismatched targets throw deterministic IllegalArgumentException。
- Implementation pseudocode:

```java
initialize algorithm with node-count=4 and exact slot map properties
assert precise Long key selects the database and table belonging to one PhysicalNode
assert zero, negative, range, and unavailable target paths throw without returning all targets
```

- Verification contribution: primary unit proof for `TEST-019/028/030` semantics。
- After this file: routing test suite is GREEN independent of remaining Light call-chain migration。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/{domain,application,adapter,facade}/**/*.java`

- Purpose: migrate Light semantic layers and HTTP/RPC-neutral boundary mapping without changing use cases。
- Symbols: `UserId(long)`、`SchoolClassId(long)`，Course/User/Class/Schedule IDs，commands/queries/results，manage/domain services，controllers/GraphQL/VO/facade。
- Repository evidence: copied layer/package tree and converters already isolate transport/domain/persistence types。
- Dependencies and consumers: adapters call application；application/domain repository ports；File 1 tests；Infrastructure updated next。
- Why now: establish authoritative internal Long signatures before persistence conversion。
- Contract/signature changes: technical IDs/foreign IDs -> `long/Long`；business codes/externalId/idempotency input remain String；HTTP IDs remain decimal String。
- Input/output and state mapping: adapter validates regex/overflow -> long；application calls `nextLongId()` exactly once；response formats `Long.toString`。
- Error and edge behavior: malformed IDs use existing validation/error wrapper；duplicate/missing/transaction/event semantics unchanged。
- Implementation pseudocode:

```java
adapter parses required decimal transport ID with Long.parseLong and requires value > 0 before command/query creation
@Transactional manage method obtains idGenerator.nextLongId(), invokes existing domain invariant, saves through unchanged port
converter maps domain long -> HTTP/GraphQL decimal String and preserves code/status/time/list ordering
```

- Verification contribution: domain/application/adapter tests move from RED to GREEN for Long mapping。
- After this file: inward layers compile with Long；Infrastructure adapters must now conform in File 5。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/{start,infrastructure}/**/*.java`

- Purpose: finish Light Long persistence/cache/MQ/config wiring while still using copied JPA for this intermediate commit。
- Symbols: eight PO Long fields、RepositoryImpl/converters、cache keys、event/message decimal IDs、`StudentManagementApplication`、Sharding config properties。
- Repository evidence: copied PO/RepositoryImpl/converter boundary centralizes DB mapping；application currently declares deprecated UUID bean。
- Dependencies and consumers: File 4 Long ports/results；JPA repositories temporarily use Long generic IDs；File 2 algorithm。
- Why now: closes compile path without changing persistence technology in the same semantic Step。
- Contract/signature changes: JPA ID generics/fields -> Long；remove `UuidV7Generator` bean；inject auto-configured `LongIdGenerator`。
- Input/output and state mapping: Domain Long <-> PO Long；cache/MQ/string protocols use decimal formatting；sharding keys Long。
- Error and edge behavior: missing machine-id context failure is fully configured in Step 4；tests use explicit fake/property；no UUID fallback。
- Implementation pseudocode:

```java
PO/converter/repository signatures carry Long for technical and foreign IDs while business code PKs stay String
remove manual UuidV7Generator @Bean; constructor injection resolves LongIdGenerator from Common ID auto-configuration
format cache/event/message identifiers as decimal strings and keep transaction-after-commit behavior unchanged
```

- Verification contribution: generated Light compile and focused business/repository tests。
- After this file: all Light source IDs are Long/decimal and UUID production imports are absent；JPA/Flyway removal remains Step 3。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/sharding/{shardingsphere-sharding.yml,shardingsphere-sharding-readwrite.yml}`

- Purpose: bind logical table rules to the renamed Long algorithm and exact repository-native keys。
- Symbols: `SNOWFLAKE_LONG_*` algorithm definitions；class `id` and schedule `school_class_id` columns。
- Repository evidence: copied YAML has the correct logical/physical topology but UUID class name/properties。
- Dependencies and consumers: File 2 SPI；ShardingSphere factory；route integration tests。
- Why now: final compile path needs configuration to instantiate the new class。
- Contract/signature changes: algorithmClassName and name only；node-count/node-map/actualDataNodes remain exact。
- Input/output and state mapping: YAML properties -> `ShardingNodeMap` -> Long route；no Flyway or schema action here。
- Error and edge behavior: topology/mapping invalidity fails context；no default/range broadcast rule added。
- Implementation pseudocode:

```yaml
replace every UUID algorithm reference with SnowflakeLongShardingAlgorithm and preserve node-count/node-map
keep school_classes shardingColumn=id and schedules shardingColumn=school_class_id for database and table rules
assert SHARDING and SHARDING_READWRITE files use identical route properties and table topology
```

- Verification contribution: datasource mode and read/write route tests can build the logical DataSource。
- After this file: Step's full generated Light call path is Long-compatible and ready for focused verification。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light-open -am integration-test`
- Expected result: exit 0；generated Light compiles/tests with Long IDs and copied JPA；renamed route tests pass；Java runtime path has no UUID/UuidV7 call site，while copied SQL remains an explicit temporary residue removed by Step 3。
- Failure returns to: File 1 for incorrect expected mapping；Files 2/3 for route failure；Files 4/5 for type/call-chain mismatch；File 6 for context load。
- Completion criteria: Light business/route tests pass，one ID per create，HTTP decimal mapping exact，no original Light edit。
- Rollback: revert only Step paths inside light-open；no database or published artifact exists。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/{domain,application,adapter,infrastructure}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java -> egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithm.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java -> egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithmTest.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/{domain,application,adapter,facade}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/{start,infrastructure}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/sharding/{shardingsphere-sharding.yml,shardingsphere-sharding-readwrite.yml}`
- Commit: `refactor(light-open): migrate identifiers to snowflake long`

### Step 3 — 用 MyBatis-Plus 与手工 SQL 完成 Light Open 持久化

- Requirements: `REQ-001`,`REQ-008`,`REQ-009`,`REQ-010`,`REQ-011`,`REQ-012`,`REQ-016`,`REQ-017`,`REQ-019`,`REQ-021`
- Dependencies: Step 2 committed。
- Baseline state: Light Open Long call chain GREEN，but generated POM/source/startup still use JPA/Flyway and copied migration resources。
- Observable outcome: eight Light Mapper contracts preserve repository behavior；application never executes schema；DBA receives two BIGINT scripts/runbook。
- End state: generated Light has official MP 3.5.17、no JPA/Flyway、explicit test SQL executor only；business tests GREEN。
- Test-first gate: `Required — repository/manual-schema tests first fail because Mapper/manual resources are absent and Flyway/JPA are still present.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/{user,teaching}/repo/**/*Test.java`

- Purpose: freeze all eight repository behaviors against Mapper fakes/H2 rather than JPA mocks。
- Symbols: save/find/count/list/order/composite-key tests for User/Role/Permission/UserRole/RolePermission/Course/Class/Schedule。
- Repository evidence: copied tests already assert domain results、duplicate/missing/cache/event behavior；only persistence collaborator changes。
- Dependencies and consumers: RepositoryImpl public ports；future Mapper interfaces/XML；explicit H2 schema helper。
- Why now: RED proves query/affected-row semantics before deleting JPA interfaces。
- Contract/signature changes: mocked `*JpaRepository` -> `*Mapper`；Long fixtures from Step 2；no domain port changes。
- Input/output and state mapping: domain -> PO -> insert/update affected count；rows -> deterministic domain lists；composite codes remain String。
- Error and edge behavior: zero affected row、duplicate unique、empty IN、case/order、transaction rollback asserted。
- Implementation pseudocode:

```java
arrange Mapper mock or explicit H2 rows matching each existing repository fixture
call unchanged Domain Repository port and assert exact Long IDs, code keys, ordering, and affected-row errors
verify no JPA EntityManager/session behavior and no schema creation occurs during ordinary context startup
```

- Verification contribution: focused RED/GREEN repository proof for 8 replacements。
- After this file: tests fail for missing Mapper types/methods until Files 3-5。

#### File 2 — `CREATE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/migration/{ManualSqlConventionTest.java,ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java}`

- Purpose: specify manual resource names、BIGINT schema、physical parity and explicit-only H2 execution。
- Symbols: six-header/order checks scoped to Light two scripts，`executeManually(DataSource, Resource...)` test helper。
- Repository evidence: copied `FlywayMigrationConventionTest`/migrator tests provide SQL parsing/topology fixtures but violate final ownership。
- Dependencies and consumers: new manual resources File 7；H2 test only；main JAR absence gate。
- Why now: establishes RED schema-delivery contract before resources/startup deletion。
- Contract/signature changes: no production migration API；test helper lives only under `src/test`。
- Input/output and state mapping: resource SQL -> explicit H2 DataSource -> expected 10 Light physical/master tables；fresh context -> zero tables。
- Error and edge behavior: missing header/target/order/column parity/duplicate seed or accidental main packaging fails test。
- Implementation pseudocode:

```java
assert resources exist only under db/manual/postgresql and contain BIGINT IDs with no UUID/Flyway/schema.sql token
start fresh H2 context without helper and assert application creates no tables
invoke ManualSchemaTestSupport explicitly, then assert master/shard table columns, keys, and suffix parity
```

- Verification contribution: `TEST-012/013/016-018` Light slice。
- After this file: manual tests RED because final resources and no-init startup are not yet present。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/pom.xml`

- Purpose: select official MP and remove forbidden JPA/Flyway/build-plugin dependencies for Light。
- Symbols: `mybatis-plus.version=3.5.17`，`mybatis-plus-spring-boot3-starter`；remove JPA/Flyway deps/profile/plugin。
- Repository evidence: copied POM lines 150/175/179/322 declare forbidden artifacts；Common ID/core/BOM and Sharding deps already exist。
- Dependencies and consumers: PO/Mapper compile；runtime datasource；Step 4 later adds DTP/ArchUnit and removes unused Dubbo。
- Why now: Mapper types must resolve before source conversion；removal deliberately makes old JPA/Flyway sources fail until following files。
- Contract/signature changes: no Common MP/raw MyBatis；H2/PostgreSQL remain test/runtime drivers；`spring.sql.init.mode=never` config later。
- Input/output and state mapping: Maven dependency tree -> approved open runtime；no DB side effect。
- Error and edge behavior: dependency convergence/unapproved `top.egon`/Flyway/JPA artifacts fail verifier。
- Implementation pseudocode:

```xml
pin mybatis-plus-spring-boot3-starter 3.5.17 and keep approved Components BOM/core/id
remove spring-boot-starter-data-jpa and flyway-core/database/plugin/profile sections owned here
retain the copied internal architecture plugin only until Step 4 replaces it atomically with ArchUnit
retain ShardingSphere 5.5.3, PostgreSQL runtime, H2 test/runtime and existing business feature dependencies
```

- Verification contribution: effective-POM/dependency-tree gate and Mapper compilation。
- After this file: build is intentionally RED on old imports until Files 4-6；JPA/Flyway dependencies are gone，and the copied architecture plugin remains only until Step 4。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/{user,teaching}/repo/mapper/*Mapper.java`

- Purpose: replace all eight JPA repository contracts with narrow MP Mapper interfaces。
- Symbols: `UserMapper/RoleMapper/PermissionMapper/UserRoleMapper/RolePermissionMapper/CourseMapper/SchoolClassMapper/ClassCourseScheduleMapper`。
- Repository evidence: 8 copied `repo/jpa/*JpaRepository` methods enumerate exact required operations。
- Dependencies and consumers: `BaseMapper<PO>`；RepositoryImpl callers；XML statement IDs in File 5。
- Why now: compile contract precedes RepositoryImpl rewiring。
- Contract/signature changes: simple CRUD via BaseMapper；custom methods name exact code/list/composite/order predicates with Long IDs。
- Input/output and state mapping: PO/Long/code collection/Page params -> row count/list/PO；no Domain types leak。
- Error and edge behavior: empty collections return empty before XML call；affected rows interpreted by RepositoryImpl。
- Implementation pseudocode:

```java
interface UserMapper extends BaseMapper<UserPO>; define only custom operations absent from BaseMapper
define findByUserId/orderByStartsAt and role/permission collection methods with @Param exact names
return PO/list/long affected counts; never expose Domain entity, Spring Data Page, or generated SQL migration
```

- Verification contribution: repository tests compile and bind against exact Mapper names。
- After this file: Mapper Java contracts exist；custom statements await XML File 5。

#### File 5 — `CREATE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/mybatis/mapper/{user,teaching}/*Mapper.xml`

- Purpose: encode JPA-derived queries explicitly on logical ShardingSphere table names。
- Symbols: statement IDs matching File 4；result maps for all eight PO；stable schedule/code ordering。
- Repository evidence: copied JPA method names and SQL schema define predicates/composite keys；current RepositoryImpl fixes call order。
- Dependencies and consumers: MyBatis Mapper namespace；logical datasource routes physical tables。
- Why now: custom methods cannot execute until namespace/statement/result mapping exists。
- Contract/signature changes: explicit columns；no `SELECT *`；Long JDBC BIGINT；code/time/status mappings exact。
- Input/output and state mapping: Long/code/list/time inputs -> logical SQL -> PO/list/count；timestamps stay `Instant` mapping。
- Error and edge behavior: schedule order `starts_at,id`；composite relation predicates include all key columns；no broadcast write。
- Implementation pseudocode:

```xml
declare resultMap with every PO field/column and BIGINT technical IDs
write logical-table SELECT/INSERT/UPDATE predicates matching each former derived query and deterministic ordering
bind collections with foreach only after RepositoryImpl rejects empty input; never name physical _0/_1 tables
```

- Verification contribution: H2 Mapper and Sharding integration tests。
- After this file: Mapper custom queries are executable；PO annotations/RepositoryImpl still require File 6。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/{user,teaching}/repo/{po,impl,converter}/**/*.java`

- Purpose: remove Jakarta persistence and compose RepositoryImpl with Mapper while preserving port semantics。
- Symbols: 8 PO `@TableName/@TableId(type=INPUT)` or composite mapping；8 RepositoryImpl constructor fields/calls；converters。
- Repository evidence: copied PO fields and RepositoryImpl methods are one-to-one conversion boundary；Step 2 already changed Long types。
- Dependencies and consumers: File 4/5 Mappers；Domain Repository ports；Spring transactions in Application。
- Why now: completes GREEN persistence path after SQL/contract exist。
- Contract/signature changes: delete JPA annotations/embedded keys/proxy constructors；use MP annotations/direct simple PO；assert affected rows。
- Input/output and state mapping: Domain Long/status/time -> PO -> Mapper；Mapper PO/list/page -> converters -> same Domain result。
- Error and edge behavior: missing/duplicate/zero affected/composite idempotence mapped to current exceptions；no lazy/session semantics。
- Implementation pseudocode:

```java
PO classes declare logical @TableName and explicit @TableId(INPUT) only for surrogate IDs; composite joins use fields only
RepositoryImpl calls mapper.select/insert/update/custom methods, validates affected count, and converts PO/domain explicitly
preserve transaction ownership in Application and never call schema tools, physical table names, or MP service abstraction
```

- Verification contribution: all eight repository tests reach GREEN with no JPA imports。
- After this file: Light business persistence is fully MP-based；old JPA files can be deleted in File 8。

#### File 7 — `CREATE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/db/manual/postgresql/{README.md,master-data/001__create_light_master_data_schema.sql,shard/002__create_light_sharded_schema.sql}`

- Purpose: deliver operator-owned final BIGINT schema and execution/verification order。
- Symbols: 10 physical/master Light tables；constraints/indexes from Spec §11；no runtime executor/history。
- Repository evidence: copied two Flyway SQL contain exact existing business columns/constraints and are template initialization only。
- Dependencies and consumers: DBA/runbook；test-only helper；Sharding YAML topology。
- Why now: Mapper integration needs the final schema contract and manual tests already RED。
- Contract/signature changes: UUID columns -> BIGINT；files/headers/manual order changed；business columns/relations preserved。
- Input/output and state mapping: DBA executes 001 on master primary，002 on every shard primary；replicas inherit via replication。
- Error and edge behavior: partial failure stops rollout；README records checksum/verify/restore or forward-fix；app never repairs/retries DDL。
- Implementation pseudocode:

```sql
001: CREATE users/user_roles/courses and unchanged code-key role/permission tables with BIGINT technical IDs
002: CREATE school_classes_0/1 and class_course_schedules_0/1 with BIGINT keys, matching constraints and parity
README: map target roles/order/prechecks/checksums/verification/restore boundary; prohibit app/Flyway execution
```

- Verification contribution: ManualSqlConvention/SchemaIntegration tests and DBA release gate。
- After this file: final manual schema exists；copied migration resources remain only until File 8 deletion。

#### File 8 — `DELETE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/{java/infrastructure/config/datasource/PhysicalDataSourceFlywayMigrator.java,resources/db/migration/**}`

- Purpose: remove all production Flyway execution/source resources after their manual replacement exists。
- Symbols: `PhysicalDataSourceFlywayMigrator` and two copied `V20260726_*` files。
- Repository evidence: bootstrapper currently invokes migrator and copied migrations live at classpath migration path。
- Dependencies and consumers: callers removed in File 9；manual resources File 7 replace schema content。
- Why now: delete only after forward replacement and tests are defined。
- Contract/signature changes: no replacement runtime migration API；schema prerequisite is operational。
- Input/output and state mapping: removes startup DDL state transition；business JDBC state unaffected。
- Error and edge behavior: search must prove no caller/resource reference；missing schema surfaces readiness/SQL error，not auto-create。
- Implementation pseudocode:

```text
delete migrator class and copied db/migration SQL only inside light-open
search all source/POM/YAML/tests for Flyway class/location/history references and route each remaining caller to removal File 9
assert original light migration files remain byte-identical and present
```

- Verification contribution: negative source/resource/package scans。
- After this file: production migrator/resources absent；datasource compile is RED until File 9 removes callers/properties。

#### File 9 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/{ShardingDataSourceBootstrapper.java,ShardingDataSourceProperties.java,ShardingTopologyValidator.java,ShardingSphereDataSourceConfiguration.java}`

- Purpose: create/validate logical datasource without Flyway targets or automatic DDL。
- Symbols: bootstrap sequence、properties records、topology rules、`@MapperScan`/DataSource bean。
- Repository evidence: copied chain is physical pools -> validator/migrator -> YAML factory；only migration substep is removed。
- Dependencies and consumers: Sharding properties/YAML、Mapper interfaces、Spring context/readiness。
- Why now: File 8 removed migrator；this restores compile and final startup semantics。
- Contract/signature changes: remove `FlywayTargetProperties/ShardingFlywayProperties` and migrator arg/call；preserve pool/topology/node/readwrite validation。
- Input/output and state mapping: config -> physical Hikari pools -> validated topology -> logical ShardingSphere DataSource；zero schema writes。
- Error and edge behavior: missing schema/config/route fails closed；no SINGLE/unsharded fallback invented。
- Implementation pseudocode:

```java
bind only datasource/mode/routing physical properties and reject missing primary/readwrite/node mappings
bootstrap: create physical pools -> validate topology/rules -> create logical DataSource from public YAML factory
never instantiate migration strategy/tool or inspect/write schema history; expose Mapper DataSource only after validation
```

- Verification contribution: datasource mode/context/no-implicit-init tests。
- After this file: production datasource compiles and performs no DDL；config/test cleanup remains File 10。

#### File 10 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/application*.yml`

- Purpose: remove Flyway target/properties and permanently disable Spring SQL init in every Light profile。
- Symbols: `spring.sql.init.mode=never`；delete `spring.flyway`/`app.*flyway.targets` entries。
- Repository evidence: copied profiles currently configure Flyway physical targets and H2 startup behavior。
- Dependencies and consumers: File 9 properties binder；Spring Boot context；manual schema runbook。
- Why now: source removal must be matched by configuration removal before context GREEN。
- Contract/signature changes: profile/test runtime never initializes schema automatically；other datasource/Nacos/business values unchanged。
- Input/output and state mapping: YAML -> no migration beans/state；test helper owns explicit schema setup。
- Error and edge behavior: any profile override enabling SQL init/Flyway is verifier failure；missing table remains visible。
- Implementation pseudocode:

```yaml
set spring.sql.init.mode: never in base/test-effective configuration
remove every spring.flyway and app datasource flyway target/location/property from dev/test/prod/base resources
preserve selected datasource mode, physical pools, Sharding route map, credentials placeholders and non-DB profiles
```

- Verification contribution: configuration binding/no-implicit-schema tests and negative scans。
- After this file: runtime config matches manual-only ownership。

#### File 11 — `DELETE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/{main/java/infrastructure/{user,teaching}/repo/jpa/**,test/java/infrastructure/{config/datasource/PhysicalDataSourceFlywayMigratorTest.java,migration/FlywayMigrationConventionTest.java}}`

- Purpose: remove superseded JPA repositories/package-info and Flyway-specific tests only after replacements are GREEN。
- Symbols: 8 `*JpaRepository`、2 package-info、migrator/convention tests。
- Repository evidence: File 4-6 replacements cover every method；Files 2/7 cover migration contracts。
- Dependencies and consumers: no remaining production/test import allowed；verifier checks absence。
- Why now: deleting earlier would obscure RED query parity；now search can prove no consumers。
- Contract/signature changes: none to Domain/Application；source boundary becomes literal no-JPA/no-Flyway。
- Input/output and state mapping: removes framework types/files；no data state。
- Error and edge behavior: any unresolved import/build reference returns to Files 4-6 or 9-10；do not restore compatibility adapter。
- Implementation pseudocode:

```text
delete all light-open repo/jpa Java and package-info after rg proves RepositoryImpl imports Mapper only
delete only Flyway-specific tests replaced by ManualSql tests; retain datasource/business integration tests
run forbidden scans for JpaRepository/jakarta.persistence/org.flywaydb/Flyway and require zero open-tree hits
```

- Verification contribution: `TEST-007/008/013` negative proof。
- After this file: Light generated source has no JPA/Flyway and all replacement tests are the only contract。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light-open -am clean integration-test`
- Expected result: exit 0；8 Mapper/repository/manual schema/datasource tests GREEN；generated POM/tree has zero JPA/Flyway；fresh test context creates no table without helper。
- Failure returns to: Files 1/4-6 for query mismatch；Files 2/7 for schema；Files 3/8-11 for forbidden residue/startup。
- Completion criteria: Light MP/manual SQL contract passes and original Light/JPA/Flyway files remain untouched。
- Rollback: revert Step paths in light-open；manual SQL is not executed，so no DB rollback。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/{user,teaching}/repo/**/*Test.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/infrastructure/migration/{ManualSqlConventionTest.java,ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java}` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/pom.xml` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/{user,teaching}/repo/mapper/*Mapper.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/mybatis/mapper/{user,teaching}/*Mapper.xml` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/{user,teaching}/repo/{po,impl,converter}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/db/manual/postgresql/{README.md,master-data/001__create_light_master_data_schema.sql,shard/002__create_light_sharded_schema.sql}` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/{java/infrastructure/config/datasource/PhysicalDataSourceFlywayMigrator.java,resources/db/migration/**}` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/infrastructure/config/datasource/{ShardingDataSourceBootstrapper.java,ShardingDataSourceProperties.java,ShardingTopologyValidator.java,ShardingSphereDataSourceConfiguration.java}` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/resources/application*.yml` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/{main/java/infrastructure/{user,teaching}/repo/jpa/**,test/java/infrastructure/{config/datasource/PhysicalDataSourceFlywayMigratorTest.java,migration/FlywayMigrationConventionTest.java}}`
- Commit: `refactor(light-open): replace jpa and flyway with mybatis manual sql`

### Step 4 — 接入 Light Open 的 DTP、Nacos、Springdoc 与 ArchUnit 运行合同

- Requirements: `REQ-001`,`REQ-005`,`REQ-006`,`REQ-007`,`REQ-012`,`REQ-013`,`REQ-015`,`REQ-016`,`REQ-017`,`REQ-018`,`REQ-019`,`REQ-021`
- Dependencies: Step 3 committed。
- Baseline state: Light Long/MP/manual schema GREEN；POM still has copied unused Dubbo/internal architecture plugin and no DTP governance。
- Observable outcome: generated Light uses required machine ID、bounded managed executor、Nacos 3.0.3-compatible config、Springdoc 2.8.17 and ArchUnit，with no Gateway/Dubbo/internal plugin。
- End state: Light Open product is feature-complete；test profile opens no Nacos/Redis/DB/MQ external socket。
- Test-first gate: `Required — runtime/async/OpenAPI/architecture tests first fail because DTP/decorator/required machine-id/ArchUnit rules are absent and copied dependencies remain.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/start/{StudentManagementApplicationTest.java,config/RuntimeConfigurationTest.java}`

- Purpose: freeze context behavior for ID、DTP test isolation、Springdoc and no forbidden edge/RPC runtime。
- Symbols: context loads with machine-id `0`、DTP disabled；missing machine-id failure；OpenAPI bean/resource presence；no Gateway/Dubbo bean。
- Repository evidence: copied tests already inspect runtime profiles/config and external-free application context。
- Dependencies and consumers: start configuration/POM/YAML Files 2-4。
- Why now: establishes RED before runtime wiring。
- Contract/signature changes: test properties explicitly set ID and DTP；no API route payload change。
- Input/output and state mapping: property set -> bean graph；no Redis/Nacos socket；OpenAPI generated from existing controllers。
- Error and edge behavior: missing/out-of-range ID machine property fails；DTP enabled without Redis is not used in test profile。
- Implementation pseudocode:

```java
load test context with machine-id=0, dtp.enabled=false, Nacos disabled and assert LongIdGenerator plus OpenAPI beans
load a minimal context without machine-id and assert the exact Common ID fail-fast message
assert classpath/bean factory contains no Gateway route locator, Dubbo service export, or internal architecture plugin output
```

- Verification contribution: runtime config RED/GREEN and external-free proof。
- After this file: tests fail until POM/YAML/Async wiring follows。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/pom.xml`

- Purpose: finalize Light dependency/architecture allowlist。
- Symbols: DTP starter、ArchUnit 1.4.2 test、Springdoc 2.8.17；remove Dubbo/internal bytecode plugin/Gateway absence。
- Repository evidence: copied POM has actual Springdoc consumer and unused Dubbo starter/plugin；Spec §6.2 exact allowlist。
- Dependencies and consumers: Async config、ArchUnit test、runtime context；no RPC source in Light。
- Why now: test classes/config require dependencies；persistence sections from Step 3 are preserved。
- Contract/signature changes: direct `top.egon` only common core/id + DTP；common-trace transitive；no Common MP/Trace Starter。
- Input/output and state mapping: effective dependency tree -> allowed runtime/test artifacts only。
- Error and edge behavior: any Gateway/Dubbo/unapproved Egon dependency fails `dependency:tree`/verifier。
- Implementation pseudocode:

```xml
add egon-cola-component-dynamic-thread-pool-starter and test-scoped org.archunit:archunit-junit5:1.4.2
retain springdoc-openapi-starter-webmvc-ui:2.8.17; remove unused dubbo starter and internal bytecode plugin
preserve official MP/Common ID/Sharding/Nacos/business dependencies from prior Steps and enforce no Gateway artifact
```

- Verification contribution: effective POM/tree and compile of Files 3/5。
- After this file: dependencies are final；runtime configuration remains RED until Files 3-4。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/start/config/async/AsyncConfiguration.java`

- Purpose: reuse Boot's executor as the one DTP-governed async executor。
- Symbols: constructor/qualifier for `applicationTaskExecutor`，`DtpTaskDecorator` bean，`getAsyncExecutor()`。
- Repository evidence: copied class implements `AsyncConfigurer` but returns null；DTP auto-config discovers `ThreadPoolTaskExecutor` beans。
- Dependencies and consumers: Boot task execution auto-config、DTP Registry、existing `@Async` behavior/uncaught handler。
- Why now: dependency exists and RED context expects one managed executor。
- Contract/signature changes: `getAsyncExecutor()` returns the Boot bean；uncaught handler unchanged；no second executor class。
- Input/output and state mapping: submitted Runnable captures/restores/cleans trace context through decorator；pool metrics identified by `applicationTaskExecutor`。
- Error and edge behavior: missing qualified bean fails context rather than silently common-pool fallback；CallerRuns/bounds from YAML。
- Implementation pseudocode:

```java
declare one DtpTaskDecorator bean consumed by Boot TaskExecutorBuilder
inject @Qualifier("applicationTaskExecutor") AsyncTaskExecutor and return it from getAsyncExecutor()
retain existing AsyncUncaughtExceptionHandler and never instantiate another raw/unbounded Executor
```

- Verification contribution: DTP executor discovery/context cleanup tests。
- After this file: one decorated executor is wired；pool bounds/DTP profiles await File 4。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/{src/main/resources/application*.yml,deploy/env/.env*.example,deploy/compose/*.yaml}`

- Purpose: define required ID、bounded Boot executor、DTP Redis/Nacos profile/image and test isolation。
- Symbols: `EGON_ID_MACHINE_ID`、`spring.task.execution.pool.*`、`egon.cola.component.dtp.*`、Nacos 3.0.3 image/config。
- Repository evidence: copied resources have Nacos 2.5.1 Compose and no DTP/ID machine property；six compose variants share structure。
- Dependencies and consumers: Common ID/DTP auto-config、AsyncConfiguration、runtime operator、tests。
- Why now: bean graph exists；configuration makes behavior deterministic and operationally visible。
- Contract/signature changes: runtime machine ID no default；local single-instance env example explicit；test disables DTP/Nacos/report。
- Input/output and state mapping: env -> ID generator/pool/DTP Redis client；no secret committed；DB SQL init remains never。
- Error and edge behavior: missing machine-id/Redis runtime fail-fast documented；tests open no socket；Nacos major mismatch forbidden。
- Implementation pseudocode:

```yaml
bind EGON_ID_MACHINE_ID without runtime default; set Boot pool core/max/queue/keepAlive from env defaults 8/32/1000/60s
configure egon.cola.component.dtp registry Redis and report in dev/prod, but enabled=false in test
update every compose image to Nacos 3.0.3 and pass non-secret ID/DTP variables consistently across runtimes
```

- Verification contribution: context/config/compose static tests and DTP isolation。
- After this file: runtime/test profiles satisfy ID/DTP/Nacos contracts。

#### File 5 — `CREATE egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/architecture/OpenArchitectureTest.java`

- Purpose: replace internal plugin with JUnit ArchUnit rules for Light layer/package boundaries。
- Symbols: `adapter/application/domain/infrastructure/start/common` dependency rules；forbidden framework imports in Domain/Application。
- Repository evidence: copied living architecture and current plugin rules define allowed direction；compiled generated classes are test input。
- Dependencies and consumers: ArchUnit test dep；all generated production packages；verify phase。
- Why now: final dependency graph exists，so bytecode rules can be exact without false RED from transitional sources。
- Contract/signature changes: no production API；test-only governance mechanism。
- Input/output and state mapping: compiled classes -> rule violations with class/dependency evidence。
- Error and edge behavior: JPA/Flyway/Gateway/Proto-in-Domain/unapproved `top.egon` imports fail；generated/test classes excluded deliberately。
- Implementation pseudocode:

```java
import production classes under generated base package and exclude tests/generated framework code
assert Adapter->Application, Infrastructure->Domain, Start composition and no inward framework dependency rules
assert Domain/Application do not depend on MyBatis, ShardingSphere, Dubbo, Gateway, JPA or Flyway packages
```

- Verification contribution: `TEST-010/014` Light architecture proof。
- After this file: architecture verify no longer needs internal Maven plugin。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/{large-monolith-light-domain-architecture.md,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`

- Purpose: document actual open dependencies、Long/API mapping、manual SQL、DTP and external Gateway boundary。
- Symbols: module/stack diagrams、operator steps、test commands、proof limits。
- Repository evidence: copied living/readme documents currently describe UUID/JPA/Flyway and original product。
- Dependencies and consumers: generated developers/DBA/operators；root public docs later link this product。
- Why now: implementation contracts are stable and can be documented without speculative text。
- Contract/signature changes: no runtime symbol；commands point to open paths and manual SQL only。
- Input/output and state mapping: user choice/config -> generated files/runtime prerequisites；no credentials。
- Error and edge behavior: missing schema/machine/Redis/Nacos failures and no-auto-repair boundary explicit。
- Implementation pseudocode:

```markdown
replace UUID/JPA/Flyway diagrams and commands with Long/MyBatis/manual SQL/DTP/ArchUnit truth
document HTTP decimal IDs, Springdoc endpoints, external Gateway non-ownership and machine-id uniqueness
keep original Light layer responsibilities and bilingual instructions synchronized; state live topology gaps
```

- Verification contribution: docs/source-token gate and generated README review。
- After this file: Light Open implementation/documentation are internally consistent。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-light-open -am clean integration-test && ! rg -n 'spring-cloud-starter-gateway|dubbo-spring-boot-starter|egon-cola-component-bytecode|org\.flywaydb|JpaRepository|UuidV7' egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources --glob '!README*' --glob '!deploy/sql/README.md'`
- Expected result: Maven exit 0；rg returns no forbidden production/dependency hit（documentation historical wording is excluded or rewritten）；OpenAPI/ArchUnit/DTP context tests pass without sockets。
- Failure returns to: File 1 assertions，File 2 tree，File 3 executor wiring，File 4 profile/image，File 5 architecture rules，File 6 stale docs。
- Completion criteria: Light Open individually satisfies all mapped requirements and stays generated-project GREEN。
- Rollback: revert only Light Open Step paths；no runtime/data action occurred。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/start/{StudentManagementApplicationTest.java,config/RuntimeConfigurationTest.java}` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/pom.xml` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/main/java/start/config/async/AsyncConfiguration.java` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/{src/main/resources/application*.yml,deploy/env/.env*.example,deploy/compose/*.yaml}` `egon-cola-archetypes/egon-cola-archetype-light-open/src/main/resources/archetype-resources/src/test/java/architecture/OpenArchitectureTest.java` `egon-cola-archetypes/egon-cola-archetype-light-open/{large-monolith-light-domain-architecture.md,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`
- Commit: `feat(light-open): finalize open runtime governance`

### Step 5 — 将 Service Open 的 Evaluation 模型迁为 Snowflake Long

- Requirements: `REQ-002`,`REQ-005`,`REQ-009`,`REQ-012`,`REQ-017`,`REQ-021`
- Dependencies: Step 1 committed；independent of Light Steps after copy baseline。
- Baseline state: Service Open copied six-module project uses String/UUID IDs、UUID sharding and external Java facades。
- Observable outcome: Evaluation Domain/Application/PO use Long；current Java facade adapter temporarily formats/parses decimal strings；route keys stay course/exam native。
- End state: generated Service remains compilable with copied JPA/external facades，ready for persistence and Proto replacements。
- Test-first gate: `Required — domain/application/repository/client/sharding tests first fail on String signatures and UUID algorithm before production migration.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,adapter,infrastructure}/src/test/java/**/*.java`

- Purpose: freeze Long Course/Exam/Paper/Score/Schedule IDs and unchanged opaque class/student semantics。
- Symbols: Course/Exam/Score manage/domain/repository tests，client mapping tests，route/node-map tests。
- Repository evidence: copied tests contain representative create/schedule/exam/score/page/rollback fixtures and no required live services。
- Dependencies and consumers: all generated layer symbols；deterministic `LongIdGenerator` fake；current Java facade remains compile bridge。
- Why now: establish RED type/mapping/route expectations before production signatures。
- Contract/signature changes: technical IDs/classId/studentId -> Long；facade strings are decimal bridge until Step 7 Proto int64。
- Input/output and state mapping: fake Long -> Domain/PO；existing facade request decimal String -> Long；page/status/time unchanged。
- Error and edge behavior: non-positive/overflow rejected；ScheduleCourse/RecordScore do not add Organization lookups；one ID per create。
- Implementation pseudocode:

```java
replace UUID fixtures with deterministic positive longs across course/exam/score/schedule tests
assert ScheduleCourse validates local course/overlap only and RecordScore validates exam/paper/duplicate only
assert Java facade bridge parses/prints decimal IDs, page totals/order and rollback/events remain identical
```

- Verification contribution: RED/GREEN for approved Service ID behavior and no fetch-then-forward drift。
- After this file: tests fail against copied String/UUID production source。

#### File 2 — `RENAME egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java -> egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithm.java`

- Purpose: provide the exact Long ShardingSphere SPI for Service routes。
- Symbols: `StandardShardingAlgorithm<Long>` and shared `ShardingNodeMap.routeSlot(long)`。
- Repository evidence: copied Service algorithm/node map is byte-equivalent to Light variant and YAML keys are course_id/id/exam_id。
- Dependencies and consumers: route tests、starter sharding YAML、Repository Mapper later。
- Why now: Long call chain requires route type before context tests。
- Contract/signature changes: same hash/spread/mask and precise target logic as Step 2；range rejected。
- Input/output and state mapping: course ID routes schedules；exam ID routes exam/paper/score co-location。
- Error and edge behavior: non-positive/null/range/missing target fails without broadcast or fallback。
- Implementation pseudocode:

```java
change generic key type to Long and delegate positive key to ShardingNodeMap Long hash slot
select exact database/table from available targets for course_id, exam id or exam_id according to YAML
reject range and unavailable target; preserve immutable power-of-two node-map validation
```

- Verification contribution: focused Service route tests and co-location integration。
- After this file: algorithm core is Long；test/YAML names still require Files 3/6。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java -> egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithmTest.java`

- Purpose: prove database/table Long selection and range/invalid rejection under Service topology。
- Symbols: precise route cases for `course_schedule` and exam family。
- Repository evidence: copied UUID test already owns SPI properties/available-target fixture。
- Dependencies and consumers: File 2 production class and node-map tests。
- Why now: production symbol exists，so RED can move to GREEN independently of domain migration。
- Contract/signature changes: Long keys/expected slots；remove UUID version/canonical assertions。
- Input/output and state mapping: known course/exam Long -> expected one physical node/suffix。
- Error and edge behavior: zero/negative/range/invalid property/missing target exceptions asserted。
- Implementation pseudocode:

```java
initialize Service algorithm with exact four-slot mapping and available shard/table names
assert course and exam positive longs map deterministically for database and table target modes
assert every invalid/range path throws and never returns all available targets
```

- Verification contribution: Service `TEST-019` unit portion。
- After this file: Service Long routing unit suite is GREEN。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application}/src/main/java/**/*.java`

- Purpose: migrate Evaluation entities/value objects/ports/commands/results/use cases to Long while preserving transactions。
- Symbols: `CourseId(long)`、`ExamId(long)`、CourseSchedule/ExamPaper/Score IDs，CourseManageImpl/ExamManageImpl/ScoreManageImpl。
- Repository evidence: current domain-first packages and manage methods centralize generator and repository calls。
- Dependencies and consumers: Adapter/current facade bridge；Infrastructure repositories；tests File 1。
- Why now: inward authoritative types must change before outward/persistence mappings。
- Contract/signature changes: all technical/cross-service class/student IDs Long；codes/status/time/page unchanged；`LongIdGenerator` injected。
- Input/output and state mapping: command Long -> domain Long；create obtains `nextLongId()` once；events/results carry Long internally。
- Error and edge behavior: existing not-found/duplicate/overlap/state/rollback errors unchanged；no remote Organization validation added。
- Implementation pseudocode:

```java
change CourseId/ExamId and entity/repository port signatures to positive long/Long
in each @Transactional create path call LongIdGenerator.nextLongId once, then existing domain service and repository
keep classId/studentId as positive opaque Long references and preserve event/page/transaction behavior
```

- Verification contribution: domain/application focused tests reach GREEN。
- After this file: Evaluation inward layers are Long；Adapter/Infrastructure still need mapping Files 5-6。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{adapter,infrastructure,starter}/src/main/java/**/*.java`

- Purpose: complete temporary Service decimal-facade/JPA Long mapping and remove UUID bean。
- Symbols: facade converters/validators/impls，PO/converters/RepositoryImpl，client projections/stubs，MQ messages，`EvaluationServiceApplication`。
- Repository evidence: copied Adapter converter/facade and Infrastructure converter/PO boundaries isolate external Java DTO/JPA fields。
- Dependencies and consumers: File 4 Long use cases/ports；external facade until Step 7；JPA until Step 6。
- Why now: closes generated compilation before changing persistence/RPC technology。
- Contract/signature changes: external String facade DTOs parse/format decimal；PO/JPA generics Long；remove manual UUID generator bean。
- Input/output and state mapping: facade/MQ decimal strings <-> Long；Domain <-> PO Long；status/Instant/page exact。
- Error and edge behavior: invalid decimal -> existing validation failure；external client failure mapping unchanged until Proto Step 7。
- Implementation pseudocode:

```java
adapter converters parse positive decimal request IDs into Long commands and format Long results for temporary Java facade DTOs
PO/converters/repositories use Long identifiers while keeping existing JPA query semantics and transaction boundaries
remove EvaluationServiceApplication UuidV7Generator bean and resolve Common LongIdGenerator through auto-configuration
```

- Verification contribution: generated Service compile、facade/business/repository tests。
- After this file: all Service source is Long-compatible；JPA/external facade remain planned transitional dependencies。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/{shardingsphere-sharding.yml,shardingsphere-sharding-readwrite.yml}`

- Purpose: bind Service topology to Long algorithm without changing approved keys。
- Symbols: schedule `course_id`；exam `id`；paper/score `exam_id`；renamed algorithm class。
- Repository evidence: copied YAML confirms exact repository-native keys and 2×2 node topology。
- Dependencies and consumers: File 2 algorithm、starter context、route integration tests。
- Why now: final Step context must instantiate new type/name。
- Contract/signature changes: algorithm name/class only；topology/readwrite rules preserved。
- Input/output and state mapping: Long values from SQL -> same slot function for database/table；no time-bit decoding。
- Error and edge behavior: misconfigured key/class/topology fails context；no broadcast/range support。
- Implementation pseudocode:

```yaml
replace UUID algorithm names/classes in both Service topology resources with SnowflakeLongShardingAlgorithm
keep course_schedule route column course_id, exam route id, and paper/score route exam_id for both rules
assert node-map/properties/actualDataNodes stay identical between sharding and readwrite modes
```

- Verification contribution: Service datasource mode/co-location tests。
- After this file: Service Long intermediate project is GREEN and ready for MP migration。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-service-open -am integration-test`
- Expected result: exit 0；Service generated project compiles with Long IDs and copied JPA/facade；route/use-case/client tests pass without live services。
- Failure returns to: File 1 contract，Files 2/3 route，File 4 domain/application，File 5 boundary mapping，File 6 config。
- Completion criteria: Service technical/cross-service IDs are Long/decimal bridge and no UUID generator call remains。
- Rollback: revert only Service Open Step paths；no SQL or runtime executed。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application,adapter,infrastructure}/src/test/java/**/*.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java -> egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithm.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java -> egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithmTest.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{domain,application}/src/main/java/**/*.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-{adapter,infrastructure,starter}/src/main/java/**/*.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/sharding/{shardingsphere-sharding.yml,shardingsphere-sharding-readwrite.yml}`
- Commit: `refactor(service-open): migrate evaluation ids to snowflake long`

### Step 6 — 用 MyBatis-Plus 与手工 SQL 完成 Service Open 持久化

- Requirements: `REQ-002`,`REQ-008`,`REQ-009`,`REQ-010`,`REQ-011`,`REQ-012`,`REQ-016`,`REQ-017`,`REQ-019`,`REQ-021`
- Dependencies: Step 5 committed。
- Baseline state: Long Service GREEN，but 5 JPA repositories、Flyway migrator/config/tests/resources remain。
- Observable outcome: five explicit Mapper/XML preserve queries/page/overlap/co-location；two manual scripts own schema。
- End state: Service persistence and datasource use official MP/Long/manual SQL only；RPC still temporary Java facade until Step 7。
- Test-first gate: `Required — repository/manual-schema tests first fail for absent Mapper/resources and existing JPA/Flyway ownership.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/{course,exam}/repo/**/*Test.java`

- Purpose: freeze 5 repository operations including overlap、exact routed pair and page metadata。
- Symbols: Course/CourseSchedule/Exam/ExamPaper/Score RepositoryImpl tests with Mapper collaborators。
- Repository evidence: copied tests and five JPA derived interfaces enumerate every query predicate。
- Dependencies and consumers: future Mappers/XML；Domain ports；Long fixtures Step 5。
- Why now: query behavior must be RED before removing JPA。
- Contract/signature changes: JPA mocks/Pageable -> Mapper/H2 and infrastructure Page conversion；ports unchanged。
- Input/output and state mapping: Long/code/time/page -> PO/list/count/page -> Domain/Page exact fields。
- Error and edge behavior: overlap predicate、exam pair route、unknown exam empty page、duplicate student and affected rows asserted。
- Implementation pseudocode:

```java
arrange Mapper rows for course code, schedule overlap, exam/paper pair and score page fixtures
call unchanged Repository ports and assert Long IDs, startsAt/id order, totalPages/pageSize/totalCount
assert duplicate/zero-row/rollback paths and no JPA session or implicit schema behavior
```

- Verification contribution: RED/GREEN for all 5 Mapper replacements。
- After this file: focused tests await Mapper/source Files 4-6。

#### File 2 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/{ManualSqlConventionTest.java,ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java}`

- Purpose: define Evaluation manual DDL、9 physical table、BIGINT and explicit-only test execution contract。
- Symbols: master/shard resource checks、suffix parity/index/FK/check checks、fresh-context no-init。
- Repository evidence: copied Flyway convention/migration tests and two existing SQL contain expected schema topology。
- Dependencies and consumers: File 7 resources、H2 test only、starter datasource context。
- Why now: RED manual ownership before migrator/resource removal。
- Contract/signature changes: no runtime schema API；test helper only。
- Input/output and state mapping: manual SQL -> explicit test DataSource -> course + 8 shard tables；no history table。
- Error and edge behavior: missing index/check/FK/BIGINT/header/target or accidental package/init fails。
- Implementation pseudocode:

```java
assert 001/002 manual resource paths, headers and absence of Flyway/UUID/schema.sql tokens
explicitly execute master and shard scripts in isolated H2 and inspect all course/schedule/exam/paper/score columns and indexes
start context without helper and assert zero schema creation plus no test helper class in main JAR
```

- Verification contribution: Service manual schema tests。
- After this file: RED until File 7 and datasource/config cleanup。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-infrastructure/pom.xml}`

- Purpose: manage/use official MP 3.5.17 and remove JPA/Flyway dependencies from Service persistence module。
- Symbols: `mybatis-plus.version` management；infrastructure starter dependency；remove external Flyway/JPA artifacts/profile references。
- Repository evidence: root manages Sharding/facades；infrastructure POM declares JPA/Flyway and drivers。
- Dependencies and consumers: Mappers/PO/File 4-6；Step 7 later changes facade/plugin/module sections，Step 8 DTP/ArchUnit。
- Why now: compile prerequisite for Mapper conversion。
- Contract/signature changes: no Common MP/raw MyBatis；H2/PostgreSQL and Sharding remain。
- Input/output and state mapping: Maven model -> MP runtime；no application/database state。
- Error and edge behavior: dependency tree must contain no JPA/Hibernate/Flyway/unapproved Common MP。
- Implementation pseudocode:

```xml
root manages mybatis-plus-spring-boot3-starter 3.5.17 without changing Boot/Cloud/SCA/Dubbo/Sharding versions
infrastructure replaces spring-data-jpa and flyway dependencies with official MP starter; keeps H2/PostgreSQL scopes
leave external facade/module/plugin sections for Step 7/8 so this Step has one persistence outcome
```

- Verification contribution: POM/tree and Mapper compile gate。
- After this file: old JPA imports RED until File 6 and deletion File 10。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{course,exam}/repo/mapper/*Mapper.java`

- Purpose: add five MP Mapper contracts matching current derived operations。
- Symbols: CourseMapper、CourseScheduleMapper、ExamMapper、ExamPaperMapper、ScoreMapper。
- Repository evidence: five copied JPA interfaces define code count/find、overlap、exam pair、duplicate and page operations。
- Dependencies and consumers: BaseMapper<Po>、RepositoryImpl、File 5 XML。
- Why now: Java contract precedes XML and RepositoryImpl rewire。
- Contract/signature changes: Long IDs；MP Page input/output stays Infrastructure；exact `@Param` names。
- Input/output and state mapping: Long/code/time/page -> PO/list/count/IPage；no Domain leakage。
- Error and edge behavior: empty/invalid inputs handled at RepositoryImpl；Mapper SQL never broadcasts mutation。
- Implementation pseudocode:

```java
declare five BaseMapper<Po> interfaces and only custom methods needed by former JPA derived queries
use Long for course/exam/score/class/student IDs and explicit @Param for overlap/page/pair predicates
return PO/list/count/IPage; keep all domain conversion and exception decisions in RepositoryImpl
```

- Verification contribution: repository test compile and XML binding。
- After this file: Mapper contracts exist；custom SQL awaits File 5。

#### File 5 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/mybatis/mapper/{course,exam}/*Mapper.xml`

- Purpose: implement logical-table SQL for five Mapper contracts。
- Symbols: resultMaps；course code/page；schedule overlap；exam/paper exact pair；score duplicate/page/order。
- Repository evidence: JPA method signatures + Spec table/index definitions + current repository page semantics。
- Dependencies and consumers: File 4 namespaces、ShardingSphere logical datasource、manual schema。
- Why now: custom query behavior must exist before RepositoryImpl GREEN。
- Contract/signature changes: explicit columns/BIGINT；score page returns `created_at,id` stable order；unknown exam yields empty page。
- Input/output and state mapping: Long/time/page -> routed logical SQL -> Po/IPage；no physical table suffix in XML。
- Error and edge behavior: schedule overlap uses `starts_at < endsAt AND ends_at > startsAt`；examId present for routed score/paper operations。
- Implementation pseudocode:

```xml
map every PO column explicitly with BIGINT IDs and TIMESTAMP/enum strings
encode course code, schedule overlap, exam/paper pair and score duplicate/page predicates exactly
order pages by created_at,id; use logical course_schedule/exam/exam_paper/score names and never physical suffixes
```

- Verification contribution: H2 Mapper/route/page tests。
- After this file: SQL statements exist；PO/RepositoryImpl still RED until File 6。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{course,exam}/repo/{po,impl,converter}/**/*.java`

- Purpose: convert five PO/RepositoryImpl families from JPA to MP composition。
- Symbols: `@TableName/@TableId(INPUT)` Long fields；Mapper constructor fields/calls；page conversion。
- Repository evidence: copied converters/RepositoryImpl own all Domain<->PO mapping and application transaction remains above。
- Dependencies and consumers: Files 4/5、Domain ports、focused tests。
- Why now: contracts/SQL are ready for minimum GREEN path。
- Contract/signature changes: remove persistence annotations/proxy requirements；replace JPA save/find/count/Page calls with Mapper equivalents。
- Input/output and state mapping: Long Domain <-> Long Po；IPage -> Domain Page current/totalPages/pageSize/totalCount exact。
- Error and edge behavior: zero affected、not-found、duplicate、overlap、transaction rollback match existing errors。
- Implementation pseudocode:

```java
annotate five Po with logical @TableName and @TableId(INPUT), removing every jakarta.persistence import
RepositoryImpl composes Mapper, calls explicit custom statements and asserts insert/update affected count
convert IPage records/total/current/size to existing Domain Page without exposing MP outside Infrastructure
```

- Verification contribution: all Service repository tests GREEN。
- After this file: MP persistence works；manual/startup replacement Files 7-9 remain。

#### File 7 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/{README.md,master-data/001__create_evaluation_master_data_schema.sql,shard/002__create_evaluation_sharded_schema.sql}`

- Purpose: deliver Evaluation BIGINT master/shard schema under manual ownership。
- Symbols: course、course_schedule_0/1、exam_0/1、exam_paper_0/1、score_0/1 and indexes/checks/FKs。
- Repository evidence: copied two migration SQL fully define baseline fields and 2×2 suffix schema。
- Dependencies and consumers: DBA、test helper、Mapper/Sharding rules。
- Why now: final schema required before deleting copied resources。
- Contract/signature changes: all technical IDs and class/student refs BIGINT；other constraints/indexes exact。
- Input/output and state mapping: 001 -> master_data；002 -> every shard primary；replicas via replication。
- Error and edge behavior: no cross-database course/class/student FK；partial failure blocks rollout；no app repair/history。
- Implementation pseudocode:

```sql
001 creates course with BIGINT id and existing code/status/time constraints
002 creates paired schedule/exam/paper/score tables with BIGINT keys, overlap/check/unique/FK/index parity
README defines target order/checksum/verification/restore-forward-fix and forbids runtime execution
```

- Verification contribution: Service manual SQL tests。
- After this file: final schema resources exist，ready to replace copied migration files。

#### File 8 — `DELETE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/{java/infrastructure/config/datasource/PhysicalDataSourceFlywayMigrator.java,resources/db/migration/**}`

- Purpose: remove Service production migration engine/resources。
- Symbols: migrator + two copied SQL。
- Repository evidence: same startup chain as Light；File 7 is complete replacement。
- Dependencies and consumers: File 9 removes caller/properties；tests File 2 replace behavior。
- Why now: replacement/schema proof exists before deletion。
- Contract/signature changes: application no longer mutates schema。
- Input/output and state mapping: removes DDL side effect only；business writes unchanged。
- Error and edge behavior: missing schema is visible/fail closed；original Service SQL remains untouched。
- Implementation pseudocode:

```text
delete only service-open migrator and copied db/migration resources
retain manual SQL and original service archetype migrations; search for callers/locations before compile
require no runtime migration fallback, history table, profile or Maven plugin to replace the deleted class
```

- Verification contribution: forbidden scan and package boundary。
- After this file: datasource source RED until File 9 removes migration chain。

#### File 9 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/{ShardingDataSourceBootstrapper.java,ShardingDataSourceProperties.java,ShardingTopologyValidator.java,ShardingSphereDataSourceConfiguration.java}`

- Purpose: remove Flyway property/target/call while preserving Service topology/readwrite validation。
- Symbols: bootstrapper constructor/order、properties records、validator rules、Mapper scan/DataSource config。
- Repository evidence: copied classes share Light flow and Service-specific table rules。
- Dependencies and consumers: physical pools、YAML、MP Mappers、starter context。
- Why now: restores compile after File 8 and creates final no-DDL startup。
- Contract/signature changes: no `FlywayTargetProperties/ShardingFlywayProperties` or migrator collaborator。
- Input/output and state mapping: selected config -> physical pools -> topology validate -> logical datasource；zero schema mutation。
- Error and edge behavior: incomplete physical topology/routing/readwrite/schema fails；no unsharded fallback。
- Implementation pseudocode:

```java
remove Flyway nested properties and migrator dependency from records, constructors and bootstrap sequence
retain physical datasource creation plus Service course/schedule/exam/paper/score topology and readwrite validation
create logical DataSource only after validation; never execute or discover manual SQL resources
```

- Verification contribution: datasource/context/no-init tests。
- After this file: Service datasource is no-DDL and compile-ready。

#### File 10 — `DELETE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/{main/java/infrastructure/{course,exam}/repo/jpa/**,test/java/infrastructure/{config/datasource/PhysicalDataSourceFlywayMigratorTest.java,migration/FlywayMigrationConventionTest.java}}`

- Purpose: remove five JPA repository/package-info and superseded Flyway tests after replacements are GREEN。
- Symbols: Course/CourseSchedule/Exam/ExamPaper/Score JPA interfaces；two package-info；migration tests。
- Repository evidence: Files 4-6 map every actual method；File 2 owns final schema tests。
- Dependencies and consumers: none permitted after source search。
- Why now: late deletion preserves method inventory until parity is proven。
- Contract/signature changes: no Domain/Application change；literal source ban satisfied。
- Input/output and state mapping: removes framework files only。
- Error and edge behavior: unresolved import returns to Mapper/RepositoryImpl；no compatibility adapter allowed。
- Implementation pseudocode:

```text
delete all service-open course/exam repo/jpa files and package-info after Mapper parity passes
delete Flyway-specific tests replaced by ManualSql tests while retaining route/readwrite/repository integration
require rg for JpaRepository/jakarta.persistence/Flyway to return zero in Service Open generated source
```

- Verification contribution: Service no-JPA/no-Flyway negative gate。
- After this file: persistence source cleanup complete。

#### File 11 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application*.yml`

- Purpose: remove Flyway targets and force manual-only SQL init across Service profiles。
- Symbols: `spring.sql.init.mode=never`；delete `spring.flyway/app.*flyway`；preserve Dubbo/Nacos/route settings。
- Repository evidence: copied resources bind physical migration targets and test H2 behavior。
- Dependencies and consumers: File 9 properties；Manual SQL README；starter tests。
- Why now: final context configuration after source cleanup。
- Contract/signature changes: test schema only through explicit helper；no runtime initializer。
- Input/output and state mapping: profile config -> pools/logical datasource only；zero DDL side effect。
- Error and edge behavior: accidental initializer/profile override fails verifier；missing table not hidden。
- Implementation pseudocode:

```yaml
set spring.sql.init.mode=never in effective base/test configuration
remove every Flyway location/target/property from Service base/dev/test/prod while preserving datasource topology
leave Dubbo/Nacos/DTP placeholders for Steps 7/8 and never point Spring to db/manual resources
```

- Verification contribution: config/no-init tests and negative scan。
- After this file: Service MP/manual schema project is fully GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-service-open -am clean integration-test`
- Expected result: exit 0；5 Mapper/manual SQL/datasource tests pass；no JPA/Flyway dependency/import/resource；external facades still resolve only until Step 7。
- Failure returns to: Files 1/4-6 query mapping；Files 2/7 schema；Files 3/8-11 dependency/startup cleanup。
- Completion criteria: Service persistence is official MP/manual-only and business/use-case tests remain GREEN。
- Rollback: revert Step-owned Service Open paths；no SQL executed。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/{course,exam}/repo/**/*Test.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/{ManualSqlConventionTest.java,ManualSchemaIntegrationTest.java,ManualSchemaTestSupport.java}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-infrastructure/pom.xml}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{course,exam}/repo/mapper/*Mapper.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/mybatis/mapper/{course,exam}/*Mapper.xml` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{course,exam}/repo/{po,impl,converter}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/manual/postgresql/{README.md,master-data/001__create_evaluation_master_data_schema.sql,shard/002__create_evaluation_sharded_schema.sql}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/{java/infrastructure/config/datasource/PhysicalDataSourceFlywayMigrator.java,resources/db/migration/**}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/{ShardingDataSourceBootstrapper.java,ShardingDataSourceProperties.java,ShardingTopologyValidator.java,ShardingSphereDataSourceConfiguration.java}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/{main/java/infrastructure/{course,exam}/repo/jpa/**,test/java/infrastructure/{config/datasource/PhysicalDataSourceFlywayMigratorTest.java,migration/FlywayMigrationConventionTest.java}}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application*.yml`
- Commit: `refactor(service-open): replace jpa and flyway with mybatis manual sql`

### Step 7 — 建立 Service Open 本地 Proto 与 Triple RPC

- Requirements: `REQ-002`,`REQ-005`,`REQ-012`,`REQ-014`,`REQ-016`,`REQ-017`,`REQ-018`,`REQ-019`,`REQ-021`
- Dependencies: Step 6 committed。
- Baseline state: Service Long/MP/manual schema GREEN；six modules still depend on external organization/evaluation facade and Java serialization。
- Observable outcome: generated Service has a local facade module with five Proto sources；11 Evaluation methods are Triple providers，Organization directory uses local Proto clients；standard gRPC interop passes。
- End state: seven modules compile without external Egon facades；Proto is sole RPC wire authority；no native second server。
- Test-first gate: `Required — descriptor/provider/client/interop tests fail because facade module/generated Proto services and status mappings do not exist.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/META-INF/maven/archetype-metadata.xml`

- Purpose: add the seventh generated facade module and include filtered Proto sources。
- Symbols: module `${rootArtifactId}-facade`；fileSets for POM、`src/main/proto/**/*.proto`、tests/resources。
- Repository evidence: copied metadata lists common/domain/application/infrastructure/adapter/starter and no proto include。
- Dependencies and consumers: Files 2-3 source tree；generated parent module；Maven Archetype filtering。
- Why now: generation must materialize contract module before its compile tests can run。
- Contract/signature changes: exactly one facade module；caller properties unchanged；Proto `${package}` filtering enabled。
- Input/output and state mapping: archetype resources -> generated facade directory/source；no runtime state。
- Error and edge behavior: duplicate/missing module/fileSet fails verifier；generated Java never included as template input。
- Implementation pseudocode:

```xml
insert ${rootArtifactId}-facade module after common with filtered pom/proto/test fileSets
include src/main/proto/**/*.proto as filtered UTF-8 resources and ordinary Java test sources only
preserve all existing module IDs/dirs and reject generated target output or new archetype input properties
```

- Verification contribution: generated seven-module tree and Proto source presence。
- After this file: metadata expects facade resources created in Files 2-3。

#### File 2 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml`

- Purpose: define contract-only local module and Dubbo 3.3 Proto codegen。
- Symbols: local parent/artifact；`protobuf-java`；`dubbo-maven-plugin:compile` `tri`；test-scoped gRPC/AssertJ/JUnit as needed。
- Repository evidence: root BOM already manages Dubbo；official 3.3 plugin path；Spec §6 prohibits old plugins。
- Dependencies and consumers: Proto Files 3；generated service/message Java；adapter/infrastructure POMs File 5。
- Why now: compile prerequisite for descriptor RED tests and generated interfaces。
- Contract/signature changes: module depends on no generated business layer or `top.egon` facade artifact。
- Input/output and state mapping: five Proto sources -> target generated Java/JAR contract。
- Error and edge behavior: codegen failure/version drift stops compile；never commit output；no protobuf-java-util without consumer。
- Implementation pseudocode:

```xml
declare local ${rootArtifactId}-facade jar with protobuf-java runtime only
configure org.apache.dubbo:dubbo-maven-plugin:${dubbo.version} compile, protoSourceDir and target generated output, type tri
add test-only descriptor/gRPC dependencies; exclude old protobuf-maven-plugin, os-maven-plugin and all Egon facades
```

- Verification contribution: facade `clean compile/test` and generated-source path。
- After this file: module model exists；Proto compile RED until File 3。

#### File 3 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto/{evaluation/v1/course.proto,evaluation/v1/exam.proto,evaluation/v1/score.proto,organization/v1/user.proto,organization/v1/teaching.proto}`

- Purpose: define all 8 services/21 operations once in Service local contract copy。
- Symbols: CourseService 4、ExamService 4、ScoreService 3、UserService 2、RoleService 1、PermissionService 2、GradeService 2、SchoolClassService 3。
- Repository evidence: primary Spec §9 inventories exact fields/deadlines/errors；current Java facades provide method/DTO semantics。
- Dependencies and consumers: generated interfaces/messages；Service providers/clients；Web copy Step 11；parity Step 13。
- Why now: source of truth precedes provider/client production edits。
- Contract/signature changes: `proto3`，wire packages v1，positive int64 IDs，Timestamp/Empty，exact page fields/field numbers reserved。
- Input/output and state mapping: Java facade DTO/domain result fields -> Proto messages；void -> Empty；errors stay out of success payload。
- Error and edge behavior: zero ID invalid at adapter；reserved removed numbers；no generic wrapper/ellipsis/duplicate RPC stack。
- Implementation pseudocode:

```proto
declare exact unary services/methods and messages from RPC-001..021 with stable field numbers
use int64 IDs, Timestamp instants, Empty voids and page current_page/total_pages/page_size/total_count
set java_multiple_files and filtered java_package; reserve removed numbers and keep evaluation/organization wire packages stable
```

- Verification contribution: descriptor snapshot、codegen and Web parity baseline。
- After this file: generated contract interfaces/messages compile；provider/client adapters are still RED。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/test/java/facade/contract/ProtoDescriptorContractTest.java`

- Purpose: freeze all service/method/field/type/package numbers and no-breaking policy。
- Symbols: descriptor-set inspection for 5 files、8 services、21 methods、int64/Timestamp/Empty/page fields。
- Repository evidence: Spec `TEST-024` and current facade inventory are exact；module has no existing test。
- Dependencies and consumers: File 3 generated descriptors；future Web parity CI。
- Why now: establishes RED/contract before provider mapping and future edits。
- Contract/signature changes: test-only snapshot assertions；no production API beyond Proto。
- Input/output and state mapping: descriptor graph -> expected service/method/input/output/field-number map。
- Error and edge behavior: missing/extra/renumbered/wrong scalar/reused reserved number fails with exact descriptor path。
- Implementation pseudocode:

```java
load generated FileDescriptor objects for all five Proto sources
assert exact 8 service names, 21 unary method names and request/response descriptor identities
assert every ID is int64, time is Timestamp, void is Empty and page fields/numbers are exact with no extras
```

- Verification contribution: Proto source RED/GREEN and future compatibility gate。
- After this file: contract test is GREEN when File 3 matches Spec exactly。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-{adapter,infrastructure}/pom.xml}`

- Purpose: wire local facade module and remove external facade coordinates/dependencies/test fixtures。
- Symbols: seventh module；local facade dependencyManagement；adapter/infrastructure dependencies；Dubbo plugin management。
- Repository evidence: root has external facade properties/management；adapter eval and infrastructure org dependencies；outer archetype POM has test facade deps。
- Dependencies and consumers: Files 1-3 contract module；provider/client production Files 6-7。
- Why now: generated types must be on compilation classpaths before imports change。
- Contract/signature changes: remove all external `top.egon` facade properties/artifacts；local `${groupId}:${rootArtifactId}-facade` only。
- Input/output and state mapping: module dependency graph -> contract classes shared locally；no runtime call added。
- Error and edge behavior: architecture cycle forbidden；facade cannot depend on other local modules；dependency tree allowlist enforced。
- Implementation pseudocode:

```xml
add facade to generated modules/dependencyManagement and make adapter/infrastructure depend on local facade jar
remove organization/evaluation facade group/artifact/package properties and outer archetype test dependencies
keep facade dependency direction outward-only and Dubbo runtime only in adapter/infrastructure/starter consumers
```

- Verification contribution: seven-module Maven graph and exact top.egon allowlist。
- After this file: old facade imports fail until Files 6-7；external artifact resolution no longer required。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/{course,exam,handler}/**/*.java`

- Purpose: expose 11 Evaluation Proto operations through existing application use cases and canonical gRPC statuses。
- Symbols: current Course/Exam/Score FacadeImpl/converters/validators/GlobalFacadeExceptionHandler -> generated CourseService/ExamService/ScoreService mappings。
- Repository evidence: three facade impls already own method orchestration/conversion/error wrapping and use `@DubboService` groups course/exam/score。
- Dependencies and consumers: generated Proto interfaces/messages；Course/Exam/Score manage APIs；Dubbo registry/port。
- Why now: local contract/classpath exists；this is minimum provider GREEN path。
- Contract/signature changes: Java `SingleResponse` removed；methods accept/return Proto；groups/version preserved；3s/5s and trailing metadata mapping。
- Input/output and state mapping: int64/Timestamp/string fields -> Long/Instant commands -> results -> Proto；page fields exact。
- Error and edge behavior: INVALID_ARGUMENT/NOT_FOUND/ALREADY_EXISTS/FAILED_PRECONDITION/UNAVAILABLE/INTERNAL + error/trace metadata；no mutation retry。
- Implementation pseudocode:

```java
each @DubboService generated interface method validates positive IDs/required fields then calls existing manage command/query
map Long/Instant/status/page records to generated messages; return Empty only after successful void transaction
catch ApplicationException/validation/dependency failures once and throw canonical StatusRuntimeException with Egon trailing metadata
```

- Verification contribution: 11 provider contract/status/deadline tests。
- After this file: Evaluation Triple provider compiles and uses Proto only；Organization client remains File 7。

#### File 7 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/**`

- Purpose: replace external User/SchoolClass facades with local generated Proto client references while preserving the unused-but-defined directory port。
- Symbols: `DubboOrganizationDirectoryClient`、`OrganizationClientFailureMapper`、`LocalOrganizationDirectoryStub` Long projections。
- Repository evidence: current client has two `@DubboReference` fields and maps external DTOs to Domain projections；no Application use-case consumes new lookup。
- Dependencies and consumers: generated UserService/SchoolClassService；Domain `OrganizationDirectoryPort`；starter external-free tests。
- Why now: provider is GREEN and local contract already contains organization consumer messages。
- Contract/signature changes: methods accept Long；generated request/response；group/version preserved；no extra network call in business use cases。
- Input/output and state mapping: Long -> Proto int64 -> response -> Domain projection Long/status/list；client failure -> existing application-facing exception。
- Error and edge behavior: INVALID_ARGUMENT/NOT_FOUND/UNAVAILABLE/DEADLINE mapping；test stub stays local and no Nacos required。
- Implementation pseudocode:

```java
replace external facade references with generated UserService and SchoolClassService Dubbo references using existing group/version
build int64 request, invoke once with read deadline, map Proto fields to OrganizationUser/OrganizationSchoolClass Long projections
map StatusRuntimeException codes to current client failure model; never call this port from ScheduleCourse/RecordScore
```

- Verification contribution: Organization client/stub/external-free tests。
- After this file: all Service provider/consumer imports are local Proto；external facade source dependency absent。

#### File 8 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/rpc/EvaluationDubboTripleIntegrationTest.java`

- Purpose: prove 11 provider calls、groups/version、status/deadline and standard gRPC transport against Triple without a second server。
- Symbols: parameterized operation fixtures and low-level `ManagedChannel` unary calls。
- Repository evidence: copied test already exports/invokes Course/Exam Java facades in-process for course/exam groups。
- Dependencies and consumers: Files 3/6 provider classes；test gRPC deps；random local port/no registry fixture。
- Why now: complete provider path exists；this closes interop GREEN。
- Contract/signature changes: no external facade classes；cover all 11 Evaluation methods and gRPC full method names。
- Input/output and state mapping: Proto request fixtures -> stubbed manage results/errors -> Proto/status metadata。
- Error and edge behavior: command timeout not retried；read deadline/status cases；server shutdown/port cleanup deterministic。
- Implementation pseudocode:

```java
start one Dubbo Triple provider on random test port with mocked manage collaborators and no Nacos registry
invoke every evaluation method through generated Dubbo reference and representative methods through standard ManagedChannel unary descriptors
assert payload mapping, groups/version, canonical statuses/trailing metadata, deadlines and one underlying mutation call
```

- Verification contribution: standard gRPC interoperability and full provider matrix。
- After this file: Service local Proto/Triple slice is independently GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-service-open -am clean integration-test`
- Expected result: exit 0；seven generated modules；descriptor 8/21 contract GREEN；11 Service providers and Organization clients compile/test；dependency tree has no external facades。
- Failure returns to: Files 1/5 module graph，Files 2-4 codegen/descriptor，File 6 provider mapping，File 7 client mapping，File 8 transport/status。
- Completion criteria: Service Open uses one Triple Proto stack and no external Egon facade/native server。
- Rollback: revert Step paths；generated output/local test ports are ephemeral；no published contract。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/META-INF/maven/archetype-metadata.xml` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto/{evaluation/v1/course.proto,evaluation/v1/exam.proto,evaluation/v1/score.proto,organization/v1/user.proto,organization/v1/teaching.proto}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/test/java/facade/contract/ProtoDescriptorContractTest.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-{adapter,infrastructure}/pom.xml}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/{course,exam,handler}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/**` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/rpc/EvaluationDubboTripleIntegrationTest.java`
- Commit: `feat(service-open): make local protobuf the triple contract`

### Step 8 — 接入 Service Open 的 DTP、Nacos 与 ArchUnit 运行合同

- Requirements: `REQ-002`,`REQ-005`,`REQ-006`,`REQ-007`,`REQ-012`,`REQ-013`,`REQ-015`,`REQ-016`,`REQ-017`,`REQ-018`,`REQ-019`,`REQ-021`
- Dependencies: Step 7 committed。
- Baseline state: Service seven-module Long/MP/Proto project GREEN；no DTP/ArchUnit；copied Nacos image/internal plugin remain。
- Observable outcome: bounded Boot executor is DTP-governed，runtime machine/Nacos/DTP config exact，Service stays controller/Springdoc/Gateway-free，ArchUnit replaces internal plugin。
- End state: Service Open feature-complete and external-free under test profile。
- Test-first gate: `Required — starter context/async/architecture tests fail before DTP dependency/decorator/config/ArchUnit and copied plugin removal.`
- Ordered files:

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/{EvaluationServiceApplicationTest.java,EvaluationExternalFreeContextTest.java}`

- Purpose: freeze ID、DTP executor discovery、no Redis/Nacos socket and service-only/no-Springdoc/no-Gateway behavior。
- Symbols: context properties/bean assertions、local Organization stub、managed executor snapshot。
- Repository evidence: copied starter tests already verify external-free context and service-only boundary。
- Dependencies and consumers: POM/Async/YAML Files 2-4。
- Why now: RED runtime contract before wiring。
- Contract/signature changes: test machine-id `0`、DTP disabled or fake registry；no Controller/API added。
- Input/output and state mapping: profile -> bean graph；executor registry contains `applicationTaskExecutor` when DTP enabled under isolated fake setup。
- Error and edge behavior: missing machine ID fails；test profile opens no Nacos/Redis；Springdoc/Gateway beans absent。
- Implementation pseudocode:

```java
load Service test profile with explicit machine-id and local RPC stubs, asserting no external socket attempts
assert bounded applicationTaskExecutor is decorated/discoverable in a fake DTP context and cleanup preserves trace state
assert no business Controller, Springdoc OpenAPI bean, Gateway route locator or unapproved Egon artifact bean exists
```

- Verification contribution: runtime RED/GREEN/external-free proof。
- After this file: tests await dependency/config/wiring Files 2-4。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-starter/pom.xml}`

- Purpose: add DTP/ArchUnit and remove internal architecture plugin while preserving Service no-Springdoc boundary。
- Symbols: DTP starter runtime；ArchUnit JUnit5 test；remove bytecode plugin；no Gateway/Springdoc dependencies。
- Repository evidence: copied starter POM owns executable/runtime/plugin；root manages dependency versions。
- Dependencies and consumers: Async/File 3、architecture test File 5、context File 1。
- Why now: compile prerequisites for final governance。
- Contract/signature changes: direct top.egon allowlist only；local facade remains project groupId。
- Input/output and state mapping: Maven tree -> DTP/runtime + ArchUnit test；no public API change。
- Error and edge behavior: any Common MP/external facade/plugin/Gateway/Springdoc tree entry fails verifier。
- Implementation pseudocode:

```xml
starter adds approved DTP starter and test-scoped archunit-junit5 1.4.2
remove egon bytecode architecture plugin while preserving Boot/Cloud/Nacos/Dubbo/local facade dependencies
do not add Springdoc, Gateway, DTP Admin/Test, Common MP or external organization/evaluation facade artifacts
```

- Verification contribution: dependency tree/compile and architecture test。
- After this file: final dependencies exist；runtime YAML/Async remain RED。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/async/AsyncConfiguration.java`

- Purpose: govern the existing Boot executor through DTP without a new config layer。
- Symbols: `DtpTaskDecorator` bean、qualified `applicationTaskExecutor`、unchanged uncaught handler。
- Repository evidence: copied class returns null；DTP discovers Spring task executor beans。
- Dependencies and consumers: Boot task config、DTP registry、`@Async` call sites/tests。
- Why now: dependency exists and context expects managed executor。
- Contract/signature changes: null/default lookup -> explicit bounded Boot bean。
- Input/output and state mapping: async Runnable -> decorator trace/MDC capture/restore/cleanup；same exception logging。
- Error and edge behavior: missing executor fails context；no unbounded/common-pool fallback。
- Implementation pseudocode:

```java
publish one DtpTaskDecorator bean and inject Boot applicationTaskExecutor by qualifier
return that executor from AsyncConfigurer.getAsyncExecutor and retain current exception handler
rely on DTP auto-discovery for governance; never create a second raw pool or Admin service
```

- Verification contribution: async/DTP context tests。
- After this file: executor wiring exists；bounds/profile config await File 4。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{__rootArtifactId__-starter/src/main/resources/application*.yml,deploy/env/.env*.example,deploy/compose/*.yaml}`

- Purpose: configure explicit machine ID、bounded pool、DTP Redis/report、Nacos 3.0.3 and test disablement。
- Symbols: same standard property set as Light；Dubbo Triple port/group/version retained。
- Repository evidence: copied runtime configs have Nacos 2.5.1/no ID/DTP and test-local profiles。
- Dependencies and consumers: Common ID/DTP/Nacos/Dubbo/runtime operator/tests。
- Why now: bean graph exists，so config closes runtime GREEN。
- Contract/signature changes: no runtime machine default；test DTP/Nacos off；Service no Springdoc/Gateway config。
- Input/output and state mapping: env -> generator/pool/Redis/Nacos/Dubbo；no secrets in repository。
- Error and edge behavior: missing machine/Redis/Nacos prod fail-fast；tests no sockets；port collision configurable。
- Implementation pseudocode:

```yaml
add required EGON_ID_MACHINE_ID and env-overridable bounded spring.task.execution properties
enable DTP Redis/report in dev/prod, disable in test, and align every local compose Nacos image to 3.0.3
preserve tri port/groups/version and assert no springdoc/gateway property appears in any Service profile
```

- Verification contribution: config/context/compose tests and negative Service API boundary。
- After this file: runtime config is final。

#### File 5 — `CREATE egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/OpenArchitectureTest.java`

- Purpose: prove seven-module dependency direction and service-only/domain-first restrictions with ArchUnit。
- Symbols: facade independence；Adapter/Application/Domain/Infrastructure/Starter rules；no Controller；framework bans inward。
- Repository evidence: Service living architecture §§1-5 and domain-first Spec define exact packages；current plugin is removed。
- Dependencies and consumers: all compiled modules visible via starter dependencies；ArchUnit test dep。
- Why now: final graph stable after Proto/persistence/runtime transitions。
- Contract/signature changes: test-only architecture governance。
- Input/output and state mapping: compiled class dependencies -> exact violations；no runtime state。
- Error and edge behavior: cycles、Proto/Mapper in Domain、Controller、JPA/Flyway/Gateway/unapproved Egon dependencies fail。
- Implementation pseudocode:

```java
assert facade depends on no local business module and Domain depends only on common/Java/validation
assert Adapter->Application/facade, Infrastructure->Domain/facade, Starter composition and domain-first package rules
assert Service contains no business controller, Springdoc/Gateway/JPA/Flyway or internal Egon plugin/runtime imports
```

- Verification contribution: generated Service architecture gate。
- After this file: Service no longer relies on internal bytecode plugin。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/{student-management-service-only-rpc-mq-architecture.md,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`

- Purpose: synchronize seven-module Proto、Long/MP/manual SQL、DTP and operational instructions。
- Symbols: module tree、dependency graph、RPC method/package notes、manual DBA order、test commands/proof limits。
- Repository evidence: copied docs describe external canonical facades、six modules、UUID/JPA/Flyway。
- Dependencies and consumers: generated developers/operators/DBA；root docs later link。
- Why now: final implementation facts are known。
- Contract/signature changes: documentation only；service-only/no HTTP preserved。
- Input/output and state mapping: config/operation -> expected module/runtime behavior；no secret/data mutation。
- Error and edge behavior: missing schema/machine/Redis/Nacos/RPC deadline and no-auto-retry documented。
- Implementation pseudocode:

```markdown
document seven modules with local facade Proto and exact provider/consumer dependency directions
replace UUID/JPA/Flyway with Long/MP/manual SQL and add machine-id/DTP/Nacos/Triple status/deadline runbooks
retain service-only/MQ/domain-first constraints and explicitly state no Springdoc/Gateway/native gRPC server
```

- Verification contribution: generated docs/token/structure gate。
- After this file: Service Open code/config/docs are consistent。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-service-open -am clean integration-test`
- Expected result: exit 0；seven modules、Proto/DTP/ArchUnit/external-free tests pass；no Springdoc/Gateway/JPA/Flyway/UUID/external facade/internal plugin。
- Failure returns to: File 1 runtime contract，File 2 tree，File 3 executor，File 4 config，File 5 architecture，File 6 docs。
- Completion criteria: Service Open individually satisfies mapped runtime/product requirements without live infra。
- Rollback: revert only Service Open Step paths；no runtime/data state。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/{EvaluationServiceApplicationTest.java,EvaluationExternalFreeContextTest.java}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-starter/pom.xml}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/async/AsyncConfiguration.java` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/{__rootArtifactId__-starter/src/main/resources/application*.yml,deploy/env/.env*.example,deploy/compose/*.yaml}` `egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/OpenArchitectureTest.java` `egon-cola-archetypes/egon-cola-archetype-service-open/{student-management-service-only-rpc-mq-architecture.md,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`
- Commit: `feat(service-open): finalize open runtime governance`

### Step 9 — 将 Web Open 的组织模型与外围标识迁为 Snowflake Long

- Requirements: `REQ-003`,`REQ-005`,`REQ-009`,`REQ-012`,`REQ-017`,`REQ-021`
- Dependencies: Step 1；不得依赖尚未建立的本地 Proto facade 或 MyBatis-Plus Mapper。
- Baseline state: Web Open 是原 Web 模板的隔离复制，业务主键、事件 ID、请求/Trace 缺省值、缓存键、分片算法和持久化边界仍以 UUID/String 为中心。
- Observable outcome: User、Role、Permission、Grade、SchoolClass 及关系实体统一使用 Long；HTTP/GraphQL 对外仍输出十进制字符串；所有模板生成的标识来自 Common ID Starter；分片节点由 Long 精确公式决定。
- End state: Web Open 的 Java 主链与分片单测先完成 Long 化；复制来的 JPA 与 SQL 只作为 Step 10 必须消除的临时中间态。
- Test-first gate: `Required — 先把应用、领域、适配器与基础设施测试改成固定 Long/Snowflake 样例，并新增非法 Long、边界 Long、同 ID 同路由断言；RED 原因必须是旧 String/UUID 签名或旧算法。`
- Ordered files: 按 Test → Sharding → Domain → Application → Adapter → Infrastructure/Starter → Config 顺序修改，避免类型迁移期间出现双写或两套生成器。

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-application,__rootArtifactId__-domain,__rootArtifactId__-adapter,__rootArtifactId__-infrastructure,__rootArtifactId__-starter}/src/test/java/**/*.java`

- Purpose: 先固定 Web Long ID、外部字符串边界、事件/请求 ID 与分片行为。
- Symbols: User/Role/Permission/Grade/SchoolClass use-case tests；controller/GraphQL/facade tests；converter/repository/cache/MQ/sharding tests；starter rollback tests。
- Repository evidence: 对应复制测试直接实例化 `UuidV7Generator`、调用 `UUID.fromString` 或把 String UUID 写入领域/PO。
- Dependencies and consumers: 后续 Files 2-6 的所有类型、生成器、序列化和路由变更。
- Why now: 类型迁移前必须先定义可观察合同，避免机械替换掩盖 HTTP/GraphQL 字符串兼容性。
- Contract/signature changes: 测试期望改为 Long 内部值、HTTP/GraphQL 十进制 String、Proto 前置桥接 Long；不接受 UUID 兼容分支。
- Input/output and state mapping: 固定 Snowflake Long → 领域/事件/PO；Long → 对外十进制 String；非法空值、负数、溢出输入 → 明确校验错误。
- Error and edge behavior: `0`、负数、非数字、超 Long、缺失机器号和跨分片关系必须分别失败；测试随机后缀不得再依赖 UUID。
- Implementation pseudocode:

```java
replace UuidV7Generator fixtures with deterministic LongIdGenerator stubs and valid Snowflake samples
assert every web response/graphql scalar renders Long as decimal text while commands parse exact Long
assert event/request/trace fallbacks are generator-derived decimal strings and invalid IDs never reach use cases
```

- Verification contribution: 为 Web Long 迁移提供 RED/GREEN 主验收集。
- After this file: 测试应因生产代码仍为 UUID/String 而失败。

#### File 2 — `RENAME egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java -> egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithm.java`

- Purpose: 以 Spec 的 Snowflake Long stable-slot hash 替代 UUIDv7 哈希分片。
- Symbols: `SnowflakeLongShardingAlgorithm`、`ShardingNodeMap.routeSlot(long)`、对应重命名测试。
- Repository evidence: Web 复制算法解析 UUIDv7 并依赖 UUID hash；目标 Spec §7.4 规定 exact database/table 公式。
- Dependencies and consumers: ShardingSphere YAML、读写路由集成测试、Step 10 的八张表规则。
- Why now: 所有 Long 实体必须共享唯一可证明的路由算法。
- Contract/signature changes: `StandardShardingAlgorithm<Long>`；`hash=Long.hashCode(id)`、`spread=hash^(hash>>>16)`、`slot=spread&(nodeCount-1)`；database/table共用同一slot map。
- Input/output and state mapping: 正 Long sharding value + actual nodes → 唯一节点；range query 或未知 node → 拒绝。
- Error and edge behavior: null、非正值、节点数不一致、超配置节点、范围路由、关系路由键缺失均 fail-fast。
- Implementation pseudocode:

```java
parse only Number/decimal Long and require value > 0
compute Long.hashCode/spread/mask exactly once and resolve both database and table from the immutable slot map
validate available target names exactly match configured node maps before returning one node
```

- Verification contribution: 精确路由单测与 ShardingSphere 集成测试。
- After this file: Web 数据路由不再接受 UUID/String key。

#### File 3 — `RENAME egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java -> egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithmTest.java`

- Purpose: 锁定 Web Long stable-slot database/table选择与非法输入拒绝行为。
- Symbols: `SnowflakeLongShardingAlgorithmTest`、positive route、non-positive/range/missing-target cases。
- Repository evidence: 复制测试已覆盖 UUID precise/range和available-target branches，可沿用fixture结构而替换key合同。
- Dependencies and consumers: File 2 algorithm/node map；Step 10 routing integration；Step 13 generated verifier。
- Why now: production symbol已重命名，先让focused route test独立GREEN再迁其他层。
- Contract/signature changes: fixtures从UUID/String改为positive Long和预计算stable slot；删除UUID version/canonical assertions。
- Input/output and state mapping: known Long → expected immutable slot/database/table suffix；invalid value → deterministic exception。
- Error and edge behavior: null、zero、negative、range、missing target、non-power-of-two map全部拒绝且不broadcast。
- Implementation pseudocode:

```java
initialize node-count=4 and an exact immutable slot-to-database/table map, then precompute expected Long hash/spread/mask slots
assert precise positive Long values resolve the same slot for database and table target modes
assert null, non-positive, range, unavailable target and invalid node-map configurations throw without returning all targets
```

- Verification contribution: Web focused sharding RED/GREEN和Spec公式回归门禁。
- After this file: Web路由算法与测试名称、key类型和stable-slot合同一致。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/{user,teaching}/**/*.java`

- Purpose: 将 Web 领域标识和值对象、仓储端口、领域事件迁为 Long。
- Symbols: UserId、RoleId、PermissionId、GradeId、SchoolClassId、关系 ID、聚合、事件、repository ports。
- Repository evidence: 当前领域对象以 String 包装 UUID，且 living architecture 要求 Domain 不感知 Web/RPC/持久化框架。
- Dependencies and consumers: Application、Adapter、Infrastructure、Proto provider。
- Why now: 分片合同确定后建立整个 Web 业务的内部 canonical type。
- Contract/signature changes: ID value objects 持有非空正 Long；领域事件 ID 保持 String 字段时也只能来自 Long 生成器的十进制文本。
- Input/output and state mapping: Long → validated value object → aggregate；关系 ID/外键不做字符串兼容。
- Error and edge behavior: null/非正 Long 构造失败；跨聚合引用保留原不变量；禁止 framework/Proto/Mapper import。
- Implementation pseudocode:

```java
change all aggregate/value/repository ID signatures from String to Long-backed value objects
preserve role-permission, user-role, grade-class-membership invariants and event semantics
validate positive IDs at construction and expose no UUID parsing or persistence annotation
```

- Verification contribution: Domain 单测与 ArchUnit 输入。
- After this file: Web 内核拥有唯一 Long ID 模型。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/{user,teaching}/**/*.java`

- Purpose: 迁移 Web commands/results/managers/use cases，并统一业务、事件与幂等 fallback 的生成入口。
- Symbols: UserManageImpl、RoleManageImpl、PermissionManageImpl、GradeManageImpl、SchoolClassManageImpl、commands/results、ports。
- Repository evidence: managers 直接调用 `UUID.randomUUID()` 生成事件 ID，且当前 ID generator 类型为 UUIDv7。
- Dependencies and consumers: Domain value objects、Adapter controller/GraphQL/RPC、缓存/MQ/仓储实现。
- Why now: 领域签名完成后让所有业务写入只接受 Common Long generator。
- Contract/signature changes: 内部 command/result ID 为 Long；对外格式化留在 Adapter；构造函数依赖 `LongIdGenerator`。
- Input/output and state mapping: validated command → generated Long aggregate/relation/event ID → repository/event ports；caller-provided positive Long reference 原样保留。
- Error and edge behavior: 生成器异常回滚用例；空/重复幂等键沿用原语义；不得生成 UUID 或在 Application 猜测分片节点。
- Implementation pseudocode:

```java
inject LongIdGenerator into every create/assign/grant manager that needs a new identity
generate aggregate, relation and event IDs once per state transition and pass typed Long values inward
preserve transaction, idempotency, cache invalidation and event ordering while removing UUID fallbacks
```

- Verification contribution: Web use-case GREEN 与回滚行为。
- After this file: 应用业务链无 UUID 生成。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/**/*.java`

- Purpose: 保持 HTTP/GraphQL/API 兼容边界，同时移除 controller、filter、MQ 与临时 facade 中的 UUID fallback。
- Symbols: controllers、GraphQL resolvers、OrganizationTraceFilter、OrganizationMessageSupport、OrganizationFacadeSupport、request/response DTO converters。
- Repository evidence: 多个 adapter 类直接 `UUID.randomUUID()`；现有 HTTP DTO 已以 String 暴露主键。
- Dependencies and consumers: 浏览器/API consumer、Application ports、Step 11 本地 Proto provider。
- Why now: Application 已成为 Long canonical，Adapter 可做唯一边界转换。
- Contract/signature changes: HTTP/GraphQL 主键仍是十进制 String；请求、trace、消息 fallback 使用 Long generator；临时 Java facade 桥接只存活到 Step 11。
- Input/output and state mapping: external decimal text → checked Long → command；Long result → decimal text；header absent → generated decimal ID。
- Error and edge behavior: 非数字/负数/溢出返回既有参数错误 envelope；保留调用方合法 idempotency/trace 值；不得静默截断。
- Implementation pseudocode:

```java
centralize strict decimal Long parsing/formatting at controller/graphql/facade boundary
inject LongIdGenerator for missing request, trace and message IDs while preserving supplied nonblank values
bridge the copied Java facade temporarily with Long values only; mark removal in Step 11 rather than adding aliases
```

- Verification contribution: MVC/GraphQL/filter/MQ/facade boundary tests。
- After this file: Web 入口无 UUID，外部字符串合同不变。

#### File 7 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-infrastructure/src/main/java/infrastructure/**/*.java,__rootArtifactId__-starter/src/main/java/starter/OrganizationApplication.java,__rootArtifactId__-starter/src/test/java/**/*.java}`

- Purpose: 迁移 PO、converter、repository bridge、cache/MQ、starter generator bean 与回滚测试。
- Symbols: 八类 PO/converter/repository impl、cache clients、event/message publishers、OrganizationApplication。
- Repository evidence: PO/JPA 泛型仍为 String；starter 手工发布 `UuidV7Generator`；缓存和 MQ 序列化沿用 UUID 文本。
- Dependencies and consumers: Step 10 Mapper；Application repository ports；Redis/Rabbit/Kafka；starter context。
- Why now: 在持久化重写前先让所有基础设施结构与 Long canonical 对齐。
- Contract/signature changes: PO/外键/缓存 identity 为 Long；序列化对外使用十进制 String；starter 注入 Common ID Starter 的 `LongIdGenerator`，不自建实现。
- Input/output and state mapping: aggregate Long ↔ PO Long；Long ↔ stable cache/message decimal text；generator property → starter bean。
- Error and edge behavior: 旧 UUID cache/message 不做双读；缺失机器号留给 Step 12 fail-fast；序列化溢出拒绝。
- Implementation pseudocode:

```java
change PO, converter, repository adapter, cache key and event/message identity fields to Long
remove the UuidV7Generator @Bean and require the common-id-starter managed LongIdGenerator
update rollback/context tests to deterministic Long values without opening database or broker sockets
```

- Verification contribution: Infrastructure/Starter Long GREEN。
- After this file: Web Java 生成路径已无 UUID；JPA 和旧 SQL 留给 Step 10。

#### File 8 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application*.yml`

- Purpose: 使 Web ShardingSphere 规则引用 Long 算法并按 `grade_id` 保持关系聚合共址。
- Symbols: algorithm class、database/table nodes、User/Role/Permission/Grade/SchoolClass/relations routing keys。
- Repository evidence: 当前 YAML 指向 `UuidV7BucketShardingAlgorithm`；Web Spec 指定 class/membership route by `grade_id`。
- Dependencies and consumers: File 2 算法、Step 10 SQL 拓扑、运行配置。
- Why now: 完成 Long 类型切换的配置闭环。
- Contract/signature changes: route values 为 Long；SchoolClass 与 SchoolClassUser 以 `grade_id` 路由，其余表按各自聚合 ID 规则。
- Input/output and state mapping: Long route key → one database/table；read/write splitting 保持原语义。
- Error and edge behavior: 缺 route key 禁止广播写；测试配置继续本地隔离；不在此步启动 schema 初始化。
- Implementation pseudocode:

```yaml
replace every UUID algorithm class/property with SnowflakeLongShardingAlgorithm and exact node counts/slot map
route school_class and school_class_user by grade_id; keep identity/authorization aggregates on their specified Long key
retain read/write topology while deleting UUID-only parsing knobs and all auto-init behavior
```

- Verification contribution: routing integration and config token gate。
- After this file: Web Long/sharding phase完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-web-open -am clean test -DskipITs`
- Expected result: exit 0；Web Long/sharding/API 边界测试通过；Java 运行路径无 UUID/UuidV7；SQL/JPA 的临时残留由 Step 10 处理。
- Failure returns to: File 1 expectations，File 2 formula，File 3 focused route test，File 4 domain types，File 5 orchestration，File 6 boundary conversion，File 7 infrastructure，File 8 routes。
- Completion criteria: Web 的所有新标识都由 Common Long generator 产生，外部 API 仍是十进制字符串，路由可由固定 Long 精确复算。
- Rollback: 回退 Web Open Step 9 路径；无数据迁移或运行状态。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-application,__rootArtifactId__-domain,__rootArtifactId__-adapter,__rootArtifactId__-infrastructure,__rootArtifactId__-starter}/src/test/java/**/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithm.java -> egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithm.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/UuidV7BucketShardingAlgorithmTest.java -> egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/config/datasource/SnowflakeLongShardingAlgorithmTest.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/{user,teaching}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/{user,teaching}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/**/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-infrastructure/src/main/java/infrastructure/**/*.java,__rootArtifactId__-starter/src/main/java/starter/OrganizationApplication.java,__rootArtifactId__-starter/src/test/java/**/*.java}` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application*.yml`
- Commit: `refactor(web-open): migrate identities and routing to long`

### Step 10 — 用 MyBatis-Plus 与手工 SQL 完成 Web Open 持久化

- Requirements: `REQ-003`,`REQ-008`,`REQ-009`,`REQ-010`,`REQ-011`,`REQ-012`,`REQ-016`,`REQ-017`,`REQ-019`,`REQ-021`
- Dependencies: Step 9 Long canonical types and routes。
- Baseline state: Web Open 已 Long 化，但八个 JPA repository、JPA annotations/dependencies、Flyway 和 UUID SQL 仍是明确的临时中间态。
- Observable outcome: 八个聚合/关系 repository 全部由 MyBatis-Plus Mapper 驱动；六份手工 SQL 中 Web 的 master/shard 两份可由 DBA 顺序执行；生成项目 classpath 不自动变更数据库。
- End state: Web Open 无 Spring Data JPA、Hibernate、Flyway、Liquibase 或 SQL auto-init；Long/BIGINT schema、Mapper、converter 与 ShardingSphere 一致。
- Test-first gate: `Required — 先把 repository slice/integration tests 改为 Mapper mock/真实 MyBatis H2 或 Testcontainers 模式，并为所有原派生查询写等价行为断言；RED 必须指向尚未创建的 Mapper/XML/config。`
- Ordered files: Test → Mapper/XML → repository adapters → PO → POM → datasource config → manual SQL/runbook；删除旧 JPA/Flyway 文件与资源放在同一原子步骤。

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/{user,teaching,config}/**/*.java`

- Purpose: 为八个 Mapper 和分片 SQL 建立行为等价测试。
- Symbols: User/Role/Permission/Grade/SchoolClass repositories、关系查询、read/write routing tests。
- Repository evidence: 原测试 autowire JPA repository 并依赖 JPA schema 生命周期。
- Dependencies and consumers: Files 2-7 的 Mapper、adapter、schema/config。
- Why now: 先冻结派生查询、计数、关联顺序与事务语义。
- Contract/signature changes: 测试夹具 ID/BIGINT；repository contract 不变，仅 infra 实现替换。
- Input/output and state mapping: typed Long repository calls → exact rows/count/order；write → master，eligible read → replica/route target。
- Error and edge behavior: empty IN、duplicate relation、missing foreign aggregate、wrong route key、null result 分别有断言。
- Implementation pseudocode:

```java
replace JpaRepository wiring with mapper/repository-adapter fixtures and explicit deterministic schema setup
cover every old derived method with exact wrapper/XML query semantics, including relation counts and ordered lookups
assert write/master and grade_id co-location behavior without Flyway or spring.sql.init
```

- Verification contribution: Web persistence RED/GREEN 主门禁。
- After this file: tests fail because Mapper/XML/config 尚不存在。

#### File 2 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{user,teaching}/repo/mapper/{UserMapper,RoleMapper,PermissionMapper,UserRoleMapper,RolePermissionMapper,GradeMapper,SchoolClassMapper,SchoolClassUserMapper}.java`

- Purpose: 用八个窄 Mapper 替代八个 JPA repository。
- Symbols: `BaseMapper<...PO>` 与仅覆盖旧 repository 派生能力的显式方法。
- Repository evidence: 实际 Web 基线包含 8 个 JPA repositories，而不是抽象估计数；PLAN-CLAR-001 要求全部迁移。
- Dependencies and consumers: repository impl、XML、MP 3.5.17、ShardingSphere datasource。
- Why now: tests 已定义等价查询集合。
- Contract/signature changes: infra-internal API 从 Optional/JPA derived names 变为 Mapper select/count/insert/update/delete；Domain ports 不变。
- Input/output and state mapping: PO/filter/Long IDs → affected rows/list/nullable record；adapter 负责 Optional 转换。
- Error and edge behavior: empty collections 直接返回 empty；关系唯一冲突传播为领域可识别异常；禁止动态表名与字符串 SQL 拼接。
- Implementation pseudocode:

```java
extend BaseMapper for each PO and declare only nontrivial old-derived queries
use typed Long parameters including gradeId on school class and membership routes
return stable collections/counts and keep pagination/order explicit at mapper boundary
```

- Verification contribution: Mapper compile 与 repository tests。
- After this file: mapper interfaces ready；complex SQL waits File 3。

#### File 3 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/mapper/{user,teaching}/*.xml`

- Purpose: 实现无法由安全 Wrapper 清晰表达的授权关系、成员关系和有序批量查询。
- Symbols: role-permission/user-role chains、permission lookup、class membership、grade/class duplicate checks。
- Repository evidence: 原 JPA methods 有 `findByRoleIdIn`、`countBy...` 和关联链，不能用未审计字符串 SQL 替代。
- Dependencies and consumers: File 2 Mapper method IDs、PO column aliases、manual SQL indexes。
- Why now: Mapper 接口已稳定，可一一匹配 statement。
- Contract/signature changes: XML statement id/signature 精确匹配；返回 Long/BIGINT。
- Input/output and state mapping: bounded Long lists/filters → parameterized SELECT/count；结果映射 → PO/Long。
- Error and edge behavior: empty list 在 Java 层截断；排序稳定；所有 route key 顶层显式传递；无 `${}` 动态值插值。
- Implementation pseudocode:

```xml
map snake_case BIGINT columns explicitly to Long PO properties
implement authorization and membership joins with bound parameters, deterministic ordering and grade_id predicates
keep physical table selection under ShardingSphere; never interpolate table names or route keys
```

- Verification contribution: XML parse、query parity、SQL safety gates。
- After this file: 所有旧派生查询已有可审计实现。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{user,teaching}/repo/{impl,po,converter}/**/*.java`

- Purpose: 将仓储适配器切到 Mapper，并把 PO 变成普通 MyBatis-Plus 持久化对象。
- Symbols: 五个 Domain repository implementations、八个 PO、converters。
- Repository evidence: 当前实现注入 JPA repositories、调用 `save/findById/countBy...`，PO 使用 JPA annotation。
- Dependencies and consumers: Files 2-3、Domain ports、transaction manager。
- Why now: Mapper/SQL 已能提供完整旧能力。
- Contract/signature changes: repository constructor dependency 与内部调用变化；Domain 接口/语义保持。
- Input/output and state mapping: aggregate ↔ PO ↔ Mapper；insert/update 根据 Long ID/状态显式选择；关系写同 route key。
- Error and edge behavior: affected rows 非预期 fail；duplicate/not-found 保持原异常；批量查询空输入无 SQL。
- Implementation pseudocode:

```java
replace every JpaRepository dependency with the matching mapper and typed query wrapper/XML method
annotate PO only with MyBatis-Plus table/id/field metadata and keep converters framework-free
preserve domain Optional/not-found/duplicate semantics and verify exact affected-row counts
```

- Verification contribution: repository parity tests。
- After this file: runtime persistence path is MP-only。

#### File 5 — `DELETE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{user,teaching}/repo/jpa/*.java`

- Purpose: 物理消除八个 JPA repositories，防止双持久化路径。
- Symbols: User、Role、Permission、UserRole、RolePermission、Grade、SchoolClass、SchoolClassUser JPA repositories。
- Repository evidence: 这些文件是 Spring Data JPA 的唯一 Web repository 声明集合。
- Dependencies and consumers: File 4 已替换所有消费者；POM 清理由 File 6 完成。
- Why now: Mapper path GREEN 后立即删除兼容路径。
- Contract/signature changes: 删除 infra-internal JPA API；无 public/domain API 变化。
- Input/output and state mapping: not applicable；删除代码路径。
- Error and edge behavior: `rg JpaRepository|jakarta.persistence` 必须无命中；若有消费者编译失败则返回 File 4。
- Implementation pseudocode:

```text
delete all eight jpa repository declarations after mapper adapters compile
reject aliases, deprecated wrappers or mixed JPA/MP fallback
verify every former consumer imports a Mapper or Domain repository port and the deleted package has zero source files
```

- Verification contribution: negative dependency/source scan。
- After this file: Web 无 JPA source。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/pom.xml}`

- Purpose: 锁定官方开源 persistence BOM/artifacts，并移除 JPA/Flyway；复制模板的内部架构插件暂留到 Step 12 由 ArchUnit 原子替换。
- Symbols: MP 3.5.17、ShardingSphere 5.5.3、MySQL/H2/test dependencies。
- Repository evidence: 复制 POM 含 JPA、Flyway、内部 architecture plugin 与旧依赖；目标禁止 Common MP Starter。
- Dependencies and consumers: Mapper、starter runtime、Step 12 ArchUnit。
- Why now: source path 已切换，可让 Maven 依赖图成为强边界。
- Contract/signature changes: 移除 `spring-boot-starter-data-jpa` 与 Flyway/Liquibase；加入官方 MP starter；不加 Common MP Starter；内部插件只保留到 Step 12。
- Input/output and state mapping: Maven coordinates → generated classpath；no runtime state。
- Error and edge behavior: dependency convergence/enforcer 失败阻断；不得排除规则掩盖 JPA/Flyway transitive leak。
- Implementation pseudocode:

```xml
import/pin official mybatis-plus 3.5.17 and sharding 5.5.3 within the approved Spring matrix
remove JPA, Hibernate and Flyway/Liquibase dependencies/configuration without weakening convergence checks
retain the copied architecture plugin until Step 12 replaces it with scoped ArchUnit test support and the exact top.egon allowlist
```

- Verification contribution: dependency tree ban gate。
- After this file: generated Web classpath persistence is MP-only；架构插件是 Step 12 必须删除的可追踪临时项。

#### File 7 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application*.yml`

- Purpose: 配置 MP mapper locations、关闭所有 schema automation，并保持 ShardingSphere Long route/read-write rules。
- Symbols: mybatis-plus mapper-locations/configuration；spring.flyway/sql.init/JPA removals；datasource/sharding properties。
- Repository evidence: 复制配置仍有 Flyway/DDL lifecycle；Step 9 已写 Long route。
- Dependencies and consumers: Mapper XML、starter context、manual SQL operator。
- Why now: POM/source 已完成切换，配置必须去掉隐式数据库副作用。
- Contract/signature changes: 应用启动只校验/使用现有 schema，不创建、不升级、不种子化。
- Input/output and state mapping: datasource properties → existing schema；mapper resources → MP statements。
- Error and edge behavior: missing table/schema 启动或首个访问明确失败；test profile 也不得启用自动 SQL。
- Implementation pseudocode:

```yaml
configure mybatis-plus mapper locations and safe naming/enum options
remove flyway, hibernate ddl, spring.sql.init and any embedded auto-schema switches from every profile
retain Step 9 Long sharding/read-write rules and require operator-provisioned schema
```

- Verification contribution: context/negative config gates。
- After this file: 程序无自动数据库变更入口。

#### File 8 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/deploy/sql/{mysql-master.sql,mysql-shard.sql,README.md}`

- Purpose: 提供 Web 的两份可手工执行 SQL 与顺序/校验/回滚 runbook。
- Symbols: 8 logical tables、BIGINT PK/FK、grade_id route/index、reserved seed IDs 1/2、manual verification queries。
- Repository evidence: 复制 Flyway migrations 使用 UUID/VARCHAR；primary Spec §8.3 指定六份 SQL 总体合同与 reserved seed。
- Dependencies and consumers: DBA/operator、ShardingSphere nodes、Mapper XML。
- Why now: Java/config final 后可从实际模型反推唯一 schema。
- Contract/signature changes: schema delivery 从应用自动迁移改为 DBA 手工执行；SQL 不在 classpath auto-init 位置。
- Input/output and state mapping: master SQL → non-sharded metadata/reference tables；shard SQL → every physical shard/table；seed 1/2 仅保留示例系统数据。
- Error and edge behavior: `IF NOT EXISTS` 只用于可重跑 DDL；不覆盖已有列/数据；回滚前要求备份；分片节点必须逐一校验。
- Implementation pseudocode:

```sql
define every identifier and foreign/route key as BIGINT with explicit unique and route indexes
place grade/class/membership rows so grade_id is present and indexed on every routed table
document manual order: backup -> all shard DDL -> master DDL/seed -> schema/index/count verification -> enable app
```

- Verification contribution: SQL token/schema parity review；不由应用执行。
- After this file: Web 持久化与手工数据库交付闭环。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-web-open -am clean integration-test && ! rg -n 'JpaRepository|jakarta\.persistence|spring-boot-starter-data-jpa|flyway|liquibase|spring\.sql\.init' egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources --glob '!README*' --glob '!deploy/sql/README.md'`
- Expected result: exit 0；8 个 Mapper 与 repository parity/routing tests 通过；禁止项无命中；应用未执行任何 SQL。
- Failure returns to: File 1 parity，Files 2-3 Mapper/XML，File 4 adapter，File 5 residue，File 6 classpath，File 7 config，File 8 schema。
- Completion criteria: Web Open 仅使用官方 MP + ShardingSphere，且数据库结构只能按 manual SQL/runbook 交付。
- Rollback: 回退 Web Open Step 10 代码/配置/SQL；若 DBA 在未来执行 SQL，按 runbook 备份/回退，不由应用负责。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/{user,teaching,config}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{user,teaching}/repo/mapper/{UserMapper,RoleMapper,PermissionMapper,UserRoleMapper,RolePermissionMapper,GradeMapper,SchoolClassMapper,SchoolClassUserMapper}.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/mapper/{user,teaching}/*.xml` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{user,teaching}/repo/{impl,po,converter}/**/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/{user,teaching}/repo/jpa/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/pom.xml}` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application*.yml` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/deploy/sql/{mysql-master.sql,mysql-shard.sql,README.md}`
- Commit: `refactor(web-open): replace jpa with mybatis plus`

### Step 11 — 建立 Web Open 本地 Proto 与 Triple RPC

- Requirements: `REQ-003`,`REQ-005`,`REQ-012`,`REQ-014`,`REQ-016`,`REQ-017`,`REQ-018`,`REQ-019`,`REQ-021`
- Dependencies: Steps 9-10 Long and persistence contracts；Service Step 7 定义的同源 Proto 内容。
- Baseline state: Web Open 仍依赖外部 Java facade artifacts，Adapter provider 直接实现外部接口；没有本地 `-facade` module、Proto source 或标准 gRPC evaluation client。
- Observable outcome: Web Open 成为七模块；本地 facade 含与 Service byte-identical 的 5 个 Proto 文件；组织 provider 以 Dubbo Triple 暴露 10 个方法；evaluation client 调用 3 个 Service 方法；无外部 facade artifact。
- End state: Proto 是唯一 wire contract；Web/Service 模板可独立生成和构建，跨模板只通过复制时固定、内容相同的 Proto 协议协作。
- Test-first gate: `Required — 先新增 descriptor、provider mapping、标准 gRPC consumer、外部 artifact absence tests；RED 必须是 facade module/generated types 尚不存在或 provider 仍实现 Java facade。`
- Ordered files: Module metadata → facade POM/Proto → generated source contract → reactor deps → provider adapters → client → runtime/test；Service/Web Proto 源在文件完成后用 `cmp` 验证一致。

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/META-INF/maven/archetype-metadata.xml`

- Purpose: 把 `__rootArtifactId__-facade` 纳入 Web 生成清单。
- Symbols: facade module fileSet、proto filtering=false、Java package filtering。
- Repository evidence: 复制 metadata 只有六模块；domain-first 包和非过滤 Proto 需要不同规则。
- Dependencies and consumers: generated tree、verify.groovy、root POM。
- Why now: 先确保后续新文件会进入生成物。
- Contract/signature changes: generated module count 6→7。
- Input/output and state mapping: facade template files → generated facade module；Proto bytes unchanged。
- Error and edge behavior: 漏 fileSet 或错误 filtering 必须由 generated verifier 失败。
- Implementation pseudocode:

```xml
add facade pom/java/proto file sets beside the six copied modules
keep proto resources unfiltered and java package files filtered consistently with existing modules
preserve every original Web module/resource entry
```

- Verification contribution: generated tree/Proto byte gate。
- After this file: archetype metadata 能生成 facade。

#### File 2 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml`

- Purpose: 定义仅含协议、生成类与最小验证依赖的 facade module。
- Symbols: protobuf BOM/runtime、grpc-stub/protobuf、Dubbo Triple codegen plugin、generated-source setup。
- Repository evidence: Web 当前依赖外部 facade；Dubbo 3.3 官方 Triple IDL 支持从 Proto 生成可供 Dubbo/标准 gRPC 使用的 stub。
- Dependencies and consumers: Adapter provider、Infrastructure evaluation client、descriptor tests。
- Why now: metadata 已声明模块，随后 Proto 需要构建宿主。
- Contract/signature changes: 本地 `${project.groupId}:${rootArtifactId}-facade` 替代外部 facade artifacts。
- Input/output and state mapping: Proto source → `target/generated-sources/protobuf/java`/gRPC types；no runtime state。
- Error and edge behavior: codegen/plugin 版本必须由 root dependency/plugin management 锁定；禁止引入业务模块或 Spring runtime。
- Implementation pseudocode:

```xml
create a leaf facade jar with protobuf/grpc API dependencies and Dubbo 3.3.6 Triple IDL generation
add target/generated-sources/protobuf/java to compilation through the official generation lifecycle
depend on no local application/domain/infrastructure/starter module and no external canonical facade
```

- Verification contribution: facade generate-sources/compile gate。
- After this file: local protocol module可构建。

#### File 3 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto/{common/v1/common.proto,organization/v1/user.proto,organization/v1/authorization.proto,organization/v1/teaching.proto,evaluation/v1/evaluation.proto}`

- Purpose: 定义 Web provider 与 evaluation consumer 的唯一 wire contract。
- Symbols: 与 Service Step 7 完全相同的 package/message/service/method/field numbers；8 services/21 methods；Web provider 10、evaluation client 3。
- Repository evidence: primary Spec §9.1-§9.4 已冻结服务/方法与 Long/int64/decimal String 边界；Service/Web 源需 byte-identical。
- Dependencies and consumers: generated interfaces、provider adapters、standard gRPC client、contract tests。
- Why now: facade build 已就绪。
- Contract/signature changes: Java facade wire contract 被 Proto 取代；ID fields 为 `int64`；分页/错误/trace 使用 common messages。
- Input/output and state mapping: domain Long ↔ Proto int64；HTTP decimal String 不进入 Proto；timestamps/status/error 明确映射。
- Error and edge behavior: 正 ID validation、deadline/status mapping、reserved field numbers、无 Java serialization fallback。
- Implementation pseudocode:

```proto
copy the exact five Service Open proto sources with identical bytes, packages, field numbers and reservations
retain all 8 services and 21 methods; Web implements the 10 organization methods and consumes 3 evaluation reads
use int64 for identifiers and common status/page/error/trace messages without HTTP string aliases
```

- Verification contribution: `cmp`、descriptor、method-count、breaking-contract review。
- After this file: Service/Web wire contract source完全一致。

#### File 4 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/test/java/facade/ProtoContractTest.java`

- Purpose: 锁定 5 文件、8 services、21 methods、field numbers 与 int64 ID 类型。
- Symbols: descriptor traversal；organization/evaluation ownership sets；forbidden Java facade/gateway assumptions。
- Repository evidence: 文本扫描不足以证明 descriptor 级协议完整性。
- Dependencies and consumers: File 3 generated descriptors；Step 13 verifier。
- Why now: 协议源已存在，先于 provider 实现建立 contract GREEN。
- Contract/signature changes: test-only executable specification。
- Input/output and state mapping: descriptors → exact sets/counts/types；no state。
- Error and edge behavior: rename、renumber、method omission、ID 类型漂移、unexpected service 均失败。
- Implementation pseudocode:

```java
load every generated descriptor and assert exact package/service/method/field-number sets
assert all identity fields use int64 and ownership is 10 Web provider methods plus 11 peer definitions
assert total service/method counts are 8/21 and common messages are reused rather than duplicated
```

- Verification contribution: protocol executable gate。
- After this file: Proto compatibility可被自动验证。

#### File 5 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-adapter/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/pom.xml}`

- Purpose: 注册 facade module、替换所有外部 facade dependencies，并布置 provider/client依赖方向。
- Symbols: modules、dependencyManagement、local facade dependency、Dubbo/grpc test runtime。
- Repository evidence: Web copied POM 有 external facade coordinates/properties，架构要求 Facade leaf + Adapter provider + Infrastructure consumer。
- Dependencies and consumers: Files 2-4、Files 6-8、generated reactor。
- Why now: contract稳定后切 reactor/classpath。
- Contract/signature changes: module count 7；删除 external facade properties/dependencies；Adapter/Infrastructure/Starter 仅依赖 local facade。
- Input/output and state mapping: reactor modules → ordered build/classpath；no runtime state。
- Error and edge behavior: dependency tree 发现 external facade、Gateway、native gRPC server transport 或 facade→business dependency 即失败。
- Implementation pseudocode:

```xml
register facade before adapter/application consumers and manage all protocol/runtime versions centrally
replace every external organization/evaluation facade coordinate with the local facade artifact
keep facade a leaf, provider in adapter, evaluation client in infrastructure and composition in starter
```

- Verification contribution: reactor/dependency direction gates。
- After this file: Web 不再需要外部 facade artifact。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/**/*.java`

- Purpose: 让现有组织 provider adapters 实现本地 generated Triple interfaces，并映射 10 个方法。
- Symbols: User 2、Role 1、Permission 2、Grade 2、SchoolClass 3；OrganizationFacadeSupport/error/status mapping。
- Repository evidence: 当前 adapters 直接实现外部 Java facade interfaces；Application use cases 已在 Step 9 Long 化。
- Dependencies and consumers: generated facade、Application ports、Dubbo service export、contract tests。
- Why now: reactor 已切本地协议。
- Contract/signature changes: provider method signatures改为 generated request/response/observer contract；业务语义不变。
- Input/output and state mapping: Proto int64/request metadata → Application command/query → domain result → Proto response/status。
- Error and edge behavior: invalid argument→INVALID_ARGUMENT；not found→NOT_FOUND；conflict→ALREADY_EXISTS/FAILED_PRECONDITION；unexpected→INTERNAL；保留 trace且不泄露内部堆栈。
- Implementation pseudocode:

```java
implement the generated organization service interfaces and map all 10 methods to existing application ports
convert int64 IDs to typed Long value objects and map domain errors to stable gRPC status responses
remove external Java facade imports/support aliases and export only through Dubbo Triple
```

- Verification contribution: provider mapping/status tests。
- After this file: Web organization provider完全本地化。

#### File 7 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/evaluation/client/GrpcEvaluationQueryClient.java`

- Purpose: 用标准 gRPC stub 建立最小 evaluation 只读 client，证明 Triple 互操作。
- Symbols: GetCourse、GetExam、GetScore client methods；deadline/channel lifecycle/status mapping。
- Repository evidence: primary Spec 只要求 3 个 evaluation consumer methods，且标准 gRPC client 是测试 interoperability 的边界。
- Dependencies and consumers: generated evaluation stubs、Application outbound port 或现有 facade adapter、starter config。
- Why now: 本地 Proto 和依赖图稳定后实现唯一必要 consumer。
- Contract/signature changes: 外部 Java facade client 被标准 gRPC client 替代；不新增业务查询。
- Input/output and state mapping: positive Long + trace/deadline → Proto request → domain-facing DTO；no cache/retry by default。
- Error and edge behavior: deadline exceeded、unavailable、not found 映射为现有 infra/domain error；写调用禁止自动 retry；channel 受 Spring lifecycle 管理。
- Implementation pseudocode:

```java
build one managed-channel-backed standard gRPC blocking/async stub with configurable target and deadline
implement only GetCourse, GetExam and GetScore and map int64 IDs/statuses to existing outbound models
close the channel on shutdown and perform no hidden retry, discovery fallback or extra organization lookup
```

- Verification contribution: mock/in-process standard gRPC interoperability tests。
- After this file: Web 能以标准 gRPC client调用 Service Triple endpoint。

#### File 8 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-starter/src/main/java/starter/config/rpc/**/*.java,__rootArtifactId__-starter/src/main/resources/application*.yml,__rootArtifactId__-adapter/src/test/java/**/*.java,__rootArtifactId__-infrastructure/src/test/java/**/*.java,__rootArtifactId__-starter/src/test/java/**/*.java}`

- Purpose: 配置 Dubbo Triple export/Nacos discovery 与 gRPC evaluation target/deadline，并完成 provider/consumer/外部依赖验收。
- Symbols: 组织 service bean/export configs、evaluation client config、provider mapping tests、gRPC interoperability/context tests。
- Repository evidence: copied configs 面向 external facade；Web 需 Spring MVC/GraphQL + Triple provider，但不能启动 native gRPC server。
- Dependencies and consumers: Files 5-7、Nacos/Dubbo runtime、Step 12 final profiles。
- Why now: provider/client implementations齐全。
- Contract/signature changes: protocol=`tri`；group/version保持 `student-management-organization`/`1.0.0`；target/deadline env-overridable。
- Input/output and state mapping: config → Dubbo exported generated service + standard gRPC client；test profile → in-process/no external sockets。
- Error and edge behavior: duplicate export、port collision、missing target、deadline、discovery unavailable明确失败；无 native grpc-server starter。
- Implementation pseudocode:

```java
export the five generated organization services through Dubbo Triple with the existing group/version contract
configure the evaluation standard gRPC client target/deadline separately from Dubbo discovery internals
test all 10 provider mappings and 3 consumer calls in-process, then assert zero external facade dependencies
```

- Verification contribution: Triple provider + standard gRPC client + external-free integration gate。
- After this file: Web 本地 Proto/Triple链闭环。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-web-open -am clean integration-test && cmp egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto/common/v1/common.proto egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto/common/v1/common.proto && diff -qr egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto`
- Expected result: exit 0；七模块生成/编译；descriptor 8/21；10 provider + 3 client tests；5 Proto 源 byte-identical；无 external facade/native gRPC server。
- Failure returns to: File 1 metadata，File 2 codegen，Files 3-4 contract，File 5 graph，File 6 provider，File 7 client，File 8 runtime/tests。
- Completion criteria: Web 以本地 Proto 作为唯一 wire contract，通过 Dubbo Triple 提供组织服务，并可由标准 gRPC 调用 evaluation 服务。
- Rollback: 回退 Web Open Step 11 paths；无外部发布或注册中心变更。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/META-INF/maven/archetype-metadata.xml` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto/{common/v1/common.proto,organization/v1/user.proto,organization/v1/authorization.proto,organization/v1/teaching.proto,evaluation/v1/evaluation.proto}` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/test/java/facade/ProtoContractTest.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-adapter/pom.xml,__rootArtifactId__-infrastructure/pom.xml,__rootArtifactId__-starter/pom.xml}` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/**/*.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/evaluation/client/GrpcEvaluationQueryClient.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-starter/src/main/java/starter/config/rpc/**/*.java,__rootArtifactId__-starter/src/main/resources/application*.yml,__rootArtifactId__-adapter/src/test/java/**/*.java,__rootArtifactId__-infrastructure/src/test/java/**/*.java,__rootArtifactId__-starter/src/test/java/**/*.java}`
- Commit: `feat(web-open): add local proto and triple contracts`

### Step 12 — 接入 Web Open 的 DTP、Nacos、Springdoc 与 ArchUnit 运行合同

- Requirements: `REQ-003`,`REQ-005`,`REQ-006`,`REQ-007`,`REQ-012`,`REQ-013`,`REQ-015`,`REQ-016`,`REQ-017`,`REQ-018`,`REQ-019`,`REQ-021`
- Dependencies: Steps 9-11 complete Web Java/POM/runtime graph。
- Baseline state: Web Open 已 Long/MP/Proto，但仍需最终 open dependency allowlist、DTP pool/config、Nacos 3.0.3、Springdoc 2.8.17、ArchUnit、manual SQL/docs 与 Gateway negative boundary。
- Observable outcome: Web app 启动时拥有一个 DTP 治理的 Boot applicationTaskExecutor、显式 Snowflake machine ID、Springdoc UI/API、Triple/Nacos runtime；架构测试强制七模块/禁止项；不生成 Gateway module/component。
- End state: Web Open 可独立生成并离线完成 context/architecture/API/RPC tests；生产部署仍需 operator 提供 DB、Redis、Nacos、broker 与 machine ID。
- Test-first gate: `Required — 先建立 context、OpenAPI、DTP、architecture、external-free tests；RED 必须来自缺依赖/bean/config 或残留 forbidden artifact。`
- Ordered files: Tests → POM → AsyncConfiguration → profiles/deploy → ArchUnit → docs；不新增第二 executor 或 Gateway placeholder。

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/{OrganizationApplicationTest,OrganizationExternalFreeContextTest,OpenApiContractTest}.java`

- Purpose: 固定 Web runnable bean graph、DTP discovery、Springdoc endpoints、Triple/MP/ID 存在和所有禁止项缺席。
- Symbols: one applicationTaskExecutor；LongIdGenerator；DtpTaskDecorator；OpenAPI group；local facade/provider/client；no Gateway/JPA/Flyway/external facades。
- Repository evidence: copied context tests仍检查旧依赖；Web 必须保留 API docs，Service 必须不含，Gateway 必须外置。
- Dependencies and consumers: Files 2-6。
- Why now: runtime components stable，可先写最终 RED contract。
- Contract/signature changes: test-only；API docs URL/metadata 作为生成项目合同。
- Input/output and state mapping: test profile → local beans/docs descriptor；no external sockets/data。
- Error and edge behavior: duplicate executor、DTP未发现、OpenAPI丢失、Gateway/JPA/Flyway/external facade residue均失败。
- Implementation pseudocode:

```java
assert exactly one Boot applicationTaskExecutor decorated/registered for DTP and one managed LongIdGenerator
assert Springdoc 2.8.17 exposes documented MVC endpoints while local Proto provider/client beans load in test profile
assert no Gateway, JPA, Flyway/Liquibase, external facade or unapproved top.egon runtime class is present
```

- Verification contribution: Web final runtime RED/GREEN gate。
- After this file: tests fail until Files 2-4 complete wiring。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-starter/pom.xml}`

- Purpose: 完成开源版本矩阵、精确 Egon allowlist、Springdoc 与 ArchUnit dependencies。
- Symbols: Boot 3.5.16、Cloud 2025.0.3、SCA 2025.0.0.0、Nacos 3.0.3、Dubbo 3.3.6、ShardingSphere 5.5.3、MP 3.5.17、Springdoc 2.8.17。
- Repository evidence: Web 复制版本为 Springdoc 2.8.13；Common/DTP 必须使用指定 direct artifacts；Gateway 只允许外部部署。
- Dependencies and consumers: generated reactor、starter、tests、CI。
- Why now: 所有功能依赖已知，可一次完成 convergence/banlist。
- Contract/signature changes: only approved `top.egon` artifacts；Springdoc升级 2.8.17；移除 internal architecture plugin；不引 Gateway。
- Input/output and state mapping: BOM/direct deps → deterministic classpath；no runtime state。
- Error and edge behavior: dependency convergence、duplicate SLF4J/Netty/protobuf、forbidden transitive依赖均阻断；禁止靠 broad exclusions 隐藏问题。
- Implementation pseudocode:

```xml
pin the approved open-source matrix and Springdoc 2.8.17 under central dependency management
allow only common-id/common-core/common-context/common-dto/common-enum/common-exception plus dtp component artifacts
add ArchUnit test support and ban Gateway, JPA, Flyway/Liquibase, external facades and internal architecture plugin
```

- Verification contribution: dependency tree/convergence/forbidden gate。
- After this file: Web classpath满足最终 allowlist。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/async/AsyncConfiguration.java`

- Purpose: 将 Web 现有 async hook 接到唯一 Boot executor 与 DTP decorator。
- Symbols: `AsyncConfigurer#getAsyncExecutor`、`applicationTaskExecutor`、`DtpTaskDecorator`、exception handler。
- Repository evidence: copied class 当前返回 null；DTP component 自动发现 `ThreadPoolTaskExecutor`。
- Dependencies and consumers: Common DTP auto-config、Spring async calls、File 4 bounds/config。
- Why now: dependency graph完成，可 wiring bean。
- Contract/signature changes: async calls显式使用唯一 managed executor；不新建 DTP Admin/service。
- Input/output and state mapping: Runnable + context → decorated bounded executor → async result/error；pool由 Boot创建。
- Error and edge behavior: missing/duplicate executor fail context；rejection policy显式；上下文清理防线程复用泄漏。
- Implementation pseudocode:

```java
publish one DtpTaskDecorator bean and inject Boot applicationTaskExecutor by qualifier
return that executor from AsyncConfigurer.getAsyncExecutor and retain current exception handler
rely on DTP auto-discovery for governance; never create a second raw pool or Admin service
```

- Verification contribution: async/DTP context tests。
- After this file: executor wiring exists；bounds/profile config await File 4。

#### File 4 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-starter/src/main/resources/application*.yml,deploy/env/.env*.example,deploy/compose/*.yaml}`

- Purpose: 配置机器号、bounded pool、DTP Redis/report、Nacos 3.0.3、Springdoc 和测试隔离。
- Symbols: `EGON_ID_MACHINE_ID`；8/32/1000/60s defaults；DTP config；Nacos image/client；OpenAPI paths；Triple/gRPC target。
- Repository evidence: copied configs无机器号/DTP且 Nacos 旧；component README 给出池默认样例；Web需Springdoc但不需Gateway。
- Dependencies and consumers: ID/DTP/Nacos/Dubbo/Springdoc/runtime operator/tests。
- Why now: bean graph exists，配置关闭 runtime GREEN。
- Contract/signature changes: 无生产机器号默认；dev/prod DTP enabled、test disabled；Springdoc docs endpoint稳定；无 Gateway config。
- Input/output and state mapping: env → generator/pool/Redis/Nacos/RPC/docs；no secrets committed。
- Error and edge behavior: prod missing machine/Redis/Nacos fail-fast；tests no sockets；pool saturation/port collision/deadline可配置。
- Implementation pseudocode:

```yaml
require EGON_ID_MACHINE_ID and define env-overridable 8/32/1000/60s task-execution bounds
enable DTP Redis/report in dev/prod, disable external integrations in test and set compose Nacos to 3.0.3
configure Springdoc 2.8.17 and Triple/gRPC properties while asserting no spring.cloud.gateway namespace
```

- Verification contribution: context/OpenAPI/config/compose gates。
- After this file: Web runtime config final。

#### File 5 — `CREATE egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/OpenArchitectureTest.java`

- Purpose: 证明七模块依赖方向、domain-first package rule、Web adapter边界与 forbidden technologies。
- Symbols: Facade leaf；Adapter/Application/Domain/Infrastructure/Starter rules；controller/GraphQL仅Adapter；no Gateway/JPA/Flyway/internal runtime。
- Repository evidence: Web living architecture §§1-5 与 domain-first Spec；内部 plugin已移除。
- Dependencies and consumers: compiled modules visible from starter test classpath；ArchUnit。
- Why now: 最终代码/协议/持久化/runtime graph已稳定。
- Contract/signature changes: test-only architecture governance。
- Input/output and state mapping: compiled dependency graph → exact violations；no runtime state。
- Error and edge behavior: cycles、Proto/Mapper in Domain、controller outside Adapter、Gateway/unapproved Egon dependency均失败。
- Implementation pseudocode:

```java
assert facade is a leaf and Domain depends only on allowed Java/common/validation APIs
assert controllers/graphql stay in Adapter, persistence/RPC clients stay in Infrastructure and Starter only composes
ban JPA, Flyway/Liquibase, Gateway, UUID generators, external facades and internal Egon architecture runtime
```

- Verification contribution: generated Web architecture gate。
- After this file: Web架构无需内部 bytecode plugin。

#### File 6 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/{multi-project-multi-module-architecture.md,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`

- Purpose: 同步七模块、本地 Proto、Long/MP/manual SQL、DTP、Springdoc、外部 Gateway 与操作合同。
- Symbols: module tree/dependency graph、10 provider/3 client、manual DBA order、env、test commands/proof limits。
- Repository evidence: copied docs仍描述 external facades、UUID/JPA/Flyway/六模块和旧版本。
- Dependencies and consumers: generated developers/operators/DBA；Step 14 root docs。
- Why now: 最终实现事实已确定。
- Contract/signature changes: documentation only；Web保持 MVC/GraphQL；Gateway明确为外部基础设施。
- Input/output and state mapping: operation/config → expected behavior；no data mutation。
- Error and edge behavior: missing schema/machine/Redis/Nacos/RPC deadline、DTP saturation与manual rollback均文档化。
- Implementation pseudocode:

```markdown
document seven modules with local Proto facade, organization Triple providers and evaluation standard-gRPC client
replace UUID/JPA/Flyway guidance with Long/MP/manual SQL and add machine-id/DTP/Nacos/Springdoc runbooks
state Gateway is external, generated Web contains no Gateway component, and static tests do not prove live topology
```

- Verification contribution: docs/token/structure gate。
- After this file: Web Open code/config/docs一致。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -pl :egon-cola-archetype-web-open -am clean integration-test && ! rg -n 'spring-cloud-starter-gateway|spring\.cloud\.gateway|spring-boot-starter-data-jpa|flyway|liquibase|UuidV7Generator|UUID\.randomUUID|egon-cola-architecture' egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources --glob '!README*' --glob '!deploy/sql/README.md'`
- Expected result: exit 0；七模块、OpenAPI、DTP、Proto、MP、ArchUnit tests通过；forbidden runtime/config无命中。
- Failure returns to: File 1 runtime contract，File 2 classpath，File 3 executor，File 4 config，File 5 architecture，File 6 docs。
- Completion criteria: Web Open 满足最终 runtime/API/architecture contracts，且不包含 Gateway/JPA/Flyway/UUID/external facade。
- Rollback: 回退 Web Open Step 12 paths；no runtime/data state。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/{OrganizationApplicationTest,OrganizationExternalFreeContextTest,OpenApiContractTest}.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{pom.xml,__rootArtifactId__-starter/pom.xml}` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/async/AsyncConfiguration.java` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/{__rootArtifactId__-starter/src/main/resources/application*.yml,deploy/env/.env*.example,deploy/compose/*.yaml}` `egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/architecture/OpenArchitectureTest.java` `egon-cola-archetypes/egon-cola-archetype-web-open/{multi-project-multi-module-architecture.md,src/main/resources/archetype-resources/README.md,src/main/resources/archetype-resources/README.zh-CN.md}`
- Commit: `feat(web-open): finalize open runtime governance`

### Step 13 — 建立三个 Open Archetype 的生成物与跨模板一致性门禁

- Requirements: `REQ-001`,`REQ-002`,`REQ-003`,`REQ-004`,`REQ-005`,`REQ-006`,`REQ-007`,`REQ-008`,`REQ-009`,`REQ-010`,`REQ-011`,`REQ-012`,`REQ-013`,`REQ-014`,`REQ-015`,`REQ-016`,`REQ-017`,`REQ-018`,`REQ-019`,`REQ-021`
- Dependencies: Steps 2-12 all template implementations complete。
- Baseline state: 三个 Open 模板可分别构建，但复制完整性、生成物依赖/目录/禁止项、Service/Web Proto 字节一致性尚未由 archetype integration verifiers 统一证明。
- Observable outcome: 三个 `verify.groovy` 对实际生成项目进行结构、dependency tree、test、config、SQL、forbidden token和original-family isolation验收；整个 archetypes reactor可在同一命令通过。
- End state: 每个 Open 变体既有模板源门禁，也有真实生成项目门禁；Service/Web Proto 同源合同和 Light/Service/Web产品边界可重复验证。
- Test-first gate: `Required — 先将 verifiers 扩展为新合同并运行各自 archetype integration-test，观察对漏模块/旧依赖/缺 SQL/缺测试的 RED；不得降低断言迁就当前实现。`
- Ordered files: Light verifier → Service verifier → Web verifier → cross-template source checks → full archetypes reactor。

#### File 1 — `MODIFY egon-cola-archetypes/egon-cola-archetype-light-open/src/test/resources/projects/basic/verify.groovy`

- Purpose: 验证 Light Open 生成物是完整原模板副本迁移后的四模块开源变体。
- Symbols: generated tree manifest、Maven tests、Long/MP/manual SQL/DTP/Springdoc/ArchUnit、forbidden checks。
- Repository evidence: copied verifier只覆盖旧 UUID/JPA/Flyway合同，不能发现 Open 回归。
- Dependencies and consumers: Steps 2-4 template files、maven archetype integration-test。
- Why now: Light implementation稳定，可建立最终 generated-project gate。
- Contract/signature changes: test-only；generated Light产品合同更新。
- Input/output and state mapping: generated project path → structural/command/token assertions；no external state。
- Error and edge behavior: missing copied file、extra module、forbidden dependency、auto SQL、Gateway/RPC leak、test失败均明确失败。
- Implementation pseudocode:

```groovy
assert the generated four-module tree matches the temporary copy manifest minus declared migration deletions plus declared additions
run focused Maven tests and inspect dependency tree/config/manual SQL/architecture/OpenAPI/DTP/Long contracts
fail on UUID, JPA, Flyway/Liquibase, Gateway, RPC, external facade or unapproved top.egon artifacts
```

- Verification contribution: Light true-generated-project acceptance。
- After this file: Light Open不再只靠模板源扫描。

#### File 2 — `MODIFY egon-cola-archetypes/egon-cola-archetype-service-open/src/test/resources/projects/basic/verify.groovy`

- Purpose: 验证 Service Open 七模块、5 Proto、11 provider/3 client、manual SQL/DTP/ArchUnit 和 service-only边界。
- Symbols: generated manifest、descriptor 8/21、Triple/standard gRPC tests、forbidden Springdoc/Gateway/JPA/Flyway/external facade。
- Repository evidence: copied verifier只认识六模块和外部 Java facade。
- Dependencies and consumers: Steps 5-8。
- Why now: Service implementation稳定。
- Contract/signature changes: test-only；generated Service contract更新。
- Input/output and state mapping: generated project → module/protocol/runtime/test assertions。
- Error and edge behavior: Proto被过滤、method漏失、Controller/Springdoc/Gateway出现、external dependency解析均失败。
- Implementation pseudocode:

```groovy
assert seven modules, five unfiltered proto files and the exact 8-service/21-method descriptor contract
run provider/standard-gRPC/persistence/DTP/architecture tests and inspect the external-free dependency tree
fail on HTTP controllers, Springdoc, Gateway, UUID, JPA, Flyway/Liquibase or external facade artifacts
```

- Verification contribution: Service true-generated-project acceptance。
- After this file: Service产品边界可重复验证。

#### File 3 — `MODIFY egon-cola-archetypes/egon-cola-archetype-web-open/src/test/resources/projects/basic/verify.groovy`

- Purpose: 验证 Web Open 七模块、本地 Proto、10 provider/3 client、Springdoc、manual SQL/DTP/ArchUnit 和外部 Gateway边界。
- Symbols: generated manifest、descriptor、OpenAPI endpoints、dependency/config/SQL/token gates。
- Repository evidence: copied verifier仍接受 UUID/JPA/Flyway/external facade。
- Dependencies and consumers: Steps 9-12。
- Why now: Web implementation稳定。
- Contract/signature changes: test-only；generated Web contract更新。
- Input/output and state mapping: generated project → structural/runtime/API/protocol assertions。
- Error and edge behavior: missing docs、Gateway module/dependency/config、Proto漂移、forbidden persistence/UUID均失败。
- Implementation pseudocode:

```groovy
assert seven modules, local proto facade, exact provider/client contract and Springdoc 2.8.17 endpoints
run Long/MP/Triple/gRPC/DTP/OpenAPI/architecture tests and inspect manual SQL plus dependency tree
fail on Gateway component/config, UUID, JPA, Flyway/Liquibase, external facade or unapproved top.egon artifacts
```

- Verification contribution: Web true-generated-project acceptance。
- After this file: Web产品边界可重复验证。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `diff -qr egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean verify`
- Expected result: exit 0；三种 archetype integration tests生成并验证真实项目；Service/Web Proto目录无差异；原 family未修改。
- Failure returns to: File 1 Light generated contract，File 2 Service contract，File 3 Web contract，或对应 Steps 2-12实际实现。
- Completion criteria: 三个 Open archetypes均通过真实生成、构建、测试、依赖/配置/SQL/结构验收，且跨模板Proto一致。
- Rollback: 只回退三个 Open verify.groovy；若暴露实现缺陷则返回 owning Step修复，不放宽门禁。
- Commit paths: `egon-cola-archetypes/egon-cola-archetype-light-open/src/test/resources/projects/basic/verify.groovy` `egon-cola-archetypes/egon-cola-archetype-service-open/src/test/resources/projects/basic/verify.groovy` `egon-cola-archetypes/egon-cola-archetype-web-open/src/test/resources/projects/basic/verify.groovy`
- Commit: `test(archetypes-open): verify generated project contracts`

### Step 14 — 接入 CI、发布脚本与根文档并完成最终不可变性审计

- Requirements: `REQ-001`,`REQ-002`,`REQ-003`,`REQ-004`,`REQ-005`,`REQ-006`,`REQ-007`,`REQ-011`,`REQ-015`,`REQ-016`,`REQ-018`,`REQ-019`,`REQ-020`
- Dependencies: Step 13 generated-project/full-reactor gates GREEN。
- Baseline state: Open modules虽可本地验证，但 CI、deploy脚本、根 README/架构图尚未列出；original family不可变性与发布前最终证据尚未集中记录。
- Observable outcome: CI和部署脚本显式覆盖3个Open artifacts；中英文根文档清楚区分 original/internal 与 open variants；两份 Open架构文档可复核技术栈/边界；最终 diff证明original family零改动。
- End state: Plan中的全部实现requirements有源码、测试、操作、CI与发布门禁；仍不自动启动项目、不执行手工SQL、不访问live infra。
- Test-first gate: `Required — 先运行CI脚本静态/目标提取检查，确认它们尚未包含Open modules而RED；更新后再跑Step 13 full reactor和original diff gate。`
- Ordered files: CI → deploy script → root README → Open architecture docs → final full verification/audit。

#### File 1 — `MODIFY .github/workflows/ci_java_compatibility.yaml`

- Purpose: 将三个 Open archetypes加入Java兼容性/生成项目CI矩阵。
- Symbols: module matrix、archetype integration verify command、artifact cache inputs。
- Repository evidence: 当前workflow只覆盖已有components/platforms/original archetypes；新modules若未列入可能在发布前不生成测试。
- Dependencies and consumers: Maven reactor、GitHub Actions、Step 13 verifiers。
- Why now: 本地final gate稳定后接CI，避免中间态持续破坏主流程。
- Contract/signature changes: CI新增3个 isolated module targets和full reactor聚合gate；不替换原任务。
- Input/output and state mapping: checkout/JDK/Maven inputs → build reports；no deployment state。
- Error and edge behavior: 每个variant失败可定位；cache key包含POM变化；网络依赖失败与合同失败分开显示。
- Implementation pseudocode:

```yaml
add light-open, service-open and web-open to the existing archetype compatibility matrix
run each archetype integration verifier and retain the aggregate egon-cola-archetypes clean verify gate
preserve original component/platform/archetype jobs and upload useful failure reports without starting apps
```

- Verification contribution: PR/release持续门禁。
- After this file: CI覆盖Open family。

#### File 2 — `MODIFY scripts/maven-deploy.sh`

- Purpose: 将三个 Open artifacts 纳入现有受控 Maven部署选择/顺序。
- Symbols: module allowlist/order、dry-run/list模式、failure propagation。
- Repository evidence: deploy脚本当前不了解新artifact；parent需先于三个Open archetypes发布。
- Dependencies and consumers: Maven repository operator、root/archetypes parent reactor。
- Why now: artifacts已通过full verification，才能加入发布面。
- Contract/signature changes: 新增三个可选/聚合deploy target；不默认启动或部署生成应用。
- Input/output and state mapping: explicit target/version/repository → Maven deploy artifacts；no DB/runtime changes。
- Error and edge behavior: 未知target拒绝；任一deploy失败停止；禁止重复/隐式发布original替代品；保留dry-run。
- Implementation pseudocode:

```bash
add the three open artifactIds to the validated deploy allowlist after their parent
preserve explicit selection, dry-run, version and repository checks plus immediate failure propagation
do not deploy generated sample apps, execute SQL or start any service
```

- Verification contribution: shell syntax/target listing/dry-run gate。
- After this file: Open artifacts可按现有流程发布。

#### File 3 — `MODIFY {README.md,README.zh-CN.md}`

- Purpose: 向使用者展示 original 与 `-open` 两族并明确选择规则。
- Symbols: archetype catalog、technology matrix、constraints、generation/verification links、manual SQL warning。
- Repository evidence: 当前根文档只列 original family，用户要求名称在原基础追加 `-open`。
- Dependencies and consumers: developers/operators/release notes、Files 4-5 architecture docs。
- Why now: implementation/CI/release路径稳定后更新公共入口。
- Contract/signature changes: docs only；original artifacts继续保留且不被重定向。
- Input/output and state mapping: archetype choice → expected stack/modules/runtime prerequisites；no runtime state。
- Error and edge behavior: 清楚说明Service无HTTP docs、Gateway外置、DB只手工SQL、machine ID/infra由operator提供。
- Implementation pseudocode:

```markdown
list original and open archetypes side by side without implying deprecation or replacement
summarize Boot/Cloud/SCA/Dubbo/Nacos/Proto/MP/Sharding/DTP/Springdoc product boundaries
link generation, manual SQL and architecture docs and state that templates/tests do not prove live topology
```

- Verification contribution: public documentation token/link gate。
- After this file:用户可正确选择Open variant。

#### File 4 — `CREATE egon-cola-archetypes/open-source-archetype-family-architecture.md`

- Purpose: 提供三种Open产品、共享/独有依赖、模块方向、协议与运行基础设施的仓库级架构总览。
- Symbols: Light 4 modules；Service/Web 7 modules；local facade；Triple/gRPC；external Gateway；manual SQL；DTP/ID。
- Repository evidence: primary Spec与三个living docs分散描述，需要一个实现后公共视图但不得替代Spec。
- Dependencies and consumers: root README、维护者、future Plans。
- Why now: 所有最终路径与依赖可从实现验证。
- Contract/signature changes: docs only；明确non-normative implementation overview并回链Spec。
- Input/output and state mapping: component/module → consumer/runtime dependency diagrams；no state。
- Error and edge behavior: 标出test proof与live Nacos/Redis/DB/broker/Gateway proof边界。
- Implementation pseudocode:

```markdown
draw the three module graphs and one runtime context using Mermaid with Facade as leaf and Gateway external
record exact approved stack/egon allowlist, ID/route/SQL contracts and provider/client ownership
link each generated runbook and distinguish static/generated tests from live infrastructure validation
```

- Verification contribution: architecture review/link gate。
- After this file: open family总架构可独立理解。

#### File 5 — `CREATE egon-cola-archetypes/open-source-archetype-code-style.md`

- Purpose: 固化Open模板的domain-first package、Adapter/Application/Domain/Infrastructure/Starter/Facade责任与禁止实践。
- Symbols: package examples、Long/Proto/HTTP mappings、Mapper XML rules、async/DTP、test pyramid、manual SQL规范。
- Repository evidence: effective domain-first Spec与living docs规定结构，Open overrides需汇总为实现维护指南。
- Dependencies and consumers: contributors、ArchUnit/verifier维护、root README。
- Why now: final code pattern已经可从三模板提炼，避免提前发明新风格。
- Contract/signature changes: docs only；不新增架构层或design pattern。
- Input/output and state mapping: change type → owning module/package/test；no state。
- Error and edge behavior: 明确禁止UUID/JPA/Flyway/Gateway内置、跨层依赖、动态表名、自动schema更新和额外RPC调用。
- Implementation pseudocode:

```markdown
document repository-native module/package ownership and examples from all three generated projects
define boundary mappings, mapper safety, DTP executor, Proto evolution and manual SQL review rules
map every forbidden practice to its ArchUnit, verifier or dependency-tree enforcement point
```

- Verification contribution: contributor/architecture consistency gate。
- After this file: Open family维护约束完整。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `bash -n scripts/maven-deploy.sh && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean verify && git diff --exit-code -- egon-cola-archetypes/egon-cola-archetype-light egon-cola-archetypes/egon-cola-archetype-service egon-cola-archetypes/egon-cola-archetype-web && git diff --check`
- Expected result: exit 0；CI/deploy/static docs检查通过；full archetypes reactor GREEN；三个original directories零diff；无whitespace errors。
- Failure returns to: File 1 CI target，File 2 deploy behavior，File 3 catalog，Files 4-5 docs，或Step 13/owning implementation Step。
- Completion criteria: 新Open family可被CI、发布与公共文档发现；所有原模板保持字节级未修改；没有项目被启动、没有SQL被执行。
- Rollback: 分别回退CI/deploy/docs paths；已发布artifact不得删除或覆盖，应提升下一版本forward-fix。
- Commit paths: `.github/workflows/ci_java_compatibility.yaml` `scripts/maven-deploy.sh` `{README.md,README.zh-CN.md}` `egon-cola-archetypes/open-source-archetype-family-architecture.md` `egon-cola-archetypes/open-source-archetype-code-style.md`
- Commit: `docs(archetypes-open): publish open family workflow`

## 8. Test, Validation, and Quality Gates

### 8.1 Step-level validation order

1. Steps 1-4：先验证 Light copy manifest、Long/sharding、MP/manual SQL、DTP/Springdoc/ArchUnit；每步只运行 `:egon-cola-archetype-light-open -am` 对应 test/integration-test。
2. Steps 5-8：验证 Service Long、5 Mapper、local Proto/Triple、DTP/ArchUnit；Service 始终不得出现 Controller、Springdoc 或 Gateway。
3. Steps 9-12：验证 Web Long、8 Mapper、local Proto/Triple、DTP/Springdoc/ArchUnit；HTTP/GraphQL ID继续为十进制String。
4. Step 13：运行三个实际生成项目 verifier，然后执行整个 `egon-cola-archetypes` reactor `clean verify`。
5. Step 14：重复 full reactor，检查CI/deploy脚本与original family零diff；只产生构建/测试证据，不启动应用、不执行SQL。

### 8.2 Required static gates

```bash
# dependency/runtime bans; commands are run per generated Open project
./mvnw -B -ntp dependency:tree
rg -n 'JpaRepository|jakarta\.persistence|spring-boot-starter-data-jpa|flyway|liquibase|UuidV7Generator|UUID\.randomUUID|spring-cloud-starter-gateway|spring\.cloud\.gateway|egon-cola-architecture' .

# protocol equality and complete descriptors
diff -qr \
  egon-cola-archetypes/egon-cola-archetype-service-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto \
  egon-cola-archetypes/egon-cola-archetype-web-open/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/proto

# original family immutability
git diff --exit-code -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
```

The forbidden-token scan is an assertion of **zero hits in runtime source/config/POM**；README/runbook中为说明禁用项而出现的字样应以精确exclude处理，不能把全局`rg`的exit 1误判为命令失败。Dependency tree仍是对transitive artifact的最终证据。

### 8.3 Required generated-project tests

| Product | Required proof |
|---|---|
| Light Open | 四模块、Long/route、8 Mapper、manual SQL、DTP、Springdoc、ArchUnit、no RPC/Gateway |
| Service Open | 七模块、5 Mapper、5 Proto、descriptor 8/21、11 provider、3 standard-gRPC client、DTP、no HTTP docs/Gateway |
| Web Open | 七模块、8 Mapper、5 Proto、descriptor 8/21、10 provider、3 standard-gRPC client、DTP、Springdoc、no Gateway component |
| Cross-template | Service/Web Proto byte-identical；only approved `top.egon` artifacts；original family zero diff |

### 8.4 Live-system gaps that remain after implementation

- Tests do not prove Nacos 3.0.3 live registration/discovery、Dubbo Triple跨进程互操作、标准gRPC client经真实LB访问、Redis-backed DTP reporting、Rabbit/Kafka delivery or MySQL ShardingSphere topology。
- Manual SQL is reviewed as text and may be parsed in disposable test schemas, but no production/target database is changed by build or application startup。
- `EGON_ID_MACHINE_ID` uniqueness is an operator/部署编排责任；unit/context tests只能证明缺失配置fail-fast与固定machine ID生成规则。
- External Spring Cloud Gateway deployment/configuration is intentionally out of these archetypes；generated项目只能证明自身不含Gateway。

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Delivery order

1. 先发布/可解析 primary Spec允许的Common ID与DTP组件版本；不得将仍迭代中的其他components/platforms作为Open模板runtime依赖。
2. 发布包含新modules的`egon-cola-archetypes` parent，再按Light Open → Service Open → Web Open发布三个artifact。
3. 用户以新artifactId生成**新项目**；本Plan不就地迁移由original模板生成的既有系统。
4. DBA按每个生成项目`deploy/sql/README.md`先备份、再逐节点执行shard SQL、最后执行master/seed SQL并核对schema/index/count。
5. Operator配置唯一machine ID、datasource、Redis、Nacos、broker、Triple/gRPC target；Gateway在外部平台独立配置。
6. 先做单服务health/context，再做Service↔Web协议互操作和API smoke；最后才接真实流量。

### 9.2 Database execution contract

- 六份SQL是`Light master/shard`、`Service master/shard`、`Web master/shard`；均位于generated项目`deploy/sql/`，不放入`classpath:db`或自动初始化目录。
- 所有业务/关系ID与route key使用`BIGINT`；Service schedule按`course_id`、exam按`id`、paper/score按`exam_id`；Web class/membership按`grade_id`。
- Seed只允许保留ID 1/2的文档化系统数据；Snowflake运行ID不得占用该范围。
- 本次交付不含从UUID schema到Long schema的数据转换脚本，因为Open archetypes是新artifact/new-project产品；若未来要求迁移既有数据库，必须另立Spec/Plan。

### 9.3 Compatibility matrix

| Boundary | Compatibility decision |
|---|---|
| Original archetype artifactIds | 保持存在且目录零改动；不redirect、不deprecate |
| Open artifactIds | 新增`-open` sibling；与original并行发布 |
| Java/domain ID | Breaking inside new generated Open projects: canonical Long only |
| HTTP/GraphQL ID | Decimal String，以避免JS精度问题 |
| Proto ID | `int64`；field numbers/reservations由descriptor test锁定 |
| External Java facade artifacts | Open Service/Web不解析、不兼容；local Proto是唯一wire contract |
| Database | New-project manual BIGINT schema；无自动migration或legacy UUID双读 |
| Gateway | External deployment compatibility only；不成为generated module/dependency/config |

### 9.4 Rollback strategy

- 发布前：按每Step commit逆序回退对应Open paths；copy manifest和original zero-diff gate保证不会伤及原模板。
- artifact已发布：不得覆盖/删除同版本；修复后提升新版本并在release notes说明。使用者可继续选择original或上一Open版本。
- DBA已执行SQL：先停止新应用流量，按runbook使用执行前备份恢复；不由应用自动drop/repair。若已有业务数据，优先forward-fix schema而非破坏性回退。
- 注册中心/线程池问题：撤下对应Open实例并恢复上一artifact/config；Nacos/DTP/Gateway外部状态不由模板脚本删除。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Implementation Steps | Primary files/gates |
|---|---|---|
| REQ-001 | 1-4, 13-14 | Light Open coordinate、template、generated verifier、catalog/release gate |
| REQ-002 | 1, 5-8, 13-14 | Service Open coordinate、seven modules、generated verifier、catalog/release gate |
| REQ-003 | 1, 9-14 | Web Open coordinate、seven modules、generated verifier、catalog/release gate |
| REQ-004 | 1, 13-14 | copy isolation、path-limited commits、original/components/platforms zero-diff |
| REQ-005 | 2, 4-5, 7-9, 11-14 | living-architecture reuse、ArchUnit、module/package/generated gates |
| REQ-006 | 1, 4, 8, 12-14 | Java/Boot/wrapper exact versions in POM、generated projects、CI |
| REQ-007 | 4, 8, 12-14 | Cloud/SCA/Nacos versions、profiles/compose、offline context tests |
| REQ-008 | 3, 6, 10, 13 | 8/5/8 Mapper replacements、official MP tree、zero JPA/Hibernate |
| REQ-009 | 2-3, 5-6, 9-10, 13 | Long sharding algorithms、route-key YAML、SQL/index/topology tests |
| REQ-010 | 3, 6, 10, 13 | no Flyway/Liquibase/runtime migrator/SQL auto-init gates |
| REQ-011 | 3, 6, 10, 13-14 | six PostgreSQL scripts、three runbooks、explicit-only execution proof |
| REQ-012 | 2-13 | exact Common/DTP allowlist、approved BOMs、dependency convergence/tree |
| REQ-013 | 4, 8, 12-13 | one bounded Boot executor、DtpTaskDecorator、profile/context tests |
| REQ-014 | 7, 11, 13 | five Proto sources、8 services/21 methods、Triple/standard-gRPC tests |
| REQ-015 | 4, 8, 12-14 | external Gateway negative gates、Light/Web Springdoc、Service no Springdoc |
| REQ-016 | 3-4, 6-8, 10-14 | direct-consumer audit、forbidden dependencies/tokens、ArchUnit/verifiers |
| REQ-017 | 2-13 | behavior fixtures、Long internal model、HTTP decimal String、Proto int64、MQ/API parity |
| REQ-018 | 1, 4, 7-8, 11-14 | independent generation/publishability、reactor、CI、deploy/catalog docs |
| REQ-019 | 3-4, 6-8, 10-14 | test profiles/in-process RPC/explicit schema helper、no live infra sockets |
| REQ-020 | 14 | commands limited to compile/test/package/static；no app start or SQL execution |
| REQ-021 | 2-13 | Common Snowflake Long、machine ID、BIGINT/int64/decimal mapping、zero UUID |

## 11. Risks, Blockers, and User Decisions

### 11.1 Risk register

| Risk / decision | Status | Mitigation / owner |
|---|---|---|
| Target Spec is `Review`, not `Ready` | Review gate | User must approve Spec and this Plan before `egon-coding-executing-plan`; no source change is authorized now |
| Actual JPA count is 21 vs Spec inventory text 19 | Clarified | PLAN-CLAR-001 migrates all Light 8 + Service 5 + Web 8; no repository is omitted |
| 1,241-file copy may silently omit hidden/binary/template files | Mitigated | PLAN-CLAR-003 uses `git ls-files` manifests and generated verifier parity, not hand-maintained filename guesses |
| Proto codegen compatibility/version drift | External gate | Pin Dubbo/Protobuf/gRPC versions, use official Dubbo Maven generation lifecycle, descriptor tests and standard-gRPC interop test |
| Service/Web Proto drift after independent edits | Mitigated | Byte-identical source directories + descriptor tests + Step 13 `diff -qr` |
| Snowflake machine ID collision | Operator gate | No prod default, startup validation, deployment inventory/runbook; live uniqueness remains external proof |
| Sharding route/schema mismatch | Mitigated | Exact formula tests, route-key YAML, manual SQL indexes, repository integration and DBA verification queries |
| DTP creates duplicate/unbounded executors | Mitigated | Reuse Boot `applicationTaskExecutor`, one decorator, explicit bounds, bean-count/context tests |
| Manual SQL executed inconsistently across nodes | Operator gate | Per-product runbook, backup/order/schema/index/count checklist; application never auto-runs SQL |
| Live Nacos/Redis/DB/broker/Gateway topology unavailable in CI | External gate | Clearly separate static/generated proof from deployment smoke and operator acceptance |
| Accidental edits to original templates/concurrent dirty work | Mitigated | Path-limited commits, original zero-diff gate, no reset/checkout, preserve unrelated worktree changes |

### 11.2 Design-pattern decision

- **Adapter/Ports-and-Adapters（沿用，不新增层）**：Proto provider、standard gRPC client、MyBatis-Plus repository adapter都位于既有Adapter/Infrastructure边界，解决协议/持久化框架变化而不污染Domain。这是现有COLA/domain-first结构的直接延续。
- **Repository pattern（沿用）**：Domain repository ports保持不变，只将JPA adapter替换为MP Mapper adapter，最小化业务行为变化并支持可替换/可测试的持久化实现。
- **Facade module as contract leaf（沿用模块模式）**：local facade只承载Proto/generated types，解决Service/Web独立生成与wire contract所有权问题；禁止Facade反向依赖业务模块。
- **未引入Strategy/Factory/Template Method等新抽象**：三个产品的分片公式、错误映射和provider数量固定，直接的算法类/mapper/provider mapping更清楚；为“可能变化”预建抽象会增加模板复杂度。若未来出现多分片策略或多协议provider，需另立需求再评估Strategy/Adapter扩展。

## 12. Review and Acceptance

### 12.1 Completion audit for this Plan

- [x] 每个 REQ-001..REQ-021 映射到至少一个有序Step、具体文件组与验证门禁。
- [x] 每个Step具备前置依赖、baseline、observable/end state、test-first gate、逐文件operation/pseudocode、验证、失败返回、回滚和独立commit scope。
- [x] Light/Service/Web分别按4/7/7模块设计；Service/Web Proto、provider/client ownership、API/Gateway边界明确。
- [x] Long/MP/Sharding/manual SQL/Common ID/DTP/Springdoc/Nacos/Dubbo/ArchUnit与forbidden dependencies均有源代码、配置、测试和generated-project gate。
- [x] Original family不可变、dirty worktree保护、no app start、no SQL execution、no live-proof overclaim均显式纳入。
- [x] Plan只描述未来实现；本次未改源码/测试/SQL/CI，未执行Maven实现验证，未创建commit。

### 12.2 Review verdict

**PASS — Ready for user review.**

This Plan is implementation-ready in file/dependency/validation order, but it is intentionally **not `Ready` for execution** while the target Spec and this Plan remain `Review`. The next permitted action is user review/approval；approval后才可调用`egon-coding-executing-plan`按Step 1→14逐步实施、逐Step验证并分别commit。
