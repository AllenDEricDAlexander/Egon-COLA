# DDC Spring Boot ConfigData YAML-only Implementation Plan

**Goal:** Replace DDC's per-key runtime configuration with one validated `application.yml` ConfigData resource, preserve Spring Boot precedence, and apply only explicitly refreshable consumers at runtime.

**Architecture:** The Starter adapts the existing signed HTTP pull contract to Spring Boot's `ConfigDataLocationResolver` and `ConfigDataLoader`. The official `YamlPropertySourceLoader` creates one dynamic PropertySource whose immutable snapshot is atomically replaced on publication. YAML leaf diffs drive the existing Applier registry and `@DdcValue`, while setter-based `@DdcRefreshable` configuration properties are rebound explicitly. Admin owns one YAML record per scope and validates every mutation boundary; Gateway updates rule leaves inside that YAML document.

**Patterns:** Use Adapter for ConfigData and official YAML loading, Strategy/Registry for leaf consumers, and Observer for post-apply change events. Do not add factories, inheritance hierarchies, Spring Cloud Context, or a second configuration merge model.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven, JUnit 5, React 19, Ant Design 6, Vitest.

## Global Constraints

- Work on the current `main` checkout and preserve all pre-existing Admin/IdP changes.
- Stage and commit only task-owned paths; each task gets one commit.
- Support only one single-document YAML resource named `application.yml` with value type `YAML`.
- Do not migrate data or edit existing Flyway migrations; existing storage columns may hold constants.
- Do not start any application or open a browser.
- Add no Spring Cloud refresh dependency and do not restart the ApplicationContext.
- Remote DDC/client/config-import/profile keys must fail the whole document in Starter and Admin.

## Task 1: Native ConfigData Bootstrap

**Files:**

- Add `bootstrap/DdcConfigDataLocationResolver.java`.
- Add `bootstrap/DdcConfigDataResource.java`.
- Add `bootstrap/DdcConfigDataLoader.java`.
- Add `bootstrap/DdcBootstrapClient.java`.
- Add `environment/DdcDynamicPropertySource.java`.
- Add `environment/DdcYamlPropertySourceLoader.java`.
- Add `environment/DdcReservedConfigurationKeys.java`.
- Add `META-INF/spring.factories` and focused Starter tests.

**Steps:**

- [x] Register the Resolver and Loader through the ConfigData SPI and parse only `ddc:application.yml`.
- [x] Bind local DDC bootstrap settings through the Resolver context without remote access in `isResolvable`.
- [x] Pull exactly one published YAML value using the existing HTTP/HMAC/TLS contract.
- [x] Parse through Boot's official loader, reject multiple/root-scalar/reserved-key documents, and return a dynamic PropertySource with `IGNORE_IMPORTS` and `IGNORE_PROFILES`.
- [x] Prove startup binding, optional semantics, source ordering, local fallback, and non-ConfigData override precedence with focused tests.
- [x] Run Starter tests and commit as `feat(ddc): load remote YAML through ConfigData`.

## Task 2: YAML Refresh and Compatibility Consumers

**Files:**

- Add `annotation/DdcRefreshable.java`.
- Add `refresh/DdcYamlConfigApplier.java`.
- Add `refresh/DdcConfigurationPropertiesRebinder.java`.
- Add `refresh/DdcConfigurationChangedEvent.java`.
- Modify `service/DdcRefreshService.java`, `DdcRuntimeCoordinator.java`, AutoConfiguration, listeners, field binding, and focused tests.
- Remove default-report DTO/client/controller runtime usage.

**Steps:**

- [x] Route only `application.yml` publications through one version/checksum/ACK path.
- [x] Atomically switch the YAML snapshot, compute added/updated/removed leaves, and restore the old snapshot on synchronous apply failure.
- [x] Dispatch changed leaf values through exact/prefix/fallback Appliers so Gateway, IdP, and `@DdcValue` keep their domain behavior.
- [x] Rebind only setter-based `@DdcRefreshable` configuration properties whose prefix changed; classify all other changed keys as restart-required.
- [x] Publish a value-free change event after successful application and ACK accepted non-dynamic changes as `SUCCESS`.
- [x] Remove default reporting from runtime coordination and prove refresh, rollback, idempotence, event classification, and compatibility with focused tests.
- [x] Run Starter and DDC consumer tests and commit as `feat(ddc): refresh YAML configuration by property leaf`.

## Task 3: Admin YAML-only Contract and Validation

**Files:**

- Modify Admin config DTO/VO/controller/service/publish validation and OpenAPI pull/controller.
- Add a shared Admin YAML validator using Boot's official loader adapter rules.
- Modify management client models and focused Admin/management-client tests.

**Steps:**

- [x] Make `application.yml` and `YAML` server-owned constants; remove caller-controlled key/type/default fields.
- [x] Enforce one config document per scope and return at most one published runtime document.
- [x] Validate UTF-8 size, one Map-root YAML document, and reserved keys on create, upsert, update, rollback, and publish.
- [x] Delete `/defaults/report` and related client calls while keeping registration, pull, ACK, topic, and publish state semantics intact.
- [x] Run Admin and management-client tests and commit as `refactor(ddc): enforce YAML-only admin configuration`.

## Task 4: Gateway YAML Publication

**Files:**

- Modify Gateway Admin publication coordinator/publisher and their tests.
- Keep Gateway Engine Applier API unchanged; add compatibility tests where necessary.

**Steps:**

- [x] Resolve the current `application.yml` document and version for every release phase.
- [x] Parse the document, replace one `gateway.rules.chunk.*` or `gateway.rules.active` leaf, serialize valid YAML, and publish the document key.
- [x] Preserve release journal leaf keys and change IDs while querying/retrying DDC tasks by their existing identities.
- [x] Prove chunk ordering, conflict recovery, retry, inline activation, and Engine leaf dispatch.
- [x] Run affected Gateway Admin/Engine tests and commit as `refactor(gateway): publish rules through DDC YAML`.

## Task 5: Admin Web YAML Editor and Final Verification

**Files:**

- Modify DDC Admin Web config API types, list/editor pages, format utilities, and tests.
- Update DDC README/sample configuration only where the old scalar/default-report contract remains documented.

**Steps:**

- [ ] Replace key/type/default controls with one YAML editor for the selected biz/env/app scope.
- [ ] Keep scope selection, optimistic version update, publish, rollback, and server error feedback.
- [ ] Remove JSON/TOML/TXT/Gateway-inline editor adapters that are no longer reachable.
- [ ] Run Admin Web unit tests, typecheck/lint/build without browser or E2E startup.
- [ ] Run focused DDC Starter/Admin/Test and affected Gateway/IdP Maven suites.
- [ ] Run `git diff --check`, verify only requested paths were committed, and commit as `refactor(ddc): provide YAML-only admin editor`.

## Completion Gate

- [ ] Initial remote YAML binds before Bean creation.
- [ ] DDC wins over local ConfigData but not Boot's higher-order non-ConfigData sources.
- [ ] DDC cannot overwrite its own bootstrap/config/profile controls.
- [ ] Runtime refresh is atomic and accurately separates refreshed from restart-required keys.
- [ ] Admin, Admin Web, Gateway publication, `@DdcValue`, and custom Appliers all use the YAML-only model.
- [ ] No application was started and all validation commands/results are recorded.
