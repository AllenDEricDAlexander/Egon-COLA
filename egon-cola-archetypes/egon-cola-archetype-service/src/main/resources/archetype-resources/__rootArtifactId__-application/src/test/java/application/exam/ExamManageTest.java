#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

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
        Course course = Course.create("course-1", new CourseCode("MATH-101"), "Math", 3);
        when(courses.findById(new CourseId("course-1"))).thenReturn(Optional.of(course));
        when(exams.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ExamManageImpl manage = new ExamManageImpl(
                courses, exams, mock(ExamPaperRepository.class), mock(ExamEventPublisher.class),
                new ExamDomainServiceImpl(),
                new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.create(new CreateExamCommand(
                "course-1", "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertEquals("course-1", result.courseId());
    }
}
