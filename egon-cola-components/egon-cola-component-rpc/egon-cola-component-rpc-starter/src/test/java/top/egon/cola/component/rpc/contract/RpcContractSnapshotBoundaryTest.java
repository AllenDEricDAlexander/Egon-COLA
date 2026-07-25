package top.egon.cola.component.rpc.contract;

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RpcContractSnapshotBoundaryTest {

    @Test
    void snapshotModelAndBytesContainNoRuntimeOrProviderLocationObjects() {
        assertThat(Arrays.stream(RpcContractSnapshot.class
                        .getRecordComponents())
                .map(RecordComponent::getType))
                .allMatch(type -> !Class.class.equals(type)
                        && !java.lang.reflect.Method.class.equals(type));
        assertThat(Arrays.stream(RpcMethodSnapshot.class
                        .getRecordComponents())
                .map(RecordComponent::getType))
                .allMatch(type -> !Class.class.equals(type)
                        && !java.lang.reflect.Method.class.equals(type));

        RpcContractSnapshot snapshot = new RpcContractSnapshotBuilder().build(
                new RpcContractValidator().validate(BoundaryContract.class)
        );
        String descriptorBytes = new String(
                snapshot.fileDescriptorSet(),
                StandardCharsets.ISO_8859_1
        );

        assertThat(descriptorBytes)
                .doesNotContain("java.lang.Class")
                .doesNotContain("java.lang.reflect.Method")
                .doesNotContain("127.0.0.1")
                .doesNotContain("localhost");
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface BoundaryContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }
}
