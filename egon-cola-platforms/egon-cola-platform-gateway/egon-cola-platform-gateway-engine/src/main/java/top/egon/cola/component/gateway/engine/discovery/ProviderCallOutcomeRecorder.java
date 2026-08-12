package top.egon.cola.component.gateway.engine.discovery;

/**
 * 中文说明：{@code ProviderCallOutcomeRecorder} 是接口契约，位于当前 Gateway 模块的相关包中，负责提供方调用OutcomeRecorder相关的职责与边界。
 * English summary: {@code ProviderCallOutcomeRecorder} is an interface contract in the current Gateway module; it owns the provider call outcome recorder-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface ProviderCallOutcomeRecorder {

    /**
     * 中文说明：执行 record 操作；该方法是 {@code ProviderCallOutcomeRecorder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record operation; this method is the invocation entry point on {@code ProviderCallOutcomeRecorder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCallOutcomeRecorder.record(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtimeIdentity 参数 运行时身份；parameter runtime identity。
     * @param outcome 参数 outcome；parameter outcome。
     */
    void record(String runtimeIdentity, ProviderCallOutcome outcome);

    /**
     * 中文说明：执行 noop 操作；该方法是 {@code ProviderCallOutcomeRecorder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the noop operation; this method is the invocation entry point on {@code ProviderCallOutcomeRecorder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCallOutcomeRecorder.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 noop 的处理结果；returns the result of the operation.
     */
    static ProviderCallOutcomeRecorder noop() {
        return (runtimeIdentity, outcome) -> {
        };
    }
}
