package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.converter;

import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.Semester;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.SchoolClassPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** MapStruct conversion between the school-class domain entity and its PO. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolClassPOConverter extends BaseConverter<SchoolClass, SchoolClassPO> {

    @Override
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "semester", expression = "java(source.semester().value())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    SchoolClassPO toTarget(SchoolClass source);

    @Override
    default SchoolClass toSource(SchoolClassPO target) {
        return new SchoolClass(new SchoolClassId(target.getId()), target.getName(),
                new Semester(target.getSemester()), SchoolClassStatus.valueOf(target.getStatus()));
    }
}
