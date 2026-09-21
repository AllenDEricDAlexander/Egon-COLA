package top.egon.cola.archetype.source.service.application.exam.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttachExamPaperCommand(
        @NotNull @Positive Long examId,
        @NotBlank String title,
        @Positive int totalPoints) {
}
