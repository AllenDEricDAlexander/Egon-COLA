package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record AttachExamPaperRequest(
        @NotBlank String examId,
        @NotBlank String title,
        @Positive int totalPoints) implements Serializable {
}
