package top.egon.cola.component.bytecode.core.enhance.executor;

public record CallSiteIdInput(
        String owner,
        String enclosingMethodName,
        String enclosingMethodDescriptor,
        int opcode,
        String targetOwner,
        String targetName,
        String targetDescriptor,
        int instructionOrdinal
) {
}
