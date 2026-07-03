#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.mq;

import ${package}.adapter.convertor.ExamResultAdapterConvertor;
import ${package}.adapter.dto.ExamResultMessage;
import ${package}.adapter.handler.ServiceExceptionHandler;
import ${package}.application.manage.examing.ExamManage;
import ${package}.common.exception.BizException;
import ${package}.domain.entities.examing.ExamResult;
import ${package}.facade.dto.examing.ExamResultDTO;
import ${package}.facade.dto.SingleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("examResultMessageConsumer")
@RequiredArgsConstructor
public class ExamResultMessageConsumer {

    @Qualifier("examManage")
    private final ExamManage examManage;

    @Qualifier("examResultAdapterConvertor")
    private final ExamResultAdapterConvertor examResultAdapterConvertor;

    @Qualifier("serviceExceptionHandler")
    private final ServiceExceptionHandler serviceExceptionHandler;

    public SingleResponse<ExamResultDTO> consume(ExamResultMessage message) {
        if (message == null) {
            return serviceExceptionHandler.handleSingle(new BizException("exam result message must not be null"));
        }
        if (message.courseId() == null || message.courseId().isBlank()) {
            return serviceExceptionHandler.handleSingle(new BizException("course id must not be blank"));
        }
        if (message.studentId() == null || message.studentId().isBlank()) {
            return serviceExceptionHandler.handleSingle(new BizException("student id must not be blank"));
        }
        if (message.score() < 0 || message.score() > 100) {
            return serviceExceptionHandler.handleSingle(new BizException("invalid exam result"));
        }
        try {
            ExamResult examResult = examManage.record(message.courseId(), message.studentId(), message.score());
            return SingleResponse.of(examResultAdapterConvertor.toDTO(examResult));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
