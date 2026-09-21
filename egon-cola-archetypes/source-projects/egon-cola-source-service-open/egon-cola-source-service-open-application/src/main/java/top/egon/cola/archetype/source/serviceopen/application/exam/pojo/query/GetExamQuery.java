package top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetExamQuery(@NotNull @Positive Long examId) {
}
