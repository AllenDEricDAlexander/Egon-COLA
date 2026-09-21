package top.egon.cola.archetype.source.serviceopen.adapter.course.facade.impl;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.serviceopen.adapter.course.pojo.convertor.CourseFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.course.validators.CourseFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.result.CourseResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Course;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateCourseRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CourseFacadeImplTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    private final CourseFacadeConverter converter = Mappers.getMapper(CourseFacadeConverter.class);
    private final CourseFacadeValidator validator =
            new CourseFacadeValidator(new ValidationUtils(VALIDATORS.getValidator()));

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    private CourseFacadeImpl facade(CourseManage manage) {
        return new CourseFacadeImpl(manage, converter, validator, new GlobalFacadeExceptionHandler());
    }

    @Test
    void shouldValidateConvertDelegateAndReturnCourse() {
        CourseManage manage = mock(CourseManage.class);
        CreateCourseCommand command = new CreateCourseCommand("MATH-101", "Math", 3);
        when(manage.create(command)).thenReturn(new CourseResult(1001L, "MATH-101", "Math", 3, "ACTIVE"));

        Course response = facade(manage).createCourse(CreateCourseRequest.newBuilder()
                .setCode("MATH-101").setName("Math").setCredit(3).build());

        assertEquals(1001L, response.getId());
        verify(manage).create(command);
    }

    @Test
    void shouldMapNullApplicationResultToInternalStatus() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(any())).thenReturn(null);

        StatusRuntimeException failure = assertThrows(StatusRuntimeException.class,
                () -> facade(manage).createCourse(CreateCourseRequest.newBuilder()
                        .setCode("MATH-101").setName("Math").setCredit(3).build()));

        assertEquals(Status.Code.INTERNAL, failure.getStatus().getCode());
    }

    @Test
    void shouldRejectAnInvalidCarrierBeforeTheUseCaseRuns() {
        CourseManage manage = mock(CourseManage.class);

        StatusRuntimeException failure = assertThrows(StatusRuntimeException.class,
                () -> facade(manage).createCourse(CreateCourseRequest.getDefaultInstance()));

        assertEquals(Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
        verifyNoInteractions(manage);
    }
}
