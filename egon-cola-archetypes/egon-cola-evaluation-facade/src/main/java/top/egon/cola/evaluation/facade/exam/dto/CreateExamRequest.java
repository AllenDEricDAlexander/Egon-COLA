package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

public record CreateExamRequest(
        @NotBlank String courseId,
        @NotBlank String title,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) implements Serializable {
}
