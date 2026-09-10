package top.egon.cola.component.rag.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.rag.autoconfigure.RagStorageProperties;
import top.egon.cola.component.rag.exception.RagStorageException;
import top.egon.cola.component.rag.exception.RagValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Stores documents on the local file system under {@code <root>/<collectionId>/<documentId>/}.
 *
 * <p>Not shareable across instances of a multi-instance deployment: each instance would only see
 * the files it wrote itself.
 *
 * <p>Writes go to a sibling temporary file first and are then moved into place atomically, so a
 * failed write never leaves a half-written document behind.
 */
@Slf4j
@RequiredArgsConstructor
public class LocalFileSystemRagDocumentStorage implements RagDocumentStorage {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private static final String FALLBACK_FILE_NAME = "document";

    private final RagStorageProperties.RagLocalStorageProperties localProperties;

    private final Clock clock;

    @Override
    public RagDocumentStorageTypeEnum type() {
        return RagDocumentStorageTypeEnum.LOCAL;
    }

    @Override
    public RagStoredObjectBO store(String collectionId, String documentId, String fileName, InputStream content) {
        if (content == null) {
            throw new RagValidationException("content must not be null");
        }
        Path directory = directoryOf(collectionId, documentId);
        Path target = directory.resolve(sanitize(fileName));
        Path temporary = directory.resolve(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(directory);
            long size = Files.copy(content, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            log.info("rag document stored: collection={}, document={}, type=LOCAL, bytes={}",
                    collectionId, documentId, size);
            return new RagStoredObjectBO(collectionId, documentId, type(), size, clock.instant());
        } catch (IOException exception) {
            deleteQuietly(temporary);
            throw new RagStorageException("failed to store document " + documentId, exception);
        }
    }

    @Override
    public InputStream open(String collectionId, String documentId) {
        Path directory = directoryOf(collectionId, documentId);
        try (Stream<Path> entries = Files.list(directory)) {
            Path stored = entries.filter(Files::isRegularFile).findFirst()
                    .orElseThrow(() -> new RagStorageException("no stored document for " + documentId));
            return Files.newInputStream(stored);
        } catch (IOException exception) {
            throw new RagStorageException("failed to read stored document " + documentId, exception);
        }
    }

    @Override
    public void delete(String collectionId, String documentId) {
        Path directory = directoryOf(collectionId, documentId);
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> entries = Files.walk(directory)) {
            for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
            log.info("rag document deleted: collection={}, document={}, type=LOCAL", collectionId, documentId);
        } catch (IOException exception) {
            throw new RagStorageException("failed to delete stored document " + documentId, exception);
        }
    }

    /**
     * Resolves the per-document directory, rejecting any identifier that could leave the root.
     *
     * <p>The character class already excludes the path separators; the explicit dot check rejects the
     * traversal names that the class would otherwise admit.
     */
    private Path directoryOf(String collectionId, String documentId) {
        return root().resolve(segment(collectionId, "collectionId")).resolve(segment(documentId, "documentId"));
    }

    private Path root() {
        return Path.of(localProperties.root()).toAbsolutePath().normalize();
    }

    private static String segment(String value, String name) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty() || ".".equals(trimmed) || "..".equals(trimmed)
                || !SEGMENT.matcher(trimmed).matches()) {
            throw new RagValidationException(name + " is not a usable storage path segment");
        }
        return trimmed;
    }

    /** Keeps only the base name, so a caller-supplied path cannot add directory components. */
    private static String sanitize(String fileName) {
        if (fileName == null) {
            return FALLBACK_FILE_NAME;
        }
        String baseName = fileName.trim().replace('\\', '/');
        int separator = baseName.lastIndexOf('/');
        if (separator >= 0) {
            baseName = baseName.substring(separator + 1);
        }
        if (baseName.isEmpty() || ".".equals(baseName) || "..".equals(baseName)) {
            return FALLBACK_FILE_NAME;
        }
        return baseName;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            log.warn("rag storage left a temporary file behind: {}", path.getFileName());
        }
    }
}
