package top.egon.cola.component.ddc.registry;

/**
 * 可关闭的注册中心订阅句柄。
 * / Closeable handle for a registry subscription.
 */
public interface DdcRegistrySubscription extends AutoCloseable {

    /**
     * 取消订阅并释放关联监听资源；重复调用不产生额外效果。
     * / Cancels the subscription and releases associated listener resources;
     * repeated calls have no additional effect.
     */
    @Override
    void close();
}
