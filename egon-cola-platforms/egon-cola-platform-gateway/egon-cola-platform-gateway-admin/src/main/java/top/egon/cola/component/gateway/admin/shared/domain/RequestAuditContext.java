package top.egon.cola.component.gateway.admin.shared.domain;


import top.egon.cola.component.common.trace.TraceContext;

/**
 * 中文说明：{@code RequestAuditContext} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责请求审计Context相关的职责与边界。
 * English summary: {@code RequestAuditContext} is an immutable data carrier in the current Gateway module; it owns the request audit context-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param requestId 参数 请求Id；parameter request id。
 * @param traceId 参数 traceId；parameter trace id。
 */
public record RequestAuditContext(
    /**
     * 中文说明：保存 请求Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RequestAuditContext} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by request id; its type is {@code String}, and {@code RequestAuditContext} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RequestAuditContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RequestAuditContext}; do not couple callers to its representation when the owning type exposes an API.
     */
    String requestId,
    /**
     * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RequestAuditContext} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code RequestAuditContext} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RequestAuditContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RequestAuditContext}; do not couple callers to its representation when the owning type exposes an API.
     */
    String traceId) {

    /**
     * 中文说明：执行 current 操作；该方法是 {@code RequestAuditContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code RequestAuditContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RequestAuditContext.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    public static RequestAuditContext current() {
        return current(null);
    }

    /**
     * 中文说明：执行 current 操作；该方法是 {@code RequestAuditContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code RequestAuditContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RequestAuditContext.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param requestId 参数 请求Id；parameter request id。
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    public static RequestAuditContext current(String requestId) {
        TraceContext context = TraceContext.currentOrCreate();
        String resolvedRequestId = safe(requestId)
                ? requestId.trim()
                : context.requestId() == null
                ? context.traceId()
                : context.requestId();
        return new RequestAuditContext(resolvedRequestId, context.traceId());
    }

    /**
     * 中文说明：执行 safe 操作；该方法是 {@code RequestAuditContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe operation; this method is the invocation entry point on {@code RequestAuditContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RequestAuditContext.safe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safe 的处理结果；returns the result of the operation.
     */
    private static boolean safe(String value) {
        return value != null
                && !value.isBlank()
                && value.indexOf('\r') < 0
                && value.indexOf('\n') < 0;
    }
}
