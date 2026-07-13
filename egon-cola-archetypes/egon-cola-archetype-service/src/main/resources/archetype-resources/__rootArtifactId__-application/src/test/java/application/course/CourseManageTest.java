#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course;

import ${package}.application.command.course.CreateCourseCommand;
import ${package}.application.converter.course.CourseApplicationConverter;
import ${package}.application.manage.course.impl.CourseManageImpl;
import ${package}.application.validators.course.CourseApplicationValidator;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.event.CourseEventPublisher;
import ${package}.domain.course.repos.CourseRepository;
import ${package}.domain.course.repos.CourseScheduleRepository;
import ${package}.domain.course.service.impl.CourseDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseManageTest {

    @Test
    void shouldCreateNormalizedCourse() {
        CourseRepository repository = mock(CourseRepository.class);
        when(repository.existsByCode(new CourseCode("MATH-101"))).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CourseManageImpl manage = new CourseManageImpl(
                repository, mock(CourseScheduleRepository.class), mock(CourseEventPublisher.class),
                new CourseDomainServiceImpl(),
                new CourseApplicationConverter(), new CourseApplicationValidator());

        var result = manage.create(new CreateCourseCommand(" math-101 ", "Math", 3));

        assertEquals("MATH-101", result.code());
    }
}
