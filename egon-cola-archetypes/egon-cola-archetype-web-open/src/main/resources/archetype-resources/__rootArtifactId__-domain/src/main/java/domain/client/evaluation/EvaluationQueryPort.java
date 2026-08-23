package ${package}.domain.client.evaluation;

public interface EvaluationQueryPort {

    EvaluationCourse getCourse(String courseId);

    EvaluationExam getExam(String examId);

    EvaluationScore getScore(String examId, String scoreId);
}
