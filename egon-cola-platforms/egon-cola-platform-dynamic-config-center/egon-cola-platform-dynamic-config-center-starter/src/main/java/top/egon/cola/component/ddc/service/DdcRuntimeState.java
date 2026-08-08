package top.egon.cola.component.ddc.service;

/**
 * DDC 客户端运行时生命周期状态。
 * Lifecycle states of the DDC client runtime.
 */
public enum DdcRuntimeState {
    /**
     * 尚未启动。 Not started.
     */
    NEW,
    /**
     * 正在启动并初始化。 Starting and initializing.
     */
    STARTING,
    /**
     * 已准备好提供运行时配置能力。 Ready to provide runtime configuration.
     */
    READY,
    /**
     * 正在从租约或通信故障中恢复。 Recovering from a lease or communication failure.
     */
    RECOVERING,
    /**
     * 正在停止运行时。 Stopping the runtime.
     */
    STOPPING,
    /**
     * 已停止。 Stopped.
     */
    STOPPED,
    /**
     * 启动或运行失败。 Startup or runtime failed.
     */
    FAILED
}
