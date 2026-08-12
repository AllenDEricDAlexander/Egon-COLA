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
 * 补充说明 / Supplementary summary: {@code McpTelemetry} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP遥测相关的职责与边界。
 * English supplement: {@code McpTelemetry} is an interface contract in the current Gateway module; it owns the mcp telemetry-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface McpTelemetry {

    /**
     * 中文说明：表示 SCOPEATTRIBUTE 这一固定值；它属于 {@code McpTelemetry} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value scope attribute; it is a state, type, or protocol value of {@code McpTelemetry} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    String SCOPE_ATTRIBUTE = "mcp.telemetry.scope";

    /**
     * 中文说明：执行 start 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 start 的处理结果；returns the result of the operation.
     */
    Scope start(Request request);

    /**
     * 中文说明：执行 noop 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the noop operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 noop 的处理结果；returns the result of the operation.
     */
    static McpTelemetry noop() {
        return ignored -> Scope.noop();
    }

    /**
     * 中文说明：执行 composite 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the composite operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.composite(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observers 参数 observers；parameter observers。
     * @return 返回 composite 的处理结果；returns the result of the operation.
     */
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
                /**
                 * 中文说明：执行 远程提供方 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the remote provider operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.remoteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param providerCode 参数 提供方Code；parameter provider code。
                 */
                @Override
                public void remoteProvider(String providerCode) {
                    scopes.forEach(scope -> safe(
                            () -> scope.remoteProvider(providerCode)
                    ));
                }

                /**
                 * 中文说明：执行 startChild 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the start child operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.startChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param kind 参数 kind；parameter kind。
                 * @return 返回 startChild 的处理结果；returns the result of the operation.
                 */
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
                        /**
                         * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                         * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
                         *
                         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                         */
                        @Override
                        public void success() {
                            children.forEach(child -> safe(child::success));
                        }

                        /**
                         * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                         * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
                         *
                         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                         * @param errorCode 参数 errorCode；parameter error code。
                         */
                        @Override
                        public void failure(String errorCode) {
                            children.forEach(child -> safe(
                                    () -> child.failure(errorCode)
                            ));
                        }
                    };
                }

                /**
                 * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 */
                @Override
                public void success() {
                    scopes.forEach(scope -> safe(scope::success));
                }

                /**
                 * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param errorCode 参数 errorCode；parameter error code。
                 */
                @Override
                public void failure(String errorCode) {
                    scopes.forEach(scope -> safe(
                            () -> scope.failure(errorCode)
                    ));
                }
            };
        };
    }

    /**
     * 中文说明：执行 startSafely 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start safely operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.startSafely(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param telemetry 参数 遥测；parameter telemetry。
     * @param request 参数 请求；parameter request。
     * @return 返回 startSafely 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 current 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    static Scope current(Map<String, Object> attributes) {
        if (attributes == null) {
            return Scope.noop();
        }
        Object value = attributes.get(SCOPE_ATTRIBUTE);
        return value instanceof Scope scope ? scope : Scope.noop();
    }

    /**
     * 中文说明：执行 observeChild 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe child operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.observeChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @param kind 参数 kind；parameter kind。
     * @param source 参数 source；parameter source。
     * @return 返回 observeChild 的处理结果；returns the result of the operation.
     */
    static <T> Publisher<T> observeChild(
            Map<String, Object> attributes,
            ChildKind kind,
            Publisher<T> source) {
        return observeChild(current(attributes), kind, source);
    }

    /**
     * 中文说明：执行 observeChild 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe child operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.observeChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param kind 参数 kind；parameter kind。
     * @param source 参数 source；parameter source。
     * @return 返回 observeChild 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 safeScope 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe scope operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.safeScope(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param delegate 参数 delegate；parameter delegate。
     * @return 返回 safeScope 的处理结果；returns the result of the operation.
     */
    private static Scope safeScope(Scope delegate) {
        Scope value = delegate == null ? Scope.noop() : delegate;
        return new Scope() {
            /**
             * 中文说明：执行 远程提供方 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the remote provider operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.remoteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param providerCode 参数 提供方Code；parameter provider code。
             */
            @Override
            public void remoteProvider(String providerCode) {
                safe(() -> value.remoteProvider(providerCode));
            }

            /**
             * 中文说明：执行 startChild 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the start child operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.startChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param kind 参数 kind；parameter kind。
             * @return 返回 startChild 的处理结果；returns the result of the operation.
             */
            @Override
            public Child startChild(ChildKind kind) {
                return safeChild(value, kind);
            }

            /**
             * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             */
            @Override
            public void success() {
                safe(value::success);
            }

            /**
             * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param errorCode 参数 errorCode；parameter error code。
             */
            @Override
            public void failure(String errorCode) {
                safe(() -> value.failure(errorCode));
            }
        };
    }

    /**
     * 中文说明：执行 safeChild 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe child operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.safeChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param kind 参数 kind；parameter kind。
     * @return 返回 safeChild 的处理结果；returns the result of the operation.
     */
    private static Child safeChild(Scope scope, ChildKind kind) {
        Child delegate;
        try {
            delegate = scope.startChild(kind);
        } catch (RuntimeException ignored) {
            delegate = Child.noop();
        }
        Child value = delegate == null ? Child.noop() : delegate;
        return new Child() {
            /**
             * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             */
            @Override
            public void success() {
                safe(value::success);
            }

            /**
             * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param errorCode 参数 errorCode；parameter error code。
             */
            @Override
            public void failure(String errorCode) {
                safe(() -> value.failure(errorCode));
            }
        };
    }

    /**
     * 中文说明：执行 errorCode 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the error code operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.errorCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 errorCode 的处理结果；returns the result of the operation.
     */
    private static String errorCode(Throwable failure) {
        return failure instanceof McpProtocolException protocol
                ? protocol.code().name()
                : McpErrorCode.MCP_INTERNAL_ERROR.name();
    }

    /**
     * 中文说明：执行 safe 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.safe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param callback 参数 callback；parameter callback。
     */
    private static void safe(Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException ignored) {
            // Telemetry is passive and cannot change request behavior.
        }
    }

    /**
     * 中文说明：{@code Request} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责请求相关的职责与边界。
     * English summary: {@code Request} is an immutable data carrier in the current Gateway module; it owns the request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param method 参数 方法；parameter method。
     * @param primitive 参数 primitive；parameter primitive。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param remoteProviderCode 参数 远程提供方Code；parameter remote provider code。
     * @param attributes 参数 attributes；parameter attributes。
     */
    record Request(
            /**
             * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTelemetry.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code McpTelemetry.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String method,
            /**
             * 中文说明：保存 primitive 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTelemetry.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by primitive; its type is {@code String}, and {@code McpTelemetry.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String primitive,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTelemetry.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpTelemetry.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 远程提供方Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTelemetry.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote provider code; its type is {@code String}, and {@code McpTelemetry.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteProviderCode,
            /**
             * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTelemetry.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code McpTelemetry.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> attributes
    ) {

        /**
         * 中文说明：创建 {@code McpTelemetry.Request} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpTelemetry.Request} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param method 参数 方法；parameter method。
         * @param primitive 参数 primitive；parameter primitive。
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param remoteProviderCode 参数 远程提供方Code；parameter remote provider code。
         * @param attributes 参数 attributes；parameter attributes。
         */
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

    /**
     * 中文说明：{@code Scope} 是接口契约，位于当前 Gateway 模块的相关包中，负责Scope相关的职责与边界。
     * English summary: {@code Scope} is an interface contract in the current Gateway module; it owns the scope-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    interface Scope {

        /**
         * 中文说明：执行 远程提供方 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the remote provider operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.remoteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param providerCode 参数 提供方Code；parameter provider code。
         */
        void remoteProvider(String providerCode);

        /**
         * 中文说明：执行 startChild 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the start child operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.startChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param kind 参数 kind；parameter kind。
         * @return 返回 startChild 的处理结果；returns the result of the operation.
         */
        Child startChild(ChildKind kind);

        /**
         * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        void success();

        /**
         * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param errorCode 参数 errorCode；parameter error code。
         */
        void failure(String errorCode);

        /**
         * 中文说明：执行 noop 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the noop operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 noop 的处理结果；returns the result of the operation.
         */
        static Scope noop() {
            return new Scope() {
                /**
                 * 中文说明：执行 远程提供方 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the remote provider operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.remoteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param providerCode 参数 提供方Code；parameter provider code。
                 */
                @Override
                public void remoteProvider(String providerCode) {
                }

                /**
                 * 中文说明：执行 startChild 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the start child operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.startChild(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param kind 参数 kind；parameter kind。
                 * @return 返回 startChild 的处理结果；returns the result of the operation.
                 */
                @Override
                public Child startChild(ChildKind kind) {
                    return Child.noop();
                }

                /**
                 * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 */
                @Override
                public void success() {
                }

                /**
                 * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry.Scope} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry.Scope} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Scope.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param errorCode 参数 errorCode；parameter error code。
                 */
                @Override
                public void failure(String errorCode) {
                }
            };
        }
    }

    /**
     * 中文说明：{@code Child} 是接口契约，位于当前 Gateway 模块的相关包中，负责Child相关的职责与边界。
     * English summary: {@code Child} is an interface contract in the current Gateway module; it owns the child-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    interface Child {

        /**
         * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry.Child} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry.Child} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Child.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        void success();

        /**
         * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry.Child} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry.Child} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Child.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param errorCode 参数 errorCode；parameter error code。
         */
        void failure(String errorCode);

        /**
         * 中文说明：执行 noop 操作；该方法是 {@code McpTelemetry.Child} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the noop operation; this method is the invocation entry point on {@code McpTelemetry.Child} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Child.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 noop 的处理结果；returns the result of the operation.
         */
        static Child noop() {
            return new Child() {
                /**
                 * 中文说明：执行 success 操作；该方法是 {@code McpTelemetry.Child} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the success operation; this method is the invocation entry point on {@code McpTelemetry.Child} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Child.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 */
                @Override
                public void success() {
                }

                /**
                 * 中文说明：执行 failure 操作；该方法是 {@code McpTelemetry.Child} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpTelemetry.Child} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.Child.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param errorCode 参数 errorCode；parameter error code。
                 */
                @Override
                public void failure(String errorCode) {
                }
            };
        }
    }

    /**
     * 中文说明：{@code ChildKind} 是枚举类型，位于当前 Gateway 模块的相关包中，负责ChildKind相关的职责与边界。
     * English summary: {@code ChildKind} is an enumeration in the current Gateway module; it owns the child kind-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    enum ChildKind {
        /**
         * 中文说明：表示 操作 这一固定值；它属于 {@code McpTelemetry.ChildKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value operation; it is a state, type, or protocol value of {@code McpTelemetry.ChildKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTelemetry.ChildKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.ChildKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        OPERATION,
        /**
         * 中文说明：表示 远程 这一固定值；它属于 {@code McpTelemetry.ChildKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value remote; it is a state, type, or protocol value of {@code McpTelemetry.ChildKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTelemetry.ChildKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.ChildKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        REMOTE,
        /**
         * 中文说明：表示 制品 这一固定值；它属于 {@code McpTelemetry.ChildKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value artifact; it is a state, type, or protocol value of {@code McpTelemetry.ChildKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTelemetry.ChildKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.ChildKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        ARTIFACT,
        /**
         * 中文说明：表示 任务 这一固定值；它属于 {@code McpTelemetry.ChildKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value task; it is a state, type, or protocol value of {@code McpTelemetry.ChildKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTelemetry.ChildKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTelemetry.ChildKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        TASK
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new IllegalArgumentException("MCP " + field + " is required");
        }
        return normalized;
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTelemetry.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
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
