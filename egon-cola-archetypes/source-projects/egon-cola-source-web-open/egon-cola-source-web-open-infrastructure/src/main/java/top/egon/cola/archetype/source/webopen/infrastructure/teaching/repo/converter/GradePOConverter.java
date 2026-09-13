package top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.converter;

import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.webopen.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.po.GradePO;
import org.mapstruct.*;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GradePOConverter extends BaseConverter<Grade, GradePO> {
    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java(grade.id())")
    @Mapping(target = "code", expression = "java(grade.code().value())")
    @Mapping(target = "name", expression = "java(grade.name())")
    @Mapping(target = "status", expression = "java(grade.status().name())")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GradePO toTarget(Grade grade);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    Grade toSource(GradePO target);

    @ObjectFactory
    @Named("restoreDomain")
    default Grade restoreDomain(GradePO target) {
        return new Grade(target.getId(), GradeCode.create(target.getCode()), target.getName(),
                GradeStatus.valueOf(target.getStatus()));
    }

    default GradePO toPO(Grade grade) {
        return toTarget(grade);
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget GradePO target, GradePO source);
}
