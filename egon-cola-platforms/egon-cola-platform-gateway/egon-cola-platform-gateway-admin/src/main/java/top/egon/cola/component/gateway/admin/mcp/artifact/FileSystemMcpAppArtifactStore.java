package top.egon.cola.component.gateway.admin.mcp.artifact;

import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ArtifactContent;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ReadRequest;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.StoredArtifact;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.WriteRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Filesystem adapter that atomically publishes immutable MCP App artifacts.
 */
public final class FileSystemMcpAppArtifactStore
        implements McpAppArtifactStore.Writer, McpAppArtifactStore.Reader {

    private static final String ENTRY_FILE = "index.html";

    private final Path root;

    public FileSystemMcpAppArtifactStore(Path root) {
        this.root = prepareRoot(root);
    }

    @Override
    public StoredArtifact write(WriteRequest request) {
        byte[] content = request.content();
        String actualDigest = sha256(content);
        if (!actualDigest.equals(request.expectedSha256())) {
            throw rejected("MCP App upload SHA-256 does not match");
        }
        validateSegment(request.appCode(), "appCode");
        validateSegment(request.version(), "version");
        String reference = "apps/" + request.appCode() + "/"
                + request.version() + "/" + ENTRY_FILE;
        Path directory = ensureDirectories(
                "apps",
                request.appCode(),
                request.version()
        );
        Path target = contained(directory.resolve(ENTRY_FILE));
        Path lockPath = contained(directory.resolve(".upload.lock"));
        try (FileChannel lockChannel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        ); FileLock ignored = lockChannel.lock()) {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                return existing(target, reference, actualDigest, content.length);
            }
            Path temporary = Files.createTempFile(
                    directory,
                    ".index.html-",
                    ".tmp"
            );
            try {
                writeAndForce(temporary, content);
                if (!actualDigest.equals(sha256(readRegular(temporary)))) {
                    throw rejected("MCP App upload verification failed");
                }
                try {
                    Files.move(
                            temporary,
                            target,
                            StandardCopyOption.ATOMIC_MOVE
                    );
                } catch (FileAlreadyExistsException race) {
                    return existing(
                            target,
                            reference,
                            actualDigest,
                            content.length
                    );
                }
                forceDirectory(directory);
                return new StoredArtifact(
                        reference,
                        actualDigest,
                        content.length
                );
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (McpAppArtifactStore.ArtifactConflictException
                 | McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact could not be stored", failure);
        }
    }

    @Override
    public ArtifactContent read(ReadRequest request) {
        Path target = resolveReference(request.artifactReference());
        byte[] content = readRegular(target);
        String actualDigest = sha256(content);
        if (!actualDigest.equals(request.expectedSha256())
                || content.length != request.expectedSizeBytes()) {
            throw rejected("MCP App artifact metadata does not match storage");
        }
        return new ArtifactContent(
                content,
                actualDigest,
                content.length
        );
    }

    private StoredArtifact existing(
            Path target,
            String reference,
            String expectedDigest,
            long expectedSize) {
        byte[] content = readRegular(target);
        String storedDigest = sha256(content);
        if (!storedDigest.equals(expectedDigest)
                || content.length != expectedSize) {
            throw new McpAppArtifactStore.ArtifactConflictException(
                    "MCP App artifact version is immutable"
            );
        }
        return new StoredArtifact(reference, storedDigest, content.length);
    }

    private Path resolveReference(String reference) {
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
                || !ENTRY_FILE.equals(relative.getName(3).toString())) {
            throw rejected("MCP App artifact reference is invalid");
        }
        validateSegment(relative.getName(1).toString(), "appCode");
        validateSegment(relative.getName(2).toString(), "version");
        Path target = contained(root.resolve(relative));
        verifyExistingPath(target);
        return target;
    }

    private Path ensureDirectories(String... segments) {
        Path current = root;
        for (String segment : segments) {
            validateSegment(segment, "path segment");
            current = contained(current.resolve(segment));
            try {
                Files.createDirectory(current);
            } catch (FileAlreadyExistsException ignored) {
                // The path is verified below without following symbolic links.
            } catch (IOException failure) {
                throw rejected(
                        "MCP App artifact directory could not be created",
                        failure
                );
            }
            if (Files.isSymbolicLink(current)
                    || !Files.isDirectory(
                    current,
                    LinkOption.NOFOLLOW_LINKS
            )) {
                throw rejected("MCP App artifact path is not a directory");
            }
        }
        return current;
    }

    private void verifyExistingPath(Path target) {
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
    }

    private byte[] readRegular(Path path) {
        verifyExistingPath(path);
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

    private void writeAndForce(Path target, byte[] content)
            throws IOException {
        try (FileChannel channel = FileChannel.open(
                target,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                LinkOption.NOFOLLOW_LINKS
        )) {
            ByteBuffer buffer = ByteBuffer.wrap(content);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private void forceDirectory(Path directory) throws IOException {
        try (FileChannel channel = FileChannel.open(
                directory,
                StandardOpenOption.READ
        )) {
            channel.force(true);
        }
    }

    private Path contained(Path value) {
        Path normalized = value.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw rejected("MCP App artifact path escapes its root");
        }
        return normalized;
    }

    private static Path prepareRoot(Path value) {
        if (value == null) {
            throw rejected("MCP App artifact root is required");
        }
        Path normalized = value.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            if (Files.isSymbolicLink(normalized)
                    || !Files.isDirectory(
                    normalized,
                    LinkOption.NOFOLLOW_LINKS
            )) {
                throw rejected("MCP App artifact root is invalid");
            }
            return normalized.toRealPath();
        } catch (McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact root is unavailable", failure);
        }
    }

    private static void validateSegment(String value, String field) {
        if (value == null
                || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
                || ".".equals(value)
                || "..".equals(value)) {
            throw rejected("MCP App artifact " + field + " is invalid");
        }
    }

    private static String sha256(byte[] content) {
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
