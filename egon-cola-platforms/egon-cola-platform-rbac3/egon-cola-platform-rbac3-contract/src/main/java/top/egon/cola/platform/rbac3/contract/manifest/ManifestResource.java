package top.egon.cola.platform.rbac3.contract.manifest;

import java.util.Map;
import java.util.Objects;

public record ManifestResource(
        String code,
        String parentCode,
        String name,
        Integer order,
        String path,
        String componentKey,
        String requiredPermissionCode,
        String redirect,
        Boolean hidden,
        Boolean keepAlive,
        String routeCode,
        String gatewayOperationId,
        String httpMethod,
        String pathPattern,
        Boolean externalAccessible,
        Map<String, String> metadata
) {

    public ManifestResource {
        code = required(code, "code");
        parentCode = optional(parentCode, "parentCode");
        name = optional(name, "name");
        path = optional(path, "path");
        componentKey = optional(componentKey, "componentKey");
        requiredPermissionCode = optional(
                requiredPermissionCode,
                "requiredPermissionCode"
        );
        redirect = optional(redirect, "redirect");
        routeCode = optional(routeCode, "routeCode");
        gatewayOperationId = optional(
                gatewayOperationId,
                "gatewayOperationId"
        );
        httpMethod = optional(httpMethod, "httpMethod");
        pathPattern = optional(pathPattern, "pathPattern");
        metadata = Map.copyOf(Objects.requireNonNull(
                metadata,
                "metadata"
        ));
        metadata.forEach((key, value) -> {
            required(key, "metadata key");
            Objects.requireNonNull(value, "metadata value");
        });
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }
}
