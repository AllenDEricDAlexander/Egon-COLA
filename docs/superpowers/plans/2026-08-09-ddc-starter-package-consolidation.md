# DDC Starter Role-Based Package Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 DDC Starter 破坏式迁移为按角色分包的单 Starter 结构，补齐每个包的中英双语包级契约和 `@NonNullApi`，同步迁移仓库内所有消费者，并保持现有 ConfigData、刷新、生命周期、Registry、HTTP、Redis 与可观测性行为不变。

**Architecture:** 公共端口统一位于 `api`，跨模块数据位于 `model`，三个 HTTP Adapter 位于 `client`，同步编排位于 `service`，异步事件入口位于 `listener`，可变运行态位于 `state`，Redis、ConfigData、环境、格式、可观测性和错误各自拥有独立技术边界。Spring Boot 自动装配仍内置于唯一的 Starter artifact，并通过显式 `@Bean`/精确 `@Import` 组装上述角色，不再依赖组件扫描。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring Framework 6.2.18 `@NonNullApi`/`@Nullable`、Redisson 3.26.0、Maven、JUnit 5、Mockito、AssertJ。

**Exact path roots used below:**

- `DDC_MAIN` = `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc`
- `DDC_TEST` = `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc`
- `DDC_RESOURCES` = `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/resources`

Every path using one of these labels is relative to the exact repository path declared here; the labels are documentation abbreviations, not shell variables or unresolved implementation placeholders.

## Global Constraints

- 规格来源：`docs/superpowers/specs/2026-08-09-ddc-starter-package-consolidation-design.md`；实现不得偏离其中的目标树、迁移映射和行为边界。
- 只保留 `egon-cola-platform-dynamic-config-center-starter` 这一个业务消费入口；不新增 `core`、`client`、`autoconfigure` 或其他 Maven module。
- 用户允许破坏式更新：旧包直接删除，禁止 deprecated 转发类型、继承壳、双包并存兼容层和旧自动配置别名。
- 每个任务完成后只暂存该任务拥有的路径并独立提交；不得把工作树中已有的 Gateway 修改带入 DDC 提交。
- `DdcLeaseSession.java` 已有用户修改，迁移该文件时必须保留其内容变化；不得 reset、checkout 或覆盖用户修改。
- 不修改数据库、数据、现有 Flyway 文件或数据库依赖；不启动应用、浏览器、Redis、PostgreSQL 或其他运行时进程。
- 不改变 `ddc:application.yml` ConfigData 优先级、YAML-only、`@DdcValue`/`@DdcRefreshable` 刷新、注册/ACK/心跳/下线顺序、Registry 订阅与本地租约、HMAC、mTLS、Trace Header 或 Redis Topic 语义。
- 使用现有设计模式：配置格式与配置应用器保留 Strategy/Registry，Redis Topic 和注册快照保留 Observer，三个 HTTP Client 保留 Adapter；不新增 God Client、Facade 基类、Abstract Factory、Template Method 或泛化 `biz`/`pojo`/`common`/`impl` 包。
- 所有目标包，包括仅组织子包的中间包，都必须有独立 `package-info.java`；中文说明在前，英文说明在后，并声明 `org.springframework.lang.NonNullApi`。
- `package-info.java` 只承载包文档和包注解，不声明普通类型或常量。共享包可见常量只在确有多类型复用时进入职责明确的 package-private `final` 类；禁止机械创建 `Constants`、`Support`、`Internal`。
- `@NonNullApi` 是真实契约：合法的可空参数和返回值使用 `org.springframework.lang.Nullable`，不新增 JSpecify 依赖，不使用 `@NonNullFields`，不以空字符串、哨兵对象或 `Optional` 改写原有可空行为。
- 生产源码移动优先使用 `git mv` 保留历史；声明、import、资源注册和测试断言使用小范围补丁修改。
- 每个任务先运行最小失败测试，再实现并运行对应通过测试；最终才运行跨模块 Maven 验证。测试命令必须包含 `-Dsurefire.failIfNoSpecifiedTests=false`，避免 `-am` 上游模块没有指定测试时误报。

---

### Task 1: Establish All 38 Package Contracts

**Files:**

- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/DdcPackageDocumentationTest.java`
- Create under `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/`: every `package-info.java` in this exact tree:

```text
package-info.java
annotation/package-info.java
api/package-info.java
api/client/package-info.java
api/extension/package-info.java
api/refresh/package-info.java
api/registry/package-info.java
model/package-info.java
model/client/package-info.java
model/config/package-info.java
model/instance/package-info.java
model/lease/package-info.java
model/management/package-info.java
model/registry/package-info.java
client/package-info.java
client/config/package-info.java
client/http/package-info.java
client/management/package-info.java
client/registry/package-info.java
service/package-info.java
service/binding/package-info.java
service/lifecycle/package-info.java
service/refresh/package-info.java
service/registry/package-info.java
listener/package-info.java
listener/config/package-info.java
listener/registry/package-info.java
state/package-info.java
redis/package-info.java
configdata/package-info.java
environment/package-info.java
format/package-info.java
observability/package-info.java
error/package-info.java
error/http/package-info.java
error/management/package-info.java
autoconfigure/package-info.java
autoconfigure/properties/package-info.java
```

**Interfaces:**

- Package default: every package declares `@NonNullApi` independently; parent package annotations are not treated as inherited.
- Documentation contract: each file states in Chinese then English the package's sole responsibility, allowed types, excluded duties, dependency direction, and important package-private types/conventions when present.
- Structural contract: the test owns the exact 38-package documentation list but does not yet reject old implementation packages; old packages disappear only after Tasks 2-6.

- [ ] **Step 1: Add the failing documentation contract test.** Add `DdcPackageDocumentationTest` with the exact relative package list above. For each entry, resolve `src/main/java/top/egon/cola/component/ddc/<entry>/package-info.java`, require the file to exist, require `@NonNullApi`, require a CJK character, require an English sentence, and reject ordinary top-level declarations in the file. Use source assertions equivalent to:

```java
private static final List<String> TARGET_PACKAGES = List.of(
        "", "annotation", "api", "api/client", "api/extension",
        "api/refresh", "api/registry", "model", "model/client",
        "model/config", "model/instance", "model/lease",
        "model/management", "model/registry", "client", "client/config",
        "client/http", "client/management", "client/registry", "service",
        "service/binding", "service/lifecycle", "service/refresh",
        "service/registry", "listener", "listener/config",
        "listener/registry", "state", "redis", "configdata",
        "environment", "format", "observability", "error", "error/http",
        "error/management", "autoconfigure", "autoconfigure/properties"
);

assertThat(source).contains("@NonNullApi");
assertThat(source).contains("import org.springframework.lang.NonNullApi;");
assertThat(source).containsPattern("[\\u4E00-\\u9FFF]");
assertThat(source).containsPattern("[A-Za-z]{4,}");
assertThat(source).doesNotContainPattern("(?m)^\\s*(public\\s+)?(class|interface|enum|record)\\s+");
```

- [ ] **Step 2: Run the focused test and confirm the expected failure.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcPackageDocumentationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Expected: assertions report missing target `package-info.java` files; no production behavior failure is expected.

- [ ] **Step 3: Create all package contracts.** Use this exact structure in each file, replacing the wording with package-specific responsibilities and exclusions:

```java
/**
 * DDC 配置客户端公共端口，定义配置拉取、实例生命周期和发布确认能力。
 * 本包只保存调用方可实现或替换的接口；HTTP、Redis 和默认实现分别位于
 * {@code client}、{@code redis} 和 {@code service} 包。
 *
 * <p>Public DDC configuration-client ports for configuration retrieval,
 * instance lifecycle, and publication acknowledgements. This package contains
 * only interfaces that callers may implement or replace; HTTP, Redis, and
 * default implementations belong to {@code client}, {@code redis}, and
 * {@code service}, respectively.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.client;

import org.springframework.lang.NonNullApi;
```

For implementation packages, document known package-private collaborators explicitly: `service.binding.DdcFieldBinding`, HTTP canonical request/signing helpers if visibility is later narrowed, listener subscription lifecycle helpers, and responsibility-specific shared constants only if such types actually exist. State that no package-level shared type exists where none is needed.

- [ ] **Step 4: Run the documentation test again.** Require the Task 1 command to pass with all 38 entries.

- [ ] **Step 5: Review documentation quality.** Run:

```bash
rg -L "@NonNullApi" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/**/package-info.java
rg -n "(package|Package) for|XX package|通用工具|common utilities" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc \
  --glob 'package-info.java'
