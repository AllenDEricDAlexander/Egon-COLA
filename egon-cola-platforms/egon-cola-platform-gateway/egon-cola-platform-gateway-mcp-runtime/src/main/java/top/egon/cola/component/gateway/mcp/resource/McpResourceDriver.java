package top.egon.cola.component.gateway.mcp.resource;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy for reading one reviewed MCP resource descriptor.
 */
public interface McpResourceDriver {

    String driverType();

    Publisher<Content> read(ReadRequest request);

    record ReadRequest(
            String serverCode,
            String name,
            String uri,
            String mimeType,
            String operationId,
            Map<String, String> configuration,
            Map<String, String> uriVariables,
            long maximumBytes,
            Map<String, Object> attributes
    ) {

        public ReadRequest {
            serverCode = required(serverCode, "serverCode");
            name = required(name, "name");
            uri = required(uri, "uri");
            mimeType = mime(mimeType);
            operationId = optional(operationId);
            configuration = configuration == null
                    ? Map.of()
                    : Map.copyOf(configuration);
            uriVariables = uriVariables == null
                    ? Map.of()
                    : Map.copyOf(uriVariables);
            if (maximumBytes < 1L || maximumBytes > 64L * 1024 * 1024) {
                throw rejected("MCP resource maximumBytes is invalid");
            }
            attributes = attributes == null ? Map.of() : Map.copyOf(
                    attributes
            );
        }
    }

    final class Content {

        private final String uri;

        private final String mimeType;

        private final byte[] data;

        private final boolean textual;

        private final Map<String, Object> metadata;

        public Content(
                String uri,
                String mimeType,
                byte[] data,
                boolean textual) {
            this(uri, mimeType, data, textual, Map.of());
        }

        public Content(
                String uri,
                String mimeType,
                byte[] data,
                boolean textual,
                Map<String, Object> metadata) {
            this.uri = required(uri, "uri");
            this.mimeType = mime(mimeType);
            this.data = Objects.requireNonNull(data, "data").clone();
            this.textual = textual;
            this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        public String uri() {
            return uri;
        }

        public String mimeType() {
            return mimeType;
        }

        public byte[] data() {
            return data.clone();
        }

        public boolean textual() {
            return textual;
        }

        public Map<String, Object> metadata() {
            return metadata;
        }

        public String text() {
            if (!textual) {
                throw new IllegalStateException("MCP resource is binary");
            }
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    static Content bounded(
            ReadRequest request,
            byte[] data,
            boolean textual) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(data, "data");
        if (data.length > request.maximumBytes()) {
            throw rejected("MCP resource exceeds its maximum size");
        }
        return new Content(
                request.uri(),
                request.mimeType(),
                data,
                textual
        );
    }

    static McpProtocolException rejected(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_RESOURCE_REJECTED,
                message
        );
    }

    private static String mime(String value) {
        String result = required(value, "mimeType");
        if (result.length() > 255
                || result.indexOf('/') < 1
                || result.contains("\r")
                || result.contains("\n")) {
            throw rejected("MCP resource MIME type is invalid");
        }
        return result;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw rejected("MCP resource " + field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
