package top.egon.cola.archetype.source.lightopen.domain.user.aggregates;

import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.validators.UserDomainValidator;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class UserAggregate {
    private final User user;
    private final Set<RoleCode> roles = new LinkedHashSet<>();

    public UserAggregate(User user) {
        this.user = Objects.requireNonNull(user);
    }

    public User user() {
        return user;
    }

    public Set<RoleCode> roles() {
        return Set.copyOf(roles);
    }

    public void assign(Role role) {
        UserDomainValidator.requireActive(user);
        UserDomainValidator.requireActive(role);
        roles.add(role.code());
    }
}
