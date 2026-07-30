package top.egon.cola.component.gateway.engine.http.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamAdapter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayHttpProxyContext(
        HttpUpstreamAdapter adapter,
        ProviderInstance provider,
        String method,
        String pathAndQuery,
        Map<String, List<String>> headers,
        Flux<DataBuffer> body,
        EffectiveGatewayTransportPolicy policy
) {

    public GatewayHttpProxyContext {
        adapter = Objects.requireNonNull(adapter, "adapter");
        provider = Objects.requireNonNull(provider, "provider");
        method = Objects.requireNonNull(method, "method");
        pathAndQuery = Objects.requireNonNull(pathAndQuery, "pathAndQuery");
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
        policy = Objects.requireNonNull(policy, "policy");
    }
}
