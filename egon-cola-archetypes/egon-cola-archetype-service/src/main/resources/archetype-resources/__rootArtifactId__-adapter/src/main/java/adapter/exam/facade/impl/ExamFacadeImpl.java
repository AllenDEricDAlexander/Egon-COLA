#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.facade.impl;
import ${package}.adapter.exam.converter.ExamFacadeConverter;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.exam.validators.ExamFacadeValidator;
import ${package}.application.exam.manage.ExamManage;
import ${package}.application.exam.query.GetExamQuery;
import top.egon.cola.evaluation.facade.exam.ExamFacade;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.exam.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
@DubboService(interfaceClass = ExamFacade.class, version = "1.0.0", group = "exam")
@RequiredArgsConstructor
public class ExamFacadeImpl implements ExamFacade {
    private final ExamManage examManage; private final ExamFacadeConverter converter;
    private final ExamFacadeValidator validator; private final GlobalFacadeExceptionHandler handler;
    public SingleResponse<ExamResponse> createExam(CreateExamRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.create(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ExamPaperResponse> attachPaper(AttachExamPaperRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.attachPaper(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ExamResponse> publishExam(PublishExamRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.publish(converter.toCommand(request)))); } catch (RuntimeException e) { return handler.toFailure(e); } }
    public SingleResponse<ExamResponse> getExam(GetExamRequest request) { try { validator.require(request); return SingleResponse.of(converter.toResponse(examManage.get(new GetExamQuery(request.examId())))); } catch (RuntimeException e) { return handler.toFailure(e); } }
}
