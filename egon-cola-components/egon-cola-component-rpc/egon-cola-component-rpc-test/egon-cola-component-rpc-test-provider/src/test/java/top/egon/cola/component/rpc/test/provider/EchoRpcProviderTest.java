package top.egon.cola.component.rpc.test.provider;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

import static org.assertj.core.api.Assertions.assertThat;

class EchoRpcProviderTest {

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());

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
                    new RpcContractValidator(VALIDATION_UTILS)
            ).scan().providers()).hasSize(1);
        }
    }
}
