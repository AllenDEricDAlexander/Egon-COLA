package top.egon.cola.component.outbox.delivery.http;

import java.util.Map;

@FunctionalInterface
public interface HttpCredentialProvider {

    Map<String, String> resolveHeaders(String destination);

    static HttpCredentialProvider none() {
        return destination -> Map.of();
    }
}
