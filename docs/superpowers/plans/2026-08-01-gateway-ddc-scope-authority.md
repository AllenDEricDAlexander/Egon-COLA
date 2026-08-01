# Gateway DDC Scope Authority Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Execute inline in the current worktree; the user explicitly prohibited subagents.

**Goal:** Make Gateway Admin consume DDC enabled namespace-env-app bindings as its only scope catalog, reuse one physical Gateway Application across namespace views, and make Gateway Web select only valid DDC scopes.

**Architecture:** Extend the existing signed `DdcManagementClient` Adapter with a read-only scope binding operation. Add a Gateway Scope Facade that joins DDC bindings to Gateway physical application identity `(bizCode, env, appCode)`, then expose it to Gateway Web. Keep namespace as a visibility view, preserve existing Gateway Group semantics, and enforce physical application uniqueness with one new Gateway Flyway migration.

**Tech Stack:** Java 21, Spring Boot MVC/Data JPA/Security, PostgreSQL/Flyway, Maven, React 19, TypeScript 6, TanStack Query, Ant Design, Vitest.

## Global Constraints

- DDC scope order is exactly `bizCode -> namespaceCode -> env -> appCode`.
- DDC enabled namespace-env-app bindings are the only selectable Gateway scopes.
- Namespace is a visibility/authorization view and never enters physical application, configuration, service or instance identity.
- Gateway Application physical identity is exactly `(bizCode, env, applicationCode)`.
- A physical application bound to multiple namespaces reuses one Gateway Application ID, Catalog and Credential aggregate.
- `serviceId` is shared by replicas of one logical service; `instanceId` and `leaseId` distinguish replicas and lease sessions.
- Gateway Admin remains `infra/ga`; Gateway Engine remains `infra/ge`; `egon-cola-gateway-engine` and `egon-gateway-rpc` remain different services under `ge`.
- Gateway Web never calls DDC directly and never receives DDC HMAC credentials.
- Do not copy DDC master data into Gateway and do not add a synchronization job.
- Do not redesign Gateway Group persistence or release/routing semantics.
- Add exactly one new Gateway migration, `V6`; never modify existing Flyway files.
- Preserve existing data. Detect conflicting physical Gateway applications and fail migration instead of deleting or merging them.
- Use the existing local PostgreSQL and Redis for live verification; do not use Docker or Testcontainers as host-local topology evidence.
- Preserve unrelated worktree changes and stage only each task's declared files.

---

## File Structure

### DDC signed management boundary

- Create `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementScopeQuery.java`: optional four-field scope query.
- Create `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementScopeBinding.java`: transport-neutral binding projection.
- Modify `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/DdcManagementClient.java`: add default read-only method.
- Modify `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/client/HttpDdcManagementClient.java`: signed HTTP Adapter implementation.
- Modify `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcManagementOpenApiController.java`: HMAC-protected scope endpoint using the existing binding service.

### Gateway backend

- Create `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/scope/GatewayScopeService.java`: DDC/Gateway join Facade and exact binding validation.
- Create `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/interfaces/management/GatewayScopeController.java`: authenticated read-only `/scopes` API.
- Create `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/GatewayApplicationAlreadyExistsException.java`: physical duplicate evidence.
- Modify `GatewayApplicationRepository.java`, `GatewayApplicationService.java`, `GatewayApplicationController.java`, and `GatewayAdminExceptionHandler.java`: physical identity, optional filtering and conflict response.
- Create `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/db/migration/V6__enforce_gateway_application_physical_identity.sql`: replace the legacy namespace-based unique index.

### Gateway Web

- Modify `src/api/types.ts` and `src/api/gatewayApi.ts`: scope binding contract and API.
- Rewrite `src/hooks/scopeDefaults.ts`: pure valid-scope resolver, cascade and options.
- Rewrite `src/hooks/useScope.tsx`: authenticated async DDC scope loading and persistence.
- Modify `src/layouts/AdminLayout.tsx`: DDC-derived `biz -> namespace -> env -> app` selectors.
- Modify `src/features/applications/ApplicationsPage.tsx`: read-only DDC identity during creation and explicit error display.

---

