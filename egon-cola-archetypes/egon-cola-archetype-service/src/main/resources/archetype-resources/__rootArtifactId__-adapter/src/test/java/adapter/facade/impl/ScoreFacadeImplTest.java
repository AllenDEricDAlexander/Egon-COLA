#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl;

import ${package}.adapter.converter.exam.ScoreFacadeConverter;
import ${package}.adapter.facade.impl.exam.ScoreFacadeImpl;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.validators.exam.ScoreFacadeValidator;
import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.manage.ScoreManage;
import ${package}.application.exam.result.ScoreResult;
import ${package}.facade.exam.dto.RecordScoreRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreFacadeImplTest {

    @Test
    void shouldValidateConvertDelegateAndReturnScore() {
        ScoreManage manage = mock(ScoreManage.class);
        RecordScoreCommand command = new RecordScoreCommand("exam-1", "student-1", 92);
        when(manage.record(command)).thenReturn(new ScoreResult(
                "score-1", "exam-1", "course-1", "student-1", 92, "RECORDED"));
        ScoreFacadeImpl facade = new ScoreFacadeImpl(
                manage, new ScoreFacadeConverter(), new ScoreFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        var response = facade.recordScore(new RecordScoreRequest("exam-1", "student-1", 92));

        assertTrue(response.isSuccess());
        assertEquals("score-1", response.getData().id());
        verify(manage).record(command);
    }
}
