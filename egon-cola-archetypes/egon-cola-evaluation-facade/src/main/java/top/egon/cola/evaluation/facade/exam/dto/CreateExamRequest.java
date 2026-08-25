package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;

public record CreateExamRequest(
        @NotNull @Positive Long courseId,
        @NotBlank String title,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) implements Serializable {
}
