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
import ${package}.domain.exam.event.ExamEventPublisher;
import ${package}.domain.course.repos.CourseRepository;
import ${package}.domain.exam.repos.ExamPaperRepository;
import ${package}.domain.exam.repos.ExamRepository;
import ${package}.domain.exam.service.impl.ExamDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
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
        CourseRepository courses = mock(CourseRepository.class);
        ExamRepository exams = mock(ExamRepository.class);
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        when(courses.findById(new CourseId(1001L))).thenReturn(Optional.of(course));
        when(exams.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ExamManageImpl manage = new ExamManageImpl(
                courses, exams, mock(ExamPaperRepository.class), mock(ExamEventPublisher.class),
                new ExamDomainServiceImpl(),
                new ExamApplicationConverter(), new ExamApplicationValidator(),
                () -> 1002L);

        var result = manage.create(new CreateExamCommand(
                1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertEquals(1002L, result.id());
        assertEquals(1001L, result.courseId());
    }

    @Test
    void shouldGenerateExamPaperIdThroughSharedGenerator() {
        ExamRepository exams = mock(ExamRepository.class);
        ExamPaperRepository papers = mock(ExamPaperRepository.class);
        var exam = new ExamDomainServiceImpl().createExam(
                1002L,
                Course.create(1001L, new CourseCode("MATH-101"), "Math", 3),
                "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(exams.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(papers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ExamManageImpl manage = new ExamManageImpl(
                mock(CourseRepository.class), exams, papers, mock(ExamEventPublisher.class),
                new ExamDomainServiceImpl(),
                new ExamApplicationConverter(), new ExamApplicationValidator(),
                () -> 1003L);

        var result = manage.attachPaper(new AttachExamPaperCommand(
                exam.getId().value(), "Midterm paper", 100));

        assertEquals(1003L, result.id());
        assertEquals(exam.getId().value(), result.examId());
    }
}
