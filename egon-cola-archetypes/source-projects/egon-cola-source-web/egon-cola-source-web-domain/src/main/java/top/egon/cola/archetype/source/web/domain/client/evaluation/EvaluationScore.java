package top.egon.cola.archetype.source.web.domain.client.evaluation;

public record EvaluationScore(
        Long id,
        Long examId,
        Long courseId,
        Long studentId,
        int points,
        String status) {
}
