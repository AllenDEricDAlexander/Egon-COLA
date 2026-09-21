package top.egon.cola.archetype.source.serviceopen.adapter.exam.pojo.convertor;

import java.util.Objects;
import org.mapstruct.AnnotateWith;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.RecordScoreCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query.GetScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query.PageScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.result.ScoreResult;
import top.egon.cola.archetype.source.serviceopen.application.pojo.result.PageResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoreResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoresRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.RecordScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Score;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/** Maps the evaluation Score wire onto the score Commands, Queries and Results. */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        componentModel = "spring")
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "scoreFacadeConverterImpl"))
public interface ScoreFacadeConverter extends BaseForwardConverter<ScoreResult, Score> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    @Mapping(target = "status", source = "status")
    Score toTarget(ScoreResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    RecordScoreCommand toCommand(RecordScoreRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "scoreId", source = "scoreId")
    GetScoreQuery toQuery(GetScoreRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    PageScoreQuery toQuery(PageScoresRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    PageScoreResponse toPage(PageResult<ScoreResult> source);

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendRecords(
            PageResult<ScoreResult> source, @MappingTarget PageScoreResponse.Builder target) {
        target.addAllRecords(Objects.requireNonNull(source.records(), "page records must not be null")
                .stream().map(this::toTarget).toList());
    }
}
