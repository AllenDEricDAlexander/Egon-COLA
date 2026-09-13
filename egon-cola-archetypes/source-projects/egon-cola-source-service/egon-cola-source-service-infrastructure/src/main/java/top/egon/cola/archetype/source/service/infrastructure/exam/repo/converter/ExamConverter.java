package top.egon.cola.archetype.source.service.infrastructure.exam.repo.converter;

import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamStatus;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.service.infrastructure.exam.repo.po.ExamPO;
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
public interface ExamConverter extends BaseConverter<Exam, ExamPO> {

    @Override
    @Mapping(target = "id", expression = "java(source.getId().value())")
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "courseId", expression = "java(source.getCourseId().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    ExamPO toTarget(Exam source);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    Exam toSource(ExamPO target);

    @ObjectFactory
    @Named("restoreDomain")
    default Exam restoreDomain(ExamPO target) {
        return new Exam(
                new ExamId(target.getId()), new CourseId(target.getCourseId()), target.getTitle(),
                target.getStartsAt(), target.getEndsAt(), ExamStatus.valueOf(target.getStatus()));
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget ExamPO target, ExamPO source);
}
