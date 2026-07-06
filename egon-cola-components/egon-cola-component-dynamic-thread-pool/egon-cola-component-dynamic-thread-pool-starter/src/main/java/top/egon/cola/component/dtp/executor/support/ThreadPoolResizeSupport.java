package top.egon.cola.component.dtp.executor.support;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author      有罗敷的马同学
 * @description 线程池安全调整支持
 * @Date        上午8:55 2026/6/29
 **/
public final class ThreadPoolResizeSupport {

    private ThreadPoolResizeSupport() {
    }

    public static void resize(ThreadPoolExecutor executor, int newCore, int newMax) {
        if (newCore <= 0 || newMax <= 0) {
            throw new IllegalArgumentException("corePoolSize and maximumPoolSize must be positive");
        }
        if (newCore > newMax) {
            throw new IllegalArgumentException("corePoolSize must <= maximumPoolSize");
        }
        if (newMax < executor.getCorePoolSize()) {
            executor.setCorePoolSize(newCore);
            executor.setMaximumPoolSize(newMax);
            return;
        }
        executor.setMaximumPoolSize(newMax);
        executor.setCorePoolSize(newCore);
    }

}
