# RBAC3 Admin Domain Package Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 RBAC3 Admin 完整改造为领域优先的 `controller / domain / repository / service` 分包，清除生产源码全部 386 个嵌套类型，并保持 HTTP、Gateway、JPA、Redis、Outbox、权限和业务行为兼容。

**Architecture:** 保留现有 Facade 与 Ports/Adapters，由业务领域作为第一层边界，领域内部按入站、模型、出站和编排组织。八个任务对应八个原子提交；每个任务先扩大结构守卫并验证预期失败，再迁移该波次的类型、消费者和测试，最后编译和回归，任何提交都不得依赖后续任务才能编译。

**Tech Stack:** Java 21、Spring Boot、Spring MVC、Spring Security、Spring Data JPA、Hibernate、Redis/Redisson、Flyway、JUnit 5、AssertJ、Mockito、Maven。

**Approved spec:** `docs/superpowers/specs/2026-08-13-rbac3-admin-domain-package-refactor-design.md`

**Planning source baseline:** RBAC3 Admin production package Git tree `ff7b546492aeb1aec1630fff0e3122b28d727643` (observed at `main@b77bbb20`; approved spec commit `3cb54ca7`).

## Global Constraints

- 生产入口 `top.egon.cola.platform.rbac3.admin.Rbac3AdminApplication` 必须留在根包，Spring Boot 默认组件扫描边界不变。
- 最终生产包只允许根入口、`bootstrap`、`config`、`shared` 和已批准业务领域；旧一级技术根包 `application / interfaces / infrastructure / integration / security / worker / snapshot` 必须清零。
- `domain` 只按真实内容创建 `dto / vo / po / enums / exception`；不得创建空包、模块级公共 DTO/VO/PO 垃圾包或只含占位 `package-info.java` 的目录。
- Request、Command、Query、Mutation、Control 使用 `DTO` 后缀；Response、View、Result、Page、Projection、Report 使用 `VO` 后缀；持久化映射类型使用 `PO` 后缀；枚举使用 `Enum` 后缀；异常使用 `Exception` 后缀。
- 每个生产 `.java` 文件只声明一个顶层 `class / record / enum / interface`；所有 386 个嵌套类型必须独立，最终每个生产类 `getDeclaredClasses().length == 0`。
- 原 private/package 辅助类型独立后保持 package-private；接口成员原先隐式 public 的类型独立后显式 public；不得为解决编译错误扩大其他类型的公共可见性。
- Controller 只依赖本领域 DTO/VO 和 Service；Service 依赖 Domain、Repository 契约和已存在的跨领域公开契约；Repository 实现依赖 Repository 契约、PO 和技术框架；Domain 不依赖 Controller、Service、Repository。
- Controller 中原入站 Port 不直接搬到 Repository：分别抽为 `SessionManagementService`、`AssignmentSessionStrengthService`、`DirectoryCommandService`、`DirectoryQueryService`、`PlatformAdminBootstrapService`，由默认 Service 实现委托 Repository 契约。
- 所有 JPA `*Entity` 改名为 `*PO` 时显式保留原 entity name，例如 `@Entity(name = "SessionEntity")`；`@Table`、列、索引、ID、Version、Converter、JPQL/SQL 语义不变。
- 不修改任何已有 Flyway migration，不创建新 Flyway migration，不修改数据库数据。
- HTTP Route、Method、Status、Header、JSON 字段/层级/空值、Validation、Jackson、权限表达式和 Gateway Operation 元数据冻结。
- Gateway Java Schema 允许 definition key 和 definition SHA 因 FQCN 改变；归一化 `$defs` key 与 `$ref` 后的字段语义必须完全相同。
- Redis Key、TTL、序列化业务字段、Outbox 字段、aggregate key、幂等 key 和投递语义冻结。
- 每个实际 package 和迁移后的顶层类型补齐项目既有风格的中英双语 JavaDoc；类、public 方法、字段/record component 必须说明职责、用法和语义。
- 不引入新依赖、ArchUnit、Lombok、代码生成器、新模块或无业务价值的 Strategy/Factory/Template Method 抽象。
- 不自动启动项目，不进行浏览器或运行态联调。
- 执行前必须创建隔离 worktree；用户工作区中 Gateway Admin 的并发修改不得被暂存、覆盖或带入提交。
- 所有 `git add` 使用任务路径白名单；禁止 `git add .`、`git add -A` 无路径和全仓格式化。

## Paths and Commands

```bash
RBAC3_ADMIN_MODULE=egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin
RBAC3_ADMIN_MAIN=$RBAC3_ADMIN_MODULE/src/main/java/top/egon/cola/platform/rbac3/admin
RBAC3_ADMIN_TEST=$RBAC3_ADMIN_MODULE/src/test/java/top/egon/cola/platform/rbac3/admin
```

每波结构测试命令：

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

每波编译命令：

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
```

最终模块测试命令：

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am test
```

## Execution Invariants

- 附录 A 是 181 个当前顶层生产类的最终目标路径；`SPLIT:` 行必须按列出的全部文件拆分，不能保留原巨型类。
- 附录 B 是 386 个嵌套类型的最终去向；每行必须在所属波次完成定义迁移、消费者更新和原声明删除。
- 附录 B 中的消费者是基线显式 `Host.NestedType` 引用；宿主内简单名、静态导入和反射字符串仍需在每波执行 `rg` 复扫。
- 附录中某个源行号在前一波之后可能漂移；定位以原 FQCN/宿主+嵌套类型名为权威，不以旧行号为唯一依据。
- 执行时若当前 RBAC3 Admin 生产包的 Git tree 不是 `ff7b546492aeb1aec1630fff0e3122b28d727643`，先重新生成清单并与附录逐项对比；新增或消失的生产类型必须在开始修改前补入计划审查，不能静默忽略。纯文档提交推进 HEAD 不会使该基线失效。
- 每个任务的结构守卫先失败是预期 RED；该任务迁移完成后的同一测试必须变绿。行为兼容测试在重构前后都应保持 GREEN。

---

### Task 1: 建立结构护栏并迁移 shared、config、bootstrap 基础边界

**Files:**

- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/architecture/AdminLayerBoundaryTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/architecture/JpaEntityNameCompatibilityTest.java`
- Move/modify: 附录 A 中目标为 `shared/**`、`config/**`、`bootstrap/**` 的生产文件
- Create: 附录 B 中波次 1 的 8 个顶层类型文件
- Create/modify: `shared/**/package-info.java`、`config/**/package-info.java`、`bootstrap/**/package-info.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDocumentCatalogContractTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/resources/contracts/rbac3-gateway-catalog-semantic-baseline.json`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/BootstrapQueryServiceTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/Rbac3PlatformAdminBootstrapCliIT.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializerTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentBootstrapTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentTopologyTest.java`
- Modify: all Admin production/test consumers listed in Appendix B wave 1

**Interfaces:**

- Consumes: approved package rules and the current root-level `Rbac3AdminApplication` component-scan boundary.
- Produces: `shared.repository.DatabaseClock`, `shared.domain.CommandContext`, bootstrap Service/Repository contracts, `config.*` assembly packages, and an AST-backed incremental structure guard used by Tasks 2-8.

- [ ] **Step 1: Verify the planning baseline and isolate the work**

```bash
git status --short
git branch --show-current
git rev-parse --short=8 HEAD
git rev-parse "HEAD:$RBAC3_ADMIN_MAIN"
```

Expected: implementation runs in an isolated worktree created with `superpowers:using-git-worktrees`; the final command prints `ff7b546492aeb1aec1630fff0e3122b28d727643`, or Appendix A/B have been reviewed against the changed production tree before continuing. Do not stage the existing Gateway Admin spec/plan from the user's main worktree.

- [ ] **Step 2: Capture the pre-refactor normalized Gateway catalog baseline**

Before moving any production type, add the recursive `$ref` resolver shown in Task 8 Step 3 to `Rbac3GatewayDocumentCatalogContractTest`. Serialize a sorted map keyed by normalized `METHOD /path`; each value contains operation name, external visibility, request schema and response schema after normalization. Add an opt-in writer guarded by `Boolean.getBoolean("rbac3.updateGatewayContract")`, run it once, then keep the default branch as a read-only equality assertion:

```java
Path baseline = Path.of(System.getProperty("basedir"))
        .resolve("src/test/resources/contracts/"
                + "rbac3-gateway-catalog-semantic-baseline.json");
String actual = objectMapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(normalizedCatalog());
if (Boolean.getBoolean("rbac3.updateGatewayContract")) {
    Files.createDirectories(baseline.getParent());
    Files.writeString(baseline, actual + System.lineSeparator());
} else {
    assertThat(actual).isEqualTo(Files.readString(baseline).stripTrailing());
}
```

Generate and immediately verify the committed baseline:

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=Rbac3GatewayDocumentCatalogContractTest \
  -Drbac3.updateGatewayContract=true \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: both runs PASS; the fixture is generated from the untouched pre-refactor Java types, while definition-key/FQCN details have already been normalized away.

- [ ] **Step 3: Add the AST-based incremental structure test**

In `AdminLayerBoundaryTest`, parse production Java sources with the JDK compiler API rather than regex. Start with `MIGRATED_ROOTS = Set.of("bootstrap", "config", "shared")` and add these assertions:

```java
private static final Set<String> MIGRATED_ROOTS =
        Set.of("bootstrap", "config", "shared");

@Test
void migratedPackagesHaveOneTopLevelTypeAndNoNestedTypes() throws Exception {
    for (Path source : migratedSources()) {
        CompilationUnitTree unit = parse(source);
        List<ClassTree> topLevelTypes = unit.getTypeDecls().stream()
                .filter(ClassTree.class::isInstance)
                .map(ClassTree.class::cast)
                .toList();
        assertThat(topLevelTypes)
                .as("one top-level type in %s", source)
                .hasSize(1);
        assertThat(topLevelTypes.getFirst().getMembers())
                .as("no nested type in %s", source)
                .noneMatch(ClassTree.class::isInstance);
        String className = unit.getPackageName() + "."
                + topLevelTypes.getFirst().getSimpleName();
        Class<?> type = Class.forName(
                className, false, Thread.currentThread().getContextClassLoader());
        assertThat(type.getDeclaredClasses())
                .as("no declared member class in %s", className)
                .isEmpty();
    }
}

@Test
void migratedPackagesDoNotUseLegacyLayerNames() throws Exception {
    for (Path source : migratedSources()) {
        Path relative = adminSourceRoot().relativize(source);
        boolean legacyLayer = StreamSupport.stream(
                        relative.spliterator(), false)
                .map(Path::toString)
                .anyMatch(Set.of("application", "infrastructure")::contains);
        assertThat(legacyLayer)
                .as("target layer name for %s", source)
                .isFalse();
    }
}

private CompilationUnitTree parse(Path source) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager manager = compiler.getStandardFileManager(
            null, Locale.ROOT, StandardCharsets.UTF_8)) {
        JavacTask task = (JavacTask) compiler.getTask(
                null, manager, null, List.of("-proc:none"), null,
                manager.getJavaFileObjects(source.toFile()));
        return task.parse().iterator().next();
    }
}
```

`migratedSources()` must walk `$basedir/src/main/java/top/egon/cola/platform/rbac3/admin`, exclude `package-info.java`, and include a file only when its first relative path segment belongs to `MIGRATED_ROOTS`.

- [ ] **Step 4: Run the new guard and verify RED**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because current `bootstrap` and `config/security` sources still contain member types and old layer paths.

- [ ] **Step 5: Move shared and config foundations without changing behavior**

Move the Appendix A rows for `CommandContext`, `DatabaseClock`, `JpaDatabaseClock`, `TenantScopedEntity`, all current `security/*`, both current `config/*`, Flyway/DDC/Redis/runtime configuration files. Rename `TenantScopedEntity` to `TenantScopedPO`; preserve mapped-superclass annotations, fields, accessors, tenant filters and JavaDoc. Update all consumers across the module in the same step.

For every new package, add bilingual `package-info.java` with this concrete responsibility format:

```java
/**
 * 提供 RBAC3 Admin 配置安全装配，包含认证转换、方法权限和过滤器配置；
 * 业务认证与会话规则不属于本包。
 * Provides RBAC3 Admin security assembly, including authentication conversion,
 * method authorization, and filter configuration; authentication and session
 * business rules do not belong to this package.
 */
package top.egon.cola.platform.rbac3.admin.config.security;
```

Use the corresponding domain/technology wording for every other package; do not copy “security assembly” into unrelated packages.

- [ ] **Step 6: Move bootstrap and extract all wave-1 nested types**

Apply Appendix A bootstrap rows and Appendix B wave 1 exactly. Keep immutable record components and compact constructors unchanged. Move CLI to `bootstrap.controller.cli`. Create `PlatformAdminBootstrapService` as the Controller-facing contract and `DefaultPlatformAdminBootstrapService` in `bootstrap.service.internal`; the default service delegates a `PlatformAdminBootstrapRepository`, whose JPA implementation is `JpaPlatformAdminBootstrapRepository`. `Rbac3DevelopmentBootstrap` continues to depend on `DevelopmentBootstrapPort` as its Repository contract.

The service-to-repository delegation must be direct:

```java
final class DefaultPlatformAdminBootstrapService
        implements PlatformAdminBootstrapService {
    private final PlatformAdminBootstrapRepository repository;

    DefaultPlatformAdminBootstrapService(
            PlatformAdminBootstrapRepository repository) {
        this.repository = repository;
    }

    @Override
    public void bootstrap(String tenantCode, String username, char[] password) {
        repository.bootstrap(tenantCode, username, password);
    }
}
```

Both `PlatformAdminBootstrapService` and `PlatformAdminBootstrapRepository` keep the exact `void bootstrap(String tenantCode, String username, char[] password)` signature, password wiping behavior, validation, transaction and exception behavior.

- [ ] **Step 7: Preserve Spring assembly and update consumers/tests**

Update `Rbac3ApplicationConfiguration`, `Rbac3PlatformIntegrationConfiguration`, security tests, bootstrap tests and every Appendix B wave-1 consumer import. Keep `Rbac3AdminApplication` at the root. Update the original Controller-repository text check in `AdminLayerBoundaryTest` to scan every `*/controller/**/*.java` rather than hard-coded `interfaces/http`.

- [ ] **Step 8: Run focused verification**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest,BootstrapQueryServiceTest,Rbac3DevelopmentAuthorizationContextInitializerTest,Rbac3DevelopmentBootstrapTest,Rbac3DevelopmentTopologyTest,Rbac3PlatformAdminBootstrapCliIT,Rbac3AdminApplicationModeTest,Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
git diff --check
```

Expected: PASS; `bootstrap/config/shared` contain one top-level type per production file and no member types; the module compiles.

- [ ] **Step 9: Commit wave 1 with path-limited staging**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MAIN/bootstrap" \
  "$RBAC3_ADMIN_MAIN/config" \
  "$RBAC3_ADMIN_MAIN/shared" \
  "$RBAC3_ADMIN_MAIN/application" \
  "$RBAC3_ADMIN_MAIN/infrastructure/persistence" \
  "$RBAC3_ADMIN_MAIN/security" \
  "$RBAC3_ADMIN_MAIN/integration/ddc/Rbac3DdcPolicyConfiguration.java" \
  "$RBAC3_ADMIN_MAIN/integration/ddc/Rbac3DdcValueDeclarations.java" \
  "$RBAC3_ADMIN_MAIN/integration/ddc/Rbac3IntegrationMetrics.java" \
  "$RBAC3_ADMIN_MAIN/integration/flyway" \
  "$RBAC3_ADMIN_MAIN/integration/runtime/Rbac3ApplicationConfiguration.java" \
  "$RBAC3_ADMIN_MAIN/integration/runtime/Rbac3GatewayStatusProperties.java" \
  "$RBAC3_ADMIN_MAIN/integration/runtime/Rbac3PlatformIntegrationConfiguration.java" \
  "$RBAC3_ADMIN_MAIN/snapshot/infrastructure/Rbac3RuntimeRedissonConfiguration.java" \
  "$RBAC3_ADMIN_MAIN/worker/Rbac3WorkerConfiguration.java" \
  "$RBAC3_ADMIN_TEST/architecture" \
  "$RBAC3_ADMIN_TEST/bootstrap" \
  "$RBAC3_ADMIN_TEST/integration/Rbac3GatewayDocumentCatalogContractTest.java" \
  "$RBAC3_ADMIN_MODULE/src/test/resources/contracts/rbac3-gateway-catalog-semantic-baseline.json" \
  "$RBAC3_ADMIN_TEST/Rbac3AdminApplicationModeTest.java"
git diff --cached --check
git commit -m "refactor(rbac3): establish admin domain foundations"
```

Expected: one compilable commit containing only wave-1 production/test paths and required consumer import updates. If consumer imports outside the listed paths changed, add those exact files individually before committing.

---

### Task 2: 迁移 tenant、identity、directory 并拆分跨领域入口

**Files:**

- Move/modify: Appendix A rows under `tenant/**`, `identity/**`, `directory/**`
- Split: `interfaces/http/TenantUserDirectoryController.java` into `tenant/controller/TenantController.java`, `identity/controller/UserDirectoryController.java`, `directory/controller/DirectoryController.java`
- Split: `identity/infrastructure/IdentityRepositories.java` into the three Appendix A targets
- Partially split: directory command/query methods from `integration/runtime/Rbac3IdentitySessionQueryStore.java`
- Create: Appendix B wave 2 top-level types
- Create: `directory/service/DefaultDirectoryCommandService.java`, `directory/service/DefaultDirectoryQueryService.java`
- Create: `directory/repository/DirectoryCommandRepository.java`, `directory/repository/DirectoryQueryRepository.java`
- Modify: `architecture/AdminLayerBoundaryTest.java`, `architecture/JpaEntityNameCompatibilityTest.java`
- Modify: `directory/DirectorySnapshotProcessorTest.java`, `identity/application/IdentityMappingFacadeTest.java`, `tenant/TenantContextFilterTest.java`
- Modify: `interfaces/http/InternalIdentityControllerTest.java`, `interfaces/http/ControllerRequestParameterMetadataTest.java`, `interfaces/http/Rbac3RequiredApiSurfaceTest.java`
- Modify: `integration/Rbac3GatewayDocumentCatalogContractTest.java`
- Modify: all Appendix B wave-2 consumer files

**Interfaces:**

- Consumes: Task 1 `shared.repository.DatabaseClock`, root component scan, incremental AST guard.
- Produces: Tenant/Identity/Directory DTO/VO/PO/Enum packages, three focused controllers, Identity repositories, Directory Service/Repository contracts, and a reduced residual `Rbac3IdentitySessionQueryStore` containing only Task-3 contracts.

- [ ] **Step 1: Freeze existing HTTP and identity behavior before moving files**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=InternalIdentityControllerTest,ControllerRequestParameterMetadataTest,Rbac3RequiredApiSurfaceTest,IdentityMappingFacadeTest,DirectorySnapshotProcessorTest,TenantContextFilterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS on the pre-refactor code; record the exact route/test count in the task log.

- [ ] **Step 2: Extend the guard and verify RED**

Add `tenant`, `identity`, `directory` to `MIGRATED_ROOTS`, then run `AdminLayerBoundaryTest`.

Expected: FAIL on nested types and `application/infrastructure` subpackages in those three domains.

- [ ] **Step 3: Move PO/enums and preserve JPA entity names**

Apply Appendix A/B wave 2. Move `TenantEntity` from the identity tree to `tenant.domain.po.TenantPO`. For every migrated Entity, add the original simple name explicitly:

