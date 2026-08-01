package top.egon.cola.platform.rbac3.admin.integration.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only typed client for Gateway Admin release and discovery observations.
 */
public final class GatewayAdminControlPlaneStatusClient {

    private final URI adminBaseUri;
    private final String gatewayGroupId;
    private final String releaseId;
    private final ServiceKey providerServiceKey;
    private final GatewayAdminStatusCredentialProvider credentials;
    private final Transport transport;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration timeout;

    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            GatewayAdminStatusCredentialProvider credentials,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, new ServiceKey(
                        "prod", "default", "HTTP_PROVIDER", "http",
                        "rbac3-admin", "default", "1.0.0"),
                credentials, new JdkTransport(timeout), objectMapper, clock, timeout);
    }

    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            ServiceKey providerServiceKey,
            GatewayAdminStatusCredentialProvider credentials,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, providerServiceKey,
                credentials, new JdkTransport(timeout), objectMapper, clock, timeout);
    }

    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            GatewayAdminStatusCredentialProvider credentials,
            Transport transport,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this(adminBaseUri, gatewayGroupId, releaseId, new ServiceKey(
                        "prod", "default", "HTTP_PROVIDER", "http",
                        "rbac3-admin", "default", "1.0.0"),
                credentials, transport, objectMapper, clock, timeout);
    }

    public GatewayAdminControlPlaneStatusClient(
            URI adminBaseUri,
            String gatewayGroupId,
            String releaseId,
            ServiceKey providerServiceKey,
            GatewayAdminStatusCredentialProvider credentials,
            Transport transport,
            ObjectMapper objectMapper,
            Clock clock,
            Duration timeout) {
        this.adminBaseUri = root(adminBaseUri);
        this.gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        this.releaseId = required(releaseId, "releaseId");
        this.providerServiceKey = Objects.requireNonNull(
                providerServiceKey, "providerServiceKey").validated();
        this.credentials = Objects.requireNonNull(credentials, "credentials");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timeout = positive(timeout);
    }

    public GatewayAdminSnapshot snapshot() {
        Instant checkedAt = clock.instant();
        Optional<GatewayAdminStatusCredentialProvider.BearerCredential> supplied =
                credentials.current();
        if (supplied.isEmpty()) {
            return unknown("CREDENTIAL_MISSING", checkedAt);
        }
        var credential = supplied.orElseThrow();
        if (!credential.expiresAt().isAfter(checkedAt)) {
            return unknown("CREDENTIAL_EXPIRED", checkedAt);
        }
        return new GatewayAdminSnapshot(
                release(credential.accessToken()),
                providers(credential.accessToken()),
                consistency(credential.accessToken()),
                checkedAt);
    }

    private ReleaseObservation release(String token) {
        Response response = get("/api/v1/gateway/admin/releases/" + encode(releaseId), token);
        if (!response.success()) {
            return ReleaseObservation.unknown(releaseId, response.reasonCode());
        }
        JsonNode json = response.json();
        return new ReleaseObservation(
                "SUCCESS", text(json, "releaseId"), text(json, "status"),
                recursiveText(json, "definitionSetId"),
                recursiveText(json, "publishedVersion"), null);
    }

    private ProviderObservation providers(String token) {
        String query = "?env=" + encode(providerServiceKey.env())
                + "&namespace=" + encode(providerServiceKey.namespace())
                + "&serviceKind=" + encode(providerServiceKey.serviceKind())
                + "&protocol=" + encode(providerServiceKey.protocol())
                + "&serviceName=" + encode(providerServiceKey.serviceName())
                + "&group=" + encode(providerServiceKey.group())
                + "&version=" + encode(providerServiceKey.version());
        Response response = get(
                "/api/v1/gateway/admin/providers/instances" + query, token);
        if (!response.success()) {
            return ProviderObservation.unknown(response.reasonCode());
        }
        List<ProviderInstance> instances = new ArrayList<>();
        collectInstances(response.json(), instances);
        return new ProviderObservation("SUCCESS", instances, null);
    }

    private ConsistencyObservation consistency(String token) {
        Response response = get(
                "/api/v1/gateway/admin/gateway-groups/" + encode(gatewayGroupId)
                        + "/runtime-consistency", token);
        if (!response.success()) {
            return ConsistencyObservation.unknown(response.reasonCode());
        }
        JsonNode json = response.json();
        return new ConsistencyObservation(
                "SUCCESS", text(json, "releaseId"),
                firstText(json, "releaseStatus", "status"),
                json.path("consistent").asBoolean(false),
                recursiveText(json, "activeRuleVersion"), null);
    }

    private Response get(String path, String token) {
        try {
            HttpResponse result = transport.get(adminBaseUri.resolve(path), token, timeout);
            if (result.statusCode() == 403) {
                return Response.failure("GATEWAY_STATUS_FORBIDDEN");
            }
            if (result.statusCode() < 200 || result.statusCode() >= 300) {
                return Response.failure("GATEWAY_STATUS_UNAVAILABLE");
            }
            return Response.success(objectMapper.readTree(result.body()));
        } catch (Exception unavailable) {
            return Response.failure("GATEWAY_STATUS_UNAVAILABLE");
        }
    }

    private void collectInstances(JsonNode node, List<ProviderInstance> target) {
        if (node.isArray()) {
            node.forEach(value -> collectInstances(value, target));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode serviceKey = node.get("serviceKey");
        if (serviceKey != null && serviceKey.isObject() && node.has("instanceId")) {
            target.add(new ProviderInstance(
                    text(node, "instanceId"), text(node, "status"),
                    new ServiceKey(
                            text(serviceKey, "env"), text(serviceKey, "namespace"),
                            text(serviceKey, "serviceKind"), text(serviceKey, "protocol"),
                            text(serviceKey, "serviceName"), text(serviceKey, "group"),
                            firstText(serviceKey, "version", "artifactVersion")),
                    recursiveText(node.path("metadata"), "gateway.definition-set-id")));
            return;
        }
        node.elements().forEachRemaining(value -> collectInstances(value, target));
    }

    private GatewayAdminSnapshot unknown(String reasonCode, Instant checkedAt) {
        return new GatewayAdminSnapshot(
                ReleaseObservation.unknown(releaseId, reasonCode),
                ProviderObservation.unknown(reasonCode),
                ConsistencyObservation.unknown(reasonCode),
                checkedAt);
    }

    private String recursiveText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(field);
            if (direct != null && direct.isValueNode() && !direct.asText().isBlank()) {
                return direct.asText();
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                String nested = recursiveText(fields.next().getValue(), field);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode value : node) {
                String nested = recursiveText(value, field);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static URI root(URI uri) {
        Objects.requireNonNull(uri, "adminBaseUri");
        if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getQuery() != null
                || uri.getFragment() != null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Gateway Admin base URI is invalid");
        }
        String value = uri.toString();
        return URI.create(value.endsWith("/") ? value : value + '/');
    }

    private static Duration positive(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    public interface Transport {

        HttpResponse get(URI uri, String bearerToken, Duration timeout)
                throws IOException, InterruptedException;
    }

    public record HttpResponse(int statusCode, String body) {
    }

    private static final class JdkTransport implements Transport {

        private final HttpClient client;

        private JdkTransport(Duration connectTimeout) {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
        }

        @Override
        public HttpResponse get(URI uri, String bearerToken, Duration timeout)
                throws IOException, InterruptedException {
            var request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET()
                    .build();
            var response = client.send(request, BodyHandlers.ofString());
            return new HttpResponse(response.statusCode(), response.body());
        }
    }

    private record Response(JsonNode json, String reasonCode) {

        static Response success(JsonNode json) {
            return new Response(json, null);
        }

        static Response failure(String reasonCode) {
            return new Response(null, reasonCode);
        }

        boolean success() {
            return json != null;
        }
    }

    public record GatewayAdminSnapshot(
            ReleaseObservation release,
            ProviderObservation providers,
            ConsistencyObservation consistency,
            Instant checkedAt) {
    }

    public record ReleaseObservation(
            String state,
            String releaseId,
            String releaseStatus,
            String definitionSetId,
            String publishedVersion,
            String reasonCode) {

        static ReleaseObservation unknown(String releaseId, String reasonCode) {
            return new ReleaseObservation(
                    "UNKNOWN", releaseId, null, null, null, reasonCode);
        }
    }

    public record ProviderObservation(
            String state,
            List<ProviderInstance> instances,
            String reasonCode) {

        public ProviderObservation {
            instances = List.copyOf(instances);
        }

        static ProviderObservation unknown(String reasonCode) {
            return new ProviderObservation("UNKNOWN", List.of(), reasonCode);
        }
    }

    public record ProviderInstance(
            String instanceId,
            String status,
            ServiceKey serviceKey,
            String definitionSetId) {
    }

    public record ServiceKey(
            String env,
            String namespace,
            String serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version) {

        ServiceKey validated() {
            return new ServiceKey(
                    required(env, "serviceKey.env"),
                    required(namespace, "serviceKey.namespace"),
                    required(serviceKind, "serviceKey.serviceKind"),
                    required(protocol, "serviceKey.protocol"),
                    required(serviceName, "serviceKey.serviceName"),
                    required(group, "serviceKey.group"),
                    required(version, "serviceKey.version"));
        }
    }

    public record ConsistencyObservation(
            String state,
            String releaseId,
            String releaseStatus,
            boolean consistent,
            String observedVersion,
            String reasonCode) {

        static ConsistencyObservation unknown(String reasonCode) {
            return new ConsistencyObservation(
                    "UNKNOWN", null, null, false, null, reasonCode);
        }
    }
}
