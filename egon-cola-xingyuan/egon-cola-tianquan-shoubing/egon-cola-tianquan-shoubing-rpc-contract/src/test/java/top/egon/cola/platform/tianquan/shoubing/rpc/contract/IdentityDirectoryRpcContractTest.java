package top.egon.cola.platform.tianquan.shoubing.rpc.contract;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityDirectoryRpcContractTest {

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void exposesStableBatchIdentityDirectoryContract() {
        RpcContractDescriptor contract = new RpcContractValidator(VALIDATION_UTILS)
                .validate(IdentityDirectoryRpc.class);

        assertThat(contract.serviceName())
                .isEqualTo("egon.tianquan.shoubing.v1.IdentityDirectoryService");
        assertThat(contract.group()).isEqualTo("tianquan-shoubing");
        assertThat(contract.version()).isEqualTo("1.0.0");
        assertThat(contract.methods()).hasSize(2);
        assertThat(contract.methods())
                .extracting(method -> method.methodName())
                .containsExactlyInAnyOrder(
                        "BatchGetIdentityProfiles",
                        "GetTenantMembership"
                );
        assertThat(contract.methods())
                .filteredOn(method -> method.methodName()
                        .equals("BatchGetIdentityProfiles"))
                .singleElement()
                .extracting(method -> method.fullMethodName())
                .isEqualTo(
                        "egon.tianquan.shoubing.v1.IdentityDirectoryService/BatchGetIdentityProfiles"
                );
        assertThat(contract.methods())
                .filteredOn(method -> method.methodName()
                        .equals("GetTenantMembership"))
                .singleElement()
                .extracting(method -> method.fullMethodName())
                .isEqualTo(
                        "egon.tianquan.shoubing.v1.IdentityDirectoryService/GetTenantMembership"
                );
    }
}
