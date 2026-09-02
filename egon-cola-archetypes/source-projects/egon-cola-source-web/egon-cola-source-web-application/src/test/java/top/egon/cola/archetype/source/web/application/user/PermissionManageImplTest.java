package top.egon.cola.archetype.source.web.application.user;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.web.application.user.manage.impl.PermissionManageImpl;
import top.egon.cola.archetype.source.web.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.web.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.web.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.web.domain.user.entities.Permission;
import top.egon.cola.archetype.source.web.domain.user.entities.Role;
import top.egon.cola.archetype.source.web.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.web.domain.user.enums.PermissionType;
import top.egon.cola.archetype.source.web.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.web.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.web.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.web.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
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
    @Mock UserDomainService<?> userDomainService;
    @Mock PermissionDomainService<?> permissionDomainService;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void grantsPermissionAndReturnsPermissionTree() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
        Role role = new Role(2001L, new RoleCode("STUDENT"), "Student", RoleStatus.ACTIVE);
        Permission permission = new Permission(3001L, new PermissionCode("CLASS_READ"),
                "Read school class", PermissionType.API, PermissionStatus.ACTIVE);
        when(userDomainService.findRoleByCode(new RoleCode("STUDENT"))).thenReturn(Optional.of(role));
        when(permissionDomainService.findByCode(new PermissionCode("CLASS_READ")))
                .thenReturn(Optional.of(permission));
        when(permissionDomainService.findByUserId(new UserId(1001L))).thenReturn(List.of(permission));
        when(idempotency.claim("grant-permission", "req-grant")).thenReturn(true);
        PermissionManageImpl manage = new PermissionManageImpl(userDomainService, permissionDomainService,
                new UserApplicationValidator(), idempotency, eventPublisher, () -> 2001L);

        manage.grantPermission(new GrantPermissionCommand("req-grant", "student", "class_read"));

        verify(userDomainService).saveRole(role);
        assertEquals(List.of("CLASS_READ"),
                manage.getPermissionTree(new PermissionTreeQuery(1001L)).permissionCodes());
    }
}