### Task 1: Add the signed DDC scope binding management contract

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementScopeQuery.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementScopeBinding.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/DdcManagementClient.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/client/HttpDdcManagementClient.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcManagementOpenApiController.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/management/client/HttpDdcManagementClientTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcManagementOpenApiControllerTest.java`

**Interfaces:**
- Produces `DdcManagementClient#getScopeBindings(DdcManagementScopeQuery)`.
- Produces signed `GET /api/v1/ddc/openapi/management/scope-bindings`.
- Query parameters `bizCode`, `namespaceCode`, `env`, `appCode` are independently optional and blank values are omitted.
- Response order is inherited from `DdcNamespaceEnvAppBindingService#list` and is stable by biz, namespace, env, app.

- [ ] **Step 1: Write failing HTTP Adapter tests for empty and partial queries**

```java
@Test
void scopeBindingsOmitBlankFiltersAndKeepSignedPartialQuery() {
    ClientFixture fixture = fixture("ak", "sk");
    fixture.server().expect(requestTo(
            "http://ddc.test/api/v1/ddc/openapi/management/scope-bindings"
                    + "?bizCode=retail&namespaceCode=ops"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(scopeBindingResponse(), MediaType.APPLICATION_JSON));

    List<DdcManagementScopeBinding> result = fixture.client().getScopeBindings(
            new DdcManagementScopeQuery("retail", "ops", " ", null));

    assertThat(result).singleElement().satisfies(binding -> {
        assertThat(binding.bizCode()).isEqualTo("retail");
        assertThat(binding.appCode()).isEqualTo("order");
    });
    fixture.server().verify();
}
```

- [ ] **Step 2: Run the Starter test and verify RED**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-starter -am \
  -Dtest=HttpDdcManagementClientTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because the scope query, binding and client method do not exist.

- [ ] **Step 3: Add the records and default client method**

```java
public record DdcManagementScopeQuery(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode
) {
}
```

```java
public record DdcManagementScopeBinding(
        String bindingId,
        String bizCode,
        String namespaceCode,
        String env,
        String appId,
        String appCode,
        String appName,
        boolean enabled
) {
}
```

```java
default List<DdcManagementScopeBinding> getScopeBindings(
        DdcManagementScopeQuery query) {
    throw new UnsupportedOperationException(
            "Scope binding lookup is not supported");
}
```

- [ ] **Step 4: Implement the signed HTTP Adapter method**

```java
@Override
public List<DdcManagementScopeBinding> getScopeBindings(
        DdcManagementScopeQuery query) {
    require(query, "query");
    return exchange(
            HttpMethod.GET,
            MANAGEMENT_PATH + "/scope-bindings",
            scopeQuery(query),
            null,
            new ParameterizedTypeReference<>() { },
            true
    );
}

private Map<String, List<String>> scopeQuery(DdcManagementScopeQuery query) {
    Map<String, List<String>> values = new LinkedHashMap<>();
    putQuery(values, "bizCode", query.bizCode());
    putQuery(values, "namespaceCode", query.namespaceCode());
    putQuery(values, "env", query.env());
    putQuery(values, "appCode", query.appCode());
    return values;
}
```

- [ ] **Step 5: Write the failing DDC OpenAPI controller projection test**

```java
@MockBean
private DdcNamespaceEnvAppBindingService bindingService;

@Test
void scopeBindingsAcceptAnySubsetOfFilters() throws Exception {
    when(bindingService.list("retail", null, null, "order"))
            .thenReturn(List.of(new DdcNamespaceEnvAppBindingVO(
                    "binding-1", "retail", "ns-ops", "ops", "local",
                    "app-order", "order", "Order", true)));

    mockMvc.perform(get("/api/v1/ddc/openapi/management/scope-bindings")
                    .param("bizCode", "retail")
                    .param("appCode", "order"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].namespaceCode").value("ops"));
}
```

- [ ] **Step 6: Implement the controller endpoint by mapping the existing binding service**

```java
@GetMapping("/scope-bindings")
public ResultRecord<List<DdcManagementScopeBinding>> scopeBindings(
        @RequestParam(value = "bizCode", required = false) String bizCode,
        @RequestParam(value = "namespaceCode", required = false) String namespaceCode,
        @RequestParam(value = "env", required = false) String env,
        @RequestParam(value = "appCode", required = false) String appCode) {
    return ResultRecord.success(bindingService.list(
            bizCode, namespaceCode, env, appCode).stream()
            .map(value -> new DdcManagementScopeBinding(
                    value.id(), value.bizCode(), value.namespaceCode(),
                    value.env(), value.appId(), value.appCode(),
                    value.appName(), value.enabled()))
            .toList());
}
```

