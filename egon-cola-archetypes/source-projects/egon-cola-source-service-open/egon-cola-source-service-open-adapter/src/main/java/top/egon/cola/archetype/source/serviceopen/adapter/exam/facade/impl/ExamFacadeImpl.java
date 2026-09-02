package top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl;

import top.egon.cola.archetype.source.serviceopen.adapter.exam.converter.ExamFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ExamFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.GetExamQuery;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.AttachExamPaperRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Exam;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.DubboExamServiceTriple;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PublishExamRequest;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0", group = "exam")
@RequiredArgsConstructor
public class ExamFacadeImpl extends DubboExamServiceTriple.ExamServiceImplBase {

    private final ExamManage examManage;
    private final ExamFacadeConverter converter;
    private final ExamFacadeValidator validator;
    private final GlobalFacadeExceptionHandler handler;

    @Override
    public Exam createExam(CreateExamRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(examManage.create(converter.toCommand(request)));
        } catch (RuntimeException failure) {
            throw handler.toStatus(failure);
        }
    }

    @Override
    public ExamPaper attachPaper(AttachExamPaperRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(examManage.attachPaper(converter.toCommand(request)));
        } catch (RuntimeException failure) {
            throw handler.toStatus(failure);
        }
    }

    @Override
    public Exam publishExam(PublishExamRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(examManage.publish(converter.toCommand(request)));
        } catch (RuntimeException failure) {
            throw handler.toStatus(failure);
        }
    }

    @Override
    public Exam getExam(GetExamRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(examManage.get(new GetExamQuery(request.getExamId())));
        } catch (RuntimeException failure) {
            throw handler.toStatus(failure);
        }
    }
}
