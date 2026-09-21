package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.archetype.source.service.facade.proto.CourseResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamResponse;
import top.egon.cola.archetype.source.service.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ScoreResponse;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationCourseBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationExamBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationScoreBO;
import top.egon.cola.component.common.core.converter.BaseConverter;

/**
 * Consumer-owned projection of the peer service facade wire.
 *
 * <p>Only the fields the existing evaluation query port declares may cross this boundary, so a
 * provider payload that carries more than the port owns stays invisible to the web domain.</p>
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EvaluationQueryConverter extends BaseConverter<CourseResponse, EvaluationCourseBO> {

    /** Validates scalar identifiers after Protobuf presence has been decoded. */
    record CourseQuery(@NotNull @Positive Long courseId) {
    }

    /** Validates scalar identifiers after Protobuf presence has been decoded. */
    record ExamQuery(@NotNull @Positive Long examId) {
    }

    /** Validates scalar identifiers after Protobuf presence has been decoded. */
    record ScoreQuery(@NotNull @Positive Long examId, @NotNull @Positive Long scoreId) {
    }

    @Override
    EvaluationCourseBO toTarget(CourseResponse source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    @Mapping(target = "status", source = "status")
    CourseResponse toSource(EvaluationCourseBO target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    EvaluationExamBO toTarget(ExamResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    ExamResponse toSource(EvaluationExamBO target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    @Mapping(target = "status", source = "status")
    EvaluationScoreBO toTarget(ScoreResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    @Mapping(target = "status", source = "status")
    ScoreResponse toSource(EvaluationScoreBO target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    GetCourseRpcRequest courseRequest(CourseQuery source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    GetExamRpcRequest examRequest(ExamQuery source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "scoreId", source = "scoreId")
    GetScoreRpcRequest scoreRequest(ScoreQuery source);

    /** The wire freezes instants as ISO_INSTANT text; absent stays null. */
    default String toProtoTime(Instant value) {
        return value == null ? null : value.toString();
    }

    /** The wire freezes instants as ISO_INSTANT text; absent stays null. */
    default Instant fromProtoTime(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
