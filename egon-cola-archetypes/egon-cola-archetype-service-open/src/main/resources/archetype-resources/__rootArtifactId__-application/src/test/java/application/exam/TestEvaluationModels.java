#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
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
