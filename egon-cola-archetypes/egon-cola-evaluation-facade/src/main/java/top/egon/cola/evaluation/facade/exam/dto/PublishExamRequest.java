package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record PublishExamRequest(@NotNull @Positive Long examId) implements Serializable {
}
