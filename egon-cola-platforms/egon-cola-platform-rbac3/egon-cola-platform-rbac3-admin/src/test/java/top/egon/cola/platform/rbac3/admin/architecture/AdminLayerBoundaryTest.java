package top.egon.cola.platform.rbac3.admin.architecture;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 RBAC3 Admin 领域分包边界，防止功能类重新声明成员类型或回退到旧技术层目录。
 * Verifies RBAC3 Admin domain-package boundaries so functional classes cannot
 * regain member types or move back into legacy technical-layer directories.
 */
class AdminLayerBoundaryTest {

    private static final Set<String> MIGRATED_ROOTS =
            Set.of("activation", "assignment", "auth", "bootstrap", "config",
                    "constraint", "directory", "identity", "management",
                    "resource", "role", "session", "shared", "tenant");
    private static final Set<String> DEFERRED_NESTED_TYPE_HOSTS = Set.of(
            "config/runtime/Rbac3WorkerConfiguration.java");

    @Test
    void controllersDoNotInjectRepositoriesAndAdminUsesUnifiedSecurityStarter()
            throws Exception {
        try (Stream<Path> files = Files.walk(adminSourceRoot())) {
            assertThat(files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/controller/"))
                    .map(this::read))
                    .noneMatch(source -> source.contains("Repository"));
        }
        String pom = Files.readString(Path.of(System.getProperty("basedir"))
                .resolve("pom.xml"));
        assertTrue(pom.contains("egon-cola-platform-rbac3-starter"));
    }

    @Test
    void migratedPackagesHaveOneTopLevelTypeAndNoNestedTypes() throws Exception {
        for (Path source : migratedSources()) {
            CompilationUnitTree unit = parse(source);
            List<ClassTree> topLevelTypes = unit.getTypeDecls().stream()
                    .filter(ClassTree.class::isInstance)
                    .map(ClassTree.class::cast)
                    .toList();
            assertThat(topLevelTypes)
                    .as("one top-level type in %s", source)
                    .hasSize(1);
            if (DEFERRED_NESTED_TYPE_HOSTS.contains(
                    normalize(adminSourceRoot().relativize(source)))) {
                continue;
            }
            assertThat(topLevelTypes.getFirst().getMembers())
                    .as("no nested type in %s", source)
                    .noneMatch(ClassTree.class::isInstance);
            String className = unit.getPackageName() + "."
                    + topLevelTypes.getFirst().getSimpleName();
            Class<?> type = Class.forName(
                    className, false,
                    Thread.currentThread().getContextClassLoader());
            assertThat(type.getDeclaredClasses())
                    .as("no declared member class in %s", className)
                    .isEmpty();
        }
    }

    @Test
    void migratedPackagesDoNotUseLegacyLayerNames() throws Exception {
        for (Path source : migratedSources()) {
            Path relative = adminSourceRoot().relativize(source);
            boolean legacyLayer = StreamSupport.stream(
                            relative.spliterator(), false)
                    .map(Path::toString)
                    .anyMatch(Set.of("application", "infrastructure")::contains);
            assertThat(legacyLayer)
                    .as("target layer name for %s", source)
                    .isFalse();
        }
    }

    private List<Path> migratedSources() throws Exception {
        try (Stream<Path> files = Files.walk(adminSourceRoot())) {
            return files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString()
                            .equals("package-info.java"))
                    .filter(path -> {
                        Path relative = adminSourceRoot().relativize(path);
                        return relative.getNameCount() > 1
                                && MIGRATED_ROOTS.contains(
                                relative.getName(0).toString());
                    })
                    .sorted()
                    .toList();
        }
    }

    private Path adminSourceRoot() {
        return Path.of(System.getProperty("basedir")).resolve(
                "src/main/java/top/egon/cola/platform/rbac3/admin");
    }

    private CompilationUnitTree parse(Path source) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                null, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(
                    null, manager, null, List.of("-proc:none"), null,
                    manager.getJavaFileObjects(source.toFile()));
            return task.parse().iterator().next();
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }
}
