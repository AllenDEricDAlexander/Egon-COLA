package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record ScoreResponse(
        @NotNull @Positive Long id,
        @NotNull @Positive Long examId,
        @NotNull @Positive Long courseId,
        @NotNull @Positive Long studentId,
        int points,
        String status) implements Serializable {
}
