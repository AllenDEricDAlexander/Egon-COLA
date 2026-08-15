# RBAC3 IAM and DDC Business/Application Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Move RBAC3 management capabilities into the IAM package and API model while making DDC the only Business/Application master-data writer and retaining all User/Role authorization in RBAC.

**Architecture:** DDC keeps the Biz -> App catalog and exposes only four read operations through the existing Management RPC chain. RBAC adds one typed IAM Business adapter over that client, stores a tenant-local Application authorization scope plus User Business grants, and derives Application access from existing UserRoleAssignment records. Java packages, REST URI, Gateway/Manifest declarations, and RBAC Admin Web routes move together with no compatibility aliases.

**Tech Stack:** Java, Spring Boot, Spring MVC, Spring Data JPA, Flyway/PostgreSQL, protobuf/gRPC, Maven, React, TypeScript, React Query, Vite, Vitest.

## Global Constraints

- This is destructive: do not keep old Java packages, REST URIs, front-end routes, Gateway aliases, or old-data compatibility adapters.
- Create exactly one new RBAC migration: V6__adopt_ddc_business_application_authorization_scope.sql. Do not edit V1 through V5.
- DDC remains the sole writer of ddc_biz and ddc_app. RBAC uses DDC Management RPC only, never DDC REST writes or direct DDC database access.
- RBAC owns UserBusinessAccess and all User/Role/Permission authorization changes. Do not create a UserApplicationAccess table; UserRoleAssignment -> Role.applicationId remains the Application authorization source of truth.
- ApplicationPO.id remains RBAC's local key. Add ddcBusinessId and ddcApplicationId; use local applicationId, not globally unique applicationCode, for RBAC relationships.
- FIELD remains FieldDefinitionPO; do not add FIELD to ResourceTypeEnum.
- Directory snapshot records remain read-only. Manual Organization/Position records use an explicit source type and never fabricate snapshotId.
- A missing, disabled, or RPC-unavailable DDC catalog record never creates a grant or an effective authorization context. Fail closed.
- Active roles continue to select which otherwise-effective roles fill the permission context. Inactive roles are not backfilled.
- Do not redesign IdP, JWT, Refresh Token, Session, SSO, Gateway authentication, or automatic data/field PEP enforcement.
- Do not add a generic relation controller, Factory, Strategy, or rule engine. DdcCatalogGateway is the one narrow external-boundary adapter; add no DDC catalog cache in this migration.
- Preserve unrelated worktree changes, stage paths narrowly, and do not start services.

---

## Scope and file map

This is one plan because the DDC read contract is required by Application scope admission, User Business authorization, effective-role calculation, and Admin Web selectors. Splitting it would create an incomplete authorization path.

| Area | Primary paths | Final responsibility |
| --- | --- | --- |
| DDC starter | egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc | Typed read-only catalog records and DdcManagementClient methods. |
| DDC RPC adapter | egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter | Proto, generated contract, mapper, client, and catalog-read operation authorization. |
| DDC provider | egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin | Serves catalog facts through existing DdcBizService and DdcAppService. |
| RBAC database | egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration | One V6 migration for scope IDs, Business grants, source fields, and manual memberships. |
| RBAC IAM | egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam | Moved domains: tenant, user, business, application, resource, permission, role, organization, position, policy. |
| RBAC UI | egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/iam | IAM routes, typed clients, CRUD, and authorization flows. |
| DDC UI | egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/BizsPage.tsx and AppsPage.tsx | The only Business/Application master-data CRUD UI. |

## Names and contracts locked by this plan

~~~java
// DDC starter model package: top.egon.cola.component.ddc.model.management
public record DdcManagementBiz(String id, String bizCode, String bizName, boolean enabled) {}
public record DdcManagementApp(
        String id, String businessId, String bizCode,
        String appCode, String appName,
        boolean enabled, boolean businessEnabled) {}
public record DdcManagementBizLookup(String id, String bizCode) {}
public record DdcManagementBizQuery(String keyword, Boolean enabled) {}
public record DdcManagementAppQuery(
        String businessId, String bizCode, String keyword, Boolean enabled) {}

// DdcManagementClient additions
Optional<DdcManagementBiz> getBiz(DdcManagementBizLookup lookup);
List<DdcManagementBiz> listBizs(DdcManagementBizQuery query);
Optional<DdcManagementApp> getApp(String ddcApplicationId);
List<DdcManagementApp> listApps(DdcManagementAppQuery query);
~~~

~~~java
// RBAC package: top.egon.cola.platform.rbac3.admin.iam.business.service
public interface DdcCatalogGateway {
    Optional<BusinessCatalogEntry> findBusiness(String ddcBusinessId);
    List<BusinessCatalogEntry> listBusinesses(String keyword);
    Optional<ApplicationCatalogEntry> findApplication(String ddcApplicationId);
    List<ApplicationCatalogEntry> listApplications(String ddcBusinessId, String keyword);
}

public record BusinessCatalogEntry(
        String ddcBusinessId, String bizCode, String bizName, boolean enabled) {}
public record ApplicationCatalogEntry(
        String ddcApplicationId, String ddcBusinessId, String bizCode,
        String appCode, String appName, boolean applicationEnabled,
        boolean businessEnabled) {}
~~~

~~~text
GET  /api/rbac3/v1/iam/catalog/businesses
GET  /api/rbac3/v1/iam/catalog/businesses/{ddcBusinessId}/applications
POST /api/rbac3/v1/iam/applications:admit
GET  /api/rbac3/v1/iam/applications
GET  /api/rbac3/v1/iam/applications/{applicationId}
PUT  /api/rbac3/v1/iam/applications/{applicationId}/status
DELETE /api/rbac3/v1/iam/applications/{applicationId}
GET  /api/rbac3/v1/iam/users/{userId}/business-accesses
PUT  /api/rbac3/v1/iam/users/{userId}/business-accesses
GET  /api/rbac3/v1/iam/users/{userId}/application-accesses
~~~

### Task 1: Add the typed, read-only DDC catalog RPC contract

**Files:**

- Create: DdcManagementBiz.java, DdcManagementApp.java, DdcManagementBizLookup.java, DdcManagementBizQuery.java, and DdcManagementAppQuery.java under the DDC starter management model package.
- Modify: DdcManagementClient.java in the DDC starter.
- Modify: ddc_management.proto, DdcManagementRpc.java, RpcDdcManagementClient.java, DdcManagementProtoMapper.java, DdcRpcOperation.java, and DdcRpcOperationResolver.java in egon-cola-component-rpc-ddc-adapter.
- Test: DdcManagementProtoMapperTest.java, RpcDdcManagementClientTest.java, and DdcRpcContractDescriptorTest.java in the same adapter module.

**Consumes:** Existing DdcManagementClient, DdcManagementRpc, mapper, and credential-scoped DdcRpcOperation infrastructure.

**Produces:** Four unary RPC operations, GetBiz, ListBizs, GetApp, and ListApps, all authorized as MANAGEMENT_CATALOG_READ. No catalog write operation is introduced.

- [ ] **Step 1: Write failing mapping and operation-resolution tests.**

~~~java
@Test
void mapsApplicationWithParentBusinessFacts() {
    DdcManagementApp value = new DdcManagementApp(
            "app-1", "biz-1", "orders", "console", "Console", true, true);

    assertThat(mapper.fromApp(mapper.toApp(value))).isEqualTo(value);
}

@Test
void resolvesCatalogMethodsAsReadOnlyManagementOperations() {
    assertThat(resolver.operationsByBareMethod()).containsEntry(
            "GetBiz", DdcRpcOperation.MANAGEMENT_CATALOG_READ);
    assertThat(resolver.operationsByBareMethod()).containsEntry(
            "ListApps", DdcRpcOperation.MANAGEMENT_CATALOG_READ);
}
~~~

