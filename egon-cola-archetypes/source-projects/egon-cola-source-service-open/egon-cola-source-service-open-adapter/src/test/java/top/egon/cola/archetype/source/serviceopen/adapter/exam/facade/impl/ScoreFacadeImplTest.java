package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import top.egon.cola.archetype.source.serviceopen.adapter.exam.converter.ScoreFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ScoreFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.RecordScoreCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.GetScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ScoreResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.RecordScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Score;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreFacadeImplTest {

    @Test
    void shouldValidateConvertDelegateAndReturnScore() {
        ScoreManage manage = mock(ScoreManage.class);
        RecordScoreCommand command = new RecordScoreCommand(1002L, 2001L, 92);
        when(manage.record(command)).thenReturn(new ScoreResult(
                1004L, 1002L, 1001L, 2001L, 92, "RECORDED"));
        ScoreFacadeImpl facade = new ScoreFacadeImpl(
                manage, new ScoreFacadeConverter(), new ScoreFacadeValidator(),
                new GlobalFacadeExceptionHandler(() -> 9001L));

        Score response = facade.recordScore(RecordScoreRequest.newBuilder()
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
        ScoreFacadeImpl facade = new ScoreFacadeImpl(
                manage, new ScoreFacadeConverter(), new ScoreFacadeValidator(),
                new GlobalFacadeExceptionHandler(() -> 9001L));

        Score response = facade.getScore(GetScoreRequest.newBuilder()
                .setExamId(1002L).setScoreId(1004L).build());

        assertEquals(1004L, response.getId());
        verify(manage).get(query);
    }
}
