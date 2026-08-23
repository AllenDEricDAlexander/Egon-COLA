package ${package}.domain.user.aggregates;

import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAggregateTest {
    @Test
    void assigns_active_role_to_active_user() {
        UserAggregate aggregate = new UserAggregate(
                new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.ACTIVE));

        aggregate.assign(new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE));

        assertEquals(1, aggregate.roles().size());
    }

    @Test
    void rejects_role_assignment_for_disabled_user() {
        UserAggregate aggregate = new UserAggregate(
                new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.DISABLED));
        Role role = new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE);

        assertThrows(UserDomainException.class, () -> aggregate.assign(role));
    }
}
