package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.facade.course.CourseFacade;
import top.egon.cola.archetype.source.service.facade.exam.ExamFacade;
import top.egon.cola.archetype.source.service.facade.exam.ScoreFacade;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationCourse;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationExam;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationQueryPort;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationScore;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Native DIRECT adapter for the existing evaluation query port. */
@Component("nativeEvaluationQueryClient")
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class NativeEvaluationQueryClient implements EvaluationQueryPort {
    @Qualifier("evaluationCourseFacade")
    private final CourseFacade courseFacade;
    @Qualifier("evaluationExamFacade")
    private final ExamFacade examFacade;
    @Qualifier("evaluationScoreFacade")
    private final ScoreFacade scoreFacade;
    @Qualifier("evaluationQueryConverter")
    private final EvaluationQueryConverter queryConverter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public EvaluationCourse getCourse(Long courseId) {
        var query = validation.validate(new EvaluationQueryConverter.CourseQuery(courseId));
        try {
            var response = courseFacade.getCourse(queryConverter.courseRequest(query));
            if (response == null) {
                throw EvaluationClientFailureMapper.incompatible("getCourse");
            }
            if (!response.getSuccess()) {
                throw EvaluationClientFailureMapper.rejected(
                        response.hasCode() ? response.getCode() : null);
            }
            var data = response.getData();
            if (!response.hasData() || !data.hasId() || data.getId() <= 0L) {
                throw EvaluationClientFailureMapper.incompatible("getCourse");
            }
            return queryConverter.toTarget(data);
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
        var query = validation.validate(new EvaluationQueryConverter.ExamQuery(examId));
        try {
            var response = examFacade.getExam(queryConverter.examRequest(query));
            if (response == null) {
                throw EvaluationClientFailureMapper.incompatible("getExam");
            }
            if (!response.getSuccess()) {
                throw EvaluationClientFailureMapper.rejected(
                        response.hasCode() ? response.getCode() : null);
            }
            var data = response.getData();
            if (!response.hasData() || !data.hasId() || data.getId() <= 0L
                    || !data.hasCourseId() || data.getCourseId() <= 0L) {
                throw EvaluationClientFailureMapper.incompatible("getExam");
            }
            return queryConverter.toTarget(data);
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
        var query = validation.validate(new EvaluationQueryConverter.ScoreQuery(examId, scoreId));
        try {
            var response = scoreFacade.getScore(queryConverter.scoreRequest(query));
            if (response == null) {
                throw EvaluationClientFailureMapper.incompatible("getScore");
            }
            if (!response.getSuccess()) {
                throw EvaluationClientFailureMapper.rejected(
                        response.hasCode() ? response.getCode() : null);
            }
            var data = response.getData();
            if (!response.hasData() || !data.hasId() || data.getId() <= 0L
                    || !data.hasExamId() || data.getExamId() <= 0L
                    || !data.hasCourseId() || data.getCourseId() <= 0L
                    || !data.hasStudentId() || data.getStudentId() <= 0L) {
                throw EvaluationClientFailureMapper.incompatible("getScore");
            }
            return queryConverter.toTarget(data);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = EvaluationClientFailureMapper.map(failure);
            log.debug("getScore dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }
}
