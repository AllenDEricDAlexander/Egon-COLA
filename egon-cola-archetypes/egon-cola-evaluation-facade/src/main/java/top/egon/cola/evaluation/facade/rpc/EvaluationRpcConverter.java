package top.egon.cola.evaluation.facade.rpc;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.time.Instant;
import java.util.Objects;

/** Maps native Protobuf fields while preserving the existing facade DTO contract. */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface EvaluationRpcConverter extends BaseConverter<
        top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest,
        top.egon.cola.evaluation.facade.rpc.proto.CreateCourseRpcRequest> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    top.egon.cola.evaluation.facade.rpc.proto.CreateCourseRpcRequest toTarget(top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.CreateCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    top.egon.cola.evaluation.facade.rpc.proto.ScheduleCourseRpcRequest toTarget(top.egon.cola.evaluation.facade.course.dto.ScheduleCourseRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    top.egon.cola.evaluation.facade.course.dto.ScheduleCourseRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.ScheduleCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    top.egon.cola.evaluation.facade.rpc.proto.GetCourseRpcRequest toTarget(top.egon.cola.evaluation.facade.course.dto.GetCourseRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    top.egon.cola.evaluation.facade.course.dto.GetCourseRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.GetCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcRequest toTarget(top.egon.cola.evaluation.facade.course.dto.PageCourseRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    top.egon.cola.evaluation.facade.course.dto.PageCourseRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    top.egon.cola.evaluation.facade.rpc.proto.CreateExamRpcRequest toTarget(top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.CreateExamRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    top.egon.cola.evaluation.facade.rpc.proto.AttachExamPaperRpcRequest toTarget(top.egon.cola.evaluation.facade.exam.dto.AttachExamPaperRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    top.egon.cola.evaluation.facade.exam.dto.AttachExamPaperRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.AttachExamPaperRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    top.egon.cola.evaluation.facade.rpc.proto.PublishExamRpcRequest toTarget(top.egon.cola.evaluation.facade.exam.dto.PublishExamRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    top.egon.cola.evaluation.facade.exam.dto.PublishExamRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.PublishExamRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    top.egon.cola.evaluation.facade.rpc.proto.GetExamRpcRequest toTarget(top.egon.cola.evaluation.facade.exam.dto.GetExamRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    top.egon.cola.evaluation.facade.exam.dto.GetExamRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.GetExamRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    top.egon.cola.evaluation.facade.rpc.proto.RecordScoreRpcRequest toTarget(top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    top.egon.cola.evaluation.facade.exam.dto.RecordScoreRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.RecordScoreRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "scoreId", source = "scoreId")
    top.egon.cola.evaluation.facade.rpc.proto.GetScoreRpcRequest toTarget(top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "scoreId", source = "scoreId")
    top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.GetScoreRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcRequest toTarget(top.egon.cola.evaluation.facade.exam.dto.PageScoreRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    top.egon.cola.evaluation.facade.exam.dto.PageScoreRequest toSource(top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.rpc.proto.CourseResponse toTarget(top.egon.cola.evaluation.facade.course.dto.CourseResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.course.dto.CourseResponse toSource(top.egon.cola.evaluation.facade.rpc.proto.CourseResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.rpc.proto.CourseScheduleResponse toTarget(top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse toSource(top.egon.cola.evaluation.facade.rpc.proto.CourseScheduleResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.rpc.proto.ExamResponse toTarget(top.egon.cola.evaluation.facade.exam.dto.ExamResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.exam.dto.ExamResponse toSource(top.egon.cola.evaluation.facade.rpc.proto.ExamResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.rpc.proto.ExamPaperResponse toTarget(top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse toSource(top.egon.cola.evaluation.facade.rpc.proto.ExamPaperResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.rpc.proto.ScoreResponse toTarget(top.egon.cola.evaluation.facade.exam.dto.ScoreResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points")
    @Mapping(target = "status", source = "status")
    top.egon.cola.evaluation.facade.exam.dto.ScoreResponse toSource(top.egon.cola.evaluation.facade.rpc.proto.ScoreResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    top.egon.cola.evaluation.facade.rpc.proto.PageCourseResponse toPageCourseResponse(top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.course.dto.CourseResponse> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "records", source = "recordsList")
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.course.dto.CourseResponse> fromPageCourseResponse(top.egon.cola.evaluation.facade.rpc.proto.PageCourseResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    top.egon.cola.evaluation.facade.rpc.proto.PageScoreResponse toPageScoreResponse(top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.exam.dto.ScoreResponse> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "records", source = "recordsList")
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.exam.dto.ScoreResponse> fromPageScoreResponse(top.egon.cola.evaluation.facade.rpc.proto.PageScoreResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.rpc.proto.CourseRpcResponse toCourseRpcResponse(top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.course.dto.CourseResponse> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.course.dto.CourseResponse> fromCourseRpcResponse(top.egon.cola.evaluation.facade.rpc.proto.CourseRpcResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.rpc.proto.CourseScheduleRpcResponse toCourseScheduleRpcResponse(top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse> fromCourseScheduleRpcResponse(top.egon.cola.evaluation.facade.rpc.proto.CourseScheduleRpcResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.rpc.proto.ExamRpcResponse toExamRpcResponse(top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.exam.dto.ExamResponse> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.exam.dto.ExamResponse> fromExamRpcResponse(top.egon.cola.evaluation.facade.rpc.proto.ExamRpcResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.rpc.proto.ExamPaperRpcResponse toExamPaperRpcResponse(top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse> fromExamPaperRpcResponse(top.egon.cola.evaluation.facade.rpc.proto.ExamPaperRpcResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.rpc.proto.ScoreRpcResponse toScoreRpcResponse(top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.exam.dto.ScoreResponse> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.exam.dto.ScoreResponse> fromScoreRpcResponse(top.egon.cola.evaluation.facade.rpc.proto.ScoreRpcResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcResponse toPageCourseRpcResponse(top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.course.dto.CourseResponse>> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.course.dto.CourseResponse>> fromPageCourseRpcResponse(top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcResponse toPageScoreRpcResponse(top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.exam.dto.ScoreResponse>> source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "data", source = "data")
    top.egon.cola.evaluation.facade.dto.SingleResponse<top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.exam.dto.ScoreResponse>> fromPageScoreRpcResponse(top.egon.cola.evaluation.facade.rpc.proto.PageScoreRpcResponse source);

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPageCourseResponseCollections(
            top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.course.dto.CourseResponse> source,
            @MappingTarget top.egon.cola.evaluation.facade.rpc.proto.PageCourseResponse.Builder target) {
        target.addAllRecords(Objects.requireNonNull(source.records(), "page records must not be null").stream().map(this::toTarget).toList());
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPageScoreResponseCollections(
            top.egon.cola.evaluation.facade.dto.PageResponse<top.egon.cola.evaluation.facade.exam.dto.ScoreResponse> source,
            @MappingTarget top.egon.cola.evaluation.facade.rpc.proto.PageScoreResponse.Builder target) {
        target.addAllRecords(Objects.requireNonNull(source.records(), "page records must not be null").stream().map(this::toTarget).toList());
    }

    default String toProtoTime(Instant value) {
        return value == null ? null : value.toString();
    }

    default Instant fromProtoTime(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
