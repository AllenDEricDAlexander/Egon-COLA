package top.egon.cola.archetype.source.light.adapter.teaching.controller;

import jakarta.servlet.Filter;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextFilter;
import top.egon.cola.archetype.source.light.adapter.filter.TraceIdFilter;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.convertor.TeachingAdapterConvertorImpl;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.start.config.NativeHttpSecurityConfiguration;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@TestPropertySource(properties = "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test")
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@WebMvcTest(CourseController.class)
@ContextConfiguration(classes = {
        NativeHttpSecurityConfiguration.class,
        CourseController.class,
        TeachingAdapterConvertorImpl.class,
        TraceIdFilter.class,
        RequestContextFilter.class
})
class CourseControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean(name = "courseManageImpl")
    private CourseManage courseManage;

    @Autowired private WebApplicationContext web;
    @Autowired private NativeSecurityContractTest.SecurityFixture security;
    @BeforeEach void verifiedRequestDefaults() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(web)
                .addFilter(web.getBean(TraceIdFilter.class))
                .addFilter(web.getBean("springSecurityFilterChain", Filter.class))
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer " + security.token("1001", Set.of("egon-cola-source-light:course:create", "egon-cola-source-light:course:read"))))
                .build();
    }

    @Test
    void creates_course() throws Exception {
        when(courseManage.create(any())).thenReturn(new CourseResult(1002L, "MATH", "Math", "ACTIVE"));
        mockMvc.perform(post("/api/courses")
                        .contentType("application/json")
                        .content("{\"code\":\"MATH\",\"name\":\"Math\"}"))
                .andExpect(status().isOk());
    }
}
