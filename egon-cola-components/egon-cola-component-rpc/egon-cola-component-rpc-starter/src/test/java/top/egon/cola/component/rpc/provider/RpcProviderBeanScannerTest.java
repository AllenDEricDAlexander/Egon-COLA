package top.egon.cola.component.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcProviderBeanScannerTest {

    @Test
    void shouldDiscoverOnlyAnnotatedProviderBean() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    "provider",
                    RpcProviderTestFixtures.EchoProvider.class
            );
            context.registerBean(
                    "ordinaryBean",
                    RpcProviderTestFixtures.NonProvider.class
            );
            context.refresh();

            RpcProviderMethodRegistry registry = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            ).scan();

            assertThat(registry.providers()).hasSize(1);
            assertThat(registry.providers().getFirst().bean())
                    .isInstanceOf(RpcProviderTestFixtures.EchoProvider.class);
        }
    }

    @Test
    void shouldRejectDuplicateServiceMethodBinding() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    "firstProvider",
                    RpcProviderTestFixtures.EchoProvider.class
            );
            context.registerBean(
                    "secondProvider",
                    RpcProviderTestFixtures.EchoProvider.class
            );
            context.refresh();

            RpcProviderBeanScanner scanner = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            );

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(EgonRpcException.class);
        }
    }
}
