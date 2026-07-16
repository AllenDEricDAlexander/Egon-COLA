package top.egon.cola.component.bytecode.core.enhance.accessguard;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.MethodMetadata;

import java.util.Set;

public record AccessGuardPolicy(
        long methodId,
        String owner,
        String methodName,
        String methodDescriptor,
        int access
) {

    public MethodMetadata methodMetadata(Set<BridgeCapability> capabilities) {
        return new MethodMetadata(
                methodId, owner, methodName, methodDescriptor, access, false, capabilities);
    }
}
