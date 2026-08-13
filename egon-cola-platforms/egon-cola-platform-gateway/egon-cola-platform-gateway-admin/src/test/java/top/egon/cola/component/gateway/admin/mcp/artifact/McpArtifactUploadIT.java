package top.egon.cola.component.gateway.admin.mcp.repository.filesystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpArtifactUploadIT {

    @TempDir
    Path temporaryDirectory;

    @Test
    void artifactVersionCannotBeOverwritten() {
        byte[] original = "<html>original</html>".getBytes(
                StandardCharsets.UTF_8
        );
        byte[] changed = "<html>changed</html>".getBytes(
                StandardCharsets.UTF_8
        );
        FileSystemMcpAppArtifactRepository store =
                new FileSystemMcpAppArtifactRepository(temporaryDirectory);

        var first = store.write(request(original));
        var replay = store.write(request(original));

        assertEquals(first, replay);
        assertArrayEquals(original, store.read(new
                McpAppArtifactStore.ReadRequest(
                first.artifactReference(),
                first.sha256(),
                first.sizeBytes()
        )).content());
        assertThrows(
                McpAppArtifactStore.ArtifactConflictException.class,
                () -> store.write(request(changed))
        );
        assertFalse(Files.exists(temporaryDirectory.resolve(
                "apps/dashboard/1.0.0/index.html.tmp"
        )));
    }

    @Test
    void symlinkEscapeAndWrongDigestAreRejected() throws IOException {
        Path outside = Files.createTempDirectory("mcp-app-outside-");
        Files.createSymbolicLink(temporaryDirectory.resolve("apps"), outside);
        FileSystemMcpAppArtifactRepository store =
                new FileSystemMcpAppArtifactRepository(temporaryDirectory);
        byte[] content = "<html>safe</html>".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                McpAppArtifactStore.ArtifactRejectedException.class,
                () -> store.write(request(content))
        );
        assertThrows(
                McpAppArtifactStore.ArtifactRejectedException.class,
                () -> new FileSystemMcpAppArtifactRepository(
                        temporaryDirectory.resolve("clean")
                ).write(new McpAppArtifactStore.WriteRequest(
                        "dashboard",
                        "1.0.0",
                        content,
                        "0".repeat(64)
                ))
        );
    }

    private McpAppArtifactStore.WriteRequest request(byte[] content) {
        return new McpAppArtifactStore.WriteRequest(
                "dashboard",
                "1.0.0",
                content,
                sha256(content)
        );
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (java.security.NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
