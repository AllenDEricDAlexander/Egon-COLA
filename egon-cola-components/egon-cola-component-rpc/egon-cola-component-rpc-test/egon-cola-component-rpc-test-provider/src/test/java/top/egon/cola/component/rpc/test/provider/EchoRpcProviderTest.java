package top.egon.cola.component.rpc.test.provider;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.provider.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

import static org.assertj.core.api.Assertions.assertThat;

class EchoRpcProviderTest {

    @Test
    void shouldReturnProtobufResponseAndBeDiscoveredAsProvider() {
        EchoRpcProvider provider = new EchoRpcProvider("provider-a");
        EchoResponse response = provider.echo(EchoRequest.newBuilder()
                .setMessage("hello")
                .build());

        assertThat(response.getProviderId()).isEqualTo("provider-a");
        assertThat(response.getMessage()).isEqualTo("hello");

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    EchoRpcProvider.class,
                    () -> provider
            );
            context.refresh();

            assertThat(new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            ).scan().providers()).hasSize(1);
        }
    }
}
