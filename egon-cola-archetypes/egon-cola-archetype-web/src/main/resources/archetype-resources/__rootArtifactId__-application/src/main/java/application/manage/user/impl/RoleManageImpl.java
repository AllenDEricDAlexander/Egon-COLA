package ${package}.application.manage.user.impl;

import ${package}.application.command.user.AssignRoleCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.manage.user.RoleManage;
import ${package}.application.validators.user.UserApplicationValidator;
import ${package}.domain.aggregates.user.UserAggregate;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.user.UserCachePort;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
import ${package}.domain.entities.user.Role;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.user.RoleRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.vos.user.RoleCode;
import ${package}.domain.vos.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("roleManage")
public class RoleManageImpl implements RoleManage {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserApplicationValidator validator;
    private final UserCachePort userCache;
    private final CommandIdempotencyPort idempotency;

    public RoleManageImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserApplicationValidator validator,
            UserCachePort userCache,
            CommandIdempotencyPort idempotency) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.validator = validator;
        this.userCache = userCache;
        this.idempotency = idempotency;
    }

    @Override
    @Transactional
    public void assignRole(AssignRoleCommand command) {
        IdempotentCommand.execute(idempotency, "assign-role", command.requestId(), () -> {
            validator.requireOrganizationAdmin();
            User user = userRepository.findById(new UserId(command.userId()))
                .orElseThrow(() -> notFound("user not found"));
            Role role = roleRepository.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> notFound("role not found"));
            UserAggregate aggregate = new UserAggregate(user);
            aggregate.assignRole(role);
            userRepository.save(aggregate.user());
            OrganizationTransactionHooks.afterCommit(() -> userCache.evict(user.id()));
        });
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(
            OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
