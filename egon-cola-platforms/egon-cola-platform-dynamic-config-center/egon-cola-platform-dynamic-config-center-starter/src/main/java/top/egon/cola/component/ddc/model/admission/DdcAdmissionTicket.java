package top.egon.cola.component.ddc.model.admission;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * IdP 签发并由 DDC 注册生产者携带的短期准入票据。
 *
 * <p>Short-lived admission ticket issued by IdP and carried by DDC registration producers.</p>
 *
 * <p>原始 JWT 是敏感凭证，{@link #toString()} 永不输出其值。</p>
 *
 * <p>The raw JWT is a sensitive credential and is never included by {@link #toString()}.</p>
 *
 * @param value 紧凑 Admission JWT；compact Admission JWT
 * @param expiresAt 票据过期时间；ticket expiration instant
 * @param resourceServerId Resource Server 标识；Resource Server identifier
 * @param resourceUri Resource URI；Resource URI
 * @param resourceVersion Resource 版本；Resource version
 * @param bizCode 业务域编码；business-domain code
 * @param appCode 应用编码；application code
 * @param environment 环境编码；environment code
 * @param instanceId DDC 实例标识；DDC instance identifier
 * @param credentialId 签发准入所用 Client JWK kid；Client JWK kid used for admission
 */
public record DdcAdmissionTicket(
        String value,
        Instant expiresAt,
        String resourceServerId,
        URI resourceUri,
        long resourceVersion,
        String bizCode,
        String appCode,
        String environment,
        String instanceId,
        String credentialId
) {

    /**
     * 校验票据审计信息并保留原始 JWT 的机密性。
     *
     * <p>Validates ticket audit data while preserving confidentiality of the raw JWT.</p>
     */
    public DdcAdmissionTicket {
        value = required(value, "value");
        if (value.length() > 16_384 || value.chars().anyMatch(
                Character::isWhitespace)) {
            throw new IllegalArgumentException("value is invalid");
        }
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        DdcAdmissionRequest identity = new DdcAdmissionRequest(
                resourceServerId,
                resourceUri,
                bizCode,
                appCode,
                environment,
                instanceId
        );
        resourceServerId = identity.resourceServerId();
        resourceUri = identity.resourceUri();
        bizCode = identity.bizCode();
        appCode = identity.appCode();
        environment = identity.environment();
        instanceId = identity.instanceId();
        if (resourceVersion < 0L) {
            throw new IllegalArgumentException(
                    "resourceVersion must not be negative"
            );
        }
        credentialId = required(credentialId, "credentialId");
    }

    /**
     * 判断票据是否绑定到精确的 Resource 实例身份。
     *
     * <p>Determines whether this ticket is bound to the exact Resource instance identity.</p>
     *
     * @param request DDC 准入请求；DDC admission request
     * @return 所有身份字段精确相等时为 {@code true}；{@code true} when all identity fields match
     */
    public boolean matches(DdcAdmissionRequest request) {
        Objects.requireNonNull(request, "request");
        return resourceServerId.equals(request.resourceServerId())
                && resourceUri.equals(request.resourceUri())
                && bizCode.equals(request.bizCode())
                && appCode.equals(request.appCode())
                && environment.equals(request.environment())
                && instanceId.equals(request.instanceId());
    }

    /**
     * 返回不包含原始 JWT 的安全诊断文本。
     *
     * <p>Returns safe diagnostic text that excludes the raw JWT.</p>
     *
     * @return 脱敏后的票据摘要；redacted ticket summary
     */
    @Override
    public String toString() {
        return "DdcAdmissionTicket["
                + "value=<redacted>, expiresAt=" + expiresAt
                + ", resourceServerId=" + resourceServerId
                + ", resourceUri=" + resourceUri
                + ", resourceVersion=" + resourceVersion
                + ", bizCode=" + bizCode
                + ", appCode=" + appCode
                + ", environment=" + environment
                + ", instanceId=" + instanceId
                + ", credentialId=" + credentialId
                + ']';
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
}
