package top.egon.cola.component.rpc.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.provider.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.RpcProviderMethodRegistry;

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
        assertThat(catalog.find("alpha.Service", "a", "2"))
                .containsSame(alphaV2);
        assertThat(catalog.find("missing.Service", "a", "1")).isEmpty();
    }

    @Test
    void contractListCannotBeMutated() {
        RpcContractCatalog catalog = new DefaultRpcContractCatalog(
                registry(List.of(binding(contract("alpha.Service", "a", "1"))))
        );

        assertThatThrownBy(() -> catalog.contracts().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private RpcContractDescriptor contract(
            String serviceName,
            String group,
            String version
    ) {
        return new RpcContractDescriptor(
                Runnable.class,
                serviceName,
                group,
                version,
                List.of()
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
}
