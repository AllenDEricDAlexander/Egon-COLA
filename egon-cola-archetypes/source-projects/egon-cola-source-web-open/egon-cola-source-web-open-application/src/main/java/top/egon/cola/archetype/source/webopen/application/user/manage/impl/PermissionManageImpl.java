package top.egon.cola.archetype.source.webopen.application.user.manage.impl;

import top.egon.cola.archetype.source.webopen.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.webopen.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.webopen.application.user.result.PermissionTreeResult;
import top.egon.cola.archetype.source.webopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.webopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.webopen.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.webopen.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.webopen.domain.user.events.PermissionGrantedEvent;
import top.egon.cola.archetype.source.webopen.application.support.IdempotentCommand;
import top.egon.cola.archetype.source.webopen.application.support.OrganizationTransactionHooks;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.Instant;

@Service("permissionManage")
@RequiredArgsConstructor
public class PermissionManageImpl implements PermissionManage {

    private final UserDomainService<?> userDomainService;
    private final PermissionDomainService<?> permissionDomainService;
    private final UserApplicationValidator validator;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;
    private final LongIdGenerator idGenerator;

    @Override
    @Transactional
    public void grantPermission(GrantPermissionCommand command) {
        IdempotentCommand.execute(idempotency, "grant-permission", command.requestId(), () -> {
            validator.requireOrganizationAdmin();
            Role role = userDomainService.findRoleByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> notFound("role not found"));
            Permission permission = permissionDomainService.findByCode(new PermissionCode(command.permissionCode()))
                .orElseThrow(() -> notFound("permission not found"));
            RolePermissionAggregate aggregate = new RolePermissionAggregate(role, role.permissionCodes());
            aggregate.grant(permission);
            userDomainService.saveRole(aggregate.role());
            OrganizationTransactionHooks.afterCommit(() -> eventPublisher.publish(
                new PermissionGrantedEvent(Long.toString(idGenerator.nextLongId()), role.id(), Instant.now(),
                    role.code().value(), permission.code().value())));
        });
    }

    @Override
    public PermissionTreeResult getPermissionTree(PermissionTreeQuery query) {
        UserId userId = new UserId(query.userId());
        return new PermissionTreeResult(userId.value(), permissionDomainService.findByUserId(userId).stream()
            .map(permission -> permission.code().value())
            .distinct()
            .sorted()
            .toList());
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(
            OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
