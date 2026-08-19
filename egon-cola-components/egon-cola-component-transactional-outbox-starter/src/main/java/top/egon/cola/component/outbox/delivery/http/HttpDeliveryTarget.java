package top.egon.cola.component.outbox.delivery.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record HttpDeliveryTarget(
        URI uri,
        String method,
        Duration connectTimeout,
        Duration readTimeout,
        Map<String, String> fixedHeaders
) {

    public HttpDeliveryTarget {
        fixedHeaders = Map.copyOf(fixedHeaders);
    }
}
