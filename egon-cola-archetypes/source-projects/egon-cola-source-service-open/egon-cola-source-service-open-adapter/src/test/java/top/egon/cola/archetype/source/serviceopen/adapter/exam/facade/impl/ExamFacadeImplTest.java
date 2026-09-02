package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import top.egon.cola.archetype.source.serviceopen.adapter.exam.converter.ExamFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ExamFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.CreateExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ExamDetailResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Exam;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
                1002L, 1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));
        ExamFacadeImpl facade = new ExamFacadeImpl(
                manage, new ExamFacadeConverter(), new ExamFacadeValidator(),
                new GlobalFacadeExceptionHandler(() -> 9001L));

        Exam response = facade.createExam(CreateExamRequest.newBuilder()
                .setCourseId(1001L).setTitle("Midterm")
                .setStartsAt(timestamp(Instant.EPOCH))
                .setEndsAt(timestamp(Instant.EPOCH.plusSeconds(60))).build());

        assertEquals(1002L, response.getId());
        verify(manage).create(command);
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }
}
