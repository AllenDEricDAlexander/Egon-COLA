package top.egon.cola.archetype.source.webopen.domain.user;

import top.egon.cola.archetype.source.webopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.webopen.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionType;
import top.egon.cola.archetype.source.webopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainException;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RolePermissionAggregateTest {

    @Test
    void assignsNormalizedRoleOnlyOnce() {
        UserAggregate aggregate = new UserAggregate(activeUser(1001L));

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

    private static User activeUser(Long id) {
        return new User(new UserId(id), "Mario", "mario@example.com", UserStatus.ACTIVE, List.of());
    }

    private static Role activeRole(String code) {
        return new Role(2001L, new RoleCode(code), code, RoleStatus.ACTIVE);
    }

    private static Permission activePermission(String code) {
        return new Permission(3001L,
            new PermissionCode(code), code, PermissionType.API, PermissionStatus.ACTIVE);
    }
}
