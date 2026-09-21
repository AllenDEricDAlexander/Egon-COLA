package top.egon.cola.archetype.source.light.application.teaching.manage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.archetype.source.light.application.teaching.manage.impl.CourseManageImpl;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.light.common.exception.TeachingDomainException;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseIdempotencyService;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingEventService;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingQueryService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.ExternalCourse;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseManageTest {
    private static final Long COURSE_ID = 1002L;
    @Mock CourseDomainService courseDomainService;
    @Mock TeachingQueryService teachingQueryService;
    @Mock TeachingEventService teachingEventService;
    @Mock CourseIdempotencyService courseIdempotencyService;
    @Mock TeachingApplicationValidator applicationValidator;
    @Mock TeachingApplicationConvertor convertor;
    @InjectMocks CourseManageImpl manage;

    @Test
    void creates_course_through_domain_service() {
        Course course = course();
        when(courseIdempotencyService.claim("request-1")).thenReturn(true);
        when(teachingQueryService.findExternalCourse(new CourseCode("math")))
                .thenReturn(Optional.of(new ExternalCourse(new CourseCode("math"), "Mathematics")));
        when(courseDomainService.createCourse(new CourseCode("math"), "Mathematics")).thenReturn(course);
        when(courseDomainService.save(course)).thenReturn(course);
        when(convertor.toTarget(course)).thenReturn(result());

        CourseResult result = manage.create(command());

        assertEquals("math", result.code());
        verify(applicationValidator).validate(command());
        ArgumentCaptor<TeachingEvent> published = ArgumentCaptor.forClass(TeachingEvent.class);
        verify(teachingEventService).publish(published.capture());
        assertEquals("course.created", published.getValue().type());
        assertEquals(COURSE_ID, published.getValue().aggregateId());
    }

    @Test
    void rejects_a_replayed_request_before_touching_the_aggregate() {
        when(courseIdempotencyService.claim("request-1")).thenReturn(false);

        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class, () -> manage.create(command()));

        assertEquals("DUPLICATE_REQUEST", error.getStatus());
        verify(courseDomainService, never()).save(any());
    }

    @Test
    void rejects_an_unresolvable_external_course() {
        when(courseIdempotencyService.claim("request-1")).thenReturn(true);
        when(teachingQueryService.findExternalCourse(new CourseCode("math"))).thenReturn(Optional.empty());

        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class, () -> manage.create(command()));

        assertEquals("EXTERNAL_COURSE_NOT_FOUND", error.getStatus());
    }

    @Test
    void translates_domain_failure() {
        when(courseIdempotencyService.claim("request-1")).thenReturn(true);
        when(teachingQueryService.findExternalCourse(new CourseCode("math")))
                .thenReturn(Optional.of(new ExternalCourse(new CourseCode("math"), "Mathematics")));
        when(courseDomainService.createCourse(new CourseCode("math"), "Mathematics"))
                .thenThrow(new TeachingDomainException("INVALID_COURSE", "invalid course"));

        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class, () -> manage.create(command()));

        assertEquals("INVALID_COURSE", error.getStatus());
    }

    @Test
    void reads_course_through_the_domain_service() {
        Course course = course();
        when(courseDomainService.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(convertor.toTarget(course)).thenReturn(result());

        assertEquals("math", manage.get(new GetCourseQuery(COURSE_ID)).code());
    }

    @Test
    void reports_missing_course() {
        when(courseDomainService.findById(COURSE_ID)).thenReturn(Optional.empty());

        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class,
                () -> manage.get(new GetCourseQuery(COURSE_ID)));

        assertEquals("COURSE_NOT_FOUND", error.getStatus());
    }

    private CreateCourseCommand command() {
        return new CreateCourseCommand("math", "Mathematics", "operator-1", "request-1");
    }

    private Course course() { return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE); }
    private CourseResult result() { return new CourseResult(COURSE_ID, "math", "Mathematics", "ACTIVE"); }
}
