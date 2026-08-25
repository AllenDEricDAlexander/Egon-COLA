package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record PageScoreRequest(
        @NotNull @Positive Long examId,
        @Min(1) int currentPage,
        @Min(1) @Max(200) int pageSize) implements Serializable {
}
