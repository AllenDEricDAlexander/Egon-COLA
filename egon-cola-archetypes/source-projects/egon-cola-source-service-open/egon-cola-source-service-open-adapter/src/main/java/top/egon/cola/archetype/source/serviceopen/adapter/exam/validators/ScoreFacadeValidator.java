package top.egon.cola.archetype.source.serviceopen.adapter.exam.validators;

import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoresRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.RecordScoreRequest;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ScoreFacadeValidator {

    public void require(RecordScoreRequest request) {
        requireRequest(request);
        requirePositive(request.getExamId(), "exam_id");
        requirePositive(request.getStudentId(), "student_id");
        if (request.getPoints() < 0 || request.getPoints() > 100) {
            throw new IllegalArgumentException("points must be between 0 and 100");
        }
    }

    public void require(GetScoreRequest request) {
        requireRequest(request);
        requirePositive(request.getExamId(), "exam_id");
        requirePositive(request.getScoreId(), "score_id");
    }

    public void require(PageScoresRequest request) {
        requireRequest(request);
        requirePositive(request.getExamId(), "exam_id");
        if (request.getCurrentPage() < 1 || request.getPageSize() < 1 || request.getPageSize() > 200) {
            throw new IllegalArgumentException("page bounds are invalid");
        }
    }

    private static void requireRequest(Object request) {
        Objects.requireNonNull(request, "request");
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
