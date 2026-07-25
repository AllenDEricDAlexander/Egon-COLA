package top.egon.cola.component.gateway.engine.rpc;

import com.google.protobuf.DescriptorProtos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRpcDynamicMessageBridgeTest {

    @Test
    void mapsJsonUsingDescriptorSetWithoutBusinessInterfaceJar() {
        HttpRpcDynamicMessageBridge bridge = new HttpRpcDynamicMessageBridge(
                new ProtobufDescriptorRegistry(descriptorSet())
        );

        byte[] bytes = bridge.requestBytes(
                "test.EchoRequest",
                "{\"value\":\"hello\"}"
        );
        String json = bridge.responseJson("test.EchoRequest", bytes);

        assertTrue(json.contains("\"value\": \"hello\""));
        assertThrows(
                IllegalArgumentException.class,
                () -> bridge.requestBytes(
                        "test.EchoRequest",
                        "{\"unknown\":true}"
                )
        );
    }

    private byte[] descriptorSet() {
        DescriptorProtos.DescriptorProto request =
                DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("EchoRequest")
                        .addField(
                                DescriptorProtos.FieldDescriptorProto.newBuilder()
                                        .setName("value")
                                        .setNumber(1)
                                        .setType(
                                                DescriptorProtos
                                                        .FieldDescriptorProto
                                                        .Type.TYPE_STRING
                                        )
                        )
                        .build();
        DescriptorProtos.FileDescriptorProto file =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("echo.proto")
                        .setPackage("test")
                        .setSyntax("proto3")
                        .addMessageType(request)
                        .build();
        return DescriptorProtos.FileDescriptorSet.newBuilder()
                .addFile(file)
                .build()
                .toByteArray();
    }
}
