package top.egon.cola.component.yuheng.edge.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutRevocationIntent;

import java.net.URI;
import java.time.Duration;

@Component("edgeOAuthTokenRevocationClient")
@RequiredArgsConstructor
@Slf4j
public class EdgeOAuthTokenRevocationClient {
    @Qualifier("reactiveClientRegistrationRepository")
    private final ReactiveClientRegistrationRepository registrations;
    @Qualifier("edgeOAuthProperties")
    private final EdgeOAuthProperties properties;
    @Qualifier("webClientBuilder")
    private final WebClient.Builder builder;
    private WebClient webClient;

    @PostConstruct
    public void initializeWebClient() {
        Duration timeout = properties.getLogout().getTimeout();
        HttpClient httpClient = HttpClient.create()
                .followRedirect(false)
                .responseTimeout(timeout);
        this.webClient = builder.clone()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public Mono<Void> revoke(EdgeLogoutRevocationIntent intent) {
        boolean configured = properties.getResources().values().stream().anyMatch(resource ->
                intent.registrationId().equals(resource.getIssuerRegistrationIds().get(intent.issuer())));
        if (!configured) { return Mono.error(new IllegalStateException("Edge revocation tuple is not in the deployment catalog")); }
        return registrations.findByRegistrationId(intent.registrationId())
                .switchIfEmpty(Mono.error(new IllegalStateException("Edge revocation registration is unavailable")))
                .flatMap(registration -> revoke(registration, intent))
                .doOnError(exception -> log.debug(
                        "Edge remote token revocation failed; intentId={}, failureType={}",
                        intent.intentId(),
                        exception.getClass().getSimpleName()));
    }

    private Mono<Void> revoke(ClientRegistration registration, EdgeLogoutRevocationIntent intent) {
        String issuer = issuer(registration);
        if (!issuer.equals(intent.issuer())
                || !registration.getClientId().equals(intent.clientId())
                || !tenantId(issuer).equals(intent.tenantId())
                || !registration.getRegistrationId().equals(intent.registrationId())
                || registration.getClientAuthenticationMethod() == null
                || !"client_secret_basic".equals(registration.getClientAuthenticationMethod().getValue())
                || !StringUtils.hasText(registration.getClientSecret())) {
            return Mono.error(new IllegalStateException("Edge revocation registration no longer matches the durable intent"));
        }
        String endpoint = revocationEndpoint(registration);
        if (!"https".equals(URI.create(endpoint).getScheme()) || !endpoint.equals(issuer + "/oauth2/revoke")
                || !sameOrigin(properties.getUpstreams().getAuthorizationServer(), URI.create(endpoint))) {
            return Mono.error(new IllegalStateException("Edge revocation endpoint is outside the configured issuer origin"));
        }

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(
                        registration.getClientId(), registration.getClientSecret()))
                .body(BodyInserters.fromFormData("token", intent.tokenValue())
                        .with("token_type_hint", intent.tokenTypeHint()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.releaseBody();
                    }
                    return response.releaseBody().then(Mono.error(new IllegalStateException(
                            "Authorization server token revocation was not confirmed")));
                })
                .timeout(properties.getLogout().getTimeout());
    }

    private static String revocationEndpoint(ClientRegistration registration) {
        Object endpoint = registration.getProviderDetails().getConfigurationMetadata()
                .get("revocation_endpoint");
        if (!(endpoint instanceof String value) || value.isBlank()) {
            throw new IllegalStateException("Authorization server revocation endpoint is unavailable");
        }
        URI uri = URI.create(value);
        if (!uri.isAbsolute() || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("Authorization server revocation endpoint is invalid");
        }
        return uri.toString();
    }

    private static String issuer(ClientRegistration registration) {
        String value = registration.getProviderDetails().getIssuerUri();
        if (!StringUtils.hasText(value)) {
            Object configured = registration.getProviderDetails().getConfigurationMetadata().get("issuer");
            value = configured instanceof String issuer ? issuer : null;
        }
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Edge issuer metadata is unavailable");
        }
        return URI.create(value).toASCIIString();
    }

    private static String tenantId(String issuer) {
        String path = URI.create(issuer).getPath();
        if ("/platform".equals(path)) {
            return "PLATFORM";
        }
        if (path != null && path.matches("/tenants/[1-9][0-9]*")) {
            return path.substring("/tenants/".length());
        }
        throw new IllegalStateException("Edge issuer tenant path is invalid");
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left != null
                && left.getScheme() != null
                && left.getHost() != null
                && left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
