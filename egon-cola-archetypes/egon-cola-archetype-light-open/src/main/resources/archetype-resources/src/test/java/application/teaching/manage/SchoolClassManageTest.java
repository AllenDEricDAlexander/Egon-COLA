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
    private static final String SCHOOL_CLASS_ID = "018f5f9c-4f6a-7c2b-8a1d-123456789ab3";
    private static final String COURSE_ID = "018f5f9c-4f6a-7c2b-8a1d-123456789ab2";

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
        when(schoolClassRepository.findAggregateById(new SchoolClassId(SCHOOL_CLASS_ID)))
                .thenReturn(Optional.of(aggregate));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(convertor.toSchedule(any(), same(course))).thenReturn(schedule);
        when(schoolClassDomainService.schedule(same(aggregate), same(course), same(schedule)))
                .thenReturn(aggregate);
        when(convertor.toResult(aggregate)).thenReturn(result());

        SchoolClassResult result = manage.schedule(command());

        assertEquals(SCHOOL_CLASS_ID, result.id());
        verify(schoolClassDomainService).schedule(same(aggregate), same(course), same(schedule));
        verify(schoolClassRepository).saveAggregate(aggregate);
        verify(teachingEventPublisher).publish(any());
    }

    @Test
    void translates_domain_failure() {
        SchoolClassAggregate aggregate = aggregate();
        Course course = course();
        CourseSchedule schedule = schedule();
        when(schoolClassRepository.findAggregateById(new SchoolClassId(SCHOOL_CLASS_ID)))
                .thenReturn(Optional.of(aggregate));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
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
        when(schoolClassRepository.findAggregateById(new SchoolClassId(SCHOOL_CLASS_ID)))
                .thenReturn(Optional.of(aggregate));
        when(convertor.toResult(aggregate)).thenReturn(result());

        SchoolClassResult result = manage.get(new GetSchoolClassQuery(SCHOOL_CLASS_ID));

        assertEquals(1, result.scheduleCount());
    }

    private ScheduleCourseCommand command() {
        return new ScheduleCourseCommand(
                SCHOOL_CLASS_ID, COURSE_ID,
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0),
                "operator-1", "request-1");
    }

    private SchoolClassAggregate aggregate() {
        return new SchoolClassAggregate(new SchoolClass(
                new SchoolClassId(SCHOOL_CLASS_ID), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ACTIVE));
    }

    private Course course() {
        return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE);
    }

    private CourseSchedule schedule() {
        return new CourseSchedule(
                new CourseCode("math"), command().startsAt(), command().endsAt());
    }

    private SchoolClassResult result() {
        return new SchoolClassResult(SCHOOL_CLASS_ID, "Class One", "2026-FALL", "ACTIVE", 1);
    }
}
