package top.egon.cola.component.gateway.test.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class GatewayAdminTestClient {

    private static final String API = "/api/v1/gateway/admin";

    private final URI baseUri;

    private final String bearerToken;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public GatewayAdminTestClient(URI baseUri, String bearerToken) {
        this(
                baseUri,
                bearerToken,
                new ObjectMapper(),
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
    }

    GatewayAdminTestClient(
            URI baseUri,
            String bearerToken,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("bearerToken is required");
        }
        this.bearerToken = bearerToken;
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public JsonNode createApplication(Object request)
            throws IOException, InterruptedException {
        return post(API + "/applications", request);
    }

    public JsonNode createCredential(String applicationId)
            throws IOException, InterruptedException {
        return post(
                API + "/applications/" + segment(applicationId)
                        + "/credentials",
                java.util.Map.of()
        );
    }

    public JsonNode applicationCatalog(String applicationId)
            throws IOException, InterruptedException {
        return get(API + "/applications/" + segment(applicationId)
                + "/catalog");
    }

    public JsonNode createGroup(Object request)
            throws IOException, InterruptedException {
        return post(API + "/gateway-groups", request);
    }

    public JsonNode putRoute(
            String groupId,
            String routeId,
            Object request) throws IOException, InterruptedException {
        return put(draft(groupId) + "/routes/" + segment(routeId), request);
    }

    public JsonNode getDraft(String groupId)
            throws IOException, InterruptedException {
        return get(draft(groupId));
    }

    public JsonNode putPolicy(
            String groupId,
            String policyId,
            Object request) throws IOException, InterruptedException {
        return put(
                draft(groupId) + "/policies/" + segment(policyId),
                request
        );
    }

    public JsonNode validateDraft(String groupId)
            throws IOException, InterruptedException {
        return post(draft(groupId) + "/validate", java.util.Map.of());
    }

    public JsonNode release(String groupId, Object request)
            throws IOException, InterruptedException {
        return post(
                API + "/gateway-groups/" + segment(groupId) + "/releases",
                request
        );
    }

    public JsonNode rollback(String groupId, Object request)
            throws IOException, InterruptedException {
        return post(
                API + "/gateway-groups/" + segment(groupId) + "/rollback",
                request
        );
    }

    public JsonNode runtimeConsistency(String groupId)
            throws IOException, InterruptedException {
        return get(API + "/gateway-groups/" + segment(groupId)
                + "/runtime-consistency");
    }

    public JsonNode engineNodes(String groupId)
            throws IOException, InterruptedException {
        return get(API + "/gateway-groups/" + segment(groupId)
                + "/engine-nodes");
    }

    public JsonNode providerInstances(
            String env,
            String namespace,
            String protocol,
            String serviceName) throws IOException, InterruptedException {
        return providerInstances(
                env,
                namespace,
                protocol,
                serviceName,
                null,
                null
        );
    }

    public JsonNode providerInstances(
            String env,
            String namespace,
            String protocol,
            String serviceName,
            String group,
            String version) throws IOException, InterruptedException {
        String scope = group == null || version == null
                ? ""
                : "&group=" + query(group) + "&version=" + query(version);
        return get(API + "/providers/instances?env=" + query(env)
                + "&namespace=" + query(namespace)
                + "&protocol=" + query(protocol)
                + "&serviceName=" + query(serviceName)
                + scope);
    }

    public JsonNode traces(
            String env,
            String namespace,
            String traceId) throws IOException, InterruptedException {
        return get(API + "/observability/traces?env=" + query(env)
                + "&namespace=" + query(namespace)
                + "&traceId=" + query(traceId));
    }

    JsonNode get(String path) throws IOException, InterruptedException {
        return exchange("GET", path, null);
    }

    JsonNode post(String path, Object body)
            throws IOException, InterruptedException {
        return exchange("POST", path, body);
    }

    JsonNode put(String path, Object body)
            throws IOException, InterruptedException {
        return exchange("PUT", path, body);
    }

    private JsonNode exchange(
            String method,
            String path,
            Object body) throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)
                );
        URI uri = baseUri.resolve(path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .method(method, publisher)
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    method + " " + uri + " returned "
                            + response.statusCode() + ": " + response.body()
            );
        }
        if (response.body() == null || response.body().isBlank()) {
            return MissingNode.getInstance();
        }
        return objectMapper.readTree(response.body());
    }

    private String draft(String groupId) {
        return API + "/gateway-groups/" + segment(groupId) + "/draft";
    }

    private String segment(String value) {
        return encode(value, "path segment");
    }

    private String query(String value) {
        return encode(value, "query value");
    }

    private String encode(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
