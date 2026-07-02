#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.mq;

import ${package}.adapter.convertor.ExamResultAdapterConvertor;
import ${package}.adapter.dto.ExamResultMessage;
import ${package}.adapter.handler.ServiceExceptionHandler;
import ${package}.application.manage.examing.ExamManage;
import ${package}.application.view.examing.ExamResultView;
import ${package}.common.exception.BizException;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.examing.ExamResultDTO;
import org.springframework.stereotype.Component;

@Component
public class ExamResultMessageConsumer {

    private final ExamManage examManage;

    private final ExamResultAdapterConvertor examResultAdapterConvertor;

    private final ServiceExceptionHandler serviceExceptionHandler;

    public ExamResultMessageConsumer(ExamManage examManage, ExamResultAdapterConvertor examResultAdapterConvertor,
            ServiceExceptionHandler serviceExceptionHandler) {
        this.examManage = examManage;
        this.examResultAdapterConvertor = examResultAdapterConvertor;
        this.serviceExceptionHandler = serviceExceptionHandler;
    }

    public SingleResponse<ExamResultDTO> consume(ExamResultMessage message) {
        if (message == null) {
            return serviceExceptionHandler.handleSingle(new BizException("exam result message must not be null"));
        }
        try {
            ExamResultView examResultView = examManage.record(message.courseId(), message.studentId(), message.score());
            return SingleResponse.of(examResultAdapterConvertor.toDTO(examResultView));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
