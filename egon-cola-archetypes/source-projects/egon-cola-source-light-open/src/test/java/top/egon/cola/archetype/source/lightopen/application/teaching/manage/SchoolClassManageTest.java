package top.egon.cola.archetype.source.lightopen.application.teaching.manage;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl.SchoolClassManageImpl;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.SchoolClassResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.lightopen.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.event.TeachingEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.teaching.exceptions.TeachingDomainException;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.Semester;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.CoursePO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.SchoolClassPO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassManageTest {
    private static final Long CLASS_ID = 1003L;
    private static final Long COURSE_ID = 1002L;
    @Mock SchoolClassDomainService<SchoolClassPO> schoolClassDomainService;
    @Mock CourseDomainService<CoursePO> courseDomainService;
    @Mock TeachingEventPublisher teachingEventPublisher;
    @Mock TeachingApplicationValidator applicationValidator;
    @Mock TeachingApplicationConvertor convertor;
    @InjectMocks SchoolClassManageImpl manage;

    @Test
    void schedules_course_and_persists_aggregate() {
        SchoolClassAggregate aggregate = aggregate();
        Course course = course();
        CourseSchedule schedule = schedule();
        when(schoolClassDomainService.findAggregateById(new SchoolClassId(CLASS_ID))).thenReturn(Optional.of(aggregate));
        when(courseDomainService.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(convertor.toSchedule(any(), any())).thenReturn(schedule);
        when(schoolClassDomainService.schedule(any(), any(), any())).thenReturn(aggregate);
        when(convertor.toResult(aggregate)).thenReturn(new SchoolClassResult(CLASS_ID, "Class One", "2026-FALL", "ACTIVE", 1));
        SchoolClassResult result = manage.schedule(new ScheduleCourseCommand(CLASS_ID, COURSE_ID,
                schedule.startsAt(), schedule.endsAt(), "operator-1", "request-1"));
        assertEquals(CLASS_ID, result.id());
        verify(schoolClassDomainService).saveAggregate(aggregate);
        verify(teachingEventPublisher).publish(any());
    }

    @Test
    void translates_domain_failure() {
        SchoolClassAggregate aggregate = aggregate();
        Course course = course();
        when(schoolClassDomainService.findAggregateById(new SchoolClassId(CLASS_ID))).thenReturn(Optional.of(aggregate));
        when(courseDomainService.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(convertor.toSchedule(any(), any())).thenReturn(schedule());
        when(schoolClassDomainService.schedule(any(), any(), any()))
                .thenThrow(new TeachingDomainException("SCHEDULE_OVERLAP", "schedule overlaps"));
        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class,
                () -> manage.schedule(new ScheduleCourseCommand(CLASS_ID, COURSE_ID, schedule().startsAt(), schedule().endsAt(), "operator-1", "request-1")));
        assertEquals("SCHEDULE_OVERLAP", error.getCode());
    }

    @Test
    void queries_school_class() {
        SchoolClassAggregate aggregate = aggregate();
        when(schoolClassDomainService.findAggregateById(new SchoolClassId(CLASS_ID))).thenReturn(Optional.of(aggregate));
        when(convertor.toResult(aggregate)).thenReturn(new SchoolClassResult(CLASS_ID, "Class One", "2026-FALL", "ACTIVE", 0));
        assertEquals(0, manage.get(new GetSchoolClassQuery(CLASS_ID)).scheduleCount());
    }

    private SchoolClassAggregate aggregate() { return new SchoolClassAggregate(new SchoolClass(new SchoolClassId(CLASS_ID), "Class One", new Semester("2026-FALL"), SchoolClassStatus.ACTIVE)); }
    private Course course() { return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE); }
    private CourseSchedule schedule() { return new CourseSchedule(new CourseCode("math"), LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 1, 10, 0)); }
}
