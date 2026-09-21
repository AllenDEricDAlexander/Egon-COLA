package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.pojo.dto.FacadeFailureDTO;
import top.egon.cola.archetype.source.service.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.service.facade.exam.ExamFacade;
import top.egon.cola.archetype.source.service.facade.proto.AttachExamPaperRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.CreateExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ExamPaperRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.GetExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PublishExamRpcRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

import java.util.Objects;

/** Native unary provider of the service-owned Exam facade; maps Protobuf onto the use cases. */
@Component("examFacadeImpl")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class ExamFacadeImpl implements ExamFacade {

    @Qualifier("evaluationExamManage")
    private final ExamManage examManage;
    @Qualifier("evaluationFacadeConverter")
    private final EvaluationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;
    @Qualifier("globalFacadeExceptionHandler")
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public ExamRpcResponse createExam(CreateExamRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.examSuccess(require(examManage.create(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("createExam", failure);
            return converter.examFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public ExamPaperRpcResponse attachPaper(AttachExamPaperRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.paperSuccess(require(examManage.attachPaper(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("attachPaper", failure);
            return converter.paperFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public ExamRpcResponse publishExam(PublishExamRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.examSuccess(require(examManage.publish(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("publishExam", failure);
            return converter.examFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public ExamRpcResponse getExam(GetExamRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.examSuccess(require(examManage.get(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("getExam", failure);
            return converter.examFailure(rejection.code(), rejection.message(), null);
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
