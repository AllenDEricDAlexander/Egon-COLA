package top.egon.cola.archetype.source.lightopen.application.user.convertor;

import top.egon.cola.archetype.source.lightopen.application.user.result.PermissionResult;
import top.egon.cola.archetype.source.lightopen.application.user.result.UserResult;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserSnapshot;
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
