package top.egon.cola.component.rpc.test.consumer;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
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

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void shouldInjectRpcProxyWithoutProviderAddress() {
        RpcConsumerProxyFactory proxyFactory = new RpcConsumerProxyFactory(
                new RpcContractValidator(VALIDATION_UTILS),
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

        new EgonRpcReferenceBeanPostProcessor(proxyFactory, VALIDATION_UTILS)
                .postProcessBeforeInitialization(client, "echoRpcClient");

        assertThat(client.rpcProxy())
                .isNotNull()
                .isInstanceOf(EchoRpc.class);
    }
}
