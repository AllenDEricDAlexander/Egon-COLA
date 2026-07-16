package top.egon.cola.component.bytecode.bridge;

import java.util.Objects;
import java.util.Set;

public record MethodMetadata(
        long methodId,
        String owner,
        String methodName,
        String methodDescriptor,
        int access,
        boolean constructor,
        Set<BridgeCapability> features
) {
    public MethodMetadata {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(methodDescriptor, "methodDescriptor");
        features = Set.copyOf(features);
    }
}
