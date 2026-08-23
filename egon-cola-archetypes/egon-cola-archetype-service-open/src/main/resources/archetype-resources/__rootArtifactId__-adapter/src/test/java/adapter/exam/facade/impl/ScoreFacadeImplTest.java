#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.facade.impl;

import ${package}.adapter.exam.converter.ScoreFacadeConverter;
import ${package}.adapter.exam.validators.ScoreFacadeValidator;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.manage.ScoreManage;
import ${package}.application.exam.query.GetScoreQuery;
import ${package}.application.exam.result.ScoreResult;
import ${package}.facade.evaluation.v1.GetScoreRequest;
import ${package}.facade.evaluation.v1.RecordScoreRequest;
import ${package}.facade.evaluation.v1.Score;
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
