package top.egon.cola.component.rpc.consumer.proxy;

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcDirectReference;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EgonRpcReferenceBeanPostProcessorTest {

    @Test
    void injectsGatewayReferenceWithGatewayFactory() {
        RpcConsumerProxyFactory gatewayFactory =
                mock(RpcConsumerProxyFactory.class);
        RpcConsumerGatewayManager gatewayManager =
                mock(RpcConsumerGatewayManager.class);
        SampleContract gatewayProxy = mock(SampleContract.class);
        when(gatewayFactory.create(SampleContract.class, 1200))
                .thenReturn(gatewayProxy);
        GatewayReferences bean = new GatewayReferences();

        processor(gatewayFactory, gatewayManager, null)
                .postProcessBeforeInitialization(bean, "gatewayReferences");

        assertThat(bean.reference).isSameAs(gatewayProxy);
        verify(gatewayManager).registerDemand();
    }

    @Test
    void injectsDirectReferenceWithDirectFactory() {
        RpcDirectReferenceProxyFactory directFactory =
                mock(RpcDirectReferenceProxyFactory.class);
        SampleContract directProxy = mock(SampleContract.class);
        when(directFactory.create(
                eq(SampleContract.class),
                any(EgonRpcDirectReference.class)
        )).thenReturn(directProxy);
        DirectReferences bean = new DirectReferences();

        processor(null, null, directFactory)
                .postProcessBeforeInitialization(bean, "directReferences");

        assertThat(bean.reference).isSameAs(directProxy);
    }

    @Test
    void allowsSameContractInSeparateGatewayAndDirectFields() {
        RpcConsumerProxyFactory gatewayFactory =
                mock(RpcConsumerProxyFactory.class);
        RpcConsumerGatewayManager gatewayManager =
                mock(RpcConsumerGatewayManager.class);
        RpcDirectReferenceProxyFactory directFactory =
                mock(RpcDirectReferenceProxyFactory.class);
        SampleContract gatewayProxy = mock(SampleContract.class);
        SampleContract directProxy = mock(SampleContract.class);
        when(gatewayFactory.create(SampleContract.class, -1))
                .thenReturn(gatewayProxy);
        when(directFactory.create(
                eq(SampleContract.class),
                any(EgonRpcDirectReference.class)
        )).thenReturn(directProxy);
        BothReferences bean = new BothReferences();

        processor(gatewayFactory, gatewayManager, directFactory)
                .postProcessBeforeInitialization(bean, "bothReferences");

        assertThat(bean.gatewayReference).isSameAs(gatewayProxy);
        assertThat(bean.directReference).isSameAs(directProxy);
        verify(gatewayManager).registerDemand();
    }

    @Test
    void rejectsDoubleAnnotatedFieldWithBeanAndFieldName() {
        DoubleAnnotatedReference bean = new DoubleAnnotatedReference();

        assertThatThrownBy(() -> processor(null, null, null)
                .postProcessBeforeInitialization(bean, "conflictingClient"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conflictingClient")
                .hasMessageContaining("reference")
                .hasMessageContaining("@EgonRpcReference")
                .hasMessageContaining("@EgonRpcDirectReference");
    }

    @Test
    void reportsMissingSelectedFactoryWithBeanFieldAndMode() {
        assertThatThrownBy(() -> processor(null, null, null)
                .postProcessBeforeInitialization(
                        new DirectReferences(),
                        "missingDirectClient"
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missingDirectClient")
                .hasMessageContaining("reference")
                .hasMessageContaining("@EgonRpcDirectReference");

        assertThatThrownBy(() -> processor(null, null, null)
                .postProcessBeforeInitialization(
                        new GatewayReferences(),
                        "missingGatewayClient"
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missingGatewayClient")
                .hasMessageContaining("reference")
                .hasMessageContaining("@EgonRpcReference");
    }

    @Test
    void keepsInvalidContractFailures() {
        RpcConsumerProxyFactory gatewayFactory =
                mock(RpcConsumerProxyFactory.class);
        when(gatewayFactory.create(MissingServiceContract.class, -1))
                .thenThrow(new EgonRpcException(
                        EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                        "RPC contract is missing @EgonRpcService"
                ));

        assertThatThrownBy(() -> processor(
                gatewayFactory,
                mock(RpcConsumerGatewayManager.class),
                null
        ).postProcessBeforeInitialization(
                new MissingServiceReference(),
                "missingServiceClient"
        )).isInstanceOfSatisfying(EgonRpcException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(
                        EgonRpcErrorCode.RPC_INVALID_CONTRACT
                ));

        assertThatThrownBy(() -> processor(null, null, null)
                .postProcessBeforeInitialization(
                        new NonInterfaceReference(),
                        "invalidFieldClient"
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalidFieldClient")
                .hasMessageContaining("reference")
                .hasMessageContaining("interface");
    }

    private EgonRpcReferenceBeanPostProcessor processor(
            RpcConsumerProxyFactory gatewayFactory,
            RpcConsumerGatewayManager gatewayManager,
            RpcDirectReferenceProxyFactory directFactory) {
        return new EgonRpcReferenceBeanPostProcessor(
                gatewayFactory,
                gatewayManager,
                directFactory
        );
    }

    private static final class GatewayReferences {

        @EgonRpcReference(timeoutMs = 1200)
        private SampleContract reference;
    }

    private static final class DirectReferences {

        @EgonRpcDirectReference(
                bizCode = "commerce",
                appCode = "orders"
        )
        private SampleContract reference;
    }

    private static final class BothReferences {

        @EgonRpcReference
        private SampleContract gatewayReference;

        @EgonRpcDirectReference(
                bizCode = "commerce",
                appCode = "orders"
        )
        private SampleContract directReference;
    }

    private static final class DoubleAnnotatedReference {

        @EgonRpcReference
        @EgonRpcDirectReference(
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
