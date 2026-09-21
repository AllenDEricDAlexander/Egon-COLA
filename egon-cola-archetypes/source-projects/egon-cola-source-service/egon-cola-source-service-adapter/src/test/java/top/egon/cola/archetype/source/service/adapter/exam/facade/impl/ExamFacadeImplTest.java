package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.application.exam.command.CreateExamCommand;
import top.egon.cola.archetype.source.service.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.service.application.exam.result.ExamDetailResult;
import top.egon.cola.archetype.source.service.facade.proto.CreateExamRpcRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamFacadeImplTest {

    private final EvaluationFacadeConverter converter = Mappers.getMapper(EvaluationFacadeConverter.class);

    @Test
    void shouldConvertDelegateAndReturnExam() {
        ExamManage manage = mock(ExamManage.class);
        CreateExamCommand command = new CreateExamCommand(
                1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(manage.create(command)).thenReturn(new ExamDetailResult(
                4001L, 1001L, "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));
        ExamFacadeImpl facade = new ExamFacadeImpl(manage, converter,
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()),
                new GlobalFacadeExceptionHandler());

        var response = facade.createExam(CreateExamRpcRequest.newBuilder()
                .setCourseId(1001L).setTitle("Midterm")
                .setStartsAt(Instant.EPOCH.toString()).setEndsAt(Instant.EPOCH.plusSeconds(60).toString())
                .build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(4001L);
        assertThat(response.getData().getStartsAt()).isEqualTo(Instant.EPOCH.toString());
        verify(manage).create(command);
    }
}
