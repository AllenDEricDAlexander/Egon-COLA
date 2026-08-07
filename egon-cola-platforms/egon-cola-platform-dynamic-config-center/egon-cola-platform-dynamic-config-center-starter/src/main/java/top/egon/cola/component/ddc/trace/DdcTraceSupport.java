package top.egon.cola.component.ddc.trace;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import top.egon.cola.component.common.trace.TraceContext;

/**
 * DDC 调用链与日志上下文辅助工具。
 *
 * <p>本类在公共 {@link TraceContext} 之上补充 DDC 组件标识和操作名称，并提供 HTTP 请求头注入、
 * 同步作用域开启及异步任务上下文传播能力。所有作用域都应通过 try-with-resources 关闭，以恢复执行线程
 * 原有的 Trace 和 MDC 内容。</p>
 */
public final class DdcTraceSupport {

    /** MDC 中标识当前组件的键。 */
    private static final String COMPONENT_KEY = "component";

    /** MDC 中标识当前 DDC 操作的键。 */
    private static final String OPERATION_KEY = "operation";

    /** 工具类不允许实例化。 */
    private DdcTraceSupport() {
    }

    /**
     * 将当前调用链的子上下文注入 HTTP 请求头。
     *
     * <p>如果当前线程没有完整 Trace，则先创建根上下文；随后创建子 Span，并写入 W3C
     * {@code traceparent}、可选 {@code tracestate} 和请求编号等请求头。</p>
     *
     * @param headers 待写入调用链信息的 HTTP 请求头
     */
    public static void inject(HttpHeaders headers) {
        TraceContext.currentOrCreate().child().inject(headers::set);
    }

    /**
     * 捕获当前线程的 Trace 和 MDC 快照；当前线程没有完整 Trace 时创建一个根上下文。
     *
     * <p>返回的上下文不会自动安装到当前线程，通常用于保存后交给 {@link #wrapContext} 跨线程恢复。</p>
     *
     * @return 当前 Trace 快照或新创建的根上下文
     */
    public static TraceContext captureOrCreate() {
        return TraceContext.currentOrCreate();
    }

    /**
     * 基于当前线程的 Trace 开启一个 DDC 操作作用域。
     *
     * <p>当前线程没有完整 Trace 时会创建根上下文；作用域内 MDC 的 {@code component} 固定为
     * {@code ddc}，{@code operation} 设置为传入的操作名称。</p>
     *
     * @param operation DDC 操作名称；为空时移除当前作用域内的操作标识
     * @return 关闭后可恢复原 Trace 和 MDC 的作用域
     */
    public static Scope openOperation(String operation) {
        return new Scope(
                TraceContext.currentOrCreate().open(),
                operation
        );
    }

    /**
     * 安装指定 Trace 上下文并开启一个 DDC 操作作用域。
     *
     * @param context   需要安装到当前线程的 Trace 和 MDC 快照
     * @param operation DDC 操作名称；为空时移除当前作用域内的操作标识
     * @return 关闭后可恢复线程原有 Trace 和 MDC 的作用域
     */
    public static Scope openContext(TraceContext context,
                                    String operation) {
        return new Scope(context.open(), operation);
    }

    /**
     * 包装一个在执行线程中开启新 DDC 操作作用域的任务。
     *
     * <p>该方法不会捕获调用方线程上下文。任务实际执行时使用执行线程已有的 Trace；如果执行线程没有
     * 完整 Trace，则创建新的根上下文。需要传播调用方上下文时应使用 {@link #wrapContext}。</p>
     *
     * @param operation DDC 操作名称
     * @param runnable  需要包装的任务
     * @return 带有自动开启和关闭操作作用域能力的任务
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
     *
     * <p>任务执行结束后会恢复执行线程原有的 Trace 和 MDC，适用于 ACK 投递等跨线程异步操作。</p>
     *
     * @param context   需要传播到执行线程的 Trace 和 MDC 快照
     * @param operation DDC 操作名称
     * @param runnable  需要包装的任务
     * @return 带有上下文安装和恢复能力的任务
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
     *
     * @param key   MDC 键
     * @param value 原有值；为空时删除该键
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
     *
     * <p>创建时安装 Trace 上下文并设置 DDC 组件与操作标识，关闭时恢复进入作用域之前的内容。</p>
     */
    public static final class Scope implements AutoCloseable {

        /** 公共 TraceContext 创建的底层作用域，负责恢复完整 MDC 快照。 */
        private final TraceContext.Scope traceScope;

        /** 安装 Trace 上下文后、覆盖 DDC 标识前的组件值。 */
        private final String previousComponent;

        /** 安装 Trace 上下文后、覆盖 DDC 标识前的操作值。 */
        private final String previousOperation;

        /**
         * 创建 DDC 操作作用域并写入组件、操作 MDC 字段。
         *
         * @param traceScope 已安装 Trace 上下文的底层作用域
         * @param operation  DDC 操作名称；为空时删除操作标识
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
         */
        @Override
        public void close() {
            restore(OPERATION_KEY, previousOperation);
            restore(COMPONENT_KEY, previousComponent);
            traceScope.close();
        }
    }
}
