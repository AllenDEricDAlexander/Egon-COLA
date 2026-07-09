package ${package}.application.teaching.manage;

import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.convertor.TeachingApplicationConvertor;
import ${package}.application.teaching.manage.impl.SchoolClassManageImpl;
import ${package}.application.teaching.query.GetSchoolClassQuery;
import ${package}.application.teaching.result.SchoolClassResult;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassManageTest {
    @Mock SchoolClassDomainService schoolClassDomainService;
    @Mock SchoolClassRepository schoolClassRepository;
    @Mock CourseRepository courseRepository;
    @Mock TeachingEventPublisher teachingEventPublisher;
    @Mock TeachingApplicationValidator applicationValidator;
    @Mock TeachingApplicationConvertor convertor;
    @InjectMocks SchoolClassManageImpl manage;

    @Test
    void schedules_course_and_persists_aggregate() {
        SchoolClassAggregate aggregate = aggregate();
        Course course = course();
        CourseSchedule schedule = schedule();
        when(schoolClassRepository.findAggregateById(new SchoolClassId("class-1")))
                .thenReturn(Optional.of(aggregate));
        when(courseRepository.findById("course-math")).thenReturn(Optional.of(course));
        when(convertor.toSchedule(any(), same(course))).thenReturn(schedule);
        when(schoolClassDomainService.schedule(same(aggregate), same(course), same(schedule)))
                .thenReturn(aggregate);
        when(convertor.toResult(aggregate)).thenReturn(result());

        SchoolClassResult result = manage.schedule(command());

        assertEquals("class-1", result.id());
        verify(schoolClassDomainService).schedule(same(aggregate), same(course), same(schedule));
        verify(schoolClassRepository).saveAggregate(aggregate);
        verify(teachingEventPublisher).publish(any());
    }

    @Test
    void translates_domain_failure() {
        SchoolClassAggregate aggregate = aggregate();
        Course course = course();
        CourseSchedule schedule = schedule();
        when(schoolClassRepository.findAggregateById(new SchoolClassId("class-1")))
                .thenReturn(Optional.of(aggregate));
        when(courseRepository.findById("course-math")).thenReturn(Optional.of(course));
        when(convertor.toSchedule(any(), same(course))).thenReturn(schedule);
        when(schoolClassDomainService.schedule(any(), any(), any()))
                .thenThrow(new TeachingDomainException("SCHEDULE_OVERLAP", "schedule overlaps"));

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> manage.schedule(command()));

        assertEquals("SCHEDULE_OVERLAP", error.getCode());
    }

    @Test
    void queries_school_class_with_schedule_count() {
        SchoolClassAggregate aggregate = aggregate();
        aggregate.schedule(course(), schedule());
        when(schoolClassRepository.findAggregateById(new SchoolClassId("class-1")))
                .thenReturn(Optional.of(aggregate));
        when(convertor.toResult(aggregate)).thenReturn(result());

        SchoolClassResult result = manage.get(new GetSchoolClassQuery("class-1"));

        assertEquals(1, result.scheduleCount());
    }

    private ScheduleCourseCommand command() {
        return new ScheduleCourseCommand(
                "class-1", "course-math",
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0),
                "operator-1", "request-1");
    }

    private SchoolClassAggregate aggregate() {
        return new SchoolClassAggregate(new SchoolClass(
                new SchoolClassId("class-1"), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ACTIVE));
    }

    private Course course() {
        return new Course("course-math", new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE);
    }

    private CourseSchedule schedule() {
        return new CourseSchedule(
                new CourseCode("math"), command().startsAt(), command().endsAt());
    }

    private SchoolClassResult result() {
        return new SchoolClassResult("class-1", "Class One", "2026-FALL", "ACTIVE", 1);
    }
}
