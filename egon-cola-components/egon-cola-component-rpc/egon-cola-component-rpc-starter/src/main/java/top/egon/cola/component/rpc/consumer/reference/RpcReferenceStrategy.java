package top.egon.cola.component.rpc.consumer.reference;

import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;

import java.util.List;

/** Fixed-mode candidate snapshot and demand lifecycle exposed to invocation code. */
public interface RpcReferenceStrategy extends AutoCloseable {

    RpcReferenceMode mode();

    String queryIdentity();

    long revision();

    List<? extends RpcEndpoint> candidates();

    @Override
    void close();
}