- [ ] **Step 2: Run the focused tests and confirm the model and RPC methods do not exist.**

Run:

~~~bash
mvn -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter -am \
  -Dtest=DdcManagementProtoMapperTest,DdcRpcContractDescriptorTest,RpcDdcManagementClientTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: test compilation fails on the missing catalog records and methods.

- [ ] **Step 3: Define the end-to-end read contract.**

Add this service shape to ddc_management.proto:

~~~proto
rpc GetBiz(GetBizRequest) returns (GetBizResponse);
rpc ListBizs(ListBizsRequest) returns (ListBizsResponse);
rpc GetApp(GetAppRequest) returns (GetAppResponse);
rpc ListApps(ListAppsRequest) returns (ListAppsResponse);

message ManagementBiz {
  string id = 1;
  string biz_code = 2;
  string biz_name = 3;
  bool enabled = 4;
}

message ManagementApp {
  string id = 1;
  string business_id = 2;
  string biz_code = 3;
  string app_code = 4;
  string app_name = 5;
  bool enabled = 6;
  bool business_enabled = 7;
}
~~~

DdcManagementBizLookup accepts exactly one nonblank selector, id or bizCode. DdcManagementAppQuery accepts exactly one nonblank Business selector, businessId or bizCode. RpcDdcManagementClient maps found=false to Optional.empty and invokes every catalog method as MANAGEMENT_CATALOG_READ.

- [ ] **Step 4: Verify absent catalog facts and invalid selectors fail safely.**

~~~java
assertThat(client.getBiz(new DdcManagementBizLookup("missing", null))).isEmpty();
assertThat(client.getApp("missing")).isEmpty();
assertThatThrownBy(() -> client.listApps(
        new DdcManagementAppQuery(null, null, null, Boolean.TRUE)))
        .isInstanceOf(IllegalArgumentException.class);
~~~

- [ ] **Step 5: Re-run the focused tests and commit.**

Expected: PASS.

~~~bash
git add \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/api/client/DdcManagementClient.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/management/DdcManagementBiz.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/management/DdcManagementApp.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/management/DdcManagementBizLookup.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/management/DdcManagementBizQuery.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/management/DdcManagementAppQuery.java \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main
git add -p egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/test
git commit -m "feat(ddc-rpc): expose read-only business catalog"
~~~

### Task 2: Serve the catalog from DDC without changing DDC REST CRUD ownership

**Files:**

- Modify: DdcManagementRpcProvider.java in the DDC Admin module.
- Modify: DdcBizRepository.java, DdcAppRepository.java, DdcBizService.java, and DdcAppService.java in the DDC Admin module.
- Test: DdcManagementRpcProviderTest.java, DdcBizControllerTest.java, and DdcAppControllerTest.java.

**Consumes:** Task 1 generated proto/client types and the existing DDC master-data services.

**Produces:** Catalog facts from DdcBizService and DdcAppService. The REST ownership of /api/v1/ddc/bizs and /api/v1/ddc/apps does not change.

- [ ] **Step 1: Add provider tests for immutable ID lookup, Business-filtered App listing, and parent status.**

~~~java
@Test
void getAppIncludesParentBusinessIdentityAndStatus() {
    when(appService.findById("app-1")).thenReturn(Optional.of(app("app-1", "orders", true)));
    when(bizService.findByBizCode("orders")).thenReturn(biz("biz-1", "orders", false));

    GetAppResponse response = provider.getApp(
            GetAppRequest.newBuilder().setDdcApplicationId("app-1").build());

    assertThat(response.getFound()).isTrue();
    assertThat(response.getApp().getBusinessId()).isEqualTo("biz-1");
    assertThat(response.getApp().getBusinessEnabled()).isFalse();
}
~~~

- [ ] **Step 2: Run DDC provider/controller tests and confirm catalog methods are absent.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcManagementRpcProviderTest,DdcBizControllerTest,DdcAppControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: compilation fails on getBiz, listBizs, getApp, and listApps.

- [ ] **Step 3: Add narrow read methods and provider mappings.**

Use these service signatures:

~~~java
// DdcBizService
public Optional<DdcBizEntity> findById(String id);
public List<DdcBizEntity> list(String keyword, Boolean enabled);

// DdcAppService
public List<DdcAppEntity> list(String bizCode, String keyword, Boolean enabled);
~~~

DdcManagementRpcProvider.getApp loads the App by immutable DDC id, then its parent Business by the App's stored bizCode. It returns found=false if the App or its parent is missing. ListApps first resolves the selected Business and returns an empty list for a nonexistent selector. Every RPC method calls DdcServicePrincipal.current. No catalog method invokes save, update, delete, or setEnabled.

- [ ] **Step 4: Preserve the DDC REST contract in controller tests.**

~~~java
mockMvc.perform(post("/api/v1/ddc/bizs").contentType(APPLICATION_JSON).content(validBizJson))
        .andExpect(status().isOk());
mockMvc.perform(put("/api/v1/ddc/apps/{id}/enabled", "app-1")
        .contentType(APPLICATION_JSON).content(enabledJson))
        .andExpect(status().isOk());

verify(bizService, never()).save(any());
verify(appService, never()).setEnabled(anyString(), anyBoolean());
~~~

The never checks belong to a provider test; the controller test retains its existing write expectations.

- [ ] **Step 5: Re-run the DDC suite and commit.**

Expected: PASS.

~~~bash
git add \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/rpc/provider/DdcManagementRpcProvider.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcBizRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcAppRepository.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcBizService.java \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcAppService.java
git add -p egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test
git commit -m "feat(ddc): serve business catalog over management rpc"
~~~

### Task 3: Add the single RBAC V6 migration and persistence primitives

**Files:**

- Create: egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V6__adopt_ddc_business_application_authorization_scope.sql.
- Modify before the package move: ApplicationPO.java, OrgUnitPO.java, and PositionPO.java.
- Create before the package move: UserBusinessAccessPO.java, UserOrganizationAssignmentPO.java, and UserPositionAssignmentPO.java.
- Modify: Rbac3MigrationContractTest.java and Rbac3FlywayPostgresqlIT.java.

**Consumes:** DDC's String ID type and the approved destructive-migration policy.

**Produces:** Local Application scope external IDs, User Business grants, explicit directory sources, and manual Organization/Position membership persistence.

- [ ] **Step 1: Add failing V6 inventory assertions.**

~~~java
assertThat(listMigrationResources()).containsExactly(
        MIGRATION, STRONG_AUTH_MIGRATION, IDP_MIGRATION,
        TENANT_SESSION_MIGRATION, STATELESS_IDENTITY_MIGRATION,
        "db/migration/V6__adopt_ddc_business_application_authorization_scope.sql");

String v6 = resourceSql(V6_MIGRATION).toLowerCase(Locale.ROOT);
assertThat(v6)
        .contains("ddc_application_id varchar(64) not null")
        .contains("ddc_business_id varchar(64) not null")
        .contains("create table rbac3_user_business_access")
        .contains("create table rbac3_user_org_assignment")
        .contains("create table rbac3_user_position_assignment");
~~~

- [ ] **Step 2: Run the migration contract test.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=Rbac3MigrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: FAIL because V6 does not exist.

- [ ] **Step 3: Implement V6 as the only new migration.**

The migration starts with a clear clean-scope precondition: rbac3_application, rbac3_service_principal, rbac3_service_permission, rbac3_operation_sod_rule, and rbac3_business_participation must all be empty. Their existing values cannot be assigned trustworthy DDC external IDs without a legacy mapping source, so V6 refuses that state rather than inventing IDs. It does preserve existing Organization and Position rows by marking them DIRECTORY_SNAPSHOT.

