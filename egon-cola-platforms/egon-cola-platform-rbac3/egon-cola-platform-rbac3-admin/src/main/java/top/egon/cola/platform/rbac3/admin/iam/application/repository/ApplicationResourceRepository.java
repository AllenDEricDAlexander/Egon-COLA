package top.egon.cola.platform.rbac3.admin.iam.application.repository;

import top.egon.cola.platform.rbac3.admin.iam.application.domain.vo.ApplicationAuthorizationScopeVO;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;

import java.util.List;
import java.util.Optional;

/**
 * Application aggregate repository. It also exposes the existing resource-query
 * port so the manifest repository remains the single local Application store.
 */
public interface ApplicationResourceRepository
        extends top.egon.cola.platform.rbac3.admin.iam.resource.repository.ApplicationResourceRepository {

    List<ApplicationAuthorizationScopeVO> authorizationScopes(Long tenantId);

    Optional<ApplicationAuthorizationScopeVO> authorizationScope(
            Long tenantId,
            Long applicationId);

    ApplicationAuthorizationScopeVO admit(
            Long tenantId,
            ApplicationCatalogEntry catalog,
            int displayPriority,
            String actorId);

    ApplicationAuthorizationScopeVO changeStatus(
            Long tenantId,
            Long applicationId,
            String status,
            long expectedVersion,
            String actorId);

    void remove(
            Long tenantId,
            Long applicationId,
            long expectedVersion,
            String actorId);
}
