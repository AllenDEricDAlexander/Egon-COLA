package top.egon.cola.archetype.source.lightopen.application.teaching.manage;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl.CourseManageImpl;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetCourseQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.lightopen.domain.teaching.client.CourseCachePort;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.event.TeachingEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.teaching.exceptions.TeachingDomainException;
import top.egon.cola.archetype.source.lightopen.domain.teaching.gateway.TeachingQueryGateway;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSnapshot;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.ExternalCourse;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.CoursePO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseManageTest {
    private static final Long COURSE_ID = 1002L;
    @Mock CourseDomainService<CoursePO> courseDomainService;
    @Mock TeachingQueryGateway teachingQueryGateway;
    @Mock CourseCachePort courseCachePort;
    @Mock TeachingEventPublisher teachingEventPublisher;
    @Mock TeachingApplicationValidator applicationValidator;
    @Mock TeachingApplicationConvertor convertor;
    @InjectMocks CourseManageImpl manage;

    @Test
    void creates_course_through_domain_service() {
        Course course = course();
        when(teachingQueryGateway.findExternalCourse(new CourseCode("math")))
                .thenReturn(Optional.of(new ExternalCourse(new CourseCode("math"), "Mathematics")));
        when(courseDomainService.createCourse(new CourseCode("math"), "Mathematics")).thenReturn(course);
        when(courseDomainService.save(course)).thenReturn(course);
        when(convertor.toResult(course)).thenReturn(result());
        CourseResult result = manage.create(new CreateCourseCommand("math", "Mathematics", "operator-1", "request-1"));
        assertEquals("math", result.code());
        verify(courseCachePort).evictCourse(COURSE_ID);
        verify(teachingEventPublisher).publish(any());
    }

    @Test
    void translates_domain_failure() {
        when(teachingQueryGateway.findExternalCourse(new CourseCode("math")))
                .thenReturn(Optional.of(new ExternalCourse(new CourseCode("math"), "Mathematics")));
        when(courseDomainService.createCourse(new CourseCode("math"), "Mathematics"))
                .thenThrow(new TeachingDomainException("INVALID_COURSE", "invalid course"));
        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class,
                () -> manage.create(new CreateCourseCommand("math", "Mathematics", "operator-1", "request-1")));
        assertEquals("INVALID_COURSE", error.getCode());
    }

    @Test
    void returns_cached_course() {
        CourseSnapshot snapshot = CourseSnapshot.from(course());
        when(courseCachePort.getCourse(COURSE_ID)).thenReturn(Optional.of(snapshot));
        when(convertor.toResult(snapshot)).thenReturn(result());
        assertEquals("math", manage.get(new GetCourseQuery(COURSE_ID)).code());
    }

    private Course course() { return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE); }
    private CourseResult result() { return new CourseResult(COURSE_ID, "math", "Mathematics", "ACTIVE"); }
}
