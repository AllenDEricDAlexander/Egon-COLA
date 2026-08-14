package top.egon.cola.platform.rbac3.starter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.VerifiedUserTokenCarrier;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpRbac3AuthorizationClientTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void fetchUsesBoundContextPathAndBothExplicitCredentials() throws Exception {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        AtomicReference<String> requestedServiceToken = new AtomicReference<>();
        AtomicReference<String> requestedUserToken = new AtomicReference<>();
        HttpRbac3AuthorizationClient.Transport transport =
                (uri, serviceToken, userToken, timeout) -> {
                    requestedUri.set(uri);
                    requestedServiceToken.set(serviceToken);
                    requestedUserToken.set(userToken);
                    return new HttpRbac3AuthorizationClient.HttpResponse(200, responseBody());
                };
        setVerifiedUserToken("user-at");
        HttpRbac3AuthorizationClient client = client(transport);

        var snapshot = client.fetch("finance", principal());

        assertThat(snapshot.identitySub()).isEqualTo("alice-sub");
        assertThat(requestedUri.get().toString()).isEqualTo(
                "http://127.0.0.1:8088/internal/v1/authorization/snapshots/current"
                        + "?systemCode=finance");
        assertThat(requestedServiceToken).hasValue("service-token");
        assertThat(requestedUserToken).hasValue("user-at");
    }

    @Test
    void fetchSelectsServiceCredentialForTheExactTargetTenant() throws Exception {
        AtomicReference<String> requestedTenant = new AtomicReference<>();
        HttpTenantServiceTokenSupplier supplier = supplier(tenantId -> {
            requestedTenant.set(tenantId);
            return "service-token-for-" + tenantId;
        });
        setVerifiedUserToken("user-at");
        HttpRbac3AuthorizationClient client = new HttpRbac3AuthorizationClient(
                URI.create("http://127.0.0.1:8088"), supplier,
                Duration.ofSeconds(1), objectMapper(),
                (uri, serviceToken, userToken, timeout) ->
                        new HttpRbac3AuthorizationClient.HttpResponse(200, responseBody()));

        client.fetch("finance", principal());

        assertThat(requestedTenant).hasValue("tenant-a");
    }

    @Test
    void deniedAndTransientStatusesAreClassifiedWithoutLeakingCredential()
            throws Exception {
        setVerifiedUserToken("do-not-leak-user-token");
        HttpRbac3AuthorizationClient denied = client(
                (uri, serviceToken, userToken, timeout) ->
                        new HttpRbac3AuthorizationClient.HttpResponse(403, "{}"));
        HttpRbac3AuthorizationClient unavailable = client(
                (uri, serviceToken, userToken, timeout) ->
                        new HttpRbac3AuthorizationClient.HttpResponse(502, "{}"));

        assertThatThrownBy(() -> denied.fetch("finance", principal()))
                .isInstanceOf(Rbac3AuthorizationClient.AuthorizationDeniedException.class)
                .hasMessage("RBAC3_AUTHORIZATION_DENIED")
                .hasMessageNotContaining("do-not-leak-user-token");
        assertThatThrownBy(() -> unavailable.fetch("finance", principal()))
                .isInstanceOf(Rbac3AuthorizationClient.AuthorizationUnavailableException.class)
                .hasMessage("RBAC3_AUTHORIZATION_UNAVAILABLE")
                .hasMessageNotContaining("do-not-leak-user-token");
    }

    @Test
    void rejectsClearTextNonLoopbackEndpoint() {
        assertThatThrownBy(() -> new HttpRbac3AuthorizationClient(
                URI.create("http://rbac3.internal"), supplier(ignored -> "token"),
                Duration.ofSeconds(1), objectMapper(),
                (uri, serviceToken, userToken, timeout) -> null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    private HttpRbac3AuthorizationClient client(
            HttpRbac3AuthorizationClient.Transport transport) {
        return new HttpRbac3AuthorizationClient(
                URI.create("http://127.0.0.1:8088"), supplier(ignored -> "service-token"),
                Duration.ofSeconds(1), objectMapper(), transport);
    }

    private HttpTenantServiceTokenSupplier supplier(
            java.util.function.Function<String, String> value) {
        return new HttpTenantServiceTokenSupplier(
                "rbac3-client", () -> value.apply("tenant-a"),
                URI.create("https://rbac3.example/internal"),
                Set.of("rbac3:authorization:read"), Duration.ofSeconds(5),
                Clock.systemUTC(), request ->
                new HttpTenantServiceTokenSupplier.TokenResponse(
                        value.apply(request.tenantId()), "Bearer", 300));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private void setVerifiedUserToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        VerifiedUserTokenCarrier.set(request, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private IdentityPrincipal principal() {
        return new IdentityPrincipal(
                "alice-sub", "tenant-a", "token-1", Set.of("finance"),
                Instant.parse("2026-08-02T04:59:30Z"),
                Instant.parse("2026-08-02T06:00:00Z"),
                AuthenticationContext.password());
    }

    private String responseBody() {
        return """
                {"data":{"tenantId":"tenant-a","identitySub":"alice-sub",
                "rbac3UserId":"101","systemCode":"finance",
                "authVersion":7,"policyVersion":11,
                "activeRoleIds":["role-1"],"permissions":["payment:read"],
                "dataScopes":{},"fieldPolicies":{},"checksum":"sha256:alice-sub",
                "generatedAt":"2026-08-02T05:00:00Z",
                "expiresAt":"2026-08-02T06:00:00Z"}}
                """;
    }
}
