package top.egon.cola.archetype.source.web.domain.client.evaluation;

import java.time.Instant;

public record EvaluationExam(
        Long id,
        Long courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
