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
        CourseDTO course = new CourseDTO(1002L, "MATH", "Math", "ACTIVE");
        when(facade.getCourse(1002L)).thenReturn(course);
        assertThat(new CourseRpcProvider(facade).getCourse(1002L)).isSameAs(course);
    }
}
