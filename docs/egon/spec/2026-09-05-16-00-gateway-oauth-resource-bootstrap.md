# Gateway 三个 OAuth Resource Server 的本地身份种子修正

| Field | Value |
| --- | --- |
| Document | `2026-09-05-16-00-gateway-oauth-resource-bootstrap.md` |
| Template Version | `7` |
| Status | `Accepted` |
| Type | `Bugfix` |
| Complexity | `Simple` |
| Complexity Drivers | 声明式 local 种子变更；身份唯一性和已有数据保护 |
| Created | `2026-09-05 16:00 CST` |
| Updated | `2026-09-05 16:20 CST` |
| Owner | 用户 / Codex |
| Repository | `Egon-COLA` |
| Scope | IdP development bootstrap 中三个 Gateway 身份及 MCP 服务授权的先决修正 |
| Change Surface | `IdpDevelopmentClientBootstrap.java` 的种子和 MCP grant ID 前缀、必要构造注入规范化；对应测试、模块 lombok.config/POM |
| Affected Chapters | §7, §8, §14, §15, §16, §17, §18 |
| Source Requirement | 用户：“两个gateway engine 在 oauth2 resource 中当作两个server…gateway admin server也是一个单独的 resource server”；已批准继续修复 |
| Baseline Revision | `main@4237b6bdbc95de37c58296e1bdf01890040f74b2`；其他脏文件受保护 |
| Amends | [双 Engine 分离](2026-09-02-19-52-gateway-dual-engine-separation.md) §3.2、§8、§15、§16.4 的“不修改 IdP/只用原 Engine 身份”范围：允许本文件定义的 local 种子修正 |
| Supersedes | `None` |
| Depends On | [双 Engine 分离](2026-09-02-19-52-gateway-dual-engine-separation.md) §7.1、§8 的固定 API_RPC/MCP 可执行边界 |
| Related Specs | `None` |
| Related Plans | [身份种子实施计划](../plan/2026-09-05-16-00-gateway-oauth-resource-bootstrap-implementation.md)；[主实施计划](../plan/2026-09-02-21-03-gateway-dual-engine-separation-implementation.md) |

## 1. Summary

为 Gateway 的三个进程身份补齐互不冲突的 Resource Server 与 confidential Client。本修复保留已有 API/RPC Engine 和 Gateway Admin 定义，新增 MCP Engine 定义，并把新建 MCP Task 服务授权归到 MCP Client。幂等启动不能重置正确的资源、已有密钥或遗留授权。

这是继续父任务的身份先决修复，不声称已完成 Gateway 发布 fan-out。DDC 的 source app 绑定不放宽；Gateway 两个目标的持久化发布、各自版本核对、脚本和真实 platforms/API/浏览器验收仍属于后续已获用户授权的工作。父 Spec 的“同 DDC app / 同数字版本”约束不能再作为最终验收依据，须在下一份分发修订中正式替换后才能进行切换。

## 2. Background and Current State

### 2.1 Business and user context

用户已明确选择一个 Admin 与两个 Engine，也批准三个独立 OAuth Resource Server。本地引导目前只有 API Engine 和 Admin，MCP Task 仍使用 API Client。

### 2.2 Repository evidence