git diff --check -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter
```

Expected: the first and second scans print nothing; `git diff --check` succeeds.

- [ ] **Step 6: Commit Task 1.** Stage only the 38 `package-info.java` files and `DdcPackageDocumentationTest.java`, then commit:

```bash
git commit -m "docs(ddc): document starter package contracts"
```

---

### Task 2: Move Public APIs, Models, Format Types, and Errors

**Files:**

- Move client ports:
  - `DDC_MAIN/configuration/client/DdcConfigClient.java` → `DDC_MAIN/api/client/DdcConfigClient.java`
  - `DDC_MAIN/management/DdcManagementClient.java` → `DDC_MAIN/api/client/DdcManagementClient.java`
  - `DDC_MAIN/registry/DdcServiceRegistryClient.java` → `DDC_MAIN/api/client/DdcServiceRegistryClient.java`
- Move public extensions and refresh ports:
  - `DDC_MAIN/configuration/runtime/DdcInstanceIdProvider.java` → `DDC_MAIN/api/extension/DdcInstanceIdProvider.java`
  - `DDC_MAIN/configuration/runtime/DdcInstanceMetadataContributor.java` → `DDC_MAIN/api/extension/DdcInstanceMetadataContributor.java`
  - `DDC_MAIN/configuration/refresh/DdcConfigApplier.java` → `DDC_MAIN/api/refresh/DdcConfigApplier.java`
  - `DDC_MAIN/configuration/refresh/DdcConfigApplierRegistry.java` → `DDC_MAIN/api/refresh/DdcConfigApplierRegistry.java`
  - `DDC_MAIN/registry/DdcRegistrySubscription.java` → `DDC_MAIN/api/registry/DdcRegistrySubscription.java`
- Move model types:
  - `DDC_MAIN/transport/http/DdcClientTransportSecurity.java` and `DDC_MAIN/management/client/DdcManagementClientProperties.java` → `DDC_MAIN/model/client/`
  - All types in `DDC_MAIN/configuration/model/` except `DdcChecksum.java` → `DDC_MAIN/model/config/`
  - `DDC_MAIN/configuration/refresh/DdcConfigurationChangedEvent.java` → `DDC_MAIN/model/config/DdcConfigurationChangedEvent.java`
  - `DDC_MAIN/configuration/runtime/DdcInstanceIdentity.java` and `DdcRuntimeState.java` → `DDC_MAIN/model/instance/`
  - All four types in `DDC_MAIN/lease/` → `DDC_MAIN/model/lease/`
  - All `DdcManagement*.java` records/enums and `DdcInstanceStatus.java` in `DDC_MAIN/management/model/` → `DDC_MAIN/model/management/`
  - All types in `DDC_MAIN/registry/model/` plus `InstanceHealthState.java` and `ServiceInstanceMeta.java` → `DDC_MAIN/model/registry/`
- Move format types:
  - `DDC_MAIN/configuration/model/DdcChecksum.java` → `DDC_MAIN/format/DdcChecksum.java`
  - All three types in `DDC_MAIN/configuration/format/` → `DDC_MAIN/format/`
  - `DDC_MAIN/management/model/ServiceInstanceMetaCodec.java` → `DDC_MAIN/format/ServiceInstanceMetaCodec.java`
- Move error types:
  - Keep `DDC_MAIN/error/DdcErrorStatus.java` and `DdcException.java` in place.
  - `DDC_MAIN/transport/http/DdcOpenApiRequestException.java` → `DDC_MAIN/error/http/DdcOpenApiRequestException.java`
  - `DDC_MAIN/management/client/DdcManagementClientException.java` and `DdcManagementErrorCode.java` → `DDC_MAIN/error/management/`
- Move affected tests to matching target packages: configuration model/format, management contract/model, registry model, and `DdcRuntimeDtoScopeTest`.
- Modify: every repository Java source importing any moved public contract, including DDC Admin/Test, Gateway Admin/Starter/Engine/Provider Runtime/Test, RPC Starter/Test Contract, IdP Admin, and RBAC3 Admin.

**Interfaces:**

- `api` remains implementation-free and exposes exactly the replaceable/callable ports approved by the specification.
- `model` owns cross-module data only; implementation collaborators such as `DdcFieldBinding` remain out of `model`.
- Signatures and serialization annotations remain byte-for-byte equivalent apart from import/package names; no DTO field, enum value, endpoint, error code, validation rule, or JSON name changes.

- [ ] **Step 1: Capture all consumers before moving types.** Save the output in the terminal/log for review; do not create a generated source file:

```bash
rg -l "top\\.egon\\.cola\\.component\\.ddc\\.(configuration\\.(client|model|format|refresh|runtime)|lease|management|registry|transport\\.http)" \
  --glob '*.java' . | sort
