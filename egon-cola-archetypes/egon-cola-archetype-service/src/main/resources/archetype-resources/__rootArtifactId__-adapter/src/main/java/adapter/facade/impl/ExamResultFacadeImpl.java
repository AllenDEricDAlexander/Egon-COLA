#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl;

import ${package}.adapter.convertor.ExamResultAdapterConvertor;
import ${package}.adapter.handler.ServiceExceptionHandler;
import ${package}.application.manage.examing.ExamManage;
import ${package}.application.view.examing.ExamResultView;
import ${package}.common.exception.BizException;
import ${package}.common.response.SingleResponse;
import ${package}.facade.api.ExamResultFacade;
import ${package}.facade.dto.examing.ExamResultDTO;
import ${package}.facade.dto.examing.RecordExamResultRequest;
import org.springframework.stereotype.Component;

@Component
public class ExamResultFacadeImpl implements ExamResultFacade {

    private final ExamManage examManage;

    private final ExamResultAdapterConvertor examResultAdapterConvertor;

    private final ServiceExceptionHandler serviceExceptionHandler;

    public ExamResultFacadeImpl(ExamManage examManage, ExamResultAdapterConvertor examResultAdapterConvertor,
            ServiceExceptionHandler serviceExceptionHandler) {
        this.examManage = examManage;
        this.examResultAdapterConvertor = examResultAdapterConvertor;
        this.serviceExceptionHandler = serviceExceptionHandler;
    }

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
            ExamResultView examResultView = examManage.record(request.courseId(), request.studentId(), request.score());
            return SingleResponse.of(examResultAdapterConvertor.toDTO(examResultView));
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
