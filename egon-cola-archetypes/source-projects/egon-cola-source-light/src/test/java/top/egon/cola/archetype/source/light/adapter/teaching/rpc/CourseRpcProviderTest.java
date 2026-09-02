package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.teaching.CourseFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO;
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
