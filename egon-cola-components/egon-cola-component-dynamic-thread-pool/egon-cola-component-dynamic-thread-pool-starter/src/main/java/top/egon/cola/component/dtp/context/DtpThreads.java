package top.egon.cola.component.dtp.context;

import java.util.Objects;

/**
 * @author      有罗敷的马同学
 * @description DTP 线程工具
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpThreads {

    private DtpThreads() {
    }

    public static Thread startVirtualThread(Runnable task) {
        Objects.requireNonNull(task, "task");
        return Thread.startVirtualThread(DtpRunnable.wrap(task));
    }

    public static Thread newPlatformThread(String name, Runnable task) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(task, "task");
        return Thread.ofPlatform().name(name).unstarted(DtpRunnable.wrap(task));
    }

    public static Thread newVirtualThread(String name, Runnable task) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(task, "task");
        return Thread.ofVirtual().name(name).unstarted(DtpRunnable.wrap(task));
    }

}
