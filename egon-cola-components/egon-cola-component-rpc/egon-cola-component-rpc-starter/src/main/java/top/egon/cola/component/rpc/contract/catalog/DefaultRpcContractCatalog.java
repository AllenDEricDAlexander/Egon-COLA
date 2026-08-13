package top.egon.cola.component.rpc.contract.catalog;

import top.egon.cola.component.rpc.provider.binding.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.contract.snapshot.RpcContractSnapshot;
import top.egon.cola.component.rpc.contract.snapshot.RpcContractSnapshotBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DefaultRpcContractCatalog implements RpcContractCatalog {

    private static final Comparator<RpcContractDescriptor> CONTRACT_ORDER =
            Comparator.comparing(RpcContractDescriptor::serviceName)
                    .thenComparing(RpcContractDescriptor::group)
                    .thenComparing(RpcContractDescriptor::version);

    private final List<RpcContractDescriptor> contracts;

    private final List<RpcContractSnapshot> snapshots;

    public DefaultRpcContractCatalog(RpcProviderMethodRegistry registry) {
        this.contracts = registry.providers().stream()
                .map(RpcProviderBinding::contract)
                .distinct()
                .sorted(CONTRACT_ORDER)
                .toList();
        RpcContractSnapshotBuilder snapshotBuilder =
                new RpcContractSnapshotBuilder();
        this.snapshots = contracts.stream()
                .map(snapshotBuilder::build)
                .toList();
    }

    @Override
    public List<RpcContractDescriptor> contracts() {
        return contracts;
    }

    @Override
    public Optional<RpcContractDescriptor> find(
            RpcServiceIdentity serviceIdentity
    ) {
        return contracts.stream()
                .filter(contract ->
                        RpcServiceIdentity.from(contract)
                                .equals(serviceIdentity))
                .findFirst();
    }

    @Override
    public List<RpcContractSnapshot> snapshots() {
        return snapshots;
    }

    @Override
    public Optional<RpcContractSnapshot> findSnapshot(
            RpcServiceIdentity serviceIdentity
    ) {
        return snapshots.stream()
                .filter(snapshot -> new RpcServiceIdentity(
                        snapshot.serviceName(),
                        snapshot.group(),
                        snapshot.version()
                ).equals(serviceIdentity))
                .findFirst();
    }
}
