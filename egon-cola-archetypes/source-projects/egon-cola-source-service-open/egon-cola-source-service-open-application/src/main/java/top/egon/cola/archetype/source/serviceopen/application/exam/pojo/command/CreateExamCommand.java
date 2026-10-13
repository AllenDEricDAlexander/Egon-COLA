package top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record CreateExamCommand(
        @NotNull @Positive Long courseId,
        @NotBlank String title,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {
    public CreateExamCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
    }
}