```

- [ ] **Step 2: Move the affected tests and change their imports/packages first.** Update `DdcPlatformBoundaryTest` assertions for the public layer:

```java
assertThat(DdcConfigClient.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.api.client");
assertThat(DdcInstanceIdentity.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.model.instance");
assertThat(DdcLeaseSession.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.model.lease");
assertThat(DdcServiceKey.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.model.registry");
assertThat(DdcChecksum.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.format");
```

- [ ] **Step 3: Run test compilation and confirm the expected failure.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -DskipTests \
  test-compile
```

Expected: test compilation fails because the new API/model/format packages do not yet contain the moved production types.

- [ ] **Step 4: Move the production contracts with `git mv`.** Update package declarations, same-module imports and Javadocs. Preserve all modifiers and serialization/validation annotations unless repository-wide usage proves a visibility reduction safe. Preserve the user's existing `DdcLeaseSession.java` edit when moving it.

- [ ] **Step 5: Migrate every repository consumer atomically.** Use the Step 1 list, update imports and fully qualified names in main/test sources, then repeat Step 1 until only production types that have not yet been moved in later tasks remain. Do not add forwarding types in the old packages.

- [ ] **Step 6: Run focused contract/model tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcPlatformBoundaryTest,DdcRuntimeDtoScopeTest,DdcManagementContractBoundaryTest,DdcManagementDtoSerializationTest,DdcInstanceStatusTest,DdcHttpProviderRegistrationTest,DdcServiceRegistrationTest,DdcChecksumTest,ServiceInstanceMetaCodecTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 7: Compile the known downstream consumers.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime,egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin \
  -am \
  -DskipTests \
  test-compile
```

- [ ] **Step 8: Scan for removed public contract packages.** Exclude historical specifications/plans only:

```bash
rg -n "top\\.egon\\.cola\\.component\\.ddc\\.(lease|management\\.(DdcManagementClient|model)|registry\\.(DdcRegistrySubscription|DdcServiceRegistryClient|model)|configuration\\.(client\\.DdcConfigClient|model|format)|transport\\.http\\.DdcClientTransportSecurity)" \
  . \
  --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.yml' --glob '*.yaml' --glob '*.md' \
  --glob '!docs/superpowers/**' --glob '!**/docs/superpowers/**'
```

Expected: no output.

- [ ] **Step 9: Commit Task 2.** Stage only the moved contract/model/format/error files, their tests, and the consumer import updates; verify unrelated Gateway edits are not staged. Commit:

```bash
git commit -m "refactor(ddc): reorganize public api and models"
```

---

### Task 3: Move HTTP Client Adapters and Shared HTTP Support

**Files:**

- Move: `DDC_MAIN/configuration/client/HttpDdcConfigClient.java` → `DDC_MAIN/client/config/HttpDdcConfigClient.java`
- Move: `DDC_MAIN/management/client/HttpDdcManagementClient.java` → `DDC_MAIN/client/management/HttpDdcManagementClient.java`
- Move: `DDC_MAIN/registry/client/HttpDdcServiceRegistryClient.java` → `DDC_MAIN/client/registry/HttpDdcServiceRegistryClient.java`
- Move from `DDC_MAIN/transport/http/` to `DDC_MAIN/client/http/`: `DdcCanonicalRequest.java`, `DdcOpenApiRequestFactory.java`, `DdcRequestSigner.java`, `DdcRestClientFactory.java`.
- Move tests from configuration/management/registry client packages and `transport/http` to matching `client/*` packages.
- Modify: DDC autoconfiguration and all downstream sources directly constructing one of the three HTTP adapters.

**Interfaces:**

- Each implementation remains an Adapter for one `api.client` port; adapters do not depend on each other.
- `client.http` owns only shared HTTP request normalization, signing, TLS/RestClient creation, and request serialization.
- `DdcClientTransportSecurity` remains caller-constructed data in `model.client`; `DdcOpenApiRequestException` remains in `error.http`.

- [ ] **Step 1: Move HTTP tests and update package assertions first.** Add these assertions to `DdcPlatformBoundaryTest`:

```java
assertThat(HttpDdcConfigClient.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.client.config");
assertThat(HttpDdcManagementClient.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.client.management");
assertThat(HttpDdcServiceRegistryClient.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.client.registry");
assertThat(DdcOpenApiRequestFactory.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.client.http");
```

- [ ] **Step 2: Run focused test compilation and confirm the expected failure.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -DskipTests \
  test-compile
```

Expected: imports for `top.egon.cola.component.ddc.client.*` implementations cannot resolve.

- [ ] **Step 3: Move the seven production types.** Update package declarations, imports, constructor references and exception imports. Keep request canonicalization, HMAC header generation, trace propagation, JSON serialization, mTLS, timeouts and domain-error mapping unchanged.

- [ ] **Step 4: Update automatic configuration and downstream constructors.** Search with:

```bash
rg -n "(configuration\\.client\\.HttpDdcConfigClient|management\\.client\\.HttpDdcManagementClient|registry\\.client\\.HttpDdcServiceRegistryClient|transport\\.http\\.(DdcCanonicalRequest|DdcOpenApiRequestFactory|DdcRequestSigner|DdcRestClientFactory))" \
  . --glob '*.java' --glob '*.xml'
```

Update every active result; require the repeated scan to print nothing.

- [ ] **Step 5: Run focused HTTP tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=HttpDdcConfigClientTest,HttpDdcManagementClientTest,HttpDdcServiceRegistryClientTest,DdcOpenApiRequestFactoryTest,DdcRequestSignerTest,DdcPlatformBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 6: Run DDC Admin and Gateway Admin test compilation.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin \
  -am \
  -DskipTests \
  test-compile
```

- [ ] **Step 7: Commit Task 3.** Stage only client moves, HTTP tests, and their active consumers. Commit:

```bash
git commit -m "refactor(ddc): reorganize client adapters"
```

---

### Task 4: Separate ConfigData, Environment, Services, and Runtime State

**Files:**

- Move all four types from `DDC_MAIN/configuration/bootstrap/` → `DDC_MAIN/configdata/`.
- Move both types from `DDC_MAIN/configuration/environment/` → `DDC_MAIN/environment/`.
- Move all four types from `DDC_MAIN/configuration/binding/` → `DDC_MAIN/service/binding/`.
- Move `DdcConfigurationPropertiesRebinder.java`, `DdcRefreshService.java`, `DdcYamlConfigApplier.java`, and `DefaultDdcConfigApplierRegistry.java` from `DDC_MAIN/configuration/refresh/` → `DDC_MAIN/service/refresh/`.
- Move `DdcAckDelivery.java`, `DdcInstanceIdentityFactory.java`, `DdcInstanceService.java`, and `DdcRuntimeCoordinator.java` from `DDC_MAIN/configuration/runtime/` → `DDC_MAIN/service/lifecycle/`.
- Move `DdcLeaseSessionHolder.java` and `DdcLocalConfigState.java` from `DDC_MAIN/configuration/runtime/` → `DDC_MAIN/state/`.
- Move `DDC_MAIN/registry/state/DdcActiveRegistrationIndex.java` → `DDC_MAIN/state/DdcActiveRegistrationIndex.java`.
- Move `DDC_MAIN/registry/DdcServiceKeyFactory.java` and `DDC_MAIN/registry/subscription/DdcRegistrySnapshotLoader.java` → `DDC_MAIN/service/registry/`.
- Move `DDC_MAIN/configuration/runtime/DdcAckDeliveryProperties.java` → `DDC_MAIN/autoconfigure/properties/DdcAckDeliveryProperties.java`.
- Move matching tests for ConfigData, binding, refresh, lifecycle, state and service-registry types.
- Modify: `DDC_RESOURCES/META-INF/spring.factories`, DDC automatic configuration imports, and every downstream source using moved implementation types.

**Interfaces:**

- ConfigData SPI keeps the exact `ddc:` location behavior and continues to be registered through `spring.factories`.
- `service` owns orchestration implementations; it depends inward on `api`/`model` and outward on focused infrastructure, while `api`/`model` never depend on `service`.
- `state` contains mutable in-process state only; moving state must not change locking, atomicity, expiry or lifecycle behavior.
- Keep `DdcLocalConfigState` public because `autoconfigure` constructs it across a package boundary; remove `@Repository` only in Task 6 when the explicit bean is added in the same commit.

- [ ] **Step 1: Move the tests and update ConfigData resource expectations first.** Update package assertions to require:

```java
assertThat(DdcConfigDataFetcher.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.configdata");
assertThat(DdcFieldBindingService.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.service.binding");
assertThat(DdcRuntimeCoordinator.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.service.lifecycle");
assertThat(DdcLocalConfigState.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.state");
```

- [ ] **Step 2: Run test compilation and confirm the expected failure.** Use the Starter `test-compile` command from Task 3. Expected: moved test imports cannot resolve.

- [ ] **Step 3: Move production types and update imports.** Preserve public APIs, constructor signatures, scheduling/lifecycle annotations and algorithms. Do not split methods or introduce new intermediary services as part of the package move.

- [ ] **Step 4: Update `spring.factories` exactly.** The two lines must point to:

```properties
org.springframework.boot.context.config.ConfigDataLocationResolver=top.egon.cola.component.ddc.configdata.DdcConfigDataLocationResolver
org.springframework.boot.context.config.ConfigDataLoader=top.egon.cola.component.ddc.configdata.DdcConfigDataLoader
```

- [ ] **Step 5: Run focused behavior tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcConfigDataFetcherTest,DdcConfigDataLoaderTest,DdcConfigDataLocationResolverTest,DdcConfigDataSpringApplicationTest,DdcFieldBindingServiceTest,DdcConfigurationPropertiesRebinderTest,DdcRefreshServiceTest,DefaultDdcConfigApplierRegistryTest,DdcAckDeliveryTest,DdcInstanceIdentityFactoryTest,DdcLeaseSessionHolderTest,DdcRuntimeCoordinatorTest,DdcActiveRegistrationIndexTest,DdcServiceKeyFactoryTest,DdcPlatformBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 6: Verify ConfigData registration from the built artifact.**

```bash
unzip -p \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/target/egon-cola-platform-dynamic-config-center-starter-*.jar \
  META-INF/spring.factories
```

Expected: only the new `configdata` resolver/loader class names appear; neither `configuration.bootstrap` nor duplicate registrations appear.

- [ ] **Step 7: Scan removed implementation prefixes.**

```bash
rg -n "top\\.egon\\.cola\\.component\\.ddc\\.(configuration\\.(bootstrap|binding|environment|refresh|runtime)|registry\\.state|registry\\.subscription\\.DdcRegistrySnapshotLoader|registry\\.DdcServiceKeyFactory)" \
  . \
  --glob '*.java' --glob '*.properties' --glob '*.xml' --glob '*.md' \
  --glob '!docs/superpowers/**' --glob '!**/docs/superpowers/**'
```

Expected: no references to the moved types. `configuration.subscription` remains until Task 5.

- [ ] **Step 8: Commit Task 4.** Stage only ConfigData/environment/service/state/property moves, resources, tests and their consumers. Commit:

```bash
git commit -m "refactor(ddc): separate services and runtime state"
```

---

### Task 5: Separate Redis Infrastructure and Event Listeners

**Files:**

- Move all three types from `DDC_MAIN/transport/redis/` → `DDC_MAIN/redis/`.
- Move `DDC_MAIN/configuration/subscription/DdcConfigChangeListener.java` → `DDC_MAIN/listener/config/DdcConfigChangeListener.java`.
- Move `DdcCatalogSubscription.java`, `DdcInstanceSubscription.java`, `DdcManagedRegistrySubscription.java`, and `DdcRegistrySubscriptionCoordinator.java` from `DDC_MAIN/registry/subscription/` → `DDC_MAIN/listener/registry/`.
- Move matching Redis and listener tests to the target packages.
- Modify: DDC automatic configuration, DDC Admin Redis-key consumers, RPC/Gateway registry consumers, and any tests importing the old packages.

**Interfaces:**

- `redis` owns Redisson connection creation, key naming and generic Topic resource handles only; it has no configuration or registry business rules.
- Configuration and registry listeners may share `DdcRedisTopicSubscription`, but retain separate event filtering, refresh/reconciliation, listener isolation and local-expiry semantics.
- `DdcRegistrySnapshotLoader` remains in `service.registry`; listener code depends on that read-only collaborator instead of moving snapshot loading into Redis infrastructure.

- [ ] **Step 1: Move tests first and strengthen role assertions.** Add:

```java
assertThat(DdcRedisClientFactory.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.redis");
assertThat(DdcConfigChangeListener.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.listener.config");
assertThat(DdcRegistrySubscriptionCoordinator.class.getPackageName())
        .isEqualTo("top.egon.cola.component.ddc.listener.registry");
```

- [ ] **Step 2: Run Starter test compilation and confirm the expected failure.** Expected: new `redis` and `listener` imports cannot resolve.

- [ ] **Step 3: Move production types and update imports.** Preserve Redisson address/auth/TLS handling, key strings, Topic registration rollback, idempotent close, event coalescing, periodic reconciliation and listener-exception isolation.

- [ ] **Step 4: Run focused Redis/listener/registry tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcRedisClientFactoryTest,DdcRedisKeysTest,DdcRedisTopicSubscriptionTest,DdcRegistrySubscriptionCoordinatorTest,DdcActiveRegistrationIndexTest,HttpDdcServiceRegistryClientTest,DdcPlatformBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 5: Compile the main registry consumers.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime \
  -am \
  -DskipTests \
  test-compile
```

- [ ] **Step 6: Scan removed Redis/listener prefixes.**

```bash
rg -n "top\\.egon\\.cola\\.component\\.ddc\\.(transport\\.redis|configuration\\.subscription|registry\\.subscription)" \
  . \
  --glob '*.java' --glob '*.properties' --glob '*.xml' --glob '*.md' \
  --glob '!docs/superpowers/**' --glob '!**/docs/superpowers/**'
```

Expected: no output.

- [ ] **Step 7: Commit Task 5.** Stage only Redis/listener moves, tests and consumers. Commit:

```bash
git commit -m "refactor(ddc): separate redis and listeners"
```

---

### Task 6: Make Starter Automatic Configuration Explicit

**Files:**

- Move: `DDC_MAIN/autoconfigure/DdcProperties.java` → `DDC_MAIN/autoconfigure/properties/DdcProperties.java`.
- Rename: `DDC_MAIN/autoconfigure/DdcAutoConfig.java` → `DDC_MAIN/autoconfigure/DdcAutoConfiguration.java`.
- Rename: `DDC_MAIN/autoconfigure/DdcRedisAutoConfig.java` → `DDC_MAIN/autoconfigure/DdcRedisAutoConfiguration.java`.
- Rename: `DDC_MAIN/autoconfigure/DdcRegistryAutoConfig.java` → `DDC_MAIN/autoconfigure/DdcRegistryAutoConfiguration.java`.
- Rename `DDC_TEST/autoconfigure/DdcAutoConfigTest.java`, `DdcRedisAutoConfigTest.java`, and `DdcRegistryAutoConfigTest.java` to their `*AutoConfigurationTest.java` names; move `DDC_TEST/autoconfigure/DdcPropertiesTest.java` to `DDC_TEST/autoconfigure/properties/DdcPropertiesTest.java`.
- Modify: `DDC_RESOURCES/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Modify: `DDC_MAIN/state/DdcLocalConfigState.java` and all automatic configuration bean methods/imports.
- Modify: every repository source importing `DdcProperties` or an old `*AutoConfig` class.

**Interfaces:**

- The Maven artifact remains named Starter; `autoconfigure` is an internal package, never a separate module.
- `DdcAutoConfiguration`, `DdcRedisAutoConfiguration` and `DdcRegistryAutoConfiguration` are the only entries in `AutoConfiguration.imports`.
- No `@ComponentScan` discovers Starter beans; every runtime collaborator is created by an explicit `@Bean` or an exact `@Import`.
- Default replaceable ports retain `@ConditionalOnMissingBean`; configuration, Redis and Registry activation conditions remain independent.

- [ ] **Step 1: Rename tests and add failing explicit-wiring assertions.** Assert the new class names resolve, old names do not, and no automatic configuration class has `@ComponentScan`. In the existing `ApplicationContextRunner` tests, require exactly one `DdcLocalConfigState` bean when DDC is enabled and none when disabled:

```java
assertThat(DdcAutoConfiguration.class.isAnnotationPresent(ComponentScan.class)).isFalse();
assertThat(DdcRedisAutoConfiguration.class.isAnnotationPresent(ComponentScan.class)).isFalse();
assertThat(DdcRegistryAutoConfiguration.class.isAnnotationPresent(ComponentScan.class)).isFalse();

contextRunner.run(context -> assertThat(context)
        .hasSingleBean(DdcLocalConfigState.class));
```

- [ ] **Step 2: Run focused tests and confirm the expected failure.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcAutoConfigurationTest,DdcRedisAutoConfigurationTest,DdcRegistryAutoConfigurationTest,DdcPropertiesTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Expected: renamed automatic configuration classes are absent and the old main configuration still carries `@ComponentScan`.

- [ ] **Step 3: Rename production classes and property package.** Update logger class literals, configuration references, `@EnableConfigurationProperties`, downstream imports and test imports. Do not rename property keys.

- [ ] **Step 4: Replace component scanning with explicit beans.** Remove `@ComponentScan` from all three automatic configurations and remove `@Repository` from `DdcLocalConfigState`. Add the state bean with replacement support:

```java
@Bean
@ConditionalOnMissingBean
public DdcLocalConfigState ddcLocalConfigState() {
    return new DdcLocalConfigState();
}
```

Audit every type previously discovered by scanning using the actual bean graph. Add an explicit `@Bean` or exact `@Import` only for a real runtime bean; do not annotate service/model/state classes merely to restore scanning.

- [ ] **Step 5: Replace `AutoConfiguration.imports` contents exactly.**

```text
top.egon.cola.component.ddc.autoconfigure.DdcRedisAutoConfiguration
top.egon.cola.component.ddc.autoconfigure.DdcAutoConfiguration
top.egon.cola.component.ddc.autoconfigure.DdcRegistryAutoConfiguration
```

- [ ] **Step 6: Run all automatic configuration tests.** Use the Task 6 focused command, then run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest='top.egon.cola.component.ddc.autoconfigure.**,top.egon.cola.component.ddc.autoconfigure.properties.**' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 7: Verify the built registration resource and annotation removal.**

```bash
unzip -p \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/target/egon-cola-platform-dynamic-config-center-starter-*.jar \
  META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
rg -n "@(ComponentScan|Repository)" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc
rg -n "(DdcAutoConfig|DdcRedisAutoConfig|DdcRegistryAutoConfig)([^u]|$)" \
  . \
  --glob '*.java' --glob '*.properties' --glob '*.xml' --glob '*.md' \
  --glob '!docs/superpowers/**' --glob '!**/docs/superpowers/**'
```

Expected: the jar resource has exactly the three new names; both `rg` scans print nothing.

- [ ] **Step 8: Commit Task 6.** Stage only automatic configuration/property moves, explicit wiring, tests, registration resource and active consumers. Commit:

```bash
git commit -m "refactor(ddc): make starter auto configuration explicit"
```

---

### Task 7: Enforce Final Package Boundaries, Nullability, and Repository Completion

**Files:**

- Modify: `DDC_TEST/DdcPlatformBoundaryTest.java`.
- Modify: `DDC_TEST/DdcPackageDocumentationTest.java`.
- Modify: every migrated DDC production type whose parameter or return value is intentionally nullable.
- Modify: package-specific `package-info.java` files if visibility/package-private findings from the completed moves require more precise documentation.
- Modify: active README, examples and configuration documentation containing removed package names or old automatic configuration class names; historical files under `docs/superpowers/specs/` and `docs/superpowers/plans/` remain unchanged.

**Interfaces:**

- Final top-level production packages are exactly: `annotation`, `api`, `model`, `client`, `service`, `listener`, `state`, `redis`, `configdata`, `environment`, `format`, `observability`, `error`, `autoconfigure`.
- Final full package list is exactly the 38 entries owned by `DdcPackageDocumentationTest`.
- All method parameters and return values are non-null by default; intentional nulls have local `@Nullable` annotations matching behavior.
- No `configuration`, top-level `lease`, `management`, `registry`, `transport`, or mixed `runtime` production package remains.

- [ ] **Step 1: Add failing final-boundary and nullability assertions.** Strengthen `DdcPlatformBoundaryTest` to compare the exact top-level directory list:

```java
assertThat(topLevelPackages).containsExactly(
        "annotation",
        "api",
        "autoconfigure",
        "client",
        "configdata",
        "environment",
        "error",
        "format",
        "listener",
        "model",
        "observability",
        "redis",
        "service",
        "state"
);
```

Add a recursive source-directory assertion that every directory containing Java sources is one of the 38 approved packages and contains `package-info.java`. Add reflection assertions for the known nullable `DdcLocalConfigState` contract:

```java
assertThat(DdcLocalConfigState.class.getMethod("version", String.class)
        .getAnnotation(Nullable.class)).isNotNull();
assertThat(DdcLocalConfigState.class.getMethod("checksum", String.class)
        .getAnnotation(Nullable.class)).isNotNull();
assertThat(DdcLocalConfigState.class
        .getMethod("updateChecksum", String.class, String.class)
        .getParameterAnnotations()[1])
        .anyMatch(annotation -> annotation.annotationType() == Nullable.class);
assertThat(DdcLocalConfigState.class
        .getMethod("restoreMetadata", String.class, Long.class, String.class)
        .getParameterAnnotations()[1])
        .anyMatch(annotation -> annotation.annotationType() == Nullable.class);
assertThat(DdcLocalConfigState.class
        .getMethod("restoreMetadata", String.class, Long.class, String.class)
        .getParameterAnnotations()[2])
        .anyMatch(annotation -> annotation.annotationType() == Nullable.class);
```

- [ ] **Step 2: Run the focused tests and confirm the expected failure.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcPlatformBoundaryTest,DdcPackageDocumentationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Expected: the nullable reflection assertions fail until explicit `@Nullable` annotations are added; any stale directory/package also fails structurally.

- [ ] **Step 3: Complete the nullability audit.** Start with explicit source evidence:

```bash
rg -n "return null;|== null|!= null|@Nullable|orElse\(null\)|getOrDefault\([^,]+, null\)" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc \
  --glob '*.java'
```

For each match, distinguish internal null checks from public/package-private nullable contracts. Add `org.springframework.lang.Nullable` only to parameters/returns that are legitimately nullable. At minimum annotate `DdcLocalConfigState.version`, `checksum`, the `checksum` parameter of `updateChecksum`, and both nullable metadata parameters of `restoreMetadata`. Preserve behavior and do not convert the API to `Optional`.

- [ ] **Step 4: Audit visibility and package-private documentation.** Use repository-wide type-name searches before reducing visibility. Keep types public when automatic configuration constructs them across packages or downstream modules use them. Reduce a type only when all callers are in its package, keep it in a same-named file, and update that package's bilingual `package-info.java` to name the important internal collaborator. Do not create empty shared helper/constant classes.

- [ ] **Step 5: Update active documentation and examples.** Locate active references:

```bash
rg -n "top\\.egon\\.cola\\.component\\.ddc\\.(configuration|lease|management|registry|transport|runtime)|Ddc(AutoConfig|RedisAutoConfig|RegistryAutoConfig)" \
  . \
  --glob 'README*' --glob '*.md' --glob '*.adoc' --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.yml' --glob '*.yaml' \
  --glob '!docs/superpowers/**' --glob '!**/docs/superpowers/**'
```

Update every active example/import/resource result. Do not rewrite historical design or implementation-plan files.

- [ ] **Step 6: Run the complete Starter test suite.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  test
```

- [ ] **Step 7: Run affected DDC and downstream verification without starting processes.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-rpc-contract,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-rpc-provider,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-rpc-consumer,egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin \
  -am \
  test
```

If a module fails for a pre-existing unrelated reason, record the exact command, failing module and error; do not weaken the DDC tests or silently omit the module. Run a narrower `-DskipTests test-compile` only as additional evidence, not as a replacement for a requested relevant test.

- [ ] **Step 8: Run final structural and residual scans.**

```bash
find \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc \
  -type d -empty -print

rg -n "^package top\\.egon\\.cola\\.component\\.ddc\\.(configuration|lease|management|registry|transport|runtime)(\\.|;)" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java \
  --glob '*.java'

rg -n "top\\.egon\\.cola\\.component\\.ddc\\.(configuration|lease|management|registry|transport|runtime)(\\.|;)|Ddc(AutoConfig|RedisAutoConfig|RegistryAutoConfig)([^u]|$)" \
  . \
  --glob '*.java' --glob '*.xml' --glob '*.properties' --glob '*.yml' --glob '*.yaml' --glob '*.md' \
  --glob '!docs/superpowers/**' --glob '!**/docs/superpowers/**'

rg -n "@(ComponentScan|Repository)" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc

git diff --check
git status --short
```

Expected: no empty directories, old production package declarations, active old imports/class names, `@ComponentScan`, `@Repository`, or whitespace errors. `git status` may still show explicitly preserved unrelated user changes, but no uncommitted Task 7 file.

- [ ] **Step 9: Inspect module and dependency boundaries.** Confirm no new `pom.xml` exists, no DDC dependency was added, the BOM still exposes the same Starter artifact, and no production type imports Admin/Test packages.

- [ ] **Step 10: Commit Task 7.** Stage only boundary/nullability/documentation changes and explicitly exclude unrelated dirty files. Commit:

```bash
git commit -m "docs(ddc): enforce starter package boundaries"
```

---

## Completion Gate

- [ ] The implementation matches all type locations in the approved target tree.
- [ ] The Starter remains the single Maven consumption artifact; no new module or dependency exists.
- [ ] All 38 packages have specific Chinese-first/English-second `package-info.java` documentation and their own `@NonNullApi` declaration.
- [ ] Every intentional nullable parameter/return has `@Nullable`; no field-level non-null default or new nullability dependency was introduced.
- [ ] `api`, `model`, `client`, `service`, `listener`, `state`, `redis`, `configdata`, `environment`, `format`, `observability`, `error`, `annotation`, and `autoconfigure` have the approved one-way responsibilities.
- [ ] No top-level `configuration`, `lease`, `management`, `registry`, `transport`, or mixed `runtime` package remains.
- [ ] No old-package compatibility shell, deprecated forwarding class, old automatic configuration name, `@ComponentScan`, or state `@Repository` remains.
- [ ] `AutoConfiguration.imports` lists exactly the three `*AutoConfiguration` classes and `spring.factories` points exactly to `configdata` SPI types.
- [ ] ConfigData/YAML-only, field/configuration-properties refresh, lifecycle, Registry, Redis, HMAC/mTLS and tracing behavior is covered by unchanged or moved focused tests.
- [ ] DDC Admin/Test and all identified Gateway, RPC, IdP and RBAC3 consumers compile against the new Starter API.
- [ ] Active documentation/examples/resources contain no stale package or class name; historical specs/plans remain historical.
- [ ] No database/Flyway file changed and no application/runtime process was started.
- [ ] Every implementation task is committed separately with path-limited staging, and all pre-existing unrelated worktree changes remain intact.
