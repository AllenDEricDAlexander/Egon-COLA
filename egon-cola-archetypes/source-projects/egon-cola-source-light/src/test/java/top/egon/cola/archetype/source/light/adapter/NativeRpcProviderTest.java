package top.egon.cola.archetype.source.light.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.light.facade.rpc.*;
import top.egon.cola.archetype.source.light.facade.rpc.proto.*;
import top.egon.cola.archetype.source.light.facade.teaching.*;
import top.egon.cola.archetype.source.light.facade.teaching.dto.*;
import top.egon.cola.archetype.source.light.facade.teaching.exceptions.TeachingFacadeException;
import top.egon.cola.archetype.source.light.facade.user.*;
import top.egon.cola.archetype.source.light.facade.user.dto.*;
import top.egon.cola.archetype.source.light.facade.user.exceptions.UserFacadeException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.springframework.stereotype.Component;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.archetype.source.light.adapter.teaching.rpc.CourseRpcProvider;
import top.egon.cola.archetype.source.light.adapter.teaching.rpc.SchoolClassRpcProvider;
import top.egon.cola.archetype.source.light.adapter.user.rpc.UserRpcProvider;
import top.egon.cola.archetype.source.light.adapter.user.rpc.PermissionRpcProvider;
import static org.assertj.core.api.Assertions.assertThat;

class NativeRpcProviderTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final LightRpcConverter converter = Mappers.getMapper(LightRpcConverter.class);

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    @Test
    void exports_all_native_contracts_with_named_beans() {
        for (Class<?> provider : new Class<?>[]{CourseRpcProvider.class, SchoolClassRpcProvider.class,
                UserRpcProvider.class, PermissionRpcProvider.class}) {
            assertThat(provider.isAnnotationPresent(EgonRpcProvider.class)).as(provider.getSimpleName()).isTrue();
            assertThat(provider.getAnnotation(Component.class).value()).isNotBlank();
            assertThat(provider.getInterfaces()).allMatch(type -> type.getSimpleName().endsWith("RpcService"));
        }
    }

    @Test
    void createCourse_preserves_request_result_and_business_failure() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        var input = new CreateCourseDTO("MATH", "Math", "operator", "request");
        var expected = new CourseDTO(1L, "MATH", "Math", "ACTIVE");
        var request = converter.toTarget(input);
        when(facade.createCourse(input)).thenReturn(expected);
        var response = provider.createCourse(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).createCourse(input);

        when(facade.createCourse(input)).thenThrow(new TeachingFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.createCourse(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).createCourse(input);

        doReturn(null).when(facade).createCourse(input);
        assertThatThrownBy(() -> provider.createCourse(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).createCourse(input);
        assertThatThrownBy(() -> provider.createCourse(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createCourse_rejects_missing_input_before_delegation() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.createCourse(CreateCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getCourse_preserves_request_result_and_business_failure() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        var input = 1L;
        var expected = new CourseDTO(1L, "MATH", "Math", "ACTIVE");
        var request = GetCourseRpcRequest.newBuilder().setCourseId(input).build();
        when(facade.getCourse(input)).thenReturn(expected);
        var response = provider.getCourse(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).getCourse(input);

        when(facade.getCourse(input)).thenThrow(new TeachingFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.getCourse(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).getCourse(input);

        doReturn(null).when(facade).getCourse(input);
        assertThatThrownBy(() -> provider.getCourse(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).getCourse(input);
        assertThatThrownBy(() -> provider.getCourse(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getCourse_rejects_missing_input_before_delegation() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.getCourse(GetCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getCourse(GetCourseRpcRequest.newBuilder().setCourseId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void createSchoolClass_preserves_request_result_and_business_failure() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(facade, converter, validation);
        var input = new CreateSchoolClassDTO("Class One", "2026-FALL", "operator", "request");
        var expected = new SchoolClassDetailDTO(2L, "Class One", "2026-FALL", "ACTIVE", 1);
        var request = converter.toTarget(input);
        when(facade.createSchoolClass(input)).thenReturn(expected);
        var response = provider.createSchoolClass(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).createSchoolClass(input);

        when(facade.createSchoolClass(input)).thenThrow(new TeachingFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.createSchoolClass(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).createSchoolClass(input);

        doReturn(null).when(facade).createSchoolClass(input);
        assertThatThrownBy(() -> provider.createSchoolClass(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).createSchoolClass(input);
        assertThatThrownBy(() -> provider.createSchoolClass(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createSchoolClass_rejects_missing_input_before_delegation() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.createSchoolClass(CreateSchoolClassRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void scheduleCourse_preserves_request_result_and_business_failure() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(facade, converter, validation);
        var input = new ScheduleCourseDTO(2L, 1L, LocalDateTime.parse("2026-09-08T10:00:00.123456789"), LocalDateTime.parse("2026-09-08T11:00:00.987654321"), "operator", "request");
        var expected = new SchoolClassDetailDTO(2L, "Class One", "2026-FALL", "ACTIVE", 1);
        var request = converter.toTarget(input);
        when(facade.scheduleCourse(input)).thenReturn(expected);
        var response = provider.scheduleCourse(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).scheduleCourse(input);

        when(facade.scheduleCourse(input)).thenThrow(new TeachingFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.scheduleCourse(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).scheduleCourse(input);

        doReturn(null).when(facade).scheduleCourse(input);
        assertThatThrownBy(() -> provider.scheduleCourse(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).scheduleCourse(input);
        assertThatThrownBy(() -> provider.scheduleCourse(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void scheduleCourse_rejects_missing_input_before_delegation() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.scheduleCourse(ScheduleCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getSchoolClass_preserves_request_result_and_business_failure() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(facade, converter, validation);
        var input = 2L;
        var expected = new SchoolClassDetailDTO(2L, "Class One", "2026-FALL", "ACTIVE", 1);
        var request = GetSchoolClassRpcRequest.newBuilder().setSchoolClassId(input).build();
        when(facade.getSchoolClass(input)).thenReturn(expected);
        var response = provider.getSchoolClass(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).getSchoolClass(input);

        when(facade.getSchoolClass(input)).thenThrow(new TeachingFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.getSchoolClass(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).getSchoolClass(input);

        doReturn(null).when(facade).getSchoolClass(input);
        assertThatThrownBy(() -> provider.getSchoolClass(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).getSchoolClass(input);
        assertThatThrownBy(() -> provider.getSchoolClass(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getSchoolClass_rejects_missing_input_before_delegation() {
        var facade = mock(SchoolClassFacade.class);
        var provider = new SchoolClassRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.getSchoolClass(GetSchoolClassRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getSchoolClass(GetSchoolClassRpcRequest.newBuilder().setSchoolClassId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void createUser_preserves_request_result_and_business_failure() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, converter, validation);
        var input = new CreateUserDTO("external", "Mario", "mario@example.com", "operator", "request");
        var expected = new UserDetailDTO(3L, "Mario", "mario@example.com", "ACTIVE");
        var request = converter.toTarget(input);
        when(facade.createUser(input)).thenReturn(expected);
        var response = provider.createUser(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).createUser(input);

        when(facade.createUser(input)).thenThrow(new UserFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.createUser(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).createUser(input);

        doReturn(null).when(facade).createUser(input);
        assertThatThrownBy(() -> provider.createUser(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).createUser(input);
        assertThatThrownBy(() -> provider.createUser(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createUser_rejects_missing_input_before_delegation() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.createUser(CreateUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void assignRole_preserves_request_result_and_business_failure() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, converter, validation);
        var input = new AssignRoleDTO(3L, "STUDENT", "operator", "request");
        var expected = new UserDetailDTO(3L, "Mario", "mario@example.com", "ACTIVE");
        var request = converter.toTarget(input);
        when(facade.assignRole(input)).thenReturn(expected);
        var response = provider.assignRole(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).assignRole(input);

        when(facade.assignRole(input)).thenThrow(new UserFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.assignRole(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).assignRole(input);

        doReturn(null).when(facade).assignRole(input);
        assertThatThrownBy(() -> provider.assignRole(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).assignRole(input);
        assertThatThrownBy(() -> provider.assignRole(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void assignRole_rejects_missing_input_before_delegation() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.assignRole(AssignRoleRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getUser_preserves_request_result_and_business_failure() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, converter, validation);
        var input = 3L;
        var expected = new UserDetailDTO(3L, "Mario", "mario@example.com", "ACTIVE");
        var request = GetUserRpcRequest.newBuilder().setUserId(input).build();
        when(facade.getUser(input)).thenReturn(expected);
        var response = provider.getUser(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).getUser(input);

        when(facade.getUser(input)).thenThrow(new UserFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.getUser(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).getUser(input);

        doReturn(null).when(facade).getUser(input);
        assertThatThrownBy(() -> provider.getUser(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).getUser(input);
        assertThatThrownBy(() -> provider.getUser(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getUser_rejects_missing_input_before_delegation() {
        var facade = mock(UserFacade.class);
        var provider = new UserRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.getUser(GetUserRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getUser(GetUserRpcRequest.newBuilder().setUserId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void grantPermission_preserves_request_result_and_business_failure() {
        var facade = mock(PermissionFacade.class);
        var provider = new PermissionRpcProvider(facade, converter, validation);
        var input = new GrantPermissionDTO("STUDENT", "course:read", "operator", "request");
        var expected = new PermissionDTO("STUDENT", "course:read", "GRANTED");
        var request = converter.toTarget(input);
        when(facade.grantPermission(input)).thenReturn(expected);
        var response = provider.grantPermission(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData())).isEqualTo(expected);
        verify(facade).grantPermission(input);

        when(facade.grantPermission(input)).thenThrow(new UserFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.grantPermission(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).grantPermission(input);

        doReturn(null).when(facade).grantPermission(input);
        assertThatThrownBy(() -> provider.grantPermission(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).grantPermission(input);
        assertThatThrownBy(() -> provider.grantPermission(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void grantPermission_rejects_missing_input_before_delegation() {
        var facade = mock(PermissionFacade.class);
        var provider = new PermissionRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.grantPermission(GrantPermissionRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void getUserPermissions_preserves_request_result_and_business_failure() {
        var facade = mock(PermissionFacade.class);
        var provider = new PermissionRpcProvider(facade, converter, validation);
        var input = 3L;
        var expected = List.of(new PermissionDetailDTO("course:read", "Read courses", List.of(new PermissionDetailDTO("course:detail", "Detail", List.of()))));
        var request = GetUserPermissionsRpcRequest.newBuilder().setUserId(input).build();
        when(facade.getUserPermissions(input)).thenReturn(expected);
        var response = provider.getUserPermissions(request);
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getDataList().stream().map(converter::toSource).toList()).isEqualTo(expected);
        verify(facade).getUserPermissions(input);

        when(facade.getUserPermissions(input)).thenThrow(new UserFacadeException("BUSINESS_REJECTED", "original reason"));
        var failure = provider.getUserPermissions(request);
        assertThat(failure.getSuccess()).isFalse();
        assertThat(failure.getCode()).isEqualTo("BUSINESS_REJECTED");
        assertThat(failure.getMessage()).isEqualTo("original reason");
        verify(facade, times(2)).getUserPermissions(input);

        doReturn(null).when(facade).getUserPermissions(input);
        assertThatThrownBy(() -> provider.getUserPermissions(request)).isInstanceOf(NullPointerException.class);
        doThrow(new IllegalStateException("unexpected")).when(facade).getUserPermissions(input);
        assertThatThrownBy(() -> provider.getUserPermissions(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getUserPermissions_rejects_missing_input_before_delegation() {
        var facade = mock(PermissionFacade.class);
        var provider = new PermissionRpcProvider(facade, converter, validation);
        assertThatThrownBy(() -> provider.getUserPermissions(GetUserPermissionsRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getUserPermissions(GetUserPermissionsRpcRequest.newBuilder().setUserId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
    }

    @Test
    void rejects_blank_strings_and_invalid_time_without_changing_default_validation_group() {
        var facade = mock(CourseFacade.class);
        var provider = new CourseRpcProvider(facade, converter, validation);
        var invalid = new CreateCourseDTO(" ", "Math", "operator", "request");
        assertThat(VALIDATORS.getValidator().validate(invalid)).isEmpty();
        assertThatThrownBy(() -> provider.createCourse(converter.toTarget(invalid)))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(facade);
        var schoolClass = mock(SchoolClassFacade.class);
        var schoolProvider = new SchoolClassRpcProvider(schoolClass, converter, validation);
        assertThatThrownBy(() -> schoolProvider.scheduleCourse(ScheduleCourseRpcRequest.newBuilder()
                .setStartsAt("invalid-time").build())).isInstanceOf(java.time.format.DateTimeParseException.class);
        verifyNoInteractions(schoolClass);
    }
}