~~~sql
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM rbac3_application)
       OR EXISTS (SELECT 1 FROM rbac3_service_principal)
       OR EXISTS (SELECT 1 FROM rbac3_service_permission)
       OR EXISTS (SELECT 1 FROM rbac3_operation_sod_rule)
       OR EXISTS (SELECT 1 FROM rbac3_business_participation) THEN
        RAISE EXCEPTION 'RBAC3 V6 requires an empty legacy application authorization graph';
    END IF;
END $$;

ALTER TABLE rbac3_service_permission
    DROP CONSTRAINT fk_rbac3_service_permission_principal,
    DROP CONSTRAINT uq_rbac3_service_permission_fact,
    DROP COLUMN application_code;

DROP INDEX idx_rbac3_service_principal_application;
DROP INDEX idx_rbac3_operation_sod_lookup;
DROP INDEX idx_rbac3_participation_conflict;

ALTER TABLE rbac3_service_principal
    DROP CONSTRAINT uq_rbac3_service_principal_application_id,
    ADD COLUMN application_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_rbac3_service_principal_application_id
        UNIQUE (tenant_id, application_id, id);

ALTER TABLE rbac3_operation_sod_rule
    DROP CONSTRAINT fk_rbac3_operation_sod_application,
    DROP CONSTRAINT uq_rbac3_operation_sod_fact,
    ADD COLUMN application_id BIGINT NOT NULL,
    DROP COLUMN application_code;

ALTER TABLE rbac3_business_participation
    DROP CONSTRAINT fk_rbac3_business_participation_application,
    DROP CONSTRAINT uq_rbac3_business_participation_event,
    ADD COLUMN application_id BIGINT NOT NULL,
    DROP COLUMN application_code;

ALTER TABLE rbac3_application
    ADD COLUMN ddc_application_id VARCHAR(64) NOT NULL,
    ADD COLUMN ddc_business_id VARCHAR(64) NOT NULL;

ALTER TABLE rbac3_application
    DROP CONSTRAINT uq_rbac3_application_identity,
    DROP CONSTRAINT uq_rbac3_application_code,
    ADD CONSTRAINT uq_rbac3_application_tenant_ddc_application
        UNIQUE (tenant_id, ddc_application_id);

ALTER TABLE rbac3_service_principal
    ADD CONSTRAINT fk_rbac3_service_principal_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application (tenant_id, id);

ALTER TABLE rbac3_service_permission
    ADD CONSTRAINT uq_rbac3_service_permission_fact
        UNIQUE (tenant_id, principal_id, permission_id, application_id),
    ADD CONSTRAINT fk_rbac3_service_permission_principal
        FOREIGN KEY (tenant_id, application_id, principal_id)
        REFERENCES rbac3_service_principal (tenant_id, application_id, id);

ALTER TABLE rbac3_operation_sod_rule
    ADD CONSTRAINT uq_rbac3_operation_sod_fact UNIQUE (
        tenant_id, application_id, business_resource,
        prior_action_code, forbidden_later_action_code, valid_from),
    ADD CONSTRAINT fk_rbac3_operation_sod_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application (tenant_id, id);

ALTER TABLE rbac3_business_participation
    ADD CONSTRAINT uq_rbac3_business_participation_event
        UNIQUE (tenant_id, application_id, business_event_id),
    ADD CONSTRAINT fk_rbac3_business_participation_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application (tenant_id, id);

CREATE TABLE rbac3_user_business_access (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    ddc_business_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(128) NOT NULL,
    reason VARCHAR(500),
    ticket_no VARCHAR(128),
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL, updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_user_business_access_source
        UNIQUE (tenant_id, user_id, ddc_business_id, source_type, source_id),
    CONSTRAINT fk_rbac3_user_business_access_user
        FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user (tenant_id, id),
    CONSTRAINT ck_rbac3_user_business_access_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_user_business_access_window
        CHECK (valid_to IS NULL OR valid_to > valid_from)
);
~~~

In the same V6, recreate idx_rbac3_service_principal_application, idx_rbac3_operation_sod_lookup, and idx_rbac3_participation_conflict using application_id where Application identity participates. The existing idx_rbac3_participation_occurred remains unchanged. Recreate the BusinessParticipation append-only trigger after its table/index changes. Any retained application code is display/audit only and never a foreign-key or uniqueness identity.

Add source_type to rbac3_org_unit and rbac3_position, mark existing rows DIRECTORY_SNAPSHOT, and use this invariant:

~~~sql
CHECK (
  (source_type = 'DIRECTORY_SNAPSHOT' AND snapshot_id IS NOT NULL)
  OR (source_type = 'MANUAL' AND snapshot_id IS NULL)
)
~~~

Create rbac3_user_org_assignment and rbac3_user_position_assignment with tenant-composite foreign keys, audit/version fields, status and validity windows. The latter stores user_id, org_unit_id, position_id, primary_assignment, source evidence, and dates. Add both User-side and Organization/Position inverse lookup indexes.

- [ ] **Step 4: Match JPA state to the new invariants.**

~~~java
public ApplicationPO(Long id, Long tenantId,
        String ddcApplicationId, String ddcBusinessId,
        String applicationCode, String applicationName,
        int displayPriority, String actorId, Instant now) {
    this.id = Objects.requireNonNull(id, "id");
    setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
    this.ddcApplicationId = required(ddcApplicationId, "ddcApplicationId");
    this.ddcBusinessId = required(ddcBusinessId, "ddcBusinessId");
    this.applicationCode = required(applicationCode, "applicationCode");
    this.applicationName = required(applicationName, "applicationName");
    this.displayPriority = displayPriority;
    this.status = ApplicationStatusEnum.ACTIVE;
    markCreated(actorId, now);
}

public enum DirectorySourceTypeEnum { DIRECTORY_SNAPSHOT, MANUAL }
public enum UserBusinessAccessStatusEnum { ACTIVE, SUSPENDED, REVOKED, EXPIRED }
~~~

ApplicationPO exposes no request-driven setter for DDC code/name/Business ownership. OrgUnitPO and PositionPO expose source and reject manual lifecycle operations for DIRECTORY_SNAPSHOT.

- [ ] **Step 5: Run schema tests and commit.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=Rbac3MigrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Drbac3-local-it -Dtest=Rbac3FlywayPostgresqlIT \
  -Dsurefire.failIfNoSpecifiedTests=false verify
~~~

Expected: first command passes. The PostgreSQL test passes when RBAC3_IT_POSTGRES variables are configured and otherwise reports skipped.

~~~bash
git add \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V6__adopt_ddc_business_application_authorization_scope.sql \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/resource/domain/po/ApplicationPO.java \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/domain/po/OrgUnitPO.java \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/directory/domain/po/PositionPO.java
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository
git commit -m "feat(rbac3): add iam authorization scope schema"
~~~

### Task 4: Move the existing IAM implementation into the final Java package tree

**Files:**

- Move all Java source/test files under admin/tenant to admin/iam/tenant.
- Move admin/identity to admin/iam/user.
- Move Application responsibility out of admin/resource to admin/iam/application.
- Move remaining resource, field, manifest, and permission files to admin/iam/resource, admin/iam/resource/field, admin/iam/resource/manifest, and admin/iam/permission.
- Move admin/role, admin/assignment, and admin/activation to admin/iam/role, admin/iam/role/assignment, and admin/iam/role/activation.
- Move Organization/snapshot responsibility from admin/directory to admin/iam/organization and admin/iam/organization/snapshot; move Position responsibility to admin/iam/position and admin/iam/position/snapshot.
- Move admin/constraint to admin/iam/policy.
- Modify imports in admin/authorization, audit, bootstrap, config, management, participation, runtime, simulation, and existing admin/shared.
- Modify Rbac3ModuleBoundaryTest.java and AdminLayerBoundaryTest.java.

