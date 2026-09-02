package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyFailure;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationCourse;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationExam;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationQueryPort;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationScore;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class LocalEvaluationQueryStub implements EvaluationQueryPort {

    @Override
    public EvaluationCourse getCourse(Long courseId) {
        rejectMissing(courseId, "course");
        return new EvaluationCourse(courseId, "LOCAL", "Local Course " + courseId, 0, "ACTIVE");
    }

    @Override
    public EvaluationExam getExam(Long examId) {
        rejectMissing(examId, "exam");
        return new EvaluationExam(
                examId,
                9001L,
                "Local Exam " + examId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                "PUBLISHED");
    }

    @Override
    public EvaluationScore getScore(Long examId, Long scoreId) {
        rejectMissing(examId, "exam");
        rejectMissing(scoreId, "score");
        return new EvaluationScore(
                scoreId, examId, 9001L, 7001L, 100, "RECORDED");
    }

    private static void rejectMissing(Long id, String resource) {
        if (id == null || id <= 0) {
            throw new ExternalDependencyException(
                    "evaluation",
                    ExternalDependencyFailure.NOT_FOUND,
                    "LOCAL_NOT_FOUND",
                    "local evaluation " + resource + " was not found",
                    null);
        }
    }
}
