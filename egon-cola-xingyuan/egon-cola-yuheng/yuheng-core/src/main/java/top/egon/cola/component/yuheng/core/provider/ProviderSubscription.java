package top.egon.cola.component.yuheng.core.provider;

public interface ProviderSubscription extends AutoCloseable {

    boolean active();

    @Override
    void close();
}
