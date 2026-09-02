package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.converter;

import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ScoreStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ScoreValue;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.po.ScorePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScoreConverter extends BaseConverter<Score, ScorePO> {

    @Override
    @Mapping(target = "examId", expression = "java(source.getExamId().value())")
    @Mapping(target = "courseId", expression = "java(source.getCourseId().value())")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", expression = "java(source.getPoints().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    ScorePO toTarget(Score source);

    @Override
    default Score toSource(ScorePO target) {
        return new Score(
                target.getId(), new ExamId(target.getExamId()), new CourseId(target.getCourseId()),
                target.getStudentId(), new ScoreValue(target.getPoints()),
                ScoreStatus.valueOf(target.getStatus()));
    }
}
