package top.egon.cola.component.gateway.test.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import top.egon.cola.component.gateway.test.process.GatewayProcessHarness;
import top.egon.cola.component.gateway.test.process.GatewayProcessSpec;
import top.egon.cola.component.gateway.test.process.GatewayTestInfrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "gateway.live.test", matches = "true")
class GatewayLiveTopologyIT {

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private static final String ENV = "test";

    private static final String NAMESPACE = "gateway-live";

    private static final String DDC_ACCESS_KEY = "gateway-live-ddc";

    private static final String DDC_SECRET_KEY =
            "gateway-live-ddc-secret-at-least-32-bytes";

    private static final String APPLICATION_CODE =
            "gateway-test-http-provider";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Test
    void reportsReleasesDiscoversForwardsAndProjectsTrace() throws Exception {
        try (GatewayTestInfrastructure infrastructure =
                     new GatewayTestInfrastructure();
             GatewayProcessHarness processes =
                     new GatewayProcessHarness("http-topology")) {
            infrastructure.start();
            infrastructure.createDatabase("gateway_ddc");
            infrastructure.createDatabase("gateway_admin");

            int ddcPort = GatewayProcessHarness.availablePort();
            int adminPort = GatewayProcessHarness.availablePort();
            int providerPort = GatewayProcessHarness.availablePort();
            int engineManagementPort = GatewayProcessHarness.availablePort();
            int enginePublicPort = GatewayProcessHarness.availablePort();
            int engineInternalPort = GatewayProcessHarness.availablePort();
            URI ddcBase = URI.create("http://127.0.0.1:" + ddcPort);
            URI adminBase = URI.create("http://127.0.0.1:" + adminPort);

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

            JsonNode application = post(
                    adminBase.resolve(
                            "/api/v1/gateway/admin/applications"
                    ),
                    Map.of(
                            "applicationCode", APPLICATION_CODE,
                            "displayName", "Gateway Live HTTP Provider",
                            "env", ENV,
                            "namespace", NAMESPACE,
                            "description", "GWS-13 live topology"
                    )
            );
            String applicationId = application.required("id").asText();
            JsonNode credential = post(
                    adminBase.resolve(
                            "/api/v1/gateway/admin/applications/"
                                    + applicationId
                                    + "/credentials"
                    ),
                    Map.of()
            );

            var provider = processes.start(providerSpec(
                    ddcBase,
                    adminBase,
                    providerPort,
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

            Path engineData = Files.createTempDirectory(
                    "gateway-live-engine-"
            );
            var engine = processes.start(engineSpec(
                    infrastructure,
                    ddcBase,
                    engineManagementPort,
                    enginePublicPort,
                    engineInternalPort,
                    engineData
            ));

            String operationId = awaitOperation(
                    processes,
                    adminBase,
                    applicationId
            );
            JsonNode group = post(
                    adminBase.resolve(
                            "/api/v1/gateway/admin/gateway-groups"
                    ),
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
            JsonNode mutation = put(
                    adminBase.resolve(
                            "/api/v1/gateway/admin/gateway-groups/"
                                    + groupId
                                    + "/draft/routes/live-http-order"
                    ),
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
            JsonNode validation = post(
                    adminBase.resolve(
                            "/api/v1/gateway/admin/gateway-groups/"
                                    + groupId
                                    + "/draft/validate"
                    ),
                    Map.of()
            );
            assertThat(validation.required("valid").asBoolean()).isTrue();
            JsonNode release = post(
                    adminBase.resolve(
                            "/api/v1/gateway/admin/gateway-groups/"
                                    + groupId
                                    + "/releases"
                    ),
                    Map.of(
                            "expectedDraftRevision", revision,
                            "changeReason", "GWS-13 live HTTP release"
                    )
            );
            assertThat(release.required("status").asText())
                    .isEqualTo("SUCCEEDED");

            processes.awaitHttp(
                    URI.create(
                            "http://127.0.0.1:"
                                    + engineManagementPort
                                    + "/actuator/health/readiness"
                    ),
                    STARTUP_TIMEOUT,
                    engine
            );

            String traceId = "live-http-trace-0000000000000001";
            HttpResponse<String> gatewayResponse = httpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:"
                                            + enginePublicPort
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

            processes.awaitCondition(
                    () -> traceCount(adminBase, traceId) == 1,
                    Duration.ofSeconds(30),
                    "Kafka call event projection in Gateway Admin"
            );
        }
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
            URI ddcBase,
            URI adminBase,
            int port,
            String accessKey,
            String secretKey) {
        return GatewayProcessSpec.builder(
                        "http-provider",
                        "top.egon.cola.component.gateway.test.http."
                                + "GatewayHttpTestProviderApplication"
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
                .argument("gateway.test.provider-id", "http-provider-live")
                .argument("gateway.test.advertised-host", "127.0.0.1")
                .argument("gateway.test.advertised-port", port)
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
                        "egon.cola.component.gateway.reporting."
                                + "artifact-version",
                        "1.0.0-live"
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

    private GatewayProcessSpec engineSpec(
            GatewayTestInfrastructure infrastructure,
            URI ddcBase,
            int managementPort,
            int publicPort,
            int internalPort,
            Path dataDirectory) {
        return GatewayProcessSpec.builder(
                        "gateway-engine",
                        "top.egon.cola.component.gateway.engine."
                                + "GatewayEngineApplication"
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
                        "gateway-engine-live"
                )
                .argument(
                        "egon.cola.component.gateway.engine.instance-id",
                        "gateway-engine-live-1"
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
                        false
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
                )
                .startupTimeout(STARTUP_TIMEOUT)
                .build();
    }

    private String awaitOperation(
            GatewayProcessHarness processes,
            URI adminBase,
            String applicationId) {
        String[] operationId = new String[1];
        processes.awaitCondition(
                () -> {
                    JsonNode tree = get(adminBase.resolve(
                            "/api/v1/gateway/admin/applications/"
                                    + applicationId
                                    + "/catalog"
                    ));
                    operationId[0] = findOperation(
                            tree,
                            "GET /api/orders/{id}"
                    );
                    return operationId[0] != null;
                },
                Duration.ofSeconds(30),
                "Starter definition report in Gateway Admin"
        );
        return operationId[0];
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

    private int traceCount(URI adminBase, String traceId) throws Exception {
        JsonNode page = get(adminBase.resolve(
                "/api/v1/gateway/admin/observability/traces"
                        + "?env="
                        + ENV
                        + "&namespace="
                        + NAMESPACE
                        + "&traceId="
                        + traceId
        ));
        return page.path("items").size();
    }

    private JsonNode get(URI uri) throws Exception {
        return exchange("GET", uri, null);
    }

    private JsonNode post(URI uri, Object body) throws Exception {
        return exchange("POST", uri, body);
    }

    private JsonNode put(URI uri, Object body) throws Exception {
        return exchange("PUT", uri, body);
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
                .header("X-Admin-Actor-Id", "gateway-live-test")
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
}
