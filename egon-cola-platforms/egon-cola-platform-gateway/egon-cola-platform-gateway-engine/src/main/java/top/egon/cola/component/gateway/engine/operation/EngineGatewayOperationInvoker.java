package top.egon.cola.component.gateway.engine.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.traffic.GatewayAttemptExecutor;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficContext;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.ProviderCallClassification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves an active operation and executes it without a gateway loopback.
 */
public final class EngineGatewayOperationInvoker
        implements GatewayOperationInvoker {

    private static final Pattern PATH_VARIABLE = Pattern.compile(
            "\\{([^}/]+)}"
    );

    private final Supplier<CompiledGatewayRules> rules;

    private final ProviderSelector providerSelector;

    private final GatewayTrafficGovernance trafficGovernance;

    private final OperationTransport transport;

    private final ObjectMapper objectMapper;

    private final Duration defaultTimeout;

    private final long maximumRequestBytes;

    private final long maximumResponseBytes;

    private final GatewayAttemptExecutor attemptExecutor =
            new GatewayAttemptExecutor();

    public EngineGatewayOperationInvoker(
            Supplier<CompiledGatewayRules> rules,
            ProviderSelector providerSelector,
            GatewayTrafficGovernance trafficGovernance,
            OperationTransport transport,
            ObjectMapper objectMapper,
            Duration defaultTimeout,
            long maximumRequestBytes,
            long maximumResponseBytes) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.providerSelector = Objects.requireNonNull(
                providerSelector,
                "providerSelector"
        );
        this.trafficGovernance = Objects.requireNonNull(
                trafficGovernance,
                "trafficGovernance"
        );
        this.transport = Objects.requireNonNull(transport, "transport");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.defaultTimeout = positive(defaultTimeout, "defaultTimeout");
        this.maximumRequestBytes = positive(
                maximumRequestBytes,
                "maximumRequestBytes"
        );
        this.maximumResponseBytes = positive(
                maximumResponseBytes,
                "maximumResponseBytes"
        );
    }

    @Override
    public Publisher<GatewayInvocationResult> invoke(
            GatewayOperationInvocation invocation) {
        return Mono.defer(() -> invokeActive(invocation));
    }

    private Mono<GatewayInvocationResult> invokeActive(
            GatewayOperationInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        CompiledGatewayRules active = rules.get();
        if (active == null) {
            return Mono.error(new IllegalStateException(
                    "GATEWAY_RULES_NOT_READY"
            ));
        }
        GatewayRuntimeOperation operation = active.snapshot()
                .content()
                .operations()
                .stream()
                .filter(candidate -> candidate.operationId().equals(
                        invocation.operationId()
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "GATEWAY_OPERATION_NOT_FOUND"
                ));
        if (operation.deprecated()) {
            return Mono.error(new IllegalArgumentException(
                    "GATEWAY_OPERATION_DEPRECATED"
            ));
        }
        PreparedRequest prepared = prepare(operation, invocation);
        GatewayTrafficContext traffic = trafficContext(
                operation,
                invocation,
                prepared
        );
        return trafficGovernance.acquire(
                operation.policyRefs(),
                traffic,
                defaultTimeout
        ).flatMap(permit -> {
            long requestLimit = permit.requestSizeLimit(
                    maximumRequestBytes
            );
            if (prepared.body().length > requestLimit) {
                permit.close();
                return Mono.error(new IllegalArgumentException(
                        "GATEWAY_REQUEST_BODY_TOO_LARGE"
                ));
            }
            long responseLimit = permit.responseSizeLimit(
                    maximumResponseBytes
            );
            Set<String> failedProviders = new LinkedHashSet<>();
            AtomicInteger attempts = new AtomicInteger();
            return attemptExecutor.execute(
                    permit.retryPolicy(),
                    idempotent(operation),
                    true,
                    permit.timeout(),
                    () -> attempt(
                            operation,
                            prepared,
                            permit,
                            failedProviders,
                            responseLimit,
                            attempts.incrementAndGet()
                    ),
                    RetryableInvocationException.class::isInstance
            ).doFinally(ignored -> permit.close());
        });
    }

    private Mono<GatewayInvocationResult> attempt(
            GatewayRuntimeOperation operation,
            PreparedRequest prepared,
            GatewayTrafficGovernance.RequestPermit requestPermit,
            Set<String> failedProviders,
            long responseLimit,
            int attemptNumber) {
        ProviderSelectionHandle selection = providerSelector.select(
                serviceKey(operation.providerService()),
                operation.policyRefs(),
                failedProviders
        );
        ProviderInstance provider = selection.instance();
        GatewayTrafficGovernance.AttemptPermit attemptPermit;
        try {
            attemptPermit = requestPermit.acquireAttempt(provider);
        } catch (RuntimeException failure) {
            selection.close();
            return Mono.error(failure);
        }
        AtomicBoolean completed = new AtomicBoolean();
        return transport.invoke(provider, prepared, requestPermit.timeout())
                .flatMap(result -> {
                    if (result.body().length > responseLimit) {
                        return Mono.error(new IllegalArgumentException(
                                "GATEWAY_RESPONSE_BODY_TOO_LARGE"
                        ));
                    }
                    if (requestPermit.retryPolicy().enabled()
                            && idempotent(operation)
                            && attemptNumber < requestPermit.retryPolicy()
                            .maxAttempts()
                            && requestPermit.retryPolicy()
                            .retryableHttpStatus(result.statusCode())) {
                        failedProviders.add(provider.runtimeIdentity());
                        return Mono.error(new RetryableInvocationException(
                                "retryable upstream status "
                                        + result.statusCode()
                        ));
                    }
                    complete(
                            attemptPermit,
                            completed,
                            classification(result.statusCode())
                    );
                    return Mono.just(result);
                })
                .onErrorMap(failure -> failure instanceof
                        RetryableInvocationException
                        || failure instanceof IllegalArgumentException
                        ? failure
                        : new RetryableInvocationException(failure))
                .doOnError(failure -> complete(
                        attemptPermit,
                        completed,
                        failure instanceof RetryableInvocationException
                                ? ProviderCallClassification.RETRYABLE_FAILURE
                                : ProviderCallClassification.BUSINESS_FAILURE
                ))
                .doFinally(ignored -> {
                    if (completed.compareAndSet(false, true)) {
                        attemptPermit.close();
                    }
                    selection.close();
                });
    }

    private void complete(
            GatewayTrafficGovernance.AttemptPermit permit,
            AtomicBoolean completed,
            ProviderCallClassification classification) {
        if (completed.compareAndSet(false, true)) {
            permit.complete(classification);
        }
    }

    private ProviderCallClassification classification(int status) {
        if (status >= 500) {
            return ProviderCallClassification.RETRYABLE_FAILURE;
        }
        if (status >= 400) {
            return ProviderCallClassification.BUSINESS_FAILURE;
        }
        return ProviderCallClassification.SUCCESS;
    }

    private PreparedRequest prepare(
            GatewayRuntimeOperation operation,
            GatewayOperationInvocation invocation) {
        String[] identity = operation.methodIdentity().split("\\s+", 2);
        String method = operation.protocol() == GatewayProtocol.HTTP
                && identity.length == 2
                ? identity[0].toUpperCase(Locale.ROOT)
                : "POST";
        String path = operation.protocol() == GatewayProtocol.HTTP
                && identity.length == 2
                ? identity[1]
                : "/rpc/" + encodePath(operation.methodIdentity());
        LinkedHashMap<String, Object> remaining = new LinkedHashMap<>(
                invocation.arguments()
        );
        LinkedHashMap<String, String> pathVariables = new LinkedHashMap<>();
        Matcher matcher = PATH_VARIABLE.matcher(path);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!remaining.containsKey(name)) {
                throw new IllegalArgumentException(
                        "GATEWAY_OPERATION_ARGUMENT_MISSING: " + name
                );
            }
            String value = Objects.toString(remaining.remove(name), "");
            pathVariables.put(name, value);
            matcher.appendReplacement(
                    resolved,
                    Matcher.quoteReplacement(encodePath(value))
            );
        }
        matcher.appendTail(resolved);
        LinkedHashMap<String, String> query = new LinkedHashMap<>();
        byte[] body;
        if (Set.of("GET", "HEAD", "DELETE", "OPTIONS").contains(method)) {
            remaining.forEach((key, value) -> query.put(
                    key,
                    Objects.toString(value, "")
            ));
            body = new byte[0];
        } else {
            body = json(remaining);
        }
        String pathAndQuery = resolved + query(query);
        LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("accept", List.of("application/json"));
        if (body.length > 0) {
            headers.put("content-type", List.of("application/json"));
        }
        if (invocation.originalBearerToken() != null) {
            headers.put("authorization", List.of(
                    invocation.originalBearerToken()
            ));
        }
        invocation.traceHeaders().forEach((name, value) ->
                headers.put(name, List.of(value))
        );
        return new PreparedRequest(
                operation,
                method,
                pathAndQuery,
                Map.copyOf(headers),
                body,
                Map.copyOf(pathVariables),
                Map.copyOf(query)
        );
    }

    private GatewayTrafficContext trafficContext(
            GatewayRuntimeOperation operation,
            GatewayOperationInvocation invocation,
            PreparedRequest prepared) {
        return new GatewayTrafficContext(
                operation.operationId(),
                "mcp:" + operation.operationId(),
                operation.providerService().appCode(),
                invocation.callerId(),
                invocation.clientIp(),
                operation.providerService().serviceName(),
                null,
                invocation.traceHeaders(),
                prepared.pathVariables(),
                prepared.queryParameters()
        );
    }

    private ProviderServiceKey serviceKey(GatewayProviderServiceRef service) {
        return new ProviderServiceKey(
                service.bizCode(),
                service.appCode(),
                service.env(),
                service.namespace(),
                service.protocol() == GatewayProtocol.HTTP
                        ? ProviderProtocolType.HTTP
                        : ProviderProtocolType.RPC,
                service.serviceName(),
                service.group(),
                service.version(),
                service.transport()
        );
    }

    private boolean idempotent(GatewayRuntimeOperation operation) {
        return Boolean.parseBoolean(operation.attributes().getOrDefault(
                "idempotent",
                "false"
        ));
    }

    private byte[] json(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsBytes(arguments);
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "GATEWAY_OPERATION_ARGUMENTS_INVALID",
                    failure
            );
        }
    }

    private String query(Map<String, String> values) {
        if (values.isEmpty()) {
            return "";
        }
        List<String> parameters = new ArrayList<>();
        values.forEach((name, value) -> parameters.add(
                encodeQuery(name) + "=" + encodeQuery(value)
        ));
        return "?" + String.join("&", parameters);
    }

    private String encodePath(String value) {
        return encodeQuery(value).replace("+", "%20");
    }

    private String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private long positive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    @FunctionalInterface
    public interface OperationTransport {

        Mono<GatewayInvocationResult> invoke(
                ProviderInstance provider,
                PreparedRequest request,
                Duration timeout);
    }

    public record PreparedRequest(
            GatewayRuntimeOperation operation,
            String method,
            String pathAndQuery,
            Map<String, List<String>> headers,
            byte[] body,
            Map<String, String> pathVariables,
            Map<String, String> queryParameters
    ) {

        public PreparedRequest {
            operation = Objects.requireNonNull(operation, "operation");
            method = Objects.requireNonNull(method, "method");
            pathAndQuery = Objects.requireNonNull(
                    pathAndQuery,
                    "pathAndQuery"
            );
            headers = Map.copyOf(headers);
            body = body.clone();
            pathVariables = Map.copyOf(pathVariables);
            queryParameters = Map.copyOf(queryParameters);
        }

        @Override
        public byte[] body() {
            return body.clone();
        }
    }

    private static final class RetryableInvocationException
            extends RuntimeException {

        private RetryableInvocationException(String message) {
            super(message);
        }

        private RetryableInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
