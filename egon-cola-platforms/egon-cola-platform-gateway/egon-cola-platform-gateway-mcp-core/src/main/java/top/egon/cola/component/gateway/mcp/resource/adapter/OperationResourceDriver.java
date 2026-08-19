package top.egon.cola.component.gateway.mcp.resource.adapter;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.operation.GatewayOperationCall;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.Content;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.ReadRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver.rejected;

/**
 * 中文说明：{@code OperationResourceDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责操作资源驱动器相关的职责与边界。
 * English summary: {@code OperationResourceDriver} is a operation resource driver driver in the current Gateway module; it owns the operation resource driver-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class OperationResourceDriver implements McpResourceDriver {

    /**
     * 中文说明：表示 驱动器TYPE 这一固定值；它属于 {@code OperationResourceDriver} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value driver type; it is a state, type, or protocol value of {@code OperationResourceDriver} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code OperationResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code OperationResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String DRIVER_TYPE = "LOCAL_OPERATION";

    /**
     * 中文说明：保存 invoker 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationInvoker}，由 {@code OperationResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by invoker; its type is {@code GatewayOperationInvoker}, and {@code OperationResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code OperationResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code OperationResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayOperationInvoker invoker;

    /**
     * 中文说明：创建 {@code OperationResourceDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code OperationResourceDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param invoker 参数 invoker；parameter invoker。
     */
    public OperationResourceDriver(GatewayOperationInvoker invoker) {
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    /**
     * 中文说明：执行 驱动器Type 操作；该方法是 {@code OperationResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the driver type operation; this method is the invocation entry point on {@code OperationResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationResourceDriver.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
     */
    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code OperationResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code OperationResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationResourceDriver.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Content> read(ReadRequest request) {
        if (request.operationId() == null) {
            throw rejected("MCP resource operation is not configured");
        }
        LinkedHashMap<String, Object> pathArguments = new LinkedHashMap<>();
        pathArguments.putAll(request.uriVariables());
        GatewayOperationInvocation invocation = new GatewayOperationInvocation(
                new GatewayOperationCall(
                        request.operationId(),
                        pathArguments,
                        Map.of("uri", request.uri()),
                        null
                ),
                attribute(request, "originalBearerToken"),
                attribute(request, "callerId"),
                attribute(request, "clientIp"),
                traceHeaders(request)
        );
        return Mono.from(invoker.invoke(invocation)).map(result -> {
            if (result.statusCode() >= 400) {
                throw rejected("MCP resource operation failed");
            }
            return bounded(
                    request,
                    result.body(),
                    textual(request.mimeType())
            );
        });
    }

    /**
     * 中文说明：执行 traceHeaders 操作；该方法是 {@code OperationResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace headers operation; this method is the invocation entry point on {@code OperationResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationResourceDriver.traceHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 traceHeaders 的处理结果；returns the result of the operation.
     */
    private Map<String, String> traceHeaders(ReadRequest request) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        copy(request, headers, "traceparent");
        copy(request, headers, "tracestate");
        copy(request, headers, "x-egon-request-id");
        return Map.copyOf(headers);
    }

    /**
     * 中文说明：执行 copy 操作；该方法是 {@code OperationResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy operation; this method is the invocation entry point on {@code OperationResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationResourceDriver.copy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param target 参数 target；parameter target。
     * @param name 参数 name；parameter name。
     */
    private void copy(
            ReadRequest request,
            Map<String, String> target,
            String name) {
        String value = attribute(request, name);
        if (value != null) {
            target.put(name, value);
        }
    }

    /**
     * 中文说明：执行 attribute 操作；该方法是 {@code OperationResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attribute operation; this method is the invocation entry point on {@code OperationResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationResourceDriver.attribute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param name 参数 name；parameter name。
     * @return 返回 attribute 的处理结果；returns the result of the operation.
     */
    private String attribute(ReadRequest request, String name) {
        Object value = request.attributes().get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }

    /**
     * 中文说明：执行 textual 操作；该方法是 {@code OperationResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the textual operation; this method is the invocation entry point on {@code OperationResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationResourceDriver.textual(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mimeType 参数 mimeType；parameter mime type。
     * @return 返回 textual 的处理结果；returns the result of the operation.
     */
    private boolean textual(String mimeType) {
        return mimeType.startsWith("text/")
                || "application/json".equals(mimeType)
                || mimeType.endsWith("+json");
    }
}
