package top.egon.cola.archetype.source.light.adapter.teaching.controller;

import jakarta.servlet.Filter;
import jakarta.validation.Validation;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
import top.egon.cola.archetype.source.light.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.start.config.NativeHttpSecurityConfiguration;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@TestPropertySource(properties = "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test")
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@WebMvcTest(SchoolClassController.class)
@ContextConfiguration(classes = {
        NativeHttpSecurityConfiguration.class,
        SchoolClassController.class,
        TeachingAdapterConvertorImpl.class,
        TeachingRequestValidator.class,
        SchoolClassControllerTest.CommonValidationConfiguration.class,
        TraceIdFilter.class,
        RequestContextFilter.class
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
    @MockitoBean(name = "schoolClassManageImpl")
    private SchoolClassManage schoolClassManage;

    @Autowired private WebApplicationContext web;
    @Autowired private NativeSecurityContractTest.SecurityFixture security;
    @BeforeEach void verifiedRequestDefaults() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(web)
                .addFilter(web.getBean(TraceIdFilter.class))
                .addFilter(web.getBean("springSecurityFilterChain", Filter.class))
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer " + security.token("1001", Set.of("egon-cola-source-light:school-class:create", "egon-cola-source-light:school-class:read", "egon-cola-source-light:school-class:schedule-course"))))
                .build();
    }

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
        assertThat(captor.getValue().operatorId()).isEqualTo("subject-1001");
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
