package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import top.egon.cola.evaluation.facade.course.CourseFacade;
import top.egon.cola.evaluation.facade.exam.ExamFacade;
import top.egon.cola.evaluation.facade.exam.ScoreFacade;
import top.egon.cola.evaluation.facade.course.dto.CourseResponse;
import top.egon.cola.evaluation.facade.course.dto.GetCourseRequest;
import top.egon.cola.evaluation.facade.exam.dto.ExamResponse;
import top.egon.cola.evaluation.facade.exam.dto.GetExamRequest;
import top.egon.cola.evaluation.facade.exam.dto.GetScoreRequest;
import top.egon.cola.evaluation.facade.exam.dto.ScoreResponse;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationCourse;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationExam;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationQueryPort;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationScore;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod"})
public class DubboEvaluationQueryClient implements EvaluationQueryPort {

    @DubboReference(
            group = "${organization.integrations.evaluation.course-group:course}",
            version = "${organization.integrations.evaluation.version:1.0.0}",
            retries = 0,
            check = true)
    private CourseFacade courseFacade;

    @DubboReference(
            group = "${organization.integrations.evaluation.exam-group:exam}",
            version = "${organization.integrations.evaluation.version:1.0.0}",
            retries = 0,
            check = true)
    private ExamFacade examFacade;

    @DubboReference(
            group = "${organization.integrations.evaluation.score-group:score}",
            version = "${organization.integrations.evaluation.version:1.0.0}",
            retries = 0,
            check = true)
    private ScoreFacade scoreFacade;

    public DubboEvaluationQueryClient() {
    }

    DubboEvaluationQueryClient(
            CourseFacade courseFacade,
            ExamFacade examFacade,
            ScoreFacade scoreFacade) {
        this.courseFacade = courseFacade;
        this.examFacade = examFacade;
        this.scoreFacade = scoreFacade;
    }

    @Override
    public EvaluationCourse getCourse(Long courseId) {
        try {
            CourseResponse response = EvaluationClientFailureMapper.requireData(
                    courseFacade.getCourse(new GetCourseRequest(courseId)), "getCourse");
            return new EvaluationCourse(
                    response.id(), response.code(), response.name(), response.credit(), response.status());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw EvaluationClientFailureMapper.map(failure);
        }
    }

    @Override
    public EvaluationExam getExam(Long examId) {
        try {
            ExamResponse response = EvaluationClientFailureMapper.requireData(
                    examFacade.getExam(new GetExamRequest(examId)), "getExam");
            return new EvaluationExam(
                    response.id(),
                    response.courseId(),
                    response.title(),
                    response.startsAt(),
                    response.endsAt(),
                    response.status());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw EvaluationClientFailureMapper.map(failure);
        }
    }

    @Override
    public EvaluationScore getScore(Long examId, Long scoreId) {
        try {
            ScoreResponse response = EvaluationClientFailureMapper.requireData(
                    scoreFacade.getScore(new GetScoreRequest(examId, scoreId)), "getScore");
            return new EvaluationScore(
                    response.id(),
                    response.examId(),
                    response.courseId(),
                    response.studentId(),
                    response.points(),
                    response.status());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw EvaluationClientFailureMapper.map(failure);
        }
    }
}
