package top.egon.cola.archetype.source.web.infrastructure.client.evaluation.impl;

import top.egon.cola.archetype.source.web.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.web.common.enums.ExternalDependencyFailure;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationCourseBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationExamBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationScoreBO;
import top.egon.cola.archetype.source.web.infrastructure.client.evaluation.EvaluationQueryClient;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Broker-free profile-local implementation of the outbound evaluation client. */
@Component("evaluationQueryClient")
@Profile("test")
@Slf4j
public class LocalEvaluationQueryClientImpl implements EvaluationQueryClient {

    @Override
    public EvaluationCourseBO getCourse(Long courseId) {
        rejectMissing(courseId, "course");
        return new EvaluationCourseBO(courseId, "LOCAL", "Local Course " + courseId, 0, "ACTIVE");
    }

    @Override
    public EvaluationExamBO getExam(Long examId) {
        rejectMissing(examId, "exam");
        return new EvaluationExamBO(
                examId,
                9001L,
                "Local Exam " + examId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                "PUBLISHED");
    }

    @Override
    public EvaluationScoreBO getScore(Long examId, Long scoreId) {
        rejectMissing(examId, "exam");
        rejectMissing(scoreId, "score");
        return new EvaluationScoreBO(
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
