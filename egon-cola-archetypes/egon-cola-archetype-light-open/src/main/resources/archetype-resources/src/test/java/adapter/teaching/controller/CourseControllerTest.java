package ${package}.adapter.teaching.controller;

import ${package}.adapter.filter.RequestContextFilter;
import ${package}.adapter.filter.TraceIdFilter;
import ${package}.adapter.teaching.convertor.TeachingAdapterConvertorImpl;
import ${package}.application.teaching.manage.CourseManage;
import ${package}.application.teaching.result.CourseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@ContextConfiguration(classes = {
        CourseController.class,
        TeachingAdapterConvertorImpl.class,
        TraceIdFilter.class,
        RequestContextFilter.class
})
class CourseControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CourseManage courseManage;

    @Test
    void creates_course() throws Exception {
        when(courseManage.create(any())).thenReturn(new CourseResult("course-1", "MATH", "Math", "ACTIVE"));
        mockMvc.perform(post("/api/courses")
                        .contentType("application/json")
                        .content("{\"code\":\"MATH\",\"name\":\"Math\"}"))
                .andExpect(status().isOk());
    }
}
