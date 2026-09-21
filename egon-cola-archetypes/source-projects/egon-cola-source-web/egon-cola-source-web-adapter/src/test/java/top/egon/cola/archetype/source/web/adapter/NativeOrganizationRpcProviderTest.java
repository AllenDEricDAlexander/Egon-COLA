package top.egon.cola.archetype.source.web.adapter;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.archetype.source.web.adapter.teaching.facade.impl.GradeFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.teaching.facade.impl.SchoolClassFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.user.facade.impl.PermissionFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.user.facade.impl.RoleFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.common.enums.OrganizationFailureType;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.GradeDetailResult;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.pojo.result.PermissionTreeResult;
import top.egon.cola.archetype.source.web.application.user.pojo.result.UserDetailResult;
import top.egon.cola.archetype.source.web.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.AssignUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateGradeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetGradeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetPermissionTreeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GrantPermissionRpcRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Wire contract of the merged native providers: Protobuf in, use case, unchanged Protobuf out. */
class NativeOrganizationRpcProviderTest {

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final OrganizationFacadeConverter converter = Mappers.getMapper(OrganizationFacadeConverter.class);

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    private UserFacadeImpl user(UserManage manage) {
        return new UserFacadeImpl(manage, converter, validation);
    }

    private RoleFacadeImpl role(RoleManage manage) {
        return new RoleFacadeImpl(manage, converter, validation);
    }

    private PermissionFacadeImpl permission(PermissionManage manage) {
        return new PermissionFacadeImpl(manage, converter, validation);
    }

    private GradeFacadeImpl grade(GradeManage manage) {
        return new GradeFacadeImpl(manage, converter, validation);
    }

    private SchoolClassFacadeImpl schoolClass(SchoolClassManage manage) {
        return new SchoolClassFacadeImpl(manage, converter, validation);
    }

    @Test
    void exports_five_contracts_and_all_ten_methods_from_named_provider_beans() {
        int count = 0;
        int methods = 0;
        for (Class<?> provider : List.of(UserFacadeImpl.class, RoleFacadeImpl.class, PermissionFacadeImpl.class,
                GradeFacadeImpl.class, SchoolClassFacadeImpl.class)) {
            assertThat(provider.getAnnotation(EgonRpcProvider.class)).isNotNull();
            assertThat(beanName(provider)).isNotBlank();
            for (Class<?> contract : provider.getInterfaces()) {
                count++;
                methods += new RpcContractValidator(VALIDATION_UTILS).validate(contract).methods().size();
            }
        }
        assertThat(count).isEqualTo(5);
        assertThat(methods).isEqualTo(10);
    }

    /** The web family keeps each provider's pre-existing stereotype; either one must carry a bean name. */
    private static String beanName(Class<?> provider) {
        var component = provider.getAnnotation(Component.class);
        return component == null ? provider.getAnnotation(Service.class).value() : component.value();
    }

    @Test
    void createUser_preserves_the_envelope_and_complete_result() {
        UserManage manage = mock(UserManage.class);
        when(manage.createUser(any())).thenReturn(new UserDetailResult(
                1L, "Mario", "mario@example.com", "ACTIVE", List.of("STUDENT", "READER")));

        var response = user(manage).createUser(CreateUserRpcRequest.newBuilder()
                .setName("Mario").setEmail("mario@example.com").build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1L);
        assertThat(response.getData().getEmail()).isEqualTo("mario@example.com");
        assertThat(response.getData().getRoleCodesList()).containsExactly("STUDENT", "READER");
    }

    @Test
    void createUser_keeps_a_business_rejection_on_the_string_code() {
        UserManage manage = mock(UserManage.class);
        when(manage.createUser(any())).thenThrow(new OrganizationApplicationException(
                OrganizationFailureType.CONFLICT, "USER_CONFLICT", "original reason"));

        var response = user(manage).createUser(CreateUserRpcRequest.newBuilder()
                .setName("Mario").setEmail("mario@example.com").build());

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("USER_CONFLICT");
        assertThat(response.getMessage()).isEqualTo("original reason");
        assertThat(response.getTraceId()).isNotBlank();
        assertThat(response.hasData()).isFalse();
    }

