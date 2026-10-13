package top.egon.cola.archetype.source.web.adapter;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityContractTest.SecurityFixture;
import top.egon.cola.archetype.source.web.adapter.user.controller.UserController;
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.UserAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.filter.OrganizationAuthContextFilter;
import top.egon.cola.archetype.source.web.adapter.filter.OrganizationTraceFilter;
import top.egon.cola.archetype.source.web.adapter.handler.OrganizationGlobalExceptionHandler;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.common.enums.OrganizationFailureType;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
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
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrganizationHttpErrorContractTest {
    @TempDir Path temporary;
    private SecurityFixture security;

    @BeforeEach
    void bindVerifiedUser() throws Exception {
        security = new SecurityFixture(temporary);
        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.copyOf(security.permissions)));
    }

    @AfterEach
    void closeVerifiedUser() { security.close(); }

    @Mock UserManage userManage;

    @ParameterizedTest
    @MethodSource("failures")
    void mapsApplicationFailures(OrganizationFailureType type, HttpStatus httpStatus, String code) throws Exception {
        when(userManage.getUser(any())).thenThrow(new OrganizationApplicationException(type, code, "failure"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new UserController(security.fields, security.properties, userManage, Mappers.getMapper(UserAdapterConverter.class)))
            .setControllerAdvice(new OrganizationGlobalExceptionHandler(new top.egon.cola.platform.tianquan.jianshen.starter.web.Rbac3AuthorizationExceptionHandler()))
            .addFilters(new OrganizationTraceFilter(), new OrganizationAuthContextFilter(security.mapper, "tenantId", "userId"))
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
