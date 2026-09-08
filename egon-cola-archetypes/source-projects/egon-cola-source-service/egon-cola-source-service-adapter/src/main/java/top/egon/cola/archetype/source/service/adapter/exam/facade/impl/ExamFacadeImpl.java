package top.egon.cola.archetype.source.service.adapter.exam.facade.impl;
import top.egon.cola.archetype.source.service.adapter.exam.converter.ExamFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.exam.validators.ExamFacadeValidator;
import top.egon.cola.archetype.source.service.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.service.application.exam.query.GetExamQuery;
import top.egon.cola.evaluation.facade.exam.ExamFacade;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.exam.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
@Component("examFacadeImpl")
@Slf4j
@RequiredArgsConstructor
public class ExamFacadeImpl implements ExamFacade {
    @Qualifier("evaluationExamManage") private final ExamManage examManage; @Qualifier("examFacadeConverterImpl") private final ExamFacadeConverter converter;
    @Qualifier("examFacadeValidator") private final ExamFacadeValidator validator; @Qualifier("globalFacadeExceptionHandler") private final GlobalFacadeExceptionHandler handler;
    public SingleResponse<ExamResponse> createExam(CreateExamRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.create(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ExamPaperResponse> attachPaper(AttachExamPaperRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.attachPaper(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ExamResponse> publishExam(PublishExamRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.publish(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ExamResponse> getExam(GetExamRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.get(new GetExamQuery(request.examId())))); } catch (RuntimeException e) { return handler.toFailure(e); } }
}
