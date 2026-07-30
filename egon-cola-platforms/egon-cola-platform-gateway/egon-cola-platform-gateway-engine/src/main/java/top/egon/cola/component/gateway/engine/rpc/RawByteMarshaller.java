package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.MethodDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public enum RawByteMarshaller
        implements MethodDescriptor.Marshaller<byte[]> {

    INSTANCE;

    @Override
    public InputStream stream(byte[] value) {
        return new ByteArrayInputStream(value.clone());
    }

    @Override
    public byte[] parse(InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "failed to read protobuf message bytes",
                    exception
            );
        }
    }

    public MethodDescriptor<byte[], byte[]> descriptor(String fullMethodName) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(this)
                .setResponseMarshaller(this)
                .build();
    }
}
