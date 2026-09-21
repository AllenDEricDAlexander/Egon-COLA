package top.egon.cola.archetype.source.service.application.exam.pojo.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PublishExamCommand(@NotNull @Positive Long examId) {
}
