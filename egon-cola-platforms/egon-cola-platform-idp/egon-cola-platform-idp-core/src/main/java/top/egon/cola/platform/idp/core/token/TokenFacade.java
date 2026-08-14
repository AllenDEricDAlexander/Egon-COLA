package top.egon.cola.platform.idp.core.token;

import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.PrincipalType;
import top.egon.cola.platform.idp.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.TokenSigner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 无状态 USER Token 门面：Access Token 短时自包含，Refresh Token 稳定且只在 IdP 保存摘要。
 * Stateless USER-token facade: short-lived self-contained access tokens and
 * stable refresh tokens whose digests are stored only at the IdP.
 */
public final class TokenFacade {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration MINIMUM_REFRESH_TTL = Duration.ofDays(1);
    private static final Duration MAXIMUM_REFRESH_TTL = Duration.ofDays(30);

    private final TokenSigner signer;
    private final RefreshTokenStore refreshTokens;
    private final IdentityUserStore users;
    private final TenantMembershipPort memberships;
    private final Clock clock;
    private final Supplier<String> idGenerator;
    private final String userAudience;

    public TokenFacade(
            TokenSigner signer,
            RefreshTokenStore refreshTokens,
            IdentityUserStore users,
            TenantMembershipPort memberships,
            Clock clock,
            Supplier<String> idGenerator,
            String userAudience
    ) {
        this.signer = Objects.requireNonNull(signer, "signer");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.users = Objects.requireNonNull(users, "users");
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        if (userAudience == null || userAudience.isBlank()) {
            throw new IllegalArgumentException("userAudience is required");
        }
        this.userAudience = userAudience.trim();
    }

    /**
     * Signs one USER AT and one stable RT after checking the active tenant membership.
     * 登录完成后检查租户成员关系，签发一个 USER AT 与一个稳定 RT。
     */
    public UserTokenPair issue(
            AuthenticatedIdentity identity,
            String tenantId,
            Duration refreshTokenTtl
    ) {
        Objects.requireNonNull(identity, "identity");
        String subject = required(identity.identitySub(), "identitySub");
        String tenant = required(tenantId, "tenantId");
        Duration refreshTtl = refreshTtl(refreshTokenTtl);
        Instant now = tokenTime();
        activeUser(subject);
        requireActiveMembership(subject, tenant);
        Instant refreshExpiresAt = now.plus(refreshTtl);
        RefreshTokenClaims refreshClaims = new RefreshTokenClaims(
                subject, tenant, nextId(), now, now, refreshExpiresAt);
        String refreshToken = signer.signRefresh(refreshClaims);
        refreshTokens.create(new RefreshTokenRecord(
                digest(refreshToken), subject, tenant, now, refreshExpiresAt,
                RefreshTokenRecord.Status.ACTIVE));
        AccessTokenClaims accessClaims = accessClaims(subject, tenant, now);
        return new UserTokenPair(
                signer.signAccess(accessClaims), refreshToken,
                accessClaims.expiresAt(), refreshExpiresAt);
    }

    /**
     * Validates the stable RT and returns a fresh AT while preserving the RT string and exp.
     * 校验稳定 RT 后只签发新 AT，RT 原文和绝对过期时间保持不变。
     */
    public UserTokenPair refresh(String rawRefreshToken) {
        Instant now = tokenTime();
        RefreshTokenClaims claims = verifiedRefresh(rawRefreshToken, now);
        RefreshTokenRecord record = refreshTokens.findValid(digest(rawRefreshToken), now)
                .orElseThrow(TokenFacade::invalidGrant);
        if (!record.identitySub().equals(claims.subject())
                || !record.tenantId().equals(claims.tenantId())
                || !record.expiresAt().equals(claims.expiresAt())
                || !claims.expiresAt().isAfter(now)) {
            throw invalidGrant();
        }
        activeUser(claims.subject());
        requireActiveMembership(claims.subject(), claims.tenantId());
        AccessTokenClaims accessClaims = accessClaims(
                claims.subject(), claims.tenantId(), now);
        return new UserTokenPair(
                signer.signAccess(accessClaims), rawRefreshToken,
                accessClaims.expiresAt(), claims.expiresAt());
    }

