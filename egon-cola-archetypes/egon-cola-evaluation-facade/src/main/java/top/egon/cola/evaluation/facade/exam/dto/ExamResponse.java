package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;

public record ExamResponse(
        @NotNull @Positive Long id,
        @NotNull @Positive Long courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) implements Serializable {
}
