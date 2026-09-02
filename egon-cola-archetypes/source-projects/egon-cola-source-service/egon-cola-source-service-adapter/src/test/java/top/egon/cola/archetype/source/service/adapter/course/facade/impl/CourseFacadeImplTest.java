package top.egon.cola.archetype.source.service.adapter.course.facade.impl;

import top.egon.cola.archetype.source.service.adapter.course.converter.CourseFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.course.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.course.validators.CourseFacadeValidator;
import top.egon.cola.archetype.source.service.application.course.command.CreateCourseCommand;
import top.egon.cola.archetype.source.service.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.service.application.course.result.CourseResult;
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
                new CourseResult(1001L, "MATH-101", "Math", 3, "ACTIVE"));
        CourseFacadeImpl facade = new CourseFacadeImpl(
                manage, Mappers.getMapper(CourseFacadeConverter.class), new CourseFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.create(new CreateCourseRequest("MATH-101", "Math", 3));

        assertTrue(response.isSuccess());
        assertEquals(1001L, response.getData().id());
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
