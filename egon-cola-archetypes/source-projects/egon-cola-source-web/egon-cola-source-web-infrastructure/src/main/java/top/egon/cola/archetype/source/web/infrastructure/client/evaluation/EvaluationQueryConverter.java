package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.evaluation.facade.course.dto.CourseResponse;
import top.egon.cola.evaluation.facade.exam.dto.ExamResponse;
import top.egon.cola.evaluation.facade.exam.dto.ScoreResponse;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationCourse;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationExam;
import top.egon.cola.archetype.source.web.domain.client.evaluation.EvaluationScore;

/** Converts facade responses into the existing consumer-owned evaluation projections. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EvaluationQueryConverter extends BaseConverter<CourseResponse, EvaluationCourse> {
    @Override
    EvaluationCourse toTarget(CourseResponse source);

    @Override
    CourseResponse toSource(EvaluationCourse target);

    EvaluationExam toTarget(ExamResponse source);

    ExamResponse toSource(EvaluationExam target);

    EvaluationScore toTarget(ScoreResponse source);

    ScoreResponse toSource(EvaluationScore target);
}
