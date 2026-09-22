package top.egon.cola.component.codegen.validation;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Rejects path escape, symlink escape and duplicate normalized paths before any write.
 */
@Slf4j
public class OutputPathValidator {

    public static final String PATH_ESCAPE = "PATH_ESCAPE";

    public static final String DUPLICATE_PATH = "DUPLICATE_PATH";

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
