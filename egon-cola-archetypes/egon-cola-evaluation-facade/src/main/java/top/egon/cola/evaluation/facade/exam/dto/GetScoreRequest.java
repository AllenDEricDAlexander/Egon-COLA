package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record GetScoreRequest(
        @NotBlank String examId,
        @NotBlank String scoreId) implements Serializable {
}
