package top.egon.cola.archetype.source.serviceopen.application.exam.pojo.result;

public record ScoreResult(
        Long id,
        Long examId,
        Long courseId,
        Long studentId,
        int points,
        String status) {
}
