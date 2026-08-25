package ${package}.application.user.manage.impl;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.query.PermissionTreeQuery;
import ${package}.application.user.result.PermissionTreeResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.events.PermissionGrantedEvent;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.service.PermissionDomainService;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
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
