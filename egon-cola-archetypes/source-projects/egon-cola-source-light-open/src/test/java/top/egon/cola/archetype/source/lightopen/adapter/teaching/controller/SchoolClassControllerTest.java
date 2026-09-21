package top.egon.cola.archetype.source.lightopen.adapter.teaching.controller;

import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextFilter;
import top.egon.cola.archetype.source.lightopen.adapter.filter.TraceIdFilter;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.convertor.TeachingAdapterConvertorImpl;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.result.SchoolClassResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import jakarta.validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchoolClassController.class)
@ContextConfiguration(classes = {
        SchoolClassController.class,
        TeachingAdapterConvertorImpl.class,
        TeachingRequestValidator.class,
        TraceIdFilter.class,
        RequestContextFilter.class,
        SchoolClassControllerTest.CommonValidationConfiguration.class
})
class SchoolClassControllerTest {

    /** The web slice has no starter auto-configuration, so it declares the common validation facade itself. */
    @TestConfiguration
    static class CommonValidationConfiguration {
        @Bean("egonColaValidationUtils")
        ValidationUtils egonColaValidationUtils() {
            return new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SchoolClassManage schoolClassManage;

    @Test
    void creates_school_class_with_typed_context() throws Exception {
        when(schoolClassManage.create(any())).thenReturn(new SchoolClassResult(1003L, "Class One", "2026-FALL", "ACTIVE", 0));
        mockMvc.perform(post("/api/school-classes")
                        .header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1")
                        .contentType("application/json")
                        .content("{\"name\":\"Class One\",\"semester\":\"2026-FALL\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateSchoolClassCommand> captor = ArgumentCaptor.forClass(CreateSchoolClassCommand.class);
        verify(schoolClassManage).create(captor.capture());
        assertThat(captor.getValue().operatorId()).isEqualTo("operator-1");
    }

    @Test
    void gets_school_class() throws Exception {
        when(schoolClassManage.get(any())).thenReturn(
                new SchoolClassResult(1003L, "Class One", "2026-FALL", "ACTIVE", 2));

        mockMvc.perform(get("/api/school-classes/1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleCount").value(2));
    }
}
