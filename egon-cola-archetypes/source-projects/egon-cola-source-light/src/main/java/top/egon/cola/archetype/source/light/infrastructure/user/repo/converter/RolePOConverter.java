package top.egon.cola.archetype.source.light.infrastructure.user.repo.converter;

import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.RolePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** MapStruct conversion between the role domain entity and its PO. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RolePOConverter extends BaseConverter<Role, RolePO> {

    @Override
    @Mapping(target = "code", expression = "java(source.code().value())")
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    RolePO toTarget(Role source);

    @Override
    default Role toSource(RolePO target) {
        return new Role(new RoleCode(target.getCode()), target.getName(),
                RoleStatus.valueOf(target.getStatus()));
    }
}
