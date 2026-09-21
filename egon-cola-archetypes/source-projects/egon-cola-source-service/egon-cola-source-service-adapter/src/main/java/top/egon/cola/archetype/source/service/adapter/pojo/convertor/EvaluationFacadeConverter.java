package top.egon.cola.archetype.source.service.adapter.pojo.convertor;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValueMappingStrategy;
import top.egon.cola.archetype.source.service.application.course.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.service.application.course.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.service.application.course.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.service.application.course.pojo.query.PageCourseQuery;
import top.egon.cola.archetype.source.service.application.course.pojo.result.CourseResult;
import top.egon.cola.archetype.source.service.application.course.pojo.result.CourseScheduleResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.command.AttachExamPaperCommand;
import top.egon.cola.archetype.source.service.application.exam.pojo.command.CreateExamCommand;
import top.egon.cola.archetype.source.service.application.exam.pojo.command.PublishExamCommand;
import top.egon.cola.archetype.source.service.application.exam.pojo.command.RecordScoreCommand;
import top.egon.cola.archetype.source.service.application.exam.pojo.query.GetExamQuery;
import top.egon.cola.archetype.source.service.application.exam.pojo.query.GetScoreQuery;
import top.egon.cola.archetype.source.service.application.exam.pojo.query.PageScoreQuery;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamDetailResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamPaperResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ScoreResult;
import top.egon.cola.archetype.source.service.application.pojo.result.PageResult;
import top.egon.cola.archetype.source.service.facade.proto.AttachExamPaperRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.CourseResponse;
import top.egon.cola.archetype.source.service.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.CourseScheduleResponse;
import top.egon.cola.archetype.source.service.facade.proto.CourseScheduleRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.CreateExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ExamPaperResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamPaperRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamResponse;
import top.egon.cola.archetype.source.service.facade.proto.ExamRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseResponse;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreResponse;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageScoreRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.PublishExamRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.RecordScoreRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.ScoreResponse;
import top.egon.cola.archetype.source.service.facade.proto.ScoreRpcResponse;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.time.Instant;
import java.util.Objects;

/**
 * Maps the service-owned Protobuf wire onto the application Commands, Queries and Results.
 *
 * <p>The envelope constants are the values previously produced by the retired transport DTOs, so a
 * successful call keeps reporting {@code SUCCESS}/{@code success} and a business rejection keeps
 * carrying its string code instead of an integer wire status.</p>
 */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface EvaluationFacadeConverter extends BaseConverter<CreateCourseCommand, CreateCourseRpcRequest> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    CreateCourseRpcRequest toTarget(CreateCourseCommand source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    CreateCourseCommand toSource(CreateCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    ScheduleCourseCommand toSource(ScheduleCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    GetCourseQuery toSource(GetCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    PageCourseQuery toSource(PageCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    CreateExamCommand toSource(CreateExamRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    AttachExamPaperCommand toSource(AttachExamPaperRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    PublishExamCommand toSource(PublishExamRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    GetExamQuery toSource(GetExamRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    RecordScoreCommand toSource(RecordScoreRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "scoreId", source = "scoreId")
    GetScoreQuery toSource(GetScoreRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    PageScoreQuery toSource(PageScoreRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    @Mapping(target = "status", source = "status")
    CourseResponse toCourseResponse(CourseResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    CourseScheduleResponse toCourseScheduleResponse(CourseScheduleResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    ExamResponse toExamResponse(ExamDetailResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    @Mapping(target = "status", source = "status")
    ExamPaperResponse toExamPaperResponse(ExamPaperResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    @Mapping(target = "status", source = "status")
    ScoreResponse toScoreResponse(ScoreResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    PageCourseResponse toPageCourseResponse(PageResult<CourseResult> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    PageScoreResponse toPageScoreResponse(PageResult<ScoreResult> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    CourseRpcResponse courseSuccess(CourseResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    CourseRpcResponse courseFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    CourseScheduleRpcResponse courseScheduleSuccess(CourseScheduleResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    CourseScheduleRpcResponse courseScheduleFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    PageCourseRpcResponse pageCourseSuccess(PageResult<CourseResult> source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    PageCourseRpcResponse pageCourseFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    ExamRpcResponse examSuccess(ExamDetailResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    ExamRpcResponse examFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    ExamPaperRpcResponse paperSuccess(ExamPaperResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    ExamPaperRpcResponse paperFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    ScoreRpcResponse scoreSuccess(ScoreResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    ScoreRpcResponse scoreFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    PageScoreRpcResponse pageScoreSuccess(PageResult<ScoreResult> source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    PageScoreRpcResponse pageScoreFailure(String code, String message, String traceId);

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPageCourseRecords(
            PageResult<CourseResult> source,
            @MappingTarget PageCourseResponse.Builder target) {
        target.addAllRecords(Objects.requireNonNull(source.records(), "page records must not be null")
                .stream().map(this::toCourseResponse).toList());
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPageScoreRecords(
            PageResult<ScoreResult> source,
            @MappingTarget PageScoreResponse.Builder target) {
        target.addAllRecords(Objects.requireNonNull(source.records(), "page records must not be null")
                .stream().map(this::toScoreResponse).toList());
    }

    default String toProtoTime(Instant value) {
        return value == null ? null : value.toString();
    }

    default Instant fromProtoTime(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
