package top.egon.cola.archetype.source.web.application.teaching.command;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassCommand(
        String requestId,
        @NotBlank String name,
        @NotBlank String gradeCode) {
}
