package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.domain.GatewayRequestBodyTooLargeException;
import top.egon.cola.component.gateway.engine.http.domain.GatewayResponseBodyTooLargeException;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.common.buffer.GatewayDataBufferOwnership;
import top.egon.cola.component.gateway.engine.http.common.buffer.GatewayDataBufferPipeline;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayBodySizeLimiter} 是类型，位于当前 Gateway 模块的相关包中，负责网关BodySizeLimiter相关的职责与边界。
 * English summary: {@code GatewayBodySizeLimiter} is a type in the current Gateway module; it owns the gateway body size limiter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayBodySizeLimiter {

    /**
     * 中文说明：执行 aggregate请求 操作；该方法是 {@code GatewayBodySizeLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the aggregate request operation; this method is the invocation entry point on {@code GatewayBodySizeLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodySizeLimiter.aggregateRequest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param body 参数 body；parameter body。
     * @param limit 参数 limit；parameter limit。
     * @return 返回 aggregate请求 的处理结果；returns the result of the operation.
     */
    public Mono<byte[]> aggregateRequest(
            Flux<DataBuffer> body,
            long limit) {
        requirePositive(limit);
        Flux<DataBuffer> releasable =
                GatewayDataBufferPipeline.releaseOnDiscardOrCancel(body);
        return releasable.collect(
                ByteArrayOutputStream::new,
                (output, buffer) -> {
                    try {
                        int readableBytes = buffer.readableByteCount();
                        if ((long) output.size() + readableBytes > limit) {
                            throw new GatewayRequestBodyTooLargeException(
                                    "request body exceeds configured limit"
                            );
                        }
                        byte[] chunk = new byte[readableBytes];
                        buffer.read(chunk);
                        output.writeBytes(chunk);
                    } finally {
                        GatewayDataBufferOwnership.release(buffer);
                    }
                }
        ).map(ByteArrayOutputStream::toByteArray);
    }

    /**
     * 中文说明：执行 validate请求Headers 操作；该方法是 {@code GatewayBodySizeLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate request headers operation; this method is the invocation entry point on {@code GatewayBodySizeLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodySizeLimiter.validateRequestHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param limit 参数 limit；parameter limit。
     */
    public void validateRequestHeaders(
            Map<String, List<String>> headers,
            long limit) {
        requirePositive(limit);
        long declaredLength = contentLength(headers);
        if (declaredLength > limit) {
            throw new GatewayRequestBodyTooLargeException(
                    "request content-length exceeds configured limit"
            );
        }
    }

    /**
     * 中文说明：执行 limit响应 操作；该方法是 {@code GatewayBodySizeLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the limit response operation; this method is the invocation entry point on {@code GatewayBodySizeLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodySizeLimiter.limitResponse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param limit 参数 limit；parameter limit。
     * @return 返回 limit响应 的处理结果；returns the result of the operation.
     */
    public GatewayOutboundHttpResponse limitResponse(
            GatewayOutboundHttpResponse response,
            long limit) {
        requirePositive(limit);
        if (contentLength(response.headers()) > limit) {
            response.abandon();
            throw new GatewayResponseBodyTooLargeException(
                    "response content-length exceeds configured limit"
            );
        }
        Flux<DataBuffer> limitedBody =
                GatewayDataBufferPipeline.releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.limitBytes(
                                response.body(),
                                limit,
                                () -> new GatewayResponseBodyTooLargeException(
                                        "response body exceeds configured limit"
                                )
                        )
        );
        return response.withBody(limitedBody);
    }

    /**
     * 中文说明：执行 contentLength 操作；该方法是 {@code GatewayBodySizeLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the content length operation; this method is the invocation entry point on {@code GatewayBodySizeLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodySizeLimiter.contentLength(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 contentLength 的处理结果；returns the result of the operation.
     */
    private long contentLength(Map<String, List<String>> headers) {
        return headers.entrySet().stream()
                .filter(entry -> "content-length".equalsIgnoreCase(
                        entry.getKey()
                ))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .map(this::parseContentLength)
                .orElse(-1L);
    }

    /**
     * 中文说明：执行 parseContentLength 操作；该方法是 {@code GatewayBodySizeLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the parse content length operation; this method is the invocation entry point on {@code GatewayBodySizeLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodySizeLimiter.parseContentLength(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 parseContentLength 的处理结果；returns the result of the operation.
     */
    private long parseContentLength(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }

    /**
     * 中文说明：执行 requirePositive 操作；该方法是 {@code GatewayBodySizeLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require positive operation; this method is the invocation entry point on {@code GatewayBodySizeLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodySizeLimiter.requirePositive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param limit 参数 limit；parameter limit。
     */
    private void requirePositive(long limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "body size limit must be positive"
            );
        }
    }
}
