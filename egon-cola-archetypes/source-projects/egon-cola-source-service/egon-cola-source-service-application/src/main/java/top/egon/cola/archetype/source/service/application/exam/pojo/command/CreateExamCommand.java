package top.egon.cola.archetype.source.service.application.exam.pojo.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record CreateExamCommand(
        @NotNull @Positive Long courseId,
        @NotBlank String title,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {
}
