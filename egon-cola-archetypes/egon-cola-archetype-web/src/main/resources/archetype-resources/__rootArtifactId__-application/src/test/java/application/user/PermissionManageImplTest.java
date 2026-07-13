package ${package}.application.user;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.user.manage.impl.PermissionManageImpl;
import ${package}.application.user.query.PermissionTreeQuery;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.PermissionType;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionManageImplTest {

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

    @AfterEach
    void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void grantsPermissionAndReturnsPermissionTree() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
        Role role = new Role("role-student", new RoleCode("STUDENT"), "Student", RoleStatus.ACTIVE);
        Permission permission = new Permission("permission-class-read", new PermissionCode("CLASS_READ"),
            "Read school class", PermissionType.API, PermissionStatus.ACTIVE);
        when(roleRepository.findByCode(new RoleCode("STUDENT"))).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode(new PermissionCode("CLASS_READ"))).thenReturn(Optional.of(permission));
        when(permissionRepository.findByUserId(new UserId("u-1"))).thenReturn(List.of(permission));
        PermissionManageImpl manage = new PermissionManageImpl(
            roleRepository, permissionRepository, new UserApplicationValidator(), idempotency, eventPublisher);
        when(idempotency.claim("grant-permission", "req-grant")).thenReturn(true);

        manage.grantPermission(new GrantPermissionCommand("req-grant", "student", "class_read"));

        verify(roleRepository).save(role);
        assertEquals(List.of("CLASS_READ"),
            manage.getPermissionTree(new PermissionTreeQuery("u-1")).permissionCodes());
    }
}
