package top.egon.cola.component.ddc.admin.security.admission;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 使用 IdP JWK 和 Redis Resource 运行态投影校验 DDC 准入票据。
 *
 * <p>Verifies DDC admission tickets using IdP JWKs and the Redis Resource runtime
 * projection.</p>
 *
 * <p>该领域服务同时供 CONFIG_CLIENT 与 Provider 租约入口使用，避免两条路径形成不同的
 * 安全规则。</p>
 *
 * <p>This domain service is shared by CONFIG_CLIENT and provider lease entrances so the two
 * paths cannot drift into different security rules.</p>
 */
public final class IdpJwtDdcAdmissionVerifier implements DdcAdmissionVerifier {

    /** Admission Ticket JOSE 类型；Admission Ticket JOSE type. */
    private static final String ADMISSION_TYPE = "rs-admission+jwt";

    /** Admission Ticket 用途；Admission Ticket use. */
    private static final String ADMISSION_TOKEN_USE = "resource_server_admission";

    /** JWT 签名和标准声明解码器；JWT signature and standard-claim decoder. */
    private final JwtDecoder decoder;

    /** 按 Resource Server 标识读取 IdP 权威运行态投影；authoritative IdP runtime state reader. */
    private final IdentityResourceServerStateReader resourceStates;

    /** 期望 IdP Issuer；expected IdP issuer. */
    private final String issuer;

    /** 期望 DDC Audience；expected DDC audience. */
    private final String audience;

    /** 安全时间源；security clock. */
    private final Clock clock;

    /**
     * 创建生产用 IdP JWT 准入校验器。
     *
     * <p>Creates the production IdP JWT admission verifier.</p>
     *
     * @param decoder IdP JWK JWT 解码器；IdP JWK JWT decoder
     * @param resourceStates IdP Resource Server 运行态读取器；IdP Resource Server runtime reader
     * @param issuer 期望 IdP Issuer；expected IdP issuer
     * @param audience 期望 DDC Audience；expected DDC audience
     */
    public IdpJwtDdcAdmissionVerifier(
            JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            String issuer,
            String audience
    ) {
        this(
                decoder,
                resourceStates,
                issuer,
                audience,
                Clock.systemUTC()
        );
    }

