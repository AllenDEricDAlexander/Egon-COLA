#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.manage;

import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.query.GetScoreQuery;
import ${package}.application.exam.query.PageScoreQuery;
import ${package}.application.exam.result.ScoreResult;
import ${package}.application.result.PageResult;

public interface ScoreManage {
    ScoreResult record(RecordScoreCommand command);
    ScoreResult get(GetScoreQuery query);
    PageResult<ScoreResult> page(PageScoreQuery query);
}
