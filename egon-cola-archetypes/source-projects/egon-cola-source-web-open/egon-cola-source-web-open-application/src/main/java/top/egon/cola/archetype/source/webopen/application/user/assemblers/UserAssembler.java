package top.egon.cola.archetype.source.webopen.application.user.assemblers;

import top.egon.cola.archetype.source.webopen.application.user.result.UserDetailResult;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;

public final class UserAssembler {

    public UserDetailResult toResult(User user) {
        return new UserDetailResult(
            user.id().value(), user.name(), user.email(), user.status().name(),
            user.roleCodes().stream().map(RoleCode::value).toList());
    }
}