**Consumes:** Task 3's models. It changes names and package locations only; it introduces no new generic layer.

**Produces:** The exact top.egon.cola.platform.rbac3.admin.iam tree and no retained legacy IAM business package.

- [ ] **Step 1: Add failing architecture assertions for final roots and absent old roots.**

~~~java
assertThat(sourceTree("top/egon/cola/platform/rbac3/admin/iam/user")).exists();
assertThat(sourceTree("top/egon/cola/platform/rbac3/admin/identity")).doesNotExist();
assertThat(allProductionImports()).noneMatch(value -> value.contains(".admin.constraint."));
assertThat(allProductionImports()).noneMatch(value -> value.contains(".admin.directory."));
~~~

- [ ] **Step 2: Run architecture tests and confirm the old roots fail the new assertions.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=Rbac3ModuleBoundaryTest,AdminLayerBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [ ] **Step 3: Perform the move by responsibility and update every declaration/import.**

Use git mv for physical moves. Update package declarations, imports, package-info, test packages, JPA references, and configuration imports in one change. Keep admin/shared and all non-IAM roots in place. Do not add forwarding classes in any old package.

~~~text
admin/iam/{tenant,user,business,application,resource,permission,role,organization,position,policy}
admin/iam/resource/{field,manifest}
admin/iam/role/{assignment,activation,inheritance}
admin/iam/organization/snapshot
admin/iam/position/snapshot
~~~

- [ ] **Step 4: Compile and re-run architecture tests.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -DskipTests compile
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=Rbac3ModuleBoundaryTest,AdminLayerBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS; the existing application scan still covers admin/iam.

- [ ] **Step 5: Commit only the mechanical move.**

~~~bash
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java
git commit -m "refactor(rbac3): move management domains under iam"
~~~

### Task 5: Implement DDC-backed Business catalog reads and local Application authorization scopes

**Files:**

- Create: admin/iam/business/service/DdcCatalogGateway.java, RpcDdcCatalogGateway.java, and BusinessCatalogService.java.
- Create: admin/iam/business/controller/BusinessCatalogController.java.
- Create: admin/iam/application/service/ApplicationScopeFacade.java and controller/ApplicationController.java.
- Create: admin/iam/application/domain/command/AdmitApplicationAuthorizationScopeCommand.java and ChangeApplicationAuthorizationScopeStatusCommand.java.
- Create: admin/iam/application/domain/vo/ApplicationAuthorizationScopeVO.java.
- Modify: admin/iam/application/repository/ApplicationResourceRepository.java.
- Modify: admin/config/runtime/Rbac3ApplicationConfiguration.java.
- Test: RpcDdcCatalogGatewayTest.java, ApplicationScopeFacadeTest.java, BusinessCatalogControllerTest.java, and ApplicationControllerTest.java in their new IAM packages.

**Consumes:** Task 1 DdcManagementClient methods and Task 3 ApplicationPO DDC IDs.

**Produces:** RBAC-owned scope admission/status/removal and DDC-backed read endpoints. No RBAC controller/repository writes to DDC.

- [ ] **Step 1: Add failing gateway/facade tests.**

~~~java
@Test
void admitLoadsDirectoryFactsFromDdcRatherThanTheRequest() {
    when(catalog.findApplication("ddc-app-1")).thenReturn(Optional.of(
            new ApplicationCatalogEntry("ddc-app-1", "ddc-biz-1", "orders",
                    "console", "Console", true, true)));

    ApplicationAuthorizationScopeVO scope = facade.admit(tenantId, actorId,
            new AdmitApplicationAuthorizationScopeCommand("ddc-app-1", 100));

    assertThat(scope.ddcBusinessId()).isEqualTo("ddc-biz-1");
    assertThat(scope.applicationCode()).isEqualTo("console");
}

@Test
void rejectsAdmissionWhenAParentOrApplicationIsDisabled() {
    when(catalog.findApplication("ddc-app-1")).thenReturn(Optional.of(
            new ApplicationCatalogEntry("ddc-app-1", "ddc-biz-1", "orders",
                    "console", "Console", true, false)));

    assertThatThrownBy(() -> facade.admit(tenantId, actorId, command))
            .isInstanceOf(IllegalStateException.class);
}
~~~

- [ ] **Step 2: Run focused tests and confirm the new IAM types are missing.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=RpcDdcCatalogGatewayTest,ApplicationScopeFacadeTest,BusinessCatalogControllerTest,ApplicationControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [ ] **Step 3: Implement one DDC boundary and scope lifecycle.**

~~~java
public final class RpcDdcCatalogGateway implements DdcCatalogGateway {
    private final DdcManagementClient client;

    public Optional<ApplicationCatalogEntry> findApplication(String id) {
        return client.getApp(id).map(this::toEntry);
    }
}

public ApplicationAuthorizationScopeVO admit(
        Long tenantId, String actorId,
        AdmitApplicationAuthorizationScopeCommand command) {
    ApplicationCatalogEntry catalog = requiredEnabledApplication(command.ddcApplicationId());
    return applicationStore.admit(tenantId, catalog, command.displayPriority(), actorId);
}
~~~

Register one closeable DdcRpcClientHandle<DdcManagementClient> in Rbac3ApplicationConfiguration, inject its client into RpcDdcCatalogGateway, and close it when the bean is destroyed. Never construct a client for each HTTP request.

POST applications:admit accepts only ddcApplicationId and displayPriority. It obtains parent ID, codes, names, and status from DDC. GET catalog routes only filter/read DDC data.

- [ ] **Step 4: Add status/removal rules and mappings.**

~~~java
@PostMapping("/applications:admit")
ApiEnvelopeVO<ApplicationAuthorizationScopeVO> admit(
        AdmitApplicationAuthorizationScopeCommand command,
        Authentication authentication)

@PutMapping("/applications/{applicationId}/status")
ApiEnvelopeVO<ApplicationAuthorizationScopeVO> changeStatus(
        Long applicationId,
        ChangeApplicationAuthorizationScopeStatusCommand command,
        Authentication authentication)

@DeleteMapping("/applications/{applicationId}")
ApiEnvelopeVO<Void> remove(
        Long applicationId,
        VersionedDeleteCommand command,
        Authentication authentication)
~~~

Removal is tenant-scoped and rejects a scope with Role, Permission, Resource, Manifest, Service Principal, or authorization dependencies. It only removes a dependency-free local scope and never invokes DDC write APIs.

- [ ] **Step 5: Re-run focused tests and commit.**

~~~bash
git add egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/config/runtime/Rbac3ApplicationConfiguration.java \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam
git commit -m "feat(rbac3): add ddc-backed application authorization scopes"
~~~

### Task 6: Add User Business grants and gate effective roles through the approved chain

**Files:**

- Create: admin/iam/business/service/UserBusinessAccessFacade.java, controller/UserBusinessAccessController.java, domain/command/ReplaceUserBusinessAccessesCommand.java, domain/vo/UserBusinessAccessVO.java, repository/UserBusinessAccessRepository.java, and repository/jpa/JpaUserBusinessAccessRepository.java.
- Create: admin/iam/role/service/RoleEligibilityService.java.
- Modify: admin/iam/role/assignment/service/AssignmentFacade.java.
- Modify: admin/iam/role/activation/service/RoleActivationCandidateService.java and repository/jpa/JpaRoleActivationFactRepository.java.
- Modify: admin/runtime/service/UserAuthorizationSnapshotProjector.java and admin/authorization/service/AuthorizationDecisionService.java.
- Test: UserBusinessAccessFacadeTest.java, RoleEligibilityServiceTest.java, RoleActivationCandidateServiceTest.java, and UserAuthorizationSnapshotProjectorTest.java.

