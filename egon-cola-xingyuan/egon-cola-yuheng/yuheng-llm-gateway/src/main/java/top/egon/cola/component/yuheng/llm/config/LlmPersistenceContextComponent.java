package top.egon.cola.component.yuheng.llm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 中文说明：{@code LlmPersistenceContextComponent} 是 LLM 只读数据面的持久化身份上下文：把部署的
 * {@code yuheng.persistence.tenant-id} 写入 MDC {@code tenantId}、把 {@code service-user-id} 写入 {@code userId}，
 * 只在实际执行语句的线程上生效，并在 finally 原样恢复先前值（先前无值即删除键），因此线程复用不会把上一个调用的
 * 租户带给下一个调用。本组件<b>不接受</b>调用方传入的租户，也不提供任何写路径入口：LLM 角色的数据库账号只有
 * channel/model 的 SELECT 授权，知识写与 DDL 一律由 Admin 承担；租户来自部署配置而非请求，正是为了让越权请求
 * 连「选哪个租户」的机会都没有。身份缺项/空白/非正数已在 {@link LlmPersistenceProperties} 绑定阶段失败关闭，
 * 这里没有任何默认租户兜底，也不外泄 PO 或持久化对象。
 * English summary: {@code LlmPersistenceContextComponent} is the persistence identity context of the LLM read-only data
 * plane: it installs the deployment's {@code yuheng.persistence.tenant-id} as MDC {@code tenantId} and
 * {@code service-user-id} as {@code userId} on the thread that actually runs the statement and restores previous values
 * in a finally block, removing keys that had none, so thread reuse cannot carry one call's tenant into the next. It
 * <b>refuses</b> caller-supplied tenancy and offers no write entry point: the LLM database role holds only SELECT grants
 * on channel/model, while knowledge writes and DDL belong to Admin alone. Tenancy comes from deployment configuration
 * rather than from a request precisely so a privileged caller never even gets a chance to choose a tenant. Missing,
 * blank or non-positive identity values already failed closed while binding {@link LlmPersistenceProperties}; no default
 * tenant exists here, and neither a PO nor a persistence object leaks out.
 *
 * 用法 / Usage: 由 {@link LlmPersistenceConfiguration} 以 bean 名 {@code llmPersistenceContextComponent} 注册，
 * 读路径一律 {@code context.call(() -> guardedRead)}，进入点必须在真正执行 SQL 的线程内。
 */
@Slf4j
@RequiredArgsConstructor
public final class LlmPersistenceContextComponent {

    /** 中文说明：部署绑定身份，租户与技术审计主体都取自它。 English summary: the deployment-bound identity supplying tenant and technical audit principal. */
    @Qualifier(LlmPersistenceProperties.BEAN_NAME)
    private final LlmPersistenceProperties persistenceProperties;

    /** 中文说明：组件侧 MDC 键配置，保证写入键与租户拦截器/审计填充读取键同源。 English summary: component MDC key configuration, so written keys match the ones the tenant interceptor and audit fill read. */
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties mybatisPlusProperties;

    /**
     * 中文说明：在部署持久化身份内执行读取工作，finally 恢复 MDC；工作抛出的异常如实传播，不吞异常也不伪造空结果。
     * English summary: Runs the read work inside the deployment persistence identity and restores the MDC in a finally
     * block; anything the work throws propagates unchanged, so no failure is swallowed and no empty result is fabricated.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmPersistenceContextComponent.call(work)}。
     * @param work 参数 需要在受守卫身份内执行的工作；parameter the work that must run inside the guarded identity.
     * @param <T> 工作的返回类型 / the work's return type.
     * @return 返回 工作自身的结果；returns whatever the work itself produces.
     * @throws Exception 工作自身抛出的异常；an exception raised by the work itself.
     */
    public <T> T call(Callable<T> work) throws Exception {
        Objects.requireNonNull(work, "work");
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
     * 用法 / Usage: 调用方式 / Usage: {@code LlmPersistenceContextComponent.installationTenantId()}。
     * @return 返回 部署配置的正数租户；returns the configured positive tenant.
     */
    public Long installationTenantId() {
        return persistenceProperties.getTenantId();
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, previous);
        }
    }
}
