package top.egon.cola.archetype.source.webopen.infrastructure.user.repo.converter;

import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.UserPO;
import org.mapstruct.*;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserPOConverter extends BaseConverter<User, UserPO> {
    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java(user.id().value())")
    @Mapping(target = "name", expression = "java(user.name())")
    @Mapping(target = "email", expression = "java(user.email())")
    @Mapping(target = "status", expression = "java(user.status().name())")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserPO toTarget(User user);

    @Override
    @BeanMapping(ignoreByDefault = true, qualifiedByName = "restoreDomain")
    User toSource(UserPO target);

    @ObjectFactory
    @Named("restoreDomain")
    default User restoreDomain(UserPO target) {
        return toEntity(target, List.of());
    }

    default User toEntity(UserPO target, List<RoleCode> roleCodes) {
        return User.restore(new UserId(target.getId()), target.getName(), target.getEmail(),
                UserStatus.valueOf(target.getStatus()), roleCodes);
    }

    default UserPO toPO(User user) {
        return toTarget(user);
    }
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget UserPO target, UserPO source);
}
