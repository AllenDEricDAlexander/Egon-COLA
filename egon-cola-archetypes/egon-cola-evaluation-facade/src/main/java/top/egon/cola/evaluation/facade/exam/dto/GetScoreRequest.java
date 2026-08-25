package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record GetScoreRequest(
        @NotNull @Positive Long examId,
        @NotNull @Positive Long scoreId) implements Serializable {
}
