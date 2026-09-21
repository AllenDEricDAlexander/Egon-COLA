package top.egon.cola.archetype.source.webopen.application.teaching.pojo.command;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateSchoolClassCommand(
        String requestId,
        @NotBlank String name,
        @NotBlank String gradeCode) implements BasePojo {
}
