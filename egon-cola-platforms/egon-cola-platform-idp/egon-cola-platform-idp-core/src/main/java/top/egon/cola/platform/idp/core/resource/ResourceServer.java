package top.egon.cola.platform.idp.core.resource;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 以业务域、应用和环境三元组定义的逻辑 Resource Server。
 *
 * <p>Logical Resource Server defined by the business-domain, application, and environment
 * triple.</p>
 *
 * @param resourceServerId      稳定内部标识；stable internal identifier
 * @param resourceUri           RFC 8707 Resource Identifier；RFC 8707 Resource Identifier
 * @param bizCode               业务域编码；business-domain code
 * @param appCode               应用编码；application code
 * @param environment           运行环境；runtime environment
 * @param managementClientId    管理与机器身份 Client；management and machine-identity Client
 * @param rbacApplicationCode   USER 权限所属 RBAC3 应用；RBAC3 application owning USER permissions
 * @param entryPermissionCode   USER 进入应用所需权限；permission required for USER application entry
 * @param admissionTicketTtl    准入票据有效期；admission-ticket lifetime
 * @param status                Resource Server 状态；Resource Server status
 * @param version               乐观锁和运行态投影版本；optimistic-lock and runtime-projection version
 */
public record ResourceServer(
        String resourceServerId,
        URI resourceUri,
        String bizCode,
        String appCode,
        String environment,
        String managementClientId,
        String rbacApplicationCode,
        String entryPermissionCode,
        Duration admissionTicketTtl,
        ResourceServerStatus status,
        long version
) {

    /**
     * Resource 编码允许的安全字符。
     *
     * <p>Safe characters accepted in Resource codes.</p>
     */
    private static final Pattern CODE = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._~-]{0,127}"
    );

    /**
     * 校验并规范化 Resource Server。
     *
     * <p>Validates and normalizes the Resource Server.</p>
     */
    public ResourceServer {
        resourceServerId = code(resourceServerId, "resourceServerId");
        resourceUri = resourceUri(resourceUri);
        bizCode = code(bizCode, "bizCode");
        appCode = code(appCode, "appCode");
        environment = code(environment, "environment");
        managementClientId = code(managementClientId, "managementClientId");
        rbacApplicationCode = code(
                rbacApplicationCode,
                "rbacApplicationCode"
        );
        entryPermissionCode = permission(
                entryPermissionCode,
                "entryPermissionCode"
        );
        admissionTicketTtl = Objects.requireNonNull(
                admissionTicketTtl,
                "admissionTicketTtl"
        );
        if (admissionTicketTtl.compareTo(Duration.ofSeconds(30)) < 0
                || admissionTicketTtl.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalArgumentException(
                    "admissionTicketTtl must be between 30 seconds and 15 minutes"
            );
        }
        status = Objects.requireNonNull(status, "status");
        if (version < 0L) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    /**
     * 判断当前 Resource 是否精确匹配业务域、应用和环境。
     *
     * <p>Determines whether this Resource exactly matches the business domain, application, and
     * environment.</p>
     *
     * @param candidateBizCode     候选业务域；candidate business-domain code
     * @param candidateAppCode     候选应用；candidate application code
     * @param candidateEnvironment 候选环境；candidate environment
     * @return 三个维度全部相等时为 {@code true}；{@code true} when all three dimensions match
     */
    public boolean matches(
            String candidateBizCode,
            String candidateAppCode,
            String candidateEnvironment) {
        return bizCode.equals(candidateBizCode)
                && appCode.equals(candidateAppCode)
                && environment.equals(candidateEnvironment);
    }

    /**
     * 判断 Resource Server 是否处于可用状态。
     *
     * <p>Determines whether the Resource Server is active.</p>
     *
     * @return ACTIVE 时为 {@code true}；{@code true} when ACTIVE
     */
    public boolean active() {
        return status == ResourceServerStatus.ACTIVE;
    }

    /**
     * 校验 RFC 8707 Resource URI。
     *
     * <p>Validates an RFC 8707 Resource URI.</p>
     *
     * @param value Resource URI；Resource URI
     * @return 已校验 URI；validated URI
     */
    private static URI resourceUri(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute()
                || value.getFragment() != null
                || value.getScheme() == null
                || value.getScheme().isBlank()) {
            throw new IllegalArgumentException(
                    "resourceUri must be an absolute URI without a fragment"
            );
        }
        return value.normalize();
    }

    /**
     * 校验稳定编码。
     *
     * <p>Validates a stable code.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验编码；validated code
     */
    private static String code(String value, String field) {
        String normalized = required(value, field);
        if (!CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }

    /**
     * 校验权限编码。
     *
     * <p>Validates a permission code.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验权限编码；validated permission code
     */
    private static String permission(String value, String field) {
        String normalized = required(value, field);
        if (normalized.length() > 256
                || !normalized.matches("[A-Za-z0-9][A-Za-z0-9:._~-]*")) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 去除首尾空白后的值；trimmed value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