```java
@Entity(name = "TenantEntity")
@Table(name = "rbac3_tenant")
public class TenantPO extends TenantScopedPO {
}
```

Keep each real existing table name and class body; the snippet specifies the entity-name rule, not a replacement table declaration. Extend `JpaEntityNameCompatibilityTest` so each migrated `*PO` annotated with `@Entity` satisfies:

```java
assertThat(type.getAnnotation(Entity.class).name())
        .isEqualTo(type.getSimpleName().replaceFirst("PO$", "Entity"));
```

- [ ] **Step 4: Split `IdentityRepositories` by existing method groups**

- `JpaTenantLookupRepository`: `findTenantByCode`; implement new
  `tenant.repository.TenantLookupRepository` with the unchanged signature
  `Optional<TenantPO> findTenantByCode(String tenantCode)` so configuration
  and services do not depend on the JPA implementation class.
- `JpaIdentityMappingRepository`: `find`, `create`, `resolve`, `tenants`, `activeMemberships`, mapping conversion helpers.
- `JpaPasswordCredentialRepository`: `withCredential`, `save`, `updatePasswordHash`, `findCredential`, credential row helper.

Preserve `@Transactional`, `readOnly`, row locking, query text, ordering and exception behavior. `MembershipRow` and `CredentialRow` become Appendix-B package-private files under `identity.repository.internal`; if package-private access crosses `jpa`/`internal`, place the helper beside its sole JPA consumer instead of making it public.

- [ ] **Step 5: Split `TenantUserDirectoryController` by exact method ownership**

- `TenantController`: `tenants`, `createTenant`, `changeTenantStatus`, `tenant`.
- `UserDirectoryController`: `users`, `user`, `changeUserStatus`.
- `DirectoryController`: `orgUnits`, `positions`, `submit`, `snapshot`.

Copy the original class/method `@RequestMapping`, `@GatewayOperation`, permission and validation annotations so the composed full paths remain byte-for-byte equal. Move DTO/VO declarations according to Appendix B wave 2.

- [ ] **Step 6: Extract directory persistence and add Controller-facing services**

From `Rbac3IdentitySessionQueryStore`, extract `submit`, `createTenant`, `changeTenantStatus`, `changeUserStatus` to `JpaDirectoryCommandRepository`; extract `findUser`, `findTenant`, tenant/user pages, org units, positions and snapshot query to `JpaDirectoryQueryRepository`. Retain exact JPQL, sort and transaction annotations.

Create `DefaultDirectoryCommandService` and `DefaultDirectoryQueryService` implementing the Appendix-B `DirectoryCommandService`/`DirectoryQueryService` contracts and delegating to Repository contracts. Controllers inject these Service types, never JPA classes.

- [ ] **Step 7: Update consumers, JavaDoc and package docs**

Update all Appendix B consumers and test imports. Add bilingual package/type/method/field documentation. Leave the residual `Rbac3IdentitySessionQueryStore` in its old package for Task 3, but remove all directory Controller Port implementations and directory-only collaborators from it.

Update `Rbac3GatewayDocumentCatalogContractTest` to discover both migrated and not-yet-migrated controllers during Tasks 2-7:

```java
private static final String ADMIN_PACKAGE =
        "top.egon.cola.platform.rbac3.admin.";
private static final String LEGACY_HTTP_PACKAGE =
        "top.egon.cola.platform.rbac3.admin.interfaces.http";

private boolean isRbac3Controller(Class<?> type) {
    String packageName = type.getPackageName();
    boolean migrated = packageName.startsWith(ADMIN_PACKAGE)
            && (packageName.endsWith(".controller")
            || packageName.contains(".controller."));
    return (packageName.equals(LEGACY_HTTP_PACKAGE) || migrated)
            && AnnotatedElementUtils.findMergedAnnotation(
                    type, RestController.class) != null;
}
```

Use this predicate in actual mapping and group assertions until Task 8 removes the legacy branch. Continue comparing the normalized catalog against the Task-1 fixture after every wave.

- [ ] **Step 8: Verify wave 2**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest,JpaEntityNameCompatibilityTest,InternalIdentityControllerTest,ControllerRequestParameterMetadataTest,Rbac3RequiredApiSurfaceTest,IdentityMappingFacadeTest,DirectorySnapshotProcessorTest,TenantContextFilterTest,Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
git diff --check
```

Expected: PASS; three controllers expose the original routes; tenant/identity/directory have no nested production type or old application/infrastructure package.

- [ ] **Step 9: Commit wave 2**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MAIN/tenant" \
  "$RBAC3_ADMIN_MAIN/identity" \
  "$RBAC3_ADMIN_MAIN/directory" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/TenantUserDirectoryController.java" \
  "$RBAC3_ADMIN_MAIN/integration/runtime/Rbac3IdentitySessionQueryStore.java" \
  "$RBAC3_ADMIN_TEST/architecture" \
  "$RBAC3_ADMIN_TEST/directory" \
  "$RBAC3_ADMIN_TEST/identity" \
  "$RBAC3_ADMIN_TEST/tenant" \
  "$RBAC3_ADMIN_TEST/interfaces/http/InternalIdentityControllerTest.java" \
  "$RBAC3_ADMIN_TEST/interfaces/http/ControllerRequestParameterMetadataTest.java" \
  "$RBAC3_ADMIN_TEST/interfaces/http/Rbac3RequiredApiSurfaceTest.java"
git add -- "$RBAC3_ADMIN_TEST/integration/Rbac3GatewayDocumentCatalogContractTest.java"
git diff --cached --check
git commit -m "refactor(rbac3): align tenant identity and directory packages"
```

---

### Task 3: 迁移 auth、session 并删除身份会话巨型 Store

**Files:**

- Move/modify: Appendix A rows under `auth/**` and `session/**`
- Move/modify: `interfaces/http/AuthController.java`, `interfaces/http/SessionController.java`
- Delete after split: `integration/runtime/Rbac3IdentitySessionQueryStore.java`
- Create: remaining JPA adapters listed by that Appendix A `SPLIT:` row
- Create: `session/service/DefaultSessionManagementService.java`, `session/repository/SessionManagementRepository.java`
- Create: `assignment/repository/jpa/JpaAssignmentSessionStrengthRepository.java`
- Create: Appendix B wave 3 types and affected package-info files
- Modify: `architecture/AdminLayerBoundaryTest.java`, `architecture/JpaEntityNameCompatibilityTest.java`
- Modify: auth/session tests and `integration/Rbac3AdminApplicationContextTest.java`
- Modify: `integration/Rbac3GatewayDocumentCatalogContractTest.java`
- Modify: all Appendix B wave-3 consumers

**Interfaces:**

- Consumes: Task 2 PO and directory split; Task 1 bootstrap Repository contract.
- Produces: Auth/Session vertical packages, Controller-facing session service, focused JPA adapters for all remaining nine-interface Store methods, and no `Rbac3IdentitySessionQueryStore`.

- [ ] **Step 1: Freeze auth/session behavior**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AuthenticationFacadeTest,AuthenticationRuntimePublicationTest,JwtKeyRingServiceTest,JwtTokenServiceTest,RefreshRuntimePublicationTest,StepUpFacadeTest,RefreshTokenConcurrencyIT,SessionFacadeTest,AuthorizationContextFacadeTest,SessionSecurityEventRecorderTest,SessionStepUpTest,AuthControllerTransportContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS before moves.

- [ ] **Step 2: Extend guard to `auth/session` and verify RED**

Add `auth` and `session` to `MIGRATED_ROOTS`; run `AdminLayerBoundaryTest` and expect failures on current nested types and old subpackages.

- [ ] **Step 3: Extract all wave-3 types and move services/controllers**

Apply Appendix A/B wave 3. Preserve record components, Password/JWT/Refresh validation, exception reason codes and enum constant order. Move `AuthController` and `SessionController` into their domain controller packages. `SessionController` injects `SessionManagementService`.

- [ ] **Step 4: Split the residual query Store into focused adapters**

Create these adapters from the exact original method groups:

- `JpaLoginStateRepository`: `load(tenantCode, userId, now)`.
- `JpaRefreshStateRepository`: `load(familyId)`.
- `JpaBootstrapSnapshotRepository`: `find(tenantId, userId, sessionId, now)`.
- `JpaSessionManagementRepository`: `findByUser`, `revoke`, `revokeAll`.
- `JpaAssignmentSessionStrengthRepository`: `authenticationStrength`; it temporarily implements the still-unmigrated `AssignmentController.SessionStrengthPort` so Task 3 can delete the giant Store without moving Assignment's wave-5 API early.
- `JpaStepUpIdentityRepository`: Step-up `load(tenantId, userId)`.
- `JpaStepUpSessionStrengthRepository`: `strengthen`.

Move common helpers to the narrowest consuming adapter; do not recreate a shared giant query utility. Add `DefaultSessionManagementService` so `SessionController` depends on a Service contract. Delete the residual original Store only after all nine contracts are wired. Task 5 replaces the temporary Assignment nested-Port implementation with a Service-to-Repository delegate.

- [ ] **Step 5: Preserve JPA and runtime behavior**

Rename `SessionEntity`/`RefreshTokenEntity` to `SessionPO`/`RefreshTokenPO`, add entity names `SessionEntity`/`RefreshTokenEntity`, and extend the compatibility test. Preserve `PESSIMISTIC_WRITE`, refresh-family rotation, replay detection, session security events, runtime publication and transaction boundaries.

- [ ] **Step 6: Update configuration, mocks and JavaDoc**

Update configuration bean wiring and Auth/Session `@WebMvcTest` `@MockitoBean` declarations from nested Controller Ports to top-level Service contracts. Update all Appendix B wave-3 consumer imports. Add package/type/method/field bilingual JavaDoc.

- [ ] **Step 7: Verify wave 3**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest,JpaEntityNameCompatibilityTest,AuthenticationFacadeTest,AuthenticationRuntimePublicationTest,JwtKeyRingServiceTest,JwtTokenServiceTest,RefreshRuntimePublicationTest,StepUpFacadeTest,RefreshTokenConcurrencyIT,SessionFacadeTest,AuthorizationContextFacadeTest,SessionSecurityEventRecorderTest,SessionStepUpTest,AuthControllerTransportContractTest,Rbac3AdminApplicationContextTest,Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
git diff --check
```

Expected: PASS; original giant Store no longer exists; Auth/Session controllers resolve through Service contracts.

- [ ] **Step 8: Commit wave 3**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MAIN/auth" \
  "$RBAC3_ADMIN_MAIN/session" \
  "$RBAC3_ADMIN_MAIN/assignment/repository/jpa/JpaAssignmentSessionStrengthRepository.java" \
  "$RBAC3_ADMIN_MAIN/bootstrap/repository/jpa/JpaBootstrapSnapshotRepository.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/AuthController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/SessionController.java" \
  "$RBAC3_ADMIN_MAIN/integration/runtime/Rbac3IdentitySessionQueryStore.java" \
  "$RBAC3_ADMIN_TEST/auth" \
  "$RBAC3_ADMIN_TEST/session" \
  "$RBAC3_ADMIN_TEST/architecture" \
  "$RBAC3_ADMIN_TEST/integration/Rbac3AdminApplicationContextTest.java" \
  "$RBAC3_ADMIN_TEST/integration/Rbac3GatewayDocumentCatalogContractTest.java" \
  "$RBAC3_ADMIN_TEST/interfaces/http/AuthControllerTransportContractTest.java"
git diff --cached --check
git commit -m "refactor(rbac3): align authentication and session packages"
```

---

### Task 4: 迁移 resource、role 及闭包存储

**Files:**

- Move/modify: Appendix A `resource/**`, `role/**` rows and their controllers
- Create: Appendix B wave 4 types and package-info files
- Modify: `architecture/AdminLayerBoundaryTest.java`, `architecture/JpaEntityNameCompatibilityTest.java`
- Modify: resource/role tests and Gateway discovery tests that import moved controllers/types
- Modify: all Appendix B wave-4 consumers

**Interfaces:**

- Consumes: Task 1 shared/runtime publication contracts.
- Produces: Resource and Role vertical packages, PO/enums, top-level Facade inputs/outputs, JPA repositories and `PostgresqlRoleClosureRepository`.

- [ ] **Step 1: Run characterization tests**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=ApplicationResourceFacadeTest,ManifestFacadeIT,RoleControlFacadeTest,RoleHierarchyConcurrencyIT,Rbac3GatewayDefinitionDiscoveryTest,Rbac3RoleActivationGatewayDiscoveryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [ ] **Step 2: Extend guard to resource/role and verify RED**

Add `resource`, `role`; run the architecture test and expect current member types/old subpackages to fail.

- [ ] **Step 3: Migrate Resource aggregate and HTTP types**

Apply Appendix A/B resource rows. Keep Application, Resource, Permission and Manifest in the single `resource` domain. Move HTTP request records to DTO, Facade/HTTP result records to VO, entity classes to PO, enums to `domain.enums`, and Repository implementations to `repository.jpa`. Preserve manifest validation, archive semantics, permission risk, field definition metadata and Gateway annotations.

- [ ] **Step 4: Migrate Role aggregate and closure implementation**

Apply Appendix A/B role rows. Keep `RoleFacade` intact after extracting its eleven member types. Rename `PostgresqlRoleClosureStore` to `PostgresqlRoleClosureRepository`; preserve recursive CTE/closure SQL, lock order and concurrency behavior. Rename JPA types to PO with explicit original entity names.

- [ ] **Step 5: Update consumers, docs and JPA checks**

Update all Appendix-B consumers, tests and configuration. Extend PO entity-name coverage. Add bilingual package/type/method/field JavaDoc.

- [ ] **Step 6: Verify wave 4**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest,JpaEntityNameCompatibilityTest,ApplicationResourceFacadeTest,ManifestFacadeIT,RoleControlFacadeTest,RoleHierarchyConcurrencyIT,Rbac3GatewayDefinitionDiscoveryTest,Rbac3RoleActivationGatewayDiscoveryTest,Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
git diff --check
```

Expected: PASS; resource/role have no nested production types or application/infrastructure packages.

- [ ] **Step 7: Commit wave 4**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MAIN/resource" \
  "$RBAC3_ADMIN_MAIN/role" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/ApplicationResourceController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/ManifestController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/RolePermissionController.java" \
  "$RBAC3_ADMIN_TEST/resource" \
  "$RBAC3_ADMIN_TEST/role" \
  "$RBAC3_ADMIN_TEST/architecture" \
  "$RBAC3_ADMIN_TEST/integration/Rbac3GatewayDocumentCatalogContractTest.java" \
  "$RBAC3_ADMIN_TEST/interfaces/http"
git diff --cached --check
git commit -m "refactor(rbac3): align resource and role packages"
```

---

### Task 5: 迁移 assignment、activation、constraint、management

**Files:**

- Move/modify: Appendix A rows for the four domains and their controllers
- Create: Appendix B wave 5 types and package-info files
- Modify: `architecture/AdminLayerBoundaryTest.java`, `architecture/JpaEntityNameCompatibilityTest.java`
- Modify: assignment/activation/constraint/management tests and impacted integration tests
- Modify: all Appendix B wave-5 consumers

**Interfaces:**

- Consumes: Task 3 temporary `JpaAssignmentSessionStrengthRepository`; Task 4 Role/Resource types.
- Produces: four complete vertical domain trees; Facades remain as orchestration boundaries, all nested contracts/data types become top-level.

- [ ] **Step 1: Run characterization/concurrency tests**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AssignmentFacadeIT,RoleCardinalityConcurrencyIT,ActiveRoleSetRevalidatorTest,RoleActivationCandidateServiceTest,RoleActivationConcurrencyIT,RoleActivationFacadeIT,SessionActiveRoleRepositoryTest,ConstraintFacadeTest,ConstraintPersistenceEntityTest,ManagementPolicyFacadeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [ ] **Step 2: Extend guard and verify RED**

Add `assignment`, `activation`, `constraint`, `management` to `MIGRATED_ROOTS`; run `AdminLayerBoundaryTest` and expect failures.

- [ ] **Step 3: Migrate Assignment and Activation**

Apply Appendix A/B rows. Extract `AssignmentController.SessionStrengthPort` to `AssignmentSessionStrengthService`, introduce `AssignmentSessionStrengthRepository`, and add `DefaultAssignmentSessionStrengthService` delegating to the Task-3 `JpaAssignmentSessionStrengthRepository`; after this change the Controller no longer depends on the Repository implementation. Preserve assignment locks, cardinality checks, version conflicts, active-role reselection, Step-up requirements and runtime publication. Move PO/enums with explicit original JPA entity names. Keep `AssignmentFacade`, `RoleActivationFacade`, `ActiveRoleSetRevalidator`, `RoleActivationCandidateService`; only extract types and move packages.

- [ ] **Step 4: Migrate Constraint and Management**

Apply Appendix A/B rows. Keep `ConstraintFacade` and `ManagementPolicyFacade` intact after extraction. Rename current concrete `ConstraintRepository` to `JpaConstraintRepository`; extracted `ConstraintFacade.ConstraintStore` becomes the top-level contract `constraint.repository.ConstraintRepository`. Preserve SOD/prerequisite/cardinality/data/field rules, policy restrictions, subject/scope meaning, query order and transactions.

- [ ] **Step 5: Update HTTP/controller service dependencies and docs**

Move the four domain controllers, update top-level DTO/VO usage, ensure no Controller injects a JPA/JDBC class, update all consumers/tests and add bilingual documentation.

- [ ] **Step 6: Verify wave 5**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest,JpaEntityNameCompatibilityTest,AssignmentFacadeIT,RoleCardinalityConcurrencyIT,ActiveRoleSetRevalidatorTest,RoleActivationCandidateServiceTest,RoleActivationConcurrencyIT,RoleActivationFacadeIT,SessionActiveRoleRepositoryTest,ConstraintFacadeTest,ConstraintPersistenceEntityTest,ManagementPolicyFacadeTest,Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
git diff --check
```

Expected: PASS with original concurrency and validation behavior.

- [ ] **Step 7: Commit wave 5**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MAIN/assignment" \
  "$RBAC3_ADMIN_MAIN/activation" \
  "$RBAC3_ADMIN_MAIN/constraint" \
  "$RBAC3_ADMIN_MAIN/management" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/AssignmentController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/RoleActivationController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/ConstraintController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/ManagementPolicyController.java" \
  "$RBAC3_ADMIN_TEST/assignment" \
  "$RBAC3_ADMIN_TEST/activation" \
  "$RBAC3_ADMIN_TEST/constraint" \
  "$RBAC3_ADMIN_TEST/management" \
  "$RBAC3_ADMIN_TEST/architecture" \
  "$RBAC3_ADMIN_TEST/integration/Rbac3GatewayDocumentCatalogContractTest.java"
git diff --cached --check
git commit -m "refactor(rbac3): align assignment policy domains"
```

---

### Task 6: 迁移 authorization、participation、audit、simulation 并拆分审计模拟入口

**Files:**

- Move/modify: Appendix A rows for the four domains and their controllers
- Split: `interfaces/http/AuditSimulationController.java`
- Create: Appendix B wave 6 types and package-info files
- Modify: `architecture/AdminLayerBoundaryTest.java`, `architecture/JpaEntityNameCompatibilityTest.java`
- Modify: authorization/participation/audit/simulation tests and affected Gateway discovery tests
- Modify: all Appendix B wave-6 consumers

**Interfaces:**

