package top.egon.cola.archetype.source.service.infrastructure.client.organization.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import top.egon.cola.archetype.source.web.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassResponse;
import top.egon.cola.archetype.source.web.facade.proto.UserRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.UserResponse;
import top.egon.cola.archetype.source.web.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.web.facade.user.UserFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.common.exception.EgonRpcException;
import top.egon.cola.component.rpc.common.enums.EgonRpcErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import top.egon.cola.archetype.source.service.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.service.common.enums.ExternalDependencyFailure;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationSchoolClassBO;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationUserBO;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.NativeOrganizationRpcConfiguration;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.NativeOrganizationRpcProperties;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.OrganizationDirectoryConverter;
import java.util.List;

class NativeOrganizationDirectoryClientImplTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final OrganizationDirectoryConverter projection = Mappers.getMapper(OrganizationDirectoryConverter.class);
    private UserFacade userFacade;
    private SchoolClassFacade schoolClassFacade;
    private NativeOrganizationDirectoryClientImpl client;

    @BeforeEach
    void setUp() {
        userFacade = mock(UserFacade.class);
        schoolClassFacade = mock(SchoolClassFacade.class);
        client = new NativeOrganizationDirectoryClientImpl(userFacade, schoolClassFacade, projection, validation);
    }

    @AfterAll
    static void closeValidationFactory() { VALIDATORS.close(); }

    @Test
    void maps_organization_user_to_the_existing_consumer_projection() {
        when(userFacade.getUser(userRequest())).thenReturn(userSuccess(UserResponse.newBuilder()
                .setId(1001L).setName("Mario").setEmail("m@example.com").setStatus("ACTIVE")
                .addRoleCodes("STUDENT").build()));
        assertThat(client.getUser(1001L)).isEqualTo(new OrganizationUserBO(1001L, "Mario", "ACTIVE"));
        verify(userFacade).getUser(userRequest());
    }

    @Test
    void maps_school_class_and_preserves_user_order() {
        var query = GetSchoolClassRpcRequest.newBuilder().setGradeId(2001L).setSchoolClassId(3001L).build();
        when(schoolClassFacade.getSchoolClass(query)).thenReturn(SchoolClassRpcResponse.newBuilder()
                .setSuccess(true)
                .setData(SchoolClassResponse.newBuilder()
                        .setId(3001L).setName("Class One").setGradeCode("G1").setGradeName("Grade One")
                        .setStatus("ACTIVE").addUserIds(1002L).addUserIds(1001L).build())
                .build());
        assertThat(client.getSchoolClass(2001L, 3001L)).isEqualTo(
                new OrganizationSchoolClassBO(3001L, "Class One", "G1", "ACTIVE", List.of(1002L, 1001L)));
        verify(schoolClassFacade).getSchoolClass(query);
    }

    @Test
    void preserves_provider_failure_categories_and_sanitizes_messages() {
        assertProviderFailure("USER_NOT_FOUND", ExternalDependencyFailure.NOT_FOUND);
        assertProviderFailure("INVALID_USER_ID", ExternalDependencyFailure.VALIDATION_FAILED);
        assertProviderFailure("USER_CONFLICT", ExternalDependencyFailure.BUSINESS_REJECTED);
        assertProviderFailure("INTERNAL_ERROR", ExternalDependencyFailure.SERVICE_FAILURE);
    }

    @Test
    void maps_native_timeout_availability_and_contract_errors() {
        assertTransportFailure(EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED, ExternalDependencyFailure.TIMEOUT);
        assertTransportFailure(EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE, ExternalDependencyFailure.UNAVAILABLE);
        assertTransportFailure(EgonRpcErrorCode.RPC_SERVICE_NOT_FOUND, ExternalDependencyFailure.UNAVAILABLE);
        assertTransportFailure(EgonRpcErrorCode.RPC_INVALID_CONTRACT, ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        assertTransportFailure(EgonRpcErrorCode.RPC_METHOD_NOT_FOUND, ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void rejects_null_missing_or_invalid_success_data() {
        when(userFacade.getUser(userRequest())).thenReturn(null);
        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(userFacade.getUser(userRequest())).thenReturn(UserRpcResponse.newBuilder().setSuccess(true).build());
        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(userFacade.getUser(userRequest())).thenReturn(userSuccess(
                UserResponse.newBuilder().setId(0L).setName("invalid").build()));
        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void validates_local_identifiers_before_any_remote_call() {
        assertThatThrownBy(() -> client.getUser(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getUser(0L)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getSchoolClass(1L, -1L)).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(userFacade, schoolClassFacade);
    }

    @Test
    void reverse_projection_does_not_invent_fields_outside_the_client_contract() {
        var user = projection.toSource(new OrganizationUserBO(1L, "Mario", "ACTIVE"));
        assertThat(user.hasEmail()).isFalse();
        assertThat(user.getRoleCodesCount()).isZero();
        assertThat(projection.toTarget(user)).isEqualTo(new OrganizationUserBO(1L, "Mario", "ACTIVE"));
    }

    private GetUserRpcRequest userRequest() { return GetUserRpcRequest.newBuilder().setUserId(1001L).build(); }

    private static UserRpcResponse userSuccess(UserResponse data) {
        return UserRpcResponse.newBuilder().setSuccess(true).setCode("SUCCESS").setMessage("success")
                .setData(data).build();
    }

    private void assertProviderFailure(String code, ExternalDependencyFailure expected) {
        when(userFacade.getUser(userRequest())).thenReturn(UserRpcResponse.newBuilder()
                .setSuccess(false).setCode(code).setMessage("remote details").setTraceId("trace-1").build());
        assertFailure(() -> client.getUser(1001L), expected);
    }

    private void assertTransportFailure(EgonRpcErrorCode code, ExternalDependencyFailure expected) {
        doThrow(new EgonRpcException(code, "remote details")).when(userFacade).getUser(userRequest());
        assertFailure(() -> client.getUser(1001L), expected);
    }

    private static void assertFailure(Runnable invocation, ExternalDependencyFailure expected) {
        assertThatThrownBy(invocation::run).isInstanceOfSatisfying(ExternalDependencyException.class, failure -> {
            assertThat(failure.dependency()).isEqualTo("organization");
            assertThat(failure.failure()).isEqualTo(expected);
            assertThat(failure.getMessage()).doesNotContain("remote details");
        });
    }

    @Test
    void builds_exact_direct_queries_with_bounded_timeout_and_no_retry_or_fallback() {
        var strategies = mock(top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategyFactory.class);
        var proxies = mock(top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory.class);
        when(strategies.create(any())).thenReturn(mock(top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy.class));
        when(proxies.create(any(), any(), any())).thenAnswer(invocation -> {
            var contract = invocation.getArgument(0, top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor.class);
            return mock(contract.contractType());
        });
        var config = new NativeOrganizationRpcConfiguration(
                new top.egon.cola.component.rpc.contract.validation.RpcContractValidator(validation),
                strategies, proxies,
                new top.egon.cola.component.rpc.context.identity.RpcProcessIdentity("consumer", "test", "localhost", 1L, "consumer-1"),
                new NativeOrganizationRpcProperties("biz", "organization-app", "student-management-organization", "1.0", 5000),
                new top.egon.cola.component.rpc.config.EgonRpcProperties(), validation);
        config.organizationUserFacade();
        config.organizationSchoolClassFacade();
        var definitions = org.mockito.ArgumentCaptor.forClass(top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition.class);
        verify(strategies, times(2)).create(definitions.capture());
        for (var definition : definitions.getAllValues()) {
            assertThat(definition.mode()).isEqualTo(top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode.DIRECT);
            assertThat(definition.directQuery().bizCode()).isEqualTo("biz");
            assertThat(definition.directQuery().appCode()).isEqualTo("organization-app");
            assertThat(definition.directQuery().env()).isEqualTo("test");
            assertThat(definition.directQuery().version()).isEqualTo("1.0");
            assertThat(definition.directQuery().protocol()).isEqualTo("grpc");
            assertThat(definition.directQuery().group()).isEqualTo(definition.serviceIdentity().group());
            assertThat(definition.typedPolicies()).isNotEmpty();
            for (var policy : definition.typedPolicies().values()) {
                assertThat(policy.timeoutMs()).isEqualTo(3000);
                assertThat(policy.retries()).isZero();
                assertThat(policy.failStrategy()).isEqualTo(top.egon.cola.component.rpc.annotation.FailStrategy.FAIL_CLOSED);
                assertThat(policy.fallbackBean()).isEmpty();
            }
        }
    }
}
