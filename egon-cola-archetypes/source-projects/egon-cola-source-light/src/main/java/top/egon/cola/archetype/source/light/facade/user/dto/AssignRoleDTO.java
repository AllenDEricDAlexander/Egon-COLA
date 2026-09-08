package top.egon.cola.archetype.source.light.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.archetype.source.light.facade.rpc.NativeRpcValidationGroup;

import java.io.Serializable;

public record AssignRoleDTO(
        @NotNull(groups = NativeRpcValidationGroup.class) @Positive(groups = NativeRpcValidationGroup.class) Long userId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String roleCode,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements Serializable {
}
