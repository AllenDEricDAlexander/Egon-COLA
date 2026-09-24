package top.egon.cola.component.yuheng.mcp.engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 中文说明：{@code McpPersistenceContextComponent} 是 MCP 引擎作为 schema 消费者的持久化身份入口，职责只有两条：
 * 先按部署配置 {@code yuheng.persistence.identity-tenant-id} 核对应被信任身份的租户（不一致或不可解析即失败关闭，
 * 绝不切换安装域），再在<b>实际执行 SQL 的线程</b>（Reactor/boundedElastic）上写入 MDC
 * {@code tenantId}/{@code userId} 并在 finally 原样恢复先前值（先前无值即删除键）。写入的租户只可能来自
 * {@code yuheng.persistence.tenant-id}：调用方自报的 {@code McpTask.tenantId()} 只落在
 * {@code subject_tenant_id} 列并继续参与 token/task 所有权比较，永不被当作持久化租户。
 * 身份缺项/空白/非正数已在 {@link McpPersistenceProperties} 绑定阶段失败关闭，这里没有任何默认租户兜底，
 * 也不外泄 PO 或持久化对象。
 * English summary: {@code McpPersistenceContextComponent} is the persistence identity entry point of the MCP engine as a
 * schema consumer, with exactly two duties: compare the tenant carried by an already trusted identity against the
 * deployment value {@code yuheng.persistence.identity-tenant-id} (fail closed on mismatch or an unparseable claim, never
 * switching the installation domain), then install MDC {@code tenantId}/{@code userId} on the thread that actually runs
 * SQL (Reactor/boundedElastic) and restore the previous values in a finally block, removing keys that had none. The
 * installed tenant can only come from {@code yuheng.persistence.tenant-id}: a caller-reported
 * {@code McpTask.tenantId()} only ever lands in the {@code subject_tenant_id} column and keeps feeding token/task
 * ownership comparison, never persistence tenancy. Missing, blank or non-positive identity values already failed closed
 * while binding {@link McpPersistenceProperties}; no default tenant exists here and no PO or persistence object leaks.
 *
 * 用法 / Usage: 由 {@link McpPersistenceConfiguration} 以 bean 名 {@code mcpPersistenceContextComponent} 注册；
 * 非 HTTP 执行路径在真正跑 SQL 的线程内调用 {@code component.call(claimedTenantId, () -> guardedWork)}，
 * 无请求身份时传 {@code null}。
 */
@Slf4j
@RequiredArgsConstructor
public final class McpPersistenceContextComponent {

    /** 中文说明：本部署的持久化身份配置，安装域租户与技术审计主体的唯一来源。 English summary: this installation's persistence identity, the only source of the installation tenant and technical principal. */
    @Qualifier(McpPersistenceProperties.BEAN_NAME)
    private final McpPersistenceProperties persistenceProperties;

    /** 中文说明：组件侧 MDC 键配置，保证写入键与租户拦截器/审计填充读取键同源。 English summary: component MDC key configuration, so written keys match the ones the tenant interceptor and audit fill read. */
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties mybatisPlusProperties;

    /**
     * 中文说明：核对身份声明租户后安装上下文并执行工作，finally 恢复 MDC；工作抛出的异常如实传播，
     * 不吞异常也不伪造成功。声明为空表示「身份不带租户」，按单企业安装域放行。
     * English summary: verifies the claimed identity tenant, then installs the context, runs the work and restores the MDC
     * in a finally block; anything the work throws propagates unchanged. An absent claim means the identity carries no
     * tenant and is admitted as the single-enterprise installation domain.
     *
     * 用法 / Usage: {@code component.call(claimedIdentityTenantId, () -> guardedWork)}。
     * @param claimedIdentityTenantId 参数 已验证身份自带的租户，可为 {@code null}；parameter the tenant carried by the verified identity, may be {@code null}.
     * @param work 参数 需要在受守卫身份内执行的工作；parameter the work that must run inside the guarded identity.
     * @param <T> 工作的返回类型 / the work's return type.
     * @return 返回 工作自身的结果；returns whatever the work itself produces.
     * @throws Exception 工作自身抛出的异常；an exception raised by the work itself.
     */
    public <T> T call(String claimedIdentityTenantId, Callable<T> work) throws Exception {
        Objects.requireNonNull(work, "work");
        assertClaimedIdentityTenant(claimedIdentityTenantId);
        String tenantKey = mybatisPlusProperties.getTenantId().getMdcKey();
        String userKey = mybatisPlusProperties.getAudit().getUserIdMdcKey();
        String previousTenant = MDC.get(tenantKey);
        String previousUser = MDC.get(userKey);
        MDC.put(tenantKey, String.valueOf(persistenceProperties.getTenantId()));
        MDC.put(userKey, persistenceProperties.getServiceUserId());
        try {
            return work.call();
        } finally {
            restore(tenantKey, previousTenant);
            restore(userKey, previousUser);
        }
    }

    /**
     * 中文说明：返回本部署已校验的安装域租户，供日志与范围核对使用；运行期租户判定仍以 MDC 为准。
     * English summary: Returns this deployment's validated installation tenant for logging and scope checks; runtime
     * tenancy decisions keep reading the MDC.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPersistenceContextComponent.installationTenantId()}。
     * @return 返回 部署配置的正数租户；returns the configured positive tenant.
     */
    public Long installationTenantId() {
        return persistenceProperties.getTenantId();
    }

    /**
     * 中文说明：身份声明必须等于部署绑定的受信任企业身份；不可解析的声明按不一致处理，因为「猜一个租户」比拒绝更危险。
     * 异常只携带 code，故细节先落日志。
     * English summary: a claim must equal the deployment's trusted enterprise identity, and an unparseable claim counts as a
     * mismatch because guessing a tenant is more dangerous than refusing. The exception carries only a code, so the detail
     * is logged first.
     *
     * 用法 / Usage: 内部守卫 / Internal guard: {@code assertClaimedIdentityTenant(claimedIdentityTenantId)}.
     * @param claimedIdentityTenantId 参数 已验证身份自带的租户，可为 {@code null}；parameter the tenant carried by the verified identity, may be {@code null}.
     */
    private void assertClaimedIdentityTenant(String claimedIdentityTenantId) {
        if (claimedIdentityTenantId == null || claimedIdentityTenantId.isBlank()) {
            return;
        }
        long trusted;
        try {
            trusted = Long.parseLong(claimedIdentityTenantId.trim());
        } catch (NumberFormatException unparseable) {
            log.error("identity tenant claim '{}' is not a tenant id, refusing to derive persistence tenancy",
                    claimedIdentityTenantId);
            throw new EgonColaMybatisPlusConfigurationException("PERSISTENCE_IDENTITY_TENANT_MISMATCH", unparseable);
        }
        if (!Objects.equals(persistenceProperties.getIdentityTenantId(), trusted)) {
            log.error("identity tenant claim {} does not equal the deployment identity tenant {}",
                    trusted, persistenceProperties.getIdentityTenantId());
            throw new EgonColaMybatisPlusConfigurationException("PERSISTENCE_IDENTITY_TENANT_MISMATCH");
        }
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, previous);
        }
    }
}