- [ ] **Step 7: Run DDC targeted and module tests**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-starter,egon-cola-platform-dynamic-config-center-admin \
  -am -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=HttpDdcManagementClientTest,DdcManagementOpenApiControllerTest,DdcNamespaceEnvAppBindingServiceTest test
```

Expected: all named tests pass and the reactor summary shows Starter and Admin were executed.

- [ ] **Step 8: Commit the DDC contract**

```bash
git add \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin
git commit -m "feat(ddc): expose signed scope bindings"
```

### Task 2: Add the Gateway Scope Facade and authenticated catalog API

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/scope/GatewayScopeService.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/interfaces/management/GatewayScopeController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/infrastructure/persistence/GatewayApplicationRepository.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/application/scope/GatewayScopeServiceTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/interfaces/management/GatewayAdminSecurityIntegrationTest.java`

**Interfaces:**
- Consumes `DdcManagementClient#getScopeBindings` from Task 1.
- Produces `GatewayScopeService#list()` and `#bindings(ScopeQuery)`.
- Produces `GatewayScopeService#requireEnabled(ScopeQuery)` returning `DdcManagementScopeBinding` for creation validation.
- Produces authenticated `GET /api/v1/gateway/admin/scopes`.
- Produces repository lookup `findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse`.

- [ ] **Step 1: Write failing Facade tests for multi-namespace reuse and upstream failure**

```java
@Test
void mapsTwoNamespaceBindingsToOneGatewayApplication() {
    when(client.getScopeBindings(any())).thenReturn(List.of(
            binding("binding-default", "default", true),
            binding("binding-ops", "ops", true)));
    when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
            .thenReturn(List.of(application("gateway-order")));

    assertThat(service.list())
            .extracting(GatewayScopeService.ScopeView::namespace,
                    GatewayScopeService.ScopeView::gatewayApplicationId)
            .containsExactly(
                    tuple("default", "gateway-order"),
                    tuple("ops", "gateway-order"));
}

@Test
void reportsDdcFailureInsteadOfReturningStaticScopes() {
    when(client.getScopeBindings(any()))
            .thenThrow(new DdcManagementClientException(
                    "DDC_MANAGEMENT_IO_ERROR", "offline", null));

    assertThatThrownBy(service::list)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DDC scope catalog");
}
```

- [ ] **Step 2: Run Gateway scope test and verify RED**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayScopeServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `GatewayScopeService` does not exist.

- [ ] **Step 3: Implement the Facade records and DDC/Gateway join**

```java
public record ScopeQuery(
        String bizCode,
        String namespace,
        String env,
        String appCode
) {
    public boolean empty() {
        return Stream.of(bizCode, namespace, env, appCode)
                .allMatch(value -> value == null || value.isBlank());
    }
}

public record PhysicalApplicationKey(
        String bizCode,
        String env,
        String appCode
) {
}

public record ScopeView(
        String bindingId,
        String bizCode,
        String namespace,
        String env,
        String appCode,
        String appName,
        boolean connected,
        String gatewayApplicationId
) {
}
```

```java
public List<DdcManagementScopeBinding> bindings(ScopeQuery query) {
    try {
        return client().getScopeBindings(new DdcManagementScopeQuery(
                        query.bizCode(), query.namespace(), query.env(), query.appCode()))
                .stream()
                .filter(DdcManagementScopeBinding::enabled)
                .sorted(BINDING_ORDER)
                .toList();
    } catch (DdcManagementClientException | UnsupportedOperationException error) {
        throw new IllegalStateException("DDC scope catalog is unavailable", error);
    }
}

public DdcManagementScopeBinding requireEnabled(ScopeQuery query) {
    return bindings(query).stream()
            .filter(value -> exact(value, query))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                    "DDC scope binding is not enabled"));
}

public List<ScopeView> list() {
    Map<PhysicalApplicationKey, String> connected = applications
            .findAllByDeletedFalseOrderByCreatedAtDesc().stream()
            .collect(Collectors.toMap(
                    value -> new PhysicalApplicationKey(
                            value.getBizCode(), value.getEnv(),
                            value.getApplicationCode()),
                    GatewayApplicationEntity::getId));
    return bindings(new ScopeQuery(null, null, null, null)).stream()
            .map(binding -> view(binding, connected))
            .toList();
}
```

The production constructor receives `ObjectProvider<DdcManagementClient>` so Gateway Admin can still start when DDC management
is disabled; `client()` then throws `IllegalStateException("DDC management client is not configured")`. The package-private test
constructor accepts a nullable `DdcManagementClient` directly.