| Evidence ID | Classification | Exact path/symbol/decision/command | Observed fact | Design significance | Verification limit/freshness |
| --- | --- | --- | --- | --- | --- |
| EVD-001 | Static repository | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java` MACHINE_CLIENTS/RESOURCES | 已有 Admin 与 API 两组；缺 MCP | 只补第三组 | 2026-09-05 静态，非运行时 |
| EVD-002 | Static repository | 同文件 MCP_TASK_SERVICE_CLIENT / mcpTaskServiceGrantId | 指向 gateway-engine-service；ID 只有 tenant hash | 改 Client 时必须换 ID 前缀，防止旧记录 PK 冲突 | 未接数据库 |
| EVD-003 | Static repository | IdP admin resource/service/impl/ResourceServerServiceImpl.ensureResourceIsUnique；db/migration/V2__add_oauth_resource_servers.sql | biz/app/env 与 management Client 唯一 | 新 app 与 Client，不放宽校验 | Schema 不变 |
| EVD-004 | User decision | 本轮“两个server…admin…单独的 resource server” | 三个身份独立 | 已获执行授权 | 不授权重置既有数据库 |

### 2.3 Problem statement and gap

缺少 MCP 独立 Resource 会使它以 API Client 获取的 token 带 API 的 source app，无法同时满足独立管理身份与 DDC app 严格绑定。仅加显示名称不解决问题。

### 2.4 Evidence and current-chain map

| Entry/trigger | Current call chain | Data read/written | External dependency | Consumers | Evidence |
| --- | --- | --- | --- | --- | --- |
| local profile + enabled | afterSingletonsInstantiated → machine reconcile → resource reconcile → grants | 既有 Client/Resource/Grant；本地 secrets | 既有 IdP Service/Repository | 三个 Gateway 服务 | EVD-001/002 |

## 3. Goals and Non-goals

### 3.1 Goals

独立身份、MCP 最小授权、幂等及旧数据保护。

### 3.2 Non-goals

不修改 IdP token 签发算法、DDC 注册校验、Schema、历史 Flyway、用户/角色权限；不在这个先决提交中修改 Gateway 发布、进程启动或 UI。后续任务不会因本次先决完成而省略。

### 3.3 Change Surface and Design Depth

| Area/layer | Disposition | Exact repository evidence | Changed or preserved behavior/contract | Required Spec treatment | Chapter(s) |
| --- | --- | --- | --- | --- | --- |
| local 声明式种子 | Affected | IdpDevelopmentClientBootstrap 的四个列表、Task Client/ID 前缀 | 增加 MCP 身份与授权 | 精确映射与失败边界 | §7, §8, §15, §16 |
| bootstrap 测试 | Affected | IdpDevelopmentClientBootstrapTest | 三身份、租户、幂等、冲突覆盖 | RED/GREEN | §14 |
| REST/RPC/Service 签名 | Unchanged | 既有 OAuthClientService/ResourceServerProjectionService | 无新入口、字段或参数 | 明确不改 | §9 |
| POJO/Converter | Context-only | 私有 record 与 IdentityResourceServerEntity | 仅新建既有 record 实例；不改类型 | 边界说明 | §10 |
| 数据库 | Context-only | IdP V2 既有唯一约束、Grant Entity 工厂 | 使用现有 local 引导 CRUD；无 schema、迁移或批量回填 | 保留不变量 | §11 |
| Web 页面 | Unchanged | IdP resource 管理页；Gateway Web | 页面自动读取已有 Resource API | 不设计新页面 | §12 |
| 模式选型 | Context-only | 既有数据驱动 reconcile | Simple 参数/常量修正，算法不变 | 拒绝仪式化模式 | §13 |
| 兼容/安全/风险 | Affected | 三个 owner Client；旧 grant ID | 不共享源身份、不覆盖旧数据 | 明确切换与限制 | §15, §16, §17, §18 |

## 4. Requirements and Acceptance Criteria

| ID | Requirement | Acceptance |
| --- | --- | --- |
| REQ-001 | Admin、API/RPC、MCP 分别拥有 Resource/Client/app | 测试精确断言 §7 三行全部字段、三组唯一，且保留 Admin/API 字段 |
| REQ-002 | MCP 拥有本地 DDC 注册、RBAC 租户服务、MCP Task 调用授权 | DDC PLATFORM 无 tenant；RBAC/Task 按每个配置 tenant，scope 不扩大；不向 MCP 授 Admin 管理 scope |
| REQ-003 | 不覆盖旧数据；引导可重入且错误身份拒绝 | 正确资源与 active secret 不重建/轮换；旧 API Task grant ID 不复用；错误 Resource owner/app 抛现有异常 |
| REQ-004 | 固定授权和兼容边界 | token/校验/schema/profile 条件不变；单元验证与 live 验收分开 |

### 4.1 Scenario matrix

| 场景 | 结果 | Requirement |
| --- | --- | --- |
| 空种子 | 创建 MCP confidential Client、Resource 及准确授权 | REQ-001/002 |
| 多 tenant | 每 tenant 独立 RBAC 与 Task grant；DDC 仍 PLATFORM | REQ-002 |
| 重启/资源已存在 | 复用正确资源与 secret；不重复写正确授权 | REQ-003 |
| 旧 Task grant 已有 | 新 MCP ID 不冲突；不删除或重绑旧 API grant | REQ-003 |
| 资源 owner/app 错误 | fail closed，现有 IllegalStateException，不自动覆盖 | REQ-003/004 |

### 4.2 Use-case analysis

| Actor ID | Actor/role | Goal and responsibility | Entry/channel | Permission/tenant context | Evidence |
| --- | --- | --- | --- | --- | --- |
| ACTOR-001 | 本地平台操作员 | 引导三个服务身份 | local bootstrap | local + enabled；配置 tenant 集 | EVD-001 |
| ACTOR-002 | MCP Engine | 获得限定用途服务授权 | 既有 ServiceToken | 自己的 Resource/Client | EVD-002 |

| ID | Use case/goal | Primary actor | Supporting actors/systems | Trigger | Preconditions | Main success outcome | Alternatives/failures | Postconditions | Requirements | Interfaces/pages | Tests |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| UC-001 | 引导三个独立身份 | ACTOR-001 | 既有 IdP CRUD | local 启动 | enabled/合法 tenant | 三组独立 Resource/Client | 正确现有值复用；错误绑定拒绝 | 不覆盖/清理已有数据 | REQ-001/003 | 既有 afterSingletonsInstantiated | TEST-001/003 |
| UC-002 | 准备 MCP 最小授权 | ACTOR-002 | ACTOR-001/IdP | 种子协调 | MCP Resource/Client 已存在 | PLATFORM DDC 与逐 tenant RBAC/Task | 已有正确 grant 不重建；错误身份拒绝 | 无 Admin 管理 scope | REQ-002/004 | 既有 grant projection | TEST-002/004 |

## 5. Constraints, Assumptions, and Decisions

### 5.1 Confirmed constraints

严格三资源；保持 IdP/DDC 核心鉴权；只修改 §8 四个文件；保护脏工作区；每任务一次提交。

### 5.2 Small-gap assumptions

名称沿用现有 local 命名：gateway-mcp-engine-default / gateway-mcp-engine-service。均为可逆本地种子名称，不推断生产资源值。

### 5.3 Resolved decisions

DEC-IDENTITY-001：保留旧 Admin/API 行，新增 MCP 行。DEC-IDENTITY-002：Task 新 grant ID 加 mcp-engine 标识，不覆盖旧 ID。DEC-IDENTITY-003：种子变更同时落实 Rule 4 的本类 DI，不重写既有 reconcile 算法。五个依赖仍为原 Service/Repository；改用 Lombok 生成的七参数构造器，参数次序 clients/resources/grants/clientEntities/projections/secretDirectory/rbac3ServiceTenantIds。两个配置参数保留 String 与原 @Value 表达式；Path 规范化移至 writeSecret/secretFile，tenantIds 解析移至启动入口和原三个租户协调方法的局部变量。先校验 tenant 再做任何 CRUD。保留原配置键/default/local/开关，不触碰 YAML。

2026-09-05 16:20 构建证据修正：首轮 GREEN 编译发现 IdP admin 未声明 Lombok，而 platforms parent 已有 annotationProcessorPaths、root 已定义 lombok.version=1.18.46。Rule 4 不能省略。仅在本模块 POM 增加 org.projectlombok:lombok:${lombok.version}、provided、optional=true，不增加新版本或下游传递依赖。原“POM 不变”判断撤回；属于用户要求自行完成修复内的必要构建修正。

### 5.4 Open major decisions

本先决范围内 None。父任务的 fan-out 设计和实施仍须完成，但用户已批准方向，无须再问同一个架构问题。

## 6. Project Technology Context

Java 21、Spring Boot 3.5.16、Maven wrapper、JUnit5/Mockito 均使用当前仓库。不引入新版本；只补 root 已管理的 Lombok 构建依赖。

### 6.1 Java architecture profile and capability baseline

保留父 Spec 已批准的 feature-local traditional layered 架构；本次仅 support/bootstrap 使用现有 oauth/service 和 resource/repo。无新模块、包或类。复用 MACHINE_CLIENTS/RESOURCES 的 record 声明、Entity 工厂、requireMatchingResource、已有 secret 原子写入和既有 grant reconciliation。BaseConverter、ValidationUtils 可用但此常量修正不新增转换或层间契约，禁止为形式创建 Mapper。

### 6.2 User-mandated Java rule compliance

| Literal rule | Affected? | Repository evidence | Exact design decision | Files/types/interfaces | Validation/test evidence | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | No | §2 EVD-001/002；§8 四个目标文件 | 不新增类型或修改既有 record 声明；仅种子实例 | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | N/A |
| Rule 2 | Yes | §2 EVD-001/002；§8 四个目标文件 | 保留 requireMatchingResource、机密 Client 类型校验与既有 Service/Repo 契约；增加不匹配拒绝测试；无新层间输入对象或复用组 | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | PASS |
| Rule 3 | No | §2 EVD-001/002；§8 四个目标文件 | 既有 record、Entity 与映射代码不变；无 Converter | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | N/A |
| Rule 4 | Yes | bootstrap 现有手写构造注入不满足 touched-class 门禁 | bootstrap 添加 @Slf4j、显式 Bean 名、@RequiredArgsConstructor；五个 final 依赖标 @Qualifier，保留两项 @Value；模块 lombok.config 复制注解，删除手写注入构造器 | IdpDevelopmentClientBootstrap、模块 lombok.config | 构造器元数据/编译/配置默认值断言 | PASS |
| Rule 5 | Yes | §2 EVD-001/002；§8 四个目标文件 | 仅既有 JDK 工具；测试 JUnit/Mockito；不引入新库，仅声明已管理 Lombok 构建依赖 | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | PASS |
| Rule 6 | No | §2 EVD-001/002；§8 四个目标文件 | 不改外部 JSON 字段或 Jackson 行为 | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | N/A |
| Rule 7 | No | §2 EVD-001/002；§8 四个目标文件 | 不改任何 profile 的配置键 | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | N/A |
| Rule 9 | Yes | §2 EVD-001/002；§8 四个目标文件 | Simple 声明式种子修正，复用 reconcile；未引入复杂业务规则 | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | PASS |
| Rule 10 | Yes | §2 EVD-001/002；§8 四个目标文件 | 时间仍用 Instant；测试固定 Instant.EPOCH | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | PASS |
| Rule 11 | Yes | §2 EVD-001/002；§8 四个目标文件 | 保留父 Spec 批准的 feature-local traditional layered 配置支持边界，无包迁移 | IdpDevelopmentClientBootstrap/测试 | TEST-001–004 和最终 diff 范围审查 | PASS |

## 7. Architecture Design

### 7.0 Minimum-design baseline and element-necessity audit

| Proposed element | Change | Requirements | Existing/direct alternative | Concrete inadequacy of alternative | Added calls/state/coupling/failures/migration/operations | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| MCP Client/Resource 种子行 | New | REQ-001 | 既有 record/reconcile | 共享 API Client 违反独立 owner | 一个 Client/Resource；已有 CRUD；无新 schema | Add |
| Grant 协调 | Keep | REQ-002/003 | 原数据驱动列表 | None，足够表达新增成员 | 新 MCP 的既有类型授权；不删旧数据 | Keep |
| 新 bootstrap/Strategy/API | Remove | REQ-004 | 现有机制 | 无缺口 | 避免新组件/接口/调用链 | Remove |

| Path | Network calls | Client states | Server contracts/state | Failure and TOCTOU points | Additional user/business value |
| --- | --- | --- | --- | --- | --- |
| Direct baseline | 引导不直接发网络请求，既有 Service/Repo 调用 | 缺少 MCP | 只有 Admin/API Gateway 身份 | MCP source app 错配 | 不满足三身份 |
| Selected design | 同一 Service/Repo 路径增加一组身份；不新增网络契约 | 新 MCP 及既有幂等状态 | 原实体/约束/投影 | 既有错误绑定拒绝；新 Task ID 避免旧 PK 冲突 | 三个独立 owner | 

### 7.1 System Architecture Design

仅修改 existing bootstrap → existing IdP CRUD 的输入种子，不修改 token/DDC 边界。

| Process | resourceServerId | resourceUri | bizCode/appCode/environment | managementClientId |
| --- | --- | --- | --- | --- |
| Admin | platform-gateway-admin-local | https://api.egon.internal/local/platform/gateway-admin | platform/gateway-admin/local | gateway-admin-service |
| API_RPC | identity-gateway-engine-default-local | https://api.egon.internal/local/identity/gateway-engine-default | identity/gateway-engine-default/local | gateway-engine-service |
| MCP | identity-gateway-mcp-engine-default-local | https://api.egon.internal/local/identity/gateway-mcp-engine-default | identity/gateway-mcp-engine-default/local | gateway-mcp-engine-service |

MCP displayName 为 Gateway MCP Engine Local，Client name 为 Gateway MCP Engine Local Service；沿用 rbacApplicationCode=mock-backend、entryPermissionCode=mock:read，userClientId=null。API 和 Admin 原值不变。

### 7.2 High-Level Design

Bean 使用 @Component("idpDevelopmentClientBootstrap")、@Slf4j、@RequiredArgsConstructor。五个依赖 Qualifier 依次 oauthClientServiceImpl、identityResourceServerRepository、identityClientResourceGrantRepository、identityClientRepository、resourceServerProjectionService。模块 lombok.config 设置 stopBubbling=true，复制 Spring Qualifier 和 Value；构造器元数据测试核对准确值。成功日志不含 secret。无参入口先调用既有 tenantIds 校验，保持错误配置在副作用之前拒绝。

增加 MACHINE_CLIENTS 和 RESOURCES 的 MCP 行，在 RBAC3_SERVICE_CLIENTS、DDC_REGISTRATION_CLIENTS 增加 MCP Client；MCP_TASK_SERVICE_CLIENT 改为 MCP Client。MCP 不接收 Admin 的 gateway:* scope，不需要 API Cookie refresh-status scope。

### 7.3 Detailed Design

Task 授权仍为 identity-gateway-test-mcp-provider-local，scope=mcp:operation:invoke，按 configured tenant。新 ID 为 dev-mcp-engine-task-grant- + 原 tenant UUID hash 前八位；旧 dev-mcp-task-grant- 不改、不删除、不重绑。

DDC 授权仍用 PLATFORM、tenant=null、scope=ddc:registration:write；MCP source app 由自身 Resource 推导，后续 DDC 配置必须同 app。RBAC 仍为 service:authorization:decide、service:authorization:snapshot、service:identity:resolve 三项，逐 tenant。

重复启动沿用 find/exact-match，不创建新轮换策略；confidential Client 或 Resource 不匹配沿用现有失败。不会自动部署新配置，也不发起真实服务启动。

## 8. Package Structure and Code File Tree

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java  MODIFY
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java  MODIFY
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/lombok.config  CREATE
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml  MODIFY
```

