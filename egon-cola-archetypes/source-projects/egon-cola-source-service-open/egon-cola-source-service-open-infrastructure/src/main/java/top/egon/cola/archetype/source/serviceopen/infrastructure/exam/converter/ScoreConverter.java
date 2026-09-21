package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.converter;

import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ScoreStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ScoreValue;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.po.ScorePO;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScoreConverter extends BaseConverter<Score, ScorePO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "examId", expression = "java(source.getExamId().value())")
    @Mapping(target = "courseId", expression = "java(source.getCourseId().value())")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", expression = "java(source.getPoints().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    ScorePO toTarget(Score source);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    Score toSource(ScorePO target);

    @ObjectFactory
    @Named("restoreDomain")
    default Score restoreDomain(ScorePO target) {
        return new Score(
                target.getId(), new ExamId(target.getExamId()), new CourseId(target.getCourseId()),
                target.getStudentId(), new ScoreValue(target.getPoints()),
                ScoreStatus.valueOf(target.getStatus()));
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget ScorePO target, ScorePO source);
}
