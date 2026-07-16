package top.egon.cola.component.bytecode.maven.scan;

import top.egon.cola.component.bytecode.core.architecture.LayerResolver;

import java.nio.file.Path;
import java.util.List;

public record ScanRequest(
        List<ScanInput> inputs,
        LayerResolver layerResolver,
        boolean cacheEnabled,
        Path cacheDirectory,
        String scanConfigurationDigest
) {
    public ScanRequest {
        inputs = List.copyOf(inputs);
    }
}
