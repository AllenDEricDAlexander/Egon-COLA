package top.egon.cola.component.gateway.mcp.resource;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.rejected;

/**
 * Reads an allowlisted database schema object through a supplied metadata port.
 */
public final class DatabaseSchemaResourceDriver
        implements McpResourceDriver {

    public static final String DRIVER_TYPE = "DATABASE_SCHEMA";

    private final SchemaReader reader;

    private final McpResourceUriValidator validator;

    public DatabaseSchemaResourceDriver(
            SchemaReader reader,
            McpResourceUriValidator validator) {
        this.reader = Objects.requireNonNull(reader, "reader");
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

    private Content readBlocking(ReadRequest request) throws Exception {
        String[] segments = validator.validate(request.uri())
                .getPath()
                .split("/", -1);
        if (segments.length != 4
                || !"schema".equals(segments[1])
                || segments[2].isBlank()
                || segments[3].isBlank()) {
            throw rejected("MCP database schema URI is invalid");
        }
        String schema = segments[2];
        String objectName = segments[3];
        if (!identifier(schema) || !identifier(objectName)
                || !allowed(request).contains(schema)) {
            throw rejected("MCP database schema is not allowlisted");
        }
        String content = reader.read(schema, objectName);
        if (content == null) {
            throw rejected("MCP database schema object was not found");
        }
        return bounded(
                request,
                content.getBytes(StandardCharsets.UTF_8),
                true
        );
    }

    private Set<String> allowed(ReadRequest request) {
        String value = request.configuration().get("allowedSchemas");
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        TreeSet<String> schemas = new TreeSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(this::identifier)
                .forEach(schemas::add);
        return Set.copyOf(schemas);
    }

    private boolean identifier(String value) {
        return value != null
                && value.matches("[A-Za-z_][A-Za-z0-9_]{0,62}");
    }

    @FunctionalInterface
    public interface SchemaReader {

        String read(String schema, String objectName) throws Exception;
    }
}
