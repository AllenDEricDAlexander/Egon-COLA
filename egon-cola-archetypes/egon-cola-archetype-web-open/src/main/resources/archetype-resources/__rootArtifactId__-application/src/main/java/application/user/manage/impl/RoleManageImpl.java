package ${package}.application.user.manage.impl;

import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.user.manage.RoleManage;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.events.RoleAssignedEvent;
import ${package}.domain.user.client.UserCachePort;
import ${package}.application.support.IdempotentCommand;
import ${package}.application.support.OrganizationTransactionHooks;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service("roleManage")
@RequiredArgsConstructor
public class RoleManageImpl implements RoleManage {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserApplicationValidator validator;
    private final UserCachePort userCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;

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
            OrganizationTransactionHooks.afterCommit(() -> {
                userCache.evict(user.id());
                eventPublisher.publish(new RoleAssignedEvent(UUID.randomUUID().toString(),
                    user.id().value(), Instant.now(), role.code().value()));
            });
        });
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(
            OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
