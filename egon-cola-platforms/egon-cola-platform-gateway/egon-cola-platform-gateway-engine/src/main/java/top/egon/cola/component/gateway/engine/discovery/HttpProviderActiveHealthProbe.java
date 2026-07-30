package top.egon.cola.component.gateway.engine.discovery;

import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.net.URI;
import java.net.URISyntaxException;

public final class HttpProviderActiveHealthProbe
        implements ProviderActiveHealthProbe {

    private final HttpClient client;

    public HttpProviderActiveHealthProbe(HttpClient client) {
        this.client = client;
    }

    @Override
    public Mono<Boolean> probe(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        return client.request(HttpMethod.valueOf(
                        policy.httpMethod(instance)
                ))
                .uri(uri(instance, policy.httpPath(instance)))
                .responseSingle((response, content) -> content.then(
                        Mono.fromSupplier(() -> policy
                                .httpSuccessStatuses(instance)
                                .contains(response.status().code()))
                ));
    }

    private String uri(ProviderInstance instance, String path) {
        try {
            return new URI(
                    instance.secure() ? "https" : "http",
                    null,
                    instance.host(),
                    instance.port(),
                    path,
                    null,
                    null
            ).toASCIIString();
        } catch (URISyntaxException invalid) {
            throw new IllegalArgumentException(
                    "invalid provider active health URI",
                    invalid
            );
        }
    }
}
