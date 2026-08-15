package top.egon.cola.platform.rbac3.admin.iam.business.service;

import top.egon.cola.platform.rbac3.admin.iam.business.domain.command.ReplaceUserBusinessAccessesCommand;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.vo.UserApplicationAccessVO;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.vo.UserBusinessAccessVO;
import top.egon.cola.platform.rbac3.admin.iam.business.repository.UserBusinessAccessRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ExpectedVersionsVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationResultVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationScopeVO;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Coordinates DDC validation and RBAC-owned User Business grant changes. */
public final class UserBusinessAccessFacade {

    private final UserBusinessAccessRepository store;
    private final DdcCatalogGateway catalog;
    private final AuthorizationMutationCoordinator mutationCoordinator;
    private final DatabaseClock databaseClock;

    public UserBusinessAccessFacade(
            UserBusinessAccessRepository store,
            DdcCatalogGateway catalog,
            AuthorizationMutationCoordinator mutationCoordinator,
            DatabaseClock databaseClock) {
        this.store = Objects.requireNonNull(store, "store");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.mutationCoordinator = Objects.requireNonNull(
                mutationCoordinator, "mutationCoordinator");
        this.databaseClock = Objects.requireNonNull(databaseClock, "databaseClock");
    }

    public List<UserBusinessAccessVO> accesses(Long tenantId, Long userId) {
        return enrich(store.accesses(
                Objects.requireNonNull(tenantId, "tenantId"),
                Objects.requireNonNull(userId, "userId")));
    }

    public List<UserBusinessAccessVO> replace(
            Long tenantId,
            Long userId,
            String actorId,
            ReplaceUserBusinessAccessesCommand command) {
        Objects.requireNonNull(command, "command");
        Long targetTenant = Objects.requireNonNull(tenantId, "tenantId");
        Long targetUser = Objects.requireNonNull(userId, "userId");
        String actor = required(actorId, "actorId");
        validateEveryBusinessIsEnabled(command.items());
        Instant now = databaseClock.transactionNow();
        MutationResultVO<List<UserBusinessAccessVO>> mutation = mutationCoordinator.execute(
                new MutationScopeVO(
                        targetTenant.toString(), "USER", targetUser.toString(),
                        command.commandId(), actor),
                targetUser.toString(),
                new ExpectedVersionsVO(null, null, null, null),
                () -> store.replaceManualAccesses(
                        targetTenant, targetUser, command.items(), actor, now));
        return enrich(Objects.requireNonNull(mutation.value(), "mutation.value"));
    }

    public List<UserApplicationAccessVO> applicationAccesses(
            Long tenantId,
            Long userId) {
        Long targetTenant = Objects.requireNonNull(tenantId, "tenantId");
        Long targetUser = Objects.requireNonNull(userId, "userId");
        Instant now = databaseClock.transactionNow();
        return store.applicationAccesses(targetTenant, targetUser, now).stream()
                .map(this::enrichApplication)
                .toList();
    }

    private void validateEveryBusinessIsEnabled(
            List<ReplaceUserBusinessAccessesCommand.Item> items) {
        for (ReplaceUserBusinessAccessesCommand.Item item : items) {
            BusinessCatalogEntry business = catalog.findBusiness(item.ddcBusinessId())
                    .orElseThrow(() -> new IllegalStateException(
                            "DDC Business is not available: " + item.ddcBusinessId()));
            if (!business.enabled()) {
                throw new IllegalStateException(
                        "DDC Business is disabled: " + item.ddcBusinessId());
            }
        }
    }

    private List<UserBusinessAccessVO> enrich(List<UserBusinessAccessVO> values) {
        return values.stream().map(value -> {
            BusinessCatalogEntry business = catalog.findBusiness(value.ddcBusinessId())
                    .orElseThrow(() -> new IllegalStateException(
                            "DDC Business is not available: " + value.ddcBusinessId()));
            return new UserBusinessAccessVO(
                    value.accessId(), value.userId(), value.ddcBusinessId(),
                    business.bizCode(), business.bizName(), value.status(),
                    value.validFrom(), value.validTo(), value.sourceType(),
                    value.sourceId(), value.reason(), value.ticketNo(), value.version());
        }).toList();
    }

    private UserApplicationAccessVO enrichApplication(UserApplicationAccessVO value) {
        ApplicationCatalogEntry application = catalog.findApplication(
                        value.ddcApplicationId())
                .orElseThrow(() -> new IllegalStateException(
                        "DDC Application is not available: " + value.ddcApplicationId()));
        if (!application.applicationEnabled() || !application.businessEnabled()) {
            throw new IllegalStateException(
                    "DDC Application or Business is disabled: " + value.ddcApplicationId());
        }
        return new UserApplicationAccessVO(
                value.applicationId(), value.ddcBusinessId(),
                value.ddcApplicationId(), application.bizCode(),
                application.appCode(), application.appName(),
                value.applicationStatus(), value.observedAt());
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
