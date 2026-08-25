package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record ExamPaperResponse(
        @NotNull @Positive Long id,
        @NotNull @Positive Long examId,
        String title,
        int totalPoints,
        String status) implements Serializable {
}
