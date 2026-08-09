package top.egon.cola.component.gateway.mcp.resource;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.rejected;

/**
 * Reads a file below a configured real root and rejects symlink escape.
 */
public final class ObjectStorageResourceDriver
        implements McpResourceDriver {

    public static final String DRIVER_TYPE = "OBJECT_STORAGE";

    private final McpResourceUriValidator validator;

    public ObjectStorageResourceDriver(McpResourceUriValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    @Override
    public Mono<Content> read(ReadRequest request) {
        return Mono.fromCallable(() -> readBlocking(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Content readBlocking(ReadRequest request) {
        URI uri = validator.validate(request.uri());
        String rootValue = request.configuration().get("root");
        if (rootValue == null || rootValue.isBlank()) {
            throw rejected("MCP object storage root is not configured");
        }
        try {
            Path root = Path.of(rootValue).toRealPath();
            String relativeValue = uri.getPath().substring(1);
            Path lexical = root.resolve(relativeValue).normalize();
            if (!lexical.startsWith(root)) {
                throw rejected("MCP object storage traversal is forbidden");
            }
            Path target = lexical.toRealPath();
            if (!target.startsWith(root)
                    || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw rejected("MCP object storage target is rejected");
            }
            long size = Files.size(target);
            if (size > request.maximumBytes()) {
                throw rejected("MCP resource exceeds its maximum size");
            }
            return bounded(
                    request,
                    Files.readAllBytes(target),
                    textual(request.mimeType())
            );
        } catch (McpProtocolException failure) {
            throw failure;
        } catch (Exception failure) {
            throw rejected("MCP object storage read was rejected");
        }
    }

    private boolean textual(String mimeType) {
        return mimeType.startsWith("text/")
                || "application/json".equals(mimeType)
                || mimeType.endsWith("+json")
                || "application/xml".equals(mimeType)
                || mimeType.endsWith("+xml");
    }
}
