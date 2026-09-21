package top.egon.cola.archetype.source.web.adapter.teaching.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateGradeRequest(@NotBlank String code, @NotBlank String name) implements BasePojo {
}
