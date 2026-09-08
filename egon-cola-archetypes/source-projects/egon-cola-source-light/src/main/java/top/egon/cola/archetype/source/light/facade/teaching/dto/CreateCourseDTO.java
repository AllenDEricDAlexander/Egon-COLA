package top.egon.cola.archetype.source.light.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.archetype.source.light.facade.rpc.NativeRpcValidationGroup;

import java.io.Serializable;

public record CreateCourseDTO(
        @NotBlank(groups = NativeRpcValidationGroup.class) String code,
        @NotBlank(groups = NativeRpcValidationGroup.class) String name,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements Serializable {
}
