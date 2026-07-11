package ${package}.application.manage.user.impl;

import ${package}.application.command.user.GrantPermissionCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.manage.user.PermissionManage;
import ${package}.application.query.user.PermissionTreeQuery;
import ${package}.application.result.user.PermissionTreeResult;
import ${package}.application.validators.user.UserApplicationValidator;
import ${package}.domain.aggregates.user.RolePermissionAggregate;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.application.support.IdempotentCommand;
import ${package}.domain.entities.user.Permission;
import ${package}.domain.entities.user.Role;
import ${package}.domain.repos.user.PermissionRepository;
import ${package}.domain.repos.user.RoleRepository;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.RoleCode;
import ${package}.domain.vos.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("permissionManage")
public class PermissionManageImpl implements PermissionManage {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserApplicationValidator validator;
    private final CommandIdempotencyPort idempotency;

    public PermissionManageImpl(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserApplicationValidator validator,
            CommandIdempotencyPort idempotency) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.validator = validator;
        this.idempotency = idempotency;
    }

    @Override
    @Transactional
    public void grantPermission(GrantPermissionCommand command) {
        IdempotentCommand.execute(idempotency, "grant-permission", command.requestId(), () -> {
            validator.requireOrganizationAdmin();
            Role role = roleRepository.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> notFound("role not found"));
            Permission permission = permissionRepository.findByCode(new PermissionCode(command.permissionCode()))
                .orElseThrow(() -> notFound("permission not found"));
            RolePermissionAggregate aggregate = new RolePermissionAggregate(role, role.permissionCodes());
            aggregate.grant(permission);
            roleRepository.save(aggregate.role());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionTreeResult getPermissionTree(PermissionTreeQuery query) {
        UserId userId = new UserId(query.userId());
        return new PermissionTreeResult(userId.value(), permissionRepository.findByUserId(userId).stream()
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
