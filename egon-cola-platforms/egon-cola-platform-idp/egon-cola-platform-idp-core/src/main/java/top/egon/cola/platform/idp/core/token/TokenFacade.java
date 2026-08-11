package top.egon.cola.platform.idp.core.token;

import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.contract.PrincipalType;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.TokenSigner;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;
import top.egon.cola.platform.idp.core.resource.ResourceAuthorizationException;
import top.egon.cola.platform.idp.core.resource.UserResourceAccessPolicy;

import java.net.URI;
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
 * 签发、刷新和撤销单 Resource 用户 Token 的领域门面。
 * Domain facade for issuing, refreshing, and revoking single-Resource user tokens.
 */
public final class TokenFacade {

    /** 最小 Access Token 有效期。 / Minimum Access Token lifetime. */
    private static final Duration MINIMUM_ACCESS_TTL = Duration.ofMinutes(5);
    /** 最大 Access Token 有效期。 / Maximum Access Token lifetime. */
    private static final Duration MAXIMUM_ACCESS_TTL = Duration.ofMinutes(30);
    /** 最小 Refresh Token 有效期。 / Minimum Refresh Token lifetime. */
    private static final Duration MINIMUM_REFRESH_TTL = Duration.ofDays(1);
    /** 最大 Refresh Token 有效期。 / Maximum Refresh Token lifetime. */
    private static final Duration MAXIMUM_REFRESH_TTL = Duration.ofDays(30);

    /** Token 签名与 Refresh 校验端口。 / Token signing and Refresh-verification port. */
    private final TokenSigner signer;
    /** Refresh Family 存储。 / Refresh-family store. */
    private final RefreshTokenStore refreshTokens;
    /** 用户聚合存储。 / Identity-user aggregate store. */
    private final IdentityUserStore users;
    /** 用户安全状态发布端口。 / User security-state publication port. */
    private final IdentityUserStatePort userStates;
    /** 身份安全事件端口。 / Identity security-event port. */
    private final IdentitySecurityEventPort securityEvents;
    /** OAuth Client 查询端口。 / OAuth Client lookup port. */
    private final OAuthClientStore clients;
    /** 统一用户 Resource 入口策略。 / Shared user Resource-entry policy. */
    private final UserResourceAccessPolicy resourceAccess;
    /** 业务时钟。 / Business clock. */
    private final Clock clock;
    /** Token 和 Family 标识生成器。 / Token and family identifier generator. */
    private final Supplier<String> idGenerator;

    /**
     * 创建用户 Token 领域门面。
     * Creates the user-token domain facade.
     *
     * @param signer Token 签名端口 / token-signing port
     * @param refreshTokens Refresh Family 存储 / refresh-family store
     * @param users 用户聚合存储 / user aggregate store
     * @param userStates 用户安全状态端口 / user security-state port
     * @param securityEvents 身份安全事件端口 / identity security-event port
     * @param clients OAuth Client 查询端口 / OAuth Client lookup port
     * @param resourceAccess USER Resource 入口策略 / USER Resource entry policy
     * @param clock 业务时钟 / business clock
     * @param idGenerator 安全标识生成器 / secure identifier generator
     */
    public TokenFacade(
            TokenSigner signer,
            RefreshTokenStore refreshTokens,
            IdentityUserStore users,
            IdentityUserStatePort userStates,
            IdentitySecurityEventPort securityEvents,
            OAuthClientStore clients,
            UserResourceAccessPolicy resourceAccess,
            Clock clock,
            Supplier<String> idGenerator) {
        this.signer = Objects.requireNonNull(signer, "signer");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.users = Objects.requireNonNull(users, "users");
        this.userStates = Objects.requireNonNull(userStates, "userStates");
        this.securityEvents = Objects.requireNonNull(securityEvents, "securityEvents");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.resourceAccess = Objects.requireNonNull(resourceAccess, "resourceAccess");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    /**
     * 从已完成换码阶段 Resource 复核的授权码签发 Token Pair。
     * Issues a token pair from a code whose Resource was revalidated during exchange.
     *
     * @param authorizationCode 已重新校验的授权码载荷 / revalidated authorization-code payload
     * @param accessTokenTtl Access Token 有效期 / access-token lifetime
     * @param refreshTokenTtl Refresh Token 有效期 / refresh-token lifetime
     * @return Access 与 Refresh Token 组合 / access and refresh token pair
     */
    public TokenPair issue(
            AuthorizationCode authorizationCode,
            Duration accessTokenTtl,
            Duration refreshTokenTtl) {
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
                authorizationCode.identitySub(), authorizationCode.tenantId(),
                sessionId, authorizationCode.clientId(), user.tokenVersion(),
                authorizationCode.resourceVersion(),
                authorizationCode.resourceUri().toString(),
                authorizationCode.nonce(), accessTtl, now);
        RefreshTokenClaims refreshClaims = new RefreshTokenClaims(
                authorizationCode.identitySub(), authorizationCode.tenantId(),
                sessionId, authorizationCode.clientId(), familyId, nextId(),
                0L, user.tokenVersion(), authorizationCode.resourceServerId(),
                authorizationCode.resourceUri().toString(),
                authorizationCode.resourceVersion(), authorizationCode.nonce(),
                now, refreshExpiresAt);
        String accessToken = signer.signAccess(accessClaims);
        String refreshToken = signer.signRefresh(refreshClaims);
        refreshTokens.create(new RefreshFamily(
                familyId, user.id(), authorizationCode.tenantId(), sessionId,
                authorizationCode.clientId(), user.tokenVersion(), 0L,
                digest(refreshToken), RefreshFamily.Status.ACTIVE, now, now,
                refreshExpiresAt));
        return new TokenPair(accessToken, refreshToken, familyId, sessionId,
                accessClaims.expiresAt(), refreshExpiresAt);
    }

