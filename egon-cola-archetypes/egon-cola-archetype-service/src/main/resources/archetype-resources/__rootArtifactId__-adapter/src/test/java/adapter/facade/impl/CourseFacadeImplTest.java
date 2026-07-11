#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl;

import ${package}.adapter.converter.course.CourseFacadeConverter;
import ${package}.adapter.facade.impl.course.CourseFacadeImpl;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.validators.course.CourseFacadeValidator;
import ${package}.application.command.course.CreateCourseCommand;
import ${package}.application.manage.course.CourseManage;
import ${package}.application.result.course.CourseResult;
import ${package}.facade.dto.course.CreateCourseRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                manage, new CourseFacadeConverter(), new CourseFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.create(new CreateCourseRequest("MATH-101", "Math", 3));

        assertTrue(response.isSuccess());
        assertEquals("course-1", response.getData().id());
        verify(manage).create(command);
    }
}
