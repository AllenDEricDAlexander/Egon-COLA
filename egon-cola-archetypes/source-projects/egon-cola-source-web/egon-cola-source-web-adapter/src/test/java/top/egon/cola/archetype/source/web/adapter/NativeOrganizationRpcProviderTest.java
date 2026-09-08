package top.egon.cola.archetype.source.web.adapter;

import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import top.egon.cola.organization.facade.rpc.*;
import top.egon.cola.organization.facade.user.*;
import top.egon.cola.organization.facade.teaching.*;
import top.egon.cola.organization.facade.user.dto.*;
import top.egon.cola.organization.facade.teaching.dto.*;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;
import top.egon.cola.archetype.source.web.adapter.user.rpc.UserRpcProvider;
import top.egon.cola.archetype.source.web.adapter.teaching.rpc.SchoolClassRpcProvider;

class NativeOrganizationRpcProviderTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final OrganizationRpcConverter converter = Mappers.getMapper(OrganizationRpcConverter.class);

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }
    @Test
    void exports_five_contracts_and_all_ten_methods_from_named_beans() {
        int count = 0;
        int methods = 0;
        for (Class<?> provider : List.of(UserRpcProvider.class, SchoolClassRpcProvider.class)) {
            assertThat(provider.getAnnotation(EgonRpcProvider.class)).isNotNull();
            assertThat(provider.getAnnotation(Component.class).value()).isNotBlank();
            for (Class<?> contract : provider.getInterfaces()) {
                count++;
                methods += new RpcContractValidator().validate(contract).methods().size();
            }
        }
        assertThat(count).isEqualTo(5);
        assertThat(methods).isEqualTo(10);
    }

    @Test
    void createUser_preserves_the_request_result_and_original_failure() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, mock(RoleFacade.class), mock(PermissionFacade.class), converter, validation);
        var input = new CreateUserDTO("Mario", "mario@example.com");
        var request = converter.toTarget(input);
        var expected = new UserDetailDTO(1L, "Mario", "mario@example.com", "ACTIVE", List.of("STUDENT", "READER"));
        when(facade.createUser(input)).thenReturn(expected);
        var response = provider.createUser(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).createUser(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).createUser(input);
        var failure = provider.createUser(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
        doReturn(null).when(facade).createUser(input);
        assertThatThrownBy(() -> provider.createUser(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createUser_rejects_missing_fields_before_delegation() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, mock(RoleFacade.class), mock(PermissionFacade.class), converter, validation);
        assertThatThrownBy(() -> provider.createUser(top.egon.cola.organization.facade.rpc.proto.CreateUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getUser_preserves_the_request_result_and_original_failure() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, mock(RoleFacade.class), mock(PermissionFacade.class), converter, validation);
        var input = 1L;
        var request = top.egon.cola.organization.facade.rpc.proto.GetUserRpcRequest.newBuilder().setUserId(input).build();
        var expected = new UserDetailDTO(1L, "Mario", "mario@example.com", "ACTIVE", List.of("STUDENT", "READER"));
        when(facade.getUser(input)).thenReturn(expected);
        var response = provider.getUser(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).getUser(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).getUser(input);
        var failure = provider.getUser(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
        doReturn(null).when(facade).getUser(input);
        assertThatThrownBy(() -> provider.getUser(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getUser_rejects_missing_fields_before_delegation() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, mock(RoleFacade.class), mock(PermissionFacade.class), converter, validation);
        assertThatThrownBy(() -> provider.getUser(top.egon.cola.organization.facade.rpc.proto.GetUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void assignRole_preserves_the_request_result_and_original_failure() {
        var facade = mock(RoleFacade.class);
        var provider = new UserRpcProvider(mock(UserFacade.class), facade, mock(PermissionFacade.class), converter, validation);
        var input = new AssignRoleDTO(1L, "READER");
        var request = converter.toTarget(input);
        var response = provider.assignRole(request);
        assertThat(response.getSuccess()).isTrue();
        verify(facade).assignRole(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).assignRole(input);
        var failure = provider.assignRole(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
    }

    @Test
    void assignRole_rejects_missing_fields_before_delegation() {
        var facade = mock(RoleFacade.class);
        var provider = new UserRpcProvider(mock(UserFacade.class), facade, mock(PermissionFacade.class), converter, validation);
        assertThatThrownBy(() -> provider.assignRole(top.egon.cola.organization.facade.rpc.proto.AssignRoleRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void grantPermission_preserves_the_request_result_and_original_failure() {
        var facade = mock(PermissionFacade.class);
        var provider = new UserRpcProvider(mock(UserFacade.class), mock(RoleFacade.class), facade, converter, validation);
        var input = new GrantPermissionDTO("READER", "course:read");
        var request = converter.toTarget(input);
        var response = provider.grantPermission(request);
        assertThat(response.getSuccess()).isTrue();
        verify(facade).grantPermission(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).grantPermission(input);
        var failure = provider.grantPermission(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
    }

    @Test
    void grantPermission_rejects_missing_fields_before_delegation() {
        var facade = mock(PermissionFacade.class);
        var provider = new UserRpcProvider(mock(UserFacade.class), mock(RoleFacade.class), facade, converter, validation);
        assertThatThrownBy(() -> provider.grantPermission(top.egon.cola.organization.facade.rpc.proto.GrantPermissionRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getPermissionTree_preserves_the_request_result_and_original_failure() {
        var facade = mock(PermissionFacade.class);
        var provider = new UserRpcProvider(mock(UserFacade.class), mock(RoleFacade.class), facade, converter, validation);
        var input = 1L;
        var request = top.egon.cola.organization.facade.rpc.proto.GetPermissionTreeRpcRequest.newBuilder().setUserId(input).build();
        var expected = new PermissionTreeDTO(1L, List.of("course:read", "exam:read"));
        when(facade.getPermissionTree(input)).thenReturn(expected);
        var response = provider.getPermissionTree(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).getPermissionTree(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).getPermissionTree(input);
        var failure = provider.getPermissionTree(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
        doReturn(null).when(facade).getPermissionTree(input);
        assertThatThrownBy(() -> provider.getPermissionTree(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getPermissionTree_rejects_missing_fields_before_delegation() {
        var facade = mock(PermissionFacade.class);
        var provider = new UserRpcProvider(mock(UserFacade.class), mock(RoleFacade.class), facade, converter, validation);
        assertThatThrownBy(() -> provider.getPermissionTree(top.egon.cola.organization.facade.rpc.proto.GetPermissionTreeRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void createGrade_preserves_the_request_result_and_original_failure() {
        var facade = mock(GradeFacade.class);
        var provider = new SchoolClassRpcProvider(facade, mock(SchoolClassFacade.class), converter, validation);
        var input = new CreateGradeDTO("G1", "Grade One");
        var request = converter.toTarget(input);
        var expected = new GradeDetailDTO(10L, "G1", "Grade One", "ACTIVE");
        when(facade.createGrade(input)).thenReturn(expected);
        var response = provider.createGrade(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).createGrade(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).createGrade(input);
        var failure = provider.createGrade(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
        doReturn(null).when(facade).createGrade(input);
        assertThatThrownBy(() -> provider.createGrade(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createGrade_rejects_missing_fields_before_delegation() {
        var facade = mock(GradeFacade.class);
        var provider = new SchoolClassRpcProvider(facade, mock(SchoolClassFacade.class), converter, validation);
        assertThatThrownBy(() -> provider.createGrade(top.egon.cola.organization.facade.rpc.proto.CreateGradeRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getGrade_preserves_the_request_result_and_original_failure() {
        var facade = mock(GradeFacade.class);
        var provider = new SchoolClassRpcProvider(facade, mock(SchoolClassFacade.class), converter, validation);
        var input = 10L;
        var request = top.egon.cola.organization.facade.rpc.proto.GetGradeRpcRequest.newBuilder().setGradeId(input).build();
        var expected = new GradeDetailDTO(10L, "G1", "Grade One", "ACTIVE");
        when(facade.getGrade(input)).thenReturn(expected);
        var response = provider.getGrade(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).getGrade(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).getGrade(input);
        var failure = provider.getGrade(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
        doReturn(null).when(facade).getGrade(input);
        assertThatThrownBy(() -> provider.getGrade(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getGrade_rejects_missing_fields_before_delegation() {
        var facade = mock(GradeFacade.class);
        var provider = new SchoolClassRpcProvider(facade, mock(SchoolClassFacade.class), converter, validation);
        assertThatThrownBy(() -> provider.getGrade(top.egon.cola.organization.facade.rpc.proto.GetGradeRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void createSchoolClass_preserves_the_request_result_and_original_failure() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(mock(GradeFacade.class), facade, converter, validation);
        var input = new CreateSchoolClassDTO("Class One", "G1");
        var request = converter.toTarget(input);
        var expected = new SchoolClassDetailDTO(20L, "Class One", "G1", "Grade One", "ACTIVE", List.of(1L, 2L));
        when(facade.createSchoolClass(input)).thenReturn(expected);
        var response = provider.createSchoolClass(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).createSchoolClass(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).createSchoolClass(input);
        var failure = provider.createSchoolClass(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
        doReturn(null).when(facade).createSchoolClass(input);
        assertThatThrownBy(() -> provider.createSchoolClass(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createSchoolClass_rejects_missing_fields_before_delegation() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(mock(GradeFacade.class), facade, converter, validation);
        assertThatThrownBy(() -> provider.createSchoolClass(top.egon.cola.organization.facade.rpc.proto.CreateSchoolClassRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getSchoolClass_preserves_the_request_result_and_original_failure() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(mock(GradeFacade.class), facade, converter, validation);
        var input = new top.egon.cola.organization.facade.rpc.RpcSchoolClassQuery(10L, 20L);
        var request = top.egon.cola.organization.facade.rpc.proto.GetSchoolClassRpcRequest.newBuilder().setGradeId(input.gradeId()).setSchoolClassId(input.schoolClassId()).build();
        var expected = new SchoolClassDetailDTO(20L, "Class One", "G1", "Grade One", "ACTIVE", List.of(1L, 2L));
        when(facade.getSchoolClass(input.gradeId(), input.schoolClassId())).thenReturn(expected);
        var response = provider.getSchoolClass(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).getSchoolClass(input.gradeId(), input.schoolClassId());
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).getSchoolClass(input.gradeId(), input.schoolClassId());
        var failure = provider.getSchoolClass(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
        doReturn(null).when(facade).getSchoolClass(input.gradeId(), input.schoolClassId());
        assertThatThrownBy(() -> provider.getSchoolClass(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getSchoolClass_rejects_missing_fields_before_delegation() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(mock(GradeFacade.class), facade, converter, validation);
        assertThatThrownBy(() -> provider.getSchoolClass(top.egon.cola.organization.facade.rpc.proto.GetSchoolClassRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void assignUser_preserves_the_request_result_and_original_failure() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(mock(GradeFacade.class), facade, converter, validation);
        var input = new AssignUserToClassDTO(10L, 1L, 20L);
        var request = converter.toTarget(input);
        var response = provider.assignUser(request);
        assertThat(response.getSuccess()).isTrue();
        verify(facade).assignUser(input);
        doThrow(new OrganizationFacadeException("ORIGINAL_CODE", "original reason", "original-trace"))
                .when(facade).assignUser(input);
        var failure = provider.assignUser(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("ORIGINAL_CODE");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        assertThat(failure.getTraceId()).isEqualTo("original-trace");
    }

    @Test
    void assignUser_rejects_missing_fields_before_delegation() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(mock(GradeFacade.class), facade, converter, validation);
        assertThatThrownBy(() -> provider.assignUser(top.egon.cola.organization.facade.rpc.proto.AssignUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void validates_email_and_positive_ids_without_changing_the_business_contract() {
        var user = mock(UserFacade.class);
        var provider = new UserRpcProvider(user, mock(RoleFacade.class), mock(PermissionFacade.class), converter, validation);
        assertThatThrownBy(() -> provider.createUser(converter.toTarget(new CreateUserDTO("Mario", "invalid"))))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getUser(top.egon.cola.organization.facade.rpc.proto.GetUserRpcRequest
                .newBuilder().setUserId(0).build())).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(user);
    }
}