    @Test
    void getUser_preserves_the_envelope_and_complete_result() {
        UserManage manage = mock(UserManage.class);
        when(manage.getUser(any())).thenReturn(new UserDetailResult(
                1L, "Mario", "mario@example.com", "ACTIVE", List.of("STUDENT")));

        var response = user(manage).getUser(GetUserRpcRequest.newBuilder().setUserId(1L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Mario");
        assertThat(response.getData().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void assignRole_returns_the_ack_envelope_and_keeps_rejections() {
        RoleManage manage = mock(RoleManage.class);
        var provider = role(manage);
        var request = AssignRoleRpcRequest.newBuilder().setUserId(1L).setRoleCode("READER").build();

        assertThat(provider.assignRole(request).getSuccess()).isTrue();
        assertThat(provider.assignRole(request).getCode()).isEqualTo("SUCCESS");

        doThrow(new OrganizationApplicationException(
                OrganizationFailureType.NOT_FOUND, "USER_NOT_FOUND", "absent")).when(manage).assignRole(any());
        var failure = provider.assignRole(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(failure.getMessage()).isEqualTo("absent");
    }

    @Test
    void grantPermission_returns_the_ack_envelope() {
        PermissionManage manage = mock(PermissionManage.class);

        assertThat(permission(manage).grantPermission(GrantPermissionRpcRequest.newBuilder()
                .setRoleCode("READER").setPermissionCode("course:read").build()).getSuccess()).isTrue();
    }

    @Test
    void getPermissionTree_preserves_the_envelope_and_complete_result() {
        PermissionManage manage = mock(PermissionManage.class);
        when(manage.getPermissionTree(any())).thenReturn(new PermissionTreeResult(
                1L, List.of("course:read", "exam:read")));

        var response = permission(manage).getPermissionTree(
                GetPermissionTreeRpcRequest.newBuilder().setUserId(1L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getUserId()).isEqualTo(1L);
        assertThat(response.getData().getPermissionCodesList()).containsExactly("course:read", "exam:read");
    }

    @Test
    void createGrade_preserves_the_envelope_and_complete_result() {
        GradeManage manage = mock(GradeManage.class);
        when(manage.createGrade(any())).thenReturn(new GradeDetailResult(10L, "G1", "Grade One", "ACTIVE"));

        var response = grade(manage).createGrade(
                CreateGradeRpcRequest.newBuilder().setCode("G1").setName("Grade One").build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(10L);
        assertThat(response.getData().getCode()).isEqualTo("G1");
    }

    @Test
    void getGrade_preserves_the_envelope_and_keeps_rejections() {
        GradeManage manage = mock(GradeManage.class);
        var provider = grade(manage);
        var request = GetGradeRpcRequest.newBuilder().setGradeId(10L).build();
        when(manage.getGrade(any())).thenReturn(new GradeDetailResult(10L, "G1", "Grade One", "ACTIVE"));

        assertThat(provider.getGrade(request).getData().getName()).isEqualTo("Grade One");

        doThrow(new OrganizationApplicationException(OrganizationFailureType.NOT_FOUND,
                "GRADE_NOT_FOUND", "absent grade")).when(manage).getGrade(any());
        var failure = provider.getGrade(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("GRADE_NOT_FOUND");
        assertThat(failure.getMessage()).isEqualTo("absent grade");
    }

    @Test
    void createSchoolClass_preserves_the_envelope_and_complete_result() {
        SchoolClassManage manage = mock(SchoolClassManage.class);
        when(manage.createSchoolClass(any())).thenReturn(new SchoolClassDetailResult(
                20L, "Class One", "G1", "Grade One", "ACTIVE", List.of()));

        var response = schoolClass(manage).createSchoolClass(CreateSchoolClassRpcRequest.newBuilder()
                .setName("Class One").setGradeCode("G1").build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(20L);
        assertThat(response.getData().getGradeName()).isEqualTo("Grade One");
    }

    @Test
    void getSchoolClass_preserves_member_order() {
        SchoolClassManage manage = mock(SchoolClassManage.class);
        when(manage.getSchoolClass(any())).thenReturn(new SchoolClassDetailResult(
                20L, "Class One", "G1", "Grade One", "ACTIVE", List.of(2L, 1L)));

        var response = schoolClass(manage).getSchoolClass(GetSchoolClassRpcRequest.newBuilder()
                .setGradeId(10L).setSchoolClassId(20L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getUserIdsList()).containsExactly(2L, 1L);
    }

    @Test
    void assignUser_returns_the_ack_envelope() {
        SchoolClassManage manage = mock(SchoolClassManage.class);

        assertThat(schoolClass(manage).assignUser(AssignUserRpcRequest.newBuilder()
                .setGradeId(10L).setUserId(1L).setSchoolClassId(20L).build()).getSuccess()).isTrue();
    }

    @Test
    void rejects_a_null_use_case_result_as_a_transport_failure() {
        UserManage manage = mock(UserManage.class);
        when(manage.getUser(any())).thenReturn(null);

        assertThatThrownBy(() -> user(manage).getUser(GetUserRpcRequest.newBuilder().setUserId(1L).build()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_every_missing_field_before_the_use_case_runs() {
        UserManage userManage = mock(UserManage.class);
        RoleManage roleManage = mock(RoleManage.class);
        PermissionManage permissionManage = mock(PermissionManage.class);
        GradeManage gradeManage = mock(GradeManage.class);
        SchoolClassManage schoolClassManage = mock(SchoolClassManage.class);

        assertThatThrownBy(() -> user(userManage).createUser(CreateUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> user(userManage).getUser(GetUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> role(roleManage).assignRole(AssignRoleRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> permission(permissionManage)
                .grantPermission(GrantPermissionRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> permission(permissionManage)
                .getPermissionTree(GetPermissionTreeRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> grade(gradeManage).createGrade(CreateGradeRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> grade(gradeManage).getGrade(GetGradeRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> schoolClass(schoolClassManage)
                .createSchoolClass(CreateSchoolClassRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> schoolClass(schoolClassManage)
                .getSchoolClass(GetSchoolClassRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> schoolClass(schoolClassManage)
                .assignUser(AssignUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(userManage, roleManage, permissionManage, gradeManage, schoolClassManage);
    }

    @Test
    void validates_email_and_positive_ids_without_coercing_zero() {
        UserManage userManage = mock(UserManage.class);
        var provider = user(userManage);

        assertThatThrownBy(() -> provider.createUser(CreateUserRpcRequest.newBuilder()
                .setName("Mario").setEmail("invalid").build()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getUser(GetUserRpcRequest.newBuilder().setUserId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);

        SchoolClassManage schoolClassManage = mock(SchoolClassManage.class);
        assertThatThrownBy(() -> schoolClass(schoolClassManage).getSchoolClass(
                GetSchoolClassRpcRequest.newBuilder().setGradeId(10L).setSchoolClassId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(userManage, schoolClassManage);
    }
}
