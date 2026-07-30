package top.egon.cola.platform.rbac3.admin.session.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Function;

/**
 * Rotates opaque refresh tokens through a store-provided database lock boundary.
 */
public final class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenStore store;
    private final SecureRandom secureRandom;

    public RefreshTokenService(RefreshTokenStore store) {
        this(store, new SecureRandom());
    }

    RefreshTokenService(RefreshTokenStore store, SecureRandom secureRandom) {
        this.store = Objects.requireNonNull(store, "store");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    public RotationResult rotate(String rawToken, Instant now) {
        Objects.requireNonNull(rawToken, "rawToken");
        Objects.requireNonNull(now, "now");
        return store.withLockedToken(hash(rawToken), current -> rotateLocked(current, now));
    }

    private RotationResult rotateLocked(TokenRecord current, Instant now) {
        if (current == null) {
            return new RotationResult(Outcome.INVALID, null, null);
        }
        if (current.status() == TokenStatus.ROTATED
                || current.status() == TokenStatus.REUSED_DETECTED) {
            store.compromiseFamily(current.familyId(), now);
            return new RotationResult(Outcome.REPLAY_DETECTED, null, current.familyId());
        }
        if (current.status() != TokenStatus.ACTIVE || !current.expiresAt().isAfter(now)) {
            return new RotationResult(Outcome.INVALID, null, current.familyId());
        }
        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String nextRawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        TokenRecord rotated = current.rotated(now);
        TokenRecord next = TokenRecord.active(
                current.tokenId() + ':' + (current.generation() + 1),
                current.tenantId(),
                current.sessionId(),
                current.familyId(),
                current.generation() + 1,
                hash(nextRawToken),
                current.expiresAt());
        store.rotate(rotated, next);
        return new RotationResult(Outcome.ROTATED, nextRawToken, current.familyId());
    }

    public static String hash(String rawToken) {
        Objects.requireNonNull(rawToken, "rawToken");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * Implementations must hold a cross-instance atomic lock for the whole callback.
     */
    public interface RefreshTokenStore {

        <T> T withLockedToken(String tokenHash, Function<TokenRecord, T> action);

        void rotate(TokenRecord oldToken, TokenRecord newToken);

        void compromiseFamily(String familyId, Instant detectedAt);
    }

    public record TokenRecord(
            String tokenId,
            String tenantId,
            String sessionId,
            String familyId,
            long generation,
            String tokenHash,
            TokenStatus status,
            Instant expiresAt,
            Instant rotatedAt
    ) {

        public TokenRecord {
            Objects.requireNonNull(tokenId, "tokenId");
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(familyId, "familyId");
            Objects.requireNonNull(tokenHash, "tokenHash");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(expiresAt, "expiresAt");
            if (generation < 0) {
                throw new IllegalArgumentException("generation must not be negative");
            }
        }

        public static TokenRecord active(
                String tokenId,
                String tenantId,
                String sessionId,
                String familyId,
                long generation,
                String tokenHash,
                Instant expiresAt) {
            return new TokenRecord(
                    tokenId,
                    tenantId,
                    sessionId,
                    familyId,
                    generation,
                    tokenHash,
                    TokenStatus.ACTIVE,
                    expiresAt,
                    null);
        }

        TokenRecord rotated(Instant now) {
            return new TokenRecord(
                    tokenId,
                    tenantId,
                    sessionId,
                    familyId,
                    generation,
                    tokenHash,
                    TokenStatus.ROTATED,
                    expiresAt,
                    now);
        }

        @Override
        public String toString() {
            return "TokenRecord[tokenId=" + tokenId
                    + ", tenantId=" + tenantId
                    + ", sessionId=" + sessionId
                    + ", familyId=" + familyId
                    + ", generation=" + generation
                    + ", tokenHash=<redacted>, status=" + status
                    + ", expiresAt=" + expiresAt
                    + ", rotatedAt=" + rotatedAt + ']';
        }
    }

    public record RotationResult(
            Outcome outcome,
            String refreshToken,
            String familyId
    ) {

        @Override
        public String toString() {
            return "RotationResult[outcome=" + outcome
                    + ", refreshToken=<redacted>, familyId=" + familyId + ']';
        }
    }

    public enum Outcome {
        ROTATED,
        REPLAY_DETECTED,
        INVALID
    }

    public enum TokenStatus {
        ACTIVE,
        ROTATED,
        REUSED_DETECTED,
        REVOKED,
        EXPIRED
    }

    public enum FamilyStatus {
        ACTIVE,
        COMPROMISED
    }
}
