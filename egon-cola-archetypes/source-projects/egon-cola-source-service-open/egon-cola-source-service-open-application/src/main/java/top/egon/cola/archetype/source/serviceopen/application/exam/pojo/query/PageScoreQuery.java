package top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PageScoreQuery(
        @NotNull @Positive Long examId,
        @Min(1) int currentPage,
        @Min(1) @Max(200) int pageSize) {
}
