package top.egon.cola.platform.rbac3.admin.tenant.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.tenant.repository.TenantLookupRepository;

import java.util.Optional;

/**
 * 基于 JPA 的租户编码查询仓储。
 * JPA repository for tenant-code lookup.
 */
@Repository
public class JpaTenantLookupRepository implements TenantLookupRepository {
    private final EntityManager entityManager;

    public JpaTenantLookupRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

/**
     * 方法 `findTenantByCode` 按照 `JpaTenantLookupRepository` 的职责处理输入，完成 `find Tenant By Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `findTenantByCode` processes its inputs according to `JpaTenantLookupRepository`'s responsibility, performs the `find Tenant By Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `findTenantByCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `findTenantByCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Transactional(readOnly = true)
    public Optional<TenantPO> findTenantByCode(String tenantCode) {
        return entityManager.createQuery(
                        "select t from TenantEntity t where lower(t.code) = :code",
                        TenantPO.class)
                .setParameter("code", tenantCode.toLowerCase(java.util.Locale.ROOT))
                .getResultStream()
                .findFirst();
    }

}