    /**
     * 校验 Refresh Token 的精确 Resource，重新执行入口策略并原子轮换 Family。
     * Validates the exact Refresh Resource, reruns entry policy, and atomically rotates the family.
     *
     * @param rawRefreshToken 原始 Refresh Token / raw refresh token
     * @param clientId Client 标识 / Client identifier
     * @param resource 精确 Resource URI / exact Resource URI
     * @param accessTokenTtl 新 Access Token 有效期 / new access-token lifetime
     * @return 轮换后的 Token 对 / rotated token pair
     */
    public TokenPair refresh(
            String rawRefreshToken,
            String clientId,
            String resource,
            Duration accessTokenTtl) {
        Duration accessTtl = accessTtl(accessTokenTtl);
        Instant now = tokenTime();
        RefreshTokenClaims current = verifiedRefresh(rawRefreshToken, clientId, now);
        URI requestedResource = resource(resource);
        if (!current.resourceUri().equals(requestedResource.toString())) {
            throw invalidGrant();
        }
        IdentityUser user = activeUser(current.subject());
        if (user.tokenVersion() != current.tokenVersion()) {
            throw invalidGrant();
        }
        OAuthClient client = clients.findById(current.clientId())
                .orElseThrow(TokenFacade::invalidGrant);
        UserResourceAccessPolicy.UserResourceAccess access = refreshedAccess(
                client, requestedResource, current);
        if (!current.resourceServerId().equals(access.resourceServerId())) {
            throw invalidGrant();
        }
        RefreshTokenClaims successor = new RefreshTokenClaims(
                current.subject(), current.tenantId(), current.sessionId(),
                current.clientId(), current.familyId(), nextId(),
                Math.addExact(current.generation(), 1L), current.tokenVersion(),
                access.resourceServerId(), access.resourceUri().toString(),
                access.resourceVersion(), current.nonce(), now,
                current.expiresAt());
        String successorToken = signer.signRefresh(successor);
        RefreshTokenStore.RotationResult rotation = refreshTokens.rotate(
                new RefreshTokenStore.RotationCommand(
                        current.familyId(), digest(rawRefreshToken),
                        digest(successorToken), successor.generation(),
                        current.subject(), current.tokenVersion(),
                        current.expiresAt(), now));
        if (rotation.outcome() == RefreshTokenStore.RotationOutcome.REPLAY) {
            revokeAllSecurityState(user, "REFRESH_TOKEN_REPLAY", now);
            throw new RefreshReplayException();
        }
        if (rotation.outcome() != RefreshTokenStore.RotationOutcome.ROTATED) {
            throw invalidGrant();
        }
        AccessTokenClaims accessClaims = accessClaims(
                current.subject(), current.tenantId(), current.sessionId(),
                current.clientId(), current.tokenVersion(),
                access.resourceVersion(), access.resourceUri().toString(),
                current.nonce(), accessTtl, now);
        return new TokenPair(signer.signAccess(accessClaims), successorToken,
                current.familyId(), current.sessionId(),
                accessClaims.expiresAt(), current.expiresAt());
    }

