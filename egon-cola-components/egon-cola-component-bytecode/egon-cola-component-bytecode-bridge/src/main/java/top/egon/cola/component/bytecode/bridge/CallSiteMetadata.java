package top.egon.cola.component.bytecode.bridge;

import java.util.Objects;

public record CallSiteMetadata(
        long callSiteId,
        String owner,
        String methodName,
        String methodDescriptor,
        String targetOwner,
        String targetName,
        String targetDescriptor,
        Integer lineNumber
) {
    public CallSiteMetadata {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(methodDescriptor, "methodDescriptor");
        Objects.requireNonNull(targetOwner, "targetOwner");
        Objects.requireNonNull(targetName, "targetName");
        Objects.requireNonNull(targetDescriptor, "targetDescriptor");
    }
}