测试先锁定契约；生产修改种子、Task ID 前缀和 Rule 4 必需的本类 DI 规范化。新增 `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/lombok.config` 复制 Qualifier/Value，修改 `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml` 补 Lombok provided/optional 构建依赖；无新业务类型、转换器、Bean 数量、接口或 schema。

## 9. Interface Definitions

Unchanged：IdP 的 OAuth/Resource REST API、OAuthClientService#create、ResourceServerProjectionService#projectResource/#projectServiceGrant 和 bootstrap 的 afterSingletonsInstantiated 签名、输入校验、错误类型不变。只是 local 种子数量和值改变；不增加/扩大面向消费者的接口，不需要生成新的 OpenAPI operation 或 SDL。

## 10. POJO and Data Model Design

Context-only：MachineClientSpec、ResourceSpec 为既有私有 record，字段/构造器不变；IdentityResourceServerEntity、IdentityClientResourceGrantEntity 的工厂与映射原样复用。无新 PO/VO/DTO/Converter，Jackson 契约不变。

## 11. Database Design

Context-only：使用 IdP 现有表和 CRUD，资源的 biz/app/env 与 management_client 唯一约束不变；Grant 主键与 exact lookup 不变。这里是已有 local-enabled 的开发种子扩充，不是生产数据迁移、schema 更改或历史数据回填，不新增 Flyway，也不改任何旧版本。特别通过新 Task ID 前缀避免错误覆盖旧授权主键。

