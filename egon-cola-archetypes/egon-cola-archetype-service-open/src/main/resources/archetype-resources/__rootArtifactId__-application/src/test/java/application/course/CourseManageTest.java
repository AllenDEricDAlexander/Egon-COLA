#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course;

import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.command.ScheduleCourseCommand;
import ${package}.application.course.converter.CourseApplicationConverter;
import ${package}.application.course.manage.impl.CourseManageImpl;
import ${package}.application.course.validators.CourseApplicationValidator;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.event.CourseEventPublisher;
import ${package}.domain.course.repos.CourseRepository;
import ${package}.domain.course.repos.CourseScheduleRepository;
import ${package}.domain.course.service.impl.CourseDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
                new CourseApplicationConverter(), new CourseApplicationValidator(),
                () -> 1001L);

        var result = manage.create(new CreateCourseCommand(" math-101 ", "Math", 3));

        assertEquals(1001L, result.id());
        assertEquals("MATH-101", result.code());
    }

    @Test
    void shouldGenerateScheduleIdThroughSharedGenerator() {
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseScheduleRepository scheduleRepository = mock(CourseScheduleRepository.class);
        Course course = Course.create(
                1001L,
                new CourseCode("MATH-101"), "Math", 3);
        when(courseRepository.findById(new CourseId(course.getId())))
                .thenReturn(Optional.of(course));
        when(scheduleRepository.findOverlapping(any(), anyLong(), any(), any()))
                .thenReturn(List.of());
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CourseManageImpl manage = new CourseManageImpl(
                courseRepository, scheduleRepository, mock(CourseEventPublisher.class),
                new CourseDomainServiceImpl(),
                new CourseApplicationConverter(), new CourseApplicationValidator(),
                () -> 1002L);

        var result = manage.schedule(new ScheduleCourseCommand(
                course.getId(), 2001L, Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertEquals(1002L, result.id());
    }
}
