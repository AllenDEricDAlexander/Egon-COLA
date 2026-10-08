package top.egon.cola.platform.tianquan.shoubing.starter.client;

import java.net.URI;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest.IdpServiceTokenRequestException;

/** Adds a request-bound SERVICE Bearer token to one named WebClient. */
@AutoConfiguration
@AutoConfigureAfter({IdpStarterAutoConfiguration.class, WebClientAutoConfiguration.class})
@ConditionalOnBean({IdpServiceOAuth2Client.class, WebClient.Builder.class})
@ConditionalOnProperty(
        prefix = "egon.cola.platform.tianquan.shoubing.service-client",
        name = {"app-id", "registration-id"})
public class ServiceTokenWebClientConfiguration {

    /** WebClient attribute carrying the typed authorization request for exactly one call. */
    public static final String SERVICE_TOKEN_REQUEST_ATTRIBUTE =
            "egon.cola.platform.tianquan.shoubing.serviceTokenRequest";

    @Bean(name = "serviceTokenWebClient")
    @ConditionalOnBean({IdpServiceOAuth2Client.class, WebClient.Builder.class})
    @ConditionalOnMissingBean(name = "serviceTokenWebClient")
    public WebClient serviceTokenWebClient(
            WebClient.Builder builder,
            @Qualifier("idpServiceOAuth2Client") IdpServiceOAuth2Client tokenClient) {
        return builder.clone()
                .filter(serviceTokenFilter(tokenClient))
                .build();
    }

    ExchangeFilterFunction serviceTokenFilter(IdpServiceOAuth2Client tokenClient) {
        IdpServiceOAuth2Client client = Objects.requireNonNull(tokenClient, "tokenClient");
        return (request, next) -> {
            Object attribute = request.attributes().get(SERVICE_TOKEN_REQUEST_ATTRIBUTE);
            if (!(attribute instanceof IdpServiceTokenRequest tokenRequest)) {
                return Mono.error(new IdpServiceTokenRequestException(
                        "a typed SERVICE token request attribute is required"));
            }
            if (request.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                return Mono.error(new IdpServiceTokenRequestException(
                        "caller-supplied Authorization is not allowed"));
            }
            if (!sameOrigin(tokenRequest.audience(), request.url())) {
                return Mono.error(new IdpServiceTokenRequestException(
                        "SERVICE token audience and request origin do not match"));
            }
            return exchange(request, next, client, tokenRequest, false);
        };
    }

    private Mono<ClientResponse> exchange(
            ClientRequest request,
            ExchangeFunction next,
            IdpServiceOAuth2Client tokenClient,
            IdpServiceTokenRequest tokenRequest,
            boolean retried) {
        return Mono.fromCallable(() -> tokenClient.authorize(tokenRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(token -> {
                    ClientRequest authenticated = ClientRequest.from(request)
                            .headers(headers -> headers.setBearerAuth(token.getTokenValue()))
                            .build();
                    return next.exchange(authenticated)
                            .flatMap(response -> afterExchange(
                                    response, request, next, tokenClient,
                                    tokenRequest, retried));
                });
    }

    private Mono<ClientResponse> afterExchange(
            ClientResponse response,
            ClientRequest request,
            ExchangeFunction next,
            IdpServiceOAuth2Client tokenClient,
            IdpServiceTokenRequest tokenRequest,
            boolean retried) {
        if (response.statusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
            return Mono.just(response);
        }
        Mono<Void> invalidate = Mono.<Void>fromRunnable(
                        () -> tokenClient.invalidate(tokenRequest))
                .subscribeOn(Schedulers.boundedElastic());
        if (!retried && request.method() == HttpMethod.GET) {
            return response.releaseBody()
                    .then(invalidate)
                    .then(Mono.defer(() -> exchange(
                            request, next, tokenClient, tokenRequest, true)));
        }
        return invalidate.thenReturn(response);
    }

    private static boolean sameOrigin(URI audience, URI target) {
        if (!secureOrigin(audience) || !secureOrigin(target)) {
            return false;
        }
        return audience.getScheme().equalsIgnoreCase(target.getScheme())
                && audience.getHost().equalsIgnoreCase(target.getHost())
                && effectivePort(audience) == effectivePort(target);
    }

    private static boolean secureOrigin(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.isOpaque()
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            return false;
        }
        return "https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme())
                && loopback(uri.getHost());
    }

    private static boolean loopback(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)
                || "::1".equals(host) || "[::1]".equals(host);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
