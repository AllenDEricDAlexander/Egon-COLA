package fixture.evaluation.facade.api;

import fixture.evaluation.facade.dto.SingleResponse;
import fixture.evaluation.facade.dto.exam.ExamResponse;
import fixture.evaluation.facade.dto.exam.GetExamRequest;

public interface ExamFacade {

    SingleResponse<ExamResponse> getExam(GetExamRequest request);
}
