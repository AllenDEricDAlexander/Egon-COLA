package top.egon.cola.archetype.source.lightopen.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record AssignRoleDTO(
        @NotNull @Positive Long userId,
        @NotBlank String roleCode,
        @NotBlank String operatorId,
        @NotBlank String requestId) implements BasePojo {
}
