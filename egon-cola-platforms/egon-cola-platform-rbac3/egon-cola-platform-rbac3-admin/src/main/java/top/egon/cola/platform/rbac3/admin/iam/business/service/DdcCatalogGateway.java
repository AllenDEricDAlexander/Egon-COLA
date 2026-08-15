package top.egon.cola.platform.rbac3.admin.iam.business.service;

import java.util.List;
import java.util.Optional;

/**
 * Narrow read-only boundary from RBAC3 to the DDC Business/Application catalog.
 * RBAC3 never writes master data through this port.
 */
public interface DdcCatalogGateway {

    Optional<BusinessCatalogEntry> findBusiness(String ddcBusinessId);

    List<BusinessCatalogEntry> listBusinesses(String keyword);

    Optional<ApplicationCatalogEntry> findApplication(String ddcApplicationId);

    List<ApplicationCatalogEntry> listApplications(
            String ddcBusinessId,
            String keyword);
}
