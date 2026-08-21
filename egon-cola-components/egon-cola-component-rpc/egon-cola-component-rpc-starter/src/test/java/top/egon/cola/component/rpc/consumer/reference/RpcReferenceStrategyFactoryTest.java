package top.egon.cola.component.rpc.consumer.reference;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RpcReferenceStrategyFactoryTest {

    @Test
    void createsFixedDirectStrategyAndNeverTouchesGatewayManager() {
        RpcConsumerProviderManager providerManager = mock(RpcConsumerProviderManager.class);
        RpcConsumerGatewayManager gatewayManager = mock(RpcConsumerGatewayManager.class);
        RpcConsumerProviderManager.Demand demand = mock(RpcConsumerProviderManager.Demand.class);
        RpcProviderQuery query = query();
        when(providerManager.retain(query)).thenReturn(demand);
        RpcReferenceStrategyFactory factory = new RpcReferenceStrategyFactory(
                gatewayManager, providerManager);

        RpcReferenceStrategy strategy = factory.create(definition(
                RpcReferenceMode.DIRECT, query));

        assertThat(strategy.mode()).isEqualTo(RpcReferenceMode.DIRECT);
        assertThat(strategy.queryIdentity()).contains("orders-api");
        strategy.close();
        verify(providerManager).retain(query);
        verify(demand).close();
        verifyNoInteractions(gatewayManager);
    }

    @Test
    void createsFixedGatewayStrategyAndNeverTouchesProviderManager() {
        RpcConsumerGatewayManager gatewayManager = mock(RpcConsumerGatewayManager.class);
        RpcConsumerProviderManager providerManager = mock(RpcConsumerProviderManager.class);
        RpcConsumerGatewayManager.Demand demand = mock(RpcConsumerGatewayManager.Demand.class);
        when(gatewayManager.retainDemand()).thenReturn(demand);
        RpcReferenceStrategyFactory factory = new RpcReferenceStrategyFactory(
                gatewayManager, providerManager);

        RpcReferenceStrategy strategy = factory.create(definition(
                RpcReferenceMode.GATEWAY, null));

        assertThat(strategy.mode()).isEqualTo(RpcReferenceMode.GATEWAY);
        strategy.close();
        verify(gatewayManager).retainDemand();
        verify(demand).close();
        verifyNoInteractions(providerManager);
    }

    private static RpcReferenceDefinition definition(
            RpcReferenceMode mode,
            RpcProviderQuery query) {
        RpcReferencePolicy policy = new RpcReferencePolicy(
                1000, 0, LoadBalance.ROUND_ROBIN, FailStrategy.FAIL_CLOSED,
                "", null);
        return new RpcReferenceDefinition(
                mode,
                new RpcServiceIdentity("orders-api", "default", "1.0.0"),
                query,
                Map.of());
    }

    private static RpcProviderQuery query() {
        return new RpcProviderQuery(
                "commerce", "orders", "test", "orders-api",
                "default", "1.0.0", "grpc");
    }
}
