# Gateway Admin Domain Packaging and Page Scope Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Gateway Admin Java 改造成领域优先分包并清零全部生产嵌套类型，同时把 Gateway Admin Web 的全局 Biz/Namespace/Env/App Scope 改成页面独立筛选，保持认证和 HTTP 契约不变。

**Architecture:** 后端按 bootstrap/config/shared/auth/application/group/catalog/credential/scope/routing/release/rule/runtime/observability/reporting/mcp 纵向领域展开，每个领域内部使用 controller/domain/repository/service，数据类型进入 dto/vo/po/enums。前端只共享 DDC Binding 查询缓存，不共享选中值；每个页面通过 URL Query 拥有自己的筛选状态。实施采用后端七个提交、前端四个提交，每个提交必须可编译、可独立审查。

**Tech Stack:** Java 21、Spring Boot、Spring MVC/Security/Data JPA/JdbcTemplate、JUnit 5、AssertJ、Maven Wrapper；React 19、TypeScript 6、React Router 7、TanStack Query 5、Ant Design 6、Vitest 4、Vite 8。

## Global Constraints

- 唯一设计依据是 docs/superpowers/specs/2026-08-13-gateway-admin-java-type-and-page-scope-design.md；其中第 5 节是 165 个嵌套类型的权威迁移清单，第 5.6 节是现有顶层类型的权威归属规则。
- GET /api/v1/auth/bootstrap 与 GET /api/v1/gateway/admin/session 必须保留；路径、入参、响应、鉴权和后端 JWT/RBAC3 行为不变。
- Gateway Admin Web 继续通过 Session API 获取身份与 capabilities，不解析 JWT，不向 admin-web-shared 增加 JWT 身份解析。
- 不改变任何既有 HTTP 路径、JSON 字段、状态码、错误码、Validation、权限注解、数据库表/列/SQL或消息契约。
- Gateway Admin 的 src/main/java 最终不得存在嵌套 record、class、enum 或 interface；165 个基线嵌套类型全部成为独立顶层文件。
- 顶层不可变载体继续使用 record；DTO、VO、PO、Enum 使用规定后缀；包级辅助类型不得因迁移扩大为 public。
- 原嵌套私有类型若依赖宿主对 private 成员的特权访问，只把必要构造器、字段或方法降为同包可见，或通过同包构造器显式注入依赖；不得为方便迁移改成 public。
- 名称以 Store 结尾的仓储契约重命名为 Repository，以 Jdbc 开头并以 Store 结尾的实现重命名为 Jdbc Repository，Entity 后缀重命名为 PO；方法签名、事务边界和持久化语义保持。
- 每个非空生产 package 都提供符合现有中英双语风格的 package-info.java；只修正本次触及类型的 JavaDoc。
- 不新增依赖、数据库 migration、状态管理框架、GoF 模式、空 Facade/Factory/接口或无业务价值的抽象。
- 设计模式取舍固定为受控组件组合与单一职责；不引入 Strategy、Factory、Template Method 或全局 Scope Store。
- 不修改无关模块；提交前只暂存当前任务文件，保留用户或其他并行工作的修改。
- 不启动 Gateway Admin、Gateway Web、DDC、IdP 或 RBAC3，不打开浏览器；Playwright 只更新夹具源码，不执行。
- 最终硬门禁包含根 Maven reactor 全量 clean install 编译；不能用只构建聚合父 POM 的命令代替。

---

## Repository Map

### Authoritative roots

- Backend module: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
- Backend production root: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin
- Backend test root: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin
- Gateway reactor: egon-cola-platforms/egon-cola-platform-gateway/pom.xml
- Frontend module: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
- Cross-module Gateway tests: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite

### Target production ownership

| Domain | Controller | Domain data | Repository | Service |
|---|---|---|---|---|
| bootstrap/config | application entry and bean assembly | config.properties | — | security/configuration only |
| shared/auth | advice, resolver, auth endpoints | actor, session/error VO, idempotency PO/enum/exception | shared JDBC | JWT converter and auth support |
| application/group/scope | resource controllers | dto/vo/po/exception and physical scope key | JPA contracts | application/group/scope orchestration |
| catalog/credential | catalog and credential controllers | dto/vo/po/enums | JDBC contracts/implementations | catalog/credential/secret services |
| routing/release/rule | draft/release controllers and schedulers | dto/vo/po/enums/state | JDBC/JPA contracts | draft, release, publication, rule compilation |
| runtime/observability/reporting | runtime, management, message, OpenAPI, scheduled adapters | dto/vo/po/enums | JDBC/JPA contracts | projection, ingest/query, reporting |
| mcp | MCP controllers | dto/vo/po/enums/exception | JDBC/filesystem | existing MCP orchestration, unsplit |

### Progressive architecture guard

Task 1 creates GatewayAdminPackageArchitectureTest. During Tasks 1-6 it contains an exact allowlist of the remaining legacy nested-type hosts. Each domain task removes every migrated host from that set. The test fails if:

1. a host outside the allowlist declares a nested type;
2. an allowlisted host no longer has nested types but was not removed from the set;
3. a target DTO/VO/PO/Enum filename does not match its directory suffix.

Task 7 makes the allowlist empty and adds the final old-package/dependency/package-info checks. This lets every intermediate commit pass while guaranteeing the final global rule.

The production-class scan must load only top-level class files and then inspect each type:

~~~java
private static Stream<Class<?>> productionTopLevelTypes() throws Exception {
    Path root = Path.of(GatewayAdminApplication.class
            .getProtectionDomain().getCodeSource().getLocation().toURI())
            .resolve("top/egon/cola/component/gateway/admin");
    try (Stream<Path> files = Files.walk(root)) {
        return files
                .filter(path -> path.toString().endsWith(".class"))
                .filter(path -> !path.getFileName().toString().contains("$"))
                .map(GatewayAdminPackageArchitectureTest::className)
                .map(GatewayAdminPackageArchitectureTest::load)
                .toList()
                .stream();
    }
}

private static List<String> nestedTypeHosts() throws Exception {
    return productionTopLevelTypes()
            .filter(type -> type.getDeclaredClasses().length > 0)
            .map(Class::getName)
            .sorted()
            .toList();
}
~~~

---

### Task 1: Establish the Guard and Migrate bootstrap/config/shared/auth

**Files:**

- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/pom.xml
- Move/modify production: GatewayAdminApplication.java, GatewayAdminConfiguration.java, config/GatewayAdminProperties.java, security/GatewayAdminTransportSecurityValidator.java, infrastructure/security/GatewayAdminSecurityConfiguration.java, infrastructure/security/GatewayAdminJwtAuthenticationConverter.java
- Move/modify shared/auth production: application/GatewayAdminIdempotencyConflictException.java, application/GatewayAdminNotFoundException.java, application/IdempotencyStore.java, application/RequestAuditContext.java, domain/AdminActor.java, domain/GatewayAdminRevisionConflictException.java, infrastructure/persistence/JdbcGatewayParameters.java, infrastructure/persistence/JdbcIdempotencyStore.java, interfaces/management/GatewayAdminActorArgumentResolver.java, GatewayAdminExceptionHandler.java, GatewayAdminSessionController.java, GatewayAdminWebMvcConfiguration.java, GatewayAuthBootstrapController.java
- Create top-level types from spec §5.1/§5.5: auth.domain.vo.GatewayAdminSessionVO, shared.domain.vo.GatewayAdminErrorVO, shared.domain.vo.GatewayAdminFieldErrorVO, shared.domain.enums.AdminActorTypeEnum, shared.domain.po.IdempotencyPO, config.properties.GatewayAdminDdcProperties, config.properties.GatewayRuleChunkProperties
- Create package metadata under bootstrap, config, config/properties, shared/controller, shared/domain, shared/domain/vo, shared/domain/po, shared/domain/enums, shared/domain/exception, shared/repository, shared/repository/jdbc, auth/controller, auth/domain/vo, auth/service
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/architecture/GatewayAdminPackageArchitectureTest.java
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/shared/controller/GatewayAdminJsonContractTest.java
- Move/modify tests: GatewayAdminApplicationConfigurationTest.java, GatewayAdminConfigurationTest.java, security/GatewayAdminTransportSecurityValidatorTest.java, infrastructure/security/GatewayAdminHmacJwtDecoderTest.java, infrastructure/security/GatewayAdminJwtAuthenticationConverterTest.java, interfaces/management/GatewayAdminSecurityIntegrationTest.java, interfaces/management/GatewayManagementActorBoundaryTest.java

**Interfaces:**

- Consumes: Spring Boot component/JPA/repository scanning rooted at top.egon.cola.component.gateway.admin; approved mappings for the seven extracted types.
- Produces: bootstrap.GatewayAdminApplication, bootstrap.GatewayAdminConfiguration, config.GatewayAdminSecurityConfiguration, auth controller/session contracts, shared controller/error contracts, and the progressive architecture test used by Tasks 2-7.

- [ ] **Step 1: Add characterization tests before moving types**

