package top.egon.cola.archetype.source.service.adapter.exam.converter;

import top.egon.cola.archetype.source.service.application.exam.command.RecordScoreCommand;
import top.egon.cola.archetype.source.service.application.exam.result.ScoreResult;
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
