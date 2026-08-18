package top.egon.cola.platform.rbac3.admin.iam.resource.field.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Manual field-definition creation command; CI reports cannot set security attributes. */
public record CreateFieldDefinitionRequestDTO(
        @NotBlank String applicationId,
        @NotBlank String resourceId,
        @NotBlank String fieldCode,
        @NotBlank String jsonPath,
        @NotBlank String dataType,
        @NotBlank String sensitivity,
        @NotBlank String defaultAccess,
        String maskingStrategy,
        @NotNull Boolean writable,
        @NotNull Boolean exportable) {
}
