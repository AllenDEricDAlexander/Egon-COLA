package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class GatewayBodySizeLimiter {

    public Mono<byte[]> aggregateRequest(Flux<byte[]> body, long limit) {
        requirePositive(limit);
        return body.collect(
                ByteArrayOutputStream::new,
                (output, bytes) -> {
                    if ((long) output.size() + bytes.length > limit) {
                        throw new GatewayRequestBodyTooLargeException(
                                "request body exceeds configured limit"
                        );
                    }
                    output.writeBytes(bytes);
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
        AtomicLong received = new AtomicLong();
        Flux<byte[]> limitedBody = response.body().handle(
                (bytes, sink) -> {
                    if (received.addAndGet(bytes.length) > limit) {
                        sink.error(
                                new GatewayResponseBodyTooLargeException(
                                        "response body exceeds configured limit"
                                )
                        );
                    } else {
                        sink.next(bytes);
                    }
                }
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
