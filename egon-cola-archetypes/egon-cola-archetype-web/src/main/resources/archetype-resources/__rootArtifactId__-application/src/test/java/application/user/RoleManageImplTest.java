package ${package}.application.user;

import ${package}.application.command.user.AssignRoleCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.manage.user.impl.RoleManageImpl;
import ${package}.application.validators.user.UserApplicationValidator;
import ${package}.domain.entities.user.Role;
import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.RoleStatus;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.repos.user.RoleRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.vos.user.RoleCode;
import ${package}.domain.vos.user.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManageImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;

    @AfterEach
    void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void assignsNormalizedRoleThroughAggregate() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
        when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(
            new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.ACTIVE, List.of())));
        when(roleRepository.findByCode(new RoleCode("STUDENT"))).thenReturn(Optional.of(
            new Role("role-student", new RoleCode("STUDENT"), "Student", RoleStatus.ACTIVE)));
        RoleManageImpl manage = new RoleManageImpl(
            userRepository, roleRepository, new UserApplicationValidator());

        manage.assignRole(new AssignRoleCommand("req-role", "u-1", "student"));

        verify(userRepository).save(argThat(
            saved -> saved.roleCodes().contains(new RoleCode("STUDENT"))));
    }
}
