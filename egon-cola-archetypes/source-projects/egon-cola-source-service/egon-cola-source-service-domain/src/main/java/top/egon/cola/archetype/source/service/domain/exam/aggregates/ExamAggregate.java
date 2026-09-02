package top.egon.cola.archetype.source.service.domain.exam.aggregates;

import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;

public record ExamAggregate(Exam exam, ExamPaper paper) {
}