- Consumes: Task 4 Role/Resource and Task 5 Constraint/Management public contracts.
- Produces: four vertical domain trees, `AuditController`, `AuthorizationSimulationController`, standalone authorization/audit/simulation request/result types.

- [ ] **Step 1: Run characterization tests**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AuthorizationDecisionServiceTest,ParticipationConcurrencyIT,AuditCursorCodecTest,AuditRedactionIT,PostgresqlAuditStoreTest,AuthorizationSimulationServiceTest,PostgresqlRoleImpactSourceTest,Rbac3DecisionRuntimeGatewayDiscoveryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [ ] **Step 2: Extend guard and verify RED**

Add `authorization`, `participation`, `audit`, `simulation`; run structure test and expect failures.

- [ ] **Step 3: Migrate Authorization and Participation**

Apply Appendix A/B rows. Preserve snapshot/fence verification, decision type, resource-access output, SOD conflict behavior and participation append semantics. Move Controller HTTP inputs/outputs to DTO/VO and Repository implementations to technology packages.

- [ ] **Step 4: Split `AuditSimulationController` and migrate both domains**

- `AuditController`: only `GET /audit-logs` and audit dependencies.
- `AuthorizationSimulationController`: `POST /simulations/authorization` and `POST /simulations/role-change-impact`.

Preserve the original complete paths, Gateway annotations, validation and response envelopes. Apply Appendix B wave-6 mappings; rename `PostgresqlAuditStore`/`PostgresqlRoleImpactSource` to Repository names without changing SQL.

- [ ] **Step 5: Update contracts, docs and tests**

Update all consumers/imports, WebMvc mocks and Gateway discovery fixtures. Extend entity-name checks for Participation/Audit PO. Add bilingual package/type/method/field JavaDoc.

- [ ] **Step 6: Verify wave 6**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest,JpaEntityNameCompatibilityTest,AuthorizationDecisionServiceTest,ParticipationConcurrencyIT,AuditCursorCodecTest,AuditRedactionIT,PostgresqlAuditStoreTest,AuthorizationSimulationServiceTest,PostgresqlRoleImpactSourceTest,Rbac3DecisionRuntimeGatewayDiscoveryTest,Rbac3RequiredApiSurfaceTest,Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
git diff --check
```

Expected: PASS; Audit and Simulation operations remain discoverable with unchanged routes.

- [ ] **Step 7: Commit wave 6**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MAIN/authorization" \
  "$RBAC3_ADMIN_MAIN/participation" \
  "$RBAC3_ADMIN_MAIN/audit" \
  "$RBAC3_ADMIN_MAIN/simulation" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/InternalAuthorizationController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/ParticipationController.java" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/AuditSimulationController.java" \
  "$RBAC3_ADMIN_TEST/authorization" \
  "$RBAC3_ADMIN_TEST/participation" \
  "$RBAC3_ADMIN_TEST/audit" \
  "$RBAC3_ADMIN_TEST/simulation" \
  "$RBAC3_ADMIN_TEST/architecture" \
  "$RBAC3_ADMIN_TEST/integration/Rbac3GatewayDocumentCatalogContractTest.java" \
  "$RBAC3_ADMIN_TEST/interfaces/http/Rbac3DecisionRuntimeGatewayDiscoveryTest.java" \
  "$RBAC3_ADMIN_TEST/interfaces/http/Rbac3RequiredApiSurfaceTest.java"
git diff --cached --check
git commit -m "refactor(rbac3): align decision and audit domains"
```

---

### Task 7: 统一 runtime，吸收 snapshot、worker、DDC、Gateway、Outbox

**Files:**

- Move/modify: Appendix A rows targeting `runtime/**` and remaining `config/ddc|redis|runtime/**`
- Delete after move: old `snapshot/**`, `worker/**`, `integration/ddc/**`, `integration/gateway/**`, `integration/outbox/**`, `integration/runtime/**`
- Move/modify: `interfaces/http/RuntimeController.java`
- Create: Appendix B wave 7 types and package-info files
- Modify: `architecture/AdminLayerBoundaryTest.java`, `architecture/JpaEntityNameCompatibilityTest.java`
- Modify: runtime/snapshot/worker/integration tests and configuration tests
- Modify: all Appendix B wave-7 consumers

**Interfaces:**

- Consumes: all domain contracts from Tasks 1-6.
- Produces: one runtime domain containing scheduled/message inbound adapters, service orchestration, JPA/Redis/DDC/HTTP/Outbox outbound adapters and runtime DTO/VO/PO/enums; no production `snapshot`, `worker` or `integration` roots remain.

- [ ] **Step 1: Run runtime/integration characterization tests**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AuthorizationMutationRepositoryTest,IdempotencyServiceTest,MutationFenceRollbackIT,RuntimeQueryServiceTest,RedisAuthorizationRuntimeStoreIT,SystemAuthorizationSnapshotServiceTest,AuthorizationWorkerRecoveryIT,DdcConfigClientStatusServiceTest,GatewayAdminControlPlaneStatusClientTest,GatewayDdcConfigurationTest,OutboxTransactionRollbackIT,Rbac3DdcRefreshIntegrationTest,Rbac3HttpProviderPublicationGateTest,Rbac3IntegrationMetricsTest,AtomicRbac3RuntimePolicyTest,Rbac3DdcPolicyConfigurationTest,Rbac3ApplicationConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS, or record any pre-existing source/API drift separately before changing runtime files; do not weaken tests to manufacture GREEN.

- [ ] **Step 2: Extend guard to runtime and verify RED**

Add `runtime` to `MIGRATED_ROOTS`; run structure test and expect failures on nested types/current application/infrastructure subpackages.

- [ ] **Step 3: Move runtime core and PO/enums**

Apply Appendix A/B runtime rows. Rename `AuthorizationMutationEntity` and `IdempotencyRecordEntity` to PO with original entity names. Move core services from `runtime.application` to `runtime.service`, Repository implementations to `runtime.repository.jpa`, and top-level contracts to `runtime.repository` or `runtime.service.internal` exactly as Appendix B specifies.

- [ ] **Step 4: Absorb snapshot and worker**

Move snapshot Services to `runtime.service`, Redis adapter to `runtime.repository.redis`, Redisson config to `config.redis`. Move scheduled workers to `runtime.controller.scheduled`; their persistence contracts/implementations go to `runtime.repository` technology packages and orchestration interfaces to `runtime.service`. Preserve schedules, distributed claim semantics, checkpoint keys, retry behavior and transaction boundaries.

- [ ] **Step 5: Absorb DDC, Gateway and Outbox**

- DDC assembly: `config.ddc`; DDC runtime adapters: `runtime.repository.ddc`.
- Gateway status clients: `runtime.repository.http`.
- Outbox handler inbound entry: `runtime.controller.message`; publisher/adapter: `runtime.repository.outbox`.
- Platform integration configuration: `config.runtime`.

Extract every private/public nested helper according to Appendix B. Keep package-private helpers beside their sole consumer. Preserve provider lease, schema status, credentials, event fields and retry outcomes.

- [ ] **Step 6: Update runtime controller, configuration and tests**

Move `RuntimeController`, update Service/VO imports, WebMvc mocks and bean wiring. Move test packages where their old production package no longer exists: snapshot tests under `runtime`, worker tests under `runtime`, DDC/runtime configuration tests under `config` or `runtime` according to the production subject. Add bilingual JavaDoc/package-info.

- [ ] **Step 7: Verify wave 7**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am \
  -Dtest=AdminLayerBoundaryTest,JpaEntityNameCompatibilityTest,AuthorizationMutationRepositoryTest,IdempotencyServiceTest,MutationFenceRollbackIT,RuntimeQueryServiceTest,RedisAuthorizationRuntimeStoreIT,SystemAuthorizationSnapshotServiceTest,AuthorizationWorkerRecoveryIT,DdcConfigClientStatusServiceTest,GatewayAdminControlPlaneStatusClientTest,GatewayDdcConfigurationTest,OutboxTransactionRollbackIT,Rbac3DdcRefreshIntegrationTest,Rbac3HttpProviderPublicationGateTest,Rbac3IntegrationMetricsTest,AtomicRbac3RuntimePolicyTest,Rbac3DdcPolicyConfigurationTest,Rbac3ApplicationConfigurationTest,Rbac3GatewayDocumentCatalogContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
git diff --check
```

Expected: PASS; production `snapshot/worker/integration` roots contain no Java file.

- [ ] **Step 8: Commit wave 7**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MAIN/runtime" \
  "$RBAC3_ADMIN_MAIN/config" \
  "$RBAC3_ADMIN_MAIN/snapshot" \
  "$RBAC3_ADMIN_MAIN/worker" \
  "$RBAC3_ADMIN_MAIN/integration" \
  "$RBAC3_ADMIN_MAIN/interfaces/http/RuntimeController.java" \
  "$RBAC3_ADMIN_TEST/runtime" \
  "$RBAC3_ADMIN_TEST/snapshot" \
  "$RBAC3_ADMIN_TEST/worker" \
  "$RBAC3_ADMIN_TEST/integration" \
  "$RBAC3_ADMIN_TEST/architecture"
git diff --cached --check
git commit -m "refactor(rbac3): consolidate admin runtime packages"
```

---

### Task 8: 删除旧技术根、启用全局守卫并完成契约验收

**Files:**

- Modify: `architecture/AdminLayerBoundaryTest.java`, `architecture/Rbac3ModuleBoundaryTest.java`, `architecture/JpaEntityNameCompatibilityTest.java`
- Modify: `integration/Rbac3GatewayDocumentCatalogContractTest.java`
- Modify: all remaining Admin tests importing old packages
- Delete: remaining production Java/package-info files under old roots
- Modify: relevant RBAC3 Admin documentation/package-info files only

**Interfaces:**

- Consumes: completed Tasks 1-7 and all Appendix A/B mappings.
- Produces: global zero-nested/zero-old-root enforcement, normalized Gateway schema compatibility check, complete test import migration and final green module.

- [ ] **Step 1: Switch incremental guards to global enforcement and verify RED if cleanup remains**

Replace `MIGRATED_ROOTS` filtering with all production Java sources. Add old-root absence:

```java
private static final Set<String> FORBIDDEN_ROOTS = Set.of(
        "application", "interfaces", "infrastructure", "integration",
        "security", "worker", "snapshot");

@Test
void oldTechnicalRootsContainNoProductionJava() throws Exception {
    for (String root : FORBIDDEN_ROOTS) {
        Path directory = adminSourceRoot().resolve(root);
        if (Files.exists(directory)) {
            try (Stream<Path> files = Files.walk(directory)) {
                assertThat(files.filter(path -> path.toString().endsWith(".java")))
                        .as("no Java source under old root %s", root)
                        .isEmpty();
            }
        }
    }
}
```

Add import/package dependency checks for Domain, Controller, Service and Repository rules using the parsed package/import names:

```java
private void assertLayerImports(CompilationUnitTree unit, Path source) {
    String packageName = unit.getPackageName().toString();
    Set<String> imports = unit.getImports().stream()
            .map(tree -> tree.getQualifiedIdentifier().toString())
            .filter(name -> name.startsWith(ADMIN_PACKAGE))
            .collect(Collectors.toSet());

    if (packageName.contains(".domain")) {
        assertThat(imports).noneMatch(name ->
                name.contains(".controller.")
                        || name.contains(".service.")
                        || name.contains(".repository."));
    }
    if (packageName.contains(".controller")) {
        String domain = domainPrefix(packageName);
        assertThat(imports).allMatch(name ->
                name.startsWith(domain + ".controller.")
                        || name.startsWith(domain + ".domain.")
                        || name.startsWith(domain + ".service.")
                        || name.startsWith(ADMIN_PACKAGE + "shared.domain.")
                        || name.startsWith(ADMIN_PACKAGE + "config.security."));
    }
    if (packageName.contains(".service")) {
        assertThat(imports).noneMatch(name ->
                name.contains(".controller.")
                        || name.matches(".*\\.repository\\."
                        + "(jpa|jdbc|redis|http|ddc|outbox|internal)\\..*"));
    }
    if (packageName.contains(".repository")) {
        assertThat(imports).noneMatch(name ->
                name.contains(".controller.")
                        || name.contains(".service."));
    }
}

private String domainPrefix(String packageName) {
    String suffix = packageName.substring(ADMIN_PACKAGE.length());
    int separator = suffix.indexOf('.');
    String root = separator < 0 ? suffix : suffix.substring(0, separator);
    return ADMIN_PACKAGE + root;
}
```

Invoke this helper for every production source except `config`, the explicit assembly root. Run the guard; any failure now represents unfinished migration and must be fixed in this task.

- [ ] **Step 2: Update Gateway Controller discovery to all domain controller packages**

In `Rbac3GatewayDocumentCatalogContractTest`, replace equality with the old `HTTP_PACKAGE` by these predicates:

```java
private static final String ADMIN_PACKAGE =
        "top.egon.cola.platform.rbac3.admin.";

private boolean isRbac3Controller(Class<?> type) {
    String packageName = type.getPackageName();
    return packageName.startsWith(ADMIN_PACKAGE)
            && (packageName.endsWith(".controller")
            || packageName.contains(".controller."))
            && AnnotatedElementUtils.findMergedAnnotation(
                    type, RestController.class) != null;
}
```

Use `isRbac3Controller` in both actual mapping collection and discovered group assertions. This must include bootstrap, every business `@RestController` and runtime endpoints; `Rbac3ApiExceptionHandler` remains a `@RestControllerAdvice` and is intentionally not a Gateway operation group.

- [ ] **Step 3: Compare the migrated catalog with the Task-1 semantic baseline**

Resolve local `$ref` targets recursively instead of comparing `$defs` names. A recursive reference is represented by its stable stack distance, so package/simple-name hash changes disappear while the schema graph remains comparable:

```java
private Object normalizeSchema(Map<String, Object> schema) {
    Map<String, Object> definitions = new LinkedHashMap<>();
    if (schema.get("$defs") instanceof Map<?, ?> rawDefinitions) {
        rawDefinitions.forEach((key, value) ->
                definitions.put(key.toString(), value));
    }
    return expandSchema(schema, definitions, new ArrayList<>());
}

private Object expandSchema(
        Object value,
        Map<String, Object> definitions,
        List<String> referenceStack) {
    if (value instanceof Map<?, ?> map) {
        Object reference = map.get("$ref");
        Object resolved = null;
        if (reference instanceof String ref
                && ref.startsWith("#/$defs/")) {
            String key = ref.substring("#/$defs/".length());
            int cycleStart = referenceStack.indexOf(key);
            if (cycleStart >= 0) {
                resolved = Map.of(
                        "$recursiveDepth",
                        referenceStack.size() - cycleStart);
            } else {
                Object definition = Objects.requireNonNull(
                        definitions.get(key), "missing schema definition " + key);
                referenceStack.add(key);
                resolved = expandSchema(definition, definitions, referenceStack);
                referenceStack.removeLast();
            }
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        map.entrySet().stream()
                .filter(entry -> !Set.of("$defs", "javaType", "$ref")
                        .contains(entry.getKey().toString()))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> normalized.put(
                        entry.getKey().toString(),
                        expandSchema(entry.getValue(), definitions, referenceStack)));
        if (resolved != null && normalized.isEmpty()) {
            return resolved;
        }
        if (resolved != null) {
            normalized.put("$resolved", resolved);
        } else if (reference != null) {
            normalized.put("$ref", reference);
        }
        return normalized;
    }
    if (value instanceof Collection<?> collection) {
        return collection.stream()
                .map(item -> expandSchema(item, definitions, referenceStack))
                .toList();
    }
    return value;
}
```

Run `Rbac3GatewayDocumentCatalogContractTest` without the update property and compare against `src/test/resources/contracts/rbac3-gateway-catalog-semantic-baseline.json` created before Task 1 moves. Do not regenerate the fixture: a failure means Route, operation metadata or normalized schema semantics changed and must be repaired. The normalizer intentionally drops only local definition keys and the diagnostic `javaType`; it preserves `type`, `format`, `properties`, `required`, `enum`, array items, validation limits and descriptions.

- [ ] **Step 4: Complete old-reference and one-type-per-file scans**

```bash
rg -n 'top\.egon\.cola\.platform\.rbac3\.admin\.(application|interfaces|infrastructure|integration|security|worker|snapshot)' \
  "$RBAC3_ADMIN_MODULE/src/main" "$RBAC3_ADMIN_MODULE/src/test"
rg -n '\b(Controller|Service|Facade|Processor|Coordinator|Revalidator|Factory)\.[A-Z][A-Za-z0-9_]*' \
  "$RBAC3_ADMIN_MODULE/src/main" "$RBAC3_ADMIN_MODULE/src/test"
```

Expected: no production/test reference to old packages or functional-class nested types. Annotation qualified names from external modules are outside this regex scope and remain unchanged.

- [ ] **Step 5: Verify all PO, package docs and type docs**

Run the global `JpaEntityNameCompatibilityTest`. Compare all real production directories with `package-info.java`; fail if a directory containing Java types lacks package docs. Inspect every migrated public type for bilingual class/member JavaDoc; package-private internal helpers require bilingual class-level responsibility documentation but not public API tags.

- [ ] **Step 6: Run the final full verification**

```bash
mvn -pl "$RBAC3_ADMIN_MODULE" -am -DskipTests compile
mvn -pl "$RBAC3_ADMIN_MODULE" -am test
git diff --check
```

Expected: Maven exits 0 with zero test failures/errors. Do not describe a partial reactor result as full success.

- [ ] **Step 7: Produce final structural evidence**

```bash
find "$RBAC3_ADMIN_MAIN" -name '*.java' -type f | wc -l
find "$RBAC3_ADMIN_MAIN" -type d | sort
rg -n '\b(public|protected|private)?\s*(static\s+)?(record|class|enum|interface)\s+[A-Z]' \
  "$RBAC3_ADMIN_MAIN"
git status --short
```

Interpret the declaration scan with the AST test as authority: the AST test must report zero member types; the text scan is an audit aid. Confirm old roots contain no production Java, every Appendix A source has a target, and every Appendix B row is removed from its host.

- [ ] **Step 8: Commit wave 8**

```bash
git add -A -- \
  "$RBAC3_ADMIN_MODULE/src/main/java/top/egon/cola/platform/rbac3/admin" \
  "$RBAC3_ADMIN_MODULE/src/test/java/top/egon/cola/platform/rbac3/admin" \
  "$RBAC3_ADMIN_MODULE/src/test/resources/contracts/rbac3-gateway-catalog-semantic-baseline.json"
git diff --cached --check
git diff --cached --name-only
git commit -m "refactor(rbac3): enforce admin package boundaries"
```

Expected: staged files are limited to RBAC3 Admin source/test/docs; no Gateway Admin user changes are included. The eighth commit is green by compile and full module test evidence.

---

## Spec Coverage Matrix

