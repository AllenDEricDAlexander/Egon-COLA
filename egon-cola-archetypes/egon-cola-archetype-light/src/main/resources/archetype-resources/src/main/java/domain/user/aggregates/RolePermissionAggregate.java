package ${package}.domain.user.aggregates;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.validators.UserDomainValidator;
import ${package}.domain.user.vos.PermissionCode;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class RolePermissionAggregate {
    private final Role role;
    private final Set<PermissionCode> permissions = new LinkedHashSet<>();

    public RolePermissionAggregate(Role role) {
        this.role = Objects.requireNonNull(role);
    }

    public Role role() {
        return role;
    }

    public Set<PermissionCode> permissions() {
        return Set.copyOf(permissions);
    }

    public void grant(Permission permission) {
        UserDomainValidator.requireAssignable(role, permission);
        permissions.add(permission.code());
    }
}
