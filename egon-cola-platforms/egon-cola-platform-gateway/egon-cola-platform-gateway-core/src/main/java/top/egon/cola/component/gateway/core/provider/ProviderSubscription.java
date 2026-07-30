package top.egon.cola.component.gateway.core.provider;

public interface ProviderSubscription extends AutoCloseable {

    boolean active();

    @Override
    void close();
}
