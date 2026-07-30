package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferOwnership;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferPipeline;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

public final class GatewayBodySizeLimiter {

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

    public GatewayOutboundHttpResponse limitResponse(
            GatewayOutboundHttpResponse response,
            long limit) {
        requirePositive(limit);
        if (contentLength(response.headers()) > limit) {
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
        return new GatewayOutboundHttpResponse(
                response.status(),
                response.headers(),
                limitedBody
        );
    }

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

    private long parseContentLength(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }

    private void requirePositive(long limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "body size limit must be positive"
            );
        }
    }
}
