package fixture.evaluation.facade.exam;

import fixture.evaluation.facade.dto.SingleResponse;
import fixture.evaluation.facade.exam.dto.ExamResponse;
import fixture.evaluation.facade.exam.dto.GetExamRequest;

public interface ExamFacade {

    SingleResponse<ExamResponse> getExam(GetExamRequest request);
}
