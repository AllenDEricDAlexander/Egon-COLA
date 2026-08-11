package top.egon.cola.platform.idp.core.resource;

import java.net.URI;
import java.util.Objects;

/**
 * Resource Server 向 IdP 声明的精确启动准入身份。
 *
 * <p>Exact startup-admission identity declared by a Resource Server to IdP.</p>
 *
 * @param resourceServerId Resource Server 标识；Resource Server identifier
 * @param resourceUri Resource URI；Resource URI
 * @param bizCode 业务域编码；business-domain code
 * @param appCode 应用编码；application code
 * @param environment 环境编码；environment code
 * @param instanceId DDC 运行实例标识；DDC runtime instance identifier
 */
public record AdmissionRequest(
        String resourceServerId,
        URI resourceUri,
        String bizCode,
        String appCode,
        String environment,
        String instanceId
) {

    /**
     * 校验 Resource、三元组与实例声明，拒绝宽松或模糊身份。
     *
     * <p>Validates the Resource, triple, and instance declaration, rejecting widened or ambiguous
     * identities.</p>
     */
    public AdmissionRequest {
        resourceServerId = code(resourceServerId, "resourceServerId");
        resourceUri = resource(resourceUri);
        bizCode = code(bizCode, "bizCode");
        appCode = code(appCode, "appCode");
        environment = code(environment, "environment");
        instanceId = instance(instanceId);
    }

    /**
     * 校验 Resource 与三元组编码。
     *
     * <p>Validates a Resource or triple code.</p>
     *
     * @param value 原始值；raw value
     * @param field 字段名；field name
     * @return 已校验编码；validated code
     */
    private static String code(String value, String field) {
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.length() > 128
                || !value.matches("[A-Za-z0-9][A-Za-z0-9._~-]*")) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    /**
     * 校验绝对 Resource URI。
     *
     * <p>Validates an absolute Resource URI.</p>
     *
     * @param value Resource URI；Resource URI
     * @return 规范化 URI；normalized URI
     */
    private static URI resource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException(
                    "resourceUri must be an absolute normalized URI without a fragment"
            );
        }
        return value;
    }

    /**
     * 校验 DDC 实例标识。
     *
     * <p>Validates a DDC instance identifier.</p>
     *
     * @param value 实例标识；instance identifier
     * @return 已校验标识；validated identifier
     */
    private static String instance(String value) {
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.length() > 256
                || !value.matches("[A-Za-z0-9][A-Za-z0-9:._~/-]*")) {
            throw new IllegalArgumentException("instanceId is invalid");
        }
        return value;
    }
}
