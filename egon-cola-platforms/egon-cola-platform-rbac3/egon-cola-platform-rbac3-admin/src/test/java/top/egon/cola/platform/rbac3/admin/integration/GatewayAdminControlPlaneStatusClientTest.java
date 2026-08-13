package top.egon.cola.platform.rbac3.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayAdminStatusCredentialProvider;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayAdminControlPlaneTransport;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminControlPlaneHttpResponseVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.BearerCredentialVO;

class GatewayAdminControlPlaneStatusClientTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void missingOrExpiredCredentialReturnsUnknownWithoutCallingGateway() {
        List<URI> calls = new ArrayList<>();
        var transport = transport(calls, 200, "{}");

        var missing = client(Optional::empty, transport).snapshot();
        var expired = client(() -> Optional.of(new BearerCredentialVO("secret-token", NOW.minusSeconds(1))), transport).snapshot();

        assertThat(missing.release().state()).isEqualTo("UNKNOWN");
        assertThat(missing.release().reasonCode()).isEqualTo("CREDENTIAL_MISSING");
        assertThat(expired.release().reasonCode()).isEqualTo("CREDENTIAL_EXPIRED");
        assertThat(calls).isEmpty();
        assertThat(missing.toString()).doesNotContain("secret-token");
    }

    @Test
    void readsAllThreeTypedStatusesWithOneReadOnlyBearerCredential() {
        List<URI> calls = new ArrayList<>();
        var transport = new GatewayAdminControlPlaneTransport() {
            @Override
            public GatewayAdminControlPlaneHttpResponseVO get(
                    URI uri, String bearerToken, Duration timeout) {
                assertThat(bearerToken).isEqualTo("secret-token");
                calls.add(uri);
                if (uri.getPath().endsWith("/releases/release-1")) {
                    return response(200, """
                            {"releaseId":"release-1","status":"SUCCESS",
                             "validationReport":{"definitionSetId":"definition-1",
                                                   "publishedVersion":"1.0.0"}}
                            """);
                }
                if (uri.getPath().endsWith("/providers/instances")) {
                    return response(200, """
                            {"value":{"instances":[{"instanceId":"instance-1",
                              "status":"UP","serviceKey":{"bizCode":"rbac3",
                              "appCode":"rbac3-admin","env":"prod",
                              "namespace":"default","serviceKind":"HTTP_PROVIDER",
                              "protocol":"http","serviceName":"rbac3-admin",
                              "group":"default","version":"1.0.0"},
                              "metadata":{"gateway.definition-set-id":"definition-1"}}]}}
                            """);
                }
                return response(200, """
                        {"releaseId":"release-1","releaseStatus":"SUCCESS",
                         "consistent":true,"nodeStates":[{"status":"CONSISTENT",
                         "activeRuleVersion":1}]}
                        """);
            }
        };

        var result = client(credential(), transport).snapshot();

        assertThat(result.release().state()).isEqualTo("SUCCESS");
        assertThat(result.release().definitionSetId()).isEqualTo("definition-1");
        assertThat(result.providers().instances()).singleElement()
                .satisfies(instance -> {
                    assertThat(instance.serviceKey().bizCode()).isEqualTo("rbac3");
                    assertThat(instance.serviceKey().appCode()).isEqualTo("rbac3-admin");
                    assertThat(instance.serviceKey().serviceName())
                            .isEqualTo("rbac3-admin");
                    assertThat(instance.definitionSetId()).isEqualTo("definition-1");
                });
        assertThat(result.consistency().consistent()).isTrue();
        assertThat(calls).hasSize(3);
        assertThat(calls.get(1).getQuery())
                .contains("bizCode=rbac3", "appCode=rbac3-admin");
    }

    @Test
    void forbiddenServerFailureAndTimeoutAreStructuredUnknownAndDoNotLeakToken() {
        List<URI> forbiddenCalls = new ArrayList<>();
        var forbidden = client(credential(), transport(forbiddenCalls, 403, "denied"))
                .snapshot();
        var serverFailure = client(credential(), transport(new ArrayList<>(), 503, "secret-token"))
                .snapshot();
        var timeout = client(credential(), (uri, bearer, duration) -> {
            throw new IOException("secret-token timed out");
        }).snapshot();

        assertThat(forbidden.release().reasonCode()).isEqualTo("GATEWAY_STATUS_FORBIDDEN");
        assertThat(serverFailure.release().reasonCode())
                .isEqualTo("GATEWAY_STATUS_UNAVAILABLE");
        assertThat(timeout.release().reasonCode()).isEqualTo("GATEWAY_STATUS_UNAVAILABLE");
        assertThat(forbidden.toString() + serverFailure + timeout)
                .doesNotContain("secret-token", "denied");
    }

    private GatewayAdminControlPlaneStatusClient client(
            GatewayAdminStatusCredentialProvider credentials,
            GatewayAdminControlPlaneTransport transport) {
        return new GatewayAdminControlPlaneStatusClient(
                URI.create("https://gateway-admin.example.test"),
                "gateway-group-1", "release-1", credentials, transport,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(2));
    }

    private GatewayAdminStatusCredentialProvider credential() {
        return () -> Optional.of(new BearerCredentialVO(
                "secret-token", NOW.plusSeconds(60)));
    }

    private GatewayAdminControlPlaneTransport transport(
            List<URI> calls,
            int status,
            String body) {
        return (uri, bearer, duration) -> {
            calls.add(uri);
            return response(status, body);
        };
    }

    private GatewayAdminControlPlaneHttpResponseVO response(
            int status,
            String body) {
        return new GatewayAdminControlPlaneHttpResponseVO(status, body);
    }
}
