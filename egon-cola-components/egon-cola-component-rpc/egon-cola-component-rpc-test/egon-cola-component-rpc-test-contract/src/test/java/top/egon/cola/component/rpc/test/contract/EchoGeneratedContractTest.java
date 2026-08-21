package top.egon.cola.component.rpc.test.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;

import static org.assertj.core.api.Assertions.assertThat;

class EchoGeneratedContractTest {

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

        assertThat(new RpcContractValidator().validate(EchoRpc.class)
                .methods())
                .singleElement()
                .extracting(method -> method.fullMethodName())
                .isEqualTo(canonical);
        assertThat(new RpcContractValidator().validate(AsyncEchoRpc.class)
                .methods())
                .singleElement()
                .extracting(method -> method.fullMethodName())
                .isEqualTo(canonical);
        assertThat(canonical).isEqualTo("egon.rpc.test.v1.EchoService/Echo");
    }
}