## 12. Frontend Page Design

Unchanged：现有 IdP Resource 页读取既有 Resource 列表即可看到第三组。Gateway OpenAPI 页和 RBAC 角色授权的 runtime 修复仍由父任务处理，本次不声称已验收。

## 13. Design Patterns and Architecture Principles

Context-only：本次为 Simple 声明式列表修正，沿用已有 reconcile。已考虑 Strategy/Factory：没有新增算法或多态行为，因此拒绝新模式类。复杂 fan-out 不是本次写入范围，将由独立修订采用现有 Coordinator 扩展。

## 14. Test Design

| ID | Test/fixture | Assertions |
| --- | --- | --- |
| TEST-001 | createsOnlyMissingPublicClients 增补 | Admin/API 旧字段与 MCP 新字段精确相符；新 Client confidential |
| TEST-002 | configured tenant 与 grant captors | MCP DDC PLATFORM 无租户，RBAC/Task 逐 tenant；新建 grant 不使用 API Client，也无 Admin 管理授权 |
| TEST-003 | existing resources/clients/secrets mock + @TempDir | 正确对象不 save、不 rotate；旧 Task ID 不复用；错误 app/owner 抛错，不保存覆盖 |
| TEST-004 | 原 bootstrap 三个测试和 IdP/DDC 鉴权测试 | 原 redirect/tenant 授权行为不回退，鉴权机制不变 |

