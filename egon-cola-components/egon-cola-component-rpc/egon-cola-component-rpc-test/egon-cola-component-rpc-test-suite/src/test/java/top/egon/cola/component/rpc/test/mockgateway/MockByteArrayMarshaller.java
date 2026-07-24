package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.MethodDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class MockByteArrayMarshaller
        implements MethodDescriptor.Marshaller<byte[]> {

    @Override
    public InputStream stream(byte[] value) {
        return new ByteArrayInputStream(value);
    }

    @Override
    public byte[] parse(InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read RPC payload", exception);
        }
    }
}
