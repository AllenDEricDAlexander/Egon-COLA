package top.egon.cola.archetype.source.serviceopen.application.course;

import top.egon.cola.archetype.source.serviceopen.application.course.command.CreateCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.converter.CourseApplicationConverter;
import top.egon.cola.archetype.source.serviceopen.application.course.manage.impl.CourseManageImpl;
import top.egon.cola.archetype.source.serviceopen.application.course.validators.CourseApplicationValidator;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.serviceopen.domain.course.event.CourseEventPublisher;
import top.egon.cola.archetype.source.serviceopen.domain.course.service.CourseDomainService;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseManageTest {

    @Test
    void shouldCreateNormalizedCourse() {
        CourseDomainService service = mock(CourseDomainService.class);
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        when(service.existsByCode(new CourseCode("MATH-101"))).thenReturn(false);
        when(service.createCourse(any(), any(), any(Integer.class))).thenReturn(course);
        when(service.save(course)).thenReturn(course);
        CourseManageImpl manage = new CourseManageImpl(
                mock(CourseEventPublisher.class), service,
                new CourseApplicationConverter(), new CourseApplicationValidator());

        var result = manage.create(new CreateCourseCommand(" math-101 ", "Math", 3));

        assertEquals(1001L, result.id());
        assertEquals("MATH-101", result.code());
    }

    @Test
    void shouldGenerateScheduleThroughDomainService() {
        CourseDomainService service = mock(CourseDomainService.class);
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        CourseSchedule schedule = new CourseSchedule(
                3001L, new CourseId(course.getId()), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);
        when(service.findById(new CourseId(course.getId()))).thenReturn(Optional.of(course));
        when(service.findOverlapping(any(), any(), any(), any())).thenReturn(List.of());
        when(service.scheduleCourse(any(), any(), any(), any(), any())).thenReturn(schedule);
        when(service.saveSchedule(schedule)).thenReturn(schedule);
        CourseManageImpl manage = new CourseManageImpl(
                mock(CourseEventPublisher.class), service,
                new CourseApplicationConverter(), new CourseApplicationValidator());

        var result = manage.schedule(new ScheduleCourseCommand(
                course.getId(), 2001L, Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertEquals(3001L, result.id());
    }
}
