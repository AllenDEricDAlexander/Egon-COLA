package top.egon.cola.archetype.source.webopen.infrastructure.user.converter;

import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.infrastructure.user.po.RolePO;
import org.mapstruct.*;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RolePOConverter extends BaseConverter<Role, RolePO> {
    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java(role.id())")
    @Mapping(target = "code", expression = "java(role.code().value())")
    @Mapping(target = "name", expression = "java(role.name())")
    @Mapping(target = "status", expression = "java(role.status().name())")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    RolePO toTarget(Role role);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    Role toSource(RolePO target);

    @ObjectFactory
    @Named("restoreDomain")
    default Role restoreDomain(RolePO target) {
        return toEntity(target, List.of());
    }

    default Role toEntity(RolePO target, List<PermissionCode> permissionCodes) {
        return new Role(target.getId(), new RoleCode(target.getCode()), target.getName(),
                RoleStatus.valueOf(target.getStatus()), permissionCodes);
    }

    default RolePO toPO(Role role) {
        return toTarget(role);
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget RolePO target, RolePO source);
}
