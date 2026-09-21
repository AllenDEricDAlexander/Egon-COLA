package top.egon.cola.archetype.source.service.application.exam.pojo.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetScoreQuery(
        @NotNull @Positive Long examId,
        @NotNull @Positive Long scoreId) {
}
