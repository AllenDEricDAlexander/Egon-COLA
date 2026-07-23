#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.converter;

import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.result.ScoreResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.ScoreResponse;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ScoreFacadeConverter {

    RecordScoreCommand toCommand(RecordScoreRequest request);

    ScoreResponse toResponse(ScoreResult result);

    @BeforeMapping
    default void requireRequest(RecordScoreRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(ScoreResult result) {
        Objects.requireNonNull(result, "result");
    }
}
