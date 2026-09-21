package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.time.Instant;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.pojo.convertor.ExamFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ExamFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.CreateExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.result.ExamDetailResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Exam;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExamFacadeImplTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    private final ExamFacadeConverter converter = Mappers.getMapper(ExamFacadeConverter.class);
    private final ExamFacadeValidator validator =
            new ExamFacadeValidator(new ValidationUtils(VALIDATORS.getValidator()));

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    @Test
    void shouldValidateConvertDelegateAndReturnExam() {
        ExamManage manage = mock(ExamManage.class);
        CreateExamCommand command = new CreateExamCommand(
                1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(manage.create(command)).thenReturn(new ExamDetailResult(
                1002L, 1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));
        ExamFacadeImpl facade = new ExamFacadeImpl(
                manage, converter, validator, new GlobalFacadeExceptionHandler());

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
