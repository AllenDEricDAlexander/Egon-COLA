package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import top.egon.cola.archetype.source.serviceopen.adapter.exam.converter.ScoreFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ScoreFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.GetScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.PageScoreQuery;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoreResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoresRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.RecordScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Score;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.DubboScoreServiceTriple;
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
