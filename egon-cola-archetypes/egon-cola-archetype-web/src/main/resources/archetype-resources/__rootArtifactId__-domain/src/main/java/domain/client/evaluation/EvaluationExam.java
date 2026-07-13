package ${package}.domain.client.evaluation;

import java.time.Instant;

public record EvaluationExam(
        String id,
        String courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
