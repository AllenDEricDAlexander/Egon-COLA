#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.application.view.examing.ExamResultView;
import ${package}.facade.dto.examing.ExamResultDTO;
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
