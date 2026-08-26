package top.egon.cola.component.gateway.starter.discovery.mcp;

import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates MCP exposure declarations and maps them to operation report
 * attributes.
 *
 * 校验 MCP 暴露声明，并将其转换为操作报告属性。
 */
public final class McpExposureMapper {

    /** Attribute key under which MCP exposure metadata is reported. 报告 MCP 暴露元数据所使用的属性键。 */
    public static final String ATTRIBUTE_NAME = "mcpExposure";

    /** Pattern accepted for MCP permission identifiers. MCP 权限标识符必须匹配的格式。 */
    private static final Pattern PERMISSION = Pattern.compile(
            "^[a-z][a-z0-9._-]*(?::[A-Za-z0-9._*-]+)+$"
    );

    /** Prevents instantiation of this utility class. 防止实例化此工具类。 */
    private McpExposureMapper() {
    }

    /**
     * Maps an operation's MCP exposure declaration to report attributes.
     *
     * 将操作的 MCP 暴露声明映射为报告属性。
     *
     * @param group             the declaring interface group annotation，声明接口分组的注解
     * @param operation         the operation annotation, or {@code null}，操作注解，可为 {@code null}
     * @param operationIdentity the operation identity used in error messages，错误消息中使用的操作标识
     * @param streaming         whether the operation has streaming semantics，操作是否具有流式语义
     * @return MCP exposure attributes, or an empty map when MCP registration
     *         is not requested，MCP 暴露属性；未请求注册时返回空映射
     * @throws IllegalArgumentException if the requested exposure is invalid or
     *                                  unsupported
     */
    public static Map<String, Object> map(
            GatewayInterfaceGroup group,
            GatewayOperation operation,
            String operationIdentity,
            boolean streaming) {
        if (operation == null || !operation.registerMcp()) {
            return Map.of();
        }
        String serverCode = required(
                group.mcpServerCode(),
                "mcpServerCode",
                operationIdentity
        );
        String name = required(
                operation.mcpName(),
                "mcpName",
                operationIdentity
        );
        if (streaming) {
            invalid(operationIdentity, "streaming operations are unsupported");
        }
        List<String> permissions = Arrays.stream(
                        operation.mcpRequiredPermissions()
                )
                .map(String::trim)
                .peek(permission -> {
                    if (!PERMISSION.matcher(permission).matches()) {
                        invalid(
                                operationIdentity,
                                "invalid MCP permission: " + permission
                        );
                    }
                })
                .distinct()
                .sorted()
                .toList();
        return Map.of(
                "registerMcp", true,
                "mcpServerCode", serverCode,
                "mcpName", name,
                "requiredPermissions", permissions,
                "riskLevel", operation.mcpRiskLevel().name(),
                "idempotent", operation.idempotent()
        );
    }

    /**
     * Requires and normalizes a non-blank MCP declaration value.
     *
     * 要求 MCP 声明值非空白，并对其进行规范化处理。
     *
     * @param value             the declared value，声明的值
     * @param field             the declaration field name，声明字段名称
     * @param operationIdentity the operation identity used in error messages，错误消息中使用的操作标识
     * @return the trimmed declaration value，去除首尾空白后的声明值
     * @throws IllegalArgumentException if the value is {@code null} or blank
     */
    private static String required(
            String value,
            String field,
            String operationIdentity) {
        if (value == null || value.isBlank()) {
            invalid(operationIdentity, field + " is required");
        }
        return value.trim();
    }

    /**
     * Throws an exception describing an invalid MCP exposure declaration.
     *
     * 抛出描述无效 MCP 暴露声明的异常。
     *
     * @param operationIdentity the invalid operation identity，无效的操作标识
     * @param message           the validation failure detail，校验失败详情
     * @throws IllegalArgumentException always
     */
    private static void invalid(String operationIdentity, String message) {
        throw new IllegalArgumentException(
                "invalid MCP exposure for " + operationIdentity + ": " + message
        );
    }
}
