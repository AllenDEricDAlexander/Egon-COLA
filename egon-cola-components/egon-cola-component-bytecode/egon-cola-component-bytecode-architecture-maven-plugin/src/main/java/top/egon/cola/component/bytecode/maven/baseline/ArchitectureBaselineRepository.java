package top.egon.cola.component.bytecode.maven.baseline;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class ArchitectureBaselineRepository {

    private final Path path;
    private final ObjectMapper objectMapper;

    public ArchitectureBaselineRepository(Path path, ObjectMapper objectMapper) {
        this.path = Objects.requireNonNull(path, "path");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public ArchitectureBaseline load() throws IOException {
        if (Files.notExists(path)) {
            return ArchitectureBaseline.empty();
        }
        ArchitectureBaseline baseline = objectMapper.readValue(path.toFile(), ArchitectureBaseline.class);
        if (baseline.schemaVersion() != ArchitectureBaseline.CURRENT_SCHEMA) {
            throw new IOException("Unsupported architecture baseline schema: " + baseline.schemaVersion());
        }
        return baseline;
    }

    public void generate(ArchitectureBaseline baseline, boolean overwrite) throws IOException {
        if (Files.exists(path) && !overwrite) {
            throw new IOException("Architecture baseline already exists; set overwrite=true: " + path);
        }
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), baseline);
            move(temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void move(Path temporary) throws IOException {
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
