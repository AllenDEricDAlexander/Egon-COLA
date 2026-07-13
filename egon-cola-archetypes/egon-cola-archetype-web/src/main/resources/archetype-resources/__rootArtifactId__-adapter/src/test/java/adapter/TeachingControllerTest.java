package ${package}.adapter;

import ${package}.adapter.controller.teaching.GradeController;
import ${package}.adapter.controller.teaching.SchoolClassController;
import ${package}.adapter.converter.GradeAdapterConverter;
import ${package}.adapter.converter.SchoolClassAdapterConverter;
import ${package}.application.teaching.manage.GradeManage;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.result.GradeDetailResult;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeachingControllerTest {
    @Mock GradeManage gradeManage;
    @Mock SchoolClassManage schoolClassManage;

    @Test
    void exposesGradeAndSchoolClassCreateGetContracts() throws Exception {
        when(gradeManage.createGrade(any())).thenReturn(
            new GradeDetailResult("grade-1", "GRADE_ONE", "Grade One", "ACTIVE"));
        when(gradeManage.getGrade(any())).thenReturn(
            new GradeDetailResult("grade-1", "GRADE_ONE", "Grade One", "ACTIVE"));
        when(schoolClassManage.createSchoolClass(any())).thenReturn(
            new SchoolClassDetailResult("class-1", "Class A", "GRADE_ONE", "Grade One", "ACTIVE", List.of()));
        when(schoolClassManage.getSchoolClass(any())).thenReturn(
            new SchoolClassDetailResult("class-1", "Class A", "GRADE_ONE", "Grade One", "ACTIVE", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new GradeController(gradeManage, new GradeAdapterConverter()),
            new SchoolClassController(schoolClassManage, new SchoolClassAdapterConverter())).build();

        mockMvc.perform(post("/api/v1/grades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"GRADE_ONE\",\"name\":\"Grade One\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("GRADE_ONE"));
        mockMvc.perform(get("/api/v1/grades/grade-1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/school-classes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Class A\",\"gradeCode\":\"GRADE_ONE\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.gradeCode").value("GRADE_ONE"));
        mockMvc.perform(get("/api/v1/school-classes/class-1")).andExpect(status().isOk());
    }
}
