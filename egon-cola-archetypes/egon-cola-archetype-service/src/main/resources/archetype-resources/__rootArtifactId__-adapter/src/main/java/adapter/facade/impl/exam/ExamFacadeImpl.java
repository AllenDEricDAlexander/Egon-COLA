#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl.exam;
import ${package}.adapter.converter.exam.ExamFacadeConverter;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.validators.exam.ExamFacadeValidator;
import ${package}.application.manage.exam.ExamManage;
import ${package}.application.query.exam.GetExamQuery;
import ${package}.facade.api.ExamFacade;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.exam.*;
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
