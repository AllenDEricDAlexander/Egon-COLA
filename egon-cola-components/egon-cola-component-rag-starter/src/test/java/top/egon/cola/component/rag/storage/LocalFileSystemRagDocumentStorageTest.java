package top.egon.cola.component.rag.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.rag.autoconfigure.RagStorageProperties;
import top.egon.cola.component.rag.exception.RagStorageException;
import top.egon.cola.component.rag.exception.RagValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks the round trip, the path safety and the failure semantics of the local backing. */
class LocalFileSystemRagDocumentStorageTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    @Test
    void round_trips_content_and_deletes_it() throws IOException {
        LocalFileSystemRagDocumentStorage storage = storageAt(root);

        RagStoredObjectBO stored = storage.store("kb-1", "doc-1", "report.txt", stream("hello"));

        assertThat(stored.storageType()).isEqualTo(RagDocumentStorageTypeEnum.LOCAL);
        assertThat(stored.sizeBytes()).isEqualTo(5);
        assertThat(stored.storedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(read(storage.open("kb-1", "doc-1"))).isEqualTo("hello");

        storage.delete("kb-1", "doc-1");

        assertThatThrownBy(() -> storage.open("kb-1", "doc-1")).isInstanceOf(RagStorageException.class);
    }

    @Test
    void keeps_the_stored_path_inside_the_root() {
        LocalFileSystemRagDocumentStorage storage = storageAt(root);

        storage.store("kb-1", "doc-1", "../../escape.txt", stream("hello"));

        assertThat(root.resolve("escape.txt")).doesNotExist();
        assertThat(root.resolve("kb-1").resolve("doc-1")).exists();
    }

    @Test
    void rejects_an_identifier_that_would_escape_the_root() {
        LocalFileSystemRagDocumentStorage storage = storageAt(root);

        assertThatThrownBy(() -> storage.store("..", "doc-1", "a.txt", stream("x")))
                .isInstanceOf(RagValidationException.class);
        assertThatThrownBy(() -> storage.store("kb-1", "a/b", "a.txt", stream("x")))
                .isInstanceOf(RagValidationException.class);
        assertThatThrownBy(() -> storage.store("kb-1", " ", "a.txt", stream("x")))
                .isInstanceOf(RagValidationException.class);
    }

    @Test
    void delete_is_idempotent() {
        LocalFileSystemRagDocumentStorage storage = storageAt(root);

        storage.delete("kb-1", "missing");

        assertThat(root).isEmptyDirectory();
    }

    @Test
    void reports_a_failure_when_the_root_cannot_be_written() throws IOException {
        Path readOnly = Files.createDirectory(root.resolve("readonly"));
        assertThat(readOnly.toFile().setWritable(false)).isTrue();
        LocalFileSystemRagDocumentStorage storage = storageAt(readOnly);

        try {
            assertThatThrownBy(() -> storage.store("kb-1", "doc-1", "a.txt", stream("x")))
                    .isInstanceOf(RagStorageException.class);
        } finally {
            readOnly.toFile().setWritable(true);
        }
    }

    @Test
    void rejects_an_empty_input_stream_target() {
        LocalFileSystemRagDocumentStorage storage = storageAt(root);

        assertThatThrownBy(() -> storage.store("kb-1", "doc-1", "a.txt", null))
                .isInstanceOf(RagValidationException.class);
    }

    private static LocalFileSystemRagDocumentStorage storageAt(Path dir) {
        return new LocalFileSystemRagDocumentStorage(
                new RagStorageProperties.RagLocalStorageProperties(dir.toString()), FIXED_CLOCK);
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(InputStream content) throws IOException {
        try (content) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
