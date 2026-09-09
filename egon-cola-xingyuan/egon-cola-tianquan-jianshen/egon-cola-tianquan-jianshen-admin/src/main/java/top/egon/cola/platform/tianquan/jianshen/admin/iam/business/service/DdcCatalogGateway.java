package top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service;

import java.util.List;
import java.util.Optional;

/**
 * Narrow read-only boundary from Tianquan-Jianshen to the Tianshu Business/Application catalog.
 * Tianquan-Jianshen never writes master data through this port.
 */
public interface DdcCatalogGateway {

    Optional<BusinessCatalogEntry> findBusiness(String ddcBusinessId);

    List<BusinessCatalogEntry> listBusinesses(String keyword);

    Optional<ApplicationCatalogEntry> findApplication(String ddcApplicationId);

    List<ApplicationCatalogEntry> listApplications(
            String ddcBusinessId,
            String keyword);
}
