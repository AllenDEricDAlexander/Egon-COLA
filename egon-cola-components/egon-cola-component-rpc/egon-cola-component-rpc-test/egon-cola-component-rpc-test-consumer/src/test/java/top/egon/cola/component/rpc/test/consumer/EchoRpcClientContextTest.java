package top.egon.cola.component.rpc.test.consumer;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.consumer.proxy.EgonRpcReferenceBeanPostProcessor;
import top.egon.cola.component.rpc.consumer.gateway.GatewayRpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.test.contract.EchoRpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EchoRpcClientContextTest {

    @Test
    void shouldInjectRpcProxyWithoutProviderAddress() {
        RpcConsumerProxyFactory proxyFactory = new RpcConsumerProxyFactory(
                new RpcContractValidator(),
                new GatewayRpcInvocationChannelProvider(
                        mock(RpcConsumerGatewayManager.class)
                ),
                new RpcProcessIdentity(
                        "consumer-test",
                        "test",
                        "default",
                        "127.0.0.1",
                        1,
                        "consumer-1"
                ),
                new RpcStatusExceptionMapper(),
                3000
        );
        EchoRpcClient client = new EchoRpcClient();

        new EgonRpcReferenceBeanPostProcessor(proxyFactory)
                .postProcessBeforeInitialization(client, "echoRpcClient");

        assertThat(client.rpcProxy())
                .isNotNull()
                .isInstanceOf(EchoRpc.class);
    }
}
