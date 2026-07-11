#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

import ${package}.application.command.exam.CreateExamCommand;
import ${package}.application.converter.exam.ExamApplicationConverter;
import ${package}.application.manage.exam.impl.ExamManageImpl;
import ${package}.application.validators.exam.ExamApplicationValidator;
import ${package}.domain.entities.course.Course;
import ${package}.domain.event.exam.ExamEventPublisher;
import ${package}.domain.repos.course.CourseRepository;
import ${package}.domain.repos.exam.ExamPaperRepository;
import ${package}.domain.repos.exam.ExamRepository;
import ${package}.domain.service.exam.impl.ExamDomainServiceImpl;
import ${package}.domain.vos.course.CourseCode;
import ${package}.domain.vos.course.CourseId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

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
                courses, provider(exams), provider(mock(ExamPaperRepository.class)),
                provider(mock(ExamEventPublisher.class)), new ExamDomainServiceImpl(),
                new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.create(new CreateExamCommand(
                "course-1", "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertEquals("course-1", result.courseId());
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(value);
        return provider;
    }
}
