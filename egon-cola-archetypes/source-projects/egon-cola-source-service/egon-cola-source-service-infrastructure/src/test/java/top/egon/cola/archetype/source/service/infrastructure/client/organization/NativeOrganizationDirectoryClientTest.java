package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import top.egon.cola.organization.facade.rpc.*;
import top.egon.cola.organization.facade.rpc.proto.GetUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GetSchoolClassRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.UserRpcResponse;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;
import top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyFailure;
import top.egon.cola.archetype.source.service.domain.client.organization.*;
import java.util.List;

class NativeOrganizationDirectoryClientTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final OrganizationRpcConverter converter = Mappers.getMapper(OrganizationRpcConverter.class);
    private final OrganizationDirectoryConverter projection = Mappers.getMapper(OrganizationDirectoryConverter.class);
    private UserRpcService userService;
    private SchoolClassRpcService schoolClassService;
    private NativeOrganizationDirectoryClient client;

    @BeforeEach
    void setUp() {
        userService = mock(UserRpcService.class);
        schoolClassService = mock(SchoolClassRpcService.class);
        client = new NativeOrganizationDirectoryClient(userService, schoolClassService, converter, projection, validation);
    }

    @AfterAll
    static void closeValidationFactory() { VALIDATORS.close(); }

    @Test
    void maps_organization_user_to_the_existing_consumer_projection() {
        when(userService.getUser(userRequest())).thenReturn(converter.userSuccess(new UserDetailDTO(
                1001L, "Mario", "m@example.com", "ACTIVE", List.of("STUDENT"))));
        assertThat(client.getUser(1001L)).isEqualTo(new OrganizationUser(1001L, "Mario", "ACTIVE"));
        verify(userService).getUser(userRequest());
    }

    @Test
    void maps_school_class_and_preserves_user_order() {
        var query = GetSchoolClassRpcRequest.newBuilder().setGradeId(2001L).setSchoolClassId(3001L).build();
        when(schoolClassService.getSchoolClass(query)).thenReturn(converter.schoolClassSuccess(
                new SchoolClassDetailDTO(3001L, "Class One", "G1", "Grade One", "ACTIVE", List.of(1002L, 1001L))));
        assertThat(client.getSchoolClass(2001L, 3001L)).isEqualTo(
                new OrganizationSchoolClass(3001L, "Class One", "G1", "ACTIVE", List.of(1002L, 1001L)));
        verify(schoolClassService).getSchoolClass(query);
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
        when(userService.getUser(userRequest())).thenReturn(null);
        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(userService.getUser(userRequest())).thenReturn(UserRpcResponse.newBuilder().setSuccess(true).build());
        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
        when(userService.getUser(userRequest())).thenReturn(converter.userSuccess(
                new UserDetailDTO(0L, "invalid", null, null, List.of())));
        assertFailure(() -> client.getUser(1001L), ExternalDependencyFailure.CONTRACT_INCOMPATIBLE);
    }

    @Test
    void validates_local_identifiers_before_any_remote_call() {
        assertThatThrownBy(() -> client.getUser(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getUser(0L)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> client.getSchoolClass(1L, -1L)).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(userService, schoolClassService);
    }

    @Test
    void reverse_projection_does_not_invent_fields_outside_the_domain_port() {
        var user = projection.toSource(new OrganizationUser(1L, "Mario", "ACTIVE"));
        assertThat(user.email()).isNull();
        assertThat(user.roleCodes()).isEmpty();
        assertThat(projection.toTarget(user)).isEqualTo(new OrganizationUser(1L, "Mario", "ACTIVE"));
    }

    private GetUserRpcRequest userRequest() { return GetUserRpcRequest.newBuilder().setUserId(1001L).build(); }

    private void assertProviderFailure(String code, ExternalDependencyFailure expected) {
        when(userService.getUser(userRequest())).thenReturn(converter.userFailure(code, "remote details", "trace-1"));
        assertFailure(() -> client.getUser(1001L), expected);
    }

    private void assertTransportFailure(EgonRpcErrorCode code, ExternalDependencyFailure expected) {
        doThrow(new EgonRpcException(code, "remote details")).when(userService).getUser(userRequest());
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
                new top.egon.cola.component.rpc.contract.validation.RpcContractValidator(), strategies, proxies,
                new top.egon.cola.component.rpc.context.identity.RpcProcessIdentity("consumer", "test", "localhost", 1L, "consumer-1"),
                new NativeOrganizationRpcProperties("biz", "organization-app", "student-management-organization", "1.0", 5000),
                new top.egon.cola.component.rpc.config.EgonRpcProperties(), validation);
        config.organizationUserRpcService();
        config.organizationSchoolClassRpcService();
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
