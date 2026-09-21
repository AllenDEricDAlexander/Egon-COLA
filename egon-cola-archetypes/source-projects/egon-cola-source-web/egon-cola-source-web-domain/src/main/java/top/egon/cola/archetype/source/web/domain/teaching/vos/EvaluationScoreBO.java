package top.egon.cola.archetype.source.web.domain.teaching.vos;

public record EvaluationScoreBO(
        Long id,
        Long examId,
        Long courseId,
        Long studentId,
        int points,
        String status) {
}
