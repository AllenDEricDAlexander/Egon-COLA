package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.converter;

import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.UserPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** MapStruct conversion between the user domain entity and its PO. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserPOConverter extends BaseConverter<User, UserPO> {

    @Override
    @Mapping(target = "externalId", expression = "java(source.externalId())")
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "email", expression = "java(source.email())")
    @Mapping(target = "status", expression = "java(source.status().name())")
    UserPO toTarget(User source);

    @Override
    default User toSource(UserPO target) {
        return new User(new UserId(target.getId()), target.getExternalId(), target.getName(),
                target.getEmail(), UserStatus.valueOf(target.getStatus()));
    }
}
