package top.egon.cola.archetype.source.light.application.teaching.manage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.archetype.source.light.application.teaching.manage.impl.SchoolClassManageImpl;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.light.common.exception.TeachingDomainException;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.light.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseIdempotencyService;
import top.egon.cola.archetype.source.light.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingEventService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.light.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.light.domain.teaching.vos.Semester;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassManageTest {
    private static final Long CLASS_ID = 1003L;
    private static final Long COURSE_ID = 1002L;
    @Mock SchoolClassDomainService schoolClassDomainService;
    @Mock CourseDomainService courseDomainService;
    @Mock TeachingEventService teachingEventService;
    @Mock CourseIdempotencyService courseIdempotencyService;
    @Mock TeachingApplicationValidator applicationValidator;
    @Mock TeachingApplicationConvertor convertor;
    @InjectMocks SchoolClassManageImpl manage;

    @Test
    void creates_school_class_and_publishes_the_class_route() {
        SchoolClass schoolClass = schoolClass();
        when(courseIdempotencyService.claim("request-1")).thenReturn(true);
        when(schoolClassDomainService.createSchoolClass("Class One", new Semester("2026-FALL")))
                .thenReturn(schoolClass);
        when(schoolClassDomainService.save(schoolClass)).thenReturn(schoolClass);
        when(convertor.toSchoolClassResult(schoolClass)).thenReturn(result(0));

        SchoolClassResult created = manage.create(new CreateSchoolClassCommand(
                "Class One", "2026-FALL", "operator-1", "request-1"));

        assertEquals(CLASS_ID, created.id());
        ArgumentCaptor<TeachingEvent> published = ArgumentCaptor.forClass(TeachingEvent.class);
        verify(teachingEventService).publish(published.capture());
        assertEquals("class.created", published.getValue().type());
        assertEquals(CLASS_ID, published.getValue().aggregateId());
    }

    @Test
    void schedules_course_and_persists_aggregate() {
        SchoolClassAggregate aggregate = aggregate();
        Course course = course();
        CourseSchedule schedule = schedule();
        when(courseIdempotencyService.claim("request-1")).thenReturn(true);
        when(schoolClassDomainService.findAggregateById(new SchoolClassId(CLASS_ID))).thenReturn(Optional.of(aggregate));
        when(courseDomainService.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(convertor.toSchedule(any(), any())).thenReturn(schedule);
        when(schoolClassDomainService.schedule(aggregate, course, schedule)).thenReturn(aggregate);
        when(convertor.toSchoolClassResult(aggregate)).thenReturn(result(1));

        SchoolClassResult result = manage.schedule(new ScheduleCourseCommand(CLASS_ID, COURSE_ID,
                schedule.startsAt(), schedule.endsAt(), "operator-1", "request-1"));

        assertEquals(CLASS_ID, result.id());
        verify(schoolClassDomainService).saveAggregate(aggregate);
        ArgumentCaptor<TeachingEvent> published = ArgumentCaptor.forClass(TeachingEvent.class);
        verify(teachingEventService).publish(published.capture());
        assertEquals("schedule.created", published.getValue().type());
    }

    @Test
    void rejects_a_replayed_scheduling_request() {
        when(courseIdempotencyService.claim("request-1")).thenReturn(false);
        ScheduleCourseCommand command = new ScheduleCourseCommand(CLASS_ID, COURSE_ID,
                schedule().startsAt(), schedule().endsAt(), "operator-1", "request-1");

        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class, () -> manage.schedule(command));

        assertEquals("DUPLICATE_REQUEST", error.getStatus());
        verify(schoolClassDomainService, never()).saveAggregate(any());
    }

    @Test
    void translates_domain_failure() {
        SchoolClassAggregate aggregate = aggregate();
        Course course = course();
        when(courseIdempotencyService.claim("request-1")).thenReturn(true);
        when(schoolClassDomainService.findAggregateById(new SchoolClassId(CLASS_ID))).thenReturn(Optional.of(aggregate));
        when(courseDomainService.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(convertor.toSchedule(any(), any())).thenReturn(schedule());
        when(schoolClassDomainService.schedule(aggregate, course, schedule()))
                .thenThrow(new TeachingDomainException("SCHEDULE_OVERLAP", "schedule overlaps"));
        ScheduleCourseCommand command = new ScheduleCourseCommand(CLASS_ID, COURSE_ID,
                schedule().startsAt(), schedule().endsAt(), "operator-1", "request-1");

        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class, () -> manage.schedule(command));

        assertEquals("SCHEDULE_OVERLAP", error.getStatus());
    }

    @Test
    void queries_school_class() {
        SchoolClassAggregate aggregate = aggregate();
        when(schoolClassDomainService.findAggregateById(new SchoolClassId(CLASS_ID))).thenReturn(Optional.of(aggregate));
        when(convertor.toSchoolClassResult(aggregate)).thenReturn(result(0));

        assertEquals(0, manage.get(new GetSchoolClassQuery(CLASS_ID)).scheduleCount());
    }

    @Test
    void reports_missing_class() {
        when(schoolClassDomainService.findAggregateById(new SchoolClassId(CLASS_ID))).thenReturn(Optional.empty());

        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class,
                () -> manage.get(new GetSchoolClassQuery(CLASS_ID)));

        assertEquals("CLASS_NOT_FOUND", error.getStatus());
    }

    private SchoolClassResult result(int scheduleCount) {
        return new SchoolClassResult(CLASS_ID, "Class One", "2026-FALL", "ACTIVE", scheduleCount);
    }

    private SchoolClassAggregate aggregate() { return new SchoolClassAggregate(schoolClass()); }
    private SchoolClass schoolClass() {
        return new SchoolClass(new SchoolClassId(CLASS_ID), "Class One", new Semester("2026-FALL"), SchoolClassStatus.ACTIVE);
    }
    private Course course() { return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE); }
    private CourseSchedule schedule() { return new CourseSchedule(new CourseCode("math"), LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 1, 10, 0)); }
}
