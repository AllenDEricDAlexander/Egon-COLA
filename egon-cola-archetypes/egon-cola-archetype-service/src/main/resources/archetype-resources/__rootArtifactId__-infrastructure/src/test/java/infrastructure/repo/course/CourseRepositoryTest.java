#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.enums.CourseStatus;
import ${package}.domain.course.vos.CourseCode;
import ${package}.infrastructure.repo.course.converter.CourseConverter;
import ${package}.infrastructure.repo.course.converter.CourseDomainMapper;
import ${package}.infrastructure.repo.course.converter.CoursePoMapper;
import ${package}.infrastructure.repo.course.po.CoursePo;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseRepositoryTest {

    @Test
    void shouldRoundTripCoursePersistenceModel() {
        CoursePoMapper poMapper = mock(CoursePoMapper.class);
        CourseDomainMapper domainMapper = mock(CourseDomainMapper.class);
        Course course = Course.create("course-1", new CourseCode("MATH-101"), "Math", 3);
        CoursePo mappedPo = new CoursePo(
                "course-1", "ignored", "Math", 3, "ACTIVE",
                LocalDateTime.MIN, LocalDateTime.MAX);
        when(poMapper.convert(course)).thenReturn(mappedPo);
        Course restored = new Course();
        when(domainMapper.convert(org.mockito.ArgumentMatchers.any(CoursePo.class)))
                .thenReturn(restored);
        CourseConverter converter = new CourseConverter(poMapper, domainMapper);

        CoursePo persisted = converter.toPo(course, LocalDateTime.MIN, LocalDateTime.MAX);
        Course result = converter.toDomain(persisted);

        assertEquals("MATH-101", persisted.getCode());
        assertEquals(new CourseCode("MATH-101"), result.getCode());
        assertEquals(CourseStatus.ACTIVE, result.getStatus());
    }
}
