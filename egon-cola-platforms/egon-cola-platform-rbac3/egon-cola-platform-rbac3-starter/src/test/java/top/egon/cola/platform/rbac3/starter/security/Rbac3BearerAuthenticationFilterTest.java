package top.egon.cola.platform.rbac3.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3BearerAuthenticationFilterTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void neverTreatsTrustedIdentityHeadersAsAuthentication() throws Exception {
        SingleFlightSnapshotLoader loader = mock(SingleFlightSnapshotLoader.class);
        Rbac3BearerAuthenticationFilter filter = new Rbac3BearerAuthenticationFilter(
                loader, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "20001");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void enrichesOnlyAnIdpAuthenticatedPrincipal() throws Exception {
        IdentityPrincipal principal = principal();
        SingleFlightSnapshotLoader loader = mock(SingleFlightSnapshotLoader.class);
        when(loader.load(principal)).thenReturn(snapshot());
        Rbac3BearerAuthenticationFilter filter = new Rbac3BearerAuthenticationFilter(
                loader, new ObjectMapper());
        SecurityContextHolder.getContext().setAuthentication(
                new IdpAuthenticationToken(principal));
        AtomicReference<Object> seenPrincipal = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> seenPrincipal.set(
                        SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal()));

        assertThat(seenPrincipal).hasValue(principal);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unavailableAuthorizationSnapshotReturnsServiceUnavailable() throws Exception {
        IdentityPrincipal principal = principal();
        SingleFlightSnapshotLoader loader = mock(SingleFlightSnapshotLoader.class);
        when(loader.load(principal)).thenThrow(
                new Rbac3AuthorizationClient.AuthorizationUnavailableException(
                        "RBAC3_UNAVAILABLE"));
        Rbac3BearerAuthenticationFilter filter = new Rbac3BearerAuthenticationFilter(
                loader, new ObjectMapper());
        SecurityContextHolder.getContext().setAuthentication(
                new IdpAuthenticationToken(principal));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("RBAC3_UNAVAILABLE");
    }

    private IdentityPrincipal principal() {
        return new IdentityPrincipal(
                "alice-sub", "tenant-a", "sid-1", "finance-web", "token-1",
                2, Set.of("finance"), NOW.minusSeconds(30), NOW.plusSeconds(300));
    }

    private SystemAuthorizationSnapshot snapshot() {
        return new SystemAuthorizationSnapshot(
                "tenant-a", "alice-sub", "101", "sid-1", "finance",
                7, 3, 11, List.of("role-1"), Set.of("payment:read"),
                Map.of(), Map.of(), "sha256:sid-1", NOW, NOW.plusSeconds(300));
    }
}
