#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.course.facade.impl;

import ${package}.adapter.course.converter.CourseFacadeConverter;
import ${package}.adapter.course.facade.impl.CourseFacadeImpl;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.course.validators.CourseFacadeValidator;
import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.manage.CourseManage;
import ${package}.application.course.result.CourseResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseFacadeImplTest {

    @Test
    void shouldValidateConvertDelegateAndReturnCourse() {
        CourseManage manage = mock(CourseManage.class);
        CreateCourseCommand command = new CreateCourseCommand("MATH-101", "Math", 3);
        when(manage.create(command)).thenReturn(
                new CourseResult("course-1", "MATH-101", "Math", 3, "ACTIVE"));
        CourseFacadeImpl facade = new CourseFacadeImpl(
                manage, Mappers.getMapper(CourseFacadeConverter.class), new CourseFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.create(new CreateCourseRequest("MATH-101", "Math", 3));

        assertTrue(response.isSuccess());
        assertEquals("course-1", response.getData().id());
        verify(manage).create(command);
    }

    @Test
    void shouldFailWhenApplicationReturnsNull() {
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(any())).thenReturn(null);
        CourseFacadeImpl facade = new CourseFacadeImpl(
                manage, Mappers.getMapper(CourseFacadeConverter.class), new CourseFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.create(new CreateCourseRequest("MATH-101", "Math", 3));

        assertFalse(response.isSuccess());
        assertEquals("INTERNAL_ERROR", response.getCode());
    }
}
