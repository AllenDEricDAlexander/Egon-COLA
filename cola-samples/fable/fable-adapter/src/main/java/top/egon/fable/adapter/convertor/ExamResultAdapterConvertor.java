package top.egon.fable.adapter.convertor;

import top.egon.fable.application.view.examing.ExamResultView;
import top.egon.fable.facade.dto.examing.ExamResultDTO;
import org.springframework.stereotype.Component;

@Component
public class ExamResultAdapterConvertor {

    public ExamResultDTO toDTO(ExamResultView examResultView) {
        return new ExamResultDTO(
                examResultView.id(),
                examResultView.courseId(),
                examResultView.studentId(),
                examResultView.score(),
                toFacadeStatus(examResultView.status(), examResultView.score()));
    }

    private String toFacadeStatus(String status, int score) {
        if ("RECORDED".equals(status)) {
            return score >= 60 ? "PASSED" : "FAILED";
        }
        return status;
    }
}
