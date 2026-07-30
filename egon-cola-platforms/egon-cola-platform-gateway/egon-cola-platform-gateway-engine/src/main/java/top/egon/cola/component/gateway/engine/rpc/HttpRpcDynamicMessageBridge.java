package top.egon.cola.component.gateway.engine.rpc;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

public final class HttpRpcDynamicMessageBridge {

    private final ProtobufDescriptorRegistry descriptors;

    public HttpRpcDynamicMessageBridge(
            ProtobufDescriptorRegistry descriptors) {
        this.descriptors = descriptors;
    }

    public byte[] requestBytes(String messageType, String json) {
        try {
            DynamicMessage.Builder builder = DynamicMessage.newBuilder(
                    descriptors.message(messageType)
            );
            JsonFormat.parser().merge(json, builder);
            return builder.build().toByteArray();
        } catch (com.google.protobuf.InvalidProtocolBufferException failure) {
            throw new IllegalArgumentException(
                    "HTTP request does not match protobuf schema",
                    failure
            );
        }
    }

    public String responseJson(String messageType, byte[] bytes) {
        try {
            DynamicMessage message = DynamicMessage.parseFrom(
                    descriptors.message(messageType),
                    bytes
            );
            return JsonFormat.printer().print(message);
        } catch (com.google.protobuf.InvalidProtocolBufferException failure) {
            throw new IllegalArgumentException(
                    "RPC response does not match protobuf schema",
                    failure
            );
        }
    }
}
