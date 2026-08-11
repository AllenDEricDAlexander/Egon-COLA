package top.egon.cola.component.ddc.model.management;

/**
 * Resource Server 停用后撤销 DDC 准入租约的精确三元组命令。
 * / Exact-triple command that revokes DDC admission leases after a Resource Server is disabled.
 *
 * @param resourceServerId Resource Server 稳定标识 / stable Resource Server identifier
 * @param bizCode 业务域编码 / business-domain code
 * @param appCode 应用编码 / application code
 * @param env 部署环境 / deployment environment
 * @param resourceVersion 触发停用的 Resource 版本 / Resource version that triggered the disable event
 */
public record DdcResourceAdmissionRevocationRequest(
        String resourceServerId,
        String bizCode,
        String appCode,
        String env,
        long resourceVersion
) {

    /**
     * 校验撤销命令的稳定标识、精确三元组和非负版本。
     * / Validates the stable identifier, exact triple, and non-negative version.
     */
    public DdcResourceAdmissionRevocationRequest {
        resourceServerId = required(resourceServerId, "resourceServerId");
        bizCode = required(bizCode, "bizCode");
        appCode = required(appCode, "appCode");
        env = required(env, "env");
        if (resourceVersion < 0L) {
            throw new IllegalArgumentException(
                    "resourceVersion must not be negative"
            );
        }
    }

    /**
     * 规范化必填文本。
     * / Normalizes required text.
     *
     * @param value 原始值 / raw value
     * @param field 字段名 / field name
     * @return 去除首尾空白的值 / trimmed value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
