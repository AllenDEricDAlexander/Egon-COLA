package top.egon.cola.component.gateway.engine.mcp.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationRequest;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.util.Objects;

public final class Rbac3McpAuthorizationAdapter
        implements McpAuthorizationPort {

    private final SingleFlightSnapshotLoader snapshotLoader;

    public Rbac3McpAuthorizationAdapter(
            SingleFlightSnapshotLoader snapshotLoader) {
        this.snapshotLoader = Objects.requireNonNull(
                snapshotLoader,
                "snapshotLoader"
        );
    }

    @Override
    public Publisher<Decision> authorize(McpAuthorizationRequest request) {
        Objects.requireNonNull(request, "request");
        return Mono.fromCallable(() -> decide(
                request,
                snapshotLoader.load(principal(request))
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

    private Decision decide(
            McpAuthorizationRequest request,
            SystemAuthorizationSnapshot snapshot) {
        if (snapshot.authVersion() < request.minimumAuthVersion()
                || snapshot.contextVersion()
                < request.minimumContextVersion()
                || snapshot.policyVersion()
                < request.minimumPolicyVersion()) {
            return Decision.denied(
                    "RBAC3_SNAPSHOT_FENCED",
                    snapshot.authVersion(),
                    snapshot.contextVersion(),
                    snapshot.policyVersion()
            );
        }
        if (!snapshot.permissions().containsAll(
                request.requiredPermissions()
        )) {
            return Decision.denied(
                    "RBAC3_PERMISSION_DENIED",
                    snapshot.authVersion(),
                    snapshot.contextVersion(),
                    snapshot.policyVersion()
            );
        }
        return Decision.allowed(
                snapshot.authVersion(),
                snapshot.contextVersion(),
                snapshot.policyVersion()
        );
    }

    private IdentityPrincipal principal(McpAuthorizationRequest request) {
        return new IdentityPrincipal(
                request.subjectId(),
                request.tenantId(),
                request.sessionId(),
                request.clientId(),
                request.tokenId(),
                request.tokenVersion(),
                java.util.Set.of(request.resourceUri()),
                request.issuedAt(),
                request.expiresAt()
        );
    }
}
