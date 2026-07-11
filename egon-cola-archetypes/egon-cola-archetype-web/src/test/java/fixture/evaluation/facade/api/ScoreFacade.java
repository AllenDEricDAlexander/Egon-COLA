package fixture.evaluation.facade.api;

import fixture.evaluation.facade.dto.SingleResponse;
import fixture.evaluation.facade.dto.exam.GetScoreRequest;
import fixture.evaluation.facade.dto.exam.ScoreResponse;

public interface ScoreFacade {

    SingleResponse<ScoreResponse> getScore(GetScoreRequest request);
}
