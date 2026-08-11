package top.egon.cola.platform.idp.core.token;

import top.egon.cola.platform.idp.contract.PrincipalType;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * IdP 授权的单 Resource、单租户 SERVICE Access Token 可信声明。
 *
 * <p>Trusted claims for an IdP-authorized, single-Resource and single-tenant SERVICE access
 * token.</p>
 *
 * @param subject 服务 Client 主体标识；service Client subject identifier
 * @param clientId OAuth Client 标识；OAuth Client identifier
 * @param audience 唯一目标 Resource URI；sole target Resource URI
 * @param tenantId 精确租户标识；exact tenant identifier
 * @param sourceBizCode 来源业务域；source business-domain code
 * @param sourceAppCode 来源应用；source application code
 * @param sourceEnvironment 来源环境；source environment
 * @param credentialId Client JWK kid；Client JWK kid
 * @param resourceVersion 目标 Resource 版本；target Resource version
 * @param scopes 本次授权的 IdP Service Scope；IdP Service scopes granted for this token
 * @param tokenId JWT ID；JWT ID
 * @param issuedAt 签发时间；issuance instant
 * @param notBefore 生效时间；not-before instant
 * @param expiresAt 过期时间；expiration instant
 */
public record ServiceAccessTokenClaims(
        String subject,
        String clientId,
        URI audience,
        String tenantId,
        String sourceBizCode,
        String sourceAppCode,
        String sourceEnvironment,
        String credentialId,
        long resourceVersion,
        Set<String> scopes,
        String tokenId,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt
) {

    /**
     * 校验并规范化 SERVICE Token 声明。
     *
     * <p>Validates and normalizes SERVICE token claims.</p>
     */
    public ServiceAccessTokenClaims {
        subject = required(subject, "subject");
        clientId = required(clientId, "clientId");
        if (!subject.equals(clientId)) {
            throw new IllegalArgumentException(
                    "SERVICE subject must equal clientId"
            );
        }
        audience = resource(audience);
        tenantId = required(tenantId, "tenantId");
        if ("*".equals(tenantId)) {
            throw new IllegalArgumentException(
                    "SERVICE tenant must be explicit"
            );
        }
        sourceBizCode = required(sourceBizCode, "sourceBizCode");
        sourceAppCode = required(sourceAppCode, "sourceAppCode");
        sourceEnvironment = required(
                sourceEnvironment,
                "sourceEnvironment"
        );
        credentialId = required(credentialId, "credentialId");
        if (resourceVersion < 0L) {
            throw new IllegalArgumentException(
                    "resourceVersion must not be negative"
            );
        }
        scopes = scopes(scopes);
        tokenId = required(tokenId, "tokenId");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        notBefore = Objects.requireNonNull(notBefore, "notBefore");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (notBefore.isBefore(issuedAt) || !expiresAt.isAfter(notBefore)) {
            throw new IllegalArgumentException(
                    "invalid SERVICE access token time range"
            );
        }
    }

    /**
     * 返回固定的服务主体类型。
     *
     * <p>Returns the fixed service principal type.</p>
     *
     * @return {@link PrincipalType#SERVICE}
     */
    public PrincipalType principalType() {
        return PrincipalType.SERVICE;
    }

    /**
     * 校验唯一目标 Resource URI。
     *
     * <p>Validates the sole target Resource URI.</p>
     *
     * @param value 目标 Resource URI；target Resource URI
     * @return 已校验 URI；validated URI
     */
    private static URI resource(URI value) {
        Objects.requireNonNull(value, "audience");
        if (!value.isAbsolute()
                || value.getScheme() == null
                || value.getScheme().isBlank()
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException(
                    "audience must be an absolute Resource URI without a fragment"
            );
        }
        return value;
    }

    /**
     * 生成排序、去重且不可变的非空 Scope 集合。
     *
     * <p>Produces a sorted, distinct, immutable, and non-empty scope set.</p>
     *
     * @param values 原始 Scope；raw scopes
     * @return 已规范化 Scope；normalized scopes
     */
    private static Set<String> scopes(Set<String> values) {
        Objects.requireNonNull(values, "scopes");
        TreeSet<String> result = new TreeSet<>();
        values.forEach(value -> result.add(required(value, "scope")));
        if (result.isEmpty() || result.size() != values.size()) {
            throw new IllegalArgumentException(
                    "scopes must be non-empty and distinct"
            );
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * 校验必填且不带首尾空白的文本。
     *
     * <p>Validates required text without surrounding whitespace.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
