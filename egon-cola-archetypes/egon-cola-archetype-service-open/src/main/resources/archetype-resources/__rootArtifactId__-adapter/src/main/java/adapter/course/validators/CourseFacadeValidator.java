#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.course.validators;

import ${package}.facade.evaluation.v1.CreateCourseRequest;
import ${package}.facade.evaluation.v1.GetCourseRequest;
import ${package}.facade.evaluation.v1.PageCoursesRequest;
import ${package}.facade.evaluation.v1.ScheduleCourseRequest;
import com.google.protobuf.Timestamp;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CourseFacadeValidator {

    public void require(CreateCourseRequest request) {
        requireRequest(request);
        requireText(request.getCode(), "code");
        requireText(request.getName(), "name");
        if (request.getCode().length() > 96 || request.getName().length() > 64 || request.getCredit() <= 0) {
            throw new IllegalArgumentException("course fields are invalid");
        }
    }

    public void require(ScheduleCourseRequest request) {
        requireRequest(request);
        requirePositive(request.getCourseId(), "course_id");
        requirePositive(request.getClassId(), "class_id");
        requireWindow(request.getStartsAt(), request.getEndsAt());
    }

    public void require(GetCourseRequest request) {
        requireRequest(request);
        requirePositive(request.getCourseId(), "course_id");
    }

    public void require(PageCoursesRequest request) {
        requireRequest(request);
        if (request.getCurrentPage() < 1 || request.getPageSize() < 1 || request.getPageSize() > 200) {
            throw new IllegalArgumentException("page bounds are invalid");
        }
    }

    private static void requireRequest(Object request) {
        Objects.requireNonNull(request, "request");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireWindow(Timestamp startsAt, Timestamp endsAt) {
        Objects.requireNonNull(startsAt, "starts_at");
        Objects.requireNonNull(endsAt, "ends_at");
        if (startsAt.getSeconds() > endsAt.getSeconds()
                || (startsAt.getSeconds() == endsAt.getSeconds()
                && startsAt.getNanos() >= endsAt.getNanos())) {
            throw new IllegalArgumentException("starts_at must be before ends_at");
        }
    }
}
