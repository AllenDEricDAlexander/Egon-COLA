package top.egon.cola.component.ddc.model.admission;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * DDC 注册生产者申请准入票据时使用的精确 Resource 实例身份。
 *
 * <p>Exact Resource instance identity used by a DDC registration producer when acquiring an
 * admission ticket.</p>
 *
 * @param resourceServerId Resource Server 标识；Resource Server identifier
 * @param resourceUri Resource URI；Resource URI
 * @param bizCode 业务域编码；business-domain code
 * @param appCode 应用编码；application code
 * @param environment 环境编码；environment code
 * @param instanceId DDC 运行实例标识；DDC runtime instance identifier
 */
public record DdcAdmissionRequest(
        String resourceServerId,
        URI resourceUri,
        String bizCode,
        String appCode,
        String environment,
        String instanceId
) {

    /** 稳定编码格式；Stable code format. */
    private static final Pattern CODE = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._~-]{0,127}"
    );

    /**
     * 校验不可伪造或模糊匹配的 Resource 实例身份。
     *
     * <p>Validates a Resource instance identity that must not be widened or ambiguously
     * matched.</p>
     */
    public DdcAdmissionRequest {
        resourceServerId = code(resourceServerId, "resourceServerId");
        resourceUri = resource(resourceUri);
        bizCode = code(bizCode, "bizCode");
        appCode = code(appCode, "appCode");
        environment = code(environment, "environment");
        instanceId = instance(instanceId);
    }

    /**
     * 校验稳定 Resource 编码。
     *
     * <p>Validates a stable Resource code.</p>
     *
     * @param value 原始值；raw value
     * @param field 字段名；field name
     * @return 已校验编码；validated code
     */
    private static String code(String value, String field) {
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || !CODE.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    /**
     * 校验绝对且无 Fragment 的 Resource URI。
     *
     * <p>Validates an absolute Resource URI without a fragment.</p>
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
