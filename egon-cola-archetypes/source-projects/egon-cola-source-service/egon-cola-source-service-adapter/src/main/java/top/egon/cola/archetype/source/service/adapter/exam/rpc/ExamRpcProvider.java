package top.egon.cola.archetype.source.service.adapter.exam.rpc;

import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.evaluation.facade.rpc.EvaluationRpcConverter;
import top.egon.cola.evaluation.facade.rpc.ExamRpcService;
import top.egon.cola.evaluation.facade.exam.ExamFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Objects;
import top.egon.cola.evaluation.facade.rpc.proto.CreateExamRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.ExamRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.AttachExamPaperRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.ExamPaperRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.PublishExamRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.GetExamRpcRequest;

/** Adapts the existing Exam facade to the native unary contract. */
@Component("examRpcProvider")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class ExamRpcProvider implements ExamRpcService {
    @Qualifier("examFacadeImpl")
    private final ExamFacade delegate;
    @Qualifier("evaluationRpcConverter")
    private final EvaluationRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public ExamRpcResponse createExam(CreateExamRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.createExam(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("createExam rejected: {}", result.getCode());
        }
        return converter.toExamRpcResponse(result);
    }

    @Override
    public ExamPaperRpcResponse attachPaper(AttachExamPaperRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.attachPaper(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("attachPaper rejected: {}", result.getCode());
        }
        return converter.toExamPaperRpcResponse(result);
    }

    @Override
    public ExamRpcResponse publishExam(PublishExamRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.publishExam(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("publishExam rejected: {}", result.getCode());
        }
        return converter.toExamRpcResponse(result);
    }

    @Override
    public ExamRpcResponse getExam(GetExamRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.getExam(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("getExam rejected: {}", result.getCode());
        }
        return converter.toExamRpcResponse(result);
    }
}
