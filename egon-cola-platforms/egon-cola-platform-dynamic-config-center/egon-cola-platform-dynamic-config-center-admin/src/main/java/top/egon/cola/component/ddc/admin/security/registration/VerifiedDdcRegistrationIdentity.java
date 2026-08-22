package top.egon.cola.component.ddc.admin.security.registration;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * DDC 已验证的 PLATFORM SERVICE registration identity。
 *
 * <p>Immutable service identity verified by DDC before a lease mutation.</p>
 *
 * @param appId 稳定业务应用身份；stable business application identity
 * @param clientId Confidential Client 标识；confidential Client identifier
 * @param resourceServerId IdP Resource Server 稳定标识；stable IdP Resource Server identifier
 * @param resourceUri Resource Server 唯一 URI；unique Resource Server URI
 * @param resourceVersion IdP Resource 当前版本；current IdP Resource version
 * @param bizCode 业务域；business domain
 * @param appCode 应用编码；application code
 * @param environment 运行环境；runtime environment
 * @param instanceId 物理实例标识；physical instance identifier
 * @param credentialId 签发 Token 所用 Secret 凭据标识；verified Secret credential identifier
 * @param tokenId Access Token jti；access-token identifier
 * @param issuedAt Token 签发时间；token issuance time
 * @param expiresAt Token 到期时间；token expiration time
 * @param scopes Token scopes；authorized scopes
 */
public record VerifiedDdcRegistrationIdentity(
        String appId,
        String clientId,
        String resourceServerId,
        String resourceUri,
        long resourceVersion,
        String bizCode,
        String appCode,
        String environment,
        String instanceId,
        String credentialId,
        String tokenId,
        Instant issuedAt,
        Instant expiresAt,
        Set<String> scopes
) {

    /**
     * 校验所有审计字段完整且时间范围有效。
     *
     * <p>Validates that every trusted identity field is complete and the time range is valid.</p>
     */
    public VerifiedDdcRegistrationIdentity {
        appId = required(appId, "appId");
        clientId = required(clientId, "clientId");
        resourceServerId = required(resourceServerId, "resourceServerId");
        resourceUri = required(resourceUri, "resourceUri");
        if (resourceVersion < 0) {
            throw new IllegalArgumentException(
                    "resourceVersion must not be negative"
            );
        }
        bizCode = required(bizCode, "bizCode");
        appCode = required(appCode, "appCode");
        environment = required(environment, "environment");
        instanceId = required(instanceId, "instanceId");
        credentialId = required(credentialId, "credentialId");
        tokenId = required(tokenId, "tokenId");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        Objects.requireNonNull(scopes, "scopes");
        TreeSet<String> normalized = new TreeSet<>();
        for (String scope : scopes) {
            normalized.add(required(scope, "scope"));
        }
        if (normalized.isEmpty() || normalized.size() != scopes.size()) {
            throw new IllegalArgumentException("scopes are invalid");
        }
        scopes = Collections.unmodifiableSet(normalized);
    }

    /**
     * 校验必填字符串。
     *
     * <p>Validates a required string.</p>
     *
     * @param value 待校验值；value to validate
     * @param name 字段名；field name
     * @return 原始非空值；original non-blank value
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
