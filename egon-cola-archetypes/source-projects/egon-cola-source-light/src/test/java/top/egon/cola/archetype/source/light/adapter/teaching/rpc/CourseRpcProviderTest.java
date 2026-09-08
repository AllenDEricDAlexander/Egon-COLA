package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.archetype.source.light.facade.rpc.proto.*;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;

import top.egon.cola.archetype.source.light.facade.teaching.CourseFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseRpcProviderTest {
    private final LightRpcConverter converter = Mappers.getMapper(LightRpcConverter.class);
    private final jakarta.validation.ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(validators.getValidator());

    @org.junit.jupiter.api.AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void delegates_to_course_facade() {
        CourseFacade facade = mock(CourseFacade.class);
        CourseDTO course = new CourseDTO(1002L, "MATH", "Math", "ACTIVE");
        when(facade.getCourse(1002L)).thenReturn(course);
        var result = new CourseRpcProvider(facade, converter, validation).getCourse(GetCourseRpcRequest.newBuilder().setCourseId(1002L).build());
        assertThat(result.getSuccess()).isTrue();
        assertThat(converter.toSource(result.getData())).isEqualTo(course);
    }
}
