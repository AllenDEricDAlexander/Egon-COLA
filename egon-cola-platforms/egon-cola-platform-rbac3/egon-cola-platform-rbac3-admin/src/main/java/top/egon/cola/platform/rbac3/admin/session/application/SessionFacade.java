package top.egon.cola.platform.rbac3.admin.session.application;

import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Owns the user-session lifecycle while authorization activation remains a separate command.
 */
public final class SessionFacade {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final LongIdGenerator idGenerator;
    private final SessionStore sessionStore;
    private final SecureRandom secureRandom;
    private final Rbac3RuntimePolicy runtimePolicy;

    public SessionFacade(
            LongIdGenerator idGenerator,
            SessionStore sessionStore,
            Rbac3RuntimePolicy runtimePolicy) {
        this(idGenerator, sessionStore, new SecureRandom(), runtimePolicy);
    }

    SessionFacade(
            LongIdGenerator idGenerator,
            SessionStore sessionStore,
            SecureRandom secureRandom,
            Rbac3RuntimePolicy runtimePolicy) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
    }

    public IssuedSession create(
            String tenantId,
            String userId,
            long authVersion,
            long policyVersion,
            String deviceIdHash,
            Instant now) {
        Rbac3RuntimePolicy.Snapshot policySnapshot = runtimePolicy.current();
        long entityId = idGenerator.nextLongId();
        String sessionId = idGenerator.nextId();
        String familyId = idGenerator.nextId();
        String refreshTokenId = idGenerator.nextId();
        String rawRefreshToken = randomToken();
        Instant absoluteExpiry = now.plus(policySnapshot.sessionAbsoluteTimeout());
        SessionRecord session = new SessionRecord(
                Long.toString(entityId),
                tenantId,
                userId,
                sessionId,
                SessionStatus.ACTIVE,
                0,
                authVersion,
                policyVersion,
                true,
                familyId,
                deviceIdHash == null ? null : RefreshTokenService.hash(deviceIdHash),
                now,
                now.plus(policySnapshot.sessionIdleTimeout()),
                absoluteExpiry);
        RefreshTokenService.TokenRecord refreshToken = RefreshTokenService.TokenRecord.active(
                refreshTokenId,
                tenantId,
                sessionId,
                familyId,
                0,
                RefreshTokenService.hash(rawRefreshToken),
                now.plus(policySnapshot.refreshTokenTtl()));
        sessionStore.create(session, refreshToken, now);
        return new IssuedSession(session, rawRefreshToken, refreshToken.expiresAt());
    }

    public boolean logout(String tenantId, String userId, String sessionId, Instant now) {
        return sessionStore.logout(tenantId, userId, sessionId, now);
    }

    private String randomToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public interface SessionStore {

        void create(
                SessionRecord session,
                RefreshTokenService.TokenRecord refreshToken,
                Instant now);

        boolean logout(String tenantId, String userId, String sessionId, Instant now);
    }

    public record SessionRecord(
            String entityId,
            String tenantId,
            String userId,
            String sessionId,
            SessionStatus status,
            long sessionVersion,
            long authVersion,
            long policyVersion,
            boolean activationRequired,
            String tokenFamilyId,
            String deviceIdHash,
            Instant authenticatedAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt
    ) {
    }

    public record IssuedSession(
            SessionRecord session,
            String refreshToken,
            Instant refreshExpiresAt
    ) {

        @Override
        public String toString() {
            return "IssuedSession[session=" + session
                    + ", refreshToken=<redacted>, refreshExpiresAt=" + refreshExpiresAt + ']';
        }
    }

    public enum SessionStatus {
        ACTIVE,
        LOGGED_OUT,
        REVOKED,
        EXPIRED,
        COMPROMISED
    }
}
