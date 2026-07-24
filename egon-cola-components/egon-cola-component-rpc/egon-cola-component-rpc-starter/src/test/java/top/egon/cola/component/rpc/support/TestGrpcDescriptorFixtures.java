package top.egon.cola.component.rpc.support;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.StringValue;
import com.google.protobuf.WrappersProto;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import io.grpc.protobuf.ProtoUtils;

public final class TestGrpcDescriptorFixtures {

    private static final Descriptors.FileDescriptor FILE_DESCRIPTOR =
            buildFileDescriptor();

    private static final Descriptors.ServiceDescriptor UNARY_PROTO_SERVICE =
            FILE_DESCRIPTOR.findServiceByName("UnaryFixtureService");

    private static final Descriptors.ServiceDescriptor STREAMING_PROTO_SERVICE =
            FILE_DESCRIPTOR.findServiceByName("StreamingFixtureService");

    private TestGrpcDescriptorFixtures() {
    }

    public static final class UnaryFixtureGrpc {

        private static final ServiceDescriptor SERVICE_DESCRIPTOR =
                buildServiceDescriptor(UNARY_PROTO_SERVICE, MethodDescriptor.MethodType.UNARY);

        private UnaryFixtureGrpc() {
        }

        public static ServiceDescriptor getServiceDescriptor() {
            return SERVICE_DESCRIPTOR;
        }
    }

    public static final class StreamingFixtureGrpc {

        private static final ServiceDescriptor SERVICE_DESCRIPTOR =
                buildServiceDescriptor(
                        STREAMING_PROTO_SERVICE,
                        MethodDescriptor.MethodType.SERVER_STREAMING
                );

        private StreamingFixtureGrpc() {
        }

        public static ServiceDescriptor getServiceDescriptor() {
            return SERVICE_DESCRIPTOR;
        }
    }

    public static final class MissingDescriptorGrpc {

        private MissingDescriptorGrpc() {
        }
    }

    private static Descriptors.FileDescriptor buildFileDescriptor() {
        DescriptorProtos.DescriptorProto ignored =
                DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("Ignored")
                        .build();
        DescriptorProtos.MethodDescriptorProto unaryMethod =
                DescriptorProtos.MethodDescriptorProto.newBuilder()
                        .setName("Echo")
                        .setInputType(".google.protobuf.StringValue")
                        .setOutputType(".google.protobuf.StringValue")
                        .build();
        DescriptorProtos.MethodDescriptorProto streamingMethod =
                unaryMethod.toBuilder()
                        .setServerStreaming(true)
                        .build();
        DescriptorProtos.FileDescriptorProto file =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("rpc_fixture.proto")
                        .setPackage("egon.rpc.fixture.v1")
                        .addDependency("google/protobuf/wrappers.proto")
                        .addMessageType(ignored)
                        .addService(DescriptorProtos.ServiceDescriptorProto.newBuilder()
                                .setName("UnaryFixtureService")
                                .addMethod(unaryMethod))
                        .addService(DescriptorProtos.ServiceDescriptorProto.newBuilder()
                                .setName("StreamingFixtureService")
                                .addMethod(streamingMethod))
                        .build();
        try {
            return Descriptors.FileDescriptor.buildFrom(
                    file,
                    new Descriptors.FileDescriptor[]{
                            WrappersProto.getDescriptor()
                    }
            );
        } catch (Descriptors.DescriptorValidationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static ServiceDescriptor buildServiceDescriptor(
            Descriptors.ServiceDescriptor protoService,
            MethodDescriptor.MethodType methodType) {
        Descriptors.MethodDescriptor protoMethod =
                protoService.findMethodByName("Echo");
        MethodDescriptor<StringValue, StringValue> method =
                MethodDescriptor.<StringValue, StringValue>newBuilder()
                        .setType(methodType)
                        .setFullMethodName(MethodDescriptor.generateFullMethodName(
                                protoService.getFullName(),
                                protoMethod.getName()
                        ))
                        .setRequestMarshaller(
                                ProtoUtils.marshaller(StringValue.getDefaultInstance())
                        )
                        .setResponseMarshaller(
                                ProtoUtils.marshaller(StringValue.getDefaultInstance())
                        )
                        .setSchemaDescriptor(
                                new FixtureMethodDescriptorSupplier(protoMethod)
                        )
                        .build();
        return ServiceDescriptor.newBuilder(protoService.getFullName())
                .addMethod(method)
                .build();
    }

    private record FixtureMethodDescriptorSupplier(
            Descriptors.MethodDescriptor methodDescriptor
    ) implements ProtoMethodDescriptorSupplier {

        @Override
        public Descriptors.MethodDescriptor getMethodDescriptor() {
            return methodDescriptor;
        }

        @Override
        public Descriptors.ServiceDescriptor getServiceDescriptor() {
            return methodDescriptor.getService();
        }

        @Override
        public Descriptors.FileDescriptor getFileDescriptor() {
            return methodDescriptor.getFile();
        }
    }
}
