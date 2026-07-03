#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl;

import ${package}.adapter.convertor.ExamResultAdapterConvertor;
import ${package}.adapter.handler.ServiceExceptionHandler;
import ${package}.adapter.validation.ValidatorUtils;
import ${package}.application.manage.examing.ExamManage;
import ${package}.common.exception.BizException;
import ${package}.domain.entities.examing.ExamResult;
import ${package}.facade.api.ExamResultFacade;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.examing.ExamResultDTO;
import ${package}.facade.dto.examing.RecordExamResultRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

@DubboService(
        interfaceClass = ExamResultFacade.class,
        version = "1.0.0",
        group = "exam-result"
)
@Validated
@RequiredArgsConstructor
public class ExamResultFacadeImpl implements ExamResultFacade {

    @Qualifier("examManage")
    private final ExamManage examManage;

    @Qualifier("examResultAdapterConvertor")
    private final ExamResultAdapterConvertor examResultAdapterConvertor;

    @Qualifier("serviceExceptionHandler")
    private final ServiceExceptionHandler serviceExceptionHandler;

    @Qualifier("validatorUtils")
    private final ValidatorUtils validatorUtils;

    @Override
    public SingleResponse<ExamResultDTO> record(RecordExamResultRequest request) {
        if (request == null) {
            return serviceExceptionHandler.handleSingle(new BizException("record exam result request must not be null"));
        }
        try {
            validatorUtils.validate(request);
            ExamResult examResult = examManage.record(request.courseId(), request.studentId(), request.score());
            return SingleResponse.of(examResultAdapterConvertor.toDTO(examResult));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (ValidationException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }

    @Override
    public SingleResponse<ExamResultDTO> getResult(String examResultId) {
        if (examResultId == null || examResultId.isBlank()) {
            return serviceExceptionHandler.handleSingle(new BizException("exam result id must not be blank"));
        }
        try {
            return SingleResponse.of(examResultAdapterConvertor.toDTO(examManage.getById(examResultId)));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (ValidationException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
