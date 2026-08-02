package top.egon.cola.component.gateway.core.mcp.app;

import java.util.Objects;

/**
 * Port for publishing and reading immutable MCP App artifacts.
 */
public interface McpAppArtifactStore {

    String MCP_APP_MIME_TYPE = "text/html;profile=mcp-app";

    long MAX_ARTIFACT_BYTES = 16L * 1024 * 1024;

    @FunctionalInterface
    interface Writer {

        StoredArtifact write(WriteRequest request);
    }

    @FunctionalInterface
    interface Reader {

        ArtifactContent read(ReadRequest request);
    }

    record WriteRequest(
            String appCode,
            String version,
            byte[] content,
            String expectedSha256
    ) {

        public WriteRequest {
            appCode = required(appCode, "appCode");
            version = required(version, "version");
            content = bytes(content);
            expectedSha256 = sha256(expectedSha256);
            size(content.length);
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    record ReadRequest(
            String artifactReference,
            String expectedSha256,
            long expectedSizeBytes
    ) {

        public ReadRequest {
            artifactReference = required(
                    artifactReference,
                    "artifactReference"
            );
            expectedSha256 = sha256(expectedSha256);
            size(expectedSizeBytes);
        }
    }

    record StoredArtifact(
            String artifactReference,
            String sha256,
            long sizeBytes
    ) {

        public StoredArtifact {
            artifactReference = required(
                    artifactReference,
                    "artifactReference"
            );
            sha256 = McpAppArtifactStore.sha256(sha256);
            size(sizeBytes);
        }
    }

    record ArtifactContent(
            byte[] content,
            String sha256,
            long sizeBytes
    ) {

        public ArtifactContent {
            content = bytes(content);
            sha256 = McpAppArtifactStore.sha256(sha256);
            size(sizeBytes);
            if (content.length != sizeBytes) {
                throw new ArtifactRejectedException(
                        "MCP App artifact size does not match its metadata"
                );
            }
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    final class ArtifactConflictException extends RuntimeException {

        public ArtifactConflictException(String message) {
            super(message);
        }
    }

    final class ArtifactRejectedException extends RuntimeException {

        public ArtifactRejectedException(String message) {
            super(message);
        }

        public ArtifactRejectedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ArtifactRejectedException(
                    "MCP App artifact " + field + " is required"
            );
        }
        return value.trim();
    }

    private static String sha256(String value) {
        String digest = required(value, "sha256").toLowerCase(
                java.util.Locale.ROOT
        );
        if (!digest.matches("[0-9a-f]{64}")) {
            throw new ArtifactRejectedException(
                    "MCP App artifact SHA-256 is invalid"
            );
        }
        return digest;
    }

    private static byte[] bytes(byte[] value) {
        byte[] content = Objects.requireNonNull(value, "content").clone();
        size(content.length);
        return content;
    }

    private static void size(long value) {
        if (value < 1L || value > MAX_ARTIFACT_BYTES) {
            throw new ArtifactRejectedException(
                    "MCP App artifact size is outside the supported range"
            );
        }
    }
}
