package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record RecordScoreRequest(
        @NotNull @Positive Long examId,
        @NotNull @Positive Long studentId,
        @Min(0) @Max(100) int points) implements Serializable {
}
