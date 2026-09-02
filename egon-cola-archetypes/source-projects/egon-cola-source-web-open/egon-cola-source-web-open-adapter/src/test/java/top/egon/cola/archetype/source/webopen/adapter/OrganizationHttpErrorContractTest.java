package top.egon.cola.archetype.source.webopen.adapter;

import top.egon.cola.archetype.source.webopen.adapter.user.controller.UserController;
import top.egon.cola.archetype.source.webopen.adapter.user.converter.UserAdapterConverter;
import top.egon.cola.archetype.source.webopen.adapter.filter.OrganizationAuthContextFilter;
import top.egon.cola.archetype.source.webopen.adapter.filter.OrganizationTraceFilter;
import top.egon.cola.archetype.source.webopen.adapter.handler.OrganizationGlobalExceptionHandler;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrganizationHttpErrorContractTest {
    @Mock UserManage userManage;

    @ParameterizedTest
    @MethodSource("failures")
    void mapsApplicationFailures(OrganizationFailureType type, HttpStatus httpStatus, String code) throws Exception {
        when(userManage.getUser(any())).thenThrow(new OrganizationApplicationException(type, code, "failure"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new UserController(userManage, Mappers.getMapper(UserAdapterConverter.class),
                    (LongIdGenerator) () -> 9001L))
            .setControllerAdvice(new OrganizationGlobalExceptionHandler())
            .addFilters(new OrganizationTraceFilter((LongIdGenerator) () -> 9001L),
                new OrganizationAuthContextFilter())
            .build();

        mockMvc.perform(get("/api/v1/users/1001").header("X-Trace-Id", "trace-1"))
            .andExpect(status().is(httpStatus.value()))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.traceId").value("trace-1"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    static Stream<Arguments> failures() {
        return Stream.of(
            Arguments.of(OrganizationFailureType.VALIDATION, HttpStatus.BAD_REQUEST, "ORG_VALIDATION_ERROR"),
            Arguments.of(OrganizationFailureType.FORBIDDEN, HttpStatus.FORBIDDEN, "ORG_FORBIDDEN"),
            Arguments.of(OrganizationFailureType.NOT_FOUND, HttpStatus.NOT_FOUND, "ORG_NOT_FOUND"),
            Arguments.of(OrganizationFailureType.CONFLICT, HttpStatus.CONFLICT, "ORG_CONFLICT"),
            Arguments.of(OrganizationFailureType.DOMAIN_REJECTED, HttpStatus.UNPROCESSABLE_ENTITY, "ORG_DOMAIN_REJECTED"),
            Arguments.of(OrganizationFailureType.DEPENDENCY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, "ORG_DEPENDENCY_UNAVAILABLE"),
            Arguments.of(OrganizationFailureType.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR, "ORG_INTERNAL_ERROR"));
    }
}
