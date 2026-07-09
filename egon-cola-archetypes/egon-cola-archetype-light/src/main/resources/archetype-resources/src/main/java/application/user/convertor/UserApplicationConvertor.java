package ${package}.application.user.convertor;

import ${package}.application.user.result.PermissionResult;
import ${package}.application.user.result.UserResult;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.vos.UserSnapshot;
import org.springframework.stereotype.Component;

@Component
public class UserApplicationConvertor {
    public UserResult toResult(User user) {
        return new UserResult(user.id().value(), user.name(), user.email(), user.status().name());
    }

    public UserResult toResult(UserSnapshot user) {
        return new UserResult(user.id(), user.name(), user.email(), user.status().name());
    }

    public UserSnapshot toSnapshot(User user) {
        return UserSnapshot.from(user);
    }

    public PermissionResult toResult(Role role, Permission permission) {
        return new PermissionResult(
                role.code().value(), permission.code().value(), permission.status().name());
    }
}
