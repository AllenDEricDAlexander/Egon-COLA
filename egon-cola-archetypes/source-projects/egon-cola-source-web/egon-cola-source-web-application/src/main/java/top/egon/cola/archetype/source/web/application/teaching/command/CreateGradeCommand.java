package top.egon.cola.archetype.source.web.application.teaching.command;

import jakarta.validation.constraints.NotBlank;

public record CreateGradeCommand(
        String requestId,
        @NotBlank String code,
        @NotBlank String name) {
}
