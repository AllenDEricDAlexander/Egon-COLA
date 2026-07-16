package top.egon.cola.component.bytecode.core.architecture;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class DefaultLayerResolver implements LayerResolver {

    private final LayerMapping mapping;

    public DefaultLayerResolver(LayerMapping mapping) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
    }

    @Override
    public ArchitectureLayer resolve(String module, String className) {
        Set<ArchitectureLayer> explicitMatches = matchingLayers(module, mapping.explicitModules());
        if (!explicitMatches.isEmpty()) {
            return uniqueMatch(module, "module", explicitMatches);
        }
        Set<ArchitectureLayer> packageMatches = new LinkedHashSet<>();
        mapping.packagePatterns().forEach((layer, patterns) -> patterns.stream()
                .filter(pattern -> matchesPackage(className, pattern))
                .forEach(ignored -> packageMatches.add(layer)));
        if (!packageMatches.isEmpty()) {
            return uniqueMatch(className, "class", packageMatches);
        }
        return resolveByModuleSuffix(module);
    }

    private Set<ArchitectureLayer> matchingLayers(
            String value,
            java.util.Map<ArchitectureLayer, Set<String>> mappings
    ) {
        Set<ArchitectureLayer> matches = new LinkedHashSet<>();
        mappings.forEach((layer, values) -> {
            if (values.contains(value)) {
                matches.add(layer);
            }
        });
        return matches;
    }

    private ArchitectureLayer uniqueMatch(
            String value,
            String valueKind,
            Set<ArchitectureLayer> matches
    ) {
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Ambiguous " + valueKind + " layer mapping for '" + value + "': " + matches);
        }
        return matches.iterator().next();
    }

    private boolean matchesPackage(String className, String packagePattern) {
        if (className == null || packagePattern == null || packagePattern.isBlank()) {
            return false;
        }
        if (packagePattern.startsWith("..") && packagePattern.endsWith("..")
                && packagePattern.length() > 4) {
            String segment = packagePattern.substring(2, packagePattern.length() - 2);
            return ("." + className + ".").contains("." + segment + ".");
        }
        if (packagePattern.endsWith("..") && !packagePattern.substring(0,
                packagePattern.length() - 2).contains("*")) {
            String prefix = packagePattern.substring(0, packagePattern.length() - 2);
            return className.equals(prefix) || className.startsWith(prefix + ".");
        }
        String regex = Pattern.quote(packagePattern)
                .replace("\\Q..\\E", ".*")
                .replace("\\Q*\\E", "[^.]*");
        return className.matches(regex);
    }

    private ArchitectureLayer resolveByModuleSuffix(String module) {
        if (module == null) {
            return ArchitectureLayer.UNKNOWN;
        }
        String normalized = module.toLowerCase(Locale.ROOT);
        for (ArchitectureLayer layer : ArchitectureLayer.values()) {
            if (layer != ArchitectureLayer.UNKNOWN
                    && normalized.endsWith("-" + layer.name().toLowerCase(Locale.ROOT))) {
                return layer;
            }
        }
        return ArchitectureLayer.UNKNOWN;
    }
}
