#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.course.converter;

import ${package}.application.course.command.ScheduleCourseCommand;
import ${package}.application.course.result.CourseResult;
import ${package}.application.course.result.CourseScheduleResult;
import ${package}.application.result.PageResult;
import ${package}.facade.evaluation.v1.Course;
import ${package}.facade.evaluation.v1.CourseSchedule;
import ${package}.facade.evaluation.v1.PageCourseResponse;
import ${package}.facade.evaluation.v1.ScheduleCourseRequest;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CourseFacadeConverter {

    public ScheduleCourseCommand toCommand(ScheduleCourseRequest request) {
        Objects.requireNonNull(request, "request");
        return new ScheduleCourseCommand(
                request.getCourseId(), request.getClassId(),
                toInstant(request.getStartsAt()), toInstant(request.getEndsAt()));
    }

    public CourseResult requireCourseResult(CourseResult result) {
        return Objects.requireNonNull(result, "result");
    }

    public Course toResponse(CourseResult result) {
        requireCourseResult(result);
        return Course.newBuilder()
                .setId(result.id())
                .setCode(result.code())
                .setName(result.name())
                .setCredit(result.credit())
                .setStatus(result.status())
                .build();
    }

    public CourseSchedule toResponse(CourseScheduleResult result) {
        Objects.requireNonNull(result, "result");
        return CourseSchedule.newBuilder()
                .setId(result.id())
                .setCourseId(result.courseId())
                .setClassId(result.classId())
                .setStartsAt(toTimestamp(result.startsAt()))
                .setEndsAt(toTimestamp(result.endsAt()))
                .setStatus(result.status())
                .build();
    }

    public PageCourseResponse toPage(PageResult<CourseResult> page) {
        Objects.requireNonNull(page, "page");
        PageCourseResponse.Builder builder = PageCourseResponse.newBuilder()
                .setCurrentPage(page.currentPage())
                .setTotalPages(page.totalPages())
                .setPageSize(page.pageSize())
                .setTotalCount(page.totalCount());
        page.records().stream().map(this::toResponse).forEach(builder::addRecords);
        return builder.build();
    }

    private static Instant toInstant(Timestamp timestamp) {
        Objects.requireNonNull(timestamp, "timestamp");
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private static Timestamp toTimestamp(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
