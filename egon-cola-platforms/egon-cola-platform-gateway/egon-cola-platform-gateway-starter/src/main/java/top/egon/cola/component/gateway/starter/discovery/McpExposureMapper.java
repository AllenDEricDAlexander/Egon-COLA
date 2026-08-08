package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates MCP exposure declarations and maps them to operation report
 * attributes.
 */
final class McpExposureMapper {

    /** Attribute key under which MCP exposure metadata is reported. */
    static final String ATTRIBUTE_NAME = "mcpExposure";

    /** Pattern accepted for MCP permission identifiers. */
    private static final Pattern PERMISSION = Pattern.compile(
            "^[a-z][a-z0-9._-]*(?::[A-Za-z0-9._*-]+)+$"
    );

    /** Prevents instantiation of this utility class. */
    private McpExposureMapper() {
    }

    /**
     * Maps an operation's MCP exposure declaration to report attributes.
     *
     * @param group             the declaring interface group annotation
     * @param operation         the operation annotation, or {@code null}
     * @param operationIdentity the operation identity used in error messages
     * @param streaming         whether the operation has streaming semantics
     * @param parameters        the request parameters to validate for MCP use
     * @return MCP exposure attributes, or an empty map when MCP registration
     *         is not requested
     * @throws IllegalArgumentException if the requested exposure is invalid or
     *                                  unsupported
     */
    static Map<String, Object> map(
            GatewayInterfaceGroup group,
            GatewayOperation operation,
            String operationIdentity,
            boolean streaming,
            List<GatewayRequestSchemaValidator.RequestParameter> parameters) {
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
        parameters.forEach(parameter -> validateParameter(
                operationIdentity,
                parameter
        ));
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
     * Validates whether a request parameter can be supplied through MCP.
     *
     * @param operationIdentity the operation identity used in error messages
     * @param parameter         the request parameter to validate
     * @throws IllegalArgumentException if the parameter requires unsupported
     *                                  multipart, header, or cookie binding
     */
    private static void validateParameter(
            String operationIdentity,
            GatewayRequestSchemaValidator.RequestParameter parameter) {
        if (parameter.location() == GatewayRequestLocation.PART) {
            invalid(operationIdentity, "multipart operations are unsupported");
        }
        if (!parameter.required()) {
            return;
        }
        if (parameter.location() == GatewayRequestLocation.HEADER
                && "Authorization".equalsIgnoreCase(parameter.name())) {
            return;
        }
        if (parameter.location() == GatewayRequestLocation.HEADER
                || parameter.location() == GatewayRequestLocation.COOKIE) {
            invalid(
                    operationIdentity,
                    "required " + parameter.location()
                            + " parameter is unsupported: "
                            + parameter.name()
            );
        }
    }

    /**
     * Requires and normalizes a non-blank MCP declaration value.
     *
     * @param value             the declared value
     * @param field             the declaration field name
     * @param operationIdentity the operation identity used in error messages
     * @return the trimmed declaration value
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
     * @param operationIdentity the invalid operation identity
     * @param message           the validation failure detail
     * @throws IllegalArgumentException always
     */
    private static void invalid(String operationIdentity, String message) {
        throw new IllegalArgumentException(
                "invalid MCP exposure for " + operationIdentity + ": " + message
        );
    }
}
