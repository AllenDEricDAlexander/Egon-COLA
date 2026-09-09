package top.egon.cola.component.yuheng.test.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.tianshu.model.management.DdcInstanceStatus;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;
import top.egon.cola.component.yuheng.test.process.GatewayProcessHarness;
import top.egon.cola.component.yuheng.test.process.GatewayProcessSpec;
import top.egon.cola.component.yuheng.test.process.GatewayTestInfrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayLiveTopologyIT {

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private static final String ENV = "test";

    private static final String NAMESPACE = "yuheng-live";

    private static final String TIANSHU_RUNTIME_ACCESS_KEY =
            "yuheng-live-tianshu-runtime";

    private static final String TIANSHU_RUNTIME_SECRET_KEY =
            "yuheng-live-tianshu-runtime-secret-at-least-32-bytes";

    private static final String TIANSHU_REGISTRY_ACCESS_KEY =
            "yuheng-live-tianshu-registry";

    private static final String TIANSHU_REGISTRY_SECRET_KEY =
            "yuheng-live-tianshu-registry-secret-at-least-32-bytes";

    private static final String TIANSHU_MANAGEMENT_ACCESS_KEY =
            "yuheng-live-tianshu-management";

    private static final String MCP_TIANSHU_RUNTIME_ACCESS_KEY = "yuheng-live-mcp-tianshu-runtime";
    private static final String MCP_TIANSHU_RUNTIME_SECRET_KEY = "yuheng-live-mcp-runtime-secret-at-least-32-bytes";
    private static final String MCP_TIANSHU_REGISTRY_ACCESS_KEY = "yuheng-live-mcp-tianshu-registry";
    private static final String MCP_TIANSHU_REGISTRY_SECRET_KEY = "yuheng-live-mcp-registry-secret-at-least-32-bytes";

    private static final String TIANSHU_MANAGEMENT_SECRET_KEY =
            "yuheng-live-tianshu-management-secret-at-least-32-bytes";

    private static final String APPLICATION_CODE =
            "yuheng-test-http-provider";

    private static final String SERVICE_VERSION = "1.0.0-live";

    private static final String RPC_APPLICATION_CODE =
            "yuheng-test-rpc-provider";

    private static final String RPC_SERVICE_NAME =
            "egon.yuheng.test.v1.EchoService";

    private static final String RPC_SERVICE_GROUP = "default";

    private static final String RPC_SERVICE_VERSION = "1.0.0";

    private static final String RPC_METHOD_NAME =
            "egon.yuheng.test.v1.EchoService/Echo";

    private static final String RPC_YUHENG_SERVICE_NAME =
            "egon-yuheng-rpc";

    private static final byte[] ADMIN_JWT_SECRET =
            "yuheng-live-jwt-secret-32-bytes!".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
            );

    private static final String ADMIN_TOKEN = adminToken();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final Map<Integer, Integer> ddcRpcPorts = new LinkedHashMap<>();

    void verifyHttpProvidersLifecycle() throws Exception {
        try (GatewayLiveEnvironment environment =
                     new GatewayLiveEnvironment("http-topology")) {
            environment.startInfrastructure();
            GatewayTestInfrastructure infrastructure =
                    environment.infrastructure();
            GatewayProcessHarness processes = environment.processes();
            infrastructure.createDatabase("gateway_ddc");
            infrastructure.createDatabase("gateway_admin");

            int ddcPort = GatewayProcessHarness.availablePort();
            int adminPort = GatewayProcessHarness.availablePort();
            int providerPort = GatewayProcessHarness.availablePort();
            int secondProviderPort =
                    GatewayProcessHarness.availablePort();
            int engineManagementPort = GatewayProcessHarness.availablePort();
            int enginePublicPort = GatewayProcessHarness.availablePort();
            int engineInternalPort = GatewayProcessHarness.availablePort();
            int secondEngineManagementPort =
                    GatewayProcessHarness.availablePort();
            int secondEnginePublicPort =
                    GatewayProcessHarness.availablePort();
            int secondEngineInternalPort =
                    GatewayProcessHarness.availablePort();
            URI ddcBase = URI.create("http://127.0.0.1:" + ddcPort);
            URI adminBase = URI.create("http://127.0.0.1:" + adminPort);
            GatewayAdminTestClient adminClient =
                    new GatewayAdminTestClient(adminBase, ADMIN_TOKEN);

            var ddc = processes.start(ddcSpec(
                    infrastructure,
                    ddcPort
            ));
            processes.awaitHttp(
                    ddcBase.resolve("/actuator/health/readiness"),
                    STARTUP_TIMEOUT,
                    ddc
            );

            var admin = processes.start(adminSpec(
                    infrastructure,
                    ddcBase,
                    adminPort
            ));
            processes.awaitHttp(
                    adminBase.resolve("/actuator/health/readiness"),
                    STARTUP_TIMEOUT,
                    admin
            );

            JsonNode application = adminClient.createApplication(
                    Map.of(
                            "applicationCode", APPLICATION_CODE,
                            "displayName", "Gateway Live HTTP Provider",
                            "env", ENV,
                            "namespace", NAMESPACE,
                            "description", "GWS-13 live topology"
                    )
            );
            String applicationId = application.required("id").asText();
            JsonNode credential = adminClient.createCredential(applicationId);

            var provider = processes.start(providerSpec(
                    infrastructure,
                    ddcBase,
                    adminBase,
                    providerPort,
                    "http-provider-one",
                    "http-provider-live-1",
                    true,
                    credential.required("accessKey").asText(),
                    credential.required("secret").asText()
            ));
            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + providerPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    provider
            );
            Path engineData = environment.dataDirectory(
                    "yuheng-biz-gateway-1"
            );
            var engine = processes.start(engineSpec(
                    infrastructure,
                    ddcBase,
                    engineManagementPort,
                    enginePublicPort,
                    engineInternalPort,
                    engineData
            ));
            Path secondEngineData = environment.dataDirectory(
                    "yuheng-biz-gateway-2"
            );
            var secondEngine = processes.start(engineSpec(
                    infrastructure,
                    ddcBase,
                    secondEngineManagementPort,
                    secondEnginePublicPort,
                    secondEngineInternalPort,
                    secondEngineData,
                    false,
                    0,
                    "yuheng-biz-gateway-2"
            ));

            List<GatewayProcessHarness.ChildProcess> mcpEngines = startMcpEngines(environment, ddcBase);
            List<GatewayProcessHarness.ChildProcess> allEngines = new ArrayList<>(List.of(engine, secondEngine));
            allEngines.addAll(mcpEngines);

            awaitHttpProviderProjection(
                    processes,
                    adminClient,
                    "http-provider-live-1",
                    admin
            );
            String operationId = awaitOperation(
                    processes,
                    adminClient,
                    applicationId
            );
            String inventoryOperationId = awaitOperation(
                    processes,
                    adminClient,
                    applicationId,
                    "GET /api/internal/inventory/{sku}"
            );
            String providerIdentityOperationId = awaitOperation(
                    processes,
                    adminClient,
                    applicationId,
                    "GET /api/providers/{requestId}"
            );
            JsonNode group = adminClient.createGroup(
                    Map.of(
                            "gatewayGroupCode", "default",
                            "displayName", "Gateway Live Group",
                            "env", ENV,
                            "namespace", NAMESPACE,
                            "description", "GWS-13 live topology"
                    )
            );
            String groupId = group.required("id").asText();

            awaitEngineConfigClients(
                    processes,
                    adminClient,
                    groupId,
                    allEngines
            );
            JsonNode mutation = adminClient.putRoute(
                    groupId,
                    "live-http-order",
                    Map.of(
                            "operationId", operationId,
                            "content", Map.of(
                                    "host", "api.yuheng.test",
                                    "httpMethod", "GET",
                                    "pathPattern", "/api/orders/{id}",
                                    "accessZones", List.of(
                                            "PUBLIC",
                                            "INTERNAL"
                                    ),
                                    "priority", 0
                            ),
                            "enabled", true,
                            "expectedRevision", 0,
                            "idempotencyKey", "live-http-order-route",
                            "changeReason", "GWS-13 live HTTP route"
                    )
            );
            long revision = mutation.required("revision").asLong();
            mutation = adminClient.putRoute(
                    groupId,
                    "live-http-inventory",
                    Map.of(
                            "operationId", inventoryOperationId,
                            "content", Map.of(
                                    "host", "internal.yuheng.test",
                                    "httpMethod", "GET",
                                    "pathPattern",
                                    "/api/internal/inventory/{sku}",
                                    "accessZones", List.of("INTERNAL"),
                                    "priority", 0
                            ),
                            "enabled", true,
                            "expectedRevision", revision,
                            "idempotencyKey",
                            "live-http-inventory-route",
                            "changeReason",
                            "GWS-13 live internal route"
                    )
            );
            revision = mutation.required("revision").asLong();
            mutation = adminClient.putRoute(
                    groupId,
                    "live-http-provider-identity",
                    Map.of(
                            "operationId", providerIdentityOperationId,
                            "content", Map.of(
                                    "host", "providers.yuheng.test",
                                    "httpMethod", "GET",
                                    "pathPattern",
                                    "/api/providers/{requestId}",
                                    "accessZones", List.of(
                                            "PUBLIC",
                                            "INTERNAL"
                                    ),
                                    "priority", 0
                            ),
                            "enabled", true,
                            "expectedRevision", revision,
                            "idempotencyKey",
                            "live-http-provider-identity-route",
                            "changeReason",
                            "GWS-13 shared Provider identity route"
                    )
            );
            revision = mutation.required("revision").asLong();
            mutation = adminClient.putPolicy(
                    groupId,
                    "live-http-rate",
                    Map.of(
                            "policyType", "RATE_LIMIT",
                            "policyScope", "OPERATION",
                            "content", Map.of(
                                    "operationIds", List.of(operationId),
                                    "keyExpression", "${operationId}",
                                    "capacity", 1,
                                    "initialTokens", 1,
                                    "refillTokens", 1,
                                    "refillPeriod", "PT1H",
                                    "mode", "DISTRIBUTED"
                            ),
                            "enabled", true,
                            "expectedRevision", revision,
                            "idempotencyKey", "live-http-rate-policy",
                            "changeReason",
                            "GWS-13 live rate-limit policy"
                    )
            );
            revision = mutation.required("revision").asLong();
            JsonNode validation = adminClient.validateDraft(groupId);
            assertThat(validation.required("valid").asBoolean())
                    .as("HTTP draft validation response: %s", validation)
                    .isTrue();
            JsonNode release = adminClient.release(
                    groupId,
                    Map.of(
                            "expectedDraftRevision", revision,
                            "changeReason", "GWS-13 live HTTP release"
                    )
            );
            assertThat(release.required("status").asText())
                    .as("HTTP v1 release response: %s", release)
                    .isEqualTo("SUCCESS");
            String v1ReleaseId = release.required("releaseId").asText();
            processes.awaitCondition(
                    () -> engineNodeCount(adminClient, groupId) >= 2,
                    Duration.ofSeconds(30),
                    "two Engine nodes visible in Gateway Admin"
            );
            awaitRuntimeConsistency(
                    processes,
                    adminClient,
                    groupId,
                    v1ReleaseId
            );
            assertMcpRestartIsIndependent(environment, mcpEngines.getFirst(), List.of(engine, secondEngine));
            awaitRuntimeConsistency(processes, adminClient, groupId, v1ReleaseId);
            revision = adminClient.getDraft(groupId)
                    .required("revision")
                    .asLong();

            mutation = adminClient.putRoute(
                    groupId,
                    "live-http-provider-identity",
                    Map.of(
                            "operationId", providerIdentityOperationId,
                            "content", Map.of(
                                    "host", "providers.yuheng.test",
                                    "httpMethod", "GET",
                                    "pathPattern",
                                    "/api/providers/{requestId}",
                                    "accessZones", List.of(
                                            "PUBLIC",
                                            "INTERNAL"
                                    ),
                                    "priority", 1
                            ),
                            "enabled", true,
                            "expectedRevision", revision,
                            "idempotencyKey",
                            "live-http-provider-identity-route-v2",
                            "changeReason",
                            "GWS-13 live HTTP v2 route priority"
                    )
            );
            revision = mutation.required("revision").asLong();
            JsonNode v2Release = adminClient.release(
                    groupId,
                    Map.of(
                            "expectedDraftRevision", revision,
                            "changeReason", "GWS-13 live HTTP v2 release"
                    )
            );
            assertThat(v2Release.required("status").asText())
                    .as("HTTP v2 release response: %s", v2Release)
                    .isEqualTo("SUCCESS");
            awaitRuntimeConsistency(
                    processes,
                    adminClient,
                    groupId,
                    v2Release.required("releaseId").asText()
            );
            revision = adminClient.getDraft(groupId)
                    .required("revision")
                    .asLong();

            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + engineManagementPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    engine
            );
            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + secondEngineManagementPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    secondEngine
            );

            String traceId = "11111111111111111111111111111111";
            HttpResponse<String> gatewayResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:"
                                            + secondEnginePublicPort
                                            + "/api/orders/order-live-1"
                            ))
                            .header("Host", "api.yuheng.test")
                            .header("X-Trace-ID", traceId)
                            .header("X-Request-Source", "yuheng-live-test")
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(gatewayResponse.statusCode()).isEqualTo(200);
            JsonNode forwarded = objectMapper.readTree(
                    gatewayResponse.body()
            );
            assertThat(forwarded.required("id").asText())
                    .isEqualTo("order-live-1");
            assertThat(forwarded.required("source").asText())
                    .isEqualTo("yuheng-live-test");
            assertThat(gatewayResponse.headers()
                    .firstValue("X-Trace-ID"))
                    .contains(traceId);

            HttpResponse<String> rateLimited = httpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:"
                                            + enginePublicPort
                                            + "/api/orders/order-live-2"
                            ))
                            .header("Host", "api.yuheng.test")
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(rateLimited.statusCode()).isEqualTo(429);
            assertThat(rateLimited.body())
                    .contains("YUHENG_RATE_LIMITED");

            HttpResponse<String> publicInternal = inventory(
                    enginePublicPort,
                    "sku-public"
            );
            assertThat(publicInternal.statusCode()).isEqualTo(404);

            HttpResponse<String> internal = inventory(
                    engineInternalPort,
                    "sku-live"
            );
            assertThat(internal.statusCode()).isEqualTo(200);
            assertThat(objectMapper.readTree(internal.body())
                    .required("providerId").asText())
                    .isEqualTo("http-provider-live-1");

            var secondProvider = processes.start(webFluxProviderSpec(
                    infrastructure,
                    ddcBase,
                    adminBase,
                    secondProviderPort,
                    "webflux-http-provider",
                    "http-provider-live-2",
                    false,
                    credential.required("accessKey").asText(),
                    credential.required("secret").asText()
            ));
            URI secondProviderReadiness = URI.create(
                    "http://127.0.0.1:"
                            + secondProviderPort
                            + "/actuator/health/readiness"
            );
            processes.awaitHttp(
                    secondProviderReadiness,
                    STARTUP_TIMEOUT,
                    secondProvider
            );
            processes.awaitCondition(
                    () -> {
                        JsonNode projection = adminClient.providerInstances(
                                ENV,
                                NAMESPACE,
                                "HTTP",
                                APPLICATION_CODE,
                                "default",
                                SERVICE_VERSION
                        );
                        Set<String> providerIds = providerIds(projection);
                        if (providerIds.containsAll(Set.of(
                                "http-provider-live-1",
                                "http-provider-live-2"
                        ))) {
                            return true;
                        }
                        throw new AssertionError(
                                "HTTP Provider projection: " + projection
                        );
                    },
                    Duration.ofSeconds(30),
                    "MVC and WebFlux Provider projection"
            );
            Map<String, String> initialLeases = providerLeases(adminClient);
            assertThat(initialLeases).containsKeys(
                    "http-provider-live-1",
                    "http-provider-live-2"
            );
            assertThat(awaitProviderFrameworks(
                    processes,
                    enginePublicPort,
                    Set.of("mvc", "webflux")
            )).containsExactlyInAnyOrder("mvc", "webflux");

            processes.stop(provider);
            processes.awaitCondition(
                    () -> !httpProviderIds(adminClient).contains(
                            "http-provider-live-1"
                    ),
                    Duration.ofSeconds(30),
                    "stopped HTTP Provider removal"
            );
            awaitOnlyProviderFramework(
                    processes,
                    enginePublicPort,
                    "webflux"
            );

            provider = processes.restart(provider);
            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + providerPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    provider
            );
            processes.awaitCondition(
                    () -> !initialLeases.get("http-provider-live-1")
                            .equals(providerLeases(adminClient).get(
                                    "http-provider-live-1"
                            )),
                    Duration.ofSeconds(30),
                    "restarted MVC Provider lease replacement"
            );
            assertThat(awaitProviderFrameworks(
                    processes,
                    enginePublicPort,
                    Set.of("mvc", "webflux")
            )).containsExactlyInAnyOrder("mvc", "webflux");

            processes.kill(secondProvider);
            processes.awaitCondition(
                    () -> !httpProviderIds(adminClient).contains(
                            "http-provider-live-2"
                    ),
                    Duration.ofSeconds(45),
                    "force-killed WebFlux Provider lease expiry"
            );
            awaitOnlyProviderFramework(
                    processes,
                    enginePublicPort,
                    "mvc"
            );

            secondProvider = processes.restart(secondProvider);
            processes.awaitHttp(
                    secondProviderReadiness,
                    STARTUP_TIMEOUT,
                    secondProvider
            );
            processes.awaitCondition(
                    () -> !initialLeases.get("http-provider-live-2")
                            .equals(providerLeases(adminClient).get(
                                    "http-provider-live-2"
                            )),
                    Duration.ofSeconds(30),
                    "restarted WebFlux Provider lease replacement"
            );
            assertThat(awaitProviderFrameworks(
                    processes,
                    enginePublicPort,
                    Set.of("mvc", "webflux")
            )).containsExactlyInAnyOrder("mvc", "webflux");

            processes.awaitCondition(
                    () -> traceCount(adminClient, traceId) == 1,
                    Duration.ofSeconds(30),
                    "Kafka call event projection in Gateway Admin"
            );

            JsonNode rollback = adminClient.rollback(
                    groupId,
                    Map.of(
                            "sourceReleaseId", v1ReleaseId,
                            "expectedDraftRevision", revision,
                            "changeReason", "GWS-13 rollback to v1 content"
                    )
            );
            assertThat(rollback.required("status").asText())
                    .as("HTTP rollback response: %s", rollback)
                    .isEqualTo("SUCCESS");
            assertThat(rollback.required("rollbackOfReleaseId").asText())
                    .isEqualTo(v1ReleaseId);
            awaitRuntimeConsistency(
                    processes,
                    adminClient,
                    groupId,
                    rollback.required("releaseId").asText()
            );
        }
    }

    void verifyRpcDualEngineLifecycle() throws Exception {
        try (GatewayLiveEnvironment environment =
                     new GatewayLiveEnvironment("rpc-topology")) {
            environment.startInfrastructure();
            GatewayTestInfrastructure infrastructure =
                    environment.infrastructure();
            GatewayProcessHarness processes = environment.processes();
            infrastructure.createDatabase("gateway_ddc");
            infrastructure.createDatabase("gateway_admin");

            int ddcPort = GatewayProcessHarness.availablePort();
            int adminPort = GatewayProcessHarness.availablePort();
            int providerManagementPort =
                    GatewayProcessHarness.availablePort();
            int providerRpcPort = GatewayProcessHarness.availablePort();
            int engineManagementPort =
                    GatewayProcessHarness.availablePort();
            int enginePublicPort = GatewayProcessHarness.availablePort();
            int engineInternalPort = GatewayProcessHarness.availablePort();
            int engineRpcPort = GatewayProcessHarness.availablePort();
            int secondEngineManagementPort =
                    GatewayProcessHarness.availablePort();
            int secondEnginePublicPort =
                    GatewayProcessHarness.availablePort();
            int secondEngineInternalPort =
                    GatewayProcessHarness.availablePort();
            int secondEngineRpcPort =
                    GatewayProcessHarness.availablePort();
            int consumerPort = GatewayProcessHarness.availablePort();
            URI ddcBase = URI.create("http://127.0.0.1:" + ddcPort);
            URI adminBase = URI.create("http://127.0.0.1:" + adminPort);
            GatewayAdminTestClient adminClient =
                    new GatewayAdminTestClient(adminBase, ADMIN_TOKEN);

            var ddc = processes.start(ddcSpec(
                    infrastructure,
                    ddcPort
            ));
            processes.awaitHttp(
                    ddcBase.resolve("/actuator/health/readiness"),
                    STARTUP_TIMEOUT,
                    ddc
            );

            var admin = processes.start(adminSpec(
                    infrastructure,
                    ddcBase,
                    adminPort
            ));
            processes.awaitHttp(
                    adminBase.resolve("/actuator/health/readiness"),
                    STARTUP_TIMEOUT,
                    admin
            );

            JsonNode application = adminClient.createApplication(
                    Map.of(
                            "applicationCode", RPC_APPLICATION_CODE,
                            "displayName", "Gateway Live RPC Provider",
                            "env", ENV,
                            "namespace", NAMESPACE,
                            "description", "GWS-13 live RPC topology"
                    )
            );
            String applicationId = application.required("id").asText();
            JsonNode credential = adminClient.createCredential(applicationId);

            var provider = processes.start(rpcProviderSpec(
                    infrastructure,
                    ddcBase,
                    adminBase,
                    providerManagementPort,
                    providerRpcPort,
                    credential.required("accessKey").asText(),
                    credential.required("secret").asText()
            ));
            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + providerManagementPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    provider
            );

            Path engineData = environment.dataDirectory(
                    "yuheng-biz-gateway-1"
            );
            var engine = processes.start(engineSpec(
                    infrastructure,
                    ddcBase,
                    engineManagementPort,
                    enginePublicPort,
                    engineInternalPort,
                    engineData,
                    true,
                    engineRpcPort
            ));
            Path secondEngineData = environment.dataDirectory(
                    "yuheng-biz-gateway-2"
            );
            var secondEngine = processes.start(engineSpec(
                    infrastructure,
                    ddcBase,
                    secondEngineManagementPort,
                    secondEnginePublicPort,
                    secondEngineInternalPort,
                    secondEngineData,
                    true,
                    secondEngineRpcPort,
                    "yuheng-biz-gateway-2"
            ));

            List<GatewayProcessHarness.ChildProcess> mcpEngines = startMcpEngines(environment, ddcBase);
            List<GatewayProcessHarness.ChildProcess> allEngines = new ArrayList<>(List.of(engine, secondEngine));
            allEngines.addAll(mcpEngines);

            String operationId = awaitOperation(
                    processes,
                    adminClient,
                    applicationId,
                    RPC_METHOD_NAME
            );
            JsonNode group = adminClient.createGroup(
                    Map.of(
                            "gatewayGroupCode", "default",
                            "displayName", "Gateway Live RPC Group",
                            "env", ENV,
                            "namespace", NAMESPACE,
                            "description", "GWS-13 live RPC topology"
                    )
            );
            String groupId = group.required("id").asText();

            awaitEngineConfigClients(
                    processes,
                    adminClient,
                    groupId,
                    allEngines
            );
            JsonNode mutation = adminClient.putRoute(
                    groupId,
                    "live-rpc-echo",
                    Map.of(
                            "operationId", operationId,
                            "content", Map.of(
                                    "host", "rpc.yuheng.test",
                                    "httpMethod", "POST",
                                    "pathPattern", "/rpc/echo",
                                    "accessZones", List.of("INTERNAL"),
                                    "priority", 0
                            ),
                            "enabled", true,
                            "expectedRevision", 0,
                            "idempotencyKey", "live-rpc-echo-route",
                            "changeReason", "GWS-13 live RPC route"
                    )
            );
            long revision = mutation.required("revision").asLong();
            JsonNode validation = adminClient.validateDraft(groupId);
            assertThat(validation.required("valid").asBoolean())
                    .as("RPC draft validation response: %s", validation)
                    .isTrue();
            JsonNode release = adminClient.release(
                    groupId,
                    Map.of(
                            "expectedDraftRevision", revision,
                            "changeReason", "GWS-13 live RPC release"
                    )
            );
            assertThat(release.required("status").asText())
                    .as("RPC release response: %s", release)
                    .isEqualTo("SUCCESS");
            processes.awaitCondition(
                    () -> engineNodeCount(adminClient, groupId) >= 2,
                    Duration.ofSeconds(30),
                    "two RPC Engine nodes visible in Gateway Admin"
            );

            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + engineManagementPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    engine
            );
            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + secondEngineManagementPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    secondEngine
            );
            awaitRpcProviderProjection(processes, adminClient, admin);

            awaitRuntimeConsistency(processes, adminClient, groupId,
                    release.required("releaseId").asText());

            var consumer = processes.start(rpcConsumerSpec(
                    infrastructure,
                    ddcBase,
                    consumerPort
            ));
            URI consumerBase = URI.create(
                    "http://127.0.0.1:" + consumerPort
            );
            processes.awaitHttp(
                    consumerBase.resolve("/actuator/health/readiness"),
                    STARTUP_TIMEOUT,
                    consumer
            );

            String traceId = "22222222222222222222222222222222";
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(consumerBase.resolve(
                                    "/test/rpc/echo?message=through-gateway"
                            ))
                            .header("X-Trace-ID", traceId)
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(response.statusCode())
                    .as(processes.output(consumer))
                    .isEqualTo(200);
            JsonNode forwarded = objectMapper.readTree(response.body());
            assertThat(forwarded.required("providerId").asText())
                    .isEqualTo("rpc-provider-live");
            assertThat(forwarded.required("message").asText())
                    .isEqualTo("through-gateway");
            assertThat(forwarded.required("traceId").asText())
                    .isEqualTo(traceId);

            String httpRpcTraceId =
                    "33333333333333333333333333333333";
            HttpResponse<String> httpRpcResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:"
                                            + engineInternalPort
                                            + "/rpc/echo"
                            ))
                            .header("Host", "rpc.yuheng.test")
                            .header("Content-Type", "application/json")
                            .header("X-Trace-ID", httpRpcTraceId)
                            .timeout(Duration.ofSeconds(10))
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"message\":\"through-http-yuheng.}"
                            ))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(httpRpcResponse.statusCode())
                    .as(processes.output(engine))
                    .isEqualTo(200);
            JsonNode httpRpc = objectMapper.readTree(
                    httpRpcResponse.body()
            );
            assertThat(httpRpc.required("providerId").asText())
                    .isEqualTo("rpc-provider-live");
            assertThat(httpRpc.required("message").asText())
                    .isEqualTo("through-http-gateway");
            assertThat(httpRpc.required("traceId").asText())
                    .isEqualTo(httpRpcTraceId);

            processes.awaitCondition(
                    () -> traceCount(adminClient, traceId) == 1,
                    Duration.ofSeconds(30),
                    "RPC Kafka call event projection in Gateway Admin"
            );
            processes.awaitCondition(
                    () -> traceCount(adminClient, httpRpcTraceId) == 1,
                    Duration.ofSeconds(30),
                    "HTTP to RPC Kafka call event projection in Gateway Admin"
            );

            Map<String, String> initialEngineLeases = engineLeases(
                    adminClient,
                    groupId
            );
            assertThat(initialEngineLeases)
                    .hasSize(2)
                    .allSatisfy((instanceId, leaseId) -> {
                        assertThat(instanceId).isNotBlank();
                        assertThat(leaseId).isNotBlank();
                    });
            Set<String> initialEngineIds = initialEngineLeases.keySet();
            Set<String> expectedEngineAliases = Set.of(
                    "yuheng-biz-gateway-1",
                    "yuheng-biz-gateway-2"
            );
            assertThat(awaitRpcEngineSelections(
                    processes,
                    adminClient,
                    consumerBase,
                    expectedEngineAliases
            )).containsExactlyInAnyOrderElementsOf(expectedEngineAliases);

            processes.stop(engine);
            processes.awaitCondition(
                    () -> {
                        Set<String> currentEngineIds = engineLeases(
                                adminClient,
                                groupId
                        ).keySet();
                        return currentEngineIds.size() == 1
                                && initialEngineIds.containsAll(currentEngineIds);
                    },
                    Duration.ofSeconds(30),
                    "stopped RPC Engine removal"
            );
            Map<String, String> survivingEngineLeases = engineLeases(
                    adminClient,
                    groupId
            );
            String survivingEngineId = survivingEngineLeases.keySet()
                    .iterator()
                    .next();
            String stoppedEngineId = initialEngineIds.stream()
                    .filter(instanceId -> !instanceId.equals(
                            survivingEngineId
                    ))
                    .findFirst()
                    .orElseThrow();
            assertRpcConsumerEngine(
                    processes,
                    adminClient,
                    consumerBase,
                    "yuheng-biz-gateway-2"
            );

            engine = processes.restart(engine);
            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + engineManagementPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    engine
            );
            processes.awaitCondition(
                    () -> {
                        Map<String, String> currentEngineLeases =
                                engineLeases(adminClient, groupId);
                        if (currentEngineLeases.size() != 2
                                || !currentEngineLeases.containsKey(
                                survivingEngineId)) {
                            return false;
                        }
                        String currentStoppedLease =
                                currentEngineLeases.get(stoppedEngineId);
                        return currentStoppedLease == null
                                || !currentStoppedLease.equals(
                                initialEngineLeases.get(stoppedEngineId)
                        );
                    },
                    Duration.ofSeconds(30),
                    "restarted RPC Engine lease replacement"
            );
            assertThat(awaitRpcEngineSelections(
                    processes,
                    adminClient,
                    consumerBase,
                    expectedEngineAliases
            )).containsExactlyInAnyOrderElementsOf(expectedEngineAliases);
        }
    }

    @Test
    void everyDdcClientUsesInfrastructureRedisCoordinates() {
        GatewayTestInfrastructure infrastructure = testInfrastructure();

        URI ddcBase = URI.create("http://127.0.0.1:18070");
        URI adminBase = URI.create("http://127.0.0.1:18080");
        GatewayProcessSpec httpProvider = providerSpec(
                infrastructure,
                ddcBase,
                adminBase,
                18084,
                "http-provider-one",
                "http-provider-live-1",
                true,
                "access-key",
                "secret-key"
        );
        List<GatewayProcessSpec> ddcClients = List.of(
                httpProvider,
                providerSpec(
                        infrastructure,
                        ddcBase,
                        adminBase,
                        18085,
                        "http-provider-two",
                        "http-provider-live-2",
                        false,
                        "access-key",
                        "secret-key"
                ),
                rpcProviderSpec(
                        infrastructure,
                        ddcBase,
                        adminBase,
                        18086,
                        19091,
                        "access-key",
                        "secret-key"
                ),
                rpcConsumerSpec(infrastructure, ddcBase, 18087),
                engineSpec(
                        infrastructure,
                        ddcBase,
                        18088,
                        18089,
                        18090,
                        Path.of("target/engine-one")
                ),
                engineSpec(
                        infrastructure,
                        ddcBase,
                        18091,
                        18092,
                        18093,
                        Path.of("target/engine-two"),
                        true,
                        19092,
                        "yuheng-biz-gateway-2"
                )
        );

        assertSoftly(softly -> {
            ddcClients.forEach(spec -> softly.assertThat(spec.arguments())
                    .as(spec.name())
                    .contains(
                            "--egon.cola.component.tianshu.rpc.target="
                                    + ddcRpcTarget(ddcBase),
                            "--egon.cola.component.tianshu.redis.host="
                                    + infrastructure.ddcRedisHost(),
                            "--egon.cola.component.tianshu.redis.port="
                                    + infrastructure.ddcRedisPort()
                    ));
        });
    }

    @Test
    void dualRoleSpecsShareReleaseScopeButNotIdentityCredentialsOrState() {
        var infrastructure = testInfrastructure();
        URI ddc = URI.create("http://127.0.0.1:18070");
        var api = engineSpec(infrastructure, ddc, 18083, 18081, 18082,
                Path.of("target/api"), false, 0, "api-1");
        var mcp = mcpEngineSpec(infrastructure, ddc, 18085, 18084, Path.of("target/mcp"), "mcp-1");
        assertThat(api.engineRole()).isEqualTo(GatewayEngineRoleEnum.API_RPC);
        assertThat(mcp.engineRole()).isEqualTo(GatewayEngineRoleEnum.MCP);
        for (var spec : List.of(api, mcp)) {
            assertThat(spec.arguments()).contains("--egon.cola.component.tianshu.biz-code=infra",
                    "--egon.cola.component.tianshu.app-code=ge", "--egon.cola.component.tianshu.env=" + ENV,
                    "--egon.cola.component.tianshu.namespace=" + NAMESPACE,
                    "--egon.cola.component.tianshu.instance.id=" + spec.name(),
                    "--egon.cola.component.tianshu.registry.http.instance-id=" + spec.name());
        }
        assertThat(api.arguments()).contains("--egon.cola.component.yuheng.engine.data-directory=target/api")
                .noneMatch(argument -> argument.startsWith("--spring.datasource.")
                        || argument.contains("yuheng.engine.mcp."));
        assertThat(mcp.arguments()).contains("--egon.cola.component.yuheng.mcp-engine.data-directory=target/mcp",
                "--spring.datasource.url=jdbc:postgresql://db:5432/gateway_admin",
                "--egon.cola.component.tianshu.rpc.auth.runtime.access-key=" + MCP_TIANSHU_RUNTIME_ACCESS_KEY)
                .noneMatch(argument -> argument.contains("yuheng.engine.http.")
                        || argument.contains("yuheng.engine.rpc."));
        assertThat(api.arguments()).contains("--egon.cola.component.tianshu.rpc.auth.runtime.access-key=" + TIANSHU_RUNTIME_ACCESS_KEY);
        assertThat(mcp.redactedArguments()).noneMatch(argument -> argument.contains(MCP_TIANSHU_RUNTIME_SECRET_KEY)
                || argument.contains(MCP_TIANSHU_REGISTRY_SECRET_KEY));
        assertThat(ddcSpec(infrastructure, 18070).arguments()).contains(
                "--egon.cola.component.tianshu.admin.rpc.credentials[3].access-key=" + MCP_TIANSHU_RUNTIME_ACCESS_KEY,
                "--egon.cola.component.tianshu.admin.rpc.credentials[4].access-key=" + MCP_TIANSHU_REGISTRY_ACCESS_KEY);
    }

    @Test
    void httpProviderSpecUsesSingleServiceVersionSource() {
        GatewayTestInfrastructure infrastructure = testInfrastructure();
        URI ddcBase = URI.create("http://127.0.0.1:18070");
        URI adminBase = URI.create("http://127.0.0.1:18080");
        GatewayProcessSpec httpProvider = providerSpec(
                infrastructure,
                ddcBase,
                adminBase,
                18084,
                "http-provider-one",
                "http-provider-live-1",
                true,
                "access-key",
                "secret-key"
        );

        assertThat(httpProvider.arguments())
                .contains("--yuheng.test.service-version=1.0.0-live")
                .noneMatch(argument -> argument.startsWith(
                        "--egon.cola.component.yuheng.reporting."
                                + "artifact-version="
                ));
    }

    @Test
    void ddcAdminSpecUsesExplicitLocalDevelopmentSecurityMode() {
        GatewayProcessSpec ddc = ddcSpec(testInfrastructure(), 18070);

        assertThat(ddc.arguments()).contains(
                "--egon.cola.component.tianshu.admin.security.local-dev=true",
                "--egon.cola.component.tianshu.admin.security.jwt."
                        + "hmac-secret-base64="
                        + Base64.getEncoder().encodeToString(
                        ADMIN_JWT_SECRET
                ),
                "--egon.cola.component.rpc.provider.port="
                        + ddcRpcPort(18070),
                "--egon.cola.component.rpc.provider.registration-mode=DISABLED",
                "--egon.cola.component.tianshu.admin.rpc."
                        + "signature-enabled=true",
                "--egon.cola.component.tianshu.admin.rpc.credentials[0]."
                        + "credential-id=yuheng-live-runtime",
                "--egon.cola.component.tianshu.admin.rpc.credentials[0]."
                        + "access-key=" + TIANSHU_RUNTIME_ACCESS_KEY,
                "--egon.cola.component.tianshu.admin.rpc.credentials[0]."
                        + "secret=" + TIANSHU_RUNTIME_SECRET_KEY,
                "--egon.cola.component.tianshu.admin.rpc.credentials[0]."
                        + "client-type=SDK",
                "--egon.cola.component.tianshu.admin.rpc.credentials[1]."
                        + "credential-id=yuheng-live-registry",
                "--egon.cola.component.tianshu.admin.rpc.credentials[1]."
                        + "client-type=REGISTRY",
                "--egon.cola.component.tianshu.admin.rpc.credentials[2]."
                        + "credential-id=yuheng-live-management",
                "--egon.cola.component.tianshu.admin.rpc.credentials[2]."
                        + "client-type=MANAGEMENT"
        );
    }

    @Test
    void gatewayAdminUsesInfrastructureRedisCoordinates() {
        GatewayTestInfrastructure infrastructure = testInfrastructure();
        GatewayProcessSpec admin = adminSpec(
                infrastructure,
                URI.create("http://127.0.0.1:18070"),
                18080
        );

        assertThat(admin.arguments()).contains(
                "--spring.data.redis.host="
                        + infrastructure.ddcRedisHost(),
                "--spring.data.redis.port="
                        + infrastructure.ddcRedisPort()
        );
    }

    @Test
    void rpcConsumerEnablesDdcServiceRegistry() {
        GatewayProcessSpec consumer = rpcConsumerSpec(
                testInfrastructure(),
                URI.create("http://127.0.0.1:18070"),
                18090
        );

        assertThat(consumer.arguments()).contains(
                "--egon.cola.component.tianshu.registry.enabled=true"
        );
    }

    private GatewayTestInfrastructure testInfrastructure() {
        GatewayTestInfrastructure infrastructure =
                mock(GatewayTestInfrastructure.class);
        when(infrastructure.ddcRedisHost()).thenReturn("tianshu-live-host");
        when(infrastructure.ddcRedisPort()).thenReturn(16379);
        when(infrastructure.rateLimitRedisHost()).thenReturn("rate-live-host");
        when(infrastructure.rateLimitRedisPort()).thenReturn(26379);
        when(infrastructure.kafkaBootstrapServers()).thenReturn("kafka:19092");
        when(infrastructure.jdbcUrl("gateway_admin")).thenReturn("jdbc:postgresql://db:5432/gateway_admin");
        when(infrastructure.postgresUsername()).thenReturn("yuheng-test");
        when(infrastructure.postgresPassword()).thenReturn("test-only-password");
        return infrastructure;
    }

    private GatewayProcessSpec.Builder ddcClient(
            GatewayProcessSpec.Builder builder,
            GatewayTestInfrastructure infrastructure,
            URI ddcBase) {
        return ddcRuntimeRpc(builder, ddcBase)
                .argument(
                        "egon.cola.component.tianshu.redis.host",
                        infrastructure.ddcRedisHost()
                )
                .argument(
                        "egon.cola.component.tianshu.redis.port",
                        infrastructure.ddcRedisPort()
                );
    }

    private GatewayProcessSpec.Builder ddcRuntimeRpc(
            GatewayProcessSpec.Builder builder,
            URI ddcBase) {
        return ddcRuntimeRpc(builder, ddcBase, GatewayEngineRoleEnum.API_RPC);
    }

    private GatewayProcessSpec.Builder ddcRuntimeRpc(
            GatewayProcessSpec.Builder builder, URI ddcBase, GatewayEngineRoleEnum role) {
        return builder
                .argument(
                        "egon.cola.component.tianshu.rpc.target",
                        ddcRpcTarget(ddcBase)
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.tls."
                                + "development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.auth.runtime.access-key",
                        role == GatewayEngineRoleEnum.MCP ? MCP_TIANSHU_RUNTIME_ACCESS_KEY : TIANSHU_RUNTIME_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.auth.runtime.secret-key",
                        role == GatewayEngineRoleEnum.MCP ? MCP_TIANSHU_RUNTIME_SECRET_KEY : TIANSHU_RUNTIME_SECRET_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.auth.registry.access-key",
                        role == GatewayEngineRoleEnum.MCP ? MCP_TIANSHU_REGISTRY_ACCESS_KEY : TIANSHU_REGISTRY_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.auth.registry.secret-key",
                        role == GatewayEngineRoleEnum.MCP ? MCP_TIANSHU_REGISTRY_SECRET_KEY : TIANSHU_REGISTRY_SECRET_KEY
                );
    }

    private String ddcRpcTarget(URI ddcBase) {
        return "dns:///127.0.0.1:" + ddcRpcPort(ddcBase.getPort());
    }

    private int ddcRpcPort(int httpPort) {
        return ddcRpcPorts.computeIfAbsent(
                httpPort,
                ignored -> availableDdcRpcPort()
        );
    }

    private int availableDdcRpcPort() {
        try {
            return GatewayProcessHarness.availablePort();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "failed to allocate Tianshu RPC port",
                    exception
            );
        }
    }

    private GatewayProcessSpec ddcSpec(
            GatewayTestInfrastructure infrastructure,
            int port) {
        GatewayProcessSpec.Builder builder = GatewayProcessSpec.builder(
                        "tianshu-admin",
                        "top.egon.cola.component.tianshu.admin."
                                + "DynamicConfigCenterAdminApplication"
                )
                .argument("server.port", port)
                .argument(
                        "spring.datasource.url",
                        infrastructure.jdbcUrl("gateway_ddc")
                )
                .argument(
                        "spring.datasource.username",
                        infrastructure.postgresUsername()
                )
                .argument(
                        "spring.datasource.password",
                        infrastructure.postgresPassword()
                )
                .argument(
                        "egon.cola.component.tianshu.admin.redis.host",
                        infrastructure.ddcRedisHost()
                )
                .argument(
                        "egon.cola.component.tianshu.admin.redis.port",
                        infrastructure.ddcRedisPort()
                )
                .argument(
                        "egon.cola.component.tianshu.admin.security.local-dev",
                        true
                )
                .argument(
                        "egon.cola.component.tianshu.admin.security.jwt."
                                + "hmac-secret-base64",
                        Base64.getEncoder().encodeToString(
                                ADMIN_JWT_SECRET
                        )
                )
                .argument("egon.cola.component.rpc.enabled", true)
                .argument("egon.cola.component.rpc.provider.enabled", true)
                .argument(
                        "egon.cola.component.rpc.provider.port",
                        ddcRpcPort(port)
                )
                .argument(
                        "egon.cola.component.rpc.provider.registration-mode",
                        "DISABLED"
                )
                .argument(
                        "egon.cola.component.rpc.tls.development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc.signature-enabled",
                        true
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].credential-id",
                        "yuheng-live-runtime"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].access-key",
                        TIANSHU_RUNTIME_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].secret",
                        TIANSHU_RUNTIME_SECRET_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].client-type",
                        "SDK"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].app-code-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].env-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].biz-code-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].namespace-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].allowed-operations[0]",
                        "SDK_REGISTER"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].allowed-operations[1]",
                        "SDK_HEARTBEAT"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].allowed-operations[2]",
                        "SDK_OFFLINE"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].allowed-operations[3]",
                        "CONFIG_PULL"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[0].allowed-operations[4]",
                        "PUBLISH_ACK"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].credential-id",
                        "yuheng-live-registry"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].access-key",
                        TIANSHU_REGISTRY_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].secret",
                        TIANSHU_REGISTRY_SECRET_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].client-type",
                        "REGISTRY"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].app-code-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].env-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].biz-code-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].namespace-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].allowed-operations[0]",
                        "REGISTRY_REGISTER"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].allowed-operations[1]",
                        "REGISTRY_HEARTBEAT"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].allowed-operations[2]",
                        "REGISTRY_DEREGISTER"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[1].allowed-operations[3]",
                        "REGISTRY_READ"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].credential-id",
                        "yuheng-live-management"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].access-key",
                        TIANSHU_MANAGEMENT_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].secret",
                        TIANSHU_MANAGEMENT_SECRET_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].client-type",
                        "MANAGEMENT"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].app-code-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].env-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].biz-code-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].namespace-patterns[0]",
                        "*"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[0]",
                        "MANAGEMENT_CONFIG_READ"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[1]",
                        "MANAGEMENT_CONFIG_WRITE"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[2]",
                        "MANAGEMENT_PUBLISH"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[3]",
                        "MANAGEMENT_TASK_READ"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[4]",
                        "MANAGEMENT_TASK_RETRY"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[5]",
                        "MANAGEMENT_INSTANCE_READ"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[6]",
                        "MANAGEMENT_SCOPE_READ"
                )
                .argument(
                        "egon.cola.component.tianshu.admin.rpc."
                                + "credentials[2].allowed-operations[7]",
                        "MANAGEMENT_REGISTRY_READ"
                )
                .startupTimeout(STARTUP_TIMEOUT);
        addDdcCredential(builder, 3, "yuheng-live-mcp-runtime", MCP_TIANSHU_RUNTIME_ACCESS_KEY,
                MCP_TIANSHU_RUNTIME_SECRET_KEY, "SDK",
                List.of("SDK_REGISTER", "SDK_HEARTBEAT", "SDK_OFFLINE", "CONFIG_PULL", "PUBLISH_ACK"));
        addDdcCredential(builder, 4, "yuheng-live-mcp-registry", MCP_TIANSHU_REGISTRY_ACCESS_KEY,
                MCP_TIANSHU_REGISTRY_SECRET_KEY, "REGISTRY",
                List.of("REGISTRY_REGISTER", "REGISTRY_HEARTBEAT", "REGISTRY_DEREGISTER", "REGISTRY_READ"));
        return builder.build();
    }

    private void addDdcCredential(GatewayProcessSpec.Builder builder, int index, String id,
                                  String accessKey, String secret, String clientType, List<String> operations) {
        String prefix = "egon.cola.component.tianshu.admin.rpc.credentials[" + index + "].";
        builder.argument(prefix + "credential-id", id).argument(prefix + "access-key", accessKey)
                .argument(prefix + "secret", secret).argument(prefix + "client-type", clientType);
        for (String dimension : List.of("app-code", "env", "biz-code", "namespace")) {
            builder.argument(prefix + dimension + "-patterns[0]", "*");
        }
        for (int operation = 0; operation < operations.size(); operation++) {
            builder.argument(prefix + "allowed-operations[" + operation + "]", operations.get(operation));
        }
    }

    private GatewayProcessSpec adminSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            int port) {
        String masterKey = Base64.getEncoder().encodeToString(
                "yuheng-live-master-key-32-byte!".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );
        return GatewayProcessSpec.builder(
                        "yuheng-admin",
                        "top.egon.cola.component.yuheng.admin."
                                + "GatewayAdminApplication"
                )
                .argument("server.port", port)
                .argument(
                        "spring.datasource.url",
                        infrastructure.jdbcUrl("gateway_admin")
                )
                .argument(
                        "spring.datasource.username",
                        infrastructure.postgresUsername()
                )
                .argument(
                        "spring.datasource.password",
                        infrastructure.postgresPassword()
                )
                .argument(
                        "spring.data.redis.host",
                        infrastructure.ddcRedisHost()
                )
                .argument(
                        "spring.data.redis.port",
                        infrastructure.ddcRedisPort()
                )
                .argument("yuheng.admin.tianshu.enabled", true)
                .argument(
                        "egon.cola.component.tianshu.rpc.target",
                        ddcRpcTarget(ddcBase)
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.tls."
                                + "development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.auth.management."
                                + "access-key",
                        TIANSHU_MANAGEMENT_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.tianshu.rpc.auth.management."
                                + "secret-key",
                        TIANSHU_MANAGEMENT_SECRET_KEY
                )
                .argument(
                        "yuheng.admin.definition-reconcile-delay",
                        "500ms"
                )
                .argument(
                        "yuheng.admin.secrets.master-key-base64",
                        masterKey
                )
                .argument(
                        "yuheng.admin.security.hmac-secret-base64",
                        Base64.getEncoder().encodeToString(
                                ADMIN_JWT_SECRET
                        )
                )
                .argument(
                        "yuheng.admin.observability.kafka.enabled",
                        true
                )
                .argument(
                        "yuheng.admin.observability.kafka."
                                + "bootstrap-servers",
                        infrastructure.kafkaBootstrapServers()
                )
                .startupTimeout(STARTUP_TIMEOUT)
                .build();
    }

    private GatewayProcessSpec providerSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            URI adminBase,
            int port,
            String processName,
            String providerId,
            boolean reportingEnabled,
            String accessKey,
            String secretKey) {
        return providerSpec(
                infrastructure,
                ddcBase,
                adminBase,
                port,
                processName,
                providerId,
                reportingEnabled,
                accessKey,
                secretKey,
                "top.egon.cola.component.yuheng.test.http."
                        + "GatewayHttpTestProviderApplication"
        );
    }

    private GatewayProcessSpec webFluxProviderSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            URI adminBase,
            int port,
            String processName,
            String providerId,
            boolean reportingEnabled,
            String accessKey,
            String secretKey) {
        return providerSpec(
                infrastructure,
                ddcBase,
                adminBase,
                port,
                processName,
                providerId,
                reportingEnabled,
                accessKey,
                secretKey,
                "top.egon.cola.component.yuheng.test.webflux."
                        + "GatewayWebFluxHttpTestProviderApplication"
        );
    }

    private GatewayProcessSpec providerSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            URI adminBase,
            int port,
            String processName,
            String providerId,
            boolean reportingEnabled,
            String accessKey,
            String secretKey,
            String mainClass) {
        return ddcClient(
                GatewayProcessSpec.builder(
                                processName,
                                mainClass
                        ),
                infrastructure,
                ddcBase
        )
                .argument("server.port", port)
                .argument("egon.cola.component.tianshu.enabled", true)
                .argument(
                        "egon.cola.component.tianshu.app-code",
                        APPLICATION_CODE
                )
                .argument("egon.cola.component.tianshu.env", ENV)
                .argument(
                        "egon.cola.component.tianshu.namespace",
                        NAMESPACE
                )
                .argument("egon.cola.component.tianshu.registry.enabled", true)
                .argument("yuheng.test.env", ENV)
                .argument("yuheng.test.namespace", NAMESPACE)
                .argument("yuheng.test.provider-id", providerId)
                .argument("yuheng.test.service-version", SERVICE_VERSION)
                .argument("yuheng.test.advertised-host", "127.0.0.1")
                .argument("yuheng.test.advertised-port", port)
                .argument(
                        "egon.cola.component.yuheng.reporting.enabled",
                        reportingEnabled
                )
                .argument(
                        "egon.cola.component.yuheng.reporting."
                                + "admin-base-url",
                        adminBase
                )
                .argument(
                        "egon.cola.component.yuheng.reporting."
                                + "application-code",
                        APPLICATION_CODE
                )
                .argument(
                        "egon.cola.component.yuheng.reporting."
                                + "application-name",
                        "Gateway Live HTTP Provider"
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.env",
                        ENV
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.build-id",
                        "yuheng-live-build"
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.access-key",
                        accessKey
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.secret-key",
                        secretKey
                )
                .startupTimeout(STARTUP_TIMEOUT)
                .build();
    }

    private GatewayProcessSpec rpcProviderSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            URI adminBase,
            int managementPort,
            int rpcPort,
            String accessKey,
            String secretKey) {
        return ddcClient(
                GatewayProcessSpec.builder(
                                "rpc-provider",
                                "top.egon.cola.component.yuheng.test.rpc."
                                        + "provider."
                                        + "GatewayRpcTestProviderApplication"
                        ),
                infrastructure,
                ddcBase
        )
                .argument("server.port", managementPort)
                .argument("egon.cola.component.tianshu.enabled", true)
                .argument(
                        "egon.cola.component.tianshu.app-code",
                        RPC_APPLICATION_CODE
                )
                .argument("egon.cola.component.tianshu.env", ENV)
                .argument(
                        "egon.cola.component.tianshu.namespace",
                        NAMESPACE
                )
                .argument("egon.cola.component.tianshu.registry.enabled", true)
                .argument("egon.cola.component.rpc.enabled", true)
                .argument(
                        "egon.cola.component.rpc.tls.development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.rpc.provider.enabled",
                        true
                )
                .argument(
                        "egon.cola.component.rpc.provider.port",
                        rpcPort
                )
                .argument(
                        "egon.cola.component.rpc.provider."
                                + "advertised-host",
                        "127.0.0.1"
                )
                .argument(
                        "egon.cola.component.rpc.provider."
                                + "advertised-port",
                        rpcPort
                )
                .argument("yuheng.test.env", ENV)
                .argument("yuheng.test.namespace", NAMESPACE)
                .argument("yuheng.test.provider-id", "rpc-provider-live")
                .argument(
                        "egon.cola.component.yuheng.reporting.enabled",
                        true
                )
                .argument(
                        "egon.cola.component.yuheng.reporting."
                                + "admin-base-url",
                        adminBase
                )
                .argument(
                        "egon.cola.component.yuheng.reporting."
                                + "application-code",
                        RPC_APPLICATION_CODE
                )
                .argument(
                        "egon.cola.component.yuheng.reporting."
                                + "application-name",
                        "Gateway Live RPC Provider"
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.env",
                        ENV
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.yuheng.reporting."
                                + "artifact-version",
                        "1.0.0-live"
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.build-id",
                        "yuheng-live-rpc-build"
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.access-key",
                        accessKey
                )
                .argument(
                        "egon.cola.component.yuheng.reporting.secret-key",
                        secretKey
                )
                .startupTimeout(STARTUP_TIMEOUT)
                .build();
    }

    private GatewayProcessSpec rpcConsumerSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            int port) {
        return ddcClient(
                GatewayProcessSpec.builder(
                                "rpc-consumer",
                                "top.egon.cola.component.yuheng.test.rpc."
                                        + "consumer."
                                        + "GatewayRpcTestConsumerApplication"
                        ),
                infrastructure,
                ddcBase
        )
                .argument("server.port", port)
                .argument("egon.cola.component.tianshu.enabled", true)
                .argument(
                        "egon.cola.component.tianshu.app-code",
                        "yuheng-test-rpc-consumer"
                )
                .argument("egon.cola.component.tianshu.env", ENV)
                .argument(
                        "egon.cola.component.tianshu.namespace",
                        NAMESPACE
                )
                .argument("egon.cola.component.tianshu.registry.enabled", true)
                .argument("egon.cola.component.rpc.enabled", true)
                .argument(
                        "egon.cola.component.rpc.tls.development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.rpc.consumer.enabled",
                        true
                )
                .argument(
                        "egon.cola.component.rpc.consumer."
                                + "yuheng-discovery-timeout-ms",
                        30000
                )
                .argument(
                        "egon.cola.component.rpc.consumer."
                                + "yuheng-service-name",
                        RPC_YUHENG_SERVICE_NAME
                )
                .argument(
                        "egon.cola.component.rpc.consumer.yuheng-group",
                        "default"
                )
                .argument(
                        "egon.cola.component.rpc.consumer.yuheng-version",
                        "1.0.0"
                )
                .startupTimeout(STARTUP_TIMEOUT)
                .build();
    }


    private List<GatewayProcessHarness.ChildProcess> startMcpEngines(
            GatewayLiveEnvironment environment, URI ddcBase) throws IOException {
        List<GatewayProcessHarness.ChildProcess> engines = new ArrayList<>();
        for (int replica = 1; replica <= 2; replica++) {
            String name = "yuheng-mcp-gateway-" + replica;
            engines.add(environment.start(mcpEngineSpec(environment.infrastructure(), ddcBase,
                    GatewayProcessHarness.availablePort(), GatewayProcessHarness.availablePort(),
                    environment.dataDirectory(name), name)));
        }
        return List.copyOf(engines);
    }

    private void assertMcpRestartIsIndependent(
            GatewayLiveEnvironment environment, GatewayProcessHarness.ChildProcess mcp,
            List<GatewayProcessHarness.ChildProcess> apiEngines) throws Exception {
        List<Long> apiPids = apiEngines.stream().map(engine -> engine.process().pid()).toList();
        assertThat(mcp.lkgDirectory()).isDirectory();
        try (var files = java.nio.file.Files.walk(mcp.lkgDirectory())) {
            assertThat(files.anyMatch(java.nio.file.Files::isRegularFile)).isTrue();
        }
        environment.kill(mcp);
        var restarted = environment.restart(mcp);
        environment.awaitHttp(environment.managementBaseUri(GatewayEngineRoleEnum.MCP, restarted)
                .resolve("/actuator/health"), STARTUP_TIMEOUT, restarted);
        assertThat(restarted.lkgDirectory()).isEqualTo(mcp.lkgDirectory());
        assertThat(apiEngines).allSatisfy(engine -> assertThat(engine.process().isAlive()).isTrue());
        assertThat(apiEngines.stream().map(engine -> engine.process().pid()).toList()).isEqualTo(apiPids);
        HttpResponse<String> ordinary = httpClient.send(HttpRequest.newBuilder(
                        environment.dataPlaneBaseUri(GatewayEngineRoleEnum.MCP, restarted).resolve("/api/not-mcp"))
                .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(ordinary.statusCode()).isEqualTo(404);
        assertThat(ordinary.body()).contains("MCP_ROUTE_NOT_FOUND");
    }

    private GatewayProcessSpec mcpEngineSpec(
            GatewayTestInfrastructure infrastructure, URI ddcBase, int managementPort, int dataPort,
            Path dataDirectory, String instanceId) {
        String prefix = "egon.cola.component.yuheng.mcp-engine.";
        return ddcRuntimeRpc(GatewayProcessSpec.engineBuilder(instanceId, GatewayEngineRoleEnum.MCP,
                        URI.create("http://127.0.0.1:" + dataPort),
                        URI.create("http://127.0.0.1:" + managementPort)), ddcBase, GatewayEngineRoleEnum.MCP)
                .argument("server.port", managementPort)
                .argument("egon.cola.component.tianshu.enabled", true)
                .argument("egon.cola.component.tianshu.biz-code", "infra")
                .argument("egon.cola.component.tianshu.app-code", "ge")
                .argument("egon.cola.component.tianshu.env", ENV)
                .argument("egon.cola.component.tianshu.namespace", NAMESPACE)
                .argument("egon.cola.component.tianshu.instance.id", instanceId)
                .argument("egon.cola.component.tianshu.registry.enabled", true)
                .argument("egon.cola.component.tianshu.registry.http.instance-id", instanceId)
                .argument("egon.cola.component.tianshu.registry.http.advertised-host", "127.0.0.1")
                .argument("egon.cola.component.tianshu.registry.http.port", dataPort)
                .argument("egon.cola.component.tianshu.redis.host", infrastructure.ddcRedisHost())
                .argument("egon.cola.component.tianshu.redis.port", infrastructure.ddcRedisPort())
                .argument(prefix + "yuheng-group-code", "default")
                .argument(prefix + "env", ENV)
                .argument(prefix + "namespace", NAMESPACE)
                .argument(prefix + "node-id", instanceId)
                .argument(prefix + "instance-id", instanceId)
                .argument(prefix + "data-directory", dataDirectory)
                .argument(prefix + "management-port", managementPort)
                .argument(prefix + "listener.host", "127.0.0.1")
                .argument(prefix + "listener.port", dataPort)
                .argument(prefix + "listener.tls.enabled", false)
                .argument(prefix + "listener.tls.development-plaintext", true)
                .argument(prefix + "outbound.rpc-tls.enabled", false)
                .argument(prefix + "outbound.rpc-tls.development-plaintext", true)
                .argument("spring.datasource.url", infrastructure.jdbcUrl("gateway_admin"))
                .argument("spring.datasource.username", infrastructure.postgresUsername())
                .argument("spring.datasource.password", infrastructure.postgresPassword())
                .argument("egon.cola.component.yuheng.engine.mcp.redis.address",
                        "redis://" + infrastructure.ddcRedisHost() + ":" + infrastructure.ddcRedisPort())
                .argument("egon.cola.component.yuheng.engine.mcp.artifact-root",
                        dataDirectory.resolveSibling("mcp-shared-artifacts"))
                .startupTimeout(STARTUP_TIMEOUT).build();
    }

    private GatewayProcessSpec engineSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            int managementPort,
            int publicPort,
            int internalPort,
            Path dataDirectory) {
        return engineSpec(
                infrastructure,
                ddcBase,
                managementPort,
                publicPort,
                internalPort,
                dataDirectory,
                false,
                0,
                "yuheng-biz-gateway-1"
        );
    }

    private GatewayProcessSpec engineSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            int managementPort,
            int publicPort,
            int internalPort,
            Path dataDirectory,
            boolean rpcEnabled,
            int rpcPort) {
        return engineSpec(
                infrastructure,
                ddcBase,
                managementPort,
                publicPort,
                internalPort,
                dataDirectory,
                rpcEnabled,
                rpcPort,
                "yuheng-biz-gateway-1"
        );
    }

    private GatewayProcessSpec engineSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            int managementPort,
            int publicPort,
            int internalPort,
            Path dataDirectory,
            boolean rpcEnabled,
            int rpcPort,
            String instanceId) {
        GatewayProcessSpec.Builder builder = ddcClient(
                GatewayProcessSpec.engineBuilder(
                                instanceId,
                                GatewayEngineRoleEnum.API_RPC,
                                URI.create("http://127.0.0.1:" + publicPort),
                                URI.create("http://127.0.0.1:" + managementPort)
                        ),
                infrastructure,
                ddcBase
        )
                .argument("server.port", managementPort)
                .argument("egon.cola.component.tianshu.enabled", true)
                .argument("egon.cola.component.tianshu.biz-code", "infra")
                .argument("egon.cola.component.tianshu.instance.id", instanceId)
                .argument("egon.cola.component.tianshu.registry.http.instance-id", instanceId)
                .argument("egon.cola.component.tianshu.registry.http.advertised-host", "127.0.0.1")
                .argument("egon.cola.component.tianshu.registry.http.port", publicPort)
                .argument(
                        "egon.cola.component.tianshu.app-code",
                        "ge"
                )
                .argument("egon.cola.component.tianshu.env", ENV)
                .argument(
                        "egon.cola.component.tianshu.namespace",
                        NAMESPACE
                )
                .argument("egon.cola.component.tianshu.registry.enabled", true)
                .argument(
                        "egon.cola.component.yuheng.engine."
                                + "yuheng-group-code",
                        "default"
                )
                .argument(
                        "egon.cola.component.yuheng.engine.env",
                        ENV
                )
                .argument(
                        "egon.cola.component.yuheng.engine.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.yuheng.engine.node-id",
                        instanceId
                )
                .argument(
                        "egon.cola.component.yuheng.engine.instance-id",
                        instanceId
                )
                .argument(
                        "egon.cola.component.yuheng.engine.data-directory",
                        dataDirectory
                )
                .argument(
                        "egon.cola.component.yuheng.engine.http."
                                + "public-port",
                        publicPort
                )
                .argument(
                        "egon.cola.component.yuheng.engine.http."
                                + "internal-port",
                        internalPort
                )
                .argument(
                        "egon.cola.component.yuheng.engine.rpc.enabled",
                        rpcEnabled
                )
                .argument(
                        "egon.cola.component.yuheng.engine.kafka.enabled",
                        true
                )
                .argument(
                        "egon.cola.component.yuheng.engine.kafka."
                                + "bootstrap-servers",
                        infrastructure.kafkaBootstrapServers()
                )
                .argument(
                        "egon.cola.component.yuheng.engine.traffic.redis."
                                + "enabled",
                        true
                )
                .argument(
                        "egon.cola.component.yuheng.engine.traffic.redis."
                                + "address",
                        "redis://"
                                + infrastructure.rateLimitRedisHost()
                                + ":"
                                + infrastructure.rateLimitRedisPort()
                );
        if (rpcEnabled) {
            builder.argument(
                            "egon.cola.component.yuheng.engine.rpc.port",
                            rpcPort
                    )
                    .argument(
                            "egon.cola.component.yuheng.engine.rpc."
                                    + "advertised-host",
                            "127.0.0.1"
                    )
                    .argument(
                            "egon.cola.component.yuheng.engine.rpc."
                                    + "service-name",
                            RPC_YUHENG_SERVICE_NAME
                    )
                    .argument(
                            "egon.cola.component.yuheng.engine.rpc.group",
                            "default"
                    )
                    .argument(
                            "egon.cola.component.yuheng.engine.rpc.version",
                            "1.0.0"
                    );
        }
        return builder.startupTimeout(STARTUP_TIMEOUT).build();
    }

    private String awaitOperation(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            String applicationId) {
        return awaitOperation(
                processes,
                adminClient,
                applicationId,
                "GET /api/orders/{id}"
        );
    }

    private String awaitOperation(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            String applicationId,
            String methodIdentity) {
        String[] operationId = new String[1];
        processes.awaitCondition(
                () -> {
                    JsonNode tree = adminClient.applicationCatalog(
                            applicationId
                    );
                    operationId[0] = findActiveOperation(
                            tree,
                            methodIdentity
                    );
                    return operationId[0] != null;
                },
                Duration.ofSeconds(30),
                "active Starter definition in Gateway Admin"
        );
        return operationId[0];
    }

    private void awaitRpcProviderProjection(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            GatewayProcessHarness.ChildProcess admin) {
        processes.awaitCondition(
                () -> {
                    JsonNode projection = adminClient.providerInstances(
                            ENV,
                            NAMESPACE,
                            "RPC",
                            RPC_SERVICE_NAME,
                            RPC_SERVICE_GROUP,
                            RPC_SERVICE_VERSION
                    );
                    JsonNode instances = projection.path("value")
                            .path("instances");
                    if (!instances.isArray()) {
                        instances = projection.path("value");
                    }
                    for (JsonNode instance : instances) {
                        boolean online = DdcInstanceStatus.fromWire(
                                instance.path("status").asText()
                        ) == DdcInstanceStatus.ONLINE;
                        boolean hasDefinition = !instance.path("metadata")
                                .path("yuheng.definition-set-id")
                                .asText()
                                .isBlank();
                        if (online && hasDefinition) {
                            return true;
                        }
                    }
                    throw new AssertionError(
                            "RPC Provider projection: " + projection
                    );
                },
                Duration.ofSeconds(30),
                "RPC Provider projection in Gateway Admin",
                admin
        );
    }

    private String findActiveOperation(
            JsonNode tree,
            String methodIdentity) {
        for (JsonNode business : tree.path("businessDomains")) {
            for (JsonNode entity : business.path("entityDomains")) {
                for (JsonNode group : entity.path("interfaceGroups")) {
                    for (JsonNode operation : group.path("operations")) {
                        if (methodIdentity.equals(
                                operation.path("methodIdentity").asText()
                        ) && "ACTIVE".equals(
                                operation.path("lifecycleStatus").asText()
                        )) {
                            return operation.path("id").asText();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void awaitEngineConfigClients(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            String groupId,
            List<GatewayProcessHarness.ChildProcess> engines) {
        processes.awaitCondition(
                () -> {
                    JsonNode projection = adminClient.engineNodes(groupId);
                    Instant now = Instant.now();
                    for (GatewayProcessHarness.ChildProcess engine : engines) {
                        GatewayProcessSpec spec = engine.spec();
                        Map<String, String> leases = activeEngineLeases(projection, now, spec.engineRole());
                        if (!leases.containsKey(spec.name())) {
                            throw new AssertionError("Missing online " + spec.engineRole()
                                    + " Engine config client " + spec.name() + ": " + projection);
                        }
                    }
                    return true;
                },
                STARTUP_TIMEOUT,
                "Engine Tianshu config-client registration",
                engines.getFirst()
        );
        engines.forEach(engine -> assertThat(engine.process().isAlive())
                .as(processes.output(engine))
                .isTrue());
    }

    private int traceCount(
            GatewayAdminTestClient adminClient,
            String traceId) throws Exception {
        JsonNode page = adminClient.traces(ENV, NAMESPACE, traceId);
        return page.path("items").size();
    }

    private HttpResponse<String> inventory(
            int listenerPort,
            String sku) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(
                                "http://127.0.0.1:"
                                        + listenerPort
                                        + "/api/internal/inventory/"
                                        + sku
                        ))
                        .header("Host", "internal.yuheng.test")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private Set<String> httpProviderIds(
            GatewayAdminTestClient adminClient) throws Exception {
        return providerLeases(adminClient).keySet();
    }

    private Map<String, String> providerLeases(
            GatewayAdminTestClient adminClient) throws Exception {
        JsonNode projection = adminClient.providerInstances(
                ENV,
                NAMESPACE,
                "HTTP",
                APPLICATION_CODE,
                "default",
                SERVICE_VERSION
        );
        return providerLeases(projection);
    }

    private Set<String> providerIds(JsonNode projection) {
        return providerLeases(projection).keySet();
    }

    private Map<String, String> providerLeases(JsonNode projection) {
        Map<String, String> providerLeases = new java.util.LinkedHashMap<>();
        JsonNode instances = projection.path("value").path("instances");
        if (!instances.isArray()) {
            instances = projection.path("value");
        }
        for (JsonNode instance : instances) {
            String instanceId = instance.path("instanceId").asText();
            if (!instanceId.isBlank()) {
                providerLeases.put(
                        instanceId,
                        instance.path("leaseId").asText()
                );
            }
        }
        return Map.copyOf(providerLeases);
    }

    private void awaitHttpProviderProjection(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            String providerId,
            GatewayProcessHarness.ChildProcess admin) {
        processes.awaitCondition(
                () -> {
                    JsonNode projection = adminClient.providerInstances(
                            ENV,
                            NAMESPACE,
                            "HTTP",
                            APPLICATION_CODE,
                            "default",
                            SERVICE_VERSION
                    );
                    JsonNode instances = projection.path("value")
                            .path("instances");
                    if (!instances.isArray()) {
                        instances = projection.path("value");
                    }
                    for (JsonNode instance : instances) {
                        boolean online = DdcInstanceStatus.fromWire(
                                instance.path("status").asText()
                        ) == DdcInstanceStatus.ONLINE;
                        String definitionSetId = instance.path(
                                "definitionSetId"
                        ).asText();
                        if (definitionSetId.isBlank()) {
                            definitionSetId = instance.path("metadata")
                                    .path("yuheng.definition-set-id")
                                    .asText();
                        }
                        boolean hasDefinition = !definitionSetId.isBlank();
                        if (providerId.equals(
                                instance.path("instanceId").asText()
                        ) && online && hasDefinition) {
                            return true;
                        }
                    }
                    throw new AssertionError(
                            "HTTP Provider projection: " + projection
                    );
                },
                Duration.ofSeconds(30),
                "HTTP Provider registration metadata in Gateway Admin",
                admin
        );
    }

    private Set<String> awaitProviderFrameworks(
            GatewayProcessHarness processes,
            int listenerPort,
            Set<String> expected) {
        Set<String> observed = new LinkedHashSet<>();
        int[] invocation = {0};
        processes.awaitCondition(
                () -> {
                    HttpResponse<String> response = providerIdentity(
                            listenerPort,
                            "request-" + invocation[0]++
                    );
                    if (response.statusCode() == 200) {
                        observed.add(objectMapper.readTree(response.body())
                                .path("framework").asText());
                    }
                    return observed.containsAll(expected);
                },
                Duration.ofSeconds(30),
                "shared Provider route selections " + expected
        );
        return Set.copyOf(observed);
    }

    private void awaitOnlyProviderFramework(
            GatewayProcessHarness processes,
            int listenerPort,
            String expected) {
        int[] invocation = {0};
        processes.awaitCondition(
                () -> {
                    HttpResponse<String> response = providerIdentity(
                            listenerPort,
                            "single-" + invocation[0]++
                    );
                    return response.statusCode() == 200
                            && expected.equals(objectMapper
                            .readTree(response.body())
                            .path("framework")
                            .asText());
                },
                Duration.ofSeconds(30),
                "shared Provider route selection " + expected
        );
    }

    private HttpResponse<String> providerIdentity(
            int listenerPort,
            String requestId) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(
                                "http://127.0.0.1:"
                                        + listenerPort
                                        + "/api/providers/"
                                        + requestId
                        ))
                        .header("Host", "providers.yuheng.test")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private int engineNodeCount(
            GatewayAdminTestClient adminClient,
            String groupId) throws Exception {
        return engineLeases(adminClient, groupId).size();
    }

    private Map<String, String> engineLeases(
            GatewayAdminTestClient adminClient,
            String groupId) throws Exception {
        return activeEngineLeases(
                adminClient.engineNodes(groupId),
                Instant.now(), GatewayEngineRoleEnum.API_RPC
        );
    }

    static Map<String, String> activeEngineLeases(JsonNode projection, Instant now) {
        return activeEngineLeases(projection, now, null);
    }

    static Map<String, String> activeEngineLeases(
            JsonNode projection, Instant now, GatewayEngineRoleEnum requiredRole) {
        JsonNode nodes = projection.path("value");
        Map<String, String> leases = new LinkedHashMap<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                String expireAt = node.path("expireAt").asText();
                DdcInstanceStatus status = DdcInstanceStatus.fromWire(
                        node.path("status").asText()
                );
                boolean active = !expireAt.isBlank() && status.isAvailable(
                        now,
                        Instant.parse(expireAt)
                );
                if (!active) {
                    continue;
                }
                if (requiredRole != null && GatewayEngineRoleEnum.fromWire(
                        node.path("metadata").path("yuheng.engine.role").asText()).orElse(null) != requiredRole) {
                    continue;
                }
                String instanceId = node.path("instanceId").asText();
                if (!instanceId.isBlank()) {
                    leases.put(instanceId, node.path("leaseId").asText());
                }
            }
        }
        return Map.copyOf(leases);
    }

    private void awaitRuntimeConsistency(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            String groupId,
            String expectedReleaseId) {
        processes.awaitCondition(
                () -> {
                    JsonNode consistency = adminClient.runtimeConsistency(
                            groupId
                    );
                    return consistency.path("consistent").asBoolean()
                            && expectedReleaseId.equals(consistency
                            .path("targetReleaseId")
                            .asText())
                            && consistency.path("readyEngineNodeCount")
                            .asInt() == 4
                            && hasUnifiedRoleAcks(adminClient.engineNodes(groupId), expectedReleaseId, 4);
                },
                Duration.ofSeconds(30),
                "API_RPC and MCP runtime consistency for " + expectedReleaseId
        );
    }

    static boolean hasUnifiedRoleAcks(JsonNode projection, String releaseId, int expectedNodes) {
        return hasUnifiedRoleAcks(projection, releaseId, expectedNodes, Instant.now());
    }

    static boolean hasUnifiedRoleAcks(JsonNode projection, String releaseId, int expectedNodes, Instant now) {
        Map<String, String> activeLeases = activeEngineLeases(projection, now);
        var roles = java.util.EnumSet.noneOf(GatewayEngineRoleEnum.class);
        String version = null;
        String checksum = null;
        int count = 0;
        for (JsonNode node : projection.path("value")) {
            if (!node.path("leaseId").asText().equals(activeLeases.get(node.path("instanceId").asText()))) {
                continue;
            }
            JsonNode metadata = node.path("metadata");
            var role = GatewayEngineRoleEnum.fromWire(metadata.path("yuheng.engine.role").asText());
            if (role.isEmpty() || !releaseId.equals(metadata.path("activeReleaseId").asText())
                    || !"ACK_SUCCESS".equals(metadata.path("lastApplyStatus").asText())
                    || metadata.path("activeRuleVersion").asText().isBlank()
                    || metadata.path("activeRuleChecksum").asText().isBlank()
                    || metadata.path("lastAckAt").asText().isBlank()) {
                return false;
            }
            try {
                Long.parseLong(metadata.path("activeRuleVersion").asText());
                Instant.parse(metadata.path("lastAckAt").asText());
            } catch (RuntimeException invalidIdentity) {
                return false;
            }
            if (version == null) {
                version = metadata.path("activeRuleVersion").asText();
                checksum = metadata.path("activeRuleChecksum").asText();
            } else if (!version.equals(metadata.path("activeRuleVersion").asText())
                    || !checksum.equals(metadata.path("activeRuleChecksum").asText())) {
                return false;
            }
            roles.add(role.orElseThrow());
            count++;
        }
        return count == expectedNodes && roles.equals(java.util.EnumSet.allOf(GatewayEngineRoleEnum.class));
    }

    private Set<String> awaitRpcEngineSelections(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            URI consumerBase,
            Set<String> expected) throws Exception {
        Set<String> observed = new LinkedHashSet<>();
        for (int invocation = 0;
             invocation < 12 && !observed.containsAll(expected);
             invocation++) {
            String traceId = UuidV7.simpleString();
            HttpResponse<String> response = rpcConsumerEcho(
                    consumerBase,
                    traceId,
                    "selection-" + invocation
            );
            assertThat(response.statusCode()).isEqualTo(200);
            processes.awaitCondition(
                    () -> traceCount(adminClient, traceId) == 1,
                    Duration.ofSeconds(30),
                    "RPC Engine selection trace " + traceId
            );
            observed.add(traceEngineId(adminClient, traceId));
        }
        return Set.copyOf(observed);
    }

    private void assertRpcConsumerEngine(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient,
            URI consumerBase,
            String expectedEngineId) throws Exception {
        String traceId = UuidV7.simpleString();
        HttpResponse<String> response = rpcConsumerEcho(
                consumerBase,
                traceId,
                "after-engine-stop"
        );
        assertThat(response.statusCode()).isEqualTo(200);
        processes.awaitCondition(
                () -> traceCount(adminClient, traceId) == 1,
                Duration.ofSeconds(30),
                "RPC Engine failover trace"
        );
        assertThat(traceEngineId(adminClient, traceId))
                .isEqualTo(expectedEngineId);
    }

    private HttpResponse<String> rpcConsumerEcho(
            URI consumerBase,
            String traceId,
            String message) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(consumerBase.resolve(
                                "/test/rpc/echo?message=" + message
                        ))
                        .header("X-Trace-ID", traceId)
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private String traceEngineId(
            GatewayAdminTestClient adminClient,
            String traceId) throws Exception {
        JsonNode items = adminClient.traces(
                ENV,
                NAMESPACE,
                traceId
        ).path("items");
        return items.isArray() && !items.isEmpty()
                ? items.get(0).path("engineInstanceId").asText()
                : "";
    }

    private JsonNode get(URI uri) throws Exception {
        return exchange("GET", uri, null);
    }

    private JsonNode exchange(
            String method,
            URI uri,
            Object body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(
                objectMapper.writeValueAsString(body)
        );
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .method(method, publisher)
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    method
                            + " "
                            + uri
                            + " returned "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }
        return objectMapper.readTree(response.body());
    }

    private static String adminToken() {
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder()
                    .withoutPadding();
            String header = encoder.encodeToString(
                    "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            );
            String payload = encoder.encodeToString(("""
                    {"sub":"yuheng-live-test","exp":%d,
                     "capabilities":["*"],"roles":["yuheng-admin"]}
                    """.formatted(
                    Instant.now().plus(Duration.ofHours(12))
                            .getEpochSecond()
            )).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String signingInput = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    ADMIN_JWT_SECRET,
                    "HmacSHA256"
            ));
            return signingInput
                    + "."
                    + encoder.encodeToString(mac.doFinal(
                    signingInput.getBytes(
                            java.nio.charset.StandardCharsets.US_ASCII
                    )
            ));
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "cannot create live topology JWT",
                    failure
            );
        }
    }
}
