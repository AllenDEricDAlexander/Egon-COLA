package top.egon.cola.component.rpc.consumer.proxy;

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinitionResolver;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategyFactory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EgonRpcReferenceBeanPostProcessorTest {

    @Test
    void injectsGatewayReferenceThroughOneFixedGatewayStrategy() {
        RpcReferenceStrategyFactory strategyFactory = mock(
                RpcReferenceStrategyFactory.class
        );
        RpcReferenceStrategy strategy = mock(RpcReferenceStrategy.class);
        RpcConsumerProxyFactory proxyFactory = mock(
                RpcConsumerProxyFactory.class
        );
        SampleContract gatewayProxy = mock(SampleContract.class);
        when(strategyFactory.create(any(RpcReferenceDefinition.class)))
                .thenReturn(strategy);
        doReturn(gatewayProxy).when(proxyFactory).create(
                any(RpcContractDescriptor.class),
                any(RpcReferenceDefinition.class),
                any(RpcReferenceStrategy.class)
        );
        GatewayReferences bean = new GatewayReferences();

        processor(strategyFactory, proxyFactory)
                .postProcessBeforeInitialization(bean, "gatewayReferences");

        assertThat(bean.reference).isSameAs(gatewayProxy);
        verify(strategyFactory).create(
                org.mockito.ArgumentMatchers.argThat(definition ->
                        definition.mode() == RpcReferenceMode.GATEWAY)
        );
    }

    @Test
    void injectsDirectReferenceWithoutInstallingGatewayDemand() {
        RpcReferenceStrategyFactory strategyFactory = mock(
                RpcReferenceStrategyFactory.class
        );
        RpcReferenceStrategy strategy = mock(RpcReferenceStrategy.class);
        RpcConsumerProxyFactory proxyFactory = mock(
                RpcConsumerProxyFactory.class
        );
        SampleContract directProxy = mock(SampleContract.class);
        when(strategyFactory.create(any(RpcReferenceDefinition.class)))
                .thenReturn(strategy);
        doReturn(directProxy).when(proxyFactory).create(
                any(RpcContractDescriptor.class),
                any(RpcReferenceDefinition.class),
                any(RpcReferenceStrategy.class)
        );
        DirectReferences bean = new DirectReferences();

        processor(strategyFactory, proxyFactory)
                .postProcessBeforeInitialization(bean, "directReferences");

        assertThat(bean.reference).isSameAs(directProxy);
        verify(strategyFactory).create(
                org.mockito.ArgumentMatchers.argThat(definition ->
                        definition.mode() == RpcReferenceMode.DIRECT)
        );
    }

    @Test
    void allowsSameContractInSeparateFixedModeFields() {
        RpcReferenceStrategyFactory strategyFactory = mock(
                RpcReferenceStrategyFactory.class
        );
        RpcReferenceStrategy gatewayStrategy = mock(
                RpcReferenceStrategy.class
        );
        RpcReferenceStrategy directStrategy = mock(RpcReferenceStrategy.class);
        when(strategyFactory.create(any(RpcReferenceDefinition.class)))
                .thenReturn(gatewayStrategy, directStrategy);
        RpcConsumerProxyFactory proxyFactory = mock(
                RpcConsumerProxyFactory.class
        );
        SampleContract gatewayProxy = mock(SampleContract.class);
        SampleContract directProxy = mock(SampleContract.class);
        doReturn(gatewayProxy, directProxy).when(proxyFactory).create(
                any(RpcContractDescriptor.class),
                any(RpcReferenceDefinition.class),
                any(RpcReferenceStrategy.class)
        );

        BothReferences bean = new BothReferences();
        processor(strategyFactory, proxyFactory)
                .postProcessBeforeInitialization(bean, "bothReferences");

        assertThat(bean.gatewayReference).isSameAs(gatewayProxy);
        assertThat(bean.directReference).isSameAs(directProxy);
    }

    @Test
    void rejectsGatewayReferenceWithDirectOnlyFields() {
        assertThatThrownBy(() -> processor(
                mock(RpcReferenceStrategyFactory.class),
                mock(RpcConsumerProxyFactory.class)
        ).postProcessBeforeInitialization(
                new InvalidGatewayReference(),
                "conflictingClient"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conflictingClient")
                .hasMessageContaining("reference")
                .hasMessageContaining("@EgonRpcReference")
                .hasMessageContaining("only valid for DIRECT");
    }

    @Test
    void reportsMissingSelectedModeFromStrategyFactory() {
        RpcReferenceStrategyFactory strategyFactory = mock(
                RpcReferenceStrategyFactory.class
        );
        when(strategyFactory.create(any(RpcReferenceDefinition.class)))
                .thenThrow(new IllegalStateException(
                        "RPC selected mode directory is required"
                ));
        assertThatThrownBy(() -> processor(
                strategyFactory,
                mock(RpcConsumerProxyFactory.class)
        ).postProcessBeforeInitialization(
                new DirectReferences(),
                "missingDirectClient"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missingDirectClient")
                .hasMessageContaining("reference")
                .hasMessageContaining("selected mode directory");
    }

    @Test
    void keepsInvalidContractFailures() {
        assertThatThrownBy(() -> processor(
                mock(RpcReferenceStrategyFactory.class),
                mock(RpcConsumerProxyFactory.class)
        ).postProcessBeforeInitialization(
                new MissingServiceReference(),
                "missingServiceClient"
        )).isInstanceOfSatisfying(EgonRpcException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(
                        EgonRpcErrorCode.RPC_INVALID_CONTRACT
                ));

        assertThatThrownBy(() -> processor(
                mock(RpcReferenceStrategyFactory.class),
                mock(RpcConsumerProxyFactory.class)
        ).postProcessBeforeInitialization(
                new NonInterfaceReference(),
                "invalidFieldClient"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalidFieldClient")
                .hasMessageContaining("interface");
    }

    private EgonRpcReferenceBeanPostProcessor processor(
            RpcReferenceStrategyFactory strategyFactory,
            RpcConsumerProxyFactory proxyFactory) {
        EgonRpcProperties properties = new EgonRpcProperties();
        RpcProcessIdentity identity = new RpcProcessIdentity(
                "proxy-test",
                "test",
                "127.0.0.1",
                1,
                "proxy-1"
        );
        return new EgonRpcReferenceBeanPostProcessor(
                new RpcContractValidator(),
                new RpcReferenceDefinitionResolver(properties, identity),
                strategyFactory,
                proxyFactory
        );
    }

    private static final class GatewayReferences {

        @EgonRpcReference(
                mode = RpcReferenceMode.GATEWAY,
                timeoutMs = 1200)
        private SampleContract reference;
    }

    private static final class DirectReferences {

        @EgonRpcReference(
                bizCode = "commerce",
                appCode = "orders"
        )
        private SampleContract reference;
    }

    private static final class BothReferences {

        @EgonRpcReference(
                mode = RpcReferenceMode.GATEWAY)
        private SampleContract gatewayReference;

        @EgonRpcReference(
                bizCode = "commerce",
                appCode = "orders"
        )
        private SampleContract directReference;
    }

    private static final class InvalidGatewayReference {

        @EgonRpcReference(
                mode = RpcReferenceMode.GATEWAY,
                bizCode = "commerce",
                appCode = "orders"
        )
        private SampleContract reference;
    }

    private static final class MissingServiceReference {

        @EgonRpcReference
        private MissingServiceContract reference;
    }

    private static final class NonInterfaceReference {

        @EgonRpcReference
        private String reference;
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "default",
            version = "1.0.0"
    )
    private interface SampleContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    private interface MissingServiceContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }
}
