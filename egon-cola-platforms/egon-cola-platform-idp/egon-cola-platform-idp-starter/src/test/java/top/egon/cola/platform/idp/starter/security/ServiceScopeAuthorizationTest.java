package top.egon.cola.platform.idp.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceScopeAuthorizationTest {

    private final ServiceScopeAuthorization authorization =
            new ServiceScopeAuthorization();

    @Test
    void acceptsOnlyServicePrincipalWithTheRequiredIdpScope() {
        var service = new TestingAuthenticationToken(
                service(), "", "ROLE_SERVICE");

        assertThat(authorization.hasScope(
                service, "service:authorization:snapshot")).isTrue();
        assertThat(authorization.hasScope(
                service, "service:authorization:decide")).isFalse();
    }

    @Test
    void rejectsUserAndMissingAuthenticationForServiceOnlyOperations() {
        var user = new TestingAuthenticationToken(
                user(), "", "ROLE_USER");

        assertThat(authorization.hasScope(
                user, "service:authorization:snapshot")).isFalse();
        assertThat(authorization.hasScope(
                null, "service:authorization:snapshot")).isFalse();
    }

    private ServiceIdentityPrincipal service() {
        Instant issuedAt = Instant.parse("2026-08-02T08:00:00Z");
        return new ServiceIdentityPrincipal(
                "rbac3-service",
                "tenant-1",
                "rbac3-service",
                "token-1",
                URI.create("https://api.example/prod/permission/rbac3"),
                12L,
                Set.of("service:authorization:snapshot"),
                "permission",
                "idp",
                "prod",
                "key-1",
                issuedAt,
                issuedAt.plusSeconds(300)
        );
    }

    private IdentityPrincipal user() {
        Instant issuedAt = Instant.parse("2026-08-02T08:00:00Z");
        return new IdentityPrincipal(
                "identity-1",
                "tenant-1",
                "session-1",
                "gateway-admin",
                "token-1",
                7L,
                Set.of("https://api.example/prod/permission/rbac3"),
                issuedAt,
                issuedAt.plusSeconds(300)
        );
    }
}
