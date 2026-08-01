package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Completes refresh rotation by loading the atomically incremented session version.
 */
public final class RefreshFacade {

    private final RefreshTokenService refreshTokenService;
    private final RefreshStateSource refreshStateSource;
    private final JwtTokenService jwtTokenService;
    private final TransactionBoundary transactionBoundary;
    private final RefreshRuntimePublisher runtimePublisher;
    private final RefreshAuditRecorder auditRecorder;

    public RefreshFacade(
            RefreshTokenService refreshTokenService,
            RefreshStateSource refreshStateSource,
            JwtTokenService jwtTokenService) {
        this(refreshTokenService, refreshStateSource, jwtTokenService,
                work -> work.get(), (state, generatedAt) -> {
                }, audit -> {
                });
    }

    public RefreshFacade(
            RefreshTokenService refreshTokenService,
            RefreshStateSource refreshStateSource,
            JwtTokenService jwtTokenService,
            TransactionBoundary transactionBoundary,
            RefreshRuntimePublisher runtimePublisher) {
        this(refreshTokenService, refreshStateSource, jwtTokenService,
                transactionBoundary, runtimePublisher, audit -> {
                });
    }

    public RefreshFacade(
            RefreshTokenService refreshTokenService,
            RefreshStateSource refreshStateSource,
            JwtTokenService jwtTokenService,
            TransactionBoundary transactionBoundary,
            RefreshRuntimePublisher runtimePublisher,
            RefreshAuditRecorder auditRecorder) {
        this.refreshTokenService = Objects.requireNonNull(
                refreshTokenService, "refreshTokenService");
        this.refreshStateSource = Objects.requireNonNull(
                refreshStateSource, "refreshStateSource");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService");
        this.transactionBoundary = Objects.requireNonNull(
                transactionBoundary, "transactionBoundary");
        this.runtimePublisher = Objects.requireNonNull(runtimePublisher, "runtimePublisher");
        this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder");
    }

    public RefreshResult refresh(String rawRefreshToken, Instant now) {
        RefreshAttempt attempt = transactionBoundary.execute(
                () -> refreshAtomically(rawRefreshToken, now));
        if (attempt.reasonCode() != null) {
            throw new Rbac3RuleViolation(attempt.reasonCode());
        }
        return attempt.result();
    }

    private RefreshAttempt refreshAtomically(String rawRefreshToken, Instant now) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                rawRefreshToken, now);
        if (rotation.outcome() == RefreshTokenService.Outcome.REPLAY_DETECTED) {
            return RefreshAttempt.rejected("REFRESH_TOKEN_REUSED");
        }
        if (rotation.outcome() != RefreshTokenService.Outcome.ROTATED) {
            return RefreshAttempt.rejected("AUTHENTICATION_FAILED");
        }
        RefreshState state = refreshStateSource.load(rotation.familyId());
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(
                new JwtTokenService.AccessTokenSubject(
                        state.tenantId(),
                        state.userId(),
                        state.sessionId(),
                        state.authVersion(),
                        state.sessionVersion(),
                        state.policyVersion()),
                now);
        runtimePublisher.publish(state, now);
        auditRecorder.record(new RefreshAudit(
                state.tenantId(), state.userId(), state.sessionId(),
                state.sessionVersion(), state.policyVersion(), now));
        return RefreshAttempt.succeeded(new RefreshResult(
                "Bearer",
                accessToken.token(),
                Duration.between(now, accessToken.expiresAt()).toSeconds(),
                rotation.refreshToken(),
                Duration.between(now, state.refreshExpiresAt()).toSeconds(),
                state.sessionId(),
                state.authVersion(),
                state.sessionVersion(),
                state.policyVersion(),
                state.roleActivationRequired(),
                state.activationReasonCode(),
                !state.roleActivationRequired()));
    }

    @FunctionalInterface
    public interface RefreshStateSource {

        RefreshState load(String familyId);
    }

    @FunctionalInterface
    public interface TransactionBoundary {

        RefreshAttempt execute(Supplier<RefreshAttempt> work);
    }

    @FunctionalInterface
    public interface RefreshRuntimePublisher {

        void publish(RefreshState state, Instant generatedAt);
    }

    @FunctionalInterface
    public interface RefreshAuditRecorder {

        void record(RefreshAudit audit);
    }

    public record RefreshAudit(
            String tenantId,
            String userId,
            String sessionId,
            long sessionVersion,
            long policyVersion,
            Instant occurredAt
    ) {
    }

    public record RefreshAttempt(RefreshResult result, String reasonCode) {

        public static RefreshAttempt succeeded(RefreshResult result) {
            return new RefreshAttempt(Objects.requireNonNull(result, "result"), null);
        }

        public static RefreshAttempt rejected(String reasonCode) {
            return new RefreshAttempt(null, Objects.requireNonNull(reasonCode, "reasonCode"));
        }
    }

    public record RefreshState(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion,
            Instant refreshExpiresAt,
            boolean roleActivationRequired,
            String activationReasonCode
    ) {
    }
}
