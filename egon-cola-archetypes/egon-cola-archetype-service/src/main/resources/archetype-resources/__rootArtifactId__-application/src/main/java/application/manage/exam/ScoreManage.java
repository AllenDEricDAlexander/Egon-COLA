#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.exam;

import ${package}.application.command.exam.RecordScoreCommand;
import ${package}.application.query.exam.GetScoreQuery;
import ${package}.application.query.exam.PageScoreQuery;
import ${package}.application.result.exam.ScoreResult;
import ${package}.domain.common.Page;

public interface ScoreManage {
    ScoreResult record(RecordScoreCommand command);
    ScoreResult get(GetScoreQuery query);
    Page<ScoreResult> page(PageScoreQuery query);
}
