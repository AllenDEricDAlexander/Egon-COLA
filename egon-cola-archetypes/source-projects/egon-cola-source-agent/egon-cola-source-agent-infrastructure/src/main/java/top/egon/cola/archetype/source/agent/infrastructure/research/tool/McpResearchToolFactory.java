package top.egon.cola.archetype.source.agent.infrastructure.research.tool;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/** Creates one fail-closed MCP SSE tool snapshot and owns its shutdown lifecycle. */
@Slf4j
public class McpResearchToolFactory implements AutoCloseable {

    private final McpResearchToolProperties properties;
    private final ClientConnector connector;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final List<ClientAdapter> clients = new ArrayList<>();
    private volatile List<ToolCallback> callbacks = List.of();

    public McpResearchToolFactory(McpResearchToolProperties properties) {
        this(properties, McpResearchToolFactory::connectSse);
    }

    public McpResearchToolFactory(McpResearchToolProperties properties, ClientConnector connector) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.connector = Objects.requireNonNull(connector, "connector must not be null");
    }

    public synchronized ToolCallback[] create() {
        if (closed.get()) {
            throw new IllegalStateException("MCP tool factory is closed");
        }
        if (!callbacks.isEmpty()) {
            return callbacks.toArray(ToolCallback[]::new);
        }
        validateProperties(properties);
        ClientAdapter client = null;
        try {
            client = Objects.requireNonNull(connector.connect(properties), "MCP client is missing");
            List<ToolCallback> discovered = List.copyOf(Objects.requireNonNull(client.callbacks(),
                    "MCP callbacks are missing"));
            validateCallbacks(discovered);
            discovered = discovered.stream()
                    .sorted(Comparator.comparing(tool -> tool.getToolDefinition().name()))
                    .toList();
            clients.add(client);
            callbacks = discovered;
            return callbacks.toArray(ToolCallback[]::new);
        } catch (RuntimeException failure) {
            closeQuietly(client);
            throw new IllegalStateException("MCP research tools are unavailable", failure);
        }
    }

    public List<ToolCallback> callbacks() {
        return callbacks;
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        RuntimeException firstFailure = null;
        for (ClientAdapter client : List.copyOf(clients)) {
            try {
                client.close();
            } catch (RuntimeException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                }
                log.warn("MCP research tool shutdown stage=CLIENT outcome=ERROR errorType={}",
                        failure.getClass().getSimpleName());
            }
        }
        clients.clear();
        callbacks = List.of();
        if (firstFailure != null) {
            throw new IllegalStateException("MCP research tool shutdown failed", firstFailure);
        }
    }

    private static void validateProperties(McpResearchToolProperties properties) {
        if (properties.baseUri() == null || properties.baseUri().isBlank()
                || properties.sseEndpoint() == null || properties.sseEndpoint().isBlank()
                || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalArgumentException("MCP research tool configuration is incomplete");
        }
        URI uri = URI.create(properties.baseUri());
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("MCP base URI scheme is unsupported");
        }
        Duration timeout = properties.requestTimeout();
        if (timeout == null || timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("MCP request timeout is outside the allowed range");
        }
    }

    private static void validateCallbacks(List<ToolCallback> callbacks) {
        if (callbacks.isEmpty()) {
            throw new IllegalStateException("MCP research tool list is empty");
        }
        Set<String> names = new HashSet<>();
        for (ToolCallback callback : callbacks) {
            if (callback == null || callback.getToolDefinition() == null
                    || callback.getToolDefinition().name() == null
                    || callback.getToolDefinition().name().isBlank()
                    || !names.add(callback.getToolDefinition().name())) {
                throw new IllegalStateException("MCP research tool names are invalid");
            }
        }
    }

    private static ClientAdapter connectSse(McpResearchToolProperties properties) {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(properties.baseUri())
                .sseEndpoint(properties.sseEndpoint())
                .customizeRequest(builder -> builder.header("Authorization", "Bearer " + properties.apiKey()))
                .connectTimeout(properties.requestTimeout())
                .build();
        McpSyncClient client = null;
        try {
            client = McpClient.sync(transport)
                    .requestTimeout(properties.requestTimeout())
                    .initializationTimeout(properties.requestTimeout())
                    .clientInfo(new McpSchema.Implementation("egon-cola-deep-research", "0.1.0"))
                    .build();
            client.initialize();
            return new SseClientAdapter(client,
                    McpToolUtils.getToolCallbacksFromSyncClients(List.of(client)));
        } catch (RuntimeException failure) {
            if (client != null) {
                client.close();
            } else {
                transport.close();
            }
            throw failure;
        }
    }

    private static void closeQuietly(ClientAdapter client) {
        if (client == null) {
            return;
        }
        try {
            client.close();
        } catch (RuntimeException failure) {
            log.warn("MCP research tool shutdown stage=PARTIAL_CLIENT outcome=ERROR errorType={}",
                    failure.getClass().getSimpleName());
        }
    }

    @FunctionalInterface
    public interface ClientConnector extends Function<McpResearchToolProperties, ClientAdapter> {
        @Override
        ClientAdapter apply(McpResearchToolProperties properties);

        default ClientAdapter connect(McpResearchToolProperties properties) {
            return apply(properties);
        }
    }

    public interface ClientAdapter extends AutoCloseable {
        List<ToolCallback> callbacks();

        @Override
        void close();
    }

    private record SseClientAdapter(McpSyncClient client, List<ToolCallback> callbacks) implements ClientAdapter {
        @Override
        public void close() {
            client.close();
        }
    }
}
