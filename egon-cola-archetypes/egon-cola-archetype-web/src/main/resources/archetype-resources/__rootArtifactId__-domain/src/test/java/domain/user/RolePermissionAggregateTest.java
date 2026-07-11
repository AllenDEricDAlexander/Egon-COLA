package ${package}.domain.user;

import ${package}.domain.aggregates.user.RolePermissionAggregate;
import ${package}.domain.aggregates.user.UserAggregate;
import ${package}.domain.entities.user.Permission;
import ${package}.domain.entities.user.Role;
import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.PermissionStatus;
import ${package}.domain.enums.user.PermissionType;
import ${package}.domain.enums.user.RoleStatus;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.RoleCode;
import ${package}.domain.vos.user.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RolePermissionAggregateTest {

    @Test
    void assignsNormalizedRoleOnlyOnce() {
        UserAggregate aggregate = new UserAggregate(activeUser("u-1"));

        aggregate.assignRole(activeRole("student"));

        assertEquals(List.of(new RoleCode("STUDENT")), aggregate.user().roleCodes());
        OrganizationDomainException exception = assertThrows(
            OrganizationDomainException.class, () -> aggregate.assignRole(activeRole("STUDENT")));
        assertEquals(OrganizationDomainErrorCode.DUPLICATE_ROLE_ASSIGNMENT, exception.code());
    }

    @Test
    void grantsActivePermissionOnlyOnce() {
        RolePermissionAggregate aggregate = new RolePermissionAggregate(activeRole("student"), List.of());

        aggregate.grant(activePermission("class_read"));

        assertEquals(List.of(new PermissionCode("CLASS_READ")), aggregate.permissionCodes());
        OrganizationDomainException exception = assertThrows(
            OrganizationDomainException.class, () -> aggregate.grant(activePermission("CLASS_READ")));
        assertEquals(OrganizationDomainErrorCode.DUPLICATE_PERMISSION_GRANT, exception.code());
    }

    private static User activeUser(String id) {
        return new User(new UserId(id), "Mario", "mario@example.com", UserStatus.ACTIVE, List.of());
    }

    private static Role activeRole(String code) {
        return new Role("role-" + code.toLowerCase(Locale.ROOT), new RoleCode(code), code, RoleStatus.ACTIVE);
    }

    private static Permission activePermission(String code) {
        return new Permission("permission-" + code.toLowerCase(Locale.ROOT),
            new PermissionCode(code), code, PermissionType.API, PermissionStatus.ACTIVE);
    }
}
