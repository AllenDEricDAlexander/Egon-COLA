package top.egon.cola.component.rpc.contract;

import java.util.List;
import java.util.Optional;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;

public interface RpcContractCatalog {

    List<RpcContractDescriptor> contracts();

    Optional<RpcContractDescriptor> find(RpcServiceIdentity serviceIdentity);

    List<RpcContractSnapshot> snapshots();

    Optional<RpcContractSnapshot> findSnapshot(
            RpcServiceIdentity serviceIdentity);
}
