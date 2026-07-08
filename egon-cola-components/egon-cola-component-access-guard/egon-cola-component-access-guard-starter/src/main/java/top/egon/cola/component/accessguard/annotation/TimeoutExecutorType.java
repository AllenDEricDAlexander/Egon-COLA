package top.egon.cola.component.accessguard.annotation;

public enum TimeoutExecutorType {
    GLOBAL_DEFAULT,
    THREAD_POOL,
    VIRTUAL_THREAD,
    HYSTRIX_ADAPTER,
    CUSTOM
}
