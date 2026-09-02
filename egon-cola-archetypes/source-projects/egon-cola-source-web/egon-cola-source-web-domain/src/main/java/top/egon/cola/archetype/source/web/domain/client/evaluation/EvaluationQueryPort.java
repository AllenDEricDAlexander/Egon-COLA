package top.egon.cola.archetype.source.web.domain.client.evaluation;

public interface EvaluationQueryPort {

    EvaluationCourse getCourse(Long courseId);

    EvaluationExam getExam(Long examId);

    EvaluationScore getScore(Long examId, Long scoreId);
}
