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
import java.util.stream.Collectors;
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

    private static final String ADMIN_PACKAGE =
            "top.egon.cola.platform.rbac3.admin.";
    private static final Set<String> FORBIDDEN_ROOTS = Set.of(
            "application", "interfaces", "infrastructure", "integration",
            "security", "worker", "snapshot");

    @Test
    void adminUsesUnifiedSecurityStarter() throws Exception {
        String pom = Files.readString(Path.of(System.getProperty("basedir"))
                .resolve("pom.xml"));
        assertTrue(pom.contains("egon-cola-platform-rbac3-starter"));
    }

    @Test
    void productionPackagesHaveOneTopLevelTypeAndNoNestedTypes() throws Exception {
        for (Path source : productionSources()) {
            CompilationUnitTree unit = parse(source);
            List<ClassTree> topLevelTypes = unit.getTypeDecls().stream()
                    .filter(ClassTree.class::isInstance)
                    .map(ClassTree.class::cast)
                    .toList();
            assertThat(topLevelTypes)
                    .as("one top-level type in %s", source)
                    .hasSize(1);
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
    void productionPackagesDoNotUseLegacyLayerNames() throws Exception {
        for (Path source : productionJavaSources()) {
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

    @Test
    void oldTechnicalRootsContainNoProductionJava() throws Exception {
        for (String root : FORBIDDEN_ROOTS) {
            Path directory = adminSourceRoot().resolve(root);
            if (Files.exists(directory)) {
                try (Stream<Path> files = Files.walk(directory)) {
                    assertThat(files.filter(path -> path.toString().endsWith(".java")))
                            .as("no Java source under old root %s", root)
                            .isEmpty();
                }
            }
        }
    }

    @Test
    void everyProductionPackageHasPackageDocumentation() throws Exception {
        Set<Path> packages = productionSources().stream()
                .map(Path::getParent)
                .collect(Collectors.toSet());
        assertThat(packages)
                .allSatisfy(directory -> assertThat(directory.resolve("package-info.java"))
                        .as("package documentation for %s", directory)
                        .exists());
    }

    @Test
    void layersRespectDependencyDirection() throws Exception {
        for (Path source : productionSources()) {
            CompilationUnitTree unit = parse(source);
            String packageName = unit.getPackageName().toString();
            if (!packageName.startsWith(ADMIN_PACKAGE + "config")) {
                assertLayerImports(unit, source);
            }
        }
    }

    private void assertLayerImports(CompilationUnitTree unit, Path source) {
        String packageName = unit.getPackageName().toString();
        Set<String> imports = unit.getImports().stream()
                .map(tree -> tree.getQualifiedIdentifier().toString())
                .filter(name -> name.startsWith(ADMIN_PACKAGE))
                .collect(Collectors.toSet());

        if (packageName.contains(".domain")) {
            assertThat(imports)
                    .as("domain imports in %s", source)
                    .noneMatch(name -> name.contains(".controller.")
                            || name.contains(".service.")
                            || name.contains(".repository."));
        }
        if (packageName.contains(".controller")) {
            String domain = domainPrefix(packageName);
            assertThat(imports)
                    .as("controller imports in %s", source)
                    .allMatch(name -> name.startsWith(domain + ".controller.")
                            || name.contains(".domain.")
                            || name.contains(".service.")
                            || name.startsWith(ADMIN_PACKAGE + "config.security."));
        }
        if (packageName.contains(".service")) {
            assertThat(imports)
                    .as("service imports in %s", source)
                    .noneMatch(name -> name.contains(".controller.")
                            || name.matches(".*\\.repository\\."
                            + "(jpa|jdbc|redis|http|ddc|outbox|internal)\\..*"));
        }
        if (packageName.contains(".repository")) {
            assertThat(imports)
                    .as("repository imports in %s", source)
                    .noneMatch(name -> name.contains(".controller.")
                            || name.contains(".service."));
        }
    }

    private String domainPrefix(String packageName) {
        String suffix = packageName.substring(ADMIN_PACKAGE.length());
        int separator = suffix.indexOf('.');
        String root = separator < 0 ? suffix : suffix.substring(0, separator);
        return ADMIN_PACKAGE + root;
    }

    private List<Path> productionSources() throws Exception {
        return productionJavaSources().stream()
                .filter(path -> !path.getFileName().toString()
                        .equals("package-info.java"))
                .toList();
    }

    private List<Path> productionJavaSources() throws Exception {
        try (Stream<Path> files = Files.walk(adminSourceRoot())) {
            return files.filter(path -> path.toString().endsWith(".java"))
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

}
