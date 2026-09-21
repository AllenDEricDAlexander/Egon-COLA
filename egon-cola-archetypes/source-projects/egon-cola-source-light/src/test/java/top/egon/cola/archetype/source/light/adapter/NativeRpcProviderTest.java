package top.egon.cola.archetype.source.light.adapter;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.teaching.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.teaching.facade.impl.SchoolClassFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.user.facade.impl.PermissionFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

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

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
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

        assertThatThrownBy(() -> provider.createCourse(CreateCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> provider.getCourse(GetCourseRpcRequest.newBuilder().setCourseId(0).build()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(courseManage);
    }

    @Test
    void native_group_rejects_blank_strings_that_the_default_group_still_accepts() {
        CreateCourseDTO blankCode = new CreateCourseDTO(" ", "Math", "operator", "request");
        assertThat(VALIDATORS.getValidator().validate(blankCode)).isEmpty();

        CourseManage courseManage = mock(CourseManage.class);
        CourseFacadeImpl provider = new CourseFacadeImpl(courseManage, converter, validation);
        assertThatThrownBy(() -> provider.createCourse(converter.toTarget(blankCode)))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(courseManage);
    }

    @Test
    void rejects_unparsable_time_before_any_delegation() {
        SchoolClassManage schoolClassManage = mock(SchoolClassManage.class);
        SchoolClassFacadeImpl provider = new SchoolClassFacadeImpl(schoolClassManage, converter, validation);

        assertThatThrownBy(() -> provider.scheduleCourse(ScheduleCourseRpcRequest.newBuilder()
                .setStartsAt("invalid-time").build()))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
        verifyNoInteractions(schoolClassManage);
    }

    @Test
    void missing_use_case_result_becomes_the_facade_empty_code() {
        CourseManage courseManage = mock(CourseManage.class);
        CourseFacadeImpl provider = new CourseFacadeImpl(courseManage, converter, validation);
        when(courseManage.create(any())).thenReturn(null);

        CourseRpcResponse response = provider.createCourse(converter.toTarget(
                new CreateCourseDTO("MATH", "Math", "operator", "request")));

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("FACADE_EMPTY_RESULT");
    }

    @Test
    void unexpected_failure_is_not_translated_into_an_empty_response() {
        SchoolClassManage schoolClassManage = mock(SchoolClassManage.class);
        SchoolClassFacadeImpl provider = new SchoolClassFacadeImpl(schoolClassManage, converter, validation);
        when(schoolClassManage.create(any())).thenThrow(new IllegalStateException("unexpected"));

        assertThatThrownBy(() -> provider.createSchoolClass(converter.toTarget(
                new top.egon.cola.archetype.source.light.facade.teaching.dto.CreateSchoolClassDTO(
                        "Class One", "2026-FALL", "operator", "request"))))
                .isInstanceOf(IllegalStateException.class);
    }
}
