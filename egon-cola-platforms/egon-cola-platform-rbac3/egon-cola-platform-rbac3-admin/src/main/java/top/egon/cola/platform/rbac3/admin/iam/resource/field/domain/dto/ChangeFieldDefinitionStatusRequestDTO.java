package top.egon.cola.platform.rbac3.admin.iam.resource.field.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Optimistic status transition for a global field definition. */
public record ChangeFieldDefinitionStatusRequestDTO(
        @NotBlank String status,
        @PositiveOrZero long expectedVersion) {
}
