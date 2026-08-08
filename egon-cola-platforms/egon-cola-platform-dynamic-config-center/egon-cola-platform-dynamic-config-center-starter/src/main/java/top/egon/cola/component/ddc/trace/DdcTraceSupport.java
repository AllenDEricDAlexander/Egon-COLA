package top.egon.cola.component.ddc.trace;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import top.egon.cola.component.common.trace.TraceContext;

/**
 * DDC 调用链与日志上下文辅助工具。
 * Helper for DDC tracing and logging context.
 *
 * <p>本类在公共 {@link TraceContext} 之上补充 DDC 组件标识和操作名称，并提供 HTTP 请求头注入、
 * 同步作用域开启及异步任务上下文传播能力。所有作用域都应通过 try-with-resources 关闭，以恢复执行线程
 * 原有的 Trace 和 MDC 内容。</p>
 *
 * <p>This class augments the shared {@link TraceContext} with DDC component and operation identifiers, and provides
 * HTTP header injection, synchronous scope creation, and asynchronous context propagation. Every scope should be
 * closed with try-with-resources to restore the executing thread's original trace and MDC values.</p>
 */
public final class DdcTraceSupport {

    /** MDC 中标识当前组件的键。 MDC key identifying the current component. */
    private static final String COMPONENT_KEY = "component";

    /** MDC 中标识当前 DDC 操作的键。 MDC key identifying the current DDC operation. */
    private static final String OPERATION_KEY = "operation";

    /** 工具类不允许实例化。 Prevents utility-class instantiation. */
    private DdcTraceSupport() {
    }

    /**
     * 将当前调用链的子上下文注入 HTTP 请求头。
     * Injects a child of the current trace context into HTTP request headers.
     *
     * <p>如果当前线程没有完整 Trace，则先创建根上下文；随后创建子 Span，并写入 W3C
     * {@code traceparent}、可选 {@code tracestate} 和请求编号等请求头。</p>
     *
     * <p>If the current thread has no complete trace, a root context is created first. A child span is then created
     * and W3C {@code traceparent}, optional {@code tracestate}, and request identifiers are written to the headers.</p>
     *
     * @param headers 待写入调用链信息的 HTTP 请求头; HTTP request headers receiving trace information
     */
    public static void inject(HttpHeaders headers) {
        TraceContext.currentOrCreate().child().inject(headers::set);
    }

    /**
     * 捕获当前线程的 Trace 和 MDC 快照；当前线程没有完整 Trace 时创建一个根上下文。
     * Captures the current thread's trace and MDC snapshot, creating a root context when no complete trace exists.
     *
     * <p>返回的上下文不会自动安装到当前线程，通常用于保存后交给 {@link #wrapContext} 跨线程恢复。</p>
     * <p>The returned context is not installed automatically and is typically saved for cross-thread restoration by {@link #wrapContext}.</p>
     *
     * @return 当前 Trace 快照或新创建的根上下文; current trace snapshot or newly created root context
     */
    public static TraceContext captureOrCreate() {
        return TraceContext.currentOrCreate();
    }

    /**
     * 基于当前线程的 Trace 开启一个 DDC 操作作用域。
     * Opens a DDC operation scope based on the current thread's trace.
     *
     * <p>当前线程没有完整 Trace 时会创建根上下文；作用域内 MDC 的 {@code component} 固定为
     * {@code ddc}，{@code operation} 设置为传入的操作名称。</p>
     *
     * <p>A root context is created when the thread has no complete trace. Within the scope, MDC {@code component}
     * is fixed to {@code ddc}, while {@code operation} is set to the supplied operation name.</p>
     *
     * @param operation DDC 操作名称；为空时移除当前作用域内的操作标识; DDC operation name, removing the scoped identifier when blank
     * @return 关闭后可恢复原 Trace 和 MDC 的作用域; scope restoring the original trace and MDC when closed
     */
    public static Scope openOperation(String operation) {
        return new Scope(
                TraceContext.currentOrCreate().open(),
                operation
        );
    }

    /**
     * 安装指定 Trace 上下文并开启一个 DDC 操作作用域。
     * Installs the specified trace context and opens a DDC operation scope.
     *
     * @param context   需要安装到当前线程的 Trace 和 MDC 快照; trace and MDC snapshot to install on the current thread
     * @param operation DDC 操作名称；为空时移除当前作用域内的操作标识; DDC operation name, removing the scoped identifier when blank
     * @return 关闭后可恢复线程原有 Trace 和 MDC 的作用域; scope restoring the thread's original trace and MDC when closed
     */
    public static Scope openContext(TraceContext context,
                                    String operation) {
        return new Scope(context.open(), operation);
    }

