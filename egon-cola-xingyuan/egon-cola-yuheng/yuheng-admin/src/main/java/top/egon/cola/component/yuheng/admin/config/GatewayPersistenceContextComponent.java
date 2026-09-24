package top.egon.cola.component.yuheng.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 中文说明：{@code GatewayPersistenceContextComponent} 是 Admin 单企业安装域的持久化身份上下文。它把已通过认证的
 * 身份声明与部署配置 {@code yuheng.persistence.identity-tenant-id} 比对（不一致或不可解析即失败关闭），随后在
 * <b>实际执行 SQL 的线程</b>上写入 MDC {@code tenantId}/{@code userId}，并在 finally 里原样恢复先前值——先前没有值就
 * 删除键，绝不让线程本地状态泄漏给下一个请求。写入的 {@code tenantId} 只可能是部署配置的
 * {@code yuheng.persistence.tenant-id}：数据源本身绑定单企业安装域，请求头/请求体/声明里的租户值永远不作为持久化租户的
 * 事实来源，声明只用来「门禁」，不用来「选库」。缺失或非法的部署值不可能到这里，因为
 * {@link GatewayPersistenceProperties} 的约束在绑定阶段就已失败关闭，这里<b>没有</b>任何默认租户（尤其不是 {@code 1}）。
 * English summary: {@code GatewayPersistenceContextComponent} is the persistence identity context of Admin's
 * single-enterprise installation domain. It compares the authenticated identity's claim with the deployment value
 * {@code yuheng.persistence.identity-tenant-id} (fail closed on mismatch or an unparseable claim), then installs the MDC
 * {@code tenantId}/{@code userId} keys on the thread that actually executes SQL and restores the previous values in a
 * finally block — removing a key that had no prior value so thread-local state never leaks to the next request. The
 * installed {@code tenantId} can only ever be the configured {@code yuheng.persistence.tenant-id}: the datasource itself
 * is bound to one enterprise installation, so tenant values from headers, bodies or claims are never a source of truth
 * for persistence tenancy and only ever gate entry. Missing or illegal deployment values cannot reach this class because
 * {@link GatewayPersistenceProperties} constraints already fail closed at binding, and there is <b>no</b> default
 * tenant here (in particular not {@code 1}).
 *
 * 用法 / Usage: 由 {@link GatewayPersistenceConfiguration} 以 bean 名 {@code gatewayPersistenceContextComponent}
 * 注册；受守卫读写一律 {@code context.call(claimedTenantId, () -> guardedWork)}，本组件不外泄任何 PO 或持久化对象。
 */
@Slf4j
@RequiredArgsConstructor
public final class GatewayPersistenceContextComponent {

    /** 中文说明：部署绑定身份；租户与服务审计主体都取自它，且已在绑定阶段完成必填/正数/非空白校验。 English summary: the deployment-bound identity; tenant and audit principal come from it and were validated at binding. */
    @Qualifier(GatewayPersistenceProperties.BEAN_NAME)
    private final GatewayPersistenceProperties persistenceProperties;

    /** 中文说明：组件侧 MDC 键配置，保证本组件写入的键与租户拦截器、审计填充读取的键同源。 English summary: component MDC key configuration, so the keys written here are the same ones the tenant interceptor and audit fill read. */
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties mybatisPlusProperties;

    /**
     * 中文说明：先比对身份声明租户，再在上下文内执行工作并在 finally 恢复 MDC；工作抛出的异常如实传播，
     * 不吞异常也不伪造成功。声明为空表示「身份不带租户」，按单企业安装域放行。
     * English summary: Compares the claimed identity tenant first, runs the work inside the context and restores the MDC
     * in a finally block; anything the work throws propagates unchanged — nothing is swallowed and no success is
     * fabricated. An absent claim means the identity carries no tenant and is served by the single-enterprise
     * installation domain.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPersistenceContextComponent.call(claimedIdentityTenantId, work)}。
     * @param claimedIdentityTenantId 参数 已验证身份自带的租户声明，可为空；parameter the tenant carried by the verified identity, possibly absent.
     * @param work 参数 需要在持久化身份内执行的工作；parameter the work that must run inside the persistence identity.
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
     * 中文说明：返回本部署已校验的安装域租户，供调用方做日志与范围核对；运行期租户判定仍以 MDC 为准。
     * English summary: Returns this deployment's validated installation tenant for logging and scope checks; runtime
     * tenancy decisions keep reading the MDC.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPersistenceContextComponent.installationTenantId()}。
     * @return 返回 部署配置的正数租户；returns the configured positive tenant.
     */
    public Long installationTenantId() {
        return persistenceProperties.getTenantId();
    }

    /**
     * 中文说明：身份声明必须等于部署绑定的受信任企业身份；不可解析的声明同样按不一致处理，因为「猜一个租户」
     * 比拒绝更危险。异常只携带 code，所以细节先落日志。
     * English summary: A claim must equal the deployment's trusted enterprise identity; an unparseable claim is treated as
     * a mismatch, because guessing a tenant is more dangerous than refusing. The exception carries only a code, so the
     * detail is logged first.
     *
     * 用法 / Usage: 内部辅助 / Internal helper: {@code GatewayPersistenceContextComponent.assertClaimedIdentityTenant(claim)}。
     * @param claimedIdentityTenantId 参数 身份声明租户，可为空；parameter the identity's claimed tenant, possibly absent.
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