- [ ] **Step 4: Add the physical repository lookup**

```java
Optional<GatewayApplicationEntity>
findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
        String bizCode,
        String applicationCode,
        String env);
```

Keep the legacy namespace-based method until all existing callers have moved in Task 3.

- [ ] **Step 5: Add the authenticated scope controller**

```java
@RestController
@RequestMapping("/api/v1/gateway/admin/scopes")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayScopeController {
    private final GatewayScopeService service;

    public GatewayScopeController(GatewayScopeService service) {
        this.service = service;
    }

    @GetMapping
    public List<GatewayScopeService.ScopeView> list() {
        return service.list();
    }
}
```

- [ ] **Step 6: Extend security coverage**

Add `GatewayScopeController.class` to the existing MVC security slice, mock `GatewayScopeService`, and assert
an unauthenticated request is rejected while a token with `gateway:read` receives `200`.

```java
mockMvc.perform(get("/api/v1/gateway/admin/scopes")
        .with(jwt().authorities(new SimpleGrantedAuthority("CAP_gateway:read"))))
        .andExpect(status().isOk());
```

- [ ] **Step 7: Run Gateway scope and security tests**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayScopeServiceTest,GatewayAdminSecurityIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 8: Commit the Gateway scope catalog**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git commit -m "feat(gateway-admin): expose DDC scope catalog"
```

### Task 3: Enforce one physical Gateway Application and scope-aware listing

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/db/migration/V6__enforce_gateway_application_physical_identity.sql`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/GatewayApplicationAlreadyExistsException.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/GatewayApplicationService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/infrastructure/persistence/GatewayApplicationRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/interfaces/management/GatewayApplicationController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/interfaces/management/GatewayAdminExceptionHandler.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/application/GatewayApplicationServiceTest.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/migration/GatewayV6MigrationTest.java`

**Interfaces:**
- Consumes `GatewayScopeService#requireEnabled` and `#bindings` from Task 2.
- `GatewayApplicationController#list` accepts four independently optional request parameters.
- `GatewayApplicationView` adds `boolean ddcMatched`; scoped results use the selected namespace as visibility context while the stored namespace remains legacy creation context.
- Duplicate physical creation throws `GatewayApplicationAlreadyExistsException(existingApplicationId)` and returns HTTP 409 with code `GATEWAY_ADMIN_APPLICATION_ALREADY_EXISTS`.

- [ ] **Step 1: Write failing service tests for exact validation, namespace reuse and partial filters**

```java
@Test
void secondNamespaceCannotCreateASecondPhysicalApplication() {
    ScopeQuery scope = new ScopeQuery("retail", "ops", "local", "order");
    when(scopes.requireEnabled(scope)).thenReturn(binding("ops"));
    when(applications.findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
            "retail", "order", "local"))
            .thenReturn(Optional.of(application("application-order", "default")));

    assertThatThrownBy(() -> service.create(command(scope), actor(), request()))
            .isInstanceOfSatisfying(
                    GatewayApplicationAlreadyExistsException.class,
                    error -> assertThat(error.existingApplicationId())
                            .isEqualTo("application-order"));
}

@Test
void listsOnePhysicalApplicationThroughEitherNamespace() {
    when(scopes.bindings(new ScopeQuery("retail", "ops", "local", "order")))
            .thenReturn(List.of(binding("ops")));
    when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
            .thenReturn(List.of(application("application-order", "default")));

    assertThat(service.list(new ScopeQuery("retail", "ops", "local", "order")))
            .singleElement()
            .satisfies(view -> {
                assertThat(view.id()).isEqualTo("application-order");
                assertThat(view.namespace()).isEqualTo("ops");
                assertThat(view.ddcMatched()).isTrue();
            });
}
```

- [ ] **Step 2: Run the application service tests and verify RED**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: Implement physical creation validation and list matching**

