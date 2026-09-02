package top.egon.cola.archetype.source.lightopen.adapter.teaching.graphql;

import top.egon.cola.archetype.source.lightopen.adapter.teaching.convertor.TeachingAdapterConvertorImpl;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.SchoolClassResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(CourseResolver.class)
@ContextConfiguration(classes = {CourseResolver.class, TeachingAdapterConvertorImpl.class})
class CourseResolverTest {
    @Autowired
    private GraphQlTester graphQlTester;
    @MockitoBean
    private CourseManage courseManage;
    @MockitoBean
    private SchoolClassManage schoolClassManage;

    @Test
    void resolves_course() {
        when(courseManage.get(any())).thenReturn(new CourseResult(1002L, "MATH", "Math", "ACTIVE"));
        graphQlTester.document("{ course(id: \"1002\") { id code name status } }")
                .execute()
                .path("course.code").entity(String.class).isEqualTo("MATH");
    }

    @Test
    void resolves_school_class() {
        when(schoolClassManage.get(any())).thenReturn(
                new SchoolClassResult(1003L, "Class One", "2026-FALL", "ACTIVE", 2));
        graphQlTester.document("{ schoolClass(id: \"1003\") { id name semester status scheduleCount } }")
                .execute()
                .path("schoolClass.scheduleCount").entity(Integer.class).isEqualTo(2);
    }
}
