package top.egon.cola.archetype.source.webopen.domain.teaching.vos;

public record EvaluationScoreBO(
        Long id,
        Long examId,
        Long courseId,
        Long studentId,
        int points,
        String status) {
}
