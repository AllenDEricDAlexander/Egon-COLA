package top.egon.fable.adapter.facade.impl;

import top.egon.fable.adapter.convertor.ExamResultAdapterConvertor;
import top.egon.fable.adapter.handler.ServiceExceptionHandler;
import top.egon.fable.application.manage.examing.ExamManage;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.facade.api.ExamResultFacade;
import top.egon.fable.facade.dto.SingleResponse;
import top.egon.fable.facade.dto.examing.ExamResultDTO;
import top.egon.fable.facade.dto.examing.RecordExamResultRequest;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(
        interfaceClass = ExamResultFacade.class,
        version = "1.0.0",
        group = "exam-result"
)
@RequiredArgsConstructor
public class ExamResultFacadeImpl implements ExamResultFacade {

    @Qualifier("examManage")
    private final ExamManage examManage;

    @Qualifier("examResultAdapterConvertor")
    private final ExamResultAdapterConvertor examResultAdapterConvertor;

    @Qualifier("serviceExceptionHandler")
    private final ServiceExceptionHandler serviceExceptionHandler;

    @Override
    public SingleResponse<ExamResultDTO> record(RecordExamResultRequest request) {
        if (request == null) {
            return serviceExceptionHandler.handleSingle(new BizException("record exam result request must not be null"));
        }
        if (request.courseId() == null || request.courseId().isBlank()) {
            return serviceExceptionHandler.handleSingle(new BizException("course id must not be blank"));
        }
        if (request.studentId() == null || request.studentId().isBlank()) {
            return serviceExceptionHandler.handleSingle(new BizException("student id must not be blank"));
        }
        if (request.score() < 0 || request.score() > 100) {
            return serviceExceptionHandler.handleSingle(new BizException("invalid exam result"));
        }
        try {
            ExamResult examResult = examManage.record(request.courseId(), request.studentId(), request.score());
            return SingleResponse.of(examResultAdapterConvertor.toDTO(examResult));
        } catch (BizException exception) {
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
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
