package top.egon.cola.component.rpc.contract;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RpcContractSnapshotReconstructionTest {

    @Test
    void reconstructsEveryExportedMethodDescriptor() throws Exception {
        RpcContractSnapshot snapshot = new RpcContractSnapshotBuilder().build(
                new RpcContractValidator().validate(ReconstructionContract.class)
        );
        DescriptorProtos.FileDescriptorSet descriptorSet =
                DescriptorProtos.FileDescriptorSet.parseFrom(
                        snapshot.fileDescriptorSet());
        Map<String, Descriptors.FileDescriptor> files = new LinkedHashMap<>();
        for (DescriptorProtos.FileDescriptorProto file
                : descriptorSet.getFileList()) {
            Descriptors.FileDescriptor[] dependencies =
                    file.getDependencyList().stream()
                            .map(files::get)
                            .toArray(Descriptors.FileDescriptor[]::new);
            files.put(file.getName(), Descriptors.FileDescriptor.buildFrom(
                    file,
                    dependencies
            ));
        }

        Descriptors.ServiceDescriptor service = files.get("rpc_fixture.proto")
                .findServiceByName(snapshot.protoServiceName());

        assertThat(service.getFile().getPackage())
                .isEqualTo(snapshot.protoPackage());
        for (RpcMethodSnapshot method : snapshot.methods()) {
            Descriptors.MethodDescriptor reconstructed =
                    service.findMethodByName(method.methodName());
            assertThat(reconstructed).isNotNull();
            assertThat(reconstructed.getInputType().getFullName())
                    .isEqualTo(method.requestType());
            assertThat(reconstructed.getOutputType().getFullName())
                    .isEqualTo(method.responseType());
            assertThat(reconstructed.isClientStreaming()).isFalse();
            assertThat(reconstructed.isServerStreaming()).isFalse();
        }
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "snapshot",
            version = "1.0.0"
    )
    interface ReconstructionContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }
}
