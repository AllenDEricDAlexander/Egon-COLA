package top.egon.cola.archetype.source.light.infrastructure.teaching.converter;

import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.light.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.light.domain.teaching.vos.Semester;
import top.egon.cola.archetype.source.light.infrastructure.teaching.po.SchoolClassPO;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** MapStruct conversion between the school-class domain entity and its PO. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolClassPOConverter extends BaseConverter<SchoolClass, SchoolClassPO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java(source.id().value())")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "semester", expression = "java(source.semester().value())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    SchoolClassPO toTarget(SchoolClass source);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    SchoolClass toSource(SchoolClassPO target);

    @ObjectFactory
    @Named("restoreDomain")
    default SchoolClass restoreDomain(SchoolClassPO target) {
        return new SchoolClass(new SchoolClassId(target.getId()), target.getName(),
                new Semester(target.getSemester()), SchoolClassStatus.valueOf(target.getStatus()));
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget SchoolClassPO target, SchoolClassPO source);
}
