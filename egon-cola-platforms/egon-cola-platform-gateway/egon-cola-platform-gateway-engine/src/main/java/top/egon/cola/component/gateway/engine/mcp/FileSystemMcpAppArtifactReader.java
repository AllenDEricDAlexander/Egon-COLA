package top.egon.cola.component.gateway.engine.mcp;

import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ArtifactContent;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ReadRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Read-only data-plane adapter for immutable MCP App artifacts.
 */
public final class FileSystemMcpAppArtifactReader
        implements McpAppArtifactStore.Reader {

    private final Path root;

    public FileSystemMcpAppArtifactReader(Path root) {
        if (root == null) {
            throw rejected("MCP App artifact root is required");
        }
        Path normalized = root.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            if (Files.isSymbolicLink(normalized)) {
                throw rejected("MCP App artifact root cannot be a symlink");
            }
            this.root = normalized.toRealPath();
        } catch (McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact root is unavailable", failure);
        }
    }

    @Override
    public ArtifactContent read(ReadRequest request) {
        Path target = resolve(request.artifactReference());
        byte[] content = readRegular(target);
        String digest = sha256(content);
        if (!digest.equals(request.expectedSha256())
                || content.length != request.expectedSizeBytes()) {
            throw rejected("MCP App artifact metadata does not match storage");
        }
        return new ArtifactContent(content, digest, content.length);
    }

    private Path resolve(String reference) {
        if (reference.indexOf('\\') >= 0) {
            throw rejected("MCP App artifact reference is invalid");
        }
        Path relative;
        try {
            relative = Path.of(reference);
        } catch (RuntimeException failure) {
            throw rejected("MCP App artifact reference is invalid", failure);
        }
        if (relative.isAbsolute()
                || relative.getNameCount() != 4
                || !"apps".equals(relative.getName(0).toString())
                || !"index.html".equals(relative.getName(3).toString())) {
            throw rejected("MCP App artifact reference is invalid");
        }
        Path target = root.resolve(relative).toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw rejected("MCP App artifact path escapes its root");
        }
        Path current = root;
        for (Path segment : root.relativize(target)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw rejected("MCP App artifact symlink is forbidden");
            }
        }
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw rejected("MCP App artifact was not found");
        }
        return target;
    }

    private byte[] readRegular(Path path) {
        try (FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS
        )) {
            long size = channel.size();
            if (size < 1L || size > McpAppArtifactStore.MAX_ARTIFACT_BYTES) {
                throw rejected("MCP App artifact size is invalid");
            }
            ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(size));
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // Continue until the bounded artifact is fully read.
            }
            if (buffer.hasRemaining() || channel.read(ByteBuffer.allocate(1)) >= 0) {
                throw rejected("MCP App artifact changed while being read");
            }
            return buffer.array();
        } catch (McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact could not be read", failure);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private static McpAppArtifactStore.ArtifactRejectedException rejected(
            String message) {
        return new McpAppArtifactStore.ArtifactRejectedException(message);
    }

    private static McpAppArtifactStore.ArtifactRejectedException rejected(
            String message,
            Throwable cause) {
        return new McpAppArtifactStore.ArtifactRejectedException(
                message,
                cause
        );
    }
}
