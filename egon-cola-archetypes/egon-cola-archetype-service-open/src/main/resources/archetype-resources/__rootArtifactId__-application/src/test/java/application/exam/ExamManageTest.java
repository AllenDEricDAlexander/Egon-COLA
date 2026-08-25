#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

import ${package}.application.exam.command.AttachExamPaperCommand;
import ${package}.application.exam.command.CreateExamCommand;
import ${package}.application.exam.converter.ExamApplicationConverter;
import ${package}.application.exam.manage.impl.ExamManageImpl;
import ${package}.application.exam.validators.ExamApplicationValidator;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.service.CourseDomainService;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.event.ExamEventPublisher;
import ${package}.domain.exam.service.ExamDomainService;
import ${package}.domain.exam.vos.ExamId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExamManageTest {

    @Test
    void shouldCreateExamForExistingCourse() {
        CourseDomainService courses = mock(CourseDomainService.class);
        ExamDomainService exams = mock(ExamDomainService.class);
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        Exam exam = new Exam(new ExamId(4001L), new ${package}.domain.course.vos.CourseId(1001L),
                "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
        when(courses.findById(any())).thenReturn(Optional.of(course));
        when(exams.createExam(any(), any(), any(), any())).thenReturn(exam);
        when(exams.save(exam)).thenReturn(exam);
        ExamManageImpl manage = new ExamManageImpl(
                courses, exams, mock(ExamEventPublisher.class),
                new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.create(new CreateExamCommand(
                1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertEquals(4001L, result.id());
        assertEquals(1001L, result.courseId());
    }

    @Test
    void shouldPersistExamPaper() {
        ExamDomainService exams = mock(ExamDomainService.class);
        Exam exam = new Exam(new ExamId(4001L), new ${package}.domain.course.vos.CourseId(1001L),
                "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
        ExamPaper paper = new ExamPaper(5001L, exam.getId(), "Paper", 100, ExamPaperStatus.DRAFT);
        when(exams.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(exams.attachPaper(any(), any(), any(Integer.class))).thenReturn(paper);
        when(exams.savePaper(paper)).thenReturn(paper);
        ExamManageImpl manage = new ExamManageImpl(
                mock(CourseDomainService.class), exams, mock(ExamEventPublisher.class),
                new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.attachPaper(new AttachExamPaperCommand(4001L, "Paper", 100));

        assertEquals(5001L, result.id());
        assertEquals(4001L, result.examId());
    }
}
