package top.egon.cola.component.gateway.engine.observability;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

@FunctionalInterface
public interface GatewayCallEventSink extends AutoCloseable {

    void send(GatewayCallEventV1 event, byte[] payload);

    @Override
    default void close() {
    }
}
