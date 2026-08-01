package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.auth.application.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.IdentityAuthenticatorStrategy;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtTokenService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationRuntimePublicationTest {

    private static final Instant NOW = Instant.parse("2026-08-01T04:00:00Z");

    @Test
    void publishesTheEmptyActivationRuntimeBeforeReturningLogin() {
        SessionFacade sessions = mock(SessionFacade.class);
        JwtTokenService tokens = mock(JwtTokenService.class);
        SessionFacade.IssuedSession issued = issuedSession();
        when(sessions.create("10", "20", 3, 4, "device", NOW)).thenReturn(issued);
        when(tokens.issue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(NOW)))
                .thenReturn(new JwtTokenService.IssuedAccessToken(
                        "access", NOW.plusSeconds(300), claims()));
        AtomicReference<SessionFacade.SessionRecord> published = new AtomicReference<>();
        AtomicReference<AuthenticationFacade.LoginAudit> audit = new AtomicReference<>();
        AuthenticationFacade facade = facade(
                sessions, tokens, (session, generatedAt) -> published.set(session), audit::set);

        var result = facade.login(request(), NOW);

        assertThat(result.sessionId()).isEqualTo("30");
        assertThat(published).hasValue(issued.session());
        assertThat(audit.get().tenantId()).isEqualTo("10");
        assertThat(audit.get().sessionId()).isEqualTo("30");
        assertThat(audit.get().authenticationMethod()).isEqualTo("PASSWORD");
        assertThat(audit.get().authenticationStrength()).isEqualTo(1);
    }

    @Test
    void revokesTheDatabaseSessionWhenInitialRuntimePublicationFails() {
        SessionFacade sessions = mock(SessionFacade.class);
        JwtTokenService tokens = mock(JwtTokenService.class);
        SessionFacade.IssuedSession issued = issuedSession();
        when(sessions.create("10", "20", 3, 4, "device", NOW)).thenReturn(issued);
        when(tokens.issue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(NOW)))
                .thenReturn(new JwtTokenService.IssuedAccessToken(
                        "access", NOW.plusSeconds(300), claims()));
        AuthenticationFacade facade = facade(
                sessions, tokens,
                (session, generatedAt) -> {
                    throw new IllegalStateException("redis unavailable");
                }, ignored -> {
                    throw new AssertionError("failed login must not be audited as success");
                });

        assertThatThrownBy(() -> facade.login(request(), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("redis unavailable");
        verify(sessions).logout("10", "20", "30", NOW);
    }

    private AuthenticationFacade facade(
            SessionFacade sessions,
            JwtTokenService tokens,
            AuthenticationFacade.LoginRuntimePublisher publisher,
            AuthenticationFacade.LoginAuditRecorder auditRecorder) {
        IdentityAuthenticatorStrategy authenticator = (request, now) ->
                new IdentityAuthenticatorStrategy.AuthenticatedIdentity(
                        "10", "20", "PASSWORD", 1);
        AuthenticationFacade.LoginStateSource state = (tenantId, userId, now) ->
                new AuthenticationFacade.LoginState("10", 3, 4, 2);
        return new AuthenticationFacade(
                authenticator, state, sessions, tokens, publisher, auditRecorder);
    }

    private SessionFacade.IssuedSession issuedSession() {
        return new SessionFacade.IssuedSession(
                new SessionFacade.SessionRecord(
                        "29", "10", "20", "30", SessionFacade.SessionStatus.ACTIVE,
                        0, 3, 4, true, "family", "device-hash", NOW,
                        NOW.plusSeconds(600), NOW.plusSeconds(3600)),
                "refresh", NOW.plusSeconds(7200));
    }

    private LoginRequest request() {
        return new LoginRequest(
                "tenant", "mario", "never-log",
                new LoginRequest.Device("device", "browser"));
    }

    private Rbac3TokenClaims claims() {
        return new Rbac3TokenClaims(
                "issuer", java.util.List.of("audience"), "20", "10", "30",
                3, 0, 4, "jti", NOW, NOW, NOW.plusSeconds(300), "kid");
    }
}
