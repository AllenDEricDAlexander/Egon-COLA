package top.egon.cola.archetype.source.light.adapter.teaching.graphql;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.convertor.TeachingAdapterConvertorImpl;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@TestPropertySource(properties = "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test")
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@GraphQlTest(CourseResolver.class)
@ContextConfiguration(classes = {CourseResolver.class, TeachingAdapterConvertorImpl.class})
class CourseResolverTest {
    @Autowired
    private GraphQlTester graphQlTester;
    @MockitoBean(name = "courseManageImpl")
    private CourseManage courseManage;
    @MockitoBean(name = "schoolClassManageImpl")
    private SchoolClassManage schoolClassManage;

    @Autowired private NativeSecurityContractTest.SecurityFixture security;
    @BeforeEach void verifiedContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.of("egon-cola-source-light:course:read", "egon-cola-source-light:school-class:read")));
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); MDC.clear(); }

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
