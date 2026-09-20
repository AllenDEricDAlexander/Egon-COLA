package top.egon.cola.component.dtp.domain.model.valobj;

import top.egon.cola.component.common.core.enums.EgonEnum;

/**
 * @author 有罗敷的马同学
 * @description 执行器类型枚举值对象
 * @Date 上午8:55 2026/6/29
 **/
public enum ExecutorKind implements EgonEnum {

    PLATFORM_THREAD_POOL(0),
    SPRING_THREAD_POOL_TASK_EXECUTOR(1),
    VIRTUAL_THREAD_PER_TASK(2),
    UNKNOWN(3);

    private final int code;

    ExecutorKind(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return name();
    }

}
