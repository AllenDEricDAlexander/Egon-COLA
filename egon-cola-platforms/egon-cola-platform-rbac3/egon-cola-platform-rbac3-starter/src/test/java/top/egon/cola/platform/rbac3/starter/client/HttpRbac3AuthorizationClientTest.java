package top.egon.cola.platform.rbac3.starter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpRbac3AuthorizationClientTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void fetchUsesBoundContextPathAndReloadedServiceCredential() throws Exception {
        Path credential = temporaryDirectory.resolve("rbac3.token");
        Files.writeString(credential, "service-token\n");
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        AtomicReference<String> requestedCredential = new AtomicReference<>();
        HttpRbac3AuthorizationClient.Transport transport = (uri, token, timeout) -> {
            requestedUri.set(uri);
            requestedCredential.set(token);
            return new HttpRbac3AuthorizationClient.HttpResponse(200, """
                    {"data":{"tenantId":"tenant-a","identitySub":"alice-sub",
                    "rbac3UserId":"101","sessionId":"sid-1","systemCode":"finance",
                    "authVersion":7,"contextVersion":3,"policyVersion":11,
                    "activeRoleIds":["role-1"],"permissions":["payment:read"],
                    "dataScopes":{},"fieldPolicies":{},"checksum":"sha256:sid-1",
                    "generatedAt":"2026-08-02T05:00:00Z",
                    "expiresAt":"2026-08-02T06:00:00Z"}}
                    """);
        };
        HttpRbac3AuthorizationClient client = new HttpRbac3AuthorizationClient(
                URI.create("http://127.0.0.1:8088"), credential,
                Duration.ofSeconds(1), objectMapper(), transport);

        var snapshot = client.fetch("finance", principal());

        assertThat(snapshot.identitySub()).isEqualTo("alice-sub");
        assertThat(requestedUri.get().toString()).isEqualTo(
                "http://127.0.0.1:8088/internal/v1/authorization/contexts/tenant-a/sid-1"
                        + "?systemCode=finance&identitySub=alice-sub");
        assertThat(requestedCredential).hasValue("service-token");
    }

    @Test
    void fetchSelectsServiceCredentialForTheExactTargetTenant()
            throws Exception {
        AtomicReference<String> requestedTenant = new AtomicReference<>();
        AtomicReference<String> requestedCredential = new AtomicReference<>();
        Function<String, String> credentials = tenantId -> {
            requestedTenant.set(tenantId);
            return "service-token-for-" + tenantId;
        };
        HttpRbac3AuthorizationClient client = new HttpRbac3AuthorizationClient(
                URI.create("http://127.0.0.1:8088"), credentials,
                Duration.ofSeconds(1), objectMapper(),
                (uri, token, timeout) -> {
                    requestedCredential.set(token);
                    return new HttpRbac3AuthorizationClient.HttpResponse(200, """
                            {"data":{"tenantId":"tenant-a","identitySub":"alice-sub",
                            "rbac3UserId":"101","sessionId":"sid-1","systemCode":"finance",
                            "authVersion":7,"contextVersion":3,"policyVersion":11,
                            "activeRoleIds":[],"permissions":[],"dataScopes":{},
                            "fieldPolicies":{},"checksum":"sha256:sid-1",
                            "generatedAt":"2026-08-02T05:00:00Z",
                            "expiresAt":"2026-08-02T06:00:00Z"}}
                            """);
                });

        client.fetch("finance", principal());

        assertThat(requestedTenant).hasValue("tenant-a");
        assertThat(requestedCredential)
                .hasValue("service-token-for-tenant-a");
    }

    @Test
    void deniedAndTransientStatusesAreClassifiedWithoutLeakingCredential()
            throws Exception {
        Path credential = temporaryDirectory.resolve("rbac3.token");
        Files.writeString(credential, "do-not-leak-this-token");
        HttpRbac3AuthorizationClient denied = client(credential, 403);
        HttpRbac3AuthorizationClient unavailable = client(credential, 502);

        assertThatThrownBy(() -> denied.fetch("finance", principal()))
                .isInstanceOf(Rbac3AuthorizationClient.AuthorizationDeniedException.class)
                .hasMessage("RBAC3_AUTHORIZATION_DENIED")
                .hasMessageNotContaining("do-not-leak-this-token");
        assertThatThrownBy(() -> unavailable.fetch("finance", principal()))
                .isInstanceOf(
                        Rbac3AuthorizationClient.AuthorizationUnavailableException.class)
                .hasMessage("RBAC3_AUTHORIZATION_UNAVAILABLE")
                .hasMessageNotContaining("do-not-leak-this-token");
    }

    @Test
    void rejectsClearTextNonLoopbackEndpoint() {
        assertThatThrownBy(() -> new HttpRbac3AuthorizationClient(
                URI.create("http://rbac3.internal"),
                temporaryDirectory.resolve("missing"), Duration.ofSeconds(1),
                objectMapper(), (uri, token, timeout) -> null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    private HttpRbac3AuthorizationClient client(Path credential, int status) {
        return new HttpRbac3AuthorizationClient(
                URI.create("http://localhost:8088"), credential,
                Duration.ofSeconds(1), objectMapper(),
                (uri, token, timeout) ->
                        new HttpRbac3AuthorizationClient.HttpResponse(status, "{}"));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private IdentityPrincipal principal() {
        return new IdentityPrincipal(
                "alice-sub", "tenant-a", "sid-1", "finance-web", "token-1",
                2, Set.of("finance"), Instant.parse("2026-08-02T04:59:30Z"),
                Instant.parse("2026-08-02T06:00:00Z"));
    }
}
