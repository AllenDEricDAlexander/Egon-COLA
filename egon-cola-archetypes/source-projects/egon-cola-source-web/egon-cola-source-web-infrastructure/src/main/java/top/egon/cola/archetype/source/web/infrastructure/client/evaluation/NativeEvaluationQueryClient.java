package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.evaluation.facade.rpc.EvaluationRpcConverter;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationQueryPort;
import top.egon.cola.evaluation.facade.rpc.CourseRpcService;
import top.egon.cola.evaluation.facade.course.dto.GetCourseRequest;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationCourse;
import top.egon.cola.evaluation.facade.rpc.ExamRpcService;
import top.egon.cola.evaluation.facade.exam.dto.GetExamRequest;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationExam;
import top.egon.cola.evaluation.facade.rpc.ScoreRpcService;
import top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationScore;

/** Native DIRECT adapter for the existing evaluation query port. */
@Component("nativeEvaluationQueryClient")
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class NativeEvaluationQueryClient implements EvaluationQueryPort {
    @Qualifier("evaluationCourseRpcService")
    private final CourseRpcService courseService;
    @Qualifier("evaluationExamRpcService")
    private final ExamRpcService examService;
    @Qualifier("evaluationScoreRpcService")
    private final ScoreRpcService scoreService;
    @Qualifier("evaluationRpcConverter")
    private final EvaluationRpcConverter converter;
    @Qualifier("evaluationQueryConverter")
    private final EvaluationQueryConverter queryConverter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public EvaluationCourse getCourse(Long courseId) {
        var query = validation.validate(new GetCourseRequest(courseId));
        try {
            var response = EvaluationClientFailureMapper.requireData(converter.fromCourseRpcResponse(
                    courseService.getCourse(converter.toTarget(query))), "getCourse");
            if (!validation.isValid(response)) {
                throw EvaluationClientFailureMapper.incompatible("getCourse");
            }
            return queryConverter.toTarget(response);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = EvaluationClientFailureMapper.map(failure);
            log.debug("getCourse dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }

    @Override
    public EvaluationExam getExam(Long examId) {
        var query = validation.validate(new GetExamRequest(examId));
        try {
            var response = EvaluationClientFailureMapper.requireData(converter.fromExamRpcResponse(
                    examService.getExam(converter.toTarget(query))), "getExam");
            if (!validation.isValid(response)) {
                throw EvaluationClientFailureMapper.incompatible("getExam");
            }
            return queryConverter.toTarget(response);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = EvaluationClientFailureMapper.map(failure);
            log.debug("getExam dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }

    @Override
    public EvaluationScore getScore(Long examId, Long scoreId) {
        var query = validation.validate(new GetScoreRequest(examId, scoreId));
        try {
            var response = EvaluationClientFailureMapper.requireData(converter.fromScoreRpcResponse(
                    scoreService.getScore(converter.toTarget(query))), "getScore");
            if (!validation.isValid(response)) {
                throw EvaluationClientFailureMapper.incompatible("getScore");
            }
            return queryConverter.toTarget(response);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = EvaluationClientFailureMapper.map(failure);
            log.debug("getScore dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }
}
