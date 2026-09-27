package top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.yuheng.mcp.engine.config.McpPersistenceContextComponent;
import top.egon.cola.component.yuheng.mcp.engine.config.McpPersistenceProperties;

import java.util.Objects;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;

/**
 * 中文说明：{@code McpGatewayPersistenceContext} 是旧 store API 的薄适配器，所有租户核对、MDC 安装与恢复都委托
 * 给 {@link McpPersistenceContextComponent}；部署身份唯一来自 {@link McpPersistenceProperties} 的
 * {@code yuheng.persistence.*}，不再读取旧 {@code egon.cola.component.yuheng.engine.mcp.persistence.*} 键。
 * English summary: {@code McpGatewayPersistenceContext} is a thin adapter for the existing store API; tenant validation,
 * MDC installation and restoration all delegate to {@link McpPersistenceContextComponent}. Deployment identity has one
 * source in {@link McpPersistenceProperties} under {@code yuheng.persistence.*}; the old
 * {@code egon.cola.component.yuheng.engine.mcp.persistence.*} keys are no longer read.
 *
 * 用法 / Usage: 由 {@code McpGatewayEngineConfiguration} 以显式命名 Bean 注册并注入 MP 适配器，执行
 * {@code context.call(() -> guardedWork)}；持久化用户通过 {@link #serviceUserId()} 取自同一配置对象。
 */
@Slf4j
@RequiredArgsConstructor
public class McpGatewayPersistenceContext {

    @Qualifier("mcpPersistenceContextComponent")
    private final McpPersistenceContextComponent persistenceContextComponent;

    @Qualifier(McpPersistenceProperties.BEAN_NAME)
    private final McpPersistenceProperties persistenceProperties;

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
        return persistenceContextComponent.call(null, work);
    }

    /** Returns the configured technical principal used by explicit Mapper audit fields. */
    public String serviceUserId() {
        return persistenceProperties.getServiceUserId();
    }
}