验证命令（repo cwd）：
```bash
./mvnw -pl :egon-cola-tianquan-shoubing-admin -am -Dtest=IdpDevelopmentClientBootstrapTest -Dsurefire.failIfNoSpecifiedTests=false test
```
新增断言先 RED；生产修正后 GREEN。临时 secret 为测试生成值、Mock Repository，无真实 DB/IdP/浏览器。不能将此作为 full platforms 验收。

## 15. Non-functional and Cross-cutting Design

安全：独立 Client 与资源三元组；不增加 wildcard/admin scope；不记录 secret。持久化：不清空数据库；只由既有 local 引导创建缺失记录。时间和工具沿用 JDK Instant/Files/UUID。profile 开关及默认值不变。权限冲突保持 fail closed。

## 16. Compatibility, Migration, Rollout, and Rollback

先交付身份种子代码，再交付 Gateway 目标发布、配置与启动脚本；在所有先决通过前不做 runtime cutover。已有正确 Admin/API 身份保持；已有 API Task grant 留存不自动撤权，避免破坏正在使用的旧流程。首次切换后可另行审计旧 grant，不能未经授权批量删除。

代码回退采用单个修正提交的人工逆向变更；不删除新 Resource/Client/secret，更不重写 Flyway。生产环境不运行 local bootstrap。本 Spec 的通过只表示种子修复已定义，不表示用户的整体测试目标完成。

