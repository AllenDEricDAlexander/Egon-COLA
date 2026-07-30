package top.egon.cola.platform.rbac3.admin.session.application;

import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.security.SecureRandom;
import java.time.Duration;
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
    private final Duration idleTimeout;
    private final Duration absoluteTimeout;
    private final Duration refreshLifetime;

    public SessionFacade(
            LongIdGenerator idGenerator,
            SessionStore sessionStore,
            Duration idleTimeout,
            Duration absoluteTimeout,
            Duration refreshLifetime) {
        this(idGenerator, sessionStore, new SecureRandom(), idleTimeout, absoluteTimeout,
                refreshLifetime);
    }

    SessionFacade(
            LongIdGenerator idGenerator,
            SessionStore sessionStore,
            SecureRandom secureRandom,
            Duration idleTimeout,
            Duration absoluteTimeout,
            Duration refreshLifetime) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        this.idleTimeout = bounded(idleTimeout, Duration.ofMinutes(5), Duration.ofHours(8),
                "idleTimeout");
        this.absoluteTimeout = bounded(absoluteTimeout, Duration.ofHours(1), Duration.ofHours(24),
                "absoluteTimeout");
        this.refreshLifetime = bounded(refreshLifetime, Duration.ofDays(1), Duration.ofDays(30),
                "refreshLifetime");
    }

    public IssuedSession create(
            String tenantId,
            String userId,
            long authVersion,
            long policyVersion,
            String deviceIdHash,
            Instant now) {
        long entityId = idGenerator.nextLongId();
        String sessionId = idGenerator.nextId();
        String familyId = idGenerator.nextId();
        String refreshTokenId = idGenerator.nextId();
        String rawRefreshToken = randomToken();
        Instant absoluteExpiry = now.plus(absoluteTimeout);
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
                now.plus(idleTimeout),
                absoluteExpiry);
        RefreshTokenService.TokenRecord refreshToken = RefreshTokenService.TokenRecord.active(
                refreshTokenId,
                tenantId,
                sessionId,
                familyId,
                0,
                RefreshTokenService.hash(rawRefreshToken),
                now.plus(refreshLifetime));
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

    private static Duration bounded(
            Duration value,
            Duration minimum,
            Duration maximum,
            String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(fieldName + " is outside the allowed range");
        }
        return value;
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
