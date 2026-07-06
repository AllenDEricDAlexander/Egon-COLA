package top.egon.cola.component.dtp.domain.model.valobj;

/**
 * @author 有罗敷的马同学
 * @description 执行器类型枚举值对象
 * @Date 上午8:55 2026/6/29
 **/
public enum ExecutorKind {

    PLATFORM_THREAD_POOL,
    SPRING_THREAD_POOL_TASK_EXECUTOR,
    VIRTUAL_THREAD_PER_TASK,
    UNKNOWN

}
