package top.egon.cola.component.dtp.context;

import org.springframework.core.task.TaskDecorator;

/**
 * @author      有罗敷的马同学
 * @description DTP 任务装饰器
 * @Date        下午9:27 2026/6/29
 **/
public class DtpTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return DtpRunnable.wrap(runnable);
    }

}
