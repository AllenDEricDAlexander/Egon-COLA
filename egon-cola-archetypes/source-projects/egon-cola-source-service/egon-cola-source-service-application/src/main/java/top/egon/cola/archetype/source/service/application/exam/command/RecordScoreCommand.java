package top.egon.cola.archetype.source.service.application.exam.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordScoreCommand(
        @NotNull @Positive Long examId,
        @NotNull @Positive Long studentId,
        @Min(0) @Max(100) int points) {
}
