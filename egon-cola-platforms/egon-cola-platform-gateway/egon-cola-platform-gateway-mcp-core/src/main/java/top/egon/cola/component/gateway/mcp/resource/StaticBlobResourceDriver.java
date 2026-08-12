package top.egon.cola.component.gateway.mcp.resource;

import reactor.core.publisher.Mono;

import java.util.Base64;

import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.rejected;

/**
 * 中文说明：{@code StaticBlobResourceDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责StaticBlob资源驱动器相关的职责与边界。
 * English summary: {@code StaticBlobResourceDriver} is a static blob resource driver driver in the current Gateway module; it owns the static blob resource driver-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class StaticBlobResourceDriver implements McpResourceDriver {

    /**
     * 中文说明：表示 驱动器TYPE 这一固定值；它属于 {@code StaticBlobResourceDriver} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value driver type; it is a state, type, or protocol value of {@code StaticBlobResourceDriver} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code StaticBlobResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code StaticBlobResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String DRIVER_TYPE = "STATIC_BLOB";

    /**
     * 中文说明：执行 驱动器Type 操作；该方法是 {@code StaticBlobResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the driver type operation; this method is the invocation entry point on {@code StaticBlobResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code StaticBlobResourceDriver.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
     */
    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code StaticBlobResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code StaticBlobResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code StaticBlobResourceDriver.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Content> read(ReadRequest request) {
        String encoded = request.configuration().get("base64");
        if (encoded == null || encoded.isBlank()) {
            throw rejected("MCP static blob content is not configured");
        }
        try {
            return Mono.just(bounded(
                    request,
                    Base64.getDecoder().decode(encoded),
                    false
            ));
        } catch (IllegalArgumentException failure) {
            throw rejected("MCP static blob content is invalid");
        }
    }
}
