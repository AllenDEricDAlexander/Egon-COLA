package top.egon.cola.archetype.source.webopen.application.user;

import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.webopen.application.user.manage.impl.PermissionManageImpl;
import top.egon.cola.archetype.source.webopen.application.user.pojo.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.webopen.application.user.pojo.convertor.PermissionConverter;
import top.egon.cola.archetype.source.webopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionType;
import top.egon.cola.archetype.source.webopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.webopen.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@ExtendWith(MockitoExtension.class)
class PermissionManageImplTest {
    @Mock ValidationUtils validationUtils;

    private UserApplicationValidator userValidator() {
        return new UserApplicationValidator(validationUtils);
    }

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Mock UserDomainService userDomainService;
    @Mock PermissionDomainService permissionDomainService;
    @Mock CommandIdempotencyService idempotency;
    @Mock OrganizationEventService eventPublisher;

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
        PermissionManageImpl manage = new PermissionManageImpl(userDomainService, permissionDomainService, userValidator(),
                Mappers.getMapper(PermissionConverter.class), idempotency, eventPublisher);

        manage.grantPermission(new GrantPermissionCommand("req-grant", "student", "class_read"));

        verify(userDomainService).saveRole(role);
        assertEquals(List.of("CLASS_READ"),
                manage.getPermissionTree(new PermissionTreeQuery(1001L)).permissionCodes());
    }
}