    /**
     * 撤销一个 Refresh Family。
     * Revokes one Refresh family.
     *
     * @param rawRefreshToken 原始 Refresh Token / raw refresh token
     * @param clientId Client 标识 / Client identifier
     */
    public void revoke(String rawRefreshToken, String clientId) {
        RefreshTokenClaims claims = verifiedRefresh(
                rawRefreshToken, clientId, clock.instant());
        refreshTokens.revokeFamily(
                claims.familyId(), "CLIENT_LOGOUT", clock.instant());
    }

    /**
     * 全局登出用户并推进安全状态版本。
     * Globally logs out a user and advances the security-state version.
     *
     * @param identitySub 用户身份标识 / user identity subject
     */
    public void logoutAll(String identitySub) {
        IdentityUser user = activeUser(identitySub);
        revokeAllSecurityState(user, "GLOBAL_LOGOUT", clock.instant());
    }

    /**
     * 重新执行 Refresh 阶段 Resource 入口策略并区分拒绝与暂时不可用。
     * Reruns Refresh-stage Resource entry policy and distinguishes denial from temporary unavailability.
     *
     * @param client OAuth Client / OAuth Client
     * @param resource 目标 Resource URI / target Resource URI
     * @param current 当前 Refresh Token 声明 / current refresh-token claims
     * @return 最新 Resource 授权上下文 / latest Resource authorization context
     */
    private UserResourceAccessPolicy.UserResourceAccess refreshedAccess(
            OAuthClient client,
            URI resource,
            RefreshTokenClaims current) {
        try {
            return resourceAccess.authorize(client, resource, current.subject(),
                    current.tenantId(), current.sessionId());
        } catch (UserResourceAccessAuthorizationPort.AccessUnavailableException exception) {
            throw new TokenException("temporarily_unavailable");
        } catch (ResourceAuthorizationException
                 | TenantMembershipPort.TenantMembershipException exception) {
            throw invalidGrant();
        }
    }

    /**
     * 创建一个单 Resource USER Access Token 声明。
     * Creates single-Resource USER Access Token claims.
     *
     * @param subject 用户主体 / user subject
     * @param tenantId 租户标识 / tenant identifier
     * @param sessionId 会话标识 / session identifier
     * @param clientId Client 标识 / Client identifier
     * @param tokenVersion 用户安全状态版本 / user security-state version
     * @param resourceVersion Resource Server 版本 / Resource Server version
     * @param resource Resource URI / Resource URI
     * @param nonce 原始授权 Nonce / original authorization nonce
     * @param accessTtl Access Token 有效期 / access-token lifetime
     * @param now 签发时间 / issuance time
     * @return Access Token 声明 / access-token claims
     */
    private AccessTokenClaims accessClaims(
            String subject,
            String tenantId,
            String sessionId,
            String clientId,
            long tokenVersion,
            long resourceVersion,
            String resource,
            String nonce,
            Duration accessTtl,
            Instant now) {
        return new AccessTokenClaims(
                PrincipalType.USER, subject, tenantId, sessionId, clientId,
                nextId(), tokenVersion, resourceVersion, resource, nonce,
                now, now, now.plus(accessTtl));
    }