```java
@Transactional
public GatewayApplicationView create(
        CreateGatewayApplication command,
        AdminActor actor,
        RequestAuditContext request) {
    GatewayScopeService.ScopeQuery scope = new GatewayScopeService.ScopeQuery(
            required(command.bizCode(), "bizCode"),
            required(command.namespace(), "namespace"),
            required(command.env(), "env"),
            required(command.applicationCode(), "applicationCode"));
    DdcManagementScopeBinding binding = scopes.requireEnabled(scope);
    GatewayApplicationEntity existing = applications
            .findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
            scope.bizCode(), scope.appCode(), scope.env())
            .orElse(null);
    if (existing != null) {
        throw new GatewayApplicationAlreadyExistsException(existing.getId());
    }
    Instant now = clock.instant();
    GatewayApplicationEntity application = new GatewayApplicationEntity(
            UuidV7.simpleString(),
            scope.bizCode(),
            scope.appCode(),
            required(command.displayName(), "displayName"),
            scope.env(),
            scope.namespace(),
            command.description(),
            actor.actorId(),
            now);
    applications.saveAndFlush(application);
    audit(actor, request, application.getId(), "CREATE", Map.of(
            "bindingId", binding.bindingId(),
            "bizCode", scope.bizCode(),
            "applicationCode", scope.appCode(),
            "env", scope.env(),
            "namespace", scope.namespace()));
    return view(application, scope.namespace(), true);
}
```

`list(ScopeQuery)` obtains enabled bindings from the Facade, converts them to a set of physical keys, filters active
Gateway applications by those keys, and maps the requested namespace into scoped views. With an empty query it returns
all active Gateway applications and sets `ddcMatched=false` for physical keys absent from the complete DDC binding set.

- [ ] **Step 4: Add optional controller filters and the conflict handler**

```java
@GetMapping
public List<GatewayApplicationView> list(
        @RequestParam(required = false) String bizCode,
        @RequestParam(required = false) String namespace,
        @RequestParam(required = false) String env,
        @RequestParam(required = false) String appCode) {
    return service.list(new GatewayScopeService.ScopeQuery(
            bizCode, namespace, env, appCode));
}
```

```java
@ExceptionHandler(GatewayApplicationAlreadyExistsException.class)
public ResponseEntity<ErrorResponse> applicationExists(
        GatewayApplicationAlreadyExistsException error) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
            "GATEWAY_ADMIN_APPLICATION_ALREADY_EXISTS",
            "gateway application already exists: " + error.existingApplicationId(),
            null,
            List.of(),
            Instant.now()));
}
```

- [ ] **Step 5: Write the V6 migration and structural regression test**

```sql
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM gateway_application
         WHERE deleted = FALSE
         GROUP BY biz_code, application_code, env
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'duplicate active gateway application physical identity';
    END IF;
END $$;

DROP INDEX IF EXISTS uk_gateway_application_scope_active;

CREATE UNIQUE INDEX uk_gateway_application_physical_active
    ON gateway_application (biz_code, application_code, env)
    WHERE deleted = FALSE;
```

`GatewayV6MigrationTest` reads the classpath resource and asserts the conflict precheck, old-index drop, new column order,
and `WHERE deleted = FALSE`. The later host-local migration is the execution proof.

- [ ] **Step 6: Run Gateway application and migration tests**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin -am \
  -Dtest=GatewayApplicationServiceTest,GatewayV6MigrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: Commit physical application identity**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git commit -m "fix(gateway-admin): reuse applications across namespaces"
```

### Task 4: Replace Gateway Web hardcoded scopes with DDC catalog resolution

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts`
- Rewrite: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeDefaults.ts`
- Rewrite test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeDefaults.test.ts`

**Interfaces:**
- Consumes `GET /api/v1/gateway/admin/scopes` from Task 2.
- Produces `GatewayScopeBinding`, `gatewayApi.scopes`, `resolveInitialScope`, `changeScope`, and `optionsFor`.
- Initial priority is last valid, configured valid, first connected, first valid.
- No function manufactures `default/default-app/dev/default` or inserts the current string into options.

- [ ] **Step 1: Write failing pure resolver and cascade tests**

```ts
it('uses last valid then configured then connected then first binding', () => {
  expect(resolveInitialScope(bindings, opsScope, configuredScope)).toEqual(opsScope)
  expect(resolveInitialScope(bindings, invalidScope, configuredScope)).toEqual(configuredScope)
  expect(resolveInitialScope(bindings, invalidScope, undefined)).toEqual(connectedScope)
})

it('keeps valid descendants and otherwise resets to the first valid branch', () => {
  expect(changeScope(bindings, defaultScope, 'namespace', 'ops')).toEqual(opsScope)
  expect(optionsFor(bindings, opsScope, 'appCode')).toEqual([
    { value: 'order', label: 'App: order' },
  ])
})

it('returns undefined when no DDC binding exists', () => {
  expect(resolveInitialScope([], undefined, undefined)).toBeUndefined()
})
```

