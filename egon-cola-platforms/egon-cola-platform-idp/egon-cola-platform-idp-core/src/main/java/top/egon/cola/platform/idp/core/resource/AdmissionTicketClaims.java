package top.egon.cola.platform.idp.core.resource;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * IdP 准入服务授权后可写入独立 Admission JWT 的可信声明。
 *
 * <p>Trusted claims authorized by the IdP admission service for an independent Admission JWT.</p>
 *
 * @param resourceServerId Resource Server 标识和 JWT Subject；Resource Server identifier and JWT
 * subject
 * @param resourceUri Resource URI；Resource URI
 * @param resourceVersion Resource 版本；Resource version
 * @param bizCode 业务域编码；business-domain code
 * @param appCode 应用编码；application code
 * @param environment 环境编码；environment code
 * @param instanceId DDC 运行实例标识；DDC runtime instance identifier
 * @param credentialId 验证 Client Assertion 的 JWK kid；JWK kid that verified the Client Assertion
 * @param tokenId Admission JWT 的 jti；Admission JWT jti
 * @param issuedAt 签发时间；issuance instant
 * @param notBefore 生效时间；not-before instant
 * @param expiresAt 过期时间；expiration instant
 */
public record AdmissionTicketClaims(
        String resourceServerId,
        URI resourceUri,
        long resourceVersion,
        String bizCode,
        String appCode,
        String environment,
        String instanceId,
        String credentialId,
        String tokenId,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt
) {

    /**
     * 校验独立用途 Admission Ticket 的可信声明。
     *
     * <p>Validates trusted claims for the independently scoped Admission Ticket.</p>
     */
    public AdmissionTicketClaims {
        AdmissionRequest identity = new AdmissionRequest(
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
        tokenId = required(tokenId, "tokenId");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        notBefore = Objects.requireNonNull(notBefore, "notBefore");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (notBefore.isBefore(issuedAt) || !expiresAt.isAfter(notBefore)) {
            throw new IllegalArgumentException(
                    "invalid admission ticket time range"
            );
        }
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
