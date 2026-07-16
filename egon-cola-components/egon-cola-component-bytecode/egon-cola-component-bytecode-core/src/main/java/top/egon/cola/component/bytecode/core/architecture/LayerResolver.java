package top.egon.cola.component.bytecode.core.architecture;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;

@FunctionalInterface
public interface LayerResolver {

    ArchitectureLayer resolve(String module, String className);
}
