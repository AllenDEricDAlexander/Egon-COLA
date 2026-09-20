package top.egon.cola.component.yuheng.engine.rpc.service;

import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.yuheng.runtime.rpc.adapter.RpcProviderChannelCache;
import top.egon.cola.component.yuheng.engine.rpc.service.RpcMethodIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertSame;

class RpcGatewayHandlerRegistryTest {

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

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
                        1024),
                rules::get
        );
        RpcMethodIndex replacement = new RpcMethodIndex(Map.of());

        rules.set(replacement);

        assertSame(replacement, registry.activeIndex());
    }
}