Add exact JSON assertions for SessionView, ErrorResponse and FieldError using their current nested FQCNs. Assert key sets and representative values, not Java class names:

~~~java
assertThat(objectMapper.valueToTree(session))
        .isEqualTo(objectMapper.readTree("""
                {
                  "actorId":"admin-1",
                  "displayName":"Mario",
                  "actorType":"HUMAN",
                  "capabilities":["gateway:read"],
                  "roles":["gateway-admin"],
                  "expiresAt":"2026-08-13T10:00:00Z"
                }
                """));
~~~

Run:

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayAdminJsonContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS against the current nested response types, establishing the wire baseline.

- [ ] **Step 2: Add the progressive architecture test and verify it catches Task 1 debt**

Start from the exact 59-host baseline in Appendix A, remove these five Task 1 hosts from the expected set, and assert actual legacy hosts equal the remaining set:

~~~text
config.GatewayAdminProperties
domain.AdminActor
application.IdempotencyStore
interfaces.management.GatewayAdminExceptionHandler
interfaces.management.GatewayAdminSessionController
~~~

Run the same Maven command with Dtest=GatewayAdminPackageArchitectureTest.

Expected: FAIL listing precisely the five Task 1 hosts because they have not been migrated yet.

- [ ] **Step 3: Move bootstrap and preserve full Spring scanning**

Move the application entry and configuration into bootstrap. Because moving the application class below the admin root changes Spring Boot auto-configuration packages, configure all three scans explicitly:

~~~java
@SpringBootApplication(scanBasePackages = GatewayAdminApplication.ADMIN_PACKAGE)
@EntityScan(GatewayAdminApplication.ADMIN_PACKAGE)
@EnableJpaRepositories(GatewayAdminApplication.ADMIN_PACKAGE)
public class GatewayAdminApplication {

    public static final String ADMIN_PACKAGE =
            "top.egon.cola.component.gateway.admin";
}
~~~

Update spring-boot-maven-plugin mainClass to:

~~~xml
<mainClass>top.egon.cola.component.gateway.admin.bootstrap.GatewayAdminApplication</mainClass>
~~~

Move GatewayAdminConfiguration to bootstrap and update imports without changing bean names, conditions, Clock behavior or destroy methods.

- [ ] **Step 4: Extract and move config/shared/auth types**

Apply every row for the five hosts from spec §5.1 and §5.5. Preserve record component order, Validation/Jackson behavior, constructors and visibility. Rename:

~~~text
IdempotencyStore -> shared.repository.IdempotencyRepository
JdbcIdempotencyStore -> shared.repository.jdbc.JdbcIdempotencyRepository
IdempotencyStore.Record -> shared.domain.po.IdempotencyPO
AdminActor.ActorType -> shared.domain.enums.AdminActorTypeEnum
~~~

Keep GatewayAuthBootstrapController and GatewayAdminSessionController as controllers. Move only their packages and Session DTO references; do not remove either endpoint.

- [ ] **Step 5: Update tests and verify Task 1**

Change the characterization test imports to the new top-level VO classes; its expected JSON must remain byte-for-byte equivalent after ObjectMapper normalization.

Run:

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayAdminPackageArchitectureTest,GatewayAdminJsonContractTest,GatewayAdminApplicationConfigurationTest,GatewayAdminConfigurationTest,GatewayAdminTransportSecurityValidatorTest,GatewayAdminHmacJwtDecoderTest,GatewayAdminJwtAuthenticationConverterTest,GatewayAdminSecurityIntegrationTest,GatewayManagementActorBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS; the architecture test reports only the 54 explicitly allowed legacy hosts.

- [ ] **Step 6: Commit Task 1**

~~~bash
git status --short
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git diff --cached --check
git commit -m "refactor(gateway): establish admin domain package guard"
~~~

---

### Task 2: Migrate application/group/scope

**Files:**

- Move/modify production: application/GatewayApplicationService.java, application/GatewayApplicationAlreadyExistsException.java, application/GatewayGroupService.java, application/scope/GatewayScopeService.java, interfaces/management/GatewayApplicationController.java, GatewayGroupController.java, GatewayScopeController.java
- Move/rename persistence: infrastructure/persistence/GatewayApplicationEntity.java to application/domain/po/GatewayApplicationPO.java; GatewayApplicationRepository.java to application/repository/GatewayApplicationRepository.java; GatewayGroupEntity.java to group/domain/po/GatewayGroupPO.java; GatewayGroupRepository.java to group/repository/GatewayGroupRepository.java
- Create all top-level DTO/VO/domain files for the five hosts from spec §5.1, §5.2 and §5.5
- Create package-info.java in each nonempty application/controller/domain/dto/domain/vo/domain/po/domain/exception/repository/service, group/controller/domain/dto/domain/vo/domain/po/repository/service, and scope/controller/domain/domain/dto/domain/vo/service package
- Move/modify tests: application/GatewayApplicationServiceTest.java to application/service/GatewayApplicationServiceTest.java; application/scope/GatewayScopeServiceTest.java to scope/service/GatewayScopeServiceTest.java; update auth/controller/GatewayAdminSecurityIntegrationTest.java and shared/controller/GatewayManagementActorBoundaryTest.java
- Modify: architecture/GatewayAdminPackageArchitectureTest.java and shared/controller/GatewayAdminJsonContractTest.java

**Interfaces:**

- Consumes: Task 1 shared actor, idempotency, exceptions, Web MVC resolver and architecture guard.
- Produces: application/group/scope controllers, services, repositories and all explicit DTO/VO/PO/domain contracts used by Catalog, Credential, Routing, Release and MCP.

- [ ] **Step 1: Extend characterization coverage**

Before moving types, add serialization tests for GatewayApplicationService.GatewayApplicationView, GatewayGroupService.GatewayGroupView and GatewayScopeService.ScopeView. Assert complete JSON trees and verify copied collection inputs cannot be mutated where the current record constructors use List.copyOf/Set.copyOf.

Run Dtest=GatewayAdminJsonContractTest. Expected: PASS on current nested types.

- [ ] **Step 2: Tighten the architecture allowlist**

Remove these five hosts from Appendix A in GatewayAdminPackageArchitectureTest:

~~~text
application.GatewayApplicationService
application.GatewayGroupService
application.scope.GatewayScopeService
interfaces.management.GatewayApplicationController
interfaces.management.GatewayGroupController
~~~

Run Dtest=GatewayAdminPackageArchitectureTest. Expected: FAIL listing those five hosts.

- [ ] **Step 3: Move controllers/services and extract all mapped types**

Apply every applicable row in spec §5.1, §5.2 and §5.5. The service signatures must use the new top-level names:

~~~java
GatewayApplicationVO create(
        GatewayApplicationCreateCommandDTO command,
        AdminActor actor,
        RequestAuditContext auditContext);

GatewayGroupVO update(
        String groupId,
        GatewayGroupUpdateCommandDTO command,
        AdminActor actor,
        RequestAuditContext auditContext);

List<GatewayScopeVO> list(GatewayScopeQueryDTO query);
~~~

Use the existing exact parameters/return values from source if the declarations contain more arguments; only replace owning nested-type references and packages.

- [ ] **Step 4: Move JPA types without persistence changes**

Rename Entity classes to PO and move Spring Data repositories. Preserve @Entity, @Table, @Column, @Version, ID generation, repository method names and query annotations. Update all constructors/imports; do not add a migration.

- [ ] **Step 5: Run focused application/group/scope tests**

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayAdminPackageArchitectureTest,GatewayAdminJsonContractTest,GatewayApplicationServiceTest,GatewayScopeServiceTest,GatewayAdminSecurityIntegrationTest,GatewayManagementActorBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS; JPA repositories are discovered from their new sibling packages and HTTP characterization remains unchanged.

- [ ] **Step 6: Commit Task 2**

~~~bash
git status --short
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git diff --cached --check
git commit -m "refactor(gateway): organize application group and scope domains"
~~~

---

### Task 3: Migrate catalog/credential

**Files:**

- Move/modify catalog production: application/catalog/GatewayCatalogService.java, GatewayCatalogStore.java, interfaces/management/GatewayCatalogController.java, infrastructure/persistence/JdbcGatewayCatalogStore.java
- Move/modify credential production: application/credential/GatewayCredentialService.java, GatewayCredentialStore.java, GatewaySecretProtector.java, interfaces/management/GatewayCredentialController.java, infrastructure/persistence/JdbcGatewayCredentialStore.java, infrastructure/security/AesGcmGatewaySecretProtector.java
- Rename contracts/implementations: GatewayCatalogStore to catalog.repository.GatewayCatalogRepository; JdbcGatewayCatalogStore to catalog.repository.jdbc.JdbcGatewayCatalogRepository; GatewayCredentialStore to credential.repository.GatewayCredentialRepository; JdbcGatewayCredentialStore to credential.repository.jdbc.JdbcGatewayCredentialRepository
- Create all catalog/credential top-level types from spec §5.1, §5.2 and §5.5, including GatewayCatalogProtocolEnum, GatewayOperationPO, GatewayCredentialPO, GatewayProtectedSecretVO and the three package-private JDBC mutable assemblers
- Create package-info.java for all nonempty catalog and credential target packages
- Move/modify tests: application/catalog/GatewayCatalogServiceTest.java, infrastructure/security/AesGcmGatewaySecretProtectorTest.java; update infrastructure/persistence/JdbcGatewayTemporalBindingTest.java imports without moving that cross-domain test yet
- Modify: architecture/GatewayAdminPackageArchitectureTest.java and shared/controller/GatewayAdminJsonContractTest.java

