package top.egon.cola.platform.idp.rpc.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceServerAdmissionRpcContractTest {

    @Test
    void exposesStableUnaryAdmissionContract() {
        RpcContractDescriptor contract = new RpcContractValidator()
                .validate(ResourceServerAdmissionRpc.class);

        assertThat(contract.serviceName())
                .isEqualTo("egon.idp.v1.ResourceServerAdmissionService");
        assertThat(contract.group()).isEqualTo("idp");
        assertThat(contract.version()).isEqualTo("1.0.0");
        assertThat(contract.methods())
                .singleElement()
                .satisfies(method -> {
                    assertThat(method.methodName()).isEqualTo("IssueAdmission");
                    assertThat(method.fullMethodName()).isEqualTo(
                            "egon.idp.v1.ResourceServerAdmissionService/IssueAdmission"
                    );
                });
        assertThat(ResourceServerAdmissionRpc.AUDIENCE.toString())
                .isEqualTo("urn:egon:rpc:idp:resource-server-admission:v1");
    }
}
