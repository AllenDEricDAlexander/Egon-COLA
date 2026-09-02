package top.egon.cola.archetype.source.serviceopen.domain.exam.aggregates;

import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;

public record ExamAggregate(Exam exam, ExamPaper paper) {
}
