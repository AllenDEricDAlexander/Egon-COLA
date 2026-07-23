package top.egon.cola.evaluation.facade.exam;

import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.exam.dto.AttachExamPaperRequest;
import top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest;
import top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse;
import top.egon.cola.evaluation.facade.exam.dto.ExamResponse;
import top.egon.cola.evaluation.facade.exam.dto.GetExamRequest;
import top.egon.cola.evaluation.facade.exam.dto.PublishExamRequest;

public interface ExamFacade {

    SingleResponse<ExamResponse> createExam(CreateExamRequest request);

    SingleResponse<ExamPaperResponse> attachPaper(AttachExamPaperRequest request);

    SingleResponse<ExamResponse> publishExam(PublishExamRequest request);

    SingleResponse<ExamResponse> getExam(GetExamRequest request);
}
