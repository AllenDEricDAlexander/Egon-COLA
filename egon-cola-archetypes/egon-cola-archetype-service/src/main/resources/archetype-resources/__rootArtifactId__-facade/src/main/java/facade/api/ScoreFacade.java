#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.api;

import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.exam.GetScoreRequest;
import ${package}.facade.dto.exam.PageScoreRequest;
import ${package}.facade.dto.exam.RecordScoreRequest;
import ${package}.facade.dto.exam.ScoreResponse;

public interface ScoreFacade {

    SingleResponse<ScoreResponse> recordScore(RecordScoreRequest request);

    SingleResponse<ScoreResponse> getScore(GetScoreRequest request);

    SingleResponse<PageResponse<ScoreResponse>> pageScores(PageScoreRequest request);
}