**Interfaces:**

- Consumes: application IDs/VOs, shared audit/idempotency contracts, DDC validation.
- Produces: catalog repository/tree/detail contracts and credential repository/secret contracts used by Controllers, MCP and bootstrap configuration.

- [ ] **Step 1: Characterize catalog and credential values**

Add JSON tests for OperationDetail, CredentialView, IssuedCredential and ProtectedSecret. Assert secret fields retain their current presence/absence semantics; do not serialize ciphertext or key material.

Run Dtest=GatewayAdminJsonContractTest. Expected: PASS before migration.

- [ ] **Step 2: Tighten the architecture allowlist**

Remove these eight hosts:

~~~text
application.catalog.GatewayCatalogService
application.catalog.GatewayCatalogStore
interfaces.management.GatewayCatalogController
infrastructure.persistence.JdbcGatewayCatalogStore
application.credential.GatewayCredentialService
application.credential.GatewayCredentialStore
application.credential.GatewaySecretProtector
interfaces.management.GatewayCredentialController
~~~

Run Dtest=GatewayAdminPackageArchitectureTest. Expected: FAIL listing those hosts.

- [ ] **Step 3: Extract catalog types and rename repositories**

Apply every catalog row in the approved manifest. Keep MutableBusiness, MutableEntity and MutableGroup package-private in catalog.repository.jdbc. Use these exact renamed shapes; their freeze methods construct the corresponding VO with the same insertion order:

~~~java
final class GatewayCatalogMutableBusiness {
    private final String id;
    private final String code;
    private final String displayName;
    final Map<String, GatewayCatalogMutableEntity> entities =
            new LinkedHashMap<>();

    GatewayCatalogMutableBusiness(
            String id,
            String code,
            String displayName) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
    }

    GatewayBusinessNodeVO freeze() {
        return new GatewayBusinessNodeVO(
                id,
                code,
                displayName,
                entities.values().stream()
                        .map(GatewayCatalogMutableEntity::freeze)
                        .toList()
        );
    }
}

final class GatewayCatalogMutableEntity {
    private final String id;
    private final String code;
    private final String displayName;
    final Map<String, GatewayCatalogMutableGroup> groups =
            new LinkedHashMap<>();

    GatewayCatalogMutableEntity(
            String id,
            String code,
            String displayName) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
    }

    GatewayEntityNodeVO freeze() {
        return new GatewayEntityNodeVO(
                id,
                code,
                displayName,
                groups.values().stream()
                        .map(GatewayCatalogMutableGroup::freeze)
                        .toList()
        );
    }
}

final class GatewayCatalogMutableGroup {
    private final String id;
    private final String code;
    private final String displayName;
    private final String sourceType;
    private final String className;
    final List<GatewayOperationNodeVO> operations = new ArrayList<>();

    GatewayCatalogMutableGroup(
            String id,
            String code,
            String displayName,
            String sourceType,
            String className) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.sourceType = sourceType;
        this.className = className;
    }

    GatewayInterfaceGroupNodeVO freeze() {
        return new GatewayInterfaceGroupNodeVO(
                id,
                code,
                displayName,
                sourceType,
                className,
                List.copyOf(operations)
        );
    }
}
~~~

Do not create an internal subpackage because package-private access would be lost.

- [ ] **Step 4: Extract credential types and preserve crypto behavior**

Move the protector interface/service/VO and JDBC repository. Preserve AES-GCM algorithm, AAD, nonce generation, key version, ciphertext representation, validFrom/validUntil conversion and one-time issued secret behavior.

- [ ] **Step 5: Run focused tests**

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayAdminPackageArchitectureTest,GatewayAdminJsonContractTest,GatewayCatalogServiceTest,AesGcmGatewaySecretProtectorTest,JdbcGatewayTemporalBindingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS with unchanged catalog SQL/JSON trees and credential crypto/temporal binding behavior.

- [ ] **Step 6: Commit Task 3**

~~~bash
git status --short
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git diff --cached --check
git commit -m "refactor(gateway): organize catalog and credential domains"
~~~

---

### Task 4: Migrate routing/release/rule

**Files:**

