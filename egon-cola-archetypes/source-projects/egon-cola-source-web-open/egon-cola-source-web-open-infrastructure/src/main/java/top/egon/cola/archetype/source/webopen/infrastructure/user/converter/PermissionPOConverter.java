package top.egon.cola.archetype.source.webopen.infrastructure.user.converter;

import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionType;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.infrastructure.user.po.PermissionPO;
import org.mapstruct.*;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionPOConverter extends BaseConverter<Permission, PermissionPO> {
    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java(permission.id())")
    @Mapping(target = "code", expression = "java(permission.code().value())")
    @Mapping(target = "name", expression = "java(permission.name())")
    @Mapping(target = "type", expression = "java(permission.type().name())")
    @Mapping(target = "status", expression = "java(permission.status().name())")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    PermissionPO toTarget(Permission permission);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    Permission toSource(PermissionPO target);

    @ObjectFactory
    @Named("restoreDomain")
    default Permission restoreDomain(PermissionPO target) {
        return new Permission(target.getId(), new PermissionCode(target.getCode()), target.getName(),
                PermissionType.valueOf(target.getType()), PermissionStatus.valueOf(target.getStatus()));
    }

    default PermissionPO toPO(Permission permission) {
        return toTarget(permission);
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget PermissionPO target, PermissionPO source);
}
