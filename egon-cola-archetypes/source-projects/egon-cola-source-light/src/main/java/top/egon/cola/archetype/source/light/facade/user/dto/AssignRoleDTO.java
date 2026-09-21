package top.egon.cola.archetype.source.light.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record AssignRoleDTO(
        @NotNull(groups = NativeRpcValidationGroup.class) @Positive(groups = NativeRpcValidationGroup.class) Long userId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String roleCode,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements BasePojo {
}
