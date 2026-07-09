package ${package}.adapter.teaching.facade;

import ${package}.adapter.teaching.facade.impl.CourseFacadeImpl;
import ${package}.application.teaching.manage.CourseManage;
import ${package}.application.teaching.manage.TeachingUseCaseException;
import ${package}.facade.teaching.dto.CreateCourseDTO;
import ${package}.facade.teaching.exceptions.TeachingFacadeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeachingFacadeImplTest {
    @Test
    void maps_application_failure_to_facade_failure() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(any())).thenThrow(new TeachingUseCaseException("COURSE_EXISTS", "Course exists", new IllegalStateException("internal")));
        CourseFacadeImpl facade = new CourseFacadeImpl(manage);

        assertThatThrownBy(() -> facade.createCourse(new CreateCourseDTO("MATH", "Math", "operator-1", "request-1")))
                .isInstanceOfSatisfying(TeachingFacadeException.class, error -> {
                    org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_EXISTS");
                    org.assertj.core.api.Assertions.assertThat(error.getCause()).isNull();
                });
    }
}