    /**
     * 创建可注入投影读取器和时间源的校验器。
     *
     * <p>Creates a verifier with injectable projection reader and clock.</p>
     *
     * @param decoder JWT 解码器；JWT decoder
     * @param resourceStates IdP Resource Server 运行态读取器；IdP Resource Server runtime reader
     * @param issuer 期望 Issuer；expected issuer
     * @param audience 期望 Audience；expected audience
     * @param clock 安全时间源；security clock
     */
    IdpJwtDdcAdmissionVerifier(
            JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            String issuer,
            String audience,
            Clock clock
    ) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.resourceStates = Objects.requireNonNull(
                resourceStates,
                "resourceStates"
        );
        this.issuer = required(issuer, "issuer");
        this.audience = required(audience, "audience");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DdcAdmissionClaims verify(
            String ticket,
            String bizCode,
            String appCode,
            String env,
            String instanceId
    ) {
        if (ticket == null || ticket.isBlank()) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_REQUIRED);
        }
        try {
            Jwt jwt = decode(ticket);
            validateEnvelope(jwt);
            DdcAdmissionClaims claims = claims(jwt);
            validateBinding(claims, bizCode, appCode, env, instanceId);
            validateProjection(claims);
            return claims;
        } catch (DdcAdmissionException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID, failure);
        }
    }

    /**
     * 解码并验证 JWT 签名。
     *
     * <p>Decodes the JWT and verifies its signature.</p>
     *
     * @param ticket 原始票据；raw ticket
     * @return 已解码 JWT；decoded JWT
     */
    private Jwt decode(String ticket) {
        try {
            return decoder.decode(ticket);
        } catch (JwtException | IllegalArgumentException failure) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID);
        }
    }

    /**
     * 校验独立 JWT 类型、用途、Issuer、Audience 和时间窗口。
     *
     * <p>Validates the isolated JWT type, use, issuer, audience, and time window.</p>
     *
     * @param jwt 已验签 JWT；signature-verified JWT
     */
    private void validateEnvelope(Jwt jwt) {
        if (!ADMISSION_TYPE.equals(jwt.getHeaders().get("typ"))
                || !ADMISSION_TOKEN_USE.equals(jwt.getClaimAsString("token_use"))
                || !issuer.equals(jwt.getIssuer() == null
                        ? null : jwt.getIssuer().toString())
                || !List.of(audience).equals(jwt.getAudience())) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID);
        }
        Instant now = clock.instant();
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_EXPIRED);
        }
        Instant issuedAt = jwt.getIssuedAt();
        Instant notBefore = jwt.getNotBefore();
        if (issuedAt == null || issuedAt.isAfter(now)
                || notBefore == null || notBefore.isAfter(now)
                || !expiresAt.isAfter(issuedAt)) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID);
        }
    }

    /**
     * 提取且严格校验准入审计声明。
     *
     * <p>Extracts and strictly validates admission audit claims.</p>
     *
     * @param jwt 已校验信封的 JWT；JWT with a validated envelope
     * @return 审计声明；audit claims
     */
    private DdcAdmissionClaims claims(Jwt jwt) {
        try {
            Object version = jwt.getClaim("resource_version");
            if (!(version instanceof Number number)) {
                throw new IllegalArgumentException("resource_version must be numeric");
            }
            return new DdcAdmissionClaims(
                    required(jwt.getSubject(), "sub"),
                    claim(jwt, "resource"),
                    number.longValue(),
                    claim(jwt, "biz"),
                    claim(jwt, "app"),
                    claim(jwt, "env"),
                    claim(jwt, "instance_id"),
                    claim(jwt, "credential_id"),
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt()
            );
        } catch (IllegalArgumentException failure) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID, failure);
        }
    }

    /**
     * 要求票据与注册请求中的业务域、应用、环境和实例完全一致。
     *
     * <p>Requires the ticket to exactly match the request business, application, environment,
     * and instance.</p>
     *
     * @param claims 准入声明；admission claims
     * @param bizCode 请求业务域；requested business domain
     * @param appCode 请求应用；requested application
     * @param env 请求环境；requested environment
     * @param instanceId 请求实例；requested instance
     */
    private void validateBinding(
            DdcAdmissionClaims claims,
            String bizCode,
            String appCode,
            String env,
            String instanceId
    ) {
        if (!claims.bizCode().equals(bizCode)
                || !claims.appCode().equals(appCode)
                || !claims.environment().equals(env)
                || !claims.instanceId().equals(instanceId)) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_BINDING_MISMATCH);
        }
    }

    /**
     * 要求 IdP Resource 投影存在、ACTIVE、版本当前且三元组和 URI 一致。
     *
     * <p>Requires an existing ACTIVE IdP Resource projection with the current version and exact
     * triple and URI.</p>
     *
     * @param claims 准入声明；admission claims
     */
    private void validateProjection(DdcAdmissionClaims claims) {
        try {
            IdentityResourceServerState projection = resourceStates.read(
                    claims.resourceServerId()
            ).orElseThrow(() -> failure(
                    DdcErrorStatus.RESOURCE_ADMISSION_INVALID));
            if (!claims.resourceServerId().equals(projection.resourceServerId())
                    || !claims.resourceUri().equals(
                    projection.resourceUri().toString())
                    || !claims.bizCode().equals(projection.bizCode())
                    || !claims.appCode().equals(projection.appCode())
                    || !claims.environment().equals(projection.environment())
                    || projection.status() != ResourceServerStatus.ACTIVE
                    || projection.version() != claims.resourceVersion()) {
                throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID);
            }
        } catch (DdcAdmissionException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw failure(DdcErrorStatus.RESOURCE_ADMISSION_INVALID, failure);
        }
    }

    /**
     * 读取必填文本声明。
     *
     * <p>Reads a required textual claim.</p>
     *
     * @param jwt JWT；JWT
     * @param name 声明名；claim name
     * @return 非空声明；non-blank claim
     */
    private String claim(Jwt jwt, String name) {
        return required(jwt.getClaimAsString(name), name);
    }

    /**
     * 校验必填字符串。
     *
     * <p>Validates a required string.</p>
     *
     * @param value 待校验值；value to validate
     * @param name 字段名；field name
     * @return 非空字符串；non-blank string
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /**
     * 创建不包含票据内容的稳定准入异常。
     *
     * <p>Creates a stable admission exception without ticket contents.</p>
     *
     * @param status 错误状态；error status
     * @return 准入异常；admission exception
     */
    private DdcAdmissionException failure(DdcErrorStatus status) {
        return new DdcAdmissionException(status);
    }

    /**
     * 创建带内部原因且不包含票据内容的稳定准入异常。
     *
     * <p>Creates a stable admission exception with an internal cause and no ticket contents.</p>
     *
     * @param status 错误状态；error status
     * @param cause 内部原因；internal cause
     * @return 准入异常；admission exception
     */
    private DdcAdmissionException failure(
            DdcErrorStatus status,
            Throwable cause
    ) {
        return new DdcAdmissionException(status, cause);
    }
}
