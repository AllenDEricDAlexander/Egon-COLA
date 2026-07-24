package top.egon.cola.component.rpc.test.consumer;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.consumer.EgonRpcReferenceBeanPostProcessor;
import top.egon.cola.component.rpc.consumer.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EchoRpcClientContextTest {

    @Test
    void shouldInjectJdkProxyWithoutProviderAddress() {
        RpcConsumerProxyFactory proxyFactory = new RpcConsumerProxyFactory(
                new RpcContractValidator(),
                mock(RpcConsumerGatewayManager.class),
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

        assertThat(Proxy.isProxyClass(
                client.rpcProxy().getClass()
        )).isTrue();
    }
}
