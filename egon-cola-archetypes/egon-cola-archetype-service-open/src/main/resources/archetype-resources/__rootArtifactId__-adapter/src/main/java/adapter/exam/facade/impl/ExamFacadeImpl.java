#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.facade.impl;

import ${package}.adapter.exam.converter.ExamFacadeConverter;
import ${package}.adapter.exam.validators.ExamFacadeValidator;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.application.exam.manage.ExamManage;
import ${package}.application.exam.query.GetExamQuery;
import ${package}.facade.evaluation.v1.AttachExamPaperRequest;
import ${package}.facade.evaluation.v1.CreateExamRequest;
import ${package}.facade.evaluation.v1.Exam;
import ${package}.facade.evaluation.v1.ExamPaper;
import ${package}.facade.evaluation.v1.DubboExamServiceTriple;
import ${package}.facade.evaluation.v1.GetExamRequest;
import ${package}.facade.evaluation.v1.PublishExamRequest;
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
