package ${package}.domain.client.evaluation;

public record EvaluationScore(
        String id,
        String examId,
        String courseId,
        String studentId,
        int points,
        String status) {
}
