package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.application.exam.command.RecordScoreCommand;
import top.egon.cola.archetype.source.service.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.service.application.exam.query.GetScoreQuery;
import top.egon.cola.archetype.source.service.application.exam.result.ScoreResult;
import top.egon.cola.archetype.source.service.facade.proto.GetScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.RecordScoreRpcRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreFacadeImplTest {

    private final EvaluationFacadeConverter converter = Mappers.getMapper(EvaluationFacadeConverter.class);

    private ScoreFacadeImpl facade(ScoreManage manage) {
        return new ScoreFacadeImpl(manage, converter,
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()),
                new GlobalFacadeExceptionHandler());
    }

    @Test
    void shouldConvertDelegateAndReturnScore() {
        ScoreManage manage = mock(ScoreManage.class);
        RecordScoreCommand command = new RecordScoreCommand(4001L, 6001L, 92);
        when(manage.record(command)).thenReturn(new ScoreResult(
                7001L, 4001L, 1001L, 6001L, 92, "RECORDED"));

        var response = facade(manage).recordScore(RecordScoreRpcRequest.newBuilder()
                .setExamId(4001L).setStudentId(6001L).setPoints(92).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(7001L);
        verify(manage).record(command);
    }

    @Test
    void shouldDelegateExamAwareScoreQuery() {
        ScoreManage manage = mock(ScoreManage.class);
        GetScoreQuery query = new GetScoreQuery(4001L, 7001L);
        when(manage.get(query)).thenReturn(new ScoreResult(
                7001L, 4001L, 1001L, 6001L, 92, "RECORDED"));

        var response = facade(manage).getScore(GetScoreRpcRequest.newBuilder()
                .setExamId(4001L).setScoreId(7001L).build());

        assertThat(response.getSuccess()).isTrue();
        verify(manage).get(query);
    }
}
