package top.egon.cola.component.gateway.engine.rpc.service;

import top.egon.cola.component.gateway.engine.rpc.adapter.RpcProviderChannelCache;
import top.egon.cola.component.gateway.engine.rpc.service.RpcMethodIndex;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;

class RpcGatewayHandlerRegistryTest {

    @Test
    void readsLatestRuleIndexWithoutRebuildingGrpcServer() {
        AtomicReference<RpcMethodIndex> rules =
                new AtomicReference<>(new RpcMethodIndex(Map.of()));
        RpcGatewayHandlerRegistry registry = new RpcGatewayHandlerRegistry(
                new RpcGatewayForwarder(
                        service -> {
                            throw new IllegalStateException();
                        },
                        new RpcProviderChannelCache(Duration.ofMillis(10)),
                        Duration.ofSeconds(1),
                        1024
                ),
                rules::get
        );
        RpcMethodIndex replacement = new RpcMethodIndex(Map.of());

        rules.set(replacement);

        assertSame(replacement, registry.activeIndex());
    }
}
