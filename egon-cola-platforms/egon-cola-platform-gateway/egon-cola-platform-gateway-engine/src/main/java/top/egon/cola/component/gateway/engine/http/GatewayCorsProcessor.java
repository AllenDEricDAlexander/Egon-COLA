package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.engine.cors.RuntimeCorsPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class GatewayCorsProcessor {

    private final Supplier<Map<String, RuntimeCorsPolicy>> policies;

    public GatewayCorsProcessor(
            Supplier<Map<String, RuntimeCorsPolicy>> policies) {
        this.policies = policies;
    }

    public Decision evaluate(
            Set<String> policyRefs,
            GatewayInboundHttpRequest request,
            String routeMethod,
            String traceId) {
        String origin = firstHeader(request.headers(), "origin");
        if (origin == null) {
            return Decision.none();
        }
        RuntimeCorsPolicy policy = referencedPolicy(policyRefs);
        if (policy == null || !policy.enabled()) {
            throw rejected("CORS policy is not configured");
        }
        if (!allowedOrigin(policy, origin)) {
            throw rejected("request origin is not allowed");
        }
        String method = routeMethod.toUpperCase(Locale.ROOT);
        if (!policy.allowedMethods().contains(method)) {
            throw rejected("request method is not allowed");
        }
        boolean preflight = "OPTIONS".equalsIgnoreCase(request.method())
                && firstHeader(
                request.headers(),
                "access-control-request-method"
        ) != null;
        Set<String> requestedHeaders = preflight
                ? requestedHeaders(request.headers())
                : Set.of();
        if (!headersAllowed(policy, requestedHeaders)) {
            throw rejected("request headers are not allowed");
        }
        Map<String, List<String>> corsHeaders = headers(
                policy,
                origin,
                preflight,
                requestedHeaders
        );
        if (!preflight) {
            return new Decision(null, corsHeaders);
        }
        return new Decision(
                new GatewayOutboundHttpResponse(
                        204,
                        corsHeaders,
                        reactor.core.publisher.Flux.empty()
                ),
                corsHeaders
        );
    }

    private RuntimeCorsPolicy referencedPolicy(Set<String> policyRefs) {
        List<RuntimeCorsPolicy> referenced = policyRefs.stream()
                .map(policies.get()::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (referenced.size() > 1) {
            throw rejected("multiple CORS policies are referenced");
        }
        return referenced.isEmpty() ? null : referenced.getFirst();
    }

    private boolean allowedOrigin(
            RuntimeCorsPolicy policy,
            String origin) {
        return policy.allowedOrigins().contains("*")
                || policy.allowedOrigins().contains(origin);
    }

    private boolean headersAllowed(
            RuntimeCorsPolicy policy,
            Set<String> requestedHeaders) {
        if (requestedHeaders.isEmpty()) {
            return true;
        }
        Set<String> allowed = lowerCase(policy.allowedHeaders());
        return allowed.contains("*") || allowed.containsAll(requestedHeaders);
    }

    private Map<String, List<String>> headers(
            RuntimeCorsPolicy policy,
            String origin,
            boolean preflight,
            Set<String> requestedHeaders) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put(
                "access-control-allow-origin",
                List.of(policy.allowedOrigins().contains("*") ? "*" : origin)
        );
        if (policy.allowCredentials()) {
            result.put(
                    "access-control-allow-credentials",
                    List.of("true")
            );
        }
        if (!policy.exposedHeaders().isEmpty()) {
            result.put(
                    "access-control-expose-headers",
                    List.of(String.join(", ", policy.exposedHeaders()))
            );
        }
        if (preflight) {
            result.put(
                    "access-control-allow-methods",
                    List.of(String.join(", ", policy.allowedMethods()))
            );
            if (!requestedHeaders.isEmpty()) {
                result.put(
                        "access-control-allow-headers",
                        List.of(String.join(", ", requestedHeaders))
                );
            }
            result.put(
                    "access-control-max-age",
                    List.of(Long.toString(policy.maxAgeSeconds()))
            );
            result.put(
                    "vary",
                    List.of(
                            "Origin",
                            "Access-Control-Request-Method",
                            "Access-Control-Request-Headers"
                    )
            );
        } else {
            result.put("vary", List.of("Origin"));
        }
        return Map.copyOf(result);
    }

    private Set<String> requestedHeaders(
            Map<String, List<String>> headers) {
        String value = firstHeader(
                headers,
                "access-control-request-headers"
        );
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(header -> !header.isEmpty())
                .map(header -> header.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        return Set.copyOf(result);
    }

    private Set<String> lowerCase(Set<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        return result;
    }

    private String firstHeader(
            Map<String, List<String>> headers,
            String name) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse(null);
    }

    private GatewayCorsException rejected(String message) {
        return new GatewayCorsException(message);
    }

    public record Decision(
            GatewayOutboundHttpResponse preflight,
            Map<String, List<String>> responseHeaders
    ) {

        private static Decision none() {
            return new Decision(null, Map.of());
        }

        public Optional<GatewayOutboundHttpResponse> preflightResponse() {
            return Optional.ofNullable(preflight);
        }

        public GatewayOutboundHttpResponse decorate(
                GatewayOutboundHttpResponse response) {
            if (responseHeaders.isEmpty()) {
                return response;
            }
            Map<String, List<String>> headers = new LinkedHashMap<>(
                    response.headers()
            );
            headers.putAll(responseHeaders);
            return response.withHeadersAndBody(
                    headers,
                    response.body()
            );
        }
    }
}
