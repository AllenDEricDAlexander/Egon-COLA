package top.egon.cola.component.yuheng.mcp.engine.mcp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewaySecurityException;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityChain;

/** Owns one physical MCP request's verified context, audit marker and security failure. */
@JsonIgnoreType
@Slf4j
@RequiredArgsConstructor
public final class McpRequestObservationComponent implements AutoCloseable {
    private final GatewaySecurityChain.AuthorizationObservationScope authorizationScope =
            new GatewaySecurityChain.AuthorizationObservationScope();
    private GatewayAuthContext verified;
    private GatewaySecurityException securityFailure;
    private boolean denialConfirmed;
    private boolean closed;

    public GatewaySecurityChain.AuthorizationObservationScope authorizationScope() { return authorizationScope; }

    public synchronized void bind(GatewayAuthContext context) {
        if (closed || verified != null || context == null || !context.principal().authenticated()
                || context.protocol() != GatewayProtocol.HTTP || !context.operationId().startsWith("yuheng.mcp.")
                || context.requiredOAuthScopes().isEmpty()) { throw unavailable(null); }
        verified = context;
    }

    public synchronized GatewayAuthContext verifiedContext() {
        requireAvailable();
        if (closed || verified == null) { throw unavailable(null); }
        return verified;
    }

    public synchronized GatewaySecurityException unavailable(Throwable cause) {
        if (securityFailure != null) { return securityFailure; }
        var failure = GatewaySecurityException.providerError();
        if (cause != null && cause != failure) { failure.initCause(cause); }
        if (!closed) { securityFailure = failure; }
        return failure;
    }

    public synchronized Throwable preserveSecurityFailure(Throwable original) {
        if (securityFailure == null) { return original; }
        if (securityFailure != original) { securityFailure.addSuppressed(original); }
        return securityFailure;
    }

    public synchronized void confirmDenial() { requireAvailable(); denialConfirmed = true; }
    public synchronized boolean denialConfirmed() { return !closed && denialConfirmed; }

    /** List handlers may filter per-capability denials; dependency failures still fail the request. */
    public synchronized void requireAvailable() {
        if (securityFailure != null) { throw securityFailure; }
        if (closed) { throw GatewaySecurityException.providerError(); }
    }

    @Override public void close() {
        synchronized (this) {
            if (closed) { return; }
            closed = true; verified = null; securityFailure = null; denialConfirmed = false;
        }
        authorizationScope.close();
    }
}
