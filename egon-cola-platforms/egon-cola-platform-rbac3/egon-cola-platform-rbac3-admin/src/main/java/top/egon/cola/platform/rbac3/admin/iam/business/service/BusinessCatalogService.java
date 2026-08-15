package top.egon.cola.platform.rbac3.admin.iam.business.service;

import java.util.List;
import java.util.Objects;

/** Application-facing read service for DDC Business/Application master data. */
public final class BusinessCatalogService {

    private final DdcCatalogGateway catalog;

    public BusinessCatalogService(DdcCatalogGateway catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public List<BusinessCatalogEntry> businesses(String keyword) {
        return List.copyOf(catalog.listBusinesses(keyword));
    }

    public List<ApplicationCatalogEntry> applications(
            String ddcBusinessId,
            String keyword) {
        return List.copyOf(catalog.listApplications(ddcBusinessId, keyword));
    }
}
