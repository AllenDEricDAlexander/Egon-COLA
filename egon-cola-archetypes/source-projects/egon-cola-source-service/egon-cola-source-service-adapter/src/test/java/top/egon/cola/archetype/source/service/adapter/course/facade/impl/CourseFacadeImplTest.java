package top.egon.cola.archetype.source.service.adapter.course.facade.impl;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.application.course.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.service.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.service.application.course.pojo.result.CourseResult;
import top.egon.cola.archetype.source.service.common.enums.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.common.exception.ApplicationException;
import top.egon.cola.archetype.source.service.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CourseFacadeImplTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    private final ValidationUtils validation = new ValidationUtils(VALIDATORS.getValidator());
    private final EvaluationFacadeConverter converter = Mappers.getMapper(EvaluationFacadeConverter.class);

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    private CourseFacadeImpl facade(CourseManage manage) {
        return new CourseFacadeImpl(manage, converter, validation, new GlobalFacadeExceptionHandler());
    }

    @Test
    void maps_protobuf_onto_the_command_and_the_result_back_onto_the_wire() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(new CreateCourseCommand("MATH-101", "Math", 3)))
                .thenReturn(new CourseResult(1001L, "MATH-101", "Math", 3, "ACTIVE"));

        var response = facade(manage).createCourse(CreateCourseRpcRequest.newBuilder()
                .setCode("MATH-101").setName("Math").setCredit(3).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1001L);
        assertThat(response.getData().getStatus()).isEqualTo("ACTIVE");
        verify(manage).create(new CreateCourseCommand("MATH-101", "Math", 3));
    }

    @Test
    void keeps_the_rejection_code_as_a_string_on_the_wire() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(any())).thenThrow(new ApplicationException(
                ApplicationErrorCode.COURSE_CODE_DUPLICATED, "duplicated code"));

        var response = facade(manage).createCourse(CreateCourseRpcRequest.newBuilder()
                .setCode("MATH-101").setName("Math").setCredit(3).build());

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("COURSE_CODE_DUPLICATED");
        assertThat(response.getMessage()).isEqualTo("duplicated code");
        assertThat(response.hasData()).isFalse();
    }

    @Test
    void rejects_an_invalid_request_before_the_use_case_runs() {
        CourseManage manage = mock(CourseManage.class);

        assertThatThrownBy(() -> facade(manage).createCourse(CreateCourseRpcRequest.getDefaultInstance()))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(manage);
    }
}
