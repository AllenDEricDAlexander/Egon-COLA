package top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.security;

import org.reactivestreams.Publisher;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.validation.Validator;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecision;
import top.egon.cola.component.yuheng.core.security.SecurityDecision;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewaySecurityException;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpRequestObservationComponent;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpOAuthScopeAuthorizationProvider;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.yuheng.core.mcp.security.McpAuthorizationRequest;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient;

import java.util.Objects;

/**
 * 中文说明：{@code Rbac3McpAuthorizationAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责Rbac3MCP授权Adapter相关的职责与边界。
 * English summary: {@code Rbac3McpAuthorizationAdapter} is a rbac3 mcp authorization adapter adapter in the current Gateway module; it owns the rbac3 mcp authorization adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Rbac3McpAuthorizationAdapter
        implements McpAuthorizationPort {

    /**
     * 中文说明：保存 snapshotLoader 对应的状态、依赖或配置值；字段类型为 {@code SingleFlightSnapshotLoader}，由 {@code Rbac3McpAuthorizationAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by snapshot loader; its type is {@code SingleFlightSnapshotLoader}, and {@code Rbac3McpAuthorizationAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code Rbac3McpAuthorizationAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code Rbac3McpAuthorizationAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Qualifier("singleFlightSnapshotLoader")
    private final SingleFlightSnapshotLoader snapshotLoader;
    @Qualifier("idpObservedVerificationComponent")
    private final IdpObservedVerificationComponent observations;
    @Qualifier("idpSecurityMetricsRecorder")
    private final IdpSecurityMetricsRecorder metrics;
    @Qualifier("defaultValidator")
    private final Validator validator;

    /**
     * 中文说明：创建 {@code Rbac3McpAuthorizationAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code Rbac3McpAuthorizationAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param snapshotLoader 参数 snapshotLoader；parameter snapshot loader。
     */
    public Rbac3McpAuthorizationAdapter(
            SingleFlightSnapshotLoader snapshotLoader) {
        this(Objects.requireNonNull(snapshotLoader, "snapshotLoader"), null, IdpSecurityMetricsRecorder.NOOP, null);
    }

    /** Managed hosts require the original strict observer and the same verified request scope. */
    public static Rbac3McpAuthorizationAdapter createConfigured(SingleFlightSnapshotLoader snapshots,
            IdpObservedVerificationComponent observations, IdpSecurityMetricsRecorder metrics, Validator validator) {
        return new Rbac3McpAuthorizationAdapter(Objects.requireNonNull(snapshots, "snapshots"),
                Objects.requireNonNull(observations, "observations"), Objects.requireNonNull(metrics, "metrics"), Objects.requireNonNull(validator, "validator"));
    }

    /**
     * 中文说明：执行 authorize 操作；该方法是 {@code Rbac3McpAuthorizationAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize operation; this method is the invocation entry point on {@code Rbac3McpAuthorizationAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code Rbac3McpAuthorizationAdapter.authorize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 authorize 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<Decision> authorize(McpAuthorizationRequest request) {
        Objects.requireNonNull(request, "request");
        if (observations != null) { return authorizeStrict(request); }
        return Mono.fromCallable(() -> decide(
                request,
                snapshotLoader.load(
                        principal(request),
                        request.userAccessToken()
                )
        )).onErrorResume(
                Rbac3AuthorizationClient.AuthorizationDeniedException.class,
                failure -> Mono.just(Decision.denied(
                        failure.getMessage(),
                        0L,
                        0L,
                        0L
                ))
        ).onErrorResume(
                Rbac3AuthorizationClient.AuthorizationUnavailableException.class,
                failure -> Mono.just(Decision.denied(
                        failure.getMessage(),
                        0L,
                        0L,
                        0L
                ))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Decision> authorizeStrict(McpAuthorizationRequest request) {
        return Mono.deferContextual(reactor -> {
            McpRequestObservationComponent observation = reactor.getOrDefault(McpRequestObservationComponent.class, null);
            if (observation == null) { return Mono.error(GatewaySecurityException.providerError()); }
            if (reactor.getOrDefault((Object) GatewayAuthContext.class, null) != observation.authorizationScope()
                    || !observation.authorizationScope().active()) { return Mono.error(observation.unavailable(null)); }
            return Mono.fromCallable(() -> verifiedContext(request, observation))
                    .flatMap(context -> Mono.fromCallable(() -> {
                        if (!"USER".equals(context.principal().principalType())) {
                            return Decision.denied("TIANQUAN_JIANSHEN_USER_PRINCIPAL_REQUIRED", 0L, 0L, 0L);
                        }
                        var attributes = context.principal().attributes();
                        var identity = new IdentityPrincipal(context.principal().principalId(), context.principal().tenantId(), request.tokenId(),
                                java.util.Set.of(attributes.get("tianquan-shoubing.audience")), request.issuedAt(), request.expiresAt(),
                                AuthenticationContext.of(attributes.get("tianquan-shoubing-auth-acr"), java.time.Instant.parse(attributes.get("tianquan-shoubing-auth-time"))));
                        return decide(request, snapshotLoader.load(identity, request.userAccessToken()));
                    }).onErrorResume(Rbac3AuthorizationClient.AuthorizationDeniedException.class,
                            denied -> Mono.just(Decision.denied("TIANQUAN_JIANSHEN_PERMISSION_DENIED", 0L, 0L, 0L)))
                            .flatMap(decision -> {
                                if (decision.allowed()) { record("ALLOW"); return Mono.just(decision); }
                                if ("TIANQUAN_JIANSHEN_SNAPSHOT_FENCED".equals(decision.reasonCode())) {
                                    return Mono.error(observation.unavailable(null));
                                }
                                return IdpOAuthScopeAuthorizationProvider.observeDecision(context, AuthorizationDecision.deny(decision.reasonCode()), observations, metrics, validator)
                                        .flatMap(observed -> {
                                            if (observed.decision() != SecurityDecision.DENY) { return Mono.error(observation.unavailable(null)); }
                                            observation.confirmDenial(); return Mono.just(decision);
                                        });
                            }))
                    .onErrorMap(failure -> {
                        if (failure instanceof InterruptedException) { Thread.currentThread().interrupt(); }
                        record("INDETERMINATE"); return observation.unavailable(failure);
                    }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    private GatewayAuthContext verifiedContext(McpAuthorizationRequest request, McpRequestObservationComponent observation) {
        var context = observation.verifiedContext(); var principal = context.principal();
        if (!(principal.trustedAuthenticationEvidence() instanceof GatewayVerifiedIdentityBO evidence) || evidence.proof() == null) {
            throw observation.unavailable(null);
        }
        new ValidationUtils(validator).validate(evidence);
        var attributes = principal.attributes();
        if (!evidence.proof().issuer().toASCIIString().equals(request.issuer()) || !evidence.proof().resource().toASCIIString().equals(request.resourceUri())
                || !principal.principalId().equals(request.subjectId()) || !Objects.equals(principal.tenantId(), request.tenantId())
                || !Objects.equals(attributes.getOrDefault("tianquan-shoubing.client-id", attributes.get("tianquan-shoubing.audience")), request.clientId())
                || !Objects.equals(attributes.get("tianquan-shoubing.token-id"), request.tokenId())
                || !Objects.equals(attributes.get("tianquan-shoubing.issued-at"), request.issuedAt().toString())
                || !Objects.equals(attributes.get("tianquan-shoubing.expires-at"), request.expiresAt().toString())
                || !evidence.verifiedScopes().containsAll(context.requiredOAuthScopes())) { throw observation.unavailable(null); }
        return context;
    }

    private void record(String result) {
        try { metrics.record(SecurityMetricObservationBO.builder().signal("rbac.authorization").result(result).build()); }
        catch (RuntimeException unavailable) { log.debug("MCP authorization metric unavailable; failureType={}", unavailable.getClass().getSimpleName()); }
    }

    /**
     * 中文说明：执行 decide 操作；该方法是 {@code Rbac3McpAuthorizationAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decide operation; this method is the invocation entry point on {@code Rbac3McpAuthorizationAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code Rbac3McpAuthorizationAdapter.decide(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @return 返回 decide 的处理结果；returns the result of the operation.
     */
    private Decision decide(
            McpAuthorizationRequest request,
            SystemAuthorizationSnapshot snapshot) {
        if (snapshot.authVersion() < request.minimumAuthVersion()
                || snapshot.policyVersion()
                < request.minimumContextVersion()
                || snapshot.policyVersion()
                < request.minimumPolicyVersion()) {
            return Decision.denied(
                    "TIANQUAN_JIANSHEN_SNAPSHOT_FENCED",
                    snapshot.authVersion(),
                    snapshot.policyVersion(),
                    snapshot.policyVersion()
            );
        }
        if (!snapshot.permissions().containsAll(
                request.requiredPermissions()
        )) {
            return Decision.denied(
                    "TIANQUAN_JIANSHEN_PERMISSION_DENIED",
                    snapshot.authVersion(),
                    snapshot.policyVersion(),
                    snapshot.policyVersion()
            );
        }
        return Decision.allowed(
                snapshot.authVersion(),
                snapshot.policyVersion(),
                snapshot.policyVersion()
        );
    }

    /**
     * 中文说明：执行 principal 操作；该方法是 {@code Rbac3McpAuthorizationAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the principal operation; this method is the invocation entry point on {@code Rbac3McpAuthorizationAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code Rbac3McpAuthorizationAdapter.principal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 principal 的处理结果；returns the result of the operation.
     */
    private IdentityPrincipal principal(McpAuthorizationRequest request) {
        return new IdentityPrincipal(
                request.subjectId(),
                request.tenantId(),
                request.tokenId(),
                java.util.Set.of("platform"),
                request.issuedAt(),
                request.expiresAt(),
                AuthenticationContext.password()
        );
    }
}