**Consumes:** Tasks 3 and 5, plus existing AuthorizationMutationCoordinator and active-role projection.

**Produces:** Business grants as RBAC authorization records; Application access as a derived view of existing assignments; one fail-closed role eligibility rule used at grant time and projection time.

- [ ] **Step 1: Add failing behavior tests for direct versus effective roles.**

~~~java
@Test
void revokingBusinessAccessKeepsDirectAssignmentButRemovesEffectiveRole() {
    assignmentStore.save(activeAssignment(userId, roleId));
    accessFacade.replace(tenantId, userId, actorId, noManualBusinessAccesses());

    assertThat(assignmentStore.directAssignments(tenantId, userId)).hasSize(1);
    assertThat(eligibility.effectiveRoles(tenantId, userId, clock.instant())).isEmpty();
    verify(mutationCoordinator).execute(any());
}

@Test
void cannotAssignRoleWithoutItsApplicationsBusinessGrant() {
    assertThatThrownBy(() -> assignments.assign(tenantId, userId, actorId,
            new AssignUserRolesCommand(List.of(activeRoleItem(roleId)))))
            .isInstanceOf(IllegalStateException.class);
}
~~~

- [ ] **Step 2: Run focused tests and confirm the Business facade/gate is missing.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=UserBusinessAccessFacadeTest,RoleEligibilityServiceTest,RoleActivationCandidateServiceTest,ActiveRoleSetRevalidatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [ ] **Step 3: Implement Business access replace in one authorization mutation.**

~~~java
public List<UserBusinessAccessVO> replace(
        Long tenantId, Long userId, String actorId,
        ReplaceUserBusinessAccessesCommand command) {
    return mutationCoordinator.execute(userScope(tenantId, userId), () -> {
        validateEveryBusinessIsEnabled(command.items());
        return store.replaceManualAccesses(tenantId, userId, command.items(), actorId, now());
    });
}
~~~

Items contain ddcBusinessId, status, validFrom, validTo, reason, ticketNo, and expectedVersion; they do not contain editable DDC code/name. The repository replaces MANUAL source records only and preserves other source types. The response gets display fields through DdcCatalogGateway.

- [ ] **Step 4: Centralize and use the role eligibility rule.**

~~~java
boolean isEffective(RoleFact role, Long tenantId, Long userId, Instant at) {
    return role.assignmentIsActiveAt(at)
            && applicationScopeIsActive(role.applicationId(), tenantId)
            && hasActiveBusinessAccess(tenantId, userId, role.ddcBusinessId(), at)
            && catalogApplicationIsEnabled(role.ddcApplicationId());
}
~~~

Grant-time validation rejects a missing/disabled DDC App or Business, inactive local Application scope, or missing effective Business grant. Projection-time evaluation filters the role without deleting its assignment. Apply it to User role assign/replace, active-role candidates/revalidation, JpaRoleActivationFactRepository consumer flow, UserAuthorizationSnapshotProjector, and AuthorizationDecisionService. If RPC fails, output no eligible Application context.

Add only these views; do not create a UserApplicationAccess persistence table:

~~~text
GET/PUT /api/rbac3/v1/iam/users/{userId}/business-accesses
GET     /api/rbac3/v1/iam/users/{userId}/application-accesses
GET     /api/rbac3/v1/iam/users/{userId}/roles
GET     /api/rbac3/v1/iam/users/{userId}/effective-roles
GET     /api/rbac3/v1/iam/users/{userId}/access-profile
~~~

- [ ] **Step 5: Add semantic User Role mappings, re-run tests, and commit.**

Expose POST/PUT users/{userId}/roles, DELETE users/{userId}/roles/{roleId}, and POST users/{userId}/role-assignments/{assignmentId}:suspend and :resume. PUT replaces manual assignments only.

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=UserBusinessAccessFacadeTest,RoleEligibilityServiceTest,RoleActivationCandidateServiceTest,AssignmentFacadeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/business \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/runtime \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin
git commit -m "feat(rbac3): gate role access by business authorization"
~~~

### Task 7: Implement Organization/Position source ownership, manual memberships, and automatic roles

**Files:**

- Create: admin/iam/organization/service/OrganizationFacade.java and controller/OrganizationController.java.
- Create: admin/iam/position/service/PositionFacade.java and controller/PositionController.java.
- Create: admin/iam/organization/service/UserOrganizationAssignmentService.java and admin/iam/position/service/UserPositionAssignmentService.java.
- Create: admin/iam/role/assignment/service/PositionAutoRoleRecalculator.java.
- Modify: admin/iam/organization/snapshot/service/DirectorySnapshotMaterializer.java and admin/iam/role/assignment/service/AssignmentFacade.java.
- Test: OrganizationFacadeTest.java, PositionFacadeTest.java, PositionAutoRoleRecalculatorTest.java, and DirectorySnapshotProcessorTest.java.

**Consumes:** Task 3 source/membership schema and Task 6 authorization mutation behavior.

**Produces:** Manual CRUD/memberships that coexist with snapshots and source-scoped automatic role updates.

- [ ] **Step 1: Add failing tests for manual versus snapshot ownership.**

~~~java
@Test
void manualOrganizationCanMoveButSnapshotOrganizationCannot() {
    assertThatCode(() -> facade.move(tenantId, manualOrgId, manualParentId, command))
            .doesNotThrowAnyException();
    assertThatThrownBy(() -> facade.move(tenantId, snapshotOrgId, manualParentId, command))
            .isInstanceOf(IllegalStateException.class);
}

@Test
void removingPositionOnlyRevokesRulesFromThatPositionSource() {
    recalculator.recalculateForPosition(tenantId, positionId, actorId);
    positionAssignments.remove(tenantId, userId, positionId, actorId, expectedVersion);

    assertThat(assignments.forUser(tenantId, userId))
            .extracting(UserRoleAssignmentPO::getSourceType)
            .doesNotContain("POSITION_RULE:" + positionId)
            .contains("MANUAL");
}
~~~

- [ ] **Step 2: Run the focused test group.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=OrganizationFacadeTest,PositionFacadeTest,PositionAutoRoleRecalculatorTest,DirectorySnapshotProcessorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: compile/test failure until the IAM services exist.

- [ ] **Step 3: Implement manual CRUD and tree/membership transactions.**

~~~java
public void move(Long tenantId, Long orgId, Long newParentId,
        MoveOrganizationCommand command, String actorId) {
    OrgUnitPO organization = store.require(tenantId, orgId);
    organization.requireManualSource();
    treeValidator.rejectCycle(tenantId, orgId, newParentId);
    store.moveAndRewritePath(tenantId, organization, newParentId,
            command.expectedVersion(), actorId);
}
~~~

Expose Organization CRUD/tree/children/move/users; Position CRUD, nested Organization positions, and Position users; User Organization add/remove; User Position add/remove. A User Organization membership has no required Position. A User Position membership explicitly carries orgUnitId and must match Position.orgUnitId.

- [ ] **Step 4: Scope snapshot materialization and automatic role recalculation.**

DirectorySnapshotMaterializer creates/updates only DIRECTORY_SNAPSHOT rows and never deletes MANUAL rows. PositionAutoRoleRecalculator handles AutoAssignmentRule.matchType=POSITION and changes only UserRoleAssignment records whose source identifies that Position rule. Each affected User is processed through AuthorizationMutationCoordinator so authVersion and runtime projection change together.

- [ ] **Step 5: Re-run tests and commit.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=OrganizationFacadeTest,PositionFacadeTest,PositionAutoRoleRecalculatorTest,DirectorySnapshotProcessorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/position \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/assignment
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam
git commit -m "feat(rbac3): add iam organization and position administration"
~~~

