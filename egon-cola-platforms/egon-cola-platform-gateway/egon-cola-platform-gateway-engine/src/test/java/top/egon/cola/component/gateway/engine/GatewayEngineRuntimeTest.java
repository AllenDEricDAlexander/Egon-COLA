package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewaySlotRuntime;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleActivationApplier;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayEngineRuntimeTest {

    @Test
    void retriesLkgRestoreWhenProviderDiscoveryIsTemporarilyUnavailable()
            throws Exception {
        GatewayEngineRuntimeProperties properties =
                new GatewayEngineRuntimeProperties();
        properties.getRpc().setEnabled(false);
        GatewayHttpServer httpServer = mock(GatewayHttpServer.class);
        RpcGatewayServer rpcServer = mock(RpcGatewayServer.class);
        RpcGatewaySlotRuntime rpcSlot = mock(RpcGatewaySlotRuntime.class);
        GatewayRuleActivationApplier activation = mock(
                GatewayRuleActivationApplier.class
        );
        ProviderDirectory providerDirectory = mock(ProviderDirectory.class);
        CountDownLatch restoreAttempts = new CountDownLatch(3);
        when(activation.restoreLkg()).thenAnswer(invocation -> {
            restoreAttempts.countDown();
            if (restoreAttempts.getCount() > 0) {
                throw new IllegalStateException("DDC unavailable");
            }
            return false;
        });

        GatewayEngineRuntime runtime = new GatewayEngineRuntime(
                properties,
                httpServer,
                rpcServer,
                rpcSlot,
                activation,
                providerDirectory
        );
        try {
            assertDoesNotThrow(runtime::start);
            assertTrue(runtime.running());
            verify(httpServer).start();
            assertTrue(restoreAttempts.await(1_500, TimeUnit.MILLISECONDS));
        } finally {
            runtime.stop();
        }
    }
}
