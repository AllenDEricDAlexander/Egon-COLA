package top.egon.cola.component.gateway.test.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.test.process.GatewayProcessHarness;
import top.egon.cola.component.gateway.test.process.GatewayProcessSpec;
import top.egon.cola.component.gateway.test.process.GatewayTestInfrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
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

    private static final String NAMESPACE = "gateway-live";

    private static final String DDC_ACCESS_KEY = "gateway-live-ddc";

    private static final String DDC_SECRET_KEY =
            "gateway-live-ddc-secret-at-least-32-bytes";

    private static final String APPLICATION_CODE =
            "gateway-test-http-provider";

    private static final String SERVICE_VERSION = "1.0.0-live";

    private static final String RPC_APPLICATION_CODE =
            "gateway-test-rpc-provider";

    private static final String RPC_SERVICE_NAME =
            "EchoService";

    private static final String RPC_METHOD_NAME =
            "egon.gateway.test.v1.EchoService/Echo";

    private static final String RPC_GATEWAY_SERVICE_NAME =
            "egon-gateway-rpc";

    private static final byte[] ADMIN_JWT_SECRET =
            "gateway-live-jwt-secret-32-bytes!".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
            );

    private static final String ADMIN_TOKEN = adminToken();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

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
                    ddcBase.resolve("/api/v1/ddc/manifest"),
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
                    "gateway-engine-1"
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
                    "gateway-engine-2"
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
                    "gateway-engine-2"
            ));

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

            awaitEngineConfigClient(
                    processes,
                    ddcBase,
                    engine
            );
            awaitEngineConfigClient(
                    processes,
                    ddcBase,
                    secondEngine
            );
            JsonNode mutation = adminClient.putRoute(
                    groupId,
                    "live-http-order",
                    Map.of(
                            "operationId", operationId,
                            "content", Map.of(
                                    "host", "api.gateway.test",
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
                                    "host", "internal.gateway.test",
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
                                    "host", "providers.gateway.test",
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
            assertThat(validation.required("valid").asBoolean()).isTrue();
            JsonNode release = adminClient.release(
                    groupId,
                    Map.of(
                            "expectedDraftRevision", revision,
                            "changeReason", "GWS-13 live HTTP release"
                    )
            );
            assertThat(release.required("status").asText())
                    .isEqualTo("SUCCEEDED");
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

            mutation = adminClient.putRoute(
                    groupId,
                    "live-http-provider-identity",
                    Map.of(
                            "operationId", providerIdentityOperationId,
                            "content", Map.of(
                                    "host", "providers.gateway.test",
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
                    .isEqualTo("SUCCEEDED");
            awaitRuntimeConsistency(
                    processes,
                    adminClient,
                    groupId,
                    v2Release.required("releaseId").asText()
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

            String traceId = "live-http-trace-0000000000000001";
            HttpResponse<String> gatewayResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:"
                                            + secondEnginePublicPort
                                            + "/api/orders/order-live-1"
                            ))
                            .header("Host", "api.gateway.test")
                            .header("X-Trace-ID", traceId)
                            .header("X-Request-Source", "gateway-live-test")
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
                    .isEqualTo("gateway-live-test");
            assertThat(gatewayResponse.headers()
                    .firstValue("X-Trace-ID"))
                    .contains(traceId);

            HttpResponse<String> rateLimited = httpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:"
                                            + enginePublicPort
                                            + "/api/orders/order-live-2"
                            ))
                            .header("Host", "api.gateway.test")
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(rateLimited.statusCode()).isEqualTo(429);
            assertThat(rateLimited.body())
                    .contains("GATEWAY_RATE_LIMITED");

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
                    () -> httpProviderIds(adminClient).containsAll(Set.of(
                            "http-provider-live-1",
                            "http-provider-live-2"
                    )),
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
                    .isEqualTo("SUCCEEDED");
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
                    ddcBase.resolve("/api/v1/ddc/manifest"),
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
                    "gateway-engine-1"
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
                    "gateway-engine-2"
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
                    "gateway-engine-2"
            ));

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

            awaitEngineConfigClient(
                    processes,
                    ddcBase,
                    engine
            );
            awaitEngineConfigClient(
                    processes,
                    ddcBase,
                    secondEngine
            );
            JsonNode mutation = adminClient.putRoute(
                    groupId,
                    "live-rpc-echo",
                    Map.of(
                            "operationId", operationId,
                            "content", Map.of(
                                    "host", "rpc.gateway.test",
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
            assertThat(validation.required("valid").asBoolean()).isTrue();
            JsonNode release = adminClient.release(
                    groupId,
                    Map.of(
                            "expectedDraftRevision", revision,
                            "changeReason", "GWS-13 live RPC release"
                    )
            );
            assertThat(release.required("status").asText())
                    .isEqualTo("SUCCEEDED");
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
            awaitRpcProviderProjection(processes, adminClient);

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

            String traceId = "live-rpc-trace-0000000000000001";
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
                    "live-http-rpc-trace-000000000001";
            HttpResponse<String> httpRpcResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:"
                                            + engineInternalPort
                                            + "/rpc/echo"
                            ))
                            .header("Host", "rpc.gateway.test")
                            .header("Content-Type", "application/json")
                            .header("X-Trace-ID", httpRpcTraceId)
                            .timeout(Duration.ofSeconds(10))
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"message\":\"through-http-gateway\"}"
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
            assertThat(initialEngineLeases).containsKeys(
                    "gateway-engine-1",
                    "gateway-engine-2"
            );
            assertThat(awaitRpcEngineSelections(
                    processes,
                    adminClient,
                    consumerBase,
                    Set.of("gateway-engine-1", "gateway-engine-2")
            )).containsExactlyInAnyOrder(
                    "gateway-engine-1",
                    "gateway-engine-2"
            );

            processes.stop(engine);
            processes.awaitCondition(
                    () -> !engineLeases(adminClient, groupId).containsKey(
                            "gateway-engine-1"
                    ),
                    Duration.ofSeconds(30),
                    "stopped RPC Engine removal"
            );
            assertRpcConsumerEngine(
                    processes,
                    adminClient,
                    consumerBase,
                    "gateway-engine-2"
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
                    () -> !initialEngineLeases.get("gateway-engine-1")
                            .equals(engineLeases(adminClient, groupId).get(
                                    "gateway-engine-1"
                            )),
                    Duration.ofSeconds(30),
                    "restarted RPC Engine lease replacement"
            );
            assertThat(awaitRpcEngineSelections(
                    processes,
                    adminClient,
                    consumerBase,
                    Set.of("gateway-engine-1", "gateway-engine-2")
            )).containsExactlyInAnyOrder(
                    "gateway-engine-1",
                    "gateway-engine-2"
            );
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
                        "gateway-engine-2"
                )
        );

        assertSoftly(softly -> {
            ddcClients.forEach(spec -> softly.assertThat(spec.arguments())
                    .as(spec.name())
                    .contains(
                            "--egon.cola.component.ddc.redis.host="
                                    + infrastructure.ddcRedisHost(),
                            "--egon.cola.component.ddc.redis.port="
                                    + infrastructure.ddcRedisPort()
                    ));
        });
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
                .contains("--gateway.test.service-version=1.0.0-live")
                .noneMatch(argument -> argument.startsWith(
                        "--egon.cola.component.gateway.reporting."
                                + "artifact-version="
                ));
    }

    private GatewayTestInfrastructure testInfrastructure() {
        GatewayTestInfrastructure infrastructure =
                mock(GatewayTestInfrastructure.class);
        when(infrastructure.ddcRedisHost()).thenReturn("ddc-live-host");
        when(infrastructure.ddcRedisPort()).thenReturn(16379);
        when(infrastructure.rateLimitRedisHost()).thenReturn("rate-live-host");
        when(infrastructure.rateLimitRedisPort()).thenReturn(26379);
        when(infrastructure.kafkaBootstrapServers()).thenReturn("kafka:19092");
        return infrastructure;
    }

    private GatewayProcessSpec.Builder ddcClient(
            GatewayProcessSpec.Builder builder,
            GatewayTestInfrastructure infrastructure) {
        return builder
                .argument(
                        "egon.cola.component.ddc.redis.host",
                        infrastructure.ddcRedisHost()
                )
                .argument(
                        "egon.cola.component.ddc.redis.port",
                        infrastructure.ddcRedisPort()
                );
    }

    private GatewayProcessSpec ddcSpec(
            GatewayTestInfrastructure infrastructure,
            int port) {
        return GatewayProcessSpec.builder(
                        "ddc-admin",
                        "top.egon.cola.component.ddc.admin."
                                + "DynamicConfigCenterAdminApplication"
                )
                .argument("server.port", port)
                .argument(
                        "spring.datasource.url",
                        infrastructure.jdbcUrl("gateway_ddc")
                )
                .argument(
                        "spring.datasource.username",
                        infrastructure.postgres().getUsername()
                )
                .argument(
                        "spring.datasource.password",
                        infrastructure.postgres().getPassword()
                )
                .argument(
                        "egon.cola.component.ddc.admin.redis.host",
                        infrastructure.ddcRedisHost()
                )
                .argument(
                        "egon.cola.component.ddc.admin.redis.port",
                        infrastructure.ddcRedisPort()
                )
                .argument(
                        "egon.cola.component.ddc.admin.openapi."
                                + "signature-enabled",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.openapi.access-key",
                        DDC_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.ddc.admin.openapi.secret-key",
                        DDC_SECRET_KEY
                )
                .startupTimeout(STARTUP_TIMEOUT)
                .build();
    }

    private GatewayProcessSpec adminSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            int port) {
        String masterKey = Base64.getEncoder().encodeToString(
                "gateway-live-master-key-32-byte!".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );
        return GatewayProcessSpec.builder(
                        "gateway-admin",
                        "top.egon.cola.component.gateway.admin."
                                + "GatewayAdminApplication"
                )
                .argument("server.port", port)
                .argument(
                        "spring.datasource.url",
                        infrastructure.jdbcUrl("gateway_admin")
                )
                .argument(
                        "spring.datasource.username",
                        infrastructure.postgres().getUsername()
                )
                .argument(
                        "spring.datasource.password",
                        infrastructure.postgres().getPassword()
                )
                .argument("gateway.admin.ddc.enabled", true)
                .argument("gateway.admin.ddc.endpoint", ddcBase)
                .argument(
                        "gateway.admin.ddc.access-key",
                        DDC_ACCESS_KEY
                )
                .argument(
                        "gateway.admin.ddc.secret-key",
                        DDC_SECRET_KEY
                )
                .argument(
                        "gateway.admin.secrets.master-key-base64",
                        masterKey
                )
                .argument(
                        "gateway.admin.security.hmac-secret-base64",
                        Base64.getEncoder().encodeToString(
                                ADMIN_JWT_SECRET
                        )
                )
                .argument(
                        "gateway.admin.observability.kafka.enabled",
                        true
                )
                .argument(
                        "gateway.admin.observability.kafka."
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
                "top.egon.cola.component.gateway.test.http."
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
                "top.egon.cola.component.gateway.test.webflux."
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
                infrastructure
        )
                .argument("server.port", port)
                .argument("egon.cola.component.ddc.enabled", true)
                .argument(
                        "egon.cola.component.ddc.app-code",
                        APPLICATION_CODE
                )
                .argument("egon.cola.component.ddc.env", ENV)
                .argument(
                        "egon.cola.component.ddc.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.ddc.admin.endpoint",
                        ddcBase
                )
                .argument(
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.signature-enabled",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.access-key",
                        DDC_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.ddc.admin.secret-key",
                        DDC_SECRET_KEY
                )
                .argument("egon.cola.component.ddc.registry.enabled", true)
                .argument("gateway.test.env", ENV)
                .argument("gateway.test.namespace", NAMESPACE)
                .argument("gateway.test.provider-id", providerId)
                .argument("gateway.test.service-version", SERVICE_VERSION)
                .argument("gateway.test.advertised-host", "127.0.0.1")
                .argument("gateway.test.advertised-port", port)
                .argument(
                        "egon.cola.component.gateway.reporting.enabled",
                        reportingEnabled
                )
                .argument(
                        "egon.cola.component.gateway.reporting."
                                + "admin-base-url",
                        adminBase
                )
                .argument(
                        "egon.cola.component.gateway.reporting."
                                + "application-code",
                        APPLICATION_CODE
                )
                .argument(
                        "egon.cola.component.gateway.reporting."
                                + "application-name",
                        "Gateway Live HTTP Provider"
                )
                .argument(
                        "egon.cola.component.gateway.reporting.env",
                        ENV
                )
                .argument(
                        "egon.cola.component.gateway.reporting.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.gateway.reporting.build-id",
                        "gateway-live-build"
                )
                .argument(
                        "egon.cola.component.gateway.reporting.fail-fast",
                        true
                )
                .argument(
                        "egon.cola.component.gateway.reporting.access-key",
                        accessKey
                )
                .argument(
                        "egon.cola.component.gateway.reporting.secret-key",
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
                                "top.egon.cola.component.gateway.test.rpc."
                                        + "provider."
                                        + "GatewayRpcTestProviderApplication"
                        ),
                infrastructure
        )
                .argument("server.port", managementPort)
                .argument("egon.cola.component.ddc.enabled", true)
                .argument(
                        "egon.cola.component.ddc.app-code",
                        RPC_APPLICATION_CODE
                )
                .argument("egon.cola.component.ddc.env", ENV)
                .argument(
                        "egon.cola.component.ddc.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.ddc.admin.endpoint",
                        ddcBase
                )
                .argument(
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.signature-enabled",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.access-key",
                        DDC_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.ddc.admin.secret-key",
                        DDC_SECRET_KEY
                )
                .argument("egon.cola.component.ddc.registry.enabled", true)
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
                .argument("gateway.test.env", ENV)
                .argument("gateway.test.namespace", NAMESPACE)
                .argument("gateway.test.provider-id", "rpc-provider-live")
                .argument(
                        "egon.cola.component.gateway.reporting.enabled",
                        true
                )
                .argument(
                        "egon.cola.component.gateway.reporting."
                                + "admin-base-url",
                        adminBase
                )
                .argument(
                        "egon.cola.component.gateway.reporting."
                                + "application-code",
                        RPC_APPLICATION_CODE
                )
                .argument(
                        "egon.cola.component.gateway.reporting."
                                + "application-name",
                        "Gateway Live RPC Provider"
                )
                .argument(
                        "egon.cola.component.gateway.reporting.env",
                        ENV
                )
                .argument(
                        "egon.cola.component.gateway.reporting.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.gateway.reporting."
                                + "artifact-version",
                        "1.0.0-live"
                )
                .argument(
                        "egon.cola.component.gateway.reporting.build-id",
                        "gateway-live-rpc-build"
                )
                .argument(
                        "egon.cola.component.gateway.reporting.fail-fast",
                        true
                )
                .argument(
                        "egon.cola.component.gateway.reporting.access-key",
                        accessKey
                )
                .argument(
                        "egon.cola.component.gateway.reporting.secret-key",
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
                                "top.egon.cola.component.gateway.test.rpc."
                                        + "consumer."
                                        + "GatewayRpcTestConsumerApplication"
                        ),
                infrastructure
        )
                .argument("server.port", port)
                .argument("egon.cola.component.ddc.enabled", true)
                .argument(
                        "egon.cola.component.ddc.app-code",
                        "gateway-test-rpc-consumer"
                )
                .argument("egon.cola.component.ddc.env", ENV)
                .argument(
                        "egon.cola.component.ddc.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.ddc.admin.endpoint",
                        ddcBase
                )
                .argument(
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.signature-enabled",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.access-key",
                        DDC_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.ddc.admin.secret-key",
                        DDC_SECRET_KEY
                )
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
                                + "gateway-discovery-timeout-ms",
                        30000
                )
                .argument(
                        "egon.cola.component.rpc.consumer."
                                + "gateway-service-name",
                        RPC_GATEWAY_SERVICE_NAME
                )
                .argument(
                        "egon.cola.component.rpc.consumer.gateway-group",
                        "default"
                )
                .argument(
                        "egon.cola.component.rpc.consumer.gateway-version",
                        "1.0.0"
                )
                .startupTimeout(STARTUP_TIMEOUT)
                .build();
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
                "gateway-engine-1"
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
                "gateway-engine-1"
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
                GatewayProcessSpec.builder(
                                instanceId,
                                "top.egon.cola.component.gateway.engine."
                                        + "GatewayEngineApplication"
                        ),
                infrastructure
        )
                .argument("server.port", managementPort)
                .argument("egon.cola.component.ddc.enabled", true)
                .argument(
                        "egon.cola.component.ddc.app-code",
                        "gateway-engine-default"
                )
                .argument("egon.cola.component.ddc.env", ENV)
                .argument(
                        "egon.cola.component.ddc.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.ddc.admin.endpoint",
                        ddcBase
                )
                .argument(
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.signature-enabled",
                        true
                )
                .argument(
                        "egon.cola.component.ddc.admin.access-key",
                        DDC_ACCESS_KEY
                )
                .argument(
                        "egon.cola.component.ddc.admin.secret-key",
                        DDC_SECRET_KEY
                )
                .argument("egon.cola.component.ddc.registry.enabled", true)
                .argument(
                        "egon.cola.component.gateway.engine."
                                + "gateway-group-code",
                        "default"
                )
                .argument(
                        "egon.cola.component.gateway.engine.env",
                        ENV
                )
                .argument(
                        "egon.cola.component.gateway.engine.namespace",
                        NAMESPACE
                )
                .argument(
                        "egon.cola.component.gateway.engine.node-id",
                        instanceId
                )
                .argument(
                        "egon.cola.component.gateway.engine.instance-id",
                        instanceId
                )
                .argument(
                        "egon.cola.component.gateway.engine.data-directory",
                        dataDirectory
                )
                .argument(
                        "egon.cola.component.gateway.engine.http."
                                + "public-port",
                        publicPort
                )
                .argument(
                        "egon.cola.component.gateway.engine.http."
                                + "internal-port",
                        internalPort
                )
                .argument(
                        "egon.cola.component.gateway.engine.rpc.enabled",
                        rpcEnabled
                )
                .argument(
                        "egon.cola.component.gateway.engine.kafka.enabled",
                        true
                )
                .argument(
                        "egon.cola.component.gateway.engine.kafka."
                                + "bootstrap-servers",
                        infrastructure.kafkaBootstrapServers()
                )
                .argument(
                        "egon.cola.component.gateway.engine.traffic.redis."
                                + "enabled",
                        true
                )
                .argument(
                        "egon.cola.component.gateway.engine.traffic.redis."
                                + "address",
                        "redis://"
                                + infrastructure.rateLimitRedisHost()
                                + ":"
                                + infrastructure.rateLimitRedisPort()
                );
        if (rpcEnabled) {
            builder.argument(
                            "egon.cola.component.gateway.engine.rpc.port",
                            rpcPort
                    )
                    .argument(
                            "egon.cola.component.gateway.engine.rpc."
                                    + "advertised-host",
                            "127.0.0.1"
                    )
                    .argument(
                            "egon.cola.component.gateway.engine.rpc."
                                    + "service-name",
                            RPC_GATEWAY_SERVICE_NAME
                    )
                    .argument(
                            "egon.cola.component.gateway.engine.rpc.group",
                            "default"
                    )
                    .argument(
                            "egon.cola.component.gateway.engine.rpc.version",
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
                    operationId[0] = findOperation(
                            tree,
                            methodIdentity
                    );
                    return operationId[0] != null;
                },
                Duration.ofSeconds(30),
                "Starter definition report in Gateway Admin"
        );
        return operationId[0];
    }

    private void awaitRpcProviderProjection(
            GatewayProcessHarness processes,
            GatewayAdminTestClient adminClient) {
        processes.awaitCondition(
                () -> {
                    JsonNode projection = adminClient.providerInstances(
                            ENV,
                            NAMESPACE,
                            "RPC",
                            RPC_SERVICE_NAME
                    );
                    JsonNode instances = projection.path("value")
                            .path("instances");
                    if (!instances.isArray()) {
                        instances = projection.path("value");
                    }
                    for (JsonNode instance : instances) {
                        if (RPC_SERVICE_NAME.equals(
                                instance.path("serviceName").asText()
                        ) || instance.toString().contains(
                                "rpc-provider-live"
                        )) {
                            return true;
                        }
                    }
                    return false;
                },
                Duration.ofSeconds(30),
                "RPC Provider projection in Gateway Admin"
        );
    }

    private String findOperation(JsonNode tree, String methodIdentity) {
        for (JsonNode business : tree.path("businessDomains")) {
            for (JsonNode entity : business.path("entityDomains")) {
                for (JsonNode group : entity.path("interfaceGroups")) {
                    for (JsonNode operation : group.path("operations")) {
                        if (methodIdentity.equals(
                                operation.path("methodIdentity").asText()
                        )) {
                            return operation.path("id").asText();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void awaitEngineConfigClient(
            GatewayProcessHarness processes,
            URI ddcBase,
            GatewayProcessHarness.ChildProcess engine) {
        processes.awaitCondition(
                () -> {
                    JsonNode instances = get(ddcBase.resolve(
                            "/api/v1/ddc/instances"
                                    + "?appCode=gateway-engine-default"
                                    + "&env="
                                    + ENV
                                    + "&namespace="
                                    + NAMESPACE
                    ));
                    return instances.toString()
                            .contains("gateway-engine-default");
                },
                STARTUP_TIMEOUT,
                "Engine DDC config-client registration"
        );
        assertThat(engine.process().isAlive())
                .as(processes.output(engine))
                .isTrue();
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
                        .header("Host", "internal.gateway.test")
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
                APPLICATION_CODE
        );
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
                        .header("Host", "providers.gateway.test")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private int engineNodeCount(
            GatewayAdminTestClient adminClient,
            String groupId) throws Exception {
        JsonNode projection = adminClient.engineNodes(groupId);
        JsonNode nodes = projection.path("value");
        return nodes.isArray() ? nodes.size() : 0;
    }

    private Map<String, String> engineLeases(
            GatewayAdminTestClient adminClient,
            String groupId) throws Exception {
        JsonNode nodes = adminClient.engineNodes(groupId).path("value");
        Map<String, String> leases = new java.util.LinkedHashMap<>();
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
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
                            .asInt() == 2;
                },
                Duration.ofSeconds(30),
                "two Engine runtime consistency for " + expectedReleaseId
        );
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
            String traceId = "rpc-engine-selection-%02d-%d".formatted(
                    invocation,
                    System.nanoTime()
            );
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
        String traceId = "rpc-engine-failover-" + System.nanoTime();
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
                    {"sub":"gateway-live-test","exp":%d,
                     "capabilities":["*"],"roles":["gateway-admin"]}
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
