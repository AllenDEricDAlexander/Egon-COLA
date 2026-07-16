package top.egon.cola.component.bytecode.maven.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.bytecode.core.cache.ClassMetadataCacheEntry;
import top.egon.cola.component.bytecode.core.cache.ClassMetadataCacheKey;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class ArchitectureCacheRepository {

    private final Path directory;
    private final ObjectMapper objectMapper;
    private final Consumer<String> debug;

    public ArchitectureCacheRepository(
            Path directory,
            ObjectMapper objectMapper,
            Consumer<String> debug
    ) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.debug = Objects.requireNonNull(debug, "debug");
    }

    public Optional<ClassMetadataCacheEntry> find(ClassMetadataCacheKey key) {
        Path path = pathFor(key);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try {
            ClassMetadataCacheEntry entry = objectMapper.readValue(
                    path.toFile(), ClassMetadataCacheEntry.class);
            if (!key.equals(entry.key())) {
                throw new IOException("Cache key mismatch");
            }
            return Optional.of(entry);
        } catch (Exception exception) {
            debug.accept("Deleting corrupt architecture cache entry " + path + ": "
                    + exception.getMessage());
            try {
                Files.deleteIfExists(path);
            } catch (IOException deleteFailure) {
                debug.accept("Unable to delete corrupt architecture cache entry " + path + ": "
                        + deleteFailure.getMessage());
            }
            return Optional.empty();
        }
    }

    public void store(ClassMetadataCacheEntry entry) throws IOException {
        Files.createDirectories(directory);
        Path target = pathFor(entry.key());
        Path temporary = Files.createTempFile(directory, entry.key().digest(), ".tmp");
        try {
            objectMapper.writeValue(temporary.toFile(), entry);
            move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public Path pathFor(ClassMetadataCacheKey key) {
        return directory.resolve(key.digest() + ".json");
    }

    private void move(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
