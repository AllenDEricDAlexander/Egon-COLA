package top.egon.cola.platform.idp.core.token;

import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.port.TokenSigner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TokenFacade {

    private static final Duration MINIMUM_ACCESS_TTL = Duration.ofMinutes(5);
    private static final Duration MAXIMUM_ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration MINIMUM_REFRESH_TTL = Duration.ofDays(1);
    private static final Duration MAXIMUM_REFRESH_TTL = Duration.ofDays(30);

    private final TokenSigner signer;
    private final RefreshTokenStore refreshTokens;
    private final IdentityUserStore users;
    private final IdentityUserStatePort userStates;
    private final IdentitySecurityEventPort securityEvents;
    private final Clock clock;
    private final Supplier<String> idGenerator;

    public TokenFacade(
            TokenSigner signer,
            RefreshTokenStore refreshTokens,
            IdentityUserStore users,
            IdentityUserStatePort userStates,
            IdentitySecurityEventPort securityEvents,
            Clock clock,
            Supplier<String> idGenerator
    ) {
        this.signer = Objects.requireNonNull(signer, "signer");
        this.refreshTokens = Objects.requireNonNull(
                refreshTokens,
                "refreshTokens"
        );
        this.users = Objects.requireNonNull(users, "users");
        this.userStates = Objects.requireNonNull(userStates, "userStates");
        this.securityEvents = Objects.requireNonNull(
                securityEvents,
                "securityEvents"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(
                idGenerator,
                "idGenerator"
        );
    }

    public TokenPair issue(
            AuthorizationCode authorizationCode,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {
        Objects.requireNonNull(authorizationCode, "authorizationCode");
        Duration accessTtl = accessTtl(accessTokenTtl);
        Duration refreshTtl = refreshTtl(refreshTokenTtl);
        Instant now = tokenTime();
        if (!authorizationCode.expiresAt().isAfter(now)) {
            throw invalidGrant();
        }
        IdentityUser user = activeUser(authorizationCode.identitySub());
        String familyId = nextId();
        String sessionId = authorizationCode.sessionId();
        Instant refreshExpiresAt = now.plus(refreshTtl);
        AccessTokenClaims accessClaims = accessClaims(
                authorizationCode,
                sessionId,
                user.tokenVersion(),
                accessTtl,
                now
        );
        RefreshTokenClaims refreshClaims = new RefreshTokenClaims(
                authorizationCode.identitySub(),
                authorizationCode.tenantId(),
                sessionId,
                authorizationCode.clientId(),
                familyId,
                nextId(),
                0L,
                user.tokenVersion(),
                List.of(authorizationCode.audience()),
                authorizationCode.nonce(),
                now,
                refreshExpiresAt
        );
        String accessToken = signer.signAccess(accessClaims);
        String refreshToken = signer.signRefresh(refreshClaims);
        refreshTokens.create(new RefreshFamily(
                familyId,
                user.id(),
                authorizationCode.tenantId(),
                sessionId,
                authorizationCode.clientId(),
                user.tokenVersion(),
                0L,
                digest(refreshToken),
                RefreshFamily.Status.ACTIVE,
                now,
                now,
                refreshExpiresAt
        ));
        return new TokenPair(
                accessToken,
                refreshToken,
                familyId,
                sessionId,
                accessClaims.expiresAt(),
                refreshExpiresAt
        );
    }

    public TokenPair refresh(
            String rawRefreshToken,
            String clientId,
            Duration accessTokenTtl
    ) {
        Duration accessTtl = accessTtl(accessTokenTtl);
        Instant now = tokenTime();
        RefreshTokenClaims current = verifiedRefresh(
                rawRefreshToken,
                clientId,
                now
        );
        IdentityUser user = activeUser(current.subject());
        if (user.tokenVersion() != current.tokenVersion()) {
            throw invalidGrant();
        }
        RefreshTokenClaims successor = new RefreshTokenClaims(
                current.subject(),
                current.tenantId(),
                current.sessionId(),
                current.clientId(),
                current.familyId(),
                nextId(),
                Math.addExact(current.generation(), 1L),
                current.tokenVersion(),
                current.audience(),
                current.nonce(),
                now,
                current.expiresAt()
        );
        String successorToken = signer.signRefresh(successor);
        RefreshTokenStore.RotationResult rotation = refreshTokens.rotate(
                new RefreshTokenStore.RotationCommand(
                        current.familyId(),
                        digest(rawRefreshToken),
                        digest(successorToken),
                        successor.generation(),
                        current.subject(),
                        current.tokenVersion(),
                        current.expiresAt(),
                        now
                )
        );
        if (rotation.outcome() == RefreshTokenStore.RotationOutcome.REPLAY) {
            revokeAllSecurityState(user, "REFRESH_TOKEN_REPLAY", now);
            throw new RefreshReplayException();
        }
        if (rotation.outcome()
                != RefreshTokenStore.RotationOutcome.ROTATED) {
            throw invalidGrant();
        }
        AccessTokenClaims accessClaims = new AccessTokenClaims(
                current.subject(),
                current.tenantId(),
                current.sessionId(),
                current.clientId(),
                nextId(),
                current.tokenVersion(),
                current.audience(),
                current.nonce(),
                now,
                now,
                now.plus(accessTtl)
        );
        return new TokenPair(
                signer.signAccess(accessClaims),
                successorToken,
                current.familyId(),
                current.sessionId(),
                accessClaims.expiresAt(),
                current.expiresAt()
        );
    }

    public void revoke(String rawRefreshToken, String clientId) {
        RefreshTokenClaims claims = verifiedRefresh(
                rawRefreshToken,
                clientId,
                clock.instant()
        );
        refreshTokens.revokeFamily(
                claims.familyId(),
                "CLIENT_LOGOUT",
                clock.instant()
        );
    }

    public void logoutAll(String identitySub) {
        IdentityUser user = activeUser(identitySub);
        revokeAllSecurityState(user, "GLOBAL_LOGOUT", clock.instant());
    }

    private AccessTokenClaims accessClaims(
            AuthorizationCode authorizationCode,
            String sessionId,
            long tokenVersion,
            Duration accessTtl,
            Instant now
    ) {
        return new AccessTokenClaims(
                authorizationCode.identitySub(),
                authorizationCode.tenantId(),
                sessionId,
                authorizationCode.clientId(),
                nextId(),
                tokenVersion,
                List.of(authorizationCode.audience()),
                authorizationCode.nonce(),
                now,
                now,
                now.plus(accessTtl)
        );
    }

    private RefreshTokenClaims verifiedRefresh(
            String rawRefreshToken,
            String clientId,
            Instant now
    ) {
        if (rawRefreshToken == null
                || rawRefreshToken.isBlank()
                || rawRefreshToken.length() > 8_192
                || clientId == null
                || clientId.isBlank()) {
            throw invalidGrant();
        }
        RefreshTokenClaims claims;
        try {
            claims = signer.verifyRefresh(rawRefreshToken);
        } catch (RuntimeException exception) {
            throw invalidGrant();
        }
        if (!claims.clientId().equals(clientId)
                || !claims.expiresAt().isAfter(now)) {
            throw invalidGrant();
        }
        return claims;
    }

    private IdentityUser activeUser(String identitySub) {
        IdentityUser user = users.findById(identitySub)
                .orElseThrow(TokenFacade::invalidGrant);
        if (user.status() != IdentityUserStatus.ACTIVE) {
            throw invalidGrant();
        }
        return user;
    }

    private void revokeAllSecurityState(
            IdentityUser current,
            String reason,
            Instant now
    ) {
        IdentityUser revoked = current.revokeSecurityState();
        users.save(revoked, current.version());
        refreshTokens.revokeSubject(revoked.id(), reason, now);
        userStates.publish(new IdentityUserState(
                revoked.id(),
                IdentityUserState.Status.valueOf(revoked.status().name()),
                revoked.tokenVersion(),
                now
        ));
        userStates.revokeFamilies(
                revoked.id(),
                revoked.tokenVersion(),
                reason
        );
        securityEvents.append(new IdentitySecurityEvent(
                "IDENTITY_TOKEN_REVOKED",
                revoked.id(),
                reason,
                "oauth",
                revoked.tokenVersion(),
                now
        ));
    }

    private String nextId() {
        String value = idGenerator.get();
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalStateException("ID generator returned invalid value");
        }
        return value;
    }

    private Instant tokenTime() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private static Duration accessTtl(Duration value) {
        return durationInRange(
                value,
                MINIMUM_ACCESS_TTL,
                MAXIMUM_ACCESS_TTL,
                "accessTokenTtl"
        );
    }

    private static Duration refreshTtl(Duration value) {
        return durationInRange(
                value,
                MINIMUM_REFRESH_TTL,
                MAXIMUM_REFRESH_TTL,
                "refreshTokenTtl"
        );
    }

    private static Duration durationInRange(
            Duration value,
            Duration minimum,
            Duration maximum,
            String field
    ) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(field + " is out of range");
        }
        return value;
    }

    private static String digest(String rawToken) {
        try {
            byte[] value = MessageDigest.getInstance("SHA-256").digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static TokenException invalidGrant() {
        return new TokenException("invalid_grant");
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            String familyId,
            String sessionId,
            Instant accessExpiresAt,
            Instant refreshExpiresAt
    ) {

        @Override
        public String toString() {
            return "TokenPair[accessToken=<redacted>, refreshToken=<redacted>"
                    + ", familyId=" + familyId
                    + ", sessionId=" + sessionId
                    + ", accessExpiresAt=" + accessExpiresAt
                    + ", refreshExpiresAt=" + refreshExpiresAt + ']';
        }
    }
}
