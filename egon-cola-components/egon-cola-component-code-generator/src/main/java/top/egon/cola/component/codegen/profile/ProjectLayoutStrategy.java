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

    Set<String> BACKEND = Set.of(
            "domain-model", "domain-query", "command", "query", "result", "converter",
            "persistence-converter", "application-converter",
            "domain-service", "domain-impl", "manage", "manage-impl");

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
            return PERSISTENCE.contains(artifact) || BACKEND.contains(artifact) || "controller".equals(artifact);
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
            return PERSISTENCE.contains(artifact) || BACKEND.contains(artifact) || "controller".equals(artifact);
        }

        @Override
        public String packageName(CodegenConfigBO config, String artifact) {
            return javaPackage(config, artifact);
        }

        @Override
        public String relativePath(CodegenConfigBO config, String artifact, String fileName) {
            return sourcePath(modulePrefix(config, artifact), config, artifact, fileName);
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
            return PERSISTENCE.contains(artifact) || BACKEND.contains(artifact);
        }

        @Override
        public String packageName(CodegenConfigBO config, String artifact) {
            return javaPackage(config, artifact);
        }

        @Override
        public String relativePath(CodegenConfigBO config, String artifact, String fileName) {
            return sourcePath(modulePrefix(config, artifact), config, artifact, fileName);
        }
    }

    private static String modulePrefix(CodegenConfigBO config, String artifact) {
        String module = config.getRoots() == null ? null : config.getRoots().get(moduleKey(artifact));
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
        return switch (artifact) {
            case "domain-model", "domain-query", "domain-service" -> base + ".domain." + domain;
            case "domain-impl" -> base + ".infrastructure." + domain + ".service.impl";
            case "persistence-converter" -> base + ".infrastructure." + domain + ".converter";
            case "po" -> base + ".infrastructure." + domain + ".po";
            case "dao", "mapper-xml" -> base + ".infrastructure." + domain + ".dao";
            case "repo" -> base + ".infrastructure." + domain + ".repo";
            case "command", "query", "result", "manage", "manage-impl" -> base + ".application." + domain;
            case "application-converter" -> base + ".application." + domain + ".converter";
            case "controller" -> base + ".adapter." + domain;
            default -> base + ".infrastructure." + domain + "." + artifact;
        };
    }

    private static String moduleKey(String artifact) {
        return switch (artifact) {
            case "domain-model", "domain-query", "domain-service" -> "domain";
            case "command", "query", "result", "manage", "manage-impl", "application-converter" -> "application";
            case "controller" -> "adapter";
            default -> "infrastructure";
        };
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
