package top.egon.cola.evaluation.facade.exam.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record GetExamRequest(@NotBlank String examId) implements Serializable {
}
