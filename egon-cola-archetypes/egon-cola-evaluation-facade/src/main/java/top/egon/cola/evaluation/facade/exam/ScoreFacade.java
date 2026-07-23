package top.egon.cola.evaluation.facade.exam;

import top.egon.cola.evaluation.facade.dto.PageResponse;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.PageScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.ScoreResponse;

public interface ScoreFacade {

    SingleResponse<ScoreResponse> recordScore(RecordScoreRequest request);

    SingleResponse<ScoreResponse> getScore(GetScoreRequest request);

    SingleResponse<PageResponse<ScoreResponse>> pageScores(PageScoreRequest request);
}
