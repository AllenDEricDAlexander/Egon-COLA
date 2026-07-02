package top.egon.fable.adapter.mq;

import top.egon.fable.adapter.convertor.ExamResultAdapterConvertor;
import top.egon.fable.adapter.dto.ExamResultMessage;
import top.egon.fable.adapter.handler.ServiceExceptionHandler;
import top.egon.fable.application.manage.examing.ExamManage;
import top.egon.fable.application.view.examing.ExamResultView;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.common.response.SingleResponse;
import top.egon.fable.facade.dto.examing.ExamResultDTO;
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
            ExamResultView examResultView = examManage.record(message.courseId(), message.studentId(), message.score());
            return SingleResponse.of(examResultAdapterConvertor.toDTO(examResultView));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
