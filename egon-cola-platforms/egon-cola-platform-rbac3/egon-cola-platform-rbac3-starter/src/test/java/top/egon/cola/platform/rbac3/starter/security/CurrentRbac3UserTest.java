package top.egon.cola.platform.rbac3.starter.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentRbac3UserTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsUserDetailsFromSecurityContextWithoutParameterInjection() {
        IdentityPrincipal identity = new IdentityPrincipal(
                "subject", "tenant", "jti", Set.of("rbac3"),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(300),
                AuthenticationContext.password());
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant", "subject", "user", "rbac3", 1L, 1L,
                List.of("role"), Set.of("permission"), Map.of(), Map.of(),
                "checksum", Instant.EPOCH, Instant.EPOCH.plusSeconds(300));
        Rbac3UserDetails details = new Rbac3UserDetails(identity, snapshot);
        SecurityContextHolder.getContext().setAuthentication(
                new Rbac3AuthenticationToken(details));

        CurrentRbac3User current = new CurrentRbac3User();

        assertThat(current.current()).contains(details);
        assertThat(current.require()).isSameAs(details);
    }

    @Test
    void rejectsAnonymousContext() {
        CurrentRbac3User current = new CurrentRbac3User();

        assertThat(current.current()).isEmpty();
        assertThatThrownBy(current::require)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
