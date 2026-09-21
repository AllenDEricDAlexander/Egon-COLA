package top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationCourseBO;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationExamBO;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationScoreBO;

/**
 * Outbound evaluation dependency. It is an infrastructure client, not a domain port: the domain
 * only sees {@code EvaluationQueryService}, and the transport stays inside the implementations.
 */
public interface EvaluationQueryClient {

    EvaluationCourseBO getCourse(@NotNull @Positive Long courseId);

    EvaluationExamBO getExam(@NotNull @Positive Long examId);

    EvaluationScoreBO getScore(@NotNull @Positive Long examId, @NotNull @Positive Long scoreId);
}
