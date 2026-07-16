package top.egon.cola.component.bytecode.core.enhance.methodextension;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;

import java.util.Set;

public record MethodExtensionPolicy(
        long methodId,
        String owner,
        String methodName,
        String methodDescriptor,
        int access
) {

    public MethodMetadata methodMetadata(Set<BridgeCapability> features) {
        return new MethodMetadata(
                methodId, owner, methodName, methodDescriptor, access, false, features);
    }
}
