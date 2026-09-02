package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;

import top.egon.cola.archetype.source.service.adapter.exam.converter.ScoreFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.exam.facade.impl.ScoreFacadeImpl;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.exam.validators.ScoreFacadeValidator;
import top.egon.cola.archetype.source.service.application.exam.command.RecordScoreCommand;
import top.egon.cola.archetype.source.service.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.service.application.exam.query.GetScoreQuery;
import top.egon.cola.archetype.source.service.application.exam.result.ScoreResult;
import top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreFacadeImplTest {

    @Test
    void shouldValidateConvertDelegateAndReturnScore() {
        ScoreManage manage = mock(ScoreManage.class);
        RecordScoreCommand command = new RecordScoreCommand(4001L, 6001L, 92);
        when(manage.record(command)).thenReturn(new ScoreResult(
                7001L, 4001L, 1001L, 6001L, 92, "RECORDED"));
        ScoreFacadeImpl facade = new ScoreFacadeImpl(
                manage, Mappers.getMapper(ScoreFacadeConverter.class), new ScoreFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.recordScore(new RecordScoreRequest(4001L, 6001L, 92));

        assertTrue(response.isSuccess());
        assertEquals(7001L, response.getData().id());
        verify(manage).record(command);
    }

    @Test
    void shouldDelegateExamAwareScoreQuery() {
        ScoreManage manage = mock(ScoreManage.class);
        GetScoreQuery query = new GetScoreQuery(4001L, 7001L);
        when(manage.get(query)).thenReturn(new ScoreResult(
            7001L, 4001L, 1001L, 6001L, 92, "RECORDED"));
        ScoreFacadeImpl facade = new ScoreFacadeImpl(
            manage, Mappers.getMapper(ScoreFacadeConverter.class), new ScoreFacadeValidator(),
            new GlobalFacadeExceptionHandler());

        var response = facade.getScore(new GetScoreRequest(4001L, 7001L));

        assertTrue(response.isSuccess());
        verify(manage).get(query);
    }
}
