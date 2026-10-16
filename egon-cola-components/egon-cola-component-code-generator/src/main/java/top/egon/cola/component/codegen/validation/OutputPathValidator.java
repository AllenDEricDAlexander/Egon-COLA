package top.egon.cola.component.codegen.validation;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rejects unsafe paths and inconsistent generated Java declarations before any write.
 */
@Slf4j
public class OutputPathValidator {

    public static final String PATH_ESCAPE = "PATH_ESCAPE";

    public static final String DUPLICATE_PATH = "DUPLICATE_PATH";

    public static final String JAVA_SOURCE_MISMATCH = "JAVA_SOURCE_MISMATCH";

    private static final Pattern PACKAGE = Pattern.compile(
            "\\A(?:\\s|//[^\\r\\n]*|/\\*[\\s\\S]*?\\*/)*package\\s+([\\w.]+)\\s*;");

    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "(?m)^\\s*public\\s+(?:(?:abstract|final|sealed|non-sealed)\\s+)*"
                    + "(?:class|interface|enum|record)\\s+(\\w+)\\b");

    public void checkJavaSource(String relativePath, byte[] bytes) {
        String marker = "src/main/java/";
        int start = relativePath.indexOf(marker);
        if (start < 0 || (start > 0 && relativePath.charAt(start - 1) != '/')
                || !relativePath.endsWith(".java")) {
            throw new PathValidationException(JAVA_SOURCE_MISMATCH, relativePath);
        }
        String javaPath = relativePath.substring(start + marker.length());
        int slash = javaPath.lastIndexOf('/');
        String expectedPackage = slash < 0 ? "" : javaPath.substring(0, slash).replace('/', '.');
        String expectedType = javaPath.substring(slash + 1, javaPath.length() - ".java".length());
        String source = new String(bytes, StandardCharsets.UTF_8);
        var packageDeclaration = PACKAGE.matcher(source);
        var typeDeclaration = PUBLIC_TYPE.matcher(source);
        if (!packageDeclaration.find() || !expectedPackage.equals(packageDeclaration.group(1))
                || !typeDeclaration.find() || !expectedType.equals(typeDeclaration.group(1))) {
            throw new PathValidationException(JAVA_SOURCE_MISMATCH, relativePath);
        }
    }

    public void check(Path outputRoot, String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..")
                || relativePath.startsWith("/") || relativePath.contains("\\")) {
            throw failure(relativePath);
        }
        Path root = real(outputRoot);
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw failure(relativePath);
        }
        Path cursor = target;
        while (cursor != null && !cursor.equals(root)) {
            if (Files.isSymbolicLink(cursor)) {
                Path linked = real(cursor);
                if (!linked.startsWith(root)) {
                    log.warn("symlink escapes output root at {}", relativePath);
                    throw failure(relativePath);
                }
            }
            cursor = cursor.getParent();
        }
    }

    public void checkDistinct(List<String> relativePaths) {
        Set<String> seen = new LinkedHashSet<>();
        for (String path : relativePaths) {
            String key = path.replace('\\', '/').toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                throw new PathValidationException(DUPLICATE_PATH, path);
            }
        }
    }

    private static Path real(Path path) {
        try {
            Path absolute = path.toAbsolutePath().normalize();
            if (Files.exists(absolute)) {
                return absolute.toRealPath();
            }
            Path parent = absolute.getParent();
            if (parent != null && Files.exists(parent)) {
                return parent.toRealPath().resolve(absolute.getFileName()).normalize();
            }
            return absolute;
        } catch (IOException exception) {
            throw new PathValidationException(PATH_ESCAPE, path.toString());
        }
    }

    private static PathValidationException failure(String path) {
        return new PathValidationException(PATH_ESCAPE, path);
    }

    public static final class PathValidationException extends RuntimeException {

        private final String code;

        public PathValidationException(String code, String path) {
            super(path);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
