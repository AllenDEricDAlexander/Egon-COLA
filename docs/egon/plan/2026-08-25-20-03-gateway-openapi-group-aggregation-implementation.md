# Gateway OpenAPI 3.1 多 Group 聚合与来源扩展实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-25-20-03-gateway-openapi-group-aggregation-implementation.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-08-25 20:03 CST` |
| Updated | `2026-08-25 20:03 CST` |
| Owner | User / Egon-COLA Gateway maintainers |
| Repository | Egon-COLA |
| Scope | `egon-cola-xingyuan/egon-cola-yuheng`：Provider Springdoc 多 Group、DDC Manifest、Admin 安全同步与聚合、Report v2/Ingestion、Catalog/MCP 投影、Admin Web |
| Source Requirement | 用户确认全部推荐方案，并要求不同业务系统配置不同 OpenAPI Group、Gateway 聚合、HTTP/RPC 来源可扩展及 MCP 可选扩展；允许破坏性更新和 fresh DB 重建 |
| Baseline Revision | `main@3be897e5cb4781890bfbac3104512e6e73bb943a`；dirty worktree，现有 `docs/egon` 用户改动不属于实施提交 |
| Implements Spec | [Gateway HTTP OpenAPI 3.1 单一事实源破坏性改造规格](../spec/2026-08-25-19-01-gateway-openapi31-source-refactor.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-08-25 19:43 CST`；增补方案由用户在当前会话明确确认 |
| Effective Specs | [Gateway HTTP OpenAPI 3.1 单一事实源破坏性改造规格](../spec/2026-08-25-19-01-gateway-openapi31-source-refactor.md)；[Gateway 多 OpenAPI Group 聚合与来源扩展增补规格](../spec/2026-08-25-19-43-gateway-openapi-group-aggregation-amendment.md)；[Gateway 声明式 Operation Schema 与 MCP 参数装配设计](../../superpowers/specs/2026-08-07-gateway-declarative-operation-schema-design.md)；[Gateway 注解托管 MCP 设计](../../superpowers/specs/2026-08-06-gateway-annotation-managed-mcp-design.md)；[GWS-10 Gateway Starter 接口定义上报 Spec](../../superpowers/specs/2026-07-25-gateway-starter-interface-reporting-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本 Plan 把已接受的 HTTP OpenAPI 3.1 单一事实源规格及已确认的多 Group 增补，拆为 14 个顺序实施 Step。依赖方向固定为：Maven/Contract → Provider OpenAPI common → MVC/WebFlux adapter → repository-local Provider/Admin 契约迁移 → RPC-only Starter 清理 → V12 持久化 → 安全获取/校验 → OpenAPI Adapter → 共享 Ingestion/聚合 → 调度生命周期 → 查询 API → Admin Web → 全链路回归。每个 Step 先写聚焦 RED，再做最小 GREEN，执行路径限定验证并只提出一个语义提交；本 Plan 不实施代码、不执行迁移、不启动项目。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Gateway HTTP OpenAPI 3.1 单一事实源破坏性改造规格](../spec/2026-08-25-19-01-gateway-openapi31-source-refactor.md)
- Status: `Accepted`
- Revision: `Updated 2026-08-25 19:43 CST`，baseline `main@3be897e5cb4781890bfbac3104512e6e73bb943a`
- Approval evidence: 用户已选择 `1A 2B 3A 4A 5A 6A`，明确 fresh DB/破坏性切换；随后对六项阻塞选择“全部A”，补充多业务系统多 Group、Gateway 聚合、HTTP/RPC 来源扩展和 MCP 可选扩展，并在本轮明确“确认”生成 Plan。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [OpenAPI 3.1 source refactor](../spec/2026-08-25-19-01-gateway-openapi31-source-refactor.md) | Accepted；2026-08-25 19:43 CST | 未被增补修改的全部内容 | 定义破坏性切换、Springdoc 2.8.17、OAuth2/SSRF、永久 Raw、V12、Admin API/UI 与边界 |
| Amendment | [Multi-Group/source amendment](../spec/2026-08-25-19-43-gateway-openapi-group-aggregation-amendment.md) | 文档为 Review；用户于本轮明确确认 | §4、§7–§17 及对 primary 的明确修正 | 将 default 文档修正为 1–16 个显式 Group、完整集原子聚合、三值来源与 MCP 可选扩展 |
| Normative dependency | [Declarative Operation Schema](../../superpowers/specs/2026-08-07-gateway-declarative-operation-schema-design.md) | Legacy accepted design | §9、§10.1、§11–§12 | 保留 RPC Protobuf、Invocation Schema v2 与 MCP Runtime 契约 |
| Normative dependency | [Annotation-managed MCP](../../superpowers/specs/2026-08-06-gateway-annotation-managed-mcp-design.md) | Legacy accepted design | §2–§5 | 保留 Managed Tool、稳定 Tool ID 与控制面投影边界 |
| Residual predecessor | [GWS-10 reporting](../../superpowers/specs/2026-07-25-gateway-starter-interface-reporting-design.md) | Legacy accepted design | 除被 primary `Supersedes` 明确替换的 HTTP 注解/扫描/上报段落外，其 HMAC、RPC reporting、幂等与生命周期部分 | 约束 RPC signed Report 路径和不可变 build 语义 |

### 2.3 Superseded or excluded content

- 排除 2026-08-07 Spec 的自研 HTTP Java Schema Compiler、HTTP `GatewayOperation`/Schema Annotation 与对应验收；由 primary 的 Springdoc/OpenAPI 3.1 设计替代。
- 排除 2026-07-25 Spec 的 HTTP Starter 扫描和主动 Report；HTTP 改由 DDC Manifest + Admin 拉取，RPC HMAC Report 保留。
- 排除 primary 中单 `default` Group、snapshot `definition_set_id` 唯一和逐 Group 独立 ingest；由 amendment 的完整 Manifest、多 snapshot→一 Definition Set、聚合事务替代。
- 排除 Swagger UI、任意 URL/Group 输入、OpenAPI 自动生产 Route、RPC 伪装为 HTTP paths、动态插件加载器、历史 backfill 和 V1–V11 修改。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | primary §4 | MVC/WebFlux Provider 由 Springdoc 2.8.17 输出 OpenAPI 3.1 | 每个发布 Group 的 `/v3/api-docs/{group}` 返回 `openapi=3.1.x` | 三个 adapter 模块、Provider Golden |
| `REQ-002` | primary §4；amendment §7.3 | 只用标准 OpenAPI + versioned `x-egon`/`x-egon-service` | 旧 HTTP Schema Annotation 为零，扩展字段完整 | annotations/customizers/HTTP 迁移 |
| `REQ-003` | primary §4 | MVC、WebFlux、RPC-only 依赖隔离 | dependency tree 无跨栈或 RPC→Springdoc 泄漏 | POM/classpath tests |
| `REQ-004` | primary §4；amendment §7.3.2 | DDC 只发布有界定位/Manifest，不含文档或 Secret | 精确 8 个 metadata key，group CSV 有界 | registration contributor |
| `REQ-005` | primary §4；amendment §15.2 | Admin 只从健康 DDC Provider 安全拉取 | OAuth2 scope、HTTPS、双 DNS/CIDR、no redirect/ref | client/security tests |
| `REQ-006` | primary §4 | Raw OpenAPI 永久保存并生成可复现 hash | byte SHA/canonical SHA/Raw 查询与重试幂等 | V12/snapshot/query |
| `REQ-007` | primary §4；amendment §7.3.7 | 同 build 漂移 fail closed | INCONSISTENT_BUILD，旧 VALID Set 不覆盖 | coordinator/repositories |
| `REQ-008` | primary §4；amendment §7.3.5 | OpenAPI 映射既有 Report v2/Invocation Schema | operationKey、请求位置、响应/错误 schema 完整 | adapters/converter/ingestion |
| `REQ-009` | primary §4 | 删除自研 HTTP Compiler 与旧 HTTP 注解用法 | production/test residual scan 为零 | Starter cleanup/migration |
| `REQ-010` | primary §4；amendment §4 | RPC/MCP/Route/Release/Engine 既有语义保持 | Descriptor Golden、Tool ID、route/release/engine 回归 | RPC-only Starter/aggregate ingest |
| `REQ-011` | primary §4；amendment §15 | 同步有界、可恢复、可观测 | CAS、最多3实例、backoff/jitter、状态/指标/审计 | scheduler/sync/lifecycle |
| `REQ-012` | primary §4；amendment §12 | Admin Web 展示应用 worst/aggregate、Group 明细和 OpenAPI | 查看/复制/下载/刷新及错误态可见 | APIs/React/E2E |
| `REQ-013` | primary §4 | 只新增 V12，fresh DB 无 backfill | V1–V11 checksum 不变，V12 两表/约束/索引 | one migration/test |
| `REQ-014` | primary §4 | OpenAPI 不自动生成生产 Route | Catalog/MCP Definition 更新，Draft/Release 仍显式 | boundary regression |
| `REQ-015` | primary §4 | 保持 gateway-admin feature-first traditional layering | 新代码只进 `openapi/{controller,service,repository,domain,client,validation,converter}` | architecture test |
| `REQ-016` | amendment §4 | Provider 显式发布 1–16 个标准 Springdoc Group | 不同业务 app 可配置不同 group，未发布的不进 DDC | properties/provider tests |
| `REQ-017` | amendment §4 | DDC 发布排序唯一且总长≤512的 Group Manifest | path template 固定，8 key 精确 | Contract DTO/registration |
| `REQ-018` | amendment §4 | Admin 全 Group VALID 后生成一个 HTTP Definition Set | 所有 snapshot/sync 指向同一 set | aggregate transaction |
| `REQ-019` | amendment §4 | Manifest/hash/operation 重复漂移阻断全 build | 无半套可见；旧 set 保留 | coordinator/drift tests |
| `REQ-020` | amendment §4 | 来源枚举仅 MANUAL/RPC_DESCRIPTOR/OPENAPI31 | protocol/source 配对校验，HTTP/RPC 均经 Ingestion | contract/reporting/frontend |
| `REQ-021` | amendment §4 | MCP 为可选 `x-egon.mcp` 扩展 | 缺失/disabled 仍入 Catalog，enabled 才投影 Tool | validator/MCP release |
| `REQ-022` | amendment §4 | 三个 Egon Annotation、operationId、x-egon-service 契约固定 | operationId 全局唯一，group/build 交叉校验 | common adapter/Golden |
| `REQ-023` | amendment §4 | canonicalization 与响应选择确定 | server/集合排序稳定；200→最低2xx→default；媒体冲突拒绝 | adapter tests |
| `REQ-024` | amendment §4 | Definition+全部 snapshot link 原子，sync VALID 逐行 CAS | 重启修复不重复 Definition | ingestion/recovery tests |
| `REQ-025` | amendment §4 | 运行默认和 SSRF SLO 固定 | 30s/50/3/2m/3s/10s/backoff/jitter/5m 与双校验 | properties/client/scheduler |
| `REQ-026` | amendment §4 | UI 展示聚合状态、逐 Group 状态/hash/error/source | Applications 展开明细，Operation 显示 group | query API/React tests |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先锁定依赖图与共享 Contract，使后续模块能编译同一来源/Manifest；随后创建 Springdoc common 及两个 stack adapter，再迁移所有仓内 HTTP consumers，最后删除旧 Compiler。数据面先用唯一 V12 和 repository 建立持久边界，再分别实现网络安全、Validation Chain、OpenAPI Adapter、共享 Ingestion/完整集 Coordinator；调度只编排这些已验证组件。查询 API 在状态稳定后开放，前端最后消费。全链路回归只验证已落地的边界，不启动本地拓扑。

### 4.2 Test-first strategy

| Behavior | RED | GREEN | Refactor/wiring |
| --- | --- | --- | --- |
| 来源/Manifest | enum/DTO contract test 缺类型或校验失败 | Contract records/enum | Jackson/Validation boundary |
| Provider docs/group | customizer/adapter contract test 缺 Bean/扩展/endpoint | common + stack adapter | auto-configuration imports |
| HTTP migration/RPC-only | Admin/Provider Golden 与 residual test 失败 | 标准注解迁移、旧类删除 | dependency isolation scan |
| Persistence | schema/repository tests 缺 V12/table/CAS | one migration + JDBC repos | PostgreSQL IT |
| Security/validation | malicious target/ref/size tests 先失败 | JDK HttpClient + Chain | named Beans/metrics |
| Mapping/aggregation | canonical/response/duplicate/atomic tests 先失败 | Adapter + Converter + Coordinator/Ingestion | recovery/lifecycle |
| API/UI | contract/component tests 先缺字段/状态 | query controller/React | E2E/static/full regression |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | all gateway POM/contract paths | locks build and shared vocabulary |
| Step 2 | Step 1 | None | common OpenAPI module | common API precedes adapters |
| Step 3 | Step 2 | None | MVC/WebFlux adapter modules | stack isolation must compile first |
| Step 4 | Step 3 | None | provider test apps | produces Golden and group manifests |
| Step 5 | Step 3 | Step 4 after shared adapter commit | gateway-admin existing HTTP surface | no overlapping provider files |
| Step 6 | Steps 4–5 | None | base starter/contract source consumers | delete only after all HTTP usages move |
| Step 7 | Step 1 | Step 4 after Step 1 | V12/domain/repository | storage contract precedes pipeline |
| Step 8 | Step 7 | None | client/validation | candidate/document persistence needed |
| Step 9 | Steps 2,8 | None | adapter/converter | validated document precedes mapping |
| Step 10 | Steps 6,7,9 | None | reporting/aggregate/MCP release | shared writer needs final source vocabulary |
| Step 11 | Step 10 | None | sync/scheduled/config/lifecycle | orchestration after atomic service |
| Step 12 | Step 11 | None | query/controller/errors | exposes stable state only |
| Step 13 | Step 12 | None | admin-web | consumes final API contract |
| Step 14 | Steps 1–13 | None | regression/E2E/static gates | closure only; no feature implementation |

### 4.4 Commit boundaries

每个 Step 对应一个语义 commit，实施时只暂存该 Step 的 `Commit paths`。Step 4/5 可在不同 worktree 并行但不得共用 POM/adapter；本仓库当前用户文档改动永不进入任何实现 commit。数据库变化只在 Step 7 的一个 commit 中新增 V12，不修改旧 migration。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 三 adapter 模块 | primary §7.0/§8；Must | base starter 同时 optional MVC/WebFlux 且有自研 scanner | 一个模块会造成跨栈依赖；Springdoc standard 已复用 | 3 POM、3 autoconfig 边界 | Implement |
| Group Manifest DTO | amendment §7.0/§10 | DDC metadata 有 32 entry/512 value 限制，无完整集模型 | 直接传 CSV 会在 Provider/Admin 重复校验 | 1 shared record，无新 HTTP API | Implement |
| Admin pull API | primary API-001 | DDC 已给 host/port/secure/metadata | caller 传 URL 是 SSRF 且 fetch-then-forward | 每轮≤50 candidates/≤3 instances | Implement trusted-context derivation |
| Validation Chain | primary §13；amendment §13 | envelope/spec/ref/limit/x-egon/mcp 为独立可测规则 | 单巨型 if 会硬编码复杂变化 | 1 interface+6 rules，短路 | Implement |
| Adapter + MapStruct converter | primary §10.4 | Swagger graph 与 Report v2 层次不同，BaseConverter 已存在 | JSON round-trip/BeanUtils 不满足规则 | algorithm traversal + lossless boundary mapping | Implement |
| 两表 + one Definition Set | primary §11；amendment §11 | V1 definition_set 已存在，snapshot/sync 不存在 | 新建第三张 aggregate 表重复；现有 set 可复用 | 仅 V12 两表、多对一索引 | Implement without third table |
| Coordinator/Ingestion split | amendment DEC-A02 | current ReportService 混合 transport/write | HTTP/RPC 各写一套会破坏不变量 | Coordinator 判完整集；Facade 单事务写 | Implement |
| Query APIs/UI | primary §9/§12；amendment §12 | 现有 Applications/Operation 页面与 API client 可扩展 | 浏览器直接拉 Provider 不安全 | 3只读 API+现有路由内面板 | Implement |
| RPC OpenAPI projection | amendment §3.2/§17 | ProtobufSchemaMapper 是类型事实源 | 把 RPC 伪装 HTTP 引入第二真相 | 本期零文件 | Reject/Not planned |
| 动态来源插件 SPI | amendment §7.0/§17 | 当前只有 HTTP/RPC 两来源 | 先新增 SPI/loader 属于投机层 | 本期只保留 Adapter seam | Reject/Not planned |
| 独立异常层 | primary §9.2 已规定 IllegalArgumentException/既有 advice | GatewayAdminExceptionHandler 已映射边界错误 | 新建未命名 exception hierarchy 无必要 | 零新异常类，扩展既有 advice | Direct reuse；`PLAN-CLAR-002` |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Build/Contract | `REQ-003,REQ-017,REQ-020` | enum/manifest/classpath tests | baseline Maven | BOM/modules/types | all backend Steps | Step 1 |
| Provider common | `REQ-001,REQ-002,REQ-004,REQ-016,REQ-017,REQ-022` | customizer/metadata RED | Step 1 | annotations/extensions/Manifest writer | adapters/providers | Step 2 |
| Stack adapters | `REQ-001,REQ-003,REQ-005` | endpoint/security/classpath RED | Step 2 | MVC/WebFlux APIs | consumers | Step 3 |
| Provider/Admin migration | `REQ-001,REQ-002,REQ-009,REQ-016,REQ-022` | Golden/residual RED | Step 3 | standard annotated HTTP code | Starter deletion | Steps 4–5 |
| RPC-only Starter | `REQ-003,REQ-009,REQ-010,REQ-020` | RPC Golden + residual RED | Steps 4–5 | Descriptor-only report | shared ingestion | Step 6 |
| V12 storage | `REQ-006,REQ-007,REQ-011,REQ-013,REQ-018,REQ-024` | schema/CAS RED | Step 1 | snapshot/sync repositories | pipeline | Step 7 |
| Secure acquire/validate | `REQ-004,REQ-005,REQ-021,REQ-025` | hostile fixture RED | Step 7 | bounded validated documents | adapter | Step 8 |
| Canonical adapter | `REQ-006,REQ-008,REQ-021,REQ-023` | canonical/mapping RED | Steps 2,8 | Report-ready DTO | aggregation | Step 9 |
| Atomic aggregate ingestion | `REQ-007,REQ-010,REQ-018,REQ-019,REQ-020,REQ-024` | transaction/recovery RED | Steps 6,7,9 | one Definition Set/all links | sync | Step 10 |
| Sync lifecycle | `REQ-011,REQ-025` | CAS/retry/stale RED | Step 10 | reconciled states/metrics | API | Step 11 |
| API/UI | `REQ-006,REQ-012,REQ-026` | controller/client/component RED | Step 11 | operator visibility | release gate | Steps 12–13 |
| Boundary regression | `REQ-010,REQ-014,REQ-015` | full static/module/E2E | all | closure evidence | implementation acceptance | Step 14 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | gateway-admin current `application/controller/service/repository/domain` feature packages；Spec §6.1 DEC-005 | Traditional Layered feature-first，仅允许此 profile | 新代码位于 `admin/openapi/...`，不引入 `biz.*` 或 Archetype COLA 混合 | All；`MC-ARCH-001` |
| Reuse/capability | Boot 3.5.16/Jackson/Validation/Security/OAuth2/DDC/JdbcTemplate/IdP token supplier/Report v2 repositories | 复用现有能力；只批准 Springdoc 2.8.17、Swagger models、MapStruct compile/processor、Testcontainers test | 无 Apache HTTP/Swagger Parser/UI；JDK HttpClient | 1–12；`MC-REUSE-001`,`MC-DEP-001` |
| Naming/model/validation/conversion | records 已广泛用于 DTO/PO/VO；`BaseConverter` 位于 common-core；Admin 当前无 MapStruct processor | simple carrier 用 record+compact ctor；enum 后缀 Enum；每层 @Valid/@Validated/groups；converter 继承 BaseConverter | 只创建 §10 命名类型；Algorithm Adapter 不冒充 Converter | 1,7–12；`MC-NAME-001`,`MC-VALID-001`,`MC-MODEL-001`,`MC-CONVERT-001` |
| Bean/logging/util/JSON/time/config | Admin 已有显式 configuration；gateway root 无 lombok.config；base/local YAML | 新/实质变更业务 Bean 使用 `@Slf4j`、显式 Bean 名、`@RequiredArgsConstructor`、final `@Qualifier`；Jackson/`java.time`；配置键一致 | 新增 root lombok config；机械 HTTP 注解迁移不改 DI；配置测试校验 parity | 1–12；`MC-LOG-001`,`MC-BEAN-001`,`MC-UTIL-001`,`MC-JSON-001`,`MC-TIME-001`,`MC-CONFIG-001` |
| Business variation/pattern | 多协议 acquisition、六类 validation、完整集/CAS/atomic ingest 为复杂流 | Adapter + Chain of Responsibility + Coordinator + Facade；scheduler 仅 orchestration | 策略按 protocol/source 配对，不建动态插件 SPI | 8–11；`MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| OpenAPI generation | existing starter HTTP scanner；Springdoc | `gateway-starter/discovery/http` 是第二类型系统；platform 无 springdoc | Springdoc 满足 MVC/WebFlux/GroupedOpenApi，现有 scanner 不满足 3.1 | approved addition | `org.springdoc:springdoc-openapi-bom:2.8.17` + common/webmvc-api/webflux-api，无 UI | Step 1–3；`MC-DEP-001` |
| JSON/OpenAPI model | Spring Boot ObjectMapper；Swagger models | Admin 已依赖 Jackson；Spec 禁止 parser | Jackson 负责 bytes/tree，Swagger model 来自 springdoc common | reuse/add approved transitive model | no parser/new JSON lib | Steps 2,8,9；`MC-JSON-001` |
| HTTP client | WebClient/RestTemplate/JDK HttpClient | Spec DEC-A06 锁定 JDK HttpClient；无需跨栈 | JDK supports no redirect/timeouts/TLS | reuse JDK | None | Step 8；`MC-REUSE-001` |
| OAuth2 | IdP `PlatformServiceAccessTokenSupplier` | Admin 已有 IdP/resource-server dependencies | 可按 trusted resource URI 获取 PLATFORM SERVICE token | reuse | None | Step 8；`MC-REUSE-001` |
| Discovery | DDC projection/registration contributor | `GatewayProjectionService` 已提供 host/port/secure/metadata/status/expireAt | 足以派生 candidate，不新增 subscription/API | reuse | None | Steps 2,11 |
| Conversion | `BaseConverter` + MapStruct 1.6.3 | `egon-cola-component-common-core/.../BaseConverter.java`；Admin 无 processor | BaseConverter contract 可复用，Admin 必须显式 compile+processor | approved addition | MapStruct 1.6.3 in Admin compiler config | Steps 1,9；`MC-CONVERT-001` |
| Persistence/transactions | JdbcTemplate/Flyway/PostgreSQL/existing report repos | Admin V1–V11、GatewayDefinitionReportRepository | 可复用 Definition 写入；缺 snapshot/sync | reuse + one V12/JDBC repos | Testcontainers test scope only | Steps 7,10 |
| Validation | Jakarta Validation/Spring `@Validated`/`ValidationUtils` | Admin/Starter 已使用 validation；无 phone fields | 原生约束+ValidationUtils 足够；业务 graph 用 Rule Chain | reuse | None | Steps 1,2,7–12 |
| Runtime schema | Invocation Schema v2/ProtobufSchemaMapper | contract/starter/RPC tests | HTTP Adapter 输出同一 Report；RPC truth 不变 | reuse | None | Steps 6,9,10 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | primary §6.2/§8/§10 | §5 Java inventory与现有 DTO/PO/VO/Enum 后缀 | tests→enum/record→service/repository/controller | 新载体只能用语义后缀，行为类不得伪装 POJO | compile + suffix/static scan | 1–12 | PASS |
| Rule 2 | primary §6.2/§9/§10；amendment §10.3 | config、DDC、job、adapter、ingest、API handoff | boundary tests→records/properties→`@Valid/@Validated` consumers | compact ctor normalization + Jakarta groups + ValidationUtils；phone N/A | positive/negative/group tests | 1–12 | PASS |
| Rule 3 | primary §6.2/§10；amendment §10 | new carriers all simple records；BaseConverter evidence | record tests→records→MapStruct converter→consumers | defensive copy/trim/sort；MapStruct component bean implements BaseConverter | constructor/mapping compile/tests | 1,7–12 | PASS |
| Rule 4 | primary §6.2/§13；amendment §6.3 | new business Beans and gateway root missing lombok.config | lombok.config→business tests→named Beans→qualified wiring | `@Slf4j` + explicit stereotype name + `@RequiredArgsConstructor` + final `@Qualifier` | context/bean-name/log/static checks | 1–12 | PASS |
| Rule 5 | primary §6.2/§15；amendment DEC-A06 | JDK/Spring/Jackson/approved commons available | POM gate before source | JDK HttpClient and JDK/Spring utilities；no custom general Utils | dependency/import scan | 1,8,14 | PASS |
| Rule 6 | primary §6.2/§9/§10 | external JSON via Jackson；Swagger model | contract tests→DTO/VO→controller/client | enum/string/Instant/raw document Jackson shape explicit | serialization/API contract tests | 1,2,7–13 | PASS |
| Rule 7 | primary §6.2/§15；amendment §15.1 | Admin/provider base/local resource files | property tests→all profile files together | identical key structure，values may differ | YAML key parity/config binding tests | 2,4,11 | PASS |
| Rule 9 | primary §13；amendment §13 | validated multi-source/validation/aggregate flow complexity | tests→Rule Chain/Adapters→Coordinator→Facade→wiring | no source/group giant switch；pattern participants named and tested | branch/pattern behavior tests | 8–11 | PASS |
| Rule 10 | primary §6.2/§10/§15；amendment §15 | existing `Clock`/Instant patterns；TIMESTAMPTZ | model tests→records/repos/services/VOs | only Instant/Duration/Clock；UTC/precision explicit | time boundary tests + forbidden import scan | 7–12 | PASS |
| Rule 11 | primary §6.1/§8；amendment §6.1 | actual feature-first Traditional Layered tree | every Step path remains under existing module/layer profile | no `biz.*`、no Archetype hybrid、dependency direction preserved | package architecture + tree scan | Every Step | PASS |

## 5. Change File Tree

```text
egon-cola-xingyuan/pom.xml                                                   MODIFY Springdoc 2.8.17 BOM
egon-cola-xingyuan/egon-cola-yuheng/
├── pom.xml                                                                   MODIFY add 3 modules/dependency management
├── lombok.config                                                             CREATE qualifier propagation
├── yuheng-contract/                                      MODIFY enum + openapi manifest + tests
├── yuheng-starter-openapi/                               CREATE common annotations/config/customizers/registration/tests/imports
├── yuheng-starter-openapi-webmvc/                        CREATE MVC API/security/classpath tests/imports
├── yuheng-starter-openapi-webflux/                       CREATE WebFlux API/security/classpath tests/imports
├── yuheng-starter/                                       MODIFY RPC-only; DELETE HTTP annotations/compiler/tests
├── yuheng-admin/
│   ├── pom.xml                                                               MODIFY adapter/common-core/MapStruct/Lombok/Testcontainers
│   ├── src/main/java/.../admin/openapi/                                      CREATE feature-first controller/client/service/scheduled/validation/converter/repository/domain
│   ├── src/main/java/.../admin/reporting/                                    MODIFY shared ingestion/source scope/lifecycle
│   ├── src/main/java/.../admin/*/controller/*.java                           MODIFY standard OpenAPI annotations
│   ├── src/main/java/.../admin/{catalog,mcp,observability,release,routing,shared}/domain/**/*.java MODIFY remove GatewaySchemaField/use @Schema only at wire DTO/VO
│   ├── src/main/resources/db/migration/V12__add_gateway_openapi_sync.sql      CREATE only migration
│   └── src/main/resources/application{,-local}.yml                           MODIFY parity
├── yuheng-admin-web/
│   ├── src/api/{types.ts,gatewayApi.ts,gatewayApi.test.ts}                   MODIFY
│   ├── src/features/applications/{ApplicationsPage.tsx,ApplicationsPage.test.tsx} MODIFY
│   ├── src/features/interface-catalog/{CatalogPage.tsx,OperationPage.tsx}     MODIFY
│   ├── src/features/interface-catalog/OperationPage.test.tsx                 CREATE
│   └── e2e/gateway-admin.spec.ts                                             MODIFY
└── yuheng-test/
    ├── *-http-provider, *-webflux-http-provider, *-idp-backend, *-mcp-provider POM/config/controllers/tests MODIFY
    └── */src/test/resources/openapi/{orders,inventory,webflux,mcp,drift}.json CREATE Golden
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-xingyuan/pom.xml` | Boot BOM，无 springdoc version | Springdoc BOM 2.8.17 after Boot BOM | dependency lock | 1 | `REQ-001,REQ-003` | dependency tree |
| CREATE/MODIFY | `egon-cola-xingyuan/egon-cola-yuheng/{lombok.config,pom.xml,yuheng-contract/**,yuheng-starter-openapi*/pom.xml,yuheng-admin/pom.xml` | modules/types absent | build graph、enum、Manifest、processor/classpath | compilation foundation | 1 | `REQ-003,REQ-017,REQ-020` | contract tests |
| CREATE | `.../yuheng-starter-openapi/src/{main,test}/**` | absent | three annotations、properties、customizers、registration、autoconfig | common Provider contract | 2 | `REQ-001,REQ-002,REQ-004,REQ-016,REQ-017,REQ-022` | common module tests |
| CREATE | `.../yuheng-starter-openapi-{webmvc,webflux}/src/{main,test}/**` | absent | stack API/security/autoconfig | stack isolation | 3 | `REQ-001,REQ-003,REQ-005` | adapter tests |
| MODIFY/CREATE | `.../yuheng-test/{yuheng-test-http-provider,yuheng-test-webflux-http-provider,yuheng-test-tianquan-shoubing-backend,yuheng-test-mcp-provider}/**` | old Gateway annotations | standard Swagger/Egon extensions、explicit groups、Golden | provider consumers | 4 | `REQ-001,REQ-002,REQ-016,REQ-021,REQ-022` | provider contracts |
| MODIFY/CREATE | `.../yuheng-admin/src/{main,test}/java/**` HTTP annotation inventory | 20 controllers + fully-qualified schema annotations | standard `@Tag/@Operation/@Schema` and x-egon customizers；业务逻辑不变 | Admin as Provider | 5 | `REQ-001,REQ-002,REQ-009,REQ-022` | Admin Golden/residual |
| MODIFY/DELETE | `.../yuheng-starter/{pom.xml,src/main/java/**,src/test/java/**}` | mixed HTTP/RPC compiler | RPC Descriptor-only Starter；HTTP classes/tests removed | breaking cleanup | 6 | `REQ-003,REQ-009,REQ-010,REQ-020` | RPC Golden/static |
| CREATE/MODIFY | `.../yuheng-admin/{src/main/resources/db/migration/V12__add_gateway_openapi_sync.sql,src/main/java/.../openapi/domain/**,src/main/java/.../openapi/repository/**,src/test/java/**}` | V1–V11 only | snapshots/sync CAS persistence | durable state | 7 | `REQ-006,REQ-007,REQ-011,REQ-013,REQ-018,REQ-024` | schema/PostgreSQL IT |
| CREATE | `.../yuheng-admin/src/{main,test}/java/.../openapi/{client,validation,domain/dto}/**` | absent | trusted candidate, bounded fetch, validation chain | secure acquisition | 8 | `REQ-004,REQ-005,REQ-021,REQ-025` | security tests |
| CREATE | `.../yuheng-admin/src/{main,test}/java/.../openapi/{converter,domain/dto}/**` | absent | canonical OpenAPI→DefinitionDTO→Report | semantic adaptation | 9 | `REQ-006,REQ-008,REQ-021,REQ-023` | adapter Golden |
| CREATE/MODIFY | `.../yuheng-admin/src/{main,test}/java/.../{openapi/service,reporting,mcp}/**` | transport/write coupled | aggregate coordinator + shared ingestion atomicity | one set/all groups | 10 | `REQ-007,REQ-010,REQ-018,REQ-019,REQ-020,REQ-024` | transaction/recovery |
| CREATE/MODIFY | `.../yuheng-admin/{src/main/java/.../{openapi/service,openapi/scheduled,config,bootstrap,reporting},src/main/resources/application*.yml,src/test/java/**}` | no OpenAPI scheduler | bounded CAS/retry/stale/metrics/config | lifecycle | 11 | `REQ-011,REQ-025` | scheduler/config tests |
| CREATE/MODIFY | `.../yuheng-admin/src/{main,test}/java/.../{openapi/controller,openapi/service,openapi/domain/vo,shared/controller}/**` | APIs absent | APIs 002–004 | read-only management | 12 | `REQ-006,REQ-012,REQ-026` | MVC/API tests |
| MODIFY/CREATE | `.../yuheng-admin-web/{src/api/**,src/features/applications/**,src/features/interface-catalog/**}` | no group/snapshot UI | aggregate/group/OpenAPI operator UI | frontend | 13 | `REQ-012,REQ-020,REQ-026` | Vitest/typecheck |
| MODIFY | `.../yuheng-admin-web/e2e/gateway-admin.spec.ts` | current catalog/admin E2E | multi-group/source/MCP optional/boundary assertions | closure | 14 | `REQ-010,REQ-014,REQ-015` | E2E/static/full reactor |

精确机械迁移清单由 Step 4/5/6 的 `rg` 命令锁定并在各 Step 文件块列出；`SchemaPanel.tsx` 明确不修改，继续渲染内部 v2 schema。任何实施时新增的匹配文件必须先回到本 Plan/Spec，不能扩大通配范围后直接提交。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 适用用户提供的 Main Agent/项目规则：最小安全改动、不启动项目、每 Step 一个 commit、现有 Flyway 不可变、使用现有风格并验证。
- 当前 `main@3be897e5...`；现有 modified/untracked `docs/egon` 均属于用户或并行工作，实施 commit 只使用每 Step 明列路径。
- 本 Plan 与 primary Spec 为未跟踪文档；写 Plan 只更新 primary Spec 的 `Related Plans`，不提交实现。
- 实施前每 Step 都必须重新执行 `git status --short` 和该 Step 的 path inventory；若 baseline 符号漂移导致公共契约/数据库/安全/事务变化，停止并回到 Spec。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | `./mvnw -version`；root `pom.xml` Java 21/Boot 3.5.16 | JDK 21，wrapper 可用 | compile/module，非运行拓扑 |
| Node | `yuheng-admin-web/package.json` | repository-defined npm scripts | frontend static/component |
| PostgreSQL | Testcontainers test scope；V12 schema test | Docker available only when running IT | persistence integration，不代表部署 |
| Runtime | user-controlled DDC/IdP/Provider/Gateway | 本 Plan 与实施 Steps 不自动启动 | live/E2E 仅由用户明确执行 |
| Git | `git status --short` | 无 Step 外 staged path | path-limited commit |

### 6.3 Immutable constraints and approved decisions

- Springdoc 锁定 2.8.17；无 Swagger UI、Swagger Parser、Apache HTTP client。
- Group 明确配置 1–16、排序唯一、CSV≤512，endpoint 固定 `/v3/api-docs/{group}`；DDC 精确 8 key。
- Source enum 精确 `MANUAL/RPC_DESCRIPTOR/OPENAPI31`；protocol/source 正交；RPC Descriptor 不经 OpenAPI ingestion。
- Raw 永久保存；fresh DB，无 backfill；只新增 `V12__add_gateway_openapi_sync.sql`，V1–V11 不改。
- Definition graph + 所有 snapshot links 一个事务；sync VALID 在事务外逐行 revision CAS，可通过 linked-set repair 恢复。
- HTTPS、trusted internal DNS、解析前/连接前所有地址 CIDR 双检、no redirect、no external `$ref`、OAuth2 PLATFORM SERVICE scope。
- OpenAPI 只更新 Definition/Catalog/MCP 投影，不自动产生 Route；MCP 缺失或 disabled 不阻断 Catalog。
- 不启动、部署、删库或执行 migration；用户将自行做运行验证。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | Admin POM 直接加入 `org.mapstruct:mapstruct:1.6.3` 与 compiler `mapstruct-processor:1.6.3`，而非只依赖 common-core | `BaseConverter` 在 common-core；其 MapStruct 为 test scope，platform compiler processor 仅 Spring config + Lombok | 只使 Spec 已要求的 mapper 可编译，不改变 wire/runtime | 若 parent 后续统一 processor，实施时可删局部重复但须保持版本一致 |
| `PLAN-CLAR-002` | 不新建无具体契约的 exception hierarchy；使用 `IllegalArgumentException`、既有 persistence exception 和 `GatewayAdminExceptionHandler` 分类 | primary §9.2 明确这些错误；§10 未命名 exception 类型 | 保持已定 HTTP error/state side effect，减少投机类型 | 若实现发现跨层必须区分且现有类型无法表达，需回 Spec 命名公共错误 |
| `PLAN-CLAR-003` | 三个新 starter 均创建 Boot `AutoConfiguration.imports`，测试 fixture 放各自 module `src/test/resources/openapi` | 现有 gateway-starter 使用 imports；Spec 指定 Golden 但未逐一给资源名 | 仅遵循 Boot 3.5 自动配置与测试资源惯例 | 路径错误只影响 wiring/test，可局部修正 |
| `PLAN-CLAR-004` | Admin 现有 controller 只做 HTTP annotation 机械迁移，不因注解替换重写其稳定 constructor/DI；Rule 4 完整应用于所有新或业务逻辑实质变更 Bean | 当前 controller 构造与业务行为已稳定；Spec change 是文档 metadata | 避免把 OpenAPI 改造扩大成全 Admin bean rename | 若用户要求“触及文件即全量迁移”，范围会扩展到所有依赖 Bean，应先增补 Spec |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 锁定 Springdoc 模块图与共享来源/Group Manifest Contract

- Requirements: `REQ-001, REQ-003, REQ-017, REQ-020`
- Dependencies: `None`
- Baseline state: Gateway reactor 只有 contract/core/mcp-core/engine/admin/starter/test；platform 无 Springdoc BOM；gateway root 无 `lombok.config`；Contract 的 `sourceType` 为 String 且无 Group Manifest。
- Observable outcome: Maven 能识别三个新 adapter module；Contract 精确暴露三值 source enum 与已归一的 Group Manifest record；依赖树无 UI/parser/跨 Web 栈泄漏。
- End state: 只建立可编译基础和共享契约，不注册 endpoint、不改变现有 HTTP/RPC 运行行为；后续 Step 可单独实现 common/stack adapter。
- Test-first gate: Required — 先新增 enum/Manifest contract tests；在生产类型出现前因 missing symbols 或枚举/校验契约不满足而 RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/pom.xml`

- Purpose: 先锁定经过批准的 Springdoc 版本和编译依赖基线。
- Symbols: `springdoc.version=2.8.17`、`springdoc-openapi-bom` import；同批随后修改 gateway aggregator/POM、新建三个 module POM、Admin POM 和 gateway `lombok.config`。
- Repository evidence: platform POM 在 Spring Boot BOM 下统一版本；gateway aggregator 列举现有 modules；Admin POM 缺 Springdoc/common-core/Lombok/MapStruct processor/Testcontainers；platform compiler processor 只有 Spring configuration processor 与 Lombok。
- Dependencies and consumers: 所有三个 adapter、Admin、Provider test apps；MapStruct 只进入 Admin compile，Testcontainers 只进入 Admin test。
- Why now: 生产测试必须先拥有可解析的 module/classpath；此文件不承载业务行为。
- Contract/signature changes: 新增 Springdoc BOM 2.8.17；gateway aggregator 加 `starter-openapi`、`starter-openapi-webmvc`、`starter-openapi-webflux`；base starter 不引入 springdoc；MVC/WebFlux POM 各只引入自己的 API artifact；Admin 引入 MVC adapter/common-core/MapStruct/Lombok/Testcontainers。
- Input/output and state mapping: Maven property/BOM → resolved dependency versions；module list → reactor order；`lombok.config` 复制 `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier` 与 `Value` propagation。
- Error and edge behavior: dependency convergence、Swagger artifact drift、UI/parser 出现或 MVC/WebFlux cross-import 均使 gate 失败；不修改 root Java/Boot version。
- Standards impact: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-BEAN-001, MC-UTIL-001`；只引入 Spec 批准能力并保持 Traditional Layered module direction。
- Literal rule enforcement: `Rule 4` 通过 gateway root `lombok.config` 保证 Qualifier 复制；`Rule 5` 禁止未批准 utility/network library；`Rule 11` 保持现有 gateway module profile。
- Implementation pseudocode:

```xml
declare property springdoc.version = 2.8.17
import springdoc-openapi-bom after spring-boot-dependencies
register common, webmvc and webflux adapter modules in dependency order
configure admin MapStruct processor 1.6.3 and test-only Testcontainers; reject swagger-ui/parser and cross-stack artifacts
```

- Verification contribution: `dependency:tree` 和三个 module 的 compile classpath 证明版本/栈隔离。
- After this file: Maven 图可解析新模块但模块仍无业务 Bean；原 starter 行为不变。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/test/java/top/egon/cola/component/gateway/contract/reporting/openapi/GatewayOpenApiGroupManifestDTOTest.java`

- Purpose: 用 RED 锁定 source enum、Manifest 归一和 Jackson/Validation wire shape。
- Symbols: `GatewayDefinitionSourceTypeEnumTest`、`GatewayOpenApiGroupManifestDTOTest`；断言 exact enum values、1–16 group、sorted unique、CSV≤512、fixed path template、defensive copy、artifact/build/resource URI。
- Repository evidence: contract tests使用 JUnit；`GatewayInterfaceDefinitionReport` 已是共享 wire record；amendment §10 指定 compact constructor。
- Dependencies and consumers: 待创建 enum/record；Provider registration 与 Admin candidate 共同消费。
- Why now: shared public contract 必须在任何 producer/consumer 实现前被冻结。
- Contract/signature changes: 测试要求 source enum 只有 `MANUAL/RPC_DESCRIPTOR/OPENAPI31`；Manifest constructor 对 groups trim/sort/copy 并拒绝 duplicate/空值/越界/非固定 template。
- Input/output and state mapping: unsorted mutable list → immutable sorted list；invalid URI/build/group → constraint violation或 `IllegalArgumentException`；Jackson JSON field names保持 camelCase。
- Error and edge behavior: 0/17 groups、重复、group regex 不符、CSV 513、错误 template、null resource/artifact/build 均 RED；phone validation N/A。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TEST-001`；测试证明 record/compact ctor/标准 Validation。
- Literal rule enforcement: `Rule 1` 固定 DTO/Enum 后缀；`Rule 2` 证明边界校验；`Rule 3` 证明 record 防御复制；`Rule 6` 证明 Jackson 外部契约；`Rule 11` 文件位于 contract 既有分层。
- Implementation pseudocode:

```java
assert enum names equal exactly MANUAL, RPC_DESCRIPTOR, OPENAPI31 in declaration order
construct manifest from groups [inventory, orders], fixed template, resourceUri, artifactVersion, buildId
assert groups are immutable and sorted; serialize then deserialize with Spring Boot ObjectMapper
for each invalid cardinality, duplicate, length and template fixture assert construction or validation fails before consumer use
```

- Verification contribution: 首次运行因类型不存在 RED；GREEN 后证明共享契约而非 producer 私有约定。
- After this file: 测试精确描述缺失契约，尚无生产实现。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/main/java/top/egon/cola/component/gateway/contract/reporting/openapi/GatewayOpenApiGroupManifestDTO.java`

- Purpose: 创建最小共享 record；同批创建 `reporting/GatewayDefinitionSourceTypeEnum.java` 和对应 enum test。
- Symbols: `GatewayOpenApiGroupManifestDTO` record、compact constructor、Validation annotations；`GatewayDefinitionSourceTypeEnum`。
- Repository evidence: contract 使用 records；amendment §10.1/10.2 给出字段和 enum；DDC metadata value limit 为512。
- Dependencies and consumers: contract/Jakarta Validation/Jackson；Step 2 registration 和 Step 11 Admin sync 依赖；不依赖 Springdoc/Admin。
- Why now: 测试已固定 public vocabulary，最小生产类型可使其 GREEN。
- Contract/signature changes: fields=`groups,pathTemplate,resourceUri,artifactVersion,buildId`；groups 1–16；path template exact；enum exact three values。
- Input/output and state mapping: constructor trim identifiers，copy/sort groups，derive CSV length check；null/blank rejected；no time/precision state。
- Error and edge behavior: duplicates rejected而非 silently dedupe；invalid URI/template/build fail before DDC publication；enum unknown由 Jackson 失败。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001`；simple immutable record，不创建 complex Lombok carrier。
- Literal rule enforcement: `Rule 1` 语义 DTO/Enum；`Rule 2` Jakarta+compact normalization；`Rule 3` record；`Rule 6` Jackson-compatible；`Rule 11` contract layer无反向依赖。
- Implementation pseudocode:

```java
record GatewayOpenApiGroupManifestDTO(groups, pathTemplate, resourceUri, artifactVersion, buildId) {
  compact constructor: require non-null fields; trim stable strings; copy and sort groups
  reject group count outside 1..16, blanks, duplicates, invalid code pattern or joined CSV length above 512
  require pathTemplate equals /v3/api-docs/{group}; retain URI without credentials
}
```

- Verification contribution: contract tests GREEN；downstream compilation可引用单一 enum/Manifest。
- After this file: build graph与共享 contract 完成；无 endpoint/DB/runtime side effect。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux -am -Dtest=GatewayDefinitionSourceTypeEnumTest,GatewayOpenApiGroupManifestDTOTest -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux dependency:tree -Dincludes=org.springdoc:*,io.swagger.core.v3:*`
- Expected result: named contract tests pass；base starter dependency tree 无 Springdoc；MVC/WebFlux adapter 只出现对应栈 API；无 swagger-ui/parser。
- Failure returns to: File 1 for version/module/classpath；File 2 for contract assertion；File 3 for validation/serialization。
- Completion criteria: 新 modules 均可解析；enum/Manifest exact contract通过；用户 dirty docs 未 staged。
- Rollback: path-limited revert Step 1 files；无数据库或运行态影响。
- Commit paths: `egon-cola-xingyuan/pom.xml`, `egon-cola-xingyuan/egon-cola-yuheng/pom.xml`, `egon-cola-xingyuan/egon-cola-yuheng/lombok.config`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/test/java/top/egon/cola/component/gateway/contract/reporting/openapi/GatewayOpenApiGroupManifestDTOTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/main/java/top/egon/cola/component/gateway/contract/reporting/openapi/GatewayOpenApiGroupManifestDTO.java`, three new adapter `pom.xml`, Admin `pom.xml`, enum production/test paths
- Commit: `build(gateway): establish openapi adapter modules and source contracts`

### Step 2 — 实现 Provider OpenAPI common 扩展与有界 Group Manifest 发布

- Requirements: `REQ-001, REQ-002, REQ-004, REQ-016, REQ-017, REQ-021, REQ-022`
- Dependencies: `Step 1`
- Baseline state: common module只有 POM；Contract 已有 enum/Manifest；DDC registration contributor pattern存在于 base starter。
- Observable outcome: Provider 配置显式 published groups 后，Springdoc customizers 生成稳定 operationId/x-egon/x-egon-service，DDC contributor 只发布精确 8 key 的排序 Manifest。
- End state: common module与 Web stack 无关；尚不暴露 MVC/WebFlux endpoint。
- Test-first gate: Required — annotation/properties/customizer/registration tests 先因 Bean/annotations/extension mapping不存在而 RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/test/java/top/egon/cola/component/gateway/openapi/customizer/EgonOpenApiCustomizerTest.java`

- Purpose: 汇总 common RED；同批创建 annotation contract、properties、operation customizer、registration contributor、auto-configuration tests。
- Symbols: `GatewayOpenApiAnnotationContractTest`、`GatewayOpenApiPropertiesTest`、`EgonOperationCustomizerTest`、`EgonOpenApiCustomizerTest`、`GatewayOpenApiRegistrationContributorTest`、`GatewayOpenApiAutoConfigurationTest`。
- Repository evidence: starter tests用 ApplicationContextRunner/JUnit；Spec固定三 annotation fields、extension version、DDC keys/defaults。
- Dependencies and consumers: Swagger OpenAPI model、GroupedOpenApi-facing customizers、DDC contributor、Contract Manifest。
- Why now: 先固定标准 OpenAPI extension wire shape与不发布 Secret/Raw 的负面契约。
- Contract/signature changes: 断言 operationId convention、`x-egon.version=1`、catalog/policy/optional mcp；service extension group/build校验；metadata exact 8 keys。
- Input/output and state mapping: annotated handler/method + group config → OpenAPI operation/root extensions；published groups → sorted CSV metadata；disabled/absent MCP → no tool extension。
- Error and edge behavior: missing operationId、group/build mismatch、unknown/unpublished group、duplicate/oversized list、secret/raw key 均 fail startup/publication；MCP absent succeeds。
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-PATTERN-001, MC-TEST-001`；验证所有 boundary/default/negative branch。
- Literal rule enforcement: `Rule 2` properties+Manifest validation；`Rule 6` extension JSON；`Rule 7` keys/default tests；`Rule 9` customizers各司其职而非 giant hardcode；`Rule 11` common module不依赖 Web stack。
- Implementation pseudocode:

```java
create annotated controller fixtures with catalog, policy and enabled/disabled/absent mcp cases
invoke operation and document customizers for published group orders and build build-1
assert stable operationId, exact versioned x-egon fields and x-egon-service cross-check
invoke registration contributor and assert exact eight sorted metadata entries, no raw document, token, URL override or secret
```

- Verification contribution: tests RED only for missing common production symbols/behavior；GREEN proves API/DDC wire contracts。
- After this file: common behavior contract frozen, production仍 absent。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/main/java/top/egon/cola/component/gateway/openapi/config/GatewayOpenApiProperties.java`

- Purpose: 创建 typed Provider configuration；同批创建 `EgonApiCatalog`、`EgonGatewayPolicy`、`EgonMcpTool` annotations。
- Symbols: `GatewayOpenApiProperties` named config Bean；`enabled,publishedGroups,pathTemplate,spec,resourceUri,artifactVersion,buildId`；三 annotations exact fields。
- Repository evidence: primary/amendment §7.3/§10/§15.1；Boot configuration properties patterns已存在。
- Dependencies and consumers: Spring Binder/Jakarta Validation/Contract Manifest；customizers与registration读取。
- Why now: annotations/config先于 customizer/registration实现。
- Contract/signature changes: prefix `gateway.openapi`；default path/spec；groups 1–16；annotation targets/retention与 fields完全按 amendment。
- Input/output and state mapping: YAML string/list → normalized immutable group set；annotations → runtime metadata；MCP enabled default false。
- Error and edge behavior: enabled=true而缺 groups/resource/build/artifact fail startup；CSV>512/invalid group fail；disabled允许不注册。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-BEAN-001, MC-CONFIG-001`；behavior config class显式 Bean，annotation不是 carrier。
- Literal rule enforcement: `Rule 1` semantic class names；`Rule 2` @Validated/native constraints/ValidationUtils aggregate check；`Rule 3` exposed group copies；`Rule 4` named Spring Bean；`Rule 7` typed keys；`Rule 11` config/annotation packages符合 profile。
- Implementation pseudocode:

```java
bind gateway.openapi fields and validate when enabled
normalize each published group by trim; reject empty, duplicates, count outside 1..16 and joined length above 512
define runtime annotations whose values map one-to-one to catalog, policy and optional mcp extension fields
expose normalized manifest input without WebMvc, WebFlux or Admin dependencies
```

- Verification contribution: properties/annotation tests GREEN；negative binding证明 fail-fast。
- After this file: config与声明 contract可用，尚未改变 OpenAPI model/DDC。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/main/java/top/egon/cola/component/gateway/openapi/customizer/EgonOperationCustomizer.java`

- Purpose: 实现 operation/root extensions；同批创建 `EgonOpenApiCustomizer`。
- Symbols: named `egonOperationCustomizer`、`egonOpenApiCustomizer`，`@Slf4j`、`@RequiredArgsConstructor`、final qualified dependencies。
- Repository evidence: Springdoc提供 OperationCustomizer/OpenApiCustomizer；Spec §7.3.3–7.3.5定义 mapping与 operationId。
- Dependencies and consumers: annotations/properties/Swagger models；GroupedOpenApi bean在各 adapter消费。
- Why now: config/annotation先完成，测试已固定 mapping。
- Contract/signature changes: only add/validate `x-egon` and `x-egon-service`；不覆盖 standard paths/components；operationId缺失/重复阻断。
- Input/output and state mapping: HandlerMethod metadata → extension maps；method/path/group/build → stable ID/cross-check；MCP absent/false不生成 enabled Tool。
- Error and edge behavior: conflicting explicit standard extension或 media mapping不在本层处理；敏感值不日志；普通 API 无 MCP block照常成功。
- Standards impact: `MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-PATTERN-001`；named Beans、qualified DI、Jackson-compatible map、Adapter职责。
- Literal rule enforcement: `Rule 4` business Beans完整注解/Bean名/Qualifier；`Rule 5` 只用 JDK/Spring/Swagger；`Rule 6` JSON extension exact；`Rule 9` operation/document customizer分工；`Rule 11` common customizer layer。
- Implementation pseudocode:

```java
customize operation: read three Egon annotations, require stable operationId, build version=1 extension maps
omit mcp map when annotation absent or enabled=false; when enabled copy governed fields and validate completeness
customize document: attach x-egon-service with application/build/group/artifact/resource identifiers
reject group/build mismatch before returning model; log only applicationId, buildId, group and safe error code
```

- Verification contribution: customizer tests证明 optional MCP与 exact extension mapping；不修改 standard schemas。
- After this file: Springdoc model可被治理增强，但未注册 auto-configuration。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/main/java/top/egon/cola/component/gateway/openapi/registration/GatewayOpenApiRegistrationContributor.java`

- Purpose: 生成 DDC Manifest并装配 common auto-configuration/imports。
- Symbols: named contributor、`GatewayOpenApiAutoConfiguration`、Boot `AutoConfiguration.imports`。
- Repository evidence: base starter已有 DDC registration contributor merge pattern；DDC metadata constraints已验证。
- Dependencies and consumers: properties/Manifest/DDC registration SPI；Admin Step 11消费 keys。
- Why now: extension模型完成后，Provider才可发布发现能力。
- Contract/signature changes: metadata exact keys=`gateway.definition-source,gateway.openapi.enabled,gateway.openapi.path-template,gateway.openapi.spec,gateway.openapi.groups,gateway.openapi.resource-uri,gateway.artifact-version,gateway.build-id`。
- Input/output and state mapping: validated properties → manifest → sorted string metadata；disabled → no OpenAPI keys/contributor effect。
- Error and edge behavior: contributor不得发布 raw bytes/token/secret/full URL；existing unrelated DDC metadata preserved；冲突 key fail startup而非覆盖。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-CONFIG-001, MC-PATTERN-001`；Facade-style contributor复用 DDC SPI。
- Literal rule enforcement: `Rule 2` 只接收 validated Manifest；`Rule 4` named/@Slf4j/RequiredArgsConstructor/Qualifier；`Rule 7` keys与 properties tests一致；`Rule 9` contributor只负责编排；`Rule 11` registration/config层。
- Implementation pseudocode:

```java
if gateway.openapi.enabled is false return registration unchanged
construct and validate GatewayOpenApiGroupManifestDTO from normalized properties
merge exactly eight allowlisted keys using sorted group CSV and fixed path template
reject collisions or DDC size violations; never include document bytes, bearer token, credentials or caller-controlled URL
```

- Verification contribution: registration/auto-config tests GREEN并证明 exact whitelist/disabled branch。
- After this file: common module完整可用且仍 Web-stack-neutral。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi -am test`
- Expected result: common module全部 annotation/property/customizer/registration/autoconfig tests pass；dependency tree无 MVC/WebFlux/UI/parser。
- Failure returns to: File 1 contract assertions；File 2 binding；File 3 extension mapping；File 4 DDC whitelist/wiring。
- Completion criteria: enabled/disabled、多 Group、MCP optional、DDC exact 8 keys与负面边界全部有 focused proof。
- Rollback: revert common module source/resources/tests；Step 1 scaffold可保留但无运行 Bean。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/test/java/top/egon/cola/component/gateway/openapi/customizer/EgonOpenApiCustomizerTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/main/java/top/egon/cola/component/gateway/openapi/config/GatewayOpenApiProperties.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/main/java/top/egon/cola/component/gateway/openapi/customizer/EgonOperationCustomizer.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi/src/main/java/top/egon/cola/component/gateway/openapi/registration/GatewayOpenApiRegistrationContributor.java`, all same-module annotations/config/customizer tests and `AutoConfiguration.imports`
- Commit: `feat(gateway-openapi): publish governed multi-group manifests`

### Step 3 — 提供隔离的 MVC 与 WebFlux OpenAPI 3.1 安全适配器

- Requirements: `REQ-001, REQ-003, REQ-005, REQ-016`
- Dependencies: `Step 2`
- Baseline state: common annotations/customizers/Manifest可用；两个 stack module只有 POM。
- Observable outcome: MVC和WebFlux分别由 Springdoc API artifact暴露 grouped documents，要求 `SCOPE_gateway.openapi.read`，不互相引入 Web stack。
- End state: adapter可供业务 Provider依赖；未迁移业务 Controller前只在测试 fixture证明 endpoint/security。
- Test-first gate: Required — 两个 module先创建 endpoint/security/classpath tests，因 autoconfiguration/security matcher不存在而 RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 2, Rule 4, Rule 5, Rule 7, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc/src/test/java/top/egon/cola/component/gateway/openapi/webmvc/GatewayOpenApiWebMvcContractTest.java`

- Purpose: 定义 MVC grouped endpoint、OAuth scope和 classpath isolation；同批创建 security/autoconfig tests。
- Symbols: `GatewayOpenApiWebMvcContractTest`、`GatewayOpenApiWebMvcSecurityAutoConfigurationTest`、`GatewayOpenApiWebMvcDependencyContractTest`。
- Repository evidence: Spring Security resource-server tests与ApplicationContextRunner模式可复用；Spec API-001固定 endpoint/scope。
- Dependencies and consumers: common module、springdoc webmvc-api、mock MVC security；Provider apps Step 4。
- Why now: 先冻结 MVC公开契约再注册 security/autoconfig。
- Contract/signature changes: GET `/v3/api-docs/orders` with correct scope→200/3.1；anonymous/wrong scope→401/403；no WebFlux classes。
- Input/output and state mapping: published group + annotated handler + JWT scope → JSON doc；unpublished group→404；no UI route。
- Error and edge behavior: `/swagger-ui/**`不存在；query/url override不得改变 group；security只放行指定 scope并保留应用其他规则。
- Standards impact: `MC-DEP-001, MC-VALID-001, MC-CONFIG-001, MC-TEST-001`；验证 stack/security边界。
- Literal rule enforcement: `Rule 2` security/endpoint boundary；`Rule 4` wiring Bean name在context断言；`Rule 5`无额外HTTP库；`Rule 7`配置读取一致；`Rule 11` MVC adapter独立 module。
- Implementation pseudocode:

```java
boot minimal MVC context with published group orders and annotated controller fixture
request /v3/api-docs/orders as anonymous, wrong-scope and correct-scope JWT
assert 401 or 403 for denied cases and 200 with openapi 3.1 plus paths/extensions for allowed case
assert WebFlux and swagger-ui classes/routes are absent from the MVC adapter classpath
```

- Verification contribution: MVC RED/GREEN与 dependency isolation proof。
- After this file: MVC行为测试固定，production config尚缺。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc/src/main/java/top/egon/cola/component/gateway/openapi/webmvc/GatewayOpenApiWebMvcSecurityAutoConfiguration.java`

- Purpose: 装配 MVC Springdoc API、GroupedOpenApi/customizers及 endpoint security；同批创建 imports。
- Symbols: named security/autoconfig Beans，conditional MVC classes/properties。
- Repository evidence: Boot 3.5 auto-configuration imports；Admin/Provider已使用 SecurityFilterChain。
- Dependencies and consumers: Step 2 common Beans、springdoc webmvc-api、existing resource server。
- Why now: MVC tests已固定行为。
- Contract/signature changes: 只注册 configured published groups；matcher `/v3/api-docs/{group}`要求 scope；不创建 UI。
- Input/output and state mapping: properties group selectors → GroupedOpenApi beans；JWT authorities → permit/deny；customizers附加治理 metadata。
- Error and edge behavior: missing resource server或 disabled properties时条件化不误开放；Bean name collision fail context；unpublished group不注册。
- Standards impact: `MC-REUSE-001, MC-LOG-001, MC-BEAN-001, MC-CONFIG-001`；named Bean/Qualifier与现有 security chain协作。
- Literal rule enforcement: `Rule 4` @Slf4j/named Beans/RequiredArgsConstructor/final Qualifier；`Rule 5` 仅Springdoc/Spring Security；`Rule 7` typed properties；`Rule 11` MVC-only package/module。
- Implementation pseudocode:

```java
for each validated published group create a uniquely named GroupedOpenApi using standard selectors
attach qualified Egon operation and document customizers
compose MVC security matcher for grouped docs and require SCOPE_gateway.openapi.read
guard configuration with servlet and property conditions; never register swagger-ui resources
```

- Verification contribution: MVC contract/security/context tests GREEN。
- After this file: MVC adapter可独立消费，WebFlux仍未实现。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux/src/test/java/top/egon/cola/component/gateway/openapi/webflux/GatewayOpenApiWebFluxContractTest.java`

- Purpose: 对称定义 reactive endpoint/security/classpath，不复制 common governance逻辑。
- Symbols: WebFlux contract/security/dependency tests。
- Repository evidence: test-webflux-provider当前为独立 module；Springdoc提供 webflux-api。
- Dependencies and consumers: common module、springdoc webflux-api、WebTestClient；Step 4 reactive provider。
- Why now: 与 MVC实现分 module，先测试防止 servlet依赖泄漏。
- Contract/signature changes: `/v3/api-docs/inventory` 3.1、scope gating、无 MVC/UI。
- Input/output and state mapping: reactive handler metadata → same x-egon wire shape；JWT→reactive security decision。
- Error and edge behavior: unauthorized/unpublished/disabled分支；no blocking client；no servlet classes。
- Standards impact: `MC-DEP-001, MC-VALID-001, MC-CONFIG-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2` endpoint/security验证；`Rule 4` context Bean assertions；`Rule 5` stack allowlist；`Rule 7` same keys；`Rule 11` WebFlux-only module。
- Implementation pseudocode:

```java
boot minimal reactive context with one published group and annotated reactive controller
call grouped document with WebTestClient under anonymous, wrong-scope and allowed JWT identities
assert same OpenAPI 3.1 and Egon extensions as common contract
assert servlet MVC and swagger-ui artifacts are absent and no blocking WebClient replacement is introduced
```

- Verification contribution: reactive RED/GREEN/classpath proof。
- After this file: WebFlux期望已冻结。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux/src/main/java/top/egon/cola/component/gateway/openapi/webflux/GatewayOpenApiWebFluxSecurityAutoConfiguration.java`

- Purpose: 以 reactive SecurityWebFilterChain/GroupedOpenApi装配 WebFlux adapter并创建 imports。
- Symbols: named WebFlux security/autoconfig Beans、conditional reactive wiring。
- Repository evidence: repository WebFlux provider与Boot autoconfig约定；common customizers不依赖 stack。
- Dependencies and consumers: Step 2 common、springdoc webflux-api；reactive providers。
- Why now: reactive tests已RED且MVC实现不能复用 security type。
- Contract/signature changes: 与 MVC相同配置/extension contract，唯一差异为 reactive stack security与resource。
- Input/output and state mapping: published groups → grouped resources；reactive authentication → scope decision。
- Error and edge behavior: disabled/no stack条件不注册；不导入 servlet API；Bean collisions fail fast。
- Standards impact: `MC-REUSE-001, MC-LOG-001, MC-BEAN-001, MC-CONFIG-001`。
- Literal rule enforcement: `Rule 4` named/@Slf4j/RequiredArgsConstructor/Qualifier；`Rule 5` approved dependencies；`Rule 7` parity；`Rule 11` strict module direction。
- Implementation pseudocode:

```java
conditionally activate only for reactive web application and gateway.openapi.enabled
register grouped resources from normalized published groups with common customizers
compose reactive security matcher requiring SCOPE_gateway.openapi.read
leave existing application security decisions intact outside /v3/api-docs/{group}
```

- Verification contribution: WebFlux tests GREEN；combined dependency tree proves隔离。
- After this file: 两个 adapter ready for consumer migration。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux -am test && ./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux dependency:tree -Dincludes=org.springframework:spring-webmvc,org.springframework:spring-webflux,org.springdoc:*,io.swagger.core.v3:*`
- Expected result: 两个 module test exit 0；authorized grouped docs为3.1；unauthorized denied；MVC/WebFlux互不泄漏且无 UI/parser。
- Failure returns to: File 1/3 contract/security expectations；File 2/4 stack-specific autoconfiguration。
- Completion criteria: API-001在两栈有 focused proof，common extension结果一致，base starter仍无 Springdoc。
- Rollback: revert两个 stack module sources/resources/tests；common module仍可保留无endpoint。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc/src/test/java/top/egon/cola/component/gateway/openapi/webmvc/GatewayOpenApiWebMvcContractTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webmvc/src/main/java/top/egon/cola/component/gateway/openapi/webmvc/GatewayOpenApiWebMvcSecurityAutoConfiguration.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux/src/test/java/top/egon/cola/component/gateway/openapi/webflux/GatewayOpenApiWebFluxContractTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter-openapi-webflux/src/main/java/top/egon/cola/component/gateway/openapi/webflux/GatewayOpenApiWebFluxSecurityAutoConfiguration.java`, same-module security/dependency tests and both `AutoConfiguration.imports`
- Commit: `feat(gateway-openapi): add isolated mvc and webflux adapters`

### Step 4 — 迁移仓内 Provider fixtures 到标准多 Group OpenAPI

- Requirements: `REQ-001, REQ-002, REQ-004, REQ-016, REQ-017, REQ-021, REQ-022`
- Dependencies: `Step 3`
- Baseline state: HTTP/WebFlux/IdP/MCP test providers依赖 base starter并使用旧 Gateway HTTP annotations；配置无显式 published groups。
- Observable outcome: 每个 fixture app依赖正确 adapter、配置自己的 groups，标准 Swagger annotations + Egon extensions生成稳定 per-group Golden；HTTP provider有 orders/inventory两组。
- End state: 所有 repository-local test Provider HTTP consumers迁移；Admin HTTP surface留给 Step 5；旧 starter类型仍暂时存在供 Step 6删除。
- Test-first gate: Required — 先改 contract tests/Golden期望为 grouped 3.1与 optional MCP；在 POM/config/controller迁移前因 endpoint/JSON差异 RED。
- Manual Checks: `MC-ARCH-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 6, Rule 7, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/src/test/java/top/egon/cola/component/gateway/test/http/HttpProviderContractTest.java`

- Purpose: 建立所有 Provider grouped Golden RED；同批修改 `WebFluxHttpProviderContractTest`、`MockBackendControllerContractTest`、`McpOperationSchemaContractTest`并创建 per-module Golden JSON。
- Symbols: group endpoint tests、extension assertions、orders/inventory/webflux/idp/mcp/drift fixtures。
- Repository evidence: 这些测试当前直接断言旧 annotations/schema；四个 Provider module为仓内真实 consumer。
- Dependencies and consumers: Step 3 adapters、待迁移 controllers/config；fixtures供 Step 9 Admin adapter复用。
- Why now: 先把最终标准文档作为破坏性迁移契约。
- Contract/signature changes: HTTP provider `/orders`与`/inventory` group文档互斥且 operationId全局唯一；WebFlux/IdP/MCP各显式 group；MCP absent/disabled/enabled三态。
- Input/output and state mapping: controllers/config → OpenAPI JSON normalized fixture；group path→only selected operations；build/group extension字段精确。
- Error and edge behavior: unpublished group 404；重复 operationId/overlap fail；drift fixture只用于Admin，不作为可发布成功文档。
- Standards impact: `MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-CONFIG-001, MC-TEST-001`；测试覆盖配置与wire model。
- Literal rule enforcement: `Rule 2` endpoint/annotations校验；`Rule 3` test fixtures不引入复杂 carrier；`Rule 6` JSON Golden；`Rule 7`配置键 parity；`Rule 11`各 fixture保持 module边界。
- Implementation pseudocode:

```java
request each configured /v3/api-docs/{group} with gateway.openapi.read authority
normalize volatile JSON fields then compare exact group Golden fixture
assert orders and inventory operationId sets are disjoint and x-egon-service group/build matches request
assert ordinary operation has no mcp projection while explicit enabled tool has complete versioned mcp extension
```

- Verification contribution: 迁移前RED；完成后四个 provider contract suites与Golden GREEN。
- After this file: final provider behavior已定义但现有 controllers/config不满足。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/pom.xml`

- Purpose: 切换四个 Provider模块到正确 adapter并保持依赖隔离。
- Symbols: HTTP/IdP/MCP→webmvc adapter；WebFlux→webflux adapter；移除用于HTTP schema的base starter依赖，RPC依赖不动。
- Repository evidence: 现有 POM直接使用 gateway-starter optional web dependencies；Step 3已提供专用 modules。
- Dependencies and consumers: controllers/tests/application.yml；reactor test modules。
- Why now: tests已定义目标，source迁移前需有标准 annotations/classpath。
- Contract/signature changes: dependency-only breaking switch；无 runtime endpoint path变化除 grouped docs。
- Input/output and state mapping: Maven artifact choice → stack-specific Springdoc autoconfiguration；base starter保留仅在真正RPC consumer。
- Error and edge behavior: WebFlux POM出现 MVC或HTTP POM出现WebFlux即 fail dependency gate；无 UI/parser。
- Standards impact: `MC-DEP-001, MC-REUSE-001, MC-SCOPE-001`。
- Literal rule enforcement: `Rule 5` dependency allowlist；`Rule 11` consumer→adapter→common→contract方向。
- Implementation pseudocode:

```xml
replace gateway-starter HTTP usage with gateway-starter-openapi-webmvc in servlet providers
replace gateway-starter HTTP usage with gateway-starter-openapi-webflux in reactive provider
preserve unrelated IdP/MCP/runtime test dependencies and exclude swagger UI/parser
verify each provider dependency tree contains exactly its selected Web stack
```

- Verification contribution: tests可编译新 annotations；dependency tree隔离。
- After this file: POM完成，旧 controller imports仍使契约RED。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/src/main/resources/application.yml`

- Purpose: 配置不同业务系统的显式 groups/Manifest；同批修改 WebFlux、IdP、MCP Provider application.yml。
- Symbols: `gateway.openapi.*`、standard `springdoc.group-configs`；HTTP two groups，其他one group。
- Repository evidence: 四个 application.yml存在且是Provider运行配置；amendment §15.1固定 keys/default。
- Dependencies and consumers: GatewayOpenApiProperties、Springdoc GroupedOpenApi、DDC contributor、contract tests。
- Why now: controller annotations必须与已验证的 group/build config交叉匹配。
- Contract/signature changes: HTTP groups=`orders,inventory`；WebFlux=`inventory-reactive`；IdP=`identity`；MCP=`jobs`；resource/artifact/build值按 fixture稳定。
- Input/output and state mapping: YAML→properties→group resources/DDC keys；排序由properties处理。
- Error and edge behavior: 每份配置不含 token/secret；base/local若 module有profile必须同键；未发布group不注册。
- Standards impact: `MC-VALID-001, MC-CONFIG-001, MC-JSON-001`。
- Literal rule enforcement: `Rule 2` typed config校验；`Rule 7`同module所有环境键一致；`Rule 11`配置归属各Provider。
- Implementation pseudocode:

```yaml
declare gateway.openapi enabled, published-groups, resource-uri, artifact-version and build-id
declare springdoc group-configs whose names exactly equal the published groups and whose paths do not overlap
keep the same key structure in every existing environment profile while allowing environment-specific values
never place bearer tokens, document bodies or arbitrary fetch URLs in configuration or DDC metadata
```

- Verification contribution: config binding/group endpoint tests GREEN prerequisite。
- After this file: groups注册，但 controllers仍需标准注解映射。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/src/main/java/top/egon/cola/component/gateway/test/http/OrderController.java`

- Purpose: 机械迁移所有仓内 Provider controllers到标准 Swagger + Egon annotations。
- Symbols: 同批精确文件：HTTP `BehaviorController,InventoryController,OrderController,ProviderIdentityController`；WebFlux `ProviderIdentityController,ReactiveInventoryController,StreamingTransportController`；IdP `MockBackendController`；MCP `McpJobController`。
- Repository evidence: repository scan显示这些是全部 test-provider旧 HTTP annotation consumers；RPC contract `EchoRpc/OrderRpc`不在本批。
- Dependencies and consumers: Provider routes保持不变；Springdoc扫描；Step 4 Golden；Step 6 residual deletion。
- Why now: POM/config已切换，逐文件替换不会改变业务 handler逻辑。
- Contract/signature changes: class `@Tag`/Egon catalog；methods standard `@Operation(operationId=...)`/responses/schema + policy/optional mcp；DTO fields用Swagger `@Schema`仅在wire类型。
- Input/output and state mapping: existing route/request/response →同义 OpenAPI paths/components；operationId使用 application-domain-action convention；group由Springdoc path selectors，不作为目录 domain。
- Error and edge behavior: streaming/multipart/media types保持标准描述；普通 operations无MCP；重叠path导致Golden/global uniqueness fail。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-SCOPE-001`；只改contract metadata，不重构 handler业务。
- Literal rule enforcement: `Rule 1`既有 controller/DTO命名保持；`Rule 2`保留现有 request validation并添加schema约束；`Rule 6`Swagger/Jackson wire annotations按需；`Rule 11`不移动文件/层。
- Implementation pseudocode:

```java
for each exact provider controller remove legacy Gateway HTTP/schema annotation imports
retain route, request validation and implementation; add standard Tag, Operation, ApiResponse and Schema metadata
add EgonApiCatalog and EgonGatewayPolicy; add EgonMcpTool only to the explicitly managed MCP job operation
assign globally unique stable operationId values and verify each operation appears in exactly one published group Golden
```

- Verification contribution: all provider Golden tests GREEN；residual scan为Step 4 scope零。
- After this file: test Providers完全使用新 adapters；base starter旧HTTP代码仅剩Admin/Starter自身待清理。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-webflux-http-provider,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-tianquan-shoubing-backend,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-mcp-provider -am test && ! rg 'gateway\.starter\.annotation\.(EgonHttpService|GatewayRequest|GatewayResponse|GatewaySchema)' egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/{yuheng-test-http-provider,yuheng-test-webflux-http-provider,yuheng-test-tianquan-shoubing-backend,yuheng-test-mcp-provider}`
- Expected result: 四个 Provider modules tests pass；per-group Golden稳定；旧 HTTP/schema annotation在这些 modules为零；RPC tests未改。
- Failure returns to: File 1 Golden contract；File 2 classpath；File 3 group/config；File 4 controller mapping。
- Completion criteria: 不同业务系统显式groups、HTTP双group、optional MCP、DDC Manifest与3.1文档都有仓内 consumer proof。
- Rollback: revert四个Provider module paths与fixtures；不影响Step 1–3 adapter实现。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/src/test/java/top/egon/cola/component/gateway/test/http/HttpProviderContractTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/pom.xml`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/src/main/resources/application.yml`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/src/main/java/top/egon/cola/component/gateway/test/http/OrderController.java`, all exact provider POM/config/controller/test/Golden paths listed in File 1–4
- Commit: `test(gateway-openapi): migrate provider fixtures to explicit groups`

### Step 5 — 迁移 Gateway Admin 自身 HTTP 文档契约到标准 OpenAPI

- Requirements: `REQ-001, REQ-002, REQ-009, REQ-015, REQ-022`
- Dependencies: `Step 3`
- Baseline state: Admin POM已具 MVC adapter，但20个 controllers与跨 catalog/mcp/routing 等 wire carriers仍引用旧 Gateway HTTP/schema annotations。
- Observable outcome: Admin grouped document由标准 Springdoc annotations与Egon extensions生成；所有现有 routes/validation/authorization/business logic保持。
- End state: Admin旧 HTTP/schema annotation consumer为零；base starter的旧types可以在Step 6安全删除。
- Test-first gate: Required — 新建 Admin grouped OpenAPI contract test并先要求稳定 operationId、schema、extension与零旧注解；迁移前RED。
- Manual Checks: `MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/GatewayAdminOpenApiContractTest.java`

- Purpose: 以一个 focused Golden/structural test覆盖 Admin全部 controller operations与旧 annotation residual。
- Symbols: `GatewayAdminOpenApiContractTest`，断言 `/v3/api-docs/gateway-admin`、operationId唯一、x-egon-service、standard schemas、现有安全 scope。
- Repository evidence: Admin已有configuration/controller tests；repository scan明确20个 controller consumer和fully-qualified `GatewaySchemaField` carriers。
- Dependencies and consumers: Admin MVC adapter、全部 controllers/DTO/VO；Step 6 deletion gate。
- Why now: 大批机械迁移必须由一份可复现契约防止漏项/改业务。
- Contract/signature changes: 文档metadata变化，HTTP endpoint/JSON runtime contract本身不改变；external DTO/VO才用Swagger `@Schema`，PO/internal record去除文档注解。
- Input/output and state mapping: current mappings + validation annotations → grouped OpenAPI；operationId list→unique set；source group/build→root extension。
- Error and edge behavior: duplicate/missing operationId、旧annotation import/FQCN、internal PO暴露、security失效均测试失败；不把Raw OpenAPI内容写日志。
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001`；mechanical contract migration，不改稳定DI。
- Literal rule enforcement: `Rule 2` 保持每个 request boundary validation；`Rule 6` standard Swagger/Jackson契约；`Rule 11`文件不移动且仅Admin feature profile。
- Implementation pseudocode:

```java
start the Admin MVC slice with gateway-admin published group and authorized documentation request
parse the OpenAPI document and assert every mapped controller method has a unique explicit operationId
assert external DTO and VO schemas remain complete while persistence PO types are not exposed as public schemas
scan Admin main sources and fail on any legacy Gateway HTTP/schema annotation import or fully-qualified reference
```

- Verification contribution: 迁移前因旧annotation/residual或缺standard docs RED；完成后作为Step 6删除前置。
- After this file: Admin最终文档契约被冻结。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/application/controller/GatewayApplicationController.java`

- Purpose: 机械迁移全部 Admin controllers；此 heading代表下列精确同类文件，均只改文档annotations/imports，除 `GatewayAdminExceptionHandler` 在Step 12再改错误映射外不改逻辑。
- Symbols: exact controller set=`GatewayApplicationController,GatewayAuthBootstrapController,GatewayCatalogController,GatewayCredentialController,GatewayGroupController,McpAppAdminController,McpApprovalController,McpCapabilityController,McpProtocolInspectorController,McpRemoteProviderController,McpServerController,McpTaskAdminController,McpToolAdminController,GatewayObservabilityController,GatewayReleaseController,GatewayDefinitionReportController,GatewayDraftController,GatewayProjectionController,GatewayScopeController,GatewayAdminExceptionHandler`。
- Repository evidence: `rg` scan给出全部旧 annotation imports；routes/services/tests均已存在。
- Dependencies and consumers: existing service collaborators/security/request-response carriers；Springdoc customizers；Admin UI/API clients。
- Why now: File 1已锁定route-equivalent output；批次必须一次清零以允许Step 6编译删除。
- Contract/signature changes: `GatewayInterfaceGroup`→standard `@Tag`/`EgonApiCatalog`；HTTP `GatewayOperation`→`@Operation(operationId,summary,description)` + `EgonGatewayPolicy`/按需MCP；response annotations使用Swagger标准。
- Input/output and state mapping: every existing request mapping/validation/security annotation remains byte-for-byte semantically equal；only documentation metadata maps to OpenAPI operation/root extensions。
- Error and edge behavior: exception handler的现有HTTP mapping保持；没有MCP的operation不添加Tool；controller constructor/DI按`PLAN-CLAR-004`不重写。
- Standards impact: `MC-ARCH-001, MC-VALID-001, MC-JSON-001, MC-BEAN-001, MC-SCOPE-001`；metadata-only change，stable DI保留。
- Literal rule enforcement: `Rule 1` controller命名/层不变；`Rule 2`现有 `@Valid/@Validated`不丢；`Rule 4`新业务逻辑N/A且现有DI不因metadata机械重写；`Rule 6`标准OpenAPI/Jackson；`Rule 11`feature-first位置不变。
- Implementation pseudocode:

```java
for each exact Admin controller preserve mappings, authorization, validation, collaborators and return types
replace legacy interface group and HTTP operation annotations with standard Tag and Operation metadata
attach Egon catalog and policy annotations with stable globally unique operationId values
add MCP annotation only where the existing managed-tool contract requires it; do not create new tools or routes
```

- Verification contribution: File 1覆盖所有operation IDs与root extensions；现有controller tests证明业务未变。
- After this file: controller旧 HTTP annotations为零；wire carriers仍待迁移。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/catalog/domain/dto/GatewayManualDefinitionDTO.java`

- Purpose: 迁移所有fully-qualified旧 schema annotations；external DTO/VO用Swagger `@Schema`，PO/internal类型直接删除文档metadata。
- Symbols: exact catalog files=`GatewayManualDefinitionDTO,GatewayManualDefinitionRequestDTO,GatewayOperationDefinitionPO,GatewayOperationPO,GatewayCurrentOperationDefinitionVO,GatewayEntityNodeVO,GatewayInterfaceGroupNodeVO,GatewayInterfaceGroupScopeVO,GatewayOperationDetailVO,GatewayOperationNodeVO`；mcp files=`McpApprovalRequestDTO,McpCapabilityMutationDTO,McpCapabilityRequestDTO,McpProtocolInspectRequestDTO,McpRemoteMountMutationDTO,McpRemoteMountRequestDTO,McpRemoteProviderMutationDTO,McpRemoteProviderRequestDTO,McpRemoteToolMutationDTO,McpRemoteToolRequestDTO,McpCapabilityRecordPO,McpRemoteCapabilityPO,McpRemoteMountDraftPO,McpRemoteProviderDraftPO,McpRemoteToolDraftPO,McpTaskPO,McpManagedToolVO,McpProtocolInspectionVO,McpRemoteToolVO`；observability=`GatewayKafkaConsumerSettingsDTO,GatewayAuditVO`；release=`GatewayReleasePO,GatewayReleaseVO`；routing=`GatewayDraftPolicyRequestDTO,GatewayDraftRouteRequestDTO,GatewayPolicyMutationDTO,GatewayRouteMutationDTO,GatewayPolicyDraftPO,GatewayRouteDraftPO`；shared=`IdempotencyPO`。
- Repository evidence: these exact files contain fully-qualified `gateway.starter.annotation.GatewaySchemaField` found by scan；records/classes and Jackson shapes already exist。
- Dependencies and consumers: controllers/repositories/services/frontend；only DTO/VO are public schema consumers。
- Why now: controllers已切换，清除carrier引用后旧 annotation types才能删除。
- Contract/signature changes: no Java field/type/JSON name change；`allowArbitraryJson` maps to Swagger free-form object schema only at external DTO/VO；PO fields get no Swagger annotation。
- Input/output and state mapping: existing record/class fields→same JSON/JDBC mapping；documentation annotations do not alter null/default/collection semantics。
- Error and edge behavior: request validation/Jackson annotations保留；Map<String,Object> wire fields明确 additionalProperties；internal PO不泄露；no constructor/model refactor。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-SCOPE-001`；现有representation保持，按boundary应用Schema。
- Literal rule enforcement: `Rule 1` DTO/PO/VO后缀保持；`Rule 2` validation annotation不丢；`Rule 3` records/现有复杂class不借机重构；`Rule 6` wire DTO/VO按需Swagger/Jackson，PO无外部annotation；`Rule 11`无移层。
- Implementation pseudocode:

```java
for each exact carrier classify it as external DTO or VO versus internal PO
remove every fully-qualified legacy GatewaySchemaField reference without changing fields, constructors or persistence mapping
on external JSON carriers add standard Schema metadata including additionalProperties for free-form maps
on PO/internal carriers add no OpenAPI annotation; retain Jakarta Validation and Jackson annotations exactly where already required
```

- Verification contribution: Admin contract/residual tests GREEN；repository/service regressions证明模型未变。
- After this file: Admin main source旧 HTTP/schema annotation引用为零。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=GatewayAdminOpenApiContractTest,GatewayAdminConfigurationTest,GatewayCatalogServiceTest,GatewayReleaseServiceTest,GatewayDraftServiceTest,McpReleaseContentFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test && ! rg 'top\.egon\.cola\.component\.gateway\.starter\.annotation\.(EgonHttpService|GatewayRequestLocation|GatewayRequestSchemaField|GatewayResponseSchema|GatewaySchemaField|GatewaySchemaRequired|GatewaySchemaShape|GatewaySchemaType)' egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main`
- Expected result: named tests exit 0；Admin grouped doc完整；旧 HTTP/schema annotation residual为零；business tests不回归。
- Failure returns to: File 1 contract coverage；File 2 controller metadata；File 3 model boundary classification。
- Completion criteria: 20 controllers与全部列明carriers迁移；routes/JSON/validation/DI无行为变化；可进入旧Starter删除。
- Rollback: path-limited revert Admin controller/carrier/test files；不回滚adapter modules。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/GatewayAdminOpenApiContractTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/application/controller/GatewayApplicationController.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/catalog/domain/dto/GatewayManualDefinitionDTO.java`, every exact controller/carrier path listed in File 2/3
- Commit: `refactor(gateway-admin): migrate http documentation to openapi`

### Step 6 — 将 Base Starter 收缩为 RPC Descriptor-only 并删除旧 HTTP Compiler

- Requirements: `REQ-003, REQ-009, REQ-010, REQ-020`
- Dependencies: `Steps 4–5`
- Baseline state: 所有仓内HTTP consumers已迁移；base starter仍含8个HTTP schema annotations、EgonHttpService、3个HTTP mapper/validator、2个contributors、Java mapper、旧HTTP tests和optional web dependencies。
- Observable outcome: base starter只产生RPC Descriptor Report，source=`RPC_DESCRIPTOR`；旧HTTP类/测试/依赖彻底删除，RPC/MCP schema Golden保持。
- End state: HTTP事实源只有Springdoc adapter；RPC事实源仍是Protobuf Descriptor；contract report sourceType使用enum的编译链闭合。
- Test-first gate: Required — 先新增/修改 RPC-only structural test、Report serialization与RPC/MCP Golden；在旧classes/旧source string存在时RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 5, Rule 6, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/test/java/top/egon/cola/component/gateway/starter/GatewayStarterRpcOnlyContractTest.java`

- Purpose: 用structural test锁定base starter零Springdoc/零HTTP compiler、RPC source/protobuf schema不变；同批修改 `RpcGatewayDefinitionContributorTest,ProtobufSchemaMapperTest,GatewayOperationSemanticsTest,McpExposureMapperTest,GatewayReportingAutoConfigurationTest` 和 contract report tests。
- Symbols: `GatewayStarterRpcOnlyContractTest`、enum serialization assertions、RPC Descriptor Golden。
- Repository evidence: starter现有 tests覆盖HTTP/RPC混合；RPC contract fixture `EchoRpc/OrderRpc`与test-suite Mcp fixture存在。
- Dependencies and consumers: base starter/contract/Admin report consumer；Step 10 shared ingestion。
- Why now: 删除前先证明保留边界和预期RED residual。
- Contract/signature changes: `GatewayInterfaceDefinitionReport.InterfaceGroup.sourceType` String→`GatewayDefinitionSourceTypeEnum`；RPC exact `RPC_DESCRIPTOR`；HTTP source不再从starter产生。
- Input/output and state mapping: protobuf descriptor→same operation/schema/report fields except typed source；Jackson enum wire name保持string。
- Error and edge behavior: invalid protocol/source pairing测试拒绝；MCP exposure只读取保留RPC annotation语义；no HTTP classes on classpath。
- Standards impact: `MC-DEP-001, MC-VALID-001, MC-JSON-001, MC-PATTERN-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 1` enum语义；`Rule 2` report boundary校验；`Rule 5` dependency residual；`Rule 6` enum JSON；`Rule 9` RPC contributor/protobuf mapper职责分离；`Rule 11` base starter保持RPC adapter职责。
- Implementation pseudocode:

```java
build RPC contract from the existing protobuf descriptor fixture and compare unchanged request/response invocation schema Golden
assert each interface group sourceType serializes as RPC_DESCRIPTOR and protocol is RPC
scan starter production class names and dependencies; fail if any HTTP contributor, Java schema mapper, Springdoc, MVC or WebFlux remains
assert MCP exposure for RPC retains stable tool identity and no HTTP schema annotation member is required
```

- Verification contribution: 旧classes存在/source string时RED；清理后RPC回归与structural GREEN。
- After this file: 保留/删除边界被冻结。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/main/java/top/egon/cola/component/gateway/contract/reporting/GatewayInterfaceDefinitionReport.java`

- Purpose: 将shared Report sourceType从String升级为三值enum并同步全部编译consumer。
- Symbols: `InterfaceGroup.sourceType` typed enum；同批修改 report canonicalizer/serialization tests、Starter report factory和Admin现有report service/repository/test的类型边界。
- Repository evidence: amendment AM-EVD-005显示当前String/STARTER/MANUAL硬编码；enum已在Step 1创建。
- Dependencies and consumers: Starter RPC producer、Admin reporting/ingestion、MCP release、frontend source filter（Step13）。
- Why now: HTTP consumers已迁移，RPC-only test已固定；公共破坏性contract可一次闭合所有Java consumer。
- Contract/signature changes: only accepted values三种；protocol/source配对验证在producer/ingestion；database仍以enum name varchar存储fresh DB。
- Input/output and state mapping: enum→Jackson name/JDBC string；legacy STARTER不映射/不兼容，符合fresh DB破坏性决定。
- Error and edge behavior: null/unknown/HTTP+RPC_DESCRIPTOR/RPC+OPENAPI31拒绝；MANUAL按现有手工protocol约束。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1` enum后缀；`Rule 2` source/protocol boundary validation；`Rule 6` Jackson enum；`Rule 11` contract无依赖反转。
- Implementation pseudocode:

```java
replace report InterfaceGroup sourceType String with GatewayDefinitionSourceTypeEnum
update canonical serialization and all Java call sites to use enum constants rather than string literals
validate protocol HTTP accepts OPENAPI31 or approved MANUAL and protocol RPC accepts RPC_DESCRIPTOR
reject STARTER and unknown values without compatibility translation because fresh database cutover is approved
```

- Verification contribution: contract/starter/Admin focused compile与serialization tests。
- After this file: typed source vocabulary贯穿Java consumer；旧HTTP classes尚待删除。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/rpc/RpcGatewayDefinitionContributor.java`

- Purpose: 保留RPC-only annotation/governance/protobuf path并删除HTTP schema declarations依赖。
- Symbols: `RpcGatewayDefinitionContributor` source enum；`GatewayInterfaceGroup,GatewayOperation,GatewayOperationSemantics,McpExposureMapper` RPC-only contracts；`GatewayRequestParameter`按真实RPC使用决定删除或去HTTP成员。
- Repository evidence: contributor使用ProtobufSchemaMapper；GatewayOperation/Group当前兼含HTTP字段；McpExposureMapper被RPC consumer调用。
- Dependencies and consumers: RPC contract/provider tests、report factory/Admin ingest。
- Why now: typed source contract已闭合，先调整保留代码再物理删除依赖types。
- Contract/signature changes: GatewayOperation移除HTTP request/response schema members，仅保留RPC governance/MCP所需字段；contributor writes `RPC_DESCRIPTOR`。
- Input/output and state mapping: Descriptor+RPC annotations→unchanged Report v2 operation/schema；source typed；MCP optional rules不变。
- Error and edge behavior: missing descriptor/schema依旧fail；不回退Java reflection schema；no HTTP path mapping。
- Standards impact: `MC-VALID-001, MC-PATTERN-001, MC-SCOPE-001`；保持Protobuf Adapter而非统一伪造。
- Literal rule enforcement: `Rule 2` contributor output validation；`Rule 9` Protobuf adapter保持独立；`Rule 11` RPC discovery package不混HTTP。
- Implementation pseudocode:

```java
read RPC catalog descriptor and retained RPC governance annotations
map input and output descriptors through ProtobufSchemaMapper into Invocation Schema v2
emit interface groups with sourceType RPC_DESCRIPTOR and protocol RPC
remove every reference to Java HTTP request/response annotations and never fall back to reflection schema
```

- Verification contribution: RPC contributor/MCP exposure/protobuf Golden tests GREEN。
- After this file: 所有保留starter代码不再依赖待删HTTP types。

#### File 4 — `DELETE egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/http/GatewayHttpOperationMapper.java`

- Purpose: 物理删除完整HTTP compiler surface、annotations、tests与web dependencies。
- Symbols: 同批 DELETE main=`discovery/http/GatewayHttpOperationMapper,GatewayRequestSchemaValidator,GatewayResponseSchemaMapper,MvcGatewayDefinitionContributor,WebFluxGatewayDefinitionContributor,package-info`；`discovery/schema/GatewayJavaSchemaMapper`；annotations=`EgonHttpService,GatewayRequestLocation,GatewayRequestSchemaField,GatewayResponseSchema,GatewaySchemaField,GatewaySchemaRequired,GatewaySchemaShape,GatewaySchemaType`；无consumer时`GatewayRequestParameter`；tests=`GatewaySchemaAnnotationContractTest,GatewayHttpOperationMapperTest,GatewayHttpServiceNameTest,GatewayJavaSchemaMapperTest,GatewayResponseSchemaMapperTest`。MODIFY starter `pom.xml,GatewayReportingAutoConfiguration,AutoConfiguration.imports`。
- Repository evidence: Step 4/5 residual为零；target Spec §8.2精确要求删除；base POM optional MVC/WebFlux仅为旧contributors。
- Dependencies and consumers: none after prior files；RPC autoconfig/reporting remains。
- Why now: 删除置于所有consumer迁移和保留代码改造之后，确保 commit可编译。
- Contract/signature changes: breaking removal，无兼容shim/deprecation；starter不再生成HTTP Report。
- Input/output and state mapping: HTTP handler metadata不再进入base starter；Provider HTTP通过Springdoc adapters；RPC path不变。
- Error and edge behavior: residual source/import/test/POM依赖使static gate失败；不删除Protobuf mapper/RPC annotations/report transport。
- Standards impact: `MC-ARCH-001, MC-DEP-001, MC-SCOPE-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 5`删除不再需要的Web utilities/dependencies；`Rule 11`形成清晰RPC base与HTTP adapter modules。
- Implementation pseudocode:

```text
delete the exact HTTP discovery package, Java schema mapper, eight HTTP schema annotations and five obsolete tests
remove MVC and WebFlux optional dependencies and HTTP auto-configuration imports from the base starter POM/resources
retain GatewayInterfaceGroup, GatewayOperation, ProtobufSchemaMapper, RPC contributor and signed reporting transport
run a repository-wide residual scan and fail if deleted symbols remain outside historical Specs/Plans
```

- Verification contribution: RPC-only structural test、dependency tree、repository residual scan。
- After this file: 第二HTTP类型系统彻底消失；base starter是RPC-only。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract,egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter,egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-rpc-contract -am test && ! rg 'EgonHttpService|GatewayRequestLocation|GatewayRequestSchemaField|GatewayResponseSchema|GatewaySchemaField|GatewaySchemaRequired|GatewaySchemaShape|GatewaySchemaType|MvcGatewayDefinitionContributor|WebFluxGatewayDefinitionContributor|GatewayJavaSchemaMapper' egon-cola-xingyuan/egon-cola-yuheng --glob '!docs/**' && ./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter dependency:tree -Dincludes=org.springdoc:*,org.springframework:spring-webmvc,org.springframework:spring-webflux`
- Expected result: contract/starter/RPC tests pass；residual零；base dependency tree无Springdoc/MVC/WebFlux；RPC Golden/Tool ID不变。
- Failure returns to: File 1保留契约；File 2 source consumer；File 3 RPC mapping；File 4遗漏删除/依赖。
- Completion criteria: REQ-009完全满足且REQ-010有focused proof；无compatibility shim。
- Rollback: revert整个Step 6 path set；不能只恢复单个旧annotation造成半兼容状态。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/test/java/top/egon/cola/component/gateway/starter/GatewayStarterRpcOnlyContractTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/main/java/top/egon/cola/component/gateway/contract/reporting/GatewayInterfaceDefinitionReport.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/rpc/RpcGatewayDefinitionContributor.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/http/GatewayHttpOperationMapper.java`, all exact source/test/POM/resource consumers/deletions in File 1–4
- Commit: `refactor(gateway-starter): remove legacy http schema compiler`

### Step 7 — 新增唯一 V12 与多 Group Snapshot/Sync CAS 持久边界

- Requirements: `REQ-006, REQ-007, REQ-011, REQ-013, REQ-018, REQ-019, REQ-024`
- Dependencies: `Step 1`
- Baseline state: Admin migrations为V1–V11；definition_set按application/build/protocol/fingerprint唯一；无snapshot/sync表与repositories；schema test期望11 migrations。
- Observable outcome: fresh PostgreSQL经V1–V12创建两表；每group immutable snapshots，多group可链接同一Definition Set；sync key唯一且revision CAS/state transitions可测。
- End state: 数据层可独立保存Raw与状态；不做HTTP fetch/adapter/ingest orchestration。
- Test-first gate: Required — 先修改schema count并新增PostgreSQL migration/repository tests；因V12/tables/repos缺失RED。
- Manual Checks: `MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 6, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/persistence/GatewayAdminSchemaTest.java`

- Purpose: 将Flyway期望从11锁到12并新增真实PostgreSQL DDL/CAS/多对一测试。
- Symbols: `GatewayAdminSchemaTest`、`GatewayOpenApiFlywayPostgresqlIT`、`JdbcGatewayOpenApiSnapshotRepositoryTest`、`JdbcGatewayOpenApiSyncRepositoryTest`。
- Repository evidence: schema test当前断言11；已有GatewayMcpFlywayPostgresqlIT可复用Testcontainers/JdbcTemplate风格。
- Dependencies and consumers: V12、PO/repository；Step 10 transaction。
- Why now: migration/DAO必须先有失败证据，且旧checksum保护显式化。
- Contract/signature changes: assert exactly V12 added；snapshot definition_set_id nonunique index；sync unique `(application_id,build_id,openapi_group)`；FK/JSONB/TIMESTAMPTZ/checks/CAS affected-row。
- Input/output and state mapping: fixture rows→DDL constraints；revision expected→update one row/new revision；same definitionSetId linked from orders+inventory。
- Error and edge behavior: duplicate snapshot identity/hash semantics、duplicate sync key、illegal state、stale revision、missing FK、oversized fields fail；V1–V11 checksum unchanged。
- Standards impact: `MC-VALID-001, MC-MODEL-001, MC-TIME-001, MC-PATTERN-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2` repository boundary constraints；`Rule 3` PO record expectations；`Rule 9` CAS并发模式；`Rule 10` TIMESTAMPTZ↔Instant；`Rule 11` infrastructure tests在既有Admin分层。
- Implementation pseudocode:

```java
migrate an empty PostgreSQL container from V1 through V12 and assert exactly twelve successful versions
inspect both tables, foreign keys, checks and indexes; assert snapshot definition_set_id is not unique
insert orders and inventory snapshots linked to one definition set and one sync row per group
perform CAS with expected revision, assert one affected row; repeat stale revision and assert zero without state corruption
```

- Verification contribution: RED由缺V12/tables/repos；GREEN证明fresh DB与真实PostgreSQL语义。
- After this file: persistence acceptance被测试冻结。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/resources/db/migration/V12__add_gateway_openapi_sync.sql`

- Purpose: 唯一新增migration创建`gateway_openapi_snapshot`与`gateway_openapi_sync_state`。
- Symbols: 两表、constraints、FK、unique/indexes；不修改V1–V11。
- Repository evidence: primary/amendment §11给出逐列设计；existing migration命名V1–V11。
- Dependencies and consumers: definition_set/application既有tables；JDBC repos/query API。
- Why now: tests已定义DDL，PO/repository实现前schema必须稳定。
- Contract/signature changes: snapshot per group/raw JSON/hash/status/provenance/nullable definition set；sync per app/build/group state/current refs/revision/retry/error/timestamps。
- Input/output and state mapping: OpenAPI raw JSON→JSONB或Spec指定列；SHA lowercase hex；Instant→TIMESTAMPTZ；one definition set referenced by many snapshots；sync unique key。
- Error and edge behavior: no backfill/default legacy values；constraints拒绝invalid hashes/state/revision；on delete behavior按Spec保留immutable provenance；rollback采用删库重建或forward-fix V13，绝不改V12已发布内容。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-TIME-001, MC-SCOPE-001`；PostgreSQL-native constraints。
- Literal rule enforcement: `Rule 1` table映射PO命名；`Rule 2` DB作为最终boundary约束；`Rule 10`只TIMESTAMPTZ；`Rule 11` migration位于`classpath:db/migration`既有结构。
- Implementation pseudocode:

```sql
create gateway_openapi_snapshot with immutable identity, application/build/group, raw document, document and canonical hashes, status, provenance and nullable definition_set_id foreign key
create nonunique index on snapshot.definition_set_id plus lookup/uniqueness indexes required by exact group/hash identity
create gateway_openapi_sync_state with unique application_id, build_id, openapi_group and revision-based mutable state fields
add checks for enum states, nonnegative revision/attempt, lowercase sha length and timestamp/error bounds; do not backfill or alter V1 through V11
```

- Verification contribution: schema/IT检查exact DDL、多对一和checksum。
- After this file: fresh DB具备持久结构；Java mapping尚缺。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/po/GatewayOpenApiSnapshotPO.java`

- Purpose: 创建simple immutable persistence records与state enum/key。
- Symbols: `GatewayOpenApiSnapshotPO`、`GatewayOpenApiSyncPO`、`GatewayOpenApiSyncStateEnum`、`GatewayOpenApiSyncKeyDTO`。
- Repository evidence: Admin repository使用records/Jdbc row mapping；Spec §10/§11给出字段和state machine。
- Dependencies and consumers: JDBC repos/services；Jackson only if API boundary N/A；java.time Instant。
- Why now: schema稳定后建立一对一列mapping。
- Contract/signature changes: records完整列字段；compact constructors trim/copy/validate；state enum exact transitions。
- Input/output and state mapping: JDBC varchar/json/time/null→typed values；revision long；nullable current refs按state guard。
- Error and edge behavior: invalid hash/state/time/revision/null组合构造失败；byte/raw content不复制到logs；no java.util.Date。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001`。
- Literal rule enforcement: `Rule 1` PO/DTO/Enum；`Rule 2` compact boundary validation；`Rule 3` simple records；`Rule 6` Raw JSON保持Jackson形态；`Rule 10` Instant；`Rule 11` domain子包。
- Implementation pseudocode:

```java
define records whose components map every V12 column with exact nullability and java.time Instant values
in compact constructors trim identifiers, copy raw structures defensively and validate hashes, revision and state-dependent references
define only the approved sync states and expose transition predicates used by repository/service tests
reject invalid state/reference combinations before invoking JdbcTemplate
```

- Verification contribution: constructor/state tests + repository row mapping。
- After this file: typed persistence model可用，尚无DAO。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/repository/jdbc/JdbcGatewayOpenApiSnapshotRepository.java`

- Purpose: 创建repository interfaces与Jdbc implementations，支持immutable upsert/find/link和sync CAS/readiness。
- Symbols: `GatewayOpenApiSnapshotRepository`,`GatewayOpenApiSyncRepository`,`JdbcGatewayOpenApiSnapshotRepository`,`JdbcGatewayOpenApiSyncRepository`；显式Bean names。
- Repository evidence: existing JdbcGatewayDefinitionReportRepository使用JdbcTemplate/affected-row校验；Spec要求一row/group与linked-set repair查询。
- Dependencies and consumers: PO/DTO、JdbcTemplate、Step10 ingestion、Step11 sync、Step12 query。
- Why now: tests/schema/model均完成，可实现最小 persistence ports。
- Contract/signature changes: snapshot insert/reuse/findByBuildGroups/linkAllToDefinitionSet；sync discover/claim/transition/CAS/setValid/listManifest/read current links。
- Input/output and state mapping: typed values→prepared SQL；list按group排序；CAS returns boolean/affected row；snapshot immutable conflicts分类。
- Error and edge behavior: duplicate same hash idempotent；same key different raw/canonical conflict；stale revision false；partial link transaction由caller rollback；SQL exceptions保持分类。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-TIME-001, MC-PATTERN-001`；repository不拥有业务编排。
- Literal rule enforcement: `Rule 2` only validated types/affected-row asserts；`Rule 4` named Beans/@Slf4j/RequiredArgsConstructor/final JdbcTemplate Qualifier；`Rule 9` CAS pattern；`Rule 10` Instant；`Rule 11` repository/jdbc direction。
- Implementation pseudocode:

```java
insert or reuse immutable snapshot by application, build, group and hashes; compare all immutable fields on conflict
query exact manifest rows ordered by group and verify no missing or extra group before returning
link every supplied snapshot id to one definition set inside caller transaction and assert affected count equals list size
claim and transition sync rows with where revision = expectedRevision, increment revision and return false on stale owner
```

- Verification contribution: JDBC tests/real PostgreSQL IT GREEN。
- After this file: V12 persistence boundary完整，尚无network/business pipeline。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=GatewayAdminSchemaTest,GatewayOpenApiFlywayPostgresqlIT,JdbcGatewayOpenApiSnapshotRepositoryTest,JdbcGatewayOpenApiSyncRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: unit/schema tests pass；Docker可用时PostgreSQL IT pass；V1–V11未修改且snapshot多对一/CAS语义正确。
- Failure returns to: File 1 acceptance；File 2 DDL；File 3 mapping；File 4 SQL/CAS。
- Completion criteria: exactly one V12、two tables、all constraints/indexes、多Group→one set link与revision CAS有proof。
- Rollback: pre-release可删库重建并revert Step 7；migration一旦发布只能forward-fix新version，绝不编辑V12。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/persistence/GatewayAdminSchemaTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/resources/db/migration/V12__add_gateway_openapi_sync.sql`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/po/GatewayOpenApiSnapshotPO.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/repository/jdbc/JdbcGatewayOpenApiSnapshotRepository.java`, same feature PO/DTO/Enum/repository/tests
- Commit: `feat(gateway-admin): add openapi snapshot and sync persistence`

### Step 8 — 实现受信 DDC Candidate、OAuth2/SSRF 安全拉取与 Validation Chain

- Requirements: `REQ-004, REQ-005, REQ-006, REQ-017, REQ-021, REQ-025`
- Dependencies: `Step 7`
- Baseline state: snapshot/sync repositories可用；Admin已有DDC projection与IdP token supplier；无OpenAPI client/validation pipeline。
- Observable outcome: Admin只从健康DDC metadata派生candidate，按Manifest逐group安全获取≤5MiB JSON并通过六规则Chain；任何SSRF/ref/size/spec/extension失败不进入Adapter/Ingestion。
- End state: 输出validated `GatewayOpenApiDocumentDTO`或分类失败；不写Definition、不改sync VALID。
- Test-first gate: Required — 先写client hostile-target与六rule tests；在client/rules缺失时RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/client/GatewayProviderOpenApiClientTest.java`

- Purpose: 固定OAuth2/SSRF/timeout/size/no-redirect安全RED；同批创建Validation Chain与六rule tests。
- Symbols: client tests；`GatewayOpenApiValidationChainTest`；DocumentEnvelope/OpenApi31/Reference/Limit/EgonExtension/McpProjection validator tests。
- Repository evidence: Spec TEST-003/007–010/114；JDK HttpClient决策；DDC/IdP现有fixtures可mock。
- Dependencies and consumers: candidate/document DTOs、client/rules；Step 9 Adapter。
- Why now: 安全边界必须在网络实现前确定且fail closed。
- Contract/signature changes: candidate只能由DDC data构造；token audience=resourceUri/scope；HTTPS/no redirect；DNS两次全部地址CIDR；content-type/status/size/JSON/ref/extensions。
- Input/output and state mapping: healthy instance+Manifest group→derived URI→bytes+hash+model；failure→safe code/message≤1024，无body/token。
- Error and edge behavior: userInfo/query/fragment/path traversal、private/denied CIDR、mixed DNS answers、DNS rebinding、redirect、401、nonJSON、>5MiB、external ref、MCP invalid均明确断言无ingest/write。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2`每个handoff validate；`Rule 4`安全日志不含secret/body；`Rule 5`JDK HttpClient；`Rule 6`Jackson；`Rule 9`Chain；`Rule 10`Duration/Clock；`Rule 11`client/validation layers。
- Implementation pseudocode:

```java
derive candidate only from a healthy non-expired DDC instance and validated manifest, never from request URL
mock trusted DNS with all-allowed, mixed, rebinding and denied address sets; assert checks occur before request and before body acceptance
mock token supplier and HTTP responses for redirect, auth failure, timeout, content type and streaming size overflow
run the ordered six-rule chain and assert first failure classification, no document handoff and no secret or response body in logs
```

- Verification contribution: hostile fixtures RED/GREEN证明security failure无side effect。
- After this file: secure acceptance contract冻结。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/dto/GatewayOpenApiSyncCandidateDTO.java`

- Purpose: 创建candidate/document simple records，明确trusted-context与raw ownership。
- Symbols: `GatewayOpenApiSyncCandidateDTO`,`GatewayOpenApiDocumentDTO` compact constructors/validation groups。
- Repository evidence: primary §10 fields；amendment Manifest作为DDC→candidate共享contract。
- Dependencies and consumers: DDC projection/client/validators/adapter/snapshot repository。
- Why now: tests已固定boundary，client/rules只接收typed validated objects。
- Contract/signature changes: candidate含application/build/group/host/port/secure/path/resource/artifact/provider identity；document含bounded raw bytes、documentSha、parsed model/canonical later。
- Input/output and state mapping: DDC instance+Manifest→trimmed immutable candidate；HTTP bytes→defensive copy/document SHA；parsed/canonical fields按阶段required。
- Error and edge behavior: 不允许caller URL；null/mismatch/expired/unsafe path拒绝；raw bytes不通过toString/log；no phone fields。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001`。
- Literal rule enforcement: `Rule 1` DTO suffix；`Rule 2` Validation groups/compact normalization；`Rule 3` records/defensive byte copy；`Rule 6` Jackson model boundary；`Rule 10` Instant；`Rule 11` domain/dto。
- Implementation pseudocode:

```java
record candidate from trusted DDC fields only and validate application, build, group, secure host/port, fixed path and resource URI
record document with copied raw bytes, lowercase document sha, parsed OpenAPI model and validation metadata by lifecycle stage
use compact constructors to trim identifiers, reject mismatches and prevent mutable list, map or byte-array aliasing
define Default and Ingestion validation groups so incomplete fetch-stage data cannot cross into adaptation
```

- Verification contribution: DTO constructor/group tests GREEN；client API不能接受arbitrary URL。
- After this file: secure typed handoff可用。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/client/GatewayProviderOpenApiClient.java`

- Purpose: 用JDK HttpClient安全派生/fetch每个group文档。
- Symbols: named `gatewayProviderOpenApiClient`、`fetch(GatewayOpenApiSyncCandidateDTO)`；`@Slf4j`、`@RequiredArgsConstructor`、qualified token supplier/Clock/DNS policy。
- Repository evidence: Admin已有PlatformServiceAccessTokenSupplier；Spec DEC-A06锁定JDK client与double DNS。
- Dependencies and consumers: validated candidate、IdP token、JDK DNS/HttpClient/ObjectMapper；Step11 sync service。
- Why now: DTO与安全tests已完成。
- Contract/signature changes: no URL parameter；URI严格由secure host/port/fixed template/group派生；timeouts connect3s/request10s；no redirect。
- Input/output and state mapping: candidate→token(resourceUri,scope)→request→bounded bytes/SHA/document；response body streaming max5MiB。
- Error and edge behavior: all resolved addresses must be allowlisted both checks；401/403不跨resource重试；max3 instances由caller；body/token不log；external ref在validator阻断。
- Standards impact: `MC-REUSE-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-TIME-001`；无Apache依赖。
- Literal rule enforcement: `Rule 2` @Validated input；`Rule 4` named/@Slf4j/RequiredArgsConstructor/Qualifiers；`Rule 5` JDK client/URI/Digest；`Rule 10` Duration/Clock；`Rule 11` client layer。
- Implementation pseudocode:

```java
validate candidate, resolve host through trusted DNS and reject unless every address matches approved CIDRs
derive HTTPS URI from host, port, fixed template and encoded group; reject userInfo, query, fragment and traversal
obtain PLATFORM SERVICE token for candidate.resourceUri and SCOPE_gateway.openapi.read; send no-redirect JDK request with fixed timeouts
resolve and verify addresses again before accepting response; stream at most five MiB, require success JSON, compute sha and return document DTO
```

- Verification contribution: client security tests GREEN；dependency scan无Apache HTTP。
- After this file: safe raw document acquisition可用。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/validation/GatewayOpenApiValidationChain.java`

- Purpose: 实现ordered Chain与六个rule classes。
- Symbols: `GatewayOpenApiValidationRule`；named chain；`GatewayOpenApiDocumentEnvelopeValidator,GatewayOpenApi31Validator,GatewayOpenApiReferenceValidator,GatewayOpenApiLimitValidator,GatewayOpenApiEgonExtensionValidator,GatewayOpenApiMcpProjectionValidator`。
- Repository evidence: primary §13选择Chain；amendment §7.3定义顺序和MCP optional。
- Dependencies and consumers: ObjectMapper/Swagger models/properties/document DTO；Step9 Adapter。
- Why now: client输出存在，进入mapping前必须通过全套validation。
- Contract/signature changes: each rule returns/throws classified validation result；order envelope→3.1→refs→limits→Egon→MCP；first failure short-circuit。
- Input/output and state mapping: raw/model/candidate→validated model；MCP absent/disabled pass，enabled complete pass；failure creates invalid snapshot data but neverDefinition。
- Error and edge behavior: external/remote refs禁止；circular/local refs与depth/count/media bounds；operationId duplicate/mismatch；unsafe error text截断/脱敏。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-PATTERN-001`；Chain适配真实独立规则。
- Literal rule enforcement: `Rule 2`每rule明确boundary；`Rule 4` named Beans/log/qualified ordered list；`Rule 5`JDK/Spring/Jackson；`Rule 6`JSON model；`Rule 9`Chain of Responsibility；`Rule 11`validation/rule package。
- Implementation pseudocode:

```java
interface rule exposes order and validate(candidate, document, context)
chain sorts qualified rule beans once, invokes each in fixed order and stops on first classified violation
envelope/spec/ref/limit/egon rules validate their single concern without persistence or network access
mcp rule returns success when extension is absent or disabled and validates complete governed fields only when enabled=true
```

- Verification contribution: all six focused tests与chain order/short-circuit GREEN。
- After this file: validated document边界完成；Adapter/Ingestion仍不存在。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=GatewayProviderOpenApiClientTest,GatewayOpenApiValidationChainTest,GatewayOpenApiDocumentEnvelopeValidatorTest,GatewayOpenApi31ValidatorTest,GatewayOpenApiReferenceValidatorTest,GatewayOpenApiLimitValidatorTest,GatewayOpenApiEgonExtensionValidatorTest,GatewayOpenApiMcpProjectionValidatorTest -Dsurefire.failIfNoSpecifiedTests=false test && ! rg 'org\.apache\.http|okhttp|swagger-parser' egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/{pom.xml,src/main/java}`
- Expected result: hostile/security/validation tests pass；无未批准client/parser依赖；失败branch无snapshot ingest/secret log。
- Failure returns to: File 1 threat contract；File 2 trusted DTO；File 3 network；File 4 rule classification/order。
- Completion criteria: DDC-derived only、OAuth2、双DNS/CIDR、HTTPS/no redirect/size/ref/MCP optional全覆盖。
- Rollback: revertclient/validation/DTO/tests；V12 persistence不受影响。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/client/GatewayProviderOpenApiClientTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/dto/GatewayOpenApiSyncCandidateDTO.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/client/GatewayProviderOpenApiClient.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/validation/GatewayOpenApiValidationChain.java`, all six rule/test and companion DTO paths
- Commit: `feat(gateway-admin): secure and validate provider openapi documents`

### Step 9 — 将每 Group OpenAPI 3.1 确定性适配到 Report v2

- Requirements: `REQ-006, REQ-008, REQ-021, REQ-022, REQ-023`
- Dependencies: `Steps 2 and 8`
- Baseline state: validated OpenAPI model/raw document可用；既有Invocation Schema v2/OperationKey/SchemaValidator可复用；无OpenAPI mapping。
- Observable outcome: 同一文档忽略servers/实例差异得到稳定canonicalSha；paths/components/x-egon映射为typed DefinitionDTO，再经MapStruct/BaseConverter生成Report v2；响应选择与错误schema确定。
- End state: 单group可纯函数适配，不写DB、不聚合多group、不投影生产Route。
- Test-first gate: Required — 先创建canonical/mapping/response/media/MCP Golden tests；因Adapters/DTO/mapper缺失RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/converter/GatewayOpenApi31ContractAdapterTest.java`

- Purpose: 固定canonicalization、operation/schema/response/error与optional MCP mapping RED；同批创建Invocation adapter与MapStruct converter tests。
- Symbols: `GatewayOpenApi31ContractAdapterTest`,`GatewayOpenApiInvocationSchemaAdapterTest`,`GatewayOpenApiDefinitionConverterTest`。
- Repository evidence: primary TEST-011–017；amendment TEST-110–113/122–124；Step 4 Golden可复用。
- Dependencies and consumers: validated document/candidate、Swagger model、待创建DTO/Adapters/Converter、existing Report canonicalizer/schema validator。
- Why now: 确定性 mapping必须先于聚合和DB事务。
- Contract/signature changes: canonical excludes/sorts exact fields；operationKey=`application:http:METHOD:path`；success response=`200→lowest2xx→default`；errors保留；media conflict拒绝。
- Input/output and state mapping: OpenAPI root/path/method/parameters/requestBody/responses/components/extensions→DefinitionDTO→Report groups/operations/invocation schemas/attributes/openapiGroup。
- Error and edge behavior: duplicate operationId、unsupported external ref、多success media conflicting schema、missing required extension、invalid enabled MCP fail；MCP absent pass。
- Standards impact: `MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-JSON-001, MC-PATTERN-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2` Adapter boundary validation；`Rule 3` records+BaseConverter；`Rule 6` JSON/OpenAPI mapping；`Rule 9` two algorithmic Adapters + MapStruct separation；`Rule 11` converter layer。
- Implementation pseudocode:

```java
load orders, inventory and drift Golden documents; vary servers and unordered sets and assert identical canonical sha
map parameters by path, query, header and cookie plus request body and component schemas into Invocation Schema v2
assert success response priority is exact and all nonselected/error responses remain represented
assert optional mcp creates no tool metadata unless enabled, and converter output equals Report v2 Golden without generating routes
```

- Verification contribution: mapping RED/GREEN与determinism proof。
- After this file: final pure mapping contract被冻结。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/dto/GatewayOpenApiDefinitionDTO.java`

- Purpose: 创建normalized simple record graph，隔离Swagger依赖与gateway-contract。
- Symbols: `GatewayOpenApiDefinitionDTO`及nested records for service/group/operation/parameter/request/response/schema/extensions。
- Repository evidence: primary §10.1明确record with nested records；Swagger不能泄漏Contract。
- Dependencies and consumers: Adapters produce；MapStruct converter consumes。
- Why now: tests已锁定field graph，mapping实现需typed intermediate。
- Contract/signature changes: fields losslessly对应Report v2并额外保留openapiGroup/canonical provenance；no duplicate BO/PO。
- Input/output and state mapping: normalized lists/maps defensively copied/sorted；null/default/media/schema语义明确；no persistence/time。
- Error and edge behavior: duplicate keys/missing required identifiers拒绝；unknown vendor extensions忽略但不改变canonical rules；free-form schemas显式表示。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001`。
- Literal rule enforcement: `Rule 1` DTO；`Rule 2` compact constructors；`Rule 3` top-level/nested records；`Rule 6` external-derived shape；`Rule 11` domain/dto。
- Implementation pseudocode:

```java
define one top-level definition DTO with nested immutable records matching Report service, group, operation and schema hierarchy
normalize identifiers and defensively copy ordered collections in compact constructors
retain openapiGroup, operationId, method, normalized path, request locations, success response, error responses and governed extensions
reject duplicate semantic keys and incomplete schema nodes before MapStruct conversion
```

- Verification contribution: construction/serialization/mapper tests compile且GREEN。
- After this file: typed normalized graph存在。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/converter/GatewayOpenApiInvocationSchemaAdapter.java`

- Purpose: 实现OpenAPI schema/ref/parameter/body/response到Invocation Schema v2算法；同批创建`GatewayOpenApi31ContractAdapter`。
- Symbols: named adapters、canonicalize/adapt methods、`@Slf4j`/qualified dependencies。
- Repository evidence: existing GatewayOperationSchemaValidator与ProtobufSchemaMapper提供目标schema语义；Spec禁止JSON round-trip mapping。
- Dependencies and consumers: Swagger models/validated document/properties/DefinitionDTO；Converter。
- Why now: typed DTO已完成，测试固定算法分支。
- Contract/signature changes: only local `$ref` resolution；stable sort；servers排除；response selection exact；x-egon cross-check。
- Input/output and state mapping: document→canonical bytes/hash + DTO；path/method/group进入operationKey；component recursion有depth/node guards。
- Error and edge behavior: cycles/unsupported composition/media conflicts/duplicate operations分类INVALID；no partial DTO；no network ref resolution。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-PATTERN-001`。
- Literal rule enforcement: `Rule 2` validated input/output；`Rule 4` named Beans/logging/qualified DI；`Rule 5`JDK/Jackson/Swagger only；`Rule 6`canonical JSON；`Rule 9`Adapter pattern；`Rule 11`converter package。
- Implementation pseudocode:

```java
canonicalize by deep-copying the parsed model, removing servers and volatile instance data, then sorting Spec-defined sets before Jackson bytes and sha
iterate paths in stable order and methods in fixed HTTP order; require unique operationId and build stable operationKey
delegate local schema and request/response graph traversal to invocation adapter with depth, node and media guards
map x-egon service/catalog/policy and optional mcp fields into normalized DTO; return no partial result on any violation
```

- Verification contribution: adapter/canonical/response Golden tests GREEN。
- After this file: validated document可确定性转normalized DTO。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/converter/GatewayOpenApiDefinitionConverter.java`

- Purpose: 用MapStruct完成normalized DTO与Report v2层级转换并继承Egon BaseConverter。
- Symbols: `@Mapper(componentModel="spring")` named `gatewayOpenApiDefinitionConverter` implements `BaseConverter<GatewayOpenApiDefinitionDTO,GatewayInterfaceDefinitionReport>`。
- Repository evidence: common-core BaseConverter真实存在；Spec §10.4强制MapStruct；Step 1已配置processor。
- Dependencies and consumers: DefinitionDTO、GatewayInterfaceDefinitionReport、Step10 coordinator/ingestion。
- Why now: algorithmic normalization完成后，structural mapping无需手写setter。
- Contract/signature changes: exact fields/source=`OPENAPI31`/protocol=`HTTP`/openapiGroup attributes；reverse mapping仅在lossless且接口要求时。
- Input/output and state mapping: nested records→report records/lists/maps；null/default/enum preserved；no JSON round-trip。
- Error and edge behavior: unmapped target policy设ERROR；null input按BaseConverter convention；source/protocol mismatch impossible byconstant/test。
- Standards impact: `MC-NAME-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001`。
- Literal rule enforcement: `Rule 1` Converter语义；`Rule 3` MapStruct + mandatory BaseConverter；`Rule 4` component bean name可注入；`Rule 6` enum/wire mapping；`Rule 11` converter不承担repository/service。
- Implementation pseudocode:

```java
declare Spring MapStruct mapper implementing the exact Egon BaseConverter generic contract
map service, group, operation, request and response nodes one-to-one with explicit sourceType OPENAPI31 and protocol HTTP
carry openapiGroup and governed attributes into the existing Report v2 extension fields
enable unmapped target errors and prove round-trip only for fields represented losslessly by the normalized DTO
```

- Verification contribution: MapStruct generated compile、BaseConverter/type mapping test GREEN。
- After this file: 每group可得到valid Report fragment；未聚合/写DB。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=GatewayOpenApi31ContractAdapterTest,GatewayOpenApiInvocationSchemaAdapterTest,GatewayOpenApiDefinitionConverterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: Golden/canonical/mapping tests pass；MapStruct generated source编译；servers变动不改canonical；媒体冲突fail closed。
- Failure returns to: File 1 expected mapping；File 2 DTO graph；File 3算法；File 4structural mapping。
- Completion criteria: OpenAPI 3.1→Report v2确定性mapping完整且无Route side effect/MCP强制。
- Rollback: revertconverter/DTO/tests；secure acquisition仍可独立运行但不ingest。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/converter/GatewayOpenApi31ContractAdapterTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/dto/GatewayOpenApiDefinitionDTO.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/converter/GatewayOpenApiInvocationSchemaAdapter.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/converter/GatewayOpenApiDefinitionConverter.java`, companion adapter/converter tests
- Commit: `feat(gateway-admin): adapt openapi groups to report v2`

### Step 10 — 抽取共享 Ingestion 并原子聚合完整 Group Set

- Requirements: `REQ-007, REQ-008, REQ-010, REQ-018, REQ-019, REQ-020, REQ-021, REQ-024`
- Dependencies: `Steps 6, 7 and 9`
- Baseline state: RPC ReportService仍混合transport/validation/write；每group Report fragment与snapshot repos存在；尚无完整集协调和snapshot-list原子link。
- Observable outcome: Coordinator只有在Manifest精确完整、跨group operation唯一、hash无漂移时构建一个HTTP Report；Ingestion以一个事务写Definition graph并链接全部snapshots；RPC与HTTP共用不变量但保持不同source/protocol truth。
- End state: Definition transaction不更新sync rows；成功返回一个definitionSetId供Step11逐行CAS；MCP projection仅对enabled operations。
- Test-first gate: Required — aggregate completeness/duplicate/drift、ingestion rollback/recovery、RPC source/MCP optional tests先RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiAggregateCoordinatorTest.java`

- Purpose: 固定完整集、duplicate、drift、atomic link与recovery RED；同批新增`GatewayDefinitionIngestionServiceTest`并修改ReportService/McpRelease tests。
- Symbols: coordinator tests、ingestion transaction tests、RPC/HTTP source pairing、linked-set repair、MCP optional tests。
- Repository evidence: amendment TEST-106–117/122–124；current ReportService tests已覆盖HMAC/idempotency/immutable build。
- Dependencies and consumers: adapters/repos/existing definition repositories/MCP release；Step11 sync。
- Why now: 复杂事务和多group一致性必须由失败/rollback assertion先固定。
- Contract/signature changes: all advertised groups exactly once；one aggregateSha/sourceScope；snapshotIds cardinality match；ingest returns existing/new set id idempotently。
- Input/output and state mapping: manifest+documents+snapshotIds→aggregate DTO→one Report→ingestion result；transaction failure→no definition graph/no links；recovery finds linked set。
- Error and edge behavior: missing/extra group、duplicate operationKey、manifest drift、same build diff aggregate、protocol/source mismatch、partial repository failure都fail closed，old current set unchanged。
- Standards impact: `MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-PATTERN-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2` Coordinator/Ingestion validation groups；`Rule 3` aggregate/command records；`Rule 9` Coordinator+Facade；`Rule 10`Clock/recovery times；`Rule 11`service/reporting boundaries。
- Implementation pseudocode:

```java
arrange an advertised manifest with orders and inventory documents plus snapshots and existing old valid set
assert missing, extra, duplicate operationKey and cross-instance hash drift never call ingestion and preserve old set
on complete input assert one aggregate report, one definition set and both snapshot links inside one transaction
inject repository failure and assert rollback; retry with prelinked set and assert reuse without duplicate definition
```

- Verification contribution: core atomicity/recovery RED/GREEN。
- After this file: aggregate/ingestion behavior被冻结。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/dto/GatewayOpenApiAggregateDTO.java`

- Purpose: 创建完整集与共享ingestion command records。
- Symbols: `GatewayOpenApiAggregateDTO`、`reporting/domain/dto/GatewayDefinitionIngestionCommandDTO`。
- Repository evidence: amendment §10 fields/cardinality；command明确snapshotIds而非sync revisions。
- Dependencies and consumers: Coordinator→Adapter/Converter→Ingestion；RPC ReportService command snapshotIds empty。
- Why now: tests先固定boundary，services只接收typed validated command。
- Contract/signature changes: aggregate application/build/groups/documents/snapshotIds/aggregateSha；command application/source/sourceScope/report/snapshotIds。
- Input/output and state mapping: group-keyed map/list严格同cardinality/order；RPC empty snapshots；OPENAPI31 nonempty snapshots；sourceScope=manifest SHA或RPC stable scope。
- Error and edge behavior: source/protocol/snapshot rule不符构造/validation失败；defensive copy；no sync IDs/revisions。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001`。
- Literal rule enforcement: `Rule 1` DTO；`Rule 2` Default/Ingestion groups；`Rule 3` records/defensive copies；`Rule 6` source enum/Report Jackson shape；`Rule 11`domain DTO归属明确。
- Implementation pseudocode:

```java
record aggregate with immutable sorted groups, exact-key document map, aligned snapshot ids and lowercase aggregate sha
record ingestion command with application, source enum, stable source scope, validated Report v2 and immutable snapshot id list
enforce OPENAPI31 plus HTTP requires nonempty snapshots while RPC_DESCRIPTOR plus RPC requires empty snapshots
reject missing or extra groups and never carry sync revisions into the transport-neutral ingestion command
```

- Verification contribution: constructor/group validation tests与service compile。
- After this file: service contracts可用。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/reporting/service/GatewayDefinitionIngestionService.java`

- Purpose: 抽取transport-neutral Definition write Facade并建立一个事务。
- Symbols: named `gatewayDefinitionIngestionService`、`ingest(@Valid GatewayDefinitionIngestionCommandDTO)`、transaction boundary/result。
- Repository evidence: current `GatewayDefinitionReportService`已有canonical validation/build/write但与HMAC耦合；existing repos支持definition graph写入。
- Dependencies and consumers: report/schema validators、definition/catalog/MCP repositories、snapshot repo；RPC ReportService与Coordinator。
- Why now: command/tests已完成；先抽共享writer再让Coordinator调用。
- Contract/signature changes: report service保留HMAC/idempotency/application resolution，转调Ingestion；ingestion校验source/protocol/immutable build并写graph+snapshot links。
- Input/output and state mapping: command→fingerprint scoped by application/build/protocol/sourceScope→existing/new Definition Set→group/operation/schema/MCP writes→snapshot links→result。
- Error and edge behavior: same scope same fingerprint idempotent；same build/protocol/scope diff fingerprint conflict；任何write/link失败整个事务rollback；不更新sync。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-PATTERN-001, MC-TIME-001`；Facade处理真实跨repo事务。
- Literal rule enforcement: `Rule 2` @Validated/@Valid Ingestion；`Rule 4` @Slf4j/named/@RequiredArgsConstructor/final Qualifiers；`Rule 9` Facade pattern；`Rule 10` injected Clock/Instant；`Rule 11` reporting service owns shared writer。
- Implementation pseudocode:

```java
validate command and source/protocol/snapshot invariants before opening writes
inside one transaction find fingerprint by application, build, protocol and sourceScope; reject immutable drift or reuse exact existing set
write definition set, groups, operations, invocation schemas and optional managed tools through existing repositories
link every command snapshot id to the resulting set and assert full affected count; return result without touching sync state
```

- Verification contribution: transaction/idempotency/fault tests GREEN；ReportService old tests GREEN。
- After this file: RPC/HTTP共享一个Definition writer。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiAggregateCoordinator.java`

- Purpose: 校验完整Manifest并合并各group DefinitionDTO/Report fragment，调用Ingestion；同批修改ReportRepository/JDBC protocol+sourceScope查询、ReportService和McpReleaseContentFactory。
- Symbols: named coordinator；`aggregateAndIngest`；repository `findBuildFingerprint(applicationId,buildId,protocol,sourceScope)`；MCP source enum branch。
- Repository evidence: current repository fingerprint lookup忽略protocol；McpReleaseContentFactory硬编码STARTER；amendment明确修正。
- Dependencies and consumers: snapshot/sync repos、Adapter/Converter、Ingestion；Step11 sync service。
- Why now: shared writer已完成，Coordinator只负责集合规则与调用。
- Contract/signature changes: sorted complete groups merge到一个HTTP Report；operationKey global unique；aggregate SHA参与sourceScope；all rows same set result。
- Input/output and state mapping: Manifest + validated per-group documents/snapshots→AggregateDTO→merged Report→command OPENAPI31→definitionSetId；MCP enabled only投影，absent不创建。
- Error and edge behavior: any invalid/incomplete/drift/duplicate prevents ingestion；old set不变；linked-set recovery直接返回同set；RPC path通过ReportService source RPC_DESCRIPTOR。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-CONVERT-001, MC-PATTERN-001`。
- Literal rule enforcement: `Rule 2` exact set/cardinality validation；`Rule 4` named/log/constructor/qualifiers；`Rule 9` Coordinator组织复杂流，Adapter/Facade分工；`Rule 11` openapi service不写SQL。
- Implementation pseudocode:

```java
load snapshots for exact manifest groups and require every document valid, same application/build/service metadata and stable per-group canonical hash
merge group reports in sorted group order, rejecting duplicate operationKey across the whole application build
compute aggregate sha from sorted group plus canonical sha tuples and detect same build manifest or aggregate drift before writes
build OPENAPI31 ingestion command with all snapshot ids; call qualified ingestion facade once and return one definitionSetId for later sync CAS
```

- Verification contribution: aggregate/ReportService/repository/MCP tests GREEN。
- After this file: 完整Group Set可原子创建一个Definition Set；sync状态仍由后续own。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=GatewayOpenApiAggregateCoordinatorTest,GatewayDefinitionIngestionServiceTest,GatewayDefinitionReportServiceTest,McpReleaseContentFactoryTest,JdbcGatewayDefinitionReportRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: complete aggregate produces one set/all links；all failure/rollback/recovery/RPC/MCP optional tests pass；sync table untouched by ingestion。
- Failure returns to: File 1 invariants；File 2 command modeling；File 3transaction；File 4aggregate/source integration。
- Completion criteria: HTTP/RPC共用Ingestion但不混事实源；多group atomicity/drift/duplicates/recovery全部证明。
- Rollback: revertStep 10 services/repository changes；V12/adapter保留无activation。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiAggregateCoordinatorTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/dto/GatewayOpenApiAggregateDTO.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/reporting/service/GatewayDefinitionIngestionService.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiAggregateCoordinator.java`, reporting repository/JDBC/ReportService/McpReleaseContentFactory and tests
- Commit: `feat(gateway-admin): atomically ingest complete openapi group sets`

### Step 11 — 编排有界 Sync、逐 Group CAS 恢复与 Definition 生命周期

- Requirements: `REQ-004, REQ-005, REQ-007, REQ-011, REQ-017, REQ-019, REQ-024, REQ-025`
- Dependencies: `Step 10`
- Baseline state: secure fetch/validation/aggregate/ingestion ports可用；无OpenAPI scheduler/service/properties；lifecycle只从DDC definition-set metadata识别active sets。
- Observable outcome: 每30s最多50 candidates、3 instances，2m lease，3s/10s timeout、指数退避+jitter、5m drift sample；每group revision CAS；linked-set repair；DDC stale fail-safe；metrics/audit safe。
- End state: 完整同步状态可稳定到VALID/INVALID/INCONSISTENT/FAILED/STALE；所有group VALID指向同一set；old set保持直到完整替换。
- Test-first gate: Required — 先写sync orchestration、reconciler、config parity、lifecycle union/recovery tests；缺services/properties时RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 4, Rule 5, Rule 7, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiSyncServiceTest.java`

- Purpose: 固定candidate discovery、claim/fetch/validate/snapshot/aggregate/CAS、retry/stale/recovery RED；同批修改 lifecycle/config tests。
- Symbols: `GatewayOpenApiSyncServiceTest`,`GatewayOpenApiSyncReconcilerTest`,`GatewayAdminOpenApiPropertiesTest`,`GatewayDefinitionLifecycleReconcilerTest`,`GatewayAdminConfigurationTest`。
- Repository evidence: current lifecycle reconciler/DDC projection/config tests存在；Spec TEST-019–023/105/114–116。
- Dependencies and consumers: DDC, client, chain, repos, coordinator, Clock/MeterRegistry；Step12 API。
- Why now: 编排默认/状态owner必须先被tests冻结。
- Contract/signature changes: reconciler batch/defaults；sync state transitions；all-group success then per-row VALID CAS；repair detects already linked set。
- Input/output and state mapping: healthy DDC instances→candidate rows；claim revision→work；failure classification→state/error/nextRetryAt；aggregate result→each group VALID same set。
- Error and edge behavior: stale DDC observation不误删active；manifest drift blocks；one CAS loss不回滚Definition，next run repairs；max3 instance attempts；invalid unchanged build noauto retry。
- Standards impact: `MC-VALID-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2` job→service validated；`Rule 7`base/local parity；`Rule 9`orchestrator不复制rules；`Rule 10`Clock/Duration/Instant；`Rule 11`scheduled/service/lifecycle层。
- Implementation pseudocode:

```java
discover two-group manifest, claim each due row by revision and fetch at most three healthy instances per group
assert one invalid or drifted group prevents coordinator call and preserves previous active definition set
on complete success assert coordinator called once then each row moves to VALID by independent CAS with the same set id
simulate crash after ingestion and CAS loss; next reconcile finds linked snapshots and repairs remaining rows without duplicate definition
```

- Verification contribution: scheduler/state/recovery/lifecycle RED/GREEN。
- After this file: orchestration行为固定。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/config/properties/GatewayAdminOpenApiProperties.java`

- Purpose: 集中同步/安全/limit defaults并同时更新base/local YAML key parity。
- Symbols: prefix `gateway.admin.openapi`；interval/batch/maxInstances/lease/connectTimeout/requestTimeout/backoff/jitter/driftSample/maxBytes/CIDRs。
- Repository evidence: `application.yml`与`application-local.yml`存在；amendment §15.1给出exact defaults。
- Dependencies and consumers: client/validators/sync/reconciler；GatewayAdminConfiguration。
- Why now: tests已锁定defaults，services wiring前先建立typed config。
- Contract/signature changes: exact defaults=30s,50,3,2m,3s,10s,approved backoff/jitter,5m,5MiB；CIDR required in enabled production。
- Input/output and state mapping: YAML→validated Duration/int/list；all profiles same keys；values可不同。
- Error and edge behavior: invalid duration/count/CIDR/size fail startup；不允许动态URL/secret字段。
- Standards impact: `MC-VALID-001, MC-TIME-001, MC-CONFIG-001, MC-BEAN-001`。
- Literal rule enforcement: `Rule 2` @Validated native constraints；`Rule 4` explicit config bean；`Rule 7`两YAML同键；`Rule 10`Duration；`Rule 11`config/properties。
- Implementation pseudocode:

```java
bind gateway.admin.openapi with exact approved defaults and validate positive bounded values
parse CIDR entries at binding with approved JDK/Spring capability and expose immutable networks
add the identical key set to application.yml and application-local.yml while allowing local-safe values
reject enabled configuration that omits trusted DNS/CIDR/resource requirements or introduces caller URL fields
```

- Verification contribution: property/default/YAML parity tests GREEN。
- After this file: runtime constants typed且一致。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiSyncService.java`

- Purpose: 编排单candidate/manifest同步和失败状态；不复制client/rule/adapter/transaction逻辑。
- Symbols: named `gatewayOpenApiSyncService`、`synchronize(candidate/revision)`、metrics/audit。
- Repository evidence: selected pattern §13；repos/client/chain/coordinator已完成。
- Dependencies and consumers: qualified DDC projection/client/chain/snapshot/sync/coordinator/Clock/MeterRegistry；reconciler。
- Why now: properties与tests已完成。
- Contract/signature changes: claims/transition methods；transaction外fetch；invalid snapshot永久保存；complete set才coordinator；post-ingest per-row CAS。
- Input/output and state mapping: DDC manifest→sync keys→FETCHING/VALIDATING/INGESTING→VALID；classified failures→INVALID/INCONSISTENT_BUILD/FETCH_FAILED/INGEST_FAILED/STALE。
- Error and edge behavior: no body/token logs；CAS loss stops ownership；retry schedule bounded；invalid unchanged no retry；old set remains。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-TIME-001, MC-PATTERN-001`。
- Literal rule enforcement: `Rule 2` validated handoffs；`Rule 4` @Slf4j/named/RequiredArgsConstructor/final Qualifiers；`Rule 5`reuse collaborators；`Rule 9`Facade/Coordinator orchestration；`Rule 10`Clock/Instant；`Rule 11`service layer。
- Implementation pseudocode:

```java
upsert exact manifest sync rows and claim due work by revision; stop immediately when ownership CAS fails
fetch outside transactions, validate, persist or reuse immutable snapshot and classify safe failures with retry policy
when every advertised group is ready call aggregate coordinator once; never ingest a partial manifest
after returned set id update each group to VALID by revision CAS; on later run repair any remaining row from persisted snapshot links
```

- Verification contribution: sync state/metrics/audit/recovery tests GREEN。
- After this file: single-run orchestration完整。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/scheduled/GatewayOpenApiSyncReconciler.java`

- Purpose: 创建有界scheduled trigger并修改Admin wiring/lifecycle active-set union。
- Symbols: named reconciler；`GatewayAdminConfiguration` enable properties/Clock/beans；`GatewayDefinitionLifecycleReconciler` union DDC RPC set IDs + DB VALID OpenAPI set IDs；repository queries。
- Repository evidence: current reconciler通过metadata `gateway.definition-set-id`；Admin config显式wiring；DDC stale语义已有tests。
- Dependencies and consumers: properties/sync service/lifecycle repositories/Spring scheduling。
- Why now: service已实现，可加薄trigger和lifecycle消费。
- Contract/signature changes: fixedDelay config、batch limit 50；no overlap claim；active set union，stale DDC observation fail-safe不清除。
- Input/output and state mapping: tick→due candidate page→service calls；DB VALID refs加入active set；metrics count safe dimensions。
- Error and edge behavior: one candidate exception不终止batch；interrupt preserved；stale DDC不做deactivate；scheduler不持有transaction/network logic。
- Standards impact: `MC-LOG-001, MC-BEAN-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001`。
- Literal rule enforcement: `Rule 4` named/log/constructor/qualifiers；`Rule 7`schedule config；`Rule 9`thin scheduler + lifecycle union；`Rule 10`Clock/Duration；`Rule 11`scheduled/reporting ownership。
- Implementation pseudocode:

```java
on each configured tick load at most batchSize due candidates and invoke sync service independently
record bounded metrics by safe application, group and outcome dimensions; continue after classified candidate failure
extend active definition set query as union of healthy DDC RPC metadata ids and database VALID OpenAPI sync set ids
when DDC observation is stale preserve prior active decisions and defer deactivation; never treat health alone as contract proof
```

- Verification contribution: reconciler/lifecycle/config tests GREEN。
- After this file: durable OpenAPI sync/lifecycle闭环完成；尚未开放管理API。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=GatewayOpenApiSyncServiceTest,GatewayOpenApiSyncReconcilerTest,GatewayAdminOpenApiPropertiesTest,GatewayDefinitionLifecycleReconcilerTest,GatewayAdminConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: named tests pass；config key parity/defaults、batch/retry/CAS repair、stale lifecycle与metrics均符合Spec。
- Failure returns to: File 1 state contract；File 2 defaults；File 3 service ownership；File 4 scheduler/lifecycle wiring。
- Completion criteria: 有界可恢复可观测sync闭环，不自动启动项目；所有group最终共享同set。
- Rollback: disable `gateway.admin.openapi.enabled` then revertStep 11；已保存snapshot/definitions保持immutable。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiSyncServiceTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/config/properties/GatewayAdminOpenApiProperties.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiSyncService.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/scheduled/GatewayOpenApiSyncReconciler.java`, application YAMLs, Admin configuration, lifecycle repository/JDBC/reconciler and tests
- Commit: `feat(gateway-admin): reconcile openapi groups with recoverable cas`

### Step 12 — 暴露只读 OpenAPI 同步/Fragment/Raw 管理 API

- Requirements: `REQ-006, REQ-011, REQ-012, REQ-020, REQ-026`
- Dependencies: `Step 11`
- Baseline state: DB状态/Definition/Raw完整但无API-002–004；existing Admin error/authorization conventions可复用。
- Observable outcome: authorized operator按application/build/group查询sync状态，按operation读OpenAPI fragment，按snapshot读/下载immutable raw；响应含source/group/hash/error/times且不暴露secret/provider arbitrary URL。
- End state: backend API contract可供frontend；全部为read-only，不触发fetch/ingest/route。
- Test-first gate: Required — 先写query/controller/authorization/404/serialization tests；缺VO/service/controller时RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/controller/GatewayOpenApiControllerTest.java`

- Purpose: 固定API-002/003/004 path/query/auth/JSON/error与no-side-effect RED；同批创建QueryService tests。
- Symbols: controller MVC tests、`GatewayOpenApiQueryServiceTest`、Jackson/permission assertions。
- Repository evidence: Admin controllers使用`CAP_gateway:read`和GatewayAdminErrorVO；frontend existing API client期望JSON。
- Dependencies and consumers: repos/catalog current definition/ObjectMapper；Step13 frontend。
- Why now: wire API必须先于controller实现稳定。
- Contract/signature changes: GET sync states；GET operation fragment；GET snapshot document；filters来自trusted IDs而非URL。
- Input/output and state mapping: DB PO/projection→VO list/fragment/raw JSON；Instant ISO UTC；enum strings exact；group排序稳定。
- Error and edge behavior: unauthorized 401/403；missing 404；invalid ID 400；raw response no token/provider host error leakage；queries never mutate/retry。
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2` request params/IDs validation；`Rule 6`Jackson API；`Rule 10`Instant serialization；`Rule 11`controller/service split。
- Implementation pseudocode:

```java
call each endpoint as anonymous, wrong-capability and CAP_gateway:read operator and assert security status
seed two group sync rows linked to one definition set and assert sorted status response with aggregate/worst fields
seed operation provenance and assert fragment contains source OPENAPI31 and originating group
read immutable snapshot raw document, assert safe headers/body and verify no repository write, fetch or retry interaction
```

- Verification contribution: API contract/security RED/GREEN。
- After this file: frontend contract被冻结。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/vo/GatewayOpenApiSyncStateVO.java`

- Purpose: 创建三个external response records。
- Symbols: `GatewayOpenApiSyncStateVO`,`GatewayOperationOpenApiVO`,`GatewayOpenApiDocumentVO`。
- Repository evidence: primary §10.1/API-002–004；amendment UI需要worst/aggregate/group/hash/error/source。
- Dependencies and consumers: QueryService/controller/frontend types；Jackson/java.time。
- Why now: controller/service实现先共享exact response shape。
- Contract/signature changes: only approved fields；Raw VO包含snapshot metadata+document，operation VO包含fragment/source/openapiGroup/snapshotId。
- Input/output and state mapping: PO/query projection→record；nullable times/error按state；collections immutable/sorted。
- Error and edge behavior: no token/resource credentials/provider arbitrary URL；error max1024 safe；raw content不进入toString/log。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001`。
- Literal rule enforcement: `Rule 1` VO；`Rule 2` compact normalization；`Rule 3` records；`Rule 6`Jackson annotations按需；`Rule 10`Instant；`Rule 11`domain/vo。
- Implementation pseudocode:

```java
define immutable sync state, operation fragment and raw document response records with exact approved fields
normalize and copy collections; serialize source and state enum by stable names and times as ISO-8601 UTC
enforce safe error length and prohibit credentials, bearer tokens and arbitrary fetch URLs from all response components
keep raw document available only in the snapshot document VO and exclude it from log-oriented string rendering
```

- Verification contribution: Jackson contract tests/type compilation。
- After this file: response model稳定。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiQueryService.java`

- Purpose: 聚合read-only repository/catalog queries并生成VO，不复用sync mutation service。
- Symbols: named `gatewayOpenApiQueryService`；`listSyncStates`,`getOperationOpenApi`,`getSnapshotDocument`。
- Repository evidence: existing CatalogRepository能查current operation definition；snapshot/sync repos已完成。
- Dependencies and consumers: qualified repositories/ObjectMapper/Clock N/A；controller。
- Why now: VO/tests已完成。
- Contract/signature changes: application/build filters optional且validated；operation resolves current definition→snapshot/group；raw bysnapshot ID。
- Input/output and state mapping: rows→worst state severity + aggregate set/fingerprint + groups；operation definition provenance→fragment；snapshot raw→document VO。
- Error and edge behavior: missing/ambiguous provenance→404/classified error；MANUAL/RPC operation无OpenAPI fragment返回not-available而非伪造；no writes/network。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-PATTERN-001`；simple query Facade。
- Literal rule enforcement: `Rule 2` validated IDs/filters；`Rule 4` named/@Slf4j/RequiredArgsConstructor/Qualifiers；`Rule 5`reuse repos/ObjectMapper；`Rule 6`raw/fragment；`Rule 11`service→repository。
- Implementation pseudocode:

```java
list exact sync rows and group by application plus build, deriving worst state by the Spec severity order and shared aggregate set id
resolve operation current definition; when source is OPENAPI31 load its snapshot and extract the stored fragment for its openapiGroup
for MANUAL or RPC_DESCRIPTOR return the established not-available error without generating an OpenAPI projection
load immutable snapshot raw document by id and map safe metadata plus raw JSON; perform no mutation, fetch or retry
```

- Verification contribution: QueryService tests GREEN并证明RPC不伪造OpenAPI。
- After this file: read model可用。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/controller/GatewayOpenApiController.java`

- Purpose: 暴露三个只读endpoints并扩展existing exception advice。
- Symbols: named controller endpoints API-002–004；modify `GatewayAdminExceptionHandler` for validation/not-found/source-not-available/snapshot conflict safe mapping。
- Repository evidence: existing Admin controller/security/error style；Step5已使用standard docs annotations。
- Dependencies and consumers: qualified QueryService、Jakarta validation、Spring Security；Admin Web。
- Why now: service/VO/contract tests已完成。
- Contract/signature changes: exact paths/methods/capability；standard OpenAPI annotations/Egon policy；content-disposition下载按Spec。
- Input/output and state mapping: validated path/query→service→VO/JSON；no provider URL parameters。
- Error and edge behavior: consistent GatewayAdminErrorVO、safe messages、status mapping；MethodValidation errors进入advice；no body/token log。
- Standards impact: `MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001`。
- Literal rule enforcement: `Rule 2` @Validated/@Valid/native constraints；`Rule 4` named/@Slf4j/RequiredArgsConstructor/final Qualifier；`Rule 6`Jackson/OpenAPI；`Rule 10`VO times；`Rule 11`controller only delegates。
- Implementation pseudocode:

```java
authorize every endpoint with CAP_gateway:read and validate application, build, operation and snapshot identifiers
delegate list, fragment and document reads to the qualified query service and return the exact VO contracts
set safe JSON or download headers for immutable raw document without accepting a URL or group override
map validation, not found and source-not-available failures through existing GatewayAdminErrorVO while masking persistence details
```

- Verification contribution: MVC/security/serialization tests GREEN。
- After this file: backend management API完整可消费。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=GatewayOpenApiControllerTest,GatewayOpenApiQueryServiceTest,GatewayAdminOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: API-002–004 success/auth/error/serialization tests pass；read paths无mutation/network；RPC source返回not-available。
- Failure returns to: File 1 contract；File 2 VO；File 3 query mapping；File 4 HTTP/security/advice。
- Completion criteria: frontend所需aggregate/group/source/hash/error/raw/fragment数据全部可用且只读安全。
- Rollback: revertAPI/controller/service/VO/advice；sync pipeline继续运行但无UI query。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/openapi/controller/GatewayOpenApiControllerTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/domain/vo/GatewayOpenApiSyncStateVO.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/service/GatewayOpenApiQueryService.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/gateway/admin/openapi/controller/GatewayOpenApiController.java`, companion VOs/tests and `GatewayAdminExceptionHandler`
- Commit: `feat(gateway-admin): expose openapi sync and document queries`

### Step 13 — 在 Admin Web 展示聚合状态、逐 Group 明细与 OpenAPI 文档

- Requirements: `REQ-012, REQ-020, REQ-026`
- Dependencies: `Step 12`
- Baseline state: `ApplicationsPage`无OpenAPI状态；`OperationPage`只有内部schema视图；API types/client无sync/fragment/raw contracts；`SchemaPanel`已可复用且明确不改。
- Observable outcome: Applications行展示worst/aggregate并可展开group状态/hash/error；Catalog过滤三值source；Operation新增OpenAPI Tab显示source/group/fragment并支持copy/download/refresh及loading/error/empty states。
- End state: frontend完整消费API-002–004且不直接访问Provider；无新增route/权限模型。
- Test-first gate: Required — 先修改API/App tests并新增OperationPage test；缺types/client/UI状态时RED。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 5, Rule 6, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/api/gatewayApi.test.ts`

- Purpose: 先固定三个API调用、query encoding、source enum和错误契约；同批修改Applications test并创建OperationPage test。
- Symbols: `gatewayApi.test.ts`,`ApplicationsPage.test.tsx`,`OperationPage.test.tsx`。
- Repository evidence: existing Vitest/RTL/MSW-like fetch mocking与API helper patterns；ApplicationsPage test已存在。
- Dependencies and consumers: pending types/api/page implementations；backend Step12 contract。
- Why now: frontend行为和states必须test-first。
- Contract/signature changes: client methods list sync states/get operation fragment/get snapshot document；UI expects worst/aggregate/groups/source/error/time。
- Input/output and state mapping: mocked JSON→typed state→badges/expanded rows/tab/editor/download；refresh invalidates exact query keys。
- Error and edge behavior: loading skeleton、empty、partial group errors、API failure/retry、MANUAL/RPC no OpenAPI、copy/download unavailable均assert；no Provider URL request。
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001`；TypeScript compile是boundary proof。
- Literal rule enforcement: `Rule 2` UI filter/ID inputs encode/validate；`Rule 5`复用现有fetch/query/AntD，无新utility；`Rule 6`wire shape exact；`Rule 10`ISO times display；`Rule 11`existing React feature structure。
- Implementation pseudocode:

```typescript
mock API responses for two groups sharing one aggregate definition set plus invalid, loading and empty variants
assert Applications renders worst status and aggregate identity, expands sorted group rows and exposes safe hash/error details
assert Operation OpenAPI tab renders OPENAPI31 group fragment, refresh, copy and snapshot download controls
assert MANUAL and RPC_DESCRIPTOR show source-appropriate empty state and the browser never calls a provider host or arbitrary URL
```

- Verification contribution: frontend RED/GREEN covering requested operator flows。
- After this file: final UI behavior被冻结。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/api/types.ts`

- Purpose: 建立与backend VO完全一致的TypeScript contracts并修改gatewayApi methods。
- Symbols: `GatewayDefinitionSourceType='MANUAL'|'RPC_DESCRIPTOR'|'OPENAPI31'`、sync/group/operation/document types；API functions。
- Repository evidence: existing types.ts/gatewayApi.ts集中管理Admin API shapes/calls。
- Dependencies and consumers: pages/tests；backend enum/VO。
- Why now: tests已固定响应，components前先提供类型化client。
- Contract/signature changes: exact query/path methods；no arbitrary URL argument；document download由snapshotId。
- Input/output and state mapping: JSON enum/time/nulls→TypeScript unions/strings；filters→URLSearchParams；API error→existing error abstraction。
- Error and edge behavior: unknown source/state在boundary显式失败或显示unknown-safe state；optional fields不转undefined/empty混淆；never pass provider URI。
- Standards impact: `MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-TIME-001`。
- Literal rule enforcement: `Rule 1` typed DTO-like接口语义命名；`Rule 2` API boundary参数encode；`Rule 6` exact JSON fields；`Rule 10`ISO string不转java.util概念；`Rule 11`api layer。
- Implementation pseudocode:

```typescript
define exact source and sync state unions plus aggregate, group, operation fragment and snapshot document interfaces
add client functions whose only inputs are validated application, build, operation or snapshot identifiers
encode optional filters with URLSearchParams and preserve null versus absent fields from the backend contract
reuse the existing request and error wrapper; do not accept or construct provider URLs in frontend code
```

- Verification contribution: API tests/typecheck GREEN。
- After this file: frontend contract/client可供pages消费。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/applications/ApplicationsPage.tsx`

- Purpose: 在现有Applications表添加worst/aggregate状态和可展开group明细，不新增页面/route。
- Symbols: query hook/state mapping、status badge、expanded row group table、refresh。
- Repository evidence: current page已有application table/query/error patterns；amendment §12 layout。
- Dependencies and consumers: gatewayApi/types/AntD/existing permissions。
- Why now: typed API已完成。
- Contract/signature changes: additive columns/expanded content；existing application actions不变。
- Input/output and state mapping: sync response按application映射；worst state→badge；groups sorted→rows；safe error/hash/times显示。
- Error and edge behavior: no sync data→Not reported；partial/loading/error不覆盖existing application table；refresh只invalidate sync query；responsive/a11y labels。
- Standards impact: `MC-REUSE-001, MC-VALID-001, MC-TIME-001, MC-SCOPE-001`。
- Literal rule enforcement: `Rule 2` filter/state boundary；`Rule 5`复用AntD/query helpers；`Rule 10`既有time formatter；`Rule 11`applications feature。
- Implementation pseudocode:

```typescript
fetch sync states alongside existing applications and index them by application id without blocking the base table
render the Spec severity-derived worst badge and aggregate definition identity in additive columns
expand a row into a sorted group table showing group, state, canonical hash, safe error and timestamps
preserve loading, empty, partial error and retry states with accessible labels and no mutation or provider request
```

- Verification contribution: ApplicationsPage tests GREEN。
- After this file: 应用级/Group级可见性完成。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/interface-catalog/OperationPage.tsx`

- Purpose: 新增OpenAPI Tab并修改Catalog source filter；明确复用未修改的SchemaPanel。
- Symbols: Operation OpenAPI query/tab/copy/download/refresh；`CatalogPage.tsx` source options=`MANUAL/RPC_DESCRIPTOR/OPENAPI31`。
- Repository evidence: OperationPage已有tabs/detail loading/error；SchemaPanel已渲染internal v2；CatalogPage已有filter。
- Dependencies and consumers: typed client、browser clipboard/download、existing auth/navigation。
- Why now: backend/API/Application UI均稳定。
- Contract/signature changes: additive tab；source/group displayed；snapshot download通过API-004；no public route change。
- Input/output and state mapping: operationId→fragment VO→formatted JSON；snapshotId→download blob；source state→available/empty分支。
- Error and edge behavior: fetch failure有retry；clipboard/download failures feedback；RPC/MANUAL不伪造OpenAPI；large raw只按用户动作下载。
- Standards impact: `MC-REUSE-001, MC-VALID-001, MC-JSON-001, MC-SCOPE-001`。
- Literal rule enforcement: `Rule 2` operation/snapshot IDs来自trusted route/API；`Rule 5`浏览器原生copy/download+现有组件；`Rule 6`JSON pretty display不改原文；`Rule 11`interface-catalog feature。
- Implementation pseudocode:

```typescript
add an OpenAPI tab that requests the selected operation fragment and shows source plus originating group
render formatted read-only JSON with copy and refresh; request immutable raw document by snapshot id only for download
for MANUAL or RPC_DESCRIPTOR render an explicit unavailable projection state while leaving the existing SchemaPanel tab unchanged
update Catalog source filter to the exact three values and preserve existing operation navigation and permissions
```

- Verification contribution: Operation/Catalog tests、typecheck、lint GREEN。
- After this file: Admin Web requestedexperience完成，无Provider直连。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web`
- Verification command: `npm test -- --run src/api/gatewayApi.test.ts src/features/applications/ApplicationsPage.test.tsx src/features/interface-catalog/OperationPage.test.tsx && npm run typecheck && npm run lint && npm run build`
- Expected result: focused tests/typecheck/lint/build exit 0；all states与三值source可见；SchemaPanel无diff。
- Failure returns to: File 1 expected UX；File 2 contract/client；File 3 Applications；File 4 Operation/Catalog。
- Completion criteria: 应用聚合/Group明细、Operation source/group/OpenAPI、copy/download/refresh完整且无新route/provider call。
- Rollback: revertfrontend Step 13 paths；backend APIs保持可用。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/api/gatewayApi.test.ts`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/api/types.ts`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/applications/ApplicationsPage.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/interface-catalog/OperationPage.tsx`, `gatewayApi.ts`, `ApplicationsPage.test.tsx`, `OperationPage.test.tsx`, `CatalogPage.tsx`
- Commit: `feat(gateway-admin-web): show openapi group aggregation status`

### Step 14 — 闭合架构、RPC/MCP/Route/Release 与 E2E 回归证据

- Requirements: `REQ-003, REQ-009, REQ-010, REQ-012, REQ-014, REQ-015, REQ-018, REQ-020, REQ-021, REQ-026`
- Dependencies: `Steps 1–13`
- Baseline state: 所有功能实现及focused tests完成；现有E2E/architecture fixture尚未表达multi-group/source/optional MCP与residual gates。
- Observable outcome: static/module/frontend/E2E contract覆盖最终系统边界；证明OpenAPI不自动生成Route、RPC Descriptor/MCP Tool ID/Engine不回归、架构与依赖隔离成立。
- End state: implementation-ready acceptance evidence完整；live topology仍需用户控制启动后单独执行，不能被静态/模块测试冒充。
- Test-first gate: Not applicable — 本Step不新增生产行为，只把已实现验收写入回归/架构/E2E tests；任何失败返回其拥有Step修复，不在此Step加入兼容逻辑。
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-VALID-001, MC-CONVERT-001, MC-JSON-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001`
- Literal Rules: `Rule 2, Rule 3, Rule 5, Rule 6, Rule 7, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/architecture/GatewayAdminPackageArchitectureTest.java`

- Purpose: 允许并约束exact `openapi` feature packages，禁止biz/archetype混合、Swagger model泄漏Contract、controller→JDBC直连与client→repository反向依赖。
- Symbols: package rules/module dependency assertions/legacy residual assertions。
- Repository evidence: existing architecture test验证Admin package boundaries；Step 1–12新增明确feature-first tree。
- Dependencies and consumers: all Admin production packages/build graph。
- Why now: 最终tree稳定后锁定架构，失败必须回拥有Step。
- Contract/signature changes: test-only architecture allowlist/update。
- Input/output and state mapping: compiled classes/import graph→allowed/forbidden dependency assertions；no runtime state。
- Error and edge behavior: any `biz.*`、Swagger in contract、JDBC in controller、old HTTP compiler、source string STARTER fails。
- Standards impact: `MC-ARCH-001, MC-DEP-001, MC-CONVERT-001, MC-SCOPE-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 3` BaseConverter/MapStruct boundary可扫描；`Rule 5`dependency allowlist；`Rule 9`pattern participants不反向依赖；`Rule 11`唯一Traditional Layered profile。
- Implementation pseudocode:

```java
allow only the approved openapi controller, service, scheduled, client, validation, converter, repository and domain packages
assert controllers depend on services rather than jdbc, clients do not depend on repositories and contract has no Swagger types
fail on biz or archetype hybrid packages, legacy HTTP compiler symbols and STARTER source literals
retain all preexisting package rules for reporting, catalog, mcp, route, release and engine boundaries
```

- Verification contribution: architecture/static boundary GREEN。
- After this file: package/dependency structure有可执行守卫。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpFixtureContractTest.java`

- Purpose: 更新跨module fixture到三值source并证明MCP optional/Tool ID与RPC Descriptor Golden；同批确保RPC contract tests仍覆盖source pairing。
- Symbols: `McpFixtureContractTest`,`GatewayRpcContractTest` final source assertions。
- Repository evidence: tests当前引用GatewayOperation/STARTER fixture；Step6保留RPC annotations，Step10修改MCP projection。
- Dependencies and consumers: starter/contract/admin MCP release/engine/mcp-core。
- Why now: all source/ingestion changes完成后做跨module regression。
- Contract/signature changes: test fixtures source values更新；runtime Tool ID/Invocation Schema expected不变。
- Input/output and state mapping: RPC descriptor→RPC_DESCRIPTOR report→same Tool/engine invocation；HTTP absent/disabled MCP→Catalog only；enabled→Managed Tool。
- Error and edge behavior: no OpenAPI adapter in RPC classpath；ordinary HTTP operations不因MCP缺失失败；invalid MCP contract ingestion fails beforerelease。
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-PATTERN-001, MC-TEST-001`。
- Literal rule enforcement: `Rule 2` source/protocol/MCP validation；`Rule 6`fixture JSON；`Rule 9`Adapter/Ingestion/Projection边界；`Rule 11`test suite不改变production structure。
- Implementation pseudocode:

```java
load RPC descriptor fixture and assert source RPC_DESCRIPTOR, unchanged invocation schema and stable managed tool id
load HTTP OPENAPI31 fixtures with absent, disabled and enabled mcp extensions
assert absent and disabled operations remain in Catalog without Tool while enabled creates exactly one governed Tool
assert neither RPC runtime nor Engine imports or executes OpenAPI acquisition code
```

- Verification contribution: RPC/MCP/Engine cross-module regression。
- After this file: source/MCP compatibility boundary闭合。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/e2e/gateway-admin.spec.ts`

- Purpose: 扩展用户控制的E2E contract，验证应用worst/aggregate、group detail、Operation OpenAPI tab与no-auto-route。
- Symbols: Playwright scenarios/mocks or live test selectors；不修改MCP control plane E2E除必要source fixture。
- Repository evidence: existing gateway-admin E2E覆盖Applications/Catalog/Route flows；Spec TEST-118–121。
- Dependencies and consumers: completed backend/frontend；user-controlled runtime。
- Why now: unit/module proof完成后才定义端到端验收，不把E2E失败用production workaround掩盖。
- Contract/signature changes: test-only selectors/fixtures；route/release remains manual。
- Input/output and state mapping: mocked/live multi-group statuses→row/detail/tab；OpenAPI sync→Catalog definition but route list unchanged。
- Error and edge behavior: one invalid group shows aggregate failure/old set；retry refresh；RPC operation noOpenAPI；unauthorized hidden/denied；raw download exact。
- Standards impact: `MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001`；runtime proof标明用户控制。
- Literal rule enforcement: `Rule 2` UI/API权限与ID boundary；`Rule 5`复用Playwright；`Rule 6`JSON/download assertions；`Rule 7`runtime config prerequisite；`Rule 11`existing E2E location。
- Implementation pseudocode:

```typescript
open Applications and assert one application row shows aggregate identity and worst state for two advertised groups
expand group details, inspect hashes/errors, navigate to an OPENAPI31 operation and verify fragment, copy and download flows
assert an RPC_DESCRIPTOR operation shows no OpenAPI projection and an optional-mcp HTTP operation remains catalog-visible
after sync fixture change assert Catalog updates while Draft and Release routes remain unchanged until the existing manual workflow runs
```

- Verification contribution: user-controlled full flow acceptance；not executed automatically by Plan writer。
- After this file: E2E acceptance定义完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng -am verify && cd egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web && npm test -- --run && npm run typecheck && npm run lint && npm run build`
- Expected result: Gateway reactor verify与Admin Web static/component gates exit 0；architecture/residual/source/RPC/MCP/Route boundary无回归。Playwright live E2E只在用户启动所需拓扑后按§8单独执行。
- Failure returns to: File 1 architecture owningStep；File 2 RPC/MCP owningSteps 6/10；File 3 backend/frontend owningSteps 11–13；不得在Step14新增compat shim。
- Completion criteria: 所有REQ trace有focused/full gate；旧HTTP residual为零；RPC/Engine/Route/Release保持；无未闭合blocker。
- Rollback: revert测试文件只撤销验收守卫，不应作为production fix；任何production失败回原Step forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/test/java/top/egon/cola/component/gateway/admin/architecture/GatewayAdminPackageArchitectureTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpFixtureContractTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/e2e/gateway-admin.spec.ts`, companion RPC/E2E fixture assertions if source literals change
- Commit: `test(gateway): close openapi aggregation regression gates`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED/GREEN 1 | repository root | Step 1 exact Maven command | Contract/module graph | enum/Manifest tests pass，dependency isolation | Step 1 File 1–3 | `REQ-003,017,020`; compile |
| RED/GREEN 2–3 | repository root | common/stack module `test` + dependency tree | Provider adapter | customizers/8-key manifest/security/stack isolation pass | Steps 2–3 | `REQ-001–005,016,017,022`; module |
| RED/GREEN 4–6 | repository root | provider/Admin/starter focused tests + residual scans | breaking migration | per-group Golden pass；old HTTP types zero；RPC Golden unchanged | Steps 4–6 | `REQ-001–004,009,010,016,020–022`; cross-module |
| Migration | repository root | Step 7 schema + Testcontainers tests | V12/PostgreSQL | 12 migrations；two tables；multi-snapshot link；CAS | Step 7 | `REQ-006,007,011,013,018,024`; integration |
| Security | repository root | Step 8 focused tests | OAuth2/SSRF/validation | all hostile inputs fail closed, no secrets | Step 8 | `REQ-004,005,021,025`; unit/contract |
| Mapping | repository root | Step 9 adapter/converter tests | canonical/Report v2 | deterministic sha, exact schemas/responses | Step 9 | `REQ-006,008,021–023`; unit/Golden |
| Transaction | repository root | Step 10 aggregate/ingestion tests | one set/all groups | rollback/recovery/source/MCP pass | Step 10 | `REQ-007,010,018–021,024`; service/integration |
| Lifecycle/API | repository root | Steps 11–12 tests | CAS/retry/stale/read API | defaults/states/recovery/security pass | Steps 11–12 | `REQ-004–007,011,012,017,019,024–026`; service/MVC |
| Frontend | Admin Web directory | Step 13 npm command | client/components/static build | tests/typecheck/lint/build exit 0 | Step 13 | `REQ-012,020,026`; browser-independent |
| Full reactor | repository root | `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-yuheng -am verify` | all Gateway Java modules | exit 0；RPC/MCP/Engine/Route/Release regressions pass | owning Step | all backend；full module，不是live topology |
| Full frontend | Admin Web directory | `npm test -- --run && npm run typecheck && npm run lint && npm run build` | Admin Web | exit 0 | Step 13/14 | UI static/component |
| User-controlled E2E | Admin Web directory after user starts DDC/IdP/Providers/Admin | `npm run test:e2e -- e2e/gateway-admin.spec.ts` | live/mocked configured E2E | grouped docs/sync/UI/no-auto-route observed | owning backend/frontend Step | `REQ-001,005,012,014,018,026`; runtime only |
| Final static | repository root | `rg` residual + `git diff --check` + `git status --short` | source/dependencies/worktree | no old HTTP/STARTER source literals；no whitespace errors；only intended paths | owning Step | `REQ-003,009,015,020`; static |

实施者每个Step先运行其focused RED并确认失败原因只是不具备目标行为，再做GREEN；本Plan作者仅校验Plan文档，不声称上述未来代码/运行测试已执行。

## 9. Migration, Compatibility, Rollout, and Rollback

1. 先发布包含Step 1–6的Provider/Contract breaking artifacts；所有业务Provider必须把旧HTTP annotations和base starter依赖一次切到对应MVC/WebFlux adapter。不存在dual-read、shim或deprecation window。
2. Admin deployment前创建fresh database并让Flyway从V1执行到唯一新V12；不backfill、不改V1–V11。数据从Provider/DDC自动重新写入。
3. Provider先配置标准Springdoc Group与`gateway.openapi.published-groups`，验证每Group 3.1 endpoint和DDC精确8 key；未完整的Provider不能进入Admin ingest。
4. Admin以`gateway.admin.openapi.enabled=false`部署schema/code后，确认OAuth2 scope、trusted DNS/CIDR、resource URI、timeouts/limits，再启用scheduler。
5. 启用后观察DISCOVERED→VALID完整集、所有group同definitionSetId、aggregate/worst metrics；任何group INVALID/INCONSISTENT时保留旧VALID set并修复Provider/build。
6. OpenAPI Definition激活只影响Catalog/MCP投影；Route/Draft/Release仍走现有人工流程。RPC继续signed Descriptor Report，不依赖Springdoc。
7. 应用回滚优先关闭OpenAPI sync并回退应用artifact；永久snapshots/definitions不删除。V12已发布后不得回改checksum，只能新增后续forward-fix migration。快速迭代环境如用户决定整体回退，可删除数据库并用目标版本fresh重建。
8. live E2E由用户启动拓扑后执行；本实施计划和未来自动Step不得自动启动项目、部署或删库。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | primary §4 | 1–5 | BOM/adapters/providers/Admin docs | adapter/provider/Admin Golden | grouped 3.1 docs |
| `REQ-002` | primary §4 | 2,4,5 | annotations/customizers/controllers/carriers | extension/residual tests | standard+x-egon only |
| `REQ-003` | primary §4 | 1,3,6,14 | POMs/adapters/base starter/architecture test | dependency tree/full verify | stack isolation |
| `REQ-004` | primary §4 | 2,4,8,11 | registration/provider/client/sync | 8-key/security/sync tests | bounded DDC capability |
| `REQ-005` | primary §4 | 3,8,11 | security adapters/client/sync | OAuth2/SSRF tests | safe healthy fetch only |
| `REQ-006` | primary §4 | 7,9,12 | V12/snapshot/adapter/query | schema/canonical/raw API | permanent raw/reproducible sha |
| `REQ-007` | primary §4 | 7,10,11 | repos/coordinator/ingestion/sync | drift/rollback/recovery | old set preserved |
| `REQ-008` | primary §4 | 9,10 | adapters/converter/ingestion | mapping/Golden/transaction | Report v2 complete |
| `REQ-009` | primary §4 | 5,6,14 | Admin migration/starter deletes/architecture | residual/dependency scan | zero legacy HTTP compiler |
| `REQ-010` | primary §4 | 6,10,14 | RPC starter/ingestion/MCP tests | RPC/Tool/Engine/full verify | existing semantics preserved |
| `REQ-011` | primary §4 | 7,11,12 | sync tables/repos/services/API | CAS/retry/stale/metrics | bounded recoverable sync |
| `REQ-012` | primary §4 | 12–14 | API/React/E2E | MVC/Vitest/E2E | operator visibility |
| `REQ-013` | primary §4 | 7 | V12/schema tests | Flyway/Testcontainers | one migration/fresh DB |
| `REQ-014` | primary §4 | 14 | E2E/route regressions | full/E2E | no automatic route |
| `REQ-015` | primary §4 | 5,14 | packages/architecture test | ArchUnit/static | feature-first only |
| `REQ-016` | amendment §4 | 2–4 | properties/adapters/provider configs | multi-group Golden | different apps/groups |
| `REQ-017` | amendment §4 | 1,2,7,8,11 | Manifest/registration/repos/sync | boundary/config tests | sorted bounded manifest |
| `REQ-018` | amendment §4 | 7,10,14 | DB/coordinator/ingestion/E2E | aggregate/transaction | one set/all groups |
| `REQ-019` | amendment §4 | 7,10,11 | constraints/coordinator/sync | duplicate/drift tests | fail closed/no partial |
| `REQ-020` | amendment §4 | 1,6,10,12–14 | enum/reporting/query/UI/tests | source pairing/full tests | exact three sources |
| `REQ-021` | amendment §4 | 2,4,8–10,14 | annotations/validators/adapters/MCP tests | optional MCP matrix | Catalog without mandatory Tool |
| `REQ-022` | amendment §4 | 2,4,5,9 | annotations/controllers/customizers/adapter | operationId/group Golden | exact governance contract |
| `REQ-023` | amendment §4 | 9 | canonical/adapters | response/media/canonical tests | deterministic mapping |
| `REQ-024` | amendment §4 | 7,10,11 | repos/ingestion/sync | rollback/CAS repair | atomic link/recoverable state |
| `REQ-025` | amendment §4 | 8,11 | client/properties/scheduler | hostile/default/config tests | exact SLO/security defaults |
| `REQ-026` | amendment §4 | 12–14 | VO/API/pages/E2E | MVC/Vitest/E2E | aggregate/group UI |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `DEC-001` | 立即破坏性切换、fresh DB、无历史兼容 | Steps 4–7/rollout | 用户明确允许删库重建 | User | Closed；无shim/backfill |
| `DEC-002` | 多业务系统显式1–16 groups，Gateway完整集聚合 | Steps 2,4,7,10–13 | amendment REQ-016–019/026，用户确认 | User | Closed |
| `DEC-003` | HTTP=OPENAPI31、RPC=RPC_DESCRIPTOR、MANUAL三值 | Steps 1,6,10,13–14 | amendment REQ-020 | User | Closed |
| `DEC-004` | MCP缺省可选、enabled才投影 | Steps 2,4,8–10,14 | amendment REQ-021 | User | Closed |
| `DEC-005` | Springdoc 2.8.17 + JDK HttpClient + OAuth2/SSRF双检 | Steps 1–3,8,11 | primary/amendment decisions | User | Closed |
| `RISK-001` | Springdoc BOM与Boot BOM dependency drift | Step 1 POMs | current platform BOM order | Maintainer | Closed by dependency tree/convergence gate |
| `RISK-002` | Group overlap/manifest drift阻断整build | Steps 4,10,11 | accepted fail-closed tradeoff | Provider owner | Closed by Golden/global duplicate/drift tests |
| `RISK-003` | Ingestion成功后部分sync CAS丢失 | Steps 10–11 | amendment DEC-A02/REQ-024 | Admin | Closed by linked-set repair |
| `RISK-004` | DNS在双检与connect间变化 | Step 8 | accepted residual in amendment | Platform security | Closed for V1 by trusted DNS/all-address checks/HTTPS/monitoring |
| `RISK-005` | Testcontainers/live E2E环境不可用 | Steps 7/14 | environment-dependent gates | User/CI | Closed planning-wise；静态/module不能替代，运行时按§8单独报告 |
| `RISK-006` | 机械Admin annotation迁移扩成Bean全量重构 | Step 5 | `PLAN-CLAR-004` | Maintainer | Closed；仅metadata，业务Bean规则用于新/实质变更Bean |
| `RISK-007` | 并行dirty docs误入commit | Every Step | current `git status --short` | Implementer | Closed by path-limited staging/status gate |

无未解决公共API、安全、事务、数据库、来源、Group或MCP决策；若实施baseline发生这些领域的漂移，Plan状态必须降级并回Spec，不能在代码中自行推断。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

14个Steps覆盖用户确认的全部A选择，并把不同业务系统多Group、Gateway完整集聚合、HTTP/RPC可扩展来源及MCP可选扩展落实到Contract、Provider、DDC、Admin、DB、UI和回归。破坏性/fresh DB决定明确，无历史兼容或backfill；不自动启动项目。

### 12.2 Spec consistency

Plan保持primary+amendment的三adapter、精确8 key、三值source、V12两表、多snapshot→一Definition Set、事务外fetch、事务内definition+links、事务外sync CAS、optional MCP、只读API/UI及Route/RPC边界。Simplicity audit拒绝RPC伪OpenAPI、动态插件、第三aggregate表、caller URL和未命名exception hierarchy；四个Clarification均为局部可逆实现细节，无fetch-then-forward或新增业务契约。

### 12.3 Repository executability

所有模块、POM、现有controllers/tests/migrations/frontend paths均在baseline核验；删除顺序在consumer迁移之后；V12版本为下一号；命令使用repository Maven wrapper和现有npm scripts。每Step明确baseline/end state、RED/GREEN、file order、failure return、cwd、commit paths和单一semantic commit；unrelated dirty docs不进入实现commit。

### 12.4 Test and release completeness

focused contract/security/mapping/CAS/transaction/API/UI tests逐层先行；dependency/static/architecture/full reactor/frontend gates闭合；PostgreSQL与live E2E清楚标记环境/用户控制边界。rollout先Provider contract、再fresh DB/Admin disabled、再安全配置/enable；rollback优先disable，已发布migration只forward-fix。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | `Applicable` | `PASS` | §4.7 actual gateway/admin tree；Steps 1–14；architecture test | 唯一Traditional Layered feature-first profile，无hybrid | None |
| `MC-REUSE-001` | `Applicable` | `PASS` | §4.7 ledger；DDC/IdP/JDK/Jackson/Jdbc/Report/BaseConverter paths | 先复用后新增，capability gap明确 | None |
| `MC-DEP-001` | `Applicable` | `PASS` | Step 1/3/6 dependency-tree gates | 只新增批准Springdoc/MapStruct/Testcontainers，no UI/parser/cross-stack | None |
| `MC-NAME-001` | `Applicable` | `PASS` | §5 inventory；Steps 1,7–12 types | DTO/PO/VO/Enum/behavior names语义明确 | None |
| `MC-VALID-001` | `Applicable` | `PASS` | §4.8 Rule2；每Step boundary/negative tests | config→DDC→client→chain→adapter→ingestion→repository→API均验证 | None；phone N/A因无phone字段 |
| `MC-MODEL-001` | `Applicable` | `PASS` | Steps 1,7–10,12 record blocks | 新carrier全为simple records+compact ctor，无复杂data class | None |
| `MC-CONVERT-001` | `Applicable` | `PASS` | Step 9 MapStruct mapper + common-core BaseConverter；Step14 architecture | structural conversion不手写/不JSON round-trip；algorithm Adapter分离 | None |
| `MC-LOG-001` | `Applicable` | `PASS` | Steps 2,3,7–12 business Bean blocks | 新/实质变更业务Bean均计划@Slf4j且日志安全 | None |
| `MC-BEAN-001` | `Applicable` | `PASS` | Step1 lombok.config；business file pseudocode/context tests | explicit Bean names、RequiredArgsConstructor、final Qualifier传播 | None；Step5 metadata-only按Clarification 004 |
| `MC-UTIL-001` | `Applicable` | `PASS` | §4.7 ledger；Step8 dependency/import scan | JDK/Spring/Jackson/approved components only，无duplicate Utils | None |
| `MC-JSON-001` | `Applicable` | `PASS` | Contract/Golden/canonical/API/frontend tests | Spring Boot Jackson+Swagger models，wire annotation/default明确 | None |
| `MC-TIME-001` | `Applicable` | `PASS` | V12 TIMESTAMPTZ；PO/VO/Service Clock/Instant/Duration | 无java.util Date/Calendar，UTC/precision有tests | None |
| `MC-CONFIG-001` | `Applicable` | `PASS` | Steps 2,4,11 properties/YAML parity tests | base/local/provider keys一致，值可不同 | None |
| `MC-PATTERN-001` | `Applicable` | `PASS` | §4.7；Steps 8–11 | Adapter、Chain、Coordinator、Facade、CAS解决真实复杂variation | None |
| `MC-SCOPE-001` | `Applicable` | `PASS` | §3/§5/每Step commit paths；§11 | exact Gateway OpenAPI闭环；RPC/Route/Engine边界和dirty docs保留 | None |
| `MC-TEST-001` | `Applicable` | `PASS` | §8及14个Step exact commands | RED/GREEN、migration/security/persistence/frontend/full/runtime boundaries完整 | None |
| `MC-BLOCKER-001` | `Applicable` | `PASS` | §6.3 decisions、§6.4 clarifications、§11 all Closed | 无FAIL/BLOCKED/UNKNOWN或待用户决定 | None |

### 12.6 Final verdict

PASS — Ready for user review
