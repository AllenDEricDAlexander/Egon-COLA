package top.egon.cola.archetype.source.lightopen.adapter.teaching.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextFilter;
import top.egon.cola.archetype.source.lightopen.adapter.filter.TraceIdFilter;
import top.egon.cola.archetype.source.lightopen.adapter.handler.GlobalExceptionHandler;
import top.egon.cola.archetype.source.lightopen.adapter.handler.ResponseWrapperHandler;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.convertor.TeachingAdapterConvertorImpl;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.lightopen.start.config.OpenApiConfig;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleUtcContractTest {

    private static final String ROUTE = "/api/school-classes/1003/courses/1002/schedule";
    private ValidatorFactory validators;
    private SchoolClassManage manage;
    private ObjectMapper mapper;
    private MockMvc mvc;

    @BeforeEach
    void prepareActualMapperAndExistingOpenContext() throws Exception {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.jackson.time-zone=UTC", "spring.jackson.serialization.write-dates-as-timestamps=false")
                .run(context -> mapper = context.getBean(ObjectMapper.class));
        validators = Validation.buildDefaultValidatorFactory();
        manage = mock(SchoolClassManage.class);
        mvc = MockMvcBuilders.standaloneSetup(controller(manage, validators))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setControllerAdvice(new GlobalExceptionHandler(), new ResponseWrapperHandler(mapper))
                .addFilters(new TraceIdFilter(), new RequestContextFilter(mapper))
                .defaultRequest(get("/").header("X-Operator-Id", "operator-1").header("X-Tenant-Id", "41"))
                .build();
    }

    @AfterEach
    void closeContext() {
        if (validators != null) { validators.close(); }
    }

    @Test
    void offsetAndUtcInputsReachTheSameMicroCommandAndKeepTheResponseEnvelope() throws Exception {
        when(manage.schedule(any())).thenReturn(new SchoolClassResult(1003L, "Class One", "2026-FALL", "ACTIVE", 2));
        for (String startsAt : List.of("2026-10-13T06:07:08.123456789Z", "2026-10-13T14:07:08.123456789+08:00")) {
            clearInvocations(manage);
            mvc.perform(post(ROUTE).contentType("application/json").content("{\"startsAt\":\"" + startsAt
                            + "\",\"endsAt\":\"2026-10-13T07:07:08.123456789Z\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.timestamp").isNumber()).andExpect(jsonPath("$.data.id").value("1003"))
                    .andExpect(jsonPath("$.data.scheduleCount").value(2)).andExpect(jsonPath("$.data.startsAt").doesNotExist());
            ArgumentCaptor<ScheduleCourseCommand> captured = ArgumentCaptor.forClass(ScheduleCourseCommand.class);
            verify(manage).schedule(captured.capture());
            assertThat((Object) captured.getValue().startsAt()).isEqualTo(Instant.parse("2026-10-13T06:07:08.123456Z"));
            assertThat((Object) captured.getValue().endsAt()).isEqualTo(Instant.parse("2026-10-13T07:07:08.123456Z"));
            assertThat(captured.getValue().operatorId()).isEqualTo("operator-1");
        }
    }

    @Test
    void invalidTimeTokensFailWithTheExistingValidationErrorWithoutCallingManage() throws Exception {
        List<String> invalid = List.of("1700000000", "1700000000.5", "\"1700000000\"", "true", "[]", "{}", "null",
                "\"2026-10-13T06:07:08\"", "\" 2026-10-13T06:07:08Z\"", "\"2026-10-13T06:07:08Z \"",
                "\"2026-10-13T06:07Z\"", "\"2026-10-13T06:07:08+08:00:00\"",
                "\"2026-12-31T23:59:60Z\"", "\"2026-10-13T06:07:08.1234567890Z\"",
                "\"2026-02-30T06:07:08Z\"", "\"2026-10-13T06:07:08[Asia/Shanghai]\"");
        for (String token : invalid) {
            mvc.perform(post(ROUTE).contentType("application/json")
                            .content("{\"startsAt\":" + token + ",\"endsAt\":\"2026-10-13T07:07:08Z\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(ResultCode.VALIDATION_ERROR.getCode()))
                    .andExpect(jsonPath("$.status").value(ResultCode.VALIDATION_ERROR.getStatus()));
            verifyNoInteractions(manage);
        }
        mvc.perform(post(ROUTE).contentType("application/json").content("{\"endsAt\":\"2026-10-13T07:07:08Z\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(ResultCode.VALIDATION_ERROR.getCode()));
        mvc.perform(post(ROUTE).contentType("application/json").content("{\"startsAt\":"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(manage);
    }

    @Test
    void windowsCollapsedByMicroPrecisionFailBeforeManage() throws Exception {
        when(manage.schedule(any())).thenReturn(new SchoolClassResult(1003L, "Class One", "2026-FALL", "ACTIVE", 2));
        mvc.perform(post(ROUTE).contentType("application/json").content("""
                {"startsAt":"2026-10-13T06:07:08.123456100Z","endsAt":"2026-10-13T06:07:08.123456900Z"}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(ResultCode.VALIDATION_ERROR.getCode()));
        verifyNoInteractions(manage);
    }

    @Test
    void generatedOpenApiDescribesTheActualTimeAndResponseContracts() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("utc-contract", Map.of(
                    "springdoc.api-docs.enabled", "true", "springdoc.cache.disabled", "true")));
            context.register(DocumentConfiguration.class);
            context.refresh();
            MockMvc docs = MockMvcBuilders.webAppContextSetup(context).build();
            String json = docs.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            JsonNode document = mapper.readTree(json);
            JsonNode operation = document.path("paths").path("/api/school-classes/{schoolClassId}/courses/{courseId}/schedule").path("post");
            assertThat(operation.path("operationId").asText()).isEqualTo("schedule");
            JsonNode request = document.path("components").path("schemas").path("ScheduleCourseDTO");
            assertThat(request.path("required").toString()).contains("startsAt", "endsAt");
            for (String field : List.of("startsAt", "endsAt")) {
                assertThat(request.path("properties").path(field).path("type").asText()).isEqualTo("string");
                assertThat(request.path("properties").path(field).path("format").asText()).isEqualTo("date-time");
                assertThat(request.path("properties").path(field).path("example").asText()).endsWith("Z");
            }
            assertThat(operation.path("description").asText()).contains("microseconds");
            JsonNode response = operation.path("responses").path("200").path("content").path("application/json").path("schema");
            assertThat(response.path("properties").path("data").path("$ref").asText()).endsWith("/SchoolClassDetailVO");
            assertThat(response.path("properties").path("timestamp").path("type").asText()).isEqualTo("integer");
            assertThat(document.path("components").path("schemas").path("SchoolClassDetailVO").path("properties").size()).isEqualTo(5);
            assertThat(operation.path("responses").path("400").toString()).contains("ResultRecord");
        }
    }

    private static SchoolClassController controller(SchoolClassManage manage, ValidatorFactory validators) {
        return new SchoolClassController(manage, new TeachingRequestValidator(new ValidationUtils(validators.getValidator())),
                new TeachingAdapterConvertorImpl());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableConfigurationProperties(SpringDocConfigProperties.class)
    @Import({JacksonAutoConfiguration.class, SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class, OpenApiConfig.class})
    static class DocumentConfiguration {
        @Bean(destroyMethod = "close") ValidatorFactory validators() { return Validation.buildDefaultValidatorFactory(); }
        @Bean("schoolClassController") SchoolClassController schoolClassController(ValidatorFactory validators) {
            return controller(mock(SchoolClassManage.class), validators);
        }
    }
}