    /**
     * 校验 Refresh Token 签名、Client 和时效。
     * Validates Refresh Token signature, Client, and lifetime.
     *
     * @param rawRefreshToken 原始 Refresh Token / raw refresh token
     * @param clientId Client 标识 / Client identifier
     * @param now 当前时间 / current time
     * @return 已验证 Refresh Token 声明 / verified refresh-token claims
     */
    private RefreshTokenClaims verifiedRefresh(
            String rawRefreshToken, String clientId, Instant now) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()
                || rawRefreshToken.length() > 8_192
                || clientId == null || clientId.isBlank()) {
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

    /**
     * 读取 ACTIVE 用户。
     * Loads an ACTIVE user.
     *
     * @param identitySub 用户身份标识 / user identity subject
     * @return ACTIVE 用户聚合 / ACTIVE user aggregate
     */
    private IdentityUser activeUser(String identitySub) {
        IdentityUser user = users.findById(identitySub)
                .orElseThrow(TokenFacade::invalidGrant);
        if (user.status() != IdentityUserStatus.ACTIVE) {
            throw invalidGrant();
        }
        return user;
    }

    /**
     * 撤销用户全部 Token 状态并写入安全审计。
     * Revokes all user token state and appends a security audit event.
     *
     * @param current 当前用户聚合 / current user aggregate
     * @param reason 撤销原因 / revocation reason
     * @param now 撤销时间 / revocation time
     */
    private void revokeAllSecurityState(
            IdentityUser current, String reason, Instant now) {
        IdentityUser revoked = current.revokeSecurityState();
        users.save(revoked, current.version());
        refreshTokens.revokeSubject(revoked.id(), reason, now);
        userStates.publish(new IdentityUserState(
                revoked.id(),
                IdentityUserState.Status.valueOf(revoked.status().name()),
                revoked.tokenVersion(), now));
        userStates.revokeFamilies(revoked.id(), revoked.tokenVersion(), reason);
        securityEvents.append(new IdentitySecurityEvent(
                "IDENTITY_TOKEN_REVOKED", revoked.id(), reason, "oauth",
                revoked.tokenVersion(), now));
    }

    /**
     * 获取下一个安全标识。
     * Obtains the next secure identifier.
     *
     * @return 安全标识 / secure identifier
     */
    private String nextId() {
        String value = idGenerator.get();
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalStateException("ID generator returned invalid value");
        }
        return value;
    }

    /**
     * 获取秒精度 Token 时间。
     * Returns token time truncated to seconds.
     *
     * @return 秒精度当前时间 / current time truncated to seconds
     */
    private Instant tokenTime() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * 解析精确 Resource URI。
     * Parses an exact Resource URI.
     *
     * @param value 原始 Resource 参数 / raw Resource parameter
     * @return 已校验 Resource URI / validated Resource URI
     */
    private URI resource(String value) {
        try {
            if (value == null || value.isBlank() || !value.equals(value.trim())) {
                throw new IllegalArgumentException("resource is required");
            }
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getScheme() == null
                    || uri.getScheme().isBlank() || uri.getFragment() != null
                    || !uri.equals(uri.normalize())) {
                throw new IllegalArgumentException("invalid Resource URI");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw invalidGrant();
        }
    }

    /**
     * 校验 Access Token 有效期。
     * Validates an Access Token lifetime.
     *
     * @param value 原始有效期 / raw lifetime
     * @return 已校验有效期 / validated lifetime
     */
    private static Duration accessTtl(Duration value) {
        return durationInRange(value, MINIMUM_ACCESS_TTL, MAXIMUM_ACCESS_TTL,
                "accessTokenTtl");
    }

    /**
     * 校验 Refresh Token 有效期。
     * Validates a Refresh Token lifetime.
     *
     * @param value 原始有效期 / raw lifetime
     * @return 已校验有效期 / validated lifetime
     */
    private static Duration refreshTtl(Duration value) {
        return durationInRange(value, MINIMUM_REFRESH_TTL, MAXIMUM_REFRESH_TTL,
                "refreshTokenTtl");
    }

    /**
     * 校验时长范围。
     * Validates a duration range.
     *
     * @param value 待校验时长 / duration to validate
     * @param minimum 最小值 / minimum
     * @param maximum 最大值 / maximum
     * @param field 字段名 / field name
     * @return 已校验时长 / validated duration
     */
    private static Duration durationInRange(
            Duration value, Duration minimum, Duration maximum, String field) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(field + " is out of range");
        }
        return value;
    }

    /**
     * 计算不保存原始 Token 的 SHA-256 摘要。
     * Computes a SHA-256 digest so raw tokens are never stored.
     *
     * @param rawToken 原始 Token / raw token
     * @return URL-safe 摘要 / URL-safe digest
     */
    private static String digest(String rawToken) {
        try {
            byte[] value = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 创建 invalid_grant Token 异常。
     * Creates an invalid_grant token exception.
     *
     * @return Token 异常 / token exception
     */
    private static TokenException invalidGrant() {
        return new TokenException("invalid_grant");
    }

    /**
     * Access Token 与仅通过 Cookie 传输的 Refresh Token 组合。
     * Access Token and cookie-only Refresh Token pair.
     *
     * @param accessToken Access Token 原文 / raw access token
     * @param refreshToken Refresh Token 原文 / raw refresh token
     * @param familyId Refresh Family 标识 / refresh-family identifier
     * @param sessionId 身份会话标识 / identity-session identifier
     * @param accessExpiresAt Access Token 过期时间 / access-token expiration
     * @param refreshExpiresAt Refresh Token 过期时间 / refresh-token expiration
     */
    public record TokenPair(
            String accessToken,
            String refreshToken,
            String familyId,
            String sessionId,
            Instant accessExpiresAt,
            Instant refreshExpiresAt) {

        /**
         * 防止日志意外输出 Token 原文。
         * Prevents raw token values from appearing in logs.
         *
         * @return 已脱敏 Token 对描述 / redacted token-pair description
         */
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
