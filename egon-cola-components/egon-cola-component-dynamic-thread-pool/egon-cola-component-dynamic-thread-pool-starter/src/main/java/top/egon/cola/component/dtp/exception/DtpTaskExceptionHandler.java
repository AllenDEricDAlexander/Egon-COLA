package top.egon.cola.component.dtp.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 线程安全、快速的应用级策略；不能改变原任务结果或回投同一个故障池。 */
@FunctionalInterface
public interface DtpTaskExceptionHandler {
    void handle(@NotNull @Valid DtpTaskFailureBO failure);
}
