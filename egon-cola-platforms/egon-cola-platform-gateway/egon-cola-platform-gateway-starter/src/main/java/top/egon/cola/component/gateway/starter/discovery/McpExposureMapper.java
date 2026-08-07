package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class McpExposureMapper {

    static final String ATTRIBUTE_NAME = "mcpExposure";

    private static final Pattern PERMISSION = Pattern.compile(
            "^[a-z][a-z0-9._-]*(?::[A-Za-z0-9._*-]+)+$"
    );

    private McpExposureMapper() {
    }

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

    private static String required(
            String value,
            String field,
            String operationIdentity) {
        if (value == null || value.isBlank()) {
            invalid(operationIdentity, field + " is required");
        }
        return value.trim();
    }

    private static void invalid(String operationIdentity, String message) {
        throw new IllegalArgumentException(
                "invalid MCP exposure for " + operationIdentity + ": " + message
        );
    }
}
