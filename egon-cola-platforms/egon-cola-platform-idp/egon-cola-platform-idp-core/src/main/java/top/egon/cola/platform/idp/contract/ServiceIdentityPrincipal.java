package top.egon.cola.platform.idp.contract;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 由 IdP Service Access Token 解析出的服务身份。
 *
 * <p>Service identity parsed from an IdP Service Access Token.</p>
 *
 * @param subject           服务主体，等于 Client 标识；service subject, equal to the Client identifier
 * @param tenantId          Token 绑定租户，PLATFORM 时为空；tenant bound to the Token, null for PLATFORM
 * @param clientId          Confidential Client 标识；Confidential Client identifier
 * @param tokenId           Access Token jti；Access Token jti
 * @param resourceUri       目标 Resource URI；target Resource URI
 * @param resourceVersion   目标 Resource 版本；target Resource version
 * @param scopes            IdP 授权 Scope；scopes authorized by IdP
 * @param sourceBizCode     源业务域；source business domain
 * @param sourceAppCode     源应用；source application
 * @param sourceEnvironment 源环境；source environment
 * @param credentialId      验证成功的 Secret 记录标识；verified Secret record identifier
 * @param issuedAt          签发时间；issued-at instant
 * @param expiresAt         过期时间；expiration instant
 * @param appId             稳定业务应用身份；stable business application identity
 * @param scopeContext      SERVICE 授权上下文；SERVICE authorization context
 */
public record ServiceIdentityPrincipal(
        String subject,
        String tenantId,
        String clientId,
        String tokenId,
        URI resourceUri,
        long resourceVersion,
        Set<String> scopes,
        String sourceBizCode,
        String sourceAppCode,
        String sourceEnvironment,
        String credentialId,
        Instant issuedAt,
        Instant expiresAt,
        String appId,
        ServiceTokenContext scopeContext
) implements IdpPrincipal {

    /**
     * 校验并规范化服务身份。
     *
     * <p>Validates and normalizes the service identity.</p>
     */
    public ServiceIdentityPrincipal {
        subject = required(subject, "subject");
        tenantId = optional(tenantId, "tenantId");
        clientId = required(clientId, "clientId");
        if (!subject.equals(clientId)) {
            throw new IllegalArgumentException(
                    "service subject must equal clientId"
            );
        }
        appId = required(appId, "appId");
        scopeContext = Objects.requireNonNull(scopeContext, "scopeContext");
        if (scopeContext == ServiceTokenContext.TENANT
                && tenantId == null) {
            throw new IllegalArgumentException(
                    "TENANT service context requires tenantId"
            );
        }
        if (scopeContext == ServiceTokenContext.PLATFORM
                && tenantId != null) {
            throw new IllegalArgumentException(
                    "PLATFORM service context must not contain tenantId"
            );
        }
        tokenId = required(tokenId, "tokenId");
        resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
        if (!resourceUri.isAbsolute() || resourceUri.getFragment() != null) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        if (resourceVersion < 0L) {
            throw new IllegalArgumentException(
                    "resourceVersion must not be negative"
            );
        }
        scopes = scopes(scopes);
        sourceBizCode = required(sourceBizCode, "sourceBizCode");
        sourceAppCode = required(sourceAppCode, "sourceAppCode");
        sourceEnvironment = required(
                sourceEnvironment,
                "sourceEnvironment"
        );
        credentialId = required(credentialId, "credentialId");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }
    }

    /**
     * 保留旧构造签名，供 USER/Resource Server 迁移期间的测试与消费者编译。
     *
     * <p>Retains the former constructor while USER and Resource Server consumers migrate.</p>
     */
    public ServiceIdentityPrincipal(
            String subject,
            String tenantId,
            String clientId,
            String tokenId,
            URI resourceUri,
            long resourceVersion,
            Set<String> scopes,
            String sourceBizCode,
            String sourceAppCode,
            String sourceEnvironment,
            String credentialId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        this(
                subject,
                tenantId,
                clientId,
                tokenId,
                resourceUri,
                resourceVersion,
                scopes,
                sourceBizCode,
                sourceAppCode,
                sourceEnvironment,
                credentialId,
                issuedAt,
                expiresAt,
                clientId,
                tenantId == null
                        ? ServiceTokenContext.PLATFORM
                        : ServiceTokenContext.TENANT
        );
    }

    /**
     * 返回 SERVICE 主体类型。
     *
     * <p>Returns the SERVICE principal type.</p>
     *
     * @return SERVICE
     */
    @Override
    public PrincipalType principalType() {
        return PrincipalType.SERVICE;
    }

    /**
     * 规范化 Scope 集合。
     *
     * <p>Normalizes the scope set.</p>
     *
     * @param values 原始 Scope；raw scopes
     * @return 已排序不可变 Scope；sorted immutable scopes
     */
    private static Set<String> scopes(Set<String> values) {
        Objects.requireNonNull(values, "scopes");
        TreeSet<String> normalized = new TreeSet<>();
        for (String value : values) {
            normalized.add(required(value, "scope"));
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("scopes is required");
        }
        return Collections.unmodifiableSet(normalized);
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String optional(String value, String field) {
        if (value == null) {
            return null;
        }
        return required(value, field);
    }
}
