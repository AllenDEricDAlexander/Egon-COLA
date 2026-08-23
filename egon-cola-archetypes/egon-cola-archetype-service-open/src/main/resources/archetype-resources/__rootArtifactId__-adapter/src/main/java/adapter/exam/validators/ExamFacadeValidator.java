#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.validators;

import ${package}.facade.evaluation.v1.AttachExamPaperRequest;
import ${package}.facade.evaluation.v1.CreateExamRequest;
import ${package}.facade.evaluation.v1.GetExamRequest;
import ${package}.facade.evaluation.v1.PublishExamRequest;
import com.google.protobuf.Timestamp;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ExamFacadeValidator {

    public void require(CreateExamRequest request) {
        requireRequest(request);
        requirePositive(request.getCourseId(), "course_id");
        requireText(request.getTitle(), "title", 128);
        requireWindow(request.getStartsAt(), request.getEndsAt());
    }

    public void require(AttachExamPaperRequest request) {
        requireRequest(request);
        requirePositive(request.getExamId(), "exam_id");
        requireText(request.getTitle(), "title", 128);
        if (request.getTotalPoints() <= 0) {
            throw new IllegalArgumentException("total_points must be positive");
        }
    }

    public void require(PublishExamRequest request) {
        requireRequest(request);
        requirePositive(request.getExamId(), "exam_id");
    }

    public void require(GetExamRequest request) {
        requireRequest(request);
        requirePositive(request.getExamId(), "exam_id");
    }

    private static void requireRequest(Object request) {
        Objects.requireNonNull(request, "request");
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
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
