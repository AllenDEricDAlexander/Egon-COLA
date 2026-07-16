package top.egon.cola.component.bytecode.core.classfile;

public interface ClassMetadataReader {

    ClassMetadata read(String module, byte[] classBytes);
}
