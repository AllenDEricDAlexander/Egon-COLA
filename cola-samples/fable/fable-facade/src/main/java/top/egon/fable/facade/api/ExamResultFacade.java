package top.egon.fable.facade.api;

import top.egon.fable.facade.dto.SingleResponse;
import top.egon.fable.facade.dto.examing.ExamResultDTO;
import top.egon.fable.facade.dto.examing.RecordExamResultRequest;

public interface ExamResultFacade {

    SingleResponse<ExamResultDTO> record(RecordExamResultRequest request);

    SingleResponse<ExamResultDTO> getResult(String examResultId);
}
