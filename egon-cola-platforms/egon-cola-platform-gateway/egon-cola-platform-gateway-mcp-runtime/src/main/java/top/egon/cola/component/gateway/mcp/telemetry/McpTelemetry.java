package top.egon.cola.component.gateway.mcp.telemetry;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Passive observer port for bounded MCP metrics, traces and audit events.
 */
@FunctionalInterface
public interface McpTelemetry {

    String SCOPE_ATTRIBUTE = "mcp.telemetry.scope";

    Scope start(Request request);

    static McpTelemetry noop() {
        return ignored -> Scope.noop();
    }

    static McpTelemetry composite(List<McpTelemetry> observers) {
        List<McpTelemetry> values = List.copyOf(Objects.requireNonNull(
                observers,
                "observers"
        ));
        return request -> {
            ArrayList<Scope> scopes = new ArrayList<>(values.size());
            values.forEach(observer -> {
                try {
                    scopes.add(Objects.requireNonNull(
                            observer,
                            "observer"
                    ).start(request));
                } catch (RuntimeException ignored) {
                    scopes.add(Scope.noop());
                }
            });
            return new Scope() {
                @Override
                public void remoteProvider(String providerCode) {
                    scopes.forEach(scope -> safe(
                            () -> scope.remoteProvider(providerCode)
                    ));
                }

                @Override
                public Child startChild(ChildKind kind) {
                    ArrayList<Child> children = new ArrayList<>(scopes.size());
                    scopes.forEach(scope -> {
                        try {
                            children.add(scope.startChild(kind));
                        } catch (RuntimeException ignored) {
                            children.add(Child.noop());
                        }
                    });
                    return new Child() {
                        @Override
                        public void success() {
                            children.forEach(child -> safe(child::success));
                        }

                        @Override
                        public void failure(String errorCode) {
                            children.forEach(child -> safe(
                                    () -> child.failure(errorCode)
                            ));
                        }
                    };
                }

                @Override
                public void success() {
                    scopes.forEach(scope -> safe(scope::success));
                }

                @Override
                public void failure(String errorCode) {
                    scopes.forEach(scope -> safe(
                            () -> scope.failure(errorCode)
                    ));
                }
            };
        };
    }

    static Scope startSafely(McpTelemetry telemetry, Request request) {
        try {
            return safeScope(Objects.requireNonNull(
                    telemetry,
                    "telemetry"
            ).start(request));
        } catch (RuntimeException ignored) {
            return Scope.noop();
        }
    }

    static Scope current(Map<String, Object> attributes) {
        if (attributes == null) {
            return Scope.noop();
        }
        Object value = attributes.get(SCOPE_ATTRIBUTE);
        return value instanceof Scope scope ? scope : Scope.noop();
    }

    static <T> Publisher<T> observeChild(
            Map<String, Object> attributes,
            ChildKind kind,
            Publisher<T> source) {
        return observeChild(current(attributes), kind, source);
    }

    static <T> Publisher<T> observeChild(
            Scope scope,
            ChildKind kind,
            Publisher<T> source) {
        Objects.requireNonNull(source, "source");
        Scope parent = Objects.requireNonNull(scope, "scope");
        ChildKind childKind = Objects.requireNonNull(kind, "kind");
        return Flux.defer(() -> {
            Child child = safeChild(parent, childKind);
            return Flux.from(source)
                    .doOnNext(ignored -> child.success())
                    .doOnError(failure -> child.failure(errorCode(failure)))
                    .doOnComplete(child::success)
                    .doOnCancel(() -> child.failure("CANCELLED"));
        });
    }

    private static Scope safeScope(Scope delegate) {
        Scope value = delegate == null ? Scope.noop() : delegate;
        return new Scope() {
            @Override
            public void remoteProvider(String providerCode) {
                safe(() -> value.remoteProvider(providerCode));
            }

            @Override
            public Child startChild(ChildKind kind) {
                return safeChild(value, kind);
            }

            @Override
            public void success() {
                safe(value::success);
            }

            @Override
            public void failure(String errorCode) {
                safe(() -> value.failure(errorCode));
            }
        };
    }

    private static Child safeChild(Scope scope, ChildKind kind) {
        Child delegate;
        try {
            delegate = scope.startChild(kind);
        } catch (RuntimeException ignored) {
            delegate = Child.noop();
        }
        Child value = delegate == null ? Child.noop() : delegate;
        return new Child() {
            @Override
            public void success() {
                safe(value::success);
            }

            @Override
            public void failure(String errorCode) {
                safe(() -> value.failure(errorCode));
            }
        };
    }

    private static String errorCode(Throwable failure) {
        return failure instanceof McpProtocolException protocol
                ? protocol.code().name()
                : McpErrorCode.MCP_INTERNAL_ERROR.name();
    }

    private static void safe(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException ignored) {
            // Telemetry is passive and cannot change request behavior.
        }
    }

    record Request(
            String method,
            String primitive,
            String serverCode,
            String remoteProviderCode,
            Map<String, Object> attributes
    ) {

        public Request {
            method = required(method, "method");
            primitive = required(primitive, "primitive");
            serverCode = required(serverCode, "serverCode");
            remoteProviderCode = optional(remoteProviderCode);
            attributes = attributes == null
                    ? Map.of()
                    : Map.copyOf(attributes);
        }
    }

    interface Scope {

        void remoteProvider(String providerCode);

        Child startChild(ChildKind kind);

        void success();

        void failure(String errorCode);

        static Scope noop() {
            return new Scope() {
                @Override
                public void remoteProvider(String providerCode) {
                }

                @Override
                public Child startChild(ChildKind kind) {
                    return Child.noop();
                }

                @Override
                public void success() {
                }

                @Override
                public void failure(String errorCode) {
                }
            };
        }
    }

    interface Child {

        void success();

        void failure(String errorCode);

        static Child noop() {
            return new Child() {
                @Override
                public void success() {
                }

                @Override
                public void failure(String errorCode) {
                }
            };
        }
    }

    enum ChildKind {
        OPERATION,
        REMOTE,
        ARTIFACT,
        TASK
    }

    private static String required(String value, String field) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new IllegalArgumentException("MCP " + field + " is required");
        }
        return normalized;
    }

    private static String optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= 128
                ? normalized
                : normalized.substring(0, 128);
    }
}
