package architecture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks physical source layout independently of compilation and generated-file hashes.
 */
class JavaSourceContractTest {

    private static final Path PROJECT = Path.of(".");
    private static final Pattern PACKAGE = Pattern.compile(
            "\\A(?:\\s|//[^\\r\\n]*|/\\*[\\s\\S]*?\\*/)*package\\s+([\\w.]+)\\s*;");
    private static final Pattern FORBIDDEN_CARRIER = Pattern.compile(
            "(?m)^\\s*(?:(?:public|protected|private|static|abstract|final|sealed|non-sealed)\\s+)*"
                    + "(?:class|interface|record)\\s+\\w*(?:Request|Response)\\b");

    @Test
    void mainSourcesMatchTheirPackageAndCarrierContract() throws IOException {
        List<Path> roots;
        try (Stream<Path> paths = Files.find(PROJECT, 4,
                (path, attributes) -> attributes.isDirectory() && path.endsWith(Path.of("src/main/java")))) {
            roots = paths.filter(path -> !path.toString().replace('\\', '/').contains("/target/")).toList();
        }
        assertFalse(roots.isEmpty(), "main Java source roots must exist");
        for (Path root : roots) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                    check(root, file);
                }
            }
        }
    }

    @Test
    void misplacedConverterFailsEvenWhenItsCallerUsesTheSamePackage(@TempDir Path root) throws IOException {
        Path file = write(root, "example/infrastructure/goods/converter/BrandPersistenceConverter.java",
                "package example.infrastructure.goods.service.impl;\npublic interface BrandPersistenceConverter {}");
        assertThrows(AssertionError.class, () -> check(root, file));
    }

    @Test
    void businessRequestsAreRejectedAndResultsRemainValid(@TempDir Path root) throws IOException {
        Path request = write(root, "example/adapter/user/pojo/dto/CreateUserRequest.java",
                "package example.adapter.user.pojo.dto;\npublic record CreateUserRequest(String name) {}");
        assertThrows(AssertionError.class, () -> check(root, request));
        Path dto = write(root, "example/adapter/user/pojo/dto/CreateUserDTO.java",
                "package example.adapter.user.pojo.dto;\npublic record CreateUserDTO(String name) {}");
        check(root, dto);
        Path result = write(root, "example/application/user/pojo/result/UserResult.java",
                "package example.application.user.pojo.result;\npublic record UserResult(String name) {}");
        check(root, result);
        Path nested = write(root, "example/adapter/user/pojo/dto/NestedDTO.java",
                "package example.adapter.user.pojo.dto;\npublic class NestedDTO {\n"
                        + "    private record ChunkingConfigRequest(String name) {}\n}");
        assertThrows(AssertionError.class, () -> check(root, nested));
    }

    private static void check(Path root, Path file) throws IOException {
        String relative = root.relativize(file).toString().replace('\\', '/');
        if (relative.equals("module-info.java")) {
            return;
        }
        var declaration = PACKAGE.matcher(Files.readString(file));
        assertTrue(declaration.find(), "missing package: " + file);
        int slash = relative.lastIndexOf('/');
        String expected = slash < 0 ? "" : relative.substring(0, slash).replace('/', '.');
        assertEquals(expected, declaration.group(1), "package/path mismatch: " + file);
        assertFalse(FORBIDDEN_CARRIER.matcher(Files.readString(file)).find(),
                "business input uses DTO/Command/Query; output keeps Result/VO: " + file);
    }

    private static Path write(Path root, String relative, String source) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, source);
    }
}
