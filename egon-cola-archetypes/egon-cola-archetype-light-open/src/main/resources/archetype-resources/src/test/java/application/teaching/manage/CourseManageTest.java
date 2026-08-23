package ${package}.application.teaching.manage;

import ${package}.application.teaching.command.CreateCourseCommand;
import ${package}.application.teaching.convertor.TeachingApplicationConvertor;
import ${package}.application.teaching.manage.impl.CourseManageImpl;
import ${package}.application.teaching.query.GetCourseQuery;
import ${package}.application.teaching.result.CourseResult;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.service.CourseCacheService;
import ${package}.domain.teaching.service.CourseDomainService;
import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.service.TeachingQueryService;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.CourseSnapshot;
import ${package}.domain.teaching.vos.ExternalCourse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseManageTest {
    private static final String COURSE_ID = "018f5f9c-4f6a-7c2b-8a1d-123456789ab2";

    @Mock CourseDomainService courseDomainService;
    @Mock CourseRepository courseRepository;
    @Mock TeachingQueryService teachingQueryService;
    @Mock CourseCacheService courseCacheService;
    @Mock TeachingEventPublisher teachingEventPublisher;
    @Mock TeachingApplicationValidator applicationValidator;
    @Mock TeachingApplicationConvertor convertor;
    @InjectMocks CourseManageImpl manage;

    @Test
    void creates_course_through_domain_ports() {
        Course course = course();
        when(teachingQueryService.findExternalCourse(new CourseCode("math")))
                .thenReturn(Optional.of(new ExternalCourse(new CourseCode("math"), "Mathematics")));
        when(courseDomainService.createCourse(new CourseCode("math"), "Mathematics")).thenReturn(course);
        when(courseRepository.save(course)).thenReturn(course);
        when(convertor.toResult(course)).thenReturn(result());

        CourseResult result = manage.create(command());

        assertEquals("math", result.code());
        verify(courseCacheService).evictCourse(COURSE_ID);
        verify(teachingEventPublisher).publish(any());
    }

    @Test
    void translates_domain_failure() {
        when(teachingQueryService.findExternalCourse(new CourseCode("math")))
                .thenReturn(Optional.of(new ExternalCourse(new CourseCode("math"), "Mathematics")));
        when(courseDomainService.createCourse(new CourseCode("math"), "Mathematics"))
                .thenThrow(new TeachingDomainException("INVALID_COURSE", "invalid course"));

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> manage.create(command()));

        assertEquals("INVALID_COURSE", error.getCode());
    }

    @Test
    void returns_cached_course_without_repository_lookup() {
        CourseSnapshot snapshot = CourseSnapshot.from(course());
        when(courseCacheService.getCourse(COURSE_ID)).thenReturn(Optional.of(snapshot));
        when(convertor.toResult(snapshot)).thenReturn(result());

        CourseResult result = manage.get(new GetCourseQuery(COURSE_ID));

        assertEquals("math", result.code());
        verify(courseRepository, never()).findById(any());
    }

    @Test
    void caches_repository_result_on_query_miss() {
        Course course = course();
        CourseSnapshot snapshot = CourseSnapshot.from(course);
        when(courseCacheService.getCourse(COURSE_ID)).thenReturn(Optional.empty());
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(convertor.toSnapshot(course)).thenReturn(snapshot);
        when(convertor.toResult(course)).thenReturn(result());

        manage.get(new GetCourseQuery(COURSE_ID));

        verify(courseCacheService).putCourse(snapshot);
    }

    private CreateCourseCommand command() {
        return new CreateCourseCommand("math", "Mathematics", "operator-1", "request-1");
    }

    private Course course() {
        return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE);
    }

    private CourseResult result() {
        return new CourseResult(COURSE_ID, "math", "Mathematics", "ACTIVE");
    }
}
