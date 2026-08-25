#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.vos.CourseCode;
import ${package}.infrastructure.course.repo.converter.CourseConverter;
import ${package}.infrastructure.course.repo.po.CoursePO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseRepositoryTest {
    @Test
    void shouldRoundTripCoursePersistenceModel() {
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        CourseConverter converter = Mappers.getMapper(CourseConverter.class);
        CoursePO target = converter.toTarget(course);
        target.setId(course.getId());
        Course restored = converter.toSource(target);
        assertEquals(1001L, target.getId());
        assertEquals("MATH-101", restored.getCode().value());
    }
}
