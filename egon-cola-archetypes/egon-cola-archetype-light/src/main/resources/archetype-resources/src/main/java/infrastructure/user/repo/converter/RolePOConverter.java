package ${package}.infrastructure.user.repo.converter;

import ${package}.domain.user.entities.Role;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.po.RolePO;
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
