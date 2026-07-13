package ${package}.application.assemblers.user;

import ${package}.application.result.user.UserDetailResult;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.vos.RoleCode;

public final class UserAssembler {

    public UserDetailResult toResult(User user) {
        return new UserDetailResult(
            user.id().value(), user.name(), user.email(), user.status().name(),
            user.roleCodes().stream().map(RoleCode::value).toList());
    }
}
