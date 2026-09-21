package top.egon.cola.archetype.source.web.application.user.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.web.application.user.pojo.result.UserDetailResult;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

import java.util.List;

/** User entity-to-result projection; role codes flatten to their wire strings. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "userConverterImpl"))
public interface UserConverter extends BaseForwardConverter<User, UserDetailResult> {

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "roleCodes", source = "roleCodes")
    UserDetailResult toTarget(UserId id, String name, String email, UserStatus status, List<RoleCode> roleCodes);

    @Override
    default UserDetailResult toTarget(User source) {
        return toTarget(source.id(), source.name(), source.email(), source.status(), source.roleCodes());
    }

    /** Callable contract kept from the previous hand-written projection. */
    default UserDetailResult toResult(User source) {
        return toTarget(source);
    }

    default List<String> toRoleCodes(List<RoleCode> roleCodes) {
        return roleCodes == null ? List.of() : roleCodes.stream().map(RoleCode::value).toList();
    }
}
