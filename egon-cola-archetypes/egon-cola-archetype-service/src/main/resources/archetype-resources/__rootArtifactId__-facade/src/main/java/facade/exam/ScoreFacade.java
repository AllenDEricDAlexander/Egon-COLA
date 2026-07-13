#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.exam;

import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.exam.dto.GetScoreRequest;
import ${package}.facade.exam.dto.PageScoreRequest;
import ${package}.facade.exam.dto.RecordScoreRequest;
import ${package}.facade.exam.dto.ScoreResponse;

public interface ScoreFacade {

    SingleResponse<ScoreResponse> recordScore(RecordScoreRequest request);

    SingleResponse<ScoreResponse> getScore(GetScoreRequest request);

    SingleResponse<PageResponse<ScoreResponse>> pageScores(PageScoreRequest request);
}