    /**
     * Re-signs a short-lived USER AT after an already authenticated step-up.
     * The stable RT is deliberately untouched and no server-side session is created.
     */
    public AccessTokenIssue issueStepUp(
            String identitySub,
            String tenantId,
            AuthenticationContext authenticationContext) {
        String subject = required(identitySub, "identitySub");
        String tenant = required(tenantId, "tenantId");
        AuthenticationContext context = Objects.requireNonNull(
                authenticationContext, "authenticationContext");
        if (!"STRONG".equals(context.acr())) {
            throw new IllegalArgumentException("step-up requires STRONG authentication");
        }
        Instant now = tokenTime();
        activeUser(subject);
        requireActiveMembership(subject, tenant);
        AccessTokenClaims claims = accessClaims(subject, tenant, now, context);
        return new AccessTokenIssue(signer.signAccess(claims), claims.expiresAt());
    }

    /** Revokes one exact RT without affecting other devices or tenants. */
    public void revoke(String rawRefreshToken) {
        Instant now = tokenTime();
        RefreshTokenClaims claims = verifiedRefresh(rawRefreshToken, now);
        refreshTokens.revokeToken(digest(rawRefreshToken), "CLIENT_LOGOUT", now);
    }

    /** Revokes every stable RT for a subject. */
    public void revokeSubject(String identitySub) {
        refreshTokens.revokeSubject(
                required(identitySub, "identitySub"), "GLOBAL_LOGOUT", tokenTime());
    }

    /** Compatibility name for callers that describe the operation as global logout. */
    public void logoutAll(String identitySub) {
        revokeSubject(identitySub);
    }

    private AccessTokenClaims accessClaims(String subject, String tenantId, Instant now) {
        return accessClaims(
                subject,
                tenantId,
                now,
                AuthenticationContext.of("PASSWORD", now));
    }

    private AccessTokenClaims accessClaims(
            String subject,
            String tenantId,
            Instant now,
            AuthenticationContext authenticationContext) {
        return new AccessTokenClaims(
                PrincipalType.USER,
                subject,
                tenantId,
                nextId(),
                userAudience,
                now,
                now,
                now.plus(ACCESS_TOKEN_TTL),
                authenticationContext);
    }

    private RefreshTokenClaims verifiedRefresh(String rawRefreshToken, Instant now) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()
                || rawRefreshToken.length() > 8_192
                || !rawRefreshToken.equals(rawRefreshToken.trim())) {
            throw invalidGrant();
        }
        RefreshTokenClaims claims;
        try {
            claims = signer.verifyRefresh(rawRefreshToken);
        } catch (RuntimeException exception) {
            throw invalidGrant();
        }
        if (!claims.expiresAt().isAfter(now)
                || claims.notBefore().isAfter(now)
                || !claims.issuedAt().isAfter(Instant.EPOCH)) {
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

    private void requireActiveMembership(String identitySub, String tenantId) {
        try {
            TenantMembershipPort.TenantMembership membership = memberships.resolve(
                    identitySub, tenantId);
            if (membership == null
                    || membership.status() != TenantMembershipPort.MembershipStatus.ACTIVE
                    || !identitySub.equals(membership.identitySub())
                    || !tenantId.equals(membership.tenantId())) {
                throw invalidGrant();
            }
        } catch (TenantMembershipPort.TenantMembershipException exception) {
            throw invalidGrant();
        }
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

    private static Duration refreshTtl(Duration value) {
        Objects.requireNonNull(value, "refreshTokenTtl");
        if (value.compareTo(MINIMUM_REFRESH_TTL) < 0
                || value.compareTo(MAXIMUM_REFRESH_TTL) > 0) {
            throw new IllegalArgumentException("refreshTokenTtl is out of range");
        }
        return value;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static String digest(String rawToken) {
        try {
            byte[] value = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static TokenException invalidGrant() {
        return new TokenException("invalid_grant");
    }

    /**
     * In-process AT result used only to set the HttpOnly cookie.
     */
    public record AccessTokenIssue(String accessToken, Instant expiresAt) {
        public AccessTokenIssue {
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("accessToken is required");
            }
            Objects.requireNonNull(expiresAt, "expiresAt");
        }

        @Override
        public String toString() {
            return "AccessTokenIssue[accessToken=<redacted>, expiresAt="
                    + expiresAt + ']';
        }
    }
}
