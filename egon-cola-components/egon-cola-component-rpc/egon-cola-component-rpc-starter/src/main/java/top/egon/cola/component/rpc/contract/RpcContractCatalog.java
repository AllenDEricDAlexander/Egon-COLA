package top.egon.cola.component.rpc.contract;

import java.util.List;
import java.util.Optional;

public interface RpcContractCatalog {

    List<RpcContractDescriptor> contracts();

    Optional<RpcContractDescriptor> find(
            String serviceName,
            String group,
            String version
    );
}
