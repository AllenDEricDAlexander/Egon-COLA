#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.course.facade.impl;

import ${package}.adapter.course.converter.CourseFacadeConverter;
import ${package}.adapter.course.validators.CourseFacadeValidator;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.manage.CourseManage;
import ${package}.application.course.result.CourseResult;
import ${package}.facade.evaluation.v1.Course;
import ${package}.facade.evaluation.v1.CreateCourseRequest;
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
