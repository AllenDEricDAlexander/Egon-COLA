package top.egon.cola.component.ddc.registry;

public interface DdcRegistrySubscription extends AutoCloseable {

    @Override
    void close();
}
