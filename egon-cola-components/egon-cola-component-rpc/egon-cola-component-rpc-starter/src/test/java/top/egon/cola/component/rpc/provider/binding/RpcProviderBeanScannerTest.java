package top.egon.cola.component.rpc.provider.binding;

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.support.RpcProviderTestFixtures;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    @Test
    void shouldRetainProxiedAsyncProviderAndInvokeItsContractMethod() throws Throwable {
        AtomicInteger adviceCalls = new AtomicInteger();
        Object proxy = Proxy.newProxyInstance(
                AsyncContract.class.getClassLoader(),
                new Class<?>[]{AsyncContract.class},
                (ignored, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(ignored);
                            case "equals" -> ignored == arguments[0];
                            case "toString" -> "async-provider-proxy";
                            default -> null;
                        };
                    }
                    adviceCalls.incrementAndGet();
                    return CompletableFuture.completedFuture(
                            StringValue.of("proxy:" + arguments[0])
                    );
                }
        );
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansWithAnnotation(EgonRpcProvider.class))
                .thenReturn(Map.of("asyncProvider", proxy));

        RpcProviderMethodRegistry registry = new RpcProviderBeanScanner(
                context,
                new RpcContractValidator()
        ).scan();

        RpcProviderBinding binding = registry.providers().getFirst();
        RpcProviderMethodBinding method = registry.methods(
                binding.serviceIdentity()).getFirst();
        assertThat(binding.bean()).isSameAs(proxy);
        assertThat(method.method().invocationMode())
                .isEqualTo(top.egon.cola.component.rpc.consumer.invocation.RpcInvocationMode.ASYNC);
        assertThat(method.invoke(StringValue.of("request")))
                .isInstanceOf(CompletionStage.class)
                .extracting(value -> ((CompletionStage<?>) value)
                        .toCompletableFuture().join())
                .isEqualTo(StringValue.of("proxy:" + StringValue.of("request")));
        assertThat(adviceCalls).hasValue(1);
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    interface AsyncContract {

        @EgonRpcMethod(name = "Echo")
        CompletionStage<StringValue> echo(StringValue request);
    }
}
