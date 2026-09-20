package top.egon.cola.component.rpc.test.contract;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;

import static org.assertj.core.api.Assertions.assertThat;

class EchoGeneratedContractTest {

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void generatesMessagesAndGrpcDescriptorFromTheOnlyEchoProto() {
        assertThat(EchoRequest.getDefaultInstance().getDescriptorForType().getFullName())
                .isEqualTo("egon.rpc.test.v1.EchoRequest");
        assertThat(EchoResponse.getDefaultInstance().getDescriptorForType().getFullName())
                .isEqualTo("egon.rpc.test.v1.EchoResponse");
        assertThat(EchoServiceGrpc.getServiceDescriptor().getName())
                .isEqualTo("egon.rpc.test.v1.EchoService");
        assertThat(EchoServiceGrpc.getEchoMethod().getFullMethodName())
                .isEqualTo("egon.rpc.test.v1.EchoService/Echo");
    }

    @Test
    void blockingAndAsyncJavaContractsReuseTheSingleCanonicalUnaryDescriptor() {
        assertThat(EchoServiceGrpc.getServiceDescriptor().getMethods())
                .hasSize(1);
        String canonical = EchoServiceGrpc.getEchoMethod().getFullMethodName();

        assertThat(new RpcContractValidator(VALIDATION_UTILS).validate(EchoRpc.class)
                .methods())
                .singleElement()
                .extracting(method -> method.fullMethodName())
                .isEqualTo(canonical);
        assertThat(new RpcContractValidator(VALIDATION_UTILS).validate(AsyncEchoRpc.class)
                .methods())
                .singleElement()
                .extracting(method -> method.fullMethodName())
                .isEqualTo(canonical);
        assertThat(canonical).isEqualTo("egon.rpc.test.v1.EchoService/Echo");
    }
}
