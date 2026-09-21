package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.pojo.dto.FacadeFailureDTO;
import top.egon.cola.archetype.source.service.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.service.facade.exam.ScoreFacade;
import top.egon.cola.archetype.source.service.facade.proto.GetScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.RecordScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ScoreRpcResponse;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

import java.util.Objects;

/** Native unary provider of the service-owned Score facade; maps Protobuf onto the use cases. */
@Component("scoreFacadeImpl")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class ScoreFacadeImpl implements ScoreFacade {

    @Qualifier("scoreManage")
    private final ScoreManage scoreManage;
    @Qualifier("evaluationFacadeConverter")
    private final EvaluationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;
    @Qualifier("globalFacadeExceptionHandler")
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public ScoreRpcResponse recordScore(RecordScoreRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.scoreSuccess(require(scoreManage.record(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("recordScore", failure);
            return converter.scoreFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public ScoreRpcResponse getScore(GetScoreRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.scoreSuccess(require(scoreManage.get(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("getScore", failure);
            return converter.scoreFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public PageScoreRpcResponse pageScores(PageScoreRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.pageScoreSuccess(require(scoreManage.page(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("pageScores", failure);
            return converter.pageScoreFailure(rejection.code(), rejection.message(), null);
        }
    }

    private static <T> T require(T result) {
        return Objects.requireNonNull(result, "facade returned null");
    }

    private FacadeFailureDTO reject(String operation, RuntimeException failure) {
        FacadeFailureDTO rejection = exceptionHandler.toFailure(failure);
        log.debug("{} rejected: {}", operation, rejection.code());
        return rejection;
    }
}
