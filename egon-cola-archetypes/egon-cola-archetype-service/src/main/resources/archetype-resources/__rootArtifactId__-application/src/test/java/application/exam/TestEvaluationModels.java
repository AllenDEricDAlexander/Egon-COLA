#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.service.impl.ExamDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import java.time.Instant;

final class TestEvaluationModels {

    private static final ExamDomainServiceImpl SERVICE = new ExamDomainServiceImpl();
    private static final Course COURSE = Course.create(
            "course-1", new CourseCode("MATH-101"), "Math", 3);
    private static final Exam EXAM = SERVICE.createExam(
            "exam-1", COURSE, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
    private static final ExamPaper PAPER = SERVICE.attachPaper(
            "paper-1", EXAM, "Paper", 100);

    static {
        SERVICE.publishExam(EXAM, PAPER);
    }

    private TestEvaluationModels() {
    }

    static Exam publishedExam() { return EXAM; }
    static ExamPaper publishedPaper() { return PAPER; }
}
