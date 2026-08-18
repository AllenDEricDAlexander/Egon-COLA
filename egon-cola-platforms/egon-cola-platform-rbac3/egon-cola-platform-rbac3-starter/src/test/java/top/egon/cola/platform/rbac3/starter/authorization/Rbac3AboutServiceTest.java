package top.egon.cola.platform.rbac3.starter.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3AboutServiceTest {

    @Test
    void returnsOnlyCurrentAuthorizationFactsAndNoResourceTree() throws Exception {
        Instant now = Instant.parse("2026-08-18T02:00:00Z");
        IdentityPrincipal identity = new IdentityPrincipal(
                "alice-sub", "tenant-a", "access-jti", Set.of("rbac3-admin"),
                now.minusSeconds(30), now.plusSeconds(300), AuthenticationContext.password());
        Rbac3UserDetails details = new Rbac3UserDetails(identity,
                new SystemAuthorizationSnapshot(
                        "tenant-a", "alice-sub", "user-1", "rbac3-admin", 4L, 7L,
                        List.of("role-1"), Set.of("payment:read"), Map.of(), Map.of(),
                        "sha256:snapshot", now, now.plusSeconds(300)));
        CurrentRbac3User current = mock(CurrentRbac3User.class);
        when(current.require()).thenReturn(details);

        var about = new Rbac3AboutService(current).current();
        String json = new ObjectMapper().writeValueAsString(about);

        assertThat(about.user().subject()).isEqualTo("alice-sub");
        assertThat(about.currentApplicationCode()).isEqualTo("rbac3-admin");
        assertThat(about.permissions()).containsExactly("payment:read");
        assertThat(json).doesNotContain("apps", "menus", "routes", "actions",
                "navigationTree", "componentKey", "path");
    }
}