- [ ] **Step 2: Run scope tests and verify RED**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web \
  test -- --run src/hooks/scopeDefaults.test.ts
```

- [ ] **Step 3: Add the API type and request**

```ts
export type GatewayScopeBinding = Scope & {
  bindingId: string
  appName: string
  connected: boolean
  gatewayApplicationId?: string
}
```

```ts
scopes: (signal?: AbortSignal) =>
  apiRequest<GatewayScopeBinding[]>(`${admin}/scopes`, { signal }),
```

Add an API test that stubs `fetch`, calls `gatewayApi.scopes()`, and asserts the exact path ends in
`/api/v1/gateway/admin/scopes` without DDC credentials or static scope query parameters.

- [ ] **Step 4: Implement the pure resolver functions**

```ts
export const resolveInitialScope = (
  bindings: GatewayScopeBinding[],
  stored?: Scope,
  configured?: Scope,
): Scope | undefined => {
  const valid = (candidate?: Scope): Scope | undefined =>
    candidate && bindings.some((binding) => sameScope(binding, candidate))
      ? candidate
      : undefined
  return valid(stored)
    ?? valid(configured)
    ?? scopeOf(bindings.find((binding) => binding.connected))
    ?? scopeOf(bindings[0])
}
```

```ts
const fieldOrder = ['bizCode', 'namespace', 'env', 'appCode'] as const

export const changeScope = (
  bindings: GatewayScopeBinding[],
  current: Scope,
  field: ScopeField,
  value: string,
): Scope => {
  const index = fieldOrder.indexOf(field)
  const prefix = { ...current, [field]: value }
  const matchesPrefix = (binding: GatewayScopeBinding) =>
    fieldOrder.slice(0, index + 1)
      .every((name) => binding[name] === prefix[name])
  const retained = bindings.find((binding) =>
    matchesPrefix(binding)
    && fieldOrder.slice(index + 1)
      .every((name) => binding[name] === current[name]))
  const selected = retained ?? bindings.find(matchesPrefix)
  if (!selected) throw new Error(`No DDC scope for ${field}=${value}`)
  return scopeOf(selected)!
}

export const optionsFor = (
  bindings: GatewayScopeBinding[],
  scope: Scope,
  field: ScopeField,
) => {
  const index = fieldOrder.indexOf(field)
  const values = bindings
    .filter((binding) => fieldOrder.slice(0, index)
      .every((name) => binding[name] === scope[name]))
    .map((binding) => binding[field])
  return [...new Set(values)].map((value) => ({
    value,
    label: `${fieldLabel[field]}: ${value}`,
  }))
}
```

- [ ] **Step 5: Run Web API and resolver tests**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web \
  test -- --run src/hooks/scopeDefaults.test.ts src/api/gatewayApi.test.ts
```

- [ ] **Step 6: Commit the pure Web scope contract**

```bash
git add \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeDefaults.ts \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/scopeDefaults.test.ts
git commit -m "feat(gateway-web): resolve DDC scopes"
```

### Task 5: Wire async scope state, selectors and DDC-owned application creation

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/useScope.tsx`
- Create test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/hooks/useScope.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx`
- Create test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/applications/ApplicationsPage.tsx`
- Create test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/applications/ApplicationsPage.test.tsx`

**Interfaces:**
- Consumes the pure functions and `gatewayApi.scopes` from Task 4.
- `ScopeContextValue` produces `{ scope, bindings, changeScope }` only after a valid DDC scope is loaded.
- Persists only complete valid tuples under `egon.gateway.admin.scope.v1`.
- Local login remains Access Token only; this task does not reintroduce Refresh Token.
- Application creation sends the selected DDC `bizCode`, `namespace`, `env`, and `appCode`; those fields are read-only in the modal.

- [ ] **Step 1: Write failing provider tests for connected fallback, persistence, empty and error states**

```tsx
it('loads scopes after login and exposes the first connected binding', async () => {
  mockScopes([unconnectedScope, connectedScope])
  renderScopeConsumer()
  expect(await screen.findByText('retail/default/local/order')).toBeInTheDocument()
})

it('does not render scoped children when DDC scope loading fails', async () => {
  mockScopesFailure()
  renderScopeConsumer()
  expect(await screen.findByText('DDC 作用域加载失败')).toBeInTheDocument()
  expect(screen.queryByTestId('scoped-child')).not.toBeInTheDocument()
})
```

