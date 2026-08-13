package top.egon.cola.component.rpc.contract.catalog;

import java.util.List;
import java.util.Optional;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.contract.snapshot.RpcContractSnapshot;

public interface RpcContractCatalog {

    List<RpcContractDescriptor> contracts();

    Optional<RpcContractDescriptor> find(RpcServiceIdentity serviceIdentity);

    List<RpcContractSnapshot> snapshots();

    Optional<RpcContractSnapshot> findSnapshot(
            RpcServiceIdentity serviceIdentity);
}
