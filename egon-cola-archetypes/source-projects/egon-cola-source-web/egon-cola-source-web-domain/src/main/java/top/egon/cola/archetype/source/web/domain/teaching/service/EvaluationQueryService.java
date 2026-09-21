package top.egon.cola.archetype.source.web.domain.teaching.service;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationCourseBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationExamBO;
import top.egon.cola.archetype.source.web.domain.teaching.vos.EvaluationScoreBO;

/** Domain-facing view of the evaluation read model; the transport sits behind Infrastructure. */
public interface EvaluationQueryService {

    EvaluationCourseBO getCourse(@NotNull Long courseId);

    EvaluationExamBO getExam(@NotNull Long examId);

    EvaluationScoreBO getScore(@NotNull Long examId, @NotNull Long scoreId);
}
