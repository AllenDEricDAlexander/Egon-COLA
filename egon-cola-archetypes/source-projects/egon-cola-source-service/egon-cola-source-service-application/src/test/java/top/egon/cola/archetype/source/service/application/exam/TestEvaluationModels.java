package top.egon.cola.archetype.source.service.application.exam;

import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.entities.Score;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamStatus;
import top.egon.cola.archetype.source.service.domain.exam.enums.ScoreStatus;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.service.domain.exam.vos.ScoreValue;
import java.time.Instant;

final class TestEvaluationModels {

    private static final Exam EXAM = new Exam(
            new ExamId(4001L), new CourseId(1001L), "Midterm",
            Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.PUBLISHED);
    private static final ExamPaper PAPER = new ExamPaper(
            5001L, EXAM.getId(), "Paper", 100, ExamPaperStatus.PUBLISHED);

    private TestEvaluationModels() {
    }

    static Exam publishedExam() { return EXAM; }
    static ExamPaper publishedPaper() { return PAPER; }
    static Score recordedScore() {
        return new Score(7001L, EXAM.getId(), new CourseId(1001L), 6001L,
                new ScoreValue(90), ScoreStatus.RECORDED);
    }
}
