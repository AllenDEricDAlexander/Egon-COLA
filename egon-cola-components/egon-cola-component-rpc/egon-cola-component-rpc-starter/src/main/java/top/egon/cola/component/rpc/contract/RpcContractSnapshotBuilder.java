package top.egon.cola.component.rpc.contract;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import io.grpc.MethodDescriptor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RpcContractSnapshotBuilder {

    public RpcContractSnapshot build(RpcContractDescriptor contract) {
        List<RpcMethodDescriptor> methods = contract.methods().stream()
                .sorted(Comparator.comparing(
                        RpcMethodDescriptor::fullMethodName))
                .toList();
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(
                    "RPC contract must contain at least one method"
            );
        }
        Descriptors.ServiceDescriptor service =
                methods.getFirst().protoMethod().getService();
        assertSingleProtoService(methods, service);

        Map<String, Descriptors.FileDescriptor> files = new TreeMap<>();
        collectFiles(service.getFile(), files);
        byte[] descriptorSet = DescriptorProtos.FileDescriptorSet.newBuilder()
                .addAllFile(files.values().stream()
                        .map(Descriptors.FileDescriptor::toProto)
                        .toList())
                .build()
                .toByteArray();

        return new RpcContractSnapshot(
                contract.serviceName(),
                contract.group(),
                contract.version(),
                service.getFile().getPackage(),
                service.getName(),
                descriptorSet,
                sha256Hex(descriptorSet),
                methods.stream().map(this::snapshot).toList()
        );
    }

    private RpcMethodSnapshot snapshot(RpcMethodDescriptor method) {
        Descriptors.MethodDescriptor protoMethod = method.protoMethod();
        if (method.grpcMethod().getType() != MethodDescriptor.MethodType.UNARY
                || protoMethod.isClientStreaming()
                || protoMethod.isServerStreaming()) {
            throw new IllegalArgumentException(
                    "RPC contract snapshot only supports unary methods"
            );
        }
        return new RpcMethodSnapshot(
                method.methodName(),
                method.fullMethodName(),
                protoMethod.getInputType().getFullName(),
                protoMethod.getOutputType().getFullName(),
                RpcType.UNARY
        );
    }

    private void collectFiles(
            Descriptors.FileDescriptor file,
            Map<String, Descriptors.FileDescriptor> files
    ) {
        file.getDependencies().forEach(dependency ->
                collectFiles(dependency, files));
        files.putIfAbsent(file.getName(), file);
    }

    private void assertSingleProtoService(
            List<RpcMethodDescriptor> methods,
            Descriptors.ServiceDescriptor service
    ) {
        boolean sameService = methods.stream()
                .allMatch(method ->
                        method.protoMethod().getService().equals(service));
        if (!sameService) {
            throw new IllegalArgumentException(
                    "RPC contract methods must belong to one proto service"
            );
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }
}
