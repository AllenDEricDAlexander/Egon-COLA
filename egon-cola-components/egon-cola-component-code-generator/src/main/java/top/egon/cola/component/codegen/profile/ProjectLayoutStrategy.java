package top.egon.cola.component.codegen.profile;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;

import java.util.Set;

/**
 * Fixed Light, Web and Service output layouts. There is no plugin registry.
 */
public interface ProjectLayoutStrategy {

    Set<String> PERSISTENCE = Set.of("po", "dao", "mapper-xml", "repo");

    CodegenProfileEnum profile();

    boolean allows(String artifact);

    String packageName(CodegenConfigBO config, String artifact);

    String relativePath(CodegenConfigBO config, String artifact, String fileName);

    static ProjectLayoutStrategy resolve(CodegenProfileEnum profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile is required");
        }
        return switch (profile) {
            case LIGHT -> LightLayout.INSTANCE;
            case WEB -> WebLayout.INSTANCE;
            case SERVICE -> ServiceLayout.INSTANCE;
        };
    }

    @Slf4j
    final class LightLayout implements ProjectLayoutStrategy {

        static final LightLayout INSTANCE = new LightLayout();

        @Override
        public CodegenProfileEnum profile() {
            return CodegenProfileEnum.LIGHT;
        }

        @Override
        public boolean allows(String artifact) {
            return PERSISTENCE.contains(artifact) || "controller".equals(artifact);
        }

        @Override
        public String packageName(CodegenConfigBO config, String artifact) {
            return javaPackage(config, artifact);
        }

        @Override
        public String relativePath(CodegenConfigBO config, String artifact, String fileName) {
            log.debug("resolve light path for {}", artifact);
            return sourcePath("", config, artifact, fileName);
        }
    }

    @Slf4j
    final class WebLayout implements ProjectLayoutStrategy {

        static final WebLayout INSTANCE = new WebLayout();

        @Override
        public CodegenProfileEnum profile() {
            return CodegenProfileEnum.WEB;
        }

        @Override
        public boolean allows(String artifact) {
            return PERSISTENCE.contains(artifact) || "controller".equals(artifact);
        }

        @Override
        public String packageName(CodegenConfigBO config, String artifact) {
            return javaPackage(config, artifact);
        }

        @Override
        public String relativePath(CodegenConfigBO config, String artifact, String fileName) {
            return sourcePath(modulePrefix(config), config, artifact, fileName);
        }
    }

    @Slf4j
    final class ServiceLayout implements ProjectLayoutStrategy {

        static final ServiceLayout INSTANCE = new ServiceLayout();

        @Override
        public CodegenProfileEnum profile() {
            return CodegenProfileEnum.SERVICE;
        }

        @Override
        public boolean allows(String artifact) {
            return PERSISTENCE.contains(artifact);
        }

        @Override
        public String packageName(CodegenConfigBO config, String artifact) {
            return javaPackage(config, artifact);
        }

        @Override
        public String relativePath(CodegenConfigBO config, String artifact, String fileName) {
            return sourcePath(modulePrefix(config), config, artifact, fileName);
        }
    }

    private static String modulePrefix(CodegenConfigBO config) {
        String module = config.getRoots() == null ? null : config.getRoots().get("infrastructure");
        if (module == null || module.isBlank()) {
            throw new IllegalArgumentException("infrastructure module path is required");
        }
        return stripSeparators(module) + "/";
    }

    private static String sourcePath(String prefix, CodegenConfigBO config, String artifact, String fileName) {
        String domain = safeSegment(config.getDomain(), "domain");
        String relative = "mapper-xml".equals(artifact)
                ? "src/main/resources/mybatis/mapper/" + domain + "/" + fileName
                : "src/main/java/" + javaPackage(config, artifact).replace('.', '/') + "/" + fileName;
        String path = prefix + relative;
        if (path.startsWith("/") || path.contains("..")) {
            throw new IllegalArgumentException("path escapes the output root");
        }
        return path;
    }

    private static String javaPackage(CodegenConfigBO config, String artifact) {
        String base = safeSegment(config.getBasePackage(), "basePackage").replace('/', '.');
        String domain = safeSegment(config.getDomain(), "domain");
        String layer = switch (artifact) {
            case "po" -> "po";
            case "dao", "mapper-xml" -> "dao";
            case "repo" -> "repo";
            default -> artifact;
        };
        return base + ".infrastructure." + domain + "." + layer;
    }

    private static String safeSegment(String value, String name) {
        if (value == null || value.isBlank() || value.contains("..") || value.contains("/") || value.contains("\\")) {
            throw new IllegalArgumentException(name + " is not a safe path segment");
        }
        return value;
    }

    private static String stripSeparators(String value) {
        String stripped = value.replace('\\', '/');
        while (stripped.startsWith("/")) {
            stripped = stripped.substring(1);
        }
        while (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        if (stripped.isBlank() || stripped.contains("..")) {
            throw new IllegalArgumentException("module path escapes the output root");
        }
        return stripped;
    }
}
