package fixture.evaluation.facade.exam;

import fixture.evaluation.facade.dto.SingleResponse;
import fixture.evaluation.facade.exam.dto.GetScoreRequest;
import fixture.evaluation.facade.exam.dto.ScoreResponse;

public interface ScoreFacade {

    SingleResponse<ScoreResponse> getScore(GetScoreRequest request);
}
