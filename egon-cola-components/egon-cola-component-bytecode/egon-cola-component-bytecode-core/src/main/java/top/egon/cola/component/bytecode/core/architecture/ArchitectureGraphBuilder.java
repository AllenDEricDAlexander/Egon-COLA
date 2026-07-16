package top.egon.cola.component.bytecode.core.architecture;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureType;
import top.egon.cola.component.bytecode.core.classfile.ClassDependency;
import top.egon.cola.component.bytecode.core.classfile.ClassMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ArchitectureGraphBuilder {

    public ArchitectureGraph build(
            Collection<ClassMetadata> metadataCollection,
            LayerResolver layerResolver
    ) {
        Objects.requireNonNull(metadataCollection, "metadataCollection");
        Objects.requireNonNull(layerResolver, "layerResolver");
        List<ArchitectureType> types = new ArrayList<>();
        Set<ArchitectureDependency> dependencies = new LinkedHashSet<>();
        for (ClassMetadata metadata : metadataCollection) {
            ArchitectureLayer sourceLayer = nonNullLayer(
                    layerResolver.resolve(metadata.module(), metadata.className()));
            types.add(new ArchitectureType(
                    metadata.module(),
                    metadata.className(),
                    sourceLayer,
                    metadata.annotations(),
                    metadata.interfaceType()
            ));
            for (ClassDependency dependency : metadata.dependencies()) {
                ArchitectureLayer targetLayer = nonNullLayer(
                        layerResolver.resolve(metadata.module(), dependency.targetClass()));
                dependencies.add(toArchitectureDependency(
                        metadata.module(), sourceLayer, targetLayer, dependency));
            }
        }
        return new ArchitectureGraph(types, List.copyOf(dependencies));
    }

    private ArchitectureDependency toArchitectureDependency(
            String module,
            ArchitectureLayer sourceLayer,
            ArchitectureLayer targetLayer,
            ClassDependency dependency
    ) {
        return new ArchitectureDependency(
                module,
                dependency.sourceClass(),
                dependency.sourceMember(),
                dependency.sourceDescriptor(),
                sourceLayer,
                dependency.targetClass(),
                dependency.targetMember(),
                dependency.targetDescriptor(),
                targetLayer,
                dependency.kind(),
                dependency.locationKind(),
                dependency.lineNumber()
        );
    }

    private ArchitectureLayer nonNullLayer(ArchitectureLayer layer) {
        return layer == null ? ArchitectureLayer.UNKNOWN : layer;
    }
}
