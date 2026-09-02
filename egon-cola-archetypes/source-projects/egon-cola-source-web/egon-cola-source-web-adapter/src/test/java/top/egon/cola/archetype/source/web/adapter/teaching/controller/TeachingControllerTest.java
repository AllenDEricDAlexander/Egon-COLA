package top.egon.cola.archetype.source.web.adapter.teaching.controller;

import top.egon.cola.archetype.source.web.adapter.teaching.controller.GradeController;
import top.egon.cola.archetype.source.web.adapter.teaching.controller.SchoolClassController;
import top.egon.cola.archetype.source.web.adapter.teaching.converter.GradeAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.teaching.converter.SchoolClassAdapterConverter;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.application.teaching.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.result.GradeDetailResult;
import top.egon.cola.archetype.source.web.application.teaching.result.SchoolClassDetailResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
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
            new GradeDetailResult(1001L, "GRADE_ONE", "Grade One", "ACTIVE"));
        when(gradeManage.getGrade(any())).thenReturn(
            new GradeDetailResult(1001L, "GRADE_ONE", "Grade One", "ACTIVE"));
        when(schoolClassManage.createSchoolClass(any())).thenReturn(
            new SchoolClassDetailResult(2001L, "Class A", "GRADE_ONE", "Grade One", "ACTIVE", List.of()));
        when(schoolClassManage.getSchoolClass(any())).thenReturn(
            new SchoolClassDetailResult(2001L, "Class A", "GRADE_ONE", "Grade One", "ACTIVE", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new GradeController(gradeManage, Mappers.getMapper(GradeAdapterConverter.class)),
            new SchoolClassController(
                schoolClassManage, Mappers.getMapper(SchoolClassAdapterConverter.class))).build();

        mockMvc.perform(post("/api/v1/grades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"GRADE_ONE\",\"name\":\"Grade One\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("GRADE_ONE"));
        mockMvc.perform(get("/api/v1/grades/1001")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/school-classes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Class A\",\"gradeCode\":\"GRADE_ONE\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.gradeCode").value("GRADE_ONE"));
        mockMvc.perform(get("/api/v1/grades/1001/school-classes/2001"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/grades/1001/school-classes/2001/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"3001\"}"))
                .andExpect(status().isNoContent());

        verify(schoolClassManage)
                .getSchoolClass(new SchoolClassDetailQuery(1001L, 2001L));
        verify(schoolClassManage).assignUser(argThat(command ->
                1001L == command.gradeId()
                        && 2001L == command.schoolClassId()
                        && 3001L == command.userId()));
    }
}
