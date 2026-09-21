package top.egon.cola.archetype.source.lightopen.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateCourseDTO(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String operatorId,
        @NotBlank String requestId) implements BasePojo {
}
