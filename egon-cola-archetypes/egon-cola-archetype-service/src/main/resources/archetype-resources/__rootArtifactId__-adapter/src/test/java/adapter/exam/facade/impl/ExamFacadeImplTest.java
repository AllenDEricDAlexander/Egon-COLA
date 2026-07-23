#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.facade.impl;

import ${package}.adapter.exam.converter.ExamFacadeConverter;
import ${package}.adapter.exam.facade.impl.ExamFacadeImpl;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.exam.validators.ExamFacadeValidator;
import ${package}.application.exam.command.CreateExamCommand;
import ${package}.application.exam.manage.ExamManage;
import ${package}.application.exam.result.ExamDetailResult;
import top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest;
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