- [ ] **Step 2: Run component tests and verify RED**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web \
  test -- --run src/hooks/useScope.test.tsx src/layouts/AdminLayout.test.tsx \
  src/features/applications/ApplicationsPage.test.tsx
```

- [ ] **Step 3: Implement authenticated async ScopeProvider**

```tsx
const scopes = useQuery({
  queryKey: ['gateway-scopes'],
  queryFn: ({ signal }) => gatewayApi.scopes(signal),
  enabled: Boolean(auth.session),
})

if (!auth.session) return children
if (scopes.isLoading) return <Spin fullscreen tip="加载 DDC 作用域" />
if (scopes.error) return <Result status="error" title="DDC 作用域加载失败" />
if (!scopes.data?.length) {
  return <Result status="info" title="DDC 暂无已启用的 namespace-env-app 绑定" />
}
```

Resolve the selected scope from stored/configured/catalog values, persist only values present in `scopes.data`, and expose a
`changeScope(field, value)` callback that delegates to the pure cascade function. Do not render routed scoped pages before a
valid tuple exists, preventing invalid Dashboard/Application/Provider requests.

- [ ] **Step 4: Replace header hardcoded options and order**

```tsx
const selectors: Array<[ScopeField, string, string]> = [
  ['bizCode', '业务域', 'Biz'],
  ['namespace', '命名空间', 'Namespace'],
  ['env', '环境', 'Env'],
  ['appCode', '应用', 'App'],
]
```

Render the four `Select` components in that order with `optionsFor(bindings, scope, field)`. On confirmed change call the
context `changeScope`, remove scoped cached queries without removing `gateway-scopes`, and navigate to `/dashboard`.

- [ ] **Step 5: Make Application identity read-only and display mutation errors**

```tsx
<Form.Item label="业务域"><Input value={scope.bizCode} disabled /></Form.Item>
<Form.Item label="命名空间"><Input value={scope.namespace} disabled /></Form.Item>
<Form.Item label="环境"><Input value={scope.env} disabled /></Form.Item>
<Form.Item label="应用"><Input value={scope.appCode} disabled /></Form.Item>
```

```ts
gatewayApi.createApplication({
  bizCode: scope.bizCode,
  namespace: scope.namespace,
  env: scope.env,
  applicationCode: scope.appCode,
  displayName: values.displayName,
  description: values.description,
})
```

Remove editable Application Code from the new form. Render `save.error` through the existing `QueryFailure`/Alert style.
After a physical-duplicate 409, invalidate the current applications query so the already connected application is displayed.

- [ ] **Step 6: Run the complete Gateway Web validation**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web test -- --run
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run lint
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run build
```

- [ ] **Step 7: Commit the Gateway Web integration**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
git commit -m "fix(gateway-web): use DDC scope authority"
```

### Task 6: Run full regression, migrate, restart and prove the live flow

**Files:**
- Runtime only: `target/local-biz-app-run/` for PID files, logs and `admin.jwt`; artifacts remain ignored.
- Source changes are allowed only after reproducing a real failure with a targeted test; each repair gets a separate `fix:` commit.

**Interfaces:**
- DDC Admin: `http://127.0.0.1:18080`.
- Gateway Admin: `http://127.0.0.1:8080`.
- Gateway Engine public/internal/management/RPC: `18081/18082/18083/19090`.
- Gateway Web: `http://127.0.0.1:5173`; DDC Web: `http://127.0.0.1:5174`.
- Order Provider replicas: `18084` and `18085`.
- Access Token file: `target/local-biz-app-run/admin.jwt`, mode `0600`; never print its content.

- [ ] **Step 1: Run backend full affected-reactor tests**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml clean test
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml clean test
```

Inspect reactor summaries and actual test counts; an aggregator-only `BUILD SUCCESS` is insufficient.

- [ ] **Step 2: Re-run all affected Web checks**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web test -- --run
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run lint
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run build
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web test -- --run
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run lint
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run build
```

- [ ] **Step 3: Preflight V6 physical identity against the host PostgreSQL**

```sql
select biz_code, application_code, env, count(*)
  from gateway_application
 where deleted = false
 group by biz_code, application_code, env
having count(*) > 1;
```

Expected: zero rows. If rows exist, stop before starting the new Gateway Admin and report exact application IDs; do not merge
or delete them automatically. Starting the packaged Gateway Admin then runs Flyway V6. Verify `flyway_schema_history` contains
version `6` and `uk_gateway_application_physical_active` exists.

