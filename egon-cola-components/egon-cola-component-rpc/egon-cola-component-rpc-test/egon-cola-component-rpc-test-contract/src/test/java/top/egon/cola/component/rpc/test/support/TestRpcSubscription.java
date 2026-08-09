package top.egon.cola.component.rpc.test.support;

@FunctionalInterface
public interface TestRpcSubscription extends AutoCloseable {

    @Override
    void close();
}
