package top.egon.cola.archetype.source.web.application.user.manage.impl;

import top.egon.cola.archetype.source.web.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.web.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.web.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.web.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.web.domain.user.events.RoleAssignedEvent;
import top.egon.cola.archetype.source.web.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.web.application.support.IdempotentCommand;
import top.egon.cola.archetype.source.web.application.support.OrganizationTransactionHooks;
import top.egon.cola.archetype.source.web.domain.user.entities.Role;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.Instant;

@Service("roleManage")
@RequiredArgsConstructor
public class RoleManageImpl implements RoleManage {

    private final UserDomainService<?> userDomainService;
    private final UserApplicationValidator validator;
    private final UserCachePort userCache;
    private final CommandIdempotencyPort idempotency;
    private final OrganizationEventPublisher eventPublisher;
    private final LongIdGenerator idGenerator;

    @Override
    @Transactional
    public void assignRole(AssignRoleCommand command) {
        IdempotentCommand.execute(idempotency, "assign-role", command.requestId(), () -> {
            validator.requireOrganizationAdmin();
            User user = userDomainService.findById(new UserId(command.userId()))
                .orElseThrow(() -> notFound("user not found"));
            Role role = userDomainService.findRoleByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> notFound("role not found"));
            UserAggregate aggregate = new UserAggregate(user);
            aggregate.assignRole(role);
            userDomainService.save(aggregate.user());
            OrganizationTransactionHooks.afterCommit(() -> {
                userCache.evict(user.id());
                eventPublisher.publish(new RoleAssignedEvent(Long.toString(idGenerator.nextLongId()),
                    user.id().value(), Instant.now(), role.code().value()));
            });
        });
    }

    private static OrganizationApplicationException notFound(String message) {
        return new OrganizationApplicationException(
            OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", message);
    }
}
