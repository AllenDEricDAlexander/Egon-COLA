package top.egon.cola.component.rpc.contract;

import org.junit.jupiter.api.Test;
import com.google.protobuf.StringValue;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.provider.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRpcContractCatalogTest {

    @Test
    void exposesOnlyProviderContractsInStableOrder() {
        RpcContractDescriptor zeta = contract("zeta.Service", "b", "2");
        RpcContractDescriptor alphaV2 = contract("alpha.Service", "a", "2");
        RpcContractDescriptor alphaV1 = contract("alpha.Service", "a", "1");
        RpcProviderMethodRegistry registry = registry(List.of(
                binding(zeta),
                binding(alphaV2),
                binding(alphaV1)
        ));

        RpcContractCatalog catalog = new DefaultRpcContractCatalog(registry);

        assertThat(catalog.contracts())
                .extracting(RpcContractDescriptor::serviceName,
                        RpcContractDescriptor::group,
                        RpcContractDescriptor::version)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "alpha.Service", "a", "1"),
                        org.assertj.core.groups.Tuple.tuple(
                                "alpha.Service", "a", "2"),
                        org.assertj.core.groups.Tuple.tuple(
                                "zeta.Service", "b", "2")
                );
        assertThat(catalog.find(
                new RpcServiceIdentity("alpha.Service", "a", "2")))
                .containsSame(alphaV2);
        assertThat(catalog.find(
                new RpcServiceIdentity("missing.Service", "a", "1"))).isEmpty();
        assertThat(catalog.snapshots())
                .extracting(RpcContractSnapshot::serviceName,
                        RpcContractSnapshot::group,
                        RpcContractSnapshot::version)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "alpha.Service", "a", "1"),
                        org.assertj.core.groups.Tuple.tuple(
                                "alpha.Service", "a", "2"),
                        org.assertj.core.groups.Tuple.tuple(
                                "zeta.Service", "b", "2")
                );
        assertThat(catalog.findSnapshot(
                new RpcServiceIdentity("zeta.Service", "b", "2")))
                .isPresent();
    }

    @Test
    void contractListCannotBeMutated() {
        RpcContractCatalog catalog = new DefaultRpcContractCatalog(
                registry(List.of(binding(contract("alpha.Service", "a", "1"))))
        );

        assertThatThrownBy(() -> catalog.contracts().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> catalog.snapshots().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private RpcContractDescriptor contract(
            String serviceName,
            String group,
            String version
    ) {
        RpcContractDescriptor template =
                new RpcContractValidator().validate(CatalogContract.class);
        return new RpcContractDescriptor(
                Runnable.class,
                serviceName,
                group,
                version,
                template.methods()
        );
    }

    private RpcProviderBinding binding(RpcContractDescriptor contract) {
        return new RpcProviderBinding(new Object(), contract);
    }

    private RpcProviderMethodRegistry registry(List<RpcProviderBinding> bindings) {
        return new RpcProviderMethodRegistry(List.of()) {
            @Override
            public List<RpcProviderBinding> providers() {
                return List.copyOf(bindings);
            }
        };
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface CatalogContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }
}