### Task 8: Complete Tenant, User, Resource, Permission, Role, and Policy IAM APIs

**Files:**

- Modify/create under admin/iam/tenant and admin/iam/user, including UserController and InternalUserController.
- Modify/create under admin/iam/resource, admin/iam/resource/field, and admin/iam/resource/manifest.
- Modify/create under admin/iam/permission, admin/iam/role, and admin/iam/policy.
- Modify: admin/iam/role/inheritance service and existing AuthorizationMutationCoordinator call sites.
- Create test: admin/iam/tenant/controller/IamTenantControllerTest.java and admin/iam/user/controller/UserControllerTest.java.
- Test: moved ApplicationResourceFacadeTest.java, ManifestFacadeIT.java, RoleControlFacadeTest.java, and ConstraintFacadeTest.java.

**Consumes:** Tasks 3 through 7, especially local applicationId identity and source ownership.

**Produces:** Basic CRUD for RBAC-owned Tenant/User/Resource/Permission/Role/Policy records and semantic relationship routes, not relation-table CRUD.

- [ ] **Step 1: Add failing semantic-boundary tests.**

~~~java
@Test
void permissionResourceReplaceRejectsManifestOwnedMappings() {
    assertThatThrownBy(() -> permissions.replaceResources(tenantId, permissionId,
            new ReplacePermissionResourcesCommand(List.of(manifestResourceId), version), actorId))
            .isInstanceOf(IllegalStateException.class);
}