- [ ] **Step 4: Build executable artifacts**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-admin -am package -DskipTests
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin,egon-cola-platform-gateway-engine,egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-http-provider \
  -am package -DskipTests
```

- [ ] **Step 5: Restart only the confirmed project processes**

```bash
lsof -nP -iTCP:18080 -iTCP:8080 -iTCP:18081 -iTCP:18082 -iTCP:18083 \
  -iTCP:19090 -iTCP:18084 -iTCP:18085 -iTCP:5173 -iTCP:5174 -sTCP:LISTEN
```

Resolve each PID from the existing `target/local-biz-app-run` PID files, command line and listening port before sending TERM.
Do not stop PostgreSQL or Redis. Start DDC Admin, Gateway Admin, Gateway Engine, the two Provider replicas, Gateway Web and
DDC Web in that dependency order, preserving the existing environment/secret files and never echoing credentials. Poll readiness
URLs instead of using a fixed long sleep.

- [ ] **Step 6: Verify signed scope and Gateway APIs with the stored Access Token**

```text
GET /api/v1/ddc/openapi/management/scope-bindings
GET /api/v1/gateway/admin/scopes
GET /api/v1/gateway/admin/applications?bizCode=retail&namespace=default&env=local&appCode=order
GET /api/v1/gateway/admin/applications?bizCode=retail&namespace=ops&env=local&appCode=order
GET /api/v1/gateway/admin/providers/instances?bizCode=retail&namespace=default&env=local&appCode=order
GET /api/v1/gateway/admin/providers/instances?bizCode=retail&namespace=ops&env=local&appCode=order
```

Use the existing HMAC credentials for the DDC signed endpoint and read the Gateway bearer token from the protected file without
printing it. Assert both namespace Application responses contain the same application ID. Assert both Provider responses contain
the same `serviceId`/service key and the same two instance IDs, with distinct lease IDs and ports.

- [ ] **Step 7: Verify UI-serving state without opening a browser**

```text
GET http://127.0.0.1:5173
GET http://127.0.0.1:5174
GET http://127.0.0.1:18080/actuator/health/readiness
GET http://127.0.0.1:8080/actuator/health/readiness
GET http://127.0.0.1:18083/actuator/health/readiness
```

Run the Web component tests as UI behavior proof because repository instructions prohibit opening a browser or using Computer Use.
Keep the requested services running for user testing after all readiness and API assertions pass.

- [ ] **Step 8: Final repository and process audit**

```bash
git diff --check
git status --short --branch
lsof -nP -iTCP:18080 -iTCP:8080 -iTCP:18081 -iTCP:18082 -iTCP:18083 \
  -iTCP:19090 -iTCP:18084 -iTCP:18085 -iTCP:5173 -iTCP:5174 -sTCP:LISTEN
```

Confirm every task commit is present, no secret is staged, and only intentionally running target services occupy the declared ports.

## Plan Self-Review

- Spec coverage: Task 1 covers the signed optional-filter DDC catalog; Task 2 covers the server-side Adapter/Facade boundary and future RBAC insertion point; Task 3 covers one physical application, legacy compatibility, unmatched records and exactly one migration; Tasks 4-5 cover authoritative initial selection, cascade, failures, read-only creation, Catalog/Credential reuse and no hardcoded fallback; Task 6 covers identity configuration regression and host-local live proof.
- Placeholder scan: the plan contains no TBD, TODO, “implement later”, generic error-handling instruction, or undefined “same as prior task” step.
- Type consistency: DDC transport uses `namespaceCode`; Gateway API/Web use `namespace`; physical keys consistently use `bizCode, env, appCode/applicationCode`; scope ordering consistently uses `bizCode, namespace, env, appCode`.
- Migration safety: only Gateway `V6` is added; V1-V5 remain untouched; active physical duplicates stop migration before the old unique index is removed.
- Identity consistency: namespace is absent from physical application/service keys; two namespace bindings map to one application ID and the same service instances; replicas share serviceId and differ by instanceId/leaseId.
- Pattern decision: the existing HTTP Management Client remains the Adapter and one Gateway Scope Facade owns joining/validation. No synchronization, Strategy, Factory or additional persistence layer is added because there is no second variation point.
- Validation boundary: Maven/Vitest/typecheck/lint/build prove source behavior; only Task 6 host PostgreSQL/Redis/multi-process and HTTP checks prove this local deployment. They do not prove production HA or multi-host behavior.
