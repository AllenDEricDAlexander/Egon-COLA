package top.egon.cola.archetype.source.web.infrastructure.teaching.converter;

import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.archetype.source.web.infrastructure.teaching.po.SchoolClassPO;
import org.mapstruct.*;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolClassPOConverter extends BaseConverter<SchoolClass, SchoolClassPO> {
    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java(schoolClass.id().value())")
    @Mapping(target = "name", expression = "java(schoolClass.name())")
    @Mapping(target = "gradeName", expression = "java(schoolClass.gradeName())")
    @Mapping(target = "gradeId", expression = "java(schoolClass.gradeId())")
    @Mapping(target = "status", expression = "java(schoolClass.status().name())")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    SchoolClassPO toTarget(SchoolClass schoolClass);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    SchoolClass toSource(SchoolClassPO target);

    @ObjectFactory
    @Named("restoreDomain")
    default SchoolClass restoreDomain(SchoolClassPO target) {
        return toEntity(target, GradeCode.create("UNKNOWN"), List.of());
    }

    default SchoolClass toEntity(SchoolClassPO target, GradeCode gradeCode, List<UserId> userIds) {
        return new SchoolClass(new SchoolClassId(target.getId()), target.getName(), target.getGradeId(),
                gradeCode, target.getGradeName(), SchoolClassStatus.valueOf(target.getStatus()), userIds);
    }

    default SchoolClassPO toPO(SchoolClass schoolClass) {
        return toTarget(schoolClass);
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget SchoolClassPO target, SchoolClassPO source);
}
