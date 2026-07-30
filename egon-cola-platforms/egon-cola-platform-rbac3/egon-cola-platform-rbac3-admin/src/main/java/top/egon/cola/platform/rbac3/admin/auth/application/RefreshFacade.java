package top.egon.cola.platform.rbac3.admin.auth.application;

import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Completes refresh rotation by loading the atomically incremented session version.
 */
public final class RefreshFacade {

    private final RefreshTokenService refreshTokenService;
    private final RefreshStateSource refreshStateSource;
    private final JwtTokenService jwtTokenService;

    public RefreshFacade(
            RefreshTokenService refreshTokenService,
            RefreshStateSource refreshStateSource,
            JwtTokenService jwtTokenService) {
        this.refreshTokenService = Objects.requireNonNull(
                refreshTokenService, "refreshTokenService");
        this.refreshStateSource = Objects.requireNonNull(
                refreshStateSource, "refreshStateSource");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService");
    }

    public RefreshResult refresh(String rawRefreshToken, Instant now) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                rawRefreshToken, now);
        if (rotation.outcome() == RefreshTokenService.Outcome.REPLAY_DETECTED) {
            throw new Rbac3RuleViolation("REFRESH_TOKEN_REUSED");
        }
        if (rotation.outcome() != RefreshTokenService.Outcome.ROTATED) {
            throw new Rbac3RuleViolation("AUTHENTICATION_FAILED");
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
        return new RefreshResult(
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
                !state.roleActivationRequired());
    }

    @FunctionalInterface
    public interface RefreshStateSource {

        RefreshState load(String familyId);
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
