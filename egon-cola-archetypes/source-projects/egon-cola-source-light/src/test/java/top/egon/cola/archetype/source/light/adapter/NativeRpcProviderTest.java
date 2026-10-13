package top.egon.cola.archetype.source.light.adapter;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.teaching.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.teaching.facade.impl.SchoolClassFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.user.facade.impl.PermissionFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.common.exception.EgonRpcRejectedException;
import top.egon.cola.archetype.source.light.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GrantPermissionRpcRequest;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Native unary surface: the facade implementations are the only Protobuf providers, they validate
 * before delegating, and they keep the group isolation and failure translation of the former provider.
 */
class NativeRpcProviderTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final LightFacadeConverter converter = Mappers.getMapper(LightFacadeConverter.class);

    @TempDir Path temporary;
    private NativeSecurityContractTest.SecurityFixture security;

    @BeforeEach void createVerifiedFixture() throws Exception { security = new NativeSecurityContractTest.SecurityFixture(temporary); }
    @AfterEach void closeVerifiedFixture() { security.close(); }

    private <T> T verifiedService(Supplier<T> work) {
        try {
            String token = security.serviceToken("1001", Set.of(NativeSecurityContractTest.SecurityFixture.SYSTEM+":course:create",
                    NativeSecurityContractTest.SecurityFixture.SYSTEM+":school-class:create", NativeSecurityContractTest.SecurityFixture.SYSTEM+":school-class:schedule-course"));
            var verification = security.verifier.verifyService(token);
            assertThat(verification).isInstanceOf(AccessTokenVerification.Valid.class);
            var verified = (AccessTokenVerification.Valid<?>) verification;
            var result = new AtomicReference<T>();
            var context = IdpRpcSecurityContext.with(verified.principal(), token, verified.proof(),
                    new IdpAuthorizationObservationContextBO(), verified.scopes());
            context.run(() -> IdpRpcSecurityContext.runWithSpringSecurity(
                    verified.principal(), verified.scopes(), verified.proof(), () -> result.set(work.get())));
            return result.get();
        } catch (RuntimeException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException("Controlled SERVICE fixture failed", failure); }
    }

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    @Test
    void conflictingPayloadActorsAreRejectedBeforeAnyOfTheSixWriteUseCases() {
        var courses = mock(CourseManage.class);
        var classes = mock(SchoolClassManage.class);
        var users = mock(UserManage.class);
        var roles = mock(RoleManage.class);
        var permissions = mock(PermissionManage.class);
        var course = new CourseFacadeImpl(courses, converter, validation);
        var schoolClass = new SchoolClassFacadeImpl(classes, converter, validation);
        var user = new UserFacadeImpl(users, roles, converter, validation);
        var permission = new PermissionFacadeImpl(permissions, converter, validation);
        for (Runnable write : java.util.List.<Runnable>of(
                () -> course.createCourse(CreateCourseRpcRequest.newBuilder().setOperatorId("forged").build()),
                () -> schoolClass.createSchoolClass(CreateSchoolClassRpcRequest.newBuilder().setOperatorId("forged").build()),
                () -> schoolClass.scheduleCourse(ScheduleCourseRpcRequest.newBuilder().setOperatorId("forged").build()),
                () -> user.createUser(CreateUserRpcRequest.newBuilder().setOperatorId("forged").build()),
                () -> user.assignRole(AssignRoleRpcRequest.newBuilder().setOperatorId("forged").build()),
                () -> permission.grantPermission(GrantPermissionRpcRequest.newBuilder().setOperatorId("forged").build()))) {
            assertThatThrownBy(() -> verifiedService(() -> { write.run(); return null; }))
                    .isInstanceOf(EgonRpcRejectedException.class).hasMessage("RPC actor conflicts with verified identity");
        }
        verifyNoInteractions(courses, classes, users, roles, permissions);
    }

    @Test
    void exports_all_native_contracts_with_named_beans() {
        for (Class<?> provider : new Class<?>[]{CourseFacadeImpl.class, SchoolClassFacadeImpl.class,
                UserFacadeImpl.class, PermissionFacadeImpl.class}) {
            assertThat(provider.isAnnotationPresent(EgonRpcProvider.class)).as(provider.getSimpleName()).isTrue();
            assertThat(provider.getAnnotation(Component.class).value()).isNotBlank();
            assertThat(provider.getInterfaces()).allMatch(type -> type.getSimpleName().endsWith("Facade"));
        }
    }

    @Test
    void validates_input_before_touching_the_use_case() {
        CourseManage courseManage = mock(CourseManage.class);
        CourseFacadeImpl provider = new CourseFacadeImpl(courseManage, converter, validation);

        assertThatThrownBy(() -> verifiedService(() -> provider.createCourse(CreateCourseRpcRequest.getDefaultInstance())))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getCourse(GetCourseRpcRequest.newBuilder().setCourseId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(courseManage);
    }

    @Test
    void native_group_rejects_blank_strings_that_the_default_group_still_accepts() {
        CreateCourseDTO blankCode = new CreateCourseDTO(" ", "Math", "light-service", "request");
        assertThat(VALIDATORS.getValidator().validate(blankCode)).isEmpty();

        CourseManage courseManage = mock(CourseManage.class);
        CourseFacadeImpl provider = new CourseFacadeImpl(courseManage, converter, validation);
        assertThatThrownBy(() -> verifiedService(() -> provider.createCourse(converter.toTarget(blankCode))))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(courseManage);
    }

    @Test
    void rejects_unparsable_time_before_any_delegation() {
        SchoolClassManage schoolClassManage = mock(SchoolClassManage.class);
        SchoolClassFacadeImpl provider = new SchoolClassFacadeImpl(schoolClassManage, converter, validation);

        assertThatThrownBy(() -> verifiedService(() -> provider.scheduleCourse(ScheduleCourseRpcRequest.newBuilder()
                .setStartsAt("invalid-time").build())))
                .isInstanceOf(DateTimeParseException.class);
        verifyNoInteractions(schoolClassManage);
    }

    @Test
    void missing_use_case_result_becomes_the_facade_empty_code() {
        CourseManage courseManage = mock(CourseManage.class);
        CourseFacadeImpl provider = new CourseFacadeImpl(courseManage, converter, validation);
        when(courseManage.create(any())).thenReturn(null);

        CourseRpcResponse response = verifiedService(() -> provider.createCourse(converter.toTarget(
                new CreateCourseDTO("MATH", "Math", "light-service", "request"))));

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("FACADE_EMPTY_RESULT");
    }

    @Test
    void unexpected_failure_is_not_translated_into_an_empty_response() {
        SchoolClassManage schoolClassManage = mock(SchoolClassManage.class);
        SchoolClassFacadeImpl provider = new SchoolClassFacadeImpl(schoolClassManage, converter, validation);
        when(schoolClassManage.create(any())).thenThrow(new IllegalStateException("unexpected"));

        assertThatThrownBy(() -> verifiedService(() -> provider.createSchoolClass(converter.toTarget(
                new CreateSchoolClassDTO(
                        "Class One", "2026-FALL", "light-service", "request")))))
                .isInstanceOf(IllegalStateException.class);
    }
}
