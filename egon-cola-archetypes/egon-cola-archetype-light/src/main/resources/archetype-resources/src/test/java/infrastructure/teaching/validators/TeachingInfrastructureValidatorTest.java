package ${package}.infrastructure.teaching.validators;

import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeachingInfrastructureValidatorTest {
    private final TeachingInfrastructureValidator validator = new TeachingInfrastructureValidator();

    @Test
    void validates_external_course_and_publication_result() {
        ExternalCourse course = new ExternalCourse(new CourseCode("OTHER"), "Other");
        assertEquals("INVALID_EXTERNAL_COURSE", assertThrows(TeachingDomainException.class,
                () -> validator.validateExternalCourse(course, new CourseCode("COURSE-001"))).getCode());
        assertEquals("TEACHING_EVENT_PUBLISH_FAILED", assertThrows(TeachingDomainException.class,
                () -> validator.requirePublished(false)).getCode());
    }

    @Test
    void rejects_malformed_cache_payload() {
        assertEquals("INVALID_COURSE_CACHE", validator.invalidCachePayload(
                "bad", new IllegalArgumentException()).getCode());
    }
}