- Move/modify routing: application/routing/GatewayDraftService.java, GatewayDraftStore.java, interfaces/management/GatewayDraftController.java, infrastructure/persistence/GatewayDraftEntity.java, GatewayDraftRepository.java, JdbcGatewayDraftStore.java, rule/GatewayRouteDraftMapper.java, GatewayRouteTransportPolicyValidator.java
- Move/modify release: application/release/GatewayReleasePublicationCoordinator.java, GatewayReleasePublicationStore.java, GatewayReleaseService.java, GatewayReleaseStore.java, interfaces/management/GatewayReleaseController.java, interfaces/scheduled/GatewayReleaseReconciler.java, GatewayRuleChunkGarbageCollector.java, infrastructure/persistence/JdbcGatewayReleasePublicationStore.java, JdbcGatewayReleaseStore.java
- Move/modify rule: rule/CompiledGatewayRelease.java, GatewayDdcPublicationCommand.java, GatewayDdcRulePublisher.java, GatewayDdcYamlDocument.java, GatewayRuleCanonicalizer.java, GatewayRuleCompiler.java
- Move domain types: domain/GatewayDraftRevision.java to routing/domain; domain/GatewayReleaseStateMachine.java and GatewayReleaseStatus.java to release/domain/enums or release/domain as specified
- Rename Store/Jdbc Store/Entity types to Repository/Jdbc Repository/PO and create every routing/release/rule top-level type from spec §5.1, §5.2 and §5.5
- Create package-info.java for every nonempty routing, release and rule target package
- Split/move tests: application/routing/*, application/release/*, infrastructure/persistence/JdbcGatewayReleasePublicationStoreTest.java, JdbcGatewayReleaseStoreTest.java, interfaces/scheduled/GatewayReleaseReconcilerTest.java, GatewayRuleChunkGarbageCollectorTest.java, rule/*, domain/GatewayAdminDomainTest.java
- Modify: architecture/GatewayAdminPackageArchitectureTest.java and shared/controller/GatewayAdminJsonContractTest.java

**Interfaces:**

- Consumes: group/application repositories, shared actor/audit/idempotency, catalog data, DDC client.
- Produces: routing drafts/validation/diff, release state/publication records, compiled rules and DDC publication contracts used by runtime/reporting/MCP.

- [ ] **Step 1: Characterize draft/release JSON and domain behavior**

Before moves, assert full JSON for DraftView, MutationResult, ValidationReport, DraftDiff, ReleaseView and PublicationOutcome. Keep GatewayDraftRevision and GatewayReleaseStateMachine behavior assertions, splitting GatewayAdminDomainTest into:

~~~text
routing/domain/GatewayDraftRevisionTest.java
release/domain/GatewayReleaseStateMachineTest.java
~~~

Run the old test names before moving. Expected: PASS.

- [ ] **Step 2: Tighten the architecture allowlist**

Remove these ten hosts:

~~~text
application.routing.GatewayDraftService
application.routing.GatewayDraftStore
interfaces.management.GatewayDraftController
application.release.GatewayReleasePublicationCoordinator
application.release.GatewayReleasePublicationStore
application.release.GatewayReleaseService
application.release.GatewayReleaseStore
interfaces.management.GatewayReleaseController
rule.GatewayDdcYamlDocument
rule.GatewayRouteTransportPolicyValidator
~~~

Run Dtest=GatewayAdminPackageArchitectureTest. Expected: FAIL listing those hosts.

- [ ] **Step 3: Migrate routing and release types**

Apply all approved rows. Keep PreparedGatewayRelease, GatewayTransportRange, GatewayTransportValidationIssue, GatewayYamlRemoval, GatewayYamlLeafLocation, GatewayYamlParentLink and GatewayYamlPrefixMatch package-private beside their service consumers. Keep publication phase/status enums in release.domain.enums with Enum suffix.

- [ ] **Step 4: Rename persistence types and preserve temporal/transaction behavior**

Rename GatewayDraftEntity to GatewayDraftPO and all release records/repositories to approved names. Preserve optimistic revisions, JSON columns, Timestamp conversion, publication phase ordering, cleanup predicates, @Transactional boundaries and SQL text.

- [ ] **Step 5: Run focused routing/release/rule tests**

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayAdminPackageArchitectureTest,GatewayAdminJsonContractTest,GatewayDraftServiceTest,GatewayDraftTransportWorkflowTest,GatewayReleasePublicationCoordinatorTest,GatewayReleaseServiceTest,JdbcGatewayReleasePublicationStoreTest,JdbcGatewayReleaseStoreTest,GatewayReleaseReconcilerTest,GatewayRuleChunkGarbageCollectorTest,GatewayDraftRevisionTest,GatewayReleaseStateMachineTest,GatewayDdcRulePublisherTest,GatewayDdcYamlDocumentTest,GatewayMcpRuleCompilerTest,GatewayRouteDraftMapperTest,GatewayRouteTransportPolicyValidatorTest,GatewayRuleCompilerTest,JdbcGatewayTemporalBindingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS; rule canonicalization/publication output and all release state transitions remain unchanged.

- [ ] **Step 6: Commit Task 4**

~~~bash
git status --short
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git diff --cached --check
git commit -m "refactor(gateway): organize routing release and rule domains"
~~~

---

### Task 5: Migrate runtime/observability/reporting

**Files:**

- Move/modify runtime: application/projection/GatewayProjectionService.java and interfaces/management/GatewayProjectionController.java
- Move/modify observability: application/observability/GatewayCallEventIngestService.java, GatewayObservabilityQueryService.java, GatewayObservabilityStore.java, interfaces/management/GatewayObservabilityController.java, infrastructure/messaging/GatewayCallEventCodec.java, GatewayCallEventConsumerHandler.java, GatewayKafkaCallEventConsumer.java, GatewayKafkaConsumerMetrics.java, interfaces/scheduled/GatewayObservabilityRetentionReaper.java
- Move/rename observability persistence: infrastructure/persistence/GatewayAuditLogEntity.java, GatewayAuditLogRepository.java, JdbcGatewayObservabilityStore.java
- Move/modify reporting: application/reporting/GatewayDefinitionLifecycleStore.java, GatewayDefinitionReportService.java, GatewayDefinitionReportStore.java, GatewayHmacNonceStore.java, GatewayOperationSchemaValidator.java, GatewayReportAuthentication.java, GatewayReportCanonicalizer.java
- Move/modify reporting adapters: interfaces/openapi/GatewayDefinitionReportController.java, GatewayReportHmacFilter.java, interfaces/scheduled/GatewayDefinitionLifecycleReconciler.java, GatewayHmacNonceReaper.java
- Move/rename reporting persistence: infrastructure/persistence/JdbcGatewayDefinitionLifecycleStore.java, JdbcGatewayDefinitionReportStore.java, JdbcGatewayHmacNonceStore.java
- Create every runtime/observability/reporting top-level DTO/VO/PO/Enum and package-private helper in spec §5.2 and §5.5
- Create package-info.java for all nonempty runtime, observability and reporting target packages
- Move/modify tests: application/projection/GatewayProjectionServiceTest.java; application/observability/GatewayObservabilityQueryServiceTest.java; infrastructure/messaging/GatewayCallEventConsumerHandlerTest.java and GatewayKafkaCallEventConsumerTest.java; infrastructure/persistence/JdbcGatewayObservabilityStoreTest.java and JdbcGatewayDefinitionLifecycleStoreTest.java; application/reporting/GatewayDefinitionReportServiceTest.java, GatewayOperationSchemaValidatorTest.java, GatewayReportCanonicalizerTest.java; interfaces/openapi/GatewayReportHmacFilterTest.java; interfaces/scheduled/GatewayDefinitionLifecycleReconcilerTest.java
- Move infrastructure/persistence/JdbcGatewayTemporalBindingTest.java to shared/repository/jdbc/JdbcGatewayTemporalBindingTest.java after updating its three cross-domain repository imports
- Modify: architecture/GatewayAdminPackageArchitectureTest.java, shared/controller/GatewayAdminJsonContractTest.java and bootstrap/GatewayAdminConfiguration.java

**Interfaces:**

- Consumes: release/rule/application/group contracts and shared JDBC utilities.
- Produces: runtime projection DTO/VO, observability query/page data, reporting authentication/lifecycle contracts and adapters.

- [ ] **Step 1: Characterize runtime/observability/reporting responses**

Before moving, add complete JSON assertions for ProjectionEnvelope, ProviderInstanceProjection, RuntimeConsistency, EngineNodeConsistency, TraceSummary, AuditSummary, DashboardSummary, Page, ReconcileResult and StoredReport. Verify List/Map copying behavior already present in constructors.

Run Dtest=GatewayAdminJsonContractTest. Expected: PASS on nested baseline types.

- [ ] **Step 2: Tighten the architecture allowlist**

Remove these twelve hosts:

~~~text
application.projection.GatewayProjectionService
application.observability.GatewayObservabilityStore
infrastructure.messaging.GatewayCallEventConsumerHandler
infrastructure.messaging.GatewayKafkaCallEventConsumer
infrastructure.persistence.JdbcGatewayObservabilityStore
application.reporting.GatewayDefinitionLifecycleStore
application.reporting.GatewayDefinitionReportService
application.reporting.GatewayDefinitionReportStore
application.reporting.GatewayOperationSchemaValidator
interfaces.openapi.GatewayReportHmacFilter
interfaces.scheduled.GatewayDefinitionLifecycleReconciler
infrastructure.persistence.JdbcGatewayDefinitionReportStore
~~~

Run Dtest=GatewayAdminPackageArchitectureTest. Expected: FAIL listing those hosts.

- [ ] **Step 3: Migrate runtime and observability**

Rename GatewayProjectionService to runtime.service package without changing its orchestration. Extract GatewayRuleExpectation and GatewayProjectionCounts as package-private service types. Move message consumers to observability.controller.message and keep GatewayKafkaRecordKey/GatewayKafkaRebalanceListener in that exact package so package-private access remains. Replace the non-static inner-listener capture with explicit same-package constructor injection:

~~~java
final class GatewayKafkaRebalanceListener
        implements ConsumerRebalanceListener {

    private final Map<GatewayKafkaRecordKey, Integer> attempts;
    private final Logger logger;

    GatewayKafkaRebalanceListener(
            Map<GatewayKafkaRecordKey, Integer> attempts,
            Logger logger) {
        this.attempts = attempts;
        this.logger = logger;
    }

    @Override
    public void onPartitionsRevoked(
            Collection<TopicPartition> partitions) {
        attempts.clear();
    }

    @Override
    public void onPartitionsAssigned(
            Collection<TopicPartition> partitions) {
        logger.info(
                "Gateway Kafka consumer assigned {} partitions",
                partitions.size()
        );
    }
}
~~~

GatewayKafkaCallEventConsumer constructs it with its existing attempts map and LOGGER. Do not expose either dependency publicly.

Rename:

~~~text
GatewayObservabilityStore -> observability.repository.GatewayObservabilityRepository
JdbcGatewayObservabilityStore -> observability.repository.jdbc.JdbcGatewayObservabilityRepository
GatewayAuditLogEntity -> observability.domain.po.GatewayAuditLogPO
~~~

Keep Kafka commit/rebalance semantics, metric names, retry/failure recording, retention cutoff and SQL filters unchanged.

- [ ] **Step 4: Migrate reporting and OpenAPI/scheduled adapters**

Rename each Store/Jdbc Store to Repository/Jdbc Repository. Keep GatewayReportAuthentication and canonicalization semantics. Extract GatewayReportAuthenticationFailure and GatewayCachedBodyRequest as package-private types beside GatewayReportHmacFilter in reporting.controller.openapi. Keep request caching, HMAC comparison, nonce expiry and response codes unchanged.

- [ ] **Step 5: Run focused runtime/observability/reporting tests**

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayAdminPackageArchitectureTest,GatewayAdminJsonContractTest,GatewayProjectionServiceTest,GatewayObservabilityQueryServiceTest,GatewayCallEventConsumerHandlerTest,GatewayKafkaCallEventConsumerTest,JdbcGatewayObservabilityStoreTest,GatewayDefinitionReportServiceTest,GatewayOperationSchemaValidatorTest,GatewayReportCanonicalizerTest,JdbcGatewayDefinitionLifecycleStoreTest,GatewayDefinitionLifecycleReconcilerTest,GatewayReportHmacFilterTest,JdbcGatewayTemporalBindingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS with unchanged pagination, projection stale markers, Kafka decisions and OpenAPI authentication results.

- [ ] **Step 6: Commit Task 5**

~~~bash
git status --short
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git diff --cached --check
git commit -m "refactor(gateway): organize runtime observability and reporting"
~~~

---

### Task 6: Migrate the complete mcp domain

**Files:**

- Move/modify service production: mcp/application/McpControlPlaneService.java, McpReleaseContentFactory.java, McpToolAdminService.java, McpValidationException.java, McpValidationService.java
- Move/modify controllers: mcp/interfaces/McpAppAdminController.java, McpApprovalController.java, McpCapabilityController.java, McpProtocolInspectorController.java, McpRemoteProviderController.java, McpServerController.java, McpTaskAdminController.java, McpToolAdminController.java
- Move/rename filesystem repository: mcp/artifact/FileSystemMcpAppArtifactStore.java to mcp/repository/filesystem/FileSystemMcpAppArtifactRepository.java
- Move/rename JDBC repositories: every file under mcp/persistence, including JdbcMcpApprovalStore, JdbcMcpArtifactMetadataStore, JdbcMcpCapabilityDraftStore, JdbcMcpManagedToolOverrideStore, JdbcMcpRemoteProviderStore, JdbcMcpRemoteToolDraftStore, JdbcMcpTaskStore, McpJdbcJson, McpServerEntity and McpServerRepository
- Create every mcp top-level DTO/VO/PO/Enum/exception/helper from spec §5.3, §5.4 and §5.5
- Create package-info.java for mcp/controller, domain, domain/dto, domain/vo, domain/po, domain/enums, domain/exception, repository, repository/jdbc, repository/filesystem and service
- Move/modify all tests currently under mcp/application, mcp/artifact, mcp/interfaces and mcp/persistence to their matching target package
- Modify: architecture/GatewayAdminPackageArchitectureTest.java, shared/controller/GatewayAdminJsonContractTest.java and bootstrap/GatewayAdminConfiguration.java

**Interfaces:**

- Consumes: catalog repository, routing/release/rule contracts, shared actor/audit/idempotency.
- Produces: the complete mcp controller/domain/repository/service boundary without splitting McpControlPlaneService or changing MCP protocols.

- [ ] **Step 1: Characterize MCP public values**

Before moving, add JSON assertions for ApprovalResponse, Inspection, CancelResult, ServerView, MutationResult, Preview, ManagedToolView, RemoteToolView, ValidationReport and ValidationFinding. Assert every field and enum string currently serialized.

Run Dtest=GatewayAdminJsonContractTest. Expected: PASS.

- [ ] **Step 2: Tighten the architecture allowlist**

Remove these nineteen hosts:

~~~text
mcp.application.McpControlPlaneService
mcp.application.McpReleaseContentFactory
mcp.application.McpToolAdminService
mcp.application.McpValidationService
mcp.interfaces.McpAppAdminController
mcp.interfaces.McpApprovalController
mcp.interfaces.McpCapabilityController
mcp.interfaces.McpProtocolInspectorController
mcp.interfaces.McpRemoteProviderController
mcp.interfaces.McpServerController
mcp.interfaces.McpTaskAdminController
mcp.interfaces.McpToolAdminController
mcp.persistence.JdbcMcpApprovalStore
mcp.persistence.JdbcMcpArtifactMetadataStore
mcp.persistence.JdbcMcpCapabilityDraftStore
mcp.persistence.JdbcMcpManagedToolOverrideStore
mcp.persistence.JdbcMcpRemoteProviderStore
mcp.persistence.JdbcMcpRemoteToolDraftStore
mcp.persistence.JdbcMcpTaskStore
~~~

Run Dtest=GatewayAdminPackageArchitectureTest. Expected: FAIL listing all nineteen.

- [ ] **Step 3: Move controllers/services and extract HTTP/application types**

Apply every row in spec §5.3 and §5.4. Preserve Jakarta Validation annotations, controller mappings, authorization annotations, idempotency keys, preview/validation/release ordering and method bodies. Keep McpControlPlaneService intact as one service.

- [ ] **Step 4: Rename repositories and extract JDBC/filesystem types**

Apply every MCP row in spec §5.5. Rename Store implementations to Repository and McpServerEntity to McpServerPO. Keep McpCapabilityBinding package-private in mcp.repository.jdbc. Preserve JSON conversion, SQL, task claiming/cancellation, artifact path validation, file permissions and cleanup behavior.

- [ ] **Step 5: Run the complete MCP-focused test set**

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayAdminPackageArchitectureTest,GatewayAdminJsonContractTest,McpControlPlaneServiceConstructorTest,McpReleaseContentFactoryTest,McpToolAdminServiceTest,McpUnifiedReleaseTest,McpApprovalControllerTest,JdbcMcpControlPlaneStoreTest,McpArtifactUploadIT,McpAdminApiIT,GatewayMcpFlywayPostgresqlIT \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS. If the IT classes are excluded by Surefire naming, run their existing profile/goal exactly as configured in the module rather than changing test naming or plugin configuration.

- [ ] **Step 6: Commit Task 6**

~~~bash
git status --short
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git diff --cached --check
git commit -m "refactor(gateway): organize mcp admin domain"
~~~

---

### Task 7: Close Java package/FQCN/contract migration

**Files:**

- Modify: backend architecture/GatewayAdminPackageArchitectureTest.java and shared/controller/GatewayAdminJsonContractTest.java
- Modify any remaining backend production/test package-info.java or imports under egon-cola-platform-gateway-admin
- Modify cross-module tests: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayRuleWireCompatibilityTest.java
- Modify cross-module tests: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpSecurityIT.java

**Interfaces:**

- Consumes: all backend domain migrations from Tasks 1-6.
- Produces: an empty legacy allowlist, enforceable package/dependency rules, no old FQCN and a fully passing Gateway Admin module.

- [ ] **Step 1: Make the nested-host allowlist empty**

Replace the progressive set with:

~~~java
private static final Set<String> LEGACY_NESTED_TYPE_HOSTS = Set.of();
~~~

The test must assert both actual nested hosts and the allowlist are empty.

- [ ] **Step 2: Add final source/package rules**

Walk src/main/java using Path.of(System.getProperty("basedir"), "src/main/java"). Assert:

~~~java
assertThat(packageNames)
        .noneMatch(name -> name.startsWith(ADMIN + ".interfaces"))
        .noneMatch(name -> name.startsWith(ADMIN + ".infrastructure"))
        .noneMatch(name -> name.matches(
                Pattern.quote(ADMIN) + "\\.mcp\\.(application|interfaces|persistence|artifact).*"))
        .noneMatch(name -> name.matches(
                Pattern.quote(ADMIN) + "\\.application\\.(catalog|credential|observability|projection|release|reporting|routing|scope).*"));
~~~

Also assert:

- controller imports do not contain .repository.jdbc, .repository.jpa or .repository.filesystem;
- a domain package source does not import its own controller/service/repository;
- files under domain/dto, domain/vo, domain/po and domain/enums end with DTO.java, VO.java, PO.java and Enum.java;
- every directory containing a production .java file contains package-info.java;
- no production filename ends in Entity.java or Store.java.

- [ ] **Step 3: Update the two external Gateway test consumers**

Use these exact new imports:

~~~java
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.mcp.service.McpValidationService;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpArtifactMetadataRepository;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCompiler;
~~~

Do not change the test scenarios or assertions.

- [ ] **Step 4: Run the full Gateway Admin module**

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am test
~~~

Expected: Gateway Admin and required upstream modules build; all selected module tests pass.

- [ ] **Step 5: Run downstream compilation/tests and static scans**

~~~bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am \
  -Dtest=GatewayRuleWireCompatibilityTest,McpSecurityIT \
  -Dsurefire.failIfNoSpecifiedTests=false test

rg -n --glob '*.java' \
  '^[[:space:]]+(public[[:space:]]+|protected[[:space:]]+|private[[:space:]]+|static[[:space:]]+|final[[:space:]]+)*(record|class|enum|interface)[[:space:]]+[A-Z]' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java

rg -n '^package .*gateway\.admin\.(interfaces|infrastructure)(\.|;)' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java

rg -n '^package .*gateway\.admin\.mcp\.(application|interfaces|persistence|artifact)(\.|;)' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java

rg -n '^package .*gateway\.admin\.application\.(catalog|credential|observability|projection|release|reporting|routing|scope)(\.|;)' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java

rg --files \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java \
  | rg '/[^/]*(Entity|Store)\.java$'
~~~

Expected: Maven PASS; every rg command returns no match. The reflection guard is authoritative for declaration forms the text pattern does not recognize.

- [ ] **Step 6: Commit Task 7**

~~~bash
git status --short
git add \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayRuleWireCompatibilityTest.java \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpSecurityIT.java
git diff --cached --check
git commit -m "test(gateway): close admin package migration"
~~~

---

### Task 8: Add page-scope primitives without changing page behavior

**Files:**

- Create: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/useGatewayScopeBindings.ts
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/useGatewayScopeBindings.test.tsx
- Create: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeSearchParams.ts
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeSearchParams.test.ts
- Create: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/components/GatewayScopeFilter.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/components/GatewayScopeFilter.test.tsx
- No deletion in this task: hooks/useScope.tsx and hooks/scopeDefaults.ts remain temporarily so existing pages compile until Tasks 9-10.

**Interfaces:**

- Consumes: gatewayApi.scopes(signal), GatewayScopeBinding, Scope, React Query and React Router URLSearchParams.
- Produces: gatewayScopeBindingsQueryKey, useGatewayScopeBindings(), ScopeField, readScopeSearchParams(), writeScopeSearchParams(), hasRequiredScopeFields() and controlled GatewayScopeFilter.

- [ ] **Step 1: Write failing pure URL-state tests**

Cover nonempty parsing, deletion of empty fields, preservation of unrelated page filters and completeness checks:

~~~ts
const current = new URLSearchParams(
  'bizCode=retail&env=prod&protocol=HTTP&page=3',
)

expect(readScopeSearchParams(current, ['bizCode', 'env'])).toEqual({
  bizCode: 'retail',
  env: 'prod',
})

expect(
  writeScopeSearchParams(current, { bizCode: '', env: 'test' }, ['bizCode', 'env'])
    .toString(),
).toBe('env=test&protocol=HTTP&page=3')

expect(
  writeScopeSearchParams(
    new URLSearchParams(),
    { bizCode: 'retail', appCode: 'order' },
    ['bizCode', 'appCode'],
    'resource',
  ).toString(),
).toBe('resourceBizCode=retail&resourceAppCode=order')
~~~

Run npm test -- --run src/hooks/scopeSearchParams.test.ts. Expected: FAIL because the module does not exist.

- [ ] **Step 2: Implement the pure URL-state functions**

Use exact signatures:

~~~ts
export const scopeFieldOrder = [
  'bizCode',
  'namespace',
  'env',
  'appCode',
] as const

export type ScopeField = typeof scopeFieldOrder[number]

export const readScopeSearchParams = (
  params: URLSearchParams,
  fields: readonly ScopeField[],
  paramPrefix?: string,
): Partial<Scope>

export const writeScopeSearchParams = (
  current: URLSearchParams,
  value: Partial<Scope>,
  fields: readonly ScopeField[],
  paramPrefix?: string,
): URLSearchParams

export const hasRequiredScopeFields = (
  value: Partial<Scope>,
  fields: readonly ScopeField[],
): boolean
~~~

Only append trimmed nonempty strings; preserve query keys outside fields. With no prefix, use bizCode/namespace/env/appCode. With prefix resource or prompt, use resourceBizCode/resourceNamespace/resourceEnv/resourceAppCode or promptBizCode/promptNamespace/promptEnv/promptAppCode by capitalizing the first field character.

- [ ] **Step 3: Write and implement the shared Binding query hook**

Test two consumers under the same QueryClientProvider and assert gatewayApi.scopes is called once. Test error/refetch passthrough.

~~~ts
export const gatewayScopeBindingsQueryKey = ['gateway-scopes'] as const

export const useGatewayScopeBindings = () => useQuery({
  queryKey: gatewayScopeBindingsQueryKey,
  queryFn: ({ signal }) => gatewayApi.scopes(signal),
})
~~~

The hook must not read auth/session, select a scope, write storage, render a full-screen state or navigate.

- [ ] **Step 4: Write failing controlled-filter tests**

Test:

1. fields controls which Select elements exist;
2. value and onChange make the component controlled;
3. changing an upstream value removes only invalid downstream values;
4. optional fields can be cleared;
5. Binding query failure renders a local alert and retry button;
6. empty Bindings do not replace the page.

Run npm test -- --run src/components/GatewayScopeFilter.test.tsx. Expected: FAIL before component creation.

- [ ] **Step 5: Implement GatewayScopeFilter**

Use the approved public props exactly:

~~~ts
export type GatewayScopeFilterProps = {
  fields: Array<'bizCode' | 'namespace' | 'env' | 'appCode'>
  value: Partial<Scope>
  required?: boolean
  onChange: (value: Partial<Scope>) => void
}
~~~

The component calls useGatewayScopeBindings, derives distinct choices from Bindings, invokes onChange with a new Partial<Scope>, and renders only a local QueryFailure/Alert with refetch. It must not use Context, LocalStorage, queryClient.removeQueries or navigate.

- [ ] **Step 6: Run Task 8 tests and commit**

~~~bash
cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm test -- --run \
  src/hooks/scopeSearchParams.test.ts \
  src/hooks/useGatewayScopeBindings.test.tsx \
  src/components/GatewayScopeFilter.test.tsx

git status --short
git add \
  src/hooks/useGatewayScopeBindings.ts \
  src/hooks/useGatewayScopeBindings.test.tsx \
  src/hooks/scopeSearchParams.ts \
  src/hooks/scopeSearchParams.test.ts \
  src/components/GatewayScopeFilter.tsx \
  src/components/GatewayScopeFilter.test.tsx
git diff --cached --check
git commit -m "feat(gateway-web): add page scope primitives"
~~~

---

### Task 9: Move Group/Application/Catalog/MCP to page-owned optional filters

**Files:**

- Modify API contracts: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts
- Modify API calls/tests: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts
- Modify Group: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/gateway-groups/GatewayGroupsPage.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/gateway-groups/GatewayGroupsPage.test.tsx
- Modify Application: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/applications/ApplicationsPage.tsx and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/applications/ApplicationsPage.test.tsx
- Modify Catalog: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/CatalogPage.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/CatalogPage.test.tsx
- Modify MCP Group pages: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpServersPage.tsx, egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpServersPage.test.tsx and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpRemoteProvidersPage.tsx
- Modify MCP application/operation panels: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpResourcesPanel.tsx and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpPromptsPanel.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpApplicationOperationSelection.test.tsx

**Interfaces:**

- Consumes: Task 8 GatewayScopeFilter, URL helpers and Binding hook.
- Produces: gatewayApi.groups(signal?), gatewayApi.applications(filters?, signal?), gatewayApi.mcpOperationOptions(applicationId, signal?), explicit GatewayGroup type, and page-local optional filtering for Group/Application/Catalog/MCP.

- [ ] **Step 1: Write failing API URL tests**

Add exact assertions:

~~~ts
await gatewayApi.groups()
expect(fetch).toHaveBeenCalledWith(
  '/api/v1/gateway/admin/gateway-groups',
  expect.anything(),
)

await gatewayApi.applications({ bizCode: 'retail', env: 'prod' })
expect(lastUrl()).toBe(
  '/api/v1/gateway/admin/applications?bizCode=retail&env=prod',
)

await gatewayApi.applications({})
expect(lastUrl()).toBe('/api/v1/gateway/admin/applications')

await gatewayApi.mcpOperationOptions('application-1')
expect(catalog).toHaveBeenCalledWith('application-1', expect.anything())
expect(applications).not.toHaveBeenCalled()
~~~

Run npm test -- --run src/api/gatewayApi.test.ts. Expected: FAIL on current signatures/URLs.

- [ ] **Step 2: Implement nonempty query construction and exact signatures**

Replace the full-scope-only query helper with:

~~~ts
const queryString = (
  values: Record<string, string | undefined>,
): string => {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([name, value]) => {
    const normalized = value?.trim()
    if (normalized) params.append(name, normalized)
  })
  return params.toString()
}

const withQuery = (
  path: string,
  values: Record<string, string | undefined>,
) => {
  const encoded = queryString(values)
  return encoded ? path + '?' + encoded : path
}
~~~

Implement:

~~~ts
groups: (signal?: AbortSignal) => Promise<GatewayGroup[]>
applications: (
  filters?: Partial<Scope>,
  signal?: AbortSignal,
) => Promise<Application[]>
mcpOperationOptions: (
  applicationId: string,
  signal?: AbortSignal,
) => Promise<McpOperationOption[]>
~~~

GatewayGroup must explicitly declare id, gatewayGroupCode, displayName, description, enabled, revision, createdAt, updatedAt, env and namespace. It must not extend Scope or declare bizCode/appCode.

- [ ] **Step 3: Write failing Group/Application page tests**

Group tests must prove two groups from different env/namespace values render by default, local URL filters hide only nonmatching rows, and creation sends the Env/Namespace selected in the modal.

Application tests must prove the first request has no query, rows display Biz/Application Code/Env/Namespace, optional page filters reach the API, and creation sends all four values from a selected unconnected Binding. A connected Binding must be disabled in the creation selector.

Run the two page test files. Expected: FAIL because both pages still read useScope.

- [ ] **Step 4: Implement Group/Application page ownership**

Use useSearchParams independently in each page. Group filtering is client-side over gatewayApi.groups(); its query key is ['gateway-groups']. Application filtering is server-side; normalize selected nonempty fields before:

~~~ts
queryKey: ['applications', normalizedFilters]
queryFn: ({ signal }) =>
  gatewayApi.applications(normalizedFilters, signal)
~~~

Use GatewayScopeFilter for page filters. For Application creation, render a full-Binding Select sourced from useGatewayScopeBindings:

~~~ts
options={bindings.map((binding) => ({
  value: binding.bindingId,
  label: [
    binding.bizCode,
    binding.appCode,
    binding.env,
    binding.namespace,
    binding.appName,
  ].join(' / '),
  disabled: binding.connected,
}))}
~~~

Resolve the selected binding by bindingId and submit its exact bizCode/namespace/env/appCode as applicationCode. Editing and credential mutations remain resource-ID based. Invalidate only ['gateway-groups'] or ['applications'] and the selected credential resource.

- [ ] **Step 5: Write failing Catalog/MCP tests**

Catalog tests must prove:

- empty filters call applications({});
- candidate labels contain Biz/App/Env/Namespace/displayName;
- filtering out the selected Application clears it;
- /applications/:applicationId/catalog uses the route ID without another page's query.

MCP tests must prove:

- Servers and Remote Providers call groups() with no Scope argument;
- their optional Env/Namespace filters remain in their own URL;
- Resources and Prompts first load all/filtered Applications, load no Catalog before Application selection, and load exactly one selected Application Catalog afterward.

Run the affected tests. Expected: FAIL on useScope and the old all-catalog Promise.all implementation.

- [ ] **Step 6: Implement Catalog/MCP page ownership**

Catalog stores only its own optional Scope and selected applicationId. Route applicationId wins over the query value.

MCP Servers and Remote Providers use gatewayApi.groups() and local Env/Namespace filtering. McpResourcesPanel and McpPromptsPanel each render their own optional Application Scope filter plus Application Select. Resources use resourceBizCode/resourceNamespace/resourceEnv/resourceAppCode/resourceApplicationId in the workbench URL; Prompts use promptBizCode/promptNamespace/promptEnv/promptAppCode/promptApplicationId so the two tabs never overwrite one another. Their operation query is:

~~~ts
queryKey: ['mcp-operation-options', selectedApplicationId]
queryFn: ({ signal }) =>
  gatewayApi.mcpOperationOptions(selectedApplicationId!, signal)
enabled: Boolean(selectedApplicationId)
~~~

Clear operationId when the selected Application changes. Do not fetch Catalogs for unselected Applications and do not copy MCP page filters to other routes.

- [ ] **Step 7: Run Task 9 frontend verification and commit**

~~~bash
cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm test -- --run \
  src/api/gatewayApi.test.ts \
  src/features/gateway-groups/GatewayGroupsPage.test.tsx \
  src/features/applications/ApplicationsPage.test.tsx \
  src/features/interface-catalog/CatalogPage.test.tsx \
  src/features/mcp/McpServersPage.test.tsx \
  src/features/mcp/McpApplicationOperationSelection.test.tsx
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm run typecheck

git status --short
git add \
  src/api/types.ts \
  src/api/gatewayApi.ts \
  src/api/gatewayApi.test.ts \
  src/features/gateway-groups/GatewayGroupsPage.tsx \
  src/features/gateway-groups/GatewayGroupsPage.test.tsx \
  src/features/applications/ApplicationsPage.tsx \
  src/features/applications/ApplicationsPage.test.tsx \
  src/features/interface-catalog/CatalogPage.tsx \
  src/features/interface-catalog/CatalogPage.test.tsx \
  src/features/mcp/McpServersPage.tsx \
  src/features/mcp/McpServersPage.test.tsx \
  src/features/mcp/McpRemoteProvidersPage.tsx \
  src/features/mcp/McpResourcesPanel.tsx \
  src/features/mcp/McpPromptsPanel.tsx \
  src/features/mcp/McpApplicationOperationSelection.test.tsx
git diff --cached --check
git commit -m "feat(gateway-web): make resource scope filters page local"
~~~

---

### Task 10: Move Dashboard/Provider/Trace/Audit and remove global Scope

**Files:**

- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/app/App.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/app/App.test.tsx
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.test.tsx
- Delete: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/useScope.tsx and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/useScope.test.tsx
- Delete: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeDefaults.ts and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeDefaults.test.ts
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/dashboard/DashboardPage.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/dashboard/DashboardPage.test.tsx
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/providers/ProvidersPage.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/providers/ProvidersPage.test.tsx
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.test.tsx
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/audit/AuditPage.tsx
- Create test: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/audit/AuditPage.test.tsx
- Modify API functions/tests: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts and egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts

**Interfaces:**

- Consumes: Task 8 URL/Filter primitives and Task 9 nonempty query helper.
- Produces: page-owned required Scope for Dashboard/Provider/Trace/Audit, App without ScopeProvider, Header without Scope selectors, and zero active useScope/default-Scope code.

- [ ] **Step 1: Write failing required-filter tests**

For Dashboard and Provider:

~~~ts
expect(gatewayApi.dashboard).not.toHaveBeenCalled()
// set bizCode, namespace, env and appCode through the page filter
expect(gatewayApi.dashboard).toHaveBeenCalledWith(
  {
    bizCode: 'retail',
    namespace: 'default',
    env: 'prod',
    appCode: 'order',
  },
  expect.anything(),
)
~~~

For Trace/Audit, assert Env+Namespace are sufficient and missing either suppresses the request. Assert form filters and page reset to page=1 are written into that page URL.

Run the four page test files. Expected: FAIL because pages still consume the global Context and request immediately.

- [ ] **Step 2: Implement Dashboard and Provider**

Use useSearchParams and GatewayScopeFilter fields ['bizCode', 'namespace', 'env', 'appCode']. Construct requestScope only when all four fields are nonempty:

~~~ts
const requestScope = hasRequiredScopeFields(
  scope,
  ['bizCode', 'namespace', 'env', 'appCode'],
) ? scope as Scope : undefined

const query = useQuery({
  queryKey: ['dashboard', requestScope],
  queryFn: ({ signal }) => gatewayApi.dashboard(requestScope!, signal),
  enabled: Boolean(requestScope),
})
~~~

Provider uses the same gating and keeps refetchInterval: 10_000 only for the enabled current Scope. Incomplete filters render a local “请选择查询范围” empty state; neither page selects the first Binding or aggregates multiple Scopes.

- [ ] **Step 3: Implement Trace and Audit URL state**

Use only Env/Namespace for backend scope:

~~~ts
type ObservabilityScope = Pick<Scope, 'env' | 'namespace'>
~~~

Keep traceId/protocol/statusCategory/page and actorId/resourceId/traceId/successful/page in their respective URLSearchParams. On a submitted filter change, set page to 1. Query keys contain normalized scope plus filters. Audit capability checks, Drawer and sanitizeForDisplay remain unchanged.

Update API signatures to the approved Pick<Scope, 'env' | 'namespace'> and use withQuery so empty filters never add a trailing ? or &.

- [ ] **Step 4: Write failing layout/root tests**

Update AdminLayout.test.tsx to assert there are no comboboxes named 业务域/命名空间/环境/应用 and that logout still calls auth.logout, queryClient.clear and navigate('/login', { replace: true }).

Create App.test.tsx and mock AuthProvider, CapabilityProvider and RouterProvider with marker elements. Render App and assert the active provider tree contains AuthProvider -> CapabilityProvider -> Suspense -> RouterProvider and has no ScopeProvider marker or import.

Expected before implementation: FAIL because four Header selectors and ScopeProvider remain.

- [ ] **Step 5: Remove the global Scope infrastructure**

Remove ScopeProvider from App.tsx. Remove useScope/optionsFor/ScopeField/selectors, confirm dialog, removeQueries and dashboard redirect from AdminLayout.tsx. Keep Admin API Badge, navigation capability filtering, user menu, queryClient.clear on logout and version footer.

Delete the four legacy Scope files/tests only after rg confirms no production/test import remains:

~~~bash
rg -n 'ScopeProvider|useScope\(|scopeDefaults|VITE_GATEWAY_ADMIN_DEFAULT_' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src
~~~

Expected after deletion: no match.

- [ ] **Step 6: Run Task 10 frontend verification and commit**

~~~bash
cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm test -- --run \
  src/layouts/AdminLayout.test.tsx \
  src/app/App.test.tsx \
  src/features/dashboard/DashboardPage.test.tsx \
  src/features/providers/ProvidersPage.test.tsx \
  src/features/observability/TracesPage.test.tsx \
  src/features/audit/AuditPage.test.tsx \
  src/api/gatewayApi.test.ts
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm run typecheck

git status --short
git add -A -- \
  src/app/App.tsx \
  src/app/App.test.tsx \
  src/layouts/AdminLayout.tsx \
  src/layouts/AdminLayout.test.tsx \
  src/hooks/useScope.tsx \
  src/hooks/useScope.test.tsx \
  src/hooks/scopeDefaults.ts \
  src/hooks/scopeDefaults.test.ts \
  src/features/dashboard/DashboardPage.tsx \
  src/features/dashboard/DashboardPage.test.tsx \
  src/features/providers/ProvidersPage.tsx \
  src/features/providers/ProvidersPage.test.tsx \
  src/features/observability/TracesPage.tsx \
  src/features/observability/TracesPage.test.tsx \
  src/features/audit/AuditPage.tsx \
  src/features/audit/AuditPage.test.tsx \
  src/api/gatewayApi.ts \
  src/api/gatewayApi.test.ts
git diff --cached --check
git commit -m "refactor(gateway-web): remove global scope context"
~~~

---

### Task 11: Documentation, E2E fixture source and final repository gate

**Files:**

- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/README.md
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/README.zh-CN.md
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/e2e/gateway-admin.spec.ts
- Modify: egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/e2e/mcp-control-plane.spec.ts
- Modify only if validation finds a task-related defect: files already owned by Tasks 1-10; do not absorb unrelated failures.

**Interfaces:**

- Consumes: all completed backend and frontend behavior.
- Produces: accurate operator docs, current E2E fixture source, full Gateway checks and root-repository Java compilation proof.

- [ ] **Step 1: Update bilingual frontend documentation**

Remove VITE_GATEWAY_ADMIN_DEFAULT_BIZ_CODE, VITE_GATEWAY_ADMIN_DEFAULT_APP_CODE, VITE_GATEWAY_ADMIN_DEFAULT_ENV and VITE_GATEWAY_ADMIN_DEFAULT_NAMESPACE from both READMEs. Document:

- Header has no global Scope selector;
- Binding data is shared query data only;
- page filters are encoded in that page's URL;
- Group/Application/Catalog/MCP default candidates can span Scope;
- Dashboard/Provider need all four fields;
- Trace/Audit need Env+Namespace;
- browser still obtains identity/capabilities from Session API and does not parse JWT.

- [ ] **Step 2: Update E2E fixture source without running Playwright**

In gateway-admin.spec.ts:

- make /gateway-groups route match the no-query URL as well as optional query forms;
- make /applications match both no-query and filtered URLs;
- before expecting Dashboard/Provider/Trace/Audit data, select required page filters or navigate with the exact page query;
- select Env/Namespace in the new Group modal;
- select a nonconnected Binding in the Application creation path.

In mcp-control-plane.spec.ts:

- allow no-query Group/Application routes;
- select Application before testing Resource/Prompt Operation choices;
- assert only /applications/application-e2e/catalog is requested, not catalogs for every Application.

Do not execute npm run e2e because it starts Vite and a browser; npm lint validates fixture syntax/style.

- [ ] **Step 3: Run the complete Gateway Admin Java suite**

From the repository root:

~~~bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am test

./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am \
  -Dtest=GatewayRuleWireCompatibilityTest,McpSecurityIT \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS and reactor summaries include the actual admin/test-suite child modules, not only parent POMs.

- [ ] **Step 4: Run the full Gateway Admin Web static suite**

~~~bash
cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
npm ci
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm run typecheck
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm test -- --run
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm run lint
VITE_IDP_ISSUER=http://127.0.0.1:18120 \
VITE_IDP_CLIENT_ID=gateway-admin-web \
VITE_IDP_RESOURCE=gateway-admin \
npm run build
~~~

Expected: all four commands PASS. These values are nonsecret build placeholders; no .env file is created.

- [ ] **Step 5: Run final static guards**

From the repository root:

~~~bash
rg -n --glob '*.java' \
  '^[[:space:]]+(public[[:space:]]+|protected[[:space:]]+|private[[:space:]]+|static[[:space:]]+|final[[:space:]]+)*(record|class|enum|interface)[[:space:]]+[A-Z]' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java

rg -n 'top\.egon\.cola\.component\.gateway\.admin\.(interfaces|infrastructure|mcp\.(application|interfaces|persistence|artifact)|application\.(catalog|credential|observability|projection|release|reporting|routing|scope))' \
  egon-cola-platforms/egon-cola-platform-gateway

rg --files \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java \
  | rg '/[^/]*(Entity|Store)\.java$'

rg -n 'ScopeProvider|useScope\(|VITE_GATEWAY_ADMIN_DEFAULT_|egon\.gateway\.admin\.scope\.v1' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web \
  --glob '!README*'

git diff --check
~~~

Expected: all four rg scans return no active-code match; git diff --check returns success.

- [ ] **Step 6: Run the user-required whole-repository Java compilation gate**

From /Users/mario/SelfProject/Egon-COLA:

~~~bash
./mvnw -B -ntp -DskipTests -Dgpg.skip=true clean install
~~~

Expected: BUILD SUCCESS and the reactor summary contains egon-cola-components, egon-cola-platforms and egon-cola-archetypes descendants. This is the required entire-repository compilation proof; targeted tests already ran in Steps 3-4.

If this command fails in an unrelated untouched module, preserve the failure output and prove the changed Gateway modules still pass; do not edit unrelated code merely to force a green root build. The task remains blocked against the user's hard gate until the root build succeeds or the user accepts the proven external failure.

- [ ] **Step 7: Commit Task 11 after all gates pass**

~~~bash
git status --short
git add \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/README.md \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/README.zh-CN.md \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/e2e/gateway-admin.spec.ts \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/e2e/mcp-control-plane.spec.ts
git diff --cached --check
git commit -m "docs(gateway): document page scope behavior"
git status --short
~~~

Expected: the final status is clean. If validation required task-related corrections, stage those exact files in this same Task 11 commit.

---

## Appendix A: Initial 59 Legacy Nested-Type Hosts

The Task 1 architecture test starts from this exact set. Tasks 1-6 remove the entries assigned above; Task 7 requires an empty set.

~~~text
top.egon.cola.component.gateway.admin.application.GatewayApplicationService
top.egon.cola.component.gateway.admin.application.GatewayGroupService
top.egon.cola.component.gateway.admin.application.IdempotencyStore
top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogService
top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore
top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialService
top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialStore
top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector
top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore
top.egon.cola.component.gateway.admin.application.projection.GatewayProjectionService
top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationCoordinator
top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationStore
top.egon.cola.component.gateway.admin.application.release.GatewayReleaseService
top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore
top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionLifecycleStore
top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionReportService
top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionReportStore
top.egon.cola.component.gateway.admin.application.reporting.GatewayOperationSchemaValidator
top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService
top.egon.cola.component.gateway.admin.application.routing.GatewayDraftStore
top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService
top.egon.cola.component.gateway.admin.config.GatewayAdminProperties
top.egon.cola.component.gateway.admin.domain.AdminActor
top.egon.cola.component.gateway.admin.infrastructure.messaging.GatewayCallEventConsumerHandler
top.egon.cola.component.gateway.admin.infrastructure.messaging.GatewayKafkaCallEventConsumer
top.egon.cola.component.gateway.admin.infrastructure.persistence.JdbcGatewayCatalogStore
top.egon.cola.component.gateway.admin.infrastructure.persistence.JdbcGatewayDefinitionReportStore
top.egon.cola.component.gateway.admin.infrastructure.persistence.JdbcGatewayObservabilityStore
top.egon.cola.component.gateway.admin.interfaces.management.GatewayAdminExceptionHandler
top.egon.cola.component.gateway.admin.interfaces.management.GatewayAdminSessionController
top.egon.cola.component.gateway.admin.interfaces.management.GatewayApplicationController
top.egon.cola.component.gateway.admin.interfaces.management.GatewayCatalogController
top.egon.cola.component.gateway.admin.interfaces.management.GatewayCredentialController
top.egon.cola.component.gateway.admin.interfaces.management.GatewayDraftController
top.egon.cola.component.gateway.admin.interfaces.management.GatewayGroupController
top.egon.cola.component.gateway.admin.interfaces.management.GatewayReleaseController
top.egon.cola.component.gateway.admin.interfaces.openapi.GatewayReportHmacFilter
top.egon.cola.component.gateway.admin.interfaces.scheduled.GatewayDefinitionLifecycleReconciler
top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService
top.egon.cola.component.gateway.admin.mcp.application.McpReleaseContentFactory
top.egon.cola.component.gateway.admin.mcp.application.McpToolAdminService
top.egon.cola.component.gateway.admin.mcp.application.McpValidationService
top.egon.cola.component.gateway.admin.mcp.interfaces.McpAppAdminController
top.egon.cola.component.gateway.admin.mcp.interfaces.McpApprovalController
top.egon.cola.component.gateway.admin.mcp.interfaces.McpCapabilityController
top.egon.cola.component.gateway.admin.mcp.interfaces.McpProtocolInspectorController
top.egon.cola.component.gateway.admin.mcp.interfaces.McpRemoteProviderController
top.egon.cola.component.gateway.admin.mcp.interfaces.McpServerController
top.egon.cola.component.gateway.admin.mcp.interfaces.McpTaskAdminController
top.egon.cola.component.gateway.admin.mcp.interfaces.McpToolAdminController
top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpApprovalStore
top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore
top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore
top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpManagedToolOverrideStore
top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore
top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteToolDraftStore
top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpTaskStore
top.egon.cola.component.gateway.admin.rule.GatewayDdcYamlDocument
top.egon.cola.component.gateway.admin.rule.GatewayRouteTransportPolicyValidator
~~~

## Coverage Matrix

| Approved requirement | Implementation task |
|---|---|
| GA-01 to GA-04 auth/HTTP freeze | Tasks 1, 2, 6, 7 |
| GA-05 to GA-07 all nested types/top-level data ownership | Tasks 1-7; Appendix A plus spec §5 |
| GA-13 to GA-19 domain-first package tree/repository naming/MCP boundary | Tasks 1-7 |
| GA-08 global Scope removal | Task 10 |
| GA-09 shared Binding data only | Task 8 |
| GA-10 cross-Scope defaults where supported | Task 9 |
| GA-11 URL page state | Tasks 8-10 |
| GA-12 no browser-side global aggregation | Tasks 9-10 |
| Java structure and behavior tests | Tasks 1-7 |
| Frontend unit/static tests | Tasks 8-11 |
| Docs/E2E fixture source | Task 11 |
| Whole-repository compilation | Task 11 Step 6 |
| No runtime/browser startup | Global Constraints and Task 11 |
