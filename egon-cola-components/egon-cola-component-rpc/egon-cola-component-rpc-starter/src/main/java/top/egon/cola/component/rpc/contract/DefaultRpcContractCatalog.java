package top.egon.cola.component.rpc.contract;

import top.egon.cola.component.rpc.provider.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.RpcProviderMethodRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultRpcContractCatalog implements RpcContractCatalog {

    private static final Comparator<RpcContractDescriptor> CONTRACT_ORDER =
            Comparator.comparing(RpcContractDescriptor::serviceName)
                    .thenComparing(RpcContractDescriptor::group)
                    .thenComparing(RpcContractDescriptor::version);

    private final List<RpcContractDescriptor> contracts;

    public DefaultRpcContractCatalog(RpcProviderMethodRegistry registry) {
        this.contracts = registry.providers().stream()
                .map(RpcProviderBinding::contract)
                .distinct()
                .sorted(CONTRACT_ORDER)
                .toList();
    }

    @Override
    public List<RpcContractDescriptor> contracts() {
        return contracts;
    }

    @Override
    public Optional<RpcContractDescriptor> find(
            String serviceName,
            String group,
            String version
    ) {
        return contracts.stream()
                .filter(contract ->
                        Objects.equals(contract.serviceName(), serviceName)
                                && Objects.equals(contract.group(), group)
                                && Objects.equals(contract.version(), version))
                .findFirst();
    }
}