@Test
void roleInheritanceRejectsDifferentLocalApplicationIds() {
    assertThatThrownBy(() -> roles.addParent(tenantId, roleId, parentFromOtherAppId, actorId))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void userCrudMaintainsOnlyRbacMembershipAndIdentitySub() {
    UserView created = users.create(platformTenantId,
            new CreateUserCommand("idp-sub-1", UserStatusEnum.ACTIVE), actorId);

    assertThat(created.identitySub()).isEqualTo("idp-sub-1");
    assertThat(created).hasNoNullFieldsOrPropertiesExcept("archivedAt");
}
~~~

- [ ] **Step 2: Run current focused tests and capture old URI/semantic failures.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=IamTenantControllerTest,UserControllerTest,ApplicationResourceFacadeTest,ManifestFacadeIT,RoleControlFacadeTest,ConstraintFacadeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [ ] **Step 3: Implement target CRUD routes with tenant and local Application checks.**

~~~text
POST/GET       /api/rbac3/v1/iam/tenants
GET/PUT/DELETE /api/rbac3/v1/iam/tenants/{tenantId}
PUT            /api/rbac3/v1/iam/tenants/{tenantId}/status
POST/GET       /api/rbac3/v1/iam/users
GET/PUT/DELETE /api/rbac3/v1/iam/users/{userId}
PUT            /api/rbac3/v1/iam/users/{userId}/status
POST/GET       /api/rbac3/v1/iam/resources
GET/PUT/DELETE /api/rbac3/v1/iam/resources/{resourceId}
PUT            /api/rbac3/v1/iam/resources/{resourceId}/status
GET/POST       /api/rbac3/v1/iam/resources/{resourceId}/fields
GET/PUT/DELETE /api/rbac3/v1/iam/fields/{fieldId}
POST/GET       /api/rbac3/v1/iam/permissions
GET/PUT/DELETE /api/rbac3/v1/iam/permissions/{permissionId}
POST/GET       /api/rbac3/v1/iam/roles
GET/PUT/DELETE /api/rbac3/v1/iam/roles/{roleId}
POST/GET       /api/rbac3/v1/iam/policies/data-rules
POST/GET       /api/rbac3/v1/iam/policies/field-rules
~~~

Tenant management retains the existing platform-administrator target-tenant semantics. User CRUD creates only the RBAC tenant membership and identitySub binding; it never calls IdP, creates a password, or renders profile fields that RBAC does not own. Controllers receive a command/query and tenant context only. Services load the target in the current tenant and validate shared local applicationId. Resource and Field source derives from sourceManifestId; MANIFEST records reject normal edit/delete.

- [ ] **Step 4: Implement only semantic relationship operations.**

~~~java
public record ReplaceRolePermissionsCommand(
        List<Long> permissionIds, long expectedRoleVersion) {}
public record ReplacePermissionResourcesCommand(
        List<Long> resourceIds, long expectedPermissionVersion) {}

@PutMapping("/roles/{roleId}/permissions")
ApiEnvelopeVO<Void> replaceRolePermissions(
        Long roleId, ReplaceRolePermissionsCommand command,
        Authentication authentication)

@PutMapping("/permissions/{permissionId}/resources")
ApiEnvelopeVO<Void> replacePermissionResources(
        Long permissionId, ReplacePermissionResourcesCommand command,
        Authentication authentication)
~~~

Add direct/effective Role permission reads, parent/child inheritance, Role policy views, Permission resource views, DataRule/FieldRule scope-reference views, and SOD/prerequisite/cardinality/operation-SOD policy routes. Keep RoleClosurePO, RolePermissionPO, PermissionResourcePO, DataRuleReferencePO, and snapshot tables internal.

- [ ] **Step 5: Re-run focused tests and commit.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=IamTenantControllerTest,UserControllerTest,ApplicationResourceFacadeTest,ManifestFacadeIT,RoleControlFacadeTest,ConstraintFacadeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/resource \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/permission \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/policy
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam
git commit -m "feat(rbac3): expose iam resource role and policy APIs"
~~~

### Task 9: Move Controller URI, Gateway/Manifest declarations, and internal consumers atomically

**Files:**

- Modify all IAM controllers under admin/iam.
- Modify RBAC bootstrap, security declaration sources, participation, authorization, and contract consumers.
- Modify Gateway/Manifest fixtures under the RBAC Admin test resources contracts directory.
- Test all RBAC controller mapping/discovery tests, BootstrapQueryServiceTest.java, and route-related manifest tests.

**Consumes:** Tasks 4 through 8.

**Produces:** One external RBAC IAM URI family, matching internal URI roots, and no code-only Application consumer.

- [ ] **Step 1: Add mapping assertions for all three roots.**

~~~java
assertThat(controllerMappings()).contains(
        "/api/rbac3/v1/iam/users",
        "/api/rbac3/v1/iam/roles",
        "/api/rbac3/v1/iam/internal/resource-manifests",
        "/internal/v1/iam/users/{identitySub}/tenants");
assertThat(controllerMappings()).noneMatch(path -> path.startsWith("/api/rbac3/v1/users"));
assertThat(controllerMappings()).noneMatch(path -> path.startsWith("/internal/v1/identity"));
~~~

- [ ] **Step 2: Run existing mapping/discovery tests.**

~~~bash
mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=ControllerRequestParameterMetadataTest,Rbac3RoleActivationGatewayDiscoveryTest,Rbac3DecisionRuntimeGatewayDiscoveryTest,Rbac3ControlPlaneGatewayDiscoveryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [ ] **Step 3: Apply this exact old-to-new URI map.**

~~~text
/api/rbac3/v1/platform/tenants -> /api/rbac3/v1/iam/tenants
/api/rbac3/v1/users            -> /api/rbac3/v1/iam/users
/internal/v1/identity           -> /internal/v1/iam/users
/api/rbac3/v1/roles            -> /api/rbac3/v1/iam/roles
/api/rbac3/v1/applications     -> /api/rbac3/v1/iam/applications
/api/rbac3/v1/org-units        -> /api/rbac3/v1/iam/organizations
/api/rbac3/v1/positions        -> /api/rbac3/v1/iam/positions
/api/rbac3/v1/data-rules       -> /api/rbac3/v1/iam/policies/data-rules
/api/rbac3/v1/field-rules      -> /api/rbac3/v1/iam/policies/field-rules
/api/rbac3/v1/auth             -> /api/rbac3/v1/iam/users/me
/api/rbac3/v1/internal         -> /api/rbac3/v1/iam/internal
~~~

Do not change DDC /api/v1/ddc/bizs or /api/v1/ddc/apps. Update GatewayOperation, Resource Manifest, permission declarations, controller mapping tests, and the applicationId-based Service Principal/Operation SOD/BusinessParticipation consumers in the same change.

- [ ] **Step 4: Add and run cleanup scans.**

~~~bash
rg -n '/api/rbac3/v1/(platform/tenants|users|roles|applications|auth)' \
  egon-cola-platforms/egon-cola-platform-rbac3 -g '!docs/**' -g '!db/migration/**'
rg -n 'top\.egon\.cola\.platform\.rbac3\.admin\.(identity|directory|constraint|assignment|activation|resource)' \
  egon-cola-platforms/egon-cola-platform-rbac3 -g '!docs/**'
~~~

Expected: no source hit. Historical documentation and old migrations are excluded.

- [ ] **Step 5: Re-run mapping tests and commit.**

~~~bash
git add -p \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/{authorization,bootstrap,config,participation,runtime} \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test
git commit -m "refactor(rbac3): move management APIs to iam routes"
~~~

### Task 10: Move RBAC3 Admin Web structure, routes, and typed API clients

**Files:**

- Move src/features/tenant to src/features/iam/tenant.
- Move UserDirectoryPage and user API from src/features/directory to src/features/iam/user.
- Move directory snapshot UI to src/features/iam/organization/snapshot.
- Move application UI to src/features/iam/application and split resource/manifest UI to src/features/iam/resource.
- Move role, assignment, and activation UI to src/features/iam/role, src/features/iam/role/assignment, and src/features/iam/role/activation.
- Move constraint UI to src/features/iam/policy.
- Create: src/features/iam/business/business.api.ts and BusinessCatalogPage.tsx.
- Modify: src/features/governance.routes.tsx, authorization.routes.tsx, shared/FeatureApi.tsx, app/App.integration.test.tsx, and all moved feature tests.

**Consumes:** Task 9 URI contract.

**Produces:** A single /iam front-end route family with correct target-tenant header behavior and no old feature route aliases.

- [ ] **Step 1: Add failing route/client assertions.**

~~~tsx
expect(applicationRouteDescriptors.map((route) => route.path)).toEqual(expect.arrayContaining([
  "/iam/tenants", "/iam/users", "/iam/businesses", "/iam/applications",
  "/iam/resources", "/iam/permissions", "/iam/roles",
  "/iam/organizations", "/iam/positions", "/iam/policies/data-rules",
  "/iam/policies/field-rules",
]));

await api.businesses("order");
expect(client.request).toHaveBeenCalledWith(
  "/api/rbac3/v1/iam/catalog/businesses", expect.anything());
~~~

- [ ] **Step 2: Run target UI tests and typecheck.**

~~~bash
cd egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web
npm run test -- src/app/App.integration.test.tsx src/features/iam
npm run typecheck
~~~

Expected: the new route assertions fail before the move.

- [ ] **Step 3: Move features and remove legacy routes, without shared-layer duplication.**

Keep shared/FeatureApi.tsx, adminApiClient.ts, authentication, and query infrastructure in their current common locations. Change the target-tenant test exactly to:

~~~ts
const tenantRequest = targetTenantId === null
  || !path.startsWith("/api/rbac3/v1/iam/tenants")
  ? request
  : Object.assign({}, request, {
      headers: Object.assign({}, request.headers, {
        "X-RBAC3-Target-Tenant": targetTenantId,
      }),
    })
~~~

Remove old /directory, /applications, /roles, /constraints, /authorization/assignments, and /authorization/role-activation routes. Do not add React Router redirects.

- [ ] **Step 4: Implement a read-only DDC catalog client/page.**

~~~ts
export const businessApi = (client: FeatureApiClient) => ({
  businesses: (keyword?: string) => client.request<readonly BusinessCatalogView[]>(
    "/api/rbac3/v1/iam/catalog/businesses", { query: { keyword } }),
  applications: (ddcBusinessId: string, keyword?: string) => client.request<readonly CatalogApplicationView[]>(
    "/api/rbac3/v1/iam/catalog/businesses/" + encodeURIComponent(ddcBusinessId) + "/applications",
    { query: { keyword } }),
})
~~~

BusinessCatalogPage displays Business/App facts and feeds selectors only. It has no create/edit/delete/enabled control and no DDC Admin client import.

- [ ] **Step 5: Re-run tests/typecheck and commit.**

~~~bash
npm run test -- src/app/App.integration.test.tsx src/features/iam
npm run typecheck
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/iam \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/authorization.routes.tsx \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/shared/FeatureApi.tsx \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx
git commit -m "refactor(rbac3-web): move administration features under iam"
~~~

### Task 11: Implement RBAC Admin Web Business/Application/User authorization flows

**Files:**

- Modify/create: src/features/iam/business/BusinessCatalogPage.tsx.
- Modify/create: src/features/iam/application/application.api.ts, ApplicationListPage.tsx, and ApplicationScopeEditor.tsx.
- Modify/create: src/features/iam/user/user.api.ts, UserPage.tsx, UserAccessProfilePage.tsx, UserBusinessAccessEditor.tsx, and UserRoleEditor.tsx.
- Modify/create: src/features/iam/role/activation/RoleActivationPage.tsx.
- Test: corresponding Business, Application, and User page tests.

**Consumes:** Tasks 5 and 6 APIs.

**Produces:** Scope admission/status/removal, User Business access replace, backend-validated User role management, and derived access-profile UI.

- [ ] **Step 1: Add failing interaction tests for exact ownership rules.**

~~~tsx
await user.click(screen.getByRole("button", {name: "接纳应用"}));
expect(request).toHaveBeenCalledWith("/api/rbac3/v1/iam/applications:admit", expect.objectContaining({
  method: "POST", body: {ddcApplicationId: "app-1", displayPriority: 100},
}));
expect(screen.queryByRole("button", {name: "新建业务域"})).not.toBeInTheDocument();

await user.click(screen.getByRole("button", {name: "保存业务域授权"}));
expect(request).toHaveBeenCalledWith("/api/rbac3/v1/iam/users/u-1/business-accesses",
  expect.objectContaining({method: "PUT"}));
~~~

- [ ] **Step 2: Run the target group.**

~~~bash
cd egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web
npm run test -- src/features/iam/business src/features/iam/application src/features/iam/user
~~~

- [ ] **Step 3: Build Application scope controls around DDC selectors.**

~~~ts
export const applicationApi = (client: FeatureApiClient) => ({
  admit: (command: AdmitApplicationScopeCommand) => client.request<ApplicationScopeView>(
    "/api/rbac3/v1/iam/applications:admit", {method: "POST", body: command}),
  changeStatus: (id: string, command: StatusCommand) => client.request<ApplicationScopeView>(
    "/api/rbac3/v1/iam/applications/" + encodeURIComponent(id) + "/status",
    {method: "PUT", body: command}),
  remove: (id: string, expectedVersion: number) => client.request<void>(
    "/api/rbac3/v1/iam/applications/" + encodeURIComponent(id),
    {method: "DELETE", body: {expectedVersion}}),
})
~~~

The form submits only ddcApplicationId and local displayPriority. It displays Business/App IDs, codes, names, and source status read-only; it contains no DDC master-data action.

- [ ] **Step 4: Build User flows on query views and atomic replace APIs.**

~~~ts
replaceBusinessAccesses: (userId, command) => client.request(
  "/api/rbac3/v1/iam/users/" + encodeURIComponent(userId) + "/business-accesses",
  {method: "PUT", body: command}),
replaceRoles: (userId, command) => client.request(
  "/api/rbac3/v1/iam/users/" + encodeURIComponent(userId) + "/roles",
  {method: "PUT", body: command}),
~~~

Show only roles for granted Businesses and admitted scopes returned by the backend. If the server rejects a changed eligibility state, show its error and retain the editor state. Invalidate Business access, Application access, access profile, role, and active-role query keys after a successful mutation.

- [ ] **Step 5: Re-run UI tests/typecheck and commit.**

~~~bash
npm run test -- src/features/iam/business src/features/iam/application src/features/iam/user
npm run typecheck
git add egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/iam
git commit -m "feat(rbac3-web): add iam business and application authorization flows"
~~~

### Task 12: Complete remaining IAM pages and execute cross-module verification

**Files:**

- Modify/create: RBAC Admin Web src/features/iam/resource, permission, role, organization, position, and policy.
- Test: ResourcePages.test.tsx, RolePages.test.tsx, OrganizationPages.test.tsx, PositionPages.test.tsx, and PolicyPages.test.tsx in those IAM directories.
- Verify: DDC Admin Web BizsPage.tsx and AppsPage.tsx without adding RBAC controls.
- Update only with actual results: egon-cola-platforms/egon-cola-platform-rbac3/docs/verification-evidence-template.md.

**Consumes:** Tasks 7 through 11.

**Produces:** Remaining CRUD/semantic editors and real verification evidence across RPC, DDC, RBAC, and both Admin Web applications.

- [ ] **Step 1: Add failing UI tests for protected sources and typed relation selectors.**

~~~tsx
expect(screen.getByText("来源：MANIFEST")).toBeInTheDocument();
expect(screen.getByRole("button", {name: "编辑资源"})).toBeDisabled();
expect(screen.getByLabelText("直接权限")).toHaveTextContent("权限选择器");
expect(screen.queryByPlaceholderText("逗号分隔权限 ID")).not.toBeInTheDocument();
~~~

- [ ] **Step 2: Implement only Spec-defined CRUD and semantic editors.**

Use typed API clients for Task 8 routes. Every relationship editor sends one semantic action, not a sequence of delete/post calls:

~~~ts
replaceRolePermissions: (roleId, command) => client.request(
  "/api/rbac3/v1/iam/roles/" + encodeURIComponent(roleId) + "/permissions",
  {method: "PUT", body: command}),
replacePermissionResources: (permissionId, command) => client.request(
  "/api/rbac3/v1/iam/permissions/" + encodeURIComponent(permissionId) + "/resources",
  {method: "PUT", body: command}),
moveOrganization: (orgId, command) => client.request(
  "/api/rbac3/v1/iam/organizations/" + encodeURIComponent(orgId) + "/move",
  {method: "POST", body: command}),
~~~

DIRECTORY_SNAPSHOT Organization/Position and MANIFEST Resource/Field records are visibly read-only. DataRule/FieldRule copy says policy configuration only; it never promises automatic filtering, masking, or nulling.

- [ ] **Step 3: Verify RBAC UI behavior.**

~~~bash
cd egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web
npm run test
npm run typecheck
npm run build
~~~

Expected: PASS.

- [ ] **Step 4: Run DDC RPC, DDC Admin, and RBAC Admin tests.**

~~~bash
mvn -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter -am \
  -Dtest=DdcManagementProtoMapperTest,DdcRpcContractDescriptorTest,RpcDdcManagementClientTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcManagementRpcProviderTest,DdcBizControllerTest,DdcAppControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am \
  -Dtest=Rbac3MigrationContractTest,Rbac3ModuleBoundaryTest,AdminLayerBoundaryTest,UserBusinessAccessFacadeTest,RoleEligibilityServiceTest,RoleActivationCandidateServiceTest,ApplicationScopeFacadeTest,OrganizationFacadeTest,PositionFacadeTest,RoleControlFacadeTest,ConstraintFacadeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

Expected: PASS.

- [ ] **Step 5: Verify DDC UI boundary and run final cleanup scans.**

~~~bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
npm run test
npm run typecheck
npm run build

rg -n '/api/rbac3/v1/(platform/tenants|users|roles|applications|auth)' \
  egon-cola-platforms/egon-cola-platform-rbac3 -g '!docs/**' -g '!db/migration/**'
rg -n 'admin\.(identity|directory|constraint|assignment|activation|resource)' \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src -g '*.java'
rg -n '/api/rbac3/v1/iam/.+(bizs|apps)' \
  egon-cola-platforms/egon-cola-platform-rbac3 -g '!docs/**'
~~~

Expected: tests pass and scans produce no output. The scans deliberately allow DDC's valid /api/v1/ddc/bizs and /api/v1/ddc/apps routes.

- [ ] **Step 6: Record actual evidence and commit the UI/verification closure.**

~~~bash
git add -p egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/iam \
  egon-cola-platforms/egon-cola-platform-rbac3/docs/verification-evidence-template.md
git commit -m "feat(rbac3-web): complete iam administration pages"
~~~

If the evidence template does not change, stage only the UI files. Do not create an empty documentation commit.

## Plan self-review

| Spec requirement | Covered by |
| --- | --- |
| DDC owns Biz/App master-data CRUD and RBAC receives only catalog reads | Tasks 1, 2, 5, 10, and 12 |
| One V6 migration, local Application IDs, Business grants, and manual directory relations | Task 3 |
| Full IAM package rename and no old-package compatibility wrappers | Task 4 and Task 9 scans |
| User Business grant is the only Business authorization fact; Application authorization is role-derived | Task 6 |
| Active roles exclude inactive/ineligible roles from the authorization context | Task 6 |
| Manual Organization/Position and auto-role behavior remain separate from snapshot records | Task 7 |
| RBAC-owned CRUD, semantic relationships, MANIFEST restrictions, and policy scope limits | Task 8 |
| New public/internal URI roots, Gateway/Manifest alignment, and applicationId-based consumers | Task 9 |
| RBAC IAM routes/pages and DDC Admin CRUD boundary | Tasks 10, 11, and 12 |
| Backend, RPC, both UIs, migration, architecture, and cleanup verification | Task 12 |

Review result: the plan does not add IdP/JWT/Session work, DDC write RPCs, a duplicate UserApplicationAccess model, FIELD ResourceType, automatic field/data enforcement, or a generic rule/relation abstraction.

## Final acceptance checklist

- [ ] DDC is the sole Business/Application master-data writer; its REST APIs and BizsPage/AppsPage remain the only master-data CRUD surface.
- [ ] RBAC reads the DDC catalog only through DdcManagementClient and DDC Management RPC. RBAC does not read DDC tables or write DDC data.
- [ ] ApplicationPO is a tenant-local RBAC scope with DDC external IDs. Code-only RBAC Application relationships are gone.
- [ ] UserBusinessAccess exists and UserApplicationAccess does not. Application authorization remains role-derived.
- [ ] Revoking Business access preserves direct role records but removes effective roles, active candidates, and permission context after authVersion/projection invalidation.
- [ ] IAM package roots, REST routes, internal routes, Gateway/Manifest declarations, API clients, and Web routes are migrated with no compatibility aliases.
- [ ] Manual Organization/Position/membership flows do not mutate snapshot-owned records.
- [ ] MANIFEST Resource/Field data cannot be overwritten by normal CRUD; policy screens make no automatic data/field enforcement claim.
- [ ] DDC RPC, backend, RBAC Web, and DDC Web validation have actual recorded results.
