package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.pojo.convertor.ExamFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ExamFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.AttachExamPaperRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.DubboExamServiceTriple;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Exam;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PublishExamRequest;

/** Dubbo Triple provider of the open Exam facade; maps Protobuf onto the use cases. */
@DubboService(version = "1.0.0", group = "exam")
@RequiredArgsConstructor
@Slf4j
public class ExamFacadeImpl extends DubboExamServiceTriple.ExamServiceImplBase {

    @Qualifier("evaluationExamManage")
    private final ExamManage examManage;
    @Qualifier("examFacadeConverterImpl")
    private final ExamFacadeConverter converter;
    @Qualifier("examFacadeValidator")
    private final ExamFacadeValidator validator;
    @Qualifier("globalFacadeExceptionHandler")
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public Exam createExam(CreateExamRequest request) {
        try {
            var input = validator.validateCarrier(converter.toCommand(request));
            validator.require(request);
            return converter.toTarget(require(examManage.create(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public ExamPaper attachPaper(AttachExamPaperRequest request) {
        try {
            var input = validator.validateCarrier(converter.toCommand(request));
            return converter.toPaper(require(examManage.attachPaper(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public Exam publishExam(PublishExamRequest request) {
        try {
            var input = validator.validateCarrier(converter.toCommand(request));
            return converter.toTarget(require(examManage.publish(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public Exam getExam(GetExamRequest request) {
        try {
            var input = validator.validateCarrier(converter.toQuery(request));
            return converter.toTarget(require(examManage.get(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    private static <T> T require(T result) {
        return Objects.requireNonNull(result, "result");
    }
}
