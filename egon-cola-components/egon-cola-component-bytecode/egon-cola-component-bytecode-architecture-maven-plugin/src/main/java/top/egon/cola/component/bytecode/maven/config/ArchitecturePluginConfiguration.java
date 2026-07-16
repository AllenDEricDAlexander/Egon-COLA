package top.egon.cola.component.bytecode.maven.config;

import java.io.File;
import java.util.List;
import java.util.Map;

public record ArchitecturePluginConfiguration(
        File outputDirectory,
        File baselinePath,
        boolean overwrite,
        boolean scanTests,
        boolean scanDependencies,
        List<File> additionalClassDirectories,
        Map<String, String> moduleMappings,
        Map<String, String> packageMappings,
        List<String> frameworkDenylist,
        List<String> frameworkAllowlist,
        List<String> facadeImplementationPackages,
        ArchitectureFailurePolicy failurePolicy,
        UnknownLayerPolicy unknownLayerPolicy,
        boolean cacheEnabled,
        File cacheDirectory
) {
    public ArchitecturePluginConfiguration {
        additionalClassDirectories = List.copyOf(additionalClassDirectories);
        moduleMappings = Map.copyOf(moduleMappings);
        packageMappings = Map.copyOf(packageMappings);
        frameworkDenylist = List.copyOf(frameworkDenylist);
        frameworkAllowlist = List.copyOf(frameworkAllowlist);
        facadeImplementationPackages = List.copyOf(facadeImplementationPackages);
    }
}
