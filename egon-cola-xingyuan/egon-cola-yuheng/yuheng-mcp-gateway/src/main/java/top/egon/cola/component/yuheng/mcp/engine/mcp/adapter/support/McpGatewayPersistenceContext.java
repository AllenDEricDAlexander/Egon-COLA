package top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.support;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 中文说明：{@code McpGatewayPersistenceContext} 是数据面写路径的身份上下文包装器：MyBatis-Plus 的租户行拦截器、
 * 审计填充与写守卫都从 SLF4J MDC 读取 {@code tenantId}（数值、部署绑定）与 {@code userId}（技术主体），
 * 而 MCP 工作者的 Reactor 线程不携带这两个键，因此每次受守卫读写都在 boundedElastic 线程上临时置入、
 * 结束后原样恢复（含原本为空的键）。租户值来自配置，绝不从调用方自报的 {@code McpTask.tenantId()} 推导；
 * 调用方租户只落在 {@code subject_tenant_id} 列。
 * English summary: {@code McpGatewayPersistenceContext} is the identity-context wrapper for the data-plane write path:
 * the MyBatis-Plus tenant-line interceptor, the audit fill and the write guards all read {@code tenantId} (numeric and
 * deployment-bound) and {@code userId} (the technical principal) from the SLF4J MDC, while MCP worker Reactor threads
 * carry neither key. Every guarded read or write therefore installs them on the boundedElastic thread and restores them
 * exactly afterwards, including keys that were absent. The tenant value comes from configuration and is never derived
 * from the caller-reported {@code McpTask.tenantId()}; that value only ever lands in the {@code subject_tenant_id}
 * column.
 *
 * 用法 / Usage: 由 {@code McpGatewayEngineConfiguration} 以显式命名 Bean 注册并注入两个 MP 适配器，
 * 用法为 {@code context.call(() -> guardedWork)}；构造时校验配置（缺失即 fail closed），运行期不在上下文外
 * 触碰 MDC。/ Register it as an explicitly named bean from {@code McpGatewayEngineConfiguration} and inject it into both
 * MP adapters, using {@code context.call(() -> guardedWork)}; the constructor validates the configuration and fails
 * closed when a mandatory identity value is missing, and no MDC key is touched outside the context at runtime.
 */
@Slf4j
public class McpGatewayPersistenceContext {

    /** 中文说明：租户 MDC 键，必须与 {@code egon.cola.component.mybatis-plus.tenant-id.mdc-key} 的组件默认值一致。 English summary: the tenant MDC key, which must equal the component default of {@code egon.cola.component.mybatis-plus.tenant-id.mdc-key}. */
    private static final String TENANT_MDC_KEY = "tenantId";

    /** 中文说明：审计用户 MDC 键，必须与 {@code egon.cola.component.mybatis-plus.audit.user-id-mdc-key} 的组件默认值一致。 English summary: the audit-user MDC key, which must equal the component default of {@code egon.cola.component.mybatis-plus.audit.user-id-mdc-key}. */
    private static final String USER_MDC_KEY = "userId";

    /** 中文说明：部署绑定的数值租户标识字符串。 English summary: the deployment-bound numeric tenant identifier as a string. */
    private final String tenantId;

    /** 中文说明：写入审计列的技术主体标识。 English summary: the technical principal written into the audit columns. */
    private final String technicalPrincipal;

    /**
     * 中文说明：按配置构造上下文；租户必须是可解析的 {@code Long}，技术主体必须非空白，否则启动即失败而不是
     * 等到第一条语句抛 {@code TENANT_CONTEXT_MALFORMED}。
     * English summary: Builds the context from configuration; the tenant must parse as a {@code Long} and the technical
     * principal must be non-blank, so a missing mandatory identity value fails at startup instead of surfacing later as
     * {@code TENANT_CONTEXT_MALFORMED}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code new McpGatewayPersistenceContext(tenantId, technicalPrincipal)}。
     * @param tenantId 参数 部署数值租户；parameter the deployment numeric tenant.
     * @param technicalPrincipal 参数 技术审计主体；parameter the technical audit principal.
     */
    public McpGatewayPersistenceContext(String tenantId, String technicalPrincipal) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId").trim();
        this.technicalPrincipal = Objects.requireNonNull(technicalPrincipal, "technicalPrincipal").trim();
        if (this.tenantId.isEmpty()) {
            throw new IllegalStateException(
                    "MCP gateway persistence tenant id is required"
            );
        }
        try {
            Long.parseLong(this.tenantId);
        } catch (NumberFormatException invalid) {
            throw new IllegalStateException(
                    "MCP gateway persistence tenant id must be numeric",
                    invalid
            );
        }
        if (this.technicalPrincipal.isEmpty()) {
            throw new IllegalStateException(
                    "MCP gateway technical principal is required"
            );
        }
    }

    /**
     * 中文说明：在受守卫身份上下文内执行工作并在 finally 原样恢复先前的 MDC；工作抛出的异常如实传播。
     * English summary: Runs the work inside the guarded identity context and restores the previous MDC in a finally block;
     * any exception the work raises propagates unchanged.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code context.call(() -> guardedWork)}。
     * @param work 参数 受守卫工作；parameter the guarded work.
     * @param <T> 返回类型 / the return type.
     * @return 返回 工作结果；returns the work result.
     * @throws Exception 工作自身抛出的异常；an exception raised by the work itself.
     */
    public <T> T call(Callable<T> work) throws Exception {
        Objects.requireNonNull(work, "work");
        String previousTenant = MDC.get(TENANT_MDC_KEY);
        String previousUser = MDC.get(USER_MDC_KEY);
        MDC.put(TENANT_MDC_KEY, tenantId);
        MDC.put(USER_MDC_KEY, technicalPrincipal);
        try {
            return work.call();
        } finally {
            restore(TENANT_MDC_KEY, previousTenant);
            restore(USER_MDC_KEY, previousUser);
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
