package top.egon.cola.platform.idp.starter.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentIdentityTest {

    private final CurrentIdentity currentIdentity = new CurrentIdentity();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requiresCurrentUserIdentity() {
        IdentityPrincipal user = user();
        SecurityContextHolder.getContext().setAuthentication(
                new IdpAuthenticationToken(user));

        assertThat(currentIdentity.current()).contains(user);
        assertThat(currentIdentity.require()).isSameAs(user);
    }

    @Test
    void rejectsAnonymousAndServiceAsUser() {
        assertThat(currentIdentity.current()).isEmpty();
        assertThatThrownBy(currentIdentity::require)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        SecurityContextHolder.getContext().setAuthentication(
                new IdpAuthenticationToken(service()));

        assertThat(currentIdentity.current()).isEmpty();
        assertThatThrownBy(currentIdentity::require)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private IdentityPrincipal user() {
        return new IdentityPrincipal(
                "user-subject",
                "tenant-a",
                "access-jti",
                Set.of("https://api.example/permission/idp"),
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(300),
                AuthenticationContext.password());
    }

    private ServiceIdentityPrincipal service() {
        return new ServiceIdentityPrincipal(
                "service-client",
                "tenant-a",
                "service-client",
                "service-jti",
                URI.create("https://api.example/permission/idp"),
                1L,
                Set.of("idp:identity:read"),
                "platform",
                "idp",
                "prod",
                "key-1",
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(300));
    }
}
