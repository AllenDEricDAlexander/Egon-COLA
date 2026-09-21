package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.pojo.convertor.ScoreFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ScoreFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.DubboScoreServiceTriple;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoreResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoresRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.RecordScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Score;

/** Dubbo Triple provider of the open Score facade; maps Protobuf onto the use cases. */
@DubboService(version = "1.0.0", group = "score")
@RequiredArgsConstructor
@Slf4j
public class ScoreFacadeImpl extends DubboScoreServiceTriple.ScoreServiceImplBase {

    @Qualifier("scoreManage")
    private final ScoreManage scoreManage;
    @Qualifier("scoreFacadeConverterImpl")
    private final ScoreFacadeConverter converter;
    @Qualifier("scoreFacadeValidator")
    private final ScoreFacadeValidator validator;
    @Qualifier("globalFacadeExceptionHandler")
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public Score recordScore(RecordScoreRequest request) {
        try {
            var input = validator.validateCarrier(converter.toCommand(request));
            return converter.toTarget(require(scoreManage.record(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public Score getScore(GetScoreRequest request) {
        try {
            var input = validator.validateCarrier(converter.toQuery(request));
            return converter.toTarget(require(scoreManage.get(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public PageScoreResponse pageScores(PageScoresRequest request) {
        try {
            var input = validator.validateCarrier(converter.toQuery(request));
            return converter.toPage(require(scoreManage.page(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    private static <T> T require(T result) {
        return Objects.requireNonNull(result, "result");
    }
}
