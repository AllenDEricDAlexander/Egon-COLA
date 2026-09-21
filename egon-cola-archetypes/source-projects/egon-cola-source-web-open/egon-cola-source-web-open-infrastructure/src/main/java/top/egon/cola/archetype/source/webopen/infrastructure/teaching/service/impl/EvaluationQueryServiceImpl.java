package top.egon.cola.archetype.source.webopen.infrastructure.teaching.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.webopen.domain.teaching.service.EvaluationQueryService;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationCourseBO;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationExamBO;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationScoreBO;
import top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation.EvaluationQueryClient;

/**
 * Domain-facing evaluation read service. The transport decision stays behind the named Client, so
 * the domain contract never learns whether a broker-free stub or a native reference answered.
 */
@Validated
@Service("evaluationQueryService")
@RequiredArgsConstructor
@Slf4j
public class EvaluationQueryServiceImpl implements EvaluationQueryService {
    @Qualifier("evaluationQueryClient")
    private final EvaluationQueryClient client;

    @Override
    public EvaluationCourseBO getCourse(Long courseId) {
        return client.getCourse(courseId);
    }

    @Override
    public EvaluationExamBO getExam(Long examId) {
        return client.getExam(examId);
    }

    @Override
    public EvaluationScoreBO getScore(Long examId, Long scoreId) {
        return client.getScore(examId, scoreId);
    }
}
