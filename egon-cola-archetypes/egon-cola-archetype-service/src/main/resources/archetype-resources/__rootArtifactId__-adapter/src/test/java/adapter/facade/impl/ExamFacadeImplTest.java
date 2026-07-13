#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl;

import ${package}.adapter.converter.exam.ExamFacadeConverter;
import ${package}.adapter.facade.impl.exam.ExamFacadeImpl;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.validators.exam.ExamFacadeValidator;
import ${package}.application.command.exam.CreateExamCommand;
import ${package}.application.manage.exam.ExamManage;
import ${package}.application.result.exam.ExamDetailResult;
import ${package}.facade.exam.dto.CreateExamRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamFacadeImplTest {

    @Test
    void shouldValidateConvertDelegateAndReturnExam() {
        ExamManage manage = mock(ExamManage.class);
        CreateExamCommand command = new CreateExamCommand(
                "course-1", "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(manage.create(command)).thenReturn(new ExamDetailResult(
                "exam-1", "course-1", "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));
        ExamFacadeImpl facade = new ExamFacadeImpl(
                manage, new ExamFacadeConverter(), new ExamFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.createExam(new CreateExamRequest(
                "course-1", "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertTrue(response.isSuccess());
        assertEquals("exam-1", response.getData().id());
        verify(manage).create(command);
    }
}