| Approved spec section | Implementing plan location |
|---|---|
| §1 Decisions, §3 Goals/non-goals | Global Constraints; all Tasks |
| §2 Baseline/census | Execution Invariants; Appendices A/B |
| §4 Target package tree | Appendix A; Tasks 1-7; Task 8 old-root guard |
| §5 Type independence/naming | Appendix B; incremental AST guard; Task 8 global guard |
| §6 Dependency/data flow | Global Constraints; Controller-facing Service adapters; Task 8 import guard |
| §7 Four mandatory host splits | Task 2 `TenantUserDirectoryController`/`IdentityRepositories`; Tasks 2-3 `Rbac3IdentitySessionQueryStore`; Task 6 `AuditSimulationController` |
| §8 JPA/database compatibility | `JpaEntityNameCompatibilityTest` in Tasks 1-8; no Flyway changes |
| §9 HTTP/Gateway/Java compatibility | Task 1 semantic fixture; per-wave catalog test; Task 8 controller discovery and full contract run |
| §10 JavaDoc/package-info | Global Constraints and documentation step in every migration task |
| §11 Structural tests | Task 1 incremental AST/reflection guard; Task 8 global dependency/old-root guard |
| §12 Eight waves/commit boundaries | Tasks 1-8, one commit per task |
| §13 Validation/Definition of Done | Per-task RED/GREEN, compile/test/diff checks; Task 8 full module verification |
| §14 Risks/rollback | Execution Invariants, path-limited staging, baseline fixture, per-wave atomic commit |
| §15 Pattern choices | Architecture preserves Facade and Ports/Adapters; no new ornamental pattern |
| §16 Planning deliverables | Appendices A/B, split method ownership, exact commands and commit boundaries |

---

## Appendix A: Existing top-level production source to final source map

The table contains all 181 non-`package-info.java` production files at the planning baseline. Paths are relative to `src/main/java/top/egon/cola/platform/rbac3/admin`.

| 当前生产源码 | 目标源码 |
|---|---|
| `Rbac3AdminApplication.java` | `Rbac3AdminApplication.java` |
| `activation/application/ActiveRoleSetRevalidator.java` | `activation/service/ActiveRoleSetRevalidator.java` |
| `activation/application/RoleActivationCandidateService.java` | `activation/service/RoleActivationCandidateService.java` |
| `activation/application/RoleActivationFacade.java` | `activation/service/RoleActivationFacade.java` |
| `activation/domain/SessionActiveRoleEntity.java` | `activation/domain/po/SessionActiveRolePO.java` |
| `activation/infrastructure/RoleActivationFactStore.java` | `activation/repository/jpa/JpaRoleActivationFactRepository.java` |
| `activation/infrastructure/SessionActiveRoleRepository.java` | `activation/repository/jpa/JpaSessionActiveRoleRepository.java` |
| `application/CommandContext.java` | `shared/domain/CommandContext.java` |
| `application/port/AuditPort.java` | `audit/repository/AuditPort.java` |
| `application/port/AuthorizationEventPort.java` | `runtime/repository/AuthorizationEventPublisher.java` |
| `application/port/DatabaseClock.java` | `shared/repository/DatabaseClock.java` |
| `application/port/Rbac3RuntimePolicy.java` | `runtime/repository/Rbac3RuntimePolicy.java` |
| `application/port/RuntimeProjectionPort.java` | `runtime/repository/RuntimeProjectionPort.java` |
| `assignment/application/AssignmentFacade.java` | `assignment/service/AssignmentFacade.java` |
| `assignment/domain/AutoAssignmentRuleEntity.java` | `assignment/domain/po/AutoAssignmentRulePO.java` |
| `assignment/domain/UserRoleAssignmentEntity.java` | `assignment/domain/po/UserRoleAssignmentPO.java` |
| `assignment/infrastructure/AssignmentRepository.java` | `assignment/repository/jpa/JpaAssignmentRepository.java` |
| `assignment/infrastructure/PostgresqlAssignmentLockStore.java` | `assignment/repository/jdbc/PostgresqlAssignmentLockRepository.java` |
| `audit/application/AuditQueryService.java` | `audit/service/AuditQueryService.java` |
| `audit/domain/AuditLogEntity.java` | `audit/domain/po/AuditLogPO.java` |
| `audit/infrastructure/AuditCursorCodec.java` | `audit/repository/internal/AuditCursorCodec.java` |
| `audit/infrastructure/PostgresqlAuditStore.java` | `audit/repository/jdbc/PostgresqlAuditRepository.java` |
| `auth/application/AuthenticationFacade.java` | `auth/service/AuthenticationFacade.java` |
| `auth/application/IdentityAuthenticatorStrategy.java` | `auth/service/IdentityAuthenticatorStrategy.java` |
| `auth/application/JwtKeyRingService.java` | `auth/service/JwtKeyRingService.java` |
| `auth/application/JwtTokenService.java` | `auth/service/JwtTokenService.java` |
| `auth/application/PasswordIdentityAuthenticator.java` | `auth/service/PasswordIdentityAuthenticator.java` |
| `auth/application/RefreshFacade.java` | `auth/service/RefreshFacade.java` |
| `auth/application/StepUpFacade.java` | `auth/service/StepUpFacade.java` |
| `auth/domain/ServiceCredentialEntity.java` | `auth/domain/po/ServiceCredentialPO.java` |
| `auth/domain/ServicePermissionEntity.java` | `auth/domain/po/ServicePermissionPO.java` |
| `auth/domain/ServicePrincipalEntity.java` | `auth/domain/po/ServicePrincipalPO.java` |
| `authorization/application/AuthorizationDecisionService.java` | `authorization/service/AuthorizationDecisionService.java` |
| `authorization/infrastructure/AuthorizationRuleRepository.java` | `authorization/repository/jpa/JpaAuthorizationRuleRepository.java` |
| `bootstrap/application/BootstrapQueryService.java` | `bootstrap/service/BootstrapQueryService.java` |
| `bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java` | `bootstrap/service/Rbac3DevelopmentAuthorizationContextInitializer.java` |
| `bootstrap/application/Rbac3DevelopmentBootstrap.java` | `bootstrap/service/Rbac3DevelopmentBootstrap.java` |
| `bootstrap/application/Rbac3DevelopmentTopology.java` | `bootstrap/service/Rbac3DevelopmentTopology.java` |
| `bootstrap/cli/Rbac3PlatformAdminBootstrapCli.java` | `bootstrap/controller/cli/Rbac3PlatformAdminBootstrapCli.java` |
| `bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java` | `bootstrap/repository/jpa/JpaDevelopmentTopologyBootstrapRepository.java` |
| `bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java` | `bootstrap/repository/jpa/JpaPlatformAdminBootstrapRepository.java` |
| `config/Rbac3AdminProperties.java` | `config/properties/Rbac3AdminProperties.java` |
| `config/Rbac3SecurityProperties.java` | `config/properties/Rbac3SecurityProperties.java` |
| `constraint/application/ConstraintFacade.java` | `constraint/service/ConstraintFacade.java` |
| `constraint/domain/DataRuleEntity.java` | `constraint/domain/po/DataRulePO.java` |
| `constraint/domain/DataRuleReferenceEntity.java` | `constraint/domain/po/DataRuleReferencePO.java` |
| `constraint/domain/FieldRuleEntity.java` | `constraint/domain/po/FieldRulePO.java` |
| `constraint/domain/OperationSodRuleEntity.java` | `constraint/domain/po/OperationSodRulePO.java` |
| `constraint/domain/RoleCardinalityEntity.java` | `constraint/domain/po/RoleCardinalityPO.java` |
| `constraint/domain/RolePrerequisiteEntity.java` | `constraint/domain/po/RolePrerequisitePO.java` |
| `constraint/domain/SodMemberEntity.java` | `constraint/domain/po/SodMemberPO.java` |
| `constraint/domain/SodSetEntity.java` | `constraint/domain/po/SodSetPO.java` |
| `constraint/infrastructure/ConstraintRepository.java` | `constraint/repository/jpa/JpaConstraintRepository.java` |
| `directory/application/DirectorySnapshotProcessor.java` | `directory/service/DirectorySnapshotProcessor.java` |
| `directory/domain/DirectorySnapshotEntity.java` | `directory/domain/po/DirectorySnapshotPO.java` |
| `directory/domain/OrgUnitEntity.java` | `directory/domain/po/OrgUnitPO.java` |
| `directory/domain/PositionEntity.java` | `directory/domain/po/PositionPO.java` |
| `directory/domain/UserPositionSnapshotEntity.java` | `directory/domain/po/UserPositionSnapshotPO.java` |
| `directory/infrastructure/DirectorySnapshotMaterializer.java` | `directory/repository/jpa/DirectorySnapshotMaterializer.java` |
| `directory/infrastructure/DirectorySnapshotStore.java` | `directory/repository/jpa/JpaDirectorySnapshotRepository.java` |
| `identity/application/IdentityMappingFacade.java` | `identity/service/IdentityMappingFacade.java` |
| `identity/domain/ExternalIdentityEntity.java` | `identity/domain/po/ExternalIdentityPO.java` |
| `identity/domain/TenantEntity.java` | `tenant/domain/po/TenantPO.java` |
| `identity/domain/UserCredentialEntity.java` | `identity/domain/po/UserCredentialPO.java` |
| `identity/domain/UserEntity.java` | `identity/domain/po/UserPO.java` |
| `identity/infrastructure/IdentityRepositories.java` | `SPLIT: identity/repository/jpa/JpaIdentityMappingRepository.java; identity/repository/jpa/JpaPasswordCredentialRepository.java; tenant/repository/jpa/JpaTenantLookupRepository.java` |
| `infrastructure/persistence/JpaDatabaseClock.java` | `shared/repository/jpa/JpaDatabaseClock.java` |
| `infrastructure/persistence/TenantScopedEntity.java` | `shared/domain/po/TenantScopedPO.java` |
| `integration/ddc/AtomicRbac3RuntimePolicy.java` | `runtime/repository/ddc/AtomicRbac3RuntimePolicy.java` |
| `integration/ddc/DdcConfigClientStatusService.java` | `runtime/repository/ddc/DdcConfigClientStatusRepository.java` |
| `integration/ddc/DdcProviderLeaseStatusService.java` | `runtime/repository/ddc/DdcProviderLeaseStatusRepository.java` |
| `integration/ddc/Rbac3DdcPolicyApplier.java` | `runtime/repository/ddc/Rbac3DdcPolicyApplier.java` |
| `integration/ddc/Rbac3DdcPolicyConfiguration.java` | `config/ddc/Rbac3DdcPolicyConfiguration.java` |
| `integration/ddc/Rbac3DdcValueDeclarations.java` | `config/ddc/Rbac3DdcValueDeclarations.java` |
| `integration/ddc/Rbac3IntegrationMetrics.java` | `config/ddc/Rbac3IntegrationMetrics.java` |
| `integration/flyway/Rbac3FlywayConfiguration.java` | `config/flyway/Rbac3FlywayConfiguration.java` |
| `integration/gateway/GatewayAdminControlPlaneStatusClient.java` | `runtime/repository/http/GatewayAdminControlPlaneStatusClient.java` |
| `integration/gateway/GatewayAdminStatusCredentialProvider.java` | `runtime/repository/http/GatewayAdminStatusCredentialProvider.java` |
| `integration/gateway/GatewayDefinitionStatusService.java` | `runtime/repository/http/GatewayDefinitionStatusRepository.java` |
| `integration/outbox/Rbac3RuntimeProjectionDeliveryHandler.java` | `runtime/controller/message/Rbac3RuntimeProjectionDeliveryHandler.java` |
| `integration/outbox/TransactionalOutboxAuthorizationEventAdapter.java` | `runtime/repository/outbox/TransactionalOutboxAuthorizationEventPublisher.java` |
| `integration/runtime/GatewayDdcRuntimeStatusService.java` | `runtime/service/GatewayDdcRuntimeStatusService.java` |
| `integration/runtime/Rbac3ApplicationConfiguration.java` | `config/runtime/Rbac3ApplicationConfiguration.java` |
| `integration/runtime/Rbac3AuthorizationFenceStore.java` | `runtime/repository/jpa/JpaAuthorizationFenceRepository.java` |
| `integration/runtime/Rbac3GatewayStatusProperties.java` | `config/properties/Rbac3GatewayStatusProperties.java` |
| `integration/runtime/Rbac3HttpProviderPublicationGate.java` | `runtime/service/Rbac3HttpProviderPublicationGate.java` |
| `integration/runtime/Rbac3IdentitySessionQueryStore.java` | `SPLIT: auth/repository/jpa/JpaLoginStateRepository.java; auth/repository/jpa/JpaRefreshStateRepository.java; bootstrap/repository/jpa/JpaBootstrapSnapshotRepository.java; session/repository/jpa/JpaSessionManagementRepository.java; assignment/repository/jpa/JpaAssignmentSessionStrengthRepository.java; auth/repository/jpa/JpaStepUpIdentityRepository.java; auth/repository/jpa/JpaStepUpSessionStrengthRepository.java; directory/repository/jpa/JpaDirectoryCommandRepository.java; directory/repository/jpa/JpaDirectoryQueryRepository.java` |
| `integration/runtime/Rbac3OperationalRuntimeStatusService.java` | `runtime/service/Rbac3OperationalRuntimeStatusService.java` |
| `integration/runtime/Rbac3PlatformIntegrationConfiguration.java` | `config/runtime/Rbac3PlatformIntegrationConfiguration.java` |
| `integration/runtime/Rbac3ReadinessIndicator.java` | `runtime/controller/Rbac3ReadinessIndicator.java` |
| `interfaces/http/ApiEnvelope.java` | `shared/domain/vo/ApiEnvelopeVO.java` |
| `interfaces/http/ApplicationResourceController.java` | `resource/controller/ApplicationResourceController.java` |
| `interfaces/http/AssignmentController.java` | `assignment/controller/AssignmentController.java` |
| `interfaces/http/AuditSimulationController.java` | `SPLIT: audit/controller/AuditController.java; simulation/controller/AuthorizationSimulationController.java` |
| `interfaces/http/AuthController.java` | `auth/controller/AuthController.java` |
| `interfaces/http/ConstraintController.java` | `constraint/controller/ConstraintController.java` |
| `interfaces/http/InternalAuthorizationController.java` | `authorization/controller/InternalAuthorizationController.java` |
| `interfaces/http/InternalIdentityController.java` | `identity/controller/InternalIdentityController.java` |
| `interfaces/http/ManagementPolicyController.java` | `management/controller/ManagementPolicyController.java` |
| `interfaces/http/ManifestController.java` | `resource/controller/ManifestController.java` |
| `interfaces/http/ParticipationController.java` | `participation/controller/ParticipationController.java` |
| `interfaces/http/Rbac3ApiExceptionHandler.java` | `shared/controller/Rbac3ApiExceptionHandler.java` |
| `interfaces/http/Rbac3AuthBootstrapController.java` | `bootstrap/controller/Rbac3AuthBootstrapController.java` |
| `interfaces/http/ResourceAccessDecisionRequest.java` | `authorization/domain/dto/ResourceAccessDecisionRequestDTO.java` |
| `interfaces/http/ResourceAccessDecisionResponse.java` | `authorization/domain/vo/ResourceAccessDecisionResponseVO.java` |
| `interfaces/http/RoleActivationController.java` | `activation/controller/RoleActivationController.java` |
| `interfaces/http/RolePermissionController.java` | `role/controller/RolePermissionController.java` |
| `interfaces/http/RuntimeController.java` | `runtime/controller/RuntimeController.java` |
| `interfaces/http/SessionController.java` | `session/controller/SessionController.java` |
| `interfaces/http/TenantUserDirectoryController.java` | `SPLIT: tenant/controller/TenantController.java; identity/controller/UserDirectoryController.java; directory/controller/DirectoryController.java` |
| `management/application/ManagementPolicyFacade.java` | `management/service/ManagementPolicyFacade.java` |
| `management/domain/ManagementOperationEntity.java` | `management/domain/po/ManagementOperationPO.java` |
| `management/domain/ManagementPolicyEntity.java` | `management/domain/po/ManagementPolicyPO.java` |
| `management/domain/ManagementRoleEntity.java` | `management/domain/po/ManagementRolePO.java` |
| `management/domain/ManagementScopeEntity.java` | `management/domain/po/ManagementScopePO.java` |
| `management/domain/ManagementSubjectEntity.java` | `management/domain/po/ManagementSubjectPO.java` |
| `management/infrastructure/ManagementPolicyRepository.java` | `management/repository/jpa/JpaManagementPolicyRepository.java` |
| `participation/application/ParticipationFacade.java` | `participation/service/ParticipationFacade.java` |
| `participation/domain/BusinessParticipationEntity.java` | `participation/domain/po/BusinessParticipationPO.java` |
| `participation/infrastructure/PostgresqlParticipationStore.java` | `participation/repository/jdbc/PostgresqlParticipationRepository.java` |
| `resource/application/ApplicationResourceFacade.java` | `resource/service/ApplicationResourceFacade.java` |
| `resource/application/ManifestFacade.java` | `resource/service/ManifestFacade.java` |
| `resource/domain/ApplicationEntity.java` | `resource/domain/po/ApplicationPO.java` |
| `resource/domain/FieldDefinitionEntity.java` | `resource/domain/po/FieldDefinitionPO.java` |
| `resource/domain/PermissionEntity.java` | `resource/domain/po/PermissionPO.java` |
| `resource/domain/PermissionResourceEntity.java` | `resource/domain/po/PermissionResourcePO.java` |
| `resource/domain/ResourceEntity.java` | `resource/domain/po/ResourcePO.java` |
| `resource/domain/ResourceManifestEntity.java` | `resource/domain/po/ResourceManifestPO.java` |
| `resource/infrastructure/ResourceManifestRepository.java` | `resource/repository/jpa/JpaResourceManifestRepository.java` |
| `role/application/RoleFacade.java` | `role/service/RoleFacade.java` |
| `role/domain/RoleClosureEntity.java` | `role/domain/po/RoleClosurePO.java` |
| `role/domain/RoleEntity.java` | `role/domain/po/RolePO.java` |
| `role/domain/RoleInheritanceEntity.java` | `role/domain/po/RoleInheritancePO.java` |
| `role/domain/RolePermissionEntity.java` | `role/domain/po/RolePermissionPO.java` |
| `role/infrastructure/PostgresqlRoleClosureStore.java` | `role/repository/jdbc/PostgresqlRoleClosureRepository.java` |
| `role/infrastructure/RoleRepository.java` | `role/repository/jpa/JpaRoleRepository.java` |
| `runtime/application/AuthorizationFenceService.java` | `runtime/service/AuthorizationFenceService.java` |
| `runtime/application/AuthorizationMutationCoordinator.java` | `runtime/service/AuthorizationMutationCoordinator.java` |
| `runtime/application/ControlPlaneRuntimeStatusPort.java` | `runtime/service/ControlPlaneRuntimeStatusPort.java` |
| `runtime/application/IdempotencyService.java` | `runtime/service/IdempotencyService.java` |
| `runtime/application/RuntimeQueryService.java` | `runtime/service/RuntimeQueryService.java` |
| `runtime/domain/AuthorizationMutationEntity.java` | `runtime/domain/po/AuthorizationMutationPO.java` |
| `runtime/domain/IdempotencyRecordEntity.java` | `runtime/domain/po/IdempotencyRecordPO.java` |
| `runtime/infrastructure/AuthorizationMutationRepository.java` | `runtime/repository/jpa/JpaAuthorizationMutationRepository.java` |
| `runtime/infrastructure/IdempotencyRepository.java` | `runtime/repository/jpa/JpaIdempotencyRepository.java` |
| `security/CurrentRbac3Principal.java` | `config/security/CurrentRbac3Principal.java` |
| `security/Rbac3AdminAuthenticationToken.java` | `config/security/Rbac3AdminAuthenticationToken.java` |
| `security/Rbac3AdminPrincipalFilter.java` | `config/security/Rbac3AdminPrincipalFilter.java` |
| `security/Rbac3AdminSecurityConfiguration.java` | `config/security/Rbac3AdminSecurityConfiguration.java` |
| `security/Rbac3JwtAuthenticationConverter.java` | `config/security/Rbac3JwtAuthenticationConverter.java` |
| `security/Rbac3JwtConfiguration.java` | `config/security/Rbac3JwtConfiguration.java` |
| `security/Rbac3MethodAuthorization.java` | `config/security/Rbac3MethodAuthorization.java` |
| `security/RequiresRbac3Permission.java` | `config/security/RequiresRbac3Permission.java` |
| `session/application/AuthorizationContextFacade.java` | `session/service/AuthorizationContextFacade.java` |
| `session/application/RefreshTokenService.java` | `session/service/RefreshTokenService.java` |
| `session/application/SessionFacade.java` | `session/service/SessionFacade.java` |
| `session/application/SessionRuntimeSynchronizer.java` | `session/service/SessionRuntimeSynchronizer.java` |
| `session/application/SessionSecurityEventRecorder.java` | `session/service/SessionSecurityEventRecorder.java` |
| `session/domain/RefreshTokenEntity.java` | `session/domain/po/RefreshTokenPO.java` |
| `session/domain/SessionEntity.java` | `session/domain/po/SessionPO.java` |
| `session/infrastructure/AuthorizationContextRepository.java` | `session/repository/jpa/JpaAuthorizationContextRepository.java` |
| `session/infrastructure/JpaSessionStore.java` | `session/repository/jpa/JpaSessionRepository.java` |
| `session/infrastructure/RefreshTokenRepository.java` | `session/repository/jpa/JpaRefreshTokenRepository.java` |
| `session/infrastructure/SessionRepository.java` | `session/repository/jpa/JpaSessionEntityRepository.java` |
| `simulation/application/AuthorizationSimulationService.java` | `simulation/service/AuthorizationSimulationService.java` |
| `simulation/infrastructure/PostgresqlRoleImpactSource.java` | `simulation/repository/jdbc/PostgresqlRoleImpactRepository.java` |
| `snapshot/application/LoginRuntimeProjectionFactory.java` | `runtime/service/LoginRuntimeProjectionFactory.java` |
| `snapshot/application/SessionSnapshotProjector.java` | `runtime/service/SessionSnapshotProjector.java` |
| `snapshot/application/SystemAuthorizationSnapshotService.java` | `runtime/service/SystemAuthorizationSnapshotService.java` |
| `snapshot/infrastructure/Rbac3RuntimeRedissonConfiguration.java` | `config/redis/Rbac3RuntimeRedissonConfiguration.java` |
| `snapshot/infrastructure/RedisAuthorizationRuntimeStore.java` | `runtime/repository/redis/RedisAuthorizationRuntimeRepository.java` |
| `tenant/TenantContext.java` | `tenant/domain/TenantContext.java` |
| `tenant/TenantContextFilter.java` | `tenant/controller/filter/TenantContextFilter.java` |
| `tenant/TenantContextResolver.java` | `tenant/service/TenantContextResolver.java` |
| `worker/AssignmentLifecycleWorker.java` | `runtime/controller/scheduled/AssignmentLifecycleWorker.java` |
| `worker/AuthorizationMutationRecoveryWorker.java` | `runtime/controller/scheduled/AuthorizationMutationRecoveryWorker.java` |
| `worker/PostgresqlAssignmentLifecycleStore.java` | `assignment/repository/jdbc/PostgresqlAssignmentLifecycleRepository.java` |
| `worker/Rbac3RuntimeProjectionRecovery.java` | `runtime/service/Rbac3RuntimeProjectionRecovery.java` |
| `worker/Rbac3WorkerConfiguration.java` | `config/runtime/Rbac3WorkerConfiguration.java` |
| `worker/RedisProjectionCheckpointStore.java` | `runtime/repository/redis/RedisProjectionCheckpointRepository.java` |
| `worker/RuntimeSnapshotRebuildWorker.java` | `runtime/controller/scheduled/RuntimeSnapshotRebuildWorker.java` |