## 17. Alternatives and Decisions

共享 API Resource/Client：拒绝，违反独立 owner。放宽 IdP 唯一约束/DDC source 绑定：拒绝，扩大安全边界。新增独立 bootstrap 实现：拒绝，现有列表机制充足。直接复用旧 Task ID：拒绝，会与旧 API grant 主键冲突。

## 18. Risks and Open Questions

| ID | Risk | Treatment |
| --- | --- | --- |
| RISK-IDENTITY-001 | 只补身份不能让旧单目标发布正确分发 MCP | 父任务下一阶段必须实施 fan-out，不能宣布双 Engine 验收完成 |
| RISK-IDENTITY-002 | 已有人为错误配置的 MCP 资源 | 既有 requireMatchingResource 拒绝；不猜测或覆盖 |
| RISK-IDENTITY-003 | 旧 Task grant 仍在历史数据库 | 保留数据，后续显式审计；新客户端不共享旧 grant |
| RISK-IDENTITY-004 | Mock 证明不等同 token/runtime 成功 | 后续全 platforms 验收检查真实三资源与 source app |

## 19. Traceability Matrix

| Requirement | Use case | Design | Files | Tests |
| --- | --- | --- | --- | --- |
| REQ-001 | UC-001 | §7 三身份表 | §8 bootstrap/测试 | TEST-001 |
| REQ-002 | UC-002 | §7.2/7.3 | 同 §8 | TEST-002 |
| REQ-003 | UC-001 | §7.3、§16 | 同 §8 | TEST-003 |
| REQ-004 | UC-002 | §9–§13、§15 | 同 §8 | TEST-004 |

## 20. Review and Acceptance

### 20.1 Original-request fidelity

