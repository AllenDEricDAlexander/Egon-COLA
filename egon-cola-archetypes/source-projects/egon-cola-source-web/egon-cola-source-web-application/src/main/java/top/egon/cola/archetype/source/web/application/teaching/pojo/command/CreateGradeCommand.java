package top.egon.cola.archetype.source.web.application.teaching.pojo.command;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateGradeCommand(
        String requestId,
        @NotBlank String code,
        @NotBlank String name) implements BasePojo {
}
