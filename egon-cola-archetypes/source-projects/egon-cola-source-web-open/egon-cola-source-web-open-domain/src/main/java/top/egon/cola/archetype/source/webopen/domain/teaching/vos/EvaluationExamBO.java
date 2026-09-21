package top.egon.cola.archetype.source.webopen.domain.teaching.vos;

import java.time.Instant;

public record EvaluationExamBO(
        Long id,
        Long courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
