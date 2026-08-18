package top.egon.cola.platform.idp.rpc.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityDirectoryRpcContractTest {

    @Test
    void exposesStableBatchIdentityDirectoryContract() {
        RpcContractDescriptor contract = new RpcContractValidator()
                .validate(IdentityDirectoryRpc.class);

        assertThat(contract.serviceName())
                .isEqualTo("egon.idp.v1.IdentityDirectoryService");
        assertThat(contract.group()).isEqualTo("idp");
        assertThat(contract.version()).isEqualTo("1.0.0");
        assertThat(contract.methods())
                .singleElement()
                .satisfies(method -> {
                    assertThat(method.methodName())
                            .isEqualTo("BatchGetIdentityProfiles");
                    assertThat(method.fullMethodName()).isEqualTo(
                            "egon.idp.v1.IdentityDirectoryService/BatchGetIdentityProfiles");
                });
    }
}
