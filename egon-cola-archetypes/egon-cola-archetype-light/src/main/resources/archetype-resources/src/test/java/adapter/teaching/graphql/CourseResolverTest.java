package ${package}.adapter.teaching.graphql;

import ${package}.application.teaching.manage.CourseManage;
import ${package}.application.teaching.result.CourseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(CourseResolver.class)
@ContextConfiguration(classes = CourseResolver.class)
class CourseResolverTest {
    @Autowired
    private GraphQlTester graphQlTester;
    @MockitoBean
    private CourseManage courseManage;

    @Test
    void resolves_course() {
        when(courseManage.get(any())).thenReturn(new CourseResult("course-1", "MATH", "Math", "ACTIVE"));
        graphQlTester.document("{ course(id: \"course-1\") { id code name status } }")
                .execute()
                .path("course.code").entity(String.class).isEqualTo("MATH");
    }
}
