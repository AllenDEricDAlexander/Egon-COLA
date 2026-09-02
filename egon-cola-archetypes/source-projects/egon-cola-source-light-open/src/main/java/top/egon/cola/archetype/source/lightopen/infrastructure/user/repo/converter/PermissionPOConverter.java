package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.converter;

import top.egon.cola.archetype.source.lightopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.PermissionPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** MapStruct conversion between the permission domain entity and its PO. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionPOConverter extends BaseConverter<Permission, PermissionPO> {

    @Override
    @Mapping(target = "code", expression = "java(source.code().value())")
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    PermissionPO toTarget(Permission source);

    @Override
    default Permission toSource(PermissionPO target) {
        return new Permission(new PermissionCode(target.getCode()), target.getName(),
                PermissionStatus.valueOf(target.getStatus()));
    }
}
