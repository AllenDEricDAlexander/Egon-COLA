package top.egon.cola.component.gateway.admin.openapi.domain.dto;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable application/build/OpenAPI Group key used by synchronization CAS.
 *
 * <p>中文：同步状态唯一键，明确按应用、构建和 OpenAPI Group 隔离。
 */
public record GatewayOpenApiSyncKeyDTO(
        String applicationId,
        String buildId,
        String openapiGroup
) {

    private static final Pattern GROUP = Pattern.compile(
            "^[a-z][a-z0-9-]{0,63}$"
    );

    public GatewayOpenApiSyncKeyDTO {
        applicationId = required(applicationId, "applicationId", 64);
        buildId = required(buildId, "buildId", 256);
        openapiGroup = required(openapiGroup, "openapiGroup", 128);
        if (!GROUP.matcher(openapiGroup).matches()) {
            throw new IllegalArgumentException(
                    "openapiGroup must start with a lowercase letter and "
                            + "contain only lowercase letters, digits or hyphens"
            );
        }
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " exceeds " + max + " characters"
            );
        }
        return normalized;
    }
}
