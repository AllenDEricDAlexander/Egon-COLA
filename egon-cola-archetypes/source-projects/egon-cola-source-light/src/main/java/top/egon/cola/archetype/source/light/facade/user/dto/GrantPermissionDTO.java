package top.egon.cola.archetype.source.light.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.archetype.source.light.facade.rpc.NativeRpcValidationGroup;

import java.io.Serializable;

public record GrantPermissionDTO(
        @NotBlank(groups = NativeRpcValidationGroup.class) String roleCode,
        @NotBlank(groups = NativeRpcValidationGroup.class) String permissionCode,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements Serializable {
}
