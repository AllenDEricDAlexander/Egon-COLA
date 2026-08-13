package top.egon.cola.component.rpc.provider.binding;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.support.RpcProviderTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

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

    @Test
    void shouldBindContractWhenProviderImplementsAnnotatedInterface() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    "provider",
                    RpcProviderTestFixtures.EchoProvider.class
            );
            context.refresh();

            RpcProviderMethodRegistry registry = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            ).scan();

            assertThat(registry.providers()).hasSize(1);
            assertThat(registry.providers().getFirst().contract().contractType())
                    .isEqualTo(RpcProviderTestFixtures.EchoContract.class);
        }
    }

    @Test
    void shouldRejectProviderWhoseInterfaceLacksServiceAnnotation() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    "contractlessProvider",
                    RpcProviderTestFixtures.ContractlessProvider.class
            );
            context.refresh();

            RpcProviderBeanScanner scanner = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            );

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(EgonRpcException.class)
                    .asInstanceOf(type(EgonRpcException.class))
                    .satisfies(exception -> assertThat(exception.getCode())
                            .isEqualTo(EgonRpcErrorCode.RPC_INVALID_CONTRACT))
                    .satisfies(exception -> assertThat(exception.getMessage())
                            .contains(RpcProviderTestFixtures
                                    .ContractlessProvider.class.getName())
                            .contains("@EgonRpcService"));
        }
    }

    @Test
    void shouldRejectProviderWithoutAnyInterface() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    "interfacelessProvider",
                    RpcProviderTestFixtures.InterfacelessProvider.class
            );
            context.refresh();

            RpcProviderBeanScanner scanner = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            );

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(EgonRpcException.class)
                    .hasMessageContaining(RpcProviderTestFixtures
                            .InterfacelessProvider.class.getName());
        }
    }

    @Test
    void shouldRejectMultipleIdentitiesForOneGrpcWireService() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    "v1Provider",
                    RpcProviderTestFixtures.EchoProvider.class
            );
            context.registerBean(
                    "v2Provider",
                    RpcProviderTestFixtures.EchoV2Provider.class
            );
            context.refresh();

            RpcProviderBeanScanner scanner = new RpcProviderBeanScanner(
                    context,
                    new RpcContractValidator()
            );

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(EgonRpcException.class)
                    .hasMessageContaining("wire service");
        }
    }
}
