package top.egon.cola.platform.rbac3.admin.iam.application.service;

import top.egon.cola.platform.rbac3.admin.iam.application.domain.command.AdmitApplicationAuthorizationScopeCommand;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.command.ChangeApplicationAuthorizationScopeStatusCommand;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.vo.ApplicationAuthorizationScopeVO;
import top.egon.cola.platform.rbac3.admin.iam.application.repository.ApplicationResourceRepository;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.DdcCatalogGateway;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates DDC-backed Application admission and the local RBAC scope lifecycle.
 */
public final class ApplicationScopeFacade {

    private final DdcCatalogGateway catalog;
    private final ApplicationResourceRepository applicationStore;

    public ApplicationScopeFacade(
            DdcCatalogGateway catalog,
            ApplicationResourceRepository applicationStore) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.applicationStore = Objects.requireNonNull(applicationStore, "applicationStore");
    }

    public ApplicationAuthorizationScopeVO admit(
            Long tenantId,
            String actorId,
            AdmitApplicationAuthorizationScopeCommand command) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(command, "command");
        ApplicationCatalogEntry entry = requiredEnabledApplication(command.ddcApplicationId());
        return applicationStore.admit(
                tenantId, entry, command.displayPriority(), required(actorId, "actorId"));
    }

    public List<ApplicationAuthorizationScopeVO> applications(Long tenantId) {
        return List.copyOf(applicationStore.authorizationScopes(
                Objects.requireNonNull(tenantId, "tenantId")));
    }

    public ApplicationAuthorizationScopeVO application(
            Long tenantId,
            Long applicationId) {
        return applicationStore.authorizationScope(
                        Objects.requireNonNull(tenantId, "tenantId"),
                        Objects.requireNonNull(applicationId, "applicationId"))
                .orElseThrow(() -> new IllegalStateException("application scope not found"));
    }

    public ApplicationAuthorizationScopeVO changeStatus(
            Long tenantId,
            Long applicationId,
            String actorId,
            ChangeApplicationAuthorizationScopeStatusCommand command) {
        Objects.requireNonNull(command, "command");
        return applicationStore.changeStatus(
                Objects.requireNonNull(tenantId, "tenantId"),
                Objects.requireNonNull(applicationId, "applicationId"),
                required(command.status(), "status"),
                command.expectedVersion(),
                required(actorId, "actorId"));
    }

    public void remove(
            Long tenantId,
            Long applicationId,
            long expectedVersion,
            String actorId) {
        if (expectedVersion < 0L) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        applicationStore.remove(
                Objects.requireNonNull(tenantId, "tenantId"),
                Objects.requireNonNull(applicationId, "applicationId"),
                expectedVersion,
                required(actorId, "actorId"));
    }

    private ApplicationCatalogEntry requiredEnabledApplication(String ddcApplicationId) {
        String id = required(ddcApplicationId, "ddcApplicationId");
        Optional<ApplicationCatalogEntry> entry = catalog.findApplication(id);
        if (entry.isEmpty()) {
            throw new IllegalStateException("DDC application is not available");
        }
        ApplicationCatalogEntry value = entry.get();
        if (!value.applicationEnabled() || !value.businessEnabled()) {
            throw new IllegalStateException("DDC application or business is disabled");
        }
        return value;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