## Appendix B: Nested type migration manifest

The table contains all 386 actual member types at the planning baseline. It excludes the local variable named `record` in `AuthorizationDecisionService`. Consumer paths are relative to repository root and include tests found by explicit `Host.NestedType` search.
| 波次 | 原嵌套类型 | 形式/可见性 | 消费者核对 | 目标 FQCN | 迁移约束 |
|---:|---|---|---|---|---|
| 1 | `BootstrapQueryService.BootstrapSnapshotSource` (`bootstrap/application/BootstrapQueryService.java:64`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.bootstrap.repository.BootstrapSnapshotRepository` | 保留可见性/注解/构造校验 |
| 1 | `Rbac3DevelopmentAuthorizationContextInitializer.CandidateSource` (`bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java:174`) | `package interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.bootstrap.service.internal.CandidateRepository` | 包级顶层；不得扩大 public API |
| 1 | `Rbac3DevelopmentAuthorizationContextInitializer.RoleActivator` (`bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java:202`) | `package interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.bootstrap.service.internal.RoleActivator` | 包级顶层；不得扩大 public API |
| 1 | `Rbac3DevelopmentBootstrap.BootstrapPort` (`bootstrap/application/Rbac3DevelopmentBootstrap.java:130`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java` | `top.egon.cola.platform.rbac3.admin.bootstrap.repository.DevelopmentBootstrapPort` | 保留可见性/注解/构造校验 |
| 1 | `Rbac3DevelopmentTopology.ApplicationDefinition` (`bootstrap/application/Rbac3DevelopmentTopology.java:167`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentTopologyTest.java` | `top.egon.cola.platform.rbac3.admin.bootstrap.domain.vo.ApplicationDefinitionVO` | 保留可见性/注解/构造校验 |
| 1 | `Rbac3PlatformAdminBootstrapCli.BootstrapPort` (`bootstrap/cli/Rbac3PlatformAdminBootstrapCli.java:183`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/Rbac3PlatformAdminBootstrapCliIT.java` | `top.egon.cola.platform.rbac3.admin.bootstrap.service.PlatformAdminBootstrapService` | 保留可见性/注解/构造校验 |
| 1 | `ApiEnvelope.Meta` (`interfaces/http/ApiEnvelope.java:58`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeMetaVO` | 保留可见性/注解/构造校验 |
| 1 | `Rbac3JwtConfiguration.Rbac3RsaKeyMaterial` (`security/Rbac3JwtConfiguration.java:166`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.config.security.Rbac3RsaKeyMaterial` | 保留可见性/注解/构造校验 |
| 2 | `DirectorySnapshotProcessor.SnapshotModel` (`directory/application/DirectorySnapshotProcessor.java:469`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.SnapshotModelVO` | 保留可见性/注解/构造校验 |
| 2 | `DirectorySnapshotProcessor.ResolvedUnit` (`directory/application/DirectorySnapshotProcessor.java:541`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/directory/DirectorySnapshotProcessorTest.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.ResolvedUnitVO` | 保留可见性/注解/构造校验 |
| 2 | `DirectorySnapshotProcessor.PositionInput` (`directory/application/DirectorySnapshotProcessor.java:639`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.dto.PositionInputDTO` | 保留可见性/注解/构造校验 |
| 2 | `DirectorySnapshotProcessor.UserPositionInput` (`directory/application/DirectorySnapshotProcessor.java:713`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.dto.UserPositionInputDTO` | 保留可见性/注解/构造校验 |
| 2 | `DirectorySnapshotProcessor.UnitInput` (`directory/application/DirectorySnapshotProcessor.java:788`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.directory.domain.dto.UnitInputDTO` | 包级顶层；不得扩大 public API |
| 2 | `DirectorySnapshotEntity.Status` (`directory/domain/DirectorySnapshotEntity.java:383`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `OrgUnitEntity.UnitType` (`directory/domain/OrgUnitEntity.java:515`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.enums.OrgUnitUnitTypeEnum` | 保留枚举值/持久化方式 |
| 2 | `OrgUnitEntity.Status` (`directory/domain/OrgUnitEntity.java:541`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.enums.OrgUnitStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `PositionEntity.Status` (`directory/domain/PositionEntity.java:406`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.enums.PositionStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `UserPositionSnapshotEntity.Status` (`directory/domain/UserPositionSnapshotEntity.java:352`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/infrastructure/DirectorySnapshotMaterializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.enums.UserPositionSnapshotStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `DirectorySnapshotMaterializer.MaterializationResult` (`directory/infrastructure/DirectorySnapshotMaterializer.java:551`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.MaterializationResultVO` | 保留可见性/注解/构造校验 |
| 2 | `DirectorySnapshotMaterializer.Counter` (`directory/infrastructure/DirectorySnapshotMaterializer.java:628`) | `private class` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.directory.repository.internal.Counter` | 包级顶层；不得扩大 public API |
| 2 | `DirectorySnapshotMaterializer.UserPositionKey` (`directory/infrastructure/DirectorySnapshotMaterializer.java:688`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.directory.domain.UserPositionKey` | 包级顶层；不得扩大 public API |
| 2 | `DirectorySnapshotMaterializer.AssignmentSignature` (`directory/infrastructure/DirectorySnapshotMaterializer.java:718`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.directory.repository.internal.AssignmentSignature` | 包级顶层；不得扩大 public API |
| 2 | `DirectorySnapshotStore.IngestionResult` (`directory/infrastructure/DirectorySnapshotStore.java:98`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.IngestionResultVO` | 保留可见性/注解/构造校验 |
| 2 | `DirectorySnapshotStore.Outcome` (`directory/infrastructure/DirectorySnapshotStore.java:120`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.enums.DirectorySnapshotOutcomeEnum` | 保留可见性/注解/构造校验 |
| 2 | `IdentityMappingFacade.MappingStore` (`identity/application/IdentityMappingFacade.java:148`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.identity.repository.IdentityMappingRepository` | 保留可见性/注解/构造校验 |
| 2 | `IdentityMappingFacade.MappingIdGenerator` (`identity/application/IdentityMappingFacade.java:220`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.identity.service.internal.MappingIdGenerator` | 保留可见性/注解/构造校验 |
| 2 | `IdentityMappingFacade.Mapping` (`identity/application/IdentityMappingFacade.java:248`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.vo.MappingVO` | 保留可见性/注解/构造校验 |
| 2 | `IdentityMappingFacade.ResolvedMembership` (`identity/application/IdentityMappingFacade.java:317`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacadeTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityControllerTest.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.vo.ResolvedMembershipVO` | 保留可见性/注解/构造校验 |
| 2 | `IdentityMappingFacade.TenantMembership` (`identity/application/IdentityMappingFacade.java:406`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacadeTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityControllerTest.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.vo.TenantMembershipVO` | 保留可见性/注解/构造校验 |
| 2 | `IdentityMappingFacade.DuplicateIdentityMappingException` (`identity/application/IdentityMappingFacade.java:457`) | `public class` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.exception.DuplicateIdentityMappingException` | 保留可见性/注解/构造校验 |
| 2 | `ExternalIdentityEntity.Status` (`identity/domain/ExternalIdentityEntity.java:266`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.enums.ExternalIdentityStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `TenantEntity.Status` (`identity/domain/TenantEntity.java:392`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/RefreshTokenRepository.java` | `top.egon.cola.platform.rbac3.admin.tenant.domain.enums.TenantStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `UserCredentialEntity.CredentialType` (`identity/domain/UserCredentialEntity.java:298`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserCredentialTypeEnum` | 保留枚举值/持久化方式 |
| 2 | `UserCredentialEntity.Status` (`identity/domain/UserCredentialEntity.java:316`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserCredentialStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `UserEntity.Status` (`identity/domain/UserEntity.java:441`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserStatusEnum` | 保留枚举值/持久化方式 |
| 2 | `IdentityRepositories.MembershipRow` (`identity/infrastructure/IdentityRepositories.java:414`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.identity.repository.internal.MembershipRow` | 包级顶层；不得扩大 public API |
| 2 | `IdentityRepositories.CredentialRow` (`identity/infrastructure/IdentityRepositories.java:453`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.identity.repository.internal.CredentialRow` | 包级顶层；不得扩大 public API |
| 2 | `InternalIdentityController.ResolveRequest` (`interfaces/http/InternalIdentityController.java:165`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityControllerTest.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.dto.IdentityResolveRequestDTO` | 保留可见性/注解/构造校验 |
| 2 | `InternalIdentityController.BindRequest` (`interfaces/http/InternalIdentityController.java:205`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityControllerTest.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.dto.IdentityBindRequestDTO` | 保留可见性/注解/构造校验 |
| 2 | `InternalIdentityController.ResolvedMembershipResponse` (`interfaces/http/InternalIdentityController.java:257`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.identity.domain.vo.ResolvedMembershipResponseVO` | 保留可见性/注解/构造校验 |
| 2 | `InternalIdentityController.TenantMembershipResponse` (`interfaces/http/InternalIdentityController.java:360`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.identity.domain.vo.TenantMembershipResponseVO` | 保留可见性/注解/构造校验 |
| 2 | `InternalIdentityController.IdentityMembershipNotFoundException` (`interfaces/http/InternalIdentityController.java:432`) | `public class` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.identity.domain.exception.IdentityMembershipNotFoundException` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.DirectoryCommandPort` (`interfaces/http/TenantUserDirectoryController.java:375`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDocumentCatalogContractTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/Rbac3GatewayDefinitionDiscoveryTest.java` | `top.egon.cola.platform.rbac3.admin.directory.service.DirectoryCommandService` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.DirectoryQueryPort` (`interfaces/http/TenantUserDirectoryController.java:442`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDocumentCatalogContractTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/Rbac3GatewayDefinitionDiscoveryTest.java` | `top.egon.cola.platform.rbac3.admin.directory.service.DirectoryQueryService` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.CreateTenantCommand` (`interfaces/http/TenantUserDirectoryController.java:560`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.tenant.domain.dto.CreateTenantCommandDTO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.TenantStatusCommand` (`interfaces/http/TenantUserDirectoryController.java:600`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.tenant.domain.dto.TenantStatusCommandDTO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.UserStatusCommand` (`interfaces/http/TenantUserDirectoryController.java:639`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.dto.UserStatusCommandDTO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.DirectorySnapshotCommand` (`interfaces/http/TenantUserDirectoryController.java:680`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.dto.DirectorySnapshotCommandDTO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.DirectorySyncView` (`interfaces/http/TenantUserDirectoryController.java:737`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySyncVO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.UserDirectoryView` (`interfaces/http/TenantUserDirectoryController.java:789`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.identity.domain.vo.UserDirectoryVO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.TenantView` (`interfaces/http/TenantUserDirectoryController.java:871`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.tenant.domain.vo.TenantVO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.OrgUnitView` (`interfaces/http/TenantUserDirectoryController.java:941`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.OrgUnitVO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.PositionView` (`interfaces/http/TenantUserDirectoryController.java:1031`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.PositionVO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.DirectorySnapshotView` (`interfaces/http/TenantUserDirectoryController.java:1100`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySnapshotVO` | 保留可见性/注解/构造校验 |
| 2 | `TenantUserDirectoryController.PageView` (`interfaces/http/TenantUserDirectoryController.java:1190`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectoryPageVO` | 保留可见性/注解/构造校验 |
| 2 | `TenantContextResolver.TenantContextResolutionException` (`tenant/TenantContextResolver.java:126`) | `public class` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/tenant/TenantContextFilter.java` | `top.egon.cola.platform.rbac3.admin.tenant.domain.exception.TenantContextResolutionException` | 保留可见性/注解/构造校验 |
| 3 | `AuthenticationFacade.LoginStateSource` (`auth/application/AuthenticationFacade.java:199`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.repository.LoginStateRepository` | 保留可见性/注解/构造校验 |
| 3 | `AuthenticationFacade.LoginRuntimePublisher` (`auth/application/AuthenticationFacade.java:224`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.repository.LoginRuntimePublisher` | 保留可见性/注解/构造校验 |
| 3 | `AuthenticationFacade.LoginAuditRecorder` (`auth/application/AuthenticationFacade.java:247`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.repository.LoginAuditRecorder` | 保留可见性/注解/构造校验 |
| 3 | `AuthenticationFacade.LoginAudit` (`auth/application/AuthenticationFacade.java:276`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.LoginAuditVO` | 保留可见性/注解/构造校验 |
| 3 | `AuthenticationFacade.LoginState` (`auth/application/AuthenticationFacade.java:348`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.LoginStateVO` | 保留可见性/注解/构造校验 |
| 3 | `IdentityAuthenticatorStrategy.AuthenticatedIdentity` (`auth/application/IdentityAuthenticatorStrategy.java:39`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/AuthenticationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/StepUpFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/StepUpFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.AuthenticatedIdentityVO` | 接口成员隐式 public；独立后显式 public |
| 3 | `JwtKeyRingService.KeyDescriptor` (`auth/application/JwtKeyRingService.java:267`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/JwtTokenService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/security/Rbac3JwtConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/JwtKeyRingServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/JwtTokenServiceTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.KeyDescriptorVO` | 保留可见性/注解/构造校验 |
| 3 | `JwtKeyRingService.KeyState` (`auth/application/JwtKeyRingService.java:378`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/security/Rbac3JwtConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/JwtKeyRingServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/JwtTokenServiceTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.enums.JwtKeyRingKeyStateEnum` | 保留可见性/注解/构造校验 |
| 3 | `JwtTokenService.AccessTokenSubject` (`auth/application/JwtTokenService.java:171`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/AuthenticationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/RefreshFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/JwtTokenServiceTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.AccessTokenSubjectVO` | 保留可见性/注解/构造校验 |
| 3 | `JwtTokenService.IssuedAccessToken` (`auth/application/JwtTokenService.java:234`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/AuthenticationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/RefreshFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/RefreshRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.IssuedAccessTokenVO` | 保留可见性/注解/构造校验 |
| 3 | `PasswordIdentityAuthenticator.CredentialStore` (`auth/application/PasswordIdentityAuthenticator.java:211`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.auth.repository.CredentialRepository` | 保留可见性/注解/构造校验 |
| 3 | `PasswordIdentityAuthenticator.PasswordCredential` (`auth/application/PasswordIdentityAuthenticator.java:275`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.PasswordCredentialVO` | 保留可见性/注解/构造校验 |
| 3 | `PasswordIdentityAuthenticator.AuthenticationFailed` (`auth/application/PasswordIdentityAuthenticator.java:428`) | `public class` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/Rbac3ApiExceptionHandler.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.exception.AuthenticationFailedException` | 保留可见性/注解/构造校验 |
| 3 | `RefreshFacade.RefreshStateSource` (`auth/application/RefreshFacade.java:222`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.auth.repository.RefreshStateRepository` | 保留可见性/注解/构造校验 |
| 3 | `RefreshFacade.TransactionBoundary` (`auth/application/RefreshFacade.java:245`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.service.internal.TransactionBoundary` | 保留可见性/注解/构造校验 |
| 3 | `RefreshFacade.RefreshRuntimePublisher` (`auth/application/RefreshFacade.java:268`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.repository.RefreshRuntimePublisher` | 保留可见性/注解/构造校验 |
| 3 | `RefreshFacade.RefreshAuditRecorder` (`auth/application/RefreshFacade.java:291`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.repository.RefreshAuditRecorder` | 保留可见性/注解/构造校验 |
| 3 | `RefreshFacade.RefreshAudit` (`auth/application/RefreshFacade.java:319`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/RefreshRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshAuditVO` | 保留可见性/注解/构造校验 |
| 3 | `RefreshFacade.RefreshAttempt` (`auth/application/RefreshFacade.java:381`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshAttemptVO` | 保留可见性/注解/构造校验 |
| 3 | `RefreshFacade.RefreshState` (`auth/application/RefreshFacade.java:441`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/RefreshRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshStateVO` | 保留可见性/注解/构造校验 |
| 3 | `StepUpFacade.IdentitySource` (`auth/application/StepUpFacade.java:123`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.auth.repository.IdentityRepository` | 保留可见性/注解/构造校验 |
| 3 | `StepUpFacade.SessionStrengthStore` (`auth/application/StepUpFacade.java:147`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.auth.repository.SessionStrengthRepository` | 保留可见性/注解/构造校验 |
| 3 | `StepUpFacade.Identity` (`auth/application/StepUpFacade.java:176`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/StepUpFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpIdentityVO` | 保留可见性/注解/构造校验 |
| 3 | `StepUpFacade.StepUpResult` (`auth/application/StepUpFacade.java:217`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/StepUpFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpResultVO` | 保留可见性/注解/构造校验 |
| 3 | `ServiceCredentialEntity.CredentialType` (`auth/domain/ServiceCredentialEntity.java:216`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.domain.enums.ServiceCredentialTypeEnum` | 保留枚举值/持久化方式 |
| 3 | `ServiceCredentialEntity.Status` (`auth/domain/ServiceCredentialEntity.java:242`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.domain.enums.ServiceCredentialStatusEnum` | 保留枚举值/持久化方式 |
| 3 | `ServicePrincipalEntity.Status` (`auth/domain/ServicePrincipalEntity.java:174`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.domain.enums.ServicePrincipalStatusEnum` | 保留枚举值/持久化方式 |
| 3 | `AuthController.LogoutView` (`interfaces/http/AuthController.java:144`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.auth.domain.vo.LogoutVO` | 保留可见性/注解/构造校验 |
| 3 | `SessionController.SessionManagementPort` (`interfaces/http/SessionController.java:158`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDocumentCatalogContractTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/Rbac3GatewayDefinitionDiscoveryTest.java` | `top.egon.cola.platform.rbac3.admin.session.service.SessionManagementService` | 保留可见性/注解/构造校验 |
| 3 | `SessionController.SessionView` (`interfaces/http/SessionController.java:218`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionVO` | 保留可见性/注解/构造校验 |
| 3 | `SessionController.RevocationView` (`interfaces/http/SessionController.java:296`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.session.domain.vo.RevocationVO` | 保留可见性/注解/构造校验 |
| 3 | `SessionController.RevokeAllView` (`interfaces/http/SessionController.java:321`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.session.domain.vo.RevokeAllVO` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.MembershipResolver` (`session/application/AuthorizationContextFacade.java:193`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.session.repository.MembershipRepository` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.AuthorizationContextStore` (`session/application/AuthorizationContextFacade.java:216`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/AuthorizationContextRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/AuthorizationContextFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.repository.AuthorizationContextRepository` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.ContextIdGenerator` (`session/application/AuthorizationContextFacade.java:262`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.session.service.internal.ContextIdGenerator` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.ContextOpener` (`session/application/AuthorizationContextFacade.java:284`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotService.java` | `top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextOpener` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.ActiveMembership` (`session/application/AuthorizationContextFacade.java:321`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/AuthorizationContextRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/AuthorizationContextFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.ActiveMembershipVO` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.AuthorizationContext` (`session/application/AuthorizationContextFacade.java:385`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/AuthorizationContextRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializerTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/AuthorizationContextFacadeTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotServiceTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.AuthorizationContextVO` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.AuthorizationContextMismatchException` (`session/application/AuthorizationContextFacade.java:492`) | `public class` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/AuthorizationContextFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.exception.AuthorizationContextMismatchException` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.InactiveIdentityMembershipException` (`session/application/AuthorizationContextFacade.java:521`) | `public class` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.session.domain.exception.InactiveIdentityMembershipException` | 保留可见性/注解/构造校验 |
| 3 | `AuthorizationContextFacade.ConcurrentContextCreationException` (`session/application/AuthorizationContextFacade.java:547`) | `public class` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/AuthorizationContextFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/AuthorizationContextRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/AuthorizationContextFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.exception.ConcurrentContextCreationException` | 保留可见性/注解/构造校验 |
| 3 | `RefreshTokenService.RefreshTokenStore` (`session/application/RefreshTokenService.java:159`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/RefreshTokenRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3ConcurrencyMatrixIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/RefreshTokenConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.session.repository.RefreshTokenRepository` | 保留可见性/注解/构造校验 |
| 3 | `RefreshTokenService.TokenRecord` (`session/application/RefreshTokenService.java:216`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/SessionFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/JpaSessionStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/RefreshTokenRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3ConcurrencyMatrixIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/RefreshTokenConcurrencyIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/SessionFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.TokenRecordVO` | 保留可见性/注解/构造校验 |
| 3 | `RefreshTokenService.RotationResult` (`session/application/RefreshTokenService.java:413`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/RefreshFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/RefreshRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.RotationResultVO` | 保留可见性/注解/构造校验 |
| 3 | `RefreshTokenService.Outcome` (`session/application/RefreshTokenService.java:463`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/RefreshFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/RefreshRuntimePublicationTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3ConcurrencyMatrixIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/RefreshTokenConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenOutcomeEnum` | 保留可见性/注解/构造校验 |
| 3 | `RefreshTokenService.TokenStatus` (`session/application/RefreshTokenService.java:497`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/RefreshTokenRepository.java` | `top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenTokenStatusEnum` | 保留可见性/注解/构造校验 |
| 3 | `RefreshTokenService.FamilyStatus` (`session/application/RefreshTokenService.java:547`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3ConcurrencyMatrixIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/RefreshTokenConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenFamilyStatusEnum` | 保留可见性/注解/构造校验 |
| 3 | `SessionFacade.SessionStore` (`session/application/SessionFacade.java:196`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/JpaSessionStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/SessionFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.repository.SessionRepository` | 保留可见性/注解/构造校验 |
| 3 | `SessionFacade.SessionRecord` (`session/application/SessionFacade.java:252`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/AuthenticationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/JpaSessionStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/LoginRuntimeProjectionFactory.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/SessionFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.SessionRecordVO` | 保留可见性/注解/构造校验 |
| 3 | `SessionFacade.IssuedSession` (`session/application/SessionFacade.java:379`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/AuthenticationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.IssuedSessionVO` | 保留可见性/注解/构造校验 |
| 3 | `SessionFacade.SessionStatus` (`session/application/SessionFacade.java:429`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/auth/AuthenticationRuntimePublicationTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/SessionFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionLifecycleStatusEnum` | 保留可见性/注解/构造校验 |
| 3 | `SessionSecurityEventRecorder.Termination` (`session/application/SessionSecurityEventRecorder.java:118`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/JpaSessionStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/RefreshTokenRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/SessionSecurityEventRecorderTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.vo.TerminationVO` | 保留可见性/注解/构造校验 |
| 3 | `RefreshTokenEntity.Status` (`session/domain/RefreshTokenEntity.java:375`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/RefreshTokenRepository.java` | `top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenStatusEnum` | 保留枚举值/持久化方式 |
| 3 | `SessionEntity.Status` (`session/domain/SessionEntity.java:875`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3AuthorizationFenceStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/SessionRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java` | `top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionStatusEnum` | 保留枚举值/持久化方式 |
| 3 | `SessionEntity.AuthenticationStrength` (`session/domain/SessionEntity.java:925`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/infrastructure/JpaSessionStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/SessionActiveRoleRepositoryTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/domain/SessionStepUpTest.java` | `top.egon.cola.platform.rbac3.admin.session.domain.enums.AuthenticationStrengthEnum` | 保留枚举值/持久化方式 |
| 4 | `ApplicationResourceController.ArchiveResourceRequest` (`interfaces/http/ApplicationResourceController.java:175`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.domain.dto.ArchiveResourceRequestDTO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestController.SubmitManifestRequest` (`interfaces/http/ManifestController.java:266`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.domain.dto.SubmitManifestRequestDTO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestController.ActivateManifestRequest` (`interfaces/http/ManifestController.java:305`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.domain.dto.ActivateManifestRequestDTO` | 保留可见性/注解/构造校验 |
| 4 | `RolePermissionController.CreateRoleRequest` (`interfaces/http/RolePermissionController.java:370`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.role.domain.dto.CreateRoleRequestDTO` | 保留可见性/注解/构造校验 |
| 4 | `RolePermissionController.UpdateRoleRequest` (`interfaces/http/RolePermissionController.java:459`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.role.domain.dto.UpdateRoleRequestDTO` | 保留可见性/注解/构造校验 |
| 4 | `RolePermissionController.BindPermissionsRequest` (`interfaces/http/RolePermissionController.java:523`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.role.domain.dto.BindPermissionsRequestDTO` | 保留可见性/注解/构造校验 |
| 4 | `RolePermissionController.InheritanceRequest` (`interfaces/http/RolePermissionController.java:577`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.role.domain.dto.InheritanceRequestDTO` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationResourceFacade.Store` (`resource/application/ApplicationResourceFacade.java:156`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ApplicationResourceFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.resource.repository.ApplicationResourceRepository` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationResourceFacade.ApplicationView` (`resource/application/ApplicationResourceFacade.java:257`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ApplicationResourceController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ApplicationResourceFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.ApplicationVO` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationResourceFacade.ResourceView` (`resource/application/ApplicationResourceFacade.java:317`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ApplicationResourceController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ApplicationResourceFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.ResourceVO` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationResourceFacade.ManifestView` (`resource/application/ApplicationResourceFacade.java:405`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManifestController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ApplicationResourceFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestVO` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationResourceFacade.ManifestValidationView` (`resource/application/ApplicationResourceFacade.java:460`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManifestController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ApplicationResourceFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestValidationVO` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationResourceFacade.ManifestImpactView` (`resource/application/ApplicationResourceFacade.java:526`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManifestController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ApplicationResourceFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestImpactVO` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationResourceFacade.ArchiveResult` (`resource/application/ApplicationResourceFacade.java:606`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ApplicationResourceController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ApplicationResourceFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.ArchiveResultVO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.ResourceKind` (`resource/application/ManifestFacade.java:336`) | `private enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.ManifestResourceKindEnum` | 包级顶层；不得扩大 public API |
| 4 | `ManifestFacade.ManifestStore` (`resource/application/ManifestFacade.java:386`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ManifestFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.resource.repository.ResourceManifestRepository` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.ActivationMutation` (`resource/application/ManifestFacade.java:462`) | `package record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.service.internal.ActivationMutation` | 包级顶层；不得扩大 public API |
| 4 | `ManifestFacade.ComponentKeyRegistry` (`resource/application/ManifestFacade.java:499`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java` | `top.egon.cola.platform.rbac3.admin.resource.repository.ComponentKeyRegistry` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.SubmitCommand` (`resource/application/ManifestFacade.java:527`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManifestController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ManifestFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.dto.SubmitCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.StoredManifest` (`resource/application/ManifestFacade.java:609`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ManifestFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.StoredManifestVO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.ActivateCommand` (`resource/application/ManifestFacade.java:702`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManifestController.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.dto.ActivateCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.SubmissionResult` (`resource/application/ManifestFacade.java:819`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManifestController.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.SubmissionResultVO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.ActivationResult` (`resource/application/ManifestFacade.java:846`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManifestController.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.vo.ActivationResultVO` | 保留可见性/注解/构造校验 |
| 4 | `ManifestFacade.SubmissionOutcome` (`resource/application/ManifestFacade.java:889`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/resource/ManifestFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.ManifestSubmissionOutcomeEnum` | 保留可见性/注解/构造校验 |
| 4 | `ApplicationEntity.Status` (`resource/domain/ApplicationEntity.java:256`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.ApplicationStatusEnum` | 保留枚举值/持久化方式 |
| 4 | `FieldDefinitionEntity.DataType` (`resource/domain/FieldDefinitionEntity.java:271`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionDataTypeEnum` | 保留枚举值/持久化方式 |
| 4 | `FieldDefinitionEntity.Sensitivity` (`resource/domain/FieldDefinitionEntity.java:337`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionSensitivityEnum` | 保留枚举值/持久化方式 |
| 4 | `FieldDefinitionEntity.DefaultAccess` (`resource/domain/FieldDefinitionEntity.java:379`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionDefaultAccessEnum` | 保留枚举值/持久化方式 |
| 4 | `FieldDefinitionEntity.Status` (`resource/domain/FieldDefinitionEntity.java:413`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionStatusEnum` | 保留枚举值/持久化方式 |
| 4 | `PermissionEntity.RiskLevel` (`resource/domain/PermissionEntity.java:197`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.PermissionRiskLevelEnum` | 保留枚举值/持久化方式 |
| 4 | `PermissionEntity.Status` (`resource/domain/PermissionEntity.java:239`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.PermissionStatusEnum` | 保留枚举值/持久化方式 |
| 4 | `PermissionResourceEntity.Status` (`resource/domain/PermissionResourceEntity.java:207`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.PermissionResourceStatusEnum` | 保留枚举值/持久化方式 |
| 4 | `ResourceEntity.ResourceType` (`resource/domain/ResourceEntity.java:380`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/domain/PermissionResourceEntity.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.ResourceTypeEnum` | 保留枚举值/持久化方式 |
| 4 | `ResourceEntity.Status` (`resource/domain/ResourceEntity.java:430`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.ResourceStatusEnum` | 保留枚举值/持久化方式 |
| 4 | `ResourceManifestEntity.Status` (`resource/domain/ResourceManifestEntity.java:414`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java` | `top.egon.cola.platform.rbac3.admin.resource.domain.enums.ResourceManifestStatusEnum` | 保留枚举值/持久化方式 |
| 4 | `ResourceManifestRepository.TypedResource` (`resource/infrastructure/ResourceManifestRepository.java:827`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.resource.repository.internal.TypedResource` | 包级顶层；不得扩大 public API |
| 4 | `RoleFacade.HierarchyStore` (`role/application/RoleFacade.java:340`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/role/RoleControlFacadeTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/role/RoleHierarchyConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.role.repository.RoleHierarchyRepository` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.RoleControlStore` (`role/application/RoleFacade.java:444`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/role/RoleControlFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.role.repository.RoleControlRepository` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.CreateRoleCommand` (`role/application/RoleFacade.java:582`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/role/RoleControlFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.role.domain.dto.CreateRoleCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.AssignPermissionCommand` (`role/application/RoleFacade.java:689`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/role/RoleControlFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.role.domain.dto.AssignPermissionCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.AssignPermissionsCommand` (`role/application/RoleFacade.java:765`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/role/RoleControlFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.role.domain.dto.AssignPermissionsCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.RemovePermissionCommand` (`role/application/RoleFacade.java:875`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.role.domain.dto.RemovePermissionCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.UpdateRoleCommand` (`role/application/RoleFacade.java:944`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.role.domain.dto.UpdateRoleCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.InheritanceCommand` (`role/application/RoleFacade.java:1034`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java` | `top.egon.cola.platform.rbac3.admin.role.domain.dto.InheritanceCommandDTO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.RoleView` (`role/application/RoleFacade.java:1103`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleVO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.RoleImpactView` (`role/application/RoleFacade.java:1193`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/application/AuthorizationSimulationService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/infrastructure/PostgresqlRoleImpactSource.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/PostgresqlRoleImpactSourceTest.java` | `top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleImpactVO` | 保留可见性/注解/构造校验 |
| 4 | `RoleFacade.RoleMutationResult` (`role/application/RoleFacade.java:1277`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RolePermissionController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/role/RoleControlFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleMutationResultVO` | 保留可见性/注解/构造校验 |
| 4 | `RoleClosureEntity.Key` (`role/domain/RoleClosureEntity.java:95`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/domain/RoleClosureEntity.java` | `top.egon.cola.platform.rbac3.admin.role.domain.RoleClosureKey` | 保留可见性/注解/构造校验 |
| 4 | `RoleEntity.RoleType` (`role/domain/RoleEntity.java:385`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleTypeEnum` | 保留枚举值/持久化方式 |
| 4 | `RoleEntity.RiskLevel` (`role/domain/RoleEntity.java:435`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleRiskLevelEnum` | 保留枚举值/持久化方式 |
| 4 | `RoleEntity.Status` (`role/domain/RoleEntity.java:477`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleStatusEnum` | 保留枚举值/持久化方式 |
| 4 | `RolePermissionEntity.Status` (`role/domain/RolePermissionEntity.java:191`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java` | `top.egon.cola.platform.rbac3.admin.role.domain.enums.RolePermissionStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `ActiveRoleSetRevalidator.CurrentActivationSource` (`activation/application/ActiveRoleSetRevalidator.java:137`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.activation.repository.CurrentActivationRepository` | 保留可见性/注解/构造校验 |
| 5 | `ActiveRoleSetRevalidator.ReselectionStore` (`activation/application/ActiveRoleSetRevalidator.java:161`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java` | `top.egon.cola.platform.rbac3.admin.activation.repository.ReselectionRepository` | 保留可见性/注解/构造校验 |
| 5 | `ActiveRoleSetRevalidator.RevalidationCommand` (`activation/application/ActiveRoleSetRevalidator.java:196`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/ActiveRoleSetRevalidatorTest.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.dto.RevalidationCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ActiveRoleSetRevalidator.CurrentActivation` (`activation/application/ActiveRoleSetRevalidator.java:250`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/ActiveRoleSetRevalidatorTest.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentActivationVO` | 保留可见性/注解/构造校验 |
| 5 | `ActiveRoleSetRevalidator.RevalidationResult` (`activation/application/ActiveRoleSetRevalidator.java:289`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.RevalidationResultVO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationCandidateService.ActivationFactSource` (`activation/application/RoleActivationCandidateService.java:206`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/ActiveRoleSetRevalidator.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/RoleActivationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/RoleActivationFactStore.java` | `top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationFactRepository` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationCandidateService.ActivationFacts` (`activation/application/RoleActivationCandidateService.java:242`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/RoleActivationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/RoleActivationFactStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SessionSnapshotProjector.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationCandidateServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/performance/AdminQueryBudgetIT.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.ActivationFactsVO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationCandidateService.ApplicationFact` (`activation/application/RoleActivationCandidateService.java:379`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/RoleActivationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/RoleActivationFactStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SessionSnapshotProjector.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationCandidateServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/performance/AdminQueryBudgetIT.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.ApplicationFactVO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.ActivationTransaction` (`activation/application/RoleActivationFacade.java:366`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.activation.repository.ActivationTransaction` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.RuntimeStore` (`activation/application/RoleActivationFacade.java:458`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/infrastructure/RedisAuthorizationRuntimeStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationRuntimeRepository` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.ReplaceCommand` (`activation/application/RoleActivationFacade.java:506`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RoleActivationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationConcurrencyIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/SessionActiveRoleRepositoryTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializerTest.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.dto.ReplaceCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.SessionState` (`activation/application/RoleActivationFacade.java:620`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.SessionStateVO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.ResolvedActivation` (`activation/application/RoleActivationFacade.java:764`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.ResolvedActivationVO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.TransactionResult` (`activation/application/RoleActivationFacade.java:801`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.TransactionResultVO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.CurrentState` (`activation/application/RoleActivationFacade.java:891`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.CurrentStateVO` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFacade.RuntimePublication` (`activation/application/RoleActivationFacade.java:958`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/infrastructure/RedisAuthorizationRuntimeStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/activation/RoleActivationFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.vo.RuntimePublicationVO` | 保留可见性/注解/构造校验 |
| 5 | `SessionActiveRoleEntity.Key` (`activation/domain/SessionActiveRoleEntity.java:231`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/domain/SessionActiveRoleEntity.java` | `top.egon.cola.platform.rbac3.admin.activation.domain.SessionActiveRoleKey` | 保留可见性/注解/构造校验 |
| 5 | `RoleActivationFactStore.MutableDsd` (`activation/infrastructure/RoleActivationFactStore.java:458`) | `private class` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.activation.repository.internal.MutableDsd` | 包级顶层；不得扩大 public API |
| 5 | `AssignmentFacade.AssignmentFactSource` (`assignment/application/AssignmentFacade.java:361`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java` | `top.egon.cola.platform.rbac3.admin.assignment.repository.AssignmentFactRepository` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignmentLock` (`assignment/application/AssignmentFacade.java:397`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/PostgresqlAssignmentLockStore.java` | `top.egon.cola.platform.rbac3.admin.assignment.repository.AssignmentLock` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignmentStore` (`assignment/application/AssignmentFacade.java:419`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java` | `top.egon.cola.platform.rbac3.admin.assignment.repository.RoleAssignmentRepository` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.LockExecution` (`assignment/application/AssignmentFacade.java:480`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/PostgresqlAssignmentLockStore.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.vo.LockExecutionVO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignRequest` (`assignment/application/AssignmentFacade.java:546`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.dto.RoleAssignmentDTO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignmentCommand` (`assignment/application/AssignmentFacade.java:673`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.dto.AssignmentCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.ChangeRequest` (`assignment/application/AssignmentFacade.java:722`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.dto.RoleAssignmentChangeDTO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignmentChangeFacts` (`assignment/application/AssignmentFacade.java:841`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.vo.AssignmentChangeFactsVO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignmentFacts` (`assignment/application/AssignmentFacade.java:886`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.vo.AssignmentFactsVO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.Cardinality` (`assignment/application/AssignmentFacade.java:996`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.vo.CardinalityVO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignmentResult` (`assignment/application/AssignmentFacade.java:1045`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.vo.AssignmentResultVO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.AssignmentView` (`assignment/application/AssignmentFacade.java:1106`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.vo.AssignmentVO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentFacade.ChangeOperation` (`assignment/application/AssignmentFacade.java:1189`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.enums.AssignmentChangeOperationEnum` | 保留可见性/注解/构造校验 |
| 5 | `AutoAssignmentRuleEntity.MatchType` (`assignment/domain/AutoAssignmentRuleEntity.java:181`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.assignment.domain.enums.AutoAssignmentRuleMatchTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `AutoAssignmentRuleEntity.Status` (`assignment/domain/AutoAssignmentRuleEntity.java:207`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.assignment.domain.enums.AutoAssignmentRuleStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `UserRoleAssignmentEntity.AssignmentType` (`assignment/domain/UserRoleAssignmentEntity.java:442`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.enums.UserRoleAssignmentTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `UserRoleAssignmentEntity.Status` (`assignment/domain/UserRoleAssignmentEntity.java:484`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/infrastructure/AssignmentRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlDevelopmentTopologyBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.assignment.domain.enums.UserRoleAssignmentStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `ConstraintFacade.RoleFactSource` (`constraint/application/ConstraintFacade.java:294`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.repository.RoleFactRepository` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.ConstraintStore` (`constraint/application/ConstraintFacade.java:316`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.repository.ConstraintRepository` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.RoleFact` (`constraint/application/ConstraintFacade.java:450`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/constraint/ConstraintFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.vo.RoleFactVO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.SodCommand` (`constraint/application/ConstraintFacade.java:483`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/constraint/ConstraintFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.SodCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.SaveSodCommand` (`constraint/application/ConstraintFacade.java:555`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.SaveSodCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.PrerequisiteGroupCommand` (`constraint/application/ConstraintFacade.java:685`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.PrerequisiteGroupCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.CardinalityCommand` (`constraint/application/ConstraintFacade.java:780`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.CardinalityCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.DataRuleCommand` (`constraint/application/ConstraintFacade.java:868`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.DataRuleCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.FieldRuleCommand` (`constraint/application/ConstraintFacade.java:1011`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.FieldRuleCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.OperationSodRuleCommand` (`constraint/application/ConstraintFacade.java:1122`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.OperationSodRuleCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.RuleReference` (`constraint/application/ConstraintFacade.java:1224`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.vo.RuleReferenceVO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.MutationResult` (`constraint/application/ConstraintFacade.java:1251`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.vo.MutationResultVO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.SodView` (`constraint/application/ConstraintFacade.java:1303`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.vo.SodVO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.DataRuleView` (`constraint/application/ConstraintFacade.java:1407`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.vo.DataRuleVO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.FieldRuleView` (`constraint/application/ConstraintFacade.java:1511`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.vo.FieldRuleVO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.OperationSodRuleView` (`constraint/application/ConstraintFacade.java:1594`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.vo.OperationSodRuleVO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintFacade.ConstraintType` (`constraint/application/ConstraintFacade.java:1661`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ConstraintController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/constraint/ConstraintFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.ConstraintTypeEnum` | 保留可见性/注解/构造校验 |
| 5 | `DataRuleEntity.ScopeType` (`constraint/domain/DataRuleEntity.java:289`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/constraint/ConstraintPersistenceEntityTest.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.DataRuleScopeTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `DataRuleEntity.Status` (`constraint/domain/DataRuleEntity.java:355`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.DataRuleStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `DataRuleReferenceEntity.ReferenceType` (`constraint/domain/DataRuleReferenceEntity.java:136`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.DataRuleReferenceReferenceTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `DataRuleReferenceEntity.Key` (`constraint/domain/DataRuleReferenceEntity.java:183`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/domain/DataRuleReferenceEntity.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.DataRuleReferenceKey` | 保留可见性/注解/构造校验 |
| 5 | `FieldRuleEntity.AccessLevel` (`constraint/domain/FieldRuleEntity.java:297`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/constraint/ConstraintPersistenceEntityTest.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.FieldRuleAccessLevelEnum` | 保留枚举值/持久化方式 |
| 5 | `FieldRuleEntity.Status` (`constraint/domain/FieldRuleEntity.java:339`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.FieldRuleStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `OperationSodRuleEntity.Status` (`constraint/domain/OperationSodRuleEntity.java:291`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/infrastructure/AuthorizationRuleRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.OperationSodRuleStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `RoleCardinalityEntity.ScopeType` (`constraint/domain/RoleCardinalityEntity.java:180`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.RoleCardinalityScopeTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `RoleCardinalityEntity.Status` (`constraint/domain/RoleCardinalityEntity.java:214`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.RoleCardinalityStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `RolePrerequisiteEntity.MatchMode` (`constraint/domain/RolePrerequisiteEntity.java:138`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.RolePrerequisiteMatchModeEnum` | 保留枚举值/持久化方式 |
| 5 | `RolePrerequisiteEntity.Status` (`constraint/domain/RolePrerequisiteEntity.java:164`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.RolePrerequisiteStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `SodMemberEntity.Key` (`constraint/domain/SodMemberEntity.java:105`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/domain/SodMemberEntity.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.SodMemberKey` | 保留可见性/注解/构造校验 |
| 5 | `SodSetEntity.ConstraintType` (`constraint/domain/SodSetEntity.java:279`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java` | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.SodSetConstraintTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `SodSetEntity.Status` (`constraint/domain/SodSetEntity.java:305`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.enums.SodSetStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `AssignmentController.SessionStrengthPort` (`interfaces/http/AssignmentController.java:429`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3IdentitySessionQueryStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3GatewayDocumentCatalogContractTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/Rbac3AssignmentManagementGatewayDiscoveryTest.java` | `top.egon.cola.platform.rbac3.admin.assignment.service.AssignmentSessionStrengthService` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentController.AssignRequest` (`interfaces/http/AssignmentController.java:460`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.assignment.domain.dto.AssignRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `AssignmentController.ChangeRequest` (`interfaces/http/AssignmentController.java:532`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.assignment.domain.dto.RoleAssignmentChangeRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintController.SodSetRequest` (`interfaces/http/ConstraintController.java:509`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.SodSetRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintController.PrerequisiteGroupRequest` (`interfaces/http/ConstraintController.java:588`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.PrerequisiteGroupRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintController.CardinalityRequest` (`interfaces/http/ConstraintController.java:636`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.CardinalityRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintController.DataRuleRequest` (`interfaces/http/ConstraintController.java:696`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.DataRuleRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintController.FieldRuleRequest` (`interfaces/http/ConstraintController.java:787`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.FieldRuleRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ConstraintController.OperationSodRuleRequest` (`interfaces/http/ConstraintController.java:870`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.constraint.domain.dto.OperationSodRuleRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyController.PolicyRequest` (`interfaces/http/ManagementPolicyController.java:547`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.dto.PolicyRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyController.Subject` (`interfaces/http/ManagementPolicyController.java:657`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.dto.ManagementPolicySubjectDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyController.Scope` (`interfaces/http/ManagementPolicyController.java:682`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.dto.ManagementPolicyScopeDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyController.Restrictions` (`interfaces/http/ManagementPolicyController.java:712`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.dto.ManagementPolicyRestrictionsDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.PolicyFactSource` (`management/application/ManagementPolicyFacade.java:238`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.repository.ManagementPolicyFactRepository` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.ControlStore` (`management/application/ManagementPolicyFacade.java:266`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.repository.ManagementPolicyControlRepository` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.Request` (`management/application/ManagementPolicyFacade.java:398`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/application/AssignmentFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/management/ManagementPolicyFacadeTest.java` | `top.egon.cola.platform.rbac3.admin.management.domain.dto.ManagementPolicyRequestDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.SaveCommand` (`management/application/ManagementPolicyFacade.java:511`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.dto.SaveCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.PolicyView` (`management/application/ManagementPolicyFacade.java:672`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.vo.PolicyVO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.Restrictions` (`management/application/ManagementPolicyFacade.java:813`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyRestrictionsVO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.Subject` (`management/application/ManagementPolicyFacade.java:883`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicySubjectVO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.Scope` (`management/application/ManagementPolicyFacade.java:908`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyScopeVO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.CapabilityView` (`management/application/ManagementPolicyFacade.java:934`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.vo.CapabilityVO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.ManagedUserView` (`management/application/ManagementPolicyFacade.java:989`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedUserVO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyFacade.ManagedRoleView` (`management/application/ManagementPolicyFacade.java:1030`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedRoleVO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementOperationEntity.Key` (`management/domain/ManagementOperationEntity.java:95`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/domain/ManagementOperationEntity.java` | `top.egon.cola.platform.rbac3.admin.management.domain.ManagementOperationKey` | 保留可见性/注解/构造校验 |
| 5 | `ManagementOperationEntity.Operation` (`management/domain/ManagementOperationEntity.java:124`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementOperationOperationEnum` | 保留枚举值/持久化方式 |
| 5 | `ManagementPolicyEntity.Status` (`management/domain/ManagementPolicyEntity.java:502`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyStatusEnum` | 保留枚举值/持久化方式 |
| 5 | `ManagementPolicyEntity.RiskLevel` (`management/domain/ManagementPolicyEntity.java:544`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyRiskLevelEnum` | 保留枚举值/持久化方式 |
| 5 | `ManagementPolicyEntity.AuthenticationStrength` (`management/domain/ManagementPolicyEntity.java:586`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyAuthenticationStrengthEnum` | 保留枚举值/持久化方式 |
| 5 | `ManagementRoleEntity.Key` (`management/domain/ManagementRoleEntity.java:92`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/domain/ManagementRoleEntity.java` | `top.egon.cola.platform.rbac3.admin.management.domain.ManagementRoleKey` | 保留可见性/注解/构造校验 |
| 5 | `ManagementScopeEntity.Key` (`management/domain/ManagementScopeEntity.java:117`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/domain/ManagementScopeEntity.java` | `top.egon.cola.platform.rbac3.admin.management.domain.ManagementScopeKey` | 保留可见性/注解/构造校验 |
| 5 | `ManagementScopeEntity.ScopeType` (`management/domain/ManagementScopeEntity.java:160`) | `public enum` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementScopeScopeTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `ManagementSubjectEntity.Key` (`management/domain/ManagementSubjectEntity.java:113`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/domain/ManagementSubjectEntity.java` | `top.egon.cola.platform.rbac3.admin.management.domain.ManagementSubjectKey` | 保留可见性/注解/构造校验 |
| 5 | `ManagementSubjectEntity.SubjectType` (`management/domain/ManagementSubjectEntity.java:156`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/management/infrastructure/ManagementPolicyRepository.java` | `top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementSubjectSubjectTypeEnum` | 保留枚举值/持久化方式 |
| 5 | `ManagementPolicyRepository.SavePolicyCommand` (`management/infrastructure/ManagementPolicyRepository.java:937`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.dto.SavePolicyCommandDTO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyRepository.Subject` (`management/infrastructure/ManagementPolicyRepository.java:1136`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicySubjectPO` | 保留可见性/注解/构造校验 |
| 5 | `ManagementPolicyRepository.Scope` (`management/infrastructure/ManagementPolicyRepository.java:1161`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementPolicyScopePO` | 保留可见性/注解/构造校验 |
| 5 | `PostgresqlAssignmentLifecycleStore.DueAssignment` (`worker/PostgresqlAssignmentLifecycleStore.java:161`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.assignment.repository.internal.DueAssignment` | 包级顶层；不得扩大 public API |
| 6 | `AuditPort.AuditEvent` (`application/port/AuditPort.java:46`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/SessionSecurityEventRecorder.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/application/AuthorizationSimulationService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/SessionSecurityEventRecorderTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditEventVO` | 接口成员隐式 public；独立后显式 public |
| 6 | `AuditQueryService.AuditStore` (`audit/application/AuditQueryService.java:262`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/audit/infrastructure/PostgresqlAuditStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/audit/AuditRedactionIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/performance/AdminQueryBudgetIT.java` | `top.egon.cola.platform.rbac3.admin.audit.repository.AuditRepository` | 保留可见性/注解/构造校验 |
| 6 | `AuditQueryService.AuditCommand` (`audit/application/AuditQueryService.java:311`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/audit/infrastructure/PostgresqlAuditStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/audit/AuditRedactionIT.java` | `top.egon.cola.platform.rbac3.admin.audit.domain.dto.AuditCommandDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuditQueryService.Query` (`audit/application/AuditQueryService.java:493`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/audit/infrastructure/PostgresqlAuditStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/audit/AuditRedactionIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/performance/AdminQueryBudgetIT.java` | `top.egon.cola.platform.rbac3.admin.audit.domain.dto.QueryDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuditQueryService.AuditView` (`audit/application/AuditQueryService.java:651`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/audit/infrastructure/PostgresqlAuditStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/audit/AuditRedactionIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/audit/PostgresqlAuditStoreTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/performance/AdminQueryBudgetIT.java` | `top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditVO` | 保留可见性/注解/构造校验 |
| 6 | `AuditQueryService.Page` (`audit/application/AuditQueryService.java:802`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/audit/infrastructure/PostgresqlAuditStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/audit/AuditRedactionIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/performance/AdminQueryBudgetIT.java` | `top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditQueryPageVO` | 保留可见性/注解/构造校验 |
| 6 | `AuditCursorCodec.CursorPosition` (`audit/infrastructure/AuditCursorCodec.java:205`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/audit/infrastructure/PostgresqlAuditStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/audit/AuditCursorCodecTest.java` | `top.egon.cola.platform.rbac3.admin.audit.domain.vo.CursorPositionVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.SnapshotSource` (`authorization/application/AuthorizationDecisionService.java:549`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/infrastructure/RedisAuthorizationRuntimeStore.java` | `top.egon.cola.platform.rbac3.admin.authorization.repository.AuthorizationSnapshotRepository` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.FenceVerifier` (`authorization/application/AuthorizationDecisionService.java:571`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/infrastructure/RedisAuthorizationRuntimeStore.java` | `top.egon.cola.platform.rbac3.admin.authorization.repository.FenceVerifier` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.SnapshotRecord` (`authorization/application/AuthorizationDecisionService.java:597`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/security/Rbac3JwtAuthenticationConverter.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/application/AuthorizationSimulationService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/infrastructure/RedisAuthorizationRuntimeStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3EndToEndUseCaseIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotServiceTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.Subject` (`authorization/application/AuthorizationDecisionService.java:674`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3EndToEndUseCaseIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.vo.AuthorizationDecisionSubjectVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.Resource` (`authorization/application/AuthorizationDecisionService.java:719`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3EndToEndUseCaseIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.vo.AuthorizationDecisionResourceVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.TokenVersions` (`authorization/application/AuthorizationDecisionService.java:757`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3EndToEndUseCaseIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.vo.TokenVersionsVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.DecisionRequest` (`authorization/application/AuthorizationDecisionService.java:805`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/application/AuthorizationSimulationService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3EndToEndUseCaseIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationControllerTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.dto.DecisionRequestDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.DecisionBundle` (`authorization/application/AuthorizationDecisionService.java:882`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/application/AuthorizationSimulationService.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.vo.DecisionBundleVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.FenceVerification` (`authorization/application/AuthorizationDecisionService.java:928`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationController.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.vo.FenceVerificationVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.ResourceAccessRequest` (`authorization/application/AuthorizationDecisionService.java:975`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ResourceAccessDecisionRequest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationControllerTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.dto.ResourceAccessRequestDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.ResourceAccessDecision` (`authorization/application/AuthorizationDecisionService.java:1050`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ResourceAccessDecisionResponse.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationControllerTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.vo.ResourceAccessDecisionVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationDecisionService.DecisionType` (`authorization/application/AuthorizationDecisionService.java:1138`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3EndToEndUseCaseIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.enums.AuthorizationDecisionDecisionTypeEnum` | 保留可见性/注解/构造校验 |
| 6 | `AuditSimulationController.SimulationRequest` (`interfaces/http/AuditSimulationController.java:221`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.simulation.domain.dto.AuthorizationSimulationRequestDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuditSimulationController.RoleChangeImpactRequest` (`interfaces/http/AuditSimulationController.java:258`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.simulation.domain.dto.AuthorizationRoleChangeImpactRequestDTO` | 保留可见性/注解/构造校验 |
| 6 | `InternalAuthorizationController.FenceRequest` (`interfaces/http/InternalAuthorizationController.java:220`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationControllerTest.java` | `top.egon.cola.platform.rbac3.admin.authorization.domain.dto.AuthorizationFenceRequestDTO` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.OperationSodRuleSource` (`participation/application/ParticipationFacade.java:217`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/infrastructure/AuthorizationRuleRepository.java` | `top.egon.cola.platform.rbac3.admin.participation.repository.OperationSodRuleRepository` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.ParticipationStore` (`participation/application/ParticipationFacade.java:247`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/participation/infrastructure/PostgresqlParticipationStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.participation.repository.ParticipationRepository` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.ParticipationRecord` (`participation/application/ParticipationFacade.java:299`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/participation/infrastructure/PostgresqlParticipationStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationRecordVO` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.ParticipationFact` (`participation/application/ParticipationFacade.java:399`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/participation/infrastructure/PostgresqlParticipationStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationFactVO` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.PriorActionRule` (`participation/application/ParticipationFacade.java:485`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/infrastructure/AuthorizationRuleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/participation/infrastructure/PostgresqlParticipationStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.participation.domain.vo.PriorActionRuleVO` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.AppendResult` (`participation/application/ParticipationFacade.java:543`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/participation/infrastructure/PostgresqlParticipationStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.participation.domain.vo.AppendResultVO` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.RecordResult` (`participation/application/ParticipationFacade.java:595`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ParticipationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.participation.domain.vo.RecordResultVO` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.ConflictQuery` (`participation/application/ParticipationFacade.java:635`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ParticipationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/participation/infrastructure/PostgresqlParticipationStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java` | `top.egon.cola.platform.rbac3.admin.participation.domain.dto.ConflictQueryDTO` | 保留可见性/注解/构造校验 |
| 6 | `ParticipationFacade.ConflictDecision` (`participation/application/ParticipationFacade.java:690`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ParticipationController.java` | `top.egon.cola.platform.rbac3.admin.participation.domain.vo.ConflictDecisionVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationSimulationService.SimulationRequest` (`simulation/application/AuthorizationSimulationService.java:171`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.simulation.domain.dto.SimulationRequestDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationSimulationService.Hypothesis` (`simulation/application/AuthorizationSimulationService.java:244`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.simulation.domain.dto.HypothesisDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationSimulationService.SimulationResult` (`simulation/application/AuthorizationSimulationService.java:292`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java` | `top.egon.cola.platform.rbac3.admin.simulation.domain.vo.SimulationResultVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationSimulationService.RoleChangeImpactRequest` (`simulation/application/AuthorizationSimulationService.java:363`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.simulation.domain.dto.RoleChangeImpactRequestDTO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationSimulationService.RoleImpactSnapshot` (`simulation/application/AuthorizationSimulationService.java:427`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/infrastructure/PostgresqlRoleImpactSource.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/AuthorizationSimulationServiceTest.java` | `top.egon.cola.platform.rbac3.admin.simulation.domain.vo.RoleImpactSnapshotVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationSimulationService.RoleChangeImpactResult` (`simulation/application/AuthorizationSimulationService.java:484`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuditSimulationController.java` | `top.egon.cola.platform.rbac3.admin.simulation.domain.vo.RoleChangeImpactResultVO` | 保留可见性/注解/构造校验 |
| 6 | `AuthorizationSimulationService.RoleImpactSource` (`simulation/application/AuthorizationSimulationService.java:527`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/infrastructure/PostgresqlRoleImpactSource.java` | `top.egon.cola.platform.rbac3.admin.simulation.repository.RoleImpactRepository` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationEventPort.AuthorizationEvent` (`application/port/AuthorizationEventPort.java:40`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/infrastructure/PostgresqlPlatformAdminBootstrapStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/constraint/infrastructure/ConstraintRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/infrastructure/ResourceManifestRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/role/infrastructure/RoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/SessionSecurityEventRecorder.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3WorkerConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/OutboxTransactionRollbackIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/SessionSecurityEventRecorderTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `Rbac3RuntimePolicy.Snapshot` (`application/port/Rbac3RuntimePolicy.java:40`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/JwtTokenService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/SessionFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicyTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.Rbac3RuntimePolicySnapshotVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `RuntimeProjectionPort.RuntimeProjection` (`application/port/RuntimeProjectionPort.java:38`) | `implicit-public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeProjectionVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `RuntimeProjectionPort.ProjectionResult` (`application/port/RuntimeProjectionPort.java:117`) | `implicit-public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ProjectionResultVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `AtomicRbac3RuntimePolicy.ApplyFailure` (`integration/ddc/AtomicRbac3RuntimePolicy.java:343`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/DdcConfigClientStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/ddc/AtomicRbac3RuntimePolicyTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ApplyFailureVO` | 保留可见性/注解/构造校验 |
| 7 | `AtomicRbac3RuntimePolicy.PolicyApplyException` (`integration/ddc/AtomicRbac3RuntimePolicy.java:387`) | `private class` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.domain.exception.PolicyApplyException` | 包级顶层；不得扩大 public API |
| 7 | `DdcProviderLeaseStatusService.ProviderLeaseStatus` (`integration/ddc/DdcProviderLeaseStatusService.java:88`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/GatewayDdcRuntimeStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DdcProviderLeaseStatusVO` | 保留可见性/注解/构造校验 |
| 7 | `Rbac3DdcPolicyApplier.ApplyObserver` (`integration/ddc/Rbac3DdcPolicyApplier.java:138`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3DdcPolicyConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/Rbac3IntegrationMetrics.java` | `top.egon.cola.platform.rbac3.admin.runtime.service.internal.ApplyObserver` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.Transport` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:569`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayAdminControlPlaneStatusClientTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayAdminControlPlaneTransport` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.HttpResponse` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:599`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayAdminControlPlaneStatusClientTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminControlPlaneHttpResponseVO` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.JdkTransport` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:621`) | `private class` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.repository.internal.JdkGatewayAdminControlPlaneTransport` | 包级顶层；不得扩大 public API |
| 7 | `GatewayAdminControlPlaneStatusClient.Response` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:686`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.repository.internal.GatewayAdminControlPlaneResponse` | 包级顶层；不得扩大 public API |
| 7 | `GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:754`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/GatewayDdcRuntimeStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminSnapshotVO` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.ReleaseObservation` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:803`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayReleaseObservationVO` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.ProviderObservation` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:881`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayProviderObservationVO` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.ProviderInstance` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:949`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayProviderInstanceVO` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.ServiceKey` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:1001`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/GatewayDdcRuntimeStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.GatewayServiceKey` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminControlPlaneStatusClient.ConsistencyObservation` (`integration/gateway/GatewayAdminControlPlaneStatusClient.java:1112`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayConsistencyObservationVO` | 保留可见性/注解/构造校验 |
| 7 | `GatewayAdminStatusCredentialProvider.BearerCredential` (`integration/gateway/GatewayAdminStatusCredentialProvider.java:83`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/gateway/GatewayAdminControlPlaneStatusClient.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayAdminControlPlaneStatusClientTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.BearerCredentialVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `GatewayDefinitionStatusService.DefinitionStatus` (`integration/gateway/GatewayDefinitionStatusService.java:122`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/GatewayDdcRuntimeStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayDefinitionStatusVO` | 保留可见性/注解/构造校验 |
| 7 | `Rbac3RuntimeProjectionDeliveryHandler.ProjectionSink` (`integration/outbox/Rbac3RuntimeProjectionDeliveryHandler.java:257`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/RuntimeSnapshotRebuildWorker.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.ProjectionSink` | 保留可见性/注解/构造校验 |
| 7 | `Rbac3RuntimeProjectionDeliveryHandler.ProjectionOutcome` (`integration/outbox/Rbac3RuntimeProjectionDeliveryHandler.java:279`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/RuntimeSnapshotRebuildWorker.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/OutboxTransactionRollbackIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum` | 保留可见性/注解/构造校验 |
| 7 | `Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope` (`integration/outbox/Rbac3RuntimeProjectionDeliveryHandler.java:332`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/RuntimeSnapshotRebuildWorker.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.EventEnvelopeVO` | 保留可见性/注解/构造校验 |
| 7 | `GatewayDdcRuntimeStatusService.ServiceIdentity` (`integration/runtime/GatewayDdcRuntimeStatusService.java:224`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/DdcProviderLeaseStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/gateway/GatewayDefinitionStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3AdminApplicationContextTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3DdcRefreshIntegrationTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ServiceIdentityVO` | 保留可见性/注解/构造校验 |
| 7 | `Rbac3OperationalRuntimeStatusService.OperationalStatus` (`integration/runtime/Rbac3OperationalRuntimeStatusService.java:297`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.OperationalStatusVO` | 保留可见性/注解/构造校验 |
| 7 | `Rbac3OperationalRuntimeStatusService.MutationFacts` (`integration/runtime/Rbac3OperationalRuntimeStatusService.java:345`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.repository.internal.MutationFacts` | 包级顶层；不得扩大 public API |
| 7 | `Rbac3ReadinessIndicator.ReadinessCheck` (`integration/runtime/Rbac3ReadinessIndicator.java:154`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3AdminApplicationContextTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ReadinessCheckVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationFenceService.FenceStore` (`runtime/application/AuthorizationFenceService.java:85`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3AuthorizationFenceStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/MutationFenceRollbackIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationFenceRepository` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationFenceService.Fence` (`runtime/application/AuthorizationFenceService.java:124`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3AuthorizationFenceStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/MutationFenceRollbackIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationFenceVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.MutationStore` (`runtime/application/AuthorizationMutationCoordinator.java:161`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/MutationFenceRollbackIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRepository` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.RuntimeProjector` (`runtime/application/AuthorizationMutationCoordinator.java:200`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.repository.RuntimeProjector` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.TransactionExecutor` (`runtime/application/AuthorizationMutationCoordinator.java:221`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.service.internal.TransactionExecutor` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.MutationIdGenerator` (`runtime/application/AuthorizationMutationCoordinator.java:243`) | `public interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.service.internal.MutationIdGenerator` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.MutationScope` (`runtime/application/AuthorizationMutationCoordinator.java:269`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/application/AssignmentFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/MutationFenceRollbackIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationScopeVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.ExpectedVersions` (`runtime/application/AuthorizationMutationCoordinator.java:327`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/application/AssignmentFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/MutationFenceRollbackIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ExpectedVersionsVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.MutationRecord` (`runtime/application/AuthorizationMutationCoordinator.java:392`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/MutationFenceRollbackIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationRecordVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.MutationResult` (`runtime/application/AuthorizationMutationCoordinator.java:450`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/assignment/application/AssignmentFacade.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationResultVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationCoordinator.MutationStatus` (`runtime/application/AuthorizationMutationCoordinator.java:501`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/assignment/AssignmentFacadeIT.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/MutationFenceRollbackIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationResultStatusEnum` | 保留可见性/注解/构造校验 |
| 7 | `ControlPlaneRuntimeStatusPort.RuntimeStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:43`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RuntimeController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/application/RuntimeQueryService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.DdcConfigClientStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:205`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/ddc/DdcConfigClientStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DdcConfigClientStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.DefinitionStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:317`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DefinitionStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.ProviderLeaseStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:369`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ProviderLeaseStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.GatewayReleaseStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:407`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayReleaseStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.FlywayStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:444`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3OperationalRuntimeStatusService.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.FlywayStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.RedisProjectionStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:469`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3OperationalRuntimeStatusService.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RedisProjectionStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.FenceMutationStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:496`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3OperationalRuntimeStatusService.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.FenceMutationStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `ControlPlaneRuntimeStatusPort.OutboxStatus` (`runtime/application/ControlPlaneRuntimeStatusPort.java:542`) | `implicit-public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3OperationalRuntimeStatusService.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.OutboxStatusVO` | 接口成员隐式 public；独立后显式 public |
| 7 | `IdempotencyService.IdempotencyStore` (`runtime/application/IdempotencyService.java:101`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/IdempotencyRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/IdempotencyServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.IdempotencyRepository` | 保留可见性/注解/构造校验 |
| 7 | `IdempotencyService.Command` (`runtime/application/IdempotencyService.java:153`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/IdempotencyServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.dto.IdempotencyCommandDTO` | 保留可见性/注解/构造校验 |
| 7 | `IdempotencyService.StoredCommand` (`runtime/application/IdempotencyService.java:237`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/IdempotencyRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/IdempotencyServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.dto.StoredCommandDTO` | 保留可见性/注解/构造校验 |
| 7 | `IdempotencyService.Claim` (`runtime/application/IdempotencyService.java:318`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/IdempotencyRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/IdempotencyServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.IdempotencyClaimVO` | 保留可见性/注解/构造校验 |
| 7 | `IdempotencyService.Outcome` (`runtime/application/IdempotencyService.java:369`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AssignmentController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ManagementPolicyController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/IdempotencyRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/IdempotencyServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyOutcomeEnum` | 保留可见性/注解/构造校验 |
| 7 | `RuntimeQueryService.MutationQueryPort` (`runtime/application/RuntimeQueryService.java:159`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.MutationQueryPort` | 保留可见性/注解/构造校验 |
| 7 | `RuntimeQueryService.MutationRecoveryPort` (`runtime/application/RuntimeQueryService.java:188`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationMutationRecoveryWorker.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.MutationRecoveryPort` | 保留可见性/注解/构造校验 |
| 7 | `RuntimeQueryService.MutationView` (`runtime/application/RuntimeQueryService.java:220`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationVO` | 保留可见性/注解/构造校验 |
| 7 | `RuntimeQueryService.MutationPage` (`runtime/application/RuntimeQueryService.java:297`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RuntimeController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationMutationPageVO` | 保留可见性/注解/构造校验 |
| 7 | `RuntimeQueryService.RetryResult` (`runtime/application/RuntimeQueryService.java:335`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RuntimeController.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationMutationRecoveryWorker.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/RuntimeQueryServiceTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RetryResultVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationEntity.ScopeType` (`runtime/domain/AuthorizationMutationEntity.java:446`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/infrastructure/SessionActiveRoleRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/runtime/AuthorizationMutationRepositoryTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationScopeTypeEnum` | 保留枚举值/持久化方式 |
| 7 | `AuthorizationMutationEntity.Status` (`runtime/domain/AuthorizationMutationEntity.java:480`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3OperationalRuntimeStatusService.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationStatusEnum` | 保留枚举值/持久化方式 |
| 7 | `IdempotencyRecordEntity.ActorType` (`runtime/domain/IdempotencyRecordEntity.java:305`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/IdempotencyRepository.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyRecordActorTypeEnum` | 保留枚举值/持久化方式 |
| 7 | `IdempotencyRecordEntity.Status` (`runtime/domain/IdempotencyRecordEntity.java:339`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/IdempotencyRepository.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyRecordStatusEnum` | 保留枚举值/持久化方式 |
| 7 | `LoginRuntimeProjectionFactory.RuntimeState` (`snapshot/application/LoginRuntimeProjectionFactory.java:139`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeStateVO` | 保留可见性/注解/构造校验 |
| 7 | `SessionSnapshotProjector.ProjectionCommand` (`snapshot/application/SessionSnapshotProjector.java:283`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/RoleActivationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.dto.ProjectionCommandDTO` | 保留可见性/注解/构造校验 |
| 7 | `SessionSnapshotProjector.RuntimeSession` (`snapshot/application/SessionSnapshotProjector.java:392`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/LoginRuntimeProjectionFactory.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/infrastructure/RedisAuthorizationRuntimeStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/snapshot/RedisAuthorizationRuntimeStoreIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RuntimeSessionVO` | 保留可见性/注解/构造校验 |
| 7 | `SessionSnapshotProjector.Projection` (`snapshot/application/SessionSnapshotProjector.java:478`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/activation/application/RoleActivationFacade.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/LoginRuntimeProjectionFactory.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/infrastructure/RedisAuthorizationRuntimeStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/snapshot/RedisAuthorizationRuntimeStoreIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.SessionSnapshotProjectionVO` | 保留可见性/注解/构造校验 |
| 7 | `SystemAuthorizationSnapshotService.ContextInitializer` (`snapshot/application/SystemAuthorizationSnapshotService.java:346`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java` | `top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationContextInitializer` | 保留可见性/注解/构造校验 |
| 7 | `SystemAuthorizationSnapshotService.ContextInitialization` (`snapshot/application/SystemAuthorizationSnapshotService.java:371`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializer.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/application/Rbac3DevelopmentAuthorizationContextInitializerTest.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotServiceTest.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.SystemAuthorizationSnapshotContextInitializationEnum` | 保留可见性/注解/构造校验 |
| 7 | `SystemAuthorizationSnapshotService.RetryPause` (`snapshot/application/SystemAuthorizationSnapshotService.java:406`) | `package interface` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.service.internal.RetryPause` | 包级顶层；不得扩大 public API |
| 7 | `RedisAuthorizationRuntimeStore.PublishCommand` (`snapshot/infrastructure/RedisAuthorizationRuntimeStore.java:431`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/snapshot/RedisAuthorizationRuntimeStoreIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.dto.PublishCommandDTO` | 保留可见性/注解/构造校验 |
| 7 | `RedisAuthorizationRuntimeStore.PublishResult` (`snapshot/infrastructure/RedisAuthorizationRuntimeStore.java:501`) | `public record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.PublishResultVO` | 保留可见性/注解/构造校验 |
| 7 | `AssignmentLifecycleWorker.LifecycleStore` (`worker/AssignmentLifecycleWorker.java:94`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/PostgresqlAssignmentLifecycleStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3WorkerConfiguration.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.AssignmentLifecycleRepository` | 保留可见性/注解/构造校验 |
| 7 | `AssignmentLifecycleWorker.ChangePublisher` (`worker/AssignmentLifecycleWorker.java:119`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/PostgresqlAssignmentLifecycleStore.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.ChangePublisher` | 保留可见性/注解/构造校验 |
| 7 | `AssignmentLifecycleWorker.LifecycleChange` (`worker/AssignmentLifecycleWorker.java:146`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/PostgresqlAssignmentLifecycleStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.vo.LifecycleChangeVO` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationRecoveryWorker.RecoveryStore` (`worker/AuthorizationMutationRecoveryWorker.java:176`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3WorkerConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRecoveryRepository` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationRecoveryWorker.ProjectionExecutor` (`worker/AuthorizationMutationRecoveryWorker.java:239`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3WorkerConfiguration.java` | `top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeProjectionExecutor` | 保留可见性/注解/构造校验 |
| 7 | `AuthorizationMutationRecoveryWorker.MutationWork` (`worker/AuthorizationMutationRecoveryWorker.java:266`) | `public record` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3ApplicationConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime/infrastructure/AuthorizationMutationRepository.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.dto.MutationWorkDTO` | 保留可见性/注解/构造校验 |
| 7 | `Rbac3WorkerConfiguration.Rbac3WorkerSchedules` (`worker/Rbac3WorkerConfiguration.java:141`) | `package class` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.repository.internal.Rbac3WorkerSchedules` | 包级顶层；不得扩大 public API |
| 7 | `RedisProjectionCheckpointStore.Checkpoint` (`worker/RedisProjectionCheckpointStore.java:274`) | `private record` | 仅宿主内使用简单名；执行时复扫 | `top.egon.cola.platform.rbac3.admin.runtime.repository.internal.Checkpoint` | 包级顶层；不得扩大 public API |
| 7 | `RuntimeSnapshotRebuildWorker.Claim` (`worker/RuntimeSnapshotRebuildWorker.java:99`) | `public enum` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/RedisProjectionCheckpointStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.domain.enums.RuntimeSnapshotRebuildClaimEnum` | 保留可见性/注解/构造校验 |
| 7 | `RuntimeSnapshotRebuildWorker.ProjectionCheckpointStore` (`worker/RuntimeSnapshotRebuildWorker.java:141`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3WorkerConfiguration.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/RedisProjectionCheckpointStore.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/worker/AuthorizationWorkerRecoveryIT.java` | `top.egon.cola.platform.rbac3.admin.runtime.repository.ProjectionCheckpointRepository` | 保留可见性/注解/构造校验 |
| 7 | `RuntimeSnapshotRebuildWorker.RebuildPort` (`worker/RuntimeSnapshotRebuildWorker.java:213`) | `public interface` | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3RuntimeProjectionRecovery.java`<br>`egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/worker/Rbac3WorkerConfiguration.java` | `top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeSnapshotRebuildService` | 保留可见性/注解/构造校验 |
