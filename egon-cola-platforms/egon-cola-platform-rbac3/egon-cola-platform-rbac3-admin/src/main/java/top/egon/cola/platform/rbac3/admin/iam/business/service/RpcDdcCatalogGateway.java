package top.egon.cola.platform.rbac3.admin.iam.business.service;

import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcManagementAppQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementBizLookup;
import top.egon.cola.component.ddc.model.management.DdcManagementBizQuery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * DDC Management RPC adapter for the RBAC3 catalog boundary.
 * The client is owned by application configuration and is not created per request.
 */
public final class RpcDdcCatalogGateway implements DdcCatalogGateway {

    private final DdcManagementClient client;

    public RpcDdcCatalogGateway(DdcManagementClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public Optional<BusinessCatalogEntry> findBusiness(String ddcBusinessId) {
        if (isBlank(ddcBusinessId)) {
            return Optional.empty();
        }
        return client.getBiz(new DdcManagementBizLookup(ddcBusinessId, null))
                .map(value -> new BusinessCatalogEntry(
                        value.id(), value.bizCode(), value.bizName(), value.enabled()));
    }

    @Override
    public List<BusinessCatalogEntry> listBusinesses(String keyword) {
        return client.listBizs(new DdcManagementBizQuery(normalize(keyword), null))
                .stream()
                .map(value -> new BusinessCatalogEntry(
                        value.id(), value.bizCode(), value.bizName(), value.enabled()))
                .toList();
    }

    @Override
    public Optional<ApplicationCatalogEntry> findApplication(String ddcApplicationId) {
        if (isBlank(ddcApplicationId)) {
            return Optional.empty();
        }
        return client.getApp(ddcApplicationId).map(this::toApplication);
    }

    @Override
    public List<ApplicationCatalogEntry> listApplications(
            String ddcBusinessId,
            String keyword) {
        return client.listApps(new DdcManagementAppQuery(
                        normalize(ddcBusinessId), null, normalize(keyword), null))
                .stream()
                .map(this::toApplication)
                .toList();
    }

    private ApplicationCatalogEntry toApplication(
            top.egon.cola.component.ddc.model.management.DdcManagementApp value) {
        return new ApplicationCatalogEntry(
                value.id(), value.businessId(), value.bizCode(), value.appCode(),
                value.appName(), value.enabled(), value.businessEnabled());
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
