package top.egon.cola.archetype.source.serviceopen.domain.exam.service;

import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;

public interface ExamEventService {
    void examPublished(Exam exam, ExamPaper paper);
    void scoreRecorded(Score score);
}
