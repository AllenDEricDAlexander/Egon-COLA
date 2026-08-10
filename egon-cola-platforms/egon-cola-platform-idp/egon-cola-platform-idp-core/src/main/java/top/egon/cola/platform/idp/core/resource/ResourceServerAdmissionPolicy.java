package top.egon.cola.platform.idp.core.resource;

import top.egon.cola.platform.idp.core.oauth.OAuthClient;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * 集中校验 Resource Server 启动准入的领域策略。
 *
 * <p>Domain policy centralizing Resource Server startup-admission checks.</p>
 */
public final class ResourceServerAdmissionPolicy {

    /**
     * 校验 Resource、Management Client、公开凭证和实例声明。
     *
     * <p>Validates the Resource, Management Client, public credential, and instance declaration.</p>
     *
     * @param resource    已登记 Resource Server；registered Resource Server
     * @param client      发起准入的 Management Client；Management Client requesting admission
     * @param credential  已完成签名选择的公开凭证；public credential selected for signature verification
     * @param bizCode     实例声明业务域；business domain declared by the instance
     * @param appCode     实例声明应用；application declared by the instance
     * @param environment 实例声明环境；environment declared by the instance
     * @param instanceId  运行实例标识；runtime instance identifier
     * @param now         当前校验时间；current verification instant
     * @return 可用于签发 Ticket 的安全授权结果；safe authorization result used to issue a Ticket
     */
    public AdmissionAuthorization authorize(
            ResourceServer resource,
            OAuthClient client,
            ClientJwkCredential credential,
            String bizCode,
            String appCode,
            String environment,
            String instanceId,
            Instant now) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(credential, "credential");
        Objects.requireNonNull(now, "now");
        String safeInstanceId = instance(instanceId);
        if (!resource.active()) {
            deny(
                    "IDP_RESOURCE_SERVER_DISABLED",
                    "Resource Server is disabled"
            );
        }
        if (!resource.matches(bizCode, appCode, environment)) {
            deny(
                    mismatch(resource, bizCode, appCode, environment),
                    "Resource Server scope does not match"
            );
        }
        if (client.status() != OAuthClient.Status.ACTIVE
                || client.clientType() != OAuthClient.ClientType.CONFIDENTIAL
                || !resource.managementClientId().equals(client.clientId())) {
            deny(
                    "IDP_RESOURCE_SERVER_CLIENT_INVALID",
                    "Management Client is invalid"
            );
        }
        if (!credential.clientId().equals(client.clientId())
                || !credential.activeAt(now)) {
            deny(
                    "IDP_RESOURCE_SERVER_CREDENTIAL_INVALID",
                    "Resource Server credential is invalid"
            );
        }
        return new AdmissionAuthorization(
                resource.resourceServerId(),
                resource.resourceUri(),
                resource.bizCode(),
                resource.appCode(),
                resource.environment(),
                safeInstanceId,
                credential.keyId(),
                resource.version()
        );
    }

    /**
     * 识别三元组不匹配维度。
     *
     * <p>Identifies the mismatching triple dimension.</p>
     *
     * @param resource    已登记 Resource；registered Resource
     * @param bizCode     声明业务域；declared business domain
     * @param appCode     声明应用；declared application
     * @param environment 声明环境；declared environment
     * @return 维度专用错误码；dimension-specific error code
     */
    private String mismatch(
            ResourceServer resource,
            String bizCode,
            String appCode,
            String environment) {
        if (!resource.bizCode().equals(bizCode)) {
            return "IDP_RESOURCE_SERVER_BIZ_MISMATCH";
        }
        if (!resource.appCode().equals(appCode)) {
            return "IDP_RESOURCE_SERVER_APP_MISMATCH";
        }
        if (!resource.environment().equals(environment)) {
            return "IDP_RESOURCE_SERVER_ENV_MISMATCH";
        }
        return "IDP_RESOURCE_SERVER_SCOPE_MISMATCH";
    }

    /**
     * 校验 DDC 实例标识。
     *
     * <p>Validates a DDC instance identifier.</p>
     *
     * @param value 原始实例标识；raw instance identifier
     * @return 已校验实例标识；validated instance identifier
     */
    private String instance(String value) {
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.length() > 256
                || !value.matches("[A-Za-z0-9][A-Za-z0-9:._~/-]*")) {
            deny(
                    "IDP_RESOURCE_SERVER_INSTANCE_INVALID",
                    "Resource Server instance is invalid"
            );
        }
        return value;
    }

    /**
     * 抛出稳定 Resource 授权异常。
     *
     * <p>Throws a stable Resource authorization exception.</p>
     *
     * @param code    稳定错误码；stable error code
     * @param message 安全错误描述；safe error description
     */
    private void deny(String code, String message) {
        throw new ResourceAuthorizationException(code, message);
    }

    /**
     * 已通过准入策略且可安全写入 Admission Ticket 的声明。
     *
     * <p>Claims authorized by the admission policy and safe to place in an Admission Ticket.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param resourceUri      Resource URI；Resource URI
     * @param bizCode          业务域；business domain
     * @param appCode          应用；application
     * @param environment      环境；environment
     * @param instanceId       实例标识；instance identifier
     * @param credentialId     公钥 kid；public-key kid
     * @param resourceVersion  Resource 版本；Resource version
     */
    public record AdmissionAuthorization(
            String resourceServerId,
            URI resourceUri,
            String bizCode,
            String appCode,
            String environment,
            String instanceId,
            String credentialId,
            long resourceVersion
    ) {
    }
}
