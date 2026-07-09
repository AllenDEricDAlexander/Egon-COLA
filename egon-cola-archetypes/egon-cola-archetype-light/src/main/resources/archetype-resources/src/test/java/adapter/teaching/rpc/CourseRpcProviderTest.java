package ${package}.adapter.teaching.rpc;

import ${package}.facade.teaching.CourseFacade;
import ${package}.facade.teaching.dto.CourseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseRpcProviderTest {
    @Test
    void delegates_to_course_facade() {
        CourseFacade facade = mock(CourseFacade.class);
        CourseDTO course = new CourseDTO("course-1", "MATH", "Math", "ACTIVE");
        when(facade.getCourse("course-1")).thenReturn(course);
        assertThat(new CourseRpcProvider(facade).getCourse("course-1")).isSameAs(course);
    }
}
