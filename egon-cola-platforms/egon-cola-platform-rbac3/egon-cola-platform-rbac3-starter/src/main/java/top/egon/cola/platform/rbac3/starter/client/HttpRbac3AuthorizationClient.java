package top.egon.cola.platform.rbac3.starter.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/** HTTP client for the service-credential protected RBAC3 snapshot endpoint. */
public final class HttpRbac3AuthorizationClient
        implements Rbac3AuthorizationClient {

    private final URI endpoint;
    private final Function<String, String> credentials;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final Transport transport;

    public HttpRbac3AuthorizationClient(
            URI endpoint,
            Path credentialFile,
            Duration timeout,
            ObjectMapper objectMapper) {
        this(endpoint, tenantId -> credential(credentialFile), timeout, objectMapper,
                new JdkTransport(timeout));
    }

    public HttpRbac3AuthorizationClient(
            URI endpoint,
            Function<String, String> credentials,
            Duration timeout,
            ObjectMapper objectMapper) {
        this(endpoint, credentials, timeout, objectMapper,
                new JdkTransport(timeout));
    }

    HttpRbac3AuthorizationClient(
            URI endpoint,
            Path credentialFile,
            Duration timeout,
            ObjectMapper objectMapper,
            Transport transport) {
        this(
                endpoint,
                tenantId -> credential(credentialFile),
                timeout,
                objectMapper,
                transport
        );
    }

    HttpRbac3AuthorizationClient(
            URI endpoint,
            Function<String, String> credentials,
            Duration timeout,
            ObjectMapper objectMapper,
            Transport transport) {
        this.endpoint = secureEndpoint(endpoint);
        this.credentials = Objects.requireNonNull(credentials, "credentials");
        this.timeout = bounded(timeout);
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    @Override
    public SystemAuthorizationSnapshot fetch(
            String systemCode,
            IdentityPrincipal principal) throws InterruptedException {
        Objects.requireNonNull(principal, "principal");
        URI uri = endpoint.resolve("/internal/v1/authorization/contexts/"
                + encode(principal.tenantId()) + '/' + encode(principal.sessionId())
                + "?systemCode=" + encode(systemCode)
                + "&identitySub=" + encode(principal.subject()));
        try {
            HttpResponse response = transport.get(
                    uri,
                    credential(credentials.apply(principal.tenantId())),
                    timeout
            );
            if (response.statusCode() == 401 || response.statusCode() == 403
                    || response.statusCode() == 404) {
                throw new AuthorizationDeniedException(
                        "RBAC3_AUTHORIZATION_DENIED");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthorizationUnavailableException(
                        "RBAC3_AUTHORIZATION_UNAVAILABLE");
            }
            JsonNode data = objectMapper.readTree(response.body()).get("data");
            if (data == null || data.isNull()) {
                throw new AuthorizationUnavailableException(
                        "RBAC3_AUTHORIZATION_RESPONSE_INVALID");
            }
            return objectMapper.treeToValue(data, SystemAuthorizationSnapshot.class);
        } catch (AuthorizationDeniedException | AuthorizationUnavailableException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new AuthorizationUnavailableException(
                    "RBAC3_AUTHORIZATION_UNAVAILABLE", exception);
        }
    }

    private static String credential(Path credentialFile) {
        try {
            Objects.requireNonNull(credentialFile, "credentialFile");
            String value = Files.readString(credentialFile).trim();
            return credential(value);
        } catch (IOException exception) {
            throw new AuthorizationUnavailableException(
                    "RBAC3_SERVICE_CREDENTIAL_UNAVAILABLE", exception);
        }
    }

    private static String credential(String value) {
        if (value == null || value.isBlank() || value.length() > 8192) {
            throw new AuthorizationUnavailableException(
                    "RBAC3_SERVICE_CREDENTIAL_UNAVAILABLE"
            );
        }
        return value.trim();
    }

    private static URI secureEndpoint(URI value) {
        Objects.requireNonNull(value, "endpoint");
        boolean loopback = "localhost".equalsIgnoreCase(value.getHost())
                || "127.0.0.1".equals(value.getHost())
                || "::1".equals(value.getHost());
        if (!"https".equalsIgnoreCase(value.getScheme())
                && !("http".equalsIgnoreCase(value.getScheme()) && loopback)) {
            throw new IllegalArgumentException(
                    "RBAC3 authorization endpoint must use HTTPS or loopback HTTP");
        }
        return value;
    }

    private static Duration bounded(Duration value) {
        Objects.requireNonNull(value, "timeout");
        if (value.compareTo(Duration.ofMillis(100)) < 0
                || value.compareTo(Duration.ofSeconds(5)) > 0) {
            throw new IllegalArgumentException("timeout is outside the safe range");
        }
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface Transport {
        HttpResponse get(URI uri, String bearerToken, Duration timeout)
                throws IOException, InterruptedException;
    }

    record HttpResponse(int statusCode, String body) {
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
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET()
                    .build();
            var response = client.send(request, BodyHandlers.ofString());
            return new HttpResponse(response.statusCode(), response.body());
        }
    }
}
