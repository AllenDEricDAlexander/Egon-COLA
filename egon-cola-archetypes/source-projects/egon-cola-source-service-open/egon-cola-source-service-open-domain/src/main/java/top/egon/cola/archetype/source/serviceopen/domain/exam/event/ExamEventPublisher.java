package top.egon.cola.archetype.source.serviceopen.domain.exam.event;

import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;

public interface ExamEventPublisher {
    void examPublished(Exam exam, ExamPaper paper);
    void scoreRecorded(Score score);
}
