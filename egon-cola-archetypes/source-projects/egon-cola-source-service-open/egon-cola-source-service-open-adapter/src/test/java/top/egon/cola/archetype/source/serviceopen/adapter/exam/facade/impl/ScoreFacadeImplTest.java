package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.pojo.convertor.ScoreFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ScoreFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.RecordScoreCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query.GetScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.result.ScoreResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.RecordScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Score;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreFacadeImplTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    private final ScoreFacadeConverter converter = Mappers.getMapper(ScoreFacadeConverter.class);
    private final ScoreFacadeValidator validator =
            new ScoreFacadeValidator(new ValidationUtils(VALIDATORS.getValidator()));

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    private ScoreFacadeImpl facade(ScoreManage manage) {
        return new ScoreFacadeImpl(manage, converter, validator, new GlobalFacadeExceptionHandler());
    }

    @Test
    void shouldValidateConvertDelegateAndReturnScore() {
        ScoreManage manage = mock(ScoreManage.class);
        RecordScoreCommand command = new RecordScoreCommand(1002L, 2001L, 92);
        when(manage.record(command)).thenReturn(new ScoreResult(
                1004L, 1002L, 1001L, 2001L, 92, "RECORDED"));

        Score response = facade(manage).recordScore(RecordScoreRequest.newBuilder()
                .setExamId(1002L).setStudentId(2001L).setPoints(92).build());

        assertEquals(1004L, response.getId());
        verify(manage).record(command);
    }

    @Test
    void shouldDelegateExamAwareScoreQuery() {
        ScoreManage manage = mock(ScoreManage.class);
        GetScoreQuery query = new GetScoreQuery(1002L, 1004L);
        when(manage.get(query)).thenReturn(new ScoreResult(
                1004L, 1002L, 1001L, 2001L, 92, "RECORDED"));

        Score response = facade(manage).getScore(GetScoreRequest.newBuilder()
                .setExamId(1002L).setScoreId(1004L).build());

        assertEquals(1004L, response.getId());
        verify(manage).get(query);
    }
}
