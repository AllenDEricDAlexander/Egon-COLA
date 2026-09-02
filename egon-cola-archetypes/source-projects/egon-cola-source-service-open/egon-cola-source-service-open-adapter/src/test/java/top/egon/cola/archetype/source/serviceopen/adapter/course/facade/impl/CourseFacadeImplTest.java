package top.egon.cola.archetype.source.serviceopen.adapter.course.facade.impl;

import top.egon.cola.archetype.source.serviceopen.adapter.course.converter.CourseFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.course.validators.CourseFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.course.command.CreateCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.serviceopen.application.course.result.CourseResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Course;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateCourseRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseFacadeImplTest {

    @Test
    void shouldValidateConvertDelegateAndReturnCourse() {
        CourseManage manage = mock(CourseManage.class);
        CreateCourseCommand command = new CreateCourseCommand("MATH-101", "Math", 3);
        when(manage.create(command)).thenReturn(new CourseResult(1001L, "MATH-101", "Math", 3, "ACTIVE"));
        CourseFacadeImpl facade = new CourseFacadeImpl(
                manage, new CourseFacadeConverter(), new CourseFacadeValidator(),
                new GlobalFacadeExceptionHandler(() -> 9001L));

        Course response = facade.createCourse(CreateCourseRequest.newBuilder()
                .setCode("MATH-101").setName("Math").setCredit(3).build());

        assertEquals(1001L, response.getId());
        verify(manage).create(command);
    }

    @Test
    void shouldMapNullApplicationResultToInternalStatus() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(any())).thenReturn(null);
        CourseFacadeImpl facade = new CourseFacadeImpl(
                manage, new CourseFacadeConverter(), new CourseFacadeValidator(),
                new GlobalFacadeExceptionHandler(() -> 9001L));

        StatusRuntimeException failure = assertThrows(StatusRuntimeException.class,
                () -> facade.createCourse(CreateCourseRequest.newBuilder()
                        .setCode("MATH-101").setName("Math").setCredit(3).build()));

        assertEquals(Status.Code.INTERNAL, failure.getStatus().getCode());
    }
}
