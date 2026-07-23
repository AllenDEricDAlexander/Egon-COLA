#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.converter;

import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.result.ScoreResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.ScoreResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ScoreFacadeConverter {

    RecordScoreCommand toCommand(RecordScoreRequest request);

    ScoreResponse toResponse(ScoreResult result);
}
