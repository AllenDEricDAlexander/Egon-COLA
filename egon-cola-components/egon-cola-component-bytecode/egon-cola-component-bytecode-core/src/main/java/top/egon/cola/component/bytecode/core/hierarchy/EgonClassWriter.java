package top.egon.cola.component.bytecode.core.hierarchy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public final class EgonClassWriter extends ClassWriter {

    private final ClassHierarchyResolver hierarchyResolver;

    public EgonClassWriter(ClassReader classReader, int flags, ClassLoader loader) {
        super(classReader, flags);
        this.hierarchyResolver = new ClassHierarchyResolver(loader);
    }

    @Override
    protected String getCommonSuperClass(String firstType, String secondType) {
        return hierarchyResolver.commonSuperClass(firstType, secondType);
    }
}
