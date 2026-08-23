#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.facade.impl;

import ${package}.adapter.exam.converter.ScoreFacadeConverter;
import ${package}.adapter.exam.validators.ScoreFacadeValidator;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.application.exam.manage.ScoreManage;
import ${package}.application.exam.query.GetScoreQuery;
import ${package}.application.exam.query.PageScoreQuery;
import ${package}.facade.evaluation.v1.GetScoreRequest;
import ${package}.facade.evaluation.v1.PageScoreResponse;
import ${package}.facade.evaluation.v1.PageScoresRequest;
import ${package}.facade.evaluation.v1.RecordScoreRequest;
import ${package}.facade.evaluation.v1.Score;
import ${package}.facade.evaluation.v1.DubboScoreServiceTriple;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0", group = "score")
@RequiredArgsConstructor
public class ScoreFacadeImpl extends DubboScoreServiceTriple.ScoreServiceImplBase {

    private final ScoreManage scoreManage;
    private final ScoreFacadeConverter converter;
    private final ScoreFacadeValidator validator;
    private final GlobalFacadeExceptionHandler handler;

    @Override
    public Score recordScore(RecordScoreRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(scoreManage.record(converter.toCommand(request)));
        } catch (RuntimeException failure) {
            throw handler.toStatus(failure);
        }
    }

    @Override
    public Score getScore(GetScoreRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(scoreManage.get(
                    new GetScoreQuery(request.getExamId(), request.getScoreId())));
        } catch (RuntimeException failure) {
            throw handler.toStatus(failure);
        }
    }

    @Override
    public PageScoreResponse pageScores(PageScoresRequest request) {
        try {
            validator.require(request);
            return converter.toPage(scoreManage.page(
                    new PageScoreQuery(request.getExamId(), request.getCurrentPage(), request.getPageSize())));
        } catch (RuntimeException failure) {
            throw handler.toStatus(failure);
        }
    }
}