    /**
     * 包装一个在执行线程中开启新 DDC 操作作用域的任务。
     * Wraps a task that opens a new DDC operation scope on its executing thread.
     *
     * <p>该方法不会捕获调用方线程上下文。任务实际执行时使用执行线程已有的 Trace；如果执行线程没有
     * 完整 Trace，则创建新的根上下文。需要传播调用方上下文时应使用 {@link #wrapContext}。</p>
     *
     * <p>This method does not capture the caller's context. At execution time, it uses the executing thread's trace
     * or creates a new root context. Use {@link #wrapContext} when the caller's context must be propagated.</p>
     *
     * @param operation DDC 操作名称; DDC operation name
     * @param runnable  需要包装的任务; task to wrap
     * @return 带有自动开启和关闭操作作用域能力的任务; task that automatically opens and closes the operation scope
     */
    public static Runnable wrapNewOperation(String operation,
                                            Runnable runnable) {
        return () -> {
            try (Scope ignored = openOperation(operation)) {
                runnable.run();
            }
        };
    }

    /**
     * 包装一个在执行期间恢复指定 Trace 上下文的任务。
     * Wraps a task that restores the specified trace context while executing.
     *
     * <p>任务执行结束后会恢复执行线程原有的 Trace 和 MDC，适用于 ACK 投递等跨线程异步操作。</p>
     * <p>The executing thread's original trace and MDC are restored afterward, making this suitable for asynchronous work such as ACK delivery.</p>
     *
     * @param context   需要传播到执行线程的 Trace 和 MDC 快照; trace and MDC snapshot to propagate
     * @param operation DDC 操作名称; DDC operation name
     * @param runnable  需要包装的任务; task to wrap
     * @return 带有上下文安装和恢复能力的任务; task that installs and restores context
     */
    public static Runnable wrapContext(TraceContext context,
                                       String operation,
                                       Runnable runnable) {
        return () -> {
            try (Scope ignored = openContext(context, operation)) {
                runnable.run();
            }
        };
    }

    /**
     * 将指定 MDC 键恢复为进入作用域前的值。
     * Restores an MDC key to its value before entering the scope.
     *
     * @param key   MDC 键; MDC key
     * @param value 原有值；为空时删除该键; original value, removing the key when null
     */
    private static void restore(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    /**
     * DDC Trace 和 MDC 操作作用域。
     * DDC trace and MDC operation scope.
     *
     * <p>创建时安装 Trace 上下文并设置 DDC 组件与操作标识，关闭时恢复进入作用域之前的内容。</p>
     * <p>Construction installs trace context and DDC identifiers; closing restores values from before the scope.</p>
     */
    public static final class Scope implements AutoCloseable {

        /**
         * 公共 TraceContext 创建的底层作用域，负责恢复完整 MDC 快照。
         * Underlying shared TraceContext scope restoring the complete MDC snapshot.
         */
        private final TraceContext.Scope traceScope;

        /** 安装 Trace 上下文后、覆盖 DDC 标识前的组件值。 Component value after trace installation and before the DDC override. */
        private final String previousComponent;

        /** 安装 Trace 上下文后、覆盖 DDC 标识前的操作值。 Operation value after trace installation and before the DDC override. */
        private final String previousOperation;

        /**
         * 创建 DDC 操作作用域并写入组件、操作 MDC 字段。
         * Creates a DDC operation scope and writes component and operation MDC fields.
         *
         * @param traceScope 已安装 Trace 上下文的底层作用域; underlying scope with installed trace context
         * @param operation  DDC 操作名称；为空时删除操作标识; DDC operation name, removing the identifier when blank
         */
        private Scope(TraceContext.Scope traceScope, String operation) {
            this.traceScope = traceScope;
            this.previousComponent = MDC.get(COMPONENT_KEY);
            this.previousOperation = MDC.get(OPERATION_KEY);
            MDC.put(COMPONENT_KEY, "ddc");
            if (operation == null || operation.isBlank()) {
                MDC.remove(OPERATION_KEY);
            } else {
                MDC.put(OPERATION_KEY, operation);
            }
        }

        /**
         * 恢复 DDC 组件与操作标识，并关闭底层 Trace 作用域以恢复线程原有 MDC。
         * Restores DDC component and operation identifiers and closes the trace scope to restore the thread's original MDC.
         */
        @Override
        public void close() {
            restore(OPERATION_KEY, previousOperation);
            restore(COMPONENT_KEY, previousComponent);
            traceScope.close();
        }
    }
}
