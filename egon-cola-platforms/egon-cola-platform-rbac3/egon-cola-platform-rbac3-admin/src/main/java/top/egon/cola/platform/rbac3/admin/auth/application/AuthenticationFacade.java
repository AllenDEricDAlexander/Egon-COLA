package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Orchestrates authentication and empty-role session creation.
 */
public final class AuthenticationFacade {

    private static final String CANDIDATES_URL =
            "/api/rbac3/v1/auth/role-activation-candidates";

    private final IdentityAuthenticatorStrategy authenticator;
    private final LoginStateSource loginStateSource;
    private final SessionFacade sessionFacade;
    private final JwtTokenService jwtTokenService;
    private final LoginRuntimePublisher runtimePublisher;
    private final LoginAuditRecorder auditRecorder;

    public AuthenticationFacade(
            IdentityAuthenticatorStrategy authenticator,
            LoginStateSource loginStateSource,
            SessionFacade sessionFacade,
            JwtTokenService jwtTokenService,
            LoginRuntimePublisher runtimePublisher) {
        this(authenticator, loginStateSource, sessionFacade, jwtTokenService,
                runtimePublisher, audit -> {
                });
    }

    public AuthenticationFacade(
            IdentityAuthenticatorStrategy authenticator,
            LoginStateSource loginStateSource,
            SessionFacade sessionFacade,
            JwtTokenService jwtTokenService,
            LoginRuntimePublisher runtimePublisher,
            LoginAuditRecorder auditRecorder) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.loginStateSource = Objects.requireNonNull(loginStateSource, "loginStateSource");
        this.sessionFacade = Objects.requireNonNull(sessionFacade, "sessionFacade");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService");
        this.runtimePublisher = Objects.requireNonNull(runtimePublisher, "runtimePublisher");
        this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder");
    }

    public LoginResult login(LoginRequest request, Instant now) {
        IdentityAuthenticatorStrategy.AuthenticatedIdentity identity =
                authenticator.authenticate(request, now);
        LoginState state = loginStateSource.load(
                identity.tenantId(), identity.userId(), now);
        SessionFacade.IssuedSession issued = sessionFacade.create(
                state.tenantId(),
                identity.userId(),
                state.authVersion(),
                state.policyVersion(),
                request.device().deviceId(),
                now);
        SessionFacade.SessionRecord session = issued.session();
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(
                new JwtTokenService.AccessTokenSubject(
                        state.tenantId(),
                        identity.userId(),
                        session.sessionId(),
                        session.authVersion(),
                        session.sessionVersion(),
                        session.policyVersion()),
                now);
        try {
            runtimePublisher.publish(session, now);
            auditRecorder.record(new LoginAudit(
                    state.tenantId(), identity.userId(), session.sessionId(),
                    identity.authenticationMethod(), identity.authenticationStrength(),
                    session.sessionVersion(), now));
        } catch (RuntimeException publicationFailure) {
            try {
                sessionFacade.logout(
                        state.tenantId(), identity.userId(), session.sessionId(), now);
            } catch (RuntimeException compensationFailure) {
                publicationFailure.addSuppressed(compensationFailure);
            }
            throw publicationFailure;
        }
        return new LoginResult(
                "Bearer",
                accessToken.token(),
                Duration.between(now, accessToken.expiresAt()).toSeconds(),
                issued.refreshToken(),
                Duration.between(now, issued.refreshExpiresAt()).toSeconds(),
                session.sessionId(),
                true,
                state.activationCandidateCount(),
                CANDIDATES_URL,
                false);
    }

    @FunctionalInterface
    public interface LoginStateSource {

        LoginState load(String tenantCode, String userId, Instant now);
    }

    @FunctionalInterface
    public interface LoginRuntimePublisher {

        void publish(SessionFacade.SessionRecord session, Instant generatedAt);
    }

    @FunctionalInterface
    public interface LoginAuditRecorder {

        void record(LoginAudit audit);
    }

    public record LoginAudit(
            String tenantId,
            String userId,
            String sessionId,
            String authenticationMethod,
            int authenticationStrength,
            long sessionVersion,
            Instant occurredAt
    ) {
    }

    public record LoginState(
            String tenantId,
            long authVersion,
            long policyVersion,
            int activationCandidateCount
    ) {

        public LoginState {
            if (authVersion < 0 || policyVersion < 0 || activationCandidateCount < 0) {
                throw new IllegalArgumentException("login state values must not be negative");
            }
        }
    }
}