三个 Resource Server 明确分开；此 Spec 只交付本地身份先决，不省略后续 Gateway fan-out、脚本与浏览器问题修复。

### 20.2 Repository and technical fidelity

已读两个目标文件及当前唯一性/source app 绑定证据；沿用当前分层、record、CRUD，无 API/DDL 扩展；仅补已管理 Lombok 编译依赖。

### 20.3 Cross-section consistency

三组名称、Client owner、授权 context/tenant/scope、ID 前缀与测试一一对应；旧身份与授权保留策略一致。

### 20.4 Relationship and effective-design review

用户最新决议授权本先决修正；父 Spec 其他进程边界保持。仅同 app/版本的后续正式修订与 fan-out 未交付，不会当作完成。

### 20.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 现有 IdP admin 的 support/bootstrap、oauth/service、resource/repo 分层；仅四个目标文件 | 不移动或引入架构 | None |
| MC-REUSE-001 | Applicable | PASS | IdpDevelopmentClientBootstrap 的 MACHINE_CLIENTS、RESOURCES、reconcileResourceAndGrant | 复用已存在的本地种子协调机制 | None |
| MC-DEP-001 | Applicable | PASS | IdP admin POM 缺 Lombok；platforms parent 已配置其处理器，root lombok.version=1.18.46 | 只补 provided/optional 的仓库已管理构建依赖 | None |
| MC-NAME-001 | Not applicable | N/A | 复用 MachineClientSpec/ResourceSpec；不新增或修改类型声明 | 无新 POJO 或行为类型 | None |
| MC-VALID-001 | Applicable | PASS | requireMatchingResource 和 confidential Client 检查保留；新增冲突测试 | 现有身份不匹配时拒绝，不覆盖 | None |
| MC-MODEL-001 | Not applicable | N/A | 既有私有 record 的组件、构造器和实体声明均不变 | 只新增种子实例 | None |
| MC-CONVERT-001 | Not applicable | N/A | createResource/reconcileResourceAndGrant 的既有参数映射不变 | 无新 Converter 或跨层映射 | None |
| MC-LOG-001 | Applicable | PASS | IdpDevelopmentClientBootstrap 使用 @Slf4j，成功日志仅记录完成，不记录 secret | Rule 4 的必要 touched-class 规范化 | None |
| MC-BEAN-001 | Applicable | PASS | 显式 idpDevelopmentClientBootstrap、@RequiredArgsConstructor、五个 @Qualifier、两项 @Value；新增 IdP admin lombok.config | 生成构造器注解与默认值需测试验证 | None |
| MC-UTIL-001 | Applicable | PASS | 仍为 JDK List/Set/UUID/Files；JUnit/Mockito 属于测试框架 | 不增加工具类 | None |
| MC-JSON-001 | Not applicable | N/A | 既有 allowedScopes 字符串构建、实体字段、序列化契约不变 | 无外部 JSON 契约修改 | None |
| MC-TIME-001 | Applicable | PASS | 既有 Instant 与测试 Instant.EPOCH | 不引入 java.util 日期 | None |
| MC-CONFIG-001 | Not applicable | N/A | @Profile(local) 和 development-bootstrap.enabled 原样保留；无 YAML 键变更 | 本次只种子声明 | None |
| MC-PATTERN-001 | Applicable | PASS | 现有数据驱动 reconcile 流程；只加入一个同形资源和 Client | Simple 常量修正，不引入 Strategy/Factory | None |
| MC-SCOPE-001 | Applicable | PASS | bootstrap/测试、模块 lombok.config、IdP admin pom.xml 四文件；不改其他业务类 | 范围锁定 | None |
| MC-TEST-001 | Applicable | PASS | IdpDevelopmentClientBootstrapTest RED/GREEN；身份/授权/重复启动/旧 ID 冲突/错误绑定 | 测试隔离使用 @TempDir 与 Mock | None |
| MC-BLOCKER-001 | Applicable | PASS | 本 Spec 仅身份种子先决修复；发布 fan-out 与真实验收在父任务继续 | 不将前置完成等同总体完成 | None |

### 20.6 Final verdict

PASS — Ready for user review

此 PASS 为已获用户批准方向下的先决设计审查，不是 runtime 或父任务完成证明。
