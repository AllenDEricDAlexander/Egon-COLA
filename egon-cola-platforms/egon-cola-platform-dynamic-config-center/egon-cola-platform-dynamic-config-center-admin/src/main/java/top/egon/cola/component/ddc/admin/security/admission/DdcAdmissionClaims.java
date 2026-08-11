package top.egon.cola.component.ddc.admin.security.admission;

import java.time.Instant;
import java.util.Objects;

/**
 * DDC 已验证的 Resource Server 准入审计声明。
 *
 * <p>Resource Server admission audit claims verified by DDC.</p>
 *
 * @param resourceServerId IdP Resource Server 稳定标识；stable IdP Resource Server identifier
 * @param resourceUri Resource Server 唯一 URI；unique Resource Server URI
 * @param resourceVersion IdP Resource 当前版本；current IdP Resource version
 * @param bizCode 业务域；business domain
 * @param appCode 应用编码；application code
 * @param environment 运行环境；runtime environment
 * @param instanceId 物理实例标识；physical instance identifier
 * @param credentialId 签发准入所用公钥凭据标识；public credential identifier used for admission
 * @param issuedAt 票据签发时间；ticket issuance time
 * @param expiresAt 票据到期时间；ticket expiration time
 */
public record DdcAdmissionClaims(
        String resourceServerId,
        String resourceUri,
        long resourceVersion,
        String bizCode,
        String appCode,
        String environment,
        String instanceId,
        String credentialId,
        Instant issuedAt,
        Instant expiresAt
) {

    /**
     * 校验所有审计字段完整且时间范围有效。
     *
     * <p>Validates that every audit field is complete and the time range is valid.</p>
     */
    public DdcAdmissionClaims {
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
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
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
