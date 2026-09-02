package top.egon.cola.archetype.source.light.domain.user.aggregates;

import top.egon.cola.archetype.source.light.domain.user.entities.Permission;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.light.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.light.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.light.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RolePermissionAggregateTest {
    @Test
    void collapses_duplicate_permission_grants() {
        RolePermissionAggregate aggregate = new RolePermissionAggregate(activeRole());
        Permission permission = activePermission();

        aggregate.grant(permission);
        aggregate.grant(permission);

        assertEquals(1, aggregate.permissions().size());
    }

    @Test
    void rejects_inactive_permission() {
        RolePermissionAggregate aggregate = new RolePermissionAggregate(activeRole());
        Permission permission = new Permission(
                new PermissionCode("user:read"), "Read user", PermissionStatus.DISABLED);

        assertThrows(UserDomainException.class, () -> aggregate.grant(permission));
    }

    @Test
    void rejects_archived_role() {
        Role role = new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ARCHIVED);
        RolePermissionAggregate aggregate = new RolePermissionAggregate(role);

        assertThrows(UserDomainException.class, () -> aggregate.grant(activePermission()));
    }

    private Role activeRole() {
        return new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE);
    }

    private Permission activePermission() {
        return new Permission(
                new PermissionCode("user:read"), "Read user", PermissionStatus.ACTIVE);
    }
}
