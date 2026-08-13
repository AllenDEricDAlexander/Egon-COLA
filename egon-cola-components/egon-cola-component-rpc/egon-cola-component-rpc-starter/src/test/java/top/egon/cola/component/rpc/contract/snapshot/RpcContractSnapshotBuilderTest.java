package top.egon.cola.component.rpc.contract.snapshot;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcType;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcContractSnapshotBuilderTest {

    @Test
    void buildsStableStandardDescriptorSetWithTransitiveDependencies()
            throws Exception {
        RpcContractDescriptor contract =
                new RpcContractValidator().validate(SnapshotContract.class);
        RpcContractSnapshotBuilder builder = new RpcContractSnapshotBuilder();

        RpcContractSnapshot first = builder.build(contract);
        RpcContractSnapshot second = builder.build(contract);
        DescriptorProtos.FileDescriptorSet descriptorSet =
                DescriptorProtos.FileDescriptorSet.parseFrom(
                        first.fileDescriptorSet());

        assertThat(first).isEqualTo(second);
        assertThat(descriptorSet.getFileList())
                .extracting(DescriptorProtos.FileDescriptorProto::getName)
                .containsExactly(
                        "google/protobuf/wrappers.proto",
                        "rpc_fixture.proto"
                );
        assertThat(first.descriptorSha256()).isEqualTo(
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256")
                                .digest(first.fileDescriptorSet())
                )
        );
        assertThat(first.protoPackage()).isEqualTo("egon.rpc.fixture.v1");
        assertThat(first.protoServiceName()).isEqualTo("UnaryFixtureService");
        assertThat(first.methods()).containsExactly(new RpcMethodSnapshot(
                "Echo",
                "egon.rpc.fixture.v1.UnaryFixtureService/Echo",
                "google.protobuf.StringValue",
                "google.protobuf.StringValue",
                RpcType.UNARY
        ));
    }

    @Test
    void snapshotDefensivelyCopiesBytesAndMethods() {
        RpcContractSnapshot snapshot = new RpcContractSnapshotBuilder().build(
                new RpcContractValidator().validate(SnapshotContract.class)
        );
        byte[] bytes = snapshot.fileDescriptorSet();
        byte original = bytes[0];

        bytes[0] = (byte) (original + 1);

        assertThat(snapshot.fileDescriptorSet()[0]).isEqualTo(original);
        assertThatThrownBy(() -> snapshot.methods().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "snapshot",
            version = "1.0.0"
    )
    interface SnapshotContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }
}
