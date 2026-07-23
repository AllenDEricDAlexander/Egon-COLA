package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record RecordScoreRequest(
        @NotBlank String examId,
        @NotBlank String studentId,
        @Min(0) @Max(100) int points) implements Serializable {
}
