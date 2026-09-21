package top.egon.cola.archetype.source.light.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateUserDTO(
        @NotBlank(groups = NativeRpcValidationGroup.class) String externalId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String name,
        @NotBlank(groups = NativeRpcValidationGroup.class) String email,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements BasePojo {
}
