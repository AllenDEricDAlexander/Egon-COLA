package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;

import top.egon.cola.archetype.source.service.adapter.exam.converter.ExamFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.exam.facade.impl.ExamFacadeImpl;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.exam.validators.ExamFacadeValidator;
import top.egon.cola.archetype.source.service.application.exam.command.CreateExamCommand;
import top.egon.cola.archetype.source.service.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.service.application.exam.result.ExamDetailResult;
import top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

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
                1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(manage.create(command)).thenReturn(new ExamDetailResult(
                4001L, 1001L, "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));
        ExamFacadeImpl facade = new ExamFacadeImpl(
                manage, Mappers.getMapper(ExamFacadeConverter.class), new ExamFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.createExam(new CreateExamRequest(
                1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));

        assertTrue(response.isSuccess());
        assertEquals(4001L, response.getData().id());
        verify(manage).create(command);
    }
}
