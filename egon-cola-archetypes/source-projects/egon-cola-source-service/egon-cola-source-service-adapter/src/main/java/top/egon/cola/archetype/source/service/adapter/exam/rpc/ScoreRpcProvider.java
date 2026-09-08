package top.egon.cola.archetype.source.service.adapter.exam.rpc;

import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.evaluation.facade.rpc.EvaluationRpcConverter;
import top.egon.cola.evaluation.facade.rpc.ScoreRpcService;
import top.egon.cola.evaluation.facade.exam.ScoreFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Objects;
import top.egon.cola.evaluation.facade.rpc.proto.RecordScoreRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.ScoreRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.GetScoreRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcResponse;

/** Adapts the existing Score facade to the native unary contract. */
@Component("scoreRpcProvider")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class ScoreRpcProvider implements ScoreRpcService {
    @Qualifier("scoreFacadeImpl")
    private final ScoreFacade delegate;
    @Qualifier("evaluationRpcConverter")
    private final EvaluationRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public ScoreRpcResponse recordScore(RecordScoreRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.recordScore(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("recordScore rejected: {}", result.getCode());
        }
        return converter.toScoreRpcResponse(result);
    }

    @Override
    public ScoreRpcResponse getScore(GetScoreRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.getScore(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("getScore rejected: {}", result.getCode());
        }
        return converter.toScoreRpcResponse(result);
    }

    @Override
    public PageScoreRpcResponse pageScores(PageScoreRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.pageScores(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("pageScores rejected: {}", result.getCode());
        }
        return converter.toPageScoreRpcResponse(result);
    }
}
