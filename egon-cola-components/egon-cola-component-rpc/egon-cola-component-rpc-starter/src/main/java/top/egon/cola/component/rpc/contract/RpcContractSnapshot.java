package top.egon.cola.component.rpc.contract;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record RpcContractSnapshot(
        String serviceName,
        String group,
        String version,
        String protoPackage,
        String protoServiceName,
        byte[] fileDescriptorSet,
        String descriptorSha256,
        List<RpcMethodSnapshot> methods
) {

    public RpcContractSnapshot {
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(protoPackage, "protoPackage");
        Objects.requireNonNull(protoServiceName, "protoServiceName");
        fileDescriptorSet = Objects.requireNonNull(
                fileDescriptorSet,
                "fileDescriptorSet"
        ).clone();
        Objects.requireNonNull(descriptorSha256, "descriptorSha256");
        methods = List.copyOf(methods);
    }

    @Override
    public byte[] fileDescriptorSet() {
        return fileDescriptorSet.clone();
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof RpcContractSnapshot other)) {
            return false;
        }
        return serviceName.equals(other.serviceName)
                && group.equals(other.group)
                && version.equals(other.version)
                && protoPackage.equals(other.protoPackage)
                && protoServiceName.equals(other.protoServiceName)
                && Arrays.equals(fileDescriptorSet, other.fileDescriptorSet)
                && descriptorSha256.equals(other.descriptorSha256)
                && methods.equals(other.methods);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                serviceName,
                group,
                version,
                protoPackage,
                protoServiceName,
                descriptorSha256,
                methods
        );
        return 31 * result + Arrays.hashCode(fileDescriptorSet);
    }
}
