#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course;

import ${package}.application.command.course.CreateCourseCommand;
import ${package}.application.converter.course.CourseApplicationConverter;
import ${package}.application.manage.course.impl.CourseManageImpl;
import ${package}.application.validators.course.CourseApplicationValidator;
import ${package}.domain.client.course.CourseClient;
import ${package}.domain.entities.course.Course;
import ${package}.domain.event.course.CourseEventPublisher;
import ${package}.domain.repos.course.CourseRepository;
import ${package}.domain.repos.course.CourseScheduleRepository;
import ${package}.domain.service.course.impl.CourseDomainServiceImpl;
import ${package}.domain.vos.course.CourseCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

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
                mock(CourseClient.class), repository, provider(mock(CourseScheduleRepository.class)),
                provider(mock(CourseEventPublisher.class)), new CourseDomainServiceImpl(),
                new CourseApplicationConverter(), new CourseApplicationValidator());

        var result = manage.create(new CreateCourseCommand(" math-101 ", "Math", 3));

        assertEquals("MATH-101", result.code());
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(value);
        return provider;
    }
}
