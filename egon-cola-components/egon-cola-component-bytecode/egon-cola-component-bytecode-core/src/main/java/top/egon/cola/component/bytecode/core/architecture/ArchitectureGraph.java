package top.egon.cola.component.bytecode.core.architecture;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ArchitectureGraph {

    private final List<ArchitectureType> types;
    private final List<ArchitectureDependency> dependencies;
    private final Map<String, ArchitectureType> typesByName;

    public ArchitectureGraph(
            List<ArchitectureType> types,
            List<ArchitectureDependency> dependencies
    ) {
        this.types = List.copyOf(types);
        this.dependencies = List.copyOf(dependencies);
        Map<String, ArchitectureType> index = new LinkedHashMap<>();
        for (ArchitectureType type : types) {
            index.put(type.className(), type);
        }
        this.typesByName = Map.copyOf(index);
    }

    public List<ArchitectureType> types() {
        return types;
    }

    public List<ArchitectureDependency> dependencies() {
        return dependencies;
    }

    public Optional<ArchitectureType> findType(String className) {
        return Optional.ofNullable(typesByName.get(className));
    }
}
