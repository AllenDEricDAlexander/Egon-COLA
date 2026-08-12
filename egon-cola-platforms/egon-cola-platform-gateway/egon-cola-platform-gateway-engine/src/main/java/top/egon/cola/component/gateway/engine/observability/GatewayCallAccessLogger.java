package top.egon.cola.component.gateway.engine.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogEvent;

import java.util.Base64;

/**
 * 中文说明：{@code GatewayCallAccessLogger} 是类型，位于当前 Gateway 模块的相关包中，负责网关调用AccessLogger相关的职责与边界。
 * English summary: {@code GatewayCallAccessLogger} is a type in the current Gateway module; it owns the gateway call access logger-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCallAccessLogger
        implements GatewayCallCompletionListener {

    /**
     * 中文说明：表示 LOGGER 这一固定值；它属于 {@code GatewayCallAccessLogger} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value logger; it is a state, type, or protocol value of {@code GatewayCallAccessLogger} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallAccessLogger} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallAccessLogger}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger("gateway.call.access");

    /**
     * 中文说明：表示 BODYLOGGER 这一固定值；它属于 {@code GatewayCallAccessLogger} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value body logger; it is a state, type, or protocol value of {@code GatewayCallAccessLogger} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallAccessLogger} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallAccessLogger}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Logger BODY_LOGGER =
            LoggerFactory.getLogger("gateway.body.access");

    /**
     * 中文说明：执行 onComplete 操作；该方法是 {@code GatewayCallAccessLogger} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on complete operation; this method is the invocation entry point on {@code GatewayCallAccessLogger} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallAccessLogger.onComplete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     */
    @Override
    public void onComplete(GatewayCallEventV1 event) {
        LOGGER.info(
                "gateway_call eventId={} traceId={} protocol={} zone={} "
                        + "group={} operation={} route={} result={} code={} "
                        + "durationMs={} attempts={}",
                event.eventId(),
                event.trace().traceId(),
                event.request().protocol(),
                event.request().accessZone(),
                event.routing().gatewayGroupId(),
                event.routing().operationId(),
                event.routing().routeId(),
                event.result().category(),
                event.result().gatewayErrorCode(),
                event.result().durationMs(),
                event.attempts().size()
        );
    }

    /**
     * 中文说明：执行 onBody 操作；该方法是 {@code GatewayCallAccessLogger} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on body operation; this method is the invocation entry point on {@code GatewayCallAccessLogger} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallAccessLogger.onBody(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     */
    public void onBody(GatewayBodyLogEvent event) {
        byte[] sample = event.sample();
        BODY_LOGGER.info(
                "gateway_body direction={} contentType={} bytes={} "
                        + "metadataOnly={} truncated={} sampleBase64={}",
                event.direction(),
                event.contentType(),
                event.totalBytes(),
                event.metadataOnly(),
                event.totalBytes() > sample.length,
                event.metadataOnly() || sample.length == 0
                        ? ""
                        : Base64.getEncoder().encodeToString(sample)
        );
    }

    /**
     * 中文说明：执行 onWebSocketFrame 操作；该方法是 {@code GatewayCallAccessLogger} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on web socket frame operation; this method is the invocation entry point on {@code GatewayCallAccessLogger} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallAccessLogger.onWebSocketFrame(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param direction 参数 direction；parameter direction。
     * @param frameType 参数 frameType；parameter frame type。
     * @param payloadBytes 参数 payloadBytes；parameter payload bytes。
     * @param finalFragment 参数 finalFragment；parameter final fragment。
     */
    public void onWebSocketFrame(
            String direction,
            String frameType,
            long payloadBytes,
            boolean finalFragment) {
        BODY_LOGGER.info(
                "gateway_body direction={} contentType=websocket bytes={} "
                        + "metadataOnly=true frameType={} finalFragment={}",
                direction,
                payloadBytes,
                frameType,
                finalFragment
        );
    }
}
