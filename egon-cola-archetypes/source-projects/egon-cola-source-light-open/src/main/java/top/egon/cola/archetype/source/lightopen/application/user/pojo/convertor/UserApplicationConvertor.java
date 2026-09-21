package top.egon.cola.archetype.source.lightopen.application.user.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.PermissionResult;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/** User entity-to-result projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "userApplicationConvertorImpl"))
public interface UserApplicationConvertor extends BaseForwardConverter<User, UserResult> {

    @Override
    @Mapping(target = "id", expression = "java(source.id().value())")
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "email", expression = "java(source.email())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    UserResult toTarget(User source);

    @Mapping(target = "roleCode", expression = "java(role.code().value())")
    @Mapping(target = "permissionCode", expression = "java(permission.code().value())")
    @Mapping(target = "status", expression = "java(permission.status().name())")
    PermissionResult toPermissionResult(Role role, Permission permission);
}
